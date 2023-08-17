CREATE OR REPLACE PACKAGE DBMS_LOB AS
  
-- CONSTANTS
  lob_readonly  CONSTANT BINARY_INTEGER := 0;
  lob_readwrite CONSTANT BINARY_INTEGER := 1;
  call    CONSTANT PLS_INTEGER  := 12;
  transaction CONSTANT PLS_INTEGER  := 11;
  session CONSTANT PLS_INTEGER  := 10;

-- FUNCTIONS and PROCEDURES
  FUNCTION GETLENGTH (
    lob_loc       IN BLOB
  )
  RETURN NUMBER;

  FUNCTION GETLENGTH (
    lob_loc       IN CLOB CHARACTER SET ANY_CS
  )
  RETURN NUMBER;
  -- This function gets the length of the specified LOB. The length in bytes or characters is returned.

  FUNCTION SUBSTR (
     lob_loc     IN    BLOB,
     amount      IN    INTEGER := 32767,
     offset      IN    INTEGER := 1)
    RETURN RAW;

  FUNCTION SUBSTR (
     lob_loc     IN    CLOB CHARACTER SET ANY_CS,
     amount      IN    INTEGER := 32767,
     offset      IN    INTEGER := 1)
    RETURN VARCHAR2;
  -- This function returns amount bytes or characters of a LOB, starting from an absolute offset from the beginning of the LOB.
  -- For fixed-width n-byte CLOBs, if the input amount for SUBSTR is greater than (32767/n), then SUBSTR returns a character buffer of length (32767/n), or the length of the CLOB, whichever is lesser.
  -- For CLOBs in a varying-width character set, n is the maximum byte-width used for characters in the CLOB.

  FUNCTION ISOPEN (
     lob_loc IN BLOB) 
    RETURN INTEGER; 

  FUNCTION ISOPEN (
     lob_loc IN CLOB CHARACTER SET ANY_CS) 
    RETURN INTEGER; 
  -- This function checks to see if the LOB was already opened using the input locator. This subprogram is for internal and external LOBs.
  
  FUNCTION INSTR (
     lob_loc    IN   BLOB,
     pattern    IN   RAW,
     offset     IN   INTEGER := 1,
     nth        IN   INTEGER := 1)
    RETURN INTEGER;

  FUNCTION INSTR (
     lob_loc    IN   CLOB CHARACTER SET ANY_CS,
     pattern    IN   VARCHAR2 CHARACTER SET ANY_CS,
     offset     IN   INTEGER := 1,
     nth        IN   INTEGER := 1)
    RETURN INTEGER;      
  -- This function returns the matching position of the nth occurrence of the pattern in the LOB, starting from the offset you specify.

  PROCEDURE OPEN (
     lob_loc   IN OUT NOCOPY BLOB,
     open_mode IN            BINARY_INTEGER);
   
  PROCEDURE OPEN (
     lob_loc   IN OUT NOCOPY CLOB CHARACTER SET ANY_CS,
     open_mode IN            BINARY_INTEGER);
  -- This procedure opens a LOB, internal or external, in the indicated mode. Valid modes include read-only, and read/write.

  PROCEDURE CLOSE (
     lob_loc    IN OUT NOCOPY BLOB); 

  PROCEDURE CLOSE (
     lob_loc    IN OUT NOCOPY CLOB CHARACTER SET ANY_CS); 
  -- This procedure closes a previously opened internal or external LOB.

  PROCEDURE CREATETEMPORARY (
     lob_loc IN OUT NOCOPY BLOB,
     cache   IN            BOOLEAN,
     dur     IN            PLS_INTEGER := 10);

  PROCEDURE CREATETEMPORARY (
     lob_loc IN OUT NOCOPY CLOB CHARACTER SET ANY_CS,
     cache   IN            BOOLEAN,
     dur     IN            PLS_INTEGER := 10);
  -- This procedure creates a temporary BLOB or CLOB and its corresponding index in your default temporary tablespace.

  PROCEDURE FREETEMPORARY (
     lob_loc  IN OUT  NOCOPY BLOB);

  PROCEDURE FREETEMPORARY (
     lob_loc  IN OUT  NOCOPY CLOB CHARACTER SET ANY_CS);
  -- This procedure frees the temporary BLOB or CLOB in the default temporary tablespace.

  PROCEDURE COPY (
    dest_lob    IN OUT NOCOPY BLOB,
    src_lob     IN            BLOB,
    amount      IN            INTEGER,
    dest_offset IN            INTEGER := 1,
    src_offset  IN            INTEGER := 1);

  PROCEDURE COPY (
    dest_lob    IN OUT NOCOPY CLOB CHARACTER SET ANY_CS,
    src_lob     IN            CLOB CHARACTER SET ANY_CS,
    amount      IN            INTEGER,
    dest_offset IN            INTEGER := 1,
    src_offset  IN            INTEGER := 1);
  -- This procedure copies all, or a part of, a source internal LOB to a destination internal LOB.

  PROCEDURE TRIM (
     lob_loc        IN OUT  NOCOPY BLOB,
     newlen         IN             INTEGER);

  PROCEDURE TRIM (
     lob_loc        IN OUT  NOCOPY CLOB CHARACTER SET ANY_CS,
     newlen         IN             INTEGER);
  -- This procedure trims the value of the internal LOB to the length you specify in the newlen parameter. Specify the length in bytes for BLOBs, and specify the length in characters for CLOBs.
  
  PROCEDURE APPEND (
     dest_lob IN OUT  NOCOPY BLOB, 
     src_lob  IN             BLOB);
  
  PROCEDURE APPEND (
    dest_lob  IN OUT NOCOPY CLOB CHARACTER SET ANY_CS, 
    src_lob   IN            CLOB CHARACTER SET ANY_CS);
  -- This procedure appends the contents of a source internal LOB to a destination LOB. It appends the complete source LOB.

  PROCEDURE WRITE (
     lob_loc  IN OUT NOCOPY  BLOB,
     amount   IN             INTEGER,
     offset   IN             INTEGER,
     buffer   IN             RAW);
  
  PROCEDURE WRITE (
     lob_loc  IN OUT  NOCOPY CLOB CHARACTER SET ANY_CS,
     amount   IN             INTEGER,
     offset   IN             INTEGER,
     buffer   IN             VARCHAR2 CHARACTER SET ANY_CS); 
  -- This procedure writes a specified amount of data into an internal LOB, starting from an absolute offset from the beginning of the LOB. The data is written from the buffer parameter.
  -- WRITE replaces (overwrites) any data that already exists in the LOB at the offset, for the length you specify.
  
  PROCEDURE WRITEAPPEND (
    lob_loc IN OUT NOCOPY BLOB,
    amount  IN            INTEGER,
    buffer  IN            RAW);

  PROCEDURE WRITEAPPEND (
    lob_loc IN OUT NOCOPY CLOB CHARACTER SET ANY_CS,
    amount  IN            INTEGER,
    buffer  IN            VARCHAR2 CHARACTER SET ANY_CS);
  -- This procedure writes a specified amount of data to the end of an internal LOB. The data is written from the buffer parameter.

  PROCEDURE ERASE (
    lob_loc           IN OUT   NOCOPY   BLOB,
    amount            IN OUT   NOCOPY   INTEGER,
    offset            IN                INTEGER := 1);

  PROCEDURE ERASE (
    lob_loc           IN OUT   NOCOPY   CLOB CHARACTER SET ANY_CS,
    amount            IN OUT   NOCOPY   INTEGER,
    offset            IN                INTEGER := 1);
  -- This procedure erases an entire internal LOB or part of an internal LOB.

  PROCEDURE READ (
     lob_loc   IN             BLOB,
     amount    IN OUT NOCOPY  INTEGER,
     offset    IN             INTEGER,
     buffer    OUT            RAW);
  
  PROCEDURE READ (
    lob_loc   IN             CLOB CHARACTER SET ANY_CS,
    amount    IN OUT NOCOPY  INTEGER,
    offset    IN             INTEGER,
    buffer    OUT            VARCHAR2 CHARACTER SET ANY_CS);
  -- This procedure reads a piece of a LOB, and returns the specified amount into the buffer parameter
  -- starting from an absolute offset from the beginning of the LOB.

  PROCEDURE CONVERTTOBLOB (
    dest_lob       IN OUT     BLOB,
    src_clob       IN         CLOB,
    amount         IN         INTEGER,
    dest_offset    IN OUT     INTEGER,
    src_offset     IN OUT     INTEGER, 
    blob_csid      IN         NUMBER,
    lang_context   IN OUT     INTEGER,
    warning        OUT        INTEGER); 
  -- This procedure reads character data from a source CLOB or NCLOB instance, converts the character data to the character set you specify, writes the converted data to a destination BLOB instance in binary format, and returns the new offsets.
  -- You can use this interface with any combination of persistent or temporary LOB instances as the source or destination.
  
END DBMS_LOB;
//
