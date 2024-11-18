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
package com.oceanbase.odc.service.connection.model;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import com.oceanbase.odc.service.session.model.SessionSettings;
import com.oceanbase.tools.dbbrowser.model.DBSession;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: Lebie
 * @Date: 2023/6/5 11:12
 * @Description: []
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DBSessionResp {
    private DBSessionRespDelegate session;
    private SessionSettings settings;

    @Mapper
    interface DBSessionRespMapper {
        DBSessionRespMapper INSTANCE = Mappers.getMapper(DBSessionRespMapper.class);

        DBSessionRespDelegate toDBSessionStatusResp(DBSession session);
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DBSessionRespDelegate extends DBSession {
        private boolean killCurrentQuerySupported;

        public static DBSessionRespDelegate of(DBSession session) {
            return DBSessionRespMapper.INSTANCE.toDBSessionStatusResp(session);
        }
    }

}
