-- 由于 ODC 4.0.0 升级了 Spring Security 版本，导致 Spring Security 某些类的序列化和反序列化不兼容
-- 需要 truncate 掉 Spring Session 的这两张表，让用户生成新的 session
truncate table `spring_session`;
truncate table `spring_session_attributes`;
