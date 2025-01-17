package org.shanoir.ng.subject.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.shanoir.ng.configuration.amqp.RabbitMQSubjectService;
import org.shanoir.ng.shared.core.model.IdName;
import org.shanoir.ng.study.model.Study;
import org.shanoir.ng.study.repository.StudyRepository;
import org.shanoir.ng.subject.dto.SimpleSubjectDTO;
import org.shanoir.ng.subject.model.Subject;
import org.shanoir.ng.subject.repository.SubjectRepository;
import org.shanoir.ng.subjectstudy.model.SubjectStudy;
import org.shanoir.ng.subjectstudy.repository.SubjectStudyRepository;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.mockito.junit.MockitoJUnitRunner;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Test class for RabbitMQSubjectService class.
 * @author fli
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class RabbitMQSubjectServiceTest {

	@Mock
	SubjectRepository subjectRepository;

	@Mock
	SubjectService subjectService;

	@Mock
	StudyRepository studyRepository;

	@Mock
	SubjectStudyRepository subjectStudyRepository;
	
	@Mock
	ObjectMapper mapper;

	@InjectMocks
	private RabbitMQSubjectService rabbitMQSubjectService;

	private Long studyId = 1L;

	private Long subjectId = 1L;

	private Subject subject = new Subject();

	private Study study = new Study();

	private IdName idName = new IdName(studyId, subjectId.toString());

	private String studyName = "studyname";
	
	@Before
	public void init() {
		subject.setId(subjectId);
		subject.setSubjectStudyList(new ArrayList<>());
		study.setId(studyId);
		study.setName(studyName);
	}
	
	@Test
	public void testGetSubjetsForStudy() throws JsonProcessingException {
		SimpleSubjectDTO dto = new SimpleSubjectDTO();
		String ident="subjectIdentifier";
		dto.setIdentifier(ident);
		Mockito.when(mapper.writeValueAsString(Mockito.any())).thenReturn(ident);

		// GIVEN a study ID, retrieve all associated subjects
		String result = rabbitMQSubjectService.getSubjectsForStudy(studyId.toString());
		assertNotNull(result);
		assertTrue(result.contains(ident));
	}

	@Test(expected = AmqpRejectAndDontRequeueException.class)
	public void testGetSubjetsForStudyFail() throws JsonProcessingException {
		// GIVEN a study ID, retrieve all associated subjects
		rabbitMQSubjectService.getSubjectsForStudy("non parsable long");
	}

	@Test
	public void testUpdateSubjectStudyExisting() throws IOException {
		SubjectStudy susu = new SubjectStudy();
		susu.setStudy(study);
		susu.setSubject(subject);

		// GIVEN a studyID and a subjectID
		subject.setSubjectStudyList(Collections.singletonList(susu));
		Mockito.when(subjectRepository.findById(subjectId)).thenReturn(Optional.of(subject));
		Mockito.when(mapper.readValue(Mockito.anyString(), Mockito.eq(IdName.class))).thenReturn(idName);
		// WHEN the subjectStudy already exists
		String message = "{id: 1, name: \"1L\"}";
		String name = rabbitMQSubjectService.updateSubjectStudy(message);
		
		// THEN nothing is created
		Mockito.verifyNoInteractions(subjectStudyRepository);
		assertEquals(name, studyName);
	}

	@Test
	public void testUpdateSubjectStudyCreating() throws IOException {
		SubjectStudy susu = new SubjectStudy();
		susu.setStudy(study);
		susu.setSubject(subject);

		// GIVEN a studyID and a subjectID
		subject.setSubjectStudyList(Collections.emptyList());
		Mockito.when(subjectRepository.findById(subjectId)).thenReturn(Optional.of(subject));
		Mockito.when(mapper.readValue(Mockito.anyString(), Mockito.eq(IdName.class))).thenReturn(idName);
		Mockito.when(studyRepository.findById(studyId)).thenReturn(Optional.of(study));

		// WHEN the subjectStudy does not exists
		String message = "{id: 1, name: \"1L\"}";
		String name = rabbitMQSubjectService.updateSubjectStudy(message);
		
		// THEN a new subejctStudy is created
		Mockito.verify(subjectStudyRepository).save(Mockito.any(SubjectStudy.class));
		assertEquals(name, studyName);
	}

	@Test
	public void testUpdateSubjectStudyFail() throws IOException {

		// WHEN the call fails
		String name = rabbitMQSubjectService.updateSubjectStudy(mapper.writeValueAsString(idName));
		
		// THEN a message is logged and null is sent
		assertNull(name);
	}

	// TODO: complete these
	@Test
	public void testManageParticipants() {
		
	}
}
