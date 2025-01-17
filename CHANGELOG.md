# OceanBase Developer Center (ODC) CHANGELOG

## 4.3.3 (2025-01-13)

### Feature Changes

**Data Lifecycle Management**

* Added archiving paths from Oracle to Object Storage.
* Added archiving paths from MySQL to Object Storage.
* Added archiving paths from OceanBase MySQL to Object Storage.
* Added archiving paths from OceanBase Oracle to Object Storage.
* Added archiving paths from PostgreSQL to Object Storage.
* Added support for editing history review with content comparison.
* Introduced dynamic target table definition to support storing historical data by day, month, or other time units.
* Added ability to delete archiving and cleanup tasks when completed or terminated.
* Optimized rollback logic to only revert data archived in the current task.

**Online Schema Change**

* Added retry mechanism with enhanced retry logic for various failure scenarios.
* Added status display for online schema changes, allowing progress tracking of running tasks.

**Change Risk Management**

* Introduced global project roles including Global Project Admin, Global Security Admin, and Global DBA.
* Added project archiving validation to check for unfinished tickets and periodic tasks before archiving.
* Added support for project deletion for archived projects.
* Implemented fine-grained view permission control with user-initiated permission requests.
* Expanded executable SQL types in SQL window to include `call`, `comment`, `set session`, and more.
* Extended SQL check support for native Oracle data sources.
* Added change approval workflow for native Oracle data sources.
* Added 2 new SQL check rules for standardizing `create like` and `create as` table creation statements.

**SQL Development**

* Added GUI support for OceanBase external tables.
* Added support for displaying secondary partitions in OceanBase partitioned tables.
* Added support for editing stored procedures in OceanBase MySQL mode.
* Added support for PL debugging via OBProxy.

**Other Enhancements**

* Added SAML single sign-on support.
* Added session kill capability for native Oracle data sources.
* Added compatibility for OceanBase V4.2.5 and V4.3.3.
* Added support for OBKV SQL mode.
* Enabled secure cookie mechanism for enhanced data transmission security.
* Added column width adjustment support for forms (ticket lists, database lists).

### Usability Improvements

* Added persistent project search criteria to reduce frequent searches.
* Maintained last used project context across user sessions.
* Standardized risk identification rule conditions using operators and English expressions to avoid ambiguity.
* Enhanced connection keep-alive logic with 3-minute database request intervals.
* Added project column in non-project ticket module for quick project identification.
* Extended "Create Again" functionality to all ticket types except logical database changes, partition plans, and shadow tables.
* Refined ticket management scope: admins and DBAs can manage all project tickets, while other roles can only manage self-initiated tickets. Also, all members can view tickets in their projects.

### Bug Fixes

**Data Sources**

* Fixed synchronization of system databases like `information_schema` to projects in bastion host integration scenarios.
* Resolved database synchronization suspension issues.

**Tickets**

* Fixed approval workflow triggering in personal workspace for data archiving tickets.
* Fixed status inconsistency in data archiving/cleanup task execution records.
* Fixed structure comparison task execution issues for non-current account users.
* Fixed Oracle table structure export failures with virtual columns.
* Fixed structure comparison failures for OceanBase MySQL tables with full-text index tokenizers.
* Fixed operation record viewing failures for periodic tasks with numerous subtasks.
* Fixed non-working configuration retention in export tasks.

**Change Management**

* Fixed unauthorized view exports.
* Fixed partition plan disable issues affecting project archiving.

**SQL Development**

* Fixed NPE exceptions in specific SQL check scenarios.
* Fixed PL drop requiring database change permissions.
* Fixed display issues for functions with year return type.
* Fixed PL creation and drop failures with @ in names.
* Fixed table detail viewing failures for Oracle tables with extended statistics (`DBMS_STATS.CREATE_EXTENDED_STATS`).
* Fixed ineffective row limit for Insert statements.
* Fixed null pointer exceptions when exporting array function result sets.
* Fixed missing run button for package subprocedures in Chrome 118.
* Fixed error when viewing subprocedures in package headers.

**Other**

* Fixed last used project not opening on subsequent ODC access.


## 4.3.2 (2024-09-27)

### Feature Changes

Logical Database Management

- Introduced logical database management, allowing the selection of physical databases to configure as a logical database. When selecting the first physical database, other databases are automatically matched.
- Automatically extract existing logical tables based on the topology distribution of physical tables in the physical database.
- Logical tables can now be created for various scenarios using expressions. The expressions support syntax for incrementing by steps, equal distribution, repetition, enumeration, and more, covering scenarios like database sharding, table sharding, and both.
- Added logical database DDL change tasks, where the change tasks are executed in parallel according to the distribution of physical databases. The change steps are executed statement-by-statement for easier exception handling.
- Supports consistency checks of logical table structures to detect inconsistencies in physical tables.
- Existing database- and table-level permission controls now apply to logical databases. When granted access to a logical database, permissions for the corresponding physical tables are automatically assigned.

Change Risk Management

- Added a SQL check rule for verifying the number of rows affected by SQL operations, supporting OceanBase MySQL and MySQL data sources.

Data Lifecycle Management

- Added data archiving links from Oracle to OceanBase Oracle.
- Added data archiving links from Oracle to Oracle.
- Added data archiving links from OceanBase Oracle to Oracle.
- Added data archiving links from PostgreSQL to OceanBase MySQL.
- Introduced Oracle data cleanup.
- Introduced PostgreSQL cleanup.

Import and Export

- The desktop version now supports importing files directly from a selected folder.

Partitioning Plan

- Added an option for partition naming where the partition name matches the range of partition content, specifically for date-type field partitions.

External Integration

- SSO integration now supports Azure AD.


### Usability Improvements

- Added a shortcut to return to the homepage within the SQL window.
- The targeted database in the SQL window will automatically appear in the visible area.
- Display DB execution time on the SQL result tab.
- Resource tree in the SQL window now sorts by name (data sources, projects, databases, tables, views, etc.).
- Right-click on a database in the SQL window's resource tree to quickly initiate any type of ticket.
- The data source details page now supports filtering the database list by availability and project assignment status.
- Enhanced the connection initialization script feature with guides for commonly used scripts.
- Unified empty state interactions across all features (data sources, projects, scripts, resource tree, etc.).
- Optimized data source selection interactions across all features (database maintenance, sensitive column scanning, adding databases to projects).
- Some forms now display content details on hover (ticket lists, database lists, user lists, etc.).
- Added a GitHub issue link on the feedback page.
- Updated user login password strength requirements: 8-32 characters in length, containing at least three of the following four character types: numbers (0-9), uppercase letters (A-Z), lowercase letters (a-z), and special characters (all English special characters).


### Bug Fixes

Change Management

- Fixed incorrect precision of `tinyint` column types (MySQL data source) in SQL generated by schema comparison tasks.
- Resolved an issue where partition names were misidentified as table names, leading to permission errors.
- Fixed a bug where `new_table` in `rename table old_table to new_table` was mistakenly flagged as needing `new_table` permissions.
- Addressed potential failures in data simulation tasks for MySQL 5.7.
- Resolved an issue where the manual execution button remained clickable after manual execution was triggered.
- Fixed failure in creating or updating rules when there were too many risk identification rules.
- Corrected the issue allowing scheduling intervals of less than ten minutes for periodic tasks.

Import and Export

- Fixed an issue where merging PL and table DDL into a single SQL file could cause import failures due to syntax errors.
- Resolved a bug where path issues could cause export failures on the Windows desktop version.
- Fixed an issue in OceanBase Oracle mode where exported result sets would be empty if table names were lowercase.

Data Archiving

- Resolved errors that occurred when modifying data archiving rate limiting parameters.

Data Sources

- Fixed an issue where testing an OceanBase SYS tenant data source incorrectly succeeded when OceanBase Oracle type was selected.

Others

- Resolved potential SSO login failures in environments with load balancing services.
- Fixed the issue where the startup parameter `ODC_APP_EXTRA_ARGS` was ineffective.


## 4.3.1 (2024-07-31)

### Feature Updates

Risk Control Changes

- Added table-level permission control, allowing project members to have different operation permissions on different tables, including query, change, and export operations, enhancing collaborative control capabilities.

Session Management

- Support for closing sessions/queries has been extended to more scenarios where OBServer is not directly connected.
    - When connected to OceanBase V4.2.3 and OBProxy V4.2.5 or higher versions, session management is performed using client session capability.
    - In OceanBase V4.2.1 Oracle mode, session management is handled using anonymous blocks.

OLAP Development

- Added real-time execution profiling, providing visual and interactive presentation of `sql_plan_monitor`.
    - This feature requires data source version OceanBase V4.2.4 or higher.
    - Supports analysis not only of completed SQL executions but also real-time analysis of ongoing SQL executions.
    - Provides graphical, tabular, and text views of execution plans, intuitively displaying operator connections and step sequences.
    - Global view offers sorting of Top 5 time-consuming operators and overall summary of execution phases, quickly pinpointing performance bottlenecks.
    - Operator nodes include execution status and detailed information such as CPU, memory, disk, output rows, and node attributes.
    - For parallel execution nodes, supports sorting by DB time, IO memory, and rows processed, quickly identifying data skew. Supports analysis of both standalone and distributed execution plans.
    - Newly designed integrated real-time diagnostic page for comprehensive query profiling combining execution plans and end-to-end trace diagnostics.

SQL Development

- During SQL execution, supports viewing execution progress, including total number of executions, current execution count, and trace ID of currently executing SQL. Real-time viewing of completed execution results is also supported.
- Supports graphical format viewing of OceanBase's logical SQL execution plan.

Data Sources

- Fully compatible with OceanBase V4.2.4, OceanBase V4.3.1, OceanBase V4.3.2.

### Usability Improvements

- Data delete tasks now support editing task configurations.
- Data source module supports batch import from MySQL, Oracle, and Doris data sources.

### Bug Fixes

Data Lifecycle Management

- Table structure comparison performed even when structure synchronization is not enabled. [#3014](https://github.com/oceanbase/odc/pull/3014)

Change Risk Control

- Automatic authorization rules did not take effect for LoginSuccess events. [#3003](https://github.com/oceanbase/odc/pull/3003)

Import/Export

- In the desktop mode, reinstalling ODC may lead to the unintended import of historical files generated during previous import tasks when initiating new tasks. [#3006](https://github.com/oceanbase/odc/pull/3006)

SQL Check

- When SQL window rules are enabled, commit and rollback buttons in SQL window may become ineffective. [#2985](https://github.com/oceanbase/odc/pull/2985)

SQL Development

- NPE may occur during PL debugging. [#2930](https://github.com/oceanbase/odc/pull/2930)
- SQL error when modifying session variables for Oracle data sources. [#2872](https://github.com/oceanbase/odc/pull/2872)

Mock Data

- Unable to terminate mock data tasks. [#2850](https://github.com/oceanbase/odc/pull/2850)

Global Object Retrieval

- Object synchronization cannot be stopped. [#2928](https://github.com/obase/odc/pull/2928)

Tickets

- Unable to retrieve SQL check results when check result file does not exist locally. [#2943](https://github.com/oceanbase/odc/pull/2943)

Auditing

- Actual length of `content_ip_address` column values exceeds the column length limit. [#2863](https://github.com/oceanbase/odc/pull/2863)

Other

- When deploying across multiple nodes using process mode to schedule tasks, all tasks may be scheduled to the same node. [#2408](https://github.com/oceanbase/odc/pull/2408)

## 4.3.0_bp1 (2024-06-24)

### Usability Improvements

- Optimize the description information of subtasks generated by multi-repository changes, highlighting the batch number and the corresponding multi-repository change tasks [#2762](https://github.com/oceanbase/odc/pull/2762)
- The creator of a scheduled tasks is allowed to alter that schedule task [#2772](https://github.com/oceanbase/odc/pull/2772)
- Message notification supports configuring network timeout [#2782](https://github.com/oceanbase/odc/pull/2782)

### Bug Fixes

SQL check

- Disable "id" as column name [#2796](https://github.com/oceanbase/odc/pull/2796)

Import Export

- Importing empty data files will cause the derivative task to fail [#2779](https://github.com/oceanbase/odc/pull/2779)

Database object management

- Opening a table object of OceanBase version 4.3.x using an ODC other than version 4.3.0 may cause a null pointer exception [#2776](https://github.com/oceanbase/odc/pull/2776)

SQL development

- A formatting error will occur when backslashes are included in the `like` and `replace` clauses of the SQL statement
- The SQL confirmation interface button does not take effect

Data Source

- When importing data sources in batches, all data sources to be imported cannot be displayed

## 4.3.0 (2024-06-07)


Table Objects

- Added support for OceanBase Oracle mode GIS data type
- Added support for OceanBase v4.3.0 columnar storage, including table storage mode and index storage mode 

Change Risk Control

- Added multi-database change tasks, which, compared to database change tasks, support configuring change pipelines when initiating tasks. The pipeline supports multiple batches, with each batch supporting multiple databases, and the pipeline can be saved as a change order template.
- Added database administrators. Project administrators can configure database administrators for the libraries within the project. Database administrators can be referenced in the ticket approval process and their information will also be included in WebHook events for external approval integration.
- Added support for configuring manual execution when initiating tickets, to prevent tasks approved for automatic execution from occurring at unexpected times. 

Data Archiving/Cleaning

- Data cleaning/archiving provides commonly used task metrics, including start time, end time, filtering conditions, processed rows, scanned rows, and real-time performance
- Data cleaning/archiving supports partition conditions
- Data cleaning/archiving supports configuring execution timeout. If the execution time exceeds the set timeout, the task will abort to ensure high-performance database during peak business periods.
- Data archiving supports incremental structural synchronization. When the structure of the source table changes, the task will automatically synchronize the table structure. When enabling structural synchronization, customized synchronization of partitions and indexes can be defined
- Data cleaning/archiving has implemented compatibility with OceanBase MySQL mode field types, adding support for additional field types: bit, set, enum, and spatial data types
- Data cleaning/archiving has implemented compatibility with OceanBase Oracle mode field types, adding support for additional field types: BINARY_FLOAT, BINARY_DOUBLE, TIMESTAMP WITH TIME ZONE, TIMESTAMP WITH LOCAL TIME ZONE, INTERVAL YEAR TO MONTH, INTERVAL DAY TO SECOND, ROW, ROWID, UROWID, and BLOB
- Data cleaning supports linkage historical library verification 

Global Object Search

- Added global object retrieval, supporting global search within the project scope, allowing instant access even with numerous library tables
- Supports almost all objects, including libraries, tables, columns, views, functions, stored procedures, program packages, triggers, etc.
- Supports the shortcut key Ctrl/Cmd+J for quick access to global object retrieval


### Usability Improvements

- JDBC parameters and initialization scripts in the data source configuration are also applied to import and export tasks, providing more flexibility for import and export tasks [#2587](https://github.com/oceanbase/odc/pull/2587)
- When archiving a project, it will check if all scheduled tasks are shut down [#2562](https://github.com/oceanbase/odc/pull/2562)
- Optimized the request time for querying table details [#2626](https://github.com/oceanbase/odc/pull/2626)
- Optimized the error message for canceling process tasks [#2624](https://github.com/oceanbase/odc/pull/2624)

### Bug Fixes

Data Source

- Failed to query table structure when connecting to a backup cluster [#2648](https://github.com/oceanbase/odc/pull/2648)
- Concurrency exception when resetting connections [#2528](https://github.com/oceanbase/odc/pull/2528)

PL Object Management

- Fixed stored procedures and functions list not sorted by name [#2636](https://github.com/oceanbase/odc/pull/2636)
- Issue with batch compilation failure in OceanBase Oracle mode [#2606](https://github.com/oceanbase/odc/pull/2606)

SQL Window

- Unable to switch databases when there are more than 2000 in the SQL window [#2520](https://github.com/oceanbase/odc/pull/2520)
- The setting to not continue execution upon execution failure in SQL window does not take effect [#2259](https://github.com/oceanbase/odc/pull/2259)
- Error in SQL window execution after closing the connection with obclient [#2528](https://github.com/oceanbase/odc/pull/2528)
- Unable to set NLS parameters in SQL window for ORACLE data sources [#2501](https://github.com/oceanbase/odc/pull/2501)

Data Archiving/Cleanup

- Message notification becomes ineffective when the task framework is closed [#2445](https://github.com/oceanbase/odc/pull/2445)

Projects and Tickets

- Error reported when editing a project that already exists [#2642](https://github.com/oceanbase/odc/pull/2642)
- Internationalization of ticket descriptions becomes ineffective [#2579](https://github.com/oceanbase/odc/pull/2579)
- Approval failure when the approval content is too long #2565 [#2565](https://github.com/oceanbase/odc/pull/2565)

Structure Comparison

- Anomalies in structure comparison results when the target table does not exist [#2638](https://github.com/oceanbase/odc/pull/2638)

Import/Export

- The setting to skip when importing table structures does not take effect [#2587](https://github.com/oceanbase/odc/pull/2587)

Table Object Management

- Error reporting table object does not exist when OceanBase tenant configures lower_case_table_names=2 [#2298](https://github.com/oceanbase/odc/pull/2298)
- Unique indexes of partitioned tables are not visible in table object information [#2297](https://github.com/oceanbase/odc/pull/2297)

Other

Issue with accessing swagger-ui.html [#2160](https://github.com/oceanbase/odc/pull/2160)

### Security

- Upgraded spring-security component to version 5.7.12 [#2690](https://github.com/oceanbase/odc/pull/2690)



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
