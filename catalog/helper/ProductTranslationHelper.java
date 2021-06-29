package com.nm.commerce.catalog.helper;

import java.util.Locale;
import java.util.StringTokenizer;

import com.nm.components.CommonComponentHelper;
import atg.repository.Repository;
import atg.repository.RepositoryItem;
import atg.repository.RepositoryView;
import atg.repository.rql.RqlStatement;

public class ProductTranslationHelper {
  
  public static String getLanguageCode(String productId, Locale locale) {
    String langCode = null;
    String localeName = null;
    
    if (locale != null) {
      langCode = locale.getLanguage();
      if ((langCode != null) && (langCode.length() > 2)) {
        langCode = langCode.substring(0, 2);
      }
      localeName = locale.getDisplayName();
      StringTokenizer st = new StringTokenizer(localeName, "(");
      if (st.hasMoreTokens()) {
        localeName = st.nextToken().trim();
      }
    }
    return langCode;
  }
  
  public static RepositoryItem getProductTranslationRI(String productId, String langCode) {
    RepositoryItem prodTransRI = null;
    boolean displayTranslation = false;
    try {
      Repository productRepository = (Repository) CommonComponentHelper.getProductRepository();
      String key = productId + ":" + langCode;
      prodTransRI = productRepository.getItem(key, "prod_translation");
    } catch (Exception ex) {
      System.out.println("ProductTranslationHelper - getProductTranslationRI() exception: " + ex.toString());
    }
    return prodTransRI;
  }
}
