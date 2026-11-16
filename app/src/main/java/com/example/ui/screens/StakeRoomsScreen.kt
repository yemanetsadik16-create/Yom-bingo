package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ActiveTicketSession
import com.example.model.BingoRound
import com.example.ui.components.BingoCardGrid
import com.example.ui.components.WalletBannerCard
import com.example.ui.i18n.AppStrings
import com.example.ui.i18n.LocalAppLang
import com.example.ui.theme.GoldBright
import com.example.ui.theme.GoldDeep
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.InkLight
import com.example.ui.theme.InkMuted
import com.example.ui.theme.InkOnGold
import com.example.ui.theme.PlayfairDisplayFontFamily
import com.example.ui.theme.RoomCardBg
import com.example.ui.theme.RoomCardBorder
import com.example.ui.theme.RoomChipBg
import com.example.ui.theme.RoomJoinDisabled
import com.example.ui.theme.RoomJoinOrange
import com.example.ui.theme.RoomPoolBg
import com.example.ui.theme.RoomSubText
import com.example.ui.theme.RoseError
import com.example.ui.theme.RoyalCellDefault
import com.example.ui.theme.TakenTicketBg
import com.example.ui.theme.VioletOutline
import com.example.ui.theme.VioletPrimary
import com.example.util.BingoEngine
import com.example.util.RoomConfig
import com.example.util.RoundCycle

@Composable
fun StakeRoomsScreen(
  walletEtb: Long,
  roundCycle: RoundCycle,
  roundsByStake: Map<Int, BingoRound>,
  activeTicket: ActiveTicketSession?,
  selectedRoom: RoomConfig?,
  selectedTicketNumber: Int,
  takenTickets: Set<Int>,
  isBusy: Boolean,
  onOpenDeposit: () -> Unit,
  onPickRoom: (RoomConfig) -> Unit,
  onClosePicker: () -> Unit,
  onSelectTicket: (Int) -> Unit,
  onConfirmJoin: () -> Unit,
  onEnterRunningGame: () -> Unit,
  modifier: Modifier = Modifier,
) {
  if (selectedRoom != null) {
    BackHandler { onClosePicker() }
    TicketPickerView(
      room = selectedRoom,
      walletEtb = walletEtb,
      roundCycle = roundCycle,
      selectedTicketNumber = selectedTicketNumber,
      takenTickets = takenTickets,
      isBusy = isBusy,
      onBack = onClosePicker,
      onSelectTicket = onSelectTicket,
      onConfirmJoin = onConfirmJoin,
      onDepositClick = onOpenDeposit,
      modifier = modifier,
    )
  } else {
    val lang = LocalAppLang.current
    Column(
      modifier = modifier
        .fillMaxSize()
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 16.dp, vertical = 14.dp)
        .widthIn(max = 520.dp),
    ) {
      WalletBannerCard(
        balanceEtb = walletEtb,
        onDepositClick = onOpenDeposit,
      )

      if (activeTicket != null && !roundCycle.isLobbyOpen) {
        val activeRound = roundsByStake[activeTicket.stake]
        if (activeRound == null || activeRound.playerCount >= 2) {
          Spacer(modifier = Modifier.height(12.dp))
          Button(
            onClick = onEnterRunningGame,
            colors = ButtonDefaults.buttonColors(
              containerColor = GoldPrimary,
              contentColor = InkOnGold,
            ),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
              .fillMaxWidth()
              .height(50.dp)
              .testTag("enter_live_game_button"),
          ) {
            Text(
              text = "Rejoin Live Game (${activeTicket.stake} ETB · #${activeTicket.ticketNumber})",
              fontWeight = FontWeight.Bold,
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      BingoEngine.ROOMS.forEach { room ->
        val round = roundsByStake[room.stake]
        val playerCount = round?.playerCount ?: 0
        val roundId = BingoEngine.roundId(room.stake, roundCycle.cycle)
        val joinedThisRoom = activeTicket?.roundId == roundId
        val prize = BingoEngine.prizeForPool(playerCount, room.stake)
        val canJoin = roundCycle.isLobbyOpen && activeTicket == null && playerCount < room.maxTickets

        val statusLabel = when {
          joinedThisRoom -> "Joined #${activeTicket!!.ticketNumber}"
          roundCycle.isLobbyOpen -> "⏳ ${BingoEngine.formatMmSs(roundCycle.msUntilGameStart)}"
          else -> AppStrings.runningLabel(lang)
        }

        Surface(
          modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 14.dp),
          shape = RoundedCornerShape(18.dp),
          color = RoomCardBg,
          border = BorderStroke(1.dp, RoomCardBorder),
        ) {
          Column(modifier = Modifier.padding(14.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Box(
                modifier = Modifier
                  .size(46.dp)
                  .clip(RoundedCornerShape(12.dp))
                  .background(Color(room.badgeColorHex)),
                contentAlignment = Alignment.Center,
              ) {
                Text(
                  text = room.stake.toString(),
                  fontWeight = FontWeight.Bold,
                  fontSize = 18.sp,
                  color = Color(0xFF1A1000),
                )
              }

              Spacer(modifier = Modifier.width(12.dp))

              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = room.name,
                  fontWeight = FontWeight.Bold,
                  fontSize = 16.sp,
                  color = Color(0xFFDFE9EC),
                )
                Text(
                  text = AppStrings.stakePerTicket(lang, room.stake),
                  fontSize = 12.sp,
                  color = RoomSubText,
                )
              }

              Surface(
                shape = RoundedCornerShape(8.dp),
                color = RoomChipBg,
              ) {
                Text(
                  text = statusLabel,
                  fontSize = 13.sp,
                  color = if (joinedThisRoom) GoldBright else Color(0xFF7FA0A6),
                  fontWeight = if (joinedThisRoom) FontWeight.Bold else FontWeight.Medium,
                  modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                )
              }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Surface(
              modifier = Modifier.fillMaxWidth(),
              shape = RoundedCornerShape(12.dp),
              color = RoomPoolBg,
            ) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Icon(
                    imageVector = Icons.Rounded.Groups,
                    contentDescription = null,
                    tint = RoomSubText,
                    modifier = Modifier.size(18.dp),
                  )
                  Spacer(modifier = Modifier.width(6.dp))
                  Text(
                    text = AppStrings.playersCount(lang, playerCount, room.maxTickets),
                    fontSize = 14.sp,
                    color = RoomSubText,
                  )
                }
                Text(
                  text = AppStrings.prizeLabel(lang, prize),
                  fontSize = 14.sp,
                  fontWeight = FontWeight.Bold,
                  color = Color(0xFFD4A21C),
                )
              }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
              onClick = { onPickRoom(room) },
              enabled = canJoin,
              colors = ButtonDefaults.buttonColors(
                containerColor = RoomJoinOrange,
                contentColor = Color.White,
                disabledContainerColor = RoomJoinDisabled,
                disabledContentColor = Color(0xFF9A8076),
              ),
              shape = RoundedCornerShape(14.dp),
              modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("join_room_${room.stake}"),
            ) {
              Text(
                text = if (joinedThisRoom) AppStrings.joinedBadge(lang) else AppStrings.joinRoom(lang),
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.6.sp,
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun TicketPickerView(
  room: RoomConfig,
  walletEtb: Long,
  roundCycle: RoundCycle,
  selectedTicketNumber: Int,
  takenTickets: Set<Int>,
  isBusy: Boolean,
  onBack: () -> Unit,
  onSelectTicket: (Int) -> Unit,
  onConfirmJoin: () -> Unit,
  onDepositClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val lang = LocalAppLang.current
  val previewCard = remember(selectedTicketNumber) { BingoEngine.cardFor(selectedTicketNumber, seed = 1) }
  val isSelectedTaken = selectedTicketNumber in takenTickets
  val lowBalance = walletEtb < room.stake.toLong()

  Column(
    modifier = modifier
      .fillMaxSize()
      .verticalScroll(rememberScrollState())
      .padding(horizontal = 16.dp, vertical = 14.dp)
      .widthIn(max = 520.dp),
  ) {
    OutlinedButton(
      onClick = onBack,
      border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.5f)),
      colors = ButtonDefaults.outlinedButtonColors(contentColor = GoldBright),
      shape = RoundedCornerShape(12.dp),
      modifier = Modifier
        .fillMaxWidth()
        .height(48.dp)
        .testTag("picker_back_button"),
    ) {
      Icon(imageVector = Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = null)
      Spacer(modifier = Modifier.width(6.dp))
      Text(text = AppStrings.back(lang), fontWeight = FontWeight.Bold)
    }

    Spacer(modifier = Modifier.height(14.dp))

    Text(
      text = "${room.name} · ${room.stake} ETB · starts in ${BingoEngine.formatMmSs(roundCycle.msUntilGameStart)}",
      fontFamily = PlayfairDisplayFontFamily,
      fontWeight = FontWeight.SemiBold,
      fontSize = 20.sp,
      color = GoldBright,
    )

    Spacer(modifier = Modifier.height(10.dp))

    LazyVerticalGrid(
      columns = GridCells.Adaptive(minSize = 44.dp),
      contentPadding = PaddingValues(2.dp),
      horizontalArrangement = Arrangement.spacedBy(6.dp),
      verticalArrangement = Arrangement.spacedBy(6.dp),
      modifier = Modifier
        .fillMaxWidth()
        .heightIn(min = 160.dp, max = 250.dp),
    ) {
      items(room.maxTickets) { index ->
        val ticketNum = index + 1
        val isTaken = ticketNum in takenTickets
        val isSelected = ticketNum == selectedTicketNumber

        val bgBrush = when {
          isSelected -> Brush.linearGradient(listOf(GoldBright, GoldDeep))
          isTaken -> Brush.linearGradient(listOf(TakenTicketBg, TakenTicketBg))
          else -> Brush.linearGradient(listOf(RoyalCellDefault, RoyalCellDefault))
        }
        val borderColor = when {
          isSelected -> GoldBright
          isTaken -> Color(0xFFC0392B)
          else -> VioletOutline
        }
        val textColor = when {
          isSelected -> InkOnGold
          isTaken -> RoseError
          else -> InkLight
        }

        Box(
          modifier = Modifier
            .height(40.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(bgBrush)
            .border(1.dp, borderColor, RoundedCornerShape(8.dp))
            .clickable { onSelectTicket(ticketNum) }
            .testTag("ticket_num_$ticketNum"),
          contentAlignment = Alignment.Center,
        ) {
          Text(
            text = ticketNum.toString(),
            fontSize = 13.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = textColor,
            textDecoration = if (isTaken) TextDecoration.LineThrough else TextDecoration.None,
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(14.dp))

    Text(
      text = "Card preview · #$selectedTicketNumber",
      fontFamily = PlayfairDisplayFontFamily,
      fontWeight = FontWeight.SemiBold,
      fontSize = 19.sp,
      color = GoldBright,
    )
    Spacer(modifier = Modifier.height(8.dp))

    BingoCardGrid(
      card = previewCard,
      markedCells = emptySet(),
      winningCells = emptyList(),
      isInteractive = false,
    )

    Spacer(modifier = Modifier.height(12.dp))
    Text(
      text = "Stake ${room.stake} ETB is taken from your wallet ($walletEtb ETB).",
      style = MaterialTheme.typography.bodyMedium,
      color = InkMuted,
      textAlign = TextAlign.Center,
      modifier = Modifier.fillMaxWidth(),
    )

    if (isSelectedTaken) {
      Spacer(modifier = Modifier.height(8.dp))
      Text(
        text = "Ticket #$selectedTicketNumber is taken. Choose another.",
        color = RoseError,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
      )
    }

    Spacer(modifier = Modifier.height(12.dp))

    if (lowBalance) {
      Button(
        onClick = onDepositClick,
        colors = ButtonDefaults.buttonColors(
          containerColor = GoldPrimary,
          contentColor = InkOnGold,
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
          .fillMaxWidth()
          .height(52.dp)
          .testTag("picker_deposit_button"),
      ) {
        Text(text = AppStrings.depositMoney(lang), fontSize = 17.sp, fontWeight = FontWeight.Bold)
      }
    } else {
      Button(
        onClick = onConfirmJoin,
        enabled = !isBusy && !isSelectedTaken,
        colors = ButtonDefaults.buttonColors(
          containerColor = VioletPrimary,
          contentColor = Color.White,
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
          .fillMaxWidth()
          .height(52.dp)
          .testTag("confirm_join_button"),
      ) {
        Text(
          text = if (isBusy) "Joining…" else "Join (${room.stake} ETB)",
          fontSize = 18.sp,
          fontWeight = FontWeight.Bold,
        )
      }
    }
  }
}
