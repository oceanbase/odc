CREATE OR REPLACE PACKAGE dbms_random AS
  ------------
  --  OVERVIEW
  --
  --  This package should be installed as SYS.  It generates a sequence of
  --  random 38-digit Oracle numbers.  The expected length of the sequence
  --  is about power(10,28), which is hopefully long enough.
  --
  --------
  --  USAGE
  --
  --  This is a random number generator.  Do not use for cryptography.
  --  For more options the cryptographic toolkit should be used.
  --
  --  By default, the package is initialized with the current user
  --  name, current time down to the second, and the current session.
  --
  --  If this package is seeded twice with the same seed, then accessed
  --  in the same way, it will produce the same results in both cases.
  --
  --------
  --  EXAMPLES
  --
  --  To initialize or reset the generator, call the seed procedure as in:
  --      execute dbms_random.seed(12345678);
  --    or
  --      execute dbms_random.seed(TO_CHAR(SYSDATE,'MM-DD-YYYY HH24:MI:SS'));
  --  To get the random number, simply call the function, e.g.
  --      my_random_number BINARY_INTEGER;
  --      my_random_number := dbms_random.random;
  --    or
  --      my_random_real NUMBER;
  --      my_random_real := dbms_random.value;
  --  To use in SQL statements:
  --      select dbms_random.value from dual;
  --      insert into a values (dbms_random.value);
  --      variable x NUMBER;
  --      execute :x := dbms_random.value;
  --      update a set a2=a2+1 where a1 < :x;

  -- init the seed with string
  PROCEDURE seed(val IN VARCHAR2);
  -- init the seed with integer 
  -- PROCEDURE seed(val IN BINARY_INTEGER);

  -- return the random value
  FUNCTION value RETURN NUMBER;

  -- return a random nubmer x, low <= x < high
  -- function overload is not support right now.
  FUNCTION  value(low IN NUMBER, high IN NUMBER) RETURN NUMBER;

  -- retrun a random string
  -- opt: specifies the retrun character type
  --      'u U' : upper case alpha charaters only
  --      'l L' : lower case alpha charaters only
  --      'a A' : alpha charaters only (mix case)
  --      'x X' : any alpha-numeric charaters (upper)
  --      'p P' : any printable characters
  FUNCTION  string(opt IN CHAR, len IN NUMBER) RETURN VARCHAR2;

  -- user define type, an array with number type
  -- TYPE num_arr IS TABLE OF NUMBER INDEX BY PLS_INTEGER;
  TYPE num_arr IS TABLE OF NUMBER;

  -- return a random value from normal distribution
  -- box-muller transform, alg from knuth II
  FUNCTION normal RETURN NUMBER;

  -- Obsolete, just call seed(val)
  PROCEDURE initialize(val IN BINARY_INTEGER);

  -- Obsolete, get integer in ( -power(2,31) <= random < power(2,31))
  FUNCTION random RETURN BINARY_INTEGER;

END dbms_random;
//
