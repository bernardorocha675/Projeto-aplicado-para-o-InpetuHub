package br.com.inpetu.hub.security;

import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.*;
import java.util.*;
import javax.crypto.*;
import javax.crypto.spec.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class SecretCrypto {
  private static final SecureRandom RANDOM = new SecureRandom();
  private final byte[] key;

  public SecretCrypto(
      @Value("${app.security.key-file:.secrets/mfa.key}") String path,
      @Value("${app.security.test-key:}") String testKey) {
    try {
      String material = testKey.isBlank() ? Files.readString(Path.of(path)).trim() : testKey;
      key = Base64.getUrlDecoder().decode(material);
      if (key.length != 32) throw new IllegalArgumentException();
    } catch (Exception ex) {
      throw new IllegalStateException(
          "Chave privada ausente ou inválida. Execute o configurador; preserve .secrets/.");
    }
  }

  public static String random(int bytes) {
    byte[] b = new byte[bytes];
    RANDOM.nextBytes(b);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(b);
  }

  public String fingerprint(String value) {
    try {
      Mac mac = Mac.getInstance("HmacSHA256");
      mac.init(new SecretKeySpec(key, "HmacSHA256"));
      return HexFormat.of().formatHex(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
    } catch (GeneralSecurityException ex) {
      throw new IllegalStateException("Falha criptográfica.");
    }
  }

  public String encrypt(String value) {
    try {
      byte[] nonce = new byte[12];
      RANDOM.nextBytes(nonce);
      Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
      c.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(128, nonce));
      byte[] encrypted = c.doFinal(value.getBytes(StandardCharsets.UTF_8));
      byte[] result = Arrays.copyOf(nonce, nonce.length + encrypted.length);
      System.arraycopy(encrypted, 0, result, nonce.length, encrypted.length);
      return Base64.getUrlEncoder().withoutPadding().encodeToString(result);
    } catch (GeneralSecurityException ex) {
      throw new IllegalStateException("Falha ao proteger segundo fator.");
    }
  }

  public String decrypt(String value) {
    try {
      byte[] all = Base64.getUrlDecoder().decode(value);
      Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
      c.init(
          Cipher.DECRYPT_MODE,
          new SecretKeySpec(key, "AES"),
          new GCMParameterSpec(128, Arrays.copyOf(all, 12)));
      return new String(c.doFinal(all, 12, all.length - 12), StandardCharsets.UTF_8);
    } catch (Exception ex) {
      throw new IllegalStateException(
          "Chave do segundo fator não corresponde ao banco. Restaure o backup completo.");
    }
  }

  public boolean matches(String expected, String actual) {
    return expected != null
        && actual != null
        && MessageDigest.isEqual(
            expected.getBytes(StandardCharsets.UTF_8), actual.getBytes(StandardCharsets.UTF_8));
  }
}
