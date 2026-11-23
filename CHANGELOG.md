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
- Multi-module Maven build (`core`, `jakarta`, `spring-boot-starter`, `examples`, `bom`) with Maven Wrapper.
- Java 17 baseline, JaCoCo coverage and Spotless (google-java-format) in the parent POM.
