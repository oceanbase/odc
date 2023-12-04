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
package com.oceanbase.odc.service.datasecurity;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.lang3.Validate;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.oceanbase.odc.ServiceTestEnv;
import com.oceanbase.odc.core.datamasking.algorithm.Hash.HashType;
import com.oceanbase.odc.core.shared.exception.BadRequestException;
import com.oceanbase.odc.metadb.datasecurity.MaskingAlgorithmRepository;
import com.oceanbase.odc.service.datasecurity.model.MaskingAlgorithm;
import com.oceanbase.odc.service.datasecurity.model.MaskingAlgorithmType;
import com.oceanbase.odc.service.datasecurity.model.MaskingSegment;
import com.oceanbase.odc.service.datasecurity.model.MaskingSegmentType;
import com.oceanbase.odc.service.datasecurity.model.MaskingSegmentsType;
import com.oceanbase.odc.service.datasecurity.model.QueryMaskingAlgorithmParams;

/**
 * @author gaoda.xy
 * @date 2023/5/18 14:57
 */
public class MaskingAlgorithmServiceTest extends ServiceTestEnv {

    @Autowired
    private MaskingAlgorithmService service;

    @Autowired
    private MaskingAlgorithmRepository algorithmRepository;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void setUp() throws Exception {
        algorithmRepository.deleteAll();
    }

    @After
    public void tearDown() throws Exception {
        algorithmRepository.deleteAll();
    }

    @Test
    public void test_exists_exist() {
        MaskingAlgorithm algorithm = createAlgorithm("test_exists", MaskingAlgorithmType.MASK, null);
        service.create(algorithm);
        Boolean exist = service.exists(algorithm.getName());
        Assert.assertTrue(exist);
    }

    @Test
    public void test_exists_notExist() {
        Boolean exist = service.exists("notExists");
        Assert.assertFalse(exist);
    }

    @Test
    public void test_create_success() {
        MaskingAlgorithm algorithm = createAlgorithm("test_create", MaskingAlgorithmType.MASK, null);
        MaskingAlgorithm created = service.create(algorithm);
        Assert.assertEquals(algorithm.getName(), created.getName());
    }

    @Test
    public void test_create_duplicated() {
        MaskingAlgorithm algorithm = createAlgorithm("test_create", MaskingAlgorithmType.MASK, null);
        service.create(algorithm);
        thrown.expect(BadRequestException.class);
        service.create(algorithm);
    }

    @Test
    public void test_create_withCustomSegments() {
        MaskingAlgorithm algorithm = createAlgorithm("test_create", MaskingAlgorithmType.MASK, null);
        algorithm.setSegmentsType(MaskingSegmentsType.CUSTOM);
        algorithm.setSegments(createSegments());
        MaskingAlgorithm created = service.create(algorithm);
        Assert.assertEquals(algorithm.getName(), created.getName());
        Assert.assertEquals("******e@******com", created.getMaskedContent());
    }

    @Test
    public void test_detail() {
        MaskingAlgorithm algorithm = createAlgorithm("test_detail", MaskingAlgorithmType.MASK, null);
        MaskingAlgorithm create = service.create(algorithm);
        MaskingAlgorithm detail = service.detail(create.getId());
        Assert.assertEquals(create, detail);
    }

    @Test
    public void test_detail_withCustomSegments() {
        MaskingAlgorithm algorithm = createAlgorithm("test_detail", MaskingAlgorithmType.MASK, null);
        algorithm.setSegmentsType(MaskingSegmentsType.CUSTOM);
        algorithm.setSegments(createSegments());
        MaskingAlgorithm create = service.create(algorithm);
        MaskingAlgorithm detail = service.detail(create.getId());
        Assert.assertEquals(create, detail);
    }

    @Test
    public void test_list_all() {
        MaskingAlgorithm algorithm1 = createAlgorithm("test_list_1", MaskingAlgorithmType.MASK, null);
        MaskingAlgorithm algorithm2 = createAlgorithm("test_list_2", MaskingAlgorithmType.MASK, null);
        service.create(algorithm1);
        service.create(algorithm2);
        Page<MaskingAlgorithm> list = service.list(QueryMaskingAlgorithmParams.builder().build(), Pageable.unpaged());
        Validate.isTrue(list.getContent().size() == 2);
        Validate.isTrue(list.getContent().get(0).getName().endsWith("test_list_1"));
    }

    @Test
    public void test_list_byName() {
        MaskingAlgorithm algorithm1 = createAlgorithm("test_list_1", MaskingAlgorithmType.MASK, null);
        MaskingAlgorithm algorithm2 = createAlgorithm("test_list_2", MaskingAlgorithmType.MASK, null);
        service.create(algorithm1);
        service.create(algorithm2);
        Page<MaskingAlgorithm> list1 =
                service.list(QueryMaskingAlgorithmParams.builder().fuzzyName("list_1").build(), Pageable.unpaged());
        Validate.isTrue(list1.getContent().size() == 1);
        Page<MaskingAlgorithm> list2 =
                service.list(QueryMaskingAlgorithmParams.builder().fuzzyName("list").build(), Pageable.unpaged());
        Validate.isTrue(list2.getContent().size() == 2);
    }

    @Test
    public void test_list_byType() {
        MaskingAlgorithm algorithm1 = createAlgorithm("test_list_1", MaskingAlgorithmType.MASK, null);
        MaskingAlgorithm algorithm2 = createAlgorithm("test_list_2", MaskingAlgorithmType.HASH, null);
        service.create(algorithm1);
        service.create(algorithm2);
        Page<MaskingAlgorithm> list1 = service
                .list(QueryMaskingAlgorithmParams.builder().types(Arrays.asList(MaskingAlgorithmType.HASH)).build(),
                        Pageable.unpaged());
        Validate.isTrue(list1.getContent().size() == 1);
        Page<MaskingAlgorithm> list2 = service.list(
                QueryMaskingAlgorithmParams.builder()
                        .types(Arrays.asList(MaskingAlgorithmType.HASH, MaskingAlgorithmType.MASK)).build(),
                Pageable.unpaged());
        Validate.isTrue(list2.getContent().size() == 2);
    }

    @Test
    public void test_test_MASK() {
        MaskingAlgorithm algorithm = createAlgorithm("test_test", MaskingAlgorithmType.MASK, null);
        algorithm.setSegmentsType(MaskingSegmentsType.PRE_1_POST_1);
        MaskingAlgorithm created = service.create(algorithm);
        MaskingAlgorithm masked = service.test(created.getId(), "example@email.com");
        Assert.assertEquals("e***************m", masked.getMaskedContent());
    }

    @Test
    public void test_test_SUBSTUTION() {
        MaskingAlgorithm algorithm = createAlgorithm("test_test", MaskingAlgorithmType.SUBSTITUTION, null);
        MaskingAlgorithm created = service.create(algorithm);
        MaskingAlgorithm masked = service.test(created.getId(), "example@email.com");
        Assert.assertEquals("xxx", masked.getMaskedContent());
    }

    @Test
    public void test_test_PSEUDO() {
        MaskingAlgorithm algorithm = createAlgorithm("test_test", MaskingAlgorithmType.PSEUDO, null);
        MaskingAlgorithm created = service.create(algorithm);
        MaskingAlgorithm masked = service.test(created.getId(), "example@email.com");
        Pattern pattern = Pattern.compile("^[a-z]{7}@[a-z]{5}\\.[a-z]{3}$");
        Assert.assertTrue(pattern.matcher(masked.getMaskedContent()).matches());
    }

    @Test
    public void test_test_HASH() {
        MaskingAlgorithm algorithm = createAlgorithm("test_test", MaskingAlgorithmType.HASH, null);
        MaskingAlgorithm created = service.create(algorithm);
        MaskingAlgorithm masked = service.test(created.getId(), "example@email.com");
        Assert.assertEquals(32, masked.getMaskedContent().length());
    }

    public void test_test_ROUNDING() {
        MaskingAlgorithm algorithm = createAlgorithm("test_test", MaskingAlgorithmType.ROUNDING, null);
        algorithm.setSampleContent("1.23");
        MaskingAlgorithm created = service.create(algorithm);
        MaskingAlgorithm masked = service.test(created.getId(), "example@email.com");
        Assert.assertEquals("1.2", masked.getMaskedContent());
    }

    public void test_test_NULL() {
        MaskingAlgorithm algorithm = createAlgorithm("test_test", MaskingAlgorithmType.NULL, null);
        MaskingAlgorithm created = service.create(algorithm);
        MaskingAlgorithm masked = service.test(created.getId(), "example@email.com");
        Assert.assertNull(masked.getMaskedContent());
    }

    private MaskingAlgorithm createAlgorithm(String name, MaskingAlgorithmType type, List<MaskingSegment> segments) {
        MaskingAlgorithm algorithm = new MaskingAlgorithm();
        algorithm.setName(name);
        algorithm.setSegments(segments);
        algorithm.setCharsets(Arrays.asList("a~z"));
        algorithm.setDecimal(true);
        algorithm.setPrecision(1);
        algorithm.setEnabled(true);
        algorithm.setHashType(HashType.MD5);
        algorithm.setSampleContent("example@email.com");
        algorithm.setSubstitution("xxx");
        algorithm.setType(type);
        algorithm.setSegmentsType(MaskingSegmentsType.ALL);
        return algorithm;
    }

    private List<MaskingSegment> createSegments() {
        return Arrays.asList(
                createSegment(MaskingSegmentType.DIGIT_PERCENTAGE, true, null, null, 30, null),
                createSegment(MaskingSegmentType.DELIMITER, false, null, "@", null, null),
                createSegment(MaskingSegmentType.LEFT_OVER, true, null, null, null, null),
                createSegment(MaskingSegmentType.DIGIT, false, null, null, null, 3));
    }

    private MaskingSegment createSegment(MaskingSegmentType type, Boolean mask, String replacedCharacters,
            String delimiter, Integer digitPercentage, Integer digitNumber) {
        MaskingSegment segment = new MaskingSegment();
        segment.setType(type);
        segment.setMask(mask);
        segment.setReplacedCharacters(replacedCharacters);
        segment.setDelimiter(delimiter);
        segment.setDigitNumber(digitNumber);
        segment.setDigitPercentage(digitPercentage);
        return segment;
    }

}
