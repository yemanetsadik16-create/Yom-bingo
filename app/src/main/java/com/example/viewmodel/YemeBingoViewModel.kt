package com.example.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.repository.BingoRepository
import com.example.model.ActiveTicketSession
import com.example.model.BingoRound
import com.example.model.PaymentRequest
import com.example.model.UiState
import com.example.model.UserProfile
import com.example.model.WalletTransaction
import com.example.model.WithdrawalRequest
import com.example.util.BingoEngine
import com.example.util.BingoGoal
import com.example.util.RoomConfig
import com.example.util.RoundCycle
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class GameOverResult(
  val title: String,
  val message: String,
  val isWinner: Boolean,
  val winningCells: List<Pair<Int, Int>> = emptyList(),
)

data class LiveGameUiState(
  val session: ActiveTicketSession,
  val round: BingoRound?,
  val card: List<List<Int>>,
  val goal: BingoGoal,
  val calledNumbers: List<Int>,
  val markedCells: Set<Pair<Int, Int>>,
  val autoMark: Boolean,
  val soundOn: Boolean,
  val statusLabel: String,
  val prizePoolEtb: Long,
  val gameOver: GameOverResult?,
)

@OptIn(ExperimentalCoroutinesApi::class)
class YemeBingoViewModel(
  private val repository: BingoRepository,
  private val currentUserId: String,
  private val userDisplayName: String?,
  private val userEmail: String?,
  private val preferredUsername: String? = null,
  private val preferredPhone: String? = null,
) : ViewModel() {

  val isAdmin: Boolean = repository.isCurrentUserAdmin()

  private val _nowMs = MutableStateFlow(System.currentTimeMillis())
  val nowMs: StateFlow<Long> = _nowMs.asStateFlow()

  val roundCycle: StateFlow<RoundCycle> = _nowMs
    .map { BingoEngine.currentCycle(it) }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), BingoEngine.currentCycle(System.currentTimeMillis()))

  private val _currentCycleId = MutableStateFlow(BingoEngine.currentCycle(System.currentTimeMillis()).cycle)

  val userProfile: StateFlow<UiState<UserProfile?>> = repository.observeUserProfile(currentUserId)
    .unwrapFirestoreErrors("Failed to observe user profile")
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UiState.Loading)

  val transactions: StateFlow<List<WalletTransaction>> = repository.observeTransactions(currentUserId)
    .catch { e -> Log.w(TAG, "Error observing transactions", unwrap(e)); emit(emptyList()) }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

  val myPayments: StateFlow<List<PaymentRequest>> = repository.observeMyPaymentRequests(currentUserId)
    .catch { e -> Log.w(TAG, "Error observing payments", unwrap(e)); emit(emptyList()) }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

  val myWithdrawals: StateFlow<List<WithdrawalRequest>> = repository.observeMyWithdrawalRequests(currentUserId)
    .catch { e -> Log.w(TAG, "Error observing withdrawals", unwrap(e)); emit(emptyList()) }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

  val roundsForCycle: StateFlow<Map<Int, BingoRound>> = _currentCycleId
    .flatMapLatest { cycle ->
      repository.observeRoundsForCycle(cycle)
        .map { list -> list.associateBy { it.stake } }
        .catch { e -> Log.w(TAG, "Error observing rounds", unwrap(e)); emit(emptyMap()) }
    }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyMap())

  private val _selectedRoom = MutableStateFlow<RoomConfig?>(null)
  val selectedRoom: StateFlow<RoomConfig?> = _selectedRoom.asStateFlow()

  private val _selectedTicketNumber = MutableStateFlow(1)
  val selectedTicketNumber: StateFlow<Int> = _selectedTicketNumber.asStateFlow()

  val takenTicketsInPickedRoom: StateFlow<Set<Int>> = combine(_selectedRoom, _currentCycleId) { room, cycle ->
    room?.let { BingoEngine.roundId(it.stake, cycle) }
  }.flatMapLatest { roundId ->
    if (roundId == null) {
      flowOf(emptySet())
    } else {
      repository.observeTicketsForRound(roundId)
        .map { tickets -> tickets.map { it.ticketNumber }.toSet() }
        .catch { e -> Log.w(TAG, "Error observing tickets", unwrap(e)); emit(emptySet()) }
    }
  }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptySet())

  private val _activeTicket = MutableStateFlow<ActiveTicketSession?>(null)
  val activeTicket: StateFlow<ActiveTicketSession?> = _activeTicket.asStateFlow()

  private val activeRoundDoc: StateFlow<BingoRound?> = _activeTicket
    .flatMapLatest { session ->
      if (session == null) flowOf(null)
      else repository.observeRound(session.roundId)
        .catch { e -> Log.w(TAG, "Error observing active round", unwrap(e)); emit(null) }
    }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), null)

  private val _markedCells = MutableStateFlow(setOf(2 to 2))
  private val _autoMark = MutableStateFlow(false)
  private val _soundEnabled = MutableStateFlow(true)
  private val _localGameOver = MutableStateFlow<GameOverResult?>(null)
  private val _leftRoundId = MutableStateFlow<String?>(null)

  private val _navigateToGameEvent = MutableStateFlow<String?>(null)
  val navigateToGameEvent: StateFlow<String?> = _navigateToGameEvent.asStateFlow()

  private val _isBusy = MutableStateFlow(false)
  val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

  private val _feedbackMessage = MutableStateFlow<String?>(null)
  val feedbackMessage: StateFlow<String?> = _feedbackMessage.asStateFlow()

  private val _modalDialogMessage = MutableStateFlow<String?>(null)
  val modalDialogMessage: StateFlow<String?> = _modalDialogMessage.asStateFlow()

  private val _gameInlineError = MutableStateFlow<String?>(null)
  val gameInlineError: StateFlow<String?> = _gameInlineError.asStateFlow()

  val liveGameState: StateFlow<LiveGameUiState?> = combine(
    _activeTicket,
    activeRoundDoc,
    _nowMs,
    _markedCells,
    _autoMark,
    _soundEnabled,
    _localGameOver,
  ) { arr ->
    val session = arr[0] as ActiveTicketSession? ?: return@combine null
    val round = arr[1] as BingoRound?
    val now = arr[2] as Long
    @Suppress("UNCHECKED_CAST")
    val marked = arr[3] as Set<Pair<Int, Int>>
    val autoMark = arr[4] as Boolean
    val soundOn = arr[5] as Boolean
    val localOver = arr[6] as GameOverResult?

    val elapsed = now - session.gameStartMs - BingoEngine.READY_LEAD_MS
    val callCount = if (elapsed < 0L) 0 else ((elapsed / BingoEngine.BALL_INTERVAL_MS) + 1L).toInt().coerceIn(0, 75)
    val sequence = BingoEngine.ballSequenceForRound(session.cycle, session.stake)
    val called = sequence.take(callCount)
    val card = BingoEngine.cardFor(session.ticketNumber, seed = 1)
    val goal = BingoEngine.goalForRound(session.cycle, session.stake)
    val playerCount = round?.playerCount ?: 1
    val prizePool = BingoEngine.prizeForPool(playerCount, session.stake)

    val remoteGameOver = if (round != null && round.status == "finished" && localOver == null) {
      if (round.winnerUid == currentUserId) {
        GameOverResult(
          title = "BINGO! You won",
          message = "Ticket #${round.winnerTicket} won in ${round.winnerCalls} calls. Reward: ${round.winnerPrize} ETB added to your wallet.",
          isWinner = true,
        )
      } else {
        GameOverResult(
          title = "Game over",
          message = "${round.winnerName} won with ticket #${round.winnerTicket} after ${round.winnerCalls} calls. Reward: ${round.winnerPrize} ETB.",
          isWinner = false,
        )
      }
    } else if (callCount >= 75 && localOver == null && round?.status != "finished") {
      GameOverResult(
        title = "Game over",
        message = "All 75 balls called. No winner in this round.",
        isWinner = false,
      )
    } else {
      localOver
    }

    val statusLabel = when {
      elapsed < 0L -> "Get ready… ${((-elapsed + 999L) / 1000L)}s"
      else -> "${session.stake} ETB Room"
    }

    LiveGameUiState(
      session = session,
      round = round,
      card = card,
      goal = goal,
      calledNumbers = called,
      markedCells = marked,
      autoMark = autoMark,
      soundOn = soundOn,
      statusLabel = statusLabel,
      prizePoolEtb = prizePool,
      gameOver = remoteGameOver,
    )
  }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), null)

  // Admin flows (only subscribed when isAdmin == true)
  val adminPayments: StateFlow<List<PaymentRequest>> = if (isAdmin) {
    repository.observeAllPayments()
      .catch { e -> Log.w(TAG, "Admin payments error", unwrap(e)); emit(emptyList()) }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())
  } else {
    MutableStateFlow(emptyList())
  }

  val adminWithdrawals: StateFlow<List<WithdrawalRequest>> = if (isAdmin) {
    repository.observeAllWithdrawals()
      .catch { e -> Log.w(TAG, "Admin withdrawals error", unwrap(e)); emit(emptyList()) }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())
  } else {
    MutableStateFlow(emptyList())
  }

  val adminUsers: StateFlow<List<UserProfile>> = if (isAdmin) {
    repository.observeAllUsers()
      .catch { e -> Log.w(TAG, "Admin users error", unwrap(e)); emit(emptyList()) }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())
  } else {
    MutableStateFlow(emptyList())
  }

  private val refundAttemptedRoundIds = mutableSetOf<String>()

  init {
    viewModelScope.launch {
      repository.ensureUserProfile(
        displayName = userDisplayName,
        email = userEmail,
        preferredUsername = preferredUsername,
        preferredPhone = preferredPhone,
      ).onFailure { err ->
        _feedbackMessage.value = err.localizedMessage ?: "Failed to initialize player profile."
      }
    }

    // Clock ticker for lobby countdowns, automatic round launch, and 5-second ball calling
    viewModelScope.launch {
      while (isActive) {
        val now = System.currentTimeMillis()
        _nowMs.value = now
        val cycle = BingoEngine.currentCycle(now)
        if (_currentCycleId.value != cycle.cycle) {
          _currentCycleId.value = cycle.cycle
          if (_selectedRoom.value != null && now >= cycle.gameStartMs) {
            _selectedRoom.value = null
          }
        }
        // Check if picked room's lobby closed while picking
        if (_selectedRoom.value != null && !cycle.isLobbyOpen) {
          _selectedRoom.value = null
        }

        // Handle active ticket transitions (auto-refund solo or auto-enter game)
        val ticket = _activeTicket.value
        if (ticket != null) {
          val roundEndMs = ticket.gameStartMs + BingoEngine.READY_LEAD_MS + 75 * BingoEngine.BALL_INTERVAL_MS + 3_000L
          if (now >= roundEndMs) {
            _activeTicket.value = null
            _localGameOver.value = null
          } else if (now >= ticket.gameStartMs) {
            val round = activeRoundDoc.value
            if (round != null && round.playerCount < 2) {
              if (refundAttemptedRoundIds.add(ticket.roundId)) {
                viewModelScope.launch {
                  repository.refundSoloRound(ticket.roundId, ticket.ticketNumber)
                    .onSuccess { refunded ->
                      _activeTicket.value = null
                      if (refunded > 0L) {
                        _feedbackMessage.value = "Not enough players in the ${ticket.stake} ETB room. ${ticket.stake} ETB refunded."
                      }
                    }
                    .onFailure { err ->
                      Log.w(TAG, "Solo refund error", err)
                    }
                }
              }
            } else if (round != null && round.playerCount >= 2 && _leftRoundId.value != ticket.roundId) {
              if (_navigateToGameEvent.value == null) {
                _navigateToGameEvent.value = ticket.roundId
              }
            }
          }
        }

        // Auto-mark new balls if enabled
        if (_autoMark.value) {
          val state = liveGameState.value
          if (state != null && state.gameOver == null) {
            val currentMarked = _markedCells.value.toMutableSet()
            var changed = false
            for (called in state.calledNumbers) {
              for (c in 0 until 5) {
                for (r in 0 until 5) {
                  if (state.card[c][r] == called && currentMarked.add(c to r)) {
                    changed = true
                  }
                }
              }
            }
            if (changed) {
              _markedCells.value = currentMarked
            }
          }
        }

        delay(if (_activeTicket.value != null) 250L else 500L)
      }
    }
  }

  fun consumeNavigateToGameEvent() {
    _navigateToGameEvent.value = null
  }

  fun consumeFeedback() {
    _feedbackMessage.value = null
  }

  fun dismissModalDialog() {
    _modalDialogMessage.value = null
  }

  fun openRoomPicker(room: RoomConfig) {
    val cycle = roundCycle.value
    if (!cycle.isLobbyOpen) {
      _feedbackMessage.value = "Round already running. Please wait for the next lobby."
      return
    }
    _selectedRoom.value = room
    _selectedTicketNumber.value = 1
  }

  fun closeRoomPicker() {
    _selectedRoom.value = null
  }

  fun selectTicketNumber(number: Int) {
    _selectedTicketNumber.value = number
  }

  fun joinPickedRoom() {
    val room = _selectedRoom.value ?: return
    val ticketNum = _selectedTicketNumber.value
    val cycle = roundCycle.value
    if (!cycle.isLobbyOpen) {
      _feedbackMessage.value = "This round has already started."
      _selectedRoom.value = null
      return
    }
    if (_activeTicket.value != null) {
      _feedbackMessage.value = "You already joined a room for this round."
      return
    }
    if (ticketNum in takenTicketsInPickedRoom.value) {
      _feedbackMessage.value = "Ticket #$ticketNum is taken. Choose another."
      return
    }

    val roundId = BingoEngine.roundId(room.stake, cycle.cycle)
    viewModelScope.launch {
      _isBusy.value = true
      repository.joinRound(roundId, cycle.cycle, room, ticketNum)
        .onSuccess {
          _activeTicket.value = ActiveTicketSession(
            roundId = roundId,
            cycle = cycle.cycle,
            stake = room.stake,
            ticketNumber = ticketNum,
            gameStartMs = cycle.gameStartMs,
          )
          _markedCells.value = setOf(2 to 2)
          _localGameOver.value = null
          _gameInlineError.value = null
          _leftRoundId.value = null
          _selectedRoom.value = null
          _modalDialogMessage.value = "Joined with ticket #$ticketNum! The game starts automatically when the countdown ends."
        }
        .onFailure { err ->
          _feedbackMessage.value = err.localizedMessage ?: "Could not reserve ticket #$ticketNum."
        }
      _isBusy.value = false
    }
  }

  fun toggleCellMark(col: Int, row: Int) {
    if (col == 2 && row == 2) return
    val state = liveGameState.value ?: return
    if (state.gameOver != null) return
    val number = state.card[col][row]
    if (number == 0) return
    if (number !in state.calledNumbers) {
      _gameInlineError.value = "$number has not been called yet."
      return
    }
    _gameInlineError.value = null
    val cell = col to row
    val next = _markedCells.value.toMutableSet()
    if (!next.add(cell)) {
      next.remove(cell)
    }
    _markedCells.value = next
  }

  fun setAutoMark(enabled: Boolean) {
    _autoMark.value = enabled
    if (enabled) {
      val state = liveGameState.value ?: return
      val next = _markedCells.value.toMutableSet()
      for (called in state.calledNumbers) {
        for (c in 0 until 5) {
          for (r in 0 until 5) {
            if (state.card[c][r] == called) next.add(c to r)
          }
        }
      }
      _markedCells.value = next
    }
  }

  fun toggleSound() {
    _soundEnabled.value = !_soundEnabled.value
  }

  fun claimBingo() {
    val state = liveGameState.value ?: return
    if (state.gameOver != null || _isBusy.value) return

    val winCells = BingoEngine.findWinningPattern(state.goal, state.markedCells)
    if (winCells == null) {
      _gameInlineError.value = "Not yet. Mark every called number for: ${state.goal.name}"
      return
    }

    _gameInlineError.value = null
    viewModelScope.launch {
      _isBusy.value = true
      repository.claimBingoWin(
        roundId = state.session.roundId,
        ticketNumber = state.session.ticketNumber,
        callsCount = state.calledNumbers.size.coerceAtLeast(1),
      ).onSuccess { prize ->
        _localGameOver.value = GameOverResult(
          title = "BINGO! You won",
          message = "Ticket #${state.session.ticketNumber} won in ${state.calledNumbers.size} calls. Reward: $prize ETB added to your wallet.",
          isWinner = true,
          winningCells = winCells,
        )
      }.onFailure { err ->
        _gameInlineError.value = err.localizedMessage ?: "Too late: another player already won."
      }
      _isBusy.value = false
    }
  }

  fun leaveGameToRooms() {
    _leftRoundId.value = _activeTicket.value?.roundId
    if (_localGameOver.value != null || activeRoundDoc.value?.status == "finished") {
      _activeTicket.value = null
      _localGameOver.value = null
    }
  }

  fun updateProfile(username: String, phone: String) {
    viewModelScope.launch {
      _isBusy.value = true
      repository.updateUserProfile(username, phone)
        .onSuccess { _feedbackMessage.value = "Profile updated." }
        .onFailure { _feedbackMessage.value = it.localizedMessage ?: "Failed to update profile." }
      _isBusy.value = false
    }
  }

  fun submitDeposit(amountText: String, receiptInput: String, onSuccessReset: () -> Unit) {
    val amount = amountText.trim().toLongOrNull() ?: 0L
    val profile = (userProfile.value as? UiState.Success)?.data
    val username = profile?.username ?: userDisplayName ?: "Player"
    val phone = profile?.phone ?: ""

    viewModelScope.launch {
      _isBusy.value = true
      repository.submitPaymentRequest(amount, receiptInput, username, phone)
        .onSuccess {
          onSuccessReset()
          _modalDialogMessage.value = "Payment received. The admin will review and credit your wallet."
        }
        .onFailure { _feedbackMessage.value = it.localizedMessage ?: "Failed to submit payment." }
      _isBusy.value = false
    }
  }

  fun submitWithdrawal(amountText: String, telebirrAccount: String, onSuccessReset: () -> Unit) {
    val amount = amountText.trim().toLongOrNull() ?: 0L
    val profile = (userProfile.value as? UiState.Success)?.data
    val username = profile?.username ?: userDisplayName ?: "Player"
    val phone = profile?.phone ?: ""

    viewModelScope.launch {
      _isBusy.value = true
      repository.submitWithdrawalRequest(amount, telebirrAccount, username, phone)
        .onSuccess {
          onSuccessReset()
          _modalDialogMessage.value = "Withdrawal requested. $amount ETB will be sent to ${BingoEngine.cleanPhone(telebirrAccount)}."
        }
        .onFailure { _feedbackMessage.value = it.localizedMessage ?: "Withdrawal request failed." }
      _isBusy.value = false
    }
  }

  // Admin Actions
  fun approvePayment(requestId: String, amount: Long) {
    viewModelScope.launch {
      _isBusy.value = true
      repository.approvePaymentRequest(requestId, amount)
        .onSuccess { _feedbackMessage.value = "Payment approved ($amount ETB credited)." }
        .onFailure { _feedbackMessage.value = it.localizedMessage ?: "Approval failed." }
      _isBusy.value = false
    }
  }

  fun declinePayment(requestId: String) {
    viewModelScope.launch {
      _isBusy.value = true
      repository.declinePaymentRequest(requestId)
        .onSuccess { _feedbackMessage.value = "Payment declined." }
        .onFailure { _feedbackMessage.value = it.localizedMessage ?: "Decline failed." }
      _isBusy.value = false
    }
  }

  fun markWithdrawalPaid(requestId: String) {
    viewModelScope.launch {
      _isBusy.value = true
      repository.markWithdrawalPaid(requestId)
        .onSuccess { _feedbackMessage.value = "Withdrawal marked paid." }
        .onFailure { _feedbackMessage.value = it.localizedMessage ?: "Action failed." }
      _isBusy.value = false
    }
  }

  fun declineAndRefundWithdrawal(requestId: String) {
    viewModelScope.launch {
      _isBusy.value = true
      repository.declineAndRefundWithdrawal(requestId)
        .onSuccess { _feedbackMessage.value = "Withdrawal declined & refunded to wallet." }
        .onFailure { _feedbackMessage.value = it.localizedMessage ?: "Refund failed." }
      _isBusy.value = false
    }
  }

  fun adminAdjustUserWallet(targetUid: String, delta: Long, label: String) {
    viewModelScope.launch {
      _isBusy.value = true
      repository.adminAdjustWallet(targetUid, delta, label)
        .onSuccess { _feedbackMessage.value = "Wallet updated." }
        .onFailure { _feedbackMessage.value = it.localizedMessage ?: "Wallet adjustment failed." }
      _isBusy.value = false
    }
  }

  fun adminDeleteUser(targetUid: String) {
    viewModelScope.launch {
      _isBusy.value = true
      repository.adminDeleteUser(targetUid)
        .onSuccess { _feedbackMessage.value = "User deleted." }
        .onFailure { _feedbackMessage.value = it.localizedMessage ?: "Delete failed." }
      _isBusy.value = false
    }
  }

  private fun <T> Flow<T>.unwrapFirestoreErrors(fallbackMsg: String): Flow<UiState<T>> =
    this.map<T, UiState<T>> { UiState.Success(it) }
      .catch { error ->
        val firestoreError = generateSequence(error) { it.cause }
          .filterIsInstance<FirebaseFirestoreException>()
          .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val rootError = firestoreError ?: error
        Log.w(TAG, fallbackMsg, rootError)
        emit(UiState.Error(rootError.localizedMessage ?: fallbackMsg))
      }

  private fun unwrap(error: Throwable): Throwable =
    generateSequence(error) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull() ?: error

  private companion object {
    const val TAG = "YemeBingoVM"
    const val STOP_TIMEOUT_MS = 5000L
  }
}
