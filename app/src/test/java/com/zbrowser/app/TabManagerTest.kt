package com.zbrowser.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for TabManager.
 * Tests tab creation, switching, closing, and edge cases.
 * Uses mock WebView references (null) since we're testing logic, not WebView behavior.
 */
class TabManagerTest {

    private lateinit var tabManager: TabManager

    @Before
    fun setup() {
        tabManager = TabManager()
    }

    @Test
    fun addTab_returnsTab() {
        val tab = tabManager.addTab(webView = null, url = "https://example.com")
        assertNotNull(tab)
        assertEquals("https://example.com", tab!!.url)
    }

    @Test
    fun addTab_assignsIncrementingIds() {
        val tab1 = tabManager.addTab(webView = null, url = "https://a.com")
        val tab2 = tabManager.addTab(webView = null, url = "https://b.com")
        assertNotNull(tab1)
        assertNotNull(tab2)
        assertTrue(tab2!!.id > tab1!!.id)
    }

    @Test
    fun addTab_respectsMaxLimit() {
        // Fill up to max tabs
        for (i in 0 until TabManager.MAX_TABS) {
            tabManager.addTab(webView = null, url = "https://tab$i.com")
        }
        // Next tab should fail
        val overflow = tabManager.addTab(webView = null, url = "https://overflow.com")
        assertNull(overflow)
        assertEquals(TabManager.MAX_TABS, tabManager.tabs.size)
    }

    @Test
    fun switchToTab_returnsTab() {
        val tab = tabManager.addTab(webView = null, url = "https://example.com")
        val switched = tabManager.switchToTab(tab!!.id)
        assertNotNull(switched)
        assertEquals(tab.id, switched!!.id)
    }

    @Test
    fun switchToTab_updatesActiveTabId() {
        val tab = tabManager.addTab(webView = null, url = "https://example.com")
        tabManager.switchToTab(tab!!.id)
        assertEquals(tab.id, tabManager.activeTabId)
    }

    @Test
    fun switchToTab_returnsNullForInvalidId() {
        val result = tabManager.switchToTab(999)
        assertNull(result)
    }

    @Test
    fun closeTab_returnsNextTab() {
        val tab1 = tabManager.addTab(webView = null, url = "https://a.com")
        val tab2 = tabManager.addTab(webView = null, url = "https://b.com")
        tabManager.switchToTab(tab1!!.id)

        val next = tabManager.closeTab(tab1.id)
        assertNotNull(next)
        assertEquals(1, tabManager.tabs.size)
    }

    @Test
    fun closeLastTab_returnsNull() {
        val tab = tabManager.addTab(webView = null, url = "https://example.com")
        val next = tabManager.closeTab(tab!!.id)
        assertNull(next)
        assertEquals(0, tabManager.tabs.size)
    }

    @Test
    fun closeAllTabs_clearsEverything() {
        tabManager.addTab(webView = null, url = "https://a.com")
        tabManager.addTab(webView = null, url = "https://b.com")
        tabManager.closeAllTabs()
        assertEquals(0, tabManager.tabs.size)
        assertEquals(-1, tabManager.activeTabId)
    }

    @Test
    fun getActiveTab_returnsActiveTab() {
        val tab = tabManager.addTab(webView = null, url = "https://example.com")
        tabManager.switchToTab(tab!!.id)
        val active = tabManager.getActiveTab()
        assertNotNull(active)
        assertEquals(tab.id, active!!.id)
    }

    @Test
    fun indexOf_returnsCorrectIndex() {
        val tab1 = tabManager.addTab(webView = null, url = "https://a.com")
        tabManager.addTab(webView = null, url = "https://b.com")
        assertEquals(0, tabManager.indexOf(tab1!!))
    }
}
