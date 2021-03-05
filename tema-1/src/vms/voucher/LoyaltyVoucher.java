package vms.voucher;

import java.util.StringJoiner;

import vms.exception.VMSArgumentException;

public final class LoyaltyVoucher extends Voucher {

	private static final String CODE_PREFIX = "LOYALTY";

	private float discount;

	public LoyaltyVoucher(int id, String email, int campaignId, float discount) throws VMSArgumentException {
		super(id, new StringJoiner(CODE_DELIMITER).add(CODE_PREFIX), email, campaignId);

		this.discount = discount;
	}

	public float getDiscount() {
		return discount;
	}

	@Override
	public String toString() {
		StringJoiner joiner = new StringJoiner(", ");

		joiner.add(super.toString());

		joiner.add("discount=" + discount);

		return LoyaltyVoucher.class.getSimpleName() + "[" + joiner + "]";
	}

}