// Generated from OBParser.g4 by ANTLR 4.9.1
import org.antlr.v4.runtime.tree.ParseTreeListener;

/**
 * This interface defines a complete listener for a parse tree produced by
 * {@link OBParser}.
 */
public interface OBParserListener extends ParseTreeListener {
	/**
	 * Enter a parse tree produced by {@link OBParser#sql_stmt}.
	 * @param ctx the parse tree
	 */
	void enterSql_stmt(OBParser.Sql_stmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#sql_stmt}.
	 * @param ctx the parse tree
	 */
	void exitSql_stmt(OBParser.Sql_stmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#stmt_list}.
	 * @param ctx the parse tree
	 */
	void enterStmt_list(OBParser.Stmt_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#stmt_list}.
	 * @param ctx the parse tree
	 */
	void exitStmt_list(OBParser.Stmt_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#stmt}.
	 * @param ctx the parse tree
	 */
	void enterStmt(OBParser.StmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#stmt}.
	 * @param ctx the parse tree
	 */
	void exitStmt(OBParser.StmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#drop_package_stmt}.
	 * @param ctx the parse tree
	 */
	void enterDrop_package_stmt(OBParser.Drop_package_stmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#drop_package_stmt}.
	 * @param ctx the parse tree
	 */
	void exitDrop_package_stmt(OBParser.Drop_package_stmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#drop_procedure_stmt}.
	 * @param ctx the parse tree
	 */
	void enterDrop_procedure_stmt(OBParser.Drop_procedure_stmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#drop_procedure_stmt}.
	 * @param ctx the parse tree
	 */
	void exitDrop_procedure_stmt(OBParser.Drop_procedure_stmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#drop_function_stmt}.
	 * @param ctx the parse tree
	 */
	void enterDrop_function_stmt(OBParser.Drop_function_stmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#drop_function_stmt}.
	 * @param ctx the parse tree
	 */
	void exitDrop_function_stmt(OBParser.Drop_function_stmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#drop_trigger_stmt}.
	 * @param ctx the parse tree
	 */
	void enterDrop_trigger_stmt(OBParser.Drop_trigger_stmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#drop_trigger_stmt}.
	 * @param ctx the parse tree
	 */
	void exitDrop_trigger_stmt(OBParser.Drop_trigger_stmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#drop_type_stmt}.
	 * @param ctx the parse tree
	 */
	void enterDrop_type_stmt(OBParser.Drop_type_stmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#drop_type_stmt}.
	 * @param ctx the parse tree
	 */
	void exitDrop_type_stmt(OBParser.Drop_type_stmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#pl_expr_stmt}.
	 * @param ctx the parse tree
	 */
	void enterPl_expr_stmt(OBParser.Pl_expr_stmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#pl_expr_stmt}.
	 * @param ctx the parse tree
	 */
	void exitPl_expr_stmt(OBParser.Pl_expr_stmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#expr_list}.
	 * @param ctx the parse tree
	 */
	void enterExpr_list(OBParser.Expr_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#expr_list}.
	 * @param ctx the parse tree
	 */
	void exitExpr_list(OBParser.Expr_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#column_ref}.
	 * @param ctx the parse tree
	 */
	void enterColumn_ref(OBParser.Column_refContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#column_ref}.
	 * @param ctx the parse tree
	 */
	void exitColumn_ref(OBParser.Column_refContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#oracle_pl_non_reserved_words}.
	 * @param ctx the parse tree
	 */
	void enterOracle_pl_non_reserved_words(OBParser.Oracle_pl_non_reserved_wordsContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#oracle_pl_non_reserved_words}.
	 * @param ctx the parse tree
	 */
	void exitOracle_pl_non_reserved_words(OBParser.Oracle_pl_non_reserved_wordsContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#complex_string_literal}.
	 * @param ctx the parse tree
	 */
	void enterComplex_string_literal(OBParser.Complex_string_literalContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#complex_string_literal}.
	 * @param ctx the parse tree
	 */
	void exitComplex_string_literal(OBParser.Complex_string_literalContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#js_literal}.
	 * @param ctx the parse tree
	 */
	void enterJs_literal(OBParser.Js_literalContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#js_literal}.
	 * @param ctx the parse tree
	 */
	void exitJs_literal(OBParser.Js_literalContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#literal}.
	 * @param ctx the parse tree
	 */
	void enterLiteral(OBParser.LiteralContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#literal}.
	 * @param ctx the parse tree
	 */
	void exitLiteral(OBParser.LiteralContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#number_literal}.
	 * @param ctx the parse tree
	 */
	void enterNumber_literal(OBParser.Number_literalContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#number_literal}.
	 * @param ctx the parse tree
	 */
	void exitNumber_literal(OBParser.Number_literalContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#expr_const}.
	 * @param ctx the parse tree
	 */
	void enterExpr_const(OBParser.Expr_constContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#expr_const}.
	 * @param ctx the parse tree
	 */
	void exitExpr_const(OBParser.Expr_constContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#conf_const}.
	 * @param ctx the parse tree
	 */
	void enterConf_const(OBParser.Conf_constContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#conf_const}.
	 * @param ctx the parse tree
	 */
	void exitConf_const(OBParser.Conf_constContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#bool_pri}.
	 * @param ctx the parse tree
	 */
	void enterBool_pri(OBParser.Bool_priContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#bool_pri}.
	 * @param ctx the parse tree
	 */
	void exitBool_pri(OBParser.Bool_priContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#is_json_constrain}.
	 * @param ctx the parse tree
	 */
	void enterIs_json_constrain(OBParser.Is_json_constrainContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#is_json_constrain}.
	 * @param ctx the parse tree
	 */
	void exitIs_json_constrain(OBParser.Is_json_constrainContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#strict_opt}.
	 * @param ctx the parse tree
	 */
	void enterStrict_opt(OBParser.Strict_optContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#strict_opt}.
	 * @param ctx the parse tree
	 */
	void exitStrict_opt(OBParser.Strict_optContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#scalars_opt}.
	 * @param ctx the parse tree
	 */
	void enterScalars_opt(OBParser.Scalars_optContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#scalars_opt}.
	 * @param ctx the parse tree
	 */
	void exitScalars_opt(OBParser.Scalars_optContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#unique_keys_opt}.
	 * @param ctx the parse tree
	 */
	void enterUnique_keys_opt(OBParser.Unique_keys_optContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#unique_keys_opt}.
	 * @param ctx the parse tree
	 */
	void exitUnique_keys_opt(OBParser.Unique_keys_optContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#json_equal_option}.
	 * @param ctx the parse tree
	 */
	void enterJson_equal_option(OBParser.Json_equal_optionContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#json_equal_option}.
	 * @param ctx the parse tree
	 */
	void exitJson_equal_option(OBParser.Json_equal_optionContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#predicate}.
	 * @param ctx the parse tree
	 */
	void enterPredicate(OBParser.PredicateContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#predicate}.
	 * @param ctx the parse tree
	 */
	void exitPredicate(OBParser.PredicateContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#collection_predicate_expr}.
	 * @param ctx the parse tree
	 */
	void enterCollection_predicate_expr(OBParser.Collection_predicate_exprContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#collection_predicate_expr}.
	 * @param ctx the parse tree
	 */
	void exitCollection_predicate_expr(OBParser.Collection_predicate_exprContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#bit_expr}.
	 * @param ctx the parse tree
	 */
	void enterBit_expr(OBParser.Bit_exprContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#bit_expr}.
	 * @param ctx the parse tree
	 */
	void exitBit_expr(OBParser.Bit_exprContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#conf_expr}.
	 * @param ctx the parse tree
	 */
	void enterConf_expr(OBParser.Conf_exprContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#conf_expr}.
	 * @param ctx the parse tree
	 */
	void exitConf_expr(OBParser.Conf_exprContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#is_nan_inf_value}.
	 * @param ctx the parse tree
	 */
	void enterIs_nan_inf_value(OBParser.Is_nan_inf_valueContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#is_nan_inf_value}.
	 * @param ctx the parse tree
	 */
	void exitIs_nan_inf_value(OBParser.Is_nan_inf_valueContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#unary_expr}.
	 * @param ctx the parse tree
	 */
	void enterUnary_expr(OBParser.Unary_exprContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#unary_expr}.
	 * @param ctx the parse tree
	 */
	void exitUnary_expr(OBParser.Unary_exprContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#simple_expr}.
	 * @param ctx the parse tree
	 */
	void enterSimple_expr(OBParser.Simple_exprContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#simple_expr}.
	 * @param ctx the parse tree
	 */
	void exitSimple_expr(OBParser.Simple_exprContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#json_function}.
	 * @param ctx the parse tree
	 */
	void enterJson_function(OBParser.Json_functionContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#json_function}.
	 * @param ctx the parse tree
	 */
	void exitJson_function(OBParser.Json_functionContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#gis_function}.
	 * @param ctx the parse tree
	 */
	void enterGis_function(OBParser.Gis_functionContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#gis_function}.
	 * @param ctx the parse tree
	 */
	void exitGis_function(OBParser.Gis_functionContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#spatial_cellid_expr}.
	 * @param ctx the parse tree
	 */
	void enterSpatial_cellid_expr(OBParser.Spatial_cellid_exprContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#spatial_cellid_expr}.
	 * @param ctx the parse tree
	 */
	void exitSpatial_cellid_expr(OBParser.Spatial_cellid_exprContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#spatial_mbr_expr}.
	 * @param ctx the parse tree
	 */
	void enterSpatial_mbr_expr(OBParser.Spatial_mbr_exprContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#spatial_mbr_expr}.
	 * @param ctx the parse tree
	 */
	void exitSpatial_mbr_expr(OBParser.Spatial_mbr_exprContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#sdo_relate_expr}.
	 * @param ctx the parse tree
	 */
	void enterSdo_relate_expr(OBParser.Sdo_relate_exprContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#sdo_relate_expr}.
	 * @param ctx the parse tree
	 */
	void exitSdo_relate_expr(OBParser.Sdo_relate_exprContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#common_cursor_attribute}.
	 * @param ctx the parse tree
	 */
	void enterCommon_cursor_attribute(OBParser.Common_cursor_attributeContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#common_cursor_attribute}.
	 * @param ctx the parse tree
	 */
	void exitCommon_cursor_attribute(OBParser.Common_cursor_attributeContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#cursor_attribute_bulk_rowcount}.
	 * @param ctx the parse tree
	 */
	void enterCursor_attribute_bulk_rowcount(OBParser.Cursor_attribute_bulk_rowcountContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#cursor_attribute_bulk_rowcount}.
	 * @param ctx the parse tree
	 */
	void exitCursor_attribute_bulk_rowcount(OBParser.Cursor_attribute_bulk_rowcountContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#cursor_attribute_bulk_exceptions}.
	 * @param ctx the parse tree
	 */
	void enterCursor_attribute_bulk_exceptions(OBParser.Cursor_attribute_bulk_exceptionsContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#cursor_attribute_bulk_exceptions}.
	 * @param ctx the parse tree
	 */
	void exitCursor_attribute_bulk_exceptions(OBParser.Cursor_attribute_bulk_exceptionsContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#implicit_cursor_attribute}.
	 * @param ctx the parse tree
	 */
	void enterImplicit_cursor_attribute(OBParser.Implicit_cursor_attributeContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#implicit_cursor_attribute}.
	 * @param ctx the parse tree
	 */
	void exitImplicit_cursor_attribute(OBParser.Implicit_cursor_attributeContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#explicit_cursor_attribute}.
	 * @param ctx the parse tree
	 */
	void enterExplicit_cursor_attribute(OBParser.Explicit_cursor_attributeContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#explicit_cursor_attribute}.
	 * @param ctx the parse tree
	 */
	void exitExplicit_cursor_attribute(OBParser.Explicit_cursor_attributeContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#cursor_attribute_expr}.
	 * @param ctx the parse tree
	 */
	void enterCursor_attribute_expr(OBParser.Cursor_attribute_exprContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#cursor_attribute_expr}.
	 * @param ctx the parse tree
	 */
	void exitCursor_attribute_expr(OBParser.Cursor_attribute_exprContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#obj_access_ref}.
	 * @param ctx the parse tree
	 */
	void enterObj_access_ref(OBParser.Obj_access_refContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#obj_access_ref}.
	 * @param ctx the parse tree
	 */
	void exitObj_access_ref(OBParser.Obj_access_refContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#dot_notation_path}.
	 * @param ctx the parse tree
	 */
	void enterDot_notation_path(OBParser.Dot_notation_pathContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#dot_notation_path}.
	 * @param ctx the parse tree
	 */
	void exitDot_notation_path(OBParser.Dot_notation_pathContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#dot_notation_path_obj_access_ref}.
	 * @param ctx the parse tree
	 */
	void enterDot_notation_path_obj_access_ref(OBParser.Dot_notation_path_obj_access_refContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#dot_notation_path_obj_access_ref}.
	 * @param ctx the parse tree
	 */
	void exitDot_notation_path_obj_access_ref(OBParser.Dot_notation_path_obj_access_refContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#path_param_array}.
	 * @param ctx the parse tree
	 */
	void enterPath_param_array(OBParser.Path_param_arrayContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#path_param_array}.
	 * @param ctx the parse tree
	 */
	void exitPath_param_array(OBParser.Path_param_arrayContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#path_param_list}.
	 * @param ctx the parse tree
	 */
	void enterPath_param_list(OBParser.Path_param_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#path_param_list}.
	 * @param ctx the parse tree
	 */
	void exitPath_param_list(OBParser.Path_param_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#path_param}.
	 * @param ctx the parse tree
	 */
	void enterPath_param(OBParser.Path_paramContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#path_param}.
	 * @param ctx the parse tree
	 */
	void exitPath_param(OBParser.Path_paramContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#dot_notation_fun_sys}.
	 * @param ctx the parse tree
	 */
	void enterDot_notation_fun_sys(OBParser.Dot_notation_fun_sysContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#dot_notation_fun_sys}.
	 * @param ctx the parse tree
	 */
	void exitDot_notation_fun_sys(OBParser.Dot_notation_fun_sysContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#dot_notation_fun}.
	 * @param ctx the parse tree
	 */
	void enterDot_notation_fun(OBParser.Dot_notation_funContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#dot_notation_fun}.
	 * @param ctx the parse tree
	 */
	void exitDot_notation_fun(OBParser.Dot_notation_funContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#obj_access_ref_normal}.
	 * @param ctx the parse tree
	 */
	void enterObj_access_ref_normal(OBParser.Obj_access_ref_normalContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#obj_access_ref_normal}.
	 * @param ctx the parse tree
	 */
	void exitObj_access_ref_normal(OBParser.Obj_access_ref_normalContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#func_access_ref}.
	 * @param ctx the parse tree
	 */
	void enterFunc_access_ref(OBParser.Func_access_refContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#func_access_ref}.
	 * @param ctx the parse tree
	 */
	void exitFunc_access_ref(OBParser.Func_access_refContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#table_element_access_list}.
	 * @param ctx the parse tree
	 */
	void enterTable_element_access_list(OBParser.Table_element_access_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#table_element_access_list}.
	 * @param ctx the parse tree
	 */
	void exitTable_element_access_list(OBParser.Table_element_access_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#table_index}.
	 * @param ctx the parse tree
	 */
	void enterTable_index(OBParser.Table_indexContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#table_index}.
	 * @param ctx the parse tree
	 */
	void exitTable_index(OBParser.Table_indexContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#expr}.
	 * @param ctx the parse tree
	 */
	void enterExpr(OBParser.ExprContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#expr}.
	 * @param ctx the parse tree
	 */
	void exitExpr(OBParser.ExprContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#not}.
	 * @param ctx the parse tree
	 */
	void enterNot(OBParser.NotContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#not}.
	 * @param ctx the parse tree
	 */
	void exitNot(OBParser.NotContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#sub_query_flag}.
	 * @param ctx the parse tree
	 */
	void enterSub_query_flag(OBParser.Sub_query_flagContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#sub_query_flag}.
	 * @param ctx the parse tree
	 */
	void exitSub_query_flag(OBParser.Sub_query_flagContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#in_expr}.
	 * @param ctx the parse tree
	 */
	void enterIn_expr(OBParser.In_exprContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#in_expr}.
	 * @param ctx the parse tree
	 */
	void exitIn_expr(OBParser.In_exprContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#case_expr}.
	 * @param ctx the parse tree
	 */
	void enterCase_expr(OBParser.Case_exprContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#case_expr}.
	 * @param ctx the parse tree
	 */
	void exitCase_expr(OBParser.Case_exprContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#window_function}.
	 * @param ctx the parse tree
	 */
	void enterWindow_function(OBParser.Window_functionContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#window_function}.
	 * @param ctx the parse tree
	 */
	void exitWindow_function(OBParser.Window_functionContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#first_or_last}.
	 * @param ctx the parse tree
	 */
	void enterFirst_or_last(OBParser.First_or_lastContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#first_or_last}.
	 * @param ctx the parse tree
	 */
	void exitFirst_or_last(OBParser.First_or_lastContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#respect_or_ignore}.
	 * @param ctx the parse tree
	 */
	void enterRespect_or_ignore(OBParser.Respect_or_ignoreContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#respect_or_ignore}.
	 * @param ctx the parse tree
	 */
	void exitRespect_or_ignore(OBParser.Respect_or_ignoreContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#win_fun_first_last_params}.
	 * @param ctx the parse tree
	 */
	void enterWin_fun_first_last_params(OBParser.Win_fun_first_last_paramsContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#win_fun_first_last_params}.
	 * @param ctx the parse tree
	 */
	void exitWin_fun_first_last_params(OBParser.Win_fun_first_last_paramsContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#win_fun_lead_lag_params}.
	 * @param ctx the parse tree
	 */
	void enterWin_fun_lead_lag_params(OBParser.Win_fun_lead_lag_paramsContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#win_fun_lead_lag_params}.
	 * @param ctx the parse tree
	 */
	void exitWin_fun_lead_lag_params(OBParser.Win_fun_lead_lag_paramsContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#generalized_window_clause}.
	 * @param ctx the parse tree
	 */
	void enterGeneralized_window_clause(OBParser.Generalized_window_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#generalized_window_clause}.
	 * @param ctx the parse tree
	 */
	void exitGeneralized_window_clause(OBParser.Generalized_window_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#win_rows_or_range}.
	 * @param ctx the parse tree
	 */
	void enterWin_rows_or_range(OBParser.Win_rows_or_rangeContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#win_rows_or_range}.
	 * @param ctx the parse tree
	 */
	void exitWin_rows_or_range(OBParser.Win_rows_or_rangeContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#win_preceding_or_following}.
	 * @param ctx the parse tree
	 */
	void enterWin_preceding_or_following(OBParser.Win_preceding_or_followingContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#win_preceding_or_following}.
	 * @param ctx the parse tree
	 */
	void exitWin_preceding_or_following(OBParser.Win_preceding_or_followingContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#win_interval}.
	 * @param ctx the parse tree
	 */
	void enterWin_interval(OBParser.Win_intervalContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#win_interval}.
	 * @param ctx the parse tree
	 */
	void exitWin_interval(OBParser.Win_intervalContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#win_bounding}.
	 * @param ctx the parse tree
	 */
	void enterWin_bounding(OBParser.Win_boundingContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#win_bounding}.
	 * @param ctx the parse tree
	 */
	void exitWin_bounding(OBParser.Win_boundingContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#win_window}.
	 * @param ctx the parse tree
	 */
	void enterWin_window(OBParser.Win_windowContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#win_window}.
	 * @param ctx the parse tree
	 */
	void exitWin_window(OBParser.Win_windowContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#simple_when_clause_list}.
	 * @param ctx the parse tree
	 */
	void enterSimple_when_clause_list(OBParser.Simple_when_clause_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#simple_when_clause_list}.
	 * @param ctx the parse tree
	 */
	void exitSimple_when_clause_list(OBParser.Simple_when_clause_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#simple_when_clause}.
	 * @param ctx the parse tree
	 */
	void enterSimple_when_clause(OBParser.Simple_when_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#simple_when_clause}.
	 * @param ctx the parse tree
	 */
	void exitSimple_when_clause(OBParser.Simple_when_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#bool_when_clause_list}.
	 * @param ctx the parse tree
	 */
	void enterBool_when_clause_list(OBParser.Bool_when_clause_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#bool_when_clause_list}.
	 * @param ctx the parse tree
	 */
	void exitBool_when_clause_list(OBParser.Bool_when_clause_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#bool_when_clause}.
	 * @param ctx the parse tree
	 */
	void enterBool_when_clause(OBParser.Bool_when_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#bool_when_clause}.
	 * @param ctx the parse tree
	 */
	void exitBool_when_clause(OBParser.Bool_when_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#case_default}.
	 * @param ctx the parse tree
	 */
	void enterCase_default(OBParser.Case_defaultContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#case_default}.
	 * @param ctx the parse tree
	 */
	void exitCase_default(OBParser.Case_defaultContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#sql_function}.
	 * @param ctx the parse tree
	 */
	void enterSql_function(OBParser.Sql_functionContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#sql_function}.
	 * @param ctx the parse tree
	 */
	void exitSql_function(OBParser.Sql_functionContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#xml_function}.
	 * @param ctx the parse tree
	 */
	void enterXml_function(OBParser.Xml_functionContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#xml_function}.
	 * @param ctx the parse tree
	 */
	void exitXml_function(OBParser.Xml_functionContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#single_row_function}.
	 * @param ctx the parse tree
	 */
	void enterSingle_row_function(OBParser.Single_row_functionContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#single_row_function}.
	 * @param ctx the parse tree
	 */
	void exitSingle_row_function(OBParser.Single_row_functionContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#numeric_function}.
	 * @param ctx the parse tree
	 */
	void enterNumeric_function(OBParser.Numeric_functionContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#numeric_function}.
	 * @param ctx the parse tree
	 */
	void exitNumeric_function(OBParser.Numeric_functionContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#character_function}.
	 * @param ctx the parse tree
	 */
	void enterCharacter_function(OBParser.Character_functionContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#character_function}.
	 * @param ctx the parse tree
	 */
	void exitCharacter_function(OBParser.Character_functionContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#translate_charset}.
	 * @param ctx the parse tree
	 */
	void enterTranslate_charset(OBParser.Translate_charsetContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#translate_charset}.
	 * @param ctx the parse tree
	 */
	void exitTranslate_charset(OBParser.Translate_charsetContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#extract_function}.
	 * @param ctx the parse tree
	 */
	void enterExtract_function(OBParser.Extract_functionContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#extract_function}.
	 * @param ctx the parse tree
	 */
	void exitExtract_function(OBParser.Extract_functionContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#conversion_function}.
	 * @param ctx the parse tree
	 */
	void enterConversion_function(OBParser.Conversion_functionContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#conversion_function}.
	 * @param ctx the parse tree
	 */
	void exitConversion_function(OBParser.Conversion_functionContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#hierarchical_function}.
	 * @param ctx the parse tree
	 */
	void enterHierarchical_function(OBParser.Hierarchical_functionContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#hierarchical_function}.
	 * @param ctx the parse tree
	 */
	void exitHierarchical_function(OBParser.Hierarchical_functionContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#environment_id_function}.
	 * @param ctx the parse tree
	 */
	void enterEnvironment_id_function(OBParser.Environment_id_functionContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#environment_id_function}.
	 * @param ctx the parse tree
	 */
	void exitEnvironment_id_function(OBParser.Environment_id_functionContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#aggregate_function}.
	 * @param ctx the parse tree
	 */
	void enterAggregate_function(OBParser.Aggregate_functionContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#aggregate_function}.
	 * @param ctx the parse tree
	 */
	void exitAggregate_function(OBParser.Aggregate_functionContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#js_agg_on_null}.
	 * @param ctx the parse tree
	 */
	void enterJs_agg_on_null(OBParser.Js_agg_on_nullContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#js_agg_on_null}.
	 * @param ctx the parse tree
	 */
	void exitJs_agg_on_null(OBParser.Js_agg_on_nullContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#js_agg_returning_type_opt}.
	 * @param ctx the parse tree
	 */
	void enterJs_agg_returning_type_opt(OBParser.Js_agg_returning_type_optContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#js_agg_returning_type_opt}.
	 * @param ctx the parse tree
	 */
	void exitJs_agg_returning_type_opt(OBParser.Js_agg_returning_type_optContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#js_agg_returning_type}.
	 * @param ctx the parse tree
	 */
	void enterJs_agg_returning_type(OBParser.Js_agg_returning_typeContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#js_agg_returning_type}.
	 * @param ctx the parse tree
	 */
	void exitJs_agg_returning_type(OBParser.Js_agg_returning_typeContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#special_func_expr}.
	 * @param ctx the parse tree
	 */
	void enterSpecial_func_expr(OBParser.Special_func_exprContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#special_func_expr}.
	 * @param ctx the parse tree
	 */
	void exitSpecial_func_expr(OBParser.Special_func_exprContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#access_func_expr_count}.
	 * @param ctx the parse tree
	 */
	void enterAccess_func_expr_count(OBParser.Access_func_expr_countContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#access_func_expr_count}.
	 * @param ctx the parse tree
	 */
	void exitAccess_func_expr_count(OBParser.Access_func_expr_countContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#access_func_expr}.
	 * @param ctx the parse tree
	 */
	void enterAccess_func_expr(OBParser.Access_func_exprContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#access_func_expr}.
	 * @param ctx the parse tree
	 */
	void exitAccess_func_expr(OBParser.Access_func_exprContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#dblink_func_expr}.
	 * @param ctx the parse tree
	 */
	void enterDblink_func_expr(OBParser.Dblink_func_exprContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#dblink_func_expr}.
	 * @param ctx the parse tree
	 */
	void exitDblink_func_expr(OBParser.Dblink_func_exprContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#func_param_list}.
	 * @param ctx the parse tree
	 */
	void enterFunc_param_list(OBParser.Func_param_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#func_param_list}.
	 * @param ctx the parse tree
	 */
	void exitFunc_param_list(OBParser.Func_param_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#func_param}.
	 * @param ctx the parse tree
	 */
	void enterFunc_param(OBParser.Func_paramContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#func_param}.
	 * @param ctx the parse tree
	 */
	void exitFunc_param(OBParser.Func_paramContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#func_param_with_assign}.
	 * @param ctx the parse tree
	 */
	void enterFunc_param_with_assign(OBParser.Func_param_with_assignContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#func_param_with_assign}.
	 * @param ctx the parse tree
	 */
	void exitFunc_param_with_assign(OBParser.Func_param_with_assignContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#pl_var_name}.
	 * @param ctx the parse tree
	 */
	void enterPl_var_name(OBParser.Pl_var_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#pl_var_name}.
	 * @param ctx the parse tree
	 */
	void exitPl_var_name(OBParser.Pl_var_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#bool_pri_in_pl_func}.
	 * @param ctx the parse tree
	 */
	void enterBool_pri_in_pl_func(OBParser.Bool_pri_in_pl_funcContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#bool_pri_in_pl_func}.
	 * @param ctx the parse tree
	 */
	void exitBool_pri_in_pl_func(OBParser.Bool_pri_in_pl_funcContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#cur_timestamp_func}.
	 * @param ctx the parse tree
	 */
	void enterCur_timestamp_func(OBParser.Cur_timestamp_funcContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#cur_timestamp_func}.
	 * @param ctx the parse tree
	 */
	void exitCur_timestamp_func(OBParser.Cur_timestamp_funcContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#updating_func}.
	 * @param ctx the parse tree
	 */
	void enterUpdating_func(OBParser.Updating_funcContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#updating_func}.
	 * @param ctx the parse tree
	 */
	void exitUpdating_func(OBParser.Updating_funcContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#updating_params}.
	 * @param ctx the parse tree
	 */
	void enterUpdating_params(OBParser.Updating_paramsContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#updating_params}.
	 * @param ctx the parse tree
	 */
	void exitUpdating_params(OBParser.Updating_paramsContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#substr_params}.
	 * @param ctx the parse tree
	 */
	void enterSubstr_params(OBParser.Substr_paramsContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#substr_params}.
	 * @param ctx the parse tree
	 */
	void exitSubstr_params(OBParser.Substr_paramsContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#returning_log_error_clause}.
	 * @param ctx the parse tree
	 */
	void enterReturning_log_error_clause(OBParser.Returning_log_error_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#returning_log_error_clause}.
	 * @param ctx the parse tree
	 */
	void exitReturning_log_error_clause(OBParser.Returning_log_error_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#returning_clause}.
	 * @param ctx the parse tree
	 */
	void enterReturning_clause(OBParser.Returning_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#returning_clause}.
	 * @param ctx the parse tree
	 */
	void exitReturning_clause(OBParser.Returning_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#log_error_clause}.
	 * @param ctx the parse tree
	 */
	void enterLog_error_clause(OBParser.Log_error_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#log_error_clause}.
	 * @param ctx the parse tree
	 */
	void exitLog_error_clause(OBParser.Log_error_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#delete_stmt}.
	 * @param ctx the parse tree
	 */
	void enterDelete_stmt(OBParser.Delete_stmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#delete_stmt}.
	 * @param ctx the parse tree
	 */
	void exitDelete_stmt(OBParser.Delete_stmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#update_stmt}.
	 * @param ctx the parse tree
	 */
	void enterUpdate_stmt(OBParser.Update_stmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#update_stmt}.
	 * @param ctx the parse tree
	 */
	void exitUpdate_stmt(OBParser.Update_stmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#update_asgn_list}.
	 * @param ctx the parse tree
	 */
	void enterUpdate_asgn_list(OBParser.Update_asgn_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#update_asgn_list}.
	 * @param ctx the parse tree
	 */
	void exitUpdate_asgn_list(OBParser.Update_asgn_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#normal_asgn_list}.
	 * @param ctx the parse tree
	 */
	void enterNormal_asgn_list(OBParser.Normal_asgn_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#normal_asgn_list}.
	 * @param ctx the parse tree
	 */
	void exitNormal_asgn_list(OBParser.Normal_asgn_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#update_asgn_factor}.
	 * @param ctx the parse tree
	 */
	void enterUpdate_asgn_factor(OBParser.Update_asgn_factorContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#update_asgn_factor}.
	 * @param ctx the parse tree
	 */
	void exitUpdate_asgn_factor(OBParser.Update_asgn_factorContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#create_resource_stmt}.
	 * @param ctx the parse tree
	 */
	void enterCreate_resource_stmt(OBParser.Create_resource_stmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#create_resource_stmt}.
	 * @param ctx the parse tree
	 */
	void exitCreate_resource_stmt(OBParser.Create_resource_stmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#opt_resource_unit_option_list}.
	 * @param ctx the parse tree
	 */
	void enterOpt_resource_unit_option_list(OBParser.Opt_resource_unit_option_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#opt_resource_unit_option_list}.
	 * @param ctx the parse tree
	 */
	void exitOpt_resource_unit_option_list(OBParser.Opt_resource_unit_option_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#resource_unit_option}.
	 * @param ctx the parse tree
	 */
	void enterResource_unit_option(OBParser.Resource_unit_optionContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#resource_unit_option}.
	 * @param ctx the parse tree
	 */
	void exitResource_unit_option(OBParser.Resource_unit_optionContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#opt_create_resource_pool_option_list}.
	 * @param ctx the parse tree
	 */
	void enterOpt_create_resource_pool_option_list(OBParser.Opt_create_resource_pool_option_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#opt_create_resource_pool_option_list}.
	 * @param ctx the parse tree
	 */
	void exitOpt_create_resource_pool_option_list(OBParser.Opt_create_resource_pool_option_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#create_resource_pool_option}.
	 * @param ctx the parse tree
	 */
	void enterCreate_resource_pool_option(OBParser.Create_resource_pool_optionContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#create_resource_pool_option}.
	 * @param ctx the parse tree
	 */
	void exitCreate_resource_pool_option(OBParser.Create_resource_pool_optionContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#alter_resource_pool_option_list}.
	 * @param ctx the parse tree
	 */
	void enterAlter_resource_pool_option_list(OBParser.Alter_resource_pool_option_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#alter_resource_pool_option_list}.
	 * @param ctx the parse tree
	 */
	void exitAlter_resource_pool_option_list(OBParser.Alter_resource_pool_option_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#id_list}.
	 * @param ctx the parse tree
	 */
	void enterId_list(OBParser.Id_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#id_list}.
	 * @param ctx the parse tree
	 */
	void exitId_list(OBParser.Id_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#alter_resource_pool_option}.
	 * @param ctx the parse tree
	 */
	void enterAlter_resource_pool_option(OBParser.Alter_resource_pool_optionContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#alter_resource_pool_option}.
	 * @param ctx the parse tree
	 */
	void exitAlter_resource_pool_option(OBParser.Alter_resource_pool_optionContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#alter_resource_stmt}.
	 * @param ctx the parse tree
	 */
	void enterAlter_resource_stmt(OBParser.Alter_resource_stmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#alter_resource_stmt}.
	 * @param ctx the parse tree
	 */
	void exitAlter_resource_stmt(OBParser.Alter_resource_stmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#drop_resource_stmt}.
	 * @param ctx the parse tree
	 */
	void enterDrop_resource_stmt(OBParser.Drop_resource_stmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#drop_resource_stmt}.
	 * @param ctx the parse tree
	 */
	void exitDrop_resource_stmt(OBParser.Drop_resource_stmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#create_tenant_stmt}.
	 * @param ctx the parse tree
	 */
	void enterCreate_tenant_stmt(OBParser.Create_tenant_stmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#create_tenant_stmt}.
	 * @param ctx the parse tree
	 */
	void exitCreate_tenant_stmt(OBParser.Create_tenant_stmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#opt_tenant_option_list}.
	 * @param ctx the parse tree
	 */
	void enterOpt_tenant_option_list(OBParser.Opt_tenant_option_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#opt_tenant_option_list}.
	 * @param ctx the parse tree
	 */
	void exitOpt_tenant_option_list(OBParser.Opt_tenant_option_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#tenant_option}.
	 * @param ctx the parse tree
	 */
	void enterTenant_option(OBParser.Tenant_optionContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#tenant_option}.
	 * @param ctx the parse tree
	 */
	void exitTenant_option(OBParser.Tenant_optionContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#zone_list}.
	 * @param ctx the parse tree
	 */
	void enterZone_list(OBParser.Zone_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#zone_list}.
	 * @param ctx the parse tree
	 */
	void exitZone_list(OBParser.Zone_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#resource_pool_list}.
	 * @param ctx the parse tree
	 */
	void enterResource_pool_list(OBParser.Resource_pool_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#resource_pool_list}.
	 * @param ctx the parse tree
	 */
	void exitResource_pool_list(OBParser.Resource_pool_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#alter_tenant_stmt}.
	 * @param ctx the parse tree
	 */
	void enterAlter_tenant_stmt(OBParser.Alter_tenant_stmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#alter_tenant_stmt}.
	 * @param ctx the parse tree
	 */
	void exitAlter_tenant_stmt(OBParser.Alter_tenant_stmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#drop_tenant_stmt}.
	 * @param ctx the parse tree
	 */
	void enterDrop_tenant_stmt(OBParser.Drop_tenant_stmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#drop_tenant_stmt}.
	 * @param ctx the parse tree
	 */
	void exitDrop_tenant_stmt(OBParser.Drop_tenant_stmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#create_restore_point_stmt}.
	 * @param ctx the parse tree
	 */
	void enterCreate_restore_point_stmt(OBParser.Create_restore_point_stmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#create_restore_point_stmt}.
	 * @param ctx the parse tree
	 */
	void exitCreate_restore_point_stmt(OBParser.Create_restore_point_stmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#drop_restore_point_stmt}.
	 * @param ctx the parse tree
	 */
	void enterDrop_restore_point_stmt(OBParser.Drop_restore_point_stmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#drop_restore_point_stmt}.
	 * @param ctx the parse tree
	 */
	void exitDrop_restore_point_stmt(OBParser.Drop_restore_point_stmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#database_key}.
	 * @param ctx the parse tree
	 */
	void enterDatabase_key(OBParser.Database_keyContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#database_key}.
	 * @param ctx the parse tree
	 */
	void exitDatabase_key(OBParser.Database_keyContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#database_factor}.
	 * @param ctx the parse tree
	 */
	void enterDatabase_factor(OBParser.Database_factorContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#database_factor}.
	 * @param ctx the parse tree
	 */
	void exitDatabase_factor(OBParser.Database_factorContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#database_option_list}.
	 * @param ctx the parse tree
	 */
	void enterDatabase_option_list(OBParser.Database_option_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#database_option_list}.
	 * @param ctx the parse tree
	 */
	void exitDatabase_option_list(OBParser.Database_option_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#charset_key}.
	 * @param ctx the parse tree
	 */
	void enterCharset_key(OBParser.Charset_keyContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#charset_key}.
	 * @param ctx the parse tree
	 */
	void exitCharset_key(OBParser.Charset_keyContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#database_option}.
	 * @param ctx the parse tree
	 */
	void enterDatabase_option(OBParser.Database_optionContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#database_option}.
	 * @param ctx the parse tree
	 */
	void exitDatabase_option(OBParser.Database_optionContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#read_only_or_write}.
	 * @param ctx the parse tree
	 */
	void enterRead_only_or_write(OBParser.Read_only_or_writeContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#read_only_or_write}.
	 * @param ctx the parse tree
	 */
	void exitRead_only_or_write(OBParser.Read_only_or_writeContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#alter_database_stmt}.
	 * @param ctx the parse tree
	 */
	void enterAlter_database_stmt(OBParser.Alter_database_stmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#alter_database_stmt}.
	 * @param ctx the parse tree
	 */
	void exitAlter_database_stmt(OBParser.Alter_database_stmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#database_name}.
	 * @param ctx the parse tree
	 */
	void enterDatabase_name(OBParser.Database_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#database_name}.
	 * @param ctx the parse tree
	 */
	void exitDatabase_name(OBParser.Database_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#load_data_stmt}.
	 * @param ctx the parse tree
	 */
	void enterLoad_data_stmt(OBParser.Load_data_stmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#load_data_stmt}.
	 * @param ctx the parse tree
	 */
	void exitLoad_data_stmt(OBParser.Load_data_stmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#load_data_with_opt_hint}.
	 * @param ctx the parse tree
	 */
	void enterLoad_data_with_opt_hint(OBParser.Load_data_with_opt_hintContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#load_data_with_opt_hint}.
	 * @param ctx the parse tree
	 */
	void exitLoad_data_with_opt_hint(OBParser.Load_data_with_opt_hintContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#compression_name}.
	 * @param ctx the parse tree
	 */
	void enterCompression_name(OBParser.Compression_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#compression_name}.
	 * @param ctx the parse tree
	 */
	void exitCompression_name(OBParser.Compression_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#lines_or_rows}.
	 * @param ctx the parse tree
	 */
	void enterLines_or_rows(OBParser.Lines_or_rowsContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#lines_or_rows}.
	 * @param ctx the parse tree
	 */
	void exitLines_or_rows(OBParser.Lines_or_rowsContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#field_or_vars_list}.
	 * @param ctx the parse tree
	 */
	void enterField_or_vars_list(OBParser.Field_or_vars_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#field_or_vars_list}.
	 * @param ctx the parse tree
	 */
	void exitField_or_vars_list(OBParser.Field_or_vars_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#field_or_vars}.
	 * @param ctx the parse tree
	 */
	void enterField_or_vars(OBParser.Field_or_varsContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#field_or_vars}.
	 * @param ctx the parse tree
	 */
	void exitField_or_vars(OBParser.Field_or_varsContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#load_set_list}.
	 * @param ctx the parse tree
	 */
	void enterLoad_set_list(OBParser.Load_set_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#load_set_list}.
	 * @param ctx the parse tree
	 */
	void exitLoad_set_list(OBParser.Load_set_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#load_set_element}.
	 * @param ctx the parse tree
	 */
	void enterLoad_set_element(OBParser.Load_set_elementContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#load_set_element}.
	 * @param ctx the parse tree
	 */
	void exitLoad_set_element(OBParser.Load_set_elementContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#load_data_extended_option_list}.
	 * @param ctx the parse tree
	 */
	void enterLoad_data_extended_option_list(OBParser.Load_data_extended_option_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#load_data_extended_option_list}.
	 * @param ctx the parse tree
	 */
	void exitLoad_data_extended_option_list(OBParser.Load_data_extended_option_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#load_data_extended_option}.
	 * @param ctx the parse tree
	 */
	void enterLoad_data_extended_option(OBParser.Load_data_extended_optionContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#load_data_extended_option}.
	 * @param ctx the parse tree
	 */
	void exitLoad_data_extended_option(OBParser.Load_data_extended_optionContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#create_synonym_stmt}.
	 * @param ctx the parse tree
	 */
	void enterCreate_synonym_stmt(OBParser.Create_synonym_stmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#create_synonym_stmt}.
	 * @param ctx the parse tree
	 */
	void exitCreate_synonym_stmt(OBParser.Create_synonym_stmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#synonym_name}.
	 * @param ctx the parse tree
	 */
	void enterSynonym_name(OBParser.Synonym_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#synonym_name}.
	 * @param ctx the parse tree
	 */
	void exitSynonym_name(OBParser.Synonym_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#synonym_object}.
	 * @param ctx the parse tree
	 */
	void enterSynonym_object(OBParser.Synonym_objectContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#synonym_object}.
	 * @param ctx the parse tree
	 */
	void exitSynonym_object(OBParser.Synonym_objectContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#drop_synonym_stmt}.
	 * @param ctx the parse tree
	 */
	void enterDrop_synonym_stmt(OBParser.Drop_synonym_stmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#drop_synonym_stmt}.
	 * @param ctx the parse tree
	 */
	void exitDrop_synonym_stmt(OBParser.Drop_synonym_stmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#special_table_type}.
	 * @param ctx the parse tree
	 */
	void enterSpecial_table_type(OBParser.Special_table_typeContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#special_table_type}.
	 * @param ctx the parse tree
	 */
	void exitSpecial_table_type(OBParser.Special_table_typeContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#on_commit_option}.
	 * @param ctx the parse tree
	 */
	void enterOn_commit_option(OBParser.On_commit_optionContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#on_commit_option}.
	 * @param ctx the parse tree
	 */
	void exitOn_commit_option(OBParser.On_commit_optionContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#create_directory_stmt}.
	 * @param ctx the parse tree
	 */
	void enterCreate_directory_stmt(OBParser.Create_directory_stmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#create_directory_stmt}.
	 * @param ctx the parse tree
	 */
	void exitCreate_directory_stmt(OBParser.Create_directory_stmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#directory_name}.
	 * @param ctx the parse tree
	 */
	void enterDirectory_name(OBParser.Directory_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#directory_name}.
	 * @param ctx the parse tree
	 */
	void exitDirectory_name(OBParser.Directory_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#directory_path}.
	 * @param ctx the parse tree
	 */
	void enterDirectory_path(OBParser.Directory_pathContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#directory_path}.
	 * @param ctx the parse tree
	 */
	void exitDirectory_path(OBParser.Directory_pathContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#drop_directory_stmt}.
	 * @param ctx the parse tree
	 */
	void enterDrop_directory_stmt(OBParser.Drop_directory_stmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#drop_directory_stmt}.
	 * @param ctx the parse tree
	 */
	void exitDrop_directory_stmt(OBParser.Drop_directory_stmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#create_keystore_stmt}.
	 * @param ctx the parse tree
	 */
	void enterCreate_keystore_stmt(OBParser.Create_keystore_stmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#create_keystore_stmt}.
	 * @param ctx the parse tree
	 */
	void exitCreate_keystore_stmt(OBParser.Create_keystore_stmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#alter_keystore_stmt}.
	 * @param ctx the parse tree
	 */
	void enterAlter_keystore_stmt(OBParser.Alter_keystore_stmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#alter_keystore_stmt}.
	 * @param ctx the parse tree
	 */
	void exitAlter_keystore_stmt(OBParser.Alter_keystore_stmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#create_table_stmt}.
	 * @param ctx the parse tree
	 */
	void enterCreate_table_stmt(OBParser.Create_table_stmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#create_table_stmt}.
	 * @param ctx the parse tree
	 */
	void exitCreate_table_stmt(OBParser.Create_table_stmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#table_element_list}.
	 * @param ctx the parse tree
	 */
	void enterTable_element_list(OBParser.Table_element_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#table_element_list}.
	 * @param ctx the parse tree
	 */
	void exitTable_element_list(OBParser.Table_element_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#table_element}.
	 * @param ctx the parse tree
	 */
	void enterTable_element(OBParser.Table_elementContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#table_element}.
	 * @param ctx the parse tree
	 */
	void exitTable_element(OBParser.Table_elementContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#column_definition}.
	 * @param ctx the parse tree
	 */
	void enterColumn_definition(OBParser.Column_definitionContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#column_definition}.
	 * @param ctx the parse tree
	 */
	void exitColumn_definition(OBParser.Column_definitionContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#column_definition_opt_datatype}.
	 * @param ctx the parse tree
	 */
	void enterColumn_definition_opt_datatype(OBParser.Column_definition_opt_datatypeContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#column_definition_opt_datatype}.
	 * @param ctx the parse tree
	 */
	void exitColumn_definition_opt_datatype(OBParser.Column_definition_opt_datatypeContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#out_of_line_index}.
	 * @param ctx the parse tree
	 */
	void enterOut_of_line_index(OBParser.Out_of_line_indexContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#out_of_line_index}.
	 * @param ctx the parse tree
	 */
	void exitOut_of_line_index(OBParser.Out_of_line_indexContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#out_of_line_constraint}.
	 * @param ctx the parse tree
	 */
	void enterOut_of_line_constraint(OBParser.Out_of_line_constraintContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#out_of_line_constraint}.
	 * @param ctx the parse tree
	 */
	void exitOut_of_line_constraint(OBParser.Out_of_line_constraintContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#out_of_line_primary_index}.
	 * @param ctx the parse tree
	 */
	void enterOut_of_line_primary_index(OBParser.Out_of_line_primary_indexContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#out_of_line_primary_index}.
	 * @param ctx the parse tree
	 */
	void exitOut_of_line_primary_index(OBParser.Out_of_line_primary_indexContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#out_of_line_unique_index}.
	 * @param ctx the parse tree
	 */
	void enterOut_of_line_unique_index(OBParser.Out_of_line_unique_indexContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#out_of_line_unique_index}.
	 * @param ctx the parse tree
	 */
	void exitOut_of_line_unique_index(OBParser.Out_of_line_unique_indexContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#out_of_line_index_state}.
	 * @param ctx the parse tree
	 */
	void enterOut_of_line_index_state(OBParser.Out_of_line_index_stateContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#out_of_line_index_state}.
	 * @param ctx the parse tree
	 */
	void exitOut_of_line_index_state(OBParser.Out_of_line_index_stateContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#constraint_state}.
	 * @param ctx the parse tree
	 */
	void enterConstraint_state(OBParser.Constraint_stateContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#constraint_state}.
	 * @param ctx the parse tree
	 */
	void exitConstraint_state(OBParser.Constraint_stateContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#enable_option}.
	 * @param ctx the parse tree
	 */
	void enterEnable_option(OBParser.Enable_optionContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#enable_option}.
	 * @param ctx the parse tree
	 */
	void exitEnable_option(OBParser.Enable_optionContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#references_clause}.
	 * @param ctx the parse tree
	 */
	void enterReferences_clause(OBParser.References_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#references_clause}.
	 * @param ctx the parse tree
	 */
	void exitReferences_clause(OBParser.References_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#reference_option}.
	 * @param ctx the parse tree
	 */
	void enterReference_option(OBParser.Reference_optionContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#reference_option}.
	 * @param ctx the parse tree
	 */
	void exitReference_option(OBParser.Reference_optionContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#reference_action}.
	 * @param ctx the parse tree
	 */
	void enterReference_action(OBParser.Reference_actionContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#reference_action}.
	 * @param ctx the parse tree
	 */
	void exitReference_action(OBParser.Reference_actionContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#opt_generated_option_list}.
	 * @param ctx the parse tree
	 */
	void enterOpt_generated_option_list(OBParser.Opt_generated_option_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#opt_generated_option_list}.
	 * @param ctx the parse tree
	 */
	void exitOpt_generated_option_list(OBParser.Opt_generated_option_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#opt_generated_identity_option}.
	 * @param ctx the parse tree
	 */
	void enterOpt_generated_identity_option(OBParser.Opt_generated_identity_optionContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#opt_generated_identity_option}.
	 * @param ctx the parse tree
	 */
	void exitOpt_generated_identity_option(OBParser.Opt_generated_identity_optionContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#opt_generated_column_attribute_list}.
	 * @param ctx the parse tree
	 */
	void enterOpt_generated_column_attribute_list(OBParser.Opt_generated_column_attribute_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#opt_generated_column_attribute_list}.
	 * @param ctx the parse tree
	 */
	void exitOpt_generated_column_attribute_list(OBParser.Opt_generated_column_attribute_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#generated_column_attribute}.
	 * @param ctx the parse tree
	 */
	void enterGenerated_column_attribute(OBParser.Generated_column_attributeContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#generated_column_attribute}.
	 * @param ctx the parse tree
	 */
	void exitGenerated_column_attribute(OBParser.Generated_column_attributeContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#opt_identity_attribute}.
	 * @param ctx the parse tree
	 */
	void enterOpt_identity_attribute(OBParser.Opt_identity_attributeContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#opt_identity_attribute}.
	 * @param ctx the parse tree
	 */
	void exitOpt_identity_attribute(OBParser.Opt_identity_attributeContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#column_definition_ref}.
	 * @param ctx the parse tree
	 */
	void enterColumn_definition_ref(OBParser.Column_definition_refContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#column_definition_ref}.
	 * @param ctx the parse tree
	 */
	void exitColumn_definition_ref(OBParser.Column_definition_refContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#column_definition_list}.
	 * @param ctx the parse tree
	 */
	void enterColumn_definition_list(OBParser.Column_definition_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#column_definition_list}.
	 * @param ctx the parse tree
	 */
	void exitColumn_definition_list(OBParser.Column_definition_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#column_definition_opt_datatype_list}.
	 * @param ctx the parse tree
	 */
	void enterColumn_definition_opt_datatype_list(OBParser.Column_definition_opt_datatype_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#column_definition_opt_datatype_list}.
	 * @param ctx the parse tree
	 */
	void exitColumn_definition_opt_datatype_list(OBParser.Column_definition_opt_datatype_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#column_name_list}.
	 * @param ctx the parse tree
	 */
	void enterColumn_name_list(OBParser.Column_name_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#column_name_list}.
	 * @param ctx the parse tree
	 */
	void exitColumn_name_list(OBParser.Column_name_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#zero_suffix_intnum}.
	 * @param ctx the parse tree
	 */
	void enterZero_suffix_intnum(OBParser.Zero_suffix_intnumContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#zero_suffix_intnum}.
	 * @param ctx the parse tree
	 */
	void exitZero_suffix_intnum(OBParser.Zero_suffix_intnumContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#cast_data_type}.
	 * @param ctx the parse tree
	 */
	void enterCast_data_type(OBParser.Cast_data_typeContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#cast_data_type}.
	 * @param ctx the parse tree
	 */
	void exitCast_data_type(OBParser.Cast_data_typeContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#treat_data_type}.
	 * @param ctx the parse tree
	 */
	void enterTreat_data_type(OBParser.Treat_data_typeContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#treat_data_type}.
	 * @param ctx the parse tree
	 */
	void exitTreat_data_type(OBParser.Treat_data_typeContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#obj_access_ref_cast}.
	 * @param ctx the parse tree
	 */
	void enterObj_access_ref_cast(OBParser.Obj_access_ref_castContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#obj_access_ref_cast}.
	 * @param ctx the parse tree
	 */
	void exitObj_access_ref_cast(OBParser.Obj_access_ref_castContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#value_or_type_name}.
	 * @param ctx the parse tree
	 */
	void enterValue_or_type_name(OBParser.Value_or_type_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#value_or_type_name}.
	 * @param ctx the parse tree
	 */
	void exitValue_or_type_name(OBParser.Value_or_type_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#udt_type_i}.
	 * @param ctx the parse tree
	 */
	void enterUdt_type_i(OBParser.Udt_type_iContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#udt_type_i}.
	 * @param ctx the parse tree
	 */
	void exitUdt_type_i(OBParser.Udt_type_iContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#type_name}.
	 * @param ctx the parse tree
	 */
	void enterType_name(OBParser.Type_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#type_name}.
	 * @param ctx the parse tree
	 */
	void exitType_name(OBParser.Type_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#data_type}.
	 * @param ctx the parse tree
	 */
	void enterData_type(OBParser.Data_typeContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#data_type}.
	 * @param ctx the parse tree
	 */
	void exitData_type(OBParser.Data_typeContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#binary_type_i}.
	 * @param ctx the parse tree
	 */
	void enterBinary_type_i(OBParser.Binary_type_iContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#binary_type_i}.
	 * @param ctx the parse tree
	 */
	void exitBinary_type_i(OBParser.Binary_type_iContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#float_type_i}.
	 * @param ctx the parse tree
	 */
	void enterFloat_type_i(OBParser.Float_type_iContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#float_type_i}.
	 * @param ctx the parse tree
	 */
	void exitFloat_type_i(OBParser.Float_type_iContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#character_type_i}.
	 * @param ctx the parse tree
	 */
	void enterCharacter_type_i(OBParser.Character_type_iContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#character_type_i}.
	 * @param ctx the parse tree
	 */
	void exitCharacter_type_i(OBParser.Character_type_iContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#rowid_type_i}.
	 * @param ctx the parse tree
	 */
	void enterRowid_type_i(OBParser.Rowid_type_iContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#rowid_type_i}.
	 * @param ctx the parse tree
	 */
	void exitRowid_type_i(OBParser.Rowid_type_iContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#interval_type_i}.
	 * @param ctx the parse tree
	 */
	void enterInterval_type_i(OBParser.Interval_type_iContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#interval_type_i}.
	 * @param ctx the parse tree
	 */
	void exitInterval_type_i(OBParser.Interval_type_iContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#number_type_i}.
	 * @param ctx the parse tree
	 */
	void enterNumber_type_i(OBParser.Number_type_iContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#number_type_i}.
	 * @param ctx the parse tree
	 */
	void exitNumber_type_i(OBParser.Number_type_iContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#timestamp_type_i}.
	 * @param ctx the parse tree
	 */
	void enterTimestamp_type_i(OBParser.Timestamp_type_iContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#timestamp_type_i}.
	 * @param ctx the parse tree
	 */
	void exitTimestamp_type_i(OBParser.Timestamp_type_iContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#data_type_precision}.
	 * @param ctx the parse tree
	 */
	void enterData_type_precision(OBParser.Data_type_precisionContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#data_type_precision}.
	 * @param ctx the parse tree
	 */
	void exitData_type_precision(OBParser.Data_type_precisionContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#int_type_i}.
	 * @param ctx the parse tree
	 */
	void enterInt_type_i(OBParser.Int_type_iContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#int_type_i}.
	 * @param ctx the parse tree
	 */
	void exitInt_type_i(OBParser.Int_type_iContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#varchar_type_i}.
	 * @param ctx the parse tree
	 */
	void enterVarchar_type_i(OBParser.Varchar_type_iContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#varchar_type_i}.
	 * @param ctx the parse tree
	 */
	void exitVarchar_type_i(OBParser.Varchar_type_iContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#double_type_i}.
	 * @param ctx the parse tree
	 */
	void enterDouble_type_i(OBParser.Double_type_iContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#double_type_i}.
	 * @param ctx the parse tree
	 */
	void exitDouble_type_i(OBParser.Double_type_iContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#datetime_type_i}.
	 * @param ctx the parse tree
	 */
	void enterDatetime_type_i(OBParser.Datetime_type_iContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#datetime_type_i}.
	 * @param ctx the parse tree
	 */
	void exitDatetime_type_i(OBParser.Datetime_type_iContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#number_precision}.
	 * @param ctx the parse tree
	 */
	void enterNumber_precision(OBParser.Number_precisionContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#number_precision}.
	 * @param ctx the parse tree
	 */
	void exitNumber_precision(OBParser.Number_precisionContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#signed_int_num}.
	 * @param ctx the parse tree
	 */
	void enterSigned_int_num(OBParser.Signed_int_numContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#signed_int_num}.
	 * @param ctx the parse tree
	 */
	void exitSigned_int_num(OBParser.Signed_int_numContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#precision_int_num}.
	 * @param ctx the parse tree
	 */
	void enterPrecision_int_num(OBParser.Precision_int_numContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#precision_int_num}.
	 * @param ctx the parse tree
	 */
	void exitPrecision_int_num(OBParser.Precision_int_numContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#precision_decimal_num}.
	 * @param ctx the parse tree
	 */
	void enterPrecision_decimal_num(OBParser.Precision_decimal_numContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#precision_decimal_num}.
	 * @param ctx the parse tree
	 */
	void exitPrecision_decimal_num(OBParser.Precision_decimal_numContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#nstring_length_i}.
	 * @param ctx the parse tree
	 */
	void enterNstring_length_i(OBParser.Nstring_length_iContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#nstring_length_i}.
	 * @param ctx the parse tree
	 */
	void exitNstring_length_i(OBParser.Nstring_length_iContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#string_length_i}.
	 * @param ctx the parse tree
	 */
	void enterString_length_i(OBParser.String_length_iContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#string_length_i}.
	 * @param ctx the parse tree
	 */
	void exitString_length_i(OBParser.String_length_iContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#urowid_length_i}.
	 * @param ctx the parse tree
	 */
	void enterUrowid_length_i(OBParser.Urowid_length_iContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#urowid_length_i}.
	 * @param ctx the parse tree
	 */
	void exitUrowid_length_i(OBParser.Urowid_length_iContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#collation_name}.
	 * @param ctx the parse tree
	 */
	void enterCollation_name(OBParser.Collation_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#collation_name}.
	 * @param ctx the parse tree
	 */
	void exitCollation_name(OBParser.Collation_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#trans_param_name}.
	 * @param ctx the parse tree
	 */
	void enterTrans_param_name(OBParser.Trans_param_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#trans_param_name}.
	 * @param ctx the parse tree
	 */
	void exitTrans_param_name(OBParser.Trans_param_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#trans_param_value}.
	 * @param ctx the parse tree
	 */
	void enterTrans_param_value(OBParser.Trans_param_valueContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#trans_param_value}.
	 * @param ctx the parse tree
	 */
	void exitTrans_param_value(OBParser.Trans_param_valueContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#dblink_info_param_name}.
	 * @param ctx the parse tree
	 */
	void enterDblink_info_param_name(OBParser.Dblink_info_param_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#dblink_info_param_name}.
	 * @param ctx the parse tree
	 */
	void exitDblink_info_param_name(OBParser.Dblink_info_param_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#dblink_info_param_value}.
	 * @param ctx the parse tree
	 */
	void enterDblink_info_param_value(OBParser.Dblink_info_param_valueContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#dblink_info_param_value}.
	 * @param ctx the parse tree
	 */
	void exitDblink_info_param_value(OBParser.Dblink_info_param_valueContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#charset_name}.
	 * @param ctx the parse tree
	 */
	void enterCharset_name(OBParser.Charset_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#charset_name}.
	 * @param ctx the parse tree
	 */
	void exitCharset_name(OBParser.Charset_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#charset_name_or_default}.
	 * @param ctx the parse tree
	 */
	void enterCharset_name_or_default(OBParser.Charset_name_or_defaultContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#charset_name_or_default}.
	 * @param ctx the parse tree
	 */
	void exitCharset_name_or_default(OBParser.Charset_name_or_defaultContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#collation}.
	 * @param ctx the parse tree
	 */
	void enterCollation(OBParser.CollationContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#collation}.
	 * @param ctx the parse tree
	 */
	void exitCollation(OBParser.CollationContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#opt_column_attribute_list}.
	 * @param ctx the parse tree
	 */
	void enterOpt_column_attribute_list(OBParser.Opt_column_attribute_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#opt_column_attribute_list}.
	 * @param ctx the parse tree
	 */
	void exitOpt_column_attribute_list(OBParser.Opt_column_attribute_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#column_attribute}.
	 * @param ctx the parse tree
	 */
	void enterColumn_attribute(OBParser.Column_attributeContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#column_attribute}.
	 * @param ctx the parse tree
	 */
	void exitColumn_attribute(OBParser.Column_attributeContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#now_or_signed_literal}.
	 * @param ctx the parse tree
	 */
	void enterNow_or_signed_literal(OBParser.Now_or_signed_literalContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#now_or_signed_literal}.
	 * @param ctx the parse tree
	 */
	void exitNow_or_signed_literal(OBParser.Now_or_signed_literalContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#cur_timestamp_func_params}.
	 * @param ctx the parse tree
	 */
	void enterCur_timestamp_func_params(OBParser.Cur_timestamp_func_paramsContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#cur_timestamp_func_params}.
	 * @param ctx the parse tree
	 */
	void exitCur_timestamp_func_params(OBParser.Cur_timestamp_func_paramsContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#signed_literal_params}.
	 * @param ctx the parse tree
	 */
	void enterSigned_literal_params(OBParser.Signed_literal_paramsContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#signed_literal_params}.
	 * @param ctx the parse tree
	 */
	void exitSigned_literal_params(OBParser.Signed_literal_paramsContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#signed_literal}.
	 * @param ctx the parse tree
	 */
	void enterSigned_literal(OBParser.Signed_literalContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#signed_literal}.
	 * @param ctx the parse tree
	 */
	void exitSigned_literal(OBParser.Signed_literalContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#opt_comma}.
	 * @param ctx the parse tree
	 */
	void enterOpt_comma(OBParser.Opt_commaContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#opt_comma}.
	 * @param ctx the parse tree
	 */
	void exitOpt_comma(OBParser.Opt_commaContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#table_option_list_space_seperated}.
	 * @param ctx the parse tree
	 */
	void enterTable_option_list_space_seperated(OBParser.Table_option_list_space_seperatedContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#table_option_list_space_seperated}.
	 * @param ctx the parse tree
	 */
	void exitTable_option_list_space_seperated(OBParser.Table_option_list_space_seperatedContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#table_option_list}.
	 * @param ctx the parse tree
	 */
	void enterTable_option_list(OBParser.Table_option_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#table_option_list}.
	 * @param ctx the parse tree
	 */
	void exitTable_option_list(OBParser.Table_option_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#primary_zone_name}.
	 * @param ctx the parse tree
	 */
	void enterPrimary_zone_name(OBParser.Primary_zone_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#primary_zone_name}.
	 * @param ctx the parse tree
	 */
	void exitPrimary_zone_name(OBParser.Primary_zone_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#locality_name}.
	 * @param ctx the parse tree
	 */
	void enterLocality_name(OBParser.Locality_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#locality_name}.
	 * @param ctx the parse tree
	 */
	void exitLocality_name(OBParser.Locality_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#table_option}.
	 * @param ctx the parse tree
	 */
	void enterTable_option(OBParser.Table_optionContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#table_option}.
	 * @param ctx the parse tree
	 */
	void exitTable_option(OBParser.Table_optionContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#parallel_option}.
	 * @param ctx the parse tree
	 */
	void enterParallel_option(OBParser.Parallel_optionContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#parallel_option}.
	 * @param ctx the parse tree
	 */
	void exitParallel_option(OBParser.Parallel_optionContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#storage_options_list}.
	 * @param ctx the parse tree
	 */
	void enterStorage_options_list(OBParser.Storage_options_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#storage_options_list}.
	 * @param ctx the parse tree
	 */
	void exitStorage_options_list(OBParser.Storage_options_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#storage_option}.
	 * @param ctx the parse tree
	 */
	void enterStorage_option(OBParser.Storage_optionContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#storage_option}.
	 * @param ctx the parse tree
	 */
	void exitStorage_option(OBParser.Storage_optionContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#size_option}.
	 * @param ctx the parse tree
	 */
	void enterSize_option(OBParser.Size_optionContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#size_option}.
	 * @param ctx the parse tree
	 */
	void exitSize_option(OBParser.Size_optionContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#int_or_unlimited}.
	 * @param ctx the parse tree
	 */
	void enterInt_or_unlimited(OBParser.Int_or_unlimitedContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#int_or_unlimited}.
	 * @param ctx the parse tree
	 */
	void exitInt_or_unlimited(OBParser.Int_or_unlimitedContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#unit_of_size}.
	 * @param ctx the parse tree
	 */
	void enterUnit_of_size(OBParser.Unit_of_sizeContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#unit_of_size}.
	 * @param ctx the parse tree
	 */
	void exitUnit_of_size(OBParser.Unit_of_sizeContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#relation_name_or_string}.
	 * @param ctx the parse tree
	 */
	void enterRelation_name_or_string(OBParser.Relation_name_or_stringContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#relation_name_or_string}.
	 * @param ctx the parse tree
	 */
	void exitRelation_name_or_string(OBParser.Relation_name_or_stringContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#opt_equal_mark}.
	 * @param ctx the parse tree
	 */
	void enterOpt_equal_mark(OBParser.Opt_equal_markContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#opt_equal_mark}.
	 * @param ctx the parse tree
	 */
	void exitOpt_equal_mark(OBParser.Opt_equal_markContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#partition_option_inner}.
	 * @param ctx the parse tree
	 */
	void enterPartition_option_inner(OBParser.Partition_option_innerContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#partition_option_inner}.
	 * @param ctx the parse tree
	 */
	void exitPartition_option_inner(OBParser.Partition_option_innerContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#external_table_partition_option}.
	 * @param ctx the parse tree
	 */
	void enterExternal_table_partition_option(OBParser.External_table_partition_optionContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#external_table_partition_option}.
	 * @param ctx the parse tree
	 */
	void exitExternal_table_partition_option(OBParser.External_table_partition_optionContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#auto_partition_option}.
	 * @param ctx the parse tree
	 */
	void enterAuto_partition_option(OBParser.Auto_partition_optionContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#auto_partition_option}.
	 * @param ctx the parse tree
	 */
	void exitAuto_partition_option(OBParser.Auto_partition_optionContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#column_group_element}.
	 * @param ctx the parse tree
	 */
	void enterColumn_group_element(OBParser.Column_group_elementContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#column_group_element}.
	 * @param ctx the parse tree
	 */
	void exitColumn_group_element(OBParser.Column_group_elementContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#column_group_list}.
	 * @param ctx the parse tree
	 */
	void enterColumn_group_list(OBParser.Column_group_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#column_group_list}.
	 * @param ctx the parse tree
	 */
	void exitColumn_group_list(OBParser.Column_group_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#with_column_group}.
	 * @param ctx the parse tree
	 */
	void enterWith_column_group(OBParser.With_column_groupContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#with_column_group}.
	 * @param ctx the parse tree
	 */
	void exitWith_column_group(OBParser.With_column_groupContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#partition_size}.
	 * @param ctx the parse tree
	 */
	void enterPartition_size(OBParser.Partition_sizeContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#partition_size}.
	 * @param ctx the parse tree
	 */
	void exitPartition_size(OBParser.Partition_sizeContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#auto_partition_type}.
	 * @param ctx the parse tree
	 */
	void enterAuto_partition_type(OBParser.Auto_partition_typeContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#auto_partition_type}.
	 * @param ctx the parse tree
	 */
	void exitAuto_partition_type(OBParser.Auto_partition_typeContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#auto_range_type}.
	 * @param ctx the parse tree
	 */
	void enterAuto_range_type(OBParser.Auto_range_typeContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#auto_range_type}.
	 * @param ctx the parse tree
	 */
	void exitAuto_range_type(OBParser.Auto_range_typeContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#partition_option}.
	 * @param ctx the parse tree
	 */
	void enterPartition_option(OBParser.Partition_optionContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#partition_option}.
	 * @param ctx the parse tree
	 */
	void exitPartition_option(OBParser.Partition_optionContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#hash_partition_option}.
	 * @param ctx the parse tree
	 */
	void enterHash_partition_option(OBParser.Hash_partition_optionContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#hash_partition_option}.
	 * @param ctx the parse tree
	 */
	void exitHash_partition_option(OBParser.Hash_partition_optionContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#hash_partition_attributes_option_list}.
	 * @param ctx the parse tree
	 */
	void enterHash_partition_attributes_option_list(OBParser.Hash_partition_attributes_option_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#hash_partition_attributes_option_list}.
	 * @param ctx the parse tree
	 */
	void exitHash_partition_attributes_option_list(OBParser.Hash_partition_attributes_option_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#list_partition_option}.
	 * @param ctx the parse tree
	 */
	void enterList_partition_option(OBParser.List_partition_optionContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#list_partition_option}.
	 * @param ctx the parse tree
	 */
	void exitList_partition_option(OBParser.List_partition_optionContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#range_partition_option}.
	 * @param ctx the parse tree
	 */
	void enterRange_partition_option(OBParser.Range_partition_optionContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#range_partition_option}.
	 * @param ctx the parse tree
	 */
	void exitRange_partition_option(OBParser.Range_partition_optionContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#interval_option}.
	 * @param ctx the parse tree
	 */
	void enterInterval_option(OBParser.Interval_optionContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#interval_option}.
	 * @param ctx the parse tree
	 */
	void exitInterval_option(OBParser.Interval_optionContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#subpartition_option}.
	 * @param ctx the parse tree
	 */
	void enterSubpartition_option(OBParser.Subpartition_optionContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#subpartition_option}.
	 * @param ctx the parse tree
	 */
	void exitSubpartition_option(OBParser.Subpartition_optionContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#subpartition_template_option}.
	 * @param ctx the parse tree
	 */
	void enterSubpartition_template_option(OBParser.Subpartition_template_optionContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#subpartition_template_option}.
	 * @param ctx the parse tree
	 */
	void exitSubpartition_template_option(OBParser.Subpartition_template_optionContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#subpartition_individual_option}.
	 * @param ctx the parse tree
	 */
	void enterSubpartition_individual_option(OBParser.Subpartition_individual_optionContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#subpartition_individual_option}.
	 * @param ctx the parse tree
	 */
	void exitSubpartition_individual_option(OBParser.Subpartition_individual_optionContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#aux_column_list}.
	 * @param ctx the parse tree
	 */
	void enterAux_column_list(OBParser.Aux_column_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#aux_column_list}.
	 * @param ctx the parse tree
	 */
	void exitAux_column_list(OBParser.Aux_column_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#vertical_column_name}.
	 * @param ctx the parse tree
	 */
	void enterVertical_column_name(OBParser.Vertical_column_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#vertical_column_name}.
	 * @param ctx the parse tree
	 */
	void exitVertical_column_name(OBParser.Vertical_column_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#hash_partition_list}.
	 * @param ctx the parse tree
	 */
	void enterHash_partition_list(OBParser.Hash_partition_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#hash_partition_list}.
	 * @param ctx the parse tree
	 */
	void exitHash_partition_list(OBParser.Hash_partition_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#hash_partition_element}.
	 * @param ctx the parse tree
	 */
	void enterHash_partition_element(OBParser.Hash_partition_elementContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#hash_partition_element}.
	 * @param ctx the parse tree
	 */
	void exitHash_partition_element(OBParser.Hash_partition_elementContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#opt_range_partition_list}.
	 * @param ctx the parse tree
	 */
	void enterOpt_range_partition_list(OBParser.Opt_range_partition_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#opt_range_partition_list}.
	 * @param ctx the parse tree
	 */
	void exitOpt_range_partition_list(OBParser.Opt_range_partition_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#range_partition_list}.
	 * @param ctx the parse tree
	 */
	void enterRange_partition_list(OBParser.Range_partition_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#range_partition_list}.
	 * @param ctx the parse tree
	 */
	void exitRange_partition_list(OBParser.Range_partition_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#partition_attributes_option_list}.
	 * @param ctx the parse tree
	 */
	void enterPartition_attributes_option_list(OBParser.Partition_attributes_option_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#partition_attributes_option_list}.
	 * @param ctx the parse tree
	 */
	void exitPartition_attributes_option_list(OBParser.Partition_attributes_option_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#range_partition_element}.
	 * @param ctx the parse tree
	 */
	void enterRange_partition_element(OBParser.Range_partition_elementContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#range_partition_element}.
	 * @param ctx the parse tree
	 */
	void exitRange_partition_element(OBParser.Range_partition_elementContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#opt_list_partition_list}.
	 * @param ctx the parse tree
	 */
	void enterOpt_list_partition_list(OBParser.Opt_list_partition_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#opt_list_partition_list}.
	 * @param ctx the parse tree
	 */
	void exitOpt_list_partition_list(OBParser.Opt_list_partition_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#list_partition_list}.
	 * @param ctx the parse tree
	 */
	void enterList_partition_list(OBParser.List_partition_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#list_partition_list}.
	 * @param ctx the parse tree
	 */
	void exitList_partition_list(OBParser.List_partition_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#list_partition_element}.
	 * @param ctx the parse tree
	 */
	void enterList_partition_element(OBParser.List_partition_elementContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#list_partition_element}.
	 * @param ctx the parse tree
	 */
	void exitList_partition_element(OBParser.List_partition_elementContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#subpartition_list}.
	 * @param ctx the parse tree
	 */
	void enterSubpartition_list(OBParser.Subpartition_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#subpartition_list}.
	 * @param ctx the parse tree
	 */
	void exitSubpartition_list(OBParser.Subpartition_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#opt_hash_subpartition_list}.
	 * @param ctx the parse tree
	 */
	void enterOpt_hash_subpartition_list(OBParser.Opt_hash_subpartition_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#opt_hash_subpartition_list}.
	 * @param ctx the parse tree
	 */
	void exitOpt_hash_subpartition_list(OBParser.Opt_hash_subpartition_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#hash_subpartition_list}.
	 * @param ctx the parse tree
	 */
	void enterHash_subpartition_list(OBParser.Hash_subpartition_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#hash_subpartition_list}.
	 * @param ctx the parse tree
	 */
	void exitHash_subpartition_list(OBParser.Hash_subpartition_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#hash_subpartition_element}.
	 * @param ctx the parse tree
	 */
	void enterHash_subpartition_element(OBParser.Hash_subpartition_elementContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#hash_subpartition_element}.
	 * @param ctx the parse tree
	 */
	void exitHash_subpartition_element(OBParser.Hash_subpartition_elementContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#opt_range_subpartition_list}.
	 * @param ctx the parse tree
	 */
	void enterOpt_range_subpartition_list(OBParser.Opt_range_subpartition_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#opt_range_subpartition_list}.
	 * @param ctx the parse tree
	 */
	void exitOpt_range_subpartition_list(OBParser.Opt_range_subpartition_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#range_subpartition_list}.
	 * @param ctx the parse tree
	 */
	void enterRange_subpartition_list(OBParser.Range_subpartition_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#range_subpartition_list}.
	 * @param ctx the parse tree
	 */
	void exitRange_subpartition_list(OBParser.Range_subpartition_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#range_subpartition_element}.
	 * @param ctx the parse tree
	 */
	void enterRange_subpartition_element(OBParser.Range_subpartition_elementContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#range_subpartition_element}.
	 * @param ctx the parse tree
	 */
	void exitRange_subpartition_element(OBParser.Range_subpartition_elementContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#opt_list_subpartition_list}.
	 * @param ctx the parse tree
	 */
	void enterOpt_list_subpartition_list(OBParser.Opt_list_subpartition_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#opt_list_subpartition_list}.
	 * @param ctx the parse tree
	 */
	void exitOpt_list_subpartition_list(OBParser.Opt_list_subpartition_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#list_subpartition_list}.
	 * @param ctx the parse tree
	 */
	void enterList_subpartition_list(OBParser.List_subpartition_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#list_subpartition_list}.
	 * @param ctx the parse tree
	 */
	void exitList_subpartition_list(OBParser.List_subpartition_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#list_subpartition_element}.
	 * @param ctx the parse tree
	 */
	void enterList_subpartition_element(OBParser.List_subpartition_elementContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#list_subpartition_element}.
	 * @param ctx the parse tree
	 */
	void exitList_subpartition_element(OBParser.List_subpartition_elementContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#list_partition_expr}.
	 * @param ctx the parse tree
	 */
	void enterList_partition_expr(OBParser.List_partition_exprContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#list_partition_expr}.
	 * @param ctx the parse tree
	 */
	void exitList_partition_expr(OBParser.List_partition_exprContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#list_expr}.
	 * @param ctx the parse tree
	 */
	void enterList_expr(OBParser.List_exprContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#list_expr}.
	 * @param ctx the parse tree
	 */
	void exitList_expr(OBParser.List_exprContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#physical_attributes_option_list}.
	 * @param ctx the parse tree
	 */
	void enterPhysical_attributes_option_list(OBParser.Physical_attributes_option_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#physical_attributes_option_list}.
	 * @param ctx the parse tree
	 */
	void exitPhysical_attributes_option_list(OBParser.Physical_attributes_option_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#physical_attributes_option}.
	 * @param ctx the parse tree
	 */
	void enterPhysical_attributes_option(OBParser.Physical_attributes_optionContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#physical_attributes_option}.
	 * @param ctx the parse tree
	 */
	void exitPhysical_attributes_option(OBParser.Physical_attributes_optionContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#opt_special_partition_list}.
	 * @param ctx the parse tree
	 */
	void enterOpt_special_partition_list(OBParser.Opt_special_partition_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#opt_special_partition_list}.
	 * @param ctx the parse tree
	 */
	void exitOpt_special_partition_list(OBParser.Opt_special_partition_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#special_partition_list}.
	 * @param ctx the parse tree
	 */
	void enterSpecial_partition_list(OBParser.Special_partition_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#special_partition_list}.
	 * @param ctx the parse tree
	 */
	void exitSpecial_partition_list(OBParser.Special_partition_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#special_partition_define}.
	 * @param ctx the parse tree
	 */
	void enterSpecial_partition_define(OBParser.Special_partition_defineContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#special_partition_define}.
	 * @param ctx the parse tree
	 */
	void exitSpecial_partition_define(OBParser.Special_partition_defineContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#range_partition_expr}.
	 * @param ctx the parse tree
	 */
	void enterRange_partition_expr(OBParser.Range_partition_exprContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#range_partition_expr}.
	 * @param ctx the parse tree
	 */
	void exitRange_partition_expr(OBParser.Range_partition_exprContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#range_expr_list}.
	 * @param ctx the parse tree
	 */
	void enterRange_expr_list(OBParser.Range_expr_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#range_expr_list}.
	 * @param ctx the parse tree
	 */
	void exitRange_expr_list(OBParser.Range_expr_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#range_expr}.
	 * @param ctx the parse tree
	 */
	void enterRange_expr(OBParser.Range_exprContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#range_expr}.
	 * @param ctx the parse tree
	 */
	void exitRange_expr(OBParser.Range_exprContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#hash_subpartition_quantity}.
	 * @param ctx the parse tree
	 */
	void enterHash_subpartition_quantity(OBParser.Hash_subpartition_quantityContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#hash_subpartition_quantity}.
	 * @param ctx the parse tree
	 */
	void exitHash_subpartition_quantity(OBParser.Hash_subpartition_quantityContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#int_or_decimal}.
	 * @param ctx the parse tree
	 */
	void enterInt_or_decimal(OBParser.Int_or_decimalContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#int_or_decimal}.
	 * @param ctx the parse tree
	 */
	void exitInt_or_decimal(OBParser.Int_or_decimalContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#tg_hash_partition_option}.
	 * @param ctx the parse tree
	 */
	void enterTg_hash_partition_option(OBParser.Tg_hash_partition_optionContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#tg_hash_partition_option}.
	 * @param ctx the parse tree
	 */
	void exitTg_hash_partition_option(OBParser.Tg_hash_partition_optionContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#tg_range_partition_option}.
	 * @param ctx the parse tree
	 */
	void enterTg_range_partition_option(OBParser.Tg_range_partition_optionContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#tg_range_partition_option}.
	 * @param ctx the parse tree
	 */
	void exitTg_range_partition_option(OBParser.Tg_range_partition_optionContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#tg_list_partition_option}.
	 * @param ctx the parse tree
	 */
	void enterTg_list_partition_option(OBParser.Tg_list_partition_optionContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#tg_list_partition_option}.
	 * @param ctx the parse tree
	 */
	void exitTg_list_partition_option(OBParser.Tg_list_partition_optionContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#tg_subpartition_option}.
	 * @param ctx the parse tree
	 */
	void enterTg_subpartition_option(OBParser.Tg_subpartition_optionContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#tg_subpartition_option}.
	 * @param ctx the parse tree
	 */
	void exitTg_subpartition_option(OBParser.Tg_subpartition_optionContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#tg_subpartition_template_option}.
	 * @param ctx the parse tree
	 */
	void enterTg_subpartition_template_option(OBParser.Tg_subpartition_template_optionContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#tg_subpartition_template_option}.
	 * @param ctx the parse tree
	 */
	void exitTg_subpartition_template_option(OBParser.Tg_subpartition_template_optionContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#tg_subpartition_individual_option}.
	 * @param ctx the parse tree
	 */
	void enterTg_subpartition_individual_option(OBParser.Tg_subpartition_individual_optionContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#tg_subpartition_individual_option}.
	 * @param ctx the parse tree
	 */
	void exitTg_subpartition_individual_option(OBParser.Tg_subpartition_individual_optionContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#opt_alter_compress_option}.
	 * @param ctx the parse tree
	 */
	void enterOpt_alter_compress_option(OBParser.Opt_alter_compress_optionContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#opt_alter_compress_option}.
	 * @param ctx the parse tree
	 */
	void exitOpt_alter_compress_option(OBParser.Opt_alter_compress_optionContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#compress_option}.
	 * @param ctx the parse tree
	 */
	void enterCompress_option(OBParser.Compress_optionContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#compress_option}.
	 * @param ctx the parse tree
	 */
	void exitCompress_option(OBParser.Compress_optionContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#opt_compress_level}.
	 * @param ctx the parse tree
	 */
	void enterOpt_compress_level(OBParser.Opt_compress_levelContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#opt_compress_level}.
	 * @param ctx the parse tree
	 */
	void exitOpt_compress_level(OBParser.Opt_compress_levelContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#external_properties_list}.
	 * @param ctx the parse tree
	 */
	void enterExternal_properties_list(OBParser.External_properties_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#external_properties_list}.
	 * @param ctx the parse tree
	 */
	void exitExternal_properties_list(OBParser.External_properties_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#external_properties}.
	 * @param ctx the parse tree
	 */
	void enterExternal_properties(OBParser.External_propertiesContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#external_properties}.
	 * @param ctx the parse tree
	 */
	void exitExternal_properties(OBParser.External_propertiesContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#external_file_format_list}.
	 * @param ctx the parse tree
	 */
	void enterExternal_file_format_list(OBParser.External_file_format_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#external_file_format_list}.
	 * @param ctx the parse tree
	 */
	void exitExternal_file_format_list(OBParser.External_file_format_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#external_file_format}.
	 * @param ctx the parse tree
	 */
	void enterExternal_file_format(OBParser.External_file_formatContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#external_file_format}.
	 * @param ctx the parse tree
	 */
	void exitExternal_file_format(OBParser.External_file_formatContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#create_tablegroup_stmt}.
	 * @param ctx the parse tree
	 */
	void enterCreate_tablegroup_stmt(OBParser.Create_tablegroup_stmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#create_tablegroup_stmt}.
	 * @param ctx the parse tree
	 */
	void exitCreate_tablegroup_stmt(OBParser.Create_tablegroup_stmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#drop_tablegroup_stmt}.
	 * @param ctx the parse tree
	 */
	void enterDrop_tablegroup_stmt(OBParser.Drop_tablegroup_stmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#drop_tablegroup_stmt}.
	 * @param ctx the parse tree
	 */
	void exitDrop_tablegroup_stmt(OBParser.Drop_tablegroup_stmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#alter_tablegroup_stmt}.
	 * @param ctx the parse tree
	 */
	void enterAlter_tablegroup_stmt(OBParser.Alter_tablegroup_stmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#alter_tablegroup_stmt}.
	 * @param ctx the parse tree
	 */
	void exitAlter_tablegroup_stmt(OBParser.Alter_tablegroup_stmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#tablegroup_option_list_space_seperated}.
	 * @param ctx the parse tree
	 */
	void enterTablegroup_option_list_space_seperated(OBParser.Tablegroup_option_list_space_seperatedContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#tablegroup_option_list_space_seperated}.
	 * @param ctx the parse tree
	 */
	void exitTablegroup_option_list_space_seperated(OBParser.Tablegroup_option_list_space_seperatedContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#tablegroup_option_list}.
	 * @param ctx the parse tree
	 */
	void enterTablegroup_option_list(OBParser.Tablegroup_option_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#tablegroup_option_list}.
	 * @param ctx the parse tree
	 */
	void exitTablegroup_option_list(OBParser.Tablegroup_option_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#tablegroup_option}.
	 * @param ctx the parse tree
	 */
	void enterTablegroup_option(OBParser.Tablegroup_optionContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#tablegroup_option}.
	 * @param ctx the parse tree
	 */
	void exitTablegroup_option(OBParser.Tablegroup_optionContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#alter_tablegroup_actions}.
	 * @param ctx the parse tree
	 */
	void enterAlter_tablegroup_actions(OBParser.Alter_tablegroup_actionsContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#alter_tablegroup_actions}.
	 * @param ctx the parse tree
	 */
	void exitAlter_tablegroup_actions(OBParser.Alter_tablegroup_actionsContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#alter_tablegroup_action}.
	 * @param ctx the parse tree
	 */
	void enterAlter_tablegroup_action(OBParser.Alter_tablegroup_actionContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#alter_tablegroup_action}.
	 * @param ctx the parse tree
	 */
	void exitAlter_tablegroup_action(OBParser.Alter_tablegroup_actionContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#default_tablegroup}.
	 * @param ctx the parse tree
	 */
	void enterDefault_tablegroup(OBParser.Default_tablegroupContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#default_tablegroup}.
	 * @param ctx the parse tree
	 */
	void exitDefault_tablegroup(OBParser.Default_tablegroupContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#create_view_stmt}.
	 * @param ctx the parse tree
	 */
	void enterCreate_view_stmt(OBParser.Create_view_stmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#create_view_stmt}.
	 * @param ctx the parse tree
	 */
	void exitCreate_view_stmt(OBParser.Create_view_stmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#create_mview_stmt}.
	 * @param ctx the parse tree
	 */
	void enterCreate_mview_stmt(OBParser.Create_mview_stmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#create_mview_stmt}.
	 * @param ctx the parse tree
	 */
	void exitCreate_mview_stmt(OBParser.Create_mview_stmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#create_mview_opts}.
	 * @param ctx the parse tree
	 */
	void enterCreate_mview_opts(OBParser.Create_mview_optsContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#create_mview_opts}.
	 * @param ctx the parse tree
	 */
	void exitCreate_mview_opts(OBParser.Create_mview_optsContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#mview_enable_disable}.
	 * @param ctx the parse tree
	 */
	void enterMview_enable_disable(OBParser.Mview_enable_disableContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#mview_enable_disable}.
	 * @param ctx the parse tree
	 */
	void exitMview_enable_disable(OBParser.Mview_enable_disableContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#mview_refresh_opt}.
	 * @param ctx the parse tree
	 */
	void enterMview_refresh_opt(OBParser.Mview_refresh_optContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#mview_refresh_opt}.
	 * @param ctx the parse tree
	 */
	void exitMview_refresh_opt(OBParser.Mview_refresh_optContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#mv_refresh_on_clause}.
	 * @param ctx the parse tree
	 */
	void enterMv_refresh_on_clause(OBParser.Mv_refresh_on_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#mv_refresh_on_clause}.
	 * @param ctx the parse tree
	 */
	void exitMv_refresh_on_clause(OBParser.Mv_refresh_on_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#mv_refresh_method}.
	 * @param ctx the parse tree
	 */
	void enterMv_refresh_method(OBParser.Mv_refresh_methodContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#mv_refresh_method}.
	 * @param ctx the parse tree
	 */
	void exitMv_refresh_method(OBParser.Mv_refresh_methodContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#mv_refresh_mode}.
	 * @param ctx the parse tree
	 */
	void enterMv_refresh_mode(OBParser.Mv_refresh_modeContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#mv_refresh_mode}.
	 * @param ctx the parse tree
	 */
	void exitMv_refresh_mode(OBParser.Mv_refresh_modeContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#mv_refresh_interval}.
	 * @param ctx the parse tree
	 */
	void enterMv_refresh_interval(OBParser.Mv_refresh_intervalContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#mv_refresh_interval}.
	 * @param ctx the parse tree
	 */
	void exitMv_refresh_interval(OBParser.Mv_refresh_intervalContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#mv_start_clause}.
	 * @param ctx the parse tree
	 */
	void enterMv_start_clause(OBParser.Mv_start_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#mv_start_clause}.
	 * @param ctx the parse tree
	 */
	void exitMv_start_clause(OBParser.Mv_start_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#mv_next_clause}.
	 * @param ctx the parse tree
	 */
	void enterMv_next_clause(OBParser.Mv_next_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#mv_next_clause}.
	 * @param ctx the parse tree
	 */
	void exitMv_next_clause(OBParser.Mv_next_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#view_subquery}.
	 * @param ctx the parse tree
	 */
	void enterView_subquery(OBParser.View_subqueryContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#view_subquery}.
	 * @param ctx the parse tree
	 */
	void exitView_subquery(OBParser.View_subqueryContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#view_with_opt}.
	 * @param ctx the parse tree
	 */
	void enterView_with_opt(OBParser.View_with_optContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#view_with_opt}.
	 * @param ctx the parse tree
	 */
	void exitView_with_opt(OBParser.View_with_optContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#with_check_option}.
	 * @param ctx the parse tree
	 */
	void enterWith_check_option(OBParser.With_check_optionContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#with_check_option}.
	 * @param ctx the parse tree
	 */
	void exitWith_check_option(OBParser.With_check_optionContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#view_name}.
	 * @param ctx the parse tree
	 */
	void enterView_name(OBParser.View_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#view_name}.
	 * @param ctx the parse tree
	 */
	void exitView_name(OBParser.View_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#opt_tablet_id}.
	 * @param ctx the parse tree
	 */
	void enterOpt_tablet_id(OBParser.Opt_tablet_idContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#opt_tablet_id}.
	 * @param ctx the parse tree
	 */
	void exitOpt_tablet_id(OBParser.Opt_tablet_idContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#opt_tablet_id_no_empty}.
	 * @param ctx the parse tree
	 */
	void enterOpt_tablet_id_no_empty(OBParser.Opt_tablet_id_no_emptyContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#opt_tablet_id_no_empty}.
	 * @param ctx the parse tree
	 */
	void exitOpt_tablet_id_no_empty(OBParser.Opt_tablet_id_no_emptyContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#create_index_stmt}.
	 * @param ctx the parse tree
	 */
	void enterCreate_index_stmt(OBParser.Create_index_stmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#create_index_stmt}.
	 * @param ctx the parse tree
	 */
	void exitCreate_index_stmt(OBParser.Create_index_stmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#index_name}.
	 * @param ctx the parse tree
	 */
	void enterIndex_name(OBParser.Index_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#index_name}.
	 * @param ctx the parse tree
	 */
	void exitIndex_name(OBParser.Index_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#constraint_and_name}.
	 * @param ctx the parse tree
	 */
	void enterConstraint_and_name(OBParser.Constraint_and_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#constraint_and_name}.
	 * @param ctx the parse tree
	 */
	void exitConstraint_and_name(OBParser.Constraint_and_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#constraint_name}.
	 * @param ctx the parse tree
	 */
	void enterConstraint_name(OBParser.Constraint_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#constraint_name}.
	 * @param ctx the parse tree
	 */
	void exitConstraint_name(OBParser.Constraint_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#sort_column_list}.
	 * @param ctx the parse tree
	 */
	void enterSort_column_list(OBParser.Sort_column_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#sort_column_list}.
	 * @param ctx the parse tree
	 */
	void exitSort_column_list(OBParser.Sort_column_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#sort_column_key}.
	 * @param ctx the parse tree
	 */
	void enterSort_column_key(OBParser.Sort_column_keyContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#sort_column_key}.
	 * @param ctx the parse tree
	 */
	void exitSort_column_key(OBParser.Sort_column_keyContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#index_expr}.
	 * @param ctx the parse tree
	 */
	void enterIndex_expr(OBParser.Index_exprContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#index_expr}.
	 * @param ctx the parse tree
	 */
	void exitIndex_expr(OBParser.Index_exprContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#opt_index_options}.
	 * @param ctx the parse tree
	 */
	void enterOpt_index_options(OBParser.Opt_index_optionsContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#opt_index_options}.
	 * @param ctx the parse tree
	 */
	void exitOpt_index_options(OBParser.Opt_index_optionsContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#index_option}.
	 * @param ctx the parse tree
	 */
	void enterIndex_option(OBParser.Index_optionContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#index_option}.
	 * @param ctx the parse tree
	 */
	void exitIndex_option(OBParser.Index_optionContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#index_using_algorithm}.
	 * @param ctx the parse tree
	 */
	void enterIndex_using_algorithm(OBParser.Index_using_algorithmContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#index_using_algorithm}.
	 * @param ctx the parse tree
	 */
	void exitIndex_using_algorithm(OBParser.Index_using_algorithmContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#create_mlog_stmt}.
	 * @param ctx the parse tree
	 */
	void enterCreate_mlog_stmt(OBParser.Create_mlog_stmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#create_mlog_stmt}.
	 * @param ctx the parse tree
	 */
	void exitCreate_mlog_stmt(OBParser.Create_mlog_stmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#opt_mlog_options}.
	 * @param ctx the parse tree
	 */
	void enterOpt_mlog_options(OBParser.Opt_mlog_optionsContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#opt_mlog_options}.
	 * @param ctx the parse tree
	 */
	void exitOpt_mlog_options(OBParser.Opt_mlog_optionsContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#mlog_option}.
	 * @param ctx the parse tree
	 */
	void enterMlog_option(OBParser.Mlog_optionContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#mlog_option}.
	 * @param ctx the parse tree
	 */
	void exitMlog_option(OBParser.Mlog_optionContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#mlog_with_values}.
	 * @param ctx the parse tree
	 */
	void enterMlog_with_values(OBParser.Mlog_with_valuesContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#mlog_with_values}.
	 * @param ctx the parse tree
	 */
	void exitMlog_with_values(OBParser.Mlog_with_valuesContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#mlog_with_special_columns}.
	 * @param ctx the parse tree
	 */
	void enterMlog_with_special_columns(OBParser.Mlog_with_special_columnsContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#mlog_with_special_columns}.
	 * @param ctx the parse tree
	 */
	void exitMlog_with_special_columns(OBParser.Mlog_with_special_columnsContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#mlog_with_special_column_list}.
	 * @param ctx the parse tree
	 */
	void enterMlog_with_special_column_list(OBParser.Mlog_with_special_column_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#mlog_with_special_column_list}.
	 * @param ctx the parse tree
	 */
	void exitMlog_with_special_column_list(OBParser.Mlog_with_special_column_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#mlog_with_special_column}.
	 * @param ctx the parse tree
	 */
	void enterMlog_with_special_column(OBParser.Mlog_with_special_columnContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#mlog_with_special_column}.
	 * @param ctx the parse tree
	 */
	void exitMlog_with_special_column(OBParser.Mlog_with_special_columnContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#mlog_with_reference_columns}.
	 * @param ctx the parse tree
	 */
	void enterMlog_with_reference_columns(OBParser.Mlog_with_reference_columnsContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#mlog_with_reference_columns}.
	 * @param ctx the parse tree
	 */
	void exitMlog_with_reference_columns(OBParser.Mlog_with_reference_columnsContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#mlog_with_reference_column_list}.
	 * @param ctx the parse tree
	 */
	void enterMlog_with_reference_column_list(OBParser.Mlog_with_reference_column_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#mlog_with_reference_column_list}.
	 * @param ctx the parse tree
	 */
	void exitMlog_with_reference_column_list(OBParser.Mlog_with_reference_column_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#mlog_with_reference_column}.
	 * @param ctx the parse tree
	 */
	void enterMlog_with_reference_column(OBParser.Mlog_with_reference_columnContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#mlog_with_reference_column}.
	 * @param ctx the parse tree
	 */
	void exitMlog_with_reference_column(OBParser.Mlog_with_reference_columnContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#mlog_including_or_excluding}.
	 * @param ctx the parse tree
	 */
	void enterMlog_including_or_excluding(OBParser.Mlog_including_or_excludingContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#mlog_including_or_excluding}.
	 * @param ctx the parse tree
	 */
	void exitMlog_including_or_excluding(OBParser.Mlog_including_or_excludingContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#mlog_purge_values}.
	 * @param ctx the parse tree
	 */
	void enterMlog_purge_values(OBParser.Mlog_purge_valuesContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#mlog_purge_values}.
	 * @param ctx the parse tree
	 */
	void exitMlog_purge_values(OBParser.Mlog_purge_valuesContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#mlog_purge_immediate_sync_or_async}.
	 * @param ctx the parse tree
	 */
	void enterMlog_purge_immediate_sync_or_async(OBParser.Mlog_purge_immediate_sync_or_asyncContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#mlog_purge_immediate_sync_or_async}.
	 * @param ctx the parse tree
	 */
	void exitMlog_purge_immediate_sync_or_async(OBParser.Mlog_purge_immediate_sync_or_asyncContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#mlog_purge_start}.
	 * @param ctx the parse tree
	 */
	void enterMlog_purge_start(OBParser.Mlog_purge_startContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#mlog_purge_start}.
	 * @param ctx the parse tree
	 */
	void exitMlog_purge_start(OBParser.Mlog_purge_startContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#mlog_purge_next}.
	 * @param ctx the parse tree
	 */
	void enterMlog_purge_next(OBParser.Mlog_purge_nextContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#mlog_purge_next}.
	 * @param ctx the parse tree
	 */
	void exitMlog_purge_next(OBParser.Mlog_purge_nextContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#drop_mlog_stmt}.
	 * @param ctx the parse tree
	 */
	void enterDrop_mlog_stmt(OBParser.Drop_mlog_stmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#drop_mlog_stmt}.
	 * @param ctx the parse tree
	 */
	void exitDrop_mlog_stmt(OBParser.Drop_mlog_stmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#drop_table_stmt}.
	 * @param ctx the parse tree
	 */
	void enterDrop_table_stmt(OBParser.Drop_table_stmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#drop_table_stmt}.
	 * @param ctx the parse tree
	 */
	void exitDrop_table_stmt(OBParser.Drop_table_stmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#table_or_tables}.
	 * @param ctx the parse tree
	 */
	void enterTable_or_tables(OBParser.Table_or_tablesContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#table_or_tables}.
	 * @param ctx the parse tree
	 */
	void exitTable_or_tables(OBParser.Table_or_tablesContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#drop_view_stmt}.
	 * @param ctx the parse tree
	 */
	void enterDrop_view_stmt(OBParser.Drop_view_stmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#drop_view_stmt}.
	 * @param ctx the parse tree
	 */
	void exitDrop_view_stmt(OBParser.Drop_view_stmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#table_list}.
	 * @param ctx the parse tree
	 */
	void enterTable_list(OBParser.Table_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#table_list}.
	 * @param ctx the parse tree
	 */
	void exitTable_list(OBParser.Table_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#drop_index_stmt}.
	 * @param ctx the parse tree
	 */
	void enterDrop_index_stmt(OBParser.Drop_index_stmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#drop_index_stmt}.
	 * @param ctx the parse tree
	 */
	void exitDrop_index_stmt(OBParser.Drop_index_stmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#insert_stmt}.
	 * @param ctx the parse tree
	 */
	void enterInsert_stmt(OBParser.Insert_stmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#insert_stmt}.
	 * @param ctx the parse tree
	 */
	void exitInsert_stmt(OBParser.Insert_stmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#opt_simple_expression}.
	 * @param ctx the parse tree
	 */
	void enterOpt_simple_expression(OBParser.Opt_simple_expressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#opt_simple_expression}.
	 * @param ctx the parse tree
	 */
	void exitOpt_simple_expression(OBParser.Opt_simple_expressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#into_err_log_caluse}.
	 * @param ctx the parse tree
	 */
	void enterInto_err_log_caluse(OBParser.Into_err_log_caluseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#into_err_log_caluse}.
	 * @param ctx the parse tree
	 */
	void exitInto_err_log_caluse(OBParser.Into_err_log_caluseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#reject_limit}.
	 * @param ctx the parse tree
	 */
	void enterReject_limit(OBParser.Reject_limitContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#reject_limit}.
	 * @param ctx the parse tree
	 */
	void exitReject_limit(OBParser.Reject_limitContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#single_table_insert}.
	 * @param ctx the parse tree
	 */
	void enterSingle_table_insert(OBParser.Single_table_insertContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#single_table_insert}.
	 * @param ctx the parse tree
	 */
	void exitSingle_table_insert(OBParser.Single_table_insertContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#multi_table_insert}.
	 * @param ctx the parse tree
	 */
	void enterMulti_table_insert(OBParser.Multi_table_insertContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#multi_table_insert}.
	 * @param ctx the parse tree
	 */
	void exitMulti_table_insert(OBParser.Multi_table_insertContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#insert_table_clause_list}.
	 * @param ctx the parse tree
	 */
	void enterInsert_table_clause_list(OBParser.Insert_table_clause_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#insert_table_clause_list}.
	 * @param ctx the parse tree
	 */
	void exitInsert_table_clause_list(OBParser.Insert_table_clause_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#insert_single_table_clause}.
	 * @param ctx the parse tree
	 */
	void enterInsert_single_table_clause(OBParser.Insert_single_table_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#insert_single_table_clause}.
	 * @param ctx the parse tree
	 */
	void exitInsert_single_table_clause(OBParser.Insert_single_table_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#conditional_insert_clause}.
	 * @param ctx the parse tree
	 */
	void enterConditional_insert_clause(OBParser.Conditional_insert_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#conditional_insert_clause}.
	 * @param ctx the parse tree
	 */
	void exitConditional_insert_clause(OBParser.Conditional_insert_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#condition_insert_clause_list}.
	 * @param ctx the parse tree
	 */
	void enterCondition_insert_clause_list(OBParser.Condition_insert_clause_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#condition_insert_clause_list}.
	 * @param ctx the parse tree
	 */
	void exitCondition_insert_clause_list(OBParser.Condition_insert_clause_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#condition_insert_clause}.
	 * @param ctx the parse tree
	 */
	void enterCondition_insert_clause(OBParser.Condition_insert_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#condition_insert_clause}.
	 * @param ctx the parse tree
	 */
	void exitCondition_insert_clause(OBParser.Condition_insert_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#values_clause}.
	 * @param ctx the parse tree
	 */
	void enterValues_clause(OBParser.Values_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#values_clause}.
	 * @param ctx the parse tree
	 */
	void exitValues_clause(OBParser.Values_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#opt_into_clause}.
	 * @param ctx the parse tree
	 */
	void enterOpt_into_clause(OBParser.Opt_into_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#opt_into_clause}.
	 * @param ctx the parse tree
	 */
	void exitOpt_into_clause(OBParser.Opt_into_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#returning_exprs}.
	 * @param ctx the parse tree
	 */
	void enterReturning_exprs(OBParser.Returning_exprsContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#returning_exprs}.
	 * @param ctx the parse tree
	 */
	void exitReturning_exprs(OBParser.Returning_exprsContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#insert_with_opt_hint}.
	 * @param ctx the parse tree
	 */
	void enterInsert_with_opt_hint(OBParser.Insert_with_opt_hintContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#insert_with_opt_hint}.
	 * @param ctx the parse tree
	 */
	void exitInsert_with_opt_hint(OBParser.Insert_with_opt_hintContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#column_list}.
	 * @param ctx the parse tree
	 */
	void enterColumn_list(OBParser.Column_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#column_list}.
	 * @param ctx the parse tree
	 */
	void exitColumn_list(OBParser.Column_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#insert_vals_list}.
	 * @param ctx the parse tree
	 */
	void enterInsert_vals_list(OBParser.Insert_vals_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#insert_vals_list}.
	 * @param ctx the parse tree
	 */
	void exitInsert_vals_list(OBParser.Insert_vals_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#insert_vals}.
	 * @param ctx the parse tree
	 */
	void enterInsert_vals(OBParser.Insert_valsContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#insert_vals}.
	 * @param ctx the parse tree
	 */
	void exitInsert_vals(OBParser.Insert_valsContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#expr_or_default}.
	 * @param ctx the parse tree
	 */
	void enterExpr_or_default(OBParser.Expr_or_defaultContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#expr_or_default}.
	 * @param ctx the parse tree
	 */
	void exitExpr_or_default(OBParser.Expr_or_defaultContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#merge_with_opt_hint}.
	 * @param ctx the parse tree
	 */
	void enterMerge_with_opt_hint(OBParser.Merge_with_opt_hintContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#merge_with_opt_hint}.
	 * @param ctx the parse tree
	 */
	void exitMerge_with_opt_hint(OBParser.Merge_with_opt_hintContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#merge_stmt}.
	 * @param ctx the parse tree
	 */
	void enterMerge_stmt(OBParser.Merge_stmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#merge_stmt}.
	 * @param ctx the parse tree
	 */
	void exitMerge_stmt(OBParser.Merge_stmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#merge_update_clause}.
	 * @param ctx the parse tree
	 */
	void enterMerge_update_clause(OBParser.Merge_update_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#merge_update_clause}.
	 * @param ctx the parse tree
	 */
	void exitMerge_update_clause(OBParser.Merge_update_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#merge_insert_clause}.
	 * @param ctx the parse tree
	 */
	void enterMerge_insert_clause(OBParser.Merge_insert_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#merge_insert_clause}.
	 * @param ctx the parse tree
	 */
	void exitMerge_insert_clause(OBParser.Merge_insert_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#source_relation_factor}.
	 * @param ctx the parse tree
	 */
	void enterSource_relation_factor(OBParser.Source_relation_factorContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#source_relation_factor}.
	 * @param ctx the parse tree
	 */
	void exitSource_relation_factor(OBParser.Source_relation_factorContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#select_stmt}.
	 * @param ctx the parse tree
	 */
	void enterSelect_stmt(OBParser.Select_stmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#select_stmt}.
	 * @param ctx the parse tree
	 */
	void exitSelect_stmt(OBParser.Select_stmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#subquery}.
	 * @param ctx the parse tree
	 */
	void enterSubquery(OBParser.SubqueryContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#subquery}.
	 * @param ctx the parse tree
	 */
	void exitSubquery(OBParser.SubqueryContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#select_with_parens}.
	 * @param ctx the parse tree
	 */
	void enterSelect_with_parens(OBParser.Select_with_parensContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#select_with_parens}.
	 * @param ctx the parse tree
	 */
	void exitSelect_with_parens(OBParser.Select_with_parensContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#select_no_parens}.
	 * @param ctx the parse tree
	 */
	void enterSelect_no_parens(OBParser.Select_no_parensContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#select_no_parens}.
	 * @param ctx the parse tree
	 */
	void exitSelect_no_parens(OBParser.Select_no_parensContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#select_clause}.
	 * @param ctx the parse tree
	 */
	void enterSelect_clause(OBParser.Select_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#select_clause}.
	 * @param ctx the parse tree
	 */
	void exitSelect_clause(OBParser.Select_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#select_clause_set}.
	 * @param ctx the parse tree
	 */
	void enterSelect_clause_set(OBParser.Select_clause_setContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#select_clause_set}.
	 * @param ctx the parse tree
	 */
	void exitSelect_clause_set(OBParser.Select_clause_setContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#select_clause_set_right}.
	 * @param ctx the parse tree
	 */
	void enterSelect_clause_set_right(OBParser.Select_clause_set_rightContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#select_clause_set_right}.
	 * @param ctx the parse tree
	 */
	void exitSelect_clause_set_right(OBParser.Select_clause_set_rightContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#select_clause_set_left}.
	 * @param ctx the parse tree
	 */
	void enterSelect_clause_set_left(OBParser.Select_clause_set_leftContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#select_clause_set_left}.
	 * @param ctx the parse tree
	 */
	void exitSelect_clause_set_left(OBParser.Select_clause_set_leftContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#select_with_opt_hint}.
	 * @param ctx the parse tree
	 */
	void enterSelect_with_opt_hint(OBParser.Select_with_opt_hintContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#select_with_opt_hint}.
	 * @param ctx the parse tree
	 */
	void exitSelect_with_opt_hint(OBParser.Select_with_opt_hintContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#update_with_opt_hint}.
	 * @param ctx the parse tree
	 */
	void enterUpdate_with_opt_hint(OBParser.Update_with_opt_hintContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#update_with_opt_hint}.
	 * @param ctx the parse tree
	 */
	void exitUpdate_with_opt_hint(OBParser.Update_with_opt_hintContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#delete_with_opt_hint}.
	 * @param ctx the parse tree
	 */
	void enterDelete_with_opt_hint(OBParser.Delete_with_opt_hintContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#delete_with_opt_hint}.
	 * @param ctx the parse tree
	 */
	void exitDelete_with_opt_hint(OBParser.Delete_with_opt_hintContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#simple_select}.
	 * @param ctx the parse tree
	 */
	void enterSimple_select(OBParser.Simple_selectContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#simple_select}.
	 * @param ctx the parse tree
	 */
	void exitSimple_select(OBParser.Simple_selectContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#select_with_hierarchical_query}.
	 * @param ctx the parse tree
	 */
	void enterSelect_with_hierarchical_query(OBParser.Select_with_hierarchical_queryContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#select_with_hierarchical_query}.
	 * @param ctx the parse tree
	 */
	void exitSelect_with_hierarchical_query(OBParser.Select_with_hierarchical_queryContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#start_with}.
	 * @param ctx the parse tree
	 */
	void enterStart_with(OBParser.Start_withContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#start_with}.
	 * @param ctx the parse tree
	 */
	void exitStart_with(OBParser.Start_withContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#fetch_next_clause}.
	 * @param ctx the parse tree
	 */
	void enterFetch_next_clause(OBParser.Fetch_next_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#fetch_next_clause}.
	 * @param ctx the parse tree
	 */
	void exitFetch_next_clause(OBParser.Fetch_next_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#fetch_next}.
	 * @param ctx the parse tree
	 */
	void enterFetch_next(OBParser.Fetch_nextContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#fetch_next}.
	 * @param ctx the parse tree
	 */
	void exitFetch_next(OBParser.Fetch_nextContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#fetch_next_count}.
	 * @param ctx the parse tree
	 */
	void enterFetch_next_count(OBParser.Fetch_next_countContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#fetch_next_count}.
	 * @param ctx the parse tree
	 */
	void exitFetch_next_count(OBParser.Fetch_next_countContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#fetch_next_percent}.
	 * @param ctx the parse tree
	 */
	void enterFetch_next_percent(OBParser.Fetch_next_percentContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#fetch_next_percent}.
	 * @param ctx the parse tree
	 */
	void exitFetch_next_percent(OBParser.Fetch_next_percentContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#fetch_next_expr}.
	 * @param ctx the parse tree
	 */
	void enterFetch_next_expr(OBParser.Fetch_next_exprContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#fetch_next_expr}.
	 * @param ctx the parse tree
	 */
	void exitFetch_next_expr(OBParser.Fetch_next_exprContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#fetch_next_percent_expr}.
	 * @param ctx the parse tree
	 */
	void enterFetch_next_percent_expr(OBParser.Fetch_next_percent_exprContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#fetch_next_percent_expr}.
	 * @param ctx the parse tree
	 */
	void exitFetch_next_percent_expr(OBParser.Fetch_next_percent_exprContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#connect_by}.
	 * @param ctx the parse tree
	 */
	void enterConnect_by(OBParser.Connect_byContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#connect_by}.
	 * @param ctx the parse tree
	 */
	void exitConnect_by(OBParser.Connect_byContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#set_type_union}.
	 * @param ctx the parse tree
	 */
	void enterSet_type_union(OBParser.Set_type_unionContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#set_type_union}.
	 * @param ctx the parse tree
	 */
	void exitSet_type_union(OBParser.Set_type_unionContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#set_type_other}.
	 * @param ctx the parse tree
	 */
	void enterSet_type_other(OBParser.Set_type_otherContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#set_type_other}.
	 * @param ctx the parse tree
	 */
	void exitSet_type_other(OBParser.Set_type_otherContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#set_type}.
	 * @param ctx the parse tree
	 */
	void enterSet_type(OBParser.Set_typeContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#set_type}.
	 * @param ctx the parse tree
	 */
	void exitSet_type(OBParser.Set_typeContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#set_expression_option}.
	 * @param ctx the parse tree
	 */
	void enterSet_expression_option(OBParser.Set_expression_optionContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#set_expression_option}.
	 * @param ctx the parse tree
	 */
	void exitSet_expression_option(OBParser.Set_expression_optionContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#opt_where}.
	 * @param ctx the parse tree
	 */
	void enterOpt_where(OBParser.Opt_whereContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#opt_where}.
	 * @param ctx the parse tree
	 */
	void exitOpt_where(OBParser.Opt_whereContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#opt_where_extension}.
	 * @param ctx the parse tree
	 */
	void enterOpt_where_extension(OBParser.Opt_where_extensionContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#opt_where_extension}.
	 * @param ctx the parse tree
	 */
	void exitOpt_where_extension(OBParser.Opt_where_extensionContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#into_clause}.
	 * @param ctx the parse tree
	 */
	void enterInto_clause(OBParser.Into_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#into_clause}.
	 * @param ctx the parse tree
	 */
	void exitInto_clause(OBParser.Into_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#into_opt}.
	 * @param ctx the parse tree
	 */
	void enterInto_opt(OBParser.Into_optContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#into_opt}.
	 * @param ctx the parse tree
	 */
	void exitInto_opt(OBParser.Into_optContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#into_var_list}.
	 * @param ctx the parse tree
	 */
	void enterInto_var_list(OBParser.Into_var_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#into_var_list}.
	 * @param ctx the parse tree
	 */
	void exitInto_var_list(OBParser.Into_var_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#into_var}.
	 * @param ctx the parse tree
	 */
	void enterInto_var(OBParser.Into_varContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#into_var}.
	 * @param ctx the parse tree
	 */
	void exitInto_var(OBParser.Into_varContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#field_opt}.
	 * @param ctx the parse tree
	 */
	void enterField_opt(OBParser.Field_optContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#field_opt}.
	 * @param ctx the parse tree
	 */
	void exitField_opt(OBParser.Field_optContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#field_term_list}.
	 * @param ctx the parse tree
	 */
	void enterField_term_list(OBParser.Field_term_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#field_term_list}.
	 * @param ctx the parse tree
	 */
	void exitField_term_list(OBParser.Field_term_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#field_term}.
	 * @param ctx the parse tree
	 */
	void enterField_term(OBParser.Field_termContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#field_term}.
	 * @param ctx the parse tree
	 */
	void exitField_term(OBParser.Field_termContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#file_opt}.
	 * @param ctx the parse tree
	 */
	void enterFile_opt(OBParser.File_optContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#file_opt}.
	 * @param ctx the parse tree
	 */
	void exitFile_opt(OBParser.File_optContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#file_option_list}.
	 * @param ctx the parse tree
	 */
	void enterFile_option_list(OBParser.File_option_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#file_option_list}.
	 * @param ctx the parse tree
	 */
	void exitFile_option_list(OBParser.File_option_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#file_option}.
	 * @param ctx the parse tree
	 */
	void enterFile_option(OBParser.File_optionContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#file_option}.
	 * @param ctx the parse tree
	 */
	void exitFile_option(OBParser.File_optionContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#file_partition_opt}.
	 * @param ctx the parse tree
	 */
	void enterFile_partition_opt(OBParser.File_partition_optContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#file_partition_opt}.
	 * @param ctx the parse tree
	 */
	void exitFile_partition_opt(OBParser.File_partition_optContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#file_size_const}.
	 * @param ctx the parse tree
	 */
	void enterFile_size_const(OBParser.File_size_constContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#file_size_const}.
	 * @param ctx the parse tree
	 */
	void exitFile_size_const(OBParser.File_size_constContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#line_opt}.
	 * @param ctx the parse tree
	 */
	void enterLine_opt(OBParser.Line_optContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#line_opt}.
	 * @param ctx the parse tree
	 */
	void exitLine_opt(OBParser.Line_optContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#line_term_list}.
	 * @param ctx the parse tree
	 */
	void enterLine_term_list(OBParser.Line_term_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#line_term_list}.
	 * @param ctx the parse tree
	 */
	void exitLine_term_list(OBParser.Line_term_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#line_term}.
	 * @param ctx the parse tree
	 */
	void enterLine_term(OBParser.Line_termContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#line_term}.
	 * @param ctx the parse tree
	 */
	void exitLine_term(OBParser.Line_termContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#hint_list_with_end}.
	 * @param ctx the parse tree
	 */
	void enterHint_list_with_end(OBParser.Hint_list_with_endContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#hint_list_with_end}.
	 * @param ctx the parse tree
	 */
	void exitHint_list_with_end(OBParser.Hint_list_with_endContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#opt_hint_list}.
	 * @param ctx the parse tree
	 */
	void enterOpt_hint_list(OBParser.Opt_hint_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#opt_hint_list}.
	 * @param ctx the parse tree
	 */
	void exitOpt_hint_list(OBParser.Opt_hint_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#hint_options}.
	 * @param ctx the parse tree
	 */
	void enterHint_options(OBParser.Hint_optionsContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#hint_options}.
	 * @param ctx the parse tree
	 */
	void exitHint_options(OBParser.Hint_optionsContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#name_list}.
	 * @param ctx the parse tree
	 */
	void enterName_list(OBParser.Name_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#name_list}.
	 * @param ctx the parse tree
	 */
	void exitName_list(OBParser.Name_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#hint_option}.
	 * @param ctx the parse tree
	 */
	void enterHint_option(OBParser.Hint_optionContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#hint_option}.
	 * @param ctx the parse tree
	 */
	void exitHint_option(OBParser.Hint_optionContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#distribute_method}.
	 * @param ctx the parse tree
	 */
	void enterDistribute_method(OBParser.Distribute_methodContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#distribute_method}.
	 * @param ctx the parse tree
	 */
	void exitDistribute_method(OBParser.Distribute_methodContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#consistency_level}.
	 * @param ctx the parse tree
	 */
	void enterConsistency_level(OBParser.Consistency_levelContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#consistency_level}.
	 * @param ctx the parse tree
	 */
	void exitConsistency_level(OBParser.Consistency_levelContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#use_plan_cache_type}.
	 * @param ctx the parse tree
	 */
	void enterUse_plan_cache_type(OBParser.Use_plan_cache_typeContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#use_plan_cache_type}.
	 * @param ctx the parse tree
	 */
	void exitUse_plan_cache_type(OBParser.Use_plan_cache_typeContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#use_jit_type}.
	 * @param ctx the parse tree
	 */
	void enterUse_jit_type(OBParser.Use_jit_typeContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#use_jit_type}.
	 * @param ctx the parse tree
	 */
	void exitUse_jit_type(OBParser.Use_jit_typeContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#for_update}.
	 * @param ctx the parse tree
	 */
	void enterFor_update(OBParser.For_updateContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#for_update}.
	 * @param ctx the parse tree
	 */
	void exitFor_update(OBParser.For_updateContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#parameterized_trim}.
	 * @param ctx the parse tree
	 */
	void enterParameterized_trim(OBParser.Parameterized_trimContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#parameterized_trim}.
	 * @param ctx the parse tree
	 */
	void exitParameterized_trim(OBParser.Parameterized_trimContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#groupby_clause}.
	 * @param ctx the parse tree
	 */
	void enterGroupby_clause(OBParser.Groupby_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#groupby_clause}.
	 * @param ctx the parse tree
	 */
	void exitGroupby_clause(OBParser.Groupby_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#groupby_element_list}.
	 * @param ctx the parse tree
	 */
	void enterGroupby_element_list(OBParser.Groupby_element_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#groupby_element_list}.
	 * @param ctx the parse tree
	 */
	void exitGroupby_element_list(OBParser.Groupby_element_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#groupby_element}.
	 * @param ctx the parse tree
	 */
	void enterGroupby_element(OBParser.Groupby_elementContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#groupby_element}.
	 * @param ctx the parse tree
	 */
	void exitGroupby_element(OBParser.Groupby_elementContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#group_by_expr}.
	 * @param ctx the parse tree
	 */
	void enterGroup_by_expr(OBParser.Group_by_exprContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#group_by_expr}.
	 * @param ctx the parse tree
	 */
	void exitGroup_by_expr(OBParser.Group_by_exprContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#rollup_clause}.
	 * @param ctx the parse tree
	 */
	void enterRollup_clause(OBParser.Rollup_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#rollup_clause}.
	 * @param ctx the parse tree
	 */
	void exitRollup_clause(OBParser.Rollup_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#cube_clause}.
	 * @param ctx the parse tree
	 */
	void enterCube_clause(OBParser.Cube_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#cube_clause}.
	 * @param ctx the parse tree
	 */
	void exitCube_clause(OBParser.Cube_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#group_by_expr_list}.
	 * @param ctx the parse tree
	 */
	void enterGroup_by_expr_list(OBParser.Group_by_expr_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#group_by_expr_list}.
	 * @param ctx the parse tree
	 */
	void exitGroup_by_expr_list(OBParser.Group_by_expr_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#grouping_sets_clause}.
	 * @param ctx the parse tree
	 */
	void enterGrouping_sets_clause(OBParser.Grouping_sets_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#grouping_sets_clause}.
	 * @param ctx the parse tree
	 */
	void exitGrouping_sets_clause(OBParser.Grouping_sets_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#grouping_sets_list}.
	 * @param ctx the parse tree
	 */
	void enterGrouping_sets_list(OBParser.Grouping_sets_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#grouping_sets_list}.
	 * @param ctx the parse tree
	 */
	void exitGrouping_sets_list(OBParser.Grouping_sets_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#grouping_sets}.
	 * @param ctx the parse tree
	 */
	void enterGrouping_sets(OBParser.Grouping_setsContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#grouping_sets}.
	 * @param ctx the parse tree
	 */
	void exitGrouping_sets(OBParser.Grouping_setsContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#order_by}.
	 * @param ctx the parse tree
	 */
	void enterOrder_by(OBParser.Order_byContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#order_by}.
	 * @param ctx the parse tree
	 */
	void exitOrder_by(OBParser.Order_byContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#sort_list}.
	 * @param ctx the parse tree
	 */
	void enterSort_list(OBParser.Sort_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#sort_list}.
	 * @param ctx the parse tree
	 */
	void exitSort_list(OBParser.Sort_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#sort_key}.
	 * @param ctx the parse tree
	 */
	void enterSort_key(OBParser.Sort_keyContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#sort_key}.
	 * @param ctx the parse tree
	 */
	void exitSort_key(OBParser.Sort_keyContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#opt_null_pos}.
	 * @param ctx the parse tree
	 */
	void enterOpt_null_pos(OBParser.Opt_null_posContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#opt_null_pos}.
	 * @param ctx the parse tree
	 */
	void exitOpt_null_pos(OBParser.Opt_null_posContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#opt_ascending_type}.
	 * @param ctx the parse tree
	 */
	void enterOpt_ascending_type(OBParser.Opt_ascending_typeContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#opt_ascending_type}.
	 * @param ctx the parse tree
	 */
	void exitOpt_ascending_type(OBParser.Opt_ascending_typeContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#opt_asc_desc}.
	 * @param ctx the parse tree
	 */
	void enterOpt_asc_desc(OBParser.Opt_asc_descContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#opt_asc_desc}.
	 * @param ctx the parse tree
	 */
	void exitOpt_asc_desc(OBParser.Opt_asc_descContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#query_expression_option_list}.
	 * @param ctx the parse tree
	 */
	void enterQuery_expression_option_list(OBParser.Query_expression_option_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#query_expression_option_list}.
	 * @param ctx the parse tree
	 */
	void exitQuery_expression_option_list(OBParser.Query_expression_option_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#query_expression_option}.
	 * @param ctx the parse tree
	 */
	void enterQuery_expression_option(OBParser.Query_expression_optionContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#query_expression_option}.
	 * @param ctx the parse tree
	 */
	void exitQuery_expression_option(OBParser.Query_expression_optionContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#projection}.
	 * @param ctx the parse tree
	 */
	void enterProjection(OBParser.ProjectionContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#projection}.
	 * @param ctx the parse tree
	 */
	void exitProjection(OBParser.ProjectionContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#opt_as}.
	 * @param ctx the parse tree
	 */
	void enterOpt_as(OBParser.Opt_asContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#opt_as}.
	 * @param ctx the parse tree
	 */
	void exitOpt_as(OBParser.Opt_asContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#select_expr_list}.
	 * @param ctx the parse tree
	 */
	void enterSelect_expr_list(OBParser.Select_expr_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#select_expr_list}.
	 * @param ctx the parse tree
	 */
	void exitSelect_expr_list(OBParser.Select_expr_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#from_list}.
	 * @param ctx the parse tree
	 */
	void enterFrom_list(OBParser.From_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#from_list}.
	 * @param ctx the parse tree
	 */
	void exitFrom_list(OBParser.From_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#table_references}.
	 * @param ctx the parse tree
	 */
	void enterTable_references(OBParser.Table_referencesContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#table_references}.
	 * @param ctx the parse tree
	 */
	void exitTable_references(OBParser.Table_referencesContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#table_reference}.
	 * @param ctx the parse tree
	 */
	void enterTable_reference(OBParser.Table_referenceContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#table_reference}.
	 * @param ctx the parse tree
	 */
	void exitTable_reference(OBParser.Table_referenceContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#table_factor}.
	 * @param ctx the parse tree
	 */
	void enterTable_factor(OBParser.Table_factorContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#table_factor}.
	 * @param ctx the parse tree
	 */
	void exitTable_factor(OBParser.Table_factorContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#select_function}.
	 * @param ctx the parse tree
	 */
	void enterSelect_function(OBParser.Select_functionContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#select_function}.
	 * @param ctx the parse tree
	 */
	void exitSelect_function(OBParser.Select_functionContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#tbl_name}.
	 * @param ctx the parse tree
	 */
	void enterTbl_name(OBParser.Tbl_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#tbl_name}.
	 * @param ctx the parse tree
	 */
	void exitTbl_name(OBParser.Tbl_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#dual_table}.
	 * @param ctx the parse tree
	 */
	void enterDual_table(OBParser.Dual_tableContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#dual_table}.
	 * @param ctx the parse tree
	 */
	void exitDual_table(OBParser.Dual_tableContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#transpose_clause}.
	 * @param ctx the parse tree
	 */
	void enterTranspose_clause(OBParser.Transpose_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#transpose_clause}.
	 * @param ctx the parse tree
	 */
	void exitTranspose_clause(OBParser.Transpose_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#pivot_aggr_clause}.
	 * @param ctx the parse tree
	 */
	void enterPivot_aggr_clause(OBParser.Pivot_aggr_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#pivot_aggr_clause}.
	 * @param ctx the parse tree
	 */
	void exitPivot_aggr_clause(OBParser.Pivot_aggr_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#pivot_single_aggr_clause}.
	 * @param ctx the parse tree
	 */
	void enterPivot_single_aggr_clause(OBParser.Pivot_single_aggr_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#pivot_single_aggr_clause}.
	 * @param ctx the parse tree
	 */
	void exitPivot_single_aggr_clause(OBParser.Pivot_single_aggr_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#transpose_for_clause}.
	 * @param ctx the parse tree
	 */
	void enterTranspose_for_clause(OBParser.Transpose_for_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#transpose_for_clause}.
	 * @param ctx the parse tree
	 */
	void exitTranspose_for_clause(OBParser.Transpose_for_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#transpose_in_clause}.
	 * @param ctx the parse tree
	 */
	void enterTranspose_in_clause(OBParser.Transpose_in_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#transpose_in_clause}.
	 * @param ctx the parse tree
	 */
	void exitTranspose_in_clause(OBParser.Transpose_in_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#transpose_in_args}.
	 * @param ctx the parse tree
	 */
	void enterTranspose_in_args(OBParser.Transpose_in_argsContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#transpose_in_args}.
	 * @param ctx the parse tree
	 */
	void exitTranspose_in_args(OBParser.Transpose_in_argsContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#transpose_in_arg}.
	 * @param ctx the parse tree
	 */
	void enterTranspose_in_arg(OBParser.Transpose_in_argContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#transpose_in_arg}.
	 * @param ctx the parse tree
	 */
	void exitTranspose_in_arg(OBParser.Transpose_in_argContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#unpivot_column_clause}.
	 * @param ctx the parse tree
	 */
	void enterUnpivot_column_clause(OBParser.Unpivot_column_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#unpivot_column_clause}.
	 * @param ctx the parse tree
	 */
	void exitUnpivot_column_clause(OBParser.Unpivot_column_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#unpivot_in_clause}.
	 * @param ctx the parse tree
	 */
	void enterUnpivot_in_clause(OBParser.Unpivot_in_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#unpivot_in_clause}.
	 * @param ctx the parse tree
	 */
	void exitUnpivot_in_clause(OBParser.Unpivot_in_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#unpivot_in_args}.
	 * @param ctx the parse tree
	 */
	void enterUnpivot_in_args(OBParser.Unpivot_in_argsContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#unpivot_in_args}.
	 * @param ctx the parse tree
	 */
	void exitUnpivot_in_args(OBParser.Unpivot_in_argsContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#unpivot_in_arg}.
	 * @param ctx the parse tree
	 */
	void enterUnpivot_in_arg(OBParser.Unpivot_in_argContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#unpivot_in_arg}.
	 * @param ctx the parse tree
	 */
	void exitUnpivot_in_arg(OBParser.Unpivot_in_argContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#dml_table_name}.
	 * @param ctx the parse tree
	 */
	void enterDml_table_name(OBParser.Dml_table_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#dml_table_name}.
	 * @param ctx the parse tree
	 */
	void exitDml_table_name(OBParser.Dml_table_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#order_by_fetch_with_check_option}.
	 * @param ctx the parse tree
	 */
	void enterOrder_by_fetch_with_check_option(OBParser.Order_by_fetch_with_check_optionContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#order_by_fetch_with_check_option}.
	 * @param ctx the parse tree
	 */
	void exitOrder_by_fetch_with_check_option(OBParser.Order_by_fetch_with_check_optionContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#insert_table_clause}.
	 * @param ctx the parse tree
	 */
	void enterInsert_table_clause(OBParser.Insert_table_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#insert_table_clause}.
	 * @param ctx the parse tree
	 */
	void exitInsert_table_clause(OBParser.Insert_table_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#dml_table_clause}.
	 * @param ctx the parse tree
	 */
	void enterDml_table_clause(OBParser.Dml_table_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#dml_table_clause}.
	 * @param ctx the parse tree
	 */
	void exitDml_table_clause(OBParser.Dml_table_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#seed}.
	 * @param ctx the parse tree
	 */
	void enterSeed(OBParser.SeedContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#seed}.
	 * @param ctx the parse tree
	 */
	void exitSeed(OBParser.SeedContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#sample_percent}.
	 * @param ctx the parse tree
	 */
	void enterSample_percent(OBParser.Sample_percentContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#sample_percent}.
	 * @param ctx the parse tree
	 */
	void exitSample_percent(OBParser.Sample_percentContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#sample_clause}.
	 * @param ctx the parse tree
	 */
	void enterSample_clause(OBParser.Sample_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#sample_clause}.
	 * @param ctx the parse tree
	 */
	void exitSample_clause(OBParser.Sample_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#table_subquery}.
	 * @param ctx the parse tree
	 */
	void enterTable_subquery(OBParser.Table_subqueryContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#table_subquery}.
	 * @param ctx the parse tree
	 */
	void exitTable_subquery(OBParser.Table_subqueryContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#use_partition}.
	 * @param ctx the parse tree
	 */
	void enterUse_partition(OBParser.Use_partitionContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#use_partition}.
	 * @param ctx the parse tree
	 */
	void exitUse_partition(OBParser.Use_partitionContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#external_table_partitions}.
	 * @param ctx the parse tree
	 */
	void enterExternal_table_partitions(OBParser.External_table_partitionsContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#external_table_partitions}.
	 * @param ctx the parse tree
	 */
	void exitExternal_table_partitions(OBParser.External_table_partitionsContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#external_table_partition}.
	 * @param ctx the parse tree
	 */
	void enterExternal_table_partition(OBParser.External_table_partitionContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#external_table_partition}.
	 * @param ctx the parse tree
	 */
	void exitExternal_table_partition(OBParser.External_table_partitionContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#use_flashback}.
	 * @param ctx the parse tree
	 */
	void enterUse_flashback(OBParser.Use_flashbackContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#use_flashback}.
	 * @param ctx the parse tree
	 */
	void exitUse_flashback(OBParser.Use_flashbackContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#relation_factor}.
	 * @param ctx the parse tree
	 */
	void enterRelation_factor(OBParser.Relation_factorContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#relation_factor}.
	 * @param ctx the parse tree
	 */
	void exitRelation_factor(OBParser.Relation_factorContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#normal_relation_factor}.
	 * @param ctx the parse tree
	 */
	void enterNormal_relation_factor(OBParser.Normal_relation_factorContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#normal_relation_factor}.
	 * @param ctx the parse tree
	 */
	void exitNormal_relation_factor(OBParser.Normal_relation_factorContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#dot_relation_factor}.
	 * @param ctx the parse tree
	 */
	void enterDot_relation_factor(OBParser.Dot_relation_factorContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#dot_relation_factor}.
	 * @param ctx the parse tree
	 */
	void exitDot_relation_factor(OBParser.Dot_relation_factorContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#opt_reverse_link_flag}.
	 * @param ctx the parse tree
	 */
	void enterOpt_reverse_link_flag(OBParser.Opt_reverse_link_flagContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#opt_reverse_link_flag}.
	 * @param ctx the parse tree
	 */
	void exitOpt_reverse_link_flag(OBParser.Opt_reverse_link_flagContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#relation_factor_in_hint}.
	 * @param ctx the parse tree
	 */
	void enterRelation_factor_in_hint(OBParser.Relation_factor_in_hintContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#relation_factor_in_hint}.
	 * @param ctx the parse tree
	 */
	void exitRelation_factor_in_hint(OBParser.Relation_factor_in_hintContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#qb_name_option}.
	 * @param ctx the parse tree
	 */
	void enterQb_name_option(OBParser.Qb_name_optionContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#qb_name_option}.
	 * @param ctx the parse tree
	 */
	void exitQb_name_option(OBParser.Qb_name_optionContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#relation_factor_in_hint_list}.
	 * @param ctx the parse tree
	 */
	void enterRelation_factor_in_hint_list(OBParser.Relation_factor_in_hint_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#relation_factor_in_hint_list}.
	 * @param ctx the parse tree
	 */
	void exitRelation_factor_in_hint_list(OBParser.Relation_factor_in_hint_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#relation_sep_option}.
	 * @param ctx the parse tree
	 */
	void enterRelation_sep_option(OBParser.Relation_sep_optionContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#relation_sep_option}.
	 * @param ctx the parse tree
	 */
	void exitRelation_sep_option(OBParser.Relation_sep_optionContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#relation_factor_in_mv_hint_list}.
	 * @param ctx the parse tree
	 */
	void enterRelation_factor_in_mv_hint_list(OBParser.Relation_factor_in_mv_hint_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#relation_factor_in_mv_hint_list}.
	 * @param ctx the parse tree
	 */
	void exitRelation_factor_in_mv_hint_list(OBParser.Relation_factor_in_mv_hint_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#relation_factor_in_pq_hint}.
	 * @param ctx the parse tree
	 */
	void enterRelation_factor_in_pq_hint(OBParser.Relation_factor_in_pq_hintContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#relation_factor_in_pq_hint}.
	 * @param ctx the parse tree
	 */
	void exitRelation_factor_in_pq_hint(OBParser.Relation_factor_in_pq_hintContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#relation_factor_in_leading_hint}.
	 * @param ctx the parse tree
	 */
	void enterRelation_factor_in_leading_hint(OBParser.Relation_factor_in_leading_hintContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#relation_factor_in_leading_hint}.
	 * @param ctx the parse tree
	 */
	void exitRelation_factor_in_leading_hint(OBParser.Relation_factor_in_leading_hintContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#tracing_num_list}.
	 * @param ctx the parse tree
	 */
	void enterTracing_num_list(OBParser.Tracing_num_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#tracing_num_list}.
	 * @param ctx the parse tree
	 */
	void exitTracing_num_list(OBParser.Tracing_num_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#relation_factor_in_leading_hint_list}.
	 * @param ctx the parse tree
	 */
	void enterRelation_factor_in_leading_hint_list(OBParser.Relation_factor_in_leading_hint_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#relation_factor_in_leading_hint_list}.
	 * @param ctx the parse tree
	 */
	void exitRelation_factor_in_leading_hint_list(OBParser.Relation_factor_in_leading_hint_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#relation_factor_in_leading_hint_list_entry}.
	 * @param ctx the parse tree
	 */
	void enterRelation_factor_in_leading_hint_list_entry(OBParser.Relation_factor_in_leading_hint_list_entryContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#relation_factor_in_leading_hint_list_entry}.
	 * @param ctx the parse tree
	 */
	void exitRelation_factor_in_leading_hint_list_entry(OBParser.Relation_factor_in_leading_hint_list_entryContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#relation_factor_in_use_join_hint_list}.
	 * @param ctx the parse tree
	 */
	void enterRelation_factor_in_use_join_hint_list(OBParser.Relation_factor_in_use_join_hint_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#relation_factor_in_use_join_hint_list}.
	 * @param ctx the parse tree
	 */
	void exitRelation_factor_in_use_join_hint_list(OBParser.Relation_factor_in_use_join_hint_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#coalesce_strategy_list}.
	 * @param ctx the parse tree
	 */
	void enterCoalesce_strategy_list(OBParser.Coalesce_strategy_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#coalesce_strategy_list}.
	 * @param ctx the parse tree
	 */
	void exitCoalesce_strategy_list(OBParser.Coalesce_strategy_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#join_condition}.
	 * @param ctx the parse tree
	 */
	void enterJoin_condition(OBParser.Join_conditionContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#join_condition}.
	 * @param ctx the parse tree
	 */
	void exitJoin_condition(OBParser.Join_conditionContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#joined_table}.
	 * @param ctx the parse tree
	 */
	void enterJoined_table(OBParser.Joined_tableContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#joined_table}.
	 * @param ctx the parse tree
	 */
	void exitJoined_table(OBParser.Joined_tableContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#natural_join_type}.
	 * @param ctx the parse tree
	 */
	void enterNatural_join_type(OBParser.Natural_join_typeContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#natural_join_type}.
	 * @param ctx the parse tree
	 */
	void exitNatural_join_type(OBParser.Natural_join_typeContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#outer_join_type}.
	 * @param ctx the parse tree
	 */
	void enterOuter_join_type(OBParser.Outer_join_typeContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#outer_join_type}.
	 * @param ctx the parse tree
	 */
	void exitOuter_join_type(OBParser.Outer_join_typeContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#join_outer}.
	 * @param ctx the parse tree
	 */
	void enterJoin_outer(OBParser.Join_outerContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#join_outer}.
	 * @param ctx the parse tree
	 */
	void exitJoin_outer(OBParser.Join_outerContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#with_select}.
	 * @param ctx the parse tree
	 */
	void enterWith_select(OBParser.With_selectContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#with_select}.
	 * @param ctx the parse tree
	 */
	void exitWith_select(OBParser.With_selectContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#with_clause}.
	 * @param ctx the parse tree
	 */
	void enterWith_clause(OBParser.With_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#with_clause}.
	 * @param ctx the parse tree
	 */
	void exitWith_clause(OBParser.With_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#with_list}.
	 * @param ctx the parse tree
	 */
	void enterWith_list(OBParser.With_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#with_list}.
	 * @param ctx the parse tree
	 */
	void exitWith_list(OBParser.With_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#common_table_expr}.
	 * @param ctx the parse tree
	 */
	void enterCommon_table_expr(OBParser.Common_table_exprContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#common_table_expr}.
	 * @param ctx the parse tree
	 */
	void exitCommon_table_expr(OBParser.Common_table_exprContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#mv_column_list}.
	 * @param ctx the parse tree
	 */
	void enterMv_column_list(OBParser.Mv_column_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#mv_column_list}.
	 * @param ctx the parse tree
	 */
	void exitMv_column_list(OBParser.Mv_column_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#alias_name_list}.
	 * @param ctx the parse tree
	 */
	void enterAlias_name_list(OBParser.Alias_name_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#alias_name_list}.
	 * @param ctx the parse tree
	 */
	void exitAlias_name_list(OBParser.Alias_name_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#column_alias_name}.
	 * @param ctx the parse tree
	 */
	void enterColumn_alias_name(OBParser.Column_alias_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#column_alias_name}.
	 * @param ctx the parse tree
	 */
	void exitColumn_alias_name(OBParser.Column_alias_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#search_set_value}.
	 * @param ctx the parse tree
	 */
	void enterSearch_set_value(OBParser.Search_set_valueContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#search_set_value}.
	 * @param ctx the parse tree
	 */
	void exitSearch_set_value(OBParser.Search_set_valueContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#analyze_stmt}.
	 * @param ctx the parse tree
	 */
	void enterAnalyze_stmt(OBParser.Analyze_stmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#analyze_stmt}.
	 * @param ctx the parse tree
	 */
	void exitAnalyze_stmt(OBParser.Analyze_stmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#analyze_statistics_clause}.
	 * @param ctx the parse tree
	 */
	void enterAnalyze_statistics_clause(OBParser.Analyze_statistics_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#analyze_statistics_clause}.
	 * @param ctx the parse tree
	 */
	void exitAnalyze_statistics_clause(OBParser.Analyze_statistics_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#opt_analyze_for_clause_list}.
	 * @param ctx the parse tree
	 */
	void enterOpt_analyze_for_clause_list(OBParser.Opt_analyze_for_clause_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#opt_analyze_for_clause_list}.
	 * @param ctx the parse tree
	 */
	void exitOpt_analyze_for_clause_list(OBParser.Opt_analyze_for_clause_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#opt_analyze_for_clause_element}.
	 * @param ctx the parse tree
	 */
	void enterOpt_analyze_for_clause_element(OBParser.Opt_analyze_for_clause_elementContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#opt_analyze_for_clause_element}.
	 * @param ctx the parse tree
	 */
	void exitOpt_analyze_for_clause_element(OBParser.Opt_analyze_for_clause_elementContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#sample_option}.
	 * @param ctx the parse tree
	 */
	void enterSample_option(OBParser.Sample_optionContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#sample_option}.
	 * @param ctx the parse tree
	 */
	void exitSample_option(OBParser.Sample_optionContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#create_outline_stmt}.
	 * @param ctx the parse tree
	 */
	void enterCreate_outline_stmt(OBParser.Create_outline_stmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#create_outline_stmt}.
	 * @param ctx the parse tree
	 */
	void exitCreate_outline_stmt(OBParser.Create_outline_stmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#alter_outline_stmt}.
	 * @param ctx the parse tree
	 */
	void enterAlter_outline_stmt(OBParser.Alter_outline_stmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#alter_outline_stmt}.
	 * @param ctx the parse tree
	 */
	void exitAlter_outline_stmt(OBParser.Alter_outline_stmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#drop_outline_stmt}.
	 * @param ctx the parse tree
	 */
	void enterDrop_outline_stmt(OBParser.Drop_outline_stmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#drop_outline_stmt}.
	 * @param ctx the parse tree
	 */
	void exitDrop_outline_stmt(OBParser.Drop_outline_stmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#explain_stmt}.
	 * @param ctx the parse tree
	 */
	void enterExplain_stmt(OBParser.Explain_stmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#explain_stmt}.
	 * @param ctx the parse tree
	 */
	void exitExplain_stmt(OBParser.Explain_stmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#explain_or_desc}.
	 * @param ctx the parse tree
	 */
	void enterExplain_or_desc(OBParser.Explain_or_descContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#explain_or_desc}.
	 * @param ctx the parse tree
	 */
	void exitExplain_or_desc(OBParser.Explain_or_descContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#explainable_stmt}.
	 * @param ctx the parse tree
	 */
	void enterExplainable_stmt(OBParser.Explainable_stmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#explainable_stmt}.
	 * @param ctx the parse tree
	 */
	void exitExplainable_stmt(OBParser.Explainable_stmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#format_name}.
	 * @param ctx the parse tree
	 */
	void enterFormat_name(OBParser.Format_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#format_name}.
	 * @param ctx the parse tree
	 */
	void exitFormat_name(OBParser.Format_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#show_stmt}.
	 * @param ctx the parse tree
	 */
	void enterShow_stmt(OBParser.Show_stmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#show_stmt}.
	 * @param ctx the parse tree
	 */
	void exitShow_stmt(OBParser.Show_stmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#opt_for_grant_user}.
	 * @param ctx the parse tree
	 */
	void enterOpt_for_grant_user(OBParser.Opt_for_grant_userContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#opt_for_grant_user}.
	 * @param ctx the parse tree
	 */
	void exitOpt_for_grant_user(OBParser.Opt_for_grant_userContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#columns_or_fields}.
	 * @param ctx the parse tree
	 */
	void enterColumns_or_fields(OBParser.Columns_or_fieldsContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#columns_or_fields}.
	 * @param ctx the parse tree
	 */
	void exitColumns_or_fields(OBParser.Columns_or_fieldsContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#from_or_in}.
	 * @param ctx the parse tree
	 */
	void enterFrom_or_in(OBParser.From_or_inContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#from_or_in}.
	 * @param ctx the parse tree
	 */
	void exitFrom_or_in(OBParser.From_or_inContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#help_stmt}.
	 * @param ctx the parse tree
	 */
	void enterHelp_stmt(OBParser.Help_stmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#help_stmt}.
	 * @param ctx the parse tree
	 */
	void exitHelp_stmt(OBParser.Help_stmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#create_user_stmt}.
	 * @param ctx the parse tree
	 */
	void enterCreate_user_stmt(OBParser.Create_user_stmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#create_user_stmt}.
	 * @param ctx the parse tree
	 */
	void exitCreate_user_stmt(OBParser.Create_user_stmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#default_role_clause}.
	 * @param ctx the parse tree
	 */
	void enterDefault_role_clause(OBParser.Default_role_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#default_role_clause}.
	 * @param ctx the parse tree
	 */
	void exitDefault_role_clause(OBParser.Default_role_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#alter_user_stmt}.
	 * @param ctx the parse tree
	 */
	void enterAlter_user_stmt(OBParser.Alter_user_stmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#alter_user_stmt}.
	 * @param ctx the parse tree
	 */
	void exitAlter_user_stmt(OBParser.Alter_user_stmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#alter_user_profile_stmt}.
	 * @param ctx the parse tree
	 */
	void enterAlter_user_profile_stmt(OBParser.Alter_user_profile_stmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#alter_user_profile_stmt}.
	 * @param ctx the parse tree
	 */
	void exitAlter_user_profile_stmt(OBParser.Alter_user_profile_stmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#alter_role_stmt}.
	 * @param ctx the parse tree
	 */
	void enterAlter_role_stmt(OBParser.Alter_role_stmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#alter_role_stmt}.
	 * @param ctx the parse tree
	 */
	void exitAlter_role_stmt(OBParser.Alter_role_stmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#user_specification}.
	 * @param ctx the parse tree
	 */
	void enterUser_specification(OBParser.User_specificationContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#user_specification}.
	 * @param ctx the parse tree
	 */
	void exitUser_specification(OBParser.User_specificationContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#require_specification}.
	 * @param ctx the parse tree
	 */
	void enterRequire_specification(OBParser.Require_specificationContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#require_specification}.
	 * @param ctx the parse tree
	 */
	void exitRequire_specification(OBParser.Require_specificationContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#tls_option_list}.
	 * @param ctx the parse tree
	 */
	void enterTls_option_list(OBParser.Tls_option_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#tls_option_list}.
	 * @param ctx the parse tree
	 */
	void exitTls_option_list(OBParser.Tls_option_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#tls_option}.
	 * @param ctx the parse tree
	 */
	void enterTls_option(OBParser.Tls_optionContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#tls_option}.
	 * @param ctx the parse tree
	 */
	void exitTls_option(OBParser.Tls_optionContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#grant_user}.
	 * @param ctx the parse tree
	 */
	void enterGrant_user(OBParser.Grant_userContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#grant_user}.
	 * @param ctx the parse tree
	 */
	void exitGrant_user(OBParser.Grant_userContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#grant_user_list}.
	 * @param ctx the parse tree
	 */
	void enterGrant_user_list(OBParser.Grant_user_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#grant_user_list}.
	 * @param ctx the parse tree
	 */
	void exitGrant_user_list(OBParser.Grant_user_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#user}.
	 * @param ctx the parse tree
	 */
	void enterUser(OBParser.UserContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#user}.
	 * @param ctx the parse tree
	 */
	void exitUser(OBParser.UserContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#opt_host_name}.
	 * @param ctx the parse tree
	 */
	void enterOpt_host_name(OBParser.Opt_host_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#opt_host_name}.
	 * @param ctx the parse tree
	 */
	void exitOpt_host_name(OBParser.Opt_host_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#user_with_host_name}.
	 * @param ctx the parse tree
	 */
	void enterUser_with_host_name(OBParser.User_with_host_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#user_with_host_name}.
	 * @param ctx the parse tree
	 */
	void exitUser_with_host_name(OBParser.User_with_host_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#password}.
	 * @param ctx the parse tree
	 */
	void enterPassword(OBParser.PasswordContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#password}.
	 * @param ctx the parse tree
	 */
	void exitPassword(OBParser.PasswordContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#password_str}.
	 * @param ctx the parse tree
	 */
	void enterPassword_str(OBParser.Password_strContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#password_str}.
	 * @param ctx the parse tree
	 */
	void exitPassword_str(OBParser.Password_strContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#drop_user_stmt}.
	 * @param ctx the parse tree
	 */
	void enterDrop_user_stmt(OBParser.Drop_user_stmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#drop_user_stmt}.
	 * @param ctx the parse tree
	 */
	void exitDrop_user_stmt(OBParser.Drop_user_stmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#user_list}.
	 * @param ctx the parse tree
	 */
	void enterUser_list(OBParser.User_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#user_list}.
	 * @param ctx the parse tree
	 */
	void exitUser_list(OBParser.User_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#set_password_stmt}.
	 * @param ctx the parse tree
	 */
	void enterSet_password_stmt(OBParser.Set_password_stmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#set_password_stmt}.
	 * @param ctx the parse tree
	 */
	void exitSet_password_stmt(OBParser.Set_password_stmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#opt_for_user}.
	 * @param ctx the parse tree
	 */
	void enterOpt_for_user(OBParser.Opt_for_userContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#opt_for_user}.
	 * @param ctx the parse tree
	 */
	void exitOpt_for_user(OBParser.Opt_for_userContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#lock_user_stmt}.
	 * @param ctx the parse tree
	 */
	void enterLock_user_stmt(OBParser.Lock_user_stmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#lock_user_stmt}.
	 * @param ctx the parse tree
	 */
	void exitLock_user_stmt(OBParser.Lock_user_stmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#lock_spec_mysql57}.
	 * @param ctx the parse tree
	 */
	void enterLock_spec_mysql57(OBParser.Lock_spec_mysql57Context ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#lock_spec_mysql57}.
	 * @param ctx the parse tree
	 */
	void exitLock_spec_mysql57(OBParser.Lock_spec_mysql57Context ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#lock_tables_stmt}.
	 * @param ctx the parse tree
	 */
	void enterLock_tables_stmt(OBParser.Lock_tables_stmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#lock_tables_stmt}.
	 * @param ctx the parse tree
	 */
	void exitLock_tables_stmt(OBParser.Lock_tables_stmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#lock_table_stmt}.
	 * @param ctx the parse tree
	 */
	void enterLock_table_stmt(OBParser.Lock_table_stmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#lock_table_stmt}.
	 * @param ctx the parse tree
	 */
	void exitLock_table_stmt(OBParser.Lock_table_stmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#lock_table_factors}.
	 * @param ctx the parse tree
	 */
	void enterLock_table_factors(OBParser.Lock_table_factorsContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#lock_table_factors}.
	 * @param ctx the parse tree
	 */
	void exitLock_table_factors(OBParser.Lock_table_factorsContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#lock_table_factor}.
	 * @param ctx the parse tree
	 */
	void enterLock_table_factor(OBParser.Lock_table_factorContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#lock_table_factor}.
	 * @param ctx the parse tree
	 */
	void exitLock_table_factor(OBParser.Lock_table_factorContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#lock_mode}.
	 * @param ctx the parse tree
	 */
	void enterLock_mode(OBParser.Lock_modeContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#lock_mode}.
	 * @param ctx the parse tree
	 */
	void exitLock_mode(OBParser.Lock_modeContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#unlock_tables_stmt}.
	 * @param ctx the parse tree
	 */
	void enterUnlock_tables_stmt(OBParser.Unlock_tables_stmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#unlock_tables_stmt}.
	 * @param ctx the parse tree
	 */
	void exitUnlock_tables_stmt(OBParser.Unlock_tables_stmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#lock_table_list}.
	 * @param ctx the parse tree
	 */
	void enterLock_table_list(OBParser.Lock_table_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#lock_table_list}.
	 * @param ctx the parse tree
	 */
	void exitLock_table_list(OBParser.Lock_table_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#create_context_stmt}.
	 * @param ctx the parse tree
	 */
	void enterCreate_context_stmt(OBParser.Create_context_stmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#create_context_stmt}.
	 * @param ctx the parse tree
	 */
	void exitCreate_context_stmt(OBParser.Create_context_stmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#context_package_name}.
	 * @param ctx the parse tree
	 */
	void enterContext_package_name(OBParser.Context_package_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#context_package_name}.
	 * @param ctx the parse tree
	 */
	void exitContext_package_name(OBParser.Context_package_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#lock_table}.
	 * @param ctx the parse tree
	 */
	void enterLock_table(OBParser.Lock_tableContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#lock_table}.
	 * @param ctx the parse tree
	 */
	void exitLock_table(OBParser.Lock_tableContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#context_option}.
	 * @param ctx the parse tree
	 */
	void enterContext_option(OBParser.Context_optionContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#context_option}.
	 * @param ctx the parse tree
	 */
	void exitContext_option(OBParser.Context_optionContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#lock_type}.
	 * @param ctx the parse tree
	 */
	void enterLock_type(OBParser.Lock_typeContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#lock_type}.
	 * @param ctx the parse tree
	 */
	void exitLock_type(OBParser.Lock_typeContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#drop_context_stmt}.
	 * @param ctx the parse tree
	 */
	void enterDrop_context_stmt(OBParser.Drop_context_stmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#drop_context_stmt}.
	 * @param ctx the parse tree
	 */
	void exitDrop_context_stmt(OBParser.Drop_context_stmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#create_sequence_stmt}.
	 * @param ctx the parse tree
	 */
	void enterCreate_sequence_stmt(OBParser.Create_sequence_stmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#create_sequence_stmt}.
	 * @param ctx the parse tree
	 */
	void exitCreate_sequence_stmt(OBParser.Create_sequence_stmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#sequence_option_list}.
	 * @param ctx the parse tree
	 */
	void enterSequence_option_list(OBParser.Sequence_option_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#sequence_option_list}.
	 * @param ctx the parse tree
	 */
	void exitSequence_option_list(OBParser.Sequence_option_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#sequence_option}.
	 * @param ctx the parse tree
	 */
	void enterSequence_option(OBParser.Sequence_optionContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#sequence_option}.
	 * @param ctx the parse tree
	 */
	void exitSequence_option(OBParser.Sequence_optionContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#simple_num}.
	 * @param ctx the parse tree
	 */
	void enterSimple_num(OBParser.Simple_numContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#simple_num}.
	 * @param ctx the parse tree
	 */
	void exitSimple_num(OBParser.Simple_numContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#drop_sequence_stmt}.
	 * @param ctx the parse tree
	 */
	void enterDrop_sequence_stmt(OBParser.Drop_sequence_stmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#drop_sequence_stmt}.
	 * @param ctx the parse tree
	 */
	void exitDrop_sequence_stmt(OBParser.Drop_sequence_stmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#alter_sequence_stmt}.
	 * @param ctx the parse tree
	 */
	void enterAlter_sequence_stmt(OBParser.Alter_sequence_stmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#alter_sequence_stmt}.
	 * @param ctx the parse tree
	 */
	void exitAlter_sequence_stmt(OBParser.Alter_sequence_stmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#create_dblink_stmt}.
	 * @param ctx the parse tree
	 */
	void enterCreate_dblink_stmt(OBParser.Create_dblink_stmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#create_dblink_stmt}.
	 * @param ctx the parse tree
	 */
	void exitCreate_dblink_stmt(OBParser.Create_dblink_stmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#drop_dblink_stmt}.
	 * @param ctx the parse tree
	 */
	void enterDrop_dblink_stmt(OBParser.Drop_dblink_stmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#drop_dblink_stmt}.
	 * @param ctx the parse tree
	 */
	void exitDrop_dblink_stmt(OBParser.Drop_dblink_stmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#dblink}.
	 * @param ctx the parse tree
	 */
	void enterDblink(OBParser.DblinkContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#dblink}.
	 * @param ctx the parse tree
	 */
	void exitDblink(OBParser.DblinkContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#tenant}.
	 * @param ctx the parse tree
	 */
	void enterTenant(OBParser.TenantContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#tenant}.
	 * @param ctx the parse tree
	 */
	void exitTenant(OBParser.TenantContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#opt_cluster}.
	 * @param ctx the parse tree
	 */
	void enterOpt_cluster(OBParser.Opt_clusterContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#opt_cluster}.
	 * @param ctx the parse tree
	 */
	void exitOpt_cluster(OBParser.Opt_clusterContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#begin_stmt}.
	 * @param ctx the parse tree
	 */
	void enterBegin_stmt(OBParser.Begin_stmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#begin_stmt}.
	 * @param ctx the parse tree
	 */
	void exitBegin_stmt(OBParser.Begin_stmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#commit_stmt}.
	 * @param ctx the parse tree
	 */
	void enterCommit_stmt(OBParser.Commit_stmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#commit_stmt}.
	 * @param ctx the parse tree
	 */
	void exitCommit_stmt(OBParser.Commit_stmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#rollback_stmt}.
	 * @param ctx the parse tree
	 */
	void enterRollback_stmt(OBParser.Rollback_stmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#rollback_stmt}.
	 * @param ctx the parse tree
	 */
	void exitRollback_stmt(OBParser.Rollback_stmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#kill_stmt}.
	 * @param ctx the parse tree
	 */
	void enterKill_stmt(OBParser.Kill_stmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#kill_stmt}.
	 * @param ctx the parse tree
	 */
	void exitKill_stmt(OBParser.Kill_stmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#create_role_stmt}.
	 * @param ctx the parse tree
	 */
	void enterCreate_role_stmt(OBParser.Create_role_stmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#create_role_stmt}.
	 * @param ctx the parse tree
	 */
	void exitCreate_role_stmt(OBParser.Create_role_stmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#role_list}.
	 * @param ctx the parse tree
	 */
	void enterRole_list(OBParser.Role_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#role_list}.
	 * @param ctx the parse tree
	 */
	void exitRole_list(OBParser.Role_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#role}.
	 * @param ctx the parse tree
	 */
	void enterRole(OBParser.RoleContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#role}.
	 * @param ctx the parse tree
	 */
	void exitRole(OBParser.RoleContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#drop_role_stmt}.
	 * @param ctx the parse tree
	 */
	void enterDrop_role_stmt(OBParser.Drop_role_stmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#drop_role_stmt}.
	 * @param ctx the parse tree
	 */
	void exitDrop_role_stmt(OBParser.Drop_role_stmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#set_role_stmt}.
	 * @param ctx the parse tree
	 */
	void enterSet_role_stmt(OBParser.Set_role_stmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#set_role_stmt}.
	 * @param ctx the parse tree
	 */
	void exitSet_role_stmt(OBParser.Set_role_stmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#role_opt_identified_by_list}.
	 * @param ctx the parse tree
	 */
	void enterRole_opt_identified_by_list(OBParser.Role_opt_identified_by_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#role_opt_identified_by_list}.
	 * @param ctx the parse tree
	 */
	void exitRole_opt_identified_by_list(OBParser.Role_opt_identified_by_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#role_opt_identified_by}.
	 * @param ctx the parse tree
	 */
	void enterRole_opt_identified_by(OBParser.Role_opt_identified_byContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#role_opt_identified_by}.
	 * @param ctx the parse tree
	 */
	void exitRole_opt_identified_by(OBParser.Role_opt_identified_byContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#sys_and_obj_priv}.
	 * @param ctx the parse tree
	 */
	void enterSys_and_obj_priv(OBParser.Sys_and_obj_privContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#sys_and_obj_priv}.
	 * @param ctx the parse tree
	 */
	void exitSys_and_obj_priv(OBParser.Sys_and_obj_privContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#grant_stmt}.
	 * @param ctx the parse tree
	 */
	void enterGrant_stmt(OBParser.Grant_stmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#grant_stmt}.
	 * @param ctx the parse tree
	 */
	void exitGrant_stmt(OBParser.Grant_stmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#grant_system_privileges}.
	 * @param ctx the parse tree
	 */
	void enterGrant_system_privileges(OBParser.Grant_system_privilegesContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#grant_system_privileges}.
	 * @param ctx the parse tree
	 */
	void exitGrant_system_privileges(OBParser.Grant_system_privilegesContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#grantee_clause}.
	 * @param ctx the parse tree
	 */
	void enterGrantee_clause(OBParser.Grantee_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#grantee_clause}.
	 * @param ctx the parse tree
	 */
	void exitGrantee_clause(OBParser.Grantee_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#role_sys_obj_all_col_priv_list}.
	 * @param ctx the parse tree
	 */
	void enterRole_sys_obj_all_col_priv_list(OBParser.Role_sys_obj_all_col_priv_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#role_sys_obj_all_col_priv_list}.
	 * @param ctx the parse tree
	 */
	void exitRole_sys_obj_all_col_priv_list(OBParser.Role_sys_obj_all_col_priv_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#role_sys_obj_all_col_priv}.
	 * @param ctx the parse tree
	 */
	void enterRole_sys_obj_all_col_priv(OBParser.Role_sys_obj_all_col_privContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#role_sys_obj_all_col_priv}.
	 * @param ctx the parse tree
	 */
	void exitRole_sys_obj_all_col_priv(OBParser.Role_sys_obj_all_col_privContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#priv_type}.
	 * @param ctx the parse tree
	 */
	void enterPriv_type(OBParser.Priv_typeContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#priv_type}.
	 * @param ctx the parse tree
	 */
	void exitPriv_type(OBParser.Priv_typeContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#obj_clause}.
	 * @param ctx the parse tree
	 */
	void enterObj_clause(OBParser.Obj_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#obj_clause}.
	 * @param ctx the parse tree
	 */
	void exitObj_clause(OBParser.Obj_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#revoke_stmt}.
	 * @param ctx the parse tree
	 */
	void enterRevoke_stmt(OBParser.Revoke_stmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#revoke_stmt}.
	 * @param ctx the parse tree
	 */
	void exitRevoke_stmt(OBParser.Revoke_stmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#prepare_stmt}.
	 * @param ctx the parse tree
	 */
	void enterPrepare_stmt(OBParser.Prepare_stmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#prepare_stmt}.
	 * @param ctx the parse tree
	 */
	void exitPrepare_stmt(OBParser.Prepare_stmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#stmt_name}.
	 * @param ctx the parse tree
	 */
	void enterStmt_name(OBParser.Stmt_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#stmt_name}.
	 * @param ctx the parse tree
	 */
	void exitStmt_name(OBParser.Stmt_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#preparable_stmt}.
	 * @param ctx the parse tree
	 */
	void enterPreparable_stmt(OBParser.Preparable_stmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#preparable_stmt}.
	 * @param ctx the parse tree
	 */
	void exitPreparable_stmt(OBParser.Preparable_stmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#variable_set_stmt}.
	 * @param ctx the parse tree
	 */
	void enterVariable_set_stmt(OBParser.Variable_set_stmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#variable_set_stmt}.
	 * @param ctx the parse tree
	 */
	void exitVariable_set_stmt(OBParser.Variable_set_stmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#sys_var_and_val_list}.
	 * @param ctx the parse tree
	 */
	void enterSys_var_and_val_list(OBParser.Sys_var_and_val_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#sys_var_and_val_list}.
	 * @param ctx the parse tree
	 */
	void exitSys_var_and_val_list(OBParser.Sys_var_and_val_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#var_and_val_list}.
	 * @param ctx the parse tree
	 */
	void enterVar_and_val_list(OBParser.Var_and_val_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#var_and_val_list}.
	 * @param ctx the parse tree
	 */
	void exitVar_and_val_list(OBParser.Var_and_val_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#set_expr_or_default}.
	 * @param ctx the parse tree
	 */
	void enterSet_expr_or_default(OBParser.Set_expr_or_defaultContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#set_expr_or_default}.
	 * @param ctx the parse tree
	 */
	void exitSet_expr_or_default(OBParser.Set_expr_or_defaultContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#var_and_val}.
	 * @param ctx the parse tree
	 */
	void enterVar_and_val(OBParser.Var_and_valContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#var_and_val}.
	 * @param ctx the parse tree
	 */
	void exitVar_and_val(OBParser.Var_and_valContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#sys_var_and_val}.
	 * @param ctx the parse tree
	 */
	void enterSys_var_and_val(OBParser.Sys_var_and_valContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#sys_var_and_val}.
	 * @param ctx the parse tree
	 */
	void exitSys_var_and_val(OBParser.Sys_var_and_valContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#scope_or_scope_alias}.
	 * @param ctx the parse tree
	 */
	void enterScope_or_scope_alias(OBParser.Scope_or_scope_aliasContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#scope_or_scope_alias}.
	 * @param ctx the parse tree
	 */
	void exitScope_or_scope_alias(OBParser.Scope_or_scope_aliasContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#to_or_eq}.
	 * @param ctx the parse tree
	 */
	void enterTo_or_eq(OBParser.To_or_eqContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#to_or_eq}.
	 * @param ctx the parse tree
	 */
	void exitTo_or_eq(OBParser.To_or_eqContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#set_var_op}.
	 * @param ctx the parse tree
	 */
	void enterSet_var_op(OBParser.Set_var_opContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#set_var_op}.
	 * @param ctx the parse tree
	 */
	void exitSet_var_op(OBParser.Set_var_opContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#argument}.
	 * @param ctx the parse tree
	 */
	void enterArgument(OBParser.ArgumentContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#argument}.
	 * @param ctx the parse tree
	 */
	void exitArgument(OBParser.ArgumentContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#execute_stmt}.
	 * @param ctx the parse tree
	 */
	void enterExecute_stmt(OBParser.Execute_stmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#execute_stmt}.
	 * @param ctx the parse tree
	 */
	void exitExecute_stmt(OBParser.Execute_stmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#argument_list}.
	 * @param ctx the parse tree
	 */
	void enterArgument_list(OBParser.Argument_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#argument_list}.
	 * @param ctx the parse tree
	 */
	void exitArgument_list(OBParser.Argument_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#deallocate_prepare_stmt}.
	 * @param ctx the parse tree
	 */
	void enterDeallocate_prepare_stmt(OBParser.Deallocate_prepare_stmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#deallocate_prepare_stmt}.
	 * @param ctx the parse tree
	 */
	void exitDeallocate_prepare_stmt(OBParser.Deallocate_prepare_stmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#deallocate_or_drop}.
	 * @param ctx the parse tree
	 */
	void enterDeallocate_or_drop(OBParser.Deallocate_or_dropContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#deallocate_or_drop}.
	 * @param ctx the parse tree
	 */
	void exitDeallocate_or_drop(OBParser.Deallocate_or_dropContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#call_stmt}.
	 * @param ctx the parse tree
	 */
	void enterCall_stmt(OBParser.Call_stmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#call_stmt}.
	 * @param ctx the parse tree
	 */
	void exitCall_stmt(OBParser.Call_stmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#call_param_list}.
	 * @param ctx the parse tree
	 */
	void enterCall_param_list(OBParser.Call_param_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#call_param_list}.
	 * @param ctx the parse tree
	 */
	void exitCall_param_list(OBParser.Call_param_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#routine_access_name}.
	 * @param ctx the parse tree
	 */
	void enterRoutine_access_name(OBParser.Routine_access_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#routine_access_name}.
	 * @param ctx the parse tree
	 */
	void exitRoutine_access_name(OBParser.Routine_access_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#routine_name}.
	 * @param ctx the parse tree
	 */
	void enterRoutine_name(OBParser.Routine_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#routine_name}.
	 * @param ctx the parse tree
	 */
	void exitRoutine_name(OBParser.Routine_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#truncate_table_stmt}.
	 * @param ctx the parse tree
	 */
	void enterTruncate_table_stmt(OBParser.Truncate_table_stmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#truncate_table_stmt}.
	 * @param ctx the parse tree
	 */
	void exitTruncate_table_stmt(OBParser.Truncate_table_stmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#rename_table_stmt}.
	 * @param ctx the parse tree
	 */
	void enterRename_table_stmt(OBParser.Rename_table_stmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#rename_table_stmt}.
	 * @param ctx the parse tree
	 */
	void exitRename_table_stmt(OBParser.Rename_table_stmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#rename_table_actions}.
	 * @param ctx the parse tree
	 */
	void enterRename_table_actions(OBParser.Rename_table_actionsContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#rename_table_actions}.
	 * @param ctx the parse tree
	 */
	void exitRename_table_actions(OBParser.Rename_table_actionsContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#rename_table_action}.
	 * @param ctx the parse tree
	 */
	void enterRename_table_action(OBParser.Rename_table_actionContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#rename_table_action}.
	 * @param ctx the parse tree
	 */
	void exitRename_table_action(OBParser.Rename_table_actionContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#alter_index_stmt}.
	 * @param ctx the parse tree
	 */
	void enterAlter_index_stmt(OBParser.Alter_index_stmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#alter_index_stmt}.
	 * @param ctx the parse tree
	 */
	void exitAlter_index_stmt(OBParser.Alter_index_stmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#alter_index_actions}.
	 * @param ctx the parse tree
	 */
	void enterAlter_index_actions(OBParser.Alter_index_actionsContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#alter_index_actions}.
	 * @param ctx the parse tree
	 */
	void exitAlter_index_actions(OBParser.Alter_index_actionsContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#alter_index_action}.
	 * @param ctx the parse tree
	 */
	void enterAlter_index_action(OBParser.Alter_index_actionContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#alter_index_action}.
	 * @param ctx the parse tree
	 */
	void exitAlter_index_action(OBParser.Alter_index_actionContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#alter_index_option_oracle}.
	 * @param ctx the parse tree
	 */
	void enterAlter_index_option_oracle(OBParser.Alter_index_option_oracleContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#alter_index_option_oracle}.
	 * @param ctx the parse tree
	 */
	void exitAlter_index_option_oracle(OBParser.Alter_index_option_oracleContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#alter_table_stmt}.
	 * @param ctx the parse tree
	 */
	void enterAlter_table_stmt(OBParser.Alter_table_stmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#alter_table_stmt}.
	 * @param ctx the parse tree
	 */
	void exitAlter_table_stmt(OBParser.Alter_table_stmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#add_external_table_partition_actions}.
	 * @param ctx the parse tree
	 */
	void enterAdd_external_table_partition_actions(OBParser.Add_external_table_partition_actionsContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#add_external_table_partition_actions}.
	 * @param ctx the parse tree
	 */
	void exitAdd_external_table_partition_actions(OBParser.Add_external_table_partition_actionsContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#add_external_table_partition_action}.
	 * @param ctx the parse tree
	 */
	void enterAdd_external_table_partition_action(OBParser.Add_external_table_partition_actionContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#add_external_table_partition_action}.
	 * @param ctx the parse tree
	 */
	void exitAdd_external_table_partition_action(OBParser.Add_external_table_partition_actionContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#alter_table_actions}.
	 * @param ctx the parse tree
	 */
	void enterAlter_table_actions(OBParser.Alter_table_actionsContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#alter_table_actions}.
	 * @param ctx the parse tree
	 */
	void exitAlter_table_actions(OBParser.Alter_table_actionsContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#alter_table_action}.
	 * @param ctx the parse tree
	 */
	void enterAlter_table_action(OBParser.Alter_table_actionContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#alter_table_action}.
	 * @param ctx the parse tree
	 */
	void exitAlter_table_action(OBParser.Alter_table_actionContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#alter_partition_option}.
	 * @param ctx the parse tree
	 */
	void enterAlter_partition_option(OBParser.Alter_partition_optionContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#alter_partition_option}.
	 * @param ctx the parse tree
	 */
	void exitAlter_partition_option(OBParser.Alter_partition_optionContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#drop_partition_name_list}.
	 * @param ctx the parse tree
	 */
	void enterDrop_partition_name_list(OBParser.Drop_partition_name_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#drop_partition_name_list}.
	 * @param ctx the parse tree
	 */
	void exitDrop_partition_name_list(OBParser.Drop_partition_name_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#split_actions}.
	 * @param ctx the parse tree
	 */
	void enterSplit_actions(OBParser.Split_actionsContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#split_actions}.
	 * @param ctx the parse tree
	 */
	void exitSplit_actions(OBParser.Split_actionsContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#add_range_or_list_partition}.
	 * @param ctx the parse tree
	 */
	void enterAdd_range_or_list_partition(OBParser.Add_range_or_list_partitionContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#add_range_or_list_partition}.
	 * @param ctx the parse tree
	 */
	void exitAdd_range_or_list_partition(OBParser.Add_range_or_list_partitionContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#add_range_or_list_subpartition}.
	 * @param ctx the parse tree
	 */
	void enterAdd_range_or_list_subpartition(OBParser.Add_range_or_list_subpartitionContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#add_range_or_list_subpartition}.
	 * @param ctx the parse tree
	 */
	void exitAdd_range_or_list_subpartition(OBParser.Add_range_or_list_subpartitionContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#modify_special_partition}.
	 * @param ctx the parse tree
	 */
	void enterModify_special_partition(OBParser.Modify_special_partitionContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#modify_special_partition}.
	 * @param ctx the parse tree
	 */
	void exitModify_special_partition(OBParser.Modify_special_partitionContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#split_range_partition}.
	 * @param ctx the parse tree
	 */
	void enterSplit_range_partition(OBParser.Split_range_partitionContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#split_range_partition}.
	 * @param ctx the parse tree
	 */
	void exitSplit_range_partition(OBParser.Split_range_partitionContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#split_list_partition}.
	 * @param ctx the parse tree
	 */
	void enterSplit_list_partition(OBParser.Split_list_partitionContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#split_list_partition}.
	 * @param ctx the parse tree
	 */
	void exitSplit_list_partition(OBParser.Split_list_partitionContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#modify_partition_info}.
	 * @param ctx the parse tree
	 */
	void enterModify_partition_info(OBParser.Modify_partition_infoContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#modify_partition_info}.
	 * @param ctx the parse tree
	 */
	void exitModify_partition_info(OBParser.Modify_partition_infoContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#tg_modify_partition_info}.
	 * @param ctx the parse tree
	 */
	void enterTg_modify_partition_info(OBParser.Tg_modify_partition_infoContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#tg_modify_partition_info}.
	 * @param ctx the parse tree
	 */
	void exitTg_modify_partition_info(OBParser.Tg_modify_partition_infoContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#alter_index_option}.
	 * @param ctx the parse tree
	 */
	void enterAlter_index_option(OBParser.Alter_index_optionContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#alter_index_option}.
	 * @param ctx the parse tree
	 */
	void exitAlter_index_option(OBParser.Alter_index_optionContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#visibility_option}.
	 * @param ctx the parse tree
	 */
	void enterVisibility_option(OBParser.Visibility_optionContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#visibility_option}.
	 * @param ctx the parse tree
	 */
	void exitVisibility_option(OBParser.Visibility_optionContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#alter_column_group_option}.
	 * @param ctx the parse tree
	 */
	void enterAlter_column_group_option(OBParser.Alter_column_group_optionContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#alter_column_group_option}.
	 * @param ctx the parse tree
	 */
	void exitAlter_column_group_option(OBParser.Alter_column_group_optionContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#alter_column_option}.
	 * @param ctx the parse tree
	 */
	void enterAlter_column_option(OBParser.Alter_column_optionContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#alter_column_option}.
	 * @param ctx the parse tree
	 */
	void exitAlter_column_option(OBParser.Alter_column_optionContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#alter_tablegroup_option}.
	 * @param ctx the parse tree
	 */
	void enterAlter_tablegroup_option(OBParser.Alter_tablegroup_optionContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#alter_tablegroup_option}.
	 * @param ctx the parse tree
	 */
	void exitAlter_tablegroup_option(OBParser.Alter_tablegroup_optionContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#flashback_stmt}.
	 * @param ctx the parse tree
	 */
	void enterFlashback_stmt(OBParser.Flashback_stmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#flashback_stmt}.
	 * @param ctx the parse tree
	 */
	void exitFlashback_stmt(OBParser.Flashback_stmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#relation_factors}.
	 * @param ctx the parse tree
	 */
	void enterRelation_factors(OBParser.Relation_factorsContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#relation_factors}.
	 * @param ctx the parse tree
	 */
	void exitRelation_factors(OBParser.Relation_factorsContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#purge_stmt}.
	 * @param ctx the parse tree
	 */
	void enterPurge_stmt(OBParser.Purge_stmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#purge_stmt}.
	 * @param ctx the parse tree
	 */
	void exitPurge_stmt(OBParser.Purge_stmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#shrink_space_stmt}.
	 * @param ctx the parse tree
	 */
	void enterShrink_space_stmt(OBParser.Shrink_space_stmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#shrink_space_stmt}.
	 * @param ctx the parse tree
	 */
	void exitShrink_space_stmt(OBParser.Shrink_space_stmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#audit_stmt}.
	 * @param ctx the parse tree
	 */
	void enterAudit_stmt(OBParser.Audit_stmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#audit_stmt}.
	 * @param ctx the parse tree
	 */
	void exitAudit_stmt(OBParser.Audit_stmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#audit_or_noaudit}.
	 * @param ctx the parse tree
	 */
	void enterAudit_or_noaudit(OBParser.Audit_or_noauditContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#audit_or_noaudit}.
	 * @param ctx the parse tree
	 */
	void exitAudit_or_noaudit(OBParser.Audit_or_noauditContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#audit_clause}.
	 * @param ctx the parse tree
	 */
	void enterAudit_clause(OBParser.Audit_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#audit_clause}.
	 * @param ctx the parse tree
	 */
	void exitAudit_clause(OBParser.Audit_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#audit_operation_clause}.
	 * @param ctx the parse tree
	 */
	void enterAudit_operation_clause(OBParser.Audit_operation_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#audit_operation_clause}.
	 * @param ctx the parse tree
	 */
	void exitAudit_operation_clause(OBParser.Audit_operation_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#audit_all_shortcut_list}.
	 * @param ctx the parse tree
	 */
	void enterAudit_all_shortcut_list(OBParser.Audit_all_shortcut_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#audit_all_shortcut_list}.
	 * @param ctx the parse tree
	 */
	void exitAudit_all_shortcut_list(OBParser.Audit_all_shortcut_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#auditing_on_clause}.
	 * @param ctx the parse tree
	 */
	void enterAuditing_on_clause(OBParser.Auditing_on_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#auditing_on_clause}.
	 * @param ctx the parse tree
	 */
	void exitAuditing_on_clause(OBParser.Auditing_on_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#auditing_by_user_clause}.
	 * @param ctx the parse tree
	 */
	void enterAuditing_by_user_clause(OBParser.Auditing_by_user_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#auditing_by_user_clause}.
	 * @param ctx the parse tree
	 */
	void exitAuditing_by_user_clause(OBParser.Auditing_by_user_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#op_audit_tail_clause}.
	 * @param ctx the parse tree
	 */
	void enterOp_audit_tail_clause(OBParser.Op_audit_tail_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#op_audit_tail_clause}.
	 * @param ctx the parse tree
	 */
	void exitOp_audit_tail_clause(OBParser.Op_audit_tail_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#audit_by_session_access_option}.
	 * @param ctx the parse tree
	 */
	void enterAudit_by_session_access_option(OBParser.Audit_by_session_access_optionContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#audit_by_session_access_option}.
	 * @param ctx the parse tree
	 */
	void exitAudit_by_session_access_option(OBParser.Audit_by_session_access_optionContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#audit_whenever_option}.
	 * @param ctx the parse tree
	 */
	void enterAudit_whenever_option(OBParser.Audit_whenever_optionContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#audit_whenever_option}.
	 * @param ctx the parse tree
	 */
	void exitAudit_whenever_option(OBParser.Audit_whenever_optionContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#audit_all_shortcut}.
	 * @param ctx the parse tree
	 */
	void enterAudit_all_shortcut(OBParser.Audit_all_shortcutContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#audit_all_shortcut}.
	 * @param ctx the parse tree
	 */
	void exitAudit_all_shortcut(OBParser.Audit_all_shortcutContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#alter_system_stmt}.
	 * @param ctx the parse tree
	 */
	void enterAlter_system_stmt(OBParser.Alter_system_stmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#alter_system_stmt}.
	 * @param ctx the parse tree
	 */
	void exitAlter_system_stmt(OBParser.Alter_system_stmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#opt_sql_throttle_using_cond}.
	 * @param ctx the parse tree
	 */
	void enterOpt_sql_throttle_using_cond(OBParser.Opt_sql_throttle_using_condContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#opt_sql_throttle_using_cond}.
	 * @param ctx the parse tree
	 */
	void exitOpt_sql_throttle_using_cond(OBParser.Opt_sql_throttle_using_condContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#sql_throttle_one_or_more_metrics}.
	 * @param ctx the parse tree
	 */
	void enterSql_throttle_one_or_more_metrics(OBParser.Sql_throttle_one_or_more_metricsContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#sql_throttle_one_or_more_metrics}.
	 * @param ctx the parse tree
	 */
	void exitSql_throttle_one_or_more_metrics(OBParser.Sql_throttle_one_or_more_metricsContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#sql_throttle_metric}.
	 * @param ctx the parse tree
	 */
	void enterSql_throttle_metric(OBParser.Sql_throttle_metricContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#sql_throttle_metric}.
	 * @param ctx the parse tree
	 */
	void exitSql_throttle_metric(OBParser.Sql_throttle_metricContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#alter_system_set_clause_list}.
	 * @param ctx the parse tree
	 */
	void enterAlter_system_set_clause_list(OBParser.Alter_system_set_clause_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#alter_system_set_clause_list}.
	 * @param ctx the parse tree
	 */
	void exitAlter_system_set_clause_list(OBParser.Alter_system_set_clause_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#alter_system_set_clause}.
	 * @param ctx the parse tree
	 */
	void enterAlter_system_set_clause(OBParser.Alter_system_set_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#alter_system_set_clause}.
	 * @param ctx the parse tree
	 */
	void exitAlter_system_set_clause(OBParser.Alter_system_set_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#alter_system_reset_clause_list}.
	 * @param ctx the parse tree
	 */
	void enterAlter_system_reset_clause_list(OBParser.Alter_system_reset_clause_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#alter_system_reset_clause_list}.
	 * @param ctx the parse tree
	 */
	void exitAlter_system_reset_clause_list(OBParser.Alter_system_reset_clause_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#alter_system_reset_clause}.
	 * @param ctx the parse tree
	 */
	void enterAlter_system_reset_clause(OBParser.Alter_system_reset_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#alter_system_reset_clause}.
	 * @param ctx the parse tree
	 */
	void exitAlter_system_reset_clause(OBParser.Alter_system_reset_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#set_system_parameter_clause}.
	 * @param ctx the parse tree
	 */
	void enterSet_system_parameter_clause(OBParser.Set_system_parameter_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#set_system_parameter_clause}.
	 * @param ctx the parse tree
	 */
	void exitSet_system_parameter_clause(OBParser.Set_system_parameter_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#reset_system_parameter_clause}.
	 * @param ctx the parse tree
	 */
	void enterReset_system_parameter_clause(OBParser.Reset_system_parameter_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#reset_system_parameter_clause}.
	 * @param ctx the parse tree
	 */
	void exitReset_system_parameter_clause(OBParser.Reset_system_parameter_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#cache_type_or_string}.
	 * @param ctx the parse tree
	 */
	void enterCache_type_or_string(OBParser.Cache_type_or_stringContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#cache_type_or_string}.
	 * @param ctx the parse tree
	 */
	void exitCache_type_or_string(OBParser.Cache_type_or_stringContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#cache_type}.
	 * @param ctx the parse tree
	 */
	void enterCache_type(OBParser.Cache_typeContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#cache_type}.
	 * @param ctx the parse tree
	 */
	void exitCache_type(OBParser.Cache_typeContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#balance_task_type}.
	 * @param ctx the parse tree
	 */
	void enterBalance_task_type(OBParser.Balance_task_typeContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#balance_task_type}.
	 * @param ctx the parse tree
	 */
	void exitBalance_task_type(OBParser.Balance_task_typeContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#tenant_list_tuple}.
	 * @param ctx the parse tree
	 */
	void enterTenant_list_tuple(OBParser.Tenant_list_tupleContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#tenant_list_tuple}.
	 * @param ctx the parse tree
	 */
	void exitTenant_list_tuple(OBParser.Tenant_list_tupleContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#tenant_list_tuple_v2}.
	 * @param ctx the parse tree
	 */
	void enterTenant_list_tuple_v2(OBParser.Tenant_list_tuple_v2Context ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#tenant_list_tuple_v2}.
	 * @param ctx the parse tree
	 */
	void exitTenant_list_tuple_v2(OBParser.Tenant_list_tuple_v2Context ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#tenant_name_list}.
	 * @param ctx the parse tree
	 */
	void enterTenant_name_list(OBParser.Tenant_name_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#tenant_name_list}.
	 * @param ctx the parse tree
	 */
	void exitTenant_name_list(OBParser.Tenant_name_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#opt_path_info}.
	 * @param ctx the parse tree
	 */
	void enterOpt_path_info(OBParser.Opt_path_infoContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#opt_path_info}.
	 * @param ctx the parse tree
	 */
	void exitOpt_path_info(OBParser.Opt_path_infoContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#policy_name}.
	 * @param ctx the parse tree
	 */
	void enterPolicy_name(OBParser.Policy_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#policy_name}.
	 * @param ctx the parse tree
	 */
	void exitPolicy_name(OBParser.Policy_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#flush_scope}.
	 * @param ctx the parse tree
	 */
	void enterFlush_scope(OBParser.Flush_scopeContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#flush_scope}.
	 * @param ctx the parse tree
	 */
	void exitFlush_scope(OBParser.Flush_scopeContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#server_info_list}.
	 * @param ctx the parse tree
	 */
	void enterServer_info_list(OBParser.Server_info_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#server_info_list}.
	 * @param ctx the parse tree
	 */
	void exitServer_info_list(OBParser.Server_info_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#server_info}.
	 * @param ctx the parse tree
	 */
	void enterServer_info(OBParser.Server_infoContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#server_info}.
	 * @param ctx the parse tree
	 */
	void exitServer_info(OBParser.Server_infoContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#server_action}.
	 * @param ctx the parse tree
	 */
	void enterServer_action(OBParser.Server_actionContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#server_action}.
	 * @param ctx the parse tree
	 */
	void exitServer_action(OBParser.Server_actionContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#server_list}.
	 * @param ctx the parse tree
	 */
	void enterServer_list(OBParser.Server_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#server_list}.
	 * @param ctx the parse tree
	 */
	void exitServer_list(OBParser.Server_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#zone_action}.
	 * @param ctx the parse tree
	 */
	void enterZone_action(OBParser.Zone_actionContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#zone_action}.
	 * @param ctx the parse tree
	 */
	void exitZone_action(OBParser.Zone_actionContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#ip_port}.
	 * @param ctx the parse tree
	 */
	void enterIp_port(OBParser.Ip_portContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#ip_port}.
	 * @param ctx the parse tree
	 */
	void exitIp_port(OBParser.Ip_portContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#zone_desc}.
	 * @param ctx the parse tree
	 */
	void enterZone_desc(OBParser.Zone_descContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#zone_desc}.
	 * @param ctx the parse tree
	 */
	void exitZone_desc(OBParser.Zone_descContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#server_or_zone}.
	 * @param ctx the parse tree
	 */
	void enterServer_or_zone(OBParser.Server_or_zoneContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#server_or_zone}.
	 * @param ctx the parse tree
	 */
	void exitServer_or_zone(OBParser.Server_or_zoneContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#add_or_alter_zone_option}.
	 * @param ctx the parse tree
	 */
	void enterAdd_or_alter_zone_option(OBParser.Add_or_alter_zone_optionContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#add_or_alter_zone_option}.
	 * @param ctx the parse tree
	 */
	void exitAdd_or_alter_zone_option(OBParser.Add_or_alter_zone_optionContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#add_or_alter_zone_options}.
	 * @param ctx the parse tree
	 */
	void enterAdd_or_alter_zone_options(OBParser.Add_or_alter_zone_optionsContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#add_or_alter_zone_options}.
	 * @param ctx the parse tree
	 */
	void exitAdd_or_alter_zone_options(OBParser.Add_or_alter_zone_optionsContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#alter_or_change_or_modify}.
	 * @param ctx the parse tree
	 */
	void enterAlter_or_change_or_modify(OBParser.Alter_or_change_or_modifyContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#alter_or_change_or_modify}.
	 * @param ctx the parse tree
	 */
	void exitAlter_or_change_or_modify(OBParser.Alter_or_change_or_modifyContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#ls}.
	 * @param ctx the parse tree
	 */
	void enterLs(OBParser.LsContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#ls}.
	 * @param ctx the parse tree
	 */
	void exitLs(OBParser.LsContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#partition_id_desc}.
	 * @param ctx the parse tree
	 */
	void enterPartition_id_desc(OBParser.Partition_id_descContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#partition_id_desc}.
	 * @param ctx the parse tree
	 */
	void exitPartition_id_desc(OBParser.Partition_id_descContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#partition_id_or_server_or_zone}.
	 * @param ctx the parse tree
	 */
	void enterPartition_id_or_server_or_zone(OBParser.Partition_id_or_server_or_zoneContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#partition_id_or_server_or_zone}.
	 * @param ctx the parse tree
	 */
	void exitPartition_id_or_server_or_zone(OBParser.Partition_id_or_server_or_zoneContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#migrate_action}.
	 * @param ctx the parse tree
	 */
	void enterMigrate_action(OBParser.Migrate_actionContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#migrate_action}.
	 * @param ctx the parse tree
	 */
	void exitMigrate_action(OBParser.Migrate_actionContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#change_actions}.
	 * @param ctx the parse tree
	 */
	void enterChange_actions(OBParser.Change_actionsContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#change_actions}.
	 * @param ctx the parse tree
	 */
	void exitChange_actions(OBParser.Change_actionsContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#change_action}.
	 * @param ctx the parse tree
	 */
	void enterChange_action(OBParser.Change_actionContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#change_action}.
	 * @param ctx the parse tree
	 */
	void exitChange_action(OBParser.Change_actionContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#replica_type}.
	 * @param ctx the parse tree
	 */
	void enterReplica_type(OBParser.Replica_typeContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#replica_type}.
	 * @param ctx the parse tree
	 */
	void exitReplica_type(OBParser.Replica_typeContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#memstore_percent}.
	 * @param ctx the parse tree
	 */
	void enterMemstore_percent(OBParser.Memstore_percentContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#memstore_percent}.
	 * @param ctx the parse tree
	 */
	void exitMemstore_percent(OBParser.Memstore_percentContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#suspend_or_resume}.
	 * @param ctx the parse tree
	 */
	void enterSuspend_or_resume(OBParser.Suspend_or_resumeContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#suspend_or_resume}.
	 * @param ctx the parse tree
	 */
	void exitSuspend_or_resume(OBParser.Suspend_or_resumeContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#baseline_id_expr}.
	 * @param ctx the parse tree
	 */
	void enterBaseline_id_expr(OBParser.Baseline_id_exprContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#baseline_id_expr}.
	 * @param ctx the parse tree
	 */
	void exitBaseline_id_expr(OBParser.Baseline_id_exprContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#sql_id_expr}.
	 * @param ctx the parse tree
	 */
	void enterSql_id_expr(OBParser.Sql_id_exprContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#sql_id_expr}.
	 * @param ctx the parse tree
	 */
	void exitSql_id_expr(OBParser.Sql_id_exprContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#baseline_asgn_factor}.
	 * @param ctx the parse tree
	 */
	void enterBaseline_asgn_factor(OBParser.Baseline_asgn_factorContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#baseline_asgn_factor}.
	 * @param ctx the parse tree
	 */
	void exitBaseline_asgn_factor(OBParser.Baseline_asgn_factorContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#tenant_name}.
	 * @param ctx the parse tree
	 */
	void enterTenant_name(OBParser.Tenant_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#tenant_name}.
	 * @param ctx the parse tree
	 */
	void exitTenant_name(OBParser.Tenant_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#namespace_expr}.
	 * @param ctx the parse tree
	 */
	void enterNamespace_expr(OBParser.Namespace_exprContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#namespace_expr}.
	 * @param ctx the parse tree
	 */
	void exitNamespace_expr(OBParser.Namespace_exprContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#cache_name}.
	 * @param ctx the parse tree
	 */
	void enterCache_name(OBParser.Cache_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#cache_name}.
	 * @param ctx the parse tree
	 */
	void exitCache_name(OBParser.Cache_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#file_id}.
	 * @param ctx the parse tree
	 */
	void enterFile_id(OBParser.File_idContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#file_id}.
	 * @param ctx the parse tree
	 */
	void exitFile_id(OBParser.File_idContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#cancel_task_type}.
	 * @param ctx the parse tree
	 */
	void enterCancel_task_type(OBParser.Cancel_task_typeContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#cancel_task_type}.
	 * @param ctx the parse tree
	 */
	void exitCancel_task_type(OBParser.Cancel_task_typeContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#alter_system_settp_actions}.
	 * @param ctx the parse tree
	 */
	void enterAlter_system_settp_actions(OBParser.Alter_system_settp_actionsContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#alter_system_settp_actions}.
	 * @param ctx the parse tree
	 */
	void exitAlter_system_settp_actions(OBParser.Alter_system_settp_actionsContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#settp_option}.
	 * @param ctx the parse tree
	 */
	void enterSettp_option(OBParser.Settp_optionContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#settp_option}.
	 * @param ctx the parse tree
	 */
	void exitSettp_option(OBParser.Settp_optionContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#partition_role}.
	 * @param ctx the parse tree
	 */
	void enterPartition_role(OBParser.Partition_roleContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#partition_role}.
	 * @param ctx the parse tree
	 */
	void exitPartition_role(OBParser.Partition_roleContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#upgrade_action}.
	 * @param ctx the parse tree
	 */
	void enterUpgrade_action(OBParser.Upgrade_actionContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#upgrade_action}.
	 * @param ctx the parse tree
	 */
	void exitUpgrade_action(OBParser.Upgrade_actionContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#alter_session_stmt}.
	 * @param ctx the parse tree
	 */
	void enterAlter_session_stmt(OBParser.Alter_session_stmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#alter_session_stmt}.
	 * @param ctx the parse tree
	 */
	void exitAlter_session_stmt(OBParser.Alter_session_stmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#var_name_of_forced_module}.
	 * @param ctx the parse tree
	 */
	void enterVar_name_of_forced_module(OBParser.Var_name_of_forced_moduleContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#var_name_of_forced_module}.
	 * @param ctx the parse tree
	 */
	void exitVar_name_of_forced_module(OBParser.Var_name_of_forced_moduleContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#var_name_of_module}.
	 * @param ctx the parse tree
	 */
	void enterVar_name_of_module(OBParser.Var_name_of_moduleContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#var_name_of_module}.
	 * @param ctx the parse tree
	 */
	void exitVar_name_of_module(OBParser.Var_name_of_moduleContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#switch_option}.
	 * @param ctx the parse tree
	 */
	void enterSwitch_option(OBParser.Switch_optionContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#switch_option}.
	 * @param ctx the parse tree
	 */
	void exitSwitch_option(OBParser.Switch_optionContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#session_isolation_level}.
	 * @param ctx the parse tree
	 */
	void enterSession_isolation_level(OBParser.Session_isolation_levelContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#session_isolation_level}.
	 * @param ctx the parse tree
	 */
	void exitSession_isolation_level(OBParser.Session_isolation_levelContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#alter_session_set_clause}.
	 * @param ctx the parse tree
	 */
	void enterAlter_session_set_clause(OBParser.Alter_session_set_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#alter_session_set_clause}.
	 * @param ctx the parse tree
	 */
	void exitAlter_session_set_clause(OBParser.Alter_session_set_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#set_system_parameter_clause_list}.
	 * @param ctx the parse tree
	 */
	void enterSet_system_parameter_clause_list(OBParser.Set_system_parameter_clause_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#set_system_parameter_clause_list}.
	 * @param ctx the parse tree
	 */
	void exitSet_system_parameter_clause_list(OBParser.Set_system_parameter_clause_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#current_schema}.
	 * @param ctx the parse tree
	 */
	void enterCurrent_schema(OBParser.Current_schemaContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#current_schema}.
	 * @param ctx the parse tree
	 */
	void exitCurrent_schema(OBParser.Current_schemaContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#set_comment_stmt}.
	 * @param ctx the parse tree
	 */
	void enterSet_comment_stmt(OBParser.Set_comment_stmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#set_comment_stmt}.
	 * @param ctx the parse tree
	 */
	void exitSet_comment_stmt(OBParser.Set_comment_stmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#create_tablespace_stmt}.
	 * @param ctx the parse tree
	 */
	void enterCreate_tablespace_stmt(OBParser.Create_tablespace_stmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#create_tablespace_stmt}.
	 * @param ctx the parse tree
	 */
	void exitCreate_tablespace_stmt(OBParser.Create_tablespace_stmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#drop_tablespace_stmt}.
	 * @param ctx the parse tree
	 */
	void enterDrop_tablespace_stmt(OBParser.Drop_tablespace_stmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#drop_tablespace_stmt}.
	 * @param ctx the parse tree
	 */
	void exitDrop_tablespace_stmt(OBParser.Drop_tablespace_stmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#tablespace}.
	 * @param ctx the parse tree
	 */
	void enterTablespace(OBParser.TablespaceContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#tablespace}.
	 * @param ctx the parse tree
	 */
	void exitTablespace(OBParser.TablespaceContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#alter_tablespace_stmt}.
	 * @param ctx the parse tree
	 */
	void enterAlter_tablespace_stmt(OBParser.Alter_tablespace_stmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#alter_tablespace_stmt}.
	 * @param ctx the parse tree
	 */
	void exitAlter_tablespace_stmt(OBParser.Alter_tablespace_stmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#alter_tablespace_actions}.
	 * @param ctx the parse tree
	 */
	void enterAlter_tablespace_actions(OBParser.Alter_tablespace_actionsContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#alter_tablespace_actions}.
	 * @param ctx the parse tree
	 */
	void exitAlter_tablespace_actions(OBParser.Alter_tablespace_actionsContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#alter_tablespace_action}.
	 * @param ctx the parse tree
	 */
	void enterAlter_tablespace_action(OBParser.Alter_tablespace_actionContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#alter_tablespace_action}.
	 * @param ctx the parse tree
	 */
	void exitAlter_tablespace_action(OBParser.Alter_tablespace_actionContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#permanent_tablespace}.
	 * @param ctx the parse tree
	 */
	void enterPermanent_tablespace(OBParser.Permanent_tablespaceContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#permanent_tablespace}.
	 * @param ctx the parse tree
	 */
	void exitPermanent_tablespace(OBParser.Permanent_tablespaceContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#permanent_tablespace_options}.
	 * @param ctx the parse tree
	 */
	void enterPermanent_tablespace_options(OBParser.Permanent_tablespace_optionsContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#permanent_tablespace_options}.
	 * @param ctx the parse tree
	 */
	void exitPermanent_tablespace_options(OBParser.Permanent_tablespace_optionsContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#permanent_tablespace_option}.
	 * @param ctx the parse tree
	 */
	void enterPermanent_tablespace_option(OBParser.Permanent_tablespace_optionContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#permanent_tablespace_option}.
	 * @param ctx the parse tree
	 */
	void exitPermanent_tablespace_option(OBParser.Permanent_tablespace_optionContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#create_profile_stmt}.
	 * @param ctx the parse tree
	 */
	void enterCreate_profile_stmt(OBParser.Create_profile_stmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#create_profile_stmt}.
	 * @param ctx the parse tree
	 */
	void exitCreate_profile_stmt(OBParser.Create_profile_stmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#alter_profile_stmt}.
	 * @param ctx the parse tree
	 */
	void enterAlter_profile_stmt(OBParser.Alter_profile_stmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#alter_profile_stmt}.
	 * @param ctx the parse tree
	 */
	void exitAlter_profile_stmt(OBParser.Alter_profile_stmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#drop_profile_stmt}.
	 * @param ctx the parse tree
	 */
	void enterDrop_profile_stmt(OBParser.Drop_profile_stmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#drop_profile_stmt}.
	 * @param ctx the parse tree
	 */
	void exitDrop_profile_stmt(OBParser.Drop_profile_stmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#profile_name}.
	 * @param ctx the parse tree
	 */
	void enterProfile_name(OBParser.Profile_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#profile_name}.
	 * @param ctx the parse tree
	 */
	void exitProfile_name(OBParser.Profile_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#password_parameters}.
	 * @param ctx the parse tree
	 */
	void enterPassword_parameters(OBParser.Password_parametersContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#password_parameters}.
	 * @param ctx the parse tree
	 */
	void exitPassword_parameters(OBParser.Password_parametersContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#password_parameter}.
	 * @param ctx the parse tree
	 */
	void enterPassword_parameter(OBParser.Password_parameterContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#password_parameter}.
	 * @param ctx the parse tree
	 */
	void exitPassword_parameter(OBParser.Password_parameterContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#verify_function_name}.
	 * @param ctx the parse tree
	 */
	void enterVerify_function_name(OBParser.Verify_function_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#verify_function_name}.
	 * @param ctx the parse tree
	 */
	void exitVerify_function_name(OBParser.Verify_function_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#password_parameter_value}.
	 * @param ctx the parse tree
	 */
	void enterPassword_parameter_value(OBParser.Password_parameter_valueContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#password_parameter_value}.
	 * @param ctx the parse tree
	 */
	void exitPassword_parameter_value(OBParser.Password_parameter_valueContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#password_parameter_type}.
	 * @param ctx the parse tree
	 */
	void enterPassword_parameter_type(OBParser.Password_parameter_typeContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#password_parameter_type}.
	 * @param ctx the parse tree
	 */
	void exitPassword_parameter_type(OBParser.Password_parameter_typeContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#user_profile}.
	 * @param ctx the parse tree
	 */
	void enterUser_profile(OBParser.User_profileContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#user_profile}.
	 * @param ctx the parse tree
	 */
	void exitUser_profile(OBParser.User_profileContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#method_opt}.
	 * @param ctx the parse tree
	 */
	void enterMethod_opt(OBParser.Method_optContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#method_opt}.
	 * @param ctx the parse tree
	 */
	void exitMethod_opt(OBParser.Method_optContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#method_list}.
	 * @param ctx the parse tree
	 */
	void enterMethod_list(OBParser.Method_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#method_list}.
	 * @param ctx the parse tree
	 */
	void exitMethod_list(OBParser.Method_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#method}.
	 * @param ctx the parse tree
	 */
	void enterMethod(OBParser.MethodContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#method}.
	 * @param ctx the parse tree
	 */
	void exitMethod(OBParser.MethodContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#for_all}.
	 * @param ctx the parse tree
	 */
	void enterFor_all(OBParser.For_allContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#for_all}.
	 * @param ctx the parse tree
	 */
	void exitFor_all(OBParser.For_allContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#size_clause}.
	 * @param ctx the parse tree
	 */
	void enterSize_clause(OBParser.Size_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#size_clause}.
	 * @param ctx the parse tree
	 */
	void exitSize_clause(OBParser.Size_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#for_columns}.
	 * @param ctx the parse tree
	 */
	void enterFor_columns(OBParser.For_columnsContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#for_columns}.
	 * @param ctx the parse tree
	 */
	void exitFor_columns(OBParser.For_columnsContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#for_columns_list}.
	 * @param ctx the parse tree
	 */
	void enterFor_columns_list(OBParser.For_columns_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#for_columns_list}.
	 * @param ctx the parse tree
	 */
	void exitFor_columns_list(OBParser.For_columns_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#for_columns_item}.
	 * @param ctx the parse tree
	 */
	void enterFor_columns_item(OBParser.For_columns_itemContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#for_columns_item}.
	 * @param ctx the parse tree
	 */
	void exitFor_columns_item(OBParser.For_columns_itemContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#column_clause}.
	 * @param ctx the parse tree
	 */
	void enterColumn_clause(OBParser.Column_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#column_clause}.
	 * @param ctx the parse tree
	 */
	void exitColumn_clause(OBParser.Column_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#extension}.
	 * @param ctx the parse tree
	 */
	void enterExtension(OBParser.ExtensionContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#extension}.
	 * @param ctx the parse tree
	 */
	void exitExtension(OBParser.ExtensionContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#set_names_stmt}.
	 * @param ctx the parse tree
	 */
	void enterSet_names_stmt(OBParser.Set_names_stmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#set_names_stmt}.
	 * @param ctx the parse tree
	 */
	void exitSet_names_stmt(OBParser.Set_names_stmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#set_charset_stmt}.
	 * @param ctx the parse tree
	 */
	void enterSet_charset_stmt(OBParser.Set_charset_stmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#set_charset_stmt}.
	 * @param ctx the parse tree
	 */
	void exitSet_charset_stmt(OBParser.Set_charset_stmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#set_transaction_stmt}.
	 * @param ctx the parse tree
	 */
	void enterSet_transaction_stmt(OBParser.Set_transaction_stmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#set_transaction_stmt}.
	 * @param ctx the parse tree
	 */
	void exitSet_transaction_stmt(OBParser.Set_transaction_stmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#transaction_characteristics}.
	 * @param ctx the parse tree
	 */
	void enterTransaction_characteristics(OBParser.Transaction_characteristicsContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#transaction_characteristics}.
	 * @param ctx the parse tree
	 */
	void exitTransaction_characteristics(OBParser.Transaction_characteristicsContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#transaction_access_mode}.
	 * @param ctx the parse tree
	 */
	void enterTransaction_access_mode(OBParser.Transaction_access_modeContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#transaction_access_mode}.
	 * @param ctx the parse tree
	 */
	void exitTransaction_access_mode(OBParser.Transaction_access_modeContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#isolation_level}.
	 * @param ctx the parse tree
	 */
	void enterIsolation_level(OBParser.Isolation_levelContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#isolation_level}.
	 * @param ctx the parse tree
	 */
	void exitIsolation_level(OBParser.Isolation_levelContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#switchover_tenant_stmt}.
	 * @param ctx the parse tree
	 */
	void enterSwitchover_tenant_stmt(OBParser.Switchover_tenant_stmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#switchover_tenant_stmt}.
	 * @param ctx the parse tree
	 */
	void exitSwitchover_tenant_stmt(OBParser.Switchover_tenant_stmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#switchover_clause}.
	 * @param ctx the parse tree
	 */
	void enterSwitchover_clause(OBParser.Switchover_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#switchover_clause}.
	 * @param ctx the parse tree
	 */
	void exitSwitchover_clause(OBParser.Switchover_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#recover_tenant_stmt}.
	 * @param ctx the parse tree
	 */
	void enterRecover_tenant_stmt(OBParser.Recover_tenant_stmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#recover_tenant_stmt}.
	 * @param ctx the parse tree
	 */
	void exitRecover_tenant_stmt(OBParser.Recover_tenant_stmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#recover_point_clause}.
	 * @param ctx the parse tree
	 */
	void enterRecover_point_clause(OBParser.Recover_point_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#recover_point_clause}.
	 * @param ctx the parse tree
	 */
	void exitRecover_point_clause(OBParser.Recover_point_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#transfer_partition_stmt}.
	 * @param ctx the parse tree
	 */
	void enterTransfer_partition_stmt(OBParser.Transfer_partition_stmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#transfer_partition_stmt}.
	 * @param ctx the parse tree
	 */
	void exitTransfer_partition_stmt(OBParser.Transfer_partition_stmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#transfer_partition_clause}.
	 * @param ctx the parse tree
	 */
	void enterTransfer_partition_clause(OBParser.Transfer_partition_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#transfer_partition_clause}.
	 * @param ctx the parse tree
	 */
	void exitTransfer_partition_clause(OBParser.Transfer_partition_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#part_info}.
	 * @param ctx the parse tree
	 */
	void enterPart_info(OBParser.Part_infoContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#part_info}.
	 * @param ctx the parse tree
	 */
	void exitPart_info(OBParser.Part_infoContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#cancel_transfer_partition_clause}.
	 * @param ctx the parse tree
	 */
	void enterCancel_transfer_partition_clause(OBParser.Cancel_transfer_partition_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#cancel_transfer_partition_clause}.
	 * @param ctx the parse tree
	 */
	void exitCancel_transfer_partition_clause(OBParser.Cancel_transfer_partition_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#service_name_stmt}.
	 * @param ctx the parse tree
	 */
	void enterService_name_stmt(OBParser.Service_name_stmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#service_name_stmt}.
	 * @param ctx the parse tree
	 */
	void exitService_name_stmt(OBParser.Service_name_stmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#service_op}.
	 * @param ctx the parse tree
	 */
	void enterService_op(OBParser.Service_opContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#service_op}.
	 * @param ctx the parse tree
	 */
	void exitService_op(OBParser.Service_opContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#create_savepoint_stmt}.
	 * @param ctx the parse tree
	 */
	void enterCreate_savepoint_stmt(OBParser.Create_savepoint_stmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#create_savepoint_stmt}.
	 * @param ctx the parse tree
	 */
	void exitCreate_savepoint_stmt(OBParser.Create_savepoint_stmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#rollback_savepoint_stmt}.
	 * @param ctx the parse tree
	 */
	void enterRollback_savepoint_stmt(OBParser.Rollback_savepoint_stmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#rollback_savepoint_stmt}.
	 * @param ctx the parse tree
	 */
	void exitRollback_savepoint_stmt(OBParser.Rollback_savepoint_stmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#var_name}.
	 * @param ctx the parse tree
	 */
	void enterVar_name(OBParser.Var_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#var_name}.
	 * @param ctx the parse tree
	 */
	void exitVar_name(OBParser.Var_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#column_name}.
	 * @param ctx the parse tree
	 */
	void enterColumn_name(OBParser.Column_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#column_name}.
	 * @param ctx the parse tree
	 */
	void exitColumn_name(OBParser.Column_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#relation_name}.
	 * @param ctx the parse tree
	 */
	void enterRelation_name(OBParser.Relation_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#relation_name}.
	 * @param ctx the parse tree
	 */
	void exitRelation_name(OBParser.Relation_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#exists_function_name}.
	 * @param ctx the parse tree
	 */
	void enterExists_function_name(OBParser.Exists_function_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#exists_function_name}.
	 * @param ctx the parse tree
	 */
	void exitExists_function_name(OBParser.Exists_function_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#function_name}.
	 * @param ctx the parse tree
	 */
	void enterFunction_name(OBParser.Function_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#function_name}.
	 * @param ctx the parse tree
	 */
	void exitFunction_name(OBParser.Function_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#column_label}.
	 * @param ctx the parse tree
	 */
	void enterColumn_label(OBParser.Column_labelContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#column_label}.
	 * @param ctx the parse tree
	 */
	void exitColumn_label(OBParser.Column_labelContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#keystore_name}.
	 * @param ctx the parse tree
	 */
	void enterKeystore_name(OBParser.Keystore_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#keystore_name}.
	 * @param ctx the parse tree
	 */
	void exitKeystore_name(OBParser.Keystore_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#date_unit}.
	 * @param ctx the parse tree
	 */
	void enterDate_unit(OBParser.Date_unitContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#date_unit}.
	 * @param ctx the parse tree
	 */
	void exitDate_unit(OBParser.Date_unitContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#timezone_unit}.
	 * @param ctx the parse tree
	 */
	void enterTimezone_unit(OBParser.Timezone_unitContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#timezone_unit}.
	 * @param ctx the parse tree
	 */
	void exitTimezone_unit(OBParser.Timezone_unitContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#date_unit_for_extract}.
	 * @param ctx the parse tree
	 */
	void enterDate_unit_for_extract(OBParser.Date_unit_for_extractContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#date_unit_for_extract}.
	 * @param ctx the parse tree
	 */
	void exitDate_unit_for_extract(OBParser.Date_unit_for_extractContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#json_mergepatch_expr}.
	 * @param ctx the parse tree
	 */
	void enterJson_mergepatch_expr(OBParser.Json_mergepatch_exprContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#json_mergepatch_expr}.
	 * @param ctx the parse tree
	 */
	void exitJson_mergepatch_expr(OBParser.Json_mergepatch_exprContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#json_mergepatch_on_error}.
	 * @param ctx the parse tree
	 */
	void enterJson_mergepatch_on_error(OBParser.Json_mergepatch_on_errorContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#json_mergepatch_on_error}.
	 * @param ctx the parse tree
	 */
	void exitJson_mergepatch_on_error(OBParser.Json_mergepatch_on_errorContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#opt_json_mergepatch}.
	 * @param ctx the parse tree
	 */
	void enterOpt_json_mergepatch(OBParser.Opt_json_mergepatchContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#opt_json_mergepatch}.
	 * @param ctx the parse tree
	 */
	void exitOpt_json_mergepatch(OBParser.Opt_json_mergepatchContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#js_mp_return_clause}.
	 * @param ctx the parse tree
	 */
	void enterJs_mp_return_clause(OBParser.Js_mp_return_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#js_mp_return_clause}.
	 * @param ctx the parse tree
	 */
	void exitJs_mp_return_clause(OBParser.Js_mp_return_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#json_array_expr}.
	 * @param ctx the parse tree
	 */
	void enterJson_array_expr(OBParser.Json_array_exprContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#json_array_expr}.
	 * @param ctx the parse tree
	 */
	void exitJson_array_expr(OBParser.Json_array_exprContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#json_array_content}.
	 * @param ctx the parse tree
	 */
	void enterJson_array_content(OBParser.Json_array_contentContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#json_array_content}.
	 * @param ctx the parse tree
	 */
	void exitJson_array_content(OBParser.Json_array_contentContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#json_array_on_null}.
	 * @param ctx the parse tree
	 */
	void enterJson_array_on_null(OBParser.Json_array_on_nullContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#json_array_on_null}.
	 * @param ctx the parse tree
	 */
	void exitJson_array_on_null(OBParser.Json_array_on_nullContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#js_array_eles}.
	 * @param ctx the parse tree
	 */
	void enterJs_array_eles(OBParser.Js_array_elesContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#js_array_eles}.
	 * @param ctx the parse tree
	 */
	void exitJs_array_eles(OBParser.Js_array_elesContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#js_array_ele}.
	 * @param ctx the parse tree
	 */
	void enterJs_array_ele(OBParser.Js_array_eleContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#js_array_ele}.
	 * @param ctx the parse tree
	 */
	void exitJs_array_ele(OBParser.Js_array_eleContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#js_array_return_clause}.
	 * @param ctx the parse tree
	 */
	void enterJs_array_return_clause(OBParser.Js_array_return_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#js_array_return_clause}.
	 * @param ctx the parse tree
	 */
	void exitJs_array_return_clause(OBParser.Js_array_return_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#json_value_expr}.
	 * @param ctx the parse tree
	 */
	void enterJson_value_expr(OBParser.Json_value_exprContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#json_value_expr}.
	 * @param ctx the parse tree
	 */
	void exitJson_value_expr(OBParser.Json_value_exprContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#json_equal_expr}.
	 * @param ctx the parse tree
	 */
	void enterJson_equal_expr(OBParser.Json_equal_exprContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#json_equal_expr}.
	 * @param ctx the parse tree
	 */
	void exitJson_equal_expr(OBParser.Json_equal_exprContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#json_value_on_opt}.
	 * @param ctx the parse tree
	 */
	void enterJson_value_on_opt(OBParser.Json_value_on_optContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#json_value_on_opt}.
	 * @param ctx the parse tree
	 */
	void exitJson_value_on_opt(OBParser.Json_value_on_optContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#js_doc_expr}.
	 * @param ctx the parse tree
	 */
	void enterJs_doc_expr(OBParser.Js_doc_exprContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#js_doc_expr}.
	 * @param ctx the parse tree
	 */
	void exitJs_doc_expr(OBParser.Js_doc_exprContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#opt_js_value_returning_type}.
	 * @param ctx the parse tree
	 */
	void enterOpt_js_value_returning_type(OBParser.Opt_js_value_returning_typeContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#opt_js_value_returning_type}.
	 * @param ctx the parse tree
	 */
	void exitOpt_js_value_returning_type(OBParser.Opt_js_value_returning_typeContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#json_value_on_empty}.
	 * @param ctx the parse tree
	 */
	void enterJson_value_on_empty(OBParser.Json_value_on_emptyContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#json_value_on_empty}.
	 * @param ctx the parse tree
	 */
	void exitJson_value_on_empty(OBParser.Json_value_on_emptyContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#json_value_on_empty_response}.
	 * @param ctx the parse tree
	 */
	void enterJson_value_on_empty_response(OBParser.Json_value_on_empty_responseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#json_value_on_empty_response}.
	 * @param ctx the parse tree
	 */
	void exitJson_value_on_empty_response(OBParser.Json_value_on_empty_responseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#json_value_on_error}.
	 * @param ctx the parse tree
	 */
	void enterJson_value_on_error(OBParser.Json_value_on_errorContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#json_value_on_error}.
	 * @param ctx the parse tree
	 */
	void exitJson_value_on_error(OBParser.Json_value_on_errorContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#json_value_on_error_response}.
	 * @param ctx the parse tree
	 */
	void enterJson_value_on_error_response(OBParser.Json_value_on_error_responseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#json_value_on_error_response}.
	 * @param ctx the parse tree
	 */
	void exitJson_value_on_error_response(OBParser.Json_value_on_error_responseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#opt_on_mismatchs}.
	 * @param ctx the parse tree
	 */
	void enterOpt_on_mismatchs(OBParser.Opt_on_mismatchsContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#opt_on_mismatchs}.
	 * @param ctx the parse tree
	 */
	void exitOpt_on_mismatchs(OBParser.Opt_on_mismatchsContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#opt_on_mismatch}.
	 * @param ctx the parse tree
	 */
	void enterOpt_on_mismatch(OBParser.Opt_on_mismatchContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#opt_on_mismatch}.
	 * @param ctx the parse tree
	 */
	void exitOpt_on_mismatch(OBParser.Opt_on_mismatchContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#json_value_on_response}.
	 * @param ctx the parse tree
	 */
	void enterJson_value_on_response(OBParser.Json_value_on_responseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#json_value_on_response}.
	 * @param ctx the parse tree
	 */
	void exitJson_value_on_response(OBParser.Json_value_on_responseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#mismatch_type_list}.
	 * @param ctx the parse tree
	 */
	void enterMismatch_type_list(OBParser.Mismatch_type_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#mismatch_type_list}.
	 * @param ctx the parse tree
	 */
	void exitMismatch_type_list(OBParser.Mismatch_type_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#mismatch_type}.
	 * @param ctx the parse tree
	 */
	void enterMismatch_type(OBParser.Mismatch_typeContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#mismatch_type}.
	 * @param ctx the parse tree
	 */
	void exitMismatch_type(OBParser.Mismatch_typeContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#json_exists_expr}.
	 * @param ctx the parse tree
	 */
	void enterJson_exists_expr(OBParser.Json_exists_exprContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#json_exists_expr}.
	 * @param ctx the parse tree
	 */
	void exitJson_exists_expr(OBParser.Json_exists_exprContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#opt_json_exist}.
	 * @param ctx the parse tree
	 */
	void enterOpt_json_exist(OBParser.Opt_json_existContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#opt_json_exist}.
	 * @param ctx the parse tree
	 */
	void exitOpt_json_exist(OBParser.Opt_json_existContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#passing_elements}.
	 * @param ctx the parse tree
	 */
	void enterPassing_elements(OBParser.Passing_elementsContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#passing_elements}.
	 * @param ctx the parse tree
	 */
	void exitPassing_elements(OBParser.Passing_elementsContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#passing_context}.
	 * @param ctx the parse tree
	 */
	void enterPassing_context(OBParser.Passing_contextContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#passing_context}.
	 * @param ctx the parse tree
	 */
	void exitPassing_context(OBParser.Passing_contextContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#sql_var_name}.
	 * @param ctx the parse tree
	 */
	void enterSql_var_name(OBParser.Sql_var_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#sql_var_name}.
	 * @param ctx the parse tree
	 */
	void exitSql_var_name(OBParser.Sql_var_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#opt_json_exists_on_error_on_empty}.
	 * @param ctx the parse tree
	 */
	void enterOpt_json_exists_on_error_on_empty(OBParser.Opt_json_exists_on_error_on_emptyContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#opt_json_exists_on_error_on_empty}.
	 * @param ctx the parse tree
	 */
	void exitOpt_json_exists_on_error_on_empty(OBParser.Opt_json_exists_on_error_on_emptyContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#json_exists_on_error}.
	 * @param ctx the parse tree
	 */
	void enterJson_exists_on_error(OBParser.Json_exists_on_errorContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#json_exists_on_error}.
	 * @param ctx the parse tree
	 */
	void exitJson_exists_on_error(OBParser.Json_exists_on_errorContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#json_exists_on_empty}.
	 * @param ctx the parse tree
	 */
	void enterJson_exists_on_empty(OBParser.Json_exists_on_emptyContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#json_exists_on_empty}.
	 * @param ctx the parse tree
	 */
	void exitJson_exists_on_empty(OBParser.Json_exists_on_emptyContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#json_exists_response_type}.
	 * @param ctx the parse tree
	 */
	void enterJson_exists_response_type(OBParser.Json_exists_response_typeContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#json_exists_response_type}.
	 * @param ctx the parse tree
	 */
	void exitJson_exists_response_type(OBParser.Json_exists_response_typeContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#json_query_expr}.
	 * @param ctx the parse tree
	 */
	void enterJson_query_expr(OBParser.Json_query_exprContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#json_query_expr}.
	 * @param ctx the parse tree
	 */
	void exitJson_query_expr(OBParser.Json_query_exprContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#json_query_on_opt}.
	 * @param ctx the parse tree
	 */
	void enterJson_query_on_opt(OBParser.Json_query_on_optContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#json_query_on_opt}.
	 * @param ctx the parse tree
	 */
	void exitJson_query_on_opt(OBParser.Json_query_on_optContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#wrapper_opts}.
	 * @param ctx the parse tree
	 */
	void enterWrapper_opts(OBParser.Wrapper_optsContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#wrapper_opts}.
	 * @param ctx the parse tree
	 */
	void exitWrapper_opts(OBParser.Wrapper_optsContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#js_query_return_type}.
	 * @param ctx the parse tree
	 */
	void enterJs_query_return_type(OBParser.Js_query_return_typeContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#js_query_return_type}.
	 * @param ctx the parse tree
	 */
	void exitJs_query_return_type(OBParser.Js_query_return_typeContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#on_mismatch_query}.
	 * @param ctx the parse tree
	 */
	void enterOn_mismatch_query(OBParser.On_mismatch_queryContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#on_mismatch_query}.
	 * @param ctx the parse tree
	 */
	void exitOn_mismatch_query(OBParser.On_mismatch_queryContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#on_error_query}.
	 * @param ctx the parse tree
	 */
	void enterOn_error_query(OBParser.On_error_queryContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#on_error_query}.
	 * @param ctx the parse tree
	 */
	void exitOn_error_query(OBParser.On_error_queryContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#on_empty_query}.
	 * @param ctx the parse tree
	 */
	void enterOn_empty_query(OBParser.On_empty_queryContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#on_empty_query}.
	 * @param ctx the parse tree
	 */
	void exitOn_empty_query(OBParser.On_empty_queryContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#opt_response_query_on_empty_error}.
	 * @param ctx the parse tree
	 */
	void enterOpt_response_query_on_empty_error(OBParser.Opt_response_query_on_empty_errorContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#opt_response_query_on_empty_error}.
	 * @param ctx the parse tree
	 */
	void exitOpt_response_query_on_empty_error(OBParser.Opt_response_query_on_empty_errorContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#opt_response_query}.
	 * @param ctx the parse tree
	 */
	void enterOpt_response_query(OBParser.Opt_response_queryContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#opt_response_query}.
	 * @param ctx the parse tree
	 */
	void exitOpt_response_query(OBParser.Opt_response_queryContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#xml_table_expr}.
	 * @param ctx the parse tree
	 */
	void enterXml_table_expr(OBParser.Xml_table_exprContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#xml_table_expr}.
	 * @param ctx the parse tree
	 */
	void exitXml_table_expr(OBParser.Xml_table_exprContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#opt_columns_clause}.
	 * @param ctx the parse tree
	 */
	void enterOpt_columns_clause(OBParser.Opt_columns_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#opt_columns_clause}.
	 * @param ctx the parse tree
	 */
	void exitOpt_columns_clause(OBParser.Opt_columns_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#opt_sequence_by_ref}.
	 * @param ctx the parse tree
	 */
	void enterOpt_sequence_by_ref(OBParser.Opt_sequence_by_refContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#opt_sequence_by_ref}.
	 * @param ctx the parse tree
	 */
	void exitOpt_sequence_by_ref(OBParser.Opt_sequence_by_refContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#opt_xml_passing_clause}.
	 * @param ctx the parse tree
	 */
	void enterOpt_xml_passing_clause(OBParser.Opt_xml_passing_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#opt_xml_passing_clause}.
	 * @param ctx the parse tree
	 */
	void exitOpt_xml_passing_clause(OBParser.Opt_xml_passing_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#opt_xml_table_path}.
	 * @param ctx the parse tree
	 */
	void enterOpt_xml_table_path(OBParser.Opt_xml_table_pathContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#opt_xml_table_path}.
	 * @param ctx the parse tree
	 */
	void exitOpt_xml_table_path(OBParser.Opt_xml_table_pathContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#opt_xml_table_ns}.
	 * @param ctx the parse tree
	 */
	void enterOpt_xml_table_ns(OBParser.Opt_xml_table_nsContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#opt_xml_table_ns}.
	 * @param ctx the parse tree
	 */
	void exitOpt_xml_table_ns(OBParser.Opt_xml_table_nsContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#xml_ns_list}.
	 * @param ctx the parse tree
	 */
	void enterXml_ns_list(OBParser.Xml_ns_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#xml_ns_list}.
	 * @param ctx the parse tree
	 */
	void exitXml_ns_list(OBParser.Xml_ns_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#xml_ns}.
	 * @param ctx the parse tree
	 */
	void enterXml_ns(OBParser.Xml_nsContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#xml_ns}.
	 * @param ctx the parse tree
	 */
	void exitXml_ns(OBParser.Xml_nsContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#xml_identifier}.
	 * @param ctx the parse tree
	 */
	void enterXml_identifier(OBParser.Xml_identifierContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#xml_identifier}.
	 * @param ctx the parse tree
	 */
	void exitXml_identifier(OBParser.Xml_identifierContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#xml_table_columns_list}.
	 * @param ctx the parse tree
	 */
	void enterXml_table_columns_list(OBParser.Xml_table_columns_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#xml_table_columns_list}.
	 * @param ctx the parse tree
	 */
	void exitXml_table_columns_list(OBParser.Xml_table_columns_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#xml_table_column}.
	 * @param ctx the parse tree
	 */
	void enterXml_table_column(OBParser.Xml_table_columnContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#xml_table_column}.
	 * @param ctx the parse tree
	 */
	void exitXml_table_column(OBParser.Xml_table_columnContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#xml_table_ordinality_column_def}.
	 * @param ctx the parse tree
	 */
	void enterXml_table_ordinality_column_def(OBParser.Xml_table_ordinality_column_defContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#xml_table_ordinality_column_def}.
	 * @param ctx the parse tree
	 */
	void exitXml_table_ordinality_column_def(OBParser.Xml_table_ordinality_column_defContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#xml_table_value_column_def}.
	 * @param ctx the parse tree
	 */
	void enterXml_table_value_column_def(OBParser.Xml_table_value_column_defContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#xml_table_value_column_def}.
	 * @param ctx the parse tree
	 */
	void exitXml_table_value_column_def(OBParser.Xml_table_value_column_defContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#xml_table_query_column_def}.
	 * @param ctx the parse tree
	 */
	void enterXml_table_query_column_def(OBParser.Xml_table_query_column_defContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#xml_table_query_column_def}.
	 * @param ctx the parse tree
	 */
	void exitXml_table_query_column_def(OBParser.Xml_table_query_column_defContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#opt_seq_by_ref_with_bracket}.
	 * @param ctx the parse tree
	 */
	void enterOpt_seq_by_ref_with_bracket(OBParser.Opt_seq_by_ref_with_bracketContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#opt_seq_by_ref_with_bracket}.
	 * @param ctx the parse tree
	 */
	void exitOpt_seq_by_ref_with_bracket(OBParser.Opt_seq_by_ref_with_bracketContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#opt_xml_table_default_value}.
	 * @param ctx the parse tree
	 */
	void enterOpt_xml_table_default_value(OBParser.Opt_xml_table_default_valueContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#opt_xml_table_default_value}.
	 * @param ctx the parse tree
	 */
	void exitOpt_xml_table_default_value(OBParser.Opt_xml_table_default_valueContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#opt_json_table_on_error_on_empty}.
	 * @param ctx the parse tree
	 */
	void enterOpt_json_table_on_error_on_empty(OBParser.Opt_json_table_on_error_on_emptyContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#opt_json_table_on_error_on_empty}.
	 * @param ctx the parse tree
	 */
	void exitOpt_json_table_on_error_on_empty(OBParser.Opt_json_table_on_error_on_emptyContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#json_table_columns_def_opt}.
	 * @param ctx the parse tree
	 */
	void enterJson_table_columns_def_opt(OBParser.Json_table_columns_def_optContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#json_table_columns_def_opt}.
	 * @param ctx the parse tree
	 */
	void exitJson_table_columns_def_opt(OBParser.Json_table_columns_def_optContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#json_table_expr}.
	 * @param ctx the parse tree
	 */
	void enterJson_table_expr(OBParser.Json_table_exprContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#json_table_expr}.
	 * @param ctx the parse tree
	 */
	void exitJson_table_expr(OBParser.Json_table_exprContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#json_table_columns_def}.
	 * @param ctx the parse tree
	 */
	void enterJson_table_columns_def(OBParser.Json_table_columns_defContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#json_table_columns_def}.
	 * @param ctx the parse tree
	 */
	void exitJson_table_columns_def(OBParser.Json_table_columns_defContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#json_table_column_def}.
	 * @param ctx the parse tree
	 */
	void enterJson_table_column_def(OBParser.Json_table_column_defContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#json_table_column_def}.
	 * @param ctx the parse tree
	 */
	void exitJson_table_column_def(OBParser.Json_table_column_defContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#json_table_ordinality_column_def}.
	 * @param ctx the parse tree
	 */
	void enterJson_table_ordinality_column_def(OBParser.Json_table_ordinality_column_defContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#json_table_ordinality_column_def}.
	 * @param ctx the parse tree
	 */
	void exitJson_table_ordinality_column_def(OBParser.Json_table_ordinality_column_defContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#json_table_column_def_path}.
	 * @param ctx the parse tree
	 */
	void enterJson_table_column_def_path(OBParser.Json_table_column_def_pathContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#json_table_column_def_path}.
	 * @param ctx the parse tree
	 */
	void exitJson_table_column_def_path(OBParser.Json_table_column_def_pathContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#json_table_exists_column_def}.
	 * @param ctx the parse tree
	 */
	void enterJson_table_exists_column_def(OBParser.Json_table_exists_column_defContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#json_table_exists_column_def}.
	 * @param ctx the parse tree
	 */
	void exitJson_table_exists_column_def(OBParser.Json_table_exists_column_defContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#json_table_query_column_def}.
	 * @param ctx the parse tree
	 */
	void enterJson_table_query_column_def(OBParser.Json_table_query_column_defContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#json_table_query_column_def}.
	 * @param ctx the parse tree
	 */
	void exitJson_table_query_column_def(OBParser.Json_table_query_column_defContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#json_table_value_column_def}.
	 * @param ctx the parse tree
	 */
	void enterJson_table_value_column_def(OBParser.Json_table_value_column_defContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#json_table_value_column_def}.
	 * @param ctx the parse tree
	 */
	void exitJson_table_value_column_def(OBParser.Json_table_value_column_defContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#json_table_nested_column_def}.
	 * @param ctx the parse tree
	 */
	void enterJson_table_nested_column_def(OBParser.Json_table_nested_column_defContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#json_table_nested_column_def}.
	 * @param ctx the parse tree
	 */
	void exitJson_table_nested_column_def(OBParser.Json_table_nested_column_defContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#opt_jt_query_type}.
	 * @param ctx the parse tree
	 */
	void enterOpt_jt_query_type(OBParser.Opt_jt_query_typeContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#opt_jt_query_type}.
	 * @param ctx the parse tree
	 */
	void exitOpt_jt_query_type(OBParser.Opt_jt_query_typeContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#opt_jt_value_type}.
	 * @param ctx the parse tree
	 */
	void enterOpt_jt_value_type(OBParser.Opt_jt_value_typeContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#opt_jt_value_type}.
	 * @param ctx the parse tree
	 */
	void exitOpt_jt_value_type(OBParser.Opt_jt_value_typeContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#js_value_return_type}.
	 * @param ctx the parse tree
	 */
	void enterJs_value_return_type(OBParser.Js_value_return_typeContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#js_value_return_type}.
	 * @param ctx the parse tree
	 */
	void exitJs_value_return_type(OBParser.Js_value_return_typeContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#js_return_type}.
	 * @param ctx the parse tree
	 */
	void enterJs_return_type(OBParser.Js_return_typeContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#js_return_type}.
	 * @param ctx the parse tree
	 */
	void exitJs_return_type(OBParser.Js_return_typeContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#js_return_default_type}.
	 * @param ctx the parse tree
	 */
	void enterJs_return_default_type(OBParser.Js_return_default_typeContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#js_return_default_type}.
	 * @param ctx the parse tree
	 */
	void exitJs_return_default_type(OBParser.Js_return_default_typeContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#js_return_text_type}.
	 * @param ctx the parse tree
	 */
	void enterJs_return_text_type(OBParser.Js_return_text_typeContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#js_return_text_type}.
	 * @param ctx the parse tree
	 */
	void exitJs_return_text_type(OBParser.Js_return_text_typeContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#json_table_on_response}.
	 * @param ctx the parse tree
	 */
	void enterJson_table_on_response(OBParser.Json_table_on_responseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#json_table_on_response}.
	 * @param ctx the parse tree
	 */
	void exitJson_table_on_response(OBParser.Json_table_on_responseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#json_table_on_error}.
	 * @param ctx the parse tree
	 */
	void enterJson_table_on_error(OBParser.Json_table_on_errorContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#json_table_on_error}.
	 * @param ctx the parse tree
	 */
	void exitJson_table_on_error(OBParser.Json_table_on_errorContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#json_table_on_empty}.
	 * @param ctx the parse tree
	 */
	void enterJson_table_on_empty(OBParser.Json_table_on_emptyContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#json_table_on_empty}.
	 * @param ctx the parse tree
	 */
	void exitJson_table_on_empty(OBParser.Json_table_on_emptyContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#json_object_expr}.
	 * @param ctx the parse tree
	 */
	void enterJson_object_expr(OBParser.Json_object_exprContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#json_object_expr}.
	 * @param ctx the parse tree
	 */
	void exitJson_object_expr(OBParser.Json_object_exprContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#opt_json_object_content}.
	 * @param ctx the parse tree
	 */
	void enterOpt_json_object_content(OBParser.Opt_json_object_contentContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#opt_json_object_content}.
	 * @param ctx the parse tree
	 */
	void exitOpt_json_object_content(OBParser.Opt_json_object_contentContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#opt_json_object_clause}.
	 * @param ctx the parse tree
	 */
	void enterOpt_json_object_clause(OBParser.Opt_json_object_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#opt_json_object_clause}.
	 * @param ctx the parse tree
	 */
	void exitOpt_json_object_clause(OBParser.Opt_json_object_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#entry_op}.
	 * @param ctx the parse tree
	 */
	void enterEntry_op(OBParser.Entry_opContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#entry_op}.
	 * @param ctx the parse tree
	 */
	void exitEntry_op(OBParser.Entry_opContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#entry_set}.
	 * @param ctx the parse tree
	 */
	void enterEntry_set(OBParser.Entry_setContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#entry_set}.
	 * @param ctx the parse tree
	 */
	void exitEntry_set(OBParser.Entry_setContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#entry_obj}.
	 * @param ctx the parse tree
	 */
	void enterEntry_obj(OBParser.Entry_objContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#entry_obj}.
	 * @param ctx the parse tree
	 */
	void exitEntry_obj(OBParser.Entry_objContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#regular_entry_obj}.
	 * @param ctx the parse tree
	 */
	void enterRegular_entry_obj(OBParser.Regular_entry_objContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#regular_entry_obj}.
	 * @param ctx the parse tree
	 */
	void exitRegular_entry_obj(OBParser.Regular_entry_objContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#json_obj_literal_expr}.
	 * @param ctx the parse tree
	 */
	void enterJson_obj_literal_expr(OBParser.Json_obj_literal_exprContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#json_obj_literal_expr}.
	 * @param ctx the parse tree
	 */
	void exitJson_obj_literal_expr(OBParser.Json_obj_literal_exprContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#js_on_null}.
	 * @param ctx the parse tree
	 */
	void enterJs_on_null(OBParser.Js_on_nullContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#js_on_null}.
	 * @param ctx the parse tree
	 */
	void exitJs_on_null(OBParser.Js_on_nullContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#json_obj_returning_type}.
	 * @param ctx the parse tree
	 */
	void enterJson_obj_returning_type(OBParser.Json_obj_returning_typeContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#json_obj_returning_type}.
	 * @param ctx the parse tree
	 */
	void exitJson_obj_returning_type(OBParser.Json_obj_returning_typeContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#json_obj_unique_key}.
	 * @param ctx the parse tree
	 */
	void enterJson_obj_unique_key(OBParser.Json_obj_unique_keyContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#json_obj_unique_key}.
	 * @param ctx the parse tree
	 */
	void exitJson_obj_unique_key(OBParser.Json_obj_unique_keyContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#opt_skip_index_type_list}.
	 * @param ctx the parse tree
	 */
	void enterOpt_skip_index_type_list(OBParser.Opt_skip_index_type_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#opt_skip_index_type_list}.
	 * @param ctx the parse tree
	 */
	void exitOpt_skip_index_type_list(OBParser.Opt_skip_index_type_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#skip_index_type}.
	 * @param ctx the parse tree
	 */
	void enterSkip_index_type(OBParser.Skip_index_typeContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#skip_index_type}.
	 * @param ctx the parse tree
	 */
	void exitSkip_index_type(OBParser.Skip_index_typeContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#xmlparse_expr}.
	 * @param ctx the parse tree
	 */
	void enterXmlparse_expr(OBParser.Xmlparse_exprContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#xmlparse_expr}.
	 * @param ctx the parse tree
	 */
	void exitXmlparse_expr(OBParser.Xmlparse_exprContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#xml_text}.
	 * @param ctx the parse tree
	 */
	void enterXml_text(OBParser.Xml_textContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#xml_text}.
	 * @param ctx the parse tree
	 */
	void exitXml_text(OBParser.Xml_textContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#xml_doc_type}.
	 * @param ctx the parse tree
	 */
	void enterXml_doc_type(OBParser.Xml_doc_typeContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#xml_doc_type}.
	 * @param ctx the parse tree
	 */
	void exitXml_doc_type(OBParser.Xml_doc_typeContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#xml_element_expr}.
	 * @param ctx the parse tree
	 */
	void enterXml_element_expr(OBParser.Xml_element_exprContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#xml_element_expr}.
	 * @param ctx the parse tree
	 */
	void exitXml_element_expr(OBParser.Xml_element_exprContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#xml_tag}.
	 * @param ctx the parse tree
	 */
	void enterXml_tag(OBParser.Xml_tagContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#xml_tag}.
	 * @param ctx the parse tree
	 */
	void exitXml_tag(OBParser.Xml_tagContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#evalname_expr}.
	 * @param ctx the parse tree
	 */
	void enterEvalname_expr(OBParser.Evalname_exprContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#evalname_expr}.
	 * @param ctx the parse tree
	 */
	void exitEvalname_expr(OBParser.Evalname_exprContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#element_name}.
	 * @param ctx the parse tree
	 */
	void enterElement_name(OBParser.Element_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#element_name}.
	 * @param ctx the parse tree
	 */
	void exitElement_name(OBParser.Element_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#xml_value_clause}.
	 * @param ctx the parse tree
	 */
	void enterXml_value_clause(OBParser.Xml_value_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#xml_value_clause}.
	 * @param ctx the parse tree
	 */
	void exitXml_value_clause(OBParser.Xml_value_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#xml_value}.
	 * @param ctx the parse tree
	 */
	void enterXml_value(OBParser.Xml_valueContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#xml_value}.
	 * @param ctx the parse tree
	 */
	void exitXml_value(OBParser.Xml_valueContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#xml_attributes_expr}.
	 * @param ctx the parse tree
	 */
	void enterXml_attributes_expr(OBParser.Xml_attributes_exprContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#xml_attributes_expr}.
	 * @param ctx the parse tree
	 */
	void exitXml_attributes_expr(OBParser.Xml_attributes_exprContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#xml_attributes_value_clause}.
	 * @param ctx the parse tree
	 */
	void enterXml_attributes_value_clause(OBParser.Xml_attributes_value_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#xml_attributes_value_clause}.
	 * @param ctx the parse tree
	 */
	void exitXml_attributes_value_clause(OBParser.Xml_attributes_value_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#attributes_name_value}.
	 * @param ctx the parse tree
	 */
	void enterAttributes_name_value(OBParser.Attributes_name_valueContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#attributes_name_value}.
	 * @param ctx the parse tree
	 */
	void exitAttributes_name_value(OBParser.Attributes_name_valueContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#xml_attributes_value}.
	 * @param ctx the parse tree
	 */
	void enterXml_attributes_value(OBParser.Xml_attributes_valueContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#xml_attributes_value}.
	 * @param ctx the parse tree
	 */
	void exitXml_attributes_value(OBParser.Xml_attributes_valueContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#xml_sequence_expr}.
	 * @param ctx the parse tree
	 */
	void enterXml_sequence_expr(OBParser.Xml_sequence_exprContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#xml_sequence_expr}.
	 * @param ctx the parse tree
	 */
	void exitXml_sequence_expr(OBParser.Xml_sequence_exprContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#insert_child_xml}.
	 * @param ctx the parse tree
	 */
	void enterInsert_child_xml(OBParser.Insert_child_xmlContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#insert_child_xml}.
	 * @param ctx the parse tree
	 */
	void exitInsert_child_xml(OBParser.Insert_child_xmlContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#delete_xml}.
	 * @param ctx the parse tree
	 */
	void enterDelete_xml(OBParser.Delete_xmlContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#delete_xml}.
	 * @param ctx the parse tree
	 */
	void exitDelete_xml(OBParser.Delete_xmlContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#xml_extract_expr}.
	 * @param ctx the parse tree
	 */
	void enterXml_extract_expr(OBParser.Xml_extract_exprContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#xml_extract_expr}.
	 * @param ctx the parse tree
	 */
	void exitXml_extract_expr(OBParser.Xml_extract_exprContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#xmlcast_expr}.
	 * @param ctx the parse tree
	 */
	void enterXmlcast_expr(OBParser.Xmlcast_exprContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#xmlcast_expr}.
	 * @param ctx the parse tree
	 */
	void exitXmlcast_expr(OBParser.Xmlcast_exprContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#xmlserialize_expr}.
	 * @param ctx the parse tree
	 */
	void enterXmlserialize_expr(OBParser.Xmlserialize_exprContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#xmlserialize_expr}.
	 * @param ctx the parse tree
	 */
	void exitXmlserialize_expr(OBParser.Xmlserialize_exprContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#unreserved_keyword}.
	 * @param ctx the parse tree
	 */
	void enterUnreserved_keyword(OBParser.Unreserved_keywordContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#unreserved_keyword}.
	 * @param ctx the parse tree
	 */
	void exitUnreserved_keyword(OBParser.Unreserved_keywordContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#aggregate_function_keyword}.
	 * @param ctx the parse tree
	 */
	void enterAggregate_function_keyword(OBParser.Aggregate_function_keywordContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#aggregate_function_keyword}.
	 * @param ctx the parse tree
	 */
	void exitAggregate_function_keyword(OBParser.Aggregate_function_keywordContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#oracle_unreserved_keyword}.
	 * @param ctx the parse tree
	 */
	void enterOracle_unreserved_keyword(OBParser.Oracle_unreserved_keywordContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#oracle_unreserved_keyword}.
	 * @param ctx the parse tree
	 */
	void exitOracle_unreserved_keyword(OBParser.Oracle_unreserved_keywordContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#unreserved_keyword_normal}.
	 * @param ctx the parse tree
	 */
	void enterUnreserved_keyword_normal(OBParser.Unreserved_keyword_normalContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#unreserved_keyword_normal}.
	 * @param ctx the parse tree
	 */
	void exitUnreserved_keyword_normal(OBParser.Unreserved_keyword_normalContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#empty}.
	 * @param ctx the parse tree
	 */
	void enterEmpty(OBParser.EmptyContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#empty}.
	 * @param ctx the parse tree
	 */
	void exitEmpty(OBParser.EmptyContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#forward_expr}.
	 * @param ctx the parse tree
	 */
	void enterForward_expr(OBParser.Forward_exprContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#forward_expr}.
	 * @param ctx the parse tree
	 */
	void exitForward_expr(OBParser.Forward_exprContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#forward_sql_stmt}.
	 * @param ctx the parse tree
	 */
	void enterForward_sql_stmt(OBParser.Forward_sql_stmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#forward_sql_stmt}.
	 * @param ctx the parse tree
	 */
	void exitForward_sql_stmt(OBParser.Forward_sql_stmtContext ctx);
}