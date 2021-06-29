package com.nm.commerce.catalog.core;

import java.util.Properties;

import atg.nucleus.GenericService;

/**
 * TemplateElement is the base class representation of any functional element on the site This class has been designed to mirror a future repository definition that is still in process.
 */
public class TemplateElement extends GenericService {
  
  /** returns the properties of this element **/
  public Properties getProperties() {
    if (properties == null) {
      properties = new Properties();
    }
    return properties;
  }
  
  /**
   * returns the unique identifier for a specific template element configuration
   * 
   * @return the identifier
   */
  public String getId() {
    return id;
  }
  
  /**
   * Returns the name for this specific template element
   * 
   * @return the name of the template element
   */
  public String getName() {
    return name;
  }
  
  /**
   * Returns the description for this specific template element
   * 
   * @return the description for the template element.
   */
  public String getDescription() {
    return description;
  }
  
  /**
   * Returns the file path location of the .jhtml (future .jsp) file that will process the configurations referenced by this file
   * 
   * @return the string path to the file
   */
  public String getContentPath() {
    return contentPath;
  }
  
  /**
   * Returns the context the getContentPath() file references. This is not used at present but is provided to allow for a template file to reference a .jsp/.jhtml file that may be specific to a brand
   * and therefore located outside the common templating element. This will allow that file to be referenced via a context
   * 
   * @return the context location of the referenced file.
   */
  public String getJ2eeContext() {
    return j2eeContext;
  }
  
  /**
   * Returns the type of element referenced by this file.
   * 
   * @return the key for the element type
   */
  public Integer getType() {
    return type;
  }
  
  /**
   * Returns a list of template elements that are contained as child elements of this specific template element. For example, the left navigation may optionally contain a reference to a refereshable
   * navaux file. The navigation element would configure the subelements to reference that asset (which is also a template element) along with any other elements that need to be rendered.
   * 
   * @returns the ordered list of sub elements contained by this element
   */
  public TemplateElement[] getSubElements() {
    return subElements;
  }
  
  /**
   * @param id
   *          - sets the id of this element
   */
  public void setId(String id) {
    this.id = id;
  }
  
  /**
   * @param name
   *          - sets the name of this element
   */
  public void setName(String name) {
    this.name = name;
  }
  
  /**
   * @param description
   *          - sets the description of this element
   */
  public void setDescription(String description) {
    this.description = description;
  }
  
  /**
   * @param contentPath
   *          - sets the path to the .jhtml/.jsp reference file for this element
   */
  public void setContentPath(String contentPath) {
    this.contentPath = contentPath;
  }
  
  /**
   * @param type
   *          - sets the type of this element
   */
  public void setType(Integer type) {
    this.type = type;
  }
  
  /**
   * @param subElements
   *          - set the child elements of this element
   */
  public void setSubElements(TemplateElement[] subElements) {
    this.subElements = subElements;
  }
  
  /**
   * @param value
   *          - sets the properties of this element
   */
  public void setProperties(Properties value) {
    properties = value;
  }
  
  private String id;
  private String name;
  private String description;
  private String contentPath;
  private String j2eeContext;
  private Integer type;
  private Properties properties;
  private TemplateElement[] subElements;
  
  public static final Integer BRAND_ELEMENT = new Integer(0);
  public static final Integer CSS = new Integer(1);
  public static final Integer BREADCRUMB = new Integer(2);
  public static final Integer NAVIGATION = new Integer(3);
  public static final Integer CATEGORYHEADER = new Integer(4);
  public static final Integer REFRESHABLE = new Integer(5);
  public static final Integer FILTER = new Integer(6);
  public static final Integer SORT = new Integer(7);
  public static final Integer PAGE = new Integer(8);
  public static final Integer CATEGORY_THUMBNAILS = new Integer(9);
  public static final Integer PRODUCT_THUMBNAILS = new Integer(10);
  public static final Integer FEATURE = new Integer(11);
  public static final Integer PROMO = new Integer(12);
  public static final Integer INDEX = new Integer(13);
  public static final Integer BOUTIQUE = new Integer(14);
  public static final Integer CUSTOM = new Integer(15);
  public static final Integer SEEMORE = new Integer(16);
  public static final Integer ENDECA = new Integer(17);
  public static final Integer PRODUCT = new Integer(18);
  public static final Integer SUITE = new Integer(19);
  public static final Integer GRID_PRODUCT_THUMBNAILS = new Integer(20);
  public static final Integer FACETS = new Integer(21);
}
