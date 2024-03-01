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
package com.oceanbase.odc.common.jwt;

import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTCreator;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.TokenExpiredException;
import com.auth0.jwt.interfaces.Claim;

public class JwtUtils {
    // 过期时间 15分钟
    public static final long EXPIRE_TIME = 15 * 60 * 1000;
    // 缓冲时间 3分钟
    public static final long BUFFER_TIME = 3 * 60 * 1000;
    // 私钥
    private static final String TOKEN_SECRET = "privateKey1234567890nfjkasfnjkhfknf@#$^&*(8(&**^$%$!~";


    /**
     * 生成签名，15分钟过期 根据内部改造，支持6中类型，Integer,Long,Boolean,Double,String,Date
     * 
     * @param map
     * @return
     */
    public static String sign(Map<String, Object> map) {
        try {
            // 设置过期时间
            Date date = new Date(System.currentTimeMillis() + EXPIRE_TIME);
            // 私钥和加密算法
            Algorithm algorithm = Algorithm.HMAC256(TOKEN_SECRET);
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
    public static boolean verify(String token) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(TOKEN_SECRET);
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
    public static Map<String, Claim> getClaims(String token) {
        Algorithm algorithm = Algorithm.HMAC256(TOKEN_SECRET);
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
    public static Date getExpiresAt(String token) {
        Algorithm algorithm = Algorithm.HMAC256(TOKEN_SECRET);
        return JWT.require(algorithm).build().verify(token).getExpiresAt();
    }

    /**
     * 获取jwt发布时间
     */
    public static Date getIssuedAt(String token) {
        Algorithm algorithm = Algorithm.HMAC256(TOKEN_SECRET);
        return JWT.require(algorithm).build().verify(token).getIssuedAt();
    }

    /**
     * 验证token是否失效
     *
     * @param expiration
     * @return true:过期 false:没过期
     */
    public static boolean isExpired(Date expiration) {
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
    public static boolean isRenew(Date expiration) {
        return (expiration.getTime() - new Date().getTime()) < BUFFER_TIME;
    }

    /**
     * 直接Base64解密获取header内容
     * 
     * @param token
     * @return
     */
    public static String getHeaderByBase64(String token) {
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
    public static String getPayloadByBase64(String token) {

        if (StringUtils.isEmpty(token)) {
            return null;
        } else {
            byte[] payload_byte = Base64.getDecoder().decode(token.split("\\.")[1]);
            String payload = new String(payload_byte);
            return payload;
        }

    }



}

