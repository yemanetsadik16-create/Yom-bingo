package com.example

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.Casino
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.auth.authStateFlow
import com.example.auth.signOut
import com.example.data.repository.BingoRepository
import com.example.model.UiState
import com.example.ui.components.ModalNoteDialog
import com.example.ui.components.RoyalBackground
import com.example.ui.components.YemeDrawerHeader
import com.example.ui.components.YemeTopBar
import com.example.ui.i18n.AppLang
import com.example.ui.i18n.AppStrings
import com.example.ui.i18n.LocalAppLang
import com.example.ui.screens.AdminScreen
import com.example.ui.screens.AuthSignInScreen
import com.example.ui.screens.BingoGameScreen
import com.example.ui.screens.FaqScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.HowToPlayScreen
import com.example.ui.screens.StakeRoomsScreen
import com.example.ui.screens.WalletScreen
import com.example.ui.theme.GoldBright
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.InkLight
import com.example.ui.theme.InkMuted
import com.example.ui.theme.InkOnGold
import com.example.ui.theme.RoseError
import com.example.ui.theme.RoyalHeaderStart
import com.example.ui.theme.RoyalSurfaceDeep
import com.example.ui.theme.VioletLine
import com.example.ui.theme.YemeBingoTheme
import com.example.viewmodel.YemeBingoViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import kotlinx.coroutines.launch

const val ROUTE_HOME = "home"
const val ROUTE_ROOMS = "rooms"
const val ROUTE_GAME = "game"
const val ROUTE_WALLET = "wallet"
const val ROUTE_HOW = "how_to_play"
const val ROUTE_FAQ = "faqs"
const val ROUTE_ADMIN = "admin"

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      YemeBingoTheme {
        AppNavigation()
      }
    }
  }
}

@Composable
fun AppNavigation() {
  val context = LocalContext.current
  val prefs = remember(context) {
    context.applicationContext.getSharedPreferences("yeme_bingo_prefs", Context.MODE_PRIVATE)
  }
  var currentLang by remember {
    val saved = prefs.getString("lang", AppLang.EN.code)
    mutableStateOf(if (saved == AppLang.TI.code) AppLang.TI else AppLang.EN)
  }
  val toggleLang: () -> Unit = {
    val next = if (currentLang == AppLang.EN) AppLang.TI else AppLang.EN
    currentLang = next
    prefs.edit().putString("lang", next.code).apply()
  }

  var pendingUsername by remember { mutableStateOf<String?>(null) }
  var pendingPhone by remember { mutableStateOf<String?>(null) }

  val currentUser by Firebase.auth.authStateFlow()
    .collectAsStateWithLifecycle(initialValue = Firebase.auth.currentUser)

  CompositionLocalProvider(LocalAppLang provides currentLang) {
    if (currentUser == null) {
      AuthSignInScreen(
        currentLang = currentLang,
        onToggleLanguage = toggleLang,
        onPreparePendingProfile = { name, phone ->
          pendingUsername = name
          pendingPhone = phone
        },
        onAuthSuccess = { /* AuthStateListener transitions automatically */ },
      )
    } else {
      val user = currentUser!!
      key(user.uid) {
        AuthenticatedYemeApp(
          user = user,
          currentLang = currentLang,
          onToggleLanguage = toggleLang,
          preferredUsername = pendingUsername,
          preferredPhone = pendingPhone,
        )
      }
    }
  }
}

private data class NavDestinationItem(
  val route: String,
  val label: String,
  val icon: ImageVector,
  val tag: String,
)

@Composable
private fun AuthenticatedYemeApp(
  user: FirebaseUser,
  currentLang: AppLang,
  onToggleLanguage: () -> Unit,
  preferredUsername: String?,
  preferredPhone: String?,
) {
  val context = LocalContext.current
  val appContext = context.applicationContext
  val viewModel: YemeBingoViewModel = viewModel(
    key = user.uid,
    factory = viewModelFactory {
      initializer {
        val repository = BingoRepository(appContext)
        YemeBingoViewModel(
          repository = repository,
          currentUserId = user.uid,
          userDisplayName = user.displayName,
          userEmail = user.email,
          preferredUsername = preferredUsername,
          preferredPhone = preferredPhone,
        )
      }
    },
  )

  val navController = rememberNavController()
  val navBackStackEntry by navController.currentBackStackEntryAsState()
  val currentRoute = navBackStackEntry?.destination?.route ?: ROUTE_HOME

  val profileState by viewModel.userProfile.collectAsStateWithLifecycle()
  val roundCycle by viewModel.roundCycle.collectAsStateWithLifecycle()
  val roundsForCycle by viewModel.roundsForCycle.collectAsStateWithLifecycle()
  val selectedRoom by viewModel.selectedRoom.collectAsStateWithLifecycle()
  val selectedTicket by viewModel.selectedTicketNumber.collectAsStateWithLifecycle()
  val takenTickets by viewModel.takenTicketsInPickedRoom.collectAsStateWithLifecycle()
  val activeTicket by viewModel.activeTicket.collectAsStateWithLifecycle()
  val liveGameState by viewModel.liveGameState.collectAsStateWithLifecycle()
  val gameInlineError by viewModel.gameInlineError.collectAsStateWithLifecycle()
  val transactions by viewModel.transactions.collectAsStateWithLifecycle()
  val myPayments by viewModel.myPayments.collectAsStateWithLifecycle()
  val myWithdrawals by viewModel.myWithdrawals.collectAsStateWithLifecycle()
  val adminPayments by viewModel.adminPayments.collectAsStateWithLifecycle()
  val adminWithdrawals by viewModel.adminWithdrawals.collectAsStateWithLifecycle()
  val adminUsers by viewModel.adminUsers.collectAsStateWithLifecycle()
  val isBusy by viewModel.isBusy.collectAsStateWithLifecycle()
  val feedbackMessage by viewModel.feedbackMessage.collectAsStateWithLifecycle()
  val modalMessage by viewModel.modalDialogMessage.collectAsStateWithLifecycle()
  val navigateToGameEvent by viewModel.navigateToGameEvent.collectAsStateWithLifecycle()

  val snackbarHostState = remember { SnackbarHostState() }
  val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
  val scope = rememberCoroutineScope()
  val credentialManager = remember(context) { CredentialManager.create(context) }

  val walletEtb = (profileState as? UiState.Success)?.data?.wallet ?: 0L

  val navigateTopLevel: (String) -> Unit = { route ->
    if (currentRoute != route) {
      navController.navigate(route) {
        popUpTo(navController.graph.findStartDestination().id) {
          saveState = true
        }
        launchSingleTop = true
        restoreState = true
      }
    }
  }

  LaunchedEffect(feedbackMessage) {
    val msg = feedbackMessage
    if (msg != null) {
      snackbarHostState.showSnackbar(msg)
      viewModel.consumeFeedback()
    }
  }

  LaunchedEffect(navigateToGameEvent) {
    if (navigateToGameEvent != null) {
      viewModel.consumeNavigateToGameEvent()
      if (currentRoute != ROUTE_GAME) {
        navController.navigate(ROUTE_GAME) {
          launchSingleTop = true
        }
      }
    }
  }

  val navItems = remember(viewModel.isAdmin, currentLang) {
    buildList {
      add(NavDestinationItem(ROUTE_HOME, AppStrings.navHome(currentLang), Icons.Rounded.Home, "drawer_home"))
      add(NavDestinationItem(ROUTE_ROOMS, AppStrings.navRooms(currentLang), Icons.Rounded.Casino, "drawer_rooms"))
      add(NavDestinationItem(ROUTE_WALLET, AppStrings.navWallet(currentLang), Icons.Rounded.AccountBalanceWallet, "drawer_wallet"))
      add(NavDestinationItem(ROUTE_HOW, AppStrings.navHowToPlay(currentLang), Icons.Rounded.Info, "drawer_how"))
      add(NavDestinationItem(ROUTE_FAQ, AppStrings.navFaqs(currentLang), Icons.AutoMirrored.Rounded.HelpOutline, "drawer_faqs"))
      if (viewModel.isAdmin) {
        add(NavDestinationItem(ROUTE_ADMIN, AppStrings.navAdmin(currentLang), Icons.Rounded.AdminPanelSettings, "drawer_admin"))
      }
    }
  }

  BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
    val isExpanded = maxWidth >= 840.dp

    val scaffoldContent: @Composable () -> Unit = {
      Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets.navigationBars,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
          YemeTopBar(
            isDrawerOpen = drawerState.isOpen,
            currentLang = currentLang,
            onToggleLanguage = onToggleLanguage,
            onBrandClick = { navigateTopLevel(ROUTE_HOME) },
            showMenuButton = !isExpanded,
            onMenuToggle = {
              if (!isExpanded) {
                scope.launch {
                  if (drawerState.isOpen) drawerState.close() else drawerState.open()
                }
              }
            },
          )
        },
      ) { innerPadding ->
        RoyalBackground(modifier = Modifier.padding(innerPadding)) {
          Row(modifier = Modifier.fillMaxSize()) {
            if (isExpanded) {
              NavigationRail(
                containerColor = RoyalHeaderStart,
                contentColor = GoldBright,
                modifier = Modifier.fillMaxHeight(),
              ) {
                Spacer(modifier = Modifier.height(8.dp))
                navItems.forEach { item ->
                  NavigationRailItem(
                    selected = currentRoute == item.route,
                    onClick = { navigateTopLevel(item.route) },
                    icon = { Icon(item.icon, contentDescription = item.label) },
                    label = { Text(item.label, maxLines = 1) },
                    colors = NavigationRailItemDefaults.colors(
                      selectedIconColor = InkOnGold,
                      selectedTextColor = GoldBright,
                      indicatorColor = GoldPrimary,
                      unselectedIconColor = InkMuted,
                      unselectedTextColor = InkMuted,
                    ),
                    modifier = Modifier.testTag("rail_${item.route}"),
                  )
                }
                Spacer(modifier = Modifier.weight(1f))
                NavigationRailItem(
                  selected = false,
                  onClick = { signOut(credentialManager = credentialManager, scope = scope) },
                  icon = {
                    Icon(
                      Icons.AutoMirrored.Rounded.Logout,
                      contentDescription = AppStrings.navSignOut(currentLang),
                    )
                  },
                  label = { Text(AppStrings.navSignOut(currentLang), maxLines = 1) },
                  colors = NavigationRailItemDefaults.colors(
                    unselectedIconColor = RoseError,
                    unselectedTextColor = RoseError,
                  ),
                  modifier = Modifier.testTag("rail_sign_out"),
                )
              }
            }

            Box(
              modifier = Modifier
                .weight(1f)
                .fillMaxSize(),
              contentAlignment = Alignment.TopCenter,
            ) {
              NavHost(
                navController = navController,
                startDestination = ROUTE_HOME,
                modifier = Modifier.fillMaxSize(),
              ) {
                composable(ROUTE_HOME) {
                  Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                    HomeScreen(
                      profileState = profileState,
                      roundCycle = roundCycle,
                      isAdmin = viewModel.isAdmin,
                      isBusy = isBusy,
                      onPlayRooms = { navigateTopLevel(ROUTE_ROOMS) },
                      onOpenWallet = { navigateTopLevel(ROUTE_WALLET) },
                      onOpenHowToPlay = { navigateTopLevel(ROUTE_HOW) },
                      onOpenFaq = { navigateTopLevel(ROUTE_FAQ) },
                      onOpenAdmin = { navigateTopLevel(ROUTE_ADMIN) },
                      onSaveProfile = { username, phone -> viewModel.updateProfile(username, phone) },
                    )
                  }
                }

                composable(ROUTE_ROOMS) {
                  Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                    StakeRoomsScreen(
                      walletEtb = walletEtb,
                      roundCycle = roundCycle,
                      roundsByStake = roundsForCycle,
                      activeTicket = activeTicket,
                      selectedRoom = selectedRoom,
                      selectedTicketNumber = selectedTicket,
                      takenTickets = takenTickets,
                      isBusy = isBusy,
                      onOpenDeposit = { navigateTopLevel(ROUTE_WALLET) },
                      onPickRoom = { room -> viewModel.openRoomPicker(room) },
                      onClosePicker = { viewModel.closeRoomPicker() },
                      onSelectTicket = { ticketNum -> viewModel.selectTicketNumber(ticketNum) },
                      onConfirmJoin = { viewModel.joinPickedRoom() },
                      onEnterRunningGame = {
                        navController.navigate(ROUTE_GAME) { launchSingleTop = true }
                      },
                    )
                  }
                }

                composable(ROUTE_GAME) {
                  Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                    BingoGameScreen(
                      gameState = liveGameState,
                      inlineError = gameInlineError,
                      isBusy = isBusy,
                      onCellClick = { col, row -> viewModel.toggleCellMark(col, row) },
                      onToggleAutoMark = { enabled -> viewModel.setAutoMark(enabled) },
                      onToggleSound = { viewModel.toggleSound() },
                      onClaimBingo = { viewModel.claimBingo() },
                      onBackToRooms = {
                        viewModel.leaveGameToRooms()
                        navController.navigate(ROUTE_ROOMS) {
                          popUpTo(ROUTE_GAME) { inclusive = true }
                          launchSingleTop = true
                        }
                      },
                    )
                  }
                }

                composable(ROUTE_WALLET) {
                  Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                    WalletScreen(
                      walletEtb = walletEtb,
                      transactions = transactions,
                      myPayments = myPayments,
                      myWithdrawals = myWithdrawals,
                      isBusy = isBusy,
                      onSubmitDeposit = { amount, receipt, reset ->
                        viewModel.submitDeposit(amount, receipt, reset)
                      },
                      onSubmitWithdrawal = { amount, account, reset ->
                        viewModel.submitWithdrawal(amount, account, reset)
                      },
                    )
                  }
                }

                composable(ROUTE_HOW) {
                  Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                    HowToPlayScreen()
                  }
                }

                composable(ROUTE_FAQ) {
                  Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                    FaqScreen()
                  }
                }

                composable(ROUTE_ADMIN) {
                  Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                    AdminScreen(
                      isAdmin = viewModel.isAdmin,
                      currentUid = user.uid,
                      payments = adminPayments,
                      withdrawals = adminWithdrawals,
                      users = adminUsers,
                      isBusy = isBusy,
                      onApprovePayment = { id, amt -> viewModel.approvePayment(id, amt) },
                      onDeclinePayment = { id -> viewModel.declinePayment(id) },
                      onMarkWithdrawalPaid = { id -> viewModel.markWithdrawalPaid(id) },
                      onDeclineRefundWithdrawal = { id -> viewModel.declineAndRefundWithdrawal(id) },
                      onAdjustWallet = { targetUid, delta, label ->
                        viewModel.adminAdjustUserWallet(targetUid, delta, label)
                      },
                      onDeleteUser = { targetUid -> viewModel.adminDeleteUser(targetUid) },
                    )
                  }
                }
              }
            }
          }
        }
      }
    }

    if (isExpanded) {
      scaffoldContent()
    } else {
      val displayName = (profileState as? UiState.Success)?.data?.username
        ?: user.displayName
        ?: user.email
        ?: "Player"

      ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
          ModalDrawerSheet(
            drawerContainerColor = RoyalSurfaceDeep,
            drawerContentColor = InkLight,
            modifier = Modifier.width(280.dp),
          ) {
            YemeDrawerHeader(userLabel = "◉ $displayName · $walletEtb ETB")
            Spacer(modifier = Modifier.height(6.dp))

            navItems.forEach { item ->
              val selected = currentRoute == item.route
              NavigationDrawerItem(
                label = {
                  Text(
                    text = item.label,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 17.sp,
                  )
                },
                selected = selected,
                onClick = {
                  scope.launch { drawerState.close() }
                  navigateTopLevel(item.route)
                },
                icon = { Icon(item.icon, contentDescription = null) },
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
                  .testTag(item.tag),
              )
            }

            Spacer(modifier = Modifier.weight(1f))
            HorizontalDivider(color = VioletLine)
            NavigationDrawerItem(
              label = {
                Text(
                  text = AppStrings.navSignOut(currentLang),
                  fontWeight = FontWeight.Bold,
                  fontSize = 17.sp,
                )
              },
              selected = false,
              onClick = {
                scope.launch { drawerState.close() }
                signOut(credentialManager = credentialManager, scope = scope)
              },
              icon = { Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = null) },
              colors = NavigationDrawerItemDefaults.colors(
                unselectedContainerColor = Color.Transparent,
                unselectedIconColor = RoseError,
                unselectedTextColor = RoseError,
              ),
              modifier = Modifier
                .padding(horizontal = 10.dp, vertical = 8.dp)
                .testTag("drawer_sign_out"),
            )
          }
        },
        content = scaffoldContent,
      )
    }
  }

  val note = modalMessage
  if (note != null) {
    ModalNoteDialog(
      message = note,
      onDismiss = { viewModel.dismissModalDialog() },
    )
  }
}
