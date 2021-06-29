package com.nm.commerce.beans;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.nm.commerce.NMProfile;
import com.nm.sitemessages.Message;
import com.nm.sitemessages.OmnitureData;

public class BaseResultBean {
  
  private List<Message> messages = new ArrayList<Message>();
  private NMProfile profile;
  private Map<String, String> additionalAttributes = new HashMap<String, String>();
  private List<OmnitureData> omnitureData = new ArrayList<OmnitureData>();

  
  public Map<String, String> getAdditionalAttributes() {
    return additionalAttributes;
  }
  
  public void setAdditionalAttributes(Map<String, String> additionalAttributes) {
    this.additionalAttributes = additionalAttributes;
  }
  
  public List<Message> getMessages() {
    return messages;
  }
  
  public void setMessages(List<Message> messages) {
    this.messages = messages;
  }
  
  public NMProfile getProfile() {
    return profile;
  }
  
  public void setProfile(NMProfile profile) {
    this.profile = profile;
  }

/**
 * @return the omnitureData
 */
public List<OmnitureData> getOmnitureData() {
	return omnitureData;
}

/**
 * @param omnitureData the omnitureData to set
 */
public void setOmnitureData(List<OmnitureData> omnitureData) {
	this.omnitureData = omnitureData;
}
  
}
