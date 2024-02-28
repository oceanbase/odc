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
package com.oceanbase.odc.service.dlm;

import com.oceanbase.odc.core.shared.exception.UnsupportedException;
import com.oceanbase.odc.service.dlm.model.DlmTask;
import com.oceanbase.tools.migrator.core.meta.JobMeta;
import com.oceanbase.tools.migrator.job.AbstractJob;
import com.oceanbase.tools.migrator.job.DeleteJob;
import com.oceanbase.tools.migrator.job.MigrateJob;
import com.oceanbase.tools.migrator.job.QuickDeleteJob;
import com.oceanbase.tools.migrator.job.RollbackJob;

import lombok.NoArgsConstructor;

/**
 * @Author：tinker
 * @Date: 2023/5/9 14:38
 * @Descripition:
 */
@NoArgsConstructor
public class DataArchiveJobFactory {

    private JobMetaFactory jobMetaFactory;

    public DataArchiveJobFactory(JobMetaFactory jobMetaFactory) {
        this.jobMetaFactory = jobMetaFactory;
    }

    public AbstractJob createJob(DlmTask parameters) throws Exception {
        return createJob(jobMetaFactory.create(parameters));
    }

    public AbstractJob createJob(JobMeta jobMeta) throws Exception {
        AbstractJob job;
        switch (jobMeta.getJobType()) {
            case MIGRATE: {
                job = new MigrateJob();
                break;
            }
            case DELETE: {
                job = new DeleteJob();
                break;
            }
            case ROLLBACK: {
                job = new RollbackJob();
                break;
            }
            case QUICK_DELETE: {
                job = new QuickDeleteJob();
                break;
            }
            default:
                throw new UnsupportedException();
        }
        job.setJobMeta(jobMeta);
        return job;
    }

}
