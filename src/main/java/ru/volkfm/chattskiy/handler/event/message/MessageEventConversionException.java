package ru.volkfm.chattskiy.handler.event.message;

import org.springframework.core.convert.ConversionException;

public class MessageEventConversionException extends ConversionException {
    public MessageEventConversionException() {
        super("Message event conversion failed");
    }
}
