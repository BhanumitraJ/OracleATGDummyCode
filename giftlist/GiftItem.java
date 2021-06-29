package com.nm.commerce.giftlist;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import atg.repository.MutableRepositoryItem;
import atg.repository.Repository;
import atg.repository.RepositoryException;
import atg.repository.RepositoryItem;
import atg.repository.RepositoryView;
import atg.repository.rql.RqlStatement;

import com.nm.commerce.catalog.NMProduct;
import com.nm.commerce.catalog.NMSku;
import com.nm.components.CommonComponentHelper;
import com.nm.utils.ProdSkuUtil;

// this class is a wrapper for the repository item gift-item
public class GiftItem {

  // default name for the virtual category, if one doesn't exist for the dept
  // code this item contains it uses this string to identify it
  public static final String DEFAULT_VCAT_ID = "DEFAULT";

  private String vCategoryId = DEFAULT_VCAT_ID;
  private String statusString; // contains the status i.e. in stock, back order etc..
  private String availableDateString; // contains the date if back ordered
  private String prodVariation1 = null;
  private String prodVariation2 = null;
  private String prodVariation3 = null;
  private ProdSkuUtil prodSkuUtil;
  private Repository storeRepository;
  private MutableRepositoryItem dataSource;
  private int mRequestedQuantityDesired = -1;

  public GiftItem() {}

  public GiftItem(final MutableRepositoryItem mutableRepositoryItem) {
    setDataSource(mutableRepositoryItem);
  }

  // set the repository item that this classes functions will use
  public void setDataSource(final MutableRepositoryItem dataSource) {
    this.dataSource = dataSource;
  }

  public MutableRepositoryItem getDataSource() {
    return dataSource;
  }

  // Prod Variation functions are untested and are here to make it easier for
  // use on other sites
  // It should pull the correct prodVariation off of the sku_prod_info table
  public String getProdVariation1() {
    try {
      if (prodVariation1 == null) {
        final Map skuProdInfo = (Map) getSkuRepositoryItem().getPropertyValue("skuProdInfo");
        setProdVariation1((String) (((RepositoryItem) skuProdInfo.get(getProductId())).getPropertyValue("cmVariation1")));
        if (prodVariation1 == null) {
          return "";
        } else {
          return prodVariation1;
        }
      } else {
        return prodVariation1;
      }
    } catch (final Exception e) {
      return "";
    }
  }

  public void setProdVariation1(final String prodVariation1) {
    this.prodVariation1 = prodVariation1;
  }

  // Prod Variation functions are untested and are here to make it easier for
  // use on other sites
  // It should pull the correct prodVariation off of the sku_prod_info table
  public String getProdVariation2() {
    try {
      if (prodVariation2 == null) {
        final Map skuProdInfo = (Map) getSkuRepositoryItem().getPropertyValue("skuProdInfo");
        setProdVariation2((String) (((RepositoryItem) skuProdInfo.get(getProductId())).getPropertyValue("cmVariation2")));
        if (prodVariation2 == null) {
          return "";
        } else {
          return prodVariation2;
        }
      } else {
        return prodVariation2;
      }
    } catch (final Exception e) {
      return "";
    }
  }

  public void setProdVariation2(final String prodVariation2) {
    this.prodVariation2 = prodVariation2;
  }

  // Prod Variation functions are untested and are here to make it easier for
  // use on other sites
  // It should pull the correct prodVariation off of the sku_prod_info table
  public String getProdVariation3() {
    try {
      if (prodVariation3 == null) {
        final Map skuProdInfo = (Map) getSkuRepositoryItem().getPropertyValue("skuProdInfo");
        setProdVariation3((String) (((RepositoryItem) skuProdInfo.get(getProductId())).getPropertyValue("cmVariation3")));
        if (prodVariation3 == null) {
          return "";
        } else {
          return prodVariation3;
        }
      } else {
        return prodVariation3;
      }
    } catch (final Exception e) {
      return "";
    }
  }

  public void setProdVariation3(final String prodVariation3) {
    this.prodVariation3 = prodVariation3;
  }

  // public void setId(String id) { getDataSource().setPropertyValue("id"); }
  public String getId() {
    return getDataSource().getRepositoryId();
  }

  // Dynamo sku Id for this this gift item
  public void setType(final Integer type) {
    getDataSource().setPropertyValue("type", type);
  }

  public Integer getType() {
    final Integer defaultValue = new Integer(0);
    Integer retValue = (Integer) getDataSource().getPropertyValue("type");

    if (retValue == null) {
      retValue = defaultValue;
    }

    return retValue;
  }

  // Dynamo sku Id for this this gift item
  public void setPriority(final int priority) {
    getDataSource().setPropertyValue("sortPriority", new Integer(priority));
  }

  public int getPriority() {
    int retValue = 0;
    final Integer priority = (Integer) getDataSource().getPropertyValue("sortPriority");
    if (priority != null) {
      retValue = priority.intValue();
    }

    return retValue;
  }

  public String getStuffedPriority() {
    String retValue = "000";
    final Integer tempPriority = (Integer) getDataSource().getPropertyValue("sortPriority");
    if (tempPriority == null) {
      return retValue;
    }
    final String priority = tempPriority.toString().trim();
    if (priority.length() > 0) {
      retValue = (priority.length() == 1) ? ("00" + priority) : ((priority.length() == 2) ? ("0" + priority) : priority);
    }

    return retValue;
  }

  // Dynamo sku Id for this this gift item
  public void setCatalogRefId(final String catalogRefId) {
    getDataSource().setPropertyValue("catalogRefId", catalogRefId);
  }

  public String getCatalogRefId() {
    return (String) getDataSource().getPropertyValue("catalogRefId");
  }

  // Dynamo prod id for this gift item
  public void setProductId(final String productId) {
    getDataSource().setPropertyValue("productId", productId);
  }

  public String getProductId() {
    return (String) getDataSource().getPropertyValue("productId");
  }

  public void setDisplayName(final String displayName) {
    getDataSource().setPropertyValue("displayName", displayName);
  }

  public String getDisplayName() {
    return (String) getDataSource().getPropertyValue("displayName");
  }

  public void setDescription(final String description) {
    getDataSource().setPropertyValue("description", description);
  }

  public String getDescription() {
    return (String) getDataSource().getPropertyValue("description");
  }

  // quantityDesired is the amount requested for the item on the gift list.
  public void setQuantityDesired(final long quantityDesired) {
    try {
      final Long quantityDesiredLong = new Long(quantityDesired);
      if (quantityDesiredLong.longValue() >= 0) {
        getDataSource().setPropertyValue("quantityDesired", quantityDesiredLong);
      }
    } catch (final NumberFormatException nfe) {}
  }

  public long getQuantityDesired() {
    final Long quantityDesiredLong = (Long) getDataSource().getPropertyValue("quantityDesired");
    if (quantityDesiredLong == null) {
      return 0;
    } else {
      return quantityDesiredLong.intValue();
    }

  }

  // sets the quantity that gift registry buyers have purchased for this item
  public void setQuantityPurchased(final int quantityPurchased) {
    try {
      final Long quantityPurchasedLong = new Long(quantityPurchased);
      if (quantityPurchasedLong.longValue() >= 0) {
        getDataSource().setPropertyValue("quantityPurchased", quantityPurchasedLong);
      }
    } catch (final NumberFormatException nfe) {}
  }

  public long getQuantityPurchased() {
    final Long quantityPurchasedLong = (Long) getDataSource().getPropertyValue("quantityPurchased");
    if (quantityPurchasedLong == null) {
      return 0;
    } else {
      return quantityPurchasedLong.longValue();
    }
  }

  // temp value for quantity box on manageRegistryPage, before the respository
  // item is changed, we have to validate if we can change the
  // quantityDesired.
  public void setRequestedQuantityDesired(final int requestedQuantityDesired) {
    if (requestedQuantityDesired >= 0) {
      mRequestedQuantityDesired = requestedQuantityDesired;
    }
  }

  public long getRequestedQuantityDesired() {
    if (mRequestedQuantityDesired < 0) {
      return getQuantityDesired();
    } else {
      return mRequestedQuantityDesired;
    }
  }

  public void setVCategoryId(final String vCategoryId) {
    this.vCategoryId = vCategoryId;
  }

  public String getVCategoryId() {
    return vCategoryId;
  }

  // quantity desired - quantity purchased
  public long getQuantityStillNeeded() {
    final long stillNeeded = getQuantityDesired() - getQuantityPurchased();
    if (stillNeeded <= 0l) {
      return 0;
    } else {
      return stillNeeded;
    }
  }

  // determines if the product can be sold
  public boolean getIsSellable() {
    boolean returnValue = false;
    // Check if the item type is store-item and if it is then return false.
    if ((dataSource.getPropertyValue("type") != null) && (((Integer) dataSource.getPropertyValue("type")).intValue() == 1)) {
      return returnValue;
    }
    try {
      returnValue = getProdSkuUtil().isSellableSku(getProductRepositoryItem(), getSkuRepositoryItem());
      return returnValue;
    } catch (final Exception e) {
      return false;
    }
  }

  public NMSku getNMSku(){
    return new NMSku(getSkuRepositoryItem());
  }

  public MutableRepositoryItem getSkuRepositoryItem() {
    return getProdSkuUtil().getMutableSkuRepositoryItem(getCatalogRefId());
  }

  public RepositoryItem getProductRepositoryItem() {
    RepositoryItem retValue = null;
    try {
      // Check the gift Item type and return the sku item from the Store
      // Catalog
      final Integer itemType = getType();
      if ((itemType != null) && (itemType.intValue() == 1)) {
        retValue = getStoreCatalog().getItem(getCatalogRefId(), "sku");
      } else {
        retValue = getProdSkuUtil().getMutableProductRepositoryItem(getProductId());
      }
    } catch (final RepositoryException re) {
      re.printStackTrace();
    }
    return retValue;
  }

  // get the date string for back order
  public String getAvailableDateString() {
    if ((availableDateString == null) || availableDateString.equals("")) {
      return getProdSkuUtil().getAvailableDateString(getProductRepositoryItem(), getSkuRepositoryItem(), null);
    } else {
      return availableDateString;
    }
  }

  // gets the stock status
  public String getStatusString() {
    if ((statusString == null) || statusString.equals("")) {
      return CommonComponentHelper.getProdSkuUtil().getStatusString(getProductRepositoryItem(), getSkuRepositoryItem());
    } else {
      return statusString;
    }
  }

  // sorts by virtual category
  public static Comparator VCAT_COMPARATOR = new Comparator() {
    @Override
    public int compare(final Object giftItem1, final Object giftItem2) {
      final String vCatId1 = ((GiftItem) giftItem1).getVCategoryId();
      final String vCatId2 = ((GiftItem) giftItem2).getVCategoryId();
      if (vCatId1.equalsIgnoreCase(GiftItem.DEFAULT_VCAT_ID)) {
        return 1;
      } else if (vCatId2.equalsIgnoreCase(GiftItem.DEFAULT_VCAT_ID)) {
        return -1;
      } else {
        return vCatId1.compareTo(vCatId2);
      }
    }
  };

  // sorts by virtual category
  public static Comparator VCAT_PRIORITY_COMPARATOR = new Comparator() {
    @Override
    public int compare(final Object giftItem1, final Object giftItem2) {
      final String vCatId1 = ((GiftItem) giftItem1).getVCategoryId() + ((GiftItem) giftItem1).getStuffedPriority();
      final String vCatId2 = ((GiftItem) giftItem2).getVCategoryId() + ((GiftItem) giftItem2).getStuffedPriority();
      if (vCatId1.equalsIgnoreCase(GiftItem.DEFAULT_VCAT_ID)) {
        return 1;
      } else if (vCatId2.equalsIgnoreCase(GiftItem.DEFAULT_VCAT_ID)) {
        return -1;
      } else {
        return vCatId1.compareTo(vCatId2);
      }
    }
  };

  // sorts by sale descending
  public static Comparator SALE_PRICE_DESC_COMPARATOR = new Comparator() {
    @Override
    public int compare(final Object giftItem1, final Object giftItem2) {
      Double salePrice1;
      Double salePrice2;
      try {
        final Integer itemType = ((GiftItem) giftItem1).getType();
        if ((itemType != null) && (itemType.intValue() == 1)) {
          salePrice1 = (Double) ((GiftItem) giftItem1).getProductRepositoryItem().getPropertyValue("price");
        } else {
          salePrice1 = (Double) ((GiftItem) giftItem1).getProductRepositoryItem().getPropertyValue("salePrice");
        }
      } catch (final Exception e) {
        salePrice1 = new Double("0");
      }
      try {
        final Integer itemType = ((GiftItem) giftItem2).getType();
        if ((itemType != null) && (itemType.intValue() == 1)) {
          salePrice2 = (Double) ((GiftItem) giftItem2).getProductRepositoryItem().getPropertyValue("price");
        } else {
          salePrice2 = (Double) ((GiftItem) giftItem2).getProductRepositoryItem().getPropertyValue("salePrice");
        }
      } catch (final Exception e) {
        salePrice2 = new Double("0");
      }
      return salePrice2.compareTo(salePrice1);
    }
  };

  // sorts by sale ascending
  public static Comparator SALE_PRICE_ASC_COMPARATOR = new Comparator() {
    @Override
    public int compare(final Object giftItem1, final Object giftItem2) {
      Double salePrice1;
      Double salePrice2;
      try {
        final Integer itemType = ((GiftItem) giftItem1).getType();

        if ((itemType != null) && (itemType.intValue() == 1)) {
          salePrice1 = (Double) ((GiftItem) giftItem1).getProductRepositoryItem().getPropertyValue("price");
        } else {
          salePrice1 = (Double) ((GiftItem) giftItem1).getProductRepositoryItem().getPropertyValue("salePrice");
        }
      } catch (final Exception e) {
        salePrice1 = new Double("0");
      }
      try {
        final Integer itemType = ((GiftItem) giftItem2).getType();

        if ((itemType != null) && (itemType.intValue() == 1)) {
          salePrice2 = (Double) ((GiftItem) giftItem2).getProductRepositoryItem().getPropertyValue("price");
        } else {
          salePrice2 = (Double) ((GiftItem) giftItem2).getProductRepositoryItem().getPropertyValue("salePrice");
        }
      } catch (final Exception e) {
        System.out.println("Caught Exception");
        salePrice2 = new Double("0");
      }
      return salePrice1.compareTo(salePrice2);
    }
  };

  // sorts by still need descending
  public static Comparator STILL_NEED_DESC_COMPARATOR = new Comparator() {
    @Override
    public int compare(final Object giftItem1, final Object giftItem2) {
      Long stillNeed1;
      Long stillNeed2;
      try {
        stillNeed1 = new Long(((GiftItem) giftItem1).getQuantityStillNeeded());
      } catch (final Exception e) {
        stillNeed1 = new Long("-999");
      }
      try {
        stillNeed2 = new Long(((GiftItem) giftItem2).getQuantityStillNeeded());
      } catch (final Exception e) {
        stillNeed2 = new Long("-999");
      }
      return stillNeed2.compareTo(stillNeed1);
    }
  };

  // sorts by brand
  public static Comparator BRAND_COMPARATOR = new Comparator() {
    @Override
    public int compare(final Object giftItem1, final Object giftItem2) {
      String cmDesignerName1;
      String cmDesignerName2;
      try {
        final Integer itemType = ((GiftItem) giftItem1).getType();
        if ((itemType != null) && (itemType.intValue() == 1)) {
          cmDesignerName1 = (String) ((GiftItem) giftItem1).getProductRepositoryItem().getPropertyValue("brand");
        } else {
          cmDesignerName1 = (String) ((GiftItem) giftItem1).getProductRepositoryItem().getPropertyValue("cmDesignerName");
        }
        if (cmDesignerName1 == null) {
          cmDesignerName1 = "ZZZZ";
        }
      } catch (final Exception e) {
        cmDesignerName1 = new String("");
      }
      try {
        final Integer itemType = ((GiftItem) giftItem2).getType();
        if ((itemType != null) && (itemType.intValue() == 1)) {
          cmDesignerName2 = (String) ((GiftItem) giftItem2).getProductRepositoryItem().getPropertyValue("brand");
        } else {
          cmDesignerName2 = (String) ((GiftItem) giftItem2).getProductRepositoryItem().getPropertyValue("cmDesignerName");
        }

        if (cmDesignerName2 == null) {
          cmDesignerName2 = "ZZZZ";
        }

      } catch (final Exception e) {
        cmDesignerName2 = new String("");
      }
      return cmDesignerName1.compareTo(cmDesignerName2);
    }
  };

  public RepositoryItem getSuiteRI() {
    try {
      final Object[] params = new Object[1];
      params[0] = getProductId();

      final RepositoryView view = CommonComponentHelper.getProductRepository().getView("suite");
      final RqlStatement statement = RqlStatement.parseRqlStatement("subproducts includes item (id = ?0)");
      final RepositoryItem[] items = statement.executeQuery(view, params);
      if ((items != null) && (items.length > 0) && (items[0] != null)) {
        return items[0];
      } else {
        return null;
      }
    } catch (final Exception e) {
      return null;
    }
  }

  /*
   * set method for Special Instructions Map
   */
  public void setSIMap(final HashMap siMap) {
    getDataSource().setPropertyValue("specialInstCodes", siMap);
  }

  /*
   * get method for Special Instructions Map
   */
  public Map getSIMap() {
    return (Map) getDataSource().getPropertyValue("specialInstcodes");
  }

  /*
   * empty check for siMap
   */
  public boolean hasSICodes() {
    if ((getDataSource().getPropertyValue("specialInstcodes") != null) && !((Map) getDataSource().getPropertyValue("specialInstcodes")).isEmpty()) {
      return true;
    } else {
      return false;
    }
  }

  private ProdSkuUtil getProdSkuUtil() {
    if (this.prodSkuUtil == null) {
      this.prodSkuUtil = CommonComponentHelper.getProdSkuUtil();
    }
    return this.prodSkuUtil;
  }

  private Repository getStoreCatalog() {
    if (this.storeRepository == null) {
      this.storeRepository = CommonComponentHelper.getStoreCatalog();
    }
    return this.storeRepository;
  }

  public String getImageUrl() {
    String returnValue = null;
    final NMProduct product = getProduct();
    returnValue = product.getImageUrl("mg");
    if (returnValue == null) {
      final RepositoryItem suite = getSuiteRI();
      if (suite != null) {
        @SuppressWarnings("unchecked")
        final
        Map<String, RepositoryItem> auxiliaryMedia = (Map<String, RepositoryItem>) suite.getPropertyValue("auxiliaryMedia");
        if (auxiliaryMedia != null) {
          final RepositoryItem mediaItem = auxiliaryMedia.get("mg");
          if (mediaItem != null) {
            returnValue = (String) mediaItem.getPropertyValue("url");
          }
        }
      }
    }

    return returnValue;
  }

  public String getCanonicalUrl() {
    String returnValue = null;
    final NMProduct product = getProduct();
    returnValue = product.getCanonicalUrl();
    if (returnValue == null) {
      final RepositoryItem suite = getSuiteRI();
      if (suite != null) {
        returnValue = (String) suite.getPropertyValue("canonicalUrl");
      }
    }

    return returnValue;
  }

  public NMProduct getProduct() {
    return new NMProduct(getProductId());
  }

  public boolean getProductHasSwatchMedia() {
    final String productId = getProductId();
    final String skuId = getCatalogRefId();
    String suiteId = null;

    final RepositoryItem suite = getSuiteRI();
    if (suite != null) {
      suiteId = suite.getRepositoryId();
    }

    return prodSkuUtil.getProductHasSwatchMedia(productId, suiteId, skuId);
  }
}
