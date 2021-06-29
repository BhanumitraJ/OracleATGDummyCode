package com.nm.commerce.upsell;

import java.util.Comparator;

import com.nm.commerce.NMCommerceItem;
import atg.nucleus.GenericService;
import atg.repository.Repository;
import atg.repository.RepositoryException;
import atg.repository.RepositoryItem;

/**
 * Class Name: CommerceItemComparator This abstract base class compares any two commerceItems based on properties specified by the subclass.
 * 
 * @author C. Chadwick
 * @author $Author: nmmc5 $
 * @since 8/30/2004 Last Modified Date: $Date: 2008/04/29 13:58:36CDT $
 * @version $Revision: 1.2 $
 */
public abstract class CommerceItemComparator extends GenericService implements Comparator {
  
  /**
   * This method compares two commerceItems based on a property determined by a subclass. The class ranking is stored in the upsell repository
   * 
   * @param o1
   *          The first item to be compared
   * @param o2
   *          The second item to be compared
   * 
   * @return <0 if o1 is greater than o2. >0 if o1 is less than 02. =0 if they are equal
   * @throws ClassCastException
   *           if both the objects are not NMCommcerceItems
   */
  public int compare(Object o1, Object o2) {
    checkedLogDebug("CommerceItemComparator.compare()...");
    
    int comparisonResult = 0;
    NMCommerceItem ci1 = (NMCommerceItem) o1;
    NMCommerceItem ci2 = (NMCommerceItem) o2;
    
    if (ci1 == null) {
      throw new ClassCastException("Object 1 is not a NMCommerceItem");
    }
    
    if (ci2 == null) {
      throw new ClassCastException("Object 2 is not a NMCommerceItem");
    }
    
    RepositoryItem product1 = (RepositoryItem) ci1.getAuxiliaryData().getProductRef();
    RepositoryItem product2 = (RepositoryItem) ci2.getAuxiliaryData().getProductRef();
    
    String productProperty1 = (String) product1.getPropertyValue(getProductPropertyName());
    String productProperty2 = (String) product2.getPropertyValue(getProductPropertyName());
    
    if (((productProperty1 != null) && (productProperty1.length() > 0)) && ((productProperty2 != null) && (productProperty2.length() > 0))) {
      try {
        checkedLogDebug("productProperty1=" + productProperty1 + " productProperty2=" + productProperty2);
        
        RepositoryItem ranking1Item = getUpsellRepository().getItem(productProperty1, getRankingDescriptorName());
        RepositoryItem ranking2Item = getUpsellRepository().getItem(productProperty2, getRankingDescriptorName());
        
        if ((ranking1Item != null) && (ranking2Item != null)) {
          Integer ranking1 = (Integer) ranking1Item.getPropertyValue(getRankingPropertyName());
          Integer ranking2 = (Integer) ranking2Item.getPropertyValue(getRankingPropertyName());
          comparisonResult = ranking1.compareTo(ranking2);
        } else {
          checkedLogDebug("One or both of the rankingItems were null so the comparison could not occur");
        }
      } catch (RepositoryException re) {
        comparisonResult = 0;
        if (isLoggingError()) {
          logError("A repository exception occurred while trying to compare commerce items", re);
        }
      }
    } else if (productProperty1 == null && productProperty2 != null) {
      // If product 1 doesn't have a product Property, it is a lower rank
      comparisonResult = 1;
    } else if (productProperty2 == null && productProperty1 != null) {
      // If product 2 doesn't have a product Property, it is a lower rank
      comparisonResult = -1;
    }
    
    return comparisonResult;
  }
  
  /**
   * Getter for the upsellRepository
   * 
   * @return Returns the upsellRepository.
   */
  public Repository getUpsellRepository() {
    return upsellRepository;
  }
  
  /**
   * Setter for the upsellRepository
   * 
   * @param upsellRepository
   *          The upsellRepository to set.
   */
  public void setUpsellRepository(Repository upsellRepository) {
    this.upsellRepository = upsellRepository;
  }
  
  /**
   * Convenience method. Checks for isLoggingDebug and then logs it
   * 
   * @param message
   */
  protected void checkedLogDebug(String message) {
    if (isLoggingDebug()) {
      logDebug(message);
    }
    
  }
  
  /**
   * Returns the property of the product that should be compared. For example, if the products should be compared based on cmos_class then the value of this should be
   * ProductCatalogRepository.PRODUCT_CMOS_ITEM_CODE so that value can be fetched from the product.
   * 
   * @return productPropertyname
   */
  abstract protected String getProductPropertyName();
  
  /**
   * Returns the ranking property name. This is the name of the ranking property of the ranking item.
   * 
   * @return rankingPropertyName
   */
  abstract protected String getRankingPropertyName();
  
  /**
   * Returns the ranking descriptor name. This it the name of the ranking item descriptor for this type of value.
   * 
   * @return rankingDescriptorName
   */
  abstract protected String getRankingDescriptorName();
  
  private Repository upsellRepository;
  
}
