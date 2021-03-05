package vms.notification;

import java.time.LocalDateTime;

import java.util.List;
import java.util.StringJoiner;

public final class Notification {

	private NotificationType type;

	LocalDateTime dateTime;

	private int campaignId;

	private List<Integer> voucherIdList;

	public Notification(
		NotificationType type,
		LocalDateTime dateTime,
		int campaignId
	) {
		this.type = type;

		this.dateTime = dateTime;

		this.campaignId = campaignId;
	}

	public Notification(Notification notification, List<Integer> voucherIdList) {
		this(notification.type, notification.dateTime, notification.campaignId);

		this.voucherIdList = voucherIdList;
	}

	public NotificationType getType() {
		return type;
	}

	public LocalDateTime getDateTime() {
		return dateTime;
	}

	public int getCampaignId() {
		return campaignId;
	}

	@Override
	public String toString() {
		StringJoiner joiner = new StringJoiner(", ");

		joiner.add("type=" + type);

		joiner.add("dateTime=" + dateTime);

		joiner.add("campaignId=" + campaignId);

		joiner.add("voucherIdList=" + voucherIdList);

		return Notification.class.getSimpleName() + "[" + joiner + "]";
	}

}