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

  /** Removes all separators; returns {@code null} for {@code null}. */
  static String stripSeparators(String input) {
    if (input == null) {
      return null;
    }
    StringBuilder sb = new StringBuilder(input.length());
    for (int i = 0; i < input.length(); i++) {
      char c = input.charAt(i);
      if (!isSeparator(c)) {
        sb.append(c);
      }
    }
    return sb.toString();
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
