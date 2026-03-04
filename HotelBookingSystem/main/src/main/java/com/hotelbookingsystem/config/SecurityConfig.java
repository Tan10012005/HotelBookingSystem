package com.hotelbookingsystem.config;

import com.hotelbookingsystem.service.CustomOAuth2UserService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;

import java.io.IOException;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private CustomOAuth2UserService customOAuth2UserService;

    @Autowired
    private OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())

                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/", "/login", "/register",
                                "/css/**", "/js/**", "/images/**",
                                "/oauth2/**", "/login/oauth2/**"
                        ).permitAll()
                        .requestMatchers("/admin/**").permitAll()
                        .anyRequest().permitAll()
                )

                .oauth2Login(oauth2 -> oauth2
                        .loginPage("/login")

                        .userInfoEndpoint(userInfo -> userInfo
                                .userService(customOAuth2UserService)
                        )

                        .successHandler(oAuth2LoginSuccessHandler)

                        // ✅ THÊM ĐOẠN NÀY — Log lỗi OAuth ra console
                        .failureHandler(new AuthenticationFailureHandler() {
                            @Override
                            public void onAuthenticationFailure(HttpServletRequest request,
                                                                HttpServletResponse response,
                                                                AuthenticationException exception)
                                    throws IOException, ServletException {

                                // ===== IN LỖI RA CONSOLE =====
                                System.out.println("========== OAUTH2 LOGIN FAILED ==========");
                                System.out.println("Error: " + exception.getMessage());
                                exception.printStackTrace();
                                System.out.println("==========================================");

                                // Redirect về login với error message
                                response.sendRedirect("/login?error=oauth2&message=" +
                                        java.net.URLEncoder.encode(exception.getMessage(), "UTF-8"));
                            }
                        })
                )

                .formLogin(form -> form.disable());

        return http.build();
    }
}