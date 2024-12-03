// Generated from OBParser.g4 by ANTLR 4.9.2
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
	 * Enter a parse tree produced by {@link OBParser#expr_as_list}.
	 * @param ctx the parse tree
	 */
	void enterExpr_as_list(OBParser.Expr_as_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#expr_as_list}.
	 * @param ctx the parse tree
	 */
	void exitExpr_as_list(OBParser.Expr_as_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#expr_with_opt_alias}.
	 * @param ctx the parse tree
	 */
	void enterExpr_with_opt_alias(OBParser.Expr_with_opt_aliasContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#expr_with_opt_alias}.
	 * @param ctx the parse tree
	 */
	void exitExpr_with_opt_alias(OBParser.Expr_with_opt_aliasContext ctx);
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
	 * Enter a parse tree produced by {@link OBParser#charset_introducer}.
	 * @param ctx the parse tree
	 */
	void enterCharset_introducer(OBParser.Charset_introducerContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#charset_introducer}.
	 * @param ctx the parse tree
	 */
	void exitCharset_introducer(OBParser.Charset_introducerContext ctx);
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
	 * Enter a parse tree produced by {@link OBParser#global_or_session_alias}.
	 * @param ctx the parse tree
	 */
	void enterGlobal_or_session_alias(OBParser.Global_or_session_aliasContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#global_or_session_alias}.
	 * @param ctx the parse tree
	 */
	void exitGlobal_or_session_alias(OBParser.Global_or_session_aliasContext ctx);
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
	 * Enter a parse tree produced by {@link OBParser#string_val_list}.
	 * @param ctx the parse tree
	 */
	void enterString_val_list(OBParser.String_val_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#string_val_list}.
	 * @param ctx the parse tree
	 */
	void exitString_val_list(OBParser.String_val_listContext ctx);
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
	 * Enter a parse tree produced by {@link OBParser#any_expr}.
	 * @param ctx the parse tree
	 */
	void enterAny_expr(OBParser.Any_exprContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#any_expr}.
	 * @param ctx the parse tree
	 */
	void exitAny_expr(OBParser.Any_exprContext ctx);
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
	 * Enter a parse tree produced by {@link OBParser#new_generalized_window_clause}.
	 * @param ctx the parse tree
	 */
	void enterNew_generalized_window_clause(OBParser.New_generalized_window_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#new_generalized_window_clause}.
	 * @param ctx the parse tree
	 */
	void exitNew_generalized_window_clause(OBParser.New_generalized_window_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#new_generalized_window_clause_with_blanket}.
	 * @param ctx the parse tree
	 */
	void enterNew_generalized_window_clause_with_blanket(OBParser.New_generalized_window_clause_with_blanketContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#new_generalized_window_clause_with_blanket}.
	 * @param ctx the parse tree
	 */
	void exitNew_generalized_window_clause_with_blanket(OBParser.New_generalized_window_clause_with_blanketContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#named_windows}.
	 * @param ctx the parse tree
	 */
	void enterNamed_windows(OBParser.Named_windowsContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#named_windows}.
	 * @param ctx the parse tree
	 */
	void exitNamed_windows(OBParser.Named_windowsContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#named_window}.
	 * @param ctx the parse tree
	 */
	void enterNamed_window(OBParser.Named_windowContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#named_window}.
	 * @param ctx the parse tree
	 */
	void exitNamed_window(OBParser.Named_windowContext ctx);
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
	 * Enter a parse tree produced by {@link OBParser#when_clause_list}.
	 * @param ctx the parse tree
	 */
	void enterWhen_clause_list(OBParser.When_clause_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#when_clause_list}.
	 * @param ctx the parse tree
	 */
	void exitWhen_clause_list(OBParser.When_clause_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#when_clause}.
	 * @param ctx the parse tree
	 */
	void enterWhen_clause(OBParser.When_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#when_clause}.
	 * @param ctx the parse tree
	 */
	void exitWhen_clause(OBParser.When_clauseContext ctx);
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
	 * Enter a parse tree produced by the {@code simple_func_expr}
	 * labeled alternative in {@link OBParser#func_expr}.
	 * @param ctx the parse tree
	 */
	void enterSimple_func_expr(OBParser.Simple_func_exprContext ctx);
	/**
	 * Exit a parse tree produced by the {@code simple_func_expr}
	 * labeled alternative in {@link OBParser#func_expr}.
	 * @param ctx the parse tree
	 */
	void exitSimple_func_expr(OBParser.Simple_func_exprContext ctx);
	/**
	 * Enter a parse tree produced by the {@code complex_func_expr}
	 * labeled alternative in {@link OBParser#func_expr}.
	 * @param ctx the parse tree
	 */
	void enterComplex_func_expr(OBParser.Complex_func_exprContext ctx);
	/**
	 * Exit a parse tree produced by the {@code complex_func_expr}
	 * labeled alternative in {@link OBParser#func_expr}.
	 * @param ctx the parse tree
	 */
	void exitComplex_func_expr(OBParser.Complex_func_exprContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#vector_distance_expr}.
	 * @param ctx the parse tree
	 */
	void enterVector_distance_expr(OBParser.Vector_distance_exprContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#vector_distance_expr}.
	 * @param ctx the parse tree
	 */
	void exitVector_distance_expr(OBParser.Vector_distance_exprContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#vector_distance_metric}.
	 * @param ctx the parse tree
	 */
	void enterVector_distance_metric(OBParser.Vector_distance_metricContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#vector_distance_metric}.
	 * @param ctx the parse tree
	 */
	void exitVector_distance_metric(OBParser.Vector_distance_metricContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#mvt_param}.
	 * @param ctx the parse tree
	 */
	void enterMvt_param(OBParser.Mvt_paramContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#mvt_param}.
	 * @param ctx the parse tree
	 */
	void exitMvt_param(OBParser.Mvt_paramContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#sys_interval_func}.
	 * @param ctx the parse tree
	 */
	void enterSys_interval_func(OBParser.Sys_interval_funcContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#sys_interval_func}.
	 * @param ctx the parse tree
	 */
	void exitSys_interval_func(OBParser.Sys_interval_funcContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#utc_timestamp_func}.
	 * @param ctx the parse tree
	 */
	void enterUtc_timestamp_func(OBParser.Utc_timestamp_funcContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#utc_timestamp_func}.
	 * @param ctx the parse tree
	 */
	void exitUtc_timestamp_func(OBParser.Utc_timestamp_funcContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#utc_time_func}.
	 * @param ctx the parse tree
	 */
	void enterUtc_time_func(OBParser.Utc_time_funcContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#utc_time_func}.
	 * @param ctx the parse tree
	 */
	void exitUtc_time_func(OBParser.Utc_time_funcContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#utc_date_func}.
	 * @param ctx the parse tree
	 */
	void enterUtc_date_func(OBParser.Utc_date_funcContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#utc_date_func}.
	 * @param ctx the parse tree
	 */
	void exitUtc_date_func(OBParser.Utc_date_funcContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#sysdate_func}.
	 * @param ctx the parse tree
	 */
	void enterSysdate_func(OBParser.Sysdate_funcContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#sysdate_func}.
	 * @param ctx the parse tree
	 */
	void exitSysdate_func(OBParser.Sysdate_funcContext ctx);
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
	 * Enter a parse tree produced by {@link OBParser#now_synonyms_func}.
	 * @param ctx the parse tree
	 */
	void enterNow_synonyms_func(OBParser.Now_synonyms_funcContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#now_synonyms_func}.
	 * @param ctx the parse tree
	 */
	void exitNow_synonyms_func(OBParser.Now_synonyms_funcContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#cur_time_func}.
	 * @param ctx the parse tree
	 */
	void enterCur_time_func(OBParser.Cur_time_funcContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#cur_time_func}.
	 * @param ctx the parse tree
	 */
	void exitCur_time_func(OBParser.Cur_time_funcContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#cur_date_func}.
	 * @param ctx the parse tree
	 */
	void enterCur_date_func(OBParser.Cur_date_funcContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#cur_date_func}.
	 * @param ctx the parse tree
	 */
	void exitCur_date_func(OBParser.Cur_date_funcContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#substr_or_substring}.
	 * @param ctx the parse tree
	 */
	void enterSubstr_or_substring(OBParser.Substr_or_substringContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#substr_or_substring}.
	 * @param ctx the parse tree
	 */
	void exitSubstr_or_substring(OBParser.Substr_or_substringContext ctx);
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
	 * Enter a parse tree produced by {@link OBParser#date_params}.
	 * @param ctx the parse tree
	 */
	void enterDate_params(OBParser.Date_paramsContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#date_params}.
	 * @param ctx the parse tree
	 */
	void exitDate_params(OBParser.Date_paramsContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#timestamp_params}.
	 * @param ctx the parse tree
	 */
	void enterTimestamp_params(OBParser.Timestamp_paramsContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#timestamp_params}.
	 * @param ctx the parse tree
	 */
	void exitTimestamp_params(OBParser.Timestamp_paramsContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#ws_level_list_or_range}.
	 * @param ctx the parse tree
	 */
	void enterWs_level_list_or_range(OBParser.Ws_level_list_or_rangeContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#ws_level_list_or_range}.
	 * @param ctx the parse tree
	 */
	void exitWs_level_list_or_range(OBParser.Ws_level_list_or_rangeContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#ws_level_list}.
	 * @param ctx the parse tree
	 */
	void enterWs_level_list(OBParser.Ws_level_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#ws_level_list}.
	 * @param ctx the parse tree
	 */
	void exitWs_level_list(OBParser.Ws_level_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#ws_level_list_item}.
	 * @param ctx the parse tree
	 */
	void enterWs_level_list_item(OBParser.Ws_level_list_itemContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#ws_level_list_item}.
	 * @param ctx the parse tree
	 */
	void exitWs_level_list_item(OBParser.Ws_level_list_itemContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#ws_level_range}.
	 * @param ctx the parse tree
	 */
	void enterWs_level_range(OBParser.Ws_level_rangeContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#ws_level_range}.
	 * @param ctx the parse tree
	 */
	void exitWs_level_range(OBParser.Ws_level_rangeContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#ws_level_number}.
	 * @param ctx the parse tree
	 */
	void enterWs_level_number(OBParser.Ws_level_numberContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#ws_level_number}.
	 * @param ctx the parse tree
	 */
	void exitWs_level_number(OBParser.Ws_level_numberContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#ws_level_flags}.
	 * @param ctx the parse tree
	 */
	void enterWs_level_flags(OBParser.Ws_level_flagsContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#ws_level_flags}.
	 * @param ctx the parse tree
	 */
	void exitWs_level_flags(OBParser.Ws_level_flagsContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#ws_nweights}.
	 * @param ctx the parse tree
	 */
	void enterWs_nweights(OBParser.Ws_nweightsContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#ws_nweights}.
	 * @param ctx the parse tree
	 */
	void exitWs_nweights(OBParser.Ws_nweightsContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#ws_level_flag_desc}.
	 * @param ctx the parse tree
	 */
	void enterWs_level_flag_desc(OBParser.Ws_level_flag_descContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#ws_level_flag_desc}.
	 * @param ctx the parse tree
	 */
	void exitWs_level_flag_desc(OBParser.Ws_level_flag_descContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#ws_level_flag_reverse}.
	 * @param ctx the parse tree
	 */
	void enterWs_level_flag_reverse(OBParser.Ws_level_flag_reverseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#ws_level_flag_reverse}.
	 * @param ctx the parse tree
	 */
	void exitWs_level_flag_reverse(OBParser.Ws_level_flag_reverseContext ctx);
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
	 * Enter a parse tree produced by {@link OBParser#delete_basic_stmt}.
	 * @param ctx the parse tree
	 */
	void enterDelete_basic_stmt(OBParser.Delete_basic_stmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#delete_basic_stmt}.
	 * @param ctx the parse tree
	 */
	void exitDelete_basic_stmt(OBParser.Delete_basic_stmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#delete_option_list}.
	 * @param ctx the parse tree
	 */
	void enterDelete_option_list(OBParser.Delete_option_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#delete_option_list}.
	 * @param ctx the parse tree
	 */
	void exitDelete_option_list(OBParser.Delete_option_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#delete_option}.
	 * @param ctx the parse tree
	 */
	void enterDelete_option(OBParser.Delete_optionContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#delete_option}.
	 * @param ctx the parse tree
	 */
	void exitDelete_option(OBParser.Delete_optionContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#multi_delete_table}.
	 * @param ctx the parse tree
	 */
	void enterMulti_delete_table(OBParser.Multi_delete_tableContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#multi_delete_table}.
	 * @param ctx the parse tree
	 */
	void exitMulti_delete_table(OBParser.Multi_delete_tableContext ctx);
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
	 * Enter a parse tree produced by {@link OBParser#update_basic_stmt}.
	 * @param ctx the parse tree
	 */
	void enterUpdate_basic_stmt(OBParser.Update_basic_stmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#update_basic_stmt}.
	 * @param ctx the parse tree
	 */
	void exitUpdate_basic_stmt(OBParser.Update_basic_stmtContext ctx);
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
	 * Enter a parse tree produced by {@link OBParser#unit_id_list}.
	 * @param ctx the parse tree
	 */
	void enterUnit_id_list(OBParser.Unit_id_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#unit_id_list}.
	 * @param ctx the parse tree
	 */
	void exitUnit_id_list(OBParser.Unit_id_listContext ctx);
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
	 * Enter a parse tree produced by {@link OBParser#create_standby_tenant_stmt}.
	 * @param ctx the parse tree
	 */
	void enterCreate_standby_tenant_stmt(OBParser.Create_standby_tenant_stmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#create_standby_tenant_stmt}.
	 * @param ctx the parse tree
	 */
	void exitCreate_standby_tenant_stmt(OBParser.Create_standby_tenant_stmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#log_restore_source_option}.
	 * @param ctx the parse tree
	 */
	void enterLog_restore_source_option(OBParser.Log_restore_source_optionContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#log_restore_source_option}.
	 * @param ctx the parse tree
	 */
	void exitLog_restore_source_option(OBParser.Log_restore_source_optionContext ctx);
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
	 * Enter a parse tree produced by {@link OBParser#create_tenant_snapshot_stmt}.
	 * @param ctx the parse tree
	 */
	void enterCreate_tenant_snapshot_stmt(OBParser.Create_tenant_snapshot_stmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#create_tenant_snapshot_stmt}.
	 * @param ctx the parse tree
	 */
	void exitCreate_tenant_snapshot_stmt(OBParser.Create_tenant_snapshot_stmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#snapshot_name}.
	 * @param ctx the parse tree
	 */
	void enterSnapshot_name(OBParser.Snapshot_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#snapshot_name}.
	 * @param ctx the parse tree
	 */
	void exitSnapshot_name(OBParser.Snapshot_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#drop_tenant_snapshot_stmt}.
	 * @param ctx the parse tree
	 */
	void enterDrop_tenant_snapshot_stmt(OBParser.Drop_tenant_snapshot_stmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#drop_tenant_snapshot_stmt}.
	 * @param ctx the parse tree
	 */
	void exitDrop_tenant_snapshot_stmt(OBParser.Drop_tenant_snapshot_stmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#clone_tenant_stmt}.
	 * @param ctx the parse tree
	 */
	void enterClone_tenant_stmt(OBParser.Clone_tenant_stmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#clone_tenant_stmt}.
	 * @param ctx the parse tree
	 */
	void exitClone_tenant_stmt(OBParser.Clone_tenant_stmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#clone_snapshot_option}.
	 * @param ctx the parse tree
	 */
	void enterClone_snapshot_option(OBParser.Clone_snapshot_optionContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#clone_snapshot_option}.
	 * @param ctx the parse tree
	 */
	void exitClone_snapshot_option(OBParser.Clone_snapshot_optionContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#clone_tenant_option}.
	 * @param ctx the parse tree
	 */
	void enterClone_tenant_option(OBParser.Clone_tenant_optionContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#clone_tenant_option}.
	 * @param ctx the parse tree
	 */
	void exitClone_tenant_option(OBParser.Clone_tenant_optionContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#clone_tenant_option_list}.
	 * @param ctx the parse tree
	 */
	void enterClone_tenant_option_list(OBParser.Clone_tenant_option_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#clone_tenant_option_list}.
	 * @param ctx the parse tree
	 */
	void exitClone_tenant_option_list(OBParser.Clone_tenant_option_listContext ctx);
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
	 * Enter a parse tree produced by {@link OBParser#create_database_stmt}.
	 * @param ctx the parse tree
	 */
	void enterCreate_database_stmt(OBParser.Create_database_stmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#create_database_stmt}.
	 * @param ctx the parse tree
	 */
	void exitCreate_database_stmt(OBParser.Create_database_stmtContext ctx);
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
	 * Enter a parse tree produced by {@link OBParser#databases_expr}.
	 * @param ctx the parse tree
	 */
	void enterDatabases_expr(OBParser.Databases_exprContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#databases_expr}.
	 * @param ctx the parse tree
	 */
	void exitDatabases_expr(OBParser.Databases_exprContext ctx);
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
	 * Enter a parse tree produced by {@link OBParser#drop_database_stmt}.
	 * @param ctx the parse tree
	 */
	void enterDrop_database_stmt(OBParser.Drop_database_stmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#drop_database_stmt}.
	 * @param ctx the parse tree
	 */
	void exitDrop_database_stmt(OBParser.Drop_database_stmtContext ctx);
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
	 * Enter a parse tree produced by {@link OBParser#use_database_stmt}.
	 * @param ctx the parse tree
	 */
	void enterUse_database_stmt(OBParser.Use_database_stmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#use_database_stmt}.
	 * @param ctx the parse tree
	 */
	void exitUse_database_stmt(OBParser.Use_database_stmtContext ctx);
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
	 * Enter a parse tree produced by {@link OBParser#create_table_like_stmt}.
	 * @param ctx the parse tree
	 */
	void enterCreate_table_like_stmt(OBParser.Create_table_like_stmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#create_table_like_stmt}.
	 * @param ctx the parse tree
	 */
	void exitCreate_table_like_stmt(OBParser.Create_table_like_stmtContext ctx);
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
	 * Enter a parse tree produced by {@link OBParser#ignore_or_replace}.
	 * @param ctx the parse tree
	 */
	void enterIgnore_or_replace(OBParser.Ignore_or_replaceContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#ignore_or_replace}.
	 * @param ctx the parse tree
	 */
	void exitIgnore_or_replace(OBParser.Ignore_or_replaceContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#ret_type}.
	 * @param ctx the parse tree
	 */
	void enterRet_type(OBParser.Ret_typeContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#ret_type}.
	 * @param ctx the parse tree
	 */
	void exitRet_type(OBParser.Ret_typeContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#create_function_stmt}.
	 * @param ctx the parse tree
	 */
	void enterCreate_function_stmt(OBParser.Create_function_stmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#create_function_stmt}.
	 * @param ctx the parse tree
	 */
	void exitCreate_function_stmt(OBParser.Create_function_stmtContext ctx);
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
	 * Enter a parse tree produced by {@link OBParser#opt_reference_option_list}.
	 * @param ctx the parse tree
	 */
	void enterOpt_reference_option_list(OBParser.Opt_reference_option_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#opt_reference_option_list}.
	 * @param ctx the parse tree
	 */
	void exitOpt_reference_option_list(OBParser.Opt_reference_option_listContext ctx);
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
	 * Enter a parse tree produced by {@link OBParser#match_action}.
	 * @param ctx the parse tree
	 */
	void enterMatch_action(OBParser.Match_actionContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#match_action}.
	 * @param ctx the parse tree
	 */
	void exitMatch_action(OBParser.Match_actionContext ctx);
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
	 * Enter a parse tree produced by {@link OBParser#get_format_unit}.
	 * @param ctx the parse tree
	 */
	void enterGet_format_unit(OBParser.Get_format_unitContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#get_format_unit}.
	 * @param ctx the parse tree
	 */
	void exitGet_format_unit(OBParser.Get_format_unitContext ctx);
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
	 * Enter a parse tree produced by {@link OBParser#string_list}.
	 * @param ctx the parse tree
	 */
	void enterString_list(OBParser.String_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#string_list}.
	 * @param ctx the parse tree
	 */
	void exitString_list(OBParser.String_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#text_string}.
	 * @param ctx the parse tree
	 */
	void enterText_string(OBParser.Text_stringContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#text_string}.
	 * @param ctx the parse tree
	 */
	void exitText_string(OBParser.Text_stringContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#collection_type_i}.
	 * @param ctx the parse tree
	 */
	void enterCollection_type_i(OBParser.Collection_type_iContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#collection_type_i}.
	 * @param ctx the parse tree
	 */
	void exitCollection_type_i(OBParser.Collection_type_iContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#json_type_i}.
	 * @param ctx the parse tree
	 */
	void enterJson_type_i(OBParser.Json_type_iContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#json_type_i}.
	 * @param ctx the parse tree
	 */
	void exitJson_type_i(OBParser.Json_type_iContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#roaringbitmap_type_i}.
	 * @param ctx the parse tree
	 */
	void enterRoaringbitmap_type_i(OBParser.Roaringbitmap_type_iContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#roaringbitmap_type_i}.
	 * @param ctx the parse tree
	 */
	void exitRoaringbitmap_type_i(OBParser.Roaringbitmap_type_iContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#vector_type_i}.
	 * @param ctx the parse tree
	 */
	void enterVector_type_i(OBParser.Vector_type_iContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#vector_type_i}.
	 * @param ctx the parse tree
	 */
	void exitVector_type_i(OBParser.Vector_type_iContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#bit_type_i}.
	 * @param ctx the parse tree
	 */
	void enterBit_type_i(OBParser.Bit_type_iContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#bit_type_i}.
	 * @param ctx the parse tree
	 */
	void exitBit_type_i(OBParser.Bit_type_iContext ctx);
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
	 * Enter a parse tree produced by {@link OBParser#text_type_i}.
	 * @param ctx the parse tree
	 */
	void enterText_type_i(OBParser.Text_type_iContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#text_type_i}.
	 * @param ctx the parse tree
	 */
	void exitText_type_i(OBParser.Text_type_iContext ctx);
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
	 * Enter a parse tree produced by {@link OBParser#bool_type_i}.
	 * @param ctx the parse tree
	 */
	void enterBool_type_i(OBParser.Bool_type_iContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#bool_type_i}.
	 * @param ctx the parse tree
	 */
	void exitBool_type_i(OBParser.Bool_type_iContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#geo_type_i}.
	 * @param ctx the parse tree
	 */
	void enterGeo_type_i(OBParser.Geo_type_iContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#geo_type_i}.
	 * @param ctx the parse tree
	 */
	void exitGeo_type_i(OBParser.Geo_type_iContext ctx);
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
	 * Enter a parse tree produced by {@link OBParser#date_year_type_i}.
	 * @param ctx the parse tree
	 */
	void enterDate_year_type_i(OBParser.Date_year_type_iContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#date_year_type_i}.
	 * @param ctx the parse tree
	 */
	void exitDate_year_type_i(OBParser.Date_year_type_iContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#blob_type_i}.
	 * @param ctx the parse tree
	 */
	void enterBlob_type_i(OBParser.Blob_type_iContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#blob_type_i}.
	 * @param ctx the parse tree
	 */
	void exitBlob_type_i(OBParser.Blob_type_iContext ctx);
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
	 * Enter a parse tree produced by {@link OBParser#merge_insert_types}.
	 * @param ctx the parse tree
	 */
	void enterMerge_insert_types(OBParser.Merge_insert_typesContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#merge_insert_types}.
	 * @param ctx the parse tree
	 */
	void exitMerge_insert_types(OBParser.Merge_insert_typesContext ctx);
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
	 * Enter a parse tree produced by {@link OBParser#ttl_definition}.
	 * @param ctx the parse tree
	 */
	void enterTtl_definition(OBParser.Ttl_definitionContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#ttl_definition}.
	 * @param ctx the parse tree
	 */
	void exitTtl_definition(OBParser.Ttl_definitionContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#ttl_expr}.
	 * @param ctx the parse tree
	 */
	void enterTtl_expr(OBParser.Ttl_exprContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#ttl_expr}.
	 * @param ctx the parse tree
	 */
	void exitTtl_expr(OBParser.Ttl_exprContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#ttl_unit}.
	 * @param ctx the parse tree
	 */
	void enterTtl_unit(OBParser.Ttl_unitContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#ttl_unit}.
	 * @param ctx the parse tree
	 */
	void exitTtl_unit(OBParser.Ttl_unitContext ctx);
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
	 * Enter a parse tree produced by {@link OBParser#key_partition_option}.
	 * @param ctx the parse tree
	 */
	void enterKey_partition_option(OBParser.Key_partition_optionContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#key_partition_option}.
	 * @param ctx the parse tree
	 */
	void exitKey_partition_option(OBParser.Key_partition_optionContext ctx);
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
	 * Enter a parse tree produced by {@link OBParser#partition_options}.
	 * @param ctx the parse tree
	 */
	void enterPartition_options(OBParser.Partition_optionsContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#partition_options}.
	 * @param ctx the parse tree
	 */
	void exitPartition_options(OBParser.Partition_optionsContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#partition_num}.
	 * @param ctx the parse tree
	 */
	void enterPartition_num(OBParser.Partition_numContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#partition_num}.
	 * @param ctx the parse tree
	 */
	void exitPartition_num(OBParser.Partition_numContext ctx);
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
	 * Enter a parse tree produced by {@link OBParser#opt_hash_partition_list}.
	 * @param ctx the parse tree
	 */
	void enterOpt_hash_partition_list(OBParser.Opt_hash_partition_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#opt_hash_partition_list}.
	 * @param ctx the parse tree
	 */
	void exitOpt_hash_partition_list(OBParser.Opt_hash_partition_listContext ctx);
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
	 * Enter a parse tree produced by {@link OBParser#partition_attributes_option}.
	 * @param ctx the parse tree
	 */
	void enterPartition_attributes_option(OBParser.Partition_attributes_optionContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#partition_attributes_option}.
	 * @param ctx the parse tree
	 */
	void exitPartition_attributes_option(OBParser.Partition_attributes_optionContext ctx);
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
	 * Enter a parse tree produced by {@link OBParser#tg_key_partition_option}.
	 * @param ctx the parse tree
	 */
	void enterTg_key_partition_option(OBParser.Tg_key_partition_optionContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#tg_key_partition_option}.
	 * @param ctx the parse tree
	 */
	void exitTg_key_partition_option(OBParser.Tg_key_partition_optionContext ctx);
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
	 * Enter a parse tree produced by {@link OBParser#row_format_option}.
	 * @param ctx the parse tree
	 */
	void enterRow_format_option(OBParser.Row_format_optionContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#row_format_option}.
	 * @param ctx the parse tree
	 */
	void exitRow_format_option(OBParser.Row_format_optionContext ctx);
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
	 * Enter a parse tree produced by {@link OBParser#external_properties_key}.
	 * @param ctx the parse tree
	 */
	void enterExternal_properties_key(OBParser.External_properties_keyContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#external_properties_key}.
	 * @param ctx the parse tree
	 */
	void exitExternal_properties_key(OBParser.External_properties_keyContext ctx);
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
	 * Enter a parse tree produced by {@link OBParser#view_attribute}.
	 * @param ctx the parse tree
	 */
	void enterView_attribute(OBParser.View_attributeContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#view_attribute}.
	 * @param ctx the parse tree
	 */
	void exitView_attribute(OBParser.View_attributeContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#view_check_option}.
	 * @param ctx the parse tree
	 */
	void enterView_check_option(OBParser.View_check_optionContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#view_check_option}.
	 * @param ctx the parse tree
	 */
	void exitView_check_option(OBParser.View_check_optionContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#view_algorithm}.
	 * @param ctx the parse tree
	 */
	void enterView_algorithm(OBParser.View_algorithmContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#view_algorithm}.
	 * @param ctx the parse tree
	 */
	void exitView_algorithm(OBParser.View_algorithmContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#view_select_stmt}.
	 * @param ctx the parse tree
	 */
	void enterView_select_stmt(OBParser.View_select_stmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#view_select_stmt}.
	 * @param ctx the parse tree
	 */
	void exitView_select_stmt(OBParser.View_select_stmtContext ctx);
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
	 * Enter a parse tree produced by {@link OBParser#check_state}.
	 * @param ctx the parse tree
	 */
	void enterCheck_state(OBParser.Check_stateContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#check_state}.
	 * @param ctx the parse tree
	 */
	void exitCheck_state(OBParser.Check_stateContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#opt_constraint_name}.
	 * @param ctx the parse tree
	 */
	void enterOpt_constraint_name(OBParser.Opt_constraint_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#opt_constraint_name}.
	 * @param ctx the parse tree
	 */
	void exitOpt_constraint_name(OBParser.Opt_constraint_nameContext ctx);
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
	 * Enter a parse tree produced by {@link OBParser#value_or_values}.
	 * @param ctx the parse tree
	 */
	void enterValue_or_values(OBParser.Value_or_valuesContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#value_or_values}.
	 * @param ctx the parse tree
	 */
	void exitValue_or_values(OBParser.Value_or_valuesContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#replace_with_opt_hint}.
	 * @param ctx the parse tree
	 */
	void enterReplace_with_opt_hint(OBParser.Replace_with_opt_hintContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#replace_with_opt_hint}.
	 * @param ctx the parse tree
	 */
	void exitReplace_with_opt_hint(OBParser.Replace_with_opt_hintContext ctx);
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
	 * Enter a parse tree produced by {@link OBParser#no_table_select}.
	 * @param ctx the parse tree
	 */
	void enterNo_table_select(OBParser.No_table_selectContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#no_table_select}.
	 * @param ctx the parse tree
	 */
	void exitNo_table_select(OBParser.No_table_selectContext ctx);
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
	 * Enter a parse tree produced by {@link OBParser#select_clause_set_with_order_and_limit}.
	 * @param ctx the parse tree
	 */
	void enterSelect_clause_set_with_order_and_limit(OBParser.Select_clause_set_with_order_and_limitContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#select_clause_set_with_order_and_limit}.
	 * @param ctx the parse tree
	 */
	void exitSelect_clause_set_with_order_and_limit(OBParser.Select_clause_set_with_order_and_limitContext ctx);
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
	 * Enter a parse tree produced by {@link OBParser#no_table_select_with_order_and_limit}.
	 * @param ctx the parse tree
	 */
	void enterNo_table_select_with_order_and_limit(OBParser.No_table_select_with_order_and_limitContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#no_table_select_with_order_and_limit}.
	 * @param ctx the parse tree
	 */
	void exitNo_table_select_with_order_and_limit(OBParser.No_table_select_with_order_and_limitContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#opt_approx}.
	 * @param ctx the parse tree
	 */
	void enterOpt_approx(OBParser.Opt_approxContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#opt_approx}.
	 * @param ctx the parse tree
	 */
	void exitOpt_approx(OBParser.Opt_approxContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#simple_select_with_order_and_limit}.
	 * @param ctx the parse tree
	 */
	void enterSimple_select_with_order_and_limit(OBParser.Simple_select_with_order_and_limitContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#simple_select_with_order_and_limit}.
	 * @param ctx the parse tree
	 */
	void exitSimple_select_with_order_and_limit(OBParser.Simple_select_with_order_and_limitContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#select_with_parens_with_order_and_limit}.
	 * @param ctx the parse tree
	 */
	void enterSelect_with_parens_with_order_and_limit(OBParser.Select_with_parens_with_order_and_limitContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#select_with_parens_with_order_and_limit}.
	 * @param ctx the parse tree
	 */
	void exitSelect_with_parens_with_order_and_limit(OBParser.Select_with_parens_with_order_and_limitContext ctx);
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
	 * Enter a parse tree produced by {@link OBParser#opt_hint_value}.
	 * @param ctx the parse tree
	 */
	void enterOpt_hint_value(OBParser.Opt_hint_valueContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#opt_hint_value}.
	 * @param ctx the parse tree
	 */
	void exitOpt_hint_value(OBParser.Opt_hint_valueContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#limit_clause}.
	 * @param ctx the parse tree
	 */
	void enterLimit_clause(OBParser.Limit_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#limit_clause}.
	 * @param ctx the parse tree
	 */
	void exitLimit_clause(OBParser.Limit_clauseContext ctx);
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
	 * Enter a parse tree produced by {@link OBParser#limit_expr}.
	 * @param ctx the parse tree
	 */
	void enterLimit_expr(OBParser.Limit_exprContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#limit_expr}.
	 * @param ctx the parse tree
	 */
	void exitLimit_expr(OBParser.Limit_exprContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#for_update_clause}.
	 * @param ctx the parse tree
	 */
	void enterFor_update_clause(OBParser.For_update_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#for_update_clause}.
	 * @param ctx the parse tree
	 */
	void exitFor_update_clause(OBParser.For_update_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#opt_lock_in_share_mode}.
	 * @param ctx the parse tree
	 */
	void enterOpt_lock_in_share_mode(OBParser.Opt_lock_in_share_modeContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#opt_lock_in_share_mode}.
	 * @param ctx the parse tree
	 */
	void exitOpt_lock_in_share_mode(OBParser.Opt_lock_in_share_modeContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#opt_for_update_wait}.
	 * @param ctx the parse tree
	 */
	void enterOpt_for_update_wait(OBParser.Opt_for_update_waitContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#opt_for_update_wait}.
	 * @param ctx the parse tree
	 */
	void exitOpt_for_update_wait(OBParser.Opt_for_update_waitContext ctx);
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
	 * Enter a parse tree produced by {@link OBParser#sort_list_for_group_by}.
	 * @param ctx the parse tree
	 */
	void enterSort_list_for_group_by(OBParser.Sort_list_for_group_byContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#sort_list_for_group_by}.
	 * @param ctx the parse tree
	 */
	void exitSort_list_for_group_by(OBParser.Sort_list_for_group_byContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#sort_key_for_group_by}.
	 * @param ctx the parse tree
	 */
	void enterSort_key_for_group_by(OBParser.Sort_key_for_group_byContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#sort_key_for_group_by}.
	 * @param ctx the parse tree
	 */
	void exitSort_key_for_group_by(OBParser.Sort_key_for_group_byContext ctx);
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
	 * Enter a parse tree produced by {@link OBParser#table_references_paren}.
	 * @param ctx the parse tree
	 */
	void enterTable_references_paren(OBParser.Table_references_parenContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#table_references_paren}.
	 * @param ctx the parse tree
	 */
	void exitTable_references_paren(OBParser.Table_references_parenContext ctx);
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
	 * Enter a parse tree produced by {@link OBParser#table_subquery_alias}.
	 * @param ctx the parse tree
	 */
	void enterTable_subquery_alias(OBParser.Table_subquery_aliasContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#table_subquery_alias}.
	 * @param ctx the parse tree
	 */
	void exitTable_subquery_alias(OBParser.Table_subquery_aliasContext ctx);
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
	 * Enter a parse tree produced by {@link OBParser#index_hint_type}.
	 * @param ctx the parse tree
	 */
	void enterIndex_hint_type(OBParser.Index_hint_typeContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#index_hint_type}.
	 * @param ctx the parse tree
	 */
	void exitIndex_hint_type(OBParser.Index_hint_typeContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#key_or_index}.
	 * @param ctx the parse tree
	 */
	void enterKey_or_index(OBParser.Key_or_indexContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#key_or_index}.
	 * @param ctx the parse tree
	 */
	void exitKey_or_index(OBParser.Key_or_indexContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#index_hint_scope}.
	 * @param ctx the parse tree
	 */
	void enterIndex_hint_scope(OBParser.Index_hint_scopeContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#index_hint_scope}.
	 * @param ctx the parse tree
	 */
	void exitIndex_hint_scope(OBParser.Index_hint_scopeContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#index_element}.
	 * @param ctx the parse tree
	 */
	void enterIndex_element(OBParser.Index_elementContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#index_element}.
	 * @param ctx the parse tree
	 */
	void exitIndex_element(OBParser.Index_elementContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#index_list}.
	 * @param ctx the parse tree
	 */
	void enterIndex_list(OBParser.Index_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#index_list}.
	 * @param ctx the parse tree
	 */
	void exitIndex_list(OBParser.Index_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#index_hint_definition}.
	 * @param ctx the parse tree
	 */
	void enterIndex_hint_definition(OBParser.Index_hint_definitionContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#index_hint_definition}.
	 * @param ctx the parse tree
	 */
	void exitIndex_hint_definition(OBParser.Index_hint_definitionContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#index_hint_list}.
	 * @param ctx the parse tree
	 */
	void enterIndex_hint_list(OBParser.Index_hint_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#index_hint_list}.
	 * @param ctx the parse tree
	 */
	void exitIndex_hint_list(OBParser.Index_hint_listContext ctx);
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
	 * Enter a parse tree produced by {@link OBParser#relation_with_star_list}.
	 * @param ctx the parse tree
	 */
	void enterRelation_with_star_list(OBParser.Relation_with_star_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#relation_with_star_list}.
	 * @param ctx the parse tree
	 */
	void exitRelation_with_star_list(OBParser.Relation_with_star_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#relation_factor_with_star}.
	 * @param ctx the parse tree
	 */
	void enterRelation_factor_with_star(OBParser.Relation_factor_with_starContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#relation_factor_with_star}.
	 * @param ctx the parse tree
	 */
	void exitRelation_factor_with_star(OBParser.Relation_factor_with_starContext ctx);
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
	 * Enter a parse tree produced by {@link OBParser#inner_join_type}.
	 * @param ctx the parse tree
	 */
	void enterInner_join_type(OBParser.Inner_join_typeContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#inner_join_type}.
	 * @param ctx the parse tree
	 */
	void exitInner_join_type(OBParser.Inner_join_typeContext ctx);
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
	 * Enter a parse tree produced by {@link OBParser#table_values_clause}.
	 * @param ctx the parse tree
	 */
	void enterTable_values_clause(OBParser.Table_values_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#table_values_clause}.
	 * @param ctx the parse tree
	 */
	void exitTable_values_clause(OBParser.Table_values_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#table_values_clause_with_order_by_and_limit}.
	 * @param ctx the parse tree
	 */
	void enterTable_values_clause_with_order_by_and_limit(OBParser.Table_values_clause_with_order_by_and_limitContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#table_values_clause_with_order_by_and_limit}.
	 * @param ctx the parse tree
	 */
	void exitTable_values_clause_with_order_by_and_limit(OBParser.Table_values_clause_with_order_by_and_limitContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#values_row_list}.
	 * @param ctx the parse tree
	 */
	void enterValues_row_list(OBParser.Values_row_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#values_row_list}.
	 * @param ctx the parse tree
	 */
	void exitValues_row_list(OBParser.Values_row_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#row_value}.
	 * @param ctx the parse tree
	 */
	void enterRow_value(OBParser.Row_valueContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#row_value}.
	 * @param ctx the parse tree
	 */
	void exitRow_value(OBParser.Row_valueContext ctx);
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
	 * Enter a parse tree produced by {@link OBParser#check_table_options}.
	 * @param ctx the parse tree
	 */
	void enterCheck_table_options(OBParser.Check_table_optionsContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#check_table_options}.
	 * @param ctx the parse tree
	 */
	void exitCheck_table_options(OBParser.Check_table_optionsContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#check_table_option}.
	 * @param ctx the parse tree
	 */
	void enterCheck_table_option(OBParser.Check_table_optionContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#check_table_option}.
	 * @param ctx the parse tree
	 */
	void exitCheck_table_option(OBParser.Check_table_optionContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#get_diagnostics_stmt}.
	 * @param ctx the parse tree
	 */
	void enterGet_diagnostics_stmt(OBParser.Get_diagnostics_stmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#get_diagnostics_stmt}.
	 * @param ctx the parse tree
	 */
	void exitGet_diagnostics_stmt(OBParser.Get_diagnostics_stmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#get_condition_diagnostics_stmt}.
	 * @param ctx the parse tree
	 */
	void enterGet_condition_diagnostics_stmt(OBParser.Get_condition_diagnostics_stmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#get_condition_diagnostics_stmt}.
	 * @param ctx the parse tree
	 */
	void exitGet_condition_diagnostics_stmt(OBParser.Get_condition_diagnostics_stmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#condition_arg}.
	 * @param ctx the parse tree
	 */
	void enterCondition_arg(OBParser.Condition_argContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#condition_arg}.
	 * @param ctx the parse tree
	 */
	void exitCondition_arg(OBParser.Condition_argContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#condition_information_item_list}.
	 * @param ctx the parse tree
	 */
	void enterCondition_information_item_list(OBParser.Condition_information_item_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#condition_information_item_list}.
	 * @param ctx the parse tree
	 */
	void exitCondition_information_item_list(OBParser.Condition_information_item_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#condition_information_item}.
	 * @param ctx the parse tree
	 */
	void enterCondition_information_item(OBParser.Condition_information_itemContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#condition_information_item}.
	 * @param ctx the parse tree
	 */
	void exitCondition_information_item(OBParser.Condition_information_itemContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#condition_information_item_name}.
	 * @param ctx the parse tree
	 */
	void enterCondition_information_item_name(OBParser.Condition_information_item_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#condition_information_item_name}.
	 * @param ctx the parse tree
	 */
	void exitCondition_information_item_name(OBParser.Condition_information_item_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#get_statement_diagnostics_stmt}.
	 * @param ctx the parse tree
	 */
	void enterGet_statement_diagnostics_stmt(OBParser.Get_statement_diagnostics_stmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#get_statement_diagnostics_stmt}.
	 * @param ctx the parse tree
	 */
	void exitGet_statement_diagnostics_stmt(OBParser.Get_statement_diagnostics_stmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#statement_information_item_list}.
	 * @param ctx the parse tree
	 */
	void enterStatement_information_item_list(OBParser.Statement_information_item_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#statement_information_item_list}.
	 * @param ctx the parse tree
	 */
	void exitStatement_information_item_list(OBParser.Statement_information_item_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#statement_information_item}.
	 * @param ctx the parse tree
	 */
	void enterStatement_information_item(OBParser.Statement_information_itemContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#statement_information_item}.
	 * @param ctx the parse tree
	 */
	void exitStatement_information_item(OBParser.Statement_information_itemContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#statement_information_item_name}.
	 * @param ctx the parse tree
	 */
	void enterStatement_information_item_name(OBParser.Statement_information_item_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#statement_information_item_name}.
	 * @param ctx the parse tree
	 */
	void exitStatement_information_item_name(OBParser.Statement_information_item_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#databases_or_schemas}.
	 * @param ctx the parse tree
	 */
	void enterDatabases_or_schemas(OBParser.Databases_or_schemasContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#databases_or_schemas}.
	 * @param ctx the parse tree
	 */
	void exitDatabases_or_schemas(OBParser.Databases_or_schemasContext ctx);
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
	 * Enter a parse tree produced by {@link OBParser#opt_storage}.
	 * @param ctx the parse tree
	 */
	void enterOpt_storage(OBParser.Opt_storageContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#opt_storage}.
	 * @param ctx the parse tree
	 */
	void exitOpt_storage(OBParser.Opt_storageContext ctx);
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
	 * Enter a parse tree produced by {@link OBParser#database_or_schema}.
	 * @param ctx the parse tree
	 */
	void enterDatabase_or_schema(OBParser.Database_or_schemaContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#database_or_schema}.
	 * @param ctx the parse tree
	 */
	void exitDatabase_or_schema(OBParser.Database_or_schemaContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#index_or_indexes_or_keys}.
	 * @param ctx the parse tree
	 */
	void enterIndex_or_indexes_or_keys(OBParser.Index_or_indexes_or_keysContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#index_or_indexes_or_keys}.
	 * @param ctx the parse tree
	 */
	void exitIndex_or_indexes_or_keys(OBParser.Index_or_indexes_or_keysContext ctx);
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
	 * Enter a parse tree produced by {@link OBParser#calibration_info_list}.
	 * @param ctx the parse tree
	 */
	void enterCalibration_info_list(OBParser.Calibration_info_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#calibration_info_list}.
	 * @param ctx the parse tree
	 */
	void exitCalibration_info_list(OBParser.Calibration_info_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#opt_show_engine}.
	 * @param ctx the parse tree
	 */
	void enterOpt_show_engine(OBParser.Opt_show_engineContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#opt_show_engine}.
	 * @param ctx the parse tree
	 */
	void exitOpt_show_engine(OBParser.Opt_show_engineContext ctx);
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
	 * Enter a parse tree produced by {@link OBParser#opt_tablespace_option}.
	 * @param ctx the parse tree
	 */
	void enterOpt_tablespace_option(OBParser.Opt_tablespace_optionContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#opt_tablespace_option}.
	 * @param ctx the parse tree
	 */
	void exitOpt_tablespace_option(OBParser.Opt_tablespace_optionContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#opt_tablespace_engine}.
	 * @param ctx the parse tree
	 */
	void enterOpt_tablespace_engine(OBParser.Opt_tablespace_engineContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#opt_tablespace_engine}.
	 * @param ctx the parse tree
	 */
	void exitOpt_tablespace_engine(OBParser.Opt_tablespace_engineContext ctx);
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
	 * Enter a parse tree produced by {@link OBParser#alter_tablespace_options}.
	 * @param ctx the parse tree
	 */
	void enterAlter_tablespace_options(OBParser.Alter_tablespace_optionsContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#alter_tablespace_options}.
	 * @param ctx the parse tree
	 */
	void exitAlter_tablespace_options(OBParser.Alter_tablespace_optionsContext ctx);
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
	 * Enter a parse tree produced by {@link OBParser#opt_alter_tablespace_options}.
	 * @param ctx the parse tree
	 */
	void enterOpt_alter_tablespace_options(OBParser.Opt_alter_tablespace_optionsContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#opt_alter_tablespace_options}.
	 * @param ctx the parse tree
	 */
	void exitOpt_alter_tablespace_options(OBParser.Opt_alter_tablespace_optionsContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#opt_alter_tablespace_option}.
	 * @param ctx the parse tree
	 */
	void enterOpt_alter_tablespace_option(OBParser.Opt_alter_tablespace_optionContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#opt_alter_tablespace_option}.
	 * @param ctx the parse tree
	 */
	void exitOpt_alter_tablespace_option(OBParser.Opt_alter_tablespace_optionContext ctx);
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
	 * Enter a parse tree produced by {@link OBParser#rotate_master_key_stmt}.
	 * @param ctx the parse tree
	 */
	void enterRotate_master_key_stmt(OBParser.Rotate_master_key_stmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#rotate_master_key_stmt}.
	 * @param ctx the parse tree
	 */
	void exitRotate_master_key_stmt(OBParser.Rotate_master_key_stmtContext ctx);
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
	 * Enter a parse tree produced by {@link OBParser#opt_tablespace_options}.
	 * @param ctx the parse tree
	 */
	void enterOpt_tablespace_options(OBParser.Opt_tablespace_optionsContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#opt_tablespace_options}.
	 * @param ctx the parse tree
	 */
	void exitOpt_tablespace_options(OBParser.Opt_tablespace_optionsContext ctx);
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
	 * Enter a parse tree produced by {@link OBParser#user_specification_list}.
	 * @param ctx the parse tree
	 */
	void enterUser_specification_list(OBParser.User_specification_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#user_specification_list}.
	 * @param ctx the parse tree
	 */
	void exitUser_specification_list(OBParser.User_specification_listContext ctx);
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
	 * Enter a parse tree produced by {@link OBParser#user_specification_without_password}.
	 * @param ctx the parse tree
	 */
	void enterUser_specification_without_password(OBParser.User_specification_without_passwordContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#user_specification_without_password}.
	 * @param ctx the parse tree
	 */
	void exitUser_specification_without_password(OBParser.User_specification_without_passwordContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#user_specification_with_password}.
	 * @param ctx the parse tree
	 */
	void enterUser_specification_with_password(OBParser.User_specification_with_passwordContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#user_specification_with_password}.
	 * @param ctx the parse tree
	 */
	void exitUser_specification_with_password(OBParser.User_specification_with_passwordContext ctx);
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
	 * Enter a parse tree produced by {@link OBParser#resource_option_list}.
	 * @param ctx the parse tree
	 */
	void enterResource_option_list(OBParser.Resource_option_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#resource_option_list}.
	 * @param ctx the parse tree
	 */
	void exitResource_option_list(OBParser.Resource_option_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#resource_option}.
	 * @param ctx the parse tree
	 */
	void enterResource_option(OBParser.Resource_optionContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#resource_option}.
	 * @param ctx the parse tree
	 */
	void exitResource_option(OBParser.Resource_optionContext ctx);
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
	 * Enter a parse tree produced by {@link OBParser#user_host_or_current_user}.
	 * @param ctx the parse tree
	 */
	void enterUser_host_or_current_user(OBParser.User_host_or_current_userContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#user_host_or_current_user}.
	 * @param ctx the parse tree
	 */
	void exitUser_host_or_current_user(OBParser.User_host_or_current_userContext ctx);
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
	 * Enter a parse tree produced by {@link OBParser#user_specification_without_password_list}.
	 * @param ctx the parse tree
	 */
	void enterUser_specification_without_password_list(OBParser.User_specification_without_password_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#user_specification_without_password_list}.
	 * @param ctx the parse tree
	 */
	void exitUser_specification_without_password_list(OBParser.User_specification_without_password_listContext ctx);
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
	 * Enter a parse tree produced by {@link OBParser#role_with_host}.
	 * @param ctx the parse tree
	 */
	void enterRole_with_host(OBParser.Role_with_hostContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#role_with_host}.
	 * @param ctx the parse tree
	 */
	void exitRole_with_host(OBParser.Role_with_hostContext ctx);
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
	 * Enter a parse tree produced by {@link OBParser#rename_user_stmt}.
	 * @param ctx the parse tree
	 */
	void enterRename_user_stmt(OBParser.Rename_user_stmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#rename_user_stmt}.
	 * @param ctx the parse tree
	 */
	void exitRename_user_stmt(OBParser.Rename_user_stmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#rename_info}.
	 * @param ctx the parse tree
	 */
	void enterRename_info(OBParser.Rename_infoContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#rename_info}.
	 * @param ctx the parse tree
	 */
	void exitRename_info(OBParser.Rename_infoContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#rename_list}.
	 * @param ctx the parse tree
	 */
	void enterRename_list(OBParser.Rename_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#rename_list}.
	 * @param ctx the parse tree
	 */
	void exitRename_list(OBParser.Rename_listContext ctx);
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
	 * Enter a parse tree produced by {@link OBParser#xa_begin_stmt}.
	 * @param ctx the parse tree
	 */
	void enterXa_begin_stmt(OBParser.Xa_begin_stmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#xa_begin_stmt}.
	 * @param ctx the parse tree
	 */
	void exitXa_begin_stmt(OBParser.Xa_begin_stmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#xa_end_stmt}.
	 * @param ctx the parse tree
	 */
	void enterXa_end_stmt(OBParser.Xa_end_stmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#xa_end_stmt}.
	 * @param ctx the parse tree
	 */
	void exitXa_end_stmt(OBParser.Xa_end_stmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#xa_prepare_stmt}.
	 * @param ctx the parse tree
	 */
	void enterXa_prepare_stmt(OBParser.Xa_prepare_stmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#xa_prepare_stmt}.
	 * @param ctx the parse tree
	 */
	void exitXa_prepare_stmt(OBParser.Xa_prepare_stmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#xa_commit_stmt}.
	 * @param ctx the parse tree
	 */
	void enterXa_commit_stmt(OBParser.Xa_commit_stmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#xa_commit_stmt}.
	 * @param ctx the parse tree
	 */
	void exitXa_commit_stmt(OBParser.Xa_commit_stmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#xa_rollback_stmt}.
	 * @param ctx the parse tree
	 */
	void enterXa_rollback_stmt(OBParser.Xa_rollback_stmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#xa_rollback_stmt}.
	 * @param ctx the parse tree
	 */
	void exitXa_rollback_stmt(OBParser.Xa_rollback_stmtContext ctx);
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
	 * Enter a parse tree produced by {@link OBParser#grant_privileges}.
	 * @param ctx the parse tree
	 */
	void enterGrant_privileges(OBParser.Grant_privilegesContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#grant_privileges}.
	 * @param ctx the parse tree
	 */
	void exitGrant_privileges(OBParser.Grant_privilegesContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#role_or_priv_list}.
	 * @param ctx the parse tree
	 */
	void enterRole_or_priv_list(OBParser.Role_or_priv_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#role_or_priv_list}.
	 * @param ctx the parse tree
	 */
	void exitRole_or_priv_list(OBParser.Role_or_priv_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#role_or_priv}.
	 * @param ctx the parse tree
	 */
	void enterRole_or_priv(OBParser.Role_or_privContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#role_or_priv}.
	 * @param ctx the parse tree
	 */
	void exitRole_or_priv(OBParser.Role_or_privContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#object_type}.
	 * @param ctx the parse tree
	 */
	void enterObject_type(OBParser.Object_typeContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#object_type}.
	 * @param ctx the parse tree
	 */
	void exitObject_type(OBParser.Object_typeContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#priv_level}.
	 * @param ctx the parse tree
	 */
	void enterPriv_level(OBParser.Priv_levelContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#priv_level}.
	 * @param ctx the parse tree
	 */
	void exitPriv_level(OBParser.Priv_levelContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#grant_options}.
	 * @param ctx the parse tree
	 */
	void enterGrant_options(OBParser.Grant_optionsContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#grant_options}.
	 * @param ctx the parse tree
	 */
	void exitGrant_options(OBParser.Grant_optionsContext ctx);
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
	 * Enter a parse tree produced by {@link OBParser#default_set_role_clause}.
	 * @param ctx the parse tree
	 */
	void enterDefault_set_role_clause(OBParser.Default_set_role_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#default_set_role_clause}.
	 * @param ctx the parse tree
	 */
	void exitDefault_set_role_clause(OBParser.Default_set_role_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#set_role_clause}.
	 * @param ctx the parse tree
	 */
	void enterSet_role_clause(OBParser.Set_role_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#set_role_clause}.
	 * @param ctx the parse tree
	 */
	void exitSet_role_clause(OBParser.Set_role_clauseContext ctx);
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
	 * Enter a parse tree produced by {@link OBParser#audit_user_list}.
	 * @param ctx the parse tree
	 */
	void enterAudit_user_list(OBParser.Audit_user_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#audit_user_list}.
	 * @param ctx the parse tree
	 */
	void exitAudit_user_list(OBParser.Audit_user_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#audit_user_with_host_name}.
	 * @param ctx the parse tree
	 */
	void enterAudit_user_with_host_name(OBParser.Audit_user_with_host_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#audit_user_with_host_name}.
	 * @param ctx the parse tree
	 */
	void exitAudit_user_with_host_name(OBParser.Audit_user_with_host_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#audit_user}.
	 * @param ctx the parse tree
	 */
	void enterAudit_user(OBParser.Audit_userContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#audit_user}.
	 * @param ctx the parse tree
	 */
	void exitAudit_user(OBParser.Audit_userContext ctx);
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
	 * Enter a parse tree produced by {@link OBParser#alter_external_table_action}.
	 * @param ctx the parse tree
	 */
	void enterAlter_external_table_action(OBParser.Alter_external_table_actionContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#alter_external_table_action}.
	 * @param ctx the parse tree
	 */
	void exitAlter_external_table_action(OBParser.Alter_external_table_actionContext ctx);
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
	 * Enter a parse tree produced by {@link OBParser#alter_constraint_option}.
	 * @param ctx the parse tree
	 */
	void enterAlter_constraint_option(OBParser.Alter_constraint_optionContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#alter_constraint_option}.
	 * @param ctx the parse tree
	 */
	void exitAlter_constraint_option(OBParser.Alter_constraint_optionContext ctx);
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
	 * Enter a parse tree produced by {@link OBParser#opt_partition_range_or_list}.
	 * @param ctx the parse tree
	 */
	void enterOpt_partition_range_or_list(OBParser.Opt_partition_range_or_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#opt_partition_range_or_list}.
	 * @param ctx the parse tree
	 */
	void exitOpt_partition_range_or_list(OBParser.Opt_partition_range_or_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#alter_tg_partition_option}.
	 * @param ctx the parse tree
	 */
	void enterAlter_tg_partition_option(OBParser.Alter_tg_partition_optionContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#alter_tg_partition_option}.
	 * @param ctx the parse tree
	 */
	void exitAlter_tg_partition_option(OBParser.Alter_tg_partition_optionContext ctx);
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
	 * Enter a parse tree produced by {@link OBParser#modify_tg_partition_info}.
	 * @param ctx the parse tree
	 */
	void enterModify_tg_partition_info(OBParser.Modify_tg_partition_infoContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#modify_tg_partition_info}.
	 * @param ctx the parse tree
	 */
	void exitModify_tg_partition_info(OBParser.Modify_tg_partition_infoContext ctx);
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
	 * Enter a parse tree produced by {@link OBParser#alter_column_group_action}.
	 * @param ctx the parse tree
	 */
	void enterAlter_column_group_action(OBParser.Alter_column_group_actionContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#alter_column_group_action}.
	 * @param ctx the parse tree
	 */
	void exitAlter_column_group_action(OBParser.Alter_column_group_actionContext ctx);
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
	 * Enter a parse tree produced by {@link OBParser#alter_column_behavior}.
	 * @param ctx the parse tree
	 */
	void enterAlter_column_behavior(OBParser.Alter_column_behaviorContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#alter_column_behavior}.
	 * @param ctx the parse tree
	 */
	void exitAlter_column_behavior(OBParser.Alter_column_behaviorContext ctx);
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
	 * Enter a parse tree produced by {@link OBParser#optimize_stmt}.
	 * @param ctx the parse tree
	 */
	void enterOptimize_stmt(OBParser.Optimize_stmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#optimize_stmt}.
	 * @param ctx the parse tree
	 */
	void exitOptimize_stmt(OBParser.Optimize_stmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#dump_memory_stmt}.
	 * @param ctx the parse tree
	 */
	void enterDump_memory_stmt(OBParser.Dump_memory_stmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#dump_memory_stmt}.
	 * @param ctx the parse tree
	 */
	void exitDump_memory_stmt(OBParser.Dump_memory_stmtContext ctx);
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
	 * Enter a parse tree produced by {@link OBParser#change_tenant_name_or_tenant_id}.
	 * @param ctx the parse tree
	 */
	void enterChange_tenant_name_or_tenant_id(OBParser.Change_tenant_name_or_tenant_idContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#change_tenant_name_or_tenant_id}.
	 * @param ctx the parse tree
	 */
	void exitChange_tenant_name_or_tenant_id(OBParser.Change_tenant_name_or_tenant_idContext ctx);
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
	 * Enter a parse tree produced by {@link OBParser#backup_tenant_name_list}.
	 * @param ctx the parse tree
	 */
	void enterBackup_tenant_name_list(OBParser.Backup_tenant_name_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#backup_tenant_name_list}.
	 * @param ctx the parse tree
	 */
	void exitBackup_tenant_name_list(OBParser.Backup_tenant_name_listContext ctx);
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
	 * Enter a parse tree produced by {@link OBParser#shared_storage_info}.
	 * @param ctx the parse tree
	 */
	void enterShared_storage_info(OBParser.Shared_storage_infoContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#shared_storage_info}.
	 * @param ctx the parse tree
	 */
	void exitShared_storage_info(OBParser.Shared_storage_infoContext ctx);
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
	 * Enter a parse tree produced by {@link OBParser#opt_storage_use_for}.
	 * @param ctx the parse tree
	 */
	void enterOpt_storage_use_for(OBParser.Opt_storage_use_forContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#opt_storage_use_for}.
	 * @param ctx the parse tree
	 */
	void exitOpt_storage_use_for(OBParser.Opt_storage_use_forContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#opt_scope_type}.
	 * @param ctx the parse tree
	 */
	void enterOpt_scope_type(OBParser.Opt_scope_typeContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#opt_scope_type}.
	 * @param ctx the parse tree
	 */
	void exitOpt_scope_type(OBParser.Opt_scope_typeContext ctx);
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
	 * Enter a parse tree produced by {@link OBParser#ls_server_or_server_or_zone_or_tenant}.
	 * @param ctx the parse tree
	 */
	void enterLs_server_or_server_or_zone_or_tenant(OBParser.Ls_server_or_server_or_zone_or_tenantContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#ls_server_or_server_or_zone_or_tenant}.
	 * @param ctx the parse tree
	 */
	void exitLs_server_or_server_or_zone_or_tenant(OBParser.Ls_server_or_server_or_zone_or_tenantContext ctx);
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
	 * Enter a parse tree produced by {@link OBParser#sql_id_or_schema_id_expr}.
	 * @param ctx the parse tree
	 */
	void enterSql_id_or_schema_id_expr(OBParser.Sql_id_or_schema_id_exprContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#sql_id_or_schema_id_expr}.
	 * @param ctx the parse tree
	 */
	void exitSql_id_or_schema_id_expr(OBParser.Sql_id_or_schema_id_exprContext ctx);
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
	 * Enter a parse tree produced by {@link OBParser#alter_system_set_parameter_actions}.
	 * @param ctx the parse tree
	 */
	void enterAlter_system_set_parameter_actions(OBParser.Alter_system_set_parameter_actionsContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#alter_system_set_parameter_actions}.
	 * @param ctx the parse tree
	 */
	void exitAlter_system_set_parameter_actions(OBParser.Alter_system_set_parameter_actionsContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#alter_system_set_parameter_action}.
	 * @param ctx the parse tree
	 */
	void enterAlter_system_set_parameter_action(OBParser.Alter_system_set_parameter_actionContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#alter_system_set_parameter_action}.
	 * @param ctx the parse tree
	 */
	void exitAlter_system_set_parameter_action(OBParser.Alter_system_set_parameter_actionContext ctx);
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
	 * Enter a parse tree produced by {@link OBParser#cluster_role}.
	 * @param ctx the parse tree
	 */
	void enterCluster_role(OBParser.Cluster_roleContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#cluster_role}.
	 * @param ctx the parse tree
	 */
	void exitCluster_role(OBParser.Cluster_roleContext ctx);
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
	 * Enter a parse tree produced by {@link OBParser#ls_role}.
	 * @param ctx the parse tree
	 */
	void enterLs_role(OBParser.Ls_roleContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#ls_role}.
	 * @param ctx the parse tree
	 */
	void exitLs_role(OBParser.Ls_roleContext ctx);
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
	 * Enter a parse tree produced by {@link OBParser#mock_stmt}.
	 * @param ctx the parse tree
	 */
	void enterMock_stmt(OBParser.Mock_stmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#mock_stmt}.
	 * @param ctx the parse tree
	 */
	void exitMock_stmt(OBParser.Mock_stmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#plugin_name}.
	 * @param ctx the parse tree
	 */
	void enterPlugin_name(OBParser.Plugin_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#plugin_name}.
	 * @param ctx the parse tree
	 */
	void exitPlugin_name(OBParser.Plugin_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#install_plugin_stmt}.
	 * @param ctx the parse tree
	 */
	void enterInstall_plugin_stmt(OBParser.Install_plugin_stmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#install_plugin_stmt}.
	 * @param ctx the parse tree
	 */
	void exitInstall_plugin_stmt(OBParser.Install_plugin_stmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#uninstall_plugin_stmt}.
	 * @param ctx the parse tree
	 */
	void enterUninstall_plugin_stmt(OBParser.Uninstall_plugin_stmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#uninstall_plugin_stmt}.
	 * @param ctx the parse tree
	 */
	void exitUninstall_plugin_stmt(OBParser.Uninstall_plugin_stmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#flush_stmt}.
	 * @param ctx the parse tree
	 */
	void enterFlush_stmt(OBParser.Flush_stmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#flush_stmt}.
	 * @param ctx the parse tree
	 */
	void exitFlush_stmt(OBParser.Flush_stmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#flush_options}.
	 * @param ctx the parse tree
	 */
	void enterFlush_options(OBParser.Flush_optionsContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#flush_options}.
	 * @param ctx the parse tree
	 */
	void exitFlush_options(OBParser.Flush_optionsContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#flush_options_list}.
	 * @param ctx the parse tree
	 */
	void enterFlush_options_list(OBParser.Flush_options_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#flush_options_list}.
	 * @param ctx the parse tree
	 */
	void exitFlush_options_list(OBParser.Flush_options_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#flush_option}.
	 * @param ctx the parse tree
	 */
	void enterFlush_option(OBParser.Flush_optionContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#flush_option}.
	 * @param ctx the parse tree
	 */
	void exitFlush_option(OBParser.Flush_optionContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#handler_stmt}.
	 * @param ctx the parse tree
	 */
	void enterHandler_stmt(OBParser.Handler_stmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#handler_stmt}.
	 * @param ctx the parse tree
	 */
	void exitHandler_stmt(OBParser.Handler_stmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#handler_read_or_scan}.
	 * @param ctx the parse tree
	 */
	void enterHandler_read_or_scan(OBParser.Handler_read_or_scanContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#handler_read_or_scan}.
	 * @param ctx the parse tree
	 */
	void exitHandler_read_or_scan(OBParser.Handler_read_or_scanContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#handler_scan_function}.
	 * @param ctx the parse tree
	 */
	void enterHandler_scan_function(OBParser.Handler_scan_functionContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#handler_scan_function}.
	 * @param ctx the parse tree
	 */
	void exitHandler_scan_function(OBParser.Handler_scan_functionContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#handler_rkey_function}.
	 * @param ctx the parse tree
	 */
	void enterHandler_rkey_function(OBParser.Handler_rkey_functionContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#handler_rkey_function}.
	 * @param ctx the parse tree
	 */
	void exitHandler_rkey_function(OBParser.Handler_rkey_functionContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#handler_rkey_mode}.
	 * @param ctx the parse tree
	 */
	void enterHandler_rkey_mode(OBParser.Handler_rkey_modeContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#handler_rkey_mode}.
	 * @param ctx the parse tree
	 */
	void exitHandler_rkey_mode(OBParser.Handler_rkey_modeContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#show_plugin_stmt}.
	 * @param ctx the parse tree
	 */
	void enterShow_plugin_stmt(OBParser.Show_plugin_stmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#show_plugin_stmt}.
	 * @param ctx the parse tree
	 */
	void exitShow_plugin_stmt(OBParser.Show_plugin_stmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#create_server_stmt}.
	 * @param ctx the parse tree
	 */
	void enterCreate_server_stmt(OBParser.Create_server_stmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#create_server_stmt}.
	 * @param ctx the parse tree
	 */
	void exitCreate_server_stmt(OBParser.Create_server_stmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#server_options_list}.
	 * @param ctx the parse tree
	 */
	void enterServer_options_list(OBParser.Server_options_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#server_options_list}.
	 * @param ctx the parse tree
	 */
	void exitServer_options_list(OBParser.Server_options_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#server_option}.
	 * @param ctx the parse tree
	 */
	void enterServer_option(OBParser.Server_optionContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#server_option}.
	 * @param ctx the parse tree
	 */
	void exitServer_option(OBParser.Server_optionContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#alter_server_stmt}.
	 * @param ctx the parse tree
	 */
	void enterAlter_server_stmt(OBParser.Alter_server_stmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#alter_server_stmt}.
	 * @param ctx the parse tree
	 */
	void exitAlter_server_stmt(OBParser.Alter_server_stmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#drop_server_stmt}.
	 * @param ctx the parse tree
	 */
	void enterDrop_server_stmt(OBParser.Drop_server_stmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#drop_server_stmt}.
	 * @param ctx the parse tree
	 */
	void exitDrop_server_stmt(OBParser.Drop_server_stmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#create_logfile_group_stmt}.
	 * @param ctx the parse tree
	 */
	void enterCreate_logfile_group_stmt(OBParser.Create_logfile_group_stmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#create_logfile_group_stmt}.
	 * @param ctx the parse tree
	 */
	void exitCreate_logfile_group_stmt(OBParser.Create_logfile_group_stmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#logfile_group_info}.
	 * @param ctx the parse tree
	 */
	void enterLogfile_group_info(OBParser.Logfile_group_infoContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#logfile_group_info}.
	 * @param ctx the parse tree
	 */
	void exitLogfile_group_info(OBParser.Logfile_group_infoContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#add_log_file}.
	 * @param ctx the parse tree
	 */
	void enterAdd_log_file(OBParser.Add_log_fileContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#add_log_file}.
	 * @param ctx the parse tree
	 */
	void exitAdd_log_file(OBParser.Add_log_fileContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#lg_undofile}.
	 * @param ctx the parse tree
	 */
	void enterLg_undofile(OBParser.Lg_undofileContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#lg_undofile}.
	 * @param ctx the parse tree
	 */
	void exitLg_undofile(OBParser.Lg_undofileContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#lg_redofile}.
	 * @param ctx the parse tree
	 */
	void enterLg_redofile(OBParser.Lg_redofileContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#lg_redofile}.
	 * @param ctx the parse tree
	 */
	void exitLg_redofile(OBParser.Lg_redofileContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#logfile_group_option_list}.
	 * @param ctx the parse tree
	 */
	void enterLogfile_group_option_list(OBParser.Logfile_group_option_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#logfile_group_option_list}.
	 * @param ctx the parse tree
	 */
	void exitLogfile_group_option_list(OBParser.Logfile_group_option_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#logfile_group_options}.
	 * @param ctx the parse tree
	 */
	void enterLogfile_group_options(OBParser.Logfile_group_optionsContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#logfile_group_options}.
	 * @param ctx the parse tree
	 */
	void exitLogfile_group_options(OBParser.Logfile_group_optionsContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#logfile_group_option}.
	 * @param ctx the parse tree
	 */
	void enterLogfile_group_option(OBParser.Logfile_group_optionContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#logfile_group_option}.
	 * @param ctx the parse tree
	 */
	void exitLogfile_group_option(OBParser.Logfile_group_optionContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#opt_ts_initial_size}.
	 * @param ctx the parse tree
	 */
	void enterOpt_ts_initial_size(OBParser.Opt_ts_initial_sizeContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#opt_ts_initial_size}.
	 * @param ctx the parse tree
	 */
	void exitOpt_ts_initial_size(OBParser.Opt_ts_initial_sizeContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#opt_ts_undo_buffer_size}.
	 * @param ctx the parse tree
	 */
	void enterOpt_ts_undo_buffer_size(OBParser.Opt_ts_undo_buffer_sizeContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#opt_ts_undo_buffer_size}.
	 * @param ctx the parse tree
	 */
	void exitOpt_ts_undo_buffer_size(OBParser.Opt_ts_undo_buffer_sizeContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#opt_ts_redo_buffer_size}.
	 * @param ctx the parse tree
	 */
	void enterOpt_ts_redo_buffer_size(OBParser.Opt_ts_redo_buffer_sizeContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#opt_ts_redo_buffer_size}.
	 * @param ctx the parse tree
	 */
	void exitOpt_ts_redo_buffer_size(OBParser.Opt_ts_redo_buffer_sizeContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#opt_ts_nodegroup}.
	 * @param ctx the parse tree
	 */
	void enterOpt_ts_nodegroup(OBParser.Opt_ts_nodegroupContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#opt_ts_nodegroup}.
	 * @param ctx the parse tree
	 */
	void exitOpt_ts_nodegroup(OBParser.Opt_ts_nodegroupContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#opt_ts_comment}.
	 * @param ctx the parse tree
	 */
	void enterOpt_ts_comment(OBParser.Opt_ts_commentContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#opt_ts_comment}.
	 * @param ctx the parse tree
	 */
	void exitOpt_ts_comment(OBParser.Opt_ts_commentContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#alter_logfile_group_stmt}.
	 * @param ctx the parse tree
	 */
	void enterAlter_logfile_group_stmt(OBParser.Alter_logfile_group_stmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#alter_logfile_group_stmt}.
	 * @param ctx the parse tree
	 */
	void exitAlter_logfile_group_stmt(OBParser.Alter_logfile_group_stmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#alter_logfile_group_info}.
	 * @param ctx the parse tree
	 */
	void enterAlter_logfile_group_info(OBParser.Alter_logfile_group_infoContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#alter_logfile_group_info}.
	 * @param ctx the parse tree
	 */
	void exitAlter_logfile_group_info(OBParser.Alter_logfile_group_infoContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#alter_logfile_group_option_list}.
	 * @param ctx the parse tree
	 */
	void enterAlter_logfile_group_option_list(OBParser.Alter_logfile_group_option_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#alter_logfile_group_option_list}.
	 * @param ctx the parse tree
	 */
	void exitAlter_logfile_group_option_list(OBParser.Alter_logfile_group_option_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#alter_logfile_group_options}.
	 * @param ctx the parse tree
	 */
	void enterAlter_logfile_group_options(OBParser.Alter_logfile_group_optionsContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#alter_logfile_group_options}.
	 * @param ctx the parse tree
	 */
	void exitAlter_logfile_group_options(OBParser.Alter_logfile_group_optionsContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#alter_logfile_group_option}.
	 * @param ctx the parse tree
	 */
	void enterAlter_logfile_group_option(OBParser.Alter_logfile_group_optionContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#alter_logfile_group_option}.
	 * @param ctx the parse tree
	 */
	void exitAlter_logfile_group_option(OBParser.Alter_logfile_group_optionContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#drop_logfile_group_stmt}.
	 * @param ctx the parse tree
	 */
	void enterDrop_logfile_group_stmt(OBParser.Drop_logfile_group_stmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#drop_logfile_group_stmt}.
	 * @param ctx the parse tree
	 */
	void exitDrop_logfile_group_stmt(OBParser.Drop_logfile_group_stmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#drop_ts_options_list}.
	 * @param ctx the parse tree
	 */
	void enterDrop_ts_options_list(OBParser.Drop_ts_options_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#drop_ts_options_list}.
	 * @param ctx the parse tree
	 */
	void exitDrop_ts_options_list(OBParser.Drop_ts_options_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#drop_ts_options}.
	 * @param ctx the parse tree
	 */
	void enterDrop_ts_options(OBParser.Drop_ts_optionsContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#drop_ts_options}.
	 * @param ctx the parse tree
	 */
	void exitDrop_ts_options(OBParser.Drop_ts_optionsContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#drop_ts_option}.
	 * @param ctx the parse tree
	 */
	void enterDrop_ts_option(OBParser.Drop_ts_optionContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#drop_ts_option}.
	 * @param ctx the parse tree
	 */
	void exitDrop_ts_option(OBParser.Drop_ts_optionContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#ts_wait}.
	 * @param ctx the parse tree
	 */
	void enterTs_wait(OBParser.Ts_waitContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#ts_wait}.
	 * @param ctx the parse tree
	 */
	void exitTs_wait(OBParser.Ts_waitContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#opt_ts_engine}.
	 * @param ctx the parse tree
	 */
	void enterOpt_ts_engine(OBParser.Opt_ts_engineContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#opt_ts_engine}.
	 * @param ctx the parse tree
	 */
	void exitOpt_ts_engine(OBParser.Opt_ts_engineContext ctx);
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
	 * Enter a parse tree produced by {@link OBParser#opt_encrypt_key}.
	 * @param ctx the parse tree
	 */
	void enterOpt_encrypt_key(OBParser.Opt_encrypt_keyContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#opt_encrypt_key}.
	 * @param ctx the parse tree
	 */
	void exitOpt_encrypt_key(OBParser.Opt_encrypt_keyContext ctx);
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
	 * Enter a parse tree produced by {@link OBParser#release_savepoint_stmt}.
	 * @param ctx the parse tree
	 */
	void enterRelease_savepoint_stmt(OBParser.Release_savepoint_stmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#release_savepoint_stmt}.
	 * @param ctx the parse tree
	 */
	void exitRelease_savepoint_stmt(OBParser.Release_savepoint_stmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#alter_cluster_stmt}.
	 * @param ctx the parse tree
	 */
	void enterAlter_cluster_stmt(OBParser.Alter_cluster_stmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#alter_cluster_stmt}.
	 * @param ctx the parse tree
	 */
	void exitAlter_cluster_stmt(OBParser.Alter_cluster_stmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#cluster_define}.
	 * @param ctx the parse tree
	 */
	void enterCluster_define(OBParser.Cluster_defineContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#cluster_define}.
	 * @param ctx the parse tree
	 */
	void exitCluster_define(OBParser.Cluster_defineContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#cluster_option_list}.
	 * @param ctx the parse tree
	 */
	void enterCluster_option_list(OBParser.Cluster_option_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#cluster_option_list}.
	 * @param ctx the parse tree
	 */
	void exitCluster_option_list(OBParser.Cluster_option_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#cluster_option}.
	 * @param ctx the parse tree
	 */
	void enterCluster_option(OBParser.Cluster_optionContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#cluster_option}.
	 * @param ctx the parse tree
	 */
	void exitCluster_option(OBParser.Cluster_optionContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#cluster_action}.
	 * @param ctx the parse tree
	 */
	void enterCluster_action(OBParser.Cluster_actionContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#cluster_action}.
	 * @param ctx the parse tree
	 */
	void exitCluster_action(OBParser.Cluster_actionContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#switchover_cluster_stmt}.
	 * @param ctx the parse tree
	 */
	void enterSwitchover_cluster_stmt(OBParser.Switchover_cluster_stmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#switchover_cluster_stmt}.
	 * @param ctx the parse tree
	 */
	void exitSwitchover_cluster_stmt(OBParser.Switchover_cluster_stmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#commit_switchover_clause}.
	 * @param ctx the parse tree
	 */
	void enterCommit_switchover_clause(OBParser.Commit_switchover_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#commit_switchover_clause}.
	 * @param ctx the parse tree
	 */
	void exitCommit_switchover_clause(OBParser.Commit_switchover_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#protection_mode_stmt}.
	 * @param ctx the parse tree
	 */
	void enterProtection_mode_stmt(OBParser.Protection_mode_stmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#protection_mode_stmt}.
	 * @param ctx the parse tree
	 */
	void exitProtection_mode_stmt(OBParser.Protection_mode_stmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#protection_mode_option}.
	 * @param ctx the parse tree
	 */
	void enterProtection_mode_option(OBParser.Protection_mode_optionContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#protection_mode_option}.
	 * @param ctx the parse tree
	 */
	void exitProtection_mode_option(OBParser.Protection_mode_optionContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#cluster_name}.
	 * @param ctx the parse tree
	 */
	void enterCluster_name(OBParser.Cluster_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#cluster_name}.
	 * @param ctx the parse tree
	 */
	void exitCluster_name(OBParser.Cluster_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#disconnect_cluster_stmt}.
	 * @param ctx the parse tree
	 */
	void enterDisconnect_cluster_stmt(OBParser.Disconnect_cluster_stmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#disconnect_cluster_stmt}.
	 * @param ctx the parse tree
	 */
	void exitDisconnect_cluster_stmt(OBParser.Disconnect_cluster_stmtContext ctx);
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
	 * Enter a parse tree produced by {@link OBParser#new_or_old}.
	 * @param ctx the parse tree
	 */
	void enterNew_or_old(OBParser.New_or_oldContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#new_or_old}.
	 * @param ctx the parse tree
	 */
	void exitNew_or_old(OBParser.New_or_oldContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#new_or_old_column_ref}.
	 * @param ctx the parse tree
	 */
	void enterNew_or_old_column_ref(OBParser.New_or_old_column_refContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#new_or_old_column_ref}.
	 * @param ctx the parse tree
	 */
	void exitNew_or_old_column_ref(OBParser.New_or_old_column_refContext ctx);
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
	 * Enter a parse tree produced by {@link OBParser#mock_jt_on_error_on_empty}.
	 * @param ctx the parse tree
	 */
	void enterMock_jt_on_error_on_empty(OBParser.Mock_jt_on_error_on_emptyContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#mock_jt_on_error_on_empty}.
	 * @param ctx the parse tree
	 */
	void exitMock_jt_on_error_on_empty(OBParser.Mock_jt_on_error_on_emptyContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#jt_column_list}.
	 * @param ctx the parse tree
	 */
	void enterJt_column_list(OBParser.Jt_column_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#jt_column_list}.
	 * @param ctx the parse tree
	 */
	void exitJt_column_list(OBParser.Jt_column_listContext ctx);
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
	 * Enter a parse tree produced by {@link OBParser#opt_value_on_empty_or_error_or_mismatch}.
	 * @param ctx the parse tree
	 */
	void enterOpt_value_on_empty_or_error_or_mismatch(OBParser.Opt_value_on_empty_or_error_or_mismatchContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#opt_value_on_empty_or_error_or_mismatch}.
	 * @param ctx the parse tree
	 */
	void exitOpt_value_on_empty_or_error_or_mismatch(OBParser.Opt_value_on_empty_or_error_or_mismatchContext ctx);
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
	 * Enter a parse tree produced by {@link OBParser#vec_index_params}.
	 * @param ctx the parse tree
	 */
	void enterVec_index_params(OBParser.Vec_index_paramsContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#vec_index_params}.
	 * @param ctx the parse tree
	 */
	void exitVec_index_params(OBParser.Vec_index_paramsContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#vec_index_param}.
	 * @param ctx the parse tree
	 */
	void enterVec_index_param(OBParser.Vec_index_paramContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#vec_index_param}.
	 * @param ctx the parse tree
	 */
	void exitVec_index_param(OBParser.Vec_index_paramContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#vec_index_param_value}.
	 * @param ctx the parse tree
	 */
	void enterVec_index_param_value(OBParser.Vec_index_param_valueContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#vec_index_param_value}.
	 * @param ctx the parse tree
	 */
	void exitVec_index_param_value(OBParser.Vec_index_param_valueContext ctx);
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
	 * Enter a parse tree produced by {@link OBParser#json_query_opt}.
	 * @param ctx the parse tree
	 */
	void enterJson_query_opt(OBParser.Json_query_optContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#json_query_opt}.
	 * @param ctx the parse tree
	 */
	void exitJson_query_opt(OBParser.Json_query_optContext ctx);
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
	 * Enter a parse tree produced by {@link OBParser#json_value_opt}.
	 * @param ctx the parse tree
	 */
	void enterJson_value_opt(OBParser.Json_value_optContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#json_value_opt}.
	 * @param ctx the parse tree
	 */
	void exitJson_value_opt(OBParser.Json_value_optContext ctx);
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
	 * Enter a parse tree produced by {@link OBParser#opt_on_empty_or_error}.
	 * @param ctx the parse tree
	 */
	void enterOpt_on_empty_or_error(OBParser.Opt_on_empty_or_errorContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#opt_on_empty_or_error}.
	 * @param ctx the parse tree
	 */
	void exitOpt_on_empty_or_error(OBParser.Opt_on_empty_or_errorContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#on_empty}.
	 * @param ctx the parse tree
	 */
	void enterOn_empty(OBParser.On_emptyContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#on_empty}.
	 * @param ctx the parse tree
	 */
	void exitOn_empty(OBParser.On_emptyContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#on_error}.
	 * @param ctx the parse tree
	 */
	void enterOn_error(OBParser.On_errorContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#on_error}.
	 * @param ctx the parse tree
	 */
	void exitOn_error(OBParser.On_errorContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#json_on_response}.
	 * @param ctx the parse tree
	 */
	void enterJson_on_response(OBParser.Json_on_responseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#json_on_response}.
	 * @param ctx the parse tree
	 */
	void exitJson_on_response(OBParser.Json_on_responseContext ctx);
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
	 * Enter a parse tree produced by {@link OBParser#lob_chunk_size}.
	 * @param ctx the parse tree
	 */
	void enterLob_chunk_size(OBParser.Lob_chunk_sizeContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#lob_chunk_size}.
	 * @param ctx the parse tree
	 */
	void exitLob_chunk_size(OBParser.Lob_chunk_sizeContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#lob_storage_parameter}.
	 * @param ctx the parse tree
	 */
	void enterLob_storage_parameter(OBParser.Lob_storage_parameterContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#lob_storage_parameter}.
	 * @param ctx the parse tree
	 */
	void exitLob_storage_parameter(OBParser.Lob_storage_parameterContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#lob_storage_parameters}.
	 * @param ctx the parse tree
	 */
	void enterLob_storage_parameters(OBParser.Lob_storage_parametersContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#lob_storage_parameters}.
	 * @param ctx the parse tree
	 */
	void exitLob_storage_parameters(OBParser.Lob_storage_parametersContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#lob_storage_clause}.
	 * @param ctx the parse tree
	 */
	void enterLob_storage_clause(OBParser.Lob_storage_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#lob_storage_clause}.
	 * @param ctx the parse tree
	 */
	void exitLob_storage_clause(OBParser.Lob_storage_clauseContext ctx);
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
	 * Enter a parse tree produced by {@link OBParser#unreserved_keyword_for_role_name}.
	 * @param ctx the parse tree
	 */
	void enterUnreserved_keyword_for_role_name(OBParser.Unreserved_keyword_for_role_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#unreserved_keyword_for_role_name}.
	 * @param ctx the parse tree
	 */
	void exitUnreserved_keyword_for_role_name(OBParser.Unreserved_keyword_for_role_nameContext ctx);
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
	 * Enter a parse tree produced by {@link OBParser#unreserved_keyword_special}.
	 * @param ctx the parse tree
	 */
	void enterUnreserved_keyword_special(OBParser.Unreserved_keyword_specialContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#unreserved_keyword_special}.
	 * @param ctx the parse tree
	 */
	void exitUnreserved_keyword_special(OBParser.Unreserved_keyword_specialContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#unreserved_keyword_extra}.
	 * @param ctx the parse tree
	 */
	void enterUnreserved_keyword_extra(OBParser.Unreserved_keyword_extraContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#unreserved_keyword_extra}.
	 * @param ctx the parse tree
	 */
	void exitUnreserved_keyword_extra(OBParser.Unreserved_keyword_extraContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#unreserved_keyword_ambiguous_roles}.
	 * @param ctx the parse tree
	 */
	void enterUnreserved_keyword_ambiguous_roles(OBParser.Unreserved_keyword_ambiguous_rolesContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#unreserved_keyword_ambiguous_roles}.
	 * @param ctx the parse tree
	 */
	void exitUnreserved_keyword_ambiguous_roles(OBParser.Unreserved_keyword_ambiguous_rolesContext ctx);
	/**
	 * Enter a parse tree produced by {@link OBParser#mysql_reserved_keyword}.
	 * @param ctx the parse tree
	 */
	void enterMysql_reserved_keyword(OBParser.Mysql_reserved_keywordContext ctx);
	/**
	 * Exit a parse tree produced by {@link OBParser#mysql_reserved_keyword}.
	 * @param ctx the parse tree
	 */
	void exitMysql_reserved_keyword(OBParser.Mysql_reserved_keywordContext ctx);
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