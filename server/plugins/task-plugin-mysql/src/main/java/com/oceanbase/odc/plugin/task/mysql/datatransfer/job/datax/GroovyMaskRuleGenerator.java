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

package com.oceanbase.odc.plugin.task.mysql.datatransfer.job.datax;

import static com.oceanbase.odc.plugin.task.mysql.datatransfer.common.Constants.LINE_BREAKER;

import java.net.URL;
import java.text.MessageFormat;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.MapUtils;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.oceanbase.odc.core.datamasking.masker.AbstractDataMasker;
import com.oceanbase.odc.core.shared.model.TableIdentity;
import com.oceanbase.tools.dbbrowser.model.DBTableColumn;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

public class GroovyMaskRuleGenerator {
    private static final String DEFAULT_FIVE_STARS = "*****";
    private static final RuleMapper MASKER_2_TEMPLATE_MAPPERS = fromYaml("/mask_rule_template.yaml");

    /**
     * Column names must strictly follow the order in the table. Currently, we only support using
     * '******' to mask fields.
     * 
     * TODO: get template and generate rule by algorithm
     */
    public static String generate(Map<TableIdentity, Map<String, AbstractDataMasker>> maskConfigs,
            List<DBTableColumn> columns) {
        StringBuilder ruleBuilder = new StringBuilder();
        for (int index = 0; index < columns.size(); index++) {
            DBTableColumn column = columns.get(index);
            Map<String, AbstractDataMasker> field2Masker = maskConfigs.get(
                    TableIdentity.of(column.getSchemaName(), column.getTableName()));
            if (MapUtils.isEmpty(field2Masker) || !field2Masker.containsKey(column.name())) {
                continue;
            }
            String maskTemplate = MASKER_2_TEMPLATE_MAPPERS.getRule("mask");
            ruleBuilder.append(MessageFormat.format(maskTemplate, index, DEFAULT_FIVE_STARS)).append(LINE_BREAKER);
        }

        ruleBuilder.append("return record;").append(LINE_BREAKER);
        return ruleBuilder.toString();
    }

    private static RuleMapper fromYaml(String resource) {
        URL url = GroovyMaskRuleGenerator.class.getResource(resource);
        try {
            ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory())
                    .setPropertyNamingStrategy(PropertyNamingStrategies.LOWER_CAMEL_CASE)
                    .setSerializationInclusion(JsonInclude.Include.NON_ABSENT)
                    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            return yamlMapper.readValue(url, RuleMapper.class);
        } catch (Exception ex) {
            return new RuleMapper();
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    private static class RuleMapper {
        Map<String, String> rules;

        public String getRule(String id) {
            if (rules == null) {
                return null;
            }
            return rules.get(id);
        }
    }

}
