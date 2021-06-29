package com.nm.commerce.checkout;

import static com.nm.common.ServiceLevelConstants.GROUP_SERVICE_LEVELS;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import atg.commerce.CommerceException;
import atg.commerce.order.ShippingGroup;
import atg.nucleus.GenericService;
import atg.nucleus.Nucleus;
import atg.servlet.ServletUtil;

import com.nm.collections.ServiceLevel;
import com.nm.collections.ServiceLevelArray;
import com.nm.commerce.NMCommerceItem;
import com.nm.commerce.NMProfile;
import com.nm.commerce.NMRepositoryContactInfo;
import com.nm.commerce.order.NMOrderManager;
import com.nm.commerce.pagedef.definition.CheckoutPageDefinition;
import com.nm.commerce.pagedef.model.CheckoutPageModel;
import com.nm.commerce.pagedef.model.bean.ShippingGroupAux;
import com.nm.commerce.promotion.NMOrderImpl;
import com.nm.common.ServiceLevelConstants;
import com.nm.common.SmartPostServiceLevelSessionBean;
import com.nm.estimateddeliverydate.session.EstimatedDeliveryDateSession;
import com.nm.estimateddeliverydate.vo.EstimatedDeliveryDateCmosShippingVO;
import com.nm.estimateddeliverydate.vo.EstimatedDeliveryDateServiceLevelVO;
import com.nm.sitemessages.Message;
import com.nm.sitemessages.MessageDefs;
import com.nm.utils.LocalizationUtils;
import com.nm.utils.NmoUtils;
import com.nm.utils.StringUtilities;

/**
 * The Class ShippingInfoUtil, this class holds all the common methods to fetch the shipping service levels by the cmos flow and the old database flow
 * 
 * @author Cognizant
 * 
 */
public class ShippingInfoUtil extends GenericService {
  
  /** The nm profile. */
  private NMProfile nmProfile = (NMProfile) ServletUtil.getCurrentRequest().resolveName("/atg/userprofiling/Profile");
  
  /** The utils. */
  private LocalizationUtils utils = ((LocalizationUtils) Nucleus.getGlobalNucleus().resolveName("/nm/utils/LocalizationUtils"));
  
  /**
   * This method sets the shipping group, service level, delivery phone shipping group and shipping country to page modal. This method is overridden in EstimatedDeliveryDateShippingGroupUtil and
   * ShippingGroupUtil, In EstimatedDeliveryDateShippingGroupUtil the servicelevels will be fetched from cmos and in ShippingGroupUtil the service levels will be fetched from database.
   * 
   * @param order
   *          the order
   * @param pageModel
   *          the page model
   * @param checkoutPageDefinition
   *          the checkout page definition
   * @param shippingGroups
   *          the shipping groups
   */
  @SuppressWarnings("unchecked")
  protected void updateShippingGroups(final NMOrderImpl order, final CheckoutPageModel pageModel, final CheckoutPageDefinition checkoutPageDefinition,
          List<ShippingGroupAux> shippingGroups) {
    final List<ShippingGroupAux> delPhoneShpGrps = getDeliveryPhoneShippingGroups(shippingGroups);
    pageModel.setShippingGroups(shippingGroups.toArray(new ShippingGroupAux[shippingGroups.size()]));
    pageModel.setDeliveryPhoneShippingGroups(delPhoneShpGrps.toArray(new ShippingGroupAux[delPhoneShpGrps.size()]));
    final String selectedServiceLevel = findSelectedServiceLevelCode(order.getShippingGroups(), checkoutPageDefinition, order);
    pageModel.setSelectedServiceLevel(selectedServiceLevel);
    // INT-1378 set country name in Shipping address section
    if (CheckoutAPI.isInternationalSession(nmProfile)) {
      pageModel.setShippingCountry(utils.getCountryNameForCountryCode(shippingGroups.get(0).getAddress().getCountry()));
    }
  }

  /**
   * The purpose of this method is to populates the value to the ShippingGroupAux class, It retrieves the list of service levels from database and performs merge logic if the service levels are not
   * fetched from cmos.
   * 
   * @param order
   *          the order
   * @param checkoutPageDefinition
   *          the checkout page definition
   * @param serviceLevelsFromCmos
   *          the service levels from cmos
   * @return the shipping groups
   */
  public List<ShippingGroupAux> getShippingGroups(final NMOrderImpl order, final CheckoutPageDefinition checkoutPageDefinition, Set<ServiceLevel> serviceLevelsFromCmos) {
    final List<ShippingGroupAux> shippingGroups = new ArrayList<ShippingGroupAux>();
    @SuppressWarnings("unchecked")
    final List<ShippingGroup> orderShippingGroups = order.getShippingGroups();
    final String selectedServiceLevelCode = findSelectedServiceLevelCode(orderShippingGroups, checkoutPageDefinition, order);
    
    for (final ShippingGroup shippingGroup : orderShippingGroups) {
      final ShippingGroupAux sga = new ShippingGroupAux();
      final List<NMCommerceItem> items = CheckoutAPI.getShippingGroupItems(shippingGroup);
      final NMRepositoryContactInfo shippingAddress = CheckoutAPI.getShippingGroupAddress(shippingGroup);
      Set<ServiceLevel> validServiceLevels = new HashSet<ServiceLevel>();
      ServiceLevel currentServiceLevel = new ServiceLevel();
      if (NmoUtils.isNotEmptyCollection(serviceLevelsFromCmos)) {
        validServiceLevels = serviceLevelsFromCmos;
        currentServiceLevel = getCmosShippingGroupServiceLevel(serviceLevelsFromCmos, shippingGroup);
      } else {
        validServiceLevels = CheckoutAPI.getShippingGroupValidServiceLevels(shippingGroup, order);
        currentServiceLevel = CheckoutAPI.getShippingGroupServiceLevel(shippingGroup, validServiceLevels);
      }
      final List<NMCommerceItem> deliveryPhoneItems = new ArrayList<NMCommerceItem>();
      
      for (final NMCommerceItem item : items) {
        if ((item.getProduct().getFlgDeliveryPhone()) || (StringUtilities.isNotEmpty(item.getServicelevel()) && item.getServicelevel().equals(ServiceLevel.SL0_SERVICE_LEVEL_TYPE))) {
          deliveryPhoneItems.add(item);
        }
        if ((null != currentServiceLevel) && StringUtilities.isNotEmpty(selectedServiceLevelCode)) {
          final String itemServiceLevelCode = item.getServicelevel();
          setItemServiceLevelToGroupServiceLevel(itemServiceLevelCode, selectedServiceLevelCode, item);
        }
      }
      
      sga.setShippingGroupRef(shippingGroup);
      sga.setId(shippingGroup.getId());
      sga.setDescription(shippingGroup.getDescription());
      sga.setItems(items.toArray(new NMCommerceItem[items.size()]));
      sga.setDeliveryPhoneItems(deliveryPhoneItems.toArray(new NMCommerceItem[deliveryPhoneItems.size()]));
      sga.setAddress(shippingAddress);
      sga.setNickname(NMRepositoryContactInfo.createNickName(shippingAddress.getRepositoryItem()));
      sga.setServiceLevel(currentServiceLevel);
      sga.setValidServiceLevels(validServiceLevels.toArray(new ServiceLevel[validServiceLevels.size()]));
      
      shippingGroups.add(sga);
    }
    
    return shippingGroups;
  }

  /**
   * The purpose of the method is to get the cmos shipping group service level which is selected previously else this returns the default service level.
   * 
   * @param serviceLevelsFromCmos
   *          the service levels from cmos
   * @param shippingGroup
   *          the shipping group
   * @return the cmos shipping group service level
   */
  private ServiceLevel getCmosShippingGroupServiceLevel(Set<ServiceLevel> serviceLevelsFromCmos, ShippingGroup shippingGroup) {
    List<NMCommerceItem> commerceItems = ShippingGroupUtil.getInstance().getItems(shippingGroup);
    ServiceLevel currentServiceLevel = new ServiceLevel();
    for (NMCommerceItem commerceItem : commerceItems) {
      for (ServiceLevel cmosServicelevel : serviceLevelsFromCmos) {
        if (StringUtilities.isNotEmpty(commerceItem.getServicelevel()) && cmosServicelevel.getCode().equalsIgnoreCase(commerceItem.getServicelevel())) {
          return cmosServicelevel;
        } else if (cmosServicelevel.isDefaultServiceLevel()) {
          currentServiceLevel = cmosServicelevel;
        }
      }
    }
    return currentServiceLevel;
  }

  /**
   * The purpose of this method is to find the service level that will be auto selected for the user on the order review page by looping through all commerce items for all shipping groups to find a
   * non SL3 item, if one does not exist return SL3.
   * 
   * @param orderShippingGroups
   *          the order shipping groups
   * @param checkoutPageDefinition
   *          the checkout page definition
   * @param order
   *          the order
   * @return Service level code that is selected for user on order review page
   */
  public String findSelectedServiceLevelCode(final List<ShippingGroup> orderShippingGroups, final CheckoutPageDefinition checkoutPageDefinition, final NMOrderImpl order) {
    String selectedServiceLevel = "";
    final SmartPostServiceLevelSessionBean smartPostServiceLevelSessionBean = CheckoutComponents.getSmartPostServiceLevelSessionBean(ServletUtil.getCurrentRequest());
    final ServiceLevelArray serviceLevelArray = CheckoutComponents.getServiceLevelArray();
    final String shipToCountry = serviceLevelArray.getShipToCountry(order);
    final String checkoutPageDefinitionId = checkoutPageDefinition.getId();
    if ((null != orderShippingGroups) && (!orderShippingGroups.isEmpty())) {
      for (final ShippingGroup shippingGroup : orderShippingGroups) {
        final List<NMCommerceItem> items = CheckoutAPI.getShippingGroupItems(shippingGroup);
        if (null != items) {
          for (final NMCommerceItem item : items) {
            if (StringUtilities.isEmpty(smartPostServiceLevelSessionBean.getSmartPostSelectedServiceLevel()) && StringUtilities.isNotEmpty(checkoutPageDefinitionId)
                    && !(checkoutPageDefinitionId.equalsIgnoreCase(ServiceLevelConstants.ORDER_COMPLETE) || checkoutPageDefinitionId.equalsIgnoreCase(ServiceLevelConstants.PRINT_ORDER))) {
              if (StringUtilities.isNotEmpty(shipToCountry) && shipToCountry.equalsIgnoreCase("CA")) {
                item.setServicelevel(ServiceLevel.SL3_SERVICE_LEVEL_TYPE);
              } else {
                findDefaultServiceLevelBasedOnServiceCall(serviceLevelArray, item);
              }
            }
            selectedServiceLevel = item.getServicelevel();
            // SmartPost : changing the default service level by calling determineFreeShippingServiceLevel method based on the amount and country
            if (StringUtilities.isNotEmpty(selectedServiceLevel) && !selectedServiceLevel.equals(serviceLevelArray.determineFreeShippingServiceLevel())) {
              break;
            }
          }
        }
        // SmartPost : changing the default service level by calling determineFreeShippingServiceLevel method based on the amount and country
        if (StringUtilities.isNotEmpty(selectedServiceLevel) && !selectedServiceLevel.equals(serviceLevelArray.determineFreeShippingServiceLevel())) {
          break;
        }
      }
    }
    return selectedServiceLevel;
  }

  /**
   * The purpose of this method is to find the default service level to be selected at the first time in order review page based on the type of service call
   * 
   * @param serviceLevelArray
   *          the service level array
   * @param item
   *          the item
   */
  private void findDefaultServiceLevelBasedOnServiceCall(final ServiceLevelArray serviceLevelArray, final NMCommerceItem item) {
    String defaultSelectedServiceLevel = serviceLevelArray.determineFreeShippingServiceLevel();
    EstimatedDeliveryDateCmosShippingVO cmosShippingInfo = CheckoutComponents.getEstimatedDeliveryDateSession(ServletUtil.getCurrentRequest()).getEstimatedDeliveryDateCmosShippingVO();
    // if in case of ESB cmos call, the default is taken from the session
    if (null != cmosShippingInfo) {
      List<EstimatedDeliveryDateServiceLevelVO> serviceLevelsFromCmos = cmosShippingInfo.getServiceLevels();
      if (NmoUtils.isNotEmptyCollection(serviceLevelsFromCmos)) {
        for (EstimatedDeliveryDateServiceLevelVO serviceLevelFromCmos : serviceLevelsFromCmos) {
          if (serviceLevelFromCmos.isDefaultSelected()) {
            defaultSelectedServiceLevel = serviceLevelFromCmos.getServiceLevelCode();
            break;
          }
        }
      }
    }
    
    item.setServicelevel(defaultSelectedServiceLevel);
  }
  
  /**
   * Gets the delivery phone shipping groups.
   * 
   * @param shippingGroups
   *          the shipping groups
   * @return the delivery phone shipping groups
   */
  public List<ShippingGroupAux> getDeliveryPhoneShippingGroups(final List<ShippingGroupAux> shippingGroups) {
    final List<ShippingGroupAux> deliveryPhoneShippingGroups = new ArrayList<ShippingGroupAux>();
    
    for (final ShippingGroupAux shippingGroup : shippingGroups) {
      if (shippingGroup.getDeliveryPhoneItems().length > 0) {
        deliveryPhoneShippingGroups.add(shippingGroup);
      }
    }
    
    return deliveryPhoneShippingGroups;
  }
  
  /**
   * Gets the valid service levels and perform the merge logic.
   * 
   * @param shippingGroups
   *          the shipping groups
   * @return the valid service levels
   */
  public Set<ServiceLevel> getValidServiceLevels(final List<ShippingGroupAux> shippingGroups) {
    final List<Set<ServiceLevel>> shippingGroupServiceLevels = new ArrayList<Set<ServiceLevel>>();
    
    for (final ShippingGroupAux sga : shippingGroups) {
      for (final ServiceLevel sl : sga.getValidServiceLevels()) {
        if ("Standard - 5-7 Business Days".equals(sl.getShortDesc())) {
          sl.setShortDesc("Standard");
        }
      }
      shippingGroupServiceLevels.add(new HashSet<ServiceLevel>(Arrays.asList(sga.getValidServiceLevels())));
    }
    
    return CheckoutAPI.mergeServiceLevels(shippingGroupServiceLevels);
  }
  
  /**
   * Sets the item service level to group service level and captures the message on it.
   * 
   * @param itemServiceLevelCode
   *          the item service level code
   * @param currentServiceLevelCode
   *          the current service level code
   * @param item
   *          the item
   */
  public void setItemServiceLevelToGroupServiceLevel(final String itemServiceLevelCode, final String currentServiceLevelCode, final NMCommerceItem item) {
    final Boolean isItemSLMatchGroupSL = ((StringUtilities.isNotEmpty(itemServiceLevelCode)) && (itemServiceLevelCode.equals(currentServiceLevelCode)));
    final Boolean isItemGwpAndGroupSL0 = ((item.isGwpItem()) && (currentServiceLevelCode.equals(ServiceLevel.SL0_SERVICE_LEVEL_TYPE)));
    final Boolean isPickupInStoreItem = StringUtilities.isNotEmpty((item.getPickupStoreNo()));
    if (!isPickupInStoreItem && isItemSLMatchGroupSL && !isItemGwpAndGroupSL0) {
      item.setServicelevel(currentServiceLevelCode);
    }
  }
  
  @SuppressWarnings("unchecked")
  public List<Message> updateServiceLevel(NMOrderManager orderMgr, NMOrderImpl order, NMProfile profile, ShippingGroup shippingGroup, ServiceLevel serviceLevel, String currentServiceLevelCode,
          boolean serviceLevelsFetchedFromCmos)
          throws CommerceException {
    List<Message> messages = new ArrayList<Message>();
    ShippingUtil shippingUtil = ShippingUtil.getInstance();
    ServiceLevelArray serviceLevelArray = CheckoutComponents.getServiceLevelArray();
    if (serviceLevelsFetchedFromCmos) {
      // Service level selected is set directly to the commerce items when service levels are fetched from cmos, NO check is made if the item is eligible for selected service level
      List<NMCommerceItem> commerceItems = order.getCommerceItems();
      for (NMCommerceItem commerceItem : commerceItems) {
        commerceItem.setServicelevel(serviceLevel.getCode());
      }
    } else {
      ServiceLevelUtil serviceLevelUtil = ServiceLevelUtil.getInstance();
      serviceLevelUtil.setShippingGroupItemServiceLevels(order, shippingGroup, serviceLevel);
    }
    
    shippingUtil.updateShippingMethodsInShippingGroups(profile, messages, shippingGroup, currentServiceLevelCode);
    shippingUtil.hasNonContinentalUSStates(order, messages, currentServiceLevelCode);
    
    for (Message message : messages) {
      message.setFieldId(GROUP_SERVICE_LEVELS);
    }

    if (order.getHasActiveShippingPromo() && !serviceLevel.isPromotional() && order.isShopRunnerPromoActive() && !serviceLevel.isShopRunner()) {
      Message msg = MessageDefs.getMessage(MessageDefs.MSG_Promotion_ShipMethodDisqualification);
      msg.setMsgText(serviceLevelArray.getPromotionOrShoprunnerDisqualificationMessage());
      msg.setFieldId(GROUP_SERVICE_LEVELS);
      msg.setError(true);
      messages.add(msg);
    } else if (order.getHasActiveShippingPromo() && !serviceLevel.isPromotional()) {
      Message msg = MessageDefs.getMessage(MessageDefs.MSG_Promotion_ShipMethodDisqualification);
      msg.setMsgText(serviceLevelArray.getPromotionDisqualificationMessage());
      msg.setFieldId(GROUP_SERVICE_LEVELS);
      msg.setError(true);
      messages.add(msg);
    } else if (order.isShopRunnerPromoActive() && !serviceLevel.isShopRunner()) {
      Message msg = MessageDefs.getMessage(MessageDefs.MSG_Promotion_ShopRunnerDisqualification);
      msg.setMsgText(serviceLevelArray.getShoprunnerDisqualificationMessage());
      msg.setFieldId(GROUP_SERVICE_LEVELS);
      msg.setError(true);
      messages.add(msg);
    }
    
    orderMgr.updateOrder(order);

    return messages;
  }
  
  /**
   * The purpose of this method is to clear estimated delivery date session info values.
   */
  public void clearEstimatedDeliveryDateInfo() {
    // Clear session info
    EstimatedDeliveryDateSession getEstimatedDeliveryDateSession = CheckoutComponents.getEstimatedDeliveryDateSession(ServletUtil.getCurrentRequest());
    getEstimatedDeliveryDateSession.setEstimatedDeliveryDateCmosShippingVO(new EstimatedDeliveryDateCmosShippingVO());
    getEstimatedDeliveryDateSession.setExcludeServiceLevelsFromDisplay(false);
  }

}
