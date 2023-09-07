UPDATE
  regulation_rule_applying
SET
  applied_dialect_types = IF(
    applied_dialect_types = '[]',
    '["MYSQL"]',
    CONCAT(
      SUBSTRING(
        applied_dialect_types,
        1,
        LENGTH(applied_dialect_types) - 1
      ),
      ',"MYSQL"]'
    )
  )
where
  rule_metadata_id in (
    select
      id
    from
      regulation_rule_metadata rrm
    where
      rrm.name not in (
        '${com.oceanbase.odc.builtin-resource.regulation.rule.sql-check.prefer-local-index.name}',
        '${com.oceanbase.odc.builtin-resource.regulation.rule.sql-check.restrict-column-name-case.name}',
        '${com.oceanbase.odc.builtin-resource.regulation.rule.sql-check.restrict-table-name-case.name}'
      )
  );

UPDATE
  regulation_rule_applying
SET
  applied_dialect_types = IF(
    applied_dialect_types = '[]',
    '["ODP_SHARDING_OB_MYSQL"]',
    CONCAT(
      SUBSTRING(
        applied_dialect_types,
        1,
        LENGTH(applied_dialect_types) - 1
      ),
      ',"ODP_SHARDING_OB_MYSQL"]'
    )
  )
where
  rule_metadata_id in (
    select
      id
    from
      regulation_rule_metadata rrm
    where
      rrm.name not in (
        '${com.oceanbase.odc.builtin-resource.regulation.rule.sql-check.restrict-column-name-case.name}',
        '${com.oceanbase.odc.builtin-resource.regulation.rule.sql-check.restrict-table-name-case.name}'
      )
  );