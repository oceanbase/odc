CREATE OR REPLACE PACKAGE BODY dbms_output AS
  enabled         BOOLEAN DEFAULT FALSE;
  buf_size        INTEGER;
  linebuflen      INTEGER DEFAULT 0;
  putidx          INTEGER DEFAULT 1;
  getidx          INTEGER DEFAULT 1;
  get_in_progress BOOLEAN DEFAULT TRUE;
  TYPE            char_arr IS TABLE OF VARCHAR(32767);
  buf             char_arr := char_arr();
  bufleft         INTEGER DEFAULT -1;

  PROCEDURE enable(buffer_size IN INTEGER DEFAULT 20000) IS
  BEGIN
    enabled := true;
    IF buffer_size < 2000 THEN
      buf_size := 2000;
    ELSIF buffer_size > 1000000 THEN
      buf_size := 1000000;
    ELSIF buffer_size IS NULL THEN
      buf_size := -1;
    ELSE
      buf_size := buffer_size;
    END IF;
    bufleft := buf_size;
  END;

  PROCEDURE disable IS
  BEGIN
    enabled := false;
    buf.delete;
    buf.extend;
    putidx := 1;
    buf(putidx) := '';
    get_in_progress := true;
  END;

  PROCEDURE put_init IS
  BEGIN
    buf.delete;
    buf.extend;
    putidx := 1;
    buf(putidx) := '';
    linebuflen := 0;
    bufleft := buf_size;
    get_in_progress := false;
  END;

  PROCEDURE put(a VARCHAR2) IS
    strlen BINARY_INTEGER;
    line_overflow EXCEPTION;
    PRAGMA EXCEPTION_INIT(line_overflow, -4019);
    buff_overflow EXCEPTION;
    PRAGMA EXCEPTION_INIT(buff_overflow, -4024);
  BEGIN
    IF enabled THEN
      IF get_in_progress THEN -- clear buffer
        put_init;
      END IF;

      strlen := NVL(LENGTH(a), 0);
      IF ((strlen + linebuflen) > 32767) THEN
        linebuflen := 0;
        buf(putidx) := '';
        RAISE line_overflow;
      END IF;

      IF (buf_size <> -1) THEN
        IF (strlen > bufleft) THEN  
          RAISE buff_overflow;
        END IF;
        bufleft := bufleft - strlen;
      END IF;

      buf(putidx) := buf(putidx) || a;
      linebuflen := linebuflen + strlen;
    END IF;
  END;

  PROCEDURE put_line(a VARCHAR2) IS
  BEGIN
    IF enabled THEN
      put(a);
      new_line;
    END IF;
  END;

  PROCEDURE new_line IS
  BEGIN
    IF enabled THEN
      IF get_in_progress THEN
        put_init;
      END IF;
      linebuflen := 0;
      buf.EXTEND;
      putidx := putidx + 1;
      buf(putidx) := '';
    END IF;
  END;

  -- get one line once, if we call put after get_line, data will be cleared
  PROCEDURE get_line(line OUT VARCHAR2, status OUT INTEGER) IS
  BEGIN
    IF NOT enabled THEN
      status := 1;
      RETURN;
    END IF;
    
    IF NOT get_in_progress THEN
      get_in_progress := true;
      IF (linebuflen > 0) and (putidx = 1) THEN -- at first line and only one line
        status := 1;
        RETURN;
      END IF;
      getidx := 1; -- we get line from first line
    END IF;

    WHILE getidx < putidx LOOP
      line := buf(getidx); -- get current line
      getidx := getidx + 1; -- ready to get next line
      status := 0;
      RETURN;
    END LOOP;
    
    status := 1;
    RETURN;
  END;
END dbms_output;
//
