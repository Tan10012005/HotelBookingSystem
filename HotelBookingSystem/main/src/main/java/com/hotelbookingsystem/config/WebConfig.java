package com.hotelbookingsystem.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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


    @Value("${app.upload.cccd-dir:uploads/cccd}")
    private String cccdUploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Cho phép truy cập file uploads qua URL /uploads/cccd/**
        registry.addResourceHandler("/uploads/cccd/**")
                .addResourceLocations("file:" + cccdUploadDir + "/");
    }
}