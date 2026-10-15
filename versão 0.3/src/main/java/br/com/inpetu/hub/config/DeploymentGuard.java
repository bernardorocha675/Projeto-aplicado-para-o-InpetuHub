package br.com.inpetu.hub.config;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class DeploymentGuard implements InitializingBean {
  private final Environment env;

  public DeploymentGuard(Environment env) {
    this.env = env;
  }

  public void afterPropertiesSet() {
    if (!env.matchesProfiles("local", "prod", "test"))
      throw new IllegalStateException("Escolha explicitamente o perfil local ou prod.");
    if (!env.matchesProfiles("test") && !env.getProperty("app.security.test-key", "").isBlank())
      throw new IllegalStateException("Configuração de teste recusada fora dos testes.");
    if (env.matchesProfiles("prod")
        && (env.matchesProfiles("local")
            || !env.getProperty("server.ssl.enabled", Boolean.class, false)
            || !env.getProperty("server.servlet.session.cookie.secure", Boolean.class, false)
            || !env.getProperty("app.security.public-email-required", Boolean.class, false)
            || env.getProperty("spring.mail.username", "").isBlank()
            || env.getProperty("spring.mail.password", "").isBlank()))
      throw new IllegalStateException(
          "Produção exige TLS direto, cookie Secure, confirmação de email e SMTP configurado; não"
              + " combinar local e prod.");
  }
}
