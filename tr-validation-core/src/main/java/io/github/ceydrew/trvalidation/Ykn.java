package io.github.ceydrew.trvalidation;

import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.random.RandomGenerator;

/**
 * Foreign identity number (Yabancı Kimlik Numarası, YKN), issued to foreign residents in Türkiye.
 *
 * <p>Rules: 11 ASCII digits starting with {@code 99} and passing the same checksum as {@link Tckn}.
 * Numbers not starting with {@code 99} are rejected with {@link InvalidReason#INVALID_PREFIX}.
 *
 * <p>Static methods accept lenient input: spaces, tabs, no-break spaces and hyphens are ignored.
 *
 * <pre>{@code
 * Ykn.isValid("99000000042");   // true
 * Ykn.isValid("10000000146");   // false, that is a TCKN
 * Ykn.mask("99000000042");      // "990******42"
 * Ykn.of("99000000042").value(); // "99000000042"
 * }</pre>
 *
 * <p>Instances are immutable and thread-safe; all static methods are stateless.
 */
public final class Ykn {

  private static final String TYPE = "YKN";
  private static final int MASK_HEAD = 3;
  private static final int MASK_TAIL = 2;

  private final String value;

  private Ykn(String value) {
    this.value = value;
  }

  /**
   * Tells whether {@code input} is a valid YKN.
   *
   * <pre>{@code
   * Ykn.isValid("99000000042"); // true
   * Ykn.isValid(null);          // false
   * }</pre>
   *
   * @param input the candidate, may be {@code null}
   * @return {@code true} if valid
   */
  public static boolean isValid(String input) {
    return validate(input).isValid();
  }

  /**
   * Validates {@code input} and explains a failure. Does not allocate.
   *
   * <pre>{@code
   * Ykn.validate("99000000042"); // Valid
   * Ykn.validate("10000000146"); // Invalid(INVALID_PREFIX)
   * Ykn.validate("99000000043"); // Invalid(CHECKSUM_MISMATCH)
   * }</pre>
   *
   * @param input the candidate, may be {@code null}
   * @return the result, never {@code null}
   */
  public static ValidationResult validate(String input) {
    return IdentityNumbers.validate(input, true);
  }

  /**
   * Removes separators (spaces, tabs, no-break spaces, hyphens). Does not validate.
   *
   * <pre>{@code
   * Ykn.normalize("990 000 000 42"); // "99000000042"
   * }</pre>
   *
   * @param input the raw value, may be {@code null}
   * @return the normalized value, or {@code null} if {@code input} is {@code null}
   */
  public static String normalize(String input) {
    return Chars.stripSeparators(input);
  }

  /**
   * Masks a YKN for display or logging, keeping the first 3 and last 2 digits. Input that does not
   * have 11 significant characters is masked completely.
   *
   * <pre>{@code
   * Ykn.mask("99000000042"); // "990******42"
   * }</pre>
   *
   * @param input the raw value, may be {@code null}
   * @return the masked value, or {@code null} if {@code input} is {@code null}
   */
  public static String mask(String input) {
    String normalized = normalize(input);
    return normalized == null
        ? null
        : Chars.mask(normalized, IdentityNumbers.LENGTH, MASK_HEAD, MASK_TAIL);
  }

  /**
   * Creates a YKN, rejecting invalid input.
   *
   * <pre>{@code
   * Ykn ykn = Ykn.of("99000000042");
   * Ykn.of("10000000146"); // throws InvalidValueException(INVALID_PREFIX)
   * }</pre>
   *
   * @param input the candidate, may be {@code null}
   * @return the YKN
   * @throws InvalidValueException if {@code input} is not a valid YKN; the message never contains
   *     the input
   */
  public static Ykn of(String input) {
    ValidationResult result = validate(input);
    if (result instanceof ValidationResult.Invalid invalid) {
      throw new InvalidValueException(TYPE, invalid.reason());
    }
    return new Ykn(normalize(input));
  }

  /**
   * Creates a YKN if {@code input} is valid.
   *
   * <pre>{@code
   * Ykn.parse("99000000042"); // Optional[990******42]
   * Ykn.parse("10000000146"); // Optional.empty
   * }</pre>
   *
   * @param input the candidate, may be {@code null}
   * @return the YKN, or empty if invalid
   */
  public static Optional<Ykn> parse(String input) {
    return isValid(input) ? Optional.of(new Ykn(normalize(input))) : Optional.empty();
  }

  /**
   * Generates a random, algorithmically valid YKN.
   *
   * <p><strong>For tests and fixtures only.</strong> The number is fictional: it passes the
   * checksum but is not issued to anyone and must never be presented as a real identity.
   *
   * <pre>{@code
   * Ykn fixture = Ykn.random();
   * }</pre>
   *
   * @return a valid, fictional YKN
   */
  public static Ykn random() {
    return random(ThreadLocalRandom.current());
  }

  /**
   * Generates a random, algorithmically valid YKN using {@code rng}, for reproducible tests.
   *
   * <p><strong>For tests and fixtures only.</strong> The number is fictional.
   *
   * <pre>{@code
   * Ykn fixture = Ykn.random(new SplittableRandom(42));
   * }</pre>
   *
   * @param rng the random source, not {@code null}
   * @return a valid, fictional YKN
   */
  public static Ykn random(RandomGenerator rng) {
    return new Ykn(IdentityNumbers.random(rng, true));
  }

  /**
   * Returns the full, normalized number. Treat it as personal data.
   *
   * <pre>{@code
   * Ykn.of("990 000 000 42").value(); // "99000000042"
   * }</pre>
   *
   * @return the 11-digit number
   */
  public String value() {
    return value;
  }

  /**
   * Returns the masked number, as {@link #mask(String)}.
   *
   * <pre>{@code
   * Ykn.of("99000000042").masked(); // "990******42"
   * }</pre>
   *
   * @return the masked number
   */
  public String masked() {
    return mask(value);
  }

  @Override
  public boolean equals(Object o) {
    return o instanceof Ykn other && value.equals(other.value);
  }

  @Override
  public int hashCode() {
    return value.hashCode();
  }

  /**
   * Returns the <strong>masked</strong> number so that logging an instance does not leak it.
   *
   * @return the masked number, e.g. {@code "990******42"}
   */
  @Override
  public String toString() {
    return masked();
  }
}
