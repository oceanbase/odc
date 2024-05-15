## 4.2.4_bp2 (2024-05-14)

### Bug fixes

Data source

- Unable to connect to ODP-Sharding data source [#2339](https://github.com/oceanbase/odc/pull/2339)

User management

- Unable to delete OWNER or DBA user of archived project [#2359](https://github.com/oceanbase/odc/pull/2359)

Shadow table synchronization

- Syntax errors may occur in statements generated when a column has a default value [#2388](https://github.com/oceanbase/odc/pull/2388)

Data security

- When there is an invalid database with the same name, desensitization fails [#2385](https://github.com/oceanbase/odc/pull/2385)

Command line window

- When entering a long SQL statement, the statement cannot be fully echoed [#2353](https://github.com/oceanbase/odc/pull/2353)

Database archiving/cleaning

- The status may be incorrectly set to success when a task is terminated [#2340](https://github.com/oceanbase/odc/pull/2340)
- If the table names are different between the source and target when configuring data archiving tasks, an error occurs stating that the table does not exist [#2313](https://github.com/oceanbase/odc/pull/2313)

Result set export

- When database access is slow, exporting the result set fails due to timeout [#2315](https://github.com/oceanbase/odc/pull/2315)

Notification

- Editing the notification channel will cause the signing key to be lost [#2314](https://github.com/oceanbase/odc/pull/2314)

Partitioning plan

- Partitions are missing and partitions are not created in OceanBase 4.x version [#2327](https://github.com/oceanbase/odc/pull/2327)
- Unable to initiate partitioning plan tasks in versions below OceanBase 3.x [#2323](https://github.com/oceanbase/odc/pull/2323)
- Partition intervals are not shown in partitioning plan details

System integration

- In third-party user integration, users who modify extra_properties will no longer be able to log in to ODC [#2336](https://github.com/oceanbase/odc/pull/2336)
- SQL console cannot be opened when using OAuth and bastion host integration at the same time [#2253](https://github.com/oceanbase/odc/pull/2253)

Other

- Tickets whose life cycle spans the release process cannot be promoted normally [#2065](https://github.com/oceanbase/odc/pull/2065)
- Unable to execute or terminate tasks manually [#2272](https://github.com/oceanbase/odc/pull/2272)
- After running for a period of time, you cannot use the account password to log in and you need to restart ODC Server to recover [#2389](https://github.com/oceanbase/odc/pull/2389)
- Conflicts occur between Basic Authentication and CSRF prevention [#2370](https://github.com/oceanbase/odc/pull/2370)

### Usability improvements

- Prompts are added for partitioning plans when no partition creation/deletion statements are generated [#2351](https://github.com/oceanbase/odc/pull/2351)
- OceanBase versions before 4.2 disable end-to-end trace diagnostics [#2219](https://github.com/oceanbase/odc/pull/2219)

### Dependency library upgrade

- Upgrade data-lifecycle-manager version to 1.1.1 [#2281](https://github.com/oceanbase/odc/pull/2281)

### Security

- Remove snappy-java dependency [#2317](https://github.com/oceanbase/odc/pull/2317)
- Data desensitization increases verification to avoid DDoS risks caused by BigDecimal [#2271](https://github.com/oceanbase/odc/pull/2271)

## 4.2.4_bp1 (2024-04-12)

### Bug fixes

PL debugging

- PL debugging timeout parameter cannot be set via connection initialization script [#2179](https://github.com/oceanbase/odc/pull/2179)

Other

- ODC Server fails to start when there is a historical task of partitioning plan [#2158](https://github.com/oceanbase/odc/pull/2158)

### Security

- Upgrade okio-jvm version to 3.4.0 [#2200](https://github.com/oceanbase/odc/pull/2200)

## 4.2.4 (2024-04-03)

### Feature Changes

Data Sources

- Supported OceanBase v4.2.2
- New Oracle data source with support for SQL development, import/export, data masking, object management, change approval
- New Doris data source with support for SQL development, import/export, data masking, table object management, session management, command-line window, change approval

SQL Development

- 71 common O&M snippets built-in, which can auto-complete in the SQL window and will match database type and version
- SQL auto-completion supports completing data dictionary/performance view
- Case sensitivity for schema names, table names, and column names in Oracle mode now consistent with PL/SQL Developer behavior

Schema Comparison

- New Schema Comparison feature added, supporting Schema Comparisons for homogenous databases (OBOracle, OBMySQL, MySQL)
- Supported scope includes table objects, with comparison properties including columns, primary keys, constraints, indexes, partitions, table properties
- Schema Comparison results provide DIFF preview and change script preview
- SQL preview results are downloadable and can directly initiate schema synchronization tasks

Online Schema Changes

- Support for adding intermediate columns to tables
- Support for concurrent index changes during table schema modifications (OceanBase Oracle mode)
- Support for primary key deletion when the table contains unique constraints (OceanBase Oracle mode)

Partitioning Plan

- Support for configuring partitioning plan for databases in OBOracle mode
- Partition field types not only support DATE/TIMESTAMP but also NUMBER/CHAR/VARCHAR2 and other field types
- Redesigned the partitioning plan strategy configuration page, custom mode can configure any partition upper boundary calculation logic through expressions, support for previewing the execution SQL of the partitioning plan
- Execution cycle for deleting partitions can be independently configured from the creation partition cycle

Data Archiving/Cleaning

- Support for configuring cleaning tasks for databases in OBOracle mode
- Support for configuring archiving tasks from OBOracle to OBOracle
- Support for previewing the actual SQL being executed for data archival/cleaning tasks when initiating the task
- Data archiving supports custom target table names
- Data cleaning supports configuring whether to use primary keys. When primary keys are not used, data deletion will directly match the deletion criteria based on indexes, and the task execution process does not need to shard tasks based on primary keys. This can significantly improve the efficiency of cleaning, especially in specific scenarios.

Security Standards

- Support for Custom Environment
    - Customize SQL window standards and SQL check rules for different business scenarios
    - When creating a custom environment, choose an existing environment for initial configuration
    - Support for configuring tag styles to easily distinguish between different environments
- Three new SQL check rules added
    - Reserved words should not be used as object names
    - Existence of offline (table lock) schema change statements, offline schema changes will result in table locking, impacting business operations
    - Existence of TRUNCATE statements, high risk of TRUNCATE tables in production environments
- Default values of risk identification rules in security standards optimized, production environments can be used out of the box
- SQL window standard adds risk tips for enabling PL debugging in production environments

Database-level Access Control

- Project collaboration adds access control for databases
- Types of database access permissions include query, export, and modification. It supports granting permissions based on types and setting the validity period of permissions
- Project developers have default access to all databases within the project, consistent with previous versions
- Added the role of project participant, with participants by default having no access to any databases
- Participants can apply for permissions to access databases through tickets
- Administrators can directly authorize participants with permissions to access databases
- Administrators can revoke permissions to access databases from participants

Notifications Messages

- Project collaboration adds message notification feature
- Supported event types include ticket approval status change, task execution status change, and task scheduling failures
- The scope of notifications can be configured through rules, messages can be configured through templates
- The notification channel supports configuring commonly used webhook channels, such as DingTalk, Lark, and WeCom. It also supports custom HTTP requests and setting limits on message sending

System Integration

- SSO supports LDAP protocol

### Usability Improvements

- Optimized the database selection component, standardizing the database selection interaction across product pages and adding fuzzy matching functionality for project names, data source names, and database names.
- Added a resource tree locator key to the SQL window for quickly identifying the current database's position within the resource tree.
- Upgraded preference settings to a top-level feature accessible directly through [Settings], new configuration options include:
    - Whether to enable end-to-end tracing during SQL execution.
    - Whether to continue SQL execution upon encountering errors.
    - Customization of editor theme and font size.
    - Configuration of editor shortcuts for running SQL commands.
    - Setting the default workspace.
    - Whether to enable user behavior analytics.
    - Desktop version now supports memory size configuration through JVM parameters.
- Added database availability indicators; the database list under projects will now show unavailable statuses and reasons.
- Improved the initiation interaction for tickets:
    - Support for initiating various tickets directly from the database in the resource tree.
    - Commonly used tickets (mock data, database changes, data archiving, data cleaning, database permission application) support reinitiation with editable task parameters.
- Enhanced database change processes to detect index change statements and automatically adjust the timeout setting (120h) to prevent index change statement execution failure due to timeout.
- Desktop version personal settings now support custom JVM configuration with memory usage control to within 1 GB.
- Desktop version supports exporting data files larger than 4 GB.
- Optimized the en-US language wording of the product.

### Bug fixes

Connection Session

- Failure to promptly clear references after connection session expiration, leading to resource leaks and potential memory consumption increases [#2125](https://github.com/oceanbase/odc/pull/2125)
- In high-frequency usage scenarios, executing SQL queries or viewing table data may encounter interface freezes and unresponsive behavior [#1914](https://github.com/oceanbase/odc/pull/1914)
- After modifying the username case sensitivity in data source configuration, connecting to OceanBase Oracle may result in errors [#1797](https://github.com/oceanbase/odc/pull/1797)
- Occasional occurrence of 404 errors when opening the SQL console [#1809](https://github.com/oceanbase/odc/pull/1809)

SQL Execution

- In OceanBase v4.2, the status of the submit/rollback button is not synchronized with the actual transaction status [#2097](https://github.com/oceanbase/odc/pull/2097)
- SQL statements with single-line comments fail to execute [#2085](https://github.com/oceanbase/odc/pull/2085)
- The absence of a delimiter at the end of the last SQL command leads to incorrect offset calculation [#1970](https://github.com/oceanbase/odc/pull/1970)
- Incompatibility of anonymous block execution module with Oracle 11g [#1759](https://github.com/oceanbase/odc/pull/1759)
- Incorrect output in DBMS execution, spaces are not fully output [#1051](https://github.com/oceanbase/odc/issues/1970)
- `#` and `$` disappear after SQL window formatting [#1490](https://github.com/oceanbase/odc/issues/1490)
- Auto-complete is not available in the SQL window for the MySQL data source [#1718](https://github.com/oceanbase/odc/issues/1718)

Result-set

- Modifying multiple rows of data simultaneously in the result set takes a long time [#2007](https://github.com/oceanbase/odc/pull/2007)
- Precision loss occurs when displaying DATETIME data type in OceanBase MySQL mode [#1996](https://github.com/oceanbase/odc/pull/1996)
- Switching back and forth between viewing BLOB field text and hexadecimal images may cause interface freezing [#300](https://github.com/oceanbase/odc/issues/300)

Table Objects

- The column order in index and constraint views is inconsistent. [#1948](https://github.com/oceanbase/odc/pull/1948)
- Unable to view table details for MySQL v5.6. [#1635](https://github.com/oceanbase/odc/pull/1635)
- Unable to view table details for Sofa ODP. [#2043](https://github.com/oceanbase/odc/pull/2043)
- Cannot change NOT NULL fields to NULL in table schema editing. [#1441](https://github.com/oceanbase/odc/issues/1441)
- When a partitioned table has multiple maximum values, only one maximum value is displayed. [#1501](https://github.com/oceanbase/odc/issues/1501)
- The button to delete table primary keys is grayed out and cannot be clicked. [#1874](https://github.com/oceanbase/odc/issues/1874)

Import/Export

- Task exception caused by incorrect SQL statement splitting when comments contain `;`. [#417](https://github.com/oceanbase/odc/issues/417)
- Export task fails when type names are lowercase. [#631](https://github.com/oceanbase/odc/issues/631)
- Export failure when exporting trigger objects. [#750](https://github.com/oceanbase/odc/issues/750)
- Export task fails when function names contain special characters. [#1331](https://github.com/oceanbase/odc/issues/1331)
- When exporting indexes in Oracle mode, the index name is prefixed with the database name. [#1491](https://github.com/oceanbase/odc/issues/1491)
- When exporting the schema of stored procedures, the `DELIMITER $$` delimiter is concatenated with the table name. [#1746](https://github.com/oceanbase/odc/issues/1746)
- After creating an export task, terminating the export task will still display the task status as executed successfully. [#1752](https://github.com/oceanbase/odc/issues/1752)
- When exporting packages, the object type is not displayed as the package body in task details. [#1755](https://github.com/oceanbase/odc/issues/1755)
- Import fails when importing CSV files containing DATE types. [#2079](https://github.com/oceanbase/odc/issues/2079)

Online Schema Change

- OSC task experiencing syntax exceptions when input statements contain comments [#1597](https://github.com/oceanbase/odc/pull/1597)

Projects and tickets

- Incorrect prompt message for successful task creation, changed to "Ticket Created Successfully" [#1320](https://github.com/oceanbase/odc/issues/1320)
- Dropdown switching project page crashes under SQL window [#1512](https://github.com/oceanbase/odc/issues/1512)

Database Changes

- When the rollback content is an attachment, it is not displayed during the rollback process [#1379](https://github.com/oceanbase/odc/issues/1379)

SQL Check

- Null Pointer Exception occurring when virtual columns are present [#2031](https://github.com/oceanbase/odc/pull/2031)
- Drop operations for primary keys as constraints going undetected [#1879](https://github.com/oceanbase/odc/pull/1879)
- Null Pointer Exception triggered by certain ALTER statements [#1865](https://github.com/oceanbase/odc/pull/1865)

SQL Plan

- Clicking to terminate SQL plan is ineffective [#1528](https://github.com/oceanbase/odc/issues/1528)
- Ticket status displays as pre-check failed when pre-check fails [#218](https://github.com/oceanbase/odc/issues/218)

Partitioning Plan

- Partition DDL execution failures when schema or table names are lowercase [#2088](https://github.com/oceanbase/odc/pull/2088)

Data Archiving/Cleaning

- When tasks exit, they do not release the database connection pool, which can occupy many threads when there is a large number of tasks.
- The connection pool will indefinitely retry and generate a large number of logs when failing to acquire connections.
- Slow SQL is generated when cleaning up using unique keys due to not using the correct index.

Users and Permissions

- Inefficiencies in batch importing users with associated roles [#1908](https://github.com/oceanbase/odc/pull/1908)

Data Security

- Data masking inconsistencies when employing nested CASE-WHEN clauses [#1410](https://github.com/oceanbase/odc/pull/1410)

System Integration

- Presence of garbled code when including Chinese content in request body [#1625](https://github.com/oceanbase/odc/pull/1625)

DB Browser

- Creation type not recognized for table statements containing indexes [#2063](https://github.com/oceanbase/odc/pull/2063)
- DDL containing extraneous spaces before and after in Oracle mode [#2050](https://github.com/oceanbase/odc/pull/2050)
- Tables with default values on columns not retrievable in Oracle 11g mode [#1733](https://github.com/oceanbase/odc/pull/1733)
- listTables not correctly returning tables for the specified schema consistently [#1632](https://github.com/oceanbase/odc/pull/1632)
- listTables failed in OceanBase versions < v2.2.30 [#1478](https://github.com/oceanbase/odc/pull/1478)
- Inadequate visualization for MySQL table schema, specifically for strings in single quotes [#1401](https://github.com/oceanbase/odc/pull/1401)

OB SQL Parser

- Parsing issues for INSERT statements into tables named 'json_table' [#1968](https://github.com/oceanbase/odc/pull/1968)

## 4.2.3_bp1 (2024-02-01)

### Feature

- database-change: database change task adapt streaming read sql file [#1437](https://github.com/oceanbase/odc/pull/1437)
- dlm: supports sharding using unique indexes [#1327](https://github.com/oceanbase/odc/pull/1327)

### Bug fixes

sql-execute

- fail to execute statement on OceanBase 2.2.30 [#1487](https://github.com/oceanbase/odc/pull/1487)
- executing anonymous block causes NPE in the team space [#1474](https://github.com/oceanbase/odc/pull/1474)
- do not roll back execute when manual commit enabled[#1468](https://github.com/oceanbase/odc/pull/1468)
- can not set a delimiter longer than 2 [#1414](https://github.com/oceanbase/odc/pull/1414)
- during SQL window query request, the front end crashes when logging out.

result-set

- when there are multiple columns in the result set, it cannot be selected after sliding and the front end crashes occasionally at random points.
- after filtering the result set, there is no content in the column mode.

table

- query table data with no column comments [#1488](https://github.com/oceanbase/odc/pull/1488)

data-export

- object types are not displayed when exporting package bodies and synonyms. [#1464](https://github.com/oceanbase/odc/pull/1464)

flow

- NPE when creating a ticket without connection information [#1479](https://github.com/oceanbase/odc/pull/1479)
- can not set task status correctly when creating task concurrently [#1419](https://github.com/oceanbase/odc/pull/1419)
- when the task creator and approver are not the current users, an error occurs when viewing the task approval node.

osc

- osc job query connection config by id throw Access Denied[#1378](https://github.com/oceanbase/odc/pull/1378)
- osc task don't show manual swap table name when full migrate is completed [#1357](https://github.com/oceanbase/odc/pull/1357)

database-change

- query task details throw flow instance not found exception [#1325](https://github.com/oceanbase/odc/pull/1325)
- query task details throw file not found exception [#1316](https://github.com/oceanbase/odc/pull/1316)

partition-plan

- delete job failed if the associated trigger does not exist [#1495](https://github.com/oceanbase/odc/pull/1495)

dlm

- the data cleaning task scheduling failed after editing the rate limit configuration [#1438](https://github.com/oceanbase/odc/pull/1438)
- the task log file does not exist [#1376](https://github.com/oceanbase/odc/pull/1376)

desktop version

- Ubuntu desktop version cannot open command line window.

others

- sql-check: failed to check statement when connect to a lower case schema for OBOracle [#1341](https://github.com/oceanbase/odc/pull/1341)
- audit: executing sql with rare words failed when metadb's default character is gbk [#1486](https://github.com/oceanbase/odc/pull/1486)

### Security

- upgrade aliyun-oss-sdk version [#1393](https://github.com/oceanbase/odc/pull/1393)
- osc: horizontal overstep access data permission when swap table manual [#1405](https://github.com/oceanbase/odc/pull/1405)

## 4.2.3 (2023-12-22)

### Feature

Data source

- Allow data sources to be bound to projects
- Supports OceanBase Sharding MySQL data source
- Supports cloning data sources
- Supports displaying data source status in the object tree in the team space

Import and Export

- Supports import and export of native MySQL data sources
- SYS account configuration is no longer provided in the import and export task configuration page of OceanBase data source

Database object management

- OceanBase MySQL and native MySQL mode supports GIS data types
- White screen gives high-risk operation prompts when creating or deleting indexes

Project

- Added 2 built-in project roles: security administrator, participant; security administrator is allowed to manage sensitive columns of the project and participate in approval, and participants are allowed to participate in approval
- Allow users to apply for project permissions
- Prohibit deletion of users who are assigned to any project with the DBA or project OWNER role

SQL-Check

- Optimized SQL interception interaction
- Added problem locating function to supports quick locating of specific problems in the original SQL

Connection session

- Added an automatic reconnection mechanism to avoid errors and usability issues caused by session destruction when not used for a long time

Partitioning Plan

- Supports scheduled scheduling

SQL execution

- SQL fragment supports a maximum content size of 65535
- Supports Ctrl+Enter shortcut key to execute the current statement

Bastion integration

- Supports SQL Check

DLM

- Supports log viewing
- Added three new task parameter configurations: query timeout, shard size, and shard strategy
- Optimized the performance of MYSQL 5.6
- Optimized the performance of OceanBase data cleaning

Full link trace

- Supports exporting Jaeger-compatible JSON files
- Optimized visual effects
- Added list view of results, supporting search and sorting

Tickets

- The project administrator can view all tickets under the project, and other roles can view the tickets they have approved

### Bug fixes

Data source

- Users can still create data sources when they leave the project and do not belong to any project or role
- The "Execution Time" column in the database session in OceanBase MySQL and native MySQL modes is 0
- Modification of the time output format through the session variable management function in OceanBase Oracle mode does not take effect in the SQL execution window
- Lowercase schema cannot be connected in OceanBase Oracle mode
- Unable to connect to MySQL data source built by percona branch

SQL execution

- Error thrown during SQL execution without internationalization
- SQL with dblink cannot be executed in the team space
- The desc statement cannot be executed in the team space if security rules allow it
- ORDER BY will be invalid when executing SELECT... ORDER BY 1 style statements in OceanBase Oracle mode
- Disabling the "SQL types allowed to be executed in the SQL window" rule does not take effect

Database object management

- In OceanBase MySQL mode, the names of table partitions and other objects displayed on the left object tree are surrounded by backticks

Result set export

- There is no log printing for tasks
- No data after exporting in excel format

PL object

- During interactive function creation, the return value of the sys_refcursor type cannot be defined through the drop-down menu
- The PL parameter value in OceanBase MySQL mode does not escape single quotes

DLM

- The database connection pool is too small, causing task execution failure

Partitioning Plan

- Task creation failed in MySQL mode of OceanBase version 1.4.79
- Tables that do not set a partition strategy will still perform partitioning plan changes

SQL-Check

- The `alter table xxx drop index` statement cannot be recognized as a DROP INDEX statement

External approval integration

- Unrecognized expression for data in indexed collection
- The data in xml form returned by the external system will lose the root tag of the original xml during deserialization

data masking

- When duplicate columns are scanned, adding sensitive columns will fail

Project

- After the user is granted "Personal Space" permission, he must log in again for it to take effect
- Transaction timeout occurred when synchronizing a large number of databases or schemas to the project
- Unable to filter tickets by project dimension
- Project OWNER can remove all users with DBA roles in the project

Bastion integration

- Inactive connections are not cleaned up

Recycle bin

- Unable to delete specific objects in Recycle Bin

Mock data

- Task takes up too much memory
- Does not support ZHSGB232 encoding
- OceanBase MySQL and native MySQL mode cannot generate tasks for bit types with a width below 8
- Unable to skip auto-incremented primary key columns
- The bit type width displays incorrectly in OceanBase MySQL and native MySQL modes

Database change task

- Memory overflow error occurs when uploading large files

Full link trace

- The memory overflow problem caused by adding the driver to the full-link diagnosis

Shadow table sync

- After the ticket is approved or rejected, the approver cannot view the task details

obclient integration

- Repeated creation of operating system users with the same name results in error reports

Tickets

- Creating a ticket takes too long
- There is a "Pending Approval" ticket for another project in the "Pending Approval" ticket list

Operation record

- The "Data Source" column in the operation record is empty
- SQL execution events are not logged
- Open SQL window event is not logged

### Improve

- Improve SQL execution performance and reduce unnecessary time-consuming operations
- Allow users to configure the maximum number of retries and account lockout time in the event of login failure
- Only allow users to modify table data with primary key constraints, unique key constraints and rowid with a blank screen
- Optimize the error text when synchronizing database errors

### Dependency database upgrade

- Upgrade obclient version to 2.2.4
- Upgrade spring security version to 5.7.10
- Upgrade hutool version to 5.8.23
- Upgrade pf4j version to 3.10.0
- Upgrade netty version to 4.1.94.FINAL

## 4.2.2 bp (2023-11-23)

### Bug fixes

Data archiving

- After the data archiving subtask starts running, updating the current limiter configuration cannot take effect.
- Data cleaning task is not running

data masking

- Entering malicious identification rules in the scenario of automatic scanning of sensitive columns will result in a denial of service by regular expressions

SQL execution

- Display NUMBER type data in scientific notation in MySQL mode

PL run

- Unable to view the contents of the cursor

SQL-Check

- When the database does not report syntax errors, SQL-Check will still prompt syntax errors.

## 4.2.2 (2023-11-07)

### Feature

Data Source

- Support MySQL data source
- Adapted to OceanBase 4.2.0/4.2.1
- Data sources add initialization scripts and customized JDBC connection parameter settings

data masking

- Support view desensitization

DLM

- Support OceanBase 4.x version
- Support MySQL to OceanBase link
- Support white screen current limiting configuration
- Support breakpoint recovery

Import and Export

- Supports import and export of type objects

### Improve

- Optimized object management performance in large-scale table scenarios, and used ob-sql-parser to parse index/constraint metadata
- Optimized database object tree interaction, the project and data source selection interaction area is folded to the top, and the database list is displayed more clearly
- Optimized the interaction between creating a new SQL window and switching databases in the SQL window. Switching databases is faster, and the SQL window adds a copy operation.
- Optimized the data masking configuration interaction, making it more convenient to select sensitive columns
- Optimized the problem of slow retrieval of data source list and slow retrieval of data source status in scenarios where there are a large number of data sources.
- Optimize the error text when running PL with wrong parameters

### Bug fixes

PL Debugging

- Unable to jump into sub-stored procedures/functions defined in the package during debugging

SQL Execution

- Continuous execution of "DROP PACKAGE" statements during SQL execution resulted in a large number of error reports
- When connecting to the OceanBase MySQL tenant, the "obodc_procedure_feature_test" stored procedure is automatically called, resulting in an error or slow connection.
- The sum of the time consumption of each sub-item in SQL execution time-consuming statistics is not equal to the parent item
- In the SQL execution time-consuming statistics, "SQL pre-check" and "SQL post-check" lack detailed sub-item time-consuming statistics

SQL-Check

- When creating type under the OceanBase Oracle tenant, if the sub-stored procedure/function has no parameter list, SQL Check will report a syntax error.
- Unable to turn off "Syntax Error" rule

data masking

- Desensitization fails when the SELECT statement contains multiple table JOINs
- Sensitive columns cannot be recognized in the case-sensitive OceanBase MySQL mode, causing desensitization to fail.

Database Object Management

- Users without the show create view permission will receive an error when viewing view details.
- The length of all character types cannot be displayed when viewing table objects
- Failed to view details of scheduled database change tasks with automatic rollback nodes in personal space

Database Changes

- The database change task timeout setting is invalid

Import and Export

- The exported package in Oracle mode does not contain the package body
- Unable to export result set containing tab characters to Excel

Operational audit

- Changes to "SQL Check Specification" and "SQL Window Specification" are not included in the operational audit scope

### Dependency database upgrade

- Upgrade ob-loader-dumper version to 4.2.5-RELEASE
- Upgrade oceanbase-client version to 2.4.5

### Security reinforcement

- Transmission of sensitive fields on the front and back ends uses asymmetric encryption

## 4.2.1 (2023-09-25)

### Bug fixes

SQL Execution

- Unable to print DBMS output when OceanBase is older than 4.0
- DML statement generation is slow when editing result sets

Import & Export

- Import/export objects are not displayed in task details during task execution

PL Debugging

- When OceanBase is deployed on multiple nodes, PL debugging occasionally fails to connect to the database
- Obtaining database connection errors when debugging anonymous blocks under OceanBase Oracle mode lowercase schema

Datasource management

- An error occurs when the flashback statement generated by the recycle bin module for the index is executed
- The session management interface cannot display the SQL being executed by the session
- A null pointer exception occurs when there are empty rows or columns in the template file during batch import connections

data masking

- When the OceanBase MySQL schema is configured as case-sensitive, sensitive columns cannot be case-sensitive

ticket management

- After the ticket is submitted, the ticket status remains "queued" for a long time and is not updated and the ticket does not report an error

Third party integration

- When the approval flow does not contain an external approval node, it will also try to obtain the ID of the external approval ticket

SQL-Check

- OceanBase Oracle mode cannot detect whether table or column comments exist

### Security reinforcement

- Address possible SSRF attacks during third-party integration
