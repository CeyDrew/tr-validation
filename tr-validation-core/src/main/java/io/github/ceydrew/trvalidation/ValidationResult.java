package io.github.ceydrew.trvalidation;

import java.util.Optional;

/**
 * Outcome of a validation: either {@link Valid} or {@link Invalid} with a reason.
 *
 * <pre>{@code
 * ValidationResult result = Tckn.validate("10000000147");
 * if (result instanceof ValidationResult.Invalid invalid) {
 *   log.warn("Rejected TCKN: {}", invalid.reason()); // CHECKSUM_MISMATCH
 * }
 * }</pre>
 *
 * <p>Instances returned by the validators are shared constants; validation does not allocate.
 */
public sealed interface ValidationResult permits ValidationResult.Valid, ValidationResult.Invalid {

  /**
   * Returns the shared successful result.
   *
   * <pre>{@code
   * ValidationResult.valid().isValid(); // true
   * }</pre>
   *
   * @return the valid result
   */
  static ValidationResult valid() {
    return Valid.INSTANCE;
  }

  /**
   * Returns the shared failed result for {@code reason}.
   *
   * <pre>{@code
   * ValidationResult.invalid(InvalidReason.WRONG_LENGTH).invalidReason(); // Optional[WRONG_LENGTH]
   * }</pre>
   *
   * @param reason why validation failed, not {@code null}
   * @return the invalid result
   * @throws NullPointerException if {@code reason} is {@code null}
   */
  static ValidationResult invalid(InvalidReason reason) {
    return Invalid.CACHE[reason.ordinal()];
  }

  /**
   * Tells whether the value passed validation.
   *
   * <pre>{@code
   * Tckn.validate("10000000146").isValid(); // true
   * }</pre>
   *
   * @return {@code true} for {@link Valid}, {@code false} for {@link Invalid}
   */
  boolean isValid();

  /**
   * Returns the failure reason, if any.
   *
   * <pre>{@code
   * Tckn.validate("123").invalidReason(); // Optional[WRONG_LENGTH]
   * }</pre>
   *
   * @return the reason for {@link Invalid}, empty for {@link Valid}
   */
  Optional<InvalidReason> invalidReason();

  /** Successful validation. Obtain it with {@link ValidationResult#valid()}. */
  record Valid() implements ValidationResult {
    private static final Valid INSTANCE = new Valid();

    @Override
    public boolean isValid() {
      return true;
    }

    @Override
    public Optional<InvalidReason> invalidReason() {
      return Optional.empty();
    }
  }

  /**
   * Failed validation.
   *
   * @param reason why validation failed, never {@code null}
   */
  record Invalid(InvalidReason reason) implements ValidationResult {
    private static final Invalid[] CACHE = createCache();

    /**
     * Creates a failed result. Prefer {@link ValidationResult#invalid(InvalidReason)}, which reuses
     * shared instances.
     *
     * @param reason why validation failed, not {@code null}
     * @throws NullPointerException if {@code reason} is {@code null}
     */
    public Invalid {
      if (reason == null) {
        throw new NullPointerException("reason");
      }
    }

    private static Invalid[] createCache() {
      InvalidReason[] reasons = InvalidReason.values();
      Invalid[] cache = new Invalid[reasons.length];
      for (InvalidReason reason : reasons) {
        cache[reason.ordinal()] = new Invalid(reason);
      }
      return cache;
    }

    @Override
    public boolean isValid() {
      return false;
    }

    @Override
    public Optional<InvalidReason> invalidReason() {
      return Optional.of(reason);
    }
  }
}
