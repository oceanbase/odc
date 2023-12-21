# OceanBase Developer Center (ODC) CHANGELOG

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

Partition plan

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

- The project administrator can view all work orders under the project, and other roles can view the work orders they have approved

### Fix

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

Partition plan

- Task creation failed in MySQL mode of OceanBase version 1.4.79
- Tables that do not set a partition strategy will still perform partition plan changes

SQL-Check

- The `alter table xxx drop index` statement cannot be recognized as a DROP INDEX statement

External approval integration

- Unrecognized expression for data in indexed collection
- The data in xml form returned by the external system will lose the root tag of the original xml during deserialization

Data desensitization

- When duplicate columns are scanned, adding sensitive columns will fail

Project

- After the user is granted "Personal Space" permission, he must log in again for it to take effect
- Transaction timeout occurred when synchronizing a large number of databases or schemas to the project
- Unable to filter work orders by project dimension
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

- After the work order is approved or rejected, the approver cannot view the task details

obclient integration

- Repeated creation of operating system users with the same name results in error reports

Tickets

- Creating a work order takes too long
- There is a "Pending Approval" work order for another project in the "Pending Approval" work order list

Operation record

- The "Data Source" column in the operation record is empty
- SQL execution events are not logged
- Open SQL window event is not logged

### Improve

- Improve SQL execution performance and reduce unnecessary time-consuming operations
- Allow users to configure the maximum number of retries and account lockout time in the event of login failure
- Only allow users to modify table data with primary key constraints, unique key constraints and rowid with a blank screen
- Optimize the error text when synchronizing database errors

### Dependency library upgrade

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

Data desensitization

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

Data Desensitization

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
- Optimized the data desensitization configuration interaction, making it more convenient to select sensitive columns
- Optimized the problem of slow retrieval of data source list and slow retrieval of data source status in scenarios where there are a large number of data sources.
- Optimize the error text when running PL with wrong parameters

### Fix

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

Data Desensitization

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

### Dependency library upgrade

- Upgrade ob-loader-dumper version to 4.2.5-RELEASE
- Upgrade oceanbase-client version to 2.4.5

### Security reinforcement

- Transmission of sensitive fields on the front and back ends uses asymmetric encryption

## 4.2.1 (2023-09-25)

### Fix

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

Data desensitization

- When the OceanBase MySQL schema is configured as case-sensitive, sensitive columns cannot be case-sensitive

Work order management

- After the work order is submitted, the work order status remains "queued" for a long time and is not updated and the work order does not report an error

Third party integration

- When the approval flow does not contain an external approval node, it will also try to obtain the ID of the external approval work order

SQL-Check

- OceanBase Oracle mode cannot detect whether table or column comments exist

### Security reinforcement

- Address possible SSRF attacks during third-party integration
