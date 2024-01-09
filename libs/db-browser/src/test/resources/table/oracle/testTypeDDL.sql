CREATE OR REPLACE TYPE TYPE_ACCESSOR AS OBJECT (
    street VARCHAR2(30),
    city VARCHAR2(20),
    state CHAR(2),
    postal_code VARCHAR2(6)
);
/

CREATE OR REPLACE TYPE TYPE_ACCESSOR2 AS OBJECT (
    first_name VARCHAR2(50),
    last_name VARCHAR2(50),
    gender VARCHAR2(10),
    birth_date DATE,
    MEMBER FUNCTION get_full_name RETURN VARCHAR2
);
/

CREATE OR REPLACE TYPE BODY TYPE_ACCESSOR2 AS
    MEMBER FUNCTION get_full_name RETURN VARCHAR2 IS
    BEGIN
        RETURN first_name || ' ' || last_name;
    END;
END;