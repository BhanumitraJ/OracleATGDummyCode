package com.nm.commerce.pagedef.evaluator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.jsp.PageContext;

import atg.nucleus.Nucleus;

import com.nm.catalog.navigation.CategoryHelper;
import com.nm.catalog.navigation.NMCategory;
import com.nm.commerce.pagedef.definition.PageDefinition;
import com.nm.commerce.pagedef.model.EmagPageModel;
import com.nm.commerce.pagedef.model.PageModel;
import com.nm.components.CommonComponentHelper;
import com.nm.tms.constants.TMSDataDictionaryConstants;
import com.nm.tms.core.TMSMessageContainer;
import com.nm.tms.utils.TMSDataDictionaryUtils;
import com.nm.utils.BrandSpecs;
import com.nm.utils.StringUtilities;

public class EmagPageEvaluator extends TemplatePageEvaluator {
  
  @Override
  public boolean evaluate(final PageDefinition pageDefinition, final PageContext pageContext) throws Exception {
    if (!super.evaluate(pageDefinition, pageContext)) {
      return false;
    }
    final BrandSpecs brandSpecs = (BrandSpecs) Nucleus.getGlobalNucleus().resolveName("/nm/utils/BrandSpecs");
    final EmagPageModel emagPageModel = (EmagPageModel) getPageModel();
    final NMCategory storyCategory = getCategory();
    String emagParentCatId = brandSpecs.getDefaultEmagParentId();
    List<NMCategory> contentsCategoryList = null;

    // Is this the default emag category?
    if (StringUtilities.areEqual(emagParentCatId, storyCategory.getId())) {
      childRedirect(storyCategory);
      return false;
    }

    // Is there a parent emag category override?
    final String emagOverrideParentCatId = getOverrideCatId(storyCategory);
    if (StringUtilities.isNotEmpty(emagOverrideParentCatId)) {
      emagParentCatId = getOverrideCatId(storyCategory);
    }

    // Set contents category list if emag category exists.
    final NMCategory emagCategory = CategoryHelper.getInstance().getNMCategory(emagParentCatId);
    if (emagCategory != null) {
      contentsCategoryList = getChildrenWithTemplate(emagCategory, storyCategory.getTemplateId());
      emagPageModel.setStoryIndex(contentsCategoryList.indexOf(storyCategory));
    }


    String emagName = emagCategory.getDisplayName();

    emagPageModel.setPageType(TMSDataDictionaryConstants.SUB_CATEGORY);
    emagPageModel.setStoryCategory(storyCategory);
    emagPageModel.setEmagCategory(emagCategory);
    emagPageModel.setContentsCategoryList(contentsCategoryList);
    // Data Dictionary Attributes population.
    final TMSDataDictionaryUtils dataDictionaryUtils = CommonComponentHelper.getDataDictionaryUtils();
    dataDictionaryUtils.processTMSDataDictionaryAttributes(pageDefinition, null, getProfile(), emagPageModel, new TMSMessageContainer());
    return true;
  }
  
  @Override
  protected PageModel allocatePageModel() {
    return new EmagPageModel();
  }
  
  /**
   * If the current category has child categories, it should redirect to the first child.
   * 
   * @return true if the system has redirected
   * @throws IOException
   */
  private void childRedirect(final NMCategory storyCategory) throws IOException {
    NMCategory redirectCategory = null;
    
    // Find first emag child category.
    for (final NMCategory category : storyCategory.getChildCategories()) {
      if (category.getTemplateId().equals(storyCategory.getTemplateId())) {
        redirectCategory = category;
        break;
      }
    }

    // Is there an emag category?
    if (redirectCategory != null) {
      getResponse().sendRedirect(redirectCategory.getTemplateUrl() + "?itemId=" + redirectCategory.getId() + "&parentId=" + storyCategory.getId());
    } else {
      getResponse().sendRedirect(getBrandSpecs().getCategoryNotFoundPath());
    }
  }

  /**
   * Returns product widget template type
   *
   * @param storyCategory
   * @return
   */
  public String getOverrideCatId(final NMCategory storyCategory) {
    final Map<String, String> categoryAttributes = storyCategory.getCategoryAttributeMap();
    String overrideCatId = "";
    if (categoryAttributes != null) {
      overrideCatId = categoryAttributes.get("emagParentCategoryOverride");
    }
    return overrideCatId;
  }

  /**
   * Returns list of child categories that have the emag template.
   *
   * @param emagCategory
   * @return
   */
  public List<NMCategory> getChildrenWithTemplate(final NMCategory emagCategory, final String templateId) {
    List<NMCategory> emagChildCategories = new ArrayList<NMCategory>();
    if (emagCategory != null) {
      emagChildCategories = emagCategory.getChildCategories();
      final Iterator<NMCategory> i = emagChildCategories.iterator();
      while (i.hasNext()) {
        final NMCategory childCategory = i.next(); // must be called before you can call i.remove()
        if (StringUtilities.areNotEqual(childCategory.getTemplateId(), templateId)) {
          i.remove();
        }
      }
    }
    return emagChildCategories;
  }

}
