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
package com.oceanbase.odc.service.datasecurity.util;

import java.util.Arrays;
import java.util.List;

import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.ast.expr.MethodCallExpression;
import org.codehaus.groovy.ast.expr.StaticMethodCallExpression;
import org.codehaus.groovy.ast.stmt.DoWhileStatement;
import org.codehaus.groovy.ast.stmt.ForStatement;
import org.codehaus.groovy.ast.stmt.Statement;
import org.codehaus.groovy.ast.stmt.SynchronizedStatement;
import org.codehaus.groovy.ast.stmt.ThrowStatement;
import org.codehaus.groovy.ast.stmt.WhileStatement;
import org.codehaus.groovy.control.customizers.SecureASTCustomizer;
import org.codehaus.groovy.control.customizers.SecureASTCustomizer.ExpressionChecker;
import org.codehaus.groovy.syntax.Types;

/**
 * @author gaoda.xy
 * @date 2023/5/29 22:27
 */
public class SecureAstCustomizerUtil {

    private static final List<String> IMPORT_WHITELIST = Arrays.asList(
            "java.lang.String",
            "java.lang.Object",
            "java.lang.Math",
            "java.lang.Integer",
            "java.lang.Float",
            "java.lang.Double",
            "java.lang.Long",
            "java.lang.Short",
            "java.lang.Character",
            "java.lang.Byte",
            "java.lang.Boolean",
            "java.lang.StringBuilder",
            "java.lang.StringBuffer",
            "java.lang.Enum",
            "java.lang.StrictMath",
            "java.net.URLEncoder",
            "java.net.URLDecoder",
            "java.net.URL",
            "java.net.URI",
            "java.util.Arrays",
            "java.util.Date",
            "java.util.ArrayList",
            "java.util.HashMap",
            "java.util.HashSet",
            "java.math.BigDecimal",
            "java.nio.charset.Charset",
            "java.text.NumberFormat",
            "groovy.util.GroovyCollections");

    private static final List<String> IMPORT_STAR_WHITELIST = Arrays.asList(
            "java.lang.String.*",
            "java.lang.Object.*",
            "java.lang.Math.*",
            "java.lang.Integer.*",
            "java.lang.Float.*",
            "java.lang.Double.*",
            "java.lang.Long.*",
            "java.lang.Short.*",
            "java.lang.Character.*",
            "java.lang.Byte.*",
            "java.lang.Boolean.*",
            "java.lang.StringBuilder.*",
            "java.lang.StringBuffer.*",
            "java.lang.Enum.*",
            "java.lang.StrictMath.*",
            "java.net.URLEncoder.*",
            "java.net.URLDecoder.*",
            "java.net.URL.*",
            "java.net.URI.*",
            "java.util.Arrays.*",
            "java.util.Date.*",
            "java.util.ArrayList.*",
            "java.util.HashMap.*",
            "java.util.HashSet.*",
            "java.math.BigDecimal.*",
            "java.nio.charset.Charset.*",
            "java.text.NumberFormat.*",
            "groovy.util.GroovyCollections.*");

    private static final List<String> CLASS_BLACKLIST = Arrays.asList(
            "java.lang.System",
            "java.lang.Thread",
            "java.lang.Shutdown",
            "java.lang.Class",
            "java.lang.ClassLoader",
            "java.lang.Runnable",
            "groovy.lang.GroovyShell");

    private static final List<String> METHOD_BLACKLIST = Arrays.asList(
            "execute",
            "exec",
            "run");

    private static final List<Integer> TOKEN_BLACKLIST = Arrays.asList(
            Types.KEYWORD_IMPORT,
            Types.KEYWORD_PACKAGE,
            Types.KEYWORD_DO,
            Types.KEYWORD_WHILE,
            Types.KEYWORD_GOTO,
            Types.KEYWORD_FOR,
            Types.KEYWORD_THROW,
            Types.KEYWORD_THROWS,
            Types.KEYWORD_DEF,
            Types.KEYWORD_DEFMACRO);

    private static final List<Class<? extends Statement>> STATEMENT_BLACKLIST = Arrays.asList(
            WhileStatement.class,
            DoWhileStatement.class,
            ForStatement.class,
            ThrowStatement.class,
            SynchronizedStatement.class);

    public static SecureASTCustomizer buildSecureASTCustomizer() {
        SecureASTCustomizer secureASTCustomizer = new SecureASTCustomizer();
        secureASTCustomizer.setPackageAllowed(false);
        secureASTCustomizer.setClosuresAllowed(false);
        secureASTCustomizer.setMethodDefinitionAllowed(false);
        secureASTCustomizer.setIndirectImportCheckEnabled(true);
        secureASTCustomizer.setImportsWhitelist(IMPORT_WHITELIST);
        secureASTCustomizer.setStaticImportsWhitelist(IMPORT_WHITELIST);
        secureASTCustomizer.setStarImportsWhitelist(IMPORT_STAR_WHITELIST);
        secureASTCustomizer.setStaticStarImportsWhitelist(IMPORT_STAR_WHITELIST);
        secureASTCustomizer.setTokensBlacklist(TOKEN_BLACKLIST);
        secureASTCustomizer.setStatementsBlacklist(STATEMENT_BLACKLIST);
        secureASTCustomizer.addExpressionCheckers(new OdcGroovyExpressionChecker());
        return secureASTCustomizer;
    }

    private static class OdcGroovyExpressionChecker implements ExpressionChecker {
        @Override
        public boolean isAuthorized(Expression exp) {
            if (exp instanceof MethodCallExpression) {
                MethodCallExpression mcExp = (MethodCallExpression) exp;
                // intercept invalid MethodCallExpression
                String clazz = mcExp.getObjectExpression().getType().getName();
                String method = mcExp.getMethodAsString();
                if (CLASS_BLACKLIST.contains(clazz) || METHOD_BLACKLIST.contains(method)) {
                    String msg = String.format("Method call is not security [Class: %s, Method: %s]", clazz, method);
                    throw new SecurityException(msg);
                }
            }
            if (exp instanceof StaticMethodCallExpression) {
                StaticMethodCallExpression smcExp = (StaticMethodCallExpression) exp;
                String clazz = smcExp.getOwnerType().getName();
                String method = smcExp.getMethodAsString();
                if (CLASS_BLACKLIST.contains(clazz) || METHOD_BLACKLIST.contains(method)) {
                    String msg =
                            String.format("Static method call is not security [Class: %s, Method: %s]", clazz, method);
                    throw new SecurityException(msg);
                }
            }
            return true;
        }
    }

}
