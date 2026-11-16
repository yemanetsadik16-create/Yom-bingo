package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.UiState
import com.example.model.UserProfile
import com.example.ui.components.RoyalPanelCard
import com.example.ui.components.YemeGradientButton
import com.example.ui.i18n.AppStrings
import com.example.ui.i18n.LocalAppLang
import com.example.ui.theme.GoldBright
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.InkLight
import com.example.ui.theme.InkMuted
import com.example.ui.theme.InkOnGold
import com.example.ui.theme.PlayfairDisplayFontFamily
import com.example.ui.theme.RoseError
import com.example.ui.theme.VioletOutline
import com.example.ui.theme.VioletPrimary
import com.example.util.BingoEngine
import com.example.util.RoundCycle

@Composable
fun HomeScreen(
  profileState: UiState<UserProfile?>,
  roundCycle: RoundCycle,
  isAdmin: Boolean,
  isBusy: Boolean,
  onPlayRooms: () -> Unit,
  onOpenWallet: () -> Unit,
  onOpenHowToPlay: () -> Unit,
  onOpenFaq: () -> Unit,
  onOpenAdmin: () -> Unit,
  onSaveProfile: (String, String) -> Unit,
  modifier: Modifier = Modifier,
) {
  val lang = LocalAppLang.current
  val countdownText = if (roundCycle.isLobbyOpen) {
    BingoEngine.formatMmSs(roundCycle.msUntilGameStart)
  } else {
    BingoEngine.formatMmSs(roundCycle.msUntilNextCycle)
  }

  Column(
    modifier = modifier
      .fillMaxSize()
      .verticalScroll(rememberScrollState())
      .padding(horizontal = 16.dp, vertical = 16.dp)
      .widthIn(max = 520.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Text(
      text = countdownText,
      fontFamily = PlayfairDisplayFontFamily,
      fontWeight = FontWeight.Bold,
      fontSize = 50.sp,
      color = GoldBright,
      textAlign = TextAlign.Center,
    )
    Text(
      text = if (roundCycle.isLobbyOpen) {
        AppStrings.roomsOpen(lang)
      } else {
        AppStrings.roundRunning(lang)
      },
      style = MaterialTheme.typography.bodyMedium,
      color = InkMuted,
      textAlign = TextAlign.Center,
    )

    Spacer(modifier = Modifier.height(16.dp))

    when (profileState) {
      is UiState.Loading -> {
        RoyalPanelCard {
          BoxCenteredLoading()
        }
      }
      is UiState.Error -> {
        RoyalPanelCard {
          Text(
            text = profileState.message,
            color = RoseError,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
          )
        }
      }
      is UiState.Success -> {
        val profile = profileState.data
        var editingProfile by remember { mutableStateOf(false) }
        var usernameInput by remember(profile?.username) { mutableStateOf(profile?.username ?: "") }
        var phoneInput by remember(profile?.phone) { mutableStateOf(profile?.phone ?: "") }

        LaunchedEffect(profile?.username, profile?.phone) {
          if (profile != null && !editingProfile) {
            usernameInput = profile.username
            phoneInput = profile.phone
          }
        }

        RoyalPanelCard {
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
              text = AppStrings.welcomePlayer(lang, profile?.username ?: "Player"),
              fontFamily = PlayfairDisplayFontFamily,
              fontWeight = FontWeight.Bold,
              fontSize = 26.sp,
              color = GoldBright,
              textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = GoldPrimary.copy(alpha = 0.25f))
            Spacer(modifier = Modifier.height(12.dp))

            Text(
              text = "${AppStrings.walletLabel(lang)}: ${profile?.wallet ?: 0L} ETB",
              style = MaterialTheme.typography.titleMedium,
              color = InkLight,
            )
            if (!profile?.phone.isNullOrBlank()) {
              Text(
                text = "Telebirr / ${AppStrings.labelPhone(lang)}: ${profile?.phone}",
                style = MaterialTheme.typography.bodyMedium,
                color = InkMuted,
              )
            }

            Spacer(modifier = Modifier.height(16.dp))

            YemeGradientButton(
              text = AppStrings.playStakeRooms(lang),
              icon = Icons.Rounded.PlayArrow,
              onClick = onPlayRooms,
              modifier = Modifier.testTag("play_rooms_button"),
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedButton(
              onClick = { editingProfile = !editingProfile },
              border = BorderStroke(1.dp, VioletOutline),
              colors = ButtonDefaults.outlinedButtonColors(contentColor = GoldBright),
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("edit_profile_toggle"),
            ) {
              Icon(imageVector = Icons.Rounded.Edit, contentDescription = null)
              Spacer(modifier = Modifier.padding(4.dp))
              Text(
                text = if (editingProfile) AppStrings.cancel(lang) else AppStrings.editProfile(lang),
                fontWeight = FontWeight.SemiBold,
              )
            }

            if (editingProfile) {
              Spacer(modifier = Modifier.height(14.dp))
              OutlinedTextField(
                value = usernameInput,
                onValueChange = { usernameInput = it },
                label = { Text(AppStrings.labelUsername(lang)) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                  focusedBorderColor = GoldPrimary,
                  unfocusedBorderColor = VioletOutline,
                  focusedTextColor = InkLight,
                  unfocusedTextColor = InkLight,
                  focusedLabelColor = GoldBright,
                  unfocusedLabelColor = InkMuted,
                ),
                modifier = Modifier
                  .fillMaxWidth()
                  .testTag("profile_username_input"),
              )
              Spacer(modifier = Modifier.height(10.dp))
              OutlinedTextField(
                value = phoneInput,
                onValueChange = { phoneInput = it },
                label = { Text(AppStrings.labelPhone(lang)) },
                placeholder = { Text("09XXXXXXXX") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                colors = OutlinedTextFieldDefaults.colors(
                  focusedBorderColor = GoldPrimary,
                  unfocusedBorderColor = VioletOutline,
                  focusedTextColor = InkLight,
                  unfocusedTextColor = InkLight,
                  focusedLabelColor = GoldBright,
                  unfocusedLabelColor = InkMuted,
                ),
                modifier = Modifier
                  .fillMaxWidth()
                  .testTag("profile_phone_input"),
              )
              Spacer(modifier = Modifier.height(12.dp))
              YemeGradientButton(
                text = AppStrings.saveProfile(lang),
                isGold = true,
                enabled = !isBusy && usernameInput.trim().length >= 2,
                onClick = {
                  onSaveProfile(usernameInput, phoneInput)
                  editingProfile = false
                },
                modifier = Modifier.testTag("save_profile_button"),
              )
            }
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(14.dp))

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      OutlinedButton(
        onClick = onOpenWallet,
        border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.45f)),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = GoldBright),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
          .weight(1f)
          .height(50.dp)
          .testTag("home_wallet_button"),
      ) {
        Icon(imageVector = Icons.Rounded.AccountBalanceWallet, contentDescription = null)
        Spacer(modifier = Modifier.padding(3.dp))
        Text(text = AppStrings.navWallet(lang), fontWeight = FontWeight.Bold)
      }
      OutlinedButton(
        onClick = onOpenHowToPlay,
        border = BorderStroke(1.dp, VioletOutline),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = InkLight),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
          .weight(1f)
          .height(50.dp)
          .testTag("home_how_button"),
      ) {
        Icon(imageVector = Icons.Rounded.Info, contentDescription = null)
        Spacer(modifier = Modifier.padding(3.dp))
        Text(text = AppStrings.navHowToPlay(lang), fontWeight = FontWeight.Medium)
      }
    }

    Spacer(modifier = Modifier.height(10.dp))

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      OutlinedButton(
        onClick = onOpenFaq,
        border = BorderStroke(1.dp, VioletOutline),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = InkLight),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
          .weight(1f)
          .height(48.dp)
          .testTag("home_faq_button"),
      ) {
        Icon(imageVector = Icons.AutoMirrored.Rounded.HelpOutline, contentDescription = null)
        Spacer(modifier = Modifier.padding(3.dp))
        Text(text = AppStrings.navFaqs(lang), fontWeight = FontWeight.Medium)
      }
      if (isAdmin) {
        OutlinedButton(
          onClick = onOpenAdmin,
          border = BorderStroke(1.dp, GoldPrimary),
          colors = ButtonDefaults.outlinedButtonColors(contentColor = GoldBright),
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier
            .weight(1f)
            .height(48.dp)
            .testTag("home_admin_button"),
        ) {
          Icon(imageVector = Icons.Rounded.AdminPanelSettings, contentDescription = null)
          Spacer(modifier = Modifier.padding(3.dp))
          Text(text = AppStrings.navAdmin(lang), fontWeight = FontWeight.Bold)
        }
      }
    }
  }
}

@Composable
private fun BoxCenteredLoading() {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .padding(vertical = 24.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    CircularProgressIndicator(color = GoldPrimary)
  }
}
