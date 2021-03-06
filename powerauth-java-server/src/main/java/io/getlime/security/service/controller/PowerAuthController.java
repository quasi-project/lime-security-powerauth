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
package io.getlime.security.service.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

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
import io.getlime.security.service.PowerAuthService;

/**
 * Class implementing the RESTful controller for PowerAuth service.
 *  
 * @author Petr Dvorak
 *
 */
@Controller
@RequestMapping(value = "/rest/pa")
public class PowerAuthController {

    @Autowired
    private PowerAuthService powerAuthService;
    
    /**
     * Call {@link PowerAuthService#getSystemStatus(GetSystemStatusRequest)} method and
     * return the response.
     * @param request Get system status request.
     * @return System status response.
     * @throws Exception In case the service throws exception.
     */
    @RequestMapping(value = "/status", method = RequestMethod.POST)
    public @ResponseBody RESTResponseWrapper<GetSystemStatusResponse> getSystemStatus(@RequestBody RESTRequestWrapper<GetSystemStatusRequest> request) throws Exception {
        return new RESTResponseWrapper<>("OK", powerAuthService.getSystemStatus(request.getRequestObject()));
    }
    
    /**
     * Call {@link PowerAuthService#getErrorCodeList(GetErrorCodeListRequest)} method and
     * return the response.
     * @param request Request for list of error codes indicating a language to be returned in.
     * @return Response with the list of error codes..
     * @throws Exception In case the service throws exception.
     */
    @RequestMapping(value = "/error/list", method = RequestMethod.POST)
    public @ResponseBody RESTResponseWrapper<GetErrorCodeListResponse> getErrorCodeList(@RequestBody RESTRequestWrapper<GetErrorCodeListRequest> request) throws Exception {
        return new RESTResponseWrapper<>("OK", powerAuthService.getErrorCodeList(request.getRequestObject()));
    }

    /**
     * Call {@link PowerAuthService#initActivation(InitActivationRequest)} method and
     * return the response.
     * @param request Init activation request.
     * @return Init activation response.
     * @throws Exception In case the service throws exception.
     */
    @RequestMapping(value = "/activation/init", method = RequestMethod.POST)
    public @ResponseBody RESTResponseWrapper<InitActivationResponse> initActivation(@RequestBody RESTRequestWrapper<InitActivationRequest> request) throws Exception {
        return new RESTResponseWrapper<>("OK", powerAuthService.initActivation(request.getRequestObject()));
    }

    /**
     * Call {@link PowerAuthService#prepareActivation(PrepareActivationRequest)} method and
     * return the response.
     * @param request Prepare activation request.
     * @return Prepare activation response.
     * @throws Exception In case the service throws exception.
     */
    @RequestMapping(value = "/activation/prepare", method = RequestMethod.POST)
    public @ResponseBody RESTResponseWrapper<PrepareActivationResponse> prepareActivation(@RequestBody RESTRequestWrapper<PrepareActivationRequest> request) throws Exception {
        return new RESTResponseWrapper<>("OK", powerAuthService.prepareActivation(request.getRequestObject()));
    }

    /**
     * Call {@link PowerAuthService#commitActivation(CommitActivationRequest)} method and
     * return the response.
     * @param request Commit activation request.
     * @return Commit activation response.     
     * @throws Exception In case the service throws exception.
     */
    @RequestMapping(value = "/activation/commit", method = RequestMethod.POST)
    public @ResponseBody RESTResponseWrapper<CommitActivationResponse> commitActivation(@RequestBody RESTRequestWrapper<CommitActivationRequest> request) throws Exception {
        return new RESTResponseWrapper<>("OK", powerAuthService.commitActivation(request.getRequestObject()));
    }

    /**
     * Call {@link PowerAuthService#getActivationStatus(GetActivationStatusRequest)} method and
     * return the response.
     * @param request Activation status request.
     * @return Activation status response.
     * @throws Exception In case the service throws exception.
     */
    @RequestMapping(value = "/activation/status", method = RequestMethod.POST)
    public @ResponseBody RESTResponseWrapper<GetActivationStatusResponse> getActivationStatus(@RequestBody RESTRequestWrapper<GetActivationStatusRequest> request) throws Exception {
        return new RESTResponseWrapper<>("OK", powerAuthService.getActivationStatus(request.getRequestObject()));
    }

    /**
     * Call {@link PowerAuthService#removeActivation(RemoveActivationRequest)} method and
     * return the response.
     * @param request Remove activation request.
     * @return Remove activation response.
     * @throws Exception In case the service throws exception.
     */
    @RequestMapping(value = "/activation/remove", method = RequestMethod.POST)
    public @ResponseBody RESTResponseWrapper<RemoveActivationResponse> removeActivation(@RequestBody RESTRequestWrapper<RemoveActivationRequest> request) throws Exception {
        return new RESTResponseWrapper<>("OK", powerAuthService.removeActivation(request.getRequestObject()));
    }

    /**
     * Call {@link PowerAuthService#getActivatioListForUser(GetActivationListForUserRequest)} method and
     * return the response.
     * @param request Activation list request.
     * @return Activation list response.
     * @throws Exception In case the service throws exception.
     */
    @RequestMapping(value = "/activation/list", method = RequestMethod.POST)
    public @ResponseBody RESTResponseWrapper<GetActivationListForUserResponse> getActivatioListForUser(@RequestBody RESTRequestWrapper<GetActivationListForUserRequest> request) throws Exception {
        return new RESTResponseWrapper<>("OK", powerAuthService.getActivatioListForUser(request.getRequestObject()));
    }

    /**
     * Call {@link PowerAuthService#verifySignature(VerifySignatureRequest)} method and
     * return the response.
     * @param request Verify signature request.
     * @return Verify signature response.
     * @throws Exception In case the service throws exception.
     */
    @RequestMapping(value = "/signature/verify", method = RequestMethod.POST)
    public @ResponseBody RESTResponseWrapper<VerifySignatureResponse> verifySignature(@RequestBody RESTRequestWrapper<VerifySignatureRequest> request) throws Exception {
        return new RESTResponseWrapper<>("OK", powerAuthService.verifySignature(request.getRequestObject()));
    }
    
    /**
     * Call {@link PowerAuthService#getSignatureAuditLog(SignatureAuditRequest)} method and
     * return the response.
     * @param request Signature audit request.
     * @return Signature audit response.
     * @throws Exception In case the service throws exception.
     */
    @RequestMapping(value = "/signature/list", method = RequestMethod.POST)
    public @ResponseBody RESTResponseWrapper<SignatureAuditResponse> getSignatureAuditLog(@RequestBody RESTRequestWrapper<SignatureAuditRequest> request) throws Exception {
        return new RESTResponseWrapper<>("OK", powerAuthService.getSignatureAuditLog(request.getRequestObject()));
    }

    /**
     * Call {@link PowerAuthService#blockActivation(BlockActivationRequest)} method and
     * return the response.
     * @param request Block activation request.
     * @return Block activation response.
     * @throws Exception In case the service throws exception.
     */
    @RequestMapping(value = "/activation/block", method = RequestMethod.POST)
    public @ResponseBody RESTResponseWrapper<BlockActivationResponse> blockActivation(@RequestBody RESTRequestWrapper<BlockActivationRequest> request) throws Exception {
        return new RESTResponseWrapper<>("OK", powerAuthService.blockActivation(request.getRequestObject()));
    }

    /**
     * Call {@link PowerAuthService#unblockActivation(UnblockActivationRequest)} method and
     * return the response.
     * @param request Unblock activation request.
     * @return Unblock activation response.
     * @throws Exception In case the service throws exception.
     */
    @RequestMapping(value = "/activation/unblock", method = RequestMethod.POST)
    public @ResponseBody RESTResponseWrapper<UnblockActivationResponse> unblockActivation(@RequestBody RESTRequestWrapper<UnblockActivationRequest> request) throws Exception {
        return new RESTResponseWrapper<>("OK", powerAuthService.unblockActivation(request.getRequestObject()));
    }
    
    /**
     * Call {@link PowerAuthService#vaultUnlock(VaultUnlockRequest)} method and
     * return the response.
     * @param request Vault unlock request.
     * @return Vault unlock response.
     * @throws Exception In case the service throws exception.
     */
    @RequestMapping(value = "/vault/unlock", method = RequestMethod.POST)
    public @ResponseBody RESTResponseWrapper<VaultUnlockResponse> vaultUnlock(@RequestBody RESTRequestWrapper<VaultUnlockRequest> request) throws Exception {
        return new RESTResponseWrapper<>("OK", powerAuthService.vaultUnlock(request.getRequestObject()));
    }
        
    /**
     * Call {@link PowerAuthService#getApplicationList(GetApplicationListRequest)} method and
     * return the response.
     * @param request Application list request.
     * @return Application list response.
     * @throws Exception In case the service throws exception.
     */
    @RequestMapping(value = "/application/list", method = RequestMethod.POST)
    public @ResponseBody RESTResponseWrapper<GetApplicationListResponse> getApplicationList(@RequestBody RESTRequestWrapper<GetApplicationListRequest> request) throws Exception {
        return new RESTResponseWrapper<>("OK", powerAuthService.getApplicationList(request.getRequestObject()));
    }
    
    /**
     * Call {@link PowerAuthService#getApplicationDetail(GetApplicationDetailRequest)} method and
     * return the response.
     * @param request Application detail request.
     * @return Application detail response.
     * @throws Exception In case the service throws exception.
     */
    @RequestMapping(value = "/application/detail", method = RequestMethod.POST)
    public @ResponseBody RESTResponseWrapper<GetApplicationDetailResponse> getApplicationDetail(@RequestBody RESTRequestWrapper<GetApplicationDetailRequest> request) throws Exception {
        return new RESTResponseWrapper<>("OK", powerAuthService.getApplicationDetail(request.getRequestObject()));
    }
    
    /**
     * Call {@link PowerAuthService#createApplication(CreateApplicationRequest)} method and
     * return the response.
     * @param request Create application request.
     * @return Create application response.
     * @throws Exception In case the service throws exception.
     */
    @RequestMapping(value = "/application/create", method = RequestMethod.POST)
    public @ResponseBody RESTResponseWrapper<CreateApplicationResponse> createApplication(@RequestBody RESTRequestWrapper<CreateApplicationRequest> request) throws Exception {
        return new RESTResponseWrapper<>("OK", powerAuthService.createApplication(request.getRequestObject()));
    }
    
    /**
     * Call {@link PowerAuthService#createApplicationVersion(CreateApplicationVersionRequest)} method and
     * return the response.
     * @param request Create application version request.
     * @return Create application version response.
     * @throws Exception In case the service throws exception.
     */
    @RequestMapping(value = "/application/version/create", method = RequestMethod.POST)
    public @ResponseBody RESTResponseWrapper<CreateApplicationVersionResponse> createApplicationVersion(@RequestBody RESTRequestWrapper<CreateApplicationVersionRequest> request) throws Exception {
        return new RESTResponseWrapper<>("OK", powerAuthService.createApplicationVersion(request.getRequestObject()));
    }

    /**
     * Call {@link PowerAuthService#unsupportApplicationVersion(UnsupportApplicationVersionRequest)} method and
     * return the response.
     * @param request Unsupport application version request.
     * @return Unsupport application version response.
     * @throws Exception In case the service throws exception.
     */
    @RequestMapping(value = "/application/version/unsupport", method = RequestMethod.POST)
    public @ResponseBody RESTResponseWrapper<UnsupportApplicationVersionResponse> unsupportApplicationVersion(@RequestBody RESTRequestWrapper<UnsupportApplicationVersionRequest> request) throws Exception {
        return new RESTResponseWrapper<>("OK", powerAuthService.unsupportApplicationVersion(request.getRequestObject()));
    }
    
    /**
     * Call {@link PowerAuthService#supportApplicationVersion(SupportApplicationVersionRequest)} method and
     * return the response.
     * @param request Support application version request.
     * @return Support application version response.
     * @throws Exception In case the service throws exception.
     */
    @RequestMapping(value = "/application/version/support", method = RequestMethod.POST)
    public @ResponseBody RESTResponseWrapper<SupportApplicationVersionResponse> supportApplicationVersion(@RequestBody RESTRequestWrapper<SupportApplicationVersionRequest> request) throws Exception {
        return new RESTResponseWrapper<>("OK", powerAuthService.supportApplicationVersion(request.getRequestObject()));
    }

}
