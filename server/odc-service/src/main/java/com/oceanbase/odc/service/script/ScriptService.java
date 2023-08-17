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
package com.oceanbase.odc.service.script;

import static com.oceanbase.odc.service.script.model.ScriptConstants.CONTENT_ABSTRACT_LENGTH;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.apache.commons.compress.utils.Lists;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.oceanbase.odc.common.util.ExceptionUtils;
import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.core.authority.util.SkipAuthorize;
import com.oceanbase.odc.core.shared.PreConditions;
import com.oceanbase.odc.core.shared.constant.ErrorCodes;
import com.oceanbase.odc.core.shared.constant.LimitMetric;
import com.oceanbase.odc.core.shared.constant.ResourceType;
import com.oceanbase.odc.core.shared.exception.ConflictException;
import com.oceanbase.odc.core.shared.exception.InternalServerError;
import com.oceanbase.odc.core.shared.exception.NotFoundException;
import com.oceanbase.odc.core.shared.exception.OverLimitException;
import com.oceanbase.odc.core.shared.exception.UnexpectedException;
import com.oceanbase.odc.metadb.script.ScriptMetaEntity;
import com.oceanbase.odc.metadb.script.ScriptMetaRepository;
import com.oceanbase.odc.metadb.script.ScriptMetaSpecs;
import com.oceanbase.odc.service.common.FileChecker;
import com.oceanbase.odc.service.iam.auth.AuthenticationFacade;
import com.oceanbase.odc.service.objectstorage.ObjectStorageFacade;
import com.oceanbase.odc.service.objectstorage.model.ObjectMetadata;
import com.oceanbase.odc.service.objectstorage.model.StorageObject;
import com.oceanbase.odc.service.script.model.QueryScriptMetaParams;
import com.oceanbase.odc.service.script.model.Script;
import com.oceanbase.odc.service.script.model.ScriptMeta;
import com.oceanbase.odc.service.script.model.ScriptProperties;
import com.oceanbase.odc.service.script.model.UpdateScriptReq;
import com.oceanbase.odc.service.script.util.ScriptMetaMapper;
import com.oceanbase.odc.service.script.util.ScriptUtils;

import lombok.extern.slf4j.Slf4j;

/**
 * @Author: Lebie
 * @Date: 2022/3/21 下午9:00
 * @Description: []
 */
@Slf4j
@Service
@SkipAuthorize("personal resource")
public class ScriptService {

    @Autowired
    private ScriptProperties scriptProperties;

    @Autowired
    private AuthenticationFacade authenticationFacade;

    @Autowired
    private ObjectStorageFacade objectStorageFacade;

    @Autowired
    private ScriptMetaRepository scriptMetaRepository;

    @Autowired
    private FileChecker fileChecker;

    private ScriptMetaMapper scriptMetaMapper = ScriptMetaMapper.INSTANCE;

    public Page<ScriptMeta> list(Pageable pageable) {
        String bucketName = ScriptUtils.getPersonalBucketName(authenticationFacade.currentUserIdStr());
        return scriptMetaRepository.findAll(ScriptMetaSpecs.of(QueryScriptMetaParams.builder().bucketName(bucketName)
                .creatorId(authenticationFacade.currentUserId()).build()), pageable)
                .map(scriptMetaMapper::entityToModel);
    }

    public List<ScriptMeta> batchPutScript(List<MultipartFile> files) {
        String bucketName = ScriptUtils.getPersonalBucketName(authenticationFacade.currentUserIdStr());
        objectStorageFacade.createBucketIfNotExists(bucketName);
        List<ScriptMeta> returnVal = Lists.newArrayList();
        for (MultipartFile file : files) {
            fileChecker.validateSuffix(file.getOriginalFilename());
            if (file.getSize() > scriptProperties.getMaxUploadLength()) {
                log.info("uploading script file size is over limit, maxUploadLength={}",
                        scriptProperties.getMaxUploadLength());
                throw new OverLimitException(LimitMetric.FILE_SIZE, (double) scriptProperties.getMaxUploadLength(),
                        "file size is over limit");
            }
            try (InputStream inputStream = file.getInputStream()) {
                ObjectMetadata objectMetadata =
                        objectStorageFacade.putObject(bucketName, file.getOriginalFilename(), file.getSize(),
                                inputStream);
                ScriptMeta scriptMeta = saveScriptMeta(objectMetadata, file.getInputStream());
                returnVal.add(scriptMeta);
            } catch (IOException ex) {
                log.warn("put object failed, cause={}", ex.getMessage());
                throw new InternalServerError("put object failed", ex);
            }
        }
        return returnVal;
    }

    public List<ScriptMeta> batchDeleteScript(List<Long> ids) {
        PreConditions.notEmpty(ids, "ids", "ids cannot be empty");
        String bucketName = ScriptUtils.getPersonalBucketName(authenticationFacade.currentUserIdStr());
        List<ScriptMeta> deletedScripts = Lists.newArrayList();
        for (Long id : ids) {
            Optional<ScriptMetaEntity> optional = scriptMetaRepository.findByIdAndBucketName(id, bucketName);
            if (!optional.isPresent()) {
                log.info("script not found, id={}", id);
                throw new NotFoundException(ResourceType.ODC_SCRIPT, "id", id);
            }
            scriptMetaRepository.deleteByIdAndBucketName(id, bucketName);
            ObjectMetadata objectMetadata = objectStorageFacade.deleteObject(bucketName, optional.get().getObjectId());
            deletedScripts.add(ScriptMeta.builder().bucketName(bucketName).objectName(objectMetadata.getObjectName())
                    .objectId(optional.get().getObjectId()).build());
        }
        return deletedScripts;
    }

    public Script detail(Long id) throws IOException {
        PreConditions.notNull(id, "id");
        String bucketName = ScriptUtils.getPersonalBucketName(authenticationFacade.currentUserIdStr());
        Optional<ScriptMetaEntity> optional = scriptMetaRepository.findByIdAndBucketName(id, bucketName);
        Script script = Script.builder().build();
        if (!optional.isPresent()) {
            log.info("script not found, id={}", id);
            throw new NotFoundException(ResourceType.ODC_SCRIPT, "id", id);
        }
        StorageObject storageObject = objectStorageFacade.loadObject(bucketName, optional.get().getObjectId());
        script.setScriptMeta(scriptMetaMapper.entityToModel(optional.get()));
        // 考虑前端性能，如果脚本长度超过限制，则只返回限制大小的内容
        if (Objects.nonNull(storageObject.getMetadata())) {
            setContentIfNotOverLimit(
                    IOUtils.toString(storageObject.getContent(), StandardCharsets.UTF_8),
                    script);
        }
        return script;
    }

    public Script updateScript(Long id, UpdateScriptReq req) {
        PreConditions.notNull(id, "id");
        PreConditions.notNull(req, "req");
        PreConditions.notBlank(req.getName(), "req.name");

        fileChecker.validateSuffix(req.getName());

        String bucketName = ScriptUtils.getPersonalBucketName(authenticationFacade.currentUserIdStr());
        Optional<ScriptMetaEntity> optional = scriptMetaRepository.findByIdAndBucketName(id, bucketName);
        if (!optional.isPresent()) {
            log.info("script not found, id={}", id);
            throw new NotFoundException(ResourceType.ODC_SCRIPT, "id", id);
        }
        long totalLength = req.getContent().getBytes(StandardCharsets.UTF_8).length;
        ScriptMetaEntity scriptMetaInDb = optional.get();
        String oldObjectId = scriptMetaInDb.getObjectId();
        ObjectMetadata objectMetadata =
                objectStorageFacade.updateObject(bucketName, req.getName(), scriptMetaInDb.getObjectId(),
                        totalLength, IOUtils.toInputStream(req.getContent(), StandardCharsets.UTF_8));
        scriptMetaInDb.setObjectId(objectMetadata.getObjectId());
        scriptMetaInDb.setBucketName(objectMetadata.getBucketName());
        scriptMetaInDb.setObjectName(objectMetadata.getObjectName());
        scriptMetaInDb.setContentAbstract(getContentAbstract(req.getContent()));
        scriptMetaInDb.setUpdateTime(new Date());
        scriptMetaInDb.setCreatorId(objectMetadata.getCreatorId());
        scriptMetaInDb.setContentLength(objectMetadata.getTotalLength());
        // 保存更新后的 script meta
        int affectRows = scriptMetaRepository.saveAndFlushIfObjectIdRetains(scriptMetaInDb, oldObjectId);
        if (affectRows != 1) {
            log.info("Script is uploading, id={}", id);
            throw new ConflictException(ErrorCodes.FileUploading, null, "File is uploading, id=" + id);
        }

        Script script = Script.builder().build();
        script.setScriptMeta(scriptMetaMapper.entityToModel(scriptMetaRepository.findByIdAndBucketName(id, bucketName)
                .orElseThrow(() -> new NotFoundException(ResourceType.ODC_SCRIPT, "id", id))));
        // 考虑前端性能，如果脚本长度超过限制，则只返回限制大小的内容
        setContentIfNotOverLimit(req.getContent(), script);
        return script;
    }


    public ScriptMeta saveScriptMeta(ObjectMetadata objectMetadata, InputStream inputStream) {
        ScriptMetaEntity entity = ScriptMetaEntity.builder()
                .objectName(objectMetadata.getObjectName())
                .objectId(objectMetadata.getObjectId())
                .creatorId(objectMetadata.getCreatorId())
                .bucketName(objectMetadata.getBucketName())
                .contentLength(objectMetadata.getTotalLength())
                .updateTime(new Date())
                .build();
        String contentAbstract;
        try {
            contentAbstract = getContentAbstract(IOUtils.toString(inputStream, StandardCharsets.UTF_8));
        } catch (IOException ex) {
            log.info("save script meta failed, reason={}", ExceptionUtils.getRootCauseReason(ex));
            throw new UnexpectedException("save script meta failed", ex);
        }
        entity.setContentAbstract(contentAbstract);

        ScriptMetaEntity entityInDb = scriptMetaRepository.saveAndFlush(entity);
        log.info("save script meta successfully, id={}, objectId={}", entityInDb.getId(), entityInDb.getObjectId());
        return scriptMetaMapper.entityToModel(entity);
    }

    private String getContentAbstract(String content) {
        if (StringUtils.isNotEmpty(content)) {
            content = (String) content.subSequence(0,
                    Math.min(content.length(), CONTENT_ABSTRACT_LENGTH));
        }
        return content;
    }

    private Script setContentIfNotOverLimit(String content, Script script) {
        PreConditions.notNull(content, "content");
        PreConditions.notNull(script, "script");
        if (content.getBytes(StandardCharsets.UTF_8).length <= scriptProperties.getMaxEditLength()) {
            script.setContent(content);
        }
        return script;
    }

    public List<String> getDownloadUrl(List<Long> scriptIds) {
        String bucketName = ScriptUtils.getPersonalBucketName(authenticationFacade.currentUserIdStr());
        List<String> downloadUrls = Lists.newArrayList();
        for (Long scriptId : scriptIds) {
            Optional<ScriptMetaEntity> opt = scriptMetaRepository.findByIdAndBucketName(scriptId, bucketName);
            if (opt.isPresent()) {
                downloadUrls.add(objectStorageFacade.getDownloadUrl(bucketName, opt.get().getObjectId()));
            }
        }
        return downloadUrls;
    }

    public ScriptMeta synchronizeScript(Long id) throws IOException {
        PreConditions.notNull(id, "id");
        String bucketName = ScriptUtils.getPersonalBucketName(authenticationFacade.currentUserIdStr());
        Optional<ScriptMetaEntity> optional = scriptMetaRepository.findByIdAndBucketName(id, bucketName);
        if (!optional.isPresent()) {
            log.info("script not found, id={}", id);
            throw new NotFoundException(ResourceType.ODC_SCRIPT, "id", id);
        }
        ScriptMeta scriptMeta = scriptMetaMapper.entityToModel(optional.get());
        objectStorageFacade.loadMetaData(bucketName, scriptMeta.getObjectId());
        return scriptMeta;
    }
}
