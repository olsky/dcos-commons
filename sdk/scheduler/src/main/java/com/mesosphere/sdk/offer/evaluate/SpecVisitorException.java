package com.mesosphere.sdk.offer.evaluate;

public class SpecVisitorException extends Exception {

    public SpecVisitorException(Throwable ex) {
        super(ex);
    }

    public SpecVisitorException(String msg) {
        super(msg);
    }

    public SpecVisitorException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
