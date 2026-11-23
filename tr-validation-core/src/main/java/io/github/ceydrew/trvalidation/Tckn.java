package io.github.ceydrew.trvalidation;

import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.random.RandomGenerator;

/**
 * Turkish citizenship identity number (T.C. Kimlik Numarası, TCKN).
 *
 * <p>Rules: 11 ASCII digits, the first digit is not {@code 0}, the 10th digit is {@code ((d1 + d3 +
 * d5 + d7 + d9) * 7 - (d2 + d4 + d6 + d8)) mod 10} and the 11th digit is {@code (d1 + ... + d10)
 * mod 10}. Numbers starting with {@code 99} are foreign identity numbers and are rejected here with
 * {@link InvalidReason#INVALID_PREFIX}; use {@link Ykn} for them.
 *
 * <p>Static methods accept lenient input: spaces, tabs, no-break spaces and hyphens are ignored.
 *
 * <pre>{@code
 * Tckn.isValid("10000000146");           // true
 * Tckn.validate("100 000 001 46");        // Valid
 * Tckn.validate("10000000147");           // Invalid(CHECKSUM_MISMATCH)
 * Tckn.mask("10000000146");               // "100******46"
 *
 * Tckn tckn = Tckn.of("10000000146");
 * tckn.value();                           // "10000000146"
 * tckn.toString();                        // "100******46" (never the full value)
 * }</pre>
 *
 * <p>Instances are immutable and thread-safe; all static methods are stateless.
 */
public final class Tckn {

  private static final String TYPE = "TCKN";
  private static final int MASK_HEAD = 3;
  private static final int MASK_TAIL = 2;

  private final String value;

  private Tckn(String value) {
    this.value = value;
  }

  /**
   * Tells whether {@code input} is a valid TCKN.
   *
   * <pre>{@code
   * Tckn.isValid("10000000146"); // true
   * Tckn.isValid("99000000000"); // false, that is a YKN prefix
   * Tckn.isValid(null);          // false
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
   * Tckn.validate("10000000146"); // Valid
   * Tckn.validate("1000000014");  // Invalid(WRONG_LENGTH)
   * Tckn.validate("01000000146"); // Invalid(INVALID_PREFIX)
   * }</pre>
   *
   * @param input the candidate, may be {@code null}
   * @return the result, never {@code null}
   */
  public static ValidationResult validate(String input) {
    return IdentityNumbers.validate(input, false);
  }

  /**
   * Removes separators (spaces, tabs, no-break spaces, hyphens). Does not validate.
   *
   * <pre>{@code
   * Tckn.normalize(" 100 000-001 46 "); // "10000000146"
   * }</pre>
   *
   * @param input the raw value, may be {@code null}
   * @return the normalized value, or {@code null} if {@code input} is {@code null}
   */
  public static String normalize(String input) {
    return Chars.stripSeparators(input);
  }

  /**
   * Masks a TCKN for display or logging, keeping the first 3 and last 2 digits. Input that does not
   * have 11 significant characters is masked completely, so nothing leaks.
   *
   * <pre>{@code
   * Tckn.mask("10000000146"); // "100******46"
   * Tckn.mask("12345");       // "*****"
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
   * Creates a TCKN, rejecting invalid input.
   *
   * <pre>{@code
   * Tckn tckn = Tckn.of("100 000 001 46"); // value() is "10000000146"
   * Tckn.of("123");                        // throws InvalidValueException(WRONG_LENGTH)
   * }</pre>
   *
   * @param input the candidate, may be {@code null}
   * @return the TCKN
   * @throws InvalidValueException if {@code input} is not a valid TCKN; the message never contains
   *     the input
   */
  public static Tckn of(String input) {
    ValidationResult result = validate(input);
    if (result instanceof ValidationResult.Invalid invalid) {
      throw new InvalidValueException(TYPE, invalid.reason());
    }
    return new Tckn(normalize(input));
  }

  /**
   * Creates a TCKN if {@code input} is valid.
   *
   * <pre>{@code
   * Tckn.parse("10000000146"); // Optional[100******46]
   * Tckn.parse("abc");         // Optional.empty
   * }</pre>
   *
   * @param input the candidate, may be {@code null}
   * @return the TCKN, or empty if invalid
   */
  public static Optional<Tckn> parse(String input) {
    return isValid(input) ? Optional.of(new Tckn(normalize(input))) : Optional.empty();
  }

  /**
   * Generates a random, algorithmically valid TCKN.
   *
   * <p><strong>For tests and fixtures only.</strong> The number is fictional: it passes the
   * checksum but is not issued to anyone and must never be presented as a real identity.
   *
   * <pre>{@code
   * Tckn fixture = Tckn.random();
   * }</pre>
   *
   * @return a valid, fictional TCKN
   */
  public static Tckn random() {
    return random(ThreadLocalRandom.current());
  }

  /**
   * Generates a random, algorithmically valid TCKN using {@code rng}, for reproducible tests.
   *
   * <p><strong>For tests and fixtures only.</strong> The number is fictional.
   *
   * <pre>{@code
   * Tckn fixture = Tckn.random(new SplittableRandom(42));
   * }</pre>
   *
   * @param rng the random source, not {@code null}
   * @return a valid, fictional TCKN
   */
  public static Tckn random(RandomGenerator rng) {
    return new Tckn(IdentityNumbers.random(rng, false));
  }

  /**
   * Returns the full, normalized number. Treat it as personal data.
   *
   * <pre>{@code
   * Tckn.of("100 000 001 46").value(); // "10000000146"
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
   * Tckn.of("10000000146").masked(); // "100******46"
   * }</pre>
   *
   * @return the masked number
   */
  public String masked() {
    return mask(value);
  }

  @Override
  public boolean equals(Object o) {
    return o instanceof Tckn other && value.equals(other.value);
  }

  @Override
  public int hashCode() {
    return value.hashCode();
  }

  /**
   * Returns the <strong>masked</strong> number so that logging an instance does not leak it.
   *
   * @return the masked number, e.g. {@code "100******46"}
   */
  @Override
  public String toString() {
    return masked();
  }
}
