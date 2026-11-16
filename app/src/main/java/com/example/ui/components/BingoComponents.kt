package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.i18n.AppLang
import com.example.ui.i18n.AppStrings
import com.example.ui.i18n.LocalAppLang
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.GoldBorder
import com.example.ui.theme.GoldBright
import com.example.ui.theme.GoldCream
import com.example.ui.theme.GoldDeep
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.InkLight
import com.example.ui.theme.InkMuted
import com.example.ui.theme.InkOnGold
import com.example.ui.theme.PlayfairDisplayFontFamily
import com.example.ui.theme.RoyalBgBottom
import com.example.ui.theme.RoyalBgTop
import com.example.ui.theme.RoyalBoardBg
import com.example.ui.theme.RoyalCellDefault
import com.example.ui.theme.RoyalHeaderMid
import com.example.ui.theme.RoyalHeaderStart
import com.example.ui.theme.RoyalSurfaceDeep
import com.example.ui.theme.RoyalSurfaceElevated
import com.example.ui.theme.VioletDeep
import com.example.ui.theme.VioletLine
import com.example.ui.theme.VioletPrimary
import com.example.util.BingoEngine
import kotlin.math.roundToInt

@Composable
fun RoyalBackground(
  modifier: Modifier = Modifier,
  content: @Composable BoxScope.() -> Unit,
) {
  Box(
    modifier = modifier
      .fillMaxSize()
      .background(
        Brush.verticalGradient(
          colors = listOf(RoyalBgTop, RoyalBgBottom)
        )
      )
      .background(
        Brush.radialGradient(
          colors = listOf(VioletDeep.copy(alpha = 0.35f), Color.Transparent),
          radius = 900f,
        )
      ),
    content = content,
  )
}

@Composable
fun YemeTopBar(
  isDrawerOpen: Boolean,
  currentLang: AppLang,
  onToggleLanguage: () -> Unit,
  onBrandClick: () -> Unit,
  onMenuToggle: () -> Unit,
  modifier: Modifier = Modifier,
  showMenuButton: Boolean = true,
) {
  Surface(
    modifier = modifier.fillMaxWidth(),
    color = Color.Transparent,
  ) {
    Column(
      modifier = Modifier
        .background(
          Brush.horizontalGradient(
            colors = listOf(RoyalHeaderStart, RoyalHeaderMid, RoyalHeaderStart)
          )
        )
        .statusBarsPadding()
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .height(60.dp)
          .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(
          modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onBrandClick)
            .padding(horizontal = 4.dp, vertical = 6.dp)
            .testTag("brand_home_button"),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Icon(
            imageVector = Icons.Rounded.GridView,
            contentDescription = null,
            tint = GoldBright,
            modifier = Modifier.size(22.dp),
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = stringResource(R.string.brand_title),
            fontFamily = PlayfairDisplayFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 23.sp,
            color = GoldBright,
          )
        }

        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Surface(
            onClick = onToggleLanguage,
            shape = CircleShape,
            color = RoyalHeaderStart.copy(alpha = 0.45f),
            border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.55f)),
            modifier = Modifier
              .minimumInteractiveComponentSize()
              .height(42.dp)
              .widthIn(min = 56.dp)
              .testTag("language_toggle_button"),
          ) {
            Box(
              contentAlignment = Alignment.Center,
              modifier = Modifier.padding(horizontal = 14.dp),
            ) {
              Text(
                text = currentLang.code,
                color = GoldBright,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
              )
            }
          }

          if (showMenuButton) {
            IconButton(
              onClick = onMenuToggle,
              modifier = Modifier
                .size(48.dp)
                .border(1.dp, GoldPrimary.copy(alpha = 0.55f), CircleShape)
                .testTag("menu_button"),
            ) {
              Icon(
                imageVector = if (isDrawerOpen) Icons.Rounded.Close else Icons.Rounded.Menu,
                contentDescription = stringResource(R.string.cd_menu_button),
                tint = GoldBright,
                modifier = Modifier.size(26.dp),
              )
            }
          }
        }
      }
      HorizontalDivider(color = GoldPrimary.copy(alpha = 0.45f), thickness = 1.dp)
    }
  }
}

@Composable
fun RoyalPanelCard(
  modifier: Modifier = Modifier,
  content: @Composable () -> Unit,
) {
  Surface(
    modifier = modifier.fillMaxWidth(),
    shape = RoundedCornerShape(24.dp),
    border = BorderStroke(1.dp, GoldBorder),
    color = RoyalSurfaceDeep,
    shadowElevation = 10.dp,
  ) {
    Box(
      modifier = Modifier
        .background(
          Brush.linearGradient(
            colors = listOf(RoyalSurfaceElevated, RoyalSurfaceDeep)
          )
        )
        .padding(20.dp)
    ) {
      content()
    }
  }
}

@Composable
fun YemeGradientButton(
  text: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  icon: ImageVector? = null,
  enabled: Boolean = true,
  isLoading: Boolean = false,
  isGold: Boolean = false,
) {
  val brush = if (isGold) {
    Brush.linearGradient(listOf(GoldBright, GoldDeep))
  } else {
    Brush.linearGradient(listOf(VioletPrimary, VioletDeep))
  }
  val contentColor = if (isGold) InkOnGold else Color.White
  val border = if (isGold) null else BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.55f))

  Button(
    onClick = onClick,
    enabled = enabled && !isLoading,
    shape = RoundedCornerShape(12.dp),
    border = border,
    colors = ButtonDefaults.buttonColors(
      containerColor = Color.Transparent,
      contentColor = contentColor,
      disabledContainerColor = Color.Transparent,
      disabledContentColor = contentColor.copy(alpha = 0.5f),
    ),
    contentPadding = PaddingValues(0.dp),
    modifier = modifier
      .fillMaxWidth()
      .height(54.dp),
  ) {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .clip(RoundedCornerShape(12.dp))
        .background(brush, alpha = if (enabled && !isLoading) 1f else 0.45f),
      contentAlignment = Alignment.Center,
    ) {
      if (isLoading) {
        CircularProgressIndicator(
          modifier = Modifier.size(22.dp),
          color = contentColor,
          strokeWidth = 2.5.dp,
        )
      } else {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.Center,
          modifier = Modifier.padding(horizontal = 16.dp),
        ) {
          if (icon != null) {
            Icon(
              imageVector = icon,
              contentDescription = null,
              tint = contentColor,
              modifier = Modifier.size(22.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
          }
          Text(
            text = text,
            fontSize = 19.sp,
            fontWeight = FontWeight.Bold,
            color = contentColor,
          )
        }
      }
    }
  }
}

@Composable
fun WalletBannerCard(
  balanceEtb: Long,
  onDepositClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val lang = LocalAppLang.current
  Surface(
    modifier = modifier.fillMaxWidth(),
    shape = RoundedCornerShape(18.dp),
    border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.5f)),
    color = RoyalHeaderStart,
  ) {
    Row(
      modifier = Modifier
        .background(
          Brush.linearGradient(
            colors = listOf(Color(0xFF4B1D8F), RoyalHeaderStart)
          )
        )
        .padding(horizontal = 18.dp, vertical = 14.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Column {
        Text(
          text = AppStrings.walletLabel(lang),
          style = MaterialTheme.typography.bodyMedium,
          color = InkMuted,
        )
        Text(
          text = "$balanceEtb ETB",
          fontFamily = PlayfairDisplayFontFamily,
          fontWeight = FontWeight.Bold,
          fontSize = 30.sp,
          color = GoldBright,
        )
      }

      Button(
        onClick = onDepositClick,
        colors = ButtonDefaults.buttonColors(
          containerColor = EmeraldSuccess,
          contentColor = Color.White,
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
          .height(48.dp)
          .testTag("quick_deposit_button"),
      ) {
        Text(
          text = AppStrings.quickDeposit(lang),
          fontWeight = FontWeight.Bold,
        )
      }
    }
  }
}

@Composable
fun YemeDrawerHeader(
  userLabel: String,
  modifier: Modifier = Modifier,
) {
  val lang = LocalAppLang.current
  Column(
    modifier = modifier
      .fillMaxWidth()
      .background(
        Brush.verticalGradient(
          colors = listOf(RoyalHeaderStart, RoyalBgTop)
        )
      ),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(top = 24.dp, bottom = 14.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Text(
        text = "YEME",
        fontFamily = PlayfairDisplayFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 46.sp,
        lineHeight = 46.sp,
        color = GoldBright,
      )
      Spacer(modifier = Modifier.height(4.dp))
      Text(
        text = "BINGO",
        fontFamily = PlayfairDisplayFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        letterSpacing = 3.sp,
        color = GoldBright,
      )
    }
    HorizontalDivider(color = GoldPrimary.copy(alpha = 0.4f))
    Text(
      text = userLabel,
      fontWeight = FontWeight.Bold,
      color = InkLight,
      textAlign = TextAlign.Center,
      modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 12.dp, horizontal = 12.dp),
    )
    HorizontalDivider(color = GoldPrimary.copy(alpha = 0.25f))
    Text(
      text = AppStrings.cloudConnected(lang),
      style = MaterialTheme.typography.bodySmall,
      color = InkMuted,
      textAlign = TextAlign.Center,
      modifier = Modifier.padding(vertical = 8.dp),
    )
  }
}

@Composable
fun BingoCardGrid(
  card: List<List<Int>>,
  markedCells: Set<Pair<Int, Int>>,
  winningCells: List<Pair<Int, Int>>,
  isInteractive: Boolean,
  shakeCell: Pair<Int, Int>? = null,
  onCellClick: (col: Int, row: Int) -> Unit = { _, _ -> },
  titleText: String? = null,
  modifier: Modifier = Modifier,
) {
  Surface(
    modifier = modifier.fillMaxWidth(),
    shape = RoundedCornerShape(20.dp),
    border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.45f)),
    color = RoyalHeaderStart,
  ) {
    Column(
      modifier = Modifier
        .background(
          Brush.linearGradient(
            colors = listOf(Color(0xFF4B1D8F), RoyalHeaderStart)
          )
        )
        .padding(14.dp)
    ) {
      if (titleText != null) {
        Text(
          text = titleText,
          color = GoldBright,
          style = MaterialTheme.typography.labelLarge,
          textAlign = TextAlign.Center,
          modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        )
      }

      if (isInteractive) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 6.dp),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          listOf("B", "I", "N", "G", "O").forEach { letter ->
            Text(
              text = letter,
              fontFamily = PlayfairDisplayFontFamily,
              fontWeight = FontWeight.Bold,
              fontSize = 24.sp,
              color = GoldBright,
              textAlign = TextAlign.Center,
              modifier = Modifier.weight(1f),
            )
          }
        }
      }

      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        for (row in 0 until 5) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            for (col in 0 until 5) {
              val number = card[col][row]
              val isFree = number == 0
              val isMarked = isFree || (col to row) in markedCells
              val isWinning = (col to row) in winningCells
              val shouldShake = shakeCell == (col to row)

              val shakeOffset = remember { Animatable(0f) }
              LaunchedEffect(shouldShake, shakeCell) {
                if (shouldShake) {
                  shakeOffset.snapTo(0f)
                  shakeOffset.animateTo(12f, tween(55))
                  shakeOffset.animateTo(-12f, tween(55))
                  shakeOffset.animateTo(8f, tween(55))
                  shakeOffset.animateTo(0f, tween(55))
                }
              }

              val cellBrush = when {
                isWinning -> Brush.linearGradient(listOf(GoldCream, GoldPrimary))
                isMarked -> Brush.linearGradient(listOf(Color(0xFFA855F7), RoyalHeaderMid))
                else -> Brush.linearGradient(listOf(RoyalCellDefault, RoyalCellDefault))
              }
              val textColor = when {
                isWinning -> InkOnGold
                isFree -> GoldBright
                else -> Color.White
              }
              val borderStroke = if (isMarked && !isWinning) {
                BorderStroke(2.dp, GoldPrimary)
              } else null

              Box(
                modifier = Modifier
                  .weight(1f)
                  .aspectRatio(1f)
                  .offset { IntOffset(shakeOffset.value.roundToInt(), 0) }
                  .clip(RoundedCornerShape(10.dp))
                  .background(cellBrush)
                  .then(
                    if (borderStroke != null) {
                      Modifier.border(borderStroke, RoundedCornerShape(10.dp))
                    } else Modifier
                  )
                  .then(
                    if (isInteractive && !isFree) {
                      Modifier
                        .clickable { onCellClick(col, row) }
                        .testTag("bingo_cell_${col}_${row}")
                    } else Modifier
                  ),
                contentAlignment = Alignment.Center,
              ) {
                Text(
                  text = if (isFree) "★" else number.toString(),
                  fontWeight = FontWeight.Bold,
                  fontSize = 20.sp,
                  color = textColor,
                )
              }
            }
          }
        }
      }
    }
  }
}

@Composable
fun CalledNumbersBoard(
  calledNumbers: List<Int>,
  modifier: Modifier = Modifier,
) {
  val lang = LocalAppLang.current
  val calledSet = remember(calledNumbers) { calledNumbers.toSet() }
  val lastBall = calledNumbers.lastOrNull()

  Surface(
    modifier = modifier.fillMaxWidth(),
    shape = RoundedCornerShape(16.dp),
    border = BorderStroke(1.dp, VioletLine),
    color = RoyalBoardBg,
  ) {
    Column(modifier = Modifier.padding(12.dp)) {
      for (row in 0 until 8) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          for (col in 0 until 10) {
            val num = row * 10 + col + 1
            if (num <= 75) {
              val isCalled = num in calledSet
              val isLast = num == lastBall
              Box(
                modifier = Modifier
                  .weight(1f)
                  .aspectRatio(1f)
                  .clip(RoundedCornerShape(5.dp))
                  .background(
                    if (isCalled) {
                      Brush.linearGradient(listOf(GoldBright, GoldDeep))
                    } else {
                      Brush.linearGradient(listOf(RoyalCellDefault, RoyalCellDefault))
                    }
                  )
                  .then(
                    if (isLast) {
                      Modifier.border(2.dp, Color.White, RoundedCornerShape(5.dp))
                    } else Modifier
                  ),
                contentAlignment = Alignment.Center,
              ) {
                Text(
                  text = num.toString(),
                  fontSize = 11.sp,
                  fontWeight = if (isCalled) FontWeight.Bold else FontWeight.Medium,
                  color = if (isCalled) InkOnGold else InkMuted,
                )
              }
            } else {
              Spacer(modifier = Modifier.weight(1f))
            }
          }
        }
        if (row < 7) Spacer(modifier = Modifier.height(4.dp))
      }

      Spacer(modifier = Modifier.height(10.dp))
      Text(
        text = if (lastBall != null) {
          if (lang == AppLang.TI) {
            "${calledNumbers.size} ካብ 75 ተጸዊዑ · ናይ መወዳእታ: ${BingoEngine.bingoLetterFor(lastBall)} $lastBall"
          } else {
            "Called ${calledNumbers.size} of 75 · Last: ${BingoEngine.bingoLetterFor(lastBall)} $lastBall"
          }
        } else {
          AppStrings.waitingFirstCall(lang)
        },
        style = MaterialTheme.typography.bodyMedium,
        color = InkMuted,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
      )
    }
  }
}

@Composable
fun ModalNoteDialog(
  message: String,
  onDismiss: () -> Unit,
) {
  val lang = LocalAppLang.current
  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = RoyalSurfaceDeep,
    titleContentColor = GoldBright,
    textContentColor = InkLight,
    text = {
      Text(
        text = message,
        style = MaterialTheme.typography.bodyLarge,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
      )
    },
    confirmButton = {
      Button(
        onClick = onDismiss,
        colors = ButtonDefaults.buttonColors(
          containerColor = VioletPrimary,
          contentColor = Color.White,
        ),
        modifier = Modifier
          .fillMaxWidth()
          .height(48.dp)
          .testTag("modal_ok_button"),
      ) {
        Text(text = AppStrings.ok(lang), fontWeight = FontWeight.Bold)
      }
    },
  )
}

@Composable
fun GameOverDialog(
  title: String,
  message: String,
  onBackToRooms: () -> Unit,
) {
  val lang = LocalAppLang.current
  AlertDialog(
    onDismissRequest = onBackToRooms,
    containerColor = RoyalSurfaceDeep,
    titleContentColor = GoldBright,
    textContentColor = InkLight,
    title = {
      Text(
        text = title,
        fontFamily = PlayfairDisplayFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 30.sp,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
      )
    },
    text = {
      Text(
        text = message,
        style = MaterialTheme.typography.bodyLarge,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
      )
    },
    confirmButton = {
      Button(
        onClick = onBackToRooms,
        colors = ButtonDefaults.buttonColors(
          containerColor = GoldPrimary,
          contentColor = InkOnGold,
        ),
        modifier = Modifier
          .fillMaxWidth()
          .height(48.dp)
          .testTag("game_over_back_button"),
      ) {
        Text(
          text = AppStrings.backToRooms(lang),
          fontWeight = FontWeight.Bold,
        )
      }
    },
  )
}
