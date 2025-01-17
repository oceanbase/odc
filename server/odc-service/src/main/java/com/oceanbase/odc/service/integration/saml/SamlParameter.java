/*
 * Copyright (c) 2023 OceanBase.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.oceanbase.odc.service.integration.saml;

import static com.oceanbase.odc.service.integration.model.SSOIntegrationConfig.parseOrganizationId;

import javax.annotation.Nullable;

import org.springframework.boot.autoconfigure.security.saml2.Saml2RelyingPartyProperties.AssertingParty;
import org.springframework.boot.autoconfigure.security.saml2.Saml2RelyingPartyProperties.Registration.Acs;
import org.springframework.security.saml2.provider.service.registration.Saml2MessageBinding;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;
import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.core.shared.Verify;
import com.oceanbase.odc.service.common.util.UrlUtils;
import com.oceanbase.odc.service.integration.model.SSOParameter;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
public class SamlParameter implements SSOParameter {

    private String registrationId;
    private String name;

    /**
     * @see Acs#getLocation()
     */
    private String acsLocation = "{baseUrl}/login/saml2/sso/{registrationId}";

    /**
     * URI to the metadata endpoint for discovery-based configuration. If specified singlesignon
     * manually, null is allowed.
     * 
     * @see AssertingParty#getMetadataUri()
     */
    @Nullable
    private String metadataUri;

    /**
     * Ensure request from sp to ldp is not tampered with privateKey generate by server, certificate
     * provided by the user only support one Credential
     * 
     * @see org.springframework.boot.autoconfigure.security.saml2.Saml2RelyingPartyProperties.Registration.Signing
     */

    private String acsEntityId = "{baseUrl}/saml2/service-provider-metadata/{registrationId}";

    /**
     * @see Acs#getBinding()
     */
    private String acsBinding = "POST";


    private Signing signing = new Signing();

    private String providerEntityId;

    /**
     * Ensure request from ldp to sp is not tampered with
     *
     * @see org.springframework.boot.autoconfigure.security.saml2.Saml2RelyingPartyProperties.AssertingParty.Verification
     */
    private Verification verification = new Verification();

    private Singlesignon singlesignon = new Singlesignon();

    /**
     * Used for decrypting the SAML authentication request.
     * 
     * @see org.springframework.boot.autoconfigure.security.saml2.Saml2RelyingPartyProperties.Decryption
     */
    private Decryption decryption = new Decryption();

    public void fillSecret(String decryptSecret) {
        SecretInfo credential = JsonUtils.fromJson(decryptSecret, SecretInfo.class);
        if (credential == null) {
            return;
        }
        if (signing != null) {
            this.signing.privateKey = credential.getSigningPrivateKey();
        }
        if (decryption != null) {
            this.decryption.privateKey = credential.getDecryptionPrivateKey();
        }
    }

    public String resolveLoginUrl() {
        String acsLocationPath = "/login/saml2/sso";
        Verify.verify(acsLocation.contains(acsLocationPath), "invalid acsLocation=" + acsLocation);
        String baseUrl = this.acsLocation.split(acsLocationPath)[0];
        return baseUrl + "/saml2/authenticate/" + registrationId;
    }

    public void amendTest() {
        registrationId = parseOrganizationId(registrationId) + "-" + "test";
        acsLocation = UrlUtils.getUrlHost(acsLocation) + "/login/saml2/sso/" + registrationId;
        acsEntityId = UrlUtils.getUrlHost(acsEntityId) + "/saml2/service-provider-metadata/" + registrationId;
    }

    /**
     * @see AssertingParty#getSinglesignon()
     */
    @Data
    public static class Singlesignon {
        private String url;

        /**
         * Whether to redirect or post authentication requests.
         * 
         * @see Saml2MessageBinding
         */
        private String binding;

        /**
         * Whether to sign authentication requests.
         */
        private Boolean signRequest;
    }

    @Data
    public static class Signing {
        /**
         * generate by server
         */
        @JsonProperty(access = Access.WRITE_ONLY)
        private String privateKey;
        /**
         * provider by user
         */
        private String certificate;
    }

    @Data
    public static class Verification {
        private String certificate;
    }

    @Data
    public static class Decryption {
        @JsonProperty(access = Access.WRITE_ONLY)
        private String privateKey;
        private String certificate;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class SecretInfo {
        private String signingPrivateKey;
        private String decryptionPrivateKey;
    }
}
