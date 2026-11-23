package io.github.ceydrew.trvalidation.jakarta.internal;

import io.github.ceydrew.trvalidation.CardNetwork;
import io.github.ceydrew.trvalidation.jakarta.CardNumber;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/** Validates {@link CardNumber}. Internal; see the package documentation. */
public final class CardNumberValidator implements ConstraintValidator<CardNumber, CharSequence> {

  /** Accepted networks; empty accepts any valid number. */
  private Set<CardNetwork> networks = EnumSet.noneOf(CardNetwork.class);

  /** Creates the validator; called by the Bean Validation provider. */
  public CardNumberValidator() {}

  @Override
  public void initialize(CardNumber annotation) {
    networks = EnumSet.noneOf(CardNetwork.class);
    networks.addAll(List.of(annotation.networks()));
  }

  @Override
  public boolean isValid(CharSequence value, ConstraintValidatorContext context) {
    if (value == null) {
      return true;
    }
    String s = value.toString();
    if (networks.isEmpty()) {
      return io.github.ceydrew.trvalidation.CardNumber.isValid(s);
    }
    return io.github.ceydrew.trvalidation.CardNumber.network(s)
        .filter(networks::contains)
        .isPresent();
  }
}
