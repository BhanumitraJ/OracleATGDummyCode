package com.nm.commerce.pagedef.evaluator;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import javax.servlet.http.Cookie;
import javax.servlet.jsp.PageContext;

import atg.core.util.StringUtils;
import atg.naming.NameContext;
import atg.nucleus.Nucleus;
import atg.nucleus.logging.ApplicationLogging;
import atg.nucleus.logging.ClassLoggingFactory;
import atg.repository.Repository;
import atg.repository.RepositoryItem;
import atg.repository.RepositoryView;
import atg.repository.rql.RqlStatement;
import atg.servlet.DynamoHttpServletRequest;
import atg.servlet.DynamoHttpServletResponse;

import com.nm.collections.State;
import com.nm.collections.StateArray;
import com.nm.commerce.pagedef.definition.PageDefinition;
import com.nm.commerce.pagedef.model.StoreAndEventLocatorPageModel;
import com.nm.storelocator.client.StoreLocatorService;
import com.nm.storelocator.gateway.dto.AddressDTO;
import com.nm.storelocator.gateway.dto.BrandDTO;
import com.nm.storelocator.gateway.dto.StoreDTO;
import com.nm.storelocator.gateway.dto.StoreEventDTO;
import com.nm.storelocator.gateway.exception.GatewayException;
import com.nm.storelocator.gateway.exception.GatewayNoMatchesException;
import com.nm.utils.CookieUtil;
import com.nm.utils.LocalizationUtils;

public class StoreAndEventLocatorEvaluator extends SimplePageEvaluator {
  
  private String COOKIE_NAME = "sael_cookie"; // store and event locator cookie
  private final ApplicationLogging mLogging = ClassLoggingFactory.getFactory().getLoggerForClass(StoreAndEventLocatorEvaluator.class);
  
  public boolean evaluate(PageDefinition pageDefinition, PageContext pageContext) throws Exception {
    boolean eval = super.evaluate(pageDefinition, pageContext);
    StoreAndEventLocatorPageModel model = (StoreAndEventLocatorPageModel) getPageModel();
    // PageDefinition definition = (PageDefinition)pageDefinition;
    DynamoHttpServletRequest request = getRequest();
    
    final StateArray stateService = (StateArray) Nucleus.getGlobalNucleus().resolveName("/nm/collections/StateArray");
    model.setAllStates(stateService.getStoreLocatorStatesSorted());
    
    // stop if we have formHandlerErrors
    NameContext ctx = request.getRequestScope();
    if (ctx != null) {
      Vector dropletExceptions = (Vector) ctx.getElement("DropletExceptions");
      if (dropletExceptions != null && dropletExceptions.size() > 0) return true;
    }
    
    DynamoHttpServletResponse response = getResponse();
    
    String city = request.getParameter("city");
    String state = request.getParameter("state");
    String zip = request.getParameter("zip");
    String viewAllStoreInfo = request.getParameter("viewAll");
    String cookieAddress = null;
    
    List<StoreDTO> stores = null;
    AddressDTO address = new AddressDTO();
    int mileRadius = 50;
    BrandDTO brand = new BrandDTO(BrandDTO.BRAND2); // LC
    BrandDTO[] brands = {brand};
    int minResults = 9999;
    boolean detailed = true;
    
    // See if there is a cookie from a previous search
    Cookie cookie = CookieUtil.getCookie(COOKIE_NAME, request);
    String cookieValue = null;
    boolean cookieValueFound = false;
    String cookieCity = null;
    String cookieState = null;
    String cookieZip = null;
    
    if (cookie != null) cookieValue = cookie.getValue();
    
    // cookieValue will be delimited string, e.g., city=foo:state=bar
    if (cookieValue != null) {
      String[] bits = cookieValue.split("&");
      for (String bit : bits) {
        String[] nameValuePair = bit.split(":");
        if (nameValuePair.length > 1) {
          cookieValueFound = true;
          if (nameValuePair[0].equals("city")) {
            cookieCity = nameValuePair[1];
          } else if (nameValuePair[0].equals("state")) {
            cookieState = nameValuePair[1];
          } else if (nameValuePair[0].equals("zip")) {
            cookieZip = nameValuePair[1];
          }
        }
      }
    }
    
    // User clicked "view all stores" link
    if (viewAllStoreInfo != null && viewAllStoreInfo.equals("true")) {
      stores = getStores("", model);
      model.setUseStateNameHeaders(true);
    }
    // user entered just a state
    else if ((StringUtils.isEmpty(city) || city.equals("city")) && (StringUtils.isEmpty(zip) || zip.equals("zip code")) && !StringUtils.isEmpty(state)) {
      stores = getStores(state, model);
      model.setUseStateNameHeaders(false);
      model.setCurrentState(state);
      cookieAddress = "state:" + state;
    }
    // user entered no input at all
    else if ((city == null || city.equals("city")) && (state == null || state.equals("")) && (zip == null || zip.equals("zip code"))) {
      model.setUseStateNameHeaders(false);
      if (cookieValueFound) {
        if (StringUtils.isEmpty(cookieCity) && StringUtils.isEmpty(cookieZip)) {
          // do a state repository search
          stores = getStores(cookieState, model);
          model.setCurrentState(cookieState);
        } else {
          // do a lat long search
          if (!StringUtils.isEmpty(cookieZip)) {
            address.setZipCode(cookieZip);
            model.setCurrentZip(cookieZip);
          } else {
            address.setCity(cookieCity);
            address.setState(cookieState);
            model.setCurrentCity(cookieCity);
            model.setCurrentState(cookieState);
          }
          stores = getStores(mileRadius, brands, address, minResults, detailed, model);
        }
        
      } else { // default to akamai headers
        LocalizationUtils localizationUtils = ((LocalizationUtils) Nucleus.getGlobalNucleus().resolveName("/nm/utils/LocalizationUtils"));
        city = localizationUtils.findCityUsingEdgescape(request);
        state = localizationUtils.findRegionCodeUsingEdgescape(request).toUpperCase();
        
        address.setCity(city);
        model.setCurrentCity(city);
        address.setState(state);
        model.setCurrentState(state);
        
        stores = getStores(mileRadius, brands, address, minResults, detailed, model);
      }
    }
    // user entered something other than just a state
    else {
      model.setUseStateNameHeaders(false);
      cookieAddress = "";
      
      // prioritize zip code
      if (StringUtils.isEmpty(zip) || zip.equals("zip code")) {
        model.setCurrentZip("");
        
        if (city != null && !city.equals("") && !city.equals("city")) {
          address.setCity(city);
          model.setCurrentCity(city);
          cookieAddress += "city:" + city + "&";
        } else
          model.setCurrentCity("");
        
        if (!StringUtils.isEmpty(state)) {
          address.setState(state);
          model.setCurrentState(state);
          cookieAddress += "state:" + state;
        } else
          model.setCurrentState("");
      } else {
        address.setZipCode(zip);
        model.setCurrentZip(zip);
        cookieAddress += "zip:" + zip;
      }
      
      stores = getStores(mileRadius, brands, address, minResults, detailed, model);
      model.setFindStores(true);
    }
    
    model.setStores(stores);
    
    if (stores.size() > 0 && !StringUtils.isEmpty(cookieAddress)) CookieUtil.setCookie(request, response, COOKIE_NAME, cookieAddress);
    
    return eval;
  }
  
  protected StoreAndEventLocatorPageModel allocatePageModel() {
    return new StoreAndEventLocatorPageModel();
  }
  
  protected List<StoreDTO> getStores(int mileRadius, BrandDTO[] brands, AddressDTO address, int minResults, boolean detailed, StoreAndEventLocatorPageModel model) {
    final StoreLocatorService storeLocatorService = (StoreLocatorService) Nucleus.getGlobalNucleus().resolveName("/nm/services/StoreLocatorService");
    
    List<StoreDTO> stores = new ArrayList<StoreDTO>();
    
    try {
      StoreDTO[] storeArray = storeLocatorService.lookupStores(mileRadius, brands, address, minResults, detailed);
      for (StoreDTO store : storeArray) {
        store.setFullStateName(getFullStateNameForStateAbbreviation(store.getState(), model));
        stores.add(store);
      }
    } catch (GatewayNoMatchesException gnme) {
      // addFormException(new DropletException("No stores were found for your search. Please try again."), request);
    }
    // really shouldn't happen in production, but might in test if the headers aren't passed
    catch (GatewayException ge) {
      stores = getStores("", model);
      model.setUseStateNameHeaders(true);
    }
    
    return stores;
    
  }
  
  protected List<StoreDTO> getStores(String state, StoreAndEventLocatorPageModel model) {
    ArrayList<StoreDTO> stores = new ArrayList<StoreDTO>();
    
    try {
      Repository storeInfoRepository = (Repository) Nucleus.getGlobalNucleus().resolveName("/nm/xml/StoreInfoRepository");
      RepositoryView rView = storeInfoRepository.getView("storeLocation");
      RqlStatement mStatement = null;
      Object[] params = null;
      
      if (state.equals("")) {
        mStatement = RqlStatement.parseRqlStatement("flgDisplay = ?0 and not(state is null) ORDER BY state, city");
        params = new Object[1];
        params[0] = Boolean.TRUE;
      } else {
        mStatement = RqlStatement.parseRqlStatement("flgDisplay = ?0 and state = ?1 ORDER BY city");
        params = new Object[2];
        params[0] = Boolean.TRUE;
        params[1] = state;
      }
      
      RepositoryItem[] items = mStatement.executeQuery(rView, params);
      
      if (items != null && items.length > 0) {
        for (int i = 0; i < items.length; i++) {
          RepositoryItem storeRepositoryItem = (RepositoryItem) items[i];
          StoreDTO store = new StoreDTO();
          store.setName((String) storeRepositoryItem.getPropertyValue("name"));
          store.setNbr((String) storeRepositoryItem.getPropertyValue("storeNumber"));
          store.setId((String) storeRepositoryItem.getPropertyValue("id"));
          store.setShortName((String) storeRepositoryItem.getPropertyValue("shortName"));
          store.setCustomerServiceInfo((String) storeRepositoryItem.getPropertyValue("custsrvInfo"));
          store.setMapUrl((String) storeRepositoryItem.getPropertyValue("mapURL"));
          store.setAddress1((String) storeRepositoryItem.getPropertyValue("address1"));
          store.setAddress2((String) storeRepositoryItem.getPropertyValue("address2"));
          store.setCity((String) storeRepositoryItem.getPropertyValue("city"));
          store.setState((String) storeRepositoryItem.getPropertyValue("state"));
          store.setZipCode((String) storeRepositoryItem.getPropertyValue("zipCode"));
          store.setPhoneNumber((String) storeRepositoryItem.getPropertyValue("phoneNumber"));
          store.setStoreHours((String) storeRepositoryItem.getPropertyValue("storeHours"));
          
          store.setFullStateName(getFullStateNameForStateAbbreviation(store.getState(), model));
          
          ArrayList<StoreEventDTO> events = new ArrayList<StoreEventDTO>();
          Set s = ((Set) storeRepositoryItem.getPropertyValue("events"));
          Iterator it = s.iterator();
          RepositoryItem repEvent = null;
          StoreEventDTO tempEvent;
          while (it.hasNext()) {
            repEvent = (RepositoryItem) it.next();
            tempEvent = new StoreEventDTO();
            tempEvent.setName((String) repEvent.getPropertyValue("name"));
            tempEvent.setDescription((String) repEvent.getPropertyValue("descr"));
            tempEvent.setDescription2((String) repEvent.getPropertyValue("descr2"));
            tempEvent.setStartDate((Date) repEvent.getPropertyValue("startDate"));
            tempEvent.setEndDate((Date) repEvent.getPropertyValue("endDate"));
            tempEvent.setEventId((String) repEvent.getPropertyValue("id"));
            RepositoryItem eventTypeItem = (RepositoryItem) repEvent.getPropertyValue("eventType");
            if (eventTypeItem != null) {
              tempEvent.setEventType((String) eventTypeItem.getPropertyValue("eventTypeName"));
            }
            tempEvent.setHighlight((Boolean) repEvent.getPropertyValue("highlight"));
            events.add(tempEvent);
          }
          store.setEvents(events);
          
          stores.add(store);
        }
      }
    } catch (Exception e) {
    	if (mLogging.isLoggingError()) { 
    		mLogging.logError("Error:StoreAndEventLocatorEvaluator:getStores:" + e);
    	}
    }
    
    return stores;
  }
  
  private String getFullStateNameForStateAbbreviation(String stateAbbreviation, StoreAndEventLocatorPageModel model) {
    for (State state : model.getAllStates()) {
      if (state.getShortName().equals(stateAbbreviation)) return state.getLongName();
    }
    
    return "";
  }
}
