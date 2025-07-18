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
-- 6120 -> 6200

alter table if exists ACT_RU_TASK
  add column if not exists SCOPE_ID_ varchar(255);
alter table if exists ACT_RU_TASK
  add column if not exists SUB_SCOPE_ID_ varchar(255);
alter table if exists ACT_RU_TASK
  add column if not exists SCOPE_TYPE_ varchar(255);
alter table if exists ACT_RU_TASK
  add column if not exists SCOPE_DEFINITION_ID_ varchar(255);
create index if not exists ACT_IDX_TASK_SCOPE on ACT_RU_TASK (SCOPE_ID_, SCOPE_TYPE_);
create index if not exists ACT_IDX_TASK_SUB_SCOPE on ACT_RU_TASK (SUB_SCOPE_ID_, SCOPE_TYPE_);
create index if not exists ACT_IDX_TASK_SCOPE_DEF on ACT_RU_TASK (SCOPE_DEFINITION_ID_, SCOPE_TYPE_);

alter table if exists ACT_HI_TASKINST
  add column if not exists SCOPE_ID_ varchar(255);
alter table if exists ACT_HI_TASKINST
  add column if not exists SUB_SCOPE_ID_ varchar(255);
alter table if exists ACT_HI_TASKINST
  add column if not exists SCOPE_TYPE_ varchar(255);
alter table if exists ACT_HI_TASKINST
  add column if not exists SCOPE_DEFINITION_ID_ varchar(255);

create index if not exists ACT_IDX_HI_TASK_SCOPE on ACT_HI_TASKINST (SCOPE_ID_, SCOPE_TYPE_);
create index if not exists ACT_IDX_HI_TASK_SUB_SCOPE on ACT_HI_TASKINST (SUB_SCOPE_ID_, SCOPE_TYPE_);
create index if not exists ACT_IDX_HI_TASK_SCOPE_DEF on ACT_HI_TASKINST (SCOPE_DEFINITION_ID_, SCOPE_TYPE_);
alter table if exists ACT_RU_VARIABLE
  add column if not exists SCOPE_ID_ varchar(255);
alter table if exists ACT_RU_VARIABLE
  add column if not exists SUB_SCOPE_ID_ varchar(255);
alter table if exists ACT_RU_VARIABLE
  add column if not exists SCOPE_TYPE_ varchar(255);
create index if not exists ACT_IDX_RU_VAR_SCOPE_ID_TYPE on ACT_RU_VARIABLE (SCOPE_ID_, SCOPE_TYPE_);
create index if not exists ACT_IDX_RU_VAR_SUB_ID_TYPE on ACT_RU_VARIABLE (SUB_SCOPE_ID_, SCOPE_TYPE_);

alter table if exists ACT_HI_VARINST
  add column if not exists SCOPE_ID_ varchar(255);
alter table if exists ACT_HI_VARINST
  add column if not exists SUB_SCOPE_ID_ varchar(255);
alter table if exists ACT_HI_VARINST
  add column if not exists SCOPE_TYPE_ varchar(255);

create index if not exists ACT_IDX_HI_VAR_SCOPE_ID_TYPE on ACT_HI_VARINST (SCOPE_ID_, SCOPE_TYPE_);
create index if not exists ACT_IDX_HI_VAR_SUB_ID_TYPE on ACT_HI_VARINST (SUB_SCOPE_ID_, SCOPE_TYPE_);

-- 6200 -> 6210

alter table if exists ACT_RU_JOB
  add column if not exists SCOPE_ID_ varchar(255);
alter table if exists ACT_RU_JOB
  add column if not exists SUB_SCOPE_ID_ varchar(255);
alter table if exists ACT_RU_JOB
  add column if not exists SCOPE_TYPE_ varchar(255);
alter table if exists ACT_RU_JOB
  add column if not exists SCOPE_DEFINITION_ID_ varchar(255);
alter table if exists ACT_RU_TIMER_JOB
  add column if not exists SCOPE_ID_ varchar(255);
alter table if exists ACT_RU_TIMER_JOB
  add column if not exists SUB_SCOPE_ID_ varchar(255);
alter table if exists ACT_RU_TIMER_JOB
  add column if not exists SCOPE_TYPE_ varchar(255);
alter table if exists ACT_RU_TIMER_JOB
  add column if not exists SCOPE_DEFINITION_ID_ varchar(255);
alter table if exists ACT_RU_SUSPENDED_JOB
  add column if not exists SCOPE_ID_ varchar(255);
alter table if exists ACT_RU_SUSPENDED_JOB
  add column if not exists SUB_SCOPE_ID_ varchar(255);
alter table if exists ACT_RU_SUSPENDED_JOB
  add column if not exists SCOPE_TYPE_ varchar(255);
alter table if exists ACT_RU_SUSPENDED_JOB
  add column if not exists SCOPE_DEFINITION_ID_ varchar(255);
alter table if exists ACT_RU_DEADLETTER_JOB
  add column if not exists SCOPE_ID_ varchar(255);
alter table if exists ACT_RU_DEADLETTER_JOB
  add column if not exists SUB_SCOPE_ID_ varchar(255);
alter table if exists ACT_RU_DEADLETTER_JOB
  add column if not exists SCOPE_TYPE_ varchar(255);
alter table if exists ACT_RU_DEADLETTER_JOB
  add column if not exists SCOPE_DEFINITION_ID_ varchar(255);
create index if not exists ACT_IDX_JOB_SCOPE on ACT_RU_JOB (SCOPE_ID_, SCOPE_TYPE_);
create index if not exists ACT_IDX_JOB_SUB_SCOPE on ACT_RU_JOB (SUB_SCOPE_ID_, SCOPE_TYPE_);
create index if not exists ACT_IDX_JOB_SCOPE_DEF on ACT_RU_JOB (SCOPE_DEFINITION_ID_, SCOPE_TYPE_);
create index if not exists ACT_IDX_TJOB_SCOPE on ACT_RU_TIMER_JOB (SCOPE_ID_, SCOPE_TYPE_);
create index if not exists ACT_IDX_TJOB_SUB_SCOPE on ACT_RU_TIMER_JOB (SUB_SCOPE_ID_, SCOPE_TYPE_);
create index if not exists ACT_IDX_TJOB_SCOPE_DEF on ACT_RU_TIMER_JOB (SCOPE_DEFINITION_ID_, SCOPE_TYPE_);
create index if not exists ACT_IDX_SJOB_SCOPE on ACT_RU_SUSPENDED_JOB (SCOPE_ID_, SCOPE_TYPE_);
create index if not exists ACT_IDX_SJOB_SUB_SCOPE on ACT_RU_SUSPENDED_JOB (SUB_SCOPE_ID_, SCOPE_TYPE_);
create index if not exists ACT_IDX_SJOB_SCOPE_DEF on ACT_RU_SUSPENDED_JOB (SCOPE_DEFINITION_ID_, SCOPE_TYPE_);
create index if not exists ACT_IDX_DJOB_SCOPE on ACT_RU_DEADLETTER_JOB (SCOPE_ID_, SCOPE_TYPE_);
create index if not exists ACT_IDX_DJOB_SUB_SCOPE on ACT_RU_DEADLETTER_JOB (SUB_SCOPE_ID_, SCOPE_TYPE_);
create index if not exists ACT_IDX_DJOB_SCOPE_DEF on ACT_RU_DEADLETTER_JOB (SCOPE_DEFINITION_ID_, SCOPE_TYPE_);
alter table if exists ACT_RU_JOB
  add column if not exists CUSTOM_VALUES_ID_ varchar(64);
alter table if exists ACT_RU_TIMER_JOB
  add column if not exists CUSTOM_VALUES_ID_ varchar(64);
alter table if exists ACT_RU_SUSPENDED_JOB
  add column if not exists CUSTOM_VALUES_ID_ varchar(64);
alter table if exists ACT_RU_DEADLETTER_JOB
  add column if not exists CUSTOM_VALUES_ID_ varchar(64);
alter table if exists ACT_RU_HISTORY_JOB
  add column if not exists CUSTOM_VALUES_ID_ varchar(64);
create index if not exists ACT_IDX_JOB_CUSTOM_VALUES_ID on ACT_RU_JOB (CUSTOM_VALUES_ID_);
create index if not exists ACT_IDX_TIMER_JOB_CUSTOM_VALUES_ID on ACT_RU_TIMER_JOB (CUSTOM_VALUES_ID_);
create index if not exists ACT_IDX_SUSPENDED_JOB_CUSTOM_VALUES_ID on ACT_RU_SUSPENDED_JOB (CUSTOM_VALUES_ID_);
create index if not exists ACT_IDX_DEADLETTER_JOB_CUSTOM_VALUES_ID on ACT_RU_DEADLETTER_JOB (CUSTOM_VALUES_ID_);
alter table if exists ACT_RU_JOB
  add constraint if not exists ACT_FK_JOB_CUSTOM_VALUES
    foreign key (CUSTOM_VALUES_ID_)
      references ACT_GE_BYTEARRAY (ID_);
alter table if exists ACT_RU_TIMER_JOB
  add constraint if not exists ACT_FK_TIMER_JOB_CUSTOM_VALUES
    foreign key (CUSTOM_VALUES_ID_)
      references ACT_GE_BYTEARRAY (ID_);
alter table if exists ACT_RU_SUSPENDED_JOB
  add constraint if not exists ACT_FK_SUSPENDED_JOB_CUSTOM_VALUES
    foreign key (CUSTOM_VALUES_ID_)
      references ACT_GE_BYTEARRAY (ID_);
alter table if exists ACT_RU_DEADLETTER_JOB
  add constraint if not exists ACT_FK_DEADLETTER_JOB_CUSTOM_VALUES
    foreign key (CUSTOM_VALUES_ID_)
      references ACT_GE_BYTEARRAY (ID_);

-- 6210 -> 6300

-- engine
alter table if exists ACT_RU_IDENTITYLINK
  add column if not exists SCOPE_ID_ varchar(255);
alter table if exists ACT_RU_IDENTITYLINK
  add column if not exists SCOPE_TYPE_ varchar(255);
alter table if exists ACT_RU_IDENTITYLINK
  add column if not exists SCOPE_DEFINITION_ID_ varchar(255);
create index if not exists ACT_IDX_IDENT_LNK_SCOPE on ACT_RU_IDENTITYLINK (SCOPE_ID_, SCOPE_TYPE_);
create index if not exists ACT_IDX_IDENT_LNK_SCOPE_DEF on ACT_RU_IDENTITYLINK (SCOPE_DEFINITION_ID_, SCOPE_TYPE_);

alter table if exists ACT_HI_IDENTITYLINK
  add column if not exists SCOPE_ID_ varchar(255);
alter table if exists ACT_HI_IDENTITYLINK
  add column if not exists SCOPE_TYPE_ varchar(255);
alter table if exists ACT_HI_IDENTITYLINK
  add column if not exists SCOPE_DEFINITION_ID_ varchar(255);

create index if not exists ACT_IDX_HI_IDENT_LNK_SCOPE on ACT_HI_IDENTITYLINK (SCOPE_ID_, SCOPE_TYPE_);
create index if not exists ACT_IDX_HI_IDENT_LNK_SCOPE_DEF on ACT_HI_IDENTITYLINK (SCOPE_DEFINITION_ID_, SCOPE_TYPE_);

update ACT_RU_TIMER_JOB
set HANDLER_TYPE_ = 'cmmn-trigger-timer'
where HANDLER_TYPE_ = 'trigger-timer'
  and SCOPE_TYPE_ = 'cmmn';

-- idm
alter table if exists ACT_ID_USER add if not exists TENANT_ID_ varchar(255) default '';
alter table if exists ACT_ID_PRIV alter column if exists NAME_ set not null;
alter table if exists ACT_ID_PRIV add constraint if not exists ACT_UNIQ_PRIV_NAME unique (NAME_);

-- 6300 -> 6301

alter table if exists ACT_RU_TASK
  add column if not exists TASK_DEF_ID_ varchar(64);
alter table if exists ACT_HI_TASKINST
  add column if not exists TASK_DEF_ID_ varchar(64);

-- 6310 -> 6320

-- idm
alter table if exists ACT_ID_USER add if not exists DISPLAY_NAME_ varchar(255) default '';

-- 6400 -> 6410

create table if not exists ACT_RU_ENTITYLINK
(
  ID_                      varchar(64),
  REV_                     integer,
  CREATE_TIME_             timestamp,
  LINK_TYPE_               varchar(255),
  SCOPE_ID_                varchar(255),
  SCOPE_TYPE_              varchar(255),
  SCOPE_DEFINITION_ID_     varchar(255),
  REF_SCOPE_ID_            varchar(255),
  REF_SCOPE_TYPE_          varchar(255),
  REF_SCOPE_DEFINITION_ID_ varchar(255),
  primary key (ID_)
);

create index if not exists ACT_IDX_ENT_LNK_SCOPE on ACT_RU_ENTITYLINK (SCOPE_ID_, SCOPE_TYPE_, LINK_TYPE_);
create index if not exists ACT_IDX_ENT_LNK_SCOPE_DEF on ACT_RU_ENTITYLINK (SCOPE_DEFINITION_ID_, SCOPE_TYPE_, LINK_TYPE_);

create table if not exists ACT_HI_ENTITYLINK
(
  ID_                      varchar(64),
  LINK_TYPE_               varchar(255),
  CREATE_TIME_             timestamp,
  SCOPE_ID_                varchar(255),
  SCOPE_TYPE_              varchar(255),
  SCOPE_DEFINITION_ID_     varchar(255),
  REF_SCOPE_ID_            varchar(255),
  REF_SCOPE_TYPE_          varchar(255),
  REF_SCOPE_DEFINITION_ID_ varchar(255),
  primary key (ID_)
);

create index if not exists ACT_IDX_HI_ENT_LNK_SCOPE on ACT_HI_ENTITYLINK (SCOPE_ID_, SCOPE_TYPE_, LINK_TYPE_);
create index if not exists ACT_IDX_HI_ENT_LNK_SCOPE_DEF on ACT_HI_ENTITYLINK (SCOPE_DEFINITION_ID_, SCOPE_TYPE_, LINK_TYPE_);

-- 6410 -> 6411

alter table if exists ACT_RU_ENTITYLINK
  add column if not exists HIERARCHY_TYPE_ varchar(255);
alter table if exists ACT_HI_ENTITYLINK
  add column if not exists HIERARCHY_TYPE_ varchar(255);

-- 6411 -> 6412

update ACT_RU_IDENTITYLINK
set SCOPE_DEFINITION_ID_ = null
where SCOPE_ID_ is not null
  and SCOPE_DEFINITION_ID_ is not null;

-- 6412 -> 6413

create table if not exists ACT_HI_TSK_LOG
(
  ID_                  bigint generated by default as identity,
  TYPE_                varchar(64),
  TASK_ID_             varchar(64) not null,
  TIME_STAMP_          timestamp   not null,
  USER_ID_             varchar(255),
  DATA_                varchar(4000),
  EXECUTION_ID_        varchar(64),
  PROC_INST_ID_        varchar(64),
  PROC_DEF_ID_         varchar(64),
  SCOPE_ID_            varchar(255),
  SCOPE_DEFINITION_ID_ varchar(255),
  SUB_SCOPE_ID_        varchar(255),
  SCOPE_TYPE_          varchar(255),
  TENANT_ID_           varchar(255) default '',
  primary key (ID_)
);

-- 6413 -> 6500

alter table if exists ACT_RU_EVENT_SUBSCR
  add column if not exists SUB_SCOPE_ID_ varchar(64);
alter table if exists ACT_RU_EVENT_SUBSCR
  add column if not exists SCOPE_ID_ varchar(64);
alter table if exists ACT_RU_EVENT_SUBSCR
  add column if not exists SCOPE_DEFINITION_ID_ varchar(64);
alter table if exists ACT_RU_EVENT_SUBSCR
  add column if not exists SCOPE_TYPE_ varchar(64);

-- 6500 -> 6501

alter table if exists ACT_RU_JOB
  add column if not exists ELEMENT_ID_ varchar(255);
alter table if exists ACT_RU_JOB
  add column if not exists ELEMENT_NAME_ varchar(255);
alter table if exists ACT_RU_TIMER_JOB
  add column if not exists ELEMENT_ID_ varchar(255);
alter table if exists ACT_RU_TIMER_JOB
  add column if not exists ELEMENT_NAME_ varchar(255);
alter table if exists ACT_RU_SUSPENDED_JOB
  add column if not exists ELEMENT_ID_ varchar(255);
alter table if exists ACT_RU_SUSPENDED_JOB
  add column if not exists ELEMENT_NAME_ varchar(255);
alter table if exists ACT_RU_DEADLETTER_JOB
  add column if not exists ELEMENT_ID_ varchar(255);
alter table if exists ACT_RU_DEADLETTER_JOB
  add column if not exists ELEMENT_NAME_ varchar(255);

-- 6501 -> 6502

create table if not exists FLW_RU_BATCH
(
  ID_            varchar(64) not null,
  REV_           integer,
  TYPE_          varchar(64) not null,
  SEARCH_KEY_    varchar(255),
  SEARCH_KEY2_   varchar(255),
  CREATE_TIME_   timestamp   not null,
  COMPLETE_TIME_ timestamp,
  STATUS_        varchar(255),
  BATCH_DOC_ID_  varchar(64),
  TENANT_ID_     varchar(255) default '',
  primary key (ID_)
);
create table if not exists FLW_RU_BATCH_PART
(
  ID_            varchar(64) not null,
  REV_           integer,
  BATCH_ID_      varchar(64),
  TYPE_          varchar(64) not null,
  SCOPE_ID_      varchar(64),
  SUB_SCOPE_ID_  varchar(64),
  SCOPE_TYPE_    varchar(64),
  SEARCH_KEY_    varchar(255),
  SEARCH_KEY2_   varchar(255),
  CREATE_TIME_   timestamp   not null,
  COMPLETE_TIME_ timestamp,
  STATUS_        varchar(255),
  RESULT_DOC_ID_ varchar(64),
  TENANT_ID_     varchar(255) default '',
  primary key (ID_)
);
create index if not exists FLW_IDX_BATCH_PART on FLW_RU_BATCH_PART (BATCH_ID_);
alter table if exists FLW_RU_BATCH_PART
  add constraint if not exists FLW_FK_BATCH_PART_PARENT
    foreign key (BATCH_ID_)
      references FLW_RU_BATCH (ID_);

-- 6502 -> 6503

alter table if exists ACT_RU_IDENTITYLINK
  add column if not exists SUB_SCOPE_ID_ varchar(255);
create index if not exists ACT_IDX_IDENT_LNK_SUB_SCOPE on ACT_RU_IDENTITYLINK (SUB_SCOPE_ID_, SCOPE_TYPE_);

alter table if exists ACT_HI_IDENTITYLINK
  add column if not exists SUB_SCOPE_ID_ varchar(255);

create index if not exists ACT_IDX_HI_IDENT_LNK_SUB_SCOPE on ACT_HI_IDENTITYLINK (SUB_SCOPE_ID_, SCOPE_TYPE_);

-- 6504 -> 6505

alter table if exists ACT_RU_TASK
  add column if not exists PROPAGATED_STAGE_INST_ID_ varchar(255);
alter table if exists ACT_HI_TASKINST
  add column if not exists PROPAGATED_STAGE_INST_ID_ varchar(255);

-- 6510 -> 6511

alter table if exists ACT_RU_ENTITYLINK
  add column if not exists ROOT_SCOPE_ID_ varchar(255);
alter table if exists ACT_RU_ENTITYLINK
  add column if not exists ROOT_SCOPE_TYPE_ varchar(255);
create index if not exists ACT_IDX_ENT_LNK_ROOT_SCOPE on ACT_RU_ENTITYLINK (ROOT_SCOPE_ID_, ROOT_SCOPE_TYPE_, LINK_TYPE_);

alter table if exists ACT_HI_ENTITYLINK
  add column if not exists ROOT_SCOPE_ID_ varchar(255);
alter table if exists ACT_HI_ENTITYLINK
  add column if not exists ROOT_SCOPE_TYPE_ varchar(255);
create index if not exists ACT_IDX_HI_ENT_LNK_ROOT_SCOPE on ACT_HI_ENTITYLINK (ROOT_SCOPE_ID_, ROOT_SCOPE_TYPE_, LINK_TYPE_);

-- 6511 -> 6512

alter table if exists ACT_RU_JOB
  add column if not exists CATEGORY_ varchar(255);
alter table if exists ACT_RU_TIMER_JOB
  add column if not exists CATEGORY_ varchar(255);
alter table if exists ACT_RU_SUSPENDED_JOB
  add column if not exists CATEGORY_ varchar(255);
alter table if exists ACT_RU_DEADLETTER_JOB
  add column if not exists CATEGORY_ varchar(255);

-- 6512 -> 6513

create table if not exists ACT_RU_EXTERNAL_JOB
(
  ID_                  varchar(64)  NOT NULL,
  REV_                 integer,
  CATEGORY_            varchar(255),
  TYPE_                varchar(255) NOT NULL,
  LOCK_EXP_TIME_       timestamp,
  LOCK_OWNER_          varchar(255),
  EXCLUSIVE_           boolean,
  EXECUTION_ID_        varchar(64),
  PROCESS_INSTANCE_ID_ varchar(64),
  PROC_DEF_ID_         varchar(64),
  ELEMENT_ID_          varchar(255),
  ELEMENT_NAME_        varchar(255),
  SCOPE_ID_            varchar(255),
  SUB_SCOPE_ID_        varchar(255),
  SCOPE_TYPE_          varchar(255),
  SCOPE_DEFINITION_ID_ varchar(255),
  RETRIES_             integer,
  EXCEPTION_STACK_ID_  varchar(64),
  EXCEPTION_MSG_       varchar(4000),
  DUEDATE_             timestamp,
  REPEAT_              varchar(255),
  HANDLER_TYPE_        varchar(255),
  HANDLER_CFG_         varchar(4000),
  CUSTOM_VALUES_ID_    varchar(64),
  CREATE_TIME_         timestamp,
  TENANT_ID_           varchar(255) default '',
  primary key (ID_)
);
create index if not exists ACT_IDX_EXTERNAL_JOB_EXCEPTION_STACK_ID on ACT_RU_EXTERNAL_JOB (EXCEPTION_STACK_ID_);
create index if not exists ACT_IDX_EXTERNAL_JOB_CUSTOM_VALUES_ID on ACT_RU_EXTERNAL_JOB (CUSTOM_VALUES_ID_);
alter table if exists ACT_RU_EXTERNAL_JOB
  add constraint if not exists ACT_FK_EXTERNAL_JOB_EXCEPTION
    foreign key (EXCEPTION_STACK_ID_)
      references ACT_GE_BYTEARRAY (ID_);
alter table if exists ACT_RU_EXTERNAL_JOB
  add constraint if not exists ACT_FK_EXTERNAL_JOB_CUSTOM_VALUES
    foreign key (CUSTOM_VALUES_ID_)
      references ACT_GE_BYTEARRAY (ID_);
create index if not exists ACT_IDX_EJOB_SCOPE on ACT_RU_EXTERNAL_JOB (SCOPE_ID_, SCOPE_TYPE_);
create index if not exists ACT_IDX_EJOB_SUB_SCOPE on ACT_RU_EXTERNAL_JOB (SUB_SCOPE_ID_, SCOPE_TYPE_);
create index if not exists ACT_IDX_EJOB_SCOPE_DEF on ACT_RU_EXTERNAL_JOB (SCOPE_DEFINITION_ID_, SCOPE_TYPE_);

-- 6513 -> 6514

alter table if exists ACT_RU_JOB
  add column if not exists CORRELATION_ID_ varchar(255);
alter table if exists ACT_RU_TIMER_JOB
  add column if not exists CORRELATION_ID_ varchar(255);
alter table if exists ACT_RU_SUSPENDED_JOB
  add column if not exists CORRELATION_ID_ varchar(255);
alter table if exists ACT_RU_DEADLETTER_JOB
  add column if not exists CORRELATION_ID_ varchar(255);
alter table if exists ACT_RU_EXTERNAL_JOB
  add column if not exists CORRELATION_ID_ varchar(255);
create index if not exists ACT_IDX_JOB_CORRELATION_ID on ACT_RU_JOB (CORRELATION_ID_);
create index if not exists ACT_IDX_TIMER_JOB_CORRELATION_ID on ACT_RU_TIMER_JOB (CORRELATION_ID_);
create index if not exists ACT_IDX_SUSPENDED_JOB_CORRELATION_ID on ACT_RU_SUSPENDED_JOB (CORRELATION_ID_);
create index if not exists ACT_IDX_DEADLETTER_JOB_CORRELATION_ID on ACT_RU_DEADLETTER_JOB (CORRELATION_ID_);
create index if not exists ACT_IDX_EXTERNAL_JOB_CORRELATION_ID on ACT_RU_EXTERNAL_JOB (CORRELATION_ID_);

-- 6515 -> 6516

alter table if exists ACT_RU_ENTITYLINK
  add column if not exists SUB_SCOPE_ID_ varchar(255);
alter table if exists ACT_RU_ENTITYLINK
  add column if not exists PARENT_ELEMENT_ID_ varchar(255);

alter table if exists ACT_HI_ENTITYLINK
  add column if not exists SUB_SCOPE_ID_ varchar(255);
alter table if exists ACT_HI_ENTITYLINK
  add column if not exists PARENT_ELEMENT_ID_ varchar(255);

-- 6600 -> 6601

create index if not exists ACT_IDX_ENT_LNK_REF_SCOPE on ACT_RU_ENTITYLINK (REF_SCOPE_ID_, REF_SCOPE_TYPE_, LINK_TYPE_);
create index if not exists ACT_IDX_HI_ENT_LNK_REF_SCOPE on ACT_HI_ENTITYLINK (REF_SCOPE_ID_, REF_SCOPE_TYPE_, LINK_TYPE_);

-- 6601 -> 6610

create index if not exists ACT_IDX_TIMER_JOB_DUEDATE on ACT_RU_TIMER_JOB (DUEDATE_);

-- 6720 -> 6721

create index if not exists ACT_IDX_EVENT_SUBSCR_SCOPEREF_ on ACT_RU_EVENT_SUBSCR (SCOPE_ID_, SCOPE_TYPE_);

-- 6721 -> 6722

alter table if exists ACT_RU_EVENT_SUBSCR
  add column if not exists LOCK_TIME_ timestamp;
alter table if exists ACT_RU_EVENT_SUBSCR
  add column if not exists LOCK_OWNER_ varchar(255);

-- 6800 -> 6810

alter table if exists ACT_RU_VARIABLE
  add column if not exists META_INFO_ varchar(4000);
alter table if exists ACT_HI_VARINST
  add column if not exists META_INFO_ varchar(4000);

-- 7000 -> 7010

alter table if exists ACT_RU_TASK
  add column if not exists STATE_ varchar(255);
alter table if exists ACT_RU_TASK
  add column if not exists IN_PROGRESS_TIME_ timestamp;
alter table if exists ACT_RU_TASK
  add column if not exists IN_PROGRESS_STARTED_BY_ varchar(255);
alter table if exists ACT_RU_TASK
  add column if not exists CLAIMED_BY_ varchar(255);
alter table if exists ACT_RU_TASK
  add column if not exists SUSPENDED_TIME_ timestamp;
alter table if exists ACT_RU_TASK
  add column if not exists SUSPENDED_BY_ varchar(255);
alter table if exists ACT_RU_TASK
  add column if not exists IN_PROGRESS_DUE_DATE_ timestamp;

alter table if exists ACT_HI_TASKINST
  add column if not exists STATE_ varchar(255);
alter table if exists ACT_HI_TASKINST
  add column if not exists IN_PROGRESS_TIME_ timestamp;
alter table if exists ACT_HI_TASKINST
  add column if not exists IN_PROGRESS_STARTED_BY_ varchar(255);
alter table if exists ACT_HI_TASKINST
  add column if not exists CLAIMED_BY_ varchar(255);
alter table if exists ACT_HI_TASKINST
  add column if not exists SUSPENDED_TIME_ timestamp;
alter table if exists ACT_HI_TASKINST
  add column if not exists SUSPENDED_BY_ varchar(255);
alter table if exists ACT_HI_TASKINST
  add column if not exists COMPLETED_BY_ varchar(255);
alter table if exists ACT_HI_TASKINST
  add column if not exists IN_PROGRESS_DUE_DATE_ timestamp;

-- 7010 -> 7011

-- common
delete from ACT_GE_PROPERTY where NAME_ = 'batch.schema.version';
delete from ACT_GE_PROPERTY where NAME_ = 'entitylink.schema.version';
delete from ACT_GE_PROPERTY where NAME_ = 'eventsubscription.schema.version';
delete from ACT_GE_PROPERTY where NAME_ = 'identitylink.schema.version';
delete from ACT_GE_PROPERTY where NAME_ = 'job.schema.version';
delete from ACT_GE_PROPERTY where NAME_ = 'task.schema.version';
delete from ACT_GE_PROPERTY where NAME_ = 'variable.schema.version';

-- event
alter table if exists ACT_RU_EVENT_SUBSCR
  add column if not exists SCOPE_DEFINITION_KEY_ varchar(255);

ALTER TABLE if exists FLW_CHANNEL_DEFINITION ADD if not exists TYPE_ VARCHAR(255);

ALTER TABLE if exists FLW_CHANNEL_DEFINITION ADD if not exists IMPLEMENTATION_ VARCHAR(255);

-- 7011 -> 7100

create index if not exists ACT_IDX_ACT_HI_TSK_LOG_TASK on ACT_HI_TSK_LOG (TASK_ID_);

-- 7101 -> 7102

create index if not exists ACT_IDX_EVENT_SUBSCR_EXEC_ID on ACT_RU_EVENT_SUBSCR (EXECUTION_ID_);
create index if not exists ACT_IDX_EVENT_SUBSCR_PROC_ID on ACT_RU_EVENT_SUBSCR (PROC_INST_ID_);

insert into ACT_GE_PROPERTY (NAME_, VALUE_, REV_)
values ('eventregistry.schema.version', '7.1.0.2', 1) ON DUPLICATE KEY UPDATE `VALUE_`= '7.1.0.2';

insert into ACT_GE_PROPERTY (NAME_, VALUE_, REV_)
values ('common.schema.version', '7.1.0.2', 1) ON DUPLICATE KEY UPDATE `VALUE_`= '7.1.0.2';

insert into ACT_GE_PROPERTY (NAME_, VALUE_, REV_)
values ('schema.version', '7.1.0.2', 1) ON DUPLICATE KEY UPDATE `VALUE_`= '7.1.0.2';

insert into ACT_ID_PROPERTY (NAME_, VALUE_, REV_)
values ('schema.version', '7.1.0.2', 1) ON DUPLICATE KEY UPDATE `VALUE_`= '7.1.0.2';






























