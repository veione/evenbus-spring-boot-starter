package com.think.event;

/**
 * An {@link RuntimeException} thrown in cases something went wrong inside EventBus.
 * 
 * @author Markus
 * 
 */
public class EventBusException extends RuntimeException {

    private static final long serialVersionUID = -2912559384646531479L;

    public EventBusException(String detailMessage) {
        super(detailMessage);
    }

    public EventBusException(Throwable throwable) {
        super(throwable);
    }

    public EventBusException(String detailMessage, Throwable throwable) {
        super(detailMessage, throwable);
    }

}
