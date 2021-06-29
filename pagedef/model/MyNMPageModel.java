package com.nm.commerce.pagedef.model;

import java.util.List;

import com.nm.ajax.mynm.beans.MyNMRespObj;
import com.nm.mynm.MyNMWidget;

public class MyNMPageModel extends PageModel {
  
  List<MyNMWidget> widgets;
  String[] widgetArray;
  private boolean loginStatus;
  /** The widgets ids. */
  private String widgetsIds;  
  /**my NM Ajax Response */
  private MyNMRespObj myNMAjaxResponse;
  
  public void setWidgets(List<MyNMWidget> widgets) {
    this.widgets = widgets;
  }
  
  public List<MyNMWidget> getWidgets() {
    return widgets;
  }
  
  public void setWidgetArray(String[] widgetArray) {
    this.widgetArray = widgetArray;
  }
  
  public String[] getWidgetArray() {
    return widgetArray;
  }
  public boolean isLoginStatus() {
	    return loginStatus;
	  }
	  
	  public void setLoginStatus(boolean loginStatus) {
	    this.loginStatus = loginStatus;
	  }

    /**
     * Gets the widgets ids.
     *
     * @return the widgetsIds
     */
    public String getWidgetsIds() {
      return widgetsIds;
    }

    /**
     * Sets the widgets ids.
     *
     * @param widgetsIds the widgetsIds to set
     */
    public void setWidgetsIds(String widgetsIds) {
      this.widgetsIds = widgetsIds;
    }

	/**
	 * @return the myNMAjaxResponse
	 */
	public MyNMRespObj getMyNMAjaxResponse() {
		return myNMAjaxResponse;
	}

	/**
	 * @param myNMAjaxResponse the myNMAjaxResponse to set
	 */
	public void setMyNMAjaxResponse(MyNMRespObj myNMAjaxResponse) {
		this.myNMAjaxResponse = myNMAjaxResponse;
	}
  
}
