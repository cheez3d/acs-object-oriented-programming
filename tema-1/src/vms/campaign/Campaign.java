package vms.campaign;

import java.time.LocalDateTime;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.StringJoiner;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import vms.VMS;

import vms.exception.VMSArgumentException;
import vms.exception.VMSStateException;

import vms.notification.Notification;
import vms.notification.NotificationType;

import vms.user.User;

import vms.voucher.GiftVoucher;
import vms.voucher.LoyaltyVoucher;
import vms.voucher.Voucher;

public final class Campaign implements Comparable<Campaign> {

	private int id;

	private String name;
	private String description;

	private LocalDateTime startDateTime;
	private LocalDateTime endDateTime;

	private int totalVoucherCount;
	private int availableVoucherCount;

	private CampaignStatusType status;

	private CampaignVoucherMap voucherMap;
	private int voucherCount;
	private List<User> observerList;

	private CampaignStrategyType strategy;

	public Campaign(
		int id,
		String name, String description,
		LocalDateTime startDateTime, LocalDateTime endDateTime,
		int totalVoucherCount,
		CampaignStrategyType strategy
	) throws VMSArgumentException {
		if (strategy != null) {
			boolean exists = false;

			try {
				VMS.getInstance().getCampaign(id);

				exists = true;
			} catch (VMSArgumentException e) {}

			if (exists) {
				throw new VMSArgumentException("Campaign with id %d already exists", id);
			}
		}

		if (startDateTime.compareTo(endDateTime) >= 0) {
			throw new VMSArgumentException("Campaign start date-time must be less than campaign end date-time");
		}

		if (totalVoucherCount < 0) {
			throw new VMSArgumentException("Campaign budget cannot be negative");
		}

		this.id = id;

		this.name = name;
		this.description = description;

		this.startDateTime = startDateTime;
		this.endDateTime = endDateTime;

		this.totalVoucherCount = totalVoucherCount;
		this.availableVoucherCount = totalVoucherCount;

		updateStatus();

		voucherMap = new CampaignVoucherMap();
		observerList = new LinkedList<>();

		this.strategy = strategy;
	}

	public Campaign(
		int id,
		String name, String description,
		LocalDateTime startDateTime, LocalDateTime endDateTime,
		int totalVoucherCount
	) throws VMSArgumentException {
		this(id, name, description, startDateTime, endDateTime, totalVoucherCount, null);
	}

	public Campaign(Campaign campaign) {
		this(
			campaign.id,
			campaign.name, campaign.description,
			campaign.startDateTime, campaign.endDateTime,
			campaign.totalVoucherCount
		);
	}

	public int getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public LocalDateTime getStartDateTime() {
		return startDateTime;
	}

	public void setStartDateTime(LocalDateTime startDateTime) throws VMSArgumentException {
		if (startDateTime.compareTo(endDateTime) >= 0) {
			throw new VMSArgumentException("Campaign start date-time must be before campaign end date-time");
		}

		this.startDateTime = startDateTime;
	}

	public LocalDateTime getEndDateTime() {
		return endDateTime;
	}

	public void setEndDateTime(LocalDateTime endDateTime) throws VMSArgumentException {
		if (endDateTime.compareTo(startDateTime) <= 0) {
			throw new VMSArgumentException("Campaign end date-time must be after campaign start date-time");
		}

		this.endDateTime = endDateTime;
	}

	public int getTotalVoucherCount() {
		return totalVoucherCount;
	}

	public void setTotalVoucherCount(int totalVoucherCount) throws VMSArgumentException {
		if (totalVoucherCount < this.totalVoucherCount) {
			throw new VMSArgumentException("Campaign budget cannot decrease");
		}

		availableVoucherCount += totalVoucherCount-this.totalVoucherCount;

		this.totalVoucherCount = totalVoucherCount;
	}

	public int getAvailableVoucherCount() {
		return availableVoucherCount;
	}

	public CampaignStatusType getStatus() {
		return status;
	}

	public void updateStatus() {
		if (status == CampaignStatusType.CANCELLED) {
			return;
		}

		LocalDateTime dateTime = VMS.getInstance().getDateTime();

		if (dateTime.compareTo(startDateTime) >= 0) {
			if (dateTime.compareTo(endDateTime) < 0) {
				status = CampaignStatusType.STARTED;
			} else {
				status = CampaignStatusType.EXPIRED;
			}
		} else {
			status = CampaignStatusType.NEW;
		}
	}

	public CampaignStrategyType getStrategy() {
		return strategy;
	}

	public void setStrategy(CampaignStrategyType strategy) {
		this.strategy = strategy;
	}

	public Voucher executeStrategy() throws VMSStateException {
		Comparator<User> compareVoucherCount = (u1, u2) -> u1.getCampaignVouchers(id).size()-u2.getCampaignVouchers(id).size();

		Voucher voucher;

		switch (strategy) {
		default:
			String userEmail;

		case A:
			userEmail = observerList.stream()
				.sorted((u1, u2) -> ThreadLocalRandom.current().nextInt(-1, 1+1))
				.findAny()
				.orElseThrow(() -> new VMSStateException("No suitable observer found"))
				.getEmail();

			voucher = generateVoucher(userEmail, "GiftVoucher",  100);

			break;

		case B:
			userEmail = observerList.stream()
				.max(compareVoucherCount)
				.orElseThrow(() -> new VMSStateException("No suitable observer found"))
				.getEmail();

			voucher = generateVoucher(userEmail, "LoyaltyVoucher", 50);

			break;

		case C:
			userEmail = observerList.stream()
				.min(compareVoucherCount)
				.orElseThrow(() -> new VMSStateException("No suitable observer found"))
				.getEmail();

			voucher = generateVoucher(userEmail, "GiftVoucher",  100);
		}

		return voucher;
	}

	public void update(Campaign ref) throws VMSArgumentException, VMSStateException {
		switch (status) {
		case NEW:
			setName(ref.name);
			setDescription(ref.description);

			setStartDateTime(ref.startDateTime);


			if (ref.strategy != null) {
				setStrategy(ref.strategy);
			}

		case STARTED:
			setEndDateTime(ref.endDateTime);

			setTotalVoucherCount(ref.totalVoucherCount);

			break;

		default:
			throw new VMSStateException("Campaign cannot be edited");
		}

		updateStatus();

		notifyAllObservers(new Notification(NotificationType.EDIT, VMS.getInstance().getDateTime(), id));
	}

	public void cancel() throws VMSStateException {
		switch (status) {
		case NEW:
		case STARTED:
			status = CampaignStatusType.CANCELLED;
			break;

		default:
			throw new VMSStateException("Campaign cannot be cancelled");
		}

		notifyAllObservers(new Notification(NotificationType.CANCEL, VMS.getInstance().getDateTime(), id));
	}

	public List<Voucher> getVouchers() {
		List<Voucher> voucherList = voucherMap.values().stream()
			.flatMap(m -> m.values().stream())
			.collect(Collectors.toList());

		return Collections.unmodifiableList(voucherList);
	}

	public Voucher getVoucher(int id) throws VMSArgumentException {
		return getVouchers().stream()
			.filter(v -> v.getId() == id)
			.findFirst()
			.orElseThrow(() -> new VMSArgumentException("No voucher with id %d for %s", id, this));
	}

	public Voucher generateVoucher(String email, String type, float value) throws VMSArgumentException, VMSStateException {
		switch (status) {
		case EXPIRED:
			throw new VMSStateException("Campaign expired");

		case CANCELLED:
			throw new VMSStateException("Campaign cancelled");

		default:
		}

		if (availableVoucherCount <= 0) {
			throw new VMSStateException("Campaign budget exhausetd");
		}

		Voucher voucher;

		switch (type) {
		case "GiftVoucher":
			voucher = new GiftVoucher(++voucherCount, email, id, value);
			break;

		case "LoyaltyVoucher":
			voucher = new LoyaltyVoucher(++voucherCount, email, id, value);
			break;

		default:
			throw new VMSArgumentException(String.format("Bad voucher type '%s'", type));
		}

		--availableVoucherCount;

		voucherMap.addVoucher(voucher);

		String observerEmail = voucher.getEmail();

		User observer = VMS.getInstance().getUserFromEmail(observerEmail);

		observer.addVoucher(voucher);

		addObserver(observer);

		return voucher;
	}

	public void redeemVoucher(int id, LocalDateTime dateTime) throws VMSArgumentException, VMSStateException {
		Voucher voucher = getVoucher(id);

		voucher.use(dateTime);
	}

	public int getObserverCount() {
		return observerList.size();
	}

	public List<User> getObservers() {
		return Collections.unmodifiableList(observerList);
	}

	public void addObserver(User observer) {
		if (observerList.contains(observer)) {
			return;
		}

		observerList.add(observer);
	}

	public void removeObserver(User observer) {
		observerList.remove(observer);
	}

	public void notifyAllObservers(Notification notification) {
		observerList.forEach(o -> o.addNotification(notification));
	}

	@Override
	public String toString() {
		StringJoiner joiner = new StringJoiner(", ");

		joiner.add("id=" + id);

		joiner.add("name=" + name);
		joiner.add("description=" + description);

		joiner.add("startDateTime=" + startDateTime);
		joiner.add("endDateTime=" + endDateTime);

		joiner.add("totalVoucherCount=" + totalVoucherCount);
		joiner.add("availableVoucherCount=" + availableVoucherCount);

		joiner.add("status=" + status);

		joiner.add("strategy=" + strategy);

		return Campaign.class.getSimpleName() + "[" + joiner + "]";
	}

	@Override
	public int compareTo(Campaign o) {
		return Integer.compare(id, o.id);
	}

}