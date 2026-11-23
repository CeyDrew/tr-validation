package io.github.ceydrew.trvalidation;

import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.random.RandomGenerator;

/**
 * Payment card number (PAN): 12 to 19 digits passing the Luhn checksum, with network detection.
 *
 * <p>Validation does not depend on the network: a Luhn-valid number from an unrecognized network is
 * valid and reported as {@link CardNetwork#UNKNOWN}. Expiry dates and CVVs are out of scope.
 *
 * <p>Static methods accept lenient input: spaces, tabs, no-break spaces and hyphens are ignored.
 *
 * <pre>{@code
 * CardNumber.isValid("4111 1111 1111 1111");  // true
 * CardNumber.network("4111 1111 1111 1111");  // Optional[VISA]
 * CardNumber.format("378282246310005");       // "3782 822463 10005"
 * CardNumber.mask("4111111111111111");        // "**** **** **** 1111"
 * }</pre>
 *
 * <p>Instances are immutable and thread-safe; all static methods are stateless. Card numbers are
 * sensitive (PCI DSS): log only {@link #masked()} or {@link #toString()}.
 */
public final class CardNumber {

  private static final String TYPE = "card number";
  private static final int MIN_LENGTH = 12;
  private static final int MAX_LENGTH = 19;
  private static final int MASK_TAIL = 4;

  private final String value;
  private final CardNetwork network;

  private CardNumber(String value) {
    this.value = value;
    this.network = detect(value);
  }

  /**
   * Tells whether {@code input} is a valid card number.
   *
   * <pre>{@code
   * CardNumber.isValid("4111 1111 1111 1111"); // true
   * CardNumber.isValid("4111 1111 1111 1112"); // false
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
   * CardNumber.validate("4111111111111111"); // Valid
   * CardNumber.validate("41111111111");      // Invalid(WRONG_LENGTH)
   * CardNumber.validate("4111111111111112"); // Invalid(CHECKSUM_MISMATCH)
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
    int sumDoublingEven = 0; // Luhn sum if digits at even positions (from the left) are doubled
    int sumDoublingOdd = 0; // Luhn sum if digits at odd positions are doubled
    for (int i = 0; i < input.length(); i++) {
      char c = input.charAt(i);
      if (Chars.isSeparator(c)) {
        continue;
      }
      if (!Chars.isDigit(c)) {
        return ValidationResult.invalid(InvalidReason.INVALID_CHARACTER);
      }
      int d = c - '0';
      if (count % 2 == 0) {
        sumDoublingEven += doubled(d);
        sumDoublingOdd += d;
      } else {
        sumDoublingEven += d;
        sumDoublingOdd += doubled(d);
      }
      count++;
    }
    if (count == 0) {
      return ValidationResult.invalid(InvalidReason.EMPTY);
    }
    if (count < MIN_LENGTH || count > MAX_LENGTH) {
      return ValidationResult.invalid(InvalidReason.WRONG_LENGTH);
    }
    // Luhn doubles every second digit counting from the right, i.e. positions with the length's
    // parity counting from the left
    int sum = count % 2 == 0 ? sumDoublingEven : sumDoublingOdd;
    return sum % 10 == 0
        ? ValidationResult.valid()
        : ValidationResult.invalid(InvalidReason.CHECKSUM_MISMATCH);
  }

  /**
   * Detects the network of a valid card number.
   *
   * <pre>{@code
   * CardNumber.network("5555 5555 5555 4444"); // Optional[MASTERCARD]
   * CardNumber.network("6011 1111 1111 1117"); // Optional[UNKNOWN]
   * CardNumber.network("1234");                // Optional.empty
   * }</pre>
   *
   * @param input the candidate, may be {@code null}
   * @return the network, or empty if {@code input} is not a valid card number
   */
  public static Optional<CardNetwork> network(String input) {
    return parse(input).map(CardNumber::network);
  }

  /**
   * Removes separators (spaces, tabs, no-break spaces, hyphens). Does not validate.
   *
   * <pre>{@code
   * CardNumber.normalize("4111-1111-1111-1111"); // "4111111111111111"
   * }</pre>
   *
   * @param input the raw value, may be {@code null}
   * @return the normalized value, or {@code null} if {@code input} is {@code null}
   */
  public static String normalize(String input) {
    return Chars.stripSeparators(input);
  }

  /**
   * Formats a valid card number as printed on the card: 4-6-5 for American Express, groups of four
   * otherwise.
   *
   * <pre>{@code
   * CardNumber.format("4111111111111111"); // "4111 1111 1111 1111"
   * CardNumber.format("378282246310005");  // "3782 822463 10005"
   * CardNumber.format("123");              // null
   * }</pre>
   *
   * @param input the raw value, may be {@code null}
   * @return the formatted number, or {@code null} if {@code input} is not a valid card number
   */
  public static String format(String input) {
    return parse(input).map(CardNumber::formatted).orElse(null);
  }

  /**
   * Masks a card number for display or logging, keeping only the last four digits, grouped like
   * {@link #format(String)}. Input that does not have 12 to 19 significant characters is masked
   * completely.
   *
   * <pre>{@code
   * CardNumber.mask("4111111111111111"); // "**** **** **** 1111"
   * CardNumber.mask("378282246310005");  // "**** ****** *0005"
   * CardNumber.mask("1234");             // "****"
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
    if (length < MIN_LENGTH || length > MAX_LENGTH) {
      return Chars.stars(length);
    }
    String masked = Chars.mask(normalized, length, 0, MASK_TAIL);
    return isValid(normalized) && detect(normalized) == CardNetwork.AMEX
        ? Chars.group(masked, 4, 6)
        : Chars.groupEvery(masked, 4);
  }

  /**
   * Creates a card number, rejecting invalid input.
   *
   * <pre>{@code
   * CardNumber card = CardNumber.of("4111 1111 1111 1111");
   * card.network(); // VISA
   * }</pre>
   *
   * @param input the candidate, may be {@code null}
   * @return the card number
   * @throws InvalidValueException if {@code input} is not a valid card number; the message never
   *     contains the input
   */
  public static CardNumber of(String input) {
    ValidationResult result = validate(input);
    if (result instanceof ValidationResult.Invalid invalid) {
      throw new InvalidValueException(TYPE, invalid.reason());
    }
    return new CardNumber(normalize(input));
  }

  /**
   * Creates a card number if {@code input} is valid.
   *
   * <pre>{@code
   * CardNumber.parse("4111111111111111"); // Optional[**** **** **** 1111]
   * CardNumber.parse("4111111111111112"); // Optional.empty
   * }</pre>
   *
   * @param input the candidate, may be {@code null}
   * @return the card number, or empty if invalid
   */
  public static Optional<CardNumber> parse(String input) {
    return isValid(input) ? Optional.of(new CardNumber(normalize(input))) : Optional.empty();
  }

  /**
   * Generates a random, Luhn-valid card number of a random known network.
   *
   * <p><strong>For tests and fixtures only.</strong> The number is fictional; never use it for
   * payments.
   *
   * <pre>{@code
   * CardNumber fixture = CardNumber.random();
   * }</pre>
   *
   * @return a valid, fictional card number
   */
  public static CardNumber random() {
    RandomGenerator rng = ThreadLocalRandom.current();
    CardNetwork[] known = {
      CardNetwork.VISA, CardNetwork.MASTERCARD, CardNetwork.AMEX, CardNetwork.TROY
    };
    return random(known[rng.nextInt(known.length)], rng);
  }

  /**
   * Generates a random, Luhn-valid card number of {@code network}.
   *
   * <p><strong>For tests and fixtures only.</strong> The number is fictional.
   *
   * <pre>{@code
   * CardNumber troy = CardNumber.random(CardNetwork.TROY, new SplittableRandom(42));
   * troy.network(); // TROY
   * }</pre>
   *
   * @param network the network, not {@code null} and not {@link CardNetwork#UNKNOWN}
   * @param rng the random source, not {@code null}
   * @return a valid, fictional card number
   * @throws IllegalArgumentException if {@code network} is {@link CardNetwork#UNKNOWN}
   */
  public static CardNumber random(CardNetwork network, RandomGenerator rng) {
    int[] d;
    switch (network) {
      case VISA -> {
        d = new int[16];
        d[0] = 4;
        fill(d, 1, rng);
      }
      case MASTERCARD -> {
        d = new int[16];
        d[0] = 5;
        d[1] = 1 + rng.nextInt(5);
        fill(d, 2, rng);
      }
      case AMEX -> {
        d = new int[15];
        d[0] = 3;
        d[1] = rng.nextBoolean() ? 4 : 7;
        fill(d, 2, rng);
      }
      case TROY -> {
        d = new int[16];
        d[0] = 9;
        d[1] = 7;
        d[2] = 9;
        d[3] = 2;
        fill(d, 4, rng);
      }
      default -> throw new IllegalArgumentException("Cannot generate numbers for " + network);
    }
    int last = d.length - 1;
    int sum = 0;
    for (int k = 0; k < last; k++) {
      sum += k % 2 == d.length % 2 ? doubled(d[k]) : d[k];
    }
    d[last] = (10 - sum % 10) % 10;
    return new CardNumber(Chars.digitsToString(d));
  }

  /**
   * Returns the full card number. Treat it as sensitive data.
   *
   * <pre>{@code
   * CardNumber.of("4111 1111 1111 1111").value(); // "4111111111111111"
   * }</pre>
   *
   * @return the digits without separators
   */
  public String value() {
    return value;
  }

  /**
   * Returns the detected network.
   *
   * <pre>{@code
   * CardNumber.of("378282246310005").network(); // AMEX
   * }</pre>
   *
   * @return the network, {@link CardNetwork#UNKNOWN} if not recognized
   */
  public CardNetwork network() {
    return network;
  }

  /**
   * Returns the number as printed on the card, as {@link #format(String)}.
   *
   * <pre>{@code
   * CardNumber.of("4111111111111111").formatted(); // "4111 1111 1111 1111"
   * }</pre>
   *
   * @return the formatted number
   */
  public String formatted() {
    return network == CardNetwork.AMEX ? Chars.group(value, 4, 6) : Chars.groupEvery(value, 4);
  }

  /**
   * Returns the masked number, as {@link #mask(String)}.
   *
   * <pre>{@code
   * CardNumber.of("4111111111111111").masked(); // "**** **** **** 1111"
   * }</pre>
   *
   * @return the masked number
   */
  public String masked() {
    return mask(value);
  }

  @Override
  public boolean equals(Object o) {
    return o instanceof CardNumber other && value.equals(other.value);
  }

  @Override
  public int hashCode() {
    return value.hashCode();
  }

  /**
   * Returns the <strong>masked</strong> number so that logging an instance does not leak it.
   *
   * @return the masked number, e.g. {@code "**** **** **** 1111"}
   */
  @Override
  public String toString() {
    return masked();
  }

  /** Detects the network of a normalized, valid number. */
  private static CardNetwork detect(String digits) {
    int length = digits.length();
    int prefix = Integer.parseInt(digits.substring(0, 4));
    int prefix2 = prefix / 100;
    if ((prefix2 == 34 || prefix2 == 37) && length == 15) {
      return CardNetwork.AMEX;
    }
    if (prefix / 1000 == 4 && (length == 13 || length == 16 || length == 19)) {
      return CardNetwork.VISA;
    }
    boolean mastercardPrefix =
        (prefix2 >= 51 && prefix2 <= 55) || (prefix >= 2221 && prefix <= 2720);
    if (mastercardPrefix && length == 16) {
      return CardNetwork.MASTERCARD;
    }
    if (prefix == 9792 && length == 16) {
      return CardNetwork.TROY;
    }
    return CardNetwork.UNKNOWN;
  }

  private static int doubled(int d) {
    int x = d * 2;
    return x > 9 ? x - 9 : x;
  }

  private static void fill(int[] digits, int from, RandomGenerator rng) {
    for (int i = from; i < digits.length - 1; i++) {
      digits[i] = rng.nextInt(10);
    }
  }
}
