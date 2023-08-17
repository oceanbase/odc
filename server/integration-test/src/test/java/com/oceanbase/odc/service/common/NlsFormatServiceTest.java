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
package com.oceanbase.odc.service.common;

import java.sql.Timestamp;
import java.util.Date;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.oceanbase.odc.ServiceTestEnv;
import com.oceanbase.odc.TestConnectionUtil;
import com.oceanbase.odc.core.session.ConnectionSession;
import com.oceanbase.odc.core.session.ConnectionSessionUtil;
import com.oceanbase.odc.core.shared.constant.ConnectType;
import com.oceanbase.odc.core.sql.util.OracleDateFormat;
import com.oceanbase.odc.core.sql.util.OracleTimestampFormat;
import com.oceanbase.odc.service.common.model.NlsFormatReq;

/**
 * Test cases for {@link NlsFormatService}
 *
 * @author yh263208
 * @date 2023-07-04 17:36
 * @since ODC_release-4.2.0
 */
public class NlsFormatServiceTest extends ServiceTestEnv {

    private static final String PATTERN = "DD-MM-RR";
    @Autowired
    private NlsFormatService nlsFormatService;

    @BeforeClass
    public static void setUp() {
        ConnectionSession connectionSession = TestConnectionUtil.getTestConnectionSession(ConnectType.OB_ORACLE);
        ConnectionSessionUtil.initConsoleSessionTimeZone(connectionSession, "Asia/Shanghai");
        ConnectionSessionUtil.setNlsDateFormat(connectionSession, PATTERN);
        ConnectionSessionUtil.setNlsTimestampFormat(connectionSession, PATTERN);
        ConnectionSessionUtil.setNlsTimestampTZFormat(connectionSession, PATTERN);
    }

    @Test
    public void format_date_formatSucceed() {
        ConnectionSession connectionSession = TestConnectionUtil.getTestConnectionSession(ConnectType.OB_ORACLE);
        NlsFormatReq req = new NlsFormatReq();
        req.setDataType("date");
        long timestamp = System.currentTimeMillis();
        req.setTimestamp(timestamp);
        String actual = this.nlsFormatService.format(connectionSession, req);
        OracleDateFormat format = new OracleDateFormat(PATTERN);
        Assert.assertEquals(format.format(new Date(timestamp)), actual);
    }

    @Test
    public void format_timestamp_formatSucceed() {
        ConnectionSession connectionSession = TestConnectionUtil.getTestConnectionSession(ConnectType.OB_ORACLE);
        NlsFormatReq req = new NlsFormatReq();
        req.setDataType("timestamp");
        long timestamp = System.currentTimeMillis();
        req.setTimestamp(timestamp);
        req.setNano(123);
        String actual = this.nlsFormatService.format(connectionSession, req);
        OracleTimestampFormat format = new OracleTimestampFormat(PATTERN);
        Assert.assertEquals(format.format(new Timestamp(timestamp)), actual);
    }

    @Test
    public void format_timestampTZ_formatSucceed() {
        ConnectionSession connectionSession = TestConnectionUtil.getTestConnectionSession(ConnectType.OB_ORACLE);
        NlsFormatReq req = new NlsFormatReq();
        req.setDataType("timestamp with time zone");
        long timestamp = System.currentTimeMillis();
        req.setTimestamp(timestamp);
        req.setNano(123);
        req.setTimeZoneId("Asia/Shanghai");
        String actual = this.nlsFormatService.format(connectionSession, req);
        OracleTimestampFormat format = new OracleTimestampFormat(PATTERN);
        Assert.assertEquals(format.format(new Timestamp(timestamp)), actual);
    }

    @Test
    public void format_timestampLTZ_formatSucceed() {
        ConnectionSession connectionSession = TestConnectionUtil.getTestConnectionSession(ConnectType.OB_ORACLE);
        NlsFormatReq req = new NlsFormatReq();
        req.setDataType("timestamp with local time zone");
        long timestamp = System.currentTimeMillis();
        req.setTimestamp(timestamp);
        req.setNano(123);
        String actual = this.nlsFormatService.format(connectionSession, req);
        OracleTimestampFormat format = new OracleTimestampFormat(PATTERN);
        Assert.assertEquals(format.format(new Timestamp(timestamp)), actual);
    }

}
