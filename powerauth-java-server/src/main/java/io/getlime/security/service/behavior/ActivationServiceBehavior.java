package io.getlime.security.service.behavior;

import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.crypto.SecretKey;
import javax.xml.datatype.DatatypeConfigurationException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.common.collect.ImmutableSet;
import com.google.common.io.BaseEncoding;

import io.getlime.security.powerauth.BlockActivationResponse;
import io.getlime.security.powerauth.CommitActivationResponse;
import io.getlime.security.powerauth.GetActivationListForUserResponse;
import io.getlime.security.powerauth.GetActivationStatusResponse;
import io.getlime.security.powerauth.InitActivationResponse;
import io.getlime.security.powerauth.PrepareActivationResponse;
import io.getlime.security.powerauth.RemoveActivationResponse;
import io.getlime.security.powerauth.UnblockActivationResponse;
import io.getlime.security.powerauth.GetActivationListForUserResponse.Activations;
import io.getlime.security.powerauth.lib.config.PowerAuthConfiguration;
import io.getlime.security.powerauth.lib.generator.KeyGenerator;
import io.getlime.security.powerauth.lib.provider.CryptoProviderUtil;
import io.getlime.security.powerauth.server.activation.PowerAuthServerActivation;
import io.getlime.security.powerauth.server.keyfactory.PowerAuthServerKeyFactory;
import io.getlime.security.repository.ActivationRepository;
import io.getlime.security.repository.ApplicationVersionRepository;
import io.getlime.security.repository.MasterKeyPairRepository;
import io.getlime.security.repository.model.ActivationStatus;
import io.getlime.security.repository.model.entity.ActivationRecordEntity;
import io.getlime.security.repository.model.entity.ApplicationEntity;
import io.getlime.security.repository.model.entity.ApplicationVersionEntity;
import io.getlime.security.repository.model.entity.MasterKeyPairEntity;
import io.getlime.security.service.PowerAuthServiceImpl;
import io.getlime.security.service.exceptions.GenericServiceException;
import io.getlime.security.service.i18n.LocalizationProvider;
import io.getlime.security.service.util.ModelUtil;
import io.getlime.security.service.util.model.ServiceError;

/**
 * Behavior class implementing processes related with activations. Used to move the
 * implementation outside of the main service implementation.
 * 
 * @author Petr Dvorak
 *
 */
@Component
public class ActivationServiceBehavior {

	@Autowired
	private ActivationRepository powerAuthRepository;

	@Autowired
	private MasterKeyPairRepository masterKeyPairRepository;
	
	@Autowired
	private ApplicationVersionRepository applicationVersionRepository;
	
	@Autowired
	private LocalizationProvider localizationProvider;

	private final PowerAuthServerKeyFactory powerAuthServerKeyFactory = new PowerAuthServerKeyFactory();
	private final PowerAuthServerActivation powerAuthServerActivation = new PowerAuthServerActivation();

	/**
	 * Deactivate the activation in CREATED or OTP_USED if it's activation expiration timestamp
	 * is below the given timestamp.
	 * @param timestamp Timestamp to check activations against.
	 * @param activation Activation to check.
	 */
	private void deactivatePendingActivation(Date timestamp, ActivationRecordEntity activation) {
		if ((activation.getActivationStatus().equals(ActivationStatus.CREATED) || activation.getActivationStatus().equals(ActivationStatus.OTP_USED)) && (timestamp.getTime() > activation.getTimestampActivationExpire().getTime())) {
			activation.setActivationStatus(ActivationStatus.REMOVED);
			powerAuthRepository.save(activation);
		}
	}

	/**
	 * Get activations for application ID and user ID
	 * @param applicationId Application ID
	 * @param userId User ID
	 * @return Response with list of matching activations
	 * @throws DatatypeConfigurationException If calendar conversion fails. 
	 */
	public GetActivationListForUserResponse getActivationList(Long applicationId, String userId) throws DatatypeConfigurationException {

		// Generate timestamp in advance
		Date timestamp = new Date();

		List<ActivationRecordEntity> activationsList = null;
		if (applicationId == null) {
			activationsList = powerAuthRepository.findByUserId(userId);
		} else {
			activationsList = powerAuthRepository.findByApplicationIdAndUserId(applicationId, userId);
		}

		GetActivationListForUserResponse response = new GetActivationListForUserResponse();
		response.setUserId(userId);
		if (activationsList != null) {
			for (ActivationRecordEntity activation : activationsList) {

				deactivatePendingActivation(timestamp, activation);

				// Map between repository object and service objects
				Activations activationServiceItem = new Activations();
				activationServiceItem.setActivationId(activation.getActivationId());
				activationServiceItem.setActivationStatus(ModelUtil.toServiceStatus(activation.getActivationStatus()));
				activationServiceItem.setActivationName(activation.getActivationName());
				activationServiceItem.setExtras(activation.getExtras());
				activationServiceItem.setTimestampCreated(ModelUtil.calendarWithDate(activation.getTimestampCreated()));
				activationServiceItem.setTimestampLastUsed(ModelUtil.calendarWithDate(activation.getTimestampLastUsed()));
				activationServiceItem.setUserId(activation.getUserId());
				activationServiceItem.setApplicationId(activation.getApplication().getId());
				activationServiceItem.setApplicationName(activation.getApplication().getName());
				response.getActivations().add(activationServiceItem);
			}
		}
		return response;
	}

	/**
	 * Get activation status for given activation ID
	 * @param activationId Activation ID
	 * @param keyConversionUtilities Key conversion utility class
	 * @return Activation status response
	 * @throws DatatypeConfigurationException If calendar convert fails
	 * @throws InvalidKeySpecException If invalid key is provided
	 * @throws InvalidKeyException If invalid key is provided
	 */
	public GetActivationStatusResponse getActivationStatus(String activationId, CryptoProviderUtil keyConversionUtilities) throws DatatypeConfigurationException, InvalidKeySpecException, InvalidKeyException {

		// Generate timestamp in advance
		Date timestamp = new Date();

		ActivationRecordEntity activation = powerAuthRepository.findFirstByActivationId(activationId);

		// Check if the activation exists
		if (activation != null) {

			// Deactivate old pending activations first
			deactivatePendingActivation(timestamp, activation);

			// Handle CREATED activation
			if (activation.getActivationStatus() == ActivationStatus.CREATED) {

				// Created activations are not able to transfer valid status blob to the client
				// since both keys were not exchanged yet and transport cannot be secured.
				byte[] randomStatusBlob = new KeyGenerator().generateRandomBytes(16);
				
				// Activation signature
				String masterPrivateKeyBase64 = masterKeyPairRepository.findFirstByApplicationIdOrderByTimestampCreatedDesc(activation.getApplication().getId()).getMasterKeyPrivateBase64();
				byte[] masterPrivateKeyBytes = BaseEncoding.base64().decode(masterPrivateKeyBase64);
				byte[] activationSignature = powerAuthServerActivation.generateActivationSignature(
						activation.getActivationIdShort(), 
						activation.getActivationOTP(), 
						keyConversionUtilities.convertBytesToPrivateKey(masterPrivateKeyBytes)
				);

				// return the data
				GetActivationStatusResponse response = new GetActivationStatusResponse();
				response.setActivationId(activationId);
				response.setUserId(activation.getUserId());
				response.setActivationStatus(ModelUtil.toServiceStatus(activation.getActivationStatus()));
				response.setActivationName(activation.getActivationName());
				response.setExtras(activation.getExtras());
				response.setApplicationId(activation.getApplication().getId());
				response.setTimestampCreated(ModelUtil.calendarWithDate(activation.getTimestampCreated()));
				response.setTimestampLastUsed(ModelUtil.calendarWithDate(activation.getTimestampLastUsed()));
				response.setEncryptedStatusBlob(BaseEncoding.base64().encode(randomStatusBlob));
				response.setActivationIdShort(activation.getActivationIdShort());
				response.setActivationOTP(activation.getActivationOTP());
				response.setActivationSignature(BaseEncoding.base64().encode(activationSignature));
				return response;

			} else {

				// Get the server private and device public keys to compute
				// the transport key
				String serverPrivateKeyBase64 = activation.getServerPrivateKeyBase64();
				String devicePublicKeyBase64 = activation.getDevicePublicKeyBase64();

				// If an activation was turned to REMOVED directly from CREATED state,
				// there is not device public key in the database - we need to handle
				// that case by defaulting the C_statusBlob to random value...
				byte[] C_statusBlob = new KeyGenerator().generateRandomBytes(16);

				// There is a device public key available, therefore we can compute
				// the real C_statusBlob value.
				if (devicePublicKeyBase64 != null) {

					PrivateKey serverPrivateKey = keyConversionUtilities.convertBytesToPrivateKey(BaseEncoding.base64().decode(serverPrivateKeyBase64));
					PublicKey devicePublicKey = keyConversionUtilities.convertBytesToPublicKey(BaseEncoding.base64().decode(devicePublicKeyBase64));

					SecretKey masterSecretKey = powerAuthServerKeyFactory.generateServerMasterSecretKey(serverPrivateKey, devicePublicKey);
					SecretKey transportKey = powerAuthServerKeyFactory.generateServerTransportKey(masterSecretKey);

					// Encrypt the status blob
					C_statusBlob = powerAuthServerActivation.encryptedStatusBlob(
							activation.getActivationStatus().getByte(),
							activation.getCounter(), 
							activation.getFailedAttempts().byteValue(), 
							activation.getMaxFailedAttempts().byteValue(), 
							transportKey
					);

				}

				// return the data
				GetActivationStatusResponse response = new GetActivationStatusResponse();
				response.setActivationId(activationId);
				response.setActivationStatus(ModelUtil.toServiceStatus(activation.getActivationStatus()));
				response.setActivationName(activation.getActivationName());
				response.setUserId(activation.getUserId());
				response.setExtras(activation.getExtras());
				response.setApplicationId(activation.getApplication().getId());
				response.setTimestampCreated(ModelUtil.calendarWithDate(activation.getTimestampCreated()));
				response.setTimestampLastUsed(ModelUtil.calendarWithDate(activation.getTimestampLastUsed()));
				response.setEncryptedStatusBlob(BaseEncoding.base64().encode(C_statusBlob));
				response.setActivationIdShort(null);
				response.setActivationOTP(null);
				response.setActivationSignature(null);

				return response;

			}
		} else {

			// Activations that do not exist should return REMOVED state and
			// a random status blob
			byte[] randomStatusBlob = new KeyGenerator().generateRandomBytes(16);

			// return the data
			GetActivationStatusResponse response = new GetActivationStatusResponse();
			response.setActivationId(activationId);
			response.setActivationStatus(ModelUtil.toServiceStatus(ActivationStatus.REMOVED));
			response.setActivationName("unknown");
			response.setUserId("unknown");
			response.setApplicationId(0L);
			response.setExtras(null);
			response.setTimestampCreated(null);
			response.setTimestampLastUsed(null);
			response.setEncryptedStatusBlob(BaseEncoding.base64().encode(randomStatusBlob));
			response.setActivationIdShort(null);
			response.setActivationOTP(null);
			response.setActivationSignature(null);
			return response;
		}
	}

	/**
	 * Init activation with given parameters
	 * @param applicationId Application ID
	 * @param userId User ID
	 * @param maxFailedCount Maximum failed attempt count (5) 
	 * @param activationExpireTimestamp Timestamp after which activation can no longer be completed
	 * @param keyConversionUtilities Utility class for key conversion
	 * @return Response with activation initialization data
	 * @throws GenericServiceException If invalid values are provided.
	 * @throws InvalidKeySpecException If invalid key is provided
	 * @throws InvalidKeyException If invalid key is provided
	 */
	public InitActivationResponse initActivation(Long applicationId, String userId, Long maxFailedCount, Date activationExpireTimestamp, CryptoProviderUtil keyConversionUtilities) throws GenericServiceException, InvalidKeySpecException, InvalidKeyException {
		// Generate timestamp in advance
		Date timestamp = new Date();

		if (userId == null) {
			throw localizationProvider.buildExceptionForCode(ServiceError.ERR0001);
		}

		if (applicationId == 0L) {
			throw localizationProvider.buildExceptionForCode(ServiceError.ERR0002);
		}

		// Get number of max attempts from request or from constants, if not provided
		Long maxAttempt = maxFailedCount;
		if (maxAttempt == null) {
			maxAttempt = PowerAuthConfiguration.SIGNATURE_MAX_FAILED_ATTEMPTS;
		}

		// Get activation expiration date from request or from constants, if not provided
		Date timestampExpiration = activationExpireTimestamp;
		if (timestampExpiration == null) {
			timestampExpiration = new Date(timestamp.getTime() + PowerAuthConfiguration.ACTIVATION_VALIDITY_BEFORE_ACTIVE);
		}

		// Fetch the latest master private key
		MasterKeyPairEntity masterKeyPair = masterKeyPairRepository.findFirstByApplicationIdOrderByTimestampCreatedDesc(applicationId);
		if (masterKeyPair == null) {
			GenericServiceException ex = localizationProvider.buildExceptionForCode(ServiceError.ERR0003);
			Logger.getLogger(PowerAuthServiceImpl.class.getName()).log(Level.SEVERE, null, ex);
			throw ex;
		}
		byte[] masterPrivateKeyBytes = BaseEncoding.base64().decode(masterKeyPair.getMasterKeyPrivateBase64());
		PrivateKey masterPrivateKey = keyConversionUtilities.convertBytesToPrivateKey(masterPrivateKeyBytes);
		if (masterPrivateKey == null) {
			GenericServiceException ex = localizationProvider.buildExceptionForCode(ServiceError.ERR0004);
			Logger.getLogger(PowerAuthServiceImpl.class.getName()).log(Level.SEVERE, null, ex);
			throw ex;
		}

		// Generate new activation data, generate a unique activation ID
		String activationId = null;
		for (int i = 0; i < PowerAuthConfiguration.ACTIVATION_GENERATE_ACTIVATION_ID_ITERATIONS; i++) {
			String tmpActivationId = powerAuthServerActivation.generateActivationId();
			ActivationRecordEntity record = powerAuthRepository.findFirstByActivationId(tmpActivationId);
			if (record == null) {
				activationId = tmpActivationId;
				break;
			} // ... else this activation ID has a collision, reset it and try to find another one
		}
		if (activationId == null) {
			throw localizationProvider.buildExceptionForCode(ServiceError.ERR0005);
		}

		// Generate a unique short activation ID for created and OTP used states
		String activationIdShort = null;
		Set<ActivationStatus> states = ImmutableSet.of(ActivationStatus.CREATED, ActivationStatus.OTP_USED);
		for (int i = 0; i < PowerAuthConfiguration.ACTIVATION_GENERATE_ACTIVATION_SHORT_ID_ITERATIONS; i++) {
			String tmpActivationIdShort = powerAuthServerActivation.generateActivationIdShort();
			ActivationRecordEntity record = powerAuthRepository.findFirstByApplicationIdAndActivationIdShortAndActivationStatusInAndTimestampActivationExpireAfter(applicationId, tmpActivationIdShort, states, timestamp);
			// this activation short ID has a collision, reset it and find
			// another one
			if (record == null) {
				activationIdShort = tmpActivationIdShort;
				break;
			}
		}
		if (activationIdShort == null) {
			throw localizationProvider.buildExceptionForCode(ServiceError.ERR0006);
		}

		// Generate activation OTP
		String activationOtp = powerAuthServerActivation.generateActivationOTP();

		// Compute activation signature
		byte[] activationSignature = powerAuthServerActivation.generateActivationSignature(activationIdShort, activationOtp, masterPrivateKey);
		String activationSignatureBase64 = BaseEncoding.base64().encode(activationSignature);

		// Generate server key pair
		KeyPair serverKeyPair = powerAuthServerActivation.generateServerKeyPair();
		byte[] serverKeyPrivateBytes = keyConversionUtilities.convertPrivateKeyToBytes(serverKeyPair.getPrivate());
		byte[] serverKeyPublicBytes = keyConversionUtilities.convertPublicKeyToBytes(serverKeyPair.getPublic());

		// Store the new activation
		ActivationRecordEntity activation = new ActivationRecordEntity();
		activation.setActivationId(activationId);
		activation.setActivationIdShort(activationIdShort);
		activation.setActivationName(null);
		activation.setActivationOTP(activationOtp);
		activation.setActivationStatus(ActivationStatus.CREATED);
		activation.setCounter(0L);
		activation.setDevicePublicKeyBase64(null);
		activation.setExtras(null);
		activation.setFailedAttempts(0L);
		activation.setApplication(masterKeyPair.getApplication());
		activation.setMasterKeyPair(masterKeyPair);
		activation.setMaxFailedAttempts(maxAttempt);
		activation.setServerPrivateKeyBase64(BaseEncoding.base64().encode(serverKeyPrivateBytes));
		activation.setServerPublicKeyBase64(BaseEncoding.base64().encode(serverKeyPublicBytes));
		activation.setTimestampActivationExpire(timestampExpiration);
		activation.setTimestampCreated(timestamp);
		activation.setTimestampLastUsed(timestamp);
		activation.setUserId(userId);
		powerAuthRepository.save(activation);

		// Return the server response
		InitActivationResponse response = new InitActivationResponse();
		response.setActivationId(activationId);
		response.setActivationIdShort(activationIdShort);
		response.setUserId(userId);
		response.setActivationOTP(activationOtp);
		response.setActivationSignature(activationSignatureBase64);
		response.setApplicationId(activation.getApplication().getId());

		return response;
	}

	/**
	 * Prepare activation with given parameters
	 * @param activationIdShort Short activation ID
	 * @param activationNonceBase64 Activation nonce encoded as Base64
	 * @param clientEphemeralPublicKeyBase64 Client ephemeral public key encoded as Base64
	 * @param cDevicePublicKeyBase64 Encrypted device public key encoded as Base64
	 * @param activationName Activation name
	 * @param extras Extra parameter
	 * @param applicationKey Application key
	 * @param applicationSignature Application signature
	 * @param keyConversionUtilities Utility class for key conversion
	 * @return Prepared activation information 
	 * @throws GenericServiceException In case invalid data is provided
	 * @throws InvalidKeySpecException If invalid key was provided
	 * @throws InvalidKeyException If invalid key was provided
	 * @throws UnsupportedEncodingException If UTF-8 is not supported on the system
	 */
	public PrepareActivationResponse prepareActivation(String activationIdShort, String activationNonceBase64, String clientEphemeralPublicKeyBase64, String cDevicePublicKeyBase64, String activationName, String extras, String applicationKey, String applicationSignature, CryptoProviderUtil keyConversionUtilities) throws GenericServiceException, InvalidKeySpecException, InvalidKeyException, UnsupportedEncodingException {

		// Get current timestamp
		Date timestamp = new Date();
		
		ApplicationVersionEntity applicationVersion = applicationVersionRepository.findByApplicationKey(applicationKey);
		// if there is no such application, exit
		if (applicationVersion == null || applicationVersion.getSupported() == false) {
			throw localizationProvider.buildExceptionForCode(ServiceError.ERR0007);
		}
		
		ApplicationEntity application = applicationVersion.getApplication();
		// if there is no such application, exit
		if (application == null) {
			throw localizationProvider.buildExceptionForCode(ServiceError.ERR0007);
		}

		// Fetch the current activation by short activation ID
		Set<ActivationStatus> states = ImmutableSet.of(ActivationStatus.CREATED);
		ActivationRecordEntity activation = powerAuthRepository.findFirstByApplicationIdAndActivationIdShortAndActivationStatusInAndTimestampActivationExpireAfter(application.getId(), activationIdShort, states, timestamp);

		// if there is no such activation or application does not match the activation application, exit 
		if (activation == null || (activation.getApplication().getId() != application.getId())) {
			throw localizationProvider.buildExceptionForCode(ServiceError.ERR0007);
		}
		
		// Get master private key
		String masterPrivateKeyBase64 = activation.getMasterKeyPair().getMasterKeyPrivateBase64();
		byte[] masterPrivateKeyBytes = BaseEncoding.base64().decode(masterPrivateKeyBase64);
		PrivateKey masterPrivateKey = keyConversionUtilities.convertBytesToPrivateKey(masterPrivateKeyBytes);
		
		// Get client ephemeral public key
		PublicKey clientEphemeralPublicKey = null;
		if (clientEphemeralPublicKeyBase64 != null) { // additional encryption is used
			byte[] clientEphemeralPublicKeyBytes = BaseEncoding.base64().decode(clientEphemeralPublicKeyBase64);
			clientEphemeralPublicKey = keyConversionUtilities.convertBytesToPublicKey(clientEphemeralPublicKeyBytes);
		}

		// Decrypt the device public key
		byte[] C_devicePublicKey = BaseEncoding.base64().decode(cDevicePublicKeyBase64);
		byte[] activationNonce = BaseEncoding.base64().decode(activationNonceBase64);
		PublicKey devicePublicKey = powerAuthServerActivation.decryptDevicePublicKey(
				C_devicePublicKey,
				activationIdShort,
				masterPrivateKey,
				clientEphemeralPublicKey,
				activation.getActivationOTP(), 
				activationNonce
		);
		
		if (devicePublicKey == null) { // invalid key was sent, return error
			activation.setActivationStatus(ActivationStatus.REMOVED);
			powerAuthRepository.save(activation);
			throw localizationProvider.buildExceptionForCode(ServiceError.ERR0009);
		}
		
		byte[] applicationSignatureBytes = BaseEncoding.base64().decode(applicationSignature);
		
		if (!powerAuthServerActivation.validateApplicationSignature(
				activationIdShort, 
				activationNonce, 
				C_devicePublicKey, 
				BaseEncoding.base64().decode(applicationKey), 
				BaseEncoding.base64().decode(applicationVersion.getApplicationSecret()), 
				applicationSignatureBytes)) {
			throw localizationProvider.buildExceptionForCode(ServiceError.ERR0007);
		}

		// Update and persist the activation record
		activation.setActivationStatus(ActivationStatus.OTP_USED);
		activation.setDevicePublicKeyBase64(BaseEncoding.base64().encode(keyConversionUtilities.convertPublicKeyToBytes(devicePublicKey)));
		activation.setActivationName(activationName);
		activation.setExtras(extras);
		powerAuthRepository.save(activation);

		// Generate response data
		byte[] activationNonceServer = powerAuthServerActivation.generateActivationNonce();
		String serverPublicKeyBase64 = activation.getServerPublicKeyBase64();
		PublicKey serverPublicKey = keyConversionUtilities.convertBytesToPublicKey(BaseEncoding.base64().decode(serverPublicKeyBase64));
		KeyPair ephemeralKeyPair = new KeyGenerator().generateKeyPair();
		PrivateKey ephemeralPrivateKey = ephemeralKeyPair.getPrivate();
		PublicKey ephemeralPublicKey = ephemeralKeyPair.getPublic();
		byte[] ephemeralPublicKeyBytes = keyConversionUtilities.convertPublicKeyToBytes(ephemeralPublicKey);
		String activationOtp = activation.getActivationOTP();

		// Encrypt the public key
		byte[] C_serverPublicKey = powerAuthServerActivation.encryptServerPublicKey(serverPublicKey, devicePublicKey, ephemeralPrivateKey, activationOtp, activationIdShort, activationNonceServer);

		// Get encrypted public key signature
		byte[] C_serverPubKeySignature = powerAuthServerActivation.computeServerDataSignature(activation.getActivationId(), C_serverPublicKey, masterPrivateKey);
		if (C_serverPubKeySignature == null) { // in case there is a technical error with signing and null is returned, return random bytes
			C_serverPubKeySignature = new KeyGenerator().generateRandomBytes(71);
		}

		// Compute the response
		PrepareActivationResponse response = new PrepareActivationResponse();
		response.setActivationId(activation.getActivationId());
		response.setActivationNonce(BaseEncoding.base64().encode(activationNonceServer));
		response.setEncryptedServerPublicKey(BaseEncoding.base64().encode(C_serverPublicKey));
		response.setEncryptedServerPublicKeySignature(BaseEncoding.base64().encode(C_serverPubKeySignature));
		response.setEphemeralPublicKey(BaseEncoding.base64().encode(ephemeralPublicKeyBytes));

		return response;
	}

	/**
	 * Commit activation with given ID
	 * @param activationId Activation ID
	 * @return Response with activation commit confirmation
	 * @throws GenericServiceException In case invalid data is provided or activation is not found, in invalid state or already expired
	 */
	public CommitActivationResponse commitActivation(String activationId) throws GenericServiceException {

		ActivationRecordEntity activation = powerAuthRepository.findFirstByActivationId(activationId);

		// Get current timestamp
		Date timestamp = new Date();

		// Does the activation exist?
		if (activation != null) {

			// Check already deactivated activation
			deactivatePendingActivation(timestamp, activation);
			if (activation.getActivationStatus().equals(ActivationStatus.REMOVED)) {
				throw localizationProvider.buildExceptionForCode(ServiceError.ERR0007);
			}

			// Activation is in correct state
			boolean activated = false;
			if (activation.getActivationStatus().equals(ActivationStatus.OTP_USED)) {

				activated = true;
				activation.setActivationStatus(ActivationStatus.ACTIVE);
				powerAuthRepository.save(activation);

				CommitActivationResponse response = new CommitActivationResponse();
				response.setActivationId(activationId);
				response.setActivated(activated);
				return response;
			} else {
				throw localizationProvider.buildExceptionForCode(ServiceError.ERR0008);
			}

		} else {
			// Activation does not exist
			throw localizationProvider.buildExceptionForCode(ServiceError.ERR0009);
		}
	}

	/**
	 * Remove activation with given ID
	 * @param activationId Activation ID
	 * @return Response with confirmation of removal
	 * @throws GenericServiceException In case activation does not exist
	 */
	public RemoveActivationResponse removeActivation(String activationId) throws GenericServiceException {
		ActivationRecordEntity activation = powerAuthRepository.findFirstByActivationId(activationId);
		boolean removed = false;
		if (activation != null) { // does the record even exist?
			removed = true;
			activation.setActivationStatus(ActivationStatus.REMOVED);
			powerAuthRepository.save(activation);

			RemoveActivationResponse response = new RemoveActivationResponse();
			response.setActivationId(activationId);
			response.setRemoved(removed);
			return response;
		} else {
			throw localizationProvider.buildExceptionForCode(ServiceError.ERR0009);
		}
	}

	/**
	 * Block activation with given ID
	 * @param activationId Activation ID
	 * @return Response confirming that activation was blocked
	 * @throws GenericServiceException In case activation does not exist.
	 */
	public BlockActivationResponse blockActivation(String activationId) throws GenericServiceException {
		ActivationRecordEntity activation = powerAuthRepository.findFirstByActivationId(activationId);
		if (activation == null) {
			throw localizationProvider.buildExceptionForCode(ServiceError.ERR0009);
		}

		// does the record even exist, is it in correct state?
		if (activation != null && activation.getActivationStatus().equals(ActivationStatus.ACTIVE)) {
			activation.setActivationStatus(ActivationStatus.BLOCKED);
			powerAuthRepository.save(activation);
		}
		BlockActivationResponse response = new BlockActivationResponse();
		response.setActivationId(activationId);
		response.setActivationStatus(ModelUtil.toServiceStatus(activation.getActivationStatus()));
		return response;
	}

	/**
	 * Unblock activation with given ID
	 * @param activationId Activation ID
	 * @return Response confirming that activation was unblocked
	 * @throws GenericServiceException In case activation does not exist.
	 */
	public UnblockActivationResponse unblockActivation(String activationId) throws GenericServiceException {
		ActivationRecordEntity activation = powerAuthRepository.findFirstByActivationId(activationId);
		if (activation == null) {
			throw localizationProvider.buildExceptionForCode(ServiceError.ERR0009);
		}
		// does the record even exist, is it in correct state?
		if (activation != null && activation.getActivationStatus().equals(ActivationStatus.BLOCKED)) {
			activation.setActivationStatus(ActivationStatus.ACTIVE);
			activation.setFailedAttempts(0L);
			powerAuthRepository.save(activation);
		}
		UnblockActivationResponse response = new UnblockActivationResponse();
		response.setActivationId(activationId);
		response.setActivationStatus(ModelUtil.toServiceStatus(activation.getActivationStatus()));
		return response;
	}

}
