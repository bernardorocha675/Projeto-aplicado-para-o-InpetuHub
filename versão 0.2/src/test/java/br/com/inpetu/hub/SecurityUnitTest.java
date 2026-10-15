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
  void totpMatchesRfc6238AndRejectsReplay() {
    var t = new TotpService();
    String key = "GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ";
    assertEquals("287082", t.code(key, 1));
    assertEquals(1, t.verify(key, "287082", 59, -1));
    assertEquals(-1, t.verify(key, "287082", 59, 1));
    assertEquals(-1, t.verify(key, "287083", 59, -1));
  }

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
