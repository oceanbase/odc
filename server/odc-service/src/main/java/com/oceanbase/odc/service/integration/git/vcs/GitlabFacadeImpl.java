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
package com.oceanbase.odc.service.integration.git.vcs;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import com.oceanbase.odc.service.integration.git.model.GitRepository;

/**
 * @author: liuyizhuo.lyz
 * @date: 2024/7/29
 */
public class GitlabFacadeImpl implements VcsFacade {
    private static final String GITLAB_API_URL = "https://gitlab.com/api/v4/projects?membership=true&simple=true";
    private final RestTemplate restTemplate;
    private final String apiUrl;

    public GitlabFacadeImpl() {
        this(GITLAB_API_URL);
    }

    public GitlabFacadeImpl(String apiUrl) {
        this.apiUrl = apiUrl;
        this.restTemplate = new RestTemplateBuilder()
                .setConnectTimeout(Duration.ofSeconds(10))
                .setReadTimeout(Duration.ofSeconds(10))
                .build();
    }

    @Override
    public List<GitRepository> listRepositories(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Bearer " + token);

        HttpEntity<String> request = new HttpEntity<>(headers);

        ResponseEntity<List> response = restTemplate.exchange(apiUrl, HttpMethod.GET, request, List.class);
        List<GitRepository> result = new ArrayList<>();
        for (Map<String, Object> map : (List<Map<String, Object>>) response.getBody()) {
            GitRepository repository = new GitRepository();
            repository.setName((String) map.get("name"));
            repository.setSshUrl((String) map.get("ssh_url_to_repo"));
            repository.setCloneUrl((String) map.get("http_url_to_repo"));
            result.add(repository);
        }
        return result;
    }
}
