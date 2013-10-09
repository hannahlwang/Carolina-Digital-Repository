package edu.unc.lib.dl.ui.controller;

import java.util.List;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.dl.search.solr.exception.InvalidHierarchicalFacetException;
import edu.unc.lib.dl.search.solr.model.HierarchicalBrowseRequest;
import edu.unc.lib.dl.search.solr.model.HierarchicalBrowseResultResponse;
import edu.unc.lib.dl.search.solr.model.SearchState;
import edu.unc.lib.dl.search.solr.util.SearchFieldKeys;

/**
 * Base structure browse controller.  
 * This is separate from StructureResultsController to allow for other controllers to use the same base functionality,
 * since controllers cannot inherit from each other. 
 * @author bbpennel
 *
 */
public class AbstractStructureResultsController extends AbstractSolrSearchController {
	private static final Logger LOG = LoggerFactory.getLogger(AbstractStructureResultsController.class);

	protected List<String> tierResultFieldsList;

	@PostConstruct
	public void init() {
		tierResultFieldsList = searchSettings.resultFields.get("structure");
	}
	
	protected HierarchicalBrowseResultResponse getStructureResult(String pid, boolean includeFiles,
			boolean collectionMode, boolean retrieveFacets, HttpServletRequest request) {
		int depth;
		try {
			depth = Integer.parseInt(request.getParameter("depth"));
			if (depth > searchSettings.structuredDepthMax)
				depth = searchSettings.structuredDepthMax;
		} catch (Exception e) {
			depth = searchSettings.structuredDepthDefault;
		}

		// Request object for the search
		HierarchicalBrowseRequest browseRequest = new HierarchicalBrowseRequest(depth);
		browseRequest.setRetrieveFacets(retrieveFacets);
		if (retrieveFacets) {
			browseRequest.setSearchState(this.searchStateFactory.createHierarchicalBrowseSearchState(request
					.getParameterMap()));

		} else {
			browseRequest.setSearchState(this.searchStateFactory.createStructureBrowseSearchState(request
					.getParameterMap()));
		}
		if (pid != null)
			browseRequest.setRootPid(pid);
		browseRequest.setIncludeFiles(includeFiles);

		SearchState searchState = browseRequest.getSearchState();

		try {
			searchActionService.executeActions(searchState, request.getParameterMap());
		} catch (InvalidHierarchicalFacetException e) {
			LOG.debug("An invalid facet was provided: " + request.getQueryString(), e);
		}

		if (pid == null && !searchState.getFacets().containsKey(SearchFieldKeys.ANCESTOR_PATH.name())) {
			browseRequest.setRetrievalDepth(1);
		}

		HierarchicalBrowseResultResponse resultResponse = null;
		if (collectionMode)
			resultResponse = queryLayer.getStructureToParentCollection(browseRequest);
		else
			resultResponse = queryLayer.getHierarchicalBrowseResults(browseRequest);

		resultResponse.setSearchState(searchState);

		if (retrieveFacets)
			queryLayer.populateBreadcrumbs(browseRequest, resultResponse);
		return resultResponse;
	}
}