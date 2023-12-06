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
package com.oceanbase.odc.server.web.controller.v2;

import static com.oceanbase.odc.service.captcha.CaptchaConstants.SESSION_KEY_VERIFICATION_CODE;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.core.shared.constant.ErrorCodes;
import com.oceanbase.odc.core.shared.exception.BadRequestException;
import com.oceanbase.odc.service.captcha.CaptchaService;
import com.oceanbase.odc.service.captcha.VerificationCode;
import com.oceanbase.odc.service.common.model.SetEnabledReq;
import com.oceanbase.odc.service.common.response.ListResponse;
import com.oceanbase.odc.service.common.response.PaginatedResponse;
import com.oceanbase.odc.service.common.response.Responses;
import com.oceanbase.odc.service.common.response.SuccessResponse;
import com.oceanbase.odc.service.common.util.WebResponseUtils;
import com.oceanbase.odc.service.iam.OrganizationService;
import com.oceanbase.odc.service.iam.ResourceRoleService;
import com.oceanbase.odc.service.iam.RoleService;
import com.oceanbase.odc.service.iam.UserBatchImportPreviewer;
import com.oceanbase.odc.service.iam.UserPermissionService;
import com.oceanbase.odc.service.iam.UserService;
import com.oceanbase.odc.service.iam.model.ChangePasswordReq;
import com.oceanbase.odc.service.iam.model.CreateRoleReq;
import com.oceanbase.odc.service.iam.model.CreateUserReq;
import com.oceanbase.odc.service.iam.model.Organization;
import com.oceanbase.odc.service.iam.model.QueryUserParams;
import com.oceanbase.odc.service.iam.model.ResourceRole;
import com.oceanbase.odc.service.iam.model.Role;
import com.oceanbase.odc.service.iam.model.UpdateRoleReq;
import com.oceanbase.odc.service.iam.model.UpdateUserReq;
import com.oceanbase.odc.service.iam.model.User;
import com.oceanbase.odc.service.iam.model.UserPreviewBatchImportResp;

import io.swagger.annotations.ApiOperation;

/**
 * @author wenniu.ly
 * @date 2021/6/25
 */

@RestController
@RequestMapping("/api/v2/iam")
public class IamController {

    @Autowired
    private RoleService roleService;

    @Autowired
    private UserService userService;

    @Autowired
    private UserPermissionService userPermissionService;

    @Autowired
    private CaptchaService captchaService;

    @Autowired
    private UserBatchImportPreviewer userBatchImportPreviewer;

    @Autowired
    private ResourceRoleService resourceRoleService;

    @Autowired
    private OrganizationService organizationService;

    /**
     * Get verification code
     *
     * @return verification code which could be an image or other format, default image
     */
    @RequestMapping(value = "/captcha", method = RequestMethod.GET)
    public ResponseEntity<InputStreamResource> getCaptchaImage(HttpServletRequest request) {
        VerificationCode verificationCode = captchaService.createVerificationCode();
        request.getSession().setAttribute(SESSION_KEY_VERIFICATION_CODE, verificationCode);
        return WebResponseUtils.getStreamResponseEntity(new InputStreamResource(verificationCode.getCode()));
    }

    /**
     * Create a user
     *
     * @param createUserReq
     * @apiNote necessary field [name accountName password]
     * @return Created user dto
     */
    @ApiOperation(value = "createUser", notes = "创建用户")
    @RequestMapping(value = "/users", method = RequestMethod.POST)
    public SuccessResponse<User> createUser(@RequestBody CreateUserReq createUserReq) {
        return Responses.success(userService.create(createUserReq));
    }

    /**
     * Batch create users
     *
     * @param createUserReqs
     * @apiNote necessary field [name accountName password]
     * @return Created users dto
     */
    @ApiOperation(value = "batchCreateUser", notes = "批量创建用户")
    @RequestMapping(value = "/users/batchCreate", method = RequestMethod.POST)
    public ListResponse<User> batchCreateUser(@RequestBody List<CreateUserReq> createUserReqs) {
        return Responses.list(userService.batchCreate(createUserReqs));
    }

    /**
     * Get detail information of a user by id
     *
     * @param id
     * @apiNote necessary field [id]
     * @return Detail user information
     */
    @ApiOperation(value = "detailUser", notes = "查询用户详细信息，id为用户唯一id")
    @RequestMapping(value = "/users/{id:[\\d]+}", method = RequestMethod.GET)
    public SuccessResponse<User> detailUser(@PathVariable long id) {
        return Responses.success(userService.detail(id));
    }

    /**
     * Get detail information of current user
     *
     * @return Detail information for current user
     */
    @ApiOperation(value = "detailCurrentUser", notes = "查询当前登录用户详细信息")
    @RequestMapping(value = "/users/me", method = RequestMethod.GET)
    public SuccessResponse<User> detailCurrentUser() {
        return Responses.success(userService.detailCurrentUser());
    }

    @ApiOperation(value = "listOrganizations", notes = "lise all organizations that current user belongs to")
    @RequestMapping(value = "/users/me/organizations", method = RequestMethod.GET)
    public ListResponse<Organization> listAllOrganizations() {
        return Responses.list(organizationService.listCurrentUserOrganizations());
    }


    /**
     * Get users list
     *
     * @apiNote unnecessary field [authorizedResource, includePermissions, roleId, filter, pageParam]
     * @implNote Explanation: authorizedResource[格式 resourceType:id 如 resourceGroup:1 或
     *           publicConnection:3]表示查询某一资源关联的用户列表; includePermissions表示结果是否包含对这个资源的权限信息;
     *           filter表示对结果的精确匹配或模糊匹配过滤; fieldToValuesMap表示匹配字段与值的对应关系，如对location字段，需要模糊匹配 ab或
     *           gym字段，则map的key为location，value为[ab, gym]组成的列表; operation表示多个匹配之间的计算关系，分为可选值AND OR，表示与还是或
     *           type表示匹配是精确匹配还是模糊匹配，可选值EXACT，FUZZY; pageParam为分页信息, page表示当前页码，从1开始，默认为1,
     *           size表示每页大小，默认10 desc表示排序，true表示降序，默认true
     * @return Paginated users information
     */
    @ApiOperation(value = "listUsers",
            notes = "查询用户列表；authorizedResource[格式 resourceType:id 如 resourceGroup:1 或 publicConnection:3]表示查询某一资源关联的用户列表，includePermissions表示结果是否包含对这个资源的权限信息；roleId表示查询某一个角色关联的用户列表;filter表示对结果的过滤，可分为精确匹配和模糊匹配")
    @RequestMapping(value = "/users", method = RequestMethod.GET)
    public PaginatedResponse<User> listUsers(
            @RequestParam(required = false) String authorizedResource,
            @RequestParam(required = false, defaultValue = "read", name = "minPrivilege") String minPrivilege,
            @RequestParam(required = false) Boolean includePermission,
            @RequestParam(required = false) List<Long> roleId,
            @RequestParam(required = false) Boolean enabled,
            @RequestParam(required = false) List<String> name,
            @RequestParam(required = false) List<String> accountName,
            @RequestParam(required = false) Boolean basic,
            @PageableDefault(size = Integer.MAX_VALUE, sort = {"id"}, direction = Direction.DESC) Pageable pageable) {
        QueryUserParams queryUserParams = QueryUserParams.builder()
                .basic(basic)
                .enabled(enabled)
                .roleIds(roleId)
                .names(name)
                .accountNames(accountName)
                .authorizedResource(authorizedResource)
                .includePermissions(includePermission)
                .minPrivilege(minPrivilege)
                .build();
        return Responses.paginated(userService.list(queryUserParams, pageable));
    }

    /**
     * Delete user record
     *
     * @param id
     * @apiNote necessary field [id]
     * @return Deleted user information
     */
    @ApiOperation(value = "deleteUser", notes = "删除用户信息，id为唯一用户id")
    @RequestMapping(value = "/users/{id:[\\d]+}", method = RequestMethod.DELETE)
    public SuccessResponse<User> deleteUser(@PathVariable long id) {
        return Responses.success(userService.delete(id));
    }

    /**
     * Update user information
     *
     * @param id
     * @apiNote necessary field [id]
     * @return Updated user information
     */
    @ApiOperation(value = "updateUser", notes = "更新用户信息，id为唯一用户id")
    @RequestMapping(value = "/users/{id:[\\d]+}", method = RequestMethod.PUT)
    public SuccessResponse<User> updateUser(@PathVariable long id,
            @RequestBody UpdateUserReq updateUserReq) {
        return Responses.success(userService.update(id, updateUserReq));
    }

    /**
     * Enable / disable a user
     *
     * @param id
     * @param req
     * @apiNote necessary field [id]
     * @return Updated user information
     */
    @ApiOperation(value = "setUserEnabled", notes = "启用/禁用用户")
    @RequestMapping(value = "/users/{id:[\\d]+}/setEnabled", method = RequestMethod.POST)
    public SuccessResponse<User> setUserEnabled(@PathVariable Long id,
            @RequestBody SetEnabledReq req) {
        return Responses.success(userService.setEnabled(id, req.getEnabled()));
    }

    /**
     * Change user password for himself
     *
     * @param changePasswordReq
     * @apiNote necessary field [changePasswordRequest], Explanation: 需要用户提供原始密码和新密码用以修改用户本身的账户密码
     * @return Information for current user
     */
    @ApiOperation(value = "changePassword", notes = "修改当前用户登录密码，需要当前密码和新密码")
    @RequestMapping(value = "/users/me/changePassword", method = RequestMethod.POST)
    public SuccessResponse<User> changePassword(@RequestBody ChangePasswordReq changePasswordReq) {
        return Responses.success(userService.changePassword(changePasswordReq));
    }

    @ApiOperation(value = "activateUser", notes = "修改当前用户登录密码，需要当前密码和新密码")
    @RequestMapping(value = "/users/{username}/activate", method = RequestMethod.POST)
    public SuccessResponse<User> activateUser(@PathVariable String username,
            @RequestBody ChangePasswordReq changePasswordReq) {
        changePasswordReq.setUsername(username);
        return Responses.success(userService.changePassword(changePasswordReq));
    }

    /**
     * Reset user password
     *
     * @param id
     * @param changePasswordReq
     * @apiNote necessary field [id, password], Explanation: 管理员即admin，重置某一用户密码
     * @return Information for user
     */
    @ApiOperation(value = "resetPassword", notes = "管理员重置某一用户密码，id为用户唯一id，password为重置后的密码")
    @RequestMapping(value = "/users/resetPassword", method = RequestMethod.POST)
    public SuccessResponse<User> resetPassword(@RequestParam Long id,
            @RequestBody ChangePasswordReq changePasswordReq) {
        return Responses.success(userService.resetPassword(id, changePasswordReq.getNewPassword()));
    }

    /**
     * Create a role
     *
     * @param createRoleReq
     * @apiNote necessary field [createRoleRequest]
     * @return Created role information
     */
    @ApiOperation(value = "createRole", notes = "创建角色")
    @RequestMapping(value = "/roles", method = RequestMethod.POST)
    public SuccessResponse<Role> createRole(@RequestBody CreateRoleReq createRoleReq) {
        return Responses.success(roleService.create(createRoleReq));
    }

    @ApiOperation(value = "exists", notes = "Returns whether a role exists")
    @RequestMapping(value = "/roles/exists", method = RequestMethod.GET)
    public SuccessResponse<Boolean> exists(@RequestParam String name) {
        // TODO: method should be POST
        return Responses.success(roleService.exists(name));
    }

    /**
     * Get detail information of a role
     *
     * @param id
     * @apiNote necessary field [id]
     * @return Detail role information
     */
    @ApiOperation(value = "detailRole", notes = "获取角色详细信息，id为角色唯一id")
    @RequestMapping(value = "/roles/{id:[\\d]+}", method = RequestMethod.GET)
    public SuccessResponse<Role> detailRole(@PathVariable long id) {
        return Responses.success(roleService.detail(id));
    }

    /**
     * Get role list
     * 
     * @param pageable paging
     * @return Get roles information
     */
    @ApiOperation(value = "listRoles", notes = "获取角色列表信息")
    @RequestMapping(value = "/roles", method = RequestMethod.GET)
    public PaginatedResponse<Role> listRoles(
            @PageableDefault(size = Integer.MAX_VALUE, sort = {"id"}, direction = Direction.DESC) Pageable pageable) {
        return Responses.paginated(roleService.list(pageable));
    }

    @ApiOperation(value = "listResourceRoles", notes = "list resource roles")
    @RequestMapping(value = "/resourceRoles", method = RequestMethod.GET)
    public ListResponse<ResourceRole> listResourceRoles() {
        return Responses.list(resourceRoleService.listResourceRoles());
    }

    /**
     * Delete specific role record
     *
     * @param id
     * @apiNote necessary field [id]
     * @return Deleted role information
     */
    @ApiOperation(value = "deleteRole", notes = "删除角色信息，id为角色唯一id")
    @RequestMapping(value = "/roles/{id:[\\d]+}", method = RequestMethod.DELETE)
    public SuccessResponse<Role> deleteRole(@PathVariable long id) {
        return Responses.success(roleService.delete(id));
    }

    /**
     * Update role information
     *
     * @param id
     * @param updateRoleRequest
     * @apiNote necessary field [id, updateRoleRequest]
     * @return Updateed role information
     */
    @ApiOperation(value = "updateRole", notes = "更新角色信息，id为角色唯一id")
    @RequestMapping(value = "/roles/{id:[\\d]+}", method = RequestMethod.PUT)
    public SuccessResponse<Role> updateRole(@PathVariable long id,
            @RequestBody UpdateRoleReq updateRoleRequest) {
        return Responses.success(roleService.update(id, updateRoleRequest));
    }

    /**
     * Enable / disable a role
     *
     * @param id
     * @param req
     * @apiNote necessary field [id]
     * @return Updated user information
     */
    @ApiOperation(value = "setRoleEnabled", notes = "启用/禁用角色")
    @RequestMapping(value = "/roles/{id:[\\d]+}/setEnabled", method = RequestMethod.POST)
    public SuccessResponse<Role> setRoleEnabled(@PathVariable Long id,
            @RequestBody SetEnabledReq req) {
        return Responses.success(roleService.setEnabled(id, req.getEnabled()));
    }

    private <T> T parseJson(String json, String parameterName, Class<T> classType) {
        if (Objects.isNull(json)) {
            return null;
        }
        T t = JsonUtils.fromJson(json, classType);
        if (Objects.nonNull(t)) {
            return t;
        }
        throw new BadRequestException(ErrorCodes.IllegalArgument,
                new Object[] {parameterName, "Invalid json format, given " + json},
                "Invalid json format, parameterName=" + parameterName);
    }

    /**
     * 解析批量导入的文件（用户）
     *
     * @param file
     * @return meta info of this file
     */
    @ApiOperation(value = "previewBatchImportUsers", notes = "Parse imported file")
    @RequestMapping(value = "/users/previewBatchImport", method = RequestMethod.POST)
    public SuccessResponse<UserPreviewBatchImportResp> previewBatchImportUser(@RequestParam MultipartFile file)
            throws IOException {
        return Responses.single(userBatchImportPreviewer.preview(file));
    }

    /**
     * 批量导入用户
     *
     * @param createUserReqs
     * @return Batch imported connections
     */
    @ApiOperation(value = "batchImportUsers", notes = "Batch create users")
    @RequestMapping(value = "/users/batchImport", method = RequestMethod.POST)
    public ListResponse<User> batchCreateUsers(@RequestBody List<CreateUserReq> createUserReqs) {
        return Responses.list(userService.batchImport(createUserReqs));
    }
}
