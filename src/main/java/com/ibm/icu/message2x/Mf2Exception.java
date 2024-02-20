package com.ibm.icu.message2x;

public class Mf2Exception extends RuntimeException {
    private static final long serialVersionUID = -7634219305388292407L;

    public Mf2Exception(String message) {
        super(message);
    }
    public Mf2Exception(Throwable cause) {
        super(cause);
    }
    public Mf2Exception(String message, Throwable cause) {
        super(message, cause);
    }
}
