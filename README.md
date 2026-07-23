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
  "version": 1,
  "architectureRoot": "",
  "slots": [
    {
      "id": "model",
      "folder": "domain/model",
      "fileName": "{Feature}",
      "extension": "java",
      "template": "model"
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

## 📄 License

MIT
