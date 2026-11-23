package io.github.ceydrew.trvalidation.examples.customer;

import io.github.ceydrew.trvalidation.Iban;
import io.github.ceydrew.trvalidation.LicensePlate;
import io.github.ceydrew.trvalidation.PhoneNumber;
import io.github.ceydrew.trvalidation.PostalCode;
import io.github.ceydrew.trvalidation.jackson.MaskType;
import io.github.ceydrew.trvalidation.jackson.Masked;

/**
 * API view of a customer. Personal data never leaves the service in full:
 *
 * <ul>
 *   <li>{@code identityNumber} is a plain string, masked by {@link Masked};
 *   <li>{@code gsm}, {@code iban} and {@code plate} are value types, masked by the starter's
 *       Jackson module.
 * </ul>
 */
public record CustomerResponse(
    long id,
    String fullName,
    @Masked(MaskType.TCKN) String identityNumber,
    PhoneNumber gsm,
    Iban iban,
    LicensePlate plate,
    PostalCode postalCode) {

  static CustomerResponse of(Customer customer) {
    return new CustomerResponse(
        customer.id(),
        customer.fullName(),
        customer.identityNumber(),
        customer.gsm(),
        customer.iban(),
        customer.plate(),
        customer.postalCode());
  }
}
