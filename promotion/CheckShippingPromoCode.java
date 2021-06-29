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

import java.util.*;

import com.nm.collections.*;
import com.nm.utils.*;

/**
 * This action is called when a promo code is entered. It will iterate thru the promotion array maintained by the global component PromotionsArray and look for an effective promotion. If it finds one
 * that is effective then is will see if it is a promo code driven promotion and determine if free shipping or upgrade is qualified.
 * 
 * @author Todd Schultz
 * @version $Id: CheckShippingPromoCode.java 1.5 2009/12/18 13:18:02CST William P Shows (NMWPS) Exp $
 */

public class CheckShippingPromoCode extends ActionImpl {
  // -------------------------------------
  /** Class version string */
  public static final String CLASS_VERSION = "$Id: CheckShippingPromoCode.java 1.5 2009/12/18 13:18:02CST William P Shows (NMWPS) Exp  $";
  
  /** Parameter for the location of the ordermanager */
  public String ORDERMANAGER_PATH = "/atg/commerce/order/OrderManager";
  
  /** Parameter for which value to update */
  public static final String PROMO_CODE = "promo_code";// The CMOS required 'FREEBASESHIPPING' code
  
  /** The shopping cart component */
  public static final String SHOPPING_CART_PATH = "/atg/commerce/ShoppingCart";
  
  /** reference to the order manager object and order holder object */
  protected OrderManager mOrderManager = null;
  protected ComponentName mOrderHolderComponent = null;
  protected Boolean mAddPromoFlag = null;// true/false flag in scenario
  protected String mPromoCode = null;
  PromotionsArray thePromos = null;
  
  private boolean logDebug;
  private boolean logError = true;
  
  public void initialize(Map pParameters) throws ScenarioException {
    logDebug = getSystemSpecs().getCsrScenarioDebug();
    logDebug("CheckShippingPromoCode is Initializing");
    mOrderManager = (OrderManager) Nucleus.getGlobalNucleus().resolveName(ORDERMANAGER_PATH);
    
    if (mOrderManager == null) throw new ScenarioException(PromotionConstants.getStringResource(PromotionConstants.ORDER_MANAGER_NOT_FOUND));
    
    storeRequiredParameter(pParameters, PROMO_CODE, java.lang.String.class);// The CMOS required 'FREEBASESHIPPING' code
    
    mOrderHolderComponent = ComponentName.getComponentName(SHOPPING_CART_PATH);
    thePromos = (PromotionsArray) Nucleus.getGlobalNucleus().resolveName("/nm/collections/PromotionsArray");
    
  }
  
  protected void executeAction(ScenarioExecutionContext pContext) throws ScenarioException {
    logDebug("just hit checkShippingPromocode");
    if (mOrderManager == null) {
      throw new ScenarioException(PromotionConstants.getStringResource(PromotionConstants.ORDER_MANAGER_NOT_FOUND));
    }
    
   
    
    // boolean stopLoop = false;
    boolean itExists = false;
    
    try {
      Order order = getOrderToModify(pContext);
      
      // this check was put in to see if free shipping has already been awarded. if so then minimize the
      // iteration and don't loop thru everything again. we have decided to not do this for now so we
      // have flexibiltiy in the future if users request more than free shipping in tool.
      /*
       * if (order instanceof NMOrderImpl) { NMOrderImpl NMorderImpl = (NMOrderImpl) order; String promoTest = NMorderImpl.getActivatedPromoCode();
       * 
       * if (promoTest != null && promoTest.indexOf("FREEBASESHIPPING" + ",") != -1) { itExists = true; } }//end nmorderimpl check
       */
      
      // if (! itExists)
      // {
      try {
        Date mystartDate = Calendar.getInstance().getTime();
        Map<String, NMPromotion> promoArray = thePromos.getAllActivePromotions();
        Iterator<NMPromotion> iterator = promoArray.values().iterator();
        
        while (iterator.hasNext()) {
          Promotion temp = (Promotion) iterator.next();
            
            // ***************************************************************************
            
            if (temp.getType().trim().equals("2")) {
              String theCode = new String(temp.getPromoCodes().trim().toUpperCase());
              String theKey = new String(temp.getCode().trim().toUpperCase());
              String theName = new String(temp.getName().trim().toUpperCase());
              mPromoCode = "FREEBASESHIPPING";// The CMOS required 'FREEBASESHIPPING' code
              try {
                // Order order = getOrderToModify(pContext);
                Double junk = new Double(temp.getDollarQualifier());
                double promoDollarQual = junk.doubleValue();
                
                if (order != null) {
                  if (order.getPriceInfo().getAmount() >= promoDollarQual) {
                    if (order instanceof NMOrderImpl) {
                      NMOrderImpl orderImpl = (NMOrderImpl) order;
                      String orderPromoCode = orderImpl.getPromoCode();
                      if (orderPromoCode == null) {
                        orderPromoCode = "";
                      }
                      StringTokenizer myToken = new StringTokenizer(orderPromoCode, ",");
                      while (myToken.hasMoreTokens()) {
                        String testString = myToken.nextToken();
                        
                        if (testString.trim().toUpperCase().equals(theCode.trim().toUpperCase())) {
                          orderImpl.setActivatedPromoCode(mPromoCode);
                          orderImpl.setActivatedPromoCode(testString);
                          // orderImpl.setPromoName(theKey + " - " + theName);
                          // stopLoop = true;
                          // System.out.println("FREE SHIPPING TYPE 2 AWARDED for " + orderImpl.getPromoName());
                          // break;
                        }
                      }// end while
                      
                    }
                  }
                  // }
                } else {
                  throw new ScenarioException("Order to be modified is null");
                }
                
              } catch (Exception ce) {
                throw new ScenarioException(ce);
              }
              
            }// end type 2
            
            else if (temp.getType().trim().equals("3")) {
              
              mPromoCode = "FREEBASESHIPPING";// The CMOS required 'FREEBASESHIPPING' code
              try {
                // Order order = getOrderToModify(pContext);
                String theCode = new String(temp.getPromoCodes().trim().toUpperCase());
                String theKey = new String(temp.getCode().trim().toUpperCase());
                String theName = new String(temp.getName().trim().toUpperCase());
                
                if (order != null) {
                  
                  if (order instanceof NMOrderImpl) {
                    
                    NMOrderImpl orderImpl = (NMOrderImpl) order;
                    String orderPromoCode = orderImpl.getPromoCode();
                    if (orderPromoCode == null) {
                      orderPromoCode = "";
                    }
                    StringTokenizer myToken = new StringTokenizer(orderPromoCode, ",");
                    while (myToken.hasMoreTokens()) {
                      String testString = myToken.nextToken();
                      if (testString.trim().toUpperCase().equals(theCode.trim().toUpperCase())) {
                        orderImpl.setActivatedPromoCode(mPromoCode);
                        orderImpl.setActivatedPromoCode(testString);
                        // orderImpl.setPromoName(theKey + " - " + theName);
                        // stopLoop = true;
                        // System.out.println("FREE SHIPPING TYPE 3 AWARDED " + orderImpl.getPromoName());
                        // break;
                      }
                    }// end while
                    
                  }
                  
                } else {
                  throw new ScenarioException("Order to be modified is null");
                }
                
              } catch (Exception ce) {
                throw new ScenarioException(ce);
              }
              
            }// end type 3
            
            else if (temp.getType().trim().equals("4")) {
              
              mPromoCode = "UPGRADE2NDDAY";// The CMOS required 'UPGRADE' code
              try {
                // Order order = getOrderToModify(pContext);
                
                String theCode = new String(temp.getPromoCodes().trim().toUpperCase());
                String theKey = new String(temp.getCode().trim().toUpperCase());
                String theName = new String(temp.getName().trim().toUpperCase());
                if (order != null) {
                  
                  if (order instanceof NMOrderImpl) {
                    
                    NMOrderImpl orderImpl = (NMOrderImpl) order;
                    String orderPromoCode = orderImpl.getPromoCode();
                    if (orderPromoCode == null) {
                      orderPromoCode = "";
                    }
                    StringTokenizer myToken = new StringTokenizer(orderPromoCode, ",");
                    while (myToken.hasMoreTokens()) {
                      String testString = myToken.nextToken();
                      if (testString.trim().toUpperCase().equals(theCode.trim().toUpperCase())) {
                        orderImpl.setActivatedPromoCode(mPromoCode);
                        orderImpl.setActivatedPromoCode(testString);
                        // orderImpl.setPromoName(theKey + " - " + theName);
                        // stopLoop = true;
                        // System.out.println("FREE UPGRADE2NDDAY AWARDED " + orderImpl.getPromoName());
                        // break;
                      }
                    }// end while
                  }
                  
                } else {
                  throw new ScenarioException("Order to be modified is null");
                }
                
              } catch (Exception ce) {
                throw new ScenarioException(ce);
              }
              
            }// end type 4
            
            else if (temp.getType().trim().equals("5")) {
              
              mPromoCode = "UPGRADEOVERNIGHT";// The CMOS required 'UPGRADE' code
              try {
                // Order order = getOrderToModify(pContext);
                String theCode = new String(temp.getPromoCodes().trim().toUpperCase());
                String theKey = new String(temp.getCode().trim().toUpperCase());
                String theName = new String(temp.getName().trim().toUpperCase());
                
                if (order != null) {
                  
                  if (order instanceof NMOrderImpl) {
                    
                    NMOrderImpl orderImpl = (NMOrderImpl) order;
                    String orderPromoCode = orderImpl.getPromoCode();
                    if (orderPromoCode == null) {
                      orderPromoCode = "";
                    }
                    StringTokenizer myToken = new StringTokenizer(orderPromoCode, ",");
                    while (myToken.hasMoreTokens()) {
                      String testString = myToken.nextToken();
                      if (testString.trim().toUpperCase().equals(theCode.trim().toUpperCase())) {
                        orderImpl.setActivatedPromoCode(mPromoCode);
                        orderImpl.setActivatedPromoCode(testString);
                        // orderImpl.setPromoName(theKey + " - " + theName);
                        // stopLoop = true;
                        // System.out.println("FREE UPGRADEOVERNIGHT AWARDED " + orderImpl.getPromoName());
                        // break;
                      }
                    }// end while
                    
                  }
                  
                } else {
                  throw new ScenarioException("Order to be modified is null");
                }
                
              } catch (Exception ce) {
                throw new ScenarioException(ce);
              }
              
            }// end type 5
            
            // ***************************************************************************
            
            else if (temp.getType().trim().equals("7")) {
              
              boolean foundDept = false;
              String theDept = new String(temp.getDeptCodes().toUpperCase().trim());
              StringTokenizer deptToken = new StringTokenizer(theDept, ",");
              String theCode = new String(temp.getPromoCodes().toUpperCase().trim());
              String theKey = new String(temp.getCode().trim().toUpperCase());
              String theName = new String(temp.getName().trim().toUpperCase());
              mPromoCode = "FREEBASESHIPPING";// The CMOS required 'FREEBASESHIPPING' code
              
              try {
                
                // Order order = getOrderToModify(pContext);
                
                if (order != null) {
                  if (order instanceof OrderImpl) {
                    
                    // Have to cast down to something that can get arbitrary property values
                    OrderImpl orderImpl = (OrderImpl) order;
                    while (deptToken.hasMoreTokens()) {
                      String tokenCheck = deptToken.nextToken();
                      // Get an iterator over each commerceItemRelationship
                      List items = mOrderManager.getAllCommerceItemRelationships(order);
                      Iterator iter = items.iterator();
                      
                      // Examine each commerceItem relationship
                      while (iter.hasNext()) {
                        CommerceItemRelationship ciRel = (CommerceItemRelationship) iter.next();
                        // Examine all commerce items
                        CommerceItem thisItem = ciRel.getCommerceItem();
                        CommerceItemImpl thisItemImpl = (CommerceItemImpl) thisItem;
                        
                        RepositoryItem productItem = (RepositoryItem) thisItemImpl.getAuxiliaryData().getProductRef();
                        
                        String deptCode = (String) productItem.getPropertyValue("deptCode");// :deptCode:The product selected by the customer
                        
                        if (deptCode.trim().toUpperCase().equals(tokenCheck.trim().toUpperCase())) {
                          foundDept = true;
                          break;
                        }// end if dept matches
                      }// end while iter
                      if (foundDept) {
                        break;
                      }
                    }// end while tokens
                  }// end order instance of orderImpl
                  
                  if (order instanceof NMOrderImpl) {
                    
                    NMOrderImpl orderImpl = (NMOrderImpl) order;
                    String orderPromoCode = orderImpl.getPromoCode();
                    if (orderPromoCode == null) {
                      orderPromoCode = "";
                    }
                    StringTokenizer myToken = new StringTokenizer(orderPromoCode, ",");
                    while (myToken.hasMoreTokens()) {
                      String testString = myToken.nextToken();
                      if (testString.trim().toUpperCase().equals(theCode.trim().toUpperCase())) {
                        if (foundDept) {
                          orderImpl.setActivatedPromoCode(mPromoCode);
                          orderImpl.setActivatedPromoCode(testString);
                          // orderImpl.setPromoName(theKey + " - " + theName);
                          // stopLoop = true;
                          // System.out.println("FREE SHIPPING TYPE 7 AWARDED " + orderImpl.getPromoName());
                          // break;
                        }
                      }
                    }// end while
                    
                  }// end if NMOrderImpl
                  
                }// end if order null
                else {
                  throw new ScenarioException("Order to be modified is null");
                }
                
              } catch (Exception ce) {
                throw new ScenarioException(ce);
              }
              
            }// end type 7
            
            // ***************************************************************************
            
            else if (temp.getType().trim().equals("9")) {
              
              boolean foundDept = false;
              String theDept = new String(temp.getDeptCodes().trim().toUpperCase());
              StringTokenizer deptToken = new StringTokenizer(theDept, ",");
              String theCode = new String(temp.getPromoCodes().trim().toUpperCase());
              String theKey = new String(temp.getCode().trim().toUpperCase());
              String theName = new String(temp.getName().trim().toUpperCase());
              mPromoCode = "FREEBASESHIPPING";// The CMOS required 'FREEBASESHIPPING' code
              Double junk = new Double(temp.getDollarQualifier());
              double promoDollarQual = junk.doubleValue();
              double customersTotal = 0;
              
              try {
                
                // Order order = getOrderToModify(pContext);
                
                if (order != null) {
                  if (order instanceof OrderImpl) {
                    
                    while (deptToken.hasMoreElements()) {
                      String deptCheck = deptToken.nextToken();
                      // Have to cast down to something that can get arbitrary property values
                      OrderImpl orderImpl = (OrderImpl) order;
                      
                      // Get an iterator over each commerceItemRelationship
                      List items = mOrderManager.getAllCommerceItemRelationships(order);
                      Iterator iter = items.iterator();
                      
                      // Examine each commerceItem relationship
                      while (iter.hasNext()) {
                        CommerceItemRelationship ciRel = (CommerceItemRelationship) iter.next();
                        // Examine all commerce items
                        CommerceItem thisItem = ciRel.getCommerceItem();
                        CommerceItemImpl thisItemImpl = (CommerceItemImpl) thisItem;
                        
                        RepositoryItem productItem = (RepositoryItem) thisItemImpl.getAuxiliaryData().getProductRef();
                        
                        String deptCode = (String) productItem.getPropertyValue("deptCode");// :deptCode:The product selected by the customer
                        
                        if (deptCode.trim().toUpperCase().equals(deptCheck.trim().toUpperCase())) {
                          foundDept = true;
                          customersTotal = customersTotal + thisItem.getPriceInfo().getAmount();
                        }// end if dept matches
                      }// end while iterate thru commerce items
                    }// end loop of depts
                  }// end order instance of orderImpl
                  
                  if (order instanceof NMOrderImpl) {
                    
                    NMOrderImpl orderImpl = (NMOrderImpl) order;
                    String orderPromoCode = orderImpl.getPromoCode();
                    if (orderPromoCode == null) {
                      orderPromoCode = "";
                    }
                    StringTokenizer myToken = new StringTokenizer(orderPromoCode, ",");
                    while (myToken.hasMoreTokens()) {
                      String testString = myToken.nextToken();
                      if (testString.trim().toUpperCase().equals(theCode.trim().toUpperCase())) {
                        if (foundDept && (customersTotal >= promoDollarQual)) {
                          orderImpl.setActivatedPromoCode(mPromoCode);
                          orderImpl.setActivatedPromoCode(testString);
                          // orderImpl.setPromoName(theKey + " - " + theName);
                          // stopLoop = true;
                          // System.out.println("FREE SHIPPING TYPE 9 AWARDED " + orderImpl.getPromoName());
                          // break;
                        }
                      }
                    }// end while
                    
                  }// end if NMOrderImpl
                }// end if order null
                else {
                  throw new ScenarioException("Order to be modified is null");
                }
                
              } catch (Exception ce) {
                throw new ScenarioException(ce);
              }
              
            }// end type 9
            
            // ***************************************************************************
            
            else if (temp.getType().trim().equals("11")) {
              
              boolean foundDepiction = false;
              String theDepiction = new String(temp.getDepiction().trim().toUpperCase());
              StringTokenizer depictToken = new StringTokenizer(theDepiction, ",");
              String theCode = new String(temp.getPromoCodes().trim().toUpperCase());
              String theKey = new String(temp.getCode().trim().toUpperCase());
              String theName = new String(temp.getName().trim().toUpperCase());
              mPromoCode = "FREEBASESHIPPING";// The CMOS required 'FREEBASESHIPPING' code
              
              try {
                
                // Order order = getOrderToModify(pContext);
                
                if (order != null) {
                  if (order instanceof OrderImpl) {
                    
                    // Have to cast down to something that can get arbitrary property values
                    OrderImpl orderImpl = (OrderImpl) order;
                    
                    while (depictToken.hasMoreTokens()) {
                      String depictTest = depictToken.nextToken();
                      // Get an iterator over each commerceItemRelationship
                      List items = mOrderManager.getAllCommerceItemRelationships(order);
                      Iterator iter = items.iterator();
                      
                      // Examine each commerceItem relationship
                      while (iter.hasNext()) {
                        CommerceItemRelationship ciRel = (CommerceItemRelationship) iter.next();
                        // Examine all commerce items
                        CommerceItem thisItem = ciRel.getCommerceItem();
                        CommerceItemImpl thisItemImpl = (CommerceItemImpl) thisItem;
                        
                        RepositoryItem productItem = (RepositoryItem) thisItemImpl.getAuxiliaryData().getProductRef();
                        
                        String cmosItemCode = (String) productItem.getPropertyValue("cmosItemCode");// :cmosItemCode:The product selected by customer
                        String cmosDepiction = cmosItemCode.substring(0, 1);
                        
                        if (cmosDepiction.trim().toUpperCase().equals(depictTest.trim().toUpperCase())) {
                          foundDepiction = true;
                          break;
                        }// end if dept matches
                      }// end while commerce items
                      
                      if (foundDepiction) {
                        break;
                      }
                    }// end dept tokenizer
                  }// end order instance of orderImpl
                  
                  if (order instanceof NMOrderImpl) {
                    
                    NMOrderImpl orderImpl = (NMOrderImpl) order;
                    String orderPromoCode = orderImpl.getPromoCode();
                    if (orderPromoCode == null) {
                      orderPromoCode = "";
                    }
                    StringTokenizer myToken = new StringTokenizer(orderPromoCode, ",");
                    while (myToken.hasMoreTokens()) {
                      String testString = myToken.nextToken();
                      if (testString.trim().toUpperCase().equals(theCode.trim().toUpperCase())) {
                        if (foundDepiction) {
                          orderImpl.setActivatedPromoCode(mPromoCode);
                          orderImpl.setActivatedPromoCode(testString);
                          // orderImpl.setPromoName(theKey + " - " + theName);
                          // stopLoop = true;
                          // System.out.println("FREE SHIPPING TYPE 11 AWARDED " + orderImpl.getPromoName());
                          // break;
                        }
                      }
                    }// end while
                    
                  }// end if NMOrderImpl
                  
                }// end if order null
                else {
                  throw new ScenarioException("Order to be modified is null");
                }
                
              } catch (Exception ce) {
                throw new ScenarioException(ce);
              }
              
            }// end type 11
            
            // ***************************************************************************
            
            else if (temp.getType().trim().equals("13")) {
              
              boolean foundDepiction = false;
              String theDepiction = new String(temp.getDepiction().trim().toUpperCase());
              StringTokenizer depictToken = new StringTokenizer(theDepiction, ",");
              String theCode = new String(temp.getPromoCodes().trim().toUpperCase());
              String theKey = new String(temp.getCode().trim().toUpperCase());
              String theName = new String(temp.getName().trim().toUpperCase());
              mPromoCode = "FREEBASESHIPPING";// The CMOS required 'FREEBASESHIPPING' code
              Double junk = new Double(temp.getDollarQualifier());
              double promoDollarQual = junk.doubleValue();
              double customersTotal = 0;
              
              try {
                
                // Order order = getOrderToModify(pContext);
                
                if (order != null) {
                  if (order instanceof OrderImpl) {
                    while (depictToken.hasMoreTokens()) {
                      String depictTest = depictToken.nextToken();
                      
                      // Have to cast down to something that can get arbitrary property values
                      OrderImpl orderImpl = (OrderImpl) order;
                      
                      // Get an iterator over each commerceItemRelationship
                      List items = mOrderManager.getAllCommerceItemRelationships(order);
                      Iterator iter = items.iterator();
                      
                      // Examine each commerceItem relationship
                      while (iter.hasNext()) {
                        CommerceItemRelationship ciRel = (CommerceItemRelationship) iter.next();
                        // Examine all commerce items
                        CommerceItem thisItem = ciRel.getCommerceItem();
                        CommerceItemImpl thisItemImpl = (CommerceItemImpl) thisItem;
                        
                        RepositoryItem productItem = (RepositoryItem) thisItemImpl.getAuxiliaryData().getProductRef();
                        
                        String cmosItemCode = (String) productItem.getPropertyValue("cmosItemCode");// :cmosItemCode:The product selected by customer
                        String cmosDepiction = cmosItemCode.substring(0, 1);
                        
                        if (cmosDepiction.trim().toUpperCase().equals(depictTest.trim().toUpperCase())) {
                          foundDepiction = true;
                          customersTotal = customersTotal + thisItem.getPriceInfo().getAmount();
                          // break;
                        }// end if dept matches
                      }// end while
                    }// end token of dept
                  }// end order instance of orderImpl
                  
                  if (order instanceof NMOrderImpl) {
                    
                    NMOrderImpl orderImpl = (NMOrderImpl) order;
                    String orderPromoCode = orderImpl.getPromoCode();
                    if (orderPromoCode == null) {
                      orderPromoCode = "";
                    }
                    StringTokenizer myToken = new StringTokenizer(orderPromoCode, ",");
                    while (myToken.hasMoreTokens()) {
                      String testString = myToken.nextToken();
                      if (testString.trim().toUpperCase().equals(theCode.trim().toUpperCase())) {
                        if (foundDepiction && (customersTotal >= promoDollarQual)) {
                          orderImpl.setActivatedPromoCode(mPromoCode);
                          orderImpl.setActivatedPromoCode(testString);
                          // orderImpl.setPromoName(theKey + " - " + theName);
                          // stopLoop = true;
                          // System.out.println("FREE SHIPPING TYPE 13 AWARDED " + orderImpl.getPromoName());
                          // break;
                        }
                      }
                    }// end while
                    
                  }// end if NMOrderImpl
                }// end if order null
                else {
                  throw new ScenarioException("Order to be modified is null");
                }
                
              } catch (Exception ce) {
                throw new ScenarioException(ce);
              }
              
            }// end type 13
          
          /*
           * if (stopLoop) { System.out.println("BREAKING out of LOOP"); break; }
           */
        } // end for
        
      } catch (Exception e) {
        System.out.println(e);
        e.printStackTrace();
      }
      
      // }//end if exists
      
    } catch (Exception e) {
      System.out.println(e);
      e.printStackTrace();
    }
    
  }
  
  /**
   * This method returns the order which should be modified.
   * 
   * It would be better to grab the order in the context, but as it is that's not going to happen given the way this action is typically used. That could be fixed, given time.
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
    String profileId = ((MutableRepositoryItem) pContext.getProfile()).getRepositoryId();
    // System.out.println("profileId from UpdateACtivatedPC is:"+profileId);
    
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
  
  private SystemSpecs getSystemSpecs() {
    return (SystemSpecs) Nucleus.getGlobalNucleus().resolveName("/nm/utils/SystemSpecs");
  }
  
  public void setLoggingDebug(boolean bin) {
    logDebug = bin;
  }
  
  public boolean isLoggingDebug() {
    return logDebug;
  }
  
  public void setLoggingError(boolean bin) {
    logError = bin;
  }
  
  public boolean isLoggingError() {
    return logError;
  }
  
  private void logError(String sin) {
    if (isLoggingError()) {
      Nucleus.getGlobalNucleus().logError("checkShippingPromoCode: " + sin);
    }
  }
  
  private void logDebug(String sin) {
    if (isLoggingDebug()) {
      Nucleus.getGlobalNucleus().logDebug("checkShippingPromoCode: " + sin);
    }
  }
  
}
