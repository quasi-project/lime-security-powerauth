/**
 * Copyright 2015 Lime - HighTech Solutions s.r.o.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.getlime.security.service;

import io.getlime.security.powerauth.BlockActivationRequest;
import io.getlime.security.powerauth.BlockActivationResponse;
import io.getlime.security.powerauth.CommitActivationRequest;
import io.getlime.security.powerauth.CommitActivationResponse;
import io.getlime.security.powerauth.CreateApplicationRequest;
import io.getlime.security.powerauth.CreateApplicationResponse;
import io.getlime.security.powerauth.CreateApplicationVersionRequest;
import io.getlime.security.powerauth.CreateApplicationVersionResponse;
import io.getlime.security.powerauth.GetActivationListForUserRequest;
import io.getlime.security.powerauth.GetActivationListForUserResponse;
import io.getlime.security.powerauth.GetActivationStatusRequest;
import io.getlime.security.powerauth.GetActivationStatusResponse;
import io.getlime.security.powerauth.GetApplicationDetailRequest;
import io.getlime.security.powerauth.GetApplicationDetailResponse;
import io.getlime.security.powerauth.GetApplicationListRequest;
import io.getlime.security.powerauth.GetApplicationListResponse;
import io.getlime.security.powerauth.GetErrorCodeListRequest;
import io.getlime.security.powerauth.GetErrorCodeListResponse;
import io.getlime.security.powerauth.GetSystemStatusRequest;
import io.getlime.security.powerauth.GetSystemStatusResponse;
import io.getlime.security.powerauth.InitActivationRequest;
import io.getlime.security.powerauth.InitActivationResponse;
import io.getlime.security.powerauth.PrepareActivationRequest;
import io.getlime.security.powerauth.PrepareActivationResponse;
import io.getlime.security.powerauth.RemoveActivationRequest;
import io.getlime.security.powerauth.RemoveActivationResponse;
import io.getlime.security.powerauth.SignatureAuditRequest;
import io.getlime.security.powerauth.SignatureAuditResponse;
import io.getlime.security.powerauth.SupportApplicationVersionRequest;
import io.getlime.security.powerauth.SupportApplicationVersionResponse;
import io.getlime.security.powerauth.UnblockActivationRequest;
import io.getlime.security.powerauth.UnblockActivationResponse;
import io.getlime.security.powerauth.UnsupportApplicationVersionRequest;
import io.getlime.security.powerauth.UnsupportApplicationVersionResponse;
import io.getlime.security.powerauth.VaultUnlockRequest;
import io.getlime.security.powerauth.VaultUnlockResponse;
import io.getlime.security.powerauth.VerifySignatureRequest;
import io.getlime.security.powerauth.VerifySignatureResponse;
import io.getlime.security.powerauth.lib.config.PowerAuthConfiguration;
import io.getlime.security.powerauth.lib.enums.PowerAuthSignatureTypes;
import io.getlime.security.powerauth.lib.provider.CryptoProviderUtil;
import io.getlime.security.powerauth.lib.provider.CryptoProviderUtilFactory;
import io.getlime.security.service.behavior.ActivationServiceBehavior;
import io.getlime.security.service.behavior.ApplicationServiceBehavior;
import io.getlime.security.service.behavior.AuditingServiceBehavior;
import io.getlime.security.service.behavior.SignatureServiceBehavior;
import io.getlime.security.service.behavior.VaultUnlockServiceBehavior;
import io.getlime.security.service.configuration.PowerAuthServiceConfiguration;
import io.getlime.security.service.exceptions.GenericServiceException;
import io.getlime.security.service.i18n.LocalizationProvider;
import io.getlime.security.service.util.ModelUtil;
import io.getlime.security.service.util.model.ServiceError;

import java.security.InvalidKeyException;
import java.security.Security;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Default implementation of the PowerAuth 2.0 Server service.
 * The implementation of this service is divided into "behaviors"
 * responsible for individual processes.
 * 
 * @author Petr Dvorak
 *
 */
@Component
public class PowerAuthServiceImpl implements PowerAuthService {
	
	@Autowired
	private PowerAuthServiceConfiguration powerAuthServiceConfiguration;

	@Autowired
	private ActivationServiceBehavior activationServiceBehavior;

	@Autowired
	private ApplicationServiceBehavior applicationServiceBehavior;
	
	@Autowired
	private AuditingServiceBehavior auditingServiceBehavior;

	@Autowired
	private SignatureServiceBehavior signatureServiceBehavior;
	
	@Autowired
	private VaultUnlockServiceBehavior vaultUnlockServiceBehavior;
	
	@Autowired
	private LocalizationProvider localizationProvider;
	
	private final CryptoProviderUtil keyConversionUtilities = PowerAuthConfiguration.INSTANCE.getKeyConvertor();

	static {
		Security.addProvider(new BouncyCastleProvider());
		PowerAuthConfiguration.INSTANCE.setKeyConvertor(CryptoProviderUtilFactory.getCryptoProviderUtils());
	}
	
	@Override
	public GetSystemStatusResponse getSystemStatus(GetSystemStatusRequest request) throws Exception {
		GetSystemStatusResponse response = new GetSystemStatusResponse();
		response.setStatus("OK");
		response.setApplicationName(powerAuthServiceConfiguration.getApplicationName());
		response.setApplicationDisplayName(powerAuthServiceConfiguration.getApplicationDisplayName());
		response.setApplicationEnvironment(powerAuthServiceConfiguration.getApplicationEnvironment());
		response.setTimestamp(ModelUtil.calendarWithDate(new Date()));
		return response;
	}
	
	@Override
	public GetErrorCodeListResponse getErrorCodeList(GetErrorCodeListRequest request) throws Exception {
		String language = request.getLanguage();
		// Check if the language is valid ISO language, use EN as default
		if (Arrays.binarySearch(Locale.getISOLanguages(), language) < 0) {
			language = Locale.ENGLISH.getLanguage();
		}
		Locale locale = new Locale(language);
		GetErrorCodeListResponse response = new GetErrorCodeListResponse();
		List<String> errorCodeList = ServiceError.allCodes();
		for (String errorCode : errorCodeList) {
			GetErrorCodeListResponse.Errors error = new GetErrorCodeListResponse.Errors();
			error.setCode(errorCode);
			error.setValue(localizationProvider.getLocalizedErrorMessage(errorCode, locale));
			response.getErrors().add(error);
		}
		return response;
	}

	@Override
	@Transactional
	public GetActivationListForUserResponse getActivatioListForUser(GetActivationListForUserRequest request) throws Exception {
		try {
			String userId = request.getUserId();
			Long applicationId = request.getApplicationId();
			return activationServiceBehavior.getActivationList(applicationId, userId);
		} catch (Exception ex) {
			Logger.getLogger(PowerAuthServiceImpl.class.getName()).log(Level.SEVERE, null, ex);
			throw new GenericServiceException(ServiceError.ERR0000, ex.getMessage(), ex.getLocalizedMessage());
		}
	}

	@Override
	@Transactional
	public GetActivationStatusResponse getActivationStatus(GetActivationStatusRequest request) throws Exception {
		try {
			String activationId = request.getActivationId();
			return activationServiceBehavior.getActivationStatus(activationId, keyConversionUtilities);
		} catch (Exception ex) {
			Logger.getLogger(PowerAuthServiceImpl.class.getName()).log(Level.SEVERE, null, ex);
			throw new GenericServiceException(ServiceError.ERR0000, ex.getMessage(), ex.getLocalizedMessage());
		}

	}

	@Override
	@Transactional
	public InitActivationResponse initActivation(InitActivationRequest request) throws Exception {
		try {
			String userId = request.getUserId();
			Long applicationId = request.getApplicationId();
			Long maxFailedCount = request.getMaxFailureCount();
			Date activationExpireTimestamp = ModelUtil.dateWithCalendar(request.getTimestampActivationExpire());
			return activationServiceBehavior.initActivation(applicationId, userId, maxFailedCount, activationExpireTimestamp, keyConversionUtilities);
		} catch (GenericServiceException ex) {
			Logger.getLogger(PowerAuthServiceImpl.class.getName()).log(Level.SEVERE, null, ex);
			throw ex;
		} catch (InvalidKeySpecException | InvalidKeyException ex) {
			Logger.getLogger(PowerAuthServiceImpl.class.getName()).log(Level.SEVERE, null, ex);
			throw localizationProvider.buildExceptionForCode(ServiceError.ERR0010);
		}
	}

	@Override
	@Transactional
	public PrepareActivationResponse prepareActivation(PrepareActivationRequest request) throws Exception {
		try {
			// Get request parameters
			String activationIdShort = request.getActivationIdShort();
			String activationNonceBase64 = request.getActivationNonce();
			String cDevicePublicKeyBase64 = request.getEncryptedDevicePublicKey();
			String activationName = request.getActivationName();
			String ephemeralPublicKey = request.getEphemeralPublicKey();
			String applicationId = request.getApplicationKey();
			String applicationSignature = request.getApplicationSignature();
			String extras = request.getExtras();
			return activationServiceBehavior.prepareActivation(activationIdShort, activationNonceBase64, ephemeralPublicKey, cDevicePublicKeyBase64, activationName, extras, applicationId, applicationSignature, keyConversionUtilities);
		} catch (IllegalArgumentException ex) {
			Logger.getLogger(PowerAuthServiceImpl.class.getName()).log(Level.SEVERE, null, ex);
			throw localizationProvider.buildExceptionForCode(ServiceError.ERR0011);
		} catch (GenericServiceException ex) {
			Logger.getLogger(PowerAuthServiceImpl.class.getName()).log(Level.SEVERE, null, ex);
			throw ex;
		} catch (Exception ex) {
			Logger.getLogger(PowerAuthServiceImpl.class.getName()).log(Level.SEVERE, null, ex);
			throw new GenericServiceException(ServiceError.ERR0000, ex.getMessage(), ex.getLocalizedMessage());
		}
	}

	private VerifySignatureResponse verifySignatureImplNonTransaction(VerifySignatureRequest request) throws Exception {

		// Get request data
		String activationId = request.getActivationId();
		String applicationKey = request.getApplicationKey();
		String dataString = request.getData();
		String signature = request.getSignature();
		String signatureType = request.getSignatureType().toLowerCase();

		return signatureServiceBehavior.verifySignature(activationId, signatureType, signature, dataString, applicationKey, keyConversionUtilities);
		
	}

	@Override
	@Transactional
	public VerifySignatureResponse verifySignature(VerifySignatureRequest request) throws Exception {
		try {
			return this.verifySignatureImplNonTransaction(request);
		} catch (Exception ex) {
			Logger.getLogger(PowerAuthServiceImpl.class.getName()).log(Level.SEVERE, null, ex);
			throw new GenericServiceException(ServiceError.ERR0000, ex.getMessage(), ex.getLocalizedMessage());
		}
	}

	@Override
	@Transactional
	public CommitActivationResponse commitActivation(CommitActivationRequest request) throws Exception {
		try {
			String activationId = request.getActivationId();
			return activationServiceBehavior.commitActivation(activationId);
		} catch (GenericServiceException ex) {
			Logger.getLogger(PowerAuthServiceImpl.class.getName()).log(Level.SEVERE, null, ex);
			throw ex;
		} catch (Exception ex) {
			Logger.getLogger(PowerAuthServiceImpl.class.getName()).log(Level.SEVERE, null, ex);
			throw new GenericServiceException(ServiceError.ERR0000, ex.getMessage(), ex.getLocalizedMessage());
		}
	}

	@Override
	@Transactional
	public RemoveActivationResponse removeActivation(RemoveActivationRequest request) throws Exception {
		try {
			String activationId = request.getActivationId();
			return activationServiceBehavior.removeActivation(activationId);
		} catch (Exception ex) {
			Logger.getLogger(PowerAuthServiceImpl.class.getName()).log(Level.SEVERE, null, ex);
			throw new GenericServiceException(ServiceError.ERR0000, ex.getMessage(), ex.getLocalizedMessage());
		}
	}

	@Override
	@Transactional
	public BlockActivationResponse blockActivation(BlockActivationRequest request) throws Exception {
		try {
			String activationId = request.getActivationId();
			return activationServiceBehavior.blockActivation(activationId);
		} catch (GenericServiceException ex) {
			Logger.getLogger(PowerAuthServiceImpl.class.getName()).log(Level.SEVERE, null, ex);
			throw ex;
		} catch (Exception ex) {
			Logger.getLogger(PowerAuthServiceImpl.class.getName()).log(Level.SEVERE, null, ex);
			throw new GenericServiceException(ServiceError.ERR0000, ex.getMessage(), ex.getLocalizedMessage());
		}
	}

	@Override
	@Transactional
	public UnblockActivationResponse unblockActivation(UnblockActivationRequest request) throws Exception {
		try {
			String activationId = request.getActivationId();
			return activationServiceBehavior.unblockActivation(activationId);
		} catch (GenericServiceException ex) {
			Logger.getLogger(PowerAuthServiceImpl.class.getName()).log(Level.SEVERE, null, ex);
			throw ex;
		} catch (Exception ex) {
			Logger.getLogger(PowerAuthServiceImpl.class.getName()).log(Level.SEVERE, null, ex);
			throw new GenericServiceException(ServiceError.ERR0000, ex.getMessage(), ex.getLocalizedMessage());
		}

	}

	@Override
	@Transactional
	public VaultUnlockResponse vaultUnlock(VaultUnlockRequest request) throws Exception {
		try {

			// Get request data
			String activationId = request.getActivationId();
			String applicationKey = request.getApplicationKey();
			String signature = request.getSignature();
			String signatureType = request.getSignatureType().toLowerCase();
			String data = request.getData();
			
			// Reject 1FA signatures.
			if (signatureType.equals(PowerAuthSignatureTypes.BIOMETRY.toString())
					|| signatureType.equals(PowerAuthSignatureTypes.KNOWLEDGE.toString())
					|| signatureType.equals(PowerAuthSignatureTypes.POSSESSION.toString())) {
				throw localizationProvider.buildExceptionForCode(ServiceError.ERR0012);
			}

			// Verify the signature
			VerifySignatureRequest verifySignatureRequest = new VerifySignatureRequest();
			verifySignatureRequest.setActivationId(activationId);
			verifySignatureRequest.setApplicationKey(applicationKey);
			verifySignatureRequest.setData(data);
			verifySignatureRequest.setSignature(signature);
			verifySignatureRequest.setSignatureType(signatureType);
			VerifySignatureResponse verifySignatureResponse = this.verifySignatureImplNonTransaction(verifySignatureRequest);

			return vaultUnlockServiceBehavior.unlockVault(activationId, verifySignatureResponse.isSignatureValid(), keyConversionUtilities);
		} catch (GenericServiceException ex) {
			Logger.getLogger(PowerAuthServiceImpl.class.getName()).log(Level.SEVERE, null, ex);
			throw ex;
		} catch (Exception ex) {
			Logger.getLogger(PowerAuthServiceImpl.class.getName()).log(Level.SEVERE, null, ex);
			throw new GenericServiceException(ServiceError.ERR0000, ex.getMessage(), ex.getLocalizedMessage());
		}
	}

	@Override
	@Transactional
	public SignatureAuditResponse getSignatureAuditLog(SignatureAuditRequest request) throws Exception {
		try {

			String userId = request.getUserId();
			Long applicationId = request.getApplicationId();
			Date startingDate = ModelUtil.dateWithCalendar(request.getTimestampFrom());
			Date endingDate = ModelUtil.dateWithCalendar(request.getTimestampTo());

			return auditingServiceBehavior.getSignatureAuditLog(userId, applicationId, startingDate, endingDate);

		} catch (Exception ex) {
			Logger.getLogger(PowerAuthServiceImpl.class.getName()).log(Level.SEVERE, null, ex);
			throw new GenericServiceException(ServiceError.ERR0000, ex.getMessage(), ex.getLocalizedMessage());
		}

	}

	@Override
	@Transactional
	public GetApplicationListResponse getApplicationList(GetApplicationListRequest request) throws Exception {
		return applicationServiceBehavior.getApplicationList();
	}

	@Override
	@Transactional
	public GetApplicationDetailResponse getApplicationDetail(GetApplicationDetailRequest request) throws Exception {
		return applicationServiceBehavior.getApplicationDetail(request.getApplicationId());
	}

	@Override
	@Transactional
	public CreateApplicationResponse createApplication(CreateApplicationRequest request) throws Exception {
		return applicationServiceBehavior.createApplication(request.getApplicationName(), keyConversionUtilities);
	}

	@Override
	@Transactional
	public CreateApplicationVersionResponse createApplicationVersion(CreateApplicationVersionRequest request) throws Exception {
		return applicationServiceBehavior.createApplicationVersion(request.getApplicationId(), request.getApplicationVersionName());
	}

	@Override
	@Transactional
	public UnsupportApplicationVersionResponse unsupportApplicationVersion(UnsupportApplicationVersionRequest request) throws Exception {
		return applicationServiceBehavior.unsupportApplicationVersion(request.getApplicationVersionId());
	}

	@Override
	@Transactional
	public SupportApplicationVersionResponse supportApplicationVersion(SupportApplicationVersionRequest request) throws Exception {
		return applicationServiceBehavior.supportApplicationVersion(request.getApplicationVersionId());
	}

}
