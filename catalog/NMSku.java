package com.nm.commerce.catalog;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import atg.nucleus.Nucleus;
import atg.repository.MutableRepositoryItem;
import atg.repository.RemovedItemException;
import atg.repository.Repository;
import atg.repository.RepositoryItem;

import com.nm.components.CommonComponentHelper;
import com.nm.integration.ShopRunnerConstants;
import com.nm.utils.GenericLogger;
import com.nm.utils.ProdSkuUtil;

public class NMSku {
  
  private RepositoryItem dataSource;
  private ProdSkuUtil prodSkuUtil;
  private boolean isSellable = false;
  private Repository mcustomCatalogAuxRepository = null;
  private String imageShotTypesList= "m*,e*,a*,b*,c*,z*";
  
  public NMSku(final RepositoryItem repositoryItem) {
    setDataSource(repositoryItem);
  }
  
  public NMSku(final String skuId) {
    setDataSource(getProdSkuUtil().getSkuRepositoryItem(skuId));
  }
  
  public RepositoryItem getDataSource() {
    return dataSource;
  }
  
  public void setDataSource(final RepositoryItem dataSource) {
    this.dataSource = dataSource;
  }
  
  public boolean getIsSellable() {
    return isSellable;
  }
  
  public void setIsSellable(final boolean isSellable) {
    this.isSellable = isSellable;
  }
  
  public Object getPropertyValue(final String propertyName) {
    try {
      return getDataSource().getPropertyValue(propertyName);
    } catch (final Exception e) {
      e.printStackTrace();
    }
    
    return null;
  }
  
  public String getId() {
    return getDataSource().getRepositoryId();
  }
  
  public ProdSkuUtil getProdSkuUtil() {
    if (prodSkuUtil == null) {
      prodSkuUtil = CommonComponentHelper.getProdSkuUtil();
    }
    return prodSkuUtil;
  }
  
  /**
   * Returns cmos sku
   */
  public String getCmosSku() {
    return (String) getPropertyValue("cmosSKU");
  }
  
  /**
   * Method to retrieve Skus' item kength
   * 
   * @return double
   */
  public double getItemLength() {
    double itemLength = 0.0;
    if (null != getDataSource().getPropertyValue("itemLength")) {
      itemLength = (Double) getDataSource().getPropertyValue("itemLength");
    }
    return itemLength;
  }
  
  /**
   * Method to retrieve Skus' item width
   * 
   * @return double
   */
  public double getItemWidth() {
    double itemWidth = 0.0;
    if (null != getDataSource().getPropertyValue("itemWidth")) {
      itemWidth = (Double) getDataSource().getPropertyValue("itemWidth");
    }
    return itemWidth;
  }
  
  /**
   * Method to retrieve Sku's item Height
   * 
   * @return double
   */
  public double getItemHeight() {
    double itemHeight = 0.0;
    if (null != getDataSource().getPropertyValue("itemHeight")) {
      itemHeight = (Double) getDataSource().getPropertyValue("itemHeight");
    }
    return itemHeight;
  }
  
  public void setPropertyValue(final String property, final Object value) {
    try {
      ((MutableRepositoryItem) getDataSource()).setPropertyValue(property, value);
    } catch (final Exception e) {
      e.printStackTrace();
    }
  }
  
  public boolean getIsNewColor() {
    final Boolean isNew = (Boolean) getDataSource().getPropertyValue("flgNewColor");
    if (isNew != null) {
      return isNew.booleanValue();
    } else {
      return false;
    }
  }
  
  public boolean getIsPerishable() {
    return ((Boolean) getDataSource().getPropertyValue("flgPerishable")).booleanValue();
  }
  
  public boolean getCanGiftWrap() {
    return getFlag("flgGiftwrap");
  }
  
  /**
   * Method to retrieve Skus' Box Weight
   * 
   * @return boxWeight
   */
  public double getBoxWeight() {
    double skuBoxweight = 0.0;
    if (null != getDataSource().getPropertyValue("boxWeight")) {
      skuBoxweight = (Double) getDataSource().getPropertyValue("boxWeight");
    }
    return skuBoxweight;
  }
  
  private boolean getFlag(final String name) {
    return ((Boolean) getDataSource().getPropertyValue(name)).booleanValue();
  }
  
  public boolean getHasMultiColors() {
    final List<RepositoryItem> multiColors = getMultiColorList();
    return (multiColors != null && multiColors.size() > 0);
  }
  
  public List<RepositoryItem> getMultiColorList() {
    @SuppressWarnings("unchecked")
    final List<RepositoryItem> list = (List<RepositoryItem>) getPropertyValue("multicolors");
    return list;
  }
  
  public int getDeliveryDays() {
    try {
      return (int) (((Double) getPropertyValue("skuDeliveryDays")).doubleValue());
    } catch (final Exception e) {
      return 0;
    }
  }
  
  public void setDeliveryDays(final int deliveryDays) {
    setPropertyValue("skuDeliveryDays", new Double(deliveryDays));
  }
  
  public long getExpShippingDays() {
    final GenericLogger log = CommonComponentHelper.getLogger();
    final Calendar expShipDate = Calendar.getInstance();
    final Calendar currentDate = Calendar.getInstance();
    final SimpleDateFormat sdf = new SimpleDateFormat(ShopRunnerConstants.EXP_SHIP_DATE_FORMAT);
    long curDateInMillis = 0;
    long expShipDateInMillis = 0;
    long skuExpShipDays = 0;
    try {
      if (null != this.getExpectedShipDate()) {
        expShipDate.setTime(this.getExpectedShipDate());
        currentDate.setTime(sdf.parse(sdf.format(currentDate.getTime())));
        expShipDateInMillis = expShipDate.getTimeInMillis();
        curDateInMillis = currentDate.getTimeInMillis();
      }
      skuExpShipDays = (expShipDateInMillis - curDateInMillis) / (24 * 60 * 60 * 1000);
      log.debug("Exp ship date :" + this.getExpectedShipDate() + "--Current date : " + sdf.format(currentDate.getTime()) + "--Shipping Days :" + skuExpShipDays);
    } catch (final ParseException e) {
      log.error("Errorwhile parsing current date");
    }
    return skuExpShipDays;
  }
  
  public int getQuantityOnOrder() {
    try {
      return (int) (((Double) getPropertyValue("quantityOnOrder")).doubleValue());
    } catch (final Exception e) {
      return 0;
    }
  }
  
  public void setQuantityOnOrder(final int quantityOnOrder) {
    setPropertyValue("quantityOnOrder", new Double(quantityOnOrder));
  }
  
  public String getRepositoryId() {
    return getDataSource().getRepositoryId();
  }
  
  public String getColor() {
    return getCodeVariation1();
  }
  
  public String getCodeVariation1() {
    final String codeVar1 = (String) getPropertyValue("codeVariation1");
    
    if (codeVar1 == null) {
      return "";
    } else {
      return codeVar1.trim();
    }
  }
  
  public void setCodeVariation1(final String codeVariation1) {
    setPropertyValue("codeVariation1", codeVariation1);
  }
  
  public String getEdisonColorCode() {
    return (String) getPropertyValue("edisonColorCode");
  }
  
  public String getSize() {
    return getCodeVariation2();
  }
  
  public String getCodeVariation2() {
    final String codeVar2 = (String) getPropertyValue("codeVariation2");
    
    if (codeVar2 == null) {
      return "";
    } else {
      return codeVar2.trim();
    }
  }
  
  public void setCodeVariation2(final String codeVariation2) {
    setPropertyValue("codeVariation2", codeVariation2);
  }
  
  public String getCodeVariation3() {
    final String codeVar3 = (String) getPropertyValue("codeVariation3");
    
    if (codeVar3 == null) {
      return "";
    } else {
      return codeVar3.trim();
    }
  }
  
  public void setCodeVariation3(final String codeVariation3) {
    setPropertyValue("codeVariation3", codeVariation3);
  }
  
  public boolean getIsOnOrder() {
    if (getQuantityOnOrder() > 0) {
      return true;
    } else {
      return false;
    }
  }
  
  public Map<String, RepositoryItem> getSkuProdInfoMap() {
    @SuppressWarnings("unchecked")
    final Map<String, RepositoryItem> result = (Map<String, RepositoryItem>) getPropertyValue("skuProdInfo");
    return result;
  }
  
  public void setSkuProdInfoMap(final Map<String, RepositoryItem> skuProdInfoMap) {
    setPropertyValue("skuProdInfo", skuProdInfoMap);
  }
  
  public String getHexCode() {
    return (String) getPropertyValue("hexCode");
  }
  
  public void setHexCode(final String hexColor) {
    setPropertyValue("hexCode", hexColor);
  }
  
  public String getStockLevel() {
    return (String) getPropertyValue("stockLevel");
  }
  
  public void setStockLevel(final String stockLevel) {
    setPropertyValue("stockLevel", stockLevel);
  }
  
  public String getSuggReplenishmentInterval() {
    return (String) getPropertyValue("suggReplenishInterval");
  }
  
  public void setSuggReplenishmentInterval(final String suggReplenishInterval) {
    setPropertyValue("suggReplenishInterval", suggReplenishInterval);
  }
  
  public boolean getInStock() {
    if (Integer.parseInt(getStockLevel()) > 0) {
      return true;
    } else {
      return false;
    }
  }
  
  public void setInStock(final boolean inStock) {
    setPropertyValue("inStock", new Boolean(inStock));
  }
  
  public boolean getIsDropShip() {
    
    /*
     * System.out.println("nmsku getIsDropShip->"+((Boolean)getPropertyValue( "flgDropShip")).booleanValue() +", getinstock()->"+getInStock() +", getStockLevel()->"+getStockLevel()+".");
     */
    return ((Boolean) getPropertyValue("flgDropShip")).booleanValue();
  }
  
  public void setIsDropShip(final boolean flgDropShip) {
    setPropertyValue("flgDropShip", new Boolean(flgDropShip));
  }
  
  public Date getExpectedShipDate() {
    return (Date) getPropertyValue("expectedShipDate");
  }
  
  public void setExpectedShipDate(final Date expectedShipDate) {
    setPropertyValue("expectedShipDate", expectedShipDate);
  }
  
  public String getVendorId() {
    return (String) getPropertyValue("vendorId");
  }
  
  public void setVendorId(final String vendorId) {
    setPropertyValue("vendorId", vendorId);
  }
  
  public boolean getIsStoreFulfillFlag() {
    final Boolean stFulfillFlag = new Boolean(false);
    if (getPropertyValue("storeFulfillFlag") == null) {
      return stFulfillFlag.booleanValue();
    } else {
      return ((Boolean) getDataSource().getPropertyValue("storeFulfillFlag")).booleanValue();
    }
  }
  
  public void setIsStoreFulfillFlag(final boolean storeFulfillFlag) {
    setPropertyValue("storeFulfillFlag", new Boolean(storeFulfillFlag));
  }
  
  public int getBossQty() {
    int qty = 0;
    final Double bossQty = (Double) getDataSource().getPropertyValue("bossTotal");
    if (null != bossQty) {
      qty = bossQty.intValue();
    }
    return qty;
  }

  private Repository getCustomCatalogRepository() {
    if (mcustomCatalogAuxRepository == null) {
      mcustomCatalogAuxRepository = (Repository) Nucleus.getGlobalNucleus().resolveName("/atg/commerce/catalog/ProductCatalogAux");
    }
    return mcustomCatalogAuxRepository;
  }

  public RepositoryItem getMediaItem(final String imageKey) {
    RepositoryItem returnValue = null;
    final Repository repository = dataSource.getRepository();
    final Repository productCatalogAuxRepository = getCustomCatalogRepository();
    try {
    	
      final RepositoryItem item = productCatalogAuxRepository.getItem(this.getId() + "~" + imageKey, "sku_media_aux");
      if (item != null) {
        final String mediaId = (String) item.getPropertyValue("mediaId");
        returnValue = repository.getItem(mediaId, "media");
      }
    } catch (final RemovedItemException exception) {
      // ignore this exception. it is a known condition that occurs when a
      // media item is removed via a service
    } catch (final Exception exception) {
      exception.printStackTrace();
    }

    return returnValue;

  }
  
	public Map<String, String> getSkuStaticImageUrl() {
		Map<String, String> returnValue = new HashMap<String, String>();

		
		 //dummy data for static images
		 //remove after media service changes has been committed for story  NMOBLDS-2838
		 //returnValue.put("m*", "DEV_NMI3966_m_01"); 
		
		try {
			String[] items = imageShotTypesList.split(",");
			List<String> shotList = Arrays.asList(items);
			for (int i = 0; i < shotList.size(); i++) {
				RepositoryItem mediaItem = null;
				mediaItem = getMediaItem(shotList.get(i));
				if (mediaItem != null) {
					String url = (String) mediaItem.getPropertyValue("url");
					returnValue.put(shotList.get(i), url);
				}
			}
		} 
		catch (final Exception exception) {
			exception.printStackTrace();
		}
		return returnValue;
	}


  public Map<String, RepositoryItem> getAuxiliaryMedia() {
	 @SuppressWarnings("unchecked")
	 final Map<String, RepositoryItem> result = (Map<String, RepositoryItem>) getPropertyValue("auxiliaryMedia");
	 return result;
  }
	  
  public static Comparator<NMSku> VARIATIONCODE2_ASC_COMPARATOR = new Comparator<NMSku>() {
    @Override
    public int compare(final NMSku sku1, final NMSku sku2) {
      String variation2_1 = sku1.getCodeVariation2();
      String variation2_2 = sku2.getCodeVariation2();
      
      if (variation2_1 == null) {
        variation2_1 = "";
      }
      if (variation2_2 == null) {
        variation2_2 = "";
      }
      
      return variation2_1.compareTo(variation2_2);
    };
  };
  
  public static Comparator<NMSku> VARIATIONCODE1_ASC_COMPARATOR = new Comparator<NMSku>() {
    @Override
    public int compare(final NMSku sku1, final NMSku sku2) {
      String variation1_1 = sku1.getCodeVariation1();
      String variation1_2 = sku2.getCodeVariation1();
      
      if (variation1_1 == null) {
        variation1_1 = "";
      }
      if (variation1_2 == null) {
        variation1_2 = "";
      }
      
      return variation1_1.compareTo(variation1_2);
    };
  };

 public Map<String, String> getSkuImageUrlMap()
 {
	 Map<String, String> skuImageUrlMap = new HashMap<String,String>();
	 Map<String, RepositoryItem> auxillaryMediaMap = getAuxiliaryMedia();
	 
	 Iterator entries = auxillaryMediaMap.entrySet().iterator();
	 while (entries.hasNext()) {
	   Entry thisEntry = (Entry) entries.next();
	   Object imgKey = thisEntry.getKey();
	   Object value = thisEntry.getValue();
	   String dynamicImageDefaultUrl = (String) ((RepositoryItem) value).getPropertyValue("dynamicImageDefaultUrl");
	   if(dynamicImageDefaultUrl!=null){
		   skuImageUrlMap.put((String) imgKey, dynamicImageDefaultUrl);
	   }
	 }
	return skuImageUrlMap;
 }
  
  /**
   * Gets the web filter color.
   *
   * @return the web filter color
   */
  public String getWebFilterColor() {
    return (String) getDataSource().getPropertyValue("webFilterColor");
  }
}
