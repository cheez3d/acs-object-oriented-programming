package vms.exception;

@SuppressWarnings("serial")
public class VMSException extends RuntimeException {

	public VMSException(String format, Exception cause, Object... args) {
		super(String.format(format, (Object[])args), cause);
	}

	public VMSException(String format, Object... args) {
		this(format, null, args);
	}

}