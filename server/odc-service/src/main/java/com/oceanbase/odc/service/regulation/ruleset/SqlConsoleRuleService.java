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
        // 获取规则集ID
        Long ruleSetId = ConnectionSessionUtil.getRuleSetId(connectionSession);
        // 如果规则集ID为空或者控制台规则不是布尔类型，则抛出异常
        if (Objects.isNull(ruleSetId) || !consoleRules.isBooleanRule()) {
            throw new UnexpectedException("find sql rule failed");
        }
        // 如果当前用户属于个人组织，则不被禁止执行
        if (authenticationFacade.currentUser().getOrganizationType() == OrganizationType.INDIVIDUAL) {
            return false;
        }
        // 从缓存中获取所有规则，并过滤出规则名称与控制台规则名称相同的规则
        List<Rule> rules = ruleService.listAllFromCache(ruleSetId).stream()
            .filter(rule -> StringUtils.equals(consoleRules.getRuleName(), rule.getMetadata().getName())).collect(
                Collectors.toList());
        // 确保只有一个规则符合条件
        Verify.singleton(rules, "rules");
        Rule rule = rules.get(0);
        // 如果规则应用的方言类型列表为空或者不包含当前数据库方言类型，则不被禁止执行
        if (CollectionUtils.isEmpty(rule.getAppliedDialectTypes())
            || !rule.getAppliedDialectTypes().contains(connectionSession.getDialectType())) {
            return false;
        }
        // 规则被启用，则被禁止执行
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

    /**
     * 获取指定规则集合中指定 SQL 控制台规则、指定方言类型和指定类型的属性列表。
     *
     * @param rulesetId   规则集合 ID
     * @param consoleRule SQL 控制台规则
     * @param dialectType 方言类型
     * @param clazz       属性类型
     * @return 属性列表的可选项
     */
    @SkipAuthorize("odc internal usage")
    public <T> Optional<List<T>> getListProperties(@NonNull Long rulesetId, @NonNull SqlConsoleRules consoleRule,
        @NonNull DialectType dialectType, @NonNull Class<T> clazz) {
        try {
            // 从缓存中获取指定规则集合中的所有规则，并过滤出符合条件的规则
            List<List> properties = ruleService.listAllFromCache(rulesetId).stream().filter(rule -> {
                // 规则的元数据名称不等于 SQL 控制台规则的名称
                if (Objects.isNull(rule.getMetadata())
                    || !StringUtils.equals(rule.getMetadata().getName(), consoleRule.getRuleName())) {
                    return false;
                }
                // 规则未启用
                if (Boolean.FALSE.equals(rule.getEnabled())) {
                    return false;
                }
                // 规则应用的方言类型列表中不包含指定的方言类型
                if (Objects.isNull(rule.getAppliedDialectTypes()) || !rule.getAppliedDialectTypes().contains(
                    dialectType)) {
                    return false;
                }
                return true;
            }).map(rule -> {
                // 获取规则的属性列表
                if (Objects.isNull(rule.getProperties())) {
                    return null;
                }
                // 获取指定属性名称的属性值
                Object property = rule.getProperties().getOrDefault(consoleRule.getPropertyName(), null);
                // 属性值为空或不是列表类型
                if (Objects.isNull(property) || !List.class.isAssignableFrom(property.getClass())) {
                    return Collections.EMPTY_LIST;
                }
                // 属性值列表为空
                if (((List<?>) property).isEmpty()) {
                    return Collections.EMPTY_LIST;
                }
                // 将属性值列表中的元素转换为指定类型，并添加到结果列表中
                List<T> result = new ArrayList<>();
                for (Object o : (List<?>) property) {
                    if (clazz.equals(o.getClass())) {
                        result.add(clazz.cast(o));
                    }
                }
                return result;
            }).collect(Collectors.toList());
            // 如果属性列表为空，则返回空的可选项
            if (CollectionUtils.isEmpty(properties)) {
                // 发生异常时，返回空的可选项
                return Optional.empty();
            }
            // 如果属性列表的第一个元素为空，则返回空的可选项
            if (properties.get(0).isEmpty()) {
                return Optional.of(Collections.emptyList());
            } else {
                // 否则，返回属性列表的第一个元素作为可选项的值
                return Optional.of((List<T>) properties.get(0));
            }
        } catch (Exception ex) {
            // 发生异常时，返回空的可选项
            return Optional.empty();
        }
    }


}
