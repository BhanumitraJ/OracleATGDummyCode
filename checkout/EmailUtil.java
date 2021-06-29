package com.nm.commerce.checkout;

import java.io.IOException;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.Vector;

import atg.commerce.order.Order;
import atg.nucleus.Nucleus;
import atg.repository.Repository;
import atg.repository.RepositoryException;
import atg.repository.RepositoryItem;
import atg.userprofiling.email.HtmlContentProcessor;
import atg.userprofiling.email.TemplateEmailException;
import atg.userprofiling.email.TemplateEmailInfoImpl;
import atg.userprofiling.email.TemplateEmailSender;

import com.nm.ajax.checkout.utils.ComponentUtils;
import com.nm.ajax.checkout.utils.ConfigurationUtils;
import com.nm.ajax.checkout.utils.RepositoryUtils;
import com.nm.collections.NMPromotion;
import com.nm.commerce.NMCommerceItem;
import com.nm.commerce.NMProfile;
import com.nm.commerce.promotion.NMOrderImpl;
import com.nm.commerce.promotion.PromotionAfterPromotion;
import com.nm.commerce.promotion.rulesBased.Promotion;
import com.nm.commerce.promotion.rulesBased.PromotionElement;
import com.nm.components.CommonComponentHelper;
import com.nm.formhandler.ShoppingCartHandler;
import com.nm.services.CheetahMailService;
import com.nm.utils.PromotionsHelper;
import com.nm.utils.SystemSpecs;

/**
 * Package access - outside of this package, methods should be accessed through CheckoutAPI.
 */
/* package */class EmailUtil {
  
  private static EmailUtil INSTANCE;
  
  // private constructor enforces singleton behavior
  private EmailUtil() {}
  
  public static synchronized EmailUtil getInstance() {
    INSTANCE = (INSTANCE == null) ? new EmailUtil() : INSTANCE;
    return INSTANCE;
  }
  
  /**
   * sendPapEmail does several steps 1. checks order for awarded Promotions 2. if so, then checks for PAP on those Promotions 3. if PAP is an emailValidated Promo, will add the orders email and PAP
   * promoCode to NM_Promotions_email_Validation table 4. if PAP, then sends Cheetahmail the emailaddress and promoCode
   */
  public void sendPapEmail(final Order order) {
    NMOrderImpl lastOrder = (NMOrderImpl) order;
    String foundPromoCode = null;
    String foundPromoKey = null;
    boolean newEntry = false;
    
    PromotionsHelper promotionsHelper = CheckoutComponents.getPromotionsHelper();
    
    try {
      // First part is included in CheetahMailService.properties file getpapEmbTriggerURL()
      // /https://trig.neimanmarcusemail.com/ebm/ebmtrigger1?aid=32760565&eid=70339&test=1&html=1
      // &email=cmedina@cheetahmail.com&CREATIVE=2&COUPON_CODE=12345
      
      if (lastOrder == null || lastOrder.getPropertyValue("homeEmail") == null) {
        if (CommonComponentHelper.getLogger().isLoggingDebug()) {
          String msg = "ORDERUTILS sendPapEmail error - could not retrieve homeEmail. [lastOrder=" + lastOrder + "]";
          if (lastOrder != null) {
            msg = msg + "[homeEmail=" + lastOrder.getPropertyValue("homeEmail") + "]";
          }
          CommonComponentHelper.getLogger().logDebug(msg);
        }
        return;
      }
      
      String orderEmail = lastOrder.getPropertyValue("homeEmail").toString();
      String papCode = "";
      Collection awardedPromotions = lastOrder.getAwardedPromotions();
      Iterator awardedPromotionsIterator = awardedPromotions.iterator();
      
      while (awardedPromotionsIterator.hasNext()) {
        NMPromotion promotion = (NMPromotion) awardedPromotionsIterator.next();
        
        // need to determine if this promotion is a Rulesbasedpromotion or not
        int promotionClass = promotion.getPromotionClass();
        
        if (promotionClass == NMPromotion.RULE_BASED) {
          // if rules based, then get promocodes List
          Promotion rbPromotion = (Promotion) promotion;
          // take each promocode from possibly stacked RBpromos and get their promoCode.
          Iterator rbIterator = rbPromotion.getPromotionElements().iterator();
          while (rbIterator.hasNext()) {
            PromotionElement promoElement = (PromotionElement) rbIterator.next();
            if (promoElement != null) {
              foundPromoKey = promoElement.getId();
              sendPapEmail(promotionsHelper, orderEmail, foundPromoKey);
            }
          }
        } else {
          // This is a standard, non rules based promotion, might be comma delimited
          foundPromoCode = promotion.getCode();
          StringTokenizer st = new StringTokenizer(foundPromoCode, ",");
          while (st.hasMoreElements()) {
            String onePapCode = (String) st.nextElement();
            sendPapEmail(promotionsHelper, orderEmail, foundPromoCode);
          }
        }
      }
    } catch (Exception e) {
      CommonComponentHelper.getLogger().error("-------->>>>>>  ORDERUTILS sendPapEmail error ", e);
    }
  }
  
  /**
   * 
   * @param promotionsHelper
   * @param orderEmail
   * @param initialPromotionKey
   * @param service
   * @throws IOException
   * @throws RepositoryException
   */
  private void sendPapEmail(PromotionsHelper promotionsHelper, String orderEmail, String initialPromotionKey) throws IOException, RepositoryException {
    PromotionAfterPromotion promotionAfterPromotion = promotionsHelper.getPromotionAfterPromotion(initialPromotionKey);
    CheckoutConfig config = CheckoutComponents.getConfig();
    
    if (promotionAfterPromotion != null) {
      // now take linkedPromotion and lookup it's PromoCode using CartUtils.getPromotionFromPromoId(linkedPromotion)
      NMPromotion lPromos = promotionsHelper.getPromotionFromPromoId(promotionAfterPromotion.getLinkedPromotion());
      
      if (lPromos != null) {
        // Get it's PromoCode if it exists
        String papCode = lPromos.getPromoCodes();
        if (papCode != null) {
          if (lPromos.isPromotionActive()) {
            // check if this PAP is an emailVerified type
            boolean newEntry = false;
            if (lPromos.requiresEmailValidation()) {
              newEntry = promotionsHelper.verifyPAPCodeHasEV(orderEmail, papCode, promotionAfterPromotion.getLimitEntries());
            }
            // send Cheetahmail the PAP
            if (config.isLoggingDebug()) {
              config.logDebug("sendPapEmail papCode: " + papCode + " newEntry: " + newEntry + " orderEmail: " + orderEmail);
            }
            
            if (papCode != null && papCode.length() > 0 && newEntry) {
              CheetahMailService cheetahMailService = (CheetahMailService) Nucleus.getGlobalNucleus().resolveName("/nm/services/CheetahMailService");
              cheetahMailService.sendPaPEmail(orderEmail, papCode);
            }
          }
        }
      } else {
        if (config.isLoggingDebug()) {
          config.logDebug("sendPapEmail could not find promotion for " + promotionAfterPromotion.getLinkedPromotion());
        }
      }
    } else {
      if (config.isLoggingDebug()) {
        config.logDebug("sendPapEmail could not find PAP for " + initialPromotionKey);
      }
    }
  }
  
  /**
   * Sends the user a confirmation email regarding their subscription order Once an order is placed, this will iterate through the lastOrder, looking for any cmos_code of SUBS. If it finds SUBS, then
   * it will determine which email to send based on the product_id of the item. Three possible product_ids for a subscription email: cprodsub0001 cprodsub0002 cprodsub0003 Each id will have a
   * refreshable html asset stored in their respective folder: /category/subscriptions/cprodsub0001.html /category/subscriptions/cprodsub0002.html /category/subscriptions/cprodsub0003.html These are
   * located in the ShoppingCartModifier.properties file
   */
  public void sendSubscriptionConfEmail(ShoppingCartHandler cart, NMProfile profile) {
    final SystemSpecs systemSpecs = ComponentUtils.getInstance().getSystemSpecs();
    final Repository systemSpecsRepos = RepositoryUtils.getInstance().getSystemSpecsRepository();
    final ConfigurationUtils config = ConfigurationUtils.getInstance();
    NMOrderImpl order = cart.getNMOrder();
    List ciList = order.getCommerceItems();
    Iterator ciListIterator = ciList.iterator();
    
    while (ciListIterator.hasNext()) {
      NMCommerceItem nmci = (NMCommerceItem) ciListIterator.next();
      
      String ciId = nmci.getCmosCatalogId();
      if (ciId.equalsIgnoreCase("SUBS") && ciId != null) {
        String customerEmail = (String) profile.getPropertyValue("email");
        String subscrEmailSubject = "Subscription Confirmation";
        String subscrEmailAddress = null;
        String subscrMessageFromText = null;
        String prodId = (String) nmci.getRepositoryItem().getPropertyValue("productId");
        String tempSubURL = null;
        
        try {
          if (prodId.equalsIgnoreCase("cprodsub001") && prodId != null) {
            RepositoryItem item = systemSpecsRepos.getItem("subscremailsubject1", "NMSystemSpecs");
            if (item != null) {
              subscrEmailSubject = (String) item.getPropertyValue("value");
              tempSubURL = config.getTemplateSub1URL();
            }
          }
          if (prodId.equalsIgnoreCase("cprodsub002") && prodId != null) {
            RepositoryItem item = systemSpecsRepos.getItem("subscremailsubject2", "NMSystemSpecs");
            if (item != null) {
              subscrEmailSubject = (String) item.getPropertyValue("value");
              tempSubURL = config.getTemplateSub2URL();
            }
          }
          if (prodId.equalsIgnoreCase("cprodsub003") && prodId != null) {
            RepositoryItem item = systemSpecsRepos.getItem("subscremailsubject3", "NMSystemSpecs");
            if (item != null) {
              subscrEmailSubject = (String) item.getPropertyValue("value");
              tempSubURL = config.getTemplateSub3URL();
            }
          }
          
          RepositoryItem item = systemSpecsRepos.getItem("subscremailaddress", "NMSystemSpecs");
          if (item != null) {
            subscrEmailAddress = (String) item.getPropertyValue("value");
          }
          
          item = systemSpecsRepos.getItem("ordermessagefromtext", "NMSystemSpecs");
          if (item != null) {
            subscrMessageFromText = (String) item.getPropertyValue("value");
          }
          if (subscrMessageFromText != null) {
            subscrMessageFromText = subscrMessageFromText + " <" + systemSpecs.getEmailAddress() + ">";
          } else {
            subscrMessageFromText = systemSpecs.getEmailAddress();
          }
          
          item = systemSpecsRepos.getItem("developmentEmail", "NMSystemSpecs");
          if (item != null && ((String) item.getPropertyValue("value")).equalsIgnoreCase("true")) {
            customerEmail = systemSpecs.getEmailAddress();
          }
          
        } catch (RepositoryException re) {
          CheckoutComponents.getConfig().logError("Unable to read ServiceLevelRepository " + re, re);
        }
        
        final TemplateEmailInfoImpl emailInfo = new TemplateEmailInfoImpl();
        Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("ShoppingCartModifier", cart);
        HtmlContentProcessor contentProcessor = new HtmlContentProcessor();
        contentProcessor.setSendAsText(true);
        
        emailInfo.setContentProcessor(contentProcessor);
        emailInfo.setTemplateParameters(parameters);
        emailInfo.setTemplateURL(tempSubURL);
        emailInfo.setMessageSubject(subscrEmailSubject);
        emailInfo.setMessageTo(customerEmail);
        emailInfo.setMessageFrom(subscrMessageFromText);
        emailInfo.setMessageReplyTo(systemSpecs.getEmailAddress());
        
        Vector pItemVec = new Vector();
        pItemVec.addElement(customerEmail);
        Enumeration elements = pItemVec.elements();
        
        final TemplateEmailSender emailSender = (TemplateEmailSender) Nucleus.getGlobalNucleus().resolveName("/atg/userprofiling/email/TemplateEmailSender");
        try {
          // send subscription message to customer
          emailSender.sendEmailMessage(emailInfo, elements, false, false);
          
          if (subscrEmailAddress != null && subscrEmailAddress.length() > 0) {
            emailInfo.setMessageTo(subscrEmailAddress);
            emailInfo.setMessageFrom(customerEmail);
            emailInfo.setMessageReplyTo(customerEmail);
            pItemVec = new Vector();
            pItemVec.addElement(subscrEmailAddress);
            elements = pItemVec.elements();
            
            // Send a copy to customercare
            emailSender.sendEmailMessage(emailInfo, elements, false, false);
          }
        } catch (TemplateEmailException tee) {
          CheckoutComponents.getConfig().logError("Error while sending subscription confirmation email for order with ID " + order.getId(), tee);
        }
      }
    }
  }
}
