package com.geemodule;

public class ModuleException extends RuntimeException {
    private static final long serialVersionUID = -8765671956898254621L;

    public ModuleException() {
	super();
    }

    public ModuleException(final String message, final Throwable cause) {
	super(message, cause);
    }

    public ModuleException(final String message) {
	super(message);
    }

    public ModuleException(final Throwable cause) {
	super(cause);
    }
}