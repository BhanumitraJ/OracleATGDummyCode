package com.nm.commerce.giftlist;

import atg.nucleus.GenericService;
import java.util.*;

public class GiftlistSearchResultsHolder extends GenericService {
  
  private ArrayList searchResults;
  private String searchString;
  private String searchValue = null;
  
  private String searchResultsSize;
  
  public void setSearchResults(ArrayList searchResults) {
    this.searchResults = searchResults;
  }
  
  public ArrayList getSearchResults() {
    if (searchResults == null) setSearchResults(new ArrayList());
    return searchResults;
  }
  
  public void setSearchString(String searchString) {
    this.searchString = searchString;
  }
  
  public String getSearchString() {
    return searchString;
  }
  
  public void setSearchValue(String searchValue) {
    this.searchValue = searchValue;
  }
  
  public String getSearchValue() {
    return searchValue;
  }
  
  public boolean getHasSearchResults() {
    if (getSearchResults().size() == 0) {
      return false;
    } else {
      return true;
    }
  }
  
  public void setSearchResultsSize(String searchResultsSize) {
    this.searchResultsSize = searchResultsSize;
  }
  
  public String getSearchResultsSize() {
    return Integer.toString(getSearchResults().size());
  }
  
}
