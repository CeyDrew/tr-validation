/**
 * Jakarta Bean Validation constraints for Turkey-specific identifiers, backed by {@code
 * tr-validation-core}.
 *
 * <pre>{@code
 * record CustomerRequest(
 *     @NotNull @Tckn String tckn,
 *     @TrIban String iban,
 *     @TrPhone(mobileOnly = true) String gsm,
 *     @TrPlate String plate) {}
 * }</pre>
 *
 * <p>All constraints apply to {@link java.lang.CharSequence} and treat {@code null} as valid.
 *
 * <p>Messages ship in English and Turkish as {@code ContributorValidationMessages.properties} and
 * {@code ContributorValidationMessages_tr.properties}, which Hibernate Validator merges with the
 * application's own bundles. Keys follow {@code
 * io.github.ceydrew.trvalidation.jakarta.<Name>.message} and can be overridden in the application's
 * {@code ValidationMessages.properties}.
 */
package io.github.ceydrew.trvalidation.jakarta;
