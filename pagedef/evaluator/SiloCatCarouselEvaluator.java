package com.nm.commerce.pagedef.evaluator;

import java.util.ArrayList;
import java.util.List;


import javax.servlet.jsp.PageContext;

import com.nm.catalog.navigation.NMCategory;
import com.nm.catalog.template.TemplateUtils;
import com.nm.commerce.pagedef.definition.PageDefinition;
import com.nm.commerce.pagedef.definition.SubcategoryTemplatePageDefinition;
import com.nm.commerce.pagedef.model.TemplatePageModel;
import com.nm.components.CommonComponentHelper;

public class SiloCatCarouselEvaluator extends SubcategoryTemplatePageEvaluator{
	
	
	public static final String SALE_SILO_CATEGORY_NAME = "Sale";
	public static final String SALE_SUB_CAT_ID = "subCatId";
	
	@Override
	  public boolean evaluate(PageDefinition pageDefinition, PageContext pageContext) throws Exception {
		if (!super.evaluate(pageDefinition, pageContext)) {
		      return false;
		    }
	    
	    SubcategoryTemplatePageDefinition subcategoryPageDefinition = (SubcategoryTemplatePageDefinition) pageDefinition;
	    TemplatePageModel templatePageModel = (TemplatePageModel) getPageModel();
	    final NMCategory category = templatePageModel.getCategory();
	    final TemplateUtils templateUtils = CommonComponentHelper.getTemplateUtils();
	    List<NMCategory> carouselSubCats = null;
	    if (templateUtils != null) {
	      carouselSubCats = templateUtils.getSiloCategories(subcategoryPageDefinition.getTemplateId(),false,category.getId(),subcategoryPageDefinition.getMaxCarouselInMobile());
	    } else {
	      carouselSubCats = new ArrayList<NMCategory>();
	    }
	    
    	templatePageModel.setCarouselList(getSaleSiloSubCatMaxThreshold(carouselSubCats,subcategoryPageDefinition.getSubcategoryThreshold()));	    
	    return true;
	  }
	
	 /**
	  * Method to retrieve sale sub category list with maximum threshold
	  * @param saleSubCatChildCategories
	  * @param subCategoryThreshold
	  * @return
	  */
	 private List<NMCategory> getSaleSiloSubCatMaxThreshold(List<NMCategory> carouselSubCats, int subCategoryThreshold) {
		  List<NMCategory> saleSubCatChildList =	new ArrayList<NMCategory>();
		    if (carouselSubCats != null && !carouselSubCats.isEmpty()) {
		    	if(carouselSubCats.size() < subCategoryThreshold) {
		    		subCategoryThreshold = carouselSubCats.size(); 
		    	}
		    		for (int i = 0; i < subCategoryThreshold; i++) {
		    			if (carouselSubCats != null && !carouselSubCats.isEmpty()) {
		    				saleSubCatChildList.add(carouselSubCats.get(i));
		    			} 
		    		}
		    	}
		    return saleSubCatChildList;
		  }
	 
}