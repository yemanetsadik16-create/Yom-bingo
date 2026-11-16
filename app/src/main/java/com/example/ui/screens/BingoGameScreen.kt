package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.BingoCardGrid
import com.example.ui.components.CalledNumbersBoard
import com.example.ui.components.GameOverDialog
import com.example.ui.i18n.AppStrings
import com.example.ui.i18n.LocalAppLang
import com.example.ui.theme.GoldBright
import com.example.ui.theme.GoldDeep
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.InkLight
import com.example.ui.theme.InkMuted
import com.example.ui.theme.InkOnGold
import com.example.ui.theme.PlayfairDisplayFontFamily
import com.example.ui.theme.RoseError
import com.example.ui.theme.RoyalCellDefault
import com.example.ui.theme.RoyalSurfaceElevated
import com.example.ui.theme.VioletDeep
import com.example.ui.theme.VioletOutline
import com.example.util.BingoEngine
import com.example.util.BingoVoiceCaller
import com.example.viewmodel.LiveGameUiState

@Composable
fun BingoGameScreen(
  gameState: LiveGameUiState?,
  inlineError: String?,
  isBusy: Boolean,
  onCellClick: (Int, Int) -> Unit,
  onToggleAutoMark: (Boolean) -> Unit,
  onToggleSound: () -> Unit,
  onClaimBingo: () -> Unit,
  onBackToRooms: () -> Unit,
  modifier: Modifier = Modifier,
) {
  BackHandler { onBackToRooms() }
  val lang = LocalAppLang.current

  if (gameState == null) {
    Column(
      modifier = modifier
        .fillMaxSize()
        .padding(24.dp),
      verticalArrangement = Arrangement.Center,
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Text(
        text = AppStrings.noActiveGame(lang),
        style = MaterialTheme.typography.titleLarge,
        color = GoldBright,
        textAlign = TextAlign.Center,
      )
      Spacer(modifier = Modifier.height(16.dp))
      Button(
        onClick = onBackToRooms,
        colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = InkOnGold),
      ) {
        Text(text = AppStrings.backToRooms(lang), fontWeight = FontWeight.Bold)
      }
    }
    return
  }

  val context = LocalContext.current
  val voiceCaller = remember(context) { BingoVoiceCaller(context) }
  DisposableEffect(voiceCaller) {
    onDispose { voiceCaller.shutdown() }
  }

  var lastSpokenCount by remember(gameState.session.roundId) { mutableIntStateOf(gameState.calledNumbers.size) }
  LaunchedEffect(gameState.calledNumbers.size, gameState.soundOn, gameState.gameOver) {
    if (!gameState.soundOn) {
      voiceCaller.stop()
      lastSpokenCount = gameState.calledNumbers.size
    } else if (gameState.gameOver == null && gameState.calledNumbers.size > lastSpokenCount) {
      val latest = gameState.calledNumbers.lastOrNull()
      if (latest != null) {
        voiceCaller.speak("${BingoEngine.bingoLetterFor(latest)}, $latest")
      }
      lastSpokenCount = gameState.calledNumbers.size
    }
  }

  LaunchedEffect(gameState.gameOver?.isWinner) {
    if (gameState.gameOver?.isWinner == true && gameState.soundOn) {
      voiceCaller.speak("Bingo! You win!")
    }
  }

  var shakeCell by remember { mutableStateOf<Pair<Int, Int>?>(null) }
  var shakeTrigger by remember { mutableIntStateOf(0) }

  Column(
    modifier = modifier
      .fillMaxSize()
      .verticalScroll(rememberScrollState())
      .padding(horizontal = 14.dp, vertical = 12.dp)
      .widthIn(max = 520.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Surface(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(10.dp),
      border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.45f)),
      color = RoyalSurfaceElevated,
    ) {
      Row(
        modifier = Modifier
          .background(Brush.horizontalGradient(listOf(VioletDeep, RoyalSurfaceElevated)))
          .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = AppStrings.targetGoal(lang),
          style = MaterialTheme.typography.titleMedium,
          color = InkLight,
        )
        Text(
          text = gameState.goal.name,
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
          color = GoldBright,
        )
      }
    }

    Spacer(modifier = Modifier.height(8.dp))

    Text(
      text = gameState.statusLabel,
      style = MaterialTheme.typography.bodyMedium,
      color = InkMuted,
    )
    Text(
      text = "Reward : ${gameState.prizePoolEtb} ETB",
      fontSize = 19.sp,
      fontWeight = FontWeight.Bold,
      color = GoldBright,
      modifier = Modifier.padding(top = 4.dp, bottom = 10.dp),
    )

    CalledNumbersBoard(calledNumbers = gameState.calledNumbers)

    Spacer(modifier = Modifier.height(14.dp))

    BingoCardGrid(
      card = gameState.card,
      markedCells = gameState.markedCells,
      winningCells = gameState.gameOver?.winningCells ?: emptyList(),
      isInteractive = gameState.gameOver == null,
      shakeCell = shakeCell,
      onCellClick = { col, row ->
        val num = gameState.card[col][row]
        if (num != 0 && num !in gameState.calledNumbers) {
          shakeTrigger++
          shakeCell = col to row
        }
        onCellClick(col, row)
      },
      titleText = "Ticket #${gameState.session.ticketNumber}",
    )

    if (!inlineError.isNullOrBlank()) {
      Spacer(modifier = Modifier.height(8.dp))
      Text(
        text = inlineError,
        color = RoseError,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
      )
    }

    Spacer(modifier = Modifier.height(14.dp))

    Button(
      onClick = onClaimBingo,
      enabled = !isBusy && gameState.gameOver == null,
      shape = CircleShape,
      colors = ButtonDefaults.buttonColors(
        containerColor = GoldPrimary,
        contentColor = InkOnGold,
      ),
      modifier = Modifier
        .fillMaxWidth()
        .height(58.dp)
        .background(
          Brush.linearGradient(listOf(GoldBright, GoldDeep)),
          CircleShape,
        )
        .testTag("claim_bingo_button"),
    ) {
      Text(
        text = AppStrings.bingoBtn(lang),
        fontFamily = PlayfairDisplayFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        letterSpacing = 3.sp,
      )
    }

    Spacer(modifier = Modifier.height(12.dp))

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(
          checked = gameState.autoMark,
          onCheckedChange = onToggleAutoMark,
          colors = CheckboxDefaults.colors(
            checkedColor = GoldPrimary,
            uncheckedColor = VioletOutline,
            checkmarkColor = InkOnGold,
          ),
          modifier = Modifier.testTag("auto_mark_checkbox"),
        )
        Text(
          text = AppStrings.autoMark(lang),
          color = InkLight,
          style = MaterialTheme.typography.bodyLarge,
        )
      }

      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(
          onClick = onToggleSound,
          border = BorderStroke(1.dp, VioletOutline),
          colors = ButtonDefaults.outlinedButtonColors(
            containerColor = RoyalCellDefault,
            contentColor = GoldBright,
          ),
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier.testTag("sound_toggle_button"),
        ) {
          Icon(
            imageVector = if (gameState.soundOn) Icons.AutoMirrored.Rounded.VolumeUp else Icons.AutoMirrored.Rounded.VolumeOff,
            contentDescription = null,
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text(
            text = if (gameState.soundOn) AppStrings.soundOn(lang) else AppStrings.soundOff(lang),
          )
        }

        OutlinedButton(
          onClick = onBackToRooms,
          border = BorderStroke(1.dp, VioletOutline),
          colors = ButtonDefaults.outlinedButtonColors(
            containerColor = RoyalCellDefault,
            contentColor = GoldBright,
          ),
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier.testTag("game_back_button"),
        ) {
          Icon(imageVector = Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = null)
          Spacer(modifier = Modifier.width(4.dp))
          Text(text = AppStrings.back(lang))
        }
      }
    }
  }

  gameState.gameOver?.let { over ->
    GameOverDialog(
      title = over.title,
      message = over.message,
      onBackToRooms = onBackToRooms,
    )
  }
}
