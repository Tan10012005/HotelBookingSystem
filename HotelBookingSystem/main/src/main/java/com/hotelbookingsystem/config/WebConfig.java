package com.hotelbookingsystem.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.*;


//Đăng ký interceptor trong Webconfig này để đánh chặn user nếu
//đang online trong hệ thống nhưng bị ban từ Admin or Database
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private AuthInterceptor authInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/rooms/**", "/booking/**", "/profile/**", "/orders/**", "/roomchange/**");
    }
}