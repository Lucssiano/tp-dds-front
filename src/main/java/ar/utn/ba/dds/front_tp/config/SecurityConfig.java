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
                "/css/**", "/js/**", "/images/**","/favicon.svg", "/favicon.ico",
                "/privacidad", "/terminos", "/contacto",
                "/hechos/subir-hecho", "/hechos/crear-hecho", "/404", "/403"
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
            .permitAll()
        )
        .logout(logout -> logout
            .logoutUrl("/auth/logout")
            .logoutSuccessUrl("/auth/login?logout=true") //si no redireccionar a home
            .invalidateHttpSession(true)
            .deleteCookies("JSESSIONID")
            .permitAll()
        )
        .exceptionHandling(ex -> ex
            .authenticationEntryPoint((request, response, authException) ->
                response.sendRedirect("/auth")
            )
        )
        .exceptionHandling(ex -> ex
            .accessDeniedPage("/403") // Redirige a tu controller simple
        );

    return http.build();
  }
}