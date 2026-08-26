package me.xdan.prism.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class PackageNameGeneratorTest {
    @Test
    fun createsStablePackageFromHost() {
        assertEquals("me.xdan.prism.example", PackageNameGenerator.forUrl("https://example.com/path"))
    }

    @Test
    fun stripsWww() {
        assertEquals("me.xdan.prism.example", PackageNameGenerator.forUrl("https://www.example.com"))
    }

    @Test
    fun avoidsLeadingDigit() {
        val packageName = PackageNameGenerator.forUrl("https://123.example.com")
        assertEquals("me.xdan.prism.app123", packageName)
    }

    @Test
    fun createsUniqueSuffix() {
        val base = "me.xdan.prism.example"
        val result = PackageNameGenerator.ensureUnique(base) { it == base || it == "${base}2" }
        assertNotEquals(base, result)
        assertEquals("me.xdan.prism.example3", result)
    }
}
