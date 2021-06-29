package com.nm.commerce.catalog;

import java.util.Map;
import java.util.Set;

/**
 * this class extends NMProductPrice holding extra data for displaying localized pricing its data is populated via ProductPriceHelper Following is complete the list of data members of this class:
 * highestPrice // this is high retail price, in case of productType=0, it is the same as lowestPrice lowestPrice // this is low retail price, in case of productType=0, it is the same as highestPrice
 * highOriginalPrice // suite highest adornment price lowOriginalPrice // suite lowest adornment price highPaPrice // product (type=0) highest Original price
 * 
 * percentDiscount //
 * 
 * %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%% data members below are used for web price display %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
 * 
 * priceString // the product current sale price string to be displayed on web, single price string for type=0 product, price range for suite product originalPriceString // the product original price
 * string to be displayed on web, single price string for type=0 product, price range for suite product // priceString and originalPriceRangeString has the following price value format: // Removes
 * each occurrence of "." followed by 2 zeros (".00") // "$10.00 - 20.00" becomes "$10 - 20" // "$10.50 - 20.00" becomes "$10.50 - 20"
 * 
 * highPaLabel // product (type=0) highest Original price label originalPriceRangeLabel // suite product original price range label adornedPriceRangeLabel // suite product price adornment price range
 * label
 * 
 * hasLocalizedPrice // whether the product has localized price priceAdornmentMap // adornment price at different positions
 * 
 * hasSuiteAdornments // boolean value: if the suite has any subproducts that has flgPricingAdornments set as true or not
 * 
 * %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%% price display examples %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%% Single product price display example:
 * this.highPaLabel this.originalPriceString "SALE" this.priceString
 * 
 * suite product price display example: this.originalPriceRangeLabel this.originalPriceString this.adornedPriceRangeLabel this.priceString
 * 
 * @author nmjh94
 * 
 */

public class NMProductDisplayPrice extends NMProductPrice {
  private String priceString; // the price string to be displayed on UI
  
  private String configuratorProductPriceString; //the price range string to be displayed on UI for configurator products alone
  
  private String originalPriceString; // the original price string to be displayed on UI
  
  private boolean hasLocalizedPrice; // whether this product has localized price for given country
  
  private Map<String, NMProductPriceAdornment> priceAdornmentMap; // key = position, value = NMProductPriceAdornment
  
  public String getPriceString() {
    return priceString;
  }
  
  public void setPriceString(String retailPrice) {
    this.priceString = retailPrice;
  }
  
  public String getConfiguratorProductPriceString() {
		return configuratorProductPriceString;
	}

	public void setConfiguratorProductPriceString(String configuratorProductPriceString) {
		this.configuratorProductPriceString = configuratorProductPriceString;
	}

	public boolean isHasLocalizedPrice() {
    return hasLocalizedPrice;
  }
  
  public void setHasLocalizedPrice(boolean hasLocalizedPrice) {
    this.hasLocalizedPrice = hasLocalizedPrice;
  }
  
  public Map<String, NMProductPriceAdornment> getPriceAdornmentMap() {
    return priceAdornmentMap;
  }
  
  public void setPriceAdornmentMap(Map<String, NMProductPriceAdornment> priceAdornmentMap) {
    this.priceAdornmentMap = priceAdornmentMap;
  }
  
  public String getOriginalPriceString() {
    return originalPriceString;
  }
  
  public void setOriginalPriceString(String originalPriceString) {
    this.originalPriceString = originalPriceString;
  }
  
  @Override
  public String toString() {
    StringBuilder prices = new StringBuilder();
    prices.append(super.toString());
    prices.append("hasLocalizedPrice=" + this.hasLocalizedPrice + " ; ");
    prices.append("priceString=" + this.priceString + " ; ");
    prices.append("OriginalPriceRangeString=" + this.originalPriceString + " ; ");
    if (priceAdornmentMap != null) {
      Set<Map.Entry<String, NMProductPriceAdornment>> adornmentSet = priceAdornmentMap.entrySet();
      for (Map.Entry<String, NMProductPriceAdornment> entry : adornmentSet) {
        prices.append(entry.getKey() + "-");
        prices.append(entry.getValue().getAdornmentLabel() + " " + entry.getValue().getAdornmentPrice() + " ; ");
      }
    }
    return prices.toString();
  }
  
}
