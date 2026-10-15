package br.com.inpetu.hub.security;

import java.time.*;
import java.util.*;
import org.springframework.stereotype.Component;

@Component
public class RateLimits {
  private record Bucket(long until, int count) {}

  private final Map<String, Bucket> buckets = new HashMap<>();
  private final Clock clock;

  public RateLimits(Clock clock) {
    this.clock = clock;
  }

  // Janela fixa, memória limitada e nenhuma identificação pessoal armazenada.
  public synchronized boolean allow(String key, int maximum, Duration period) {
    long now = clock.millis();
    if (buckets.size() >= 10000) buckets.entrySet().removeIf(e -> e.getValue().until <= now);
    Bucket old = buckets.get(key);
    if (old == null || old.until <= now) {
      if (old == null && buckets.size() >= 10000) return false;
      buckets.put(key, new Bucket(now + period.toMillis(), 1));
      return true;
    }
    if (old.count >= maximum) return false;
    buckets.put(key, new Bucket(old.until, old.count + 1));
    return true;
  }
}
