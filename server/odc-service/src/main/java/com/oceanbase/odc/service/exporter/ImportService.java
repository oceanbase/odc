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
package com.oceanbase.odc.service.exporter;

import static com.oceanbase.odc.service.exporter.model.ExportConstants.SIGNATURE;

import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.oceanbase.odc.metadb.export.ImportFileRowHistoryEntity;
import com.oceanbase.odc.metadb.export.ImportFileRowHistoryRepository;
import com.oceanbase.odc.service.exporter.model.ExportProperties;

@Service
public class ImportService {

    private static final Logger log = LoggerFactory.getLogger(ImportService.class);
    @Autowired
    private ImportFileRowHistoryRepository importFileRowHistoryRepository;

    public boolean imported(String fileSignature, String rowId) {
        return importFileRowHistoryRepository.existsByFileSignatureAndRowId(fileSignature, rowId);
    }

    public boolean imported(ExportProperties exportProperties, String rowId) {
        String fileSignature = exportProperties.getStringValue(SIGNATURE);
        log.info("imported file signature: {}", fileSignature);
        return imported(fileSignature, rowId);
    }

    @Transactional
    public void importAndSaveHistory(String fileSignature, String rowId, Callable<Boolean> doImport) {
        try {
            Boolean success = doImport.call();
            if (success) {
                ImportFileRowHistoryEntity exportFileRowHistoryEntity = new ImportFileRowHistoryEntity();
                exportFileRowHistoryEntity.setFileSignature(fileSignature);
                exportFileRowHistoryEntity.setRowId(rowId);
                importFileRowHistoryRepository.save(exportFileRowHistoryEntity);
            }
        } catch (Exception e) {
            log.error("Save import history failed", e);
            throw new RuntimeException(e);
        }
    }

    @Transactional
    public void importAndSaveHistory(ExportProperties exportProperties, String rowId, Callable<Boolean> doImport) {
        String fileSignature = exportProperties.getStringValue(SIGNATURE);
        importAndSaveHistory(fileSignature, rowId, doImport);
    }
}
