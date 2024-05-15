(unreleased)
------------

New
~~~
- Feat(dlm): upgrade dlm sdk to 1.1.1 (#2281) [guowl3]
- Feat(connect): supports connect backup instance (#2192) [pynzzZ]

Changes
~~~~~~~
- Refactor(osc): modify i18n messages for white list (#2221) [krihy]

Fix
~~~
- Fix(shadowtable): bad sql grammer when table column's default value is
  a string type in MySQL (#2388) [jingtian, pynzzZ]
- Fix(data-masking): unavailable when existing invalid database with
  duplicated name (#2385) [XiaoYang]
- Fix(obclient): remove unexpected characters to avoid obclient display
  incorrectly (#2353) [LuckyLeo]
- Fix(iam): failed to delete a user who is an archived project's Owner
  or DBA (#2359) [yiminpeng]
- Fix(account-integration): failed to login after updating user extra
  properties (#2336) [XiaoYang]
- Fix(encryption): exception occurs when invoking RSA decryption
  concurrently (#2371) [XiaoYang]
- Fix(web-framework): csrf prevention conflict with basic authentication
  (#2370) [yizhou]
- Fix(odp-sharding): cannot connect to odp sharding (#2339) [yizhou]
- Fix(web-framework): csrfToken API does not return token (#2277)
  [yizhou]
- Fix(full-link-trace): enable trace by default on OB 4.x (#2347)
  [LuckyLeo]
- Fix(flow): task will be failed when its execution undergoes a version
  upgrade #2342. [IL MARE]
- Fix(taskframework): oss log info is ignored when cancel job (#2341)
  [krihy]
- Fix(schedule): status not found (#2333) [guowl3]
- Fix(dlm): the task status was set to completed when the task was
  canceled (#2340) [guowl3]
- Fix(connection): add serialVersionUID for ConnectionConfig (#2065)
  [Xiao Kang, yh263208, zijia.cj]
- Fix(partition-plan): generated partition expression is not contiguous
  (#2327) [IL MARE]
- Fix(taskframework): always print warn log about "Task-framework
  isEnabled" (#2331) [krihy]
- Fix(dlm): correct the task status when the task exits abnormally
  (#2330) [guowl3]
- Fix(osc): osc job is terminated when database id is not exist (#2320)
  [krihy]
- Fix(partition-plan): failed to list partitioned tables on OceanBase
  1.4.79 (#2323) [IL MARE]
- Fix(connection): connection may be blocked (#2307) [IL MARE]
- Fix(result-set-export): use console datasource to avoid socket timeout
  (#2315) [LuckyLeo]
- Fix(notification): lost sign when updating channel (#2314) [LuckyLeo]
- Fix(alarm): alarm msg add request (#2319) [Ang]
- Fix(dlm): the target table does not exist (#2313) [guowl3]
- Fix(taskframework): optimize log content (#2299) [krihy]
- Fix(dlm):get table ddl failed in oracle mode #2296. [guowl3]
- Fix(taskframework): column 'job_id' cannot be null (#2292) [krihy]
- Fix(objectstorage): wrong log/database change/rollback plan download
  URLs if cloud storage is enabled (#2289) [pynzzZ]
- Fix(ticket): access denied when approve a ticket (#2288) [XiaoYang]
- Fix(partition-plan): failed to start up a partition plan on oceanbase
  before 3.x #2287. [IL MARE]
- Fix(taskframework): running task log is not found when close task-
  framework && send mistake alarm (#2268) [krihy]
- Fix(dlm): rollback task failed when customizing target table names
  (#2279) [guowl3]
- Fix(flow): cancel pending task cause "Some tasks is not found" (#2272)
  [krihy]
- Fix(dlm): get log failed when the task framework was switched during
  task execution (#2276) [guowl3]
- Fix(notification): use blacklist to check webhook instead of whitelist
  (#2267) [LuckyLeo]
- Fix(dlm): custom target table names invalid in non-task framework mode
  (#2269) [guowl3]
- Fix(taskframework): optimize flowable transaction manager (#2255)
  [krihy]
- Fix(notification): failed to query connection info (#2249) [LuckyLeo]
- Fix(bastion): could not access SQL console when using OAuth for
  account integration (#2253) [XiaoYang]
- Fix(taskframework): add pod config from properties (#2250) [krihy]
- Fix(taskframework): alarm notification is not effective (#2242)
  [krihy]
- Fix(permission): permission error message is not readable (#2227) [IL
  MARE]
- Fix(tag): change default tag key (#2225) [Ang]
- Fix(security): horizontal privilege escalation issue of
  getOscDatabaseInfo interface (#2209) [krihy]
- Fix(sql-execute): disable full link trace on OB 4.1.x (#2219)
  [LuckyLeo]
- Fix(taskframework): pending pod timeout after exceed one hour (#2187)
  [krihy]
- Fix(tag): can‘t find tagServiceFacade (#2217) [Ang]
- Fix(connection): add version check before set weak read consistency
  (#2214) [pynzzZ]

Security
~~~~~~~~
- Security: exclude dependency on snappy-java (#2317) [LuckyLeo]
- Security: test masking using rounding algorithm may cause denial of
  service (DoS) risk (##) [XiaoYang]


v4.2.4_bp1 (2024-04-12)
-----------------------

New
~~~
- Feat(connection): supports query connections by username (#1981)
  [zhangxiao]

Fix
~~~
- Fix(configuration): add Configuration Consumer for UserConfigService
  (#2198) [zhangxiao]
- Fix(security): upgrade  okio-jvm to 3.4.0 (#2200) [krihy]
- Fix(connection): add some log about datasource lock (#2196) [Ang]
- Fix(security):horizontal privilege escalation issue of getDatabase
  interface (#2194) [zhangxiao]
- Fix(notification): failure to enqueue event will result in failure of
  ticket rejection (#2185) [LuckyLeo]
- Fix(db-browser): failed to listTables when lack mysql schema
  permissions (#2184) [zhangxiao]
- Fix(data-transfer): obloader fail to load MANIFEST.bin (#2181)
  [LuckyLeo]
- Fix(pl-debug): make timeout settings can be overwritten by session
  init script (#2179) [IL MARE]
- Fix(taskframework):  calculate capacity of task in  single node by
  node memory (#2174) [krihy]
- Fix(taskframework): dlm task show log not exists (#2169) [krihy]
- Fix(partition-plan): failed to migrate historical partition plan data
  (#2158) [IL MARE]
- Fix(taskframework): load log configuration NPE in task pod (#2153)
  [krihy]


v4.2.4 (2024-04-03)
-------------------

New
~~~
- Feat(tag): add tag service (#2090) [Ang]
- Feat(dlm): adapts to the task framework and supports OceanBase Oracle
  mode (#2059) [guowl3]
- Feat(taskframework): supports external log4j configuration file
  (#2080) [krihy]
- Feat(partition-plan): make partition name generation based on a
  partition key (#2013) [IL MARE]
- Feat(authentication): use jwt instead of jsession for authentication
  (#1663) [jonas, yh263208]
- Feat(partition-plan): migrate historical partition plan's data and
  remove useless code (#1868) [IL MARE]
- Feat(builtin-snippet): more snippets for  developer (#1934) [yizhou]
- Feat(database-change): supports retry execute in database change task
  (#1863) [LuckyLeo]
- Feat(monitor): add api rt alarm (#1877) [Ang]
- Feat(stateful): add stateful route (#1608) [Ang]
- Feat(partition-plan): returns partition table configs when list
  candidate tables (#1783) [IL MARE]
- Feat(task): task executing strip from flowable (#1706) [krihy]
- Feat(sql-check): add offline ddl detecting, key words detecting and
  truncate statement detecting rules (#1766) [IL MARE]
- Feat(partition-plan): disable the previous flow instance when a
  partition plan is related to an exists flow (#1750) [IL MARE]
- Feat(risk-detect): add default risk detect rules (#1664) [pynzzZ]
- Feat(taskframework): support modify job parameters (#1612) [krihy]
- Feat(partition-plan): add controller implementation (#1590) [IL MARE]
- Feat(snippet): more builtin snippets (#1682) [yizhou]
- Feat(snippet): initial builtin snippet framework (#1662) [yh263208,
  yizhou]
- Feat: make odc adapt to OceanBase 4.2.2 (#1660) [IL MARE]
- Feat(database-change): adaption for oracle11g (#1617) [zhangxiao]
- Feat(osc): supports add column in new ddl (#1611) [krihy]
- Feat(notification): supports scheduling failed and task terminated
  (#1647) [LuckyLeo]
- Feat(dlm): data clean adapt to task framework (#1643) [MarkPotato777,
  guowl3]
- Feat(config): new user configuration api for settings center (#1609)
  [yizhou]
- Feat(dlm): supports review sql (#1606) [guowl3]
- Feat(databaseChange): automatically adjust the timeout if the database
  change task involves time-consuming index change operations (#1578)
  [zhangxiao]
- Feat(data-transfer): add task-plugin-oracle and datatransfer module
  (#1500) [LuckyLeo]
- Feat(notification): supports custom http request for wehbook channel
  (#1604) [LuckyLeo]
- Feat(datasource): session management adapt for oracle11g (#1520)
  [zhangxiao]
- Feat(rollback-plan): adapt to task framework (#1569) [XiaoYang]
- Feat(data-masking): adapt to task framework (#1535) [XiaoYang]
- Feat(structure-comparison): integrate structural comparison into the
  service layer (#1473) [zhangxiao]
- Feat(datasource): support new datasource of doris (#1494) [IL MARE,
  isadba]
- Feat(environment): supports custom environment (#1381) [pynzzZ]
- Feat(datasource): connection module supports oracle11g (#806)
  [zhangxiao]
- Feat(partition-plan): add implementation for oracle mode (#1485) [IL
  MARE]
- Feat(notification): add channel implement and refactor message broker
  (#1451) [LuckyLeo]
- Feat(partition-plan): add implementation for mysql mode (#1456) [IL
  MARE]
- Feat(data-transfer): upgrade ob-loader-dumper to 4.2.8-RELEASE (#1427)
  [LuckyLeo, LuckyPickleZZ]
- Feat(data-transfer): not compress export output in client mode (#1459)
  [LuckyLeo]
- Feat(database-permission): service implementation (#1392) [XiaoYang]
- Feat(partition-plan): add service implementation and plugin api
  (#1430) [IL MARE]
- Feat(deployment): add datetime output for ob-odc-web.std.log (#1420)
  [yizhou]
- Feat(db-browser): structural comparison task adaptation on ob-oracle
  mode in db-browser layer (#1395) [zhangxiao]
- Feat(notification): add controller and service layer for notification
  (#1377) [LuckyLeo]
- Feat(sso): supports ldap (#1349) [Ang, ungreat]
- Feat(partition-plan): add api and storage layer (#1374) [IL MARE]
- Feat(db-browser): make db-browser adapt to oracle11g (#503)
  [zhangxiao]
- Feat(database-permission): add apply database permission ticket
  (#1372) [XiaoYang]
- Feat(structure-compare): structural comparison task interface layer
  code implementation (#1342) [zhangxiao]
- Feat(notification): add migration script and i18n properties for
  notification metadata (#1344) [LuckyLeo]
- Feat(database-permission): adapt permission framework (#1366)
  [XiaoYang]
- Feat(structure-comapre): implement database object structure
  comparison (#1364) [zhangxiao]
- Feat(database-permission): add API definition (#1358) [XiaoYang]
- Feat(version): upgrade the version of odc from 4.2.3 to 4.2.4 (#1361)
  [IL MARE]
- Feat(dlm): adapt to log service (#1538) [guowl3]
- Feat(dlm): adapt to task framework (#1529) [guowl3, krihy]
- Feat(taskframework): control schedule job rate by executor running
  status (#1513) [krihy]

Changes
~~~~~~~
- Refactor(taskframework): refactor job rate limiter for extension
  (#2112) [krihy]
- Refactor(notification): extract siteUrl cacluation logic into util
  (#2021) [LuckyLeo]
- Refactor(taskframework): add LoggerService for DLM query log (#1534)
  [krihy]

Fix
~~~
- Fix(log): correct the log routing path #2148. [guowl3]
- Fix(environment): wrong default environment description (#2146)
  [pynzzZ]
- Fix(taskframework): fix refresh entity replaced by native sql (#2145)
  [krihy]
- Fix(taskframework): refresh entity from database after update destroy
  time (#2141) [krihy]
- Fix(partition-plan): migrate useless partition plan table config
  (#2142) [IL MARE]
- Fix(stateful): default disabled stateful route (#2138) [Ang]
- Fix(alter-schedule): correct the log routing path #2137. [guowl3]
- Fix(dlm):interrupt job failed (#2135) [guowl3]
- Fix(taskframework): fix decrypt meta db password (#2129) [krihy]
- Fix(session): failed to remove session when session is expired (#2125)
  [IL MARE]
- Fix(taskframework): handle all preparing and running task to failed
  when task-framework be set disabled (#2119) [krihy]
- Fix(taskframework): add alarm in taskframework (#2098) [krihy]
- Fix(audit): add audit type and event for structure comparing task
  (#2117) [pynzzZ]
- Fix(structure-comapre): failed to update task status when task fails
  (#2116) [zhangxiao]
- Fix(taskframework): ip change with docker restart cause jobs process
  interrupted and status cannot be terminated (#2030) [krihy]
- Fix(data-masking): cannot masking result set when using Oracle (#2114)
  [XiaoYang]
- Fix(sso): userAccountName allow null string (#2113) [Ang]
- Fix(data-transfer): support data transfer for doris (#2106) [LuckyLeo]
- Fix(connect): failed to sync standby cluster's databases (#2107) [IL
  MARE]
- Fix(doris): failed to view active db sessions in doris (#2104) [IL
  MARE]
- Fix(security): add authorize annotation in service class (#2103)
  [LuckyLeo]
- Fix(shadowtable): it occurs NPE while creating shadow table comparing
  tasks (#2087) [pynzzZ]
- Fix(partition-plan): failed to execute partition ddl when schema or
  table name is in lower case (#2088) [IL MARE]
- Fix(partition-plan): failed to view partition plan tables when
  partition is not active (#2099) [IL MARE]
- Fix(flow): flow task service node complete time is not correct (#2052)
  [krihy]
- Fix(db-browser): failed to view detail of the table in doris (#2081)
  [jonas]
- Fix(sql-execute): commit/rollback button does not sync with trans
  state for oceanbase 4.2 (#2097) [yizhou]
- Fix(security): add horizontal authentication for service (#2064)
  [guowl3]
- Fix(sql-execute): cannot execute SQLs with single-line comments
  (#2085) [pynzzZ]
- Fix(builtin-snippets): duplicated prefix and wrong column for ob 3.x
  (#2077) [yizhou]
- Fix(database): failed to create doris database when input charset and
  collation (#2076) [XiaoYang]
- Fix(unit-test): load test DB connect session on demand (#2073)
  [XiaoYang]
- Fix(result-set-export): fix export result-set for oracle failed
  (#2033) [LuckyLeo]
- Fix(partition-plan): wrap the partition identifier when drop ddl
  generated (#2068) [IL MARE]
- Fix(db-browser): create table statement containing index is not
  recognized as a create type (#2063) [jonas]
- Fix(sql-check): fix npe caused by offline statement detection (#2057)
  [IL MARE]
- Fix(task): try to upload log file even through the task has no log
  file (#2051) [XiaoYang]
- Fix(db-browser): remove spaces before and after ddl in oracle mode
  (#2050) [zhangxiao]
- Fix(connection): modify DruidDataSource MaxWait timeout (#2011)
  [zhangxiao]
- Fix(schema-plugin): failed to view table's detail on sofa odp (#2043)
  [IL MARE]
- Fix(taskframework): cannot preview latest log for dlm (#2024) [krihy]
- Fix(sql-check): avoid npe when virtual column exists (#2031) [IL MARE]
- Fix(sql-execute): add an user config and add concurrent control for
  session creating (#2020) [IL MARE]
- Fix(sso):ldap mapper can't load on not local type (#1988) [Ang]
- Fix(resultset-edit): bad performance when modifing query result set
  (#2007) [XiaoYang]
- Fix(notification): fix some security vulnerabilities (#2001)
  [LuckyLeo]
- Fix(taskframework): limit running job count by calculate free memory
  when StartJobRateLimiter starting (#1932) [krihy]
- Fix(taskframework): fix cancel result when status is done   (#2002)
  [krihy]
- Fix(structure-compare): failed to run structure compare task without
  update connection permission (#2006) [zhangxiao]
- Fix(sql): precision is lost when displaying timestamps (#1996) [jonas]
- Fix(partition-plan): failed to generate partition correctly when
  partition upper bound is not increased by 1 year/month/day (#1992) [IL
  MARE]
- Fix(encryption): add log output when failed to decrypt #1994.
  [XiaoYang]
- Fix(shadowtable): NPE occurs when project admins detail the
  shadowtable and structure comparing task (#1960) [pynzzZ]
- Fix(result-set): failed export oracle result set (#1956) [LuckyLeo]
- Fix(connect): can not access Doris datasource (#1990) [XiaoYang]
- Fix(flowTask): fix failed to get flow task results (#1985) [zhangxiao]
- Fix(taskframework): update schedule task status when cancel completed
  or heart check timeout (#1973) [krihy]
- Fix(osc): osc will be failed if check oms step accumulate failed time
  bigger than threshold (#1613) [krihy]
- Fix(flow): wrong total elements and total pages of flow instances
  while querying in page (#1947) [pynzzZ]
- Fix(connection): SingleConnectionDataSource concurrent getConnection
  may have  problems (#1914) [Ang]
- Fix(ticket): cannot preview latest log and download complete log file
  (#1940) [XiaoYang]
- Fix(taskframework): fix cancel job and update executionTimes failed
  (#1961) [krihy]
- Fix(environment): modify the error message while disabling
  environments (#1959) [pynzzZ]
- Fix(sql-execute): the offset of the last sql goes wrong when it
  doesn't end with the delimiter (#1970) [pynzzZ]
- Fix(parser): failed to parse insert statement with a table named
  'json_table' (#1968) [IL MARE]
- Fix(builtin-snippets): fix wrong description for builtin snippets
  (#1969) [yizhou]
- Fix(taskframework): fix retry job reset destroy and heart time
  (#1952) [krihy]
- Fix(db-browser): the order of column names in Index and constraint are
  inconsistent (#1948) [zhangxiao]
- Fix(data-transfer): after data transfer is completed, the directory is
  not cleared (#1951) [LuckyLeo]
- Fix(stateful): stateful optional allow nullable (#1945) [Ang]
- Fix(partition-plan): the first partition value is incorrect when
  creating partitions sequentially based on the current time (#1804) [IL
  MARE]
- Fix(notification): NPE occurred when convert event to message (#1938)
  [LuckyLeo]
- Fix(database-permission): unable apply for database permission (#1896)
  [XiaoYang]
- Fix(sql-check): failed to detect pk dropping when drop it as
  constraint (#1879) [IL MARE]
- Fix(iam): failed to batch import user with roles (#1908) [LuckyLeo]
- Fix(stateful): stateful interceptor will be npe when clientMode
  (#1923) [Ang]
- Fix(taskframework): add free memory check before start new process
  avoid start process failed (#1883) [krihy]
- Fix(partition-plan): fix wrong api's path variable #1928. [IL MARE]
- Fix(taskframework): fix retry log attribute (#1904) [krihy]
- Fix(stateful): max pool size may less than core pool size and cause
  error (#1919) [Ang]
- Fix(database-management): unable to create a connection using database
  details (#1890) [XiaoYang]
- Fix(stateful): RouteHealthManager wrong conditional on property
  (#1880) [Ang]
- Fix(sql-check): npe will be thrown when some alter statements exists
  #1865. [IL MARE]
- Fix(taskframework): fix invalid CSRF token when task process report
  heart to sever (#1808) [krihy]
- Fix(taskframework): fix executor meta db config (#1870) [krihy]
- Fix(workflow): build front resource when only build client artifact
  (#1867) [XiaoYang]
- Fix(database-permission): return authorized permission types for
  detail database interface (#1843) [XiaoYang]
- Fix(multi-cloud): failed to create new connection with read only
  account (#1838) [zhangxiao]
- Fix(schedule): no permission to edit (#1847) [guowl3]
- Fix(database-change): read sql file failed causing sql not executed
  (#1807) [XiaoYang]
- Fix(data-transfer): oracle mode import with incorrect splitted sqls
  (#1832) [LuckyLeo]
- Fix(connect): sometimes open sql console result in 404 error (#1809)
  [IL MARE]
- Fix(environment): flow instance selects wrong approval flow config
  after updating risk detect rules (#1800) [pynzzZ]
- Fix(datasource): connecting failed in OceanBase Oracle after
  correcting the username (#1797) [pynzzZ]
- Fix(dlm): check database permission failed (#1799) [guowl3]
- Fix(environment): add an environment exists api (#1785) [pynzzZ]
- Fix(taskframework): deserialize log occur error from remote (#1795)
  [krihy]
- Fix(audit): add audit keys and i18n messages for some module #1772.
  [LuckyLeo]
- Fix(data-transfer): export oracle object ddl without line breaks
  (#1786) [LuckyLeo]
- Fix(dlm): interrupt job failed (#1778) [guowl3]
- Fix(alarm): add scheduling failed alarm (#1779) [Ang]
- Fix(partition-plan): failed to create a drop strategy only (#1774) [IL
  MARE]
- Fix(taskframework): fix task update final status when completed
  (#1768) [krihy]
- Fix(flow): create flow instance failed when environment absent (#1770)
  [XiaoYang]
- Fix(sql-execute): anonymous block execution module adapted to
  oracle11g (#1759) [zhangxiao]
- Fix(database-change): failed to check if time-consuming sql exists in
  personal space (#1720) [zhangxiao]
- Fix(dlm): database not found (#1721) [guowl3]
- Fix(taskframework):  limit remote ip as localhost to access task api
  when task-framework run in process mode (#1730) [krihy]
- Fix(notification): failed to send notification for partition plan
  (#1726) [LuckyLeo]
- Fix(partition-plan): npe will be thrown when input is invalid (#1748)
  [IL MARE]
- Fix(db-browser):failed to get table when column has default value in
  oracle11g mode (#1733) [zhangxiao]
- Fix(database-permission): permission intecept failed when switch
  schema or call PL (#1678) [XiaoYang]
- Fix(database-change): create database change task failed in private
  sapce (#1702) [XiaoYang]
- Fix(db-browser): failed to getTable in mysql5.6 (#1635) [zhangxiao]
- Fix(database-change): show wrong executing result status (#1637)
  [XiaoYang]
- Fix(taskframework): enable taskframework default (#1691) [krihy]
- Fix(dlm): job scheduler not found (#1690) [guowl3]
- Fix(structure-compare): alter table remove partitioning sql is not put
  in comment (#1674) [zhangxiao]
- Fix(notification): some event failed to be sent (#1629) [LuckyLeo]
- Fix(taskframework):  use default entry point to replace init cmd
  (#1601) [krihy]
- Fix(config): wrong reason while full link trace not enabled (#1644)
  [yizhou]
- Fix(security): share public and private key when ODC is deployed on
  multiple nodes (#1641) [zhangxiao]
- Fix(db-browser): listTables correctly returns the table under the
  specified schema (#1632) [zhangxiao]
- Fix(database-permission): delete related permission records when
  deleting data source (#1619) [XiaoYang]
- Fix(integration): garbled code exists when using Chinese in request
  body (#1625) [XiaoYang]
- Fix(structure-comparison): failed to create structure-comparison task
  in personal space (#1623) [zhangxiao]
- Fix(mock-data): failed to mock any data for ob-mysql mode (#1594) [IL
  MARE]
- Fix(database-permission): DB permission interceptor invalid when
  executing PL in the SQL console (#1592) [XiaoYang]
- Fix(db-browser): failed to list tables when ob's version is no greater
  than 2.2.30 (#1478) [zhangxiao]
- Fix(pre-check): load uploaded files failed (#1470) [XiaoYang]
- Fix(database-permission): user holds no db permission in personal
  space (#1467) [XiaoYang]
- Fix(database-permission): failed to verify database permission in
  personal space (#1458) [XiaoYang]
- Fix(flow): revert #1380 and #1402 from dev/4.2.x (#1454) [Ang]
- Fix(sso): frontend-backend integration testing (#1406) [Ang]
- Fix(data-security): data masking failed when using nesting case-when
  clause (#1410) [XiaoYang]
- Fix(sql-execution): precision loss when displaying datetime type
  (#1411) [IL MARE, jonas]
- Fix(flow): reduce the result set size of the flow Instance query by
  parent instance id (#1402) [Ang, ungreat]
- Fix(db-browser): fix the visualization of mysql table structure design
  and supports strings enclosed in single quotes (#1401) [IL MARE,
  isadba]
- Fix(flow):improve list API rt (#1383) [Ang, ungreat]
- Fix(migrate): rename notification migrate script (#1373) [LuckyLeo]
- Fix(dlm): create target table failed (#1614) [guowl3]
- Fix(cloud): add a CacheManager bean which allows null values #1610.
  [pynzzZ]
- Fix(osc): validate input ddl  throw syntax exception when contain
  comment (#1597) [krihy]
- Fix(cloud): tenant/serverless instance test connection failed in some
  specific scenery (#1602) [pynzzZ]
- Fix(osc): supports ob oracle 4.0 drop primary constraint when contain
  unique key (#1591) [krihy]
- Fix(osc): fix i18n hint when user started swap table (#1580) [krihy]
- Fix(osc): supports creating index sql in ob oracle  (#1560) [krihy]
- Fix(cloud): serverless instance adaption #1561. [pynzzZ]
- Fix(taskframework): k8s system config from data.sql is empty string
  (#1541) [krihy]


v4.2.3_bp1 (2024-02-01)
-----------------------

New
~~~
- Feat(pre-check): adapt to task framework (#1489) [XiaoYang]
- Feat(taskframework): add process run model for task running (#1447)
  [gaoda.xy, krihy]
- Feat(database-change): database change task adapt streaming read sql
  file (#1437) [XiaoYang]
- Feat(task-framework): merge from feat/424_taskframework into dev/4.2.3
  (#1365) [krihy]
- Feat(osc): add project list  adapter oms new api (#1318) [krihy]
- Feat(mock-data): add a logger for log printing (#1407) [IL MARE]
- Feat(dlm):upgrade SDK to 1.0.10 (#1396) [guowl3]
- Feat(dlm): supports sharding using unique indexes (#1327) [guowl3]

Changes
~~~~~~~
- Refactor(objectstorage): create publicEndpointCloudClient and
  internalEndpointCloudClient to distinguish uploading and generating
  presignedUrl circumstance (#1319) [pynzzZ]

Fix
~~~
- Fix(taskframework): start process failed when local odc server start
  by java -jar (#1492) [krihy]
- Fix(partition-plan):delete job failed if the associated trigger does
  not exist (#1495) [guowl3]
- Fix(table): query table data with no column comments (#1488)
  [LuckyLeo]
- Fix(sql-execute): fail to execute statement on OceanBase 2.2.30
  (#1487) [LuckyLeo]
- Fix(audit): executing sql with rare words failed when metadb's default
  character is gbk (#1486) [pynzzZ]
- Fix(flow): NPE when creating a ticket without connection information
  (#1479) [XiaoYang]
- Fix(sql-execute): executing anonymous block causes NPE in the team
  space (#1474) [pynzzZ]
- Fix(taskframework): lower k8s client version cause security problem
  (#1472) [krihy]
- Fix(sql-execute): do not follback execute when manual commit enabled
  (#1468) [LuckyLeo]
- Fix(data-transfer): fix wrong object type names were used (#1464)
  [LuckyLeo]
- Fix(data-transfer): do not create os user in client mode (#1465)
  [LuckyLeo]
- Fix(dlm): the data cleaning task scheduling failed after editing the
  rate limit configuration (#1438) [guowl3]
- Fix(flow): remove unnecessary query (#1429) [Ang]
- Fix(flow): can not set task status correctly when creating task
  concurrently (#1419) [IL MARE]
- Fix(sql-execution): can not set a delimiter longer than 2 (#1414) [IL
  MARE]
- Fix(osc): exists horizontal overstep access data permission when swap
  table manual (#1405) [krihy]
- Fix(mock-data): failed to upload file to oss (#1345) [IL MARE]
- Fix(osc): osc job query connection config by id throw Access Denied
  (#1378) [krihy]
- Fix(dlm): the task log file does not exist (#1376) [guowl3]
- Fix(osc): osc task don't show manual swap table name when full migrate
  is completed (#1357) [krihy]
- Fix(sql-check): failed to check statement when connect to a lower case
  schema  (#1341) [IL MARE]
- Fix(database-change): query task details throw flow instance not found
  exception (#1325) [XiaoYang]
- Fix(database-change): query task details throw file not found
  exception (#1316) [XiaoYang]
- Fix(object-storage): remove dependency on OssTaskReferManager (#1314)
  [LuckyLeo]

Security
~~~~~~~~
- Security: upgrade aliyun-oss-sdk version (#1393) [pynzzZ]


v4.2.3 (2023-12-26)
-------------------

New
~~~
- Feat(mock-data): increase the max number of the mock data to 100
  million (#1294) [IL MARE]
- Feat(dlm): upgrade dlm's version to 1.0.8 (#1299) [guowl3]
- Feat(dlm): supports viewing task logs (#1017) [guowl3]
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
- Fix(sql-rule): the rule 'allow-sql-types' of dev environment is
  disabled by default (#1302) [pynzzZ]
- Fix(sql-rule): adjust several sql-console rules' default values
  (#1281) [pynzzZ]
- Fix(result-set-export): failed to rewrite sql for mysql (#1288)
  [LuckyLeo]
- Fix(datasource): make ODP_SHARDING_OB_MYSQL not be converted to
  OB_MYSQL in some special cases (#1280) [zhangxiao]
- Fix(result-export): failed to export mysql data (#1275) [LuckyLeo]
- Fix(monitor): format alarm error stack to inline (#1273) [Ang]
- Fix(mock-data): failed to recognize the charset key of 'UTF8' (#1272)
  [IL MARE]
- Fix(mock-data): upgrade mock-data module's version to fix several bugs
  (#1227) [IL MARE]
- Fix(flow): close prepared stmt and resultset when batch creating end
  (#1266) [Ang, yh263208]
- Fix(datasource):  convert the type of ob-mysql-sharding data source to
  ob-mysql (#1253) [zhangxiao]
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


