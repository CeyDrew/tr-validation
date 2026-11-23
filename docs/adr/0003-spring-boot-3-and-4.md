# ADR 0003: Supporting Spring Boot 3 and 4 from one starter

- Status: accepted
- Date: 2026-11-23

## Context

The project targets Java 17 so that Spring Boot 3 users are included (see PLAN.md). Spring Boot 4,
current at the time of writing, moved to Jackson 3 (`tools.jackson`), while Spring Boot 3 uses
Jackson 2 (`com.fasterxml.jackson`). The two Jackson versions have different packages and partly
different APIs (`ValueSerializer` vs `JsonSerializer`, `ValueSerializerModifier` vs
`BeanSerializerModifier`). Many teams, including in insurance and banking, will stay on Spring Boot 3
for a while.

Options considered:

- **A. Two starters** (`...-starter` for Boot 4, `...-starter-boot3`). Clear, but two artifacts to
  release, document and keep in sync.
- **B. One starter for Boot 4 only.** Simplest, excludes Boot 3 users.
- **C. One starter, compiled against Boot 4, with a Jackson 2 and a Jackson 3 module that each activate
  only when their Jackson is on the classpath.**

## Decision

Option **C**.

- `TrValidationJacksonModule` (Jackson 3) and `TrValidationJackson2Module` (Jackson 2) share their logic
  through a package-private `ValueType` table and `MaskType`; only the thin Jackson adapters differ.
- The auto-configuration declares each module in a nested configuration guarded by
  `@ConditionalOnClass(name = ...)` with string class names, so the missing Jackson is never loaded.
- Both Jackson dependencies are `optional` in the starter; the application's Spring Boot brings the
  right one. `spring-boot-starter-validation` is a normal dependency whose version the application's
  Spring Boot dependency management decides.
- The auto-configuration uses only APIs present in both Spring Boot 3 and 4 (`@AutoConfiguration`,
  `@ConditionalOnClass`, `@ConditionalOnMissingBean`, `@EnableConfigurationProperties`, JavaBean-style
  `@ConfigurationProperties`).
- An unpublished module, `tr-validation-boot3-tests`, runs the starter jar in a Spring Boot 3.5
  application with Jackson 2 on every build. Dependabot ignores major Spring Boot updates so that this
  module stays on 3.x and the starter on 4.x.

## Consequences

- One artifact serves both Spring Boot generations; the README compatibility table states which
  versions are tested.
- Each masking feature is implemented twice in small adapters; a shared contract test
  (`JacksonModuleContract`) runs the same cases against both modules.
- When Spring Boot 3 reaches end of support, the Jackson 2 module and the compatibility module can be
  removed in a major release.
