package com.nm.commerce.pagedef.model.bean;

public class Breadcrumb {
  private String url;
  private String title;
  private int pageNum;
  private boolean clickable;
  private boolean currentPage;
  
  public Breadcrumb(String url, String title, int pageNum) {
    this(url, title, false, false, pageNum);
  }
  
  public Breadcrumb(String url, String title, boolean clickable, boolean currentPage, int pageNum) {
    this.url = url;
    this.title = title;
    this.clickable = clickable;
    this.currentPage = currentPage;
    this.pageNum = pageNum;
  }
  
  public String getUrl() {
    return url;
  }
  
  public void setUrl(String url) {
    this.url = url;
  }
  
  public String getTitle() {
    return title;
  }
  
  public void setTitle(String title) {
    this.title = title;
  }
  
  public int getPageNum() {
    return pageNum;
  }
  
  public void setPageNum(int pageNum) {
    this.pageNum = pageNum;
  }
  
  public boolean isClickable() {
    return clickable;
  }
  
  public void setClickable(boolean clickable) {
    this.clickable = clickable;
  }
  
  public boolean isCurrentPage() {
    return currentPage;
  }
  
  public void setCurrentPage(boolean currentPage) {
    this.currentPage = currentPage;
  }
}
