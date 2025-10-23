package com.fibank.cashdesk;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Main application class for Cash Desk Module.
 */
@SpringBootApplication
@EnableScheduling
public class CashDeskApplication {

    private static final Logger log = LoggerFactory.getLogger(CashDeskApplication.class);

    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(CashDeskApplication.class, args);

        String port = context.getEnvironment().getProperty("server.port");
        log.info("=".repeat(60));
        log.info("Cash Desk Module started successfully on port {}", port);
        log.info("API endpoints:");
        log.info("  POST http://localhost:{}/api/v1/cash-operation", port);
        log.info("  GET  http://localhost:{}/api/v1/cash-balance", port);
        log.info("=".repeat(60));
    }
}
