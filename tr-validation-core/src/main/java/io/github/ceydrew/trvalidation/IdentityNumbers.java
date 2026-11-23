package io.github.ceydrew.trvalidation;

import java.util.random.RandomGenerator;

/** Shared TCKN/YKN algorithm; both use the same 11-digit checksum. */
final class IdentityNumbers {

  static final int LENGTH = 11;

  private IdentityNumbers() {}

  /**
   * Validates {@code input} in a single pass without allocating.
   *
   * @param foreign {@code true} for YKN (must start with 99), {@code false} for TCKN (must not)
   */
  static ValidationResult validate(String input, boolean foreign) {
    if (input == null) {
      return ValidationResult.invalid(InvalidReason.EMPTY);
    }
    int count = 0;
    int oddSum = 0; // digits 1, 3, 5, 7, 9
    int evenSum = 0; // digits 2, 4, 6, 8
    int first = 0;
    int second = 0;
    int tenth = 0;
    int eleventh = 0;
    for (int i = 0; i < input.length(); i++) {
      char c = input.charAt(i);
      if (Chars.isSeparator(c)) {
        continue;
      }
      if (!Chars.isDigit(c)) {
        return ValidationResult.invalid(InvalidReason.INVALID_CHARACTER);
      }
      int d = c - '0';
      count++;
      switch (count) {
        case 1 -> {
          first = d;
          oddSum += d;
        }
        case 2 -> {
          second = d;
          evenSum += d;
        }
        case 3, 5, 7, 9 -> oddSum += d;
        case 4, 6, 8 -> evenSum += d;
        case 10 -> tenth = d;
        case 11 -> eleventh = d;
        default -> {
          // more than 11 digits; keep scanning so invalid characters still win
        }
      }
    }
    if (count == 0) {
      return ValidationResult.invalid(InvalidReason.EMPTY);
    }
    if (count != LENGTH) {
      return ValidationResult.invalid(InvalidReason.WRONG_LENGTH);
    }
    boolean startsWith99 = first == 9 && second == 9;
    if (first == 0 || startsWith99 != foreign) {
      return ValidationResult.invalid(InvalidReason.INVALID_PREFIX);
    }
    if (tenth != tenthDigit(oddSum, evenSum) || eleventh != (oddSum + evenSum + tenth) % 10) {
      return ValidationResult.invalid(InvalidReason.CHECKSUM_MISMATCH);
    }
    return ValidationResult.valid();
  }

  /**
   * Generates a valid number.
   *
   * @param foreign {@code true} for a YKN (prefix 99), {@code false} for a TCKN
   */
  static String random(RandomGenerator rng, boolean foreign) {
    int[] d = new int[LENGTH];
    if (foreign) {
      d[0] = 9;
      d[1] = 9;
    } else {
      d[0] = 1 + rng.nextInt(9);
      // a TCKN must not start with 99, so the second digit avoids 9 when the first one is 9
      d[1] = rng.nextInt(d[0] == 9 ? 9 : 10);
    }
    for (int i = 2; i < 9; i++) {
      d[i] = rng.nextInt(10);
    }
    int oddSum = d[0] + d[2] + d[4] + d[6] + d[8];
    int evenSum = d[1] + d[3] + d[5] + d[7];
    d[9] = tenthDigit(oddSum, evenSum);
    d[10] = (oddSum + evenSum + d[9]) % 10;
    return Chars.digitsToString(d);
  }

  private static int tenthDigit(int oddSum, int evenSum) {
    return Math.floorMod(oddSum * 7 - evenSum, 10);
  }
}
