package com.nm.commerce.pagedef.evaluator.product;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

import javax.servlet.ServletException;

import atg.servlet.DynamoHttpServletRequest;

import com.nm.commerce.catalog.ImageShot;
import com.nm.commerce.catalog.ImageShotHelper;
import com.nm.commerce.catalog.NMProduct;
import com.nm.commerce.pagedef.evaluator.AbstractShotHelper;

// package access
public class MobileProductShotHelper extends AbstractShotHelper {
  private static final String CATEGORY_PAGE_IMAGE = "mj"; // constant for now, needs to be a value in property file
  private static final MobileProductShotHelper INSTANCE = new MobileProductShotHelper();
  private ProductShotHelper mProductShotHelper;
  
  private MobileProductShotHelper() {
    mProductShotHelper = ProductShotHelper.getInstance();
    // private constructor to enforce singleton
  }
  
  public static MobileProductShotHelper getInstance() {
    return INSTANCE;
  }
  
  protected ImageShotHelper getImageShotHelper(DynamoHttpServletRequest request) {
    return (ImageShotHelper) request.resolveName("/nm/commerce/catalog/MobileImageShotHelperDynamic");
  }
  
  protected ArrayList<ImageShot> determineValidAlternateViewsAvailable(NMProduct product, boolean secure, ImageShotHelper imageShotHelper, String grpId) throws ServletException, IOException {
    return mProductShotHelper.determineValidAlternateViewsAvailable(product, secure, imageShotHelper, grpId);
  }
  
  public ImageShot getCategoryImageShot(NMProduct product, DynamoHttpServletRequest request) {
    ImageShotHelper imageShotHelper = this.getImageShotHelper(request);
    
    ImageShot imageShot = imageShotHelper.createImageShot(product, CATEGORY_PAGE_IMAGE, false);
    
    return imageShot;
  }
}
