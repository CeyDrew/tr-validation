package io.github.ceydrew.trvalidation.examples.customer;

import io.github.ceydrew.trvalidation.jakarta.Tckn;
import io.github.ceydrew.trvalidation.jakarta.TrIban;
import io.github.ceydrew.trvalidation.jakarta.TrPhone;
import io.github.ceydrew.trvalidation.jakarta.TrPlate;
import io.github.ceydrew.trvalidation.jakarta.TrPostalCode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Registration of a policy holder.
 *
 * @param fullName name and surname
 * @param identityNumber TCKN, or YKN for foreign residents
 * @param gsm mobile number for SMS notifications
 * @param iban optional account for claim payments
 * @param plate optional vehicle plate for a motor policy (kasko/trafik)
 * @param postalCode postal code of the home address
 */
public record CustomerRequest(
    @NotBlank @Size(max = 100) String fullName,
    @NotBlank @Tckn(allowYkn = true) String identityNumber,
    @NotBlank @TrPhone(mobileOnly = true) String gsm,
    @TrIban String iban,
    @TrPlate String plate,
    @NotBlank @TrPostalCode String postalCode) {}
