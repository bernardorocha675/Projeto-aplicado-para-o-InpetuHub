package br.com.inpetu.hub;

import static org.junit.jupiter.api.Assertions.*;

import br.com.inpetu.hub.config.PrivateAccountInitializer;
import br.com.inpetu.hub.security.*;
import java.nio.file.*;
import java.time.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class SecurityUnitTest {
  @TempDir Path temp;

  @Test
  void rateLimitDoesNotPermanentlyLockVictim() {
    var c =
        new Clock() {
          long now = 0;

          public ZoneId getZone() {
            return ZoneOffset.UTC;
          }

          public Clock withZone(ZoneId z) {
            return this;
          }

          public Instant instant() {
            return Instant.ofEpochMilli(now);
          }

          public long millis() {
            return now;
          }
        };
    var rate = new RateLimits(c);
    assertTrue(rate.allow("a", 1, Duration.ofSeconds(1)));
    assertFalse(rate.allow("a", 1, Duration.ofSeconds(1)));
    c.now = 1001;
    assertTrue(rate.allow("a", 1, Duration.ofSeconds(1)));
  }

  @Test
  void productionLoginStillLimitsAttempts() throws Exception {
    var crypto = new SecretCrypto("unused", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA");
    var filter = new AbuseFilter(new RateLimits(Clock.systemUTC()), crypto, true, () -> true);
    for (int i = 0; i < 11; i++) {
      var req = new org.springframework.mock.web.MockHttpServletRequest("POST", "/login");
      req.setSecure(true);
      req.setServletPath("/login");
      req.setRemoteAddr("198.51.100.55");
      req.addParameter("username", "áluno teste");
      var res = new org.springframework.mock.web.MockHttpServletResponse();
      filter.doFilter(req, res, (request, response) -> {});
      assertEquals(i < 10 ? 200 : 429, res.getStatus());
    }
  }

  @Test
  void provisioningValidatesAllEntriesWithoutExposingThem() throws Exception {
    var file = temp.resolve("accounts.properties");
    String base =
        "account.1.name=Fixture\n"
            + "account.1.login=fixture-private\n"
            + "account.1.password=Synthetic-only-Password!\n"
            + "account.1.role=ADMIN\n";
    Files.writeString(file, base);
    var users = PrivateAccountInitializer.read(file, new BCryptPasswordEncoder(4));
    assertEquals(1, users.size());
    assertTrue(new BCryptPasswordEncoder().matches("Synthetic-only-Password!", users.get(0).senha));
    Files.writeString(file, base + "account.3.login=gap-not-allowed\n");
    assertThrows(
        IllegalStateException.class,
        () -> PrivateAccountInitializer.read(file, new BCryptPasswordEncoder(4)));
    Files.writeString(file, base.replace("Synthetic-only-Password!", "short"));
    var ex =
        assertThrows(
            IllegalStateException.class,
            () -> PrivateAccountInitializer.read(file, new BCryptPasswordEncoder(4)));
    assertFalse(ex.getMessage().contains("short"));
  }
}
