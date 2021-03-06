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

/**
 * Interface containing all methods that are published by the PowerAuth 2.0 Server
 * instance. These methods are then used to publish both SOAP and REST interface.
 * 
 * @author Petr Dvorak.
 *
 */
public interface PowerAuthService {

	/**
	 * Get PowerAuth 2.0 Server system status.
	 * @param request Empty object.
	 * @return System status.
	 * @throws Exception In case of a business logic error.
	 */
	public GetSystemStatusResponse getSystemStatus(GetSystemStatusRequest request) throws Exception;
	
	/**
	 * Get activations for a given user.
	 * @param request Activation list request object. 
	 * @return Activation list.
	 * @throws Exception In case of a business logic error.
	 */
    public GetActivationListForUserResponse getActivatioListForUser(GetActivationListForUserRequest request) throws Exception;

    /**
     * Get activation status for given activation ID.
     * @param request Activation status request object.
     * @return Activation status.
     * @throws Exception In case of a business logic error.
     */
    public GetActivationStatusResponse getActivationStatus(GetActivationStatusRequest request) throws Exception;
    
    /**
     * Get the list of error codes for given language.
     * @param request Error code list request object.
     * @return Error code list.
     * @throws Exception In case of a business logic error.
     */
    public GetErrorCodeListResponse getErrorCodeList(GetErrorCodeListRequest request) throws Exception;

    /**
     * Initiate a new activation for a given application and user ID. The new activation record is in
	 * CREATED state after calling this method.
     * @param request Init activation request object.
     * @return Activation init data.
     * @throws Exception In case of a business logic error.
     */
    public InitActivationResponse initActivation(InitActivationRequest request) throws Exception;

    /**
     * Receive a PowerAuth 2.0 Client public key and return own PowerAuth 2.0 Server public key. The
     * activation with provided ID is in OTP_USED state adter calling this method.
     * @param request Prepare activation request object.
     * @return Prepare activation response.
     * @throws Exception In case of a business logic error.
     */
    public PrepareActivationResponse prepareActivation(PrepareActivationRequest request) throws Exception;

    /**
     * Verify signature against provided data using activation with given ID. Each call to this method
     * increments a counter associated with an activation with given ID. In case too many failed
     * verification attempts occur (max. fail count is a property of an activation, default is 5),
     * activation is moved to BLOCKED state. In case a successful verification occurs, the fail counter 
     * is reset back to zero.
     * @param request Verify signature request object.
     * @return Signature verification response.
     * @throws Exception In case of a business logic error.
     */
    public VerifySignatureResponse verifySignature(VerifySignatureRequest request) throws Exception;

    /**
     * Commit a created activation. Only activations in OTP_USED state can be committed - in case activation
     * is in other state, exception is raised. In case of successful call of this method, activation with
     * provided ID is in ACTIVE state. 
     * @param request Activation commit request object.
     * @return Activation commit response.
     * @throws Exception In case of a business logic error.
     */
    public CommitActivationResponse commitActivation(CommitActivationRequest request) throws Exception;

    /**
     * Remove activation with given ID - change it's status to REMOVED. Activations in any state can be removed.
     * @param request Activation remove request object.
     * @return Activation remove response.
     * @throws Exception In case of a business logic error.
     */
    public RemoveActivationResponse removeActivation(RemoveActivationRequest request) throws Exception;

    /**
     * Block activation with given ID. Activation moves to BLOCKED state, only activations in ACTIVE state
     * can be blocked. Attempt to block an activation in incorrect state results in exception.
     * @param request Block activation request object.
     * @return Block activation response.
     * @throws Exception In case of a business logic error.
     */
    public BlockActivationResponse blockActivation(BlockActivationRequest request) throws Exception;

    /**
     * Unblock activation with given ID. Activation moves to ACTIVE state, only activations in BLOCKED state
     * can be blocked. Attempt to unblock an activation in incorrect state results in exception.
     * @param request Unblock activation request object.
     * @return Unblock activation response.
     * @throws Exception In case of a business logic error.
     */
    public UnblockActivationResponse unblockActivation(UnblockActivationRequest request) throws Exception;
    
    /**
     * Return the data for the vault unlock request. Part of the vault unlock process is performing a signature
     * validation - the rules for blocking activation and counter increment are therefore similar as for the
     * {@link PowerAuthService#verifySignature(VerifySignatureRequest)} method. For vaultUnlock, however,
     * counter is incremented by 2 - one for signature validation, second for the transport key derivation.  
     * @param request Vault unlock request object.
     * @return Vault unlock response.
     * @throws Exception In case of a business logic error.
     */
    public VaultUnlockResponse vaultUnlock(VaultUnlockRequest request) throws Exception;
    
    /**
     * Get records from the signature audit log.
     * @param request Signature audit log request.
     * @return Signature audit log response.
     * @throws Exception In case of a business logic error.
     */
    public SignatureAuditResponse getSignatureAuditLog(SignatureAuditRequest request) throws Exception;
    
    /**
     * Get all applications in the system.
     * @param request Application list request object.
     * @return Application list response.
     * @throws Exception In case of a business logic error.
     */
    public GetApplicationListResponse getApplicationList(GetApplicationListRequest request) throws Exception;
    
    /**
     * Get application detail, including application version list.
     * @param request Application detail request object.
     * @return Application detail response.
     * @throws Exception In case of a business logic error.
     */
    public GetApplicationDetailResponse getApplicationDetail(GetApplicationDetailRequest request) throws Exception;
    
    /**
     * Create a new application with given name. Master key pair and default application version is automatically
     * generated when calling this method.
     * @param request Create application request.
     * @return Created application information response.
     * @throws Exception In case of a business logic error.
     */
    public CreateApplicationResponse createApplication(CreateApplicationRequest request) throws Exception;
    
    /**
     * Create a new application version with given name. Each application version has its own APPLICATION_KEY
     * and APPLICATION_SECRET values.
     * @param request Application version create request object.
     * @return Application version create response.
     * @throws Exception In case of a business logic error.
     */
    public CreateApplicationVersionResponse createApplicationVersion(CreateApplicationVersionRequest request) throws Exception;
    
    /**
     * Unsupport an application version. If an application is unsupported, it's APPLICATION_KEY and APPLICATION_SECRET
     * cannot be used for computing a signature.
     * @param request Unsupport application version request.
     * @return Unsupport application version response.
     * @throws Exception In case of a business logic error.
     */
    public UnsupportApplicationVersionResponse unsupportApplicationVersion(UnsupportApplicationVersionRequest request) throws Exception;
    
    /**
     * Support an application version. If an application is supported, it's APPLICATION_KEY and APPLICATION_SECRET
     * can be used for computing a signature.
     * @param request Support application version request.
     * @return Support application version response.
     * @throws Exception In case of a business logic error.
     */
    public SupportApplicationVersionResponse supportApplicationVersion(SupportApplicationVersionRequest request) throws Exception;

}
