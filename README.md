# Cag — Clean Architecture Generator

Um plugin para IntelliJ IDEA que gera os arquivos de uma nova feature seguindo a arquitetura Clean Architecture definida no arquivo `.cag/config.json` do projeto.

## Funcionalidades

- Gera arquivos de uma feature seguindo a arquitetura definida
- Suporte a projetos multi-módulo
- Preview interativo antes da geração
- Templates customizáveis por projeto
- Resolução automática de packages e imports

## Como usar

1. Instale o plugin do JetBrains Marketplace
2. Configure a arquitetura no arquivo `.cag/config.json` na raiz do projeto
3. Use o atalho `Ctrl+Alt+Shift+N` ou acesse via menu Tools
4. Digite o nome da feature e confirme os arquivos no preview

## Configuração

O arquivo `.cag/config.json` define a arquitetura do projeto:

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
