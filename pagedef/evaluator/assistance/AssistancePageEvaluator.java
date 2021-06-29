package com.nm.commerce.pagedef.evaluator.assistance;

import static com.nm.common.INMGenericConstants.ACTIVE;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import javax.servlet.jsp.PageContext;

import atg.nucleus.Nucleus;
import atg.repository.RepositoryItem;

import com.nm.abtest.AbTestHelper;
import com.nm.catalog.navigation.CategoryHelper;
import com.nm.catalog.navigation.NMCategory;
import com.nm.commerce.NMProfile;
import com.nm.commerce.pagedef.definition.PageDefinition;
import com.nm.commerce.pagedef.evaluator.SimplePageEvaluator;
import com.nm.commerce.pagedef.model.AssistancePageModel;
import com.nm.commerce.pagedef.model.PageModel;
import com.nm.components.CommonComponentHelper;
import com.nm.returnseligibility.config.ReturnsEligibilityConfig;
import com.nm.tms.constants.TMSDataDictionaryConstants;
import com.nm.tms.core.TMSMessageContainer;
import com.nm.tms.utils.TMSDataDictionaryUtils;
import com.nm.utils.NmoUtils;
import com.nm.utils.RefreshableContent;
import com.nm.utils.StringUtilities;

/**
 * Updates variables and sets up any redirects before page execution. Returns true if page content should be output.
 */
public class AssistancePageEvaluator extends SimplePageEvaluator {
  
  private final String REST_OF_WORLD_CODE = "ROW";
  private final String US_CODE = "US";
  private final String US_ASSISTNACE_CATEGORY_ID = "catAssistance";
  
  @Override
  public boolean evaluate(final PageDefinition pageDefinition, final PageContext pageContext) throws Exception {
    String assistanceCatId;
    
    if (!super.evaluate(pageDefinition, pageContext)) {
      return false;
    }
    
    final AssistancePageModel pageModel = (AssistancePageModel) getPageModel();
    final NMProfile profile = getProfile();
    final String country = profile.getCountryPreference();
    final RefreshableContent refreshable = (RefreshableContent) Nucleus.getGlobalNucleus().resolveName("/nm/utils/RefreshableContent");
    if (refreshable.getAssistanceCatId() != null) {
      assistanceCatId = refreshable.getAssistanceCatId().getProperty(country.toUpperCase());
      // if country code doesn't match any of the configured key, fall back to ROW configure
      if (assistanceCatId == null) {
        assistanceCatId = refreshable.getAssistanceCatId().getProperty(REST_OF_WORLD_CODE);
      }
      // in case nothing (neither country code or ROW) is configured for the given country, fall back to US category id
      if (assistanceCatId == null) {
        assistanceCatId = refreshable.getAssistanceCatId().getProperty(US_CODE);
      }
    } else { // AssistanceCatId is not configured
      assistanceCatId = refreshable.getAssistanceCatId().getProperty(US_ASSISTNACE_CATEGORY_ID);
    }
    final RepositoryItem assistanceItem = getCategoryHelper().lookupCategory(assistanceCatId);
    if (assistanceItem == null) {
      getResponse().sendRedirect(getBrandSpecs().getCategoryNotFoundPath());
      return false;
    }
    pageModel.setAssistanceCategory(CategoryHelper.getInstance().getNMCategory(assistanceItem, null));
    pageModel.setCurrentAssistanceCategory(getCurrentCategory(assistanceCatId));
    // Sets true if user belongs to returns charge test group.
    final ReturnsEligibilityConfig returnsEligibilityConfig = CommonComponentHelper.getReturnsEligibilityConfig();
    final String returnsEligibilityTest = AbTestHelper.getAbTestValue(getRequest(), returnsEligibilityConfig.getAbTestReturnsEligibilityGroup());
    if (StringUtilities.isNotEmpty(returnsEligibilityTest) && returnsEligibilityTest.equalsIgnoreCase(ACTIVE)
            && getCurrentCategory(assistanceCatId).getRepositoryId().equalsIgnoreCase(returnsEligibilityConfig.getToBeOverriddenAssistanceCategories())) {
      pageModel.setChargeForReturnsActive(Boolean.TRUE);
    }
    pageModel.setPageType(TMSDataDictionaryConstants.ASSISTANCE);
    // Data Dictionary Attributes population.
    final TMSDataDictionaryUtils dataDictionaryUtils = CommonComponentHelper.getDataDictionaryUtils();
    dataDictionaryUtils.processTMSDataDictionaryAttributes(pageDefinition, null, getProfile(), pageModel, new TMSMessageContainer());
    return true;
  }
  
  @Override
  protected PageModel allocatePageModel() {
    return new AssistancePageModel();
  }
  
  /**
   * Returns the current Assistance category specified by the itemId parameter or defaulting to the first childCategory under the assistanceCategory when the itemId cannot be found or is not
   * specified.
   * 
   * @return category
   * @throws IOException
   */
  private NMCategory getCurrentCategory(final String assistanceCatId) throws IOException {
    final String categoryId = getRequest().getParameter("itemId");
    final NMCategory assistanceCategory = getCategoryHelper().getNMCategory(assistanceCatId);
    final List<NMCategory> childrenCats = assistanceCategory.getChildCategories();
    boolean isChild = false;
    if (!NmoUtils.isEmpty(categoryId)) {
      final Iterator<NMCategory> catIter = childrenCats.iterator();
      NMCategory child = null;
      // check to see if the itemId is a child of current assistance page
      while (catIter.hasNext()) {
        child = catIter.next();
        if (categoryId.equals(child.getId())) {
          isChild = true;
          break;
        }
      }
      if (isChild) {
        final NMCategory current = CategoryHelper.getInstance().getNMCategory(categoryId);
        if (current != null) {
          return current;
        }
      } else { // if not a child, redirect to page of first child of current assistance category
        final String redirectItemId = childrenCats.size() > 0 ? assistanceCategory.getChildCategories().get(0).getId() : "";
        if (!"".equals(redirectItemId)) {
          final String redirectTo = getRequest().getRequestURIWithQueryString().replace("itemId=" + categoryId, "itemId=" + redirectItemId);
          getResponse().sendRedirect(redirectTo);
        }
      }
    }
    
    // default to first childCatergory under assistanceCategory if childCats are available
    final NMCategory categoryItem = childrenCats.size() > 0 ? assistanceCategory.getChildCategories().get(0) : null;
    if (categoryItem != null) {
      return categoryItem;
    }
    
    return null;
  }
}
