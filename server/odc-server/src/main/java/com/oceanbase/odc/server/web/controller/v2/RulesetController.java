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
package com.oceanbase.odc.server.web.controller.v2;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.oceanbase.odc.service.common.model.Stats;
import com.oceanbase.odc.service.common.response.ListResponse;
import com.oceanbase.odc.service.common.response.Responses;
import com.oceanbase.odc.service.common.response.SuccessResponse;
import com.oceanbase.odc.service.regulation.ruleset.RuleService;
import com.oceanbase.odc.service.regulation.ruleset.RulesetService;
import com.oceanbase.odc.service.regulation.ruleset.model.MetadataLabel;
import com.oceanbase.odc.service.regulation.ruleset.model.QueryRuleMetadataParams;
import com.oceanbase.odc.service.regulation.ruleset.model.Rule;
import com.oceanbase.odc.service.regulation.ruleset.model.RuleType;
import com.oceanbase.odc.service.regulation.ruleset.model.Ruleset;

import io.swagger.annotations.ApiOperation;

/**
 * @Author: Lebie
 * @Date: 2023/4/13 15:24
 * @Description: []
 */
@RestController
@RequestMapping("/api/v2/regulation/rulesets")
public class RulesetController {

    @Autowired
    private RulesetService rulesetService;

    @Autowired
    private RuleService ruleService;

    @ApiOperation(value = "getRuleset", notes = "Detail a ruleset")
    @RequestMapping(value = "/{id:[\\d]+}", method = RequestMethod.GET)
    public SuccessResponse<Ruleset> getRuleset(@PathVariable Long id) {
        return Responses.success(rulesetService.detail(id));
    }

    @ApiOperation(value = "getRule", notes = "Detail a rule under a ruleset")
    @RequestMapping(value = "/{rulesetId:[\\d]+}/rules/{ruleId:[\\d]+}", method = RequestMethod.GET)
    public SuccessResponse<Rule> getRule(@PathVariable Long rulesetId, @PathVariable Long ruleId) {
        return Responses.success(ruleService.detail(rulesetId, ruleId));
    }

    @ApiOperation(value = "updateRule", notes = "update a rule")
    @RequestMapping(value = "/{rulesetId:[\\d]+}/rules/{ruleId:[\\d]+}", method = RequestMethod.PUT)
    public SuccessResponse<Rule> updateRule(@PathVariable Long rulesetId, @PathVariable Long ruleId,
            @RequestBody Rule rule) {
        return Responses.success(ruleService.update(rulesetId, ruleId, rule));
    }

    @ApiOperation(value = "listRules", notes = "list rules")
    @RequestMapping(value = "/{rulesetId:[\\d]+}/rules", method = RequestMethod.GET)
    public ListResponse<Rule> listRules(@PathVariable Long rulesetId,
            @RequestParam(name = "types", required = false) List<RuleType> ruleTypes,
            @RequestParam(name = "subTypes", required = false) List<String> subTypes,
            @RequestParam(name = "supportedDialectTypes", required = false) List<String> supportedDialectTypes) {
        Map<MetadataLabel, List<String>> labels = new HashMap<>();
        if (CollectionUtils.isNotEmpty(subTypes)) {
            labels.put(MetadataLabel.SUB_TYPE, subTypes);
        }
        if (CollectionUtils.isNotEmpty(supportedDialectTypes)) {
            labels.put(MetadataLabel.SUPPORTED_DIALECT_TYPE, supportedDialectTypes);
        }
        QueryRuleMetadataParams params = QueryRuleMetadataParams.builder().ruleTypes(ruleTypes).labels(labels).build();
        return Responses.list(ruleService.list(rulesetId, params));
    }

    @ApiOperation(value = "statsRules", notes = "rules stats")
    @RequestMapping(value = "/{rulesetId:[\\d]+}/rules/stats", method = RequestMethod.GET)
    public SuccessResponse<Stats> statsRules(@PathVariable Long rulesetId,
            @RequestParam(name = "type", required = false) RuleType type) {
        QueryRuleMetadataParams params = new QueryRuleMetadataParams();
        if (Objects.nonNull(type)) {
            params.setRuleTypes(Arrays.asList(type));
        }
        return Responses.success(ruleService.statsRules(rulesetId, params));
    }
}
