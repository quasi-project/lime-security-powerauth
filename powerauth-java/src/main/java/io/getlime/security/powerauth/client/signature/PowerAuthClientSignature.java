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
package io.getlime.security.powerauth.client.signature;

import io.getlime.security.powerauth.lib.util.SignatureUtils;
import java.security.InvalidKeyException;
import java.util.List;

import javax.crypto.SecretKey;

/**
 * Class implementing client-side signature related processes.
 *  
 * @author Petr Dvorak
 *
 */
public class PowerAuthClientSignature {

    private final SignatureUtils signatureUtils = new SignatureUtils();

    /**
     * Compute a PowerAuth 2.0 signature for given data, signature keys and
     * counter. Signature keys are symmetric keys deduced using
     * private device key KEY_DEVICE_PRIVATE and server public key
     * KEY_SERVER_PUBLIC, and then using KDF function with proper index. See
     * PowerAuth protocol specification for details.
     *
     * @param data Data to be signed.
     * @param signatureKeys A signature keys.
     * @param ctr Counter / index of the derived key KEY_DERIVED.
     * @return PowerAuth 2.0 signature for given data.
     * @throws InvalidKeyException In case signature key is invalid.
     */
    public String signatureForData(
            byte[] data,
            List<SecretKey> signatureKeys,
            long ctr) throws InvalidKeyException {
        return signatureUtils.computePowerAuthSignature(data, signatureKeys, ctr);
    }

}
