package vms.user;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.stream.Collectors;

import vms.VMS;

import vms.campaign.Campaign;

import vms.exception.VMSArgumentException;

import vms.notification.Notification;

import vms.voucher.Voucher;

public final class User implements Comparable<User> {

	private int id;

	private String name;
	private String email;
	private String password;

	private UserType type;

	UserVoucherMap voucherMap;

	List<Notification> notificationList;

	public User(int id, String name, String email, String password, UserType type) throws VMSArgumentException {
		try {
			User user = VMS.getInstance().getUser(id);

			throw new VMSArgumentException("%s already exists", user);
		} catch (VMSArgumentException e) {}

		this.id = id;

		this.name = name;
		this.email = email;
		this.password = password;

		this.type = type;

		voucherMap = new UserVoucherMap();

		notificationList = new LinkedList<>();
	}

	public int getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public String getEmail() {
		return email;
	}

	public String getPassword() {
		return password;
	}

	public UserType getType() {
		return type;
	}

	public int getCampaignCount() {
		return voucherMap.size();
	}

	public List<Campaign> getCampaigns() {
		List<Campaign> campaignList = voucherMap.entrySet().stream()
			// .filter(e -> !e.getValue().isEmpty())
			.map(e -> VMS.getInstance().getCampaign(e.getKey()))
			.collect(Collectors.toList());

		return Collections.unmodifiableList(campaignList);
	}

	public int getCampaignVoucherCount(int campaignId) throws VMSArgumentException {
		VMS.getInstance().getCampaign(campaignId); // verifica daca campania cu id-ul specificat exista

		return voucherMap.get(campaignId).size();
	}

	public List<Voucher> getCampaignVouchers(int campaignId) throws VMSArgumentException {
		VMS.getInstance().getCampaign(campaignId); // verifica daca campania cu id-ul specificat exista

		List<Voucher> voucherList = voucherMap.get(campaignId).values().stream()
			.collect(Collectors.toList());

		return Collections.unmodifiableList(voucherList);
	}

	public List<Voucher> getVouchers() {
		List<Voucher> voucherList = voucherMap.values().stream()
			.flatMap(m -> m.values().stream())
			.collect(Collectors.toList());

		return Collections.unmodifiableList(voucherList);
	}

	public void addVoucher(Voucher voucher) {
		voucherMap.addVoucher(voucher);
	}

	public int getNotificationCount() {
		return notificationList.size();
	}

	public List<Notification> getNotifications() {
		return Collections.unmodifiableList(notificationList);
	}

	public void addNotification(Notification notification) {
		Map<Integer, Voucher> campaignVoucherMap = voucherMap.get(notification.getCampaignId());

		List<Integer> voucherIdList = campaignVoucherMap.values().stream()
			.map(Voucher::getId)
			.collect(Collectors.toList());

		voucherIdList = Collections.unmodifiableList(voucherIdList);

		notificationList.add(new Notification(notification, voucherIdList));
	}

	@Override
	public String toString() {
		StringJoiner joiner = new StringJoiner(", ");

		joiner.add("id=" + id);

		joiner.add("name=" + name);
		joiner.add("email=" + email);
		joiner.add("password=" + "*".repeat(password.length()));

		joiner.add("type=" + type.toString());

		return User.class.getSimpleName() + "[" + joiner + "]";
	}

	@Override
	public int compareTo(User o) {
		return Integer.compare(id, o.id);
	}

}