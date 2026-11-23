package io.github.ceydrew.trvalidation;

/** IBAN length per country, from the SWIFT IBAN registry. */
final class IbanCountries {

  /** Country code followed by IBAN length, four characters per entry. */
  private static final String REGISTRY =
      "AD24AE23AL28AT20AZ28BA20BE16BG22BH22BI27BR29BY28CH21CR22CY28CZ24DE22DJ27DK18DO28"
          + "EE20EG29ES24FI18FK18FO18FR27GB22GE22GI23GL18GR27GT28HN28HR21HU28IE22IL23IQ23IS26"
          + "IT27JO30KW30KZ20LB28LC32LI21LT20LU20LV21LY25MC27MD24ME22MK19MN20MR27MT31MU30NI28"
          + "NL18NO15OM23PK24PL28PS29QA29RO24RS22RU33SA24SC31SD18SE24SI19SK24SM27SO23ST25SV28"
          + "TL23TN24TR26UA29VA22VG24XK20YE30";

  private static final byte[] LENGTHS = createTable();

  private IbanCountries() {}

  /**
   * Returns the IBAN length for the upper-case country code, or {@code 0} if the country does not
   * use IBAN.
   */
  static int lengthOf(char first, char second) {
    return LENGTHS[(first - 'A') * 26 + (second - 'A')];
  }

  private static byte[] createTable() {
    byte[] table = new byte[26 * 26];
    for (int i = 0; i < REGISTRY.length(); i += 4) {
      int index = (REGISTRY.charAt(i) - 'A') * 26 + (REGISTRY.charAt(i + 1) - 'A');
      table[index] = (byte) Integer.parseInt(REGISTRY.substring(i + 2, i + 4));
    }
    return table;
  }
}
