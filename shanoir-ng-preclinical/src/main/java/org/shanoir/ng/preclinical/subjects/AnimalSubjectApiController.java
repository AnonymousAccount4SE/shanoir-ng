package org.shanoir.ng.preclinical.subjects;

import java.util.List;

import javax.validation.Valid;

import org.shanoir.ng.preclinical.pathologies.subject_pathologies.SubjectPathologyService;
import org.shanoir.ng.preclinical.references.RefsService;
import org.shanoir.ng.preclinical.therapies.subject_therapies.SubjectTherapyService;
import org.shanoir.ng.shared.error.FieldErrorMap;
import org.shanoir.ng.shared.exception.ErrorDetails;
import org.shanoir.ng.shared.exception.ErrorModel;
import org.shanoir.ng.shared.exception.RestServiceException;
import org.shanoir.ng.shared.exception.ShanoirPreclinicalException;
import org.shanoir.ng.shared.validation.EditableOnlyByValidator;
import org.shanoir.ng.shared.validation.RefValueExistsValidator;
import org.shanoir.ng.shared.validation.UniqueValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import io.swagger.annotations.ApiParam;

@Controller
public class AnimalSubjectApiController implements AnimalSubjectApi {

	/**
	 * Logger
	 */
	private static final Logger LOG = LoggerFactory.getLogger(AnimalSubjectApiController.class);

	@Autowired
	private AnimalSubjectService subjectService;
	@Autowired
	private RefsService refsService;
	@Autowired
	private SubjectPathologyService subjectPathologyService;
	@Autowired
	private SubjectTherapyService subjectTherapyService;

	public ResponseEntity<AnimalSubject> createAnimalSubject(
			@ApiParam(value = "AnimalSubject object to add", required = true) @RequestBody @Valid final AnimalSubject animalSubject,
			final BindingResult result) throws RestServiceException {

		/* Validation */
		// A basic user can only update certain fields, check that
		final FieldErrorMap accessErrors = this.getCreationRightsErrors(animalSubject);
		// Check hibernate validation
		final FieldErrorMap hibernateErrors = new FieldErrorMap(result);
		// Check unique constraint
		final FieldErrorMap uniqueErrors = this.getUniqueConstraintErrors(animalSubject);
		// Check if given reference values exist
		final FieldErrorMap refValuesExistsErrors = this.checkRefsValueExists(animalSubject);

		/* Merge errors. */
		final FieldErrorMap errors = new FieldErrorMap(accessErrors, hibernateErrors, uniqueErrors,
				refValuesExistsErrors);
		if (!errors.isEmpty()) {
			LOG.error("ERROR while creating AnimalSubject - error in fields :" + errors.size());
			throw new RestServiceException(
					new ErrorModel(HttpStatus.UNPROCESSABLE_ENTITY.value(), "Bad arguments", new ErrorDetails(errors)));
		}
		// Guarantees it is a creation, not an update
		animalSubject.setId(null);

		try {
			final AnimalSubject createdSubject = subjectService.save(animalSubject);
			return new ResponseEntity<AnimalSubject>(createdSubject, HttpStatus.OK);
		} catch (ShanoirPreclinicalException e) {
			throw new RestServiceException(e,
					new ErrorModel(HttpStatus.UNPROCESSABLE_ENTITY.value(), "Bad arguments", null));
		}
	}

	public ResponseEntity<Void> deleteAnimalSubject(
			@ApiParam(value = "AnimalSubject id to delete", required = true) @PathVariable("id") Long id) {
		if (subjectService.findById(id) == null) {
			LOG.error("ERROR animalSubject not found while deleting " + id);
			return new ResponseEntity<Void>(HttpStatus.NOT_FOUND);
		}
		try {
			AnimalSubject animalSubject = subjectService.findById(id);
			if (animalSubject == null) {
				LOG.error("ERROR animalSubject not found while deleting " + id);
				return new ResponseEntity<Void>(HttpStatus.NOT_FOUND);
			} else {
				subjectPathologyService.deleteByAnimalSubject(animalSubject);
				subjectTherapyService.deleteByAnimalSubject(animalSubject);
			}
			subjectService.deleteById(id);
		} catch (ShanoirPreclinicalException e) {
			LOG.error("ERROR while deleting animal subject " + id, e);
			return new ResponseEntity<Void>(HttpStatus.NOT_ACCEPTABLE);
		}
		return new ResponseEntity<Void>(HttpStatus.OK);
	}

	public ResponseEntity<AnimalSubject> getAnimalSubjectById(
			@ApiParam(value = "ID of animalSubject that needs to be fetched", required = true) @PathVariable("id") Long id) {
		final AnimalSubject subject = subjectService.findById(id);
		if (subject == null) {
			return new ResponseEntity<AnimalSubject>(HttpStatus.NOT_FOUND);
		}
		return new ResponseEntity<AnimalSubject>(subject, HttpStatus.OK);
	}

	public ResponseEntity<List<AnimalSubject>> getAnimalSubjects() {
		LOG.info("PRECLINICAL getAnimalSubjects");
		final List<AnimalSubject> subjects = subjectService.findAll();
		if (subjects.isEmpty()) {
			return new ResponseEntity<List<AnimalSubject>>(HttpStatus.NO_CONTENT);
		}
		return new ResponseEntity<List<AnimalSubject>>(subjects, HttpStatus.OK);
	}

	public ResponseEntity<Void> updateAnimalSubject(
			@ApiParam(value = "ID of animalSubject that needs to be updated", required = true) @PathVariable("id") Long id,
			@ApiParam(value = "Subject object that needs to be updated", required = true) @RequestBody AnimalSubject animalSubject,
			final BindingResult result) throws RestServiceException {

		// IMPORTANT : avoid any confusion that could lead to security breach
		animalSubject.setId(id);

		// A basic template can only update certain fields, check that
		final FieldErrorMap accessErrors = this.getUpdateRightsErrors(animalSubject);
		// Check hibernate validation
		final FieldErrorMap hibernateErrors = new FieldErrorMap(result);
		// Check unique constrainte
		final FieldErrorMap uniqueErrors = this.getUniqueConstraintErrors(animalSubject);
		/* Merge errors. */
		final FieldErrorMap errors = new FieldErrorMap(accessErrors, hibernateErrors, uniqueErrors);
		if (!errors.isEmpty()) {
			throw new RestServiceException(
					new ErrorModel(HttpStatus.UNPROCESSABLE_ENTITY.value(), "Bad arguments", new ErrorDetails(errors)));
		}

		/* Update template in db. */
		try {
			subjectService.update(animalSubject);
		} catch (ShanoirPreclinicalException e) {
			LOG.error("Error while trying to update subject " + id + " : ", e);
			throw new RestServiceException(e,
					new ErrorModel(HttpStatus.UNPROCESSABLE_ENTITY.value(), "Bad arguments", null));
		}

		return new ResponseEntity<Void>(HttpStatus.OK);
	}

	public ResponseEntity<AnimalSubject> getAnimalSubjectBySubjectId(
			@ApiParam(value = "ID of subject that needs to be fetched", required = true) @PathVariable("id") Long id) {
		final List<AnimalSubject> subjects = subjectService.findBy("subjectId", id);
		if (subjects == null || subjects.isEmpty()) {
			return new ResponseEntity<AnimalSubject>(HttpStatus.NOT_FOUND);
		}
		return new ResponseEntity<AnimalSubject>(subjects.get(0), HttpStatus.OK);
	}

	private FieldErrorMap getUpdateRightsErrors(final AnimalSubject subject) {
		final AnimalSubject previousStateSubject = subjectService.findById(subject.getId());
		final FieldErrorMap accessErrors = new EditableOnlyByValidator<AnimalSubject>().validate(previousStateSubject,
				subject);
		return accessErrors;
	}

	private FieldErrorMap getCreationRightsErrors(final AnimalSubject subject) {
		return new EditableOnlyByValidator<AnimalSubject>().validate(subject);
	}

	private FieldErrorMap getUniqueConstraintErrors(final AnimalSubject subject) {
		final UniqueValidator<AnimalSubject> uniqueValidator = new UniqueValidator<AnimalSubject>(subjectService);
		final FieldErrorMap uniqueErrors = uniqueValidator.validateWithoutId(subject);
		return uniqueErrors;
	}

	private FieldErrorMap checkRefsValueExists(final AnimalSubject subject) {
		FieldErrorMap refsValuesErrors = new RefValueExistsValidator<AnimalSubject>(refsService).validate(subject);
		return refsValuesErrors;
	}
}
