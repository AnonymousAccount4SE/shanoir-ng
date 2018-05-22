package org.shanoir.ng.preclinical.therapies;

import java.util.List;
import java.util.Optional;

import org.shanoir.ng.shared.exception.ShanoirPreclinicalException;
import org.shanoir.ng.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;


/**
 * Therapies service implementation.
 * 
 * @author sloury
 *
 */
@Service
public class TherapyServiceImpl implements TherapyService {

	/**
	 * Logger
	 */
	private static final Logger LOG = LoggerFactory.getLogger(TherapyServiceImpl.class);

	@Autowired
	private RabbitTemplate rabbitTemplate;

	@Autowired
	private TherapyRepository therapiesRepository;

	@Override
	public void deleteById(final Long id) throws ShanoirPreclinicalException {
		therapiesRepository.delete(id);
	}

	@Override
	public List<Therapy> findAll() {
		return Utils.toList(therapiesRepository.findAll());
	}

	@Override
	public List<Therapy> findBy(final String fieldName, final Object value) {
		return therapiesRepository.findBy(fieldName, value);
	}
	
	@Override
	public Therapy findById(final Long id) {
		return therapiesRepository.findOne(id);
	}
	
	@Override
	public Therapy findByName(final String name) {
		Optional<Therapy> therapy = therapiesRepository.findByName(name);
		if(therapy.isPresent()) return therapy.get();
		return null;
	}
	
	@Override
	public List<Therapy> findByTherapyType(final TherapyType type) {
		return therapiesRepository.findByTherapyType(type);
	}

	@Override
	public Therapy save(final Therapy therapy) throws ShanoirPreclinicalException {
		Therapy savedTherapy = null;
		try {
			savedTherapy = therapiesRepository.save(therapy);
		} catch (DataIntegrityViolationException dive) {
			ShanoirPreclinicalException.logAndThrow(LOG, "Error while creating therapy: " + dive.getMessage());
		}
		return savedTherapy;
	}

	@Override
	public Therapy update(final Therapy therapy) throws ShanoirPreclinicalException {
		final Therapy therapyDb = therapiesRepository.findOne(therapy.getId());
		updateTherapyValues(therapyDb, therapy);
		try {
			therapiesRepository.save(therapyDb);
		} catch (Exception e) {
			ShanoirPreclinicalException.logAndThrow(LOG, "Error while updating therapy: " + e.getMessage());
		}
		return therapyDb;
	}

	

	private Therapy updateTherapyValues(final Therapy therapyDb, final Therapy therapy) {
		therapyDb.setName(therapy.getName());
		therapyDb.setTherapyType(therapy.getTherapyType());
		therapyDb.setComment(therapy.getComment());
		return therapyDb;
	}

}
