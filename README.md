# Cag — Clean Architecture Generator

Plugin para IntelliJ IDEA que gera automaticamente os arquivos de uma nova feature seguindo os princípios de Clean Architecture, com base na configuração definida no projeto.

## Funcionalidades

- Geração automática de arquivos por camada (domain, data, presentation, etc.)
- Suporte a projetos multi-módulo com seleção de módulo de destino
- Preview interativo dos arquivos antes da geração
- Templates customizáveis por projeto
- Resolução automática de packages e imports
- Variáveis de template para diferentes convenções de nomenclatura

## Instalação

Instale diretamente do [JetBrains Marketplace](https://plugins.jetbrains.com/plugin/cag-clean-architecture-generator).

## Uso

1. Configure a arquitetura no arquivo `.cag/config.json` na raiz do projeto
2. Pressione `Ctrl+Alt+Shift+N` ou acesse via menu *Tools*
3. Digite o nome da feature
4. Revise os arquivos no preview e confirme a geração

## Configuração

O arquivo `.cag/config.json` define a arquitetura do projeto. Cada *slot* representa uma camada ou arquivo que será gerado:

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
      "template": "model",
      "enabledByDefault": true
    }
  ]
}
```

### Variáveis de template

| Variável | Descrição | Exemplo |
|----------|-----------|---------|
| `{Feature}` | Nome em PascalCase | `AssinantePlano` |
| `{feature}` | Nome em camelCase | `assinantePlano` |
| `{FEATURE}` | Nome em UPPER_SNAKE | `ASSINANTE_PLANO` |
| `{feature_snake}` | Nome em snake_case | `assinante_plano` |
| `{feature-kebab}` | Nome em kebab-case | `assinante-plano` |
| `{package}` | Package do arquivo | `com.empresa.domain.model` |
| `{fqn.<id>}` | FQN de outro slot | `com.empresa.domain.model.Assinante` |
| `{class.<id>}` | Nome de classe de outro slot | `Assinante` |
| `{package.<id>}` | Package de outro slot | `com.empresa.domain.model` |

## Templates

Templates customizados podem ser colocados em `.cag/templates/`. O plugin procura templates na seguinte ordem:

1. `.cag/templates/<nome>.template` no projeto
2. Templates embutidos no plugin
3. Fallback mínimo gerado em código

## Desenvolvimento

```bash
# Build
./gradlew build

# Testes
./gradlew test

# Executar IDE de teste
./gradlew runIde
```

## Licença

MIT
