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
package com.oceanbase.tools.dbbrowser.parser.result;

import java.util.List;

import com.oceanbase.tools.dbbrowser.model.DBPLParam;
import com.oceanbase.tools.dbbrowser.parser.listener.MysqlModePLParserListener;

import lombok.Getter;
import lombok.Setter;

/**
 * @author wenniu.ly
 * @date 2021/8/25
 */

@Getter
@Setter
public class ParseMysqlPLResult extends ParsePLResult {

    private List<DBPLParam> paramList; // pl参数

    public ParseMysqlPLResult(MysqlModePLParserListener listener) {
        super(listener);
        this.varibaleList = listener.getVaribaleList();
        this.typeList = listener.getTypeList();
        this.returnType = listener.getReturnType();
        this.plName = listener.getPlName();
        this.plType = listener.getPlType();
        this.paramList = listener.getParamList();
        this.empty = listener.isEmpty();
    }
}
