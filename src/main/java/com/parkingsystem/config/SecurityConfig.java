package com.parkingsystem.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.JdbcUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

import javax.sql.DataSource;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        // Public paths
                        .requestMatchers("/", "/parking", "/parking/**", "/css/**", "/js/**", "/images/**").permitAll()
                        // Admin paths
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        // Attendant paths
                        .requestMatchers("/attendant/**").hasRole("ATTENDANT")
                        // User paths - add this section
                        .requestMatchers("/user/**").hasRole("USER")
                        // Any other request must be authenticated

                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .defaultSuccessUrl("/")
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutSuccessUrl("/")
                        .permitAll()
                )
                // Enable CSRF protection but disable for specific endpoints if needed
                .csrf(csrf -> csrf
                        .ignoringRequestMatchers("/api/**")
                )
                // Set proper header configuration
                .headers(headers -> headers
                        .frameOptions(frameOptions -> frameOptions
                                .sameOrigin()
                        )
                );

        return http.build();
    }

    @Bean
    public UserDetailsService userDetailsService(DataSource dataSource) {
        JdbcUserDetailsManager manager = new JdbcUserDetailsManager(dataSource);

        // Define queries to retrieve user details from our custom User table
        manager.setUsersByUsernameQuery("select username, password, 1 as enabled from users where username = ?");
        manager.setAuthoritiesByUsernameQuery("select username, concat('ROLE_', role) as authority from users where username = ?");

        return manager;
    }
}