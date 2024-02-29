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

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import com.auth0.jwt.interfaces.Claim;

public class JwtUtilsTest {
    private static final long BUFFER_TIME = 3 * 60 * 1000;

    @Test
    public void testSignPositive() {
        Map<String, Object> map = new HashMap<>();
        map.put("userId", 123);
        map.put("username", "testUser");
        String token = JwtUtils.sign(map);
        Assert.assertNotNull(token);
    }

    @Test
    public void testVerifyPositive() {
        Map<String, Object> map = new HashMap<>();
        map.put("userId", 123);
        map.put("username", "testUser");
        String token = JwtUtils.sign(map);
        Assert.assertTrue(JwtUtils.verify(token));
    }

    @Test
    public void testVerifyNegative() {
        Assert.assertFalse(JwtUtils.verify("invalidToken"));
    }

    @Test
    public void testGetClaims() {
        Map<String, Object> map = new HashMap<>();
        map.put("userId", 123);
        map.put("username", "testUser");
        String token = JwtUtils.sign(map);
        Map<String, Claim> claims = JwtUtils.getClaims(token);
        Assert.assertNotNull(claims);
        Assert.assertEquals(new Integer(123), claims.get("userId").asInt());
        Assert.assertEquals("testUser", claims.get("username").asString());
    }

    @Test
    public void testGetExpiresAt() {
        Map<String, Object> map = new HashMap<>();
        map.put("userId", 123);
        map.put("username", "testUser");
        String token = JwtUtils.sign(map);
        Date expiresAt = JwtUtils.getExpiresAt(token);
        Assert.assertNotNull(expiresAt);
    }

    @Test
    public void testGetIssuedAt() {
        Map<String, Object> map = new HashMap<>();
        map.put("userId", 123);
        map.put("username", "testUser");
        String token = JwtUtils.sign(map);
        Date issuedAt = JwtUtils.getIssuedAt(token);
        Assert.assertNotNull(issuedAt);
    }

    @Test
    public void testIsExpired() {
        Date expiration = new Date(System.currentTimeMillis() - 1000);
        Assert.assertTrue(JwtUtils.isExpired(expiration));
    }

    @Test
    public void testIsRenew() {
        Date expiration = new Date(System.currentTimeMillis() + JwtUtilsTest.BUFFER_TIME - 1000);
        Assert.assertTrue(JwtUtils.isRenew(expiration));
    }

    @Test
    public void testGetHeaderByBase64() {
        Map<String, Object> map = new HashMap<>();
        map.put("userId", 123);
        map.put("username", "testUser");
        String token = JwtUtils.sign(map);
        String header = JwtUtils.getHeaderByBase64(token);
        Assert.assertEquals("{\"typ\":\"JWT\",\"alg\":\"HS256\"}", header);
    }

}
