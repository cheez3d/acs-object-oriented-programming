package vms.user;

public enum UserType {

	ADMIN("Administrator"),
	GUEST("Guest");

	private String string;

	private UserType(String string) {
		this.string = string;
	}

	@Override
	public String toString() {
		return string;
	}

}