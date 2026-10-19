package br.com.inpetu.hub;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class InpetuApplication {
  /** Saída da conferência quando outro processo mantém o lock; o iniciador a distingue de falhas. */
  public static final int DATA_IN_USE_EXIT = 75;

  private static java.nio.channels.FileChannel lockChannel;
  private static java.nio.channels.FileLock runtimeLock;

  public static void main(String[] args) throws Exception {
    boolean stoppedCheck = args.length == 1 && args[0].equals("--verificar-parado");
    boolean accessTool =
        java.util.Arrays.stream(args)
            .anyMatch(a -> a.equals("--verificar-acessos") || a.equals("--recuperar-acessos"));
    if (accessTool && args.length != 1)
      throw new IllegalArgumentException(
          "Use somente o comando de acessos, sem opções de servidor.");
    java.nio.file.Files.createDirectories(java.nio.file.Path.of(".secrets"));
    lockChannel =
        java.nio.channels.FileChannel.open(
            java.nio.file.Path.of(".secrets/runtime.lock"),
            java.nio.file.StandardOpenOption.CREATE,
            java.nio.file.StandardOpenOption.WRITE);
    runtimeLock = lockChannel.tryLock();
    if (runtimeLock == null && stoppedCheck) {
      lockChannel.close();
      System.exit(DATA_IN_USE_EXIT);
      return;
    }
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
    if (accessTool || stoppedCheck) {
      int result;
      try {
        result = stoppedCheck ? 0 : br.com.inpetu.hub.maintenance.LocalAccessTool.run(args[0]);
      } finally {
        runtimeLock.release();
        lockChannel.close();
      }
      System.exit(result);
      return;
    }
    br.com.inpetu.hub.observability.PrivateLogDirectory.prepare();
    SpringApplication.run(InpetuApplication.class, args);
  }
}
