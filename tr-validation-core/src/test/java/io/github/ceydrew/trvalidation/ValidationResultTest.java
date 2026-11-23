package io.github.ceydrew.trvalidation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class ValidationResultTest {

  @Test
  void validIsSharedAndHasNoReason() {
    ValidationResult result = ValidationResult.valid();

    assertThat(result)
        .isSameAs(ValidationResult.valid())
        .isInstanceOf(ValidationResult.Valid.class);
    assertThat(result.isValid()).isTrue();
    assertThat(result.invalidReason()).isEmpty();
  }

  @ParameterizedTest
  @EnumSource(InvalidReason.class)
  void invalidIsCachedPerReason(InvalidReason reason) {
    ValidationResult result = ValidationResult.invalid(reason);

    assertThat(result).isSameAs(ValidationResult.invalid(reason));
    assertThat(result.isValid()).isFalse();
    assertThat(result.invalidReason()).contains(reason);
    assertThat(((ValidationResult.Invalid) result).reason()).isEqualTo(reason);
  }

  @Test
  void invalidEqualsByReason() {
    assertThat(new ValidationResult.Invalid(InvalidReason.EMPTY))
        .isEqualTo(ValidationResult.invalid(InvalidReason.EMPTY))
        .isNotEqualTo(ValidationResult.invalid(InvalidReason.WRONG_LENGTH));
  }

  @Test
  void invalidRejectsNullReason() {
    assertThatNullPointerException().isThrownBy(() -> new ValidationResult.Invalid(null));
    assertThatNullPointerException().isThrownBy(() -> ValidationResult.invalid(null));
  }
}
