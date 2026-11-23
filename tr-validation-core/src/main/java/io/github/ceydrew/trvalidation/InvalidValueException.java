package io.github.ceydrew.trvalidation;

/**
 * Thrown by factory methods such as {@link Tckn#of(String)} when the input is not valid.
 *
 * <p>The message names the type and the {@link InvalidReason} but <strong>never contains the
 * rejected value</strong>, so it is safe to log without leaking personal data.
 *
 * <pre>{@code
 * try {
 *   Tckn.of(input);
 * } catch (InvalidValueException e) {
 *   e.getMessage(); // "Invalid TCKN: CHECKSUM_MISMATCH"
 *   e.reason();     // CHECKSUM_MISMATCH
 * }
 * }</pre>
 */
public final class InvalidValueException extends IllegalArgumentException {

  private static final long serialVersionUID = 1L;

  private final String type;
  private final InvalidReason reason;

  InvalidValueException(String type, InvalidReason reason) {
    super("Invalid " + type + ": " + reason);
    this.type = type;
    this.reason = reason;
  }

  /**
   * Returns the short name of the rejected type.
   *
   * <pre>{@code
   * e.type(); // "TCKN"
   * }</pre>
   *
   * @return the type name, e.g. {@code "TCKN"} or {@code "VKN"}
   */
  public String type() {
    return type;
  }

  /**
   * Returns why the value was rejected.
   *
   * <pre>{@code
   * e.reason(); // WRONG_LENGTH
   * }</pre>
   *
   * @return the reason, never {@code null}
   */
  public InvalidReason reason() {
    return reason;
  }
}
