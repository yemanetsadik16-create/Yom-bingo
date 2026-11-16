package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.util.BingoEngine
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class BingoRepositoryRuleTest : FirestoreEmulatorTestBase() {

  @Test
  fun ensureUserProfile_createsProfileWithZeroWallet() = runBlocking {
    val uid = signInTestUser("alice_${UUID.randomUUID()}@test.com")
    val repo = BingoRepository(firestore, auth)

    val result = withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.ensureUserProfile("Alice", "alice@test.com")
    }
    assertTrue(result.isSuccess)
    val profile = result.getOrThrow()
    assertEquals(uid, profile.userId)
    assertEquals(0L, profile.wallet)
  }

  @Test
  fun endToEndRoomLifecycle_andNegativeSecurityChecks() = runBlocking {
    val suffix = UUID.randomUUID().toString().take(8)
    val aliceEmail = "alice_$suffix@test.com"
    val bobEmail = "bob_$suffix@test.com"

    val aliceUid = signInTestUser(aliceEmail)
    val aliceRepo = BingoRepository(firestore, auth)
    withTimeout(DEFAULT_TIMEOUT_MS) { aliceRepo.ensureUserProfile("Alice", aliceEmail).getOrThrow() }

    val bobUid = signInTestUser(bobEmail)
    val bobRepo = BingoRepository(firestore, auth)
    withTimeout(DEFAULT_TIMEOUT_MS) { bobRepo.ensureUserProfile("Bob", bobEmail).getOrThrow() }

    signInTestUser(BingoEngine.ADMIN_EMAIL, verifyEmail = true)
    val adminRepo = BingoRepository(firestore, auth)
    withTimeout(DEFAULT_TIMEOUT_MS) {
      adminRepo.adminAdjustWallet(aliceUid, 100L, "Admin credit").getOrThrow()
      adminRepo.adminAdjustWallet(bobUid, 100L, "Admin credit").getOrThrow()
    }

    val cycle = 1_000_000L + (System.currentTimeMillis() % 500_000L)
    val room = BingoEngine.ROOMS.first() // 10 ETB
    val roundId = BingoEngine.roundId(room.stake, cycle)

    // Alice joins ticket #12
    signInTestUser(aliceEmail)
    withTimeout(DEFAULT_TIMEOUT_MS) {
      aliceRepo.joinRound(roundId, cycle, room, 12).getOrThrow()
    }

    // Bob joins ticket #19
    signInTestUser(bobEmail)
    withTimeout(DEFAULT_TIMEOUT_MS) {
      bobRepo.joinRound(roundId, cycle, room, 19).getOrThrow()
    }

    // Observe rounds & tickets in real time
    val rounds = withTimeout(FLOW_TIMEOUT_MS) {
      bobRepo.observeRoundsForCycle(cycle).first { list -> list.any { it.id == roundId && it.playerCount == 2 } }
    }
    assertEquals(2, rounds.first { it.id == roundId }.playerCount)

    val tickets = withTimeout(FLOW_TIMEOUT_MS) {
      bobRepo.observeTicketsForRound(roundId).first { list -> list.size == 2 }
    }
    assertTrue(tickets.map { it.ticketNumber }.containsAll(listOf(12, 19)))

    // Alice wins (pool = 20, prize = 16)
    signInTestUser(aliceEmail)
    val prize = withTimeout(DEFAULT_TIMEOUT_MS) {
      aliceRepo.claimBingoWin(roundId, 12, 18).getOrThrow()
    }
    assertEquals(16L, prize)

    val updatedAlice = withTimeout(FLOW_TIMEOUT_MS) {
      aliceRepo.observeUserProfile(aliceUid).first { it != null && it.wallet == 106L }
    }
    assertNotNull(updatedAlice)
    assertEquals(106L, updatedAlice!!.wallet)

    // Bob cannot claim win on already-finished round
    signInTestUser(bobEmail)
    val secondClaim = withTimeout(DEFAULT_TIMEOUT_MS) {
      bobRepo.claimBingoWin(roundId, 19, 20)
    }
    assertTrue(secondClaim.isFailure)

    // Cross-user profile read is rejected
    val crossReadError = runCatching {
      withTimeout(FLOW_TIMEOUT_MS) {
        bobRepo.observeUserProfile(aliceUid).first()
      }
    }.exceptionOrNull()
    assertNotNull(crossReadError)
    assertEquals(
      FirebaseFirestoreException.Code.PERMISSION_DENIED,
      crossReadError?.unwrapFirestoreException()?.code
    )

    // Unauthenticated observeRoundsForCycle fails with fail-fast or PERMISSION_DENIED
    auth.signOut()
    val unauthError = runCatching {
      withTimeout(FLOW_TIMEOUT_MS) {
        aliceRepo.observeRoundsForCycle(cycle).first()
      }
    }.exceptionOrNull()
    assertNotNull(unauthError)
  }

  @Test
  fun soloRoundRefund_refundsStakeOnce() = runBlocking {
    val suffix = UUID.randomUUID().toString().take(8)
    val aliceEmail = "solo_$suffix@test.com"
    val aliceUid = signInTestUser(aliceEmail)
    val aliceRepo = BingoRepository(firestore, auth)
    withTimeout(DEFAULT_TIMEOUT_MS) { aliceRepo.ensureUserProfile("Solo", aliceEmail).getOrThrow() }

    signInTestUser(BingoEngine.ADMIN_EMAIL, verifyEmail = true)
    val adminRepo = BingoRepository(firestore, auth)
    withTimeout(DEFAULT_TIMEOUT_MS) {
      adminRepo.adminAdjustWallet(aliceUid, 50L, "Seed").getOrThrow()
    }

    val cycle = 2_000_000L + (System.currentTimeMillis() % 500_000L)
    val room = BingoEngine.ROOMS[1] // 20 ETB
    val roundId = BingoEngine.roundId(room.stake, cycle)

    signInTestUser(aliceEmail)
    withTimeout(DEFAULT_TIMEOUT_MS) {
      aliceRepo.joinRound(roundId, cycle, room, 4).getOrThrow()
    }
    val refunded = withTimeout(DEFAULT_TIMEOUT_MS) {
      aliceRepo.refundSoloRound(roundId, 4).getOrThrow()
    }
    assertEquals(20L, refunded)

    val profile = withTimeout(FLOW_TIMEOUT_MS) {
      aliceRepo.observeUserProfile(aliceUid).first { it != null && it.wallet == 50L }
    }
    assertEquals(50L, profile!!.wallet)
  }

  private companion object {
    const val DEFAULT_TIMEOUT_MS = 10000L
    const val FLOW_TIMEOUT_MS = 5000L
  }
}
