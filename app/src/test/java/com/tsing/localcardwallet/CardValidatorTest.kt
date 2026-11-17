package com.tsing.localcardwallet

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * CardValidator 纯函数测试：不碰 Android 框架，可在 JVM 上直接跑。
 * 卡号取自各卡组织公开的测试号段，仅用于校验算法验证。
 */
class CardValidatorTest {

    @Test
    fun luhn_acceptsKnownTestNumbers() {
        assertTrue(CardValidator.luhn("4111111111111111")) // Visa
        assertTrue(CardValidator.luhn("5500005555555559")) // Mastercard
        assertTrue(CardValidator.luhn("378282246310005")) // Amex
        assertTrue(CardValidator.luhn("6011111111111117")) // Discover
        assertTrue(CardValidator.luhn("3530111333300000")) // JCB
    }

    @Test
    fun luhn_rejectsBadChecksumAndShortInput() {
        assertFalse(CardValidator.luhn("4111111111111112"))
        assertFalse(CardValidator.luhn("123456789012"))
        assertFalse(CardValidator.luhn("123"))
        assertFalse(CardValidator.luhn(""))
        assertFalse(CardValidator.luhn("4111-1111-1111-1111"))
    }

    @Test
    fun detectType_matchesNetworks() {
        assertEquals("VISA", CardValidator.detectType("4111111111111111"))
        assertEquals("MASTER CARD", CardValidator.detectType("5500005555555559"))
        assertEquals("MASTER CARD", CardValidator.detectType("2223000044445555"))
        assertEquals("AMERICAN EXPRESS", CardValidator.detectType("378282246310005"))
        assertEquals("JCB", CardValidator.detectType("3530111333300000"))
        assertEquals("DISCOVER", CardValidator.detectType("6011111111111117"))
        assertEquals("UNIONPAY", CardValidator.detectType("6222021234567890"))
        assertEquals("DINERS CLUB", CardValidator.detectType("30000000000004"))
        assertEquals("UNKNOWN", CardValidator.detectType("9999999999999999"))
        assertEquals("UNKNOWN", CardValidator.detectType(""))
    }

    @Test
    fun digits_stripsNonDigits() {
        assertEquals("4111111111111111", CardValidator.digits("4111 1111 1111 1111"))
        assertEquals("622202", CardValidator.digits("6222-02"))
        assertEquals("", CardValidator.digits("abcd"))
    }
}
