--
-- 完整 PL 包参考文档：https://docs.oracle.com/database/121/ARPLS/d_resmgr.htm
--
CREATE OR REPLACE PACKAGE dbms_resource_manager AUTHID CURRENT_USER AS

  -- Attribute Names
  ORACLE_USER       CONSTANT STRING      :=     'ORACLE_USER';
  ORACLE_FUNCTION   CONSTANT STRING      :=     'ORACLE_FUNCTION';

--
-- create a resource plan
--
  PROCEDURE create_plan(
    plan    IN VARCHAR2,
    comment IN VARCHAR2 DEFAULT ''
  );

--
-- delete resource plan
--
  PROCEDURE delete_plan(
    plan    IN VARCHAR2
  );

--
-- create consumer group
--
  PROCEDURE CREATE_CONSUMER_GROUP (
    consumer_group  IN VARCHAR2,
    comment         IN VARCHAR2 DEFAULT NULL
  );

--
-- delete consumer group
--
  PROCEDURE DELETE_CONSUMER_GROUP (
    consumer_group IN VARCHAR2
  );

--
-- create plan directive
--
  PROCEDURE create_plan_directive(
    plan              IN VARCHAR2,
    group_or_subplan  IN VARCHAR2,
    comment           IN VARCHAR2 DEFAULT '',
    mgmt_p1           IN INT DEFAULT 100,
    utilization_limit IN INT DEFAULT 100
  );

--
-- update plan directive
--
  PROCEDURE update_plan_directive(
    plan                  IN VARCHAR2,
    group_or_subplan      IN VARCHAR2,
    new_comment           IN VARCHAR2 DEFAULT NULL,
    new_mgmt_p1           IN INT DEFAULT NULL,
    new_utilization_limit IN INT DEFAULT NULL
  );

--
-- delete plan directive
--
  PROCEDURE DELETE_PLAN_DIRECTIVE (
    plan              IN VARCHAR2,
    group_or_subplan  IN VARCHAR2
  );

--
-- set consumer group mapping rule
--
  PROCEDURE SET_CONSUMER_GROUP_MAPPING (
    attribute IN VARCHAR2,
    value IN VARCHAR2,
    consumer_group IN VARCHAR2 DEFAULT NULL
  );



END dbms_resource_manager;
//

