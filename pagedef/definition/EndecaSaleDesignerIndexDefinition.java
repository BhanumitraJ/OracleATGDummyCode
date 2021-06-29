package com.nm.commerce.pagedef.definition;

public class EndecaSaleDesignerIndexDefinition extends EndecaTemplatePageDefinition {
  
  private String numColumns;
  private Boolean onlyDisplayLastBackToTop;
  private Boolean useStructuredAZ;
  
  private String topCenterContentPath;
  private String leftTopContentPath;
  private String designerIndexLetterText;
  private String designerBodyContentPath;
  private String designerIndexVendorColumn;
  private String leftBottomContentPath;
  private String rightTopContentPath;
  private String rightBottomContentPath;
  private Boolean useSiloSpecificPromos;
  private Boolean queryOnName;
  private Boolean event = false;
  
  private String designerPromoFile;
  private String internationalDesignerPromoFile;
  
  public String getNumColumns() {
    return numColumns;
  }
  
  public void setNumColumns(String numColumns) {
    this.numColumns = numColumns;
  }
  
  public Boolean getOnlyDisplayLastBackToTop() {
    return getValue(onlyDisplayLastBackToTop, basis, "getOnlyDisplayLastBackToTop");
  }
  
  public void setOnlyDisplayLastBackToTop(Boolean onlyDisplayLastBackToTop) {
    this.onlyDisplayLastBackToTop = onlyDisplayLastBackToTop;
  }
  
  public Boolean getUseStructuredAZ() {
    return this.getValue(useStructuredAZ, basis, "getUseStructuredAZ");
  }
  
  public void setUseStructuredAZ(Boolean useStructuredAZ) {
    this.useStructuredAZ = useStructuredAZ;
  }
  
  public String getTopCenterContentPath() {
    return getValue(topCenterContentPath, basis, "getTopCenterContentPath");
  }
  
  public void setTopCenterContentPath(String topCenterContentPath) {
    this.topCenterContentPath = topCenterContentPath;
  }
  
  public String getLeftTopContentPath() {
    return getValue(leftTopContentPath, basis, "getLeftTopContentPath");
  }
  
  public void setLeftTopContentPath(String leftTopContentPath) {
    this.leftTopContentPath = leftTopContentPath;
  }
  
  public String getDesignerIndexLetterText() {
    return getValue(designerIndexLetterText, basis, "getDesignerIndexLetterText");
  }
  
  public void setDesignerIndexLetterText(String designerIndexLetterText) {
    this.designerIndexLetterText = designerIndexLetterText;
  }
  
  public String getDesignerBodyContentPath() {
    return getValue(designerBodyContentPath, basis, "getDesignerBodyContentPath");
  }
  
  public void setDesignerBodyContentPath(String designerBodyContentPath) {
    this.designerBodyContentPath = designerBodyContentPath;
  }
  
  public String getDesignerIndexVendorColumn() {
    return getValue(designerIndexVendorColumn, basis, "getDesignerIndexVendorColumn");
  }
  
  public void setDesignerIndexVendorColumn(String designerIndexVendorColumn) {
    this.designerIndexVendorColumn = designerIndexVendorColumn;
  }
  
  public String getLeftBottomContentPath() {
    return getValue(leftBottomContentPath, basis, "getLeftBottomContentPath");
  }
  
  public void setLeftBottomContentPath(String leftBottomContentPath) {
    this.leftBottomContentPath = leftBottomContentPath;
  }
  
  public String getRightTopContentPath() {
    return getValue(rightTopContentPath, basis, "getRightTopContentPath");
  }
  
  public void setRightTopContentPath(String rightTopContentPath) {
    this.rightTopContentPath = rightTopContentPath;
  }
  
  public String getRightBottomContentPath() {
    return getValue(rightBottomContentPath, basis, "getRightBottomContentPath");
  }
  
  public void setRightBottomContentPath(String rightBottomContentPath) {
    this.rightBottomContentPath = rightBottomContentPath;
  }
  
  public void setUseSiloSpecificPromos(Boolean useSiloSpecificPromos) {
    this.useSiloSpecificPromos = useSiloSpecificPromos;
  }
  
  public Boolean getUseSiloSpecificPromos() {
    return getValue(useSiloSpecificPromos, basis, "getUseSiloSpecificPromos");
  }
  
  public void setQueryOnName(Boolean queryOnName) {
    this.queryOnName = queryOnName;
  }
  
  public Boolean getQueryOnName() {
    return getValue(queryOnName, basis, "getQueryOnName");
  }
  
  public Boolean getEvent() {
    return getValue(event, basis, "event");
  }
  
  public void setEvent(Boolean event) {
    this.event = event;
  }
  
  public String getDesignerPromoFile() {
    return designerPromoFile;
  }
  
  public void setDesignerPromoFile(String designerPromoFile) {
    this.designerPromoFile = designerPromoFile;
  }
  
  public String getInternationalDesignerPromoFile() {
    return internationalDesignerPromoFile;
  }
  
  public void setInternationalDesignerPromoFile(String internationalDesignerPromoFile) {
    this.internationalDesignerPromoFile = internationalDesignerPromoFile;
  }
  
}
