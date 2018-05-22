package org.shanoir.ng.utils;

import org.shanoir.ng.preclinical.therapies.Therapy;
import org.shanoir.ng.preclinical.therapies.TherapyType;
import org.shanoir.ng.preclinical.therapies.subject_therapies.SubjectTherapy;

/**
 * Utility class for test. Generates therapy.
 * 
 * @author sloury
 *
 */
public final class TherapyModelUtil {

	// Therapy data
	public static final Long THERAPY_BEAIN_ID = 1L;
	public static final String THERAPY_NAME_BRAIN = "Brainectomy";
	public static final Long THERAPY_BRAIN_ID = 2L;
	public static final String THERAPY_NAME_CHIMIO = "Chimiotherapy";
	public static final Long SUBJECT_THERAPY_ID = 1L;

	/**
	 * Create a therapy.
	 * 
	 * @return therapy.
	 */
	public static Therapy createTherapyBrain() {
		Therapy therapy = new Therapy();
		therapy.setId(THERAPY_BEAIN_ID);
		therapy.setName(THERAPY_NAME_BRAIN);
		therapy.setTherapyType(TherapyType.SURGERY);
		return therapy;
	}

	public static Therapy createTherapyChimio() {
		Therapy therapy = new Therapy();
		therapy.setId(THERAPY_BRAIN_ID);
		therapy.setName(THERAPY_NAME_CHIMIO);
		therapy.setTherapyType(TherapyType.SURGERY);
		return therapy;
	}

	/**
	 * Create a subject therapy
	 * 
	 * @return subject therapy.
	 */
	public static SubjectTherapy createSubjectTherapy() {
		SubjectTherapy stherapy = new SubjectTherapy();
		stherapy.setId(SUBJECT_THERAPY_ID);
		stherapy.setTherapy(createTherapyBrain());
		stherapy.setAnimalSubject(AnimalSubjectModelUtil.createAnimalSubject());
		return stherapy;
	}

}
