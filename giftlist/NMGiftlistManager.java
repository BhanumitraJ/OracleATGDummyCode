package com.nm.commerce.giftlist;

import java.io.IOException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.transaction.SystemException;

import atg.commerce.CommerceException;
import atg.commerce.gifts.GiftlistManager;
import atg.commerce.gifts.GiftlistTools;
import atg.dtm.TransactionDemarcation;
import atg.dtm.TransactionDemarcationException;
import atg.nucleus.naming.ComponentName;
import atg.repository.MutableRepository;
import atg.repository.MutableRepositoryItem;
import atg.repository.Repository;
import atg.repository.RepositoryException;
import atg.repository.RepositoryItem;
import atg.repository.RepositoryView;
import atg.repository.rql.RqlStatement;
import atg.security.LoginUserAuthority;
import atg.security.Persona;
import atg.security.User;
import atg.service.lockmanager.ClientLockManager;
import atg.service.lockmanager.DeadlockException;
import atg.service.lockmanager.LockManagerException;
import atg.service.lockmanager.LockReleaser;
import atg.servlet.DynamoHttpServletRequest;

import com.nm.utils.RegistryUtils;
import com.nm.utils.ValidateSku;
import com.nm.utils.datasource.NMTransactionDemarcation;

public class NMGiftlistManager extends GiftlistManager {
  
  private static final int STORE_ITEM = 1;
  private static final int WEB_ITEM = 0;
  
  // attempts to add a giftItem to a giftlist, it will increase item quantity if the sku/productId combo already exist on the registry
  public boolean addItemToGiftlistCombine(String giftlistId, String giftItemId) throws ServletException, IOException, CommerceException {
    
    if (isLoggingDebug()) {
      logDebug("....entered addItemToGiftlistCombine(String giftlistId, String giftItemId)");
      logDebug("giftlistId-->" + giftlistId + "<----giftItemId---->");
    }
    try {
      RepositoryItem giftlistItem = getGiftlist(giftlistId);
      MutableRepositoryItem giftItemRI = (MutableRepositoryItem) getGiftitem(giftItemId);
      String skuId = (String) giftItemRI.getPropertyValue("catalogRefId");
      String productId = (String) giftItemRI.getPropertyValue("productId");
      long quantity = ((Long) giftItemRI.getPropertyValue("quantityDesired")).longValue();
      
      if (giftlistId == null || giftItemId == null || skuId == null || productId == null || quantity == 0) return false;
      
      String giftItemMatchId = lookupGiftItem(giftlistId, skuId, productId, (Map) giftItemRI.getPropertyValue("specialInstCodes"));
      
      if (isLoggingDebug()) {
        logDebug("gift Id lookup results--->" + giftItemMatchId + "<----");
      }
      
      if (giftItemMatchId != null && !giftItemMatchId.trim().equals("")) {
        increaseGiftlistItemQuantityDesired(giftlistId, giftItemMatchId, quantity);
        if (isLoggingDebug()) logDebug("increasing quantity of current gift item by--->" + quantity + "<----");
        return true;
      } else {
        if (isLoggingDebug()) {
          logDebug("adding gift item repository and then to the gift list");
        }
        giftItemRI.setPropertyValue("quantityPurchased", new Long(0));
        getGiftlistTools().addItem(giftItemRI);
        addItemToGiftlist(giftlistId, giftItemId);
        
        ((MutableRepositoryItem) giftlistItem).setPropertyValue("lastModifiedDate", Calendar.getInstance().getTime());
        
        return true;
      }
      
    } catch (RepositoryException exc) {
      throw new CommerceException(exc);
    }
  }
  
  public String lookupGiftItem(String giftlistId, String skuId, String productId) {
    return lookupGiftItem(giftlistId, skuId, productId, null);
  }
  
  // returns the first gift-item id that matches the passed in giftlistId, skuId and productId
  // pass giftItemId if your lookup wants to match on SICodes from another gift item
  public String lookupGiftItem(String giftlistId, String skuId, String productId, Map siCodesMap) {
    
    if (siCodesMap == null) siCodesMap = new HashMap();
    
    if (isLoggingDebug()) logDebug("Inside lookupGiftItem: giftlist id = " + giftlistId + ", skuId = " + skuId + ", productId = " + productId + ", siCodesMap = " + siCodesMap);
    
    List giftlistItems = getGiftlistItems(giftlistId);
    
    Iterator itemsIterator = giftlistItems.iterator();
    Repository rep = getGiftlistTools().getGiftlistRepository();
    
    while (itemsIterator.hasNext()) {
      RepositoryItem giftItemRI = (RepositoryItem) itemsIterator.next();
      
      if (giftItemRI.getPropertyValue(getGiftlistTools().getCatalogRefIdProperty()).equals(skuId) && giftItemRI.getPropertyValue(getGiftlistTools().getProductIdProperty()).equals(productId)) {
        
        if (!siCodesMap.isEmpty()) {
          
          Map siCodesGiftItemMap = null;
          siCodesGiftItemMap = (Map) giftItemRI.getPropertyValue("specialInstCodes");
          
          if (isLoggingDebug()) {
            logDebug("siCodesMap-->" + siCodesMap + "<---");
            logDebug("siCodesGiftItemMap-->" + siCodesGiftItemMap + "<---");
          }
          
          if (giftItemRI != null && siCodesGiftItemMap != null && siCodesGiftItemMap.equals(siCodesMap)) {
            return (String) giftItemRI.getPropertyValue("id");
          }
          
        } else {
          return (String) giftItemRI.getPropertyValue("id");
        }
      }
    }
    
    return null;
  }
  
  public RepositoryItem getGiftListTypeRI(String giftListType) {
    RepositoryItem riItem = null;
    try {
      Repository rep = getGiftlistTools().getGiftlistRepository();
      RepositoryView m_view = rep.getView("giftlist-type");
      RqlStatement statement = RqlStatement.parseRqlStatement("giftlistType = ?0");
      
      Object params[] = new Object[1];
      params[0] = giftListType.trim();
      RepositoryItem riArray[] = statement.executeQuery(m_view, params);
      
      if (riArray[0] != null) {
        riItem = (RepositoryItem) riArray[0];
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    return riItem;
  }
  
  /**
   * Name of Method: getVCategoryId
   * 
   * @param String
   *          productId
   * @param String
   *          giftlistId
   * @return String virtualCategory
   * 
   *         Returns a virtual category for a given product Id and gift list parameters
   */
  public String getVCategoryId(String productId, String giftlistId) {
    
    RepositoryItem riItem = null;
    try {
      Repository repository = (Repository) resolveName("/atg/commerce/catalog/ProductCatalog");
      RepositoryItem productRI = repository.getItem(productId, "product");
      
      if (productRI != null) {
        String deptCode = (String) productRI.getPropertyValue("deptCode");
        RepositoryItem giftlistRI = getGiftlist(giftlistId);
        RepositoryItem giftlistType = getGiftListTypeRI(((RepositoryItem) giftlistRI.getPropertyValue("giftlistType")).getRepositoryId());
        List vCategories = (List) giftlistType.getPropertyValue("vCategories");
        
        if (vCategories != null) {
          Iterator vCatIterator = vCategories.iterator();
          while (vCatIterator.hasNext()) {
            RepositoryItem vCategory = (RepositoryItem) vCatIterator.next();
            List vCatDeptCodes = (List) vCategory.getPropertyValue("deptCodes");
            if (vCatDeptCodes != null) {
              Iterator vCatDeptCodesIterator = vCatDeptCodes.iterator();
              while (vCatDeptCodesIterator.hasNext()) {
                String vCatDeptCode = (String) vCatDeptCodesIterator.next();
                if (vCatDeptCode != null) {
                  if (vCatDeptCode.equalsIgnoreCase(deptCode.trim())) {
                    return vCategory.getRepositoryId();
                  }
                }
              }
            }
          }
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    return GiftItem.DEFAULT_VCAT_ID;
  }
  
  /**
   * Create a gift list event repository item. Utilizes the MutableRepository createItem method to create the data source. The data source is then placed in the GiftlistEvent wrapper object which
   * contains the set/get methods that correspond to the data represented by the gift list event.
   * 
   * @return Gift list event object - If the object is null, the operation failed
   */
  private GiftlistEvent createGiftlistEvent() {
    GiftlistEvent gLE = null;
    
    // Create the gift list event item
    try {
      MutableRepository mRep = (MutableRepository) getGiftlistTools().getGiftlistRepository();
      if (mRep != null) {
        MutableRepositoryItem mRI = mRep.createItem("giftlist-event");
        gLE = new GiftlistEvent(mRI);
      }
    }
    // Catch any repository exceptions that are thrown from creating the item
    catch (Exception e) {
      gLE = null;
    }
    
    // Return the gift list event object
    return (gLE);
  }
  
  public boolean addGiftListEventToGiftRegistry(String eventType, String eventData, String giftListId) {
    try {
      MutableRepository giftlistRepository = (MutableRepository) getGiftlistTools().getGiftlistRepository();
      MutableRepositoryItem giftItemEventRI = giftlistRepository.createItem("giftlist-event");
      
      giftItemEventRI.setPropertyValue("eventType", eventType);
      giftItemEventRI.setPropertyValue("data", eventData);
      giftlistRepository.addItem(giftItemEventRI);
      
      RepositoryItem giftListRI = getGiftlist(giftListId);
      List events = (List) giftListRI.getPropertyValue("events");
      
      events.add(giftItemEventRI);
      
      return true;
    } catch (Exception e) {
      return false;
    }
  }
  
  /*
   * private VirtualCategories bridalVirtualCats = new VirtualCategories();
   * 
   * public String getBridalVCategoryId (String productId,String giftlistId, boolean isStore) {
   * 
   * String retValue = GiftItem.DEFAULT_VCAT_ID; if (isStore) return retValue; RepositoryItem riItem = null; RepositoryItem productRI = null; String deptCode = null; Repository repository =
   * (Repository)resolveName("/atg/commerce/catalog/ProductCatalog"); boolean testVal = bridalVirtualCats.isCatInit(); if ( mBridalRootCat == null) return retValue;
   * 
   * if (!testVal){ initBridalCats(getBridalRootCat(), repository); }
   * 
   * try{ if (repository != null) productRI = repository.getItem(productId,"product"); if (productRI != null ) deptCode = (String)productRI.getPropertyValue("deptCode"); if (deptCode != null )
   * retValue = bridalVirtualCats.getVCategory(deptCode); } catch( Exception e ) { e.printStackTrace(); } return retValue; }
   */
  
  public long getStoreNumber(DynamoHttpServletRequest pRequest) {
    Repository storeEmployeeRep = (Repository) pRequest.resolveName("/atg/dynamo/security/AdminSqlRepository");
    RegistryUtils ru = (RegistryUtils) pRequest.resolveName("/nm/utils/RegistryUtils");
    LoginUserAuthority lua = (LoginUserAuthority) pRequest.resolveName("/atg/dynamo/security/AdminUserAuthority");
    RepositoryItem thisAccount = null;
    String isStoreView = ru.getStoreView();
    long storeNum = 0;
    String storeNumStr = "";
    
    if (isStoreView.equalsIgnoreCase("erots")) // -- in store view, so find store number
    {
      // get the current CSR/bridal consultant user Id and add it here.
      User currentUser = getCurrentUser(pRequest);
      Persona lPersona = currentUser.getPrimaryPersona(lua);
      String userName = "";
      if (lPersona != null) userName = lPersona.getName();
      
      try {
        thisAccount = (RepositoryItem) storeEmployeeRep.getItem(userName, "account");
        storeNumStr = (String) thisAccount.getPropertyValue("storeNumber"); // format e.g.: 20, where we require the last 2 chars, i.e. 20 as the store number
        storeNumStr = storeNumStr.substring(storeNumStr.length() - 2, storeNumStr.length());
        storeNum = Long.parseLong(storeNumStr);
      } catch (RepositoryException e) {
        e.printStackTrace();
      }
      
    } else // -- in web view, therefore store number = web's store number
    {
      storeNumStr = getWebStoreNumber();
      System.out.println("storeNumStr -- from getStoreNumber(): " + storeNumStr);
      storeNum = Long.parseLong(storeNumStr);
    }
    
    System.out.println("storeNum: " + storeNum);
    return storeNum;
  }
  
  public long getSequenceNumberAndSetPrefix(long pStoreNumber, DynamoHttpServletRequest pRequest) {
    TransactionDemarcation td = new NMTransactionDemarcation();
    long seqNum = 0;
    long newSeqNum = 0;
    
    try {
      try {
        MutableRepository storeInfoRep = (MutableRepository) pRequest.resolveName("/nm/xml/StoreInfoRepository");
        RepositoryView view = storeInfoRep.getView("storeLocation");
        RepositoryItem[] items = null;
        String thisStoreRepId = "";
        // String statementString = "(id CONTAINS ?0 and storeType STARTS WITH IGNORECASE ?1)";
        String statementString = "(id CONTAINS ?0)";
        
        RqlStatement rqlStatement = RqlStatement.parseRqlStatement(statementString);
        
        // Object params[] = new Object[2];
        // params[0] = String.valueOf(storeNumber);
        // params[1] = "standard";
        Object params[] = new Object[1];
        params[0] = String.valueOf(pStoreNumber);
        
        items = (RepositoryItem[]) rqlStatement.executeQuery(view, params);
        
        if (items != null && items.length > 0) { // -- expecting only 1 result as store numbers are unique values
          thisStoreRepId = items[0].getRepositoryId();
        } else {
          System.out.println("Could not find any items");
          return seqNum;
        }
        
        // acquire the lock and get the desired item
        GiftlistTools tools = getGiftlistTools();
        td.begin(getGiftlistTools().getTransactionManager(), 3);
        ClientLockManager clm = getGiftlistTools().getClientLockManager();
        clm.acquireWriteLock(thisStoreRepId);
        LockReleaser lr = new LockReleaser(clm, tools.getTransactionManager().getTransaction(), true);
        lr.addWriteLock(thisStoreRepId);
        MutableRepositoryItem thisStoreInfo = storeInfoRep.getItemForUpdate(thisStoreRepId, "storeLocation");
        
        // setting prefix value
        String prefixfromdb = (String) thisStoreInfo.getPropertyValue("prefix");
        System.out.println("prefixfromdb: " + prefixfromdb);
        setPrefix(prefixfromdb);
        
        // get old sequence number, keep it; also, modify it, and set new value
        // perform the tasks as follow, depending on the values of storeNumber and seqNumber
        Long seqNumLong = (Long) thisStoreInfo.getPropertyValue("seqNumber");
        if (seqNumLong != null) {
          if (seqNumLong.longValue() == 99999) {
            // for registry being created via web
            String prefixVal = getPrefix();
            if (prefixVal.equals("99")) {
              if (isLoggingError()) logError("Cannot create registry ids after creation of this registry. Quota exceeded.");
              seqNum = seqNumLong.longValue();
            } else {
              seqNum = seqNumLong.longValue();
              
              // increment the store number prefix by 1...
              long prefix = Long.parseLong(prefixVal) + 1;
              prefixVal = new Long(prefix).toString();
              if (prefix < 10) prefixVal = "0" + prefixVal;
              
              thisStoreInfo.setPropertyValue("prefix", prefixVal);
              
              // ... and reset the sequence number to 1
              thisStoreInfo.setPropertyValue("seqNumber", new Long(1));
              
              storeInfoRep.updateItem(thisStoreInfo);
            }
          } else {
            // for registry being created via store or web, and if the quota is not being exceeded
            seqNum = seqNumLong.longValue();
            newSeqNum = seqNum + 1;
            thisStoreInfo.setPropertyValue("seqNumber", new Long(newSeqNum));
            
            storeInfoRep.updateItem(thisStoreInfo);
          }
        }
        System.out.println("seqNum: " + seqNum);
        return seqNum;
      } catch (RepositoryException r) {
        System.out.println("In RE");
        if (isLoggingError()) logError(r);
        return seqNum;
      } catch (TransactionDemarcationException tde) {
        System.out.println("In TDE");
        if (isLoggingError()) logError(tde);
        return seqNum;
      } catch (DeadlockException d) {
        System.out.println("In DE");
        if (isLoggingError()) logError(d);
        return seqNum;
      } catch (LockManagerException lme) {
        System.out.println("In LME");
        if (isLoggingError()) logError(lme);
        return seqNum;
      } catch (SystemException s) {
        System.out.println("In SE");
        if (isLoggingError()) logError(s);
      }
      return seqNum;
    } finally {
      try {
        td.end();
      } catch (TransactionDemarcationException tde) {
        System.out.println("In TDE within finally");
        if (isLoggingError()) logError(tde);
        return seqNum;
      }
    }
  }
  
  /*****************************************************************************
   * BREAKDOWN OF A GIFT REGISTRY NUMBER -- example 400026000372 -- Positions 1-2 are either 40 or 41 (41 for converted registries from old mainframe system, 40 for new registries) -- Positions 3-4
   * are the prefix values, in this example 00 (for web) -- Positions 5-6 are the store number, in this example 26 (for web) -- Positions 3-6 are the store number, in this example 0026 (for stores) --
   * Positions 7-11 are the sequence number within store, in this example 00037 -- Position 12 is a check digit
   ******************************************************************************/
  public long calcCDigitAndRegId(long pStoreNumber, long pSequence, String pPrefix) {
    long theSum = 0;
    long firstDigit = 4; // first digit is always a 4 for gift registry
    long secondDigit = 0; // second digit is always a 0 for creating a new gift registry
    long[] entireNumber = new long[11]; // initialized with 11 zeroes by default. this represents the entire number (registry id) without the check digit.
    int index = 10; // position of last index filled
    
    long lLongNumber = pSequence;
    while (lLongNumber > 0) {
      entireNumber[index--] = lLongNumber % 10;
      lLongNumber = lLongNumber / 10;
    }
    index = 5;
    
    // Set the Store Number.
    entireNumber[index--] = pStoreNumber % 10; // gets the units place of the number
    entireNumber[index--] = pStoreNumber / 10; // gets the 10's place of store number
    
    // Set the prefix.
    entireNumber[index--] = Long.parseLong(pPrefix) % 10; // Gets the second Prefix into the array
    entireNumber[index--] = Long.parseLong(pPrefix) / 10; // Gets the first Prefix into the array
    
    entireNumber[index--] = secondDigit; // placing the second digit
    entireNumber[index--] = firstDigit; // placing the first digit
    
    // printing out entireNum[], which is the combo of '40' + storeNumber + sequence
    String finalRegistryId = "";
    for (int i = 0; i < 11; i++)
      finalRegistryId += (new Long(entireNumber[i])).toString();
    System.out.println("Registry id w/o check digit: " + finalRegistryId);
    
    // using the given algorithm to calculate the check digit
    for (int i = 0; i < 11; i++) {
      if (i % 2 == 0) // even element number
        theSum += entireNumber[i] * 3;
      else
        // odd element number
        theSum += entireNumber[i] * 1;
    }
    
    long checkDigit = (10 - theSum % 10) % 10; // equals 10 - 3 = 7
    
    // attaching the check digit to finalRegistryId
    finalRegistryId += (new Long(checkDigit)).toString();
    
    return Long.parseLong(finalRegistryId);
  }
  
  public long getCreatedRegistryId(DynamoHttpServletRequest pRequest) {
    long storeNum = getStoreNumber(pRequest);
    long seqNum = getSequenceNumberAndSetPrefix(storeNum, pRequest);
    long regId = calcCDigitAndRegId(storeNum, seqNum, getPrefix());
    
    return regId;
  }
  
  private String mBridalRootCat;
  private String mPrefix;
  private String webStoreNumber;
  private ValidateSku mValidateSku;
  
  public ValidateSku getValidateSku() {
    return mValidateSku;
  }
  
  public void setValidateSku(ValidateSku pstrInput) {
    mValidateSku = pstrInput;
  }
  
  public String getBridalRootCat() {
    return mBridalRootCat;
  }
  
  public void setBridalRootCat(String pstrInput) {
    mBridalRootCat = pstrInput;
  }
  
  /*
   * public void initBridalCats(String catId, Repository pRepository){ bridalVirtualCats.initCats(catId, pRepository); }
   */
  
  public User getCurrentUser(DynamoHttpServletRequest pRequest) {
    ComponentName userComponent = ComponentName.getComponentName("/atg/dynamo/security/User");
    return (User) pRequest.resolveName(userComponent);
  }
  
  public String getPrefix() {
    return mPrefix;
  }
  
  public void setPrefix(String pstrInput) {
    mPrefix = pstrInput;
  }
  
  public String getWebStoreNumber() {
    return webStoreNumber;
  }
  
  public void setWebStoreNumber(String pstrInput) {
    webStoreNumber = pstrInput;
  }
  
  public boolean addItemToGiftlist(String pGiftlistId, String pSkuId, String productId, long pQty, boolean pIsStore) throws ServletException, IOException, CommerceException {
    
    boolean retValue = false;
    
    try {
      try {
        RepositoryItem giftlistItem = getGiftlist(pGiftlistId);
        
        MutableRepositoryItem giftItem = (MutableRepositoryItem) getGiftItem(pSkuId, productId, giftlistItem);
        
        if (giftItem == null) {
          
          // MutableRepository giftRepository = (MutableRepository)getGiftlistTools().getGiftlistRepository();
          giftItem = (MutableRepositoryItem) getGiftlistTools().createGiftlistItem();
          giftItem.setPropertyValue("catalogRefId", pSkuId);
          giftItem.setPropertyValue("productId", productId);
          giftItem.setPropertyValue("quantityDesired", new Long(pQty));
          int giftItemType = (pIsStore) ? STORE_ITEM : WEB_ITEM;
          giftItem.setPropertyValue("type", new Integer(giftItemType));
          
          getGiftlistTools().addItem(giftItem);
          ((MutableRepositoryItem) giftlistItem).setPropertyValue("lastModifiedDate", Calendar.getInstance().getTime());
          
          retValue = getGiftlistTools().addItemToGiftlist(pGiftlistId, giftItem.getRepositoryId());
        } else {
          retValue = increaseGiftlistItemQuantityDesired(pGiftlistId, giftItem.getRepositoryId(), pQty);
        }
      } catch (CommerceException ce) {
        ce.printStackTrace();
        if (isLoggingError()) logError("RepositoryException: Encountered in handleAddToGiftList()" + ce);
      }
    } catch (RepositoryException re) {
      re.printStackTrace();
      if (isLoggingError()) logError("RepositoryException: Encountered in handleAddToGiftList()" + re);
    }
    return retValue;
  }
  
  /**
   * Name of Method: addItemToGiftlist
   * 
   * @param String
   *          pGiftlistId
   * @param String
   *          pSkuId
   * @param long pQty
   * @param boolean pIsStore
   * @return boolean success
   * 
   *         Returns a boolean value as to the success of the addition of the store Item.
   */
  public boolean addItemToGiftlist(String pGiftlistId, String pSkuId, long pQty, boolean pIsStore) throws ServletException, IOException, CommerceException {
    String lSkuId = null;
    String lProdId = null;
    RepositoryItem lSkuItem = null;
    boolean retValue = false;
    String infoStr;
    
    // Get Sku String.
    if (pSkuId != null && pSkuId.length() > 0)
      lSkuItem = mValidateSku.getSkuItem(pSkuId, pIsStore);
    else
      return retValue;
    
    if (lSkuItem == null) return retValue;
    
    // Get Sku String.
    lProdId = mValidateSku.getProductItemId(lSkuItem, pIsStore);
    
    if (lProdId == null) return retValue;
    
    try {
      try {
        RepositoryItem giftlistItem = getGiftlist(pGiftlistId);
        MutableRepositoryItem giftItem = (MutableRepositoryItem) getGiftItem(lSkuItem.getRepositoryId(), giftlistItem);
        if (giftItem == null) {
          // MutableRepository giftRepository = (MutableRepository)getGiftlistTools().getGiftlistRepository();
          giftItem = (MutableRepositoryItem) getGiftlistTools().createGiftlistItem();
          giftItem.setPropertyValue("catalogRefId", lSkuItem.getRepositoryId());
          giftItem.setPropertyValue("productId", lProdId);
          giftItem.setPropertyValue("quantityDesired", new Long(pQty));
          int giftItemType = (pIsStore) ? STORE_ITEM : WEB_ITEM;
          
          giftItem.setPropertyValue("type", new Integer(giftItemType));
          
          // Add the item to the repository
          getGiftlistTools().addItem(giftItem);
          ((MutableRepositoryItem) giftlistItem).setPropertyValue("lastModifiedDate", Calendar.getInstance().getTime());
          
          // Add I tem to the gift registry.
          retValue = getGiftlistTools().addItemToGiftlist(pGiftlistId, giftItem.getRepositoryId());
          if (retValue) infoStr = "Added the Sku: " + lSkuItem.getRepositoryId() + " with " + pQty + " Item(s) successfully added!";
        } else {
          System.out.println("Gift Item found in the registry.");
          retValue = increaseGiftlistItemQuantityDesired(pGiftlistId, giftItem.getRepositoryId(), pQty);
          if (retValue) infoStr = "Increase the Sku: " + lSkuItem.getRepositoryId() + " Desired Qty by " + pQty + " Item(s) successfully!";
          
        }
      } catch (CommerceException ce) {
        if (isLoggingError()) logError("RepositoryException: Encountered in handleAddToGiftList()" + ce);
      }
    } catch (RepositoryException re) {
      if (isLoggingError()) logError("RepositoryException: Encountered in handleAddToGiftList()" + re);
    }
    return retValue;
  }
  
  /***
   * Method to get the gift item from prodId and SkuId or a registry with Regid
   * 
   */
  private RepositoryItem getGiftItem(String pStrSku, RepositoryItem pRIGiftlist) {
    
    RepositoryItem retValue = null;
    
    if (pStrSku == null || pRIGiftlist == null || pStrSku.length() == 0) return retValue;
    
    List giftItems = (List) pRIGiftlist.getPropertyValue("giftlistItems");
    
    Iterator giftIterator = giftItems.iterator();
    while (giftIterator.hasNext()) {
      RepositoryItem tempItem = (RepositoryItem) giftIterator.next();
      String skuId = (String) tempItem.getPropertyValue("catalogRefId");
      String prodId = (String) tempItem.getPropertyValue("productId");
      
      if (skuId.equals(pStrSku)) {
        retValue = tempItem;
        break;
      }
      
    }
    return retValue;
  }
  
  private RepositoryItem getGiftItem(String pStrSku, String productId, RepositoryItem pRIGiftlist) {
    
    RepositoryItem retValue = null;
    
    if (pStrSku == null || pRIGiftlist == null || pStrSku.length() == 0) return retValue;
    
    List giftItems = (List) pRIGiftlist.getPropertyValue("giftlistItems");
    
    Iterator giftIterator = giftItems.iterator();
    while (giftIterator.hasNext()) {
      RepositoryItem tempItem = (RepositoryItem) giftIterator.next();
      String skuId = (String) tempItem.getPropertyValue("catalogRefId");
      String prodId = (String) tempItem.getPropertyValue("productId");
      
      if (skuId.equals(pStrSku) && productId.equals(prodId)) {
        retValue = tempItem;
        break;
      }
      
    }
    return retValue;
  }
  
}
