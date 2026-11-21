package com.example

import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CandlestickChart
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.firebase.BrokerageRepository
import com.example.data.firebase.attemptAutoSignIn
import com.example.data.firebase.configureAppCheck
import com.example.data.firebase.onGoogleSignInClicked
import com.example.data.firebase.signOut
import com.example.data.localization.AppTranslationManager
import com.example.data.localization.AppTranslationManager.tr
import com.example.data.localization.GlobalCurrencyManager
import com.example.data.market.LiveMarketRepository
import com.example.data.model.KycStatus
import com.example.data.model.OrderSide
import com.example.data.model.OrderType
import com.example.data.model.TimeInForce
import com.example.data.model.TradeMode
import com.example.data.model.UiState
import com.example.data.model.UserProfile
import com.example.ui.components.AiSupportSheet
import com.example.ui.components.BiometricGateScreen
import com.example.ui.components.BrokerageTopBar
import com.example.ui.components.DemoModeBanner
import com.example.ui.components.WorldCurrencyPickerDialog
import com.example.ui.components.WorldLanguagePickerDialog
import com.example.ui.screens.AccountKycSecurityScreen
import com.example.ui.screens.ActivityFundingScreen
import com.example.ui.screens.AdminControlPanelScreen
import com.example.ui.screens.AssetDetailScreen
import com.example.ui.screens.AuthLandingScreen
import com.example.ui.screens.MarketsScreenerScreen
import com.example.ui.screens.PortfolioDashboardScreen
import com.example.ui.theme.ApexBrokerTheme
import com.example.ui.viewmodel.AiSupportViewModel
import com.example.ui.viewmodel.AuthenticatedBrokerageViewModel
import com.example.ui.viewmodel.GuestDemoViewModel
import com.example.util.Formatters
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import kotlinx.coroutines.launch

private enum class BrokerageTab(val label: String, val icon: ImageVector) {
    PORTFOLIO("Portfolio", Icons.Default.AccountBalanceWallet),
    MARKETS("Markets", Icons.Default.ShowChart),
    TRADE("Trade", Icons.Default.CandlestickChart),
    ACTIVITY("Activity", Icons.Default.ReceiptLong),
    ACCOUNT("Account", Icons.Default.ManageAccounts)
}

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configureAppCheck(this, intent)
        enableEdgeToEdge()
        val sharedMarketRepo = LiveMarketRepository()
        val brokerageRepo = BrokerageRepository(applicationContext)

        setContent {
            var darkTheme by rememberSaveable { mutableStateOf(true) }
            ApexBrokerTheme(darkTheme = darkTheme) {
                ApexBrokerAppRoot(
                    marketRepository = sharedMarketRepo,
                    brokerageRepository = brokerageRepo,
                    darkTheme = darkTheme,
                    onToggleTheme = { darkTheme = it }
                )
            }
        }
    }
}

@Composable
private fun ApexBrokerAppRoot(
    marketRepository: LiveMarketRepository,
    brokerageRepository: BrokerageRepository,
    darkTheme: Boolean,
    onToggleTheme: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }
    var isGuestDemo by rememberSaveable { mutableStateOf(false) }
    var showLanguagePicker by rememberSaveable { mutableStateOf(false) }
    var showCurrencyPicker by rememberSaveable { mutableStateOf(false) }

    val activeCurrencyState by GlobalCurrencyManager.activeCurrency.collectAsStateWithLifecycle()
    val liveFxRates by GlobalCurrencyManager.liveRates.collectAsStateWithLifecycle()
    LaunchedEffect(activeCurrencyState) {
        Formatters.syncCurrencyState(activeCurrencyState)
    }

    LaunchedEffect(Unit) {
        launch { GlobalCurrencyManager.initialize(context.applicationContext) }
        launch { AppTranslationManager.initialize(context.applicationContext) }
    }

    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
            if (auth.currentUser != null) {
                isGuestDemo = false
            }
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose { Firebase.auth.removeAuthStateListener(listener) }
    }

    LaunchedEffect(Unit) {
        if (currentUser == null) {
            attemptAutoSignIn(
                context = context,
                credentialManager = credentialManager,
                onAuthSuccess = {
                    currentUser = Firebase.auth.currentUser
                    isGuestDemo = false
                },
                onUnauthenticated = {},
                scope = scope
            )
        }
    }

    val signedInUser = currentUser
    if (signedInUser != null) {
        key(signedInUser.uid) {
            AuthenticatedShell(
                user = signedInUser,
                brokerageRepository = brokerageRepository,
                marketRepository = marketRepository,
                darkTheme = darkTheme,
                onToggleTheme = onToggleTheme,
                onOpenLanguagePicker = { showLanguagePicker = true },
                onOpenCurrencyPicker = { showCurrencyPicker = true },
                onSignOut = {
                    signOut(credentialManager, scope) {
                        currentUser = null
                        isGuestDemo = false
                    }
                }
            )
        }
    } else if (isGuestDemo) {
        GuestDemoShell(
            marketRepository = marketRepository,
            darkTheme = darkTheme,
            onToggleTheme = onToggleTheme,
            onOpenLanguagePicker = { showLanguagePicker = true },
            onOpenCurrencyPicker = { showCurrencyPicker = true },
            onExitGuestDemo = { isGuestDemo = false },
            onUpgradeToGoogleSignIn = {
                onGoogleSignInClicked(
                    context = context,
                    credentialManager = credentialManager,
                    onAuthSuccess = {
                        currentUser = Firebase.auth.currentUser
                        isGuestDemo = false
                    },
                    onAuthError = {},
                    scope = scope
                )
            }
        )
    } else {
        val previewAssets by marketRepository.assets.collectAsStateWithLifecycle()
        LaunchedEffect(Unit) {
            marketRepository.refreshAllQuotes(forceNetwork = false)
        }
        AuthLandingScreen(
            previewAssets = previewAssets,
            onOpenLanguagePicker = { showLanguagePicker = true },
            onOpenCurrencyPicker = { showCurrencyPicker = true },
            onAuthSuccess = {
                currentUser = Firebase.auth.currentUser
                isGuestDemo = false
            },
            onTryFreeDemo = { isGuestDemo = true }
        )
    }

    if (showLanguagePicker) {
        WorldLanguagePickerDialog(
            activeLanguage = AppTranslationManager.activeLanguage,
            onSelectLanguage = { lang ->
                showLanguagePicker = false
                scope.launch {
                    AppTranslationManager.selectLanguage(context.applicationContext, lang)
                }
            },
            onDismiss = { showLanguagePicker = false }
        )
    }

    if (showCurrencyPicker) {
        WorldCurrencyPickerDialog(
            activeCurrencyCode = activeCurrencyState.code,
            liveRates = liveFxRates,
            onSelectCurrency = { cur ->
                showCurrencyPicker = false
                scope.launch {
                    GlobalCurrencyManager.selectCurrency(context.applicationContext, cur.code)
                }
            },
            onRefreshRates = {
                scope.launch { GlobalCurrencyManager.refreshLiveFxRates() }
            },
            onDismiss = { showCurrencyPicker = false }
        )
    }
}

@Composable
private fun AuthenticatedShell(
    user: FirebaseUser,
    brokerageRepository: BrokerageRepository,
    marketRepository: LiveMarketRepository,
    darkTheme: Boolean,
    onToggleTheme: (Boolean) -> Unit,
    onOpenLanguagePicker: () -> Unit,
    onOpenCurrencyPicker: () -> Unit,
    onSignOut: () -> Unit
) {
    val vm: AuthenticatedBrokerageViewModel = viewModel(
        key = "auth_vm_${user.uid}",
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return AuthenticatedBrokerageViewModel(
                    currentUserId = user.uid,
                    currentUser = user,
                    repository = brokerageRepository,
                    marketRepository = marketRepository
                ) as T
            }
        }
    )

    val isLocked by vm.isLockedByBiometric.collectAsStateWithLifecycle()
    if (isLocked) {
        BiometricGateScreen(
            onUnlocked = { vm.unlockBiometric() },
            onSignOutFallback = onSignOut
        )
        return
    }

    val profileState by vm.profileState.collectAsStateWithLifecycle()
    val marketAssets by vm.marketAssets.collectAsStateWithLifecycle()
    val syncStatus by vm.marketSyncStatus.collectAsStateWithLifecycle()
    val globalSearchResults by vm.globalSearchResults.collectAsStateWithLifecycle()
    val isSearchingGlobal by vm.isSearchingGlobal.collectAsStateWithLifecycle()
    val assetDetail by vm.assetDetail.collectAsStateWithLifecycle()
    val selectedTimeframe by vm.selectedTimeframe.collectAsStateWithLifecycle()
    val watchlists by vm.watchlists.collectAsStateWithLifecycle()
    val priceAlerts by vm.priceAlerts.collectAsStateWithLifecycle()
    val myKyc by vm.myKyc.collectAsStateWithLifecycle()
    val depositWallets by vm.depositWallets.collectAsStateWithLifecycle()
    val myDeposits by vm.myDeposits.collectAsStateWithLifecycle()
    val myWithdrawals by vm.myWithdrawals.collectAsStateWithLifecycle()
    val myOrders by vm.myOrders.collectAsStateWithLifecycle()
    val myPositions by vm.myPositions.collectAsStateWithLifecycle()
    val myLedger by vm.myLedger.collectAsStateWithLifecycle()
    val myTickets by vm.myTickets.collectAsStateWithLifecycle()
    val marketControls by vm.marketControls.collectAsStateWithLifecycle()
    val isAdmin by vm.isAdmin.collectAsStateWithLifecycle()

    val allUsers by vm.allUsersAdmin.collectAsStateWithLifecycle()
    val allKyc by vm.allKycAdmin.collectAsStateWithLifecycle()
    val allDeposits by vm.allDepositsAdmin.collectAsStateWithLifecycle()
    val allWithdrawals by vm.allWithdrawalsAdmin.collectAsStateWithLifecycle()
    val allOrders by vm.allOrdersAdmin.collectAsStateWithLifecycle()
    val allTickets by vm.allTicketsAdmin.collectAsStateWithLifecycle()
    val auditLogs by vm.auditLogsAdmin.collectAsStateWithLifecycle()
    val adminRoles by vm.adminRoles.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(Unit) {
        vm.toastEvents.collect { msg -> snackbarHostState.showSnackbar(msg) }
    }

    val profile = when (val st = profileState) {
        is UiState.Success -> st.data ?: UserProfile(
            userId = user.uid,
            email = user.email ?: "",
            displayName = user.displayName ?: "Apex Investor"
        )
        else -> UserProfile(
            userId = user.uid,
            email = user.email ?: "",
            displayName = user.displayName ?: "Apex Investor"
        )
    }

    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var activityInitialTab by rememberSaveable { mutableIntStateOf(0) }
    var detailOverlayOpen by rememberSaveable { mutableStateOf(false) }
    var isAdminViewActive by rememberSaveable { mutableStateOf(false) }
    var showAiSupport by rememberSaveable { mutableStateOf(false) }
    val aiSupportVm: AiSupportViewModel = viewModel(key = "ai_support_${user.uid}")

    if (detailOverlayOpen) {
        BackHandler { detailOverlayOpen = false }
    }

    val activeMode = profile.modeEnum
    val availableBalance = if (activeMode == TradeMode.LIVE) profile.liveAvailableBalance else profile.demoAvailableBalance
    val activeWatchlist = watchlists.firstOrNull()

    AdaptiveBrokerageScaffold(
        snackbarHostState = snackbarHostState,
        selectedTab = selectedTab,
        showNav = !(isAdminViewActive && isAdmin),
        onSelectTab = { idx ->
            detailOverlayOpen = false
            selectedTab = idx
        },
        topBar = {
            Column(modifier = Modifier.statusBarsPadding()) {
                BrokerageTopBar(
                    activeMode = activeMode,
                    kycStatus = profile.kycEnum,
                    isGuestDemo = false,
                    isAdmin = isAdmin,
                    isAdminViewActive = isAdminViewActive,
                    onToggleMode = { vm.switchMode(it) },
                    onToggleAdminView = {
                        isAdminViewActive = !isAdminViewActive
                        detailOverlayOpen = false
                    },
                    onKycBadgeClick = {
                        isAdminViewActive = false
                        detailOverlayOpen = false
                        selectedTab = BrokerageTab.ACCOUNT.ordinal
                    },
                    onOpenAiSupport = { showAiSupport = true },
                    onOpenLanguagePicker = onOpenLanguagePicker,
                    onOpenCurrencyPicker = onOpenCurrencyPicker
                )
                if (activeMode == TradeMode.DEMO && !(isAdminViewActive && isAdmin)) {
                    DemoModeBanner(
                        virtualBalance = profile.demoAvailableBalance,
                        isGuestDemo = false,
                        onResetBalance = { vm.resetDemoBalance() },
                        onSwitchToLiveOrUpgrade = { vm.switchMode(TradeMode.LIVE) }
                    )
                }
            }
        }
    ) {
        if (isAdminViewActive && isAdmin) {
            AdminControlPanelScreen(
                users = allUsers,
                kycSubmissions = allKyc,
                deposits = allDeposits,
                withdrawals = allWithdrawals,
                orders = allOrders,
                wallets = depositWallets,
                marketAssets = marketAssets,
                marketControls = marketControls,
                tickets = allTickets,
                auditLogs = auditLogs,
                adminRoles = adminRoles,
                onExitAdmin = { isAdminViewActive = false },
                onApproveKyc = { vm.adminApproveKyc(it) },
                onRejectKyc = { kyc, reason -> vm.adminRejectKyc(kyc, reason) },
                onApproveDeposit = { vm.adminApproveDeposit(it) },
                onRejectDeposit = { dep, reason -> vm.adminRejectDeposit(dep, reason) },
                onApproveWithdrawal = { w, tx -> vm.adminApproveWithdrawal(w, tx) },
                onRejectWithdrawal = { w, reason -> vm.adminRejectWithdrawal(w, reason) },
                onAdjustBalance = { u, delta, r -> vm.adminAdjustBalance(u, delta, r) },
                onToggleFreeze = { u, frozen -> vm.adminToggleFreeze(u, frozen) },
                onReset2fa = { vm.adminReset2fa(it) },
                onSaveWallet = { vm.adminSaveDepositWallet(it) },
                onSeedWallets = { vm.adminSeedWallets() },
                onSaveMarketControl = { vm.adminSaveMarketControl(it) },
                onReplyTicket = { t, s, r -> vm.adminReplyTicket(t, s, r) },
                onAddAdminRole = { uid, email, role -> vm.adminAddRole(uid, email, role) },
                onRemoveAdminRole = { vm.adminRemoveRole(it) }
            )
        } else if (detailOverlayOpen) {
            val detailSym = assetDetail?.asset?.symbol ?: "BTC"
            val currentPos = myPositions.firstOrNull { it.symbol == detailSym && it.tradeMode == activeMode.name }
            val ctrl = marketControls.firstOrNull { it.symbol == detailSym }
            AssetDetailScreen(
                detail = assetDetail,
                selectedTimeframe = selectedTimeframe,
                activeMode = activeMode,
                availableBalance = availableBalance,
                currentPosition = currentPos,
                marketControl = ctrl,
                isWatchlisted = activeWatchlist?.symbols?.contains(detailSym) == true,
                canCreateAlerts = true,
                onBack = { detailOverlayOpen = false },
                onSelectTimeframe = { vm.selectTimeframe(it) },
                onToggleWatchlist = {
                    activeWatchlist?.let { vm.toggleWatchlist(it, detailSym) }
                },
                onCreateAlert = { px, cond -> vm.createPriceAlert(detailSym, px, cond) },
                onPlaceOrder = { side, type, tif, qty, limit, stop, tp ->
                    val asset = assetDetail?.asset ?: return@AssetDetailScreen
                    vm.placeOrder(profile, asset, side, type, tif, qty, limit, stop, tp)
                }
            )
        } else {
            when (selectedTab) {
                BrokerageTab.PORTFOLIO.ordinal -> PortfolioDashboardScreen(
                    profile = profile,
                    positions = myPositions,
                    marketAssets = marketAssets,
                    watchlistSymbols = activeWatchlist?.symbols.orEmpty(),
                    priceAlerts = priceAlerts,
                    onQuickDeposit = {
                        activityInitialTab = 2
                        selectedTab = BrokerageTab.ACTIVITY.ordinal
                    },
                    onQuickWithdraw = {
                        activityInitialTab = 3
                        selectedTab = BrokerageTab.ACTIVITY.ordinal
                    },
                    onQuickTrade = { selectedTab = BrokerageTab.TRADE.ordinal },
                    onQuickKyc = { selectedTab = BrokerageTab.ACCOUNT.ordinal },
                    onQuickReports = {
                        activityInitialTab = 5
                        selectedTab = BrokerageTab.ACTIVITY.ordinal
                    },
                    onAssetClick = { sym ->
                        vm.selectAsset(sym)
                        detailOverlayOpen = true
                    }
                )
                BrokerageTab.MARKETS.ordinal -> MarketsScreenerScreen(
                    assets = marketAssets,
                    syncStatus = syncStatus,
                    watchlists = watchlists,
                    activeWatchlist = activeWatchlist,
                    globalSearchResults = globalSearchResults,
                    isSearchingGlobal = isSearchingGlobal,
                    onSearchGlobalMarkets = { vm.searchGlobalMarkets(it) },
                    onSelectGlobalSearchAsset = { asset ->
                        vm.selectGlobalSearchAsset(asset) { detailOverlayOpen = true }
                    },
                    onSelectAsset = { sym ->
                        vm.selectAsset(sym)
                        detailOverlayOpen = true
                    },
                    onToggleWatchlist = { wl, sym -> vm.toggleWatchlist(wl, sym) },
                    onCreateWatchlist = { name, syms -> vm.createWatchlist(name, syms) },
                    onRefreshQuotes = { vm.refreshQuotesNow() }
                )
                BrokerageTab.TRADE.ordinal -> {
                    val detailSym = assetDetail?.asset?.symbol ?: "BTC"
                    val currentPos = myPositions.firstOrNull { it.symbol == detailSym && it.tradeMode == activeMode.name }
                    val ctrl = marketControls.firstOrNull { it.symbol == detailSym }
                    AssetDetailScreen(
                        detail = assetDetail,
                        selectedTimeframe = selectedTimeframe,
                        activeMode = activeMode,
                        availableBalance = availableBalance,
                        currentPosition = currentPos,
                        marketControl = ctrl,
                        isWatchlisted = activeWatchlist?.symbols?.contains(detailSym) == true,
                        canCreateAlerts = true,
                        onBack = null,
                        onSelectTimeframe = { vm.selectTimeframe(it) },
                        onToggleWatchlist = { activeWatchlist?.let { vm.toggleWatchlist(it, detailSym) } },
                        onCreateAlert = { px, cond -> vm.createPriceAlert(detailSym, px, cond) },
                        onPlaceOrder = { side, type, tif, qty, limit, stop, tp ->
                            val asset = assetDetail?.asset ?: return@AssetDetailScreen
                            vm.placeOrder(profile, asset, side, type, tif, qty, limit, stop, tp)
                        }
                    )
                }
                BrokerageTab.ACTIVITY.ordinal -> ActivityFundingScreen(
                    initialTab = activityInitialTab,
                    profile = profile,
                    isGuestDemo = false,
                    marketAssets = marketAssets,
                    positions = myPositions,
                    orders = myOrders,
                    depositWallets = depositWallets,
                    deposits = myDeposits,
                    withdrawals = myWithdrawals,
                    ledgerEntries = myLedger,
                    onSwitchToLive = { vm.switchMode(TradeMode.LIVE) },
                    onUpgradeGuestToReal = {},
                    onNavigateToKyc = { selectedTab = BrokerageTab.ACCOUNT.ordinal },
                    onClosePosition = { pos, asset ->
                        vm.placeOrder(profile, asset, OrderSide.SELL, OrderType.MARKET, TimeInForce.GTC, pos.quantity, null, null, null)
                    },
                    onTogglePositionDrip = { pos, enabled -> vm.togglePositionDrip(pos, enabled) },
                    onCancelOrder = { vm.cancelOrder(it) },
                    onModifyOrderLimit = { ord, lim -> vm.modifyOrderLimit(ord, lim) },
                    onSubmitDeposit = { w, usd, crypto, tx, proof -> vm.submitDeposit(w, usd, crypto, tx, proof) },
                    onSubmitWithdrawal = { sym, net, dest, usd -> vm.submitWithdrawal(profile, sym, net, dest, usd) }
                )
                else -> AccountKycSecurityScreen(
                    profile = profile,
                    isGuestDemo = false,
                    isAdmin = isAdmin,
                    darkTheme = darkTheme,
                    kycSubmission = myKyc,
                    priceAlerts = priceAlerts,
                    tickets = myTickets,
                    onToggleTheme = onToggleTheme,
                    onToggleBiometric = { vm.toggleBiometric(it) },
                    onSetupTwoFactor = { en, hash -> vm.setupTwoFactor(en, hash) },
                    onToggleDrip = { vm.toggleDrip(it) },
                    onAddWhitelistAddress = { vm.addWhitelistedAddress(profile.whitelistedAddresses, it) },
                    onDeleteAlert = { vm.deletePriceAlert(it) },
                    onSubmitKyc = { vm.submitKyc(it) },
                    onSubmitTicket = { subj, cat, msg -> vm.submitTicket(subj, cat, msg) },
                    onOpenAdminPanel = { isAdminViewActive = true },
                    onOpenAiSupport = { showAiSupport = true },
                    onOpenLanguagePicker = onOpenLanguagePicker,
                    onOpenCurrencyPicker = onOpenCurrencyPicker,
                    onUpgradeGuest = {},
                    onSignOut = onSignOut
                )
            }
        }
    }

    if (showAiSupport) {
        AiSupportSheet(
            aiViewModel = aiSupportVm,
            profile = profile,
            positions = myPositions,
            marketAssets = marketAssets,
            onDismiss = { showAiSupport = false }
        )
    }
}

@Composable
private fun GuestDemoShell(
    marketRepository: LiveMarketRepository,
    darkTheme: Boolean,
    onToggleTheme: (Boolean) -> Unit,
    onOpenLanguagePicker: () -> Unit,
    onOpenCurrencyPicker: () -> Unit,
    onExitGuestDemo: () -> Unit,
    onUpgradeToGoogleSignIn: () -> Unit
) {
    val vm: GuestDemoViewModel = viewModel(
        key = "guest_demo_vm",
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return GuestDemoViewModel(marketRepository = marketRepository) as T
            }
        }
    )

    val profile by vm.demoEngine.profile.collectAsStateWithLifecycle()
    val positions by vm.demoEngine.positions.collectAsStateWithLifecycle()
    val orders by vm.demoEngine.orders.collectAsStateWithLifecycle()
    val ledger by vm.demoEngine.ledger.collectAsStateWithLifecycle()
    val watchlist by vm.demoEngine.watchlist.collectAsStateWithLifecycle()
    val marketAssets by vm.marketAssets.collectAsStateWithLifecycle()
    val syncStatus by vm.marketSyncStatus.collectAsStateWithLifecycle()
    val globalSearchResults by vm.globalSearchResults.collectAsStateWithLifecycle()
    val isSearchingGlobal by vm.isSearchingGlobal.collectAsStateWithLifecycle()
    val assetDetail by vm.assetDetail.collectAsStateWithLifecycle()
    val selectedTimeframe by vm.selectedTimeframe.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(Unit) {
        vm.toastEvents.collect { msg -> snackbarHostState.showSnackbar(msg) }
    }

    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var activityInitialTab by rememberSaveable { mutableIntStateOf(0) }
    var detailOverlayOpen by rememberSaveable { mutableStateOf(false) }
    var showAiSupport by rememberSaveable { mutableStateOf(false) }
    val aiSupportVm: AiSupportViewModel = viewModel(key = "ai_support_guest")

    if (detailOverlayOpen) {
        BackHandler { detailOverlayOpen = false }
    } else {
        BackHandler(onBack = onExitGuestDemo)
    }

    AdaptiveBrokerageScaffold(
        snackbarHostState = snackbarHostState,
        selectedTab = selectedTab,
        showNav = true,
        onSelectTab = { idx ->
            detailOverlayOpen = false
            selectedTab = idx
        },
        topBar = {
            Column(modifier = Modifier.statusBarsPadding()) {
                BrokerageTopBar(
                    activeMode = TradeMode.DEMO,
                    kycStatus = KycStatus.NOT_SUBMITTED,
                    isGuestDemo = true,
                    isAdmin = false,
                    isAdminViewActive = false,
                    onToggleMode = {},
                    onToggleAdminView = {},
                    onKycBadgeClick = {},
                    onOpenAiSupport = { showAiSupport = true },
                    onOpenLanguagePicker = onOpenLanguagePicker,
                    onOpenCurrencyPicker = onOpenCurrencyPicker
                )
                DemoModeBanner(
                    virtualBalance = profile.demoAvailableBalance,
                    isGuestDemo = true,
                    onResetBalance = { vm.resetDemo() },
                    onSwitchToLiveOrUpgrade = onUpgradeToGoogleSignIn
                )
            }
        }
    ) {
        if (detailOverlayOpen) {
            val detailSym = assetDetail?.asset?.symbol ?: "BTC"
            val currentPos = positions.firstOrNull { it.symbol == detailSym }
            AssetDetailScreen(
                detail = assetDetail,
                selectedTimeframe = selectedTimeframe,
                activeMode = TradeMode.DEMO,
                availableBalance = profile.demoAvailableBalance,
                currentPosition = currentPos,
                marketControl = null,
                isWatchlisted = watchlist.symbols.contains(detailSym),
                canCreateAlerts = false,
                onBack = { detailOverlayOpen = false },
                onSelectTimeframe = { vm.selectTimeframe(it) },
                onToggleWatchlist = { vm.demoEngine.toggleWatchlist(detailSym) },
                onCreateAlert = { _, _ -> },
                onPlaceOrder = { side, type, tif, qty, limit, stop, tp ->
                    val asset = assetDetail?.asset ?: return@AssetDetailScreen
                    vm.placeOrder(asset, side, type, tif, qty, limit, stop, tp)
                }
            )
        } else {
            when (selectedTab) {
                BrokerageTab.PORTFOLIO.ordinal -> PortfolioDashboardScreen(
                    profile = profile,
                    positions = positions,
                    marketAssets = marketAssets,
                    watchlistSymbols = watchlist.symbols,
                    priceAlerts = emptyList(),
                    onQuickDeposit = {
                        activityInitialTab = 2
                        selectedTab = BrokerageTab.ACTIVITY.ordinal
                    },
                    onQuickWithdraw = {
                        activityInitialTab = 3
                        selectedTab = BrokerageTab.ACTIVITY.ordinal
                    },
                    onQuickTrade = { selectedTab = BrokerageTab.TRADE.ordinal },
                    onQuickKyc = onUpgradeToGoogleSignIn,
                    onQuickReports = {
                        activityInitialTab = 5
                        selectedTab = BrokerageTab.ACTIVITY.ordinal
                    },
                    onAssetClick = { sym ->
                        vm.selectAsset(sym)
                        detailOverlayOpen = true
                    }
                )
                BrokerageTab.MARKETS.ordinal -> MarketsScreenerScreen(
                    assets = marketAssets,
                    syncStatus = syncStatus,
                    watchlists = listOf(watchlist),
                    activeWatchlist = watchlist,
                    globalSearchResults = globalSearchResults,
                    isSearchingGlobal = isSearchingGlobal,
                    onSearchGlobalMarkets = { vm.searchGlobalMarkets(it) },
                    onSelectGlobalSearchAsset = { asset ->
                        vm.selectGlobalSearchAsset(asset) { detailOverlayOpen = true }
                    },
                    onSelectAsset = { sym ->
                        vm.selectAsset(sym)
                        detailOverlayOpen = true
                    },
                    onToggleWatchlist = { _, sym -> vm.demoEngine.toggleWatchlist(sym) },
                    onCreateWatchlist = { _, _ -> },
                    onRefreshQuotes = { vm.refreshNow() }
                )
                BrokerageTab.TRADE.ordinal -> {
                    val detailSym = assetDetail?.asset?.symbol ?: "BTC"
                    val currentPos = positions.firstOrNull { it.symbol == detailSym }
                    AssetDetailScreen(
                        detail = assetDetail,
                        selectedTimeframe = selectedTimeframe,
                        activeMode = TradeMode.DEMO,
                        availableBalance = profile.demoAvailableBalance,
                        currentPosition = currentPos,
                        marketControl = null,
                        isWatchlisted = watchlist.symbols.contains(detailSym),
                        canCreateAlerts = false,
                        onBack = null,
                        onSelectTimeframe = { vm.selectTimeframe(it) },
                        onToggleWatchlist = { vm.demoEngine.toggleWatchlist(detailSym) },
                        onCreateAlert = { _, _ -> },
                        onPlaceOrder = { side, type, tif, qty, limit, stop, tp ->
                            val asset = assetDetail?.asset ?: return@AssetDetailScreen
                            vm.placeOrder(asset, side, type, tif, qty, limit, stop, tp)
                        }
                    )
                }
                BrokerageTab.ACTIVITY.ordinal -> ActivityFundingScreen(
                    initialTab = activityInitialTab,
                    profile = profile,
                    isGuestDemo = true,
                    marketAssets = marketAssets,
                    positions = positions,
                    orders = orders,
                    depositWallets = emptyList(),
                    deposits = emptyList(),
                    withdrawals = emptyList(),
                    ledgerEntries = ledger,
                    onSwitchToLive = onUpgradeToGoogleSignIn,
                    onUpgradeGuestToReal = onUpgradeToGoogleSignIn,
                    onNavigateToKyc = onUpgradeToGoogleSignIn,
                    onClosePosition = { pos, asset ->
                        vm.placeOrder(asset, OrderSide.SELL, OrderType.MARKET, TimeInForce.GTC, pos.quantity, null, null, null)
                    },
                    onTogglePositionDrip = { _, _ -> },
                    onCancelOrder = { vm.demoEngine.cancelOrder(it.id) },
                    onModifyOrderLimit = { _, _ -> },
                    onSubmitDeposit = { _, _, _, _, _ -> },
                    onSubmitWithdrawal = { _, _, _, _ -> }
                )
                else -> AccountKycSecurityScreen(
                    profile = profile,
                    isGuestDemo = true,
                    isAdmin = false,
                    darkTheme = darkTheme,
                    kycSubmission = null,
                    priceAlerts = emptyList(),
                    tickets = emptyList(),
                    onToggleTheme = onToggleTheme,
                    onToggleBiometric = {},
                    onSetupTwoFactor = { _, _ -> },
                    onToggleDrip = {},
                    onAddWhitelistAddress = {},
                    onDeleteAlert = {},
                    onSubmitKyc = {},
                    onSubmitTicket = { _, _, _ -> },
                    onOpenAdminPanel = {},
                    onOpenAiSupport = { showAiSupport = true },
                    onOpenLanguagePicker = onOpenLanguagePicker,
                    onOpenCurrencyPicker = onOpenCurrencyPicker,
                    onUpgradeGuest = onUpgradeToGoogleSignIn,
                    onSignOut = onExitGuestDemo
                )
            }
        }
    }

    if (showAiSupport) {
        AiSupportSheet(
            aiViewModel = aiSupportVm,
            profile = profile,
            positions = positions,
            marketAssets = marketAssets,
            onDismiss = { showAiSupport = false }
        )
    }
}

@Composable
private fun AdaptiveBrokerageScaffold(
    snackbarHostState: SnackbarHostState,
    selectedTab: Int,
    showNav: Boolean,
    onSelectTab: (Int) -> Unit,
    topBar: @Composable () -> Unit,
    content: @Composable () -> Unit
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 600.dp

        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = topBar,
            bottomBar = {
                if (showNav && !useRail) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        windowInsets = WindowInsets.navigationBars
                    ) {
                        BrokerageTab.entries.forEachIndexed { idx, tab ->
                            val translatedLabel = tr(tab.label)
                            NavigationBarItem(
                                selected = selectedTab == idx,
                                onClick = { onSelectTab(idx) },
                                icon = { Icon(tab.icon, contentDescription = translatedLabel) },
                                label = { Text(translatedLabel, maxLines = 1) },
                                modifier = Modifier.testTag("nav_tab_${tab.name.lowercase()}")
                            )
                        }
                    }
                }
            },
            contentWindowInsets = WindowInsets(0, 0, 0, 0)
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (showNav && useRail) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        BrokerageTab.entries.forEachIndexed { idx, tab ->
                            val translatedLabel = tr(tab.label)
                            NavigationRailItem(
                                selected = selectedTab == idx,
                                onClick = { onSelectTab(idx) },
                                icon = { Icon(tab.icon, contentDescription = translatedLabel) },
                                label = { Text(translatedLabel) },
                                modifier = Modifier.testTag("rail_tab_${tab.name.lowercase()}")
                            )
                        }
                    }
                }
                Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                    content()
                }
            }
        }
    }
}
