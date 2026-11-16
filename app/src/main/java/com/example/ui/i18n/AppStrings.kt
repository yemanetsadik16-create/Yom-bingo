package com.example.ui.i18n

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import com.example.util.BingoEngine

enum class AppLang(val code: String) {
  EN("EN"),
  TI("TI"),
}

val LocalAppLang = staticCompositionLocalOf { AppLang.EN }

@Composable
fun tr(en: String, ti: String): String {
  return if (LocalAppLang.current == AppLang.TI) ti else en
}

object AppStrings {
  fun brandTitle() = "YEME BINGO"

  fun roomsOpen(lang: AppLang) =
    if (lang == AppLang.TI) "ክፍልታት ክፉታት እዮም · ሕጂ ተጸንበሩ" else "Rooms are open · join now"

  fun roundRunning(lang: AppLang) =
    if (lang == AppLang.TI) "ጸወታ ይካየድ ኣሎ · ዝቕጽል ዙር ዝኽፈተሉ" else "Round running · next rooms open in"

  fun menuLabel(lang: AppLang) =
    if (lang == AppLang.TI) "ዝርዝር መውጽኢ" else "Menu"

  fun cloudConnected(lang: AppLang) =
    if (lang == AppLang.TI) "☁ ምስ ክላውድ ተራኺቡ" else "☁ Cloud connected"

  fun navHome(lang: AppLang) = if (lang == AppLang.TI) "መበገሲ" else "Home"
  fun navRooms(lang: AppLang) = if (lang == AppLang.TI) "መጻወቲ ክፍልታት" else "Stake Rooms"
  fun navWallet(lang: AppLang) = if (lang == AppLang.TI) "ዋሌት" else "Wallet"
  fun navRegister(lang: AppLang) = if (lang == AppLang.TI) "ተመዝገብ" else "Register"
  fun navHowToPlay(lang: AppLang) = if (lang == AppLang.TI) "ኣመጻውታ" else "How to Play"
  fun navFaqs(lang: AppLang) = if (lang == AppLang.TI) "ተደጋጋሚ ሕቶታት" else "FAQs"
  fun navAdmin(lang: AppLang) = if (lang == AppLang.TI) "ምምሕዳር" else "Admin"
  fun navSignOut(lang: AppLang) = if (lang == AppLang.TI) "ውጻእ" else "Sign Out"

  fun loginTitle(lang: AppLang) = if (lang == AppLang.TI) "እቶ" else "Login"
  fun loginWithGoogle(lang: AppLang) = if (lang == AppLang.TI) "ብ Google እቶ" else "Login with Google"
  fun loginSubtitle(lang: AppLang) =
    if (lang == AppLang.TI) "ብ Google ኣካውንትኩም ብደሓን እተዉ። ዋሌትኩምን ጸወታኹምን ብቀጥታ ይስምሩ።"
    else "Sign in securely with your Google account to sync your ETB wallet and join live multiplayer rooms."

  fun registerButton(lang: AppLang) = if (lang == AppLang.TI) "ተመዝገብ" else "Register"
  fun userRegistrationTitle(lang: AppLang) =
    if (lang == AppLang.TI) "ምዝገባ ተጠቃሚ" else "User Registration"
  fun registerWithGoogle(lang: AppLang) =
    if (lang == AppLang.TI) "ብ Google ተመዝገብ" else "Register with Google"
  fun labelUsername(lang: AppLang) = if (lang == AppLang.TI) "ስም ተጠቃሚ" else "Username"
  fun hintUsername(lang: AppLang) = if (lang == AppLang.TI) "ስም ተጠቃሚ ኣእቱ" else "Enter Username"
  fun labelPhone(lang: AppLang) = if (lang == AppLang.TI) "ቁጽሪ ስልኪ" else "Phone No"
  fun hintPhone(lang: AppLang) = if (lang == AppLang.TI) "ቁጽሪ ስልኪ ኣእቱ" else "Enter Phone Number"
  fun haveAccountPrefix(lang: AppLang) = if (lang == AppLang.TI) "ኣካውንት ኣለካ?" else "Have an account"

  fun authRequiredRooms(lang: AppLang) =
    if (lang == AppLang.TI) "ናብ ክፍሊ ንምእታው መጀመርታ ብ Google እቶ።" else "Log in with Google to join a room."
  fun authRequiredWallet(lang: AppLang) =
    if (lang == AppLang.TI) "ዋሌትካ ንምርኣይ መጀመርታ ብ Google እቶ።" else "Log in with Google to see your wallet."

  fun welcomePlayer(lang: AppLang, name: String) =
    if (lang == AppLang.TI) "እንቋዕ ብደሓን መጻእካ፣ $name" else "Welcome, $name"
  fun walletLabel(lang: AppLang) = if (lang == AppLang.TI) "ዋሌት" else "Wallet"
  fun walletBalanceLabel(lang: AppLang) =
    if (lang == AppLang.TI) "ዝተረፈ ገንዘብ" else "Wallet balance"
  fun quickDeposit(lang: AppLang) = if (lang == AppLang.TI) "+ ገንዘብ ኣእቱ" else "+ Deposit"
  fun playStakeRooms(lang: AppLang) =
    if (lang == AppLang.TI) "ኣብ መጻወቲ ክፍልታት ተጻወት" else "Play in Stake Rooms"
  fun editProfile(lang: AppLang) = if (lang == AppLang.TI) "ፕሮፋይል ኣስተኻኽል" else "Edit Profile"
  fun saveProfile(lang: AppLang) = if (lang == AppLang.TI) "ፕሮፋይል ዓቅብ" else "Save Profile"
  fun cancel(lang: AppLang) = if (lang == AppLang.TI) "ሰርዝ" else "Cancel"
  fun back(lang: AppLang) = if (lang == AppLang.TI) "ተመለስ" else "Back"
  fun ok(lang: AppLang) = if (lang == AppLang.TI) "ሕራይ" else "OK"

  fun joinRoom(lang: AppLang) = if (lang == AppLang.TI) "ናብ ክፍሊ እቶ" else "JOIN ROOM"
  fun joinedBadge(lang: AppLang) = if (lang == AppLang.TI) "✔ ተጸንቢርካ" else "✔ JOINED"
  fun runningLabel(lang: AppLang) = if (lang == AppLang.TI) "ይካየድ ኣሎ" else "Running"
  fun playersCount(lang: AppLang, count: Int, max: Int) =
    if (lang == AppLang.TI) "$count/$max ተጻወትቲ" else "$count/$max Players"
  fun prizeLabel(lang: AppLang, prize: Long) =
    if (lang == AppLang.TI) "ሽልማት: $prize ETB" else "Prize: $prize ETB"
  fun stakePerTicket(lang: AppLang, stake: Int) =
    if (lang == AppLang.TI) "መደብ: $stake ETB / ካርቴላ" else "Stake: $stake ETB / Ticket"
  fun depositMoney(lang: AppLang) = if (lang == AppLang.TI) "ገንዘብ ኣእቱ" else "Deposit money"

  fun targetGoal(lang: AppLang) = if (lang == AppLang.TI) "ዕላማ ጸወታ:" else "Target Goal:"
  fun bingoBtn(lang: AppLang) = if (lang == AppLang.TI) "ቢንጎ!" else "BINGO!"
  fun autoMark(lang: AppLang) = if (lang == AppLang.TI) "ባዕሉ ይምልከት" else "Auto-mark"
  fun soundOn(lang: AppLang) = if (lang == AppLang.TI) "ድምጺ ክፉት" else "Sound on"
  fun soundOff(lang: AppLang) = if (lang == AppLang.TI) "ድምጺ ዕጹው" else "Sound off"
  fun waitingFirstCall(lang: AppLang) =
    if (lang == AppLang.TI) "ንመጀመርታ ጻውዒት ይጽበ ኣሎ…" else "Waiting for the first call…"
  fun noActiveGame(lang: AppLang) =
    if (lang == AppLang.TI) "ንሕጂ ዝካየድ ጸወታ ቢንጎ የለን።" else "No active Bingo game for this round."
  fun backToRooms(lang: AppLang) = if (lang == AppLang.TI) "ናብ ክፍልታት ተመለስ" else "Back to rooms"

  fun deposit(lang: AppLang) = if (lang == AppLang.TI) "ገንዘብ ኣእቱ" else "Deposit"
  fun withdraw(lang: AppLang) = if (lang == AppLang.TI) "ገንዘብ ኣውጽእ" else "Withdraw"
  fun addMoneyTitle(lang: AppLang) =
    if (lang == AppLang.TI) "ገንዘብ ወስኽ (Telebirr)" else "Add Money (Telebirr)"
  fun withdrawMoneyTitle(lang: AppLang) =
    if (lang == AppLang.TI) "ገንዘብ ኣውጽእ" else "Withdraw Money"
  fun amountPaidLabel(lang: AppLang) =
    if (lang == AppLang.TI) "ዝተኸፍለ መጠን (ብር)" else "Amount paid (birr)"
  fun receiptLinkLabel(lang: AppLang) =
    if (lang == AppLang.TI) "ናይ Telebirr ሪሲት ሊንክ ወይ ቁጽሪ" else "Telebirr receipt link or number"
  fun submitPayment(lang: AppLang) =
    if (lang == AppLang.TI) "ክፍሊት ስደድ" else "Submit payment"
  fun depositAdminNote(lang: AppLang) =
    if (lang == AppLang.TI) "ኣድሚን ነቲ ክፍሊት ምስ ኣረጋገጸ ናብ ዋሌትካ ይውሰኽ።"
    else "The admin checks your payment and credits your wallet once approved."
  fun withdrawAmountLabel(lang: AppLang) =
    if (lang == AppLang.TI) "ዘውጽእ መጠን (ብር)" else "Amount to withdraw (birr)"
  fun telebirrAccountLabel(lang: AppLang) =
    if (lang == AppLang.TI) "ቁጽሪ ኣካውንት Telebirr" else "Telebirr account number"
  fun requestWithdrawal(lang: AppLang) =
    if (lang == AppLang.TI) "ሕቶ ምውጻእ ስደድ" else "Request withdrawal"
  fun withdrawHoldNote(lang: AppLang) =
    if (lang == AppLang.TI) "እቲ ገንዘብ ሕጂ ካብ ዋሌትካ ይተሓዝ። እንተተነጺጉ ይምለስ።"
    else "The amount is held from your wallet now. If declined, it is returned."
  fun paymentRequests(lang: AppLang) =
    if (lang == AppLang.TI) "ሕቶታት ክፍሊት" else "Payment requests"
  fun withdrawalsSection(lang: AppLang) =
    if (lang == AppLang.TI) "ምውጻእ ገንዘብ" else "Withdrawals"
  fun recentTransactions(lang: AppLang) =
    if (lang == AppLang.TI) "ቀረባ ግብይታት" else "Recent transactions"
  fun noTransactions(lang: AppLang) =
    if (lang == AppLang.TI) "ክሳብ ሕጂ ግብይት የለን።" else "No transactions yet."

  fun payWithTelebirrTo(lang: AppLang) =
    if (lang == AppLang.TI) "ብ Telebirr ናብዚ ስደዱ\n${BingoEngine.TELEBIRR_MERCHANT_NAME} : ${BingoEngine.TELEBIRR_MERCHANT_NUMBER}"
    else "Pay with Telebirr to\n${BingoEngine.TELEBIRR_MERCHANT_NAME} : ${BingoEngine.TELEBIRR_MERCHANT_NUMBER}"
}
