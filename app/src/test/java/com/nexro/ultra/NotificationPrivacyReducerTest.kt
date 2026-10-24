package com.nexro.ultra

import org.junit.Assert.*
import org.junit.Test

class NotificationPrivacyReducerTest {
    private fun record(key: String = "k1", phase: PrivacyPhase = PrivacyPhase.NOTIFICATION_OBSERVED) =
        NotificationPrivacyRecord(key, "com.example", 100L, 1, 4, "Title", "Text", phase = phase)

    @Test fun postedEventIsTrackedWithoutClaimingPopupVisibility() {
        val result = NotificationPrivacyReducer.reduce(emptyMap(), NotificationPrivacyEvent.Posted(record()))
        assertEquals(PrivacyPhase.NOTIFICATION_OBSERVED, result["k1"]?.phase)
        assertEquals(PopupVisibility.UNKNOWN, result["k1"]?.popupVisibility)
    }

    @Test fun duplicatePostDoesNotDuplicateKey() {
        val first = NotificationPrivacyReducer.reduce(emptyMap(), NotificationPrivacyEvent.Posted(record()))
        val second = NotificationPrivacyReducer.reduce(first, NotificationPrivacyEvent.Posted(record()))
        assertEquals(1, second.size)
        assertEquals("Text", second["k1"]?.text)
    }

    @Test fun removalMarksRecordDismissed() {
        val first = NotificationPrivacyReducer.reduce(emptyMap(), NotificationPrivacyEvent.Posted(record()))
        val result = NotificationPrivacyReducer.reduce(first, NotificationPrivacyEvent.Removed("k1"))
        assertEquals(PrivacyPhase.DISMISSED, result["k1"]?.phase)
        assertTrue(result["k1"]?.removed == true)
    }

    @Test fun maskRequestIsUnsupportedWhenPopupVisibilityIsUnknown() {
        val first = NotificationPrivacyReducer.reduce(emptyMap(), NotificationPrivacyEvent.Posted(record()))
        val result = NotificationPrivacyReducer.reduce(first, NotificationPrivacyEvent.MaskRequested("k1"))
        assertEquals(PrivacyPhase.UNSUPPORTED, result["k1"]?.phase)
    }

    @Test fun notificationUpdatePreservesActiveMaskStateButRemovalWins() {
        val masked = record(phase = PrivacyPhase.MASK_ACTIVE).copy(popupVisibility = PopupVisibility.CONFIRMED_VISIBLE)
        val first = NotificationPrivacyReducer.reduce(emptyMap(), NotificationPrivacyEvent.Posted(masked))
        val updated = NotificationPrivacyReducer.reduce(first, NotificationPrivacyEvent.Posted(record().copy(text = "Updated")))
        assertEquals(PrivacyPhase.MASK_ACTIVE, updated["k1"]?.phase)
        val removed = NotificationPrivacyReducer.reduce(updated, NotificationPrivacyEvent.Removed("k1"))
        assertEquals(PrivacyPhase.DISMISSED, removed["k1"]?.phase)
    }

    @Test fun multipleNotificationsRemainIndependent() {
        val first = NotificationPrivacyReducer.reduce(emptyMap(), NotificationPrivacyEvent.Posted(record("a")))
        val second = NotificationPrivacyReducer.reduce(first, NotificationPrivacyEvent.Posted(record("b")))
        val third = NotificationPrivacyReducer.reduce(second, NotificationPrivacyEvent.Removed("a"))
        assertEquals(2, third.size)
        assertEquals(PrivacyPhase.DISMISSED, third["a"]?.phase)
        assertEquals(PrivacyPhase.NOTIFICATION_OBSERVED, third["b"]?.phase)
    }

    @Test fun unknownVisibilityNeverCountsAsExactMaskSupport() {
        val decision = PrivacyEligibilityEngine.evaluate(record())
        assertFalse(decision.exactMaskSupported)
        assertTrue(decision.reason.contains("bounds"))
    }
}
