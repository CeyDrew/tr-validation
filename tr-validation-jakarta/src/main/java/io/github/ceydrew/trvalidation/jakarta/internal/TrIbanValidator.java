package io.github.ceydrew.trvalidation.jakarta.internal;

import io.github.ceydrew.trvalidation.Iban;
import io.github.ceydrew.trvalidation.jakarta.TrIban;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/** Validates {@link TrIban}. Internal; see the package documentation. */
public final class TrIbanValidator implements ConstraintValidator<TrIban, CharSequence> {

  private boolean allowForeign;

  /** Creates the validator; called by the Bean Validation provider. */
  public TrIbanValidator() {}

  @Override
  public void initialize(TrIban annotation) {
    allowForeign = annotation.allowForeign();
  }

  @Override
  public boolean isValid(CharSequence value, ConstraintValidatorContext context) {
    if (value == null) {
      return true;
    }
    String s = value.toString();
    return allowForeign ? Iban.isValid(s) : Iban.isValidTr(s);
  }
}
