package com.bloodnetwork.web.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Spring MVC configuration registering security and CSRF interceptors.
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;
    private final CsrfInterceptor csrfInterceptor;

    @Autowired
    public WebMvcConfig(AuthInterceptor authInterceptor, CsrfInterceptor csrfInterceptor) {
        this.authInterceptor = authInterceptor;
        this.csrfInterceptor = csrfInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // Register CSRF verification for all non-static web requests
        registry.addInterceptor(csrfInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns("/css/**", "/js/**", "/vendor/**", "/images/**", "/favicon.ico", "/favicon.svg", "/error", "/api/**");

        // Register authorization checks on role-protected routes and login
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/admin/**", "/hospital/**", "/donor/**", "/login");
    }
}
