package com.cag.core

import org.junit.Assert.assertEquals
import org.junit.Test

class TemplateEngineTest {

    @Test
    fun `substitutes known placeholders`() {
        val out = TemplateEngine.render(
            "package {package};\n\npublic class {Feature}Dto {}",
            mapOf("package" to "com.app.domain.models", "Feature" to "Assinante"),
        )
        assertEquals("package com.app.domain.models;\n\npublic class AssinanteDto {}", out)
    }

    @Test
    fun `supports dotted cross-reference keys`() {
        val out = TemplateEngine.render(
            "import {fqn.dto};\nimplements {class.useCase}",
            mapOf("fqn.dto" to "com.app.domain.models.AssinanteDto", "class.useCase" to "AssinanteUseCase"),
        )
        assertEquals("import com.app.domain.models.AssinanteDto;\nimplements AssinanteUseCase", out)
    }

    @Test
    fun `keeps unknown placeholders untouched`() {
        val out = TemplateEngine.render("hello {unknown} world", emptyMap())
        assertEquals("hello {unknown} world", out)
    }
}
