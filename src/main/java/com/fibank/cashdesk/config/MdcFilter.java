package com.fibank.cashdesk.config;

import com.fibank.cashdesk.util.MdcUtil;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Filter to set up MDC (Mapped Diagnostic Context) for each HTTP request.
 * Generates correlation IDs for request tracing and ensures proper cleanup.
 */
@Component
@Order(1) // Execute before other filters
public class MdcFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(MdcFilter.class);
    private static final String CORRELATION_ID_HEADER = "X-Correlation-ID";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        try {
            if (request instanceof HttpServletRequest httpRequest) {
                // Try to get correlation ID from request header, or generate new one
                String correlationId = httpRequest.getHeader(CORRELATION_ID_HEADER);

                if (correlationId == null || correlationId.isBlank()) {
                    correlationId = MdcUtil.generateCorrelationId();
                    log.debug("Generated new correlation ID: {}", correlationId);
                } else {
                    MdcUtil.setCorrelationId(correlationId);
                    log.debug("Using correlation ID from header: {}", correlationId);
                }

                // Add correlation ID to response header for client tracking
                if (response instanceof jakarta.servlet.http.HttpServletResponse httpResponse) {
                    httpResponse.setHeader(CORRELATION_ID_HEADER, correlationId);
                }
            }

            // Continue with the request
            chain.doFilter(request, response);

        } finally {
            // Always clear MDC after request to prevent memory leaks in thread pools
            MdcUtil.clear();
        }
    }
}
