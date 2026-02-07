package com.krzelj.lms.web.api;

import com.krzelj.lms.security.jwt.JwtAuthenticationFilter;
import com.krzelj.lms.security.jwt.JwtService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ApiControllerTestSecurityConfig {

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter(JwtService jwtService) {
        return new JwtAuthenticationFilter(jwtService);
    }
}
