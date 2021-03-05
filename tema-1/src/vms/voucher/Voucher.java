package vms.voucher;

import java.time.LocalDateTime;
import java.util.Random;
import java.util.StringJoiner;
import java.util.stream.IntStream;

import vms.VMS;
import vms.campaign.Campaign;
import vms.exception.VMSArgumentException;
import vms.exception.VMSStateException;

public abstract class Voucher {

	protected static final String CODE_DELIMITER = "-";
	private static final Random CODE_RANDOM = new Random();
	private static final int CODE_RANDOM_COUNT = 4;
	private static final int CODE_RANDOM_LENGTH = 5;

	private int id;

	private String code;

	private VoucherStatusType status;
	private LocalDateTime redemptionDateTime;

	private String email;

	private int campaignId;

	protected Voucher(int id, StringJoiner codeJoiner, String email, int campaignId) throws VMSArgumentException {
		Campaign campaign = VMS.getInstance().getCampaign(campaignId);

		{
			boolean exists = false;

			try {
				campaign.getVoucher(id);

				exists = true;
			} catch (VMSArgumentException e) {}

			if (exists) {
				throw new VMSArgumentException("Voucher with id %d already exists for campaign", id);
			}
		}

		VMS.getInstance().getUserFromEmail(email); // verifica daca adresa de email specificata exista
		VMS.getInstance().getCampaign(campaignId); // verifica daca campania cu id-ul specificat exista

		this.id = id;

		codeJoiner.add(Integer.toString(id));

		IntStream.rangeClosed(1, CODE_RANDOM_COUNT).forEach(i -> {
			codeJoiner.add(CODE_RANDOM.ints('A', 'Z'+1)
				.limit(CODE_RANDOM_LENGTH)
				.collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
				.toString());
		});

		this.code = codeJoiner.toString();

		this.status = VoucherStatusType.UNUSED;

		this.email = email;

		this.campaignId = campaignId;
	}

	public int getId() {
		return id;
	}

	public String getCode() {
		return code;
	}

	public VoucherStatusType getStatus() {
		return status;
	}

	public LocalDateTime getRedemptionDateTime() {
		return redemptionDateTime;
	}

	public String getEmail() {
		return email;
	}

	public int getCampaignId() {
		return campaignId;
	}

	public void use(LocalDateTime redemptionDateTime) throws VMSArgumentException, VMSStateException {
		Campaign campaign = VMS.getInstance().getCampaign(campaignId);

		switch (campaign.getStatus()) {
		case EXPIRED:
			throw new VMSStateException("Voucher campaign expired");

		case CANCELLED:
			throw new VMSStateException("Voucher campaign cancelled");

		default:
		}

		if (redemptionDateTime.compareTo(campaign.getEndDateTime()) >= 0) {
			throw new VMSStateException("Voucher expired");
		}

		switch (status) {
		case USED:
			throw new VMSStateException("Voucher already used");

		default:
		}

		this.status = VoucherStatusType.USED;
		this.redemptionDateTime = redemptionDateTime;
	}

	@Override
	public String toString() {
		StringJoiner joiner = new StringJoiner(", ");

		joiner.add("id=" + id);

		joiner.add("code=" + code);

		joiner.add("status=" + status);
		joiner.add("redemptionDateTime=" + redemptionDateTime);

		joiner.add("email=" + email);

		joiner.add("campaignId=" + campaignId);

		return Voucher.class.getSimpleName() + "[" + joiner + "]";
	}

}