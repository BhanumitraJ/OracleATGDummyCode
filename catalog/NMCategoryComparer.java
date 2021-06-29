package com.nm.commerce.catalog;

import java.util.Comparator;

/**
 * Created by SLeija on 3/27/2014.
 */
public class NMCategoryComparer implements Comparator<com.nm.catalog.navigation.NMCategory> {
  
  @Override
  public int compare(com.nm.catalog.navigation.NMCategory cat1, com.nm.catalog.navigation.NMCategory cat2) {
    
    String cat1Name = null;
    String cat2Name = null;
    
    // if(cat1.getAlternateCategoryName() != null)
    // cat1Name = cat1.getAlternateCategoryName();
    // else
    cat1Name = cat1.getDisplayName();
    
    // if(cat2.getAlternateCategoryName() != null)
    // cat2Name = cat2.getAlternateCategoryName();
    // else
    cat2Name = cat2.getDisplayName();
    
    return cat1Name.compareToIgnoreCase(cat2Name);
  }
}
