package com.life.train.err;



public class AppException extends Exception {
    private static final long serialVersionUID = -6923532577521768266L;
    public final String code;

    private final static String CODE_UNKNOWN = "remote_exception_unknown";

    public AppException(String code, String details) {
        super(details);
        this.code = code;
    }

    public AppException(String details) {
        super(details);
        code = CODE_UNKNOWN;
    }


    public AppException(String details, Throwable inner) {
        super(details, inner);
        code = CODE_UNKNOWN;
    }

}
