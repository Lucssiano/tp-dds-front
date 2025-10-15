package ar.utn.ba.dds.front_tp.config;

import ar.utn.ba.dds.front_tp.providers.CustomAuthProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@EnableMethodSecurity(prePostEnabled = true)
@Configuration
public class SecurityConfig {

  @Bean
  public AuthenticationManager authManager(HttpSecurity http, CustomAuthProvider provider) throws Exception {
    return http.getSharedObject(AuthenticationManagerBuilder.class)
        .authenticationProvider(provider)
        .build();
  }

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
        .csrf(csrf -> csrf.disable())
        .authorizeHttpRequests(auth -> auth
            // ✅ Rutas públicas (no requieren login)
            .requestMatchers(
                "/", "/home", "/auth/**", "/hechos/**", "/colecciones/**",
                "/css/**", "/js/**", "/images/**"
            ).permitAll()
            // 🔒 Rutas de Administrador (requieren rol ADMIN)
            .requestMatchers("/admin/**").hasRole("ADMIN")
            // 🔒 Cualquier otra ruta requiere que el usuario esté autenticado
            .anyRequest().authenticated()
        )
        .formLogin(form -> form
            .loginPage("/auth")
            .loginProcessingUrl("/auth/login")
            .usernameParameter("email")
            .passwordParameter("password")
            .defaultSuccessUrl("/", true)
            .failureUrl("/auth?error=true")
            .permitAll()
        )
        .logout(logout -> logout
            .logoutUrl("/auth/logout")
            .logoutSuccessUrl("/") // volvemos al home tras logout
            .invalidateHttpSession(true)
            .deleteCookies("JSESSIONID")
            .permitAll()
        )
        .exceptionHandling(ex -> ex
            .authenticationEntryPoint((request, response, authException) ->
                response.sendRedirect("/auth")
            )
        );

    return http.build();
  }
}

