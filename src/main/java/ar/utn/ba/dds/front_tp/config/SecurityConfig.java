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
        .csrf(csrf -> csrf.disable()) // ✅ desactiva CSRF para evitar bloqueos en formularios simples
        .authorizeHttpRequests(auth -> auth
            // ✅ rutas públicas
            .requestMatchers("/", "/home", "/auth", "/auth/**", "/css/**", "/js/**", "/images/**").permitAll()
            // 🔒 el resto requiere autenticación
            .anyRequest().authenticated()
        )
        .formLogin(form -> form
            .loginPage("/auth")                     // tu template de login (auth.html)
            .permitAll()
            .defaultSuccessUrl("/hechos", true)     // redirigir tras login exitoso
        )
        .logout(logout -> logout
            .logoutUrl("/logout")
            .logoutSuccessUrl("/auth?logout")       // redirigir tras logout
            .permitAll()
        )
        .exceptionHandling(ex -> ex
            .authenticationEntryPoint((request, response, authException) ->
                // si intenta acceder a una ruta protegida sin login, redirige a /auth
                response.sendRedirect("/auth")
            )
        );

    return http.build();
  }

}
