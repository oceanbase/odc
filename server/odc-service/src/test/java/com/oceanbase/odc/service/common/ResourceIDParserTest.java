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
package com.oceanbase.odc.service.common;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.oceanbase.odc.service.common.model.ResourceIdentifier;
import com.oceanbase.odc.service.common.util.ResourceIDParser;

public class ResourceIDParserTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void testParseDatabase() {
        ResourceIdentifier identifier = ResourceIDParser.parse("sid:1:d:d_test");
        Assert.assertTrue(identifier.getSid().equals("1"));
        Assert.assertTrue(identifier.getDatabase().equals("d_test"));
    }

    @Test
    public void testParseTable() {
        ResourceIdentifier identifier = ResourceIDParser.parse("sid:1:d:test:t:t_test");
        Assert.assertTrue(identifier.getSid().equals("1"));
        Assert.assertTrue(identifier.getTable().equals("t_test"));
    }

    @Test
    public void testParseColumn() {
        ResourceIdentifier identifier = ResourceIDParser.parse("sid:2:d:test:t:t_test:c:c_test");
        Assert.assertTrue(identifier.getSid().equals("2"));
        Assert.assertTrue(identifier.getColumn().equals("c_test"));
    }

    @Test
    public void testParseIndex() {
        ResourceIdentifier identifier = ResourceIDParser.parse("sid:3-1:d:test:t:t_test:i:i_test");
        Assert.assertTrue(identifier.getSid().equals("3-1"));
        Assert.assertTrue(identifier.getIndex().equals("i_test"));
    }

    @Test
    public void testParseFunction() {
        ResourceIdentifier identifier = ResourceIDParser.parse("sid:2:d:test:t:t_test:f:f_test");
        Assert.assertTrue(identifier.getFunction().equals("f_test"));
    }

    @Test
    public void testParseProcedure() {
        ResourceIdentifier identifier = ResourceIDParser.parse("sid:2:d:test:t:t_test:p:p_test");
        Assert.assertTrue(identifier.getProcedure().equals("p_test"));
    }

    @Test
    public void testParsePartition() {
        ResourceIdentifier identifier = ResourceIDParser.parse("sid:3:d:test:t:t_test:tp:tp_test");
        Assert.assertTrue(identifier.getPartition().equals("tp_test"));
    }

    @Test
    public void testParsePackage() {
        ResourceIdentifier identifier = ResourceIDParser.parse("sid:3:d:test:t:t_test:pkg:pk_test");
        Assert.assertTrue(identifier.getPkg().equals("pk_test"));
    }

    @Test
    public void testSequence() {
        ResourceIdentifier identifier = ResourceIDParser.parse("sid:3:d:test:s:seq1");
        Assert.assertTrue(identifier.getSequence().equals("seq1"));
    }

    @Test
    public void testParseView() {
        ResourceIdentifier identifier = ResourceIDParser.parse("sid:3:d:test:v:v_test:tp:tp_test");
        Assert.assertTrue(identifier.getView().equals("v_test"));
    }

    @Test
    public void testParseVariableScope() {
        ResourceIdentifier identifier = ResourceIDParser.parse("sid:3:d:test:var:session:tp:tp_test");
        Assert.assertTrue(identifier.getVariableScope().equals("session"));
    }

    @Test
    public void parse_static_connection_id() {
        ResourceIdentifier identifier = ResourceIDParser.parse("1001");
        Assert.assertEquals(identifier.getSid(), "1001");
    }

    @Test
    public void parse_view_name_with_colon() {
        ResourceIdentifier identifier = ResourceIDParser.parse("sid:1000769-5:d:LEGEND:v::new");
        Assert.assertEquals(identifier.getView(), ":new");
    }

    @Test
    public void parse_view_name_with_colon_inside() {
        ResourceIdentifier identifier = ResourceIDParser.parse("sid:1000769-5:d:LEGEND:v:n:ew");
        Assert.assertEquals(identifier.getView(), "n:ew");
    }

    @Test
    public void parse_view_name_with_colon_begin() {
        String sid = "sid:1001-1:d:abc:v::view1";
        ResourceIdentifier identifier = ResourceIDParser.parse(sid);
        Assert.assertEquals(identifier.getSid(), "1001-1");
        Assert.assertEquals(identifier.getDatabase(), "abc");
        Assert.assertEquals(identifier.getView(), ":view1");
    }

    @Test
    public void testParse_NotEndWithColon_Success() {
        ResourceIdentifier identifier = ResourceIDParser.parse("sid:12-1:d:CHZ:t:TEST");
        Assert.assertEquals("12-1", identifier.getSid());
        Assert.assertEquals("CHZ", identifier.getDatabase());
        Assert.assertEquals("TEST", identifier.getTable());
    }

    @Test
    public void testParse_EndWithSingleColon_Success() {
        ResourceIdentifier identifier = ResourceIDParser.parse("sid:12-1:d:CHZ:t:TEST:");
        Assert.assertEquals("12-1", identifier.getSid());
        Assert.assertEquals("CHZ", identifier.getDatabase());
        Assert.assertEquals("TEST:", identifier.getTable());
    }

    public void testParse_EndWithMultiColons_Success() {
        ResourceIdentifier identifier = ResourceIDParser.parse("sid:12-1:d:CHZ:t:TEST::");
        Assert.assertEquals("12-1", identifier.getSid());
        Assert.assertEquals("CHZ", identifier.getDatabase());
        Assert.assertEquals("TEST::", identifier.getTable());
    }

    @Test
    public void testParse_WithSingleColonInside_Success() {
        ResourceIdentifier identifier = ResourceIDParser.parse("sid:12-1:d:CHZ::t:TEST");
        Assert.assertEquals("12-1", identifier.getSid());
        Assert.assertEquals("CHZ:", identifier.getDatabase());
        Assert.assertEquals("TEST", identifier.getTable());
    }

    @Test
    public void testParse_WithMultiColonsInside_Success() {
        ResourceIdentifier identifier = ResourceIDParser.parse("sid:12-1:d:CHZ:::t:TEST");
        Assert.assertEquals("12-1", identifier.getSid());
        Assert.assertEquals("CHZ::", identifier.getDatabase());
        Assert.assertEquals("TEST", identifier.getTable());
    }
}
