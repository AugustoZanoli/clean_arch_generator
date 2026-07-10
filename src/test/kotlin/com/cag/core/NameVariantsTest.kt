package com.cag.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NameVariantsTest {

    @Test
    fun `derives variants from a simple lowercase word`() {
        val n = NameVariants("assinante")
        assertEquals("Assinante", n.pascal)
        assertEquals("assinante", n.camel)
        assertEquals("ASSINANTE", n.upperSnake)
        assertEquals("assinante", n.snake)
        assertEquals("assinante", n.kebab)
        assertTrue(n.isValid)
    }

    @Test
    fun `splits multi-word input regardless of separator`() {
        val expectedPascal = "AssinantePlano"
        assertEquals(expectedPascal, NameVariants("assinante plano").pascal)
        assertEquals(expectedPascal, NameVariants("assinante-plano").pascal)
        assertEquals(expectedPascal, NameVariants("assinante_plano").pascal)
        assertEquals(expectedPascal, NameVariants("AssinantePlano").pascal)
    }

    @Test
    fun `normalizes snake and kebab for multi-word input`() {
        val n = NameVariants("Assinante Plano")
        assertEquals("assinantePlano", n.camel)
        assertEquals("ASSINANTE_PLANO", n.upperSnake)
        assertEquals("assinante_plano", n.snake)
        assertEquals("assinante-plano", n.kebab)
    }

    @Test
    fun `blank or symbol-only input is invalid`() {
        assertFalse(NameVariants("   ").isValid)
        assertFalse(NameVariants("--__").isValid)
    }
}
