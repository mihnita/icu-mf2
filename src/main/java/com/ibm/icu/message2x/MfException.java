package com.ibm.icu.message2x;

public class MfException extends RuntimeException {
    private static final long serialVersionUID = -7634219305388292407L;

    public MfException(String message) {
        super(message);
    }
    public MfException(Throwable cause) {
        super(cause);
    }
    public MfException(String message, Throwable cause) {
        super(message, cause);
    }
}
