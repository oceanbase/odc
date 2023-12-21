(unreleased)
------------

New
~~~
- Feat(monitor):add api alarm (#1212) [Ang]
- Feat(datatransfer): support masking data for mysql datatransfer
  (#1198) [LuckyLeo]
- Feat(datasource): show datasource's connect status in team space's SQL
  console (#1224) [pynzzZ]
- Feat(partition-plan): support setting scheduling strategy (#1136)
  [guowl3]
- Feat(data-masking): prohibit data-masking for native MySQL datasource
  (#1095) [XiaoYang]
- Feat(data-transfer): support log throughput of datatransfer (#1056)
  [LuckyLeo]
- Feat(sql-execute): supports locating specific issue locations in
  multiple sqls during sql interception stage and pre-check stage (#976)
  [IL MARE, pynzzZ]
- Feat(sql-execute): unable to obtain locale info in subthread (#994)
  [IL MARE, LuckyLeo]
- Feat(data-transfer): support transfer mysql data by DataX (#871) [IL
  MARE, LuckyLeo]
- Feat(osc): lock user is not required when create osc task on ob (#970)
  [IL MARE, krihy]
- Feat(result-set-export): use task-plugin.datatransfer to export result
  set (#919) [IL MARE, LuckyLeo]
- Feat(datatype): support gis datatype for mysql and ob mysql (#898) [IL
  MARE, zhangxiao]
- Feat(sql-execution): let sql be only parsed once during execution
  (#858) [IL MARE]
- Feat(bastion): adapt bastion integration and datasource bind project
  (#847) [XiaoYang]
- Feat(session): make connect session auto-reconnect when session is
  deleted or disabled (#844) [IL MARE]
- Feat(obclient): upgrade obclient to 2.2.4 (#861) [LuckyLeo]
- Feat(project): add two built-in project roles (#755) [pynzzZ]
- Feat(data-transfer): add task-plugin-mysql for data-transfer (#833)
  [LuckyLeo]
- Feat(auth): add system config for max attempt times and account lock
  time (#795) [IL MARE]
- Feat(security-control): safety regulation adapt to ODP sharding MySQL
  (#780) [zhangxiao]
- Feat(osc): support swap table manual after full transfer and full
  verify completed (#736) [krihy]
- Feat(data-transfer): implement task-plugin-ob-mysql by ob-loader-
  dumper (#680) [LuckyLeo]
- Feat(osc): reactor api get database about lock user required (#726)
  [krihy]
- Feat(sql-splitter): support SqlCommentProcessor to split sql by stream
  (#661) [LuckyLeo]
- Feat(osc): odc user can assign lock db user when create osc task
  (#539) [krihy]
- Feat(permission): support apply for project permission (#515)
  [XiaoYang]
- Feat:(osc): monitor user lock status and relational sessions (#489)
  [krihy]
- Feat(unit-test): use cloud database as test cluster and adapt for
  github action (#411) [XiaoYang]

Changes
~~~~~~~
- Refactor(data-transfer): add task-plugin and
  DataTransferExtensionPoint (#625) [LuckyLeo]
- Refactor(unit-test): use blowfish encryption algorithm to replace aes
  (#443) [XiaoYang]

Fix
~~~
- Fix(ticket): project "pending approval" tickets shows other project's
  "pending approval" tickets (#1260) [pynzzZ]
- Fix(monitor): format druid log (#1251) [Ang]
- Fix(schema-plugin): show partition name with identifiers (#1249)
  [zhangxiao]
- Fix(partition plan):failed to disable table partition plan (#1247)
  [guowl3]
- Fix(datasource): it occurs 'duplicate data source name' error when
  creating a data source in team space (#1243) [pynzzZ]
- Fix(rollback-plan): NPE when user input sql content is empty (#1242)
  [XiaoYang]
- Fix(web-framework): invalid csrf token result into Invalid session
  error message (#1233) [yizhou]
- Fix(apply-project): failed to set mdc value (#1237) [XiaoYang]
- Fix(flow): creating flow costs too much time (#1183) [IL MARE,
  ungreat]
- Fix(osc): lock ob mysql user failed when host with ip limited (#1072)
  [krihy]
- Fix(audit): several operating records issues after ODC V4.2.0 (#1222)
  [pynzzZ]
- Fix(database-object):modify the prompt that prompts users about the
  risk of index changes #1228. [zhangxiao]
- Fix(database-change): timeout or oom when upload a large sql files
  (#1151) [XiaoYang]
- Fix(monitor): fix druid stats parser error (#1213) [Ang]
- Fix(sql-rule): disabling the rule 'allow-execute-sql-types'  does not
  work (#1194) [pynzzZ]
- Fix(database): optimize error message of synchronizing databases
  failure (#1202) [pynzzZ]
- Fix(monitor): druid stats use mysql parser (#1208) [Ang]
- Fix(concurrent): remove servlet configuration (#1188) [LuckyLeo]
- Fix(osc): osc log is not show totally and  flow task is done
  unnormally (#1110) [krihy]
- Fix(iam): users need re-login to access the individual space after
  they are granted for individual_space (#1147) [pynzzZ]
- Fix(sql-rule): several sql interception bugs (#1165) [pynzzZ]
- Fix(data-transfer): only inject sys tenant config when it's configured
  in datasource (#1172) [LuckyLeo]
- Fix(sql-check): can not give violations related comments normally when
  there exists same name tables (#1163) [IL MARE]
- Fix(project): transaction timeout when transfer too many databases or
  add too many users into projects (#1071) [pynzzZ]
- Fix(session): session creation will fail when the oracle schema name
  is lowercase (#1135) [pynzzZ]
- Fix(dlm): displays incomplete information after editing (#1073)
  [guowl3]
- Fix(database-object) :Provide relevant prompts to users for high-risk
  operations when drop or create index (#1143) [zhangxiao]
- Fix(project): could delete users who are currently joining projects
  (#1061) [pynzzZ]
- Fix(jdbc): full link trace leads to OOM exception (#1145) [LuckyLeo]
- Fix(result-set-export): there is no data in exported xlsx file (#1139)
  [LuckyLeo]
- Fix(ticket): approvers viewing shadow table sync ticket fails after
  the ticket is approved/rejected (#1119) [pynzzZ]
- Fix(connect-plugin): failed to connect to native percona mysql
  datasource when there is "-" in version string (#1115) [zhangxiao]
- Fix(ticket): tickets not filtered by projects (#1111) [pynzzZ]
- Fix(ticket): list all tickets returns empty in individual space
  (#1089) [pynzzZ]
- Fix(project): the project owner can remove all project dbas from the
  project (#1114) [pynzzZ]
- Fix(obclient): do not create os user when it already exists (#1096)
  [LuckyLeo]
- Fix(full-link-trace): no tags and references in downloaded json file
  (#1102) [LuckyLeo]
- Fix(project): project participants can create database and add them
  into the project (#1098) [pynzzZ]
- Fix(sql-rule): cannot execute sqls with dblink in team space's sql
  console (#1083) [pynzzZ]
- Fix(result-export): there is no log printed for result export task
  (#1081) [LuckyLeo]
- Fix(sql-rule): the sql type 'desc' does not work in the allow-sql-
  types rule (#1079) [pynzzZ]
- Fix(pl): no sys_refcursor shown in return type select panel when
  creating function (#1078) [IL MARE]
- Fix(parse-sid): optimize parse sid failed error message (#1062)
  [zhangxiao]
- Fix(datasource): built-in database still belong to previous project
  when datasource unbind project (#1059) [XiaoYang]
- Fix(dlm):task cannot be executed due to insufficient connections
  (#1052) [guowl3]
- Fix(partition-plan): create partition plan task failed in obmysql 1479
  (#1053) [pynzzZ]
- Fix(sql-check): failed to recognize several drop statements (#1026)
  [IL MARE]
- Fix(sql-rules): cannot add/update any sql rule default values (#1014)
  [pynzzZ]
- Fix(database): block built-in databases when auto-sync databases to
  project (#968) [XiaoYang]
- Fix(integration): cannot deal with array when parsing json or xml
  response (#1039) [XiaoYang]
- Fix(permission): user can create datasource without any project and
  role (#1019) [XiaoYang]
- Fix(data-security): create sensitive columns failed due to scanning
  duplicated columns (#1021) [XiaoYang]
- Fix(ticket): horizontal unauthorized when query approver related role
  names (#1011) [IL MARE, XiaoYang]
- Fix(apply-project): project role names are not internationalized
  (#1000) [XiaoYang]
- Fix(db-browser): DB session list show 0 in execute time for ob mysql
  and mysql mode (#1001) [IL MARE, zhangxiao]
- Fix(bastion): inactive datasources are not cleared (#997) [XiaoYang]
- Fix(variables): variable updating may lead to sql injection  (#1008)
  [IL MARE]
- Fix(data-security): test masking algorithm may lead to security issue
  (#987) [XiaoYang]
- Fix(connection):add back connection cluster name (#942) [Ang]
- Fix(connect-plugin): move JdbcUrlParser to connect plugin (#914) [IL
  MARE]
- Fix(PL): PL params of MySQL mode are not escaped (#904) [IL MARE,
  LuckyLeo]
- Fix(data-transfer): set page size to avoid ob-dumper splitting files
  (#906) [LuckyLeo]
- Fix(obclient): fix unzip exceptions and symbolic link failed (#891)
  [LuckyLeo, yh263208]
- Fix(sql-execute): move internal rowid to after last select item when
  rewriting sql (#888) [LuckyLeo]
- Fix(snippet): snippet body's size is too long to insert into metadb
  (#887) [IL MARE]
- Fix(config): modify bad system configuration (#875) [XiaoYang]
- Fix(security): upgrade the version of some modules to avoid security
  problems (#872) [IL MARE]
- Fix(trace): remove RESPONSE_TIME from MDC (#866) [Ang]
- Fix(database-change): OOM may occur when executing database change
  task with large SQL files (#864) [XiaoYang]
- Fix(security): MySQL JDBC arbitrary file reading vulnerability (#856)
  [zhangxiao]
- Fix(db-browser): add "SYS" prefix for oracle dictionary views (#846)
  [zhangxiao]
- Fix(data-editing): optimize error message when the length of field
  exceeds the maximum limit (#845) [zhangxiao]
- Fix(schema-plugin): cannot get table detail in odp sharding mysql mode
  when lower_case_table_names = 1 or 2 (#814) [zhangxiao]
- Fix(recyclebin): fix can not delete recyclebin objects (#783) [IL
  MARE]
- Fix(result-set): only allow to edit result set when there is pk / uk /
  rowid (#781) [LuckyLeo]
- Fix(SSO): test login protocal not match (#766) [Ang]
- Fix(apply-project): Resource role name in DTO is modified but be
  trusted (#760) [XiaoYang]
- Fix(unit-test): unsafe log output and unstable test case (#549)
  [XiaoYang]
- Fix(osc): fix get cloud main account id throw exception when
  environment is not cloud (#530) [krihy]
- Fix(unit-test): unit test logs expose sensitive information (#498)
  [XiaoYang]
- Fix(data-masking): add unit test case for select sql syntax (#398)
  [XiaoYang]


v4.2.2_bp1 (2023-11-24)
-----------------------

New
~~~
- Feat(notification): support send notification when schedule job failed
  (#711) [LuckyLeo]
- Feat(jdbc): upgrade jdbc to 2.4.7.1 (#761) [LuckyLeo]
- Feat(ob-sql-parser): supports insert statement and add timeout
  settings (#754) [IL MARE]

Fix
~~~
- Fix(pl): failed to execute a pl that contains an out sys_refcursor
  parameter (#911) [IL MARE]
- Fix(dlm):data delete failure in periodic task and remove sys tenant
  verification (#857) [guowl3]
- Fix(ob-sql-parser): ob-sql-parser's timeout setting may overflow
  (#882) [IL MARE]
- Fix(sql-execution): avoid adding rowid when dblink exists (#881) [IL
  MARE]
- Fix(migrate): failed to start up when there is no users or
  organizations (#860) [IL MARE]
- Fix(dlm):update limiter failed after data-delete job was created.
  (#840) [guowl3]
- Fix(data-security): regex column recognization rule may suffer ReDos 2
  (#848) [XiaoYang]
- Fix(data-security): regex column recognization rule may suffer ReDos
  (#843) [XiaoYang]
- Fix(dlm): data delete failed after data archived. (#735) [guowl3]
- Fix(name): change resource name length limit from 64 to 128 (#839)
  [XiaoYang]
- Fix(security): add white list for security scanning and modify mysql
  pl parser's g4 (#837) [IL MARE]
- Fix(sql-execute): fix number data display error format (#764) [IL
  MARE]
- Fix(pldebug): pldebug monitor does not exit block process exiting
  (#765) [yizhou]
- Fix(pl): fix column name is wrong when viewing cursor's content (#757)
  [IL MARE]
- Fix(sql-check): avoid reporting syntax error when sql is executed
  successfully (#748) [IL MARE]
- Fix(web): response header content-type would be application/xml while
  using RestTemplate (#722) [pynzzZ]

Security
~~~~~~~~
- Security: fix mysql jdbc deserialization security vulnerability (#912)
  [IL MARE, zhangxiao]
- Security: MySQL JDBC arbitrary file reading vulnerability (#885)
  [zhangxiao]


v4.2.2 (2023-11-07)
-------------------

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
- Fix(dlm):wrong order status when task is rollback (#707) [guowl3]
- Fix(sql-execute): no trace id when sql executing failed (#700)
  [LuckyLeo]
- Fix(SSO):saved SSO intergration test login failed (#698) [Ang]
- Fix(SSO):saved SSO intergration test login failed (#698) [Ang]
- Fix(SSO):saved SSO intergration test login failed (#698) [Ang]
- Fix(sql-parser): failed to report syntax error if the input's any
  prefix is grammatical (#699) [IL MARE]
- Fix(datasource): make 'socketTimeout' and 'connectTimeout' settings
  work for backend datasource (#691) [IL MARE]
- Fix(result-set-export): get wrong filename for result-set export task
  on cloud (#685) [LuckyLeo]
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


