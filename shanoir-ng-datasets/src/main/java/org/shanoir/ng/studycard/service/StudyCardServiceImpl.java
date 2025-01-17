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

package org.shanoir.ng.studycard.service;

import java.util.ArrayList;
import java.util.List;

import org.shanoir.ng.shared.exception.EntityNotFoundException;
import org.shanoir.ng.shared.exception.MicroServiceCommunicationException;
import org.shanoir.ng.studycard.model.StudyCard;
import org.shanoir.ng.studycard.model.rule.StudyCardRule;
import org.shanoir.ng.studycard.repository.StudyCardRepository;
import org.shanoir.ng.utils.Utils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Study Card service implementation.
 *
 * @author msimon
 *
 */
@Service
public class StudyCardServiceImpl implements StudyCardService {

	@Autowired
	private StudyCardRepository studyCardRepository;

	@Override
	public void deleteById(final Long id) throws EntityNotFoundException, MicroServiceCommunicationException {
		final StudyCard studyCard = studyCardRepository.findById(id).orElse(null);
		if (studyCard == null) {
			throw new EntityNotFoundException(StudyCard.class, id);
		}
		studyCardRepository.deleteById(id);
	}

	@Override
	public List<StudyCard> findAll() {
		return Utils.toList(studyCardRepository.findAll());
	}

	@Override
	public StudyCard findById(final Long id) {
		return studyCardRepository.findById(id).orElse(null);
	}

	@Override
	public StudyCard save(final StudyCard card) throws MicroServiceCommunicationException {
	    card.setLastEditTimestamp(System.currentTimeMillis());
		StudyCard savedStudyCard = studyCardRepository.save(card);
		return savedStudyCard;
	}

	@Override
	public List<StudyCard> search(final List<Long> studyIdList) {
		return studyCardRepository.findByStudyIdIn(studyIdList);
	}

	@Override
	public StudyCard update(final StudyCard card) throws EntityNotFoundException, MicroServiceCommunicationException {
		final StudyCard studyCardDb = studyCardRepository.findById(card.getId()).orElse(null);
		if (studyCardDb == null) throw new EntityNotFoundException(StudyCard.class, card.getId());
		updateStudyCardValues(studyCardDb, card);
		studyCardDb.setLastEditTimestamp(System.currentTimeMillis());
		studyCardRepository.save(studyCardDb);
		return studyCardDb;
	}


	/**
	 * Update some values of template to save them in database.
	 *
	 * @param templateDb template found in database.
	 * @param template template with new values.
	 * @return database template with new values.
	 */
	private StudyCard updateStudyCardValues(final StudyCard studyCardDb, final StudyCard studyCard) {
		studyCardDb.setName(studyCard.getName());
		studyCardDb.setDisabled(studyCard.isDisabled());
		studyCardDb.setAcquisitionEquipmentId(studyCard.getAcquisitionEquipmentId());
		studyCardDb.setId(studyCard.getId());
		studyCardDb.setNiftiConverterId(studyCard.getNiftiConverterId());
		studyCardDb.setStudyId(studyCard.getStudyId());
		if (studyCardDb.getRules() == null) studyCardDb.setRules(new ArrayList<StudyCardRule<?>>());
		else studyCardDb.getRules().clear();
		if (studyCard.getRules() != null) studyCardDb.getRules().addAll(studyCard.getRules());
		return studyCardDb;
	}

	@Override
	public List<StudyCard> findByStudy(Long studyId) {
		return this.studyCardRepository.findByStudyId(studyId);
	}

	@Override
	public List<StudyCard> findStudyCardsByAcqEq(Long acqEqId) {
		return this.studyCardRepository.findByAcquisitionEquipmentId(acqEqId);
	}

	@Override
	public StudyCard findByName(String name) {
		return studyCardRepository.findByName(name);
	}

}
