# ADR 0002: Message bundles of the Jakarta module

- Status: accepted
- Date: 2026-11-23

## Context

`tr-validation-jakarta` needs default messages for its constraints in English and Turkish.
The Bean Validation specification reads user messages from the `ValidationMessages` resource bundle.
`ResourceBundle` loads only the **first** `ValidationMessages.properties` it finds on the classpath, so:

- if the library ships `ValidationMessages.properties`, an application with its own file either hides
  the library's messages (users see `{io.github...Tckn.message}`) or is hidden by them, depending on
  classpath order;
- `ResourceBundle` falls back to the **JVM default locale** before the base bundle. On a server whose
  default locale is `tr_TR`, a request for English messages would return the Turkish ones unless an
  explicit `_en` bundle exists. This was reproduced in this project's test suite on a Turkish Windows
  machine.

## Decision

- Ship the messages as `ContributorValidationMessages.properties`, `_en` and `_tr`. Hibernate Validator
  (the provider behind Spring Boot and most Jakarta EE servers) merges every
  `ContributorValidationMessages` bundle on the classpath, after the application's own
  `ValidationMessages`.
- Ship an explicit `_en` bundle identical to the base bundle, so English never falls back to a Turkish
  JVM default.
- Keys are `io.github.ceydrew.trvalidation.jakarta.<AnnotationName>.message`. Applications override
  them in their own `ValidationMessages[_xx].properties`.
- Files are UTF-8 (Java 9+ `PropertyResourceBundle` reads UTF-8).
- One message per constraint; the failure reason (`InvalidReason`) is not exposed in the message.

## Consequences

- No clash with application bundles; overriding works the usual way.
- With a provider other than Hibernate Validator the default messages are not found and the raw key
  is shown; such users set `message = "..."` or copy the keys into their `ValidationMessages`.
- Adding a language means adding one more `ContributorValidationMessages_xx.properties` file; a test
  checks that all bundles define the same keys.
