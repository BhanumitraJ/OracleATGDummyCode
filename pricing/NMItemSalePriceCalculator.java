package com.nm.commerce.pricing;

import java.util.Locale;
import java.util.Map;

import atg.adapter.gsa.GSAItem;
import atg.commerce.order.CommerceItem;
import atg.commerce.pricing.ItemListPriceCalculator;
import atg.commerce.pricing.ItemPriceInfo;
import atg.commerce.pricing.PricingException;
import atg.core.util.StringUtils;
import atg.repository.RepositoryItem;

import com.nm.commerce.NMCommerceItem;
import com.nm.commerce.NMProfile;
import com.nm.monogram.utils.MonogramConstants;
import com.nm.utils.LocalizationUtils;
import com.nm.utils.StringUtilities;

public class NMItemSalePriceCalculator extends ItemListPriceCalculator {
  private LocalizationUtils localizationUtils;
  
  /**
   * override super method, this method adds the specialInstruction price to the existing price if in case the optional monogram item is monogrammed
   */
  protected void priceItem(double pPrice, ItemPriceInfo pPriceQuote, CommerceItem pItem, RepositoryItem pPricingModel, Locale pLocale, RepositoryItem pProfile, Map pExtraParameters)
          throws PricingException {
	  NMCommerceItem nmItem = (NMCommerceItem) pItem;
	  String specialInstructionFlag = nmItem.getSpecialInstructionFlag();
	  double specialInstructionPrice = nmItem.getSpecialInstructionPrice();
	  boolean isMonogramSelected = !StringUtilities.isEmpty(nmItem.getCodeSetType());
	  double price = specialInstructionFlag.equalsIgnoreCase(MonogramConstants.OPTIONAL_MONOGRAM_FLAG) && isMonogramSelected ? pPrice + specialInstructionPrice : pPrice;
	  //convert to localized price and round before sending back to ATG pricing pipeline 
    if(!StringUtils.isBlank(nmItem.getConfigurationKey())){
      price = getConfigSetPrice(pPrice, nmItem);
    }
	  super.priceItem(price, pPriceQuote, pItem, pPricingModel, pLocale, pProfile, pExtraParameters);
  }
  
  /**
   * Gets the configurator additional cost.
   *
   * @param pPrice the price
   * @param nmItem the nm item
   * @return the configurator additional cost
   */
  private double getConfigSetPrice(double pPrice, NMCommerceItem nmItem) {
    return  getPricingTools().round(pPrice + nmItem.getConfigSetPrice());
  }
  
  /**
   * override super method this method takes into account localized price
   */
  @Override
  protected double getPrice(Object pPriceSource) throws PricingException {
    NMProfile profile = localizationUtils.getProfile();
    String countryCode = "";
    
    if (profile != null) {
      countryCode = profile.getCountryPreference();
    } else {
      return super.getPrice(pPriceSource);
    }
    
    Double localizedPrice = localizationUtils.getCommerceItemListPriceInUSD(pPriceSource, countryCode, profile.getCurrencyPreference(), isLoggingDebug());
    
    if (localizedPrice != null) {
      // need to round before sending back to ATG pricing pipeline,
      // so that later calculation has consistent starting point
      double salePrice = getPricingTools().round(localizedPrice);
      if (isLoggingDebug()) {
        logDebug("commerce item " + ((GSAItem) pPriceSource).getRepositoryId() + " has localized price in country " + countryCode + ", price value is " + localizedPrice + " USD");
      }
      return salePrice;
    } else {
      // if the item do not have localized price, we use the US price directly from DB
      return super.getPrice(pPriceSource);
    }
  }
  
  public LocalizationUtils getLocalizationUtils() {
    return localizationUtils;
  }
  
  public void setLocalizationUtils(LocalizationUtils localizationUtils) {
    this.localizationUtils = localizationUtils;
  }
}
