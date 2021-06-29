/*
 * Created on Feb 19, 2004
 * 
 * To change the template for this generated file go to Window>Preferences>Java>Code Generation>Code and Comments Designed to compute a total based on a products Catalog Code
 */

package com.nm.commerce.promotion;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.Vector;

import atg.commerce.CommerceException;
import atg.commerce.order.CommerceItem;
import atg.commerce.order.CommerceItemImpl;
import atg.commerce.order.CommerceItemManager;
import atg.commerce.order.CommerceItemRelationship;
import atg.commerce.order.CommerceItemRelationshipContainer;
import atg.commerce.order.Order;
import atg.commerce.order.OrderHolder;
import atg.commerce.order.OrderImpl;
import atg.commerce.order.OrderManager;
import atg.commerce.order.OrderQueries;
import atg.commerce.promotion.PromotionConstants;
import atg.commerce.states.OrderStates;
import atg.commerce.states.StateDefinitions;
import atg.nucleus.Nucleus;
import atg.nucleus.naming.ComponentName;
import atg.process.ProcessException;
import atg.process.ProcessExecutionContext;
import atg.process.action.ActionImpl;
import atg.repository.Repository;
import atg.repository.RepositoryException;
import atg.repository.RepositoryItem;
import atg.scenario.ScenarioException;
import atg.servlet.DynamoHttpServletRequest;

import com.nm.commerce.NMCommerceItem;

/**
 * @author nmmpe Created on Feb 19, 2004 This will calculate the total of designated Catalog Codes in a order for promotional purposes
 */

public class CatalogFilter extends ActionImpl {
  
  public static final String ORDERMANAGER_PATH = "/atg/commerce/order/OrderManager";
  public static final String ORDERQUERIES_PATH = "/atg/commerce/order/OrderQueries";
  public static final String VARIABLE_PARAM = "variable"; // totalPromotionalOrderVariable
  public static final String CMOSSKU_PARAM = "cmos_sku";
  public static final String CATALOG_CODE = "Catalog_Code";
  public static final String SHOPPING_CART_PATH = "/atg/commerce/ShoppingCart";
  
  protected ComponentName mOrderHolderComponent = null;
  protected OrderManager mOrderManager = null;
  protected OrderQueries mOrderQuery = null;
  
  protected CommerceItemRelationshipContainer mCommerceItemRelationshipContainer = null;
  protected CommerceItemManager mCommerceItemManager = null;
  
  protected String mTPOV = null;// The totalPromotionalOrderVariable set in the scenario
  protected String mCmosSku = null;// The CmosSKU set in the scenario
  protected String mCatalogCode = null;// The CatalogCode set in the scenario
  protected boolean itemMatch = false;
  protected boolean CatalogCodeflag = false;
  protected int orderState = 0;
  
  @Override
  public void initialize(@SuppressWarnings("rawtypes") Map pParameters) throws ProcessException {
    mOrderManager = (OrderManager) Nucleus.getGlobalNucleus().resolveName(ORDERMANAGER_PATH);
    mOrderQuery = (OrderQueries) Nucleus.getGlobalNucleus().resolveName(ORDERQUERIES_PATH);
    mCommerceItemManager = new CommerceItemManager();
    storeRequiredParameter(pParameters, VARIABLE_PARAM, java.lang.String.class);
    storeRequiredParameter(pParameters, CMOSSKU_PARAM, java.lang.String.class);// filters out GWP sku
    storeRequiredParameter(pParameters, CATALOG_CODE, java.lang.String.class);// "Catalog Code to be checked"
    
    mOrderHolderComponent = ComponentName.getComponentName(SHOPPING_CART_PATH);
    
  }
  
  @Override
  protected void executeAction(ProcessExecutionContext pContext) throws ProcessException {
    if (mOrderManager == null) throw new ProcessException(PromotionConstants.getStringResource(PromotionConstants.ORDER_MANAGER_NOT_FOUND));
    
    if (mTPOV == null) {
      synchronized (VARIABLE_PARAM) {
        mTPOV = (String) getParameterValue(VARIABLE_PARAM, pContext);
      }
    }
    if (mTPOV == null) throw new ProcessException("***CF***mTPOV was not specified");
    
    if (mCmosSku == null) {
      synchronized (CMOSSKU_PARAM) {
        mCmosSku = (String) getParameterValue(CMOSSKU_PARAM, pContext);
      }
    }
    if (mCmosSku == null) throw new ProcessException("***CF***mCmosSku was not specified");
    
    if (mCatalogCode == null) {
      synchronized (CATALOG_CODE) {
        mCatalogCode = (String) getParameterValue(CATALOG_CODE, pContext);
      }
    }
    if (mCatalogCode == null) throw new ProcessException("***CF***mCatalogCode was not specified");
    
    try {
      modifyOrder(mTPOV, mCmosSku, pContext);
    } catch (CommerceException ce) {
      throw new ScenarioException(ce);
    } catch (RepositoryException re) {
      throw new ScenarioException(re);
    }
    
  }
  
  protected void modifyOrder(String pTPOVariable, String pCMOSSKU, ProcessExecutionContext pContext) throws ProcessException, CommerceException, RepositoryException {
    Order order = getOrderToModify(pContext);
    Repository pRepository = (Repository) Nucleus.getGlobalNucleus().resolveName("/atg/commerce/catalog/ProductCatalog");
    // System.out.println("***CF***Inside modifyOrder pContext is:"+pContext );
    
    if (order != null) {
      if (order instanceof OrderImpl) {
        // System.out.println("***CF***Inside order instanceof OrderImpl:");
        OrderImpl orderImpl = (OrderImpl) order;
        // System.out.println("***CF***orderImpl is:" + orderImpl.toString() );
        
        // Get an iterator over each commerceItemRelationship
        @SuppressWarnings("unchecked")
        List<CommerceItemRelationship> commerceItems = mCommerceItemManager.getAllCommerceItemRelationships(order);
        Iterator<CommerceItemRelationship> commerceItemIterator = commerceItems.iterator();
        // The accumulator that represents our value
        double qualifyingValue = 0.00;
        // System.out.println("***CF***Examining Order");
        while (commerceItemIterator.hasNext()) {
          // need to check if its a commerceitem first
          CommerceItemRelationship ciRel = (CommerceItemRelationship) commerceItemIterator.next();
          CommerceItem thisItem = ciRel.getCommerceItem();
          if (thisItem instanceof NMCommerceItem) {
            // System.out.println("***CF***Examining commerceItem " + thisItem.getId() + " (catalogRefId = " + thisItem.getCatalogRefId() + ")");
            
            if (itemMatchesCriteria(thisItem, pCMOSSKU)) {
              // qualifyingValue = 0.00;
              // This commerce item is valid. Add it to our running total.
              NMCommerceItem nmci = (NMCommerceItem) thisItem;
              String prodId = (String) nmci.getPropertyValue("productId");
              
              // System.out.println("***CF*** prodId:"+prodId);
              RepositoryItem ri = (RepositoryItem) pRepository.getItem(prodId, "product");
              double rPrice = ((Double) ri.getPropertyValue("retailPrice")).doubleValue();
              
              // System.out.println("***CF***Match! itemMatchesCriteria  Including item in total.");
              // System.out.println("***CF***rPrice is:"+rPrice);
              // System.out.println("***CF***thisItem.getQuantity()is:"+thisItem.getQuantity());
              // System.out.println("***CF***item qualifyingValue is:"+ thisItem.getPriceInfo().getAmount());
              // ****NOTICE getPriceInfo().getAmount was not being updated correctly!!Went to this new way to fix.****
              // qualifyingValue += thisItem.getPriceInfo().getAmount() * thisItem.getQuantity();
              qualifyingValue += rPrice * thisItem.getQuantity();
              // System.out.println("***CF***Total qualifyingValue is:" + qualifyingValue);
              
            } // if (itemMatchesCriteria(thisItem))
          }
          // System.out.println("***CF <<<Not an instance of NMCommerceItem:thisItem"+thisItem);
        } // while (commerceItemIterator.hasNext())
        
        orderImpl.setPropertyValue(pTPOVariable, new Double(qualifyingValue));
      }
    } // if (order != null)
    
  }
  
  protected boolean itemMatchesCriteria(CommerceItem pItem, String pCmosSkuId) {
    if (pItem instanceof CommerceItemImpl) {
      CommerceItemImpl thisItem = (CommerceItemImpl) pItem;
      String cmosCatId = (String) thisItem.getPropertyValue("cmosCatalogId"); // selected by user
      Vector<String> vCatalogCodes = new Vector<String>();// Holds CatalogCodes from scenario
      
      // Build a list of CatalogCodes from the scenario and add them to the vector for checking
      StringTokenizer stCatalogCodes = new StringTokenizer(mCatalogCode, ",");// from scenario
      // System.out.println("2. ***CF******mCmosItemCode is:"+mCmosItemCode);
      
      while (stCatalogCodes.hasMoreTokens()) {
        String fCatalogCodes = stCatalogCodes.nextToken();
        
        if (!vCatalogCodes.contains(fCatalogCodes)) ;
        {
          vCatalogCodes.addElement(fCatalogCodes.trim());
        }
        // System.out.println("***CF***vCatalogCodes size is:"+vCatalogCodes.size());
      }// end while vCatalogCodes now loaded
      
      if (vCatalogCodes.contains(cmosCatId)) {
        itemMatch = true;
      } else {
        itemMatch = false;
      }
    }
    
    return itemMatch;
  }
  
  private Order getOrderToModify(ProcessExecutionContext pContext) throws CommerceException {
    DynamoHttpServletRequest request = pContext.getRequest();
    
    // If we are executing this action in the context of a session then we want to get a hold
    // of the OrderHolder and use the Order within it as the Order that we want to get
    // information from.
    if (request != null) {
      OrderHolder oh = (OrderHolder) request.resolveName(mOrderHolderComponent);
      return oh.getCurrent();
    }
    
    String profileId = pContext.getSubject().getRepositoryId();
    orderState = StateDefinitions.ORDERSTATES.getStateValue(OrderStates.INCOMPLETE);
    @SuppressWarnings("unchecked")
    List<Order> incompleteOrder = mOrderQuery.getOrdersForProfileInState(profileId, orderState);
    // Just modify the user's first order that's in the correct state
    if (incompleteOrder != null) {
      if (incompleteOrder.size() > 0) {
        return (Order) incompleteOrder.get(0);
      }
    }
    
    return null;
  }
  
}// end of class

