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
package com.oceanbase.odc.metadb.iam;

import org.springframework.stereotype.Component;

import com.oceanbase.odc.service.iam.model.UserInfo;

/**
 * @author
 *
 *         已过时，对应的是odc以前的用户表 odc_user_info, 该类目前仅在数据迁移中使用，业务中不要使用。
 */
@Component
@Deprecated
public interface UserInfoDAO {

    /**
     * get user by id
     * 
     * @param id
     * @return
     */
    UserInfo get(long id);

    /**
     * @param email
     * @return
     */
    UserInfo detail(String email);

    /**
     * @param odcUser
     * @return
     */
    int insert(UserInfo odcUser);

    /**
     * update
     *
     * @param odcUser
     * @return
     */
    int update(UserInfo odcUser);

    /**
     * delete
     *
     * @param id
     * @return
     */
    int delete(long id);

}
