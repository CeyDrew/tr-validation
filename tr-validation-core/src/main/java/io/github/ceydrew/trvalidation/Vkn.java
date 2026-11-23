package io.github.ceydrew.trvalidation;

import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.random.RandomGenerator;

/**
 * Turkish tax identification number (Vergi Kimlik Numarası, VKN), issued to companies and other
 * legal entities.
 *
 * <p>Rules: 10 ASCII digits (a leading {@code 0} is allowed). For the first nine digits {@code
 * d[i]} ({@code i = 0..8}): {@code t = (d[i] + 9 - i) mod 10}; {@code v = (t * 2^(9 - i)) mod 9},
 * and {@code v = 9} when {@code t != 0 && v == 0}. The last digit equals {@code (10 - (sum of v mod
 * 10)) mod 10}.
 *
 * <p>Static methods accept lenient input: spaces, tabs, no-break spaces and hyphens are ignored.
 *
 * <pre>{@code
 * Vkn.isValid("1234567890");  // true
 * Vkn.validate("1234567891"); // Invalid(CHECKSUM_MISMATCH)
 * Vkn.mask("1234567890");     // "12******90"
 * Vkn.of("123 456 7890").value(); // "1234567890"
 * }</pre>
 *
 * <p>Instances are immutable and thread-safe; all static methods are stateless.
 */
public final class Vkn {

  private static final String TYPE = "VKN";
  private static final int LENGTH = 10;
  private static final int MASK_HEAD = 2;
  private static final int MASK_TAIL = 2;

  private final String value;

  private Vkn(String value) {
    this.value = value;
  }

  /**
   * Tells whether {@code input} is a valid VKN.
   *
   * <pre>{@code
   * Vkn.isValid("1234567890"); // true
   * Vkn.isValid(null);         // false
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
   * Vkn.validate("1234567890");  // Valid
   * Vkn.validate("123456789");   // Invalid(WRONG_LENGTH)
   * Vkn.validate("12345678A0");  // Invalid(INVALID_CHARACTER)
   * }</pre>
   *
   * @param input the candidate, may be {@code null}
   * @return the result, never {@code null}
   */
  public static ValidationResult validate(String input) {
    if (input == null) {
      return ValidationResult.invalid(InvalidReason.EMPTY);
    }
    int count = 0;
    int sum = 0;
    int check = 0;
    for (int i = 0; i < input.length(); i++) {
      char c = input.charAt(i);
      if (Chars.isSeparator(c)) {
        continue;
      }
      if (!Chars.isDigit(c)) {
        return ValidationResult.invalid(InvalidReason.INVALID_CHARACTER);
      }
      int d = c - '0';
      if (count < LENGTH - 1) {
        sum += weigh(d, count);
      } else {
        check = d;
      }
      count++;
    }
    if (count == 0) {
      return ValidationResult.invalid(InvalidReason.EMPTY);
    }
    if (count != LENGTH) {
      return ValidationResult.invalid(InvalidReason.WRONG_LENGTH);
    }
    return check == checkDigit(sum)
        ? ValidationResult.valid()
        : ValidationResult.invalid(InvalidReason.CHECKSUM_MISMATCH);
  }

  /**
   * Removes separators (spaces, tabs, no-break spaces, hyphens). Does not validate.
   *
   * <pre>{@code
   * Vkn.normalize("123-456-7890"); // "1234567890"
   * }</pre>
   *
   * @param input the raw value, may be {@code null}
   * @return the normalized value, or {@code null} if {@code input} is {@code null}
   */
  public static String normalize(String input) {
    return Chars.stripSeparators(input);
  }

  /**
   * Masks a VKN for display or logging, keeping the first 2 and last 2 digits. Input that does not
   * have 10 significant characters is masked completely.
   *
   * <pre>{@code
   * Vkn.mask("1234567890"); // "12******90"
   * Vkn.mask("123");        // "***"
   * }</pre>
   *
   * @param input the raw value, may be {@code null}
   * @return the masked value, or {@code null} if {@code input} is {@code null}
   */
  public static String mask(String input) {
    String normalized = normalize(input);
    return normalized == null ? null : Chars.mask(normalized, LENGTH, MASK_HEAD, MASK_TAIL);
  }

  /**
   * Creates a VKN, rejecting invalid input.
   *
   * <pre>{@code
   * Vkn vkn = Vkn.of("1234567890");
   * Vkn.of("1234567891"); // throws InvalidValueException(CHECKSUM_MISMATCH)
   * }</pre>
   *
   * @param input the candidate, may be {@code null}
   * @return the VKN
   * @throws InvalidValueException if {@code input} is not a valid VKN; the message never contains
   *     the input
   */
  public static Vkn of(String input) {
    ValidationResult result = validate(input);
    if (result instanceof ValidationResult.Invalid invalid) {
      throw new InvalidValueException(TYPE, invalid.reason());
    }
    return new Vkn(normalize(input));
  }

  /**
   * Creates a VKN if {@code input} is valid.
   *
   * <pre>{@code
   * Vkn.parse("1234567890"); // Optional[12******90]
   * Vkn.parse("");           // Optional.empty
   * }</pre>
   *
   * @param input the candidate, may be {@code null}
   * @return the VKN, or empty if invalid
   */
  public static Optional<Vkn> parse(String input) {
    return isValid(input) ? Optional.of(new Vkn(normalize(input))) : Optional.empty();
  }

  /**
   * Generates a random, algorithmically valid VKN.
   *
   * <p><strong>For tests and fixtures only.</strong> The number is fictional: it passes the
   * checksum but may not belong to any taxpayer and must never be presented as a real one.
   *
   * <pre>{@code
   * Vkn fixture = Vkn.random();
   * }</pre>
   *
   * @return a valid, fictional VKN
   */
  public static Vkn random() {
    return random(ThreadLocalRandom.current());
  }

  /**
   * Generates a random, algorithmically valid VKN using {@code rng}, for reproducible tests.
   *
   * <p><strong>For tests and fixtures only.</strong> The number is fictional.
   *
   * <pre>{@code
   * Vkn fixture = Vkn.random(new SplittableRandom(42));
   * }</pre>
   *
   * @param rng the random source, not {@code null}
   * @return a valid, fictional VKN
   */
  public static Vkn random(RandomGenerator rng) {
    int[] d = new int[LENGTH];
    int sum = 0;
    for (int i = 0; i < LENGTH - 1; i++) {
      d[i] = rng.nextInt(10);
      sum += weigh(d[i], i);
    }
    d[LENGTH - 1] = checkDigit(sum);
    return new Vkn(Chars.digitsToString(d));
  }

  /**
   * Returns the full, normalized number.
   *
   * <pre>{@code
   * Vkn.of("123 456 7890").value(); // "1234567890"
   * }</pre>
   *
   * @return the 10-digit number
   */
  public String value() {
    return value;
  }

  /**
   * Returns the masked number, as {@link #mask(String)}.
   *
   * <pre>{@code
   * Vkn.of("1234567890").masked(); // "12******90"
   * }</pre>
   *
   * @return the masked number
   */
  public String masked() {
    return mask(value);
  }

  @Override
  public boolean equals(Object o) {
    return o instanceof Vkn other && value.equals(other.value);
  }

  @Override
  public int hashCode() {
    return value.hashCode();
  }

  /**
   * Returns the <strong>masked</strong> number so that logging an instance does not leak it.
   *
   * @return the masked number, e.g. {@code "12******90"}
   */
  @Override
  public String toString() {
    return masked();
  }

  /** Weighted value of digit {@code d} at zero-based position {@code i} (0..8). */
  private static int weigh(int d, int i) {
    int t = (d + 9 - i) % 10;
    if (t == 0) {
      return 0;
    }
    int v = (t << (9 - i)) % 9;
    return v == 0 ? 9 : v;
  }

  private static int checkDigit(int sum) {
    return (10 - sum % 10) % 10;
  }
}
