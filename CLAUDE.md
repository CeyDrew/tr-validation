# CLAUDE.md

Portföy projesi: `tr-validation`, Türkiye'ye özgü doğrulama (TCKN, VKN, IBAN, plaka...) için Java kütüphanesi. Core + Jakarta Validation + Spring Boot starter; hedef Maven Central.
Tüm yol haritası `PLAN.md` içinde. İşe başlamadan önce onu oku ve kaldığın fazı `git log` / branch'lerden tespit et.

## Çalışma kuralları

- Fazları `PLAN.md` sırasıyla uygula. Her faz bitince dur, kullanıcıya kısa özet ver, onay almadan sonraki faza geçme.
- Her faz için branch: `feature/phase-N-kisa-ad`. Faz bitince testler geçiyorsa `main`'e merge et ve push et.
- Conventional Commits: `feat:`, `fix:`, `refactor:`, `test:`, `docs:`, `chore:`, `build:`, `ci:`.
- GitHub repo oluşturma, force push, branch silme, **release tag push etme ve Maven Central'a yayın** öncesi kullanıcıya sor. Central'a çıkan sürüm geri alınamaz.
- GPG anahtarı, Sonatype token gibi secret'lar asla repoya girmez; sadece GitHub Actions secret'larında durur.
- `D:\projeler\insurance-policy-management` başka bir oturumun aktif projesi; PLAN.md Faz 7'ye kadar ona dokunma.

## Ortam (Windows 11)

- JDK 22 kurulu; proje `release 17` hedefler. Global Maven yok: `./mvnw` (Windows'ta `mvnw.cmd`).
- `gh` CLI giriş yapılmış (hesap: CeyDrew).

## Kod kuralları

- `tr-validation-core` **sıfır runtime bağımlılık**. Bağımlılık eklemen gerekiyorsa core'a değil, diğer modüllere ekle.
- Public API küçük ve kararlı: public olması gerekmeyen her şey package-private. Public API değişikliği CHANGELOG'a yazılır.
- Doğrulayıcılar thread-safe, durumsuz, null'a karşı tanımlı davranışlı (null → geçersiz, exception değil; Bean Validation tarafında null → geçerli, `@NotNull` ile birlikte kullanılır).
- Regex yerine mümkün olduğunca karakter bazlı kontrol (performans + ReDoS riski yok).
- Her public metot Javadoc'lu, örnekli.
- Her doğrulayıcı için: parametrized testler + jqwik property testleri. Kapsam hedefi core'da %100.
- Kod formatı Spotless ile; commit öncesi `./mvnw spotless:apply`.

## Komutlar

```bash
./mvnw verify                         # build + test + coverage
./mvnw spotless:apply                 # format
./mvnw -pl tr-validation-examples spring-boot:run
./mvnw -P release deploy              # sadece CI'da, Faz 6
```
