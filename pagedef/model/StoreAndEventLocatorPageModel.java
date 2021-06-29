package com.nm.commerce.pagedef.model;

import java.util.List;

import com.nm.collections.State;
import com.nm.storelocator.gateway.dto.StoreDTO;

public class StoreAndEventLocatorPageModel extends PageModel {
  
  private List<StoreDTO> stores;
  private boolean useStateNameHeaders = false;
  private String currentState;
  private String currentCity;
  private String currentZip;
  private State[] allStates;
  private boolean findStores = false;
  
  public boolean getFindStores() {
    return findStores;
  }
  
  public void setFindStores(boolean findStores) {
    this.findStores = findStores;
  }
  
  public List<StoreDTO> getStores() {
    return stores;
  }
  
  public void setStores(List<StoreDTO> stores) {
    this.stores = stores;
  }
  
  public boolean isUseStateNameHeaders() {
    return useStateNameHeaders;
  }
  
  public void setUseStateNameHeaders(boolean useStateNameHeaders) {
    this.useStateNameHeaders = useStateNameHeaders;
  }
  
  public String getCurrentState() {
    return currentState;
  }
  
  public void setCurrentState(String currentState) {
    this.currentState = currentState;
  }
  
  public String getCurrentCity() {
    return currentCity;
  }
  
  public void setCurrentCity(String currentCity) {
    this.currentCity = currentCity;
  }
  
  public String getCurrentZip() {
    return currentZip;
  }
  
  public void setCurrentZip(String currentZip) {
    this.currentZip = currentZip;
  }
  
  public State[] getAllStates() {
    return allStates;
  }
  
  public void setAllStates(State[] allStates) {
    this.allStates = allStates;
  }
  
}
