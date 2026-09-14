package app.olauncher.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Process
import android.text.Spannable
import android.annotation.SuppressLint
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.view.inputmethod.BaseInputConnection
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Recycler
import app.olauncher.MainViewModel
import app.olauncher.R
import app.olauncher.StopwatchState
import app.olauncher.TimeUtilityViewModel
import app.olauncher.TimerState
import app.olauncher.data.AppModel
import app.olauncher.data.Constants
import app.olauncher.data.Prefs
import app.olauncher.databinding.FragmentAppDrawerBinding
import app.olauncher.helper.copyToClipboard
import app.olauncher.helper.deletePinnedShortcut
import app.olauncher.helper.hideKeyboard
import app.olauncher.helper.isEinkDisplay
import app.olauncher.helper.isSystemAnimationsDisabled
import app.olauncher.helper.isSystemApp
import app.olauncher.helper.openAppInfo
import app.olauncher.helper.openSearch
import app.olauncher.helper.openUrl
import app.olauncher.helper.openUrlInBrowser
import app.olauncher.helper.openWebSearch
import app.olauncher.helper.showKeyboard
import app.olauncher.helper.showToast
import app.olauncher.helper.timeutils.TimeFormatters
import app.olauncher.helper.timeutils.TimeUtility
import app.olauncher.helper.uninstall
import app.olauncher.universal.CalculatorProvider
import app.olauncher.universal.TimeUtilityProvider
import app.olauncher.universal.UniversalSearch
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.Date
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class AppDrawerFragment : BaseFragment() {

    private lateinit var prefs: Prefs
    private lateinit var adapter: AppDrawerAdapter
    private lateinit var linearLayoutManager: LinearLayoutManager
    private var searchTextView: TextView? = null
    private var cachedIsCjkKeyboard: Boolean? = null
    private var currentCalcResult: String? = null

    private var flag = Constants.FLAG_LAUNCH_APP
    private var canRename = false
    private var currentAppList: List<AppModel>? = null
    private var currentPrivateSpaceApps: List<AppModel>? = null
    private var currentPrivateSpaceLocked: Boolean = true
    private var currentPrivateSpaceAvailable: Boolean = false
    private var currentUtility: TimeUtility? = null
    private var tickerJob: Job? = null
    private var lastUtilityItemKey: String? = null
    private lateinit var notificationPermissionLauncher: ActivityResultLauncher<String>

    private val universalSearch = UniversalSearch()
    private val viewModel: MainViewModel by activityViewModels()
    private val timeViewModel: TimeUtilityViewModel by activityViewModels()
    private var _binding: FragmentAppDrawerBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentAppDrawerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        prefs = Prefs(requireContext())
        arguments?.let {
            flag = it.getInt(Constants.Key.FLAG, Constants.FLAG_LAUNCH_APP)
            canRename = it.getBoolean(Constants.Key.RENAME, false)
        }

        initPermissionLauncher()
        initViews()
        initSearch()
        initAdapter()
        initObservers()
        initUtilityObservers()
        initClickListeners()
    }

    private fun initPermissionLauncher() {
        notificationPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { /* Timer already started; notification will be suppressed if denied */ }
    }

    private fun initViews() {
        if (flag == Constants.FLAG_HIDDEN_APPS)
            binding.search.queryHint = getString(R.string.hidden_apps)
        else if (flag in Constants.FLAG_SET_HOME_APP_1..Constants.FLAG_SET_CALENDAR_APP)
            binding.search.queryHint = "Please select an app"
        try {
            searchTextView = binding.search.findViewById(R.id.search_src_text)
            searchTextView?.gravity = prefs.appLabelAlignment
            binding.tvCalcResult?.gravity = prefs.appLabelAlignment
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun initSearch() {
        binding.search.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                if (currentCalcResult != null) {
                    copyCalcResult()
                    return true
                }
                if (currentUtility is TimeUtility.Timer || currentUtility is TimeUtility.Stopwatch) {
                    handleUtilityEnter()
                    return true
                }
                if (query?.startsWith("!") == true)
                    requireContext().openUrl(Constants.URL_DUCK_SEARCH + query.replace(" ", "%20"))
                else if (adapter.itemCount == 0) {
                    val trimmedQuery = query?.trim()
                    if (flag == Constants.FLAG_LAUNCH_APP && !trimmedQuery.isNullOrEmpty())
                        requireContext().openWebSearch(trimmedQuery)
                    else
                        requireContext().openSearch(query?.trim())
                } else
                    adapter.launchSelectedOrFirst()
                return true
            }

            override fun onQueryTextChange(newText: String): Boolean {
                try {
                    val response = if (flag == Constants.FLAG_LAUNCH_APP)
                        universalSearch.response(newText)
                    else UniversalSearch.SearchResponse()

                    updateCalcResult(response.calculatorResult, newText)
                    val utility = parseTimeUtility(newText)
                    handleUtility(utility)
                    adapter.allowAutoLaunch = !isSearchComposing() && response.calculatorResult == null && utility == null
                    adapter.allowWebSearch = adapter.allowAutoLaunch

                    val provider: (() -> AppModel.TimeUtilityResult?)? = if (TimeUtilityProvider.isDisplayType(utility)) {
                        ({ buildTimeUtilityItem(utility!!) })
                    } else null

                    adapter.timeUtilityProvider = provider

                    adapter.filter.filter(newText)
                    binding.appRename.visibility =
                        if (canRename && newText.isNotBlank()) View.VISIBLE else View.GONE
                    return true
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                return false
            }
        })
    }

    private fun parseTimeUtility(text: String): TimeUtility? {
        if (isSearchComposing() || text.startsWith("!") || flag != Constants.FLAG_LAUNCH_APP) return null
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) TimeUtilityProvider.parse(text) else null
    }

    private fun handleUtility(utility: TimeUtility?) {
        currentUtility = utility
        lastUtilityItemKey = null
        when (utility) {
            is TimeUtility.Timer -> {
                timeViewModel.configureTimer(utility.durationMillis)
                showUtilityStrip()
                renderTimerStrip()
            }

            is TimeUtility.Stopwatch -> {
                showUtilityStrip()
                renderStopwatchStrip()
            }

            else -> hideUtilityStrip()
        }
        if (TimeUtilityProvider.needsTicker(utility)) startTicker() else stopTicker()
    }

    private fun showUtilityStrip() {
        if (binding.utilityLayout.visibility != View.VISIBLE)
            binding.utilityLayout.visibility = View.VISIBLE
    }

    private fun hideUtilityStrip() {
        if (binding.utilityLayout.visibility != View.GONE)
            binding.utilityLayout.visibility = View.GONE
    }

    private fun startTicker() {
        if (tickerJob?.isActive == true) return
        tickerJob = viewLifecycleOwner.lifecycleScope.launch {
            while (isActive) {
                tickLiveUtility()
                delay(200)
            }
        }
    }

    private fun stopTicker() {
        tickerJob?.cancel()
        tickerJob = null
    }

    private fun tickLiveUtility() {
        when (val utility = currentUtility) {
            is TimeUtility.Timer -> timeViewModel.tickTimer()
            is TimeUtility.Stopwatch -> timeViewModel.tickStopwatch()
            is TimeUtility.Now, is TimeUtility.TimeIn -> refreshUtilityListItem()
            else -> {}
        }
    }

    private fun handleUtilityEnter() {
        when (val utility = currentUtility) {
            is TimeUtility.Timer -> when (timeViewModel.timerState.value) {
                TimerState.IDLE, TimerState.FINISHED -> requestTimerStart()
                TimerState.RUNNING -> timeViewModel.pauseTimer()
                TimerState.PAUSED -> timeViewModel.startTimer()
                else -> {}
            }

            is TimeUtility.Stopwatch -> when (timeViewModel.stopwatchState.value) {
                StopwatchState.IDLE, StopwatchState.PAUSED -> timeViewModel.startStopwatch()
                StopwatchState.RUNNING -> timeViewModel.pauseStopwatch()
                else -> {}
            }

            else -> {}
        }
    }

    private fun requestTimerStart() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            requireContext().checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        timeViewModel.startTimer()
    }

    private fun renderTimerStrip() {
        if (currentUtility !is TimeUtility.Timer) return
        if (binding.utilityLayout.visibility != View.VISIBLE) return

        val state = timeViewModel.timerState.value ?: TimerState.IDLE
        val remaining = timeViewModel.timerRemaining.value ?: 0L

        binding.tvUtilityTitle.visibility = View.VISIBLE
        binding.tvUtilityTitle.text = getString(R.string.timer)
        when (state) {
            TimerState.IDLE -> {
                binding.tvUtilityValue.text = TimeFormatters.formatTimer(remaining)
                binding.tvUtilityHint.text = getString(R.string.timer_ready_hint)
                binding.tvUtilityPrimary.text = getString(R.string.start)
                binding.tvUtilityPrimary.setOnClickListener { requestTimerStart() }
                binding.tvUtilitySecondary.visibility = View.GONE
            }

            TimerState.RUNNING -> {
                binding.tvUtilityValue.text = TimeFormatters.formatTimer(remaining)
                binding.tvUtilityHint.text = getString(R.string.timer_running_hint)
                binding.tvUtilityPrimary.text = getString(R.string.pause)
                binding.tvUtilityPrimary.setOnClickListener { timeViewModel.pauseTimer() }
                binding.tvUtilitySecondary.visibility = View.GONE
            }

            TimerState.PAUSED -> {
                binding.tvUtilityValue.text = TimeFormatters.formatTimer(remaining)
                binding.tvUtilityHint.text = getString(R.string.timer_paused_hint)
                binding.tvUtilityPrimary.text = getString(R.string.resume)
                binding.tvUtilityPrimary.setOnClickListener { requestTimerStart() }
                binding.tvUtilitySecondary.apply {
                    visibility = View.VISIBLE
                    text = getString(R.string.reset)
                    setOnClickListener {
                        timeViewModel.resetTimer()
                        renderTimerStrip()
                    }
                }
            }

            TimerState.FINISHED -> {
                binding.tvUtilityValue.text = getString(R.string.timer_done)
                binding.tvUtilityHint.text = getString(R.string.timer_finished_hint)
                binding.tvUtilityPrimary.text = getString(R.string.reset)
                binding.tvUtilityPrimary.setOnClickListener {
                    timeViewModel.resetTimer()
                    renderTimerStrip()
                }
                binding.tvUtilitySecondary.visibility = View.GONE
            }
        }
    }

    private fun renderStopwatchStrip() {
        if (currentUtility !is TimeUtility.Stopwatch) return
        if (binding.utilityLayout.visibility != View.VISIBLE) return

        val state = timeViewModel.stopwatchState.value ?: StopwatchState.IDLE
        val elapsed = timeViewModel.stopwatchElapsed.value ?: 0L

        binding.tvUtilityTitle.visibility = View.VISIBLE
        binding.tvUtilityTitle.text = getString(R.string.stopwatch)
        when (state) {
            StopwatchState.IDLE -> {
                binding.tvUtilityValue.text = TimeFormatters.formatStopwatch(elapsed)
                binding.tvUtilityHint.text = getString(R.string.stopwatch_ready_hint)
                binding.tvUtilityPrimary.text = getString(R.string.start)
                binding.tvUtilityPrimary.setOnClickListener { timeViewModel.startStopwatch() }
                binding.tvUtilitySecondary.visibility = View.GONE
            }

            StopwatchState.RUNNING -> {
                binding.tvUtilityValue.text = TimeFormatters.formatStopwatch(elapsed)
                binding.tvUtilityHint.text = getString(R.string.stopwatch_running_hint)
                binding.tvUtilityPrimary.text = getString(R.string.pause)
                binding.tvUtilityPrimary.setOnClickListener { timeViewModel.pauseStopwatch() }
                binding.tvUtilitySecondary.visibility = View.GONE
            }

            StopwatchState.PAUSED -> {
                binding.tvUtilityValue.text = TimeFormatters.formatStopwatch(elapsed)
                binding.tvUtilityHint.text = getString(R.string.stopwatch_paused_hint)
                binding.tvUtilityPrimary.text = getString(R.string.resume)
                binding.tvUtilityPrimary.setOnClickListener { timeViewModel.startStopwatch() }
                binding.tvUtilitySecondary.apply {
                    visibility = View.VISIBLE
                    text = getString(R.string.reset)
                    setOnClickListener {
                        timeViewModel.resetStopwatch()
                        renderStopwatchStrip()
                    }
                }
            }
        }
    }

    private fun initUtilityObservers() {
        timeViewModel.timerState.observe(viewLifecycleOwner) { renderTimerStrip() }
        timeViewModel.timerRemaining.observe(viewLifecycleOwner) { renderTimerStrip() }
        timeViewModel.stopwatchState.observe(viewLifecycleOwner) { renderStopwatchStrip() }
        timeViewModel.stopwatchElapsed.observe(viewLifecycleOwner) { renderStopwatchStrip() }
    }

    @SuppressLint("NewApi")
    private fun buildTimeUtilityItem(utility: TimeUtility): AppModel.TimeUtilityResult {
        val resources = requireContext().resources
        return when (utility) {
            is TimeUtility.Now -> {
                val now = ZonedDateTime.now()
                val time = android.text.format.DateFormat.getTimeFormat(requireContext())
                    .format(Date(now.toInstant().toEpochMilli()))
                AppModel.TimeUtilityResult(
                    title = time,
                    subtitle = TimeFormatters.fullDate(now.toLocalDate())
                )
            }

            is TimeUtility.DayOfWeek -> AppModel.TimeUtilityResult(
                title = TimeFormatters.dayName(utility.date),
                subtitle = TimeFormatters.fullDate(utility.date)
            )

            is TimeUtility.DaysBetween -> {
                val days = TimeFormatters.daysBetween(utility.from, utility.to).toInt()
                AppModel.TimeUtilityResult(
                    title = resources.getQuantityString(R.plurals.days_count, days, days),
                    subtitle = "${TimeFormatters.mediumDate(utility.from)} → ${TimeFormatters.mediumDate(utility.to)}"
                )
            }

            is TimeUtility.DaysUntil -> {
                val days = TimeFormatters.daysBetween(LocalDate.now(), utility.date).toInt()
                val title = when {
                    days > 0 -> resources.getQuantityString(R.plurals.days_count, days, days)
                    days < 0 -> resources.getQuantityString(R.plurals.days_ago_count, -days, -days)
                    else -> getString(R.string.today)
                }
                AppModel.TimeUtilityResult(
                    title = title,
                    subtitle = TimeFormatters.fullDate(utility.date)
                )
            }

            is TimeUtility.RelativeDate -> AppModel.TimeUtilityResult(
                title = utility.label,
                subtitle = TimeFormatters.fullDate(utility.date)
            )

            is TimeUtility.TimeIn -> {
                val zoneId = ZoneId.of(utility.zoneId)
                val now = ZonedDateTime.now(zoneId)
                val epochMillis = now.toInstant().toEpochMilli()
                val time = android.text.format.DateFormat.getTimeFormat(requireContext())
                    .format(Date(epochMillis))
                val offset = TimeFormatters.formatUtcOffset(zoneId, epochMillis)
                val abbreviation = TimeFormatters.zoneAbbreviation(zoneId, epochMillis)
                val title = if (abbreviation.isEmpty()) "$time $offset" else "$time $abbreviation ($offset)"
                AppModel.TimeUtilityResult(
                    title = title,
                    subtitle = "${utility.city} · ${TimeFormatters.mediumDate(now.toLocalDate())}"
                )
            }

            else -> AppModel.TimeUtilityResult(title = "")
        }
    }

    private fun refreshUtilityListItem() {
        val utility = currentUtility ?: return
        if (utility !is TimeUtility.Now && utility !is TimeUtility.TimeIn) return
        val item = buildTimeUtilityItem(utility)
        val key = "${item.title}|${item.subtitle}"
        if (key == lastUtilityItemKey) return
        lastUtilityItemKey = key
        if (adapter.appFilteredList.size == 1 &&
            adapter.appFilteredList.firstOrNull() is AppModel.TimeUtilityResult
        ) {
            adapter.appFilteredList[0] = item
            adapter.notifyItemChanged(0)
        }
    }

    private fun updateCalcResult(result: String?, text: String) {
        if (result == null || flag != Constants.FLAG_LAUNCH_APP || text.isBlank()) {
            hideCalcResult()
            return
        }
        currentCalcResult = result
        binding.tvCalcResult?.text = getString(R.string.result_preview, result)
        binding.tvCalcResult?.visibility = View.VISIBLE
    }

    private fun hideCalcResult() {
        currentCalcResult = null
        binding.tvCalcResult?.visibility = View.GONE
    }

    private fun copyCalcResult() {
        val result = currentCalcResult ?: return
        requireContext().copyToClipboard(result)
    }

    private fun isSearchComposing(): Boolean {
        val text = searchTextView?.text
        if (text !is Spannable) return false
        val start = BaseInputConnection.getComposingSpanStart(text)
        val end = BaseInputConnection.getComposingSpanEnd(text)
        if (start !in 0 until end) return false
        return isCjkKeyboard()
    }

    private fun isCjkKeyboard(): Boolean {
        cachedIsCjkKeyboard?.let { return it }
        val result = try {
            val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            val subtype = imm.currentInputMethodSubtype
            val language = when {
                subtype == null -> ""
                subtype.languageTag.isNotEmpty() -> subtype.languageTag
                else -> subtype.locale
            }
            language.startsWith("zh") || language.startsWith("ja") || language.startsWith("ko")
        } catch (e: Exception) {
            false
        }
        cachedIsCjkKeyboard = result
        return result
    }

    private fun initAdapter() {
        adapter = AppDrawerAdapter(
            context = requireContext(),
            flag = flag,
            appLabelGravity = prefs.appLabelAlignment,
            appClickListener = { appModel ->
                when (appModel) {
                    is AppModel.WebSearchResult -> requireContext().openWebSearch(appModel.query)
                    is AppModel.UrlResult -> requireContext().openUrlInBrowser(appModel.url)
                    else -> {
                        viewModel.selectedApp(appModel, flag)
                        if (flag == Constants.FLAG_LAUNCH_APP || flag == Constants.FLAG_HIDDEN_APPS)
                            findNavController().popBackStack(R.id.mainFragment, false)
                        else
                            findNavController().popBackStack()
                    }
                }
            },
            appInfoListener = {
                openAppInfo(requireContext(), it.user, it.appPackage)
                findNavController().popBackStack(R.id.mainFragment, false)
            },
            appDeleteListener = { appModel ->
                when (appModel) {
                    is AppModel.PrivateSpaceHeader -> {}
                    is AppModel.WebSearchResult -> {}
                    is AppModel.TimeUtilityResult -> {}
                    is AppModel.UrlResult -> {}
                    is AppModel.PinnedShortcut ->
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
                            requireContext().deletePinnedShortcut(
                                packageName = appModel.appPackage,
                                shortcutIdToDelete = appModel.shortcutId,
                                user = appModel.user,
                            )
                        }

                    is AppModel.App -> {
                        if (appModel.user != Process.myUserHandle()) {
                            openAppInfo(requireContext(), appModel.user, appModel.appPackage)
                        } else if (requireContext().isSystemApp(appModel.appPackage, appModel.user)) {
                            requireContext().showToast(getString(R.string.system_app_cannot_delete))
                            openAppInfo(requireContext(), appModel.user, appModel.appPackage)
                        } else {
                            requireContext().uninstall(appModel.appPackage)
                        }
                    }
                }
                viewModel.getAppList()
            },
            appHideListener = { appModel, position ->
                if (appModel is AppModel.PinnedShortcut) {
                    requireContext().showToast("Hiding pinned shortcuts is not supported")
                    return@AppDrawerAdapter
                }
                adapter.appFilteredList.removeAt(position)
                adapter.notifyItemRemoved(position)
                adapter.appsList.remove(appModel)

                val newSet = mutableSetOf<String>()
                newSet.addAll(prefs.hiddenApps)
                if (flag == Constants.FLAG_HIDDEN_APPS)
                    newSet.remove(appModel.appPackage + "|" + appModel.user.toString())
                else
                    newSet.add(appModel.appPackage + "|" + appModel.user.toString())

                prefs.hiddenApps = newSet
                if (newSet.isEmpty())
                    findNavController().popBackStack()
                if (prefs.firstHide) {
                    binding.search.hideKeyboard()
                    prefs.firstHide = false
                    viewModel.showDialog.postValue(Constants.Dialog.HIDDEN)
                    findNavController().navigate(R.id.action_appListFragment_to_settingsFragment2)
                }
                viewModel.getAppList()
                viewModel.getHiddenApps()
            },
            appRenameListener = { appModel, renameLabel ->
                val identifier = when (appModel) {
                    is AppModel.PinnedShortcut -> appModel.identity
                    is AppModel.App -> appModel.appPackage
                    else -> return@AppDrawerAdapter
                }
                prefs.setAppRenameLabel(identifier, renameLabel)
                viewModel.getAppList()
            },
            privateSpaceToggleListener = {
                viewModel.togglePrivateSpaceLock()
            },
            privateSpaceSettingsListener = {
                viewModel.openPrivateSpaceSettings()
                findNavController().popBackStack(R.id.mainFragment, false)
            }
        )

        linearLayoutManager = object : LinearLayoutManager(requireContext()) {
            override fun scrollVerticallyBy(
                dx: Int,
                recycler: Recycler,
                state: RecyclerView.State,
            ): Int {
                val scrollRange = super.scrollVerticallyBy(dx, recycler, state)
                val overScroll = dx - scrollRange
                if (overScroll < -10 && binding.recyclerView.scrollState == RecyclerView.SCROLL_STATE_DRAGGING)
                    checkMessageAndExit()
                return scrollRange
            }
        }

        binding.recyclerView.layoutManager = linearLayoutManager
        binding.recyclerView.adapter = adapter
        binding.recyclerView.addOnScrollListener(getRecyclerViewOnScrollListener())
        binding.recyclerView.itemAnimator = null
        if (requireContext().isEinkDisplay())
            binding.recyclerView.overScrollMode = View.OVER_SCROLL_NEVER
        else if (requireContext().isSystemAnimationsDisabled().not())
            binding.recyclerView.layoutAnimation =
                AnimationUtils.loadLayoutAnimation(requireContext(), R.anim.layout_anim_from_bottom)
    }

    private fun initObservers() {
        viewModel.firstOpen.observe(viewLifecycleOwner) {
        }
        if (flag == Constants.FLAG_HIDDEN_APPS) {
            viewModel.hiddenApps.observe(viewLifecycleOwner) {
                it?.let {
                    adapter.setAppList(it.toMutableList())
                }
            }
        } else {
            viewModel.appList.observe(viewLifecycleOwner) {
                currentAppList = it
                updateCombinedAppList()
            }
            if (flag == Constants.FLAG_LAUNCH_APP) {
                viewModel.privateSpaceAvailable.observe(viewLifecycleOwner) {
                    currentPrivateSpaceAvailable = it
                    updateCombinedAppList()
                }
                viewModel.privateSpaceLocked.observe(viewLifecycleOwner) {
                    currentPrivateSpaceLocked = it
                    updateCombinedAppList()
                }
                viewModel.privateSpaceApps.observe(viewLifecycleOwner) {
                    currentPrivateSpaceApps = it
                    updateCombinedAppList()
                }
            }
        }
    }

    private fun updateCombinedAppList() {
        val apps = currentAppList ?: return
        val combined = apps.toMutableList()

        if (flag == Constants.FLAG_LAUNCH_APP && currentPrivateSpaceAvailable) {
            combined.add(AppModel.PrivateSpaceHeader(isLocked = currentPrivateSpaceLocked))
            if (!currentPrivateSpaceLocked) {
                currentPrivateSpaceApps?.let { combined.addAll(it) }
            }
        }

        adapter.setAppList(combined)
        adapter.filter.filter(binding.search.query)
    }

    private fun initClickListeners() {
        binding.tvCalcResult?.setOnClickListener { copyCalcResult() }
        binding.appRename.setOnClickListener {
            val name = binding.search.query.toString().trim()
            if (name.isEmpty()) {
                requireContext().showToast(getString(R.string.type_a_new_app_name_first))
                binding.search.showKeyboard()
                return@setOnClickListener
            }

            when (flag) {
                Constants.FLAG_SET_HOME_APP_1 -> prefs.appName1 = name
                Constants.FLAG_SET_HOME_APP_2 -> prefs.appName2 = name
                Constants.FLAG_SET_HOME_APP_3 -> prefs.appName3 = name
                Constants.FLAG_SET_HOME_APP_4 -> prefs.appName4 = name
                Constants.FLAG_SET_HOME_APP_5 -> prefs.appName5 = name
                Constants.FLAG_SET_HOME_APP_6 -> prefs.appName6 = name
                Constants.FLAG_SET_HOME_APP_7 -> prefs.appName7 = name
                Constants.FLAG_SET_HOME_APP_8 -> prefs.appName8 = name
            }
            findNavController().popBackStack()
        }
    }

    private fun initSearchKeyboardNavigation() {
        searchTextView?.setOnKeyListener { _, keyCode, event ->
            if (event.action != KeyEvent.ACTION_DOWN || isSearchComposing()) return@setOnKeyListener false
            when (keyCode) {
                KeyEvent.KEYCODE_DPAD_DOWN -> {
                    if (adapter.hasQuery) {
                        if (adapter.moveSelection(1))
                            linearLayoutManager.scrollToPositionWithOffset(adapter.selectedPosition, 0)
                        true
                    } else false
                }

                KeyEvent.KEYCODE_DPAD_UP -> {
                    if (adapter.hasQuery && adapter.selectedPosition > 0) {
                        if (adapter.moveSelection(-1))
                            linearLayoutManager.scrollToPositionWithOffset(adapter.selectedPosition, 0)
                        true
                    } else false
                }

                KeyEvent.KEYCODE_ESCAPE -> {
                    binding.search.hideKeyboard()
                    true
                }

                else -> false
            }
        }
    }

    private fun getRecyclerViewOnScrollListener(): RecyclerView.OnScrollListener {
        return object : RecyclerView.OnScrollListener() {

            var onTop = false

            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                when (newState) {

                    RecyclerView.SCROLL_STATE_DRAGGING -> {
                        onTop = !recyclerView.canScrollVertically(-1)
                        if (onTop)
                            binding.search.hideKeyboard()
                    }

                    RecyclerView.SCROLL_STATE_IDLE -> {
                        if (!recyclerView.canScrollVertically(1))
                            binding.search.hideKeyboard()
                        else if (!recyclerView.canScrollVertically(-1))
                            if (!onTop && isRemoving.not())
                                binding.search.showKeyboard(prefs.autoShowKeyboard)
                    }
                }
            }
        }
    }

    private fun checkMessageAndExit() {
        findNavController().popBackStack()
        if (flag == Constants.FLAG_LAUNCH_APP)
            viewModel.checkForMessages.call()
    }

    override fun onStart() {
        super.onStart()
        cachedIsCjkKeyboard = null
        binding.search.showKeyboard(prefs.autoShowKeyboard)
        initSearchKeyboardNavigation()
        if (TimeUtilityProvider.needsTicker(currentUtility)) {
            startTicker()
            tickLiveUtility()
        }
    }

    override fun onStop() {
        stopTicker()
        binding.search.hideKeyboard()
        super.onStop()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        stopTicker()
        currentUtility = null
        searchTextView = null
        _binding = null
    }
}