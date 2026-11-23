package io.github.ceydrew.trvalidation.spring;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration of the tr-validation starter, under the {@code tr-validation} prefix.
 *
 * <pre>{@code
 * # application.properties
 * tr-validation.masking.enabled=false   # e.g. in an internal back-office service
 * }</pre>
 */
@ConfigurationProperties("tr-validation")
public class TrValidationProperties {

  private final Masking masking = new Masking();

  /** Creates the properties with defaults; called by Spring Boot. */
  public TrValidationProperties() {}

  /**
   * Returns the masking settings.
   *
   * @return the masking settings
   */
  public Masking getMasking() {
    return masking;
  }

  /** Settings for masking personal data in JSON. */
  public static class Masking {

    private boolean enabled = true;

    /** Creates the settings with defaults; called by Spring Boot. */
    public Masking() {}

    /**
     * Tells whether {@code @Masked} properties and tr-validation value types are written masked.
     *
     * @return {@code true} (the default) to mask
     */
    public boolean isEnabled() {
      return enabled;
    }

    /**
     * Enables or disables masking.
     *
     * @param enabled {@code false} to write full values
     */
    public void setEnabled(boolean enabled) {
      this.enabled = enabled;
    }
  }
}
