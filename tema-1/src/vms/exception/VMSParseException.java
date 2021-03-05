package vms.exception;

@SuppressWarnings("serial")
public class VMSParseException extends VMSException {

	public VMSParseException(String format, Exception cause, Object... args) {
		super(format, cause, (Object[])args);
	}

	public VMSParseException(String format, Object... args) {
		this(format, null, (Object[])args);
	}

}