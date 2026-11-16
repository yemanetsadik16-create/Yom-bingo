package com.example.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

sealed interface UiState<out T> {
  data object Loading : UiState<Nothing>
  data class Success<T>(val data: T) : UiState<T>
  data class Error(val message: String) : UiState<Nothing>
}

@IgnoreExtraProperties
data class UserProfile(
  @DocumentId val id: String = "",
  val userId: String = "",
  val username: String = "",
  val phone: String = "",
  val email: String = "",
  val wallet: Long = 0L,
  val lastWinRoundId: String? = null,
  val lastRefundRoundId: String? = null,
  val lastRefundTicketId: String? = null,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class WalletTransaction(
  @DocumentId val id: String = "",
  val userId: String = "",
  val label: String = "",
  val amount: Long = 0L,
  val type: String = "",
  val roundId: String? = null,
  @ServerTimestamp val createdAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class BingoRound(
  @DocumentId val id: String = "",
  val cycle: Long = 0L,
  val stake: Int = 10,
  val maxTickets: Int = 30,
  val playerCount: Int = 0,
  val status: String = "waiting",
  val winnerUid: String = "",
  val winnerName: String = "",
  val winnerTicket: Int = 0,
  val winnerCalls: Int = 0,
  val winnerPrize: Long = 0L,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class RoundTicket(
  @DocumentId val id: String = "",
  val roundId: String = "",
  val ticketNumber: Int = 0,
  val userId: String = "",
  val username: String = "",
  val stake: Int = 10,
  val refunded: Boolean = false,
  @ServerTimestamp val createdAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class PaymentRequest(
  @DocumentId val id: String = "",
  val userId: String = "",
  val username: String = "",
  val phone: String = "",
  val amount: Long = 0L,
  val receiptId: String = "",
  val receiptLink: String = "",
  val status: String = "pending",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class WithdrawalRequest(
  @DocumentId val id: String = "",
  val userId: String = "",
  val username: String = "",
  val phone: String = "",
  val amount: Long = 0L,
  val telebirrAccount: String = "",
  val status: String = "pending",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

data class ActiveTicketSession(
  val roundId: String,
  val cycle: Long,
  val stake: Int,
  val ticketNumber: Int,
  val gameStartMs: Long,
)
