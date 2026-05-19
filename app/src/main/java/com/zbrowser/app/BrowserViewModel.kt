package com.zbrowser.app

import androidx.lifecycle.ViewModel
import java.io.Serializable

/**
 * ViewModel for the browser that survives configuration changes.
 * Stores tab metadata (not WebViews, which are Activity-scoped) so that
 * tab state can be restored after rotation.
 *
 * Uses regular ViewModel properties instead of SavedStateHandle.
 * ViewModel survives configuration changes (rotation) which is the primary
 * use case. For process death, the app gracefully recreates with HOME_URL.
 */
class BrowserViewModel : ViewModel() {

    /**
     * Serializable tab metadata for state preservation.
     */
    data class TabState(
        val id: Int,
        val title: String,
        val url: String,
        val isDesktopMode: Boolean
    ) : Serializable {
        companion object {
            private const val serialVersionUID = 1L
        }
    }

    private var _tabStates: List<TabState>? = null
    private var _activeTabId: Int = -1
    private var _nextTabId: Int = 1
    private var _desktopMode: Boolean = false

    /** Save current tab states for restoration after config change */
    fun saveTabStates(tabs: List<BrowserTab>, activeTabId: Int, nextTabId: Int) {
        _tabStates = tabs.map { TabState(it.id, it.title, it.url, it.isDesktopMode) }
        _activeTabId = activeTabId
        _nextTabId = nextTabId
    }

    /** Restore tab states */
    fun getTabStates(): List<TabState>? = _tabStates

    fun getActiveTabId(): Int = _activeTabId

    fun getNextTabId(): Int = _nextTabId

    /** Save global desktop mode preference */
    fun saveDesktopMode(isDesktop: Boolean) { _desktopMode = isDesktop }

    fun getDesktopMode(): Boolean = _desktopMode
}
