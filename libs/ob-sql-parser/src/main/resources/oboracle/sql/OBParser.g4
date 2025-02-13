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
    | merge_stmt
    | create_table_stmt
    | alter_database_stmt
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
    | call_stmt
    | alter_table_stmt
    | alter_index_stmt
    | alter_system_stmt
    | audit_stmt
    | deallocate_prepare_stmt
    | create_user_stmt
    | alter_user_stmt
    | alter_user_profile_stmt
    | drop_user_stmt
    | set_password_stmt
    | lock_user_stmt
    | grant_stmt
    | revoke_stmt
    | begin_stmt
    | commit_stmt
    | rollback_stmt
    | create_index_stmt
    | drop_index_stmt
    | kill_stmt
    | create_mlog_stmt
    | drop_mlog_stmt
    | help_stmt
    | create_view_stmt
    | create_mview_stmt
    | create_tenant_stmt
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
    | create_synonym_stmt
    | drop_synonym_stmt
    | create_directory_stmt
    | drop_directory_stmt
    | create_keystore_stmt
    | alter_keystore_stmt
    | create_tablespace_stmt
    | drop_tablespace_stmt
    | alter_tablespace_stmt
    | create_savepoint_stmt
    | rollback_savepoint_stmt
    | lock_tables_stmt
    | lock_table_stmt
    | unlock_tables_stmt
    | flashback_stmt
    | purge_stmt
    | create_sequence_stmt
    | alter_sequence_stmt
    | drop_sequence_stmt
    | alter_session_stmt
    | analyze_stmt
    | set_comment_stmt
    | pl_expr_stmt
    | shrink_space_stmt
    | load_data_stmt
    | create_dblink_stmt
    | drop_dblink_stmt
    | create_role_stmt
    | drop_role_stmt
    | alter_role_stmt
    | set_role_stmt
    | create_profile_stmt
    | alter_profile_stmt
    | drop_profile_stmt
    | method_opt
    | drop_package_stmt
    | drop_procedure_stmt
    | drop_function_stmt
    | drop_trigger_stmt
    | drop_type_stmt
    | create_context_stmt
    | drop_context_stmt
    | switchover_tenant_stmt
    | recover_tenant_stmt
    | transfer_partition_stmt
    | service_name_stmt
    ;

drop_package_stmt
    : DROP PACKAGE BODY? relation_factor
    ;

drop_procedure_stmt
    : DROP PROCEDURE (IF EXISTS)? relation_factor
    ;

drop_function_stmt
    : DROP FUNCTION (IF EXISTS)? relation_factor
    ;

drop_trigger_stmt
    : DROP TRIGGER relation_factor
    ;

drop_type_stmt
    : DROP TYPE BODY? relation_factor (FORCE | VALIDATE)?
    ;

pl_expr_stmt
    : DO expr
    ;

expr_list
    : bit_expr (Comma bit_expr)*
    ;

column_ref
    : column_name
    | LEVEL
    | ROWNUM
    | oracle_pl_non_reserved_words
    ;

oracle_pl_non_reserved_words
    : ACCESS
    | ADD
    | AUDIT
    | CHAR
    | COLUMN
    | COMMENT
    | CURRENT
    | DATE
    | DECIMAL
    | FILE_KEY
    | FLOAT
    | IMMEDIATE
    | INCREMENT
    | INITIAL_
    | INTEGER
    | LONG
    | MAXEXTENTS
    | MODIFY
    | NOAUDIT
    | NOTFOUND
    | NUMBER
    | OFFLINE
    | ONLINE
    | PCTFREE
    | PRIVILEGES
    | RAW
    | RENAME
    | ROW
    | ROWLABEL
    | ROWS
    | SESSION
    | SET
    | SMALLINT
    | SUCCESSFUL
    | SYNONYM
    | TRIGGER
    | VALIDATE
    | VARCHAR
    | VARCHAR2
    | WHENEVER
    | DUAL
    ;

complex_string_literal
    : STRING_VALUE
    ;

js_literal
    : literal
    ;

literal
    : complex_string_literal
    | DATE_VALUE
    | TIMESTAMP_VALUE
    | INTNUM
    | APPROXNUM
    | DECIMAL_VAL
    | NULLX
    | INTERVAL_VALUE
    ;

number_literal
    : INTNUM
    | DECIMAL_VAL
    ;

expr_const
    : literal
    | SYSTEM_VARIABLE
    | QUESTIONMARK
    | (GLOBAL_ALIAS|SESSION_ALIAS) Dot column_name
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
    | (GLOBAL_ALIAS|SESSION_ALIAS) Dot column_name
    ;

bool_pri
    : bit_expr IS not? (NULLX|is_nan_inf_value)
    | bit_expr IS NOT? (JSON FORMAT)? is_json_constrain
    | bit_expr ((((COMP_EQ SOME|COMP_NSEQ)|(COMP_LE|COMP_LT) SOME)|((COMP_GE|COMP_GT? COMP_EQ)|(COMP_LE|COMP_LT COMP_EQ?)) sub_query_flag?)|(((COMP_GE|COMP_GT) SOME|(COMP_NE SOME|COMP_NE_PL))|((COMP_LT? COMP_GT|COMP_NE)|(Caret|Not) COMP_EQ) sub_query_flag?)) bit_expr
    | predicate
    ;

is_json_constrain
    : JSON strict_opt? ((scalars_opt unique_keys_opt?)?|unique_keys_opt scalars_opt?)
    | JSON LeftParen strict_opt scalars_opt? unique_keys_opt? RightParen
    | JSON LeftParen strict_opt? unique_keys_opt scalars_opt RightParen
    | JSON LeftParen scalars_opt strict_opt? unique_keys_opt? RightParen
    | JSON scalars_opt strict_opt unique_keys_opt?
    | JSON scalars_opt? unique_keys_opt strict_opt
    | JSON LeftParen scalars_opt? unique_keys_opt strict_opt RightParen
    | JSON LeftParen unique_keys_opt ((strict_opt scalars_opt)?|scalars_opt strict_opt) RightParen
    | JSON unique_keys_opt strict_opt scalars_opt
    | JSON unique_keys_opt scalars_opt strict_opt
    ;

strict_opt
    : LAX
    | STRICT
    ;

scalars_opt
    : ALLOW SCALARS
    | DISALLOW SCALARS
    ;

unique_keys_opt
    : (WITH|WITHOUT) UNIQUE KEYS
    ;

json_equal_option
    : (BOOL_VALUE|ERROR_P) ON ERROR_P
    ;

predicate
    : LNNVL LeftParen bool_pri RightParen
    | bit_expr not? IN in_expr
    | bit_expr not? BETWEEN bit_expr AND bit_expr
    | bit_expr not? LIKE bit_expr (ESCAPE bit_expr)?
    | REGEXP_LIKE LeftParen substr_params RightParen
    | exists_function_name select_with_parens
    | collection_predicate_expr
    | updating_func
    ;

collection_predicate_expr
    : bit_expr MEMBER OF? bit_expr
    | bit_expr NOT MEMBER OF? bit_expr
    | bit_expr SUBMULTISET OF? bit_expr
    | bit_expr NOT SUBMULTISET OF? bit_expr
    | bit_expr IS A_ SET
    | bit_expr IS NOT A_ SET
    | bit_expr IS EMPTY
    | bit_expr IS NOT EMPTY
    ;

bit_expr
    : bit_expr Plus bit_expr
    | bit_expr Minus bit_expr
    | bit_expr Star bit_expr
    | bit_expr Div bit_expr
    | bit_expr CNNOP bit_expr
    | bit_expr AT TIME ZONE bit_expr
    | bit_expr AT LOCAL
    | bit_expr MULTISET_OP (ALL | DISTINCT)? bit_expr
    | bit_expr POW_PL bit_expr
    | bit_expr MOD bit_expr
    | unary_expr
    | BOOL_VALUE
    ;

is_nan_inf_value
    : NAN_VALUE
    | INFINITE_VALUE
    ;

unary_expr
    : Plus? simple_expr
    | Minus simple_expr
    ;

simple_expr
    : simple_expr collation
    | obj_access_ref COLUMN_OUTER_JOIN_SYMBOL
    | expr_const
    | select_with_parens
    | CURSOR LeftParen select_stmt RightParen
    | LeftParen bit_expr RightParen
    | LeftParen expr_list Comma bit_expr RightParen
    | MATCH LeftParen column_list RightParen AGAINST LeftParen STRING_VALUE ((IN NATURAL LANGUAGE MODE) | (IN BOOLEAN MODE))? RightParen
    | case_expr
    | obj_access_ref
    | sql_function
    | cursor_attribute_expr
    | window_function
    | USER_VARIABLE
    | PLSQL_VARIABLE
    | PRIOR unary_expr
    | CONNECT_BY_ROOT unary_expr
    | SET LeftParen bit_expr RightParen
    | MULTISET select_with_parens
    | column_ref Dot column_ref USER_VARIABLE
    | column_ref Dot column_ref Dot column_ref USER_VARIABLE
    | dblink_func_expr
    | {this.is_pl_parse_}? QUESTIONMARK Dot column_name
    ;

json_function
    : json_value_expr
    | json_query_expr
    | json_mergepatch_expr
    | json_array_expr
    | json_exists_expr
    | json_object_expr
    | json_table_expr
    | json_equal_expr
    ;

gis_function
    : spatial_cellid_expr
    | spatial_mbr_expr
    | sdo_relate_expr
    ;

spatial_cellid_expr
    : SPATIAL_CELLID LeftParen bit_expr RightParen
    ;

spatial_mbr_expr
    : SPATIAL_MBR LeftParen bit_expr RightParen
    ;

sdo_relate_expr
    : SDO_RELATE LeftParen bit_expr Comma bit_expr Comma bit_expr RightParen
    ;

common_cursor_attribute
    : ISOPEN
    | FOUND
    | NOTFOUND
    | ROWCOUNT
    ;

cursor_attribute_bulk_rowcount
    : BULK_ROWCOUNT LeftParen bit_expr RightParen
    ;

cursor_attribute_bulk_exceptions
    : BULK_EXCEPTIONS Dot COUNT
    | BULK_EXCEPTIONS LeftParen bit_expr RightParen Dot (ERROR_CODE|ERROR_INDEX)
    ;

implicit_cursor_attribute
    : SQL Mod ((common_cursor_attribute|cursor_attribute_bulk_rowcount)|cursor_attribute_bulk_exceptions)
    ;

explicit_cursor_attribute
    : obj_access_ref Mod common_cursor_attribute
    ;

cursor_attribute_expr
    : explicit_cursor_attribute
    | implicit_cursor_attribute
    ;

obj_access_ref
    : column_ref ((Dot obj_access_ref) | (Dot Star))?
    | access_func_expr func_access_ref?
    | QUESTIONMARK func_access_ref
    | column_ref Dot FIRST LeftParen RightParen
    | column_ref Dot LAST LeftParen RightParen
    | column_ref Dot COUNT LeftParen RightParen
    | column_ref dot_notation_path
    | dot_notation_fun_sys
    ;

dot_notation_path
    : LeftBracket path_param_array RightBracket dot_notation_path_obj_access_ref
    ;

dot_notation_path_obj_access_ref
    : empty
    | Dot obj_access_ref
    | dot_notation_path
    ;

path_param_array
    : Star (Comma path_param_list)?
    | path_param_list
    ;

path_param_list
    : path_param (Comma path_param)*
    ;

path_param
    : INTNUM (TO path_param)?
    ;

dot_notation_fun_sys
    : dot_notation_fun
    ;

dot_notation_fun
    : func_name=(DATE|SIZE|NUMBER) LeftParen RightParen
    ;

obj_access_ref_normal
    : pl_var_name (Dot obj_access_ref_normal)?
    | access_func_expr_count ((Dot obj_access_ref_normal) | table_element_access_list)?
    | var_name LeftParen func_param_list? RightParen ((Dot obj_access_ref_normal) | table_element_access_list)?
    | PRIOR LeftParen func_param_list? RightParen ((Dot obj_access_ref_normal) | table_element_access_list)?
    | exists_function_name LeftParen func_param_list? RightParen ((Dot obj_access_ref_normal) | table_element_access_list)?
    ;

func_access_ref
    : table_element_access_list? Dot obj_access_ref
    | table_element_access_list
    ;

table_element_access_list
    : LeftParen table_index RightParen
    | table_element_access_list LeftParen table_index RightParen
    ;

table_index
    : bit_expr
    ;

expr
    : NOT expr
    | (USER_VARIABLE set_var_op)? bit_expr
    | bool_pri
    | LeftParen expr RightParen
    | expr (AND|OR) expr
    ;

not
    : NOT
    ;

sub_query_flag
    : ALL
    | ANY
    ;

in_expr
    : bit_expr
    ;

case_expr
    : CASE (bit_expr simple_when_clause_list|bool_when_clause_list) case_default END CASE?
    ;

window_function
    : func_name=COUNT LeftParen ALL? Star RightParen OVER LeftParen generalized_window_clause RightParen
    | func_name=COUNT LeftParen ALL? bit_expr RightParen OVER LeftParen generalized_window_clause RightParen
    | func_name=COUNT LeftParen DISTINCT bit_expr RightParen OVER LeftParen generalized_window_clause RightParen
    | func_name=APPROX_COUNT_DISTINCT LeftParen expr_list RightParen OVER LeftParen generalized_window_clause RightParen
    | func_name=APPROX_COUNT_DISTINCT_SYNOPSIS LeftParen expr_list RightParen OVER LeftParen generalized_window_clause RightParen
    | func_name=APPROX_COUNT_DISTINCT_SYNOPSIS_MERGE LeftParen bit_expr RightParen OVER LeftParen generalized_window_clause RightParen
    | func_name=SUM LeftParen (ALL | DISTINCT | UNIQUE)? bit_expr RightParen OVER LeftParen generalized_window_clause RightParen
    | func_name=MAX LeftParen (ALL | DISTINCT | UNIQUE)? bit_expr RightParen OVER LeftParen generalized_window_clause RightParen
    | func_name=MIN LeftParen (ALL | DISTINCT | UNIQUE)? bit_expr RightParen OVER LeftParen generalized_window_clause RightParen
    | func_name=AVG LeftParen (ALL | DISTINCT | UNIQUE)? bit_expr RightParen OVER LeftParen generalized_window_clause RightParen
    | func_name=MEDIAN LeftParen (ALL | DISTINCT | UNIQUE)? bit_expr RightParen OVER LeftParen generalized_window_clause RightParen
    | func_name=STDDEV LeftParen (ALL | DISTINCT | UNIQUE)? bit_expr RightParen OVER LeftParen generalized_window_clause RightParen
    | func_name=VARIANCE LeftParen (ALL | DISTINCT | UNIQUE)? bit_expr RightParen OVER LeftParen generalized_window_clause RightParen
    | func_name=STDDEV_POP LeftParen (ALL | DISTINCT | UNIQUE)? bit_expr RightParen OVER LeftParen generalized_window_clause RightParen
    | func_name=STDDEV_SAMP LeftParen (ALL | DISTINCT | UNIQUE)? bit_expr RightParen OVER LeftParen generalized_window_clause RightParen
    | func_name=LISTAGG LeftParen (ALL | DISTINCT | UNIQUE)? expr_list RightParen WITHIN GROUP LeftParen order_by RightParen OVER LeftParen generalized_window_clause RightParen
    | func_name=LISTAGG LeftParen (ALL | DISTINCT | UNIQUE)? expr_list RightParen OVER LeftParen generalized_window_clause RightParen
    | func_name=RANK LeftParen RightParen OVER LeftParen generalized_window_clause RightParen
    | func_name=DENSE_RANK LeftParen RightParen OVER LeftParen generalized_window_clause RightParen
    | func_name=PERCENT_RANK LeftParen RightParen OVER LeftParen generalized_window_clause RightParen
    | func_name=ROW_NUMBER LeftParen RightParen OVER LeftParen generalized_window_clause RightParen
    | func_name=NTILE LeftParen bit_expr RightParen OVER LeftParen generalized_window_clause RightParen
    | func_name=CUME_DIST LeftParen RightParen OVER LeftParen generalized_window_clause RightParen
    | func_name=FIRST_VALUE win_fun_first_last_params OVER LeftParen generalized_window_clause RightParen
    | func_name=LAST_VALUE win_fun_first_last_params OVER LeftParen generalized_window_clause RightParen
    | func_name=LEAD win_fun_lead_lag_params OVER LeftParen generalized_window_clause RightParen
    | func_name=LAG win_fun_lead_lag_params OVER LeftParen generalized_window_clause RightParen
    | func_name=NTH_VALUE LeftParen bit_expr Comma bit_expr RightParen (FROM first_or_last)? (respect_or_ignore NULLS)? OVER LeftParen generalized_window_clause RightParen
    | func_name=RATIO_TO_REPORT LeftParen (ALL | DISTINCT | UNIQUE)? bit_expr RightParen OVER LeftParen generalized_window_clause RightParen
    | func_name=CORR LeftParen (ALL | DISTINCT | UNIQUE)? bit_expr Comma bit_expr RightParen OVER LeftParen generalized_window_clause RightParen
    | func_name=COVAR_POP LeftParen (ALL | DISTINCT | UNIQUE)? bit_expr Comma bit_expr RightParen OVER LeftParen generalized_window_clause RightParen
    | func_name=COVAR_SAMP LeftParen (ALL | DISTINCT | UNIQUE)? bit_expr Comma bit_expr RightParen OVER LeftParen generalized_window_clause RightParen
    | func_name=VAR_POP LeftParen (ALL | DISTINCT | UNIQUE)? bit_expr RightParen OVER LeftParen generalized_window_clause RightParen
    | func_name=VAR_SAMP LeftParen (ALL | DISTINCT | UNIQUE)? bit_expr RightParen OVER LeftParen generalized_window_clause RightParen
    | func_name=REGR_SLOPE LeftParen (ALL | DISTINCT | UNIQUE)? bit_expr Comma bit_expr RightParen OVER LeftParen generalized_window_clause RightParen
    | func_name=REGR_INTERCEPT LeftParen (ALL | DISTINCT | UNIQUE)? bit_expr Comma bit_expr RightParen OVER LeftParen generalized_window_clause RightParen
    | func_name=REGR_COUNT LeftParen (ALL | DISTINCT | UNIQUE)? bit_expr Comma bit_expr RightParen OVER LeftParen generalized_window_clause RightParen
    | func_name=REGR_R2 LeftParen (ALL | DISTINCT | UNIQUE)? bit_expr Comma bit_expr RightParen OVER LeftParen generalized_window_clause RightParen
    | func_name=REGR_AVGX LeftParen (ALL | DISTINCT | UNIQUE)? bit_expr Comma bit_expr RightParen OVER LeftParen generalized_window_clause RightParen
    | func_name=REGR_AVGY LeftParen (ALL | DISTINCT | UNIQUE)? bit_expr Comma bit_expr RightParen OVER LeftParen generalized_window_clause RightParen
    | func_name=REGR_SXX LeftParen (ALL | DISTINCT | UNIQUE)? bit_expr Comma bit_expr RightParen OVER LeftParen generalized_window_clause RightParen
    | func_name=REGR_SYY LeftParen (ALL | DISTINCT | UNIQUE)? bit_expr Comma bit_expr RightParen OVER LeftParen generalized_window_clause RightParen
    | func_name=REGR_SXY LeftParen (ALL | DISTINCT | UNIQUE)? bit_expr Comma bit_expr RightParen OVER LeftParen generalized_window_clause RightParen
    | func_name=MAX LeftParen (ALL | DISTINCT | UNIQUE)? bit_expr RightParen KEEP LeftParen DENSE_RANK first_or_last order_by RightParen OVER LeftParen generalized_window_clause RightParen
    | func_name=MIN LeftParen (ALL | DISTINCT | UNIQUE)? bit_expr RightParen KEEP LeftParen DENSE_RANK first_or_last order_by RightParen OVER LeftParen generalized_window_clause RightParen
    | func_name=SUM LeftParen (ALL | DISTINCT | UNIQUE)? bit_expr RightParen KEEP LeftParen DENSE_RANK first_or_last order_by RightParen OVER LeftParen generalized_window_clause RightParen
    | func_name=COUNT LeftParen ALL? Star RightParen KEEP LeftParen DENSE_RANK first_or_last order_by RightParen OVER LeftParen generalized_window_clause RightParen
    | func_name=COUNT LeftParen ALL? bit_expr RightParen KEEP LeftParen DENSE_RANK first_or_last order_by RightParen OVER LeftParen generalized_window_clause RightParen
    | func_name=AVG LeftParen (ALL | DISTINCT | UNIQUE)? bit_expr RightParen KEEP LeftParen DENSE_RANK first_or_last order_by RightParen OVER LeftParen generalized_window_clause RightParen
    | func_name=VARIANCE LeftParen (ALL | DISTINCT | UNIQUE)? bit_expr RightParen KEEP LeftParen DENSE_RANK first_or_last order_by RightParen OVER LeftParen generalized_window_clause RightParen
    | func_name=STDDEV LeftParen (ALL | DISTINCT | UNIQUE)? bit_expr RightParen KEEP LeftParen DENSE_RANK first_or_last order_by RightParen OVER LeftParen generalized_window_clause RightParen
    | func_name=PERCENTILE_CONT LeftParen (ALL | DISTINCT | UNIQUE)? expr_list RightParen WITHIN GROUP LeftParen order_by RightParen OVER LeftParen generalized_window_clause RightParen
    | func_name=PERCENTILE_DISC LeftParen (ALL | DISTINCT | UNIQUE)? expr_list RightParen WITHIN GROUP LeftParen order_by RightParen OVER LeftParen generalized_window_clause RightParen
    | func_name=WM_CONCAT LeftParen (ALL | DISTINCT | UNIQUE)? bit_expr RightParen OVER LeftParen generalized_window_clause RightParen
    | func_name=WMSYS Dot sub_func_name=WM_CONCAT LeftParen (ALL | DISTINCT | UNIQUE)? bit_expr RightParen OVER LeftParen generalized_window_clause RightParen
    | func_name=WM_CONCAT LeftParen (ALL | DISTINCT | UNIQUE)? bit_expr RightParen KEEP LeftParen DENSE_RANK first_or_last order_by RightParen OVER LeftParen generalized_window_clause RightParen
    | func_name=WMSYS Dot sub_func_name=WM_CONCAT LeftParen (ALL | DISTINCT | UNIQUE)? bit_expr RightParen KEEP LeftParen DENSE_RANK first_or_last order_by RightParen OVER LeftParen generalized_window_clause RightParen
    | func_name=TOP_K_FRE_HIST LeftParen bit_expr Comma bit_expr Comma bit_expr RightParen OVER LeftParen generalized_window_clause RightParen
    | func_name=HYBRID_HIST LeftParen bit_expr Comma bit_expr RightParen OVER LeftParen generalized_window_clause RightParen
    | function_name LeftParen func_param_list RightParen OVER LeftParen generalized_window_clause RightParen
    | function_name LeftParen ALL func_param_list RightParen OVER LeftParen generalized_window_clause RightParen
    | function_name LeftParen DISTINCT func_param_list RightParen OVER LeftParen generalized_window_clause RightParen
    | function_name LeftParen UNIQUE func_param_list RightParen OVER LeftParen generalized_window_clause RightParen
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
    : LeftParen bit_expr respect_or_ignore NULLS RightParen
    | LeftParen bit_expr RightParen (respect_or_ignore NULLS)?
    ;

win_fun_lead_lag_params
    : LeftParen bit_expr respect_or_ignore NULLS RightParen
    | LeftParen bit_expr respect_or_ignore NULLS Comma expr_list RightParen
    | LeftParen expr_list RightParen (respect_or_ignore NULLS)?
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
    : bit_expr
    ;

win_bounding
    : CURRENT ROW
    | win_interval win_preceding_or_following
    ;

win_window
    : win_rows_or_range BETWEEN win_bounding AND win_bounding
    | win_rows_or_range win_bounding
    ;

simple_when_clause_list
    : simple_when_clause+
    ;

simple_when_clause
    : WHEN bit_expr THEN bit_expr
    ;

bool_when_clause_list
    : bool_when_clause+
    ;

bool_when_clause
    : WHEN expr THEN bit_expr
    ;

case_default
    : ELSE bit_expr
    | empty
    ;

sql_function
    : single_row_function
    | aggregate_function
    | special_func_expr
    ;

xml_function
    : xmlparse_expr
    | xml_element_expr
    | xml_extract_expr
    | delete_xml
    | insert_child_xml
    | xmlserialize_expr
    | xmlcast_expr
    | xml_sequence_expr
    ;

single_row_function
    : numeric_function
    | character_function
    | extract_function
    | conversion_function
    | hierarchical_function
    | environment_id_function
    | json_function
    | xml_function ((Dot obj_access_ref_normal) | table_element_access_list)?
    | gis_function
    ;

numeric_function
    : MOD LeftParen bit_expr Comma bit_expr RightParen
    ;

character_function
    : TRIM LeftParen parameterized_trim RightParen
    | ASCII LeftParen bit_expr RightParen
    | TRANSLATE LeftParen bit_expr USING translate_charset RightParen
    | TRANSLATE LeftParen bit_expr Comma bit_expr Comma bit_expr RightParen
    ;

translate_charset
    : CHAR_CS
    | NCHAR_CS
    ;

extract_function
    : EXTRACT LeftParen date_unit_for_extract FROM bit_expr RightParen
    ;

conversion_function
    : CAST LeftParen bit_expr AS cast_data_type RightParen
    | TREAT LeftParen bit_expr AS treat_data_type RightParen
    ;

hierarchical_function
    : SYS_CONNECT_BY_PATH LeftParen bit_expr Comma bit_expr RightParen
    ;

environment_id_function
    : USER
    | UID
    ;

aggregate_function
    : funcName=APPROX_COUNT_DISTINCT LeftParen DISTINCT? expr_list RightParen
    | funcName=APPROX_COUNT_DISTINCT_SYNOPSIS LeftParen expr_list RightParen
    | funcName=APPROX_COUNT_DISTINCT_SYNOPSIS_MERGE LeftParen bit_expr RightParen
    | funcName=SUM LeftParen (ALL | DISTINCT | UNIQUE)? bit_expr RightParen
    | funcName=MAX LeftParen (ALL | DISTINCT | UNIQUE)? bit_expr RightParen
    | funcName=MIN LeftParen (ALL | DISTINCT | UNIQUE)? bit_expr RightParen
    | funcName=AVG LeftParen (ALL | DISTINCT | UNIQUE)? bit_expr RightParen
    | funcName=JSON_ARRAYAGG LeftParen (ALL | DISTINCT | UNIQUE)? bit_expr (FORMAT JSON)? order_by? js_agg_on_null? js_agg_returning_type_opt? STRICT? RightParen
    | funcName=JSON_OBJECTAGG LeftParen KEY? bit_expr VALUE bit_expr (FORMAT JSON)? js_agg_on_null? js_agg_returning_type_opt? STRICT? json_obj_unique_key? RightParen
    | funcName=JSON_OBJECTAGG LeftParen bit_expr Comma bit_expr (FORMAT JSON)? js_agg_on_null? js_agg_returning_type_opt? STRICT? json_obj_unique_key? RightParen
    | funcName=MEDIAN LeftParen (ALL | DISTINCT | UNIQUE)? bit_expr RightParen
    | funcName=STDDEV LeftParen (ALL | DISTINCT | UNIQUE)? bit_expr RightParen
    | funcName=VARIANCE LeftParen (ALL | DISTINCT | UNIQUE)? bit_expr RightParen
    | funcName=STDDEV_POP LeftParen (ALL | DISTINCT | UNIQUE)? bit_expr RightParen
    | funcName=STDDEV_SAMP LeftParen (ALL | DISTINCT | UNIQUE)? bit_expr RightParen
    | funcName=GROUPING LeftParen bit_expr RightParen
    | funcName=LISTAGG LeftParen (ALL | DISTINCT | UNIQUE)? expr_list RightParen WITHIN GROUP LeftParen order_by RightParen
    | funcName=LISTAGG LeftParen (ALL | DISTINCT | UNIQUE)? expr_list RightParen
    | funcName=CORR LeftParen (ALL | DISTINCT | UNIQUE)? bit_expr Comma bit_expr RightParen
    | funcName=COVAR_POP LeftParen (ALL | DISTINCT | UNIQUE)? bit_expr Comma bit_expr RightParen
    | funcName=COVAR_SAMP LeftParen (ALL | DISTINCT | UNIQUE)? bit_expr Comma bit_expr RightParen
    | funcName=VAR_POP LeftParen (ALL | DISTINCT | UNIQUE)? bit_expr RightParen
    | funcName=VAR_SAMP LeftParen (ALL | DISTINCT | UNIQUE)? bit_expr RightParen
    | funcName=REGR_SLOPE LeftParen (ALL | DISTINCT | UNIQUE)? bit_expr Comma bit_expr RightParen
    | funcName=REGR_INTERCEPT LeftParen (ALL | DISTINCT | UNIQUE)? bit_expr Comma bit_expr RightParen
    | funcName=REGR_COUNT LeftParen (ALL | DISTINCT | UNIQUE)? bit_expr Comma bit_expr RightParen
    | funcName=REGR_R2 LeftParen (ALL | DISTINCT | UNIQUE)? bit_expr Comma bit_expr RightParen
    | funcName=REGR_AVGX LeftParen (ALL | DISTINCT | UNIQUE)? bit_expr Comma bit_expr RightParen
    | funcName=REGR_AVGY LeftParen (ALL | DISTINCT | UNIQUE)? bit_expr Comma bit_expr RightParen
    | funcName=REGR_SXX LeftParen (ALL | DISTINCT | UNIQUE)? bit_expr Comma bit_expr RightParen
    | funcName=REGR_SYY LeftParen (ALL | DISTINCT | UNIQUE)? bit_expr Comma bit_expr RightParen
    | funcName=REGR_SXY LeftParen (ALL | DISTINCT | UNIQUE)? bit_expr Comma bit_expr RightParen
    | funcName=RANK LeftParen (ALL | DISTINCT | UNIQUE)? expr_list RightParen WITHIN GROUP LeftParen order_by RightParen
    | funcName=PERCENT_RANK LeftParen (ALL | DISTINCT | UNIQUE)? expr_list RightParen WITHIN GROUP LeftParen order_by RightParen
    | funcName=DENSE_RANK LeftParen (ALL | DISTINCT | UNIQUE)? expr_list RightParen WITHIN GROUP LeftParen order_by RightParen
    | funcName=CUME_DIST LeftParen (ALL | DISTINCT | UNIQUE)? expr_list RightParen WITHIN GROUP LeftParen order_by RightParen
    | funcName=PERCENTILE_CONT LeftParen (ALL | DISTINCT | UNIQUE)? expr_list RightParen WITHIN GROUP LeftParen order_by RightParen
    | funcName=PERCENTILE_DISC LeftParen (ALL | DISTINCT | UNIQUE)? expr_list RightParen WITHIN GROUP LeftParen order_by RightParen
    | funcName=MAX LeftParen (ALL | DISTINCT | UNIQUE)? bit_expr RightParen KEEP LeftParen DENSE_RANK first_or_last order_by RightParen
    | funcName=MIN LeftParen (ALL | DISTINCT | UNIQUE)? bit_expr RightParen KEEP LeftParen DENSE_RANK first_or_last order_by RightParen
    | funcName=SUM LeftParen (ALL | DISTINCT | UNIQUE)? bit_expr RightParen KEEP LeftParen DENSE_RANK first_or_last order_by RightParen
    | funcName=AVG LeftParen (ALL | DISTINCT | UNIQUE)? bit_expr RightParen KEEP LeftParen DENSE_RANK first_or_last order_by RightParen
    | funcName=VARIANCE LeftParen (ALL | DISTINCT | UNIQUE)? bit_expr RightParen KEEP LeftParen DENSE_RANK first_or_last order_by RightParen
    | funcName=STDDEV LeftParen (ALL | DISTINCT | UNIQUE)? bit_expr RightParen KEEP LeftParen DENSE_RANK first_or_last order_by RightParen
    | funcName=WM_CONCAT LeftParen (ALL | DISTINCT | UNIQUE)? bit_expr RightParen
    | funcName=WMSYS Dot subFuncName=WM_CONCAT LeftParen (ALL | DISTINCT | UNIQUE)? bit_expr RightParen
    | funcName=WM_CONCAT LeftParen (ALL | DISTINCT | UNIQUE)? bit_expr RightParen KEEP LeftParen DENSE_RANK first_or_last order_by RightParen
    | funcName=WMSYS Dot subFuncName=WM_CONCAT LeftParen (ALL | DISTINCT | UNIQUE)? bit_expr RightParen KEEP LeftParen DENSE_RANK first_or_last order_by RightParen
    | funcName=TOP_K_FRE_HIST LeftParen bit_expr Comma bit_expr Comma bit_expr RightParen
    | funcName=HYBRID_HIST LeftParen bit_expr Comma bit_expr RightParen
    | funcName=XMLAGG LeftParen simple_expr order_by? RightParen
    | funcName=SUM_OPNSIZE LeftParen bit_expr RightParen
    ;

js_agg_on_null
    : (ABSENT | NULLX) ON NULLX
    ;

js_agg_returning_type_opt
    : RETURNING js_return_type
    | RETURNING js_agg_returning_type
    ;

js_agg_returning_type
    : RAW LeftParen zero_suffix_intnum RightParen
    | RAW
    | NVARCHAR2 nstring_length_i?
    ;

special_func_expr
    : (((DATE|ISNULL)|(TIME|YEAR))|MONTH) LeftParen bit_expr RightParen
    | cur_timestamp_func
    | INSERT LeftParen bit_expr Comma bit_expr Comma bit_expr Comma bit_expr RightParen
    | (CALC_PARTITION_ID|LEFT) LeftParen bit_expr Comma bit_expr RightParen
    | POSITION LeftParen bit_expr IN bit_expr RightParen
    | (DEFAULT|VALUES) LeftParen column_definition_ref RightParen
    | CALC_PARTITION_ID LeftParen bit_expr Comma bit_expr Comma bit_expr RightParen
    | LAST_REFRESH_SCN LeftParen INTNUM RightParen
    ;

access_func_expr_count
    : COUNT LeftParen ALL? Star RightParen
    | COUNT LeftParen ALL? bit_expr RightParen
    | COUNT LeftParen DISTINCT bit_expr RightParen
    | COUNT LeftParen UNIQUE bit_expr RightParen
    | COUNT LeftParen ALL? Star RightParen KEEP LeftParen DENSE_RANK first_or_last order_by RightParen
    | COUNT LeftParen ALL? bit_expr RightParen KEEP LeftParen DENSE_RANK first_or_last order_by RightParen
    ;

access_func_expr
    : access_func_expr_count
    | function_name LeftParen RightParen
    | function_name LeftParen func_param_list RightParen
    | NEW NAME_OB LeftParen RightParen
    | NEW NAME_OB LeftParen func_param_list RightParen
    | function_name LeftParen func_param_list opt_json_exist RightParen
    | function_name LeftParen func_param_list json_equal_option RightParen
    | aggregate_function_keyword LeftParen RightParen
    | aggregate_function_keyword LeftParen func_param_list RightParen
    | function_name LeftParen ALL func_param_list RightParen
    | function_name LeftParen DISTINCT func_param_list RightParen
    | function_name LeftParen UNIQUE func_param_list RightParen
    | exists_function_name LeftParen func_param_list? RightParen
    ;

dblink_func_expr
    : column_ref USER_VARIABLE LeftParen func_param_list? RightParen
    | column_ref Dot column_ref USER_VARIABLE LeftParen func_param_list? RightParen
    | column_ref Dot column_ref Dot column_ref USER_VARIABLE LeftParen func_param_list? RightParen
    ;

func_param_list
    : func_param (Comma func_param)*
    ;

func_param
    : func_param_with_assign
    | bit_expr
    | bool_pri_in_pl_func
    ;

func_param_with_assign
    : pl_var_name PARAM_ASSIGN_OPERATOR (bit_expr|bool_pri_in_pl_func)
    ;

pl_var_name
    : var_name
    | oracle_pl_non_reserved_words
    ;

bool_pri_in_pl_func
    : bool_pri
    | NOT bool_pri_in_pl_func
    | LeftParen bool_pri_in_pl_func RightParen
    | bool_pri_in_pl_func (AND|OR) bool_pri_in_pl_func
    ;

cur_timestamp_func
    : SYSDATE
    | ((LOCALTIMESTAMP|SYSTIMESTAMP)|CURRENT_TIMESTAMP) LeftParen INTNUM RightParen
    ;

updating_func
    : UPDATING LeftParen updating_params RightParen
    ;

updating_params
    : STRING_VALUE
    | pl_var_name
    ;

substr_params
    : bit_expr Comma bit_expr (Comma bit_expr)?
    ;

returning_log_error_clause
    : returning_clause
    | log_error_clause
    | returning_clause log_error_clause
    ;

returning_clause
    : (RETURNING | RETURN) returning_exprs opt_into_clause
    ;

log_error_clause
    : LOG ERRORS into_err_log_caluse opt_simple_expression reject_limit
    ;

delete_stmt
    : delete_with_opt_hint FROM table_factor opt_where_extension returning_log_error_clause?
    | delete_with_opt_hint table_factor ((WHERE expr) | (WHERE HINT_VALUE expr))? returning_log_error_clause?
    ;

update_stmt
    : update_with_opt_hint dml_table_clause SET update_asgn_list opt_where_extension returning_log_error_clause?
    ;

update_asgn_list
    : normal_asgn_list
    | ROW COMP_EQ obj_access_ref_normal
    ;

normal_asgn_list
    : update_asgn_factor (Comma update_asgn_factor)*
    ;

update_asgn_factor
    : column_definition_ref COMP_EQ expr_or_default
    | LeftParen column_list RightParen COMP_EQ LeftParen subquery order_by? fetch_next_clause? RightParen
    ;

create_resource_stmt
    : CREATE RESOURCE UNIT relation_name (resource_unit_option | (opt_resource_unit_option_list Comma resource_unit_option))?
    | CREATE RESOURCE POOL relation_name (create_resource_pool_option | (opt_create_resource_pool_option_list Comma create_resource_pool_option))?
    ;

opt_resource_unit_option_list
    : resource_unit_option
    | empty
    | opt_resource_unit_option_list Comma resource_unit_option
    ;

resource_unit_option
    : MIN_CPU COMP_EQ? conf_const
    | MIN_IOPS COMP_EQ? conf_const
    | MIN_MEMORY COMP_EQ? conf_const
    | MAX_CPU COMP_EQ? conf_const
    | MAX_MEMORY COMP_EQ? conf_const
    | MEMORY_SIZE COMP_EQ? conf_const
    | MAX_IOPS COMP_EQ? conf_const
    | MAX_DISK_SIZE COMP_EQ? conf_const
    | MAX_SESSION_NUM COMP_EQ? conf_const
    | IOPS_WEIGHT COMP_EQ? conf_const
    | LOG_DISK_SIZE COMP_EQ? conf_const
    ;

opt_create_resource_pool_option_list
    : create_resource_pool_option
    | empty
    | opt_create_resource_pool_option_list Comma create_resource_pool_option
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

id_list
    : INTNUM (Comma INTNUM)*
    ;

alter_resource_pool_option
    : UNIT COMP_EQ? relation_name_or_string
    | UNIT_NUM COMP_EQ? INTNUM (DELETE UNIT opt_equal_mark LeftParen id_list RightParen)?
    | ZONE_LIST COMP_EQ? LeftParen zone_list RightParen
    ;

alter_resource_stmt
    : ALTER RESOURCE UNIT relation_name (resource_unit_option | (opt_resource_unit_option_list Comma resource_unit_option))?
    | ALTER RESOURCE POOL relation_name alter_resource_pool_option_list
    | ALTER RESOURCE POOL relation_name SPLIT INTO LeftParen resource_pool_list RightParen ON LeftParen zone_list RightParen
    | ALTER RESOURCE POOL MERGE LeftParen resource_pool_list RightParen INTO LeftParen resource_pool_list RightParen
    | ALTER RESOURCE TENANT relation_name UNIT_NUM COMP_EQ? INTNUM (DELETE UNIT_GROUP opt_equal_mark LeftParen id_list RightParen)?
    ;

drop_resource_stmt
    : DROP RESOURCE (POOL|UNIT) relation_name
    ;

create_tenant_stmt
    : CREATE TENANT relation_name (tenant_option | (opt_tenant_option_list Comma tenant_option))? ((SET sys_var_and_val_list) | (SET VARIABLES sys_var_and_val_list) | (VARIABLES sys_var_and_val_list))?
    ;

opt_tenant_option_list
    : tenant_option
    | empty
    | opt_tenant_option_list Comma tenant_option
    ;

tenant_option
    : LOGONLY_REPLICA_NUM COMP_EQ? INTNUM
    | LOCALITY COMP_EQ? STRING_VALUE FORCE?
    | REPLICA_NUM COMP_EQ? INTNUM
    | REWRITE_MERGE_VERSION COMP_EQ? INTNUM
    | STORAGE_FORMAT_VERSION COMP_EQ? INTNUM
    | STORAGE_FORMAT_WORK_VERSION COMP_EQ? INTNUM
    | PRIMARY_ZONE COMP_EQ? primary_zone_name
    | RESOURCE_POOL_LIST COMP_EQ? LeftParen resource_pool_list RightParen
    | ZONE_LIST COMP_EQ? LeftParen zone_list RightParen
    | charset_key COMP_EQ? charset_name
    | read_only_or_write
    | COMMENT COMP_EQ? STRING_VALUE
    | default_tablegroup
    | ENABLE_EXTENDED_ROWID COMP_EQ? BOOL_VALUE
    ;

zone_list
    : STRING_VALUE (opt_comma STRING_VALUE)*
    ;

resource_pool_list
    : STRING_VALUE (Comma STRING_VALUE)*
    ;

alter_tenant_stmt
    : ALTER TENANT relation_name SET? (tenant_option | (opt_tenant_option_list Comma tenant_option))? (VARIABLES sys_var_and_val_list)?
    | ALTER TENANT relation_name lock_spec_mysql57
    ;

drop_tenant_stmt
    : DROP TENANT relation_name
    ;

create_restore_point_stmt
    : CREATE RESTORE POINT relation_name
    ;

drop_restore_point_stmt
    : DROP RESTORE POINT relation_name
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

charset_key
    : CHARSET
    | CHARACTER SET
    ;

database_option
    : DEFAULT? charset_key COMP_EQ? charset_name
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

alter_database_stmt
    : ALTER database_key database_name? SET? database_option_list
    ;

database_name
    : NAME_OB
    ;

load_data_stmt
    : load_data_with_opt_hint (LOCAL | REMOTE_OSS)? INFILE STRING_VALUE (IGNORE | REPLACE)? INTO TABLE relation_factor (COMPRESSION opt_equal_mark compression_name)? (CHARACTER SET charset_name_or_default)? field_opt line_opt (IGNORE INTNUM lines_or_rows)? ((LeftParen RightParen) | (LeftParen field_or_vars_list RightParen))? (SET load_set_list)? load_data_extended_option_list?
    | load_data_with_opt_hint (LOCAL | REMOTE_OSS)? INFILE STRING_VALUE (IGNORE | REPLACE)? INTO TABLE relation_factor use_partition (COMPRESSION opt_equal_mark compression_name)? (CHARACTER SET charset_name_or_default)? field_opt line_opt (IGNORE INTNUM lines_or_rows)? ((LeftParen RightParen) | (LeftParen field_or_vars_list RightParen))? (SET load_set_list)? load_data_extended_option_list?
    ;

load_data_with_opt_hint
    : LOAD DATA
    | LOAD_DATA_HINT_BEGIN hint_list_with_end
    ;

compression_name
    : NAME_OB
    | unreserved_keyword
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

create_synonym_stmt
    : CREATE (OR REPLACE)? PUBLIC? SYNONYM synonym_name FOR synonym_object (USER_VARIABLE opt_reverse_link_flag)?
    | CREATE (OR REPLACE)? PUBLIC? SYNONYM database_factor Dot synonym_name FOR synonym_object (USER_VARIABLE opt_reverse_link_flag)?
    | CREATE (OR REPLACE)? PUBLIC? SYNONYM synonym_name FOR database_factor Dot synonym_object (USER_VARIABLE opt_reverse_link_flag)?
    | CREATE (OR REPLACE)? PUBLIC? SYNONYM database_factor Dot synonym_name FOR database_factor Dot synonym_object (USER_VARIABLE opt_reverse_link_flag)?
    ;

synonym_name
    : NAME_OB
    | unreserved_keyword
    ;

synonym_object
    : NAME_OB
    | unreserved_keyword
    ;

drop_synonym_stmt
    : DROP PUBLIC? SYNONYM synonym_name FORCE?
    | DROP PUBLIC? SYNONYM database_factor Dot synonym_name FORCE?
    ;

special_table_type
    : GLOBAL TEMPORARY
    | EXTERNAL
    | empty
    ;

on_commit_option
    : ON COMMIT (DELETE|PRESERVE) ROWS
    | empty
    ;

create_directory_stmt
    : CREATE (OR REPLACE)? DIRECTORY directory_name AS directory_path
    ;

directory_name
    : NAME_OB
    | unreserved_keyword
    ;

directory_path
    : STRING_VALUE
    ;

drop_directory_stmt
    : DROP DIRECTORY directory_name
    ;

create_keystore_stmt
    : ADMINISTER KEY MANAGEMENT CREATE KEYSTORE keystore_name IDENTIFIED BY password
    ;

alter_keystore_stmt
    : ADMINISTER KEY MANAGEMENT ALTER KEYSTORE PASSWORD IDENTIFIED BY password SET password
    | ADMINISTER KEY MANAGEMENT SET ((KEY|KEYSTORE CLOSE)|KEYSTORE OPEN) IDENTIFIED BY password
    ;

create_table_stmt
    : CREATE special_table_type TABLE relation_factor LeftParen table_element_list RightParen table_option_list? (partition_option | auto_partition_option)? with_column_group? on_commit_option
    | CREATE special_table_type TABLE relation_factor LeftParen table_element_list RightParen table_option_list? (partition_option | auto_partition_option)? with_column_group? on_commit_option AS subquery order_by? fetch_next_clause?
    | CREATE special_table_type TABLE relation_factor table_option_list (partition_option | auto_partition_option)? with_column_group? on_commit_option AS subquery order_by? fetch_next_clause?
    | CREATE special_table_type TABLE relation_factor partition_option with_column_group? on_commit_option AS subquery order_by? fetch_next_clause?
    | CREATE special_table_type TABLE relation_factor with_column_group? on_commit_option AS subquery order_by? fetch_next_clause?
    ;

table_element_list
    : table_element (Comma table_element)*
    ;

table_element
    : column_definition
    | out_of_line_constraint
    | out_of_line_index
    ;

column_definition
    : column_definition_ref data_type visibility_option? opt_column_attribute_list?
    | column_definition_ref data_type visibility_option? (GENERATED opt_generated_option_list)? AS LeftParen bit_expr RightParen VIRTUAL? opt_generated_column_attribute_list?
    | column_definition_ref visibility_option? (GENERATED opt_generated_option_list)? AS LeftParen bit_expr RightParen VIRTUAL? opt_generated_column_attribute_list?
    | column_definition_ref data_type visibility_option? (GENERATED opt_generated_option_list)? AS opt_identity_attribute sequence_option_list? opt_column_attribute_list?
    | column_definition_ref visibility_option? opt_column_attribute_list?
    | column_definition_ref visibility_option? (GENERATED opt_generated_option_list)? AS opt_identity_attribute sequence_option_list? opt_column_attribute_list?
    ;

column_definition_opt_datatype
    : column_definition_ref opt_column_attribute_list?
    | column_definition_ref visibility_option opt_column_attribute_list?
    | column_definition_ref data_type visibility_option? opt_column_attribute_list?
    | column_definition_ref visibility_option? (GENERATED opt_generated_option_list)? AS LeftParen bit_expr RightParen VIRTUAL? opt_generated_column_attribute_list?
    | column_definition_ref data_type visibility_option? (GENERATED opt_generated_option_list)? AS LeftParen bit_expr RightParen VIRTUAL? opt_generated_column_attribute_list?
    | column_definition_ref data_type visibility_option? (GENERATED opt_generated_option_list)? AS opt_identity_attribute sequence_option_list? opt_column_attribute_list?
    ;

out_of_line_index
    : INDEX index_using_algorithm? LeftParen sort_column_list RightParen opt_index_options?
    | INDEX index_name index_using_algorithm? LeftParen sort_column_list RightParen opt_index_options?
    ;

out_of_line_constraint
    : constraint_and_name? out_of_line_unique_index
    | constraint_and_name? out_of_line_primary_index[true]
    | constraint_and_name? FOREIGN KEY LeftParen column_name_list RightParen references_clause constraint_state
    | constraint_and_name? CHECK LeftParen expr RightParen constraint_state
    ;

out_of_line_primary_index[boolean using_idx_flag]
    : PRIMARY KEY LeftParen column_name_list RightParen out_of_line_index_state[$using_idx_flag]?
    ;

out_of_line_unique_index
    : UNIQUE LeftParen sort_column_list RightParen out_of_line_index_state[true]?
    ;

out_of_line_index_state[boolean using_idx_flag]
    : {!$using_idx_flag}? opt_index_options
    | {$using_idx_flag}? USING INDEX opt_index_options?
    ;

constraint_state
    : (RELY | NORELY)? ((USING INDEX opt_index_options) | (USING INDEX))? enable_option? (VALIDATE | NOVALIDATE)?
    ;

enable_option
    : ENABLE
    | DISABLE
    ;

references_clause
    : REFERENCES normal_relation_factor LeftParen column_name_list RightParen reference_option?
    | REFERENCES normal_relation_factor reference_option?
    ;

reference_option
    : ON DELETE reference_action
    ;

reference_action
    : CASCADE
    | SET NULLX
    ;

opt_generated_option_list
    : ALWAYS
    | BY DEFAULT opt_generated_identity_option
    | empty
    ;

opt_generated_identity_option
    : ON NULLX
    | empty
    ;

opt_generated_column_attribute_list
    : opt_generated_column_attribute_list generated_column_attribute
    | generated_column_attribute
    ;

generated_column_attribute
    : constraint_and_name? NOT NULLX constraint_state
    | constraint_and_name? NULLX
    | UNIQUE KEY
    | PRIMARY? KEY
    | UNIQUE
    | COMMENT STRING_VALUE
    | ID INTNUM
    | constraint_and_name? CHECK LeftParen expr RightParen constraint_state
    | SRID INTNUM
    ;

opt_identity_attribute
    : IDENTITY
    ;

column_definition_ref
    : (relation_name Dot)? column_name
    | relation_name Dot relation_name Dot column_name
    ;

column_definition_list
    : column_definition (Comma column_definition)*
    ;

column_definition_opt_datatype_list
    : column_definition_opt_datatype (Comma column_definition_opt_datatype)*
    ;

column_name_list
    : column_name (Comma column_name)*
    ;

zero_suffix_intnum
    : INTNUM
    | DECIMAL_VAL
    ;

cast_data_type
    : binary_type_i
    | character_type_i[true]
    | rowid_type_i
    | datetime_type_i
    | timestamp_type_i
    | int_type_i
    | number_type_i
    | float_type_i
    | double_type_i
    | interval_type_i
    | treat_data_type
    ;

treat_data_type
    : JSON
    | udt_type_i
    | obj_access_ref_cast
    ;

obj_access_ref_cast
    : value_or_type_name (Dot obj_access_ref_cast)?
    ;

value_or_type_name
    : NAME_OB
    ;

udt_type_i
    : (database_name Dot)? type_name
    ;

type_name
    : NAME_OB
    ;

data_type
    : int_type_i
    | float_type_i
    | double_type_i
    | number_type_i
    | timestamp_type_i
    | datetime_type_i
    | character_type_i[false] (charset_key charset_name)? collation?
    | binary_type_i
    | STRING_VALUE
    | JSON
    | XMLTYPE
    | SDO_GEOMETRY
    | interval_type_i
    | rowid_type_i
    ;

binary_type_i
    : RAW LeftParen zero_suffix_intnum RightParen
    | BLOB
    ;

float_type_i
    : FLOAT (data_type_precision | (LeftParen RightParen))?
    | REAL data_type_precision?
    ;

character_type_i [boolean in_cast_data_type]
    : CHARACTER string_length_i? BINARY?
    | CHAR string_length_i? BINARY?
    | varchar_type_i string_length_i BINARY?
    | {$in_cast_data_type}? varchar_type_i
    | NVARCHAR2 string_length_i
    | NCHAR string_length_i
    | CLOB
    ;

rowid_type_i
    : UROWID urowid_length_i?
    | ROWID urowid_length_i?
    ;

interval_type_i
    : INTERVAL YEAR year_precision=data_type_precision? TO MONTH
    | INTERVAL DAY day_precision=data_type_precision? TO SECOND second_precision=data_type_precision?
    ;

number_type_i
    : NUMBER number_precision?
    | NUMERIC number_precision?
    | DECIMAL number_precision?
    | DEC number_precision?
    ;

timestamp_type_i
    : TIMESTAMP data_type_precision?
    | TIMESTAMP data_type_precision? WITH TIME ZONE
    | TIMESTAMP data_type_precision? WITH LOCAL TIME ZONE
    ;

data_type_precision
    : LeftParen precision_int_num RightParen
    | LeftParen precision_decimal_num RightParen
    ;

int_type_i
    : SMALLINT
    | INT
    | INTEGER
    ;

varchar_type_i
    : VARCHAR
    | VARCHAR2
    ;

double_type_i
    : BINARY_DOUBLE
    | BINARY_FLOAT
    ;

datetime_type_i
    : DATE
    ;

number_precision
    : LeftParen ((Star|signed_int_num) (Comma signed_int_num)?|precision_decimal_num) RightParen
    ;

signed_int_num
    : Minus? INTNUM
    ;

precision_int_num
    : INTNUM
    ;

precision_decimal_num
    : DECIMAL_VAL
    ;

nstring_length_i
    : LeftParen zero_suffix_intnum RightParen
    ;

string_length_i
    : LeftParen zero_suffix_intnum (CHARACTER | CHAR | BYTE)? RightParen
    ;

urowid_length_i
    : LeftParen INTNUM RightParen
    ;

collation_name
    : NAME_OB
    | STRING_VALUE
    ;

trans_param_name
    : STRING_VALUE
    ;

trans_param_value
    : STRING_VALUE
    | INTNUM
    ;

charset_name
    : NAME_OB
    | STRING_VALUE
    | BINARY
    | ASCII
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
    : constraint_and_name? NOT NULLX constraint_state
    | constraint_and_name? NULLX
    | DEFAULT bit_expr
    | ORIG_DEFAULT now_or_signed_literal
    | constraint_and_name? PRIMARY KEY
    | constraint_and_name? UNIQUE
    | ID INTNUM
    | SRID INTNUM
    | constraint_and_name? CHECK LeftParen expr RightParen constraint_state
    | constraint_and_name? references_clause constraint_state
    | SKIP_INDEX LeftParen (skip_index_type | (opt_skip_index_type_list Comma skip_index_type))? RightParen
    ;

now_or_signed_literal
    : cur_timestamp_func_params
    | signed_literal_params
    ;

cur_timestamp_func_params
    : LeftParen cur_timestamp_func_params RightParen
    | cur_timestamp_func
    ;

signed_literal_params
    : LeftParen signed_literal_params RightParen
    | signed_literal
    ;

signed_literal
    : literal
    | Plus number_literal
    | Minus number_literal
    ;

opt_comma
    : Comma?
    ;

table_option_list_space_seperated
    : table_option table_option_list_space_seperated?
    ;

table_option_list
    : table_option_list_space_seperated
    | table_option Comma table_option_list
    ;

primary_zone_name
    : DEFAULT
    | RANDOM
    | relation_name_or_string
    ;

locality_name
    : STRING_VALUE
    | DEFAULT
    ;

table_option
    : SORTKEY LeftParen column_name_list RightParen
    | parallel_option
    | TABLE_MODE COMP_EQ? STRING_VALUE
    | DUPLICATE_SCOPE COMP_EQ? STRING_VALUE
    | LOCALITY COMP_EQ? locality_name FORCE?
    | EXPIRE_INFO COMP_EQ? LeftParen bit_expr RightParen
    | PROGRESSIVE_MERGE_NUM COMP_EQ? INTNUM
    | BLOCK_SIZE COMP_EQ? INTNUM
    | TABLE_ID COMP_EQ? INTNUM
    | REPLICA_NUM COMP_EQ? INTNUM
    | compress_option
    | USE_BLOOM_FILTER COMP_EQ? BOOL_VALUE
    | PRIMARY_ZONE COMP_EQ? primary_zone_name
    | TABLEGROUP COMP_EQ? relation_name_or_string
    | read_only_or_write
    | ENGINE_ COMP_EQ? relation_name_or_string
    | TABLET_SIZE COMP_EQ? INTNUM
    | MAX_USED_PART_ID COMP_EQ? INTNUM
    | ENABLE ROW MOVEMENT
    | DISABLE ROW MOVEMENT
    | ENABLE_EXTENDED_ROWID COMP_EQ? BOOL_VALUE
    | physical_attributes_option
    | LOCATION COMP_EQ? STRING_VALUE
    | FORMAT COMP_EQ? LeftParen external_file_format_list RightParen
    | PROPERTIES COMP_EQ? LeftParen external_properties_list RightParen
    | PATTERN COMP_EQ? STRING_VALUE
    | PARTITION_TYPE COMP_EQ? USER_SPECIFIED
    | MICRO_INDEX_CLUSTERED COMP_EQ? BOOL_VALUE
    | AUTO_REFRESH COMP_EQ? (OFF|IMMEDIATE|INTERVAL)
    ;

parallel_option
    : PARALLEL COMP_EQ? INTNUM
    | NOPARALLEL
    ;

storage_options_list
    : storage_option+
    ;

storage_option
    : INITIAL_ size_option
    | NEXT size_option
    | MINEXTENTS INTNUM
    | MAXEXTENTS int_or_unlimited
    ;

size_option
    : INTNUM unit_of_size?
    ;

int_or_unlimited
    : INTNUM
    | UNLIMITED
    ;

unit_of_size
    : K_SIZE
    | M_SIZE
    | G_SIZE
    | T_SIZE
    | P_SIZE
    | E_SIZE
    ;

relation_name_or_string
    : relation_name
    | STRING_VALUE
    ;

opt_equal_mark
    : COMP_EQ?
    ;

partition_option_inner
    : hash_partition_option
    | range_partition_option
    | list_partition_option
    | external_table_partition_option
    ;

external_table_partition_option
    : PARTITION BY LeftParen column_name_list RightParen
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
    : PARTITION BY RANGE LeftParen column_name_list? RightParen
    ;

partition_option
    : partition_option_inner
    ;

hash_partition_option
    : PARTITION BY HASH LeftParen column_name_list RightParen subpartition_option? (PARTITIONS INTNUM)? (LeftParen hash_partition_list RightParen)? hash_partition_attributes_option_list?
    ;

hash_partition_attributes_option_list
    : TABLESPACE tablespace
    | compress_option
    | TABLESPACE tablespace compress_option
    ;

list_partition_option
    : PARTITION BY LIST LeftParen column_name_list RightParen subpartition_option? opt_list_partition_list
    ;

range_partition_option
    : PARTITION BY RANGE LeftParen column_name_list RightParen interval_option? subpartition_option? opt_range_partition_list
    ;

interval_option
    : INTERVAL LeftParen bit_expr RightParen
    ;

subpartition_option
    : subpartition_template_option
    | subpartition_individual_option
    ;

subpartition_template_option
    : SUBPARTITION BY HASH LeftParen column_name_list RightParen SUBPARTITION TEMPLATE opt_hash_subpartition_list
    | SUBPARTITION BY RANGE LeftParen column_name_list RightParen SUBPARTITION TEMPLATE opt_range_subpartition_list
    | SUBPARTITION BY LIST LeftParen column_name_list RightParen SUBPARTITION TEMPLATE opt_list_subpartition_list
    ;

subpartition_individual_option
    : SUBPARTITION BY HASH LeftParen column_name_list RightParen (SUBPARTITIONS hash_subpartition_quantity)?
    | SUBPARTITION BY RANGE LeftParen column_name_list RightParen
    | SUBPARTITION BY LIST LeftParen column_name_list RightParen
    ;

aux_column_list
    : vertical_column_name (Comma vertical_column_name)*
    ;

vertical_column_name
    : column_name
    | LeftParen column_name_list RightParen
    ;

hash_partition_list
    : hash_partition_element (Comma hash_partition_element)*
    ;

hash_partition_element
    : PARTITION relation_factor? (ID INTNUM)? hash_partition_attributes_option_list? subpartition_list?
    ;

opt_range_partition_list
    : LeftParen range_partition_list RightParen
    ;

range_partition_list
    : range_partition_element (Comma range_partition_element)*
    ;

partition_attributes_option_list
    : physical_attributes_option_list
    | compress_option
    | physical_attributes_option_list compress_option
    ;

range_partition_element
    : PARTITION relation_factor VALUES LESS THAN range_partition_expr (ID INTNUM)? partition_attributes_option_list? subpartition_list?
    | PARTITION VALUES LESS THAN range_partition_expr (ID INTNUM)? partition_attributes_option_list? subpartition_list?
    ;

opt_list_partition_list
    : LeftParen list_partition_list RightParen
    ;

list_partition_list
    : list_partition_element (Comma list_partition_element)*
    ;

list_partition_element
    : PARTITION relation_factor VALUES list_partition_expr (ID INTNUM)? partition_attributes_option_list? subpartition_list?
    | PARTITION VALUES list_partition_expr (ID INTNUM)? partition_attributes_option_list? subpartition_list?
    ;

subpartition_list
    : opt_hash_subpartition_list
    | opt_range_subpartition_list
    | opt_list_subpartition_list
    ;

opt_hash_subpartition_list
    : LeftParen hash_subpartition_list RightParen
    ;

hash_subpartition_list
    : hash_subpartition_element (Comma hash_subpartition_element)*
    ;

hash_subpartition_element
    : SUBPARTITION relation_factor physical_attributes_option_list?
    ;

opt_range_subpartition_list
    : LeftParen range_subpartition_list RightParen
    ;

range_subpartition_list
    : range_subpartition_element (Comma range_subpartition_element)*
    ;

range_subpartition_element
    : SUBPARTITION relation_factor VALUES LESS THAN range_partition_expr physical_attributes_option_list?
    ;

opt_list_subpartition_list
    : LeftParen list_subpartition_list RightParen
    ;

list_subpartition_list
    : list_subpartition_element (Comma list_subpartition_element)*
    ;

list_subpartition_element
    : SUBPARTITION relation_factor VALUES list_partition_expr physical_attributes_option_list?
    ;

list_partition_expr
    : LeftParen (DEFAULT|list_expr) RightParen
    ;

list_expr
    : bit_expr (Comma bit_expr)*
    ;

physical_attributes_option_list
    : physical_attributes_option+
    ;

physical_attributes_option
    : PCTFREE COMP_EQ? INTNUM
    | PCTUSED INTNUM
    | INITRANS INTNUM
    | MAXTRANS INTNUM
    | STORAGE LeftParen storage_options_list RightParen
    | TABLESPACE tablespace
    ;

opt_special_partition_list
    : LeftParen special_partition_list RightParen
    ;

special_partition_list
    : special_partition_define (Comma special_partition_define)*
    ;

special_partition_define
    : PARTITION (ID INTNUM)?
    | PARTITION relation_factor (ID INTNUM)?
    ;

range_partition_expr
    : LeftParen range_expr_list RightParen
    ;

range_expr_list
    : range_expr (Comma range_expr)*
    ;

range_expr
    : Plus? literal
    | Minus literal
    | access_func_expr
    | MAXVALUE
    ;

hash_subpartition_quantity
    : INTNUM
    ;

int_or_decimal
    : INTNUM
    | DECIMAL_VAL
    ;

tg_hash_partition_option
    : PARTITION BY HASH tg_subpartition_option (PARTITIONS INTNUM)?
    | PARTITION BY HASH INTNUM tg_subpartition_option (PARTITIONS INTNUM)?
    ;

tg_range_partition_option
    : PARTITION BY RANGE COLUMNS? INTNUM tg_subpartition_option opt_range_partition_list
    ;

tg_list_partition_option
    : PARTITION BY LIST COLUMNS? INTNUM tg_subpartition_option opt_list_partition_list
    ;

tg_subpartition_option
    : tg_subpartition_template_option
    | tg_subpartition_individual_option
    ;

tg_subpartition_template_option
    : SUBPARTITION BY RANGE COLUMNS? INTNUM SUBPARTITION TEMPLATE opt_range_subpartition_list
    | SUBPARTITION BY HASH SUBPARTITION TEMPLATE hash_subpartition_quantity
    | SUBPARTITION BY HASH INTNUM SUBPARTITION TEMPLATE hash_subpartition_quantity
    | SUBPARTITION BY LIST COLUMNS? INTNUM SUBPARTITION TEMPLATE opt_list_subpartition_list
    | empty
    ;

tg_subpartition_individual_option
    : SUBPARTITION BY HASH (SUBPARTITIONS hash_subpartition_quantity)?
    | SUBPARTITION BY HASH INTNUM (SUBPARTITIONS hash_subpartition_quantity)?
    | SUBPARTITION BY RANGE COLUMNS? INTNUM
    | SUBPARTITION BY LIST COLUMNS? INTNUM
    ;

opt_alter_compress_option
    : MOVE compress_option
    ;

compress_option
    : NOCOMPRESS
    | COMPRESS (BASIC | (FOR OLTP) | (FOR QUERY opt_compress_level) | (FOR ARCHIVE opt_compress_level))?
    ;

opt_compress_level
    : (LOW | HIGH)?
    ;

external_properties_list
    : external_properties (opt_comma external_properties)*
    ;

external_properties
    :  external_properties_key COMP_EQ STRING_VALUE
    ;

external_properties_key
    : ((((ACCESSID|ACCESSKEY)|(ACCESSTYPE|TYPE))|((ENDPOINT|STSTOKEN)|(PROJECT_NAME|SCHEMA_NAME)))|((COMPRESSION_CODE|QUOTA_NAME)|TABLE_NAME))
    ;

external_file_format_list
    : external_file_format (opt_comma external_file_format)*
    ;

external_file_format
    : format_key=(ENCODING|TYPE) COMP_EQ STRING_VALUE
    | format_key=(ESCAPE|FIELD_OPTIONALLY_ENCLOSED_BY|FIELD_DELIMITER|LINE_DELIMITER) COMP_EQ bit_expr
    | format_key=SKIP_HEADER COMP_EQ INTNUM
    | format_key=(SKIP_BLANK_LINES|TRIM_SPACE|EMPTY_FIELD_AS_NULL) COMP_EQ BOOL_VALUE
    | format_key=NULL_IF_EXETERNAL COMP_EQ LeftParen expr_list RightParen
    | format_key=COMPRESSION COMP_EQ compression_name
    ;

create_tablegroup_stmt
    : CREATE TABLEGROUP relation_name tablegroup_option_list? (tg_hash_partition_option | tg_range_partition_option | tg_list_partition_option)?
    ;

drop_tablegroup_stmt
    : DROP TABLEGROUP relation_name
    ;

alter_tablegroup_stmt
    : ALTER TABLEGROUP relation_name ADD TABLE? table_list
    | ALTER TABLEGROUP relation_name ((alter_partition_option|alter_tablegroup_actions)|tg_modify_partition_info)
    ;

tablegroup_option_list_space_seperated
    : tablegroup_option tablegroup_option_list_space_seperated?
    ;

tablegroup_option_list
    : tablegroup_option_list_space_seperated
    | tablegroup_option Comma tablegroup_option_list
    ;

tablegroup_option
    : LOCALITY COMP_EQ? locality_name FORCE?
    | PRIMARY_ZONE COMP_EQ? primary_zone_name
    | TABLEGROUP_ID COMP_EQ? INTNUM
    | BINDING COMP_EQ? BOOL_VALUE
    | SHARDING COMP_EQ? STRING_VALUE
    | MAX_USED_PART_ID COMP_EQ? INTNUM
    ;

alter_tablegroup_actions
    : alter_tablegroup_action (Comma alter_tablegroup_action)*
    ;

alter_tablegroup_action
    : SET? tablegroup_option_list_space_seperated
    ;

default_tablegroup
    : DEFAULT_TABLEGROUP COMP_EQ? relation_name
    | DEFAULT_TABLEGROUP COMP_EQ? NULLX
    ;

create_view_stmt
    : CREATE (OR REPLACE)? ((NO FORCE) | FORCE)? VIEW view_name (LeftParen alias_name_list RightParen)? (TABLE_ID COMP_EQ INTNUM)? AS view_subquery view_with_opt
    ;

create_mview_stmt
    : CREATE MATERIALIZED VIEW view_name (LeftParen mv_column_list RightParen)? table_option_list? (partition_option | auto_partition_option)? (WITH_COLUMN_GROUP LeftParen column_group_list RightParen)? create_mview_opts AS view_subquery view_with_opt
    ;

create_mview_opts
    : mview_refresh_opt
    | mview_refresh_opt mview_enable_disable ON QUERY COMPUTATION
    | mview_refresh_opt mview_enable_disable QUERY REWRITE
    | mview_refresh_opt mview_enable_disable ON QUERY COMPUTATION mview_enable_disable QUERY REWRITE
    | mview_refresh_opt mview_enable_disable QUERY REWRITE mview_enable_disable ON QUERY COMPUTATION
    | empty
    ;

mview_enable_disable
    : DISABLE
    | ENABLE
    ;

mview_refresh_opt
    : REFRESH mv_refresh_method mv_refresh_on_clause mv_refresh_interval
    | NEVER REFRESH
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

view_subquery
    : subquery order_by? fetch_next_clause?
    ;

view_with_opt
    : WITH READ ONLY
    | with_check_option
    | empty
    ;

with_check_option
    : WITH CHECK OPTION
    ;

view_name
    : relation_factor
    ;

opt_tablet_id
    : TABLET_ID opt_equal_mark INTNUM
    | empty
    ;

opt_tablet_id_no_empty
    : TABLET_ID opt_equal_mark INTNUM
    ;

create_index_stmt
    : CREATE UNIQUE? INDEX normal_relation_factor index_using_algorithm? ON relation_factor LeftParen sort_column_list RightParen opt_index_options? (partition_option | auto_partition_option)? with_column_group?
    | CREATE UNIQUE? INDEX normal_relation_factor index_using_algorithm? ON relation_factor LeftParen sort_column_list RightParen INDEXTYPE IS MDSYS Dot SPATIAL_INDEX opt_index_options?
    ;

index_name
    : relation_name
    ;

constraint_and_name
    : CONSTRAINT constraint_name
    ;

constraint_name
    : relation_name
    ;

sort_column_list
    : sort_column_key (Comma sort_column_key)*
    ;

sort_column_key
    : index_expr opt_asc_desc (ID INTNUM)?
    ;

index_expr
    : bit_expr
    ;

opt_index_options
    : index_option+
    ;

index_option
    : GLOBAL
    | LOCAL
    | BLOCK_SIZE COMP_EQ? INTNUM
    | COMMENT STRING_VALUE
    | STORING LeftParen column_name_list RightParen
    | WITH ROWID
    | WITH PARSER STRING_VALUE
    | index_using_algorithm
    | visibility_option
    | DATA_TABLE_ID COMP_EQ? INTNUM
    | INDEX_TABLE_ID COMP_EQ? INTNUM
    | MAX_USED_PART_ID COMP_EQ? INTNUM
    | physical_attributes_option
    | REVERSE
    | parallel_option
    ;

index_using_algorithm
    : USING BTREE
    | USING HASH
    ;

create_mlog_stmt
    : CREATE MATERIALIZED VIEW LOG ON relation_factor opt_mlog_options? (WITH mlog_with_values)? (mlog_including_or_excluding NEW VALUES)? (PURGE mlog_purge_values)?
    ;

opt_mlog_options
    : mlog_option+
    ;

mlog_option
    : physical_attributes_option
    | parallel_option
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
    : DROP TABLE relation_factor (CASCADE CONSTRAINTS)? PURGE?
    ;

table_or_tables
    : TABLE
    | TABLES
    ;

drop_view_stmt
    : DROP MATERIALIZED? VIEW relation_factor (CASCADE CONSTRAINTS)?
    ;

table_list
    : relation_factor (Comma relation_factor)*
    ;

drop_index_stmt
    : DROP INDEX relation_name (Dot relation_name)?
    ;

insert_stmt
    : insert_with_opt_hint single_table_insert
    | insert_with_opt_hint multi_table_insert
    ;

opt_simple_expression
    : empty
    | LeftParen simple_expr RightParen
    ;

into_err_log_caluse
    : empty
    | INTO relation_factor
    ;

reject_limit
    : empty
    | REJECT LIMIT (INTNUM|UNLIMITED)
    ;

single_table_insert
    : INTO insert_table_clause NOLOGGING? LeftParen column_list RightParen values_clause returning_log_error_clause?
    | INTO insert_table_clause NOLOGGING? LeftParen RightParen values_clause returning_log_error_clause?
    | INTO insert_table_clause NOLOGGING? values_clause returning_log_error_clause?
    | OVERWRITE insert_table_clause NOLOGGING? (LeftParen column_list RightParen)? values_clause
    ;

multi_table_insert
    : ALL insert_table_clause_list subquery order_by? fetch_next_clause?
    | conditional_insert_clause subquery order_by? fetch_next_clause?
    ;

insert_table_clause_list
    : insert_single_table_clause+
    ;

insert_single_table_clause
    : INTO dml_table_name
    | INTO dml_table_name LeftParen column_list RightParen
    | INTO dml_table_name VALUES LeftParen insert_vals RightParen
    | INTO dml_table_name LeftParen column_list RightParen VALUES LeftParen insert_vals RightParen
    ;

conditional_insert_clause
    : (ALL | FIRST)? condition_insert_clause_list (ELSE insert_table_clause_list)?
    ;

condition_insert_clause_list
    : condition_insert_clause+
    ;

condition_insert_clause
    : WHEN expr THEN insert_table_clause_list
    ;

values_clause
    : VALUES insert_vals_list
    | VALUES obj_access_ref_normal
    | subquery order_by? fetch_next_clause?
    ;

opt_into_clause
    : into_clause?
    ;

returning_exprs
    : projection (Comma projection)*
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
    : expr_or_default (Comma expr_or_default)*
    ;

expr_or_default
    : bit_expr
    | DEFAULT
    ;

merge_with_opt_hint
    : MERGE
    | MERGE_HINT_BEGIN hint_list_with_end
    ;

merge_stmt
    : merge_with_opt_hint INTO source_relation_factor USING source_relation_factor ON LeftParen expr RightParen merge_update_clause? merge_insert_clause
    | merge_with_opt_hint INTO source_relation_factor USING source_relation_factor ON LeftParen expr RightParen merge_insert_clause? merge_update_clause
    ;

merge_update_clause
    : WHEN MATCHED THEN UPDATE SET update_asgn_list ((WHERE expr) | (WHERE HINT_VALUE expr))? (DELETE WHERE expr)?
    ;

merge_insert_clause
    : WHEN NOT MATCHED THEN INSERT (LeftParen column_list RightParen)? VALUES LeftParen insert_vals RightParen ((WHERE expr) | (WHERE HINT_VALUE expr))?
    ;

source_relation_factor
    : relation_factor relation_name?
    | select_with_parens relation_name?
    | TABLE LeftParen (select_no_parens|simple_expr) RightParen relation_name?
    | dual_table relation_name?
    ;

select_stmt
    : subquery fetch_next_clause?
    | subquery for_update
    | subquery fetch_next for_update
    | subquery order_by fetch_next_clause?
    | subquery order_by fetch_next_clause? for_update
    | subquery for_update order_by
    ;

subquery
    : select_no_parens
    | select_with_parens
    | with_select
    ;

select_with_parens
    : LeftParen (select_no_parens|with_select) order_by? fetch_next_clause? RightParen
    | LeftParen select_with_parens RightParen
    ;

select_no_parens
    : select_clause
    | select_clause_set
    ;

select_clause
    : simple_select
    | select_with_hierarchical_query
    ;

select_clause_set
    : select_clause_set set_type select_clause_set_right
    | select_clause_set_left set_type select_clause_set_right
    ;

select_clause_set_right
    : simple_select
    | select_with_hierarchical_query
    | select_with_parens
    ;

select_clause_set_left
    : select_clause_set_right
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
    : select_with_opt_hint query_expression_option_list? select_expr_list into_opt FROM from_list ((WHERE expr) | (WHERE HINT_VALUE expr))? ((GROUP BY groupby_clause) | (HAVING expr) | (GROUP BY groupby_clause HAVING expr) | (HAVING expr GROUP BY groupby_clause))?
    ;

select_with_hierarchical_query
    : select_with_opt_hint query_expression_option_list? select_expr_list into_opt FROM from_list ((WHERE expr) | (WHERE HINT_VALUE expr))? start_with connect_by ((GROUP BY groupby_clause) | (HAVING expr) | (GROUP BY groupby_clause HAVING expr) | (HAVING expr GROUP BY groupby_clause))?
    | select_with_opt_hint query_expression_option_list? select_expr_list into_opt FROM from_list ((WHERE expr) | (WHERE HINT_VALUE expr))? connect_by start_with? ((GROUP BY groupby_clause) | (HAVING expr) | (GROUP BY groupby_clause HAVING expr) | (HAVING expr GROUP BY groupby_clause))?
    ;

start_with
    : START WITH expr
    ;

fetch_next_clause
    : OFFSET bit_expr (ROW|ROWS) fetch_next?
    | fetch_next
    ;

fetch_next
    : fetch_next_count
    | fetch_next_percent
    ;

fetch_next_count
    : fetch_next_expr (ONLY|WITH TIES)
    ;

fetch_next_percent
    : fetch_next_percent_expr (ONLY|WITH TIES)
    ;

fetch_next_expr
    : FETCH (FIRST|NEXT) bit_expr? (ROW|ROWS)
    ;

fetch_next_percent_expr
    : FETCH (FIRST|NEXT) bit_expr PERCENT (ROW|ROWS)
    ;

connect_by
    : CONNECT BY NOCYCLE? expr
    ;

set_type_union
    : UNION
    ;

set_type_other
    : INTERSECT
    | MINUS
    ;

set_type
    : set_type_union set_expression_option
    | set_type_other
    ;

set_expression_option
    : ALL?
    ;

opt_where
    : empty
    | WHERE HINT_VALUE? expr
    ;

opt_where_extension
    : opt_where
    | WHERE CURRENT OF obj_access_ref
    ;

into_clause
    : (BULK COLLECT)? INTO into_var_list
    ;

into_opt
    : INTO OUTFILE STRING_VALUE file_partition_opt (charset_key charset_name)? field_opt line_opt file_opt
    | INTO DUMPFILE STRING_VALUE
    | into_clause
    | empty
    ;

into_var_list
    : into_var (Comma into_var)*
    ;

into_var
    : USER_VARIABLE
    | obj_access_ref_normal
    | QUESTIONMARK
    | {this.is_pl_parse_}? QUESTIONMARK Dot column_name
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
file_opt
    : file_option_list?
    ;

file_option_list
    : file_option+
    ;

file_option
    : SINGLE COMP_EQ? BOOL_VALUE
    | MAX_FILE_SIZE COMP_EQ? file_size_const
    | BUFFER_SIZE COMP_EQ? file_size_const
    ;

file_partition_opt
    : empty
    | PARTITION BY bit_expr
    ;

file_size_const
    : INTNUM
    | STRING_VALUE
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
    : relation_name
    | name_list relation_name
    | name_list Comma relation_name
    ;

hint_option
    : NO_REWRITE
    | READ_CONSISTENCY LeftParen consistency_level RightParen
    | INDEX_HINT LeftParen qb_name_option relation_factor_in_hint NAME_OB RightParen
    | QUERY_TIMEOUT LeftParen INTNUM RightParen
    | FROZEN_VERSION LeftParen INTNUM RightParen
    | TOPK LeftParen INTNUM INTNUM RightParen
    | HOTSPOT
    | LOG_LEVEL LeftParen NAME_OB RightParen
    | LOG_LEVEL LeftParen STRING_VALUE RightParen
    | LEADING_HINT LeftParen qb_name_option relation_factor_in_leading_hint_list_entry RightParen
    | LEADING_HINT LeftParen qb_name_option relation_factor_in_hint_list RightParen
    | ORDERED
    | FULL_HINT LeftParen qb_name_option relation_factor_in_hint RightParen
    | USE_PLAN_CACHE LeftParen use_plan_cache_type RightParen
    | USE_MERGE LeftParen qb_name_option relation_factor_in_use_join_hint_list RightParen
    | NO_USE_MERGE LeftParen qb_name_option relation_factor_in_use_join_hint_list RightParen
    | USE_HASH LeftParen qb_name_option relation_factor_in_use_join_hint_list RightParen
    | NO_USE_HASH LeftParen qb_name_option relation_factor_in_use_join_hint_list RightParen
    | USE_NL LeftParen qb_name_option relation_factor_in_use_join_hint_list RightParen
    | NO_USE_NL LeftParen qb_name_option relation_factor_in_use_join_hint_list RightParen
    | USE_BNL LeftParen qb_name_option relation_factor_in_use_join_hint_list RightParen
    | NO_USE_BNL LeftParen qb_name_option relation_factor_in_use_join_hint_list RightParen
    | USE_NL_MATERIALIZATION LeftParen qb_name_option relation_factor_in_use_join_hint_list RightParen
    | NO_USE_NL_MATERIALIZATION LeftParen qb_name_option relation_factor_in_use_join_hint_list RightParen
    | USE_HASH_AGGREGATION
    | NO_USE_HASH_AGGREGATION
    | MERGE_HINT (LeftParen qb_name_option RightParen)?
    | NO_MERGE_HINT (LeftParen qb_name_option RightParen)?
    | NO_EXPAND (LeftParen qb_name_option RightParen)?
    | USE_CONCAT (LeftParen qb_name_option RightParen)?
    | UNNEST (LeftParen qb_name_option RightParen)?
    | NO_UNNEST (LeftParen qb_name_option RightParen)?
    | PLACE_GROUP_BY (LeftParen qb_name_option RightParen)?
    | NO_PLACE_GROUP_BY (LeftParen qb_name_option RightParen)?
    | NO_PRED_DEDUCE (LeftParen qb_name_option RightParen)?
    | USE_JIT LeftParen use_jit_type RightParen
    | NO_USE_JIT
    | USE_LATE_MATERIALIZATION
    | NO_USE_LATE_MATERIALIZATION
    | TRACE_LOG
    | STAT LeftParen tracing_num_list RightParen
    | TRACING LeftParen tracing_num_list RightParen
    | DOP LeftParen INTNUM Comma INTNUM RightParen
    | USE_PX
    | NO_USE_PX
    | TRANS_PARAM LeftParen trans_param_name Comma? trans_param_value RightParen
    | PX_JOIN_FILTER LeftParen qb_name_option relation_factor_in_use_join_hint_list RightParen
    | NO_PX_JOIN_FILTER LeftParen qb_name_option relation_factor_in_use_join_hint_list RightParen
    | FORCE_REFRESH_LOCATION_CACHE
    | QB_NAME LeftParen NAME_OB RightParen
    | MAX_CONCURRENT LeftParen INTNUM RightParen
    | PARALLEL LeftParen INTNUM RightParen
    | NO_PARALLEL
    | MONITOR
    | PQ_DISTRIBUTE LeftParen qb_name_option relation_factor_in_pq_hint Comma? distribute_method (opt_comma distribute_method)? RightParen
    | PQ_MAP LeftParen qb_name_option relation_factor_in_hint RightParen
    | LOAD_BATCH_SIZE LeftParen INTNUM RightParen
    | NAME_OB
    | EOF
    | PARSER_SYNTAX_ERROR
    | ENABLE_PARALLEL_DML
    | DISABLE_PARALLEL_DML
    | INLINE (LeftParen qb_name_option RightParen)?
    | MATERIALIZE (LeftParen qb_name_option RightParen)?
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
    | BASIC
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

for_update
    : FOR UPDATE (OF column_list)? ((WAIT DECIMAL_VAL) | (WAIT INTNUM) | NOWAIT | (R_SKIP LOCKED))?
    ;

parameterized_trim
    : (BOTH FROM)? bit_expr
    | BOTH? bit_expr FROM bit_expr
    | (LEADING|TRAILING) bit_expr? FROM bit_expr
    ;

groupby_clause
    : groupby_element_list
    ;

groupby_element_list
    : groupby_element (Comma groupby_element)*
    ;

groupby_element
    : group_by_expr
    | rollup_clause
    | cube_clause
    | grouping_sets_clause
    | LeftParen RightParen
    ;

group_by_expr
    : bit_expr
    ;

rollup_clause
    : ROLLUP LeftParen group_by_expr_list RightParen
    ;

cube_clause
    : CUBE LeftParen group_by_expr_list RightParen
    ;

group_by_expr_list
    : group_by_expr (Comma group_by_expr)*
    ;

grouping_sets_clause
    : GROUPING SETS LeftParen grouping_sets_list RightParen
    ;

grouping_sets_list
    : grouping_sets (Comma grouping_sets)*
    ;

grouping_sets
    : group_by_expr
    | rollup_clause
    | cube_clause
    | LeftParen RightParen
    ;

order_by
    : ORDER SIBLINGS? BY sort_list
    ;

sort_list
    : sort_key (Comma sort_key)*
    ;

sort_key
    : bit_expr opt_asc_desc
    ;

opt_null_pos
    : empty
    | NULLS LAST
    | NULLS FIRST
    ;

opt_ascending_type
    : (ASC | DESC)?
    ;

opt_asc_desc
    : opt_ascending_type opt_null_pos
    ;

query_expression_option_list
    : query_expression_option query_expression_option?
    ;

query_expression_option
    : ALL
    | DISTINCT
    | UNIQUE
    | SQL_CALC_FOUND_ROWS
    ;

projection
    : bit_expr (AS column_label|column_label?)
    | Star
    ;

opt_as
    : AS?
    ;

select_expr_list
    : projection (Comma projection)*
    ;

from_list
    : table_references
    ;

table_references
    : table_reference (Comma table_reference)*
    ;

table_reference
    : table_factor
    | joined_table
    ;

table_factor
    : tbl_name
    | (LATERAL)? table_subquery
    | LeftParen table_reference RightParen
    | TABLE LeftParen (select_no_parens|simple_expr) RightParen relation_name?
    | select_function relation_name?
    | json_table_expr (AS? relation_name)?
    | xml_table_expr (AS? relation_name)?
    ;

select_function
    : access_func_expr
    | database_factor Dot select_function
    ;

tbl_name
    : relation_factor use_partition? (sample_clause seed|sample_clause?) use_flashback? relation_name?
    | relation_factor use_partition? (sample_clause seed|sample_clause?) relation_name? transpose_clause
    | dual_table relation_name?
    ;

dual_table
    : DUAL
    ;

transpose_clause
    : PIVOT LeftParen pivot_aggr_clause transpose_for_clause transpose_in_clause RightParen
    | PIVOT LeftParen pivot_aggr_clause transpose_for_clause transpose_in_clause RightParen relation_name
    | UNPIVOT ((EXCLUDE NULLS) | (INCLUDE NULLS))? LeftParen unpivot_column_clause transpose_for_clause unpivot_in_clause RightParen
    | UNPIVOT ((EXCLUDE NULLS) | (INCLUDE NULLS))? LeftParen unpivot_column_clause transpose_for_clause unpivot_in_clause RightParen relation_name
    ;

pivot_aggr_clause
    : pivot_single_aggr_clause (Comma pivot_single_aggr_clause)*
    ;

pivot_single_aggr_clause
    : aggregate_function (opt_as relation_name)?
    | access_func_expr_count (opt_as relation_name)?
    ;

transpose_for_clause
    : FOR column_name
    | FOR LeftParen column_name_list RightParen
    ;

transpose_in_clause
    : IN LeftParen transpose_in_args RightParen
    ;

transpose_in_args
    : transpose_in_arg (Comma transpose_in_arg)*
    ;

transpose_in_arg
    : bit_expr (AS relation_name|relation_name?)
    ;

unpivot_column_clause
    : column_name
    | LeftParen column_name_list RightParen
    ;

unpivot_in_clause
    : IN LeftParen unpivot_in_args RightParen
    ;

unpivot_in_args
    : unpivot_in_arg (Comma unpivot_in_arg)*
    ;

unpivot_in_arg
    : unpivot_column_clause (AS bit_expr)?
    ;

dml_table_name
    : relation_factor use_partition?
    ;

order_by_fetch_with_check_option
    : with_check_option
    | fetch_next_clause with_check_option?
    | order_by fetch_next_clause? with_check_option?
    ;

insert_table_clause
    : dml_table_name relation_name?
    | select_with_parens relation_name?
    | LeftParen subquery order_by_fetch_with_check_option RightParen relation_name?
    ;

dml_table_clause
    : dml_table_name relation_name?
    | ONLY LeftParen dml_table_name RightParen relation_name?
    | select_with_parens relation_name?
    | LeftParen subquery order_by_fetch_with_check_option RightParen relation_name?
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
    : select_with_parens use_flashback? relation_name? transpose_clause?
    | LeftParen subquery order_by_fetch_with_check_option RightParen use_flashback? relation_name? transpose_clause?
    ;

use_partition
    : (PARTITION|SUBPARTITION) LeftParen name_list RightParen
    | PARTITION LeftParen external_table_partitions? RightParen
    ;

external_table_partitions
    : external_table_partition
    | external_table_partitions Comma external_table_partition
    ;

external_table_partition
    : relation_name COMP_EQ expr_const
    ;

use_flashback
    : AS OF (SCN|TIMESTAMP) bit_expr
    ;

relation_factor
    : normal_relation_factor
    | dot_relation_factor
    ;

normal_relation_factor
    : relation_name (USER_VARIABLE opt_reverse_link_flag)?
    | database_factor Dot relation_name (USER_VARIABLE opt_reverse_link_flag)?
    ;

dot_relation_factor
    : Dot relation_name
    ;

opt_reverse_link_flag
    : Not?
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

tracing_num_list
    : INTNUM (relation_sep_option tracing_num_list)?
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

join_condition
    : ON expr
    | USING LeftParen column_list RightParen
    ;

joined_table
    : table_factor outer_join_type JOIN table_factor join_condition
    | table_factor INNER? JOIN table_factor ON expr
    | table_factor INNER? JOIN table_factor USING LeftParen column_list RightParen
    | table_factor (CROSS JOIN|natural_join_type) table_factor
    | joined_table natural_join_type table_factor
    | joined_table CROSS JOIN table_factor
    | joined_table outer_join_type JOIN table_factor join_condition
    | joined_table JOIN table_factor ON expr
    | joined_table INNER JOIN table_factor ON expr
    | joined_table JOIN table_factor USING LeftParen column_list RightParen
    | joined_table INNER JOIN table_factor USING LeftParen column_list RightParen
    ;

natural_join_type
    : NATURAL (INNER|outer_join_type?) JOIN
    ;

outer_join_type
    : FULL join_outer
    | LEFT join_outer
    | RIGHT join_outer
    ;

join_outer
    : OUTER?
    ;

with_select
    : with_clause (select_no_parens |select_with_parens)
    ;

with_clause
    : WITH (RECURSIVE common_table_expr|with_list)
    ;

with_list
    : common_table_expr (Comma common_table_expr)*
    ;

common_table_expr
    : relation_name (LeftParen alias_name_list RightParen)? AS LeftParen select_no_parens order_by? fetch_next_clause? RightParen ((SEARCH DEPTH FIRST BY sort_list search_set_value) | (SEARCH BREADTH FIRST BY sort_list search_set_value))? (CYCLE alias_name_list SET var_name TO STRING_VALUE DEFAULT STRING_VALUE)?
    | relation_name (LeftParen alias_name_list RightParen)? AS LeftParen with_select RightParen ((SEARCH DEPTH FIRST BY sort_list search_set_value) | (SEARCH BREADTH FIRST BY sort_list search_set_value))? (CYCLE alias_name_list SET var_name TO STRING_VALUE DEFAULT STRING_VALUE)?
    | relation_name (LeftParen alias_name_list RightParen)? AS LeftParen select_with_parens RightParen ((SEARCH DEPTH FIRST BY sort_list search_set_value) | (SEARCH BREADTH FIRST BY sort_list search_set_value))? (CYCLE alias_name_list SET var_name TO STRING_VALUE DEFAULT STRING_VALUE)?
    | relation_name (LeftParen alias_name_list RightParen)? AS LeftParen subquery order_by fetch_next_clause? RightParen ((SEARCH DEPTH FIRST BY sort_list search_set_value) | (SEARCH BREADTH FIRST BY sort_list search_set_value))? (CYCLE alias_name_list SET var_name TO STRING_VALUE DEFAULT STRING_VALUE)?
    ;

mv_column_list
    : column_name_list
    | column_name_list Comma PRIMARY KEY LeftParen column_name_list RightParen ((USING INDEX opt_index_options) | (USING INDEX))?
    | PRIMARY KEY LeftParen column_name_list RightParen ((USING INDEX opt_index_options) | (USING INDEX))?
    ;

alias_name_list
    : column_alias_name (Comma column_alias_name)*
    ;

column_alias_name
    : column_name
    ;

search_set_value
    : SET var_name
    ;

analyze_stmt
    : ANALYZE TABLE relation_factor use_partition? analyze_statistics_clause
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
    | merge_stmt
    | update_stmt
    ;

format_name
    : TRADITIONAL
    | JSON
    ;

show_stmt
    : SHOW FULL? columns_or_fields from_or_in relation_factor (from_or_in database_factor)? ((LIKE STRING_VALUE) | (LIKE STRING_VALUE ESCAPE STRING_VALUE) | (WHERE expr))?
    | SHOW TABLE STATUS (from_or_in database_factor)? ((LIKE STRING_VALUE) | (LIKE STRING_VALUE ESCAPE STRING_VALUE) | (WHERE expr))?
    | SHOW (GLOBAL | SESSION | LOCAL)? VARIABLES ((LIKE STRING_VALUE) | (LIKE STRING_VALUE ESCAPE STRING_VALUE) | (WHERE expr))?
    | SHOW CREATE TABLE relation_factor
    | SHOW CREATE VIEW relation_factor
    | SHOW CREATE PROCEDURE relation_factor
    | SHOW CREATE FUNCTION relation_factor
    | SHOW CREATE TRIGGER relation_factor
    | SHOW GRANTS opt_for_grant_user
    | SHOW charset_key ((LIKE STRING_VALUE) | (LIKE STRING_VALUE ESCAPE STRING_VALUE) | (WHERE expr))?
    | SHOW TRACE ((LIKE STRING_VALUE) | (LIKE STRING_VALUE ESCAPE STRING_VALUE) | (WHERE expr))?
    | SHOW TRACE FORMAT COMP_EQ STRING_VALUE ((LIKE STRING_VALUE) | (LIKE STRING_VALUE ESCAPE STRING_VALUE) | (WHERE expr))?
    | SHOW COLLATION ((LIKE STRING_VALUE) | (LIKE STRING_VALUE ESCAPE STRING_VALUE) | (WHERE expr))?
    | SHOW PARAMETERS ((LIKE STRING_VALUE) | (LIKE STRING_VALUE ESCAPE STRING_VALUE) | (WHERE expr))? tenant_name?
    | SHOW FULL? PROCESSLIST
    | SHOW TABLEGROUPS ((LIKE STRING_VALUE) | (LIKE STRING_VALUE ESCAPE STRING_VALUE) | (WHERE expr))?
    | SHOW PRIVILEGES
    | SHOW RECYCLEBIN
    | SHOW CREATE TABLEGROUP relation_name
    ;

opt_for_grant_user
    : opt_for_user
    | FOR CURRENT_USER LeftParen RightParen
    ;

columns_or_fields
    : COLUMNS
    | FIELDS
    ;

from_or_in
    : FROM
    | IN
    ;

help_stmt
    : HELP STRING_VALUE
    | HELP NAME_OB
    ;

create_user_stmt
    : CREATE USER user_specification user_profile? (DEFAULT TABLESPACE tablespace)? (PRIMARY_ZONE opt_equal_mark primary_zone_name)?
    | CREATE USER user_specification require_specification user_profile? (DEFAULT TABLESPACE tablespace)? (PRIMARY_ZONE opt_equal_mark primary_zone_name)?
    ;

default_role_clause
    : role_opt_identified_by_list
    | ALL (EXCEPT role_list)?
    | NONE
    ;

alter_user_stmt
    : ALTER USER user_with_host_name DEFAULT ROLE default_role_clause
    | ALTER USER user_with_host_name PRIMARY_ZONE COMP_EQ? primary_zone_name
    | ALTER USER user_list GRANT CONNECT THROUGH user_list ((WITH ROLE role_list) | (WITH ROLE ALL EXCEPT role_list) | (WITH NO ROLE) | (WITH NO ROLES))?
    | ALTER USER user_list REVOKE CONNECT THROUGH user_list
    ;

alter_user_profile_stmt
    : ALTER USER user_with_host_name user_profile
    ;

alter_role_stmt
    : ALTER ROLE role (NOT IDENTIFIED)?
    | ALTER ROLE role IDENTIFIED BY password
    | ALTER ROLE role IDENTIFIED BY VALUES password
    ;

user_specification
    : user USER_VARIABLE? IDENTIFIED BY password
    | user USER_VARIABLE? IDENTIFIED BY VALUES password
    ;

require_specification
    : REQUIRE NONE
    | REQUIRE SSL
    | REQUIRE X509
    | REQUIRE tls_option_list
    ;

tls_option_list
    : tls_option
    | tls_option_list tls_option
    | tls_option_list AND tls_option
    ;

tls_option
    : CIPHER STRING_VALUE
    | ISSUER STRING_VALUE
    | SUBJECT STRING_VALUE
    ;

grant_user
    : user USER_VARIABLE?
    | CONNECT
    | RESOURCE
    | PUBLIC
    ;

grant_user_list
    : grant_user (Comma grant_user)*
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
    | CONNECT
    | RESOURCE
    | PUBLIC
    ;

password
    : INTNUM
    | NAME_OB
    | unreserved_keyword
    ;

password_str
    : STRING_VALUE
    ;

drop_user_stmt
    : DROP USER user_list CASCADE?
    ;

user_list
    : user_with_host_name (Comma user_with_host_name)*
    ;

set_password_stmt
    : SET PASSWORD COMP_EQ password_str
    | SET PASSWORD FOR user USER_VARIABLE? COMP_EQ password_str
    | SET PASSWORD COMP_EQ PASSWORD LeftParen password RightParen
    | SET PASSWORD FOR user USER_VARIABLE? COMP_EQ PASSWORD LeftParen password RightParen
    | ALTER USER user_with_host_name IDENTIFIED BY password
    | ALTER USER user_with_host_name IDENTIFIED BY VALUES password_str
    | ALTER USER user_with_host_name require_specification
    ;

opt_for_user
    : FOR user opt_host_name
    | empty
    ;

lock_user_stmt
    : ALTER USER user_list ACCOUNT lock_spec_mysql57
    ;

lock_spec_mysql57
    : LOCK
    | UNLOCK
    ;

lock_tables_stmt
    : LOCK_ table_or_tables lock_table_list
    ;

lock_table_stmt
    : LOCK TABLE lock_table_factors IN lock_mode MODE ((WAIT INTNUM) | NOWAIT)?
    ;

lock_table_factors
    : lock_table_factor (Comma lock_table_factor)*
    ;

lock_table_factor
    : relation_factor use_partition?
    ;

lock_mode
    : (ROW|SHARE ROW)? EXCLUSIVE
    | ROW? SHARE
    | SHARE UPDATE
    ;

unlock_tables_stmt
    : UNLOCK TABLES
    ;

lock_table_list
    : lock_table (Comma lock_table)*
    ;

create_context_stmt
    : CREATE (OR REPLACE)? CONTEXT relation_name USING context_package_name context_option
    ;

context_package_name
    : relation_name (Dot relation_name)?
    ;

lock_table
    : relation_factor (AS relation_name|relation_name?) lock_type
    ;

context_option
    : ACCESSED GLOBALLY
    | INITIALIZED GLOBALLY
    | INITIALIZED EXTERNALLY
    | empty
    ;

lock_type
    : READ LOCAL?
    | WRITE
    | LOW_PRIORITY WRITE
    ;

drop_context_stmt
    : DROP CONTEXT relation_name
    ;

create_sequence_stmt
    : CREATE SEQUENCE relation_factor sequence_option_list?
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
    : Plus? INTNUM
    | Minus INTNUM
    | Plus? DECIMAL_VAL
    | Minus DECIMAL_VAL
    ;

drop_sequence_stmt
    : DROP SEQUENCE relation_factor
    ;

alter_sequence_stmt
    : ALTER SEQUENCE relation_factor sequence_option_list?
    ;

create_dblink_stmt
    : CREATE DATABASE LINK dblink CONNECT TO user tenant IDENTIFIED BY password NAME_OB? ip_port (CLUSTER relation_name)? (MY_NAME user tenant IDENTIFIED BY password ip_port opt_cluster)?
    ;

drop_dblink_stmt
    : DROP DATABASE LINK dblink
    ;

dblink
    : relation_name
    ;

tenant
    : USER_VARIABLE
    ;

opt_cluster
    : CLUSTER relation_name
    | empty
    ;

begin_stmt
    : BEGIN HINT_VALUE? WORK?
    | START HINT_VALUE? TRANSACTION ((WITH CONSISTENT SNAPSHOT) | transaction_access_mode | (WITH CONSISTENT SNAPSHOT Comma transaction_access_mode) | (transaction_access_mode Comma WITH CONSISTENT SNAPSHOT))?
    ;

commit_stmt
    : COMMIT HINT_VALUE? WORK?
    | COMMIT HINT_VALUE? WORK? COMMENT STRING_VALUE
    ;

rollback_stmt
    : ROLLBACK WORK?
    | ROLLBACK HINT_VALUE WORK?
    ;

kill_stmt
    : KILL (CONNECTION?|QUERY) bit_expr
    ;

create_role_stmt
    : CREATE ROLE role (NOT IDENTIFIED)?
    | CREATE ROLE role IDENTIFIED BY password
    | CREATE ROLE role IDENTIFIED BY VALUES password
    ;

role_list
    : role (Comma role)*
    ;

role
    : STRING_VALUE
    | NAME_OB
    | DBA
    | RESOURCE
    | CONNECT
    | PUBLIC
    ;

drop_role_stmt
    : DROP ROLE role
    ;

set_role_stmt
    : SET ROLE default_role_clause
    ;

role_opt_identified_by_list
    : role_opt_identified_by (Comma role_opt_identified_by)*
    ;

role_opt_identified_by
    : role
    | role IDENTIFIED BY password
    ;

sys_and_obj_priv
    : priv_type
    | CREATE ((ANY? TABLE|SESSION)|ANY? (PROCEDURE|VIEW))
    | EXEMPT (ACCESS|REDACTION) POLICY
    | SYSDBA
    | SYSOPER
    | SYSBACKUP
    | ((((ALTER|BACKUP)|(DROP|LOCK))|((COMMENT|SELECT)|(INSERT|UPDATE)))|(DELETE|FLASHBACK)) ANY TABLE
    | ((ALTER|GRANT) ANY|(CREATE|DROP ANY)) ROLE
    | AUDIT ANY
    | GRANT ANY OBJECT? PRIVILEGE
    | (ALTER|CREATE) ANY INDEX
    | DROP ((ANY TYPE|PROFILE)|ANY (INDEX|VIEW))
    | SELECT ANY (DICTIONARY|SEQUENCE)
    | (ALTER|DROP) ANY ((PROCEDURE|SEQUENCE)|TRIGGER)
    | EXECUTE ANY (PROCEDURE|TYPE)
    | CREATE ((ANY? TYPE|PROFILE)|(ANY?|PUBLIC) SYNONYM)
    | DROP ((ANY OUTLINE|USER)|(ANY|PUBLIC) SYNONYM)
    | CREATE ((ANY OUTLINE|USER)|ANY? (SEQUENCE|TRIGGER))
    | ALTER (ANY TYPE|PROFILE)
    | ALTER (ANY OUTLINE|USER)
    | UNDER ANY TYPE
    | PURGE DBA_RECYCLEBIN
    | SYSKM
    | CREATE ((ANY DIRECTORY|TABLESPACE)|ANY CONTEXT)
    | ALTER TABLESPACE
    | DROP ((DATABASE LINK|TABLESPACE)|ANY (CONTEXT|DIRECTORY))
    | SHOW PROCESS
    | ALTER SYSTEM
    | CREATE PUBLIC? DATABASE LINK
    | (ALTER|DEBUG CONNECT) SESSION
    | ALTER DATABASE
    | DEBUG ANY PROCEDURE
    ;

grant_stmt
    : GRANT role_sys_obj_all_col_priv_list ON obj_clause TO grant_user_list (WITH GRANT OPTION)?
    | GRANT grant_system_privileges
    ;

grant_system_privileges
    : role_sys_obj_all_col_priv_list TO grantee_clause (WITH ADMIN OPTION)?
    ;

grantee_clause
    : grant_user_list
    | grant_user IDENTIFIED BY password
    ;

role_sys_obj_all_col_priv_list
    : role_sys_obj_all_col_priv (Comma role_sys_obj_all_col_priv)*
    ;

role_sys_obj_all_col_priv
    : role
    | sys_and_obj_priv (LeftParen column_list RightParen)?
    | ALL PRIVILEGES? (LeftParen column_list RightParen)?
    ;

priv_type
    : ALTER
    | CREATE
    | DELETE
    | DROP
    | GRANT OPTION
    | INSERT
    | UPDATE
    | SELECT
    | INDEX
    | SHOW VIEW
    | SHOW DATABASES
    | SUPER
    | PROCESS
    | USAGE
    | REFERENCES
    | EXECUTE
    | FLASHBACK
    | READ
    | WRITE
    | FILE_KEY
    | DEBUG
    ;

obj_clause
    : Star (Dot Star)?
    | relation_name Dot (Star|relation_name)
    | DIRECTORY? relation_name
    ;

revoke_stmt
    : REVOKE role_sys_obj_all_col_priv_list ON obj_clause FROM user_list
    | REVOKE role_sys_obj_all_col_priv_list FROM grantee_clause
    ;

prepare_stmt
    : PREPARE stmt_name FROM preparable_stmt
    ;

stmt_name
    : column_label
    ;

preparable_stmt
    : select_stmt
    | insert_stmt
    | merge_stmt
    | update_stmt
    | delete_stmt
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
    : bit_expr
    | ON
    | DEFAULT
    ;

var_and_val
    : USER_VARIABLE (set_var_op|to_or_eq) bit_expr
    | USER_VARIABLE to_or_eq PARSER_SYNTAX_ERROR
    | sys_var_and_val
    | (SYSTEM_VARIABLE|scope_or_scope_alias column_name) to_or_eq set_expr_or_default
    | NAMES charset_name_or_default collation?
    | charset_key charset_name_or_default
    ;

sys_var_and_val
    : obj_access_ref_normal to_or_eq set_expr_or_default
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

set_var_op
    : SET_VAR
    ;

argument
    : USER_VARIABLE
    ;

execute_stmt
    : EXECUTE stmt_name (USING argument_list)?
    ;

argument_list
    : argument (Comma argument)*
    ;

deallocate_prepare_stmt
    : deallocate_or_drop PREPARE stmt_name
    ;

deallocate_or_drop
    : DEALLOCATE
    | DROP
    ;

call_stmt
    : CALL routine_access_name call_param_list?
    ;

call_param_list
    : LeftParen func_param_list? RightParen
    ;

routine_access_name
    : var_name Dot (var_name Dot)? routine_name
    | routine_name
    ;

routine_name
    : NAME_OB
    | oracle_unreserved_keyword
    | unreserved_keyword_normal
    | aggregate_function_keyword
    | ADD
    | SET
    | MODIFY
    | DELETE
    ;

truncate_table_stmt
    : TRUNCATE TABLE? relation_factor
    ;

rename_table_stmt
    : RENAME rename_table_actions
    ;

rename_table_actions
    : rename_table_action
    ;

rename_table_action
    : relation_factor TO relation_factor
    ;

alter_index_stmt
    : ALTER INDEX relation_factor alter_index_actions
    ;

alter_index_actions
    : alter_index_action
    ;

alter_index_action
    : alter_index_option_oracle
    ;

alter_index_option_oracle
    : RENAME TO index_name
    | parallel_option
    | TABLESPACE tablespace
    ;

alter_table_stmt
    : ALTER EXTERNAL? TABLE relation_factor alter_table_actions
    | ALTER TABLE relation_factor alter_column_group_action
    | ALTER EXTERNAL TABLE relation_factor alter_external_table_action
    ;

alter_external_table_action
    : ADD PARTITION LeftParen add_external_table_partition_actions? RightParen LOCATION STRING_VALUE
    | DROP PARTITION LOCATION STRING_VALUE
    ;

add_external_table_partition_actions
    : add_external_table_partition_action
    | add_external_table_partition_actions Comma add_external_table_partition_action
    ;

add_external_table_partition_action
    : column_name COMP_EQ? expr_const
    ;

alter_table_actions
    : alter_table_action (Comma alter_table_action)*
    ;

alter_table_action
    : table_option_list_space_seperated
    | SET table_option_list_space_seperated
    | SET INTERVAL LeftParen bit_expr? RightParen
    | opt_alter_compress_option
    | alter_column_option
    | alter_tablegroup_option
    | RENAME TO? relation_factor
    | alter_index_option
    | alter_partition_option
    | modify_partition_info
    | DROP CONSTRAINT constraint_name
    | enable_option ALL TRIGGERS
    | REFRESH
    ;

alter_partition_option
    : DROP (PARTITION|SUBPARTITION) drop_partition_name_list
    | DROP (PARTITION|SUBPARTITION) drop_partition_name_list UPDATE GLOBAL INDEXES
    | RENAME (PARTITION|SUBPARTITION) relation_name TO relation_name
    | add_range_or_list_partition
    | SPLIT PARTITION relation_factor split_actions
    | TRUNCATE (PARTITION|SUBPARTITION) name_list
    | TRUNCATE (PARTITION|SUBPARTITION) name_list UPDATE GLOBAL INDEXES
    | MODIFY PARTITION relation_factor add_range_or_list_subpartition
    | EXCHANGE PARTITION relation_name WITH TABLE relation_factor INCLUDING INDEXES WITHOUT VALIDATION
    ;

drop_partition_name_list
    : name_list
    | LeftParen name_list RightParen
    ;

split_actions
    : VALUES LeftParen list_expr RightParen modify_special_partition
    | AT LeftParen range_expr_list RightParen modify_special_partition
    | split_range_partition
    | split_list_partition
    ;

add_range_or_list_partition
    : ADD range_partition_list
    | ADD list_partition_list
    ;

add_range_or_list_subpartition
    : ADD range_subpartition_list
    | ADD list_subpartition_list
    ;

modify_special_partition
    : INTO opt_special_partition_list
    | empty
    ;

split_range_partition
    : INTO opt_range_partition_list
    | INTO LeftParen range_partition_list Comma special_partition_list RightParen
    ;

split_list_partition
    : INTO opt_list_partition_list
    | INTO LeftParen list_partition_list Comma special_partition_list RightParen
    ;

modify_partition_info
    : MODIFY hash_partition_option
    | MODIFY list_partition_option
    | MODIFY range_partition_option
    ;

tg_modify_partition_info
    : MODIFY tg_hash_partition_option
    | MODIFY tg_range_partition_option
    | MODIFY tg_list_partition_option
    ;

alter_index_option
    : ADD out_of_line_constraint
    | ADD LeftParen out_of_line_constraint RightParen
    | ALTER INDEX index_name visibility_option
    | DROP PRIMARY KEY
    | MODIFY out_of_line_primary_index[false]
    | MODIFY CONSTRAINT constraint_name (RELY | NORELY)? enable_option? (VALIDATE | NOVALIDATE)?
    | enable_option (VALIDATE | NOVALIDATE)? CONSTRAINT constraint_name
    ;

visibility_option
    : VISIBLE
    | INVISIBLE
    ;

alter_column_group_action
    : (ADD|DROP) COLUMN GROUP LeftParen column_group_list RightParen
    ;

alter_column_option
    : ADD column_definition
    | ADD LeftParen column_definition_list RightParen
    | DROP COLUMN column_definition_ref (CASCADE | RESTRICT)?
    | DROP LeftParen column_list RightParen
    | RENAME COLUMN column_definition_ref TO column_name
    | MODIFY column_definition_opt_datatype
    | MODIFY LeftParen column_definition_opt_datatype_list RightParen
    ;

alter_tablegroup_option
    : DROP TABLEGROUP
    ;

flashback_stmt
    : FLASHBACK TABLE relation_factors TO BEFORE DROP (RENAME TO relation_factor)?
    | FLASHBACK database_key database_factor TO BEFORE DROP (RENAME TO database_factor)?
    | FLASHBACK TENANT relation_name TO BEFORE DROP (RENAME TO relation_name)?
    | FLASHBACK TABLE relation_factors TO TIMESTAMP bit_expr
    | FLASHBACK TABLE relation_factors TO SCN bit_expr
    ;

relation_factors
    : relation_factor (Comma relation_factor)*
    ;

purge_stmt
    : PURGE (((INDEX|TABLE) relation_factor|(RECYCLEBIN|database_key database_factor))|TENANT relation_name)
    ;

shrink_space_stmt
    : ALTER TABLE relation_factor SHRINK SPACE
    | ALTER TENANT (ALL|relation_name) SHRINK SPACE
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

auditing_by_user_clause
    : BY user_list
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
    | PUBLIC? (DATABASE LINK|SYNONYM)
    | DIRECTORY
    | MATERIALIZED? VIEW
    | NOT EXISTS
    | OUTLINE
    | EXECUTE? PROCEDURE
    | PROFILE
    | ROLE
    | ALTER? SEQUENCE
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
    | GRANT SEQUENCE
    | GRANT TABLE
    | INSERT TABLE?
    | SELECT SEQUENCE?
    | SELECT TABLE
    | UPDATE TABLE?
    | EXECUTE
    | FLASHBACK
    | INDEX
    | RENAME
    ;

alter_system_stmt
    : ALTER SYSTEM BOOTSTRAP (CLUSTER partition_role)? server_info_list (PRIMARY_ROOTSERVICE_LIST STRING_VALUE)?
    | ALTER SYSTEM FLUSH cache_type_or_string CACHE namespace_expr? flush_scope
    | ALTER SYSTEM FLUSH KVCACHE tenant_name? cache_name?
    | ALTER SYSTEM FLUSH SS_MICRO_CACHE tenant_name?
    | ALTER SYSTEM FLUSH ILOGCACHE file_id?
    | ALTER SYSTEM ALTER PLAN BASELINE tenant_name? sql_id_expr? baseline_id_expr? SET baseline_asgn_factor
    | ALTER SYSTEM LOAD PLAN BASELINE FROM PLAN CACHE (TENANT COMP_EQ tenant_name_list)? sql_id_expr?
    | ALTER SYSTEM SWITCH REPLICA partition_role partition_id_or_server_or_zone
    | ALTER SYSTEM SWITCH ROOTSERVICE partition_role server_or_zone
    | ALTER SYSTEM alter_or_change_or_modify REPLICA partition_id_desc ip_port alter_or_change_or_modify change_actions FORCE?
    | ALTER SYSTEM DROP REPLICA partition_id_desc ip_port (CREATE_TIMESTAMP opt_equal_mark INTNUM)? zone_desc? FORCE?
    | ALTER SYSTEM migrate_action REPLICA partition_id_desc SOURCE COMP_EQ? STRING_VALUE DESTINATION COMP_EQ? STRING_VALUE FORCE?
    | ALTER SYSTEM REPORT REPLICA server_or_zone?
    | ALTER SYSTEM RECYCLE REPLICA server_or_zone?
    | ALTER SYSTEM START MERGE zone_desc
    | ALTER SYSTEM suspend_or_resume MERGE tenant_list_tuple_v2?
    | ALTER SYSTEM CLEAR MERGE ERROR_P tenant_list_tuple_v2?
    | ALTER SYSTEM CANCEL cancel_task_type TASK STRING_VALUE
    | ALTER SYSTEM MAJOR FREEZE ((tenant_list_tuple opt_tablet_id) | (tenant_list_tuple ls opt_tablet_id) | opt_tablet_id_no_empty)? (REBUILD COLUMN GROUP)?
    | ALTER SYSTEM CHECKPOINT
    | ALTER SYSTEM MINOR FREEZE ((tenant_list_tuple opt_tablet_id) | (tenant_list_tuple ls opt_tablet_id) | opt_tablet_id_no_empty)? (SERVER opt_equal_mark LeftParen server_list RightParen)? zone_desc?
    | ALTER SYSTEM ARCHIVELOG (TENANT opt_equal_mark tenant_name_list)? (DESCRIPTION opt_equal_mark STRING_VALUE)?
    | ALTER SYSTEM NOARCHIVELOG (TENANT opt_equal_mark tenant_name_list)? (DESCRIPTION opt_equal_mark STRING_VALUE)?
    | ALTER SYSTEM BACKUP DATABASE (TO opt_equal_mark STRING_VALUE)? (DESCRIPTION opt_equal_mark STRING_VALUE)?
    | ALTER SYSTEM BACKUP INCREMENTAL DATABASE (TO opt_equal_mark STRING_VALUE)? (DESCRIPTION opt_equal_mark STRING_VALUE)?
    | ALTER SYSTEM BACKUP DATABASE (TO opt_equal_mark STRING_VALUE)? PLUS ARCHIVELOG (DESCRIPTION opt_equal_mark STRING_VALUE)?
    | ALTER SYSTEM BACKUP INCREMENTAL DATABASE (TO opt_equal_mark STRING_VALUE)? PLUS ARCHIVELOG (DESCRIPTION opt_equal_mark STRING_VALUE)?
    | ALTER SYSTEM BACKUP KEY (TO opt_equal_mark STRING_VALUE)? (ENCRYPTED BY STRING_VALUE)?
    | ALTER SYSTEM alter_or_change_or_modify EXTERNAL_STORAGE_DEST opt_path_info SET? (ACCESS_INFO opt_equal_mark STRING_VALUE)? (ATTRIBUTE opt_equal_mark STRING_VALUE)?
    | ALTER SYSTEM CANCEL BACKUP (TENANT opt_equal_mark tenant_name_list)?
    | SET ENCRYPTION ON IDENTIFIED BY STRING_VALUE ONLY
    | ALTER SYSTEM CANCEL DELETE BACKUP (TENANT opt_equal_mark tenant_name_list)? (DESCRIPTION opt_equal_mark STRING_VALUE)?
    | ALTER SYSTEM ADD DELETE BACKUP policy_name (RECOVERY_WINDOW opt_equal_mark STRING_VALUE)? (REDUNDANCY opt_equal_mark INTNUM)? (BACKUP_COPIES opt_equal_mark INTNUM)? (TENANT opt_equal_mark tenant_name_list)?
    | ALTER SYSTEM DROP DELETE BACKUP policy_name (TENANT opt_equal_mark tenant_name_list)?
    | ALTER SYSTEM CLEAR ROOTTABLE tenant_name?
    | ALTER SYSTEM server_action SERVER server_list zone_desc?
    | ALTER SYSTEM ADD ZONE relation_name_or_string add_or_alter_zone_options
    | ALTER SYSTEM zone_action ZONE relation_name_or_string
    | ALTER SYSTEM alter_or_change_or_modify ZONE relation_name_or_string SET? add_or_alter_zone_options
    | ALTER SYSTEM REFRESH SCHEMA server_or_zone?
    | ALTER SYSTEM SET_TP alter_system_settp_actions
    | ALTER SYSTEM CLEAR LOCATION CACHE server_or_zone?
    | ALTER SYSTEM REMOVE BALANCE TASK (TENANT COMP_EQ tenant_name_list)? (ZONE COMP_EQ zone_list)? (TYPE opt_equal_mark balance_task_type)?
    | ALTER SYSTEM RELOAD GTS
    | ALTER SYSTEM RELOAD UNIT
    | ALTER SYSTEM RELOAD SERVER
    | ALTER SYSTEM RELOAD ZONE
    | ALTER SYSTEM MIGRATE UNIT COMP_EQ? INTNUM DESTINATION COMP_EQ? STRING_VALUE
    | ALTER SYSTEM CANCEL MIGRATE UNIT INTNUM
    | ALTER SYSTEM ADD REPLICA ls SERVER COMP_EQ? STRING_VALUE REPLICA_TYPE COMP_EQ? STRING_VALUE (DATA_SOURCE opt_equal_mark STRING_VALUE)? (PAXOS_REPLICA_NUM opt_equal_mark INTNUM)? tenant_name?
    | ALTER SYSTEM REMOVE REPLICA ls SERVER COMP_EQ? STRING_VALUE (PAXOS_REPLICA_NUM opt_equal_mark INTNUM)? tenant_name?
    | ALTER SYSTEM MIGRATE REPLICA ls SOURCE COMP_EQ? STRING_VALUE DESTINATION COMP_EQ? STRING_VALUE (DATA_SOURCE opt_equal_mark STRING_VALUE)? tenant_name?
    | ALTER SYSTEM MODIFY REPLICA ls SERVER COMP_EQ? STRING_VALUE REPLICA_TYPE COMP_EQ? STRING_VALUE (PAXOS_REPLICA_NUM opt_equal_mark INTNUM)? tenant_name?
    | ALTER SYSTEM MODIFY ls PAXOS_REPLICA_NUM COMP_EQ? INTNUM tenant_name?
    | ALTER SYSTEM CANCEL REPLICA TASK TASK_ID COMP_EQ? STRING_VALUE tenant_name?
    | ALTER SYSTEM UPGRADE VIRTUAL SCHEMA
    | ALTER SYSTEM RUN JOB STRING_VALUE server_or_zone?
    | ALTER SYSTEM upgrade_action UPGRADE
    | ALTER SYSTEM REFRESH TIME_ZONE_INFO
    | ALTER SYSTEM ENABLE SQL THROTTLE (FOR PRIORITY COMP_LE INTNUM)? opt_sql_throttle_using_cond
    | ALTER SYSTEM DISABLE SQL THROTTLE
    | ALTER SYSTEM SET DISK VALID ip_port
    | ALTER SYSTEM DROP TABLES IN SESSION INTNUM
    | ALTER SYSTEM REFRESH TABLES IN SESSION INTNUM
    | ALTER SYSTEM SET alter_system_set_clause_list
    | ALTER SYSTEM KILL SESSION bit_expr IMMEDIATE
    | ALTER SYSTEM KILL SESSION bit_expr
    | ALTER SYSTEM RESET alter_system_reset_clause_list
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

alter_system_set_clause_list
    : alter_system_set_clause+
    ;

alter_system_set_clause
    : set_system_parameter_clause
    ;

alter_system_reset_clause_list
    : alter_system_reset_clause+
    ;

alter_system_reset_clause
    : reset_system_parameter_clause
    ;

set_system_parameter_clause
    : var_name COMP_EQ bit_expr
    ;

reset_system_parameter_clause
    : var_name
    ;

cache_type_or_string
    : cache_type
    | NAME_OB
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
    | LIB
    ;

balance_task_type
    : AUTO
    | MANUAL
    | ALL
    ;

tenant_list_tuple
    : TENANT COMP_EQ? LeftParen tenant_name_list RightParen
    ;

tenant_list_tuple_v2
    : TENANT COMP_EQ? tenant_name_list
    ;

tenant_name_list
    : relation_name_or_string (Comma relation_name_or_string)*
    ;

opt_path_info
    : PATH opt_equal_mark STRING_VALUE
    ;

policy_name
    : POLICY COMP_EQ? STRING_VALUE
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
    ;

server_list
    : STRING_VALUE (Comma STRING_VALUE)*
    ;

zone_action
    : DELETE
    | START
    | FORCE? STOP
    ;

ip_port
    : SERVER COMP_EQ? STRING_VALUE
    | HOST STRING_VALUE
    ;

zone_desc
    : ZONE COMP_EQ? relation_name_or_string
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

ls
    : LS COMP_EQ? INTNUM
    ;

partition_id_desc
    : PARTITION_ID COMP_EQ? STRING_VALUE
    ;

partition_id_or_server_or_zone
    : partition_id_desc ip_port
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

baseline_asgn_factor
    : column_name COMP_EQ literal
    ;

tenant_name
    : TENANT COMP_EQ? relation_name_or_string
    ;

namespace_expr
    : NAMESPACE COMP_EQ? STRING_VALUE
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

partition_role
    : LEADER
    | FOLLOWER
    ;

upgrade_action
    : BEGIN
    | END
    ;

alter_session_stmt
    : ALTER SESSION SET CURRENT_SCHEMA COMP_EQ current_schema
    | ALTER SESSION SET ISOLATION_LEVEL COMP_EQ session_isolation_level
    | ALTER SESSION SET alter_session_set_clause
    | ALTER SESSION FORCE var_name_of_forced_module PARALLEL INTNUM
    | ALTER SESSION switch_option var_name_of_module
    ;

var_name_of_forced_module
    : PARALLEL DML
    | PARALLEL QUERY
    | PARALLEL DDL
    ;

var_name_of_module
    : PARALLEL DML
    | PARALLEL QUERY
    | PARALLEL DDL
    ;

switch_option
    : ENABLE
    | DISABLE
    ;

session_isolation_level
    : isolation_level
    ;

alter_session_set_clause
    : set_system_parameter_clause_list
    ;

set_system_parameter_clause_list
    : set_system_parameter_clause+
    ;

current_schema
    : relation_name
    ;

set_comment_stmt
    : COMMENT ON TABLE normal_relation_factor IS STRING_VALUE
    | COMMENT ON COLUMN column_definition_ref IS STRING_VALUE
    ;

create_tablespace_stmt
    : CREATE TABLESPACE tablespace permanent_tablespace
    ;

drop_tablespace_stmt
    : DROP TABLESPACE tablespace
    ;

tablespace
    : NAME_OB
    ;

alter_tablespace_stmt
    : ALTER TABLESPACE tablespace alter_tablespace_actions
    ;

alter_tablespace_actions
    : alter_tablespace_action (Comma alter_tablespace_action)?
    ;

alter_tablespace_action
    : permanent_tablespace_option
    ;

permanent_tablespace
    : permanent_tablespace_options?
    ;

permanent_tablespace_options
    : permanent_tablespace_option (Comma permanent_tablespace_option)*
    ;

permanent_tablespace_option
    : ENCRYPTION USING STRING_VALUE
    ;

create_profile_stmt
    : CREATE PROFILE profile_name LIMIT password_parameters
    ;

alter_profile_stmt
    : ALTER PROFILE profile_name LIMIT password_parameters
    ;

drop_profile_stmt
    : DROP PROFILE profile_name
    ;

profile_name
    : NAME_OB
    | unreserved_keyword
    | DEFAULT
    ;

password_parameters
    : password_parameter+
    ;

password_parameter
    : password_parameter_type password_parameter_value
    ;

verify_function_name
    : relation_name
    | NULLX
    ;

password_parameter_value
    : number_literal
    | verify_function_name
    | DEFAULT
    ;

password_parameter_type
    : FAILED_LOGIN_ATTEMPTS
    | PASSWORD_LOCK_TIME
    | PASSWORD_VERIFY_FUNCTION
    | PASSWORD_LIFE_TIME
    | PASSWORD_GRACE_TIME
    ;

user_profile
    : PROFILE profile_name
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

for_all
    : FOR ALL (INDEXED | HIDDEN_)? COLUMNS size_clause?
    ;

size_clause
    : SIZE AUTO
    | SIZE REPEAT
    | SIZE SKEWONLY
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
    | extension
    ;

extension
    : LeftParen column_name_list RightParen
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

switchover_tenant_stmt
    : ALTER SYSTEM switchover_clause VERIFY?
    ;

switchover_clause
    : ACTIVATE STANDBY tenant_name?
    | SWITCHOVER TO PRIMARY tenant_name?
    | SWITCHOVER TO STANDBY tenant_name?
    ;

recover_tenant_stmt
    : ALTER SYSTEM RECOVER STANDBY tenant_name? recover_point_clause
    ;

recover_point_clause
    : ((UNTIL TIME opt_equal_mark STRING_VALUE) | (UNTIL SCN opt_equal_mark INTNUM))?
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
    : TABLE_ID COMP_EQ? INTNUM Comma OBJECT_ID COMP_EQ? INTNUM
    ;

cancel_transfer_partition_clause
    : part_info
    | ALL
    ;

service_name_stmt
    : ALTER SYSTEM service_op SERVICE relation_name tenant_name?
    ;

service_op
    : CREATE
    | DELETE
    | START
    | STOP
    ;

create_savepoint_stmt
    : SAVEPOINT var_name
    ;

rollback_savepoint_stmt
    : ROLLBACK WORK? TO var_name
    | ROLLBACK TO SAVEPOINT var_name
    ;

var_name
    : NAME_OB
    | oracle_unreserved_keyword
    | unreserved_keyword_normal
    | aggregate_function_keyword
    ;

column_name
    : NAME_OB
    | unreserved_keyword
    | ROWID
    ;

relation_name
    : NAME_OB
    | unreserved_keyword
    ;

exists_function_name
    : EXISTS
    ;

function_name
    : NAME_OB
    | oracle_unreserved_keyword
    | unreserved_keyword_normal
    | oracle_pl_non_reserved_words
    | PRIOR
    | RANDOM
    ;

column_label
    : NAME_OB
    | unreserved_keyword
    ;

keystore_name
    : NAME_OB
    | unreserved_keyword
    ;

date_unit
    : YEAR
    | MONTH
    | DAY
    | HOUR
    | MINUTE
    | SECOND
    ;

timezone_unit
    : TIMEZONE_HOUR
    | TIMEZONE_MINUTE
    | TIMEZONE_REGION
    | TIMEZONE_ABBR
    ;

date_unit_for_extract
    : date_unit
    | timezone_unit
    ;

json_mergepatch_expr
    : JSON_MERGEPATCH LeftParen bit_expr Comma bit_expr js_mp_return_clause? json_mergepatch_opt RightParen
    ;

json_mergepatch_opt
    : opt_json_mergepatch json_mergepatch_on_error?
    ;

json_mergepatch_on_error
    : (ERROR_P | NULLX) ON ERROR_P
    ;

opt_json_mergepatch
    : ASCII? PRETTY? TRUNCATE?
    | PRETTY ASCII TRUNCATE?
    | TRUNCATE ASCII? PRETTY
    | TRUNCATE PRETTY? ASCII
    ;

js_mp_return_clause
    : RETURNING js_return_type
    ;

json_array_expr
    : JSON_ARRAY LeftParen json_array_content? RightParen
    | JSON LeftBracket json_array_content RightBracket
    ;

json_array_content
    : js_array_eles json_array_on_null? js_array_return_clause? STRICT?
    ;

json_array_on_null
    : (ABSENT | NULLX) ON NULLX
    ;

js_array_eles
    : js_array_ele (Comma js_array_ele)*
    ;

js_array_ele
    : bit_expr (FORMAT JSON)?
    ;

js_array_return_clause
    : RETURNING js_return_type
    ;

json_value_expr
    : JSON_VALUE LeftParen js_doc_expr Comma js_literal opt_js_value_returning_type json_value_opt RightParen
    ;

json_value_opt
    : TRUNCATE? ASCII? json_value_on_opt?
    ;

json_equal_expr
    : JSON_EQUAL LeftParen func_param_list json_equal_option? RightParen
    ;

json_value_on_opt
    : json_value_on_empty
    | json_value_on_error
    | json_value_on_empty json_value_on_error
    | json_value_on_error json_value_on_empty
    | opt_on_mismatchs
    | json_value_on_empty opt_on_mismatchs
    | json_value_on_error opt_on_mismatchs
    | json_value_on_empty json_value_on_error opt_on_mismatchs
    | json_value_on_error json_value_on_empty opt_on_mismatchs
    ;

js_doc_expr
    : bit_expr (FORMAT JSON)?
    ;

opt_js_value_returning_type
    : RETURNING (NCHAR nstring_length_i|js_value_return_type)
    | RETURNING NVARCHAR2
    | RETURNING CHAR string_length_i? BINARY?
    | RETURNING RAW
    | js_return_default_type
    ;

json_value_on_empty
    : json_value_on_empty_response
    ;

json_value_on_empty_response
    : (DEFAULT signed_literal|json_value_on_response) ON EMPTY
    ;

json_value_on_error
    : json_value_on_error_response
    ;

json_value_on_error_response
    : (DEFAULT signed_literal|json_value_on_response) ON ERROR_P
    ;

opt_on_mismatchs
    : opt_on_mismatch+
    ;

opt_on_mismatch
    : (IGNORE|json_value_on_response) ON MISMATCH
    | (IGNORE|json_value_on_response) ON MISMATCH LeftParen mismatch_type_list RightParen
    ;

json_value_on_response
    : ERROR_P
    | NULLX
    ;

mismatch_type_list
    : mismatch_type (Comma mismatch_type)*
    ;

mismatch_type
    : MISSING DATA
    | EXTRA DATA
    | TYPE ERROR_P
    | empty
    ;

json_exists_expr
    : JSON_EXISTS LeftParen js_doc_expr Comma literal opt_json_exist? RightParen
    ;

opt_json_exist
    : PASSING passing_elements opt_json_exists_on_error_on_empty?
    | opt_json_exists_on_error_on_empty
    ;

passing_elements
    : passing_context (Comma passing_context)*
    ;

passing_context
    : bit_expr AS sql_var_name
    ;

sql_var_name
    : NAME_OB
    ;

opt_json_exists_on_error_on_empty
    : json_exists_on_error json_exists_on_empty?
    | json_exists_on_empty
    ;

json_exists_on_error
    : json_exists_response_type ON ERROR_P
    ;

json_exists_on_empty
    : json_exists_response_type ON EMPTY
    ;

json_exists_response_type
    : BOOL_VALUE
    | ERROR_P
    ;

json_query_expr
    : JSON_QUERY LeftParen js_doc_expr Comma js_literal (RETURNING js_query_return_type)? json_query_opt RightParen
    ;

json_query_opt
    : TRUNCATE? scalars_opt? PRETTY? ASCII? wrapper_opts? ASIS? json_query_on_opt? MULTIVALUE?
    ;

json_query_on_opt
    : on_empty_query
    | on_error_query
    | on_mismatch_query
    | on_error_query on_empty_query
    | on_empty_query on_error_query
    | on_error_query on_mismatch_query
    | on_empty_query on_mismatch_query
    | on_error_query on_empty_query on_mismatch_query
    | on_empty_query on_error_query on_mismatch_query
    ;

wrapper_opts
    : WITHOUT WRAPPER
    | WITHOUT ARRAY WRAPPER
    | WITH WRAPPER
    | WITH ARRAY WRAPPER
    | WITH UNCONDITIONAL WRAPPER
    | WITH CONDITIONAL WRAPPER
    | WITH UNCONDITIONAL ARRAY WRAPPER
    | WITH CONDITIONAL ARRAY WRAPPER
    ;

js_query_return_type
    : js_value_return_type
    | BLOB
    | JSON
    ;

on_mismatch_query
    : (DOT|opt_response_query) ON MISMATCH
    ;

on_error_query
    : opt_response_query_on_empty_error ON ERROR_P
    ;

on_empty_query
    : opt_response_query_on_empty_error ON EMPTY
    ;

opt_response_query_on_empty_error
    : EMPTY ARRAY?
    | EMPTY OBJECT
    | opt_response_query
    ;

opt_response_query
    : ERROR_P
    | NULLX
    ;

xml_table_expr
    : XMLTABLE LeftParen (opt_xml_table_ns Comma)? opt_xml_table_path? opt_xml_passing_clause? opt_sequence_by_ref? opt_columns_clause? RightParen
    ;

opt_columns_clause
    : COLUMNS xml_table_columns_list
    ;

opt_sequence_by_ref
    : RETURNING SEQUENCE BY REF
    ;

opt_xml_passing_clause
    : PASSING simple_expr
    | PASSING BY VALUE simple_expr
    ;

opt_xml_table_path
    : PATH complex_string_literal
    | complex_string_literal
    ;

opt_xml_table_ns
    : XMLNAMESPACES LeftParen xml_ns_list RightParen
    ;

xml_ns_list
    : xml_ns (Comma xml_ns)*
    ;

xml_ns
    : STRING_VALUE AS xml_identifier
    | DEFAULT STRING_VALUE
    ;

xml_identifier
    : relation_name
    ;

xml_table_columns_list
    : xml_table_column (Comma xml_table_column)*
    ;

xml_table_column
    : xml_table_ordinality_column_def
    | xml_table_value_column_def
    | xml_table_query_column_def
    ;

xml_table_ordinality_column_def
    : column_name FOR ORDINALITY
    ;

xml_table_value_column_def
    : column_name cast_data_type? opt_xml_table_path? opt_xml_table_default_value?
    ;

xml_table_query_column_def
    : column_name XMLTYPE opt_seq_by_ref_with_bracket? opt_xml_table_path? opt_xml_table_default_value?
    ;

opt_seq_by_ref_with_bracket
    : LeftParen SEQUENCE RightParen BY REF
    ;

opt_xml_table_default_value
    : DEFAULT bit_expr
    ;

opt_json_table_on_error_on_empty
    : json_table_on_error
    | json_table_on_empty
    | json_table_on_error json_table_on_empty
    ;

json_table_columns_def_opt
    : json_table_columns_def
    | LeftParen json_table_columns_def RightParen
    ;

json_table_expr
    : JSON_TABLE LeftParen js_doc_expr (Comma literal)? opt_json_table_on_error_on_empty? COLUMNS json_table_columns_def_opt RightParen
    ;

json_table_columns_def
    : json_table_column_def (Comma json_table_column_def)*
    ;

json_table_column_def
    : json_table_ordinality_column_def
    | json_table_exists_column_def
    | json_table_query_column_def
    | json_table_value_column_def
    | json_table_nested_column_def
    ;

json_table_ordinality_column_def
    : column_name FOR ORDINALITY
    ;

json_table_column_def_path
    : PATH literal
    | PATH column_name
    | PATH column_name dot_notation_path
    ;

json_table_exists_column_def
    : column_name opt_jt_value_type TRUNCATE? EXISTS json_table_column_def_path? ASIS? opt_json_exists_on_error_on_empty?
    ;

json_table_query_column_def
    : column_name opt_jt_query_type FORMAT JSON TRUNCATE? scalars_opt? wrapper_opts? json_table_column_def_path? ASIS? json_query_on_opt?
    | column_name JSON scalars_opt? wrapper_opts? json_table_column_def_path? ASIS? json_query_on_opt?
    ;

json_table_value_column_def
    : column_name opt_jt_value_type TRUNCATE? json_table_column_def_path? ASIS? json_value_on_opt?
    ;

json_table_nested_column_def
    : NESTED PATH literal COLUMNS LeftParen json_table_columns_def RightParen
    ;

opt_jt_query_type
    : js_return_type
    | js_return_default_type
    ;

opt_jt_value_type
    : js_value_return_type
    | int_type_i
    | CHAR string_length_i? BINARY?
    | NVARCHAR2 nstring_length_i
    | NCHAR nstring_length_i
    | js_return_default_type
    ;

js_value_return_type
    : datetime_type_i
    | timestamp_type_i
    | NUMBER number_precision?
    | double_type_i
    | interval_type_i
    | js_return_text_type
    ;

js_return_type
    : BLOB
    | JSON
    | js_return_text_type
    ;

js_return_default_type
    : empty
    ;

js_return_text_type
    : CLOB
    | varchar_type_i string_length_i BINARY?
    | varchar_type_i
    ;

json_table_on_response
    : ERROR_P
    | NULLX
    | DEFAULT signed_literal
    ;

json_table_on_error
    : json_table_on_response ON ERROR_P
    ;

json_table_on_empty
    : json_table_on_response ON EMPTY
    ;

json_object_expr
    : JSON_OBJECT LeftParen opt_json_object_content RightParen
    | JSON LeftBrace opt_json_object_content RightBrace
    ;

opt_json_object_content
    : entry_op? opt_json_object_clause
    | entry_op STRICT json_obj_unique_key?
    | entry_op? json_obj_unique_key
    ;

opt_json_object_clause
    : empty
    | (js_on_null json_obj_returning_type?|json_obj_returning_type) STRICT? json_obj_unique_key?
    ;

entry_op
    : Star
    | entry_set
    ;

entry_set
    : entry_obj (Comma entry_obj)*
    ;

entry_obj
    : regular_entry_obj (FORMAT JSON)?
    ;

regular_entry_obj
    : JSON_OBJECT_VALUE
    | KEY? json_obj_literal_expr VALUE json_obj_literal_expr
    | json_obj_literal_expr (Colon json_obj_literal_expr)?
    ;

json_obj_literal_expr
    : bit_expr
    ;

js_on_null
    : (ABSENT|NULLX) ON NULLX
    ;

json_obj_returning_type
    : RETURNING js_return_type
    ;

json_obj_unique_key
    : WITH UNIQUE KEYS
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

xmlparse_expr
    : XMLPARSE LeftParen xml_doc_type xml_text WELLFORMED? RightParen
    ;

xml_text
    : bit_expr
    ;

xml_doc_type
    : DOCUMENT
    | CONTENT
    ;

xml_element_expr
    : XMLELEMENT LeftParen xml_tag (Comma xml_attributes_expr)? RightParen
    | XMLELEMENT LeftParen xml_tag Comma (xml_attributes_expr Comma)? xml_value_clause RightParen
    ;

xml_tag
    : ENTITYESCAPING? element_name
    | NOENTITYESCAPING element_name
    ;

evalname_expr
    : simple_expr
    | evalname_expr CNNOP evalname_expr
    ;

element_name
    : NAME? column_name
    | EVALNAME evalname_expr
    ;

xml_value_clause
    : xml_value (Comma xml_value)*
    ;

xml_value
    : bit_expr (AS column_label|column_label?)
    ;

xml_attributes_expr
    : XMLATTRIBUTES LeftParen (ENTITYESCAPING?|NOENTITYESCAPING) (NOSCHEMACHECK|SCHEMACHECK?) xml_attributes_value_clause RightParen
    ;

xml_attributes_value_clause
    : xml_attributes_value (Comma xml_attributes_value_clause)?
    ;

attributes_name_value
    : bit_expr
    ;

xml_attributes_value
    : attributes_name_value ((AS EVALNAME bit_expr) | (AS relation_name))?
    ;

xml_sequence_expr
    : XMLSEQUENCE LeftParen bit_expr RightParen
    ;

insert_child_xml
    : INSERTCHILDXML LeftParen bit_expr Comma bit_expr Comma bit_expr Comma bit_expr (Comma bit_expr)? RightParen
    ;

delete_xml
    : DELETEXML LeftParen bit_expr Comma bit_expr (Comma bit_expr)? RightParen
    ;

xml_extract_expr
    : EXTRACT LeftParen bit_expr Comma bit_expr (Comma literal)? RightParen
    ;

xmlcast_expr
    : XMLCAST LeftParen bit_expr AS cast_data_type RightParen
    ;

xmlserialize_expr
    : XMLSERIALIZE LeftParen xml_doc_type bit_expr (AS cast_data_type)? (ENCODING STRING_VALUE)? (VERSION literal)? ((NO INDENT) | INDENT | (INDENT SIZE COMP_EQ signed_int_num))? ((HIDE DEFAULTS) | (SHOW DEFAULTS))? RightParen
    ;

unreserved_keyword
    : oracle_unreserved_keyword
    | unreserved_keyword_normal
    | aggregate_function_keyword
    | STAT
    | LOG_LEVEL
    | CLIENT_VERSION
    ;

aggregate_function_keyword
    : COUNT
    | MAX
    | MIN
    | SUM
    | AVG
    | APPROX_COUNT_DISTINCT
    | APPROX_COUNT_DISTINCT_SYNOPSIS
    | APPROX_COUNT_DISTINCT_SYNOPSIS_MERGE
    | MEDIAN
    | STDDEV
    | VARIANCE
    | STDDEV_POP
    | STDDEV_SAMP
    | LISTAGG
    | RANK
    | DENSE_RANK
    | PERCENT_RANK
    | ROW_NUMBER
    | NTILE
    | CUME_DIST
    | FIRST_VALUE
    | LAST_VALUE
    | LEAD
    | LAG
    | NTH_VALUE
    | RATIO_TO_REPORT
    | CORR
    | COVAR_POP
    | COVAR_SAMP
    | VAR_POP
    | VAR_SAMP
    | REGR_SLOPE
    | REGR_INTERCEPT
    | REGR_COUNT
    | REGR_R2
    | REGR_AVGX
    | REGR_AVGY
    | REGR_SXX
    | REGR_SYY
    | REGR_SXY
    | PERCENTILE_CONT
    | PERCENTILE_DISC
    | WM_CONCAT
    | TOP_K_FRE_HIST
    | HYBRID_HIST
    ;

oracle_unreserved_keyword
    : ACCESSED
    | ADMIN
    | AFTER
    | ALLOCATE
    | ANALYZE
    | ARCHIVE
    | ARCHIVELOG
    | AUTHORIZATION
    | BACKUP
    | BECOME
    | BEFORE
    | BEGIN
    | BLOCK
    | BODY
    | CACHE
    | CANCEL
    | CASCADE
    | CHANGE
    | CHARACTER
    | CHECKPOINT
    | CLOSE
    | COBOL
    | COMMIT
    | COMPILE
    | CONSTRAINT
    | CONSTRAINTS
    | CONTENTS
    | CONTINUE
    | CONTROLFILE
    | CURSOR
    | CYCLE
    | DATABASE
    | DATAFILE
    | DBA
    | DEC
    | DECLARE
    | DISABLE
    | DISMOUNT
    | DOP
    | DOUBLE
    | DUMP
    | EACH
    | ENABLE
    | END
    | ESCAPE
    | EVENTS
    | EXCEPT
    | EXCEPTIONS
    | EXEC
    | EXECUTE
    | EXPLAIN
    | EXTENT
    | EXTERNAL
    | EXTERNALLY
    | FETCH
    | FLUSH
    | FORCE
    | FOREIGN
    | FORTRAN
    | FOUND
    | FREELIST
    | FREELISTS
    | FUNCTION
    | GLOBALLY
    | GO
    | GOTO
    | GROUPS
    | INCLUDING
    | INDICATOR
    | INITIALIZED
    | INITRANS
    | INSTANCE
    | INT
    | KEY
    | LATERAL
    | LANGUAGE
    | LAYER
    | LINK
    | LISTS
    | LOGFILE
    | MANAGE
    | MANUAL
    | MAXDATAFILES
    | MAXINSTANCES
    | MAXLOGFILES
    | MAXLOGHISTORY
    | MAXLOGMEMBERS
    | MAXTRANS
    | MAXVALUE
    | MINEXTENTS
    | MINVALUE
    | MODULE
    | MOUNT
    | NEW
    | NEXT
    | NOARCHIVELOG
    | NOCACHE
    | NOCYCLE
    | NOMAXVALUE
    | NOMINVALUE
    | NONE
    | NOORDER
    | NORESETLOGS
    | NOSORT
    | NUMERIC
    | OFF
    | OLD
    | ONLY
    | OPEN
    | OPTIMAL
    | OWN
    | PACKAGE_KEY
    | PARALLEL
    | NOPARALLEL
    | PCTINCREASE
    | PCTUSED
    | PLAN
    | PLI
    | PRECISION
    | PRIMARY
    | PRIVATE
    | PROCEDURE
    | PROFILE
    | QUOTA
    | READ
    | REAL
    | RECOVER
    | REFERENCES
    | REFERENCING
    | RESETLOGS
    | RESTRICTED
    | REUSE
    | ROLE
    | ROLES
    | ROLLBACK
    | SAVEPOINT
    | SCHEMA
    | SCN
    | SECTION
    | SEGMENT
    | SEQUENCE
    | SHARED
    | SNAPSHOT
    | SOME
    | SORT
    | SQL
    | SQLCODE
    | SQLERROR
    | SQLSTATE
    | STATEMENT_ID
    | STATEMENT
    | STATISTICS
    | STOP
    | STORAGE
    | SWITCH
    | SYSTEM
    | TABLES
    | TABLESPACE
    | TEMPORARY
    | THREAD
    | TIME
    | TRACING
    | TRANSACTION
    | TRIGGERS
    | TRUNCATE
    | UNDER
    | UNLIMITED
    | UNTIL
    | USE
    | USING
    | WHEN
    | WORK
    | WRITE
    ;

unreserved_keyword_normal
    : ACCESS_INFO
    | ACCOUNT
    | ABSENT
    | ACCESSIBLE
    | ACCESSID
    | ACCESSKEY
    | ACCESSTYPE
    | ACTION
    | ACTIVE
    | ACTIVATE
    | ADDDATE
    | ADMINISTER
    | AGGREGATE
    | AGAINST
    | ALGORITHM
    | ALLOW
    | ALWAYS
    | ANALYSE
    | ARRAY
    | ASCII
    | ASENSITIVE
    | ASIS
    | ASYNCHRONOUS
    | AT
    | ATTRIBUTE
    | AUTHORS
    | AUTO
    | AUTOEXTEND_SIZE
    | AUTO_REFRESH
    | AVG_ROW_LENGTH
    | BACKUP_COPIES
    | BADFILE
    | BASE
    | BASELINE
    | BASELINE_ID
    | BASIC
    | BALANCE
    | BINARY
    | BINARY_DOUBLE
    | BINARY_DOUBLE_INFINITY
    | BINARY_DOUBLE_NAN
    | BINARY_FLOAT
    | BINARY_FLOAT_INFINITY
    | BINARY_FLOAT_NAN
    | BINDING
    | SHARDING
    | BINLOG
    | BIT
    | BLOB
    | BLOCK_SIZE
    | BLOCK_INDEX
    | BLOOM_FILTER
    | BOOL
    | BOOLEAN
    | BOOTSTRAP
    | BOTH
    | BTREE
    | BUFFER_SIZE
    | BULK
    | BULK_EXCEPTIONS
    | BULK_ROWCOUNT
    | BYTE
    | BREADTH
    | CALC_PARTITION_ID
    | CALL
    | COMPLETE
    | CASCADED
    | CAST
    | CATALOG_NAME
    | CONTENT
    | CHAIN
    | CHANGED
    | CHARSET
    | CHAR_CS
    | CHECKSUM
    | CIPHER
    | CLASS_ORIGIN
    | CLEAN
    | CLEAR
    | CLIENT
    | CLOB
    | CLOG
    | CLUSTER_ID
    | CLUSTER_NAME
    | COALESCE
    | CODE
    | COLLATE
    | COLLATION
    | COLLECT
    | COLUMN_FORMAT
    | COLUMN_NAME
    | COLUMN_OUTER_JOIN_SYMBOL
    | COLUMN_STAT
    | COLUMNS
    | COMMITTED
    | COMPACT
    | COMPLETION
    | COMPRESSED
    | COMPRESSION
    | COMPRESSION_CODE
    | COMPUTATION
    | COMPUTE
    | CONCURRENT
    | CONDITIONAL
    | CONNECTION
    | CONNECT_BY_ISCYCLE
    | CONNECT_BY_ISLEAF
    | CONSISTENT
    | CONSISTENT_MODE
    | CONSTRAINT_CATALOG
    | CONSTRAINT_NAME
    | CONSTRAINT_SCHEMA
    | CONTAINS
    | CONTEXT
    | CONTRIBUTORS
    | COPY
    | CPU
    | CREATE_TIMESTAMP
    | CROSS
    | CUBE
    | CURRENT_USER
    | CURRENT_SCHEMA
    | CURRENT_DATE
    | CURRENT_TIMESTAMP
    | DATA
    | DATABASES
    | DATABASE_ID
    | DATA_TABLE_ID
    | DATA_SOURCE
    | DATE_ADD
    | DATE_SUB
    | DATETIME
    | DAY
    | DAY_HOUR
    | DAY_MICROSECOND
    | DAY_MINUTE
    | DAY_SECOND
    | DBA_RECYCLEBIN
    | DBTIMEZONE
    | DEALLOCATE
    | DEFAULTS
    | DEFAULT_AUTH
    | DEFINER
    | DELAY
    | DELAYED
    | DELAY_KEY_WRITE
    | DELETEXML
    | DELETING
    | DEPTH
    | DEMAND
    | DESCRIPTION
    | DES_KEY_FILE
    | DESCRIBE
    | DESTINATION
    | DETERMINISTIC
    | DIAGNOSTICS
    | DICTIONARY
    | DIRECTORY
    | DISALLOW
    | DISCARD
    | DISK
    | DML
    | DDL
    | DISTINCTROW
    | DIV
    | DO
    | DOT
    | DOCUMENT
    | DUMPFILE
    | DUPLICATE
    | DUPLICATE_SCOPE
    | DYNAMIC
    | DEFAULT_TABLEGROUP
    | DEBUG
    | E_SIZE
    | EFFECTIVE
    | ELSEIF
    | ENCLOSED
    | ENCODING
    | ENCRYPTED
    | ENCRYPTION
    | ENDS
    | ENDPOINT
    | ENGINE_
    | ENGINES
    | ENUM
    | ERROR_CODE
    | ERROR_P
    | ERROR_INDEX
    | ERRORS
    | ESCAPED
    | ESTIMATE
    | EVENT
    | EVERY
    | EXCHANGE
    | EXCLUDE
    | EXCLUDING
    | EXEMPT
    | EXIT
    | EXPANSION
    | EXPIRE
    | EXPIRE_INFO
    | EXPORT
    | EXTENDED
    | EXTENDED_NOADDR
    | ENABLE_EXTENDED_ROWID
    | EXTENT_SIZE
    | EXTERNAL_STORAGE_DEST
    | EXTRA
    | EXTRACT
    | EVALNAME
    | ENTITYESCAPING
    | EXTRACTVALUE
    | FAILED_LOGIN_ATTEMPTS
    | FAST
    | FAULTS
    | FIELDS
    | FIELD_DELIMITER
    | FIELD_OPTIONALLY_ENCLOSED_BY
    | SKIP_HEADER
    | SKIP_BLANK_LINES
    | TRIM_SPACE
    | NULL_IF_EXETERNAL
    | NEVER
    | EMPTY_FIELD_AS_NULL
    | FILE_ID
    | FILEX
    | FINAL_COUNT
    | FIRST
    | FIXED
    | FLASHBACK
    | FLOAT4
    | FLOAT8
    | FOLLOWER
    | FOLLOWING
    | FORMAT
    | FREEZE
    | FREQUENCY
    | FROZEN
    | FULL
    | G_SIZE
    | GENERAL
    | GENERATED
    | GEOMETRY
    | GEOMETRYCOLLECTION
    | GET
    | GET_FORMAT
    | GLOBAL
    | GLOBAL_ALIAS
    | GRANTS
    | GROUP_ID
    | GROUPING
    | GROUPING_ID
    | GTS
    | HANDLER
    | HASH
    | HELP
    | HIDE
    | HIGH
    | HIGH_PRIORITY
    | HOUR_MICROSECOND
    | HOUR_MINUTE
    | HOUR_SECOND
    | HOST
    | HOSTS
    | HOUR
    | ID
    | IDC
    | IDENTITY
    | IF
    | IFIGNORE
    | IGNORE
    | IGNORE_SERVER_IDS
    | ILOG
    | ILOGCACHE
    | IMPORT
    | INDENT
    | INDEXES
    | INDEXTYPE
    | INDEX_TABLE_ID
    | INCR
    | INCLUDE
    | INCREMENTAL
    | INFO
    | INFILE
    | INFINITE_VALUE
    | INITIAL_SIZE
    | INNER
    | INNER_PARSE
    | INOUT
    | INSENSITIVE
    | INSERTCHILDXML
    | INSERTING
    | INSERT_METHOD
    | INSTALL
    | INT1
    | INT2
    | INT3
    | INT4
    | INT8
    | INTERVAL
    | INVOKER
    | IO
    | IOPS_WEIGHT
    | IO_AFTER_GTIDS
    | IO_BEFORE_GTIDS
    | IO_THREAD
    | IPC
    | ISNULL
    | ISOLATION
    | ISSUER
    | ITERATE
    | JOB
    | JOIN
    | JSON
    | JSON_ARRAY
    | JSON_EMPTY
    | JSON_EQUAL
    | JSON_TABLE
    | JSON_VALUE
    | JSON_QUERY
    | JSON_EXISTS
    | JSON_MERGEPATCH
    | JSON_ARRAYAGG
    | JSON_OBJECTAGG
    | JSON_OBJECT
    | K_SIZE
    | KEY_BLOCK_SIZE
    | KEYS
    | KEYSTORE
    | KEY_VERSION
    | KILL
    | KEEP
    | KVCACHE
    | LAST
    | LAST_REFRESH_SCN
    | LAX
    | LEADER
    | LEADING
    | LEAVE
    | LEAVES
    | LEFT
    | LESS
    | LIMIT
    | LINEAR
    | LINES
    | LINESTRING
    | LINE_DELIMITER
    | LIST
    | LNNVL
    | LOAD
    | LOCAL
    | LOCALITY
    | LOCALTIMESTAMP
    | LOCK_
    | LOCKED
    | LOCKS
    | LOGONLY_REPLICA_NUM
    | LOG
    | LOGS
    | LONGBLOB
    | LONGTEXT
    | LOOP
    | LOW
    | LOW_PRIORITY
    | LS
    | ISOPEN
    | ISOLATION_LEVEL
    | M_SIZE
    | MAJOR
    | MANAGEMENT
    | MASTER
    | MASTER_AUTO_POSITION
    | MASTER_BIND
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
    | MASTER_SSL_VERIFY_SERVER_CERT
    | MASTER_USER
    | MATCH
    | MATCHED
    | MAX_CONNECTIONS_PER_HOUR
    | MAX_CPU
    | MAX_FILE_SIZE
    | LOG_DISK_SIZE
    | MAX_IOPS
    | MEMORY_SIZE
    | MAX_QUERIES_PER_HOUR
    | MAX_ROWS
    | MAX_SESSION_NUM
    | MAX_SIZE
    | MAX_UPDATES_PER_HOUR
    | MAX_USED_PART_ID
    | MAX_USER_CONNECTIONS
    | MDSYS
    | MEDIUM
    | MEDIUMBLOB
    | MEDIUMINT
    | MEDIUMTEXT
    | MEMORY
    | MEMSTORE_PERCENT
    | MEMTABLE
    | MERGE
    | MESSAGE_TEXT
    | META
    | MICROSECOND
    | MIDDLEINT
    | MIGRATE
    | MIGRATION
    | MIN_CPU
    | MIN_IOPS
    | MIN_MAX
    | MIN_MEMORY
    | MINOR
    | MIN_ROWS
    | MINUTE
    | MINUTE_MICROSECOND
    | MINUTE_SECOND
    | MISMATCH
    | MISSING
    | MOD
    | MODIFIES
    | MONTH
    | MOVE
    | MOVEMENT
    | MULTILINESTRING
    | MULTIPOINT
    | MULTIPOLYGON
    | MULTIVALUE
    | MULTISET
    | MUTEX
    | MYSQL_ERRNO
    | MY_NAME
    | NAME
    | NAMES
    | NAN_VALUE
    | NATIONAL
    | NATURAL
    | NCHAR
    | NCHAR_CS
    | NDB
    | NDBCLUSTER
    | NESTED
    | NO
    | NOENTITYESCAPING
    | NODEGROUP
    | NOLOGGING
    | NOSCHEMACHECK
    | NOW
    | NOWAIT
    | NO_WAIT
    | NO_WRITE_TO_BINLOG
    | NULLS
    | NVARCHAR
    | NVARCHAR2
    | OBJECT
    | OCCUR
    | ORDINALITY
    | OFFSET
    | OLD_PASSWORD
    | OLD_KEY
    | OLTP
    | OVER
    | ONE
    | ONE_SHOT
    | OPTIONS
    | OPTIMIZE
    | OPTIONALLY
    | ORA_ROWSCN
    | ORIG_DEFAULT
    | OUT
    | OUTER
    | OUTFILE
    | OUTLINE
    | OWNER
    | P_SIZE
    | PACK_KEYS
    | PAGE
    | PARAMETERS
    | PARAM_ASSIGN_OPERATOR
    | PARSER
    | PARTIAL
    | PARTITION
    | PARTITION_ID
    | PARTITIONING
    | PARTITIONS
    | PARTITION_TYPE
    | PASSING
    | PASSWORD
    | PASSWORD_GRACE_TIME
    | PASSWORD_LIFE_TIME
    | PASSWORD_LOCK_TIME
    | PASSWORD_VERIFY_FUNCTION
    | PATH
    | PATTERN
    | PAUSE
    | PAXOS_REPLICA_NUM
    | PERCENTAGE
    | PHASE
    | PLANREGRESS
    | PLUGIN
    | PLUGIN_DIR
    | PLUGINS
    | PLUS
    | PIVOT
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
    | PRIVILEGE
    | PROCESS
    | PROCESSLIST
    | PROFILES
    | PROGRESSIVE_MERGE_NUM
    | PROJECT_NAME
    | PROPERTIES
    | PROXY
    | PURGE
    | QUARTER
    | QUERY
    | QUICK
    | RANGE
    | READ_WRITE
    | READS
    | READ_ONLY
    | REBUILD
    | RECOVERY_WINDOW
    | RECURSIVE
    | RECYCLE
    | RECYCLEBIN
    | REDACTION
    | REDO_BUFFER_SIZE
    | REDOFILE
    | REDUNDANCY
    | REDUNDANT
    | REF
    | REFRESH
    | REGEXP_LIKE
    | REGION
    | REJECT
    | RELAY
    | RELAYLOG
    | RELAY_LOG_FILE
    | RELAY_LOG_POS
    | RELAY_THREAD
    | RELEASE
    | RELOAD
    | REMOVE
    | REORGANIZE
    | REPAIR
    | REPEAT
    | REPEATABLE
    | REPLACE
    | REPLICA
    | REPLICA_NUM
    | REPLICA_TYPE
    | REPLICATION
    | REPORT
    | REQUIRE
    | RESET
    | RESIGNAL
    | RESOURCE_POOL_LIST
    | RESPECT
    | RESTART
    | RESTORE
    | RESTRICT
    | RESUME
    | RETURN
    | RETURNED_SQLSTATE
    | RETURNING
    | RETURNS
    | REVERSE
    | REWRITE
    | REWRITE_MERGE_VERSION
    | REMOTE_OSS
    | RLIKE
    | RIGHT
    | ROLLUP
    | ROOT
    | ROOTTABLE
    | ROOTSERVICE
    | ROOTSERVICE_LIST
    | ROUTINE
    | ROWCOUNT
    | ROW_COUNT
    | ROW_FORMAT
    | RTREE
    | RUN
    | SAMPLE
    | SCALARS
    | SCHEDULE
    | SCHEMACHECK
    | SCHEMAS
    | SCHEMA_NAME
    | SCOPE
    | SDO_GEOMETRY
    | SDO_RELATE
    | SEARCH
    | SECOND
    | SECOND_MICROSECOND
    | SECURITY
    | SEED
    | SENSITIVE
    | SEPARATOR
    | SERIAL
    | SERIALIZABLE
    | SERVER
    | SERVER_IP
    | SERVER_PORT
    | SERVER_TYPE
    | SESSION_ALIAS
    | SESSION_USER
    | SESSIONTIMEZONE
    | SET_MASTER_CLUSTER
    | SET_SLAVE_CLUSTER
    | SET_TP
    | SETS
    | SHRINK
    | SHOW
    | SHUTDOWN
    | SIBLINGS
    | SIGNAL
    | SIGNED
    | SIMPLE
    | SINGLE
    | R_SKIP
    | SKIP_INDEX
    | SLAVE
    | SLOW
    | SOCKET
    | SONAME
    | SORTKEY
    | SOUNDS
    | SOURCE
    | SPACE
    | SPATIAL
    | SPATIAL_CELLID
    | SPATIAL_INDEX
    | SPATIAL_MBR
    | SPECIFIC
    | SPFILE
    | SPLIT
    | SQLEXCEPTION
    | SQLWARNING
    | SQL_BIG_RESULT
    | SQL_CALC_FOUND_ROW
    | SQL_SMALL_RESULT
    | SQL_AFTER_GTIDS
    | SQL_AFTER_MTS_GAPS
    | SQL_BEFORE_GTIDS
    | SQL_BUFFER_RESULT
    | SQL_CACHE
    | SQL_ID
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
    | SSL
    | SS_MICRO_CACHE
    | STRAIGHT_JOIN
    | STARTING
    | STARTS
    | STATS_AUTO_RECALC
    | STATS_PERSISTENT
    | STATS_SAMPLE_PAGES
    | STATUS
    | STATEMENTS
    | STORAGE_FORMAT_VERSION
    | STORAGE_FORMAT_WORK_VERSION
    | STORED
    | STORING
    | STRICT
    | STRONG
    | STANDBY
    | STSTOKEN
    | SUBCLASS_ORIGIN
    | SUBDATE
    | SUBJECT
    | SUBPARTITION
    | SUBPARTITIONS
    | SUBSTR
    | SUPER
    | SUSPEND
    | SWAPS
    | SWITCHES
    | SWITCHOVER
    | SYNCHRONOUS
    | SYSTEM_USER
    | SYSTIMESTAMP
    | SYSBACKUP
    | SYSDBA
    | SYSKM
    | SYSOPER
    | SYS_CONNECT_BY_PATH
    | T_SIZE
    | TABLEGROUP
    | TABLE_CHECKSUM
    | TABLE_MODE
    | TABLEGROUPS
    | TABLEGROUP_ID
    | TABLE_ID
    | TABLE_NAME
    | TABLET
    | TABLET_ID
    | TABLET_SIZE
    | TABLET_MAX_SIZE
    | TASK
    | TASK_ID
    | TEMPLATE
    | TEMPTABLE
    | TENANT
    | TERMINATED
    | TEXT
    | THAN
    | THROUGH
    | TIMESTAMP
    | TIMESTAMPADD
    | TIMESTAMPDIFF
    | TIMEZONE_ABBR
    | TIMEZONE_HOUR
    | TIMEZONE_MINUTE
    | TIMEZONE_REGION
    | TIME_ZONE_INFO
    | TINYBLOB
    | TINYTEXT
    | TP_NAME
    | TP_NO
    | TRACE
    | TRADITIONAL
    | TRAILING
    | TREAT
    | TRIM
    | TRANSLATE
    | TYPE
    | TYPENAME
    | TYPES
    | UNCOMMITTED
    | UNCONDITIONAL
    | UNDEFINED
    | UNDO
    | UNDO_BUFFER_SIZE
    | UNDOFILE
    | UNICODE
    | UNKNOWN
    | UNINSTALL
    | UNIT
    | UNIT_GROUP
    | UNIT_NUM
    | UNLOCK
    | UNLOCKED
    | UNUSUAL
    | UNPIVOT
    | UPDATING
    | UPDATEXML
    | UPGRADE
    | UROWID
    | USAGE
    | USER_SPECIFIED
    | USE_BLOOM_FILTER
    | USE_FRM
    | USER_RESOURCES
    | UTC_DATE
    | UTC_TIMESTAMP
    | UNBOUNDED
    | VALID
    | VARIABLES
    | VALUE
    | VERBOSE
    | VERSION
    | MATERIALIZED
    | WAIT
    | WARNINGS
    | WEEK
    | WEIGHT_STRING
    | WITHOUT
    | WMSYS
    | WRAPPER
    | X509
    | XA
    | XML
    | YEAR
    | ZONE
    | ZONE_LIST
    | ZONE_TYPE
    | LOCATION
    | VARYING
    | VIRTUAL
    | VISIBLE
    | VERIFY
    | INVISIBLE
    | RELY
    | NORELY
    | NOVALIDATE
    | WITHIN
    | WEAK
    | WELLFORMED
    | WHILE
    | XMLAGG
    | XMLPARSE
    | XOR
    | XMLELEMENT
    | XMLATTRIBUTES
    | XMLNAMESPACES
    | XMLSEQUENCE
    | XMLSERIALIZE
    | XMLTABLE
    | XMLTYPE
    | XMLCAST
    | YEAR_MONTH
    | ZEROFILL
    | PERCENT
    | TIES
    | MEMBER
    | SUBMULTISET
    | EMPTY
    | A_
    | THROTTLE
    | PRIORITY
    | RT
    | NETWORK
    | LOGICAL_READS
    | QUEUE_TIME
    | QUOTA_NAME
    | HIDDEN_
    | INDEXED
    | SKEWONLY
    | NAMESPACE
    | LIB
    | OBJECT_ID
    | TRANSFER
    | SUM_OPNSIZE
    | VALIDATION
    | OVERWRITE
    | SERVICE
    | MICRO_INDEX_CLUSTERED
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


