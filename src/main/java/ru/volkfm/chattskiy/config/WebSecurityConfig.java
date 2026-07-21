package ru.volkfm.chattskiy.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import ru.volkfm.chattskiy.repository.postgres.UserRepository;
import ru.volkfm.chattskiy.service.R2dbUserDetailsService;

@Configuration
@EnableWebFluxSecurity
@Slf4j
public class WebSecurityConfig {
    @Bean
    public R2dbUserDetailsService userDetailsService(UserRepository userRepository) {
        return new R2dbUserDetailsService(userRepository);
    }

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
         http
                 .authorizeExchange(exchanges -> exchanges
                        .pathMatchers("/chat").hasRole("USER")
                        .anyExchange().authenticated())
                        .httpBasic(Customizer.withDefaults())
                .csrf(csrf -> csrf.disable());
        return http.build();
    }
}