package com.nm.commerce.checkout;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import atg.commerce.order.ShippingGroup;
import atg.repository.Repository;
import atg.repository.RepositoryException;
import atg.repository.RepositoryItem;
import atg.repository.RepositoryView;
import atg.repository.rql.RqlStatement;
import atg.servlet.DynamoHttpServletRequest;
import atg.servlet.ServletUtil;

import com.nm.abtest.AbTestHelper;
import com.nm.collections.ServiceLevel;
import com.nm.collections.ServiceLevelArray;
import com.nm.commerce.NMCommerceItem;
import com.nm.commerce.catalog.NMProdCountry;
import com.nm.commerce.catalog.NMProduct;
import com.nm.commerce.promotion.NMOrderImpl;
import com.nm.common.ServiceLevelConstants;
import com.nm.components.CommonComponentHelper;
import com.nm.estimateddeliverydate.session.EstimatedDeliveryDateSession;
import com.nm.estimateddeliverydate.util.EstimatedDeliveryDateShippingGroupUtil;
import com.nm.estimateddeliverydate.vo.EstimatedDeliveryDateCmosShippingVO;
import com.nm.estimateddeliverydate.vo.EstimatedDeliveryDateServiceLevelVO;
import com.nm.formhandler.ShoppingCartHandler;
import com.nm.utils.LocalizationUtils;
import com.nm.utils.NmoUtils;
import com.nm.utils.PromotionsHelper;
import com.nm.utils.StringUtilities;

/* package */class ServiceLevelUtil {
  private static ServiceLevelUtil INSTANCE; // avoid static initialization
  
  // private constructor enforces singleton behavior
  private ServiceLevelUtil() {}
  
  public static synchronized ServiceLevelUtil getInstance() {
    INSTANCE = INSTANCE == null ? new ServiceLevelUtil() : INSTANCE;
    return INSTANCE;
  }
  
  private ServiceLevel[] serviceLevelsValid = null;
  
  public ServiceLevel getShippingGroupValidServiceLevel(final NMOrderImpl order, final ShippingGroup shippingGroup, final String serviceLevelCode) {
    final Set<ServiceLevel> serviceLevels = getShippingGroupValidServiceLevelsWithAbTestsAndPromotions(shippingGroup, order);
    
    for (final ServiceLevel validServiceLevel : serviceLevels) {
      if (validServiceLevel.getCode().equals(serviceLevelCode)) {
        return validServiceLevel;
      }
    }
    
    return null;
  }
  
  public ServiceLevel getShippingGroupServiceLevel(final ShippingGroup shippingGroup) {
    return getShippingGroupServiceLevel(shippingGroup, getShippingGroupValidServiceLevels(shippingGroup));
  }
  
  public ServiceLevel getShippingGroupServiceLevelWithAbTestsAndPromotionsApplied(final ShippingGroup shippingGroup, final NMOrderImpl order) {
    return getShippingGroupServiceLevel(shippingGroup, getShippingGroupValidServiceLevelsWithAbTestsAndPromotions(shippingGroup, order));
  }
  
  public ServiceLevel getShippingGroupServiceLevel(final ShippingGroup shippingGroup, final Set<ServiceLevel> validServiceLevels) {
    if (validServiceLevels.isEmpty()) {
      return null;
    } else if (validServiceLevels.size() == 1) {
      return validServiceLevels.iterator().next();
    }
    
    final List<NMCommerceItem> items = ShippingGroupUtil.getInstance().getItems(shippingGroup);
    final String shipToCountry = ShippingGroupUtil.getInstance().getCountry(shippingGroup);
    
    for (final NMCommerceItem item : items) {
      final Set<ServiceLevel> itemValidServiceLevels = getCommerceItemValidServiceLevels(item, shipToCountry);
      
      if (itemValidServiceLevels.size() > 1) {
        for (final ServiceLevel validServiceLevel : validServiceLevels) {
          // SmartPostABTest : comparing the service level with the item service level code which is taken based on smart post AB test group
          if (validServiceLevel.getCode().equalsIgnoreCase(item.getServicelevel())) {
            return validServiceLevel;
          } else if (StringUtilities.isNotEmpty(item.getServicelevel()) && item.getServicelevel().equals(ServiceLevel.SL0_SERVICE_LEVEL_TYPE)) {
            final ServiceLevel sl0 = CheckoutAPI.getServiceLevelByCodeAndCountry(ServiceLevel.SL0_SERVICE_LEVEL_TYPE, LocalizationUtils.US_COUNTRY_CODE);
            return sl0;
          }
        }
      }
    }
    
    return null;
  }
  
  public Set<ServiceLevel> getShippingGroupValidServiceLevels(final ShippingGroup shippingGroup) {
    final Set<ServiceLevel> serviceLevels = new TreeSet<ServiceLevel>();
    final List<Set<ServiceLevel>> itemServiceLevels = new ArrayList<Set<ServiceLevel>>();
    
    if (shippingGroup == null) {
      return serviceLevels;
    }
    
    final String shipToCountry = ShippingGroupUtil.getInstance().getCountry(shippingGroup);
    final List<NMCommerceItem> items = ShippingGroupUtil.getInstance().getItems(shippingGroup);
    for (final NMCommerceItem item : items) {
      final Set<ServiceLevel> itemSLs = getCommerceItemValidServiceLevels(item, shipToCountry);
      itemServiceLevels.add(new HashSet<ServiceLevel>(itemSLs));
    }
    
    serviceLevels.addAll(mergeServiceLevels(itemServiceLevels));
    
    return serviceLevels;
  }
  
  public Set<ServiceLevel> getShippingGroupValidServiceLevelsWithAbTestsAndPromotions(final ShippingGroup shippingGroup, final NMOrderImpl order) {
    return applyAbTestsAndPromotions(getShippingGroupValidServiceLevels(shippingGroup), order);
  }
  
  public Set<ServiceLevel> getCommerceItemValidServiceLevels(final NMCommerceItem item, final String shipToCountry) {
    final Set<ServiceLevel> serviceLevels = new TreeSet<ServiceLevel>();
    // WR 41198 - retrieve service levels for drop ship items
    if (!item.isDropship() || item.getProduct().getIsDropShipMerchTypeEligibleToDisplayServiceLevels()) {
      serviceLevels.addAll(getProductValidServiceLevels(item.getProduct(), shipToCountry));
    }
    return serviceLevels;
  }
  
  public Set<ServiceLevel> getProductValidServiceLevels(final NMProduct product, final String shipToCountry) {
    String serviceLevelCodes = null;
    final String merchandiseType = product.getMerchandiseType();
    final Map<String, NMProdCountry> countryMap = product.getCountryMap();
    
    if (countryMap != null) {
      final NMProdCountry prodCountry = countryMap.get(shipToCountry);
      
      if (prodCountry != null) {
        serviceLevelCodes = prodCountry.getServiceLevelCodes();
      }
    }
    // SmartPost : retrieving the valid service levels based on AB test and estimated ship dates based on the order type
    final ShoppingCartHandler cart = CheckoutComponents.getCartHandler(ServletUtil.getCurrentRequest());
    final NMOrderImpl order = cart.getNMOrder();
    
    return new TreeSet<ServiceLevel>(Arrays.asList(CheckoutComponents.getServiceLevelArray().getValidServiceLevels(shipToCountry, serviceLevelCodes, merchandiseType,
            CheckoutComponents.getServiceLevelArray().getServiceLevelGroupBasedOnAbTest(), CheckoutComponents.getServiceLevelArray().getDeliveryDaysBasedOnPromotionAndOrder(order))));
  }
  
  public ServiceLevelUtil setShippingGroupItemServiceLevels(final NMOrderImpl order, final ShippingGroup shippingGroup, final ServiceLevel serviceLevel) {
    final String sgId = shippingGroup.getId();
    @SuppressWarnings("unchecked")
    final List<ShippingGroup> orderShippingGroups = order.getShippingGroups();
    final ServiceLevelArray serviceLevelArray = CheckoutComponents.getServiceLevelArray();
    for (final ShippingGroup orderShippingGroup : orderShippingGroups) {
      final String orderSgId = orderShippingGroup.getId();
      
      if (orderSgId.equals(sgId)) {
        final String shipToCountry = ShippingGroupUtil.getInstance().getCountry(shippingGroup);
        final List<NMCommerceItem> items = ShippingGroupUtil.getInstance().getItems(orderShippingGroup);
        
        for (final NMCommerceItem item : items) {
          final Set<ServiceLevel> itemValidLevels = getCommerceItemValidServiceLevels(item, shipToCountry);
          
          if ((serviceLevel != null) && (ServiceLevel.SL0_SERVICE_LEVEL_TYPE.equals(serviceLevel.getCode()) || itemValidLevels.contains(serviceLevel))) {
            if (ServiceLevel.SL0_SERVICE_LEVEL_TYPE.equals(serviceLevel.getCode()) && item.isGwpItem()) {
              item.setServicelevel(serviceLevelArray.determineFreeShippingServiceLevel());// SmartPost : setting the service level dynamically based on the free shipping
            } else {
              item.setServicelevel(serviceLevel.getCode());
            }
          } else if (!itemValidLevels.isEmpty()) {
            final ServiceLevel lowestServiceLevel = itemValidLevels.iterator().next();
            item.setServicelevel(lowestServiceLevel.getCode());
          } else {
            // must set required service level property even if no valid service levels for items
            final ServiceLevel[] serviceLevels = serviceLevelArray.getAllServiceLevels();
            final Set<ServiceLevel> sortedServiceLevels = new TreeSet<ServiceLevel>(Arrays.asList(serviceLevels));
            final String serviceLevelCode = sortedServiceLevels.isEmpty() ? serviceLevelArray.determineFreeShippingServiceLevel() : sortedServiceLevels.iterator().next().getCode();
            item.setServicelevel(serviceLevelCode);
          }
        }
      }
    }
    
    return this;
  }
  
  /**
   * Method sets the service level of all commerce items in all shipping groups to the specific serviceLevel if the commerce item supports that level; useful for shoprunner primarily since you would
   * use shippinggrouputil.updateAllServiceLevels otherwise
   * 
   * @param order
   * @param serviceLevel
   * @return
   */
  public ServiceLevelUtil setShippingGroupItemsServiceLevel(final NMOrderImpl order, final String serviceLevelCode) {
    if (serviceLevelCode != null) {
      @SuppressWarnings("unchecked")
      final List<ShippingGroup> orderShippingGroups = order.getShippingGroups();
      
      for (final ShippingGroup orderShippingGroup : orderShippingGroups) {
        
        final String shipToCountry = ShippingGroupUtil.getInstance().getCountry(orderShippingGroup);
        final List<NMCommerceItem> items = ShippingGroupUtil.getInstance().getItems(orderShippingGroup);
        
        for (final NMCommerceItem item : items) {
          final Set<ServiceLevel> itemValidLevels = getCommerceItemValidServiceLevels(item, shipToCountry);
          
          for (final ServiceLevel serviceLevel : itemValidLevels) {
            if (serviceLevel.getCode().equals(serviceLevelCode)) {
              item.setServicelevel(serviceLevelCode);
              break;
            }
          }
        }
      }
    }
    
    return this;
  }
  
  public boolean getHasMultipleServiceLevels(final NMOrderImpl order) {
    if (!ShippingUtil.getInstance().getIsShipToMultiple(order)) {
      return false;
    }
    
    ServiceLevel serviceLevel = null;
    final List<ShippingGroup> shippingGroups = ShippingGroupUtil.getInstance().getShippingGroups(order);
    
    for (final ShippingGroup shippingGroup : shippingGroups) {
      final ServiceLevel sgsl = getShippingGroupServiceLevel(shippingGroup);
      if ((serviceLevel != null) && (sgsl != null) && !serviceLevel.equals(sgsl)) {
        return true;
      }
      serviceLevel = sgsl;
    }
    
    return false;
  }
  
  public Set<ServiceLevel> mergeServiceLevels(final List<Set<ServiceLevel>> serviceLevelSets) {
    final Set<ServiceLevel> serviceLevels = new TreeSet<ServiceLevel>();
    
    if (serviceLevelSets.isEmpty()) {
      return serviceLevels;
    }
    
    // intersect of item service level sets with one or more service levels
    for (final Set<ServiceLevel> itemServiceLevels : serviceLevelSets) {
      if (itemServiceLevels.size() > 1) {
        if (serviceLevels.isEmpty()) {
          serviceLevels.addAll(itemServiceLevels);
        } else {
          serviceLevels.retainAll(itemServiceLevels);
        }
      }
    }
    
    // since empty, intersect of all item service level sets
    if (serviceLevels.isEmpty()) {
      for (final Set<ServiceLevel> itemServiceLevels : serviceLevelSets) {
        if (!itemServiceLevels.isEmpty()) {
          if (serviceLevels.isEmpty()) {
            serviceLevels.addAll(itemServiceLevels);
          } else {
            serviceLevels.retainAll(itemServiceLevels);
          }
        }
      }
    }
    
    return serviceLevels;
  }
  
  protected Set<ServiceLevel> applyAbTestsAndPromotions(final Set<ServiceLevel> serviceLevels, final NMOrderImpl order) {
    applyServiceLevelABTests(serviceLevels, ServletUtil.getCurrentRequest());
    applyServiceLevelPromotions(serviceLevels, order);
    return serviceLevels;
  }
  
  private ServiceLevelUtil applyServiceLevelABTests(final Set<ServiceLevel> serviceLevels, final DynamoHttpServletRequest request) {
    // WR34301
    final String cuspServiceLevelAmountTest = AbTestHelper.getAbTestValue(request, "cuspServiceLevelAmountTest");
    if ("TestAmount".equals(cuspServiceLevelAmountTest)) {
      for (final ServiceLevel serviceLevel : serviceLevels) {
        if ("SL2".equals(serviceLevel.getCode())) {
          serviceLevel.setAmount(8.0);
          return this;
        }
      }
    }
    return this;
  }
  
  private ServiceLevelUtil applyServiceLevelPromotions(final Set<ServiceLevel> serviceLevels, final NMOrderImpl order) {
    final PromotionsHelper promotionsHelper = CommonComponentHelper.getPromotionsHelper();
    final Set<ServiceLevel> sortedServiceLevels = new TreeSet<ServiceLevel>(serviceLevels);
    
    if (!sortedServiceLevels.isEmpty() && order.hasShippingPromotion() && promotionsHelper.hasActiveShippingPromo(order)) {
      
      // get lowest service level (assumes sorted descending)
      sortedServiceLevels.iterator().next().makePromotional();
    }
    
    if (!sortedServiceLevels.isEmpty() && order.isShopRunnerPromoActive()) {
      for (final ServiceLevel sl : serviceLevels) {
        if (sl.getCode().equals("SL2")) {
          sl.makeShopRunner();
        } else if (sl.getCode().equals("SL3") && order.isAlipay()) {
          sl.makeAlipayShopRunner();
        }
      }
    }
    
    return this;
  }
  
  /**
   * This method returns ServiceLevel object for a given service level Code and Country code
   * 
   * @param serviceLevelCode
   * @param countryCode
   * @return ServiceLevel
   */
  public ServiceLevel getServiceLevelByCodeAndCountry(final String serviceLevelCode, final String countryCode) {
    ServiceLevel serviceLevel = null;
    final Object[] params = new Object[2];
    params[0] = serviceLevelCode;
    params[1] = countryCode;
    final String query = "((code = ?0)) and (countryCode = ?1)";
    final Repository serviceLevelRepository = CommonComponentHelper.getServiceLevelRepository();
    RepositoryView view;
    try {
      view = serviceLevelRepository.getView(ServiceLevelConstants.INTERNAIONAL_SERVICE_LEVEL);
      final RqlStatement statement = RqlStatement.parseRqlStatement(query);
      final RepositoryItem[] items = statement.executeQuery(view, params);
      // service level code and country combination must be unique
      if (items.length == 1) {
        serviceLevel = new ServiceLevel(items[0]);
      }
    } catch (final RepositoryException e) {
      e.printStackTrace();
    }
    return serviceLevel;
  }
  
  public List<String> updateProductCountryValidServiceLevels(ShoppingCartHandler cart, final List commerceItems, final String shipToCountry, final String currentServiceLevel) {
    
    NMProduct product = null;
    List<String> popupMessage = new ArrayList<String>();
    EstimatedDeliveryDateCmosShippingVO estimatedDeliveryDateCmosShippingVO = null;
    EstimatedDeliveryDateSession estimatedDeliverySession = CheckoutComponents.getEstimatedDeliveryDateSession(ServletUtil.getCurrentRequest());
    if(estimatedDeliverySession != null) {
      estimatedDeliveryDateCmosShippingVO = estimatedDeliverySession.getEstimatedDeliveryDateCmosShippingVO();
    }
    Set<ServiceLevel> serviceLevelsUpdated = new HashSet<ServiceLevel>();
    Set<ServiceLevel> serviceLevelsFromCmos = new HashSet<ServiceLevel>();
    if(estimatedDeliveryDateCmosShippingVO != null) {
      List<EstimatedDeliveryDateServiceLevelVO> cmosShippingServiceLevels = estimatedDeliveryDateCmosShippingVO.getServiceLevels();
      if(!NmoUtils.isEmptyCollection(cmosShippingServiceLevels)) {        
        serviceLevelsFromCmos = EstimatedDeliveryDateShippingGroupUtil.getInstance().convertCmosServiceLevelResponseTOServiceLevelObject(cmosShippingServiceLevels);
        serviceLevelsUpdated = serviceLevelsFromCmos;
      }
    }
    if(serviceLevelsFromCmos != null && serviceLevelsFromCmos.isEmpty() && serviceLevelsValid != null){
      serviceLevelsUpdated = new HashSet<ServiceLevel>(Arrays.asList(serviceLevelsValid)); 
    }
    
    for (Object cItem : commerceItems){
     String[] serviceLevelCodes = null;
     Map<String, NMProdCountry> countryMap = new HashMap<String, NMProdCountry>();
     NMCommerceItem comItem = (NMCommerceItem) cItem;
     product = comItem.getProduct();
     countryMap = product.getCountryMap();
     
     if (countryMap != null) {
       final NMProdCountry prodCountry = countryMap.get(shipToCountry);
       
       if (prodCountry != null && prodCountry.getServiceLevelCodes() != null) {
         serviceLevelCodes = prodCountry.getServiceLevelCodes().split(",");
       }
     }
     String slevel = new String();
     if((currentServiceLevel != null) && (!currentServiceLevel.isEmpty())){
       slevel = currentServiceLevel;
     }else{
       slevel = comItem.getServicelevel();
     }
     if(!(slevel != null && slevel.equals(ServiceLevel.SL0_SERVICE_LEVEL_TYPE))){   
       List<String> listSL = (serviceLevelCodes == null) ? (new ArrayList<String>()) : (Arrays.asList(serviceLevelCodes));
       if(listSL != null && listSL.contains(slevel)){
         comItem.setServicelevel(slevel);
       } else if(serviceLevelsUpdated != null  && (listSL != null && !listSL.isEmpty())){
         comItem.setServicelevel(product.getServiceLevel());
         Iterator<ServiceLevel> serviceLevelIterator = serviceLevelsUpdated.iterator();
         while(serviceLevelIterator.hasNext()){
           ServiceLevel sl = serviceLevelIterator.next();
           if(sl != null && sl.getCode() != null && sl.getCode().equals(slevel)){
             popupMessage.add(product.getCmosItemCode()+" cannot be shipped through "+sl.getShortDesc()+".");
           }
         }
       }
     }
    }
    return popupMessage;
  }
  
  public ServiceLevel[] getServiceLevelsValid() {
    return serviceLevelsValid;
  }

  public void setServiceLevelsValid(ServiceLevel[] serviceLevelsValid) {
    this.serviceLevelsValid = serviceLevelsValid;
    
  }
  
}
