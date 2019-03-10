package ru.rrr.tcp;

public class NetworkExchangeException extends Exception {
    public NetworkExchangeException() {
    }

    public NetworkExchangeException(String s) {
        super(s);
    }

    public NetworkExchangeException(String s, Throwable throwable) {
        super(s, throwable);
    }

    public NetworkExchangeException(Throwable throwable) {
        super(throwable);
    }

    public NetworkExchangeException(String s, Throwable throwable, boolean b, boolean b1) {
        super(s, throwable, b, b1);
    }
}
