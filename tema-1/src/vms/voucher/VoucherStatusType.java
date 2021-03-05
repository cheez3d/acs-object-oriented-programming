package vms.voucher;

public enum VoucherStatusType {

	USED("Used"),
	UNUSED("Unused");

	private String string;

	private VoucherStatusType(String string) {
		this.string = string;
	}

	@Override
	public String toString() {
		return string;
	}

}