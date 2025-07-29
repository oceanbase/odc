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
package com.oceanbase.odc.metadb.schedule;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import org.apache.poi.ss.formula.functions.T;
import org.springframework.data.jpa.domain.Specification;

import com.oceanbase.odc.service.schedule.model.QueryScheduleParams;
import com.oceanbase.odc.service.schedule.model.ScheduleStatus;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.validation.constraints.NotNull;

/**
 * @author longpeng.zlp
 * @date 2025/7/16 10:22
 */
public class ScheduleSpecs {
    private static final String SCHEDULE_CHANGE_LOG = "scheduleChangeLog";

    /**
     * 创建查询条件： SELECT a.* FROM schedule_schedule a INNER JOIN schedule_changelog b ON
     * a.latest_schedule_changelog_id = b.id WHERE b.status IN ('SUCCESS') AND a.organization_id = xx
     * AND a.latest_schedule_changelog_id IS NOT NULL AND ....
     */
    public static Specification<ScheduleEntity> joinScheduleChangeLog(@NotNull QueryScheduleParams params) {
        return (root, query, cb) -> {
            // build join
            // INNER JOIN schedule_changelog b ON a.latest_schedule_changelog_id = b.id
            Join<ScheduleEntity, ScheduleChangeLogEntity> join =
                    root.join(SCHEDULE_CHANGE_LOG, JoinType.INNER);

            // create where clause
            // WHERE b.status IN ('SUCCESS')
            // AND a.organization_id = xx
            // AND a.latest_schedule_changelog_id IS NOT NULL
            // AND ....
            //
            List<Predicate> predicateList = buildPredictList(params, cb, root, join);
            return cb.and(predicateList.toArray(new Predicate[0]));
        };
    }

    private static List<Predicate> buildPredictList(@NotNull QueryScheduleParams params, CriteriaBuilder cb,
            Root<ScheduleEntity> root, Join<ScheduleEntity, ScheduleChangeLogEntity> join) {
        List<Predicate> predicateList = new ArrayList<>();
        buildSpecs(params.getStartTime(), (t) -> cb.greaterThanOrEqualTo(root.get(ScheduleEntity_.createTime), t),
                predicateList::add);
        buildSpecs(params.getEndTime(), (t) -> cb.lessThanOrEqualTo(root.get(ScheduleEntity_.createTime), t),
                predicateList::add);
        buildSpecs(params.getDataSourceIds(), (t) -> {
            Path<?> expression = root.get(ScheduleEntity_.dataSourceId);
            return cb.isTrue(expression.in(t));
        }, predicateList::add);
        buildSpecs(params.getType(), (t) -> cb.equal(root.get(ScheduleEntity_.type), t), predicateList::add);
        buildSpecs(params.getProjectIds(), (t) -> {
            Path<?> expression = root.get(ScheduleEntity_.projectId);
            return cb.isTrue(expression.in(t));
        }, predicateList::add);
        buildSpecs(params.getId(), (t) -> cb.equal(root.get(ScheduleEntity_.id), t), predicateList::add);
        buildSpecs(params.getId(), (t) -> cb.equal(root.get(ScheduleEntity_.id), t), predicateList::add);
        buildSpecs(params.getStatuses(), (t) -> {
            Path<?> expression = root.get(ScheduleEntity_.status);
            return cb.isTrue(expression.in(t));
        }, predicateList::add);
        buildSpecs(ScheduleStatus.DELETED, (t) -> cb.notEqual(root.get(ScheduleEntity_.status), t), predicateList::add);
        buildSpecs(params.getCreatorIds(), (t) -> {
            Path<?> expression = root.get(ScheduleEntity_.creatorId);
            return cb.isTrue(expression.in(t));
        }, predicateList::add);
        buildSpecs(params.getName(), (t) -> cb.like(root.get(ScheduleEntity_.name), "%" + t + "%"), predicateList::add);
        buildSpecs(params.getOrganizationId(), (t) -> cb.equal(root.get(ScheduleEntity_.organizationId), t),
                predicateList::add);
        buildSpecs(Boolean.FALSE, (t) -> cb.equal(root.get(ScheduleEntity_.isInner), t), predicateList::add);
        buildSpecs(Boolean.FALSE, (t) -> cb.isNotNull(root.get(ScheduleEntity_.LATEST_SCHEDULE_CHANGELOG_ID)),
                predicateList::add);
        buildSpecs(params.getLatestScheduleChangeStatuses(), (t) -> {
            Path<?> expression = join.get(ScheduleChangeLogEntity_.STATUS);
            return cb.isTrue(expression.in(t));
        }, predicateList::add);
        return predicateList;
    }

    private static <T> void buildSpecs(T parameters, Function<T, Predicate> caster, Consumer<Predicate> consumer) {
        if (null == parameters) {
            return;
        }
        Predicate predicate = caster.apply(parameters);
        if (null != predicate) {
            consumer.accept(predicate);
        }
    }
}


