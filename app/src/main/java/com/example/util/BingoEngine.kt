package com.example.util

import kotlin.math.floor

data class RoomConfig(
  val stake: Int,
  val name: String,
  val maxTickets: Int,
  val badgeColorHex: Long,
)

data class RoundCycle(
  val nowMs: Long,
  val cycle: Long,
  val cycleStartMs: Long,
  val gameStartMs: Long,
) {
  val isLobbyOpen: Boolean get() = nowMs < gameStartMs
  val nextOpenMs: Long get() = cycleStartMs + BingoEngine.CYCLE_DURATION_MS
  val msUntilGameStart: Long get() = (gameStartMs - nowMs).coerceAtLeast(0L)
  val msUntilNextCycle: Long get() = (nextOpenMs - nowMs).coerceAtLeast(0L)
}

data class BingoGoal(
  val name: String,
  val patterns: List<List<Pair<Int, Int>>>,
)

object BingoEngine {
  const val PAY_MIN_BIRR = 30L
  const val TELEBIRR_MERCHANT_NAME = "Yemane"
  const val TELEBIRR_MERCHANT_NUMBER = "0939383425"
  const val ADMIN_EMAIL = "desbelebisrat12@gmail.com"

  const val CYCLE_DURATION_MS = 480_000L
  const val LOBBY_WAIT_MS = 90_000L
  const val BALL_INTERVAL_MS = 5_000L
  const val READY_LEAD_MS = 5_000L
  const val PRIZE_PAYOUT_RATE = 0.80

  val ROOMS = listOf(
    RoomConfig(10, "Mini Starter", 30, 0xFF1F9D77),
    RoomConfig(20, "Standard Room", 60, 0xFFB8960F),
    RoomConfig(50, "High Roller Suite", 60, 0xFFC4492A),
    RoomConfig(100, "Mega Addis Jackpot", 100, 0xFFD4A21C),
    RoomConfig(200, "Habesha VIP Club", 150, 0xFFE65100),
  )

  val GOALS: List<BingoGoal> = buildGoals()

  private fun buildGoals(): List<BingoGoal> {
    val lines = mutableListOf<List<Pair<Int, Int>>>()
    for (i in 0 until 5) {
      val row = mutableListOf<Pair<Int, Int>>()
      val col = mutableListOf<Pair<Int, Int>>()
      for (j in 0 until 5) {
        row.add(j to i)
        col.add(i to j)
      }
      lines.add(row)
      lines.add(col)
    }
    val d1 = (0 until 5).map { it to it }
    val d2 = (0 until 5).map { it to (4 - it) }
    lines.add(d1)
    lines.add(d2)

    fun cellsMatching(pred: (Int, Int) -> Boolean): List<Pair<Int, Int>> {
      val out = mutableListOf<Pair<Int, Int>>()
      for (c in 0 until 5) {
        for (r in 0 until 5) {
          if (pred(c, r)) out.add(c to r)
        }
      }
      return out
    }

    return listOf(
      BingoGoal("Any Line", lines),
      BingoGoal("Four Corners", listOf(cellsMatching { c, r -> c % 4 == 0 && r % 4 == 0 })),
      BingoGoal("Both Diagonals", listOf(d1 + d2)),
      BingoGoal("Plus Sign", listOf(cellsMatching { c, r -> c == 2 || r == 2 })),
      BingoGoal("Letter T", listOf(cellsMatching { c, r -> r == 0 || c == 2 })),
      BingoGoal("Full House", listOf(cellsMatching { _, _ -> true })),
    )
  }

  fun currentCycle(nowMs: Long): RoundCycle {
    val c = Math.floorDiv(nowMs, CYCLE_DURATION_MS)
    val start = c * CYCLE_DURATION_MS
    return RoundCycle(
      nowMs = nowMs,
      cycle = c,
      cycleStartMs = start,
      gameStartMs = start + LOBBY_WAIT_MS,
    )
  }

  fun roundId(stake: Int, cycle: Long): String = "c${cycle}_${stake}"

  fun roomForStake(stake: Int): RoomConfig =
    ROOMS.firstOrNull { it.stake == stake } ?: ROOMS.first()

  fun prizeForPool(playerCount: Int, stake: Int): Long =
    floor(playerCount * stake * PRIZE_PAYOUT_RATE).toLong()

  fun formatMmSs(ms: Long): String {
    val totalSec = ((ms.coerceAtLeast(0L) + 999L) / 1000L)
    val min = totalSec / 60L
    val sec = totalSec % 60L
    return "%02d:%02d".format(min, sec)
  }

  fun bingoLetterFor(number: Int): Char = when ((number - 1) / 15) {
    0 -> 'B'
    1 -> 'I'
    2 -> 'N'
    3 -> 'G'
    else -> 'O'
  }

  /**
   * Deterministic Mulberry32 PRNG matching the original JS implementation so
   * every ticket number and round ball sequence is identical across all players.
   */
  private class Mulberry32(seed: Int) {
    private var state: Int = seed

    fun nextDouble(): Double {
      state += 0x6D2B79F5
      var t = imul(state xor (state ushr 15), 1 or state)
      t = t xor (t + imul(t xor (t ushr 7), 61 or t))
      val unsigned = (t xor (t ushr 14)).toLong() and 0xFFFFFFFFL
      return unsigned.toDouble() / 4294967296.0
    }

    private fun imul(a: Int, b: Int): Int = a * b
  }

  private fun shuffleDeterministic(items: MutableList<Int>, rng: Mulberry32): List<Int> {
    for (i in items.size - 1 downTo 1) {
      val j = floor(rng.nextDouble() * (i + 1)).toInt()
      val tmp = items[i]
      items[i] = items[j]
      items[j] = tmp
    }
    return items
  }

  /** Returns 5 columns (B, I, N, G, O), each of 5 rows; center (2, 2) is 0 (FREE). */
  fun cardFor(ticketNumber: Int, seed: Int = 1): List<List<Int>> {
    val rng = Mulberry32(seed * 1000 + ticketNumber * 7919 + 13)
    val cols = MutableList(5) { colIdx ->
      val start = colIdx * 15 + 1
      val pool = (start until start + 15).toMutableList()
      shuffleDeterministic(pool, rng).take(5).toMutableList()
    }
    cols[2][2] = 0
    return cols
  }

  fun ballSequenceForRound(cycle: Long, stake: Int): List<Int> {
    val seed = (cycle * 131L + stake.toLong()).toInt()
    val rng = Mulberry32(seed)
    return shuffleDeterministic((1..75).toMutableList(), rng)
  }

  fun goalForRound(cycle: Long, stake: Int): BingoGoal {
    val idx = ((cycle + stake.toLong()) % GOALS.size + GOALS.size) % GOALS.size
    return GOALS[idx.toInt()]
  }

  fun findWinningPattern(
    goal: BingoGoal,
    marked: Set<Pair<Int, Int>>,
  ): List<Pair<Int, Int>>? =
    goal.patterns.firstOrNull { line -> line.all { cell -> cell in marked } }

  private val RECEIPT_REGEX =
    Regex("^(?:https?://transactioninfo\\.ethiotelecom\\.et/receipt/)?([A-Za-z0-9]{8,14})/?$")

  private val TELEBIRR_PHONE_REGEX = Regex("^(\\+?251|0)9\\d{8}$")

  fun extractTelebirrReceiptId(input: String): String? =
    RECEIPT_REGEX.matchEntire(input.trim())?.groupValues?.getOrNull(1)?.uppercase()

  fun isValidTelebirrPhone(phone: String): Boolean {
    val cleaned = phone.replace(Regex("[\\s-]"), "")
    return TELEBIRR_PHONE_REGEX.matches(cleaned)
  }

  fun cleanPhone(phone: String): String = phone.replace(Regex("[\\s-]"), "")
}
