package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.automirrored.rounded.Login
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Casino
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import com.example.auth.attemptAutoSignIn
import com.example.auth.onGoogleSignInClicked
import com.example.ui.components.RoyalBackground
import com.example.ui.components.RoyalPanelCard
import com.example.ui.components.YemeDrawerHeader
import com.example.ui.components.YemeGradientButton
import com.example.ui.components.YemeTopBar
import com.example.ui.i18n.AppLang
import com.example.ui.i18n.AppStrings
import com.example.ui.i18n.LocalAppLang
import com.example.ui.theme.GoldBright
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.InkLight
import com.example.ui.theme.InkMuted
import com.example.ui.theme.InkOnGold
import com.example.ui.theme.PlayfairDisplayFontFamily
import com.example.ui.theme.RoseError
import com.example.ui.theme.RoyalSurfaceDeep
import com.example.ui.theme.VioletOutline
import com.example.util.BingoEngine
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

private enum class UnauthTab { LOGIN, REGISTER, ROOMS, WALLET, HOW, FAQ }

@Composable
fun AuthSignInScreen(
  currentLang: AppLang,
  onToggleLanguage: () -> Unit,
  onPreparePendingProfile: (username: String?, phone: String?) -> Unit,
  onAuthSuccess: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val credentialManager = remember(context) { CredentialManager.create(context) }
  val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
  val lang = LocalAppLang.current

  var activeTab by rememberSaveable { mutableStateOf(UnauthTab.LOGIN) }
  var regUsername by rememberSaveable { mutableStateOf("") }
  var regPhone by rememberSaveable { mutableStateOf("") }
  var isLoading by remember { mutableStateOf(false) }
  var errorMessage by remember { mutableStateOf<String?>(null) }
  var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }

  LaunchedEffect(Unit) {
    attemptAutoSignIn(
      context = context,
      credentialManager = credentialManager,
      onAuthSuccess = onAuthSuccess,
      onUnauthenticated = {},
      scope = scope,
    )
    while (isActive) {
      nowMs = System.currentTimeMillis()
      delay(1000L)
    }
  }

  val cycle = BingoEngine.currentCycle(nowMs)
  val countdownText = if (cycle.isLobbyOpen) {
    BingoEngine.formatMmSs(cycle.msUntilGameStart)
  } else {
    BingoEngine.formatMmSs(cycle.msUntilNextCycle)
  }

  fun launchGoogleAuth(pendingName: String?, pendingPhone: String?) {
    errorMessage = null
    onPreparePendingProfile(pendingName, pendingPhone)
    isLoading = true
    onGoogleSignInClicked(
      context = context,
      credentialManager = credentialManager,
      onAuthSuccess = {
        isLoading = false
        onAuthSuccess()
      },
      onAuthError = { msg ->
        isLoading = false
        onPreparePendingProfile(null, null)
        errorMessage = msg
      },
      scope = scope,
      onAuthCancelled = {
        isLoading = false
        onPreparePendingProfile(null, null)
      },
    )
  }

  ModalNavigationDrawer(
    drawerState = drawerState,
    drawerContent = {
      ModalDrawerSheet(
        drawerContainerColor = RoyalSurfaceDeep,
        drawerContentColor = InkLight,
        modifier = Modifier.width(280.dp),
      ) {
        YemeDrawerHeader(userLabel = "☰ ${AppStrings.menuLabel(lang)}")
        val items = listOf(
          Triple(UnauthTab.LOGIN, AppStrings.navHome(lang), Icons.Rounded.Home),
          Triple(UnauthTab.ROOMS, AppStrings.navRooms(lang), Icons.Rounded.Casino),
          Triple(UnauthTab.WALLET, AppStrings.navWallet(lang), Icons.Rounded.AccountBalanceWallet),
          Triple(UnauthTab.REGISTER, AppStrings.navRegister(lang), Icons.Rounded.PersonAdd),
          Triple(UnauthTab.HOW, AppStrings.navHowToPlay(lang), Icons.Rounded.Info),
          Triple(UnauthTab.FAQ, AppStrings.navFaqs(lang), Icons.AutoMirrored.Rounded.HelpOutline),
        )
        items.forEach { (tab, label, icon) ->
          val selected = activeTab == tab
          NavigationDrawerItem(
            label = {
              Text(
                text = label,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                fontSize = 17.sp,
              )
            },
            selected = selected,
            onClick = {
              errorMessage = null
              activeTab = tab
              scope.launch { drawerState.close() }
            },
            icon = { Icon(icon, contentDescription = null) },
            colors = NavigationDrawerItemDefaults.colors(
              selectedContainerColor = GoldPrimary,
              selectedIconColor = InkOnGold,
              selectedTextColor = InkOnGold,
              unselectedContainerColor = Color.Transparent,
              unselectedIconColor = InkMuted,
              unselectedTextColor = InkLight,
            ),
            modifier = Modifier
              .padding(horizontal = 10.dp, vertical = 2.dp)
              .testTag("unauth_drawer_${tab.name.lowercase()}"),
          )
        }
      }
    },
  ) {
    Scaffold(
      modifier = modifier.fillMaxSize(),
      containerColor = Color.Transparent,
      contentWindowInsets = WindowInsets.navigationBars,
      topBar = {
        YemeTopBar(
          isDrawerOpen = drawerState.isOpen,
          currentLang = currentLang,
          onToggleLanguage = onToggleLanguage,
          onBrandClick = {
            errorMessage = null
            activeTab = UnauthTab.LOGIN
          },
          onMenuToggle = {
            scope.launch {
              if (drawerState.isOpen) drawerState.close() else drawerState.open()
            }
          },
        )
      },
    ) { innerPadding ->
      RoyalBackground(modifier = Modifier.padding(innerPadding)) {
        when (activeTab) {
          UnauthTab.HOW -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
              HowToPlayScreen()
            }
          }

          UnauthTab.FAQ -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
              FaqScreen()
            }
          }

          UnauthTab.ROOMS, UnauthTab.WALLET -> {
            Column(
              modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 20.dp)
                .widthIn(max = 500.dp)
                .align(Alignment.TopCenter),
              horizontalAlignment = Alignment.CenterHorizontally,
            ) {
              RoyalPanelCard {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                  Text(
                    text = if (activeTab == UnauthTab.ROOMS) AppStrings.navRooms(lang) else AppStrings.navWallet(lang),
                    fontFamily = PlayfairDisplayFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 28.sp,
                    color = GoldBright,
                  )
                  Spacer(modifier = Modifier.height(10.dp))
                  HorizontalDivider(color = GoldPrimary.copy(alpha = 0.28f))
                  Spacer(modifier = Modifier.height(14.dp))
                  Text(
                    text = if (activeTab == UnauthTab.ROOMS) {
                      AppStrings.authRequiredRooms(lang)
                    } else {
                      AppStrings.authRequiredWallet(lang)
                    },
                    color = InkMuted,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyLarge,
                  )
                  Spacer(modifier = Modifier.height(18.dp))
                  YemeGradientButton(
                    text = AppStrings.loginTitle(lang),
                    icon = Icons.AutoMirrored.Rounded.Login,
                    onClick = { activeTab = UnauthTab.LOGIN },
                    modifier = Modifier.testTag("unauth_prompt_login_button"),
                  )
                }
              }
            }
          }

          UnauthTab.LOGIN -> {
            Column(
              modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 14.dp)
                .widthIn(max = 500.dp)
                .align(Alignment.TopCenter),
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
                text = if (cycle.isLobbyOpen) {
                  AppStrings.roomsOpen(lang)
                } else {
                  AppStrings.roundRunning(lang)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = InkMuted,
                textAlign = TextAlign.Center,
              )

              Spacer(modifier = Modifier.height(16.dp))

              RoyalPanelCard {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                  Icon(
                    imageVector = Icons.Rounded.Person,
                    contentDescription = null,
                    tint = GoldPrimary,
                    modifier = Modifier.size(72.dp),
                  )
                  Spacer(modifier = Modifier.height(6.dp))
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                  ) {
                    Icon(
                      imageVector = Icons.AutoMirrored.Rounded.Login,
                      contentDescription = null,
                      tint = GoldBright,
                      modifier = Modifier.size(26.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                      text = AppStrings.loginTitle(lang),
                      fontFamily = PlayfairDisplayFontFamily,
                      fontWeight = FontWeight.Bold,
                      fontSize = 28.sp,
                      color = GoldBright,
                    )
                  }
                  Spacer(modifier = Modifier.height(10.dp))
                  HorizontalDivider(color = GoldPrimary.copy(alpha = 0.28f))
                  Spacer(modifier = Modifier.height(16.dp))

                  Text(
                    text = AppStrings.loginSubtitle(lang),
                    style = MaterialTheme.typography.bodyMedium,
                    color = InkLight,
                    textAlign = TextAlign.Center,
                  )

                  if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                      text = errorMessage!!,
                      color = RoseError,
                      style = MaterialTheme.typography.bodyMedium,
                      fontWeight = FontWeight.Bold,
                      textAlign = TextAlign.Center,
                    )
                  }

                  Spacer(modifier = Modifier.height(20.dp))

                  YemeGradientButton(
                    text = AppStrings.loginWithGoogle(lang),
                    icon = Icons.AutoMirrored.Rounded.Login,
                    isLoading = isLoading,
                    onClick = { launchGoogleAuth(null, null) },
                    modifier = Modifier.testTag("google_sign_in_button"),
                  )
                }
              }

              Spacer(modifier = Modifier.height(14.dp))

              YemeGradientButton(
                text = AppStrings.registerButton(lang),
                icon = Icons.Rounded.Add,
                isGold = true,
                enabled = !isLoading,
                onClick = {
                  errorMessage = null
                  activeTab = UnauthTab.REGISTER
                },
                modifier = Modifier.testTag("open_register_button"),
              )
            }
          }

          UnauthTab.REGISTER -> {
            Column(
              modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 14.dp)
                .widthIn(max = 500.dp)
                .align(Alignment.TopCenter),
              horizontalAlignment = Alignment.CenterHorizontally,
            ) {
              RoyalPanelCard {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                  Icon(
                    imageVector = Icons.Rounded.Person,
                    contentDescription = null,
                    tint = GoldPrimary,
                    modifier = Modifier.size(72.dp),
                  )
                  Spacer(modifier = Modifier.height(6.dp))
                  Text(
                    text = AppStrings.userRegistrationTitle(lang),
                    fontFamily = PlayfairDisplayFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 27.sp,
                    color = GoldBright,
                    textAlign = TextAlign.Center,
                  )
                  Spacer(modifier = Modifier.height(10.dp))
                  HorizontalDivider(color = GoldPrimary.copy(alpha = 0.28f))
                  Spacer(modifier = Modifier.height(16.dp))

                  OutlinedTextField(
                    value = regUsername,
                    onValueChange = { regUsername = it; errorMessage = null },
                    label = { Text(AppStrings.labelUsername(lang)) },
                    placeholder = { Text(AppStrings.hintUsername(lang)) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                      focusedBorderColor = GoldPrimary,
                      unfocusedBorderColor = VioletOutline,
                      focusedTextColor = InkLight,
                      unfocusedTextColor = InkLight,
                      focusedLabelColor = GoldBright,
                      unfocusedLabelColor = InkMuted,
                      focusedPlaceholderColor = InkMuted.copy(alpha = 0.7f),
                      unfocusedPlaceholderColor = InkMuted.copy(alpha = 0.7f),
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                      .fillMaxWidth()
                      .testTag("register_username_input"),
                  )

                  Spacer(modifier = Modifier.height(14.dp))

                  OutlinedTextField(
                    value = regPhone,
                    onValueChange = { regPhone = it; errorMessage = null },
                    label = { Text(AppStrings.labelPhone(lang)) },
                    placeholder = { Text(AppStrings.hintPhone(lang)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    colors = OutlinedTextFieldDefaults.colors(
                      focusedBorderColor = GoldPrimary,
                      unfocusedBorderColor = VioletOutline,
                      focusedTextColor = InkLight,
                      unfocusedTextColor = InkLight,
                      focusedLabelColor = GoldBright,
                      unfocusedLabelColor = InkMuted,
                      focusedPlaceholderColor = InkMuted.copy(alpha = 0.7f),
                      unfocusedPlaceholderColor = InkMuted.copy(alpha = 0.7f),
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                      .fillMaxWidth()
                      .testTag("register_phone_input"),
                  )

                  if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                      text = errorMessage!!,
                      color = RoseError,
                      style = MaterialTheme.typography.bodyMedium,
                      fontWeight = FontWeight.Bold,
                      textAlign = TextAlign.Center,
                    )
                  }

                  Spacer(modifier = Modifier.height(20.dp))

                  YemeGradientButton(
                    text = AppStrings.registerWithGoogle(lang),
                    icon = Icons.Rounded.Add,
                    isLoading = isLoading,
                    onClick = {
                      val cleanUser = regUsername.trim()
                      val cleanPhone = BingoEngine.cleanPhone(regPhone)
                      when {
                        cleanUser.length < 3 -> {
                          errorMessage = "Username must be at least 3 characters."
                        }
                        cleanPhone.isNotEmpty() && !Regex("^\\+?\\d{9,13}$").matches(cleanPhone) -> {
                          errorMessage = "Enter a valid phone number."
                        }
                        else -> launchGoogleAuth(cleanUser, cleanPhone.ifEmpty { null })
                      }
                    },
                    modifier = Modifier.testTag("register_submit_button"),
                  )

                  Spacer(modifier = Modifier.height(16.dp))

                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                  ) {
                    Icon(
                      imageVector = Icons.Rounded.Info,
                      contentDescription = null,
                      tint = InkMuted,
                      modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                      text = AppStrings.haveAccountPrefix(lang),
                      color = InkLight,
                      fontSize = 16.sp,
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                      text = AppStrings.loginTitle(lang),
                      color = GoldBright,
                      fontWeight = FontWeight.Bold,
                      fontSize = 16.sp,
                      textDecoration = TextDecoration.Underline,
                      modifier = Modifier
                        .clickable {
                          errorMessage = null
                          activeTab = UnauthTab.LOGIN
                        }
                        .padding(vertical = 8.dp, horizontal = 4.dp)
                        .testTag("switch_to_login_link"),
                    )
                  }
                }
              }
            }
          }
        }
      }
    }
  }
}
