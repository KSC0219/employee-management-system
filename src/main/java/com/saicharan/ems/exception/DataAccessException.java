package com.saicharan.ems.exception;

/** Unchecked wrapper around SQLException so callers are not forced to handle JDBC details. */
public class DataAccessException extends RuntimeException {
    public DataAccessException(String message, Throwable cause) {
        super(message, cause);
    }
}
