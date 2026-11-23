# ADR 0001: Public API style of the core module

- Status: accepted
- Date: 2026-11-23

## Context

`tr-validation-core` is consumed in three very different ways:

1. **Quick checks** at system boundaries (`if (!Tckn.isValid(input)) ...`), often on raw user input
   with spaces or hyphens.
2. **Explaining failures** to users or logs: "wrong length" and "checksum mismatch" need different
   messages, so a plain boolean is not enough.
3. **Domain modelling**: once a value is known to be valid, code wants to pass a typed value around
   (`Tckn` instead of `String`) so validity does not have to be re-checked and the value cannot be
   confused with a VKN or a phone number.

On top of that the data is personal data under KVKK: it must not leak through `toString()`,
exception messages or logs by accident.

Options considered:

- **A. Static utility class only** (`TcknValidator.isValid(String)`). Simple, zero allocation, but
  no typed value and no place for "parsed and known valid".
- **B. Value object only** (`Tckn.of(String)` throwing). Good for domain code, awkward for quick
  checks: callers write try/catch for control flow.
- **C. Value object with static helpers on the same type.** One entry point per identifier.

## Decision

Option **C**. Every identifier is a `final` class with a private constructor:

| Method | Returns | On invalid / null input |
|---|---|---|
| `isValid(String)` | `boolean` | `false` |
| `validate(String)` | `ValidationResult` (sealed: `Valid` / `Invalid(InvalidReason)`) | `Invalid` with reason |
| `normalize(String)` | `String` with separators removed, no validation | `null` for `null` |
| `mask(String)` | masked `String`, never reveals more than the documented digits | fully masked; `null` for `null` |
| `of(String)` | typed instance | throws `InvalidValueException` (an `IllegalArgumentException`) |
| `parse(String)` | `Optional<T>` | `Optional.empty()` |
| `random()` / `random(RandomGenerator)` | typed instance | — (test data only) |

Instances expose `value()` (normalized) and `masked()`; `toString()` returns the **masked** form.

Further rules:

- **Lenient input.** Static methods accept spaces, tabs, non-breaking spaces and hyphens anywhere and
  ignore them. Only ASCII digits `0-9` count as digits (no Unicode digit look-alikes).
- **No exceptions for bad input** except from `of(...)`. `null` is just another invalid value
  (`InvalidReason.EMPTY`).
- **No input in exception messages.** `InvalidValueException` carries the type name and the reason,
  never the value.
- **Zero allocation on the hot path.** `isValid`/`validate` scan the input once without creating
  strings or arrays; `ValidationResult` instances are cached constants.
- **One shared reason enum** (`InvalidReason`). New constants may be added in minor releases;
  callers switching over it should keep a `default` branch.
- **Single package** `io.github.ceydrew.trvalidation`, so shared helpers stay package-private and
  out of the public API. The jar declares `Automatic-Module-Name: io.github.ceydrew.trvalidation`.

## Consequences

- One class per identifier to discover and document; IDE completion on `Tckn.` shows everything.
- Static and instance APIs live together, so each class is slightly larger than a pure utility.
- Adding a method to all identifiers is a public API change and goes to the CHANGELOG.
- Masked `toString()` means debugging shows `123******01`; use `value()` deliberately when the full
  value is needed.
