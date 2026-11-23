package io.github.ceydrew.trvalidation;

import java.util.Optional;

/**
 * Turkish phone number (mobile, fixed line or non-geographic), normalized to E.164.
 *
 * <p>Accepted forms of the 10-digit national number {@code 5321234567}: {@code +90 532 123 45 67},
 * {@code +90 (532) 123-45-67}, {@code 0090 532 123 45 67}, {@code 90 532 123 45 67}, {@code 0532
 * 123 45 67}, {@code (0532) 123 45 67}, {@code 532.123.45.67} and the common mistake {@code +90
 * 0532 123 45 67}. Spaces, tabs, hyphens, dots and parentheses are ignored; {@code +} is allowed
 * only at the start.
 *
 * <p>The national number must start with {@code 5} (mobile), {@code 2}/{@code 3}/{@code 4} (fixed
 * line) or be {@code 800}/{@code 850}/{@code 900} (non-geographic); see {@link PhoneType}. Short
 * numbers such as {@code 444 xx xx} and emergency numbers are not supported.
 *
 * <pre>{@code
 * PhoneNumber.isValid("0532 123 45 67");     // true
 * PhoneNumber.normalize("(0532) 123-45-67"); // "+905321234567"
 * PhoneNumber.format("05321234567");         // "+90 532 123 45 67"
 * PhoneNumber.mask("05321234567");           // "+90 532 *** ** 67"
 * PhoneNumber.type("0212 123 45 67");        // Optional[FIXED_LINE]
 * }</pre>
 *
 * <p>Unlike the other types there is no {@code random()}: Türkiye has no reserved fictional number
 * range, so a generated number could reach a real person.
 *
 * <p>Instances are immutable and thread-safe; all static methods are stateless.
 */
public final class PhoneNumber {

  private static final String TYPE = "phone number";
  private static final String COUNTRY_PREFIX = "+90";
  private static final int NATIONAL_LENGTH = 10;

  /** Leading digits kept while scanning: enough for {@code 0090}, a trunk {@code 0} and 3 more. */
  private static final int HEAD_DIGITS = 8;

  private static final int[] POW10 = {1, 10, 100, 1_000, 10_000, 100_000, 1_000_000, 10_000_000};
  private static final InvalidReason[] REASONS = InvalidReason.values();
  private static final PhoneType[] TYPES = PhoneType.values();

  private final String value;
  private final PhoneType type;

  private PhoneNumber(String value, PhoneType type) {
    this.value = value;
    this.type = type;
  }

  /**
   * Tells whether {@code input} is a valid Turkish phone number.
   *
   * <pre>{@code
   * PhoneNumber.isValid("+90 532 123 45 67"); // true
   * PhoneNumber.isValid("+1 202 555 0100");   // false
   * }</pre>
   *
   * @param input the candidate, may be {@code null}
   * @return {@code true} if valid
   */
  public static boolean isValid(String input) {
    return analyze(input) >= 0;
  }

  /**
   * Validates {@code input} and explains a failure. Does not allocate.
   *
   * <pre>{@code
   * PhoneNumber.validate("0532 123 45 67");  // Valid
   * PhoneNumber.validate("+44 20 7946 0000"); // Invalid(INVALID_COUNTRY_CODE)
   * PhoneNumber.validate("0532 123 45");      // Invalid(WRONG_LENGTH)
   * PhoneNumber.validate("0123 456 78 90");   // Invalid(INVALID_PREFIX)
   * }</pre>
   *
   * @param input the candidate, may be {@code null}
   * @return the result, never {@code null}
   */
  public static ValidationResult validate(String input) {
    int code = analyze(input);
    return code >= 0 ? ValidationResult.valid() : ValidationResult.invalid(REASONS[-code - 1]);
  }

  /**
   * Returns the kind of a valid phone number.
   *
   * <pre>{@code
   * PhoneNumber.type("0532 123 45 67"); // Optional[MOBILE]
   * PhoneNumber.type("0850 123 45 67"); // Optional[NON_GEOGRAPHIC]
   * PhoneNumber.type("123");            // Optional.empty
   * }</pre>
   *
   * @param input the candidate, may be {@code null}
   * @return the type, or empty if {@code input} is not valid
   */
  public static Optional<PhoneType> type(String input) {
    int code = analyze(input);
    return code >= 0 ? Optional.of(TYPES[code / 8]) : Optional.empty();
  }

  /**
   * Converts a valid phone number to E.164.
   *
   * <p>Unlike the other types' {@code normalize}, this validates: a phone number can only be put
   * into E.164 once its country code and trunk prefix are understood.
   *
   * <pre>{@code
   * PhoneNumber.normalize("0532 123 45 67");   // "+905321234567"
   * PhoneNumber.normalize("0090 212 123 4567"); // "+902121234567"
   * PhoneNumber.normalize("12345");            // null
   * }</pre>
   *
   * @param input the raw value, may be {@code null}
   * @return the E.164 form, or {@code null} if {@code input} is not valid
   */
  public static String normalize(String input) {
    int code = analyze(input);
    return code >= 0 ? COUNTRY_PREFIX + nationalNumber(input, code % 8) : null;
  }

  /**
   * Formats a valid phone number for display.
   *
   * <pre>{@code
   * PhoneNumber.format("05321234567"); // "+90 532 123 45 67"
   * PhoneNumber.format("12345");       // null
   * }</pre>
   *
   * @param input the raw value, may be {@code null}
   * @return the formatted number, or {@code null} if {@code input} is not valid
   */
  public static String format(String input) {
    return parse(input).map(PhoneNumber::formatted).orElse(null);
  }

  /**
   * Masks a phone number for display or logging, keeping the operator or area code and the last two
   * digits. Invalid input is masked completely.
   *
   * <pre>{@code
   * PhoneNumber.mask("0532 123 45 67"); // "+90 532 *** ** 67"
   * PhoneNumber.mask("12345");          // "*****"
   * }</pre>
   *
   * @param input the raw value, may be {@code null}
   * @return the masked value, or {@code null} if {@code input} is {@code null}
   */
  public static String mask(String input) {
    if (input == null) {
      return null;
    }
    Optional<PhoneNumber> phone = parse(input);
    return phone.isPresent()
        ? phone.get().masked()
        : Chars.stars(Chars.stripSeparators(input).length());
  }

  /**
   * Creates a phone number, rejecting invalid input.
   *
   * <pre>{@code
   * PhoneNumber phone = PhoneNumber.of("0532 123 45 67");
   * phone.value(); // "+905321234567"
   * phone.type();  // MOBILE
   * }</pre>
   *
   * @param input the candidate, may be {@code null}
   * @return the phone number
   * @throws InvalidValueException if {@code input} is not valid; the message never contains the
   *     input
   */
  public static PhoneNumber of(String input) {
    int code = analyze(input);
    if (code < 0) {
      throw new InvalidValueException(TYPE, REASONS[-code - 1]);
    }
    return create(input, code);
  }

  /**
   * Creates a phone number if {@code input} is valid.
   *
   * <pre>{@code
   * PhoneNumber.parse("+90 532 123 45 67"); // Optional[+90 532 *** ** 67]
   * PhoneNumber.parse("911");               // Optional.empty
   * }</pre>
   *
   * @param input the candidate, may be {@code null}
   * @return the phone number, or empty if invalid
   */
  public static Optional<PhoneNumber> parse(String input) {
    int code = analyze(input);
    return code >= 0 ? Optional.of(create(input, code)) : Optional.empty();
  }

  /**
   * Returns the number in E.164 form. Treat it as personal data.
   *
   * <pre>{@code
   * PhoneNumber.of("0532 123 45 67").value(); // "+905321234567"
   * }</pre>
   *
   * @return the E.164 number
   */
  public String value() {
    return value;
  }

  /**
   * Returns the kind of number.
   *
   * <pre>{@code
   * PhoneNumber.of("0312 123 45 67").type(); // FIXED_LINE
   * }</pre>
   *
   * @return the type
   */
  public PhoneType type() {
    return type;
  }

  /**
   * Returns the number formatted for display, as {@link #format(String)}.
   *
   * <pre>{@code
   * PhoneNumber.of("05321234567").formatted(); // "+90 532 123 45 67"
   * }</pre>
   *
   * @return the formatted number
   */
  public String formatted() {
    return COUNTRY_PREFIX + " " + Chars.group(value.substring(3), 3, 3, 2);
  }

  /**
   * Returns the masked number, as {@link #mask(String)}.
   *
   * <pre>{@code
   * PhoneNumber.of("05321234567").masked(); // "+90 532 *** ** 67"
   * }</pre>
   *
   * @return the masked number
   */
  public String masked() {
    return COUNTRY_PREFIX + " " + value.substring(3, 6) + " *** ** " + value.substring(11);
  }

  @Override
  public boolean equals(Object o) {
    return o instanceof PhoneNumber other && value.equals(other.value);
  }

  @Override
  public int hashCode() {
    return value.hashCode();
  }

  /**
   * Returns the <strong>masked</strong> number so that logging an instance does not leak it.
   *
   * @return the masked number, e.g. {@code "+90 532 *** ** 67"}
   */
  @Override
  public String toString() {
    return masked();
  }

  private static PhoneNumber create(String input, int code) {
    return new PhoneNumber(COUNTRY_PREFIX + nationalNumber(input, code % 8), TYPES[code / 8]);
  }

  /**
   * Scans {@code input} once without allocating.
   *
   * @return {@code type.ordinal() * 8 + prefixDigits} if valid, where {@code prefixDigits} is the
   *     number of leading digits before the national number; otherwise {@code -(reason.ordinal() +
   *     1)}
   */
  private static int analyze(String input) {
    if (input == null) {
      return fail(InvalidReason.EMPTY);
    }
    boolean plus = false;
    int count = 0;
    int head = 0;
    for (int i = 0; i < input.length(); i++) {
      char c = input.charAt(i);
      if (isIgnorable(c)) {
        continue;
      }
      if (c == '+') {
        if (plus || count > 0) {
          return fail(InvalidReason.INVALID_CHARACTER);
        }
        plus = true;
        continue;
      }
      if (!Chars.isDigit(c)) {
        return fail(InvalidReason.INVALID_CHARACTER);
      }
      if (count < HEAD_DIGITS) {
        head = head * 10 + (c - '0');
      }
      count++;
    }
    if (count == 0) {
      return fail(InvalidReason.EMPTY);
    }
    if (count < NATIONAL_LENGTH) {
      return fail(InvalidReason.WRONG_LENGTH);
    }
    // from here on head holds exactly HEAD_DIGITS digits
    int prefix;
    if (plus) {
      if (digit(head, 0) != 9 || digit(head, 1) != 0) {
        return fail(InvalidReason.INVALID_COUNTRY_CODE);
      }
      prefix = 2;
    } else if (digit(head, 0) == 0 && digit(head, 1) == 0) {
      if (digit(head, 2) != 9 || digit(head, 3) != 0) {
        return fail(InvalidReason.INVALID_COUNTRY_CODE);
      }
      prefix = 4;
    } else if (count == NATIONAL_LENGTH + 2 && digit(head, 0) == 9 && digit(head, 1) == 0) {
      prefix = 2;
    } else if (digit(head, 0) == 0) {
      prefix = 1;
    } else {
      prefix = 0;
    }
    // tolerate a trunk 0 after the country code: +90 0532 ...
    if (prefix >= 2 && count - prefix == NATIONAL_LENGTH + 1 && digit(head, prefix) == 0) {
      prefix++;
    }
    if (count - prefix != NATIONAL_LENGTH) {
      return fail(InvalidReason.WRONG_LENGTH);
    }
    int leading3 = head / POW10[HEAD_DIGITS - prefix - 3] % 1000;
    PhoneType type = typeOf(leading3);
    if (type == null) {
      return fail(InvalidReason.INVALID_PREFIX);
    }
    return type.ordinal() * 8 + prefix;
  }

  private static PhoneType typeOf(int leading3) {
    switch (leading3 / 100) {
      case 5:
        return PhoneType.MOBILE;
      case 2:
      case 3:
      case 4:
        return PhoneType.FIXED_LINE;
      default:
        return leading3 == 800 || leading3 == 850 || leading3 == 900
            ? PhoneType.NON_GEOGRAPHIC
            : null;
    }
  }

  /** Digits of {@code input} after the first {@code prefix} digits. */
  private static String nationalNumber(String input, int prefix) {
    StringBuilder sb = new StringBuilder(NATIONAL_LENGTH);
    int seen = 0;
    for (int i = 0; i < input.length(); i++) {
      char c = input.charAt(i);
      if (Chars.isDigit(c) && seen++ >= prefix) {
        sb.append(c);
      }
    }
    return sb.toString();
  }

  private static boolean isIgnorable(char c) {
    return Chars.isSeparator(c) || c == '(' || c == ')' || c == '.';
  }

  /** The {@code index}-th (0-based) of the {@link #HEAD_DIGITS} leading digits. */
  private static int digit(int head, int index) {
    return head / POW10[HEAD_DIGITS - 1 - index] % 10;
  }

  private static int fail(InvalidReason reason) {
    return -(reason.ordinal() + 1);
  }
}
