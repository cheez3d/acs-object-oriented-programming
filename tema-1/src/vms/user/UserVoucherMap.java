package vms.user;

import java.util.Map;
import java.util.TreeMap;

import util.ArrayMap;

import vms.voucher.Voucher;

public final class UserVoucherMap extends ArrayMap<Integer, Map<Integer, Voucher>> {

	public boolean addVoucher(Voucher voucher) {
		Map<Integer, Voucher> campaignVoucherMap = get(voucher.getCampaignId());

		if (campaignVoucherMap.get(voucher.getId()) != null) {
			return false;
		}

		campaignVoucherMap.put(voucher.getId(), voucher);

		return true;
	}

	@Override
	public Map<Integer, Voucher> put(Integer campaignId, Map<Integer, Voucher> campaignVoucherMap) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Map<Integer, Voucher> get(Object campaignId) {
		Map<Integer, Voucher> campaignVoucherMap = super.get(campaignId);

		if (campaignVoucherMap == null) {
			campaignVoucherMap = new TreeMap<>();

			super.put((int)campaignId, campaignVoucherMap);
		}

		return campaignVoucherMap;
	}

}