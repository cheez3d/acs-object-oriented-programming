package vms.exception;

@SuppressWarnings("serial")
public class VMSArgumentException extends VMSException {

	public VMSArgumentException(String format, Exception cause, Object... args) {
		super(format, cause, (Object[])args);
	}

	public VMSArgumentException(String format, Object... args) {
		this(format, null, (Object[])args);
	}

}