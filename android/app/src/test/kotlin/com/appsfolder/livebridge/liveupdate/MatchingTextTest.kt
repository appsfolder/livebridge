package com.appsfolder.livebridge.liveupdate

import org.junit.Assert.assertEquals
import org.junit.Test

class MatchingTextTest {
    @Test fun dottedCapitalIFoldsToPlainIAndKeepsLength() {
        val source = "ŞİFRENİZ: 123456"
        val lowered = source.lowercaseForMatching()
        assertEquals("şifreniz: 123456", lowered)
        assertEquals(source.length, lowered.length)
    }
    @Test fun otherTextLowercasesWithoutLocaleRules() {
        assertEquals("tek kullanimlik kod", "TEK KULLANIMLIK KOD".lowercaseForMatching())
        assertEquals("verification code", "Verification Code".lowercaseForMatching())
    }
}
