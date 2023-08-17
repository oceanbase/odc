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
package com.oceanbase.odc.metadb.snippet;

import java.util.List;

import org.springframework.stereotype.Component;

import com.oceanbase.odc.service.snippet.Snippet;

/**
 * @author mogao.zj
 */

@Component
public interface SnippetsDAO {

    /**
     * list by userId
     * 
     * @return
     */
    List<Snippet> list(long userId);

    /**
     * get by id
     * 
     * @param id
     * @return
     */
    Snippet get(long id);

    /**
     * @param snippet
     * @return
     */
    long insert(Snippet snippet);

    /**
     * update
     *
     * @param snippet
     * @return
     */
    long update(Snippet snippet);

    /**
     * delete
     *
     * @param id
     * @return
     */
    long delete(long id);


    /**
     * delete all, for UT
     *
     * @return
     */
    long deleteAll();


    /**
     * queryByName
     *
     * @param snippet
     * @return
     */
    Snippet queryByUserIdAndName(Snippet snippet);
}
