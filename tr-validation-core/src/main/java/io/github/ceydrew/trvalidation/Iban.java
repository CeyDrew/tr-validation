package io.github.ceydrew.trvalidation;

import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.random.RandomGenerator;

/**
 * International Bank Account Number (IBAN, ISO 13616), with extra checks for Turkish IBANs.
 *
 * <p>{@link #validate(String)} accepts an IBAN of any country in the SWIFT IBAN registry: two
 * letters, two check digits, the country's exact length and the mod-97 checksum. {@link
 * #validateTr(String)} additionally requires a Turkish IBAN: {@code TR}, 26 characters, a 5-digit
 * bank code and a numeric reserved digit ({@code TRkk BBBB B0AA AAAA AAAA AAAA AA}).
 *
 * <p>Static methods accept lenient input: spaces, tabs, no-break spaces and hyphens are ignored and
 * letters may be lower case.
 *
 * <pre>{@code
 * Iban.isValid("TR33 0006 1005 1978 6457 8413 26");   // true
 * Iban.isValid("DE89 3704 0044 0532 0130 00");        // true
 * Iban.isValidTr("DE89 3704 0044 0532 0130 00");      // false, not Turkish
 * Iban.format("tr330006100519786457841326");          // "TR33 0006 1005 1978 6457 8413 26"
 * Iban.mask("TR330006100519786457841326");            // "TR33 **** **** **** **** **13 26"
 * Iban.of("TR330006100519786457841326").bankCode();   // Optional["00061"]
 * }</pre>
 *
 * <p>Instances are immutable and thread-safe; all static methods are stateless.
 */
public final class Iban {

  private static final String TYPE = "IBAN";
  private static final int TR_LENGTH = 26;
  private static final int MIN_LENGTH = 15;
  private static final int MAX_LENGTH = 34;
  private static final int MASK_HEAD = 4;
  private static final int MASK_TAIL = 4;

  /** {@code "TR00"} with letters converted to numbers (T = 29, R = 27), as used by mod-97. */
  private static final int TR00_DIGITS = 292700;

  private final String value;

  private Iban(String value) {
    this.value = value;
  }

  /**
   * Tells whether {@code input} is a valid IBAN of any registered country.
   *
   * <pre>{@code
   * Iban.isValid("GB82 WEST 1234 5698 7654 32"); // true
   * Iban.isValid("TR33 0006 1005 1978 6457 8413 27"); // false, checksum
   * }</pre>
   *
   * @param input the candidate, may be {@code null}
   * @return {@code true} if valid
   */
  public static boolean isValid(String input) {
    return validate(input).isValid();
  }

  /**
   * Validates an IBAN of any registered country and explains a failure. Does not allocate.
   *
   * <pre>{@code
   * Iban.validate("TR330006100519786457841326"); // Valid
   * Iban.validate("XX330006100519786457841326"); // Invalid(INVALID_COUNTRY_CODE)
   * Iban.validate("TR3300061005197864578413");   // Invalid(WRONG_LENGTH)
   * Iban.validate("TR340006100519786457841326"); // Invalid(CHECKSUM_MISMATCH)
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
    char country0 = 0;
    char country1 = 0;
    int head = 0; // country code and check digits converted to numbers
    int remainder = 0; // mod 97 of the BBAN converted to numbers
    for (int i = 0; i < input.length(); i++) {
      char c = input.charAt(i);
      if (Chars.isSeparator(c)) {
        continue;
      }
      boolean digit = Chars.isDigit(c);
      if (!digit && !Chars.isLetter(c)) {
        return ValidationResult.invalid(InvalidReason.INVALID_CHARACTER);
      }
      int numeric = digit ? c - '0' : Chars.toUpper(c) - 'A' + 10;
      int scale = digit ? 10 : 100;
      if (count < 2) {
        if (digit) {
          return ValidationResult.invalid(InvalidReason.INVALID_COUNTRY_CODE);
        }
        if (count == 0) {
          country0 = Chars.toUpper(c);
        } else {
          country1 = Chars.toUpper(c);
        }
        head = head * scale + numeric;
      } else if (count < 4) {
        if (!digit) {
          return ValidationResult.invalid(InvalidReason.INVALID_FORMAT);
        }
        head = head * scale + numeric;
      } else {
        remainder = (remainder * scale + numeric) % 97;
      }
      count++;
    }
    if (count == 0) {
      return ValidationResult.invalid(InvalidReason.EMPTY);
    }
    if (count < 2) {
      return ValidationResult.invalid(InvalidReason.WRONG_LENGTH);
    }
    int expectedLength = IbanCountries.lengthOf(country0, country1);
    if (expectedLength == 0) {
      return ValidationResult.invalid(InvalidReason.INVALID_COUNTRY_CODE);
    }
    if (count != expectedLength) {
      return ValidationResult.invalid(InvalidReason.WRONG_LENGTH);
    }
    // head always has six decimal digits: two letters (two digits each) plus two check digits
    if ((remainder * 1_000_000 + head) % 97 != 1) {
      return ValidationResult.invalid(InvalidReason.CHECKSUM_MISMATCH);
    }
    return ValidationResult.valid();
  }

  /**
   * Tells whether {@code input} is a valid Turkish IBAN.
   *
   * <pre>{@code
   * Iban.isValidTr("TR33 0006 1005 1978 6457 8413 26"); // true
   * Iban.isValidTr("DE89 3704 0044 0532 0130 00");      // false
   * }</pre>
   *
   * @param input the candidate, may be {@code null}
   * @return {@code true} if valid and Turkish
   */
  public static boolean isValidTr(String input) {
    return validateTr(input).isValid();
  }

  /**
   * Validates a Turkish IBAN: everything {@link #validate(String)} checks, plus country {@code TR}
   * and a numeric bank code and reserved digit. Does not allocate.
   *
   * <pre>{@code
   * Iban.validateTr("TR330006100519786457841326"); // Valid
   * Iban.validateTr("DE89370400440532013000");     // Invalid(INVALID_COUNTRY_CODE)
   * }</pre>
   *
   * @param input the candidate, may be {@code null}
   * @return the result, never {@code null}
   */
  public static ValidationResult validateTr(String input) {
    ValidationResult result = validate(input);
    if (!result.isValid()) {
      return result;
    }
    int count = 0;
    for (int i = 0; i < input.length(); i++) {
      char c = input.charAt(i);
      if (Chars.isSeparator(c)) {
        continue;
      }
      if ((count == 0 && Chars.toUpper(c) != 'T') || (count == 1 && Chars.toUpper(c) != 'R')) {
        return ValidationResult.invalid(InvalidReason.INVALID_COUNTRY_CODE);
      }
      // positions 4-8 are the bank code, 9 the reserved digit
      if (count >= 4 && count <= 9 && !Chars.isDigit(c)) {
        return ValidationResult.invalid(InvalidReason.INVALID_FORMAT);
      }
      count++;
    }
    return result;
  }

  /**
   * Removes separators and upper-cases letters. Does not validate.
   *
   * <pre>{@code
   * Iban.normalize("tr33 0006 1005 1978 6457 8413 26"); // "TR330006100519786457841326"
   * }</pre>
   *
   * @param input the raw value, may be {@code null}
   * @return the normalized value, or {@code null} if {@code input} is {@code null}
   */
  public static String normalize(String input) {
    return Chars.stripSeparatorsUpper(input);
  }

  /**
   * Formats a valid IBAN in groups of four, the usual print format.
   *
   * <pre>{@code
   * Iban.format("TR330006100519786457841326"); // "TR33 0006 1005 1978 6457 8413 26"
   * Iban.format("TR33");                       // null
   * }</pre>
   *
   * @param input the raw value, may be {@code null}
   * @return the formatted IBAN, or {@code null} if {@code input} is not a valid IBAN
   */
  public static String format(String input) {
    return isValid(input) ? Chars.groupEvery(normalize(input), 4) : null;
  }

  /**
   * Masks an IBAN for display or logging, keeping the country code, check digits and last four
   * characters, grouped in fours. Input shorter than 15 or longer than 34 significant characters is
   * masked completely.
   *
   * <pre>{@code
   * Iban.mask("TR330006100519786457841326"); // "TR33 **** **** **** **** **13 26"
   * Iban.mask("TR12");                       // "****"
   * }</pre>
   *
   * @param input the raw value, may be {@code null}
   * @return the masked value, or {@code null} if {@code input} is {@code null}
   */
  public static String mask(String input) {
    String normalized = normalize(input);
    if (normalized == null) {
      return null;
    }
    int length = normalized.length();
    boolean plausible = length >= MIN_LENGTH && length <= MAX_LENGTH;
    String masked =
        plausible ? Chars.mask(normalized, length, MASK_HEAD, MASK_TAIL) : Chars.stars(length);
    return Chars.groupEvery(masked, 4);
  }

  /**
   * Creates an IBAN of any registered country, rejecting invalid input.
   *
   * <pre>{@code
   * Iban iban = Iban.of("tr33 0006 1005 1978 6457 8413 26"); // value() is "TR330006100519786457841326"
   * Iban.of("TR33");                                         // throws InvalidValueException
   * }</pre>
   *
   * @param input the candidate, may be {@code null}
   * @return the IBAN
   * @throws InvalidValueException if {@code input} is not a valid IBAN; the message never contains
   *     the input
   */
  public static Iban of(String input) {
    ValidationResult result = validate(input);
    if (result instanceof ValidationResult.Invalid invalid) {
      throw new InvalidValueException(TYPE, invalid.reason());
    }
    return new Iban(normalize(input));
  }

  /**
   * Creates an IBAN if {@code input} is valid.
   *
   * <pre>{@code
   * Iban.parse("TR330006100519786457841326"); // Optional[TR33 **** ...]
   * Iban.parse("TR00");                       // Optional.empty
   * }</pre>
   *
   * @param input the candidate, may be {@code null}
   * @return the IBAN, or empty if invalid
   */
  public static Optional<Iban> parse(String input) {
    return isValid(input) ? Optional.of(new Iban(normalize(input))) : Optional.empty();
  }

  /**
   * Generates a random, algorithmically valid Turkish IBAN.
   *
   * <p><strong>For tests and fixtures only.</strong> The IBAN is fictional: it passes all checks
   * but is not known to belong to any account; never use it for real payments.
   *
   * <pre>{@code
   * Iban fixture = Iban.randomTr();
   * }</pre>
   *
   * @return a valid, fictional Turkish IBAN
   */
  public static Iban randomTr() {
    return randomTr(ThreadLocalRandom.current());
  }

  /**
   * Generates a random, algorithmically valid Turkish IBAN using {@code rng}, for reproducible
   * tests.
   *
   * <p><strong>For tests and fixtures only.</strong> The IBAN is fictional.
   *
   * <pre>{@code
   * Iban fixture = Iban.randomTr(new SplittableRandom(42));
   * }</pre>
   *
   * @param rng the random source, not {@code null}
   * @return a valid, fictional Turkish IBAN
   */
  public static Iban randomTr(RandomGenerator rng) {
    char[] chars = new char[TR_LENGTH];
    chars[0] = 'T';
    chars[1] = 'R';
    int remainder = 0;
    for (int i = 4; i < TR_LENGTH; i++) {
      // position 9 is the reserved digit, always 0 in issued IBANs
      int d = i == 9 ? 0 : rng.nextInt(10);
      chars[i] = (char) ('0' + d);
      remainder = (remainder * 10 + d) % 97;
    }
    int check = 98 - (remainder * 1_000_000 + TR00_DIGITS) % 97;
    chars[2] = (char) ('0' + check / 10);
    chars[3] = (char) ('0' + check % 10);
    return new Iban(new String(chars));
  }

  /**
   * Returns the full, normalized IBAN.
   *
   * <pre>{@code
   * Iban.of("tr33 0006 1005 1978 6457 8413 26").value(); // "TR330006100519786457841326"
   * }</pre>
   *
   * @return the IBAN without separators, upper case
   */
  public String value() {
    return value;
  }

  /**
   * Returns the ISO 3166 country code.
   *
   * <pre>{@code
   * Iban.of("DE89370400440532013000").countryCode(); // "DE"
   * }</pre>
   *
   * @return the two-letter country code
   */
  public String countryCode() {
    return value.substring(0, 2);
  }

  /**
   * Returns the 5-digit bank code of a Turkish IBAN.
   *
   * <pre>{@code
   * Iban.of("TR330006100519786457841326").bankCode(); // Optional["00061"]
   * Iban.of("DE89370400440532013000").bankCode();     // Optional.empty
   * }</pre>
   *
   * @return the bank code, or empty if this is not a Turkish IBAN with a numeric bank code
   */
  public Optional<String> bankCode() {
    return isValidTr(value) ? Optional.of(value.substring(4, 9)) : Optional.empty();
  }

  /**
   * Returns the IBAN in groups of four, as {@link #format(String)}.
   *
   * <pre>{@code
   * Iban.of("TR330006100519786457841326").formatted(); // "TR33 0006 1005 1978 6457 8413 26"
   * }</pre>
   *
   * @return the formatted IBAN
   */
  public String formatted() {
    return Chars.groupEvery(value, 4);
  }

  /**
   * Returns the masked IBAN, as {@link #mask(String)}.
   *
   * <pre>{@code
   * Iban.of("TR330006100519786457841326").masked(); // "TR33 **** **** **** **** **13 26"
   * }</pre>
   *
   * @return the masked IBAN
   */
  public String masked() {
    return mask(value);
  }

  @Override
  public boolean equals(Object o) {
    return o instanceof Iban other && value.equals(other.value);
  }

  @Override
  public int hashCode() {
    return value.hashCode();
  }

  /**
   * Returns the <strong>masked</strong> IBAN so that logging an instance does not leak it.
   *
   * @return the masked IBAN, e.g. {@code "TR33 **** **** **** **** **13 26"}
   */
  @Override
  public String toString() {
    return masked();
  }
}
