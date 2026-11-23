package io.github.ceydrew.trvalidation;

/** Turkish province (il) codes, used by license plates and postal codes. */
final class Provinces {

  static final int MIN = 1;
  static final int MAX = 81;

  private Provinces() {}

  static boolean isValid(int code) {
    return code >= MIN && code <= MAX;
  }

  /** Two-digit, zero-padded form: {@code 6} to {@code "06"}. */
  static String format(int code) {
    return code < 10 ? "0" + code : Integer.toString(code);
  }
}
