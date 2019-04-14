package ru.rrr.cfg;

public class NodeConfigException extends Exception {
    public NodeConfigException() {
    }

    public NodeConfigException(String s) {
        super(s);
    }

    public NodeConfigException(String s, Throwable throwable) {
        super(s, throwable);
    }

    public NodeConfigException(Throwable throwable) {
        super(throwable);
    }

    public NodeConfigException(String s, Throwable throwable, boolean b, boolean b1) {
        super(s, throwable, b, b1);
    }
}
