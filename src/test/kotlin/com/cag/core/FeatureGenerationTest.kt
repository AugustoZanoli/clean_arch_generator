package com.cag.core

import com.cag.config.CagConfigService
import com.intellij.testFramework.fixtures.BasePlatformTestCase

/**
 * Teste de integração (Platform Test Framework) do caminho que depende de VFS:
 * resolução da arquitetura, montagem do plano e geração real dos arquivos.
 */
class FeatureGenerationTest : BasePlatformTestCase() {

    fun testBuildsPlanWithResolvedPackagesAndImports() {
        val config = CagConfigService.defaultConfig()
        val arch = ArchitectureResolver(project).resolve(config)
            ?: error("Nenhum source root disponível no fixture de teste")

        val plan = PlanBuilder(project).build("Assinante", config, arch)

        assertEquals(6, plan.files.size)

        val dto = plan.files.first { it.slot.id == "dto" }
        assertEquals("AssinanteDto.java", dto.fileName)
        assertTrue(dto.packageName.endsWith("domain.models"))
        assertTrue(dto.content.contains("class AssinanteDto"))

        val impl = plan.files.first { it.slot.id == "useCaseImpl" }
        assertTrue(impl.content.contains("class AssinanteUseCaseImpl implements AssinanteUseCase"))
        // O import da interface deve apontar para o package resolvido do useCase.
        val useCasePkg = plan.files.first { it.slot.id == "useCase" }.packageName
        assertTrue(impl.content.contains("import $useCasePkg.AssinanteUseCase;"))
    }

    fun testGeneratesFilesOnDisk() {
        val config = CagConfigService.defaultConfig()
        val arch = ArchitectureResolver(project).resolve(config)
            ?: error("Nenhum source root disponível no fixture de teste")
        val plan = PlanBuilder(project).build("Assinante", config, arch)

        val created = FeatureGenerator(project).generate(plan)

        assertEquals(6, created.size)
        val implFile = created.first { it.name == "AssinanteUseCaseImpl.java" }
        val text = String(implFile.contentsToByteArray(), implFile.charset)
        assertTrue(text.contains("class AssinanteUseCaseImpl"))

        // Reexecutar re-resolvendo (como o diálogo faz): os arquivos agora já existem e são pulados.
        val arch2 = ArchitectureResolver(project).resolve(config)
            ?: error("Nenhum source root disponível no fixture de teste")
        val plan2 = PlanBuilder(project).build("Assinante", config, arch2)
        assertTrue("arquivos existentes deveriam ser detectados", plan2.files.all { it.alreadyExists })
        assertEquals(0, FeatureGenerator(project).generate(plan2).size)
    }
}
