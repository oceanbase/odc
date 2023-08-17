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
package com.oceanbase.odc.core.sql.execute.cache;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.oceanbase.odc.core.sql.execute.cache.model.BinaryContentMetaData;
import com.oceanbase.odc.core.sql.execute.cache.model.BinaryVirtualElement;
import com.oceanbase.odc.core.sql.execute.cache.model.CommonVirtualElement;
import com.oceanbase.odc.core.sql.execute.cache.table.CrossLinkedVirtualTable;
import com.oceanbase.odc.core.sql.execute.cache.table.VirtualColumn;
import com.oceanbase.odc.core.sql.execute.cache.table.VirtualElement;
import com.oceanbase.odc.core.sql.execute.cache.table.VirtualLine;
import com.oceanbase.odc.core.sql.execute.cache.table.VirtualTable;
import com.oceanbase.odc.core.sql.execute.cache.table.VirtualTableEventListener;

/**
 * Test case for {@code VirtualTable}
 *
 * @author yh263208
 * @date 2021-11-04 19:47
 * @since ODC_release_3.2.2
 */
public class CrossLinkedVirtualTableTest {

    private final String tableId = "test_table";
    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void testPutElement() {
        CrossLinkedVirtualTable virtualTable = generateVirtualTable();
        VirtualElement[][] elements = generateElementMatrix(10, 5);
        int[] lineIndexes = new int[] {
                0, 1, 2, 3, 4, 5, 6, 7, 8, 9
        };
        int[] columnIndexes = new int[] {
                0, 1, 2, 3, 4
        };
        rowFirstInsertionMatrix(lineIndexes, columnIndexes, elements, virtualTable);
    }

    @Test
    public void testPutBinaryMetaElement() {
        CrossLinkedVirtualTable virtualTable = generateVirtualTable();
        VirtualElement[][] elements = generateMetaElementMatrix(10, 5);
        int[] lineIndexes = new int[] {
                0, 1, 2, 3, 4, 5, 6, 7, 8, 9
        };
        int[] columnIndexes = new int[] {
                0, 1, 2, 3, 4
        };
        rowFirstInsertionMatrix(lineIndexes, columnIndexes, elements, virtualTable);
    }

    @Test
    public void testPutElementRank() {
        CrossLinkedVirtualTable virtualTable = generateVirtualTable();
        VirtualElement[][] elements = generateElementMatrix(10, 5);
        int[] lineIndexes = new int[] {
                0, 1, 2, 3, 4, 5, 6, 7, 8, 9
        };
        int[] columnIndexes = new int[] {
                0, 1, 2, 3, 4
        };
        columnFirstInsertionMatrix(lineIndexes, columnIndexes, elements, virtualTable);
    }

    @Test
    public void testForwardInsertColumnNodeList() {
        CrossLinkedVirtualTable virtualTable = generateVirtualTable();
        VirtualElement[][] elements = generateElementMatrix(10, 5);
        int[] lineIndexes = new int[] {
                9, 8, 7, 6, 5, 4, 3, 2, 1, 0
        };
        int[] columnIndexes = new int[] {
                4, 3, 2, 1, 0
        };
        rowFirstInsertionMatrix(lineIndexes, columnIndexes, elements, virtualTable);
    }

    @Test
    public void testForwardInsertColumnNodeListRank() {
        CrossLinkedVirtualTable virtualTable = generateVirtualTable();
        VirtualElement[][] elements = generateElementMatrix(10, 5);
        int[] lineIndexes = new int[] {
                9, 8, 7, 6, 5, 4, 3, 2, 1, 0
        };
        int[] columnIndexes = new int[] {
                4, 3, 2, 1, 0
        };
        columnFirstInsertionMatrix(lineIndexes, columnIndexes, elements, virtualTable);
    }

    @Test
    public void testMiddleInsertColumnNodeList() {
        CrossLinkedVirtualTable virtualTable = generateVirtualTable();
        VirtualElement[][] elements = generateElementMatrix(10, 5);
        int[] lineIndexes = new int[] {
                0, 9, 8, 1, 7, 2, 6, 3, 5, 4
        };
        int[] columnIndexes = new int[] {
                0, 4, 1, 3, 2
        };
        rowFirstInsertionMatrix(lineIndexes, columnIndexes, elements, virtualTable);
    }

    @Test
    public void testMiddleInsertColumnNodeListRank() {
        CrossLinkedVirtualTable virtualTable = generateVirtualTable();
        VirtualElement[][] elements = generateElementMatrix(10, 5);
        int[] lineIndexes = new int[] {
                0, 9, 8, 1, 7, 2, 6, 3, 5, 4
        };
        int[] columnIndexes = new int[] {
                0, 4, 1, 3, 2
        };
        columnFirstInsertionMatrix(lineIndexes, columnIndexes, elements, virtualTable);
    }

    @Test
    public void testReplaceAnElement() {
        CrossLinkedVirtualTable virtualTable = generateVirtualTable();
        VirtualElement element = getElement(1, 2, "1-2");
        virtualTable.put(element);
        Assert.assertEquals(virtualTable.get(1L, 2).getContent(), "1-2");

        element = getElement(1, 2, "1-5");
        virtualTable.put(element);
        Assert.assertEquals(virtualTable.get(1L, 2).getContent(), "1-5");

    }

    @Test
    public void testGetNullElement() {
        CrossLinkedVirtualTable virtualTable = generateVirtualTable();
        VirtualElement element = getElement(1, 2, "1-2");
        virtualTable.put(element);

        element = getElement(1, 5, "1-5");
        virtualTable.put(element);

        element = getElement(1, 4, "1-4");
        virtualTable.put(element);

        Assert.assertEquals(virtualTable.get(1L, 4).getContent(), "1-4");
        Assert.assertNull(virtualTable.get(0L, 0));
        Assert.assertNull(virtualTable.get(1L, 0));
    }

    @Test
    public void testEventListener() {
        CrossLinkedVirtualTable virtualTable = generateVirtualTable();
        VirtualElement[][] elements = generateElementMatrix(5, 4);
        AtomicInteger nonNullElementCount = new AtomicInteger(0);
        AtomicInteger lineNum = new AtomicInteger(0);
        AtomicInteger columnNum = new AtomicInteger(0);
        virtualTable.addListener(new VirtualTableEventListener() {
            @Override
            public void onElementPut(VirtualTable virtualTable, VirtualElement element) {
                nonNullElementCount.getAndIncrement();
            }

            @Override
            public void onColumnAdded(VirtualTable virtualTable, VirtualColumn virtualColumn) {
                columnNum.getAndIncrement();
            }

            @Override
            public void onLineAdded(VirtualTable virtualTable, VirtualLine virtualLine) {
                lineNum.getAndIncrement();
            }
        });
        int[] lineIndexes = new int[] {
                0, 1, 2, 3, 4
        };
        int[] columnIndexes = new int[] {
                0, 1, 2, 3
        };
        columnFirstInsertionMatrix(lineIndexes, columnIndexes, elements, virtualTable);
        for (VirtualElement[] line : elements) {
            for (VirtualElement elt : line) {
                if (elt != null) {
                    nonNullElementCount.decrementAndGet();
                }
            }
        }
        Assert.assertEquals(nonNullElementCount.get(), 0);
    }

    @Test
    public void testSelectLines() {
        CrossLinkedVirtualTable virtualTable = generateVirtualTable();
        VirtualElement[][] elements = generateElementMatrix(10, 5);
        int[] lineIndexes = new int[] {
                0, 1, 2, 3, 4, 5, 6, 7, 8, 9
        };
        int[] columnIndexes = new int[] {
                0, 1, 2, 3, 4
        };
        rowFirstInsertionMatrix(lineIndexes, columnIndexes, elements, virtualTable);
        VirtualTable selectedTable = virtualTable.select(virtualLine -> {
            for (VirtualElement element : virtualLine) {
                if (Objects.equals(element.getContent(), "3-2")) {
                    return true;
                }
            }
            return false;
        });
        selectedTable.forEach(virtualLine -> Assert.assertEquals((long) virtualLine.rowId(), 3));
    }

    @Test
    public void testProjectColumn() {
        CrossLinkedVirtualTable virtualTable = generateVirtualTable();
        VirtualElement[][] elements = generateElementMatrix(10, 5);
        int[] lineIndexes = new int[] {
                0, 1, 2, 3, 4, 5, 6, 7, 8, 9
        };
        int[] columnIndexes = new int[] {
                0, 1, 2, 3, 4
        };
        rowFirstInsertionMatrix(lineIndexes, columnIndexes, elements, virtualTable);
        VirtualTable projectedTable = virtualTable.project(Arrays.asList(2, 3), virtualColumn -> virtualColumn);
        projectedTable.forEach(virtualLine -> {
            for (VirtualElement element : virtualLine) {
                Assert.assertTrue(element.columnId() == 2 || element.columnId() == 3);
            }
        });
    }

    @Test
    public void testProjectNullColumn() {
        CrossLinkedVirtualTable virtualTable = generateVirtualTable();
        VirtualElement[][] elements = generateElementMatrix(10, 5);
        int[] lineIndexes = new int[] {
                0, 1, 2, 3, 4, 5, 6, 7, 8, 9
        };
        int[] columnIndexes = new int[] {
                0, 1, 2, 3, 4
        };
        rowFirstInsertionMatrix(lineIndexes, columnIndexes, elements, virtualTable);

        thrown.expect(NullPointerException.class);
        thrown.expectMessage("Column with Id 5 is not found");
        virtualTable.project(Arrays.asList(2, 5), virtualColumn -> virtualColumn);
    }

    private VirtualElement getElement(long rowId, int columnId, Object value) {
        if (value instanceof BinaryContentMetaData) {
            return new BinaryVirtualElement(tableId, rowId, columnId, "test_type", "test_name",
                    (BinaryContentMetaData) value);
        }
        return new CommonVirtualElement(tableId, rowId, columnId, "test_type", "test_name", value);
    }

    private CrossLinkedVirtualTable generateVirtualTable() {
        return new CrossLinkedVirtualTable(tableId);
    }

    private VirtualElement[][] generateElementMatrix(int rowNum, int colNum) {
        Random random = new Random();
        VirtualElement[][] elements = new VirtualElement[rowNum][colNum];
        for (int i = 0; i < rowNum; i++) {
            for (int j = 0; j < colNum; j++) {
                if (random.nextInt(10) >= 5) {
                    elements[i][j] = getElement(i, j, i + "-" + j);
                } else {
                    elements[i][j] = null;
                }
            }
        }
        return elements;
    }

    private VirtualElement[][] generateMetaElementMatrix(int rowNum, int colNum) {
        Random random = new Random();
        VirtualElement[][] elements = new VirtualElement[rowNum][colNum];
        for (int i = 0; i < rowNum; i++) {
            for (int j = 0; j < colNum; j++) {
                if (random.nextInt(10) >= 5) {
                    elements[i][j] = getElement(i, j,
                            new BinaryContentMetaData(i + "-" + j, random.nextInt(1000), random.nextInt(1000)));
                } else {
                    elements[i][j] = null;
                }
            }
        }
        return elements;
    }

    private void rowFirstInsertionMatrix(int[] lineIndexes, int[] columnIndexes, VirtualElement[][] elements,
            CrossLinkedVirtualTable virtualTable) {
        Set<Integer> lineSet = new HashSet<>();
        for (int lineIndex : lineIndexes) {
            for (int columnIndex : columnIndexes) {
                VirtualElement elt = elements[lineIndex][columnIndex];
                if (elt != null) {
                    lineSet.add(lineIndex);
                    virtualTable.put(elt);
                }
            }
        }
        Assert.assertEquals(lineSet.size(), (long) virtualTable.count());
        checkMatrixSequence(elements, virtualTable);
    }

    private void columnFirstInsertionMatrix(int[] lineIndexes, int[] columnIndexes, VirtualElement[][] elements,
            CrossLinkedVirtualTable virtualTable) {
        Set<Integer> lineSet = new HashSet<>();
        for (int columnIndex : columnIndexes) {
            for (int lineIndex : lineIndexes) {
                VirtualElement element = elements[lineIndex][columnIndex];
                if (element != null) {
                    lineSet.add(lineIndex);
                    virtualTable.put(element);
                }
            }
        }
        Assert.assertEquals(lineSet.size(), (long) virtualTable.count());
        checkMatrixSequence(elements, virtualTable);
    }

    private void checkMatrixSequence(VirtualElement[][] elements, VirtualTable virtualTable) {
        List<Integer> lines = new LinkedList<>();
        int lineNum = elements.length;
        for (int i = 0; i < lineNum; i++) {
            VirtualElement[] elts = elements[i];
            boolean existLine = false;
            for (VirtualElement elt : elts) {
                if (elt != null) {
                    existLine = true;
                    break;
                }
            }
            if (existLine) {
                lines.add(i);
            }
        }
        AtomicInteger lineIndex = new AtomicInteger(0);
        virtualTable.forEach(virtualLine -> {
            int columnNum = 0;
            VirtualElement[] lineElts = elements[lines.get(lineIndex.getAndIncrement())];
            List<VirtualElement> checkList = new LinkedList<>();
            for (VirtualElement elt : lineElts) {
                if (elt != null) {
                    checkList.add(elt);
                }
            }
            for (VirtualElement element : virtualLine) {
                VirtualElement secondElt = checkList.get(columnNum++);
                Assert.assertEquals(element.getContent(), secondElt.getContent());
            }
            Assert.assertEquals(columnNum, checkList.size());
        });
    }

}
