package com.nm.commerce.pagedef.evaluator.product;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.servlet.ServletException;

import com.nm.abtest.AbTestHelper;
import com.nm.commerce.catalog.ImageShot;
import com.nm.commerce.catalog.ImageShotHelper;
import com.nm.commerce.catalog.NMProduct;
import com.nm.commerce.pagedef.evaluator.AbstractShotHelper;
import com.nm.utils.StringUtilities;

// package access
class ProductShotHelper extends AbstractShotHelper {

	private static final ProductShotHelper INSTANCE = new ProductShotHelper();

	private ProductShotHelper() {
		// private constructor to enforce singleton
	}

	public static ProductShotHelper getInstance() {
		return INSTANCE;
	}

  protected ArrayList<ImageShot> determineValidAlternateViewsAvailable(NMProduct product, boolean secure, ImageShotHelper imageShotHelper, String grpId) throws ServletException, IOException {

		Set<ImageShot> altViewsList = new LinkedHashSet<ImageShot>();
		String[] typesList = imageShotHelper.getImageShotTypesList();
   // imageShotHelper.setProductDetailPage(this.isProductDetailPage());
		// Create a list of valid alternate imageShots
		int length = typesList.length;
		boolean imageAvailable = false;
		boolean useDynamicImg = product.getFlgDynamicImageSpecialInstruction() || product.getFlgDynamicImageColor();
		for (int i = 0; i < length; i++) {
			String currentImageShotType = typesList[i];
			String currentImageShotLabel = currentImageShotType + imageShotHelper.getAlternateImageSize();
			ImageShot imageShot = imageShotHelper.createImageShot(product, currentImageShotLabel, secure, useDynamicImg);
			if (imageShot != null) {
				imageAvailable = true;
				//System.out.println("imageShot url"+imageShot.getProdMainImageURL());
				altViewsList.add(imageShot);
			}
		}
		if(grpId != null && !imageAvailable){
			NMProduct nmproduct = new NMProduct(grpId);
			useDynamicImg = nmproduct.getFlgDynamicImageSpecialInstruction() || nmproduct.getFlgDynamicImageColor();
			for (int i = 0; i < length; i++) {
				String currentImageShotType = typesList[i];
				String currentImageShotLabel = currentImageShotType + imageShotHelper.getAlternateImageSize();
				ImageShot imageShot = imageShotHelper.createImageShot(nmproduct, currentImageShotLabel, secure, useDynamicImg);
				if (imageShot != null) {
					imageAvailable = true;
					//System.out.println("Group imageShot url"+imageShot.getProdMainImageURL());
					altViewsList.add(imageShot);
				}
			}
		}
		

		if (imageShotHelper.isProductPageVideoEnabled() && isVideoAbTestActive()) {
			String videoMainImageSize = imageShotHelper.getVideoMainImageSize();
			String videoAltImageSize = imageShotHelper.getVideoAltImageSize();
			String videoImageShotLabel = imageShotHelper.getVideoImageType() + imageShotHelper.getAlternateImageSize();
			if (StringUtilities.isNotEmpty(videoAltImageSize)) {
				//Video image override available. Use the Video Alt Image Size.
				videoImageShotLabel = imageShotHelper.getVideoImageType() + videoAltImageSize;
			}
			ImageShot videoShot = imageShotHelper.createVideoShot(product, videoImageShotLabel, secure, videoMainImageSize, videoAltImageSize);
			if (videoShot != null) {
				altViewsList.add(videoShot);
			}
		}

		return new ArrayList<ImageShot>(altViewsList);
	} // end determineValidAlternateViewsAvailable

	// Remove this method and any references to it when the Video ABTest is over.
	private boolean isVideoAbTestActive() {
		String videoAbTestGroup = AbTestHelper.getAbTestValue(atg.servlet.ServletUtil.getCurrentRequest(), "videoABTest");
		return (!(videoAbTestGroup != null && videoAbTestGroup.equals("B")));
	}
}
