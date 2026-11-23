# tr-validation — Türkiye'ye Özgü Doğrulama Kütüphanesi — Geliştirme Planı

## Amaç

İş başvurusu için destekleyici portföy projesi. Hedef ilan: **Mid-Level Full Stack Developer (sigorta sektörü; Java, Spring Boot)**.
Gösterilecek şey: **başkalarının kullanacağı, yayınlanmış, kaliteli bir Java kütüphanesi** yazabilmek. API tasarımı, geriye uyumluluk, sürümleme, dokümantasyon, test disiplini.

Kütüphane Türkiye'ye özgü kimlik ve finans verilerini doğrular, maskeler ve formatlar. Sigorta, banka, e-ticaret projelerinde ortak ihtiyaç.

## Kapsam kısıtları

- Kütüphane; sunucu, veritabanı, deploy yok.
- Her faz sonunda GitHub'a push.
- Maven Central'a yayın **son fazda** ve kullanıcının hesap/anahtar adımlarını tamamlamasından sonra.

## Koordinatlar ve modüller

- groupId: `io.github.ceydrew` (GitHub kullanıcı adıyla Central Portal namespace doğrulaması)
- Java **17** baseline (Spring Boot 3 kullanıcıları dahil geniş kitle). Makinede JDK 22 var; `release 17` ile derlenir.
- Maven multi-module, Maven Wrapper:

```
tr-validation/
├── tr-validation-core/                    # sıfır bağımlılık; saf Java doğrulayıcılar
├── tr-validation-jakarta/                 # Bean Validation anotasyonları (@Tckn, @Vkn, @TrIban...)
├── tr-validation-spring-boot-starter/     # otomatik konfigürasyon + i18n mesajlar + Jackson maskeleme
├── tr-validation-examples/                # örnek Spring Boot uygulaması (yayınlanmaz)
├── tr-validation-bom/                     # sürüm hizalama için BOM
├── docs/
├── CLAUDE.md
├── PLAN.md
├── CHANGELOG.md
└── README.md
```

## Kapsam (doğrulayıcılar)

| Doğrulayıcı | Kural |
|---|---|
| TCKN | 11 hane, ilk hane 0 değil, 10. hane `((tekler toplamı × 7) − çiftler toplamı) mod 10`, 11. hane ilk 10 hanenin toplamı mod 10 |
| YKN (yabancı kimlik no) | 99 ile başlayan, TCKN algoritmasıyla geçerli numara |
| VKN | 10 hane, resmi vergi kimlik numarası kontrol algoritması |
| IBAN | ISO 13616 mod-97; genel IBAN + TR'ye özgü kontrol (26 karakter, banka kodu alanı) |
| Plaka | İl kodu 01–81, harf/rakam kombinasyon kuralları (1 harf + 4 rakam, 2 harf + 3/4 rakam, 3 harf + 2/3 rakam), boşluklu/boşluksuz giriş normalize edilir |
| Telefon | TR mobil/sabit, `+90`, `0`, boşluk, tire, parantez varyasyonları; E.164'e normalize |
| Kredi kartı | Luhn + kart ağı tespiti (Visa, Mastercard, Troy, Amex) |
| Posta kodu | 5 hane, ilk iki hane geçerli il kodu |

Her doğrulayıcı için:
- `isValid(String)` → boolean
- `validate(String)` → sebepli sonuç (`ValidationResult` / sealed tip: `Valid`, `Invalid(reason)`); sebep enum'u (`WRONG_LENGTH`, `CHECKSUM_MISMATCH`...)
- `normalize(String)` (boşluk, tire, küçük harf temizliği)
- `mask(String)` (ör. TCKN `123******01`, IBAN `TR12 **** **** **** **** **12 34`, kart `**** **** **** 1234`)
- `format(String)` (ör. IBAN 4'lü gruplama)
- Test verisi üretici: `Tckn.random()`, `Iban.randomTr()`. Sadece test amaçlı, algoritmik olarak geçerli kurgusal değerler; Javadoc'ta bunu açıkça yaz.

## Fazlar

Her faz: branch `feature/phase-N-kisa-ad` → testler geçer → `main`'e merge → push.
Her fazın sonunda **dur, kullanıcıya özet ver, onay almadan sonraki faza geçme.**

### Faz 0 — İskelet ve GitHub
- Multi-module Maven + Wrapper, `.gitignore`, Apache-2.0 `LICENSE`, `CHANGELOG.md` (Keep a Changelog formatı)
- Ortak ayarlar parent POM'da: `release 17`, encoding, Surefire, JaCoCo, Spotless (google-java-format)
- `git init`, ilk commit, `gh repo create` ile **public** repo (önce kullanıcıya adı onaylat), push
- **Bitti kriteri:** `./mvnw verify` boş modüllerle geçiyor.

### Faz 1 — Core: kimlik doğrulayıcıları
- TCKN, YKN, VKN: validate / isValid / normalize / mask / random
- Public API tasarımı: final sınıflar + statik fabrikalar veya value object (`Tckn.of("...")` geçersizse exception, `Tckn.parse` → `Optional`). Kararı `docs/adr/0001-api-style.md`'ye yaz.
- Testler: JUnit 5 parametrized testler, sınır değerleri, null/boş/boşluklu girişler; **jqwik ile property-based test** (üretilen her random değer geçerli olmalı, tek hane değişince geçersiz olmalı)
- **Bitti kriteri:** core'da %100 satır + dal kapsamı.

### Faz 2 — Core: finans ve iletişim doğrulayıcıları
- IBAN, kredi kartı, telefon, plaka, posta kodu; aynı API deseni
- JMH ile mikro benchmark modülü (yayınlanmaz): doğrulama sıfır/az nesne üretimiyle çalışmalı; sonuçlar README'de
- **Bitti kriteri:** tüm doğrulayıcılar testli, %100 kapsam, benchmark sonuçları yazılı.

### Faz 3 — Jakarta Bean Validation modülü
- Anotasyonlar: `@Tckn`, `@Ykn`, `@Vkn`, `@TrIban`, `@TrPlate`, `@TrPhone`, `@CardNumber`, `@TrPostalCode`; seçenekler (ör. `@Tckn(allowYkn = true)`, `@TrPhone(mobileOnly = true)`)
- `ValidationMessages.properties` (İngilizce) + `ValidationMessages_tr.properties` (Türkçe)
- Hibernate Validator ile testler
- **Bitti kriteri:** DTO üzerinde anotasyonlar çalışıyor, mesajlar iki dilde doğru.

### Faz 4 — Spring Boot starter
- Auto-configuration (`@AutoConfiguration`, `AutoConfiguration.imports`), `@ConfigurationProperties` (`tr-validation.masking.enabled` vb.)
- Jackson modülü: `@Masked` anotasyonlu alanlar JSON'a maskeli yazılır (log ve API cevaplarında kişisel veri sızmasın; KVKK vurgusu README'de)
- Spring MVC'de `@RequestParam @Tckn String tckn` kullanım örneği
- `ApplicationContextRunner` ile auto-config testleri
- `tr-validation-examples`: küçük bir sigorta müşteri kayıt API'si; tüm özellikleri gösterir
- **Bitti kriteri:** örnek uygulama starter'ı sadece bağımlılık ekleyerek kullanıyor, testler geçiyor.

### Faz 5 — Dokümantasyon ve CI
- README: rozetler (CI, coverage, Maven Central), kurulum (Maven + Gradle), her doğrulayıcı için örnek kod, maskeleme örnekleri, uyumluluk tablosu (Java/Spring Boot sürümleri)
- Javadoc: tüm public API eksiksiz; `-Xdoclint` hatasız
- GitHub Actions: Java 17 ve 21 matrisi ile build + test, JaCoCo raporu, Spotless kontrolü
- Dependabot ayarı
- `docs/adr/` karar kayıtları
- **Bitti kriteri:** CI yeşil, Javadoc uyarısız.

### Faz 6 — Maven Central'a yayın
Bu fazdan önce **kullanıcının elle yapması gerekenler** (Claude Code bunları yapamaz, kullanıcıya adım adım anlatır ve bekler):
1. https://central.sonatype.com hesabı açmak ve `io.github.ceydrew` namespace'ini doğrulamak (GitHub ile giriş veya doğrulama reposu)
2. GPG anahtarı üretip public kısmını keyserver'a göndermek
3. Central Portal'dan user token almak
4. Token ve GPG anahtarını GitHub repo secret'larına eklemek

Claude Code'un yapacakları:
- `central-publishing-maven-plugin`, `maven-gpg-plugin`, `maven-source-plugin`, `maven-javadoc-plugin`; POM'larda Central'ın zorunlu metadata'sı (name, description, url, license, developers, scm)
- `release` profili; `examples` ve benchmark modülleri yayından hariç
- GitHub Actions release workflow: `v*` tag'i push edilince imzalı yayın + GitHub Release notları (CHANGELOG'dan)
- Önce `0.1.0` ile deneme yayını; **tag push etmeden önce kullanıcıdan onay al** (Central'a yayınlanan sürüm geri alınamaz)
- **Bitti kriteri:** `io.github.ceydrew:tr-validation-core:0.1.0` Maven Central'da görünüyor; boş bir projede bağımlılık olarak eklenip kullanılabiliyor.

### Faz 7 — Ana projede kullanım (opsiyonel)
- `insurance-policy-management` projesinde elle yazılmış TCKN/VKN/IBAN doğrulamasını bu kütüphaneyle değiştirmek.
- O proje ayrı bir oturumda geliştiriliyor; bu faz **sadece kullanıcı isterse** ve o projede çalışan oturum yokken yapılır.

## Kapsam dışı
- Adres/il-ilçe veritabanı, MERNIS gibi harici servis sorguları (kütüphane çevrimdışı ve bağımlılıksız kalır)
- Android'e özel modül
