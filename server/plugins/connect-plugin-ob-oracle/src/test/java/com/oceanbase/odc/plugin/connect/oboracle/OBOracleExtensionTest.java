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
package com.oceanbase.odc.plugin.connect.oboracle;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.CollectionUtils;

import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.core.datasource.ConnectionInitializer;
import com.oceanbase.odc.core.session.ConnectionSessionUtil;
import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.core.shared.constant.ErrorCodes;
import com.oceanbase.odc.core.shared.constant.OdcConstants;
import com.oceanbase.odc.core.shared.model.SqlExecDetail;
import com.oceanbase.odc.core.shared.model.SqlExplain;
import com.oceanbase.odc.plugin.connect.api.ConnectionExtensionPoint;
import com.oceanbase.odc.plugin.connect.api.InformationExtensionPoint;
import com.oceanbase.odc.plugin.connect.api.SessionExtensionPoint;
import com.oceanbase.odc.plugin.connect.api.SqlDiagnoseExtensionPoint;
import com.oceanbase.odc.plugin.connect.api.TestResult;
import com.oceanbase.odc.plugin.connect.api.TraceExtensionPoint;
import com.oceanbase.odc.test.database.TestDBConfiguration;
import com.oceanbase.odc.test.database.TestDBConfigurations;

import lombok.extern.slf4j.Slf4j;

/**
 * @author yaobin
 * @date 2023-04-15
 * @since 4.2.0
 */
@Slf4j
public class OBOracleExtensionTest extends BaseExtensionPointTest {

    private static ConnectionExtensionPoint connectionExtensionPoint;
    private static SessionExtensionPoint sessionExtensionPoint;
    private static InformationExtensionPoint informationExtensionPoint;
    private static TraceExtensionPoint traceExtensionPoint;
    private static SqlDiagnoseExtensionPoint sqlDiagnoseExtensionPoint;
    private static TestDBConfiguration configuration =
            TestDBConfigurations.getInstance().getTestOBOracleConfiguration();
    private static JdbcTemplate jdbcTemplate = new JdbcTemplate(configuration.getDataSource());
    private static final String BASE_PATH = "src/test/resources/diagnose/";

    @BeforeClass
    public static void init() {
        connectionExtensionPoint = getInstance(OBOracleConnectionExtension.class);
        sessionExtensionPoint = getInstance(OBOracleSessionExtension.class);
        informationExtensionPoint = getInstance(OBOracleInformationExtension.class);
        traceExtensionPoint = getInstance(OBOracleTraceExtension.class);
        sqlDiagnoseExtensionPoint = getInstance(OBOracleDiagnoseExtension.class);
        jdbcTemplate.execute(TestDBConfigurations.loadAsString(BASE_PATH + "tableDDL.sql"));
    }

    @AfterClass
    public static void clear() {
        jdbcTemplate.execute(TestDBConfigurations.loadAsString(BASE_PATH + "drop.sql"));
    }

    @Test
    public void test_ob_oracle_url_is_valid() {
        String url = connectionExtensionPoint.generateJdbcUrl(configuration.getHost(), configuration.getPort(),
                configuration.getDefaultDBName(), null);
        TestResult result = connectionExtensionPoint.test(url, getUsername(configuration),
                configuration.getPassword(), 30);
        Assert.assertTrue(result.isActive());
        Assert.assertNull(result.getErrorCode());
    }

    @Test
    public void test_ob_oracle_connect_invalid_password() {
        String url = connectionExtensionPoint.generateJdbcUrl(configuration.getHost(), configuration.getPort(),
                configuration.getDefaultDBName(), null);
        TestResult result = connectionExtensionPoint.test(url, getUsername(configuration),
                UUID.randomUUID().toString(), 30);
        Assert.assertFalse(result.isActive());
        Assert.assertEquals(ErrorCodes.ObAccessDenied, result.getErrorCode());
    }

    @Test
    @Ignore("TODO: fix this test")
    public void test_ob_oracle_connect_invalid_port() {
        String url = connectionExtensionPoint.generateJdbcUrl(configuration.getHost(), configuration.getPort() + 100,
                configuration.getDefaultDBName(), null);
        TestResult result = connectionExtensionPoint.test(url, getUsername(configuration),
                configuration.getPassword(), 30);

        Assert.assertFalse(result.isActive());
        Assert.assertEquals(ErrorCodes.ConnectionUnknownPort, result.getErrorCode());
    }

    @Test
    public void test_ob_oracle_connect_invalid_host() {
        String url = connectionExtensionPoint.generateJdbcUrl(UUID.randomUUID().toString(),
                configuration.getPort(), configuration.getDefaultDBName(), null);
        TestResult result = connectionExtensionPoint.test(url, getUsername(configuration),
                configuration.getPassword(), 30);

        Assert.assertFalse(result.isActive());
        Assert.assertEquals(ErrorCodes.ConnectionUnknownHost, result.getErrorCode());
    }

    @Test
    public void test_ob_oracle_connect_driver() {
        Assert.assertEquals(
                connectionExtensionPoint.getDriverClassName(), OdcConstants.DEFAULT_DRIVER_CLASS_NAME);
    }

    @Test
    public void test_ob_oracle_connect_get_initializers() throws SQLException {
        List<ConnectionInitializer> connectionInitializers =
                connectionExtensionPoint.getConnectionInitializers();
        Assert.assertFalse(CollectionUtils.isEmpty(connectionInitializers));
        try (Connection connection = getConnection()) {
            connectionInitializers.forEach(initializer -> {
                try {
                    initializer.init(connection);
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            });
        }
    }

    @Test
    public void test_ob_oracle_session_get_connect_id() throws SQLException {

        try (Connection connection = getConnection()) {
            String connectId = sessionExtensionPoint.getConnectionId(connection);
            Assert.assertNotNull("connectId is null", connectId);
        }

    }

    @Test
    public void test_ob_oracle_session_switch_schema() {
        String targetSchema = "SYS";
        try (Connection connection = getConnection()) {
            String originSchema = sessionExtensionPoint.getCurrentSchema(connection);
            sessionExtensionPoint.switchSchema(connection, targetSchema);
            String changedSchema = sessionExtensionPoint.getCurrentSchema(connection);
            Assert.assertEquals(targetSchema, changedSchema);
            sessionExtensionPoint.switchSchema(connection, originSchema);
        } catch (SQLException e) {
            Assert.assertNull("SwitchSchema occur exception " + e.getMessage(), e);
        }
    }

    @Test
    public void test_ob_oracle_session_kill_query() throws SQLException {
        try (Connection connection = getConnection();
                Connection targetConnection = getConnection()) {
            String connectId = sessionExtensionPoint.getConnectionId(targetConnection);
            sessionExtensionPoint.killQuery(connection, connectId);
            Assert.assertTrue(targetConnection.isValid(3));
            Assert.assertFalse(targetConnection.isClosed());
        }
    }

    @Test(expected = Exception.class)
    public void test_ob_oracle_session_kill_query_invalid_id() throws SQLException {
        try (Connection connection = getConnection()) {
            sessionExtensionPoint.killQuery(connection, "-1");
        }
    }

    @Test
    public void test_ob_oracle_information_get_db_version() throws SQLException {
        try (Connection connection = getConnection()) {
            String version = informationExtensionPoint.getDBVersion(connection);
            Assert.assertNotNull("Version is null", version);
        }
    }

    @Test
    public void test_ob_oracle_get_explain_1_callSucceed() throws SQLException {
        try (Connection connection = getConnection()) {
            String sql = "select * from t_test_explain;";
            SqlExplain explain = sqlDiagnoseExtensionPoint.getExplain(connection.createStatement(), sql);
            Assert.assertNotNull(explain);
            Assert.assertNotNull(explain.getExpTree());
            Assert.assertNotNull(explain.getOutline());
            Assert.assertNotNull(explain.getOriginalText());
        }
    }

    @Test
    public void test_ob_oracle_get_explain_2_callSucceed() throws SQLException {
        try (Connection connection = getConnection()) {
            String sql = "SELECT A.CLAIM_CNTR_ID, A.SEQ_NO,\n"
                    + "A.M_CNTR_NO, A.CNTR_NO, A.SYS_NO_ORIGINAL, A.CLAIM_CNTR_PROC_STAT, A.MGR_BRANCH_NO, A.POL_CODE,\n"
                    + "B.CPNST_TYPE_CODE, B.SUB_POL_CODE, A.IPSN_NO, B.COVERAGE_NO, B.CPNST_CAL_CODE, B.GIFT_FLAG,\n"
                    + "B.CAL_CPNST_PSN_AMNT, B.CAL_CPNST_AMNT, B.CFM_CPNST_AMNT, B.CAL_CPNST_SI_AMNT,\n"
                    + "(SELECT NVL(C.SUB_DICT_NAME, '未定义') FROM D_PARAM_DICT_LST C WHERE C.DICT_CODE = 'POL_CODE_LIST'\n"
                    + "AND C.SUB_DICT_CODE = A.POL_CODE ) SUB_POL_NAME,\n"
                    + "(CASE WHEN SUBSTR(B.CPNST_CAL_CODE,1,2) ='M5' THEN\n"
                    + "(SELECT '涉及收据数' || COUNT(DISTINCT X1.RCPT_ID) || '申请总额' || SUM(X1.APPL_AMNT)\n"
                    + "FROM CLAIM_CAL_EXPEN X1 WHERE X1.CLAIM_CAL_ID = B.CLAIM_CAL_ID)\n"
                    + "WHEN SUBSTR(B.CPNST_CAL_CODE,1,2) ='M3' THEN\n"
                    + "(SELECT '申请天数' || SUM(X2.APPL_SUBSIDY_DAYS) || '实际赔付天数' || SUM(X2.SUBSIDY_DAYS)\n"
                    + "FROM CLAIM_CAL_SUBSIDY X2 WHERE X2.CLAIM_CAL_ID = B.CLAIM_CAL_ID) END ) ABOUT_NOTE\n"
                    + "FROM CLAIM_CNTR A LEFT JOIN CLAIM_CAL B ON A.CLAIM_CNTR_ID = B.CLAIM_CNTR_ID\n"
                    + "WHERE A.CLAIM_NO = '20191100009000000092' ORDER BY A.SEQ_NO;";
            SqlExplain explain = sqlDiagnoseExtensionPoint.getExplain(connection.createStatement(), sql);
            Assert.assertNotNull(explain);
            Assert.assertNotNull(explain.getExpTree());
            Assert.assertNotNull(explain.getOutline());
            Assert.assertNotNull(explain.getOriginalText());
        }
    }

    @Test
    public void test_ob_oracle_getExecutionDetailBySql_callSucceed() throws SQLException {
        try (Connection connection = getConnection()) {
            String sql = "select * from t_test_exec_detail";
            SqlExecDetail detail = sqlDiagnoseExtensionPoint.getExecutionDetailBySql(connection, sql);
            Assert.assertNotNull(detail);
            Assert.assertNotNull(detail.getTraceId());
        }
    }

    @Test
    public void test_ob_oracle_getPhysicalPlanBySql_callSucceed() throws SQLException {
        try (Connection connection = getConnection()) {
            String sql = "select t1.* from t_test_get_explain1 t1 where t1.c1 in (select c1 from t_test_get_explain2)";
            SqlExplain explain = sqlDiagnoseExtensionPoint.getPhysicalPlanBySql(connection, sql);
            Assert.assertNotNull(explain);
            Assert.assertNotNull(explain.getExpTree());
            Assert.assertNotNull(explain.getOutline());
        }
    }

    private String getUsername(TestDBConfiguration configuration) {
        String username = ConnectionSessionUtil.getUserOrSchemaString(
                configuration.getUsername(), DialectType.OB_ORACLE);
        if (StringUtils.isNotBlank(configuration.getTenant())) {
            username = username + "@" + configuration.getTenant();
        }
        if (StringUtils.isNotBlank(configuration.getCluster())) {
            username = username + "#" + configuration.getCluster();
        }
        return username;
    }
}
