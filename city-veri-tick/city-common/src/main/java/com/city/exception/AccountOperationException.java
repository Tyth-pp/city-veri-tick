package com.city.exception;

public class AccountOperationException extends IllegalArgumentException {
    public AccountOperationException(String message) {
        super(message);
    }
}
