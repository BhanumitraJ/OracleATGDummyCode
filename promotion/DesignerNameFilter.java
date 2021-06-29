/*--------------------------------------------------
 CLASS NAME:	DesignerNameFilter.java
 Author:		Pat Everheart

Used to filter totals based on web designer name.
 --------------------------------------------------*/

package com.nm.commerce.promotion;

import atg.nucleus.Nucleus;
import atg.nucleus.naming.ComponentName;

import atg.process.*;
import atg.process.action.ActionImpl;
import atg.repository.Repository;
import atg.repository.RepositoryException;
import atg.repository.RepositoryItem;
import atg.scenario.*;
import atg.servlet.DynamoHttpServletRequest;
import atg.commerce.promotion.*;
import atg.commerce.order.*;
import atg.commerce.order.CommerceItemRelationship;
import atg.commerce.states.OrderStates;
import atg.commerce.states.StateDefinitions;
import atg.commerce.CommerceException;

import java.util.*;

import com.nm.commerce.NMCommerceItem;

public class DesignerNameFilter extends ActionImpl {
  
  public static final String CLASS_VERSION = "$Id: DesignerNameFilter.java 1.3 2012/12/11 11:22:54CST Jessica McCarty (nmjjm4) Exp  $";
  public String ORDERMANAGER_PATH = "/atg/commerce/order/OrderManager";
  public static final String ORDERQUERIES_PATH = "/atg/commerce/order/OrderQueries";
  public static final String AMOUNT_PARAM = "amount";
  public static final String VARIABLE_PARAM = "variable";
  public static final String CMOSSKU_PARAM = "cmos_sku";
  public static final String WEB_DESIGNER_NAME = "Web_Designer_Name";
  
  protected String mCmosSku = null;// The CmosSKU set in the scenario
  protected String mWebDesignerName = null;// The Web Designer Name set in the scenario
  protected boolean itemMatch = false;
  protected boolean WebDesignerflag = false;
  public static final String SHOPPING_CART_PATH = "/atg/commerce/ShoppingCart";
  protected String mTPOV = null;// The totalPromotionalOrderVariable set in the scenario
  protected double mAmount = 0.0;
  protected int orderState = 0;
  
  protected ComponentName mOrderHolderComponent = null;
  protected OrderManager mOrderManager = null;
  protected OrderQueries mOrderQuery = null;
  
  protected CommerceItemRelationshipContainer mCommerceItemRelationshipContainer = null;
  
  public void initialize(Map pParameters) throws ProcessException {
    mOrderManager = (OrderManager) Nucleus.getGlobalNucleus().resolveName(ORDERMANAGER_PATH);
    mOrderQuery = (OrderQueries) Nucleus.getGlobalNucleus().resolveName(ORDERQUERIES_PATH);
    
    storeRequiredParameter(pParameters, VARIABLE_PARAM, java.lang.String.class);
    storeRequiredParameter(pParameters, CMOSSKU_PARAM, java.lang.String.class);// filters out GWP sku
    storeRequiredParameter(pParameters, WEB_DESIGNER_NAME, java.lang.String.class);// "webdesignername"
    
    mOrderHolderComponent = ComponentName.getComponentName(SHOPPING_CART_PATH);
    
  }
  
  protected void executeAction(ProcessExecutionContext pContext) throws ProcessException {
    if (mOrderManager == null) throw new ScenarioException(PromotionConstants.getStringResource(PromotionConstants.ORDER_MANAGER_NOT_FOUND));
    
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
    
    if (mWebDesignerName == null) {
      synchronized (WEB_DESIGNER_NAME) {
        mWebDesignerName = (String) getParameterValue(WEB_DESIGNER_NAME, pContext);
        // System.out.println("***DNF***mWebDesignerName is:"+mWebDesignerName);
      }
    }
    
    try {
      modifyOrder(mTPOV, mCmosSku, pContext);
    } catch (CommerceException ce) {
      throw new ScenarioException(ce);
    } catch (RepositoryException re) {
      throw new ScenarioException(re);
    }
    
  }
  
  public Order getOrderToModify(ProcessExecutionContext pContext) throws CommerceException {
    DynamoHttpServletRequest request = pContext.getRequest();
    
    if (request != null) {
      OrderHolder oh = (OrderHolder) request.resolveName(mOrderHolderComponent);
      return oh.getCurrent();
    }
    
    String profileId = pContext.getSubject().getRepositoryId();
    orderState = StateDefinitions.ORDERSTATES.getStateValue(OrderStates.INCOMPLETE);
    List incompleteOrder = mOrderQuery.getOrdersForProfileInState(profileId, orderState);
    // Just modify the user's first order that's in the correct state
    if (incompleteOrder != null) {
      if (incompleteOrder.size() > 0) {
        return (Order) incompleteOrder.get(0);
      }
    }
    
    return null;
  }
  
  protected boolean itemMatchesCriteria(CommerceItem pItem, String pCmosSkuId) {
    if (pItem instanceof CommerceItemImpl) {
      CommerceItemImpl thisItem = (CommerceItemImpl) pItem;
      
      RepositoryItem productItem = (RepositoryItem) thisItem.getAuxiliaryData().getProductRef();
      Vector vDeptSource = new Vector();// Holds DeptSource codes from scenario
      Vector vWebDesignerName = new Vector();// Holds WEBDESIGNERNAMES from scenario
      String webDesignerName = (String) productItem.getPropertyValue("cmDesignerName");// :webDesignerName:The product selected by customer
      
      // Build a list of Web Designer Names from the scenario and add them to the vector for checking
      StringTokenizer stWebDesignerNames = new StringTokenizer(mWebDesignerName, ",");// from scenario
      
      if (mWebDesignerName != null && mWebDesignerName.trim().length() > 0) {
        if (mWebDesignerName.equalsIgnoreCase(webDesignerName)) {
          WebDesignerflag = true;
        }
        
      }
      
      if ((WebDesignerflag)) {
        while (stWebDesignerNames.hasMoreTokens()) {
          String fWebDesignerNames = stWebDesignerNames.nextToken();
          if (!vWebDesignerName.contains(fWebDesignerNames)) ;
          {
            vDeptSource.addElement(fWebDesignerNames.trim());
          }
        }// end while Vector now loaded
        
      }
      if (WebDesignerflag) {
        // cust selected modDeptCode or cDeptSource
        // scenario set vDeptSource
        if (vDeptSource.contains(webDesignerName)) {
          itemMatch = true;
        } else {
          itemMatch = false;
        }
        
      }
      
    }// end if(Commerce)
    
    if (itemMatch) {
      return true;
    } else {
      return false;
    }
  }
  
  protected void modifyOrder(String pTPOVariable, String pCMOSSKU, ProcessExecutionContext pContext) throws ProcessException, CommerceException, RepositoryException {
    Order order = getOrderToModify(pContext);
    Repository pRepository = (Repository) Nucleus.getGlobalNucleus().resolveName("/atg/commerce/catalog/ProductCatalog");
    // System.out.println("***DNF***Inside modifyOrder pContext is:"+pContext );
    
    if (order != null) {
      
      if (order instanceof OrderImpl) {
        // System.out.println("***DNF***Inside order instanceof OrderImpl:");
        // Have to cast down to something that can get arbitrary property values
        OrderImpl orderImpl = (OrderImpl) order;
        // System.out.println("***DNF***orderImpl is:" + orderImpl.toString() );
        // System.out.println(pTPOVariable + " = " + orderImpl.getPropertyValue(pTPOVariable).toString());
        
        // Get an iterator over each commerceItemRelationship
        // List items = mOrderManager.getAllCommerceItemRelationships(order);
        // List items = mCommerceItemRelationshipContainer.getCommerceItemRelationships();
        List items = order.getRelationships();
        Iterator iter = items.iterator();
        
        // The accumulator that represents our value
        double qualifyingValue = 0.00;
        
        // System.out.println("***DNF***Examining Order");
        
        // Examine each commerceItem relationship
        while (iter.hasNext()) {
          CommerceItemRelationship ciRel = (CommerceItemRelationship) iter.next();
          CommerceItem thisItem = ciRel.getCommerceItem();
          
          // System.out.println("***DNF***Examining commerceItem " + thisItem.getId() + " (catalogRefId = " + thisItem.getCatalogRefId() + ")");
          
          if (itemMatchesCriteria(thisItem, pCMOSSKU)) {
            // qualifyingValue = 0.00;
            // This commerce item is valid. Add it to our running total.
            NMCommerceItem nmci = (NMCommerceItem) thisItem;
            String prodId = (String) nmci.getPropertyValue("productId");
            
            // System.out.println("***CF*** prodId:"+prodId);
            RepositoryItem ri = (RepositoryItem) pRepository.getItem(prodId, "product");
            double rPrice = ((Double) ri.getPropertyValue("retailPrice")).doubleValue();
            
            // System.out.println("***DNF***Match! itemMatchesCriteria  Including item in total.");
            // System.out.println("***DNF***rPrice is:"+rPrice);
            // System.out.println("***DNF***thisItem.getQuantity()is:"+thisItem.getQuantity());
            // System.out.println("***DNF***item qualifyingValue is:"+ thisItem.getPriceInfo().getAmount());
            // ****NOTICE getPriceInfo().getAmount was not being updated correctly!!Went to this new way to fix.****
            // qualifyingValue += thisItem.getPriceInfo().getAmount() * thisItem.getQuantity();
            qualifyingValue += rPrice * thisItem.getQuantity();
            // System.out.println("***DNF***Total qualifyingValue is:" + qualifyingValue);
            
          } // if (itemMatchesCriteria(thisItem))
          
          // System.out.println("***DNF <<<qualifyingValue getting set to order is:" + qualifyingValue);
          // orderImpl.setPropertyValue(pTPOVariable, new Double(qualifyingValue));
          
        } // while (iter.hasNext())
        
        // Update the appropriate variable with what we've found
        // System.out.println("***DNF <<<qualifyingValue getting set to order is:" + qualifyingValue);
        orderImpl.setPropertyValue(pTPOVariable, new Double(qualifyingValue));
        
      }
      
    } // if (order != null)
    
  }
  
} // end of class

