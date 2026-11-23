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

## Bean Validation

```java
record CustomerRequest(
    @NotNull @Tckn(allowYkn = true) String identityNumber,
    @TrIban String iban,
    @TrPhone(mobileOnly = true) String gsm,
    @TrPlate String plate,
    @CardNumber(networks = {CardNetwork.VISA, CardNetwork.TROY}) String card) {}
```

Annotations are in `io.github.ceydrew.trvalidation.jakarta` (`tr-validation-jakarta`). `null` is valid,
so add `@NotNull` where a value is required. Default messages come in English and Turkish
(`must be a valid Turkish identity number` / `geçerli bir T.C. kimlik numarası olmalıdır`); override them
in your `ValidationMessages.properties` with keys such as
`io.github.ceydrew.trvalidation.jakarta.Tckn.message`. See [ADR 0002](docs/adr/0002-validation-message-bundles.md).

## Spring Boot

Add one dependency; nothing to configure:

```xml
<dependency>
  <groupId>io.github.ceydrew</groupId>
  <artifactId>tr-validation-spring-boot-starter</artifactId>
  <version>${tr-validation.version}</version>
</dependency>
```

- All constraints above, with messages in the request's `Accept-Language` (English or Turkish).
- Constraints on controller parameters work without `@Validated`:
  `@GetMapping ... find(@RequestParam @Tckn String tckn)`.
- Jackson masking (Jackson 3 on Spring Boot 4, Jackson 2 on Spring Boot 3):

```java
record CustomerResponse(
    @Masked(MaskType.TCKN) String identityNumber,  // "100******46"
    PhoneNumber gsm,                               // value type: "+90 532 *** ** 67"
    Iban iban) {}                                  // value type: "TR33 **** **** **** **** **13 26"
```

Core value types (`Tckn`, `Iban`, `PhoneNumber`, ...) are also read from JSON strings, so they can be
used directly in request DTOs. Set `tr-validation.masking.enabled=false` to write full values, e.g. in
an internal service.

### Personal data (KVKK)

TCKN, phone numbers, IBANs and plates are personal data under the Turkish data protection law (KVKK).
tr-validation is built so they do not leak by accident:

- `toString()` of every value type is masked, so logging a `Tckn` or `PhoneNumber` is safe;
- exception messages (`InvalidValueException`, JSON deserialization errors) never contain the rejected value;
- `@Masked` and the value types keep full numbers out of API responses and JSON logs.

Masking is a safety net, not a compliance programme: decide what you store, for how long and who can
read it.

The [example application](tr-validation-examples) is a small insurance customer registration API using
all of the above; its error responses list the invalid fields without echoing their values:

```bash
./mvnw -pl tr-validation-examples -am install -DskipTests
./mvnw -pl tr-validation-examples spring-boot:run
curl -X POST localhost:8080/api/customers -H "Content-Type: application/json" -H "Accept-Language: tr" \
  -d '{"fullName":"Ayşe Yılmaz","identityNumber":"10000000147","gsm":"05321234567","postalCode":"34710"}'
# {"status":400,"title":"Validation failed","errors":[{"field":"identityNumber","message":"geçerli bir T.C. kimlik numarası olmalıdır"}]}
```

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
