package com.mobileshop.app.core.util

import com.billme.app.core.util.ImeiValidator
import org.junit.Test
import org.junit.Assert.*

class ImeiValidatorTest {

    @Test
    fun `validateImei returns true for valid IMEI`() {
        // Test with a valid IMEI using Luhn algorithm
        val validImei = "490154203237518"
        assertTrue(ImeiValidator.isValidImei(validImei))
    }

    @Test
    fun `validateImei returns false for invalid IMEI checksum`() {
        // Test with invalid checksum
        val invalidImei = "490154203237510"
        assertFalse(ImeiValidator.isValidImei(invalidImei))
    }

    @Test
    fun `validateImei returns false for wrong length`() {
        val shortImei = "49015420323751"
        assertFalse(ImeiValidator.isValidImei(shortImei))
        
        val longImei = "4901542032375180"
        assertFalse(ImeiValidator.isValidImei(longImei))
    }

    @Test
    fun `validateImei returns false for non-numeric IMEI`() {
        val alphaImei = "49015420323751A"
        assertFalse(ImeiValidator.isValidImei(alphaImei))
    }

    @Test
    fun `cleanImei removes separators`() {
        val formattedImei = "490154-203237-518"
        val cleanedImei = ImeiValidator.cleanImei(formattedImei)
        assertEquals("490154203237518", cleanedImei)
    }

    @Test
    fun `formatImei adds separators correctly`() {
        val imei = "490154203237518"
        val formattedImei = ImeiValidator.formatImei(imei)
        assertEquals("490154-203237-518", formattedImei)
    }

    @Test
    fun `extractImei finds IMEI in text`() {
        val text = "IMEI: 490154203237518 Serial: ABC123"
        val extractedImei = ImeiValidator.extractImei(text)
        assertEquals("490154203237518", extractedImei)
    }

    @Test
    fun `extractDualImei finds both IMEIs`() {
        val text = "IMEI1: 490154203237518 IMEI2: 490154203237526"
        val (imei1, imei2) = ImeiValidator.extractDualImei(text)
        assertEquals("490154203237518", imei1)
        assertEquals("490154203237526", imei2)
    }

    @Test
    fun `getValidationError returns correct error message`() {
        assertEquals("IMEI cannot be empty", ImeiValidator.getValidationError(""))
        assertEquals("IMEI must be exactly 15 digits", ImeiValidator.getValidationError("12345"))
        assertEquals("IMEI must contain only numbers", ImeiValidator.getValidationError("49015420323751A"))
        assertEquals("Invalid IMEI checksum", ImeiValidator.getValidationError("490154203237510"))
        assertNull(ImeiValidator.getValidationError("490154203237518"))
    }

    @Test
    fun `validateImei returns comprehensive result`() {
        val validResult = ImeiValidator.validateImei("490154203237518")
        assertTrue(validResult.isValid)
        assertEquals("490154203237518", validResult.cleanImei)
        assertNull(validResult.errorMessage)

        val invalidResult = ImeiValidator.validateImei("invalid")
        assertFalse(invalidResult.isValid)
        assertNull(invalidResult.cleanImei)
        assertNotNull(invalidResult.errorMessage)
    }
}