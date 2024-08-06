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
package com.oceanbase.odc.service.worksheet;

import java.util.List;
import java.util.Set;

import com.oceanbase.odc.service.worksheet.model.BatchOperateWorksheetsResp;
import com.oceanbase.odc.service.worksheet.model.BatchUploadWorksheetsReq;
import com.oceanbase.odc.service.worksheet.model.UpdateWorksheetReq;
import com.oceanbase.odc.service.worksheet.model.WorksheetMetaResp;
import com.oceanbase.odc.service.worksheet.model.WorksheetResp;

/**
 * the facade service for project worksheets management, used to choreograph the different
 * behavioral logic of Worksheets and GitRepos。
 * </p>
 * worksheet is the abstract of worksheet/folder in /Worksheets/ and /Reps/RepoName/
 * </p>
 * 
 * @author keyang
 * @date 2024/08/06
 * @since 4.3.2
 */
public interface WorksheetServiceFacade {
    /**
     * generate an oss upload url with an expiration date for upload worksheet content in frontend.
     *
     *
     * @param projectId project id
     * @return temp oss upload url
     */
    String generateUploadUrl(Long projectId);

    /**
     * create worksheet.
     * </p>
     * the content of worksheet is already uploaded to oss in frontend, and in backend, only need to
     * save objectKey.
     *
     * @param projectId project id
     * @param pathStr worksheet path
     * @param objectKey the oss objectKey of create worksheet
     * @return mete info of created worksheet
     */
    WorksheetMetaResp createWorksheet(Long projectId, String pathStr, String objectKey);

    /**
     * get worksheet detail info
     *
     * @param projectId project id
     * @param pathStr worksheet path
     * @return detail worksheet info
     */
    WorksheetResp getWorksheetDetail(Long projectId, String pathStr);

    /**
     * list next level worksheets of pathStr
     *
     *
     * @param projectId project id
     * @param pathStr worksheet path
     * @return the next level worksheet meta list.If the worksheet path is worksheet,return empty.
     */
    List<WorksheetMetaResp> listWorksheets(Long projectId, String pathStr);

    /**
     * search project worksheets by nameLike in project
     *
     * @param projectId project id
     * @param nameLike query criteria for fuzzy matching of worksheet name
     * @return fit worksheet meta list.It will return empty if the nameLike is blank.
     */
    List<WorksheetMetaResp> searchWorksheets(Long projectId, String nameLike);

    /**
     * batch upload worksheets in a directory.
     *
     * @param projectId project id
     * @param req request
     * @return It could be a partial success and partial failure
     */
    BatchOperateWorksheetsResp batchUploadWorksheets(Long projectId, BatchUploadWorksheetsReq req);

    /**
     * batch delete worksheets
     *
     * @param projectId project id
     * @param paths path list to delete
     * @return It could be a partial success and partial failure
     */
    BatchOperateWorksheetsResp batchDeleteWorksheets(Long projectId, List<String> paths);

    /**
     * rename worksheet
     *
     * @param projectId project id
     * @param pathStr worksheet path
     * @param destination worksheet path after rename.If the destination equals to pathStr ,it will
     *        throw exception.
     * @return the update worksheet meta list after rename
     */
    List<WorksheetMetaResp> renameWorksheet(Long projectId, String pathStr, String destination);

    /**
     * edit worksheet
     *
     * @param projectId project id
     * @param pathStr worksheet path
     * @param req edit request
     * @return the update worksheet meta list after edit
     */
    List<WorksheetMetaResp> editWorksheet(Long projectId, String pathStr, UpdateWorksheetReq req);

    /**
     * batch download worksheets
     *
     * @param projectId project id
     * @param paths worksheet path list to download
     * @return the oss url to download need worksheets
     */
    String batchDownloadWorksheets(Long projectId, Set<String> paths);
}