package com.zbrowser.app

import android.os.Bundle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.savedstate.SavedStateHandle

/**
 * ViewModel for the browser that survives configuration changes.
 * Stores tab metadata (not WebViews, which are Activity-scoped) so that
 * tab state can be restored after rotation or process death.
 */
class BrowserViewModel(private val savedStateHandle: SavedStateHandle) : ViewModel() {

    companion object {
        private const val KEY_TAB_DATA = "tab_data"
        private const val KEY_ACTIVE_TAB_ID = "active_tab_id"
        private const val KEY_NEXT_TAB_ID = "next_tab_id"
        private const val KEY_DESKTOP_MODE = "desktop_mode"
    }

    /**
     * Serializable tab metadata for state preservation.
     */
    data class TabState(
        val id: Int,
        val title: String,
        val url: String,
        val isDesktopMode: Boolean
    )

    /** Save current tab states for restoration after config change or process death */
    fun saveTabStates(tabs: List<BrowserTab>, activeTabId: Int, nextTabId: Int) {
        val states = tabs.map { TabState(it.id, it.title, it.url, it.isDesktopMode) }
        savedStateHandle[KEY_TAB_DATA] = states
        savedStateHandle[KEY_ACTIVE_TAB_ID] = activeTabId
        savedStateHandle[KEY_NEXT_TAB_ID] = nextTabId
    }

    /** Restore tab states */
    fun getTabStates(): List<TabState>? {
        return savedStateHandle.get<List<TabState>>(KEY_TAB_DATA)
    }

    fun getActiveTabId(): Int {
        return savedStateHandle.get(KEY_ACTIVE_TAB_ID) ?: -1
    }

    fun getNextTabId(): Int {
        return savedStateHandle.get(KEY_NEXT_TAB_ID) ?: 1
    }

    /** Save global desktop mode preference */
    fun saveDesktopMode(isDesktop: Boolean) {
        savedStateHandle[KEY_DESKTOP_MODE] = isDesktop
    }

    fun getDesktopMode(): Boolean {
        return savedStateHandle.get(KEY_DESKTOP_MODE) ?: false
    }
}
