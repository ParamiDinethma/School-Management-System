package com.wsims.config;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
public class CustomAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                      Authentication authentication) throws IOException, ServletException {
        
        String redirectUrl = "/dashboard"; // Default redirect
        
        if (authentication.getAuthorities().contains(new SimpleGrantedAuthority("STUDENT"))) {
            redirectUrl = "/student/dashboard";
        } else if (authentication.getAuthorities().contains(new SimpleGrantedAuthority("TEACHER"))) {
            redirectUrl = "/teacher/dashboard"; // Redirect teachers to teacher dashboard
        } else if (authentication.getAuthorities().contains(new SimpleGrantedAuthority("PRINCIPAL"))) {
            redirectUrl = "/dashboard";
        } else if (authentication.getAuthorities().contains(new SimpleGrantedAuthority("IT_ADMIN"))) {
            redirectUrl = "/dashboard";
        } else if (authentication.getAuthorities().contains(new SimpleGrantedAuthority("REGISTRAR"))) {
            redirectUrl = "/dashboard";
        } else if (authentication.getAuthorities().contains(new SimpleGrantedAuthority("PARENT"))) {
            redirectUrl = "/parent/dashboard";
        }
        
        response.sendRedirect(redirectUrl);
    }
}
