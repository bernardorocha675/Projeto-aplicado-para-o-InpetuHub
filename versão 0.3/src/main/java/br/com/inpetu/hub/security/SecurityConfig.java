package br.com.inpetu.hub.security;

import br.com.inpetu.hub.repository.ColaboradorRepository;
import java.time.Clock;
import org.springframework.context.annotation.*;
import org.springframework.core.env.Environment;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;

@Configuration
public class SecurityConfig {
  @Bean
  PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder(12);
  }

  @Bean
  UserDetailsService users(ColaboradorRepository repo) {
    return login ->
        repo.findByLoginIgnoreCase(LoginNames.normalize(login))
            .map(AccountPrincipal::new)
            .orElseThrow(() -> new UsernameNotFoundException("Login inválido"));
  }

  @Bean
  SecurityFilterChain security(
      HttpSecurity http,
      ColaboradorRepository users,
      RateLimits limits,
      SecretCrypto crypto,
      AuditService audit,
      Clock clock,
      Environment env,
      org.springframework.boot.availability.ApplicationAvailability availability)
      throws Exception {
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
                        "/reservas/sucesso",
                        "/solicitacoes/confirmar")
                    .permitAll()
                    .requestMatchers("/admin/config/**")
                    .hasRole("ADMIN")
                    .anyRequest()
                    .authenticated())
        .headers(
            h ->
                h.contentSecurityPolicy(
                        c ->
                            c.policyDirectives(
                                "default-src 'self'; script-src 'self'; style-src 'self'; img-src"
                                    + " 'self' data:; connect-src 'self'; object-src 'none';"
                                    + " base-uri 'none'; frame-ancestors 'none'; form-action"
                                    + " 'self'"))
                    .referrerPolicy(
                        p -> p.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.NO_REFERRER))
                    .permissionsPolicy(p -> p.policy("camera=(), microphone=(), geolocation=()")))
        .exceptionHandling(
            e ->
                e.accessDeniedHandler(
                    (req, res, ex) -> {
                      var a =
                          org.springframework.security.core.context.SecurityContextHolder
                              .getContext()
                              .getAuthentication();
                      audit.security(
                          a != null && a.getPrincipal() instanceof AccountPrincipal p
                              ? p.id()
                              : null,
                          "ACESSO_NEGADO",
                          "NEGADO");
                      res.sendError(403);
                    }))
        .formLogin(
            f ->
                f.loginPage("/login")
                    .successHandler(
                        (req, res, auth) -> {
                          AccountPrincipal p = (AccountPrincipal) auth.getPrincipal();
                          req.getSession().setAttribute("auth.started", clock.millis());
                          audit.security(p.id(), "LOGIN_SENHA", "OK");
                          res.sendRedirect("/admin");
                        })
                    .failureHandler(
                        (req, res, ex) -> {
                          audit.security(null, "LOGIN_NEGADO", "NEGADO");
                          res.sendRedirect("/login?error");
                        })
                    .permitAll())
        .logout(
            l ->
                l.addLogoutHandler(
                        (req, res, a) -> {
                          if (a != null && a.getPrincipal() instanceof AccountPrincipal p)
                            audit.security(p.id(), "LOGOUT", "OK");
                        })
                    .logoutSuccessUrl("/login?logout"))
        .addFilterBefore(
            new AbuseFilter(
                limits,
                crypto,
                env.matchesProfiles("prod"),
                () ->
                    env.matchesProfiles("test")
                        || availability.getReadinessState()
                            == org.springframework.boot.availability.ReadinessState
                                .ACCEPTING_TRAFFIC),
            UsernamePasswordAuthenticationFilter.class)
        .addFilterAfter(
            new SessionGuardFilter(users, audit, clock), UsernamePasswordAuthenticationFilter.class)
        .build();
  }
}
