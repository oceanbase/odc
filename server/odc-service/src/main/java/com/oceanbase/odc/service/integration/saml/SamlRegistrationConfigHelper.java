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
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.springframework.boot.autoconfigure.security.saml2.Saml2RelyingPartyProperties.AssertingParty.Verification;
import org.springframework.boot.autoconfigure.security.saml2.Saml2RelyingPartyProperties.Decryption;
import org.springframework.boot.autoconfigure.security.saml2.Saml2RelyingPartyProperties.Registration;
import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.boot.ssl.pem.PemContent;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.UrlResource;
import org.springframework.security.saml2.Saml2Exception;
import org.springframework.security.saml2.core.Saml2X509Credential;
import org.springframework.security.saml2.core.Saml2X509Credential.Saml2X509CredentialType;
import org.springframework.security.saml2.provider.service.registration.AssertingPartyMetadata;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistration;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistration.AssertingPartyDetails;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistration.Builder;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistrations;
import org.springframework.security.saml2.provider.service.registration.Saml2MessageBinding;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import com.oceanbase.odc.common.util.EncodeUtils;
import com.oceanbase.odc.core.shared.Verify;
import com.oceanbase.odc.service.integration.model.SSOIntegrationConfig;
import com.oceanbase.odc.service.integration.saml.SamlParameter.Signing;

import lombok.Setter;

public final class SamlRegistrationConfigHelper {

    private static final ResourceLoader resourceLoader = new DefaultResourceLoader();


    public static RelyingPartyRegistration asRegistration(SSOIntegrationConfig ssoIntegrationConfig) {
        Verify.verify("SAML".equals(ssoIntegrationConfig.getType()), "Invalid type=" + ssoIntegrationConfig.getType());
        SamlParameter parameter = (SamlParameter) ssoIntegrationConfig.getSsoParameter();
        boolean usingMetadata = StringUtils.hasText(parameter.getMetadataUri());
        String id = parameter.getRegistrationId();
        Builder builder = (!usingMetadata) ? RelyingPartyRegistration.withRegistrationId(id)
                : createBuilderUsingMetadata(parameter).registrationId(id);
        builder.assertionConsumerServiceLocation(parameter.getAcsLocation());
        builder.assertionConsumerServiceBinding(Saml2MessageBinding.valueOf(parameter.getAcsBinding()));
        builder.assertingPartyMetadata(mapAssertingParty(parameter));
        builder.signingX509Credentials(
                (credentials) -> addCredentialIfNotNull(credentials, () -> asSigningCredential(parameter)));
        builder.decryptionX509Credentials(
                (credentials) -> addCredentialIfNotNull(credentials, () -> asDecryptionCredential(parameter)));
        builder.assertingPartyMetadata((details) -> details
                .verificationX509Credentials((credentials) -> addCredentialIfNotNull(credentials,
                        () -> asVerificationCredential(parameter))));
        builder.entityId(parameter.getAcsEntityId());
        builder.nameIdFormat(parameter.getNameIdFormat());
        RelyingPartyRegistration registration = builder.build();
        boolean signRequest = registration.getAssertingPartyMetadata().getWantAuthnRequestsSigned();
        validateSigningCredentials(parameter, signRequest);
        return registration;
    }

    private static RelyingPartyRegistration.Builder createBuilderUsingMetadata(SamlParameter parameter) {
        String requiredEntityId = parameter.getProviderEntityId();
        Collection<Builder> candidates = RelyingPartyRegistrations
                .collectionFromMetadataLocation(parameter.getMetadataUri());
        for (RelyingPartyRegistration.Builder candidate : candidates) {
            if (requiredEntityId == null || requiredEntityId.equals(getEntityId(candidate))) {
                return candidate;
            }
        }
        throw new IllegalStateException("No relying party with Entity ID '" + requiredEntityId + "' found");
    }

    private static Object getEntityId(RelyingPartyRegistration.Builder candidate) {
        String[] result = new String[1];
        candidate.assertingPartyMetadata((builder) -> result[0] = builder.build().getEntityId());
        return result[0];
    }


    private void validateSigningCredentials(Registration properties, boolean signRequest) {
        if (signRequest) {
            Assert.state(!properties.getSigning().getCredentials().isEmpty(),
                    "Signing credentials must not be empty when authentication requests require signing.");
        }
    }

    private Saml2X509Credential asSigningCredential(Registration.Signing.Credential properties) {
        RSAPrivateKey privateKey = readPrivateKey(properties.getPrivateKeyLocation());
        X509Certificate certificate = readCertificate(properties.getCertificateLocation());
        return new Saml2X509Credential(privateKey, certificate, Saml2X509CredentialType.SIGNING);
    }

    private Saml2X509Credential asDecryptionCredential(Decryption.Credential properties) {
        RSAPrivateKey privateKey = readPrivateKey(properties.getPrivateKeyLocation());
        X509Certificate certificate = readCertificate(properties.getCertificateLocation());
        return new Saml2X509Credential(privateKey, certificate, Saml2X509CredentialType.DECRYPTION);
    }

    private Saml2X509Credential asVerificationCredential(Verification.Credential properties) {
        X509Certificate certificate = readCertificate(properties.getCertificateLocation());
        return new Saml2X509Credential(certificate, Saml2X509Credential.Saml2X509CredentialType.ENCRYPTION,
                Saml2X509Credential.Saml2X509CredentialType.VERIFICATION);
    }

    private RSAPrivateKey readPrivateKey(Resource location) {
        Assert.state(location != null, "No private key location specified");
        Assert.state(location.exists(), () -> "Private key location '" + location + "' does not exist");
        try (InputStream inputStream = location.getInputStream()) {
            PemContent pemContent = PemContent.load(inputStream);
            PrivateKey privateKey = pemContent.getPrivateKey();
            Assert.isInstanceOf(RSAPrivateKey.class, privateKey,
                    "PrivateKey in resource '" + location + "' must be an RSAPrivateKey");
            return (RSAPrivateKey) privateKey;
        } catch (Exception ex) {
            throw new IllegalArgumentException(ex);
        }
    }

    private X509Certificate readCertificate(Resource location) {
        Assert.state(location != null, "No certificate location specified");
        Assert.state(location.exists(), () -> "Certificate  location '" + location + "' does not exist");
        try (InputStream inputStream = location.getInputStream()) {
            PemContent pemContent = PemContent.load(inputStream);
            List<X509Certificate> certificates = pemContent.getCertificates();
            return certificates.get(0);
        } catch (Exception ex) {
            throw new IllegalArgumentException(ex);
        }
    }

    public static RelyingPartyRegistration.Builder fromMetadataLocation(String metadataLocation) {
        Resource resource = resourceLoader.getResource(metadataLocation);
        if (resource instanceof UrlResource) {
            UrlResource urlResource = (UrlResource) resource;
            resource = new TimeoutUrlResourceAdaptor(urlResource.getURL());
        }
        try (InputStream source = resource.getInputStream()) {
            return RelyingPartyRegistrations.fromMetadata(source);
        } catch (IOException ex) {
            if (ex.getCause() instanceof Saml2Exception) {
                throw (Saml2Exception) ex.getCause();
            }
            throw new Saml2Exception(ex);
        }
    }

    private static void addCredentialIfNotNull(Collection<Saml2X509Credential> credentials,
            Supplier<Saml2X509Credential> supplier) {
        Saml2X509Credential saml2X509Credential = supplier.get();
        if (saml2X509Credential != null) {
            credentials.add(saml2X509Credential);
        }
    }

    private static Consumer<AssertingPartyMetadata.Builder<?>> mapAssertingParty(SamlParameter parameter) {
        return (details) -> {
            PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
            map.from(parameter::getProviderEntityId).to(details::entityId);
            map.from(() -> Saml2MessageBinding.valueOf(parameter.getSinglesignon().getBinding()))
                    .to(details::singleSignOnServiceBinding);
            map.from(parameter.getSinglesignon()::getUrl).to(details::singleSignOnServiceLocation);
            map.from(parameter.getSinglesignon()::getSignRequest).to(details::wantAuthnRequestsSigned);
        };
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

    @Setter
    static class TimeoutUrlResourceAdaptor extends UrlResource {

        private int connectTimeout = 2000;
        private int readTimeout = 2000;

        public TimeoutUrlResourceAdaptor(URL url) {
            super(url);
        }

        @Override
        protected void customizeConnection(HttpURLConnection con) throws IOException {
            con.setConnectTimeout(connectTimeout);
            con.setReadTimeout(readTimeout);
        }
    }
}
