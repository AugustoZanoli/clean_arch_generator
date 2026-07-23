# CAG - Clean Architecture Generator

> Generate complete Clean Architecture features in seconds directly from IntelliJ IDEA.

CAG is an IntelliJ IDEA plugin that scaffolds files, packages and modules following your project's architecture conventions. Instead of manually creating dozens of files every time you start a new feature, CAG generates the entire structure automatically using configurable templates.

[![Marketplace](https://img.shields.io/jetbrains/plugin/v/33066)](https://plugins.jetbrains.com/plugin/33066)
[![Downloads](https://img.shields.io/jetbrains/plugin/d/33066)](https://plugins.jetbrains.com/plugin/33066)
![License](https://img.shields.io/github/license/AugustoZanoli/clean_arch_generator)
[![Build](https://github.com/AugustoZanoli/clean_arch_generator/actions/workflows/build.yml/badge.svg)](https://github.com/AugustoZanoli/clean_arch_generator/actions)

---

## ✨ Features

- 🚀 Generate complete Clean Architecture features
- 📦 Multi-module project support
- 👀 Interactive preview before generation
- 📝 Fully customizable templates
- 📂 Automatic package resolution
- 🔗 Cross-template references
- ⚙️ Project-specific configuration
- 💡 Native IntelliJ IDEA integration

---

## 🎥 How to use
```
Right Click
      ↓
Generate Feature
      ↓
Enter feature name
      ↓
Preview generated files
      ↓
Generate
```

---

## 📦 Installation

Install CAG directly from the JetBrains Marketplace.

> https://plugins.jetbrains.com/plugin/cag-clean-architecture-generator

---

## 🚀 Quick Start

Create a configuration file:

```
.cag/
└── config.json
```

Example:

```json
{
  "version": 2,
  "architectureRoot": "",
  "modules": [
    {
      "name": "Domain",
      "slots": [
        {
          "id": "model",
          "subfolder": "model",
          "fileName": "{Feature}",
          "extension": "java",
          "template": "model"
        }
      ]
    }
  ]
}
```

Then:

1. Open **Tools → CAG → Generate Feature**
2. Enter the feature name.
3. Review the preview.
4. Generate.

---

## 📦 Module Structure

The configuration uses a modular structure where each module (Domain, Application, Infrastructure, etc.) contains its slots:

```json
{
  "modules": [
    {
      "name": "Domain",
      "slots": [
        { "id": "model", "subfolder": "model", "fileName": "{Feature}", "template": "model" },
        { "id": "repository", "subfolder": "repository", "fileName": "{Feature}Repository", "template": "repository" }
      ]
    },
    {
      "name": "Infrastructure",
      "slots": [
        { "id": "controller", "subfolder": "controller", "fileName": "{Feature}Controller", "template": "controller" }
      ]
    }
  ]
}
```

The folder path is automatically constructed as `{module.name}/{slot.subfolder}`. For example, a slot with `module.name = "Domain"` and `subfolder = "model"` will generate files in `Domain/model/`.

---

## 📁 Project Structure

```
.cag
├── config.json
└── templates
    ├── entity.template
    ├── repository.template
    ├── usecase.template
    └── ...
```

---

## 📝 Template Variables

| Variable | Example |
|----------|---------|
| `{Feature}` | UserAccount |
| `{feature}` | userAccount |
| `{FEATURE}` | USER_ACCOUNT |
| `{feature_snake}` | user_account |
| `{feature-kebab}` | user-account |
| `{package}` | com.example.domain |
| `{class.<id>}` | User |
| `{package.<id>}` | com.example.domain.model |
| `{fqn.<id>}` | com.example.domain.model.User |

---

## 🎨 Templates

Templates are resolved in the following order:

1. `.cag/templates/<name>.template`
2. Built-in plugin templates
3. Minimal fallback template

This allows every project to have its own generation strategy.

---

## 🛣️ Roadmap

- [x] Multi-module support
- [x] Template engine
- [x] Interactive preview
- [x] Cross-template references
- [ ] Kotlin support
- [ ] Architecture presets
- [ ] Template editor
- [ ] Import / Export configurations
- [ ] Marketplace template packs

---

## 📋 Versions

### 1.1.0

Estrutura de configuração migrada para módulos hierárquicos e otimizações de performance.

**Novidades:**
- Estrutura de configuração modular (módulos + slots)
- UI de configuração reestruturada com dois painéis
- Migração automática de configs legados
- Templates `model.template` e `mapper.template`

**Alterado:**
- Campo `folder` substituído por `module.name` + `subfolder`
- Architecture root auto-detection usa nomes dos módulos
- Cache de templates embutidos
- Substituição de `isAncestor` custom por `VfsUtil.isAncestor()` do IntelliJ SDK
- Uso de `buildMap` (idiomatic Kotlin)

**Removido:**
- Campo `folder` do `SlotConfig`
- Comentários desnecessários e código morto

---

### 1.0.0

Versão inicial — geração automatizada de features seguindo Clean Architecture.

**Funcionalidades:**
- Geração automática de arquivos seguindo Clean Architecture
- Suporte a projetos multi-módulo com seleção de módulo
- Preview interativo antes da geração dos arquivos
- Templates customizáveis via `.cag/templates/`
- Resolução automática de packages e imports
- Referências cruzadas entre slots via `{fqn.<id>}`, `{class.<id>}`, `{package.<id>}`
- Configuração centralizada em `.cag/config.json`

---

## 📄 License

MIT
