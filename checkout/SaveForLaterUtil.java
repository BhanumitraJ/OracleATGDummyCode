package com.nm.commerce.checkout;

import static com.nm.international.fiftyone.checkoutapi.OrderRepositoryConstants.OPTION_CHOICES;

import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import atg.commerce.CommerceException;
import atg.commerce.catalog.CatalogTools;
import atg.commerce.order.CommerceItemManager;
import atg.commerce.order.HardgoodShippingGroup;
import atg.commerce.order.OrderManager;
import atg.commerce.pricing.ItemPriceInfo;
import atg.core.util.StringUtils;
import atg.nucleus.GenericService;
import atg.repository.MutableRepository;
import atg.repository.MutableRepositoryItem;
import atg.repository.RepositoryItem;
import atg.repository.RepositoryView;
import atg.repository.rql.RqlStatement;
import atg.servlet.DynamoHttpServletRequest;
import atg.servlet.ServletUtil;

import com.nm.commerce.NMCommerceItem;
import com.nm.commerce.NMCommerceItemManager;
import com.nm.commerce.NMOrderHolder;
import com.nm.commerce.NMProfile;
import com.nm.commerce.catalog.NMProduct;
import com.nm.commerce.catalog.NMSuite;
import com.nm.commerce.checkout.beans.ResultBean;
import com.nm.commerce.checkout.beans.ResultBeanHelper;
import com.nm.commerce.checkout.beans.SaveForLaterItemCollection;
import com.nm.commerce.order.NMOrderManager;
import com.nm.commerce.promotion.NMOrderImpl;
import com.nm.components.CommonComponentHelper;
import com.nm.formhandler.NMProfileFormHandler;
import com.nm.formhandler.ShoppingCartHandler;
import com.nm.international.fiftyone.checkoutapi.OrderRepositoryConstants;
import com.nm.sitemessages.Message;
import com.nm.utils.SystemSpecs;

/**
 * Package access - outside of this package, methods should be accessed through CheckoutAPI.
 */
/* package */class SaveForLaterUtil {
  
  private static SaveForLaterUtil INSTANCE; // avoid static initialization
  
  // private constructor enforces singleton behavior
  private SaveForLaterUtil() {}
  
  public static synchronized SaveForLaterUtil getInstance() {
    INSTANCE = (INSTANCE == null) ? new SaveForLaterUtil() : INSTANCE;
    return INSTANCE;
  }
  
  public SaveForLaterItemCollection getSaveForLaterItems(final NMProfile profile, final OrderManager orderMgr) {
    final SaveForLaterItemCollection returnValue = new SaveForLaterItemCollection();
    int noLongerAvailableItemCount = 0;
    
    try {
      @SuppressWarnings("unchecked")
      final Set<RepositoryItem> saveForLaterCartItems = (Set<RepositoryItem>) profile.getPropertyValue("saveForLaterCartItems");
      final Iterator<RepositoryItem> iterator = saveForLaterCartItems.iterator();
      
      while (iterator.hasNext()) {
        final RepositoryItem saveForLaterItem = iterator.next();
        NMCommerceItem commerceItem = null;
        try {
          commerceItem = createCommerceItemFromSflItem(orderMgr, saveForLaterItem);
        } catch (final NullPointerException npe) {
          // if product no longer exists, remove it from the saved for later items.
          // nothing can be displayed to the customer
          ++noLongerAvailableItemCount;
          iterator.remove();
        }
        
        if (commerceItem != null) {
          commerceItem.setId(saveForLaterItem.getRepositoryId()); // set the id on the commerceItem to match the sflItem
          
          // if item is no longer sellable then remove it from the saved for later items. But go ahead and insert it
          // into the returnValue collection so that it can be displayed to the customer one last time.
          if (!commerceItem.getProductDisplayFlag()) {
            ++noLongerAvailableItemCount;
            iterator.remove();
          }
          
          returnValue.add(commerceItem);
        }
      }
    } catch (final Exception exception) {
      final GenericService config = CheckoutComponents.getConfig();
      config.logError(exception);
    }
    
    final SaveForLaterComparator c = new SaveForLaterComparator();
    Collections.sort(returnValue, c);
    
    returnValue.setNoLongerAvailableItemCount(noLongerAvailableItemCount);
    return returnValue;
  }
  
  public ResultBean moveSFLItemToCart(final ShoppingCartHandler cart, final String sflItemId, final NMProfileFormHandler profileFormHandler) throws Exception {
    // add cloned commerce item to profile save for later items
    final ResultBean result = new ResultBean();
    
    final NMOrderImpl order = cart.getNMOrder();
    final MutableRepositoryItem profile = profileFormHandler.getProfile();
    
    @SuppressWarnings("unchecked")
    final Set<RepositoryItem> saveForLaterItems = (Set<RepositoryItem>) profile.getPropertyValue("saveForLaterCartItems");
    
    if (saveForLaterItems != null) {
      final Iterator<RepositoryItem> i = saveForLaterItems.iterator();
      
      while (i.hasNext()) {
        final RepositoryItem saveForLaterItem = i.next();
        
        if ((saveForLaterItem != null) && saveForLaterItem.getRepositoryId().equals(sflItemId)) {
          final Integer quantity = (Integer) saveForLaterItem.getPropertyValue("quantity");
          
          if (saveForLaterItem != null) {
            NMCommerceItem commerceItem = null;
            try {
              final OrderManager orderMgr = cart.getOrderManager();
              final CommerceItemManager commerceItemManager = orderMgr.getCommerceItemManager();
              commerceItem = createCommerceItemFromSflItem(orderMgr, saveForLaterItem);
              commerceItem.setSaveForLaterTransfer(true);
              
              // add commerce item to order
              commerceItemManager.addAsSeparateItemToOrder(order, commerceItem);
              
              final DynamoHttpServletRequest request = ServletUtil.getCurrentRequest();
              final SystemSpecs systemSpecs = getSystemSpecs(request);
              
              final String styleCatCode = systemSpecs.getStyleCatalogCode();
              final String styleSystemCode = systemSpecs.getStyleSystemCode();
              String currentSystemCode = (String) order.getPropertyValue("systemCode");
              final String systemCode = cart.getSystemCode();
              
              if (currentSystemCode == null) {
                currentSystemCode = systemCode;
              }
              
              if ((styleCatCode != null) && (styleSystemCode != null) && commerceItem.getCmosCatalogId().equalsIgnoreCase(styleCatCode)) {
                order.setSystemCode(styleSystemCode);
              } else if (currentSystemCode.equalsIgnoreCase(styleSystemCode)) {
                // do nothing
              } else {
                order.setSystemCode(systemCode);
              }
              
              @SuppressWarnings("unchecked")
              final List<HardgoodShippingGroup> shippingGroups = order.getShippingGroups();
              HardgoodShippingGroup shippingGroup = null;
              
              if ((shippingGroups == null) || shippingGroups.isEmpty()) {
                shippingGroup = (HardgoodShippingGroup) orderMgr.getShippingGroupManager().createShippingGroup();
                orderMgr.getShippingGroupManager().addShippingGroupToOrder(order, shippingGroup);
              } else {
                shippingGroup = shippingGroups.get(0);
              }
              
              if (shippingGroup != null) {
                // alot of this code is copied from the ShoppingCartHandler.addAsSeparateItemToOrder method
                // if the profile does not have a shipping address then create one for it.
                if (profile.getPropertyValue("shippingAddress") == null) {
                  // create new shippingAddress for the Profile
                  final MutableRepository profileRepository = (MutableRepository) atg.nucleus.Nucleus.getGlobalNucleus().resolveName("/atg/userprofiling/ProfileAdapterRepository");
                  final MutableRepositoryItem shippingAddressRI = profileRepository.createItem("contactInfo");
                  
                  profile.setPropertyValue("shippingAddress", shippingAddressRI);
                  profileRepository.addItem(shippingAddressRI);
                  profileRepository.updateItem(shippingAddressRI);
                }
                
                final String nickname = shippingGroup.getDescription();
                final String shippingGroupId = shippingGroup.getId();
                
                if (shippingGroupId.equals(nickname)) {
                  ShippingUtil.getInstance().copyProfileAddressToShippingGroupAddress("My Shipping Address", shippingGroup, (NMProfile) profile, order, orderMgr, cart);
                }
                
                commerceItemManager.addItemQuantityToShippingGroup(order, commerceItem.getId(), shippingGroup.getId(), quantity.longValue());
                
                orderMgr.updateOrder(order);
                
                // remove commerce item from SFL list
                i.remove();
                
                ResultBeanHelper.addSaveForLaterItemToCart(result, commerceItem);
              }
              
            } catch (final CommerceException ce) {
              ce.printStackTrace();
            }
            break;
          }
        }
      }
    }
    return result;
  }
  
  public ResultBean moveItemToSFLCart(final String commerceItemId, final ShoppingCartHandler cart, final NMProfileFormHandler profileFormHandler) throws Exception {
    
    NMOrderImpl order = cart.getNMOrder();
    final OrderManager orderMgr = cart.getOrderManager();
    final NMOrderHolder orderHolder = (NMOrderHolder) cart.getOrderHolder();
    final NMProfile nmProfile = CheckoutComponents.getProfile(ServletUtil.getCurrentRequest());
    
    final ResultBean result = new ResultBean();
    
    if (commerceItemId != null) {
      
      // add cloned commerce item to profile save for later items
      final MutableRepositoryItem profile = profileFormHandler.getProfile();
      
      @SuppressWarnings("unchecked")
      Set<RepositoryItem> saveForLaterItems = (Set<RepositoryItem>) profile.getPropertyValue("saveForLaterCartItems");
      
      if (saveForLaterItems == null) {
        saveForLaterItems = new HashSet<RepositoryItem>();
      }
      
      NMCommerceItem commerceItem = null;
      
      try {
        commerceItem = (NMCommerceItem) order.getCommerceItem(commerceItemId);
      } catch (final Exception e) {
        System.out.println("ExceptionExceptionExceptionExceptionExceptionExceptionExceptionExceptionExceptionExceptionExceptionExceptionExceptionException");
        // nmjah - do we need to tell the user that the commerce item was not found?
        // nmjah - this issue comes up with co-browsing...if this happens, we end up
        // nmjah - refreshing the page and the item 'appears' as though it transfered
      }
      
      if (commerceItem != null) {
        ResultBeanHelper.addSaveForLaterItem(result, commerceItem);
        final MutableRepository profileRepository = CheckoutComponents.getCommerceProfileTools().getProfileRepository();
        // create SFL item with commerceItem properties
        final MutableRepositoryItem sflItem = profileRepository.createItem("saveForLaterItem");
        sflItem.setPropertyValue("catalogRefId", commerceItem.getCatalogRefId());
        sflItem.setPropertyValue("productId", commerceItem.getAuxiliaryData().getProductId());
        sflItem.setPropertyValue("quantity", new Integer(new Long(commerceItem.getQuantity()).intValue()));
        sflItem.setPropertyValue("dateAdded", new Date());
        sflItem.setPropertyValue("specialInstCodes", commerceItem.getSpecialInstCodes());
        sflItem.setPropertyValue("selectedInterval", commerceItem.getSelectedInterval());
        sflItem.setPropertyValue("dynamicImageUrl", commerceItem.getDynamicImageUrl());
        // added for NMOBLD-2167
        sflItem.setPropertyValue(OrderRepositoryConstants.CONFIGURATION_KEY, commerceItem.getConfigurationKey());
        // added for NMOBLD-2184
        sflItem.setPropertyValue(OPTION_CHOICES, commerceItem.getOptionChoices());
        sflItem.setPropertyValue(OrderRepositoryConstants.CONFIG_SET_PRICE, commerceItem.getConfigSetPrice());
        profileRepository.addItem(sflItem);
        
        // remove existing commerce item from main shopping cart
        OrderUtil.getInstance().removeItem(orderHolder, orderMgr, commerceItem.getId(), cart);
        OrderUtil.getInstance().removeEmptyShippingGroups(order, orderMgr);
        saveForLaterItems.add(sflItem);
        order = cart.getNMOrder();
        
        // reprice the order to get messages back, if any
        final List<Message> promoMessages = PricingUtil.getInstance().repriceOrder(cart, nmProfile, SnapshotUtil.EDIT_ITEM_FROM_REGULAR_CART);
        result.setMessages(promoMessages);
        
        orderMgr.updateOrder(order);
      }
    }
    return result;
  }
  
  public ResultBean removeItemFromSFLCart(final String itemId, final NMProfileFormHandler profileFormHandler) throws Exception {
    final ResultBean result = new ResultBean();
    final RepositoryItem profile = profileFormHandler.getProfile();
    @SuppressWarnings("unchecked")
    final Set<RepositoryItem> saveForLaterItems = (Set<RepositoryItem>) profile.getPropertyValue("saveForLaterCartItems");
    
    if (saveForLaterItems != null) {
      final Iterator<RepositoryItem> iterator = saveForLaterItems.iterator();
      
      while (iterator.hasNext()) {
        final RepositoryItem saveForLaterItem = iterator.next();
        if (saveForLaterItem.getRepositoryId().equals(itemId)) {
          // add information about item to result
          ResultBeanHelper.removeItemFromSFL(result, saveForLaterItem);
          // remove commerce item
          iterator.remove();
          break;
        }
      }
    }
    
    return result;
  }
  
  /**
   * 
   * @param orderMgr
   * @param saveForLaterItem
   * @return NMCommerceItem
   * @throws Exception
   */
  public NMCommerceItem createCommerceItemFromSflItem(final OrderManager orderMgr, final RepositoryItem saveForLaterItem) throws Exception {
    NMCommerceItem commerceItem = null;
    
    final NMCommerceItemManager commerceItemManager = (NMCommerceItemManager) orderMgr.getCommerceItemManager();
    final CatalogTools catalogTools = orderMgr.getCatalogTools();
    
    final String catalogRefId = (String) saveForLaterItem.getPropertyValue("catalogRefId");
    final String productId = (String) saveForLaterItem.getPropertyValue("productId");
    final Integer quantity = (Integer) saveForLaterItem.getPropertyValue("quantity");
    Date dateAdded = (Date) saveForLaterItem.getPropertyValue("dateAdded");
    @SuppressWarnings("unchecked")
    final Map<String, String> specialInstCodes = (Map<String, String>) saveForLaterItem.getPropertyValue("specialInstCodes");
    final String dynamicImageUrl = (String) saveForLaterItem.getPropertyValue("dynamicImageUrl");
    final String selectedInterval = (String) saveForLaterItem.getPropertyValue("selectedInterval");
    // added for NMOBLDS-2167
    String configurationKey = (String) saveForLaterItem.getPropertyValue(OrderRepositoryConstants.CONFIGURATION_KEY);
    // add for NMOBLDS-2184
    Map<String, String> optionChoicesMap = (Map<String, String>) saveForLaterItem.getPropertyValue(OPTION_CHOICES);
    double configSetPrice = (Double) saveForLaterItem.getPropertyValue(OrderRepositoryConstants.CONFIG_SET_PRICE);
    final RepositoryItem skuRI = catalogTools.findSKU(catalogRefId);
    final RepositoryItem productRI = catalogTools.findProduct(productId);
    RepositoryItem suiteRI = null;
    
    commerceItem = (NMCommerceItem) commerceItemManager.createCommerceItem(catalogRefId, productId, quantity.longValue());
    
    // if the item is no longer sellable then change it's date so it sorts to the top.
    if (!commerceItem.getProductDisplayFlag()) {
      dateAdded = new Date(System.currentTimeMillis());
      commerceItem.setCommerceItemDate(dateAdded);
    } else {
      commerceItem.setCommerceItemDate(dateAdded);
    }
    
    commerceItem.setCmosCatalogId((String) productRI.getPropertyValue("cmosCatalogId"));
    commerceItem.setCmosSKUId((String) skuRI.getPropertyValue("cmosSKU"));
    commerceItem.setCmosItemCode((String) productRI.getPropertyValue("cmosItemCode"));
    commerceItem.setCmosProdName((String) productRI.getPropertyValue("displayName"));
    commerceItem.setWebDesignerName((String) productRI.getPropertyValue("cmDesignerName"));
    commerceItem.setSelectedInterval(selectedInterval);
    
    commerceItem.setDynamicImageUrl(dynamicImageUrl);
    commerceItem.setSpecialInstCodes(specialInstCodes);
    // added for NMOBLDS-2167
    commerceItem.setConfigurationKey(configurationKey);
    // added for NMOBLDS-2184
    commerceItem.setOptionChoices(optionChoicesMap);
    commerceItem.setConfigSetPrice(configSetPrice);
    @SuppressWarnings("unchecked")
    final Map<String, RepositoryItem> skuProdInfo = (Map<String, RepositoryItem>) skuRI.getPropertyValue("skuProdInfo");
    String prodVariation = (String) (skuProdInfo.get(productId).getPropertyValue("cmVariation1"));
    commerceItem.setProdVariation1(prodVariation);
    prodVariation = (String) (skuProdInfo.get(productId).getPropertyValue("cmVariation2"));
    commerceItem.setProdVariation2(prodVariation);
    prodVariation = (String) (skuProdInfo.get(productId).getPropertyValue("cmVariation3"));
    commerceItem.setProdVariation3(prodVariation);
    
    String StatusStr = CommonComponentHelper.getProdSkuUtil().getStatusString(productRI, skuRI);
    // PDU
    if (StatusStr.contains("fromStore") && StatusStr.contains("In Stock")) {
      StatusStr = "In Stock";
    }
    commerceItem.setTransientStatus(StatusStr);
    
    final Object[] params = new Object[1];
    params[0] = productId;
    
    final RepositoryView view = CommonComponentHelper.getProductRepository().getView("suite");
    final RqlStatement statement = RqlStatement.parseRqlStatement("subproducts includes item (id = ?0)");
    final RepositoryItem[] items = statement.executeQuery(view, params);
    
    String suiteId = null;
    if ((items != null) && (items.length > 0) && (items[0] != null)) {
      suiteId = items[0].getRepositoryId();
      suiteRI = catalogTools.findProduct(suiteId);
    }
    
    commerceItem.setSuiteId(suiteId);
    
    final boolean perishable = ((Boolean) skuRI.getPropertyValue("flgPerishable")).booleanValue();
    commerceItem.setPerishable(perishable);
    
    // determine if this is a catalog quick order based on whether the product or it's suite has
    // an mg image.
    NMProduct nmProduct = new NMProduct(productRI);
    
    if (nmProduct.hasImage("mg")) {
      commerceItem.setQuickOrder(false);
    } else if (suiteRI != null) {
      nmProduct = new NMSuite(suiteRI);
      
      if (nmProduct.hasImage("mg")) {
        commerceItem.setQuickOrder(false);
      } else {
        commerceItem.setQuickOrder(true);
      }
    } else {
      commerceItem.setQuickOrder(true);
    }
    
    commerceItem.setCodeSetType((String) productRI.getPropertyValue("codeSetType"));
    
    commerceItem.setTransientAvailableDate(CommonComponentHelper.getProdSkuUtil().getAvailableDateString(productRI, skuRI, null));
    
    final ItemPriceInfo itemPriceInfo = commerceItem.getPriceInfo();
    
    if (itemPriceInfo != null) {
      itemPriceInfo.setAmount(((Double) productRI.getPropertyValue("retailPrice")).doubleValue() * quantity.longValue());
      itemPriceInfo.setRawTotalPrice(((Double) productRI.getPropertyValue("retailPrice")).doubleValue() * quantity.longValue());
    }
    
    return commerceItem;
  }
  
  /**
   * sorts list of Save For Later items by their date added (set as commerceItemDate) sorts most recently added to the top of the list
   */
  private class SaveForLaterComparator implements Comparator<NMCommerceItem> {
    
    @Override
    public int compare(final NMCommerceItem o1, final NMCommerceItem o2) {
      
      if ((o1 != null) && (o2 != null)) {
        final Date date1 = o1.getCommerceItemDate();
        final Date date2 = o2.getCommerceItemDate();
        
        if ((date1 != null) && (date2 != null)) {
          if (date1.before(date2)) {
            return 1;
          }
          if (date1.after(date2)) {
            return -1;
          }
        }
      }
      
      return 0;
    }
  }
  
  protected SystemSpecs getSystemSpecs(final DynamoHttpServletRequest request) {
    final SystemSpecs systemSpecs = (SystemSpecs) request.resolveName("/nm/utils/SystemSpecs");
    return systemSpecs;
  }
  
  /**
   * Returns commerce item from the save for later items
   * 
   * @param itemId
   * @param profile
   * @param orderMgr
   * @return
   */
  public NMCommerceItem getCommerCeItem(final String itemId, final NMProfile profile, final NMOrderManager orderMgr) {
    final Set<RepositoryItem> saveForLaterCartItems = (Set<RepositoryItem>) profile.getPropertyValue("saveForLaterCartItems");
    final Iterator<RepositoryItem> iterator = saveForLaterCartItems.iterator();
    NMCommerceItem commerceItem = null;
    while (iterator.hasNext()) {
      final RepositoryItem saveForLaterItem = iterator.next();
      final String repositoryId = saveForLaterItem.getRepositoryId();
      if (!StringUtils.isEmpty(itemId) && itemId.equalsIgnoreCase(repositoryId)) {
        try {
          commerceItem = createCommerceItemFromSflItem(orderMgr, saveForLaterItem);
        } catch (final Exception exception) {
          final GenericService config = CheckoutComponents.getConfig();
          config.logError(exception);
        }
        if (commerceItem != null) {
          commerceItem.setId(saveForLaterItem.getRepositoryId()); // set the id on the commerceItem to match the sflItem
          
        }
        break;
      }
      
    }
    return commerceItem;
  }
}
