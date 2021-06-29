package com.nm.commerce;

import atg.repository.Repository;
import atg.repository.RepositoryException;
import atg.repository.RepositoryItem;

import com.nm.components.CommonComponentHelper;
import com.nm.nucleus.NMGenericService;

public class HtmlFragmentHelper extends NMGenericService {
  public static final String PATH = "/nm/commerce/catalog/HtmlFragmentHelper";
  
  private static final HtmlFragmentHelper HtmlFragmentHelper = new HtmlFragmentHelper();
  
  public static HtmlFragmentHelper getInstance() {
    return (HtmlFragmentHelper);
  }
  
  private Repository catalogRepository = CommonComponentHelper.getProductRepository();
  
  public String getFragmentValue(String fragmentId) {
    String value = null;
    try {
      RepositoryItem fragment = null;
      if (fragmentId != null) {
        fragment = catalogRepository.getItem(fragmentId, "htmlfragments");
        if (fragment != null) {
          value = (String) fragment.getPropertyValue("frag_value");
        }
      }
    } catch (RepositoryException e) {
      this.logError(e);
    }
    return value;
  }
  
}
