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
package com.oceanbase.odc.service.snippet;

import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import com.oceanbase.odc.ServiceTestEnv;
import com.oceanbase.odc.metadb.snippet.SnippetsDAO;
import com.oceanbase.odc.service.iam.auth.AuthenticationFacade;

public class SnippetsServiceTest extends ServiceTestEnv {
    private static final long USER_ID = 1L;

    @Autowired
    private SnippetsService snippetsService;
    @Autowired
    private SnippetsDAO snippetsDAO;
    @MockBean
    private AuthenticationFacade authenticationFacade;

    @Before
    public void setUp() throws Exception {
        when(authenticationFacade.currentUserId()).thenReturn(USER_ID);
        snippetsDAO.deleteAll();
    }

    @Test
    public void testCreateSnippet() {
        Snippet snippet = new Snippet();
        snippet.setPrefix("select1");
        snippet.setBody("select * from test;");
        snippet.setDescription("this is a query demo");
        snippet.setType("DQL");
        Snippet created = this.snippetsService.create(snippet);

        Assert.assertNotNull(created);
    }

    @Test
    public void testUpdateSnippet() {
        Snippet snippet = new Snippet();
        snippet.setPrefix("select1");
        snippet.setBody("select * from test;");
        snippet.setDescription("this is a query demo");
        snippet.setType("DQL");
        this.snippetsService.create(snippet);

        snippet.setBody("select * from test1;");
        Snippet updated = this.snippetsService.update(snippet);

        Assert.assertEquals("select * from test1;", updated.getBody());
    }

    @Test
    public void testQuerySnippet() {
        Snippet snippet = new Snippet();
        snippet.setPrefix("select1");
        snippet.setBody("select * from test;");
        snippet.setDescription("this is a query demo");
        snippet.setType("DQL");
        this.snippetsService.create(snippet);

        List<Snippet> list = this.snippetsService.list();

        Assert.assertEquals(1, list.size());
    }

    @Test
    public void testDeleteSnippet() {
        Snippet snippet = new Snippet();
        snippet.setPrefix("select1");
        snippet.setBody("select * from test;");
        snippet.setDescription("this is a query demo");
        snippet.setType("DQL");
        Snippet created = this.snippetsService.create(snippet);

        Snippet deleted = this.snippetsService.delete(created.getId());

        Assert.assertEquals(created.getId(), deleted.getId());
    }

}
