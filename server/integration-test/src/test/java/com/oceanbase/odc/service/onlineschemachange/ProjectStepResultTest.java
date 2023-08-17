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
package com.oceanbase.odc.service.onlineschemachange;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.util.CollectionUtils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.service.onlineschemachange.oms.response.ProjectFullVerifyResultResponse;
import com.oceanbase.odc.service.onlineschemachange.oms.response.ProjectStepVO;

import lombok.extern.slf4j.Slf4j;

/**
 * @author yaobin
 * @date 2023-06-12
 * @since 4.2.0
 */
@Slf4j
public class ProjectStepResultTest {

    @Test
    public void readProjectStepFile() {
        List<ProjectStepVO> projectStepVOS = projectSteps();
        Assert.assertFalse(CollectionUtils.isEmpty(projectStepVOS));
    }

    @Test
    public void readVerifyResultFile() {
        ProjectFullVerifyResultResponse fullVerifyResultResponse = verifyResult();
        Assert.assertNotNull(fullVerifyResultResponse);
    }

    public List<ProjectStepVO> projectSteps() {
        return getJsonResult("project_step.json", new TypeReference<List<ProjectStepVO>>() {});
    }

    public ProjectFullVerifyResultResponse verifyResult() {
        return getJsonResult("verify_result.json", new TypeReference<ProjectFullVerifyResultResponse>() {});
    }

    @SuppressWarnings("all")
    public <T> T getJsonResult(String fileName, TypeReference<T> typeReference) {
        byte[] bytes = new byte[1024];

        try (BufferedInputStream is = new BufferedInputStream(
                getClass().getClassLoader().getResourceAsStream("onlineschemachange/" + fileName));
                ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            int length;
            while ((length = is.read(bytes, 0, 1024)) != -1) {
                baos.write(bytes, 0, length);
            }
            return JsonUtils.fromJson(baos.toString(), typeReference);
        } catch (Exception exception) {
            log.warn("Read json file error", exception);
        }
        return null;
    }
}
