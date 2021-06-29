package com.nm.commerce.catalog;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.nm.catalog.navigation.NMCategory;

/**
 * The DrawerColumn is a simple container for all categories and their children contained within a column. The categories and their children are held in a map retrieved by getCategoryMap. The key to
 * the map will be the direct child categories of the silo with the value being children if present. This object does not contain any signficant logic itself and is populated primarily by the
 * DrawerContainer object which holds it.
 * 
 * @author nmjjm4
 * 
 */
public class DrawerColumn {
  
  private Map<NMCategory, List<NMCategory>> list = new LinkedHashMap<NMCategory, List<NMCategory>>();
  
  private boolean isAdded = false;
  
  /**
   * Set if the column has been added to the drawer.
   * 
   * @return if the column is added to the drawer
   */
  public boolean isAdded() {
    return isAdded;
  }
  
  /**
   * Indicates that this column has been to the drawer.
   * 
   * @param isAdded
   *          - set when the column is added
   */
  public void setAdded(boolean isAdded) {
    this.isAdded = isAdded;
  }
  
  private boolean isBrokenStart = false;
  
  /**
   * Retrieves the broken start flag. This indicates to the UI that a column breaks on a subcategory rather than a parent.
   * 
   * @return the broken start flag value
   */
  public boolean getBrokenStart() {
    return isBrokenStart;
  }
  
  /**
   * Sets the broken start flag. This is only set when the column is broken on a subcategory rather than a parent category.
   */
  public void setBrokenStart() {
    isBrokenStart = true;
  }
  
  /**
   * Retrieves the map of categories and their children associated with the drawer column.
   * 
   * @return map of categories and subcategories keyed on the parent category object.
   */
  public Map<NMCategory, List<NMCategory>> getCategoryMap() {
    return list;
  }
  
  /**
   * Adds a new category to the column
   * 
   * @param category
   *          - the category add
   */
  public void addCategory(NMCategory category) {
    list.put(category, new ArrayList<NMCategory>());
  }
  
  /**
   * Adds a new child category to the parent specified
   * 
   * @param category
   *          - the new category
   * @param parent
   *          - the parent for the new category
   */
  public void addChild(NMCategory category, NMCategory parent) {
    list.get(parent).add(category);
  }
  
}
