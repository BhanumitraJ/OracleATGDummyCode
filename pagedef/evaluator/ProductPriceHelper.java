package com.nm.commerce.pagedef.evaluator;

import static com.nm.common.INMGenericConstants.ONE;
import static com.nm.monogram.utils.MonogramConstants.FREE_MONOGRAM_FLAG;
import static com.nm.monogram.utils.MonogramConstants.OPTIONAL_MONOGRAM_FLAG;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import atg.nucleus.Nucleus;
import atg.nucleus.logging.ApplicationLogging;
import atg.nucleus.logging.ClassLoggingFactory;
import atg.repository.RepositoryItem;

import com.nm.commerce.catalog.NMProduct;
import com.nm.commerce.catalog.NMProductDisplayPrice;
import com.nm.commerce.catalog.NMProductFactory;
import com.nm.commerce.catalog.NMProductPrice;
import com.nm.commerce.catalog.NMProductPriceAdornment;
import com.nm.commerce.catalog.NMSuite;
import com.nm.commerce.catalog.NMSuperSuite;
import com.nm.utils.LocalizationUtils;
import com.nm.utils.NmoUtils;
import com.nm.utils.logging.LoggingConfigure;

public class ProductPriceHelper {
  
  private final DecimalFormat myFormatter = new DecimalFormat("0.00");
  private NMProductPrice nmProductPrice;
  private Double highOriginalPrice = new Double(0);
  private Double lowOriginalPrice = new Double(0);
  boolean hasSuiteAdornments = false;
  boolean isSuiteSoldout = false;
  private final LocalizationUtils localizationUtils;
  private String countryCode = null;
  boolean disableAdornment = false;
  boolean populateDisplayPrices = false;
  private boolean logDebug = false;
  private ApplicationLogging logger = null;
  private LoggingConfigure loggingConfigure = null;
  
  public ProductPriceHelper() {
    this(false);
  }
  
  /**
   * if parameter "populateDisplayPrices" is set to true, converted display prices will (retail & adornments) be populated into NMProductDisplayPrice
   * 
   * @param populateDisplayPrices
   */
  public ProductPriceHelper(final boolean populateDisplayPrices) {
    if (populateDisplayPrices) {
      nmProductPrice = new NMProductDisplayPrice();
    } else {
      nmProductPrice = new NMProductPrice();
    }
    this.populateDisplayPrices = populateDisplayPrices;
    localizationUtils = ((LocalizationUtils) Nucleus.getGlobalNucleus().resolveName("/nm/utils/LocalizationUtils"));
    logger = ClassLoggingFactory.getFactory().getLoggerForClass(ProductPriceHelper.class);
    loggingConfigure = (LoggingConfigure) Nucleus.getGlobalNucleus().resolveName("/nm/utils/logging/LoggingConfigure");
    this.logDebug = loggingConfigure.getDebugConfigure("ProductPriceHelper");
  }
  
  /**
   * This method take into account localized pricing and price conversion It will get retail price from NMProduct for price calculation
   * 
   * @param product
   * @param countryCode
   * @param isSale
   * @return
   */
  public NMProductDisplayPrice generateProductPricingWithLocalization(final NMProduct product, final String countryCode, final boolean isSale) {
    if (!(nmProductPrice instanceof NMProductDisplayPrice)) {
      nmProductPrice = new NMProductDisplayPrice();
    }
    
    this.countryCode = countryCode;
    ((NMProductDisplayPrice) nmProductPrice).setHasLocalizedPrice(false);
    
    // product price will be converted to user currency
    generateProductPricing(product, isSale, true);
    
    int type = product.getType();
    String specialInstructionFlag = product.getSpecialInstructionFlag();
    if (specialInstructionFlag.equalsIgnoreCase(OPTIONAL_MONOGRAM_FLAG) || specialInstructionFlag.equalsIgnoreCase(FREE_MONOGRAM_FLAG)) {
      type = ONE; // to display the thumbnail price in range in case of optional monogram product
    }
    final String priceRange = generatePriceString(nmProductPrice.getLowestPrice(), nmProductPrice.getHighestPrice(), type);
    
    ((NMProductDisplayPrice) nmProductPrice).setPriceString(priceRange);
    
    String originalPriceRange = "";
    if (type == 0 && !(specialInstructionFlag.equalsIgnoreCase(OPTIONAL_MONOGRAM_FLAG) || specialInstructionFlag.equalsIgnoreCase(FREE_MONOGRAM_FLAG))) {
      originalPriceRange = generatePriceString(nmProductPrice.getHighPaPrice(), nmProductPrice.getHighPaPrice(), type);
    } else {
      originalPriceRange = generatePriceString(nmProductPrice.getLowOriginalPrice(), nmProductPrice.getHighOriginalPrice(), type);
    }
    
    if(product.getFlgConfigurable()) {
    	((NMProductDisplayPrice) nmProductPrice).setConfiguratorProductPriceString(generatePriceStringForConfiguratorProducts(nmProductPrice.getLowestPrice(), nmProductPrice.getHighestPrice()));
    }

    ((NMProductDisplayPrice) nmProductPrice).setOriginalPriceString(originalPriceRange);
    loggingDebug("productDisplayPrice for product " + product.getId() + "\t" + nmProductPrice.toString());
    
    return (NMProductDisplayPrice) nmProductPrice;
  }
  
  public static String generatePriceStringForConfiguratorProducts(final String low, final String high) {
  	DecimalFormat myFormatter = new DecimalFormat("0.00");
    if (!NmoUtils.isEmpty(low) && !NmoUtils.isEmpty(high)) {
      final StringBuilder priceRange = new StringBuilder();
      final Double lowd = Double.valueOf(low);
      final Double highd = Double.valueOf(high);
      if ((low != null) && (high != null)) {
        priceRange.append(myFormatter.format(lowd));
        if (highd > lowd) {
          priceRange.append(" - ").append(myFormatter.format(highd));
        }
      }
      // Removes each occurrence of "." followed by 2 zeros (".00")
      // "$10.00 - 20.00" becomes "$10 - 20"
      // "$10.50 - 20.00" becomes "$10.50 - 20"
      return priceRange.toString().replaceAll("\\.0{2}", "");
    } else {
      return null;
    }
  }

  public static  String generatePriceString(final String low, final String high, final int type) {
    DecimalFormat myFormatter = new DecimalFormat("0.00");
    if (!NmoUtils.isEmpty(low) && !NmoUtils.isEmpty(high)) {
      final StringBuilder priceRange = new StringBuilder();
      final Double lowd = Double.valueOf(low);
      final Double highd = Double.valueOf(high);
      if ((low != null) && (high != null)) {
        priceRange.append(myFormatter.format(lowd));
        if (type > 0 && !low.equalsIgnoreCase(high)) {
          priceRange.append(" - ").append(myFormatter.format(highd));
        }
      }
      // Removes each occurrence of "." followed by 2 zeros (".00")
      // "$10.00 - 20.00" becomes "$10 - 20"
      // "$10.50 - 20.00" becomes "$10.50 - 20"
      return priceRange.toString().replaceAll("\\.0{2}", "");
    } else {
      return null;
    }
  }
  
  /**
   * return retail price, either look into nm_product_aux table to get retail price or get it from NMProduct, where localized pricing and currency conversion is taken into account
   * 
   * @param product
   * @param convertCurrency
   * @return
   */
  private double getProductRetailPrice(final NMProduct product, final boolean internationalPrice) {
    Double productRetailPrice = new Double("0.00");
    if (internationalPrice) {
      if (!NmoUtils.isEmpty(product.getRetailPrice())) {
        productRetailPrice = Double.valueOf(product.getRetailPrice());
      }
    } else {
      return product.getPrice();
    }
    return productRetailPrice;
  }
  
  /**
   * @param product
   * @param internationalPrice
   * @return
   */
  private double getProductRetailPriceWithMinAdditionalCost(final NMProduct product, final boolean internationalPrice) {
    product.setIncludeRetailMaxPrice(false);
    product.setIncludeRetailMinPrice(true);
    return getProductRetailPrice(product, internationalPrice);
  }
  
  /**
   * @param product
   * @param internationalPrice
   * @return
   */
  private double getProductRetailPriceWithMaxAdditionalCost(final NMProduct product, final boolean internationalPrice) {
    product.setIncludeRetailMaxPrice(true);
    product.setIncludeRetailMinPrice(false);
    return getProductRetailPrice(product, internationalPrice);
  }
  
  /**
   * converts the monogrammed product price from string to double
   * 
   * @param product
   * @return returns the price in double data type
   */
  private double getMonogrammedPrice(final NMProduct product) {
    double doublePrice;
    try {
      doublePrice = Double.parseDouble(product.getMonogrammedPrice());
    } catch (final NumberFormatException nfe) {
      doublePrice = 0.0d;
    }
    return doublePrice;
  }
  
  private void loggingDebug(final String msg) {
    if (logDebug) {
      logger.logDebug(msg);
    }
  }
  
  private boolean productHasLocalizedPrice(final NMProduct product) {
    return localizationUtils.isProductHasLocalizedPrice(this.countryCode, product);
  }
  
  /**
   * This method doesn't take into account localized pricing and price currency conversion. It will always uses price in USD for price calculation This method is to support existing usage before
   * International localized pricing (WR 43181)
   * 
   * @param productRI
   * @param isSale
   * @return
   */
  public NMProductPrice generateProductPricing(final RepositoryItem productRI, final boolean isSale) {
    return generateProductPricing(NMProductFactory.createNMProduct(productRI), isSale, false);
  }
  
  /**
   * This method doesn't take into account localized pricing and price currency conversion. It will always uses price in USD for price calculation This method is to support existing usage before
   * International localized pricing (WR 43181)
   * 
   * @param productRI
   * @param isSale
   * @return
   */
  public NMProductPrice generateProductPricing(final NMProduct product, final boolean isSale) {
    return generateProductPricing(product, isSale, false);
  }
  
  /**
   * 
   * @param product
   * @param isSale
   *          : this parameter was used to decide whether to include a sub-product in suite price calculation as requested in INT-1933, we only look flgDisplay to decide whether a sub-product will be
   *          included in suite price calculation
   * @param internationalPrice
   * @return
   */
  public NMProductPrice generateProductPricing(final NMProduct product, final boolean isSale, final boolean internationalPrice) {
    
    double workingPercentDiscount = 0;
    double workingLowRetailPrice = 0;
    double workingAdornmentPrice = 0;
    double workingHighRetailPrice = 0;
    Boolean workingFlgDisplay = new Boolean(false);
    boolean foundDisplayableProduct = false;
    boolean firstAdornedProduct = true;
    
    Double highPriceForElement = new Double(0);
    Double lowPriceForElement = 0.0;
    Double percentDiscountForElement = new Double(0);
    boolean optionalMonogramProduct = false;
    
    switch (product.getType()) {
      case 0:
    	  final String specialInstructionFlag = product.getSpecialInstructionFlag();
    	  optionalMonogramProduct = specialInstructionFlag.equalsIgnoreCase(OPTIONAL_MONOGRAM_FLAG);
			if (product.getFlgConfigurable()) {
				workingLowRetailPrice = getProductRetailPriceWithMinAdditionalCost(product, internationalPrice);
				workingHighRetailPrice = getProductRetailPriceWithMaxAdditionalCost(product, internationalPrice);
				for (RepositoryItem productRepositoryItem : product.getRelatedConfigProducts()) {
					NMProduct nmProduct = new NMProduct(productRepositoryItem);
					if (nmProduct.getIsDisplayable()) {
						double relatedWProductorkingMinRetailPrice = getProductRetailPriceWithMinAdditionalCost(nmProduct, internationalPrice);
						double relatedWProductorkingMaxRetailPrice = getProductRetailPriceWithMaxAdditionalCost(nmProduct, internationalPrice);
						if (relatedWProductorkingMinRetailPrice < workingLowRetailPrice)
							workingLowRetailPrice = relatedWProductorkingMinRetailPrice;
						if (relatedWProductorkingMaxRetailPrice > workingHighRetailPrice)
							workingHighRetailPrice = relatedWProductorkingMaxRetailPrice;
					}
				}
			} else {
	            if (optionalMonogramProduct) {// In case of optional monnogram product the service fee is also include in calculating the price range
		          workingHighRetailPrice = getMonogrammedPrice(product);
		        } else {
		          workingHighRetailPrice = getProductRetailPrice(product, internationalPrice);
		        }
				// find the retail price
				workingLowRetailPrice = getProductRetailPrice(product, internationalPrice);
			}
        // find the max adornment for the product
        workingAdornmentPrice = findMaxAdornment(product, product.getType(), internationalPrice);
        
        // calculate the percent discount
        workingPercentDiscount = 100 - ((workingLowRetailPrice / workingAdornmentPrice) * 100);
        // store the price to highPriceForElement
        if (workingHighRetailPrice > highPriceForElement.doubleValue()) {
          highPriceForElement = new Double(workingHighRetailPrice);
        }
        // store the price to lowPriceForElement
        if (workingLowRetailPrice < Double.MAX_VALUE) {
          lowPriceForElement = new Double(workingLowRetailPrice);
        }
        // store the percent discount to percendDiscountForElement
        if (workingPercentDiscount > percentDiscountForElement.doubleValue()) {
          percentDiscountForElement = new Double(workingPercentDiscount);
        }
        
        if ((specialInstructionFlag.equalsIgnoreCase(OPTIONAL_MONOGRAM_FLAG) || specialInstructionFlag.equalsIgnoreCase(FREE_MONOGRAM_FLAG)) && product.getFlgPricingAdornments()) {
          findSuiteAdornmentPriceRanges((Map) product.getPropertyValue("pricingAdornments"), firstAdornedProduct);
          firstAdornedProduct = false;
        }
        
        if ((workingAdornmentPrice > 0) && (product.getSpecialInstructionFlag().equalsIgnoreCase(OPTIONAL_MONOGRAM_FLAG) || product.getSpecialInstructionFlag().equalsIgnoreCase(FREE_MONOGRAM_FLAG))) {
          lowOriginalPrice = workingAdornmentPrice;
          if (optionalMonogramProduct) {
            highOriginalPrice = workingAdornmentPrice + Double.valueOf(product.getLocalizedSpecialInstructionPrice());
          } else {
            highOriginalPrice = workingAdornmentPrice;
          }
          hasSuiteAdornments = true;
        }
      
      break;
      case 1:
        final NMSuite suite = new NMSuite(product.getId());
        final List<NMProduct> subProducts = suite.getProductList();
        isSuiteSoldout = (Boolean) suite.getPropertyValue("flgDisplay") ? false : true;
        final List<Double> suiteAdormentList = new ArrayList<Double>();
        
        for (final NMProduct subProduct : subProducts) {
          workingFlgDisplay = subProduct.getIsDisplayable();
          if (workingFlgDisplay || isSuiteSoldout) { // only include sellable product in suite price calculation
          
            foundDisplayableProduct = true;
            optionalMonogramProduct = subProduct.getSpecialInstructionFlag().equalsIgnoreCase(OPTIONAL_MONOGRAM_FLAG);
            if (optionalMonogramProduct) {// In case of optional monnogram product the service fee is also include in calculating the price
                                                                                                 // range
              workingHighRetailPrice = getMonogrammedPrice(subProduct);
            } else {
              workingHighRetailPrice = getProductRetailPrice(subProduct, internationalPrice);
            }
            // find price for subproduct
            workingLowRetailPrice = getProductRetailPrice(subProduct, internationalPrice);
            
            // find max adornment for subProduct
            workingAdornmentPrice = findMaxAdornment(subProduct, product.getType(), internationalPrice);
            
            if (workingAdornmentPrice != 0.0) {
              suiteAdormentList.add(workingAdornmentPrice);
              if (optionalMonogramProduct) {
                suiteAdormentList.add(workingAdornmentPrice + Double.parseDouble(subProduct.getLocalizedSpecialInstructionPrice()));
              }
            } else {
              suiteAdormentList.add(workingLowRetailPrice);
              suiteAdormentList.add(workingHighRetailPrice);
            }
            
            if (subProduct.getFlgPricingAdornments()) {
              // find suite adornment ranges for subProduct
              findSuiteAdornmentPriceRanges((Map) subProduct.getPropertyValue("pricingAdornments"), firstAdornedProduct);
              firstAdornedProduct = false;
              
            }
            
            // calculate percent discount for subproduct
            workingPercentDiscount = 100 - ((workingLowRetailPrice / workingAdornmentPrice) * 100);
            // store the price to lowPriceForElement
            if (workingHighRetailPrice > highPriceForElement.doubleValue()) {
              highPriceForElement = new Double(workingHighRetailPrice);
            }
            // store the price to highPriceForElement
            if (workingLowRetailPrice < Double.MAX_VALUE) {
              lowPriceForElement = new Double(workingLowRetailPrice);
            }
            // if this subproduct (suite member) has the highest
            // %discount, store it in percentDiscountforElement
            if (workingPercentDiscount > percentDiscountForElement.doubleValue()) {
              percentDiscountForElement = new Double(workingPercentDiscount);
            }
          }
        }
        
        if (suiteAdormentList.size() > 0) {
          lowOriginalPrice = Collections.min(suiteAdormentList);
          highOriginalPrice = Collections.max(suiteAdormentList);
        }
      
      break;
      case 2:
        final NMSuperSuite superSuite = new NMSuperSuite(product.getId());
        final List<NMSuite> suites = superSuite.getSuiteList();
        isSuiteSoldout = (Boolean) superSuite.getPropertyValue("flgDisplay") ? false : true;
        final List<Double> superSuiteAdormentList = new ArrayList<Double>();
        
        for (final NMSuite subsuite : suites) {
          for (final NMProduct subProduct : subsuite.getProductList()) {
            workingFlgDisplay = subProduct.getIsDisplayable();
            if (workingFlgDisplay || isSuiteSoldout) { // only include sellable product in suite price calculation
              foundDisplayableProduct = true;
              // find price for subproduct
              optionalMonogramProduct = subProduct.getSpecialInstructionFlag().equalsIgnoreCase(OPTIONAL_MONOGRAM_FLAG);
              if (optionalMonogramProduct) {// In case of optional monnogram product the service fee is also include in calculating the price range
                workingHighRetailPrice = getMonogrammedPrice(subProduct);
              } else {
                workingHighRetailPrice = getProductRetailPrice(subProduct, internationalPrice);
              }
              workingLowRetailPrice = getProductRetailPrice(subProduct, internationalPrice);
              
              // find max adornment for subProduct
              workingAdornmentPrice = findMaxAdornment(subProduct, product.getType(), internationalPrice);
              
              if (workingAdornmentPrice != 0.0) {
                superSuiteAdormentList.add(workingAdornmentPrice);
                if (optionalMonogramProduct) {
                  superSuiteAdormentList.add(workingAdornmentPrice + Double.parseDouble(subProduct.getLocalizedSpecialInstructionPrice()));
                }
              } else {
                superSuiteAdormentList.add(workingLowRetailPrice);
                superSuiteAdormentList.add(workingHighRetailPrice);
              }
              if (subProduct.getFlgPricingAdornments()) {
                // find suite adornment ranges for subProduct
                findSuiteAdornmentPriceRanges((Map) subProduct.getPropertyValue("pricingAdornments"), firstAdornedProduct);
                firstAdornedProduct = false;
              }
              
              // calculate percent discount for subproduct
              workingPercentDiscount = 100 - ((workingLowRetailPrice / workingAdornmentPrice) * 100);
              // store the price to lowPriceForElement
              if (workingHighRetailPrice > highPriceForElement.doubleValue()) {
                highPriceForElement = new Double(workingHighRetailPrice);
              }
              // store the price to highPriceForElement
              if (workingLowRetailPrice < Double.MAX_VALUE) {
                lowPriceForElement = new Double(workingLowRetailPrice);
              }
              // if this subproduct (suite member) has the highest
              // %discount, store it in percentDiscountforElement
              if (workingPercentDiscount > percentDiscountForElement.doubleValue()) {
                percentDiscountForElement = new Double(workingPercentDiscount);
              }
            }
          }
        }
        
        if (superSuiteAdormentList.size() > 0) {
          lowOriginalPrice = Collections.min(superSuiteAdormentList);
          highOriginalPrice = Collections.max(superSuiteAdormentList);
        }
      
      break;
    }
    
    nmProductPrice.setPercentDiscount(Integer.toString(percentDiscountForElement.intValue()));
    nmProductPrice.setHighestPrice(highPriceForElement.toString());
    nmProductPrice.setLowestPrice(lowPriceForElement.toString());
    nmProductPrice.setHasSuiteAdornments(hasSuiteAdornments);
    nmProductPrice.setHighOriginalPrice(highOriginalPrice.toString());
    nmProductPrice.setLowOriginalPrice(lowOriginalPrice.toString());
    
    return nmProductPrice;
  }
  
  private Double findMaxAdornment(final NMProduct product, final int ancestorType, final boolean convertCurrency) {
    // find max adornment for subProduct
    if (!disableAdornment && convertCurrency && productHasLocalizedPrice(product)) {
      // disable adornment when the product has localized pricing.
      // If this product is a child of a suite or super suite, do not continue to get adornment for the whole suite/supersuite
      disableAdornment = true;
      // set the NMProductDisplayPrice has localized price true
      loggingDebug("product has localized price, product - " + product.getId());
      ((NMProductDisplayPrice) nmProductPrice).setHasLocalizedPrice(true);
    }
    if (!disableAdornment) {
      loggingDebug("find max adornment for product - " + product.getId());
      return findMaxAdornment((Map) product.getPropertyValue("pricingAdornments"), ancestorType, convertCurrency);
    } else {
      loggingDebug("product has localized price, adornment is disabled for product - " + product.getId());
      return 0.0;
    }
  }
  
  /**
   * @param pricingAdornments
   * @return The maximum value pricing adornment from the collection for single product
   */
  private double findMaxAdornment(final Map<String, RepositoryItem> pricingAdornments, final int ancestorType, final boolean internationalPrice) {
    double maxAdornment = 0;
    Map<String, NMProductPriceAdornment> adornmentMap = null;
    
    if ((pricingAdornments != null) && (pricingAdornments.size() > 0)) {
      final int start = pricingAdornments.size();
      for (int pos = start; pos > 0; pos--) {
        final String position = "position" + pos;
        final RepositoryItem adornment = pricingAdornments.get(position);
        Double adornmentPrice = 0.0;
        String convertedAdornmentString = "";
        if (adornment != null) {
          adornmentPrice = (Double) adornment.getPropertyValue("adornment_price");
          // convert adornmentPrice (always in USD) to user selected currency price value,
          
          if (internationalPrice) {
            // if we converted product retail price, we need to convert the adornment price so that they are comparable
            convertedAdornmentString = localizationUtils.getConvertedCurrency(adornmentPrice);
            adornmentPrice = Double.valueOf(localizationUtils.getConvertedCurrency(adornmentPrice));
            
            // populate adornment price for single products
            if (!disableAdornment && (ancestorType == 0)) {
              if (adornmentMap == null) {
                adornmentMap = new HashMap<String, NMProductPriceAdornment>();
              }
              final NMProductPriceAdornment priceAdornment = new NMProductPriceAdornment();
              priceAdornment.setAdornmentLabel((String) adornment.getPropertyValue("label"));
              
              priceAdornment.setAdornmentPrice(adornmentPrice);
              adornmentMap.put(position, priceAdornment);
            }
          }
        } else {
          loggingDebug("*** Adornment is null ***");
        }
        
        if (adornmentPrice > maxAdornment) {
          maxAdornment = adornmentPrice.doubleValue();
          nmProductPrice.setHighPaLabel((String) adornment.getPropertyValue("label"));
          if (nmProductPrice.getHighPaLabel() == null) {
            nmProductPrice.setHighPaLabel("Was");
          }
          
          nmProductPrice.setHighPaPrice(myFormatter.format(adornmentPrice));
          if (nmProductPrice.getHighPaPrice() == null) {
            nmProductPrice.setHighPaPrice("0.00");
          }
        }
        // set the adornment price into NMProductDisplayPrice
        if (adornmentMap != null) {
          ((NMProductDisplayPrice) nmProductPrice).setPriceAdornmentMap(adornmentMap);
        }
      }
    }
    return maxAdornment;
  }
  
  /**
   * @param pricingAdornments
   * @return The suite range values for pricing adornments from all products in the suite
   */
  private void findSuiteAdornmentPriceRanges(final Map<String, RepositoryItem> pricingAdornments, final boolean firstAdornedProduct) {
    if (!disableAdornment && (pricingAdornments != null) && (pricingAdornments.size() > 0)) {
      hasSuiteAdornments = true;
      final int start = pricingAdornments.size();
      
      for (int pos = start; pos > 0; pos--) {
        final String position = "position" + pos;
        final RepositoryItem adornment = pricingAdornments.get(position);
        final Double adornmentPrice = (Double) adornment.getPropertyValue("adornment_price");
        
        // "WAS" - Original price range label comes from highest position adornment of first product
        if (firstAdornedProduct && (pos == start)) {
          nmProductPrice.setOriginalPriceRangeLabel((String) adornment.getPropertyValue("label"));
        }
        // "NOW" - Adorned price range label comes from position 1 of first product
        if (firstAdornedProduct && (pos == 1)) {
          nmProductPrice.setAdornedPriceRangeLabel((String) adornment.getPropertyValue("label"));
        }
        
        /*
         * The adorned price range for suite is already recorded as highPriceForElement & lowPriceForElement in the generateProductPricing() method (above) Low & high original price range needs to be
         * pulled from positions 2 & 3 (below)
         */
        // if (pos > 1) {
        // // store the price to highOriginalPrice
        // if (adornmentPrice > highOriginalPrice.doubleValue()) {
        // highOriginalPrice = new Double(adornmentPrice);
        // }
        // // store the price to lowOriginalPrice
        // if(lowOriginalPrice.doubleValue() == 0.00 || adornmentPrice < lowOriginalPrice.doubleValue()) {
        // lowOriginalPrice = new Double(adornmentPrice);
        // }
        // }
      }
    }
  }
}
