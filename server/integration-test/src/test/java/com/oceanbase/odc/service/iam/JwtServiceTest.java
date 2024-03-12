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

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import com.auth0.jwt.interfaces.Claim;
import com.oceanbase.odc.ServiceTestEnv;

public class JwtServiceTest extends ServiceTestEnv {
    @Value("${odc.iam.auth.jwt.buffer-time:3*60*1000}")
    private long bufferTime;

    @Autowired
    private JwtService jwtService;

    @Test
    public void testSignPositive() {
        Map<String, Object> map = new HashMap<>();
        map.put("userId", 123);
        map.put("username", "testUser");
        String token = jwtService.sign(map);
        Assert.assertNotNull(token);
    }

    @Test
    public void testVerifyPositive() {
        Map<String, Object> map = new HashMap<>();
        map.put("userId", 123);
        map.put("username", "testUser");
        String token = jwtService.sign(map);
        Assert.assertTrue(jwtService.verify(token));
    }

    @Test
    public void testVerifyNegative() {
        Assert.assertFalse(jwtService.verify("invalidToken"));
    }

    @Test
    public void testGetClaims() {
        Map<String, Object> map = new HashMap<>();
        map.put("userId", 123);
        map.put("username", "testUser");
        String token = jwtService.sign(map);
        Map<String, Claim> claims = jwtService.getClaims(token);
        Assert.assertNotNull(claims);
        Assert.assertEquals(new Integer(123), claims.get("userId").asInt());
        Assert.assertEquals("testUser", claims.get("username").asString());
    }

    @Test
    public void testGetExpiresAt() {
        Map<String, Object> map = new HashMap<>();
        map.put("userId", 123);
        map.put("username", "testUser");
        String token = jwtService.sign(map);
        Date expiresAt = jwtService.getExpiresAt(token);
        Assert.assertNotNull(expiresAt);
    }

    @Test
    public void testGetIssuedAt() {
        Map<String, Object> map = new HashMap<>();
        map.put("userId", 123);
        map.put("username", "testUser");
        String token = jwtService.sign(map);
        Date issuedAt = jwtService.getIssuedAt(token);
        Assert.assertNotNull(issuedAt);
    }

    @Test
    public void testIsExpired() {
        Date expiration = new Date(System.currentTimeMillis() - 1000);
        Assert.assertTrue(jwtService.isExpired(expiration));
    }

    @Test
    public void testIsRenew() {
        Date expiration = new Date(System.currentTimeMillis() + bufferTime - 1000);
        Assert.assertTrue(jwtService.isRenew(expiration));
    }

    @Test
    public void testGetHeaderByBase64() {
        Map<String, Object> map = new HashMap<>();
        map.put("userId", 123);
        map.put("username", "testUser");
        String token = jwtService.sign(map);
        String header = jwtService.getHeaderByBase64(token);
        Assert.assertEquals("{\"typ\":\"JWT\",\"alg\":\"HS256\"}", header);
    }

}
