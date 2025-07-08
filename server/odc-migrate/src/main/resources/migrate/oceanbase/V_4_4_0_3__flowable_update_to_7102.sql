/*
 * Copyright (c) 2025 OceanBase.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
 * Copyright (c) 2025 OceanBase.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

-- common
-- 6400 -> 6410
create table if not exists ACT_RU_ENTITYLINK (
                                 ID_ varchar(64),
                                 REV_ integer,
                                 CREATE_TIME_ datetime(3),
                                 LINK_TYPE_ varchar(255),
                                 SCOPE_ID_ varchar(255),
                                 SCOPE_TYPE_ varchar(255),
                                 SCOPE_DEFINITION_ID_ varchar(255),
                                 REF_SCOPE_ID_ varchar(255),
                                 REF_SCOPE_TYPE_ varchar(255),
                                 REF_SCOPE_DEFINITION_ID_ varchar(255),
                                 primary key (ID_)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE utf8_bin;

create index if not exists ACT_IDX_ENT_LNK_SCOPE on ACT_RU_ENTITYLINK(SCOPE_ID_, SCOPE_TYPE_, LINK_TYPE_);
create index if not exists ACT_IDX_ENT_LNK_SCOPE_DEF on ACT_RU_ENTITYLINK(SCOPE_DEFINITION_ID_, SCOPE_TYPE_, LINK_TYPE_);

create table if not exists ACT_HI_ENTITYLINK (
                                 ID_ varchar(64),
                                 LINK_TYPE_ varchar(255),
                                 CREATE_TIME_ datetime(3),
                                 SCOPE_ID_ varchar(255),
                                 SCOPE_TYPE_ varchar(255),
                                 SCOPE_DEFINITION_ID_ varchar(255),
                                 REF_SCOPE_ID_ varchar(255),
                                 REF_SCOPE_TYPE_ varchar(255),
                                 REF_SCOPE_DEFINITION_ID_ varchar(255),
                                 primary key (ID_)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE utf8_bin;


create index if not exists ACT_IDX_HI_ENT_LNK_SCOPE on ACT_HI_ENTITYLINK(SCOPE_ID_, SCOPE_TYPE_, LINK_TYPE_);
create index if not exists ACT_IDX_HI_ENT_LNK_SCOPE_DEF on ACT_HI_ENTITYLINK(SCOPE_DEFINITION_ID_, SCOPE_TYPE_, LINK_TYPE_);



-- 6410 -> 6411
CALL AddColumnIfNotExists('ACT_RU_ENTITYLINK', 'HIERARCHY_TYPE_', 'VARCHAR(255)');
CALL AddColumnIfNotExists('ACT_HI_ENTITYLINK', 'HIERARCHY_TYPE_', 'VARCHAR(255)');



-- 6411 -> 6412
update ACT_RU_IDENTITYLINK set SCOPE_DEFINITION_ID_ = null where SCOPE_ID_ is not null and SCOPE_DEFINITION_ID_ is not null;

-- 6412 -> 6413
create table if not exists ACT_HI_TSK_LOG (
                              ID_ bigint auto_increment,
                              TYPE_ varchar(64),
                              TASK_ID_ varchar(64) not null,
                              TIME_STAMP_ timestamp(3) not null,
                              USER_ID_ varchar(255),
                              DATA_ varchar(4000),
                              EXECUTION_ID_ varchar(64),
                              PROC_INST_ID_ varchar(64),
                              PROC_DEF_ID_ varchar(64),
                              SCOPE_ID_ varchar(255),
                              SCOPE_DEFINITION_ID_ varchar(255),
                              SUB_SCOPE_ID_ varchar(255),
                              SCOPE_TYPE_ varchar(255),
                              TENANT_ID_ varchar(255) default '',
                              primary key (ID_)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE utf8_bin;


-- 6413 -> 6500
CALL AddColumnIfNotExists('ACT_RU_EVENT_SUBSCR', 'SUB_SCOPE_ID_', 'VARCHAR(64)');
CALL AddColumnIfNotExists('ACT_RU_EVENT_SUBSCR', 'SCOPE_ID_', 'VARCHAR(64)');
CALL AddColumnIfNotExists('ACT_RU_EVENT_SUBSCR', 'SCOPE_DEFINITION_ID_', 'VARCHAR(64)');
CALL AddColumnIfNotExists('ACT_RU_EVENT_SUBSCR', 'SCOPE_TYPE_', 'VARCHAR(64)');


-- 6500 -> 6501
CALL AddColumnIfNotExists('ACT_RU_JOB', 'ELEMENT_ID_', 'VARCHAR(255)');
CALL AddColumnIfNotExists('ACT_RU_JOB', 'ELEMENT_NAME_', 'VARCHAR(255)');

CALL AddColumnIfNotExists('ACT_RU_TIMER_JOB', 'ELEMENT_ID_', 'VARCHAR(255)');
CALL AddColumnIfNotExists('ACT_RU_TIMER_JOB', 'ELEMENT_NAME_', 'VARCHAR(255)');


CALL AddColumnIfNotExists('ACT_RU_SUSPENDED_JOB', 'ELEMENT_ID_', 'VARCHAR(255)');
CALL AddColumnIfNotExists('ACT_RU_SUSPENDED_JOB', 'ELEMENT_NAME_', 'VARCHAR(255)');

CALL AddColumnIfNotExists('ACT_RU_DEADLETTER_JOB', 'ELEMENT_ID_', 'VARCHAR(255)');
CALL AddColumnIfNotExists('ACT_RU_DEADLETTER_JOB', 'ELEMENT_NAME_', 'VARCHAR(255)');



-- 6501->6502
create table if not exists FLW_RU_BATCH (
                            ID_ varchar(64) not null,
                            REV_ integer,
                            TYPE_ varchar(64) not null,
                            SEARCH_KEY_ varchar(255),
                            SEARCH_KEY2_ varchar(255),
                            CREATE_TIME_ datetime(3) not null,
                            COMPLETE_TIME_ datetime(3),
                            STATUS_ varchar(255),
                            BATCH_DOC_ID_ varchar(64),
                            TENANT_ID_ varchar(255) default '',
                            primary key (ID_)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE utf8_bin;

create table if not exists FLW_RU_BATCH_PART (
                                 ID_ varchar(64) not null,
                                 REV_ integer,
                                 BATCH_ID_ varchar(64),
                                 TYPE_ varchar(64) not null,
                                 SCOPE_ID_ varchar(64),
                                 SUB_SCOPE_ID_ varchar(64),
                                 SCOPE_TYPE_ varchar(64),
                                 SEARCH_KEY_ varchar(255),
                                 SEARCH_KEY2_ varchar(255),
                                 CREATE_TIME_ datetime(3) not null,
                                 COMPLETE_TIME_ datetime(3),
                                 STATUS_ varchar(255),
                                 RESULT_DOC_ID_ varchar(64),
                                 TENANT_ID_ varchar(255) default '',
                                 primary key (ID_)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE utf8_bin;

create index if not exists FLW_IDX_BATCH_PART on FLW_RU_BATCH_PART(BATCH_ID_);

CALL AddForeignKeyIfNotExists(
    'FLW_RU_BATCH_PART',         -- 表名
    'FLW_FK_BATCH_PART_PARENT',  -- 外键约束名
    'BATCH_ID_',                 -- 外键列名
    'FLW_RU_BATCH',              -- 被引用表名
    'ID_'                        -- 被引用列名
);


-- 6502 -> 6503
CALL AddColumnIfNotExists('ACT_RU_IDENTITYLINK', 'SUB_SCOPE_ID_', 'VARCHAR(255)');


create index if not exists ACT_IDX_IDENT_LNK_SUB_SCOPE on ACT_RU_IDENTITYLINK(SUB_SCOPE_ID_, SCOPE_TYPE_);

CALL AddColumnIfNotExists('ACT_HI_IDENTITYLINK', 'SUB_SCOPE_ID_', 'VARCHAR(255)');

create index if not exists ACT_IDX_HI_IDENT_LNK_SUB_SCOPE on ACT_HI_IDENTITYLINK(SUB_SCOPE_ID_, SCOPE_TYPE_);

-- 6504-> 6505
CALL AddColumnIfNotExists('ACT_RU_TASK', 'PROPAGATED_STAGE_INST_ID_', 'VARCHAR(255)');
CALL AddColumnIfNotExists('ACT_HI_TASKINST', 'PROPAGATED_STAGE_INST_ID_', 'VARCHAR(255)');


-- 6510 -> 6511
CALL AddColumnIfNotExists('ACT_RU_ENTITYLINK', 'ROOT_SCOPE_ID_', 'VARCHAR(255)');
CALL AddColumnIfNotExists('ACT_RU_ENTITYLINK', 'ROOT_SCOPE_TYPE_', 'VARCHAR(255)');


create index if not exists ACT_IDX_ENT_LNK_ROOT_SCOPE on ACT_RU_ENTITYLINK(ROOT_SCOPE_ID_, ROOT_SCOPE_TYPE_, LINK_TYPE_);

CALL AddColumnIfNotExists('ACT_HI_ENTITYLINK', 'ROOT_SCOPE_ID_', 'VARCHAR(255)');
CALL AddColumnIfNotExists('ACT_HI_ENTITYLINK', 'ROOT_SCOPE_TYPE_', 'VARCHAR(255)');

create index if not exists ACT_IDX_HI_ENT_LNK_ROOT_SCOPE on ACT_HI_ENTITYLINK(ROOT_SCOPE_ID_, ROOT_SCOPE_TYPE_, LINK_TYPE_);


-- 6511 -> 6512
CALL AddColumnIfNotExists('ACT_RU_JOB', 'CATEGORY_', 'VARCHAR(255)');
CALL AddColumnIfNotExists('ACT_RU_TIMER_JOB', 'CATEGORY_', 'VARCHAR(255)');
CALL AddColumnIfNotExists('ACT_RU_SUSPENDED_JOB', 'CATEGORY_', 'VARCHAR(255)');
CALL AddColumnIfNotExists('ACT_RU_DEADLETTER_JOB', 'CATEGORY_', 'VARCHAR(255)');



-- 6512 -> 6513
create table if not exists ACT_RU_EXTERNAL_JOB (
                                   ID_ varchar(64) NOT NULL,
                                   REV_ integer,
                                   CATEGORY_ varchar(255),
                                   TYPE_ varchar(255) NOT NULL,
                                   LOCK_EXP_TIME_ timestamp(3) NULL,
                                   LOCK_OWNER_ varchar(255),
                                   EXCLUSIVE_ boolean,
                                   EXECUTION_ID_ varchar(64),
                                   PROCESS_INSTANCE_ID_ varchar(64),
                                   PROC_DEF_ID_ varchar(64),
                                   ELEMENT_ID_ varchar(255),
                                   ELEMENT_NAME_ varchar(255),
                                   SCOPE_ID_ varchar(255),
                                   SUB_SCOPE_ID_ varchar(255),
                                   SCOPE_TYPE_ varchar(255),
                                   SCOPE_DEFINITION_ID_ varchar(255),
                                   RETRIES_ integer,
                                   EXCEPTION_STACK_ID_ varchar(64),
                                   EXCEPTION_MSG_ varchar(4000),
                                   DUEDATE_ timestamp(3) NULL,
                                   REPEAT_ varchar(255),
                                   HANDLER_TYPE_ varchar(255),
                                   HANDLER_CFG_ varchar(4000),
                                   CUSTOM_VALUES_ID_ varchar(64),
                                   CREATE_TIME_ timestamp(3) NULL,
                                   TENANT_ID_ varchar(255) default '',
                                   primary key (ID_)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE utf8_bin;

create index if not exists ACT_IDX_EXTERNAL_JOB_EXCEPTION_STACK_ID on ACT_RU_EXTERNAL_JOB(EXCEPTION_STACK_ID_);
create index if not exists ACT_IDX_EXTERNAL_JOB_CUSTOM_VALUES_ID on ACT_RU_EXTERNAL_JOB(CUSTOM_VALUES_ID_);


CALL AddForeignKeyIfNotExists(
    'ACT_RU_EXTERNAL_JOB',         -- 表名
    'ACT_FK_EXTERNAL_JOB_EXCEPTION',  -- 外键约束名
    'EXCEPTION_STACK_ID_',                 -- 外键列名
    'ACT_GE_BYTEARRAY',              -- 被引用表名
    'ID_'                        -- 被引用列名
);


CALL AddForeignKeyIfNotExists(
    'ACT_RU_EXTERNAL_JOB',         -- 表名
    'ACT_FK_EXTERNAL_JOB_CUSTOM_VALUES',  -- 外键约束名
    'CUSTOM_VALUES_ID_',                 -- 外键列名
    'ACT_GE_BYTEARRAY',              -- 被引用表名
    'ID_'                        -- 被引用列名
);

create index if not exists ACT_IDX_EJOB_SCOPE on ACT_RU_EXTERNAL_JOB(SCOPE_ID_, SCOPE_TYPE_);
create index if not exists ACT_IDX_EJOB_SUB_SCOPE on ACT_RU_EXTERNAL_JOB(SUB_SCOPE_ID_, SCOPE_TYPE_);
create index if not exists ACT_IDX_EJOB_SCOPE_DEF on ACT_RU_EXTERNAL_JOB(SCOPE_DEFINITION_ID_, SCOPE_TYPE_);


-- 6513 -> 6514
CALL AddColumnIfNotExists('ACT_RU_JOB', 'CORRELATION_ID_', 'VARCHAR(255)');
CALL AddColumnIfNotExists('ACT_RU_TIMER_JOB', 'CORRELATION_ID_', 'VARCHAR(255)');
CALL AddColumnIfNotExists('ACT_RU_SUSPENDED_JOB', 'CORRELATION_ID_', 'VARCHAR(255)');
CALL AddColumnIfNotExists('ACT_RU_DEADLETTER_JOB', 'CORRELATION_ID_', 'VARCHAR(255)');
CALL AddColumnIfNotExists('ACT_RU_EXTERNAL_JOB', 'CORRELATION_ID_', 'VARCHAR(255)');


create index if not exists ACT_IDX_JOB_CORRELATION_ID on ACT_RU_JOB(CORRELATION_ID_);
create index if not exists ACT_IDX_TIMER_JOB_CORRELATION_ID on ACT_RU_TIMER_JOB(CORRELATION_ID_);
create index if not exists ACT_IDX_SUSPENDED_JOB_CORRELATION_ID on ACT_RU_SUSPENDED_JOB(CORRELATION_ID_);
create index if not exists ACT_IDX_DEADLETTER_JOB_CORRELATION_ID on ACT_RU_DEADLETTER_JOB(CORRELATION_ID_);
create index if not exists ACT_IDX_EXTERNAL_JOB_CORRELATION_ID on ACT_RU_EXTERNAL_JOB(CORRELATION_ID_);

-- 6515->6516
CALL AddColumnIfNotExists('ACT_RU_ENTITYLINK', 'SUB_SCOPE_ID_', 'VARCHAR(255)');
CALL AddColumnIfNotExists('ACT_RU_ENTITYLINK', 'PARENT_ELEMENT_ID_', 'VARCHAR(255)');

CALL AddColumnIfNotExists('ACT_HI_ENTITYLINK', 'SUB_SCOPE_ID_', 'VARCHAR(255)');
CALL AddColumnIfNotExists('ACT_HI_ENTITYLINK', 'PARENT_ELEMENT_ID_', 'VARCHAR(255)');


-- 6600 -> 6601
create index if not exists ACT_IDX_ENT_LNK_REF_SCOPE on ACT_RU_ENTITYLINK(REF_SCOPE_ID_, REF_SCOPE_TYPE_, LINK_TYPE_);

create index if not exists ACT_IDX_HI_ENT_LNK_REF_SCOPE on ACT_HI_ENTITYLINK(REF_SCOPE_ID_, REF_SCOPE_TYPE_, LINK_TYPE_);

-- 6601 -> 6610
create index if not exists ACT_IDX_TIMER_JOB_DUEDATE on ACT_RU_TIMER_JOB(DUEDATE_);

-- 6720 -> 6721
create index if not exists ACT_IDX_EVENT_SUBSCR_SCOPEREF_ on ACT_RU_EVENT_SUBSCR(SCOPE_ID_, SCOPE_TYPE_);

-- 6721 -> 6722
CALL AddColumnIfNotExists('ACT_RU_EVENT_SUBSCR', 'LOCK_TIME_', 'timestamp(3)');
CALL AddColumnIfNotExists('ACT_RU_EVENT_SUBSCR', 'LOCK_OWNER_', 'varchar(255)');

-- 6800 -> 6810
CALL AddColumnIfNotExists('ACT_RU_VARIABLE', 'META_INFO_', 'varchar(4000)');
CALL AddColumnIfNotExists('ACT_HI_VARINST', 'META_INFO_', 'varchar(4000)');

-- 7000 -> 7010

CALL AddColumnIfNotExists('ACT_RU_TASK', 'STATE_', 'varchar(255)');
CALL AddColumnIfNotExists('ACT_RU_TASK', 'IN_PROGRESS_TIME_', 'datetime(3)');
CALL AddColumnIfNotExists('ACT_RU_TASK', 'IN_PROGRESS_STARTED_BY_', 'varchar(255)');
CALL AddColumnIfNotExists('ACT_RU_TASK', 'CLAIMED_BY_', 'varchar(255)');
CALL AddColumnIfNotExists('ACT_RU_TASK', 'SUSPENDED_TIME_', 'datetime(3)');
CALL AddColumnIfNotExists('ACT_RU_TASK', 'IN_PROGRESS_DUE_DATE_', 'datetime(3)');
CALL AddColumnIfNotExists('ACT_RU_TASK', 'SUSPENDED_BY_', 'varchar(255)');



CALL AddColumnIfNotExists('ACT_HI_TASKINST', 'STATE_', 'varchar(255)');
CALL AddColumnIfNotExists('ACT_HI_TASKINST', 'IN_PROGRESS_TIME_', 'datetime(3)');
CALL AddColumnIfNotExists('ACT_HI_TASKINST', 'IN_PROGRESS_STARTED_BY_', 'varchar(255)');
CALL AddColumnIfNotExists('ACT_HI_TASKINST', 'CLAIMED_BY_', 'varchar(255)');
CALL AddColumnIfNotExists('ACT_HI_TASKINST', 'SUSPENDED_TIME_', 'datetime(3)');
CALL AddColumnIfNotExists('ACT_HI_TASKINST', 'IN_PROGRESS_DUE_DATE_', 'datetime(3)');
CALL AddColumnIfNotExists('ACT_HI_TASKINST', 'SUSPENDED_BY_', 'varchar(255)');
CALL AddColumnIfNotExists('ACT_HI_TASKINST', 'COMPLETED_BY_', 'varchar(255)');




-- 7010 -> 7011
CALL AddColumnIfNotExists('ACT_RU_EVENT_SUBSCR', 'SCOPE_DEFINITION_KEY_', 'varchar(255)');

-- 7100 -> 7101
delete from ACT_GE_PROPERTY where NAME_ = 'batch.schema.version';
delete from ACT_GE_PROPERTY where NAME_ = 'entitylink.schema.version';
delete from ACT_GE_PROPERTY where NAME_ = 'eventsubscription.schema.version';
delete from ACT_GE_PROPERTY where NAME_ = 'identitylink.schema.version';
delete from ACT_GE_PROPERTY where NAME_ = 'job.schema.version';
delete from ACT_GE_PROPERTY where NAME_ = 'task.schema.version';
delete from ACT_GE_PROPERTY where NAME_ = 'variable.schema.version';

-- 7101 -> 7102
create index if not exists ACT_IDX_EVENT_SUBSCR_EXEC_ID on ACT_RU_EVENT_SUBSCR(EXECUTION_ID_);
create index if not exists ACT_IDX_EVENT_SUBSCR_PROC_ID on ACT_RU_EVENT_SUBSCR(PROC_INST_ID_);
update ACT_GE_PROPERTY set VALUE_ = '7.1.0.2' where NAME_ = 'common.schema.version';


-- engine
-- 6411 -> 6412
create table if not exists ACT_RU_ACTINST (
                              ID_ varchar(64) not null,
                              REV_ integer default 1,
                              PROC_DEF_ID_ varchar(64) not null,
                              PROC_INST_ID_ varchar(64) not null,
                              EXECUTION_ID_ varchar(64) not null,
                              ACT_ID_ varchar(255) not null,
                              TASK_ID_ varchar(64),
                              CALL_PROC_INST_ID_ varchar(64),
                              ACT_NAME_ varchar(255),
                              ACT_TYPE_ varchar(255) not null,
                              ASSIGNEE_ varchar(255),
                              START_TIME_ datetime(3) not null,
                              END_TIME_ datetime(3),
                              DURATION_ bigint,
                              DELETE_REASON_ varchar(4000),
                              TENANT_ID_ varchar(255) default '',
                              primary key (ID_)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE utf8_bin;

create index if not exists ACT_IDX_RU_ACTI_START on ACT_RU_ACTINST(START_TIME_);
create index if not exists ACT_IDX_RU_ACTI_END on ACT_RU_ACTINST(END_TIME_);
create index if not exists ACT_IDX_RU_ACTI_PROC on ACT_RU_ACTINST(PROC_INST_ID_);
create index if not exists ACT_IDX_RU_ACTI_PROC_ACT on ACT_RU_ACTINST(PROC_INST_ID_, ACT_ID_);
create index if not exists ACT_IDX_RU_ACTI_EXEC on ACT_RU_ACTINST(EXECUTION_ID_);
create index if not exists ACT_IDX_RU_ACTI_EXEC_ACT on ACT_RU_ACTINST(EXECUTION_ID_, ACT_ID_);

-- 6412 -> 6413
delete
from ACT_RU_VARIABLE
where TYPE_ in ('item', 'message')
  and BYTEARRAY_ID_ is null
  and DOUBLE_ is null
  and LONG_ is null
  and TEXT_ is null
  and TEXT2_ is null;

delete
from ACT_RU_VARIABLE
where TYPE_ = 'null'
  and NAME_ in ('org.activiti.engine.impl.bpmn.CURRENT_MESSAGE', 'org.flowable.engine.impl.bpmn.CURRENT_MESSAGE');


-- 6503 -> 6504
CALL AddColumnIfNotExists('ACT_RU_EXECUTION', 'REFERENCE_ID_', 'varchar(255)');
CALL AddColumnIfNotExists('ACT_RU_EXECUTION', 'REFERENCE_TYPE_', 'varchar(255)');

-- 6504 -> 6505
CALL AddColumnIfNotExists('ACT_RU_EXECUTION', 'PROPAGATED_STAGE_INST_ID_', 'varchar(255)');

-- 6505 -> 6506
CALL AddColumnIfNotExists('ACT_HI_PROCINST', 'REFERENCE_ID_', 'varchar(255)');
CALL AddColumnIfNotExists('ACT_HI_PROCINST', 'REFERENCE_TYPE_', 'varchar(255)');

-- 6512 -> 6513
CALL AddColumnIfNotExists('ACT_RU_EXECUTION', 'LOCK_OWNER_', 'varchar(255)');
CALL AddColumnIfNotExists('ACT_RU_EXECUTION', 'EXTERNAL_WORKER_JOB_COUNT_', 'varchar(255)');

-- 6514->6515
CALL AddColumnIfNotExists('ACT_RU_ACTINST', 'TRANSACTION_ORDER_', 'integer');


-- 6600 -> 6601
CALL AddColumnIfNotExists('ACT_HI_PROCINST', 'PROPAGATED_STAGE_INST_ID_', 'varchar(255)');

-- 6700 -> 6701
CALL AddColumnIfNotExists('ACT_RU_EXECUTION', 'BUSINESS_STATUS_', 'varchar(255)');

-- 6620 -> 6621
create index if not exists ACT_IDX_EXEC_REF_ID_ on ACT_RU_EXECUTION(REFERENCE_ID_);
create index if not exists ACT_IDX_RU_ACTI_TASK on ACT_RU_ACTINST(TASK_ID_);

-- 6622 -> 6623
create index if not exists ACT_IDX_HI_PRO_SUPER_PROCINST on ACT_HI_PROCINST(SUPER_PROCESS_INSTANCE_ID_);

-- history


-- 6412 -> 6413
delete
from ACT_HI_VARINST
where VAR_TYPE_ in ('item', 'message')
  and BYTEARRAY_ID_ is null
  and DOUBLE_ is null
  and LONG_ is null
  and TEXT_ is null
  and TEXT2_ is null;

delete from ACT_HI_VARINST
where VAR_TYPE_ = 'null'
  and NAME_ in ('org.activiti.engine.impl.bpmn.CURRENT_MESSAGE', 'org.flowable.engine.impl.bpmn.CURRENT_MESSAGE');


-- 6505 -> 6506
CALL AddColumnIfNotExists('ACT_HI_PROCINST', 'REFERENCE_ID_', 'varchar(255)');
CALL AddColumnIfNotExists('ACT_HI_PROCINST', 'REFERENCE_TYPE_', 'varchar(255)');


-- 6514->6515
CALL AddColumnIfNotExists('ACT_HI_ACTINST', 'TRANSACTION_ORDER_', 'integer');

-- 6600 -> 6601
CALL AddColumnIfNotExists('ACT_HI_PROCINST', 'PROPAGATED_STAGE_INST_ID_', 'varchar(255)');

-- 6700 -> 6701
CALL AddColumnIfNotExists('ACT_HI_PROCINST', 'BUSINESS_STATUS_', 'varchar(255)');

-- 6722 -> 6723
create index if not exists ACT_IDX_HI_PRO_SUPER_PROCINST on ACT_HI_PROCINST(SUPER_PROCESS_INSTANCE_ID_);

update ACT_GE_PROPERTY set VALUE_ = '7.1.0.2' where NAME_ = 'schema.version';

-- identity

-- 6310 -> 6320
CALL AddColumnIfNotExists('ACT_ID_USER', 'DISPLAY_NAME_', 'varchar(255) DEFAULT ""');

update ACT_ID_PROPERTY set VALUE_ = '7.1.0.2' where NAME_ = 'schema.version';


-- flowable.mysql.create.eventregistry.sql
CREATE TABLE if not exists FLW_EVENT_DEPLOYMENT (ID_ VARCHAR(255) NOT NULL, NAME_ VARCHAR(255) NULL, CATEGORY_ VARCHAR(255) NULL, DEPLOY_TIME_ datetime(3) NULL, TENANT_ID_ VARCHAR(255) NULL, PARENT_DEPLOYMENT_ID_ VARCHAR(255) NULL, CONSTRAINT PK_FLW_EVENT_DEPLOYMENT PRIMARY KEY (ID_));

CREATE TABLE if not exists FLW_EVENT_RESOURCE (ID_ VARCHAR(255) NOT NULL, NAME_ VARCHAR(255) NULL, DEPLOYMENT_ID_ VARCHAR(255) NULL, RESOURCE_BYTES_ LONGBLOB NULL, CONSTRAINT PK_FLW_EVENT_RESOURCE PRIMARY KEY (ID_));

CREATE TABLE if not exists FLW_EVENT_DEFINITION (ID_ VARCHAR(255) NOT NULL, NAME_ VARCHAR(255) NULL, VERSION_ INT NULL, KEY_ VARCHAR(255) NULL, CATEGORY_ VARCHAR(255) NULL, DEPLOYMENT_ID_ VARCHAR(255) NULL, TENANT_ID_ VARCHAR(255) NULL, RESOURCE_NAME_ VARCHAR(255) NULL, DESCRIPTION_ VARCHAR(255) NULL, CONSTRAINT PK_FLW_EVENT_DEFINITION PRIMARY KEY (ID_));

CREATE UNIQUE INDEX if not exists ACT_IDX_EVENT_DEF_UNIQ ON FLW_EVENT_DEFINITION(KEY_, VERSION_, TENANT_ID_);

CREATE TABLE if not exists FLW_CHANNEL_DEFINITION (ID_ VARCHAR(255) NOT NULL, NAME_ VARCHAR(255) NULL, VERSION_ INT NULL, KEY_ VARCHAR(255) NULL, CATEGORY_ VARCHAR(255) NULL, DEPLOYMENT_ID_ VARCHAR(255) NULL, CREATE_TIME_ datetime(3) NULL, TENANT_ID_ VARCHAR(255) NULL, RESOURCE_NAME_ VARCHAR(255) NULL, DESCRIPTION_ VARCHAR(255) NULL, CONSTRAINT PK_FLW_CHANNEL_DEFINITION PRIMARY KEY (ID_));

CREATE UNIQUE INDEX if not exists ACT_IDX_CHANNEL_DEF_UNIQ ON FLW_CHANNEL_DEFINITION(KEY_, VERSION_, TENANT_ID_);

CALL AddColumnIfNotExists('FLW_CHANNEL_DEFINITION','TYPE_', 'VARCHAR(255) NULL');

CALL AddColumnIfNotExists('FLW_CHANNEL_DEFINITION','IMPLEMENTATION_', 'VARCHAR(255) NULL');

insert into ACT_GE_PROPERTY
values ('eventregistry.schema.version', '7.1.0.2', 1);