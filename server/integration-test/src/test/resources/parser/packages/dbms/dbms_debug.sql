CREATE OR REPLACE PACKAGE DBMS_DEBUG AS

  ------------------------------
  --  PUBLIC CONSTANTS and TYPES

  ----------------------------- PROGRAM_INFO ---------------------
  -- Specifies a program location - a line number in a program unit.
  -- Used for stack backtraces and for setting and examining breakpoints.
  --
  -- The read-only fields are currently ignored by Probe for breakpoint
  -- operations.  They are set by Probe only for stack backtraces.
  --    EntrypointName - null unless this is a nested procedure/function
  --    LibunitType    - to disambiguate among objects that share the same
  --                     namespace (eg. procedure and package spec).  See
  --                     the LibunitType_* constants below.
  --
  TYPE program_info IS RECORD
  (
    -- The following fields are used when setting a breakpoint
    Namespace        BINARY_INTEGER,  -- See 'NAMESPACES' section below.
    Name             VARCHAR2(128),  -- name of the program unit
    Owner            VARCHAR2(128)   -- owner of the program unit
  );

  ------------------------------- RUNTIME_INFO -----------------------
  -- Runtime_info gives context information about the running program.
  --
  TYPE runtime_info IS RECORD
  (
    Line#            BINARY_INTEGER,  -- (duplicate of program.line#)
    Terminated       BINARY_INTEGER,  -- has the program terminated?
    Breakpoint       BINARY_INTEGER,  -- breakpoint number
    StackDepth       BINARY_INTEGER,  -- number of frames on the stack
    Reason           BINARY_INTEGER,  -- reason for suspension
    Program          program_info     -- source location
   );

  ---------------------------- BREAKPOINT_INFO ----------------------------
  -- Information about a breakpoint: its current status and the program unit
  -- in which it was placed.
  --
  -- (The reason for duplicating fields from the 'program_info' record is
  --  that PL/SQL doesn't yet support tables of composite records, and 
  --  show_breakpoints is going to build a table of these records.)
  --
  TYPE breakpoint_info IS RECORD
  (
     -- These fields are duplicates of 'program_info':
     Name        VARCHAR2(128),
     Owner       VARCHAR2(128),
     Line#       BINARY_INTEGER,

     Status      BINARY_INTEGER   -- see breakpoint_status_* below
  );
  
  -- Breakpoint statuses.  
  --   breakpoint_status_unused - the breakpoint is not in use.
  --
  -- Otherwise the status is a mask of the following values:
  --   breakpoint_status_active - this is a line breakpoint
  --   breakpoint_status_active2 - this is an entry breakpoint (deprecated
  --                               functionality)
  --   breakpoints_status_disabled - this breakpoint is currently disabled
  --   breakpoint_status_remote - this is a 'shadow' breakpoint (a local
  --          representation of a remote breakpoint).
  --    
  -- (Internal note: these map to the PBBPT constants)
  --
  breakpoint_status_unused    CONSTANT BINARY_INTEGER := 0;
  breakpoint_status_active    CONSTANT BINARY_INTEGER := 1;
  breakpoint_status_disabled  CONSTANT BINARY_INTEGER := 4;

  ------------------------------ INDEX_TABLE ------------------------------
  -- Used by get_indexes to return the available indexes for a given
  -- indexed table.  See get_indexes for more details.
  --
  TYPE index_table IS table of BINARY_INTEGER index by BINARY_INTEGER;

  ---------------------------- BACKTRACE_TABLE ----------------------------
  -- Used by print_backtrace.
  TYPE backtrace_table IS TABLE OF program_info INDEX BY BINARY_INTEGER;

  --------------------------- BREAKPOINT_TABLE ---------------------------
  -- Used by show_breakpoints.
  TYPE breakpoint_table IS TABLE OF breakpoint_info INDEX BY BINARY_INTEGER;

  ------------------------------- NAMESPACES -----------------------------
  -- Program units on the server reside in different namespaces.  When
  -- setting a breakpoint it is necessary to specify the desired namespace
  -- (to distinguish between a package spec and a package body, for example).
  --
  -- 1. Namespace_cursor contains cursors (anonymous blocks)
  -- 2. Namespace_pgkspec_or_toplevel contains:
  --       + package specifications
  --       + procedures and functions (that are not nested inside other
  --                                   packages/procedures/functions)
  --       + object types 
  -- 3. Namespace_pkg_body contains package bodies and type bodies.
  -- 4. Namespace_trigger contains triggers.
  -- 5. Namespace_none is used to describe a frame by number,
  --    currently supported by set_breakpoint only.
  --
  -- (Internal note: these map to the KGLN constants)
--
  namespace_cursor               CONSTANT BINARY_INTEGER := 0;
  namespace_pkgspec_or_toplevel  CONSTANT BINARY_INTEGER := 1;
  namespace_pkg_body             CONSTANT BINARY_INTEGER := 2;
  namespace_trigger              CONSTANT BINARY_INTEGER := 3;
  namespace_none                 CONSTANT BINARY_INTEGER := 255;

  --------------------------- BREAK FLAGS --------------------------------
  -- Values to use for the 'breakflags' parameter to continue(), in order
  -- to tell Probe what events are of interest to the client.
  -- These flags may be combined.
  --
  -- (Internal note: these map to the PEDE constants)
  --
  -- Line stepping:
  --   break_next_line - break at next source line (step over calls)
  --   break_any_call  - break at next source line (step into calls)
  --   break_any_return - break after returning from current entrypoint 
  --        (skip over any entrypoints that are called from the current)
  --   break_return     - break the next time an entrypoint gets ready to
  --         return (note that this includes entrypoints called from the
  --         current one.  If interpreter is executing Proc1, which calls
  --         Proc2, then break_return will stop at the end of Proc2.)
  --
  --  (Yes, break_any_return and break_return are backwards.  Too late to
  --   fix it now that we have existing clients...)
  --
  -- Exceptions:
  --   break_exception - break when an exception is raised
  --   break_handler   - break when an exception handler is executed
  --
  -- Execution termination:
  --   abort_execution  - terminate execution and force an 'exit' event
  --     as soon as DBMS_DEBUG.continue is called
  --
  break_exception      CONSTANT PLS_INTEGER :=    2;
  break_any_call       CONSTANT PLS_INTEGER :=   12;  -- 4 | 8
  break_return         CONSTANT PLS_INTEGER :=   16;
  break_next_line      CONSTANT PLS_INTEGER :=   32;
  break_any_return     CONSTANT PLS_INTEGER :=  512;
  break_handler        CONSTANT PLS_INTEGER := 2048;
  abort_execution      CONSTANT PLS_INTEGER := 8192;

  ------------------------- INFORMATION FLAGS ---------------------------
  -- These are flags that can be passed as the 'info_requested' parameter
  -- to synchronize, continue, and get_runtime_info.
  -- 
  -- (Internal note: these map to the PBBA constants)
  --
  info_getStackDepth    CONSTANT PLS_INTEGER := 2;  -- get stack depth
  info_getBreakpoint    CONSTANT PLS_INTEGER := 4;  -- get breakpoint number
  info_getLineinfo      CONSTANT PLS_INTEGER := 8;  -- get program info
  info_getOerInfo       CONSTANT PLS_INTEGER := 32; -- (Probe v2.4)

  ------------------------- REASONS -------------------------------------
  -- Reasons for suspension. After continue is executed, the program will
  -- either run to completion or break on some line. 
  --
  -- (Internal note: these map to the PBEVN constants)
  --
  reason_none        CONSTANT BINARY_INTEGER :=  0;
  reason_interpreter_starting CONSTANT BINARY_INTEGER :=  2;
  reason_breakpoint  CONSTANT BINARY_INTEGER :=  3; -- at a breakpoint
  reason_enter       CONSTANT BINARY_INTEGER :=  6; -- procedure entry
  reason_return      CONSTANT BINARY_INTEGER :=  7; -- procedure return
  reason_finish      CONSTANT BINARY_INTEGER :=  8; -- procedure is finished
  reason_line        CONSTANT BINARY_INTEGER :=  9; -- reached a new line
  reason_interrupt   CONSTANT BINARY_INTEGER := 10; -- an interrupt occurred
  reason_exception   CONSTANT BINARY_INTEGER := 11; -- an exception was raised
  reason_exit        CONSTANT BINARY_INTEGER := 15; -- interpreter is exiting
  reason_handler     CONSTANT BINARY_INTEGER := 16; -- start exception-handler
  reason_timeout     CONSTANT BINARY_INTEGER := 17; -- a timeout occurred
  reason_instantiate CONSTANT BINARY_INTEGER := 20; -- instantiation block
  reason_abort       CONSTANT BINARY_INTEGER := 21; -- interpeter is aborting
  reason_knl_exit    CONSTANT BINARY_INTEGER := 25; -- interpreter is exiting

  ------------------------------ ERROR CODES ------------------------------
  -- These values are returned by the various functions that are called in
  -- the debug session (synchronize, continue, set_breakpoint, etc).
  --
  -- (If PL/SQL exceptions worked across client/server and server/server
  --  boundaries then these would all be exceptions rather than error 
  --  codes.)
  --
  -- (Internal note: these map to the PBERR constants)
  --
  success               CONSTANT binary_integer :=  0;
  -- Statuses returned by GET_VALUE and SET_VALUE:
  error_bogus_frame     CONSTANT binary_integer :=  1;  -- no such frame
  error_no_debug_info   CONSTANT binary_integer :=  2;  -- debuginfo missing
  error_no_such_object  CONSTANT binary_integer :=  3;  -- no such var/parm
  error_unknown_type    CONSTANT binary_integer :=  4;  -- debuginfo garbled
  error_indexed_table   CONSTANT binary_integer := 18;  -- Can't get/set an
                                                        -- entire collection 
                                                        -- at once
  error_illegal_index   CONSTANT binary_integer := 19;  -- illegal collection
                                                        -- index (V8)
  error_nullcollection  CONSTANT binary_integer := 40;  -- collection is 
                                                        -- atomically null 
                                                        -- (V8)
  error_nullvalue     CONSTANT binary_integer := 32;    -- value is null

  -- Statuses returned by set_value:
  error_illegal_value   CONSTANT binary_integer :=  5;  -- constraint vio
  error_illegal_null    CONSTANT binary_integer :=  6;  -- constraint vio
  error_value_malformed CONSTANT binary_integer :=  7;  -- bad value
  error_other           CONSTANT binary_integer :=  8;  -- unknown error
  error_name_incomplete CONSTANT binary_integer := 11;  -- not a scalar lvalue

  -- Statuses returned by the breakpoint functions:
  error_illegal_line    CONSTANT binary_integer := 12;  -- no such line
  error_no_such_breakpt CONSTANT binary_integer := 13;  -- no such breakpoint
  error_idle_breakpt    CONSTANT binary_integer := 14;  -- unused breakpoint
  error_stale_breakpt   CONSTANT binary_integer := 15;  -- pu changed under 
                                                        -- bpt
  error_bad_handle      CONSTANT binary_integer := 16;  -- can't set bpt there

  -- General error codes (returned by many of the dbms_debug routines)
  error_unimplemented   CONSTANT binary_integer := 17;  -- NYI functionality
  error_deferred        CONSTANT binary_integer := 27; -- request was deferred
                                                       -- (currently unused)
  error_exception     CONSTANT binary_integer := 28; -- exception inside Probe
  error_communication CONSTANT binary_integer := 29; -- generic pipe error
  error_timeout       CONSTANT binary_integer := 31; -- timeout


  -- Error codes that only apply to client-side PL/SQL
  error_pbrun_mismatch  CONSTANT binary_integer :=  9;
  error_no_rph          CONSTANT binary_integer := 10;
  error_probe_invalid   CONSTANT binary_integer := 20;
  error_upierr          CONSTANT binary_integer := 21;
  error_noasync         CONSTANT binary_integer := 22;
  error_nologon         CONSTANT binary_integer := 23;
  error_reinit          CONSTANT binary_integer := 24;
  error_unrecognized    CONSTANT binary_integer := 25;
  error_synch           CONSTANT binary_integer := 26;
  error_incompatible    CONSTANT binary_integer := 30;

  --------------------------- TIMEOUT OPTIONS ---------------------------
  -- Timeout options for the target session are registered with the
  -- target session by calling set_timeout_behaviour.
  --
  -- retry_on_timeout - Retry.  Timeout has no effect.  This is like
  --    setting the timeout to an infinitely large value.
  --
  -- continue_on_timeout - Continue execution, using same event flags.
  --
  -- nodebug_on_timeout - Turn debug-mode OFF (ie. call debug_off) and
  --    then continue execution.  No more events will be generated
  --    by this target session unless it is reinitialized by calling
  --    debug_on.
  --
  -- abort_on_timeout - Continue execution, using the abort_execution
  --    flag, which should cause the program to abort immediately.  The
  --    session remains in debug-mode.
  --
  retry_on_timeout      CONSTANT BINARY_INTEGER := 0;
  continue_on_timeout   CONSTANT BINARY_INTEGER := 1;
  nodebug_on_timeout    CONSTANT BINARY_INTEGER := 2;
  abort_on_timeout      CONSTANT BINARY_INTEGER := 3;

  ------------------------------
  --  EXCEPTIONS
  illegal_init         EXCEPTION;  -- DEBUG_ON called prior to INITIALIZE
  
  -- Functionality that is no longer supported.  (Some calls in the 
  -- underlying packages are replaced with calls in dbms_debug.)
  desupported             EXCEPTION;

  unimplemented           EXCEPTION;   -- Not yet implemented.
  target_error            EXCEPTION;   -- problem in target session

  no_target_program       EXCEPTION;   -- target is not running

  ------------------------------
  --  PUBLIC VARIABLES

  -- The timeout value (used by both sessions).
  -- The smallest possible timeout is 1 second - if default_timeout is
  -- set to 0 then a large value (3600) will be used.
  --
  default_timeout  BINARY_INTEGER := 3600;  -- 60 minutes


------------------------ COMMON Section -----------------------------
-- These functions/procedures may be called in either the target or
-- debug session.

  -------------------------- PROBE_VERSION -------------------------
  -- Return the version number of DBMS_DEBUG on the server.
  --
  -- PARAMETERS
  --   major - major version number.
  --   minor - minor version number.  Incremented as functionality is added
  --           to DBMS_DEBUG.
  PROCEDURE probe_version(major out BINARY_INTEGER,
                          minor out BINARY_INTEGER);
  
  ------------------------------ SET_TIMEOUT ------------------------------
  -- Set the timeout value.
  --
  -- PARAMETERS
  --   timeout - the timeout value to set.  If 0, then uses default_timeout
  --
  -- RETURNS 
  --   the new timeout value.
  --
  FUNCTION set_timeout(timeout BINARY_INTEGER) RETURN BINARY_INTEGER;

------------------------ TARGET SESSION Section ------------------------
--
-- These functions and procedures are to be executed in the target session
-- (the session that is to be debugged).
--

  ----------------------- INITIALIZE ---------------------------
  -- Initializes the target session by registering a debugID.
  --
  -- PARAMETERS
  --   debug_session_id - A session-id name to identify the target session.
  --                      If null, a unique ID will be generated.
  --   diagnostics      - whether to dump diagnostic output to the tracefile
  --                      0 = no output, 1 = minimal output
  --   debug_role       - An additional role to use for debugger privilege
  --                      checks.  Will not affect SQL or PL/SQL execution.
  --   debug_role_pwd   - Password for the debug_role, if needed.
  --
  -- RETURNS
  --   the newly-registered debug-session-id (debugID)
  --
  FUNCTION initialize(debug_session_id  IN VARCHAR2       := NULL, 
                      diagnostics       IN BINARY_INTEGER := 0,
                      debug_role        IN VARCHAR2       := NULL,
                      debug_role_pwd    IN VARCHAR2       := NULL)
    RETURN VARCHAR2;

  ----------------------- DEBUG_ON -----------------------------
  -- Mark the target session so that all PL/SQL is executed in
  -- debug mode.  This must be done before any debugging can take 
  -- place.
  -- 
  -- PARAMETERS
  --    no_client_side_plsql_engine - whether this debugging session
  --         is standalone or results from a client-side PL/SQL session.
  --         (Client-side PL/SQL uses different entrypoints into the 
  --         server, and therefore has restricted use of dbms_debug.)
  --    immediate - whether to switch into debug mode immediately, or
  --         to wait until the call has completed.  If TRUE, then a
  --         debug session will be required in order to complete the call.
  --
  -- NOTES
  --   If in doubt, use the default parameter values.
  --
  PROCEDURE debug_on(no_client_side_plsql_engine BOOLEAN := TRUE,
                     immediate                   BOOLEAN := FALSE);

  ----------------------- DEBUG_OFF ----------------------------  
  -- Notify the target session that debugging is no longer to take
  -- place in that session.
  -- It is not necessary to call this function before logging the
  -- session off.
  --
  -- NOTES
  --   The server does not handle this entrypoint specially.  Therefore
  --   it will attempt to debug this entrypoint.
  --
  PROCEDURE debug_off;

  --------------------- SET_TIMEOUT_BEHAVIOUR ------------------
  -- Tell Probe what to do with the target session when a timeout
  -- occurs.
  --
  -- PARAMETERS
  --    behaviour - one of the following (see descriptions above):
  --       retry_on_timeout
  --       continue_on_timeout
  --       nodebug_on_timeout
  --       abort_on_timeout
  --
  -- EXCEPTIONS
  --    unimplemented - the requested behaviour is not recognized
  --
  -- NOTES
  --    The default behaviour (if this procedure is not called)
  --    is continue_on_timeout, since it allows a debugger client
  --    to re-establish control (at the next event) but does not
  --    cause the target session to hang indefinitely.
  --
  PROCEDURE set_timeout_behaviour(behaviour IN PLS_INTEGER);

  --------------------- GET_TIMEOUT_BEHAVIOUR ------------------
  -- Return the current timeout behaviour.
  --
  FUNCTION get_timeout_behaviour RETURN BINARY_INTEGER;


------------------------- DEBUG SESSION Section -------------------------
--
-- These functions and procedures are to be executed in the debug session
-- only.
--

  ----------------------- ATTACH_SESSION -----------------------  
  -- Notify the debug session about the target program.
  --
  -- PARAMETERS
  --   debug_session_id - the debugID from a call to initialize()
  --   diagnostics      - generate diagnostic output (in the rdbms
  --                      tracefile)? 0 = no output, 1 = minimal output
  --
  PROCEDURE attach_session(debug_session_id  IN VARCHAR2,
                           diagnostics       IN BINARY_INTEGER := 0);

  ------------------------------ SYNCHRONIZE ------------------------------
  -- This function:
  --   1. Waits until the target program signals an event.
  --   2. Calls get_runtime_info
  --
  -- PARAMETERS
  --   run_info -  structure in which to write information about the program
  --             By default this will include information about what program
  --             is executing and at which line execution has paused.
  --   info_requested -  optional bit-field in which to request information
  --           other than the default (which is info_getStackDepth +
  --           info_getLineInfo).  0 means that no information is requested.
  --
  -- RETURNS
  --    success
  --    error_timeout   - timed out before the program started execution
  --    error_communication - other communication error
  --
  FUNCTION synchronize(run_info       OUT  runtime_info,
                       info_requested IN   BINARY_INTEGER := NULL)
    RETURN BINARY_INTEGER;

  ---------------------- PRINT_BACKTRACE -------------------
  -- Print a backtrace listing of the current execution stack.
  -- Should only be called if a program is currently executing.
  --
  -- PARAMETERS
  --    listing - A formatted character buffer with embedded newlines
  --
  PROCEDURE print_backtrace(listing IN OUT VARCHAR);

  -- PARAMETERS
  --   backtrace - 1-based indexed table of backtrace entries.  The
  --        currently-executing procedure is the last entry in the
  --        table (ie. the frame numbering is the same as that used by
  --        get_value).  Entry 1 is the oldest procedure on the stack.
  --        
  PROCEDURE print_backtrace(backtrace OUT backtrace_table);

  ----------------------- CONTINUE --------------------------  
  -- Continue execution of the target program.
  --
  -- This function:
  --   1. Passes the given breakflags (a mask of the events that are
  --      of interest) to Probe in the target process.
  --   2. Tells Probe to continue execution of the target process.
  --   3. Waits until the target process either runs to completion or
  --      signals an event.
  --   4. If info_requested is not 0 and program has not terminated, 
  --      calls get_runtime_info.
  -- 
  -- PARAMETERS
  --   run_info       - information about the new state of the program
  --   breakflags     - mask of events that are of interest
  --   info_requested - what information should be returned in 'run_info'
  --                    when the program stops.  See 'information flags'
  --                    0 means no information.  Null means default info.
  -- RETURNS
  --    success
  --    error_timeout       - timeout while waiting for target session
  --    error_communication - other communication error
  --
  -- HANDLING TIMEOUTS
  --    If this function returns with a timeout then the target session
  --    either terminated abnormally or is still running.  If it is still
  --    running, and the client wishes to continue waiting for it, then
  --    'synchronize' should be called.
  --
  FUNCTION continue(run_info       IN OUT runtime_info,
                    breakflags     IN     BINARY_INTEGER,
                    info_requested IN     BINARY_INTEGER := null)
    RETURN BINARY_INTEGER;

  -------------------------- SET_BREAKPOINT ----------------------
  -- Set a breakpoint in a program unit, which persists for the current
  -- session.  Execution will pause if the target program reaches the
  -- breakpoint.
  --
  -- PARAMETERS
  --   program      Information about the program unit in which the 
  --                breakpoint is to be set.
  --   line#        The line at which the breakpoint is to be set.
  --   breakpoint#  On successful completion, will contain the unique
  --                breakpoint number by which to refer to the breakpoint.
  --   fuzzy        Only applicable if there is no executable code at
  --                the specified line:
  --                  0 means return error_illegal_line.
  --                  1 means search forward for an adjacent line at which
  --                    to place the breakpoint.
  --                 -1 means search backwards for an adjacent line at
  --                    which to place the breakpoint.
  --   iterations   The number of times to wait before signalling this
  --                breakpoint.
  --
  -- RETURNS
  --   success
  --   error_illegal_line  - can't set a breakpoint at that line
  --   error_bad_handle    - no such program unit exists
  --
  -- RESTRICTIONS/BUGS
  --   'fuzzy' and 'iterations' not yet implemented.
  --
  FUNCTION set_breakpoint(program     IN  program_info,
                          line#       IN  BINARY_INTEGER,
                          breakpoint# OUT BINARY_INTEGER,
                          fuzzy       IN  BINARY_INTEGER := 0,
                          iterations  IN  BINARY_INTEGER := 0)
    RETURN BINARY_INTEGER;

  -------------------------- DELETE_BREAKPOINT ----------------------
  -- Deletes a breakpoint.
  --
  -- PARAMETERS
  --   breakpoint - a breakpoint number returned by SET_BREAKPOINT
  --
  -- RETURNS
  --   success
  --   error_no_such_breakpt - no such breakpoint exists
  --   error_idle_breakpt    - breakpoint was already deleted
  --   error_stale_breakpt   - the program unit was redefined since the
  --                           breakpoint was set
  --
  FUNCTION delete_breakpoint(breakpoint IN BINARY_INTEGER)
    RETURN BINARY_INTEGER;

  -------------------------- DISABLE_BREAKPOINT ----------------------
  -- With this procedure the breakpoint will still be there, but not be 
  -- active. After disabling the breakpoint needs to be enabled to make it
  -- active
  --
  -- PARAMETERS
  --   breakpoint - a breakpoint number returned by SET_BREAKPOINT
  --
  -- RETURNS
  --   success
  --   error_no_such_breakpt - no such breakpoint exists
  --   error_idle_breakpt    - breakpoint was already deleted
  --   error_stale_breakpt   - the program unit was redefined since the
  --                           breakpoint was set
  --
  FUNCTION disable_breakpoint(breakpoint IN BINARY_INTEGER)
    RETURN BINARY_INTEGER;

  -------------------------- ENABLE_BREAKPOINT ----------------------
  -- Reverse of disabling. This procedure "activates" an exsiting breakpoint
  --
  -- PARAMETERS
  --   breakpoint - a breakpoint number returned by SET_BREAKPOINT
  --
  -- RETURNS
  --   success
  --   error_no_such_breakpt - no such breakpoint exists
  --   error_idle_breakpt    - breakpoint was already deleted
  --   error_stale_breakpt   - the program unit was redefined since the
  --                           breakpoint was set
  --
  FUNCTION enable_breakpoint(breakpoint IN BINARY_INTEGER)
    RETURN BINARY_INTEGER;

  -------------------------- SHOW_BREAKPOINTS ----------------------
  -- Return a listing of the current breakpoints.
  --
  -- PARAMETERS
  --  listing - a formatted buffer (including newlines) of the breakpoints
  PROCEDURE show_breakpoints(listing    IN OUT VARCHAR2);

  -- PARAMETERS
  --   listing - indexed table of breakpoint entries.  The
  --       breakpoint number is indicated by the index into the table.
  --       Breakpoint numbers start at 1 and are reused when deleted.
  --
  PROCEDURE show_breakpoints(listing  OUT breakpoint_table);

  ------------------------------- GET_VALUE -------------------------------
  -- Get a value from the currently-executing program.
  --
  -- PARAMETERS
  --  variable_name  the name of the variable or parameter
  --  frame#         the frame in which it lives (0 means the current 
  --                   procedure)
  --  scalar_value   its value
  --  format         an optional date format to use, if meaningful.
  --
  -- RETURNS
  --   success
  --   error_bogus_frame    - frame# does not exist
  --   error_no_debug_info  - entrypoint has no debug information
  --   error_no_such_object - variable_name does not exist in frame#
  --   error_unknown_type   - the type information in the debug information
  --                          is illegible
  --   error_nullvalue      - value is null
  --
  -- NOTES
  --   If the variable is a cursor, then a special string value is returned,
  --   which the caller is expected to parse.  See the comments at the
  --   'Cursor flags' section above.
  --
  -- BUGS
  --   - There are situations when the cursor flags may appear to be 
  --     incorrect: one common case is recursive cursors (ie. a procedure 
  --     containing a cursor, where the procedure calls itself recursively).
  --     On return from the recursive call the cursor flags are *not* 
  --     restored, even though the interpreter behaves correctly.  
  --     The difficulty is that both PL/SQL and the kernel maintain their
  --     own cursor-caches, and it is difficult for Probe to follow the 
  --     mapping in these situations.  
  --     This will be fixed in a later release.
  --
  FUNCTION get_value(variable_name  IN  VARCHAR2,
                     frame#         IN  BINARY_INTEGER,
                     scalar_value   OUT VARCHAR2,
                     format         IN  VARCHAR2 := NULL)
    RETURN BINARY_INTEGER;

  --
  -- This form of get_value is for fetching package variables.
  -- Instead of a frame#, it takes a handle, which describes the package
  -- containing the variable.
  --
  -- PARAMETERS
  --    (See description above for the other parameters.)
  --    handle -  package description.  The 'Name,' 'Owner,' and Namespace 
  --              fields  must be initialized appopriately.
  --
  -- RETURNS
  --   error_no_such_object if:
  --       1. the package does not exist
  --       2. the package is not instantiated
  --       3. the user does not have privileges to debug the package
  --       4. the object does not exist in the package
  --
  -- EXAMPLE
  --   Given a package PACK in schema SCOTT, containing variable VAR,
  --   do the following to get its value:
  --
  --   DECLARE
  --      handle     dbms_debug.program_info;
  --      resultbuf  VARCHAR2(500);
  --   BEGIN
  --      handle.Owner := 'SCOTT';
  --      handle.Name  := 'PACK';
  --      handle.namespace := dbms_debug.namespace_pkgspec_or_toplevel;
  --      retval := dbms_debug.get_value('VAR', handle, resultbuf, NULL);
  --   END;
  --
  FUNCTION get_value(variable_name  IN  VARCHAR2,
                     handle         IN  program_info,
                     scalar_value   OUT VARCHAR2,
                     format         IN  VARCHAR2 := NULL)
     RETURN BINARY_INTEGER;

  -------------------------- DETACH_SESSION ----------------------
  -- Detach from the currently attached session - ie. stop debugging
  -- the target program.  This procedure may be called at any time,
  -- but it does not notify the target session that the debug session
  -- is detaching itself, and it does not abort execution of the target
  -- session.  Therefore care should be taken to ensure that the target
  -- session does not hang itself.
  --
  PROCEDURE detach_session;

  --------------------------- GET_RUNTIME_INFO ---------------------------
  -- This function returns information about the current program.  It is
  -- only needed if the 'info_requested' parameter to synchronize or
  -- continue was set to 0.
  --
  -- Currently only used by client-side PL/SQL.
  --
  -- PARAMETERS
  --   info_requested - bitmask of the information to fetch.  0 means 
  --                    nothing, null means 'default information.'
  --   run_info - location in which to stash the requested information.
  --
  -- RETURNS
  --   success
  --   error_timeout       - pipe timed out
  --   error_communication - other communication error
  --
  FUNCTION get_runtime_info(info_requested  IN  BINARY_INTEGER,
                            run_info        OUT runtime_info)
    RETURN BINARY_INTEGER;

  ------------------------ TARGET_PROGRAM_RUNNING ------------------------
  -- Return TRUE if the target session is currently executing a stored
  -- procedure, or FALSE if it is not.
  --
  FUNCTION target_program_running RETURN BOOLEAN;


  ------------------------------- PING ---------------------------------
  -- Ping the target session, to prevent it from timing out.  This
  -- procedure is intended for use when execution is suspended in the
  -- target session (for example at a breakpoint).
  --
  -- If the timeout_behaviour is set to retry_on_timeout then this
  -- procedure is not strictly necessary.
  --
  -- EXCEPTIONS
  --   no_target_program will be raised if there is no target program
  --      or if the target session is not currently waiting for input
  --      from the debug session.
  --
  PROCEDURE ping;

  ----------------------------- GET_LINE_MAP ------------------------------
    -- Get information about line numbers in program unit
    --
    -- Finds program unit and returns highest source line number, number
    -- of entry points, and a line map that allows to determine which
    -- lines are executable (or, to be precise, whether user can install
    -- a break-point or step on that line)
    --
    -- Line map is represented as a bitmap. If line number N is executable,
    -- bit number N MOD 8 will be set to 1 at linemap position N / 8.
    -- The length of returned linemap is either maxline divided by 8
    -- (plus one if maxline MOD 8 is not zero) or 32767 in the unlikely
    -- case of maxline being larger than 32767 * 8.
    --
    -- RETURNS:
    --   error_no_debug_info - line map is not available
    --   error_bad_handle    - if program unit info could not be found
    --   success             - successfull completion
    FUNCTION get_line_map(program IN program_info,
                          maxline OUT BINARY_INTEGER,
                          number_of_entry_points OUT BINARY_INTEGER,
                          linemap OUT raw)
      RETURN binary_integer;

    ----------------------------- GET_ALL_VALUES ------------------------------
    FUNCTION get_values(scalar_values OUT VARCHAR2)
      RETURN binary_integer;

END DBMS_DEBUG;
//


