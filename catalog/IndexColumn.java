package com.nm.commerce.catalog;

import java.lang.*;
import java.util.*;
import atg.repository.*;

public class IndexColumn {
  
  private ArrayList categories = new ArrayList();
  private boolean isSplit = false;
  private int startWith = 1;
  private int howMany = 0;
  private boolean displayHeader = true;
  
  /*************************************************************************
   * CONSTRUCTOR
   ************************************************************************/
  public IndexColumn(RepositoryItem categoryItem, int startNum, int loopNum, boolean split) {
    addCategoryItem(categoryItem);
    this.setStartWith(startNum);
    this.setHowMany(loopNum);
    this.setIsSplit(split);
    determineDisplayHeader();
    
  }
  
  /*************************************************************************
   * Name of Method: determineDisplayHeader Determine if a header will be displayed based on whether or not it is a continued column
   * 
   ************************************************************************/
  public void determineDisplayHeader() {
    if (startWith > 1) {
      displayHeader = false;
    }
  }
  
  /*************************************************************************
   * Name of Method: addCategoryItem Add a category RepositoryItem to the ArrayList of categories
   * 
   ************************************************************************/
  public void addCategoryItem(RepositoryItem categoryItem) {
    categories.add(categoryItem);
  }
  
  // ****************************************************
  // ATTRIBUTE GETTERS AND SETTERS START HERE
  /*****************************************************
   * @return Returns an ArrayList of the categories that belong in the column
   *****************************************************/
  public ArrayList getCategories() {
    return this.categories;
  }
  
  /*****************************************************
   * @param categories
   *          The categories that are to be displayed in the column
   *****************************************************/
  public void setCategories(ArrayList categories) {
    this.categories = categories;
  }
  
  /*****************************************************
   * @return Returns true if the category is split
   *****************************************************/
  public boolean getIsSplit() {
    return this.isSplit;
  }
  
  /*****************************************************
   * @param isSplit
   *****************************************************/
  public void setIsSplit(boolean isSplit) {
    this.isSplit = isSplit;
  }
  
  /*****************************************************
   * @return Returns the int that indicates which childCat that will display first in the column
   *****************************************************/
  public int getStartWith() {
    return this.startWith;
  }
  
  /*****************************************************
   * @param startWith
   *          Sets the int that indicates which childCat will display first in the column
   *****************************************************/
  public void setStartWith(int startWith) {
    this.startWith = startWith;
  }
  
  /*****************************************************
   * @return Returns the int that indicates how many categories will be displayed
   *****************************************************/
  public int getHowMany() {
    return this.howMany;
  }
  
  /*****************************************************
   * @param howMany
   *          Sets the int that indicates how many categories will be displayed
   *****************************************************/
  public void setHowMany(int howMany) {
    this.howMany = howMany;
  }
  
  /*****************************************************
   * @return Returns the displayHeader
   *****************************************************/
  public boolean getDisplayHeader() {
    return this.displayHeader;
  }
  
  /*****************************************************
   * @param displayHeader
   *          The displayHeader to set.
   *****************************************************/
  public void setDisplayHeader(boolean displayHeader) {
    this.displayHeader = displayHeader;
  }
  
}// end class

