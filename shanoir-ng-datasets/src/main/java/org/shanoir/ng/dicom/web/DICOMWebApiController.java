package org.shanoir.ng.dicom.web;

import java.util.Iterator;
import java.util.Map;

import org.apache.solr.common.StringUtils;
import org.shanoir.ng.dicom.web.service.DICOMWebService;
import org.shanoir.ng.examination.model.Examination;
import org.shanoir.ng.examination.service.ExaminationService;
import org.shanoir.ng.shared.exception.RestServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

@Controller
public class DICOMWebApiController implements DICOMWebApi {
	
	private static final Logger LOG = LoggerFactory.getLogger(DICOMWebApiController.class);
	
	@Autowired
	private ExaminationService examinationService;
	
	@Autowired
	private DICOMWebService dicomWebService;

	@Autowired
	private StudyInstanceUIDHandler studyInstanceUIDHandler;
	
	private ObjectMapper mapper = new ObjectMapper();
	
	@Override
	public ResponseEntity<String> findPatients() throws RestServiceException {
		return null;
	}

	@Override
	public ResponseEntity<String> findStudies(Map<String, String> allParams) throws RestServiceException, JsonMappingException, JsonProcessingException {
		int offset = Integer.valueOf(allParams.get("offset"));
		int limit = Integer.valueOf(allParams.get("limit"));
		Pageable pageable = PageRequest.of(offset, limit);
		// Search for studies with patient name (DICOM patientID does not make sense in case of Shanoir)
		String patientName = allParams.get("PatientName");
		Page<Examination> examinations = examinationService.findPage(pageable, patientName);
		if (examinations.getContent().isEmpty()) {
			return new ResponseEntity<>(HttpStatus.NO_CONTENT);
		}
		StringBuffer studies = new StringBuffer();
		studies.append("[");
		Iterator<Examination> iterator = examinations.iterator();
		while (iterator.hasNext()) {
			Examination examination = iterator.next();
			String studyInstanceUID = studyInstanceUIDHandler.findStudyInstanceUIDFromCacheOrDatabase(examination.getId());
			if (studyInstanceUID != null) {
				String studyJson = dicomWebService.findStudy(studyInstanceUID);
				JsonNode root = mapper.readTree(studyJson);
				replaceStudyInstanceUIDsWithExaminationIds(root, examination.getId(), true);
				studyJson = mapper.writeValueAsString(root);
				studyJson = studyJson.substring(1, studyJson.length() - 1);
				studies.append(studyJson);
				if (iterator.hasNext()) {
					studies.append(",");
				}
			}
		}
		studies.append("]");
		return new ResponseEntity<String>(studies.toString(), HttpStatus.OK);
	}

	@Override
	public ResponseEntity<String> findSeries() throws RestServiceException {
		return null;
	}

	@Override
	public ResponseEntity<String> findSeriesOfStudy(Long examinationId)
			throws RestServiceException, JsonMappingException, JsonProcessingException {
		String studyInstanceUID = studyInstanceUIDHandler.findStudyInstanceUIDFromCacheOrDatabase(examinationId);
		if (studyInstanceUID != null) {
			String response = dicomWebService.findSeriesOfStudy(studyInstanceUID);
			JsonNode root = mapper.readTree(response);
			replaceStudyInstanceUIDsWithExaminationIds(root, examinationId, false);
			return new ResponseEntity<String>(mapper.writeValueAsString(root), HttpStatus.OK);
		} else {
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}
	}
	
	private void replaceStudyInstanceUIDsWithExaminationIds(JsonNode root, Long examinationId, boolean studyLevel) {
		if (root.isObject()) {
			Iterator<String> fieldNames = root.fieldNames();
			while (fieldNames.hasNext()) {
				String fieldName = fieldNames.next();
				// find attribute: StudyInstanceUID
				if (fieldName.equals("0020000D")) {
					JsonNode studyInstanceUIDNode = root.get(fieldName);
					ArrayNode studyInstanceUIDArray = (ArrayNode) studyInstanceUIDNode.path("Value");
					for (int i = 0; i < studyInstanceUIDArray.size(); i++) {
						studyInstanceUIDArray.remove(i);
						studyInstanceUIDArray.add(examinationId.toString());
					}
				}
				// find attribute: RetrieveURL
				if (fieldName.equals("00081190")) {
					JsonNode retrieveURLNode = root.get(fieldName);
					ArrayNode retrieveURLArray = (ArrayNode) retrieveURLNode.path("Value");
					for (int i = 0; i < retrieveURLArray.size(); i++) {
						JsonNode arrayElement = retrieveURLArray.get(i);
						String retrieveURL = arrayElement.asText();
						if (studyLevel) { // study level
							retrieveURL = retrieveURL.replaceFirst("/studies/(.*)", "/studies/" + examinationId);
							retrieveURLArray.remove(i);
							retrieveURLArray.add(retrieveURL);
						} else { // serie level
							retrieveURL = retrieveURL.replaceFirst("/studies/(.*)/series/", "/studies/" + examinationId + "/series/");
							retrieveURLArray.remove(i);
							retrieveURLArray.add(retrieveURL);
						}
					}
				}
			}
		} else if (root.isArray()) {
			ArrayNode arrayNode = (ArrayNode) root;
			for (int i = 0; i < arrayNode.size(); i++) {
				JsonNode arrayElement = arrayNode.get(i);
				replaceStudyInstanceUIDsWithExaminationIds(arrayElement, examinationId, studyLevel);
			}
		}
	}
	
	@Override
	public ResponseEntity<String> findSerieMetadataOfStudy(Long examinationId, String serieId)
			throws RestServiceException, JsonMappingException, JsonProcessingException {
		String studyInstanceUID = studyInstanceUIDHandler.findStudyInstanceUIDFromCacheOrDatabase(examinationId);
		if (studyInstanceUID != null && serieId != null) {
			String response = dicomWebService.findSerieMetadataOfStudy(studyInstanceUID, serieId);
			JsonNode root = mapper.readTree(response);
			replaceStudyInstanceUIDsWithExaminationIds(root, examinationId, false);
			return new ResponseEntity<String>(mapper.writeValueAsString(root), HttpStatus.OK);
		} else {
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}
	}
	
	@Override
	public ResponseEntity findFrameOfStudyOfSerieOfInstance(Long examinationId, String serieInstanceUID,
			String sopInstanceUID, String frame) throws RestServiceException {
		String studyInstanceUID = studyInstanceUIDHandler.findStudyInstanceUIDFromCacheOrDatabase(examinationId);
		if (!StringUtils.isEmpty(studyInstanceUID) && !StringUtils.isEmpty(serieInstanceUID)
				&& !StringUtils.isEmpty(sopInstanceUID) && !StringUtils.isEmpty(frame))  {
			return dicomWebService.findFrameOfStudyOfSerieOfInstance(studyInstanceUID, serieInstanceUID, sopInstanceUID, frame);
		} else {
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}
	}

	@Override
	public ResponseEntity<String> findInstancesOfStudyOfSerie(String studyInstanceUID, String serieInstanceUID)
			throws RestServiceException {
		return null;
	}

	@Override
	public ResponseEntity<String> findInstances() throws RestServiceException {
		return null;
	}

	@Override
	public ResponseEntity<String> findInstancesOfStudy(String studyInstanceUID) throws RestServiceException {
		return null;
	}

}
