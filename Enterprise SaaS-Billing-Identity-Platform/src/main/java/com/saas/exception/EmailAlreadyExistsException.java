package com.saas.exception;

public class EmailAlreadyExistsException extends RuntimeException {

    public EmailAlreadyExistsException(String email) {
        super("Account already exists with email: " + email);
    }
}
