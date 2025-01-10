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

import static com.oceanbase.odc.service.integration.util.EncryptionUtil.CERTIFICATE_KEY_PREFIX;
import static com.oceanbase.odc.service.integration.util.EncryptionUtil.CERTIFICATE_KEY_SUFFIX;
import static com.oceanbase.odc.service.integration.util.EncryptionUtil.PRIVATE_KEY_PREFIX;
import static com.oceanbase.odc.service.integration.util.EncryptionUtil.PRIVATE_KEY_SUFFIX;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.security.KeyFactory;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Collection;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.security.saml2.core.Saml2X509Credential;
import org.springframework.security.saml2.core.Saml2X509Credential.Saml2X509CredentialType;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistration;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistration.AssertingPartyDetails;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistration.Builder;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistrations;
import org.springframework.security.saml2.provider.service.registration.Saml2MessageBinding;
import org.springframework.util.StringUtils;

import com.oceanbase.odc.common.util.EncodeUtils;
import com.oceanbase.odc.core.shared.Verify;
import com.oceanbase.odc.service.integration.model.SSOIntegrationConfig;
import com.oceanbase.odc.service.integration.saml.SamlParameter.Signing;

public final class SamlRegistrationConfigHelper {

    public static RelyingPartyRegistration asRegistration(SSOIntegrationConfig ssoIntegrationConfig) {
        Verify.verify("SAML".equals(ssoIntegrationConfig.getType()), "Invalid type=" + ssoIntegrationConfig.getType());
        SamlParameter parameter = (SamlParameter) ssoIntegrationConfig.getSsoParameter();
        boolean usingMetadata = StringUtils.hasText(parameter.getMetadataUri());
        Builder builder = (usingMetadata)
                ? RelyingPartyRegistrations.fromMetadataLocation(parameter.getMetadataUri())
                        .registrationId(parameter.getRegistrationId())
                : RelyingPartyRegistration.withRegistrationId(parameter.getRegistrationId());
        builder.assertionConsumerServiceLocation(parameter.getAcsLocation());
        builder.assertionConsumerServiceBinding(Saml2MessageBinding.valueOf(parameter.getAcsBinding()));
        builder.assertingPartyDetails(mapAssertingParty(parameter, usingMetadata));
        builder.signingX509Credentials(
                (credentials) -> addCredentialIfNotNull(credentials, () -> asSigningCredential(parameter)));
        builder.decryptionX509Credentials(
                (credentials) -> addCredentialIfNotNull(credentials, () -> asDecryptionCredential(parameter)));
        builder.assertingPartyDetails((details) -> details
                .verificationX509Credentials((credentials) -> addCredentialIfNotNull(credentials,
                        () -> asVerificationCredential(parameter))));
        builder.entityId(parameter.getAcsEntityId());
        RelyingPartyRegistration registration = builder.build();
        boolean signRequest = registration.getAssertingPartyDetails().getWantAuthnRequestsSigned();
        validateSigningCredentials(parameter, signRequest);
        return registration;
    }

    private static void addCredentialIfNotNull(Collection<Saml2X509Credential> credentials,
            Supplier<Saml2X509Credential> supplier) {
        Saml2X509Credential saml2X509Credential = supplier.get();
        if (saml2X509Credential != null) {
            credentials.add(saml2X509Credential);
        }
    }

    private static Consumer<AssertingPartyDetails.Builder> mapAssertingParty(SamlParameter parameter,
            boolean usingMetadata) {
        return (details) -> {
            PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
            map.from(parameter::getProviderEntityId).to(details::entityId);
            map.from(() -> Optional.ofNullable(parameter.getSinglesignon())
                    .map(SamlParameter.Singlesignon::getBinding)
                    .map(Saml2MessageBinding::valueOf)
                    .orElse(null))
                    .to(details::singleSignOnServiceBinding);
            map.from(() -> Optional.ofNullable(parameter.getSinglesignon())
                    .map(SamlParameter.Singlesignon::getUrl)
                    .orElse(null))
                    .to(details::singleSignOnServiceLocation);
            map.from(() -> Optional.ofNullable(parameter.getSinglesignon())
                    .map(SamlParameter.Singlesignon::getSignRequest)
                    .orElse(null))
                    .when((ignored) -> !usingMetadata)
                    .to(details::wantAuthnRequestsSigned);
        };
    }

    private static Saml2X509Credential asSigningCredential(SamlParameter parameter) {
        Signing signing = parameter.getSigning();
        if (signing == null || signing.getCertificate() == null) {
            return null;
        }
        return asSaml2X509Credential(signing.getPrivateKey(), signing.getCertificate());
    }

    private static Saml2X509Credential asVerificationCredential(SamlParameter parameter) {
        if (parameter.getVerification() == null || parameter.getVerification().getCertificate() == null) {
            return null;
        }
        X509Certificate x509Certificate = getCertificateFromBase64(parameter.getVerification().getCertificate());
        return new Saml2X509Credential(x509Certificate, Saml2X509Credential.Saml2X509CredentialType.ENCRYPTION,
                Saml2X509Credential.Saml2X509CredentialType.VERIFICATION);
    }

    private static Saml2X509Credential asDecryptionCredential(SamlParameter parameter) {
        if (parameter.getDecryption() == null || parameter.getDecryption().getCertificate() == null) {
            return null;
        }
        Verify.notNull(parameter.getDecryption().getCertificate(), "certificate");
        Verify.notNull(parameter.getDecryption().getPrivateKey(), "privateKey");
        return asSaml2X509Credential(parameter.getDecryption().getPrivateKey(),
                parameter.getDecryption().getCertificate());
    }

    private static Saml2X509Credential asSaml2X509Credential(String base64PrivateKey, String base64Certificate) {
        RSAPrivateKey rsaPrivateKey = base64ToRSAPrivateKey(base64PrivateKey);
        X509Certificate x509Certificate = getCertificateFromBase64(base64Certificate);
        return new Saml2X509Credential(rsaPrivateKey, x509Certificate, Saml2X509CredentialType.SIGNING);
    }

    private static RSAPrivateKey base64ToRSAPrivateKey(String base64PrivateKey) {
        try {
            byte[] privateKeyBytes = EncodeUtils.base64DecodeFromString(removeBase64PrivateKeyPem(base64PrivateKey));
            Verify.notNull(privateKeyBytes, "privateKeyBytes cannot be null");
            PKCS8EncodedKeySpec pkcs8EncodedKeySpec = new PKCS8EncodedKeySpec(privateKeyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            return (RSAPrivateKey) keyFactory.generatePrivate(pkcs8EncodedKeySpec);
        } catch (Exception e) {
            throw new RuntimeException("Invalid base64PrivateKey=" + base64PrivateKey, e);
        }
    }

    public static String removeBase64PrivateKeyPem(String base64PrivateKeyPem) {
        return base64PrivateKeyPem
                .replace(PRIVATE_KEY_PREFIX, "")
                .replace(PRIVATE_KEY_SUFFIX, "")
                .replaceAll("\\s", "");
    }

    private static X509Certificate getCertificateFromBase64(String base64Certificate) {
        try {
            byte[] certificateBytes = EncodeUtils.base64DecodeFromString(removeBase64CertificatePem(base64Certificate));
            CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
            Verify.notNull(certificateFactory, "certificateFactory cannot be null");
            InputStream in = new ByteArrayInputStream(certificateBytes);
            return (X509Certificate) certificateFactory.generateCertificate(in);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid Base64 certificate=" + base64Certificate, e);
        }
    }

    public static String removeBase64CertificatePem(String base64Certificate) {
        return base64Certificate
                .replace(CERTIFICATE_KEY_PREFIX, "")
                .replace(CERTIFICATE_KEY_SUFFIX, "")
                .replace("\\n", "")
                .replaceAll("\\s", "");
    }

    private static void validateSigningCredentials(SamlParameter parameter, boolean signRequest) {
        if (signRequest) {
            Verify.verify(
                    parameter.getSigning() != null && parameter.getSigning().getCertificate() != null
                            && parameter.getSigning().getPrivateKey() != null,
                    "Signing credentials must not be empty when authentication requests require signing.");
        }
    }
}
