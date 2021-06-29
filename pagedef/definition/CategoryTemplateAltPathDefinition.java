package com.nm.commerce.pagedef.definition;

import java.util.HashMap;
import java.util.Map;

import atg.nucleus.GenericService;

/**
 * Define Alt Path for category templates for AB Tests
 */
public class CategoryTemplateAltPathDefinition extends GenericService {
  
  private static final Map<String, String> altPaths = new HashMap<String, String>() {
    private static final long serialVersionUID = 1L;
    {
      put("/nm/commerce/pagedef/template/P5", "/nm/commerce/pagedef/template/P3");
      put("/nm/commerce/pagedef/template/P5Beauty", "/nm/commerce/pagedef/template/P3Beauty");
      put("/nm/commerce/pagedef/template/P5Home", "/nm/commerce/pagedef/template/P3Home");
      
      put("/nm/commerce/pagedef/template/P5b", "/nm/commerce/pagedef/template/P3");
      put("/nm/commerce/pagedef/template/P5bBeauty", "/nm/commerce/pagedef/template/P3Beauty");
      put("/nm/commerce/pagedef/template/P5bHome", "/nm/commerce/pagedef/template/P3Home");
      
      put("/nm/commerce/pagedef/template/P5Feature", "/nm/commerce/pagedef/template/P3");
      put("/nm/commerce/pagedef/template/P5FeatureBeauty", "/nm/commerce/pagedef/template/P3Beauty");
      put("/nm/commerce/pagedef/template/P5FeatureHome", "/nm/commerce/pagedef/template/P3Home");
      
      put("/nm/commerce/pagedef/template/PVAll", "/nm/commerce/pagedef/template/P3VAll");
      put("/nm/commerce/pagedef/template/PVAllBeauty", "/nm/commerce/pagedef/template/P3VAllBeauty");
      put("/nm/commerce/pagedef/template/PVAllHome", "/nm/commerce/pagedef/template/P3VAllHome");
      
      put("/nm/commerce/pagedef/template/P9", "/nm/commerce/pagedef/template/P3");
      put("/nm/commerce/pagedef/template/P9Beauty", "/nm/commerce/pagedef/template/P3Beauty");
      put("/nm/commerce/pagedef/template/P9Home", "/nm/commerce/pagedef/template/P3Home");
      put("/nm/commerce/pagedef/template/P6", "/nm/commerce/pagedef/template/P3");
      put("/nm/commerce/pagedef/template/P6Beauty", "/nm/commerce/pagedef/template/P3Beauty");
      put("/nm/commerce/pagedef/template/P6Home", "/nm/commerce/pagedef/template/P3Home");
      put("/nm/commerce/pagedef/template/P4", "/nm/commerce/pagedef/template/P3");
      put("/nm/commerce/pagedef/template/P4Beauty", "/nm/commerce/pagedef/template/P3Beauty");
      put("/nm/commerce/pagedef/template/P4Home", "/nm/commerce/pagedef/template/P3Home");
      put("/nm/commerce/pagedef/template/ChanelP9", "/nm/commerce/pagedef/template/ChanelP3");
      put("/nm/commerce/pagedef/etemplate/EFs", "/nm/commerce/pagedef/etemplate/EFsNew");
      put("/nm/commerce/pagedef/etemplate/Search", "/nm/commerce/pagedef/etemplate/SearchNew");
    }
  };
  
  public CategoryTemplateAltPathDefinition() {
    super();
  }
  
  public static String getAltTemplate(String key) {
    return altPaths.get(key);
  }
}
