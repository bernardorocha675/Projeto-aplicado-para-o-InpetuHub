package br.com.inpetu.hub.config;

import java.time.Clock;
import org.springframework.context.annotation.*;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

@Configuration
public class ValidationConfig {
  @Bean
  public LocalValidatorFactoryBean validator(Clock clock) {
    var v = new LocalValidatorFactoryBean();
    v.setConfigurationInitializer(c -> c.clockProvider(() -> clock));
    return v;
  }
}
