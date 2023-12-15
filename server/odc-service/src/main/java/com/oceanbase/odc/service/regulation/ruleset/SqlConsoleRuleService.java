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
package com.oceanbase.odc.service.regulation.ruleset;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.alibaba.druid.util.StringUtils;
import com.oceanbase.odc.core.authority.util.SkipAuthorize;
import com.oceanbase.odc.core.session.ConnectionSession;
import com.oceanbase.odc.core.session.ConnectionSessionUtil;
import com.oceanbase.odc.core.shared.Verify;
import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.core.shared.constant.OrganizationType;
import com.oceanbase.odc.core.shared.exception.UnexpectedException;
import com.oceanbase.odc.service.iam.auth.AuthenticationFacade;
import com.oceanbase.odc.service.regulation.ruleset.model.Rule;
import com.oceanbase.odc.service.regulation.ruleset.model.SqlConsoleRules;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * @Author: Lebie
 * @Date: 2023/7/17 15:54
 * @Description: []
 */
@Service
@Slf4j
public class SqlConsoleRuleService {
    @Autowired
    private RuleService ruleService;

    @Autowired
    private AuthenticationFacade authenticationFacade;

    @SkipAuthorize("odc internal usage")
    public boolean isForbidden(@NonNull SqlConsoleRules consoleRules,
            @NonNull ConnectionSession connectionSession) {
        Long ruleSetId = ConnectionSessionUtil.getRuleSetId(connectionSession);
        if (Objects.isNull(ruleSetId) || !consoleRules.isBooleanRule()) {
            throw new UnexpectedException("find sql rule failed");
        }
        if (authenticationFacade.currentUser().getOrganizationType() == OrganizationType.INDIVIDUAL) {
            return false;
        }
        List<Rule> rules = ruleService.listAllFromCache(ruleSetId).stream()
                .filter(rule -> StringUtils.equals(consoleRules.getRuleName(), rule.getMetadata().getName())).collect(
                        Collectors.toList());
        Verify.singleton(rules, "rules");
        Rule rule = rules.get(0);
        if (CollectionUtils.isEmpty(rule.getAppliedDialectTypes())
                || !rule.getAppliedDialectTypes().contains(connectionSession.getDialectType())) {
            return false;
        }
        return rule.getEnabled();
    }

    @SkipAuthorize("odc internal usage")
    public <T> Optional<T> getProperties(@NonNull Long rulesetId, @NonNull SqlConsoleRules consoleRule,
            @NonNull DialectType dialectType, @NonNull Class<T> clazz) {
        List<T> properties = ruleService.listAllFromCache(rulesetId).stream().filter(rule -> {
            if (Objects.isNull(rule.getMetadata())
                    || !StringUtils.equals(rule.getMetadata().getName(), consoleRule.getRuleName())) {
                return false;
            }
            if (Boolean.FALSE.equals(rule.getEnabled())) {
                return false;
            }
            if (Objects.isNull(rule.getAppliedDialectTypes()) || !rule.getAppliedDialectTypes().contains(
                    dialectType)) {
                return false;
            }
            return true;
        }).map(rule -> {
            if (Objects.isNull(rule.getProperties())) {
                return null;
            }
            Object property = rule.getProperties().getOrDefault(consoleRule.getPropertyName(), null);
            if (Objects.isNull(property) || !property.getClass().equals(clazz)) {
                return null;
            }
            return (T) property;
        }).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(properties) || Objects.isNull(properties.get(0))) {
            return Optional.empty();
        }
        return Optional.of(properties.get(0));
    }

    @SkipAuthorize("odc internal usage")
    public <T> Optional<List<T>> getListProperties(@NonNull Long rulesetId, @NonNull SqlConsoleRules consoleRule,
            @NonNull DialectType dialectType, @NonNull Class<T> clazz) {
        try {
            List<List> properties = ruleService.listAllFromCache(rulesetId).stream().filter(rule -> {
                if (Objects.isNull(rule.getMetadata())
                        || !StringUtils.equals(rule.getMetadata().getName(), consoleRule.getRuleName())) {
                    return false;
                }
                if (Boolean.FALSE.equals(rule.getEnabled())) {
                    return false;
                }
                if (Objects.isNull(rule.getAppliedDialectTypes()) || !rule.getAppliedDialectTypes().contains(
                        dialectType)) {
                    return false;
                }
                return true;
            }).map(rule -> {
                if (Objects.isNull(rule.getProperties())) {
                    return Collections.EMPTY_LIST;
                }
                Object property = rule.getProperties().getOrDefault(consoleRule.getPropertyName(), null);
                if (Objects.isNull(property) || !List.class.isAssignableFrom(property.getClass())) {
                    return Collections.EMPTY_LIST;
                }
                if (((List<?>) property).isEmpty()) {
                    return Collections.EMPTY_LIST;
                }
                List<T> result = new ArrayList<>();
                for (Object o : (List<?>) property) {
                    if (clazz.equals(o.getClass())) {
                        result.add(clazz.cast(o));
                    }
                }
                return result;
            }).collect(Collectors.toList());
            if (CollectionUtils.isEmpty(properties) || properties.get(0).isEmpty()) {
                return Optional.of(Collections.emptyList());
            } else {
                return Optional.of((List<T>) properties.get(0));
            }
        } catch (Exception ex) {
            return Optional.empty();
        }
    }


}
