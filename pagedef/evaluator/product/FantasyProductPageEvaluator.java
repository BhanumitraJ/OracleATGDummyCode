package com.nm.commerce.pagedef.evaluator.product;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import javax.servlet.jsp.PageContext;

import com.nm.catalog.navigation.CategoryHelper;
import com.nm.catalog.navigation.NMCategory;
import com.nm.commerce.pagedef.definition.FantasyProductPageDefinition;
import com.nm.commerce.pagedef.definition.PageDefinition;
import com.nm.commerce.pagedef.evaluator.SimplePageEvaluator;
import com.nm.commerce.pagedef.model.FantasyProductPageModel;
import com.nm.commerce.pagedef.model.PageModel;
import com.nm.utils.NmoUtils;

/**
 * Evaluate fantasy product page for Christmas Book Set a FantasyProduct object in page model containing image paths to be used in jsp page to display the product images Also set return url in page
 * model to return to the category page that led to the fantasy product page.
 * 
 * @author nmjh94 09-05-2012
 */

public class FantasyProductPageEvaluator extends SimplePageEvaluator {
  private final String defaultReturnLinkText = "The Christmas Book";
  
  @Override
  public boolean evaluate(PageDefinition pageDefinition, PageContext pageContext) throws Exception {
    if (!super.evaluate(pageDefinition, pageContext)) {
      return false;
    }
    
    FantasyProductPageModel model = (FantasyProductPageModel) getPageModel();
    FantasyProductPageDefinition definition = (FantasyProductPageDefinition) pageDefinition;
    String productId = validateParam(getRequest().getParameter("cid"));
    FantasyProduct fprod = new FantasyProduct();
    if (!NmoUtils.isEmpty(productId)) {
      fprod.setGiftId("NM" + productId.substring(6));
    } else {
      fprod.setGiftId(definition.getDefaultFantasyProduct());
    }
    
    buildReturnUrl(model, definition);
    generateImgPaths(model, definition, fprod);
    model.setFproduct(fprod);
    return true;
  }
  
  /**
   * build return url based on request parameters request parameters may be: r return to cat id (required) rDesc override link desc value (optional) rParams additional values for new link (optional)
   * 
   * The "r" parameter is a category id, used for CategoryLookup The "rdesc" parameter is an override value for the link text (default is cat display name) The "rparams" parameter contains override
   * and additional values
   * 
   * request url example: "?cid=CBF12_O1234&r=cat123&rdesc=Our%20Christmas%20Book&rparams=page%3D23,something%3Danything&..."
   * 
   * @param model
   * @param definition
   * @throws Exception
   */
  
  private void buildReturnUrl(FantasyProductPageModel model, FantasyProductPageDefinition definition) throws Exception {
    
    String categoryId = getRequest().getParameter("r");
    String desc = validateParam(getRequest().getParameter("rdesc"));
    String rParam = getRequest().getParameter("rparams");
    String rLinkDesc;
    if (desc.equals("")) {
      rLinkDesc = defaultReturnLinkText;
    } else {
      rLinkDesc = desc;
    }
    String returnUrl;
    
    // if no return category provided or CB category Id is provided, or a nonexisting category ID is provided, return to CB page
    // otherwise, return to the category page of the given categoryId
    if (NmoUtils.isEmpty(categoryId)) {
      returnUrl = definition.getCBBaseUrl();
      rLinkDesc = defaultReturnLinkText;
    } else {
      
      NMCategory category = CategoryHelper.getInstance().getNMCategory(categoryId);
      if (category == null) {
        returnUrl = definition.getCBBaseUrl();
        rLinkDesc = defaultReturnLinkText;
      } else {
        if (desc.equals("")) {
          String catDisplayName = (String) category.getDisplayName();
          rLinkDesc = catDisplayName;
        }
        if (definition.getCBCategoryId().equals(categoryId)) {
          returnUrl = appendParam(rParam, categoryId, definition.getCBBaseUrl());
        } else {
          returnUrl = definition.getFantasyCateogryUrl() + "?itemId=" + categoryId;
        }
      }
    }
    
    model.setReturnUrl(returnUrl);
    model.setReturnCatDesc(rLinkDesc);
  }
  
  /**
   * Generate corresponding image src path for the fantasy gift product, according to the cidShots parameter cidShots parameter provides comma delimited list of image shot prefix for main, thumbnail
   * and larger images. e.g.:"...cidShots=a,c,e,m"
   * 
   * @param model
   * @param definition
   * @param fprod
   */
  private void generateImgPaths(FantasyProductPageModel model, FantasyProductPageDefinition definition, FantasyProduct fprod) {
    fprod.setDefaultImgPath(definition.getImgRefreshablePath() + fprod.getGiftId() + "/" + fprod.getGiftId() + "_" + definition.getDefaultShot() + ".jpg");
    String shotsParam = (String) getRequest().getParameter("cidShots");
    List<FantasyProduct.FantasyImage> images = new ArrayList<FantasyProduct.FantasyImage>();
    if (!NmoUtils.isEmpty(shotsParam)) {
      String[] shots = shotsParam.split(",");
      if (!(shots.length == 1 && "m".equals(shots[0]))) {
        String mainImg;
        String thumbnailImg;
        String largerImg;
        String zoomImg;
        for (String shot : shots) {
          shot = verifyShot(shot);
          if (!"".equals(shot)) {
            mainImg = definition.getImgRefreshablePath() + fprod.getGiftId() + "/" + fprod.getGiftId() + "_" + shot + definition.getMainShot() + ".jpg";
            thumbnailImg = definition.getImgRefreshablePath() + fprod.getGiftId() + "/" + fprod.getGiftId() + "_" + shot + definition.getThumbnailShot() + ".jpg";
            largerImg = definition.getImgRefreshablePath() + fprod.getGiftId() + "/" + fprod.getGiftId() + "_" + shot + definition.getLargerShot() + ".jpg";
            zoomImg = definition.getImgRefreshablePath() + fprod.getGiftId() + "/" + fprod.getGiftId() + "_" + shot + definition.getZoomShot() + ".jpg";
            FantasyProduct.FantasyImage image = fprod.new FantasyImage(mainImg, thumbnailImg, largerImg, shot, zoomImg);
            images.add(image);
          }
          
        }
        if (images.size() > 0) {
          model.setHasAltImg(true);
          fprod.setFantasyAltImages(images);
        }
      }
    }
  }
  
  /**
   * verify cidShots parameter: make sure it is a single letter
   * 
   * @param shot
   * @return lower case letter
   */
  private String verifyShot(String shot) {
    String verifiedShot = shot.replaceAll("\\s", "");
    Pattern pattern = Pattern.compile("[a-zA-Z]");
    if (pattern.matcher(verifiedShot).matches() && verifiedShot.length() == 1) {
      return verifiedShot.toLowerCase();
    } else {
      return "";
    }
  }
  
  @Override
  protected PageModel allocatePageModel() {
    return new FantasyProductPageModel();
  }
  
  private String appendParam(String rParams, String itemId, String baseURL) {
    
    String url = baseURL + "?itemId=" + itemId;
    if (rParams == null || rParams.equals("")) {
      return url;
    } else {
      for (String param : rParams.split(",")) {
        if (!param.trim().equals("") && param.indexOf("=") >= 0) {
          url += "&" + param;
        }
      }
    }
    
    return url;
  }
  
  /**
   * 
   * Method duplicated from GenerateRParamURL droplet. Returns an empty string if the "in" parameter contains anything besides letters, numbers, underscores, dashes, or spaces. Or if the attr or value
   * exceeds 30. Otherwise, it returns the "in" string.
   */
  
  private String validateParam(String in) {
    String out = "";
    String attr = "";
    String value = "";
    Pattern pattern = Pattern.compile("[\\w\\.\\-\\s]+");
    
    if (in != null && (!(in.equals("")))) {
      
      int equalPos = in.indexOf("=");
      
      // validate a value only
      if (equalPos < 0) {
        if (in.length() < 31) {
          if (pattern.matcher(in).matches()) {
            out = in;
          }
        }
        // validate an attribute/value pair
      } else {
        attr = in.substring(0, equalPos);
        value = in.substring(equalPos + 1);
        if (attr.length() < 31 && value.length() < 31) {
          if (pattern.matcher(attr).matches() && pattern.matcher(value).matches()) {
            out = attr + "=" + value;
          }
        }
      }
    }
    return out;
  }
  
}
