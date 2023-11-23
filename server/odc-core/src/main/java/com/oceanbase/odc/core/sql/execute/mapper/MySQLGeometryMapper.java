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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKBReader;

import com.oceanbase.tools.dbbrowser.model.datatype.DataType;

import lombok.NonNull;

/**
 * @author jingtian
 * @date 2023/11/21
 * @since ODC_release_4.2.4
 */
public class MySQLGeometryMapper implements JdbcColumnMapper {
    @Override
    public Object mapCell(@NonNull CellData data) throws SQLException, IOException {
        InputStream binaryStream = data.getBinaryStream();
        if (binaryStream == null) {
            return null;
        }
        try {
            Geometry geometry = getGeometryFromInputStream(binaryStream);
            String text = geometry.toText();
            return geometry.getSRID() == 0 ? text : text + " | " + geometry.getSRID();
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse geometry:" + e);
        }
    }

    @Override
    public boolean supports(@NonNull DataType dataType) {
        return "GEOMETRY".equalsIgnoreCase(dataType.getDataTypeName());
    }

    private Geometry getGeometryFromInputStream(InputStream inputStream) throws ParseException, IOException {
        Geometry dbGeometry = null;
        // Convert binary stream into byte array
        byte[] buffer = new byte[255];

        int bytesRead = 0;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        while ((bytesRead = inputStream.read(buffer)) != -1) {
            baos.write(buffer, 0, bytesRead);
        }
        byte[] geometryAsBytes = baos.toByteArray();
        // Byte array less than 5 is an exception
        if (geometryAsBytes.length < 5) {
            throw new IllegalStateException("Abnormal coordinates");
        }

        // The first 4 bytes of the byte array represent SRID and should be removed
        byte[] sridBytes = new byte[4];
        System.arraycopy(geometryAsBytes, 0, sridBytes, 0, 4);
        boolean bigEndian = (geometryAsBytes[4] == 0x00);
        // Parse SRID
        int srid = 0;
        if (bigEndian) {
            for (int i = 0; i < sridBytes.length; i++) {
                srid = (srid << 8) + (sridBytes[i] & 0xff);
            }
        } else {
            for (int i = 0; i < sridBytes.length; i++) {
                srid += (sridBytes[i] & 0xff) << (8 * i);
            }
        }

        WKBReader wkbReader = new WKBReader();
        // Use WKBReader to convert the byte array into a geometry object
        byte[] wkb = new byte[geometryAsBytes.length - 4];
        System.arraycopy(geometryAsBytes, 4, wkb, 0, wkb.length);
        dbGeometry = wkbReader.read(wkb);
        dbGeometry.setSRID(srid);
        return dbGeometry;
    }
}
