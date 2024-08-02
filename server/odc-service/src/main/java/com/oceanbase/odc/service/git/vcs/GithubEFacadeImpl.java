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
package com.oceanbase.odc.service.git.vcs;

/**
 * @author: liuyizhuo.lyz
 * @date: 2024/7/29
 */
public class GithubEFacadeImpl extends GithubFacadeImpl {
    private static final String API_URL_FORMAT = "%s/api/v3/user/repos";

    public GithubEFacadeImpl(String host) {
        super(String.format(API_URL_FORMAT, host));
    }
}
