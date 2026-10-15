package br.com.inpetu.hub.security;

import java.nio.ByteBuffer;
import java.security.*;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Service;

@Service
public class TotpService {
  private static final String ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";

  public String generate() {
    byte[] b = new byte[20];
    new SecureRandom().nextBytes(b);
    return encode(b);
  }

  static String encode(byte[] bytes) {
    StringBuilder result = new StringBuilder();
    int bits = 0, value = 0;
    for (byte b : bytes) {
      value = (value << 8) | (b & 255);
      bits += 8;
      while (bits >= 5) {
        result.append(ALPHABET.charAt((value >> (bits - 5)) & 31));
        bits -= 5;
      }
    }
    if (bits > 0) result.append(ALPHABET.charAt((value << (5 - bits)) & 31));
    return result.toString();
  }

  static byte[] decode(String text) {
    var out = new java.io.ByteArrayOutputStream();
    int value = 0, bits = 0;
    for (char c : text.toCharArray()) {
      int d = ALPHABET.indexOf(c);
      if (d < 0) throw new IllegalArgumentException("Chave TOTP inválida.");
      value = (value << 5) | d;
      bits += 5;
      if (bits >= 8) {
        out.write((value >> (bits - 8)) & 255);
        bits -= 8;
      }
    }
    return out.toByteArray();
  }

  public String code(String secret, long counter) {
    try {
      Mac mac = Mac.getInstance("HmacSHA1");
      mac.init(new SecretKeySpec(decode(secret), "HmacSHA1"));
      byte[] hash = mac.doFinal(ByteBuffer.allocate(8).putLong(counter).array());
      int o = hash[hash.length - 1] & 15;
      int binary =
          ((hash[o] & 127) << 24)
              | ((hash[o + 1] & 255) << 16)
              | ((hash[o + 2] & 255) << 8)
              | (hash[o + 3] & 255);
      return String.format(java.util.Locale.ROOT, "%06d", binary % 1000000);
    } catch (GeneralSecurityException ex) {
      throw new IllegalStateException("Falha no segundo fator.");
    }
  }

  public long verify(String secret, String code, long now, long previous) {
    if (code == null || !code.matches("[0-9]{6}")) return -1;
    long current = now / 30;
    for (long counter = current - 1; counter <= current + 1; counter++)
      if (counter > previous
          && MessageDigest.isEqual(
              code(secret, counter).getBytes(java.nio.charset.StandardCharsets.US_ASCII),
              code.getBytes(java.nio.charset.StandardCharsets.US_ASCII))) return counter;
    return -1;
  }
}
