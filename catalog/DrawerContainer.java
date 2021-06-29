package com.nm.commerce.catalog;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

import atg.nucleus.logging.ApplicationLogging;
import atg.nucleus.logging.ClassLoggingFactory;
import atg.repository.Repository;
import atg.repository.RepositoryException;

import com.nm.catalog.navigation.NMCategory;
import com.nm.catman.vo.SiloNavDrawerConfigVO;
import com.nm.components.CommonComponentHelper;
import com.nm.utils.DrawersBUtils;
import com.nm.utils.NmoUtils;

/**
 * 
 * @author nmjjm4
 * 
 */
public class DrawerContainer {
  
  private int limit = 4;
  private NMCategory silo;
  private List<DrawerColumn> drawer = new ArrayList<DrawerColumn>();
  private List<NMCategory> promos = new ArrayList<NMCategory>();
  private int columnCount = limit;
  private NMCategory promoCategory;
  private boolean intlDrawerBreaksEnabled;
  private HashSet<String> intlDrawerBreaks;
  private boolean inBTest; /* Remove this variable and associated method arguments after ABTest related to WR41211 is complete. */
  
  /** The m logger. */
  private static ApplicationLogging mLogger = ClassLoggingFactory.getFactory().getLoggerForClass(DrawerContainer.class);

  /**
   * Gets the logger.
   * 
   * @return the logger
   */
  private ApplicationLogging getLogger() {
    return mLogger;
  }
  
  /**
   * Constructor for the DrawerContainer object.
   * 
   * @param limit
   *          - The maximum number of columns supported by this drawer. Hard limit is 4.
   * @param silo
   *          - The silo the drawer will be created for.
   */
  public DrawerContainer(int limit, NMCategory silo, boolean inBTest) {
    if (limit > 4) {
      limit = 4;
    }
    
    this.limit = limit;
    this.silo = silo;
    this.inBTest = inBTest; /* Remove this variable and associated method arguments after ABTest related to WR41211 is complete. */
    
    if (!(silo.getCountryCode().equalsIgnoreCase("us"))) {
      
      String intlDrawerBreakStr = this.silo.getCountryCategoryAttributeMap().get("drawerColumnBreaksByLocale");
      
      if (intlDrawerBreakStr != null && !intlDrawerBreakStr.isEmpty()) {
        intlDrawerBreaksEnabled = true;
        intlDrawerBreaks = new HashSet<String>(Arrays.asList(intlDrawerBreakStr.split(",")));
      } else {
        intlDrawerBreaksEnabled = false;
      }
    }
    
    generateDrawerCategories();
    
    promoCategory = (NMCategory) DrawerHelper.generateDrawerPromos(silo.getCountryCode()).get(silo.getDisplayName());
    if (promoCategory != null && !promoCategory.getFlgImgAvailable()) {
      promos = promoCategory.getChildCategoriesByCount(this.limit);
    }
    
    columnCount = drawer.size();
    if (promos.size() > columnCount) {
      columnCount = promos.size();
    }
    
  }
  
  /**
   * Generates the columns for the drawer in a less then elegant fashion. The routine will iterate through each direct child of the silo category in order to load the columns. Categories are placed in
   * the current column until a category with the 'flgDrawerColumnBreak' flag is checked. This is the indication that the category should start a new column. For each child if the
   * 'flgDrawerExpandChild' flag is checked the sub-categories of the category will also be loaded into the column. Note that it is possible for the 'flgDrawerColumnBreak' flag to be set on a
   * sub-category as well as a category.
   * 
   * Regardless of the number of column break flags that may be checked, the system will stop creating columns at the limit passed to the original object.
   */
  private void generateDrawerCategories() {
    try {
      List<NMCategory> filteredCategories = silo.getChildCategories();
      Repository productRepository = (Repository) CommonComponentHelper.getProductRepository();
      List<SiloNavDrawerConfigVO> categories = DrawersBUtils.getSiloNavDrawerConfig(silo.getRepositoryId(), productRepository);
      DrawersBUtils.removeHiddenCategories(filteredCategories, inBTest);
      DrawerColumn column = new DrawerColumn();
      if (NmoUtils.isNotEmptyCollection(categories)) {
      for (SiloNavDrawerConfigVO siloNavDrawerConfig : categories) {
          if (null != siloNavDrawerConfig && !siloNavDrawerConfig.isFlgDrawerSuppress()) {
            for (NMCategory category : filteredCategories) {
              if (null != category && StringUtils.isNotBlank(siloNavDrawerConfig.getTopCategoryId()) && category.getId().equalsIgnoreCase(siloNavDrawerConfig.getTopCategoryId())) {
          if (siloNavDrawerConfig.isFlgDrawerColumnBreak()) {
            add(column);
            column = new DrawerColumn();
          }
          column.addCategory(category);
      
      List<NMCategory> items = new ArrayList<NMCategory>();
      items.add(category);
      DrawersBUtils.findPersCategories(items);
      
          if (siloNavDrawerConfig.isFlgDrawerExpandChild()) {
            List<NMCategory> subcategories = category.getChildCategories();
            /* Remove this code after ABTest related to WR41211 is complete. */
            if (inBTest) {
              DrawersBUtils.sortListByOrderOverride(subcategories);
            }
            DrawersBUtils.removeHiddenCategories(subcategories, inBTest);
        //show or hide personalized categories
        DrawersBUtils.findPersCategories(subcategories);
        
            for (int j = 0; j < subcategories.size(); j++) {
              NMCategory subcategory = subcategories.get(j);
              column.addChild(subcategory, category);
            }
          }
                break;
              }
        }
      }
        }
      if (!column.isAdded()) {
        add(column);
      }
      }
    } catch (RepositoryException repositoryException) {
      if (getLogger().isLoggingError()) {
        getLogger().logError("Exception occured while retreiving the silo category items : ", repositoryException);
      }
    }
  }
  
  private boolean isDrawerColumnBreak(NMCategory category) {
    if (intlDrawerBreaksEnabled) {
      return intlDrawerBreaks.contains(category.getId());
    } else {
      
      /* Remove this code after ABTest related to WR41211 is complete. */
      if (inBTest) {
        Map<String, String> categoryAttributes = category.getCategoryAttributeMap();
        if (categoryAttributes != null) {
          String columnBreakFlagString = categoryAttributes.get(DrawersBUtils.CATEGORY_COLUMN_BREAK);
          if (columnBreakFlagString != null) {
            return columnBreakFlagString.equals("true");
          }
        }
      }
      /* Remove this code after ABTest related to WR41211 is complete. */
      
      return category.getFlgDrawerColumnBreak();
    }
  }
  
  private boolean isExpandChildren(NMCategory category) {
    
    if (inBTest) {
      Map<String, String> categoryAttributes = category.getCategoryAttributeMap();
      if (categoryAttributes != null) {
        String expandChildrenFlagString = categoryAttributes.get(DrawersBUtils.CATEGORY_EXPAND_CHILDREN);
        if (expandChildrenFlagString != null) {
          return expandChildrenFlagString.equals("true");
        }
      }
    }
    
    return category.getFlgDrawerExpandChild();
  }
  
  /**
   * Adds a column to the drawer.
   * 
   * @param column
   *          - a new drawer column.
   */
  private void add(DrawerColumn column) {
    if (drawer.size() != limit && !column.getCategoryMap().isEmpty()) {
      column.setAdded(true);
      drawer.add(column);
    } else {
      column.setAdded(false);
    }
  }
  
  /**
   * Retrieve the columns associated with the drawer.
   * 
   * @return a list of drawer columns for this drawer.
   */
  public List<DrawerColumn> getColumns() {
    return drawer;
  }
  
  /**
   * Retrieve the silo object which this drawer was built for
   * 
   * @return the silo associated with the drawer
   */
  public NMCategory getSilo() {
    return silo;
  }
  
  /**
   * Retrieve the promo category associated with the drawer. This category promo will only be utilized if the flgImgAvailable flag is checked for the category.
   * 
   * @return the primary promo category for the drawer.
   */
  public NMCategory getPromoCategory() {
    return promoCategory;
  }
  
  /**
   * If the promo category is not used, child categories are checked for promotions. Any categories found will be contained in the list of promo categories
   * 
   * @return the list of child promotions available for a drawer if the primary promotion is not used.
   */
  public List<NMCategory> getPromos() {
    return promos;
  }
  
  /**
   * The total number of columns that this drawer will contain. This is either the column count of categories or the promotions, whichever is larger. The number of columns need to be known by the
   * interface in order for the drawer to size properly on the UI.
   * 
   * @return the number of physical columns that must be accounted for as either promos or categories.
   */
  public int getColumnCount() {
    return columnCount;
  }
}
