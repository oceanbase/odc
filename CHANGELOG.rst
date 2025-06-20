(unreleased)
------------

New
~~~
- Feat(schdule): add schedule task import. [Ang]
- Feat(schedule): add schedule task import api (#4276) [Ang]
- Feat(export):schedule export support sqlPlain and PartitionPlan
  (#4259) [Ang]
- Feat(log): add route log callable (#4251) [Ang]
- Feat(flow): add cancel flow method without permission (#4250) [Ang]
- Feat(configarchive): support acvhive shchedule config. [Ang]
- Feat(schedule): support schedule export in Community edition (#4298)
  [Ang]
- Feat(schdule): add schedule task import. [Ang]
- Feat(schedule): add schedule task import api (#4276) [Ang]
- Feat(rbac): support selecting 'resources I created' when creating a
  role (#4265) [pynzzZ]
- Feat(export):schedule export support sqlPlain and PartitionPlan
  (#4259) [Ang]
- Feat(log): add route log callable (#4251) [Ang]
- Feat(flow): add cancel flow method without permission (#4250) [Ang]
- Feat(configarchive): support acvhive shchedule config. [Ang]
- Feat(materialized view):adapt to the latest ob4352 changes (#4382)
  [zijia.cj]
- Feat(config): Add a data source key configuration item in the
  organization configuration. (#4341) [GGBond]
- Feat(materialized view):Implement the logic of obtaining indexes and
  constraints for materialized views (#4357) [zijia.cj]
- Feat(flow): Merges apply database or table permission flow instance
  (#4322) [ystring]
- Feat(session): support client session. [Ang]
- Feat(materialized view):add drop index function and more display
  information for materialized view. [zijia.cj]
- Feat(task): correct task timeout to endtime. [LioRoger]
- Feat(materialized view): materialized view adapt to existing functions
  in odc. [zijia.cj]
- Feat(config): add organization config API impl (#4280) [GGBond]
- Feat(materialized view):obtain refresh records for materialized view
  (#4310) [zijia.cj]
- Feat(materialized view): add implement of materialized view for ob
  oracle (#4302) [zijia.cj]
- Feat(schedule): Adds landing page to view alterSchedule task stat and
  database access history (#4277) [ystring]
- Feat(ob-sql-parser):add materialized view parsing support for ob msyql
  (#4288) [yh263208@oceanbase.com, zijia.cj]
- Feat(script): add batch download scripts method. [CHLK]
- Feat(materialized view):add new controllers for materialized view and
  materialized view log (#4255) [zijia.cj]
- Feat(database): Supports to add database remark (#4273) [ystring]
- Feat(config): add ddl 4 create table and load meta data (#4275)
  [GGBond]
- Feat(partition-plan): optimized character types partition for
  automatic partitioning (#4252) [kiko]
- Feat(partition-plan): optimized  numeric types partition for automatic
  partitioning  (#4244) [kiko]
- Feat(config): add organization configuration (#4257) [GGBond]
- Feat(database): supports query database by tenantName、clusterNam and
  dataSourceName (#4232) [ystring]
- Feat(database): Sync current user database objects when grouping
  databases (#4236) [ystring]
- Feat(task): introduce supervisor agent. [LioRoger]
- Feat(osc): support drop column sql. [LioRoger]
- Feat(schedule):support common quartz schedule job (#4092) [zhangxiao]
- Feat(load-data): describe user adapt obcloud (#4078) [LuckyLeo]
- Feat(cloud): add getConfiguration method in CloudMetadataClient
  (#4008) [zhangxiao]
- Feat(ObjectStorage): add copyObject in ObjectStorageClient (#3980)
  [CHLK]
- Feat(workflow): integration airflow rest api (#3954) [LuckyLeo]
- Feat(ConnectSession):  add param createConnectionSessionSupplier in
  nullSafeGet,  add param maxConcurrentTaskCount in
  createPhysicalConnectionSession  (#3897) [CHLK]
- Feat(sql-execution-plan): add errorCode for intelligent analysis of
  sql execution plan (#3881) [zhangxiao]
- Feat(LimitMetric): add several LimitMetrics (#3835) [CHLK]
- Feat(loaddata): support dss datasource (#3832) [LuckyLeo]
- Feat(ErrorCodes): add EditVersionConflict in ResourceType for general
  version conflict error (#3834) [CHLK]
- Feat(dashboard): support dashboard (#3808) [CHLK]
- Feat(workflow): add a new resource type for workflow cluster (#3755)
  [IL MARE]
- Feat(load-data): upgrade ob-loader-dumper to 4.3.1.1 (#3753)
  [LuckyLeo]
- Feat(dlm): response the comments on #4151 (#4165) [guowl3]
- Feat(task): customized task submission. [guowl3]
- Feat(sql-check):support part of modify data type online ddl check.
  [LioRoger]

Changes
~~~~~~~
- Refactor(schedule): fill attribute at scheduleMapper (#3937) [Ang]

Fix
~~~
- Fix(schedule): To stat the count of all state schedule for each
  scheduleType type (#4418) [ystring]
- Fix:The prefix and suffix naming conventions need to verify that the
  time format cannot be empty. [梓烟]
- Fix:support partition plan naming suffix expr. [梓烟]
- Fix:support partition plan naming suffix expr. [梓烟]
- Fix(session): fix SQL execute generate result failed caused by
  material view. [GGBond]
- Fix(db browser):failed to get table in doris #4420. [zijia.cj]
- Fix(db browser): fix the syntax error of fuzzy query (#4415)
  [zijia.cj]
- Fix(audit): Fix the database operation type in the audit event that
  does not record database info (#4414) [GGBond]
- Fix(task): k8s parse resource identifier failed. [LioRoger]
- Fix(flow):  apply project tickets not shown when it's under approval
  (#4407) [pynzzZ]
- Fix(db-browser): Add '%' when to like table name (#4401) [ystring]
- Fix:support partition plan naming suffix expr. [梓烟]
- Fix:support partition plan naming suffix expr. [梓烟]
- Fix(collaboration): access denied when synchronizing obcloud
  organization #4363. [LuckyLeo]
- Fix(object-storage): use doesBucketExist for gcs (#4361) [LuckyLeo]
- Fix(alarm): Listener may not load. [Ang]
- Fix(permission): authentication will wrongly fail when resource ids
  are the same but resource types are different #4319. [pynzzZ]
- Fix(obcloud):filter obcloud tenants when status is not connected.
  [zhangxiao]
- Fix(cloud): fail to start odc when oss location is finance region
  #4301. [LuckyLeo]
- Fix(import): schedule import not have default name and lack of migrate
  sql  (#4295) [Ang]
- Fix(obcloud): failed to remove user from organization in obcloud
  #4239. [zhangxiao]
- Fix(cloud):failed to deserialize OBInstanceType. [zhangxiao]
- Fix(audit): add intsance migrate audit meta data. [Ang]
- Fix(alarm): Listener may not load. [Ang]
- Fix(task): find execute timeout task to destroy. [LioRoger]
- Fix(tripartite): Improve the query efficiency of precheck. [Ang]
- Fix(schedule): can't teminate schedule. [Ang]
- Fix(riskLevel): add default detect rule condition that is task type
  equals APPLY_TABLE_PERMISSION (#4369) [ystring]
- Fix(db browser):desc view without schema (#4386) [zijia.cj]
- Fix(migrate): rename duplicate migrate file (#4379) [GGBond]
- Fix(session): Supports fuzzy matching of table names and view names
  (#4373) [ystring]
- Fix(objectstorage): response InvalidArgument error when download file
  from download presigned url of aws s3. [CHLK]
- Fix(flow): fix multiple database change task parameter incompleteness
  #4367. [GGBond]
- Fix(flow): can view project when locate at applyDatabasePermission or
  applyTablePermission #4372. [ystring]
- Fix(resource): let resource destroy concurrent safe. [LioRoger]
- Fix:fix partition plan error. [梓烟]
- Fix:fix partition plan error. [梓烟]
- Fix(riskLevel): the riskLevelDescriber under the two databases can be
  distinguished  (#4344) [ystring]
- Fix(Script): add duplicated filename tag before suffix of filename.
  [CHLK]
- Fix(sql check ): Getting the number of sql affected rows does not
  support the old ob version (#4342) [zijia.cj]
- Fix(notification): fix metadata and message resending. [LuckyLeo]
- Fix(schedule): landing page displayed number of partition plans
  enabled is incorrect (#4338) [ystring]
- Fix(sql check):There are no ob oracle and oracle options for sql check
  rule that unable to Judge affected rows  (#4336) [zijia.cj]
- Fix: Optimized the interface for loading the partition planning table.
  [梓烟]
- Fix: partition plan task invalid when delete any table. [梓烟]
- Fix: partition plan task invalid when delete any table. [梓烟]
- Fix: support delete when datasource invalid. [梓烟]
- Fix(session): fix query limit conflict #4325. [GGBond]
- Fix(task): force update job last_heart_time once when job started.
  [LioRoger]
- Fix(database): listing interface did not display datasource
  attributes. [guowl3]
- Fix(data-transfer): fail to merge file. [LuckyLeo]
- Fix(Script): only one script download when select many same name
  scripts (#4294) [CHLK]
- Fix(table permission): When executing sql in an sql window, a
  permission check on a table involves a database that does not exist
  (#4269) [zijia.cj]
- Fix(task): doStopV2 scheduler not in transaction exception. [LioRoger]
- Fix(resource): delete redundant status transfer of k8s deployment
  #4184. [LuckyLeo]
- Fix(migrate): vectorydb only load on server mode #4161. [Ang]
- Fix(db-browser): remove @JsonProperty in DBTableStats.dataSizeInBytes
  #4129. [zhangxiao]
- Fix(db-browser):can not get the precision of year data type column
  #4127. [zhangxiao]
- Fix(workflow): add dfa transfer from unknown state (#4118) [LuckyLeo]
- Fix(schdule):delete schedule with check sub running tasks #3998.
  [kiko]
- Fix(DefaultSqlExecuteTaskManager): npe in
  DefaultSqlExecuteTaskManager. [CHLK]
- Fix(task): change DefaultResourceOperatorBuilder default resource
  match type. [LioRoger]

Security
~~~~~~~~
- Security: return error details when login failed. [pynzzZ]
- Security: lack permission check when listing another user's resources.
  [pynzzZ]
- Security: upgrade tomcat from 9.0.98 to 9.0.99. [pynzzZ]
- Security: upgrade tomcat version to 9.0.98 #4305. [pynzzZ]


v4.3.3_bp2 (2025-03-05)
-----------------------

New
~~~
- Feat(dlm): create temporary tables when data is archived to file
  system (#4243) [guowl3]
- Feat(sql-plan): sync schedule description to subtask (#4218) [guowl3]
- Feat(dlm): response the comments on #4151 (#4165) [guowl3]
- Feat(task): customized task submission. [guowl3]

Fix
~~~
- Fix(sync-table): default value is too long (#4258) [guowl3]
- Fix(sync-table): throw NPE when target table is not exist (#4254)
  [guowl3]
- Fix(sync-table): the default value is considered missing when it is
  whitespace (#4247) [guowl3]
- Fix(ob-sql-parser): adapt some syntax rules of ob435 (#4245)
  [zijia.cj]
- Fix(pl debug):the username configured for new odp connections does not
  contain cluster names. #4240. [zijia.cj]
- Fix(notification): fail to generate notification event (#4230)
  [LuckyLeo]
- Fix(sql-execute): nls_format invalidation. [LuckyLeo]
- Fix(dlm): upgrade sdk to 1.2.1 (#4225) [guowl3]
- Fix(osc): add uid for oms updateConfigRequest. [LioRoger]
- Fix(config): add task enabled configurations #4222. [zhangxiao]
- Fix(osc): updateRateLimiterConfig npe issue. [LioRoger]
- Fix(dlm): correct status when task is terminated. [guowl3]
- Fix(task): sync report taskResult call. [LioRoger]
- Fix(schedule): unreasonable parameter validation (#4212) [guowl3]
- Fix(task): process mode not invoke destroy action when job is
  canceled. [LioRoger]
- Fix(permission): 'AccessDenied' happens when project owners try
  swapping tables in an osc task. [pynzzZ]
- Fix(task): change DefaultResourceOperatorBuilder default resource
  match type. [LioRoger]


v4.3.3_bp1 (2025-01-23)
-----------------------

Fix
~~~
- Fix(changelog): cannot rollback update changelog status (#4198)
  [guowl3]
- Fix(connection): failed to connect to ob sharding mysql (#4197)
  [zhangxiao]
- Fix(changelog): the status does not progress to 'Failed' when a change
  error occurs (#4196) [guowl3]
- Fix(migrate): skip builtin ruleset migration when migrating rules
  #4192. [pynzzZ]
- Fix(risklevel): delete user defined environments may cause NPE due to
  the detect rule could be null #4191. [pynzzZ]
- Fix(migrate): upgrade to 4.3.3 will fail if it exists custom
  environments before #4186. [pynzzZ]
- Fix(sql-check): it may occur NPE when doing SQL check #4187. [pynzzZ]
- Fix(schedule): detail schedule in the individual space may fail #4188.
  [pynzzZ]
- Fix(datatransfer): failed to re-import odc exported zip file #4189.
  [LuckyLeo]


v4.3.3 (2025-01-17)
-------------------

New
~~~
- Feat(dlm): support filesystem (#4151) [guowl3]
- Feat(resource): add double check when destroy resource. [LioRoger]
- Feat(permission): support for global project roles (#3820) [pynzzZ]
- Feat(pl debug): pl debugging adapt odp directional routing (#3938)
  [zijia.cj]
- Feat(session): SQL console connection keep alive (#3993) [pynzzZ]
- Feat(project): support delete projects (#3948) [pynzzZ]
- Feat(permission): add view permission control (#3946) [zijia.cj]
- Feat(sso): support saml integration (#3783) [Ang]
- Feat(sub partition) : add the associated partition information for sub
  partition  (#3926) [zijia.cj]
- Feat(subpartition): finish sub partitions (#3905) [zijia.cj]
- Feat(session): support Oracle kill session (#3898) [pynzzZ]
- Feat(regulation): sql check and sql console rules support for Oracle
  (#3877) [pynzzZ]
- Feat(dlm): data archive support generating dynamic target table name
  (#3883) [kiko]
- Feat(logicaldatabase): add missed code for data.sql when merge main to
  dev/4.3.x (#3882) [LioRoger]
- Feat(external table): external table support sensitive column (#3821)
  [zijia.cj]
- Feat(sql-console-rule): sql console adapt sql type of call ,comment on
  and set session (#3764) [zijia.cj]
- Feat(external tables): supports syncing external table files (#3802)
  [zijia.cj]
- Feat(task): remove DefaultTaskResult (#3827) [LioRoger]
- Feat(task): refactor task interface, introduce task runtime component
  (#3807) [LioRoger]
- Feat(web): session cookie secure default false (#3798) [yizhou]
- Feat(ticket): allow project members to view  and owners to update all
  tickets in the project (#3739) [pynzzZ]
- Feat(task): refactor base task, let it not be force depended any more
  (#3796) [LioRoger]
- Feat(ob-sql-parser): make parser adapt for oceanbase 4.3.3 (#3762) [IL
  MARE]
- Feat(task): refactor task getStatus interface return TaskStatus
  instead of JobStatus (#3766) [LioRoger]
- Feat(pl): support editing pl sql for ob mysql (#3661) [zijia.cj]
- Feat(sqlcheck): add sql affect rows support for oracle and ob oracle
  (#3735) [zijia.cj]
- Feat(ob-sql-parser): adapt for OceanBase 4.3.3 (#3686) [pynzzZ]
- Feat(data-transfer): upgrade ob-loader-dumper version to 4.3.1.1
  (#3247) (#3681) [LuckyLeo, youshu]
- Feat(task): separate resource state from job (#3700) [LioRoger]
- Feat(authentication): add currentProjectId in AuthenticationFacade
  (#3643) [CHLK]
- Feat(object-storage): support get abortable object stream #3654.
  [LuckyLeo]
- Feat(dataset): add dataset id into LoadDataResource (#3646) [LuckyLeo]
- Feat(alarm): supports list schedule by ob project id (#3634) [guowl3]
- Feat(resourcehistory): add batchAdd in ResourceLastAccessService
  #3635. [CHLK]
- Feat(objectstorage): add generatePresignedUrlWithCustomFileName
  (#3633) [CHLK]
- Feat(workspace): move WorkspaceDataSourceAdapter from odc-workspace to
  odc-service to decouple odc-workspace and obcloud-starter (#3623)
  [CHLK]
- Feat(sql): add sql execute interceptor enabled flag (#3607) [CHLK]
- Feat(vectordb): add a vectordb configuration for ai application
  (#3594) [IL MARE]
- Feat(shared): add WORKSPACE_COUNT_IN_PROJECT in LimitMetric (#3494)
  [CHLK]
- Feat(response): add batch operation response (#3428) [CHLK]
- Feat(resource): adapt for other k8s resource and add resource status
  dfa impl (#3379) [IL MARE]
- Feat(worksheet): move worksheet to odc-cloud (#3409) [CHLK]
- Feat(worksheet): add organizationId and workspaceId to worksheet
  (#3345) [CHLK]
- Feat(worksheet): add implementation for flat list (#3301) [CHLK]
- Feat(workspace): add the capabilities for workspace development in
  odc-cloud  (#3333) [CHLK]
- Feat(resource): adapting resource modules to workflows  (#3313) [IL
  MARE]
- Feat(resource): introduce resource module (#3291) [LioRoger]
- Feat(resourcehistory): add resource last access time (#3190) [CHLK]
- Feat(worksheet): add implementation for worksheet (#3138) [CHLK]
- Feat(dlm): upgrade dlm sdk version to 1.1.6 (#3579) [guowl3]
- Feat(schedule): cherry-pick the new read log code to obcloud (#3583)
  [ystring]
- Feat(osc): check ghost table not exist before dispatch flow task
  (#3605) [LioRoger]
- Feat(external table): adapt external tables (#3195) [zijia.cj]

Changes
~~~~~~~
- Doc: init 4.3.3 changelog (#4162) [guowl3]
- Refactor(resource): refactor the logic of getting related pods of a
  deployment (#3526) [IL MARE]
- Refactor(resourcehistory): shorten the length of index index names in
  history_resource_last_access (#3411) [CHLK]
- Refactor(dfa): move it from odc-core to odc-common (#3299) [IL MARE]

Fix
~~~
- Fix(migrate): 4331 migrate script may failed on ob1479 #4179. [Ang]
- Fix(session): add retry logic when get session list #4176. [pynzzZ]
- Fix(session): kill console query may failed cause session occupied
  #4175. [Ang, MarkPotato777]
- Fix(session): DBSession does not involve svrIp when OB version less
  than 4.0 #4174. [pynzzZ]
- Fix(datatransfer): invalid batchSize when importing external csv file.
  [LuckyLeo]
- Fix(session): get wrong server port #4171. [Ang]
- Fix(session): killing session may fail due to a wrong regex #4169.
  [pynzzZ]
- Fix(session): killing sessions failed in OSC tasks #4166. [pynzzZ]
- Fix(sql-parser): failed to parse table ddl when create fulltext key
  with parser #4154. [zhangxiao]
- Fix(schedule):delete without running task #4157. [kiko]
- Fix(sql check):add two sql check rules for CREATE LIKE/AS statement.
  [zijia.cj]
- Fix(session): kill session may happen invalid server ip address #4144.
  [Ang]
- Fix(schedule): cherry pick describe database (#4150) [guowl3]
- Fix(session): kill oracle session may cause sockettimeout #4148.
  [pynzzZ]
- Fix(schedule): remove slow sql #4130. [guowl3]
- Fix(logicaldatabase): it may NPE when the logical database task
  starts. [pynzzZ]
- Fix(schedule):check has running task when delete (#4143) [kiko]
- Fix(audit): update i18n resources and add archive/delete projects
  audit events #4141. [pynzzZ]
- Fix(session): oracle mode effected by kill-query-or-session.max-
  supported-ob-version #4134. [Ang]
- Fix(permission): external approval integration doesn't take effect
  #4128. [pynzzZ]
- Fix(task): log4j set failed for task. [LioRoger]
- Fix(session): kill session may lead npe #4114. [Ang]
- Fix(permission): global project roles cannot operate database/table
  permission apply and schedule tickets #4124. [pynzzZ]
- Fix(structure-compare):the structure synchronization task cannot be
  initiated when the structure comparison task is not created by
  yourself #4122. [zhangxiao]
- Fix(permission): global project roles cannot view/execute/approve
  tickets #4117. [pynzzZ]
- Fix(function):The function does not display properly if the return
  value type is year (#4093) [zijia.cj]
- Fix(db):miss quote of pl name which contains @ causes pl edit failed
  #4115. [zijia.cj]
- Fix(permission): global project role cannot see approvable tickets
  #4116. [pynzzZ]
- Fix(migrate): complete data for connect_database.connect_type (#4113)
  [pynzzZ]
- Fix(db): The method of determining whether opening the global client
  session is incorrect. [zijia.cj]
- Fix(session):drop pl require database change permission #4112.
  [zijia.cj]
- Fix(integration): it doesn't sync internal schemas in the project in
  the bastion mode #4109. [pynzzZ]
- Fix(notification): fail to enqueue schedule event (#4091) [LuckyLeo]
- Fix(execution-plan): avoid invalid number #4087  Open. [LuckyLeo]
- Fix(notification): support send account name in message (#4085)
  [LuckyLeo]
- Fix(flow): add currentUserResourceRole in flow APIs #4096. [pynzzZ]
- Fix(dbbrowser): cant get correct ddl of procedure (#4084) [zijia.cj]
- Fix(db): cannot replace original pl name where editing pl (#4088)
  [zijia.cj]
- Fix(task): correct region key name for resourceID. [LioRoger]
- Fix(saml): saml may blocked tomcat thread. [Ang]
- Fix(approval): approval is not needed in individual organizations
  #4067. [guowl3]
- Fix(project): archiving projects will fail because of wrong check of
  tickets references #4089. [pynzzZ]
- Fix(flow):view export function can be initiated without permission
  #4090. [zijia.cj]
- Fix(changelog): opt the changelog content (#4083) [guowl3]
- Fix(osc): remove distinct from osc query user sql. [LioRoger]
- Fix(permission):time is missing when initiating apply database task
  again (#4046) [zijia.cj]
- Fix(db-browser): failed to get table due to create extended stats in
  column in oracle mode (#4026) [zhangxiao]
- Fix(migrate): rename V_4_3_3_1 to V_4_3_3_2 (#3974) [zijia.cj]
- Fix(task): add index to resource_resource table. [LioRoger]
- Fix(task): rollback DestroyExecutorJob to do destroy job action.
  [LioRoger]
- Fix(pl-edit): procedure name in drop procedure statements are
  recognized as the table name in DBSchemaExtractor (#3894) [zijia.cj]
- Fix(sql-check): cannot get oracle affect sql rows (#3892) [zijia.cj]
- Fix(external table):get table and external table together without
  exception (#3793) [zijia.cj]
- Fix(taskframework): abnormal status caused by concurrent updates
  (#3734) [guowl3]
- Fix(query-profie): NPE occurred when filter's value is null #3737.
  [LuckyLeo]
- Fix(taskframework): update load data task result only (#3726)
  [LuckyLeo]
- Fix(query-profile): set observer's version to 4.3.3.1 (#3723)
  [LuckyLeo]
- Fix(task-framework): check job status before cancel (#3721) [guowl3]
- Fix(database-change): condition search invalid (#3720) [kiko]
- Fix(database-change): ob multi cloud cancel permission verification
  (#3718) [kiko]
- Fix(schedule): fail to filter by tenantId and clusterId (#3709)
  [guowl3]
- Fix(datasource): npe will be thrown when calling get connection
  concurrently (#3717) [IL MARE]
- Fix(task-framework): starting the job twice (#3716) [guowl3]
- Fix(database-chabge): allow delete finished schedule (#3715) [kiko]
- Fix(errorCode): add error code for cloud service error (#3714)
  [zhangxiao]
- Fix(task-framework): update task result before canceling (#3710)
  [LuckyLeo]
- Fix(datasource): no return DSS datasource id #3712. [guowl3]
- Fix(database-change): sql content delimiter invalid (#3708) [kiko]
- Fix(dlm): upgrade dlm sdk version to 1.1.6.bp (#3679) [guowl3]
- Fix(database-change): task status error (#3704) [kiko]
- Fix(schedule): the creatorIds field in QueryScheduleParams is not
  valid #3697. [LuckyLeo]
- Fix(scheduleTask): correct status on task restart (#3689) [guowl3]
- Fix(database-change): task execute sql content failed (#3699) [kiko]
- Fix(database-change): stream closesed during task execution (#3691)
  [kiko]
- Fix(load-data): fix start load task without region (#3693) [LuckyLeo]
- Fix(errorcodes): add ErrorCodes: WorkspaceDatabaseUserTypeMustBeAdmin
  (#3680) [CHLK]
- Fix(database-change): sql plan task execute file script failed (#3675)
  [kiko]
- Fix(global-search): failed to sync database which get 'PENDING' status
  and 'object_last_sync_time' is null (#3731) [IL MARE]
- Fix(osc): osc task result not update when oms step info changed
  (#3678) [LioRoger]
- Fix(dlm): the status is incorrect if creating the pod timeout (#3671)
  [guowl3]
- Fix(datasource): missing tenantName and instanceName in data source
  details (#3664) [guowl3]
- Fix(sync-database): modify syncDatabaseTaskExecutor QueueCapacity to
  Integer.MAX_VALUE (#3667) [zhangxiao]
- Fix(taskframework): cannot release pod if OSS initialization fails
  #3669. [guowl3]
- Fix(approval): default no approval in obcloud (#3666) [guowl3]
- Fix(databaseChange): load file from oss failed #3652. [guowl3]
- Fix(database-change): sql plan task execute failed (#3645) [kiko]
- Fix(database-change): get object input stream error (#3640) [kiko]
- Fix(database-change): failed to filter database changes by
  organization (#3638) [guowl3]
- Fix(load-data): add instanceId and tenantId into OBInstance (#3636)
  [LuckyLeo]
- Fix(database-change): database connect failed due to password invalid
  (#3629) [kiko]
- Fix(database-change):datasource can not connect (#3620) [kiko]
- Fix(sql-check): a specific sql will lead to npe (#3611) [IL MARE]
- Fix(dlm): NPE when create task (#3608) [guowl3]
- Fix(schedule): multi-cloud oss config error when download log bases on
  obcloud (#3584) [ystring]
- Fix(schedule): detail schedule failed (#3569) [guowl3]
- Fix(resource): the status will be 'UNKNOWN' when resource is not
  created immediately (#3539) [IL MARE]
- Fix(response): error dependency for Error in ResourceOperationResult
  (#3431) [CHLK]
- Fix(dlm): check datasource cloud provider and region (#3567) [guowl3]
- Fix(database-change): supports the execution of sql by downloading
  files from a separate bucket (#3436) [kiko, tinker]
- Fix(dlm): task status is wrong (#3491) (#3518) [guowl3]
- Fix(load-data): several bugfix (#3471) [LuckyLeo]
- Fix(datasource): fail to access the MySQL data source via a proxy
  (#3482) [guowl3]
- Fix(schedule): restore deleted method (#3455) [LuckyLeo]
- Fix(sqlplan): generate task failed (#3535) [guowl3]
- Fix(schedule): risk level mismatch when operating a schedule (#3529)
  [guowl3]
- Fix(dlm): task status is wrong (#3491) [guowl3]

Security
~~~~~~~~
- Security: add @SkipAuthorize for IntegrationService #4160. [pynzzZ]
- Security: add @SkipAuthorize in public method #4153. [pynzzZ]
- Security: exclude sshd-common from spring-cloud-context (#3901)
  [pynzzZ]
- Secure(framework): enable secure cookie for http session (#3781)
  [yizhou]


v4.3.2_bp3 (2024-12-27)
-----------------------

Fix
~~~
- Fix(migrate): newly added sql check rules in 4.3.2 will not be
  migrated successfully #4105. [pynzzZ]


v4.3.2_bp2 (2024-12-20)
-----------------------

New
~~~
- Feat(common): reduce log of JsonUtils. [LioRoger]
- Feat(dlm): support configure sharding strategy (#3275) [guowl3]
- Feat(dlm): update dlm sdk version to 1.1.4.bp1 (#3251) [guowl3]
- Feat(schedule): check the validity of the task each time it is
  scheduled (#3767) [guowl3]
- Feat(alarm): complete the stability optimization of alarm message
  (#3609) [IL MARE, LioRoger, LuckyLeo, pynzzZ, ystring]
- Feat(task): introduce errorMessage to TaskResult interface (#3647)
  [LioRoger]
- Feat(session): add client info when connection session init (#3642)
  [Ang]
- Feat(common): sensitive data utils support mask json (#3653) [Ang]
- Feat(monitor): add prometheus actuator (#3322) [Ang]
- Feat(osc): check ghost table not exist before dispatch flow task
  (#3605) [LioRoger]

Changes
~~~~~~~
- Refactor(monitor): refactor MeterName to interface and exclude
  repository metrics (#3727) [Ang]
- Refactor(alarm): adjust the structure of alarm message (#3740)
  [ystring]
- Refactor(schedule): add schedule task full log download url to
  OrganizationAuthenticationInterceptor whiteList (#3650) [ystring]

Fix
~~~
- Fix(authorize): skip authorization for inner methods. [guowl3]
- Fix(actuator): disable actuator autoconfiguration in client mode
  (#4047) [Ang]
- Fix(actuator): diasble actuator by default (#3991) [Ang]
- Fix(session): add non support kill version (#3979) [Ang]
- Fix(session): add svrIp in session list (#3961) [Ang]
- Fix(osc): add version whitelist to enable lock table feature.
  [LioRoger]
- Fix(taskframework): cherry-pick the bug fix from obcloud_202409bp
  (#3909) [guowl3]
- Fix(manual execution): manual execution button should not be displayed
  after clicking manual execution in odc431 (#3279) [zijia.cj]
- Fix(statefulRoute): sensitive column may can't get result (#3261)
  [Ang]
- Fix(resultset-export): the exported file is empty if use lower table
  name for oracle mode (#3254) [LuckyLeo]
- Fix(statefulRoute): batchCompilations and getRecyclebinSettings will
  failed if statefulroute enabled (#3257) [Ang]
- Fix(logicaldatabase): add system config to enabled/disable the logical
  database feature #3863. [pynzzZ]
- Fix(query-profile): set observer's version to 4.3.3.1 (#3723) (#3838)
  [LuckyLeo]
- Fix(taskframework): merge 24v4 into 202407 (#3837) [guowl3]
- Fix(session): logical session status will npe (#3836) [Ang]
- Fix(task): add task framework error message (#3830) [ystring]
- Fix(datasource): datasource sync failed (#3829) [pynzzZ]
- Fix(schedule): only perform automatic termination checks for periodic
  tasks (#3828) [guowl3]
- Fix(session): put killCurrentQuerySupported into session (#3824) [Ang]
- Fix(session): disable kill session/query for logical database session
  (#3822) [pynzzZ]
- Fix(schedule): correct the status when scheduling tasks (#3823)
  [guowl3]
- Fix(session): DefaultConnectSessionFactory will failed if not in
  spring context (#3810) [Ang]
- Fix(datasource): add unit tests for sync data source (#3816) [pynzzZ]
- Fix(datasource): data source sync failed (#3814) [pynzzZ]
- Fix(partition-plan): task's status is not as expected when error
  strategy is set to 'ignore any errors' (#3805) [IL MARE]
- Fix(schedule): can't map schedule task parameter (#3803) [Ang]
- Fix(alarm): delete duplicate alarm (#3795) [guowl3]
- Fix(monitor): meterKey construct use interface #3792. [Ang]
- Fix(schedule): can map PartitionPlan parameter (#3790) [Ang]
- Fix(osc): correct osc session get key to CONSOLE_DATASOURCE (#3788)
  [LioRoger]
- Fix(session): use sys datasource may database not exits (#3787) [Ang]
- Fix(osc): mdc context may not correct (#3780) [LioRoger]
- Fix(data-transfer): read timout occurred when getting large for
  internal usage (#3778) [LuckyLeo]
- Fix(session): session status typo (#3751) [Ang]
- Fix(osc): remove ip mask when match user and session (#3747)
  [LioRoger]
- Fix(schedule): pick schedule fix from obcloud 202409 (#3705) [Ang]
- Fix(osc): osc task result not update when oms step info changed
  (#3678) [LioRoger]

Security
~~~~~~~~
- Security: follow a authenticated schedule id allows you to perform
  operations on any other schedule tasks (#3741) [ystring]
- Security(notification): filter illegal characters in notification
  webhook (#3779) [LuckyLeo]


v4.3.2_bp1 (2024-10-17)
-----------------------

Changes
~~~~~~~
- Doc: update 4.3.2 release date in changelog (#3624) [pynzzZ]
- Refactor(schedule): add listByJobNames to ScheduleTaskService (#3614)
  [ystring]

Fix
~~~
- Fix(rule): executing SQLs occurs NPE when the database belongs to a
  customized environment after upgrading to 4.3.2 (#3694) [pynzzZ]
- Fix(collaboration): databases' environment have not been updated after
  the datasource's environment changed #3695. [pynzzZ]
- Fix(dlm): upgrade dlm sdk version to 1.1.6.bp (#3679) [guowl3]
- Fix(pl): PL debug will fail when there are databases with the same
  name under the data source (one exists and one does not) (#3687)
  [pynzzZ]
- Fix(collaboration): databases' environment have not been updated after
  the datasource's environment changed (#3688) [pynzzZ]
- Fix(dlm): custom sharding strategy being ineffective (#3677) [guowl3]
- Fix(build): obclient installation is missing from the ODC image
  (#3625) [LuckyLeo]
- Fix(sql-check): a specific sql will lead to npe (#3611) (#3617) [IL
  MARE]


v4.3.2 (2024-09-27)
-------------------

New
~~~
- Feat(dlm): upgrade dlm sdk to 1.1.6 (#3577) [guowl3]
- Feat(schedule): support download full log for running task (#3335)
  [ystring]
- Feat(taskframework): add job termination event for all scheduled tasks
  (#3344) [guowl3]
- Feat(logicaldatabase): logical database change task implementation
  (#3324) [pynzzZ]
- Feat(schedule): adaption for support obcloud operation event (#3325)
  [ZhanHong, jingtian]
- Feat(load-data):  add load data job  (#3280) [LuckyLeo]
- Feat(connection): adaption for obcloud kv instance type (#3120)
  [zhangxiao]
- Feat(partition-plan): opt part name generator (#3253) [guowl3]
- Feat(dfa): add simple dfa for workflow cluster's status transfer
  (#3289) [IL MARE]
- Feat(database-change): sql plan task execution (#3246) [IL MARE, kiko,
  zhangxiao]
- Feat(sqlcheck): SQL check supports affected  rows (#3147) [yiminpeng]
- Feat(data-transfer): data import supports file directory (#3248)
  [youshu]
- Feat(data-transfer): upgrade ob-loader-dumper version to 4.3.1.1
  (#3247) [youshu]
- Feat(database-change): add and adapt the current schedule interface
  (#3143) [LioRoger, kiko]
- Feat(logicaldatabase): rewriting the logical sql into the physical sql
  (#3176) [pynzzZ]
- Feat(logicaldatabase): supplementary implementation of logical
  database/table metadata management (#3146) [pynzzZ]
- Feat(module): increase sub-module loading capabilities  (#3154) [IL
  MARE]
- Feat(objectstorage): add pure object storage without the operating of
  object storage metadata and predefined objectName (#3119) [CHLK]
- Feat(dlm): support to connect postgres datasource (#3079) [kiko,
  tinker]
- Feat(schedule): schedule module adapts to the historical API (#3126)
  [guowl3]
- Feat(git-integration): add service layer for git repository
  integration (#3116) [LuckyLeo]
- Feat(osc):enhance osc, let swap table reentrant (#3123) [LioRoger]
- Feat(osc): refactor online schame change module, introduce fms package
  (#3106) [IL MARE, LioRoger]
- Feat(worksheet): add controller define for project files (#3089)
  [CHLK]
- Feat(integration-git): init git integration storage layer and git
  client (#3070) [LuckyLeo]
- Feat(log): display time zone when printing logs (#2932) [zhangxiao]
- Feat(util): enhance duplicated migrator validation, output detail
  scripts info (#2878) [yizhou]
- Feat(task-framework): task log download support multiple region
  scenarios (#2866) [yizhou]
- Feat(task-framework): handle executor endpoint for PULL monitor mode
  (#2851) [yizhou]
- Feat(schedule): modify the schedule module API and add change log
  (#2832) [guowl3]
- Feat(auth): add projectId in TraceContextHolder (#2788) [zhangxiao]

Changes
~~~~~~~
- Doc: 4.3.2 changelog English version (#3554) [pynzzZ]
- Doc: init 4.3.2 changelog (#3465) [pynzzZ]
- Refactor(load-data): move LoadDataParameters to odc-server (#3414)
  [LuckyLeo]
- Refactor(migrate): supports load sql script from custom jar file
  (#3209) [IL MARE]
- Refactor(web): remove unnecessary bean (#3130) [Ang]
- Refactor(statefulRoute): generalization plDebugsession to UUID stateId
  (#2960) [Ang]
- Refactor(schedule): separate the schedule module interface (#2881)
  [guowl3]
- Refactor(task-executor): involve pull mode for task monitor (#2801)
  [yizhou]

Fix
~~~
- Fix(dlm): update rate limit failed (#3597) [guowl3]
- Fix(notification): incorrent status of DLM task notification (#3595)
  [LuckyLeo]
- Fix(schedule): wrong status when a schedule change is rejected (#3593)
  [guowl3]
- Fix(dlm): OOM occurs when a table's unique key contains nullable
  columns (#3592) [guowl3]
- Fix(logicaldatabase): several bugfixes when extacting logical
  databases and showing the databases (#3575) [pynzzZ]
- Fix(flow): filter out all ALTER_SCHEDULE flow instances in 'all
  tickets', 'created by me' and 'approving by me' #3565. [pynzzZ]
- Fix(data-transfer): revert upgrade obloaderdumper 4311 (#3561)
  [youshu]
- Fix(sqlplan): generate task failed (#3535) (#3566) [guowl3]
- Fix(logicaldatabase): generated create logical table ddl is wrong
  (#3560) [pynzzZ]
- Fix(schedule): wrong status (#3563) [guowl3]
- Fix(schema): enable syncing databases by datasource in sql console
  (#3559) [zijia.cj]
- Fix(task-framework): remove useless saving results in case of updating
  status failure (#3557) [pynzzZ]
- Fix(task-framework): starting task fails when environment variables
  exceed 2MB in Process Mode (#3545) [pynzzZ]
- Fix(dlm): cherry pick from 4.3.x  (#3541) [guowl3]
- Fix(logicaldatabase): no logical database extract task is submitted
  after the logical change task is completed (#3528) [pynzzZ]
- Fix(sqlcheck): affected rows for the native MySQL INSERT statement
  cannot take effect (#3329) [yiminpeng]
- Fix(logicaldatabase): terminal sub-tasks failed (#3520) [pynzzZ]
- Fix(logicaldatabase): when sql contains physical databases that does
  not belong to the logical database, the front-end crashes (#3515)
  [pynzzZ]
- Fix(schedule): read log failed when to forward request (#3504)
  [ystring]
- Fix(logicaldatabase): failed to recognize the wrong expression like
  db.tb.xxx (#3501) [pynzzZ]
- Fix(logicaldatabase): add max physical database count limit in one
  logical database (#3500) [pynzzZ]
- Fix(database): the databases list should be ordered by type and name
  by default (#3499) [pynzzZ]
- Fix(multiple database): all database type must be same in multiple
  databases task (#3493) [zijia.cj]
- Fix(permission): logical database/table permission not shown (#3490)
  [pynzzZ]
- Fix(logicaldatabase): generate wrong ddl when alter table (#3496)
  [pynzzZ]
- Fix(logicaldatabase): generate wrong table topologies when logical
  expression is like db.tb_[00-10] #3495. [pynzzZ]
- Fix(logicaldatabase): physical databases who have already belonged to
  a logical database should not be able to configure again (#3492)
  [pynzzZ]
- Fix(logicaldatabase): incorrect status transform logic of logical
  database change subtasks (#3474) [pynzzZ]
- Fix(logicaldatabase): adapt logical database/table permission apply
  (#3452) [pynzzZ]
- Fix(schedule): get schedule detail may cause NPE in logical database
  job (#3454) [pynzzZ]
- Fix(osc): new create table ddl not display when use alter table mode
  (#3437) [LioRoger]
- Fix(logicaldatabase): project info not shown and schedule description
  shows 'null' (#3410) [pynzzZ]
- Fix(logicaldatabase): apply logicla database permission caused to NPE
  (#3435) [pynzzZ]
- Fix(structure-compare): show incorrect precision of tinyint datatype
  in mysql mode (#3419) [zhangxiao]
- Fix(script): local build libs failure (#3424) [ystring]
- Fix(table permission): index name displays as table name in sql
  windows check (#3401) [zijia.cj]
- Fix(taskframework): cannot release resource when logfile upload failed
  (#3417) [guowl3]
- Fix(migrate): alter column `value_json` size in
  `regulation_riskdetect_rule` table (#3396) [yiminpeng]
- Fix(schedule): several bugfix (#3377) [guowl3]
- Fix(table permission): partition name shouldn't display in the table
  permission check window (#3402) [zijia.cj]
- Fix(logicaldatabase): submit a extract logical table task after
  logical database change task done (#3372) [pynzzZ]
- Fix(logicaldatabase): logical database last sync time not shown
  (#3408) [pynzzZ]
- Fix(logicaldatabase): wrong generated ddl of creating and altering
  logical tables (#3399) [pynzzZ]
- Fix(permission): failed to execute sql of renaming table (#3361)
  [zijia.cj]
- Fix(mock-data): failed to mock data at mysql 5.7 (#3392) [IL MARE]
- Fix(database-change): sql plan job set database name as default schema
  (#3376) [kiko]
- Fix(dlm): create connection failed in oracle (#3355) [guowl3]
- Fix(execute-sql): failed to execute insert upload file sql (#3338)
  [zhangxiao]
- Fix(connection): scanning and adding sensitive columns, selecting a
  database will not display database details (#3271) [yiminpeng]
- Fix(auth): adaption for obcloud auth (#3151) [zhangxiao]
- Fix(dlm):  detail schedule task missing parameters (#3323) [guowl3]
- Fix(osc): osc manual swap table button not displayed (#3320)
  [LioRoger]
- Fix(sql-plan): detail sql-plan failed (#3321) [guowl3]
- Fix(database-change): missing sql plan task execution result  (#3290)
  [kiko]
- Fix(data-transfer): incorrect schema file would be generated when
  merge schema (#3315) [LuckyLeo]
- Fix(project): developer localized display errors. [zijia.cj]
- Fix(sql-plan): create sql-plan failed #3302. [guowl3]
- Fix(web): add server restart alarm (#3296) [Ang]
- Fix(data-transfer): fix several data-transfer bugs (#3288) [LuckyLeo]
- Fix(data-transfer): fail to start transfer task on windows (#3281)
  [LuckyLeo]
- Fix(sso): get state params may failed when use load banlance (#3273)
  [Ang]
- Fix(manual execution): manual execution button should not be displayed
  after clicking manual execution (#3269) [zijia.cj]
- Fix(sso): azure integration migrate failed (#3263) [Ang]
- Fix(deployment): ODC_APP_EXTRA_ARGS not works (#3244) [yizhou]
- Fix(connection): choosing the wrong mode when testing connection with
  sys tenant result in false success (#3124) [ZhanHong]
- Fix(web): fix tomcat threads config (#3201) [Ang]
- Fix(partition-plan): failed to generate partition name when using
  custom generator (#3131) [IL MARE]
- Fix(osc): compatible with cloud odc code, fix ScheduleTaskParameter
  deserialize… #3202. [LioRoger]
- Fix(connect): make odc connect to the obcloud instance (#3203) [IL
  MARE]
- Fix(logicaldatabase): index out of bound when there only exists one
  table in the database (#3160) [pynzzZ]
- Fix(dlm): alter file seq for 'connect_connection.sql' from 4 to 2
  (#3142) [kiko]
- Fix(task): fix 2203 by triggering task log rollover when task start up
  (#3088) [CHLK]
- Fix(schedule):  concurrent execution (#3017) [guowl3]
- Fix(organization): failed to list individual organization (#3007)
  [zhangxiao]
- Fix(schedule): concurrent executing job (#2999) [guowl3]
- Fix(dlm): update limit config throw NPE (#2990) [guowl3]
- Fix(schedule): check cron expression (#2989) [guowl3]
- Fix(schedule): parameters missing in changelog  (#2977) [guowl3]
- Fix(dlm): several bugfix (#2965) [guowl3]
- Fix(diagnose): get incorrect record from sql audit (#2974) [LuckyLeo]
- Fix(stateful): remove wrong generator conditional load (#2976) [Ang]
- Fix(statefulRoute): failed to list built-in snippets (#2935) [Ang]
- Fix(statefulRoute): fix list column can't reach (#2953) [Ang]
- Fix(connect): failed to connect to ap instance (#2967) [zhangxiao]
- Fix(session): support auto recreate session when upgrade (#2959)
  [zhangxiao]
- Fix(task-framework): ignore location validation and update task status
  after refresh (#2961) [guowl3]
- Fix(dlm): start job failed  (#2937) [guowl3]
- Fix(connection): automatically reconnect during the cloud upgrade
  process (#2924) [zhangxiao]
- Fix(alarm): condition not found (#2927) [guowl3, yizhouxw]
- Fix(schedule): several bugfix during integration with task framework
  (#2919) [guowl3, yizhouxw]
- Fix(auth): access denied for current project interface (#2862)
  [zhangxiao]
- Fix(taskframework): daemon job be fired at one time in cluster model
  (#2408) [krihy]

Security
~~~~~~~~
- Security: add @SkipAuthorize annotation (#3590) [pynzzZ]
- Security: some method lacks permission check (#3580) [pynzzZ]


v4.3.1_bp1 (2024-08-13)
-----------------------

Fix
~~~
- Fix(notification): class cast exception would be thrown when sending
  to feishu or wecom (#3118) [LuckyLeo]
- Fix(notification): support response type text/plain and others (#3114)
  [LuckyLeo]
- Fix(permission): users cannot call function in oracle mode (#3109)
  [pynzzZ]
- Fix(dlm): sync table structure failed (#3100) [guowl3]
- Fix(permission): 1) users could export the whole database even if they
  don't have database export permissions 2) SQL contains function call
  will be wrongly intercepted (#3101) [pynzzZ]
- Fix(object-search): failed to sync db object when sync logic is run by
  an async thread (#3104) [IL MARE]
- Fix(schema): show constraint's on delete rule correctly by parser
  (#3098) [zhangxiao]
- Fix(osc): enhance osc feature (#3087) [LioRoger]
- Fix(schedule): the creator cannot change the schedule (#3085) [guowl3]


v4.3.1 (2024-07-31)
-------------------

New
~~~
- Feat(kill session): adapt global client session and using block in
  oracle model in kill session (#2978) [zijia.cj]
- Feat(server): allows some beans to be loaded only in server mode
  (#2757) [Ang]
- Feat(sql-execute): supports service layer for query profile (#2423)
  [LuckyLeo]
- Feat(CI): support run build release by ob farm (#2738) [niyuhang]
- Feat(osc): add rate limiter for osc (#2402) [krihy]
- Feat(logical-database): logical database metadata management (#2358)
  [pynzzZ]
- Feat(config):add creator_id to config entity (#2485) [Ang]
- Feat(table-permission): supports table level authority control (#2324)
  [XiaoYang, isadba]

Changes
~~~~~~~
- Refactor(statefulRoute): generalization plDebugsession to UUID stateId
  (#2960) [Ang]
- Refactor: change the CODEOWNERS (#2931) [IL MARE]
- Chore: update client version (#2821) [Xiao Kang]
- Refactor(flow): add organizationId to flow instance detail (#2841)
  [Ang]
- Refactor(config): Add more fields to configEntity #2493. [Ang]

Fix
~~~
- Fix(parser): failed to recognize the schema or package name from an
  anonymous block (#3069) [IL MARE]
- Fix(table-permission): creating table needs table change permissions
  (#3057) [pynzzZ]
- Fix(flow): wrong approval flow for database/table permission apply
  ticket (#3072) [pynzzZ]
- Fix(database-permission): wrongly recognize packages as schemas
  (#3067) [pynzzZ]
- Fix(dlm): create the target table if the sync table structure is off
  in MySQL mode (#3050) [guowl3]
- Fix(table-permission): table permission apply tickets warning log not
  found (#3049) [pynzzZ]
- Fix(table-permission): could create tickets when users have no
  permission to the database (#3046) [pynzzZ]
- Fix(partition-plan): can not recognize the partition key's data type
  on mysql mode (#3039) [guowl3]
- Fix(dlm): several bug related to editing data cleaning (#3033)
  [guowl3]
- Fix(table-permission): not select the specific database/table by
  default when create the permission application ticket (#3035) [pynzzZ]
- Fix(database): creating databases under the data source failed (#3037)
  [pynzzZ]
- Fix(dlm): don't compare the table structure if syncTableStructure is
  off (#3014) [guowl3]
- Fix(login): set max_login_record_time_minutes default value to 0 in
  web mode (#3003) [Ang]
- Fix(data-transfer): clean work directory before import (#3006)
  [LuckyLeo]
- Fix(alarm): task alarm add exception message (#3004) [Ang]
- Fix(query-profile): modified the version supporting query profile
  (#3002) [LuckyLeo]
- Fix(diagnose): failed to view query profile for distributed OB (#2945)
  [LuckyLeo]
- Fix(db-browser): failed to recognize the commit and rollback statement
  (#2985) [IL MARE]
- Fix(global-search): unable to stop data object synchronization (#2928)
  [IL MARE]
- Fix(pre-check): failed to get sql check result when the check result
  file is not on this machine (#2943) [IL MARE]
- Fix(security): update oauth2 client version (#2981) [Ang]
- Fix(stateful): remove wrong condition (#2975) [Ang]
- Fix(osc): the online schema change blocked when rate limiter modified
  before swap table action (#2908) [LioRoger]
- Fix(web): modify tomcat keepAliveTimeout to 70 seconds (#2964) [Ang]
- Fix(statefulRoute): failed to list built-in snippets (#2935) [Ang]
- Fix(permission): fail to submit ticket if lack of database permission
  (#2946) [LuckyLeo]
- Fix(statefulRoute): fix list column can't reach (#2953) [Ang]
- Fix(import): add template api and supports mysql, oracle and doris
  datasource importing (#2936) [IL MARE]
- Fix(pl-debug): avoid npe during the pl debugging (#2930) [IL MARE]
- Fix(mock data): failed to cancel the mock data task (#2850) [zijia.cj]
- Fix(sql): the sql of modifying session parameter in oracle is error
  (#2872) [zijia.cj]
- Fix(migrate): fix login process resource load faild (#2883)
  [yiminpeng]
- Fix(flow): failed to startup a ticket (#2798) [IL MARE]
- Fix(audit): client ip length more langer then audit column
  client_ip_address (#2863) [CHLK]
- Fix(ob-sql-parser): failed to recognize interval expression in ob-
  oracle mode (#2873) [IL MARE]
- Fix(data viewing): get result-set timeout (#2848) [zijia.cj]
- Fix(database-permission): mistake caused by code merge (#2786)
  [XiaoYang]
- Fix(metadb): change systemConfigDao to systemConfigRepository. (#2467)
  [Ang]
- Fix(deserialization): failed to deserialize the page object (#2434)
  [Ang]
- Fix(taskframework): daemon job be fired at one time in cluster model
  (#2408) [krihy]

Security
~~~~~~~~
- Security: modify annotations on some service classes (#2955)
  [LuckyLeo]


v4.3.0_bp1 (2024-06-24)
-----------------------

Fix
~~~
- Fix(sql-check): remove the word 'id' from the reserved words (#2796)
  [IL MARE]
- Fix(clientMode): fail to migrate metadb in client mode (#2797)
  [LuckyLeo]
- Fix(data-transfer): avoid task failure by processing exception
  messages (#2779) [LuckyLeo]
- Fix(table-object): there would be an NPE if fail to parse index ddl
  (#2776) [LuckyLeo]
- Fix(multiple database): added exclusive description of the subticket
  (#2762) [zijia.cj]
- Fix(taskframework): running task be canceled incorrect due to
  heartbeat timeout  (#2763) [krihy]
- Fix(schedule): creator is not allowed to alter schedule (#2772)
  [guowl3]
- Fix(multiple database): frequently printing logs (#2765) [zijia.cj]
- Fix(taskframework): cannot rollback stop when destroy executor failed
  (#2755) [krihy]
- Fix(client-mode): odc failed to start in client mode (#2761)
  [LuckyLeo]


v4.3.0 (2024-06-11)
-------------------

New
~~~
- Feat(config):add creator_id to config entity (#2485) [Ang]
- Feat(dlm):upgrade dlm sdk version to 1.1.3 #2601. [guowl3]
- Feat(projectService): adaption for organization、project、role service
  (#2448) [zhangxiao]
- Feat(multipledatabase): add audit events for multiple databases
  (#2442) [zijia.cj]
- Feat(data-transfer): apply jdbc parameters and scripts in connection
  config to data transfer (#2455) [LuckyLeo]
- Feat(notification): notification support multiple database change task
  (#2469) [LuckyLeo]
- Feat(object-search): individual space support global object search
  (#2436) [XiaoYang]
- Feat(databasechange): implement the new interface and flow of multiple
  databases change (#2275) [Ang, Xiao Kang, zijia.cj]
- Feat(dlm): supports viewing schedule task details (#2354) [guowl3]
- Feat(dlm): data clearing tasks support data check before delete
  (#2401) [kiko]
- Feat(dlm): incremenntal table structure synchronization (#2189)
  [guowl3]
- Feat(migrate): migrate history uniqueIdentifier in
  collaboration_project (#2377) [zhangxiao]
- Feat(migrate): add unique identifier in collaboration project (#2372)
  [zhangxiao]
- Feat(object-management): add accessor and service support for column-
  group (#2349) [LuckyLeo]
- Feat(column-group): add support of column group into ob-sql-parser
  (#2300) [LuckyLeo]
- Feat(logicaldatabase): logical table expression parser (#2274)
  [pynzzZ]
- Feat(object-search): database schema synchronizing implementation
  (#2222) [XiaoYang]
- Feat(iam): password strength match oceanbase style (#2247) [yizhou]
- Feat(multiple databases changes): add new feature for multiple
  database changes (#1848) [jonas]
- Feat(datatype): update odc_version_diff_config for ob oracle
  SDO_GEOMETRY datatype (#2232) [zhangxiao]
- Feat(resultset): supports gis datatype for ob oracle mode (#2216)
  [zhangxiao]
- Feat(query-profile): add DTO and VO models for query profile (#2212)
  [LuckyLeo]
- Feat(ob-sql-parser): upgrade antlr g4 for oceanbase v4.3.0 (#2124)
  [yizhou]
- Feat(object-search): persistence and service layer implementation
  (#2155) [XiaoYang]
- Feat(logicaldatabase): supports automatic recognition of logical
  tables and logical table expression generation  (#2166) [pynzzZ]
- Feat(collaboration): support for configuring database administrators
  and participating in approvals (#2168) [XiaoYang, isadba]

Changes
~~~~~~~
- Refactor(schedule): add without permission method (#2670) [Ang]
- Refactor(security): add configurable security whitelists (#2714) [Ang]
- Refactor(flow): add skip auth to flow mapper (#2538) [Ang]
- Refactor(config): Add more fields to configEntity #2493. [Ang]
- Chore: use OBE error code (#2413) [yizhou]
- Refactor(sql-execute): refactor SQL async execute api into streaming
  return  (#2246) [LuckyLeo]

Fix
~~~
- Fix(schedule): terminate if schedule is invalid (#2725) [guowl3]
- Fix(structure-comparison): get a wrong result when comparing two same
  tables (#2720) [IL MARE]
- Fix(multiple database): change the method when initiating child
  tickets (#2719) [zijia.cj]
- Fix(ticket): failed to view all tickets (#2716) [IL MARE]
- Fix(dlm): upgrade dlm sdk to 1.1.4 (#2697) [guowl3]
- Fix(dlm): the task status does not update properly when structural
  synchronization fails (#2712) [guowl3]
- Fix(ticket): project owners failed to abort a ticket (#2709) [IL MARE]
- Fix(db-browser): failed to open SYS console when user without query
  sys permissions (#2708) [zhangxiao]
- Fix(security): add skip auth annotation (#2704) [guowl3]
- Fix(dlm): alter execute task job type for data cleaning (#2706) [kiko]
- Fix(multiple database): the return of method intercepted in multiple
  database pre check node is incorrect  (#2702) [zijia.cj]
- Fix(data-transfer): truncate will cause the import task to fail
  (#2679) [LuckyLeo]
- Fix(dlm): table structure synchronization failed (#2682) [guowl3]
- Fix(apply database): failing to apply database permission deliver
  (#2684) [zijia.cj]
- Fix(dlm): target database id is null in task framework mode (#2676)
  [guowl3]
- Fix(multiple database): the method isIntercepted in multiple database
  pre check node is error (#2677) [zijia.cj]
- Fix(parser): failed to parse json_function for native oracle (#2664)
  [IL MARE]
- Fix(db-object): exception occurred when open oracle table in GBK
  encoding (#2661) [LuckyLeo]
- Fix(dlm): task timeout was not effective (#2651) [guowl3]
- Fix(multiple database): pre sql check node failed (#2592) [zijia.cj]
- Fix(structure-comparison): syntax error when executing structure
  comparison (#2638) [IL MARE]
- Fix(dml): failed to modify data which is geometry type (#2640) [IL
  MARE]
- Fix(schema): failed to query variables on native oracle (#2649) [IL
  MARE]
- Fix(collaboration): can not modify the description of project (#2642)
  [XiaoYang]
- Fix(connect): failed to connect to a standby cluster and view table
  structure (#2648) [IL MARE]
- Fix(database-permission): wrong to check DB permission when existing
  Invalid DB with the same name (#2641) [XiaoYang]
- Fix(multiple database): error occurs when viewing the list without
  templates (#2639) [zijia.cj]
- Fix(schema): function and procedure list is not ordered by their name
  in ob-mysql (#2636) [IL MARE]
- Fix(dlm): table not found in task framework mode (#2637) [guowl3]
- Fix(schema): loading table detail costs too much time (#2626) [IL
  MARE]
- Fix(metadb): change systemConfigDao to systemConfigRepository. (#2467)
  [Ang]
- Fix(deserialization): failed to deserialize the page object (#2434)
  [Ang]
- Fix(flow-task): optimize error message of flow task cancelation
  (#2624) [LuckyLeo]
- Fix(stateful): batch compile failed with message 'stateId' (#2606)
  [Ang]
- Fix(flow): cannot find approvers for multiple database change task
  when using database owner  (#2625) [XiaoYang]
- Fix(data-transfer): exception occurs when object exists  and
  configured continue when error (#2587) [LuckyLeo]
- Fix(schedule): schedule cannot be disabled if project is archived
  (#2562) [guowl3]
- Fix(dlm): data delete retry failed (#2564) [guowl3]
- Fix(ticket): failed to approve ticket when input over-sized comment
  (#2565) [XiaoYang]
- Fix(flow): the disabled user can still approving or rejecting a flow
  (#2589) [XiaoYang]
- Fix(multiple database): optimize error message when creating and
  updating template (#2593) [zijia.cj]
- Fix(collaboration): vertical unauthorizing exists when editing
  database owners (#2590) [XiaoYang]
- Fix(multiple database): add project permission verification to the
  exist method (#2585) [zijia.cj]
- Fix(object-search): failed to sync database metadata in individual
  space (#2563) [XiaoYang]
- Fix(multiple database): no execution record is generated before or
  during a multi-database change task  (#2569) [zijia.cj]
- Fix(ticket): wrong i18n description for task (#2579) [XiaoYang]
- Fix(multiple database): the current database does not match the
  corresponding sql check result (#2584) [zijia.cj]
- Fix(dlm): table structure synchronization failure when table names are
  inconsistent (#2497) [guowl3]
- Fix(integration): basic auth miss authentication initialization
  (#2549) [yizhou]
- Fix(flow): failed to create a ticket which manual strategy in
  individual space (#2534) [yiminpeng]
- Fix(database): database sync involved no-privilege databases in
  OBMySQL (#2523) [pynzzZ]
- Fix(web): cannot return a page with more than 2000 records (#2520)
  [pynzzZ]
- Fix(multiple database): hover the template name does not show the
  contained database (#2542) [zijia.cj]
- Fix(dlm): set default value is source table name if data cleaning
  target table name is null (#2533) [kiko]
- Fix(connection): concurrent exception will be thrown when a connection
  is reset (#2528) [IL MARE]
- Fix(object-search): bad performance when syncing table or view columns
  (#2486) [XiaoYang]
- Fix(multiple databases): database changing order in template cannot be
  edited  (#2511) [zijia.cj]
- Fix(db-browser): adaption for ALL_TAB_COLS.USER_GENERATED in ob oracle
  (#2231) [zhangxiao]
- Fix(session): failed to set nls parameters for native oracle in sql-
  console (#2501) [IL MARE]
- Fix(dlm): optimize error message (#2498) [guowl3]
- Fix(dlm): sync table structure failed #2489. [guowl3]
- Fix(project): optimize error message when update a project name to an
  existed project name (#2464) [pynzzZ]
- Fix(dlm): archiving specified partition failed (#2474) [guowl3]
- Fix(flow): optimize flow submitter about exception handler (#2431)
  [krihy]
- Fix(notification): DLM events were missed when task framework not
  enabled (#2445) [LuckyLeo]
- Fix(database-permission): could not call inside dbms package in SQL
  console (#2417) [XiaoYang]
- Fix(schema-plugin): remove the logic that automatically converts table
  names to lowercase when getTable (#2298) [zhangxiao]
- Fix(schema-plugin):fix table ddl do not show unique index when table
  is partitioned (#2297) [zhangxiao]
- Fix(ticket): failed to set download log file url (#2405) [XiaoYang]
- Fix(data-transfer): fix incorrect task result update (#2403)
  [LuckyLeo]
- Fix(data-masking): unavailable when existing invalid database with
  duplicated name (#2355) [XiaoYang]
- Fix(db-browser): partition definitions is not ordered (#2328) [IL
  MARE]
- Fix(sql-execute): failed to kill query (#2259) [IL MARE]
- Fix(web-framework): swagger-ui.html page 404 notfound (#2160) [yizhou]

Security
~~~~~~~~
- Security: upgrade spring-security from 5.1.10 to 5.7.12, fix
  CVE-2024-22257. [yizhouxw]


v4.2.4_bp2 (2024-05-15)
-----------------------

New
~~~
- Feat(dlm): upgrade dlm sdk to 1.1.1 (#2281) [guowl3]
- Feat(connect): supports connect backup instance (#2192) [pynzzZ]

Changes
~~~~~~~
- Refactor(osc): modify i18n messages for white list (#2221) [krihy]

Fix
~~~
- Fix(database): database sync failed after updated an invalid
  datasource to a valid datasource (#2382) [pynzzZ, yh263208]
- Fix(encryption): RSA decrypting failed if already decrypted a invalid
  input string (#2389) [XiaoYang]
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
  service (DoS) risk (#2271) [XiaoYang]


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


