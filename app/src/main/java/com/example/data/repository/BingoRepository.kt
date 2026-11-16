package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.OperationType
import com.example.data.handleFirestoreError
import com.example.model.BingoRound
import com.example.model.PaymentRequest
import com.example.model.RoundTicket
import com.example.model.UserProfile
import com.example.model.WalletTransaction
import com.example.model.WithdrawalRequest
import com.example.util.BingoEngine
import com.example.util.RoomConfig
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

class BingoRepository(
  private val db: FirebaseFirestore,
  private val auth: FirebaseAuth = Firebase.auth,
) {
  constructor(context: Context) : this(
    FirebaseFirestore.getInstance(
      context.applicationContext.getString(R.string.firestore_database_id)
    ),
    Firebase.auth,
  )

  private fun requireUserId(): String =
    auth.currentUser?.uid
      ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")

  fun isCurrentUserAdmin(): Boolean {
    val user = auth.currentUser ?: return false
    return user.isEmailVerified && user.email.equals(BingoEngine.ADMIN_EMAIL, ignoreCase = true)
  }

  suspend fun ensureUserProfile(
    displayName: String?,
    email: String?,
    preferredUsername: String? = null,
    preferredPhone: String? = null,
  ): Result<UserProfile> {
    val uid = requireUserId()
    val docRef = db.collection("users").document(uid)
    return try {
      val cleanPreferredName = preferredUsername?.trim()?.takeIf { it.length >= 2 }?.take(40)
      val cleanPreferredPhone = preferredPhone?.let { BingoEngine.cleanPhone(it) }?.takeIf { it.isNotBlank() }?.take(20)
      val snap = docRef.get().await()
      if (snap.exists()) {
        val profile = snap.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
          ?: throw IllegalStateException("Failed to deserialize user profile")
        if (cleanPreferredName != null || cleanPreferredPhone != null) {
          val updates = mutableMapOf<String, Any>(
            "updatedAt" to FieldValue.serverTimestamp(),
          )
          if (cleanPreferredName != null) updates["username"] = cleanPreferredName
          if (cleanPreferredPhone != null) updates["phone"] = cleanPreferredPhone
          docRef.update(updates).await()
          return Result.success(
            profile.copy(
              username = cleanPreferredName ?: profile.username,
              phone = cleanPreferredPhone ?: profile.phone,
            )
          )
        }
        Result.success(profile)
      } else {
        val rawName = (cleanPreferredName
          ?: displayName?.trim().takeIf { !it.isNullOrBlank() }
          ?: email?.substringBefore('@')
          ?: "Player")
          .take(40)
          .let { if (it.length < 2) "Player" else it }
        val payload = mapOf(
          "userId" to uid,
          "username" to rawName,
          "phone" to (cleanPreferredPhone ?: ""),
          "email" to (email ?: "").take(120),
          "wallet" to 0L,
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp(),
        )
        docRef.set(payload).await()
        val created = docRef.get().await()
          .toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
          ?: UserProfile(
            id = uid,
            userId = uid,
            username = rawName,
            phone = cleanPreferredPhone ?: "",
            email = email ?: "",
            wallet = 0L,
          )
        Result.success(created)
      }
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, docRef.path, auth)
      Result.failure(e)
    }
  }

  suspend fun updateUserProfile(username: String, phone: String): Result<Unit> {
    val uid = requireUserId()
    val cleanUsername = username.trim()
    require(cleanUsername.length in 2..40) { "Username must be 2 to 40 characters." }
    val cleanPhone = BingoEngine.cleanPhone(phone)
    require(cleanPhone.length <= 20) { "Phone number is too long." }

    val docRef = db.collection("users").document(uid)
    return try {
      docRef.update(
        mapOf(
          "username" to cleanUsername,
          "phone" to cleanPhone,
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      ).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path, auth)
      Result.failure(e)
    }
  }

  fun observeUserProfile(uid: String): Flow<UserProfile?> = flow {
    val path = "users/$uid"
    emitAll(
      db.collection("users").document(uid)
        .snapshots()
        .map { snap ->
          if (snap.exists()) {
            snap.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
          } else {
            null
          }
        }
        .catch { error ->
          logListenerError(error, OperationType.GET, path, uid)
          throw error
        }
    )
  }

  fun observeTransactions(uid: String): Flow<List<WalletTransaction>> = flow {
    val path = "users/$uid/transactions"
    emitAll(
      db.collection("users").document(uid).collection("transactions")
        .orderBy("createdAt", Query.Direction.DESCENDING)
        .limit(25)
        .snapshots()
        .map { snap ->
          snap.toObjects(WalletTransaction::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        }
        .catch { error ->
          logListenerError(error, OperationType.LIST, path, uid)
          throw error
        }
    )
  }

  fun observeRoundsForCycle(cycle: Long): Flow<List<BingoRound>> = flow {
    val uid = requireUserId()
    val path = "rounds"
    emitAll(
      db.collection(path)
        .whereEqualTo("cycle", cycle)
        .snapshots()
        .map { snap ->
          snap.toObjects(BingoRound::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        }
        .catch { error ->
          logListenerError(error, OperationType.LIST, path, uid)
          throw error
        }
    )
  }

  fun observeRound(roundId: String): Flow<BingoRound?> = flow {
    val uid = requireUserId()
    val path = "rounds/$roundId"
    emitAll(
      db.collection("rounds").document(roundId)
        .snapshots()
        .map { snap ->
          if (snap.exists()) {
            snap.toObject(BingoRound::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
          } else {
            null
          }
        }
        .catch { error ->
          logListenerError(error, OperationType.GET, path, uid)
          throw error
        }
    )
  }

  fun observeTicketsForRound(roundId: String): Flow<List<RoundTicket>> = flow {
    val uid = requireUserId()
    val path = "rounds/$roundId/tickets"
    emitAll(
      db.collection("rounds").document(roundId).collection("tickets")
        .whereEqualTo("roundId", roundId)
        .snapshots()
        .map { snap ->
          snap.toObjects(RoundTicket::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        }
        .catch { error ->
          logListenerError(error, OperationType.LIST, path, uid)
          throw error
        }
    )
  }

  suspend fun joinRound(
    roundId: String,
    cycle: Long,
    room: RoomConfig,
    ticketNumber: Int,
  ): Result<RoundTicket> {
    val uid = requireUserId()
    val userRef = db.collection("users").document(uid)
    val roundRef = db.collection("rounds").document(roundId)
    val ticketRef = roundRef.collection("tickets").document(ticketNumber.toString())
    val txRef = userRef.collection("transactions").document(UUID.randomUUID().toString())

    return try {
      val ticket = db.runTransaction { transaction ->
        val userSnap = transaction.get(userRef)
        val userWallet = userSnap.getLong("wallet") ?: 0L
        val username = (userSnap.getString("username") ?: "Player").take(40)
        if (userWallet < room.stake.toLong()) {
          throw IllegalStateException("Not enough balance. Please deposit first.")
        }

        val ticketSnap = transaction.get(ticketRef)
        if (ticketSnap.exists()) {
          throw IllegalStateException("Ticket #$ticketNumber is already taken.")
        }

        val roundSnap = transaction.get(roundRef)
        if (!roundSnap.exists()) {
          transaction.set(
            roundRef,
            mapOf(
              "cycle" to cycle,
              "stake" to room.stake,
              "maxTickets" to room.maxTickets,
              "playerCount" to 1,
              "status" to "waiting",
              "winnerUid" to "",
              "winnerName" to "",
              "winnerTicket" to 0,
              "winnerCalls" to 0,
              "winnerPrize" to 0L,
              "createdAt" to FieldValue.serverTimestamp(),
              "updatedAt" to FieldValue.serverTimestamp(),
            )
          )
        } else {
          val status = roundSnap.getString("status") ?: "waiting"
          val count = (roundSnap.getLong("playerCount") ?: 0L).toInt()
          if (status != "waiting") {
            throw IllegalStateException("This round has already started.")
          }
          if (count >= room.maxTickets) {
            throw IllegalStateException("This room is full.")
          }
          transaction.update(
            roundRef,
            mapOf(
              "playerCount" to count + 1,
              "updatedAt" to FieldValue.serverTimestamp(),
            )
          )
        }

        val ticketPayload = mapOf(
          "roundId" to roundId,
          "ticketNumber" to ticketNumber,
          "userId" to uid,
          "username" to username,
          "stake" to room.stake,
          "refunded" to false,
          "createdAt" to FieldValue.serverTimestamp(),
        )
        transaction.set(ticketRef, ticketPayload)

        transaction.update(
          userRef,
          mapOf(
            "wallet" to userWallet - room.stake.toLong(),
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        )

        transaction.set(
          txRef,
          mapOf(
            "userId" to uid,
            "label" to "Room ${room.stake} ETB (#$ticketNumber)",
            "amount" to -room.stake.toLong(),
            "type" to "join",
            "roundId" to roundId,
            "createdAt" to FieldValue.serverTimestamp(),
          )
        )

        RoundTicket(
          id = ticketNumber.toString(),
          roundId = roundId,
          ticketNumber = ticketNumber,
          userId = uid,
          username = username,
          stake = room.stake,
          refunded = false,
        )
      }.await()
      Result.success(ticket)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, roundRef.path, auth)
      Result.failure(e)
    }
  }

  suspend fun claimBingoWin(
    roundId: String,
    ticketNumber: Int,
    callsCount: Int,
  ): Result<Long> {
    val uid = requireUserId()
    val userRef = db.collection("users").document(uid)
    val roundRef = db.collection("rounds").document(roundId)
    val txRef = userRef.collection("transactions").document(UUID.randomUUID().toString())

    return try {
      val prize = db.runTransaction { transaction ->
        val roundSnap = transaction.get(roundRef)
        if (!roundSnap.exists()) {
          throw IllegalStateException("Round does not exist.")
        }
        val status = roundSnap.getString("status") ?: "waiting"
        if (status == "finished") {
          val winner = roundSnap.getString("winnerName") ?: "Another player"
          throw IllegalStateException("Too late: $winner already won this round.")
        }
        if (status == "refunded") {
          throw IllegalStateException("This round was refunded.")
        }
        val playerCount = (roundSnap.getLong("playerCount") ?: 0L).toInt()
        if (playerCount < 2) {
          throw IllegalStateException("Round needs at least 2 players.")
        }
        val stake = (roundSnap.getLong("stake") ?: 0L).toInt()
        val computedPrize = BingoEngine.prizeForPool(playerCount, stake)

        val userSnap = transaction.get(userRef)
        val currentWallet = userSnap.getLong("wallet") ?: 0L
        val username = (userSnap.getString("username") ?: "Player").take(40)

        transaction.update(
          roundRef,
          mapOf(
            "status" to "finished",
            "winnerUid" to uid,
            "winnerName" to username,
            "winnerTicket" to ticketNumber,
            "winnerCalls" to callsCount.coerceIn(1, 75),
            "winnerPrize" to computedPrize,
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        )

        if (computedPrize > 0L) {
          transaction.update(
            userRef,
            mapOf(
              "wallet" to currentWallet + computedPrize,
              "lastWinRoundId" to roundId,
              "updatedAt" to FieldValue.serverTimestamp(),
            )
          )
          transaction.set(
            txRef,
            mapOf(
              "userId" to uid,
              "label" to "Room win (#$ticketNumber)",
              "amount" to computedPrize,
              "type" to "win",
              "roundId" to roundId,
              "createdAt" to FieldValue.serverTimestamp(),
            )
          )
        }
        computedPrize
      }.await()
      Result.success(prize)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, roundRef.path, auth)
      Result.failure(e)
    }
  }

  suspend fun refundSoloRound(roundId: String, ticketNumber: Int): Result<Long> {
    val uid = requireUserId()
    val userRef = db.collection("users").document(uid)
    val roundRef = db.collection("rounds").document(roundId)
    val ticketId = ticketNumber.toString()
    val ticketRef = roundRef.collection("tickets").document(ticketId)
    val txRef = userRef.collection("transactions").document(UUID.randomUUID().toString())

    return try {
      val refundAmount = db.runTransaction { transaction ->
        val roundSnap = transaction.get(roundRef)
        val ticketSnap = transaction.get(ticketRef)
        val userSnap = transaction.get(userRef)

        if (!roundSnap.exists() || !ticketSnap.exists()) {
          throw IllegalStateException("Ticket not found for refund.")
        }
        val playerCount = (roundSnap.getLong("playerCount") ?: 0L).toInt()
        if (playerCount >= 2) {
          throw IllegalStateException("Round already has $playerCount players.")
        }
        val alreadyRefunded = ticketSnap.getBoolean("refunded") ?: false
        if (alreadyRefunded) {
          return@runTransaction 0L
        }
        val stake = roundSnap.getLong("stake") ?: 0L
        val currentWallet = userSnap.getLong("wallet") ?: 0L

        transaction.update(
          roundRef,
          mapOf(
            "status" to "refunded",
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        )
        transaction.update(ticketRef, mapOf("refunded" to true))
        transaction.update(
          userRef,
          mapOf(
            "wallet" to currentWallet + stake,
            "lastRefundRoundId" to roundId,
            "lastRefundTicketId" to ticketId,
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        )
        transaction.set(
          txRef,
          mapOf(
            "userId" to uid,
            "label" to "Room refund (#$ticketNumber)",
            "amount" to stake,
            "type" to "refund",
            "roundId" to roundId,
            "createdAt" to FieldValue.serverTimestamp(),
          )
        )
        stake
      }.await()
      Result.success(refundAmount)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, roundRef.path, auth)
      Result.failure(e)
    }
  }

  fun observeMyPaymentRequests(uid: String): Flow<List<PaymentRequest>> = flow {
    val path = "payment_requests"
    emitAll(
      db.collection(path)
        .whereEqualTo("userId", uid)
        .snapshots()
        .map { snap ->
          snap.toObjects(PaymentRequest::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            .sortedByDescending { it.createdAt?.seconds ?: 0L }
        }
        .catch { error ->
          logListenerError(error, OperationType.LIST, path, uid)
          throw error
        }
    )
  }

  fun observeMyWithdrawalRequests(uid: String): Flow<List<WithdrawalRequest>> = flow {
    val path = "withdrawal_requests"
    emitAll(
      db.collection(path)
        .whereEqualTo("userId", uid)
        .snapshots()
        .map { snap ->
          snap.toObjects(WithdrawalRequest::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            .sortedByDescending { it.createdAt?.seconds ?: 0L }
        }
        .catch { error ->
          logListenerError(error, OperationType.LIST, path, uid)
          throw error
        }
    )
  }

  suspend fun submitPaymentRequest(
    amount: Long,
    receiptInput: String,
    username: String,
    phone: String,
  ): Result<String> {
    val uid = requireUserId()
    val receiptId = BingoEngine.extractTelebirrReceiptId(receiptInput)
      ?: return Result.failure(IllegalArgumentException("Enter a valid Telebirr receipt link or number."))
    if (amount < BingoEngine.PAY_MIN_BIRR) {
      return Result.failure(IllegalArgumentException("Payment below ${BingoEngine.PAY_MIN_BIRR} birr is not confirmed."))
    }

    val colRef = db.collection("payment_requests")
    return try {
      val existingMine = colRef.whereEqualTo("userId", uid).get().await()
      val duplicate = existingMine.documents.any { doc ->
        doc.getString("receiptId") == receiptId && doc.getString("status") != "declined"
      }
      if (duplicate) {
        return Result.failure(IllegalStateException("This receipt was already submitted."))
      }

      val docId = UUID.randomUUID().toString()
      val docRef = colRef.document(docId)
      docRef.set(
        mapOf(
          "userId" to uid,
          "username" to username.take(40).ifBlank { "Player" },
          "phone" to BingoEngine.cleanPhone(phone).take(20),
          "amount" to amount,
          "receiptId" to receiptId,
          "receiptLink" to receiptInput.trim().take(300),
          "status" to "pending",
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      ).await()
      Result.success(docId)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, colRef.path, auth)
      Result.failure(e)
    }
  }

  suspend fun submitWithdrawalRequest(
    amount: Long,
    telebirrAccount: String,
    username: String,
    phone: String,
  ): Result<String> {
    val uid = requireUserId()
    if (amount < BingoEngine.PAY_MIN_BIRR) {
      return Result.failure(IllegalArgumentException("Minimum withdrawal is ${BingoEngine.PAY_MIN_BIRR} birr."))
    }
    if (!BingoEngine.isValidTelebirrPhone(telebirrAccount)) {
      return Result.failure(IllegalArgumentException("Enter a valid Telebirr account number (09XXXXXXXX)."))
    }
    val cleanedAccount = BingoEngine.cleanPhone(telebirrAccount)
    val userRef = db.collection("users").document(uid)
    val wdId = UUID.randomUUID().toString()
    val wdRef = db.collection("withdrawal_requests").document(wdId)
    val txRef = userRef.collection("transactions").document(UUID.randomUUID().toString())

    return try {
      db.runTransaction { transaction ->
        val userSnap = transaction.get(userRef)
        val wallet = userSnap.getLong("wallet") ?: 0L
        if (amount > wallet) {
          throw IllegalStateException("You cannot withdraw more than your wallet balance.")
        }
        transaction.update(
          userRef,
          mapOf(
            "wallet" to wallet - amount,
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        )
        transaction.set(
          wdRef,
          mapOf(
            "userId" to uid,
            "username" to username.take(40).ifBlank { "Player" },
            "phone" to BingoEngine.cleanPhone(phone).take(20),
            "amount" to amount,
            "telebirrAccount" to cleanedAccount,
            "status" to "pending",
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        )
        transaction.set(
          txRef,
          mapOf(
            "userId" to uid,
            "label" to "Withdrawal ($cleanedAccount)",
            "amount" to -amount,
            "type" to "withdraw",
            "createdAt" to FieldValue.serverTimestamp(),
          )
        )
      }.await()
      Result.success(wdId)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, wdRef.path, auth)
      Result.failure(e)
    }
  }

  // ===== Admin Queries & Mutations =====

  fun observeAllPayments(): Flow<List<PaymentRequest>> = flow {
    val uid = requireUserId()
    val path = "payment_requests"
    emitAll(
      db.collection(path)
        .snapshots()
        .map { snap ->
          snap.toObjects(PaymentRequest::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            .sortedByDescending { it.createdAt?.seconds ?: 0L }
        }
        .catch { error ->
          logListenerError(error, OperationType.LIST, path, uid)
          throw error
        }
    )
  }

  fun observeAllWithdrawals(): Flow<List<WithdrawalRequest>> = flow {
    val uid = requireUserId()
    val path = "withdrawal_requests"
    emitAll(
      db.collection(path)
        .snapshots()
        .map { snap ->
          snap.toObjects(WithdrawalRequest::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            .sortedByDescending { it.createdAt?.seconds ?: 0L }
        }
        .catch { error ->
          logListenerError(error, OperationType.LIST, path, uid)
          throw error
        }
    )
  }

  fun observeAllUsers(): Flow<List<UserProfile>> = flow {
    val uid = requireUserId()
    val path = "users"
    emitAll(
      db.collection(path)
        .snapshots()
        .map { snap ->
          snap.toObjects(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            .sortedBy { it.username.lowercase() }
        }
        .catch { error ->
          logListenerError(error, OperationType.LIST, path, uid)
          throw error
        }
    )
  }

  suspend fun approvePaymentRequest(requestId: String, approvedAmount: Long): Result<Unit> {
    requireUserId()
    if (approvedAmount < BingoEngine.PAY_MIN_BIRR) {
      return Result.failure(IllegalArgumentException("Amount must be at least ${BingoEngine.PAY_MIN_BIRR} ETB."))
    }
    val reqRef = db.collection("payment_requests").document(requestId)
    return try {
      db.runTransaction { transaction ->
        val reqSnap = transaction.get(reqRef)
        if (!reqSnap.exists() || reqSnap.getString("status") != "pending") {
          throw IllegalStateException("Payment request is no longer pending.")
        }
        val targetUid = reqSnap.getString("userId") ?: throw IllegalStateException("Missing userId.")
        val userRef = db.collection("users").document(targetUid)
        val userSnap = transaction.get(userRef)
        if (!userSnap.exists()) {
          throw IllegalStateException("User no longer exists.")
        }
        val wallet = userSnap.getLong("wallet") ?: 0L
        val txRef = userRef.collection("transactions").document(UUID.randomUUID().toString())

        transaction.update(
          reqRef,
          mapOf(
            "status" to "approved",
            "amount" to approvedAmount,
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        )
        transaction.update(
          userRef,
          mapOf(
            "wallet" to wallet + approvedAmount,
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        )
        transaction.set(
          txRef,
          mapOf(
            "userId" to targetUid,
            "label" to "Telebirr Top-up",
            "amount" to approvedAmount,
            "type" to "topup",
            "createdAt" to FieldValue.serverTimestamp(),
          )
        )
      }.await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, reqRef.path, auth)
      Result.failure(e)
    }
  }

  suspend fun declinePaymentRequest(requestId: String): Result<Unit> {
    requireUserId()
    val reqRef = db.collection("payment_requests").document(requestId)
    return try {
      reqRef.update(
        mapOf(
          "status" to "declined",
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      ).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, reqRef.path, auth)
      Result.failure(e)
    }
  }

  suspend fun markWithdrawalPaid(requestId: String): Result<Unit> {
    requireUserId()
    val reqRef = db.collection("withdrawal_requests").document(requestId)
    return try {
      reqRef.update(
        mapOf(
          "status" to "approved",
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      ).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, reqRef.path, auth)
      Result.failure(e)
    }
  }

  suspend fun declineAndRefundWithdrawal(requestId: String): Result<Unit> {
    requireUserId()
    val reqRef = db.collection("withdrawal_requests").document(requestId)
    return try {
      db.runTransaction { transaction ->
        val reqSnap = transaction.get(reqRef)
        if (!reqSnap.exists() || reqSnap.getString("status") != "pending") {
          throw IllegalStateException("Withdrawal request is no longer pending.")
        }
        val targetUid = reqSnap.getString("userId") ?: throw IllegalStateException("Missing userId.")
        val amount = reqSnap.getLong("amount") ?: 0L
        val userRef = db.collection("users").document(targetUid)
        val userSnap = transaction.get(userRef)

        transaction.update(
          reqRef,
          mapOf(
            "status" to "declined",
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        )
        if (userSnap.exists() && amount > 0L) {
          val wallet = userSnap.getLong("wallet") ?: 0L
          val txRef = userRef.collection("transactions").document(UUID.randomUUID().toString())
          transaction.update(
            userRef,
            mapOf(
              "wallet" to wallet + amount,
              "updatedAt" to FieldValue.serverTimestamp(),
            )
          )
          transaction.set(
            txRef,
            mapOf(
              "userId" to targetUid,
              "label" to "Withdrawal refund",
              "amount" to amount,
              "type" to "refund",
              "createdAt" to FieldValue.serverTimestamp(),
            )
          )
        }
      }.await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, reqRef.path, auth)
      Result.failure(e)
    }
  }

  suspend fun adminAdjustWallet(targetUid: String, delta: Long, label: String): Result<Unit> {
    requireUserId()
    val userRef = db.collection("users").document(targetUid)
    val txRef = userRef.collection("transactions").document(UUID.randomUUID().toString())
    return try {
      db.runTransaction { transaction ->
        val snap = transaction.get(userRef)
        if (!snap.exists()) throw IllegalStateException("User not found.")
        val current = snap.getLong("wallet") ?: 0L
        val next = current + delta
        if (next < 0L) throw IllegalStateException("Not enough balance to deduct.")
        transaction.update(
          userRef,
          mapOf(
            "wallet" to next,
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        )
        transaction.set(
          txRef,
          mapOf(
            "userId" to targetUid,
            "label" to label,
            "amount" to delta,
            "type" to "admin",
            "createdAt" to FieldValue.serverTimestamp(),
          )
        )
      }.await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, userRef.path, auth)
      Result.failure(e)
    }
  }

  suspend fun adminDeleteUser(targetUid: String): Result<Unit> {
    requireUserId()
    val userRef = db.collection("users").document(targetUid)
    return try {
      userRef.delete().await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, userRef.path, auth)
      Result.failure(e)
    }
  }

  private fun logListenerError(
    error: Throwable,
    operationType: OperationType,
    path: String,
    expectedUid: String,
  ) {
    val firestoreError = generateSequence(error) { it.cause }
      .filterIsInstance<FirebaseFirestoreException>()
      .firstOrNull()
    if (firestoreError == null && error is CancellationException) return
    val toLog = firestoreError ?: (error as? Exception) ?: return
    if (auth.currentUser?.uid == expectedUid) {
      handleFirestoreError(toLog, operationType, path, auth)
    }
  }
}
