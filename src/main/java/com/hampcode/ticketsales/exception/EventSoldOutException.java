package com.hampcode.ticketsales.exception;

public class EventSoldOutException extends RuntimeException {
    public EventSoldOutException(String message) {
        super(message);
    }
}
