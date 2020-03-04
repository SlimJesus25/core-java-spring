package eu.arrowhead.core.qos.database.service;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.icmp4j.IcmpPingResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import eu.arrowhead.common.CommonConstants;
import eu.arrowhead.common.CoreCommonConstants;
import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.database.entity.Cloud;
import eu.arrowhead.common.database.entity.QoSInterDirectMeasurement;
import eu.arrowhead.common.database.entity.QoSInterDirectPingMeasurement;
import eu.arrowhead.common.database.entity.QoSInterDirectPingMeasurementLog;
import eu.arrowhead.common.database.entity.QoSInterDirectPingMeasurementLogDetails;
import eu.arrowhead.common.database.entity.QoSInterRelayEchoMeasurement;
import eu.arrowhead.common.database.entity.QoSInterRelayEchoMeasurementLog;
import eu.arrowhead.common.database.entity.QoSInterRelayMeasurement;
import eu.arrowhead.common.database.entity.QoSIntraMeasurement;
import eu.arrowhead.common.database.entity.QoSIntraPingMeasurement;
import eu.arrowhead.common.database.entity.QoSIntraPingMeasurementLog;
import eu.arrowhead.common.database.entity.QoSIntraPingMeasurementLogDetails;
import eu.arrowhead.common.database.entity.Relay;
import eu.arrowhead.common.database.entity.System;
import eu.arrowhead.common.database.repository.QoSInterDirectMeasurementPingRepository;
import eu.arrowhead.common.database.repository.QoSInterDirectMeasurementRepository;
import eu.arrowhead.common.database.repository.QoSInterDirectPingMeasurementLogDetailsRepository;
import eu.arrowhead.common.database.repository.QoSInterDirectPingMeasurementLogRepository;
import eu.arrowhead.common.database.repository.QoSInterRelayEchoMeasurementLogRepository;
import eu.arrowhead.common.database.repository.QoSInterRelayEchoMeasurementRepository;
import eu.arrowhead.common.database.repository.QoSInterRelayMeasurementRepository;
import eu.arrowhead.common.database.repository.QoSIntraMeasurementPingRepository;
import eu.arrowhead.common.database.repository.QoSIntraMeasurementRepository;
import eu.arrowhead.common.database.repository.QoSIntraPingMeasurementLogDetailsRepository;
import eu.arrowhead.common.database.repository.QoSIntraPingMeasurementLogRepository;
import eu.arrowhead.common.database.repository.SystemRepository;
import eu.arrowhead.common.dto.internal.CloudResponseDTO;
import eu.arrowhead.common.dto.internal.DTOConverter;
import eu.arrowhead.common.dto.internal.PingMeasurementListResponseDTO;
import eu.arrowhead.common.dto.internal.PingMeasurementResponseDTO;
import eu.arrowhead.common.dto.internal.QoSInterRelayEchoMeasurementListResponseDTO;
import eu.arrowhead.common.dto.internal.RelayResponseDTO;
import eu.arrowhead.common.dto.internal.RelayType;
import eu.arrowhead.common.dto.shared.QoSMeasurementStatus;
import eu.arrowhead.common.dto.shared.QoSMeasurementType;
import eu.arrowhead.common.dto.shared.SystemResponseDTO;
import eu.arrowhead.common.exception.ArrowheadException;
import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.core.qos.dto.PingMeasurementCalculationsDTO;
import eu.arrowhead.core.qos.dto.RelayEchoMeasurementCalculationsDTO;
import eu.arrowhead.core.qos.dto.RelayEchoMeasurementDetailsDTO;

@Service
public class QoSDBService {

	//=================================================================================================
	// members

	private static final String LESS_THAN_ONE_ERROR_MESSAGE= " must be greater than zero.";
	private static final String NOT_AVAILABLE_SORTABLE_FIELD_ERROR_MESSAGE = " sortable field is not available.";
	private static final String NOT_IN_DB_ERROR_MESSAGE = " is not available in database";
	private static final String EMPTY_OR_NULL_ERROR_MESSAGE = " is empty or null";
	private static final String NULL_ERROR_MESSAGE = " is null";
	private static final String NOT_APPLICABLE_RELAY_TYPE = " is not an applicable relay type";

	@Autowired
	private QoSIntraMeasurementRepository qoSIntraMeasurementRepository;
	
	@Autowired
	private QoSIntraMeasurementPingRepository qoSIntraMeasurementPingRepository;
	
	@Autowired
	private QoSIntraPingMeasurementLogRepository qoSIntraPingMeasurementLogRepository;
	
	@Autowired
	private QoSIntraPingMeasurementLogDetailsRepository qoSIntraPingMeasurementLogDetailsRepository;
	
	@Autowired
	private QoSInterDirectMeasurementRepository qoSInterDirectMeasurementRepository;
	
	@Autowired
	private QoSInterDirectMeasurementPingRepository qoSInterDirectMeasurementPingRepository;

	@Autowired
	private QoSInterDirectPingMeasurementLogRepository qoSInterDirectPingMeasurementLogRepository;
	
	@Autowired
	private QoSInterDirectPingMeasurementLogDetailsRepository qoSInterDirectPingMeasurementLogDetailsRepository;

	@Autowired
	private QoSInterRelayMeasurementRepository qosInterRelayMeasurementRepository;

	@Autowired
	private QoSInterRelayEchoMeasurementRepository qosInterRelayEchoMeasurementRepository;

	@Autowired
	private QoSInterRelayEchoMeasurementLogRepository qosInterRelayEchoMeasurementLogRepository;

	@Autowired
	private SystemRepository systemRepository;
	
	protected Logger logger = LogManager.getLogger(QoSDBService.class);
	
	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------	
	@Transactional (rollbackFor = ArrowheadException.class)
	public void updateIntraCountStartedAt() {
		logger.debug("updateIntraCountStartedAt started...");

		try {
			final List<QoSIntraPingMeasurement> measurementList = qoSIntraMeasurementPingRepository.findAll();
			for (final QoSIntraPingMeasurement qoSIntraPingMeasurement : measurementList) {
				qoSIntraPingMeasurement.setSent(0);
				qoSIntraPingMeasurement.setReceived(0);
				qoSIntraPingMeasurement.setCountStartedAt(ZonedDateTime.now());
			}
			qoSIntraMeasurementPingRepository.saveAll(measurementList);
			qoSIntraMeasurementPingRepository.flush();
		} catch (final Exception ex) {
			logger.debug(ex.getMessage(), ex);
			throw new ArrowheadException(CoreCommonConstants.DATABASE_OPERATION_EXCEPTION_MSG);
		}
	}
	
	//-------------------------------------------------------------------------------------------------
	@Transactional (rollbackFor = ArrowheadException.class)
	public QoSIntraMeasurement createIntraMeasurement(final System system, final QoSMeasurementType ping, final ZonedDateTime aroundNow) {
		logger.debug("createIntraMeasurement started...");

		final QoSIntraMeasurement measurement = new QoSIntraMeasurement(system, QoSMeasurementType.PING, aroundNow);

		try {
			qoSIntraMeasurementRepository.saveAndFlush(measurement);
		} catch (final Exception ex) {
			logger.debug(ex.getMessage(), ex);
			throw new ArrowheadException(CoreCommonConstants.DATABASE_OPERATION_EXCEPTION_MSG);
		}

		return measurement;
	}
	
	//-------------------------------------------------------------------------------------------------
	@Transactional (rollbackFor = ArrowheadException.class)
	public QoSIntraMeasurement getOrCreateIntraMeasurement(final SystemResponseDTO systemResponseDTO) {
		logger.debug("getOrCreateIntraMeasurement started...");

		validateSystemResponseDTO(systemResponseDTO);

		final System system;
		final Optional<System> systemOptional = systemRepository.findBySystemNameAndAddressAndPort(
				systemResponseDTO.getSystemName(),
				systemResponseDTO.getAddress(),
				systemResponseDTO.getPort());
		if (systemOptional.isPresent()) {
			system = systemOptional.get();
		} else {
			throw new InvalidParameterException("Requested system" + NOT_IN_DB_ERROR_MESSAGE);
		}

		final QoSIntraMeasurement measurement;
		final Optional<QoSIntraMeasurement> qoSIntraMeasurementOptional = qoSIntraMeasurementRepository.findBySystemAndMeasurementType(system, QoSMeasurementType.PING);
		if (qoSIntraMeasurementOptional.isEmpty()) {
			final ZonedDateTime aroundNow = ZonedDateTime.now();
			measurement = createIntraMeasurement(system, QoSMeasurementType.PING, aroundNow);
		}else {
			 measurement = qoSIntraMeasurementOptional.get();
		}

		return measurement;
	}
	
	//-------------------------------------------------------------------------------------------------
	@Transactional (rollbackFor = ArrowheadException.class)
	public void createIntraPingMeasurement(final QoSIntraMeasurement measurementParam, final PingMeasurementCalculationsDTO calculations, final ZonedDateTime aroundNow) {
		logger.debug("createIntraPingMeasurement started...");

		if (measurementParam == null) {
			throw new InvalidParameterException("QoSIntraMeasurement" + NULL_ERROR_MESSAGE);
		}
		if (calculations == null) {
			throw new InvalidParameterException("PingMeasurementCalculationsDTO" + NULL_ERROR_MESSAGE);
		}
		if (aroundNow == null) {
			throw new InvalidParameterException("ZonedDateTime" + NULL_ERROR_MESSAGE);
		}

		final QoSIntraPingMeasurement pingMeasurement = new QoSIntraPingMeasurement();

		pingMeasurement.setMeasurement(measurementParam);
		pingMeasurement.setAvailable(calculations.isAvailable());
		pingMeasurement.setMaxResponseTime(calculations.getMaxResponseTime());
		pingMeasurement.setMinResponseTime(calculations.getMinResponseTime());
		pingMeasurement.setMeanResponseTimeWithoutTimeout(calculations.getMeanResponseTimeWithoutTimeout());
		pingMeasurement.setMeanResponseTimeWithTimeout(calculations.getMeanResponseTimeWithTimeout());
		pingMeasurement.setJitterWithoutTimeout(calculations.getJitterWithoutTimeout());
		pingMeasurement.setJitterWithTimeout(calculations.getJitterWithTimeout());
		pingMeasurement.setLostPerMeasurementPercent(calculations.getLostPerMeasurementPercent());
		pingMeasurement.setCountStartedAt(aroundNow);
		pingMeasurement.setLastAccessAt(calculations.isAvailable() ? aroundNow : null);
		pingMeasurement.setSent(calculations.getSentInThisPing());
		pingMeasurement.setSentAll(calculations.getSentInThisPing());
		pingMeasurement.setReceived(calculations.getReceivedInThisPing());
		pingMeasurement.setReceivedAll(calculations.getReceivedInThisPing());

		try {

			qoSIntraMeasurementPingRepository.saveAndFlush(pingMeasurement);

		} catch (final Exception ex) {
			logger.debug(ex.getMessage(), ex);
			throw new ArrowheadException(CoreCommonConstants.DATABASE_OPERATION_EXCEPTION_MSG);
		}
	}
	
	//-------------------------------------------------------------------------------------------------
	public Optional<QoSIntraPingMeasurement> getIntraPingMeasurementByMeasurement(final QoSIntraMeasurement measurement) {
		logger.debug("getIntraPingMeasurementByMeasurement started...");

		if (measurement == null) {
			throw new InvalidParameterException("QoSIntraMeasurement" + NULL_ERROR_MESSAGE);
		}

		try {
			return qoSIntraMeasurementPingRepository.findByMeasurement(measurement);
		} catch (final Exception ex) {
			logger.debug(ex.getMessage(), ex);
			throw new ArrowheadException(CoreCommonConstants.DATABASE_OPERATION_EXCEPTION_MSG);
		}
	}
	
	//-------------------------------------------------------------------------------------------------
	@Transactional (rollbackFor = ArrowheadException.class)
	public QoSIntraPingMeasurementLog logIntraMeasurementToDB(final String address, final PingMeasurementCalculationsDTO calculations, final ZonedDateTime aroundNow) {
		logger.debug("logIntraMeasurementToDB started...");

		if (Utilities.isEmpty(address)) {
			throw new InvalidParameterException("Address" + EMPTY_OR_NULL_ERROR_MESSAGE);
		} else if (calculations == null) {
			throw new InvalidParameterException("PingMeasurementCalculationsDTO" + NULL_ERROR_MESSAGE);
		} else if (aroundNow == null) {
			throw new InvalidParameterException("ZonedDateTime" + NULL_ERROR_MESSAGE);
		}

		final QoSIntraPingMeasurementLog measurementLog = new QoSIntraPingMeasurementLog();
		measurementLog.setMeasuredSystemAddress(address);
		measurementLog.setAvailable(calculations.isAvailable());
		measurementLog.setMinResponseTime(calculations.getMinResponseTime());
		measurementLog.setMaxResponseTime(calculations.getMaxResponseTime());
		measurementLog.setMeanResponseTimeWithoutTimeout(calculations.getMeanResponseTimeWithoutTimeout());
		measurementLog.setMeanResponseTimeWithTimeout(calculations.getMeanResponseTimeWithTimeout());
		measurementLog.setJitterWithoutTimeout(calculations.getJitterWithoutTimeout());
		measurementLog.setJitterWithTimeout(calculations.getJitterWithTimeout());
		measurementLog.setLostPerMeasurementPercent(calculations.getLostPerMeasurementPercent());
		measurementLog.setSent(calculations.getSentInThisPing());
		measurementLog.setReceived(calculations.getReceivedInThisPing());
		measurementLog.setMeasuredAt(aroundNow);

		try {
			return qoSIntraPingMeasurementLogRepository.saveAndFlush(measurementLog);
		} catch (final Exception ex) {
			logger.debug(ex.getMessage(), ex);
			throw new ArrowheadException(CoreCommonConstants.DATABASE_OPERATION_EXCEPTION_MSG);
		}

	}
	
	//-------------------------------------------------------------------------------------------------
	@Transactional (rollbackFor = ArrowheadException.class)
	public void logIntraMeasurementDetailsToDB(final QoSIntraPingMeasurementLog measurementLogSaved,
			final List<IcmpPingResponse> responseList, final ZonedDateTime aroundNow) {
		logger.debug("logIntraMeasurementDetailsToDB started...");

		if (responseList == null || responseList.isEmpty()) {
			throw new InvalidParameterException("List<IcmpPingResponse>" + EMPTY_OR_NULL_ERROR_MESSAGE);
		} else if (measurementLogSaved == null) {
			throw new InvalidParameterException("QoSIntraPingMeasurementLog" + NULL_ERROR_MESSAGE);
		} else if (aroundNow == null) {
			throw new InvalidParameterException("ZonedDateTime" + NULL_ERROR_MESSAGE);
		}

		final List<QoSIntraPingMeasurementLogDetails> measurementLogDetailsList = new ArrayList<>(responseList.size());

		int measurementSequenece = 0;
		for (final IcmpPingResponse icmpPingResponse : responseList) {

			final QoSIntraPingMeasurementLogDetails measurementLogDetails = new QoSIntraPingMeasurementLogDetails();
			measurementLogDetails.setMeasurementLog(measurementLogSaved);
			measurementLogDetails.setMeasurementSequeneceNumber( measurementSequenece++ );
			measurementLogDetails.setSuccessFlag(icmpPingResponse.getSuccessFlag());
			measurementLogDetails.setTimeoutFlag(icmpPingResponse.getTimeoutFlag());
			measurementLogDetails.setErrorMessage(icmpPingResponse.getErrorMessage());
			measurementLogDetails.setThrowable(icmpPingResponse.getThrowable() == null ? null : icmpPingResponse.getThrowable().toString());
			measurementLogDetails.setSize(icmpPingResponse.getSize());
			measurementLogDetails.setTtl(icmpPingResponse.getTtl());
			measurementLogDetails.setRtt(icmpPingResponse.getRtt());
			measurementLogDetails.setDuration((int) icmpPingResponse.getDuration());
			measurementLogDetails.setMeasuredAt(aroundNow);

			measurementLogDetailsList.add(measurementLogDetails);
		}

		try {
			qoSIntraPingMeasurementLogDetailsRepository.saveAll(measurementLogDetailsList);
			qoSIntraPingMeasurementLogDetailsRepository.flush();
		} catch (final Exception ex) {
			logger.debug(ex.getMessage(), ex);
			throw new ArrowheadException(CoreCommonConstants.DATABASE_OPERATION_EXCEPTION_MSG);
		}
	}
	
	//-------------------------------------------------------------------------------------------------
	@Transactional (rollbackFor = ArrowheadException.class)
	public void updateIntraPingMeasurement(final QoSIntraMeasurement measurement, final PingMeasurementCalculationsDTO calculations, final QoSIntraPingMeasurement pingMeasurement,final ZonedDateTime aroundNow) {
		logger.debug("updateIntraPingMeasurement started...");

		if (measurement == null) {
			throw new InvalidParameterException("QoSIntraMeasurement" + NULL_ERROR_MESSAGE);
		} else if (calculations == null) {
			throw new InvalidParameterException("PingMeasurementCalculationsDTO" + NULL_ERROR_MESSAGE);
		} else if (pingMeasurement == null) {
			throw new InvalidParameterException("QoSIntraPingMeasurement" + NULL_ERROR_MESSAGE);
		} else if (aroundNow == null) {
			throw new InvalidParameterException("ZonedDateTime" + NULL_ERROR_MESSAGE);
		}

		pingMeasurement.setMeasurement(measurement);
		pingMeasurement.setAvailable(calculations.isAvailable());
		pingMeasurement.setMaxResponseTime(calculations.getMaxResponseTime());
		pingMeasurement.setMinResponseTime(calculations.getMinResponseTime());
		pingMeasurement.setMeanResponseTimeWithoutTimeout(calculations.getMeanResponseTimeWithoutTimeout());
		pingMeasurement.setMeanResponseTimeWithTimeout(calculations.getMeanResponseTimeWithTimeout());
		pingMeasurement.setJitterWithoutTimeout(calculations.getJitterWithoutTimeout());
		pingMeasurement.setJitterWithTimeout(calculations.getJitterWithTimeout());
		pingMeasurement.setLostPerMeasurementPercent(calculations.getLostPerMeasurementPercent());
		pingMeasurement.setCountStartedAt(pingMeasurement.getCountStartedAt());
		pingMeasurement.setLastAccessAt(calculations.isAvailable() ? aroundNow : pingMeasurement.getLastAccessAt());
		pingMeasurement.setSent(pingMeasurement.getSent() + calculations.getSentInThisPing());
		pingMeasurement.setSentAll(pingMeasurement.getSentAll() + calculations.getSentInThisPing());
		pingMeasurement.setReceived(pingMeasurement.getReceived() + calculations.getReceivedInThisPing());
		pingMeasurement.setReceivedAll(pingMeasurement.getReceivedAll() + calculations.getReceivedInThisPing());

		try {

			qoSIntraMeasurementPingRepository.saveAndFlush(pingMeasurement);

		} catch (final Exception ex) {
			logger.debug(ex.getMessage(), ex);
			throw new ArrowheadException(CoreCommonConstants.DATABASE_OPERATION_EXCEPTION_MSG);
		}
	}
	
	//-------------------------------------------------------------------------------------------------
	@Transactional (rollbackFor = ArrowheadException.class)
	public void updateIntraMeasurement(final ZonedDateTime aroundNow, final QoSIntraMeasurement measurement) {
		logger.debug("updateIntraMeasurement started...");

		if (measurement == null) {
			throw new InvalidParameterException("QoSIntraMeasurement" + NULL_ERROR_MESSAGE);
		} else if (aroundNow == null) {
			throw new InvalidParameterException("ZonedDateTime" + NULL_ERROR_MESSAGE);
		}

		measurement.setLastMeasurementAt(aroundNow);
		try {
			qoSIntraMeasurementRepository.saveAndFlush(measurement);
		} catch (final Exception ex) {
			logger.debug(ex.getMessage(), ex);
			throw new ArrowheadException(CoreCommonConstants.DATABASE_OPERATION_EXCEPTION_MSG);
		}
	}
	
	//-------------------------------------------------------------------------------------------------
	public PingMeasurementListResponseDTO getIntraPingMeasurementResponse(final int page, final int size, final Direction direction, final String sortField) {
		logger.debug("getIntraPingMeasurementResponse started...");

		return DTOConverter.convertQoSIntraPingMeasurementPageToPingMeasurementListResponseDTO(getIntraPingMeasurementPage(page, size, direction, sortField));
	}

	//-------------------------------------------------------------------------------------------------
	public Page<QoSIntraPingMeasurement> getIntraPingMeasurementPage(final int page, final int size, final Direction direction, final String sortField) {
		logger.debug("getIntraPingMeasurementPage started...");

		final int validatedPage = page < 0 ? 0 : page;
		final int validatedSize = size < 1 ? Integer.MAX_VALUE : size;
		final Direction validatedDirection = direction == null ? Direction.ASC : direction;
		final String validatedSortField = Utilities.isEmpty(sortField) ? CommonConstants.COMMON_FIELD_NAME_ID : sortField.trim();

		if (!QoSIntraPingMeasurement.SORTABLE_FIELDS_BY.contains(validatedSortField)) {
			throw new InvalidParameterException(validatedSortField + NOT_AVAILABLE_SORTABLE_FIELD_ERROR_MESSAGE);
		}

		try {
			return qoSIntraMeasurementPingRepository.findAll(PageRequest.of(validatedPage, validatedSize, validatedDirection, validatedSortField));
		} catch (final Exception ex) {
			logger.debug(ex.getMessage(), ex);
			throw new ArrowheadException(CoreCommonConstants.DATABASE_OPERATION_EXCEPTION_MSG);
		}
	}
	
	//-------------------------------------------------------------------------------------------------
	public PingMeasurementResponseDTO getIntraPingMeasurementBySystemIdResponse(final long id) {
		logger.debug("getIntraPingMeasurementBySystemIdResponse started ...");

		final QoSIntraPingMeasurement pingMeausrement = getIntraPingMeasurementBySystemId(id);
		if (pingMeausrement == null ) {

			final PingMeasurementResponseDTO response = new PingMeasurementResponseDTO();
			response.setId( null );

			return response;
		}

		return DTOConverter.convertQoSIntraPingMeasurementToPingMeasurementResponseDTO(pingMeausrement);
	}

	//-------------------------------------------------------------------------------------------------
	public QoSIntraPingMeasurement getIntraPingMeasurementBySystemId(final long id) {
		logger.debug("getIntraPingMeasurementBySystemId started ...");

		if (id < 1) {
			throw new InvalidParameterException("SystemId" + LESS_THAN_ONE_ERROR_MESSAGE);
		}

		final System system;
		final Optional<System> systemOptional;
		try {

			systemOptional = systemRepository.findById(id);
			if (systemOptional.isPresent()) {
				system = systemOptional.get();
			} else {
				throw new InvalidParameterException("Requested system" + NOT_IN_DB_ERROR_MESSAGE);
			}

			final QoSIntraMeasurement measurement;
			final Optional<QoSIntraMeasurement> qoSIntraMeasurementOptional = qoSIntraMeasurementRepository.findBySystemAndMeasurementType(system, QoSMeasurementType.PING);
			if (qoSIntraMeasurementOptional.isEmpty()) {
	
				return null;
			} else {
				 measurement = qoSIntraMeasurementOptional.get();
			}

			final Optional<QoSIntraPingMeasurement> measurementOptional = qoSIntraMeasurementPingRepository.findByMeasurement(measurement);
			if (measurementOptional.isEmpty()) {

				return null;
			} else {

				return measurementOptional.get();
			}
		} catch (final InvalidParameterException ex) {
			throw ex;
		} catch (final Exception ex) {
			logger.debug(ex.getMessage(), ex);
			throw new ArrowheadException(CoreCommonConstants.DATABASE_OPERATION_EXCEPTION_MSG);
		}
	}
	
	//-------------------------------------------------------------------------------------------------
	@Transactional (rollbackFor = ArrowheadException.class)
	public QoSInterDirectMeasurement createInterDirectMeasurement(final Cloud cloud, final String address, final QoSMeasurementType ping, final ZonedDateTime aroundNow) {
		logger.debug("createInterMeasurement started...");

		final QoSInterDirectMeasurement measurement = new QoSInterDirectMeasurement(cloud, address, QoSMeasurementType.PING, aroundNow);

		try {
			qoSInterDirectMeasurementRepository.saveAndFlush(measurement);
		} catch (final Exception ex) {
			logger.debug(ex.getMessage(), ex);
			throw new ArrowheadException(CoreCommonConstants.DATABASE_OPERATION_EXCEPTION_MSG);
		}

		return measurement;
	}
	
	//-------------------------------------------------------------------------------------------------
	public List<QoSInterDirectMeasurement> getInterDirectMeasurementByCloud(final CloudResponseDTO cloudResponseDTO) {
		logger.debug("getInterDirectMeasurementByCloud started ...");

		final Cloud cloud = DTOConverter.convertCloudResponseDTOToCloud(cloudResponseDTO);
		try {

			final List<QoSInterDirectMeasurement> measurementList = qoSInterDirectMeasurementRepository.findByCloudAndMeasurementType(cloud, QoSMeasurementType.PING);

			return measurementList;

		} catch (final Exception ex) {

			logger.debug(ex.getMessage(), ex);
			throw new ArrowheadException(CoreCommonConstants.DATABASE_OPERATION_EXCEPTION_MSG);
		}
	}

	//-------------------------------------------------------------------------------------------------
	public Optional<QoSInterDirectPingMeasurement> getInterDirectPingMeasurementByMeasurement(final QoSInterDirectMeasurement measurement) {
		logger.debug("getInterDirectPingMeasurementByMeasurement started ...");

		if (measurement == null) {
			throw new InvalidParameterException("QoSIntraMeasurement" + NULL_ERROR_MESSAGE);
		}

		try {
			return qoSInterDirectMeasurementPingRepository.findByMeasurement(measurement);
		} catch (final Exception ex) {
			logger.debug(ex.getMessage(), ex);
			throw new ArrowheadException(CoreCommonConstants.DATABASE_OPERATION_EXCEPTION_MSG);
		}
	}

	//-------------------------------------------------------------------------------------------------
	public void createInterDirectPingMeasurement(final QoSInterDirectMeasurement measurementParam, final PingMeasurementCalculationsDTO calculations, final ZonedDateTime aroundNow) {
		logger.debug("createInterDirectPingMeasurement started ...");

		if (measurementParam == null) {
			throw new InvalidParameterException("QoSInterMeasurement" + NULL_ERROR_MESSAGE);
		}
		if (calculations == null) {
			throw new InvalidParameterException("PingMeasurementCalculationsDTO" + NULL_ERROR_MESSAGE);
		}
		if (aroundNow == null) {
			throw new InvalidParameterException("ZonedDateTime" + NULL_ERROR_MESSAGE);
		}

		final QoSInterDirectPingMeasurement pingMeasurement = new QoSInterDirectPingMeasurement();

		pingMeasurement.setMeasurement(measurementParam);
		pingMeasurement.setAvailable(calculations.isAvailable());
		pingMeasurement.setMaxResponseTime(calculations.getMaxResponseTime());
		pingMeasurement.setMinResponseTime(calculations.getMinResponseTime());
		pingMeasurement.setMeanResponseTimeWithoutTimeout(calculations.getMeanResponseTimeWithoutTimeout());
		pingMeasurement.setMeanResponseTimeWithTimeout(calculations.getMeanResponseTimeWithTimeout());
		pingMeasurement.setJitterWithoutTimeout(calculations.getJitterWithoutTimeout());
		pingMeasurement.setJitterWithTimeout(calculations.getJitterWithTimeout());
		pingMeasurement.setLostPerMeasurementPercent(calculations.getLostPerMeasurementPercent());
		pingMeasurement.setCountStartedAt(aroundNow);
		pingMeasurement.setLastAccessAt(calculations.isAvailable() ? aroundNow : null);
		pingMeasurement.setSent(calculations.getSentInThisPing());
		pingMeasurement.setSentAll(calculations.getSentInThisPing());
		pingMeasurement.setReceived(calculations.getReceivedInThisPing());
		pingMeasurement.setReceivedAll(calculations.getReceivedInThisPing());

		try {

			qoSInterDirectMeasurementPingRepository.saveAndFlush(pingMeasurement);

		} catch (final Exception ex) {
			logger.debug(ex.getMessage(), ex);
			throw new ArrowheadException(CoreCommonConstants.DATABASE_OPERATION_EXCEPTION_MSG);
		}

	}

	//-------------------------------------------------------------------------------------------------
	public QoSInterDirectPingMeasurementLog logInterDirectMeasurementToDB(final String address, final PingMeasurementCalculationsDTO calculations, final ZonedDateTime aroundNow) {
		logger.debug("logInterDirectMeasurementToDB started ...");

		if (Utilities.isEmpty(address)) {
			throw new InvalidParameterException("Address" + EMPTY_OR_NULL_ERROR_MESSAGE);
		} else if (calculations == null) {
			throw new InvalidParameterException("PingMeasurementCalculationsDTO" + NULL_ERROR_MESSAGE);
		} else if (aroundNow == null) {
			throw new InvalidParameterException("ZonedDateTime" + NULL_ERROR_MESSAGE);
		}

		final QoSInterDirectPingMeasurementLog measurementLog = new QoSInterDirectPingMeasurementLog();
		measurementLog.setMeasuredSystemAddress(address);
		measurementLog.setAvailable(calculations.isAvailable());
		measurementLog.setMinResponseTime(calculations.getMinResponseTime());
		measurementLog.setMaxResponseTime(calculations.getMaxResponseTime());
		measurementLog.setMeanResponseTimeWithoutTimeout(calculations.getMeanResponseTimeWithoutTimeout());
		measurementLog.setMeanResponseTimeWithTimeout(calculations.getMeanResponseTimeWithTimeout());
		measurementLog.setJitterWithoutTimeout(calculations.getJitterWithoutTimeout());
		measurementLog.setJitterWithTimeout(calculations.getJitterWithTimeout());
		measurementLog.setLostPerMeasurementPercent(calculations.getLostPerMeasurementPercent());
		measurementLog.setSent(calculations.getSentInThisPing());
		measurementLog.setReceived(calculations.getReceivedInThisPing());
		measurementLog.setMeasuredAt(aroundNow);

		try {
			return qoSInterDirectPingMeasurementLogRepository.saveAndFlush(measurementLog);
		} catch (final Exception ex) {
			logger.debug(ex.getMessage(), ex);
			throw new ArrowheadException(CoreCommonConstants.DATABASE_OPERATION_EXCEPTION_MSG);
		}

	}

	//-------------------------------------------------------------------------------------------------
	public void logInterDirectMeasurementDetailsToDB(final QoSInterDirectPingMeasurementLog measurementLogSaved, final List<IcmpPingResponse> responseList, final ZonedDateTime aroundNow) {
		logger.debug("logInterDirectMeasurementDetailsToDB started ...");

		if (responseList == null || responseList.isEmpty()) {
			throw new InvalidParameterException("List<IcmpPingResponse>" + EMPTY_OR_NULL_ERROR_MESSAGE);
		} else if (measurementLogSaved == null) {
			throw new InvalidParameterException("QoSInterPingMeasurementLog" + NULL_ERROR_MESSAGE);
		} else if (aroundNow == null) {
			throw new InvalidParameterException("ZonedDateTime" + NULL_ERROR_MESSAGE);
		}

		final List<QoSInterDirectPingMeasurementLogDetails> interMeasurementLogDetailsList = new ArrayList<>(responseList.size());

		int measurementSequenece = 0;
		for (final IcmpPingResponse icmpPingResponse : responseList) {

			final QoSInterDirectPingMeasurementLogDetails measurementLogDetails = new QoSInterDirectPingMeasurementLogDetails();
			measurementLogDetails.setMeasurementLog(measurementLogSaved);
			measurementLogDetails.setMeasurementSequeneceNumber( measurementSequenece++ );
			measurementLogDetails.setSuccessFlag(icmpPingResponse.getSuccessFlag());
			measurementLogDetails.setTimeoutFlag(icmpPingResponse.getTimeoutFlag());
			measurementLogDetails.setErrorMessage(icmpPingResponse.getErrorMessage());
			measurementLogDetails.setThrowable(icmpPingResponse.getThrowable() == null ? null : icmpPingResponse.getThrowable().toString());
			measurementLogDetails.setSize(icmpPingResponse.getSize());
			measurementLogDetails.setTtl(icmpPingResponse.getTtl());
			measurementLogDetails.setRtt(icmpPingResponse.getRtt());
			measurementLogDetails.setDuration((int) icmpPingResponse.getDuration());
			measurementLogDetails.setMeasuredAt(aroundNow);

			interMeasurementLogDetailsList.add(measurementLogDetails);
		}

		try {
			qoSInterDirectPingMeasurementLogDetailsRepository.saveAll(interMeasurementLogDetailsList);
			qoSInterDirectPingMeasurementLogDetailsRepository.flush();
		} catch (final Exception ex) {
			logger.debug(ex.getMessage(), ex);
			throw new ArrowheadException(CoreCommonConstants.DATABASE_OPERATION_EXCEPTION_MSG);
		}
	}

	//-------------------------------------------------------------------------------------------------
	public void updateInterDirectPingMeasurement(final QoSInterDirectMeasurement measurement, final PingMeasurementCalculationsDTO calculations, final QoSInterDirectPingMeasurement pingMeasurement,
												 final ZonedDateTime aroundNow) {
		logger.debug("updateInterDirectPingMeasurement started ...");


		if (measurement == null) {
			throw new InvalidParameterException("QoSIntraMeasurement" + NULL_ERROR_MESSAGE);
		} else if (calculations == null) {
			throw new InvalidParameterException("PingMeasurementCalculationsDTO" + NULL_ERROR_MESSAGE);
		} else if (pingMeasurement == null) {
			throw new InvalidParameterException("QoSInterPingMeasurement" + NULL_ERROR_MESSAGE);
		} else if (aroundNow == null) {
			throw new InvalidParameterException("ZonedDateTime" + NULL_ERROR_MESSAGE);
		}

		pingMeasurement.setMeasurement(measurement);
		pingMeasurement.setAvailable(calculations.isAvailable());
		pingMeasurement.setMaxResponseTime(calculations.getMaxResponseTime());
		pingMeasurement.setMinResponseTime(calculations.getMinResponseTime());
		pingMeasurement.setMeanResponseTimeWithoutTimeout(calculations.getMeanResponseTimeWithoutTimeout());
		pingMeasurement.setMeanResponseTimeWithTimeout(calculations.getMeanResponseTimeWithTimeout());
		pingMeasurement.setJitterWithoutTimeout(calculations.getJitterWithoutTimeout());
		pingMeasurement.setJitterWithTimeout(calculations.getJitterWithTimeout());
		pingMeasurement.setLostPerMeasurementPercent(calculations.getLostPerMeasurementPercent());
		pingMeasurement.setCountStartedAt(pingMeasurement.getCountStartedAt());
		pingMeasurement.setLastAccessAt(calculations.isAvailable() ? aroundNow : pingMeasurement.getLastAccessAt());
		pingMeasurement.setSent(pingMeasurement.getSent() + calculations.getSentInThisPing());
		pingMeasurement.setSentAll(pingMeasurement.getSentAll() + calculations.getSentInThisPing());
		pingMeasurement.setReceived(pingMeasurement.getReceived() + calculations.getReceivedInThisPing());
		pingMeasurement.setReceivedAll(pingMeasurement.getReceivedAll() + calculations.getReceivedInThisPing());

		try {

			qoSInterDirectMeasurementPingRepository.saveAndFlush(pingMeasurement);

		} catch (final Exception ex) {
			logger.debug(ex.getMessage(), ex);
			throw new ArrowheadException(CoreCommonConstants.DATABASE_OPERATION_EXCEPTION_MSG);
		}
	}

	//-------------------------------------------------------------------------------------------------
	public QoSInterDirectMeasurement getOrCreateDirectInterMeasurement(final String address, final CloudResponseDTO cloudResponseDTO) {
		logger.debug("getOrCreateDirectInterMeasurement started ...");

		if (Utilities.isEmpty(address)) {
			throw new InvalidParameterException("Address" + EMPTY_OR_NULL_ERROR_MESSAGE);
		}

		if (cloudResponseDTO == null) {
			throw new InvalidParameterException("CloudResponseDTO" + NULL_ERROR_MESSAGE);
		}

		final Cloud cloud = DTOConverter.convertCloudResponseDTOToCloud(cloudResponseDTO);
		final QoSInterDirectMeasurement measurement;
		final Optional<QoSInterDirectMeasurement> qoSInterDirectMeasurementOptional = qoSInterDirectMeasurementRepository.findByCloudAndAddressAndMeasurementType(cloud, address, QoSMeasurementType.PING);
		if (qoSInterDirectMeasurementOptional.isEmpty()) {
			final ZonedDateTime aroundNow = ZonedDateTime.now();
			measurement = createInterDirectMeasurement(cloud, address, QoSMeasurementType.PING, aroundNow);
		}else {
			 measurement = qoSInterDirectMeasurementOptional.get();
		}

		return measurement;
	}

	//-------------------------------------------------------------------------------------------------
	public void updateInterDirectMeasurement(final ZonedDateTime aroundNow, final QoSInterDirectMeasurement measurement) {
		logger.debug("updateInterDirectMeasurement started ...");

		if (measurement == null) {
			throw new InvalidParameterException("QoSInterMeasurement" + NULL_ERROR_MESSAGE);
		} else if (aroundNow == null) {
			throw new InvalidParameterException("ZonedDateTime" + NULL_ERROR_MESSAGE);
		}

		measurement.setLastMeasurementAt(aroundNow);
		try {
			qoSInterDirectMeasurementRepository.saveAndFlush(measurement);
		} catch (final Exception ex) {
			logger.debug(ex.getMessage(), ex);
			throw new ArrowheadException(CoreCommonConstants.DATABASE_OPERATION_EXCEPTION_MSG);
		}
	}
	
	//-------------------------------------------------------------------------------------------------	
	@Transactional (rollbackFor = ArrowheadException.class)
	public void updateInterRelayCountStartedAt() {
		logger.debug("updateInterRelayCountStartedAt started...");

		try {
			final List<QoSInterRelayEchoMeasurement> measurementList = qosInterRelayEchoMeasurementRepository.findAll();
			for (final QoSInterRelayEchoMeasurement qoSInterRelayEchoMeasurement : measurementList) {
				qoSInterRelayEchoMeasurement.setSent(0);
				qoSInterRelayEchoMeasurement.setReceived(0);
				qoSInterRelayEchoMeasurement.setCountStartedAt(ZonedDateTime.now());
			}
			qosInterRelayEchoMeasurementRepository.saveAll(measurementList);
			qosInterRelayEchoMeasurementRepository.flush();
		} catch (final Exception ex) {
			logger.debug(ex.getMessage(), ex);
			throw new ArrowheadException(CoreCommonConstants.DATABASE_OPERATION_EXCEPTION_MSG);
		}
	}
	
	//-------------------------------------------------------------------------------------------------
	@Transactional (rollbackFor = ArrowheadException.class)
	public QoSInterRelayMeasurement createInterRelayMeasurement(final Cloud cloud, final Relay relay, final QoSMeasurementType type, final ZonedDateTime aroundNow) {
		logger.debug("createInterRelayMeasurement started...");

		final QoSInterRelayMeasurement measurement = new QoSInterRelayMeasurement(cloud, relay, type, aroundNow);

		try {
			qosInterRelayMeasurementRepository.saveAndFlush(measurement);
		} catch (final Exception ex) {
			logger.debug(ex.getMessage(), ex);
			throw new ArrowheadException(CoreCommonConstants.DATABASE_OPERATION_EXCEPTION_MSG);
		}

		return measurement;
	}
	
	//-------------------------------------------------------------------------------------------------
	@Transactional (rollbackFor = ArrowheadException.class)
	public QoSInterRelayMeasurement getOrCreateInterRelayMeasurement(final CloudResponseDTO cloudResponseDTO, final RelayResponseDTO relayResponseDTO, final QoSMeasurementType type) {
		logger.debug("getOrCreateInterRelayMeasurement started...");

		validateCloudResponseDTO(cloudResponseDTO);
		validateRelayResponseDTO(relayResponseDTO);
		
		final Cloud cloud = DTOConverter.convertCloudResponseDTOToCloud(cloudResponseDTO);
		final Relay relay = DTOConverter.convertRelayResponseDTOToRelay(relayResponseDTO);
		
		final QoSInterRelayMeasurement measurement;
		final Optional<QoSInterRelayMeasurement> qoSInterRelayMeasurementOptional = qosInterRelayMeasurementRepository.findByCloudAndRelayAndMeasurementType(cloud, relay, type);
		if (qoSInterRelayMeasurementOptional.isEmpty()) {
			final ZonedDateTime aroundNow = ZonedDateTime.now();
			measurement = createInterRelayMeasurement(cloud, relay, type, aroundNow);
		}else {
			measurement = qoSInterRelayMeasurementOptional.get();
			measurement.setStatus(QoSMeasurementStatus.PENDING);
			try {
				qosInterRelayMeasurementRepository.saveAndFlush(measurement);
			} catch (final Exception ex) {
				logger.debug(ex.getMessage(), ex);
				throw new ArrowheadException(CoreCommonConstants.DATABASE_OPERATION_EXCEPTION_MSG);
			}
		}

		return measurement;
	}

	//-------------------------------------------------------------------------------------------------
	@Transactional (rollbackFor = ArrowheadException.class)
	public void changeInterRelayMeasurementStatusById(final long id, final QoSMeasurementStatus status) {
		final Optional<QoSInterRelayMeasurement> optional = qosInterRelayMeasurementRepository.findById(id);
		if (optional.isEmpty()) {
			throw new InvalidParameterException("InterRelayMeasurement with id: " + id + NOT_IN_DB_ERROR_MESSAGE);
		} else {
			final QoSInterRelayMeasurement measurement = optional.get();
			measurement.setStatus(status);
			try {
				qosInterRelayMeasurementRepository.saveAndFlush(measurement);
			} catch (final Exception ex) {
				logger.debug(ex.getMessage(), ex);
				throw new ArrowheadException(CoreCommonConstants.DATABASE_OPERATION_EXCEPTION_MSG);
			}
		}		
	}
	
	//-------------------------------------------------------------------------------------------------
	@Transactional (rollbackFor = ArrowheadException.class)
	public void createInterRelayEchoMeasurement(final QoSInterRelayMeasurement measurementParam, final RelayEchoMeasurementCalculationsDTO calculations, final ZonedDateTime aroundNow) {
		logger.debug("createInterRelayEchoMeasurement started...");

		if (measurementParam == null) {
			throw new InvalidParameterException("QoSRelayEchoMeasurement" + NULL_ERROR_MESSAGE);
		}
		if (calculations == null) {
			throw new InvalidParameterException("RelayEchoMeasurementCalculationsDTO" + NULL_ERROR_MESSAGE);
		}
		if (aroundNow == null) {
			throw new InvalidParameterException("ZonedDateTime" + NULL_ERROR_MESSAGE);
		}

		final QoSInterRelayEchoMeasurement measurement = new QoSInterRelayEchoMeasurement();

		measurement.setMeasurement(measurementParam);
		measurement.setMaxResponseTime(calculations.getMaxResponseTime());
		measurement.setMinResponseTime(calculations.getMinResponseTime());
		measurement.setMeanResponseTimeWithoutTimeout(calculations.getMeanResponseTimeWithoutTimeout());
		measurement.setMeanResponseTimeWithTimeout(calculations.getMeanResponseTimeWithTimeout());
		measurement.setJitterWithoutTimeout(calculations.getJitterWithoutTimeout());
		measurement.setJitterWithTimeout(calculations.getJitterWithTimeout());
		measurement.setLostPerMeasurementPercent(calculations.getLostPerMeasurementPercent());
		measurement.setCountStartedAt(aroundNow);
		measurement.setLastAccessAt(aroundNow);
		measurement.setSent(calculations.getSentInThisTest());
		measurement.setSentAll(calculations.getSentInThisTest());
		measurement.setReceived(calculations.getReceivedInThisTest());
		measurement.setReceivedAll(calculations.getReceivedInThisTest());

		try {

			qosInterRelayEchoMeasurementRepository.saveAndFlush(measurement);

		} catch (final Exception ex) {
			logger.debug(ex.getMessage(), ex);
			throw new ArrowheadException(CoreCommonConstants.DATABASE_OPERATION_EXCEPTION_MSG);
		}

	}
	
	//-------------------------------------------------------------------------------------------------
	public Optional<QoSInterRelayEchoMeasurement> getInterRelayEchoMeasurementByMeasurement(final QoSInterRelayMeasurement measurement) {
		logger.debug("getInterRelayEchoMeasurementByMeasurement started...");

		if (measurement == null) {
			throw new InvalidParameterException("QoSInterRelayMeasurement" + NULL_ERROR_MESSAGE);
		}

		try {
			return qosInterRelayEchoMeasurementRepository.findByMeasurement(measurement);
		} catch (final Exception ex) {
			logger.debug(ex.getMessage(), ex);
			throw new ArrowheadException(CoreCommonConstants.DATABASE_OPERATION_EXCEPTION_MSG);
		}
	}
	
	//-------------------------------------------------------------------------------------------------
	@Transactional (rollbackFor = ArrowheadException.class)
	public void logInterRelayEchoMeasurementToDB(final QoSInterRelayMeasurement measurement, final List<RelayEchoMeasurementDetailsDTO> measurementDetails, final ZonedDateTime aroundNow) {
		logger.debug("logInterRelayEchoMeasurementToDB started...");

		if (measurement == null) {
			throw new InvalidParameterException("measurement" + NULL_ERROR_MESSAGE);
		} else if (measurementDetails == null) {
			throw new InvalidParameterException("RelayEchoMeasurementDetailsDTO" + NULL_ERROR_MESSAGE);
		} else if (aroundNow == null) {
			throw new InvalidParameterException("ZonedDateTime" + NULL_ERROR_MESSAGE);
		}

		List<QoSInterRelayEchoMeasurementLog> toSave = new ArrayList<>(measurementDetails.size());
		for (RelayEchoMeasurementDetailsDTO details : measurementDetails) {
			final QoSInterRelayEchoMeasurementLog measurementLog = new QoSInterRelayEchoMeasurementLog();
			measurementLog.setMeasurement(measurement);
			measurementLog.setMeasurementSequeneceNumber(details.getMeasurementSequenceNumber());
			measurementLog.setTimeoutFlag(details.getTimeoutFlag());
			measurementLog.setErrorMessage(details.getErrorMessage());
			measurementLog.setThrowable(details.getThrowable() == null ? null : details.getThrowable().toString());
			measurementLog.setSize(details.getSize());
			measurementLog.setDuration(details.getDuration());
			measurementLog.setMeasuredAt(aroundNow);
			toSave.add(measurementLog);
		}

		try {
			qosInterRelayEchoMeasurementLogRepository.saveAll(toSave);
			qosInterRelayEchoMeasurementLogRepository.flush();
		} catch (final Exception ex) {
			logger.debug(ex.getMessage(), ex);
			throw new ArrowheadException(CoreCommonConstants.DATABASE_OPERATION_EXCEPTION_MSG);
		}
	}
	
	//-------------------------------------------------------------------------------------------------
	@Transactional (rollbackFor = ArrowheadException.class)
	public void updateInterRelayEchoMeasurement(final QoSInterRelayMeasurement measurement, final RelayEchoMeasurementCalculationsDTO calculations,
												final QoSInterRelayEchoMeasurement relayEchoMeasurement,final ZonedDateTime aroundNow) {
		logger.debug("updateInterRelayEchoMeasurement started...");

		if (measurement == null) {
			throw new InvalidParameterException("QoSInterRelayMeasurement" + NULL_ERROR_MESSAGE);
		} else if (calculations == null) {
			throw new InvalidParameterException("RelayEchoMeasurementCalculationsDTO" + NULL_ERROR_MESSAGE);
		} else if (relayEchoMeasurement == null) {
			throw new InvalidParameterException("QoSInterRelayEchoMeasurement" + NULL_ERROR_MESSAGE);
		} else if (aroundNow == null) {
			throw new InvalidParameterException("ZonedDateTime" + NULL_ERROR_MESSAGE);
		}

		relayEchoMeasurement.setMeasurement(measurement);
		relayEchoMeasurement.setMaxResponseTime(calculations.getMaxResponseTime());
		relayEchoMeasurement.setMinResponseTime(calculations.getMinResponseTime());
		relayEchoMeasurement.setMeanResponseTimeWithoutTimeout(calculations.getMeanResponseTimeWithoutTimeout());
		relayEchoMeasurement.setMeanResponseTimeWithTimeout(calculations.getMeanResponseTimeWithTimeout());
		relayEchoMeasurement.setJitterWithoutTimeout(calculations.getJitterWithoutTimeout());
		relayEchoMeasurement.setJitterWithTimeout(calculations.getJitterWithTimeout());
		relayEchoMeasurement.setLostPerMeasurementPercent(calculations.getLostPerMeasurementPercent());
		relayEchoMeasurement.setCountStartedAt(relayEchoMeasurement.getCountStartedAt());
		relayEchoMeasurement.setSent(relayEchoMeasurement.getSent() + calculations.getSentInThisTest());
		relayEchoMeasurement.setSentAll(relayEchoMeasurement.getSentAll() + calculations.getSentInThisTest());
		relayEchoMeasurement.setReceived(relayEchoMeasurement.getReceived() + calculations.getReceivedInThisTest());
		relayEchoMeasurement.setReceivedAll(relayEchoMeasurement.getReceivedAll() + calculations.getReceivedInThisTest());

		try {
			qosInterRelayEchoMeasurementRepository.saveAndFlush(relayEchoMeasurement);
		} catch (final Exception ex) {
			logger.debug(ex.getMessage(), ex);
			throw new ArrowheadException(CoreCommonConstants.DATABASE_OPERATION_EXCEPTION_MSG);
		}
	}
	
	//-------------------------------------------------------------------------------------------------
	@Transactional (rollbackFor = ArrowheadException.class)
	public void updateInterRelayMeasurement(final ZonedDateTime aroundNow, final QoSInterRelayMeasurement measurement) {
		logger.debug("updateInterRelayMeasurement started...");

		if (measurement == null) {
			throw new InvalidParameterException("QoSInterRelayMeasurement" + NULL_ERROR_MESSAGE);
		} else if (aroundNow == null) {
			throw new InvalidParameterException("ZonedDateTime" + NULL_ERROR_MESSAGE);
		}

		measurement.setLastMeasurementAt(aroundNow);
		try {
			qosInterRelayMeasurementRepository.saveAndFlush(measurement);
		} catch (final Exception ex) {
			logger.debug(ex.getMessage(), ex);
			throw new ArrowheadException(CoreCommonConstants.DATABASE_OPERATION_EXCEPTION_MSG);
		}
	}

	//-------------------------------------------------------------------------------------------------
	public QoSInterRelayEchoMeasurementListResponseDTO getInterRelayEchoMeasurementsResponse(final int page, final int size, final Direction direction, final String sortField) {
		logger.debug("getInterRelayEchoMeasurementsPage started...");
		return DTOConverter.convertQoSInterRelayEchoMeasurementPageToQoSInterRelayEchoMeasurementListResponseDTO(getInterRelayEchoMeasurementsPage(page, size, direction, sortField));
	}
	//-------------------------------------------------------------------------------------------------
	public Page<QoSInterRelayEchoMeasurement> getInterRelayEchoMeasurementsPage(final int page, final int size, final Direction direction, final String sortField) {
		logger.debug("getInterRelayEchoMeasurementsPage started...");

		final int validatedPage = page < 0 ? 0 : page;
		final int validatedSize = size < 1 ? Integer.MAX_VALUE : size;
		final Direction validatedDirection = direction == null ? Direction.ASC : direction;
		final String validatedSortField = Utilities.isEmpty(sortField) ? CommonConstants.COMMON_FIELD_NAME_ID : sortField.trim();

		if (!QoSInterRelayEchoMeasurement.SORTABLE_FIELDS_BY.contains(validatedSortField)) {
			throw new InvalidParameterException(validatedSortField + NOT_AVAILABLE_SORTABLE_FIELD_ERROR_MESSAGE);
		}
		
		try {
			return qosInterRelayEchoMeasurementRepository.findAll(PageRequest.of(validatedPage, validatedSize, validatedDirection, validatedSortField));
		} catch (final Exception ex) {
			logger.debug(ex.getMessage(), ex);
			throw new ArrowheadException(CoreCommonConstants.DATABASE_OPERATION_EXCEPTION_MSG);
		}
	}
	
	//-------------------------------------------------------------------------------------------------
	public QoSInterRelayEchoMeasurement getInterRelayEchoMeasurementByCloudAndRealy(final CloudResponseDTO cloudResponseDTO, final RelayResponseDTO relayResponseDTO) {
		logger.debug("getInterRelayEchoMeasurementByCloudAndRealy started ...");
		
		validateCloudResponseDTO(cloudResponseDTO);
		validateRelayResponseDTO(relayResponseDTO);

		final Cloud cloud = DTOConverter.convertCloudResponseDTOToCloud(cloudResponseDTO);
		final Relay relay = DTOConverter.convertRelayResponseDTOToRelay(relayResponseDTO);
		try {

			final QoSInterRelayMeasurement measurement;
			final Optional<QoSInterRelayMeasurement> qoSInterRelayMeasurementOptional = qosInterRelayMeasurementRepository.findByCloudAndRelayAndMeasurementType(cloud, relay, QoSMeasurementType.RELAY_ECHO);
			if (qoSInterRelayMeasurementOptional.isEmpty()) {	
				return null;
			} else {
				 measurement = qoSInterRelayMeasurementOptional.get();
			}

			final Optional<QoSInterRelayEchoMeasurement> measurementOptional = qosInterRelayEchoMeasurementRepository.findByMeasurement(measurement);
			if (measurementOptional.isEmpty()) {
				return null;
			} else {
				return measurementOptional.get();
			}
		} catch (final InvalidParameterException ex) {
			throw ex;
		} catch (final Exception ex) {
			logger.debug(ex.getMessage(), ex);
			throw new ArrowheadException(CoreCommonConstants.DATABASE_OPERATION_EXCEPTION_MSG);
		}
	}
	
	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private void validateSystemResponseDTO(final SystemResponseDTO systemResponseDTO) {
		logger.debug("validateSystemRequestDTO started...");

		if (systemResponseDTO == null) {
			throw new InvalidParameterException("SystemRequestDTO" + NULL_ERROR_MESSAGE);
		}

		if (Utilities.isEmpty(systemResponseDTO.getSystemName())) {
			throw new InvalidParameterException("System name" + EMPTY_OR_NULL_ERROR_MESSAGE);
		}

		if (Utilities.isEmpty(systemResponseDTO.getAddress())) {
			throw new InvalidParameterException("System address" + EMPTY_OR_NULL_ERROR_MESSAGE);
		}
	}
	
	//-------------------------------------------------------------------------------------------------
	private void validateCloudResponseDTO (final CloudResponseDTO dto) {
		logger.debug("validateCloudResponseDTO started...");

		if (dto == null) {
			throw new InvalidParameterException("CloudResponseDTO" + NULL_ERROR_MESSAGE);
		}
		
		if (Utilities.isEmpty(dto.getName())) {
			throw new InvalidParameterException("Cloud name" + EMPTY_OR_NULL_ERROR_MESSAGE);
		}
		
		if (Utilities.isEmpty(dto.getOperator())) {
			throw new InvalidParameterException("Cloud operator" + EMPTY_OR_NULL_ERROR_MESSAGE);
		}
	}
	
	//-------------------------------------------------------------------------------------------------
	private void validateRelayResponseDTO (final RelayResponseDTO dto) {
		logger.debug("validateRelayResponseDTO started...");

		if (dto == null) {
			throw new InvalidParameterException("RelayResponseDTO" + NULL_ERROR_MESSAGE);
		}
		
		if (Utilities.isEmpty(dto.getAddress())) {
			throw new InvalidParameterException("Relay address" + EMPTY_OR_NULL_ERROR_MESSAGE);
		}
		
		if (dto.getType() == RelayType.GATEWAY_RELAY) {
			throw new InvalidParameterException(dto.getType().name() + NOT_APPLICABLE_RELAY_TYPE);
		}
	}
}