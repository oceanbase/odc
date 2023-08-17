CREATE TABLE HASH_PART_BY_PARSER (COL1 NUMBER, COL2 VARCHAR2(50))
                partition by hash(col1)
                (partition P0,
                partition P1,
                partition P2,
                partition P3,
                partition P4,
                partition P5);

CREATE TABLE LIST_PART_BY_PARSER (LOG_ID NUMBER(38), LOG_VALUE VARCHAR2(20))
                partition by list(log_value)
                (partition P01 values  ('A','B','C'),
                partition P02 values  ('D','E','F'),
                partition P03 values  ('G','H','I'),
                partition P04 values  (DEFAULT));

CREATE TABLE RANGE_PART_BY_PARSER (
                LOG_ID NUMBER(38),
                LOG_DATE DATE DEFAULT sysdate CONSTRAINT "RANGE_PART_OBNOTNULL_1688526018016703" NOT NULL ENABLE)
                partition by range(log_date)
                (partition M202001 values less than (TO_DATE('2020-02-01 00:00:00', 'SYYYY-MM-DD HH24:MI:SS', 'NLS_CALENDAR=GREGORIAN')),
                partition M202002 values less than (TO_DATE('2020-03-01 00:00:00', 'SYYYY-MM-DD HH24:MI:SS','NLS_CALENDAR=GREGORIAN')),
                partition M202003 values less than (TO_DATE('2020-04-01 00:00:00', 'SYYYY-MM-DD HH24:MI:SS','NLS_CALENDAR=GREGORIAN')),
                partition MMAX values less than (MAXVALUE));

CREATE TABLE CONSTRAINT_PRIMARY_BY_PARSER (
  COL1 NUMBER(38),
  COL2 NUMBER(38),
  CONSTRAINT "CONSTRAINT_PRIMARY_OBPK_1688550078531369" PRIMARY KEY ("COL1")
);

CREATE TABLE CONSTRAINT_MULTY_BY_PARSER (
                COL1 NUMBER(*,0) CONSTRAINT "test约束_OBNOTNULL_1677652859452753" NOT NULL ENABLE,
                COL2 NUMBER CONSTRAINT "CONSTRAINT_MULTY3_OBNOTNULL_1688716122141818" NOT NULL ENABLE,
                COL3 CHAR(120),
                COL4 VARCHAR2(120),
                COL5 VARCHAR2(120),
                COL6 DATE,
                COL7 TIMESTAMP(6),
                COL8 TIMESTAMP(6) WITH TIME ZONE,
                COL9 TIMESTAMP(6) WITH LOCAL TIME ZONE,
                COL10 NCHAR(120),
                COL11 NVARCHAR2(120),
                COL12 FLOAT(126),
                A NUMBER(*,0),
                CONSTRAINT "pk3" PRIMARY KEY ("COL1"),
                CONSTRAINT "fk3" FOREIGN KEY ("A") REFERENCES "CONSTRAINT_PRIMARY_BY_PARSER"("COL1"),
                CONSTRAINT "unq33" UNIQUE ("COL2"),
                CONSTRAINT "unq332" UNIQUE ("COL3", "COL4"),
                CONSTRAINT "check33" CHECK (("COL5" > 1)) ENABLE);

CREATE TABLE TEST_INDEX_BY_PARSER (
  ID NUMBER PRIMARY KEY,
  COL1 VARCHAR2(10),
  COL2 NUMBER UNIQUE,
  COL3 NUMBER,
  COL4 NUMBER,
  COL5 NUMBER,
  COL6 NUMBER,
  COL7 NUMBER,
  CONSTRAINT CONSTRAINT_UNIQUE_TEST_INDEX_BY_PARSER UNIQUE (COL5,COL6)
);
CREATE INDEX IND_FUNCTION_BASED on TEST_INDEX_BY_PARSER (UPPER("COL1")) GLOBAL;
CREATE UNIQUE INDEX UNIQUE_IDX_TEST_INDEX_BY_PARSER on TEST_INDEX_BY_PARSER (COL3, COL4) LOCAL;
CREATE INDEX NORMAL_IDX_TEST_INDEX_BY_PARSER on TEST_INDEX_BY_PARSER (COL7) LOCAL;