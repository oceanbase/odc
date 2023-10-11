SELECT
  CAST(INDEX_NAME AS VARCHAR2(128)) AS INDEX_NAME,
  CAST(VISIBILITY AS VARCHAR2(10)) AS VISIBILITY,
  CAST(STATUS AS VARCHAR2(10)) AS STATUS
FROM
  (
    SELECT
      DATABASE_NAME AS INDEX_OWNER,
      CASE
      WHEN (
        TABLE_TYPE = 5
        AND B.DATABASE_NAME != '__recyclebin'
      ) THEN SUBSTR(
        TABLE_NAME,
        7 + INSTR(SUBSTR(TABLE_NAME, 7), '_')
      )
      WHEN (
        TABLE_TYPE = 5
        AND B.DATABASE_NAME = '__recyclebin'
      ) THEN TABLE_NAME
      ELSE (CONS_TAB.CONSTRAINT_NAME)
    END
      AS INDEX_NAME,
      DATABASE_NAME AS TABLE_OWNER,
      CASE
      WHEN (TABLE_TYPE = 3) THEN A.TABLE_ID
      ELSE A.DATA_TABLE_ID
    END
      AS TABLE_ID,
      A.TABLE_ID AS INDEX_ID,
      A.INDEX_TYPE AS A_INDEX_TYPE,
      A.PART_LEVEL AS A_PART_LEVEL,
      A.TABLE_TYPE AS A_TABLE_TYPE,
      CASE
      WHEN BITAND(A.INDEX_ATTRIBUTES_SET, 1) = 0 THEN 'VISIBLE'
      ELSE 'INVISIBLE'
    END
      AS VISIBILITY,
      CASE
      WHEN TABLE_TYPE = 3 THEN 'VALID'
      WHEN A.INDEX_STATUS = 2 THEN 'VALID'
      WHEN A.INDEX_STATUS = 3 THEN 'CHECKING'
      WHEN A.INDEX_STATUS = 4 THEN 'INELEGIBLE'
      WHEN A.INDEX_STATUS = 5 THEN 'ERROR'
      ELSE 'UNUSABLE'
    END
      AS STATUS,
      A.TABLESPACE_ID
    FROM
      SYS.ALL_VIRTUAL_TABLE_REAL_AGENT A
      JOIN SYS.ALL_VIRTUAL_DATABASE_REAL_AGENT B ON A.DATABASE_ID = B.DATABASE_ID
      AND A.TENANT_ID = EFFECTIVE_TENANT_ID()
      AND B.TENANT_ID = EFFECTIVE_TENANT_ID()
      AND (
        A.DATABASE_ID = USERENV('SCHEMAID')
        OR USER_CAN_ACCESS_OBJ(
          1,
          DECODE(TABLE_TYPE, 3, A.TABLE_ID, 5, DATA_TABLE_ID),
          A.DATABASE_ID
        ) = 1
      )
      AND TABLE_TYPE IN (5, 3)
      LEFT JOIN SYS.ALL_VIRTUAL_CONSTRAINT_REAL_AGENT CONS_TAB ON (
        CONS_TAB.TABLE_ID = A.TABLE_ID
        AND CONS_TAB.TENANT_ID = EFFECTIVE_TENANT_ID()
      )
    WHERE
      NOT(
        TABLE_TYPE = 3
        AND CONSTRAINT_NAME IS NULL
      )
      AND (
        CONS_TAB.CONSTRAINT_TYPE IS NULL
        OR CONS_TAB.CONSTRAINT_TYPE = 1
      )
  ) C
  JOIN SYS.ALL_VIRTUAL_TABLE_REAL_AGENT D ON C.TABLE_ID = D.TABLE_ID
  AND D.TENANT_ID = EFFECTIVE_TENANT_ID()
  LEFT JOIN SYS.ALL_VIRTUAL_TENANT_TABLESPACE_REAL_AGENT TP ON C.TABLESPACE_ID = TP.TABLESPACE_ID
  AND TP.TENANT_ID = EFFECTIVE_TENANT_ID()
WHERE INDEX_OWNER=? AND TABLE_NAME=?
ORDER BY INDEX_NAME ASC;