package com.nm.commerce.catalog.helper;

import static com.nm.monogram.utils.MonogramConstants.FREE_MONOGRAM_FLAG;
import static com.nm.monogram.utils.MonogramConstants.OPTIONAL_MONOGRAM_FLAG;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.codehaus.jackson.annotate.JsonAutoDetect.Visibility;
import org.codehaus.jackson.annotate.JsonMethod;
import org.codehaus.jackson.map.ObjectMapper;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import atg.core.util.StringUtils;
import atg.repository.RepositoryItem;

import com.nm.commerce.catalog.NMProduct;
import com.nm.commerce.catalog.NMSku;
import com.nm.components.CommonComponentHelper;
import com.nm.utils.BrandSpecs;
import com.nm.utils.InventoryUtil;
import com.nm.utils.NmoUtils;
import com.nm.utils.ProdSkuUtil;
import com.nm.utils.SkuColorData;
import com.nm.utils.StringUtilities;

public class ProductMatrixJSHelper {
  
  public static String getMatrixJavaScript(final boolean textOnly, final NMProduct nmProduct) {
    final StringBuffer jsString = new StringBuffer(200);
    int var1Count;
    int var2Count;
    final String matrixName = nmProduct.getId() + "Matrix";
    if (nmProduct.getIsPriorityCodeVariation1()) {
      var1Count = nmProduct.getSellableVariation1Count();
      var2Count = nmProduct.getSellableVariation2Count();
    } else {
      var1Count = nmProduct.getSellableVariation2Count();
      var2Count = nmProduct.getSellableVariation1Count();
    }
    
    jsString.append("\n");
    jsString.append("<SCRIPT LANGUAGE='JavaScript'>");
    jsString.append("\n");
    // create the blank arrays
    if (var1Count > 0) {
      jsString.append(matrixName + " = new Array();");
      jsString.append("\n");
      for (int i = 0; i < var1Count; i++) {
        jsString.append(matrixName + "[" + i + "] = " + "new Array();");
        jsString.append("\n");
      }
    } else if (var2Count > 0) {
      jsString.append(matrixName + " = new Array();");
      jsString.append("\n");
      for (int i = 0; i < var2Count; i++) {
        jsString.append(matrixName + "[" + i + "] = " + "new Array();");
        jsString.append("\n");
      }
    } else {
      jsString.append(matrixName + " = new Array();");
      jsString.append("\n");
      jsString.append(matrixName + "[0] = new Array();");
      jsString.append("\n");
    }
    
    int invThreshold = 0;
    
    if (isInventoryStatusShowable(nmProduct)) {
      final InventoryUtil util = InventoryUtil.getInstance();
      invThreshold = util.getMaxInventoryThreshold(nmProduct.getId());
    }
    
    // sort the sellableSkuList by Size
    if (nmProduct.getIsPriorityCodeVariation1()) {
      Collections.sort(nmProduct.getSellableSkuList(), NMSku.VARIATIONCODE1_ASC_COMPARATOR);
    } else {
      Collections.sort(nmProduct.getSellableSkuList(), NMSku.VARIATIONCODE2_ASC_COMPARATOR);
    }
    // start the process of outputing the sku information into the matrix
    final Iterator<NMSku> sellableSkuListIterator = nmProduct.getSellableSkuList().iterator();
    String lastVariation1 = null;
    int sellableVariation2Counter = 0;
    int sellableVariation1Counter = 0;
    while (sellableSkuListIterator.hasNext()) {
      final NMSku nmSku = sellableSkuListIterator.next();
      String codeVariation1 = nmSku.getCodeVariation1().trim();
      
      // if type 1 or 2 we need to determine if variation1 in the matrix
      // should be color or size
      if (nmProduct.getVariationType() == NMProduct.QTY_BOX_COLOR) {
        codeVariation1 = nmSku.getCodeVariation1().trim();
      } else if (nmProduct.getVariationType() == NMProduct.QTY_BOX_SIZE) {
        codeVariation1 = nmSku.getCodeVariation2().trim();
      } else {
        // check if first variation is going to be color or size
        if (nmProduct.getIsPriorityCodeVariation1()) {
          codeVariation1 = nmSku.getCodeVariation1().trim();
        } else {
          codeVariation1 = nmSku.getCodeVariation2().trim();
        }
      }
      
      if (lastVariation1 != null) {
        if (lastVariation1.equals(codeVariation1)) {
          sellableVariation2Counter++;
        } else {
          sellableVariation1Counter++;
          sellableVariation2Counter = 0;
        }
      }
      
      jsString.append(matrixName + "[" + sellableVariation1Counter + "][" + sellableVariation2Counter + "] = ");
      jsString.append(getProductObjectValues(nmSku, textOnly, nmProduct, invThreshold));
      
      // for vendorRestrictedDates
      if (nmProduct.getHasVendorRestrictedDates()) {
        final SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyyMMdd");// yyyyMMdd
        String dateString;
        jsString.append("\n");
        
        for (int i = 0; i < nmProduct.getVendorRestrictedDateList().size(); i++) {
          jsString.append(matrixName + "[" + sellableVariation1Counter + "][" + sellableVariation2Counter + "].vendorRestrictedDates[" + i + "] = ");
          dateString = dateFormatter.format(nmProduct.getVendorRestrictedDateList().get(i));
          jsString.append("'" + dateString + "';");
          jsString.append("\n");
        }
      }
      
      jsString.append("\n");
      lastVariation1 = codeVariation1;
      
      jsString.append("\n");
      final String storeFulFillFlag = nmSku.getIsStoreFulfillFlag() + "";
      jsString.append(matrixName + "[" + sellableVariation1Counter + "][" + sellableVariation2Counter + "].storeFulfillStatus = ");
      jsString.append("'" + storeFulFillFlag + "';");
      jsString.append("\n");
      jsString.append(matrixName + "[" + sellableVariation1Counter + "][" + sellableVariation2Counter + "].colorswatch = ");
      String cmVariation1 = "";
      try {
        cmVariation1 = constructSkuColorData(nmSku, nmProduct);
        cmVariation1 = NmoUtils.searchAndReplace("'", "\\'", cmVariation1);
      } catch (final Exception e) {
        e.printStackTrace();
      }
      jsString.append("'" + cmVariation1 + "';");
      jsString.append("\n");
      
      if (nmProduct.getSuggReplenishmentInterval() != null) {
        jsString.append("\n");
        final String suggestedInterval = nmSku.getSuggReplenishmentInterval() + "";
        jsString.append(matrixName + "[" + sellableVariation1Counter + "][" + sellableVariation2Counter + "].suggestedInterval = ");
        jsString.append("'" + suggestedInterval + "';");
        jsString.append("\n");
      }
      
      //vendor quantity restriction start
      if (!StringUtils.isBlank(nmProduct.getCmosItemCode())) {
          jsString.append("\n");
          jsString.append(matrixName + "[" + sellableVariation1Counter + "][" + sellableVariation2Counter + "].cmosItemCode = ");
          jsString.append("'" + nmProduct.getCmosItemCode() + "';");
          jsString.append("\n");
	     }
		final BrandSpecs brandSpecs = CommonComponentHelper.getBrandSpecs();
		if (brandSpecs.isEnableDepartmentQuantityRestrictions() && !StringUtils.isBlank(nmProduct.getDepartment())) {
			RepositoryItem vendorQuantityData = nmProduct.getProdSkuUtil().getDeptQuantityData(nmProduct.getDepartment());
		    if(vendorQuantityData != null && (Integer) vendorQuantityData.getPropertyValue("quantity") != null){
			int vendorquantity = (Integer) vendorQuantityData.getPropertyValue("quantity");
			if(vendorquantity > 0 ){
				  jsString.append("\n");
		          jsString.append(matrixName + "[" + sellableVariation1Counter + "][" + sellableVariation2Counter + "].vendorQuantity = ");
		          jsString.append("'" + vendorquantity + "';");
		          jsString.append("\n");
		          
		          jsString.append("\n");
		          jsString.append(matrixName + "[" + sellableVariation1Counter + "][" + sellableVariation2Counter + "].departmentRestrictionEnabled = ");
		          jsString.append("'" + true + "';");
		          jsString.append("\n");
			}
		}
    }
      //vendor quantity restriction end
      
    }
    
    jsString.append("\n");
    jsString.append("</SCRIPT>\n");
    jsString.append("\n");
    jsString.append("\n");
    jsString.append("\n");
    
    return jsString.toString();
  }
  
  /**
   * Name of Method: getProductObjectValues
   * 
   * @param NMSku
   *          nmSku
   * @return String
   * 
   *         this method populates a line in the matrix for a particular sku its values have a one to one relationship to values in the jsProductObjectProperties array
   */
  private static String getProductObjectValues(final NMSku nmSku, final boolean textOnly, final NMProduct nmProduct, final int invThreshold) {
    final StringBuffer valueString = new StringBuffer(50);
    valueString.append("new product(");
    valueString.append("'" + nmProduct.getProductNumber() + "'");
    valueString.append(",");
    valueString.append("'" + nmProduct.getId() + "'");
    valueString.append(",");
    valueString.append("'" + nmSku.getId() + "'");
    valueString.append(",");
    
    // if type 1 or 2 we need to determine if variation1 in the matrix
    // should be color or size
    String cmVariation1 = nmProduct.getProdSkuUtil().getProdSkuVariation(nmProduct.getDataSource(), nmSku.getDataSource(), "cmVariation1");
    
    String cmVariation2 = nmProduct.getProdSkuUtil().getProdSkuVariation(nmProduct.getDataSource(), nmSku.getDataSource(), "cmVariation2");
    
    if (cmVariation1 != null) {
      cmVariation1 = NmoUtils.searchAndReplace("'", "\\'", cmVariation1);
    }
    
    if (cmVariation2 != null) {
      cmVariation2 = NmoUtils.searchAndReplace("'", "\\'", cmVariation2);
    }
    
    if (nmProduct.getVariationType() == NMProduct.QTY_BOX_COLOR) {
      valueString.append("'" + cmVariation1 + "'");
      valueString.append(",");
      valueString.append("'" + cmVariation2 + "'");
    } else if (nmProduct.getVariationType() == NMProduct.QTY_BOX_SIZE) {
      valueString.append("'" + cmVariation2 + "'");
      valueString.append(",");
      valueString.append("'" + cmVariation1 + "'");
    } else {
      // check if first variation is going to be color or size
      if (nmProduct.getIsPriorityCodeVariation1()) {
        valueString.append("'" + cmVariation1 + "'");
        valueString.append(",");
        valueString.append("'" + cmVariation2 + "'");
      } else {
        valueString.append("'" + cmVariation2 + "'");
        valueString.append(",");
        valueString.append("'" + cmVariation1 + "'");
      }
    }
    
    final String displayName = nmProduct.getDisplayName() != null ? NmoUtils.searchAndReplace("'", "\\'", nmProduct.getDisplayName().trim()) : "";
    
    valueString.append(",");
    valueString.append("'" + displayName + "'");
    valueString.append(",");
    valueString.append(nmSku.getIsPerishable());
    valueString.append(",");
    valueString.append(nmSku.getDeliveryDays());
    // valueString.append(getDeliveryDays());
    valueString.append(",");
    if (nmProduct.getIsStatusShowable()) {
      valueString.append("'" + nmProduct.getProdSkuUtil().getAvailableDateString(nmProduct.getDataSource(), nmSku.getDataSource(), "") + "'");
    } else {
      valueString.append("''");
    }
    
    boolean isOptionalMonogramProduct = nmProduct.getSpecialInstructionFlag().equalsIgnoreCase(OPTIONAL_MONOGRAM_FLAG) || nmProduct.getSpecialInstructionFlag().equalsIgnoreCase(FREE_MONOGRAM_FLAG);
    valueString.append(",");
    if (nmProduct.getIsStatusShowable()) {
      valueString.append("'" + nmProduct.getProdSkuUtil().getAvailableDateString(nmProduct.getDataSource(), nmSku.getDataSource(), "", isOptionalMonogramProduct) + "'");
    } else {
      valueString.append("''");
    }

    valueString.append(",");
    if (nmSku.getIsDropShip()) {
      valueString.append("999,false,0");
    } else {
		valueString.append(nmProduct.getProdSkuUtil().getStockLevel(nmProduct.getDataSource(), nmSku.getDataSource()));
		/* Checking backOrderMessage is eligible or not*/
		
		final BrandSpecs brandSpecs = CommonComponentHelper.getBrandSpecs();
  		if(brandSpecs.isEnableOrderStatusChange()){
  			valueString.append(","+nmProduct.getProdSkuUtil().isBackOrderMessageEnabled(nmProduct.getDataSource(), nmSku.getDataSource()));
  			valueString.append(","+ new Integer(nmSku.getStockLevel()).intValue());
        }else{
        	 valueString.append(",false,0");
        }
    }
    
    valueString.append(",");
    valueString.append("'" + nmProduct.getVariationType() + "'");
    valueString.append(",");
    // added try/catch to account for changes to
    // getProdSkuUtil().getDiscontinuedCode
    try {
      if (nmProduct.getIsStatusShowable()) {
        final int statusType = textOnly ? 1 : 2;
        valueString.append("'"
                + nmProduct.getProdSkuUtil().getStatusDisplay(statusType, nmSku.getIsDropShip(), nmSku.getInStock(), nmProduct.getIsExclusive(),
                        nmProduct.getProdSkuUtil().isPreOrder(nmProduct.getDataSource()), nmProduct.getProdSkuUtil().getDiscontinuedCode(nmProduct.getDataSource(), nmSku.getDataSource()),
                        nmSku.getIsOnOrder(), nmProduct.getProdSkuUtil().isMonogramItem(nmProduct.getDataSource()), nmProduct.getProdSkuUtil().isVirtualGiftCard(nmProduct.getDataSource())) + "'");
        valueString.append(",");
        
        final String stockStatusInText =
                nmProduct.getProdSkuUtil().getStatusDisplay(1, nmSku.getIsDropShip(), nmSku.getInStock(), nmProduct.getIsExclusive(), nmProduct.getProdSkuUtil().isPreOrder(nmProduct.getDataSource()),
                        nmProduct.getProdSkuUtil().getDiscontinuedCode(nmProduct.getDataSource(), nmSku.getDataSource()), nmSku.getIsOnOrder(),
                        nmProduct.getProdSkuUtil().isMonogramItem(nmProduct.getDataSource()), nmProduct.getProdSkuUtil().isVirtualGiftCard(nmProduct.getDataSource()));
        String onlyXLeftMessage = "";
        /*Suppress Only X left messaging for discontinued flag N and non LC brands*/
        final BrandSpecs brandSpecs = CommonComponentHelper.getBrandSpecs();
        if ( brandSpecs.isEnableOrderStatusChange() && !nmProduct.getProdSkuUtil().getDiscontinuedCode(nmProduct.getDataSource(), nmSku.getDataSource()).equalsIgnoreCase("N")
            	&& nmProduct.getProdSkuUtil().getStatusInStockString().equalsIgnoreCase(stockStatusInText) ) {
          
          final int skuStock = nmProduct.getProdSkuUtil().getAvailableQty(nmSku);
          if ((invThreshold > 0) && (skuStock > 0) && (skuStock <= invThreshold)) {
            onlyXLeftMessage = "Only " + skuStock + " Left";
          }
        }
        valueString.append("'" + stockStatusInText + "'");
        valueString.append(",");
        valueString.append("'" + onlyXLeftMessage + "'");
      } else {
        valueString.append(textOnly ? "" : "'/common/images/shim.gif'");
        valueString.append(",");
        valueString.append("");
        valueString.append(",");
        valueString.append("");
      }
    } catch (final Exception e) {
      valueString.append(textOnly ? "" : "'/common/images/shim.gif'");
      valueString.append(",");
      valueString.append("");
      valueString.append(",");
      valueString.append("");
    }
    
    valueString.append(",");
    valueString.append("new Array()");
    valueString.append(",");
    valueString.append("'" + nmProduct.getParentSuiteId() + "'"); // suiteId
    valueString.append(",");
    valueString.append("" + "'" + nmSku.getIsStoreFulfillFlag() + "'");
    valueString.append(",");
    valueString.append(nmProduct.getProdSkuUtil().getMaxPurchaseQty(nmProduct.getDataSource())); // max purchase quantity
    valueString.append(",");
    valueString.append("" + "'" + nmSku.getSuggReplenishmentInterval() + "'");
    valueString.append(");");
    
    return valueString.toString();
  }
  
  private static String constructSkuColorData(final NMSku nmSku, final NMProduct nmProduct) throws Exception {
    final SkuColorData skuColorData = nmProduct.getProdSkuUtil().constructSkuColorData(nmSku, nmProduct);
    final ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.setVisibility(JsonMethod.FIELD, Visibility.ANY);
    return objectMapper.writeValueAsString(skuColorData);
  }
  
  @SuppressWarnings("unchecked")
  public static Map<String, Object> getProductsAsJSON(final ArrayList<String> productIds) throws Exception {
    final Map<String, Object> productDetailsMap = new HashMap<String, Object>();
    final List<NMProduct> nmProductsList = new ArrayList<NMProduct>();
    final List<Map<String, List<NMSku>>> defaultSelectedProductsMapList = new ArrayList<Map<String, List<NMSku>>>();
    final List<List<NMSku>> defaultSelectedSkusList = new ArrayList<List<NMSku>>();
    final JSONArray jsonProducts = new JSONArray();
    
    for (final String productId : productIds) {
      // we have seen strange cases where the productIds are huge..Looks like someone trying to use our gateways so
      // we're going to log details about the request when we see long productIds
      if (productId.length() > 40) {
        throw new Exception("Invalid productId: " + productId);
      } else {
        final Map<String, Object> jsonProduct = getProductSkusAsJSON(productId, productDetailsMap, nmProductsList);
        jsonProducts.add(jsonProduct.get("jsonProduct"));
        productDetailsMap.put("jsonProducts", jsonProducts);
        defaultSelectedProductsMapList.add((Map<String, List<NMSku>>) jsonProduct.get("defaultSelectedProductsMap"));
        productDetailsMap.put("defaultSelectedProductsMap", defaultSelectedProductsMapList);
        defaultSelectedSkusList.add((List<NMSku>) jsonProduct.get("defaultSelectedSkusList"));
        productDetailsMap.put("defaultSelectedSkusList", defaultSelectedSkusList);
      }
    }
    
    return productDetailsMap;
  }
  
  public static Map<String, Object> getProductSkusAsJSON(final String productId, final Map<String, Object> productDetailsMap, final List<NMProduct> nmProductsList) {
    final NMProduct nmProduct = new NMProduct(productId);
    nmProductsList.add(nmProduct);
    final List<NMSku> nmDefaultSelectedSkusList = new ArrayList<NMSku>();
    final Map<String, List<NMSku>> nmDefaultSelectedProductsMap = new HashMap<String, List<NMSku>>();
    
    if (nmProduct.getPropertyValue("childSKUs") == null) {
      return null;
    }
    
    if (nmProduct.getIsPriorityCodeVariation1()) {
      Collections.sort(nmProduct.getSellableSkuList(), NMSku.VARIATIONCODE1_ASC_COMPARATOR);
    } else {
      Collections.sort(nmProduct.getSellableSkuList(), NMSku.VARIATIONCODE2_ASC_COMPARATOR);
    }
    
    final JSONArray jsonProductSkus = getProductSkusAsJSON(nmProduct, nmDefaultSelectedSkusList);
    if (nmDefaultSelectedSkusList.size() > 0) {
      nmDefaultSelectedProductsMap.put(nmProduct.getId(), nmDefaultSelectedSkusList);
    }
    
    final JSONObject jsonProduct = new JSONObject();
    jsonProduct.put("productId", productId);
    jsonProduct.put("skus", jsonProductSkus);
    jsonProduct.put("variationType", nmProduct.getVariationType());
    jsonProduct.put("productName", nmProduct.getDisplayName());
    
    // for vendorRestrictedDates
    final JSONArray jsonVendorRestrictedDates = new JSONArray();
    if (nmProduct.getHasVendorRestrictedDates()) {
      final SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyyMMdd");// yyyyMMdd
      String dateString;
      
      for (int i = 0; i < nmProduct.getVendorRestrictedDateList().size(); i++) {
        dateString = dateFormatter.format(nmProduct.getVendorRestrictedDateList().get(i));
        jsonVendorRestrictedDates.add(dateString);
      }
    }
    
    jsonProduct.put("restrictedDates", jsonVendorRestrictedDates);
    productDetailsMap.put("jsonProduct", jsonProduct);
    productDetailsMap.put("nmProductsList", nmProductsList);
    productDetailsMap.put("defaultSelectedProductsMap", nmDefaultSelectedProductsMap);
    productDetailsMap.put("defaultSelectedSkusList", nmDefaultSelectedSkusList);
    return productDetailsMap;
  }
  
  public static JSONArray getProductSkusAsJSON(final NMProduct nmProduct, final List<NMSku> nmSkusList) {
    final JSONArray jsonProductSkus = new JSONArray();
    
    int invThreshold = 0;
    
    if (isInventoryStatusShowable(nmProduct)) {
      final InventoryUtil util = InventoryUtil.getInstance();
      invThreshold = util.getMaxInventoryThreshold(nmProduct.getId());
    }
    
    // start the process of outputing the sku information into the matrix
    final Iterator<NMSku> sellableSkuListIterator = nmProduct.getSellableSkuList().iterator();
    boolean foundDefaultSkuColor = false;
    while (sellableSkuListIterator.hasNext()) {
      final JSONObject jsonProductDetails = new JSONObject();
      
      final NMSku nmSku = sellableSkuListIterator.next();
      String codeVariation1 = nmSku.getCodeVariation1().trim();
      
      // if type 1 or 2 we need to determine if variation1 in the matrix
      // should be color or size
      if (nmProduct.getVariationType() == NMProduct.QTY_BOX_COLOR) {
        codeVariation1 = nmSku.getCodeVariation1().trim();
      } else if (nmProduct.getVariationType() == NMProduct.QTY_BOX_SIZE) {
        codeVariation1 = nmSku.getCodeVariation2().trim();
      } else {
        // check if first variation is going to be color or size
        if (nmProduct.getIsPriorityCodeVariation1()) {
          codeVariation1 = nmSku.getCodeVariation1().trim();
        } else {
          codeVariation1 = nmSku.getCodeVariation2().trim();
        }
      }
      
      jsonProductDetails.put("sku", nmSku.getId());
      
      // if type 1 or 2 we need to determine if variation1 in the matrix
      // should be color or size
      final String cmVariation1 = nmProduct.getProdSkuUtil().getProdSkuVariation(nmProduct.getDataSource(), nmSku.getDataSource(), "cmVariation1");
      String cmVariation2 = nmProduct.getProdSkuUtil().getProdSkuVariation(nmProduct.getDataSource(), nmSku.getDataSource(), "cmVariation2");
      
      // code commented as it was not working fine if cmVariation1 contains apostrophe.
      /*
       * if (cmVariation1 != null) { cmVariation1 = NmoUtils.searchAndReplace("'", "\\'", cmVariation1); }
       */
      
      if (cmVariation2 != null) {
        cmVariation2 = NmoUtils.searchAndReplace("'", "\\'", cmVariation2);
      }
      
      if (nmProduct.getVariationType() == NMProduct.QTY_BOX_COLOR) {
        populateColor(nmProduct, nmSku, cmVariation1, jsonProductDetails);
        
      } else if (nmProduct.getVariationType() == NMProduct.QTY_BOX_SIZE) {
        jsonProductDetails.put("size", cmVariation2);
      } else {
        // check if first variation is going to be color or size
        if (nmProduct.getIsPriorityCodeVariation1()) {
          populateColor(nmProduct, nmSku, cmVariation2, jsonProductDetails);
          jsonProductDetails.put("size", cmVariation1);
        } else {
          populateColor(nmProduct, nmSku, cmVariation1, jsonProductDetails);
          jsonProductDetails.put("size", cmVariation2);
        }
      }
      
      String status;
      try {
        status =
                nmProduct.getProdSkuUtil().getStatusDisplay(1, nmSku.getIsDropShip(), nmSku.getInStock(), nmProduct.getIsExclusive(), nmProduct.getProdSkuUtil().isPreOrder(nmProduct.getDataSource()),
                        nmProduct.getProdSkuUtil().getDiscontinuedCode(nmProduct.getDataSource(), nmSku.getDataSource()), nmSku.getIsOnOrder(),
                        nmProduct.getProdSkuUtil().isMonogramItem(nmProduct.getDataSource()), nmProduct.getProdSkuUtil().isVirtualGiftCard(nmProduct.getDataSource()));
        jsonProductDetails.put("status", status);
        String onlyXLeftMessage = "";
        /*Suppress Only X left messaging for discontinued flag N and non LC brands*/
        final BrandSpecs brandSpecs = CommonComponentHelper.getBrandSpecs();
        if (brandSpecs.isEnableOrderStatusChange() && !nmProduct.getProdSkuUtil().getDiscontinuedCode(nmProduct.getDataSource(), nmSku.getDataSource()).equalsIgnoreCase("N")
        		&& nmProduct.getProdSkuUtil().getStatusInStockString().equalsIgnoreCase(status)) {
          
          final int skuStock = nmProduct.getProdSkuUtil().getAvailableQty(nmSku);
          if ((invThreshold > 0) && (skuStock > 0) && (skuStock <= invThreshold)) {
            onlyXLeftMessage = "Only " + skuStock + " Left";
          }
        }
        
        jsonProductDetails.put("onlyXLeftMessage", onlyXLeftMessage);
        
      } catch (final Exception e) {
        // logError("Exception getting product status: " + e.getMessage());
      }
      
      final ProdSkuUtil prodSkuUtil = CommonComponentHelper.getProdSkuUtil();
      final boolean isDefaultSkuColor = prodSkuUtil.checkIfDefaultSkuColor(nmProduct, nmSku);
      if (isDefaultSkuColor) {
        foundDefaultSkuColor = true;
        nmSkusList.add(nmSku);
      }
      
      jsonProductDetails.put("storeFulfill", nmSku.getIsStoreFulfillFlag());
      jsonProductDetails.put("perishable", nmSku.getIsPerishable());
      jsonProductDetails.put("delivDays", nmSku.getDeliveryDays());
      jsonProductDetails.put("defaultSkuColor", isDefaultSkuColor);
      jsonProductDetails.put("cmosSku", nmSku.getCmosSku());
      
      if (nmProduct.getIsStatusShowable()) {
        jsonProductDetails.put("availDate", nmProduct.getProdSkuUtil().getAvailableDateString(nmProduct.getDataSource(), nmSku.getDataSource(), ""));
        jsonProductDetails.put("availPlainDate", nmProduct.getProdSkuUtil().getAvailableDateString(nmProduct.getDataSource(), nmSku.getDataSource(), "", true));
      }
      
      if (!nmSku.getIsDropShip()) {
        jsonProductDetails.put("stockLevel", nmProduct.getProdSkuUtil().getStockLevel(nmProduct.getDataSource(), nmSku.getDataSource()));
      }
      /* Checking backOrderMessage is eligible or not*/
      
      final BrandSpecs brandSpecs = CommonComponentHelper.getBrandSpecs();
      if(brandSpecs.isEnableOrderStatusChange()){
    	  jsonProductDetails.put("backOrderFlag", nmProduct.getProdSkuUtil().isBackOrderMessageEnabled(nmProduct.getDataSource(), nmSku.getDataSource()));
         jsonProductDetails.put("stockAvailable", new Integer(nmSku.getStockLevel()).intValue());
          String tempDate = nmProduct.getProdSkuUtil().getRealExpectedShipDate(nmProduct.getDataSource(), nmSku.getDataSource());
          if (StringUtils.isBlank(tempDate))
        	  tempDate = "Date Unavailable";
          jsonProductDetails.put("deliveryDate", "Expected to ship no later than "+tempDate);
      }else{
    	  jsonProductDetails.put("backOrderFlag", false);
    	  jsonProductDetails.put("stockAvailable", 0);
    	  jsonProductDetails.put("deliveryDate", "");
      }
      /* Checking backOrderMessage is eligible or not*/
      jsonProductDetails.put("suggestedInterval", nmSku.getSuggReplenishmentInterval());
      jsonProductDetails.put("maxPurchaseQty", nmProduct.getProdSkuUtil().getMaxPurchaseQty(nmProduct.getDataSource())); // max purchase quantity
      
      // vendor quantity restriction start
      if (!StringUtils.isBlank(nmProduct.getCmosItemCode())) {
        jsonProductDetails.put("cmosItemCode", nmProduct.getCmosItemCode());
      }
      if (brandSpecs.isEnableDepartmentQuantityRestrictions() && !StringUtils.isBlank(nmProduct.getDepartment())) {
        final RepositoryItem vendorQuantityData = nmProduct.getProdSkuUtil().getDeptQuantityData(nmProduct.getDepartment());
        if ((vendorQuantityData != null) && ((Integer) vendorQuantityData.getPropertyValue("quantity") != null)) {
          final int vendorquantity = (Integer) vendorQuantityData.getPropertyValue("quantity");
          if (vendorquantity > 0) {
            jsonProductDetails.put("departmentRestrictionEnabled", true);
            jsonProductDetails.put("vendorQuantity", vendorquantity);
          }
        }
      }
      // vendor quantity restriction end
      
      jsonProductSkus.add(jsonProductDetails);
    }
    if (nmProduct.getFlgDynamicImageColor() && !foundDefaultSkuColor && (jsonProductSkus.size() > 0)) {
      ((JSONObject) jsonProductSkus.get(0)).put("defaultSkuColor", true);
      nmSkusList.add(nmProduct.getSellableSkuList().get(0));
    }
    
    return jsonProductSkus;
  }
  
  private static void populateColor(final NMProduct nmProduct, final NMSku nmSku, final String cmColorVariation, final JSONObject jsonProductDetails) {
    if (StringUtilities.isNotEmpty(cmColorVariation)) {
      String hexCode = "";
      if (nmProduct.getHasMultiColorSellableSkuList()) {
        final List<RepositoryItem> multiColors = nmSku.getMultiColorList();
        final Iterator<RepositoryItem> multiColorsIterator = multiColors.iterator();
        while (multiColorsIterator.hasNext()) {
          final RepositoryItem ri = multiColorsIterator.next();
          hexCode = hexCode + (String) ri.getPropertyValue("hexColor");
          if (multiColorsIterator.hasNext()) {
            hexCode = hexCode + ";";
          }
        }
      } else {
        hexCode = nmSku.getHexCode();
      }
      final String variation1AndHex = cmColorVariation + "?" + hexCode + "?" + nmSku.getIsNewColor();
      jsonProductDetails.put("color", variation1AndHex);
    }
  }
  
  private static boolean isInventoryStatusShowable(final NMProduct nmProduct) {
    
    boolean showInventoryStatus = false;
    
    try {
      final Iterator<NMSku> sellableSkuListIterator = nmProduct.getSellableSkuList().iterator();
      
      while (sellableSkuListIterator.hasNext()) {
        
        final NMSku nmSku = sellableSkuListIterator.next();
        
        final String stockStatusInText =
                nmProduct.getProdSkuUtil().getStatusDisplay(1, nmSku.getIsDropShip(), nmSku.getInStock(), nmProduct.getIsExclusive(), nmProduct.getProdSkuUtil().isPreOrder(nmProduct.getDataSource()),
                        nmProduct.getProdSkuUtil().getDiscontinuedCode(nmProduct.getDataSource(), nmSku.getDataSource()), nmSku.getIsOnOrder(),
                        nmProduct.getProdSkuUtil().isMonogramItem(nmProduct.getDataSource()), nmProduct.getProdSkuUtil().isVirtualGiftCard(nmProduct.getDataSource()));
        
        if (nmProduct.getProdSkuUtil().getStatusInStockString().equalsIgnoreCase(stockStatusInText)) {
          
          showInventoryStatus = true;
          break;
        }
      }
    } catch (final Exception exception) {
      
    }
    
    return showInventoryStatus;
  }
  
}
