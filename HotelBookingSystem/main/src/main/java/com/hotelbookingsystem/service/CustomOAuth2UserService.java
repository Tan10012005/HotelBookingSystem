package com.hotelbookingsystem.service;

import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        System.out.println("===== GOOGLE USER INFO =====");
        System.out.println("Email: " + oAuth2User.getAttribute("email"));
        System.out.println("Name: " + oAuth2User.getAttribute("name"));
        System.out.println("============================");

        return oAuth2User;
    }
}