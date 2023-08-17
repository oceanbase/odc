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
package com.oceanbase.odc.service.objectstorage.operator;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Optional;

import com.oceanbase.odc.metadb.objectstorage.ObjectBlockEntity;
import com.oceanbase.odc.metadb.objectstorage.ObjectBlockRepository;

/**
 * @Author: Lebie
 * @Date: 2022/3/15 下午6:18
 * @Description: []
 */
public class ObjectBlockIterator implements Iterator<byte[]> {

    private long index;

    private byte[] nextBlock;

    private final ObjectBlockRepository blockRepository;

    private final String objectId;

    public ObjectBlockIterator(ObjectBlockRepository blockRepository, String objectId) {
        this.blockRepository = blockRepository;
        this.objectId = objectId;
    }

    @Override
    public boolean hasNext() {
        Optional<byte[]> block = getOneBlock();
        if (block.isPresent()) {
            nextBlock = block.get();
            return true;
        }
        return false;
    }

    @Override
    public byte[] next() {
        // 如果下一块是 null，那就重新到数据库中查询一次后判断是否为 null.
        if (nextBlock == null) {
            this.nextBlock = getOneBlock().orElseThrow(() -> new NoSuchElementException(
                    String.format("No block exist, objectId=%s, idx=%s", objectId, index)));
        }
        ++index;
        return nextBlock;
    }

    private Optional<byte[]> getOneBlock() {
        return blockRepository.findByObjectIdAndIndex(objectId, index).map(ObjectBlockEntity::getContent);
    }

}
