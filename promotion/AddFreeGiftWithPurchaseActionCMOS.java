package com.nm.commerce.promotion;

import java.util.List;
import java.util.Map;

import atg.commerce.CommerceException;
import atg.commerce.order.CommerceItem;
import atg.commerce.order.ItemAddedToOrder;
import atg.commerce.order.ItemRemovedFromOrder;
import atg.commerce.order.Order;
import atg.commerce.order.OrderManager;
import atg.commerce.order.ShippingGroup;
import atg.commerce.promotion.AddItemToOrder;
import atg.commerce.promotion.PromotionConstants;
import atg.nucleus.Nucleus;
import atg.nucleus.logging.ApplicationLogging;
import atg.nucleus.logging.ClassLoggingFactory;
import atg.nucleus.naming.ComponentName;
import atg.process.ProcessException;
import atg.process.ProcessExecutionContext;
import atg.repository.MutableRepositoryItem;
import atg.repository.Repository;
import atg.repository.RepositoryException;
import atg.repository.RepositoryItem;
import atg.repository.RepositoryView;
import atg.repository.rql.RqlStatement;
import atg.scenario.ScenarioException;

import com.nm.commerce.NMCommerceItem;
import com.nm.commerce.order.NMOrderManager;
import com.nm.utils.SystemSpecs;

/**
 * AddFreeGiftWithPurchaseActionCMOS
 * 
 * This custom action was developed to allow scenarios to be deployed across environments. It utilizes String variables passed from scenario and then creates a sku and product Repository Item which
 * will be added to cart. All of the variables are required to be passed in if they are blank an exception will be generated.
 * 
 * @author Todd Schultz
 * @since 03/27/2002
 */

public class AddFreeGiftWithPurchaseActionCMOS extends AddItemToOrder {
  
  public static final String PROMO_TYPE_PARAM = "promo_type";
  public static final String PROMO_NAME_PARAM = "promo_name";
  public static final String CMOSSKU_PARAM = "cmosSKU";
  public static final String ITEM_CODE_PARAM = "item_code";
  public static final String PROMO_STR = "Promotion";
  public static final String CHECK_DISPLAY_FLAG = "check_display_flag";
  
  private ApplicationLogging mLogging = ClassLoggingFactory.getFactory().getLoggerForClass(AddFreeGiftWithPurchaseActionCMOS.class);
  
  public void initialize(@SuppressWarnings("rawtypes") Map pParameters) throws ProcessException {
    /** Resolve OrderManager and Promotion components. */
    
    mOrderManager = OrderManager.getOrderManager();
    
    if (mOrderManager == null) throw new ScenarioException(PromotionConstants.getStringResource(PromotionConstants.ORDER_MANAGER_NOT_FOUND));
    
    mOrderHolderComponent = ComponentName.getComponentName("/atg/commerce/ShoppingCart");
    storeOptionalParameter(pParameters, PROMO_TYPE_PARAM, java.lang.String.class);
    storeOptionalParameter(pParameters, PROMO_NAME_PARAM, java.lang.String.class);
    storeOptionalParameter(pParameters, CMOSSKU_PARAM, java.lang.String.class);
    storeOptionalParameter(pParameters, ITEM_CODE_PARAM, java.lang.String.class);
    storeOptionalParameter(pParameters, CHECK_DISPLAY_FLAG, java.lang.Boolean.class);
    
  }
  
  /*****************************************************************************************
   * This method is called from scenario, note no params are accepted but they are still required to be passed by scenario. Once validation is performed that all values needed are populated RQL
   * queries are perfored to create Repository Item
   * 
   * @param ScenarioExecutionContext
   *          pContext
   * @return void
   * @exception ScenarioException
   *****************************************************************************************/
  protected void executeAction(ProcessExecutionContext pContext) throws ProcessException {
    // System.out.println("###################################AddFreeGiftWithPurchaseActionCMOS Called");
    
    String promoType = (String) getParameterValue(PROMO_TYPE_PARAM, pContext);
    String promoName = (String) getParameterValue(PROMO_NAME_PARAM, pContext);
    String itemCode = (String) getParameterValue(ITEM_CODE_PARAM, pContext);
    String catalogCode = getCurrentCatalogCode();
    String cmosSku = (String) getParameterValue(CMOSSKU_PARAM, pContext);
    Boolean checkDisplayFlag = (Boolean) getParameterValue(CHECK_DISPLAY_FLAG, pContext); // set in scenario file
    
    if ((promoType == null) || (promoType.trim().equals("")) || (promoName == null) || (promoName.trim().equals("")) || (itemCode == null) || (itemCode.trim().equals("")) || (catalogCode == null)
            || (catalogCode.trim().equals("")) || (cmosSku == null) || (cmosSku.trim().equals("")) || (checkDisplayFlag == null) || (checkDisplayFlag.toString().trim().equals(""))) {
    	if (mLogging.isLoggingDebug()) {
    		mLogging.logDebug("AddFreeGiftWithPurchaseActionCMOS was called with an unpopulated value check scenario ");
    	}
      throw new ScenarioException("AddFreeGiftWithPurchaseActionCMOS was called with an unpopulated value check scenario ");
    } else {
      if (mOrderManager == null) {
        throw new ScenarioException(PromotionConstants.getStringResource(PromotionConstants.ORDER_MANAGER_NOT_FOUND));
      }
      
      // Prepare to remove
      Object message = pContext.getMessage();
      // System.out.println("########## AddFreeGiftWithPurchaseActionCMOS message-type[" + message.getClass().getName() + "] cmosSku[" + cmosSku + "]");
      boolean hasRemoveMessage = message instanceof ItemRemovedFromOrder;
      boolean hasAddMessage = message instanceof ItemAddedToOrder;
      boolean hasPromoMessage = message instanceof com.nm.scenario.PromoCodeMessage;
      long ciQuantity = 1L;
      if (hasRemoveMessage) {
        ItemRemovedFromOrder iOrder = (ItemRemovedFromOrder) pContext.getMessage();
        ciQuantity = iOrder.getQuantity();
      }
      
      if (hasAddMessage || hasPromoMessage || hasRemoveMessage) {
        // Get the promo type from scenario parameter
        RepositoryItem prodItem = null;
        RepositoryItem skuItem = null;
        
        // System.out.println("***AFGWPCMOS***promoType " + promoType);
        // System.out.println("***AFGWPCMOS***itemCode " + itemCode);
        // System.out.println("***AFGWPCMOS***catalogCode " + catalogCode);
        // System.out.println("***AFGWPCMOS***cmosSku " + cmosSku);
        // System.out.println("***AFGWPCMOS***checkDisplayFlag " + checkDisplayFlag);
        // System.out.println("***AFGWPCMOS***inventoryFlag " + inventoryFlag);
        
        try {
          Repository repository = (Repository) Nucleus.getGlobalNucleus().resolveName("/atg/commerce/catalog/ProductCatalog");
          RepositoryView m_view = repository.getView("sku");
          
          RqlStatement statement = RqlStatement.parseRqlStatement("cmosSKU = ?0");
          Object params[] = new Object[1];
          params[0] = cmosSku.trim();
          // System.out.println("***AFGWPCMOS***params " + params[0]);
          
          RepositoryItem skuArray[] = statement.executeQuery(m_view, params);
          // System.out.println("***AFGWPCMOS*** 1.skuArray repos
          // catalogCode " + catalogCode);
          if (skuArray == null) {
            throw new NullPointerException("skuArray is null for [SELECT * FROM " + "NM_SKU_AUX" + " WHERE cmos_sku='" + params[0] + "'"
                    + "\n Item with this cmos_sku number does not exist in the database. ");
          } else {
            if (skuArray[0] != null)
              skuItem = (RepositoryItem) skuArray[0];
            else
              throw new NullPointerException("skuArray[0] is null");
          }
          
          RepositoryView m_view2 = repository.getView("product");
          // System.out.println("***AFGWPCMOS*** 2.skuArray m_view2 catalogCode " + catalogCode);
          
          RqlStatement statement2 = RqlStatement.parseRqlStatement("cmosCatalogId = ?0 AND cmosItemCode = ?1");
          Object params2[] = new Object[2];
          params2[0] = catalogCode.trim();
          params2[1] = itemCode.trim();
          
          RepositoryItem productArray[] = statement2.executeQuery(m_view2, params2);
          // System.out.println("***AFGWPCMOS*** 3.productArray catalogCode " + catalogCode);
          if (productArray == null) {
            throw new NullPointerException("productArray is null for [SELECT * FROM " + "NM_PRODUCT_AUX" + " WHERE cmos_catalog_id='" + params2[0] + "' AND cmos_item='" + params2[1] + "']");
          } else {
            if (productArray[0] != null)
              prodItem = (RepositoryItem) productArray[0];
            else
              throw new NullPointerException("product[0] is null");
          }
          
        } catch (Exception e) {
        	if (mLogging.isLoggingError()) {
        		mLogging.logError("Exception caught getting Repository Items in AddFreeGiftWithPurchaseActionCMOS  ");
        	}
        }
        
        if (skuItem != null && prodItem != null) {
          
          /*
           * if checkDisplayFlag is set to true in the scenario, we will check the displayflag on the product if the product is displayable, we add it to the cart, regardless of stock if
           * checkDisplayFlag is false, we will always add it to the cart.
           */
          Boolean prodDisplayable = (Boolean) prodItem.getPropertyValue("flgDisplay");
          
          // set in scenario
          if (checkDisplayFlag.toString().trim().equals("true")) {
            // products display flag
            prodDisplayable = (Boolean) prodItem.getPropertyValue("flgDisplay");
          } else {
            // set to true if we dont want to check displayable. we will default to 'true'
            prodDisplayable = Boolean.valueOf("true");
          }
          // System.out.println("****AFGWPACMOS prodDisplayable is:"+prodDisplayable);
          
          if (prodDisplayable.booleanValue()) {
            String productID, skuID;
            
            skuID = skuItem.getRepositoryId();
            productID = prodItem.getRepositoryId();
            // Add the free gift item to the cart
            try {
              addItem(skuID, productID, ciQuantity, promoType, promoName, pContext);
            } catch (CommerceException ce) {
              throw new ScenarioException(ce);
            } catch (RepositoryException re) {
              throw new ScenarioException(re);
            }
          } else {
            // end if prodDisplayable
          }
        } // if RI not null
      } // is itemAddedToOrder
    } // else variables are populated
  } // end of executeAction method
  
  public String getCurrentCatalogCode() {
    String catcode = getSystemSpecs().getCatalogCode();
    // System.out.println("***GettingCurrentCatCode***" + catcode);
    return catcode;
  }
  
  private SystemSpecs getSystemSpecs() {
    return (SystemSpecs) Nucleus.getGlobalNucleus().resolveName("/nm/utils/SystemSpecs");
  }
  
  /**
   * This method will add item to order based on NM's business requirement, adding each item as a separate line item. Additional NM's required commerce item information is added in this method.
   * 
   * <P>
   * 
   * If different behavior is desired when an item is being added to the order, this method should be overriden.
   * 
   * @param pSkuId
   *          the sku id of the commerce item that will be created and added to the order
   * @param pProductId
   *          product id of the commerce item that will be created and added
   * @param pQuantity
   *          the quantity of the particular item to add
   * @param pContext
   *          the context in which the action is occuring
   * @exception CommerceException
   *              if an error occurs
   * @exception RepositoryException
   *              if an error occurs
   */
  protected void addItem(String pSkuId, String pProductId, long pQuantity, String promoType, String promoName, ProcessExecutionContext pContext) throws CommerceException, RepositoryException {
    Order o = getOrderToAddItemTo(pContext);
    
    // System.out.println(o.getId());
    
    if (o != null) {
      @SuppressWarnings("unchecked")
      List<ShippingGroup> sgs = o.getShippingGroups();
      if (sgs != null && sgs.size() > 0) {
        ShippingGroup sg = (ShippingGroup) o.getShippingGroups().get(0);
        
        Long NMquantity = new Long(pQuantity);
        int ciQuantity = NMquantity.intValue();
        
        for (int ciNo = 0; ciNo < ciQuantity; ciNo++) {
          if (mOrderManager instanceof NMOrderManager) {
            NMOrderManager om = (NMOrderManager) mOrderManager;
            CommerceItem ci = om.addSeparateItemToShippingGroup(o, pSkuId, pProductId, Long.parseLong("1"), sg);
            if (ci instanceof NMCommerceItem) {
              // Populate data for extended commerce item properties
              
              NMCommerceItem NMci = (NMCommerceItem) ci;
              RepositoryItem prodItem = (RepositoryItem) NMci.getAuxiliaryData().getProductRef();
              RepositoryItem skuItem = (RepositoryItem) NMci.getAuxiliaryData().getCatalogRef();
              
              NMci.setCmosCatalogId((String) prodItem.getPropertyValue("cmosCatalogId"));
              NMci.setCmosItemCode((String) prodItem.getPropertyValue("cmosItemCode"));
              NMci.setCmosProdName((String) prodItem.getPropertyValue("displayName"));
              NMci.setCmosSKUId((String) skuItem.getPropertyValue("cmosSKU"));
              NMci.setDeptCode((String) prodItem.getPropertyValue("deptCode"));
              NMci.setSourceCode((String) prodItem.getPropertyValue("sourceCode"));
              NMci.setWebDesignerName((String) prodItem.getPropertyValue("cmDesignerName"));
              
              String variation1 = null;
              String variation2 = null;
              String variation3 = null;
              try {
                @SuppressWarnings("unchecked")
                Map<String, MutableRepositoryItem> tmpMapVariation = (Map<String, MutableRepositoryItem>) skuItem.getPropertyValue("skuProdInfo");
                RepositoryItem ri = (RepositoryItem) tmpMapVariation.get(prodItem.getRepositoryId());
                variation1 = (String) ri.getPropertyValue("cmVariation1");
                variation2 = (String) ri.getPropertyValue("cmVariation2");
                variation3 = (String) ri.getPropertyValue("cmVariation3");
              } catch (Exception e) {
            	  if (mLogging.isLoggingError()) {
            		  String item =  prodItem.getRepositoryId();
            		  mLogging.logError("An exception occurred in AddFreeGiftWithPurchaseActionCMOS:addItem>" + e);
            		  mLogging.logError("Variations will not be set for product:" + item);
            	  }
              }
              if (variation1 == null) {
                NMci.setProdVariation1("");
              } else {
                NMci.setProdVariation1(variation1);
              }
              
              if (variation2 == null) {
                NMci.setProdVariation2("");
              } else {
                NMci.setProdVariation2(variation2);
              }
              
              if (variation3 == null) {
                NMci.setProdVariation3("");
              } else {
                NMci.setProdVariation3(variation3);
              }
              
              NMci.setQuickOrder(false);
              NMci.setPerishable(false);
              
              // This is hardcoded for the assumption that the item added is an promotional item
              NMci.setTransientStatus(PROMO_STR);
              NMci.setPromoType(promoType);
              NMci.setPromoName(promoName);
              NMci.setCoreMetricsCategory("gwp");
              
            }
          }
        }
      }
    }
  }
}
