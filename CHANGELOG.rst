(unreleased)
------------

New
~~~
- Feat(dlm):support breakpoint recovery (#635) [guowl3]
- Feat(dlm):support configuring limiter (#626) [guowl3]
- Feat(data-security): add data type unit into response (#629)
  [XiaoYang]
- Feat(dlm): data archive supports MySQL to OB (#544) [guowl3]
- Feat: add timeout settings for pl-debug (#576) [IL MARE]
- Feat: make odc adapt to OceanBase 4.2 (#541) [IL MARE]
- Feat(ob-sql-parser): make ob-sql-parser adapt to OceanBase 4.2 (#441)
  [IL MARE]
- Feat(connection): add initialization configuration capabilities for
  data sources (#488) [IL MARE]
- Feat(data-transfer): upgrade ob-loader-dumper to 4.2.5-RELEASE (#494)
  [LuckyLeo]
- Feat(integration): support retrieve xml format response (#338)
  [XiaoYang]
- Feat(data-security): data masking support columns in view (#97)
  [XiaoYang]
- Feat(encryption): support asymmetric encryption (#99) [XiaoYang]
- Feat(schema-plugin): schema-plugin access service layer (#88)
  [zhangxiao]

Changes
~~~~~~~
- Refactor(unit-test): cherry-pick unit-test commits from 4.2.x to 4.2.2
  (#474) [XiaoYang]
- Refactor(submodule): update submodule (#470) [IL MARE]
- Refactor(unit-test): refact unit test cases (#139) (#142) [IL MARE]
- Refactor(ob-sql-parser): add several new syntaxes which added in
  OceanBase 4.1.0 (#132) [IL MARE]
- Refactor(unit-test): refact unit test cases (#139) (#141) [IL MARE]

Fix
~~~
- Fix(dlm): submit task got condition not supported error while
  condition contains subquery (#668) [guowl3]
- Fix(database-change): failed to view a scheduled database change task
  with rollback plan in personal space (#669) [zhangxiao]
- Fix(pl-debug): enable dbms_output first (#677) [IL MARE]
- Fix(database): use datasource's environment as database's environment
  to prevent data inconsistency  (#659) [pynzzZ]
- Fix: dirty meta data (#663) [XiaoYang]
- Fix(sql-execute): fix failed to get time consuming (#658) [IL MARE]
- Fix(migration): rule metadata migration will be triggered every time
  the ODC server starts up (#649) [pynzzZ]
- Fix(sql-check): fix syntax error check rule can not be disabled (#652)
  [IL MARE]
- Fix: fix can not get plan (#660) [IL MARE]
- Fix(data-transfer): no package body (#653) [LuckyLeo]
- Fix(web): editor.worker.js static resource 404 not found (#656)
  [pynzzZ]
- Fix(data-transfer): fix wrong data objects and schema objects (#620)
  [LuckyLeo]
- Fix(datasource): the data source list refreshes very slowly and cannot
  obtain the connect status while there are a huge amount of data
  sources (#599) [pynzzZ, yh263208]
- Fix: fix failed to query data and sql rules changing is not recorded
  by audit event (#608) [IL MARE]
- Fix(connection): fix failed to set setConnectionAttrs (#601) [IL MARE]
- Fix(db-browser): cannot get table charset in native mysql mode (#592)
  [zhangxiao]
- Fix(result-export): failed to convert CSV file into Excel file (#586)
  [LuckyLeo]
- Fix(diagnose): optimize log information when explain failed (#589)
  [LuckyLeo]
- Fix(pl): fix wrong parameter check error message (#583) [IL MARE]
- Fix(schema-plugin): cannot display constraint name for ob oralce 4.2.1
  (#533) [zhangxiao]
- Fix(pl-debug): fix failed to step in a subprocedure or subfunction
  defined in package (#566) [IL MARE]
- Fix(integration): recover bastion integration (#559) [yizhou]
- Fix(databasechange): fix task costs too much time to start up (#551)
  [IL MARE]
- Fix: remove pl delete code (#548) [IL MARE]
- Fix(ob-sql-parser): fix failed to parse member proc without parameters
  (#546) [IL MARE]
- Fix(osc): fix get cloud main account id throw exception when
  environment is not cloud (#529) [krihy]
- Fix(data-security): exist sensitive is not filtered and view
  desensitization data failed (#509) [XiaoYang]
- Fix(unit-test): unit test logs expose sensitive information (#498)
  (#516) [XiaoYang]
- Fix(view): fix get view failed without show view permission (#507)
  [zhangxiao]
- Fix: masking failed (#485) [XiaoYang]
- Fix(osc): execute pre and post interceptor in retry rename table
  (#486) [krihy]
- Fix(unit-test): fix failed unit test cases (#476) [XiaoYang, yh263208]
- Fix(data-security): error metadata of built-in sensitive algorithm
  (#458) [XiaoYang]
- Fix: database change failed (#455) [XiaoYang]
- Fix: scan sensitive columns (#444) [XiaoYang]
- Fix(mvc): api response content type converts to xml (#377) [XiaoYang]
- Fix: extract column from SQL with multiple join clauses (#327)
  [XiaoYang]


v4.2.1 (2023-10-09)
-------------------

New
~~~
- Feat(db-browser): upgrade db-browser's version to 1.0.2 (#402) [IL
  MARE]
- Feat(data-transfer): support saving export objects  (#73) [LuckyLeo]
- Feat(workflow): add checkbox for installing db-browser and ob-sql-
  parser (#75) [IL MARE]

Changes
~~~~~~~
- Refactor(submodule): update submodule (#436) [IL MARE]
- Refactor(migration): extract data migration interface (#290) [pynzzZ]
- Refactor(migrates): add some abstract methods for migrates (#275) [IL
  MARE]
- Refactor(migrate): speed up resource migration and add transaction
  control (#243) [IL MARE]
- Refactor(unit-test): refact unit test cases (#139) [IL MARE]
- Refactor(osc): refactor rename table (#65) [yaobin-khb]
- Refactor(osc): schedule task improve stable  (#62) [yaobin-khb]
- Refactor(workflow): add mvn install step for dev (#92) [IL MARE]
- Refactor(workflow): add mvn install step for dev (#91) [IL MARE]
- Refactor(workflow): merge from main to dev/4.2.x (#74) [IL MARE,
  gaoda.xy, guowl3, zhangxiao]

Fix
~~~
- Fix(dlm): validate condition by sql explain. (#440) [guowl3]
- Fix(datasource): optimize datasource synchronization (#391) [pynzzZ]
- Fix(osc): osc support ob ce add type  ob mysql ce (#390) [krihy]
- Fix: masking enabled (#383) [XiaoYang]
- Fix(clientMode): fail to start for lack of Service annotations (#371)
  [LuckyLeo]
- Fix(security): risky URLs discovered by security scans (#369)
  [XiaoYang]
- Fix(clientMode): odc fail to start on clientMode (#345) [LuckyLeo]
- Fix(osc): fix input sql check unsupported foreign key and different
  column (#364) [krihy]
- Fix(sql-execute): fix failed to print dbms output (#361) [IL MARE]
- Fix(connection):adapter result has been overwritten. (#340) [guowl3]
- Fix(data-transfer): failed to update data-transfer task status during
  running. [LuckyLeo]
- Fix(recyclebin): fix failed to generate flashback sql (#303) [IL MARE]
- Fix(audit): fix audit meta event for desktop  (#289) [krihy]
- Fix(pldebug): fix debug obtain connection info from direct connection
  config (#287) [yaobin]
- Fix(pldebug): fix debugger create new connection attach debuggee
  sessionId failed (#254) [yaobin]
- Fix(connection): check database type when test connection. (#232)
  [guowl3]
- Fix(data-security): create sensitive columns with case insensitive
  same column and table names (#175) [XiaoYang]
- Fix(pl-debug): Get connection failed when debug anonymous blocks in
  lowcase schema name (#198) [XiaoYang]
- Fix(security): http request during integration may receive SSRF attack
  (#172) [XiaoYang]
- Fix(flow): flow's status is illegal when failed to submit a task
  (#134) [IL MARE]
- Fix(osc): fix old running task throw npe when enable full verify
  (#173) [yaobin]
- Fix(pl-debug): add exception prompt when debugging errors (#168) [IL
  MARE]
- Fix(osc): fix create osc task ddl contains unique key but oms precheck
  table  not found (#165) [yaobin]
- Fix(integration): uncatched exception when failed to get flow instance
  (#156) [XiaoYang]
- Fix(osc): fix duplicate foreign key constraint name when execute new
  table create ddl (#135) [yaobin]
- Fix(db-session): fix can not get latest query sql when list all
  sessions (#133) [IL MARE]
- Fix(integration): external approval is always created when initiating
  a ticket (#140) [XiaoYang]
- Fix(osc): fix sql of alter replace table name not correct (#130)
  [yaobin]
- Fix(sql-check): fix can not detect table&column comment does not exist
  (#113) [IL MARE]
- Fix(osc): fix oracle rename table failed (#117) [yaobin]
- Fix(workflow): Make pnpm run in hoisted mode (#103) [Xiao Kang]
- Fix(result-set): generate dml slowly when edit result-set (#78)
  [LuckyLeo]
- Fix(unit-test): read properties from .env and system environment
  variables (#89) [yaobin-khb]
- Fix(batch-import): NPE when template file contains blank rows or
  columns (#77) [gaoda.xy]


