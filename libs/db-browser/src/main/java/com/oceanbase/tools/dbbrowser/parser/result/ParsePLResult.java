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

import java.util.ArrayList;
import java.util.List;

import com.oceanbase.tools.dbbrowser.model.DBPLType;
import com.oceanbase.tools.dbbrowser.model.DBPLVariable;
import com.oceanbase.tools.dbbrowser.parser.listener.BasicParserListener;

import lombok.Getter;
import lombok.Setter;

/**
 * @author wenniu.ly
 * @date 2021/8/25
 */

@Getter
@Setter
public class ParsePLResult extends BasicResult {

    protected List<DBPLVariable> varibaleList = new ArrayList<>();
    protected List<DBPLType> typeList = new ArrayList<>();
    protected List<DBPLType> cursorList = new ArrayList<>();
    protected String returnType;

    protected String plName; // pl对象名
    protected String plType;// pl类型，function、procedure、package
    protected String isOrAs;
    protected boolean empty;

    public ParsePLResult(BasicParserListener listener) {
        super(listener);
    }
}
