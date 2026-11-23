# tr-validation

Validation, masking and formatting for Turkey-specific identifiers: TCKN, YKN, VKN, IBAN, license plates,
phone numbers, card numbers and postal codes.

> Work in progress. See [PLAN.md](PLAN.md) for the roadmap.

## Modules

| Module | Purpose |
|---|---|
| `tr-validation-core` | Zero-dependency validators, maskers and formatters |
| `tr-validation-jakarta` | Jakarta Bean Validation constraints |
| `tr-validation-spring-boot-starter` | Spring Boot auto-configuration, i18n messages, Jackson masking |
| `tr-validation-bom` | Bill of materials for version alignment |
| `tr-validation-examples` | Example Spring Boot application (not published) |

## Build

```bash
./mvnw verify
```

Requires JDK 17+.

## License

[Apache License 2.0](LICENSE)
