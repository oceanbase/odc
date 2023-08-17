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

parser grammar PLParser;


options { tokenVocab=PLLexer; }


// start rule: null
@parser::header {
import com.oceanbase.tools.sqlparser.obmysql.OBLexer;
import com.oceanbase.tools.sqlparser.obmysql.OBParser;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import java.util.Map;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
}
@parser::members {
Map<String, HashSet<Integer>> expr2endTokens = new HashMap<String, HashSet<Integer>>(){{
				put("sql_stmt", new HashSet<Integer>() {{ add(DELIMITER);}});
		    	put("opt_cexpr", new HashSet<Integer>() {{ add(Comma);add(RightParen);}});
		    	put("expr", new HashSet<Integer>() {{ add(INTO);add(USING);add(WHEN);add(THEN);add(DELIMITER);
		    	add(LIMIT);add(Comma);add(END_KEY);add(DO);}});
}};
class PLSQLErrorListener extends BaseErrorListener {
    @Override
    public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine,
                            String msg, RecognitionException e) throws RecognitionException {
      // just throw the exception to PL Parser without handling anything.
        if (e != null) {
             throw e;
        }
    }
}

class PLErrorStrategy extends DefaultErrorStrategy {
    @Override
    public void reportError(Parser recognizer, RecognitionException e) {
        // do not recover even in ErrorRecoveryMode
    // the following code is copied from DefaultErrorStrategy#reportError
        this.beginErrorCondition(recognizer);
        if (e instanceof NoViableAltException) {
            this.reportNoViableAlternative(recognizer, (NoViableAltException) e);
        } else if (e instanceof InputMismatchException) {
            this.reportInputMismatch(recognizer, (InputMismatchException) e);
        } else if (e instanceof FailedPredicateException) {
            this.reportFailedPredicate(recognizer, (FailedPredicateException) e);
        } else {
            System.err.println("unknown recognition error type: " + e.getClass().getName());
            recognizer.notifyErrorListeners(e.getOffendingToken(), e.getMessage(), e);
        }
    }
    @Override
    public Token recoverInline(Parser recognizer) throws RecognitionException {
    	InputMismatchException e;
    	if (this.nextTokensContext == null) {
    		e = new InputMismatchException(recognizer);
    	} else {
    		e = new InputMismatchException(recognizer, this.nextTokensState, this.nextTokensContext);
    	}
    	throw e;
    }
}

public void ForwardSQL(ParserRuleContext ctx, String exprName) throws RecognitionException {
    // Forward sql string to the forward_sql_stmt node of SQL Parser
  String text = _input.getText(ctx.start, _input.LT(-1));
  text = addMoreLAT(text, exprName);
  CharStream cs = CharStreams.fromString(text);
    PLSQLErrorListener lexerErrorListener = new PLSQLErrorListener();
    PLSQLErrorListener parserErrorListener = new PLSQLErrorListener();
    OBLexer lexer = new OBLexer(cs);
    lexer.removeErrorListeners();
    lexer.addErrorListener(lexerErrorListener);
    CommonTokenStream tokens = new CommonTokenStream(lexer);
    tokens.fill();
    OBParser parser = new OBParser(tokens);
    parser.removeErrorListeners();
    parser.addErrorListener(parserErrorListener);
    parser.setErrorHandler(new PLErrorStrategy());
    parser.is_pl_parse_ = true;
    parser.is_pl_parse_expr_ = false;
    while(ctx.getChildCount() != 0) {
    	ctx.removeLastChild();
    }
    ctx.addChild(parser.forward_sql_stmt());
}

public void ForwardExpr(ParserRuleContext ctx, String exprName) throws RecognitionException {
    // Forward sql string to the forward_expr node of SQL Parser
  String text = _input.getText(ctx.start, _input.LT(-1));
  if (text.length() == 0) {
    return;
  }
  text = addMoreLAT(text, exprName);
    CharStream cs = CharStreams.fromString(text);
    PLSQLErrorListener lexerErrorListener = new PLSQLErrorListener();
    PLSQLErrorListener parserErrorListener = new PLSQLErrorListener();
    OBLexer lexer = new OBLexer(cs);
    lexer.removeErrorListeners();
    lexer.addErrorListener(lexerErrorListener);
    CommonTokenStream tokens = new CommonTokenStream(lexer);
    tokens.fill();
    OBParser parser = new OBParser(tokens);
    parser.removeErrorListeners();
    parser.addErrorListener(parserErrorListener);
    parser.setErrorHandler(new PLErrorStrategy());
    parser.is_pl_parse_ = true;
    parser.is_pl_parse_expr_ = true;
    while(ctx.getChildCount() != 0) {
    	ctx.removeLastChild();
    }
    ctx.addChild(parser.forward_expr());
}

private String addMoreLAT(String text, String exprName){
    int leftParenCount = 0;
    int rightParenCount = 0;
    boolean inSingleQuote = false;
	boolean inDoubleQuote = false;
	boolean inEscape = false;
	for (char c : text.toCharArray()) {
	    if (inEscape) {
    		inEscape = false;
    		continue;
    	}
    	if (c == '\\' && (inDoubleQuote || inSingleQuote)) {
    		inEscape = true;
    		continue;
    	}
		if (c == '\'' && !inDoubleQuote) {
	    	inSingleQuote = !inSingleQuote;
	    }
		if (c == '"' && !inSingleQuote) {
			inDoubleQuote = !inDoubleQuote;
		}
	    if (!inSingleQuote && !inDoubleQuote && c == '(') {
	        leftParenCount++;
	    } else if (!inSingleQuote && !inDoubleQuote && c == ')') {
	        rightParenCount++;
	    }
	}
    // Refer from obpl_oracle_read_sql_construct in pl_parser_oracle_mode.y
  Set<Integer> endTokens = expr2endTokens.get(exprName);
    boolean isBreak = leftParenCount == rightParenCount;
    while (!isBreak) {
      // get the next lookahead token
        int _la = _input.LA(1);
        if (_la == EOF) {
      isBreak = true;
          break;
    }
        if (leftParenCount == rightParenCount && endTokens.contains(_la)) {
          isBreak = true;
          break;
        } else if (_la == LeftParen) {
          leftParenCount++;
        } else if (_la == RightParen) {
          rightParenCount++;
        }
        text += _input.LT(1).getText();
        // match and consume the current lookahead token
        _errHandler.reportMatch(this);
        consume();
    }
    return text;
}
}

stmt_block
    : stmt_list EOF
    ;

stmt_list
    : stmt (DELIMITER stmt)*
    ;

stmt
    : outer_stmt?
    ;

outer_stmt
    : create_procedure_stmt
    | create_function_stmt
    | drop_procedure_stmt
    | drop_function_stmt
    | alter_procedure_stmt
    | alter_function_stmt
    | sql_stmt
    | call_sp_stmt
    | do_sp_stmt
    | signal_stmt
    | resignal_stmt
    | package_block
    | package_body_block
    | create_trigger_stmt
    | drop_trigger_stmt
    | plsql_trigger_source
    ;

sql_keyword
    : SQL_KEYWORD
    | TABLE
    | INSERT
    | DELETE
    | UPDATE
    ;

sql_stmt
    : sql_keyword (~(DELIMITER))*?{this.ForwardSQL($ctx, "sql_stmt");}
    | CREATE sql_keyword (~(DELIMITER))*?{this.ForwardSQL($ctx, "sql_stmt");}
    | DROP sql_keyword (~(DELIMITER))*?{this.ForwardSQL($ctx, "sql_stmt");}
    | ALTER sql_keyword (~(DELIMITER))*?{this.ForwardSQL($ctx, "sql_stmt");}
    | SET (~(DELIMITER))*?{this.ForwardSQL($ctx, "sql_stmt");}
    | COMMIT
    | ROLLBACK
    | SELECT (~(DELIMITER))*?{this.ForwardSQL($ctx, "sql_stmt");}
    | LeftParen SELECT (~(DELIMITER))*?{this.ForwardSQL($ctx, "sql_stmt");}
    ;

do_sp_stmt
    : DO expr_list
    | DO sp_unlabeled_block
    | DD sp_unlabeled_block
    | DO sp_proc_stmt_open
    | DO sp_proc_stmt_fetch
    | DO sp_proc_stmt_close
    ;

call_sp_stmt
    : CALL sp_call_name (LeftParen opt_sp_cparams RightParen)?
    | CALL sp_proc_stmt
    ;

opt_sp_cparams
    : opt_cexpr (Comma opt_cexpr)*
    ;

opt_cexpr
    : (~(DELIMITER))*?{this.ForwardExpr($ctx, "opt_cexpr");}
    ;

sp_name
    : ident (Dot ident)?
    ;

sp_call_name
    : ident Dot ident (Dot ident)?
    | ident
    ;

ident
    : IDENT
    | unreserved_keyword
    ;

unreserved_keyword
    : AFTER
    | BEFORE
    | BODY
    | EACH
    | ON
    | PACKAGE
    | ROW
    | ROWTYPE
    | JSON
    ;

package_block
    : PACKAGE sp_name package_stmts? END_KEY ident?
    ;

package_stmts
    : package_stmts package_stmt DELIMITER
    | package_stmt DELIMITER
    ;

package_stmt
    : sp_decl
    | func_decl
    | proc_decl
    ;

func_decl
    : FUNCTION ident LeftParen sp_param_list? RightParen RETURN sp_data_type
    ;

proc_decl
    : PROCEDURE ident LeftParen sp_param_list? RightParen
    ;

package_body_block
    : PACKAGE BODY sp_name decl_stmt_ext_list? END_KEY ident?
    ;

decl_stmt_ext_list
    : decl_stmt_ext DELIMITER
    | decl_stmt_ext_list decl_stmt_ext DELIMITER
    ;

decl_stmt_ext
    : sp_decl
    | func_decl function_body
    | proc_decl procedure_body
    ;

create_trigger_stmt
    : CREATE ((DEFINER Equal STRING) | (DEFINER Equal IDENT) | (DEFINER Equal CURRENT_USER))? plsql_trigger_source
    ;

plsql_trigger_source
    : TRIGGER sp_name trigger_definition
    ;

trigger_definition
    : trigger_time trigger_event ON sp_name FOR EACH ROW trigger_body
    ;

trigger_time
    : BEFORE
    | AFTER
    ;

trigger_event
    : INSERT
    | DELETE
    | UPDATE
    ;

trigger_body
    : sp_proc_stmt
    ;

drop_trigger_stmt
    : DROP TRIGGER (IF EXISTS)? sp_name
    ;

create_procedure_stmt
    : CREATE ((DEFINER Equal STRING) | (DEFINER Equal IDENT) | (DEFINER Equal CURRENT_USER))? PROCEDURE sp_name LeftParen sp_param_list? RightParen (opt_sp_create_chistics sp_create_chistic)? procedure_body
    ;

create_function_stmt
    : CREATE ((DEFINER Equal STRING) | (DEFINER Equal IDENT) | (DEFINER Equal CURRENT_USER))? FUNCTION sp_name LeftParen sp_fparam_list? RightParen RETURNS sp_data_type (opt_sp_create_chistics sp_create_chistic)? function_body
    ;

sp_param_list
    : sp_param (Comma sp_param)*
    ;

sp_param
    : (IN | OUT | INOUT)? ident param_type
    ;

sp_fparam_list
    : sp_fparam (Comma sp_fparam)*
    ;

sp_fparam
    : ident param_type
    ;

param_type
    : sp_data_type
    ;

simple_ident
    : ident
    | (Dot?|ident Dot) ident Dot ident
    ;

opt_sp_create_chistics
    : empty
    | opt_sp_create_chistics sp_create_chistic
    ;

sp_create_chistic
    : sp_chistic
    | NOT? DETERMINISTIC
    ;

sp_chistic
    : COMMENT STRING
    | LANGUAGE SQL
    | NO SQL
    | CONTAINS SQL
    | (MODIFIES|READS) SQL DATA
    | SQL SECURITY (DEFINER|INVOKER)
    ;

procedure_body
    : sp_proc_stmt
    ;

function_body
    : sp_proc_independent_statement
    ;

alter_procedure_stmt
    : ALTER PROCEDURE sp_name (opt_sp_alter_chistics sp_chistic)?
    ;

alter_function_stmt
    : ALTER FUNCTION sp_name (opt_sp_alter_chistics sp_chistic)?
    ;

opt_sp_alter_chistics
    : empty
    | opt_sp_alter_chistics sp_chistic
    ;

sp_proc_stmt
    : sp_proc_outer_statement
    | sp_proc_inner_statement
    ;

sp_proc_outer_statement
    : outer_stmt
    ;

sp_proc_inner_statement
    : sp_proc_independent_statement
    | sp_proc_stmt_iterate
    | sp_proc_stmt_leave
    | sp_proc_stmt_open
    | sp_proc_stmt_fetch
    | sp_proc_stmt_close
    ;

sp_proc_independent_statement
    : sp_proc_stmt_if
    | sp_proc_stmt_case
    | sp_unlabeled_block
    | sp_labeled_block
    | sp_unlabeled_control
    | sp_labeled_control
    | sp_proc_stmt_return
    ;

sp_proc_stmt_if
    : IF sp_if END_KEY IF
    ;

sp_if
    : expr THEN sp_proc_stmts ((ELSEIF sp_if) | (ELSE sp_proc_stmts))?
    ;

sp_proc_stmt_case
    : CASE expr sp_when_list ((ELSEIF sp_if) | (ELSE sp_proc_stmts))? END_KEY CASE
    ;

sp_when_list
    : sp_when+
    ;

sp_when
    : WHEN expr THEN sp_proc_stmts
    ;

sp_unlabeled_block
    : sp_block_content
    ;

sp_block_content
    : BEGIN_KEY (opt_sp_decls sp_decl DELIMITER)? sp_proc_stmts? END_KEY
    ;

sp_labeled_block
    : label_ident Colon sp_block_content label_ident?
    ;

label_ident
    : ident
    ;

sp_proc_stmts
    : sp_proc_stmt DELIMITER
    | sp_proc_stmts sp_proc_stmt DELIMITER
    ;

opt_sp_decls
    : empty
    | opt_sp_decls sp_decl DELIMITER
    ;

sp_decl
    : DECLARE sp_decl_idents sp_data_type (DEFAULT expr)?
    | DECLARE ident CONDITION FOR sp_cond
    | DECLARE sp_handler_type HANDLER FOR sp_hcond_list sp_proc_stmt
    | DECLARE ident CURSOR FOR sql_stmt
    ;

sp_handler_type
    : EXIT
    | CONTINUE
    ;

sp_hcond_list
    : sp_hcond (Comma sp_hcond)*
    ;

sp_hcond
    : sp_cond
    | IDENT
    | SQLWARNING
    | NOT FOUND
    | SQLEXCEPTION
    ;

sp_cond
    : number_literal
    | sqlstate
    ;

sqlstate
    : SQLSTATE VALUE? STRING
    ;

sp_proc_stmt_open
    : OPEN ident
    ;

sp_proc_stmt_close
    : CLOSE ident
    ;

sp_proc_stmt_fetch
    : FETCH (FROM?|NEXT FROM) ident into_clause
    ;

into_clause
    : INTO expr_list
    ;

sp_decl_idents
    : ident (Comma ident)*
    ;

sp_data_type
    : scalar_data_type
    ;

expr_list
    : expr (Comma expr)*
    ;

expr
    : (~(DELIMITER))*?{this.ForwardExpr($ctx, "expr");}
    ;

sp_unlabeled_control
    : LOOP sp_proc_stmts END_KEY LOOP
    | WHILE expr DO sp_proc_stmts END_KEY WHILE
    | REPEAT sp_proc_stmts UNTIL expr END_KEY REPEAT
    ;

sp_labeled_control
    : label_ident Colon sp_unlabeled_control label_ident?
    ;

sp_proc_stmt_return
    : RETURN expr
    ;

sp_proc_stmt_iterate
    : ITERATE label_ident
    ;

sp_proc_stmt_leave
    : LEAVE label_ident
    ;

drop_procedure_stmt
    : DROP PROCEDURE (IF EXISTS)? sp_name
    ;

drop_function_stmt
    : DROP FUNCTION (IF EXISTS)? sp_name
    ;

scalar_data_type
    : int_type_i (LeftParen INTNUM RightParen)? (UNSIGNED | SIGNED)? ZEROFILL?
    | float_type_i ((LeftParen INTNUM Comma INTNUM RightParen) | (LeftParen INTNUM RightParen) | (LeftParen DECIMAL_VAL RightParen))? (UNSIGNED | SIGNED)? ZEROFILL?
    | NUMBER ((LeftParen INTNUM Comma INTNUM RightParen) | (LeftParen INTNUM RightParen))? (UNSIGNED | SIGNED)? ZEROFILL?
    | datetime_type_i (LeftParen INTNUM RightParen)?
    | date_year_type_i
    | CHARACTER string_length_i? BINARY? (charset_key charset_name)? collation?
    | VARCHAR string_length_i BINARY? (charset_key charset_name)? collation?
    | BINARY string_length_i?
    | VARBINARY string_length_i
    | STRING
    | BIT (LeftParen INTNUM RightParen)?
    | BOOL
    | BOOLEAN
    | ENUM LeftParen string_list RightParen BINARY? (charset_key charset_name)? collation?
    | SET LeftParen string_list RightParen BINARY? (charset_key charset_name)? collation?
    | pl_obj_access_ref
    | pl_obj_access_ref Mod ROWTYPE
    ;

pl_obj_access_ref
    : sp_call_name
    ;

int_type_i
    : TINYINT
    | SMALLINT
    | MEDIUMINT
    | INTEGER
    | BIGINT
    ;

float_type_i
    : FLOAT
    | DOUBLE PRECISION?
    ;

datetime_type_i
    : DATETIME
    | TIMESTAMP
    | TIME
    ;

date_year_type_i
    : DATE
    | YEAR (LeftParen INTNUM RightParen)?
    ;

number_literal
    : INTNUM
    | DECIMAL_VAL
    ;

literal
    : number_literal
    | DATE_VALUE
    | PARSER_SYNTAX_ERROR
    | NULLX
    ;

string_length_i
    : LeftParen number_literal RightParen
    ;

string_list
    : text_string (Comma text_string)*
    ;

text_string
    : STRING
    | PARSER_SYNTAX_ERROR
    ;

collation_name
    : ident
    | STRING
    ;

charset_name
    : ident
    | STRING
    | BINARY
    ;

charset_key
    : CHARSET
    | CHARACTER SET
    ;

collation
    : COLLATE collation_name
    ;

signal_stmt
    : SIGNAL signal_value (SET signal_information_item_list)?
    ;

resignal_stmt
    : RESIGNAL signal_value? (SET signal_information_item_list)?
    ;

signal_value
    : ident
    | sqlstate
    ;

signal_information_item_list
    : signal_information_item (Comma signal_information_item)*
    ;

signal_information_item
    : scond_info_item_name Equal signal_allowed_expr
    ;

signal_allowed_expr
    : literal
    | variable
    | simple_ident
    ;

variable
    : SYSTEM_VARIABLE
    | USER_VARIABLE
    ;

scond_info_item_name
    : CLASS_ORIGIN
    | SUBCLASS_ORIGIN
    | CONSTRAINT_CATALOG
    | CONSTRAINT_SCHEMA
    | CONSTRAINT_NAME
    | CATALOG_NAME
    | SCHEMA_NAME
    | TABLE_NAME
    | COLUMN_NAME
    | CURSOR_NAME
    | MESSAGE_TEXT
    | MYSQL_ERRNO
    ;

empty
    :
    ;


