/*
 * <ATGCOPYRIGHT> Copyright (C) 1997-2001 Art Technology Group, Inc. All Rights Reserved. No use, copying or distribution of this work may be made except in accordance with a valid license agreement
 * from Art Technology Group. This notice must be included on all copies, modifications and derivatives of this work.
 * 
 * Art Technology Group (ATG) MAKES NO REPRESENTATIONS OR WARRANTIES ABOUT THE SUITABILITY OF THE SOFTWARE, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, OR NON-INFRINGEMENT. ATG SHALL NOT BE LIABLE FOR ANY DAMAGES SUFFERED BY LICENSEE AS A RESULT OF USING, MODIFYING OR DISTRIBUTING THIS SOFTWARE OR
 * ITS DERIVATIVES.
 * 
 * "Dynamo" is a trademark of Art Technology Group, Inc. </ATGCOPYRIGHT>
 */

package com.nm.commerce.promotion;

import atg.commerce.promotion.*;
import atg.commerce.order.*;
import atg.commerce.CommerceException;
import atg.commerce.states.StateDefinitions;
import atg.commerce.order.CommerceItemRelationship;

import atg.nucleus.Nucleus;
import atg.nucleus.naming.ComponentName;
import atg.servlet.DynamoHttpServletRequest;

import atg.repository.*;
import atg.scenario.*;
import atg.scenario.action.*;

import java.util.Map;
import java.util.List;
import java.util.Iterator;
import java.util.*;
// ------------------------
import com.nm.commerce.*;

/**
 * This action is responsible for re-calculating the value of a particular order paramater with the total value of all items that match a particular set of criteria.
 * 
 * 
 * @author Ert Dredge
 * @modified by: Chee-Chien Loo
 * @extensively modified by: Pat Everheart
 * @version $Id: UpdatePromotionalOrderVariable.java 1.2 2008/04/29 13:59:35CDT nmmc5 Exp $
 */

public class UpdatePromotionalOrderVariable extends ActionImpl {
  // -------------------------------------
  /** Class version string */
  public static final String CLASS_VERSION = "$Id: UpdatePromotionalOrderVariable.java 1.2 2008/04/29 13:59:35CDT nmmc5 Exp  $";
  
  /** Parameter for the location of the ordermanager */
  public String ORDERMANAGER_PATH = "/atg/commerce/order/OrderManager";
  
  /** Parameter for how much to change by */
  public static final String AMOUNT_PARAM = "amount";
  
  /** Parameter for which variable to change */
  public static final String VARIABLE_PARAM = "variable";
  
  /** Parameter for whether to increment, decrement, or zero the value */
  // public static final String DIRECTION_PARAM = "direction";
  
  /** Parameter for excluded CMOS SKU id information */
  // public static final String SKU_PARAM = "sku_id";
  public static final String CMOSSKU_PARAM = "cmos_sku";
  
  /** Parameter for dept information */
  public static final String DEPTSOURCE_PARAM = "DeptSource_code";
  
  /** Parameter for source code information */
  public static final String SOURCE_CODE_PARAM = "source_code";
  
  /** Parameter for CMOS_ITEM_CODE information */
  public static final String CMOS_ITEM_CODES = "CMOS_Item_Code";
  
  /** reference to the order manager object */
  protected OrderManager mOrderManager = null;
  
  protected String mCmosSku = null;// The CmosSKU set in the scenario
  protected String mDeptCode = null;// The DeptCode set in the scenario
  protected String mSourceCode = null;// The sourcCode set in the scenario
  protected String mCmosItemCode = null;// The CmosItemCodes set in the scenario
  protected boolean itemMatch = false;
  protected boolean CRflag = false;// Is set if we are checking for cmositems that begin with 'C' or 'R'
  protected Repository productRepository;
  
  /**
   * Variables that describe what items should be included in the total value count
   */
  
  /** The shopping cart component */
  public static final String SHOPPING_CART_PATH = "/atg/commerce/ShoppingCart";
  
  protected String mVariable = null;
  protected double mAmount = 0.0;
  
  protected ComponentName mOrderHolderComponent = null;
  
  // -------------------------------------
  /**
   * UpdatePromotionalOrderVariable
   */
  
  public void initialize(Map pParameters) throws ScenarioException {
    mOrderManager = (OrderManager) Nucleus.getGlobalNucleus().resolveName(ORDERMANAGER_PATH);
    
    // <TBD> make sure that all strings are put in resource files.
    if (mOrderManager == null) throw new ScenarioException(PromotionConstants.getStringResource(PromotionConstants.ORDER_MANAGER_NOT_FOUND));
    
    storeRequiredParameter(pParameters, VARIABLE_PARAM, java.lang.String.class);
    storeRequiredParameter(pParameters, CMOSSKU_PARAM, java.lang.String.class);// filters out GWP sku
    storeRequiredParameter(pParameters, DEPTSOURCE_PARAM, java.lang.String.class);
    storeRequiredParameter(pParameters, CMOS_ITEM_CODES, java.lang.String.class);// "C,R,etc.."
    
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
    
    if (mVariable == null) throw new ScenarioException("***UPOV***Variable name was not specified");
    
    if (mCmosSku == null) {
      synchronized (CMOSSKU_PARAM) {
        mCmosSku = (String) getParameterValue(CMOSSKU_PARAM, pContext);
        // System.out.println("***UPOV***mCmosSku is:"+mCmosSku);
        
      }
      
    }
    
    if (mDeptCode == null) {
      synchronized (DEPTSOURCE_PARAM) {
        mDeptCode = (String) getParameterValue(DEPTSOURCE_PARAM, pContext);
        // System.out.println("***UPOV***mDeptCode is:"+mDeptCode);
        
      }
    }
    
    if (mCmosItemCode == null) {
      synchronized (CMOS_ITEM_CODES) {
        mCmosItemCode = (String) getParameterValue(CMOS_ITEM_CODES, pContext);
        // System.out.println("***UPOV***mCmosItemCode is:"+mCmosItemCode);
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
   * This method also checks for Dept and SourceCode or CMOSItemCode of the sku added to the cart. If the sku is in the desired Dept and/or SourceCode or CMOSItemCode, then the price of the item will
   * be added to the promotional value. Dept Code is not required. Source Code is not required. Format of Dept/Source code-- (100 5,232 16,666,434 12) cmosItemCode is not required(C,R,etc..)
   * 
   * @param pItem
   *          - the commerce item to be examined
   */
  
  protected boolean itemMatchesCriteria(CommerceItem pItem, String pCmosSkuId) {
    if (pItem instanceof CommerceItemImpl) {
      boolean deptFilter = false;
      
      CommerceItemImpl thisItem = (CommerceItemImpl) pItem;
      String cmosCatId = (String) thisItem.getPropertyValue("cmosCatalogId");
      
      RepositoryItem productItem = (RepositoryItem) thisItem.getAuxiliaryData().getProductRef();
      Vector vDeptSource = new Vector();// Holds DeptSource codes from scenario
      Vector vCMOSItemCodes = new Vector();// Holds CMOSITEMCODES from scenario
      
      String deptCode = (String) productItem.getPropertyValue("deptCode");// :deptCode:The product selected by the customer
      String sourceCode = (String) productItem.getPropertyValue("sourceCode");// :sourceCode:The product selected by customer
      String cmosItemCode = (String) productItem.getPropertyValue("cmosItemCode");// :cmosItemCode:The product selected by customer
      
      // System.out.println("***UPOV******pCmosSkuId***"+pCmosSkuId);
      // System.out.println("***UPOV******customer selected deptCode***"+deptCode); //:deptCode:The product selected by the customer
      // System.out.println("***UPOV******customer selected sourceCode***"+sourceCode);//:sourceCode:The product selected by customer
      // System.out.println("***UPOV******customer selected cmosItemCode***"+cmosItemCode);//:cmosItemCode:The product selected by customer
      // System.out.println("***UPOV******scenario mDeptCode is:"+mDeptCode); //:mDeptCode:The product set in the scenario
      
      String cDeptSource = deptCode + " " + sourceCode; // Customer selected product
      // System.out.println("***UPOV***Customer selected cDeptSource"+cDeptSource);
      // System.out.println("***UPOV***Scenario selected mDeptCode"+mDeptCode);
      
      // Break down scenario list of codes and load into a Vector
      // Dept and Source code are separated by a space and grouped by commas(100 13,222 5,343 12)
      StringTokenizer stDeptCodes = new StringTokenizer(mDeptCode, ",");
      while (stDeptCodes.hasMoreTokens()) {
        deptFilter = true;
        // System.out.println("deptFilter is true:"+deptFilter);
        String fToken = stDeptCodes.nextToken();
        // System.out.println("**List of Tokens**"+fToken);
        
        // Check each token for a sourceCode. If none, add the ' *' to it.
        // We will end up with this(100 13,222 *,343 *,615 9) if there isnt a source code
        StringTokenizer stfToken = new StringTokenizer(fToken);
        stfToken.nextToken();
        // Looking for the presence of a 'space',if none,add the ' *'
        if (!stfToken.hasMoreTokens()) {
          fToken = fToken + " *";
          // System.out.println("fToken is now:"+fToken);
        }
        
        if (!vDeptSource.contains(fToken)) ;
        {
          // System.out.println("---if(!vDeptSource.contains(fToken)---");
          vDeptSource.addElement(fToken.trim());
          // System.out.println("---item added to vector---"+fToken);
        }
        // System.out.println("***UPOV*******selected deptSource***"+cDeptSource);
        // System.out.println("***UPOV***vDeptSource size is:"+vDeptSource.size());
      }// Vector now loaded
      
      // System.out.println("1. ***UPOV******vDeptSource*** vector loaded with depts."+vDeptSource);
      // *------------------------------------------------------------------------------------
      // Build a list of Item Codes from the scenario and add them to the vector of dept/class codes
      // for checking
      // System.out.println("deptFilter is false:"+deptFilter);
      
      StringTokenizer stCMOSItemCodes = new StringTokenizer(mCmosItemCode, ",");// from scenario
      String itemCode = "";
      if (stCMOSItemCodes.countTokens() > 0) {
        itemCode = (String) stCMOSItemCodes.nextToken();
      }
      // System.out.println("2. ***UPOV******mCmosItemCode is:"+mCmosItemCode);
      
      // Set CRflag if we are just filtering on CR. Usually for The Beauty Event
      // if(mCmosItemCode.equalsIgnoreCase("C,R") ) //took out to now check any item codes
      // System.out.println("***mCmosItemCode length is:"+itemCode.length());
      // System.out.println("***mCmosItemCode = " + itemCode);
      if (itemCode.length() == 1) {
        CRflag = true;
        // System.out.println("***UPOV******CRFlag is true**"+itemCode);
        stCMOSItemCodes = new StringTokenizer(mCmosItemCode, ",");
      } else {
        CRflag = false;
        // System.out.println("***UPOV******CRFlag is false**"+itemCode);
        stCMOSItemCodes = new StringTokenizer(mCmosItemCode, ",");
      }
      
      if (CRflag) {
        while (stCMOSItemCodes.hasMoreTokens()) {
          String fItemCodes = stCMOSItemCodes.nextToken();
          // System.out.println("***UPOV******CRflag is true");
          // System.out.println("***UPOV******List of customer selected fItemCodes**"+fItemCodes);
          
          // Check each token for C,R CMOSITEMCode.
          if (!vDeptSource.contains(fItemCodes)) ;
          {
            vDeptSource.addElement(fItemCodes.trim());
            // System.out.println("3. ***UPOV******cus selected modItemCode added to vDeptSource---"+vDeptSource);
          }
          // System.out.println("***UPOV***vDeptSource size is:"+vDeptSource.size());
        }// end while Vector now loaded
        
      } else {
        while (stCMOSItemCodes.hasMoreTokens()) {
          String fItemCodes = stCMOSItemCodes.nextToken();
          // System.out.println("***UPOV******List of customer selected fItemCodes**"+fItemCodes);
          
          // Check each token for a CMOSITEMCode.
          String modItemCode = fItemCodes + " *";
          
          if (!vDeptSource.contains(modItemCode)) ;
          {
            vDeptSource.addElement(modItemCode.trim());
            // System.out.println("3. ***UPOV******cus selected modItemCode added to vDeptSource---"+vDeptSource);
          }
          // System.out.println("***UPOV***vDeptSource size is:"+vDeptSource.size());
        }// end while Vector now loaded
        
      }
      
      // *------------------------------------------------------------------------------------
      
      // Now we compare against the vector for a match of just a dept code(95 *)
      // or a specific Dept + SourceCode(100 13)
      // or a specifice cmosItemCode(6004)
      // or if it begins with 'C' or 'R',usually for The Beauty Event!
      // mod them so we can check vectors more easily
      String modDeptCode = deptCode + " *";
      String modcmosItemCode = cmosItemCode + " *";
      
      // Check the CRflag for CR checking only
      if (CRflag) {
        // System.out.println("***UPOV******CRflag is true");
        // cust selected cmosItemCode
        // scenario set vDeptSource
        String firstChar = String.valueOf(cmosItemCode.charAt(0));
        
        if (vDeptSource.contains(firstChar)) {
          // System.out.println("4. ***UPOV***cust selected cmosItemCode starts with:"+firstChar);
          // System.out.println("***UPOV*** itemMatch = true");
          itemMatch = true;
        } else {
          // System.out.println("***UPOV******Vector does not contain these codes!***"+firstChar);
          // System.out.println("***UPOV*** itemMatch = false");
          itemMatch = false;
        }
        
      } else {
        
        // cust selected modDeptCode or cDeptSource
        // scenario set vDeptSource
        if (vDeptSource.contains(modDeptCode) || vDeptSource.contains(cDeptSource) || vDeptSource.contains(modcmosItemCode)) {
          // System.out.println("4. ***UPOV***cust selected modDeptCode or cDeptSource is:"+modDeptCode+" "+cDeptSource+" "+modcmosItemCode);
          // System.out.println("***UPOV*** itemMatch = true");
          itemMatch = true;
        } else {
          // System.out.println("***UPOV******Vector does not contain these codes!***"+modcmosItemCode);
          // System.out.println("***UPOV*** itemMatch = false");
          itemMatch = false;
        }
        
      }
    }// end if(Commerce)
    
    // We didn't match
    // System.out.println("***UPOV***Item " + pItem.toString() + " does not match criteria.");
    
    if (itemMatch) {
      return true;
    } else {
      return false;
    }
    // return false;
  }
  
  /**
   * This method will actually perform the action of changing the value.
   * 
   * @param pAmount
   *          The amount to increment/decrement the parameter
   * @param pDirection
   *          Whether to increment/decrement the value
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
    // System.out.println("***UPOV***Inside modifyOrder pVariable is:"+pVariable );
    
    if (order != null) {
      
      if (order instanceof OrderImpl) {
        // System.out.println("***UPOV***Inside order instanceof OrderImpl:");
        // Have to cast down to something that can get arbitrary property values
        OrderImpl orderImpl = (OrderImpl) order;
        Repository pRepository = (Repository) Nucleus.getGlobalNucleus().resolveName("/atg/commerce/catalog/ProductCatalog");
        
        // System.out.println("***UPOV***orderImpl is:" + orderImpl.toString() );
        // System.out.println(pVariable + " = " + orderImpl.getPropertyValue(pVariable).toString());
        
        // Get an iterator over each commerceItemRelationship
        List items = mOrderManager.getAllCommerceItemRelationships(order);
        Iterator iter = items.iterator();
        
        // The accumulator that represents our value
        double qualifyingValue = 0.00;
        
        // System.out.println("***UPOV***Examining Order");
        
        // Examine each commerceItem relationship
        while (iter.hasNext()) {
          CommerceItemRelationship ciRel = (CommerceItemRelationship) iter.next();
          
          // Examine all commerce items
          CommerceItem thisItem = ciRel.getCommerceItem();
          
          // System.out.println("***UPOV***Examining commerceItem " + thisItem.getId() + " (catalogRefId = " + thisItem.getCatalogRefId() + ")");
          
          if (itemMatchesCriteria(thisItem, pCMOSSKU)) {
            // This commerce item is valid. Add it to our running total.
            
            // System.out.println("***UPOV***Match! itemMatchesCriteria  Including item in total.");
            // System.out.println("***UPOV*** thisItem is:"+thisItem.getId());
            
            NMCommerceItem nmci = (NMCommerceItem) thisItem;
            String prodId = (String) nmci.getPropertyValue("productId");
            
            // System.out.println("***UPOV*** prodId:"+prodId);
            RepositoryItem ri = (RepositoryItem) pRepository.getItem(prodId, "product");
            
            double rPrice = ((Double) ri.getPropertyValue("retailPrice")).doubleValue();
            
            // qualifyingValue += thisItem.getPriceInfo().getListPrice() * thisItem.getQuantity();
            // System.out.println("***UPOV***thisItem retailPrice:"+rPrice);
            // System.out.println("***UPOV***item qualifyingValue is:"+ thisItem.getPriceInfo().getAmount());
            // System.out.println("***UPOV***item quantity is:"+ thisItem.getQuantity());
            
            // qualifyingValue += thisItem.getPriceInfo().getAmount() * thisItem.getQuantity();
            qualifyingValue += rPrice * thisItem.getQuantity();
            // System.out.println("***UPOV***Total qualifyingValue is:" + qualifyingValue);
            
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

