package com.nm.commerce.pagedef.evaluator.template;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.servlet.jsp.PageContext;

import net.jawr.web.util.StringUtils;
import atg.repository.Query;
import atg.repository.QueryBuilder;
import atg.repository.RepositoryException;
import atg.repository.RepositoryItem;
import atg.repository.RepositoryView;
import atg.userprofiling.ProfileTools;

import com.nm.catalog.navigation.NMCategory;
import com.nm.collections.comparators.FavoriteItemDateOrIdComparator;
import com.nm.commerce.catalog.DynamicChildProductsCategory;
import com.nm.commerce.pagedef.definition.PageDefinition;
import com.nm.commerce.pagedef.evaluator.ProductTemplatePageEvaluator;
import com.nm.components.CommonComponentHelper;
import com.nm.utils.EncryptDecrypt;
import com.nm.utils.ProdSkuUtil;

public class FavoritesPageEvaluator extends ProductTemplatePageEvaluator {
  
  public boolean evaluate(PageDefinition pageDefinition, PageContext pageContext) throws Exception {
    return super.evaluate(pageDefinition, pageContext);
  }
  
  protected NMCategory getCategory() throws IOException {
    NMCategory categoryItem = getCategoryHelper().getNMCategory(getRequest().getParameter("itemId"), getBrandSpecs().getRootCategoryId());
    if (categoryItem != null) {
      String favoritesListId = getRequest().getParameter("favoritesListId");
      List<RepositoryItem> favoriteItems = null;
      
      if (StringUtils.isNotEmpty(favoritesListId)) {    	  
    	  favoritesListId = EncryptDecrypt.decrypt(favoritesListId.trim());
    	  
    	  if(favoritesListId.startsWith("mynm")){
    		  favoriteItems = getEmailFavorites(favoritesListId);
    	  }
    	  else{
    		  favoriteItems = getSortedFavoriteProducts(favoritesListId);
    	  }      

      } else {
    	  favoriteItems = getSortedProfileFavoriteProducts();
      }
      categoryItem = new DynamicChildProductsCategory(categoryItem, favoriteItems);
    }
    
    return categoryItem;
  }
  
  private List<RepositoryItem> getEmailFavorites(String favoritesListId) {
	  RepositoryItem subset = null;
	  List<RepositoryItem> favoriteProducts = new ArrayList<RepositoryItem>();
	  try {
		  subset = CommonComponentHelper.getEmailMyFavoritesRepository().getItem(favoritesListId, "EmailMyFavorites");
		  String productIds = (String) subset.getPropertyValue("favoriteList");

		  List<String> products = Arrays.asList(productIds.split(","));
		  ProdSkuUtil prodSkuUtil = CommonComponentHelper.getProdSkuUtil();	         

		  for (String productId : products) {
			  if (StringUtils.isNotEmpty(productId)) {
				  RepositoryItem product = prodSkuUtil.getProductRepositoryItem(productId);
				  if (product != null) {
					  favoriteProducts.add(product);
				  }
			  }    	  
		  }
	  } catch (final RepositoryException re) {
		  re.printStackTrace();
	  }
	  return favoriteProducts;
  }

private List<RepositoryItem> getSortedFavoriteProducts(String favoritesListId) {
    return getSortedFavoriteProducts(getUserFavoritesItems(favoritesListId));
  }
  
  private List<RepositoryItem> getSortedProfileFavoriteProducts() {
    return getSortedFavoriteProducts(getProfileFavoriteItems());
  }
  
  private List<RepositoryItem> getSortedFavoriteProducts(List<RepositoryItem> favoriteItems) {
    Collections.sort(favoriteItems, new FavoriteItemDateOrIdComparator());
    return getFavoriteProducts(favoriteItems);
  }
  
  private List<RepositoryItem> getFavoriteProducts(List<RepositoryItem> favoriteItems) {
    ProdSkuUtil prodSkuUtil = CommonComponentHelper.getProdSkuUtil();
    List<RepositoryItem> favoriteProducts = new ArrayList<RepositoryItem>();
    
    for (RepositoryItem favoriteItem : favoriteItems) {
      String productId = (String) favoriteItem.getPropertyValue("productId");
      if (StringUtils.isNotEmpty(productId)) {
        RepositoryItem product = prodSkuUtil.getProductRepositoryItem(productId);
        if (product != null) {
          favoriteProducts.add(product);
        }
      }
    }
    
    return favoriteProducts;
  }
  
  private List<RepositoryItem> getUserFavoritesItems(String userId) {
    RepositoryView userView = getUserView();
    List<RepositoryItem> favoriteItems = new ArrayList<RepositoryItem>();
    RepositoryItem[] users = null;
    
    if (userView != null && StringUtils.isNotEmpty(userId)) {
      try {
        QueryBuilder queryBuilder = userView.getQueryBuilder();
        Query query = queryBuilder.createIdMatchingQuery(new String[] {userId});
        users = userView.executeQuery(query);
      } catch (RepositoryException e) {
        if (log.isLoggingDebug()) {
          log.debug("RepositoryException thrown when querying users for userId " + userId + ": " + e.getMessage());
        }
      }
      
      if (users != null) {
        @SuppressWarnings("unchecked")
        Set<RepositoryItem> userFavoriteItems = (Set<RepositoryItem>) users[0].getPropertyValue("favoriteCartItems");
        if (userFavoriteItems != null) {
          favoriteItems.addAll(userFavoriteItems);
        }
      }
    }
    
    return favoriteItems;
  }
  
  private RepositoryView getUserView() {
    ProfileTools profileTools = (ProfileTools) getRequest().resolveName("/atg/userprofiling/ProfileTools");
    RepositoryView userView = null;
    String userItemDescriptor = "user";
    
    try {
      userView = profileTools.getProfileRepository().getView(userItemDescriptor);
    } catch (RepositoryException e) {
      if (log.isLoggingDebug()) {
        log.logDebug("RepositoryException thrown when looking up " + userItemDescriptor + " view not in profile repository: " + e.getMessage());
      }
    }
    
    return userView;
  }
  
  @SuppressWarnings("unchecked")
  private List<RepositoryItem> getProfileFavoriteItems() {
    Set<RepositoryItem> profileFavoriteItems = (Set<RepositoryItem>) getProfile().getPropertyValue("favoriteCartItems");
    return new ArrayList<RepositoryItem>(profileFavoriteItems);
  }
}
