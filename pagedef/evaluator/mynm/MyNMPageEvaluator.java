package com.nm.commerce.pagedef.evaluator.mynm;

import java.util.List;

import javax.servlet.jsp.PageContext;

import atg.servlet.ServletUtil;

import com.nm.authentication.AuthenticationHelper;
import com.nm.catalog.navigation.NMCategory;
import com.nm.commerce.catalog.DynamicChildProductsCategory;
import com.nm.commerce.pagedef.definition.MyNMPageDefinition;
import com.nm.commerce.pagedef.definition.PageDefinition;
import com.nm.commerce.pagedef.evaluator.SimplePageEvaluator;
import com.nm.commerce.pagedef.model.MyNMPageModel;
import com.nm.commerce.pagedef.model.PageModel;
import com.nm.common.INMGenericConstants;
import com.nm.components.CommonComponentHelper;
import com.nm.mynm.MyNMWidget;
import com.nm.mynm.MyNMWidgetHelper;
import com.nm.tms.constants.TMSDataDictionaryConstants;
import com.nm.tms.core.TMSMessageContainer;
import com.nm.tms.utils.TMSDataDictionaryUtils;
import com.nm.utils.NmoUtils;

public class MyNMPageEvaluator extends SimplePageEvaluator {
  
  @Override
  public boolean evaluate(final PageDefinition pageDefinition, final PageContext pageContext) throws Exception {
    if (!super.evaluate(pageDefinition, pageContext)) {
      return false;
    }
    final MyNMPageDefinition myNMPageDefinition = (MyNMPageDefinition) pageDefinition;
    final MyNMPageModel myNMPageModel = (MyNMPageModel) getPageModel();
    final NMCategory category = getCategory();
    final boolean previewEnabled = Boolean.parseBoolean(getRequest().getParameter("preview"));
    boolean isAuth = false;
    final AuthenticationHelper authHelper = (AuthenticationHelper) getRequest().resolveName("/nm/authentication/AuthenticationHelper");
    isAuth = authHelper.sessionHasLoggedInRegisteredUser(getRequest());
    myNMPageModel.setLoginStatus(isAuth);
    ServletUtil.getCurrentRequest().getSession().setAttribute("MYNM_PROD_CAT_AFFINITY", null);
    ServletUtil.getCurrentRequest().getSession().setAttribute("MYNM_EDIT_CAT_AFFINITY", null);
    final MyNMWidgetHelper widgetHelper = MyNMWidgetHelper.getInstance();
    final List<MyNMWidget> basicWidgetList = widgetHelper.getBasicWidgetList(category, previewEnabled, myNMPageDefinition.isRWD());
    myNMPageModel.setWidgets(basicWidgetList);
    myNMPageModel.setPageType(TMSDataDictionaryConstants.MY_NM);
    myNMPageModel.setWidgetsIds(getWidgetsIdentifier(widgetHelper));
    // Data Dictionary Attributes population.
    final TMSDataDictionaryUtils dataDictionaryUtils = CommonComponentHelper.getDataDictionaryUtils();
    dataDictionaryUtils.processTMSDataDictionaryAttributes(pageDefinition, null, getProfile(), myNMPageModel, new TMSMessageContainer());
    return true;
  }
  
  @Override
  protected PageModel allocatePageModel() {
    return new MyNMPageModel();
  }
  
  /**
   * Returns base MyNM category
   * 
   * @return
   */
  public NMCategory getCategory() {
    NMCategory category = getCategoryHelper().getNMCategory(getRequest().getParameter("itemId"), getRequest().getParameter("parentId"));
    if (category != null) {
      category = new DynamicChildProductsCategory(category);
    }
    return category;
  }
  
  /**
   * Gets the widgets identifier.
   * 
   * @param basicWidgetList
   *          the basic widget list
   * @param widgetHelper
   *          the widget helper
   * @return the widgets identifier
   */
  private String getWidgetsIdentifier(final MyNMWidgetHelper widgetHelper) {
    final StringBuilder widgetList = new StringBuilder();
    final List<MyNMWidget> basicWidgetList = widgetHelper.getMyNMWidgetPosList();
    if (NmoUtils.isNotEmptyCollection(basicWidgetList)) {
      int widgetPos = 0;
      String widgetId = INMGenericConstants.EMPTY_STRING;
      String widgetTemplate = INMGenericConstants.EMPTY_STRING;
      String widgetCode = INMGenericConstants.EMPTY_STRING;
      String widgetKey = INMGenericConstants.EMPTY_STRING;
      String widgetType = INMGenericConstants.EMPTY_STRING;
      final String isPersonalized = INMGenericConstants.TILDE_STRING;
      for (MyNMWidget nmWidget : basicWidgetList) {
        widgetPos++;
        if (null != nmWidget) {
          widgetType = nmWidget.getType();
          widgetId = nmWidget.getId();
          if (INMGenericConstants.PRODUCT.equalsIgnoreCase(widgetType)) {
            widgetCode = widgetHelper.getWidgetOmnitureCode(widgetHelper.getProductWidgetCategory(widgetId));
          } else if (INMGenericConstants.EDITORIAL.equalsIgnoreCase(widgetType)) {
            widgetCode = nmWidget.getTemplateOmnitureCode();
          } else if (INMGenericConstants.PRODUCT_EDITORIAL.equalsIgnoreCase(widgetType)) {
            widgetCode = nmWidget.getTemplateOmnitureCode();
          } else if (INMGenericConstants.STORE.equalsIgnoreCase(widgetType)) {
            widgetCode = nmWidget.getTemplateOmnitureCode();
          }
          widgetTemplate = nmWidget.getTemplate();
          widgetKey = widgetPos + widgetCode + isPersonalized + widgetTemplate + INMGenericConstants.COMMA_STRING;
          widgetList.append(widgetKey);
        }
      }
      widgetList.replace(widgetList.lastIndexOf(INMGenericConstants.COMMA_STRING), widgetList.length(), INMGenericConstants.EMPTY_STRING);
    }
    return widgetList.toString();
  }
  
}
