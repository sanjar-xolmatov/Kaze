package app.olauncher.ui

import android.content.Context
import android.content.pm.LauncherApps
import android.os.UserHandle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Filter
import android.widget.Filterable
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import app.olauncher.R
import app.olauncher.data.AppModel
import app.olauncher.data.Constants
import app.olauncher.databinding.AdapterAppDrawerBinding
import app.olauncher.databinding.AdapterPrivateSpaceHeaderBinding
import app.olauncher.databinding.AdapterTimeUtilityBinding
import app.olauncher.databinding.AdapterWebSearchBinding
import app.olauncher.helper.hideKeyboard
import app.olauncher.helper.isSystemApp
import app.olauncher.helper.showKeyboard
import app.olauncher.universal.AppMatcher
import app.olauncher.universal.UrlProvider

class AppDrawerAdapter(
    private val context: Context,
    private var flag: Int,
    private val appLabelGravity: Int,
    private val appClickListener: (AppModel) -> Unit,
    private val appInfoListener: (AppModel) -> Unit,
    private val appDeleteListener: (AppModel) -> Unit,
    private val appHideListener: (AppModel, Int) -> Unit,
    private val appRenameListener: (AppModel, String) -> Unit,
    private val privateSpaceToggleListener: () -> Unit = {},
    private val privateSpaceSettingsListener: () -> Unit = {},
) : ListAdapter<AppModel, RecyclerView.ViewHolder>(DIFF_CALLBACK), Filterable {

    companion object {
        const val VIEW_TYPE_APP = 0
        const val VIEW_TYPE_PRIVATE_HEADER = 1
        const val VIEW_TYPE_WEB_SEARCH = 2
        const val VIEW_TYPE_TIME_UTILITY = 3
        const val VIEW_TYPE_URL = 4

        val DIFF_CALLBACK = object : DiffUtil.ItemCallback<AppModel>() {
            override fun areItemsTheSame(oldItem: AppModel, newItem: AppModel): Boolean = when {
                oldItem is AppModel.App && newItem is AppModel.App ->
                    oldItem.appPackage == newItem.appPackage && oldItem.user == newItem.user

                oldItem is AppModel.PinnedShortcut && newItem is AppModel.PinnedShortcut ->
                    oldItem.identity == newItem.identity

                oldItem is AppModel.PrivateSpaceHeader && newItem is AppModel.PrivateSpaceHeader -> true

                oldItem is AppModel.WebSearchResult && newItem is AppModel.WebSearchResult ->
                    oldItem.query == newItem.query

                oldItem is AppModel.TimeUtilityResult && newItem is AppModel.TimeUtilityResult ->
                    oldItem.title == newItem.title && oldItem.subtitle == newItem.subtitle

                oldItem is AppModel.UrlResult && newItem is AppModel.UrlResult ->
                    oldItem.url == newItem.url

                else -> false
            }

            override fun areContentsTheSame(oldItem: AppModel, newItem: AppModel): Boolean =
                oldItem == newItem
        }
    }

    private var autoLaunch = true
    private var isBangSearch = false
    var allowAutoLaunch = true
    var allowWebSearch = true
    var hasQuery = false
        private set
    private val appFilter = createAppFilter()
    private val myUserHandle = android.os.Process.myUserHandle()

    var appsList: MutableList<AppModel> = mutableListOf()
    var appFilteredList: MutableList<AppModel> = mutableListOf()
    var timeUtilityProvider: (() -> AppModel.TimeUtilityResult?)? = null

    var selectedPosition = 0
        private set

    fun moveSelection(delta: Int): Boolean {
        val last = appFilteredList.lastIndex
        if (last < 0) return false
        val next = (selectedPosition + delta).coerceIn(0, last)
        if (next == selectedPosition) return false
        val previous = selectedPosition
        selectedPosition = next
        if (previous in 0..last) notifyItemChanged(previous)
        notifyItemChanged(next)
        return true
    }

    fun selectedModel(): AppModel? = appFilteredList.getOrNull(selectedPosition)

    fun launchSelectedOrFirst() {
        val list = appFilteredList
        val selected = if (list.isEmpty()) null else list.getOrNull(selectedPosition)
        val target = selected?.takeIf { it.isLaunchable() }
            ?: list.firstOrNull { it.isLaunchable() }
        if (target != null) appClickListener(target)
    }

    private fun AppModel.isLaunchable(): Boolean =
        this !is AppModel.PrivateSpaceHeader &&
            (this !is AppModel.App || this.appPackage.isNotEmpty())

    private fun resetSelection() {
        if (!hasQuery) {
            selectedPosition = 0
            return
        }
        val last = appFilteredList.lastIndex
        val clamped = selectedPosition.coerceIn(0, if (last < 0) 0 else last)
        if (clamped != selectedPosition) selectedPosition = clamped
    }

    override fun getItemViewType(position: Int): Int {
        return when (appFilteredList.getOrNull(position)) {
            is AppModel.PrivateSpaceHeader -> VIEW_TYPE_PRIVATE_HEADER
            is AppModel.WebSearchResult -> VIEW_TYPE_WEB_SEARCH
            is AppModel.TimeUtilityResult -> VIEW_TYPE_TIME_UTILITY
            is AppModel.UrlResult -> VIEW_TYPE_URL
            else -> VIEW_TYPE_APP
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_TYPE_PRIVATE_HEADER -> PrivateSpaceHeaderViewHolder(
                AdapterPrivateSpaceHeaderBinding.inflate(inflater, parent, false)
            )

            VIEW_TYPE_WEB_SEARCH -> WebSearchViewHolder(
                AdapterWebSearchBinding.inflate(inflater, parent, false)
            )

            VIEW_TYPE_TIME_UTILITY -> TimeUtilityViewHolder(
                AdapterTimeUtilityBinding.inflate(inflater, parent, false)
            )

            VIEW_TYPE_URL -> UrlViewHolder(
                AdapterTimeUtilityBinding.inflate(inflater, parent, false)
            )

            else -> ViewHolder(
                AdapterAppDrawerBinding.inflate(inflater, parent, false)
            )
        }.also { vh ->
            vh.itemView.background = context.getDrawable(R.drawable.search_selection_bg)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        try {
            if (appFilteredList.isEmpty() || position == RecyclerView.NO_POSITION) return
            val appModel = appFilteredList[holder.bindingAdapterPosition]
            holder.itemView.isSelected = hasQuery && holder.bindingAdapterPosition == selectedPosition
            when (holder) {
                is PrivateSpaceHeaderViewHolder -> holder.bind(
                    appLabelGravity, privateSpaceToggleListener, privateSpaceSettingsListener,
                )

                is WebSearchViewHolder -> holder.bind(
                    appLabelGravity, (appModel as AppModel.WebSearchResult).query, appClickListener,
                )

                is TimeUtilityViewHolder -> holder.bind(
                    appLabelGravity, appModel as AppModel.TimeUtilityResult,
                )

                is UrlViewHolder -> holder.bind(
                    appLabelGravity, appModel as AppModel.UrlResult, appClickListener,
                )

                is ViewHolder -> holder.bind(
                    flag, appLabelGravity, myUserHandle, appModel,
                    appClickListener, appDeleteListener, appInfoListener,
                    appHideListener, appRenameListener,
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onViewAttachedToWindow(holder: RecyclerView.ViewHolder) {
        super.onViewAttachedToWindow(holder)
        holder.itemView.isSelected = hasQuery && holder.bindingAdapterPosition == selectedPosition
    }

    override fun getFilter(): Filter = this.appFilter

    private fun createAppFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(charSearch: CharSequence?): FilterResults {
                isBangSearch = charSearch?.startsWith("!") ?: false
                autoLaunch = allowAutoLaunch && (charSearch?.startsWith(" ")?.not() ?: true)
                val q = charSearch?.toString() ?: ""
                hasQuery = q.isNotBlank()

                val result = if (!hasQuery) {
                    appsList.toMutableList()
                } else {
                    appsList.mapNotNull { app ->
                        if (app is AppModel.PrivateSpaceHeader) null
                        else AppMatcher.score(app.appLabel, app.appPackage, q)?.let { score ->
                            app to score
                        }
                    }.sortedBy { it.second }.map { it.first }.toMutableList()
                }

                if (hasQuery && result.isEmpty()) {
                    val webSearchCandidate = q.trim()
                    if (webSearchCandidate.isNotEmpty() &&
                        flag == Constants.FLAG_LAUNCH_APP &&
                        isBangSearch.not()
                    ) {
                        val urlRow = UrlProvider.row(webSearchCandidate, context.getString(R.string.open_url))
                        if (urlRow != null) result.add(urlRow)
                        else if (allowWebSearch) result.add(AppModel.WebSearchResult(webSearchCandidate))
                    }
                }

                val filterResults = FilterResults()
                filterResults.values = result
                return filterResults
            }

            @Suppress("UNCHECKED_CAST")
            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                results?.values?.let {
                    val items = it as MutableList<AppModel>
                    val utilityItem = timeUtilityProvider?.invoke()
                    if (utilityItem != null) {
                        val hasRealApps = items.any {
                            it is AppModel.App || it is AppModel.PinnedShortcut
                        }
                        if (!hasRealApps) {
                            items.removeAll { it is AppModel.WebSearchResult }
                            items.add(utilityItem)
                        }
                    }
                    appFilteredList = items
                    resetSelection()
                    submitList(appFilteredList) {
                        autoLaunch()
                    }
                }
            }
        }
    }

    private fun autoLaunch() {
        try {
            if (itemCount == 1
                && autoLaunch
                && isBangSearch.not()
                && flag == Constants.FLAG_LAUNCH_APP
                && appFilteredList.isNotEmpty()
                && appFilteredList[0] !is AppModel.PrivateSpaceHeader
                && appFilteredList[0] !is AppModel.WebSearchResult
                && appFilteredList[0] !is AppModel.TimeUtilityResult
                && appFilteredList[0] !is AppModel.UrlResult
            ) appClickListener(appFilteredList[0])
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun setAppList(appsList: MutableList<AppModel>) {
        appsList.add(
            AppModel.App(
                appLabel = "",
                key = null,
                appPackage = "",
                activityClassName = "",
                isNew = false,
                user = android.os.Process.myUserHandle()
            )
        )
        this.appsList = appsList
        this.appFilteredList = appsList
        selectedPosition = 0
        submitList(appsList)
    }

    fun launchFirstInList() {
        val first = appFilteredList.firstOrNull { it.isLaunchable() }
        if (first != null) appClickListener(first)
    }

    class PrivateSpaceHeaderViewHolder(private val binding: AdapterPrivateSpaceHeaderBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(
            appLabelGravity: Int,
            toggleListener: () -> Unit,
            settingsListener: () -> Unit,
        ) = with(binding) {
            privateSpaceTitle.gravity = appLabelGravity
            privateSpaceTitle.setOnClickListener { toggleListener() }
            privateSpaceTitle.setOnLongClickListener {
                settingsListener()
                true
            }
        }
    }

    class WebSearchViewHolder(private val binding: AdapterWebSearchBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(
            appLabelGravity: Int,
            query: String,
            clickListener: (AppModel) -> Unit,
        ) = with(binding) {
            webSearchTitle.gravity = appLabelGravity
            webSearchQuery.gravity = appLabelGravity
            webSearchQuery.text = query
            root.setOnClickListener { clickListener(AppModel.WebSearchResult(query)) }
        }
    }

    class TimeUtilityViewHolder(private val binding: AdapterTimeUtilityBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(
            appLabelGravity: Int,
            item: AppModel.TimeUtilityResult,
        ) = with(binding) {
            timeUtilityTitle.gravity = appLabelGravity
            timeUtilityTitle.text = item.title
            if (item.subtitle.isNotEmpty()) {
                timeUtilitySubtitle.visibility = View.VISIBLE
                timeUtilitySubtitle.gravity = appLabelGravity
                timeUtilitySubtitle.text = item.subtitle
            } else {
                timeUtilitySubtitle.visibility = View.GONE
            }
        }
    }

    class UrlViewHolder(private val binding: AdapterTimeUtilityBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(
            appLabelGravity: Int,
            item: AppModel.UrlResult,
            clickListener: (AppModel) -> Unit,
        ) = with(binding) {
            timeUtilityTitle.gravity = appLabelGravity
            timeUtilityTitle.text = item.title
            timeUtilitySubtitle.visibility = View.VISIBLE
            timeUtilitySubtitle.gravity = appLabelGravity
            timeUtilitySubtitle.text = item.subtitle
            root.setOnClickListener { clickListener(item) }
        }
    }

    class ViewHolder(private val binding: AdapterAppDrawerBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(
            flag: Int,
            appLabelGravity: Int,
            myUserHandle: UserHandle,
            appModel: AppModel,
            clickListener: (AppModel) -> Unit,
            appDeleteListener: (AppModel) -> Unit,
            appInfoListener: (AppModel) -> Unit,
            appHideListener: (AppModel, Int) -> Unit,
            appRenameListener: (AppModel, String) -> Unit,
        ) = with(binding) {
            appHideLayout.visibility = View.GONE
            renameLayout.visibility = View.GONE
            appTitle.visibility = View.VISIBLE

            appTitle.text = buildString {
                append(appModel.appLabel)
                if (appModel.isNew) append(" ✦")
            }
            appTitle.gravity = appLabelGravity
            otherProfileIndicator.isVisible = appModel.user != myUserHandle

            appTitle.setOnClickListener { clickListener(appModel) }

            appTitle.setOnLongClickListener {
                if (appModel.appPackage.isNotEmpty()) {
                    appDelete.alpha = when (
                        appModel is AppModel.PinnedShortcut || !root.context.isSystemApp(appModel.appPackage, appModel.user)
                    ) {
                        true -> 1.0f
                        false -> 0.5f
                    }
                    appHide.text = if (flag == Constants.FLAG_HIDDEN_APPS)
                        root.context.getString(R.string.adapter_show)
                    else
                        root.context.getString(R.string.adapter_hide)
                    appTitle.visibility = View.INVISIBLE
                    appHide.alpha = when (appModel is AppModel.PinnedShortcut) {
                        true -> 0.5f
                        false -> 1.0f
                    }
                    appHideLayout.visibility = View.VISIBLE
                    appRename.isVisible = flag != Constants.FLAG_HIDDEN_APPS
                }
                true
            }

            appRename.setOnClickListener {
                if (appModel.appPackage.isNotEmpty()) {
                    etAppRename.hint = getAppName(etAppRename.context, appModel.appPackage, appModel.user)
                    etAppRename.setText(appModel.appLabel)
                    etAppRename.setSelectAllOnFocus(true)
                    renameLayout.visibility = View.VISIBLE
                    appHideLayout.visibility = View.GONE
                    etAppRename.showKeyboard()
                    etAppRename.imeOptions = EditorInfo.IME_ACTION_DONE
                }
            }
            etAppRename.onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
                appTitle.visibility = if (hasFocus) View.INVISIBLE else View.VISIBLE
            }
            etAppRename.addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {
                    etAppRename.hint = getAppName(etAppRename.context, appModel.appPackage, appModel.user)
                }

                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int,
                ) {
                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    etAppRename.hint = ""
                }
            })
            etAppRename.setOnEditorActionListener { _, actionCode, _ ->
                if (actionCode == EditorInfo.IME_ACTION_DONE) {
                    val renameLabel = etAppRename.text.toString().trim()
                    if (renameLabel.isNotBlank() && appModel.appPackage.isNotBlank()) {
                        appRenameListener(appModel, renameLabel)
                        renameLayout.visibility = View.GONE
                    }
                    true
                }
                false
            }
            tvSaveRename.setOnClickListener {
                etAppRename.hideKeyboard()
                val renameLabel = etAppRename.text.toString().trim()
                if (renameLabel.isNotBlank() && appModel.appPackage.isNotBlank()) {
                    appRenameListener(appModel, renameLabel)
                    renameLayout.visibility = View.GONE
                } else {
                    appRenameListener(
                        appModel,
                        getAppName(etAppRename.context, appModel.appPackage, appModel.user)
                    )
                    renameLayout.visibility = View.GONE
                }
            }
            appInfo.setOnClickListener { appInfoListener(appModel) }
            appDelete.setOnClickListener { appDeleteListener(appModel) }
            appMenuClose.setOnClickListener {
                appHideLayout.visibility = View.GONE
                appTitle.visibility = View.VISIBLE
            }
            appRenameClose.setOnClickListener {
                renameLayout.visibility = View.GONE
                appTitle.visibility = View.VISIBLE
            }
            appHide.setOnClickListener { appHideListener(appModel, bindingAdapterPosition) }
        }

        private fun getAppName(context: Context, appPackage: String, user: UserHandle): String {
            val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
            return try {
                val activityList = launcherApps.getActivityList(appPackage, user)
                if (activityList.isNotEmpty()) {
                    activityList.first().label.toString()
                } else {
                    val packageManager = context.packageManager
                    packageManager.getApplicationLabel(
                        packageManager.getApplicationInfo(appPackage, 0)
                    ).toString()
                }
            } catch (_: Exception) {
                ""
            }
        }
    }
}