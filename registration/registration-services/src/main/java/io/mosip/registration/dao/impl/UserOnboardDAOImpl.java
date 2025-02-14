package io.mosip.registration.dao.impl;

import static io.mosip.registration.constants.LoggerConstants.LOG_REG_USER_ONBOARD;
import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_ID;
import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_NAME;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import javax.transaction.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import io.mosip.commons.packet.constants.Biometric;
import io.mosip.commons.packet.util.PacketManagerHelper;
import io.mosip.kernel.biometrics.entities.BIR;
import io.mosip.kernel.biometrics.entities.BiometricRecord;
import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.util.DateUtils;
import io.mosip.registration.config.AppConfig;
import io.mosip.registration.constants.BiometricAttributes;
import io.mosip.registration.constants.RegistrationConstants;
import io.mosip.registration.context.ApplicationContext;
import io.mosip.registration.context.SessionContext;
import io.mosip.registration.dao.UserOnboardDAO;
import io.mosip.registration.dto.biometric.BiometricDTO;
import io.mosip.registration.dto.biometric.FingerprintDetailsDTO;
import io.mosip.registration.dto.packetmanager.BiometricsDto;
import io.mosip.registration.entity.UserBiometric;
import io.mosip.registration.entity.UserDetail;
import io.mosip.registration.entity.UserMachineMapping;
import io.mosip.registration.entity.id.UserBiometricId;
import io.mosip.registration.entity.id.UserMachineMappingID;
import io.mosip.registration.exception.RegBaseUncheckedException;
import io.mosip.registration.repositories.UserBiometricRepository;
import io.mosip.registration.repositories.UserDetailRepository;
import io.mosip.registration.repositories.UserMachineMappingRepository;

/**
 * The implementation class of {@link UserOnboardDAO}
 * 
 * @author Sreekar Chukka
 *
 * @since 1.0.0
 */
@Repository
@Transactional
public class UserOnboardDAOImpl implements UserOnboardDAO {

	@Autowired
	private UserBiometricRepository userBiometricRepository;

	/**
	 * machineMapping instance creation using autowired annotation
	 */
	@Autowired
	private UserMachineMappingRepository machineMappingRepository;

	@Autowired
	private UserDetailRepository userDetailRepository;

	@Autowired
	private PacketManagerHelper packetManagerHelper;

	/**
	 * logger for logging
	 */
	private static final Logger LOGGER = AppConfig.getLogger(UserOnboardDAOImpl.class);

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * io.mosip.registration.dao.UserOnBoardDao#insert(io.mosip.registration.dto.
	 * biometric.BiometricDTO)
	 */
	@Override
	public String insert(BiometricDTO biometricDTO) {

		LOGGER.info(LOG_REG_USER_ONBOARD, APPLICATION_NAME, APPLICATION_ID, "Entering insert method");

		String response = RegistrationConstants.EMPTY;

		try {

			LOGGER.info(LOG_REG_USER_ONBOARD, APPLICATION_NAME, APPLICATION_ID,
					"Biometric information insertion into table");

			List<UserBiometric> bioMetricsList = new ArrayList<>();

			List<FingerprintDetailsDTO> fingerPrint = biometricDTO.getOperatorBiometricDTO().getFingerprintDetailsDTO()
					.stream().flatMap(o -> o.getSegmentedFingerprints().stream()).collect(Collectors.toList());

			fingerPrint.forEach(fingerPrintData -> {

				UserBiometric bioMetrics = new UserBiometric();
				UserBiometricId biometricId = new UserBiometricId();

				biometricId.setBioAttributeCode(fingerPrintData.getFingerprintImageName());
				biometricId.setBioTypeCode(RegistrationConstants.FIN);
				biometricId.setUsrId(SessionContext.userContext().getUserId());
				bioMetrics
						.setBioIsoImage(fingerPrintData.getFingerPrintISOImage());
				bioMetrics.setNumberOfRetry(fingerPrintData.getNumRetry());
				bioMetrics.setUserBiometricId(biometricId);
				Double qualitySocre = fingerPrintData.getQualityScore();
				bioMetrics.setQualityScore(qualitySocre.intValue());
				bioMetrics.setCrBy(SessionContext.userContext().getUserId());
				bioMetrics.setCrDtime(Timestamp.valueOf(DateUtils.getUTCCurrentDateTime()));
				bioMetrics.setIsActive(true);

				bioMetricsList.add(bioMetrics);

			});

			biometricDTO.getOperatorBiometricDTO().getIrisDetailsDTO().forEach(iries -> {

				UserBiometric bioMetrics = new UserBiometric();
				UserBiometricId biometricId = new UserBiometricId();

				biometricId.setBioAttributeCode(iries.getIrisImageName());
				biometricId.setBioTypeCode(RegistrationConstants.IRS);
				biometricId.setUsrId(SessionContext.userContext().getUserId());
				bioMetrics.setBioIsoImage(iries.getIrisIso());
				bioMetrics.setNumberOfRetry(iries.getNumOfIrisRetry());
				bioMetrics.setUserBiometricId(biometricId);
				Double qualitySocre = iries.getQualityScore();
				bioMetrics.setQualityScore(qualitySocre.intValue());
				bioMetrics.setCrBy(SessionContext.userContext().getUserId());
				bioMetrics.setCrDtime(Timestamp.valueOf(DateUtils.getUTCCurrentDateTime()));
				bioMetrics.setIsActive(true);

				bioMetricsList.add(bioMetrics);

			});

			biometricDTO.getOperatorBiometricDTO().getFace();

			UserBiometric bioMetrics = new UserBiometric();
			UserBiometricId biometricId = new UserBiometricId();

			biometricId.setBioAttributeCode(RegistrationConstants.APPLICANT_PHOTOGRAPH_NAME);
			biometricId.setBioTypeCode(RegistrationConstants.FACE);
			biometricId.setUsrId(SessionContext.userContext().getUserId());
			bioMetrics.setBioIsoImage(biometricDTO.getOperatorBiometricDTO().getFace().getFaceISO());
			bioMetrics.setNumberOfRetry(biometricDTO.getOperatorBiometricDTO().getFace().getNumOfRetries());
			bioMetrics.setUserBiometricId(biometricId);
			Double qualitySocre = biometricDTO.getOperatorBiometricDTO().getFace().getQualityScore();
			bioMetrics.setQualityScore(qualitySocre.intValue());
			bioMetrics.setCrBy(SessionContext.userContext().getUserId());
			bioMetrics.setCrDtime(Timestamp.valueOf(DateUtils.getUTCCurrentDateTime()));
			bioMetrics.setIsActive(true);

			bioMetricsList.add(bioMetrics);

			userBiometricRepository.deleteByUserBiometricIdUsrId(SessionContext.userContext().getUserId());

			userBiometricRepository.saveAll(bioMetricsList);

			LOGGER.info(LOG_REG_USER_ONBOARD, APPLICATION_NAME, APPLICATION_ID,
					"Biometric information insertion successful");

			response = RegistrationConstants.SUCCESS;

			LOGGER.info(LOG_REG_USER_ONBOARD, APPLICATION_NAME, APPLICATION_ID, "Leaving insert method");

		} catch (RuntimeException runtimeException) {

			LOGGER.error(LOG_REG_USER_ONBOARD, APPLICATION_NAME, APPLICATION_ID,
					runtimeException.getMessage() + ExceptionUtils.getStackTrace(runtimeException));
			response = RegistrationConstants.USER_ON_BOARDING_ERROR_RESPONSE;
			throw new RegBaseUncheckedException(RegistrationConstants.USER_ON_BOARDING_EXCEPTION + response,
					runtimeException.getMessage());
		}

		return response;
	}

	@Override
	public String save() {
		String response = RegistrationConstants.EMPTY;
		try {
			// find user
			LOGGER.info(LOG_REG_USER_ONBOARD, APPLICATION_NAME, APPLICATION_ID,
					"Preparing User and machine information for insertion");

			UserMachineMapping user = new UserMachineMapping();
			UserMachineMappingID userID = new UserMachineMappingID();
			userID.setUsrId(SessionContext.userContext().getUserId());
			userID.setCntrId(ApplicationContext.map().get(RegistrationConstants.USER_CENTER_ID).toString());
			userID.setMachineId(ApplicationContext.map().get(RegistrationConstants.USER_STATION_ID).toString());

			user.setUserMachineMappingId(userID);
			user.setCrBy(SessionContext.userContext().getUserId());
			user.setCrDtime(Timestamp.valueOf(DateUtils.getUTCCurrentDateTime()));
			user.setUpdBy(SessionContext.userContext().getUserId());
			user.setUpdDtimes(Timestamp.valueOf(DateUtils.getUTCCurrentDateTime()));
			user.setIsActive(true);
			user.setLangCode(ApplicationContext.applicationLanguage());

			machineMappingRepository.save(user);

			LOGGER.info(LOG_REG_USER_ONBOARD, APPLICATION_NAME, APPLICATION_ID,
					"User and machine information insertion sucessful");

			response = RegistrationConstants.SUCCESS;
		} catch (RuntimeException runtimeException) {
			LOGGER.error(LOG_REG_USER_ONBOARD, APPLICATION_NAME, APPLICATION_ID,
					runtimeException.getMessage() + ExceptionUtils.getStackTrace(runtimeException));
			throw new RegBaseUncheckedException(RegistrationConstants.MACHINE_MAPPING_RUN_TIME_EXCEPTION,
					runtimeException.getMessage());
		}
		return response;
	}


	@Override
	public Timestamp getLastUpdatedTime(String usrId) {
		try {
			LOGGER.info(LOG_REG_USER_ONBOARD, APPLICATION_NAME, APPLICATION_ID,
					"fetching userMachineMapping details from repository....");
			UserMachineMapping userMachineMapping = machineMappingRepository.findByUserMachineMappingIdUsrIdIgnoreCase(usrId);
			return userMachineMapping != null ? userMachineMapping.getCrDtime() : null;
		} catch (RuntimeException runtimeException) {
			
			throw new RegBaseUncheckedException(RegistrationConstants.USER_ON_BOARDING_EXCEPTION,
					runtimeException.getMessage());
		}
	}

	@Override
	public String insert(List<BiometricsDto> biometrics) {
		LOGGER.info(LOG_REG_USER_ONBOARD, APPLICATION_NAME, APPLICATION_ID, "Entering insert method");
		String response = null;
		LOGGER.info(LOG_REG_USER_ONBOARD, APPLICATION_NAME, APPLICATION_ID,
				"Biometric information insertion into table");
		List<UserBiometric> bioMetricsList = new ArrayList<>();
		
		try {
			biometrics.forEach( dto -> {
				UserBiometric bioMetrics = new UserBiometric();
				UserBiometricId biometricId = new UserBiometricId();
				biometricId.setBioAttributeCode(dto.getBioAttribute());
				biometricId.setBioTypeCode(getBioAttributeCode(dto.getBioAttribute()));
				biometricId.setUsrId(SessionContext.userContext().getUserId());
				bioMetrics.setBioIsoImage(dto.getAttributeISO());
				bioMetrics.setNumberOfRetry(dto.getNumOfRetries());
				bioMetrics.setUserBiometricId(biometricId);
				Double qualitySocre = dto.getQualityScore();
				bioMetrics.setQualityScore(qualitySocre.intValue());
				bioMetrics.setCrBy(SessionContext.userContext().getUserId());
				bioMetrics.setCrDtime(Timestamp.valueOf(DateUtils.getUTCCurrentDateTime()));
				bioMetrics.setIsActive(true);
				bioMetricsList.add(bioMetrics);
			});

			clearUserBiometrics(SessionContext.userContext().getUserId());
			userBiometricRepository.saveAll(bioMetricsList);

			LOGGER.info(LOG_REG_USER_ONBOARD, APPLICATION_NAME, APPLICATION_ID,
					"Biometric information insertion successful");
			return RegistrationConstants.SUCCESS;

		} catch (RuntimeException runtimeException) {
			LOGGER.error(LOG_REG_USER_ONBOARD, APPLICATION_NAME, APPLICATION_ID, ExceptionUtils.getStackTrace(runtimeException));
			response = RegistrationConstants.USER_ON_BOARDING_ERROR_RESPONSE;
		}
		throw new RegBaseUncheckedException(RegistrationConstants.USER_ON_BOARDING_EXCEPTION, response);
	}
	
	/**
	 * Gets the bio attribute.
	 *
	 * @param bioAttribute the bio attribute
	 * @return the bio attribute
	 */
	private String getBioAttributeCode(String bioAttribute) {

		Biometric bioType = Biometric.getBiometricByAttribute(bioAttribute);
		return bioType.getBiometricType().value();

	}
	
	@Override
	public String insertExtractedTemplates(List<BIR> templates) {
		LOGGER.info(LOG_REG_USER_ONBOARD, APPLICATION_NAME, APPLICATION_ID, "Entering insertExtractedTemplates method");
		String response = RegistrationConstants.EMPTY;
		LOGGER.info(LOG_REG_USER_ONBOARD, APPLICATION_NAME, APPLICATION_ID,
				"Biometric information insertion into table");
		List<UserBiometric> bioMetricsList = new ArrayList<>();
		
		try {
			for (BIR template : templates) {
				UserBiometric biometrics = new UserBiometric();
				UserBiometricId biometricId = new UserBiometricId();
				biometricId.setBioAttributeCode(getBioAttribute(template.getBdbInfo().getSubtype()));
				biometricId.setBioTypeCode(getBioAttributeCode(getBioAttribute(template.getBdbInfo().getSubtype())));
				biometricId.setUsrId(SessionContext.userContext().getUserId());
				biometrics.setBioIsoImage(template.getBdb());
				biometrics.setUserBiometricId(biometricId);
				Long qualityScore = template.getBdbInfo().getQuality().getScore();
				biometrics.setQualityScore(qualityScore.intValue());
				biometrics.setCrBy(SessionContext.userContext().getUserId());
				biometrics.setCrDtime(Timestamp.valueOf(DateUtils.getUTCCurrentDateTime()));
				biometrics.setIsActive(true);

				BiometricRecord biometricRecord = new BiometricRecord();
				biometricRecord.setOthers(new HashMap<>());
				biometricRecord.setSegments(Arrays.asList(template));
				biometrics.setBioRawImage(packetManagerHelper.getXMLData(biometricRecord, true));

				bioMetricsList.add(biometrics);
			}

			clearUserBiometrics(SessionContext.userContext().getUserId());
			userBiometricRepository.saveAll(bioMetricsList);
			LOGGER.info(LOG_REG_USER_ONBOARD, APPLICATION_NAME, APPLICATION_ID,
					"Biometric information insertion successful");
	
			response = RegistrationConstants.SUCCESS;
		} catch (Exception exception) {
			LOGGER.error("Saving operator details in DB failed with error - ", exception);
			response = RegistrationConstants.USER_ON_BOARDING_ERROR_RESPONSE;
			throw new RegBaseUncheckedException(RegistrationConstants.USER_ON_BOARDING_EXCEPTION + response,
					exception.getMessage());
		}
		LOGGER.info(LOG_REG_USER_ONBOARD, APPLICATION_NAME, APPLICATION_ID, "Leaving insertExtractedTemplates method");
		return response;
	}

	private String getBioAttribute(List<String> subType) {
		String subTypeName = (subType == null || subType.isEmpty()) ? "Face" : String.join("", subType);
		return BiometricAttributes.getAttributeBySubType(subTypeName);
	}

	private void clearUserBiometrics(String userId) {
		List<UserBiometric> existingBiometrics = userBiometricRepository.findByUserBiometricIdUsrId(userId);
		if(existingBiometrics != null) {
			for(UserBiometric userBiometric : existingBiometrics) {
				userBiometric.setUserDetail(null);
			}
			userBiometricRepository.saveAll(existingBiometrics);
		}
		UserDetail userDetails = userDetailRepository.findByIdIgnoreCaseAndIsActiveTrue(userId);
		if(userDetails != null) {
			userDetails.getUserBiometric().clear();
			userDetailRepository.save(userDetails);
		}
		userBiometricRepository.deleteByUserBiometricIdUsrId(userId);
	}
}
