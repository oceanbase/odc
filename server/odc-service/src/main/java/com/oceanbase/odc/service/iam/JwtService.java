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
package com.oceanbase.odc.service.iam;

import java.util.Base64;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.jdbc.lock.JdbcLockRegistry;
import org.springframework.stereotype.Service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTCreator;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.TokenExpiredException;
import com.auth0.jwt.interfaces.Claim;
import com.oceanbase.odc.metadb.config.SystemConfigEntity;
import com.oceanbase.odc.service.config.SystemConfigService;
import com.oceanbase.odc.service.config.model.Configuration;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@Data
public class JwtService {

    private static final long TRY_LOCK_TIMEOUT_SECONDS = 5;
    private static final String LOCK_KEY = "ODC_JWT_SECRET_LOCK_KEY";
    private static final String ENCRYPTION_JWT_KEY_SYSTEM_CONFIG_KEY = "odc.iam.auth.jwt.secret-key";
    @Autowired
    private JwtProperties jwtProperties;

    /**
     * Ensure the consistency of automatically generated jwt key in distributed mode
     * 
     * @param
     * @param systemConfigService
     * @param jdbcLockRegistry
     */
    public JwtService(JwtProperties jwtProperties,
            SystemConfigService systemConfigService, JdbcLockRegistry jdbcLockRegistry) {
        try {
            if (StringUtils.isNotBlank(jwtProperties.getTokenSecret())) {
                return;
            }
            log.info("Try to lock odc jwt secret...");
            Lock lock = jdbcLockRegistry.obtain(LOCK_KEY);
            if (lock.tryLock(TRY_LOCK_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                try {
                    log.info("Successfully acquired the jwt secret lock");
                    List<Configuration> list = systemConfigService.queryByKeyPrefix(
                            ENCRYPTION_JWT_KEY_SYSTEM_CONFIG_KEY);
                    if (verifySystemConfig(list)) {
                        jwtProperties.setTokenSecret(list.get(0).getValue());
                    } else {
                        String tokenSecret = UUID.randomUUID().toString();
                        jwtProperties.setTokenSecret(tokenSecret);
                        SystemConfigEntity jwtSecretKey =
                                createSystemConfigEntity(ENCRYPTION_JWT_KEY_SYSTEM_CONFIG_KEY,
                                        tokenSecret, "ODC jwt secret key");
                        systemConfigService.saveConfig(Collections.singletonList(jwtSecretKey));
                    }

                } finally {
                    lock.unlock();
                }
            } else {
                log.info(
                        "Failed to get jwt secret lock, try to get jwt secret from system configuration");
                List<Configuration> list =
                        systemConfigService.queryByKeyPrefix(ENCRYPTION_JWT_KEY_SYSTEM_CONFIG_KEY);
                if (verifySystemConfig(list)) {
                    jwtProperties.setTokenSecret(list.get(0).getValue());;
                } else {
                    throw new RuntimeException("Failed to get jwt secret from system configuration");
                }
            }
        } catch (Exception e) {
            log.warn("Failed to init jwt secret, message={}", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    private boolean verifySystemConfig(List<Configuration> key) {
        return key != null && !key.isEmpty() && Objects.nonNull(key.get(0))
                && StringUtils.isNotBlank(key.get(0).getValue());
    }

    private SystemConfigEntity createSystemConfigEntity(String key, String value, String description) {
        SystemConfigEntity entity = new SystemConfigEntity();
        entity.setKey(key);
        entity.setValue(value);
        entity.setDescription(description);
        return entity;
    }

    /**
     * Generate the jwtToken
     *
     * @param map
     * @return
     */
    public String sign(Map<String, Object> map) {
        try {
            Date date = new Date(System.currentTimeMillis() + jwtProperties.getExpireTime());
            Algorithm algorithm = Algorithm.HMAC256(jwtProperties.getTokenSecret());
            Map<String, Object> header = new HashMap<>(2);
            header.put("typ", "jwt");
            JWTCreator.Builder builder = JWT.create()
                    .withHeader(header)
                    .withIssuedAt(new Date())
                    .withExpiresAt(date);
            map.entrySet().forEach(entry -> {
                if (entry.getValue() instanceof Integer) {
                    builder.withClaim(entry.getKey(), (Integer) entry.getValue());
                } else if (entry.getValue() instanceof Long) {
                    builder.withClaim(entry.getKey(), (Long) entry.getValue());
                } else if (entry.getValue() instanceof Boolean) {
                    builder.withClaim(entry.getKey(), (Boolean) entry.getValue());
                } else if (entry.getValue() instanceof String) {
                    builder.withClaim(entry.getKey(), String.valueOf(entry.getValue()));
                } else if (entry.getValue() instanceof Double) {
                    builder.withClaim(entry.getKey(), (Double) entry.getValue());
                } else if (entry.getValue() instanceof Date) {
                    builder.withClaim(entry.getKey(), (Date) entry.getValue());
                }
            });
            return builder.sign(algorithm);
        } catch (Exception e) {
            return null;
        }
    }

    public boolean verify(String token) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(jwtProperties.getTokenSecret());
            JWTVerifier verifier = JWT.require(algorithm).build();
            verifier.verify(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public Map<String, Claim> getClaims(String token) {
        Algorithm algorithm = Algorithm.HMAC256(jwtProperties.getTokenSecret());
        JWTVerifier verifier = JWT.require(algorithm).build();
        return verifier.verify(token).getClaims();
    }

    public Date getExpiresAt(String token) {
        Algorithm algorithm = Algorithm.HMAC256(jwtProperties.getTokenSecret());
        return JWT.require(algorithm).build().verify(token).getExpiresAt();
    }

    public Date getIssuedAt(String token) {
        Algorithm algorithm = Algorithm.HMAC256(jwtProperties.getTokenSecret());
        return JWT.require(algorithm).build().verify(token).getIssuedAt();
    }

    public boolean isExpired(Date expiration) {
        try {
            return expiration.before(new Date());
        } catch (TokenExpiredException e) {
            return true;
        }
    }

    public boolean isRenew(Date expiration) {
        return (expiration.getTime() - System.currentTimeMillis()) < jwtProperties.getBufferTime();
    }

    public String getHeaderByBase64(String token) {
        if (StringUtils.isEmpty(token)) {
            return null;
        } else {
            return new String(Base64.getDecoder().decode(token.split("\\.")[0]));
        }
    }

    public String getPayloadByBase64(String token) {
        if (StringUtils.isEmpty(token)) {
            return null;
        } else {
            return new String(Base64.getDecoder().decode(token.split("\\.")[1]));
        }
    }

}
