package com.nm.commerce.pagedef.evaluator.product;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

import javax.servlet.ServletException;

import com.nm.commerce.catalog.ImageShot;
import com.nm.commerce.catalog.ImageShotHelper;
import com.nm.commerce.catalog.NMProduct;
import com.nm.commerce.pagedef.evaluator.AbstractShotHelper;

public class QuickViewShotHelper extends AbstractShotHelper {
  
  @Override
  protected ArrayList<ImageShot> determineValidAlternateViewsAvailable(final NMProduct product, final boolean secure, final ImageShotHelper imageShotHelper, String grpId) throws ServletException, IOException {
    
    final ArrayList<ImageShot> altViewsList = new ArrayList<ImageShot>();
    final ArrayList<ImageShot> validAltViewsList = new ArrayList<ImageShot>();
    
    // Create a list of valid alternate imageShots
    final String[] typesList = imageShotHelper.getImageShotTypesList();
    final int length = typesList.length;
    for (int i = 0; i < length; i++) {
      final String currentImageShotType = typesList[i];
      final String currentImageKey = currentImageShotType + imageShotHelper.getAlternateImageSize();
      
      final boolean useDynamicImg = product.getFlgDynamicImageSpecialInstruction() || product.getFlgDynamicImageColor();
      if (useDynamicImg) {
        final ImageShot imageShot = imageShotHelper.createImageShot(product, currentImageKey, secure, useDynamicImg);
        if (imageShot != null) {
          altViewsList.add(imageShot);
        }
      } else {
        final ImageShot imageShot = imageShotHelper.createViewShotWithCustomMainSize(product, currentImageKey, imageShotHelper.getQuickViewImageSize(), secure);
        if (imageShot != null) {
          altViewsList.add(imageShot);
        }
      }
    }
    
    // duplicate check before adding to list here
    final Iterator<ImageShot> altViewsIterator = altViewsList.iterator();
    while (altViewsIterator.hasNext()) {
      final ImageShot altViewImageShot = altViewsIterator.next();
      if (!validAltViewsList.contains(altViewImageShot)) {
        validAltViewsList.add(altViewImageShot);
      }
    }
    
    return validAltViewsList;
  } // end determineValidAlternateViewsAvailable
  
}
