package com.nm.commerce.catalog;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.nm.catalog.navigation.CategoryHelper;
import com.nm.catalog.navigation.NMCategory;
import com.nm.commerce.HtmlFragmentHelper;
import com.nm.droplet.FilterCategoryGroup;

/**
 * Entry point for the retrieval of a drawer.
 * 
 * @author nmjjm4
 * 
 */
public class DrawerHelper {
  
  public static final String DRAWER_PROMO_FRAG_NAME = "DRAWER_PROMO_CATEGORY_B";
  
  /**
   * Creates the drawer object for the silo specified.
   * 
   * @param siloObj
   *          - The repository item for the silo the drawer is to be built for.
   * @param limit
   *          - The physical maximum of columns supported by the drawer. This is hard capped at 4.
   * @param fcg
   *          - The filter group for the drawer.
   * @param inBTest
   *          - The users AB Test state, passed to the drawer to filter and reorder the categories in the list.
   * @return the drawer object.
   */
  public static DrawerContainer getDrawer(String siloId, int limit, FilterCategoryGroup fcg, String countryCode, boolean inBTest) {
    DrawerContainer dc = null;
    NMCategory siloObj = CategoryHelper.getInstance().getNMCategory(siloId, fcg, countryCode);
    if (siloObj != null) {
      dc = new DrawerContainer(limit, CategoryHelper.getInstance().getNMCategory(siloObj, fcg, countryCode), inBTest);
    }
    
    return dc;
  }
  
  /**
   * Each drawer has the ability to display one or more promotions at the bottom of the drawer. These promotions are configured using a fragment of the category tree. The category which holds the
   * promotions for the drawers is held in the DRAWER_PROMO_CATEGORY_B html fragment and is controlled through catman. Each child of this category should represent a silo on the tree. The child
   * category which maps to the silo is identified through name (i.e. the name of the silo category and the name of the promotional category MUST match identically in order for the promotions for the
   * silo drawer to be found).
   * 
   * This method will look up the root promotional category and store all the silo promotion categories in a map which is keyed off the name of the category.
   * 
   * @return a map of promotional categories keyed off silo display name.
   */
  public static Map<String, NMCategory> generateDrawerPromos(String countryCode) {
    
    Map<String, NMCategory> drawerPromos = new HashMap<String, NMCategory>();
    
    String categoryId = HtmlFragmentHelper.getInstance().getFragmentValue(DRAWER_PROMO_FRAG_NAME);
    if (categoryId != null) {
      
      NMCategory category = CategoryHelper.getInstance().getNMCategory(categoryId, null);
      if (category != null) {
        category.setCountryCode(countryCode);
        List<NMCategory> categories = category.getChildCategories();
        for (int i = 0; i < categories.size(); i++) {
          NMCategory child = categories.get(i);
          if (child.getDisplayName() != null) {
            child.setCountryCode(countryCode);
            drawerPromos.put(child.getDisplayName(), child);
          }
        }
        
      }
      
    }
    
    return drawerPromos;
  }
  
}
