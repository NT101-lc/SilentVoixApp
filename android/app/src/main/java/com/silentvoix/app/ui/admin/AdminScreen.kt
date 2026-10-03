package com.silentvoix.app.ui.admin

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.silentvoix.app.BuildConfig
import com.silentvoix.app.R
import com.silentvoix.app.data.admin.AdminOverview
import com.silentvoix.app.data.admin.FeedbackItem
import com.silentvoix.app.data.admin.FeedbackKind
import com.silentvoix.app.data.admin.ManagedUser
import com.silentvoix.app.data.admin.matching
import com.silentvoix.app.data.api.ApiError
import com.silentvoix.app.data.api.ApiResult
import com.silentvoix.app.data.api.SilentVoixApi
import com.silentvoix.app.data.auth.Session
import com.silentvoix.app.data.auth.UserRole
import com.silentvoix.app.data.backend.BackendStatus
import com.silentvoix.app.data.history.EntryDay
import com.silentvoix.app.data.history.entryDay
import com.silentvoix.app.ui.common.BackendStatusRow
import com.silentvoix.app.ui.common.ScreenHeader
import com.silentvoix.app.ui.common.SectionLabel
import com.silentvoix.app.ui.common.SegmentedControl
import com.silentvoix.app.ui.common.apiErrorMessage
import com.silentvoix.app.ui.common.apiErrorRes
import com.silentvoix.app.ui.common.pressBounce
import com.silentvoix.app.ui.common.rememberHapticTap
import com.silentvoix.app.ui.scene.skyWash
import com.silentvoix.app.ui.theme.EyebrowStyle
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

enum class AdminSection { OVERVIEW, USERS, FEEDBACK }

/** Something fetched from the server: still coming, failed, or here. */
sealed interface Loadable<out T> {
    data object Loading : Loadable<Nothing>

    data class Failed(val error: ApiError) : Loadable<Nothing>

    data class Loaded<T>(val value: T) : Loadable<T>
}

/** Account actions an admin can take from a user's menu. */
enum class UserAction { MAKE_ADMIN, REMOVE_ADMIN, LOCK, UNLOCK }

private fun <T> ApiResult<T>.toLoadable(): Loadable<T> = when (this) {
    is ApiResult.Ok -> Loadable.Loaded(value)
    is ApiResult.Failed -> Loadable.Failed(error)
}

/**
 * The admin tab. Everything comes from the server, which re-checks the role on every call: a 401
 * means the session is gone ([onUnauthorized]); a 403 means this person is no longer an admin
 * ([onForbidden], which refreshes the account so the tab disappears).
 */
@Composable
fun AdminScreen(
    api: SilentVoixApi,
    session: Session,
    backendStatus: BackendStatus,
    onRetryBackend: () -> Unit,
    onUnauthorized: () -> Unit,
    onForbidden: () -> Unit,
    showMessage: (String) -> Unit,
    hapticsEnabled: Boolean,
    contentPadding: PaddingValues,
) {
    var section by rememberSaveable { mutableStateOf(AdminSection.OVERVIEW) }
    var query by rememberSaveable { mutableStateOf("") }
    var openOnly by rememberSaveable { mutableStateOf(true) }
    var overview by remember { mutableStateOf<Loadable<AdminOverview>>(Loadable.Loading) }
    var users by remember { mutableStateOf<Loadable<List<ManagedUser>>>(Loadable.Loading) }
    var feedback by remember { mutableStateOf<Loadable<List<FeedbackItem>>>(Loadable.Loading) }
    var busy by remember { mutableStateOf(emptySet<String>()) }
    var reload by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()
    val token = session.token

    // Any failure that means "not allowed any more" is handled once, here.
    fun <T> ApiResult<T>.checked(): ApiResult<T> {
        if (this is ApiResult.Failed) {
            when (error) {
                ApiError.UNAUTHORIZED -> onUnauthorized()
                ApiError.FORBIDDEN -> onForbidden()
                else -> Unit
            }
        }
        return this
    }

    LaunchedEffect(reload) {
        val o = async { api.overview(token).checked().toLoadable() }
        val u = async { api.users(token).checked().toLoadable() }
        overview = o.await()
        users = u.await()
        onRetryBackend()
    }
    LaunchedEffect(reload, openOnly) {
        feedback = Loadable.Loading
        feedback = api.feedback(token, openOnly).checked().toLoadable()
    }

    val context = LocalContext.current
    AdminContent(
        section = section,
        onSectionChange = { section = it },
        overview = overview,
        users = users,
        feedback = feedback,
        query = query,
        onQueryChange = { query = it },
        feedbackOpenOnly = openOnly,
        onFeedbackOpenOnlyChange = { openOnly = it },
        currentUserId = session.account.id,
        busyIds = busy,
        backendStatus = backendStatus,
        onRetryBackend = onRetryBackend,
        onRefresh = { reload++ },
        onUserAction = { user, action ->
            busy = busy + user.id
            scope.launch {
                val result = when (action) {
                    UserAction.MAKE_ADMIN -> api.updateUser(token, user.id, role = UserRole.ADMIN)
                    UserAction.REMOVE_ADMIN -> api.updateUser(token, user.id, role = UserRole.USER)
                    UserAction.LOCK -> api.updateUser(token, user.id, locked = true)
                    UserAction.UNLOCK -> api.updateUser(token, user.id, locked = false)
                }.checked()
                busy = busy - user.id
                when (result) {
                    is ApiResult.Ok -> {
                        (users as? Loadable.Loaded)?.let { loaded ->
                            users = Loadable.Loaded(loaded.value.map { if (it.id == user.id) result.value else it })
                        }
                        showMessage(
                            context.getString(R.string.admin_user_updated, result.value.displayName ?: result.value.email.orEmpty()),
                        )
                        overview = api.overview(token).checked().toLoadable()
                    }
                    is ApiResult.Failed -> showMessage(context.getString(apiErrorRes(result.error)))
                }
            }
        },
        onToggleResolved = { item ->
            busy = busy + item.id
            scope.launch {
                val result = api.setResolved(token, item.id, !item.isResolved).checked()
                busy = busy - item.id
                when (result) {
                    is ApiResult.Ok -> {
                        (feedback as? Loadable.Loaded)?.let { loaded ->
                            feedback = Loadable.Loaded(
                                loaded.value
                                    .map { if (it.id == item.id) result.value else it }
                                    // In the open inbox a handled item leaves at once.
                                    .filter { !openOnly || !it.isResolved },
                            )
                        }
                        overview = api.overview(token).checked().toLoadable()
                    }
                    is ApiResult.Failed -> showMessage(context.getString(apiErrorRes(result.error)))
                }
            }
        },
        hapticsEnabled = hapticsEnabled,
        nowMillis = System.currentTimeMillis(),
        zone = ZoneId.systemDefault(),
        contentPadding = contentPadding,
    )
}

private val ContentMaxWidth = 720.dp
private val TimeFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
private val DateFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

@Composable
fun AdminContent(
    section: AdminSection,
    onSectionChange: (AdminSection) -> Unit,
    overview: Loadable<AdminOverview>,
    users: Loadable<List<ManagedUser>>,
    feedback: Loadable<List<FeedbackItem>>,
    query: String,
    onQueryChange: (String) -> Unit,
    feedbackOpenOnly: Boolean,
    onFeedbackOpenOnlyChange: (Boolean) -> Unit,
    currentUserId: String,
    busyIds: Set<String>,
    backendStatus: BackendStatus,
    onRetryBackend: () -> Unit,
    onRefresh: () -> Unit,
    onUserAction: (ManagedUser, UserAction) -> Unit,
    onToggleResolved: (FeedbackItem) -> Unit,
    nowMillis: Long,
    zone: ZoneId,
    contentPadding: PaddingValues,
    hapticsEnabled: Boolean = true,
) {
    val tap = rememberHapticTap(hapticsEnabled)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .skyWash()
            .padding(contentPadding)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = ContentMaxWidth)
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            ScreenHeader(
                title = stringResource(R.string.title_admin),
                supporting = stringResource(R.string.admin_supporting),
                action = {
                    IconButton(onClick = {
                        tap()
                        onRefresh()
                    }) {
                        Icon(Icons.Filled.Refresh, contentDescription = stringResource(R.string.admin_refresh))
                    }
                },
            )
            SegmentedControl(
                options = AdminSection.entries,
                selected = section,
                label = {
                    stringResource(
                        when (it) {
                            AdminSection.OVERVIEW -> R.string.admin_tab_overview
                            AdminSection.USERS -> R.string.admin_tab_users
                            AdminSection.FEEDBACK -> R.string.admin_tab_feedback
                        },
                    )
                },
                onSelect = {
                    tap()
                    onSectionChange(it)
                },
                modifier = Modifier.fillMaxWidth(),
            )
            AnimatedContent(
                targetState = section,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "adminSection",
            ) { target ->
                when (target) {
                    AdminSection.OVERVIEW -> OverviewSection(
                        overview = overview,
                        backendStatus = backendStatus,
                        onRetryBackend = onRetryBackend,
                        onRetry = onRefresh,
                        onOpenFeedback = { onSectionChange(AdminSection.FEEDBACK) },
                        onOpenUsers = { onSectionChange(AdminSection.USERS) },
                    )
                    AdminSection.USERS -> UsersSection(
                        users = users,
                        query = query,
                        onQueryChange = onQueryChange,
                        currentUserId = currentUserId,
                        busyIds = busyIds,
                        onAction = onUserAction,
                        onRetry = onRefresh,
                        nowMillis = nowMillis,
                        zone = zone,
                    )
                    AdminSection.FEEDBACK -> FeedbackSection(
                        feedback = feedback,
                        openOnly = feedbackOpenOnly,
                        onOpenOnlyChange = onFeedbackOpenOnlyChange,
                        busyIds = busyIds,
                        onToggleResolved = onToggleResolved,
                        onRetry = onRefresh,
                        nowMillis = nowMillis,
                        zone = zone,
                    )
                }
            }
        }
    }
}

// ---- overview -------------------------------------------------------------------------------

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun OverviewSection(
    overview: Loadable<AdminOverview>,
    backendStatus: BackendStatus,
    onRetryBackend: () -> Unit,
    onRetry: () -> Unit,
    onOpenFeedback: () -> Unit,
    onOpenUsers: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        LoadableBox(overview, onRetry) { counts ->
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionLabel(stringResource(R.string.admin_section_people), Modifier.padding(start = 4.dp))
                FlowRow(
                    maxItemsInEachRow = 2,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    val tile = Modifier.weight(1f)
                    StatTile(counts.users, stringResource(R.string.admin_stat_users), tile, onClick = onOpenUsers)
                    StatTile(counts.activeUsersThisWeek, stringResource(R.string.admin_stat_active), tile, onClick = onOpenUsers)
                    StatTile(counts.newUsersThisWeek, stringResource(R.string.admin_stat_new), tile, onClick = onOpenUsers)
                    StatTile(counts.admins, stringResource(R.string.admin_stat_admins), tile, onClick = onOpenUsers)
                    StatTile(
                        counts.openFeedback,
                        stringResource(R.string.admin_stat_open_feedback),
                        tile,
                        highlight = counts.openFeedback > 0,
                        onClick = onOpenFeedback,
                    )
                    StatTile(counts.lockedUsers, stringResource(R.string.admin_stat_locked), tile, onClick = onOpenUsers)
                }
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SectionLabel(stringResource(R.string.admin_section_system), Modifier.padding(start = 4.dp))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surfaceContainerLow,
            ) {
                Column {
                    BackendStatusRow(status = backendStatus, onRetry = onRetryBackend)
                    HorizontalDivider(Modifier.padding(horizontal = 18.dp), color = MaterialTheme.colorScheme.outlineVariant)
                    InfoLine(stringResource(R.string.settings_about_backend), BuildConfig.BACKEND_BASE_URL)
                    HorizontalDivider(Modifier.padding(horizontal = 18.dp), color = MaterialTheme.colorScheme.outlineVariant)
                    InfoLine(stringResource(R.string.settings_about_version), BuildConfig.VERSION_NAME)
                }
            }
        }
    }
}

@Composable
private fun StatTile(value: Int, label: String, modifier: Modifier, highlight: Boolean = false, onClick: () -> Unit) {
    val press = remember { MutableInteractionSource() }
    Surface(
        onClick = onClick,
        interactionSource = press,
        modifier = modifier
            .pressBounce(press)
            .heightIn(min = 104.dp),
        shape = MaterialTheme.shapes.large,
        color = if (highlight) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.surfaceContainerLow,
        contentColor = if (highlight) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onSurface,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(value.toString(), style = MaterialTheme.typography.displaySmall, maxLines = 1)
            Text(
                label,
                style = MaterialTheme.typography.labelLarge,
                color = if (highlight) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun InfoLine(label: String, value: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 14.dp)
            .semantics(mergeDescendants = true) {},
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(label.uppercase(), style = EyebrowStyle, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}

// ---- users ----------------------------------------------------------------------------------

@Composable
private fun UsersSection(
    users: Loadable<List<ManagedUser>>,
    query: String,
    onQueryChange: (String) -> Unit,
    currentUserId: String,
    busyIds: Set<String>,
    onAction: (ManagedUser, UserAction) -> Unit,
    onRetry: () -> Unit,
    nowMillis: Long,
    zone: ZoneId,
) {
    var confirming by remember { mutableStateOf<Pair<ManagedUser, UserAction>?>(null) }
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = { Text(stringResource(R.string.admin_search_users)) },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            singleLine = true,
            shape = CircleShape,
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                unfocusedBorderColor = Color.Transparent,
            ),
            modifier = Modifier.fillMaxWidth(),
        )
        LoadableBox(users, onRetry) { all ->
            val shown = all.matching(query)
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    stringResource(R.string.admin_users_count, shown.size),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .padding(start = 4.dp)
                        .semantics { liveRegion = LiveRegionMode.Polite },
                )
                if (shown.isEmpty()) {
                    Text(
                        stringResource(R.string.admin_users_empty),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(4.dp),
                    )
                }
                shown.forEach { user ->
                    UserCard(
                        user = user,
                        isYou = user.id == currentUserId,
                        busy = user.id in busyIds,
                        lastSeen = lastSeenLabel(user.lastSeenAtMillis, nowMillis, zone),
                        onAction = { action ->
                            // Granting power and locking someone out are asked twice; undoing them is not.
                            if (action == UserAction.LOCK || action == UserAction.MAKE_ADMIN) {
                                confirming = user to action
                            } else {
                                onAction(user, action)
                            }
                        },
                    )
                }
            }
        }
    }
    confirming?.let { (user, action) ->
        val who = user.displayName ?: user.email.orEmpty()
        val locking = action == UserAction.LOCK
        AlertDialog(
            onDismissRequest = { confirming = null },
            title = {
                Text(stringResource(if (locking) R.string.admin_lock_confirm_title else R.string.admin_role_confirm_title, who))
            },
            text = { Text(stringResource(if (locking) R.string.admin_lock_confirm_text else R.string.admin_role_confirm_text)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirming = null
                        onAction(user, action)
                    },
                    colors = if (locking) {
                        ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    } else {
                        ButtonDefaults.textButtonColors()
                    },
                ) {
                    Text(stringResource(if (locking) R.string.admin_lock_confirm else R.string.admin_role_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirming = null }) { Text(stringResource(R.string.common_cancel)) }
            },
        )
    }
}

@Composable
private fun lastSeenLabel(lastSeenAtMillis: Long?, nowMillis: Long, zone: ZoneId): String {
    if (lastSeenAtMillis == null) return stringResource(R.string.admin_user_never_seen)
    val at = Instant.ofEpochMilli(lastSeenAtMillis).atZone(zone)
    val whenText = when (val day = entryDay(lastSeenAtMillis, nowMillis, zone)) {
        EntryDay.Today -> stringResource(R.string.time_today_at, at.format(TimeFormat))
        EntryDay.Yesterday -> stringResource(R.string.time_yesterday_at, at.format(TimeFormat))
        is EntryDay.Earlier -> day.date.format(DateFormat)
    }
    return stringResource(R.string.admin_user_last_seen, whenText)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun UserCard(
    user: ManagedUser,
    isYou: Boolean,
    busy: Boolean,
    lastSeen: String,
    onAction: (UserAction) -> Unit,
) {
    val name = user.displayName ?: user.email?.substringBefore('@') ?: "?"
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, end = 4.dp, top = 14.dp, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Avatar(name = name, role = user.role, locked = user.locked)
            Spacer(Modifier.width(14.dp))
            Column(
                modifier = Modifier
                    .weight(1f)
                    .semantics(mergeDescendants = true) {},
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    user.email ?: stringResource(R.string.admin_user_no_email),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(top = 4.dp),
                ) {
                    RolePill(user.role)
                    if (isYou) Pill(stringResource(R.string.admin_user_you), MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer)
                    if (user.locked) Pill(stringResource(R.string.admin_user_locked), MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.onErrorContainer)
                }
                Text(
                    lastSeen,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            when {
                busy -> CircularProgressIndicator(Modifier.padding(14.dp).size(20.dp), strokeWidth = 2.5.dp)
                // Your own account is not editable here: the server refuses it too.
                isYou -> Spacer(Modifier.size(48.dp))
                else -> UserMenu(user = user, name = name, onAction = onAction)
            }
        }
    }
}

@Composable
private fun UserMenu(user: ManagedUser, name: String, onAction: (UserAction) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { open = true }) {
            Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.admin_user_actions, name))
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            val roleAction = if (user.role == UserRole.ADMIN) UserAction.REMOVE_ADMIN else UserAction.MAKE_ADMIN
            val lockAction = if (user.locked) UserAction.UNLOCK else UserAction.LOCK
            listOf(roleAction, lockAction).forEach { action ->
                DropdownMenuItem(
                    text = {
                        Text(
                            stringResource(
                                when (action) {
                                    UserAction.MAKE_ADMIN -> R.string.admin_action_make_admin
                                    UserAction.REMOVE_ADMIN -> R.string.admin_action_remove_admin
                                    UserAction.LOCK -> R.string.admin_action_lock
                                    UserAction.UNLOCK -> R.string.admin_action_unlock
                                },
                            ),
                            color = if (action == UserAction.LOCK) MaterialTheme.colorScheme.error else Color.Unspecified,
                        )
                    },
                    onClick = {
                        open = false
                        onAction(action)
                    },
                )
            }
        }
    }
}

@Composable
private fun Avatar(name: String, role: UserRole, locked: Boolean) {
    val container = when {
        locked -> MaterialTheme.colorScheme.surfaceContainerHighest
        role == UserRole.ADMIN -> MaterialTheme.colorScheme.tertiaryContainer
        else -> MaterialTheme.colorScheme.secondaryContainer
    }
    val content = when {
        locked -> MaterialTheme.colorScheme.onSurfaceVariant
        role == UserRole.ADMIN -> MaterialTheme.colorScheme.onTertiaryContainer
        else -> MaterialTheme.colorScheme.onSecondaryContainer
    }
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(container),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            name.trim().take(1).uppercase().ifEmpty { "?" },
            style = MaterialTheme.typography.titleLarge,
            color = content,
        )
    }
}

@Composable
fun RolePill(role: UserRole) {
    if (role == UserRole.ADMIN) {
        Pill(
            stringResource(R.string.role_admin),
            MaterialTheme.colorScheme.tertiaryContainer,
            MaterialTheme.colorScheme.onTertiaryContainer,
            icon = ImageVector.vectorResource(R.drawable.ic_admin),
        )
    } else {
        Pill(stringResource(R.string.role_user), MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer)
    }
}

@Composable
private fun Pill(text: String, container: Color, content: Color, icon: ImageVector? = null) {
    Surface(shape = CircleShape, color = container, contentColor = content) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            if (icon != null) Icon(icon, contentDescription = null, modifier = Modifier.size(14.dp))
            Text(text, style = MaterialTheme.typography.labelMedium)
        }
    }
}

// ---- feedback -------------------------------------------------------------------------------

@Composable
private fun FeedbackSection(
    feedback: Loadable<List<FeedbackItem>>,
    openOnly: Boolean,
    onOpenOnlyChange: (Boolean) -> Unit,
    busyIds: Set<String>,
    onToggleResolved: (FeedbackItem) -> Unit,
    onRetry: () -> Unit,
    nowMillis: Long,
    zone: ZoneId,
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        SegmentedControl(
            options = listOf(true, false),
            selected = openOnly,
            label = { stringResource(if (it) R.string.admin_feedback_open else R.string.admin_feedback_all) },
            onSelect = onOpenOnlyChange,
            modifier = Modifier.fillMaxWidth(),
        )
        LoadableBox(feedback, onRetry) { items ->
            if (items.isEmpty()) {
                EmptyInbox()
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items.forEach { item ->
                        FeedbackCard(
                            item = item,
                            busy = item.id in busyIds,
                            time = sentLabel(item.createdAtMillis, nowMillis, zone),
                            onToggleResolved = { onToggleResolved(item) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun sentLabel(millis: Long, nowMillis: Long, zone: ZoneId): String {
    val at = Instant.ofEpochMilli(millis).atZone(zone)
    return when (val day = entryDay(millis, nowMillis, zone)) {
        EntryDay.Today -> stringResource(R.string.time_today_at, at.format(TimeFormat))
        EntryDay.Yesterday -> stringResource(R.string.time_yesterday_at, at.format(TimeFormat))
        is EntryDay.Earlier -> day.date.format(DateFormat)
    }
}

@Composable
private fun FeedbackCard(item: FeedbackItem, busy: Boolean, time: String, onToggleResolved: () -> Unit) {
    val (kindLabel, container, content) = when (item.kind) {
        FeedbackKind.WRONG_RESULT -> Triple(
            stringResource(R.string.feedback_kind_wrong_result),
            MaterialTheme.colorScheme.secondaryContainer,
            MaterialTheme.colorScheme.onSecondaryContainer,
        )
        FeedbackKind.BUG -> Triple(
            stringResource(R.string.feedback_kind_bug),
            MaterialTheme.colorScheme.errorContainer,
            MaterialTheme.colorScheme.onErrorContainer,
        )
        FeedbackKind.IDEA -> Triple(
            stringResource(R.string.feedback_kind_idea),
            MaterialTheme.colorScheme.tertiaryContainer,
            MaterialTheme.colorScheme.onTertiaryContainer,
        )
        null -> Triple(
            stringResource(R.string.admin_feedback_kind_other),
            MaterialTheme.colorScheme.surfaceContainerHighest,
            MaterialTheme.colorScheme.onSurface,
        )
    }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Pill(kindLabel, container, content)
                if (item.isResolved) {
                    Pill(
                        stringResource(R.string.admin_feedback_resolved),
                        MaterialTheme.colorScheme.primaryContainer,
                        MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
                Spacer(Modifier.weight(1f))
                Text(time, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(item.message, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = item.authorName?.let { name -> item.authorEmail?.let { "$name · $it" } ?: name }
                    ?: item.authorEmail
                    ?: stringResource(R.string.admin_feedback_anonymous),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                if (busy) {
                    CircularProgressIndicator(Modifier.padding(12.dp).size(20.dp), strokeWidth = 2.5.dp)
                } else if (item.isResolved) {
                    OutlinedButton(onClick = onToggleResolved, modifier = Modifier.heightIn(min = 44.dp)) {
                        Text(stringResource(R.string.admin_feedback_reopen))
                    }
                } else {
                    FilledTonalButton(
                        onClick = onToggleResolved,
                        modifier = Modifier.heightIn(min = 44.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        ),
                    ) {
                        Text(stringResource(R.string.admin_feedback_resolve))
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyInbox() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 36.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            stringResource(R.string.admin_feedback_empty_title),
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.semantics { heading() },
        )
        Text(
            stringResource(R.string.admin_feedback_empty_text),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// ---- shared ---------------------------------------------------------------------------------

@Composable
private fun <T> LoadableBox(state: Loadable<T>, onRetry: () -> Unit, content: @Composable (T) -> Unit) {
    when (state) {
        Loadable.Loading -> Box(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 40.dp),
            contentAlignment = Alignment.Center,
        ) { CircularProgressIndicator() }
        is Loadable.Failed -> Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
        ) {
            Column(
                modifier = Modifier
                    .padding(18.dp)
                    .semantics { liveRegion = LiveRegionMode.Polite },
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(stringResource(R.string.admin_load_failed), style = MaterialTheme.typography.titleMedium)
                Text(apiErrorMessage(state.error), style = MaterialTheme.typography.bodyMedium)
                TextButton(onClick = onRetry, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onErrorContainer)) {
                    Text(stringResource(R.string.action_retry))
                }
            }
        }
        is Loadable.Loaded -> content(state.value)
    }
}
