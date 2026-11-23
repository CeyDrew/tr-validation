package io.github.ceydrew.trvalidation.examples.customer;

import io.github.ceydrew.trvalidation.Iban;
import io.github.ceydrew.trvalidation.LicensePlate;
import io.github.ceydrew.trvalidation.PhoneNumber;
import io.github.ceydrew.trvalidation.PostalCode;

/**
 * A registered policy holder. Validated input is converted to tr-validation value types, which
 * store normalized values (E.164 phone, upper-case IBAN without spaces) and print masked.
 */
record Customer(
    long id,
    String fullName,
    String identityNumber,
    PhoneNumber gsm,
    Iban iban,
    LicensePlate plate,
    PostalCode postalCode) {

  static Customer from(long id, CustomerRequest request) {
    return new Customer(
        id,
        request.fullName().strip(),
        io.github.ceydrew.trvalidation.Tckn.normalize(request.identityNumber()),
        PhoneNumber.of(request.gsm()),
        request.iban() == null ? null : Iban.of(request.iban()),
        request.plate() == null ? null : LicensePlate.of(request.plate()),
        PostalCode.of(request.postalCode()));
  }
}
