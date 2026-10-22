package com.example.factura.config;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.util.Base64;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {
    public static final String EMISOR = "nicamarket-laboratorio";

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // Usuarios de laboratorio: solo para desarrollo, no usar en producción.
    @Bean
    UserDetailsService usuarios(PasswordEncoder encoder) {
        return new InMemoryUserDetailsManager(
            User.withUsername("admin")
                .password(encoder.encode("Admin123!"))
                .roles("ADMIN").build(),
            User.withUsername("operador")
                .password(encoder.encode("Operador123!"))
                .roles("USER").build()
        );
    }

    @Bean
    AuthenticationManager authenticationManager(
            UserDetailsService usuarios, PasswordEncoder encoder) {
        DaoAuthenticationProvider provider =
            new DaoAuthenticationProvider(usuarios);
        provider.setPasswordEncoder(encoder);
        return new ProviderManager(provider);
    }

    @Bean
    SecretKey jwtSecretKey() {
        String base64 = System.getenv("JWT_SECRET_BASE64");
        if (base64 == null || base64.isBlank()) {
            throw new IllegalStateException(
                "Configure JWT_SECRET_BASE64 en IntelliJ");
        }
        byte[] clave = Base64.getDecoder().decode(base64.trim());
        if (clave.length < 32) {
            throw new IllegalStateException(
                "La clave HS256 debe tener al menos 32 bytes");
        }
        return new SecretKeySpec(clave, "HmacSHA256");
    }

    @Bean
    JwtEncoder jwtEncoder(SecretKey clave) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(clave));
    }

    @Bean
    JwtDecoder jwtDecoder(SecretKey clave) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(clave)
            .macAlgorithm(MacAlgorithm.HS256).build();
        // Además de firma y vencimiento, exige que el emisor sea esta API.
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(EMISOR));
        return decoder;
    }

    @Bean
    JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter roles =
            new JwtGrantedAuthoritiesConverter();
        roles.setAuthoritiesClaimName("roles");
        roles.setAuthorityPrefix("ROLE_");
        JwtAuthenticationConverter converter =
            new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(roles);
        return converter;
    }

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtAuthenticationConverter converter) throws Exception {
        return http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Públicos: login y saludo.
                .requestMatchers(HttpMethod.POST, "/api/auth/login")
                    .permitAll()
                .requestMatchers("/api/publico/**", "/error")
                    .permitAll()
                // Perfil: cualquier usuario autenticado.
                .requestMatchers(HttpMethod.GET, "/api/auth/perfil")
                    .authenticated()
                // Productos: consulta USER/ADMIN, modificación solo ADMIN.
                .requestMatchers(HttpMethod.GET, "/api/productos", "/api/productos/**")
                    .hasAnyRole("USER", "ADMIN")
                .requestMatchers("/api/productos", "/api/productos/**")
                    .hasRole("ADMIN")
                // Categorías (reto final): consulta USER/ADMIN,
                // POST, PUT y DELETE solo ADMIN.
                .requestMatchers(HttpMethod.GET, "/api/categorias", "/api/categorias/**")
                    .hasAnyRole("USER", "ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/categorias")
                    .hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/categorias/**")
                    .hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/categorias/**")
                    .hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth -> oauth.jwt(jwt ->
                jwt.jwtAuthenticationConverter(converter)))
            .build();
    }
}
