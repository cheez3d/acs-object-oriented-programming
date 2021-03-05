package vms.campaign;

public enum CampaignStatusType {

	NEW("New"),
	STARTED("Started"),
	EXPIRED("Expired"),
	CANCELLED("Cancelled");

	private String string;

	private CampaignStatusType(String string) {
		this.string = string;
	}

	@Override
	public String toString() {
		return string;
	}

}