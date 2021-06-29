package com.nm.commerce.promotion;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Iterator;
import java.util.Random;
import java.util.Set;

import atg.nucleus.GenericService;
import atg.nucleus.ServiceException;
import atg.repository.RepositoryItem;
import com.nm.components.CommonComponentHelper;
import EDU.oswego.cs.dl.util.concurrent.CopyOnWriteArrayList;

/**
 * UpSellRandomizer component
 * 
 * This Dynamo component returns a unique randomized list of products associated with a given list of parent categories. It ensures that the list of randomized products does not contain entries
 * specified in the "productsToExclude" list (populated originally from the shopping cart), and that the randomized list itself contains no redundancies
 * 
 * @author Anthony Barnes
 * 
 */
public class UpSellRandomizer extends GenericService {
  
  public UpSellRandomizer() {
    // nothing to do
    super();
  }
  
  /**
   * Runs when the component starts up
   */
  public void doStartService() throws ServiceException {
    setDirty(); // Set the cache to dirty initially.
    super.doStartService();
  }
  
  /**
   * Runs when the component is stopped
   */
  public void doStopService() throws ServiceException {
    // Nothing to do
    super.doStopService();
  }
  
  /**
   * get default category property, used if a list of categories is not originally specifed.
   * 
   * @return default category for a particular brand
   */
  public String getDefaultTopSellerCategory() {
    return defaultCat_;
  }
  
  /**
   * sets default category property, used if a list of categories is not originally specifed. Set in a Dynamo component properties file, per brand
   * 
   * @param cat
   *          default category value
   */
  public void setDefaultTopSellerCategory(String cat) {
    defaultCat_ = cat;
    
    // create initial categories to include list from this category
    setCategoriesToInclude(Collections.nCopies(5, defaultCat_));
    
    // create blank products to exclude list
    setProductsToExclude(new HashSet());
  }
  
  /**
   * get categories to include property. Each entry in this list represents a "slot" in the upsell list on the shopping cart page
   * 
   * @return list of categories to search for randomized products
   */
  public List getCategoriesToInclude() {
    if (isLoggingDebug()) logDebug("invoking getCategoriesToInclude");
    
    return catsToInclude_;
  }
  
  /**
   * set categories to include property.
   * 
   * @param categories
   *          list of categories to search for randomized products
   */
  public void setCategoriesToInclude(List categories) {
    // if category list is empty, populate each 'slot' with the default
    // category (brand specific)
    if (categories.size() == 0) {
      catsToInclude_ = new CopyOnWriteArrayList(Collections.nCopies(5, defaultCat_));
    } else {
      catsToInclude_ = new CopyOnWriteArrayList(categories);
    }
    
    // initialize the products list array
    catProds_ = new Object[catsToInclude_.size()];
    
    if (isLoggingDebug()) logDebug("categories to include are: " + categories);
  }
  
  /**
   * set products to exclude property
   * 
   * @param products
   */
  public void setProductsToExclude(Set products) {
    prodsToExclude_ = products;
    
    if (isLoggingDebug()) logDebug("products to exclude are: " + products);
  }
  
  /**
   * get list of products belonging to a particular category
   * 
   * @param index
   *          category 'slot' whose products we'd like to retrieve
   * @return list of products associated with specified category
   */
  public List<RepositoryItem> getCategoryToProducts(int index) {
    if (isLoggingDebug()) logDebug("invoking getCategoryToProducts(" + index + ")");
    
    // return (List)catProds_[index];
    
    List tempCatProdList = (List) catProds_[index];
    
    List<RepositoryItem> returnCatProds_ = new ArrayList<RepositoryItem>();
    
    if (tempCatProdList != null) {
      Iterator tempCatProdListIterator = tempCatProdList.iterator();
      
      try {
        
        while (tempCatProdListIterator.hasNext()) {
          
          String productId = (String) tempCatProdListIterator.next();
          
          if (productId != null) {
            returnCatProds_.add(CommonComponentHelper.getProdSkuUtil().getProductRepositoryItem(productId));
          }
          
        }
        
      } catch (Exception e) {
        logError(e);
      }
    }
    
    if (isLoggingDebug()) {
      logDebug("returnCatProds_-->" + returnCatProds_ + "<--");
    }
    
    return returnCatProds_;
  }
  
  /**
   * set list of products belonging to a particular category
   * 
   * @param index
   *          category 'slot' whose products we'd like to set
   * @param obj
   *          list of products to associate with specified category
   */
  public void setCategoryToProducts(int index, List obj) {
    if (isLoggingDebug()) logDebug("invoking setCategoryToProducts(" + index + "," + obj + ")");
    
    List tempObjList = new ArrayList();
    
    if (obj != null) {
      Iterator objIterator = obj.iterator();
      
      while (objIterator.hasNext()) {
        RepositoryItem productRI = (RepositoryItem) objIterator.next();
        
        if (productRI != null) {
          tempObjList.add(productRI.getRepositoryId());
        }
      }
    }
    
    catProds_[index] = tempObjList;
  }
  
  /**
   * Toggle the randomizer to generate newly randomized list on next invokation from shopping cart page. Typically set as result of a shopping cart scenario firing
   */
  public void setDirty() {
    dirty_ = true;
  }
  
  public List<RepositoryItem> getCachedRandProdsRI() {
    
    List<RepositoryItem> returnCachedRandProds_ = new ArrayList<RepositoryItem>();
    Iterator cachedRandProds_Iterator = cachedRandProds_.iterator();
    
    try {
      while (cachedRandProds_Iterator.hasNext()) {
        
        String productId = (String) cachedRandProds_Iterator.next();
        
        if (productId != null) {
          returnCachedRandProds_.add(CommonComponentHelper.getProdSkuUtil().getProductRepositoryItem(productId));
        }
        
      }
    } catch (Exception e) {
      logError(e);
    }
    
    if (isLoggingDebug()) {
      logDebug("returnCachedRandProds_-->" + returnCachedRandProds_ + "<--");
    }
    
    return returnCachedRandProds_;
  }
  
  public void setCachedRandProds(List repositoryItems) {
    
    if (cachedRandProds_ == null) cachedRandProds_ = new CopyOnWriteArrayList();
    
    cachedRandProds_.clear();
    
    if (repositoryItems != null) {
      Iterator repositoryItemsIterator = repositoryItems.iterator();
      
      while (repositoryItemsIterator.hasNext()) {
        RepositoryItem productRI = (RepositoryItem) repositoryItemsIterator.next();
        
        if (productRI != null) {
          cachedRandProds_.add(productRI.getRepositoryId());
        }
      }
    }
    
    if (isLoggingDebug()) {
      logDebug("setCachedRandProds-->" + cachedRandProds_ + "<---");
    }
    
  }
  
  public List getCachedRandProds() {
    
    if (cachedRandProds_ == null) cachedRandProds_ = new CopyOnWriteArrayList();
    
    return cachedRandProds_;
  }
  
  /**
   * the magical randomizer function.
   * 
   * @return list of products, randomized per category 'slot'
   */
  public List getRandomizedProducts() {
    // if shopping cart hasn't been modified, return previously cached
    // randomized list
    if (!dirty_) {
      return getCachedRandProdsRI();
    }
    
    // initialize return list
    setCachedRandProds(new ArrayList());
    
    // if this component hasn't been given a categories list yet,
    // build one, using the (brand/specific) default category for each slot
    if (catsToInclude_.size() == 0) {
      setCategoriesToInclude(Collections.nCopies(5, defaultCat_));
      setProductsToExclude(new HashSet());
    }
    
    if (isLoggingDebug()) {
      logDebug("there are " + catsToInclude_.size() + " categories in catsToInclude");
      logDebug("there are " + catProds_.length + " categories in catProdMap");
    }
    
    // temp list of products to exclude. need this because aw we
    // as we find each randomized product, the randomize product itself must be placed
    // in productsToExclude list to prevent redundant products from showing in all slots
    HashSet currentProdsToExclude = new HashSet(prodsToExclude_);
    
    try {
      // for each category 'slot'
      for (int i = 0; i < catsToInclude_.size(); i++) {
        // get category ID
        String category = (String) catsToInclude_.get(i);
        if (isLoggingDebug()) logDebug("finding products in category " + category);
        
        // get list of products belonging to category in slot
        List prodList = (List) getCategoryToProducts(i);
        
        if (isLoggingDebug()) logDebug("products are " + prodList);
        
        // it's hammer time! let's get a random product from this list
        if (prodList != null && prodList.size() > 0) {
          // safely cast the products list
          RepositoryItem[] products = (RepositoryItem[]) prodList.toArray(new RepositoryItem[0]);
          
          if (isLoggingDebug()) logDebug("found " + products.length + " products in category " + category);
          
          // keep a record of indices we've already traversed
          BitSet markedIndices = new BitSet(products.length);
          
          boolean foundProduct = false;
          while (!foundProduct) {
            // if we've scanned the entire product list and no product is usable,
            // we're outa here
            if (!areBitsAvailable(markedIndices, products.length)) {
              break;
            }
            
            // find random product in category
            int randIndex = randomizer_.nextInt(products.length);
            if (isLoggingDebug()) logDebug("randIndex is " + randIndex);
            
            RepositoryItem product = products[randIndex];
            
            Boolean boolDisplay = (Boolean) product.getPropertyValue("flgDisplay");
            String product_id = (String) product.getPropertyValue("id");
            
            if (isLoggingDebug()) logDebug("examining product " + product_id);
            
            // if random product is already in shopping cart or is not displayable,
            // mark it as unusable and find another one
            markedIndices.set(randIndex);
            if (!currentProdsToExclude.contains(product_id) && boolDisplay.booleanValue()) {
              getCachedRandProds().add(product.getRepositoryId());
              currentProdsToExclude.add(product_id);
              foundProduct = true;
            } else {
              if (isLoggingDebug()) logDebug("product " + product_id + " is either in exclude list, or is undisplayable");
            }
          }
        }
      }
    } catch (Exception ex) {
      logDebug(ex);
    }
    
    // got our randomized list. on to glory...
    dirty_ = false;
    return getCachedRandProdsRI();
  }
  
  // are empty positions still available in this BitSet?
  private boolean areBitsAvailable(BitSet set, int bitSetSize) {
    for (int i = 0; i < bitSetSize; i++) {
      boolean isPopulated = set.get(i);
      
      if (!isPopulated) {
        if (isLoggingDebug()) logDebug("the non populated big is " + i);
        
        return true;
      }
    }
    
    return false;
  }
  
  // category-to-products mapping. each index in array represents category 'slot'
  // each value in array is a list of products associated with category slot
  private Object[] catProds_;
  
  // list of categories to show in slots
  private CopyOnWriteArrayList catsToInclude_;
  
  // list of products to exclude from randomized product list
  private Set prodsToExclude_;
  
  // default category to use for randomized product list, if no others are specified
  private String defaultCat_;
  
  // does randomization funtion really require invoking? (toggled if add_to/remove_from shopping cart
  // scenario is fired)
  private boolean dirty_;
  
  // cached list of previously randomized products.
  private CopyOnWriteArrayList cachedRandProds_;
  
  // randomizer object. i figure this this expensive, so it's a class var
  private static final Random randomizer_ = new Random();
}
