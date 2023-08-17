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
package com.oceanbase.odc.metadb.snippet;

import java.util.Collections;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.oceanbase.odc.ServiceTestEnv;
import com.oceanbase.odc.service.snippet.Snippet;
import com.oceanbase.odc.test.tool.TestRandom;

public class SnippetsDAOTest extends ServiceTestEnv {

    @Autowired
    private SnippetsDAO snippetsDAO;

    @Test
    public void insert_insertSnippet_insertSucceed() {
        Snippet snippet = TestRandom.nextObject(Snippet.class);
        Assert.assertEquals(1, snippetsDAO.insert(snippet));
    }

    @Test
    public void get_getSnippet_getSucceed() {
        Snippet snippet = TestRandom.nextObject(Snippet.class);
        snippetsDAO.insert(snippet);
        Snippet actual = snippetsDAO.get(snippet.getId());
        snippet.setModifyTime(null);
        snippet.setCreateTime(null);
        actual.setModifyTime(null);
        actual.setCreateTime(null);
        Assert.assertEquals(snippet, actual);
    }

    @Test
    public void list_listSnippets_listSucceed() {
        Snippet snippet = TestRandom.nextObject(Snippet.class);
        snippetsDAO.insert(snippet);
        List<Snippet> actual = snippetsDAO.list(snippet.getUserId());
        snippet.setModifyTime(null);
        snippet.setCreateTime(null);
        actual.forEach(snippet1 -> {
            snippet1.setModifyTime(null);
            snippet1.setCreateTime(null);
        });
        Assert.assertEquals(Collections.singletonList(snippet), actual);
    }

    @Test
    public void update_updateSnippet_updateSucceed() {
        Snippet snippet = TestRandom.nextObject(Snippet.class);
        snippetsDAO.insert(snippet);
        snippet.setBody("new body");
        snippet.setDescription("new desp");
        snippet.setPrefix("new prefix");
        snippet.setType("new type");
        snippetsDAO.update(snippet);
        Snippet actual = snippetsDAO.get(snippet.getId());
        snippet.setModifyTime(null);
        snippet.setCreateTime(null);
        actual.setModifyTime(null);
        actual.setCreateTime(null);
        Assert.assertEquals(snippet, actual);
    }

    @Test
    public void delete_deleteSnippet_deleteSucceed() {
        Snippet snippet = TestRandom.nextObject(Snippet.class);
        snippetsDAO.insert(snippet);
        Assert.assertEquals(1, snippetsDAO.delete(snippet.getId()));
    }

    @Test
    public void deleteAll_deleteAllSnippets_deleteSucceed() {
        snippetsDAO.deleteAll();
        Snippet snippet = TestRandom.nextObject(Snippet.class);
        snippetsDAO.insert(snippet);
        snippet = TestRandom.nextObject(Snippet.class);
        snippetsDAO.insert(snippet);
        Assert.assertEquals(2, snippetsDAO.deleteAll());
    }

    @Test
    public void queryByUserIdAndName_queryByUserIdAndName_querySucceed() {
        Snippet snippet = TestRandom.nextObject(Snippet.class);
        snippetsDAO.insert(snippet);
        Snippet query = new Snippet();
        query.setUserId(snippet.getUserId());
        query.setPrefix(snippet.getPrefix());
        Snippet actual = snippetsDAO.queryByUserIdAndName(query);
        snippet.setModifyTime(null);
        snippet.setCreateTime(null);
        actual.setModifyTime(null);
        actual.setCreateTime(null);
        Assert.assertEquals(snippet, actual);
    }

}
