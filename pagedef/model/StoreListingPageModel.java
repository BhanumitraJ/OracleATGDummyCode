package com.nm.commerce.pagedef.model;

import java.util.List;

import com.nm.common.StoreAndRestaurantDetails;

public class StoreListingPageModel extends StorePageModel {
	private StoreAndRestaurantDetails bgStoreDetails;
	private List<String> UnsupportedBrowers;
	private String storeMap;
	private String storePromo;

	public String getStoreMap() {
		return storeMap;
	}

	public void setStoreMap(String storeMap) {
		this.storeMap = storeMap;
	}

	public List<String> getUnsupportedBrowers() {
		return UnsupportedBrowers;
	}

	public void setUnsupportedBrowers(List<String> unsupportedBrowers) {
		UnsupportedBrowers = unsupportedBrowers;
	}

	public void setBgStoreDetails(StoreAndRestaurantDetails bgStoreDetails) {
		this.bgStoreDetails = bgStoreDetails;
	}

	public StoreAndRestaurantDetails getBgStoreDetails() {
		return bgStoreDetails;
	}

	public String getStorePromo() {
		return storePromo;
	}

	public void setStorePromo(String storePromo) {
		this.storePromo = storePromo;
	}

}
