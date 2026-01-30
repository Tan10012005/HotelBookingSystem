package com.hotelbookingsystem.config;

import com.hotelbookingsystem.entity.User;
import com.hotelbookingsystem.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.util.Optional;

//Interceptor kiểm tra session hiện tại
//Nếu user đang login rồi bị admin ban thì khi họ next request sẽ bị logout
@Component
public class AuthInterceptor implements HandlerInterceptor {

    @Autowired
    private UserRepository userRepository;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException {
        HttpSession session = request.getSession(false);
        if (session != null) {
            User user = (User) session.getAttribute("user");
            if (user != null) {
                Optional<User> latest = userRepository.findById(user.getId());
                if (latest.isPresent()) {
                    User u = latest.get();
                    if (u.getIsActive() == null || !u.getIsActive()) {
                        // user has been banned -> invalidate session and redirect to login with flag
                        session.invalidate();
                        response.sendRedirect(request.getContextPath() + "/login?banned=true");
                        return false;
                    }
                }
            }
        }
        return true;
    }
}