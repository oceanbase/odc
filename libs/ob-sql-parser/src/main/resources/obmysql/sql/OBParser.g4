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

parser grammar OBParser;


options { tokenVocab=OBLexer; }

@parser::members {
public boolean is_pl_parse_ = false;
public boolean is_pl_parse_expr_ = false;
}


// start rule: sql_stmt

sql_stmt
    : stmt_list
    ;

stmt_list
    : EOF
    | DELIMITER
    | stmt EOF
    | stmt DELIMITER EOF?
    ;

stmt
    : select_stmt
    | insert_stmt
    | create_table_stmt
    | create_function_stmt
    | drop_function_stmt
    | drop_procedure_stmt
    | drop_trigger_stmt
    | create_table_like_stmt
    | create_database_stmt
    | drop_database_stmt
    | alter_database_stmt
    | use_database_stmt
    | update_stmt
    | delete_stmt
    | drop_table_stmt
    | drop_view_stmt
    | explain_stmt
    | create_outline_stmt
    | alter_outline_stmt
    | drop_outline_stmt
    | show_stmt
    | prepare_stmt
    | variable_set_stmt
    | execute_stmt
    | alter_table_stmt
    | alter_system_stmt
    | audit_stmt
    | deallocate_prepare_stmt
    | create_user_stmt
    | drop_user_stmt
    | set_password_stmt
    | rename_user_stmt
    | lock_user_stmt
    | grant_stmt
    | revoke_stmt
    | begin_stmt
    | commit_stmt
    | rollback_stmt
    | create_tablespace_stmt
    | drop_tablespace_stmt
    | alter_tablespace_stmt
    | rotate_master_key_stmt
    | create_index_stmt
    | drop_index_stmt
    | kill_stmt
    | create_mlog_stmt
    | drop_mlog_stmt
    | help_stmt
    | create_view_stmt
    | create_mview_stmt
    | create_tenant_stmt
    | create_standby_tenant_stmt
    | alter_tenant_stmt
    | drop_tenant_stmt
    | create_restore_point_stmt
    | drop_restore_point_stmt
    | create_resource_stmt
    | alter_resource_stmt
    | drop_resource_stmt
    | set_names_stmt
    | set_charset_stmt
    | create_tablegroup_stmt
    | drop_tablegroup_stmt
    | alter_tablegroup_stmt
    | rename_table_stmt
    | truncate_table_stmt
    | set_transaction_stmt
    | create_savepoint_stmt
    | rollback_savepoint_stmt
    | release_savepoint_stmt
    | lock_tables_stmt
    | unlock_tables_stmt
    | flashback_stmt
    | purge_stmt
    | analyze_stmt
    | load_data_stmt
    | create_dblink_stmt
    | drop_dblink_stmt
    | create_sequence_stmt
    | alter_sequence_stmt
    | drop_sequence_stmt
    | xa_begin_stmt
    | xa_end_stmt
    | xa_prepare_stmt
    | xa_commit_stmt
    | xa_rollback_stmt
    | switchover_cluster_stmt
    | disconnect_cluster_stmt
    | alter_cluster_stmt
    | optimize_stmt
    | dump_memory_stmt
    | protection_mode_stmt
    | get_diagnostics_stmt
    | pl_expr_stmt
    | method_opt
    | switchover_tenant_stmt
    | recover_tenant_stmt
    | transfer_partition_stmt
    | create_tenant_snapshot_stmt
    | drop_tenant_snapshot_stmt
    | clone_tenant_stmt
    ;

pl_expr_stmt
    : DO expr
    ;

switchover_tenant_stmt
    : ALTER SYSTEM switchover_clause
    ;

switchover_clause
    : ACTIVATE STANDBY tenant_name?
    | SWITCHOVER TO PRIMARY tenant_name?
    | SWITCHOVER TO STANDBY tenant_name?
    ;

recover_tenant_stmt
    : ALTER SYSTEM RECOVER STANDBY tenant_name? recover_point_clause?
    ;

recover_point_clause
    : UNTIL TIME COMP_EQ STRING_VALUE
    | UNTIL SCN COMP_EQ INTNUM
    | UNTIL UNLIMITED
    | CANCEL
    ;

transfer_partition_stmt
    : ALTER SYSTEM transfer_partition_clause tenant_name?
    ;

transfer_partition_clause
    : TRANSFER PARTITION part_info TO LS INTNUM
    ;

part_info
    : TABLE_ID opt_equal_mark INTNUM Comma OBJECT_ID opt_equal_mark INTNUM
    ;

expr_list
    : expr (Comma expr)*
    ;

expr_as_list
    : expr_with_opt_alias (Comma expr_with_opt_alias)*
    ;

expr_with_opt_alias
    : expr
    | expr AS? (column_label|STRING_VALUE)
    ;

column_ref
    : column_name
    | (Dot?|relation_name Dot) (relation_name|mysql_reserved_keyword) Dot (column_name|mysql_reserved_keyword|Star)
    ;

complex_string_literal
    : charset_introducer? STRING_VALUE
    | charset_introducer PARSER_SYNTAX_ERROR
    | STRING_VALUE string_val_list
    | NATIONAL_LITERAL
    ;

charset_introducer
    : UnderlineUTF8
    | UnderlineUTF8MB4
    | UnderlineBINARY
    | UnderlineGBK
    | UnderlineLATIN1
    | UnderlineGB18030
    | UnderlineGB18030_2022
    | UnderlineUTF16
    ;

literal
    : complex_string_literal
    | DATE_VALUE
    | TIMESTAMP_VALUE
    | INTNUM
    | HEX_STRING_VALUE
    | APPROXNUM
    | DECIMAL_VAL
    | BOOL_VALUE
    | NULLX
    | PARSER_SYNTAX_ERROR
    ;

number_literal
    : INTNUM
    | DECIMAL_VAL
    ;

expr_const
    : literal
    | SYSTEM_VARIABLE
    | QUESTIONMARK
    | global_or_session_alias Dot column_name
    ;

conf_const
    : STRING_VALUE
    | DATE_VALUE
    | TIMESTAMP_VALUE
    | Minus? INTNUM
    | APPROXNUM
    | Minus? DECIMAL_VAL
    | BOOL_VALUE
    | NULLX
    | SYSTEM_VARIABLE
    | global_or_session_alias Dot column_name
    ;

global_or_session_alias
    : GLOBAL_ALIAS
    | SESSION_ALIAS
    ;

bool_pri
    : predicate
    | bool_pri (COMP_EQ|COMP_GE|COMP_GT|COMP_LE|COMP_LT|COMP_NE|COMP_NSEQ) predicate
    | bool_pri IS not? NULLX
    | bool_pri (COMP_EQ|COMP_GE|COMP_GT|COMP_LE|COMP_LT|COMP_NE) (ALL|ANY|SOME) LeftParen select_no_parens RightParen
    ;


predicate
    : bit_expr not? IN in_expr
    | bit_expr not? BETWEEN bit_expr AND predicate
    | bit_expr not? LIKE (simple_expr|string_val_list) ((ESCAPE simple_expr)?|ESCAPE string_val_list)
    | bit_expr not? REGEXP (string_val_list|bit_expr)
    | bit_expr MEMBER OF LeftParen simple_expr RightParen
    | bit_expr
    ;

string_val_list
    : STRING_VALUE+
    ;

bit_expr
    : INTERVAL expr date_unit Plus bit_expr
    | simple_expr
    | bit_expr (And|Caret|DIV|Div|MOD|Minus|Mod|Or|Plus|SHIFT_LEFT|SHIFT_RIGHT|Star) bit_expr
    | bit_expr (Minus|Plus) INTERVAL expr date_unit
    ;

simple_expr
    : simple_expr collation
    | column_ref
    | expr_const
    | simple_expr CNNOP simple_expr
    | (BINARY|Plus|Minus|Tilde|Not|NOT) simple_expr
    | ROW? LeftParen expr_list RightParen
    | EXISTS? select_with_parens
    | MATCH LeftParen column_list RightParen AGAINST LeftParen STRING_VALUE ((IN NATURAL LANGUAGE MODE) | (IN BOOLEAN MODE))? RightParen
    | case_expr
    | func_expr
    | window_function
    | LeftBrace relation_name expr RightBrace
    | USER_VARIABLE
    | column_definition_ref (JSON_EXTRACT|JSON_EXTRACT_UNQUOTED) complex_string_literal
    | relation_name Dot relation_name (Dot relation_name)? USER_VARIABLE
    ;

expr
    : (NOT|USER_VARIABLE SET_VAR) expr
    | LeftParen expr RightParen
    | bool_pri (IS not? (BOOL_VALUE|UNKNOWN))?
    | expr (AND|AND_OP|CNNOP|OR|XOR) expr
    ;

not
    : NOT
    ;


//sub_query_flag
//    : ALL
//    | ANY
//    | SOME
//    ;

in_expr
    : select_with_parens
    | LeftParen expr_list RightParen
    ;

case_expr
    : CASE expr? when_clause_list case_default? END
    ;

window_function
    : func_name=COUNT LeftParen ALL? (Star|expr) RightParen OVER new_generalized_window_clause
    | func_name=COUNT LeftParen DISTINCT expr_list RightParen OVER new_generalized_window_clause
    | func_name=(APPROX_COUNT_DISTINCT|APPROX_COUNT_DISTINCT_SYNOPSIS|NTILE) LeftParen expr_list RightParen OVER new_generalized_window_clause
    | func_name=(SUM|MAX|MIN|AVG|JSON_ARRAYAGG|APPROX_COUNT_DISTINCT_SYNOPSIS_MERGE) LeftParen (ALL | DISTINCT | UNIQUE)? expr RightParen OVER new_generalized_window_clause
    | func_name=JSON_OBJECTAGG LeftParen expr Comma expr RightParen OVER new_generalized_window_clause
    | func_name=(STD|STDDEV|VARIANCE|STDDEV_POP|STDDEV_SAMP|VAR_POP|VAR_SAMP|BIT_AND|BIT_OR|BIT_XOR) LeftParen ALL? expr RightParen OVER new_generalized_window_clause
    | func_name=(GROUP_CONCAT|LISTAGG) LeftParen (DISTINCT | UNIQUE)? expr_list order_by? (SEPARATOR STRING_VALUE)? RightParen OVER new_generalized_window_clause
    | func_name=(RANK|DENSE_RANK|PERCENT_RANK|ROW_NUMBER|CUME_DIST) LeftParen RightParen OVER new_generalized_window_clause
    | func_name=(FIRST_VALUE|LAST_VALUE|LEAD|LAG) win_fun_first_last_params OVER new_generalized_window_clause
    | func_name=NTH_VALUE LeftParen expr Comma expr RightParen (FROM first_or_last)? (respect_or_ignore NULLS)? OVER new_generalized_window_clause
    | func_name=TOP_K_FRE_HIST LeftParen bit_expr Comma bit_expr Comma bit_expr RightParen OVER new_generalized_window_clause
    | func_name=HYBRID_HIST LeftParen bit_expr Comma bit_expr RightParen OVER new_generalized_window_clause
    ;

first_or_last
    : FIRST
    | LAST
    ;

respect_or_ignore
    : RESPECT
    | IGNORE
    ;

win_fun_first_last_params
    : LeftParen expr respect_or_ignore NULLS RightParen
    | LeftParen expr RightParen (respect_or_ignore NULLS)?
    ;


new_generalized_window_clause
    : NAME_OB
    | new_generalized_window_clause_with_blanket
    ;

new_generalized_window_clause_with_blanket
    : LeftParen NAME_OB? generalized_window_clause RightParen
    ;

named_windows
    : named_window (Comma named_window)*
    ;

named_window
    : NAME_OB AS new_generalized_window_clause_with_blanket
    ;

generalized_window_clause
    : (PARTITION BY expr_list)? order_by? win_window?
    ;

win_rows_or_range
    : ROWS
    | RANGE
    ;

win_preceding_or_following
    : PRECEDING
    | FOLLOWING
    ;

win_interval
    : expr
    | INTERVAL expr date_unit
    ;

win_bounding
    : CURRENT ROW
    | win_interval win_preceding_or_following
    ;

win_window
    : win_rows_or_range BETWEEN win_bounding AND win_bounding
    | win_rows_or_range win_bounding
    ;


when_clause_list
    : when_clause+
    ;

when_clause
    : WHEN expr THEN expr
    ;

case_default
    : ELSE expr
    //| empty
    ;

func_expr
    : func_name=COUNT LeftParen ALL? (Star|expr) RightParen # simple_func_expr
    | func_name=COUNT LeftParen (DISTINCT|UNIQUE) expr_list RightParen # simple_func_expr
    | func_name=(APPROX_COUNT_DISTINCT|APPROX_COUNT_DISTINCT_SYNOPSIS|CHARACTER) LeftParen expr_list RightParen # simple_func_expr
    | func_name=(SUM|MAX|MIN|AVG|JSON_ARRAYAGG) LeftParen (ALL | DISTINCT | UNIQUE)? expr RightParen # simple_func_expr
    | func_name=(COUNT|STD|STDDEV|VARIANCE|STDDEV_POP|STDDEV_SAMP|VAR_POP|VAR_SAMP|BIT_AND|BIT_OR|BIT_XOR) LeftParen ALL? expr RightParen # simple_func_expr
    | func_name=(GROUPING|ISNULL|DATE|YEAR|TIME|MONTH|WEEK|DAY|TIMESTAMP) LeftParen expr RightParen # simple_func_expr
    | GROUP_CONCAT LeftParen (DISTINCT | UNIQUE)? expr_list order_by? (SEPARATOR STRING_VALUE)? RightParen # complex_func_expr
    | func_name=TOP_K_FRE_HIST LeftParen bit_expr Comma bit_expr Comma bit_expr RightParen # simple_func_expr
    | func_name=HYBRID_HIST LeftParen bit_expr Comma bit_expr RightParen # simple_func_expr
    | func_name=IF LeftParen expr Comma expr Comma expr RightParen # simple_func_expr
    | cur_timestamp_func # complex_func_expr
    | sysdate_func # complex_func_expr
    | cur_time_func # complex_func_expr
    | cur_date_func # complex_func_expr
    | utc_timestamp_func # complex_func_expr
    | utc_time_func # complex_func_expr
    | utc_date_func # complex_func_expr
    | CAST LeftParen expr AS cast_data_type RightParen # complex_func_expr
    | func_name=INSERT LeftParen expr Comma expr Comma expr Comma expr RightParen # simple_func_expr
    | CONVERT LeftParen expr Comma cast_data_type RightParen # complex_func_expr
    | CONVERT LeftParen expr USING charset_name RightParen # complex_func_expr
    | POSITION LeftParen bit_expr IN expr RightParen # complex_func_expr
    | substr_or_substring LeftParen substr_params RightParen # complex_func_expr
    | TRIM LeftParen parameterized_trim RightParen # complex_func_expr
    | func_name=(ADDDATE|SUBDATE|LEFT|TIMESTAMP|WEEK|LOG|MOD|JSON_OBJECTAGG) LeftParen expr Comma expr RightParen # simple_func_expr
    | func_name=(QUARTER|SECOND|MINUTE|MICROSECOND|HOUR|ASCII|LOG|LN|APPROX_COUNT_DISTINCT_SYNOPSIS_MERGE) LeftParen expr RightParen # simple_func_expr
    | GET_FORMAT LeftParen get_format_unit Comma expr RightParen # complex_func_expr
    | (DATE_ADD|DATE_SUB|ADDDATE|SUBDATE) LeftParen date_params RightParen # complex_func_expr
    | func_name=SUBDATE LeftParen expr Comma expr RightParen # simple_func_expr
    | (TIMESTAMPDIFF|TIMESTAMPADD) LeftParen timestamp_params RightParen # complex_func_expr
    | EXTRACT LeftParen date_unit FROM expr RightParen # complex_func_expr
    | func_name=(DEFAULT|VALUES) LeftParen column_definition_ref RightParen # simple_func_expr
    | CHARACTER LeftParen expr_list USING charset_name RightParen # complex_func_expr
    | function_name LeftParen expr_as_list? RightParen # simple_func_expr
    | relation_name Dot function_name LeftParen expr_as_list? RightParen # simple_func_expr
    | sys_interval_func # complex_func_expr
    | func_name=CALC_PARTITION_ID LeftParen bit_expr Comma bit_expr RightParen # simple_func_expr
    | func_name=CALC_PARTITION_ID LeftParen bit_expr Comma bit_expr Comma bit_expr RightParen # simple_func_expr
    | WEIGHT_STRING LeftParen expr (AS CHARACTER ws_nweights)? (LEVEL ws_level_list_or_range)? RightParen # complex_func_expr
    | WEIGHT_STRING LeftParen expr AS BINARY ws_nweights RightParen # complex_func_expr
    | WEIGHT_STRING LeftParen expr Comma INTNUM Comma INTNUM Comma INTNUM Comma INTNUM RightParen # complex_func_expr
    | json_value_expr # complex_func_expr
    | func_name=POINT LeftParen expr Comma expr RightParen # simple_func_expr
    | func_name=LINESTRING LeftParen expr_list RightParen # simple_func_expr
    | func_name=MULTIPOINT LeftParen expr_list RightParen # simple_func_expr
    | func_name=MULTILINESTRING LeftParen expr_list RightParen # simple_func_expr
    | func_name=POLYGON LeftParen expr_list RightParen # simple_func_expr
    | func_name=MULTIPOLYGON LeftParen expr_list RightParen # simple_func_expr
    | func_name=GEOMETRYCOLLECTION LeftParen expr_list? RightParen # simple_func_expr
    | func_name=GEOMCOLLECTION LeftParen expr_list? RightParen # simple_func_expr
    | func_name=ST_ASMVT LeftParen column_ref RightParen # simple_func_expr
    | func_name=ST_ASMVT LeftParen column_ref Comma mvt_param RightParen # simple_func_expr
    | func_name=ST_ASMVT LeftParen column_ref Comma mvt_param Comma mvt_param RightParen # simple_func_expr
    | func_name=ST_ASMVT LeftParen column_ref Comma mvt_param Comma mvt_param Comma mvt_param RightParen # simple_func_expr
    | func_name=ST_ASMVT LeftParen column_ref Comma mvt_param Comma mvt_param Comma mvt_param Comma mvt_param RightParen # simple_func_expr
    ;

mvt_param
    : STRING_VALUE
    | Minus? INTNUM
    | NULLX
    | NAME_OB
    | unreserved_keyword
    ;

sys_interval_func
    : INTERVAL LeftParen expr (Comma expr)+ RightParen
    | CHECK LeftParen expr RightParen
    ;

utc_timestamp_func
    : UTC_TIMESTAMP (LeftParen INTNUM? RightParen)?
    ;

utc_time_func
    : UTC_TIME (LeftParen INTNUM? RightParen)?
    ;

utc_date_func
    : UTC_DATE (LeftParen RightParen)?
    ;

sysdate_func
    : SYSDATE LeftParen INTNUM? RightParen
    ;

cur_timestamp_func
    : (now_synonyms_func|NOW) (LeftParen INTNUM? RightParen)?
    ;

now_synonyms_func
    : CURRENT_TIMESTAMP
    | LOCALTIME
    | LOCALTIMESTAMP
    ;

cur_time_func
    : (CURTIME|CURTIME|CURRENT_TIME) LeftParen INTNUM? RightParen
    ;

cur_date_func
    : (CURDATE|CURRENT_DATE) LeftParen RightParen
    | CURRENT_DATE
    ;

substr_or_substring
    : SUBSTR
    | SUBSTRING
    ;

substr_params
    : expr Comma expr (Comma expr)?
    | expr FROM expr (FOR expr)?
    ;

date_params
    : expr Comma INTERVAL expr date_unit
    ;

timestamp_params
    : date_unit Comma expr Comma expr
    ;

ws_level_list_or_range
    : ws_level_list
    | ws_level_range
    ;

ws_level_list
    : ws_level_list_item (Comma ws_level_list_item)*
    ;

ws_level_list_item
    : ws_level_number ws_level_flags
    ;

ws_level_range
    : ws_level_number Minus ws_level_number
    ;

ws_level_number
    : INTNUM
    ;

ws_level_flags
    : empty
    | ws_level_flag_desc ws_level_flag_reverse?
    | ws_level_flag_reverse
    ;

ws_nweights
    : LeftParen INTNUM RightParen
    ;

ws_level_flag_desc
    : ASC
    | DESC
    ;

ws_level_flag_reverse
    : REVERSE
    ;

delete_stmt
    : with_clause? delete_basic_stmt
    ;

delete_basic_stmt
    : delete_with_opt_hint FROM tbl_name (WHERE opt_hint_value expr)? order_by? limit_clause?
    | delete_with_opt_hint multi_delete_table (WHERE opt_hint_value expr)?
    ;

multi_delete_table
    : relation_with_star_list FROM table_references
    | FROM relation_with_star_list USING table_references
    ;

update_stmt
    : with_clause? update_basic_stmt
    ;

update_basic_stmt
    : update_with_opt_hint IGNORE? table_references SET update_asgn_list (WHERE opt_hint_value expr)? order_by? limit_clause?
    ;

update_asgn_list
    : update_asgn_factor (Comma update_asgn_factor)*
    ;

update_asgn_factor
    : column_definition_ref COMP_EQ expr_or_default
    ;

create_resource_stmt
    : CREATE RESOURCE UNIT (IF not EXISTS)? relation_name (resource_unit_option | (opt_resource_unit_option_list Comma resource_unit_option))?
    | CREATE RESOURCE POOL (IF not EXISTS)? relation_name (create_resource_pool_option | (opt_create_resource_pool_option_list Comma create_resource_pool_option))?
    ;

opt_resource_unit_option_list
    : resource_unit_option
    | empty
    | opt_resource_unit_option_list Comma resource_unit_option
    ;

resource_unit_option
    : (MIN_CPU|MIN_IOPS|MIN_MEMORY|MAX_CPU|MAX_MEMORY|MAX_IOPS|MAX_DISK_SIZE|MAX_SESSION_NUM|MEMORY_SIZE|IOPS_WEIGHT|LOG_DISK_SIZE) COMP_EQ? conf_const
    ;

opt_create_resource_pool_option_list
    : create_resource_pool_option
    | empty
    | create_resource_pool_option (Comma create_resource_pool_option)*
    ;

create_resource_pool_option
    : UNIT COMP_EQ? relation_name_or_string
    | UNIT_NUM COMP_EQ? INTNUM
    | ZONE_LIST COMP_EQ? LeftParen zone_list RightParen
    | REPLICA_TYPE COMP_EQ? STRING_VALUE
    ;

alter_resource_pool_option_list
    : alter_resource_pool_option (Comma alter_resource_pool_option)*
    ;

unit_id_list
    : INTNUM (Comma INTNUM)*
    ;

id_list
    : INTNUM (Comma INTNUM)*
    ;

alter_resource_pool_option
    : UNIT COMP_EQ? relation_name_or_string
    | UNIT_NUM COMP_EQ? INTNUM (DELETE UNIT opt_equal_mark LeftParen unit_id_list RightParen)?
    | ZONE_LIST COMP_EQ? LeftParen zone_list RightParen
    ;

alter_resource_stmt
    : ALTER RESOURCE UNIT relation_name (resource_unit_option | (opt_resource_unit_option_list Comma resource_unit_option))?
    | ALTER RESOURCE POOL relation_name alter_resource_pool_option_list
    | ALTER RESOURCE POOL relation_name SPLIT INTO LeftParen resource_pool_list RightParen ON LeftParen zone_list RightParen
    | ALTER RESOURCE POOL MERGE LeftParen resource_pool_list RightParen INTO LeftParen resource_pool_list RightParen
    | ALTER RESOURCE TENANT relation_name UNIT_NUM opt_equal_mark INTNUM (DELETE UNIT_GROUP opt_equal_mark LeftParen id_list RightParen)?
    ;

drop_resource_stmt
    : DROP RESOURCE (UNIT|POOL) (IF EXISTS)? relation_name
    ;

create_tenant_stmt
    : CREATE TENANT (IF not EXISTS)? relation_name (tenant_option | (opt_tenant_option_list Comma tenant_option))? ((SET sys_var_and_val_list) | (SET VARIABLES sys_var_and_val_list) | (VARIABLES sys_var_and_val_list))?
    ;

create_standby_tenant_stmt
    : CREATE STANDBY TENANT (IF not EXISTS)? relation_name log_restore_source_option? (tenant_option | (opt_tenant_option_list Comma tenant_option))?
    ;

log_restore_source_option
    : LOG_RESTORE_SOURCE COMP_EQ? conf_const
    ;

opt_tenant_option_list
    : tenant_option
    | empty
    | opt_tenant_option_list Comma tenant_option
    ;

tenant_option
    : (LOGONLY_REPLICA_NUM|REPLICA_NUM|REWRITE_MERGE_VERSION|STORAGE_FORMAT_VERSION|STORAGE_FORMAT_WORK_VERSION|PROGRESSIVE_MERGE_NUM) COMP_EQ? INTNUM
    | LOCALITY COMP_EQ? STRING_VALUE FORCE?
    | PRIMARY_ZONE COMP_EQ? primary_zone_name
    | RESOURCE_POOL_LIST COMP_EQ? LeftParen resource_pool_list RightParen
    | ENABLE_ARBITRATION_SERVICE COMP_EQ? BOOL_VALUE
    | ZONE_LIST COMP_EQ? LeftParen zone_list RightParen
    | charset_key COMP_EQ? charset_name
    | COLLATE COMP_EQ? collation_name
    | read_only_or_write
    | COMMENT COMP_EQ? STRING_VALUE
    | default_tablegroup
    | ENABLE_EXTENDED_ROWID COMP_EQ? BOOL_VALUE
    ;

zone_list
    : STRING_VALUE (Comma? STRING_VALUE)*
    ;

resource_pool_list
    : STRING_VALUE (Comma STRING_VALUE)*
    ;

alter_tenant_stmt
    : ALTER TENANT relation_name SET? (tenant_option | (opt_tenant_option_list Comma tenant_option))? (VARIABLES sys_var_and_val_list)?
    | ALTER TENANT ALL SET? (tenant_option | (opt_tenant_option_list Comma tenant_option))? (VARIABLES sys_var_and_val_list)?
    | ALTER TENANT relation_name RENAME GLOBAL_NAME TO relation_name
    | ALTER TENANT relation_name lock_spec_mysql57
    ;

create_tenant_snapshot_stmt
    : CREATE SNAPSHOT snapshot_name FOR TENANT relation_name
    | CREATE SNAPSHOT snapshot_name
    ;

snapshot_name
    : relation_name?
    ;

drop_tenant_snapshot_stmt
    : DROP SNAPSHOT relation_name FOR TENANT relation_name
    | DROP SNAPSHOT relation_name
    ;

clone_tenant_stmt
    : CREATE TENANT (IF not EXISTS)? relation_name FROM relation_name clone_snapshot_option WITH clone_tenant_option_list
    ;

clone_snapshot_option
    : USING SNAPSHOT relation_name
    | empty
    ;

clone_tenant_option
    : RESOURCE_POOL opt_equal_mark relation_name_or_string
    | UNIT opt_equal_mark relation_name_or_string
    ;

clone_tenant_option_list
    : clone_tenant_option Comma clone_tenant_option
    ;

drop_tenant_stmt
    : DROP TENANT (IF EXISTS)? relation_name (FORCE | PURGE)?
    ;

create_restore_point_stmt
    : CREATE RESTORE POINT relation_name
    ;

drop_restore_point_stmt
    : DROP RESTORE POINT relation_name
    ;

create_database_stmt
    : CREATE database_key (IF not EXISTS)? database_factor database_option_list?
    ;

database_key
    : DATABASE
    | SCHEMA
    ;

database_factor
    : relation_name
    ;

database_option_list
    : database_option+
    ;

databases_expr
    : DATABASES COMP_EQ? STRING_VALUE
    ;

charset_key
    : CHARSET
    | CHARACTER SET
    ;

database_option
    : DEFAULT? charset_key COMP_EQ? charset_name
    | DEFAULT? COLLATE COMP_EQ? collation_name
    | REPLICA_NUM COMP_EQ? INTNUM
    | PRIMARY_ZONE COMP_EQ? primary_zone_name
    | read_only_or_write
    | default_tablegroup
    | DATABASE_ID COMP_EQ? INTNUM
    ;

read_only_or_write
    : READ ONLY
    | READ WRITE
    ;

drop_database_stmt
    : DROP database_key (IF EXISTS)? database_factor
    ;

alter_database_stmt
    : ALTER database_key NAME_OB? SET? database_option_list
    ;

load_data_stmt
    : load_data_with_opt_hint (LOCAL | REMOTE_OSS)? INFILE STRING_VALUE (IGNORE | REPLACE)? INTO TABLE relation_factor use_partition? (CHARACTER SET charset_name_or_default)? field_opt line_opt ((IGNORE INTNUM lines_or_rows) | (GENERATED INTNUM lines_or_rows))? ((LeftParen RightParen) | (LeftParen field_or_vars_list RightParen))? (SET load_set_list)? load_data_extended_option_list?
    ;

load_data_with_opt_hint
    : LOAD DATA
    | LOAD_DATA_HINT_BEGIN hint_list_with_end
    ;

lines_or_rows
    : LINES
    | ROWS
    ;

field_or_vars_list
    : field_or_vars (Comma field_or_vars)*
    ;

field_or_vars
    : column_definition_ref
    | USER_VARIABLE
    ;

load_set_list
    : load_set_element (Comma load_set_element)*
    ;

load_set_element
    : column_definition_ref COMP_EQ expr_or_default
    ;

load_data_extended_option_list
    : load_data_extended_option load_data_extended_option_list?
    ;

load_data_extended_option
    : LOGFILE COMP_EQ? STRING_VALUE
    | REJECT LIMIT COMP_EQ? INTNUM
    | BADFILE COMP_EQ? STRING_VALUE
    ;

use_database_stmt
    : USE database_factor
    ;

special_table_type
    : (TEMPORARY | EXTERNAL)?
    ;

create_table_like_stmt
    : CREATE special_table_type TABLE (IF not EXISTS)? relation_factor LIKE relation_factor
    | CREATE special_table_type TABLE (IF not EXISTS)? relation_factor LeftParen LIKE relation_factor RightParen
    ;

create_table_stmt
    : CREATE special_table_type TABLE (IF not EXISTS)? relation_factor LeftParen table_element_list RightParen table_option_list? (partition_option | auto_partition_option)? with_column_group?
    | CREATE special_table_type TABLE (IF not EXISTS)? relation_factor LeftParen table_element_list RightParen table_option_list? (partition_option | auto_partition_option)? with_column_group? AS? select_stmt
    | CREATE special_table_type TABLE (IF not EXISTS)? relation_factor table_option_list (partition_option | auto_partition_option)? with_column_group? AS? select_stmt
    | CREATE special_table_type TABLE (IF not EXISTS)? relation_factor partition_option with_column_group? AS? select_stmt
    | CREATE special_table_type TABLE (IF not EXISTS)? relation_factor with_column_group? AS? select_stmt
    ;

ret_type
    : STRING
    | INTEGER
    | REAL
    | DECIMAL
    | FIXED
    | NUMERIC
    ;

create_function_stmt
    : CREATE AGGREGATE? FUNCTION NAME_OB RETURNS ret_type SONAME STRING_VALUE
    ;

drop_function_stmt
    : DROP FUNCTION (IF EXISTS)? relation_factor
    ;

drop_procedure_stmt
    : DROP PROCEDURE (IF EXISTS)? relation_factor
    ;

drop_trigger_stmt
    : DROP TRIGGER (IF EXISTS)? relation_factor
    ;

table_element_list
    : table_element (Comma table_element)*
    ;

table_element
    : column_definition
    | out_of_line_index
    | out_of_line_constraint
    ;

out_of_line_constraint
    : (CONSTRAINT opt_constraint_name)? out_of_line_primary_index
    | (CONSTRAINT opt_constraint_name)? out_of_line_unique_index
    | (CONSTRAINT opt_constraint_name)? CHECK LeftParen expr RightParen check_state?
    | (CONSTRAINT opt_constraint_name)? FOREIGN KEY index_name? LeftParen column_name_list RightParen references_clause
    ;

references_clause
    : REFERENCES relation_factor LeftParen column_name_list RightParen (MATCH match_action)? (opt_reference_option_list reference_option)?
    ;

out_of_line_index
    : key_or_index index_name? index_using_algorithm? LeftParen sort_column_list RightParen opt_index_options? (partition_option | auto_partition_option)? with_column_group?
    | (FULLTEXT | SPATIAL) key_or_index? index_name? index_using_algorithm? LeftParen sort_column_list RightParen opt_index_options? (partition_option | auto_partition_option)? with_column_group?
    ;

out_of_line_primary_index
    : PRIMARY KEY index_name? index_using_algorithm? LeftParen column_name_list RightParen opt_index_options?
    ;

out_of_line_unique_index
    : UNIQUE key_or_index? index_name? index_using_algorithm? LeftParen sort_column_list RightParen opt_index_options? (partition_option | auto_partition_option)? with_column_group?
    ;

opt_reference_option_list
    : opt_reference_option_list reference_option
    | empty
    ;

reference_option
    : ON (DELETE|UPDATE) reference_action
    ;

reference_action
    : RESTRICT
    | CASCADE
    | SET NULLX
    | NO ACTION
    | SET DEFAULT
    ;

match_action
    : SIMPLE
    | FULL
    | PARTIAL
    ;

column_definition
    : column_definition_ref data_type opt_column_attribute_list? (FIRST | (BEFORE column_name) | (AFTER column_name))?
    | column_definition_ref data_type (GENERATED opt_generated_option_list)? AS LeftParen expr RightParen (VIRTUAL | STORED)? opt_generated_column_attribute_list? (FIRST | (BEFORE column_name) | (AFTER column_name))?
    ;

opt_generated_option_list
    : ALWAYS
    ;

opt_generated_column_attribute_list
    : opt_generated_column_attribute_list generated_column_attribute
    | generated_column_attribute
    ;

generated_column_attribute
    : NOT NULLX
    | NULLX
    | UNIQUE KEY
    | PRIMARY? KEY
    | UNIQUE
    | COMMENT STRING_VALUE
    | ID INTNUM
    | (CONSTRAINT opt_constraint_name)? CHECK LeftParen expr RightParen check_state?
    | SRID INTNUM
    ;

column_definition_ref
    : ((relation_name Dot)? relation_name Dot)?  column_name
    ;

column_definition_list
    : column_definition (Comma column_definition)*
    ;

cast_data_type
    : binary_type_i[true]
    | character_type_i[true]
    | datetime_type_i[true]
    | date_year_type_i
    | float_type_i[true]
    | number_type_i[true]
    | json_type_i
    | geo_type_i
    | (SIGNED|UNSIGNED) INTEGER?
    ;

get_format_unit
    : DATETIME
    | TIMESTAMP
    | DATE
    | TIME
    ;

precision_int_num [int max_precision_count]
    locals [int precision_count=1]
    : LeftParen INTNUM ({$max_precision_count>$precision_count}? Comma INTNUM {$precision_count++;})* RightParen
    ;

precision_decimal_num
    : LeftParen DECIMAL_VAL RightParen
    ;

data_type_precision [int max_int_precision_count]
    : precision_int_num[max_int_precision_count]
    | precision_decimal_num
    ;

data_type
    : int_type_i
    | float_type_i[false]
    | number_type_i[false]
    | bool_type_i
    | datetime_type_i[false]
    | date_year_type_i
    | text_type_i
    | character_type_i[false]
    | blob_type_i
    | binary_type_i[false]
    | bit_type_i
    | json_type_i
    | collection_type_i
    | geo_type_i
    | STRING_VALUE
    ;

string_list
    : text_string (Comma text_string)*
    ;

text_string
    : STRING_VALUE
    | PARSER_SYNTAX_ERROR
    ;

collection_type_i
    : ENUM LeftParen string_list RightParen BINARY? (charset_key charset_name)? collation?
    | SET LeftParen string_list RightParen BINARY? (charset_key charset_name)? collation?
    ;

json_type_i
    : JSON
    ;

bit_type_i
    : BIT precision_int_num[1]?
    ;

int_type_i
    : (TINYINT | SMALLINT | MEDIUMINT | INTEGER | BIGINT) precision_int_num[1]? (UNSIGNED | SIGNED)? ZEROFILL?
    ;

float_type_i [boolean in_cast_data_type]
    : {!$in_cast_data_type}? (FLOAT | DOUBLE PRECISION? | REAL PRECISION?) data_type_precision[2]? (UNSIGNED | SIGNED)? ZEROFILL?
    | {$in_cast_data_type}? FLOAT precision_int_num[1]?
    | {$in_cast_data_type}? DOUBLE
    ;

number_type_i [boolean in_cast_data_type]
    : {$in_cast_data_type}? (NUMBER | DECIMAL | FIXED) precision_int_num[2]?
    | {!$in_cast_data_type}? (NUMBER | DECIMAL | FIXED) precision_int_num[2]? (UNSIGNED | SIGNED)? ZEROFILL?
    ;

text_type_i
    : (TINYTEXT | TEXT | MEDIUMTEXT VARCHAR? | LONGTEXT) string_length_i? BINARY? (charset_key charset_name)? collation?
    ;

character_type_i [boolean in_cast_data_type]
    : {!$in_cast_data_type}? CHARACTER string_length_i? BINARY? (charset_key charset_name)? collation?
    | {$in_cast_data_type}? CHARACTER string_length_i? BINARY?
    | {$in_cast_data_type}? CHARACTER string_length_i? charset_key charset_name
    | {!$in_cast_data_type}? NCHAR string_length_i? BINARY?
    | {!$in_cast_data_type}? NATIONAL CHARACTER string_length_i? BINARY?
    | {!$in_cast_data_type}? VARCHAR string_length_i BINARY? (charset_key charset_name)? collation?
    | {!$in_cast_data_type}? NCHAR VARCHAR string_length_i BINARY?
    | {!$in_cast_data_type}? NVARCHAR string_length_i BINARY?
    | {!$in_cast_data_type}? NATIONAL VARCHAR string_length_i BINARY?
    | {!$in_cast_data_type}? CHARACTER VARYING string_length_i BINARY? (charset_key charset_name)?
    | {!$in_cast_data_type}? NATIONAL CHARACTER VARYING string_length_i BINARY?
    ;

bool_type_i
    : BOOL
    | BOOLEAN
    ;

geo_type_i
    : POINT
    | GEOMETRY
    | LINESTRING
    | POLYGON
    | MULTIPOINT
    | MULTILINESTRING
    | MULTIPOLYGON
    | GEOMETRYCOLLECTION
    | GEOMCOLLECTION
    ;

datetime_type_i [boolean in_cast_data_type]
    : (DATETIME | TIME) precision_int_num[1]?
    | {!$in_cast_data_type}? TIMESTAMP precision_int_num[1]?
    ;

date_year_type_i
    : DATE
    | YEAR precision_int_num[1]?
    ;

blob_type_i
    : (TINYBLOB | BLOB | MEDIUMBLOB | LONGBLOB) string_length_i?
    ;

binary_type_i [boolean in_cast_data_type]
    : BINARY string_length_i?
    | {!$in_cast_data_type}? VARBINARY string_length_i
    ;

string_length_i
    : LeftParen number_literal RightParen
    ;

collation_name
    : NAME_OB
    | STRING_VALUE
    ;

trans_param_name
    : Quote STRING_VALUE Quote
    ;

trans_param_value
    : Quote STRING_VALUE Quote
    | INTNUM
    ;

charset_name
    : NAME_OB
    | STRING_VALUE
    | BINARY
    ;

charset_name_or_default
    : charset_name
    | DEFAULT
    ;

collation
    : COLLATE collation_name
    ;

opt_column_attribute_list
    : opt_column_attribute_list column_attribute
    | column_attribute
    ;

column_attribute
    : not NULLX
    | NULLX
    | DEFAULT now_or_signed_literal
    | ORIG_DEFAULT now_or_signed_literal
    | AUTO_INCREMENT
    | UNIQUE KEY
    | PRIMARY? KEY
    | UNIQUE
    | COMMENT STRING_VALUE
    | ON UPDATE cur_timestamp_func
    | ID INTNUM
    | (CONSTRAINT opt_constraint_name)? CHECK LeftParen expr RightParen check_state?
    | SRID INTNUM
    | COLLATE collation_name
    | SKIP_INDEX LeftParen (skip_index_type | (opt_skip_index_type_list Comma skip_index_type))? RightParen
    ;

now_or_signed_literal
    : cur_timestamp_func
    | signed_literal
    ;

signed_literal
    : literal
    | Plus number_literal
    | Minus number_literal
    ;

table_option_list_space_seperated
    : table_option+
    ;

table_option_list
    : table_option_list_space_seperated
    | table_option Comma table_option_list
    ;

primary_zone_name
    : DEFAULT
    | RANDOM
    | USER_VARIABLE
    | relation_name_or_string
    ;

tablespace
    : NAME_OB
    ;

locality_name
    : STRING_VALUE
    | DEFAULT
    ;

table_option
    : SORTKEY LeftParen column_name_list RightParen
    | (TABLE_MODE|DUPLICATE_SCOPE|COMMENT|COMPRESSION) COMP_EQ? STRING_VALUE
    | LOCALITY COMP_EQ? locality_name FORCE?
    | EXPIRE_INFO COMP_EQ? LeftParen expr RightParen
    | (PROGRESSIVE_MERGE_NUM|BLOCK_SIZE|TABLE_ID|REPLICA_NUM
        |STORAGE_FORMAT_VERSION|TABLET_SIZE|PCTFREE|MAX_USED_PART_ID) COMP_EQ? INTNUM
    | ROW_FORMAT COMP_EQ? row_format_option
    | USE_BLOOM_FILTER COMP_EQ? BOOL_VALUE
    | DEFAULT? charset_key COMP_EQ? charset_name
    | DEFAULT? COLLATE COMP_EQ? collation_name
    | PRIMARY_ZONE COMP_EQ? primary_zone_name
    | (TABLEGROUP|ENGINE_) COMP_EQ? relation_name_or_string
    | AUTO_INCREMENT COMP_EQ? int_or_decimal
    | read_only_or_write
    | TABLESPACE tablespace
    | parallel_option
    | DELAY_KEY_WRITE COMP_EQ? INTNUM
    | AVG_ROW_LENGTH COMP_EQ? INTNUM
    | CHECKSUM COMP_EQ? INTNUM
    | AUTO_INCREMENT_MODE COMP_EQ? STRING_VALUE
    | ENABLE_EXTENDED_ROWID COMP_EQ? BOOL_VALUE
    | LOCATION COMP_EQ? STRING_VALUE
    | FORMAT COMP_EQ? LeftParen external_file_format_list RightParen
    | PATTERN COMP_EQ? STRING_VALUE
    | TTL LeftParen ttl_definition RightParen
    | KV_ATTRIBUTES COMP_EQ? STRING_VALUE
    | DEFAULT_LOB_INROW_THRESHOLD COMP_EQ? INTNUM
    | LOB_INROW_THRESHOLD COMP_EQ? INTNUM
    ;

parallel_option
    : PARALLEL COMP_EQ? INTNUM
    | NOPARALLEL
    ;

ttl_definition
    : ttl_expr (Comma ttl_expr)*
    ;

ttl_expr
    : column_definition_ref Plus INTERVAL INTNUM ttl_unit
    ;

ttl_unit
    : SECOND
    | MINUTE
    | HOUR
    | DAY
    | MONTH
    | YEAR
    ;

relation_name_or_string
    : relation_name
    | STRING_VALUE
    | ALL
    ;

opt_equal_mark
    : COMP_EQ?
    ;

partition_option
    : hash_partition_option
    | key_partition_option
    | range_partition_option
    | list_partition_option
    ;

auto_partition_option
    : auto_partition_type PARTITION SIZE partition_size PARTITIONS AUTO
    ;

column_group_element
    : ALL COLUMNS
    | EACH COLUMN
    | relation_name LeftParen column_name_list RightParen
    ;

column_group_list
    : column_group_element (Comma column_group_element)*
    ;

with_column_group
    : WITH_COLUMN_GROUP LeftParen column_group_list RightParen
    ;

partition_size
    : conf_const
    | AUTO
    ;

auto_partition_type
    : auto_range_type
    ;

auto_range_type
    : PARTITION BY RANGE LeftParen expr? RightParen
    | PARTITION BY RANGE COLUMNS LeftParen column_name_list RightParen
    ;

hash_partition_option
    : PARTITION BY HASH LeftParen expr RightParen partition_options? opt_hash_partition_list?
    ;

list_partition_option
    : PARTITION BY BISON_LIST LeftParen expr RightParen partition_options? opt_list_partition_list
    | PARTITION BY BISON_LIST COLUMNS LeftParen column_name_list RightParen partition_options? opt_list_partition_list
    ;

key_partition_option
    : PARTITION BY KEY LeftParen column_name_list? RightParen partition_options? opt_hash_partition_list?
    ;

range_partition_option
    : PARTITION BY RANGE LeftParen expr RightParen partition_options? opt_range_partition_list
    | PARTITION BY RANGE COLUMNS LeftParen column_name_list RightParen partition_options? opt_range_partition_list
    ;

partition_options
    : partition_num? subpartition_option
    | subpartition_option? partition_num
    ;

partition_num
    : PARTITIONS INTNUM
    ;

aux_column_list
    : vertical_column_name (Comma vertical_column_name)*
    ;

vertical_column_name
    : column_name
    | LeftParen column_name_list RightParen
    ;

column_name_list
    : column_name (Comma column_name)*
    ;

subpartition_option
    : subpartition_template_option
    | subpartition_individual_option
    ;

subpartition_template_option
    : SUBPARTITION BY RANGE LeftParen expr RightParen SUBPARTITION TEMPLATE opt_range_subpartition_list
    | SUBPARTITION BY RANGE COLUMNS LeftParen column_name_list RightParen SUBPARTITION TEMPLATE opt_range_subpartition_list
    | SUBPARTITION BY HASH LeftParen expr RightParen SUBPARTITION TEMPLATE opt_hash_subpartition_list
    | SUBPARTITION BY BISON_LIST LeftParen expr RightParen SUBPARTITION TEMPLATE opt_list_subpartition_list
    | SUBPARTITION BY BISON_LIST COLUMNS LeftParen column_name_list RightParen SUBPARTITION TEMPLATE opt_list_subpartition_list
    | SUBPARTITION BY KEY LeftParen column_name_list RightParen SUBPARTITION TEMPLATE opt_hash_subpartition_list
    ;

subpartition_individual_option
    : SUBPARTITION BY (RANGE|BISON_LIST) LeftParen expr RightParen
    | SUBPARTITION BY (RANGE|BISON_LIST) COLUMNS LeftParen column_name_list RightParen
    | SUBPARTITION BY HASH LeftParen expr RightParen (SUBPARTITIONS INTNUM)?
    | SUBPARTITION BY KEY LeftParen column_name_list RightParen (SUBPARTITIONS INTNUM)?
    ;

opt_hash_partition_list
    : LeftParen hash_partition_list RightParen
    ;

hash_partition_list
    : hash_partition_element (Comma hash_partition_element)*
    ;

subpartition_list
    : opt_hash_subpartition_list
    | opt_range_subpartition_list
    | opt_list_subpartition_list
    ;

hash_partition_element
    : PARTITION relation_factor (ID INTNUM)? partition_attributes_option? subpartition_list?
    ;

opt_range_partition_list
    : LeftParen range_partition_list RightParen
    ;

range_partition_list
    : range_partition_element (Comma range_partition_element)*
    ;

range_partition_element
    : PARTITION relation_factor VALUES LESS THAN range_partition_expr (ID INTNUM)? partition_attributes_option? subpartition_list?
    ;

opt_list_partition_list
    : LeftParen list_partition_list RightParen
    ;

list_partition_list
    : list_partition_element (Comma list_partition_element)*
    ;

list_partition_element
    : PARTITION relation_factor VALUES IN list_partition_expr (ID INTNUM)? partition_attributes_option? subpartition_list?
    ;

opt_hash_subpartition_list
    : LeftParen hash_subpartition_list RightParen
    ;

hash_subpartition_list
    : hash_subpartition_element (Comma hash_subpartition_element)*
    ;

partition_attributes_option
    : ENGINE_ COMP_EQ INNODB
    ;

hash_subpartition_element
    : SUBPARTITION relation_factor partition_attributes_option?
    ;

opt_range_subpartition_list
    : LeftParen range_subpartition_list RightParen
    ;

range_subpartition_list
    : range_subpartition_element (Comma range_subpartition_element)*
    ;

range_subpartition_element
    : SUBPARTITION relation_factor VALUES LESS THAN range_partition_expr partition_attributes_option?
    ;

opt_list_subpartition_list
    : LeftParen list_subpartition_list RightParen
    ;

list_subpartition_list
    : list_subpartition_element (Comma list_subpartition_element)*
    ;

list_subpartition_element
    : SUBPARTITION relation_factor VALUES IN list_partition_expr partition_attributes_option?
    ;

list_partition_expr
    : LeftParen (DEFAULT|list_expr) RightParen
    ;

list_expr
    : expr (Comma expr)*
    ;

range_partition_expr
    : LeftParen range_expr_list RightParen
    | MAXVALUE
    ;

range_expr_list
    : range_expr (Comma range_expr)*
    ;

range_expr
    : expr
    | MAXVALUE
    ;

int_or_decimal
    : INTNUM
    | DECIMAL_VAL
    ;

tg_hash_partition_option
    : PARTITION BY HASH tg_subpartition_option (PARTITIONS INTNUM)?
    ;

tg_key_partition_option
    : PARTITION BY KEY INTNUM tg_subpartition_option (PARTITIONS INTNUM)?
    ;

tg_range_partition_option
    : PARTITION BY RANGE (COLUMNS INTNUM)? tg_subpartition_option (PARTITIONS INTNUM)? opt_range_partition_list
    ;

tg_list_partition_option
    : PARTITION BY BISON_LIST (COLUMNS INTNUM)? tg_subpartition_option (PARTITIONS INTNUM)? opt_list_partition_list
    ;

tg_subpartition_option
    : tg_subpartition_template_option
    | tg_subpartition_individual_option
    ;

tg_subpartition_template_option
    : SUBPARTITION BY RANGE (COLUMNS INTNUM)? SUBPARTITION TEMPLATE opt_range_subpartition_list
    | SUBPARTITION BY BISON_LIST (COLUMNS INTNUM)? SUBPARTITION TEMPLATE opt_list_subpartition_list
    | empty
    ;

tg_subpartition_individual_option
    : SUBPARTITION BY (HASH|KEY INTNUM) (SUBPARTITIONS INTNUM)?
    | SUBPARTITION BY RANGE (COLUMNS INTNUM)?
    | SUBPARTITION BY BISON_LIST (COLUMNS INTNUM)?
    ;

row_format_option
    : REDUNDANT
    | COMPACT
    | DYNAMIC
    | COMPRESSED
    | CONDENSED
    | DEFAULT
    ;

external_file_format_list
    : external_file_format (Comma? external_file_format)*
    ;

external_file_format
    : format_key=(ENCODING|TYPE) COMP_EQ STRING_VALUE
    | format_key=(ESCAPE|FIELD_OPTIONALLY_ENCLOSED_BY|FIELD_DELIMITER|LINE_DELIMITER) COMP_EQ expr
    | format_key=SKIP_HEADER COMP_EQ INTNUM
    | format_key=(SKIP_BLANK_LINES|TRIM_SPACE|EMPTY_FIELD_AS_NULL) COMP_EQ BOOL_VALUE
    | format_key=NULL_IF_EXETERNAL COMP_EQ LeftParen expr_list RightParen
    ;

create_tablegroup_stmt
    : CREATE TABLEGROUP (IF not EXISTS)? relation_name tablegroup_option_list? (tg_hash_partition_option | tg_key_partition_option | tg_range_partition_option | tg_list_partition_option)?
    ;

drop_tablegroup_stmt
    : DROP TABLEGROUP (IF EXISTS)? relation_name
    ;

alter_tablegroup_stmt
    : ALTER TABLEGROUP relation_name ADD TABLE? table_list
    | ALTER TABLEGROUP relation_name alter_tablegroup_actions
    | ALTER TABLEGROUP relation_name alter_tg_partition_option
    ;

tablegroup_option_list_space_seperated
    : tablegroup_option+
    ;

tablegroup_option_list
    : tablegroup_option_list_space_seperated
    | tablegroup_option Comma tablegroup_option_list
    ;

tablegroup_option
    : LOCALITY COMP_EQ? locality_name FORCE?
    | PRIMARY_ZONE COMP_EQ? primary_zone_name
    | (TABLEGROUP_ID|MAX_USED_PART_ID) COMP_EQ? INTNUM
    | BINDING COMP_EQ? BOOL_VALUE
    | SHARDING COMP_EQ? STRING_VALUE
    ;

alter_tablegroup_actions
    : alter_tablegroup_action (Comma alter_tablegroup_action)*
    ;

alter_tablegroup_action
    : SET? tablegroup_option_list_space_seperated
    ;

default_tablegroup
    : DEFAULT? TABLEGROUP COMP_EQ? (NULLX|relation_name)
    ;

create_view_stmt
    : CREATE (OR REPLACE)? view_attribute MATERIALIZED? VIEW view_name (LeftParen column_name_list RightParen)? (TABLE_ID COMP_EQ INTNUM)? AS view_select_stmt view_check_option?
    | ALTER view_attribute VIEW view_name (LeftParen column_name_list RightParen)? (TABLE_ID COMP_EQ INTNUM)? AS view_select_stmt view_check_option?
    ;

create_mview_stmt
    : CREATE MATERIALIZED VIEW view_name (LeftParen column_name_list RightParen)? table_option_list? (partition_option | auto_partition_option)? create_mview_refresh AS view_select_stmt ((WITH CHECK OPTION) | (WITH CASCADED CHECK OPTION) | (WITH LOCAL CHECK OPTION))?
    ;

create_mview_refresh
    : REFRESH mv_refresh_method mv_refresh_on_clause mv_refresh_interval
    | NEVER REFRESH
    | empty
    ;

mv_refresh_on_clause
    : ON mv_refresh_mode
    | empty
    ;

mv_refresh_method
    : FAST
    | COMPLETE
    | FORCE
    ;

mv_refresh_mode
    : DEMAND
    | COMMIT
    | STATEMENT
    ;

mv_refresh_interval
    : mv_start_clause mv_next_clause
    ;

mv_start_clause
    : START WITH bit_expr
    | empty
    ;

mv_next_clause
    : NEXT bit_expr
    | empty
    ;

view_attribute
    : (ALGORITHM COMP_EQ view_algorithm)? (DEFINER COMP_EQ user)? (SQL SECURITY (DEFINER | INVOKER))?
    | empty
    ;

view_check_option
    : WITH CHECK OPTION
    | WITH CASCADED CHECK OPTION
    | WITH LOCAL CHECK OPTION
    ;

view_algorithm
    : UNDEFINED
    | MERGE
    | TEMPTABLE
    ;

view_select_stmt
    : select_stmt
    ;

view_name
    : relation_factor
    ;

opt_tablet_id
    : TABLET_ID COMP_EQ INTNUM
    ;

opt_tablet_id_no_empty
    : TABLET_ID opt_equal_mark INTNUM
    ;

create_index_stmt
    : CREATE (FULLTEXT | SPATIAL | UNIQUE)? INDEX (IF not EXISTS)? normal_relation_factor index_using_algorithm? ON relation_factor LeftParen sort_column_list RightParen opt_index_options? (partition_option | auto_partition_option)? with_column_group?
    ;

index_name
    : relation_name
    ;

check_state
    : NOT? ENFORCED
    ;

opt_constraint_name
    : constraint_name?
    ;

constraint_name
    : relation_name
    ;

sort_column_list
    : sort_column_key (Comma sort_column_key)*
    ;

sort_column_key
    : column_name (LeftParen INTNUM RightParen)? (ASC | DESC)? (ID INTNUM)?
    | LeftParen expr RightParen (ASC | DESC)? (ID INTNUM)?
    ;

opt_index_options
    : index_option+
    ;

index_option
    : GLOBAL
    | LOCAL
    | (BLOCK_SIZE|DATA_TABLE_ID|INDEX_TABLE_ID|VIRTUAL_COLUMN_ID|MAX_USED_PART_ID) COMP_EQ? INTNUM
    | COMMENT STRING_VALUE
    | (STORING|CTXCAT) LeftParen column_name_list RightParen
    | WITH ROWID
    | WITH PARSER STRING_VALUE
    | index_using_algorithm
    | visibility_option
    | parallel_option
    ;

index_using_algorithm
    : USING (BTREE|HASH)
    ;

create_mlog_stmt
    : CREATE MATERIALIZED VIEW LOG ON relation_factor opt_mlog_options? (WITH mlog_with_values)? (mlog_including_or_excluding NEW VALUES)? (PURGE mlog_purge_values)?
    ;

opt_mlog_options
    : mlog_option+
    ;

mlog_option
    : parallel_option
    ;

mlog_with_values
    : mlog_with_special_columns mlog_with_reference_columns
    ;

mlog_with_special_columns
    : mlog_with_special_column_list?
    ;

mlog_with_special_column_list
    : mlog_with_special_column (Comma mlog_with_special_column_list)?
    ;

mlog_with_special_column
    : PRIMARY KEY
    | ROWID
    | SEQUENCE
    ;

mlog_with_reference_columns
    : empty
    | LeftParen mlog_with_reference_column_list? RightParen
    ;

mlog_with_reference_column_list
    : mlog_with_reference_column (Comma mlog_with_reference_column_list)?
    ;

mlog_with_reference_column
    : column_name
    ;

mlog_including_or_excluding
    : INCLUDING
    | EXCLUDING
    ;

mlog_purge_values
    : IMMEDIATE mlog_purge_immediate_sync_or_async
    | mlog_purge_start mlog_purge_next
    ;

mlog_purge_immediate_sync_or_async
    : (SYNCHRONOUS | ASYNCHRONOUS)?
    ;

mlog_purge_start
    : empty
    | START WITH bit_expr
    ;

mlog_purge_next
    : empty
    | NEXT bit_expr
    ;

drop_mlog_stmt
    : DROP MATERIALIZED VIEW LOG ON relation_factor
    ;

drop_table_stmt
    : DROP (TEMPORARY | MATERIALIZED)? table_or_tables (IF EXISTS)? table_list (CASCADE | RESTRICT)?
    ;

table_or_tables
    : TABLE
    | TABLES
    ;

drop_view_stmt
    : DROP VIEW (IF EXISTS)? table_list (CASCADE | RESTRICT)?
    | DROP MATERIALIZED VIEW (IF EXISTS)? table_list (CASCADE | RESTRICT)?
    ;

table_list
    : relation_factor (Comma relation_factor)*
    ;

drop_index_stmt
    : DROP INDEX relation_name ON relation_factor
    ;

insert_stmt
    : insert_with_opt_hint IGNORE? INTO? single_table_insert (ON DUPLICATE KEY UPDATE update_asgn_list)?
    | replace_with_opt_hint IGNORE? INTO? single_table_insert
    ;

single_table_insert
    : dml_table_name (SET update_asgn_list (AS table_subquery_alias)?|values_clause)
    | dml_table_name LeftParen column_list? RightParen values_clause
    ;

values_clause
    : value_or_values insert_vals_list (AS table_subquery_alias)?
    | select_stmt
    ;

value_or_values
    : VALUE
    | VALUES
    ;

replace_with_opt_hint
    : REPLACE
    | REPLACE_HINT_BEGIN hint_list_with_end
    ;

insert_with_opt_hint
    : INSERT
    | INSERT_HINT_BEGIN hint_list_with_end
    ;

column_list
    : column_definition_ref (Comma column_definition_ref)*
    ;

insert_vals_list
    : LeftParen insert_vals RightParen
    | insert_vals_list Comma LeftParen insert_vals RightParen
    ;

insert_vals
    : expr_or_default
    | empty
    | insert_vals Comma expr_or_default
    ;

expr_or_default
    : expr
    | DEFAULT
    ;

select_stmt
    : with_clause? (select_no_parens into_clause? |select_with_parens)
    ;


select_with_parens
    : LeftParen with_clause? (select_no_parens |select_with_parens) RightParen
    ;

select_no_parens
    : select_clause (for_update_clause | opt_lock_in_share_mode)?
    | select_clause_set (for_update_clause | opt_lock_in_share_mode)?
    | select_clause_set_with_order_and_limit (for_update_clause | opt_lock_in_share_mode)?
    ;

no_table_select
    : select_with_opt_hint query_expression_option_list? select_expr_list into_opt (FROM DUAL (WHERE opt_hint_value expr)? (GROUP BY groupby_clause)? (HAVING expr)? (WINDOW named_windows)?)?
    ;

select_clause
    : no_table_select_with_order_and_limit
    | simple_select_with_order_and_limit
    | select_with_parens_with_order_and_limit
    | table_values_clause
    | table_values_clause_with_order_by_and_limit
    ;

select_clause_set_with_order_and_limit
    : select_clause_set order_by
    | select_clause_set order_by? limit_clause
    ;

select_clause_set
    : select_clause_set order_by? limit_clause? set_type select_clause_set_right
    | select_clause_set_left set_type select_clause_set_right
    ;

select_clause_set_right
    : no_table_select
    | simple_select
    | select_with_parens
    | table_values_clause
    ;

select_clause_set_left
    : no_table_select_with_order_and_limit
    | simple_select_with_order_and_limit
    | select_clause_set_right
    ;

no_table_select_with_order_and_limit
    : no_table_select order_by? limit_clause?
    ;

//
simple_select_with_order_and_limit
    : simple_select order_by? limit_clause?
    ;

select_with_parens_with_order_and_limit
    : select_with_parens order_by
    | select_with_parens order_by? limit_clause
    ;

select_with_opt_hint
    : SELECT
    | SELECT_HINT_BEGIN hint_list_with_end
    ;

update_with_opt_hint
    : UPDATE
    | UPDATE_HINT_BEGIN hint_list_with_end
    ;

delete_with_opt_hint
    : DELETE
    | DELETE_HINT_BEGIN hint_list_with_end
    ;

simple_select
    : select_with_opt_hint query_expression_option_list? select_expr_list into_opt FROM from_list (WHERE opt_hint_value expr)? (GROUP BY groupby_clause)? (HAVING expr)? (WINDOW named_windows)?
    ;

set_type_union
    : UNION
    ;

set_type_other
    : INTERSECT
    | EXCEPT
    | MINUS
    ;

set_type
    : set_type_union set_expression_option
    | set_type_other
    ;

set_expression_option
    : (ALL | DISTINCT | UNIQUE)?
    ;

opt_hint_value
    : HINT_VALUE?
    ;

limit_clause
    : LIMIT limit_expr ((OFFSET limit_expr)?|Comma limit_expr)
    ;

into_clause
    : INTO OUTFILE STRING_VALUE (charset_key charset_name)? field_opt line_opt
    | INTO DUMPFILE STRING_VALUE
    | INTO into_var_list
    ;

into_opt
    : into_clause?
    ;

into_var_list
    : into_var (Comma into_var)*
    ;

into_var
    : USER_VARIABLE
    | NAME_OB
    | unreserved_keyword_normal
    ;

field_opt
    : columns_or_fields field_term_list
    | empty
    ;

field_term_list
    : field_term+
    ;

field_term
    : ((OPTIONALLY? ENCLOSED|TERMINATED)|ESCAPED) BY STRING_VALUE
    ;

line_opt
    : LINES line_term_list
    | empty
    ;

line_term_list
    : line_term+
    ;

line_term
    : (STARTING|TERMINATED) BY STRING_VALUE
    ;

hint_list_with_end
    : (hint_options | (opt_hint_list Comma hint_options))? HINT_END
    ;

opt_hint_list
    : hint_options
    | empty
    | opt_hint_list Comma hint_options
    ;

hint_options
    : hint_option+
    ;

name_list
    : NAME_OB
    | name_list Comma? NAME_OB
    ;

hint_option
    : (NO_REWRITE|HOTSPOT|ORDERED|USE_HASH_AGGREGATION|NO_USE_HASH_AGGREGATION|NO_USE_JIT|USE_LATE_MATERIALIZATION|USE_LATE_MATERIALIZATION
    | NO_USE_LATE_MATERIALIZATION|TRACE_LOG|USE_PX|NO_USE_PX|NAME_OB|EOF|PARSER_SYNTAX_ERROR
    | ENABLE_PARALLEL_DML| DISABLE_PARALLEL_DML|NO_PARALLEL|MONITOR)
    | READ_CONSISTENCY LeftParen consistency_level RightParen
    | INDEX_HINT LeftParen qb_name_option relation_factor_in_hint NAME_OB RightParen
    | (QUERY_TIMEOUT|FROZEN_VERSION) LeftParen INTNUM RightParen
    | TOPK LeftParen INTNUM INTNUM RightParen
    | LOG_LEVEL LeftParen NAME_OB RightParen
    | LOG_LEVEL LeftParen Quote STRING_VALUE Quote RightParen
    | LEADING_HINT LeftParen qb_name_option relation_factor_in_leading_hint_list_entry RightParen
    | LEADING_HINT LeftParen qb_name_option relation_factor_in_hint_list RightParen
    | (FULL_HINT|PQ_MAP) LeftParen qb_name_option relation_factor_in_hint RightParen
    | USE_PLAN_CACHE LeftParen use_plan_cache_type RightParen
    | (USE_MERGE|NO_USE_MERGE|USE_HASH|NO_USE_HASH|USE_NL|PX_JOIN_FILTER|NO_PX_JOIN_FILTER
    | NO_USE_NL|USE_BNL|NO_USE_BNL|USE_NL_MATERIALIZATION|NO_USE_NL_MATERIALIZATION) LeftParen qb_name_option relation_factor_in_use_join_hint_list RightParen
    | (MERGE_HINT|NO_MERGE_HINT|NO_EXPAND|USE_CONCAT|UNNEST|NO_UNNEST|PLACE_GROUP_BY|NO_PLACE_GROUP_BY|NO_PRED_DEDUCE|INLINE|MATERIALIZE) (LeftParen qb_name_option RightParen)?
    | USE_JIT LeftParen use_jit_type RightParen
    | (STAT|TRACING) LeftParen tracing_num_list RightParen
    | DOP LeftParen INTNUM Comma INTNUM RightParen
    | TRANS_PARAM LeftParen trans_param_name Comma? trans_param_value RightParen
    | FORCE_REFRESH_LOCATION_CACHE
    | QB_NAME LeftParen NAME_OB RightParen
    | (MAX_CONCURRENT|PARALLEL|LOAD_BATCH_SIZE) LeftParen INTNUM RightParen
    | PQ_DISTRIBUTE LeftParen qb_name_option relation_factor_in_pq_hint Comma? distribute_method (Comma? distribute_method)? RightParen
    ;

consistency_level
    : WEAK
    | STRONG
    | FROZEN
    ;

use_plan_cache_type
    : NONE
    | DEFAULT
    ;

use_jit_type
    : AUTO
    | FORCE
    ;

distribute_method
    : ALL
    | NONE
    | PARTITION
    | RANDOM
    | RANDOM_LOCAL
    | HASH
    | BROADCAST
    | LOCAL
    | BC2HOST
    | RANGE
    | LIST
    ;

limit_expr
    : INTNUM
    | QUESTIONMARK
    | column_ref
    ;

for_update_clause
    : FOR UPDATE opt_for_update_wait
    ;

opt_lock_in_share_mode
    : LOCK_ IN SHARE MODE
    ;

opt_for_update_wait
    : empty
    | WAIT DECIMAL_VAL
    | WAIT INTNUM
    | NOWAIT
    | NO_WAIT
    ;

parameterized_trim
    : (BOTH FROM)? expr
    | BOTH? expr FROM expr
    | (LEADING|TRAILING) expr? FROM expr
    ;

groupby_clause
    : sort_list_for_group_by (WITH ROLLUP)?
    ;

sort_list_for_group_by
    : sort_key_for_group_by (Comma sort_key_for_group_by)*
    ;

sort_key_for_group_by
    : expr (ASC | DESC)?
    ;

order_by
    : ORDER BY sort_list
    ;

sort_list
    : sort_key (Comma sort_key)*
    ;

sort_key
    : expr (ASC | DESC)?
    ;

query_expression_option_list
    : query_expression_option+
    ;

query_expression_option
    : ALL
    | DISTINCT
    | UNIQUE
    | SQL_CALC_FOUND_ROWS
    | SQL_NO_CACHE
    | SQL_CACHE
    ;

projection
    : expr AS? (column_label|STRING_VALUE)?
    | Star
    ;

select_expr_list
    : projection (Comma projection)*
    ;

from_list
    : table_references
    ;

table_references
    : table_reference (Comma table_reference)*
    | table_references_paren (Comma table_references_paren)*
    ;

table_references_paren
    : table_reference (Comma table_reference)*
    | LeftParen table_reference (Comma table_reference)* RightParen
    | LeftParen table_references_paren (Comma table_reference)* RightParen
    ;

table_reference
    : table_factor
    | joined_table
    ;

table_factor
    : tbl_name
    | table_subquery
    | select_with_parens use_flashback?
    | TABLE LeftParen simple_expr RightParen (AS relation_name|relation_name?)
    | LeftParen table_reference RightParen
    | LeftBrace OJ table_reference RightBrace
    | json_table_expr (AS? relation_name)?
    ;

tbl_name
    : relation_factor use_partition? (sample_clause seed?|use_flashback?) relation_name?
    | relation_factor use_partition? ((AS? relation_name|sample_clause?)|sample_clause (relation_name|seed relation_name?)) index_hint_list
    | relation_factor use_partition? use_flashback? AS relation_name
    | relation_factor use_partition? sample_clause seed? AS relation_name index_hint_list?
    ;

dml_table_name
    : relation_factor use_partition?
    ;

seed
    : SEED LeftParen INTNUM RightParen
    ;

sample_percent
    : INTNUM
    | DECIMAL_VAL
    ;

sample_clause
    : SAMPLE BLOCK? (ALL | BASE | INCR)? LeftParen sample_percent RightParen
    ;

table_subquery
    : select_with_parens use_flashback? AS? table_subquery_alias
    ;

table_subquery_alias
    : relation_name
    | relation_name LeftParen alias_name_list RightParen
    ;

use_partition
    : PARTITION LeftParen name_list RightParen
    ;

use_flashback
    : AS OF SNAPSHOT bit_expr
    ;

index_hint_type
    : FORCE
    | IGNORE
    ;

key_or_index
    : KEY
    | INDEX
    ;

index_hint_scope
    : empty
    | FOR ((JOIN|ORDER BY)|GROUP BY)
    ;

index_element
    : NAME_OB
    | PRIMARY
    ;

index_list
    : index_element (Comma index_element)*
    ;

index_hint_definition
    : USE key_or_index index_hint_scope LeftParen index_list? RightParen
    | index_hint_type key_or_index index_hint_scope LeftParen index_list RightParen
    ;

index_hint_list
    : index_hint_definition+
    ;

relation_factor
    : normal_relation_factor
    | dot_relation_factor
    ;

relation_with_star_list
    : relation_factor_with_star (Comma relation_factor_with_star)*
    ;

relation_factor_with_star
    : relation_name (Dot relation_name)? (Dot Star)?
    ;

normal_relation_factor
    : relation_name USER_VARIABLE?
    | relation_name Dot relation_name USER_VARIABLE?
    | relation_name Dot mysql_reserved_keyword
    ;

dot_relation_factor
    : Dot (relation_name|mysql_reserved_keyword)
    ;

relation_factor_in_hint
    : normal_relation_factor qb_name_option
    ;

qb_name_option
    : At NAME_OB
    | empty
    ;

relation_factor_in_hint_list
    : relation_factor_in_hint (relation_sep_option relation_factor_in_hint)*
    ;

relation_sep_option
    : Comma?
    ;

relation_factor_in_pq_hint
    : relation_factor_in_hint
    | LeftParen relation_factor_in_hint_list RightParen
    ;

relation_factor_in_leading_hint
    : LeftParen relation_factor_in_hint_list RightParen
    ;

relation_factor_in_leading_hint_list
    : relation_factor_in_leading_hint
    | LeftParen (relation_factor_in_hint_list relation_sep_option)? relation_factor_in_leading_hint_list RightParen
    | relation_factor_in_leading_hint_list relation_sep_option (relation_factor_in_hint|relation_factor_in_leading_hint)
    | relation_factor_in_leading_hint_list relation_sep_option LeftParen relation_factor_in_hint_list relation_sep_option relation_factor_in_leading_hint_list RightParen
    ;

relation_factor_in_leading_hint_list_entry
    : (relation_factor_in_hint_list relation_sep_option)? relation_factor_in_leading_hint_list
    ;

relation_factor_in_use_join_hint_list
    : relation_factor_in_hint
    | LeftParen relation_factor_in_hint_list RightParen
    | relation_factor_in_use_join_hint_list relation_sep_option relation_factor_in_hint
    | relation_factor_in_use_join_hint_list relation_sep_option LeftParen relation_factor_in_hint_list RightParen
    ;

tracing_num_list
    : INTNUM (relation_sep_option tracing_num_list)?
    ;

join_condition
    : ON expr
    | USING LeftParen column_list RightParen
    ;

joined_table
    : table_factor inner_join_type table_factor (ON expr)?
    | table_factor inner_join_type table_factor USING LeftParen column_list RightParen
    | table_factor outer_join_type table_factor join_condition?
    | table_factor natural_join_type table_factor
    | joined_table inner_join_type table_factor (ON expr)?
    | joined_table inner_join_type table_factor USING LeftParen column_list RightParen
    | joined_table outer_join_type table_factor join_condition?
    | joined_table natural_join_type table_factor
    ;

natural_join_type
    : NATURAL outer_join_type
    | NATURAL INNER? JOIN
    ;

inner_join_type
    : INNER? JOIN
    | CROSS JOIN
    | STRAIGHT_JOIN
    ;

outer_join_type
    : (FULL|LEFT|RIGHT) OUTER? JOIN
    ;


with_clause
    : WITH RECURSIVE? with_list
    ;

with_list
    : common_table_expr (Comma common_table_expr)*
    ;

common_table_expr
    : relation_name (LeftParen alias_name_list RightParen)? AS LeftParen with_clause? (select_no_parens |select_with_parens) RightParen
    ;

alias_name_list
    : column_alias_name (Comma column_alias_name)*
    ;

column_alias_name
    : column_name
    ;

table_values_clause
    : VALUES values_row_list
    ;

table_values_clause_with_order_by_and_limit
    : table_values_clause order_by
    | table_values_clause order_by? limit_clause
    ;

values_row_list
    : row_value (Comma row_value)*
    ;

row_value
    : ROW LeftParen insert_vals RightParen
    ;

analyze_stmt
    : ANALYZE TABLE relation_factor UPDATE HISTOGRAM ON column_name_list WITH INTNUM BUCKETS
    | ANALYZE TABLE relation_factor (DROP|UPDATE) HISTOGRAM ON column_name_list
    | ANALYZE TABLE relation_factor use_partition? analyze_statistics_clause
    | ANALYZE TABLE table_list
    ;

analyze_statistics_clause
    : COMPUTE STATISTICS opt_analyze_for_clause_list?
    | ESTIMATE STATISTICS opt_analyze_for_clause_list? (SAMPLE INTNUM sample_option)?
    ;

opt_analyze_for_clause_list
    : opt_analyze_for_clause_element
    ;

opt_analyze_for_clause_element
    : FOR TABLE
    | for_all
    | for_columns
    ;

sample_option
    : ROWS
    | PERCENTAGE
    ;

for_all
    : FOR ALL (INDEXED | HIDDEN_)? COLUMNS size_clause?
    ;

size_clause
    : SIZE (AUTO|REPEAT|SKEWONLY)
    | SIZE number_literal
    ;

for_columns
    : FOR COLUMNS for_columns_list?
    ;

for_columns_list
    : for_columns_item
    | for_columns_list for_columns_item
    | for_columns_list Comma for_columns_item
    ;

for_columns_item
    : column_clause size_clause?
    | size_clause
    ;

column_clause
    : column_name
    | LeftParen column_name_list RightParen
    ;

create_outline_stmt
    : CREATE (OR REPLACE)? OUTLINE relation_name ON explainable_stmt (TO explainable_stmt)?
    | CREATE (OR REPLACE)? OUTLINE relation_name ON STRING_VALUE USING HINT_HINT_BEGIN hint_list_with_end
    ;

alter_outline_stmt
    : ALTER OUTLINE relation_name ADD explainable_stmt (TO explainable_stmt)?
    ;

drop_outline_stmt
    : DROP OUTLINE relation_factor
    ;

explain_stmt
    : explain_or_desc relation_factor (STRING_VALUE | column_name)?
    | explain_or_desc explainable_stmt
    | explain_or_desc PRETTY explainable_stmt
    | explain_or_desc PRETTY_COLOR explainable_stmt
    | explain_or_desc BASIC explainable_stmt
    | explain_or_desc BASIC PRETTY explainable_stmt
    | explain_or_desc BASIC PRETTY_COLOR explainable_stmt
    | explain_or_desc OUTLINE explainable_stmt
    | explain_or_desc OUTLINE PRETTY explainable_stmt
    | explain_or_desc OUTLINE PRETTY_COLOR explainable_stmt
    | explain_or_desc EXTENDED explainable_stmt
    | explain_or_desc EXTENDED PRETTY explainable_stmt
    | explain_or_desc EXTENDED PRETTY_COLOR explainable_stmt
    | explain_or_desc EXTENDED_NOADDR explainable_stmt
    | explain_or_desc EXTENDED_NOADDR PRETTY explainable_stmt
    | explain_or_desc EXTENDED_NOADDR PRETTY_COLOR explainable_stmt
    | explain_or_desc PLANREGRESS explainable_stmt
    | explain_or_desc PLANREGRESS PRETTY explainable_stmt
    | explain_or_desc PLANREGRESS PRETTY_COLOR explainable_stmt
    | explain_or_desc PARTITIONS explainable_stmt
    | explain_or_desc PARTITIONS PRETTY explainable_stmt
    | explain_or_desc PARTITIONS PRETTY_COLOR explainable_stmt
    | explain_or_desc SET STATEMENT_ID COMP_EQ literal explainable_stmt
    | explain_or_desc INTO relation_name explainable_stmt
    | explain_or_desc INTO relation_name SET STATEMENT_ID COMP_EQ literal explainable_stmt
    | explain_or_desc FORMAT COMP_EQ format_name explainable_stmt
    ;

explain_or_desc
    : EXPLAIN
    | DESCRIBE
    | DESC
    ;

explainable_stmt
    : select_stmt
    | delete_stmt
    | insert_stmt
    | update_stmt
    ;

format_name
    : TRADITIONAL
    | JSON
    ;

show_stmt
    : SHOW (FULL | EXTENDED | (EXTENDED FULL))? TABLES (from_or_in database_factor)? ((LIKE STRING_VALUE) | (LIKE STRING_VALUE ESCAPE STRING_VALUE) | (WHERE expr))?
    | SHOW databases_or_schemas STATUS? ((LIKE STRING_VALUE) | (LIKE STRING_VALUE ESCAPE STRING_VALUE) | (WHERE expr))?
    | SHOW (FULL | EXTENDED | (EXTENDED FULL))? columns_or_fields from_or_in relation_factor (from_or_in database_factor)? ((LIKE STRING_VALUE) | (LIKE STRING_VALUE ESCAPE STRING_VALUE) | (WHERE expr))?
    | SHOW (TABLE|PROCEDURE|FUNCTION|TRIGGERS) STATUS (from_or_in database_factor)? ((LIKE STRING_VALUE) | (LIKE STRING_VALUE ESCAPE STRING_VALUE) | (WHERE expr))?
    | SHOW SERVER STATUS ((LIKE STRING_VALUE) | (LIKE STRING_VALUE ESCAPE STRING_VALUE) | (WHERE expr))?
    | SHOW (GLOBAL | SESSION | LOCAL)? VARIABLES ((LIKE STRING_VALUE) | (LIKE STRING_VALUE ESCAPE STRING_VALUE) | (WHERE expr))?
    | SHOW SCHEMA
    | SHOW CREATE database_or_schema (IF not EXISTS)? database_factor
    | SHOW CREATE (TABLE|VIEW|PROCEDURE|FUNCTION|TRIGGER) relation_factor
    | SHOW (WARNINGS|ERRORS) ((LIMIT INTNUM Comma INTNUM) | (LIMIT INTNUM))?
    | SHOW COUNT LeftParen Star RightParen (WARNINGS|ERRORS)
    | SHOW GRANTS opt_for_grant_user
    | SHOW charset_key ((LIKE STRING_VALUE) | (LIKE STRING_VALUE ESCAPE STRING_VALUE) | (WHERE expr))?
    | SHOW (TRACE|COLLATION|PARAMETERS|TABLEGROUPS) ((LIKE STRING_VALUE) | (LIKE STRING_VALUE ESCAPE STRING_VALUE) | (WHERE expr))?
    | SHOW TRACE FORMAT COMP_EQ STRING_VALUE ((LIKE STRING_VALUE) | (LIKE STRING_VALUE ESCAPE STRING_VALUE) | (WHERE expr))?
    | SHOW EXTENDED? index_or_indexes_or_keys from_or_in relation_factor (from_or_in database_factor)? (WHERE opt_hint_value expr)?
    | SHOW FULL? PROCESSLIST
    | SHOW (GLOBAL | SESSION | LOCAL)? STATUS ((LIKE STRING_VALUE) | (LIKE STRING_VALUE ESCAPE STRING_VALUE) | (WHERE expr))?
    | SHOW TENANT STATUS?
    | SHOW CREATE TENANT relation_name
    | SHOW STORAGE? ENGINES
    | SHOW PRIVILEGES
    | SHOW QUERY_RESPONSE_TIME
    | SHOW RECYCLEBIN
    | SHOW CREATE TABLEGROUP relation_name
    | SHOW RESTORE PREVIEW
    | SHOW SEQUENCES ((LIKE STRING_VALUE) | (LIKE STRING_VALUE ESCAPE STRING_VALUE) | (WHERE expr))? (from_or_in database_factor)?
    ;

get_diagnostics_stmt
    : get_condition_diagnostics_stmt
    | get_statement_diagnostics_stmt
    ;

get_condition_diagnostics_stmt
    : GET (CURRENT|STACKED)? DIAGNOSTICS CONDITION condition_arg condition_information_item_list
    ;

condition_arg
    : INTNUM
    | USER_VARIABLE
    | STRING_VALUE
    | BOOL_VALUE
    | QUESTIONMARK
    | column_name
    ;

condition_information_item_list
    : condition_information_item (Comma condition_information_item)*
    ;

condition_information_item
    : (QUESTIONMARK|USER_VARIABLE|column_name) COMP_EQ condition_information_item_name
    ;

condition_information_item_name
    : CLASS_ORIGIN
    | SUBCLASS_ORIGIN
    | RETURNED_SQLSTATE
    | MESSAGE_TEXT
    | MYSQL_ERRNO
    | CONSTRAINT_CATALOG
    | CONSTRAINT_SCHEMA
    | CONSTRAINT_NAME
    | CATALOG_NAME
    | SCHEMA_NAME
    | TABLE_NAME
    | COLUMN_NAME
    | CURSOR_NAME
    ;

get_statement_diagnostics_stmt
    : GET (CURRENT|STACKED)? DIAGNOSTICS statement_information_item_list
    ;

statement_information_item_list
    : statement_information_item (Comma statement_information_item)*
    ;

statement_information_item
    : (QUESTIONMARK|USER_VARIABLE|column_name) COMP_EQ statement_information_item_name
    ;

statement_information_item_name
    : NUMBER
    | ROW_COUNT
    ;

databases_or_schemas
    : DATABASES
    | SCHEMAS
    ;

opt_for_grant_user
    : opt_for_user
    | FOR CURRENT_USER (LeftParen RightParen)?
    ;

columns_or_fields
    : COLUMNS
    | FIELDS
    ;

database_or_schema
    : DATABASE
    | SCHEMA
    ;

index_or_indexes_or_keys
    : INDEX
    | INDEXES
    | KEYS
    ;

from_or_in
    : FROM
    | IN
    ;

calibration_info_list
    : empty
    | STRING_VALUE
    | calibration_info_list Comma STRING_VALUE
    ;

help_stmt
    : HELP STRING_VALUE
    | HELP NAME_OB
    ;

create_tablespace_stmt
    : CREATE TABLESPACE tablespace permanent_tablespace
    ;

permanent_tablespace
    : permanent_tablespace_options?
    ;

permanent_tablespace_option
    : ENCRYPTION COMP_EQ? STRING_VALUE
    ;

drop_tablespace_stmt
    : DROP TABLESPACE tablespace
    ;

alter_tablespace_actions
    : alter_tablespace_action (Comma alter_tablespace_action)?
    ;

alter_tablespace_action
    : SET? permanent_tablespace_option
    ;

alter_tablespace_stmt
    : ALTER TABLESPACE tablespace alter_tablespace_actions
    ;

rotate_master_key_stmt
    : ALTER INSTANCE ROTATE INNODB MASTER KEY
    ;

permanent_tablespace_options
    : permanent_tablespace_option (Comma permanent_tablespace_option)*
    ;

create_user_stmt
    : CREATE USER (IF not EXISTS)? user_specification_list require_specification? (WITH resource_option_list)?
    ;

user_specification_list
    : user_specification (Comma user_specification)*
    ;

user_specification
    : user USER_VARIABLE?
    | user USER_VARIABLE? IDENTIFIED ((WITH STRING_VALUE) | (WITH NAME_OB))? BY password
    | user USER_VARIABLE? IDENTIFIED ((WITH STRING_VALUE) | (WITH NAME_OB))? BY PASSWORD password
    ;

require_specification
    : REQUIRE (NONE|SSL|X509)
    | REQUIRE tls_option_list
    ;

resource_option_list
    : resource_option+
    ;

resource_option
    : MAX_CONNECTIONS_PER_HOUR INTNUM
    | MAX_USER_CONNECTIONS INTNUM
    ;

tls_option_list
    : tls_option
    | tls_option_list tls_option
    | tls_option_list AND tls_option
    ;

tls_option
    : (CIPHER|ISSUER|SUBJECT) STRING_VALUE
    ;

user
    : STRING_VALUE
    | NAME_OB
    | unreserved_keyword
    ;

opt_host_name
    : USER_VARIABLE?
    ;

user_with_host_name
    : user USER_VARIABLE?
    ;

password
    : STRING_VALUE
    ;

drop_user_stmt
    : DROP USER user_list
    ;

user_list
    : user_with_host_name (Comma user_with_host_name)*
    ;

set_password_stmt
    : SET PASSWORD (FOR user opt_host_name)? COMP_EQ STRING_VALUE
    | SET PASSWORD (FOR user opt_host_name)? COMP_EQ PASSWORD LeftParen password RightParen
    | ALTER USER user_with_host_name IDENTIFIED ((WITH STRING_VALUE) | (WITH NAME_OB))? BY password
    | ALTER USER user_with_host_name require_specification
    | ALTER USER user_with_host_name WITH resource_option_list
    ;

opt_for_user
    : FOR user opt_host_name
    | empty
    ;

rename_user_stmt
    : RENAME USER rename_list
    ;

rename_info
    : user USER_VARIABLE? TO user USER_VARIABLE?
    ;

rename_list
    : rename_info (Comma rename_info)*
    ;

lock_user_stmt
    : ALTER USER user_list ACCOUNT lock_spec_mysql57
    ;

lock_spec_mysql57
    : LOCK_
    | UNLOCK
    ;

lock_tables_stmt
    : LOCK_ table_or_tables lock_table_list
    ;

unlock_tables_stmt
    : UNLOCK TABLES
    ;

lock_table_list
    : lock_table (Comma lock_table)*
    ;

lock_table
    : relation_factor lock_type
    | relation_factor AS? relation_name lock_type
    ;

lock_type
    : READ LOCAL?
    | WRITE
    | LOW_PRIORITY WRITE
    ;

create_sequence_stmt
    : CREATE SEQUENCE (IF not EXISTS)? relation_factor sequence_option_list?
    ;

sequence_option_list
    : sequence_option+
    ;

sequence_option
    : (INCREMENT BY|MAXVALUE) simple_num
    | (MINVALUE|START WITH) simple_num
    | NOMAXVALUE
    | NOMINVALUE
    | CYCLE
    | NOCYCLE
    | CACHE simple_num
    | NOCACHE
    | ORDER
    | NOORDER
    | RESTART
    ;

simple_num
    : (INTNUM|DECIMAL_VAL)
    | (Minus|Plus) (INTNUM|DECIMAL_VAL)
    ;

drop_sequence_stmt
    : DROP SEQUENCE (IF EXISTS)? relation_factor
    ;

alter_sequence_stmt
    : ALTER SEQUENCE relation_factor sequence_option_list?
    ;

create_dblink_stmt
    : CREATE DATABASE LINK (IF not EXISTS)? relation_name CONNECT TO user USER_VARIABLE DATABASE database_factor IDENTIFIED BY password ip_port (CLUSTER STRING_VALUE)?
    ;

drop_dblink_stmt
    : DROP DATABASE LINK (IF EXISTS)? relation_name
    ;

begin_stmt
    : BEGIN HINT_VALUE? WORK?
    | START HINT_VALUE? TRANSACTION ((WITH CONSISTENT SNAPSHOT) | transaction_access_mode | (WITH CONSISTENT SNAPSHOT Comma transaction_access_mode) | (transaction_access_mode Comma WITH CONSISTENT SNAPSHOT))?
    ;

xa_begin_stmt
    : XA (BEGIN|START) STRING_VALUE
    ;

xa_end_stmt
    : XA END STRING_VALUE
    ;

xa_prepare_stmt
    : XA PREPARE STRING_VALUE
    ;

xa_commit_stmt
    : XA COMMIT STRING_VALUE
    ;

xa_rollback_stmt
    : XA ROLLBACK STRING_VALUE
    ;

commit_stmt
    : COMMIT HINT_VALUE? WORK?
    ;

rollback_stmt
    : ROLLBACK WORK?
    | ROLLBACK HINT_VALUE WORK?
    ;

kill_stmt
    : KILL (CONNECTION?|QUERY) expr
    ;

grant_stmt
    : GRANT grant_privileges ON object_type? priv_level TO user_specification_list grant_options
    ;

grant_privileges
    : priv_type_list
    | ALL PRIVILEGES?
    ;

priv_type_list
    : priv_type (Comma priv_type)*
    ;

priv_type
    : ALTER TENANT?
    | CREATE (RESOURCE POOL|USER?)
    | DELETE
    | DROP (DATABASE LINK)?
    | GRANT OPTION
    | INSERT
    | UPDATE
    | SELECT
    | INDEX
    | CREATE (RESOURCE UNIT|VIEW)
    | SHOW VIEW
    | SHOW DATABASES
    | SUPER
    | PROCESS
    | USAGE
    | FILEX
    | ALTER SYSTEM
    | REPLICATION SLAVE
    | REPLICATION CLIENT
    | CREATE (DATABASE LINK|ROUTINE)
    | EXECUTE
    | ALTER ROUTINE
    ;

object_type
    : TABLE
    | PROCEDURE
    | FUNCTION
    ;

priv_level
    : Star (Dot Star)?
    | relation_name ((Dot Star)?|Dot relation_name)
    ;

grant_options
    : WITH GRANT OPTION
    | empty
    ;

revoke_stmt
    : REVOKE grant_privileges ON priv_level FROM user_list
    | REVOKE grant_privileges ON object_type priv_level FROM user_list
    | REVOKE ALL PRIVILEGES? Comma GRANT OPTION FROM user_list
    ;

prepare_stmt
    : PREPARE stmt_name FROM preparable_stmt
    ;

stmt_name
    : column_label
    ;

preparable_stmt
    : text_string
    | USER_VARIABLE
    ;

variable_set_stmt
    : SET var_and_val_list
    ;

sys_var_and_val_list
    : sys_var_and_val (Comma sys_var_and_val)*
    ;

var_and_val_list
    : var_and_val (Comma var_and_val)*
    ;

set_expr_or_default
    : expr
    | ON
    | BINARY
    | DEFAULT
    ;

var_and_val
    : USER_VARIABLE (SET_VAR|to_or_eq) expr
    | sys_var_and_val
    | (SYSTEM_VARIABLE|scope_or_scope_alias column_name) to_or_eq set_expr_or_default
    ;

sys_var_and_val
    : var_name (SET_VAR|to_or_eq) set_expr_or_default
    ;

scope_or_scope_alias
    : GLOBAL
    | SESSION
    | GLOBAL_ALIAS Dot
    | SESSION_ALIAS Dot
    ;

to_or_eq
    : TO
    | COMP_EQ
    ;

execute_stmt
    : EXECUTE stmt_name (USING argument_list)?
    ;

argument_list
    : argument (Comma argument)*
    ;

argument
    : USER_VARIABLE
    ;

deallocate_prepare_stmt
    : deallocate_or_drop PREPARE stmt_name
    ;

deallocate_or_drop
    : DEALLOCATE
    | DROP
    ;

truncate_table_stmt
    : TRUNCATE TABLE? relation_factor
    ;

audit_stmt
    : audit_or_noaudit audit_clause
    ;

audit_or_noaudit
    : AUDIT
    | NOAUDIT
    ;

audit_clause
    : audit_operation_clause (auditing_by_user_clause|auditing_on_clause?) op_audit_tail_clause
    ;

audit_operation_clause
    : audit_all_shortcut_list
    | ALL STATEMENTS?
    ;

audit_all_shortcut_list
    : audit_all_shortcut (Comma audit_all_shortcut)*
    ;

auditing_on_clause
    : ON normal_relation_factor
    | ON DEFAULT
    ;

audit_user_list
    : audit_user_with_host_name (Comma audit_user_with_host_name)*
    ;

audit_user_with_host_name
    : audit_user USER_VARIABLE?
    ;

audit_user
    : STRING_VALUE
    | NAME_OB
    | unreserved_keyword_normal
    ;

auditing_by_user_clause
    : BY audit_user_list
    ;

op_audit_tail_clause
    : empty
    | audit_by_session_access_option audit_whenever_option?
    | audit_whenever_option
    ;

audit_by_session_access_option
    : BY ACCESS
    ;

audit_whenever_option
    : WHENEVER NOT? SUCCESSFUL
    ;

audit_all_shortcut
    : ALTER SYSTEM?
    | CLUSTER
    | CONTEXT
    | MATERIALIZED? VIEW
    | NOT EXISTS
    | OUTLINE
    | EXECUTE? PROCEDURE
    | PROFILE
    | SESSION
    | SYSTEM? AUDIT
    | SYSTEM? GRANT
    | ALTER? TABLE
    | TABLESPACE
    | TRIGGER
    | GRANT? TYPE
    | USER
    | COMMENT TABLE?
    | DELETE TABLE?
    | GRANT PROCEDURE
    | GRANT TABLE
    | INSERT TABLE?
    | SELECT TABLE?
    | UPDATE TABLE?
    | EXECUTE
    | FLASHBACK
    | INDEX
    | RENAME
    ;

rename_table_stmt
    : RENAME TABLE rename_table_actions
    ;

rename_table_actions
    : rename_table_action (Comma rename_table_action)*
    ;

rename_table_action
    : relation_factor TO relation_factor
    ;

alter_table_stmt
    : ALTER EXTERNAL? TABLE relation_factor alter_table_actions?
    | ALTER TABLE relation_factor alter_column_group_option
    ;

alter_table_actions
    : alter_table_action
    | alter_table_actions Comma alter_table_action
    ;

alter_table_action
    : SET? table_option_list_space_seperated
    | CONVERT TO CHARACTER SET charset_name collation?
    | alter_column_option
    | alter_tablegroup_option
    | RENAME TO? relation_factor
    | alter_index_option
    | alter_partition_option
    | alter_constraint_option
    | REFRESH
    ;

alter_constraint_option
    : ADD out_of_line_constraint
    | ADD LeftParen out_of_line_constraint RightParen
    | DROP (CHECK | CONSTRAINT) LeftParen name_list RightParen
    | DROP (CHECK | CONSTRAINT) constraint_name
    | DROP FOREIGN KEY index_name
    | DROP PRIMARY KEY
    | ALTER (CHECK | CONSTRAINT) constraint_name check_state
    ;

alter_partition_option
    : DROP (PARTITION|SUBPARTITION) drop_partition_name_list
    | ADD PARTITION opt_partition_range_or_list
    | modify_partition_info
    | REORGANIZE PARTITION name_list INTO opt_partition_range_or_list
    | TRUNCATE (PARTITION|SUBPARTITION) name_list
    | REMOVE PARTITIONING
    ;

opt_partition_range_or_list
    : opt_range_partition_list
    | opt_list_partition_list
    ;

alter_tg_partition_option
    : DROP (PARTITION|SUBPARTITION) drop_partition_name_list
    | ADD PARTITION opt_partition_range_or_list
    | modify_tg_partition_info
    | REORGANIZE PARTITION name_list INTO opt_partition_range_or_list
    | TRUNCATE PARTITION name_list
    ;

drop_partition_name_list
    : name_list
    | LeftParen name_list RightParen
    ;

modify_partition_info
    : hash_partition_option
    | key_partition_option
    | range_partition_option
    | list_partition_option
    ;

modify_tg_partition_info
    : tg_hash_partition_option
    | tg_key_partition_option
    | tg_range_partition_option
    | tg_list_partition_option
    ;

alter_index_option
    : ADD out_of_line_index
    | ADD LeftParen out_of_line_index RightParen
    | DROP key_or_index index_name
    | ALTER INDEX index_name (parallel_option|visibility_option)
    | RENAME key_or_index index_name TO index_name
    ;

visibility_option
    : VISIBLE
    | INVISIBLE
    ;

alter_column_group_option
    : (ADD|DROP) COLUMN GROUP LeftParen column_group_list RightParen
    ;

alter_column_option
    : ADD COLUMN? column_definition
    | ADD COLUMN? LeftParen column_definition_list RightParen
    | DROP column_definition_ref (CASCADE | RESTRICT)?
    | DROP COLUMN column_definition_ref (CASCADE | RESTRICT)?
    | ALTER COLUMN? column_definition_ref alter_column_behavior
    | CHANGE COLUMN? column_definition_ref column_definition
    | MODIFY COLUMN? column_definition
    | RENAME COLUMN column_definition_ref TO column_name
    ;

alter_tablegroup_option
    : DROP TABLEGROUP
    ;

alter_column_behavior
    : SET DEFAULT signed_literal
    | DROP DEFAULT
    ;

flashback_stmt
    : FLASHBACK TABLE relation_factor TO BEFORE DROP (RENAME TO relation_factor)?
    | FLASHBACK database_key database_factor TO BEFORE DROP (RENAME TO database_factor)?
    | FLASHBACK TENANT relation_name TO BEFORE DROP (RENAME TO relation_name)?
    ;

purge_stmt
    : PURGE (((INDEX|TABLE) relation_factor|(RECYCLEBIN|database_key database_factor))|TENANT relation_name)
    ;

optimize_stmt
    : OPTIMIZE TABLE table_list
    | OPTIMIZE TENANT (ALL|relation_name)
    ;

dump_memory_stmt
    : DUMP (CHUNK|ENTITY) ALL
    | DUMP ENTITY P_ENTITY COMP_EQ STRING_VALUE Comma SLOT_IDX COMP_EQ INTNUM
    | DUMP CHUNK TENANT_ID COMP_EQ INTNUM Comma CTX_ID COMP_EQ relation_name_or_string
    | DUMP CHUNK P_CHUNK COMP_EQ STRING_VALUE
    | SET OPTION LEAK_MOD COMP_EQ STRING_VALUE
    | SET OPTION LEAK_RATE COMP_EQ INTNUM
    | DUMP MEMORY LEAK
    ;

alter_system_stmt
    : ALTER SYSTEM BOOTSTRAP server_info_list
    | ALTER SYSTEM FLUSH cache_type CACHE namespace_expr? sql_id_or_schema_id_expr? databases_expr? (TENANT opt_equal_mark tenant_name_list)? flush_scope
    | ALTER SYSTEM FLUSH SQL cache_type (TENANT opt_equal_mark tenant_name_list)? flush_scope
    | ALTER SYSTEM FLUSH KVCACHE tenant_name? cache_name?
    | ALTER SYSTEM FLUSH DAG WARNINGS
    | ALTER SYSTEM FLUSH ILOGCACHE file_id?
    | ALTER SYSTEM SWITCH REPLICA ls_role ls_server_or_server_or_zone_or_tenant
    | ALTER SYSTEM SWITCH ROOTSERVICE partition_role server_or_zone
    | ALTER SYSTEM REPORT REPLICA server_or_zone?
    | ALTER SYSTEM RECYCLE REPLICA server_or_zone?
    | ALTER SYSTEM START MERGE zone_desc
    | ALTER SYSTEM suspend_or_resume MERGE tenant_list_tuple?
    | ALTER SYSTEM suspend_or_resume RECOVERY zone_desc?
    | ALTER SYSTEM CLEAR MERGE ERROR_P tenant_list_tuple?
    | ALTER SYSTEM ADD ARBITRATION SERVICE STRING_VALUE
    | ALTER SYSTEM REMOVE ARBITRATION SERVICE STRING_VALUE
    | ALTER SYSTEM REPLACE ARBITRATION SERVICE STRING_VALUE WITH STRING_VALUE
    | ALTER SYSTEM CANCEL cancel_task_type TASK STRING_VALUE
    | ALTER SYSTEM MAJOR FREEZE ((tenant_list_tuple opt_tablet_id) | (tenant_list_tuple ls opt_tablet_id) | opt_tablet_id_no_empty)? (REBUILD COLUMN GROUP)?
    | ALTER SYSTEM CHECKPOINT
    | ALTER SYSTEM MINOR FREEZE ((tenant_list_tuple opt_tablet_id) | (tenant_list_tuple ls opt_tablet_id) | opt_tablet_id_no_empty)? (SERVER opt_equal_mark LeftParen server_list RightParen)? zone_desc?
    | ALTER SYSTEM CHECKPOINT SLOG ((TENANT_ID opt_equal_mark INTNUM) | (TENANT opt_equal_mark relation_name_or_string))? ip_port
    | ALTER SYSTEM CLEAR ROOTTABLE tenant_name?
    | ALTER SYSTEM server_action SERVER server_list zone_desc?
    | ALTER SYSTEM ADD ZONE relation_name_or_string add_or_alter_zone_options
    | ALTER SYSTEM zone_action ZONE relation_name_or_string
    | ALTER SYSTEM alter_or_change_or_modify ZONE relation_name_or_string SET? add_or_alter_zone_options
    | ALTER SYSTEM REFRESH SCHEMA server_or_zone?
    | ALTER SYSTEM REFRESH MEMORY STAT server_or_zone?
    | ALTER SYSTEM WASH MEMORY FRAGMENTATION server_or_zone?
    | ALTER SYSTEM REFRESH IO CALIBRATION (STORAGE opt_equal_mark STRING_VALUE)? (CALIBRATION_INFO opt_equal_mark LeftParen calibration_info_list RightParen)? server_or_zone?
    | ALTER SYSTEM SET? alter_system_set_parameter_actions
    | ALTER SYSTEM SET_TP alter_system_settp_actions server_or_zone?
    | ALTER SYSTEM CLEAR LOCATION CACHE server_or_zone?
    | ALTER SYSTEM REMOVE BALANCE TASK (TENANT opt_equal_mark tenant_name_list)? (ZONE opt_equal_mark zone_list)? (TYPE opt_equal_mark balance_task_type)?
    | ALTER SYSTEM RELOAD GTS
    | ALTER SYSTEM RELOAD UNIT
    | ALTER SYSTEM RELOAD SERVER
    | ALTER SYSTEM RELOAD ZONE
    | ALTER SYSTEM MIGRATE UNIT opt_equal_mark INTNUM DESTINATION opt_equal_mark STRING_VALUE
    | ALTER SYSTEM CANCEL MIGRATE UNIT INTNUM
    | ALTER SYSTEM UPGRADE VIRTUAL SCHEMA
    | ALTER SYSTEM RUN JOB STRING_VALUE server_or_zone?
    | ALTER SYSTEM upgrade_action UPGRADE
    | ALTER SYSTEM RUN UPGRADE JOB STRING_VALUE tenant_list_tuple?
    | ALTER SYSTEM STOP UPGRADE JOB
    | ALTER SYSTEM upgrade_action ROLLING UPGRADE
    | ALTER SYSTEM REFRESH TIME_ZONE_INFO
    | ALTER SYSTEM ENABLE SQL THROTTLE (FOR PRIORITY COMP_LE INTNUM)? opt_sql_throttle_using_cond
    | ALTER SYSTEM DISABLE SQL THROTTLE
    | ALTER SYSTEM SET DISK VALID ip_port
    | ALTER SYSTEM SET NETWORK BANDWIDTH REGION relation_name_or_string TO relation_name_or_string conf_const
    | ALTER SYSTEM ADD RESTORE SOURCE STRING_VALUE
    | ALTER SYSTEM CLEAR RESTORE SOURCE
    | ALTER SYSTEM RECOVER TABLE
    ;

opt_sql_throttle_using_cond
    : USING sql_throttle_one_or_more_metrics
    ;

sql_throttle_one_or_more_metrics
    : sql_throttle_metric sql_throttle_one_or_more_metrics?
    ;

sql_throttle_metric
    : ((CPU|RT)|(NETWORK|QUEUE_TIME)) COMP_EQ int_or_decimal
    | (IO|LOGICAL_READS) COMP_EQ INTNUM
    ;

change_tenant_name_or_tenant_id
    : relation_name_or_string
    | TENANT_ID COMP_EQ? INTNUM
    ;

cache_type
    : ALL
    | LOCATION
    | CLOG
    | ILOG
    | COLUMN_STAT
    | BLOCK_INDEX
    | BLOCK
    | ROW
    | BLOOM_FILTER
    | SCHEMA
    | PLAN
    | AUDIT
    | PL
    | PS
    | LIB
    ;

balance_task_type
    : AUTO
    | MANUAL
    | ALL
    ;

tenant_list_tuple
    : TENANT COMP_EQ? LeftParen tenant_name_list RightParen
    | TENANT COMP_EQ? tenant_name_list
    ;

tenant_name_list
    : relation_name_or_string (Comma relation_name_or_string)*
    ;

backup_tenant_name_list
    : COMP_EQ? tenant_name_list
    ;

flush_scope
    : GLOBAL?
    ;

server_info_list
    : server_info (Comma server_info)*
    ;

server_info
    : REGION COMP_EQ? relation_name_or_string ZONE COMP_EQ? relation_name_or_string SERVER COMP_EQ? STRING_VALUE
    | ZONE COMP_EQ? relation_name_or_string SERVER COMP_EQ? STRING_VALUE
    ;

server_action
    : ADD
    | CANCEL? DELETE
    | START
    | FORCE? STOP
    | ISOLATE
    ;

server_list
    : STRING_VALUE (Comma STRING_VALUE)*
    ;

zone_action
    : DELETE
    | START
    | FORCE? STOP
    | ISOLATE
    ;

ip_port
    : SERVER COMP_EQ? STRING_VALUE
    | HOST STRING_VALUE
    ;

zone_desc
    : ZONE COMP_EQ? relation_name_or_string
    ;

policy_name
    : POLICY COMP_EQ? STRING_VALUE
    ;

server_or_zone
    : ip_port
    | zone_desc
    ;

add_or_alter_zone_option
    : REGION COMP_EQ? relation_name_or_string
    | IDC COMP_EQ? relation_name_or_string
    | ZONE_TYPE COMP_EQ? relation_name_or_string
    ;

add_or_alter_zone_options
    : add_or_alter_zone_option
    | empty
    | add_or_alter_zone_options Comma add_or_alter_zone_option
    ;

alter_or_change_or_modify
    : ALTER
    | CHANGE
    | MODIFY
    ;

partition_id_desc
    : PARTITION_ID COMP_EQ? STRING_VALUE
    ;

ls
    : LS COMP_EQ? INTNUM
    ;

ls_server_or_server_or_zone_or_tenant
    : ls ip_port tenant_name
    | ip_port tenant_name?
    | zone_desc tenant_name?
    ;

migrate_action
    : MOVE
    | COPY
    ;

change_actions
    : change_action change_actions?
    ;

change_action
    : replica_type
    | memstore_percent
    ;

replica_type
    : REPLICA_TYPE COMP_EQ? STRING_VALUE
    ;

memstore_percent
    : MEMSTORE_PERCENT COMP_EQ? INTNUM
    ;

suspend_or_resume
    : SUSPEND
    | RESUME
    ;

baseline_id_expr
    : BASELINE_ID COMP_EQ? INTNUM
    ;

sql_id_expr
    : SQL_ID COMP_EQ? STRING_VALUE
    ;

sql_id_or_schema_id_expr
    : sql_id_expr
    | SCHEMA_ID COMP_EQ? INTNUM
    ;

namespace_expr
    : NAMESPACE COMP_EQ? STRING_VALUE
    ;

baseline_asgn_factor
    : column_name COMP_EQ literal
    ;

tenant_name
    : TENANT COMP_EQ? relation_name_or_string
    ;

cache_name
    : CACHE COMP_EQ? relation_name_or_string
    ;

file_id
    : FILE_ID COMP_EQ? INTNUM
    ;

cancel_task_type
    : PARTITION MIGRATION
    | empty
    ;

alter_system_set_parameter_actions
    : alter_system_set_parameter_action (Comma alter_system_set_parameter_action)*
    ;

alter_system_set_parameter_action
    : NAME_OB COMP_EQ conf_const (COMMENT STRING_VALUE)? ((SCOPE COMP_EQ MEMORY) | (SCOPE COMP_EQ SPFILE) | (SCOPE COMP_EQ BOTH))? server_or_zone? tenant_name?
    | TABLET_SIZE COMP_EQ conf_const (COMMENT STRING_VALUE)? ((SCOPE COMP_EQ MEMORY) | (SCOPE COMP_EQ SPFILE) | (SCOPE COMP_EQ BOTH))? server_or_zone? tenant_name?
    | CLUSTER_ID COMP_EQ conf_const (COMMENT STRING_VALUE)? ((SCOPE COMP_EQ MEMORY) | (SCOPE COMP_EQ SPFILE) | (SCOPE COMP_EQ BOTH))? server_or_zone? tenant_name?
    | ROOTSERVICE_LIST COMP_EQ STRING_VALUE (COMMENT STRING_VALUE)? ((SCOPE COMP_EQ MEMORY) | (SCOPE COMP_EQ SPFILE) | (SCOPE COMP_EQ BOTH))? server_or_zone? tenant_name?
    | BACKUP_BACKUP_DEST COMP_EQ STRING_VALUE (COMMENT STRING_VALUE)? ((SCOPE COMP_EQ MEMORY) | (SCOPE COMP_EQ SPFILE) | (SCOPE COMP_EQ BOTH))? server_or_zone? tenant_name?
    | OBCONFIG_URL COMP_EQ STRING_VALUE (COMMENT STRING_VALUE)? ((SCOPE COMP_EQ MEMORY) | (SCOPE COMP_EQ SPFILE) | (SCOPE COMP_EQ BOTH))? server_or_zone? tenant_name?
    | LOG_DISK_SIZE COMP_EQ STRING_VALUE (COMMENT STRING_VALUE)? ((SCOPE COMP_EQ MEMORY) | (SCOPE COMP_EQ SPFILE) | (SCOPE COMP_EQ BOTH))? server_or_zone? tenant_name?
    | LOG_RESTORE_SOURCE COMP_EQ STRING_VALUE (COMMENT STRING_VALUE)? ((SCOPE COMP_EQ MEMORY) | (SCOPE COMP_EQ SPFILE) | (SCOPE COMP_EQ BOTH))? server_or_zone? tenant_name?
    ;

alter_system_settp_actions
    : settp_option
    | empty
    | alter_system_settp_actions Comma settp_option
    ;

settp_option
    : TP_NO COMP_EQ? INTNUM
    | TP_NAME COMP_EQ? relation_name_or_string
    | OCCUR COMP_EQ? INTNUM
    | FREQUENCY COMP_EQ? INTNUM
    | ERROR_CODE COMP_EQ? INTNUM
    | MATCH COMP_EQ? INTNUM
    ;

cluster_role
    : PRIMARY
    | STANDBY
    ;

partition_role
    : LEADER
    | FOLLOWER
    ;

ls_role
    : LEADER
    | FOLLOWER
    | DEFAULT
    ;

upgrade_action
    : BEGIN
    | END
    ;

method_opt
    : method_list
    ;

method_list
    : method+
    ;

method
    : for_all
    | for_columns
    ;

set_names_stmt
    : SET NAMES charset_name_or_default collation?
    ;

set_charset_stmt
    : SET charset_key charset_name_or_default
    ;

set_transaction_stmt
    : SET ((GLOBAL?|SESSION)|LOCAL) TRANSACTION transaction_characteristics
    ;

transaction_characteristics
    : transaction_access_mode
    | (transaction_access_mode Comma)? ISOLATION LEVEL isolation_level
    | ISOLATION LEVEL isolation_level Comma transaction_access_mode
    ;

transaction_access_mode
    : READ ONLY
    | READ WRITE
    ;

isolation_level
    : READ UNCOMMITTED
    | READ COMMITTED
    | REPEATABLE READ
    | SERIALIZABLE
    ;

opt_encrypt_key
    : empty
    | ENCRYPTED BY STRING_VALUE
    ;

create_savepoint_stmt
    : SAVEPOINT var_name
    ;

rollback_savepoint_stmt
    : ROLLBACK WORK? TO var_name
    | ROLLBACK TO SAVEPOINT var_name
    ;

release_savepoint_stmt
    : RELEASE SAVEPOINT var_name
    ;

alter_cluster_stmt
    : ALTER SYSTEM cluster_action VERIFY
    | ALTER SYSTEM cluster_action cluster_define FORCE?
    | ALTER SYSTEM alter_or_change_or_modify CLUSTER cluster_define SET? cluster_option_list
    ;

cluster_define
    : cluster_name CLUSTER_ID COMP_EQ? conf_const
    ;

cluster_option_list
    : cluster_option (Comma cluster_option_list)?
    ;

cluster_option
    : ROOTSERVICE_LIST COMP_EQ? STRING_VALUE
    | REDO_TRANSPORT_OPTIONS COMP_EQ? relation_name_or_string
    ;

cluster_action
    : ADD CLUSTER
    | REMOVE CLUSTER
    | (DISABLE|ENABLE) CLUSTER SYNCHRONIZATION
    ;

switchover_cluster_stmt
    : ALTER SYSTEM commit_switchover_clause FORCE?
    ;

commit_switchover_clause
    : COMMIT TO SWITCHOVER TO PRIMARY (WITH SESSION SHUTDOWN)?
    | COMMIT TO SWITCHOVER TO PHYSICAL STANDBY (WITH SESSION SHUTDOWN)?
    | ACTIVATE PHYSICAL STANDBY CLUSTER
    | CONVERT TO PHYSICAL STANDBY
    | FAILOVER TO cluster_define
    ;

protection_mode_stmt
    : ALTER SYSTEM SET STANDBY CLUSTER TO MAXIMIZE protection_mode_option
    ;

protection_mode_option
    : AVAILABILITY
    | PERFORMANCE
    | PROTECTION
    ;

cluster_name
    : relation_name
    | STRING_VALUE
    ;

disconnect_cluster_stmt
    : ALTER SYSTEM DISCONNECT STANDBY CLUSTER cluster_define SET CLUSTER_NAME cluster_name (OBCONFIG_URL STRING_VALUE)? FORCE? VERIFY?
    ;

var_name
    : NAME_OB
    | unreserved_keyword_normal
    | new_or_old_column_ref
    ;

new_or_old
    : NEW
    | OLD
    ;

new_or_old_column_ref
    : new_or_old Dot column_name
    ;

column_name
    : NAME_OB
    | unreserved_keyword
    ;

relation_name
    : NAME_OB
    | unreserved_keyword
    ;

function_name
    : NAME_OB
    | RANDOM
    | DUMP
    | CHARSET
    | COLLATION
    | KEY_VERSION
    | USER
    | DATABASE
    | SCHEMA
    | COALESCE
    | REPEAT
    | ROW_COUNT
    | REVERSE
    | RIGHT
    | CURRENT_USER
    | SYSTEM_USER
    | SESSION_USER
    | REPLACE
    | TRUNCATE
    | FORMAT
    | NORMAL
    ;

column_label
    : NAME_OB
    | unreserved_keyword
    ;

date_unit
    : DAY
    | DAY_HOUR
    | DAY_MICROSECOND
    | DAY_MINUTE
    | DAY_SECOND
    | HOUR
    | HOUR_MICROSECOND
    | HOUR_MINUTE
    | HOUR_SECOND
    | MICROSECOND
    | MINUTE
    | MINUTE_MICROSECOND
    | MINUTE_SECOND
    | MONTH
    | QUARTER
    | SECOND
    | SECOND_MICROSECOND
    | WEEK
    | YEAR
    | YEAR_MONTH
    ;

json_table_expr
    : JSON_TABLE LeftParen simple_expr Comma literal mock_jt_on_error_on_empty COLUMNS LeftParen jt_column_list RightParen RightParen
    ;

mock_jt_on_error_on_empty
    : empty
    ;

jt_column_list
    : json_table_column_def (Comma json_table_column_def)*
    ;

json_table_column_def
    : json_table_ordinality_column_def
    | json_table_exists_column_def
    | json_table_value_column_def
    | json_table_nested_column_def
    ;

json_table_ordinality_column_def
    : column_name FOR ORDINALITY
    ;

json_table_exists_column_def
    : column_name data_type collation? EXISTS PATH literal mock_jt_on_error_on_empty
    ;

json_table_value_column_def
    : column_name data_type collation? PATH literal opt_value_on_empty_or_error_or_mismatch
    ;

json_table_nested_column_def
    : NESTED PATH? literal COLUMNS LeftParen jt_column_list RightParen
    ;

opt_value_on_empty_or_error_or_mismatch
    : opt_on_empty_or_error opt_on_mismatch
    ;

opt_on_mismatch
    : empty
    ;

json_value_expr
    : JSON_VALUE LeftParen simple_expr Comma complex_string_literal (RETURNING cast_data_type)? TRUNCATE? ASCII? (on_empty | on_error | (on_empty on_error))? RightParen
    ;

opt_on_empty_or_error
    : empty
    | on_empty on_error?
    | on_error
    ;

on_empty
    : json_on_response ON EMPTY
    ;

on_error
    : json_on_response ON ERROR_P
    ;

json_on_response
    : ERROR_P
    | NULLX
    | DEFAULT signed_literal
    ;

opt_skip_index_type_list
    : empty
    | skip_index_type
    | opt_skip_index_type_list Comma skip_index_type
    ;

skip_index_type
    : MIN_MAX
    | SUM
    ;

unreserved_keyword
    : unreserved_keyword_normal
    | unreserved_keyword_special
    | unreserved_keyword_extra
    ;

unreserved_keyword_normal
    : ACCOUNT
    | ACTION
    | ACTIVE
    | ADDDATE
    | AFTER
    | AGAINST
    | AGGREGATE
    | ALGORITHM
    | ALWAYS
    | ANALYSE
    | ANY
    | APPROX_COUNT_DISTINCT
    | APPROX_COUNT_DISTINCT_SYNOPSIS
    | APPROX_COUNT_DISTINCT_SYNOPSIS_MERGE
    | ARCHIVELOG
    | ARBITRATION
    | ASCII
    | AT
    | AUDIT
    | AUTHORS
    | AUTO
    | AUTOEXTEND_SIZE
    | AUTO_INCREMENT
    | AUTO_INCREMENT_MODE
    | AVG
    | AVG_ROW_LENGTH
    | BACKUP
    | BACKUPSET
    | BACKUP_COPIES
    | BADFILE
    | BASE
    | BASELINE
    | BASELINE_ID
    | BASIC
    | BALANCE
    | BANDWIDTH
    | BEGIN
    | BINDING
    | BINLOG
    | BIT
    | BIT_AND
    | BIT_OR
    | BIT_XOR
    | BISON_LIST
    | BLOCK
    | BLOCK_SIZE
    | BLOCK_INDEX
    | BLOOM_FILTER
    | BOOL
    | BOOLEAN
    | BOOTSTRAP
    | BTREE
    | BYTE
    | BREADTH
    | BUCKETS
    | CACHE
    | CALIBRATION
    | CALIBRATION_INFO
    | KVCACHE
    | ILOGCACHE
    | CALC_PARTITION_ID
    | CANCEL
    | CASCADED
    | CAST
    | CATALOG_NAME
    | CHAIN
    | CHANGED
    | CHARSET
    | CHECKSUM
    | CHECKPOINT
    | CHUNK
    | CIPHER
    | CLASS_ORIGIN
    | CLEAN
    | CLEAR
    | CLIENT
    | CLOSE
    | CLOG
    | CLUSTER
    | CLUSTER_ID
    | CLUSTER_NAME
    | COALESCE
    | CODE
    | COLLATION
    | COLUMN_FORMAT
    | COLUMN_NAME
    | COLUMN_STAT
    | COLUMNS
    | COMMENT
    | COMMIT
    | COMMITTED
    | COMPACT
    | COMPLETION
    | COMPRESSED
    | COMPRESSION
    | COMPUTE
    | CONCURRENT
    | CONDENSED
    | CONNECTION
    | CONSISTENT
    | CONSISTENT_MODE
    | CONSTRAINT_CATALOG
    | CONSTRAINT_NAME
    | CONSTRAINT_SCHEMA
    | CONTAINS
    | CONTEXT
    | CONTRIBUTORS
    | COPY
    | COUNT
    | CPU
    | CREATE_TIMESTAMP
    | CTXCAT
    | CTX_ID
    | CUBE
    | CUME_DIST
    | CURDATE
    | CURRENT
    | CURSOR_NAME
    | CURTIME
    | CYCLE
    | DAG
    | DATA
    | DATABASE_ID
    | DATAFILE
    | DATA_TABLE_ID
    | DATE
    | DATE_ADD
    | DATE_SUB
    | DATETIME
    | DAY
    | DEALLOCATE
    | DECRYPTION
    | DEFAULT_AUTH
    | DEFINER
    | DELAY
    | DELAY_KEY_WRITE
    | DENSE_RANK
    | DEPTH
    | DES_KEY_FILE
    | DESCRIPTION
    | DESTINATION
    | DIAGNOSTICS
    | DIRECTORY
    | DISABLE
    | DISCARD
    | DISK
    | DISKGROUP
    | DISCONNECT
    | DO
    | DUMP
    | DUMPFILE
    | DUPLICATE
    | DUPLICATE_SCOPE
    | DYNAMIC
    | DEFAULT_TABLEGROUP
    | DEFAULT_LOB_INROW_THRESHOLD
    | EFFECTIVE
    | EMPTY
    | EMPTY_FIELD_AS_NULL
    | ENABLE
    | ENABLE_ARBITRATION_SERVICE
    | ENABLE_EXTENDED_ROWID
    | ENCODING
    | ENCRYPTED
    | ENCRYPTION
    | END
    | ENDS
    | ENFORCED
    | ENGINE_
    | ENGINES
    | ENUM
    | ENTITY
    | ERROR_CODE
    | ERROR_P
    | ERRORS
    | ESCAPE
    | ESTIMATE
    | EVENT
    | EVENTS
    | EVERY
    | EXCEPT
    | EXCHANGE
    | EXECUTE
    | EXPANSION
    | EXPIRE
    | EXPIRED
    | EXPIRE_INFO
    | EXPORT
    | EXTENDED
    | EXTENDED_NOADDR
    | EXTENT_SIZE
    | EXTERNAL
    | FAILOVER
    | EXTRACT
    | FAST
    | FAULTS
    | FLASHBACK
    | FIELDS
    | FIELD_DELIMITER
    | FIELD_OPTIONALLY_ENCLOSED_BY
    | FILEX
    | FILE_ID
    | FINAL_COUNT
    | FIRST
    | FIRST_VALUE
    | FIXED
    | FLUSH
    | FOLLOWER
    | FOLLOWING
    | FORMAT
    | FROZEN
    | FOUND
    | FRAGMENTATION
    | FREEZE
    | FREQUENCY
    | FUNCTION
    | FULL
    | GENERAL
    | GEOMETRY
    | GEOMCOLLECTION
    | GEOMETRYCOLLECTION
    | GET_FORMAT
    | GLOBAL
    | GLOBAL_NAME
    | GRANTS
    | GROUPING
    | GROUP_CONCAT
    | GTS
    | HANDLER
    | HASH
    | HELP
    | HISTOGRAM
    | HOST
    | HOSTS
    | HOUR
    | HYBRID_HIST
    | ID
    | IDC
    | IDENTIFIED
    | IGNORE_SERVER_IDS
    | ILOG
    | IMPORT
    | INDEXES
    | INDEX_TABLE_ID
    | INCR
    | INFO
    | INITIAL_SIZE
    | INNODB
    | INSERT_METHOD
    | INSTALL
    | INSTANCE
    | INTERSECT
    | INVOKER
    | INCREMENT
    | INCREMENTAL
    | IO
    | IOPS_WEIGHT
    | IO_THREAD
    | IPC
    | ISNULL
    | ISOLATION
    | ISOLATE
    | ISSUER
    | JOB
    | JSON
    | JSON_VALUE
    | JSON_ARRAYAGG
    | JSON_OBJECTAGG
    | JSON_TABLE
    | KEY_BLOCK_SIZE
    | KEY_VERSION
    | LAG
    | LATERAL
    | LANGUAGE
    | LAST
    | LAST_VALUE
    | LEAD
    | LEADER
    | LEAK
    | LEAK_MOD
    | LEAK_RATE
    | LEAVES
    | LESS
    | LEVEL
    | LINE_DELIMITER
    | LINESTRING
    | LIST_
    | LISTAGG
    | LN
    | LOB_INROW_THRESHOLD
    | LOCAL
    | LOCALITY
    | LOCKED
    | LOCKS
    | LOG
    | LOGFILE
    | LOGONLY_REPLICA_NUM
    | LOGS
    | LOG_RESTORE_SOURCE
    | MAJOR
    | MANUAL
    | MASTER
    | MASTER_AUTO_POSITION
    | MASTER_CONNECT_RETRY
    | MASTER_DELAY
    | MASTER_HEARTBEAT_PERIOD
    | MASTER_HOST
    | MASTER_LOG_FILE
    | MASTER_LOG_POS
    | MASTER_PASSWORD
    | MASTER_PORT
    | MASTER_RETRY_COUNT
    | MASTER_SERVER_ID
    | MASTER_SSL
    | MASTER_SSL_CA
    | MASTER_SSL_CAPATH
    | MASTER_SSL_CERT
    | MASTER_SSL_CIPHER
    | MASTER_SSL_CRL
    | MASTER_SSL_CRLPATH
    | MASTER_SSL_KEY
    | MASTER_USER
    | MAX
    | MAX_CONNECTIONS_PER_HOUR
    | MAX_CPU
    | LOG_DISK_SIZE
    | MAX_IOPS
    | MEMORY_SIZE
    | MAX_QUERIES_PER_HOUR
    | MAX_ROWS
    | MAX_SIZE
    | MAX_UPDATES_PER_HOUR
    | MAX_USER_CONNECTIONS
    | MEDIUM
    | MEMBER
    | MEMORY
    | MEMTABLE
    | MERGE
    | MESSAGE_TEXT
    | MEMSTORE_PERCENT
    | META
    | MICROSECOND
    | MIGRATE
    | MIGRATION
    | MIN
    | MINVALUE
    | MIN_CPU
    | MIN_IOPS
    | MINOR
    | MIN_ROWS
    | MINUTE
    | MINUS
    | MODE
    | MODIFY
    | MONTH
    | MOVE
    | MULTILINESTRING
    | MULTIPOINT
    | MULTIPOLYGON
    | MUTEX
    | MYSQL_ERRNO
    | MAX_USED_PART_ID
    | NAME
    | NAMES
    | NATIONAL
    | NCHAR
    | NDB
    | NDBCLUSTER
    | NEW
    | NEXT
    | NO
    | NOARCHIVELOG
    | NOAUDIT
    | NOCACHE
    | NOCYCLE
    | NODEGROUP
    | NOMINVALUE
    | NOMAXVALUE
    | NONE
    | NOORDER
    | NOPARALLEL
    | NORMAL
    | NOW
    | NOWAIT
    | NO_WAIT
    | NTILE
    | NTH_VALUE
    | NUMBER
    | NULL_IF_EXETERNAL
    | NULLS
    | NVARCHAR
    | OCCUR
    | OF
    | OFF
    | OFFSET
    | OLD
    | OLD_PASSWORD
    | OLD_KEY
    | OJ
    | OVER
    | OBCONFIG_URL
    | ONE
    | ONE_SHOT
    | ONLY
    | OPEN
    | OPTIONS
    | ORIG_DEFAULT
    | REMOTE_OSS
    | OUTLINE
    | OWNER
    | PACK_KEYS
    | PAGE
    | PARALLEL
    | PARAMETERS
    | PARSER
    | PARTIAL
    | PARTITION_ID
    | LS
    | PARTITIONING
    | PARTITIONS
    | PATTERN
    | PERCENT_RANK
    | PAUSE
    | PERCENTAGE
    | PHASE
    | PHYSICAL
    | PL
    | PLANREGRESS
    | PLUGIN
    | PLUGIN_DIR
    | PLUGINS
    | PLUS
    | POINT
    | POLICY
    | POLYGON
    | POOL
    | PORT
    | POSITION
    | PRECEDING
    | PREPARE
    | PRESERVE
    | PRETTY
    | PRETTY_COLOR
    | PREV
    | PRIMARY_ZONE
    | PRIVILEGES
    | PROCESS
    | PROCESSLIST
    | PROFILE
    | PROFILES
    | PROGRESSIVE_MERGE_NUM
    | PROXY
    | PS
    | PUBLIC
    | PCTFREE
    | P_ENTITY
    | P_CHUNK
    | QUARTER
    | QUERY
    | QUERY_RESPONSE_TIME
    | QUEUE_TIME
    | QUICK
    | RANK
    | READ_ONLY
    | REBUILD
    | RECOVER
    | RECOVERY
    | RECOVERY_WINDOW
    | RECURSIVE
    | RECYCLE
    | RECYCLEBIN
    | ROTATE
    | ROW_NUMBER
    | REDO_BUFFER_SIZE
    | REDOFILE
    | REDUNDANCY
    | REDUNDANT
    | REFRESH
    | REGION
    | REJECT
    | RELAY
    | RELAYLOG
    | RELAY_LOG_FILE
    | RELAY_LOG_POS
    | RELAY_THREAD
    | RELOAD
    | REMOVE
    | REORGANIZE
    | REPAIR
    | REPEATABLE
    | REPLICA
    | REPLICA_NUM
    | REPLICA_TYPE
    | REPLICATION
    | REPORT
    | RESET
    | RESOURCE
    | RESOURCE_POOL_LIST
    | RESPECT
    | RESTART
    | RESTORE
    | RESUME
    | RETURNED_SQLSTATE
    | RETURNING
    | RETURNS
    | REVERSE
    | ROLLBACK
    | ROLLING
    | ROLLUP
    | ROOT
    | ROOTSERVICE
    | ROOTSERVICE_LIST
    | ROOTTABLE
    | ROUTINE
    | ROW
    | ROW_COUNT
    | ROW_FORMAT
    | ROWS
    | RTREE
    | RUN
    | SAMPLE
    | SAVEPOINT
    | SCHEDULE
    | SCHEMA_NAME
    | SCN
    | SCOPE
    | SECOND
    | SECURITY
    | SEED
    | SEQUENCE
    | SEQUENCES
    | SERIAL
    | SERIALIZABLE
    | SERVER
    | SERVER_IP
    | SERVER_PORT
    | SERVER_TYPE
    | SERVICE
    | SESSION
    | SESSION_USER
    | SET_MASTER_CLUSTER
    | SET_SLAVE_CLUSTER
    | SET_TP
    | SHARDING
    | SHARE
    | SHUTDOWN
    | SIGNED
    | SIZE
    | SIMPLE
    | SKIP_BLANK_LINES
    | SKIP_HEADER
    | SLAVE
    | SLOW
    | SNAPSHOT
    | SOCKET
    | SOME
    | SONAME
    | SOUNDS
    | SOURCE
    | SPFILE
    | SPLIT
    | SQL_AFTER_GTIDS
    | SQL_AFTER_MTS_GAPS
    | SQL_BEFORE_GTIDS
    | SQL_BUFFER_RESULT
    | SQL_CACHE
    | SQL_ID
    | SCHEMA_ID
    | SQL_NO_CACHE
    | SQL_THREAD
    | SQL_TSI_DAY
    | SQL_TSI_HOUR
    | SQL_TSI_MINUTE
    | SQL_TSI_MONTH
    | SQL_TSI_QUARTER
    | SQL_TSI_SECOND
    | SQL_TSI_WEEK
    | SQL_TSI_YEAR
    | SRID
    | ST_ASMVT
    | STACKED
    | STANDBY
    | START
    | STARTS
    | STAT
    | STATISTICS
    | STATS_AUTO_RECALC
    | STATS_PERSISTENT
    | STATS_SAMPLE_PAGES
    | STATUS
    | STATEMENTS
    | STD
    | STDDEV
    | STDDEV_POP
    | STDDEV_SAMP
    | STOP
    | STORAGE
    | STORAGE_FORMAT_VERSION
    | STORE
    | STORING
    | STRONG
    | STRING
    | SUBCLASS_ORIGIN
    | SUBDATE
    | SUBJECT
    | SUBPARTITION
    | SUBPARTITIONS
    | SUBSTR
    | SUBSTRING
    | SUCCESSFUL
    | SUM
    | SUPER
    | SUSPEND
    | SWAPS
    | SWITCH
    | SWITCHES
    | SWITCHOVER
    | SYSTEM
    | SYSTEM_USER
    | SYSDATE
    | SLOG
    | TABLE_CHECKSUM
    | TABLE_MODE
    | TABLEGROUPS
    | TABLE_ID
    | TABLE_NAME
    | TABLES
    | TABLESPACE
    | TABLET
    | TABLET_ID
    | TABLET_SIZE
    | TABLET_MAX_SIZE
    | TASK
    | TEMPLATE
    | TEMPORARY
    | TEMPTABLE
    | TENANT
    | TENANT_ID
    | SLOT_IDX
    | TEXT
    | THAN
    | TIME
    | TIMESTAMP
    | TIMESTAMPADD
    | TIMESTAMPDIFF
    | TIME_ZONE_INFO
    | TP_NAME
    | TP_NO
    | TRACE
    | TRANSACTION
    | TRADITIONAL
    | TRIGGERS
    | TRIM
    | TRIM_SPACE
    | TRUNCATE
    | TYPE
    | TYPES
    | TABLEGROUP_ID
    | TOP_K_FRE_HIST
    | UNCOMMITTED
    | UNDEFINED
    | UNDO_BUFFER_SIZE
    | UNDOFILE
    | UNICODE
    | UNKNOWN
    | UNINSTALL
    | UNIT
    | UNIT_GROUP
    | UNIT_NUM
    | UNLOCKED
    | UNTIL
    | UNUSUAL
    | UPGRADE
    | USE_BLOOM_FILTER
    | USE_FRM
    | USER
    | USER_RESOURCES
    | UNBOUNDED
    | UNLIMITED
    | VALID
    | VALIDATE
    | VALUE
    | VARIANCE
    | VARIABLES
    | VAR_POP
    | VAR_SAMP
    | VERBOSE
    | VIRTUAL_COLUMN_ID
    | MATERIALIZED
    | VIEW
    | VERIFY
    | WAIT
    | WARNINGS
    | WASH
    | WEAK
    | WEEK
    | WEIGHT_STRING
    | WHENEVER
    | WINDOW
    | WORK
    | WRAPPER
    | X509
    | XA
    | XML
    | YEAR
    | ZONE
    | ZONE_LIST
    | ZONE_TYPE
    | LOCATION
    | PLAN
    | VISIBLE
    | INVISIBLE
    | ACTIVATE
    | SYNCHRONIZATION
    | THROTTLE
    | PRIORITY
    | RT
    | NETWORK
    | LOGICAL_READS
    | REDO_TRANSPORT_OPTIONS
    | MAXIMIZE
    | AVAILABILITY
    | PERFORMANCE
    | PROTECTION
    | OBSOLETE
    | HIDDEN_
    | INDEXED
    | SKEWONLY
    | BACKUPPIECE
    | PREVIEW
    | BACKUP_BACKUP_DEST
    | BACKUPROUND
    | UP
    | TIMES
    | BACKED
    | NAMESPACE
    | LIB
    | LINK
    | MY_NAME
    | CONNECT
    | STATEMENT_ID
    | KV_ATTRIBUTES
    | OBJECT_ID
    | TRANSFER
    ;

unreserved_keyword_special
    : PASSWORD
    ;

unreserved_keyword_extra
    : ACCESS
    ;

mysql_reserved_keyword
    : ACCESSIBLE
    | ADD
    | ALTER
    | ANALYZE
    | AND
    | AS
    | ASC
    | ASENSITIVE
    | BEFORE
    | BETWEEN
    | BIGINT
    | BINARY
    | BLOB
    | BY
    | CALL
    | CASCADE
    | CASE
    | CHANGE
    | CHAR
    | CHARACTER
    | CHECK
    | COLLATE
    | COLUMN
    | CONDITION
    | CONSTRAINT
    | CONTINUE
    | CONVERT
    | CREATE
    | CROSS
    | CURRENT_DATE
    | CURRENT_TIME
    | CURRENT_TIMESTAMP
    | CURRENT_USER
    | CURSOR
    | DATABASE
    | DATABASES
    | DAY_HOUR
    | DAY_MICROSECOND
    | DAY_MINUTE
    | DAY_SECOND
    | DECLARE
    | DECIMAL
    | DEFAULT
    | DELAYED
    | DELETE
    | DESC
    | DESCRIBE
    | DETERMINISTIC
    | DISTINCTROW
    | DIV
    | DOUBLE
    | DROP
    | DUAL
    | EACH
    | ELSE
    | ELSEIF
    | ENCLOSED
    | ESCAPED
    | EXISTS
    | EXIT
    | EXPLAIN
    | FETCH
    | FLOAT
    | FLOAT4
    | FLOAT8
    | FOR
    | FORCE
    | FOREIGN
    | FULLTEXT
    | GENERATED
    | GET
    | GRANT
    | GROUP
    | HAVING
    | HIGH_PRIORITY
    | HOUR_MICROSECOND
    | HOUR_MINUTE
    | HOUR_SECOND
    | IF
    | IGNORE
    | IN
    | INDEX
    | INFILE
    | INNER
    | INOUT
    | INSENSITIVE
    | INSERT
    | INT
    | INT1
    | INT2
    | INT3
    | INT4
    | INT8
    | INTEGER
    | INTERVAL
    | INTO
    | IO_AFTER_GTIDS
    | IO_BEFORE_GTIDS
    | IS
    | ITERATE
    | JOIN
    | KEY
    | KEYS
    | KILL
    | LEAVE
    | LEFT
    | LIKE
    | LIMIT
    | LINEAR
    | LINES
    | LOAD
    | LOCALTIME
    | LOCALTIMESTAMP
    | LONG
    | LONGBLOB
    | LONGTEXT
    | LOOP
    | LOW_PRIORITY
    | MASTER_BIND
    | MASTER_SSL_VERIFY_SERVER_CERT
    | MATCH
    | MAXVALUE
    | MEDIUMBLOB
    | MEDIUMINT
    | MEDIUMTEXT
    | MIDDLEINT
    | MINUTE_MICROSECOND
    | MINUTE_SECOND
    | MOD
    | MODIFIES
    | NATURAL
    | NOT
    | NO_WRITE_TO_BINLOG
    | NUMERIC
    | ON
    | OPTIMIZE
    | OPTION
    | OPTIONALLY
    | OR
    | ORDER
    | OUT
    | OUTER
    | OUTFILE
    | PARTITION
    | PRECISION
    | PRIMARY
    | PROCEDURE
    | PURGE
    | RANGE
    | READ
    | READS
    | READ_WRITE
    | REAL
    | REFERENCES
    | REGEXP
    | RELEASE
    | RENAME
    | REPEAT
    | REPLACE
    | REQUIRE
    | RESIGNAL
    | RESTRICT
    | RETURN
    | REVOKE
    | RIGHT
    | RLIKE
    | SCHEMA
    | SCHEMAS
    | SECOND_MICROSECOND
    | SENSITIVE
    | SEPARATOR
    | SET
    | SHOW
    | SIGNAL
    | SMALLINT
    | SPATIAL
    | SPECIFIC
    | SQL
    | SQLEXCEPTION
    | SQLSTATE
    | SQLWARNING
    | SQL_BIG_RESULT
    | SQL_SMALL_RESULT
    | SSL
    | STARTING
    | STORED
    | STRAIGHT_JOIN
    | TABLE
    | TERMINATED
    | THEN
    | TINYBLOB
    | TINYINT
    | TINYTEXT
    | TO
    | TRIGGER
    | UNDO
    | UNION
    | UNLOCK
    | UNSIGNED
    | UPDATE
    | USAGE
    | USE
    | USING
    | UTC_DATE
    | UTC_TIME
    | UTC_TIMESTAMP
    | VALUES
    | VARBINARY
    | VARCHAR
    | VARCHARACTER
    | VARYING
    | VIRTUAL
    | WHERE
    | WHILE
    | WITH
    | WRITE
    | XOR
    | YEAR_MONTH
    | ZEROFILL
    ;

empty
    :
    ;

forward_expr
    : expr EOF
    ;

forward_sql_stmt
    : stmt EOF
    ;


