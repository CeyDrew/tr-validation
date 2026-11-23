package io.github.ceydrew.trvalidation;

import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.random.RandomGenerator;

/**
 * Turkish postal code (posta kodu): 5 digits, the first two being a province code {@code 01-81}.
 *
 * <p>This checks the structure only; it does not know which codes are actually assigned.
 *
 * <pre>{@code
 * PostalCode.isValid("34710");       // true
 * PostalCode.validate("99100");      // Invalid(INVALID_PROVINCE_CODE)
 * PostalCode.of("06100").provinceCode(); // 6
 * PostalCode.mask("34710");          // "34***"
 * }</pre>
 *
 * <p>Instances are immutable and thread-safe; all static methods are stateless.
 */
public final class PostalCode {

  private static final String TYPE = "postal code";
  private static final int LENGTH = 5;
  private static final int MASK_HEAD = 2;

  private final String value;

  private PostalCode(String value) {
    this.value = value;
  }

  /**
   * Tells whether {@code input} is a valid postal code.
   *
   * <pre>{@code
   * PostalCode.isValid("34710"); // true
   * PostalCode.isValid("3471");  // false
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
   * PostalCode.validate("34710");  // Valid
   * PostalCode.validate("3471A");  // Invalid(INVALID_CHARACTER)
   * PostalCode.validate("347100"); // Invalid(WRONG_LENGTH)
   * PostalCode.validate("00100");  // Invalid(INVALID_PROVINCE_CODE)
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
    int province = 0;
    for (int i = 0; i < input.length(); i++) {
      char c = input.charAt(i);
      if (Chars.isSeparator(c)) {
        continue;
      }
      if (!Chars.isDigit(c)) {
        return ValidationResult.invalid(InvalidReason.INVALID_CHARACTER);
      }
      if (count < 2) {
        province = province * 10 + (c - '0');
      }
      count++;
    }
    if (count == 0) {
      return ValidationResult.invalid(InvalidReason.EMPTY);
    }
    if (count != LENGTH) {
      return ValidationResult.invalid(InvalidReason.WRONG_LENGTH);
    }
    return Provinces.isValid(province)
        ? ValidationResult.valid()
        : ValidationResult.invalid(InvalidReason.INVALID_PROVINCE_CODE);
  }

  /**
   * Removes separators. Does not validate.
   *
   * <pre>{@code
   * PostalCode.normalize(" 34 710 "); // "34710"
   * }</pre>
   *
   * @param input the raw value, may be {@code null}
   * @return the normalized value, or {@code null} if {@code input} is {@code null}
   */
  public static String normalize(String input) {
    return Chars.stripSeparators(input);
  }

  /**
   * Masks a postal code, keeping the province part. Input without 5 significant characters is
   * masked completely.
   *
   * <pre>{@code
   * PostalCode.mask("34710"); // "34***"
   * PostalCode.mask("347");   // "***"
   * }</pre>
   *
   * @param input the raw value, may be {@code null}
   * @return the masked value, or {@code null} if {@code input} is {@code null}
   */
  public static String mask(String input) {
    String normalized = normalize(input);
    return normalized == null ? null : Chars.mask(normalized, LENGTH, MASK_HEAD, 0);
  }

  /**
   * Creates a postal code, rejecting invalid input.
   *
   * <pre>{@code
   * PostalCode code = PostalCode.of("34710");
   * PostalCode.of("99999"); // throws InvalidValueException(INVALID_PROVINCE_CODE)
   * }</pre>
   *
   * @param input the candidate, may be {@code null}
   * @return the postal code
   * @throws InvalidValueException if {@code input} is not valid
   */
  public static PostalCode of(String input) {
    ValidationResult result = validate(input);
    if (result instanceof ValidationResult.Invalid invalid) {
      throw new InvalidValueException(TYPE, invalid.reason());
    }
    return new PostalCode(normalize(input));
  }

  /**
   * Creates a postal code if {@code input} is valid.
   *
   * <pre>{@code
   * PostalCode.parse("34710"); // Optional[34***]
   * PostalCode.parse("abc");   // Optional.empty
   * }</pre>
   *
   * @param input the candidate, may be {@code null}
   * @return the postal code, or empty if invalid
   */
  public static Optional<PostalCode> parse(String input) {
    return isValid(input) ? Optional.of(new PostalCode(normalize(input))) : Optional.empty();
  }

  /**
   * Generates a random, structurally valid postal code.
   *
   * <p><strong>For tests and fixtures only.</strong> The code may not be assigned to any area.
   *
   * <pre>{@code
   * PostalCode fixture = PostalCode.random();
   * }</pre>
   *
   * @return a valid postal code
   */
  public static PostalCode random() {
    return random(ThreadLocalRandom.current());
  }

  /**
   * Generates a random, structurally valid postal code using {@code rng}, for reproducible tests.
   *
   * <p><strong>For tests and fixtures only.</strong>
   *
   * <pre>{@code
   * PostalCode fixture = PostalCode.random(new SplittableRandom(42));
   * }</pre>
   *
   * @param rng the random source, not {@code null}
   * @return a valid postal code
   */
  public static PostalCode random(RandomGenerator rng) {
    String province = Provinces.format(rng.nextInt(Provinces.MIN, Provinces.MAX + 1));
    return new PostalCode(province + String.format(Locale.ROOT, "%03d", rng.nextInt(1000)));
  }

  /**
   * Returns the 5-digit postal code.
   *
   * <pre>{@code
   * PostalCode.of(" 34 710 ").value(); // "34710"
   * }</pre>
   *
   * @return the postal code
   */
  public String value() {
    return value;
  }

  /**
   * Returns the province code.
   *
   * <pre>{@code
   * PostalCode.of("06100").provinceCode(); // 6
   * }</pre>
   *
   * @return the province code, {@code 1-81}
   */
  public int provinceCode() {
    return Integer.parseInt(value.substring(0, 2));
  }

  /**
   * Returns the masked postal code, as {@link #mask(String)}.
   *
   * <pre>{@code
   * PostalCode.of("34710").masked(); // "34***"
   * }</pre>
   *
   * @return the masked postal code
   */
  public String masked() {
    return mask(value);
  }

  @Override
  public boolean equals(Object o) {
    return o instanceof PostalCode other && value.equals(other.value);
  }

  @Override
  public int hashCode() {
    return value.hashCode();
  }

  /**
   * Returns the postal code itself; it is not personal data on its own.
   *
   * @return the postal code
   */
  @Override
  public String toString() {
    return value;
  }
}
