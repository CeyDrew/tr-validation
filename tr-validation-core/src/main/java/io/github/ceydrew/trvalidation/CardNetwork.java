package io.github.ceydrew.trvalidation;

/**
 * Payment card network, detected from the number's prefix (IIN) and length.
 *
 * <pre>{@code
 * CardNumber.network("4111 1111 1111 1111"); // VISA
 * CardNumber.network("9792 0000 0000 0001"); // TROY (if the checksum is valid)
 * }</pre>
 *
 * <p>New networks may be added in minor releases; keep a {@code default} branch when switching.
 */
public enum CardNetwork {
  /** Visa: prefix {@code 4}, 13, 16 or 19 digits. */
  VISA,
  /** Mastercard: prefix {@code 51-55} or {@code 2221-2720}, 16 digits. */
  MASTERCARD,
  /** American Express: prefix {@code 34} or {@code 37}, 15 digits. */
  AMEX,
  /** TROY, the Turkish domestic network: prefix {@code 9792}, 16 digits. */
  TROY,
  /** A valid card number from a network this library does not recognize. */
  UNKNOWN
}
