package com.fibank.cashdesk.config;

import com.fibank.cashdesk.util.MdcUtil;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
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
@Order(1)
public class MdcFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(MdcFilter.class);
    private static final String CORRELATION_ID_HEADER = "X-Correlation-ID";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        try {
            if (request instanceof HttpServletRequest httpRequest) {
                String correlationId = httpRequest.getHeader(CORRELATION_ID_HEADER);

                if (correlationId == null || correlationId.isBlank()) {
                    correlationId = MdcUtil.generateCorrelationId();
                    log.debug("Generated new correlation ID: {}", correlationId);
                } else {
                    MdcUtil.setCorrelationId(correlationId);
                    log.debug("Using correlation ID from header: {}", correlationId);
                }

                if (response instanceof jakarta.servlet.http.HttpServletResponse httpResponse) {
                    httpResponse.setHeader(CORRELATION_ID_HEADER, correlationId);
                }
            }

            chain.doFilter(request, response);

        } finally {
            MdcUtil.clear();
        }
    }
}
