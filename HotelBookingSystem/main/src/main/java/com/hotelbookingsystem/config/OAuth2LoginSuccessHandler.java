package com.hotelbookingsystem.config;

import com.hotelbookingsystem.entity.User;
import com.hotelbookingsystem.repository.UserRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;

@Component
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    @Autowired
    private UserRepository userRepository;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication)
            throws IOException, ServletException {

        try {
            OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
            String email = oAuth2User.getAttribute("email");
            String fullName = oAuth2User.getAttribute("name");

            System.out.println("===== OAuth2 SUCCESS HANDLER =====");
            System.out.println("Email: " + email);
            System.out.println("Name: " + fullName);

            // Tìm user trong DB
            User user = userRepository.findByEmail(email);

            if (user == null) {
                // Tạo user mới
                user = User.builder()
                        .email(email)
                        .password("GOOGLE_OAUTH2")
                        .fullName(fullName)
                        .role("USER")
                        .isActive(true)
                        .createdAt(LocalDateTime.now())
                        .build();
                user = userRepository.save(user);
                System.out.println("✅ Created new Google user: " + email);
            } else {
                System.out.println("✅ Existing user found: " + email);

                // Cập nhật fullName nếu chưa có
                if (user.getFullName() == null || user.getFullName().isBlank()) {
                    user.setFullName(fullName);
                    user = userRepository.save(user);
                }
            }

            // Kiểm tra banned
            if (user.getIsActive() == null || !user.getIsActive()) {
                System.out.println("⛔ User is banned: " + email);
                response.sendRedirect("/login?banned=true");
                return;
            }

            // ✅ SET SESSION — giống hệt flow login thường
            HttpSession session = request.getSession();
            session.setAttribute("user", user);

            System.out.println("✅ Session set, redirecting to /rooms");
            System.out.println("==================================");

            response.sendRedirect("/rooms");

        } catch (Exception e) {
            System.out.println("❌ ERROR in OAuth2LoginSuccessHandler:");
            e.printStackTrace();
            response.sendRedirect("/login?error=google_failed");
        }
    }
}