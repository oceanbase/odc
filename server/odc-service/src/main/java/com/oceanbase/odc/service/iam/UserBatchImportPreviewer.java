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
package com.oceanbase.odc.service.iam;

import static com.oceanbase.odc.core.shared.constant.FieldName.USER_ACCOUNTNAME;
import static com.oceanbase.odc.core.shared.constant.FieldName.USER_DESCRIPTION;
import static com.oceanbase.odc.core.shared.constant.FieldName.USER_ENABLED;
import static com.oceanbase.odc.core.shared.constant.FieldName.USER_NAME;
import static com.oceanbase.odc.core.shared.constant.FieldName.USER_PASSWORD;
import static com.oceanbase.odc.core.shared.constant.FieldName.USER_ROLEIDS;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.multipart.MultipartFile;

import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.core.authority.util.Authenticated;
import com.oceanbase.odc.core.authority.util.PreAuthenticate;
import com.oceanbase.odc.core.shared.constant.ErrorCodes;
import com.oceanbase.odc.service.common.util.FileConvertUtils;
import com.oceanbase.odc.service.iam.auth.AuthenticationFacade;
import com.oceanbase.odc.service.iam.model.BatchImportUser;
import com.oceanbase.odc.service.iam.model.Role;
import com.oceanbase.odc.service.iam.model.User;
import com.oceanbase.odc.service.iam.model.UserPreviewBatchImportResp;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@Validated
@Authenticated
public class UserBatchImportPreviewer {

    private static final Pattern REG_NUMBER = Pattern.compile(".*\\d+.*");
    private static final Pattern REG_UPPERCASE = Pattern.compile(".*[A-Z]+.*");
    private static final Pattern REG_LOWERCASE = Pattern.compile(".*[a-z]+.*");
    private static final Pattern REG_SYMBOL = Pattern.compile(".*[._+@#$%]+.*");
    private static final Pattern SPACE_PATTERN = Pattern.compile("^(?=\\S).+(?<=[^.\\s])$");
    public static final int PASSWORD_MAX_LENGTH = 32;
    public static final int PASSWORD_MIN_LENGTH = 8;
    public static final int NAME_MAX_LENGTH = 64;
    public static final int ACCOUNTNAME_MAX_LENGTH = 64;
    public static final int ACCOUNTNAME_MIN_LENGTH = 4;
    public static final int DESCRIPTION_MAX_LENGTH = 140;
    public static final int NUMBER_MIN_NUMBER = 2;
    public static final int UPPER_MIN_NUMBER = 2;
    public static final int LOWER_MIN_NUMBER = 2;
    public static final int SYMBOL_MIN_NUMBER = 2;
    @Autowired
    private RoleService roleService;
    @Autowired
    private UserService userService;
    @Autowired
    private AuthenticationFacade authenticationFacade;

    @PreAuthenticate(actions = "create", resourceType = "ODC_USER", isForAll = true)
    public UserPreviewBatchImportResp preview(MultipartFile file) throws IOException {
        if (!checkFileType(file)) {
            return UserPreviewBatchImportResp.ofFail(ErrorCodes.ImportInvalidFileType);
        }
        List<Map<String, String>> list;
        List<BatchImportUser> batchImportUserList = new ArrayList<>();
        try (InputStream inputStream = file.getInputStream()) {
            list = FileConvertUtils.convertXlsRowsToMapList(inputStream);
        }
        Long organizationId = authenticationFacade.currentOrganizationId();
        Set<String> accountNames = userService.getByOrganizationId(organizationId).stream().map(User::getAccountName)
                .collect(Collectors.toSet());
        Map<String, Role> roleName2RoleMap = roleService.list(Pageable.unpaged()).getContent().stream()
                .collect(Collectors.toMap(Role::getName, role -> role, (k1, k2) -> k1));
        for (Map<String, String> map : list) {
            BatchImportUser batchImportUser = createBatchImportUser(map, accountNames, roleName2RoleMap);
            batchImportUserList.add(batchImportUser);
        }
        return UserPreviewBatchImportResp.ofUserExcel(batchImportUserList);
    }

    private boolean checkFileType(MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        if (file.isEmpty()) {
            log.warn("The import file cannot be empty，fileName={}", originalFilename);
            return false;
        }
        if (!StringUtils.endsWithIgnoreCase(originalFilename, ".xls")
                && !StringUtils.endsWithIgnoreCase(originalFilename, ".xlsx")) {
            log.warn("The uploaded file is not an Excel file，fileName={}", originalFilename);
            return false;
        }
        return true;
    }

    private BatchImportUser createBatchImportUser(Map<String, String> map, Set<String> accountNames,
            Map<String, Role> roleName2RoleMap) {
        BatchImportUser batchImportUser = new BatchImportUser();
        String accountName = map.get(USER_ACCOUNTNAME.getLocalizedMessage());
        if (!checkAccountName(accountName) || accountNames.contains(accountName)) {
            batchImportUser.setErrorMessage("file content error:" + USER_ACCOUNTNAME.getLocalizedMessage());
        } else {
            batchImportUser.setAccountName(accountName);
        }
        String name = map.get(USER_NAME.getLocalizedMessage());
        if (StringUtils.isEmpty(name) || name.length() > NAME_MAX_LENGTH || !SPACE_PATTERN.matcher(name).matches()) {
            batchImportUser.setErrorMessage("file content error:" + USER_NAME.getLocalizedMessage());
        } else {
            batchImportUser.setName(name);
        }
        String password = map.get(USER_PASSWORD.getLocalizedMessage());
        if (StringUtils.isEmpty(password) || !checkPassword(password)) {
            batchImportUser.setErrorMessage("file content error:" + USER_PASSWORD.getLocalizedMessage());
        } else {
            batchImportUser.setPassword(password);
        }
        String enabled = map.get(USER_ENABLED.getLocalizedMessage());
        if (!StringUtils.equalsIgnoreCase(enabled, "true") && !StringUtils.equalsIgnoreCase(enabled, "false")) {
            batchImportUser.setErrorMessage("file content error:" + USER_ENABLED.getLocalizedMessage());
        } else {
            batchImportUser.setEnabled(enabled.equalsIgnoreCase("true"));
        }
        String roleNameList = map.get(USER_ROLEIDS.getLocalizedMessage());
        batchImportUser.setRoleIds(Collections.emptyList());
        batchImportUser.setRoleNames(Collections.emptyList());
        if (StringUtils.isNotBlank(roleNameList)) {
            Set<String> roleNames = new HashSet<>(Arrays.asList(roleNameList.split(",")));
            for (String roleName : roleNames) {
                if (roleName2RoleMap.containsKey(roleName)) {
                    Role role = roleName2RoleMap.get(roleName);
                    batchImportUser.getRoleIds().add(role.getId());
                    batchImportUser.getRoleNames().add(role.getName());
                }
            }
            if (batchImportUser.getRoleIds().isEmpty()) {
                batchImportUser.setErrorMessage("file content error:" + USER_ROLEIDS.getLocalizedMessage());
            }
        }
        String description = map.get(USER_DESCRIPTION.getLocalizedMessage());
        if (description.length() > DESCRIPTION_MAX_LENGTH) {
            batchImportUser.setErrorMessage("file content error:" + USER_DESCRIPTION.getLocalizedMessage());
        } else {
            batchImportUser.setDescription(description);
        }
        return batchImportUser;
    }

    private boolean checkPassword(String password) {
        int numberNumber = 0;
        int upperNumber = 0;
        int lowerNumber = 0;
        int symbolNumber = 0;
        if (password.length() < PASSWORD_MIN_LENGTH || password.length() > PASSWORD_MAX_LENGTH
                || password.contains(" ")) {
            return false;
        }
        for (int i = 0; i < password.length(); i++) {
            String pwd = password.substring(i, i + 1);
            if (REG_NUMBER.matcher(pwd).matches())
                numberNumber++;
            else if (REG_LOWERCASE.matcher(pwd).matches())
                upperNumber++;
            else if (REG_UPPERCASE.matcher(pwd).matches())
                lowerNumber++;
            else if (REG_SYMBOL.matcher(pwd).matches())
                symbolNumber++;
            else
                return false;
        }
        if (numberNumber < NUMBER_MIN_NUMBER || upperNumber < UPPER_MIN_NUMBER || lowerNumber < LOWER_MIN_NUMBER
                || symbolNumber < SYMBOL_MIN_NUMBER) {
            return false;
        }
        return true;
    }

    private boolean checkAccountName(String accountName) {
        if (accountName == null || accountName.length() < ACCOUNTNAME_MIN_LENGTH
                || accountName.length() > ACCOUNTNAME_MAX_LENGTH || accountName.contains(" ")) {
            return false;
        }
        for (int i = 0; i < accountName.length(); i++) {
            String name = accountName.substring(i, i + 1);
            if (!REG_NUMBER.matcher(name).matches() && !REG_LOWERCASE.matcher(name).matches()
                    && !REG_UPPERCASE.matcher(name).matches() && !REG_SYMBOL.matcher(name).matches()) {
                return false;
            }
        }
        return true;
    }
}
