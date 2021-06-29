package com.nm.commerce.pagedef.evaluator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.servlet.ServletException;

import org.springframework.util.CollectionUtils;

import atg.repository.RepositoryItem;
import atg.servlet.DynamoHttpServletRequest;

import com.nm.abtest.AbTestHelper;
import com.nm.commerce.catalog.ImageShot;
import com.nm.commerce.catalog.ImageShotHelper;
import com.nm.commerce.catalog.NMProduct;
import com.nm.components.CommonComponentHelper;
import com.nm.utils.StringUtilities;

public abstract class AbstractShotHelper {
  
  public ArrayList<ImageShot> setupProductImageShots(NMProduct product, DynamoHttpServletRequest request) throws ServletException, IOException {
    
    ImageShotHelper imageShotHelper = getImageShotHelper(request);
    
    boolean secure = "1".equals(request.getHeader("ISSECURE"));
    imageShotHelper.setUseSecureProtocol(secure);
    
    final String grpId = request.getParameter("grpId");
    
    // Load up valid alternate image shots according to business rules
    // specified through the values in properties file
    return getAlternateViewImages(product, secure, imageShotHelper, grpId, request);
  }
  
  protected ImageShotHelper getImageShotHelper(DynamoHttpServletRequest request) {
    return (ImageShotHelper) request.resolveName("/nm/commerce/catalog/ImageShotHelperDynamic");
  }
  
  @SuppressWarnings("unchecked")
  private ArrayList<ImageShot> getAlternateViewImages(NMProduct product, boolean secure, ImageShotHelper imageShotHelper, String grpId, DynamoHttpServletRequest request) throws ServletException,
          IOException {
    int prodType = product.getType();
    ArrayList<ImageShot> validAlternateViewsList = new ArrayList<ImageShot>();
    
    loadValidAlternateViewsList(product, secure, imageShotHelper, validAlternateViewsList, grpId);

    String elimsGroup = "";
    List<RepositoryItem> subProductsList = null;
    if (request != null) {
      elimsGroup = AbTestHelper.getAbTestValue(request, AbTestHelper.ELIMS_SUITE_GROUP);
    }
	if (prodType == 0 && StringUtilities.isNotEmpty(elimsGroup) && StringUtilities.areEqualIgnoreCase(elimsGroup, "Y")
			&& CommonComponentHelper.getSystemSpecs().getProductionSystemCode().equalsIgnoreCase("LC")) {
		subProductsList = (List<RepositoryItem>) product.getPropertyValue("fixedRelatedProducts");
	}
	if (prodType == 1) {
		subProductsList = (List<RepositoryItem>) product.getPropertyValue("subProducts");
	}
	if (subProductsList != null || !CollectionUtils.isEmpty(subProductsList)) {
		Iterator<RepositoryItem> subProductsIterator = subProductsList.iterator();
		while (subProductsIterator.hasNext()) {
			RepositoryItem subProduct = (RepositoryItem) subProductsIterator.next();
			loadValidAlternateViewsList(new NMProduct(subProduct), secure, imageShotHelper, validAlternateViewsList, grpId);
		}
	}
    return validAlternateViewsList;
  }
  
  private void loadValidAlternateViewsList(NMProduct product, boolean secure, ImageShotHelper imageShotHelper, ArrayList<ImageShot> validAlternateViewsList, String grpId) throws ServletException, IOException {
    
    Iterator<ImageShot> altViewsIterator = determineValidAlternateViewsAvailable(product, secure, imageShotHelper, grpId).iterator();
    while (altViewsIterator.hasNext()) {
      // duplicate check before adding to list here
      // Must compare at shotLabel level to catch duplicate products under different subSuites
      ImageShot altViewImageShot = altViewsIterator.next();
      
      Iterator<ImageShot> validAltViewsIterator = validAlternateViewsList.iterator();
      boolean inValidAltList = false;
      while (validAltViewsIterator.hasNext()) {
        ImageShot validAltViewImageShot = validAltViewsIterator.next();
        if (validAltViewImageShot.getShotLabel().equalsIgnoreCase(altViewImageShot.getShotLabel())) {
          inValidAltList = true;
          break;
        }
      }
      if (!inValidAltList) {
        validAlternateViewsList.add(altViewImageShot);
      }
      
    }
  }
  
  protected abstract ArrayList<ImageShot> determineValidAlternateViewsAvailable(NMProduct product, boolean secure, ImageShotHelper imageShotHelper, String grpId) throws ServletException, IOException;
  
}
