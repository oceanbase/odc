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
package com.oceanbase.odc.service.datasecurity.recognizer;

import org.codehaus.groovy.control.CompilerConfiguration;

import com.oceanbase.odc.service.datasecurity.util.SecureAstCustomizerUtil;
import com.oceanbase.tools.dbbrowser.model.DBTableColumn;

import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import groovy.lang.Script;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * @author gaoda.xy
 * @date 2023/5/30 10:50
 */
public class GroovyColumnRecognizer implements ColumnRecognizer {

    private final Script script;
    private static final String COLUMN_KEYWORD = "column";

    public GroovyColumnRecognizer(String groovyScript) {
        CompilerConfiguration config = new CompilerConfiguration();
        config.addCompilationCustomizers(SecureAstCustomizerUtil.buildSecureASTCustomizer());
        GroovyShell shell = new GroovyShell(config);
        this.script = shell.parse(groovyScript);
    }

    @Override
    public boolean recognize(DBTableColumn column) {
        try {
            GroovyColumnMeta groovyColumnMeta = new GroovyColumnMeta(column);
            Binding binding = new Binding();
            binding.setVariable(COLUMN_KEYWORD, groovyColumnMeta);
            script.setBinding(binding);
            return (boolean) script.run();
        } catch (Exception e) {
            return false;
        }
    }

    @Data
    @AllArgsConstructor
    public static class GroovyColumnMeta {
        private String schema;
        private String table;
        private String name;
        private String comment;
        private String type;

        public GroovyColumnMeta(DBTableColumn column) {
            this.schema = column.getSchemaName();
            this.table = column.getTableName();
            this.name = column.getName();
            this.comment = column.getComment();
            this.type = column.getTypeName();
        }
    }

}
