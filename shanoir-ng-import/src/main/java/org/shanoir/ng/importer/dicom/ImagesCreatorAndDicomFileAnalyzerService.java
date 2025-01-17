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

package org.shanoir.ng.importer.dicom;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Tag;
import org.dcm4che3.data.UID;
import org.dcm4che3.emf.MultiframeExtractor;
import org.dcm4che3.io.DicomInputStream;
import org.shanoir.ng.importer.model.EchoTime;
import org.shanoir.ng.importer.model.EquipmentDicom;
import org.shanoir.ng.importer.model.Image;
import org.shanoir.ng.importer.model.Instance;
import org.shanoir.ng.importer.model.InstitutionDicom;
import org.shanoir.ng.importer.model.Patient;
import org.shanoir.ng.importer.model.Serie;
import org.shanoir.ng.importer.model.Study;
import org.shanoir.ng.shared.dateTime.DateTimeUtils;
import org.shanoir.ng.shared.event.ShanoirEvent;
import org.shanoir.ng.shared.event.ShanoirEventService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * This class reads all instances. A FileInputStream in form of a DicomInputStream is opened
 * to all files to read additional informations, e.g. missing in the DicomDir.
 * This class splits the instances array into two different array nodes: non-images and images,
 * on using the sop class uid. Before the instances are numbered with their instance number
 * and added like this by DicomDirToModelReader. DicomFileAnalyzer removes/deletes
 * the instances node and splits into two nodes: images and nonImages. As this class
 * is reading the content of each dicom file already it adds as well the informations,
 * which are later necessary to separate datasets inside each serie:
 * acquisitionNumber, echoNumbers and imageOrientationsPatient.
 * 
 * In case of the import from pacs, the files are accessed using the STORAGE_PATTERN defined
 * in DicomStoreSCPServer.
 * 
 * @author mkain
 *
 */
@Service
public class ImagesCreatorAndDicomFileAnalyzerService {

	private static final Logger LOG = LoggerFactory.getLogger(ImagesCreatorAndDicomFileAnalyzerService.class);

	private static final String SLASH = "/";

	private static final String SUFFIX_DCM = ".dcm";
	
	private static final String YES = "YES";

	@Autowired
	private DicomSerieAndInstanceAnalyzer dicomSerieAndInstanceAnalyzer;

	@Autowired
	private ShanoirEventService eventService;

	@Value("${shanoir.import.upload.folder}")
	private String uploadFolder;

	public void createImagesAndAnalyzeDicomFiles(List<Patient> patients, String folderFileAbsolutePath, boolean isImportFromPACS, ShanoirEvent event)
			throws FileNotFoundException {
		// patient level
		for (Iterator<Patient> patientsIt = patients.iterator(); patientsIt.hasNext();) {
			Patient patient = patientsIt.next();
			// study level
			List<Study> studies = patient.getStudies();
			for (Iterator<Study> studiesIt = studies.iterator(); studiesIt.hasNext();) {
				Study study = studiesIt.next();
				// serie level
				List<Serie> series = study.getSeries();
				int nbSeries = series.size();
				int cpt = 1;
				for (Iterator<Serie> seriesIt = series.iterator(); seriesIt.hasNext();) {
					Serie serie = seriesIt.next();
					if(event != null){
						event.setMessage("Creating images and analyzing DICOM files for serie [" + (serie.getProtocolName() == null ? serie.getSeriesInstanceUID() : serie.getProtocolName()) + "] " + cpt + "/" + nbSeries + ")");
						eventService.publishEvent(event);
					}
					filterAndCreateImages(folderFileAbsolutePath, serie, isImportFromPACS);
					getAdditionalMetaDataFromFirstInstanceOfSerie(folderFileAbsolutePath, serie, patient, isImportFromPACS);
					cpt++;
				}
			}
		}
	}

	/**
	 * @param folderFileAbsolutePath
	 * @param serie
	 * @throws FileNotFoundException
	 */
	private void getAdditionalMetaDataFromFirstInstanceOfSerie(String folderFileAbsolutePath, Serie serie, Patient patient, boolean isImportFromPACS)
			throws FileNotFoundException {
		List<Instance> instances = serie.getInstances();
		if (!instances.isEmpty()) {
			Instance firstInstance = instances.get(0);
			File firstInstanceFile = getFileFromInstance(firstInstance, serie, folderFileAbsolutePath, isImportFromPACS);
			processDicomFileForFirstInstance(firstInstanceFile, serie, patient);
		}
	}

	/**
	 * This method iterates over all instances, filters only the images
	 * and puts them into a new list: images. For the moment non-images are
	 * not implemented.
	 * 
	 * @param folderFileAbsolutePath
	 * @param serie
	 * @throws FileNotFoundException
	 */
	private void filterAndCreateImages(String folderFileAbsolutePath, Serie serie, boolean isImportFromPACS) throws FileNotFoundException {
		List<Image> images = new ArrayList<Image>();
		List<Object> nonImages = new ArrayList<Object>();
		List<Instance> instances = serie.getInstances();
		for (Iterator<Instance> instancesIt = instances.iterator(); instancesIt.hasNext();) {
			Instance instance = instancesIt.next();
			File instanceFile = getFileFromInstance(instance, serie, folderFileAbsolutePath, isImportFromPACS);
			processDicomFileForAllInstances(instanceFile, images, folderFileAbsolutePath);
		}
		serie.setNonImages(nonImages);
		serie.setNonImagesNumber(nonImages.size());
		serie.setImages(images);
		serie.setImagesNumber(images.size());
	}

	/**
	 * This method accesses to the dicom file of each instance and handles it.
	 * 
	 * @param instance
	 * @param folderFileAbsolutePath
	 * @param nonImages
	 * @param images
	 * @throws FileNotFoundException
	 */
	private File getFileFromInstance(Instance instance, Serie serie, String folderFileAbsolutePath, boolean isImportFromPACS)
			throws FileNotFoundException {
		StringBuilder instanceFilePath = new StringBuilder();
		if (isImportFromPACS) {
			instanceFilePath.append(folderFileAbsolutePath)
				.append(File.separator)
				.append(serie.getSeriesInstanceUID())
				.append(File.separator)
				.append(instance.getSopInstanceUID())
				.append(SUFFIX_DCM);
		} else {
			String[] instancePathArray = instance.getReferencedFileID();
			if (instancePathArray != null) {
				instanceFilePath.append(folderFileAbsolutePath).append(File.separator);
				for (int count = 0; count < instancePathArray.length; count++) {
					instanceFilePath.append(instancePathArray[count]);
					if (count != instancePathArray.length - 1) {
						instanceFilePath.append(File.separator);
					}
				}
			} else {
				throw new FileNotFoundException(
						"instancePathArray in DicomDir: missing file: " + instancePathArray);
			}
		}
		File instanceFile = new File(instanceFilePath.toString());
		if (instanceFile.exists()) {
			return instanceFile;
		} else {
			throw new FileNotFoundException(
					"instanceFilePath in DicomDir: missing file: " + instanceFilePath);
		}
	}
	
	/**
	 * This method opens the connection to each dcm file and reads its attributes
	 * and extracts meta-data from the dicom, that will be used later.
	 * 
	 * @param dicomFile
	 * @param serie
	 * @param instances
	 * @param instance
	 * @param index
	 * @param nonImages
	 * @param images
	 */
	private void processDicomFileForAllInstances(File dicomFile, List<Image> images, String folderFileAbsolutePath) {
		try (DicomInputStream dIS = new DicomInputStream(dicomFile)) {
			Attributes attributes = dIS.readDataset(-1, -1);
			// Some DICOM files with a particular SOPClassUID are ignored: such as Raw Data Storage etc.
			if (dicomSerieAndInstanceAnalyzer.checkInstanceIsIgnored(attributes)) {
				// do nothing here as instances list will be emptied after split between images and non-images
			} else {
				// divide here between non-images and images, non-images at first
				Image image = new Image();
				/**
				 * Attention: the path of each image is always relative: either to the temporary folder created
				 * with dicom zip import during the upload or with the DicomStoreSCPServer folder for PACS import
				 */
				String relativeFilePath = dicomFile.getAbsolutePath().replace(folderFileAbsolutePath + SLASH, "");
				image.setPath(relativeFilePath);
				addImageSeparateDatasetsInfo(image, attributes);
				images.add(image);
			}
		} catch (IOException e) {
			LOG.error("Error during DICOM file process.", e);
		}
	}
	
	/**
	 * This method reads the first dicom file of a serie to complete missing informations.
	 * 
	 * @param dicomFile
	 * @param serie
	 * @param patient
	 */
	private void processDicomFileForFirstInstance(File dicomFile, Serie serie, Patient patient) {
		try (DicomInputStream dIS = new DicomInputStream(dicomFile)) {
			LOG.debug("Process first DICOM file of serie {} path {}", serie.getSeriesInstanceUID() + " " + serie.getSeriesDescription(), dicomFile.getAbsolutePath());
			Attributes attributes = dIS.readDataset(-1, -1);
			checkPatientData(patient, attributes);
			checkSerieData(serie, attributes);
			addSeriesEquipment(serie, attributes);
			addSeriesCenter(serie, attributes);
		} catch (IOException e) {
			LOG.error("Error during processing of DICOM file:", e);
		}
	}

	/**
	 * This method adds all required infos to separate datasets within series for
	 * each image.
	 * 
	 * @param image
	 * @param datasetAttributes
	 */
	private void addImageSeparateDatasetsInfo(Image image, Attributes attributes) {
		if (UID.EnhancedMRImageStorage.equals(attributes.getString(Tag.SOPClassUID))) {
			MultiframeExtractor emf = new MultiframeExtractor();
			attributes = emf.extract(attributes, 0);
		}
		// acquisition number
		image.setAcquisitionNumber(attributes.getInt(Tag.AcquisitionNumber, 0));
		// image orientation patient
		List<Double> imageOrientationPatient = new ArrayList<>();
		double[] imageOrientationPatientArray = attributes.getDoubles(Tag.ImageOrientationPatient);
		if (imageOrientationPatientArray != null) {
			for (int i = 0; i < imageOrientationPatientArray.length; i++) {
				imageOrientationPatient.add(imageOrientationPatientArray[i]);
			}
			image.setImageOrientationPatient(imageOrientationPatient);
		} else {
			LOG.debug("imageOrientationPatientArray in dcm file null: {}", image.getPath());
		}
		// repetition time
		image.setRepetitionTime(attributes.getDouble(Tag.RepetitionTime, 0));
		// inversion time
		image.setInversionTime(attributes.getDouble(Tag.InversionTime, 0));
		// flip angle
		String flipAngle = attributes.getString(Tag.FlipAngle);
		if (flipAngle == null) {
			flipAngle = "0";
		}
		image.setFlipAngle(flipAngle);
		// echo times
		Set<EchoTime> echoTimes = new HashSet<>();
		EchoTime echoTime = new EchoTime();
		echoTime.setEchoNumber(attributes.getInt(Tag.EchoNumbers, 0));
		echoTime.setEchoTime(attributes.getDouble(Tag.EchoTime, 0.0));
		echoTimes.add(echoTime);
		image.setEchoTimes(echoTimes);
	}

	/**
	 * Adds the equipment information. We suppose here that the info coming
	 * from the first file is more reliable than the infos coming from the
	 * dicomdir or the pacs querying.
	 * 
	 * @param serie
	 * @param attributes
	 */
	private void addSeriesEquipment(Serie serie, Attributes attributes) {
		if (serie.getEquipment() == null || !serie.getEquipment().isComplete()) {
			String manufacturer = attributes.getString(Tag.Manufacturer);
			String manufacturerModelName = attributes.getString(Tag.ManufacturerModelName);
			String deviceSerialNumber = attributes.getString(Tag.DeviceSerialNumber);
			serie.setEquipment(new EquipmentDicom(manufacturer, manufacturerModelName, deviceSerialNumber));
		}
	}

	/**
	 * Adds the equipment information.
	 * 
	 * @param serie
	 * @param datasetAttributes
	 */
	private void addSeriesCenter(Serie serie, Attributes attributes) {
		if (serie.getInstitution() == null) {
			InstitutionDicom institution = new InstitutionDicom();
			String institutionName = attributes.getString(Tag.InstitutionName);
			String institutionAddress = attributes.getString(Tag.InstitutionAddress);
			institution.setInstitutionName(institutionName);
			institution.setInstitutionAddress(institutionAddress);
			serie.setInstitution(institution);
		}
	}

	/**
	 * Normally we get the seriesDescription from the DicomDir, if not: null or
	 * empty, get the seriesDescription from the .dcm file, if existing in .dcm file.
	 * 
	 * @param serie
	 * @param attributes
	 */
	private void checkSerieData(Serie serie, Attributes attributes) {
		if (StringUtils.isEmpty(serie.getSopClassUID())) {
			// has not been found in dicomdir or before in other file, so we get it from .dcm file:
			String sopClassUIDDicomFile = attributes.getString(Tag.SOPClassUID);
			if (StringUtils.isNotEmpty(sopClassUIDDicomFile)) {
				serie.setSopClassUID(sopClassUIDDicomFile);
			}
		}
		if (StringUtils.isEmpty(serie.getSeriesDescription())) {
			// has not been found in dicomdir or before in other file, so we get it from .dcm file:
			String seriesDescriptionDicomFile = attributes.getString(Tag.SeriesDescription);
			if (StringUtils.isNotEmpty(seriesDescriptionDicomFile)) {
				serie.setSeriesDescription(seriesDescriptionDicomFile);
			}
		}
		dicomSerieAndInstanceAnalyzer.checkSerieIsEnhanced(serie, attributes);
		dicomSerieAndInstanceAnalyzer.checkSerieIsSpectroscopy(serie, attributes);
		if (serie.getSeriesDate() == null) {
			serie.setSeriesDate(DateTimeUtils.dateToLocalDate(attributes.getDate(Tag.SeriesDate)));
		}
		if (serie.getIsCompressed() == null) {
			String transferSyntaxUID = attributes.getString(Tag.TransferSyntaxUID);
			serie.setIsCompressed(transferSyntaxUID != null && transferSyntaxUID.startsWith("1.2.840.10008.1.2.4"));
		}
		if (StringUtils.isEmpty(serie.getProtocolName())) {
			serie.setProtocolName(attributes.getString(Tag.ProtocolName));
		}
		// keep this check at this place: enhanced Dicom needs to be checked first
		dicomSerieAndInstanceAnalyzer.checkSerieIsMultiFrame(serie, attributes);
	}

	/**
	 * Normally we get the Patient BirthDate from the DicomDir, if not: null or
	 * empty, get the Patient BirthDate from the .dcm file, if existing in .dcm file
	 * add it in JsonNode tree.
	 * 
	 * @param serie
	 * @param attributes
	 */
	private void checkPatientData(Patient patient, Attributes attributes) {
		if (patient.getPatientBirthDate() == null) {
			// has not been found in dicomdir, so we get it from .dcm file:
			patient.setPatientBirthDate(DateTimeUtils.dateToLocalDate(attributes.getDate(Tag.PatientBirthDate)));
		}
		if (StringUtils.isEmpty(patient.getPatientSex())) {
			// has not been found in dicomdir, so we get it from .dcm file:
			patient.setPatientSex(attributes.getString(Tag.PatientSex));
		}
		// we can not display this information for the pacs in select series: as info not available
		String patientIdentityRemoved = attributes.getString(Tag.PatientIdentityRemoved);
		if (StringUtils.isNotBlank(patientIdentityRemoved)) {
			if (YES.equals(patientIdentityRemoved)) {
				patient.setPatientIdentityRemoved(true);
				String deIdentificationMethod = attributes.getString(Tag.DeidentificationMethod);
				patient.setDeIdentificationMethod(deIdentificationMethod);
			}
		}
	}

}
