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
package com.oceanbase.odc.service.archiver.streamprovider;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class RemoteUriStreamProvider implements ExportedDataStreamProvider {

    private final String remoteUrl;

    public RemoteUriStreamProvider(String remoteUrl) {
        this.remoteUrl = remoteUrl;
    }

    @Override
    public InputStream getInputStream() throws Exception {
        URL url = new URL(remoteUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");

        int responseCode = connection.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) {
            return connection.getInputStream();
        } else {
            throw new RuntimeException("Failed to open input stream for link: " + remoteUrl +
                    ". Response code: " + responseCode);
        }
    }
}
