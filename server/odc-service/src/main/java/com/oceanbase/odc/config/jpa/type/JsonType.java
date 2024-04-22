/*
 * Copyright (c) 2023 OceanBase.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.oceanbase.odc.config.jpa.type;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Objects;
import java.util.Properties;

import org.hibernate.HibernateException;
import org.hibernate.annotations.common.reflection.java.JavaXMember;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.usertype.DynamicParameterizedType;
import org.hibernate.usertype.UserType;

import com.fasterxml.jackson.databind.JavaType;
import com.oceanbase.odc.common.json.JsonUtils;

public class JsonType implements UserType, DynamicParameterizedType {

    private Class<?> returnedClass;

    private JavaType targetJavaType;

    @Override
    public int[] sqlTypes() {
        return new int[] {Types.CLOB};
    }

    @Override
    public Class<?> returnedClass() {
        return returnedClass;
    }

    @Override
    public boolean equals(Object x, Object y) throws HibernateException {
        return Objects.equals(x, y);
    }

    @Override
    public int hashCode(Object x) throws HibernateException {
        return x.hashCode();
    }

    @Override
    public Object nullSafeGet(ResultSet rs, String[] names, SharedSessionContractImplementor session, Object owner)
            throws HibernateException,
            SQLException {
        String value = rs.getString(names[0]);
        return fromJson(value);
    }

    @Override
    public void nullSafeSet(PreparedStatement st, Object value, int index, SharedSessionContractImplementor session)
            throws HibernateException, SQLException {
        if (value == null) {
            st.setNull(index, Types.CLOB);
            return;
        }
        st.setString(index, toJson(value));
    }

    private Object fromJson(String value) {
        if (value == null) {
            return null;
        }
        return JsonUtils.fromJson(value, targetJavaType);
    }

    private String toJson(Object value) {
        if (value == null) {
            return null;
        }
        return JsonUtils.toJson(value);
    }

    @Override
    public Object deepCopy(Object value) throws HibernateException {
        return fromJson(toJson(value));
    }

    @Override
    public boolean isMutable() {
        return true;
    }

    @Override
    public Serializable disassemble(Object value) throws HibernateException {
        throw new UnsupportedOperationException("disassemble is not supported");
    }

    @Override
    public Object assemble(Serializable cached, Object owner) throws HibernateException {
        throw new UnsupportedOperationException("assemble is not supported");
    }

    @Override
    public Object replace(Object original, Object target, Object owner) throws HibernateException {
        return original;
    }

    @Override
    public void setParameterValues(Properties parameters) {
        Object xProperty = parameters.get(DynamicParameterizedType.XPROPERTY);
        Field annotatedField = (Field) getAnnotatedElement(xProperty);

        returnedClass = annotatedField.getType();
        Type type = annotatedField.getGenericType();
        targetJavaType = JsonUtils.constructType(type);
    }

    private Object getAnnotatedElement(Object target) {
        try {
            Field field = JavaXMember.class.getSuperclass().getDeclaredField("annotatedElement");
            field.setAccessible(true);
            return field.get(target);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new AssertionError(e);
        }
    }

}
