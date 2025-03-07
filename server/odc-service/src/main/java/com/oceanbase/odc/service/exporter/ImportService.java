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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.oceanbase.odc.metadb.export.ImportFileRowHistoryEntity;
import com.oceanbase.odc.metadb.export.ImportFileRowHistoryRepository;
import com.oceanbase.odc.service.exporter.model.ExportProperties;

@Service
public class ImportService {

    @Autowired
    private ImportFileRowHistoryRepository importFileRowHistoryRepository;

    public boolean imported(String fileSignature, String rowId) {
        return importFileRowHistoryRepository.existsByFileSignatureAndRowId(fileSignature, rowId);
    }

    public boolean imported(ExportProperties exportProperties, String rowId) {
        String fileSignature = exportProperties.getStringValue(SIGNATURE);
        return imported(fileSignature, rowId);
    }

    @Transactional
    public void importAndSaveHistory(String fileSignature, String rowId, Runnable doImport) {
        doImport.run();
        ImportFileRowHistoryEntity exportFileRowHistoryEntity = new ImportFileRowHistoryEntity();
        exportFileRowHistoryEntity.setFileSignature(fileSignature);
        exportFileRowHistoryEntity.setRowId(rowId);
        importFileRowHistoryRepository.save(exportFileRowHistoryEntity);
    }

    @Transactional
    public void importAndSaveHistory(ExportProperties exportProperties, String rowId, Runnable doImport) {
        String fileSignature = exportProperties.getStringValue(SIGNATURE);
        importAndSaveHistory(fileSignature, rowId, doImport);
    }
}
