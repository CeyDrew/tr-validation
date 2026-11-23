package io.github.ceydrew.trvalidation;

import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.random.RandomGenerator;

/**
 * Turkish civilian vehicle license plate (plaka).
 *
 * <p>Rules: a province code {@code 01-81}, then one of these letter/digit combinations:
 *
 * <ul>
 *   <li>1 letter + 4 digits: {@code 34 A 1234}
 *   <li>2 letters + 3 or 4 digits: {@code 06 AB 123}, {@code 06 AB 1234}
 *   <li>3 letters + 2 or 3 digits: {@code 35 ABC 12}, {@code 35 ABC 123}
 * </ul>
 *
 * <p>Letters are the 23 used on plates: the Latin alphabet without {@code Q}, {@code W} and {@code
 * X} (and without Turkish-specific letters). Input may be lower case and contain spaces, hyphens or
 * no separators at all. Diplomatic, military and other special plates are out of scope.
 *
 * <pre>{@code
 * LicensePlate.isValid("34 ABC 123");   // true
 * LicensePlate.isValid("34abc123");     // true
 * LicensePlate.validate("82 A 1234");   // Invalid(INVALID_PROVINCE_CODE)
 * LicensePlate.format("06ab1234");      // "06 AB 1234"
 * LicensePlate.mask("34 ABC 123");      // "34 *** *23"
 * }</pre>
 *
 * <p>Instances are immutable and thread-safe; all static methods are stateless.
 */
public final class LicensePlate {

  private static final String TYPE = "license plate";
  private static final String LETTERS = "ABCDEFGHIJKLMNOPRSTUVYZ";
  private static final int PROVINCE_DIGITS = 2;
  private static final int MASK_TAIL = 2;

  /** Allowed letter/digit counts, as {letters, digits} pairs. */
  private static final int[][] PATTERNS = {{1, 4}, {2, 3}, {2, 4}, {3, 2}, {3, 3}};

  private final String value;
  private final int letterCount;

  private LicensePlate(String value) {
    this.value = value;
    int letters = 0;
    for (int i = PROVINCE_DIGITS; i < value.length(); i++) {
      if (Chars.isLetter(value.charAt(i))) {
        letters++;
      }
    }
    this.letterCount = letters;
  }

  /**
   * Tells whether {@code input} is a valid license plate.
   *
   * <pre>{@code
   * LicensePlate.isValid("06 AB 123"); // true
   * LicensePlate.isValid("06 ABCD 1"); // false
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
   * LicensePlate.validate("34 ABC 123");  // Valid
   * LicensePlate.validate("34 QW 123");   // Invalid(INVALID_CHARACTER)
   * LicensePlate.validate("00 A 1234");   // Invalid(INVALID_PROVINCE_CODE)
   * LicensePlate.validate("34 A 12");     // Invalid(INVALID_FORMAT)
   * }</pre>
   *
   * @param input the candidate, may be {@code null}
   * @return the result, never {@code null}
   */
  public static ValidationResult validate(String input) {
    if (input == null) {
      return ValidationResult.invalid(InvalidReason.EMPTY);
    }
    int leadingDigits = 0;
    int province = 0;
    int letters = 0;
    int trailingDigits = 0;
    for (int i = 0; i < input.length(); i++) {
      char c = input.charAt(i);
      if (Chars.isSeparator(c)) {
        continue;
      }
      if (Chars.isDigit(c)) {
        if (letters > 0) {
          trailingDigits++;
        } else if (++leadingDigits <= PROVINCE_DIGITS) {
          province = province * 10 + (c - '0');
        }
      } else if (isPlateLetter(c)) {
        if (leadingDigits == 0 || trailingDigits > 0) {
          return ValidationResult.invalid(InvalidReason.INVALID_FORMAT);
        }
        letters++;
      } else {
        return ValidationResult.invalid(InvalidReason.INVALID_CHARACTER);
      }
    }
    if (leadingDigits + letters == 0) {
      return ValidationResult.invalid(InvalidReason.EMPTY);
    }
    if (leadingDigits != PROVINCE_DIGITS) {
      return ValidationResult.invalid(InvalidReason.INVALID_FORMAT);
    }
    if (!Provinces.isValid(province)) {
      return ValidationResult.invalid(InvalidReason.INVALID_PROVINCE_CODE);
    }
    for (int[] pattern : PATTERNS) {
      if (pattern[0] == letters && pattern[1] == trailingDigits) {
        return ValidationResult.valid();
      }
    }
    return ValidationResult.invalid(InvalidReason.INVALID_FORMAT);
  }

  /**
   * Removes separators and upper-cases letters. Does not validate.
   *
   * <pre>{@code
   * LicensePlate.normalize("34 abc-123"); // "34ABC123"
   * }</pre>
   *
   * @param input the raw value, may be {@code null}
   * @return the normalized value, or {@code null} if {@code input} is {@code null}
   */
  public static String normalize(String input) {
    return Chars.stripSeparatorsUpper(input);
  }

  /**
   * Formats a valid plate with single spaces between the groups.
   *
   * <pre>{@code
   * LicensePlate.format("34abc123"); // "34 ABC 123"
   * LicensePlate.format("34");       // null
   * }</pre>
   *
   * @param input the raw value, may be {@code null}
   * @return the formatted plate, or {@code null} if {@code input} is not valid
   */
  public static String format(String input) {
    return parse(input).map(LicensePlate::formatted).orElse(null);
  }

  /**
   * Masks a plate for display or logging, keeping the province code and the last two digits.
   * Invalid input is masked completely.
   *
   * <pre>{@code
   * LicensePlate.mask("34 ABC 123"); // "34 *** *23"
   * LicensePlate.mask("34 A 1234");  // "34 * **34"
   * LicensePlate.mask("XYZ");        // "***"
   * }</pre>
   *
   * @param input the raw value, may be {@code null}
   * @return the masked value, or {@code null} if {@code input} is {@code null}
   */
  public static String mask(String input) {
    if (input == null) {
      return null;
    }
    Optional<LicensePlate> plate = parse(input);
    return plate.isPresent() ? plate.get().masked() : Chars.stars(normalize(input).length());
  }

  /**
   * Creates a plate, rejecting invalid input.
   *
   * <pre>{@code
   * LicensePlate plate = LicensePlate.of("34 abc 123");
   * plate.value();        // "34ABC123"
   * plate.provinceCode(); // 34
   * }</pre>
   *
   * @param input the candidate, may be {@code null}
   * @return the plate
   * @throws InvalidValueException if {@code input} is not valid; the message never contains the
   *     input
   */
  public static LicensePlate of(String input) {
    ValidationResult result = validate(input);
    if (result instanceof ValidationResult.Invalid invalid) {
      throw new InvalidValueException(TYPE, invalid.reason());
    }
    return new LicensePlate(normalize(input));
  }

  /**
   * Creates a plate if {@code input} is valid.
   *
   * <pre>{@code
   * LicensePlate.parse("06 AB 1234"); // Optional[06 ** **34]
   * LicensePlate.parse("06 AB 1");    // Optional.empty
   * }</pre>
   *
   * @param input the candidate, may be {@code null}
   * @return the plate, or empty if invalid
   */
  public static Optional<LicensePlate> parse(String input) {
    return isValid(input) ? Optional.of(new LicensePlate(normalize(input))) : Optional.empty();
  }

  /**
   * Generates a random valid plate.
   *
   * <p><strong>For tests and fixtures only.</strong> The plate is fictional and may coincide with a
   * registered vehicle; never present it as a real one.
   *
   * <pre>{@code
   * LicensePlate fixture = LicensePlate.random();
   * }</pre>
   *
   * @return a valid, fictional plate
   */
  public static LicensePlate random() {
    return random(ThreadLocalRandom.current());
  }

  /**
   * Generates a random valid plate using {@code rng}, for reproducible tests.
   *
   * <p><strong>For tests and fixtures only.</strong> The plate is fictional.
   *
   * <pre>{@code
   * LicensePlate fixture = LicensePlate.random(new SplittableRandom(42));
   * }</pre>
   *
   * @param rng the random source, not {@code null}
   * @return a valid, fictional plate
   */
  public static LicensePlate random(RandomGenerator rng) {
    int[] pattern = PATTERNS[rng.nextInt(PATTERNS.length)];
    StringBuilder sb = new StringBuilder(9);
    sb.append(Provinces.format(rng.nextInt(Provinces.MIN, Provinces.MAX + 1)));
    for (int i = 0; i < pattern[0]; i++) {
      sb.append(LETTERS.charAt(rng.nextInt(LETTERS.length())));
    }
    sb.append(1 + rng.nextInt(9));
    for (int i = 1; i < pattern[1]; i++) {
      sb.append(rng.nextInt(10));
    }
    return new LicensePlate(sb.toString());
  }

  /**
   * Returns the normalized plate, without separators. Treat it as personal data.
   *
   * <pre>{@code
   * LicensePlate.of("34 abc 123").value(); // "34ABC123"
   * }</pre>
   *
   * @return the plate
   */
  public String value() {
    return value;
  }

  /**
   * Returns the province code.
   *
   * <pre>{@code
   * LicensePlate.of("06 AB 123").provinceCode(); // 6
   * }</pre>
   *
   * @return the province code, {@code 1-81}
   */
  public int provinceCode() {
    return Integer.parseInt(value.substring(0, PROVINCE_DIGITS));
  }

  /**
   * Returns the plate with single spaces between the groups, as {@link #format(String)}.
   *
   * <pre>{@code
   * LicensePlate.of("34abc123").formatted(); // "34 ABC 123"
   * }</pre>
   *
   * @return the formatted plate
   */
  public String formatted() {
    return Chars.group(value, PROVINCE_DIGITS, letterCount);
  }

  /**
   * Returns the masked plate, as {@link #mask(String)}.
   *
   * <pre>{@code
   * LicensePlate.of("34 ABC 123").masked(); // "34 *** *23"
   * }</pre>
   *
   * @return the masked plate
   */
  public String masked() {
    int length = value.length();
    String masked =
        value.substring(0, PROVINCE_DIGITS)
            + Chars.stars(length - PROVINCE_DIGITS - MASK_TAIL)
            + value.substring(length - MASK_TAIL);
    return Chars.group(masked, PROVINCE_DIGITS, letterCount);
  }

  @Override
  public boolean equals(Object o) {
    return o instanceof LicensePlate other && value.equals(other.value);
  }

  @Override
  public int hashCode() {
    return value.hashCode();
  }

  /**
   * Returns the <strong>masked</strong> plate so that logging an instance does not leak it.
   *
   * @return the masked plate, e.g. {@code "34 *** *23"}
   */
  @Override
  public String toString() {
    return masked();
  }

  private static boolean isPlateLetter(char c) {
    if (!Chars.isLetter(c)) {
      return false;
    }
    char upper = Chars.toUpper(c);
    return upper != 'Q' && upper != 'W' && upper != 'X';
  }
}
