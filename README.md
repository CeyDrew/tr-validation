# tr-validation

[![CI](https://github.com/CeyDrew/tr-validation/actions/workflows/ci.yml/badge.svg)](https://github.com/CeyDrew/tr-validation/actions/workflows/ci.yml)
[![codecov](https://codecov.io/gh/CeyDrew/tr-validation/branch/main/graph/badge.svg)](https://codecov.io/gh/CeyDrew/tr-validation)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.ceydrew/tr-validation-core)](https://central.sonatype.com/artifact/io.github.ceydrew/tr-validation-core)
[![Java 17+](https://img.shields.io/badge/Java-17%2B-blue)](#compatibility)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue)](LICENSE)

Validation, masking and formatting for Turkey-specific identifiers: TCKN, YKN, VKN, IBAN, payment card
numbers, phone numbers, license plates and postal codes. Zero-dependency core, Jakarta Bean Validation
constraints with English and Turkish messages, and a Spring Boot starter that keeps personal data out of
JSON responses and logs.

```java
Tckn.isValid("10000000146");                     // true
Tckn.validate("10000000147");                    // Invalid[reason=CHECKSUM_MISMATCH]
PhoneNumber.normalize("(0532) 123 45 67");       // "+905321234567"
Iban.mask("TR330006100519786457841326");         // "TR33 **** **** **** **** **13 26"

record CustomerRequest(@NotNull @Tckn String tckn, @TrIban String iban) {}
record CustomerResponse(@Masked(MaskType.TCKN) String tckn, Iban iban) {}  // JSON: masked
```

> **Status:** not yet released to Maven Central. Until the first release, build it locally with
> `./mvnw install`.

## Contents

- [Installation](#installation)
- [Core API](#core-api)
- [Bean Validation](#bean-validation)
- [Spring Boot](#spring-boot)
- [Personal data (KVKK)](#personal-data-kvkk)
- [Compatibility](#compatibility)
- [Performance](#performance)
- [Design decisions](#design-decisions)
- [Building](#building)

## Installation

| Module | Use it for |
|---|---|
| `tr-validation-core` | Validators, value types, masking and formatting. No dependencies. |
| `tr-validation-jakarta` | `@Tckn`, `@TrIban`, ... constraints for Jakarta Bean Validation. |
| `tr-validation-spring-boot-starter` | Everything above plus Spring Boot auto-configuration and Jackson masking. |
| `tr-validation-bom` | Aligning versions when using several modules. |

Maven:

```xml
<dependency>
  <groupId>io.github.ceydrew</groupId>
  <artifactId>tr-validation-spring-boot-starter</artifactId>
  <version>0.1.0</version>
</dependency>
```

Gradle (Kotlin DSL):

```kotlin
implementation("io.github.ceydrew:tr-validation-spring-boot-starter:0.1.0")
```

With the BOM:

```xml
<dependencyManagement>
  <dependencies>
    <dependency>
      <groupId>io.github.ceydrew</groupId>
      <artifactId>tr-validation-bom</artifactId>
      <version>0.1.0</version>
      <type>pom</type>
      <scope>import</scope>
    </dependency>
  </dependencies>
</dependencyManagement>
```

```kotlin
implementation(platform("io.github.ceydrew:tr-validation-bom:0.1.0"))
implementation("io.github.ceydrew:tr-validation-core")
```

## Core API

Every identifier is a class in `io.github.ceydrew.trvalidation` with the same methods
([ADR 0001](docs/adr/0001-api-style.md)):

| Method | Returns | On invalid input |
|---|---|---|
| `isValid(String)` | `boolean` | `false` |
| `validate(String)` | `ValidationResult`: `Valid` or `Invalid(InvalidReason)` | `Invalid` with the reason |
| `normalize(String)` | value without separators | (does not validate) |
| `mask(String)` | masked value | fully masked |
| `of(String)` | value object | throws `InvalidValueException` (no input in the message) |
| `parse(String)` | `Optional` value object | `Optional.empty()` |
| `random()` | valid, fictional value for tests | |

Input is lenient: spaces, tabs, no-break spaces and hyphens are ignored; `null` is invalid, never an
exception. Value objects are immutable, compare by normalized value and print **masked** in `toString()`.

```java
ValidationResult result = Tckn.validate(input);
if (result instanceof ValidationResult.Invalid invalid) {
  switch (invalid.reason()) {
    case WRONG_LENGTH -> ...;        // EMPTY, INVALID_CHARACTER, WRONG_LENGTH, INVALID_PREFIX,
    case CHECKSUM_MISMATCH -> ...;   // CHECKSUM_MISMATCH, INVALID_COUNTRY_CODE,
    default -> ...;                  // INVALID_PROVINCE_CODE, INVALID_FORMAT
  }
}
```

### TCKN and YKN

T.C. kimlik numarası and yabancı kimlik numarası: 11 digits with two check digits. Numbers starting with
`99` are YKNs and are rejected by `Tckn`.

```java
Tckn.isValid("100 000 001 46");          // true
Tckn.validate("99000000042");            // Invalid[reason=INVALID_PREFIX]
Ykn.isValid("99000000042");              // true
Tckn tckn = Tckn.of("10000000146");
tckn.value();                            // "10000000146"
tckn.toString();                         // "100******46"
Tckn.random(new SplittableRandom(42));   // valid, fictional test data
```

### VKN

Vergi kimlik numarası: 10 digits, mod-10 checksum.

```java
Vkn.isValid("1234567890");               // true
Vkn.mask("1234567890");                  // "12******90"
```

### IBAN

Any country of the SWIFT IBAN registry (length and mod 97), plus Turkish structure checks.

```java
Iban.isValid("DE89 3704 0044 0532 0130 00");      // true
Iban.isValidTr("DE89 3704 0044 0532 0130 00");    // false, not Turkish
Iban.format("tr330006100519786457841326");        // "TR33 0006 1005 1978 6457 8413 26"
Iban.of("TR330006100519786457841326").bankCode(); // Optional["00061"]
Iban.randomTr();                                  // valid, fictional TR IBAN
```

### Card numbers

Luhn checksum, 12 to 19 digits, network detection (Visa, Mastercard, American Express, Troy).

```java
CardNumber.isValid("4111 1111 1111 1111");  // true
CardNumber.network("378282246310005");      // Optional[AMEX]
CardNumber.format("378282246310005");       // "3782 822463 10005"
CardNumber.mask("4111111111111111");        // "**** **** **** 1111"
```

### Phone numbers

Turkish mobile, fixed-line and 800/850/900 numbers in any common notation, normalized to E.164.

```java
PhoneNumber.normalize("+90 (532) 123-45-67"); // "+905321234567"
PhoneNumber.normalize("0090 212 123 4567");   // "+902121234567"
PhoneNumber.type("0532 123 45 67");           // Optional[MOBILE]
PhoneNumber.format("05321234567");            // "+90 532 123 45 67"
PhoneNumber.mask("05321234567");              // "+90 532 *** ** 67"
```

There is no `PhoneNumber.random()`: Türkiye has no fictional number range, so generated numbers could
belong to real people.

### License plates and postal codes

```java
LicensePlate.isValid("34abc123");            // true
LicensePlate.validate("82 A 1234");          // Invalid[reason=INVALID_PROVINCE_CODE]
LicensePlate.format("06ab1234");             // "06 AB 1234"
LicensePlate.mask("34 ABC 123");             // "34 *** *23"
PostalCode.of("06100").provinceCode();       // 6
```

## Bean Validation

```java
record CustomerRequest(
    @NotNull @Tckn(allowYkn = true) String identityNumber,
    @Vkn String taxNumber,
    @TrIban String iban,                     // @TrIban(allowForeign = true) for any country
    @TrPhone(mobileOnly = true) String gsm,
    @TrPlate String plate,
    @TrPostalCode String postalCode,
    @CardNumber(networks = {CardNetwork.VISA, CardNetwork.TROY}) String card,
    List<@TrPhone String> otherPhones) {}
```

Annotations are in `io.github.ceydrew.trvalidation.jakarta` and apply to any `CharSequence`. `null` is
valid, so combine with `@NotNull` or `@NotBlank` where a value is required.

Default messages:

| Constraint | English | Turkish |
|---|---|---|
| `@Tckn` | must be a valid Turkish identity number | geçerli bir T.C. kimlik numarası olmalıdır |
| `@Ykn` | must be a valid foreign identity number | geçerli bir yabancı kimlik numarası olmalıdır |
| `@Vkn` | must be a valid tax identification number | geçerli bir vergi kimlik numarası olmalıdır |
| `@TrIban` | must be a valid IBAN | geçerli bir IBAN olmalıdır |
| `@TrPlate` | must be a valid license plate | geçerli bir araç plakası olmalıdır |
| `@TrPhone` | must be a valid phone number | geçerli bir telefon numarası olmalıdır |
| `@CardNumber` | must be a valid card number | geçerli bir kart numarası olmalıdır |
| `@TrPostalCode` | must be a valid postal code | geçerli bir posta kodu olmalıdır |

Override any of them in your `ValidationMessages.properties`, e.g.
`io.github.ceydrew.trvalidation.jakarta.Tckn.message=TCKN hatalı`, or per field with `message = "..."`.
The bundles are Hibernate Validator contributor bundles, so they never clash with your own
([ADR 0002](docs/adr/0002-validation-message-bundles.md)).

## Spring Boot

Add `tr-validation-spring-boot-starter`; there is nothing to configure.

- All constraints, with messages in the request's `Accept-Language`.
- Constraints on controller parameters work without `@Validated`:

  ```java
  @GetMapping("/customers")
  CustomerResponse find(@RequestParam @Tckn String tckn) { ... }
  ```

- Jackson masking, on Jackson 3 (Spring Boot 4) and Jackson 2 (Spring Boot 3):

  ```java
  record CustomerResponse(
      @Masked(MaskType.TCKN) String identityNumber,  // "100******46"
      @Masked(MaskType.FULL) String motherName,      // "****"
      PhoneNumber gsm,                               // "+90 532 *** ** 67"
      Iban iban) {}                                  // "TR33 **** **** **** **** **13 26"
  ```

  `@Masked` works on `CharSequence` properties; `MaskType` has a constant per identifier plus `FULL`.
  Value types (`Tckn`, `Iban`, `PhoneNumber`, ...) are written masked and read from JSON strings, so they
  can be used in request DTOs. Invalid JSON values fail without echoing the input.

| Property | Default | |
|---|---|---|
| `tr-validation.masking.enabled` | `true` | `false` writes full values, e.g. in an internal service |

The [example application](tr-validation-examples) is a small insurance customer registration API using
all of the above:

```bash
./mvnw -pl tr-validation-examples -am install -DskipTests
./mvnw -pl tr-validation-examples spring-boot:run
curl -X POST localhost:8080/api/customers -H "Content-Type: application/json" -H "Accept-Language: tr" \
  -d '{"fullName":"Ayşe Yılmaz","identityNumber":"10000000147","gsm":"05321234567","postalCode":"34710"}'
# {"status":400,"title":"Validation failed","errors":[{"field":"identityNumber","message":"geçerli bir T.C. kimlik numarası olmalıdır"}]}
```

## Personal data (KVKK)

TCKN, phone numbers, IBANs, card numbers and plates are personal data under the Turkish Personal Data
Protection Law (KVKK, No. 6698). tr-validation is built so they do not leak by accident:

- `toString()` of every value type is masked, so logging a `Tckn` or `PhoneNumber` is safe;
- exception messages (`InvalidValueException`, JSON deserialization errors) never contain the rejected value;
- `@Masked` and the value types keep full numbers out of API responses and JSON logs;
- `mask(...)` of malformed input masks everything instead of guessing which part is safe to show;
- `random()` generates fictional test data, so fixtures need no real identity numbers.

Masking is a safety net, not a compliance programme: you still decide what you store, for how long and
who can read it. Algorithmic validity also does not mean a number is issued; verifying identities needs
the official services (e.g. NVİ KPS).

## Compatibility

| | Supported | Tested in CI |
|---|---|---|
| Java | 17+ | 17, 21 |
| Jakarta Bean Validation | 3.0, 3.1 | Hibernate Validator 8.0 and 9.1 |
| Spring Boot (starter) | 3.x, 4.x | 3.5.16 (Jackson 2), 4.2.0 (Jackson 3) |
| Jackson (starter) | 2.x, 3.x | 2.21, 3.1 |

The starter is compiled against Spring Boot 4 and uses only APIs that also exist in Spring Boot 3; the
[`tr-validation-boot3-tests`](tr-validation-boot3-tests) module runs it inside a Spring Boot 3.5 application
on every build ([ADR 0003](docs/adr/0003-spring-boot-3-and-4.md)). Spring Boot versions before 3.5 are
expected to work but are not tested.

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
Validators use character checks rather than regular expressions, so there is no ReDoS risk.

```bash
./mvnw -pl tr-validation-benchmarks -am package -DskipTests
java -jar tr-validation-benchmarks/target/benchmarks.jar -prof gc
```

## Design decisions

- [ADR 0001: Public API style of the core module](docs/adr/0001-api-style.md)
- [ADR 0002: Message bundles of the Jakarta module](docs/adr/0002-validation-message-bundles.md)
- [ADR 0003: Supporting Spring Boot 3 and 4 from one starter](docs/adr/0003-spring-boot-3-and-4.md)

## Building

```bash
./mvnw verify          # format check, tests, 100% coverage gate on core and jakarta
./mvnw spotless:apply  # format
```

Requires JDK 17+. Every validator has parameterized tests and jqwik property tests that compare it with
an independent reference implementation where one exists.

## License

[Apache License 2.0](LICENSE)
