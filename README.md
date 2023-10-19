![image.png](docs/en-US/images/odc-head.png)

English | [中文](./README-zh-CN.md)

# OceanBase Developer Center (ODC)

OceanBase Developer Center, or ODC for short, is an open-source, all-encompassing tool for collaborative database development and data management across various scenarios. By streamlining collaborative efforts, ODC addresses the challenges of risk management during database changes, efficient data management, and data security concerns.
![image](https://github.com/oceanbase/odc/assets/15030999/cef8937e-904f-43a0-b5c6-22ebae10905a)


## 🤔 What ODC Offers and Why Choose ODC?

### Effortless SQL Development, Anytime, Anywhere

- Utilizing modern web architecture, ODC provides unparalleled accessibility to your databases, allowing you to perform SQL development tasks on the go, right from your web browser.
- ODC boasts a comprehensive and user-friendly SQL development environment that encompasses all the features you'd expect from desktop development tools, and even offers support for PL debugging.
![image](https://github.com/oceanbase/odc/assets/15030999/1d9e32a6-f359-4286-b0be-82cf514c0883)


### Guard Every Change in Your SQL Development Journey 

- Throughout the entire SQL development process, ODC is your steadfast companion, offering risk control at every turn. From visual object management to SQL queries, data editing, and import/export tasks, ODC comes equipped with built-in risk management tools.
- ODC introduces project-based collaboration and change approval workflows, complemented by SQL validation rules, SQL window guidelines, and risk level identification.
![image](https://github.com/oceanbase/odc/assets/15030999/6d90509b-91cb-465a-ac75-1604f96c9ffd)


### Automated Data Lifecycle Management

- ODC facilitates data retention policies, enabling you to effortlessly purge or archive data based on specified time frames, and build a hot-cold data separation system tailored to your needs in just 5 minutes.
- Beyond time-based data handling, ODC supports batch processing based on partitioning, allowing you to efficiently manage large datasets.
- With ODC's SQL scheduled tasks, completing computational tasks becomes a breeze, rendering traditional CRONTAB systems obsolete.
![image](https://github.com/oceanbase/odc/assets/15030999/c95b57fd-3d49-4e41-96f9-32f26cb2e2d3)


### Sensitive Data Protection in Every Scenario 

- ODC's data desensitization capabilities extend to both static and dynamic scenarios, ensuring that sensitive information remains safeguarded during schema changes, SQL queries, result set exports, and data extractions.
- Security administrators can configure sensitive data rules and desensitization algorithms, ensuring that both DBAs and developers are unable to access sensitive data.
![image](https://github.com/oceanbase/odc/assets/15030999/56606949-e198-4d80-a484-778e78454d65)


### Seamless Integration Without Additional Code Development 

- Integrate ODC seamlessly into your current database development workflow without the need for disruptive changes.
- From Single Sign-On (SSO) to approval integration, SQL review integration, bastion host integration, and audit integration, ODC offers a comprehensive suite of features that cater to enterprise control needs.
![image](https://github.com/oceanbase/odc/assets/15030999/1d76fec9-1820-4526-964c-3cc8affb0215)


## 🌟 Features at a Glance


ODC boasts a wealth of powerful features, encompassing two core aspects: SQL development, and control management and collaboration. Let's delve into the core functionalities in detail.


### SQL Development

- Multi-Source Compatibility: Seamlessly supports OceanBase MySQL/Oracle mode, ODP Sharing, MySQL, and more.
- Database Objects: Visually manage various database objects such as tables, views, sequences, synonyms, triggers, stored procedures, functions, packages, and types.
- SQL Execution: Effortlessly edit and execute SQL queries, leverage command-line windows, scripts, and code snippets.
- Data Viewing and Editing: Allows for data viewing and editing, and interoperability with Excel on result sets.
- Monitoring and Diagnostics: Access execution plans, profiling, and database trace for comprehensive monitoring and diagnostics.
- Data Generation: Generate test and scenario-specific data.
- Import and Export: Manage data with features like schema import/export, data import/export, and import/export of the entire database.
- PL Lifecycle: Execute, compile, and debug PL objects.
- Database Administration: Manage sessions, variables, the recycle bin, and privileges (upcoming feature).


### Control Management and Collaboration

- Privilege Management: Manage users and define custom roles based on the RBAC model.
- Team Collaboration: Facilitate multi-role collaboration among administrators, DBAs, and developers within projects, offering database-level access control.
- Change Control: Customize approval workflows and implement SQL checks based on syntax rules.
- Stable Changes: Execute lock-free schema and data changes (upcoming feature).
- Data Lifecycle Management: Automate data cleansing, archiving, partitioning, and scheduling SQL tasks.
- Data Security: Manage sensitive column data, export anonymized data, and control sensitive column access.
- Compliance Auditing: Embrace robust auditing capabilities, including operational and SQL audit integration.
- Collaborative Efficiency: Configure batch import settings, automate authorization rules, and stay informed with the notification center.
- System Integration: Leverage OAuth2 and OIDC account integration, bastion host integration, approval integration, and SQL review integration.
- Learning and Exploration: Gain access to lab resources, tutorials, courses, and code repositories (upcoming feature).


## 🚀 Installation and Deployment 

ODC comes in two distinct forms: the dynamic Web version and the efficient Desktop version. Let's take a closer look at these offerings:

- **Desktop Version**: Effortless Individual Development 

    The Desktop version is designed for personal development scenarios, offering an efficient and user-friendly SQL development tool. Supported operating systems include Windows and MacOS.

- **Web Version**: Unleash Collaborative Power 

    The Web version caters to multi-person collaborative development scenarios, encompassing collaborative development, risk control, data security, and more. It serves as an integrated platform for database development, management, and control. The Web version provides various installation mediums, such as rpm and Docker images, supporting x86 and ARM architectures. It also encapsulates all the features of SQL development in the Desktop version.

### Deploying the Web Version

Please note that the deployment steps outlined in this section are meant for experiencing the functionality. For production environment deployment, please refer to the [Deployment Guide](https://github.com/oceanbase/odc-doc/blob/V4.2.0/en-US/1100.deployment-guide/100.deployment-overview.md).


#### Before You Start

The ODC Web version is packaged as a Docker image and relies on an OceanBase MySQL tenant as MetaDB. If you have not had a MetaDB, please follow **(Optional) Step 1: Create a MetaDB** below for instructions.

Before you start, ensure that your environment meets the following requirements:

- Docker is installed and the service is up and running. It is recommended to use the latest version of Docker.
- The deployment machine for ODC has at least 2 available CPU cores and 4 GB of RAM.
- The MetaDB requires a minimum specification of 1 vCPU and 4 GB of RAM.


#### (Optional) Step 1: Create a MetaDB

Use the sample script below to create a MetaDB if you do not already have one. To create a MetaDB, it would require an OceanBase cluster and a test tenant, which the script would also complete for you. The creation process may take about 2 minutes.

Please note that the deployment machine for the OceanBase cluster should have at least 4 vCPUs and 8 GB of RAM.

This script is for demo purposes only. If you have already had a MetaDB, please skip this step.

```shell
# Start an OceanBase cluster, which would automatically create a test tenant.
docker run -p 2881:2881 --name oceanbase-ce -d oceanbase/oceanbase-ce

# Connect to the test tenant within the cluster.
docker exec -it oceanbase-ce ob-mysql root

# Create a database user 'odc' and schema 'odc_metadb' within the tenant. Replace <password> with the actual password of your choice.
CREATE USER odc IDENTIFIED BY '<password>';
CREATE DATABASE odc_metadb;
GRANT ALL ON odc_metadb.* TO odc;
```


#### Step 2: Launch the ODC Server

Use the script below to launch the ODC Docker container.

```shell
# Launch ODC Server. The following example limits the container to 2 CPU cores and 4 GB of memory.

# Replace <your_metadb_password> with the actual password of your MetaDB. If you have followed Step 1 above to create the MetaDB, then replace <your_metadb_password> with the password you set in Step 1.

# Set the initial password for your admin account of ODC using the parameter <your_admin_password>. This password would be the one you use to log into the ODC Web. The password must satisfy the following requirements:
# - At least 2 digits
# - At least 2 lowercase letters
# - At least 2 uppercase letters
# - At least 2 special characters: ._+@#$%
# - No spaces or other special characters
# - 8~32 characters in length

# The process to launch ODC may take about 2 minutes.

 docker run -d -it --name odc --network host \
 --cpu-period 100000 --cpu-quota 200000 --memory=4G \
 -e "DATABASE_HOST=127.0.0.1" -e "DATABASE_PORT=2881" -e "DATABASE_NAME=odc_metadb" \
 -e "DATABASE_USERNAME=odc@test" -e "DATABASE_PASSWORD=<your_metadb_password>" \
 -e "ODC_ADMIN_INITIAL_PASSWORD=<your_admin_password>" \
 -e "ODC_SERVER_PORT=8989" \
 oceanbase/odc:latest
```
#### What to do next

After deploying ODC Web, you can follow our [Quick Start guide](https://github.com/oceanbase/odc-doc/blob/V4.2.0/en-US/300.quickstart/200.web-odc-quickstart/3.quickstart-using-web-odc.md) to start your journey with ODC.

### Installing the Desktop Version

The Desktop version harnesses Electron technology to transform the web application into a desktop application, offering compatibility with multiple desktop operating systems. Installing the desktop application is straightforward: simply download the version-specific installer and double-click to initiate the installation process.

ODC Desktop version employs the h2database embedded database as MetaDB, eliminating the need for MetaDB database configuration.

Here are the download links for ODC Desktop version installers:

- Windows 64-bit version: [odc-win64-with-jre.exe](https://obodc-front.oss-cn-beijing.aliyuncs.com/ODC%204.2.0/OceanBase%20Developer%20Center%20Setup%204.2.0%20win64jre.exe)
- MacOS version: [odc-macos-with-jre.dmg](https://obodc-front.oss-cn-beijing.aliyuncs.com/ODC%204.2.0/OceanBase%20Developer%20Center-4.2.0jre.dmg)
- Fedora/Ubuntu version: [odc-ubuntu.deb](https://obodc-front.oss-cn-beijing.aliyuncs.com/ODC%204.2.0/OceanBase%20Developer%20Center%20Setup%204.2.0%20amd64.deb)

#### What to do next

After installing the Desktop version, you can follow our [Quick Start guide](https://github.com/oceanbase/odc-doc/blob/V4.2.0/en-US/300.quickstart/100.client-odc-quickstart/3.quickstart-using-client-odc.md) to start your journey with ODC.


## 🤝 Join the Contributing Community

ODC envisions an open community, collaboratively crafting a database development and control management tool. We welcome your contributions in any form:

- Report bugs through [Issues](https://github.com/oceanbase/odc/issues).
- Participate in or initiate discussions via [Discussion](https://github.com/oceanbase/odc/discussions).
- Contribute bug fixes or new features through [Pull requests](https://github.com/oceanbase/odc/pulls).
- Share ODC to your friends and colleagues, and help expand the ODC community's influence.

For detailed guidelines on contributing, please refer to the [Contribution Guide](docs/en-US/CONTRIBUTION.md). For comprehensive guidance on different types of code changes via pull requests, consult the [Pull requests](https://github.com/oceanbase/odc/pulls).


## 🛤️ Roadmap Ahead 

Here's a glimpse into ODC's 2023 roadmap.

| Focus Area     | Q1                                         | Q2                                                         | Q3                                            | Q4                                        |
|--------|--------------------------------------------|------------------------------------------------------------|-----------------------------------------------|-------------------------------------------|
| SQL Development | OceanBase 4.0/4.1 SSL connection</br>Enhanced performance for large-scale table/column scenarios | Enhanced PL execution and debugging support</br>Database trace</br>PL/SQLDeveloper compatibility</br>Oracle-compatible time and numeric formats | Support for MySQL data sources</br>Common operation command assistant</br>Desktop resource optimization                 | Oracle data source support</br>Parallel execution plan visualization</br>OceanBase AP functionality adaptation |
| Change Risk Control | Custom roles                                      | Project-based control collaboration</br>SQL checks and guidelines</br>Risk level identification</br>Automatic rollback scripts for data changes               | Lock-free schema changes</br>Best practice templates for SQL checks                           | Flow-based SQL checks</br>Schema comparison and synchronization                    |
| Data Lifecycle | SQL scheduled tasks</br>Automated partition management                            | Data archiving/cleaning</br>Support for OceanBase MySQL mode                              | Lock-free data changes</br>Data archiving/cleaning</br>Support for MySQL and OceanBase Oracle modes | Row-level recycle bin</br>Logical backup and restore</br>Cloud storage backup and restore support                    |
| Security and Compliance  |                                            | Dynamic data anonymization</br>Expanded operation audit coverage                                          | Database/table-level access control</br>Automatic identification of sensitive columns                             | Sensitive data protection compliance certification</br>Database SQL auditing                   |
| Collaborative Efficiency   | Automatic authorization rules</br>Batch user/connection configuration                         | SSO integration</br>External approval integration</br>SQL review integration                                    | Open API</br>Bastion host integration                                | Database grouping and logical databases</br>Multi-table support for test data generation</br>Global metadata indexing for multiple data sources          |


## License

ODC is licensed under the [Apache-2.0](https://www.apache.org/licenses/LICENSE-2.0) license.


## Help and Support

We welcome you to raise any questions or provide feedback through GitHub Issues. If you have trouble accessing GitHub, you can also join our DingTalk Group to seek assistance there.

* [GitHub Issues](https://github.com/oceanbase/odc/issues)
* [Discord Server](https://discord.gg/drPUb2kq)
* [ODC DingTalk Group](https://qr.dingtalk.com/action/joingroup?code=v1,k1,OacNAwktCbVGqRk2jQ0TDga6j6AHtXtvU7ZrD6Orah0=&_dt_no_comment=1&origin=11)
