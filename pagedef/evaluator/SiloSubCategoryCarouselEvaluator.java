package com.nm.commerce.pagedef.evaluator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.jsp.PageContext;

import atg.repository.RepositoryItem;
import atg.servlet.DynamoHttpServletRequest;

import com.nm.catalog.navigation.NMCategory;
import com.nm.commerce.pagedef.definition.PageDefinition;
import com.nm.commerce.pagedef.definition.SiloTemplatePageDefinition;
import com.nm.commerce.pagedef.model.TemplatePageModel;

public class SiloSubCategoryCarouselEvaluator extends SiloProductCarouselEvaluator {

	public static final String FEATURE_SUBCATS_NAME = "Feature Subcats";

	@Override
	public boolean evaluate(PageDefinition pageDefinition,PageContext pageContext) throws Exception {
		boolean redirect = super.evaluate(pageDefinition, pageContext);
		return redirect;
	}

	@Override
	public void evaluateContent(final PageContext context) throws Exception {
		super.evaluateContent(context);
		
		final DynamoHttpServletRequest request = this.getRequest();
		final TemplatePageModel templatePageModel = (TemplatePageModel) getPageModel();
		final SiloTemplatePageDefinition siloTemplatePageDefinition = (SiloTemplatePageDefinition) templatePageModel.getPageDefinition();
		NMCategory category = templatePageModel.getCategory();

		List<NMCategory> childCategories = null;
		NMCategory thumbnailParentCategory = getParentCategory(category,templatePageModel, request, siloTemplatePageDefinition);
		if (thumbnailParentCategory != null) {
			thumbnailParentCategory.setFilterCategoryGroup(getFilterCategoryGroup());
			childCategories = thumbnailParentCategory.getOrderedChildCategories(true);
		}
		String countKey = childCategories == null ? "0" : String.valueOf(childCategories.size());
		int restrictElementCount = siloTemplatePageDefinition.getRestrictElementCount();
		if (restrictElementCount == 0 || childCategories == null || childCategories.size() == 0) {
			templatePageModel.setDisplayableSubcatList(new ArrayList<RepositoryItem>());
		} else {
			templatePageModel.setDisplayableSubcatList(childCategories);
		}

		calculateThumbnailIteration(templatePageModel, siloTemplatePageDefinition,getRequest());

	}

	/*
	 * adapted from com.nm.droplet.CategoryAtIndex.java In case child items need
	 * to be pulled out from the descendant of the active category
	 */
	private NMCategory getParentCategory(NMCategory category,TemplatePageModel model, DynamoHttpServletRequest req,SiloTemplatePageDefinition siloTemplatePageDefinition) {
		NMCategory thumbnailParentCategory = category;

		// if alternateSaleCategoryId is specified, substitute it only for sale
		// silo category
		/*
		 * String alternateSaleCategoryId =
		 * subcategoryPageDefinition.getAlternateSaleCategoryId(); if
		 * (!isEmpty(alternateSaleCategoryId)) { BrandSpecs specs = (BrandSpecs)
		 * req.resolveName("/nm/utils/BrandSpecs"); String saleSiloId =
		 * specs.getSaleCategoryId(); if (category.getId().equals(saleSiloId)) {
		 * NMCategory categoryItem =
		 * CategoryHelper.getInstance().getNMCategory(alternateSaleCategoryId);
		 * if (categoryItem != null) { thumbnailParentCategory = categoryItem; }
		 * } }
		 */
		String parentIndex = siloTemplatePageDefinition.getThumbnailParentIndex();
		if (!isEmpty(parentIndex)) {
			try {
				// List<NMCategory> children
				// =initializeFilteredCategories(category, model, req);
				List<NMCategory> children = category.getChildCategories(false);
				if (children != null) {
					if (parentIndex.equalsIgnoreCase("last")) {
						thumbnailParentCategory = children.get(children.size() - 1);
					} else if (parentIndex.equalsIgnoreCase("feature")) {
						// if the "Feature Subcats" category does not exist we
						// don't want to show any thumbnails
						thumbnailParentCategory = null;
						boolean foundCat = false;
						for (int i = 0; i < children.size() && foundCat == false; i++) {
							NMCategory subcat = children.get(i);
							// The Feature Subcats is an auto-generated category
							// via button in CM2 so the display name should
							// match
							if (subcat.getDisplayName().equalsIgnoreCase(FEATURE_SUBCATS_NAME)) {
								thumbnailParentCategory = subcat;
								foundCat = true;
							}
						}

					} else {
						try {
							Integer index = new Integer(parentIndex);
							if (index < children.size()) {
								thumbnailParentCategory = children.get(index);
							} else {
								thumbnailParentCategory = children.get(children.size() - 1);
							}

						} catch (Exception e) {
							thumbnailParentCategory = children.get(0);
						}
					}
				}
			} catch (Exception e) {
				log.error(e.getMessage());
				e.printStackTrace();
			}
		}

		if (thumbnailParentCategory != null) {
			String thumbnailParentCategoryId = thumbnailParentCategory.getId();
			String thumbnailMasterCategoryId = thumbnailParentCategory.getParentId();
			model.setThumbnailParentCategoryId(thumbnailParentCategoryId);
			model.setThumbnailMasterCategoryId(thumbnailMasterCategoryId);
		}
		return thumbnailParentCategory;
	}

	private void calculateThumbnailIteration(TemplatePageModel pageModel,SiloTemplatePageDefinition firstPage,DynamoHttpServletRequest request) throws IOException {

		// determine the elements to display for the current page.
		int pageNumber = 1;
		int firstThumbnailIndex = 1;
		int itemsOnFirstPage = pageModel.getDisplayableSubcatList().size();
		Integer minimum = firstPage.getMinimumNumberCategories();
		int restrictElementCount = firstPage.getRestrictElementCount();

		int columnCount = firstPage.getColumnCount();
		boolean isSuperViewAll = pageModel.isSuperViewAll();

		if (isSuperViewAll) {
			// itemsOnFirstPage = pageModel.getDisplayableSubcatList().size();

		} else {
			if (itemsOnFirstPage < minimum) {
				pageModel.getDisplayableSubcatList().clear();

			} else if (restrictElementCount != -1
					&& restrictElementCount < pageModel.getDisplayableSubcatList().size()) {
				itemsOnFirstPage = restrictElementCount;
			}

			NMCategory category = pageModel.getCategory();
			if (!isEmpty(firstPage.getGraphicBlockPath())&& category.getFlgStaticImage()) {
				pageModel.setShowGraphicBlock(true);
			}
		}
		int thumbnailCount = itemsOnFirstPage;
		pageModel.setPageNumber(pageNumber);
		pageModel.setFirstThumbnailIndex(firstThumbnailIndex);
		pageModel.setThumbnailCount(thumbnailCount);
		pageModel.setColumnCount(columnCount);
	}

}
