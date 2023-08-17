CREATE OR REPLACE PACKAGE DBMS_CRYPTO AS
  -- Hash Functions
  HASH_MD4           CONSTANT PLS_INTEGER            :=     1;
  HASH_MD5           CONSTANT PLS_INTEGER            :=     2;
  HASH_SH1           CONSTANT PLS_INTEGER            :=     3;
  HASH_SH256         CONSTANT PLS_INTEGER            :=     4;
  HASH_SH384         CONSTANT PLS_INTEGER            :=     5;
  HASH_SH512         CONSTANT PLS_INTEGER            :=     6;

  -- Block Cipher Algorithms
  ENCRYPT_AES128     CONSTANT PLS_INTEGER            :=     6;  -- 0x0006
  ENCRYPT_AES192     CONSTANT PLS_INTEGER            :=     7;  -- 0x0007
  ENCRYPT_AES256     CONSTANT PLS_INTEGER            :=     8;  -- 0x0008

  -- Block Cipher Chaining Modifiers
  CHAIN_CBC          CONSTANT PLS_INTEGER            :=   256;  -- 0x0100
  CHAIN_ECB          CONSTANT PLS_INTEGER            :=   768;  -- 0x0300
  -- Block Cipher Padding Modifiers
  PAD_PKCS5          CONSTANT PLS_INTEGER            :=  4096;  -- 0x1000

  FUNCTION ENCRYPT(
      src                 IN RAW,
      typ                 IN PLS_INTEGER,
      key                 IN RAW,
      iv                  IN RAW DEFAULT NULL
  )
  RETURN RAW;

  FUNCTION DECRYPT (
      src             IN RAW,
      typ             IN PLS_INTEGER,
      key             IN RAW,
      iv              IN RAW DEFAULT NULL
  )
  RETURN RAW;

  ------------------------------------------------------------------------
  --
  -- NAME:  Hash
  --
  -- DESCRIPTION:
  --
  --   Hash source data by cryptographic hash type.
  --
  -- PARAMETERS
  --
  --   source    - Source data to be hashed
  --   hash_type - Hash algorithm to be used
  --
  -- USAGE NOTES:
  --   SHA-1 (HASH_SH1) is recommended.  Consider encoding returned
  --   raw value to hex or base64 prior to storage.
  --
  ------------------------------------------------------------------------

  FUNCTION Hash (src IN RAW,
                 typ IN PLS_INTEGER)
    RETURN RAW DETERMINISTIC;

  FUNCTION Hash (src IN BLOB,
                 typ IN PLS_INTEGER)
    RETURN RAW DETERMINISTIC;

  FUNCTION Hash (src IN CLOB CHARACTER SET ANY_CS,
                 typ IN PLS_INTEGER)
    RETURN RAW DETERMINISTIC;

END DBMS_CRYPTO;
//
