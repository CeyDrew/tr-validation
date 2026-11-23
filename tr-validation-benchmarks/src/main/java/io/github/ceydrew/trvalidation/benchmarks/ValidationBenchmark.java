package io.github.ceydrew.trvalidation.benchmarks;

import io.github.ceydrew.trvalidation.CardNumber;
import io.github.ceydrew.trvalidation.Iban;
import io.github.ceydrew.trvalidation.LicensePlate;
import io.github.ceydrew.trvalidation.PhoneNumber;
import io.github.ceydrew.trvalidation.PostalCode;
import io.github.ceydrew.trvalidation.Tckn;
import io.github.ceydrew.trvalidation.ValidationResult;
import io.github.ceydrew.trvalidation.Vkn;
import io.github.ceydrew.trvalidation.Ykn;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

/**
 * Throughput and allocation of {@code validate} on typical, formatted input.
 *
 * <p>Run with {@code java -jar tr-validation-benchmarks/target/benchmarks.jar -prof gc}; the {@code
 * gc.alloc.rate.norm} column should be ~0 B/op for every validator.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(1)
@State(Scope.Benchmark)
public class ValidationBenchmark {

  // non-final so the JIT cannot constant-fold the inputs
  String tckn = "100 000 001 46";
  String ykn = "99000000042";
  String vkn = "123-456-7890";
  String iban = "TR33 0006 1005 1978 6457 8413 26";
  String card = "4111 1111 1111 1111";
  String phone = "+90 (532) 123 45 67";
  String plate = "34 ABC 123";
  String postalCode = "34710";
  String invalidTckn = "10000000147";

  @Benchmark
  public ValidationResult tckn() {
    return Tckn.validate(tckn);
  }

  @Benchmark
  public ValidationResult tcknInvalid() {
    return Tckn.validate(invalidTckn);
  }

  @Benchmark
  public ValidationResult ykn() {
    return Ykn.validate(ykn);
  }

  @Benchmark
  public ValidationResult vkn() {
    return Vkn.validate(vkn);
  }

  @Benchmark
  public ValidationResult iban() {
    return Iban.validate(iban);
  }

  @Benchmark
  public ValidationResult ibanTr() {
    return Iban.validateTr(iban);
  }

  @Benchmark
  public ValidationResult cardNumber() {
    return CardNumber.validate(card);
  }

  @Benchmark
  public ValidationResult phoneNumber() {
    return PhoneNumber.validate(phone);
  }

  @Benchmark
  public ValidationResult licensePlate() {
    return LicensePlate.validate(plate);
  }

  @Benchmark
  public ValidationResult postalCode() {
    return PostalCode.validate(postalCode);
  }

  /** For contrast: masking builds new strings, so it allocates. */
  @Benchmark
  public String tcknMask() {
    return Tckn.mask(tckn);
  }
}
