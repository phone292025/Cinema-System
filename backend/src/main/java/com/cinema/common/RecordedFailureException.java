package com.cinema.common;

import org.springframework.http.HttpStatus;

/**
 * A failure whose state changes (an expired booking, a failed payment) must still be committed.
 * Transactions that can raise it declare {@code noRollbackFor = RecordedFailureException.class}.
 */
public class RecordedFailureException extends ApiException {
    public RecordedFailureException(HttpStatus status, String message) {
        super(status, message);
    }
}
