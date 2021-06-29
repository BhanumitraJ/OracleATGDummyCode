package com.nm.commerce.promotion;

import atg.commerce.promotion.*;
import atg.commerce.order.*;
import atg.commerce.CommerceException;
import atg.commerce.states.StateDefinitions;
import atg.commerce.order.CommerceItemRelationship;

import atg.nucleus.Nucleus;
import atg.nucleus.naming.ComponentName;
import atg.servlet.DynamoHttpServletRequest;

import atg.naming.NameResolver;
import atg.repository.*;
import atg.scenario.*;
import atg.scenario.action.*;
import atg.repository.rql.*;

import java.util.Map;
import java.util.Collection;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.*;

import javax.servlet.ServletException;
import java.io.IOException;
// ------------------------
import com.nm.commerce.*;

/**
 * This Filter action is responsible for re-calculating the value of a particular order parameter with the total value of all items that match a particular set of criteria. In this case it would be
 * Dept,Class,and VendorId.
 * 
 * 
 * @author by: Pat Everheart
 * 
 * */

public class DeptClassVendorFilter extends ActionImpl {
  public static final String CLASS_VERSION = "$Id: DeptClassVendorFilter.java 1.2 2008/04/29 13:57:33CDT nmmc5 Exp  $";
  
  Repository pRepository = (Repository) Nucleus.getGlobalNucleus().resolveName("/atg/commerce/catalog/ProductCatalog");
  
  /** Parameter for the location of the ordermanager */
  public String ORDERMANAGER_PATH = "/atg/commerce/order/OrderManager";
  
  /** Parameter for which variable to change */
  public static final String VARIABLE_PARAM = "variable"; // The variable totalPromotionalOrderValue
  
  /** Parameter for excluded CMOS SKU id information */
  public static final String CMOSSKU_PARAM = "cmos_sku";
  
  /** Parameter for dept information */
  public static final String DEPTSOURCE_PARAM = "DeptSource_code";
  
  /** Parameter for source code information */
  public static final String SOURCE_CODE_PARAM = "source_code";
  
  public static final String DEPT_CLASS_VENDORID = "Dept_Class_VendorId";
  
  protected OrderManager mOrderManager = null;
  
  protected String mCmosSku = null;// The CmosSKU set in the scenario
  protected String mDeptClassVendorId = null;// The mDeptClassVendorId set in the scenario
  protected boolean itemMatch = false;
  protected Repository productRepository;
  
  public static final String SHOPPING_CART_PATH = "/atg/commerce/ShoppingCart";
  
  protected String mVariable = null;
  protected double mAmount = 0.0;
  
  protected ComponentName mOrderHolderComponent = null;
  
  public void initialize(Map pParameters) throws ScenarioException {
    mOrderManager = (OrderManager) Nucleus.getGlobalNucleus().resolveName(ORDERMANAGER_PATH);
    
    if (mOrderManager == null) throw new ScenarioException(PromotionConstants.getStringResource(PromotionConstants.ORDER_MANAGER_NOT_FOUND));
    
    storeRequiredParameter(pParameters, VARIABLE_PARAM, java.lang.String.class);
    storeRequiredParameter(pParameters, CMOSSKU_PARAM, java.lang.String.class);// filters out GWP sku
    storeRequiredParameter(pParameters, DEPT_CLASS_VENDORID, java.lang.String.class);// "255 29 90047916"
    
    mOrderHolderComponent = ComponentName.getComponentName(SHOPPING_CART_PATH);
  }
  
  protected void executeAction(ScenarioExecutionContext pContext) throws ScenarioException {
    if (mOrderManager == null) throw new ScenarioException(PromotionConstants.getStringResource(PromotionConstants.ORDER_MANAGER_NOT_FOUND));
    
    // If the sku id is null then we should see if it has been passed in. In theory this
    // should happen once and only once. At the same time we will take the opportunity to load
    // up the other properties
    
    if (mVariable == null) {
      synchronized (VARIABLE_PARAM) {
        mVariable = (String) getParameterValue(VARIABLE_PARAM, pContext);
        // System.out.println(VARIABLE_PARAM + " parameter is " + mVariable);
      } // synchronized (VARIABLE_PARAM)
    } // mVariable == null
    
    if (mVariable == null) throw new ScenarioException("***DCVF***Variable name was not specified");
    
    if (mCmosSku == null) {
      synchronized (CMOSSKU_PARAM) {
        mCmosSku = (String) getParameterValue(CMOSSKU_PARAM, pContext);
        // System.out.println("***DCVF***mCmosSku is:"+mCmosSku);
        
      }
      
    }
    
    if (mDeptClassVendorId == null) {
      synchronized (DEPT_CLASS_VENDORID) {
        mDeptClassVendorId = (String) getParameterValue(DEPT_CLASS_VENDORID, pContext);
        // System.out.println("***DCVF***mDeptClassVendorId is:"+mDeptClassVendorId);
      }
    }
    
    try {
      modifyOrder(mVariable, mCmosSku, pContext);
    } catch (CommerceException ce) {
      throw new ScenarioException(ce);
    } catch (RepositoryException re) {
      throw new ScenarioException(re);
    }
    
  }
  
  /**
   * This method returns the order which should be modified.
   * 
   * @param pContext
   *          - the scenario context this is being evaluated in
   **/
  public Order getOrderToModify(ScenarioExecutionContext pContext) throws CommerceException, RepositoryException {
    
    DynamoHttpServletRequest request = pContext.getRequest();
    
    // If we are executing this action in the context of a session then we want to get a hold
    // of the OrderHolder and use the Order within it as the Order that we want to get
    // information from.
    if (request != null) {
      OrderHolder oh = (OrderHolder) request.resolveName(mOrderHolderComponent);
      return oh.getCurrent();
    }
    
    // Get the profile from the context
    String profileId = ((RepositoryItem) pContext.getProfile()).getRepositoryId();
    
    // Only change orders that are in the "incomplete" state
    List l = mOrderManager.getOrdersForProfileInState(profileId, StateDefinitions.ORDERSTATES.getStateValue(StateDefinitions.ORDERSTATES.INCOMPLETE));
    
    // Just modify the user's first order that's in the correct state
    if (l != null) {
      if (l.size() > 0) {
        return (Order) l.get(0);
      }
    }
    return null;
  }
  
  /**
   * This method is needed so that it will not add the value of the promotional item to the PromotionalOrderVariable. This is so its value will not affect to PromotionalValue, especially when removing
   * items from the cart. If this doesnt occur, and a promo item has a price, that price will be included in the PromotionalOrderVariable, thus throwing off the checked value. Returns true if the
   * passed-in item matches our search criteria
   * 
   * This method also checks for Dept and SourceCode and VendorId of the sku added to the cart. If the sku is in the desired Dept and/or SourceCode and/or VendorId, then the price of the item will be
   * added to the promotional value. Dept Code is not required. Source Code is not required. Format of Dept/Source/VendorId code-- (100/5/90047916,232/16/90047916) vendorId is not required
   * 
   * @param pItem
   *          - the commerce item to be examined
   */
  
  protected boolean itemMatchesCriteria(CommerceItem pItem, String pCmosSkuId) {
    if (pItem instanceof CommerceItemImpl) {
      
      NMCommerceItem thisItem = (NMCommerceItem) pItem;
      String cmosCatId = (String) thisItem.getPropertyValue("cmosCatalogId");
      String vendorId = "";
      String CmosSkuId = (String) thisItem.getCmosSKUId();
      // System.out.println("***DCVF**customer selected pCmosSkuId is:"+CmosSkuId);
      
      RepositoryItem productItem = (RepositoryItem) thisItem.getAuxiliaryData().getProductRef();
      Vector vDeptSource = new Vector();// Holds DeptSource codes from scenario
      Vector vVendorId = new Vector();// Holds VendorIds from scenario
      
      String deptCode = (String) productItem.getPropertyValue("deptCode");// :deptCode:The product selected by the customer
      String sourceCode = (String) productItem.getPropertyValue("sourceCode");// :sourceCode:The product selected by customer
      String cmosItemCode = (String) productItem.getPropertyValue("cmosItemCode");// :cmosItemCode:The product selected by customer
      // System.out.println("***DCVF**customer selected :"+deptCode+"-"+sourceCode+"-"+cmosItemCode);
      
      try {
        RepositoryView skuview = pRepository.getView("sku");
        
        RqlStatement skuStatement = RqlStatement.parseRqlStatement("cmosSKU = ?0");
        Object params[] = new Object[1];
        params[0] = CmosSkuId;
        RepositoryItem skuArray[] = skuStatement.executeQuery(skuview, params);
        // System.out.println("***DCVF**skuArray not null:"+skuArray);
        
        for (int i = 0; i < skuArray.length; i++) {
          RepositoryItem sku = (RepositoryItem) skuArray[i];
          // System.out.println("***DCVF**RepositoryItem is made:");
          
          vendorId = (String) sku.getPropertyValue("vendorId");
          // System.out.println("***DCVF**customer selected vendorId is:"+vendorId);
          // Must filter out the nulls
          if (vendorId == null) {
            vendorId = "none";
          }
          // System.out.println("***DCVF**customer selected vendorId is now:"+vendorId);
        }
        
      } catch (RepositoryException re) {
        // System.out.println("***Inside DCVF catch run ****");
        re.printStackTrace();
      }
      
      // System.out.println("***DCVF******pCmosSkuId***"+pCmosSkuId);
      // System.out.println("***DCVF******customer selected deptCode***"+deptCode); //:deptCode:The product selected by the customer
      // System.out.println("***DCVF******customer selected sourceCode***"+sourceCode);//:sourceCode:The product selected by customer
      // System.out.println("***DCVF******customer selected cmosItemCode***"+cmosItemCode);//:cmosItemCode:The product selected by customer
      // System.out.println("***DCVF******customer selected vendorId***"+vendorId);//:vendorId:The sku selected by customer
      // System.out.println("***DCVF******scenario mDeptClassVendorId is:"+mDeptClassVendorId); //:mDeptClassVendorId
      
      String cDeptSourceVendor = deptCode + "/" + sourceCode + "/" + vendorId; // Customer selected product
      // System.out.println("***DCVF***Customer selected cDeptSourceVendor:"+cDeptSourceVendor);
      // System.out.println("***DCVF***Scenario selected mDeptCode"+mDeptCode);
      
      // Break down scenario list of codes and load into a Vector
      // Dept and Source code and VendorId are separated by a space and grouped by commas(100 13 7916,222 5,343 12)
      // Use asterisk for blank placeholders ie: If no sourceCode user *(100 * 7916,222 5 7916,* * 7916)
      StringTokenizer stDeptClassVendorCodes = new StringTokenizer(mDeptClassVendorId, ",");
      while (stDeptClassVendorCodes.hasMoreTokens()) {
        String fToken = stDeptClassVendorCodes.nextToken();
        
        if (!vDeptSource.contains(fToken)) ;
        {
          vDeptSource.addElement(fToken.trim());
          // System.out.println("***DCVF** fToken added to vector---"+fToken);
        }
      }// Vector now loaded
      
      // Now we compare against the vector for a match of just a dept code(95 *)
      // or a specific Dept + SourceCode(100 13)
      // or a specifice vendorId(7916)
      // mod them so we can check vectors more easily
      String modDeptCode = deptCode.trim() + "/*/*";
      String modDeptVendorId = deptCode.trim() + "/*/" + vendorId.trim(); // no class
      String modVendorId = "*/*/" + vendorId.trim(); // no dept or class
      
      // System.out.println("***DCVF*** cDeptSourceVendor is:"+cDeptSourceVendor);
      // cust selected modDeptCode or cDeptSource
      // scenario set vDeptSource
      if (vDeptSource.contains(cDeptSourceVendor) || vDeptSource.contains(modDeptVendorId) || vDeptSource.contains(modVendorId)) {
        // System.out.println("***DCVF*** itemMatch = true");
        itemMatch = true;
      } else {
        // System.out.println("***DCVF*** itemMatch = false");
        itemMatch = false;
      }
      
    }// end if(Commerce)
    
    if (itemMatch) {
      return true;
    } else {
      return false;
    }
  }
  
  /**
   * This method will actually perform the action of changing the value.
   * 
   * @param pVariable
   *          The parameter to be modified
   * @param pContext
   *          the context in which the action is occuring
   * @exception CommerceException
   *              if an error occurs
   * @exception RepositoryException
   *              if an error occurs
   */
  protected void modifyOrder(String pVariable, String pCMOSSKU, ScenarioExecutionContext pContext) throws CommerceException, RepositoryException {
    Order order = getOrderToModify(pContext);
    
    if (order != null) {
      
      if (order instanceof OrderImpl) {
        // System.out.println("***DCVF***Inside order instanceof OrderImpl:");
        // Have to cast down to something that can get arbitrary property values
        OrderImpl orderImpl = (OrderImpl) order;
        
        // Get an iterator over each commerceItemRelationship
        List items = mOrderManager.getAllCommerceItemRelationships(order);
        Iterator iter = items.iterator();
        
        // The accumulator that represents our value
        double qualifyingValue = 0.00;
        
        // Examine each commerceItem relationship
        while (iter.hasNext()) {
          CommerceItemRelationship ciRel = (CommerceItemRelationship) iter.next();
          
          // Examine all commerce items
          CommerceItem thisItem = ciRel.getCommerceItem();
          
          if (itemMatchesCriteria(thisItem, pCMOSSKU)) {
            // This commerce item is valid. Add it to our running total.
            
            // System.out.println("***DCVF***Match! itemMatchesCriteria  Including item in total.");
            // System.out.println("***DCVF*** thisItem is:"+thisItem.getId());
            
            NMCommerceItem nmci = (NMCommerceItem) thisItem;
            String prodId = (String) nmci.getPropertyValue("productId");
            
            // System.out.println("***DCVF*** prodId:"+prodId);
            RepositoryItem ri = (RepositoryItem) pRepository.getItem(prodId, "product");
            
            double rPrice = ((Double) ri.getPropertyValue("retailPrice")).doubleValue();
            
            // qualifyingValue += thisItem.getPriceInfo().getListPrice() * thisItem.getQuantity();
            // System.out.println("***DCVF***thisItem retailPrice:"+rPrice);
            // System.out.println("***DCVF***item qualifyingValue is:"+ thisItem.getPriceInfo().getAmount());
            // System.out.println("***DCVF***item quantity is:"+ thisItem.getQuantity());
            
            qualifyingValue += rPrice * thisItem.getQuantity();
            // System.out.println("***DCVF***Total qualifyingValue is:" + qualifyingValue);
            
          } // if (itemMatchesCriteria(thisItem))
          
        } // while (iter.hasNext())
        
        // Update the appropriate variable with what we've found
        orderImpl.setPropertyValue(pVariable, new Double(qualifyingValue));
        
      } else {
        // System.out.println("Order can't be cast to a repository-holding class!");
      } // if (order instanceof OrderImpl)
      
    } // if (order != null)
    
  } // end of method ModifyOrder
  
} // end of class

