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
package com.oceanbase.odc.service.datatransfer.loader;

import java.io.File;
import java.nio.file.StandardCopyOption;

import org.apache.commons.io.FileUtils;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ThirdPartyOutputConverter {

    public static void convert(File input) {
        try {
            ThirdPartyOutput outPutConverter = getOutPutConverter(input);
            if (outPutConverter != null) {
                File dest = getDestFile(input, outPutConverter.getNewFilePrefix());

                outPutConverter.toObLoaderDumperCompatibleFormat(dest);
                FileUtils.moveFile(dest, input, StandardCopyOption.REPLACE_EXISTING);

                log.info("Successfully convert input file {} of {} into new file format!", input.getName(),
                        outPutConverter.getNewFilePrefix());
            }
        } catch (Exception e) {
            log.warn("Failed to parse file, will use origin file.", e);
        }
    }

    private static ThirdPartyOutput getOutPutConverter(File input) throws Exception {
        String filename = input.getName();
        if (filename.endsWith(".zip")) {
            PlSqlMultiFileOutput plSqlMultiFileOutput = new PlSqlMultiFileOutput(input);
            if (plSqlMultiFileOutput.supports()) {
                return plSqlMultiFileOutput;
            }
        } else if (filename.endsWith(".sql")) {
            PlSqlSingleFileOutput plSqlSingleFileOutput = new PlSqlSingleFileOutput(input);
            if (plSqlSingleFileOutput.supports()) {
                return plSqlSingleFileOutput;
            }
        }
        return null;
    }

    private static File getDestFile(File input, String prefix) {
        File parentFile = input.getParentFile();
        String newFilename = prefix + "_" + input.getName();
        return new File(parentFile.getAbsolutePath() + File.separator + newFilename);
    }

}
