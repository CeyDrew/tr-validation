# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added

- Core API (`io.github.ceydrew.trvalidation`): `Tckn`, `Ykn`, `Vkn` with `isValid`, `validate`,
  `normalize`, `mask`, `of`, `parse`, `random`, `value`, `masked`; masked `toString()`.
- `ValidationResult` (sealed: `Valid` / `Invalid`), `InvalidReason`, `InvalidValueException`
  (message never contains the rejected value).
- ADR 0001 on the public API style.
- `Iban` (any SWIFT-registered country, plus `validateTr`/`isValidTr`, `format`, `bankCode`,
  `randomTr`), `CardNumber` with `CardNetwork` (Visa, Mastercard, Amex, Troy), `PhoneNumber` with
  `PhoneType` (E.164 normalization), `LicensePlate`, `PostalCode`.
- `InvalidReason` constants `INVALID_COUNTRY_CODE`, `INVALID_PROVINCE_CODE`, `INVALID_FORMAT`.
- `tr-validation-benchmarks`: JMH benchmarks (not published).
- Jakarta module (`io.github.ceydrew.trvalidation.jakarta`): `@Tckn(allowYkn)`, `@Ykn`, `@Vkn`,
  `@TrIban(allowForeign)`, `@TrPlate`, `@TrPhone(mobileOnly)`, `@CardNumber(networks)`,
  `@TrPostalCode`; `null` is valid. English and Turkish messages in `ContributorValidationMessages`
  bundles (ADR 0002).
- Spring Boot starter: `TrValidationAutoConfiguration`, `TrValidationProperties`
  (`tr-validation.masking.enabled`), `@Masked` with `MaskType`, `TrValidationJacksonModule` (Jackson 3)
  and `TrValidationJackson2Module` (Jackson 2) that mask annotated properties and read/write core
  value types.
- `tr-validation-examples`: insurance customer registration API (not published).
- `tr-validation-boot3-tests`: runs the starter on Spring Boot 3.5 with Jackson 2 (not published).
- GitHub Actions CI (Java 17 and 21, Spotless, coverage, strict Javadoc), Codecov upload, Dependabot.
- README: installation (Maven, Gradle, BOM), examples for every identifier, KVKK notes,
  compatibility table. ADR 0003 on Spring Boot 3 and 4 support.

### Fixed

- Javadoc failed on Windows because of a raw no-break space in the source; separators are now written
  as explicit character codes.
- Multi-module Maven build (`core`, `jakarta`, `spring-boot-starter`, `examples`, `bom`) with Maven Wrapper.
- Java 17 baseline, JaCoCo coverage and Spotless (google-java-format) in the parent POM.
