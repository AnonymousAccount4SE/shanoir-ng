package org.shanoir.ng.study;

import java.util.List;

import org.shanoir.ng.shared.exception.RestServiceException;
import org.shanoir.ng.study.dto.SimpleStudyDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2017-03-23T10:35:29.288Z")

@Api(value = "study", description = "the study API")
@RequestMapping("/study")
public interface StudyApi {

	@ApiOperation(value = "", notes = "Deletes a study", response = Void.class, tags = {})
	@ApiResponses(value = { @ApiResponse(code = 204, message = "study deleted", response = Void.class),
			@ApiResponse(code = 401, message = "unauthorized", response = Void.class),
			@ApiResponse(code = 403, message = "forbidden", response = Void.class),
			@ApiResponse(code = 404, message = "no study found", response = Void.class),
			@ApiResponse(code = 500, message = "unexpected error", response = Void.class) })
	@RequestMapping(value = "/{studyId}", produces = { "application/json" }, method = RequestMethod.DELETE)
	ResponseEntity<Void> deleteStudy(
			@ApiParam(value = "id of the study", required = true) @PathVariable("studyId") Long studyId);

	@ApiOperation(value = "", notes = "Returns all the studies", response = Study.class, responseContainer = "List", tags = {})
	@ApiResponses(value = { @ApiResponse(code = 200, message = "found studies", response = Study.class),
			@ApiResponse(code = 204, message = "no study found", response = Study.class),
			@ApiResponse(code = 401, message = "unauthorized", response = Study.class),
			@ApiResponse(code = 403, message = "forbidden", response = Study.class),
			@ApiResponse(code = 500, message = "unexpected error", response = Study.class) })
	@RequestMapping(value = "/all", produces = { "application/json" }, method = RequestMethod.GET)
	ResponseEntity<List<Study>> findStudies();

	@ApiOperation(value = "", notes = "If exists, returns the studies that the user is allowed to see", response = Study.class, tags = {})
	@ApiResponses(value = { @ApiResponse(code = 200, message = "found studies", response = Study.class),
			@ApiResponse(code = 401, message = "unauthorized", response = Study.class),
			@ApiResponse(code = 403, message = "forbidden", response = Study.class),
			@ApiResponse(code = 404, message = "no study found", response = Study.class),
			@ApiResponse(code = 500, message = "unexpected error", response = Study.class) })
	@RequestMapping(value = "/list", produces = { "application/json" }, method = RequestMethod.GET)
	ResponseEntity<List<Study>> findStudiesByUserId();

	@ApiOperation(value = "", notes = "If exists, returns the studies with theirs study cards that the user is allowed to see", response = SimpleStudyDTO.class, tags = {})
	@ApiResponses(value = { @ApiResponse(code = 200, message = "found studies", response = SimpleStudyDTO.class),
			@ApiResponse(code = 401, message = "unauthorized", response = SimpleStudyDTO.class),
			@ApiResponse(code = 403, message = "forbidden", response = SimpleStudyDTO.class),
			@ApiResponse(code = 404, message = "no study found", response = SimpleStudyDTO.class),
			@ApiResponse(code = 500, message = "unexpected error", response = SimpleStudyDTO.class) })
	@RequestMapping(value = "/listwithcards", produces = { "application/json" }, method = RequestMethod.GET)
	ResponseEntity<List<SimpleStudyDTO>> findStudiesWithStudyCardsByUserId();

	@ApiOperation(value = "", notes = "If exists, returns the study corresponding to the given id", response = Study.class, tags = {})
	@ApiResponses(value = { @ApiResponse(code = 200, message = "found study", response = Study.class),
			@ApiResponse(code = 401, message = "unauthorized", response = Study.class),
			@ApiResponse(code = 403, message = "forbidden", response = Study.class),
			@ApiResponse(code = 404, message = "no study found", response = Study.class),
			@ApiResponse(code = 500, message = "unexpected error", response = Study.class) })
	@RequestMapping(value = "/{studyId}", produces = { "application/json" }, method = RequestMethod.GET)
	ResponseEntity<Study> findStudyById(
			@ApiParam(value = "id of the study", required = true) @PathVariable("studyId") Long studyId);

	@ApiOperation(value = "", notes = "Saves a new study", response = Study.class, tags = {})
	@ApiResponses(value = { @ApiResponse(code = 200, message = "created study", response = Study.class),
			@ApiResponse(code = 401, message = "unauthorized", response = Study.class),
			@ApiResponse(code = 403, message = "forbidden", response = Study.class),
			@ApiResponse(code = 422, message = "bad parameters", response = Study.class),
			@ApiResponse(code = 500, message = "unexpected error", response = Study.class) })
	@RequestMapping(value = "", produces = { "application/json" }, consumes = {
			"application/json" }, method = RequestMethod.POST)
	ResponseEntity<Study> saveNewStudy(@ApiParam(value = "study to create", required = true) @RequestBody Study study)
			throws RestServiceException;

	@ApiOperation(value = "", notes = "Updates a study", response = Void.class, tags = {})
	@ApiResponses(value = { @ApiResponse(code = 204, message = "study updated", response = Void.class),
			@ApiResponse(code = 401, message = "unauthorized", response = Void.class),
			@ApiResponse(code = 403, message = "forbidden", response = Void.class),
			@ApiResponse(code = 422, message = "bad parameters", response = Void.class),
			@ApiResponse(code = 500, message = "unexpected error", response = Void.class) })
	@RequestMapping(value = "/{studyId}", produces = { "application/json" }, consumes = {
			"application/json" }, method = RequestMethod.PUT)
	ResponseEntity<Void> updateStudy(
			@ApiParam(value = "id of the study", required = true) @PathVariable("studyId") Long studyId,
			@ApiParam(value = "study to update", required = true) @RequestBody Study study) throws RestServiceException;

}