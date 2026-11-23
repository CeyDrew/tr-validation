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
- Multi-module Maven build (`core`, `jakarta`, `spring-boot-starter`, `examples`, `bom`) with Maven Wrapper.
- Java 17 baseline, JaCoCo coverage and Spotless (google-java-format) in the parent POM.
