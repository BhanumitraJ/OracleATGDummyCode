package com.nm.commerce.pagedef.definition;

public class StoreAndRestaurantDirectoryPagedefinition extends PageDefinition {
	private String reservationLinkText;
	private int columnLength;
	private String storeMapbanner;
	private String storePromobanner;

	public String getStoreMapbanner() {
		return storeMapbanner;
	}

	public void setStoreMapbanner(String storeMapbanner) {
		this.storeMapbanner = storeMapbanner;
	}

	public String getStorePromobanner() {
		return storePromobanner;
	}

	public void setStorePromobanner(String storePromobanner) {
		this.storePromobanner = storePromobanner;
	}

	public int getColumnLength() {
		return columnLength;
	}

	public void setColumnLength(final int columnLength) {
		this.columnLength = columnLength;
	}

	public String getReservationLinkText() {
		return reservationLinkText;
	}

	public void setReservationLinkText(final String reservationLinkText) {
		this.reservationLinkText = reservationLinkText;
	}
}
