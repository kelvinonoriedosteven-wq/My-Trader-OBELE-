package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.demo.GuestDemoEngine
import com.example.data.firebase.BrokerageRepository
import com.example.data.firebase.unwrapFirestoreMessage
import com.example.data.market.LiveMarketRepository
import com.example.data.model.AdminRole
import com.example.data.model.AdminRoleType
import com.example.data.model.AssetDetailData
import com.example.data.model.AuditLog
import com.example.data.model.DepositRequest
import com.example.data.model.DepositWallet
import com.example.data.model.KycSubmission
import com.example.data.model.LedgerEntry
import com.example.data.model.MarketAsset
import com.example.data.model.MarketControl
import com.example.data.model.OrderSide
import com.example.data.model.OrderType
import com.example.data.model.PortfolioPosition
import com.example.data.model.PriceAlert
import com.example.data.model.SupportTicket
import com.example.data.model.TicketStatus
import com.example.data.model.TimeInForce
import com.example.data.model.Timeframe
import com.example.data.model.TradeMode
import com.example.data.model.TradeOrder
import com.example.data.model.UiState
import com.example.data.model.UserProfile
import com.example.data.model.Watchlist
import com.example.data.model.WithdrawalRequest
import com.example.util.Formatters
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class AuthenticatedBrokerageViewModel(
    private val currentUserId: String,
    private val currentUser: FirebaseUser,
    private val repository: BrokerageRepository,
    val marketRepository: LiveMarketRepository,
    initialMode: TradeMode = TradeMode.LIVE
) : ViewModel() {

    private val _toastEvents = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val toastEvents: SharedFlow<String> = _toastEvents.asSharedFlow()

    private val _selectedSymbol = MutableStateFlow("BTC")
    val selectedSymbol: StateFlow<String> = _selectedSymbol.asStateFlow()

    private val _selectedTimeframe = MutableStateFlow(Timeframe.H1)
    val selectedTimeframe: StateFlow<Timeframe> = _selectedTimeframe.asStateFlow()

    private val _assetDetail = MutableStateFlow<AssetDetailData?>(null)
    val assetDetail: StateFlow<AssetDetailData?> = _assetDetail.asStateFlow()

    private val _isLockedByBiometric = MutableStateFlow(false)
    val isLockedByBiometric: StateFlow<Boolean> = _isLockedByBiometric.asStateFlow()

    private val _globalSearchResults = MutableStateFlow<List<MarketAsset>>(emptyList())
    val globalSearchResults: StateFlow<List<MarketAsset>> = _globalSearchResults.asStateFlow()

    private val _isSearchingGlobal = MutableStateFlow(false)
    val isSearchingGlobal: StateFlow<Boolean> = _isSearchingGlobal.asStateFlow()

    private var globalSearchJob: Job? = null
    private var biometricInitialGateEvaluated = false

    val marketAssets: StateFlow<List<MarketAsset>> = marketRepository.assets
    val marketSyncStatus: StateFlow<String> = marketRepository.lastSyncStatus

    private fun <T> Flow<T>.toUiState(label: String, initial: UiState<T> = UiState.Loading): StateFlow<UiState<T>> =
        this.map<T, UiState<T>> { UiState.Success(it) }
            .catch { error ->
                if (error is CancellationException) {
                    val unwrapped = unwrapFirestoreMessage(error, "Failed to load $label")
                    Log.w(TAG, "Stream error ($label): $unwrapped")
                    emit(UiState.Error(unwrapped))
                } else {
                    Log.w(TAG, "Stream error ($label)", error)
                    emit(UiState.Error(error.localizedMessage ?: "Failed to load $label"))
                }
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000L),
                initialValue = initial
            )

    val profileState: StateFlow<UiState<UserProfile?>> =
        repository.observeUserProfile(currentUserId).toUiState("profile")

    val watchlists: StateFlow<List<Watchlist>> =
        repository.observeWatchlists(currentUserId)
            .catch { if (it is CancellationException) unwrapFirestoreMessage(it, "") }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val priceAlerts: StateFlow<List<PriceAlert>> =
        repository.observePriceAlerts(currentUserId)
            .catch { if (it is CancellationException) unwrapFirestoreMessage(it, "") }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val myKyc: StateFlow<KycSubmission?> =
        repository.observeMyKyc(currentUserId)
            .catch { if (it is CancellationException) unwrapFirestoreMessage(it, "") }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)

    val depositWallets: StateFlow<List<DepositWallet>> =
        repository.observeDepositWallets(currentUserId)
            .catch { if (it is CancellationException) unwrapFirestoreMessage(it, "") }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val myDeposits: StateFlow<List<DepositRequest>> =
        repository.observeMyDeposits(currentUserId)
            .catch { if (it is CancellationException) unwrapFirestoreMessage(it, "") }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val myWithdrawals: StateFlow<List<WithdrawalRequest>> =
        repository.observeMyWithdrawals(currentUserId)
            .catch { if (it is CancellationException) unwrapFirestoreMessage(it, "") }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val myOrders: StateFlow<List<TradeOrder>> =
        repository.observeMyOrders(currentUserId)
            .catch { if (it is CancellationException) unwrapFirestoreMessage(it, "") }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val myPositions: StateFlow<List<PortfolioPosition>> =
        repository.observeMyPositions(currentUserId)
            .catch { if (it is CancellationException) unwrapFirestoreMessage(it, "") }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val myLedger: StateFlow<List<LedgerEntry>> =
        repository.observeMyLedger(currentUserId)
            .catch { if (it is CancellationException) unwrapFirestoreMessage(it, "") }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val myTickets: StateFlow<List<SupportTicket>> =
        repository.observeMyTickets(currentUserId)
            .catch { if (it is CancellationException) unwrapFirestoreMessage(it, "") }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val marketControls: StateFlow<List<MarketControl>> =
        repository.observeMarketControls(currentUserId)
            .catch { if (it is CancellationException) unwrapFirestoreMessage(it, "") }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    private val isAdminDoc: Flow<Boolean> = repository.observeIsAdminDoc(currentUserId)
        .catch { emit(false) }

    val isAdmin: StateFlow<Boolean> = isAdminDoc
        .map { hasDoc -> hasDoc || repository.isBootstrappedAdmin(currentUser) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), repository.isBootstrappedAdmin(currentUser))

    val allUsersAdmin: StateFlow<List<UserProfile>> = isAdmin
        .flatMapLatest { admin ->
            if (admin) repository.observeAllUsers(currentUserId).catch { emit(emptyList()) }
            else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val allKycAdmin: StateFlow<List<KycSubmission>> = isAdmin
        .flatMapLatest { admin ->
            if (admin) repository.observeAllKyc(currentUserId).catch { emit(emptyList()) }
            else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val allDepositsAdmin: StateFlow<List<DepositRequest>> = isAdmin
        .flatMapLatest { admin ->
            if (admin) repository.observeAllDeposits(currentUserId).catch { emit(emptyList()) }
            else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val allWithdrawalsAdmin: StateFlow<List<WithdrawalRequest>> = isAdmin
        .flatMapLatest { admin ->
            if (admin) repository.observeAllWithdrawals(currentUserId).catch { emit(emptyList()) }
            else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val allOrdersAdmin: StateFlow<List<TradeOrder>> = isAdmin
        .flatMapLatest { admin ->
            if (admin) repository.observeAllOrders(currentUserId).catch { emit(emptyList()) }
            else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val allTicketsAdmin: StateFlow<List<SupportTicket>> = isAdmin
        .flatMapLatest { admin ->
            if (admin) repository.observeAllTickets(currentUserId).catch { emit(emptyList()) }
            else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val auditLogsAdmin: StateFlow<List<AuditLog>> = isAdmin
        .flatMapLatest { admin ->
            if (admin) repository.observeAuditLogs(currentUserId).catch { emit(emptyList()) }
            else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val adminRoles: StateFlow<List<AdminRole>> = isAdmin
        .flatMapLatest { admin ->
            if (admin) repository.observeAdminRoles(currentUserId).catch { emit(emptyList()) }
            else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    init {
        viewModelScope.launch {
            repository.ensureUserProvisioned(currentUser, initialMode).onFailure {
                _toastEvents.tryEmit(it.localizedMessage ?: "Account setup error")
            }
        }
        viewModelScope.launch {
            profileState.collect { state ->
                if (state is UiState.Success && !biometricInitialGateEvaluated) {
                    biometricInitialGateEvaluated = true
                    if (state.data?.biometricEnabled == true) {
                        _isLockedByBiometric.value = true
                    }
                }
            }
        }
        viewModelScope.launch {
            while (isActive) {
                marketRepository.refreshAllQuotes(forceNetwork = false)
                delay(15_000L)
            }
        }
        viewModelScope.launch {
            combine(_selectedSymbol, _selectedTimeframe, marketAssets) { sym, tf, _ -> sym to tf }
                .collect { (sym, tf) ->
                    _assetDetail.value = marketRepository.loadAssetDetail(sym, tf)
                }
        }
    }

    fun unlockBiometric() {
        _isLockedByBiometric.value = false
    }

    fun selectAsset(symbol: String) {
        _selectedSymbol.value = symbol
    }

    fun selectTimeframe(timeframe: Timeframe) {
        _selectedTimeframe.value = timeframe
    }

    fun refreshQuotesNow() {
        viewModelScope.launch {
            marketRepository.refreshAllQuotes(forceNetwork = true)
            _assetDetail.value = marketRepository.loadAssetDetail(_selectedSymbol.value, _selectedTimeframe.value)
        }
    }

    fun searchGlobalMarkets(query: String) {
        globalSearchJob?.cancel()
        val clean = query.trim()
        if (clean.length < 2) {
            _globalSearchResults.value = emptyList()
            _isSearchingGlobal.value = false
            return
        }
        globalSearchJob = viewModelScope.launch {
            _isSearchingGlobal.value = true
            try {
                _globalSearchResults.value = marketRepository.searchGlobalMarkets(clean)
            } finally {
                _isSearchingGlobal.value = false
            }
        }
    }

    fun selectGlobalSearchAsset(asset: MarketAsset, onReady: () -> Unit = {}) {
        viewModelScope.launch {
            marketRepository.registerAndLoadAsset(asset)
            _selectedSymbol.value = asset.symbol
            _assetDetail.value = marketRepository.loadAssetDetail(asset.symbol, _selectedTimeframe.value)
            onReady()
        }
    }

    fun switchMode(mode: TradeMode) {
        viewModelScope.launch {
            repository.switchTradeMode(mode)
                .onSuccess { _toastEvents.tryEmit("Switched to ${mode.name} mode") }
                .onFailure { _toastEvents.tryEmit(it.localizedMessage ?: "Mode switch failed") }
        }
    }

    fun resetDemoBalance() {
        viewModelScope.launch {
            repository.resetDemoBalance()
                .onSuccess { _toastEvents.tryEmit("Demo balance reset to ${Formatters.formatUsd(10_000.0)}") }
                .onFailure { _toastEvents.tryEmit(it.localizedMessage ?: "Reset failed") }
        }
    }

    fun toggleBiometric(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateSecuritySettings(biometricEnabled = enabled)
                .onSuccess { _toastEvents.tryEmit("Biometric authentication ${if (enabled) "enabled" else "disabled"}") }
                .onFailure { _toastEvents.tryEmit(it.localizedMessage ?: "Update failed") }
        }
    }

    fun setupTwoFactor(enabled: Boolean, pinHash: String?) {
        viewModelScope.launch {
            repository.updateSecuritySettings(twoFactorEnabled = enabled, twoFactorPinHash = pinHash)
                .onSuccess { _toastEvents.tryEmit("2FA security ${if (enabled) "activated" else "disabled"}") }
                .onFailure { _toastEvents.tryEmit(it.localizedMessage ?: "2FA update failed") }
        }
    }

    fun toggleDrip(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateSecuritySettings(dripEnabled = enabled)
                .onSuccess { _toastEvents.tryEmit("DRIP auto-reinvestment ${if (enabled) "ON" else "OFF"}") }
                .onFailure { _toastEvents.tryEmit(it.localizedMessage ?: "Update failed") }
        }
    }

    fun addWhitelistedAddress(current: List<String>, address: String) {
        viewModelScope.launch {
            repository.addWhitelistedAddress(current, address)
                .onSuccess { _toastEvents.tryEmit("Withdrawal address whitelisted") }
                .onFailure { _toastEvents.tryEmit(it.localizedMessage ?: "Failed to whitelist address") }
        }
    }

    fun toggleWatchlist(watchlist: Watchlist, symbol: String) {
        viewModelScope.launch {
            repository.toggleWatchlistSymbol(watchlist, symbol)
                .onFailure { _toastEvents.tryEmit(it.localizedMessage ?: "Watchlist error") }
        }
    }

    fun createWatchlist(name: String, symbols: List<String>) {
        viewModelScope.launch {
            repository.createWatchlist(name, symbols)
                .onSuccess { _toastEvents.tryEmit("Created watchlist $name") }
                .onFailure { _toastEvents.tryEmit(it.localizedMessage ?: "Failed to create watchlist") }
        }
    }

    fun createPriceAlert(symbol: String, targetPrice: Double, condition: String) {
        viewModelScope.launch {
            repository.createPriceAlert(symbol, targetPrice, condition)
                .onSuccess { _toastEvents.tryEmit("Alert set for $symbol @ $$targetPrice") }
                .onFailure { _toastEvents.tryEmit(it.localizedMessage ?: "Failed to set alert") }
        }
    }

    fun deletePriceAlert(alertId: String) {
        viewModelScope.launch {
            repository.deletePriceAlert(alertId)
        }
    }

    fun submitKyc(submission: KycSubmission, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            repository.submitKyc(submission)
                .onSuccess {
                    _toastEvents.tryEmit("KYC submitted for compliance review")
                    onDone()
                }
                .onFailure { _toastEvents.tryEmit(it.localizedMessage ?: "KYC submission failed") }
        }
    }

    fun submitDeposit(wallet: DepositWallet, amountUsd: Double, cryptoAmount: Double, txHash: String, proofBase64: String?, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            repository.submitDepositRequest(wallet, amountUsd, cryptoAmount, txHash, proofBase64)
                .onSuccess {
                    _toastEvents.tryEmit("Deposit proof submitted for admin verification")
                    onDone()
                }
                .onFailure { _toastEvents.tryEmit(it.localizedMessage ?: "Deposit submission failed") }
        }
    }

    fun submitWithdrawal(profile: UserProfile, symbol: String, network: String, destination: String, amountUsd: Double, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            repository.submitWithdrawalRequest(profile, symbol, network, destination, amountUsd)
                .onSuccess {
                    _toastEvents.tryEmit("Withdrawal submitted & funds locked pending payout")
                    onDone()
                }
                .onFailure { _toastEvents.tryEmit(it.localizedMessage ?: "Withdrawal failed") }
        }
    }

    fun placeOrder(
        profile: UserProfile,
        asset: MarketAsset,
        side: OrderSide,
        orderType: OrderType,
        timeInForce: TimeInForce,
        quantity: Double,
        limitPrice: Double?,
        stopPrice: Double?,
        takeProfitPrice: Double?,
        onSuccess: () -> Unit = {}
    ) {
        val control = marketControls.value.firstOrNull { it.symbol == asset.symbol }
        val mode = profile.modeEnum
        val existingPos = myPositions.value.firstOrNull { it.symbol == asset.symbol && it.tradeMode == mode.name }
        viewModelScope.launch {
            repository.placeOrder(
                profile = profile,
                asset = asset,
                side = side,
                orderType = orderType,
                timeInForce = timeInForce,
                quantity = quantity,
                limitPrice = limitPrice,
                stopPrice = stopPrice,
                takeProfitPrice = takeProfitPrice,
                marketControl = control,
                currentPosition = existingPos
            )
                .onSuccess { msg ->
                    _toastEvents.tryEmit(msg)
                    onSuccess()
                }
                .onFailure { _toastEvents.tryEmit(it.localizedMessage ?: "Order rejected") }
        }
    }

    fun cancelOrder(order: TradeOrder) {
        viewModelScope.launch {
            repository.cancelOrder(order)
                .onSuccess { _toastEvents.tryEmit("Order canceled") }
                .onFailure { _toastEvents.tryEmit(it.localizedMessage ?: "Cancel failed") }
        }
    }

    fun modifyOrderLimit(order: TradeOrder, newLimit: Double) {
        viewModelScope.launch {
            repository.modifyOrderLimit(order, newLimit)
                .onSuccess { _toastEvents.tryEmit("Order limit updated to $$newLimit") }
                .onFailure { _toastEvents.tryEmit(it.localizedMessage ?: "Modify failed") }
        }
    }

    fun togglePositionDrip(position: PortfolioPosition, enabled: Boolean) {
        viewModelScope.launch {
            repository.togglePositionDrip(position, enabled)
        }
    }

    fun submitTicket(subject: String, category: String, message: String, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            repository.submitSupportTicket(subject, category, message)
                .onSuccess {
                    _toastEvents.tryEmit("Support ticket submitted")
                    onDone()
                }
                .onFailure { _toastEvents.tryEmit(it.localizedMessage ?: "Ticket failed") }
        }
    }

    // --- ADMIN ACTIONS ---
    fun adminApproveKyc(kyc: KycSubmission) {
        viewModelScope.launch {
            repository.adminApproveKyc(kyc)
                .onSuccess { _toastEvents.tryEmit("KYC approved for ${kyc.fullName}") }
                .onFailure { _toastEvents.tryEmit(it.localizedMessage ?: "Approve failed") }
        }
    }

    fun adminRejectKyc(kyc: KycSubmission, reason: String) {
        viewModelScope.launch {
            repository.adminRejectKyc(kyc, reason)
                .onSuccess { _toastEvents.tryEmit("KYC rejected") }
                .onFailure { _toastEvents.tryEmit(it.localizedMessage ?: "Reject failed") }
        }
    }

    fun adminApproveDeposit(dep: DepositRequest) {
        viewModelScope.launch {
            repository.adminApproveDeposit(dep)
                .onSuccess { _toastEvents.tryEmit("Deposit credited ($${dep.amountUsd})") }
                .onFailure { _toastEvents.tryEmit(it.localizedMessage ?: "Credit failed") }
        }
    }

    fun adminRejectDeposit(dep: DepositRequest, reason: String) {
        viewModelScope.launch {
            repository.adminRejectDeposit(dep, reason)
                .onSuccess { _toastEvents.tryEmit("Deposit rejected") }
                .onFailure { _toastEvents.tryEmit(it.localizedMessage ?: "Reject failed") }
        }
    }

    fun adminApproveWithdrawal(w: WithdrawalRequest, txHash: String) {
        viewModelScope.launch {
            repository.adminApproveWithdrawal(w, txHash)
                .onSuccess { _toastEvents.tryEmit("Withdrawal marked paid") }
                .onFailure { _toastEvents.tryEmit(it.localizedMessage ?: "Approve failed") }
        }
    }

    fun adminRejectWithdrawal(w: WithdrawalRequest, reason: String) {
        viewModelScope.launch {
            repository.adminRejectWithdrawal(w, reason)
                .onSuccess { _toastEvents.tryEmit("Withdrawal rejected & locked funds refunded") }
                .onFailure { _toastEvents.tryEmit(it.localizedMessage ?: "Reject failed") }
        }
    }

    fun adminAdjustBalance(user: UserProfile, deltaUsd: Double, reason: String) {
        viewModelScope.launch {
            repository.adminAdjustBalance(user, deltaUsd, reason)
                .onSuccess { _toastEvents.tryEmit("Updated balance for ${user.displayName}") }
                .onFailure { _toastEvents.tryEmit(it.localizedMessage ?: "Balance update failed") }
        }
    }

    fun adminToggleFreeze(user: UserProfile, frozen: Boolean) {
        viewModelScope.launch {
            repository.adminToggleFreeze(user, frozen)
                .onSuccess { _toastEvents.tryEmit("${user.displayName} ${if (frozen) "frozen" else "unfrozen"}") }
                .onFailure { _toastEvents.tryEmit(it.localizedMessage ?: "Freeze update failed") }
        }
    }

    fun adminReset2fa(user: UserProfile) {
        viewModelScope.launch {
            repository.adminReset2fa(user)
                .onSuccess { _toastEvents.tryEmit("2FA reset for ${user.email}") }
                .onFailure { _toastEvents.tryEmit(it.localizedMessage ?: "Reset failed") }
        }
    }

    fun adminSaveDepositWallet(wallet: DepositWallet) {
        viewModelScope.launch {
            repository.adminSaveDepositWallet(wallet)
                .onSuccess { _toastEvents.tryEmit("Saved ${wallet.symbol} (${wallet.network}) wallet") }
                .onFailure { _toastEvents.tryEmit(it.localizedMessage ?: "Wallet save failed") }
        }
    }

    fun adminSaveMarketControl(control: MarketControl) {
        viewModelScope.launch {
            repository.adminSaveMarketControl(control)
                .onSuccess { _toastEvents.tryEmit("Updated controls for ${control.symbol}") }
                .onFailure { _toastEvents.tryEmit(it.localizedMessage ?: "Control save failed") }
        }
    }

    fun adminReplyTicket(ticket: SupportTicket, status: TicketStatus, reply: String) {
        viewModelScope.launch {
            repository.adminReplyTicket(ticket, status, reply)
                .onSuccess { _toastEvents.tryEmit("Ticket updated") }
                .onFailure { _toastEvents.tryEmit(it.localizedMessage ?: "Update failed") }
        }
    }

    fun adminAddRole(uid: String, email: String, role: AdminRoleType) {
        viewModelScope.launch {
            repository.adminAddRole(uid, email, role)
                .onSuccess { _toastEvents.tryEmit("Admin role saved") }
                .onFailure { _toastEvents.tryEmit(it.localizedMessage ?: "Role save failed") }
        }
    }

    fun adminRemoveRole(uid: String) {
        viewModelScope.launch {
            repository.adminRemoveRole(uid)
                .onSuccess { _toastEvents.tryEmit("Admin role removed") }
                .onFailure { _toastEvents.tryEmit(it.localizedMessage ?: "Remove failed") }
        }
    }

    fun adminSeedWallets() {
        viewModelScope.launch {
            repository.seedAdminDefaults(currentUser)
                .onSuccess { _toastEvents.tryEmit("Default deposit wallets seeded") }
                .onFailure { _toastEvents.tryEmit(it.localizedMessage ?: "Seed failed") }
        }
    }

    companion object {
        private const val TAG = "BrokerageVM"
    }
}

class GuestDemoViewModel(
    val marketRepository: LiveMarketRepository,
    val demoEngine: GuestDemoEngine = GuestDemoEngine()
) : ViewModel() {
    private val _toastEvents = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val toastEvents: SharedFlow<String> = _toastEvents.asSharedFlow()

    private val _selectedSymbol = MutableStateFlow("BTC")
    val selectedSymbol: StateFlow<String> = _selectedSymbol.asStateFlow()

    private val _selectedTimeframe = MutableStateFlow(Timeframe.H1)
    val selectedTimeframe: StateFlow<Timeframe> = _selectedTimeframe.asStateFlow()

    private val _assetDetail = MutableStateFlow<AssetDetailData?>(null)
    val assetDetail: StateFlow<AssetDetailData?> = _assetDetail.asStateFlow()

    private val _globalSearchResults = MutableStateFlow<List<MarketAsset>>(emptyList())
    val globalSearchResults: StateFlow<List<MarketAsset>> = _globalSearchResults.asStateFlow()

    private val _isSearchingGlobal = MutableStateFlow(false)
    val isSearchingGlobal: StateFlow<Boolean> = _isSearchingGlobal.asStateFlow()

    private var globalSearchJob: Job? = null

    val marketAssets = marketRepository.assets
    val marketSyncStatus = marketRepository.lastSyncStatus

    init {
        viewModelScope.launch {
            while (isActive) {
                marketRepository.refreshAllQuotes(forceNetwork = false)
                delay(15_000L)
            }
        }
        viewModelScope.launch {
            combine(_selectedSymbol, _selectedTimeframe, marketAssets) { s, tf, _ -> s to tf }
                .collect { (s, tf) ->
                    _assetDetail.value = marketRepository.loadAssetDetail(s, tf)
                }
        }
    }

    fun selectAsset(symbol: String) { _selectedSymbol.value = symbol }
    fun selectTimeframe(tf: Timeframe) { _selectedTimeframe.value = tf }
    fun refreshNow() {
        viewModelScope.launch {
            marketRepository.refreshAllQuotes(true)
            _assetDetail.value = marketRepository.loadAssetDetail(_selectedSymbol.value, _selectedTimeframe.value)
        }
    }

    fun searchGlobalMarkets(query: String) {
        globalSearchJob?.cancel()
        val clean = query.trim()
        if (clean.length < 2) {
            _globalSearchResults.value = emptyList()
            _isSearchingGlobal.value = false
            return
        }
        globalSearchJob = viewModelScope.launch {
            _isSearchingGlobal.value = true
            try {
                _globalSearchResults.value = marketRepository.searchGlobalMarkets(clean)
            } finally {
                _isSearchingGlobal.value = false
            }
        }
    }

    fun selectGlobalSearchAsset(asset: MarketAsset, onReady: () -> Unit = {}) {
        viewModelScope.launch {
            marketRepository.registerAndLoadAsset(asset)
            _selectedSymbol.value = asset.symbol
            _assetDetail.value = marketRepository.loadAssetDetail(asset.symbol, _selectedTimeframe.value)
            onReady()
        }
    }

    fun resetDemo() {
        demoEngine.resetDemo()
        _toastEvents.tryEmit("Virtual balance reset to ${Formatters.formatUsd(10_000.0)}")
    }

    fun placeOrder(
        asset: MarketAsset,
        side: OrderSide,
        orderType: OrderType,
        timeInForce: TimeInForce,
        quantity: Double,
        limitPrice: Double?,
        stopPrice: Double?,
        takeProfitPrice: Double?,
        onSuccess: () -> Unit = {}
    ) {
        demoEngine.placeDemoOrder(asset, side, orderType, timeInForce, quantity, limitPrice, stopPrice, takeProfitPrice)
            .onSuccess {
                _toastEvents.tryEmit(it)
                onSuccess()
            }
            .onFailure {
                _toastEvents.tryEmit(it.localizedMessage ?: "Order failed")
            }
    }
}
