package io.github.ceydrew.trvalidation;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Random;
import java.util.function.Function;
import java.util.function.Predicate;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.constraints.IntRange;

/** Property-based tests shared by TCKN, YKN and VKN. */
class IdentityProperties {

  @Property
  void randomTcknIsValid(@ForAll Random random) {
    assertThat(Tckn.isValid(Tckn.random(random).value())).isTrue();
  }

  @Property
  void randomYknIsValid(@ForAll Random random) {
    assertThat(Ykn.isValid(Ykn.random(random).value())).isTrue();
  }

  @Property
  void randomVknIsValid(@ForAll Random random) {
    assertThat(Vkn.isValid(Vkn.random(random).value())).isTrue();
  }

  @Property
  void changingOneTcknDigitInvalidates(@ForAll("tcknMutations") Mutation m) {
    assertThat(Tckn.isValid(m.mutated())).as("%s -> %s", m.original(), m.mutated()).isFalse();
  }

  @Property
  void changingOneYknDigitInvalidates(@ForAll("yknMutations") Mutation m) {
    assertThat(Ykn.isValid(m.mutated())).as("%s -> %s", m.original(), m.mutated()).isFalse();
  }

  @Property
  void changingOneVknDigitInvalidates(@ForAll("vknMutations") Mutation m) {
    assertThat(Vkn.isValid(m.mutated())).as("%s -> %s", m.original(), m.mutated()).isFalse();
  }

  @Property
  void separatorsDoNotChangeTheResult(
      @ForAll Random random, @ForAll @IntRange(min = 0, max = 10) int position) {
    String value = Tckn.random(random).value();
    String spaced = value.substring(0, position) + " -\t" + value.substring(position);

    assertThat(Tckn.isValid(spaced)).isTrue();
    assertThat(Tckn.normalize(spaced)).isEqualTo(value);
  }

  @Property
  void maskNeverRevealsTheMiddle(@ForAll Random random) {
    String value = Tckn.random(random).value();
    String masked = Tckn.mask(value);

    assertThat(masked).hasSize(11).startsWith(value.substring(0, 3)).endsWith(value.substring(9));
    assertThat(masked.substring(3, 9)).isEqualTo("******");
  }

  @Property
  void arbitraryStringsNeverThrow(@ForAll String input) {
    assertThat(Tckn.validate(input)).isNotNull();
    assertThat(Ykn.validate(input)).isNotNull();
    assertThat(Vkn.validate(input)).isNotNull();
    assertThat(Tckn.mask(input)).hasSameSizeAs(Tckn.normalize(input));
  }

  @Provide
  Arbitrary<Mutation> tcknMutations() {
    return mutations(r -> Tckn.random(r).value(), 11);
  }

  @Provide
  Arbitrary<Mutation> yknMutations() {
    return mutations(r -> Ykn.random(r).value(), 11);
  }

  @Provide
  Arbitrary<Mutation> vknMutations() {
    return mutations(r -> Vkn.random(r).value(), 10);
  }

  private static Arbitrary<Mutation> mutations(Function<Random, String> generator, int length) {
    return Combinators.combine(
            Arbitraries.randoms().map(generator),
            Arbitraries.integers().between(0, length - 1),
            Arbitraries.integers().between(1, 9))
        .as(
            (value, index, delta) -> {
              char[] chars = value.toCharArray();
              chars[index] = (char) ('0' + (chars[index] - '0' + delta) % 10);
              return new Mutation(value, new String(chars));
            })
        .filter(Predicate.not(m -> m.original().equals(m.mutated())));
  }

  record Mutation(String original, String mutated) {}
}
