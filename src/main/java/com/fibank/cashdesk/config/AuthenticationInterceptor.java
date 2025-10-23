package com.fibank.cashdesk.config;

import com.fibank.cashdesk.exception.UnauthorizedException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Interceptor for validating FIB-X-AUTH header on all requests.
 */
@Component
public class AuthenticationInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(AuthenticationInterceptor.class);

    @Value("${cashdesk.security.api-key}")
    private String validApiKey;

    @Value("${cashdesk.security.header-name}")
    private String headerName;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String apiKey = request.getHeader(headerName);

        if (apiKey == null || apiKey.isBlank()) {
            log.warn("Unauthorized request: missing {} header from IP {}", headerName, request.getRemoteAddr());
            throw new UnauthorizedException("Missing authentication header: " + headerName);
        }

        if (!validApiKey.equals(apiKey)) {
            log.warn("Unauthorized request: invalid API key from IP {}", request.getRemoteAddr());
            throw new UnauthorizedException("Invalid API key");
        }

        return true;
    }
}
