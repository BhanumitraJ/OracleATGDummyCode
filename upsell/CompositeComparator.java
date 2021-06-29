package com.nm.commerce.upsell;

import java.util.Comparator;

import atg.nucleus.GenericService;
import atg.nucleus.ServiceException;

/**
 * Class Name: CompositeComparator The composite comparator allows the "daisy chaining" of comparators so that comparisons can be made at various levels of granularity. The class takes a property that
 * is an array of comparators. The CompositeComparator will attempt to use the first comparator to compare the objects. If that comparator says they are equal the CompositeComparator will use the next
 * comparator to compare them. Two objects are not considered equal unless every comparator in the array says they are. If any comparator says anything besides equal, then that is the result that is
 * returned.
 * 
 * @author C. Chadwick
 * @author $Author: nmmc5 $
 * @since 8/30/2004 Last Modified Date: $Date: 2008/04/29 13:57:40CDT $
 * @version $Revision: 1.2 $
 */
public class CompositeComparator extends GenericService implements Comparator {
  private Comparator[] comparators;
  
  /**
   * Creates a new CompositeComparator object.
   */
  public CompositeComparator() {
    setComparators(new Comparator[0]);
  }
  
  /**
   * Runs when the component starts up
   */
  public void doStartService() throws ServiceException {
    super.doStartService();
    // Nothing to do
  }
  
  /**
   * Runs when the component is stopped
   */
  public void doStopService() throws ServiceException {
    // Nothing to do
    super.doStopService();
  }
  
  /**
   * Compares two NMCommerceItems
   * 
   * Iterates through the list of comparators until a comparator says the two objects are not equal or until there are no comparators left to ask.
   * 
   * @param o1
   *          The first item to be compared
   * @param o2
   *          The second item to be compared
   * 
   * @return <0 if o1 is greater than o2. >0 if o1 is less than 02. =0 if they are equal
   * @throws ClassCastException
   *           if both the objects are not NMCommcerceItems
   */
  public int compare(Object o1, Object o2) throws ClassCastException {
    int result = 0;
    int numComparators = getComparators().length;
    
    for (int i = 0; i < numComparators; i++) {
      Comparator currentComparator = getComparators()[i];
      
      result = currentComparator.compare(o1, o2);
      if (isLoggingDebug()) {
        logDebug("Comparing object 1:" + o1 + " and object2:" + o2 + " using " + currentComparator.getClass() + " with result " + result);
      }
      if (result != 0) {
        break;
      }
    }
    
    return result;
  }
  
  /**
   * Getter for the comparators property
   * 
   * @return Returns the comparators.
   */
  public Comparator[] getComparators() {
    return comparators;
  }
  
  /**
   * Setter for the comparators property
   * 
   * @param comparators
   *          The comparators to set.
   */
  public void setComparators(Comparator[] comparators) {
    this.comparators = comparators;
  }
}

// ====================================================================
// File: CompositeComparator.java
//
// Copyright (c) 2004 Neiman Marcus.
// All rights reserved.
//
// This is an unpublished proprietary source code of the Neiman Marcus Group.
// The copyright notice above does not evidence and actual or intended publication
// of said source code.
// ====================================================================
