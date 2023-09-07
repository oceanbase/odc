UPDATE regulation_rule_applying rra
SET rra.applied_dialect_types = CONCAT(SUBSTRING(rra.applied_dialect_types, 1, LENGTH(rra.applied_dialect_types) - 1), ',"MYSQL"]')
where rra.rule_metadata_id in (
    select id from regulation_rule_metadata rrm where rrm.name not in (
        '${com.oceanbase.odc.builtin-resource.regulation.rule.sql-check.prefer-local-index.name}',
        '${com.oceanbase.odc.builtin-resource.regulation.rule.sql-check.restrict-column-name-case.name}',
        '${com.oceanbase.odc.builtin-resource.regulation.rule.sql-check.restrict-table-name-case.name}'
    )
);