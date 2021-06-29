package com.nm.commerce.pagedef.evaluator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

import atg.nucleus.Nucleus;
import atg.nucleus.logging.ApplicationLogging;
import atg.nucleus.logging.ClassLoggingFactory;
import atg.repository.RepositoryException;
import atg.repository.RepositoryItem;

import com.nm.catalog.navigation.CategoryHelper;
import com.nm.ajax.myfavorites.utils.MyFavoritesUtil;
import com.nm.catalog.navigation.NMCategory;
import com.nm.collections.DesignerIndexer;
import com.nm.collections.DesignerIndexer.Category;
import com.nm.collections.DesignerIndexer.Designer;
import com.nm.commerce.pagedef.model.DesignerIndexPageModel;
import com.nm.commerce.pagedef.model.EndecaDesignerIndexPageModel;
import com.nm.commerce.pagedef.model.PageModel;

public class DesignerIndexUtils {
  
  public static final String ALTERNATE_PAGE_DEFINITION_PATH_ALPHA = "/nm/commerce/pagedef/template/SiloIndexDrawer"; // page definition for BOB Designer page, from Designer Drawer landing
  public static final String ALPHA_INDEXER_PATH = "/nm/collections/DesignerIndexer";
  private static ApplicationLogging logger = ClassLoggingFactory.getFactory().getLoggerForClass(DesignerIndexUtils.class);
  
  /**
   * find and return the Category object that is the category that user requested to view designer list If user request root category, i.e., view all categories, this method doesn't apply
   * 
   * @param categories
   * @param categoryId
   * @return
   */
  public static DesignerIndexer.Category getActiveDesignerCategory(List<DesignerIndexer.Category> categories, String categoryId) {
    DesignerIndexer.Category activeDesignerCategory = null;
    Iterator<DesignerIndexer.Category> categoryItor = categories.iterator();
    DesignerIndexer.Category tempCategory = null;
    while (categoryItor.hasNext()) {
      tempCategory = (DesignerIndexer.Category) categoryItor.next();
      if (tempCategory.getId().equals(categoryId)) {
        activeDesignerCategory = tempCategory;
        break;
      }
    }
    return activeDesignerCategory;
  }
  
  /**
   * set the alpha designer list to page model
   * 
   * @param model
   * @param indexer
   * @param categoryGroup
   */
  public static void renderAlphaList(PageModel model, DesignerIndexer indexer, String categoryGroup) {
    if (model instanceof DesignerIndexPageModel) {
      DesignerIndexPageModel dModel = (DesignerIndexPageModel) model;
      dModel.setIsByCategory(false);
      Map<String, List<Designer>> alphaDesigners = (HashMap<String, List<Designer>>) indexer.getAlphaDesignerMap().get(categoryGroup);
      Map<String, List<String>> alphaDesignersCountryRestriction = (HashMap<String, List<String>>) indexer.getAlphaDesignerRestrictedCountryMap().get(categoryGroup);
      dModel.setAlphaDesignerMap(alphaDesigners);
      dModel.setAlphaDesignerCountryRestrictionMap(alphaDesignersCountryRestriction);
      
    } else if (model instanceof EndecaDesignerIndexPageModel) {
      EndecaDesignerIndexPageModel eModel = (EndecaDesignerIndexPageModel) model;
      eModel = (EndecaDesignerIndexPageModel) model;
      Map<String, List<Designer>> alphaDesigners = (HashMap<String, List<Designer>>) indexer.getAlphaDesignerMap().get(categoryGroup);
      eModel.setAlphaDesignerMap(alphaDesigners);
    }
  }
  
  // Filter out designers that aren't related to the particular silo.
  public static Map<String, List<Designer>> filterAlphaMapBySilo(DesignerIndexer indexer, String groupCode, String siloId) {
    Map<String, Map<String, List<Designer>>> alphaDesignerMap = indexer.getAlphaDesignerMap();
    Map<String, List<Designer>> filteredLetterMap = null;
    if (alphaDesignerMap != null) {
      Map<String, List<Designer>> letterMap = alphaDesignerMap.get(groupCode);
      filteredLetterMap = new LinkedHashMap<String, List<Designer>>();
      if (letterMap == null) {
        return null;
      }
      for (String key : DesignerIndexer.ALPHA_LIST) {
        List<Designer> allDesigners = letterMap.get(key);
        if (allDesigners == null) {
          continue;
        }
        List<Designer> filteredDesigners = new LinkedList<Designer>();
        boolean hasLetter = false;
        for (Designer designer : allDesigners) {
          if (designer.getDesignerIndexSiloId().equals(siloId)) {
            filteredDesigners.add(designer);
            hasLetter = true;
          }
        }
        if (hasLetter) {
          filteredLetterMap.put(key, filteredDesigners);
        }
      }
      
    }
    return filteredLetterMap;
  }
  
  /**
   * Filter Favorite designers.
   * 
   * @param indexer
   *          designer indexer
   * @param groupCode
   *          all'#'
   * @param favList
   *          fav designers id list
   * @return list of favorite designer objects.
   */
  public static List<Designer> filterAlphaMapByFavId(DesignerIndexer indexer, String groupCode, List<String> favList) {   
    List<Designer> filteredDesigners = new LinkedList<Designer>();    
    List<Designer> allAlphaSortedDesigners = new LinkedList<Designer>();
    
    Map<String, Map<String, List<Designer>>> alphaDesignerMap = indexer.getAlphaDesignerMap();    
    List<String> favDesignerFound=new ArrayList<String>();
    ArrayList<String> favDesignersList = new ArrayList<String>();

    //get the designer object for the given fav id from alphaDesignerMap. If not found get the object from categoryDesignersMap. 
    if (alphaDesignerMap != null) {
    	Map<String, List<Designer>> letterMap = alphaDesignerMap.get(groupCode);
    	if (letterMap != null) {
    		for (String key : DesignerIndexer.ALPHA_LIST) {
    			List<Designer> allDesigners = letterMap.get(key);
    			if (allDesigners == null) {
    				continue;
    			}
    			for (String favId : favList) {
    				for (Designer designer : allDesigners) {
    					if (designer.getItemId() != null && designer.getItemId().equals(favId)) {
    						if (!favDesignersList.contains(designer.getSeoDesignerName().toUpperCase())) {
    							filteredDesigners.add(designer);
    							favDesignerFound.add(favId);
    							favDesignersList.add(designer.getSeoDesignerName().toUpperCase());    							
    							break;
    						}
    					}
    				}
    			}
    		}
    	}
    }

    if (favDesignerFound.size() != favList.size()) {
    	List<String> newList = new ArrayList<String>();
    	for (String favId : favList) {
    		if (!favDesignerFound.contains(favId)) {
    			newList.add(favId);
    		}
    	}
		System.out.println("MYNM-472 newList-"+newList);
    	Map<String, List<Category>> categoryHierarchyMap = indexer.getCategoryHierarchyMap();
    	List<DesignerIndexer.Category> categories = categoryHierarchyMap.get(groupCode);
    	Map<String, Map<String, List<Designer>>> categoryDesignerMap = indexer.getCategoryDesignerMap();
    	Map<String, List<DesignerIndexer.Designer>> categoryDesigners = categoryDesignerMap.get(groupCode);

    	for (DesignerIndexer.Category category : categories) {
    		List<DesignerIndexer.Designer> designers = categoryDesigners.get(category.getId());
    		if (designers != null) {
    			for (String favId : newList) {
    				for (DesignerIndexer.Designer designer : designers) {
    					if (designer.getItemId() != null && designer.getItemId().equals(favId)) {
    						if (!favDesignersList.contains(designer.getSeoDesignerName().toUpperCase())) {
    							filteredDesigners.add(designer);
    							favDesignersList.add(designer.getSeoDesignerName().toUpperCase());
    							break;
    						}
    					}
    				}
    			}
    		}
    	}
		System.out.println("MYNM-472 favDesignersList-"+favDesignersList);
    }

    allAlphaSortedDesigners.addAll(filteredDesigners);
    Map<String, List<Designer>> alphaDesigners=new LinkedHashMap<String, List<Designer>>(); 
    alphaDesigners.put("#", allAlphaSortedDesigners);
    DesignerIndexer.sortMap(alphaDesigners);
    return alphaDesigners.get("#");
  }
  
  // Filter out other silos' By Categories and those that have no designer results.
  public static List<DesignerIndexer.Category> filterCategoryListBySilo(DesignerIndexer indexer, String groupCode, String siloId) {
    Map<String, List<Category>> categoryHierarchyMap = indexer.getCategoryHierarchyMap();
    Map<String, Map<String, List<Designer>>> categoryDesignerMap = indexer.getCategoryDesignerMap();
    List<DesignerIndexer.Category> filteredCats = null;
    if (categoryHierarchyMap != null) {
      List<DesignerIndexer.Category> categories = categoryHierarchyMap.get(groupCode);
      Map<String, List<DesignerIndexer.Designer>> categoryDesigners = categoryDesignerMap.get(groupCode);
      filteredCats = new LinkedList<DesignerIndexer.Category>();
      for (DesignerIndexer.Category category : categories) {
        boolean addCategory = false;
        if (category.getDesignerIndexSiloId().equals(siloId)) {
          addCategory = true;
          if (categoryDesigners.get(category.getId()) == null || categoryDesigners.get(category.getId()).isEmpty()) {
            addCategory = false;
          }
          
          // Keep category if correct silo and childCategories have designer results.
          List<NMCategory> childCategories = category.getChildCategories();
          if (childCategories != null) {
            for (NMCategory childCat : childCategories) {
              if (categoryDesigners.get(childCat.getId()) != null && !categoryDesigners.get(childCat.getId()).isEmpty()) {
                addCategory = true;
              }
            }
          }
        }
        
        if (addCategory) {
          filteredCats.add(category);
        }
      }
    }
    return filteredCats;
  }
  
  /**
   * Get the cached designer indexer object.
   * 
   * @return a {@link DesignerIndexer} singleton object.
   */
  public static DesignerIndexer getDesignerIndexer() {
    DesignerIndexer designerIndexer = (DesignerIndexer) Nucleus.getGlobalNucleus().resolveName(ALPHA_INDEXER_PATH);
    designerIndexerReadiness(designerIndexer);
    return designerIndexer;
  }
  
  /**
   * check is DesignerIndexer finish indexing with maximum waiting time.
   * 
   * @param designerIndexer
   */
  public static void designerIndexerReadiness(DesignerIndexer designerIndexer) {
    int maxWaitTime = designerIndexer.getMaxWebWaitTime() * 1000;
    int checkingInterval = designerIndexer.getWebCheckInterval() * 1000;
    int totalWaitTime = 0;
    while (!designerIndexer.hasRunOnce() && totalWaitTime < maxWaitTime) {
      try {
        totalWaitTime = totalWaitTime + checkingInterval;
        Thread.sleep(checkingInterval);
      } catch (Exception e) {
        logger.logError(e);
      }
    }
  }

 /** This method fetches the recommended designers from database which is configured in CM2 by merchant and removes the overlapping id's with user favorited designers.
  * Also it fetches the designer object for the designer id's.
 * @param indexer designer indexer
 * @param favDesigners user favorited designers
 * @return List of recommended designer objects
 */
public static List<Designer> getRecommendedDesigners(final DesignerIndexer indexer, final List<String> favDesigners) {
		RepositoryItem[] rItems;
		final List<String> recommendedDesigners = new ArrayList<String>();
		final List<String> favDesNames = new ArrayList<String>();
		for (final String desId : favDesigners) {
			String designerDisplayName = MyFavoritesUtil.getInstance().getDesignerName(desId).trim().toUpperCase();
			if (StringUtils.isNotEmpty(designerDisplayName)) {
				favDesNames.add(designerDisplayName);
			}
		}
		try {
			rItems = MyFavoritesUtil.getInstance().getDesigners("recommendedDesigners", "getRecommendedDesigners");
			for (final RepositoryItem rep : rItems) {
				final String designerId = (String) rep.getPropertyValue("designerId");
				String recomDesName = MyFavoritesUtil.getInstance().getDesignerName(designerId).trim();
				
				if (StringUtils.isNotEmpty(recomDesName)) {
					if (!favDesNames.contains(recomDesName.toUpperCase())) {
						recommendedDesigners.add(designerId);
					}
				}
			}
		} catch (final RepositoryException e) {
			logger.logError(e);
		}
		return filterAlphaMapByFavId(indexer, "#", recommendedDesigners);
	}
}
