package com.nm.commerce.pagedef.evaluator.product;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import atg.nucleus.Nucleus;
import atg.servlet.DynamoHttpServletRequest;

import com.endeca.navigation.Navigation;
import com.nm.catalog.history.NMCatalogHistory;
import com.nm.catalog.history.NMEndecaShoppableClick;
import com.nm.commerce.catalog.NMPagingPrevNext;
import com.nm.commerce.catalog.NMProduct;
import com.nm.search.endeca.NMDimensionValue;
import com.nm.search.endeca.NMSearchManager;
import com.nm.search.endeca.NMSearchRecord;
import com.nm.search.endeca.NMSearchResult;
import com.nm.search.endeca.NMSearchSessionData;
import com.nm.search.endeca.NMSearchUtil;
import com.nm.search.endeca.SearchParameters;
import com.nm.utils.GenericLogger;

// package protected
class EndecaPrevNextProductHelper {
  public final static String PP_SEARCH_RESULT = "ppSearchResult";
  public final static String PREV_PROD = "prevProd";
  public final static String NEXT_PROD = "nextProd";
  public final static String PREV_PROD_ENDECA = "prevProdEndeca";
  public final static String NEXT_PROD_ENDECA = "nextProdEndeca";
  public final static String DISPLAY_DIVIDE_BAR = "displayDivideBar";
  public final static String DESIGNER_DIMVAL = "designerDimval";
  public final static String BREADCRUMB_PROD_ID = "bcId";
  public final static String CURRENT_POSITION = "currentPosition";
  public final static String TOTAL_RECORDS = "totalRecords";
  public final static String NEXT_POSITION = "nextPosition";
  public final static String PREV_POSITION = "prevPosition";
  
  public final static String SEARCH_RESULT_INPUT = "endecaNavigationObject";
  public final static String PROD_LIST_SEARCHRESULTS = "prodPageSearchResults";
  public final static String NM_SEARCH_RESULT = "searchResult";
  public final static String CURRENT_PROD_E = "currentProdE";
  public final static String CURRENT_PROD_P = "currentProdP";
  
  private GenericLogger log;
  private NMSearchManager nmSearchManager;
  
  private DynamoHttpServletRequest request;
  
  private String currentProdE;
  private String currentProdP;
  private NMEndecaShoppableClick shoppableClick;
  private Navigation nav;
  private int index;
  private String pageSize;
  private String position;
  
  public EndecaPrevNextProductHelper(String currentProduct, String currentProductP, String pageSize, String position, NMCatalogHistory catalogHistory, DynamoHttpServletRequest request) {
    log = (GenericLogger) Nucleus.getGlobalNucleus().resolveName("/nm/utils/GenericLogger");
    
    this.request = request;
    this.shoppableClick = ((NMEndecaShoppableClick) catalogHistory.getLastShoppableClick());
    
    this.nav = this.shoppableClick.getSearchResult().getNavigation();
    
    this.currentProdE = currentProduct;
    this.currentProdP = currentProductP;
    
    if (pageSize == null) {
      NMSearchSessionData searchData = (NMSearchSessionData) request.resolveName("/nm/search/endeca/NMSearchSessionData");
      if (searchData != null) {
        this.pageSize = searchData.getPageSize();
      }
    } else {
      this.pageSize = pageSize;
    }
    
    this.position = position;
    
    shoppableClick.setCurrentProdE(currentProdE);
    shoppableClick.setCurrentProdP(currentProdP);
  }
  
  public NMPagingPrevNext getEndecaPaging() {
    NMSearchResult searchResult = shoppableClick.getSearchResult();
    NMSearchResult prodPageSearchResults = shoppableClick.getSearchResultProdPage();
    
    int sizeOfSearchResult = searchResult.getSearchRecordList().size();
    int sizeOfSearchResultZeroBased = sizeOfSearchResult - 1;
    
    NMSearchResult nmSearchResultToUse = determineWhichSearchResult(searchResult, prodPageSearchResults, currentProdE, request);
    
    this.index = 0;
    try {
      this.index = Integer.valueOf(position);
    } catch (Exception e) {
      long pageIndex = findCurrentSpot(currentProdE, searchResult, sizeOfSearchResultZeroBased);
      if (pageIndex > 0) {
        long offset = nav.getERecsOffset();
        this.index = (int) offset + (int) pageIndex;
      }
    }
    
    NMPagingPrevNext productPaging = new NMPagingPrevNext();
    
    productPaging.setPreviousProduct(new NMProduct(getPreviousId(currentProdE, nmSearchResultToUse, request)));
    productPaging.setNextProduct(new NMProduct(getNextId(currentProdE, nmSearchResultToUse, request)));
    
    getDesignerDimval(currentProdE, nmSearchResultToUse, request);
    
    request.setParameter(BREADCRUMB_PROD_ID, currentProdE);
    
    if (index > 0) {
      long totalRecords = nav.getTotalNumERecs();
      
      productPaging.setIndex((int) index);
      productPaging.setTotalProducts((int) totalRecords);
    }
    
    return productPaging;
  }
  
  private NMSearchResult determineWhichSearchResult(NMSearchResult nmSearchResultUser, NMSearchResult nmSearchResultPP, String currentPROD, DynamoHttpServletRequest request) {
    
    NMSearchResult newSearchResultExecuted = null;
    
    // we have the users searchResult and potentially another one if we have
    // had to go back and get another batch if
    // the user has clicked past the original result on the prod page.this
    // is where we figure out which one to use
    // or if we need to execute another query and set the new one in the
    // navhistory
    
    boolean foundInUser = prodExistsInList(currentPROD, nmSearchResultUser);
    boolean foundInPP = prodExistsInList(currentPROD, nmSearchResultPP);
    
    if (log.isLoggingDebug()) {
      log.logDebug("current product looknig for----->" + currentPROD);
      log.logDebug("does the current product exist in user query-->" + foundInUser);
      log.logDebug("does the current product exist in productpage query-->" + foundInPP);
    }
    
    if (nmSearchResultPP != null && foundInPP) {
      // if this is not null then we have already generated an additional
      // query with the users query string.The only use of this
      // is currently this droplet.We use it here until we reach an edge
      // and need to query again.
      
      // first check to see if we are at the edge of these results and
      // need to requery
      
      int sizeOfSearchResultPP = nmSearchResultPP.getProductIDList().size();
      // decrement it since we are dealign with zero based
      int sizeOfSearchResultZeroBasedPP = sizeOfSearchResultPP - 1;
      boolean listStillGoodPP = listIsValid(currentPROD, nmSearchResultPP, sizeOfSearchResultZeroBasedPP, pageSize);
      int locationOfCurrentRec = findCurrentSpot(currentPROD, nmSearchResultPP, sizeOfSearchResultZeroBasedPP);
      
      if (listStillGoodPP) {
        return nmSearchResultPP;
      } else {
        // we must be at the edge of the searchresult for productpage so need to requery
        Map<String, Object> searchStringMap = NMSearchUtil.getParameterMap(nmSearchResultPP.getSearchString());
        // clear the old list
        nmSearchResultPP.getProductIDList().clear();
        nmSearchResultPP.getEndecaProdIdPPIdMap().clear();
        
        newSearchResultExecuted = getNextBatch(nmSearchResultPP, searchStringMap, locationOfCurrentRec, pageSize);
        request.setParameter(PP_SEARCH_RESULT, newSearchResultExecuted);
        return newSearchResultExecuted;
      }
    } else {
      // so far we have not executed another query we are paging thru the users original searchresult
      int sizeOfSearchResultUser = nmSearchResultUser.getProductIDList().size();
      // decrement it since we are dealign with zero based
      int sizeOfSearchResultZeroBasedUser = sizeOfSearchResultUser - 1;
      
      int locationOfCurrentRec = findCurrentSpot(currentPROD, nmSearchResultUser, sizeOfSearchResultZeroBasedUser);
      boolean listStillGoodUser = listIsValid(currentPROD, nmSearchResultUser, sizeOfSearchResultZeroBasedUser, pageSize);
      if (listStillGoodUser) {
        return nmSearchResultUser;
      } else {
        // we must be at the edge of the searchresult for user so need to requery
        Map<String, Object> searchStringMap = NMSearchUtil.getParameterMap(nmSearchResultUser.getSearchString());
        newSearchResultExecuted = getNextBatch(nmSearchResultUser, searchStringMap, locationOfCurrentRec, pageSize);
        request.setParameter(PP_SEARCH_RESULT, newSearchResultExecuted);
        return newSearchResultExecuted;
      }
    }
  }
  
  private boolean listIsValid(String currentProdId, NMSearchResult nmSearchResult, int sizeOfList, String thePageSize) {
    boolean prodListStillGood = true;
    int thePosition = 0;
    
    Map<String, Object> searchStringMap = NMSearchUtil.getParameterMap(nmSearchResult.getSearchString());
    
    String theNoParam = (String) searchStringMap.get("No");
    int theOffset = 0;
    
    if (theNoParam != null) {
      theOffset = Integer.valueOf(theNoParam).intValue();
    }
    
    List<String> theProdList = nmSearchResult.getProductIDList();
    
    if (log.isLoggingDebug()) {
      log.logDebug("we are checking if list is still valid this is the current list-->" + theProdList);
      log.logDebug("this is the current product we are looking for-->" + currentProdId);
      log.logDebug("the spot in the list for this product is-->" + findCurrentSpot(currentProdId, nmSearchResult, sizeOfList));
      log.logDebug("the offset in searchResult is product is-->" + theOffset);
    }
    
    long sizeOfSearch = nmSearchResult.getProductIDList().size();
    long totalSearchSize = nmSearchResult.getTotalRecordCount();
    long erecOffsetSearch = nmSearchResult.getNavigation().getERecsOffset();
    long moreLeft = totalSearchSize - erecOffsetSearch;
    long offsetPlusLeft = erecOffsetSearch + moreLeft;
    --totalSearchSize;
    
    if (log.isLoggingDebug()) {
      log.logDebug("list is valid check totalSearchSize---->" + totalSearchSize);
      log.logDebug("list is valid check erecOffsetSearch---->->" + erecOffsetSearch);
      log.logDebug("list is valid check moreLeft---->->" + moreLeft);
      log.logDebug("list is valid check sizeOfSearch---->->" + sizeOfSearch);
      log.logDebug("list is valid check offsetPlusLeft---->->" + offsetPlusLeft);
    }
    
    synchronized (theProdList) {
      if (totalSearchSize == sizeOfList) {
        return prodListStillGood;
      }
      
      if (theProdList.contains(currentProdId)) {
        thePosition = theProdList.indexOf(currentProdId);
      }
    }
    // can we use this list need one on both sides
    
    int lowerLimit = thePosition - 1;
    int upperLimit = thePosition + 1;
    
    if (log.isLoggingDebug()) {
      log.logDebug("looking at if we have a next/prev the lower is-->" + lowerLimit);
      log.logDebug("looking at if we have a next/prev the upper is-->" + upperLimit);
      log.logDebug("looking at if we have a next/prev positon of current rec-->" + thePosition);
    }
    
    if (lowerLimit < 0) {
      if (theOffset > 0) {
        if (log.isLoggingDebug()) {
          log.logDebug("list failed because lower limit is negative-->" + lowerLimit);
        }
        prodListStillGood = false;
      }
    }
    
    if (upperLimit > sizeOfList) {
      if (log.isLoggingDebug()) {
        log.logDebug("list failed because upper limit is greater than size of list--upperLimit-->" + upperLimit);
        log.logDebug("list failed because upper limit is greater than size of list--sizeOfList-->" + sizeOfList);
      }
      
      prodListStillGood = false;
    }
    
    if (log.isLoggingDebug()) {
      log.logDebug("list is valid check is the list still good---->" + prodListStillGood);
    }
    
    return prodListStillGood;
  }
  
  private String getPreviousId(String currentProdId, NMSearchResult nmSearchResult, DynamoHttpServletRequest request) {
    String thePrevId = null;
    int thePosition = 0;
    
    List<String> theProdList = nmSearchResult.getProductIDList();
    Map<String, String> theProdMap = nmSearchResult.getEndecaProdIdPPIdMap();
    synchronized (theProdList) {
      if (theProdList.contains(currentProdId)) {
        thePosition = theProdList.indexOf(currentProdId);
        --thePosition;
        
        if (thePosition >= 0) {
          String thePrevEndecaId = (String) theProdList.get(thePosition);
          // this is the endeca id we need to get the prod page id
          // (suites etc) off the map
          thePrevId = (String) theProdMap.get(thePrevEndecaId);
          request.setParameter(PREV_PROD, thePrevId);
          request.setParameter(PREV_PROD_ENDECA, thePrevEndecaId);
        }
      }
    }
    
    return thePrevId;
  }
  
  private String getNextId(String currentProdId, NMSearchResult nmSearchResult, DynamoHttpServletRequest request) {
    
    String theNextid = null;
    int thePosition = 0;
    int sizeOfList = nmSearchResult.getProductIDList().size();
    --sizeOfList;// decrement to use for zero based list
    
    List<String> theProdList = nmSearchResult.getProductIDList();
    Map<String, String> theProdMap = nmSearchResult.getEndecaProdIdPPIdMap();
    synchronized (theProdList) {
      if (theProdList.contains(currentProdId)) {
        thePosition = theProdList.indexOf(currentProdId);
        ++thePosition;
        
        if (thePosition <= sizeOfList) {
          String theNextEndecaId = (String) theProdList.get(thePosition);
          theNextid = (String) theProdMap.get(theNextEndecaId);
          request.setParameter(NEXT_PROD, theNextid);
          request.setParameter(NEXT_PROD_ENDECA, theNextEndecaId);
        }
      }
    }
    
    return theNextid;
  }
  
  private int findCurrentSpot(String currentProdId, NMSearchResult nmSearchResult, int sizeOfSearchResult) {
    int thePosition = -1;
    int sizeOfList = nmSearchResult.getProductIDList().size();
    --sizeOfList;// decrement to use for zero based list
    
    List<String> theProdList = nmSearchResult.getProductIDList();
    synchronized (theProdList) {
      if (theProdList.contains(currentProdId)) {
        thePosition = theProdList.indexOf(currentProdId);
        ++thePosition;
      }
    }
    
    return thePosition;
  }
  
  private boolean prodExistsInList(String currentProdId, NMSearchResult nmSearchResult) {
    boolean productExists = false;
    if (nmSearchResult != null) {
      List<String> theProdList = nmSearchResult.getProductIDList();
      
      synchronized (theProdList) {
        if (theProdList.contains(currentProdId)) {
          productExists = true;
        }
      }
    }
    
    return productExists;
  }
  
  private int calculateNewOffset(int theOffset, int locationOfCurrentRec, int totalSearchSize) {
    int theNewOffset = 0;
    int currentOffsetWholeList = theOffset + locationOfCurrentRec;
    int remainingRecs = totalSearchSize - currentOffsetWholeList;
    // dealing with zero based lists so increment
    remainingRecs++;
    currentOffsetWholeList--;
    
    if (log.isLoggingDebug()) {
      log.logDebug("calculateNewOffsetcurrentOffsetWholeList---->" + currentOffsetWholeList);
      log.logDebug("calculateNewOffsetremainingRecs---->" + remainingRecs);
      log.logDebug("calculateNewOffsettotalSearchSize---->" + totalSearchSize);
    }
    
    if (currentOffsetWholeList < 60 && locationOfCurrentRec < 60) {
      if (log.isLoggingDebug()) {
        log.logDebug("user is at beginning of result set so we are returning zero for new offset---->" + theNewOffset);
      }
      return theNewOffset;
    }
    
    // are they at the end
    if (remainingRecs < 60) {
      // they are at the tail end so get rest of them
      theNewOffset = totalSearchSize - 60;
      
      if (log.isLoggingDebug()) {
        log.logDebug("user is at tail with our 60 rec query so returning----------->" + theNewOffset);
      }
      return theNewOffset;
    }
    
    // user must have recs on each side
    // they are in the middle of a result set so take the location of the
    // rec they are at and take 30 away
    // with a 60 result set this will leave us with 30 on each side of the
    // rec they are at
    
    theNewOffset = currentOffsetWholeList - 30;
    
    if (log.isLoggingDebug()) {
      log.logDebug("trying to figure out new offset came back ----------->" + theNewOffset);
    }
    
    if (theNewOffset < 0) {
      theNewOffset = 0;
      if (log.isLoggingDebug()) {
        log.logDebug("trying to figure out new offset came back negative so returning zero----------->" + theNewOffset);
      }
    }
    return theNewOffset;
    
  }
  
  private NMSearchResult getNextBatch(NMSearchResult newSearchResultExecuted, Map<String, Object> searchStringMap, int locationOfCurrentRec, String thePageSize) {
    String originalQueryString = newSearchResultExecuted.getSearchString();
    NMSearchResult nmSearchResult = null;
    try {
      
      String theNoParam = (String) searchStringMap.get("No");
      
      int totalSearchSize = (int) newSearchResultExecuted.getTotalRecordCount();
      
      int theOffset = 0;
      int theNewOffset = 0;
      
      if (theNoParam != null) {
        theOffset = Integer.valueOf(theNoParam).intValue();
      }
      
      theNewOffset = calculateNewOffset(theOffset, locationOfCurrentRec, totalSearchSize);
      
      // we need to calculate where we want to pull the products from we
      // are going to get a batch of 60 products and tehn user can have 20
      // on each side of
      // their list.
      
      String theSearchString = replace(originalQueryString, "No=" + theOffset, "No=" + theNewOffset);
      theSearchString = replace(theSearchString, "&pageSize=" + thePageSize, "&pageSize=60");
      
      if (log.isLoggingDebug()) {
        log.logDebug("we executed a new query to get another result set this is the old query string-->" + originalQueryString);
        log.logDebug("we executed a new query to get another result set this is the new query string-->" + theSearchString);
      }
      
      SearchParameters searchParameters = new SearchParameters();
      searchParameters.setSearchString(theSearchString);
      searchParameters.setExposeAllDimensions(true);
      nmSearchResult = getSearchManager().search(searchParameters);
      
    } catch (Exception e) {
      log.logError(e);
    }
    return nmSearchResult;
  }
  
  private String replace(String str, String patternStr, String replace) {
    
    if (patternStr == null) {
      return str;
    }
    
    String output = null;
    Pattern pattern = Pattern.compile(patternStr);
    Matcher matcher = pattern.matcher(str);
    boolean canReplace = matcher.find();
    
    if (canReplace) {
      output = matcher.replaceAll(replace);
    } else {
      if (log.isLoggingDebug()) {
        log.logDebug("was not able to replace so inserted-->" + replace);
      }
      
      output = str + "&" + replace;
    }
    
    if (log.isLoggingDebug()) {
      log.logDebug("this is the input string search/replace reg exp->" + str);
      log.logDebug("this is waht we are returning for search/replace reg exp->" + output);
    }
    
    return output;
  }
  
  private NMDimensionValue getDesignerDimval(String currentProdId, NMSearchResult nmSearchResult, DynamoHttpServletRequest request) {
    NMDimensionValue designerDimval = null;
    int thePosition = 0;
    
    List<String> theProdList = nmSearchResult.getProductIDList();
    synchronized (theProdList) {
      if (theProdList.contains(currentProdId)) {
        thePosition = theProdList.indexOf(currentProdId);
        NMSearchRecord currentRec = (NMSearchRecord) nmSearchResult.getSearchRecordList().get(thePosition);
        designerDimval = currentRec.getDesignerDimval();
        
        if (designerDimval != null) {
          request.setParameter(DESIGNER_DIMVAL, designerDimval);
        }
      }
    }
    return designerDimval;
  }
  
  public NMSearchManager getSearchManager() {
    return nmSearchManager;
  }
  
  public void setSearchManager(NMSearchManager nmSearchManager) {
    this.nmSearchManager = nmSearchManager;
  }
}
