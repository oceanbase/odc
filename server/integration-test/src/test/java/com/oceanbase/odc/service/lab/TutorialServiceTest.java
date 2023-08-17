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
package com.oceanbase.odc.service.lab;

import java.util.List;
import java.util.Optional;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.oceanbase.odc.ServiceTestEnv;
import com.oceanbase.odc.metadb.lab.TutorialEntity;
import com.oceanbase.odc.metadb.lab.TutorialRepository;
import com.oceanbase.odc.service.lab.model.Tutorial;

public class TutorialServiceTest extends ServiceTestEnv {
    @Autowired
    private TutorialService tutorialService;
    @Autowired
    private TutorialRepository tutorialRepository;

    @Before
    public void setUp() {
        tutorialRepository.deleteAll();
        tutorialRepository.saveAndFlush(createTutorial());
    }

    @After
    public void tearDown() {
        tutorialRepository.deleteAll();
    }

    private TutorialEntity createTutorial() {
        TutorialEntity tutorialEntity = new TutorialEntity();
        tutorialEntity.setId(1L);
        tutorialEntity.setName("test tutorial");
        tutorialEntity.setFilename("test.md");
        tutorialEntity.setLanguage("zh-CN");
        tutorialEntity.setContent("test content<a href=xxx><img src=xxx></a> "
                + "test[立即体验](https://play.oceanbase.com#/gateway/eyJkYXRhIjp1dG9yaWFsIn0=)test "
                + "[立即体验](https://play.oceanbase.com#/gateway/eyJkYXRhIjp1dG9yaWFsIn0=)");
        return tutorialEntity;
    }

    @Test
    public void testList_Success() {
        List<Tutorial> tutorials = tutorialService.list();
        Assert.assertEquals(1, tutorials.size());
    }

    @Test
    public void testFindById() {
        List<Tutorial> tutorials = tutorialService.list();
        Optional<Tutorial> first = tutorials.stream().findFirst();
        Tutorial tutorial = tutorialService.findById(first.get().getId());
        Assert.assertEquals("test content testtest ", tutorial.getContent());
    }

    @Test
    public void testFindByFilenameAndLanguage() {
        Tutorial tutorial = tutorialService.findByFilenameAndLanguage("test.md", "zh-CN");
        Assert.assertEquals("test content testtest ", tutorial.getContent());
    }

}
