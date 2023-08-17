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
package com.oceanbase.odc.server.web.controller.v2;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.oceanbase.odc.service.common.response.PaginatedResponse;
import com.oceanbase.odc.service.common.response.Responses;
import com.oceanbase.odc.service.common.response.SuccessResponse;
import com.oceanbase.odc.service.datasecurity.MaskingAlgorithmService;
import com.oceanbase.odc.service.datasecurity.model.MaskingAlgorithm;
import com.oceanbase.odc.service.datasecurity.model.MaskingAlgorithmType;
import com.oceanbase.odc.service.datasecurity.model.QueryMaskingAlgorithmParams;

import io.swagger.annotations.ApiOperation;

/**
 * @author gaoda.xy
 * @date 2023/5/9 16:24
 */
@RestController
@RequestMapping("api/v2/datasecurity/maskingAlgorithms")
public class MaskingAlgorithmController {

    @Autowired
    private MaskingAlgorithmService maskingAlgorithmService;

    @ApiOperation(value = "detailMaskingAlgorithm", notes = "View masking algorithm details")
    @RequestMapping(value = "/{id:[\\d]+}", method = RequestMethod.GET)
    public SuccessResponse<MaskingAlgorithm> detailMaskingAlgorithm(@PathVariable Long id) {
        return Responses.success(maskingAlgorithmService.detail(id));
    }

    @ApiOperation(value = "listMaskingAlgorithm", notes = "List masking algorithms")
    @RequestMapping(value = "/", method = RequestMethod.GET)
    public PaginatedResponse<MaskingAlgorithm> listMaskingAlgorithm(
            @RequestParam(name = "name", required = false) String fuzzyName,
            @RequestParam(name = "type", required = false) List<MaskingAlgorithmType> types,
            @PageableDefault(size = Integer.MAX_VALUE, sort = {"id"}, direction = Direction.ASC) Pageable pageable) {
        QueryMaskingAlgorithmParams params = QueryMaskingAlgorithmParams.builder()
                .fuzzyName(fuzzyName)
                .types(types).build();
        return Responses.paginated(maskingAlgorithmService.list(params, pageable));
    }

    @ApiOperation(value = "testMaskingAlgorithm", notes = "Test the effect of masking algorithm")
    @RequestMapping(value = "/test", method = RequestMethod.POST)
    public SuccessResponse<MaskingAlgorithm> testMaskingAlgorithm(@RequestBody MaskingAlgorithm algorithm) {
        algorithm.validate();
        return Responses.success(maskingAlgorithmService.test(algorithm));
    }

}
