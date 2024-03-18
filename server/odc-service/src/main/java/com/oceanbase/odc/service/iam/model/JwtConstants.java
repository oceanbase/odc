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
package com.oceanbase.odc.service.iam.model;

public class JwtConstants {

    public static final String ID = "ID";
    /**
     * Keep the format consistent with the key format in the cookie
     */
    public static final String ODC_JWT_TOKEN = "ODC-JWT-TOKEN";

    public static final String AUTHENTICATION_BLANK_VALUE = "";

    public static final String PRINCIPAL = "PRINCIPAL";

    public static final String ORGANIZATION_ID = "oORGANIZATION_ID";

    public static final String ORGANIZATION_TYPE = "ORGANIZATION_TYPE";

}
