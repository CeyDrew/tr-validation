# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

First release.

### Added

#### `tr-validation-core` (no dependencies)

- Validators and value types in `io.github.ceydrew.trvalidation`: `Tckn`, `Ykn`, `Vkn`, `Iban`,
  `CardNumber`, `PhoneNumber`, `LicensePlate`, `PostalCode`. Each offers `isValid`, `validate`,
  `normalize`, `mask`, `of`, `parse` and (except `PhoneNumber`) `random` for test data; instances expose
  `value()` and `masked()` and print masked in `toString()` (ADR 0001).
- `Iban`: any SWIFT-registered country, plus `validateTr`/`isValidTr`, `format`, `bankCode` and
  `randomTr`.
- `CardNumber`: Luhn, network detection with `CardNetwork` (Visa, Mastercard, American Express, Troy).
- `PhoneNumber`: Turkish numbers in any common notation, E.164 normalization, `PhoneType`.
- `ValidationResult` (sealed `Valid` / `Invalid`), `InvalidReason`, and `InvalidValueException`, whose
  message never contains the rejected value.
- Allocation-free `validate(...)` (JMH results in the README).

#### `tr-validation-jakarta`

- Constraints in `io.github.ceydrew.trvalidation.jakarta`: `@Tckn(allowYkn)`, `@Ykn`, `@Vkn`,
  `@TrIban(allowForeign)`, `@TrPlate`, `@TrPhone(mobileOnly)`, `@CardNumber(networks)`,
  `@TrPostalCode`; `null` is valid.
- English and Turkish messages as Hibernate Validator contributor bundles (ADR 0002).

#### `tr-validation-spring-boot-starter`

- Auto-configuration for Spring Boot 3 and 4 (ADR 0003); `tr-validation.masking.enabled`.
- `@Masked` with `MaskType` for JSON masking; `TrValidationJacksonModule` (Jackson 3) and
  `TrValidationJackson2Module` (Jackson 2) also read and write the core value types.

#### `tr-validation-bom`

- Bill of materials for the modules above.
