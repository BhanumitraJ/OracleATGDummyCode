package com.nm.commerce;

import static com.nm.common.INMGenericConstants.DOUBLE_ZERO;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import atg.core.util.StringUtils;
import atg.repository.RepositoryItem;

import com.nm.utils.StringUtilities;

public class NMCommerceItemTempHolder {
  
  private String reqDeliveryDate;
  private String dynamoSkuId;
  private String dynamoProductId;
  private String dynamoCategoryId;
  private String qty;
  private String deptCode;
  private String sourceCode;
  private String webDesignerName;
  private String cmosCatalogId;
  private String cmosSKUId;
  private String cmosItemCode;
  private String cmosProdName;
  private String prodAdvVariation1;
  private String prodAdvVariation2;
  private String prodAdvVariation3;
  private boolean cqo;
  private boolean perishable;
  private String transientAvailableDate;
  private boolean autoGiftwrapFlg;
  private String transientStatus;
  private String commerceItemId;
  private boolean dropship;
  private boolean inStock;
  // private boolean dropshipInHouse;
  private String suiteId;
  private String promoType;
  private boolean hasValuesForAllSpecialInst;
  private String codeSetType;
  private HashMap<String, String> specialInstCodes = new HashMap<String, String>();
  private ArrayList<Integer> availableMatchingCodeSets = new ArrayList<Integer>();
  private boolean flgPromoQualifier;
  private boolean parentheticalPromo;
  private boolean flgPromoQtyUnrestricted;
  private boolean flgPromoProd;
  private ArrayList<String> promoProds;
  private ArrayList<String> promoQualifiers;
  private String promoTimeStamp;
  private String coreMetricsCategory;
  private String relatedProduct;
  private String promoName;
  private String sendCmosPromoCode;
  private String sendCmosPromoKey;
  private String registryId;
  private NMCommerceItemTempHolder promoProd;
  private NMCommerceItemTempHolder promoQualifier;
  private String swatchHexColor;
  private String swatchMediaLargeId;
  private String swatchMediaSmallId;
  private boolean flgBngl;
  private String storeNum;
  private String selectedInterval;
  private String dynamicImageUrl;
  private String specialInstructionFlag;
  private Double specialInstructionPrice;
  
  /***********************************************
   * The default gift wrap properties for a NMGR item are different from other items (wishlist, other brand GRs, regular catalog, etc.). To accomodate this, these default values need to be assigned to
   * an NMGR item during the beginning of its life as a commerce item. The addition of the following 5 properties here and setting them accordingly later will take care of this.
   ***********************************************/
  private RepositoryItem giftWrap;
  private int giftNote;
  private String noteLine1;
  private String noteLine2;
  private boolean giftWrapSeparately;
  // added for NMOBLDS-2167
  /** The configuration key. */
  private String configurationKey;
  // added for NMOBLDS-2184
  /** The option choices. */
  private Map<String, String> optionChoices = new HashMap<String, String>();
  /** The additional cost. */
  private double configSetPrice;
  
  public NMCommerceItemTempHolder() {
    transientAvailableDate = "";
    autoGiftwrapFlg = false;
    transientStatus = "";
    reqDeliveryDate = "";
    dynamoSkuId = "";
    dynamoProductId = "";
    dynamoCategoryId = "";
    qty = "";
    deptCode = "";
    sourceCode = "";
    webDesignerName = "";
    cmosCatalogId = "";
    cmosSKUId = "";
    cmosItemCode = "";
    cmosProdName = "";
    prodAdvVariation1 = "";
    prodAdvVariation2 = "";
    prodAdvVariation3 = "";
    cqo = false;
    flgBngl = false;
    perishable = false;
    commerceItemId = "";
    hasValuesForAllSpecialInst = false;
    flgPromoQualifier = false;
    flgPromoQtyUnrestricted = false;
    dropship = false;
    // dropshipInHouse = false;
    inStock = false;
    flgPromoProd = false;
    parentheticalPromo = false;
    promoProds = new ArrayList<String>();
    promoQualifiers = new ArrayList<String>();
    promoTimeStamp = "";
    coreMetricsCategory = "";
    relatedProduct = "";
    promoName = "";
    sendCmosPromoCode = "";
    sendCmosPromoKey = "";
    registryId = "";
    giftWrap = null;
    giftNote = 0;
    noteLine1 = "";
    noteLine2 = "";
    giftWrapSeparately = false;
    swatchHexColor = "";
    swatchMediaLargeId = "";
    swatchMediaSmallId = "";
    selectedInterval = "";
    specialInstructionFlag = "";
    specialInstructionPrice = DOUBLE_ZERO;
  }
  
  public void setRegistryId(String registryId) {
    this.registryId = registryId;
  }
  
  public String getRegistryId() {
    return registryId;
  }
  
  public int getGiftNote() {
    return giftNote;
  }
  
  public void setGiftNote(int i) {
    giftNote = i;
  }
  
  public RepositoryItem getGiftWrap() {
    return giftWrap;
  }
  
  public void setGiftWrap(RepositoryItem item) {
    giftWrap = item;
  }
  
  public String getNoteLine1() {
    return noteLine1;
  }
  
  public void setNoteLine1(String string) {
    noteLine1 = string;
  }
  
  public String getNoteLine2() {
    return noteLine2;
  }
  
  public void setNoteLine2(String string) {
    noteLine2 = string;
  }
  
  public boolean isGiftWrapSeparately() {
    return giftWrapSeparately;
  }
  
  public void setGiftWrapSeparately(boolean pGiftWrapSeparately) {
    giftWrapSeparately = pGiftWrapSeparately;
  }
  
  public boolean getFlgPromoQualifier() {
    return this.flgPromoQualifier;
  }
  
  public void setFlgPromoQualifier(boolean flgPromoQualifier) {
    this.flgPromoQualifier = flgPromoQualifier;
  }
  
  public boolean getFlgPromoQtyUnrestricted() {
    return this.flgPromoQtyUnrestricted;
  }
  
  public void setFlgPromoQtyUnrestricted(boolean flgPromoQtyUnrestricted) {
    this.flgPromoQtyUnrestricted = flgPromoQtyUnrestricted;
  }
  
  public boolean getFlgPromoProd() {
    return this.flgPromoProd;
  }
  
  public void setFlgPromoProd(boolean flgPromoProd) {
    this.flgPromoProd = flgPromoProd;
  }
  
  public ArrayList<String> getPromoProds() {
    return this.promoProds;
  }
  
  public void setPromoProds(ArrayList<String> promoProds) {
    this.promoProds = promoProds;
  }
  
  public void setPromoProds(int index, String s) {
    this.promoProds.add(s);
  }
  
  public String getPromoProds(int index) {
    try {
      return ((String) promoProds.get(index));
    } catch (Exception e) {
      return "";
    }
  }
  
  public ArrayList<String> getPromoQualifiers() {
    return this.promoQualifiers;
  }
  
  public void setPromoQualifiers(ArrayList<String> promoQualifiers) {
    this.promoQualifiers = promoQualifiers;
  }
  
  public void setPromoQualifiers(int index, String s) {
    this.promoQualifiers.add(s);
  }
  
  public String getPromoQualifiers(int index) {
    try {
      return ((String) promoQualifiers.get(index));
    } catch (Exception e) {
      return "";
    }
  }
  
  public String getPromoTimeStamp() {
    return this.promoTimeStamp;
  }
  
  public void setPromoTimeStamp(String promoTimeStamp) {
    this.promoTimeStamp = promoTimeStamp;
  }
  
  public String getCoreMetricsCategory() {
    return this.coreMetricsCategory;
  }
  
  public void setCoreMetricsCategory(String coreMetricsCategory) {
    this.coreMetricsCategory = coreMetricsCategory;
  }
  
  public String getRelatedProduct() {
    return this.relatedProduct;
  }
  
  public void setRelatedProduct(String pRelatedProduct) {
    this.relatedProduct = pRelatedProduct;
  }
  
  public String getSendCmosPromoCode() {
    return this.sendCmosPromoCode;
  }
  
  public void setSendCmosPromoCode(String sendCmosPromoCodeIn) {
    this.sendCmosPromoCode = StringUtilities.addValueToDelimitedString(sendCmosPromoCodeIn, this.sendCmosPromoCode, ",", 256);
  }
  
  public String getSendCmosPromoKey() {
    return this.sendCmosPromoKey;
  }
  
  public void setSendCmosPromoKey(String sendCmosPromoKeyIn) {
    this.sendCmosPromoKey = StringUtilities.addValueToDelimitedString(sendCmosPromoKeyIn, this.sendCmosPromoKey, ",", 256);
  }
  
  public String getPromoName() {
    return this.promoName;
  }
  
  public void setPromoName(String promoName) {
    this.promoName = promoName;
  }
  
  public NMCommerceItemTempHolder getPromoProd() {
    return this.promoProd;
  }
  
  public void setPromoProd(NMCommerceItemTempHolder promoProd) {
    this.promoProd = promoProd;
  }
  
  public NMCommerceItemTempHolder getPromoQualifier() {
    return this.promoQualifier;
  }
  
  public void setPromoQualifier(NMCommerceItemTempHolder promoQualifier) {
    this.promoQualifier = promoQualifier;
  }
  
  public ArrayList<Integer> getAvailableMatchingCodeSets() {
    return this.availableMatchingCodeSets;
  }
  
  public void setAvailableMatchingCodeSets(ArrayList<Integer> availableMatchingCodeSets) {
    this.availableMatchingCodeSets = availableMatchingCodeSets;
  }
  
  public String getCodeSetType() {
    return this.codeSetType;
  }
  
  public void setCodeSetType(String codeSetType) {
    this.codeSetType = codeSetType;
  }
  
  public void setPromoType(String promoType) {
    this.promoType = promoType;
  }
  
  public String getPromoType() {
    return this.promoType;
  }
  
  public HashMap<String, String> getSpecialInstCodes() {
  	if(!StringUtils.isBlank(getConfigurationKey()))
  		return new HashMap<String, String>();
    return this.specialInstCodes;
  }
  
  public void setSpecialInstCodes(HashMap<String, String> specialInstCodes) {
    this.specialInstCodes = specialInstCodes;
  }
  
  public void setSuiteId(String suiteId) {
    this.suiteId = suiteId;
  }
  
  public String getSuiteId() {
    return suiteId;
  }
  
  public void setCommerceItemId(String inCI) {
    commerceItemId = inCI;
  }
  
  public String getCommerceItemId() {
    return commerceItemId;
  }
  
  public void setTransientAvailableDate(String str) {
    transientAvailableDate = str;
  }
  
  public String getTransientAvailableDate() {
    return transientAvailableDate;
  }
  
  public void setAutoGiftwrapFlg(boolean b) {
    this.autoGiftwrapFlg = b;
  }
  
  public boolean getAutoGiftwrapFlg() {
    return autoGiftwrapFlg;
  }
  
  public void setTransientStatus(String str) {
    transientStatus = str;
  }
  
  public String getTransientStatus() {
    return transientStatus;
  }
  
  public Date getReqDeliveryDateInDateFormat() {
    DateFormat df;
    Date d = null;
    try {
      df = DateFormat.getDateInstance(DateFormat.SHORT);
      d = df.parse(reqDeliveryDate);
    } catch (Exception e) {
      // System.out.println("GET---------reqDeliveryDateInDateFormat--Invalid Date Format--------->" + reqDeliveryDate + "");
    }
    // System.out.println("GET---------reqDeliveryDateInDateFormat---------->" + d + "");
    return d;
  }
  
  public void setReqDeliveryDate(String d) {
    // System.outprintln("SET---------reqDeliveryDate---------->" + d + "");
    this.reqDeliveryDate = d;
  }
  
  public String getReqDeliveryDate() {
    // System.outprintln("GET---------reqDeliveryDate---------->" + reqDeliveryDate + "");
    return reqDeliveryDate;
  }
  
  public void setDynamoSkuId(String str) {
    // System.outprintln("SET---------dynamoSkuId---------->" + str + "");
    if (str.trim().equals("")) {
      this.dynamoSkuId = null;
    } else {
      this.dynamoSkuId = str;
    }
  }
  
  public String getDynamoSkuId() {
    // System.outprintln("GET---------dynamoSkuId---------->" + dynamoSkuId + "");
    return dynamoSkuId;
  }
  
  public void setDynamoProductId(String str) {
    // System.outprintln("SET---------dynamoProductId------>" + str + "");
    this.dynamoProductId = str;
  }
  
  public String getDynamoProductId() {
    // System.outprintln("GET---------dynamoProductId------>" + dynamoProductId + "");
    return dynamoProductId;
  }
  
  public void setDynamoCategoryId(String str) {
    // System.outprintln("SET---------dynamoCategoryId----->" + str + "");
    this.dynamoCategoryId = str;
  }
  
  public String getDynamoCategoryId() {
    // System.outprintln("GET---------dynamoCategoryId----->" + dynamoCategoryId + "");
    return dynamoCategoryId;
  }
  
  public void setQty(String str) {
    // System.outprintln("SET---------qty------------------>" + str + "");
    try {
      int i = Integer.parseInt(str.trim());
      if (i < 0) {
        this.qty = "0";
      } else {
        this.qty = "" + i;
      }
    } catch (NumberFormatException nfe) {
      this.qty = "0";
    } catch (NullPointerException npe) {
      this.qty = "0";
    }
  }
  
  public String getQty() {
    // System.outprintln("GET---------qty------------------>" + qty + "");
    return qty;
  }
  
  public void setDeptCode(String deptCode) {
    this.deptCode = deptCode;
  }
  
  public String getDeptCode() {
    return deptCode;
  }
  
  public void setSourceCode(String sourceCode) {
    this.sourceCode = sourceCode;
  }
  
  public String getSourceCode() {
    return sourceCode;
  }
  
  public void setWebDesignerName(String webDesignerName) {
    this.webDesignerName = webDesignerName;
  }
  
  public String getWebDesignerName() {
    return webDesignerName;
  }
  
  public void setCmosCatalogId(String str) {
    // System.outprintln("SET---------cmosCatalogId-------->" + str + "");
    this.cmosCatalogId = str;
  }
  
  public String getCmosCatalogId() {
    // System.outprintln("GET---------cmosCatalogId-------->" + cmosCatalogId + "");
    return cmosCatalogId;
  }
  
  public void setCmosSKUId(String str) {
    // System.outprintln("SET---------cmosSKUId------------>" + str + "");
    this.cmosSKUId = str;
  }
  
  public String getCmosSKUId() {
    // System.outprintln("GET---------cmosSKUId------------>" + cmosSKUId + "");
    return cmosSKUId;
  }
  
  public void setCmosItemCode(String str) {
    // System.outprintln("SET---------cmosItemCode--------->" + str + "");
    this.cmosItemCode = str;
  }
  
  public String getCmosItemCode() {
    // System.outprintln("GET---------cmosItemCode--------->" + cmosItemCode + "");
    return cmosItemCode;
  }
  
  public void setCmosProdName(String str) {
    // System.outprintln("SET---------cmosProdName--------->" + str + "");
    this.cmosProdName = str;
  }
  
  public String getCmosProdName() {
    // System.outprintln("GET---------cmosProdName--------->" + cmosProdName + "");
    return cmosProdName;
  }
  
  public void setProdAdvVariation1(String str) {
    // System.outprintln("SET---------prodAdvVariation1---->" + str + "");
    this.prodAdvVariation1 = str;
  }
  
  public String getProdAdvVariation1() {
    // System.outprintln("GET---------prodAdvVariation1---->" + prodAdvVariation1 + "");
    return prodAdvVariation1;
  }
  
  public void setProdAdvVariation2(String str) {
    // System.outprintln("SET---------prodAdvVariation2---->" + str + "");
    this.prodAdvVariation2 = str;
  }
  
  public String getProdAdvVariation2() {
    // System.outprintln("GET---------prodAdvVariation2---->" + prodAdvVariation2 + "");
    return prodAdvVariation2;
  }
  
  public void setProdAdvVariation3(String str) {
    // System.outprintln("SET---------prodAdvVariation3---->" + str + "");
    this.prodAdvVariation3 = str;
  }
  
  public String getProdAdvVariation3() {
    // System.outprintln("GET---------prodAdvVariation3---->" + prodAdvVariation3 + "");
    return prodAdvVariation3;
  }
  
  public void setCqo(boolean b) {
    // System.outprintln("SET---------cqo---->" + cqo + "");
    this.cqo = b;
  }
  
  public boolean getCqo() {
    // System.outprintln("GET---------cqo---->" + cqo + "");
    return cqo;
  }
  
  public void setFlgBngl(boolean b) {
    // System.out.println("SET---------flgBngl---->" + flgBngl + "");
    this.flgBngl = b;
  }
  
  public boolean getFlgBngl() {
    // System.out.println("GET---------flgBngl---->" + flgBngl + "");
    return flgBngl;
  }
  
  public void setPerishable(boolean b) {
    // System.outprintln("SET---------cqo---->" + cqo + "");
    this.perishable = b;
  }
  
  public boolean getPerishable() {
    // System.outprintln("GET---------cqo---->" + cqo + "");
    return perishable;
  }
  
  public void setHasValuesForAllSpecialInst(boolean b) {
    // System.outprintln("SET---------hasValuesForAllSpecialInst---->" + hasValuesForAllSpecialInst + "");
    this.hasValuesForAllSpecialInst = b;
  }
  
  public boolean getHasValuesForAllSpecialInst() {
    // System.outprintln("GET---------hasValuesForAllSpecialInst---->" + hasValuesForAllSpecialInst + "");
    /*
     * Though this method could interrogate the hashmap for null values, we are going to rely on the SIProdhandler.handleLastStep() to set the boolean hasValuesForAllSpecialInst instead. This is
     * because the handler knows better if there may be allowable null values (e.g. 2nd line address).
     */
    return hasValuesForAllSpecialInst;
  }
  
  public String getAlphabetizedSIString() {
    String alphabetizedSIString = "";
    if (StringUtilities.isEmpty(getCodeSetType())) {
    	return alphabetizedSIString;
    }
    List<String> list = new ArrayList<String>();
    
    Set<String> keySet = specialInstCodes.keySet();
    Iterator<String> iKeySet = keySet.iterator();
    
    while (iKeySet.hasNext()) {
      list.add(iKeySet.next());
    }
    
    Collections.sort(list);
    Iterator<String> iList = list.iterator();
    
    while (iList.hasNext()) {
      alphabetizedSIString += (String) iList.next();
    }
    return alphabetizedSIString;
  }
  
  @Override
  public String toString() {
    String str = "";
    
    // str += "[," + + "]\n";
    
    str += "\n---- NMCommerceItemTempHolder ----\n";
    str += "[reqDeliveryDate," + reqDeliveryDate + "]\n";
    str += "[dynamoSkuId," + dynamoSkuId + "]\n";
    str += "[dynamoProductId," + dynamoProductId + "]\n";
    str += "[dynamoCategoryId," + dynamoCategoryId + "]\n";
    str += "[qty," + qty + "]\n";
    str += "[deptCode," + deptCode + "]\n";
    str += "[sourceCode," + sourceCode + "]\n";
    str += "[webDesignerName," + webDesignerName + "]\n";
    str += "[cmosCatalogId," + cmosCatalogId + "]\n";
    str += "[cmosSKUId," + cmosSKUId + "]\n";
    str += "[cmosItemCode," + cmosItemCode + "]\n";
    str += "[cmosProdName," + cmosProdName + "]\n";
    str += "[prodAdvVariation1," + prodAdvVariation1 + "]\n";
    str += "[prodAdvVariation2," + prodAdvVariation2 + "]\n";
    str += "[prodAdvVariation3," + prodAdvVariation3 + "]\n";
    str += "[cqo," + cqo + "]\n";
    str += "[flgBngl," + flgBngl + "]\n";
    str += "[hasValuesForAllSpecialInst," + hasValuesForAllSpecialInst + "]\n";
    str += "[alphabetizedSIString," + getAlphabetizedSIString() + "]\n";
    str += "-------------------------\n";
    
    return str;
  }
  
  public boolean isDropshipInHouse() {
    // System.out.println("nmcommerceitemtempholder isDropshipInHouse isDropship->"+isDropship()+", isInStock->"+isInStock()+".");
    
    return getDropshipInHouse();
  }
  
  // public void setDropshipInHouse(boolean in)
  // {
  // dropshipInHouse = in;
  // }
  public boolean getDropshipInHouse() {
    // System.out.println("nmcommerceitemtempholder getDropshipInHouse isDropship->"+isDropship()+", isInStock->"+isInStock()+".");
    if (isDropship()) {
      if (isInStock()) {
        return true;
      }
    }
    return false;
  }
  
  public boolean isDropship() {
    return dropship;
  }
  
  public void setDropship(boolean dropship) {
    this.dropship = dropship;
  }
  
  public boolean isInStock() {
    return inStock;
  }
  
  public void setInStock(boolean inStock) {
    this.inStock = inStock;
  }
  
  public boolean getParentheticalPromo() {
    return parentheticalPromo;
  }
  
  public boolean isParentheticalPromo() {
    return parentheticalPromo;
  }
  
  public void setParentheticalPromo(boolean parentheticalPromo) {
    this.parentheticalPromo = parentheticalPromo;
  }
  
  public String getSwatchHexColor() {
    return swatchHexColor;
  }
  
  public void setSwatchHexColor(String swatchHexColor) {
    this.swatchHexColor = swatchHexColor;
  }
  
  public String getSwatchMediaLargeId() {
    return swatchMediaLargeId;
  }
  
  public void setSwatchMediaLargeId(String swatchMediaLargeId) {
    this.swatchMediaLargeId = swatchMediaLargeId;
  }
  
  public String getSwatchMediaSmallId() {
    return swatchMediaSmallId;
  }
  
  public void setSwatchMediaSmallId(String swatchMediaSmallId) {
    this.swatchMediaSmallId = swatchMediaSmallId;
  }
  
  public String getProductHasSwatchMedia() {
    if ((null != getSwatchHexColor() && getSwatchHexColor().length() > 2) && (null != getSwatchMediaSmallId() && getSwatchMediaSmallId().length() > 2)
            && (null != getSwatchMediaLargeId() && getSwatchMediaLargeId().length() > 2)) {
      return "true";
    } else {
      return "false";
    }
  }
  
  public String getStoreNum() {
    return storeNum;
  }
  
  public void setStoreNum(String storeNum) {
    this.storeNum = storeNum;
  }
  
  public String getSelectedInterval() {
    return selectedInterval;
  }
  
  public void setSelectedInterval(String selectedInterval) {
    this.selectedInterval = selectedInterval;
  }
  
  public String getDynamicImageUrl() {
    return dynamicImageUrl;
  }
  
  public void setDynamicImageUrl(String dynamicImageUrl) {
    this.dynamicImageUrl = dynamicImageUrl;
  }
  
  /**
   * Gets the configuration key.
   * 
   * @return the configuration key
   */
  public String getConfigurationKey() {
    return configurationKey;
  }
  
  /**
   * Sets the configuration key.
   * 
   * @param configurationKey
   *          the new configuration key
   */
  public void setConfigurationKey(final String configurationKey) {
    this.configurationKey = configurationKey;
  }
  
  /**
   * Gets the option choices.
   * 
   * @return the option choices
   */
  public Map<String, String> getOptionChoices() {
    return optionChoices;
  }
  
  /**
   * Sets the option choices.
   * 
   * @param optionChoices
   *          the option choices
   */
  public void setOptionChoices(Map<String, String> optionChoices) {
    this.optionChoices = optionChoices;
  }
  
  /** 
  * @return the specialInstructionFlag
   */
  public String getSpecialInstructionFlag() {
    return specialInstructionFlag;
  }
  
  /**
   * @param specialInstructionFlag
   *          the specialInstructionFlag to set
   */
  public void setSpecialInstructionFlag(String specialInstructionFlag) {
    this.specialInstructionFlag = specialInstructionFlag;
  }
  
  /**
   * @return the specialInstructionPrice
   */
  public Double getSpecialInstructionPrice() {
    return specialInstructionPrice;
  }
  
  /**
   * @param specialInstructionPrice
   *          the specialInstructionPrice to set
   */
  public void setSpecialInstructionPrice(Double specialInstructionPrice) {
    this.specialInstructionPrice = specialInstructionPrice;
  }

  public double getConfigSetPrice() {
    return configSetPrice;
  }

  public void setConfigSetPrice(double configSetPrice) {
    this.configSetPrice = configSetPrice;
  }

}
