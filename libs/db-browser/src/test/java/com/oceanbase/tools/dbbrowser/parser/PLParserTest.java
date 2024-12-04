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
package com.oceanbase.tools.dbbrowser.parser;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.oceanbase.tools.dbbrowser.model.DBFunction;
import com.oceanbase.tools.dbbrowser.model.DBObjectType;
import com.oceanbase.tools.dbbrowser.model.DBProcedure;
import com.oceanbase.tools.dbbrowser.parser.constant.SqlType;
import com.oceanbase.tools.dbbrowser.parser.result.ParseMysqlPLResult;
import com.oceanbase.tools.dbbrowser.parser.result.ParseOraclePLResult;
import com.oceanbase.tools.sqlparser.SyntaxErrorException;

/**
 * @author wenniu.ly
 * @date 2021/8/26
 */
public class PLParserTest {

    @Test
    public void testParseMysqlFunction() {
        String pl = "create function f_test(p1 int, p2 int) returns int begin return;end;";
        ParseMysqlPLResult result = PLParser.parseObMysql(pl);
        Assert.assertEquals(0, result.getVaribaleList().size());
        Assert.assertEquals("int", result.getReturnType());
        Assert.assertEquals("f_test", result.getPlName());
        Assert.assertEquals("function", result.getPlType());
        Assert.assertEquals(2, result.getParamList().size());
    }

    @Test
    public void testParseMysqlProcedure() {
        String pl = "create procedure testProduce (out p1 int) \n" + "BEGIN \n" + "DECLARE Eno INT DEFAULT 10000;\n"
                + "DECLARE En VARCHAR(20);\n" + "DECLARE J VARCHAR(20);\n" + "DECLARE M INT DEFAULT 80000;\n"
                + "DECLARE H YEAR;\n" + "DECLARE Dno INT;\n" + "DECLARE i INT DEFAULT 1; \n" + "RETURN;\n" + "END;";
        ParseMysqlPLResult result = PLParser.parseObMysql(pl);
        Assert.assertEquals(7, result.getVaribaleList().size());
        Assert.assertEquals("testProduce", result.getPlName());
        Assert.assertEquals("procedure", result.getPlType());
        Assert.assertEquals(1, result.getParamList().size());
    }

    @Test
    public void testParseOracleProcedure() {
        String pl = "create procedure pl_test(p1 in int default 1, p2 in varchar2) \n" + "is \n" + "v1 number;\n"
                + "begin \n"
                + "return;\n" + "end;";
        ParseOraclePLResult result = PLParser.parseObOracle(pl);
        Assert.assertEquals(1, result.getVaribaleList().size());
        Assert.assertEquals("pl_test", result.getPlName());
        Assert.assertEquals("procedure", result.getPlType());
        List<DBProcedure> procedures = result.getProcedureList();
        Assert.assertEquals(1, procedures.size());
        Assert.assertEquals("1", procedures.get(0).getParams().get(0).getDefaultValue());
    }

    @Test
    public void testParseOracleFunction() {
        String pl =
                "CREATE OR REPLACE FUNCTION F_TEST(v_input  number default 100) RETURN NUMBER IS v1 number; v2 varchar2(100); "
                        + "type cur_emp is ref cursor; BEGIN RETURN;END;";

        ParseOraclePLResult result = PLParser.parseObOracle(pl);
        Assert.assertEquals(2, result.getVaribaleList().size());
        Assert.assertEquals("NUMBER", result.getReturnType());
        Assert.assertEquals("F_TEST", result.getPlName());
        Assert.assertEquals("FUNCTION", result.getPlType());
        Assert.assertEquals(1, result.getTypeList().size());
        List<DBFunction> functions = result.getFunctionList();
        Assert.assertEquals(1, functions.size());
        Assert.assertEquals("100", functions.get(0).getParams().get(0).getDefaultValue());
    }

    @Test
    public void testParseOraclePackageHead() {
        String pl = "create or replace package t_package  is v1 number; \n" + "type cur_emp is ref cursor;\n"
                + "procedure append_proc(p1 in out varchar2, p2 number);\n"
                + "function append_fun(p2 out varchar2) return varchar2;\n" + "end;";

        ParseOraclePLResult result = PLParser.parseObOracle(pl);
        Assert.assertEquals(1, result.getVaribaleList().size());
        Assert.assertEquals("t_package", result.getPlName());
        Assert.assertEquals("package", result.getPlType());
        Assert.assertEquals(1, result.getTypeList().size());
        Assert.assertEquals(1, result.getProcedureList().size());
        Assert.assertEquals(1, result.getFunctionList().size());
    }

    @Test
    public void testParseOraclePackageBody() {
        String pl = "create or replace package body t_package  \n" + "is  \n" + "  v_t varchar2(30);  \n" + "\n"
                + "  function append_fun(p1 IN OUT NOCOPY varchar2) return varchar2 is  \n" + "  begin  \n"
                + "     return v_t;  \n" + "  end;  \n" + "\n"
                + "  procedure append_proc(p1 in out varchar2, p2 number) is  \n" + "  begin  \n"
                + "     return v_t;  \n"
                + "  end;   \n" + "end; ";
        ParseOraclePLResult result = PLParser.parseObOracle(pl);
        Assert.assertEquals(1, result.getVaribaleList().size());
        Assert.assertEquals("t_package", result.getPlName());
        Assert.assertEquals("PACKAGE BODY", result.getPlType());
        Assert.assertEquals(0, result.getTypeList().size());
        Assert.assertEquals(1, result.getProcedureList().size());
        Assert.assertEquals(1, result.getFunctionList().size());
    }


    @Test(expected = SyntaxErrorException.class)
    public void testParseBigPackage() throws Exception {
        String pl = "create or replace package body g_core is\n" + "\n"
                + "type ret_code_tbl_rt is table of varchar2(8);\n"
                + "type amount_tbl_rt is table of number(12, 2);\n" + "type bool_tbl_rt is table of boolean;\n" + "\n"
                + "--prepaid data count\n"
                + "type state_domain_0_rt is record (charged number(10), uncharged number(10), ongoing number(10));\n"
                + "-- postpaid data count\n"
                + "type state_domain_1_rt is record (id raw(16), charged number(10), uncharged number(10));\n"
                + "type state_domain_1_aa is table of state_domain_1_rt index by varchar2(8);\n"
                + "-- prepaid amount sum\n"
                + "type state_domain_2_rt is record (charged number(12,2), ongoing number(12,2));\n"
                + "-- postpaid amount sum\n" + "type state_domain_3_rt is record (charged number(12,2));\n" + "\n"
                + "-- service & rule data for START_TRANS process\n" + "type enter_context_rt is record (\n"
                + "\ts_id mv_charge_core.s_id%TYPE,\n" + "\tcustomer_id mv_charge_core.customer_id%TYPE,\n"
                + "\tp_id mv_charge_core.p_id%TYPE,\n" + "\tproduct_code mv_charge_core.product_code%TYPE,\n"
                + "\tstart_time mv_charge_core.start_time%TYPE,\n" + "\tfinish_time mv_charge_core.finish_time%TYPE,\n"
                + "\ts_state mv_charge_core.s_state%TYPE,\n" + "\trule_plan mv_charge_core.rule_plan%TYPE,\n"
                + "\texists_gift_balance mv_charge_core.exists_gift_balance%TYPE,\n"
                + "\tenable_local_gift mv_charge_core.enable_local_gift%TYPE,\n" + "\ta_id mv_charge_core.a_id%TYPE,\n"
                + "\ta_state mv_charge_core.a_state%TYPE,\n" + "\tdata_quanlity mv_charge_core.data_quanlity%TYPE,\n"
                + "\ta_type mv_charge_core.a_type%TYPE,\n" + "\tr_id mv_charge_core.r_id%TYPE,\n"
                + "\tpriority mv_charge_core.priority%TYPE,\n" + "\tunit mv_charge_core.unit%TYPE,\n"
                + "\tsource mv_charge_core.source%TYPE,\n" + "\tenter_policy mv_charge_core.enter_policy%TYPE,\n"
                + "\tenter_conf mv_charge_core.enter_conf%TYPE,\n" + "\tdomains mv_charge_core.domains%TYPE,\n"
                + "\top_id mv_charge_core.op_id%TYPE,\n" + "\toutput_plan mv_charge_core.output_plan%TYPE\n" + ");\n"
                + "type enter_rule_context_rt is record (\n" + "\tr_id g_charge_service_rule.recid%TYPE,\n"
                + "\tpriority g_charge_service_rule.priority%TYPE,\n"
                + "\tunit g_charge_service_rule.balance_unit%TYPE,\n"
                + "\tsource g_charge_service_rule.balance_source%TYPE,\n"
                + "\tenter_policy g_charge_service_rule.enter_policy%TYPE,\n"
                + "\tenter_conf g_charge_service_rule.enter_conf%TYPE,\n"
                + "\tdomains g_charge_service_rule.domains%TYPE\n" + ");\n" + "\n"
                + "-- service & rule data for FINISH_TRANS\n" + "type exit_context_rt is record (\n"
                + "\ts_id mv_charge_core.s_id%TYPE,\n" + "\tcustomer_id mv_charge_core.customer_id%TYPE,\n"
                + "\tenable_local_gift mv_charge_core.enable_local_gift%TYPE,\n" + "\tr_id mv_charge_core.r_id%TYPE,\n"
                + "\tpriority mv_charge_core.priority%TYPE,\n" + "\tunit mv_charge_core.unit%TYPE,\n"
                + "\tsource mv_charge_core.source%TYPE,\n" + "\texit_policy mv_charge_core.exit_policy%TYPE,\n"
                + "\texit_conf mv_charge_core.exit_conf%TYPE,\n" + "\texit_ref mv_charge_core.exit_ref%TYPE,\n"
                + "\tdomains mv_charge_core.domains%TYPE,\n" + "\tcc_id mv_charge_core.cc_id%TYPE,\n"
                + "\tcharge_condition mv_charge_core.charge_condition%TYPE\n" + ");\n"
                + "type exit_rule_context_rt is record (\n" + "\tr_id g_charge_service_rule.recid%TYPE,\n"
                + "\tpriority g_charge_service_rule.priority%TYPE,\n"
                + "\tunit g_charge_service_rule.balance_unit%TYPE,\n"
                + "\tsource g_charge_service_rule.balance_source%TYPE,\n"
                + "\texit_policy mv_charge_core.exit_policy%TYPE,\n" + "\texit_conf mv_charge_core.exit_conf%TYPE,\n"
                + "\texit_ref mv_charge_core.exit_ref%TYPE,\n" + "\tdomains mv_charge_core.domains%TYPE\n" + ");\n"
                + "type exit_rule_context_tbl_rt is table of exit_rule_context_rt;\n"
                + "type rc_exit_rule_context_map_aat is table of exit_rule_context_rt index by varchar2(8);\n" + "\n"
                + "-- transaction & entry data for FINISH_TRANS\n" + "type trans_ongoing_rt is record (\n"
                + "\trecid g_transaction_ongoing.recid%TYPE,\n" + "\tseat_id g_transaction_ongoing.seat_id%TYPE,\n"
                + "\tservice_id g_transaction_ongoing.service_id%TYPE,\n"
                + "\trequest_time g_transaction_ongoing.request_time%TYPE,\n"
                + "\tdata_count g_transaction_ongoing.data_count%TYPE,\n"
                + "\tstart_time g_transaction_ongoing.start_time%TYPE,\n"
                + "\ttrans_state g_transaction_ongoing.trans_state%TYPE,\n"
                + "\thit_rule g_transaction_ongoing.hit_rule%TYPE,\n"
                + "\tbalance_unit g_transaction_ongoing.balance_unit%TYPE,\n"
                + "\tbalance_source g_transaction_ongoing.balance_source%TYPE,\n"
                + "\tenter_amount_sum g_transaction_ongoing.enter_amount_sum%TYPE\n" + ");\n"
                + "type trans_ongoing_entry_rt is record (\n" + "\trecid g_transaction_ongoing_entry.recid%TYPE,\n"
                + "\ttrans_id g_transaction_ongoing_entry.trans_id%TYPE,\n"
                + "\tseq g_transaction_ongoing_entry.seq%TYPE,\n"
                + "\tenter_amount g_transaction_ongoing_entry.enter_amount%TYPE\n" + ");\n"
                + "type trans_ongoing_entry_tbl_rt is table of trans_ongoing_entry_rt;\n" + "\n"
                + "-- transaction & entry data for MOVE_TRANS\n" + "type trans_moving_rt is record (\n"
                + "\trecid g_transaction_ongoing.recid%TYPE,\n" + "\tseat_id g_transaction_ongoing.seat_id%TYPE,\n"
                + "\tservice_id g_transaction_ongoing.service_id%TYPE,\n"
                + "\trequest_time g_transaction_ongoing.request_time%TYPE,\n"
                + "\tdata_count g_transaction_ongoing.data_count%TYPE,\n"
                + "\tstart_time g_transaction_ongoing.start_time%TYPE,\n"
                + "\tfinish_time g_transaction_ongoing.finish_time%TYPE,\n"
                + "\ttrans_state g_transaction_ongoing.trans_state%TYPE,\n"
                + "\thit_rule g_transaction_ongoing.hit_rule%TYPE,\n"
                + "\tbalance_unit g_transaction_ongoing.balance_unit%TYPE,\n"
                + "\tbalance_source g_transaction_ongoing.balance_source%TYPE,\n"
                + "\tenter_amount_sum g_transaction_ongoing.enter_amount_sum%TYPE,\n"
                + "\texit_amount_sum g_transaction_ongoing.exit_amount_sum%TYPE,\n"
                + "\tcharge_amount_sum g_transaction_ongoing.charge_amount_sum%TYPE,\n"
                + "\tcharge_data_count g_transaction_ongoing.charge_data_count%TYPE,\n"
                + "\tcall_type g_transaction_ongoing.call_type%TYPE,\n"
                + "\tcall_ip4_addr g_transaction_ongoing.call_ip4_addr%TYPE\n" + ");\n"
                + "type trans_moving_tbl_rt is table of trans_moving_rt;\n" + "type trans_entry_moving_rt is record (\n"
                + "\trecid g_transaction_ongoing_entry.recid%TYPE,\n"
                + "\ttrans_id g_transaction_ongoing_entry.trans_id%TYPE,\n"
                + "\tseq g_transaction_ongoing_entry.seq%TYPE,\n"
                + "\tenter_amount g_transaction_ongoing_entry.enter_amount%TYPE,\n"
                + "\texit_amount g_transaction_ongoing_entry.exit_amount%TYPE,\n"
                + "\tcharge_amount g_transaction_ongoing_entry.charge_amount%TYPE,\n"
                + "\tret_code g_transaction_ongoing_entry.ret_code%TYPE,\n"
                + "\tcharge_condition g_transaction_ongoing_entry.charge_condition%TYPE\n" + ");\n"
                + "type trans_entry_moving_tbl_rt is table of trans_entry_moving_rt;\n"
                + "type retcode_map_aat is table of number index by varchar2(8);\n" + "\n"
                + "------------------------------ 30 limit\n" + "not_yet exception;\n"
                + "unknown_balance_source exception;\n" + "corrupted_global_balance exception;\n"
                + "corrupted_local_balance exception;\n" + "unknown_ruleplan exception;\n"
                + "unknown_valuepolicy exception;\n" + "corrupted_fixvaluepolicy exception;\n"
                + "corrupted_ldrvaluepolicy exception;\n" + "missing_rule_domain_0 exception;\n"
                + "missing_rule_domain_1 exception;\n" + "------------------------------ 30 limit\n"
                + "prany_missing_enter_policy exception;\n" + "unknown_ldrvaluepolicy_param exception;\n"
                + "missing_charge_service exception;\n" + "prany_missing_hit_rule exception;\n"
                + "missing_charge_condition exception;\n" + "unexpected_ongoing_entry exception;\n"
                + "poany_missing_exit_policy exception;\n" + "poany_evaluate_failed exception;\n"
                + "corrupted_mapvaluepolicy exception;\n" + "unknown_mapvaluepolicy_param exception;\n"
                + "------------------------------ 30 limit\n" + "missing_rule_domain_2 exception;\n"
                + "missing_rule_domain_3 exception;\n" + "missing_global_balance exception;\n"
                + "missing_local_balance exception;\n" + "insufficient_test_balance exception;\n"
                + "rc_missing_exit_ref exception;\n" + "rc_duplicate_exit_ref exception;\n" + "\n"
                + "function parse_ldr_param (\n" + "\tconf varchar2\n" + ") return varchar2\n" + "is\n"
                + "\ti_dlr pls_integer;\n" + "begin\n" + "\ti_dlr := instr(conf, '$');\n" + "\tif i_dlr = 0 then\n"
                + "\t\traise corrupted_ldrvaluepolicy;\n" + "\tend if;\n" + "\treturn substr(conf, 1, i_dlr - 1);\n"
                + "end;\n" + "\n" + "procedure calc_amount_enter_all_fix (\n" + "\tconf in varchar2,\n"
                + "\tdata_count in pls_integer,\n" + "\ta_tbl out amount_tbl_rt,\n" + "\ta_sum out number\n" + ") is\n"
                + "\ti pls_integer;\n" + "\tunit_value number(12,2);\n" + "begin\n" + "\tbegin\n"
                + "\t\tselect cast(conf as number(12,2)) into unit_value from dual;\n" + "\texception\n"
                + "\t\twhen others then raise corrupted_fixvaluepolicy;\n" + "\tend;\n"
                + "\ta_tbl := amount_tbl_rt();\n"
                + "\ta_tbl.extend(data_count);\n" + "\ta_sum := 0;\n" + "\ti := 1;\n" + "\twhile i <= data_count loop\n"
                + "\t\ta_tbl(i) := unit_value;\n" + "\t\ti := i + 1;\n" + "\tend loop;\n"
                + "\ta_sum := unit_value * data_count;\n" + "end;\n" + "\n" + "-- conf: \n"
                + "-- data_count: total data count to calculate\n" + "-- base: value to determine step\n"
                + "-- offset: offset in conf of current step\n" + "-- filter: filter by ret_code_tbl \n"
                + "-- a_i: index of data count, should reach data_count eventually\n" + "-- a_tbl: amount \n"
                + "-- a_sum:\n" + "procedure calc_amount_all_ldr_dc (\n" + "\tconf in varchar2,\n"
                + "\tdata_count in pls_integer,\n" + "\tbase in out pls_integer,\n" + "\toffset in out pls_integer,\n"
                + "\tfilter in bool_tbl_rt,\n" + "\ta_i in out pls_integer,\n" + "\ta_tbl in out amount_tbl_rt,\n"
                + "\ta_sum in out number\n" + ") is\n" + "\ti_cln pls_integer;\n" + "\ti_scln pls_integer;\n"
                + "\tstep_to pls_integer;\n" + "\tunit_value number;\n" + "begin\n" + "\tloop\n"
                + "\t\ti_cln := instr(conf, ':', offset);\n" + "\t\tbegin\n"
                + "\t\t\tselect cast(substr(conf, offset, i_cln - offset) as number) into step_to from dual;\n"
                + "\t\texception\n" + "\t\t\twhen others then raise corrupted_ldrvaluepolicy;\n" + "\t\tend;\n"
                + "\t\tif step_to = -1 then\n" + "\t\t\tbegin\n"
                + "\t\t\t\tselect cast(substr(conf, i_cln + 1) as number) into unit_value from dual;\n"
                + "\t\t\texception\n" + "\t\t\t\twhen others then raise corrupted_ldrvaluepolicy;\n" + "\t\t\tend;\n"
                + "\t\telse\n" + "\t\t\ti_scln := instr(conf, ';', i_cln + 1);\n" + "\t\t\tbegin\n"
                + "\t\t\t\tselect cast(substr(conf, i_cln + 1, i_scln - i_cln -1) as number) into unit_value from dual;\n"
                + "\t\t\texception\n" + "\t\t\t\twhen others then raise corrupted_ldrvaluepolicy;\n" + "\t\t\tend;\n"
                + "\t\tend if;\n" + "\t\t\n" + "\t\tif step_to = -1 then\n" + "\t\t\twhile a_i <= data_count loop\n"
                + "\t\t\t\tif a_tbl is null then \n" + "\t\t\t\t\ta_tbl := amount_tbl_rt();\n" + "\t\t\t\tend if;\n"
                + "\t\t\t\ta_tbl.extend(1);\n" + "\t\t\t\tif filter is null then\n"
                + "\t\t\t\t\ta_tbl(a_i) := unit_value;\n" + "\t\t\t\t\ta_sum := a_sum + unit_value;\n"
                + "\t\t\t\telsif filter(a_i) then\n" + "\t\t\t\t\ta_tbl(a_i) := unit_value;\n"
                + "\t\t\t\t\ta_sum := a_sum + unit_value;\n" + "\t\t\t\telse\n" + "\t\t\t\t\ta_tbl(a_i) := 0;\n"
                + "\t\t\t\tend if;\n" + "\t\t\t\ta_i := a_i + 1;\n" + "\t\t\tend loop;\n" + "\t\t\texit;\n"
                + "\t\telsif base + 1 < step_to then\n" + "\t\t\tloop\n" + "\t\t\t\tif a_tbl is null then\n"
                + "\t\t\t\t\ta_tbl := amount_tbl_rt();\n" + "\t\t\t\tend if;\n" + "\t\t\t\ta_tbl.extend(1);\n"
                + "\t\t\t\tif filter is null then\n" + "\t\t\t\t\ta_tbl(a_i) := unit_value;\n"
                + "\t\t\t\t\ta_sum := a_sum + unit_value;\n" + "\t\t\t\t\tbase := base + 1;\n"
                + "\t\t\t\telsif filter(a_i) then\n" + "\t\t\t\t\ta_tbl(a_i) := unit_value;\n"
                + "\t\t\t\t\ta_sum := a_sum + unit_value;\n" + "\t\t\t\t\tbase := base + 1;\n" + "\t\t\t\telse\n"
                + "\t\t\t\t\ta_tbl(a_i) := 0;\n" + "\t\t\t\tend if;\n" + "\t\t\t\tif a_i = data_count then\n"
                + "\t\t\t\t\treturn;\n" + "\t\t\t\telse\n" + "\t\t\t\t\ta_i := a_i + 1;\n"
                + "\t\t\t\t\tif base + 1 = step_to then\n" + "\t\t\t\t\t\texit;\n" + "\t\t\t\t\telse\n"
                + "\t\t\t\t\t\tcontinue;\n" + "\t\t\t\t\tend if;\n" + "\t\t\t\tend if;\n" + "\t\t\tend loop;\n"
                + "\t\tend if;\n" + "\t\toffset := i_scln + 1;\n" + "\tend loop;\n" + "end;\n" + "\n"
                + "procedure calc_amount_all_ldr_as (\n" + "\tconf in varchar2,\n" + "\tdata_count in pls_integer,\n"
                + "\tbase in out number,\n" + "\toffset in out pls_integer,\n" + "\tfilter in bool_tbl_rt,\n"
                + "\ta_i in out pls_integer,\n" + "\ta_tbl in out amount_tbl_rt,\n" + "\ta_sum in out number\n"
                + ") is\n"
                + "\ti_cln pls_integer;\n" + "\ti_scln pls_integer;\n" + "\tstep_to pls_integer;\n"
                + "\tunit_value number;\n" + "begin\n" + "\tloop\n" + "\t\ti_cln := instr(conf, ':', offset);\n"
                + "\t\tbegin\n"
                + "\t\t\tselect cast(substr(conf, offset, i_cln - offset) as number) into step_to from dual;\n"
                + "\t\texception\n" + "\t\t\twhen others then raise corrupted_ldrvaluepolicy;\n" + "\t\tend;\n"
                + "\t\tif step_to = -1 then\n" + "\t\t\tbegin\n"
                + "\t\t\t\tselect cast(substr(conf, i_cln + 1) as number) into unit_value from dual;\n"
                + "\t\t\texception\n" + "\t\t\t\twhen others then raise corrupted_ldrvaluepolicy;\n" + "\t\t\tend;\n"
                + "\t\telse\n" + "\t\t\ti_scln := instr(conf, ';', i_cln + 1);\n" + "\t\t\tbegin\n"
                + "\t\t\t\tselect cast(substr(conf, i_cln + 1, i_scln - i_cln -1) as number) into unit_value from dual;\n"
                + "\t\t\texception\n" + "\t\t\t\twhen others then raise corrupted_ldrvaluepolicy;\n" + "\t\t\tend;\n"
                + "\t\tend if;\n" + "\t\t\n" + "\t\tif step_to = -1 then\n" + "\t\t\twhile a_i <= data_count loop\n"
                + "\t\t\t\tif a_tbl is null then \n" + "\t\t\t\t\ta_tbl := amount_tbl_rt();\n" + "\t\t\t\tend if;\n"
                + "\t\t\t\ta_tbl.extend(1);\n" + "\t\t\t\tif filter is null then\n"
                + "\t\t\t\t\ta_tbl(a_i) := unit_value;\n" + "\t\t\t\t\ta_sum := a_sum + unit_value;\n"
                + "\t\t\t\telsif filter(a_i) then\n" + "\t\t\t\t\ta_tbl(a_i) := unit_value;\n"
                + "\t\t\t\t\ta_sum := a_sum + unit_value;\n" + "\t\t\t\telse\n" + "\t\t\t\t\ta_tbl(a_i) := 0;\n"
                + "\t\t\t\tend if;\n" + "\t\t\t\ta_i := a_i + 1;\n" + "\t\t\tend loop;\n" + "\t\t\texit;\n"
                + "\t\telsif base < step_to then\n" + "\t\t\tloop\n" + "\t\t\t\tif a_tbl is null then\n"
                + "\t\t\t\t\ta_tbl := amount_tbl_rt();\n" + "\t\t\t\tend if;\n" + "\t\t\t\ta_tbl.extend(1);\n"
                + "\t\t\t\tif filter is null then\n" + "\t\t\t\t\ta_tbl(a_i) := unit_value;\n"
                + "\t\t\t\t\ta_sum := a_sum + unit_value;\n" + "\t\t\t\t\tbase := base + unit_value;\n"
                + "\t\t\t\telsif filter(a_i) then\n" + "\t\t\t\t\ta_tbl(a_i) := unit_value;\n"
                + "\t\t\t\t\ta_sum := a_sum + unit_value;\n" + "\t\t\t\t\tbase := base + unit_value;\n"
                + "\t\t\t\telse\n"
                + "\t\t\t\t\ta_tbl(a_i) := 0;\n" + "\t\t\t\tend if;\n" + "\t\t\t\tif a_i = data_count then\n"
                + "\t\t\t\t\treturn;\n" + "\t\t\t\telse\n" + "\t\t\t\t\ta_i := a_i + 1;\n"
                + "\t\t\t\t\tif base >= step_to then\n" + "\t\t\t\t\t\texit;\n" + "\t\t\t\t\telse\n"
                + "\t\t\t\t\t\tcontinue;\n" + "\t\t\t\t\tend if;\n" + "\t\t\t\tend if;\n" + "\t\t\tend loop;\n"
                + "\t\tend if;\n" + "\t\toffset := i_scln + 1;\n" + "\tend loop;\n" + "end;\n" + "\n"
                + "-- all data use same rule to calculate amount\n" + "procedure calc_amount_enter_all (\n"
                + "\tpolicy in varchar2,\n" + "\tconf in varchar2,\n" + "\tdata_count in pls_integer,\n"
                + "\tstate_domain_0 in state_domain_0_rt,\n" + "\tstate_domain_2 in state_domain_2_rt,\n"
                + "\ta_tbl out amount_tbl_rt,\n" + "\ta_sum out number\n" + ") is\n" + "begin\n"
                + "\tif policy = 'FIX' then\n" + "\t\tcalc_amount_enter_all_fix(conf, data_count, a_tbl, a_sum);\n"
                + "\telsif policy = 'LDR_DC' then\n" + "\t\tdeclare\n" + "\t\t\tparam_name varchar2(8);\n"
                + "\t\t\tbase pls_integer;\n" + "\t\t\toffset pls_integer;\n" + "\t\t\ta_i pls_integer;\n"
                + "\t\tbegin\n"
                + "\t\t\tparam_name := parse_ldr_param(conf);\n" + "\t\t\toffset := length(param_name) + 2;\n"
                + "\t\t\ta_i := 1;\n" + "\t\t\ta_sum := 0;\n" + "\t\t\tif param_name = 'ODC' then\n"
                + "\t\t\t\tbase := state_domain_0.charged + state_domain_0.ongoing;\n"
                + "\t\t\t\tcalc_amount_all_ldr_dc(conf, data_count, base, offset, null, a_i, a_tbl, a_sum);\n"
                + "\t\t\telse\n" + "\t\t\t\traise unknown_ldrvaluepolicy_param;\n" + "\t\t\tend if;\n" + "\t\tend;\n"
                + "\telsif policy = 'LDR_AS' then\n" + "\t\tdeclare\n" + "\t\t\tparam_name varchar2(8);\n"
                + "\t\t\tbase pls_integer;\n" + "\t\t\toffset pls_integer;\n" + "\t\t\ta_i pls_integer;\n"
                + "\t\tbegin\n"
                + "\t\t\tparam_name := parse_ldr_param(conf);\n" + "\t\t\toffset := length(param_name) + 2;\n"
                + "\t\t\ta_i := 1;\n" + "\t\t\ta_sum := 0;\n" + "\t\t\tif param_name = 'OAS' then\n"
                + "\t\t\t\tbase := state_domain_2.charged + state_domain_2.ongoing;\n"
                + "\t\t\t\tcalc_amount_all_ldr_as(conf, data_count, base, offset, null, a_i, a_tbl, a_sum);\n"
                + "\t\t\telse\n" + "\t\t\t\traise unknown_ldrvaluepolicy_param;\n" + "\t\t\tend if;\n" + "\t\tend;\n"
                + "\telse\n" + "\t\traise unknown_valuepolicy;\n" + "\tend if;\n" + "end;\n" + "\n"
                + "procedure try_lock_balance_local (\n" + "\tp_service_id in raw,\n"
                + "\tp_balance_unit in varchar2,\n"
                + "\tp_balance_amount in number,\n" + "\teval_okay out boolean\n" + ") is\n" + "begin\n"
                + "\tupdate g_charge_service_balance t\n"
                + "\t\tset locked_amount = t.locked_amount + p_balance_amount\n"
                + "\t\twhere t.service_id = p_service_id and t.balance_unit = p_balance_unit\n"
                + "\t\t\tand t.limit_amount - t.locked_amount - t.consumed_amount - p_balance_amount >= 0;\n"
                + "\tif SQL%ROWCOUNT = 0 then \n" + "\t\teval_okay := FALSE;\n" + "\telsif SQL%ROWCOUNT = 1 then\n"
                + "\t\teval_okay := TRUE;\n" + "\telse\n" + "\t\traise corrupted_local_balance;\n" + "\tend if;\n"
                + "end;\n" + "\n" + "procedure try_lock_balance_global (\n" + "\tp_customer_id in raw,\n"
                + "\tp_balance_unit in varchar2,\n" + "\tp_balance_amount in number,\n" + "\tokay out boolean\n"
                + ") is\n" + "begin\n" + "\tupdate g_customer_balance t\n"
                + "\t\tset locked_amount = t.locked_amount + p_balance_amount, available_amount = t.available_amount - p_balance_amount\n"
                + "\t\twhere t.customer_id = p_customer_id\tand t.balance_unit = p_balance_unit\n"
                + "\t\t\tand t.available_amount - p_balance_amount >= 0;\n" + "\tif SQL%ROWCOUNT = 0 then \n"
                + "\t\tokay := FALSE;\n" + "\telsif SQL%ROWCOUNT = 1 then\n" + "\t\tokay := TRUE;\n" + "\telse\n"
                + "\t\traise corrupted_global_balance;\n" + "\tend if;\n" + "end;\n" + "\n"
                + "procedure evaluate_enter_rule_prany (\n" + "\tctx in enter_context_rt,\n"
                + "\tr in enter_rule_context_rt,\n" + "\tp_data_count in pls_integer,\n" + "\teval_okay out boolean,\n"
                + "\tusing_rule out pls_integer,\n" + "\tusing_source out varchar2,\n" + "\tusing_unit out varchar2,\n"
                + "\tamount_tbl out amount_tbl_rt,\n" + "\tamount_sum out number\n" + ") is\n"
                + "\tcursor sd0_c (p_rule_id raw) \n"
                + "\t\tis select charged_data_count, uncharged_data_count, ongoing_data_count\n"
                + "\t\tfrom g_charge_rule_state_0 where recid = p_rule_id for update;\n"
                + "\tcursor sd2_c (p_rule_id raw)\n" + "\t\tis select charged_amount_sum, ongoing_amount_sum \n"
                + "\t\tfrom g_charge_rule_state_2 where recid = p_rule_id for update;\n"
                + "\tstate_domain_0 state_domain_0_rt;\n" + "\tstate_domain_2 state_domain_2_rt;\n" + "begin\n"
                + "\tif r.enter_policy is null then\n" + "\t\traise prany_missing_enter_policy;\n" + "\tend if;\n"
                + "\t\n" + "\tif utl_raw.bit_and(r.domains, hextoraw('01000000')) = hextoraw('01000000') then\n"
                + "\t\topen sd0_c (r.r_id);\n" + "\t\tfetch sd0_c into state_domain_0;\n"
                + "\t\tif sd0_c%NOTFOUND then\n"
                + "\t\t\traise missing_rule_domain_0;\n" + "\t\tend if;\n" + "\tend if;\n" + "\n"
                + "\tif utl_raw.bit_and(r.domains, hextoraw('04000000')) = hextoraw('04000000') then\n"
                + "\t\topen sd2_c (r.r_id);\n" + "\t\tfetch sd2_c into state_domain_2;\n"
                + "\t\tif sd2_c%NOTFOUND then\n"
                + "\t\t\traise missing_rule_domain_2;\n" + "\t\tend if;\n" + "\tend if;\n" + "\t\n"
                + "\t-- to ensures that cursor will be closed correctly\n" + "\tbegin\n"
                + "\t\tcalc_amount_enter_all(r.enter_policy, r.enter_conf, p_data_count,\n"
                + "\t\t\tstate_domain_0, state_domain_2, amount_tbl, amount_sum);\n" + "\t\t\n"
                + "\t\tif r.source = 'LOCAL' then\n"
                + "\t\t\ttry_lock_balance_local(ctx.s_id, r.unit, amount_sum, eval_okay);\n"
                + "\t\telsif r.source = 'GLOBAL' then\n"
                + "\t\t\ttry_lock_balance_global(ctx.customer_id, r.unit, amount_sum, eval_okay);\n"
                + "\t\telsif r.source = 'NONE' then\n" + "\t\t\teval_okay := TRUE;\n" + "\t\telse\n"
                + "\t\t\traise unknown_balance_source;\n" + "\t\tend if;\n" + "\t\t\n" + "\t\tif eval_okay then\n"
                + "\t\t\tif utl_raw.bit_and(r.domains, hextoraw('01000000')) = hextoraw('01000000') then\n"
                + "\t\t\t\tupdate g_charge_rule_state_0 set ongoing_data_count = ongoing_data_count + p_data_count\n"
                + "\t\t\t\t\twhere current of sd0_c;\n" + "\t\t\t\tclose sd0_c;\n" + "\t\t\tend if;\n" + "\t\t\t\n"
                + "\t\t\tif utl_raw.bit_and(r.domains, hextoraw('04000000')) = hextoraw('04000000') then\n"
                + "\t\t\t\tupdate g_charge_rule_state_2 set ongoing_amount_sum = ongoing_amount_sum + amount_sum\n"
                + "\t\t\t\t\twhere current of sd2_c;\n" + "\t\t\t\tclose sd2_c;\n" + "\t\t\tend if;\n" + "\t\t\t\n"
                + "\t\t\tusing_rule := r.priority;\n" + "\t\t\tusing_unit := r.unit;\n"
                + "\t\t\tusing_source := r.source;\n" + "\t\telsif r.source = 'LOCAL' and r.unit = 'TEST' then\n"
                + "\t\t\traise insufficient_test_balance; -- XXX not quite reasonable through raising exception\n"
                + "\t\tend if;\n" + "\texception when others then\n" + "\t\tif sd0_c%ISOPEN then close sd0_c; end if;\n"
                + "\t\tif sd2_c%ISOPEN then close sd2_c; end if;\n" + "\t\traise;\n" + "\tend;\n" + "\t\n"
                + "\tif sd0_c%ISOPEN then close sd0_c; end if;\n" + "\tif sd2_c%ISOPEN then close sd2_c; end if;\n"
                + "end;\n" + "\n" + "-- PR_ANY require each rule contains a enter policy\n"
                + "procedure evaluate_enter_plan_prany (\n" + "\tctx in enter_context_rt,\n"
                + "\tp_data_count in pls_integer,\n" + "\teval_okay out boolean,\n" + "\tusing_rule out pls_integer,\n"
                + "\tusing_source out varchar2,\n" + "\tusing_unit out varchar2,\n"
                + "\tamount_tbl out amount_tbl_rt,\n"
                + "\tamount_sum out number\n" + ") is\n" + "\tcursor c is \n" + "\t\tselect\n"
                + "\t\t\tr.recid rule_id,\n" + "\t\t\tr.priority,\n" + "\t\t\tr.balance_unit,\n"
                + "\t\t\tr.balance_source,\n" + "\t\t\tr.enter_policy,\n" + "\t\t\tr.enter_conf,\n"
                + "\t\t\tr.domains\n"
                + "\t\tfrom \n" + "\t\t\tg_charge_service_rule r\n" + "\t\twhere\n"
                + "\t\t\tr.service_id = ctx.s_id and priority > 0\n" + "\t\torder by\n" + "\t\t\tr.priority;\n"
                + "\tr c%ROWTYPE;\n" + "\ti pls_integer;\n" + "begin\n" + "\teval_okay := FALSE;\n" + "\t\n"
                + "\t-- try local gift\n" + "\tif ctx.enable_local_gift = 1 then\n"
                + "\t\ttry_lock_balance_local(ctx.s_id, 'GIFT', p_data_count, eval_okay);\n"
                + "\t\tif eval_okay then \n"
                + "\t\t\tusing_rule := -99;\n" + "\t\t\tusing_source := 'LOCAL';\n" + "\t\t\tusing_unit := 'GIFT';\n"
                + "\t\t\tamount_tbl.extend(p_data_count);\n" + "\t\t\ti := 1;\n"
                + "\t\t\twhile i <= p_data_count loop\n"
                + "\t\t\t\tamount_tbl(i) := 1;\n" + "\t\t\tend loop;\n" + "\t\t\tamount_sum := p_data_count;\n"
                + "\t\t\treturn;\n" + "\t\tend if;\n" + "\tend if;\n" + "\t\n"
                + "\t-- fisrt rule fetch from materialized view\n" + "\tr.rule_id := ctx.r_id;\n"
                + "\tr.priority := ctx.priority;\n" + "\tr.balance_unit := ctx.unit;\n"
                + "\tr.balance_source := ctx.source;\n" + "\tr.enter_policy := ctx.enter_policy;\n"
                + "\tr.enter_conf := ctx.enter_conf;\n" + "\tr.domains := ctx.domains;\n" + "\t\n"
                + "\tevaluate_enter_rule_prany(ctx, r, p_data_count,\n"
                + "\t\teval_okay, using_rule, using_source, using_unit, amount_tbl, amount_sum);\n" + "\t\n"
                + "\tif eval_okay then\n" + "\t\treturn;\n" + "\tend if;\n" + "\t\n"
                + "\t-- using FOR/LOOP in case of exception\n" + "\tfor x in c loop\n" + "\t\tr.rule_id := x.rule_id;\n"
                + "\t\tr.priority := x.priority;\n" + "\t\tr.balance_unit := x.balance_unit;\n"
                + "\t\tr.balance_source := x.balance_source;\n" + "\t\tr.enter_policy := x.enter_policy;\n"
                + "\t\tr.enter_conf := x.enter_conf;\n" + "\t\tr.domains := x.domains;\n" + "\t\t\n"
                + "\t\tevaluate_enter_rule_prany(ctx, r, p_data_count,\n"
                + "\t\t\teval_okay, using_rule, using_source, using_unit, amount_tbl, amount_sum);\n" + "\t\t\n"
                + "\t\tif eval_okay then\n" + "\t\t\treturn;\n" + "\t\tend if;\n" + "\tend loop;\n" + "end;\n" + "\n"
                + "procedure evaluate_enter_plan_prorc (\n" + "\tctx in enter_context_rt,\n"
                + "\tp_data_count in pls_integer,\n" + "\teval_okay out boolean,\n" + "\tusing_rule out pls_integer,\n"
                + "\tusing_source out varchar2,\n" + "\tusing_unit out varchar2,\n"
                + "\tamount_tbl out amount_tbl_rt,\n"
                + "\tamount_sum out number\n" + ") is\n" + "begin\n" + "\teval_okay := FALSE;\n" + "\t\n"
                + "\tif ctx.enable_local_gift = 1 then\n"
                + "\t\ttry_lock_balance_local(ctx.s_id, 'GIFT', p_data_count, eval_okay);\n"
                + "\t\tif eval_okay then \n"
                + "\t\t\tusing_rule := -99;\n" + "\t\t\tusing_source := 'LOCAL';\n" + "\t\t\tusing_unit := 'GIFT';\n"
                + "\t\t\tamount_tbl.extend(p_data_count);\n"
                + "\t\t\tfor i in amount_tbl.first .. amount_tbl.last loop\n"
                + "\t\t\t\tamount_tbl(i) := 1;\n" + "\t\t\tend loop;\n" + "\t\t\tamount_sum := p_data_count;\n"
                + "\t\t\treturn;\n" + "\t\tend if;\n" + "\tend if;\n" + "\t\n" + "\tusing_rule := -1;\n"
                + "\tusing_source := ctx.source;\n" + "\tusing_unit := ctx.unit;\n" + "\t\n"
                + "\tcalc_amount_enter_all(ctx.enter_policy, ctx.enter_conf, p_data_count,\n"
                + "\t\tnull, null, amount_tbl, amount_sum);\n" + "\t\t\n" + "\tif ctx.source = 'LOCAL' then\n"
                + "\t\ttry_lock_balance_local(ctx.s_id, ctx.unit, amount_sum, eval_okay);\n"
                + "\telsif ctx.source = 'GLOBAL' then\n"
                + "\t\ttry_lock_balance_global(ctx.customer_id, ctx.unit, amount_sum, eval_okay);\n"
                + "\telsif ctx.source = 'NONE' then\n" + "\t\teval_okay := TRUE;\n" + "\telse\n"
                + "\t\traise unknown_balance_source;\n" + "\tend if;\n" + "end;\n" + "\n"
                + "procedure start_trans_standard (\n" + "\tctx in enter_context_rt,\n"
                + "\tp_request_time in timestamp,\n" + "\tp_transaction_id in raw,\n" + "\tp_seat_id in raw,\n"
                + "\tp_data_count in pls_integer,\n" + "\tp_call_type in varchar2,\n"
                + "\tp_call_ip4_addr in varchar2,\n"
                + "\tstate_code out pls_integer\n" + ") is\n" + "\tpragma autonomous_transaction;\n"
                + "\tstate_domain_0 state_domain_0_rt;\n" + "\tstate_domain_2 state_domain_2_rt;\n" + "begin\n"
                + "\tif ctx.rule_plan = 'PR_ANY' then\n" + "\t\tdeclare\n" + "\t\t\teval_okay boolean;\n"
                + "\t\t\tusing_rule pls_integer;\n" + "\t\t\tusing_source varchar2(8);\n"
                + "\t\t\tusing_unit varchar2(8);\n" + "\t\t\tamount_tbl amount_tbl_rt;\n"
                + "\t\t\tamount_sum number(12,2);\n" + "\t\tbegin\n"
                + "\t\t\tevaluate_enter_plan_prany(ctx, p_data_count, \n"
                + "\t\t\t\teval_okay, using_rule, using_source, using_unit, amount_tbl, amount_sum);\n"
                + "\t\t\tif eval_okay then\n"
                + "\t\t\t\tinsert into g_transaction_ongoing (recid, seat_id, service_id, request_time, start_time,\n"
                + "\t\t\t\t\t\ttrans_state, hit_rule, data_count, balance_source, balance_unit,\n"
                + "\t\t\t\t\t\tenter_amount_sum, call_type, call_ip4_addr)\n"
                + "\t\t\t\t\tvalues (p_transaction_id, p_seat_id, ctx.s_id, p_request_time, localtimestamp(3),\n"
                + "\t\t\t\t\t\t1, using_rule, p_data_count, using_source, using_unit,\n"
                + "\t\t\t\t\t\tamount_sum, p_call_type, p_call_ip4_addr);\n"
                + "\t\t\t\tfor i in amount_tbl.first .. amount_tbl.last loop\n"
                + "\t\t\t\t\tinsert into g_transaction_ongoing_entry (recid, trans_id, seq, enter_amount)\n"
                + "\t\t\t\t\t\tvalues (sys_guid(), p_transaction_id, i, amount_tbl(i));\n" + "\t\t\t\tend loop;\n"
                + "\t\t\t\tcommit; state_code := 0; return;\n" + "\t\t\telse\n"
                + "\t\t\t\trollback; state_code := 202; return;\n" + "\t\t\tend if;\n" + "\t\tend;\n"
                + "\telsif ctx.rule_plan = 'PO_ANY' then\n" + "\t\tdeclare\n" + "\t\t\ti pls_integer;\n" + "\t\tbegin\n"
                + "\t\t\tinsert into g_transaction_ongoing (recid, seat_id, service_id, request_time, start_time,\n"
                + "\t\t\t\t\ttrans_state, hit_rule, data_count, balance_source, balance_unit, enter_amount_sum, call_type, call_ip4_addr)\n"
                + "\t\t\t\tvalues (p_transaction_id, p_seat_id, ctx.s_id, p_request_time, localtimestamp(3),\n"
                + "\t\t\t\t\t2, -1, p_data_count, 'TBD', 'TBD', 0, p_call_type, p_call_ip4_addr);\n" + "\t\t\ti := 1;\n"
                + "\t\t\twhile i <= p_data_count loop\n"
                + "\t\t\t\tinsert into g_transaction_ongoing_entry (recid, trans_id, seq, enter_amount)\n"
                + "\t\t\t\t\tvalues (sys_guid(), p_transaction_id, i, 0);\n" + "\t\t\t\ti := i + 1;\n"
                + "\t\t\tend loop;\n" + "\t\tend;\n" + "\t\tcommit; state_code := 0; return;\n"
                + "\telsif ctx.rule_plan = 'PR_O_RC' then \n" + "\t\tdeclare\n" + "\t\t\teval_okay boolean;\n"
                + "\t\t\tusing_rule pls_integer;\n" + "\t\t\tusing_source varchar2(8);\n"
                + "\t\t\tusing_unit varchar2(8);\n" + "\t\t\tamount_tbl amount_tbl_rt;\n"
                + "\t\t\tamount_sum number(12,2);\n" + "\t\tbegin\n"
                + "\t\t\tevaluate_enter_plan_prorc(ctx, p_data_count, \n"
                + "\t\t\t\teval_okay, using_rule, using_source, using_unit, amount_tbl, amount_sum);\n"
                + "\t\t\tif eval_okay then\n"
                + "\t\t\t\tinsert into g_transaction_ongoing (recid, seat_id, service_id, request_time, start_time,\n"
                + "\t\t\t\t\t\ttrans_state, hit_rule, data_count, balance_source, balance_unit,\n"
                + "\t\t\t\t\t\tenter_amount_sum, call_type, call_ip4_addr)\n"
                + "\t\t\t\t\tvalues (p_transaction_id, p_seat_id, ctx.s_id, p_request_time, localtimestamp(3),\n"
                + "\t\t\t\t\t\t4, using_rule, p_data_count, using_source, using_unit,\n"
                + "\t\t\t\t\t\tamount_sum, p_call_type, p_call_ip4_addr);\n"
                + "\t\t\t\tfor i in amount_tbl.first .. amount_tbl.last loop\n"
                + "\t\t\t\t\tinsert into g_transaction_ongoing_entry (recid, trans_id, seq, enter_amount)\n"
                + "\t\t\t\t\t\tvalues (sys_guid(), p_transaction_id, i, amount_tbl(i));\n" + "\t\t\t\tend loop;\n"
                + "\t\t\t\tcommit; state_code := 0; return;\n" + "\t\t\telse\n"
                + "\t\t\t\trollback; state_code := 202; return;\n" + "\t\t\tend if;\n" + "\t\tend;\n"
                + "\telsif ctx.rule_plan = 'PO_O_RC' then \n" + "\t\tdeclare\n" + "\t\t\ti pls_integer;\n"
                + "\t\tbegin\n"
                + "\t\t\tinsert into g_transaction_ongoing (recid, seat_id, service_id, request_time, start_time,\n"
                + "\t\t\t\t\ttrans_state, hit_rule, data_count, balance_source, balance_unit, enter_amount_sum, call_type, call_ip4_addr)\n"
                + "\t\t\t\tvalues (p_transaction_id, p_seat_id, ctx.s_id, p_request_time, localtimestamp(3),\n"
                + "\t\t\t\t\t5, -1, p_data_count, 'TBD', 'TBD', 0, p_call_type, p_call_ip4_addr);\n" + "\t\t\ti := 1;\n"
                + "\t\t\twhile i <= p_data_count loop\n"
                + "\t\t\t\tinsert into g_transaction_ongoing_entry (recid, trans_id, seq, enter_amount)\n"
                + "\t\t\t\t\tvalues (sys_guid(), p_transaction_id, i, 0);\n" + "\t\t\t\ti := i + 1;\n"
                + "\t\t\tend loop;\n" + "\t\tend;\n" + "\telse\n" + "\t\traise unknown_ruleplan;\n" + "\tend if;\n"
                + "exception\n" + "\twhen unknown_ruleplan then rollback; state_code := 532;\n"
                + "\twhen unknown_valuepolicy then rollback; state_code := 533;\n"
                + "\twhen corrupted_fixvaluepolicy then rollback; state_code := 534;\n"
                + "\twhen corrupted_ldrvaluepolicy then rollback; state_code := 535;\n"
                + "\twhen unknown_balance_source then rollback; state_code := 536;\n"
                + "\twhen corrupted_global_balance then rollback; state_code := 537;\n"
                + "\twhen corrupted_local_balance then rollback; state_code := 538;\n"
                + "\twhen missing_rule_domain_0 then rollback; state_code := 539;\n"
                + "\twhen prany_missing_enter_policy then rollback; state_code := 540;\n"
                + "\twhen unknown_ldrvaluepolicy_param then rollback; state_code := 541;\n"
                + "\twhen missing_rule_domain_2 then rollback; state_code := 549;\n"
                + "\twhen unknown_mapvaluepolicy_param then rollback; state_code := 553;\n"
                + "\twhen corrupted_mapvaluepolicy then rollback; state_code := 554;\n"
                + "\twhen insufficient_test_balance then rollback; state_code := 206;\n"
                + "\twhen not_yet then rollback; state_code := 1542;\n"
                + "\twhen others then rollback; state_code:= 1543;\n" + "end;\n" + "\n"
                + "procedure start_trans_delegate (\n" + "\tctx in enter_context_rt,\n"
                + "\tp_request_time in timestamp,\n" + "\tp_transaction_id in raw,\n" + "\tp_seat_id in raw,\n"
                + "\tp_data_count in pls_integer,\n" + "\tp_call_type in varchar2,\n"
                + "\tp_call_ip4_addr in varchar2,\n"
                + "\tstate_code out pls_integer\n" + ") is\n" + "\tpragma autonomous_transaction;\n"
                + "\ti pls_integer;\n" + "begin\n"
                + "\tinsert into g_transaction_ongoing (recid, seat_id, service_id, request_time, start_time,\n"
                + "\t\t\ttrans_state, hit_rule, data_count, balance_source, balance_unit, enter_amount_sum, call_type, call_ip4_addr)\n"
                + "\t\tvalues (p_transaction_id, p_seat_id, ctx.s_id, p_request_time, localtimestamp(3),\n"
                + "\t\t\t3, -1, p_data_count, 'NONE', 'DELEGATE', p_data_count, p_call_type, p_call_ip4_addr);\n"
                + "\ti := 1;\n" + "\twhile i <= p_data_count loop\n"
                + "\t\tinsert into g_transaction_ongoing_entry (recid, trans_id, seq, enter_amount)\n"
                + "\t\t\tvalues (sys_guid(), p_transaction_id, i, 1);\n" + "\t\ti := i + 1;\n" + "\tend loop;\n"
                + "\tstate_code := 0;\n" + "\tcommit;\n" + "exception\n"
                + "\twhen others then rollback; state_code := 1543;\n" + "end;\n" + "\n"
                + "procedure start_trans_ts (\n"
                + "\tp_customer_id in raw,\n" + "\tp_seat_id in raw,\n" + "\tp_product_code in varchar2,\n"
                + "\tp_data_count in pls_integer,\n" + "\tp_mode in pls_integer,\n" + "\tp_call_type in varchar2,\n"
                + "\tp_call_ip4_addr in varchar2,\n" + "\tp_request_time timestamp,\n"
                + "\tstate_code out pls_integer,\n"
                + "\ttransaction_id out raw,\n" + "\toutput_plan out varchar2,\n" + "\tdata_quanlity out varchar2\n"
                + ") is\n" + "\tc_transaction_id raw(16);\n" + "\tcursor c is\n"
                + "\t\tselect s_id, customer_id, p_id, product_code, start_time,\n"
                + "\t\t\tfinish_time, s_state, rule_plan, exists_gift_balance, enable_local_gift,\n"
                + "\t\t\ta_id, a_state, data_quanlity, a_type, r_id, priority, unit, source,\n"
                + "\t\t\tenter_policy, enter_conf, domains, op_id, output_plan\n" + "\t\t\tfrom mv_charge_core \n"
                + "\t\t\twhere customer_id = p_customer_id and product_code = p_product_code\n"
                + "\t\t\t\tand p_request_time >= start_time and p_request_time < finish_time\n"
                + "\t\t\torder by finish_time;\n" + "\tctx enter_context_rt;\n" + "begin\n" + "\t\n"
                + "\tif p_customer_id is null or p_seat_id is null or p_product_code is null then\n"
                + "\t\tstate_code := 101;\n" + "\t\treturn;\n" + "\tend if;\n" + "\t\n"
                + "\tif p_data_count is null or p_data_count <= 0 then\n" + "\t\tstate_code := 101;\n" + "\t\treturn;\n"
                + "\tend if;\n" + "\t\n" + "\tif p_mode <> 1 and p_mode <> 2 then\n" + "\t\tstate_code := 207;\n"
                + "\t\treturn;\n" + "\tend if;\n" + "\t\n" + "\tstate_code := 201;\n" + "\t\n" + "\topen c;\n"
                + "\tbegin\n" + "\t\tloop\n" + "\t\t\tfetch c into ctx;\n" + "\t\t\tif c%NOTFOUND then\n"
                + "\t\t\t\trollback; close c; return;\n" + "\t\t\tend if;\n" + "\t\t\t\n"
                + "\t\t\tif ctx.a_state <> 1 or ctx.s_state <> 1 then\n" + "\t\t\t\tstate_code := 209;\n"
                + "\t\t\t\tcontinue;\n" + "\t\t\tend if;\n" + "\t\t\t\n" + "\t\t\tif ctx.op_id is null then\n"
                + "\t\t\t\tstate_code := 543;\n" + "\t\t\t\tcontinue;\n" + "\t\t\tend if;\n" + "\t\t\t\n"
                + "\t\t\tselect sys_guid() into transaction_id from dual;\n" + "\t\t\n" + "\t\t\tif p_mode = 1 then\n"
                + "\t\t\t\tdeclare\n" + "\t\t\t\t\tc_status pls_integer;\n" + "\t\t\t\tbegin\n"
                + "\t\t\t\t\tselect status into c_status from g_calltype_status s \n"
                + "\t\t\t\t\t\twhere s.authr_id = ctx.a_id and s.calltype = p_call_type;\n"
                + "\t\t\t\t\tif c_status = 1 and ctx.a_type = 7 or c_status = 2 and ctx.a_type = 1 then\n"
                + "\t\t\t\t\t\tnull;\n" + "\t\t\t\t\telse\n" + "\t\t\t\t\t\tstate_code := 553; continue;\n"
                + "\t\t\t\t\tend if;\n" + "\t\t\t\texception\n"
                + "\t\t\t\t\twhen no_data_found then state_code := 554; continue;\n" + "\t\t\t\tend;\n"
                + "\t\t\t\tstart_trans_standard(ctx, p_request_time, transaction_id, \n"
                + "\t\t\t\t\tp_seat_id, p_data_count, p_call_type, p_call_ip4_addr,\n" + "\t\t\t\t\tstate_code);\n"
                + "\t\t\telsif p_mode = 2 then\n"
                + "\t\t\t\tstart_trans_delegate(ctx, p_request_time, transaction_id, \n"
                + "\t\t\t\t\tp_seat_id, p_data_count, p_call_type, p_call_ip4_addr,\n" + "\t\t\t\t\tstate_code);\n"
                + "\t\t\tend if;\n" + "\t\t\t\n" + "\t\t\tif state_code = 0 then\n"
                + "\t\t\t\toutput_plan := ctx.output_plan;\n" + "\t\t\t\tdata_quanlity := ctx.data_quanlity;\n"
                + "\t\t\t\texit;\n" + "\t\t\tend if;\n" + "\t\tend loop;\n" + "\texception when others then\n"
                + "\t\tif c%ISOPEN then close c; raise; end if;\n" + "\tend;\n" + "\tclose c;\n" + "end;\n" + "\n"
                + "procedure start_trans (\n" + "\tp_customer_id in raw,\n" + "\tp_seat_id in raw,\n"
                + "\tp_product_code in varchar2,\n" + "\tp_data_count in pls_integer,\n" + "\tp_mode in pls_integer,\n"
                + "\tp_call_type in varchar2,\n" + "\tp_call_ip4_addr in varchar2,\n"
                + "\tstate_code out pls_integer,\n"
                + "\ttransaction_id out raw,\n" + "\toutput_plan out varchar2,\n" + "\tdata_quanlity out varchar2\n"
                + ") is\n" + "\tc_request_time timestamp(3);\n" + "\tc_transaction_id raw(16);\n" + "\tcursor c is\n"
                + "\t\tselect s_id, customer_id, p_id, product_code, start_time,\n"
                + "\t\t\tfinish_time, s_state, rule_plan, exists_gift_balance, enable_local_gift,\n"
                + "\t\t\ta_id, a_state, data_quanlity, a_type, r_id, priority, unit, source,\n"
                + "\t\t\tenter_policy, enter_conf, domains, op_id, output_plan\n" + "\t\t\tfrom mv_charge_core \n"
                + "\t\t\twhere customer_id = p_customer_id and product_code = p_product_code\n"
                + "\t\t\t\tand localtimestamp(3) >= start_time and localtimestamp(3) < finish_time\n"
                + "\t\t\torder by finish_time;\n" + "\tctx enter_context_rt;\n" + "begin\n" + "\t\n"
                + "\tif p_customer_id is null or p_seat_id is null or p_product_code is null then\n"
                + "\t\tstate_code := 101;\n" + "\t\treturn;\n" + "\tend if;\n" + "\t\n"
                + "\tif p_data_count is null or p_data_count <= 0 then\n" + "\t\tstate_code := 101;\n" + "\t\treturn;\n"
                + "\tend if;\n" + "\t\n" + "\tif p_mode <> 1 and p_mode <> 2 then\n" + "\t\tstate_code := 207;\n"
                + "\t\treturn;\n" + "\tend if;\n" + "\t\n"
                + "\tselect localtimestamp(3) into c_request_time from dual;\n"
                + "\t\n" + "\tstate_code := 201;\n" + "\t\n" + "\topen c;\n" + "\tbegin\n" + "\t\tloop\n"
                + "\t\t\tfetch c into ctx;\n" + "\t\t\tif c%NOTFOUND then\n" + "\t\t\t\trollback; close c; return;\n"
                + "\t\t\tend if;\n" + "\t\t\t\n" + "\t\t\t-- either authr unavailable or service unavailable\n"
                + "\t\t\tif ctx.a_state <> 1 or ctx.s_state <> 1 then\n" + "\t\t\t\tstate_code := 209;\n"
                + "\t\t\t\tcontinue;\n" + "\t\t\tend if;\n" + "\t\t\t\n" + "\t\t\tif ctx.op_id is null then\n"
                + "\t\t\t\tstate_code := 543;\n" + "\t\t\t\tcontinue;\n" + "\t\t\tend if;\n" + "\t\t\t\n"
                + "\t\t\tdeclare\n" + "\t\t\t\tc_status pls_integer;\n" + "\t\t\tbegin\n"
                + "\t\t\t\tselect status into c_status from g_calltype_status s \n"
                + "\t\t\t\t\twhere s.authr_id = ctx.a_id and s.calltype = p_call_type;\n"
                + "\t\t\t\tif c_status = 1 and ctx.a_type = 7 or c_status = 2 and ctx.a_type = 1 then\n"
                + "\t\t\t\t\tnull;\n" + "\t\t\t\telse\n" + "\t\t\t\t\tstate_code := 553; continue;\n"
                + "\t\t\t\tend if;\n" + "\t\t\texception\n"
                + "\t\t\t\twhen no_data_found then state_code := 554; continue;\n" + "\t\t\tend;\n" + "\t\t\t\n"
                + "\t\t\tselect sys_guid() into transaction_id from dual;\n" + "\t\t\n" + "\t\t\tif p_mode = 1 then\n"
                + "\t\t\t\tstart_trans_standard(ctx, c_request_time, transaction_id, \n"
                + "\t\t\t\t\tp_seat_id, p_data_count, p_call_type, p_call_ip4_addr,\n" + "\t\t\t\t\tstate_code);\n"
                + "\t\t\telsif p_mode = 2 then\n"
                + "\t\t\t\tstart_trans_delegate(ctx, c_request_time, transaction_id, \n"
                + "\t\t\t\t\tp_seat_id, p_data_count, p_call_type, p_call_ip4_addr,\n" + "\t\t\t\t\tstate_code);\n"
                + "\t\t\tend if;\n" + "\t\t\t\n" + "\t\t\tif state_code = 0 then\n"
                + "\t\t\t\toutput_plan := ctx.output_plan;\n" + "\t\t\t\tdata_quanlity := ctx.data_quanlity;\n"
                + "\t\t\t\texit;\n" + "\t\t\tend if;\n" + "\t\tend loop;\n" + "\texception when others then\n"
                + "\t\tif c%ISOPEN then close c; raise; end if;\n" + "\tend;\n" + "\tclose c;\n" + "end;\n" + "\n"
                + "procedure calc_amount_exit_all_fix (\n" + "\tconf in varchar2,\n"
                + "\tret_code_tbl in ret_code_tbl_rt,\n" + "\twhether_tbl in bool_tbl_rt,\n"
                + "\ta_tbl out amount_tbl_rt,\n" + "\ta_sum out number\n" + ") is\n" + "\ti pls_integer;\n"
                + "\tunit_value number(12,2);\n" + "begin\n" + "\tbegin\n"
                + "\t\tselect cast(conf as number(12,2)) into unit_value from dual;\n" + "\texception\n"
                + "\t\twhen others then raise corrupted_fixvaluepolicy;\n" + "\tend;\n"
                + "\ta_tbl := amount_tbl_rt();\n"
                + "\ta_tbl.extend(ret_code_tbl.count);\n" + "\ta_sum := 0;\n" + "\ti := 1;\n"
                + "\twhile i <= ret_code_tbl.count loop\n" + "\t\tif whether_tbl(i) then\n"
                + "\t\t\ta_tbl(i) := unit_value;\n" + "\t\t\ta_sum := a_sum + unit_value;\n" + "\t\telse\n"
                + "\t\t\ta_tbl(i) := 0;\n" + "\t\tend if;\n" + "\t\ti := i + 1;\n" + "\tend loop;\n" + "end;\n" + "\n"
                + "procedure calc_amount_exit_all_map (\n" + "\tconf in varchar2,\n"
                + "\tret_code_tbl in ret_code_tbl_rt,\n" + "\twhether_tbl in bool_tbl_rt,\n"
                + "\ta_tbl out amount_tbl_rt,\n" + "\ta_sum out number\n" + ") is\n" + "\ti_lp pls_integer;\n"
                + "\ti_rp pls_integer;\n" + "\tparam_name varchar2(8);\n" + "\toffset pls_integer;\n" + "\t\n"
                + "\tret_code varchar2(8);\n" + "\tvalue_str varchar2(10);\n" + "\tvalue_len pls_integer;\n"
                + "\ti_value pls_integer;\n" + "\ti_scln pls_integer;\n" + "\tunit_value number(12,2);\n" + "begin\n"
                + "\ti_lp := instr(conf, '(');\n" + "\tif i_lp = 0 then\n" + "\t\traise corrupted_mapvaluepolicy;\n"
                + "\tend if;\n" + "\tparam_name := substr(conf, 1, i_lp - 1);\n"
                + "\ti_rp := instr(conf, ')', i_lp + 1);\n" + "\tif i_rp = 0 then\n"
                + "\t\traise corrupted_mapvaluepolicy;\n" + "\tend if;\n" + "\t\n"
                + "\tif param_name = 'RETCODE' then\n"
                + "\t\ta_tbl := amount_tbl_rt();\n" + "\t\ta_tbl.extend(ret_code_tbl.count);\n" + "\t\ta_sum := 0;\n"
                + "\t\t\n" + "\t\tfor i in ret_code_tbl.first .. ret_code_tbl.last loop\n"
                + "\t\t\tif whether_tbl(i) then\n" + "\t\t\t\tret_code := ret_code_tbl(i);\n"
                + "\t\t\t\tvalue_str := '['||ret_code||']';\n" + "\t\t\t\tvalue_len := length(value_str);\n"
                + "\t\t\t\ti_value := instr(conf, value_str, i_rp + 1);\n" + "\t\t\t\tif i_value = 0 then \n"
                + "\t\t\t\t\ta_tbl(i) := 0;\n" + "\t\t\t\telse\n"
                + "\t\t\t\t\ti_scln := instr(conf, ';', i_value + value_len);\n" + "\t\t\t\t\tif i_scln = 0 then\n"
                + "\t\t\t\t\t\traise corrupted_mapvaluepolicy;\n" + "\t\t\t\t\tend if;\n" + "\t\t\t\t\tbegin\n"
                + "\t\t\t\t\t\tselect cast(substr(conf, i_value + value_len, i_scln - i_value - value_len) as number) into unit_value from dual;\n"
                + "\t\t\t\t\texception\n" + "\t\t\t\t\t\twhen others then raise corrupted_mapvaluepolicy;\n"
                + "\t\t\t\t\tend;\n" + "\t\t\t\t\ta_tbl(i) := unit_value;\n"
                + "\t\t\t\t\ta_sum := a_sum + unit_value;\n"
                + "\t\t\t\tend if;\n" + "\t\t\telse\n" + "\t\t\t\ta_tbl(i) := 0;\n" + "\t\t\tend if;\n"
                + "\t\tend loop;\n" + "\telse\n" + "\t\traise unknown_mapvaluepolicy_param;\n" + "\tend if;\n"
                + "end;\n"
                + "\n" + "procedure calc_amount_exit_all (\n" + "\tpolicy in varchar2,\n" + "\tconf in varchar2,\n"
                + "\tstate_domain_0 in state_domain_0_rt,\n" + "\tstate_domain_1 in state_domain_1_rt,\n"
                + "\tstate_domain_2 in state_domain_2_rt,\n" + "\tstate_domain_3 in state_domain_3_rt,\n"
                + "\tret_code_tbl in ret_code_tbl_rt,\n" + "\twhether_tbl in bool_tbl_rt,\n"
                + "\ta_tbl out amount_tbl_rt,\n" + "\ta_sum out number\n" + ") is\n" + "begin\n"
                + "\tif policy = 'FIX' then\n"
                + "\t\tcalc_amount_exit_all_fix(conf, ret_code_tbl, whether_tbl, a_tbl, a_sum);\n"
                + "\telsif policy = 'LDR_DC' then\n" + "\t\tdeclare\n" + "\t\t\tparam_name varchar2(8);\n"
                + "\t\t\tbase pls_integer;\n" + "\t\t\toffset pls_integer;\n" + "\t\t\ta_i pls_integer;\n"
                + "\t\tbegin\n"
                + "\t\t\tparam_name := parse_ldr_param(conf);\n" + "\t\t\toffset := length(param_name) + 2;\n"
                + "\t\t\ta_i := 1;\n" + "\t\t\ta_sum := 0;\n" + "\t\t\tif param_name = 'CDC' then\n"
                + "\t\t\t\tbase := state_domain_0.charged;\n"
                + "\t\t\t\tcalc_amount_all_ldr_dc(conf, ret_code_tbl.count, base, offset, whether_tbl, a_i, a_tbl, a_sum);\n"
                + "\t\t\telsif param_name = 'CDC2' then\n" + "\t\t\t\tbase := state_domain_1.charged;\n"
                + "\t\t\t\tcalc_amount_all_ldr_dc(conf, ret_code_tbl.count, base, offset, whether_tbl, a_i, a_tbl, a_sum);\n"
                + "\t\t\telse\n" + "\t\t\t\traise unknown_ldrvaluepolicy_param;\n" + "\t\t\tend if;\n" + "\t\tend;\n"
                + "\telsif policy = 'LDR_AS' then\n" + "\t\tdeclare\n" + "\t\t\tparam_name varchar2(8);\n"
                + "\t\t\tbase pls_integer;\n" + "\t\t\toffset pls_integer;\n" + "\t\t\ta_i pls_integer;\n"
                + "\t\tbegin\n"
                + "\t\t\tparam_name := parse_ldr_param(conf);\n" + "\t\t\toffset := length(param_name) + 2;\n"
                + "\t\t\ta_i := 1;\n" + "\t\t\ta_sum := 0;\n" + "\t\t\tif param_name = 'CAS' then\n"
                + "\t\t\t\tbase := state_domain_2.charged;\n"
                + "\t\t\t\tcalc_amount_all_ldr_as(conf, ret_code_tbl.count, base, offset, whether_tbl, a_i, a_tbl, a_sum);\n"
                + "\t\t\telsif param_name = 'CAS2' then\n" + "\t\t\t\tbase := state_domain_3.charged;\n"
                + "\t\t\t\tcalc_amount_all_ldr_as(conf, ret_code_tbl.count, base, offset, whether_tbl, a_i, a_tbl, a_sum);\n"
                + "\t\t\telse\n" + "\t\t\t\traise unknown_ldrvaluepolicy_param;\n" + "\t\t\tend if;\n" + "\t\tend;\n"
                + "\telsif policy = 'MAPPING' then\n"
                + "\t\tcalc_amount_exit_all_map(conf, ret_code_tbl, whether_tbl, a_tbl, a_sum);\n" + "\telse\n"
                + "\t\traise unknown_valuepolicy;\n" + "\tend if;\n" + "end;\n" + "\n"
                + "function calc_amount_one_ldr_dc (\n" + "\tconf in varchar2,\n" + "\tbase in pls_integer,\n"
                + "\toffset in out pls_integer\n" + ") return number\n" + "is\n" + "\ti_cln pls_integer;\n"
                + "\ti_scln pls_integer;\n" + "\tstep_to pls_integer;\n" + "\tunit_value number;\n" + "begin\n"
                + "\tloop\n" + "\t\ti_cln := instr(conf, ':', offset);\n" + "\t\tbegin\n"
                + "\t\t\tselect cast(substr(conf, offset, i_cln - offset) as number) into step_to from dual;\n"
                + "\t\texception\n" + "\t\t\twhen others then raise corrupted_ldrvaluepolicy;\n" + "\t\tend;\n"
                + "\t\tif step_to = -1 then\n" + "\t\t\tbegin\n"
                + "\t\t\t\tselect cast(substr(conf, i_cln + 1) as number) into unit_value from dual;\n"
                + "\t\t\texception\n" + "\t\t\t\twhen others then raise corrupted_ldrvaluepolicy;\n" + "\t\t\tend;\n"
                + "\t\t\treturn unit_value;\n" + "\t\telse\n" + "\t\t\ti_scln := instr(conf, ';', i_cln + 1);\n"
                + "\t\t\tbegin\n"
                + "\t\t\t\tselect cast(substr(conf, i_cln + 1, i_scln - i_cln -1) as number) into unit_value from dual;\n"
                + "\t\t\texception\n" + "\t\t\t\twhen others then raise corrupted_ldrvaluepolicy;\n" + "\t\t\tend;\n"
                + "\t\tend if;\n" + "\t\t\n" + "\t\tif base + 1 < step_to then\n" + "\t\t\treturn unit_value;\n"
                + "\t\tend if;\n" + "\t\toffset := i_scln + 1;\n" + "\tend loop;\n" + "end;\n" + "\n"
                + "procedure unlock_balance_local (\n" + "\tp_service_id in raw,\n" + "\tp_balance_unit in varchar2,\n"
                + "\tunlock in number,\n" + "\tcharge in number\n" + ") is\n" + "begin\n"
                + "\tupdate g_charge_service_balance t\n"
                + "\t\tset locked_amount = locked_amount - unlock, consumed_amount = consumed_amount + charge\n"
                + "\t\twhere t.service_id = p_service_id and t.balance_unit = p_balance_unit;\n"
                + "\tif SQL%ROWCOUNT <> 1 then\n" + "\t\traise missing_local_balance;\n" + "\tend if;\n" + "end;\n"
                + "\n"
                + "procedure unlock_balance_global (\n" + "\tp_customer_id in raw,\n"
                + "\tp_balance_unit in varchar2,\n"
                + "\tunlock in number,\n" + "\tcharge in number\n" + ") is\n" + "begin\n"
                + "\tupdate g_customer_balance t\n"
                + "\t\tset locked_amount = locked_amount - unlock, available_amount = available_amount + unlock - charge\n"
                + "\t\twhere t.customer_id = p_customer_id and t.balance_unit = p_balance_unit;\n"
                + "\tif SQL%ROWCOUNT <> 1 then\n" + "\t\traise missing_global_balance;\n" + "\tend if;\n" + "end;\n"
                + "\n" + "procedure unlock_balance (\n" + "\tp_service_id in raw,\n"
                + "\tp_balance_source in varchar2,\n"
                + "\tp_balance_unit in varchar2,\n" + "\tunlock in number,\n" + "\tcharge in number\n" + ") is\n"
                + "begin\n" + "\tif p_balance_source = 'GLOBAL' then\n" + "\t\tupdate g_customer_balance t\n"
                + "\t\t\tset locked_amount = locked_amount - unlock,\n"
                + "\t\t\t\tavailable_amount = available_amount + unlock - charge\n" + "\t\t\twhere\n"
                + "\t\t\t\tt.customer_id = (select customer_id from g_charge_service t where t.recid = p_service_id)\n"
                + "\t\t\t\tand t.balance_unit = p_balance_unit;\n" + "\t\tif SQL%ROWCOUNT <> 1 then\n"
                + "\t\t\traise corrupted_global_balance;\n" + "\t\tend if;\n"
                + "\telsif p_balance_source = 'LOCAL' then\n" + "\t\tupdate g_charge_service_balance t\n"
                + "\t\t\tset locked_amount = locked_amount - unlock,\n"
                + "\t\t\t\tconsumed_amount = consumed_amount + charge\n" + "\t\t\twhere\n"
                + "\t\t\t\tt.service_id = p_service_id and t.balance_unit = p_balance_unit;\n"
                + "\t\tif SQL%ROWCOUNT <> 1 then\n" + "\t\t\traise corrupted_local_balance;\n" + "\t\tend if;\n"
                + "\telse\n" + "\t\traise unknown_balance_source;\n" + "\tend if;\n" + "end;\n" + "\n"
                + "procedure try_consume_balance_local (\n" + "\tp_service_id in raw,\n"
                + "\tp_balance_unit in varchar2,\n" + "\tamount in number,\n" + "\tokay out boolean\n" + ") is\n"
                + "begin\n" + "\tupdate g_charge_service_balance t\n"
                + "\t\tset consumed_amount = consumed_amount + amount\n"
                + "\t\twhere t.service_id = p_service_id and t.balance_unit = p_balance_unit\n"
                + "\t\t\tand t.limit_amount - t.locked_amount - t.consumed_amount - amount >= 0;\n"
                + "\t\tif SQL%ROWCOUNT = 0 then\n" + "\t\t\tokay := FALSE;\n" + "\t\telsif SQL%ROWCOUNT = 1 then\n"
                + "\t\t\tokay := TRUE;\n" + "\t\telse\n" + "\t\t\traise corrupted_local_balance;\n" + "\t\tend if;\n"
                + "end;\n" + "\n" + "procedure evaluate_exit_rule_poany (\n" + "\tctx in exit_context_rt,\n"
                + "\tr in exit_rule_context_rt,\n" + "\ttrans in trans_ongoing_rt,\n"
                + "\tret_code_tbl in ret_code_tbl_rt,\n" + "\twhether_tbl in bool_tbl_rt,\n"
                + "\twhether_count in pls_integer,\n" + "\teval_okay out boolean,\n" + "\tusing_rule out pls_integer,\n"
                + "\tusing_unit out varchar2,\n" + "\tusing_source out varchar2,\n" + "\ta_tbl out amount_tbl_rt,\n"
                + "\ta_sum out number\n" + ") is\n" + "\tcursor sd1_c (p_rule_id raw)\n"
                + "\t\tis select recid, charged_data_count, uncharged_data_count\n"
                + "\t\t\tfrom g_charge_rule_state_1 where recid = p_rule_id for update;\n"
                + "\tcursor sd3_c (p_rule_id raw)\n" + "\t\tis select charged_amount_sum\n"
                + "\t\t\tfrom g_charge_rule_state_3 where recid = p_rule_id for update;\n"
                + "\tstate_domain_1 state_domain_1_rt;\n" + "\tstate_domain_3 state_domain_3_rt;\n" + "begin\n"
                + "\tif r.exit_policy is null then\n" + "\t\traise poany_missing_exit_policy;\n" + "\tend if;\n" + "\n"
                + "\tif utl_raw.bit_and(r.domains, hextoraw('02000000')) = hextoraw('02000000') then\n"
                + "\t\topen sd1_c (r.r_id);\n" + "\t\tfetch sd1_c into state_domain_1;\n"
                + "\t\tif sd1_c%NOTFOUND then\n"
                + "\t\t\traise missing_rule_domain_1;\n" + "\t\tend if;\n" + "\tend if;\n" + "\n"
                + "\tif utl_raw.bit_and(r.domains, hextoraw('08000000')) = hextoraw('08000000') then\n"
                + "\t\topen sd3_c (r.r_id);\n" + "\t\tfetch sd3_c into state_domain_3;\n"
                + "\t\tif sd3_c%NOTFOUND then\n"
                + "\t\t\traise missing_rule_domain_3;\n" + "\t\tend if;\n" + "\tend if;\n" + "\n"
                + "\tcalc_amount_exit_all(r.exit_policy, r.exit_conf, null, state_domain_1, null, state_domain_3,\n"
                + "\t\tret_code_tbl, whether_tbl, a_tbl, a_sum);\n" + "\n" + "\tif r.source = 'NONE' then\n"
                + "\t\teval_okay := TRUE;\n" + "\telsif r.source = 'LOCAL' then\n"
                + "\t\ttry_consume_balance_local(ctx.s_id, r.unit, a_sum, eval_okay);\n"
                + "\telsif r.source = 'GLOBAL' then\n" + "\t\traise not_yet;\n" + "\telse\n"
                + "\t\traise unknown_balance_source;\n" + "\tend if;\n" + "\n" + "\tif eval_okay then\n"
                + "\t\tusing_rule := r.priority;\n" + "\t\tusing_unit := r.unit;\n" + "\t\tusing_source := r.source;\n"
                + "\n" + "\t\tif utl_raw.bit_and(r.domains, hextoraw('02000000')) = hextoraw('02000000') then\n"
                + "\t\t\tupdate g_charge_rule_state_1\n"
                + "\t\t\t\tset charged_data_count = charged_data_count + whether_count,\n"
                + "\t\t\t\t\tuncharged_data_count = uncharged_data_count + trans.data_count - whether_count\n"
                + "\t\t\t\twhere current of sd1_c;\n" + "\t\t\tclose sd1_c;\n" + "\t\tend if;\n" + "\n"
                + "\t\tif utl_raw.bit_and(r.domains, hextoraw('08000000')) = hextoraw('08000000') then\n"
                + "\t\t\tupdate g_charge_rule_state_3\n"
                + "\t\t\t\tset charged_amount_sum = charged_amount_sum + a_sum\n"
                + "\t\t\t\twhere current of sd3_c;\n" + "\t\t\tclose sd3_c;\n" + "\t\tend if;\n" + "\tend if;\n" + "\n"
                + "\tif sd1_c%ISOPEN then\n" + "\t\tclose sd1_c;\n" + "\tend if;\n" + "\tif sd3_c%ISOPEN then\n"
                + "\t\tclose sd3_c;\n" + "\tend if;\n" + "end;\n" + "\n" + "procedure finish_trans_state_5 (\n"
                + "\tctx in exit_context_rt,\n" + "\ttrans in trans_ongoing_rt,\n"
                + "\tentries in trans_ongoing_entry_tbl_rt,\n" + "\tret_code_tbl in ret_code_tbl_rt,\n"
                + "\twhether_tbl in bool_tbl_rt,\n" + "\twhether_count in pls_integer\n" + ") is\n" + "begin\n"
                + "\tnull;\n" + "end;\n" + "\n" + "procedure evaluate_whether_charge (\n"
                + "\tctx in exit_context_rt,\n"
                + "\tp_ret_code_tbl in ret_code_tbl_rt,\n" + "\twhether_tbl out bool_tbl_rt,\n"
                + "\twhether_count out pls_integer\n" + ") is\n" + "begin\n" + "\twhether_tbl := bool_tbl_rt();\n"
                + "\twhether_tbl.extend(p_ret_code_tbl.count);\n" + "\twhether_count := 0;\n"
                + "\tfor i in p_ret_code_tbl.first .. p_ret_code_tbl.last loop\n"
                + "\t\tif instr(ctx.charge_condition, '\"'||p_ret_code_tbl(i)||'\"') > 0 then\n"
                + "\t\t\twhether_tbl(i) := TRUE;\n" + "\t\t\twhether_count := whether_count + 1;\n" + "\t\telse\n"
                + "\t\t\twhether_tbl(i) := FALSE;\n" + "\t\tend if;\n" + "\tend loop;\n" + "end;\n" + "\n"
                + "procedure split_ret_code (\n" + "\tp_ret_code_concat in varchar2,\n"
                + "\tret_code_tbl out ret_code_tbl_rt\n" + ") is\n" + "\tfr pls_integer;\n" + "\tci pls_integer;\n"
                + "\tti pls_integer;\n" + "\tlen constant pls_integer := length(p_ret_code_concat);\n" + "begin\n"
                + "\tret_code_tbl := ret_code_tbl_rt();\n" + "\tfr := 1;\n" + "\tti := 1;\n" + "\tloop\n"
                + "\t\tci := instr(p_ret_code_concat, ',', fr);\n" + "\t\tif ci = 0 then\n"
                + "\t\t\tret_code_tbl.extend(1);\n" + "\t\t\tret_code_tbl(ti) := substr(p_ret_code_concat, fr);\n"
                + "\t\t\treturn;\n" + "\t\telse\n" + "\t\t\tret_code_tbl.extend(1);\n"
                + "\t\t\tret_code_tbl(ti) := substr(p_ret_code_concat, fr, ci - fr);\n" + "\t\t\tti := ti + 1;\n"
                + "\t\t\tfr := ci + 1;\n" + "\t\tend if;\n" + "\tend loop;\n" + "end;\n" + "\n"
                + "procedure finish_trans (\n" + "\tp_transaction_id in raw,\n" + "\tp_ret_code_concat in varchar2\n"
                + ") is\n" + "\tstate_code pls_integer;\n" + "begin\n"
                + "\tfinish_trans_debug(p_transaction_id, p_ret_code_concat, state_code);\n" + "end;\n" + "\n"
                + "procedure finish_trans_debug (\n" + "\tp_transaction_id in raw,\n"
                + "\tp_ret_code_concat in varchar2,\n" + "\tstate_code out pls_integer\n" + ") is\n"
                + "\tpragma autonomous_transaction;\n" + "\tret_code_tbl ret_code_tbl_rt;\n"
                + "\tctx exit_context_rt;\n"
                + "\twhether_tbl bool_tbl_rt;\n" + "\twhether_count pls_integer;\n" + "\ttrans trans_ongoing_rt;\n"
                + "\tentries trans_ongoing_entry_tbl_rt;\n" + "begin\n"
                + "\tif p_transaction_id is null or p_ret_code_concat is null then\n"
                + "\t\tstate_code := 101; return;\n"
                + "\tend if;\n" + "\n" + "\tbegin\n" + "\t\tsplit_ret_code(p_ret_code_concat, ret_code_tbl);\n"
                + "\texception\n" + "\t\twhen others then state_code := 101; return;\n" + "\tend;\n" + "\n"
                + "\tbegin\n"
                + "\t\tselect recid, seat_id, service_id, request_time, data_count,\n"
                + "\t\t\t\tstart_time, trans_state, hit_rule, balance_unit,\n"
                + "\t\t\t\tbalance_source, enter_amount_sum into trans\n"
                + "\t\t\tfrom g_transaction_ongoing where recid = p_transaction_id for update; -- LOCK !\n"
                + "\texception\n" + "\t\twhen no_data_found then rollback; state_code := 203; return;\n" + "\tend;\n"
                + "\n" + "\tif trans.data_count <> ret_code_tbl.count then\n"
                + "\t\trollback; state_code := 204; return;\n" + "\tend if;\n" + "\n"
                + "\tif trans.trans_state < 1 or trans.trans_state > 5 then\n"
                + "\t\trollback; state_code := 205; return;\n" + "\tend if;\n" + "\n" + "\tbegin\n"
                + "\t\t-- may not ordered by seq\n" + "\t\tselect recid, trans_id, seq, enter_amount\n"
                + "\t\t\tbulk collect into entries\n"
                + "\t\t\tfrom g_transaction_ongoing_entry where trans_id = p_transaction_id;\n" + "\texception\n"
                + "\t\twhen no_data_found then raise unexpected_ongoing_entry;\n" + "\tend;\n" + "\n"
                + "\tif entries.count <> trans.data_count then\n" + "\t\traise unexpected_ongoing_entry;\n"
                + "\tend if;\n" + "\n" + "\tbegin\n"
                + "\t\tselect s_id, customer_id, enable_local_gift, r_id, priority, unit, source, exit_policy, exit_conf, exit_ref, domains, cc_id, charge_condition into ctx\n"
                + "\t\t\tfrom mv_charge_core where s_id = trans.service_id;\n" + "\texception\n"
                + "\t\twhen no_data_found then raise missing_charge_service;\n" + "\tend;\n" + "\n"
                + "\tevaluate_whether_charge(ctx, ret_code_tbl, whether_tbl, whether_count);\n" + "\n"
                + "\tif trans.trans_state = 1 then\n"
                + "\t\tfinish_trans_state_1(ctx, trans, entries, ret_code_tbl, whether_tbl, whether_count);\n"
                + "\t\tstate_code := 0; commit;\n" + "\telsif trans.trans_state = 2 then\n"
                + "\t\tfinish_trans_state_2(ctx, trans, entries, ret_code_tbl, whether_tbl, whether_count);\n"
                + "\t\tstate_code := 0; commit;\n" + "\telsif trans.trans_state = 3 then\n"
                + "\t\tfinish_trans_state_3(trans, entries, ret_code_tbl, whether_tbl, whether_count);\n"
                + "\t\tstate_code := 0; commit;\n" + "\telsif trans.trans_state = 4 then\n"
                + "\t\tfinish_trans_state_4(ctx, trans, entries, ret_code_tbl, whether_tbl, whether_count);\n"
                + "\t\tstate_code := 0; commit;\n" + "\telsif trans.trans_state = 5 then\n"
                + "\t\tfinish_trans_state_5(ctx, trans, entries, ret_code_tbl, whether_tbl, whether_count);\n"
                + "\t\tstate_code := 0; commit;\n" + "\telse\n" + "\t\tstate_code := 205; rollback; return;\n"
                + "\tend if;\n" + "exception\n" + "\twhen unknown_ruleplan then rollback; state_code := 532;\n"
                + "\twhen unknown_valuepolicy then rollback; state_code := 533;\n"
                + "\twhen corrupted_fixvaluepolicy then rollback; state_code := 534;\n"
                + "\twhen corrupted_ldrvaluepolicy then rollback; state_code := 535;\n"
                + "\twhen unknown_balance_source then rollback; state_code := 536;\n"
                + "\twhen missing_rule_domain_0 then rollback; state_code := 539;\n"
                + "\twhen unknown_ldrvaluepolicy_param then rollback; state_code := 541;\n"
                + "\twhen prany_missing_hit_rule then rollback; state_code := 542;\n"
                + "\twhen missing_charge_condition then rollback; state_code := 543;\n"
                + "\twhen unexpected_ongoing_entry then rollback; state_code := 544;\n"
                + "\twhen poany_evaluate_failed then rollback; state_code := 545;\n"
                + "\twhen missing_charge_service then rollback; state_code := 546;\n"
                + "\twhen missing_rule_domain_1 then rollback; state_code := 547;\n"
                + "\twhen poany_missing_exit_policy then rollback; state_code := 548;\n"
                + "\twhen missing_rule_domain_2 then rollback; state_code := 549;\n"
                + "\twhen missing_rule_domain_3 then rollback; state_code := 550;\n"
                + "\twhen missing_global_balance then rollback; state_code := 551;\n"
                + "\twhen missing_local_balance then rollback; state_code := 552;\n"
                + "\twhen unknown_mapvaluepolicy_param then rollback; state_code := 553;\n"
                + "\twhen corrupted_mapvaluepolicy then rollback; state_code := 554;\n"
                + "\twhen rc_missing_exit_ref then rollback; state_code := 555;\n"
                + "\twhen rc_duplicate_exit_ref then rollback; state_code := 556;\n"
                + "\twhen not_yet then rollback; state_code := 1542;\n"
                + "\twhen others then rollback; state_code := 1543;\n" + "\t\tdbms_output.put_line(SQLERRM); raise;\n"
                + "end;\n" + "\n" + "end g_core;\n";
        ParseOraclePLResult result = PLParser.parseObOracle(pl);
        Assert.assertEquals(66, result.getVaribaleList().size());
    }

    @Test
    public void testParsePackageWithPipelined() throws Exception {
        String pl = "create or replace Package \"SPK_A2_PSNLACCOUNT_OB\" Is --  Author  : XUEWEIJUN\n"
                + "--  Created : 2012-08-30\n"
                + "--  Purpose : 个人账户管理\n"
                + "--  update : chenke 20130115 规范化及调试\n"
                + "Type Ac43_Table_Recode Is Record(\n"
                + "  Aaz223 Ac43.Aaz223 % Type,\n"
                + "  -- AC43主键\n"
                + "  Aaa115 Ac43.Aaa115 % Type,\n"
                + "  -- 应缴类型\n"
                + "  aae079 ac43.aae079 % type,\n"
                + "  Maxaae003 Char(6),\n"
                + "  -- 所属期\n"
                + "  Minaae003 Char(6),\n"
                + "  -- 所属期\n"
                + "  Aae202 Ac43.Aae202 % Type,\n"
                + "  -- 累计缴费月数增加额\n"
                + "  Aae180 Ac43.Aae180 % Type,\n"
                + "  -- 缴费基数\n"
                + "  Aae415 Ac43.Aae415 % Type,\n"
                + "  -- 累计记账基数增加额\n"
                + "  Aaa041 Ac43.Aaa041 % Type,\n"
                + "  -- 个人缴费比例\n"
                + "  Aaa042 Ac43.Aaa042 % Type,\n"
                + "  -- 单位缴费比例\n"
                + "  Aaa043 Ac43.Aaa043 % Type,\n"
                + "  -- 单位缴费划入个人账户比例或定额标准\n"
                + "  Aae001 Varchar2(6),\n"
                + "  -- 缴费所属期年度\n"
                + "  Aae081 Number(16, 6),\n"
                + "  -- 单位实缴划入个人账户金额\n"
                + "  Aae083 Number(16, 6),\n"
                + "  -- 个人实缴划入个人账户金额\n"
                + "  Aae085 Number(16, 6),\n"
                + "  -- 财政补助（补贴）实拨款划入个人账户金额\n"
                + "  Aae263 Number(16, 6),\n"
                + "  -- 单位利息\n"
                + "  Aae265 Number(16, 6),\n"
                + "  -- 个人利息\n"
                + "  aae003 ac43.aae003 % type,\n"
                + "  aab019 ab01.aab019 % type\n"
                + ");\n"
                + "Function Cal_Curyear_Interest(\n"
                + "  Pi_Aac001 In Varchar2,\n"
                + "  -- 个人标识\n"
                + "  Pi_Aae140 In Varchar2,\n"
                + "  -- 险种类型\n"
                + "  Pi_Type In Varchar2,\n"
                + "  -- 结转类型\n"
                + "  Pi_Aae041_Contri In Varchar2,\n"
                + "  -- 收入开始年月\n"
                + "  Pi_Aae042_Contri In Varchar2,\n"
                + "  -- 收入截至年月\n"
                + "  Pi_Aae041_Pay In Varchar2,\n"
                + "  -- 支出开始年月\n"
                + "  Pi_Aae042_Pay In Varchar2,\n"
                + "  -- 支出结束年月\n"
                + "  Pi_Aaa027 In Varchar2,\n"
                + "  -- 统筹区编码\n"
                + "  Pi_Aac008 In Varchar2\n"
                + ") Return Table_Acb3 Pipelined;\n"
                + "Procedure Groupby_Psnl_Interest(\n"
                + "  Pi_Aac001 In Varchar2,\n"
                + "  -- 个人标识\n"
                + "  Pi_Aae140 In Varchar2,\n"
                + "  -- 险种类型\n"
                + "  Pi_Aae041 In Varchar2,\n"
                + "  -- 结转开始年月\n"
                + "  Pi_Aae042 In Varchar2,\n"
                + "  -- 结转终止月份(年月为YYYYMM)\n"
                + "  Pi_Aae001 In Varchar2,\n"
                + "  -- 结转年度\n"
                + "  Pi_Zzny In Varchar2,\n"
                + "  -- 终止年月\n"
                + "  Pi_Type In Varchar2,\n"
                + "  -- -- 结息类型（1结转到某时点(有返回ac51)，2转移结转（利率不同），3年度结转（更新利息），4退休终止结息,5按照银行利率结息）\n"
                + "  Recreateflag In Varchar2,\n"
                + "  -- 重新生成标志（0没有的年度生成不更新数据，1没有的年度生成更新数据，2全部重新生成不更新数据，3全部重新生成更新数据）\n"
                + "  Pi_Ac51list In Ac51_Table_Recode,\n"
                + "  -- 上年度信息\n"
                + "  Pi_Aaa027 In Varchar2,\n"
                + "  -- 统筹区编码\n"
                + "  Pi_Aac008 In Varchar2,\n"
                + "  -- 在职退休标志\n"
                + "  Po_Ac51list Out Ac51_Table_Recode,\n"
                + "  -- 返回上年度信息\n"
                + "  Po_Retcode Out Number,\n"
                + "  -- 返回值(参照:Procedure 调用返回值定义)\n"
                + "  Po_Errmsg Out Varchar2 -- 错误描述\n"
                + ");\n"
                + "End Spk_A2_Psnlaccount_ob";
        ParseOraclePLResult result = PLParser.parseObOracle(pl);
        Assert.assertEquals(0, result.getVaribaleList().size());
        Assert.assertEquals("SPK_A2_PSNLACCOUNT_OB", result.getPlName());
        Assert.assertEquals("Package", result.getPlType());
        Assert.assertEquals(1, result.getTypeList().size());
        Assert.assertEquals(1, result.getProcedureList().size());
        Assert.assertEquals(1, result.getFunctionList().size());
    }

    @Test
    public void test_parse_mysql_function_wit_quotation() {
        String pl = "create function `返回参DECIMAL(10,0)` ( `p` varchar(45)) returns decimal(10,0) begin \n"
                + "end";
        ParseMysqlPLResult result = PLParser.parseObMysql(pl);
        Assert.assertEquals(1, result.getParamList().size());
        Assert.assertEquals("p", result.getParamList().get(0).getParamName());
    }

    @Test
    public void test_parse_oracle_alter_function() {
        String pl = "ALTER FUNCTION func COMPILE";
        ParseOraclePLResult result = PLParser.parseObOracle(pl);
        Assert.assertEquals(1, result.getDbObjectNameList().size());
        Assert.assertEquals("FUNC", result.getDbObjectNameList().get(0));
    }

    @Test
    public void test_parse_oracle_alter_procedure() {
        String pl = "ALTER PROCEDURE pro COMPILE";
        ParseOraclePLResult result = PLParser.parseObOracle(pl);
        Assert.assertEquals(1, result.getDbObjectNameList().size());
        Assert.assertEquals("PRO", result.getDbObjectNameList().get(0));
    }

    @Test
    public void test_parse_oracle_alter_package() {
        String pl = "ALTER PACKAGE pkg COMPILE PACKAGE";
        ParseOraclePLResult result = PLParser.parseObOracle(pl);
        Assert.assertEquals(1, result.getDbObjectNameList().size());
        Assert.assertEquals("PKG", result.getDbObjectNameList().get(0));
        Assert.assertEquals(DBObjectType.PACKAGE, result.getDbObjectType());
    }

    @Test
    public void test_parse_oracle_alter_package_body() {
        String pl = "ALTER PACKAGE pkg COMPILE BODY";
        ParseOraclePLResult result = PLParser.parseObOracle(pl);
        Assert.assertEquals(1, result.getDbObjectNameList().size());
        Assert.assertEquals("PKG", result.getDbObjectNameList().get(0));
        Assert.assertEquals(DBObjectType.PACKAGE_BODY, result.getDbObjectType());
    }

    @Test
    public void test_parse_oracle_procedure_with_comment() {
        String pl = "create or replace PROCEDURE SP_AM_LOANACCT_DETAIL_TEST\n"
                + "(\n"
                + "    VI_CDATADATE  IN VARCHAR2 ,    \n"
                + "    VO_CSQLCODE   OUT NUMBER       \n"
                + ")\n"
                + "IS\n"
                + "BEGIN \n"
                + "VO_CSQLCODE := 0;      \n"
                + "    INSERT INTO TEST_20211230\n"
                + "      (c1,\n"
                + "    c2\n"
                + "       )\n"
                + "      SELECT c1,c2\n"
                + "        FROM TEST_20211230 T \n"
                + "        where c2=VI_CDATADATE ;\n"
                + "        commit;\n"
                + "VO_CSQLCODE:=SQLCODE;\n"
                + "END;";
        ParseOraclePLResult result = PLParser.parseObOracle(pl);
        Assert.assertEquals("PROCEDURE", result.getPlType());
        Assert.assertEquals("SP_AM_LOANACCT_DETAIL_TEST", result.getPlName());
        Assert.assertEquals(1, result.getProcedureList().size());
        Assert.assertEquals("SP_AM_LOANACCT_DETAIL_TEST", result.getProcedureList().get(0).getProName());
    }

    @Test
    public void test_parse_procedure_with_non_letter_name_without_double_quote() {
        String pl = "create or replace procedure 威威(param in VARCHAR default 'hello') is v_str varchar2(32);\n"
                + "begin\n"
                + "  v_str := 'Hello,world';\n"
                + "dbms_output.put_line(v_str);\n"
                + "end\n"
                + "  威威;";
        ParseOraclePLResult r1 = PLParser.parseOracle(pl);
        Assert.assertEquals(1, r1.getProcedureList().get(0).getParams().size());
        ParseOraclePLResult r2 = PLParser.parseObOracle(pl);
        Assert.assertEquals(1, r2.getProcedureList().get(0).getParams().size());
    }

    @Test
    public void parseObMysql_procWithTid_getParamsSucceed() {
        String sql = "create procedure pro7 (OUT `a` varchar(45))\n"
                + "begin\n"
                + "declare\n"
                + "  tid int;\n"
                + "set\n"
                + "  a = 2;\n"
                + "set\n"
                + "  tid = a + 2;\n"
                + "select\n"
                + "  tid;\n"
                + "end";
        ParseMysqlPLResult actual = PLParser.parseObMysql(sql);
        Assert.assertEquals(1, actual.getParamList().size());
        Assert.assertEquals(1, actual.getVaribaleList().size());
    }

    @Test
    public void parseObMysql_procWithCycleReservedKeyWord_getParamsSucceed() {
        String sql = "CREATE FUNCTION `NEXTVAL`(a_seq_name VARCHAR(55)) RETURNS bigint(20)\n"
                + "BEGIN\n"
                + "DECLARE\n"
                + "  seq_val BIGINT;\n"
                + "DECLARE\n"
                + "  min_val BIGINT;\n"
                + "DECLARE\n"
                + "  max_val BIGINT;\n"
                + "DECLARE\n"
                + "  cycle_val VARCHAR(10);\n"
                + "SET\n"
                + "  seq_val = -1;\n"
                + "IF EXISTS(\n"
                + "    SELECT\n"
                + "      1\n"
                + "    FROM\n"
                + "      TFM_SEQUENCES holdlock\n"
                + "    WHERE\n"
                + "      SEQUENCE_NAME = a_seq_name\n"
                + "  ) THEN\n"
                + "SELECT\n"
                + "  CURRENT_VALUE + INCREMENT_BY,\n"
                + "  MIN_VALUE,\n"
                + "  MAX_VALUE,\n"
                + "  CYCLE INTO seq_val,\n"
                + "  min_val,\n"
                + "  max_val,\n"
                + "  cycle_val\n"
                + "FROM\n"
                + "  TFM_SEQUENCES\n"
                + "WHERE\n"
                + "  SEQUENCE_NAME = a_seq_name FOR\n"
                + "UPDATE;\n"
                + "IF seq_val > max_val THEN IF cycle_val = 'CYCLE' THEN\n"
                + "SET\n"
                + "  seq_val = min_val;\n"
                + "END\n"
                + "  IF;\n"
                + "END\n"
                + "  IF;\n"
                + "UPDATE\n"
                + "  TFM_SEQUENCES\n"
                + "SET\n"
                + "  CURRENT_VALUE = seq_val\n"
                + "WHERE\n"
                + "  SEQUENCE_NAME = a_seq_name;\n"
                + "END\n"
                + "  IF;\n"
                + "RETURN seq_val;\n"
                + "END";
        ParseMysqlPLResult actual = PLParser.parseObMysql(sql);
        Assert.assertEquals(1, actual.getParamList().size());
    }

    @Test
    public void parseOracle_packageWithChineseChar_getNameSucceed() {
        String sql = "create\n"
                + "or replace PACKAGE PKG_OB_测试程序包 AS FUNCTION fun_example (p1 IN NUMBER) RETURN NUMBER;\n"
                + "PROCEDURE PRC_L_OB_PROCEDURE_DEMO(\n"
                + "  PRM_AAC001 IN NUMBER,\n"
                + "  PRM_AAC002 IN VARCHAR2,\n"
                + "  PRM_appContext IN VARCHAR2,\n"
                + "  PRM_APPCODE OUT NUMBER,\n"
                + "  PRM_ERRORMSG OUT VARCHAR2\n"
                + ");\n"
                + "PROCEDURE PRC_L_OB_1测试程序包 (\n"
                + "  PRM_AAC001 IN NUMBER,\n"
                + "  PRM_AAC002 IN VARCHAR2,\n"
                + "  PRM_appContext IN VARCHAR2,\n"
                + "  PRM_APPCODE OUT NUMBER,\n"
                + "  PRM_ERRORMSG OUT VARCHAR2\n"
                + ");\n"
                + "PROCEDURE 测试程序包PRC_L_OB_1 (\n"
                + "  PRM_AAC001 IN NUMBER,\n"
                + "  PRM_AAC002 IN VARCHAR2,\n"
                + "  PRM_appContext IN VARCHAR2,\n"
                + "  PRM_APPCODE OUT NUMBER,\n"
                + "  PRM_ERRORMSG OUT VARCHAR2\n"
                + ");\n"
                + "PROCEDURE 测试程序包 (\n"
                + "  PRM_AAC001 IN NUMBER,\n"
                + "  PRM_AAC002 IN VARCHAR2,\n"
                + "  PRM_appContext IN VARCHAR2,\n"
                + "  PRM_APPCODE OUT NUMBER,\n"
                + "  PRM_ERRORMSG OUT VARCHAR2\n"
                + ");\n"
                + "END\n"
                + "  PKG_OB_测试程序包";
        ParseOraclePLResult actual = PLParser.parseOracle(sql);
        Assert.assertEquals("PKG_OB_测试程序包", actual.getPlName());
        Assert.assertEquals(4, actual.getProcedureList().size());
        Assert.assertEquals(1, actual.getFunctionList().size());
    }

    @Test
    public void parseOracle_commitStmt_getSqlTypeSucceed() {
        ParseOraclePLResult actual = PLParser.parseOracle("commit");
        Assert.assertEquals(DBObjectType.OTHERS, actual.getDbObjectType());
        Assert.assertEquals(SqlType.COMMIT, actual.getSqlType());
    }

    @Test
    public void parseOracle_rollbackStmt_getSqlTypeSucceed() {
        ParseOraclePLResult actual = PLParser.parseOracle("rollback");
        Assert.assertEquals(DBObjectType.OTHERS, actual.getDbObjectType());
        Assert.assertEquals(SqlType.ROLLBACK, actual.getSqlType());
    }

    @Test
    public void parseOracle_commentOnTable_getSqlTypeSucceed() {
        ParseOraclePLResult actual = PLParser.parseOracle("comment on table a is 'xxx'");
        Assert.assertEquals(DBObjectType.TABLE, actual.getDbObjectType());
        Assert.assertEquals(SqlType.COMMENT_ON, actual.getSqlType());
    }

    @Test
    public void parseOracle_commentOnColumn_getSqlTypeSucceed() {
        ParseOraclePLResult actual = PLParser.parseOracle("comment on column a is 'xxx'");
        Assert.assertEquals(DBObjectType.COLUMN, actual.getDbObjectType());
        Assert.assertEquals(SqlType.COMMENT_ON, actual.getSqlType());
    }

    @Test
    public void parseOracle_commentOnMaterialized_getSqlTypeSucceed() {
        ParseOraclePLResult actual = PLParser.parseOracle("comment on materialized a is 'xxx'");
        Assert.assertEquals(DBObjectType.OTHERS, actual.getDbObjectType());
        Assert.assertEquals(SqlType.COMMENT_ON, actual.getSqlType());
    }

    @Test
    public void parseOracle_call_getSqlTypeSucceed() {
        ParseOraclePLResult actual = PLParser.parseOracle("call proc()");
        Assert.assertEquals(DBObjectType.PROCEDURE, actual.getDbObjectType());
        Assert.assertEquals(SqlType.CALL, actual.getSqlType());
    }

    @Test
    public void parseOBMysql_call_getSqlTypeSucceed() {
        ParseMysqlPLResult actual = PLParser.parseObMysql("call proc()");
        Assert.assertEquals(DBObjectType.PROCEDURE, actual.getDbObjectType());
        Assert.assertEquals(SqlType.CALL, actual.getSqlType());
    }

    @Test
    public void test_oracle_alter_session() {
        String sql = "alter SESSION set ob_query_timeout=6000000000;";
        ParseOraclePLResult result = PLParser.parseOracle(sql);
        Assert.assertEquals(SqlType.ALTER_SESSION, result.getSqlType());
        Assert.assertEquals(DBObjectType.OTHERS, result.getDbObjectType());
    }

    @Test
    public void test_oracle_set_session() {
        String sql = "SET SESSION ob_query_timeout=6000000000;";
        ParseOraclePLResult result = PLParser.parseOracle(sql);
        Assert.assertEquals(SqlType.SET_SESSION, result.getSqlType());
        Assert.assertEquals(DBObjectType.OTHERS, result.getDbObjectType());
    }
}
