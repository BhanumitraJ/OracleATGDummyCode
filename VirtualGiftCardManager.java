package com.nm.commerce;

import atg.repository.*;
import atg.repository.rql.*;
import atg.nucleus.GenericService;
import java.util.*;
import atg.userprofiling.email.*;
import java.net.*;
import atg.nucleus.*;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.sql.Timestamp;

import com.nm.utils.*;

public class VirtualGiftCardManager extends GenericService {
  
  private static final SimpleDateFormat yformat = new SimpleDateFormat("yyyy-MM-dd 00:00:00");
  private static final SimpleDateFormat wformat = new SimpleDateFormat("MM/dd/yyyy");
  
  private MutableRepository virtualGiftCardRepository;
  private SystemSpecs systemSpecs;
  private TemplateEmailSender templateEmailSender;
  private SystemNameLookup sysNameLookup;
  private String vgcPickPage;
  private String vgcFollowUpPage1;
  private String vgcFollowUpPage2;
  
  protected static VirtualGiftCardManager virtualGiftCardManager = (VirtualGiftCardManager) Nucleus.getGlobalNucleus().resolveName("/nm/commerce/VirtualGiftCardManager");
  
  public static final String SI_EMAIL = "VGC00E";
  public static final String SI_NAME_TO = "VGC00T";
  public static final String SI_NAME_FROM = "VGC00F";
  public static final String SI_MESSAGE = "VGC00M";
  public static final String SI_STYLE = "VGC00S";
  public static final String[] VGC_SI_CODES = {SI_EMAIL , SI_NAME_TO , SI_NAME_FROM , SI_MESSAGE , SI_STYLE};
  
  public static String VGC_REPOSITORY_DESCRIPTOR = "virtualGiftCard";
  public static String VGC_EVENT_REPOSITORY_DESCRIPTOR = "virtualGiftCardEvent";
  
  public VirtualGiftCardManager() {
    if (virtualGiftCardManager == null) virtualGiftCardManager = this;
  };
  
  public void setVgcPickupPage(String value) {
    vgcPickPage = value;
  }
  
  public String getVgcPickupPage() {
    return vgcPickPage;
  }
  
  public void setVgcFollowUpPage1(String value) {
    vgcFollowUpPage1 = value;
  }
  
  public String getVgcFollowUpPage1() {
    return vgcFollowUpPage1;
  }
  
  public void setVgcFollowUpPage2(String value) {
    vgcFollowUpPage2 = value;
  }
  
  public String getVgcFollowUpPage2() {
    return vgcFollowUpPage2;
  }
  
  public void setVirtualGiftCardRepository(MutableRepository virtualGiftCardRepository) {
    this.virtualGiftCardRepository = virtualGiftCardRepository;
  }
  
  public MutableRepository getVirtualGiftCardRepository() {
    return virtualGiftCardRepository;
  }
  
  public static VirtualGiftCardManager getVirtualGiftCardManager() {
    return virtualGiftCardManager;
  }
  
  public MutableRepositoryItem createVirtualGiftCardRepositoryItem() throws RepositoryException {
    MutableRepositoryItem virtualGiftCard = getVirtualGiftCardRepository().createItem(VGC_REPOSITORY_DESCRIPTOR);
    addVirtualGiftCard(virtualGiftCard);
    return virtualGiftCard;
  }
  
  public MutableRepositoryItem createVirtualGiftCardEventRepositoryItem() throws RepositoryException {
    MutableRepositoryItem virtualGiftCardEvent = getVirtualGiftCardRepository().createItem(VGC_EVENT_REPOSITORY_DESCRIPTOR);
    addVirtualGiftCardEvent(virtualGiftCardEvent);
    return virtualGiftCardEvent;
  }
  
  public void addVirtualGiftCard(VirtualGiftCard virtualGiftCard) throws RepositoryException {
    getVirtualGiftCardRepository().addItem(virtualGiftCard.getRepositoryItem());
  }
  
  public void addVirtualGiftCard(MutableRepositoryItem virtualGiftCardRepositoryItem) throws RepositoryException {
    getVirtualGiftCardRepository().addItem(virtualGiftCardRepositoryItem);
  }
  
  public void addVirtualGiftCardEvent(VirtualGiftCardEvent virtualGiftCardEvent) throws RepositoryException {
    getVirtualGiftCardRepository().addItem(virtualGiftCardEvent.getRepositoryItem());
  }
  
  public void addVirtualGiftCardEvent(MutableRepositoryItem virtualGiftCardEventRepositoryItem) throws RepositoryException {
    getVirtualGiftCardRepository().addItem(virtualGiftCardEventRepositoryItem);
  }
  
  public void removeVirtualGiftCard(VirtualGiftCard virtualGiftCard) throws RepositoryException {
    getVirtualGiftCardRepository().removeItem(virtualGiftCard.getRepositoryItem().getRepositoryId(), VGC_REPOSITORY_DESCRIPTOR);
  }
  
  public VirtualGiftCard getVirtualGiftCard(String repositoryId) throws RepositoryException {
    return new VirtualGiftCard(getVirtualGiftCardRepository().getItemForUpdate(repositoryId, VGC_REPOSITORY_DESCRIPTOR));
  }
  
  public void setSystemSpecs(SystemSpecs systemSpecs) {
    this.systemSpecs = systemSpecs;
  }
  
  public SystemSpecs getSystemSpecs() {
    return systemSpecs;
  }
  
  public void setSystemNameLookup(SystemNameLookup system) {
    this.sysNameLookup = system;
  }
  
  public SystemNameLookup getSystemNameLookup() {
    return sysNameLookup;
  }
  
  public void setTemplateEmailSender(TemplateEmailSender templateEmailSender) {
    this.templateEmailSender = templateEmailSender;
  }
  
  public TemplateEmailSender getTemplateEmailSender() {
    return templateEmailSender;
  }
  
  public boolean sendVirtualGiftCardEmail(String repositoryId, String eventType) {
    try {
      TemplateEmailInfoImpl emailInfo = new TemplateEmailInfoImpl();
      HtmlContentProcessor contentProcessor = new HtmlContentProcessor();
      
      VirtualGiftCard vgcToSend = getVirtualGiftCard(repositoryId);
      // mod check? if fails send to Log?
      
      if (vgcToSend.getGiftCardNumber() != null && !vgcToSend.getGiftCardNumber().equals("") && vgcToSend.getStatus() != null && vgcToSend.getStatus().trim().equals(VirtualGiftCard.VGC_ACTIVE)) {
        contentProcessor.setSendAsText(false);
        contentProcessor.setSendAsHtml(true);
        String systemTopicName = getSystemNameLookup().translateVGC(getSystemSpecs().getProductionSystemCode());
        
        emailInfo.setTemplateURL("/category/VGC/templates/styleHtml" + vgcToSend.getStyleCode() + ".jhtml");
        emailInfo.setMessageTo(vgcToSend.getRecipientEmail().toLowerCase());
        emailInfo.setMessageFrom(getSystemSpecs().getVirtualGiftCardEmailAddress().toLowerCase());
        // emailInfo.setMessageSubject( systemTopicName + " Virtual Gift Card from " + vgcToSend.getNameFrom() + ".");
        emailInfo.setMessageSubject(vgcToSend.getNameFrom() + " Has Sent You a " + systemTopicName + " Virtual Gift Card.");
        emailInfo.setContentProcessor(contentProcessor);
        
        HashMap parameters = new HashMap();
        String retrievalURL = "";
        String currentHttpsProtocol = getSystemSpecs().getHttpsProtocol();
        String currentHttpsHost = getSystemSpecs().getHttpsHost();
        String currentHttpsPort = String.valueOf(getSystemSpecs().getHttpsPort());
        String pickupPage = getVgcPickupPage();
        
        String vid = vgcToSend.getRecipientEmail() + "~" + vgcToSend.getVgcId();
        String encryptedVid = EncryptDecrypt.encrypt(vid);
        String encodedVid = URLEncoder.encode(encryptedVid, "ISO-8859-1");
        
        if (currentHttpsPort.equals("443")) {
          retrievalURL = currentHttpsProtocol + "://" + currentHttpsHost + pickupPage + "?vid=" + encodedVid;
        } else {
          retrievalURL = currentHttpsProtocol + "://" + currentHttpsHost + ":" + currentHttpsPort + pickupPage + "?vid=" + encodedVid;
        }
        parameters.put("nameTo", vgcToSend.getNameTo());
        parameters.put("nameFrom", vgcToSend.getNameFrom());
        parameters.put("message", vgcToSend.getMessage());
        parameters.put("giftCardAmount", vgcToSend.getGiftCardAmount());
        parameters.put("retrievalURL", retrievalURL);
        
        emailInfo.setTemplateParameters(parameters);
        
        // send email to the customer
        Vector pItemVec = new Vector();
        pItemVec.addElement(vgcToSend.getRecipientEmail().toLowerCase());
        Enumeration enu = pItemVec.elements();
        
        if (this.isLoggingDebug()) {
          StringBuffer sb = new StringBuffer("TemplateEmailSender Info - VGC: \n");
          sb.append("SiteHttpServerName - " + getTemplateEmailSender().getSiteHttpServerName() + "\n");
          sb.append("SiteHttpServerPort - " + getTemplateEmailSender().getSiteHttpServerPort() + "\n");
          sb.append("ApplicationPrefix - " + getTemplateEmailSender().getApplicationPrefix() + "\n");
          sb.append("DynamoInitSessionURL - " + getTemplateEmailSender().getDynamoInitSessionURL() + "\n");
          this.logDebug(sb.toString());
        }
        
        getTemplateEmailSender().sendEmailMessage(emailInfo, enu, false, false);
        
        VirtualGiftCardEvent vgcEvent = new VirtualGiftCardEvent();
        vgcEvent.setEventType(eventType);
        String subdata = "email sent to: " + vgcToSend.getRecipientEmail();
        subdata += "; last 8 chars of URL[" + retrievalURL.substring(retrievalURL.length() - 8) + "]";
        vgcEvent.setData(subdata);
        vgcToSend.addEvent(vgcEvent);
      } else {
        return false;
      }
    } catch (RepositoryException e) {
      e.printStackTrace();
      return false;
    } catch (Exception e) {
      e.printStackTrace();
      return false;
    }
    
    return true;
    
  }
  
  public boolean sendVirtualGiftCardFollowUpEmail(String repositoryId, String eventType) {
    try {
      TemplateEmailInfoImpl emailInfo = new TemplateEmailInfoImpl();
      HtmlContentProcessor contentProcessor = new HtmlContentProcessor();
      
      VirtualGiftCard vgcToSend = getVirtualGiftCard(repositoryId);
      // mod check? if fails send to Log?
      String orderNumber = vgcToSend.getOrderId();
      
      if (orderNumber.startsWith("W")) {
        String orderId = vgcToSend.getOrderId();
        orderNumber = orderId.substring(2);
      }
      
      Repository repository = (Repository) resolveName("/atg/commerce/order/OrderRepository");
      RepositoryView view = repository.getView("order");
      Object params[] = new Object[1];
      RqlStatement statement = RqlStatement.parseRqlStatement("id =?0");
      
      params[0] = orderNumber;
      MutableRepositoryItem[] items = (MutableRepositoryItem[]) statement.executeQuery(view, params);
      
      if (items != null && items.length > 0) {
        String homeEmail = (String) items[0].getPropertyValue("homeEmail");
        
        if (homeEmail.equalsIgnoreCase(vgcToSend.getRecipientEmail())) {
          emailInfo.setTemplateURL(getVgcFollowUpPage1());
        } else {
          emailInfo.setTemplateURL(getVgcFollowUpPage2());
        }
      } else {
        return false;
      }
      
      contentProcessor.setSendAsText(true);
      contentProcessor.setSendAsHtml(false);
      
      String systemTopicName = getSystemNameLookup().translateVGC(getSystemSpecs().getProductionSystemCode());
      emailInfo.setMessageTo(vgcToSend.getRecipientEmail().toLowerCase());
      emailInfo.setMessageFrom(getSystemSpecs().getVirtualGiftCardEmailAddress().toLowerCase());
      emailInfo.setContentProcessor(contentProcessor);
      emailInfo.setMessageSubject(systemTopicName + " Virtual Gift Card from " + vgcToSend.getNameFrom() + ".");
      
      String emailDomainName = systemTopicName;
      
      int spaceIndex = systemTopicName.indexOf(" ");
      if (spaceIndex > 0) {
        String firstString = systemTopicName.substring(0, spaceIndex);
        String secondString = systemTopicName.substring(spaceIndex + 1);
        emailDomainName = firstString.concat(secondString);
      }
      
      HashMap parameters = new HashMap();
      
      Date vDate = vgcToSend.getOrderPlacedTimeStamp();
      String vgcDate = wformat.format(vDate);
      parameters.put("nameTo", vgcToSend.getRecipientEmail());
      parameters.put("nameFrom", vgcToSend.getNameFrom());
      parameters.put("message", vgcToSend.getMessage());
      parameters.put("giftCardAmount", vgcToSend.getGiftCardAmount());
      parameters.put("divisionName", systemTopicName);
      parameters.put("domainName", emailDomainName);
      parameters.put("vgcDate", vgcDate);
      
      emailInfo.setTemplateParameters(parameters);
      
      // send email to the customer
      Vector pItemVec = new Vector();
      pItemVec.addElement(vgcToSend.getRecipientEmail().toLowerCase());
      Enumeration enu = pItemVec.elements();
      getTemplateEmailSender().sendEmailMessage(emailInfo, enu, false, false);
      
      VirtualGiftCardEvent vgcEvent = new VirtualGiftCardEvent();
      vgcEvent.setEventType(eventType);
      String subdata = "email sent to: " + vgcToSend.getRecipientEmail();
      vgcEvent.setData(subdata);
      vgcToSend.addEvent(vgcEvent);
      
    } catch (RepositoryException e) {
      e.printStackTrace();
      return false;
    } catch (Exception e) {
      e.printStackTrace();
      return false;
    }
    
    return true;
    
  }
  
  public List lookupByOrderId(String orderId) throws RepositoryException {
    MutableRepository repository = getVirtualGiftCardRepository();
    RepositoryView view = repository.getView("virtualGiftCard");
    RqlStatement statement = RqlStatement.parseRqlStatement("orderId CONTAINS ?0");
    Object params[] = new Object[1];
    
    if (orderId != null) orderId = orderId.toUpperCase();
    params[0] = orderId;
    MutableRepositoryItem[] items = (MutableRepositoryItem[]) statement.executeQuery(view, params);
    
    if (items != null && items.length > 0) {
      ArrayList returnValues = new ArrayList();
      for (int i = 0; i < items.length; i++) {
        returnValues.add(new VirtualGiftCard(items[i]));
      }
      return returnValues;
    } else {
      return new ArrayList();
    }
  }
  
  public VirtualGiftCard lookupByCommerceItemId(String commerceItemId) throws RepositoryException {
    MutableRepository repository = getVirtualGiftCardRepository();
    RepositoryView view = repository.getView("virtualGiftCard");
    RqlStatement statement = RqlStatement.parseRqlStatement("commerceItemId = ?0");
    Object params[] = new Object[1];
    
    if (commerceItemId != null) commerceItemId = commerceItemId.toUpperCase();
    params[0] = commerceItemId;
    MutableRepositoryItem[] items = (MutableRepositoryItem[]) statement.executeQuery(view, params);
    if (items != null && items.length > 1) {
      throw new RepositoryException("Duplicate Commerce Item IDs detected with ID-->" + commerceItemId + "<---");
    }
    if (items != null && items.length > 0) {
      return new VirtualGiftCard(items[0]);
    } else {
      return null;
    }
  }
  
  public List lookupByEmail(String email) throws RepositoryException {
    MutableRepository repository = getVirtualGiftCardRepository();
    RepositoryView view = repository.getView("virtualGiftCard");
    RqlStatement statement = RqlStatement.parseRqlStatement("recipientEmail CONTAINS ?0");
    Object params[] = new Object[1];
    
    if (email != null) email = email.toUpperCase();
    params[0] = email;
    MutableRepositoryItem[] items = (MutableRepositoryItem[]) statement.executeQuery(view, params);
    
    if (items != null && items.length > 0) {
      ArrayList returnValues = new ArrayList();
      for (int i = 0; i < items.length; i++) {
        returnValues.add(new VirtualGiftCard(items[i]));
      }
      return returnValues;
    } else {
      return new ArrayList();
    }
  }
  
  public Timestamp convertDateToFormat(Date firstDate) {
    
    String simpleDate = yformat.format(firstDate);
    Timestamp timeStamp = Timestamp.valueOf(simpleDate);
    
    return timeStamp;
    
  }
  
  public List lookupByDate(Date dateFrom, Date dateTo, String eventType) throws RepositoryException {
    MutableRepository repository = getVirtualGiftCardRepository();
    RepositoryView view = repository.getView("virtualGiftCard");
    RqlStatement statement = RqlStatement.parseRqlStatement("events INCLUDES ITEM ( eventTimeStamp > ?0 and eventTimeStamp < ?1 and eventType contains ?2)");
    Object params[] = new Object[3];
    
    params[0] = convertDateToFormat(dateFrom);
    params[1] = convertDateToFormat(dateTo);
    params[2] = eventType;
    
    MutableRepositoryItem[] items = (MutableRepositoryItem[]) statement.executeQuery(view, params);
    
    if (items != null && items.length > 0) {
      ArrayList returnValues = new ArrayList();
      for (int i = 0; i < items.length; i++) {
        returnValues.add(new VirtualGiftCard(items[i]));
      }
      return returnValues;
    } else {
      return new ArrayList();
    }
  }
  
  public String convertPickedUpFlg(String pickedUp) {
    String pickedUpFlg = "";
    
    if (pickedUp.equals("false")) {
      pickedUpFlg = "0";
    } else {
      pickedUpFlg = "1";
    }
    return pickedUpFlg;
  }
  
  public List lookupByPickedUp(String pickedUp) throws RepositoryException {
    MutableRepository repository = getVirtualGiftCardRepository();
    RepositoryView view = repository.getView("virtualGiftCard");
    RqlStatement statement = RqlStatement.parseRqlStatement("flgPickedUp CONTAINS ?0");
    Object params[] = new Object[1];
    
    params[0] = convertPickedUpFlg(pickedUp);
    MutableRepositoryItem[] items = (MutableRepositoryItem[]) statement.executeQuery(view, params);
    
    if (items != null && items.length > 0) {
      ArrayList returnValues = new ArrayList();
      for (int i = 0; i < items.length; i++) {
        returnValues.add(new VirtualGiftCard(items[i]));
      }
      return returnValues;
    } else {
      return new ArrayList();
    }
  }
  
  public List lookupByDateAndPickedUp(String pickedUp, Date dateFrom, Date dateTo, String eventType) throws RepositoryException {
    MutableRepository repository = getVirtualGiftCardRepository();
    RepositoryView view = repository.getView("virtualGiftCard");
    RqlStatement statement = RqlStatement.parseRqlStatement("flgPickedUp CONTAINS ?0 AND events INCLUDES ITEM ( eventTimeStamp > ?1 and eventTimeStamp < ?2 and  eventType contains ?3)");
    Object params[] = new Object[4];
    
    params[0] = convertPickedUpFlg(pickedUp);
    params[1] = convertDateToFormat(dateFrom);
    params[2] = convertDateToFormat(dateTo);
    params[3] = eventType;
    MutableRepositoryItem[] items = (MutableRepositoryItem[]) statement.executeQuery(view, params);
    
    if (items != null && items.length > 0) {
      ArrayList returnValues = new ArrayList();
      for (int i = 0; i < items.length; i++) {
        returnValues.add(new VirtualGiftCard(items[i]));
      }
      return returnValues;
    } else {
      return new ArrayList();
    }
  }
  
  public List lookupByOrderIdAndDate(String orderId, Date dateFrom, Date dateTo, String eventType) throws RepositoryException {
    MutableRepository repository = getVirtualGiftCardRepository();
    RepositoryView view = repository.getView("virtualGiftCard");
    RqlStatement statement = RqlStatement.parseRqlStatement("orderId CONTAINS ?0 AND events INCLUDES ITEM ( eventTimeStamp > ?1 and eventTimeStamp < ?2 and  eventType contains ?3)");
    Object params[] = new Object[4];
    
    params[0] = orderId;
    params[1] = convertDateToFormat(dateFrom);
    params[2] = convertDateToFormat(dateTo);
    params[3] = eventType;
    MutableRepositoryItem[] items = (MutableRepositoryItem[]) statement.executeQuery(view, params);
    
    if (items != null && items.length > 0) {
      ArrayList returnValues = new ArrayList();
      for (int i = 0; i < items.length; i++) {
        returnValues.add(new VirtualGiftCard(items[i]));
      }
      return returnValues;
    } else {
      return new ArrayList();
    }
  }
  
  public List lookupByOrderIdAndDateAndPickedUp(String orderId, Date dateFrom, Date dateTo, String pickedUp, String eventType) throws RepositoryException {
    MutableRepository repository = getVirtualGiftCardRepository();
    RepositoryView view = repository.getView("virtualGiftCard");
    RqlStatement statement =
            RqlStatement.parseRqlStatement("orderId CONTAINS ?0 AND events INCLUDES ITEM ( eventTimeStamp > ?1 and eventTimeStamp < ?2 and  eventType contains ?4) AND flgPickedUp CONTAINS ?3");
    Object params[] = new Object[5];
    
    params[0] = orderId;
    params[1] = convertDateToFormat(dateFrom);
    params[2] = convertDateToFormat(dateTo);
    params[3] = convertPickedUpFlg(pickedUp);
    params[4] = eventType;
    MutableRepositoryItem[] items = (MutableRepositoryItem[]) statement.executeQuery(view, params);
    
    if (items != null && items.length > 0) {
      ArrayList returnValues = new ArrayList();
      for (int i = 0; i < items.length; i++) {
        returnValues.add(new VirtualGiftCard(items[i]));
      }
      return returnValues;
    } else {
      return new ArrayList();
    }
  }
  
  public List lookupByEmailAndOrderId(String email, String orderId) throws RepositoryException {
    MutableRepository repository = getVirtualGiftCardRepository();
    RepositoryView view = repository.getView("virtualGiftCard");
    RqlStatement statement = RqlStatement.parseRqlStatement("recipientEmail CONTAINS ?0 AND orderId CONTAINS ?1");
    Object params[] = new Object[2];
    
    params[0] = email;
    params[1] = orderId;
    MutableRepositoryItem[] items = (MutableRepositoryItem[]) statement.executeQuery(view, params);
    
    if (items != null && items.length > 0) {
      ArrayList returnValues = new ArrayList();
      for (int i = 0; i < items.length; i++) {
        returnValues.add(new VirtualGiftCard(items[i]));
      }
      return returnValues;
    } else {
      return new ArrayList();
    }
  }
  
  public List lookupByEmailAndPickedUp(String email, String pickedUp) throws RepositoryException {
    MutableRepository repository = getVirtualGiftCardRepository();
    RepositoryView view = repository.getView("virtualGiftCard");
    RqlStatement statement = RqlStatement.parseRqlStatement("recipientEmail CONTAINS ?0 AND flgPickedUp CONTAINS ?1");
    Object params[] = new Object[2];
    
    params[0] = email;
    params[1] = convertPickedUpFlg(pickedUp);
    MutableRepositoryItem[] items = (MutableRepositoryItem[]) statement.executeQuery(view, params);
    
    if (items != null && items.length > 0) {
      ArrayList returnValues = new ArrayList();
      for (int i = 0; i < items.length; i++) {
        returnValues.add(new VirtualGiftCard(items[i]));
      }
      return returnValues;
    } else {
      return new ArrayList();
    }
  }
  
  public List lookupByEmailAndDate(String email, Date dateFrom, Date dateTo, String eventType) throws RepositoryException {
    MutableRepository repository = getVirtualGiftCardRepository();
    RepositoryView view = repository.getView("virtualGiftCard");
    RqlStatement statement = RqlStatement.parseRqlStatement("recipientEmail CONTAINS ?0 AND ( events INCLUDES ITEM ( eventTimeStamp > ?1 and eventTimeStamp < ?2 and  eventType contains ?3))");
    Object params[] = new Object[4];
    
    params[0] = email;
    params[1] = convertDateToFormat(dateFrom);
    params[2] = convertDateToFormat(dateTo);
    params[3] = eventType;
    MutableRepositoryItem[] items = (MutableRepositoryItem[]) statement.executeQuery(view, params);
    
    if (items != null && items.length > 0) {
      ArrayList returnValues = new ArrayList();
      for (int i = 0; i < items.length; i++) {
        returnValues.add(new VirtualGiftCard(items[i]));
      }
      return returnValues;
    } else {
      return new ArrayList();
    }
  }
  
  public List lookupByEmailAndDateAndPickedUp(String email, Date dateFrom, Date dateTo, String pickedUp, String eventType) throws RepositoryException {
    MutableRepository repository = getVirtualGiftCardRepository();
    RepositoryView view = repository.getView("virtualGiftCard");
    RqlStatement statement =
            RqlStatement.parseRqlStatement("recipientEmail CONTAINS ?0 AND events INCLUDES ITEM ( eventTimeStamp > ?1 and eventTimeStamp < ?2 and  eventType contains ?4) and flgPickedUp CONTAINS ?3");
    Object params[] = new Object[5];
    
    params[0] = email;
    params[1] = convertDateToFormat(dateFrom);
    params[2] = convertDateToFormat(dateTo);
    params[3] = convertPickedUpFlg(pickedUp);
    params[4] = eventType;
    MutableRepositoryItem[] items = (MutableRepositoryItem[]) statement.executeQuery(view, params);
    
    if (items != null && items.length > 0) {
      ArrayList returnValues = new ArrayList();
      for (int i = 0; i < items.length; i++) {
        returnValues.add(new VirtualGiftCard(items[i]));
      }
      return returnValues;
    } else {
      return new ArrayList();
    }
  }
  
  public List lookupByEmailAndOrderIdAndPickedUp(String email, String orderId, String pickedUp) throws RepositoryException {
    MutableRepository repository = getVirtualGiftCardRepository();
    RepositoryView view = repository.getView("virtualGiftCard");
    RqlStatement statement = RqlStatement.parseRqlStatement("recipientEmail CONTAINS ?0 AND orderId CONTAINS ?1 AND flgPickedUp CONTAINS ?2");
    Object params[] = new Object[3];
    
    params[0] = email;
    params[1] = orderId;
    params[2] = convertPickedUpFlg(pickedUp);
    
    MutableRepositoryItem[] items = (MutableRepositoryItem[]) statement.executeQuery(view, params);
    
    if (items != null && items.length > 0) {
      ArrayList returnValues = new ArrayList();
      for (int i = 0; i < items.length; i++) {
        returnValues.add(new VirtualGiftCard(items[i]));
      }
      return returnValues;
    } else {
      return new ArrayList();
    }
  }
  
  public List lookupByOrderIdAndPickedUp(String orderId, String pickedUp) throws RepositoryException {
    MutableRepository repository = getVirtualGiftCardRepository();
    RepositoryView view = repository.getView("virtualGiftCard");
    RqlStatement statement = RqlStatement.parseRqlStatement("orderId CONTAINS ?0 AND flgPickedUp CONTAINS ?1");
    Object params[] = new Object[2];
    
    params[0] = orderId;
    params[1] = convertPickedUpFlg(pickedUp);
    MutableRepositoryItem[] items = (MutableRepositoryItem[]) statement.executeQuery(view, params);
    
    if (items != null && items.length > 0) {
      ArrayList returnValues = new ArrayList();
      for (int i = 0; i < items.length; i++) {
        returnValues.add(new VirtualGiftCard(items[i]));
      }
      return returnValues;
    } else {
      return new ArrayList();
    }
  }
  
  public List lookupByEmailAndOrderIdAndDate(String email, String orderId, Date dateFrom, Date dateTo, String eventType) throws RepositoryException {
    MutableRepository repository = getVirtualGiftCardRepository();
    RepositoryView view = repository.getView("virtualGiftCard");
    RqlStatement statement =
            RqlStatement.parseRqlStatement("recipientEmail CONTAINS ?0 AND orderId CONTAINS ?1 AND events INCLUDES ITEM ( eventTimeStamp > ?2 and eventTimeStamp < ?3 and  eventType contains ?4)");
    Object params[] = new Object[5];
    
    params[0] = email;
    params[1] = orderId;
    params[2] = convertDateToFormat(dateFrom);
    params[3] = convertDateToFormat(dateTo);
    params[4] = eventType;
    MutableRepositoryItem[] items = (MutableRepositoryItem[]) statement.executeQuery(view, params);
    
    if (items != null && items.length > 0) {
      ArrayList returnValues = new ArrayList();
      for (int i = 0; i < items.length; i++) {
        returnValues.add(new VirtualGiftCard(items[i]));
      }
      return returnValues;
    } else {
      return new ArrayList();
    }
  }
  
  public List lookupByEmailAndOrderIdAndDateAndPickedUp(String email, String orderId, Date dateFrom, Date dateTo, String pickedUp, String eventType) throws RepositoryException {
    MutableRepository repository = getVirtualGiftCardRepository();
    RepositoryView view = repository.getView("virtualGiftCard");
    RqlStatement statement =
            RqlStatement
                    .parseRqlStatement("recipientEmail CONTAINS ?0 AND orderId CONTAINS ?1 AND events INCLUDES ITEM ( eventTimeStamp > ?2 and eventTimeStamp < ?3 and  eventType contains ?5) AND flgPickedUp CONTAINS ?4");
    Object params[] = new Object[6];
    
    params[0] = email;
    params[1] = orderId;
    params[2] = convertDateToFormat(dateFrom);
    params[3] = convertDateToFormat(dateTo);
    params[4] = convertPickedUpFlg(pickedUp);
    params[5] = eventType;
    MutableRepositoryItem[] items = (MutableRepositoryItem[]) statement.executeQuery(view, params);
    
    if (items != null && items.length > 0) {
      ArrayList returnValues = new ArrayList();
      for (int i = 0; i < items.length; i++) {
        returnValues.add(new VirtualGiftCard(items[i]));
      }
      return returnValues;
    } else {
      return new ArrayList();
    }
  }
  
  public VirtualGiftCard lookupByRepositoryId(String repositoryId) throws RepositoryException {
    MutableRepository repository = getVirtualGiftCardRepository();
    RepositoryView view = repository.getView("virtualGiftCard");
    RqlStatement statement = RqlStatement.parseRqlStatement("vgcId = ?0");
    Object params[] = new Object[1];
    params[0] = repositoryId;
    MutableRepositoryItem[] items = (MutableRepositoryItem[]) statement.executeQuery(view, params);
    if (items != null && items.length > 0) {
      return new VirtualGiftCard(items[0]);
    } else {
      return null;
    }
  }
}
