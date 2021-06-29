package com.nm.commerce.upsell;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import atg.repository.RepositoryItem;

/**
 * Class Name: ProductListDecorator Abstract base class for product list decorators. The idea is that ProductListDecorator subclasses would act as constant filters and filter out products that did not
 * match certain restrictions that the subclass was supposed to watch for. Also possible: subclasses that add items to the list based on certain conditions.
 * 
 * See GoF Decorator Pattern
 * 
 * @author C. Chadwick
 * @author $Author: Richard A Killen (NMRAK3) $
 * @since 10/1/2004 Last Modified Date: $Date: 2012/07/12 10:19:45CDT $
 * @version $Revision: 1.3 $
 */
public class ProductListDecorator implements List<RepositoryItem> {
  private List<RepositoryItem> productList;
  
  /**
   * Creates a new ProductListDecorator object.
   * 
   * @param productList
   *          The list we are decorating
   */
  public ProductListDecorator(List<RepositoryItem> productList) {
    List<RepositoryItem> listClone = new ArrayList<RepositoryItem>(productList);
    this.productList = listClone;
  }
  
  /**
   * Simple pass through. Calls wrapped list's method.
   * 
   * @return size
   */
  public int size() {
    updateList();
    
    return productList.size();
  }
  
  /**
   * Simple pass through. Calls wrapped list's method.
   */
  public boolean isEmpty() {
    updateList();
    
    return productList.isEmpty();
  }
  
  /**
   * Simple pass through. Calls wrapped list's method.
   */
  public boolean contains(Object o) {
    updateList();
    
    return productList.contains(o);
  }
  
  /**
   * Simple pass through. Calls wrapped list's method.
   */
  public Iterator<RepositoryItem> iterator() {
    updateList();
    
    return productList.iterator();
  }
  
  /**
   * Simple pass through. Calls wrapped list's method.
   */
  public RepositoryItem[] toArray() {
    updateList();
    
    return productList.toArray(new RepositoryItem[productList.size()]);
  }
  
  /**
   * Simple pass through. Calls wrapped list's method.
   */
  public <T> T[] toArray(T[] a) {
    updateList();
    
    return productList.toArray(a);
  }
  
  /**
   * Simple pass through. Calls wrapped list's method.
   */
  public boolean add(RepositoryItem o) {
    return productList.add(o);
  }
  
  /**
   * Simple pass through. Calls wrapped list's method.
   */
  public boolean remove(Object o) {
    return productList.remove(o);
  }
  
  /**
   * Simple pass through. Calls wrapped list's method.
   */
  public boolean containsAll(Collection<?> c) {
    updateList();
    
    return productList.containsAll(c);
  }
  
  /**
   * Simple pass through. Calls wrapped list's method.
   */
  public boolean addAll(Collection<? extends RepositoryItem> c) {
    return productList.addAll(c);
  }
  
  /**
   * Simple pass through. Calls wrapped list's method.
   */
  public boolean addAll(int index, Collection<? extends RepositoryItem> c) {
    return productList.addAll(index, c);
  }
  
  /**
   * Simple pass through. Calls wrapped list's method.
   */
  public boolean removeAll(Collection<?> c) {
    return productList.removeAll(c);
  }
  
  /**
   * Simple pass through. Calls wrapped list's method.
   */
  public boolean retainAll(Collection<?> c) {
    updateList();
    
    return productList.retainAll(c);
  }
  
  /**
   * Simple pass through. Calls wrapped list's method.
   */
  public void clear() {
    productList.clear();
  }
  
  /**
   * Simple pass through. Calls wrapped list's method.
   */
  public RepositoryItem get(int index) {
    updateList();
    
    return productList.get(index);
  }
  
  /**
   * Simple pass through. Calls wrapped list's method.
   */
  public RepositoryItem set(int index, RepositoryItem element) {
    return productList.set(index, element);
  }
  
  /**
   * Simple pass through. Calls wrapped list's method.
   */
  public void add(int index, RepositoryItem element) {
    productList.add(index, element);
  }
  
  /**
   * Simple pass through. Calls wrapped list's method.
   */
  public RepositoryItem remove(int index) {
    return productList.remove(index);
  }
  
  /**
   * Simple pass through. Calls wrapped list's method.
   */
  public int indexOf(Object o) {
    return productList.indexOf(o);
  }
  
  /**
   * Simple pass through. Calls wrapped list's method.
   */
  public int lastIndexOf(Object o) {
    updateList();
    
    return productList.lastIndexOf(o);
  }
  
  /**
   * Simple pass through. Calls wrapped list's method.
   */
  public ListIterator<RepositoryItem> listIterator() {
    updateList();
    
    return productList.listIterator();
  }
  
  /**
   * Simple pass through. Calls wrapped list's method.
   */
  public ListIterator<RepositoryItem> listIterator(int index) {
    updateList();
    
    return productList.listIterator(index);
  }
  
  /**
   * Simple pass through. Calls wrapped list's method.
   */
  public List<RepositoryItem> subList(int fromIndex, int toIndex) {
    updateList();
    
    return productList.subList(fromIndex, toIndex);
  }
  
  /**
   * Simple pass through. Calls wrapped list's method.
   */
  protected void updateList() {};
  
  /**
   * Simple pass through. Calls wrapped list's method.
   */
  protected List<RepositoryItem> getProductList() {
    return productList;
  }
}

// ====================================================================
// File: ProductListDecorator.java
//
// Copyright (c) 2004 Neiman Marcus.
// All rights reserved.
//
// This is an unpublished proprietary source code of the Neiman Marcus Group.
// The copyright notice above does not evidence and actual or intended publication
// of said source code.
// ====================================================================
