package br.com.inpetu.hub.config;

import java.nio.file.*;
import org.springframework.boot.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/** Encerramento local solicitado pelo iniciador do mesmo usuario, sem rota HTTP administrativa. */
@Component
@Profile("local")
@ConditionalOnProperty(name="app.launcher.enabled",havingValue="true")
public class LocalLauncherLifecycle implements ApplicationRunner {
  private final ConfigurableApplicationContext context;
  public LocalLauncherLifecycle(ConfigurableApplicationContext context) { this.context=context; }
  @Override public void run(ApplicationArguments arguments) {
    Thread watcher=new Thread(() -> {
      while(context.isActive()) {
        try {
          Thread.sleep(500);
          Path request=Path.of(".secrets/launcher.stop");
          if(Files.isRegularFile(request,LinkOption.NOFOLLOW_LINKS)) {
            Files.delete(request);
            context.close();
            System.exit(0);
            return;
          }
        } catch(InterruptedException ex) { Thread.currentThread().interrupt();return; }
          catch(Exception ignored) { /* O iniciador pode encerrar o processo se o pedido falhar. */ }
      }
    },"inpetu-local-stop");
    watcher.setDaemon(true);watcher.start();
  }
}
