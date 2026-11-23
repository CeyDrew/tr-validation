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
| `tr-validation-benchmarks` | JMH benchmarks (not published) |

## Performance

`validate(...)` scans the input once and returns cached result objects, so it does not allocate.
JMH, average time per call on formatted input (`"100 000 001 46"`, `"TR33 0006 1005 1978 6457 8413 26"`, ...):

| Validator | Time (ns/op) | Allocation (B/op) |
|---|---:|---:|
| `PostalCode.validate` | 8.6 ± 0.5 | 0 |
| `Vkn.validate` | 22.3 ± 2.6 | 0 |
| `LicensePlate.validate` | 33.5 ± 2.2 | 0 |
| `Ykn.validate` | 39.5 ± 5.8 | 0 |
| `CardNumber.validate` | 40.2 ± 3.6 | 0 |
| `Tckn.validate` (invalid input) | 40.3 ± 1.8 | 0 |
| `PhoneNumber.validate` | 45.7 ± 18.2 | 0 |
| `Tckn.validate` | 47.0 ± 8.7 | 0 |
| `Iban.validate` | 80.0 ± 4.4 | 0 |
| `Iban.validateTr` | 145.5 ± 23.3 | 0 |
| `Tckn.mask` (for contrast) | 40.9 ± 2.4 | 160 |

JDK 22.0.1, AMD Ryzen 5 7600X, Windows 11; 1 fork, 3×1 s warmup, 5×1 s measurement, `-prof gc`.
Reproduce:

```bash
./mvnw -pl tr-validation-benchmarks -am package -DskipTests
java -jar tr-validation-benchmarks/target/benchmarks.jar -prof gc
```

## Build

```bash
./mvnw verify
```

Requires JDK 17+.

## License

[Apache License 2.0](LICENSE)
