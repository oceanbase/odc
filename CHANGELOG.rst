(unreleased)
------------

New
~~~
- Feat(data-transfer): support saving export objects  (#73) [LuckyLeo]
- Feat(workflow): add checkbox for installing db-browser and ob-sql-
  parser (#75) [IL MARE]

Changes
~~~~~~~
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


