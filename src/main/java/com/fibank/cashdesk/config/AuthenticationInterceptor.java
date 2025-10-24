package com.fibank.cashdesk.config;

import com.fibank.cashdesk.exception.UnauthorizedException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Interceptor for validating FIB-X-AUTH header on all requests.
 * Uses SHA-256 hashing to avoid storing plaintext API keys in configuration.
 */
@Component
public class AuthenticationInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(AuthenticationInterceptor.class);

    @Value("${cashdesk.security.api-key-hash}")
    private String validApiKeyHash;

    @Value("${cashdesk.security.header-name}")
    private String headerName;

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull Object handler) {
        String apiKey = request.getHeader(headerName);
        String clientIp = getClientIpAddress(request);

        if (apiKey == null || apiKey.isBlank()) {
            log.warn("Unauthorized request: missing {} header from IP {}", headerName, clientIp);
            throw new UnauthorizedException("Missing authentication header: " + headerName);
        }

        String incomingKeyHash = hashApiKey(apiKey);
        if (!validApiKeyHash.equals(incomingKeyHash)) {
            log.warn("Unauthorized request: invalid API key from IP {}", clientIp);
            throw new UnauthorizedException("Invalid API key");
        }

        return true;
    }

    /**
     * Gets the client IP address in IPv4 format when possible.
     * Handles X-Forwarded-For header for proxied requests and converts IPv6 localhost to IPv4.
     *
     * @param request the HTTP request
     * @return the client IP address, preferring IPv4 format
     */
    private String getClientIpAddress(HttpServletRequest request) {
        // Check for X-Forwarded-For header (for requests behind proxy/load balancer)
        String ipAddress = request.getHeader("X-Forwarded-For");
        if (ipAddress != null && !ipAddress.isBlank()) {
            // X-Forwarded-For can contain multiple IPs (client, proxy1, proxy2, ...)
            // The first one is the original client IP
            ipAddress = ipAddress.split(",")[0].trim();
        } else {
            ipAddress = request.getRemoteAddr();
        }

        // Convert IPv6 localhost to IPv4 localhost
        if ("0:0:0:0:0:0:0:1".equals(ipAddress) || "::1".equals(ipAddress)) {
            return "127.0.0.1";
        }

        // For IPv6 addresses that are IPv4-mapped (::ffff:192.168.1.1), extract the IPv4 part
        if (ipAddress != null && ipAddress.startsWith("::ffff:")) {
            return ipAddress.substring(7);
        }

        return ipAddress;
    }

    /**
     * Hashes the API key using SHA-256.
     * @param apiKey the plaintext API key
     * @return the hexadecimal representation of the hash
     */
    private String hashApiKey(String apiKey) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(apiKey.getBytes(StandardCharsets.UTF_8));

            // Convert byte array to hexadecimal string
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            log.error("SHA-256 algorithm not available", e);
            throw new RuntimeException("Failed to hash API key", e);
        }
    }
}
