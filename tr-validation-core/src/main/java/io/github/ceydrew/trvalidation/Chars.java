package io.github.ceydrew.trvalidation;

/** Character helpers shared by the validators. */
final class Chars {

  private Chars() {}

  /** Separators ignored anywhere in lenient input: whitespace, no-break space and hyphen. */
  static boolean isSeparator(char c) {
    return c == ' ' || c == '-' || c == '\t' || c == ' ' || c == '\n' || c == '\r';
  }

  /** ASCII digits only; Unicode digits such as Arabic-Indic numerals are rejected. */
  static boolean isDigit(char c) {
    return c >= '0' && c <= '9';
  }

  /** ASCII letters only, either case. */
  static boolean isLetter(char c) {
    return (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z');
  }

  /** Upper-cases ASCII letters and leaves everything else unchanged (locale independent). */
  static char toUpper(char c) {
    return c >= 'a' && c <= 'z' ? (char) (c - ('a' - 'A')) : c;
  }

  /** Removes all separators; returns {@code null} for {@code null}. */
  static String stripSeparators(String input) {
    return strip(input, false);
  }

  /**
   * Removes all separators and upper-cases ASCII letters; returns {@code null} for {@code null}.
   */
  static String stripSeparatorsUpper(String input) {
    return strip(input, true);
  }

  private static String strip(String input, boolean upper) {
    if (input == null) {
      return null;
    }
    StringBuilder sb = new StringBuilder(input.length());
    for (int i = 0; i < input.length(); i++) {
      char c = input.charAt(i);
      if (!isSeparator(c)) {
        sb.append(upper ? toUpper(c) : c);
      }
    }
    return sb.toString();
  }

  /**
   * Inserts a space after every {@code size} characters: {@code "ABCDEFGHIJ"} to {@code "ABCD EFGH
   * IJ"}.
   */
  static String groupEvery(String value, int size) {
    StringBuilder sb = new StringBuilder(value.length() + value.length() / size);
    for (int i = 0; i < value.length(); i++) {
      if (i > 0 && i % size == 0) {
        sb.append(' ');
      }
      sb.append(value.charAt(i));
    }
    return sb.toString();
  }

  /**
   * Splits {@code value} into groups of the given sizes joined by spaces; the last group takes the
   * rest. {@code group("5321234567", 3, 3, 2)} gives {@code "532 123 45 67"}.
   */
  static String group(String value, int... sizes) {
    StringBuilder sb = new StringBuilder(value.length() + sizes.length);
    int start = 0;
    for (int size : sizes) {
      sb.append(value, start, start + size).append(' ');
      start += size;
    }
    return sb.append(value, start, value.length()).toString();
  }

  /** Returns a string of {@code length} asterisks. */
  static String stars(int length) {
    return "*".repeat(length);
  }

  /** Renders single-digit ints ({@code 0-9}) as a digit string. */
  static String digitsToString(int[] digits) {
    char[] out = new char[digits.length];
    for (int i = 0; i < digits.length; i++) {
      out[i] = (char) ('0' + digits[i]);
    }
    return new String(out);
  }

  /**
   * Keeps the first {@code head} and last {@code tail} characters of {@code value} when it has
   * exactly {@code expectedLength} characters; otherwise masks everything so that malformed input
   * never leaks.
   */
  static String mask(String value, int expectedLength, int head, int tail) {
    int length = value.length();
    char[] out = new char[length];
    boolean keepEnds = length == expectedLength;
    for (int i = 0; i < length; i++) {
      boolean keep = keepEnds && (i < head || i >= length - tail);
      out[i] = keep ? value.charAt(i) : '*';
    }
    return new String(out);
  }
}
