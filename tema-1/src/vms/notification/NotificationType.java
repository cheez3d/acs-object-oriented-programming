package vms.notification;

public enum NotificationType {

	EDIT("Edit"),
	CANCEL("Cancellation");

	private String string;

	private NotificationType(String string) {
		this.string = string;
	}

	@Override
	public String toString() {
		return string;
	}

}