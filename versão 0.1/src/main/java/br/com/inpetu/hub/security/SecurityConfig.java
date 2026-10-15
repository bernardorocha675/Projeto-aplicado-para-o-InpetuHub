package br.com.inpetu.hub.security;

import br.com.inpetu.hub.repository.ColaboradorRepository;
import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {
  @Bean
  PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  UserDetailsService users(ColaboradorRepository repo) {
    return login ->
        repo.findByLogin(login)
            .map(
                c ->
                    User.withUsername(c.login)
                        .password(c.senha)
                        .roles(c.perfil.name())
                        .disabled(!c.ativo)
                        .build())
            .orElseThrow(() -> new UsernameNotFoundException("Login inválido"));
  }

  @Bean
  SecurityFilterChain security(HttpSecurity http) throws Exception {
    return http.authorizeHttpRequests(
            a ->
                a.requestMatchers(
                        "/css/**",
                        "/js/**",
                        "/images/**",
                        "/",
                        "/agenda",
                        "/login",
                        "/error",
                        "/disponibilidade",
                        "/reserva",
                        "/reservas/resumo",
                        "/reservas/solicitar",
                        "/reservas/sucesso")
                    .permitAll()
                    .requestMatchers("/admin/config/**")
                    .hasRole("ADMIN")
                    .anyRequest()
                    .authenticated())
        .formLogin(f -> f.loginPage("/login").defaultSuccessUrl("/admin", true).permitAll())
        .logout(l -> l.logoutSuccessUrl("/login?logout"))
        .build();
  }
}
