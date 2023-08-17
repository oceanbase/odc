CREATE OR REPLACE PACKAGE BODY DBMS_CRYPTO AS
  FUNCTION ENCRYPT(
      src                 IN RAW,
      typ                 IN PLS_INTEGER,
      key                 IN RAW,
      iv                  IN RAW DEFAULT NULL
  )
  RETURN RAW AS
      ret  raw(2000);
  BEGIN
    ret := dbms_crypto_encrypt(src, typ, key, iv);
    RETURN RET; 
  END;

  FUNCTION DECRYPT (
      src             IN RAW,
      typ             IN PLS_INTEGER,
      key             IN RAW,
      iv              IN RAW DEFAULT NULL
  )
  RETURN RAW AS
      ret raw(2000);
  BEGIN
      ret := dbms_crypto_decrypt(src, typ, key, iv);
      RETURN RET;
  END;

  FUNCTION Hash (
      src IN RAW,
      typ IN PLS_INTEGER)
  RETURN RAW DETERMINISTIC;

  -- PRAGMA INTERFACE(c, dbms_crypto_hash);

  FUNCTION Hash (
      src IN BLOB,
      typ IN PLS_INTEGER)
  RETURN RAW DETERMINISTIC;

  -- PRAGMA INTERFACE(c, dbms_crypto_hash);

  FUNCTION Hash (
      src IN CLOB CHARACTER SET ANY_CS,
      typ IN PLS_INTEGER)
  RETURN RAW DETERMINISTIC;

  -- PRAGMA INTERFACE(c, dbms_crypto_hash);

END DBMS_CRYPTO;
//
