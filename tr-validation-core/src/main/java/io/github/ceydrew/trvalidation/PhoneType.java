package io.github.ceydrew.trvalidation;

/**
 * Kind of a Turkish phone number, derived from the first digits of the national number.
 *
 * <pre>{@code
 * PhoneNumber.type("0532 123 45 67"); // Optional[MOBILE]
 * PhoneNumber.type("0212 123 45 67"); // Optional[FIXED_LINE]
 * }</pre>
 *
 * <p>New constants may be added in minor releases; keep a {@code default} branch when switching.
 */
public enum PhoneType {
  /** Mobile (GSM) number: national number starts with {@code 5}. */
  MOBILE,
  /** Geographic fixed line: national number starts with {@code 2}, {@code 3} or {@code 4}. */
  FIXED_LINE,
  /**
   * Non-geographic number: {@code 800} (toll free), {@code 850} (corporate) or {@code 900} (premium
   * rate).
   */
  NON_GEOGRAPHIC
}
