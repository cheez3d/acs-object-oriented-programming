package vms.exception;

@SuppressWarnings("serial")
public class VMSNotFoundException extends VMSException {

	public VMSNotFoundException(String format, Exception cause, Object... args) {
		super(format, cause, (Object[])args);
	}

	public VMSNotFoundException(String format, Object... args) {
		super(format, (Object[])args);
	}

}