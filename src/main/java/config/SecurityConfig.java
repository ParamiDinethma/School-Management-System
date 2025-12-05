package com.wsims.config;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private CustomAuthenticationSuccessHandler customAuthenticationSuccessHandler;
    
    @Autowired
    private UserDetailsService userDetailsService;

    @Bean
    public static PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .authorizeHttpRequests((authorize) ->
                        authorize.requestMatchers("/", "/login", "/css/**", "/js/**").permitAll()
                                .requestMatchers("/principal/**").hasAuthority("PRINCIPAL")
                                .requestMatchers("/teacher/**").hasAuthority("TEACHER")
                                // Allow parents to access the student grades API used from parent portal
                                .requestMatchers("/student/api/grades/**").hasAnyAuthority("STUDENT", "PARENT")
                                .requestMatchers("/student/**").hasAuthority("STUDENT")
                                .requestMatchers("/parent/**").hasAuthority("PARENT")
                                .requestMatchers("/admin/**").hasAnyAuthority("PRINCIPAL", "IT_ADMIN", "REGISTRAR")
                                .anyRequest().authenticated()
                ).formLogin(
                        form -> form
                                .loginPage("/login")
                                .loginProcessingUrl("/login")
                                .successHandler(customAuthenticationSuccessHandler)
                                .permitAll()
                ).userDetailsService(userDetailsService)
                .logout(
                        logout -> logout
                                .logoutRequestMatcher(new AntPathRequestMatcher("/logout"))
                                .permitAll()
                );
        return http.build();
    }
}