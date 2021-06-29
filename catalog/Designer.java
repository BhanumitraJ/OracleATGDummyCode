package com.nm.commerce.catalog;

import com.nm.catalog.navigation.DriveToCategory;

/********************************************************
 * @author Elaine Hopkins
 * @since 2/10/2007 This data class accepts a category RepositoryItem and breaks down the attributes into usuable formats and creates getters/setters to have easy access for pages like Designer Index
 ********************************************************/

public class Designer {
  
  private com.nm.catalog.navigation.NMCategory dataSource = null;
  private String displayName = "";
  private String boutiqueTextAdornments = "";
  private String lowerDisplayName = "";
  private String description = "";
  private String templateURL = null;
  private String alphaSortChar = "";
  private String id = "";
  private String parentId = "";
  private String masterId = "";
  DriveToCategory driveToSubCategory = null;
  private Boolean redTextFlag = false;
  
  /*************************************************************************
   * CONSTRUCTOR
   ************************************************************************/
  public Designer(final com.nm.catalog.navigation.NMCategory objCategory, final String objParentId, final String objMasterId) {
    // RepositoryItem designerItem = new CategoryRepositoryItem ((RepositoryItem) objCategory, (String) objParentId);
    final com.nm.catalog.navigation.NMCategory designerItem = objCategory;
    final String strParentId = objParentId;
    final String strMasterId = objMasterId;
    this.setDataSource(designerItem);
    this.setBoutiqueTextAdornments(designerItem.getBoutiqueTextAdornments());
    this.setDisplayName(designerItem.getDisplayName());
    this.setDescription(designerItem.getDescription());
    this.setId(designerItem.getRepositoryId());
    this.setParentId(strParentId);
    this.setMasterId(strMasterId);
    this.setDriveToSubCategory(objCategory.getDriveToSubcategory());
    this.setRedTextFlag(designerItem.getRedTextFlag());
    determineTemplateURL();
    determineLowerDisplayName();
    determineAlphaSortChar();
  }
  
  /*************************************************************************
   * Name of Method: determineTemplateURL Determine if a template URL has been assigned to this designer to avoid a null pointer exception
   * 
   ************************************************************************/
  public void determineTemplateURL() {
    final String url = getDataSource().getTemplateUrl();
    if (url != null) {
      setTemplateURL(url);
    }
    // RepositoryItem riTemplateItem = (RepositoryItem)getDataSource().getPropertyValue("template");
    // if (riTemplateItem != null){
    // String strTemplateURL = (String)riTemplateItem.getPropertyValue("url");
    // if (strTemplateURL != null){
    // setTemplateURL(strTemplateURL);
    // }
    // }
  }
  
  /*************************************************************************
   * Name of Method: determineLowerDisplayName Strips the designer name (or alternateCategoryName if applicable) of any extra spaces and the hidden alt255 character at end of designer name and changes
   * all letters to lowercase to ensure proper alpha sorting
   ************************************************************************/
  public void determineLowerDisplayName() {
    String designerName = getDisplayName();
    if (designerName != null) {
      // Replace double spaces with single spaces
      designerName = designerName.replaceAll("\\s\\s", " ");
      designerName = designerName.trim();
      final int intLastChar = designerName.length() - 1;
      final char lastChar = designerName.charAt(intLastChar);
      
      // Some names have hidden space at end (the Alt-255 character) - remove it
      final boolean isLetterDigit = Character.isLetterOrDigit(lastChar);
      if (!(isLetterDigit)) {
        designerName = designerName.substring(0, (intLastChar));
      }
      
      designerName = designerName.toLowerCase();
      setLowerDisplayName(designerName);
    }
  }
  
  /*************************************************************************
   * Name of Method: determineAlphaSortChar determines which alpha header letter this designer should appear under
   ************************************************************************/
  public void determineAlphaSortChar() {
    final String designerName = getLowerDisplayName();
    String strAlphaSortChar = "";
    
    if (designerName != null) {
      final char alphaSortChar = designerName.charAt(0);
      final boolean sortCharIsLetter = Character.isLetter(alphaSortChar);
      if (!(sortCharIsLetter)) {
        strAlphaSortChar = "#";
      } else {
        strAlphaSortChar = String.valueOf(alphaSortChar);
        strAlphaSortChar = strAlphaSortChar.toUpperCase();
      }
      setAlphaSortChar(strAlphaSortChar);
    }
  }
  
  // ****************************************************
  // ATTRIBUTE GETTERS AND SETTERS START HERE
  /*****************************************************
   * @return Returns the dataSource
   *****************************************************/
  public com.nm.catalog.navigation.NMCategory getDataSource() {
    return this.dataSource;
  }
  
  /*****************************************************
   * @param dataSource
   *          The dataSource to set.
   *****************************************************/
  public void setDataSource(final com.nm.catalog.navigation.NMCategory dataSource) {
    this.dataSource = dataSource;
  }
  
  public String getAlternateSeoName() {
    
    return getDataSource().getAlternateSeoName();
    
    // String seoName = "";
    // boolean useDisplayName = false;
    
    // if(getDataSource() != null) {
    // try {
    // if (getDataSource().getItemDescriptor().hasProperty("alternateSeoName")) {
    // seoName = (String) getDataSource().getPropertyValue("alternateSeoName");
    
    // } else {
    // useDisplayName = true;
    // }
    // } catch (RepositoryException e) {
    
    // useDisplayName = true;
    // e.printStackTrace();
    // }
    
    // } else {
    // useDisplayName = true;
    // }
    
    // if(useDisplayName || seoName == null || seoName.equals("")) {
    // seoName = getDisplayName();
    // }
    
    // return seoName;
    
  }
  
  /*****************************************************
   * @return Returns the templateURL
   *****************************************************/
  public String getTemplateURL() {
    return this.templateURL;
  }
  
  /*****************************************************
   * @param templateURL
   *          The templateURL to set.
   *****************************************************/
  public void setTemplateURL(final String templateURL) {
    if (templateURL != null) {
      this.templateURL = templateURL;
    }
  }
  
  /*****************************************************
   * @return Returns the displayName
   *****************************************************/
  public String getDisplayName() {
    return this.displayName;
  }
  
  /*****************************************************
   * @param displayName
   *          The displayName to set.
   *****************************************************/
  public void setDisplayName(final String displayName) {
    if (displayName != null) {
      this.displayName = displayName;
    }
  }
  
  /*****************************************************
   * @return Returns the lowerDisplayName
   *****************************************************/
  public String getLowerDisplayName() {
    return this.lowerDisplayName;
  }
  
  /*****************************************************
   * @param lowerDisplayName
   *          The lowerDisplayName to set.
   *****************************************************/
  public void setLowerDisplayName(final String lowerDisplayName) {
    if (lowerDisplayName != null) {
      this.lowerDisplayName = lowerDisplayName;
    }
  }
  
  /*****************************************************
   * @return Returns the category description
   *****************************************************/
  public String getDescription() {
    return this.description;
  }
  
  /*****************************************************
   * @param description
   *          The category description to set.
   *****************************************************/
  public void setDescription(final String description) {
    if (description != null) {
      this.description = description;
    }
  }
  
  /*****************************************************
   * @return Returns the alphaSortChar
   *****************************************************/
  public String getAlphaSortChar() {
    return this.alphaSortChar;
  }
  
  /*****************************************************
   * @param alphaSortChar
   *          The alphaSortChar to set.
   *****************************************************/
  public void setAlphaSortChar(final String alphaSortChar) {
    if (alphaSortChar != null) {
      this.alphaSortChar = alphaSortChar;
    }
  }
  
  /*****************************************************
   * @return Returns the id
   *****************************************************/
  public String getId() {
    return this.id;
  }
  
  /*****************************************************
   * @param id
   *          The id to set.
   *****************************************************/
  public void setId(final String id) {
    if (id != null) {
      this.id = id;
    }
  }
  
  /*****************************************************
   * @return Returns the parentId
   *****************************************************/
  public String getParentId() {
    return this.parentId;
  }
  
  /*****************************************************
   * @param parentId
   *          The parentId to set.
   *****************************************************/
  public void setParentId(final String parentId) {
    if (parentId != null) {
      this.parentId = parentId;
    }
  }
  
  /*****************************************************
   * @return Returns the masterId
   *****************************************************/
  public String getMasterId() {
    return this.masterId;
  }
  
  /*****************************************************
   * @param masterId
   *          The masterId to set.
   *****************************************************/
  public void setMasterId(final String masterId) {
    if (masterId != null) {
      this.masterId = masterId;
    }
  }
  
  public String getBoutiqueTextAdornments() {
    return boutiqueTextAdornments;
  }
  
  public void setBoutiqueTextAdornments(final String boutiqueTextAdornments) {
    this.boutiqueTextAdornments = boutiqueTextAdornments;
  }
  
  public DriveToCategory getDriveToSubCategory() {
    return driveToSubCategory;
  }
  
  public void setDriveToSubCategory(final DriveToCategory driveToSubCategory) {
    this.driveToSubCategory = driveToSubCategory;
  }
  
  public Boolean getRedTextFlag() {
    return redTextFlag;
  }
  
  public void setRedTextFlag(final Boolean redTextFlag) {
    this.redTextFlag = redTextFlag;
  }
  
}// end class
