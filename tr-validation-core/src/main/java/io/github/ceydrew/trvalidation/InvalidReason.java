package io.github.ceydrew.trvalidation;

/**
 * Why a value failed validation.
 *
 * <p>Shared by all validators in this library. New constants may be added in minor releases, so
 * code that switches over this enum should keep a {@code default} branch.
 *
 * <pre>{@code
 * String message = switch (reason) {
 *   case WRONG_LENGTH -> "TCKN must have 11 digits";
 *   case CHECKSUM_MISMATCH -> "TCKN check digits do not match";
 *   default -> "Invalid TCKN";
 * };
 * }</pre>
 */
public enum InvalidReason {
  /** The input is {@code null}, empty, or contains only separators. */
  EMPTY,
  /** The input contains a character that is neither allowed nor an ignorable separator. */
  INVALID_CHARACTER,
  /** The input has the wrong number of significant characters. */
  WRONG_LENGTH,
  /**
   * The input starts with a value that is not allowed, for example a TCKN starting with {@code 0}
   * or a YKN not starting with {@code 99}.
   */
  INVALID_PREFIX,
  /** The check digit(s) do not match the rest of the value. */
  CHECKSUM_MISMATCH,
  /**
   * The country code is unknown or not allowed, for example an IBAN with an unregistered country or
   * a phone number with a non-Turkish country code.
   */
  INVALID_COUNTRY_CODE,
  /** The Turkish province code is outside {@code 01-81}, as in a license plate or postal code. */
  INVALID_PROVINCE_CODE,
  /**
   * The characters are allowed but not in this arrangement, for example a license plate with four
   * letters or an IBAN whose check digits are letters.
   */
  INVALID_FORMAT
}
