CREATE OR REPLACE PACKAGE dbms_metadata AUTHID CURRENT_USER AS
---------------------------------------------------------------------
-- Overview
-- This pkg implements the mdAPI, a means to retrieve the aggregated
-- definitions of database objects as either XML docs. or their creation DDL,
-- or to submit the XML documents to execute the DDL.
---------------------------------------------------------------------
-- SECURITY
-- This package is owned by SYS with execute access granted to PUBLIC.
-- It runs with invokers rights, i.e., with the security profile of
-- the caller.  It calls DBMS_METADATA_INT to perform privileged
-- functions.
-- The object views defined in catmeta.sql implement the package's security
-- policy via the WHERE clause on the public views which include syntax to
-- control user access to metadata: if the current user is SYS or has
-- SELECT_CATALOG_ROLE, then all objects are visible; otherwise, only
-- objects in the schema of the current user are visible.

---------------------------
-- PROCEDURES AND FUNCTIONS
--

-- GET_DDL:     Return the metadata for a single object as DDL.
--      This interface is meant for casual browsing (e.g., from SQLPlus)
--      vs. the programmatic OPEN / FETCH / CLOSE interfaces above.
-- PARAMETERS:
--      object_type     - The type of object to be retrieved.
--      name            - Name of the object.
--      ob_schema       - Schema containing the object.  Defaults to
--                        the caller's schema.
--      version         - The version of the objects' metadata.
--      model           - The object model for the metadata.
--      transform       - XSL-T transform to be applied.
-- RETURNS:     Metadata for the object transformed to DDL as a CLOB.

  FUNCTION get_ddl (
    object_type     VARCHAR,
    name            VARCHAR,
    ob_schema       VARCHAR DEFAULT NULL,
    version         VARCHAR DEFAULT 'COMPATIBLE',
    model           VARCHAR DEFAULT 'ORACLE',
    transform       VARCHAR DEFAULT 'DDL')
  RETURN CLOB;
END DBMS_METADATA;
//
