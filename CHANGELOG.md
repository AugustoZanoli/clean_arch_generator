<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# Cag Changelog

## [Unreleased]

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
