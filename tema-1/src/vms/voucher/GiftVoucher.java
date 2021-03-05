package vms.voucher;

import java.util.StringJoiner;

import vms.exception.VMSArgumentException;

public final class GiftVoucher extends Voucher {

	private static final String CODE_PREFIX = "GIFT";

	private float amount;

	public GiftVoucher(int id, String email, int campaignId, float amount) throws VMSArgumentException {
		super(id, new StringJoiner(CODE_DELIMITER).add(CODE_PREFIX), email, campaignId);

		this.amount = amount;
	}

	public float getAmount() {
		return amount;
	}

	@Override
	public String toString() {
		StringJoiner joiner = new StringJoiner(", ");

		joiner.add(super.toString());

		joiner.add("amount=" + amount);

		return GiftVoucher.class.getSimpleName() + "[" + joiner + "]";
	}

}