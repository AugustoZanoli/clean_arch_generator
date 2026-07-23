<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# Cag Changelog

## [Unreleased]

## [1.0.1]
### Changed
- Cache de templates embutidos para evitar leituras repetidas do disco
- Substituição de `isAncestor` custom por `VfsUtil.isAncestor()` do IntelliJ SDK
- Extração de `STANDARD_PACKAGES` para constante compartilhada no `ArchitectureResolver`
- Deduplicação da computação de `nameVars` no `PlanBuilder`
- Abertura de arquivos no editor movida para fora de `WriteCommandAction`
- Uso de `buildMap` (idiomatic Kotlin) em `PlanBuilder.nameVars`

### Added
- Templates `model.template` e `mapper.template` (antes usavam fallback genérico)

### Removed
- Comentários desnecessários em todos os arquivos fonte
- Código morto e imports redundantes

## [1.0.0]
### Added
- Geração automática de arquivos seguindo Clean Architecture
- Suporte a projetos multi-módulo com seleção de módulo
- Preview interativo antes da geração dos arquivos
- Templates customizáveis via `.cag/templates/`
- Resolução automática de packages e imports
- Variáveis de template (PascalCase, camelCase, snake_case, kebab-case, UPPER_SNAKE)
- Referências cruzadas entre slots via `{fqn.<id>}`, `{class.<id>}`, `{package.<id>}`
- Configuração centralizada em `.cag/config.json`
- Compatível com Kotlin 2 (K2)
