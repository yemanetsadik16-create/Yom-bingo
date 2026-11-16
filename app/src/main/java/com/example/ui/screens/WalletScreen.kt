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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.example.model.PaymentRequest
import com.example.model.WalletTransaction
import com.example.model.WithdrawalRequest
import com.example.ui.components.RoyalPanelCard
import com.example.ui.components.WalletBannerCard
import com.example.ui.components.YemeGradientButton
import com.example.ui.i18n.AppStrings
import com.example.ui.i18n.LocalAppLang
import com.example.ui.theme.GoldBright
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.InkLight
import com.example.ui.theme.InkMuted
import com.example.ui.theme.MintCredit
import com.example.ui.theme.PlayfairDisplayFontFamily
import com.example.ui.theme.RoseError
import com.example.ui.theme.VioletLine
import com.example.ui.theme.VioletOutline
import com.example.ui.theme.VioletPrimary
import com.example.util.BingoEngine

private enum class WalletPanelMode { NONE, DEPOSIT, WITHDRAW }

@Composable
fun WalletScreen(
  walletEtb: Long,
  transactions: List<WalletTransaction>,
  myPayments: List<PaymentRequest>,
  myWithdrawals: List<WithdrawalRequest>,
  isBusy: Boolean,
  onSubmitDeposit: (amountText: String, receiptInput: String, resetForm: () -> Unit) -> Unit,
  onSubmitWithdrawal: (amountText: String, telebirrAccount: String, resetForm: () -> Unit) -> Unit,
  modifier: Modifier = Modifier,
) {
  val lang = LocalAppLang.current
  var panelMode by remember { mutableStateOf(WalletPanelMode.NONE) }
  var depAmount by remember { mutableStateOf("") }
  var depReceipt by remember { mutableStateOf("") }
  var wdAmount by remember { mutableStateOf("") }
  var wdAccount by remember { mutableStateOf("") }

  Column(
    modifier = modifier
      .fillMaxSize()
      .verticalScroll(rememberScrollState())
      .padding(horizontal = 16.dp, vertical = 14.dp)
      .widthIn(max = 520.dp),
  ) {
    WalletBannerCard(
      balanceEtb = walletEtb,
      onDepositClick = {
        panelMode = if (panelMode == WalletPanelMode.DEPOSIT) WalletPanelMode.NONE else WalletPanelMode.DEPOSIT
      },
    )

    Spacer(modifier = Modifier.height(12.dp))

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Button(
        onClick = {
          panelMode = if (panelMode == WalletPanelMode.DEPOSIT) WalletPanelMode.NONE else WalletPanelMode.DEPOSIT
        },
        colors = ButtonDefaults.buttonColors(
          containerColor = VioletPrimary,
          contentColor = Color.White,
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
          .weight(1f)
          .height(50.dp)
          .testTag("wallet_deposit_toggle"),
      ) {
        Text(text = AppStrings.deposit(lang), fontWeight = FontWeight.Bold)
      }

      OutlinedButton(
        onClick = {
          panelMode = if (panelMode == WalletPanelMode.WITHDRAW) WalletPanelMode.NONE else WalletPanelMode.WITHDRAW
        },
        border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.6f)),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = GoldBright),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
          .weight(1f)
          .height(50.dp)
          .testTag("wallet_withdraw_toggle"),
      ) {
        Text(text = AppStrings.withdraw(lang), fontWeight = FontWeight.Bold)
      }
    }

    if (panelMode == WalletPanelMode.DEPOSIT) {
      Spacer(modifier = Modifier.height(14.dp))
      RoyalPanelCard {
        Column {
          Text(
            text = AppStrings.addMoneyTitle(lang),
            fontFamily = PlayfairDisplayFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
            color = GoldBright,
          )
          Spacer(modifier = Modifier.height(6.dp))
          Text(
            text = AppStrings.payWithTelebirrTo(lang),
            style = MaterialTheme.typography.bodyLarge,
            color = InkLight,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
          )
          Spacer(modifier = Modifier.height(12.dp))

          OutlinedTextField(
            value = depAmount,
            onValueChange = { depAmount = it },
            label = { Text(AppStrings.amountPaidLabel(lang)) },
            placeholder = { Text("Minimum ${BingoEngine.PAY_MIN_BIRR} birr") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            colors = walletInputColors(),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("deposit_amount_input"),
          )
          Spacer(modifier = Modifier.height(10.dp))
          OutlinedTextField(
            value = depReceipt,
            onValueChange = { depReceipt = it },
            label = { Text(AppStrings.receiptLinkLabel(lang)) },
            placeholder = { Text("https://transactioninfo.ethiotelecom.et/receipt/...") },
            singleLine = true,
            colors = walletInputColors(),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("deposit_receipt_input"),
          )
          Spacer(modifier = Modifier.height(12.dp))

          YemeGradientButton(
            text = AppStrings.submitPayment(lang),
            enabled = !isBusy,
            onClick = {
              onSubmitDeposit(depAmount, depReceipt) {
                depAmount = ""
                depReceipt = ""
                panelMode = WalletPanelMode.NONE
              }
            },
            modifier = Modifier.testTag("submit_deposit_button"),
          )

          Spacer(modifier = Modifier.height(8.dp))
          Text(
            text = AppStrings.depositAdminNote(lang),
            style = MaterialTheme.typography.bodyMedium,
            color = InkMuted,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
          )
        }
      }
    }

    if (panelMode == WalletPanelMode.WITHDRAW) {
      Spacer(modifier = Modifier.height(14.dp))
      RoyalPanelCard {
        Column {
          Text(
            text = AppStrings.withdrawMoneyTitle(lang),
            fontFamily = PlayfairDisplayFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
            color = GoldBright,
          )
          Spacer(modifier = Modifier.height(10.dp))

          OutlinedTextField(
            value = wdAmount,
            onValueChange = { wdAmount = it },
            label = { Text(AppStrings.withdrawAmountLabel(lang)) },
            placeholder = { Text("Minimum ${BingoEngine.PAY_MIN_BIRR} birr") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            colors = walletInputColors(),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("withdraw_amount_input"),
          )
          Spacer(modifier = Modifier.height(10.dp))
          OutlinedTextField(
            value = wdAccount,
            onValueChange = { wdAccount = it },
            label = { Text(AppStrings.telebirrAccountLabel(lang)) },
            placeholder = { Text("09XXXXXXXX") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            colors = walletInputColors(),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("withdraw_account_input"),
          )
          Spacer(modifier = Modifier.height(12.dp))

          YemeGradientButton(
            text = AppStrings.requestWithdrawal(lang),
            enabled = !isBusy,
            onClick = {
              onSubmitWithdrawal(wdAmount, wdAccount) {
                wdAmount = ""
                wdAccount = ""
                panelMode = WalletPanelMode.NONE
              }
            },
            modifier = Modifier.testTag("submit_withdraw_button"),
          )

          Spacer(modifier = Modifier.height(8.dp))
          Text(
            text = AppStrings.withdrawHoldNote(lang),
            style = MaterialTheme.typography.bodyMedium,
            color = InkMuted,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
          )
        }
      }
    }

    if (myPayments.isNotEmpty()) {
      Spacer(modifier = Modifier.height(18.dp))
      Text(
        text = AppStrings.paymentRequests(lang),
        fontFamily = PlayfairDisplayFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        color = GoldBright,
      )
      Spacer(modifier = Modifier.height(6.dp))
      myPayments.take(5).forEach { req ->
        val (statusLabel, statusColor) = when (req.status) {
          "approved" -> "Approved" to MintCredit
          "declined" -> "Declined" to RoseError
          else -> "Pending approval" to GoldBright
        }
        LedgerRow(left = "${req.amount} birr · ${req.receiptId}", right = statusLabel, rightColor = statusColor)
      }
    }

    if (myWithdrawals.isNotEmpty()) {
      Spacer(modifier = Modifier.height(18.dp))
      Text(
        text = AppStrings.withdrawalsSection(lang),
        fontFamily = PlayfairDisplayFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        color = GoldBright,
      )
      Spacer(modifier = Modifier.height(6.dp))
      myWithdrawals.take(5).forEach { wd ->
        val (statusLabel, statusColor) = when (wd.status) {
          "approved" -> "Paid" to MintCredit
          "declined" -> "Declined (refunded)" to RoseError
          else -> "Pending approval" to GoldBright
        }
        LedgerRow(left = "${wd.amount} birr · ${wd.telebirrAccount}", right = statusLabel, rightColor = statusColor)
      }
    }

    Spacer(modifier = Modifier.height(18.dp))
    Text(
      text = AppStrings.recentTransactions(lang),
      fontFamily = PlayfairDisplayFontFamily,
      fontWeight = FontWeight.SemiBold,
      fontSize = 20.sp,
      color = GoldBright,
    )
    Spacer(modifier = Modifier.height(6.dp))
    if (transactions.isEmpty()) {
      Text(
        text = AppStrings.noTransactions(lang),
        style = MaterialTheme.typography.bodyMedium,
        color = InkMuted,
      )
    } else {
      transactions.take(12).forEach { tx ->
        val amountStr = if (tx.amount > 0) "+${tx.amount}" else tx.amount.toString()
        LedgerRow(
          left = tx.label,
          right = amountStr,
          rightColor = if (tx.amount > 0) MintCredit else RoseError,
        )
      }
    }
  }
}

@Composable
private fun LedgerRow(
  left: String,
  right: String,
  rightColor: Color,
) {
  Column {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 10.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        text = left,
        style = MaterialTheme.typography.bodyMedium,
        color = InkLight,
        modifier = Modifier.weight(1f),
      )
      Text(
        text = right,
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.Bold,
        color = rightColor,
      )
    }
    HorizontalDivider(color = VioletLine)
  }
}

@Composable
private fun walletInputColors() = OutlinedTextFieldDefaults.colors(
  focusedBorderColor = GoldPrimary,
  unfocusedBorderColor = VioletOutline,
  focusedTextColor = InkLight,
  unfocusedTextColor = InkLight,
  focusedLabelColor = GoldBright,
  unfocusedLabelColor = InkMuted,
)
