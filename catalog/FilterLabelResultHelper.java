package com.nm.commerce.catalog;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import atg.repository.Repository;
import atg.repository.RepositoryException;
import atg.repository.RepositoryItem;

import com.nm.components.CommonComponentHelper;
import com.nm.utils.GenericLogger;
import com.nm.utils.ProdSkuUtil;

/********************************************************
 * @author Elaine Hopkins
 * @since 6/23/2006 This helper class accepts a set of repository items of type productSizeMap through it's get method. It will iterate through all of the sizeMaps and create a FilterLabelResult
 *        object for each unique classLabelId and load each with their corresponding sizes. The FilterLabelResult objects will be stored/returned in a HashMap with keys that correspond to the unique
 *        types.
 * 
 ********************************************************/

public class FilterLabelResultHelper {
  
  ProdSkuUtil prodSkuUtil = CommonComponentHelper.getProdSkuUtil();
  
  /*********************************************
   * CONSTRUCTOR
   *********************************************/
  public FilterLabelResultHelper() {}
  
  /*****************************************************
   * @param Set
   *          (RepositoryItems)
   * @return filterDisplayMap Returns the HashMap of FilterLabelResult objects
   *****************************************************/
  public HashMap getFilterLabelResultMap(Set sizeMaps) {
    
    HashMap filterLabelResultMap = new HashMap();
    FilterLabelResult filterLabelResultItem = null;
    FilterLabelResult allLabelResultItem = null;
    Repository productRepository = CommonComponentHelper.getProdSkuUtil().getProductRepository();
    RepositoryItem sizeMapItem = null;
    
    if (sizeMaps != null && sizeMaps.size() > 0) {
      Iterator sizeMapsIterator = sizeMaps.iterator();
      
      while (sizeMapsIterator.hasNext()) {
        
        sizeMapItem = (RepositoryItem) sizeMapsIterator.next();
        String classLabelId = (String) ((RepositoryItem) sizeMapItem.getPropertyValue("classLabel")).getPropertyValue("classLabelId");
        String classSortOrder = (String) ((RepositoryItem) sizeMapItem.getPropertyValue("classLabel")).getPropertyValue("sortOrder");
        String sizeLabelId = (String) ((RepositoryItem) sizeMapItem.getPropertyValue("sizeLabel")).getPropertyValue("sizeLabelId");
        String sizeSortOrder = (String) ((RepositoryItem) sizeMapItem.getPropertyValue("sizeLabel")).getPropertyValue("sortOrder");
        
        if (filterLabelResultMap.containsKey(classLabelId)) {
          // Add new size to existing FilterLabelResult object
          ((FilterLabelResult) filterLabelResultMap.get(classLabelId)).setSizes(sizeSortOrder, sizeLabelId);
          
        } else {
          // Create new FilterLabelResult object and load the current size
          filterLabelResultItem = new FilterLabelResult(sizeMapItem);
          
          // Create new entry in filterLabelResultMap
          filterLabelResultMap.put(classLabelId, filterLabelResultItem);
        }
        
        // Maintain a filterLabelResultMap key entry of "ALL" that stores a filterLabelResult object with all sizes
        // Assume that the "ALL" record is always in NM_CLASS_LABEL table with class_label_id of "ALL0" (zero)
        // URL code at page level depends on the class_label_id being the value "ALL0"
        // The display_name, web_display_label and sort_order for the "ALL" record can still be changed by merchant so pull their values dynamically
        try {
          RepositoryItem allClassItem = null;
          allClassItem = (RepositoryItem) productRepository.getItem("ALL0", "classLabel");
          
          if (filterLabelResultMap.containsKey("ALL0")) {
            // Add new sizes to the existing filterLabelResult object
            
            ((FilterLabelResult) filterLabelResultMap.get("ALL0")).setSizes(sizeSortOrder, sizeLabelId);
          } else {
            // Create new filterLabelResult object for "ALL"
            // "ALL" is NOT a mapped entry of the NM_PRODUCT_SIZE_MAP so the sizes must set manually for each sizeMap loop
            allLabelResultItem = new FilterLabelResult();
            allLabelResultItem.setClassLabelId("ALL0");
            allLabelResultItem.setSortOrder((String) allClassItem.getPropertyValue("sortOrder"));
            allLabelResultItem.setWebDisplayLabel((String) allClassItem.getPropertyValue("webDisplayLabel"));
            allLabelResultItem.setSizes(sizeSortOrder, sizeLabelId);
            
            // Load new entry in filterLabelResultMap
            filterLabelResultMap.put("ALL0", allLabelResultItem);
          }
          
        } catch (RepositoryException re) {
          GenericLogger log = CommonComponentHelper.getLogger();
          log.debug("FilterLabelResultHelper - Unable to get repository item of type classLabel - " + re);
        }
      }// end while
      
      // Besides type "ALL", if there is only one other filter type available (i.e. tops), then make that the type the default and remove "ALL"
      if (!(filterLabelResultMap.size() > 2)) {
        // Remove the "ALL" filterLabelResult object leaving only the one available type being passed to the filterType drop down for display
        filterLabelResultMap.remove("ALL0");
      } else {
        // Multiple filter types are available, show each of them and the "ALL" type in the filterType dropdown
      }
    }// end sizeMaps null check
    
    return filterLabelResultMap;
  }// end getFilterLabelResultMap
  
}// end class
