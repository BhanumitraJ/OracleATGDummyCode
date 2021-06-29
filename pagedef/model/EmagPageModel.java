package com.nm.commerce.pagedef.model;

import java.util.List;

import com.nm.catalog.navigation.NMCategory;

public class EmagPageModel extends TemplatePageModel {
  private NMCategory emagCategory;
  private List<NMCategory> contentsCategoryList;
  private int storyIndex;
  
  /** The story category. */
  private NMCategory storyCategory;
  
  public NMCategory getEmagCategory() {
    return emagCategory;
  }
  
  public void setEmagCategory(final NMCategory emagCategory) {
    this.emagCategory = emagCategory;
  }
  
  public List<NMCategory> getContentsCategoryList() {
    return contentsCategoryList;
  }
  
  public void setContentsCategoryList(final List<NMCategory> contentsCategoryList) {
    this.contentsCategoryList = contentsCategoryList;
  }
  
  public int getStoryIndex() {
    return storyIndex;
  }
  
  public void setStoryIndex(final int storyIndex) {
    this.storyIndex = storyIndex;
  }
  
  /**
   * Gets the story category.
   * 
   * @return the storyCategory
   */
  public NMCategory getStoryCategory() {
    return storyCategory;
  }
  
  /**
   * Sets the story category.
   * 
   * @param storyCategory
   *          the storyCategory to set
   */
  public void setStoryCategory(final NMCategory storyCategory) {
    this.storyCategory = storyCategory;
  }
  
}
