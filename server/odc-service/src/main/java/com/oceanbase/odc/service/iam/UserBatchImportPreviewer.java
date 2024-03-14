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
import com.oceanbase.odc.core.shared.Verify;
import com.oceanbase.odc.core.shared.constant.ErrorCodes;
import com.oceanbase.odc.core.shared.exception.VerifyException;
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

    private static final Pattern ACCOUNT_NAME_PATTERN = Pattern.compile("^[\\w.+@#$%]+$");
    private static final Pattern PASSWORD_PATTERN = Pattern.compile(
            "^(?=(?:[^0-9]*[0-9]){2})(?=(?:[^A-Z]*[A-Z]){2})(?=(?:[^a-z]*[a-z]){2})"
                    + "(?=(?:[^._+@#$%]*[._+@#$%]){2})[\\w._+@#$%]{8,32}$");
    private static final Pattern SPACE_PATTERN = Pattern.compile("^(?=\\S).{1,64}(?<=[^.\\s])$");
    public static final int ACCOUNTNAME_MAX_LENGTH = 64;
    public static final int ACCOUNTNAME_MIN_LENGTH = 4;
    public static final int DESCRIPTION_MAX_LENGTH = 140;
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
            BatchImportUser batchImportUser;
            try {
                batchImportUser = createBatchImportUser(map, accountNames, roleName2RoleMap);
            } catch (VerifyException e) {
                batchImportUser = BatchImportUser.ofFail(e.getLocalizedMessage());
                batchImportUser.setName(map.get(USER_NAME.getLocalizedMessage()));
            }
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

        String name = map.get(USER_NAME.getLocalizedMessage());
        Verify.verify(SPACE_PATTERN.matcher(name).matches(),
                "username length should not exceed 64 and not start or end with spaces");
        batchImportUser.setName(name);

        String accountName = map.get(USER_ACCOUNTNAME.getLocalizedMessage());
        Verify.verify(!accountNames.contains(accountName), "account name already exists");
        checkAccountName(accountName);
        batchImportUser.setAccountName(accountName);

        String password = map.get(USER_PASSWORD.getLocalizedMessage());
        Verify.verify(PASSWORD_PATTERN.matcher(password).matches(),
                "must be 8 to 32 characters long and include at least 2 digits, 2 uppercase letters, "
                        + "2 lowercase letters, and 2 special characters from ._+@#$%");
        batchImportUser.setPassword(password);

        String enabled = map.get(USER_ENABLED.getLocalizedMessage());
        Verify.verify(StringUtils.equalsIgnoreCase(enabled, "true") || StringUtils.equalsIgnoreCase(enabled, "false"),
                "enabled must be true or false");
        batchImportUser.setEnabled(enabled.equalsIgnoreCase("true"));

        String roleNameList = map.get(USER_ROLEIDS.getLocalizedMessage());
        batchImportUser.setRoleIds(new ArrayList<>());
        batchImportUser.setRoleNames(new ArrayList<>());
        if (StringUtils.isNotBlank(roleNameList)) {
            Set<String> roleNames = new HashSet<>(Arrays.asList(roleNameList.split(",")));
            for (String roleName : roleNames) {
                if (roleName2RoleMap.containsKey(roleName)) {
                    Role role = roleName2RoleMap.get(roleName);
                    batchImportUser.getRoleIds().add(role.getId());
                    batchImportUser.getRoleNames().add(role.getName());
                }
            }
            Verify.notEmpty(batchImportUser.getRoleIds(), "Invalid roles");
        }

        String description = map.get(USER_DESCRIPTION.getLocalizedMessage());
        Verify.notGreaterThan(description.length(), DESCRIPTION_MAX_LENGTH, "description");
        batchImportUser.setDescription(description);

        return batchImportUser;
    }

    private void checkAccountName(String accountName) {
        Verify.notNull(accountName, "account name");
        Verify.notLessThan(accountName.length(), ACCOUNTNAME_MIN_LENGTH, "account name's length");
        Verify.notGreaterThan(accountName.length(), ACCOUNTNAME_MAX_LENGTH, "account name's length");
        Verify.verify(ACCOUNT_NAME_PATTERN.matcher(accountName).matches(),
                "account name must consist of letters, numbers, and special characters in ._+@#$% only. ");
    }

}
