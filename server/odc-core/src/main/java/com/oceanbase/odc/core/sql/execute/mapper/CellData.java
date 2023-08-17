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
package com.oceanbase.odc.core.sql.execute.mapper;

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.Array;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Date;
import java.sql.NClob;
import java.sql.Ref;
import java.sql.ResultSet;
import java.sql.RowId;
import java.sql.SQLException;
import java.sql.SQLXML;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Map;

import org.apache.commons.lang3.Validate;

import com.oceanbase.tools.dbbrowser.model.datatype.DataType;

import lombok.Getter;
import lombok.NonNull;

/**
 * {@link CellData}
 *
 * @author yh263208
 * @date 2022-06-22 16:02
 * @since ODC_release_3.4.0
 */
public class CellData {

    private final ResultSet resultSet;
    private final Integer columnIndex;
    @Getter
    private final DataType dataType;

    /**
     * Default constructor
     *
     * @param resultSet {@link ResultSet}
     * @param columnIndex from 0
     */
    public CellData(@NonNull ResultSet resultSet, int columnIndex, @NonNull DataType dataType) {
        Validate.isTrue(columnIndex >= 0, "ColumnIndex can not be negative");
        this.resultSet = resultSet;
        this.columnIndex = columnIndex;
        this.dataType = dataType;
    }

    public String getString() throws SQLException {
        return resultSet.getString(columnIndex + 1);
    }

    public boolean getBoolean() throws SQLException {
        return resultSet.getBoolean(columnIndex + 1);
    }

    public byte getByte() throws SQLException {
        return resultSet.getByte(columnIndex + 1);
    }

    public short getShort() throws SQLException {
        return resultSet.getShort(columnIndex + 1);
    }

    public int getInt() throws SQLException {
        return resultSet.getInt(columnIndex + 1);
    }

    public long getLong() throws SQLException {
        return resultSet.getLong(columnIndex + 1);
    }

    public float getFloat() throws SQLException {
        return resultSet.getFloat(columnIndex + 1);
    }

    public double getDouble() throws SQLException {
        return resultSet.getDouble(columnIndex + 1);
    }

    @Deprecated
    public BigDecimal getBigDecimal(int scale) throws SQLException {
        return resultSet.getBigDecimal(columnIndex + 1, scale);
    }

    public byte[] getBytes() throws SQLException {
        return resultSet.getBytes(columnIndex + 1);
    }

    public Date getDate() throws SQLException {
        return resultSet.getDate(columnIndex + 1);
    }

    public Time getTime() throws SQLException {
        return resultSet.getTime(columnIndex + 1);
    }

    public Timestamp getTimestamp() throws SQLException {
        return resultSet.getTimestamp(columnIndex + 1);
    }

    public InputStream getAsciiStream() throws SQLException {
        return resultSet.getAsciiStream(columnIndex + 1);
    }

    @Deprecated
    public InputStream getUnicodeStream() throws SQLException {
        return resultSet.getUnicodeStream(columnIndex + 1);
    }

    public InputStream getBinaryStream() throws SQLException {
        return resultSet.getBinaryStream(columnIndex + 1);
    }

    public Object getObject() throws SQLException {
        return resultSet.getObject(columnIndex + 1);
    }

    public Reader getCharacterStream() throws SQLException {
        return resultSet.getCharacterStream(columnIndex + 1);
    }

    public BigDecimal getBigDecimal() throws SQLException {
        return resultSet.getBigDecimal(columnIndex + 1);
    }

    public Object getObject(Map<String, Class<?>> map) throws SQLException {
        return resultSet.getObject(columnIndex + 1, map);
    }

    public Ref getRef() throws SQLException {
        return resultSet.getRef(columnIndex + 1);
    }

    public Blob getBlob() throws SQLException {
        return resultSet.getBlob(columnIndex + 1);
    }

    public Clob getClob() throws SQLException {
        return resultSet.getClob(columnIndex + 1);
    }

    public Array getArray() throws SQLException {
        return resultSet.getArray(columnIndex + 1);
    }

    public Date getDate(Calendar cal) throws SQLException {
        return resultSet.getDate(columnIndex + 1, cal);
    }

    public Time getTime(Calendar cal) throws SQLException {
        return resultSet.getTime(columnIndex + 1, cal);
    }

    public Timestamp getTimestamp(Calendar cal) throws SQLException {
        return resultSet.getTimestamp(columnIndex + 1, cal);
    }

    public URL getURL() throws SQLException {
        return resultSet.getURL(columnIndex + 1);
    }

    public RowId getRowId() throws SQLException {
        return resultSet.getRowId(columnIndex + 1);
    }

    public NClob getNClob() throws SQLException {
        return resultSet.getNClob(columnIndex + 1);
    }

    public SQLXML getSQLXML() throws SQLException {
        return resultSet.getSQLXML(columnIndex + 1);
    }

    public String getNString() throws SQLException {
        return resultSet.getNString(columnIndex + 1);
    }

    public Reader getNCharacterStream() throws SQLException {
        return resultSet.getNCharacterStream(columnIndex + 1);
    }

    public <T> T getObject(Class<T> type) throws SQLException {
        return resultSet.getObject(columnIndex + 1, type);
    }

}
