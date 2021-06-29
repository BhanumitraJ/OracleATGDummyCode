package com.nm.commerce.pagedef.definition;

/**
 * Basic definition for a SiloMain4Carousel template page. Configuration for content productCarouselPath,
 * thumbnailParentIndex, etc.
 */
public class SiloTemplatePageDefinition extends ProductTemplatePageDefinition {

	private String thumbnailParentIndex;
	private Integer restrictElementCount;
	private Integer minimumNumberCategories;
	private String postText;
	private Boolean showTopText;
	private Boolean showBottomText;
	private Boolean showAdditionalText;
	private String refreshableIconPath;
	private String productCarouselPath;


	public void setThumbnailParentIndex(String thumbnailParentIndex) {
		this.thumbnailParentIndex = thumbnailParentIndex;
	}

	public String getThumbnailParentIndex() {
		return getValue(thumbnailParentIndex, basis, "getThumbnailParentIndex");
	}

	public void setRestrictElementCount(Integer restrictElementCount) {
		this.restrictElementCount = restrictElementCount;
	}

	public Integer getRestrictElementCount() {
		return getValue(restrictElementCount, basis, "getRestrictElementCount");
	}

	public Integer getMinimumNumberCategories() {
		return getValue(minimumNumberCategories, basis,
				"getMinimumNumberCategories");
	}

	public void setMinimumNumberCategories(Integer minimumNumberCat) {
		this.minimumNumberCategories = minimumNumberCat;
	}

	public void setPostText(String postText) {
		this.postText = postText;
	}

	public String getPostText() {
		return getValue(postText, basis, "getPostText");
	}

	public void setShowTopText(Boolean showTopText) {
		this.showTopText = showTopText;
	}

	public Boolean getShowTopText() {
		return getValue(showTopText, basis, "getShowTopText");
	}

	public void setShowBottomText(Boolean showBottomText) {
		this.showBottomText = showBottomText;
	}

	public Boolean getShowBottomText() {
		return getValue(showBottomText, basis, "getShowBottomText");
	}

	public void setShowAdditionalText(Boolean showAdditionalText) {
		this.showAdditionalText = showAdditionalText;
	}

	public Boolean getShowAdditionalText() {
		return getValue(showAdditionalText, basis, "getShowAdditionalText");
	}

	public void setRefreshableIconPath(String refreshableIconPath) {
		this.refreshableIconPath = refreshableIconPath;
	}

	public String getRefreshableIconPath() {
		return getValue(refreshableIconPath, basis, "getRefreshableIconPath");
	}

	/**
	 * @return the productCarouselPath
	 */
	public String getProductCarouselPath() {
		return getValue(productCarouselPath, basis, "getProductCarouselPath");
	}

	/**
	 * @param productCarouselPath
	 *            the productCarouselPath to set
	 */
	public void setProductCarouselPath(String productCarouselPath) {
		this.productCarouselPath = productCarouselPath;

	}
	
}
