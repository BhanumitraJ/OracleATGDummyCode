package com.nm.commerce.catalog;

import java.util.Comparator;
import java.util.SortedMap;
import java.util.TreeMap;

import atg.repository.RepositoryItem;

/********************************************************
 * @author Elaine Hopkins
 * @since 6/23/2006 This data holder accepts a RepositoryItem of type productSizeMap. The object is meant to represent one unique class type such as "tops". It will hold a list of all available sizes
 *        of all products of this type. The size list is stored in a SortedMap with the sortOrder value as the key so that it will naturally sort in ascending order by the key.
 ********************************************************/

public class FilterLabelResult implements Comparable {
  
  private RepositoryItem dataSource = null;
  private String classLabelId = "";
  private String sortOrder;
  private String webDisplayLabel = "";
  private SortedMap sizes = new TreeMap();
  
  /*************************************************************************
   * CONSTRUCTORS
   ************************************************************************/
  public FilterLabelResult() {}
  
  public FilterLabelResult(RepositoryItem sizeMapItem) {
    this.setDataSource(sizeMapItem);
    this.setClassLabelId((String) ((RepositoryItem) getDataSource().getPropertyValue("classLabel")).getPropertyValue("classLabelId"));
    this.setSortOrder((String) ((RepositoryItem) getDataSource().getPropertyValue("classLabel")).getPropertyValue("sortOrder"));
    this.setWebDisplayLabel((String) ((RepositoryItem) getDataSource().getPropertyValue("classLabel")).getPropertyValue("webDisplayLabel"));
    this.setSizes((String) ((RepositoryItem) getDataSource().getPropertyValue("sizeLabel")).getPropertyValue("sortOrder"),
            (String) ((RepositoryItem) getDataSource().getPropertyValue("sizeLabel")).getPropertyValue("sizeLabelId"));
    
  }
  
  // ****************************************************
  // ATTRIBUTE GETTERS AND SETTERS START HERE
  /*****************************************************
   * @return Returns the dataSource
   *****************************************************/
  public RepositoryItem getDataSource() {
    return this.dataSource;
  }
  
  /*****************************************************
   * @param dataSource
   *          The dataSource to set.
   *****************************************************/
  public void setDataSource(RepositoryItem dataSource) {
    if (dataSource != null) {
      this.dataSource = dataSource;
    }
  }
  
  /*****************************************************
   * @return Returns the classLabelId
   *****************************************************/
  public String getClassLabelId() {
    return this.classLabelId;
  }
  
  /*****************************************************
   * @param classLabelId
   *          The classLabelId to set.
   *****************************************************/
  public void setClassLabelId(String classLabelId) {
    if (classLabelId != null && classLabelId.length() > 0) {
      this.classLabelId = classLabelId;
    }
  }
  
  /*****************************************************
   * @return Returns the sortOrder
   *****************************************************/
  public String getSortOrder() {
    return this.sortOrder;
  }
  
  /*****************************************************
   * @param sortOrder
   *          The sortOrder to set
   *****************************************************/
  public void setSortOrder(String sortOrder) {
    if (sortOrder != null && sortOrder.length() > 0) {
      this.sortOrder = sortOrder;
    }
  }
  
  /*****************************************************
   * @return Returns the webDisplayLabel
   *****************************************************/
  public String getWebDisplayLabel() {
    return this.webDisplayLabel;
  }
  
  /*****************************************************
   * @param webDisplayLabel
   *          The webDisplayLabel to set.
   *****************************************************/
  public void setWebDisplayLabel(String webDisplayLabel) {
    if (webDisplayLabel != null && webDisplayLabel.length() > 0) {
      this.webDisplayLabel = webDisplayLabel;
    }
  }
  
  /*****************************************************
   * @return Returns the sizes map
   *****************************************************/
  public SortedMap getSizes() {
    return this.sizes;
  }
  
  /*****************************************************
   * @param sizeSortOrder
   *          The sortOrder to set in the sizes map
   * @param sizeLabelId
   *          The sizeLabelId to set in the sizes map This loads a SortedMap which should sort the entries according to the natural order of the keys which we are casting to the class of Integer to
   *          enforce correct ordering of numbers.
   *****************************************************/
  public void setSizes(String sizeSortOrder, String sizeLabelId) {
    Integer sizeSortOrderInt;
    try {
      if (sizeLabelId != null && sizeLabelId.length() > 0) {
        sizeLabelId = sizeLabelId;
      } else {
        sizeLabelId = "";
      }
      
      if (sizeSortOrder != null && sizeSortOrder.length() > 0) {
        sizeSortOrder = sizeSortOrder.trim();
        sizeSortOrderInt = new Integer(sizeSortOrder);
        
      } else {
        sizeSortOrderInt = new Integer(999);
      }
    } catch (Exception e) {
      sizeSortOrderInt = new Integer(999);
    }
    this.sizes.put(sizeSortOrderInt, sizeLabelId);
  }
  
  /*
   * (non-Javadoc)
   * 
   * @see java.lang.Comparable#compareTo(java.lang.Object)
   */
  public int compareTo(Object o) {
    // These items have no inherently comparable properties.
    // Use one of the enclosed Comparators
    return 0;
  }
  
  /*****************************************************
   * Method Name: SORT_ORDER_ASC Comparator used to sort by classLabel sortOrder
   *****************************************************/
  
  public static Comparator SORT_ORDER_ASC = new Comparator() {
    public int compare(Object filterLabelResult1, Object filterLabelResult2) {
      Integer labelResult1SortOrder = new Integer(((FilterLabelResult) filterLabelResult1).getSortOrder());
      Integer labelResult2SortOrder = new Integer(((FilterLabelResult) filterLabelResult2).getSortOrder());
      
      return labelResult1SortOrder.compareTo(labelResult2SortOrder);
    }
  };
  
}// end class
