package com.healthcare.config;
import com.healthcare.model.*;
import com.healthcare.repo.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.*;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import java.util.List;

@Configuration @EnableMethodSecurity
public class SecurityConfig {
    @Bean PasswordEncoder encoder() { return new BCryptPasswordEncoder(); }

    @Bean UserDetailsService userDetailsService(UserRepository users) {
        return name -> users.findByUsername(name)
                .map(u -> org.springframework.security.core.userdetails.User.withUsername(u.username)
                        .password(u.passwordHash).roles(u.role.name()).build())
                .orElseThrow(() -> new UsernameNotFoundException(name));
    }
    @Bean SecurityFilterChain chain(HttpSecurity http, @Value("${app.cors-origin}") String origin) throws Exception {
        http.csrf(c -> c.disable())
            .cors(c -> c.configurationSource(req -> {
                CorsConfiguration cc = new CorsConfiguration();
                cc.setAllowedOrigins(List.of(origin)); cc.setAllowedMethods(List.of("*")); cc.setAllowedHeaders(List.of("*"));
                cc.setExposedHeaders(List.of("Content-Disposition")); return cc; }))
            .authorizeHttpRequests(a -> a.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers("/api/auth/register").permitAll().anyRequest().authenticated())
            .httpBasic(Customizer.withDefaults());
        return http.build();
    }
    /** creates the default Admin account on first start */
    @Bean CommandLineRunner seedAdmin(UserRepository users, PasswordEncoder enc, @Value("${app.admin-password}") String pw) {
        return args -> {
            if (users.findByUsername("admin").isEmpty()) {
                User u = new User(); u.username = "admin"; u.passwordHash = enc.encode(pw);
                u.role = Role.ADMIN; u.attributes = "admin"; users.save(u);
            }
        };
    }
}
