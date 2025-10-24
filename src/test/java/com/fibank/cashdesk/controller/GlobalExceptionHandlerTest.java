package com.fibank.cashdesk.controller;

import com.fibank.cashdesk.dto.response.ErrorResponse;
import com.fibank.cashdesk.exception.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.List;
import java.util.Objects;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive tests for GlobalExceptionHandler.
 * Tests all exception handlers including file I/O and data corruption scenarios.
 */
@DisplayName("GlobalExceptionHandler Tests")
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler exceptionHandler;

    @BeforeEach
    void setUp() {
        exceptionHandler = new GlobalExceptionHandler();
    }

    // ===================== UnauthorizedException Tests =====================

    @Test
    @DisplayName("Should handle UnauthorizedException with 401 status")
    void shouldHandleUnauthorizedException() {
        UnauthorizedException exception = new UnauthorizedException("Invalid API key");

        ResponseEntity<ErrorResponse> response = exceptionHandler.handleUnauthorizedException(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        ErrorResponse body = Objects.requireNonNull(response.getBody());
        assertThat(body.getStatus()).isEqualTo(401);
        assertThat(body.getError()).isEqualTo("Unauthorized");
        assertThat(body.getMessage()).isEqualTo("Invalid API key");
    }

    @Test
    @DisplayName("Should handle UnauthorizedException with missing header message")
    void shouldHandleUnauthorizedExceptionWithMissingHeader() {
        UnauthorizedException exception = new UnauthorizedException("Missing FIB-X-AUTH header");

        ResponseEntity<ErrorResponse> response = exceptionHandler.handleUnauthorizedException(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        ErrorResponse body = Objects.requireNonNull(response.getBody());
        assertThat(body.getMessage()).contains("FIB-X-AUTH");
    }

    // ===================== InvalidCashierException Tests =====================

    @Test
    @DisplayName("Should handle InvalidCashierException with 404 status")
    void shouldHandleInvalidCashierException() {
        InvalidCashierException exception = new InvalidCashierException("Cashier JOHN not found");

        ResponseEntity<ErrorResponse> response = exceptionHandler.handleInvalidCashierException(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        ErrorResponse body = Objects.requireNonNull(response.getBody());
        assertThat(body.getStatus()).isEqualTo(404);
        assertThat(body.getError()).isEqualTo("Not Found");
        assertThat(body.getMessage()).isEqualTo("Cashier JOHN not found");
    }

    @Test
    @DisplayName("Should handle InvalidCashierException with various cashier names")
    void shouldHandleInvalidCashierExceptionWithVariousCashiers() {
        InvalidCashierException exception = new InvalidCashierException("Invalid cashier: UNKNOWN");

        ResponseEntity<ErrorResponse> response = exceptionHandler.handleInvalidCashierException(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        ErrorResponse body = Objects.requireNonNull(response.getBody());
        assertThat(body.getMessage()).contains("UNKNOWN");
    }

    // ===================== InvalidDenominationException Tests =====================

    @Test
    @DisplayName("Should handle InvalidDenominationException with 400 status")
    void shouldHandleInvalidDenominationException() {
        InvalidDenominationException exception = new InvalidDenominationException(
            "Invalid denomination: 25 is not valid for BGN"
        );

        ResponseEntity<ErrorResponse> response = exceptionHandler.handleInvalidDenominationException(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        ErrorResponse body = Objects.requireNonNull(response.getBody());
        assertThat(body.getStatus()).isEqualTo(400);
        assertThat(body.getError()).isEqualTo("Bad Request");
        assertThat(body.getMessage()).contains("Invalid denomination");
    }

    @Test
    @DisplayName("Should handle InvalidDenominationException for sum mismatch")
    void shouldHandleInvalidDenominationExceptionForSumMismatch() {
        InvalidDenominationException exception = new InvalidDenominationException(
            "Denomination sum does not match amount"
        );

        ResponseEntity<ErrorResponse> response = exceptionHandler.handleInvalidDenominationException(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        ErrorResponse body = Objects.requireNonNull(response.getBody());
        assertThat(body.getMessage()).contains("does not match amount");
    }

    // ===================== InsufficientFundsException Tests =====================

    @Test
    @DisplayName("Should handle InsufficientFundsException with 400 status")
    void shouldHandleInsufficientFundsException() {
        InsufficientFundsException exception = new InsufficientFundsException(
            "Insufficient funds for withdrawal"
        );

        ResponseEntity<ErrorResponse> response = exceptionHandler.handleInsufficientFundsException(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        ErrorResponse body = Objects.requireNonNull(response.getBody());
        assertThat(body.getStatus()).isEqualTo(400);
        assertThat(body.getError()).isEqualTo("Bad Request");
        assertThat(body.getMessage()).contains("Insufficient funds");
    }

    @Test
    @DisplayName("Should handle InsufficientFundsException for specific denomination")
    void shouldHandleInsufficientFundsExceptionForSpecificDenomination() {
        InsufficientFundsException exception = new InsufficientFundsException(
            "Insufficient 50 BGN notes. Available: 5, Required: 10"
        );

        ResponseEntity<ErrorResponse> response = exceptionHandler.handleInsufficientFundsException(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        ErrorResponse body = Objects.requireNonNull(response.getBody());
        assertThat(body.getMessage()).contains("50 BGN notes");
        assertThat(body.getMessage()).contains("Available: 5");
    }

    // ===================== InvalidDateRangeException Tests =====================

    @Test
    @DisplayName("Should handle InvalidDateRangeException with 400 status")
    void shouldHandleInvalidDateRangeException() {
        InvalidDateRangeException exception = new InvalidDateRangeException(
            "dateFrom must be before dateTo"
        );

        ResponseEntity<ErrorResponse> response = exceptionHandler.handleInvalidDateRangeException(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        ErrorResponse body = Objects.requireNonNull(response.getBody());
        assertThat(body.getStatus()).isEqualTo(400);
        assertThat(body.getError()).isEqualTo("Bad Request");
        assertThat(body.getMessage()).contains("dateFrom");
    }

    @Test
    @DisplayName("Should handle InvalidDateRangeException for various date issues")
    void shouldHandleInvalidDateRangeExceptionForVariousDateIssues() {
        InvalidDateRangeException exception = new InvalidDateRangeException(
            "Invalid date format"
        );

        ResponseEntity<ErrorResponse> response = exceptionHandler.handleInvalidDateRangeException(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        ErrorResponse body = Objects.requireNonNull(response.getBody());
        assertThat(body.getMessage()).isEqualTo("Invalid date format");
    }

    // ===================== FileStorageException Tests =====================

    @Test
    @DisplayName("Should handle FileStorageException with 500 status")
    void shouldHandleFileStorageException() {
        FileStorageException exception = new FileStorageException(
            "Failed to write to transaction file"
        );

        ResponseEntity<ErrorResponse> response = exceptionHandler.handleFileStorageException(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        ErrorResponse body = Objects.requireNonNull(response.getBody());
        assertThat(body.getStatus()).isEqualTo(500);
        assertThat(body.getError()).isEqualTo("Internal Server Error");
        assertThat(body.getMessage()).isEqualTo("An error occurred while processing your request");
    }

    @Test
    @DisplayName("Should handle FileStorageException with cause")
    void shouldHandleFileStorageExceptionWithCause() {
        FileStorageException exception = new FileStorageException(
            "Failed to create balance file",
            new java.io.IOException("Disk full")
        );

        ResponseEntity<ErrorResponse> response = exceptionHandler.handleFileStorageException(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        ErrorResponse body = Objects.requireNonNull(response.getBody());
        assertThat(body.getStatus()).isEqualTo(500);
        // Should not expose internal error details to client
        assertThat(body.getMessage()).isEqualTo("An error occurred while processing your request");
    }

    @Test
    @DisplayName("Should handle FileStorageException for read failures")
    void shouldHandleFileStorageExceptionForReadFailures() {
        FileStorageException exception = new FileStorageException(
            "Failed to read transactions from file"
        );

        ResponseEntity<ErrorResponse> response = exceptionHandler.handleFileStorageException(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        ErrorResponse body = Objects.requireNonNull(response.getBody());
        assertThat(body.getMessage()).isEqualTo("An error occurred while processing your request");
    }

    @Test
    @DisplayName("Should handle FileStorageException for invalid file path")
    void shouldHandleFileStorageExceptionForInvalidPath() {
        FileStorageException exception = new FileStorageException(
            "Invalid file path: /invalid/path/file.txt"
        );

        ResponseEntity<ErrorResponse> response = exceptionHandler.handleFileStorageException(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        ErrorResponse body = Objects.requireNonNull(response.getBody());
        assertThat(body.getError()).isEqualTo("Internal Server Error");
    }

    // ===================== DataCorruptionException Tests =====================

    @Test
    @DisplayName("Should handle DataCorruptionException with 500 status")
    void shouldHandleDataCorruptionException() {
        DataCorruptionException exception = new DataCorruptionException(
            "Transaction file is corrupted"
        );

        ResponseEntity<ErrorResponse> response = exceptionHandler.handleDataCorruptionException(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        ErrorResponse body = Objects.requireNonNull(response.getBody());
        assertThat(body.getStatus()).isEqualTo(500);
        assertThat(body.getError()).isEqualTo("Internal Server Error");
        assertThat(body.getMessage()).isEqualTo("Data corruption detected");
    }

    @Test
    @DisplayName("Should handle DataCorruptionException with cause")
    void shouldHandleDataCorruptionExceptionWithCause() {
        DataCorruptionException exception = new DataCorruptionException(
            "Balance data is inconsistent",
            new IllegalStateException("Negative balance detected")
        );

        ResponseEntity<ErrorResponse> response = exceptionHandler.handleDataCorruptionException(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        ErrorResponse body = Objects.requireNonNull(response.getBody());
        assertThat(body.getStatus()).isEqualTo(500);
        // Should not expose internal corruption details to client
        assertThat(body.getMessage()).isEqualTo("Data corruption detected");
    }

    @Test
    @DisplayName("Should handle DataCorruptionException for parsing errors")
    void shouldHandleDataCorruptionExceptionForParsingErrors() {
        DataCorruptionException exception = new DataCorruptionException(
            "Failed to parse denomination data"
        );

        ResponseEntity<ErrorResponse> response = exceptionHandler.handleDataCorruptionException(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        ErrorResponse body = Objects.requireNonNull(response.getBody());
        assertThat(body.getMessage()).isEqualTo("Data corruption detected");
    }

    @Test
    @DisplayName("Should handle DataCorruptionException for checksum mismatch")
    void shouldHandleDataCorruptionExceptionForChecksumMismatch() {
        DataCorruptionException exception = new DataCorruptionException(
            "File checksum mismatch detected"
        );

        ResponseEntity<ErrorResponse> response = exceptionHandler.handleDataCorruptionException(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        ErrorResponse body = Objects.requireNonNull(response.getBody());
        assertThat(body.getError()).isEqualTo("Internal Server Error");
    }

    // ===================== MethodArgumentNotValidException Tests =====================

    @Test
    @DisplayName("Should handle MethodArgumentNotValidException with validation errors")
    void shouldHandleValidationException() throws Exception {
        // Mock BindingResult
        BindingResult bindingResult = mock(BindingResult.class);
        FieldError fieldError1 = new FieldError("cashOperationRequest", "cashier", "must not be blank");
        FieldError fieldError2 = new FieldError("cashOperationRequest", "amount", "must be greater than 0");
        when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError1, fieldError2));

        // Create a proper MethodParameter (using any method from this test class as a dummy)
        java.lang.reflect.Method method = this.getClass().getDeclaredMethod("shouldHandleValidationException");
        org.springframework.core.MethodParameter methodParameter =
            new org.springframework.core.MethodParameter(method, -1);

        MethodArgumentNotValidException exception = new MethodArgumentNotValidException(methodParameter, bindingResult);

        ResponseEntity<ErrorResponse> response = exceptionHandler.handleValidationException(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        ErrorResponse body = Objects.requireNonNull(response.getBody());
        assertThat(body.getStatus()).isEqualTo(400);
        assertThat(body.getError()).isEqualTo("Bad Request");
        assertThat(body.getMessage()).isEqualTo("Validation failed");
        assertThat(body.getDetails()).isNotNull();
        assertThat(body.getDetails()).containsKey("validationErrors");
    }

    @Test
    @DisplayName("Should handle MethodArgumentNotValidException with multiple field errors")
    void shouldHandleValidationExceptionWithMultipleErrors() throws Exception {
        BindingResult bindingResult = mock(BindingResult.class);
        FieldError error1 = new FieldError("request", "cashier", "must not be blank");
        FieldError error2 = new FieldError("request", "amount", "must be greater than 0");
        FieldError error3 = new FieldError("request", "currency", "must not be null");
        when(bindingResult.getFieldErrors()).thenReturn(List.of(error1, error2, error3));

        // Create a proper MethodParameter
        java.lang.reflect.Method method = this.getClass().getDeclaredMethod("shouldHandleValidationExceptionWithMultipleErrors");
        org.springframework.core.MethodParameter methodParameter =
            new org.springframework.core.MethodParameter(method, -1);

        MethodArgumentNotValidException exception = new MethodArgumentNotValidException(methodParameter, bindingResult);

        ResponseEntity<ErrorResponse> response = exceptionHandler.handleValidationException(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        ErrorResponse body = Objects.requireNonNull(response.getBody());
        String validationErrors = (String) body.getDetails().get("validationErrors");
        assertThat(validationErrors).contains("cashier");
        assertThat(validationErrors).contains("amount");
        assertThat(validationErrors).contains("currency");
    }

    // ===================== General Exception Tests =====================

    @Test
    @DisplayName("Should handle unexpected general exceptions with 500 status")
    void shouldHandleGeneralException() {
        Exception exception = new RuntimeException("Unexpected error occurred");

        ResponseEntity<ErrorResponse> response = exceptionHandler.handleGeneralException(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        ErrorResponse body = Objects.requireNonNull(response.getBody());
        assertThat(body.getStatus()).isEqualTo(500);
        assertThat(body.getError()).isEqualTo("Internal Server Error");
        assertThat(body.getMessage()).isEqualTo("An unexpected error occurred");
    }

    @Test
    @DisplayName("Should handle NullPointerException as general exception")
    void shouldHandleNullPointerException() {
        Exception exception = new NullPointerException("Null value encountered");

        ResponseEntity<ErrorResponse> response = exceptionHandler.handleGeneralException(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        ErrorResponse body = Objects.requireNonNull(response.getBody());
        // Should not expose internal error details
        assertThat(body.getMessage()).isEqualTo("An unexpected error occurred");
    }

    @Test
    @DisplayName("Should handle IllegalStateException as general exception")
    void shouldHandleIllegalStateException() {
        Exception exception = new IllegalStateException("Invalid state");

        ResponseEntity<ErrorResponse> response = exceptionHandler.handleGeneralException(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        ErrorResponse body = Objects.requireNonNull(response.getBody());
        assertThat(body.getError()).isEqualTo("Internal Server Error");
    }

    // ===================== Edge Cases =====================

    @Test
    @DisplayName("Should handle exception with null message")
    void shouldHandleExceptionWithNullMessage() {
        UnauthorizedException exception = new UnauthorizedException(null);

        ResponseEntity<ErrorResponse> response = exceptionHandler.handleUnauthorizedException(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNotNull();
    }

    @Test
    @DisplayName("Should handle exception with empty message")
    void shouldHandleExceptionWithEmptyMessage() {
        InvalidCashierException exception = new InvalidCashierException("");

        ResponseEntity<ErrorResponse> response = exceptionHandler.handleInvalidCashierException(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        ErrorResponse body = Objects.requireNonNull(response.getBody());
        assertThat(body.getMessage()).isEmpty();
    }

    @Test
    @DisplayName("Should handle exception with very long message")
    void shouldHandleExceptionWithLongMessage() {
        String longMessage = "Error: " + "x".repeat(1000);
        FileStorageException exception = new FileStorageException(longMessage);

        ResponseEntity<ErrorResponse> response = exceptionHandler.handleFileStorageException(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        ErrorResponse body = Objects.requireNonNull(response.getBody());
        // Generic message used, not the long internal message
        assertThat(body.getMessage()).isEqualTo("An error occurred while processing your request");
    }
}
