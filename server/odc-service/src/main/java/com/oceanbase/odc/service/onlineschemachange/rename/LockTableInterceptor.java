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
package com.oceanbase.odc.service.onlineschemachange.rename;


/**
 * @author yaobin
 * @date 2023-08-02
 * @since 4.2.0
 */
public class LockTableInterceptor implements RenameTableInterceptor {

    @Override
    public void preRename(RenameTableParameters parameters) {

    }

    @Override
    public void renameSucceed(RenameTableParameters parameters) {

    }

    @Override
    public void renameFailed(RenameTableParameters parameters) {

    }

    @Override
    public void postRenamed(RenameTableParameters parameters) {

    }
}
