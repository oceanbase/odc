-- dev environment
-- update the rule 'allow-sql-types'
update
  regulation_rule_applying rra
set
  rra.properties_json = '{"${com.oceanbase.odc.builtin-resource.regulation.rule.sql-console.allow-sql-types.metadata.name}":["UPDATE", "DELETE", "INSERT", "SELECT", "CREATE", "DROP", "ALTER", "REPLACE", "SET", "USE_DB", "EXPLAIN", "SHOW", "HELP", "START_TRANS", "COMMIT", "ROLLBACK", "SORT", "DESC", "TRUNCATE", "OTHERS"]}'
where
  rra.create_time = rra.update_time
  and rra.rule_metadata_id = (
    select
      rrm.id
    from
      regulation_rule_metadata rrm
    where
      rrm.type = 'SQL_CONSOLE'
      and rrm.name = '${com.oceanbase.odc.builtin-resource.regulation.rule.sql-console.allow-sql-types.name}'
  )
  and rra.ruleset_id in (
    select
      rr.id
    from
      regulation_ruleset rr
    where
      rr.name = '${com.oceanbase.odc.builtin-resource.regulation.ruleset.default-dev-ruleset.name}'
  );

-- update the rule 'max-return-rows'
update
  regulation_rule_applying rra
set
  rra.properties_json = '{"${com.oceanbase.odc.builtin-resource.regulation.rule.sql-console.max-return-rows.metadata.name}":100000}'
where
  rra.create_time = rra.update_time
  and rra.rule_metadata_id = (
    select
      rrm.id
    from
      regulation_rule_metadata rrm
    where
      rrm.type = 'SQL_CONSOLE'
      and rrm.name = '${com.oceanbase.odc.builtin-resource.regulation.rule.sql-console.max-return-rows.name}'
  )
  and rra.ruleset_id in (
    select
      rr.id
    from
      regulation_ruleset rr
    where
      rr.name = '${com.oceanbase.odc.builtin-resource.regulation.ruleset.default-dev-ruleset.name}'
  );

-- sit environment
-- update the rule 'max-return-rows'
update
  regulation_rule_applying rra
set
  rra.properties_json = '{"${com.oceanbase.odc.builtin-resource.regulation.rule.sql-console.max-return-rows.metadata.name}":100000}'
where
  rra.create_time = rra.update_time
  and rra.rule_metadata_id = (
    select
      rrm.id
    from
      regulation_rule_metadata rrm
    where
      rrm.type = 'SQL_CONSOLE'
      and rrm.name = '${com.oceanbase.odc.builtin-resource.regulation.rule.sql-console.max-return-rows.name}'
  )
  and rra.ruleset_id in (
    select
      rr.id
    from
      regulation_ruleset rr
    where
      rr.name = '${com.oceanbase.odc.builtin-resource.regulation.ruleset.default-sit-ruleset.name}'
  );

-- prod environment
-- update the rule 'allow-sql-types'
update
  regulation_rule_applying rra
set
  rra.properties_json = '{"${com.oceanbase.odc.builtin-resource.regulation.rule.sql-console.allow-sql-types.metadata.name}":["SELECT", "EXPLAIN", "SHOW", "HELP", "START_TRANS", "COMMIT", "ROLLBACK", "SORT", "DESC"]}'
where
  rra.create_time = rra.update_time
  and rra.rule_metadata_id = (
    select
      rrm.id
    from
      regulation_rule_metadata rrm
    where
      rrm.type = 'SQL_CONSOLE'
      and rrm.name = '${com.oceanbase.odc.builtin-resource.regulation.rule.sql-console.allow-sql-types.name}'
  )
  and rra.ruleset_id in (
    select
      rr.id
    from
      regulation_ruleset rr
    where
      rr.name = '${com.oceanbase.odc.builtin-resource.regulation.ruleset.default-prod-ruleset.name}'
  );

-- default environment
-- update the rule 'allow-sql-types'
update
  regulation_rule_applying rra
set
  rra.properties_json = '{"${com.oceanbase.odc.builtin-resource.regulation.rule.sql-console.allow-sql-types.metadata.name}":["SELECT", "EXPLAIN", "SHOW", "HELP", "START_TRANS", "COMMIT", "ROLLBACK", "SORT", "DESC"]}'
where
  rra.create_time = rra.update_time
  and rra.rule_metadata_id = (
    select
      rrm.id
    from
      regulation_rule_metadata rrm
    where
      rrm.type = 'SQL_CONSOLE'
      and rrm.name = '${com.oceanbase.odc.builtin-resource.regulation.rule.sql-console.allow-sql-types.name}'
  )
  and rra.ruleset_id in (
    select
      rr.id
    from
      regulation_ruleset rr
    where
      rr.name = '${com.oceanbase.odc.builtin-resource.regulation.ruleset.default-default-ruleset.name}'
  );