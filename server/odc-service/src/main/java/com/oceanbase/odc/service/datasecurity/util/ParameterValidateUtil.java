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

import java.util.List;
import java.util.regex.Pattern;

import org.codehaus.groovy.control.CompilerConfiguration;

import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.core.shared.PreConditions;
import com.oceanbase.odc.core.shared.constant.ErrorCodes;
import com.oceanbase.odc.service.datasecurity.recognizer.GroovyColumnRecognizer.GroovyColumnMeta;

import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import groovy.lang.Script;

/**
 * @author gaoda.xy
 * @date 2023/5/24 11:36
 */
public class ParameterValidateUtil {

    private static final Pattern PATH_EXPRESSION_PATTERN = Pattern.compile("^[^,.\\s]+\\.[^,.\\s]+\\.[^,.\\s]+$");

    public static void validatePathExpression(List<String> pathIncludes, List<String> pathExcludes) {
        PreConditions.notEmpty(pathIncludes, "pathIncludes");
        for (String include : pathIncludes) {
            String msg = String.format("pathInclude: %s is not valid", include);
            PreConditions.validArgumentState(isValidPathExpression(include), ErrorCodes.IllegalArgument,
                    new Object[] {"pathInclude", msg}, msg);
        }
        for (String exclude : pathExcludes) {
            String msg = String.format("pathExclude: %s is not valid", exclude);
            PreConditions.validArgumentState(isValidPathExpression(exclude), ErrorCodes.IllegalArgument,
                    new Object[] {"pathExclude", msg}, msg);
        }
    }

    public static void validateRegexExpression(String databaseRegexExpression, String tableRegexExpression,
            String columnRegexExpression, String columnCommentRegexExpression) {
        PreConditions.validArgumentState(StringUtils.isNotBlank(databaseRegexExpression)
                || StringUtils.isNotBlank(tableRegexExpression)
                || StringUtils.isNotBlank(columnRegexExpression)
                || StringUtils.isNotBlank(columnCommentRegexExpression),
                ErrorCodes.BadArgument, null, null);
        if (StringUtils.isNotBlank(databaseRegexExpression)) {
            String msg = String.format("databaseRegexExpression: %s is not valid", databaseRegexExpression);
            PreConditions.validArgumentState(isValidRegexExpression(databaseRegexExpression),
                    ErrorCodes.IllegalArgument,
                    new Object[] {"databaseRegexExpression", msg}, msg);
        }
        if (StringUtils.isNotBlank(tableRegexExpression)) {
            String msg = String.format("tableRegexExpression: %s is not valid", tableRegexExpression);
            PreConditions.validArgumentState(isValidRegexExpression(tableRegexExpression), ErrorCodes.IllegalArgument,
                    new Object[] {"tableRegexExpression", msg}, msg);
        }
        if (StringUtils.isNotBlank(columnRegexExpression)) {
            String msg = String.format("columnRegexExpression: %s is not valid", columnRegexExpression);
            PreConditions.validArgumentState(isValidRegexExpression(columnRegexExpression), ErrorCodes.IllegalArgument,
                    new Object[] {"columnRegexExpression", msg}, msg);
        }
        if (StringUtils.isNotBlank(columnCommentRegexExpression)) {
            String msg = String.format("columnCommentRegexExpression: %s is not valid", columnCommentRegexExpression);
            PreConditions.validArgumentState(isValidRegexExpression(columnCommentRegexExpression),
                    ErrorCodes.IllegalArgument, new Object[] {"columnCommentRegexExpression", msg}, msg);
        }
    }

    public static void validateGroovyScript(String groovyScript) {
        PreConditions.notBlank(groovyScript, "groovyScript");
        executeGroovyScript(groovyScript);
    }

    private static boolean isValidPathExpression(String pathExpression) {
        return PATH_EXPRESSION_PATTERN.matcher(pathExpression).matches();
    }

    private static void executeGroovyScript(String groovyScript) {
        // Check whether the groovy script can be compiled and executed
        CompilerConfiguration config = new CompilerConfiguration();
        config.addCompilationCustomizers(SecureAstCustomizerUtil.buildSecureASTCustomizer());
        GroovyShell shell = new GroovyShell(config);
        Script script = shell.parse(groovyScript);
        GroovyColumnMeta groovyColumnMeta = new GroovyColumnMeta("schema", "table", "column", "comment", "type");
        Binding binding = new Binding();
        binding.setVariable("column", groovyColumnMeta);
        script.setBinding(binding);
        script.run();
    }

    private static boolean isValidRegexExpression(String regexExpression) {
        try {
            Pattern.compile(regexExpression);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
