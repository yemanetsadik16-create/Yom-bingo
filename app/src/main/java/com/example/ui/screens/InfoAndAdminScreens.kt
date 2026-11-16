package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.PaymentRequest
import com.example.model.UserProfile
import com.example.model.WithdrawalRequest
import com.example.ui.components.RoyalPanelCard
import com.example.ui.i18n.AppStrings
import com.example.ui.i18n.LocalAppLang
import com.example.ui.theme.CrimsonDecline
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.GoldBright
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.InkLight
import com.example.ui.theme.InkMuted
import com.example.ui.theme.InkOnGold
import com.example.ui.theme.MintCredit
import com.example.ui.theme.PlayfairDisplayFontFamily
import com.example.ui.theme.RoseError
import com.example.ui.theme.RoyalSurfaceDeep
import com.example.ui.theme.RoyalSurfaceElevated
import com.example.ui.theme.VioletLine
import com.example.ui.theme.VioletOutline
import com.example.ui.theme.VioletPrimary
import com.example.util.BingoEngine

private data class HowToStep(val number: Int, val title: String, val body: String)

private val HOW_TO_STEPS = listOf(
  HowToStep(
    1,
    "Sign In & Top Up Wallet",
    "Register or log in and open Wallet to send a Telebirr deposit (minimum ${BingoEngine.PAY_MIN_BIRR} ETB) to ${BingoEngine.TELEBIRR_MERCHANT_NAME} (${BingoEngine.TELEBIRR_MERCHANT_NUMBER}). Submit your Telebirr receipt link or reference ID for admin approval.",
  ),
  HowToStep(
    2,
    "Wait for the Open Lobby",
    "Every 8 minutes a synchronized cycle begins with a 90-second open lobby across all 5 stake rooms (10, 20, 50, 100, and 200 ETB).",
  ),
  HowToStep(
    3,
    "Pick Your Lucky Ticket",
    "Select a room, browse the numbered tickets, preview the 5x5 card layout (with a free center ★), and tap JOIN ROOM. Your stake is deducted from your wallet.",
  ),
  HowToStep(
    4,
    "Minimum 2 Players Rule",
    "Each room needs at least 2 players when the countdown reaches 00:00. If you are the only player in your room, your stake is automatically refunded back to your wallet.",
  ),
  HowToStep(
    5,
    "Mark Balls & Watch the Pattern",
    "When the game starts, a new ball is called every 5 seconds. Tap matching numbers on your card or toggle Auto-mark. Each round displays a Target Goal pattern (Any Line, Four Corners, Both Diagonals, Plus Sign, Letter T, or Full House).",
  ),
  HowToStep(
    6,
    "Claim BINGO! to Win 80% of the Pool",
    "Complete the round's pattern and press BINGO! before anyone else. The winner receives 80% of the total stake pool directly into their wallet.",
  ),
)

private data class FaqItem(val question: String, val answer: String)

private val FAQ_ITEMS = listOf(
  FaqItem(
    "How do deposits work?",
    "Send your deposit via Telebirr to ${BingoEngine.TELEBIRR_MERCHANT_NAME} (${BingoEngine.TELEBIRR_MERCHANT_NUMBER}), copy the Telebirr receipt link or transaction ID, and submit it in the Wallet tab. Once verified by the admin, your wallet is credited.",
  ),
  FaqItem(
    "What is the minimum deposit and withdrawal?",
    "Both deposits and withdrawals have a minimum of ${BingoEngine.PAY_MIN_BIRR} ETB. Withdrawal requests hold the amount immediately and are refunded automatically if declined.",
  ),
  FaqItem(
    "What happens if nobody else joins my room?",
    "At least 2 players are required for a room to start. If the countdown ends with only 1 player, the room does not launch and your full ticket stake is refunded to your wallet.",
  ),
  FaqItem(
    "How are tickets and balls determined?",
    "Tickets and the 75-ball draw order are deterministically synced across all connected devices for each 8-minute cycle and room stake, so everyone in the room sees the exact same calls at the exact same time.",
  ),
  FaqItem(
    "What is the prize payout?",
    "The winner receives 80% of the combined stake pool (playerCount × stake × 0.80).",
  ),
  FaqItem(
    "Can I enable voice announcements?",
    "Yes! On the live Bingo screen, tap Sound on / Sound off to toggle on-device English Text-to-Speech ball announcements.",
  ),
)

@Composable
fun HowToPlayScreen(
  modifier: Modifier = Modifier,
) {
  val lang = LocalAppLang.current
  Column(
    modifier = modifier
      .fillMaxSize()
      .verticalScroll(rememberScrollState())
      .padding(horizontal = 16.dp, vertical = 16.dp)
      .widthIn(max = 600.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    Text(
      text = AppStrings.navHowToPlay(lang),
      fontFamily = PlayfairDisplayFontFamily,
      fontWeight = FontWeight.Bold,
      fontSize = 30.sp,
      color = GoldBright,
    )

    HOW_TO_STEPS.forEach { step ->
      RoyalPanelCard {
        Row(verticalAlignment = Alignment.Top) {
          Box(
            modifier = Modifier
              .size(34.dp)
              .clip(CircleShape)
              .background(GoldPrimary),
            contentAlignment = Alignment.Center,
          ) {
            Text(
              text = step.number.toString(),
              color = InkOnGold,
              fontWeight = FontWeight.Bold,
              fontSize = 16.sp,
            )
          }
          Spacer(modifier = Modifier.width(14.dp))
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = step.title,
              style = MaterialTheme.typography.titleMedium,
              color = GoldCreamColor,
              fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = step.body,
              style = MaterialTheme.typography.bodyMedium,
              color = InkLight,
            )
          }
        }
      }
    }
  }
}

private val GoldCreamColor = Color(0xFFFFF3C9)

@Composable
fun FaqScreen(
  modifier: Modifier = Modifier,
) {
  val lang = LocalAppLang.current
  var expandedIndex by remember { mutableIntStateOf(0) }

  Column(
    modifier = modifier
      .fillMaxSize()
      .verticalScroll(rememberScrollState())
      .padding(horizontal = 16.dp, vertical = 16.dp)
      .widthIn(max = 600.dp),
    verticalArrangement = Arrangement.spacedBy(10.dp),
  ) {
    Text(
      text = AppStrings.navFaqs(lang),
      fontFamily = PlayfairDisplayFontFamily,
      fontWeight = FontWeight.Bold,
      fontSize = 30.sp,
      color = GoldBright,
    )

    FAQ_ITEMS.forEachIndexed { index, item ->
      val expanded = expandedIndex == index
      Surface(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(16.dp))
          .clickable { expandedIndex = if (expanded) -1 else index }
          .testTag("faq_item_$index"),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, if (expanded) GoldPrimary else VioletLine),
        color = RoyalSurfaceElevated,
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              text = item.question,
              style = MaterialTheme.typography.titleMedium,
              color = GoldBright,
              fontWeight = FontWeight.Bold,
              modifier = Modifier.weight(1f),
            )
            Icon(
              imageVector = if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
              contentDescription = null,
              tint = GoldBright,
            )
          }
          AnimatedVisibility(visible = expanded) {
            Column {
              Spacer(modifier = Modifier.height(8.dp))
              HorizontalDivider(color = VioletLine)
              Spacer(modifier = Modifier.height(8.dp))
              Text(
                text = item.answer,
                style = MaterialTheme.typography.bodyMedium,
                color = InkLight,
              )
            }
          }
        }
      }
    }
  }
}

@Composable
fun AdminScreen(
  isAdmin: Boolean,
  currentUid: String,
  payments: List<PaymentRequest>,
  withdrawals: List<WithdrawalRequest>,
  users: List<UserProfile>,
  isBusy: Boolean,
  onApprovePayment: (requestId: String, amount: Long) -> Unit,
  onDeclinePayment: (requestId: String) -> Unit,
  onMarkWithdrawalPaid: (requestId: String) -> Unit,
  onDeclineRefundWithdrawal: (requestId: String) -> Unit,
  onAdjustWallet: (targetUid: String, delta: Long, label: String) -> Unit,
  onDeleteUser: (targetUid: String) -> Unit,
  modifier: Modifier = Modifier,
) {
  if (!isAdmin) {
    Column(
      modifier = modifier
        .fillMaxSize()
        .padding(24.dp),
      verticalArrangement = Arrangement.Center,
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Text(
        text = stringResource(R.string.admin_access_denied),
        style = MaterialTheme.typography.titleMedium,
        color = RoseError,
      )
    }
    return
  }

  var selectedTab by remember { mutableIntStateOf(0) }
  var userSearch by remember { mutableStateOf("") }
  var adjustTargetUser by remember { mutableStateOf<UserProfile?>(null) }
  var adjustAmountText by remember { mutableStateOf("") }
  var adjustIsCredit by remember { mutableStateOf(true) }
  var deleteTargetUser by remember { mutableStateOf<UserProfile?>(null) }

  Column(
    modifier = modifier
      .fillMaxSize()
      .padding(horizontal = 16.dp, vertical = 12.dp)
      .widthIn(max = 720.dp),
  ) {
    Text(
      text = stringResource(R.string.admin_panel_title),
      fontFamily = PlayfairDisplayFontFamily,
      fontWeight = FontWeight.Bold,
      fontSize = 28.sp,
      color = GoldBright,
    )

    Spacer(modifier = Modifier.height(8.dp))

    TabRow(
      selectedTabIndex = selectedTab,
      containerColor = RoyalSurfaceDeep,
      contentColor = GoldBright,
      indicator = { tabPositions ->
        if (selectedTab < tabPositions.size) {
          TabRowDefaults.SecondaryIndicator(
            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
            color = GoldPrimary,
          )
        }
      },
    ) {
      Tab(
        selected = selectedTab == 0,
        onClick = { selectedTab = 0 },
        text = { Text("${stringResource(R.string.admin_tab_payments)} (${payments.count { it.status == "pending" }})") },
        modifier = Modifier.testTag("admin_tab_payments"),
      )
      Tab(
        selected = selectedTab == 1,
        onClick = { selectedTab = 1 },
        text = { Text("${stringResource(R.string.admin_tab_withdrawals)} (${withdrawals.count { it.status == "pending" }})") },
        modifier = Modifier.testTag("admin_tab_withdrawals"),
      )
      Tab(
        selected = selectedTab == 2,
        onClick = { selectedTab = 2 },
        text = { Text("${stringResource(R.string.admin_tab_users)} (${users.size})") },
        modifier = Modifier.testTag("admin_tab_users"),
      )
    }

    Spacer(modifier = Modifier.height(12.dp))

    Column(
      modifier = Modifier
        .fillMaxSize()
        .verticalScroll(rememberScrollState()),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      when (selectedTab) {
        0 -> {
          if (payments.isEmpty()) {
            Text(text = stringResource(R.string.admin_no_payments), color = InkMuted)
          } else {
            payments.forEach { payment ->
              Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, VioletLine),
                color = RoyalSurfaceElevated,
              ) {
                Column(modifier = Modifier.padding(14.dp)) {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                  ) {
                    Text(
                      text = "${payment.username} · ${payment.amount} ETB",
                      fontWeight = FontWeight.Bold,
                      color = GoldBright,
                    )
                    Text(
                      text = payment.status.uppercase(),
                      fontWeight = FontWeight.Bold,
                      color = when (payment.status) {
                        "approved" -> MintCredit
                        "declined" -> RoseError
                        else -> GoldPrimary
                      },
                    )
                  }
                  if (payment.phone.isNotBlank()) {
                    Text(text = "Phone: ${payment.phone}", style = MaterialTheme.typography.bodySmall, color = InkMuted)
                  }
                  Text(text = "Receipt ID: ${payment.receiptId}", style = MaterialTheme.typography.bodySmall, color = InkLight)
                  if (payment.receiptLink.isNotBlank()) {
                    Text(text = payment.receiptLink, style = MaterialTheme.typography.bodySmall, color = InkMuted)
                  }
                  if (payment.status == "pending") {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                      Button(
                        onClick = { onApprovePayment(payment.id, payment.amount) },
                        enabled = !isBusy,
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess, contentColor = Color.White),
                        modifier = Modifier.weight(1f).testTag("approve_payment_${payment.id}"),
                      ) {
                        Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Approve")
                      }
                      Button(
                        onClick = { onDeclinePayment(payment.id) },
                        enabled = !isBusy,
                        colors = ButtonDefaults.buttonColors(containerColor = CrimsonDecline, contentColor = Color.White),
                        modifier = Modifier.weight(1f).testTag("decline_payment_${payment.id}"),
                      ) {
                        Icon(Icons.Rounded.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Decline")
                      }
                    }
                  }
                }
              }
            }
          }
        }

        1 -> {
          if (withdrawals.isEmpty()) {
            Text(text = stringResource(R.string.admin_no_withdrawals), color = InkMuted)
          } else {
            withdrawals.forEach { wd ->
              Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, VioletLine),
                color = RoyalSurfaceElevated,
              ) {
                Column(modifier = Modifier.padding(14.dp)) {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                  ) {
                    Text(
                      text = "${wd.username} · ${wd.amount} ETB",
                      fontWeight = FontWeight.Bold,
                      color = GoldBright,
                    )
                    Text(
                      text = if (wd.status == "approved") "PAID" else wd.status.uppercase(),
                      fontWeight = FontWeight.Bold,
                      color = when (wd.status) {
                        "approved", "paid" -> MintCredit
                        "declined" -> RoseError
                        else -> GoldPrimary
                      },
                    )
                  }
                  Text(text = "Telebirr: ${wd.telebirrAccount}", style = MaterialTheme.typography.bodySmall, color = InkLight)
                  if (wd.status == "pending") {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                      Button(
                        onClick = { onMarkWithdrawalPaid(wd.id) },
                        enabled = !isBusy,
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess, contentColor = Color.White),
                        modifier = Modifier.weight(1f).testTag("mark_paid_${wd.id}"),
                      ) {
                        Text("Mark Paid")
                      }
                      Button(
                        onClick = { onDeclineRefundWithdrawal(wd.id) },
                        enabled = !isBusy,
                        colors = ButtonDefaults.buttonColors(containerColor = CrimsonDecline, contentColor = Color.White),
                        modifier = Modifier.weight(1f).testTag("refund_wd_${wd.id}"),
                      ) {
                        Text("Decline & Refund")
                      }
                    }
                  }
                }
              }
            }
          }
        }

        2 -> {
          OutlinedTextField(
            value = userSearch,
            onValueChange = { userSearch = it },
            label = { Text(stringResource(R.string.admin_search_users)) },
            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, tint = GoldBright) },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
              focusedBorderColor = GoldPrimary,
              unfocusedBorderColor = VioletOutline,
              focusedTextColor = InkLight,
              unfocusedTextColor = InkLight,
              focusedLabelColor = GoldBright,
              unfocusedLabelColor = InkMuted,
            ),
            modifier = Modifier.fillMaxWidth().testTag("admin_user_search_input"),
          )

          val filtered = remember(users, userSearch) {
            val q = userSearch.trim().lowercase()
            if (q.isEmpty()) users
            else users.filter {
              it.username.lowercase().contains(q) ||
                it.email.lowercase().contains(q) ||
                it.phone.lowercase().contains(q)
            }
          }

          if (filtered.isEmpty()) {
            Text(text = stringResource(R.string.admin_no_users), color = InkMuted)
          } else {
            filtered.forEach { user ->
              val uid = user.userId.ifBlank { user.id }
              Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, VioletLine),
                color = RoyalSurfaceElevated,
              ) {
                Column(modifier = Modifier.padding(14.dp)) {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                  ) {
                    Column(modifier = Modifier.weight(1f)) {
                      Text(
                        text = user.username.ifBlank { "Player" },
                        fontWeight = FontWeight.Bold,
                        color = GoldBright,
                      )
                      if (user.email.isNotBlank()) {
                        Text(text = user.email, style = MaterialTheme.typography.bodySmall, color = InkMuted)
                      }
                      if (user.phone.isNotBlank()) {
                        Text(text = user.phone, style = MaterialTheme.typography.bodySmall, color = InkMuted)
                      }
                    }
                    Text(
                      text = "${user.wallet} ETB",
                      fontWeight = FontWeight.Bold,
                      color = MintCredit,
                      fontSize = 16.sp,
                    )
                  }

                  Spacer(modifier = Modifier.height(10.dp))
                  Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                      onClick = {
                        adjustTargetUser = user
                        adjustIsCredit = true
                        adjustAmountText = ""
                      },
                      enabled = !isBusy,
                      border = BorderStroke(1.dp, EmeraldSuccess),
                      colors = ButtonDefaults.outlinedButtonColors(contentColor = MintCredit),
                      modifier = Modifier.weight(1f).testTag("admin_credit_${uid}"),
                    ) {
                      Text("+ Credit")
                    }
                    OutlinedButton(
                      onClick = {
                        adjustTargetUser = user
                        adjustIsCredit = false
                        adjustAmountText = ""
                      },
                      enabled = !isBusy,
                      border = BorderStroke(1.dp, GoldPrimary),
                      colors = ButtonDefaults.outlinedButtonColors(contentColor = GoldBright),
                      modifier = Modifier.weight(1f).testTag("admin_deduct_${uid}"),
                    ) {
                      Text("- Deduct")
                    }
                    if (uid != currentUid) {
                      OutlinedButton(
                        onClick = { deleteTargetUser = user },
                        enabled = !isBusy,
                        border = BorderStroke(1.dp, RoseError),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = RoseError),
                        modifier = Modifier.testTag("admin_delete_${uid}"),
                      ) {
                        Icon(Icons.Rounded.Delete, contentDescription = "Delete user", modifier = Modifier.size(18.dp))
                      }
                    }
                  }
                }
              }
            }
          }
        }
      }
      Spacer(modifier = Modifier.height(16.dp))
    }
  }

  val targetAdjust = adjustTargetUser
  if (targetAdjust != null) {
    val targetUid = targetAdjust.userId.ifBlank { targetAdjust.id }
    AlertDialog(
      onDismissRequest = { adjustTargetUser = null },
      containerColor = RoyalSurfaceDeep,
      titleContentColor = GoldBright,
      textContentColor = InkLight,
      title = {
        Text(text = if (adjustIsCredit) "Credit Wallet" else "Deduct Wallet")
      },
      text = {
        Column {
          Text(text = "Player: ${targetAdjust.username} (${targetAdjust.wallet} ETB)")
          Spacer(modifier = Modifier.height(10.dp))
          OutlinedTextField(
            value = adjustAmountText,
            onValueChange = { adjustAmountText = it.filter { ch -> ch.isDigit() } },
            label = { Text("Amount (ETB)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
              focusedBorderColor = GoldPrimary,
              unfocusedBorderColor = VioletOutline,
              focusedTextColor = InkLight,
              unfocusedTextColor = InkLight,
            ),
            modifier = Modifier.fillMaxWidth().testTag("admin_adjust_amount_input"),
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            val amt = adjustAmountText.trim().toLongOrNull() ?: 0L
            if (amt > 0L) {
              val delta = if (adjustIsCredit) amt else -amt
              val label = if (adjustIsCredit) "Admin credit" else "Admin deduction"
              onAdjustWallet(targetUid, delta, label)
              adjustTargetUser = null
            }
          },
          colors = ButtonDefaults.buttonColors(containerColor = VioletPrimary, contentColor = Color.White),
          modifier = Modifier.testTag("admin_adjust_confirm"),
        ) {
          Text("Apply")
        }
      },
      dismissButton = {
        OutlinedButton(onClick = { adjustTargetUser = null }) {
          Text(stringResource(R.string.action_cancel), color = InkMuted)
        }
      },
    )
  }

  val targetDelete = deleteTargetUser
  if (targetDelete != null) {
    val targetUid = targetDelete.userId.ifBlank { targetDelete.id }
    AlertDialog(
      onDismissRequest = { deleteTargetUser = null },
      containerColor = RoyalSurfaceDeep,
      titleContentColor = RoseError,
      textContentColor = InkLight,
      title = { Text("Delete User Profile?") },
      text = { Text("Remove ${targetDelete.username}'s profile document from Firestore?") },
      confirmButton = {
        Button(
          onClick = {
            onDeleteUser(targetUid)
            deleteTargetUser = null
          },
          colors = ButtonDefaults.buttonColors(containerColor = CrimsonDecline, contentColor = Color.White),
          modifier = Modifier.testTag("admin_delete_confirm"),
        ) {
          Text("Delete")
        }
      },
      dismissButton = {
        OutlinedButton(onClick = { deleteTargetUser = null }) {
          Text(stringResource(R.string.action_cancel), color = InkMuted)
        }
      },
    )
  }
}
