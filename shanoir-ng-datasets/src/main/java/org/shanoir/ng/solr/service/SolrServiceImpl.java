/**
 * Shanoir NG - Import, manage and share neuroimaging data
 * Copyright (C) 2009-2019 Inria - https://www.inria.fr/
 * Contact us on https://project.inria.fr/shanoir/
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see https://www.gnu.org/licenses/gpl-3.0.html
 */

/**
 * NOTE: This class is auto generated by the swagger code generator program (2.2.3).
 * https://github.com/swagger-api/swagger-codegen
 * Do not edit the class manually.
 */
package org.shanoir.ng.solr.service;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.shanoir.ng.shared.dateTime.DateTimeUtils;
import org.shanoir.ng.shared.exception.RestServiceException;
import org.shanoir.ng.shared.paging.PageImpl;
import org.shanoir.ng.shared.security.rights.StudyUserRight;
import org.shanoir.ng.solr.model.ShanoirMetadata;
import org.shanoir.ng.solr.model.ShanoirSolrDocument;
import org.shanoir.ng.solr.model.ShanoirSolrFacet;
import org.shanoir.ng.solr.repository.ShanoirMetadataRepository;
import org.shanoir.ng.solr.repository.SolrRepository;
import org.shanoir.ng.study.rights.StudyUserRightsRepository;
import org.shanoir.ng.utils.KeycloakUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.solr.core.query.result.SolrResultPage;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author yyao
 *
 */
@Service
public class SolrServiceImpl implements SolrService {

	//	private static final Logger LOG = LoggerFactory.getLogger(SolrServiceImpl.class);

	@Autowired
	private SolrRepository solrRepository;

	@Autowired
	private ShanoirMetadataRepository shanoirMetadataRepository;

	@Autowired
	private StudyUserRightsRepository rightsRepository;

	@Transactional
	@Override
	public void addToIndex (final ShanoirSolrDocument document) {
		solrRepository.save(document);
	}

	@Transactional
	@Override
	public void deleteFromIndex(Long datasetId) {
		solrRepository.deleteByDatasetId(datasetId);
	}
	
	@Transactional
	@Override
	public void deleteFromIndex(List<Long> datasetIds) {
		solrRepository.deleteByDatasetIdIn(datasetIds);
	}

	@Transactional
	public void deleteAll() {
		solrRepository.deleteAll();
	}

	@Transactional
	@Override
	@Scheduled(cron = "0 0 6 * * *", zone="Europe/Paris")
	public void indexAll() {
		// 1. delete all
		deleteAll();

		// 2. get all datasets
		List<ShanoirMetadata> documents = shanoirMetadataRepository.findAllAsSolrDoc();
		Iterator<ShanoirMetadata> docIt = documents.iterator();
		while (docIt.hasNext()) {
			ShanoirMetadata shanoirMetadata = docIt.next();
			ShanoirSolrDocument doc = getShanoirSolrDocument(shanoirMetadata);
			addToIndex(doc);
		}
	}
	
	private ShanoirSolrDocument getShanoirSolrDocument(ShanoirMetadata shanoirMetadata) {
		return new ShanoirSolrDocument(shanoirMetadata.getDatasetId(), shanoirMetadata.getDatasetName(),
				shanoirMetadata.getDatasetType(), shanoirMetadata.getDatasetNature(), DateTimeUtils.localDateToDate(shanoirMetadata.getDatasetCreationDate()),
				shanoirMetadata.getExaminationComment(), DateTimeUtils.localDateToDate(shanoirMetadata.getExaminationDate()),
				shanoirMetadata.getSubjectName(), shanoirMetadata.getStudyName(), shanoirMetadata.getStudyId());
	}

	@Transactional
	@Override
	public void indexDataset(Long datasetId) {
		// Get all associated datasets and index them to solr
		ShanoirMetadata shanoirMetadata = shanoirMetadataRepository.findOneSolrDoc(datasetId);
		ShanoirSolrDocument doc = getShanoirSolrDocument(shanoirMetadata);
		solrRepository.save(doc);
	}

	@Transactional
	@Override
	public SolrResultPage<ShanoirSolrDocument> findAll(Pageable pageable) {
		SolrResultPage<ShanoirSolrDocument> result = null;
		pageable = prepareTextFields(pageable);
		if (KeycloakUtil.getTokenRoles().contains("ROLE_ADMIN")) {
			result = solrRepository.findAllDocsAndFacets(pageable);
		} else {
			List<Long> studyIds = rightsRepository.findDistinctStudyIdByUserId(KeycloakUtil.getTokenUserId(), StudyUserRight.CAN_SEE_ALL.getId());
			if (studyIds.isEmpty()) {
				return new SolrResultPage<ShanoirSolrDocument>(Collections.emptyList());
			}
			result = solrRepository.findByStudyIdIn(studyIds, pageable);
		}
		return result;
	}

	@Transactional
	@Override
	public SolrResultPage<ShanoirSolrDocument> facetSearch(ShanoirSolrFacet facet, Pageable pageable) throws RestServiceException {
		SolrResultPage<ShanoirSolrDocument> result = null;
		pageable = prepareTextFields(pageable);
		if (KeycloakUtil.getTokenRoles().contains("ROLE_ADMIN")) {
			result = solrRepository.findByFacetCriteria(facet, pageable);
		} else {
			List<Long> studyIds = rightsRepository.findDistinctStudyIdByUserId(KeycloakUtil.getTokenUserId(), StudyUserRight.CAN_SEE_ALL.getId());
			result = solrRepository.findByStudyIdInAndFacetCriteria(studyIds, facet, pageable);
		}
		return result;
	}

	private Pageable prepareTextFields(Pageable pageable) {
		for (Sort.Order order : pageable.getSort()) {
			if (order.getProperty().equals("studyName") || order.getProperty().equals("subjectName")
					|| order.getProperty().equals("datasetName") || order.getProperty().equals("datasetNature")
					|| order.getProperty().equals("datasetType") || order.getProperty().equals("examinationComment")) {
				pageable = new PageRequest(pageable.getPageNumber(), pageable.getPageSize(),
						order.getDirection(), order.getProperty().concat("_str"));
			} else if (order.getProperty().equals("id")) {
				pageable = new PageRequest(pageable.getPageNumber(), pageable.getPageSize(),
						order.getDirection(), "datasetId");
			}
		}
		return pageable;
	}

	@Override
	public Page<ShanoirSolrDocument> getByIdIn(List<Long> datasetIds, Pageable pageable) {
		if (datasetIds.isEmpty()) {
			return new PageImpl<ShanoirSolrDocument>();
		}		
		Page<ShanoirSolrDocument> result;
		pageable = prepareTextFields(pageable);
		if (KeycloakUtil.getTokenRoles().contains("ROLE_ADMIN")) {
			result = solrRepository.findByDatasetIdIn(datasetIds, pageable);
		} else {
			List<Long> studyIds = rightsRepository.findDistinctStudyIdByUserId(KeycloakUtil.getTokenUserId(), StudyUserRight.CAN_SEE_ALL.getId());
			if (studyIds.isEmpty()) {
				return new PageImpl<ShanoirSolrDocument>();
			}
			result = solrRepository.findByStudyIdInAndDatasetIdIn(studyIds, datasetIds, pageable);
		}
		return result;
	}

}
