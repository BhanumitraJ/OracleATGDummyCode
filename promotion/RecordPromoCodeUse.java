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

import java.util.*;
import atg.service.util.CurrentDate;

import javax.servlet.ServletException;
import java.io.IOException;

// ** This scenario action will record valid promocodes used
// by the customer. We will keep a list of all valid codes
// for future reference,for instance, 1 time only use codes.
// These will be saved in the tables:NM_USER_PROMOCODES and NM_PROMOCODE_LIST
// **
public class RecordPromoCodeUse extends ActionImpl {
  /** Class version string */
  public static final String CLASS_VERSION = "$Id: RecordPromoCodeUse.java 1.2 2008/04/29 13:59:34CDT nmmc5 Exp  $";
  
  /** Parameter for the location of the ordermanager */
  public String ORDERMANAGER_PATH = "/atg/commerce/order/OrderManager";
  public static final String SHOPPING_CART_PATH = "/atg/commerce/ShoppingCart";
  
  /** Parameter for if we need to record number of times promocode is used */
  public static final String RECORD_PROMOCODE_USE = "record_promoCode_use";
  
  /** Parameter for the promocode is to be recorded */
  public static final String PROMOCODE_TO_RECORD = "promocode_to_record";
  
  /** reference to the order manager object and order holder object */
  protected OrderManager mOrderManager = null;
  protected ComponentName mOrderHolderComponent = null;
  protected Boolean recordPromoCodeUse = null; // flag for recording number of times promocode is used :true/false
  public String promocodeToRecord = "";// promocode (set in scenario) to record for customer. Should match the qualifiing code.
  public String RepositoryId = "";
  public String loginID = "";
  public String Promosused = "";
  
  public void initialize(Map pParameters) throws ScenarioException {
    mOrderManager = (OrderManager) Nucleus.getGlobalNucleus().resolveName(ORDERMANAGER_PATH);
    if (mOrderManager == null) throw new ScenarioException(PromotionConstants.getStringResource(PromotionConstants.ORDER_MANAGER_NOT_FOUND));
    storeRequiredParameter(pParameters, RECORD_PROMOCODE_USE, java.lang.Boolean.class);// flag set in Scenario for use checking
    storeRequiredParameter(pParameters, PROMOCODE_TO_RECORD, java.lang.String.class);// code to be stored
    mOrderHolderComponent = ComponentName.getComponentName(SHOPPING_CART_PATH);
  }
  
  protected void executeAction(ScenarioExecutionContext pContext) throws ScenarioException {
    String OrderPromocode = "";
    
    if (recordPromoCodeUse == null) {
      synchronized (RECORD_PROMOCODE_USE) {
        recordPromoCodeUse = (Boolean) getParameterValue(RECORD_PROMOCODE_USE, pContext);
        // System.out.println("*RPC*RPC*RPC*"+ RECORD_PROMOCODE_USE + " parameter is " + recordPromoCodeUse);
      }
    }
    if (recordPromoCodeUse == null) throw new ScenarioException("*RPC*RPC*RPC*RECORD_PROMOCODE_USE was not specified");
    
    if (promocodeToRecord == "") {
      synchronized (PROMOCODE_TO_RECORD) {
        promocodeToRecord = (String) getParameterValue(PROMOCODE_TO_RECORD, pContext);
        // System.out.println("*RPC*RPC*RPC*"+ PROMOCODE_TO_RECORD + " parameter is " + promocodeToRecord);
      }
    }
    if (promocodeToRecord == "") throw new ScenarioException("*RPC*RPC*RPC*PROMOCODE_TO_RECORD was not specified");
    
    RepositoryId = ((MutableRepositoryItem) pContext.getProfile()).getRepositoryId();
    // System.out.println("*RPC*RPC*RPC****RepId is:"+RepositoryId);
    loginID = (String) ((MutableRepositoryItem) pContext.getProfile()).getPropertyValue("login");
    // System.out.println("*RPC*RPC*RPC****loginID is:"+loginID);
    // System.out.println("*RPC*RPC*RPC****promocodeToRecord is:"+promocodeToRecord);
    
    // This is were we will record the promocode use in the tables.
    // ************************************************************
    // DATE FORMAT NEEDED: 2002-07-25 00:00:00
    // java.sql.Date
    
    System.out.println("*RPC*RPC*RPC***sdate is getting set:");
    Calendar sdate = Calendar.getInstance();
    int year = sdate.get(Calendar.YEAR);
    int month = sdate.get(Calendar.MONTH);
    int day = sdate.get(Calendar.DATE);
    boolean codefound = false;
    boolean promoAdded = false;
    boolean promoUpdated = false;
    
    // System.out.println("***sdate is :"+year+"-"+month+"-"+day);
    // System.out.println("*RPC*RPC*RPC***date is getting set:");
    
    // Get a list of Promocodes used by user.
    // System.out.println("*RPC*RPC*RPC****making repositories");
    
    MutableRepositoryItem userPCodeItem = ((MutableRepositoryItem) pContext.getProfile());
    // System.out.println("*RPC*RPC*RPC****making List");
    List usedPromos = (List) userPCodeItem.getPropertyValue("PromosUsed");
    // System.out.println("*RPC*RPC*RPC****List made");
    
    int count = usedPromos.size();
    
    // 1. List of codes does not exist. Will be added.
    if (usedPromos.isEmpty()) {
      // System.out.println("*RPC*RPC*RPC****usedPromos is empty");
      createRepositoryItem(userPCodeItem, sdate, usedPromos);
      promoAdded = true;
    }
    
    // 2. List of codes exists.
    // Now we start to go through the list of codes already used.
    // The index(j) on the List will correspond with the codes used in the repository
    for (int j = 0; j < count; j++) {
      // System.out.println("*RPC*RPC*RPC****items in list");
      // System.out.println("*RPC*RPC*RPC****"+j+usedPromos.get(j));
      // System.out.println("*RPC*RPC*RPC****"+((MutableRepositoryItem)usedPromos.get(j)).getPropertyValue("PROMOCODE") );
      
      String pcode = (String) ((MutableRepositoryItem) usedPromos.get(j)).getPropertyValue("PROMOCODE");
      
      // System.out.println("*RPC*RPC*RPC****pcode is:"+pcode);
      // System.out.println("*RPC*RPC*RPC****promocodeToRecord is:"+promocodeToRecord);
      
      // if one in List matches the one entered, we have a match.
      // Code has been found.
      // Index(j) of the List will correspond with the index of Repository
      if (promocodeToRecord.equals(pcode) && !promoUpdated) {
        MutableRepositoryItem itemFound = (MutableRepositoryItem) usedPromos.get(j);
        // System.out.println("*RPC*RPC*RPC****Code Found!");
        codefound = true;
        // ******************************
        // Where we will increment the use of code
        Integer nt = (Integer) itemFound.getPropertyValue("NUM_TIMES");
        int ntt = nt.intValue();
        
        // System.out.println("*RPC*RPC*RPC****ntt before increment is:"+ntt);
        ntt++;
        // System.out.println("*RPC*RPC*RPC****ntt after increment is:"+ntt);
        // System.out.println("*RPC*RPC*RPC****setPropertyValue");
        
        try {
          
          // System.out.println("*RPC*RPC*RPC****MutableRepositoryItem newPCodeRep next");
          MutableRepository newPCodeRep = ((MutableRepository) userPCodeItem.getRepository());
          
          // System.out.println("*RPC*RPC*RPC****MutableRepositoryItem updateCountRI getItemForUpdate next");
          MutableRepositoryItem updateCountRI = (MutableRepositoryItem) newPCodeRep.getItemForUpdate(itemFound.getRepositoryId(), "promocode_list");
          
          updateCountRI.setPropertyValue("NUM_TIMES", new Integer(ntt));
          // System.out.println("*RPC*RPC*RPC****newPCodeRep.updateItem next");
          newPCodeRep.updateItem(updateCountRI);
          promoUpdated = true;
        }
        
        catch (RepositoryException re) {
          throw new ScenarioException(re);
        }
        
      }// end if
      
    }// end for
    
    // 3. List exists, but it does not have this code in it. Will be added.
    if (!codefound && !promoAdded) {
      // System.out.println("*RPC*RPC*RPC****!codefound ");
      // System.out.println("*RPC*RPC*RPC****createRepositoryItem");
      createRepositoryItem(userPCodeItem, sdate, usedPromos);
    }
    
  }// end of executeAction()
  
  // mri is MRItem in context, cDate is date, lPCodes is list of codes in context
  public void createRepositoryItem(MutableRepositoryItem mri, Calendar cDate, List lPCodes) throws ScenarioException {
    // ******************************
    // PC Not Found in List
    // ADD a New one to the tables
    // System.out.println("*RPC*RPC*RPC****Promocode NOT detected!");
    // System.out.println("*RPC*RPC*RPC****Creating MRep newPCodeRep");
    MutableRepository newPCodeRep = ((MutableRepository) mri.getRepository());
    
    Integer numtimes = new Integer(1);
    Integer maxuse = new Integer(1);
    
    // System.out.println("*RPC*RPC*RPC****Creating MRepItem newPC");
    try {
      MutableRepositoryItem newPC = newPCodeRep.createItem("promocode_list");
      // System.out.println("*RPC*RPC*RPC****newPC made");
      // System.out.println("*RPC*RPC*RPC****Loading properties with values");
      newPC.setPropertyValue("PROMOCODE", promocodeToRecord);
      newPC.setPropertyValue("NUM_TIMES", numtimes);
      newPC.setPropertyValue("MAX_USE", maxuse);
      newPC.setPropertyValue("DATE_USED", cDate.getTime());
      // add MRitem newPC to List lPCodes
      lPCodes.add(newPC);
      // System.out.println("*RPC*RPC*RPC****newPCodeRep.addItem next");
      // add List newPC to MRepository
      newPCodeRep.addItem(newPC);
      // System.out.println("*RPC*RPC*RPC****userPCodeItem.setPropertyValue next");
      mri.setPropertyValue("PromosUsed", lPCodes);
      // System.out.println("*RPC*RPC*RPC****userPCodeItem.setPropertyValue Successful");
    } catch (RepositoryException re) {
      throw new ScenarioException(re);
    }
    
  }
  
  public Order getOrderInfo(ScenarioExecutionContext pContext) throws CommerceException, RepositoryException {
    
    DynamoHttpServletRequest request = pContext.getRequest();
    
    // If we are executing this action in the context of a session then we want to get a hold
    // of the OrderHolder and use the Order within it as the Order that we want to get
    // information from.
    if (request != null) {
      
      OrderHolder oh = (OrderHolder) request.resolveName(mOrderHolderComponent);
      // System.out.println("*RPC*RPC*RPC****Getting Current Order***"+oh.getCurrent());
      // System.out.println("*RPC*RPC*RPC****OrderHolder***"+oh);
      // System.out.println("*RPC*RPC*RPC****Getting the Last Order(current)***");
      // System.out.println("*RPC*RPC*RPC"+oh.getLast());
      
      // return oh.getLast();
      return oh.getCurrent();
    }
    
    return null;
  }
  
}// end of class
