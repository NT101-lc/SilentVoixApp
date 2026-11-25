package com.silentvoix.app.ui.admin

import com.silentvoix.app.domain.common.ApiError
import com.silentvoix.app.domain.common.ApiResult
import com.silentvoix.app.ui.fakes.FakeAdmin
import com.silentvoix.app.ui.fakes.MainDispatcherRule
import com.silentvoix.app.ui.fakes.feedbackItem
import com.silentvoix.app.ui.fakes.managedUser
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class AdminViewModelTest {

    @get:Rule
    val main = MainDispatcherRule()

    private val admin = FakeAdmin()
    private val viewModel = AdminViewModel(admin, "token")

    @Test
    fun refreshLoadsTheOpenInboxByDefault() {
        viewModel.refresh()

        val state = viewModel.state.value
        assertTrue(state.overview is Loadable.Loaded)
        assertEquals(2, (state.users as Loadable.Loaded).value.size)
        assertEquals(listOf(true), admin.feedbackRequests)
    }

    @Test
    fun aChangedAccountReplacesItsRow() = runTest {
        viewModel.refresh()

        viewModel.onUserAction(managedUser("u2"), UserAction.LOCK)

        val users = (viewModel.state.value.users as Loadable.Loaded).value
        assertEquals(listOf(false, true), users.map { it.locked })
        assertTrue(viewModel.state.value.busyIds.isEmpty())
        assertEquals(AdminEvent.UserUpdated(managedUser("u2", locked = true)), viewModel.events.first())
    }

    @Test
    fun aResolvedItemLeavesTheOpenInbox() {
        viewModel.refresh()

        viewModel.onToggleResolved(feedbackItem("f1"))

        assertTrue((viewModel.state.value.feedback as Loadable.Loaded).value.isEmpty())
    }

    @Test
    fun losingTheAdminRoleIsReported() = runTest {
        admin.overviewResult = ApiResult.Failed(ApiError.FORBIDDEN)

        viewModel.refresh()

        assertEquals(AdminEvent.Forbidden, viewModel.events.first())
        assertEquals(Loadable.Failed(ApiError.FORBIDDEN), viewModel.state.value.overview)
    }

    @Test
    fun switchingToAllFeedbackReloadsIt() {
        viewModel.refresh()

        viewModel.setFeedbackOpenOnly(false)

        assertEquals(listOf(true, false), admin.feedbackRequests)
    }
}
