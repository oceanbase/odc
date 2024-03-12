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

import java.util.Arrays;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.integration.jdbc.lock.JdbcLockRegistry;
import org.springframework.stereotype.Component;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTCreator;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.TokenExpiredException;
import com.auth0.jwt.interfaces.Claim;
import com.oceanbase.odc.metadb.config.SystemConfigEntity;
import com.oceanbase.odc.service.config.SystemConfigService;
import com.oceanbase.odc.service.config.model.Configuration;
import com.oceanbase.odc.service.encryption.SensitivePropertyHandler;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class JwtService {

    private SensitivePropertyHandler sensitivePropertyHandler;
    // 过期时间 15分钟
    @Value("${odc.iam.auth.jwt.expire-time:15*60*1000}")
    private long expireTime;
    // 缓冲时间 3分钟
    @Value("${odc.iam.auth.jwt.buffer-time:3*60*1000}")
    private long bufferTime;
    // 私钥
    private String tokenSecret;
    private static final long TRY_LOCK_TIMEOUT_SECONDS = 5;

    private static final String LOCK_KEY = "ODC_JWT_SECRET_LOCK_KEY";

    private static final String ENCRYPTION_JWT_KEY_SYSTEM_CONFIG_KEY = "odc.iam.auth.jwt.secret-key";

    private static final int ENCRYPTION_KEY_SIZE = 64;

    /**
     * 确保分布式下密钥一致性
     * 
     * @param tokenSecret
     * @param systemConfigService
     * @param jdbcLockRegistry
     */
    public JwtService(
            @Value("${odc.iam.auth.jwt.secret-key:#{null}}") String tokenSecret,
            @Qualifier("sensitivePropertyHandlerImpl") SensitivePropertyHandler sensitivePropertyHandler,
            SystemConfigService systemConfigService,
            JdbcLockRegistry jdbcLockRegistry) {
        try {
            this.sensitivePropertyHandler = sensitivePropertyHandler;
            if (Objects.nonNull(tokenSecret)) {
                this.tokenSecret = sensitivePropertyHandler.decrypt(tokenSecret);
                return;
            }
            log.info("Try to lock odc jwt secret...");
            Lock lock = jdbcLockRegistry.obtain(LOCK_KEY);
            if (lock.tryLock(TRY_LOCK_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                try {
                    log.info("Successfully acquired the jwt secret lock");
                    // 生成jwt密钥
                    this.tokenSecret = UUID.randomUUID().toString();
                    // 对明文密钥加密
                    String encrypt = sensitivePropertyHandler.encrypt(this.tokenSecret);
                    SystemConfigEntity jwtSecretKey =
                            createSystemConfigEntity(ENCRYPTION_JWT_KEY_SYSTEM_CONFIG_KEY,
                                    encrypt, "ODC jwt secret key");

                    systemConfigService.insert(Arrays.asList(jwtSecretKey));
                } finally {
                    lock.unlock();
                }
            } else {
                log.info(
                        "Failed to get jwt secret lock, try to get jwt secret from system configuration");
                List<Configuration> list =
                        systemConfigService.queryByKeyPrefix(ENCRYPTION_JWT_KEY_SYSTEM_CONFIG_KEY);
                if (verifySystemConfig(list)) {
                    // 获取解密后的密钥
                    this.tokenSecret = sensitivePropertyHandler.decrypt(list.get(0).getValue());
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
        return key != null && !key.isEmpty() && Objects.nonNull(key.get(0)) && Objects.nonNull(key.get(0).getValue());
    }

    private SystemConfigEntity createSystemConfigEntity(String key, String value, String description) {
        SystemConfigEntity entity = new SystemConfigEntity();
        entity.setKey(key);
        entity.setValue(value);
        entity.setDescription(description);
        return entity;
    }


    /**
     * 生成签名，15分钟过期 根据内部改造，支持6中类型，Integer,Long,Boolean,Double,String,Date
     *
     * @param map
     * @return
     */
    public String sign(Map<String, Object> map) {
        try {
            // 设置过期时间
            Date date = new Date(System.currentTimeMillis() + expireTime);
            // 私钥和加密算法
            Algorithm algorithm = Algorithm.HMAC256(tokenSecret);
            // 设置头部信息
            Map<String, Object> header = new HashMap<>(2);
            header.put("typ", "jwt");
            // 返回token字符串
            JWTCreator.Builder builder = JWT.create()
                    .withHeader(header)
                    .withIssuedAt(new Date()) // 发证时间
                    .withExpiresAt(date); // 过期时间
            // .sign(algorithm); //密钥
            // map.entrySet().forEach(entry -> builder.withClaim( entry.getKey(),entry.getValue()));
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
            e.printStackTrace();
            return null;
        }
    }


    /**
     * 检验token是否正确
     *
     * @param **token**
     * @return
     */
    public boolean verify(String token) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(tokenSecret);
            JWTVerifier verifier = JWT.require(algorithm).build();
            verifier.verify(token);

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 获取用户自定义Claim集合
     *
     * @param token
     * @return
     */
    public Map<String, Claim> getClaims(String token) {
        Algorithm algorithm = Algorithm.HMAC256(tokenSecret);
        JWTVerifier verifier = JWT.require(algorithm).build();
        Map<String, Claim> jwt = verifier.verify(token).getClaims();
        return jwt;
    }

    /**
     * 获取过期时间
     *
     * @param token
     * @return
     */
    public Date getExpiresAt(String token) {
        Algorithm algorithm = Algorithm.HMAC256(tokenSecret);
        return JWT.require(algorithm).build().verify(token).getExpiresAt();
    }

    /**
     * 获取jwt发布时间
     */
    public Date getIssuedAt(String token) {
        Algorithm algorithm = Algorithm.HMAC256(tokenSecret);
        return JWT.require(algorithm).build().verify(token).getIssuedAt();
    }

    /**
     * 验证token是否失效
     *
     * @param expiration
     * @return true:过期 false:没过期
     */
    public boolean isExpired(Date expiration) {
        try {
            return expiration.before(new Date());
        } catch (TokenExpiredException e) {
            // e.printStackTrace();
            return true;
        }

    }

    /**
     * 判断是否需要续期
     *
     * @param expiration
     * @return
     */
    public boolean isRenew(Date expiration) {
        return (expiration.getTime() - new Date().getTime()) < bufferTime;
    }

    /**
     * 直接Base64解密获取header内容
     *
     * @param token
     * @return
     */
    public String getHeaderByBase64(String token) {
        if (StringUtils.isEmpty(token)) {
            return null;
        } else {
            byte[] header_byte = Base64.getDecoder().decode(token.split("\\.")[0]);
            String header = new String(header_byte);
            return header;
        }

    }

    /**
     * 直接Base64解密获取payload内容
     *
     * @param token
     * @return
     */
    public String getPayloadByBase64(String token) {

        if (StringUtils.isEmpty(token)) {
            return null;
        } else {
            byte[] payload_byte = Base64.getDecoder().decode(token.split("\\.")[1]);
            String payload = new String(payload_byte);
            return payload;
        }

    }

}
