package vms.exception;

@SuppressWarnings("serial")
public class VMSAccessException extends VMSException {

	public VMSAccessException(String format, Exception cause, Object... args) {
		super(format, cause, (Object[])args);
	}

	public VMSAccessException(String format, Object... args) {
		this(format, null, (Object[])args);
	}

}