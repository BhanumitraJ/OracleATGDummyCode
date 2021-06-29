package com.nm.commerce.promotion;

import static com.nm.common.INMGenericConstants.ZERO;

import java.util.List;
import java.util.Map;

import javax.transaction.TransactionManager;

import atg.commerce.CommerceException;
import atg.commerce.order.CommerceItem;
import atg.commerce.order.InvalidVersionException;
import atg.commerce.order.Order;
import atg.commerce.order.OrderHolder;
import atg.commerce.order.OrderManager;
import atg.commerce.order.OrderQueries;
import atg.commerce.pricing.PricingTools;
import atg.commerce.promotion.PromotionConstants;
import atg.commerce.states.OrderStates;
import atg.commerce.states.StateDefinitions;
import atg.core.util.StringUtils;
import atg.dtm.TransactionDemarcation;
import atg.dtm.TransactionDemarcationException;
import atg.nucleus.Nucleus;
import atg.nucleus.naming.ComponentName;
import atg.process.ProcessException;
import atg.process.ProcessExecutionContext;
import atg.process.action.ActionImpl;
import atg.repository.MutableRepositoryItem;
import atg.repository.RepositoryException;
import atg.repository.RepositoryItem;
import atg.servlet.DynamoHttpServletRequest;
import atg.servlet.ServletUtil;

import com.nm.commerce.NMCommerceItem;
import com.nm.commerce.NMProfile;
import com.nm.commerce.catalog.NMProduct;
import com.nm.commerce.checkout.CheckoutComponents;
import com.nm.commerce.pricing.Markdown;
import com.nm.commerce.pricing.NMItemPriceInfo;
import com.nm.components.CommonComponentHelper;
import com.nm.edo.constants.EmployeeDiscountConstants;
import com.nm.edo.exception.EmployeeDiscountsException;
import com.nm.edo.util.EmployeeDiscountsUtil;
import com.nm.edo.vo.ItemsListVO;
import com.nm.edo.vo.ProductDiscountServiceResponse;
import com.nm.utils.ExceptionUtil;
import com.nm.utils.PromotionsHelper;
import com.nm.utils.SystemSpecs;

/**
 * This action is called when employee promotions are enabled and employee on the order review page(employee response state is order review). If silentAuthentication success on the order review page
 * call product discount service and the get the employeeDiscount and additionalEmployeeDiscount percentages for each item level and apply to the employee order.
 * 
 */

public class EvaluateEmployeeDiscounts extends ActionImpl implements IPromoAction {
  // -------------------------------------
  /** Class version string */
  public static final String CLASS_VERSION = "$Id: EvaluateEmployeeDiscounts.java 1.12 2014/06/24 04:57:48CDT Rajasekhar Ginjupalli (vsrg4) Exp  $";
  
  public String ORDERMANAGER_PATH = "/atg/commerce/order/OrderManager";
  public String PRICINGTOOLS_PATH = "/atg/commerce/pricing/PricingTools";
  public static final String SHOPPING_CART_PATH = "/atg/commerce/ShoppingCart";
  public static final String EMPLOYEE_DISCOUNTS_UTIL_PATH = "/nm/edo/util/EmployeeDiscountsUtil";
  public static final String PROMOHELPER_PATH = "/nm/utils/PromotionsHelper";
  protected OrderManager orderManager = null;
  protected OrderQueries orderQueries = null;
  protected ComponentName orderHolderComponent = null;
  protected PricingTools pricingTools;
  protected EmployeeDiscountsUtil employeeDiscountsUtil = null;
  PromotionsHelper promotionsHelper = null;
  private boolean logDebug;
  private boolean logError = true;
  public static final int EMPLOYEE_DISCOUNT_PERCENTAGE = 1;
  public static final int ADDITIONAL_DISCOUNT_PERCENTAGE = 2;
  
  /**
   * Initialize the components.
   * 
   * @param params
   *          map of parameters
   * @throws ProcessException
   *           process exception
   */
  @Override
  public void initialize(final Map params) throws ProcessException {
    logDebug = getSystemSpecs().getCsrScenarioDebug();
    orderManager = (OrderManager) Nucleus.getGlobalNucleus().resolveName(ORDERMANAGER_PATH);
    pricingTools = (PricingTools) Nucleus.getGlobalNucleus().resolveName(PRICINGTOOLS_PATH);
    if (orderManager == null) {
      throw new ProcessException(PromotionConstants.getStringResource(PromotionConstants.ORDER_MANAGER_NOT_FOUND));
    }
    orderQueries = orderManager.getOrderQueries();
    orderHolderComponent = ComponentName.getComponentName(SHOPPING_CART_PATH);
    employeeDiscountsUtil = (EmployeeDiscountsUtil) Nucleus.getGlobalNucleus().resolveName(EMPLOYEE_DISCOUNTS_UTIL_PATH);
    promotionsHelper = (PromotionsHelper) Nucleus.getGlobalNucleus().resolveName(PROMOHELPER_PATH);
  }
  
  /**
   * Initialize the EvaluateEmployeeDiscounts Java
   * 
   * @param context
   *          context object
   * @throws ProcessException
   *           process exception
   */
  @Override
  protected void executeAction(final ProcessExecutionContext context) throws ProcessException {
    try {
      final Order order = getOrderToModify(context);
      evaluatePromo(order);
    } catch (final Exception e) {
      throw new ProcessException(ExceptionUtil.getExceptionInfo(e), e);
    }
  }
  
  /**
   * Evaluate method for EmployeeDiscounts. Discounts are applied only if employee/depended are logged in.
   * 
   * @param order
   *          Order object
   * @throws PromoException
   *           possible PromoException if any exception caught.
   */
  
  @Override
  public void evaluatePromo(final Order order) throws PromoException {
    if (orderManager == null) {
      throw new PromoException(PromotionConstants.getStringResource(PromotionConstants.ORDER_MANAGER_NOT_FOUND));
    }
    final TransactionDemarcation td = new TransactionDemarcation();
    boolean rollBack = false;
    try {
      final TransactionManager tm = CommonComponentHelper.getTransactionManager();
      if (tm != null) {
        td.begin(tm, TransactionDemarcation.REQUIRED);
      }
      
      final NMOrderImpl orderImpl = (NMOrderImpl) order;
      final NMProfile profile = CheckoutComponents.getProfile(getRequest());
      orderImpl.setEmployeePromotionApplied(false);
      if (employeeDiscountsUtil.isAssociateLoggedIn(profile) && (orderImpl.getEmployeeLoginStatus() >= NMOrderImpl.EMPLOYEE_PERFORMED_SILENT_AUTHENTICATION)) {
        handleApplyEmpPromotion(order, profile);
        orderManager.updateOrder(order);
        logDebug("order version-after updateorder-->" + orderImpl.getVersion());
      }
    } catch (final InvalidVersionException versionException) {
      logError("Ignoring, invalid version error occured in evaluate promos method.", versionException);
    } catch (final TransactionDemarcationException tde) {
      logError("Can not begin transaction in evaluate promo method.", tde);
      throw new PromoException(tde);
    } catch (final Exception e) {
      rollBack = true;
      logError("EvaluateEmployeeDiscounts.executeAction(): Exception " + e.getMessage());
      throw new PromoException(e);
    } finally {
      try {
        td.end(rollBack); // commit work
      } catch (final TransactionDemarcationException tde) {
        logError("Transaction error in evaluate promo method.", tde);
        throw new PromoException(tde);
      }
    }
  }
  
  /**
   * This method is used to call productDiscountService API and gets the employee discount and gets any additional discount percentages.
   * 
   * @param order
   *          Order object
   * @param nmProfile
   *          Profile object
   * @throws CommerceException
   *           possible exception on order update
   */
  private void handleApplyEmpPromotion(final Order order, final NMProfile nmProfile) throws CommerceException {
    final NMOrderImpl orderImpl = (NMOrderImpl) order;
    final List<NMCommerceItem> commerceItemsList = orderImpl.getNmCommerceItems();
    if (orderImpl.getPriceInfo() == null) {
      if (isLoggingError()) {
        logError("Skipping employee discounts evaluation as price info object are null.");
      }
      return;
    }
    logDebugCommerceItems(orderImpl);
    boolean orderHasBeenDiscounted = false;
    // ESB call for product discount service
    try {
      final ProductDiscountServiceResponse productDiscResponse = employeeDiscountsUtil.invokeProductDiscountService(nmProfile, orderImpl);
      if (null != productDiscResponse) {
        final Map<String, ItemsListVO> itemMap = productDiscResponse.getItemMap();
        for (final NMCommerceItem commerceItem : commerceItemsList) {
          final RepositoryItem repProd = (RepositoryItem) commerceItem.getAuxiliaryData().getProductRef();
          final NMProduct nmProduct = new NMProduct(repProd.getRepositoryId());
          final String offerItemId = nmProduct.getCmosCatalogId().concat("_" + nmProduct.getCmosItemCode());
          if (!StringUtils.isBlank(offerItemId)) {
            final ItemsListVO itemList = itemMap.get(offerItemId);
            if (null != itemList) {
              final String empDiscPct = itemList.getEmployeeDiscountPercentage();
              if (!StringUtils.isBlank(empDiscPct) && (Integer.parseInt(empDiscPct) > ZERO)) {
                applyDiscountToLineItem(empDiscPct, commerceItem, orderImpl, EMPLOYEE_DISCOUNT_PERCENTAGE);
                orderHasBeenDiscounted = true;
              }
              final String addDiscPct = itemList.getAdditionalDiscountPercentage();
              if (!StringUtils.isBlank(addDiscPct) && (Integer.parseInt(addDiscPct) > ZERO)) {
                applyDiscountToLineItem(addDiscPct, commerceItem, orderImpl, ADDITIONAL_DISCOUNT_PERCENTAGE);
                orderHasBeenDiscounted = true;
              }
            }
          }
        }
      }
    } catch (final EmployeeDiscountsException e) {
      if (isLoggingError()) {
        logError("Error in Product Discount Service call" + e.getMessage());
      }
      /* EDO changes */
      if (e.getErrorCode().equalsIgnoreCase(EmployeeDiscountConstants.INTERNAL_SERVICE_ERROR) || e.getErrorCode().equalsIgnoreCase(EmployeeDiscountConstants.COMMUNICATION_FAILURE)) {
        getRequest().getSession().setAttribute(EmployeeDiscountConstants.SYNC_FAIL, e.getMessage());
        nmProfile.setPropertyValue(EmployeeDiscountConstants.IS_ASSOCIATE_LOGGED_IN, true);
      }
    }
    
    if (orderHasBeenDiscounted) {
      orderImpl.getPriceInfo().setDiscounted(true);
      orderManager.updateOrder(orderImpl);
    }
  }
  
  /**
   * Applies the employee discount and any other additional discount percentage to the line item on the order.
   * 
   * @param discountPercent
   *          Discount Percent value from discount service
   * @param commerceItem
   *          CommerceItem to update price values
   * @param order
   *          Order object
   * @throws NumberFormatException
   *           possible number parsing error.
   * @throws CommerceException
   *           possible commerce exception on price update
   */
  private void applyDiscountToLineItem(final String discountPercent, final NMCommerceItem commerceItem, final NMOrderImpl order, final int percentType) throws NumberFormatException, CommerceException {
    if (promotionsHelper.verifyNotExcludedItem(commerceItem)) {
      applyDiscount(discountPercent, commerceItem, order, percentType);
      logDebug("Applied discounts to product -->" + ((RepositoryItem) commerceItem.getAuxiliaryData().getProductRef()).getRepositoryId());
      logDebug("Discount percentage and type(1=Discount, 2=ExtraDiscount) are -->" + discountPercent + ", " + percentType);
    }
  }
  
  /**
   * Applies the discount to the line item on the order.
   * 
   * @param discountPercent
   *          Discount Percent value from discount service
   * @param commerceItem
   *          CommerceItem to update price values
   * @param order
   *          Order object
   * @throws NumberFormatException
   *           possible number parsing error.
   * @throws CommerceException
   *           possible commerce exception on price update
   */
  private void applyDiscount(final String discountPercent, final CommerceItem commerceItem, final NMOrderImpl order, final int percentType) throws NumberFormatException, CommerceException {
    logDebug("***Applying Employee Discount or Extra Percent Discount****");
    final double amount = commerceItem.getPriceInfo().getAmount();
    logDebug("CI previously discounted amount-->" + amount);
    final double roundedAmount = pricingTools.round(amount);
    final double discRate = Double.parseDouble(discountPercent) / 100.0;
    double discountAmount = discRate * roundedAmount;
    double roundedDiscountAmount = pricingTools.round(discountAmount);
    double newamnt = roundedAmount - roundedDiscountAmount;
    newamnt = pricingTools.round(newamnt);
    double roundedSum = newamnt + roundedDiscountAmount;
    roundedSum = pricingTools.round(roundedSum);
    
    logDebug("**************************");
    logDebug("amount-->" + amount);
    logDebug("roundedAmount-->" + roundedAmount);
    logDebug("discRate-->" + String.valueOf(discRate));
    logDebug("discamount-->" + discountAmount);
    logDebug("roundedDiscAmt-->" + roundedDiscountAmount);
    logDebug("newamnt-->" + newamnt);
    logDebug("roundedSum-->" + roundedSum);
    
    if (roundedSum == roundedAmount) {
      NMItemPriceInfo ipi = (NMItemPriceInfo) commerceItem.getPriceInfo();
      ipi.setAmount(newamnt);
      ipi.setDiscounted(true);
      Map<String, Markdown> promotionsApplied = ipi.getPromotionsApplied();
      final double previousDiscountAmount = ipi.getOrderDiscountShare();
      ipi.setOrderDiscountShare(previousDiscountAmount + roundedDiscountAmount);
      switch (percentType) {
        case EMPLOYEE_DISCOUNT_PERCENTAGE:
          ipi.setEmployeeDiscountAmount(roundedDiscountAmount);
          ipi.setEmployeeDiscountPercent(Double.parseDouble(discountPercent));
          logDebug("employeeDiscountAmount is" + roundedDiscountAmount);
          logDebug("employeeDiscountPercent is" + roundedAmount);
        break;
        case ADDITIONAL_DISCOUNT_PERCENTAGE:
          ipi.setEmployeeExtraDiscountAmount(roundedDiscountAmount);
          ipi.setEmployeeExtraDiscountPercent(Double.parseDouble(discountPercent));
          logDebug("EmployeeExtraDiscountAmount is" + amount);
          logDebug("EmployeeExtraDiscountPercent is" + roundedAmount);
        break;
        default:
          logError("Invalid percentType parameter passed.");
      }
      ipi.markAsFinal();
      orderManager.updateOrder(order);
      
      // try to find the leftovers
      // the leftovers are the remainders from rounding every unit of the
      // CommerceItem
      // we are doing this after updating the original commerce item so we
      // can determine
      // that if we need to account for any fraction that is lost with
      // averaging the price of
      // the CI and the discount applied
      final double amountAfterDiscount = commerceItem.getPriceInfo().getAmount();
      final double averagePrice = amountAfterDiscount / commerceItem.getQuantity();
      final double roundedAverage = pricingTools.round(averagePrice);
      final double leftovers = commerceItem.getPriceInfo().getRawTotalPrice() - (roundedAverage * commerceItem.getQuantity());
      double lineItemTotal = commerceItem.getQuantity() * roundedAverage;
      lineItemTotal = pricingTools.round(lineItemTotal);
      discountAmount = roundedAmount - lineItemTotal;
      roundedDiscountAmount = pricingTools.round(discountAmount);
      
      logDebug("************************");
      logDebug("averagePrice-->" + averagePrice);
      logDebug("roundedAverage-->" + roundedAverage);
      logDebug("leftovers-->" + leftovers);
      logDebug("lineItemTotal-->" + lineItemTotal);
      logDebug("roundedDiscountAmount-->" + roundedDiscountAmount);
      logDebug("************************");
      
      ipi = (NMItemPriceInfo) commerceItem.getPriceInfo();
      ipi.setAmount(lineItemTotal);
      ipi.setDiscounted(true);
      promotionsApplied = ipi.getPromotionsApplied();
      ipi.setOrderDiscountShare(previousDiscountAmount + roundedDiscountAmount);
      ipi.setPromotionsApplied(promotionsApplied);
      order.setEmployeePromotionApplied(true);
      ipi.markAsFinal();
      orderManager.updateOrder(order);
    } else {
      
      // the amounts did not add up so try to see if it was a rounding
      // fluke and if we add a penny to the
      // customer it works
      
      roundedDiscountAmount = roundedDiscountAmount + 0.01;
      roundedDiscountAmount = pricingTools.round(roundedDiscountAmount);
      
      double theSum2 = newamnt + roundedDiscountAmount;
      theSum2 = pricingTools.round(theSum2);
      
      if (theSum2 == amount) {
        logDebug("HAD TO ADD a PENNY TO CUSTOMER TO FIX ROUNDING");
        final NMItemPriceInfo ipi = (NMItemPriceInfo) commerceItem.getPriceInfo();
        ipi.setAmount(newamnt);
        ipi.setDiscounted(true);
        ipi.setPromoPercentOff(discountPercent);
        
        final Map<String, Markdown> promotionsApplied = ipi.getPromotionsApplied();
        ipi.setPromotionsApplied(promotionsApplied);
        ipi.setOrderDiscountShare(roundedDiscountAmount);
        ipi.markAsFinal();
        orderManager.updateOrder(order);
      } else {
        logError("PERCENT OFF CALCULATION ERROR");
        logError("ROUNDING DID NOT WORK");
        logError("NOTIFY APPLICATION DEVELOPMENT");
        logError("amount-->" + amount);
        logError("theSum2-->" + theSum2);
        logError("roundedAmount-->" + roundedAmount);
        logError("discRate-->" + discRate);
        logError("discountAmount-->" + discountAmount);
        logError("roundedDiscountAmount-->" + roundedDiscountAmount);
        logError("newamnt-->" + newamnt);
        logError("roundedSum-->" + roundedSum);
        logError("**************END % ERRROR*************");
      }
    }
  }
  
  /**
   * This method returns the order which should be modified. <br>
   * 
   * @param context
   *          - the scenario context this is being evaluated in
   * @return Order object to modify
   */
  public Order getOrderToModify(final ProcessExecutionContext context) throws CommerceException, RepositoryException {
    
    final DynamoHttpServletRequest request = context.getRequest();
    
    // If we are executing this action in the context of a session then we
    // want to get a hold
    // of the OrderHolder and use the Order within it as the Order that we
    // want to get
    // information from.
    if (request != null) {
      final OrderHolder oh = (OrderHolder) request.resolveName(orderHolderComponent);
      return oh.getCurrent();
    }
    
    // Get the profile from the context
    final String profileId = ((MutableRepositoryItem) context.getSubject()).getRepositoryId();
    // logDebug("profileId from UpdateACtivatedPC is:"+profileId);
    
    // Only change orders that are in the "incomplete" state
    final List l = orderQueries.getOrdersForProfileInState(profileId, StateDefinitions.ORDERSTATES.getStateValue(OrderStates.INCOMPLETE));
    // Just modify the user's first order that's in the correct state
    if (l != null) {
      if (l.size() > 0) {
        return (Order) l.get(0);
      }
    }
    return null;
  }
  
  /**
   * This method is used to display all commerce items in the order.
   * 
   * @param order
   *          NMOrderImpl object
   */
  
  private void logDebugCommerceItems(final NMOrderImpl order) {
    if (isLoggingDebug()) {
      final List commerceItems = order.getCommerceItems();
      logDebug("COMMERCE ITEMS  START");
      for (int i = 0; i < commerceItems.size(); i++) {
        final NMCommerceItem currCi = (NMCommerceItem) commerceItems.get(i);
        logDebug("commerceItems[" + i + "] " + currCi.getProductId() + "/" + currCi.getCmosItemCode() + ":  qty=" + new Long(currCi.getQuantity()).toString() + " / price per item="
                + currCi.getPriceInfo().getListPrice());
      }
      logDebug("COMMERCE ITEMS END");
    }
  }
  
  public void setLoggingDebug(final boolean logDebug) {
    this.logDebug = logDebug;
  }
  
  public boolean isLoggingDebug() {
    return logDebug;
  }
  
  public void setLoggingError(final boolean logError) {
    this.logError = logError;
  }
  
  public boolean isLoggingError() {
    return logError;
  }
  
  private void logError(final String message) {
    if (isLoggingError()) {
      Nucleus.getGlobalNucleus().logError("EvaluateEmployeeDiscounts: " + message);
    }
  }
  
  private void logError(final String message, final Throwable exception) {
    if (isLoggingError()) {
      Nucleus.getGlobalNucleus().logError("EvaluateEmployeeDiscounts: " + message, exception);
    }
  }
  
  private void logDebug(final String message) {
    if (isLoggingDebug()) {
      Nucleus.getGlobalNucleus().logDebug("EvaluateEmployeeDiscounts: " + message);
    }
  }
  
  private SystemSpecs getSystemSpecs() {
    return (SystemSpecs) Nucleus.getGlobalNucleus().resolveName("/nm/utils/SystemSpecs");
  }
  
  public DynamoHttpServletRequest getRequest() {
    return ServletUtil.getCurrentRequest();
  }
  
}
