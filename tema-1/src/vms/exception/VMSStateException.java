package vms.exception;

@SuppressWarnings("serial")
public class VMSStateException extends VMSException {

	public VMSStateException(String format, Exception cause, Object... args) {
		super(format, cause, (Object[])args);
	}

	public VMSStateException(String format, Object... args) {
		this(format, null, (Object[])args);
	}

}