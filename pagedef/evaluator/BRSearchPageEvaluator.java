package com.nm.commerce.pagedef.evaluator;

import java.io.IOException;

import javax.servlet.jsp.PageContext;

import atg.core.util.StringUtils;
import atg.servlet.DynamoHttpServletRequest;
import atg.servlet.DynamoHttpServletResponse;

import com.nm.ajax.myfavorites.utils.MyFavoritesUtil;
import com.nm.commerce.checkout.CheckoutComponents;
import com.nm.commerce.pagedef.definition.PageDefinition;
import com.nm.commerce.pagedef.model.BRSearchPageModel;
import com.nm.commerce.pagedef.model.PageModel;
import com.nm.common.INMGenericConstants;
import com.nm.components.CommonComponentHelper;
import com.nm.formhandler.EndecaSearchFormHandler;
import com.nm.search.bloomreach.BloomreachSearch;
import com.nm.search.bloomreach.BloomreachSearchConstants;
import com.nm.search.bloomreach.BloomreachSearchException;
import com.nm.tms.core.TMSMessageContainer;
import com.nm.tms.utils.TMSDataDictionaryUtils;
import com.nm.utils.GenericLogger;
import com.nm.utils.NmoUtils;
import com.nm.utils.StringUtilities;

/**
 * Updates variables and sets up any redirects before page execution. Returns true if page content should be output.
 */
public class BRSearchPageEvaluator extends SimplePageEvaluator {
  
  public static final String ENDECA_SEARCH_FORM_HANDLER_PATH = "/nm/formhandler/SearchFormHandler";
  
  /** The Constant BRERROR_JSP. */
  private static final String BRERROR_JSP = "/brerror.jsp";
  
  /** The Constant SEARCH_URL */
  private static final String SEARCH_URL_PARAM = "searchURL=/search.jsp";
  
  /** The Constant BR_SEARCH */
  private static final String BR_SEARCH = "brSearch";
  
  private static final String KEYWORD = "keyword";
  
  protected GenericLogger log = CommonComponentHelper.getLogger();
  final BloomreachSearch brSearch = BloomreachSearch.getInstance();
  
  @Override
  protected PageModel allocatePageModel() {
    return new BRSearchPageModel();
  }
  
  @Override
  public boolean evaluate(final PageDefinition pageDefinition, final PageContext pageContext) throws Exception {
    boolean returnValue = true;
    if (!super.evaluate(pageDefinition, pageContext)) {
      return false;
    }
    
    final TMSMessageContainer tmsMessageContainer = CheckoutComponents.getTMSMessageContainer(getRequest());
    final DynamoHttpServletRequest request = getRequest();
    BRSearchPageModel pageModel = null;
    try {
      pageModel = (BRSearchPageModel) getPageModel();
      
      if (!brSearch.isEnabled()) {
        returnValue = redirectToEndecaSearch(request, getResponse());
        return returnValue;
      } else {
        final BloomreachSearch brSearch = BloomreachSearch.getInstance();
        // BR Changes - sets the bloom reach response to page model
        brSearch.setBloomReachResponse(request, getResponse(), pageModel);
        // To check if Keyword Redirect
        handleKeywordRedirect(pageModel);
        // To check if Depiction Redirect
        handleDepictionRedirect(pageModel);
      }
      // Changes for MyNM direction
      final String fromPage = request.getParameter(BloomreachSearchConstants.FROM);
      if (BloomreachSearchConstants.ERROR.equalsIgnoreCase(pageModel.getBrResultStatus())) {
        request.setParameter(BloomreachSearchConstants.IS_BR_SEARCH_TIMEOUT, INMGenericConstants.TRUE_STRING);
        if ((fromPage != null) && BloomreachSearchConstants.MY_NM.equals(fromPage)) {
          request.getRequestDispatcher(BRERROR_JSP).forward(request, getResponse());
        } else {
          returnValue = redirectToEndecaSearch(request, getResponse());
        }// log for Appman reporting for BR
        log.logError("Bloomreach Search Exception ---- ");
        return returnValue;
      } else if (BloomreachSearchConstants.BR_NO_RESULT.equalsIgnoreCase(pageModel.getBrResultStatus())) {
        request.setParameter(BloomreachSearchConstants.BR_NO_RESULT, INMGenericConstants.TRUE_STRING);
      }
      
    } catch (final BloomreachSearchException bre) {
      if (log.isLoggingError()) {
        log.logError("Bloomreach Search Exception ---- " + bre.getStackTrace());
      }
      redirectToEndecaSearch(request, getResponse());
    } catch (final Exception e) {
      if (log.isLoggingError()) {
        log.logError("Bloomreach Search ---- " + e.getStackTrace());
      }
      redirectToEndecaSearch(request, getResponse());
    }
    /**
     * only when search type ahead is used "aq" parameter is available in the request parameter. request parameter "aq" contains the search term entered by user.
     */
    if (StringUtilities.isNotBlank(request.getParameter("aq"))) {
      pageModel.setInternalSearchTerm(request.getParameter("aq"));
      pageDefinition.setUsedTypeAhead(request.getParameter("q"));
    } else {
      pageModel.setInternalSearchTerm(pageModel.getSearchTerm());
      pageDefinition.setUsedTypeAhead(null);
    }
    // to set the favorite item to profile
    if (getBrandSpecs().isEnableMyFavorites()) {
      pageModel.setMyFavItems(org.apache.commons.lang.StringUtils.join(MyFavoritesUtil.getInstance().getMyFavoriteItemsList(getProfile(), null, false), ','));
    }
    final TMSDataDictionaryUtils dataDictionaryUtils = CommonComponentHelper.getDataDictionaryUtils();
    dataDictionaryUtils.processTMSDataDictionaryAttributes(pageDefinition, null, getProfile(), pageModel, tmsMessageContainer);
    // set ILink data for BR Search Page
    pageModel.setDisplayILinkData(CommonComponentHelper.getTemplateUtils().isDisplayILinkData());
    return true;
  }
  
  private EndecaSearchFormHandler getEndecaSearchFormHandler(final DynamoHttpServletRequest request) {
    
    final EndecaSearchFormHandler endecaSearchFormHandler = (EndecaSearchFormHandler) request.resolveName("/nm/formhandler/SearchFormHandler");
    return endecaSearchFormHandler;
  }
  
  private boolean redirectToEndecaSearch(final DynamoHttpServletRequest request, final DynamoHttpServletResponse response) {
    try {
      String searchTerm = request.getParameter("l");
      if (StringUtils.isEmpty(searchTerm)) {
        searchTerm = request.getParameter("Ntt");
      }
      Object searchString = request.getParameter("N");
      if (null == searchString) {
        searchString = 0;
      }
      request.setParameter("N", searchString);
      String fullSearchString = "N=" + searchString;
      
      request.setParameter("Ntt", searchTerm);
      // Append searchString with st parameter if SALE category selection is available in request
      final String sParameterValue = request.getParameter("st");
      if (!StringUtils.isEmpty(sParameterValue)) {
        request.setParameter("st", sParameterValue);
        fullSearchString = fullSearchString + "&st=" + sParameterValue;
      }
      // Append searchString with Ns parameter if Sort option selection is available in request
      final String nsParameterValue = request.getParameter("Ns");
      if (!StringUtils.isEmpty(nsParameterValue)) {
        request.setParameter("Ns", nsParameterValue);
        fullSearchString = fullSearchString + "&Ns=" + nsParameterValue;
      }
      // Append searchString with pageSize parameter if View by option selection is available in request
      final String pageSizeParameterValue = request.getParameter("pageSize");
      if (!StringUtils.isEmpty(pageSizeParameterValue)) {
        request.setParameter("pageSize", pageSizeParameterValue);
        fullSearchString = fullSearchString + "&pageSize=" + pageSizeParameterValue;
      }
      // Append searchString with Ra parameter if VIEW ALL RESULTS option selection is available in request
      final String raParameterValue = request.getParameter("Ra");
      if (!StringUtils.isEmpty(raParameterValue)) {
        request.setParameter("Ra", raParameterValue);
        fullSearchString = fullSearchString + "&Ra=" + raParameterValue;
      }
      // Append searchString with location parameter if Instore filter selection is available in request
      final String locationParameterValue = request.getParameter("location");
      if (!StringUtils.isEmpty(locationParameterValue)) {
        request.setParameter("location", locationParameterValue);
        fullSearchString = fullSearchString + "&location=" + locationParameterValue;
      }
      // Append searchString with onlineOrStore parameter if Instore filter selection is available in request
      final String onlineOrStoreParameterValue = request.getParameter("onlineOrStore");
      if (!StringUtils.isEmpty(onlineOrStoreParameterValue)) {
        request.setParameter("onlineOrStore", onlineOrStoreParameterValue);
        fullSearchString = fullSearchString + "&onlineOrStore=" + onlineOrStoreParameterValue;
      }
      // Append searchString with radius parameter if Instore filter selection is available in request
      final String radiusParameterValue = request.getParameter("radius");
      if (!StringUtils.isEmpty(radiusParameterValue)) {
        request.setParameter("radius", radiusParameterValue);
        fullSearchString = fullSearchString + "&radius=" + radiusParameterValue;
      }
      request.setAttribute(BloomreachSearchConstants.ERROR, true);
      
      final EndecaSearchFormHandler endecaSearchForm = getEndecaSearchFormHandler(request);
      
      if (null != endecaSearchForm) {
        endecaSearchForm.setSearchString(fullSearchString);
        endecaSearchForm.setSearchText(searchTerm);
        endecaSearchForm.handleSearch(request, response);
      }
    } catch (final Exception e) {
      if (log.isLoggingError()) {
        log.logError("BR Redirect To Endeca Seach : " + e);
      }
    }
    return false;
  }
  
  /**
   * This method handle keyword redirect
   * 
   * @param pageModel
   * @throws BloomreachSearchException
   * 
   */
  private void handleKeywordRedirect(final BRSearchPageModel pageModel) throws BloomreachSearchException {
    try {
      if (!NmoUtils.isEmpty(pageModel.getRedirectedUrl())) {
        // Redirecting Response
        final DynamoHttpServletResponse response = getResponse();
        response.sendRedirect(pageModel.getRedirectedUrl());
      }
    } catch (final IOException e) {
      if (log.isLoggingError()) {
        log.logError("Bloomreach Search -- Keyword Redirection Error" + e);
      }
      throw new BloomreachSearchException("Error occured during Keyword redirect in BloomReach search flow " + e);
    }
  }
  
  /**
   * <p>
   * This method handles Depiction Redirect to PDP if any one of the search patterns<BR>
   * (Regular Product Depiction Number/Group Product Depiction Number/Product ID/Offer Item No/Short SKU)<BR>
   * is found matched and numFound = 1 from BR Response.
   * </p>
   * 
   * @param pageModel
   * @throws BloomreachSearchException
   * 
   */
  private void handleDepictionRedirect(final BRSearchPageModel pageModel) throws BloomreachSearchException {
    try {
      final String depictionRedirectUrl = pageModel.getDepictionRedirectUrl();
      if (!NmoUtils.isEmpty(depictionRedirectUrl)) {
        getResponse().sendRedirect(depictionRedirectUrl);
      }
    } catch (final IOException e) {
      if (log.isLoggingError()) {
        log.logError("Bloomreach Search -- Depiction Redirection Error" + e);
      }
      throw new BloomreachSearchException("Error occured during depiction redirect in BloomReach search flow " + e);
    }
  }
  
} // End of Class

