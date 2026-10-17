package br.com.inpetu.hub.observability;

import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.*;
import java.util.*;
import java.util.regex.Pattern;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Limpeza serializada e independente da compressão. Nunca remove o arquivo ativo. */
@Component
@Profile("!test")
public class LogRetention {
  static final long CAP = 200L * 1024 * 1024;
  static final Pattern NAME =
      Pattern.compile("inpetu\\.(\\d{4}-\\d{2}-\\d{2})\\.(\\d{1,9})\\.log\\.gz");

  record Archive(Path path, LocalDate date, int index, long size, Instant modified) {}

  @EventListener(ApplicationReadyEvent.class)
  public void ready() {
    clean();
  }

  @Scheduled(initialDelay = 60000, fixedDelay = 60000)
  public synchronized void clean() {
    try {
      clean(Path.of("logs"), Clock.systemUTC());
    } catch (Exception ex) {
      OperationalEvents.retentionFailure(ex);
    }
  }

  static void clean(Path folder, Clock clock) throws Exception {
    clean(folder, clock, CAP);
  }

  static void clean(Path folder, Clock clock, long cap) throws Exception {
    if (!Files.isDirectory(folder, LinkOption.NOFOLLOW_LINKS)) return;
    Instant now = clock.instant();
    LocalDate oldest = LocalDate.ofInstant(now, ZoneOffset.UTC).minusDays(13);
    List<Archive> archives = new ArrayList<>();
    try (var files = Files.list(folder)) {
      for (Path p : files.toList()) {
        var match = NAME.matcher(p.getFileName().toString());
        if (!match.matches()) continue;
        var attrs = Files.readAttributes(p, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
        if (!attrs.isRegularFile()) continue;
        LocalDate date;
        try {
          date = LocalDate.parse(match.group(1));
        } catch (DateTimeException ignored) {
          continue;
        }
        // A compressão pode estar em andamento; aguardar pelo menos um minuto de estabilidade.
        if (attrs.lastModifiedTime().toInstant().isAfter(now.minusSeconds(60))) continue;
        if (date.isBefore(oldest)) Files.delete(p);
        else
          archives.add(
              new Archive(
                  p,
                  date,
                  Integer.parseInt(match.group(2)),
                  attrs.size(),
                  attrs.lastModifiedTime().toInstant()));
      }
    }
    archives.sort(
        Comparator.comparing(Archive::date)
            .reversed()
            .thenComparing(Comparator.comparingInt(Archive::index).reversed()));
    long kept = 0;
    for (Archive archive : archives) {
      if (archive.size() > cap - kept) Files.delete(archive.path());
      else kept += archive.size();
    }
  }
}
