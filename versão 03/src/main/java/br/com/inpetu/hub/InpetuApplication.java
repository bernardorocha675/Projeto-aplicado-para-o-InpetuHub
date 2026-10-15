package br.com.inpetu.hub;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class InpetuApplication {
  private static java.nio.channels.FileChannel lockChannel;
  private static java.nio.channels.FileLock runtimeLock;

  public static void main(String[] args) throws Exception {
    java.nio.file.Files.createDirectories(java.nio.file.Path.of(".secrets"));
    lockChannel =
        java.nio.channels.FileChannel.open(
            java.nio.file.Path.of(".secrets/runtime.lock"),
            java.nio.file.StandardOpenOption.CREATE,
            java.nio.file.StandardOpenOption.WRITE);
    runtimeLock = lockChannel.tryLock();
    if (runtimeLock == null)
      throw new IllegalStateException("Outra execução ou backup está usando esta instalação.");
    Runtime.getRuntime()
        .addShutdownHook(
            new Thread(
                () -> {
                  try {
                    runtimeLock.release();
                    lockChannel.close();
                  } catch (Exception ignored) {
                  }
                }));
    br.com.inpetu.hub.observability.PrivateLogDirectory.prepare();
    SpringApplication.run(InpetuApplication.class, args);
  }
}
