package com.nm.commerce.promotion;

public interface PromotionAttributeTypes {
  String ATTR_action = "action";
  String ATTR_catalog = "catalog";
  String ATTR_categories = "categories";
  String ATTR_classcodes = "classcodes";
  String ATTR_classes = "classes";
  String ATTR_comments = "comments";
  String ATTR_departments = "departments";
  String ATTR_depiction = "depiction";
  String ATTR_dunsNumber = "dunsNumber";
  String ATTR_dollar = "dollar";
  String ATTR_orderdollar = "orderdollar";
  String ATTR_enddate = "enddate";
  String ATTR_excludeclasscodes = "excludeclasscodes";
  String ATTR_excludedepartments = "excludedepartments";
  String ATTR_excludeitems = "excludeitems";
  String ATTR_excludevendor = "excludevendor";
  String ATTR_giftwrapprice = "giftwrapprice";
  String ATTR_gwpitem = "gwpitem";
  String ATTR_pwpitem = "pwpitem";
  String ATTR_itemCount = "itemCount";
  String ATTR_items = "items";
  String ATTR_key = "key";
  String ATTR_markdate = "markdate";
  String ATTR_markenddate = "markenddate";
  String ATTR_name = "name";
  String ATTR_onetime = "onetime";
  String ATTR_awardParenthetical = "awardParenthetical";
  String ATTR_onlineonly = "onlineonly";
  String ATTR_percentoff = "percentoff";
  String ATTR_dollaroff = "dollaroff";
  String ATTR_tieredstackedflag = "tieredstackedflag";
  String ATTR_promocode = "promocode";
  String ATTR_promotype = "promotype";
  String ATTR_highestPricedItemFlag = "highestPricedItemFlag";
  String ATTR_signature = "signature";
  String ATTR_startdate = "startdate";
  String ATTR_subject = "subject";
  String ATTR_type = "type";
  String ATTR_typetext = "typetext";
  String ATTR_vendor = "vendor";
  String ATTR_multigwp = "multigwp";
  String ATTR_multiGWPDollarQual = "multiGWPDollarQual";
  String ATTR_gwpShipWithItem = "gwpShipWithItem";
  String ATTR_saleQualification = "saleQualification";
  String ATTR_promotionalPrice = "promotionalPrice";
  String ATTR_details = "details";
  String ATTR_bnglDate = "bnglDate";
  String ATTR_limitPromoUse = "limitPromoUse";
  String ATTR_limitMaxUse = "limitMaxUse";
  String ATTR_newCustomerFlag = "newCustomerFlag";
  String ATTR_stackableFlag = "stackableFlag";
  String ATTR_plccFlag = "plccFlag";
  String ATTR_dynamicFlag = "dynamicFlag";
  String ATTR_spendMoreFlag = "spendMoreFlag";
  String ATTR_buyOneGetOneQualifiedPromotion = "buyOneGetOneQualifiedPromotion";
  String ATTR_buyOneGetOneQualifyingItemCount = "buyOneGetOneQualifyingItemCount";
  
  /** The ATTR_personalized flag. */
  String ATTR_personalizedFlag = "personalizedFlag";
  
  String PROMOTYPE_PERCENT_OFF = "%OFF";
  String PROMOTYPE_DOLLAR_OFF = "$OFF";
  String PROMOTYPE_GWP_SELECT = "GWP SELECT";
  String PROMOTYPE_GWP = "GWP";
  String PROMOTYPE_GIFT_WRAP = "GIFT WRAP";
  String PROMOTYPE_FREE_SHIPPING = "FREE SHIPPING";
  String PROMOTYPE_EXTRA_ADDR = "EXTRA ADDR";
  String PROMOTYPE_PARENTHETICAL_SHP = "PARENTHETICAL SHIPPING";
  String PROMOTYPE_PWP = "PWP";
  
  String ACTION_ADD = "A";
  String ACTION_CHANGE = "C";
  String ACTION_DELETE = "D";
  
  String[] SALE_QUALIFICATION_LABEL = {"INCLUDE" , "EXCLUDE" , "ONLY"};
}
