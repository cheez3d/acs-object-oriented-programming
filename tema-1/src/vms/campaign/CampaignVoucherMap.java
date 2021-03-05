package vms.campaign;

import java.util.Map;
import java.util.TreeMap;

import util.ArrayMap;

import vms.voucher.Voucher;

public final class CampaignVoucherMap extends ArrayMap<String, Map<Integer, Voucher>> {

	public boolean addVoucher(Voucher voucher) {
		Map<Integer, Voucher> userVoucherMap = get(voucher.getEmail());

		if (userVoucherMap == null) {
			userVoucherMap = new TreeMap<>();
		}

		if (userVoucherMap.get(voucher.getId()) != null) {
			return false;
		}

		userVoucherMap.put(voucher.getId(), voucher);

		return true;
	}

	@Override
	public Map<Integer, Voucher> put(String userEmail, Map<Integer, Voucher> userVoucherMap) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Map<Integer, Voucher> get(Object userEmail) {
		Map<Integer, Voucher> userVoucherMap = super.get(userEmail);

		if (userVoucherMap == null) {
			userVoucherMap = new TreeMap<>();

			super.put((String)userEmail, userVoucherMap);
		}

		return userVoucherMap;
	}

}