package com.nm.commerce;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.servlet.ServletException;

import atg.commerce.CommerceException;
import atg.commerce.gifts.GiftlistManager;
import atg.commerce.order.HardgoodShippingGroup;
import atg.commerce.order.OrderHolder;
import atg.commerce.order.OrderManager;
import atg.commerce.order.RepositoryContactInfo;
import atg.commerce.profile.CommerceProfileTools;
import atg.commerce.states.OrderStates;
import atg.core.util.StringUtils;
import atg.dtm.TransactionDemarcation;
import atg.dtm.TransactionDemarcationException;
import atg.nucleus.Nucleus;
import atg.repository.MutableRepository;
import atg.repository.MutableRepositoryItem;
import atg.repository.Repository;
import atg.repository.RepositoryException;
import atg.repository.RepositoryItem;
import atg.repository.RepositoryView;
import atg.repository.rql.RqlStatement;
import atg.servlet.ServletUtil;
import atg.userprofiling.Profile;

import com.nm.cmos.CmosReplenishmentException;
import com.nm.cmos.ReplenishmentData;
import com.nm.cmos.data.Replenishment;
import com.nm.cmos.data.ReplenishmentContract;
import com.nm.commerce.beans.RecentlyChangedCommerceItem;
import com.nm.commerce.checkout.CheckoutComponents;
import com.nm.commerce.checkout.beans.OutOfStockItemDetails;
import com.nm.commerce.giftlist.GiftlistConstants;
import com.nm.commerce.pagedef.model.bean.RichRelevanceProductBean;
import com.nm.commerce.promotion.NMOrderImpl;
import com.nm.components.CommonComponentHelper;
import com.nm.edo.constants.EmployeeDiscountConstants;
import com.nm.profile.ProfileProperties;
import com.nm.repository.ProfileRepository;
import com.nm.twoclickcheckout.TwoClickCheckoutConstants;
import com.nm.utils.EDWCustData;
import com.nm.utils.GenericLogger;
import com.nm.utils.LinkedEmailUtils;
import com.nm.utils.LocalizationUtils;
import com.nm.utils.NmoUtils;
import com.nm.utils.ProdSkuUtil;
import com.nm.utils.ReplenishmentUtils;
import com.nm.utils.StringUtilities;
import com.nm.utils.SystemSpecs;
import com.nm.utils.datasource.NMTransactionDemarcation;
import com.nm.utils.fiftyone.FiftyOneUtils;

public class NMProfile extends Profile {
  public static final int UNREGISTERED = 0;
  public static final int REGISTERED = 1;
  private List<RepositoryItem> allShippingAddresses;
  private List<RepositoryItem> profileShippingAddresses;
  private List<RepositoryItem> allGiftRegistries;
  private List<RepositoryItem> activeGiftRegistries;
  private final GenericLogger log = CommonComponentHelper.getLogger();;
  private boolean mVerbose = false;
  private int mPasswordRequestAttempts = 0;
  private String mUserIPAddress;
  private GiftlistManager giftlistManager;
  private boolean sentABMarkersToOmniture;
  private String preferredLocation;
  private int preferredLocationSearchRadius;
  private String preferredSddLocation;
  public static final String TYPE = "product";
  private EDWCustData edwCustData;
  private String userClcCountryName;
  private boolean countryVerified;
  private String clcSelected = "";
  private List<OutOfStockItemDetails> oosItems = new ArrayList<OutOfStockItemDetails>();
  private Map<String, RichRelevanceProductBean> oosCartRecommendations = new HashMap<String, RichRelevanceProductBean>();
  /** The ab test group assignments. */
  private Map<String, String> abTestGroupAssignments;
   
  // personalized categories for the user.  
  private String persAllHiddenCatIds;  
  /* property to hold the recently changed commerce items */
  private List<RecentlyChangedCommerceItem> recentlyCancelledItems;
  
  /** The ab test attribute map. */
  private Map<String, String> abTestAttributeMap;
  
  /** The account recently registered. */
  private boolean accountRecentlyRegistered;
  
  public String getUserClcCountryName() {
    return userClcCountryName;
  }
  
  public void setUserClcCountryName(final String userClcCountryName) {
    this.userClcCountryName = userClcCountryName;
  }
  
  public boolean isCountryVerified() {
    return countryVerified;
  }
  
  public void setCountryVerified(final boolean countryVerified) {
    this.countryVerified = countryVerified;
  }
  
  public String getClcSelected() {
    return clcSelected;
  }
  
  public void setClcSelected(final String clcSelected) {
    this.clcSelected = clcSelected;
  }
  
  public boolean getUserValidatedCountry() {
    final Object obj = getPropertyValue("isUserValidatedCountry");
    if ((obj == null) || obj.toString().isEmpty()) {
      return false;
    } else {
      return ((Boolean) getPropertyValue("isUserValidatedCountry")).booleanValue();
    }
  }
  
  public void setUserValidatedCountry(final boolean userValidatedCountry) {
    setPropertyValue("isUserValidatedCountry", new Boolean(userValidatedCountry));
  }
  
  public String getMyHomePage() {
    return getStringValue(ProfileRepository.MY_HOME_PAGE);
  }
  
  public void setMyHomePage(final String myHomePage) {
    setPropertyValue(ProfileRepository.MY_HOME_PAGE, myHomePage);
  }
  
  /* holds a list of categories a customer has provided an email address to access */
  private List<String> emailCategoryAccessList;
  
  /**
   * Returns the list of categories a customer has provided an email address to access
   * 
   * @return list of category ids
   */
  public List<String> getEmailCategoryAccessList() {
    return emailCategoryAccessList;
  }
  
  /**
   * Searches the list of categories a customer has provided an email address to access
   * 
   * @param categoryId
   *          - the categoryId to search on
   * @return true if the customer has already provided an email during that session
   */
  public boolean hasEmailCategoryAccess(final String categoryId) {
    boolean hasAccess = true;
    if (emailCategoryAccessList != null) {
      hasAccess = emailCategoryAccessList.contains(categoryId);
    } else {
      hasAccess = false;
    }
    return hasAccess;
  }
  
  /** The loggedInPreviousPageFlag flag. */
  private boolean loggedInPreviousPageFlag;
  
  /**
   * @return the loggedInPreviousPageFlag
   */
  public boolean isLoggedInPreviousPageFlag() {
    return loggedInPreviousPageFlag;
  }
  
  /**
   * @param loggedInPreviousPageFlag
   *          the loggedInPreviousPageFlag to set
   */
  public void setLoggedInPreviousPageFlag(final boolean loggedInPreviousPageFlag) {
    this.loggedInPreviousPageFlag = loggedInPreviousPageFlag;
  }
  
  /**
   * Adds a category id to the list of categories a customer has provided an email address to access
   * 
   * @param categoryId
   *          - the category id to add
   */
  public void setEmailCategoryAccess(final String categoryId) {
    if (emailCategoryAccessList == null) {
      emailCategoryAccessList = new ArrayList<String>();
    }
    emailCategoryAccessList.add(categoryId);
  }
  
  public NMProfile() {
    super();
    setSentABMarkersToOmniture(false);
  }
  
  public boolean getSentABMarkersToOmniture() {
    return sentABMarkersToOmniture;
  }
  
  public void setSentABMarkersToOmniture(final boolean sentABMarkersToOmniture) {
    this.sentABMarkersToOmniture = sentABMarkersToOmniture;
  }
  
  public String getPersonalizationId() {
    return getRepositoryId();
  }
  
  public String getId() {
    return getRepositoryId();
  }
  
  public Map<String, MutableRepositoryItem> getCreditCards() {
    @SuppressWarnings("unchecked")
    final Map<String, MutableRepositoryItem> returnValue = (Map<String, MutableRepositoryItem>) getPropertyValue("creditCards");
    return returnValue;
  }
  
  public MutableRepositoryItem getBillingAddress() {
    return (MutableRepositoryItem) getPropertyValue("billingAddress");
  }
  
  public NMAddress getBillingNMAddress() {
    final MutableRepositoryItem billingAddress = getBillingAddress();
    return billingAddress != null ? new NMAddress(billingAddress) : null;
  }
  
  public void setBillingAddress(final RepositoryItem value) {
    setPropertyValue("billingAddress", value);
  }
  
  public MutableRepositoryItem getShippingAddress() {
    return (MutableRepositoryItem) getPropertyValue("shippingAddress");
  }
  
  public NMAddress getShippingNMAddress() {
    final MutableRepositoryItem shippingAddress = getShippingAddress();
    return shippingAddress != null ? new NMAddress(shippingAddress) : null;
  }
  
  public void setShippingAddress(final RepositoryItem value) {
    setPropertyValue("shippingAddress", value);
  }
  
  public void setShippingAddress(final RepositoryContactInfo shippingAddress) {
    setShippingAddress(shippingAddress.getRepositoryItem());
  }
  
  public Map<String, MutableRepositoryItem> getSecondaryAddresses() {
    @SuppressWarnings("unchecked")
    final Map<String, MutableRepositoryItem> returnValue = (Map<String, MutableRepositoryItem>) getPropertyValue("secondaryAddresses");
    return returnValue;
  }
  
  public Map<String, NMAddress> getSecondaryNMAddresses() {
    final Map<String, NMAddress> secondaryNMAddresses = new LinkedHashMap<String, NMAddress>();
    final Map<String, MutableRepositoryItem> secondaryAddresses = getSecondaryAddresses();
    for (final String nickname : secondaryAddresses.keySet()) {
      final MutableRepositoryItem address = secondaryAddresses.get(nickname);
      secondaryNMAddresses.put(nickname, address != null ? new NMAddress(address) : null);
    }
    return secondaryNMAddresses;
  }
  
  /**
   * This method is used to display replenishment products.
   * 
   * @return replenishmentProducts
   */
  public List<String> getReplenishmentProducts() {
    final List<String> replenishmentProductName = new ArrayList<String>();
    final ReplenishmentUtils replenishmentUtils = CheckoutComponents.getReplenishmentUtils(ServletUtil.getCurrentRequest());
    final Repository productCatalogRepository = replenishmentUtils.getProductRepository();
    final ReplenishmentData[] replData = replenishmentUtils.getReplenishmentSummary();
    Replenishment replenishment;
    RepositoryItem productCatalogItem;
    if (null != replData) {
      for (final ReplenishmentData replenishmentData : replData) {
        if (!StringUtils.isBlank(replenishmentData.getContractId())) {
          try {
            replenishment = replenishmentUtils.getReplenishment(replenishmentData.getContractId());
            for (final ReplenishmentContract replenishmentContract : replenishment.replenishment_contract) {
              if (!StringUtils.isBlank(replenishmentContract.web_product_id)) {
                productCatalogItem = productCatalogRepository.getItem(replenishmentContract.web_product_id, TYPE);
                if ((null != productCatalogItem) && !StringUtils.isBlank(productCatalogItem.getItemDisplayName())) {
                  replenishmentProductName.add(productCatalogItem.getItemDisplayName());
                }
              }
            }
          } catch (final CmosReplenishmentException e) {
            log.error("error in getting product catalog repository", e);
            e.printStackTrace();
          } catch (final RepositoryException e) {
            log.error("error while gettitg product repository item", e);
          }
          
        }
      }
    }
    
    return replenishmentProductName;
  }
  
  public void setVerbose(final boolean pVerbose) {
    mVerbose = pVerbose;
  }
  
  public boolean isVerbose() {
    return mVerbose;
  }
  
  public void setPasswordRequestAttempts(final int pPasswordRequestAttempts) {
    mPasswordRequestAttempts = pPasswordRequestAttempts;
  }
  
  public int getPasswordRequestAttempts() {
    return mPasswordRequestAttempts;
  }
  
  public void setUserIPAddress(final String pUserIPAddress) {
    mUserIPAddress = pUserIPAddress;
  }
  
  public String getUserIPAddress() {
    return mUserIPAddress;
  }
  
  public void setFlgRegisteredBeanProp(final boolean flgRegistered) {
    setPropertyValue(ProfileRepository.FLG_REGISTERED_PROP, new Boolean(flgRegistered));
  }
  
  public boolean getFlgRegisteredBeanProp() {
    return ((Boolean) getPropertyValue(ProfileRepository.FLG_REGISTERED_PROP)).booleanValue();
  }
  
  public boolean getFlgManagingRegistry() {
    return ((Boolean) getPropertyValue(ProfileRepository.FLG_MANAGING_REGISTRY_PROP)).booleanValue();
  }
  
  public void setFlgManagingRegistry(final boolean flgManagingRegistry) {
    setPropertyValue(ProfileRepository.FLG_MANAGING_REGISTRY_PROP, new Boolean(flgManagingRegistry));
  }
  
  public boolean getUserHasActiveWishList() {
    return ((Boolean) getPropertyValue(ProfileRepository.USER_HAS_WISHLIST_PROP)).booleanValue();
  }
  
  public void setUserHasActiveWishList(final boolean flgUserHasActiveWishList) {
    setPropertyValue(ProfileRepository.USER_HAS_WISHLIST_PROP, new Boolean(flgUserHasActiveWishList));
  }
  
  public void setFlgProfileMWS(final boolean flgProfileMWS) {
    setPropertyValue(ProfileRepository.FLG_PROFILE_MWS, new Boolean(flgProfileMWS));
  }
  
  public boolean getFlgProfileMWS() {
    if (getPropertyValue(ProfileRepository.FLG_PROFILE_MWS) == null) {
      return false;
    }
    return ((Boolean) getPropertyValue(ProfileRepository.FLG_PROFILE_MWS)).booleanValue();
  }
  
  private boolean publishedStatus;
  
  public boolean getPublishedStatus() {
    return publishedStatus;
  }
  
  public void setPublishedStatus(final boolean publishedStatus) {
    this.publishedStatus = publishedStatus;
  }
  
  public Repository getGiftlistRepository() {
    return (Repository) Nucleus.getGlobalNucleus().resolveName("/atg/commerce/gifts/Giftlists");
  }
  
  public GiftlistManager getGiftlistManager() {
    giftlistManager = (GiftlistManager) Nucleus.getGlobalNucleus().resolveName("/atg/commerce/gifts/GiftlistManager");
    return giftlistManager;
  }
  
  // My initial naming of this function was getActiveGiftRegistry, dynamo
  // wouldn't work with that name for some reason. Renaming the function fixed
  // the problem.
  public RepositoryItem getTransActiveGiftRegistry() {
    if (getPropertyValue(ProfileRepository.ACTIVE_GIFT_REGISTRY_PROP) == null) {
      final List<RepositoryItem> giftlists = getGiftlistsByType(GiftlistConstants.ACTIVE_GIFT_REGISTRY);
      if (giftlists.size() > 0) {
        setTransActiveGiftRegistry(giftlists.get(0));
        return giftlists.get(0);
      } else {
        return null;
      }
    } else {
      return (MutableRepositoryItem) getPropertyValue(ProfileRepository.ACTIVE_GIFT_REGISTRY_PROP);
    }
  }
  
  public void setTransActiveGiftRegistry(final RepositoryItem giftlistRI) {
    setPropertyValue(ProfileRepository.ACTIVE_GIFT_REGISTRY_PROP, giftlistRI);
  }
  
  public RepositoryItem getTransActiveWishlist() {
    if (getPropertyValue("activeWishlist") == null) {
      final List<RepositoryItem> giftlists = getGiftlistsByType(GiftlistConstants.ACTIVE_WISH_LIST);
      if (giftlists.size() > 0) {
        setTransActiveWishlist(giftlists.get(0));
        return giftlists.get(0);
      } else {
        return null;
      }
    } else {
      return (MutableRepositoryItem) getPropertyValue("activeWishlist");
    }
  }
  
  public void setTransActiveWishlist(final RepositoryItem giftlistRI) {
    setPropertyValue("activeWishlist", giftlistRI);
  }
  
  public RepositoryItem getAddressByNickname(final String nickname) {
    RepositoryItem item = null;
    if (nickname != null) {
      if (nickname.equals("My Shipping Address")) {
        item = (RepositoryItem) getDataSource().getPropertyValue("shippingAddress");
      } else {
        final Map<String, MutableRepositoryItem> otherAddrMap = getSecondaryAddresses();
        if ((otherAddrMap != null) && !otherAddrMap.isEmpty()) {
          item = otherAddrMap.get(nickname);
        }
      }
    }
    return item;
  }
  
  public void setAllShippingAddresses(final List<RepositoryItem> allShippingAddresses) {
    this.allShippingAddresses = allShippingAddresses;
  }
  
  public List<RepositoryItem> getAllShippingAddresses() {
    return buildAllShippingAddresses();
  }
  
  public List<NMRepositoryContactInfo> getAllShippingNMAddresses() {
    final List<NMRepositoryContactInfo> shippingAddresses = new ArrayList<NMRepositoryContactInfo>();
    for (final RepositoryItem address : getAllShippingAddresses()) {
      final NMRepositoryContactInfo nmAddress = new NMRepositoryContactInfo();
      nmAddress.setRepositoryItem((MutableRepositoryItem) address);
      shippingAddresses.add(address != null ? nmAddress : null);
    }
    return shippingAddresses;
  }
  
  private List<RepositoryItem> buildAllShippingAddresses() {
    allShippingAddresses = new ArrayList<RepositoryItem>(getProfileShippingAddresses());
    
    try {
      final Set<String> vrKeySet = getViewedRegistriesList().keySet();
      for (final String key : vrKeySet) {
        final atg.repository.RepositoryItem giftlist = getGiftlistManager().getGiftlistTools().getGiftlist(key);
        if (giftlist != null) {
          final atg.repository.RepositoryItem theCI = (atg.repository.RepositoryItem) giftlist.getPropertyValue("shippingAddress");
          allShippingAddresses.add(theCI);
        }
      }
    } catch (final Exception e) {
      e.printStackTrace();
    }
    
    return allShippingAddresses;
  }
  
  public List<RepositoryItem> getProfileShippingAddresses() {
    profileShippingAddresses = new ArrayList<RepositoryItem>();
    final RepositoryItem shippingAddressRI = (RepositoryItem) getPropertyValue("shippingAddress");
    String address1 = "";
    
    if (shippingAddressRI != null) {
      address1 = (String) shippingAddressRI.getPropertyValue("address1");
    }
    
    if ((address1 != null) && !address1.equals("")) {
      profileShippingAddresses.add((RepositoryItem) getPropertyValue("shippingAddress"));
    }
    
    if (getSecondaryAddresses() != null) {
      final Map<String, MutableRepositoryItem> secondaryAddresses = getSecondaryAddresses();
      profileShippingAddresses.addAll(secondaryAddresses.values());
    }
    return profileShippingAddresses;
  }
  
  public void setProfileShippingAddresses(final List<RepositoryItem> profileShippingAddresses) {
    this.profileShippingAddresses = profileShippingAddresses;
  }
  
  public List<RepositoryItem> getGiftlistsByType(final String type) {
    final List<RepositoryItem> returnValue = new ArrayList<RepositoryItem>();
    try {
      @SuppressWarnings("unchecked")
      final List<RepositoryItem> giftlists = (List<RepositoryItem>) getGiftlistManager().getGiftlistTools().getGiftlists(getDataSource());
      if ((giftlists != null) && (giftlists.size() > 0)) {
        final Iterator<RepositoryItem> giftlistIterator = giftlists.iterator();
        while (giftlistIterator.hasNext()) {
          final MutableRepositoryItem giftlistRI = (MutableRepositoryItem) giftlistIterator.next();
          final RepositoryItem giftlistTypeRI = (RepositoryItem) giftlistRI.getPropertyValue(ProfileRepository.GIFT_LIST_TYPE_PROP);
          if ((giftlistTypeRI != null) && ((String) giftlistTypeRI.getPropertyValue(ProfileRepository.GIFT_LIST_TYPE_PROP)).trim().equalsIgnoreCase(type)) {
            returnValue.add(giftlistRI);
          }
        }
      }
    } catch (final Exception e) {
      e.printStackTrace();
    }
    return returnValue;
  }
  
  public void setLastViewedGiftlistId(final String lastViewedGiftlist) {
    if ((lastViewedGiftlist != null) && !lastViewedGiftlist.equals("")) {
      setPropertyValue(ProfileRepository.LAST_VIEWED_GIFT_LIST_PROP, lastViewedGiftlist);
      getViewedRegistriesList().put(lastViewedGiftlist, new String(""));
    }
  }
  
  public String getLastViewedGiftlistId() {
    
    return (String) getPropertyValue(ProfileRepository.LAST_VIEWED_GIFT_LIST_PROP);
  }
  
  public Map<String, String> getViewedRegistriesList() {
    @SuppressWarnings("unchecked")
    final Map<String, String> map = (Map<String, String>) getPropertyValue(ProfileRepository.VIEWED_REGISTRIES_PROP);
    return map;
  }
  
  public Set<String> getViewedRegistryIds() {
    final Set<String> viewedRegistryIds = new HashSet<String>();
    final Map<String, String> viewedRegistries = getViewedRegistriesList();
    if (viewedRegistries != null) {
      viewedRegistryIds.addAll(viewedRegistries.keySet());
    }
    return viewedRegistryIds;
  }
  
  /**
   * Gives a default 0 value when the internal object is null
   * 
   * @param key
   *          the field name
   * @return the primitive int value in the Integer object at the field name
   */
  protected int getIntValue(final String key) {
    final Integer valueObj = (Integer) getPropertyValue(key);
    int value = 0;
    if (valueObj != null) {
      value = valueObj.intValue();
    }
    return value;
  }
  
  /**
   * Gives a default 0.0 value when the internal object is null
   * 
   * @param key
   *          the field name
   * @return the primitive double value in the Double object at the field name
   */
  protected double getDoubleValue(final String key) {
    final Double valueObj = (Double) getPropertyValue(key);
    double value = 0.0;
    if (valueObj != null) {
      value = valueObj.doubleValue();
    }
    return value;
  }
  
  /**
   * Gives a default empty-string value when value is null
   * 
   * @param key
   *          the field name
   * @return an always valid (non-null) value
   */
  protected String getStringValue(final String key) {
    String value = (String) getPropertyValue(key);
    if (value == null) {
      value = "";
    }
    return value;
  }
  
  public String getLogin() {
    return (String) getPropertyValue("login");
  }
  
  public int getTotalVisitsBeanProp() {
    return getIntValue(ProfileRepository.TOTAL_VISITS_PROP);
  }
  
  public int incTotalVisitsBeanProp() {
    return incBeanProp(ProfileRepository.TOTAL_VISITS_PROP);
  }
  
  public void setNumberOfOrdersBeanProp(final int value) {
    setPropertyValue(ProfileRepository.NUMBER_OF_ORDERS_PROP, new Integer(value));
  }
  
  public int incNumberOfOrdersBeanProp() {
    return incBeanProp(ProfileRepository.NUMBER_OF_ORDERS_PROP);
  }
  
  public int getNumberOfOrdersBeanProp() {
    return getIntValue(ProfileRepository.NUMBER_OF_ORDERS_PROP);
  }
  
  public void setFirstPurchaseDateBeanProp(final Date value) {
    setPropertyValue(ProfileRepository.FIRST_PURCHASE_DATE_PROP, value);
  }
  
  public Date getFirstPurchaseDateBeanProp() {
    return (Date) getPropertyValue(ProfileRepository.FIRST_PURCHASE_DATE_PROP);
  }
  
  public void setLastPurchaseDateBeanProp(final Date value) {
    setPropertyValue(ProfileRepository.LAST_PURCHASE_DATE_PROP, value);
  }
  
  public Date getLastPurchaseDateBeanProp() {
    return (Date) getPropertyValue(ProfileRepository.LAST_PURCHASE_DATE_PROP);
  }
  
  public void setLastVisitDateBeanProp(final Date value) {
    setPropertyValue(ProfileRepository.LAST_VISIT_DATE_PROP, value);
  }
  
  public Date getLastVisitDateBeanProp() {
    return (Date) getPropertyValue(ProfileRepository.LAST_VISIT_DATE_PROP);
  }
  
  public void setLastActivityBeanProp(final Date value) {
    setPropertyValue(ProfileRepository.LAST_ACTIVITY_PROP, value);
  }
  
  public Date getLastActivityBeanProp() {
    return (Date) getPropertyValue(ProfileRepository.LAST_ACTIVITY_PROP);
  }
  
  public void setCurrentVisitDateBeanProp(final Date value) {
    setPropertyValue(ProfileRepository.CURRENT_VISIT_DATE_PROP, value);
  }
  
  public Date getCurrentVisitDateBeanProp() {
    return (Date) getPropertyValue(ProfileRepository.CURRENT_VISIT_DATE_PROP);
  }
  
  public void setLastOrderTotalBeanProp(final double value) {
    setPropertyValue(ProfileRepository.LAST_ORDER_TOTAL_PROP, new Double(value));
  }
  
  public double getLastOrderTotalBeanProp() {
    return getDoubleValue(ProfileRepository.LAST_ORDER_TOTAL_PROP);
  }
  
  public void resetLifetimeOrderTotalBeanProp() {
    setPropertyValue(ProfileRepository.LIFETIME_ORDER_TOTAL_PROP, new Double(0.0));
  }
  
  public void incLifetimeOrderTotalBeanProp(final double value) {
    incBeanProp(ProfileRepository.LIFETIME_ORDER_TOTAL_PROP, value);
  }
  
  public double getLifetimeOrderTotalBeanProp() {
    return getDoubleValue(ProfileRepository.LIFETIME_ORDER_TOTAL_PROP);
  }
  
  public double getAverageOrderTotalBeanProp() {
    return getDoubleValue(ProfileRepository.AVERAGE_ORDER_TOTAL_PROP);
  }
  
  public void incAverageMonthlyOrderTotalBeanProp(final double value) {
    incBeanProp(ProfileRepository.AVERAGE_MONTHLY_ORDER_TOTAL_PROP, value);
  }
  
  public double getAverageMonthlyOrderTotalBeanProp() {
    return getDoubleValue(ProfileRepository.AVERAGE_MONTHLY_ORDER_TOTAL_PROP);
  }
  
  public double getAverageYearlyOrderTotalBeanProp() {
    return getDoubleValue(ProfileRepository.AVERAGE_YEARLY_ORDER_TOTAL_PROP);
  }
  
  public void incTotalOrderTotalThisCalendarYearBeanProp(final double value) {
    incBeanProp(ProfileRepository.TOTAL_ORDER_TOTAL_THIS_CALENDAR_YEAR_PROP, value);
  }
  
  public void setTotalOrderTotalThisCalendarYearBeanProp(final double value) {
    setPropertyValue(ProfileRepository.TOTAL_ORDER_TOTAL_THIS_CALENDAR_YEAR_PROP, new Double(value));
  }
  
  public double getTotalOrderTotalThisCalendarYearBeanProp() {
    return getDoubleValue(ProfileRepository.TOTAL_ORDER_TOTAL_THIS_CALENDAR_YEAR_PROP);
  }
  
  public double getAverageOrderTotalThisCalendarYearBeanProp() {
    return getDoubleValue(ProfileRepository.AVERAGE_ORDER_TOTAL_THIS_CALENDAR_YEAR_PROP);
  }
  
  public void setLastPurchaseState(final NMOrderImpl order) {
    @SuppressWarnings("unchecked")
    final List<HardgoodShippingGroup> sgs = order.getShippingGroups();
    if ((sgs != null) && (sgs.size() > 0)) {
      final HardgoodShippingGroup sg = sgs.get(0);
      setLastPurchaseStateBeanProp(sg.getShippingAddress().getState());
    }
  }
  
  public void setLastPurchaseStateBeanProp(final String value) {
    setPropertyValue(ProfileRepository.LAST_PURCHASE_STATE_PROP, value);
  }
  
  public String getLastPurchaseStateBeanProp() {
    return getStringValue(ProfileRepository.LAST_PURCHASE_STATE_PROP);
  }
  
  public void setPurchaseDepictionCodesBeanProp(final String value) {
    setPropertyValue(ProfileRepository.PURCHASE_DEPICTION_CODES_PROP, value);
  }
  
  public String getPurchaseDepictionCodesBeanProp() {
    return getStringValue(ProfileRepository.PURCHASE_DEPICTION_CODES_PROP);
  }
  
  public void setEmailAddressBeanProp(final String value) {
    setPropertyValue(ProfileRepository.EMAIL_ADDRESS_PROP, value);
  }
  
  public String getEmailAddressBeanProp() {
    return (String) getPropertyValue(ProfileRepository.EMAIL_ADDRESS_PROP);
  }
  
  public void setReceiveEmailBeanProp(final boolean value) {
    if (value) {
      setPropertyValue(ProfileRepository.RECEIVE_EMAIL_PROP, "yes");
    } else {
      setPropertyValue(ProfileRepository.RECEIVE_EMAIL_PROP, "no");
    }
    
  }
  
  public boolean getReceiveEmailBeanProp() {
    final String receiveEmailString = (String) getPropertyValue(ProfileRepository.RECEIVE_EMAIL_PROP);
    if ((receiveEmailString != null) && receiveEmailString.equals("yes")) {
      return true;
    } else {
      return false;
    }
  }
  
  // SEGMENTATION
  public void setMWSPurchaseCount(final String value) {
    setPropertyValue(ProfileRepository.MWS_PURCHASE_COUNT, value);
  }
  
  public String getMWSPurchaseCount() {
    return getStringValue(ProfileRepository.MWS_PURCHASE_COUNT);
  }
  
  public int incMWSPurchaseCountBeanProp(final int itemQuantity) {
    return incBeanProp(ProfileRepository.MWS_PURCHASE_COUNT, itemQuantity);
  }
  
  public void setPromoCodeUseCount(final int value) {
    setPropertyValue(ProfileRepository.PROMOCODE_USE_COUNT, new Integer(value));
  }
  
  public int getPromoCodeUseCount() {
    return getIntValue(ProfileRepository.PROMOCODE_USE_COUNT);
  }
  
  public int incPromoCodeUseCountBeanProp() {
    return incBeanProp(ProfileRepository.PROMOCODE_USE_COUNT);
  }
  
  public void setDOLPromoCodeUseBeanProp(final Date value) {
    setPropertyValue(ProfileRepository.DATE_OF_LAST_PROMOCODE_USE, value);
  }
  
  public Date getDOLPromoCodeUseBeanProp() {
    return (Date) getPropertyValue(ProfileRepository.DATE_OF_LAST_PROMOCODE_USE);
  }
  
  public void setGiftOptionsUseCount(final String value) {
    setPropertyValue(ProfileRepository.GIFT_OPTIONS_USE_COUNT, value);
  }
  
  public String getGiftOptionsUseCount() {
    return getStringValue(ProfileRepository.GIFT_OPTIONS_USE_COUNT);
  }
  
  public int incGiftOptionsUseCountBeanProp() {
    return incBeanProp(ProfileRepository.GIFT_OPTIONS_USE_COUNT);
  }
  
  public void setDOLGiftOptionsUseBeanProp(final Date value) {
    setPropertyValue(ProfileRepository.DATE_OF_LAST_GIFT_OPTIONS_USE, value);
  }
  
  public Date getDOLGiftOptionsUseBeanProp() {
    return (Date) getPropertyValue(ProfileRepository.DATE_OF_LAST_GIFT_OPTIONS_USE);
  }
  
  public void setProfileCreateDateBeanProp(final Date value) {
    setPropertyValue(ProfileRepository.PROFILE_CREATE_DATE, value);
  }
  
  public Date getProfileCreateDateBeanProp() {
    return (Date) getPropertyValue(ProfileRepository.PROFILE_CREATE_DATE);
  }
  
  public void setChatClickBeanProp(final int value) {
    setPropertyValue(ProfileRepository.CHAT_CLICK, new Integer(value));
  }
  
  public int getChatClickBeanProp() {
    int type = 0;
    final Integer typeObj = (Integer) getPropertyValue(ProfileRepository.CHAT_CLICK);
    if (typeObj != null) {
      type = typeObj.intValue();
    }
    return type;
  }
  
  public void setVisitStorePageBeanProp(final int value) {
    setPropertyValue(ProfileRepository.VISIT_STORE_PAGE, new Integer(value));
  }
  
  public int getVisitStorePageBeanProp() {
    int type = 0;
    final Integer typeObj = (Integer) getPropertyValue(ProfileRepository.VISIT_STORE_PAGE);
    if (typeObj != null) {
      type = typeObj.intValue();
    }
    return type;
  }
  
  public void setVisitIncirclePageBeanProp(final int value) {
    setPropertyValue(ProfileRepository.VISIT_INCIRCLE_PAGE, new Integer(value));
  }
  
  public int getVisitIncirclePageBeanProp() {
    int type = 0;
    final Integer typeObj = (Integer) getPropertyValue(ProfileRepository.VISIT_INCIRCLE_PAGE);
    if (typeObj != null) {
      type = typeObj.intValue();
    }
    return type;
  }
  
  public void setVisitCreditAreaBeanProp(final int value) {
    setPropertyValue(ProfileRepository.VISIT_CREDIT_AREA_PAGE, new Integer(value));
  }
  
  public int getVisitCreditAreaBeanProp() {
    int type = 0;
    final Integer typeObj = (Integer) getPropertyValue(ProfileRepository.VISIT_CREDIT_AREA_PAGE);
    if (typeObj != null) {
      type = typeObj.intValue();
    }
    return type;
  }
  
  public void setVisitAssistancePageBeanProp(final int value) {
    setPropertyValue(ProfileRepository.VISIT_ASSISTANCE_PAGE, new Integer(value));
  }
  
  public int getVisitAssistancePageBeanProp() {
    int type = 0;
    final Integer typeObj = (Integer) getPropertyValue(ProfileRepository.VISIT_ASSISTANCE_PAGE);
    if (typeObj != null) {
      type = typeObj.intValue();
    }
    return type;
  }
  
  protected void computeMWSPurchaseCount(final NMOrderImpl order) {
    @SuppressWarnings("unchecked")
    final List<NMCommerceItem> items = order.getCommerceItems();
    for (final NMCommerceItem nmci : items) {
      try {
        final String cmCatCode = nmci.getCoreMetricsCategory();
        final String rProd = (String) nmci.getPropertyValue("relatedProduct");
        
        if (((cmCatCode != null) && cmCatCode.equalsIgnoreCase("up")) || ((rProd != null) && !rProd.equalsIgnoreCase(""))) {
          final Long quan = new Long(nmci.getQuantity());
          incMWSPurchaseCountBeanProp(quan.intValue());
        }
      } catch (final Exception e) {
        final String errString = "computeMWSPurchaseCount value error";
        log.error(errString, this);
      }
    }
  }
  
  /**
   * will increment by one if any promotions have been applied to order at ordersubmit
   * 
   * @param order
   */
  protected void computePromoCodeUse(final NMOrderImpl order) {
    try {
      
      final String pName = (String) order.getPropertyValue("promoName");
      
      if ((pName != null) && !pName.equalsIgnoreCase("")) {
        incPromoCodeUseCountBeanProp();
        setDOLPromoCodeUseBeanProp(order.getLastModifiedDate());
      }
    } catch (final Exception e) {
      final String errString = "computePromoCodeUse value error";
      log.error(errString, this);
      
    }
    
  }
  
  protected void computeGiftOptionsUseCount(final NMOrderImpl order) {
    @SuppressWarnings("unchecked")
    final List<NMCommerceItem> items = order.getCommerceItems();
    boolean hasGift = false;
    for (final NMCommerceItem nmci : items) {
      if ((nmci.getGiftWrap() != null) || (nmci.getGiftNote() != 0)) {
        hasGift = true;
        break;
      }
    }
    
    if (hasGift) {
      incGiftOptionsUseCountBeanProp();
      setDOLGiftOptionsUseBeanProp(order.getLastModifiedDate());
    }
  }
  
  protected void computeAvgTimeBetweenVisits() {
    int numVisits = 0;
    int atbv = 0;
    final Calendar currentDate = Calendar.getInstance();
    final Calendar fDCal = Calendar.getInstance();
    java.util.Date firstDate = null;
    
    final int pType = this.getTypeBeanProp();
    
    try {
      
      if (getIntValue(ProfileRepository.TOTAL_VISITS_PROP) != 0) {
        numVisits = getIntValue(ProfileRepository.TOTAL_VISITS_PROP);
      } else {
        numVisits = 1;
      }
      
      // 1 is registered,0 is anon
      if (pType == 1) {
        if (this.getPropertyValue("registrationDate") != null) {
          firstDate = (Date) this.getPropertyValue("registrationDate");
        } else {
          firstDate = (Date) this.getPropertyValue("profileCreateDate");
        }
        
      } else {
        if (pType == 0) {
          if (this.getPropertyValue("DOLNmPurchase") != null) {
            firstDate = (Date) this.getPropertyValue("DOLNmPurchase");
          } else {
            firstDate = (Date) this.getPropertyValue("lastActivity");
          }
        }
      }
      
      if (firstDate == null) {
        firstDate = fDCal.getTime();
      }
      
      fDCal.setTime(firstDate);
      final long value = currentDate.getTimeInMillis() - fDCal.getTimeInMillis();
      final long val2 = value / numVisits;
      final long hours = val2 / 3600;
      final float days = hours / 24;// /1000;
      final BigDecimal bddays = new BigDecimal(days / 1000);
      
      if (bddays.intValue() < 1) {
        atbv = 1;
      } else {
        atbv = bddays.intValue();
      }
    } catch (final Exception e) {
      final String errString = "computeAvgTimeBetweenVisits value error";
      log.error(errString, this);
    }
    setAvgTimeBetweenVisits(atbv);
  }
  
  public int getPercentOfPromoCodeUse() {
    return getIntValue("percentPromoCodeUseCount");
  }
  
  public void setPercentOfPromoCodeUse(final int value) {
    setPropertyValue("percentPromoCodeUseCount", new Integer(value));
  }
  
  protected void computePercentOfPromoCodeUse() {
    final float pCodeUse = getPromoCodeUseCount();
    final long numOrders = getNumberOfOrdersBeanProp();
    BigDecimal pOPCU = null;
    if (pCodeUse != 0.0F) {
      if (numOrders != 0L) {
        pOPCU = new BigDecimal(pCodeUse / numOrders);
        final int ppc = pOPCU.movePointRight(2).intValue();
        setPercentOfPromoCodeUse(ppc);
      }
    } else {
      setPercentOfPromoCodeUse(0);
    }
  }
  
  public void setAvgTimeBetweenVisits(final int value) {
    setPropertyValue(ProfileRepository.AVG_TIME_BETWEEN_VISITS, new Integer(value));
  }
  
  public int getAvgTimeBetweenVisits() {
    return getIntValue(ProfileRepository.AVG_TIME_BETWEEN_VISITS);
  }
  
  public void setSegmentId(final String value) {
    setPropertyValue(ProfileRepository.SEGMENT_ID, value);
  }
  
  public String getSegmentId() {
    return getStringValue(ProfileRepository.SEGMENT_ID);
  }
  
  public void setSegmentName(final String value) {
    setPropertyValue(ProfileRepository.SEGMENT_NAME, value);
  }
  
  public String getSegmentName() {
    return getStringValue(ProfileRepository.SEGMENT_NAME);
  }
  
  public void setSegmentEnrollmentDate(final Date value) {
    setPropertyValue(ProfileRepository.SEGMENT_ENROLLMENT_DATE, value);
  }
  
  public Date getSegmentEnrollmentDate() {
    return (Date) getPropertyValue(ProfileRepository.SEGMENT_ENROLLMENT_DATE);
  }
  
  public void setIncircleFlgFromMarketing(final int value) {
    setPropertyValue(ProfileRepository.INCIRCLE_FLG_FROM_MARKETING, new Integer(value));
  }
  
  public int getIncircleFlgFromMarketing() {
    int type = 0;
    final Integer typeObj = (Integer) getPropertyValue(ProfileRepository.INCIRCLE_FLG_FROM_MARKETING);
    if (typeObj != null) {
      type = typeObj.intValue();
    }
    return type;
  }
  
  public void setBestCustomerFlg(final int value) {
    setPropertyValue(ProfileRepository.BEST_CUSTOMER_FLG, new Integer(value));
  }
  
  public int getBestCustomerFlg() {
    int type = 0;
    final Integer typeObj = (Integer) getPropertyValue(ProfileRepository.BEST_CUSTOMER_FLG);
    if (typeObj != null) {
      type = typeObj.intValue();
    }
    return type;
  }
  
  public void setStoreArea(final String value) {
    setPropertyValue(ProfileRepository.STORE_AREA, value);
  }
  
  public String getStoreArea() {
    return getStringValue(ProfileRepository.STORE_AREA);
  }
  
  public void setNMActiveCard(final int value) {
    setPropertyValue(ProfileRepository.NM_ACTIVE_CARD, new Integer(value));
  }
  
  public int getNMActiveCard() {
    int type = 0;
    final Integer typeObj = (Integer) getPropertyValue(ProfileRepository.NM_ACTIVE_CARD);
    if (typeObj != null) {
      type = typeObj.intValue();
    }
    return type;
    
  }
  
  public void setPercentOfNmcardOnlineSales(final int value) {
    setPropertyValue(ProfileRepository.PERCENT_OF_NMCARD_ONLINE_SALES, new Integer(value));
  }
  
  public int getPercentOfNmcardOnlineSales() {
    return getIntValue(ProfileRepository.PERCENT_OF_NMCARD_ONLINE_SALES);
  }
  
  public void setInstoreCardUse(final String value) {
    setPropertyValue(ProfileRepository.INSTORE_CARD_USE, value);
  }
  
  public String getInstoreCardUse() {
    return getStringValue(ProfileRepository.INSTORE_CARD_USE);
  }
  
  public void setNMCardLastPurchOnlineDate(final Date value) {
    setPropertyValue(ProfileRepository.NMCARD_LAST_PURCH_ONLINE_DATE, value);
  }
  
  public Date getNMCardLastPurchOnlineDate() {
    return (Date) getPropertyValue(ProfileRepository.NMCARD_LAST_PURCH_ONLINE_DATE);
  }
  
  public void setCatUpsellFlg(final int value) {
    setPropertyValue(ProfileRepository.CAT_UPSELL_FLAG, new Integer(value));
  }
  
  public int getCatUpsellFlg() {
    int type = 0;
    final Integer typeObj = (Integer) getPropertyValue(ProfileRepository.CAT_UPSELL_FLAG);
    if (typeObj != null) {
      type = typeObj.intValue();
    }
    return type;
    
  }
  
  public void setShowroomBuyerFlg(final int value) {
    setPropertyValue(ProfileRepository.SHOWROOM_BUYER_FLAG, new Integer(value));
  }
  
  public int getShowroomBuyerFlg() {
    int type = 0;
    final Integer typeObj = (Integer) getPropertyValue(ProfileRepository.SHOWROOM_BUYER_FLAG);
    if (typeObj != null) {
      type = typeObj.intValue();
    }
    return type;
  }
  
  public void setDateAddrChangePo(final Date value) {
    setPropertyValue(ProfileRepository.DATE_OF_ADDR_CHANGE_PO, value);
  }
  
  public Date getDateAddrChangePo() {
    return (Date) getPropertyValue(ProfileRepository.DATE_OF_ADDR_CHANGE_PO);
  }
  
  public void setDateAddrChangeNcoa(final Date value) {
    setPropertyValue(ProfileRepository.DATE_OF_ADDR_CHANGE_NCOA, value);
  }
  
  public Date getDateAddrChangeNcoa() {
    return (Date) getPropertyValue(ProfileRepository.DATE_OF_ADDR_CHANGE_NCOA);
  }
  
  public void setCrosssellBuyer(final int value) {
    setPropertyValue(ProfileRepository.CROSSSELL_BUYER, new Integer(value));
  }
  
  public int getCrosssellBuyer() {
    int type = 0;
    final Integer typeObj = (Integer) getPropertyValue(ProfileRepository.CROSSSELL_BUYER);
    if (typeObj != null) {
      type = typeObj.intValue();
    }
    return type;
    
  }
  
  public void setDOLNMPurchase(final Date value) {
    setPropertyValue(ProfileRepository.DOL_NM_PURCHASE, value);
  }
  
  public Date getDOLNMPurchase() {
    return (Date) getPropertyValue(ProfileRepository.DOL_NM_PURCHASE);
  }
  
  public void setLuxuryBuyerDate(final Date value) {
    setPropertyValue(ProfileRepository.LUXURY_BUYER_DATE, value);
  }
  
  public Date getLuxuryBuyerDate() {
    return (Date) getPropertyValue(ProfileRepository.LUXURY_BUYER_DATE);
  }
  
  public void setSegmentAge(final int value) {
    setPropertyValue(ProfileRepository.SEGMENT_AGE, new Integer(value));
  }
  
  public int getSegmentAge() {
    return getIntValue(ProfileRepository.SEGMENT_AGE);
  }
  
  public void setIncome(final String value) {
    setPropertyValue(ProfileRepository.INCOME, value);
  }
  
  public String getIncome() {
    return getStringValue(ProfileRepository.INCOME);
  }
  
  public void setHomeValue(final String value) {
    setPropertyValue(ProfileRepository.HOME_VALUE, value);
  }
  
  public String getHomeValue() {
    return getStringValue(ProfileRepository.HOME_VALUE);
  }
  
  public void setMaritalStatus(final String value) {
    setPropertyValue(ProfileRepository.MARITAL_STATUS, value);
  }
  
  public String getMaritalStatus() {
    return getStringValue(ProfileRepository.MARITAL_STATUS);
  }
  
  public void setGender(final String value) {
    setPropertyValue(ProfileRepository.GENDER, value);
  }
  
  public String getGender() {
    return getStringValue(ProfileRepository.GENDER);
  }
  
  public void setGenderFeed(final String value) {
    setPropertyValue(ProfileRepository.GENDER_FEED, value);
  }
  
  public String getGenderFeed() {
    return getStringValue(ProfileRepository.GENDER_FEED);
  }
  
  public void setDateVar1(final Date value) {
    setPropertyValue(ProfileRepository.DATE_VAR1, value);
  }
  
  public Date getDateVar1() {
    return (Date) getPropertyValue(ProfileRepository.DATE_VAR1);
  }
  
  public void setDateVar2(final Date value) {
    setPropertyValue(ProfileRepository.DATE_VAR2, value);
  }
  
  public Date getDateVar2() {
    return (Date) getPropertyValue(ProfileRepository.DATE_VAR2);
  }
  
  public void setVar3(final String value) {
    setPropertyValue(ProfileRepository.VAR3, value);
  }
  
  public String getVar3() {
    return getStringValue(ProfileRepository.VAR3);
  }
  
  public void setVar4(final String value) {
    setPropertyValue(ProfileRepository.VAR4, value);
  }
  
  public String getVar4() {
    return getStringValue(ProfileRepository.VAR4);
  }
  
  public void setVar5(final String value) {
    setPropertyValue(ProfileRepository.VAR5, value);
  }
  
  public String getVar5() {
    return getStringValue(ProfileRepository.VAR5);
  }
  
  public void setVar6(final String value) {
    setPropertyValue(ProfileRepository.VAR6, value);
  }
  
  public String getVar6() {
    return getStringValue(ProfileRepository.VAR6);
  }
  
  public void setVar7(final String value) {
    setPropertyValue(ProfileRepository.VAR7, value);
  }
  
  public String getVar7() {
    return getStringValue(ProfileRepository.VAR7);
  }
  
  public void setVar8(final String value) {
    setPropertyValue(ProfileRepository.VAR8, value);
  }
  
  public String getVar8() {
    return getStringValue(ProfileRepository.VAR8);
  }
  
  public String getVar49() {
    return getEmailAddressBeanProp();
  }
  
  public void setIpCntry(final String value) {
    setPropertyValue("ipCountryCode", value);
  }
  
  public String getIpCntry() {
    final String rtn = getStringValue("ipCountryCode");
    if (rtn == null) {
      return "";
    }
    return rtn;
  }
  
  private LocalizationUtils localizationUtils;
  
  public LocalizationUtils getLocalizationUtils() {
    return localizationUtils;
  }
  
  public void setLocalizationUtils(final LocalizationUtils localizationUtils) {
    this.localizationUtils = localizationUtils;
  }
  
  public String getCountryPreference() {
    if (getPropertyValue(ProfileRepository.COUNTRY_PREFERENCE) == null) {
      localizationUtils.setLocalizationPreferences(ServletUtil.getCurrentRequest(), this);
      
      if (getPropertyValue(ProfileRepository.COUNTRY_PREFERENCE) == null) {
        return FiftyOneUtils.DEFAULT_COUNTRY_CODE;
      }
    }
    
    return (String) getPropertyValue(ProfileRepository.COUNTRY_PREFERENCE);
  }
  
  public void setCountryPreference(final String countryPreference) {
    setStringPropertyValueIfChanged(ProfileRepository.COUNTRY_PREFERENCE, countryPreference);
  }
  
  public String getCurrencyPreference() {
    if (getPropertyValue(ProfileRepository.CURRENCY_PREFERENCE) == null) {
      localizationUtils.setLocalizationPreferences(ServletUtil.getCurrentRequest(), this);
      
      if (getPropertyValue(ProfileRepository.CURRENCY_PREFERENCE) == null) {
        return FiftyOneUtils.DEFAULT_CURRENCY_CODE;
      }
    }
    
    return (String) getPropertyValue(ProfileRepository.CURRENCY_PREFERENCE);
  }
  
  public void setCurrencyPreference(final String currencyPreference) {
    setStringPropertyValueIfChanged(ProfileRepository.CURRENCY_PREFERENCE, currencyPreference);
  }
  
  public String getCountryName() {
    return localizationUtils.getCountryNameForCountryCode(getCountryPreference());
  }
  
  public String getLanguagePreference() {
    if (getPropertyValue(ProfileRepository.LANGUAGE_PREFERENCE) == null) {
      localizationUtils.setLocalizationPreferences(ServletUtil.getCurrentRequest(), this);
      
      if (getPropertyValue(ProfileRepository.LANGUAGE_PREFERENCE) == null) {
        return localizationUtils.getDefaultLanguageForCountry(FiftyOneUtils.DEFAULT_COUNTRY_CODE);
      }
    }
    
    return (String) getPropertyValue(ProfileRepository.LANGUAGE_PREFERENCE);
  }
  
  public void setLanguagePreference(final String languagePreference) {
    setStringPropertyValueIfChanged(ProfileRepository.LANGUAGE_PREFERENCE, languagePreference);
  }
  
  // To show alternative language in header toggle : start
  public String getLanguageAlternative() {
    return (String) getPropertyValue(ProfileRepository.LANGUAGE_ALTERNATIVE);
  }
  
  public void setLanguageAlternative(final String languageAlternative) {
    setStringPropertyValueIfChanged(ProfileRepository.LANGUAGE_ALTERNATIVE, languageAlternative);
  }
  
  // To show alternative language in header toggle : end
  
  public String getLocalePreference() {
    return getLanguagePreference() + "-" + getCountryPreference().toLowerCase();
  }
  
  public void setLinkedEmail(final String value) {
    setPropertyValue(ProfileRepository.LINKED_EMAIL, value);
  }
  
  public String getLinkedEmail() {
    final SystemSpecs systemSpecs = (SystemSpecs) resolveName("/nm/utils/SystemSpecs");
    String email = getStringValue(ProfileRepository.LINKED_EMAIL);
    if (StringUtilities.isEmpty(email) && systemSpecs.isEnableLinkedEmail()) {
      email = LinkedEmailUtils.getInstance().getDecryptedCookie(ServletUtil.getCurrentRequest());
    }
    return email;
  }
  
  /**
   * This is basically where we add to the comma-separated-list of single character depiction codes that represent the Set(tm) of every depiction-code of every item ever bought by this customer.
   * <p>
   * We don't waste time trying to keep the list in alphabetical order.
   * 
   * @param order
   *          the most-recent order
   */
  protected void addPurchaseDepictionCodes(final NMOrderImpl order) {
    @SuppressWarnings("unchecked")
    final List<NMCommerceItem> items = order.getCommerceItems();
    
    String codes = getPurchaseDepictionCodesBeanProp();
    for (final NMCommerceItem nmci : items) {
      String currCode = null;
      
      final String itemCode = nmci.getCmosItemCode();
      if (itemCode != null) {
        currCode = itemCode.substring(0, 1);
        
        // Quick Catalog orders begin with numbers so do not append
        // to purchase depiction codes list
        final char charCurrCode = currCode.charAt(0);
        if (Character.isDigit(charCurrCode)) {
          currCode = null;
        }
      }
      
      if ((currCode != null) && (codes.indexOf(currCode) == -1)) {
        if (codes.length() > 0) {
          codes += ",";
        }
        codes += currCode;
      }
    }
    setPurchaseDepictionCodesBeanProp(codes);
  }
  
  public void setTypeBeanProp(final int value) {
    setPropertyValue(ProfileRepository.TYPE_PROP, new Integer(value));
  }
  
  public int getTypeBeanProp() {
    int type = UNREGISTERED;
    final Integer typeObj = (Integer) getPropertyValue(ProfileRepository.TYPE_PROP);
    if (typeObj != null) {
      type = typeObj.intValue();
    }
    return type;
  }
  
  /**
   * This method returns all the ACTIVE_GIFT_REGISTRY and INACTIVE_GIFT_REGISTRY associated with this user profile.
   * */
  public List<RepositoryItem> getAllGiftRegistries() {
    try {
      allGiftRegistries = getGiftlistsByType(GiftlistConstants.ACTIVE_GIFT_REGISTRY);
      final List<RepositoryItem> inActivegiftRegistries = getGiftlistsByType(GiftlistConstants.INACTIVE_GIFT_REGISTRY);
      for (final RepositoryItem item : inActivegiftRegistries) {
        allGiftRegistries.add(item);
      }
    } catch (final Exception e) {
      e.printStackTrace();
      return null;
      
    }
    return allGiftRegistries;
  }
  
  public void setAllGiftRegistries(final List<RepositoryItem> allGiftRegistries) {
    this.allGiftRegistries = allGiftRegistries;
  }
  
  /**
   * vske1 2/23/2005 This method returns all the ACTIVE_GIFT_REGISTRY associated with this user profile.
   * */
  public List<RepositoryItem> getActiveGiftRegistries() {
    try {
      activeGiftRegistries = getGiftlistsByType(GiftlistConstants.ACTIVE_GIFT_REGISTRY);
    } catch (final Exception e) {
      e.printStackTrace();
      return null;
      
    }
    return activeGiftRegistries;
  }
  
  public void setActiveGiftRegistries(final List<RepositoryItem> activeGiftRegistries) {
    this.activeGiftRegistries = activeGiftRegistries;
  }
  
  // ---------------------------------------------
  /*
   * vsrb1 6/24/2003 Added the following method, which is returns the encrypted password. Currently only the isAnonymous method (in the same Class) calls the getPassword method:
   */
  protected String getPassword() {
    try {
      String password = null;
      password = (String) getPropertyValue(getProfileTools().getPropertyManager().getPasswordPropertyName());
      
      return password;
    } catch (final NullPointerException npe) {
      npe.printStackTrace();
      return null;
    }
  }
  
  public int getSecurityStatus() {
    final Integer propertyValue = (Integer) getPropertyValue("securityStatus");
    return propertyValue != null ? propertyValue.intValue() : getProfileTools().getPropertyManager().getSecurityStatusAnonymous();
  }
  
  /**
   * nmjll4 Returns true if the profile's current security level is less than the logged in security level. <br>
   * Compares "securityStatus" to getProfileTools().getPropertyManager().getSecurityStatusLogin()
   * 
   */
  public boolean isAnonymous() {
    try {
      if (((Integer) getPropertyValue("securityStatus")).intValue() < getProfileTools().getPropertyManager().getSecurityStatusLogin()) {
        return true;
      } else {
        return false;
      }
    } catch (final Exception e) {
      // if unable to determine security status, err on the safe side and return true
      logError("Unable to determine if profile is anonymous: " + e);
      return true;
    }
  }
  
  public boolean isAuthorized() {
    return !isAnonymous();
  }
  
  public boolean isSessionExpired() {
    return isAnonymous() && isRegisteredProfile() && (getSecurityStatus() == 2);
  }
  
  // EDO changes
  /**
   * Method to return employee/dependent login status.
   * 
   * @return boolean indicating employee/dependent login status.
   * */
  public boolean isAssociateLoggedIn() {
    return isAuthorized() && NmoUtils.getBooleanValue(getPropertyValue(ProfileRepository.ASSOCIATE_LOGIN));
  }
  
  /**
   * vsrb1 6/24/2003 Override super method to also idenitfy whether profile isAnonymous()
   */
  @Override
  public String toString() {
    return super.toString() + "\n anonymous = " + isAnonymous();
  }
  
  /**
   * Makes the current user anonymous by creating a new blank profile for them and copying over the current profile's cart.
   * 
   * @param orderHolder
   *          - The current shopping cart
   * @param orderManager
   *          - The current order manager
   * @throws ServletException
   * @throws RepositoryException
   * @author nmjll4
   */
  public void makeAnonymous(final String checkedOut, final OrderHolder orderHolder, final OrderManager orderManager) throws RepositoryException, CommerceException {
    
    final MutableRepository rep = (MutableRepository) getRepository();
    final RepositoryItem item = rep.createItem("user");
    
    setDataSource(item);
    
    /**
     * PERS2 persist profile
     */
    createAnonymousProfile(item);
    
    final NMOrderImpl order = (NMOrderImpl) orderHolder.getCurrent();
    if ((order != null) && order.getStateAsString().equalsIgnoreCase(OrderStates.INCOMPLETE) && (order.getCommerceItemCount() > 0)) {
      try {
        // copy order to the new anonymous profile and continue
        if (order.isShopRunner()) {
          order.setProfileId(getRepositoryId());
          orderManager.updateOrder(order);
          orderHolder.setCurrent(order);
        } else {
          final NMOrderImpl orderClone = (NMOrderImpl) orderManager.createOrder(getRepositoryId());
          orderManager.mergeOrders(order, orderClone, true, false);
          orderManager.addOrder(orderClone);
          orderHolder.setCurrent(orderClone);
        }
      } catch (final CommerceException e) {
        e.printStackTrace();
      }
    } else {
      // since there the cart was empty, generate an empty cart for the
      // new profile
      final NMOrderImpl emptyOrder = (NMOrderImpl) orderManager.createOrder(getRepositoryId());
      orderManager.addOrder(emptyOrder);
      orderHolder.setCurrent(emptyOrder);
    }
  }
  
  /**
   * Creates a clean profile with a clean order
   * 
   * @param orderHolder
   *          - The current shopping cart
   * @param orderManager
   *          - The current order manager
   * @throws RepositoryException
   * @throws CommerceException
   */
  public void createAssociatePurchaseProfile(final OrderHolder orderHolder, final OrderManager orderManager) throws RepositoryException, CommerceException {
    final MutableRepository rep = (MutableRepository) getRepository();
    final RepositoryItem item = rep.createItem("user");
    
    setDataSource(item);
    
    createAnonymousProfile(item);
    
    final NMOrderImpl emptyOrder = (NMOrderImpl) orderManager.createOrder(getRepositoryId());
    orderManager.addOrder(emptyOrder);
    orderHolder.setCurrent(emptyOrder);
  }
  
  /**
   * For the persisting of anonymous profiles for the purposes of the saving of personalization stats PERS2 added RepositoryItem for the new profileId we are assigning for anon checkout
   * 
   */
  public void createAnonymousProfile(final RepositoryItem item) {
    if ((item != null) && item.isTransient()) {
      boolean rollbackTrans = false;
      final TransactionDemarcation trans = new NMTransactionDemarcation();
      try {
        trans.begin(CommonComponentHelper.getTransactionManager());
        
        final MutableRepositoryItem profile = (MutableRepositoryItem) item;
        
        // login ids must be unique, due to DB constraints, even if
        // never used
        final String login = profile.getRepositoryId();// + "@anon.gen";
        profile.setPropertyValue(ProfileRepository.LOGIN_PROP, login);
        final String email = (String) profile.getPropertyValue(ProfileRepository.EMAIL_ADDRESS_PROP);
        if ((email == null) || (email.trim().length() == 0)) {
          profile.setPropertyValue(ProfileRepository.EMAIL_ADDRESS_PROP, "");
        }
        profile.setPropertyValue(ProfileRepository.PASSWORD_PROP, login);
        profile.setPropertyValue(ProfileRepository.FLG_REGISTERED_PROP, Boolean.FALSE);
        profile.setPropertyValue(ProfileRepository.USER_CODE_PROP, "A");
        // this is an unregistered profile
        profile.setPropertyValue(ProfileRepository.TYPE_PROP, new Integer(UNREGISTERED));
        // do NOT transfer this profile to CMOS
        profile.setPropertyValue(ProfileRepository.CMOS_TRANSFER_PROP, new Integer(0));
        profile.setPropertyValue(ProfileRepository.FLG_FREE_BASE_SHIPPING_PROP, Boolean.FALSE);
        profile.setPropertyValue(ProfileRepository.FLG_AUTO_LOGIN_PROP, Boolean.TRUE);
        profile.setPropertyValue(ProfileRepository.LAST_ACTIVITY_PROP, new Timestamp(System.currentTimeMillis()));
        profile.setPropertyValue(ProfileRepository.LAST_VISIT_DATE_PROP, new Timestamp(System.currentTimeMillis()));
        profile.setPropertyValue(ProfileRepository.CURRENT_VISIT_DATE_PROP, new Timestamp(System.currentTimeMillis()));
      } catch (final TransactionDemarcationException e) {
        rollbackTrans = true;
        logError(e);
      } finally {
        try {
          trans.end(rollbackTrans);
        } catch (final TransactionDemarcationException e) {
          logError(e);
        }
      }
    }
  }
  
  public void resetCalendarYearStats() {
    setTotalOrderTotalThisCalendarYearBeanProp(0.0);
  }
  
  public void resetAllPurchaseStats() {
    resetCalendarYearStats();
    resetLifetimeOrderTotalBeanProp();
    setLastOrderTotalBeanProp(0.0);
    setLastPurchaseStateBeanProp("");
    setNumberOfOrdersBeanProp(0);
    setFirstPurchaseDateBeanProp(null);
    setLastPurchaseDateBeanProp(null);
    setPurchaseDepictionCodesBeanProp("");
  }
  
  /**
   * Compiles usage statistics based on latest completed order. Also copies those values into the linked profile (if link exists) Called from SCH.updatePersonalizationStats
   * 
   * @param order
   *          order detail object to get things like purchase-date and purchase-total from.
   */
  public void compileStats(final NMOrderImpl order) {
    // PERS2 changed persist method params
    try {
      
      final RepositoryItem repId = ((CommerceProfileTools) getProfileTools()).getProfileForOrder(order);
      createAnonymousProfile(repId);
    } catch (final RepositoryException re) {
      re.printStackTrace();
    }
    // ***********************************
    
    setLastPurchaseDateBeanProp(order.getLastModifiedDate());
    if (getFirstPurchaseDateBeanProp() == null) {
      setFirstPurchaseDateBeanProp(order.getLastModifiedDate());
    }
    incNumberOfOrdersBeanProp();
    final double orderTotal = order.getPriceInfo().getTotal();
    setLastOrderTotalBeanProp(orderTotal);
    incLifetimeOrderTotalBeanProp(orderTotal);
    incTotalOrderTotalThisCalendarYearBeanProp(orderTotal);
    setLastPurchaseState(order);
    addPurchaseDepictionCodes(order);
    computeMWSPurchaseCount(order);
    computePromoCodeUse(order);
    computeGiftOptionsUseCount(order);
  }
  
  public void compileVisitStats() {
    incTotalVisitsBeanProp();
    setLastVisitDateBeanProp(lastVisitDate());
    setCurrentVisitDateBeanProp(new Timestamp(System.currentTimeMillis()));
    computeAvgTimeBetweenVisits();
    computePercentOfPromoCodeUse();
  }
  
  /**
   * nmjll4 Changed. user code is no longer useful.
   * 
   * @return
   */
  public boolean isRegisteredProfile() {
    if ((getPropertyValue("type") == null) || getPropertyValue("type").equals(new Integer(0))) {
      return false;
    } else {
      return getTypeBeanProp() == 1;
    }
  }
  
  /**
   * Convenience method to do the debug checking and debug logging together. Assumes user of method only ever wants it logged if its enabled.
   * 
   * @param pMessage
   *          the message that is displayed IF isLoggingDebug() returns true.
   */
  public final void checkedLogDebug(final String pMessage) {
    if (isLoggingDebug()) {
      logDebug(pMessage);
    }
  }
  
  public final void checkedLogError(final String message) {
    if (isLoggingError()) {
      logError(message);
    }
  }
  
  /**
   * Convenience method that defaults increment-by value to 1
   * 
   * @param key
   *          the name of the field
   * @return the incremented value
   */
  protected int incBeanProp(final String key) {
    return incBeanProp(key, 1);
  }
  
  /**
   * Gets the given field, increments its value and then stores the new value back into the field/property.
   * 
   * @param key
   *          the name of the field
   * @return the incremented value
   */
  protected int incBeanProp(final String key, final int incValue) {
    int val = getIntValue(key);
    val += incValue;
    setPropertyValue(key, new Integer(val));
    // broadcastPropertyIncrement(key,incValue);
    return val;
  }
  
  /**
   * Gets the given field, increments its value and then stores the new value back into the field/property.
   * 
   * @param key
   *          the name of the field
   * @return the incremented value
   */
  protected double incBeanProp(final String key, final double incValue) {
    
    double val = getDoubleValue(key);
    val += incValue;
    setPropertyValue(key, new Double(val));
    // broadcastPropertyIncrement(key,incValue);
    return val;
  }
  
  // check if a user has any wishlist.
  public void userHasWishList(final NMProfile profile) {
    final String profileId = profile.getRepositoryId();
    
    try {
      final Repository giftListRep = (Repository) resolveName("/atg/commerce/gifts/Giftlists");
      final RepositoryView giftListView = giftListRep.getView("gift-list");
      final RqlStatement statement = RqlStatement.parseRqlStatement("owner = ?0");
      final Object params[] = new Object[1];
      params[0] = profileId;
      final RepositoryItem giftListArray[] = statement.executeQuery(giftListView, params);
      
      if ((giftListArray != null) && (giftListArray.length > 0)) {
        profile.setPropertyValue("userHasActiveWishList", new Boolean(true));
        
      } else {
        profile.setPropertyValue("userHasActiveWishList", new Boolean(false));
      }
      
    } catch (final RepositoryException e) {
      if (isLoggingError()) { 
    	  logError("Exception thrown in NMProfile.userHasWishList()");
      }
    }
  }
  
  // setting the lastvisitDate date from currentVisit date
  public Date lastVisitDate() {
    
    Date lastactivityDate = null;
    final Date lastactivityDateOld = getCurrentVisitDateBeanProp();
    
    if (getCurrentVisitDateBeanProp() != null) {
      lastactivityDate = lastactivityDateOld;
    } else {
      lastactivityDate = new Timestamp(System.currentTimeMillis());
    }
    return lastactivityDate;
    
  }// end of lastVisitDate
  
  public Set<RepositoryItem> getCampaignIds() {
    @SuppressWarnings("unchecked")
    final Set<RepositoryItem> returnValue = (Set<RepositoryItem>) getPropertyValue(ProfileRepository.SITETRACKER_CAMPAIGN_IDS);
    return returnValue;
  }
  
  public void setCampaignIds(final Set<RepositoryItem> campaignIds) {
    setPropertyValue(ProfileRepository.SITETRACKER_CAMPAIGN_IDS, campaignIds);
  }
  
  // this map contains the creditCards but with the values as NMCreditCard
  // Objects
  public Map<String, NMCreditCard> getCreditCardMap() {
    final Map<String, NMCreditCard> creditCardMap = new LinkedHashMap<String, NMCreditCard>();
    @SuppressWarnings("unchecked")
    final Map<String, RepositoryItem> profileCreditCardMap = (Map<String, RepositoryItem>) getPropertyValue(ProfileProperties.Profile_Desc_creditCards);
    final RepositoryItem defaultCreditCardRI = (RepositoryItem) getPropertyValue(ProfileProperties.Profile_defaultCreditCard);
    
    if (defaultCreditCardRI != null) {
      profileCreditCardMap.put(ProfileProperties.Profile_defaultCreditCard, defaultCreditCardRI);
    }
    
    // populate creditCardMap from profileCreditCards
    final Set<Map.Entry<String, RepositoryItem>> profileCreditCardSet = profileCreditCardMap.entrySet();
    final Map<NMCreditCard, String> sortedCards = new TreeMap<NMCreditCard, String>();
    
    for (final Map.Entry<String, RepositoryItem> ccMapEntry : profileCreditCardSet) {
      final NMCreditCard nmCreditCard = new NMCreditCard();
      nmCreditCard.setRepositoryItem((MutableRepositoryItem) ccMapEntry.getValue());
      
      if (ccMapEntry.getKey().equals(ProfileProperties.Profile_defaultCreditCard)) {
        creditCardMap.put(ccMapEntry.getKey(), nmCreditCard);
      } else {
        sortedCards.put(nmCreditCard, ccMapEntry.getKey());
      }
    }
    
    for (final Map.Entry<NMCreditCard, String> sortedEntry : sortedCards.entrySet()) {
      creditCardMap.put(sortedEntry.getValue(), sortedEntry.getKey());
    }
    
    return creditCardMap;
  }
  
  public Map<String, NMCreditCard> getCFACreditCardMap() {
    final Map<String, NMCreditCard> creditCardMap = new LinkedHashMap<String, NMCreditCard>();
    @SuppressWarnings("unchecked")
    final Map<String, RepositoryItem> profileCreditCardMap = (Map<String, RepositoryItem>) getPropertyValue(ProfileProperties.Profile_Desc_creditCards);
    // populate creditCardMap from profileCreditCards
    final Set<Map.Entry<String, RepositoryItem>> profileCreditCardSet = profileCreditCardMap.entrySet();
    final Map<NMCreditCard, String> sortedCards = new TreeMap<NMCreditCard, String>();
    for (final Map.Entry<String, RepositoryItem> ccMapEntry : profileCreditCardSet) {
      final NMCreditCard nmCreditCard = new NMCreditCard();
      nmCreditCard.setRepositoryItem((MutableRepositoryItem) ccMapEntry.getValue());
      sortedCards.put(nmCreditCard, ccMapEntry.getKey());
    }
    for (final Map.Entry<NMCreditCard, String> sortedEntry : sortedCards.entrySet()) {
      creditCardMap.put(sortedEntry.getValue(), sortedEntry.getKey());
    }
    return creditCardMap;
  }
  
  public NMCreditCard getDefaultCreditCard() {
    final RepositoryItem defaultCardItem = (RepositoryItem) getPropertyValue(ProfileProperties.Profile_defaultCreditCard);
    
    if (defaultCardItem == null) {
      return null;
    }
    
    final NMCreditCard defaultCard = new NMCreditCard();
    defaultCard.setRepositoryItem((MutableRepositoryItem) defaultCardItem);
    return defaultCard;
  }
  
  public void setDefaultCreditCard(final RepositoryItem creditCard) {
    setPropertyValue(ProfileProperties.Profile_defaultCreditCard, creditCard);
  }
  
  public NMCreditCard getCreditCardByNumber(final String cardNumber) {
    if (StringUtilities.isEmpty(cardNumber)) {
      return null;
    }
    
    for (final NMCreditCard creditCard : getCreditCardMap().values()) {
      if (cardNumber.equals(creditCard.getCreditCardNumber())) {
        return creditCard;
      }
    }
    return null;
  }
  
  public NMCreditCard getCreditCardById(final String cardId) {
    if (StringUtilities.isEmpty(cardId)) {
      return null;
    }
    
    if (cardId.equals(ProfileProperties.Profile_defaultCreditCard)) {
      return getCreditCardMap().get(cardId);
    }
    
    for (final NMCreditCard creditCard : getCreditCardMap().values()) {
      if (creditCard.getRepositoryId().equals(cardId)) {
        return creditCard;
      }
    }
    
    return null;
  }
  
  public NMCreditCard getPayPalByEmail(final String email) {
    if (StringUtilities.isEmpty(email)) {
      return null;
    }
    
    for (final NMCreditCard creditCard : getCreditCardMap().values()) {
      if (email.equalsIgnoreCase(creditCard.getPayPalEmail())) {
        return creditCard;
      }
    }
    
    return null;
  }
  
  public String addCreditCard(final NMCreditCard creditCard) {
    if (creditCard == null) {
      return null;
    }
    
    final RepositoryItem defaultCreditCard = getCreditCards().get(ProfileProperties.Profile_defaultCreditCard);
    final String nickname =
            (defaultCreditCard == null) || (defaultCreditCard.getRepositoryId() == creditCard.getRepositoryId()) ? ProfileProperties.Profile_defaultCreditCard : creditCard.getRepositoryId();
    
    getCreditCards().put(nickname, creditCard.getRepositoryItem());
    
    if (nickname.equals(ProfileProperties.Profile_defaultCreditCard)) {
      setDefaultCreditCard(creditCard.getRepositoryItem());
    }
    
    return nickname;
  }
  
  public void removeCreditCard(final NMCreditCard creditCard) throws RepositoryException {
    if (creditCard == null) {
      return;
    }
    
    final RepositoryItem defaultCreditCard = getCreditCards().get(ProfileProperties.Profile_defaultCreditCard);
    final String nickname =
            (defaultCreditCard == null) || (defaultCreditCard.getRepositoryId() == creditCard.getRepositoryId()) ? ProfileProperties.Profile_defaultCreditCard : creditCard.getRepositoryId();
    final RepositoryItem removeBillingAddress = (RepositoryItem) creditCard.getPropertyValue(ProfileProperties.CreditCard_billingAddress);
    final String removeBillingAddressId = removeBillingAddress != null ? removeBillingAddress.getRepositoryId() : "";
    
    if (nickname.equals(ProfileProperties.Profile_defaultCreditCard)) {
      setDefaultCreditCard(null);
    }
    
    if ((getBillingAddress() != null) && getBillingAddress().getRepositoryId().equals(removeBillingAddressId)) {
      setBillingAddress(null);
    }
    
    getCreditCards().remove(nickname);
    
    if ((getDefaultCreditCard() == null) && !getCreditCards().isEmpty()) {
      final Map<String, MutableRepositoryItem> cards = getCreditCards();
      final Map.Entry<String, MutableRepositoryItem> firstCard = getCreditCards().entrySet().iterator().next();
      setDefaultCreditCard(firstCard.getValue());
      cards.remove(firstCard.getKey());
      cards.put(ProfileProperties.Profile_defaultCreditCard, firstCard.getValue());
    }
    
    if ((getBillingAddress() == null) && (getDefaultCreditCard() != null)) {
      setBillingAddress((RepositoryItem) getDefaultCreditCard().getPropertyValue(ProfileProperties.CreditCard_billingAddress));
    }
  }
  
  public boolean changeDefaultCreditCard(final NMCreditCard creditCard) throws RepositoryException {
    if (creditCard == null) {
      return false;
    }
    
    final Map<String, MutableRepositoryItem> cards = getCreditCards();
    final MutableRepositoryItem defaultMappedCard = cards.get(ProfileProperties.Profile_defaultCreditCard);
    final RepositoryItem cardBillingAddress = (RepositoryItem) creditCard.getPropertyValue(ProfileProperties.CreditCard_billingAddress);
    
    if (!getCreditCardMap().values().contains(creditCard)) {
      return false;
    }
    
    setDefaultCreditCard(creditCard.getRepositoryItem());
    if (cardBillingAddress != null) {
      setBillingAddress(cardBillingAddress);
    } else {
      creditCard.setPropertyValue(ProfileProperties.CreditCard_billingAddress, getBillingAddress());
    }
    
    if (defaultMappedCard == null) {
      cards.put(ProfileProperties.Profile_defaultCreditCard, creditCard.getRepositoryItem());
      
    } else if (!creditCard.getRepositoryId().equals(defaultMappedCard.getRepositoryId())) {
      cards.remove(creditCard.getRepositoryId());
      cards.put(ProfileProperties.Profile_defaultCreditCard, creditCard.getRepositoryItem());
      cards.put(defaultMappedCard.getRepositoryId(), defaultMappedCard);
    }
    
    return true;
  }
  
  // This method is to clear the contact info data and credit card data from
  // the profile. These records will be
  // orphaned if this is not done. Even on removal of the profile itself these
  // records will orphan.
  // DPS_CONTACT_INFO
  // DPS_CREDIT_CARD
  public void clearSensitiveData() {
    boolean rollbackTrans = false;
    final TransactionDemarcation trans = new NMTransactionDemarcation();
    
    try {
      trans.begin(CommonComponentHelper.getTransactionManager());
      final Map<String, String> addrIdsToRemoveMap = new HashMap<String, String>();
      final Map<String, String> ccIdsToRemoveMap = new HashMap<String, String>();
      
      // first remove CC's
      final RepositoryItem defaultCCRI = (RepositoryItem) getPropertyValue(ProfileProperties.Profile_defaultCreditCard);
      
      if (defaultCCRI != null) {
        ccIdsToRemoveMap.put(defaultCCRI.getRepositoryId(), "");
        final RepositoryItem ccAddressRI = (RepositoryItem) defaultCCRI.getPropertyValue(ProfileProperties.CreditCard_billingAddress);
        
        if (ccAddressRI != null) {
          addrIdsToRemoveMap.put(ccAddressRI.getRepositoryId(), "");
          ((MutableRepositoryItem) defaultCCRI).setPropertyValue(ProfileProperties.CreditCard_billingAddress, null);
        }
        
      }
      
      @SuppressWarnings("unchecked")
      final Map<String, RepositoryItem> profileCreditCardMap = (Map<String, RepositoryItem>) getPropertyValue(ProfileProperties.Profile_Desc_creditCards);
      final Set<Map.Entry<String, RepositoryItem>> ccMapEntrySet = profileCreditCardMap.entrySet();
      for (final Map.Entry<String, RepositoryItem> mapEntry : ccMapEntrySet) {
        try {
          final String ccId = mapEntry.getValue().getRepositoryId();
          
          if ((ccId != null) && !ccId.equals("")) {
            ccIdsToRemoveMap.put(ccId, "");
            profileCreditCardMap.remove(mapEntry.getKey());
          }
          
        } catch (final Exception e) {
          // continue on failure
          logError(e);
        }
      }
      
      setPropertyValue(ProfileProperties.Profile_defaultCreditCard, null);
      getProfileTools().getProfileRepository().updateItem((MutableRepositoryItem) getDataSource());
      profileCreditCardMap.clear();
      
      final Set<String> ccIdsToRemoveSet = ccIdsToRemoveMap.keySet();
      
      for (final String ccId : ccIdsToRemoveSet) {
        try {
          getProfileTools().getProfileRepository().removeItem(ccId, "credit-card");
          getProfileTools().getProfileRepository().updateItem((MutableRepositoryItem) getDataSource());
        } catch (final Exception e) {
          // continue on failure
          logError(e);
        }
      }
      
      // remove addresses
      final RepositoryItem shippingAddressRI = (RepositoryItem) getPropertyValue("shippingAddress");
      final RepositoryItem billingAddressRI = (RepositoryItem) getPropertyValue("billingAddress");
      final RepositoryItem homeAddressRI = (RepositoryItem) getPropertyValue("homeAddress");
      
      if (shippingAddressRI != null) {
        addrIdsToRemoveMap.put(shippingAddressRI.getRepositoryId(), "");
      }
      
      if (billingAddressRI != null) {
        addrIdsToRemoveMap.put(billingAddressRI.getRepositoryId(), "");
      }
      
      if (homeAddressRI != null) {
        addrIdsToRemoveMap.put(homeAddressRI.getRepositoryId(), "");
      }
      
      setPropertyValue("shippingAddress", null);
      setPropertyValue("billingAddress", null);
      setPropertyValue("homeAddress", null);
      
      getProfileTools().getProfileRepository().updateItem((MutableRepositoryItem) getDataSource());
      
      final Map<String, MutableRepositoryItem> secondaryAddressMap = getSecondaryAddresses();
      final Set<Map.Entry<String, MutableRepositoryItem>> secondaryAddressEntrySet = secondaryAddressMap.entrySet();
      for (final Map.Entry<String, MutableRepositoryItem> mapEntry : secondaryAddressEntrySet) {
        try {
          final RepositoryItem addrRI = mapEntry.getValue();
          
          if (addrRI != null) {
            addrIdsToRemoveMap.put(addrRI.getRepositoryId(), "");
            addrIdsToRemoveMap.remove(mapEntry.getKey());
          }
          
        } catch (final Exception e) {
          logError(e);
        }
      }
      
      secondaryAddressMap.clear();
      
      final Set<String> addrIdsToRemoveSet = addrIdsToRemoveMap.keySet();
      for (final String addrId : addrIdsToRemoveSet) {
        try {
          getProfileTools().getProfileRepository().removeItem(addrId, ProfileProperties.Profile_Desc_contactInfo);
          getProfileTools().getProfileRepository().updateItem((MutableRepositoryItem) getDataSource());
        } catch (final Exception e) {
          // continue on failure
          e.printStackTrace();
        }
      }
      
    } catch (final Exception e) {
      rollbackTrans = true;
      logError(e);
    } finally {
      try {
        trans.end(rollbackTrans);
      } catch (final TransactionDemarcationException e) {
        logError(e);
      }
    }
  }
  
  public Set<String> getProfileCampaigns() {
    final Set<String> stringSet = new HashSet<String>();
    try {
      @SuppressWarnings("unchecked")
      final Set<RepositoryItem> arep = (Set<RepositoryItem>) getPropertyValue("stCampaignIds");
      
      for (final RepositoryItem repCC : arep) {
        // download to Profile
        final String arepCC = repCC.getPropertyValue("campaignCategory").toString();
        // String arepTestFlag = "false";
        stringSet.add(arepCC);
      }
      
    } catch (final Exception e) {
      logError(e);
    }
    if (isLoggingDebug()) {
      logDebug("--profile id-->" + getRepositoryId() + "<--campaignCategoryList-->" + stringSet + "<--");
    }
    return stringSet;
  }
  
  public boolean getShowProfileMWSNew() {
    if (isLoggingDebug()) {
      logDebug("***===--- inside getShowProfileMWSNew ---===***");
    }
    boolean newItems = false;
    try {
      if (getFlgProfileMWS()) {
        if (isLoggingDebug()) {
          logDebug("       __________ getFlgProfileMWS = TRUE __________");
        }
        final String queryString = "profileId =  ?0  AND flgViewed =  ?1";
        final RepositoryView view = getProfileTools().getProfileRepository().getView("profile_mws");
        final Object params[] = new Object[2];
        params[0] = getRepositoryId();
        params[1] = "0";
        final RqlStatement statement = RqlStatement.parseRqlStatement(queryString);
        
        final RepositoryItem[] items = statement.executeQuery(view, params);
        
        if (items != null) {
          for (int i = 0; i < items.length; i++) {
            final Boolean beenViewed = (Boolean) items[i].getPropertyValue("flgViewed");
            
            if (beenViewed.booleanValue() == false) {
              newItems = true;
              break;
            }
          }// end for loop
        }// items not null
      } else {
        if (isLoggingDebug()) {
          logDebug("       __________ getFlgProfileMWS = FALSE _________");
        }
      }
      
    } catch (final Exception e) {
      logError(e);
    }
    
    return newItems;
  }
  
  public boolean getShowProfileMws() {
    if (isLoggingDebug()) {
      logDebug("***===--- inside getShowProfileMws ---===***");
    }
    boolean showIt = false;
    
    try {
      if (getFlgProfileMWS()) {
        if (isLoggingDebug()) {
          logDebug("       __________ getFlgProfileMWS = TRUE __________");
        }
        final RepositoryView view = getProfileTools().getProfileRepository().getView("profile_mws");
        final Object[] params = null;
        final RqlStatement statement = RqlStatement.parseRqlStatement("profileId = \"" + getRepositoryId() + "\"");
        
        final RepositoryItem[] items = statement.executeQuery(view, params);
        
        if ((items != null) && (items.length > 0)) {
          showIt = true;
        }
      } else {
        if (isLoggingDebug()) {
          logDebug("       __________ getFlgProfileMWS = FALSE __________");
        }
      }
      
    } catch (final Exception e) {
      logError(e);
    }
    return showIt;
  }
  
  private ProdSkuUtil prodSkuUtil;
  
  public ProdSkuUtil getProdSkuUtil() {
    if (prodSkuUtil == null) {
      prodSkuUtil = CommonComponentHelper.getProdSkuUtil();
    }
    return prodSkuUtil;
  }
  
  public List<RepositoryItem> getProfileMWSReset() {
    if (isLoggingDebug()) {
      logDebug("***===--- inside getProfileMWSReset ---===***");
    }
    final List<RepositoryItem> theList = new ArrayList<RepositoryItem>();
    try {
      if (getFlgProfileMWS()) {
        if (isLoggingDebug()) {
          logDebug("       ___________ getFlgProfileMWS = TRUE __________");
        }
        final RepositoryView view = getProfileTools().getProfileRepository().getView("profile_mws");
        final Object[] params = null;
        final RqlStatement statement = RqlStatement.parseRqlStatement("profileId = \"" + getRepositoryId() + "\"");
        
        final RepositoryItem[] items = statement.executeQuery(view, params);
        
        if (items != null) {
          final MutableRepository mutRep = (MutableRepository) getRepository();
          
          for (int i = 0; i < items.length; i++) {
            
            final String prodId = (String) items[i].getPropertyValue("productId");
            final RepositoryItem mwsRI = items[i];
            final RepositoryItem prodRI = getProdSkuUtil().getProductRepositoryItem(prodId);
            theList.add(prodRI);
            
            final MutableRepositoryItem mutRI = mutRep.getItemForUpdate(mwsRI.getRepositoryId(), "profile_mws");
            mutRI.setPropertyValue("flgViewed", Boolean.TRUE);
            mutRep.updateItem(mutRI);
          }
          
        }
        
      } else {
        if (isLoggingDebug()) {
          logDebug("       __________ getFlgProfileMWS = FALSE __________");
        }
      }
      
    } catch (final Exception e) {
      logError(e);
    }
    
    return theList;
  }
  
  public int getSaveForLaterCartItemCount() {
    // code has been adjusted to reflect the actual qty counts instead of
    // items in cart for sfl.
    @SuppressWarnings("unchecked")
    final Set<RepositoryItem> saveForLaterCartItems = (Set<RepositoryItem>) getPropertyValue("saveForLaterCartItems");
    int sflQty = 0;
    
    for (final RepositoryItem saveForLaterItem : saveForLaterCartItems) {
      final Integer quantity = (Integer) saveForLaterItem.getPropertyValue("quantity");
      sflQty = sflQty + quantity.intValue();
    }
    return sflQty;
  }
  
  private String wishlistReferenceShippingId;
  
  public String getWishlistReferenceShippingId() {
    return wishlistReferenceShippingId;
  }
  
  public void setWishlistReferenceShippingId(final String value) {
    wishlistReferenceShippingId = value;
  }
  
  /**
   * Gets the preferred store number
   * 
   * @return the preferred store number
   * @author nmve1
   */
  public String getPreferredStoreNbr() {
    return getStringValue(ProfileRepository.PREFERRED_STORE_NBR);
  }
  
  /**
   * Sets the preferred store number
   * 
   * @param preferredStoreNbr
   * @return void
   */
  public void setPreferredStoreNbr(final String preferredStoreNbr) {
    // this.preferredStoreNbr = preferredStoreNbr;
    setPropertyValue(ProfileRepository.PREFERRED_STORE_NBR, preferredStoreNbr);
  }
  
  /*
   * @SuppressWarnings("unchecked") public void setAddToFavorite(String addToFavorite, String profileId) { try { MutableRepository userRep = getProfileTools().getProfileRepository(); //RepositoryView
   * view = getProfileTools().getProfileRepository().getView("favoriteItem"); MutableRepositoryItem userItem = userRep.getItemForUpdate(profileId, "user");
   * 
   * Set<RepositoryItem> favoriteItems = ((Set<RepositoryItem>) userItem.getPropertyValue("favoriteCartItems")); if ( favoriteItems == null ) favoriteItems = new HashSet<RepositoryItem>();
   * MutableRepositoryItem favItem = getProfileTools().getProfileRepository().createItem ("favoriteItem"); favItem.setPropertyValue("productId", addToFavorite); favItem.setPropertyValue("dateAdded",
   * new Timestamp(System.currentTimeMillis())); userRep.addItem(favItem); favoriteItems.add(favItem); } catch (RepositoryException e) {
   * System.out.print("Exception thrown in NMProfile.setAddToFavorite()"); e.printStackTrace(); } }
   * 
   * @SuppressWarnings("unchecked") public void setRemoveFromFavorite(String removeFromFavorite, String profileId) { try{ MutableRepository userRep = getProfileTools().getProfileRepository();
   * 
   * MutableRepositoryItem userItem = userRep.getItemForUpdate(profileId, "user");
   * 
   * Set<RepositoryItem> favoriteItems = ((Set<RepositoryItem>) userItem.getPropertyValue("favoriteCartItems")); if ( favoriteItems != null && !favoriteItems.isEmpty()){ RepositoryItem removeFavItem =
   * null; Iterator<RepositoryItem> iter = favoriteItems.iterator(); while(iter.hasNext()){ RepositoryItem vItem = iter.next(); if(vItem.getPropertyValue("productId").equals(removeFromFavorite)){
   * removeFavItem = vItem; if(isLoggingDebug()) { logDebug( "removed item " +removeFavItem); } break; } } try{ if(removeFavItem !=null){ userRep.removeItem(removeFavItem.getRepositoryId(),"user");
   * favoriteItems.remove(removeFavItem); userRep.removeItem(removeFavItem.getRepositoryId(),"favoriteItem"); } }
   * 
   * catch (RepositoryException e) { System.out.print("Exception thrown in NMProfile.setRemoveFromFavorite()"); e.printStackTrace(); }
   * 
   * } }catch(RepositoryException e) { System.out.print("Exception thrown in NMProfile.setRemoveFromFavorite()"); e.printStackTrace(); }
   * 
   * }
   */
  
  public String getWishlistName() {
    String returnValue = "";
    
    final RepositoryItem wishlist = getTransActiveWishlist();
    if (wishlist != null) {
      RepositoryItem item = (RepositoryItem) wishlist.getPropertyValue("owner");
      item = (RepositoryItem) item.getPropertyValue("billingAddress");
      final String firstName = (String) item.getPropertyValue("firstName");
      final String lastName = (String) item.getPropertyValue("lastName");
      returnValue = firstName + " " + lastName;
    }
    
    return returnValue;
  }
  
  public String getPreferredLocation() {
    return preferredLocation;
  }
  
  public void setPreferredLocation(final String value) {
    preferredLocation = value;
  }
  
  public int getPreferredLocationSearchRadius() {
    return preferredLocationSearchRadius;
  }
  
  public void setPreferredLocationSearchRadius(final int radius) {
    preferredLocationSearchRadius = radius;
  }
  
  public String getPreferredSddLocation() {
    return preferredSddLocation;
  }
  
  public void setPreferredSddLocation(final String preferredSddLocation) {
    this.preferredSddLocation = preferredSddLocation;
  }
  
  public boolean isLocked() {
    if (null != getPropertyValue(ProfileRepository.FLG_LOCKED)) {
      return ((Boolean) getPropertyValue(ProfileRepository.FLG_LOCKED)).booleanValue();
    } else {
      return false;
    }
  }
  
  public void setLocked(final boolean flgLocked) {
    setPropertyValue(ProfileRepository.FLG_LOCKED, new Boolean(flgLocked));
  }
  
  public MutableRepositoryItem getRepositoryItem(final String id, final String name) {
    try {
      return (MutableRepositoryItem) getRepository().getItem(id, name);
    } catch (final RepositoryException e) {
      if (isLoggingWarning()) {
        logWarning(String.format("exception getting profile repository item {id: %s, name: %s}: %s", id, name, e));
      }
      return null;
    }
  }
  
  /**
   * Sets the repository property value if and only if the new property value is not equal to the existing property value. This may prevent unnecessary hits to the database in the case where the same
   * value is repeatedly set over and over.
   * 
   * @param pPropertyName
   *          The repository property name.
   * @param pPropertyValue
   *          The repository property value to set if changed.
   */
  public void setStringPropertyValueIfChanged(final String pPropertyName, final String pPropertyValue) {
    if ((pPropertyValue != null) && (getPropertyValue(pPropertyName) != null)) {
      if (!pPropertyValue.equals(getPropertyValue(pPropertyName))) {
        // new value and existing value are both not null, and are different, update the repository
        setPropertyValue(pPropertyName, pPropertyValue);
      }
    } else if ((pPropertyValue == null) && (getPropertyValue(pPropertyName) == null)) {
      // both new value and existing value are null, do nothing
    } else {
      // one value is null and one value is not null, update the repository
      setPropertyValue(pPropertyName, pPropertyValue);
    }
  }
  
  // use this boolean property to check whether to show localized language for CLC or not.
  
  private boolean showLocalizedLanguage;
  private String countryInLocalLang;
  private String languageInLocalLang;
  private String currencyInLocalLang;
  private String englishInLocalLang;
  
  /**
   * @return
   */
  public String getEnglishInLocalLang() {
    return englishInLocalLang;
  }
  
  /**
   * @param englishInLocalLang
   */
  public void setEnglishInLocalLang(final String englishInLocalLang) {
    this.englishInLocalLang = englishInLocalLang;
  }
  
  /**
   * @return the showLocalizedClcLanguage
   */
  public boolean isShowLocalizedLanguage() {
    return showLocalizedLanguage;
  }
  
  /**
   * @param showLocalizedClcLanguage
   *          the showLocalizedClcLanguage to set
   */
  public void setShowLocalizedLanguage(final boolean showLocalizedLanguage) {
    this.showLocalizedLanguage = showLocalizedLanguage;
  }
  
  /**
   * @return the countryInLocalLang
   */
  public String getCountryInLocalLang() {
    return countryInLocalLang;
  }
  
  /**
   * @param countryInLocalLang
   *          the countryInLocalLang to set
   */
  public void setCountryInLocalLang(final String countryInLocalLang) {
    this.countryInLocalLang = countryInLocalLang;
  }
  
  /**
   * @return the languageInLocalLang
   */
  public String getLanguageInLocalLang() {
    return languageInLocalLang;
  }
  
  /**
   * @param languageInLocalLang
   *          the languageInLocalLang to set
   */
  public void setLanguageInLocalLang(final String languageInLocalLang) {
    this.languageInLocalLang = languageInLocalLang;
  }
  
  /**
   * @return the currencyInLocalLang
   */
  public String getCurrencyInLocalLang() {
    return currencyInLocalLang;
  }
  
  /**
   * @param currencyInLocalLang
   *          the currencyInLocalLang to set
   */
  public void setCurrencyInLocalLang(final String currencyInLocalLang) {
    this.currencyInLocalLang = currencyInLocalLang;
  }
  
  public String getShopRunnerEmail() {
    return getStringValue(ProfileRepository.SHOPRUNNER_EMAIL);
  }
  
  public void setShopRunnerEmail(final String shopRunnerEmail) {
    setPropertyValue(ProfileRepository.SHOPRUNNER_EMAIL, shopRunnerEmail);
  }
  
  public String getShopRunnerFirstName() {
    return getStringValue(ProfileRepository.SHOPRUNNER_FIRST_NAME);
  }
  
  public void setShopRunnerFirstName(final String firstName) {
    setPropertyValue(ProfileRepository.SHOPRUNNER_FIRST_NAME, firstName);
  }
  
  public String getShopRunnerLastName() {
    return getStringValue(ProfileRepository.SHOPRUNNER_LAST_NAME);
  }
  
  public void setShopRunnerLastName(final String lastName) {
    setPropertyValue(ProfileRepository.SHOPRUNNER_LAST_NAME, lastName);
  }
  
  public String getShopRunnerPhoneNumber() {
    return getStringValue(ProfileRepository.SHOPRUNNER_PHONE_NUMBER);
  }
  
  public void setShopRunnerPhoneNumber(final String phoneNumber) {
    setPropertyValue(ProfileRepository.SHOPRUNNER_PHONE_NUMBER, phoneNumber);
  }
  
  public String getShopRunnerAddress1() {
    return getStringValue(ProfileRepository.SHOPRUNNER_ADDRESS1);
  }
  
  public void setShopRunnerAddress1(final String shopRunnerAddress1) {
    setPropertyValue(ProfileRepository.SHOPRUNNER_ADDRESS1, shopRunnerAddress1);
  }
  
  public String getShopRunnerAddress2() {
    return getStringValue(ProfileRepository.SHOPRUNNER_ADDRESS2);
  }
  
  public void setShopRunnerAddress2(final String shopRunnerAddress2) {
    setPropertyValue(ProfileRepository.SHOPRUNNER_ADDRESS2, shopRunnerAddress2);
  }
  
  public String getShopRunnerCity() {
    return getStringValue(ProfileRepository.SHOPRUNNER_CITY);
  }
  
  public void setShopRunnerCity(final String shopRunnerCity) {
    setPropertyValue(ProfileRepository.SHOPRUNNER_CITY, shopRunnerCity);
  }
  
  public String getShopRunnerState() {
    return getStringValue(ProfileRepository.SHOPRUNNER_STATE);
  }
  
  public void setShopRunnerState(final String shopRunnerState) {
    setPropertyValue(ProfileRepository.SHOPRUNNER_STATE, shopRunnerState);
  }
  
  public String getShopRunnerZip() {
    return getStringValue(ProfileRepository.SHOPRUNNER_ZIP);
  }
  
  public void setShopRunnerZip(final String shopRunnerZip) {
    setPropertyValue(ProfileRepository.SHOPRUNNER_ZIP, shopRunnerZip);
  }
  
  public String getShopRunnerCountry() {
    return getStringValue(ProfileRepository.SHOPRUNNER_COUNTRY);
  }
  
  public void setShopRunnerCountry(final String shopRunnerCountry) {
    setPropertyValue(ProfileRepository.SHOPRUNNER_COUNTRY, shopRunnerCountry);
  }
  
  /**
   * 
   * @param value
   */
  public void setWebId(final String value) {
    setPropertyValue("webId", value);
  }
  
  /**
   * Returns the webId. The webId is a cookie value that spans multiple session and does not change when a customer logs out or checks out anonymously (unlike the profile id which does change in those
   * cases).
   * 
   * @param value
   * @return
   */
  public String getWebId() {
    return !StringUtilities.isEmpty((String) getPropertyValue("webId")) ? (String) getPropertyValue("webId") : getRepositoryId();
  }
  
  /**
   * 
   * @param value
   */
  public void setRegisteredId(final String value) {
    setPropertyValue("registeredId", value);
  }
  
  /**
   * Returns the registeredId. The registeredId links registered and anonymous profiles. An anonymous profile that log's into a registered account will have it's registeredId set to the profile id of
   * that registered account
   * 
   * @param value
   * @return
   */
  public String getRegisteredId() {
    return (String) getPropertyValue("registeredId");
  }
  
  public void setUcId(final String value) {
    if (null != edwCustData) {
      edwCustData.setUcid(value);
    }
  }
  
  public String getUcId() {
    String ucid = null;
    if (null != edwCustData) {
      ucid = edwCustData.getUcid();
    }
    return ucid;
  }
  
  public EDWCustData getEdwCustData() {
    return edwCustData;
  }
  
  public void setEdwCustData(final EDWCustData edwCustData) {
    this.edwCustData = edwCustData;
  }
  
  /***
   * Set session token to profile.
   * 
   * @param token
   *          token value to set
   */
  public void setSessionToken(final String token) {
    setPropertyValue(ProfileRepository.SESSION_TOKEN, token);
  }
  
  /***
   * Get session token from profile
   * 
   * @return session token.
   */
  public String getSessionToken() {
    return (String) getPropertyValue(ProfileRepository.SESSION_TOKEN);
  }
  
  /***
   * Verify the token with profile session token
   * 
   * @param token
   *          token to verify
   * @return boolean indicating passed token validity
   */
  public boolean isValidSessionToken(final String token) {
    boolean isValidToken = false;
    if (StringUtilities.isNotEmpty(token)) {
      final String profileSessionToken = getSessionToken();
      isValidToken = StringUtilities.areEqual(token, profileSessionToken);
    }
    return isValidToken;
  }
  
  public List<OutOfStockItemDetails> getOosItems() {
    return oosItems;
  }
  
  public void setOosItems(final List<OutOfStockItemDetails> oosItems) {
    this.oosItems = oosItems;
  }
  
  public Map<String, RichRelevanceProductBean> getOosCartRecommendations() {
    return oosCartRecommendations;
  }
  
  public void setOosCartRecommendations(final Map<String, RichRelevanceProductBean> oosCartRecommendations) {
    this.oosCartRecommendations = oosCartRecommendations;
  }
  
  /**
   * Gets the ab test group assignments.
   * 
   * @return the ab test group assignments
   */
  @SuppressWarnings("unchecked")
  public Map<String, String> getAbTestGroupAssignments() {
    return (Map<String, String>) getPropertyValue("abTestGroupAssignments");
  }
  
  /**
   * Sets the ab test group assignments.
   * 
   * @param abTestGroupAssignments
   *          the ab test group assignments
   */
  public void setAbTestGroupAssignments(final Map<String, String> abTestGroupAssignments) {
    setPropertyValue("abTestGroupAssignments", abTestGroupAssignments);
  }
  
  /**
   * Gets the ab test attribute map.
   * 
   * @return the ab test attribute map
   */
  @SuppressWarnings("unchecked")
  public Map<String, String> getAbTestAttributeMap() {
    return (Map<String, String>) getPropertyValue("abTestAttributeMap");
  }
  
  /**
   * Sets the ab test attribute map.
   * 
   * @param abTestAttributeMap
   *          the ab test attribute map
   */
  public void setAbTestAttributeMap(final Map<String, String> abTestAttributeMap) {
    setPropertyValue("abTestAttributeMap", abTestAttributeMap);
  }
  
  /**
   * 
   * @return geoCity
   */
  public String getGeoCity() {
    return getStringValue(ProfileRepository.GEO_CITY);
  }
  
  /**
   * 
   * @param city
   */
  public void setGeoCity(final String geoCity) {
    setPropertyValue(ProfileRepository.GEO_CITY, geoCity);
  }
  
  /**
   * 
   * @return geoState
   */
  public String getGeoState() {
    return getStringValue(ProfileRepository.GEO_STATE);
  }
  
  /**
   * 
   * @param geoState
   */
  public void setGeoState(final String geoState) {
    setPropertyValue(ProfileRepository.GEO_STATE, geoState);
  }
  
  /**
   * 
   * @return geoZip
   */
  public String getGeoZip() {
    return getStringValue(ProfileRepository.GEO_ZIP);
  }
  
  /**
   * 
   * @param geoZip
   */
  public void setGeoZip(final String geoZip) {
    setPropertyValue(ProfileRepository.GEO_ZIP, geoZip);
  }
  
  /**
   * Gets the associate type from profile
   * 
   * @return the associateType
   */
  public String getAssociateType() {
    return (String) getPropertyValue(EmployeeDiscountConstants.ASSOCIATE_TYPE);
  }
  
  /**
   * @param associateType
   *          the associateType to set
   */
  public void setAssociateType(final String associateType) {
    setPropertyValue(EmployeeDiscountConstants.ASSOCIATE_TYPE, associateType);
  }
 

  /**
   * @return the accountRecentlyRegistered
   */
  public boolean isAccountRecentlyRegistered() {
    return accountRecentlyRegistered;
  }
  
  /**
   * @param accountRecentlyRegistered
   *          the accountRecentlyRegistered to set
   */
  public void setAccountRecentlyRegistered(final boolean accountRecentlyRegistered) {
    this.accountRecentlyRegistered = accountRecentlyRegistered;
  }

  public String getPersAllHiddenCatIds() {
	  return persAllHiddenCatIds;
  }

  public void setPersAllHiddenCatIds(String persAllHiddenCatIds) {
	  this.persAllHiddenCatIds = persAllHiddenCatIds;
  }

/**
 * @return the recentlyCancelledItems
 */
public List<RecentlyChangedCommerceItem> getRecentlyCancelledItems() {
	if (NmoUtils.isEmptyCollection(recentlyCancelledItems)) {
		recentlyCancelledItems = new ArrayList<RecentlyChangedCommerceItem>();
    }
	return recentlyCancelledItems;
}

/**
 * @param recentlyCancelledItems the recentlyCancelledItems to set
 */
public void setRecentlyCancelledItems(
		List<RecentlyChangedCommerceItem> recentlyCancelledItems) {
	this.recentlyCancelledItems = recentlyCancelledItems;
}
  
  /**
   * Checks if is two click checkout enabled.
   * 
   * @return true, if is two click checkout enabled
   */
  public boolean isTwoClickCheckoutEnabled() {
    return NmoUtils.getBooleanPropertyValue(getDataSource(), ProfileRepository.TWO_CLICK_CHECK_ENABLED);
  }
  
  /**
   * Sets two click checkout enabled.
   * 
   * @param twoClickCheckoutEnabled
   *          the two click checkout enabled
   */
  public void setTwoClickCheckoutEnabled(final boolean twoClickCheckoutEnabled) {
    setPropertyValue(ProfileRepository.TWO_CLICK_CHECK_ENABLED, twoClickCheckoutEnabled);
  }
  
  /**
   * Checks if all details for default credit card are available.
   * 
   * @return true, all credit card details available.
   * */
  public boolean isDefaultCreditCardAuthorized() {
    boolean isDefaultCreditCardAuthorized = false;
    NMCreditCard defaultCreditCard = getDefaultCreditCard(); 
    if (null != defaultCreditCard) {
      if (StringUtilities.isNotBlank(defaultCreditCard.getCreditCardNumber()) && StringUtilities.isNotBlank(defaultCreditCard.getCreditCardType())) {
        if ((StringUtilities.isNotBlank(defaultCreditCard.getExpirationMonth()) && StringUtilities.isNotBlank(defaultCreditCard.getExpirationYear()) && defaultCreditCard.getCidAuthorized())
                || (defaultCreditCard.getCreditCardType().equalsIgnoreCase(TwoClickCheckoutConstants.NEIMAN_MARCUS_CARD) || defaultCreditCard.getCreditCardType().equalsIgnoreCase(
                        TwoClickCheckoutConstants.BERGDORF_GOODMAN_CARD))) {
          isDefaultCreditCardAuthorized = true;
        }
      }
    }
    return isDefaultCreditCardAuthorized;
  }
  
  /** Flag for auto subscription for all designer alerts.
   * @return true if subscribed.
   */
 public boolean isAutoDesignerAlerts() {
    if ((getPropertyValue(ProfileRepository.AUTO_DESIGNER_ALERTS) == null) || getPropertyValue(ProfileRepository.AUTO_DESIGNER_ALERTS).equals(new Integer(0))) {
      return false;
    } else {
      return true;
    }
  }
  
  /** Setter for auto subscription of all designer alerts. Its called only when the user opt for subscription.
   * 
   */
  public void setAutoDesignerAlerts(){
    setPropertyValue(ProfileRepository.AUTO_DESIGNER_ALERTS, new Integer(1));
  }

}
