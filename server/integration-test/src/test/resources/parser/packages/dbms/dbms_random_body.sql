CREATE OR REPLACE PACKAGE BODY dbms_random AS
  mem       num_arr := num_arr();
  cnt       INTEGER := 55;
  need_init BOOLEAN := TRUE;
  saved_norm NUMBER := 0;

--  PROCEDURE seed(val IN BINARY_INTEGER) IS
--  BEGIN
--    seed(TO_CHAR(val));
--  END seed;

  PROCEDURE seed(val IN VARCHAR2) IS
    junk    VARCHAR2(2000);
    piece   VARCHAR2(20);
    randval NUMBER;
    tmp     NUMBER;
    j       INTEGER;

  BEGIN
    need_init     := FALSE;
    cnt           := 0;
    mem.delete;
    mem.extend(55);
    -- junk      := TO_SINGLE_BYTE(val);
    -- should use to_single_byte, which is not support by ob right now
    junk          := val;
    FOR i IN 1 .. 55 LOOP
      piece       := SUBSTR(junk, 1, 19);
      randval     := 0;
      j           := 1;

      FOR j IN 1 .. 19 LOOP
        randval   := 100 * randval + NVL(ASCII(SUBSTR(piece, j, 1)), 0.0);
      END LOOP;

      -- bug: randval * 1e-38 = 0, this will cause seed is same even param is different
      -- randval     := randval * 1e-38 + i * 0.01020304050607080910111213141516171819;
      randval     := randval * 0.00000000000000000000000000000000000001 + i * 0.01020304050607080910111213141516171819;
      mem(i)      := randval - TRUNC(randval);

      junk        := SUBSTR(junk, 20);
    END LOOP;

    randval       := mem(55);
    FOR j IN 0 .. 10 LOOP
      FOR i IN 1 .. 55 LOOP
        randval   := randval * 1000000000000000000000000;
        tmp       := TRUNC(randval);
        randval   := (randval - tmp) + (tmp * 0.00000000000000000000000000000000000001);

        randval   := mem(i) + randval;
        IF (randval >= 1.0) THEN
          randval := randval - 1.0;
        END IF;
        mem(i)    := randval;
      END LOOP;
    END LOOP;
  END seed;

  FUNCTION value RETURN NUMBER IS
  rndval NUMBER;
  BEGIN
    cnt                := cnt + 1;
    IF cnt >= 56 THEN
      IF (need_init) THEN
        seed(TO_CHAR(SYSDATE, 'MM-DD-YYYY HH24:MI:SS') || USER || USERENV('SESSIONID'));
      ELSE
        FOR i IN 1 .. 31 LOOP
          rndval       := mem(i + 24) + mem(i);
          IF (rndval >= 1.0) THEN
            rndval     := rndval - 1.0;
          END IF;
          mem(i)       := rndval;
        END LOOP;
        FOR i IN 32 .. 55 LOOP 
          rndval := mem(i - 31) + mem(i);
          IF (rndval >= 1.0) THEN
            rndval     := rndval - 1.0;
          END IF;
          mem(i)       := rndval;
        END LOOP;
      END IF; 
      cnt              := 1;
    END IF;
    RETURN mem(cnt);
  END value;

  FUNCTION value(low IN NUMBER, high IN NUMBER) RETURN NUMBER IS
  BEGIN
   RETURN (value() * (high-low)) + low;
  END value;

  -- Random string.  Pilfered from Chris Ellis.
  FUNCTION string (opt IN CHAR, len IN NUMBER)
    RETURN VARCHAR2 IS
    optx char (1)  := lower(opt);
    rng  NUMBER;
    n    INTEGER;
    -- candidate character subset
    ccs  VARCHAR2 (128);
    xstr VARCHAR2 (4000) := NULL;
  BEGIN
    -- upper case alpha characters only
    IF  optx = 'u' THEN
        ccs := 'ABCDEFGHIJKLMNOPQRSTUVWXYZ';
        rng := 26;
    -- lower case alpha characters only
    ELSIF optx = 'l' THEN
        ccs := 'abcdefghijklmnopqrstuvwxyz';
        rng := 26; 
    -- alpha characters only (mixed case)
    ELSIF optx = 'a' THEN
        ccs := 'ABCDEFGHIJKLMNOPQRSTUVWXYZ' ||
              'abcdefghijklmnopqrstuvwxyz';
        rng := 52;
    -- any alpha-numeric characters (upper)
    ELSIF optx = 'x' THEN
        ccs := '0123456789' ||
              'ABCDEFGHIJKLMNOPQRSTUVWXYZ';
        rng := 36;
    -- any printable char (ASCII subset)
    ELSIF optx = 'p' THEN
        ccs := ' !"$%&''()*+,-./' || '0123456789' || ':;<=>?@' ||
              'ABCDEFGHIJKLMNOPQRSTUVWXYZ' || '[\]^_`' ||
              'abcdefghijklmnopqrstuvwxyz' || '{|}~' ;
        rng := 95;
    ELSE
        ccs := 'ABCDEFGHIJKLMNOPQRSTUVWXYZ';
        rng := 26;
    END IF;
    IF len > 0 THEN
      FOR i IN 1 .. least(len,4000) LOOP
        -- Get random integer within specified range
        n := TRUNC(rng * value()) + 1;
        -- Append character to string
        xstr := xstr || SUBSTR(ccs,n,1);
      END LOOP;
    ELSE
      xstr := NULL;
    END IF;
    RETURN xstr;
  END string;

  FUNCTION normal RETURN NUMBER IS
  v1          NUMBER;
  v2          NUMBER;
  r2          NUMBER;
  fac         NUMBER;
  BEGIN
    -- saved from last time
    IF saved_norm != 0 THEN         
    v1         := saved_norm;
    saved_norm := 0;
    ELSE
      r2 := 2;
      -- Find two independent uniform variables
      WHILE r2 > 1 OR r2 = 0 LOOP
        v1 := value();
        v1 := v1 + v1 - 1;
        v2 := value();
        v2 := v2 + v2 - 1;
        -- r2 is radius
        r2 := v1 * v1 + v2 * v2;
        -- 0 < r2 <= 1:  in unit circle
      END LOOP;                            
      -- Now derive two independent normally-distributed variables
      fac        := sqrt(-2*ln(r2) / r2);
      -- to be returned this time
      v1         := v1*fac;
      -- to be saved for next time
      saved_norm := v2*fac;
    END IF;
    RETURN v1;
  END normal;

  -- For compatibility with oracle
  PROCEDURE initialize(val IN BINARY_INTEGER) IS
  BEGIN
    seed(TO_CHAR(val));
  END initialize;

  -- For compatibility with oracle
  FUNCTION random RETURN BINARY_INTEGER IS
  BEGIN
    RETURN TRUNC(value() * 4294967296) - 2147483648;
  END random;
END dbms_random;
//
