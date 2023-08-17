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
package com.oceanbase.odc.service.common.response;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;

import com.oceanbase.odc.core.shared.PreConditions;

/**
 * factory for create response
 *
 * @author yizhou.xw
 * @version : Responses.java, v 0.1 2021-02-19 20:07
 */
public class Responses {

    /**
     * create EmptyResponse, ListResponse is one kind of SuccessResponse with null value inside
     */
    public static EmptyResponse empty() {
        return new EmptyResponse();
    }

    /**
     * create single response, alias of {@link #success(Object)}
     *
     * @param <T> type of value
     */
    public static <T> SuccessResponse<T> single(T value) {
        return success(value);
    }

    /**
     * create okay response, alias of {@link #success(Object)}
     *
     * @param <T> type of value
     */
    public static <T> SuccessResponse<T> ok(T value) {
        return success(value);
    }

    /**
     * Custom pagination return
     *
     * @param iterable
     * @param paginated
     * @param <T>
     * @return PaginatedResponse
     */
    public static <T> PaginatedResponse<T> paginated(Iterable<T> iterable, CustomPage paginated) {
        PaginatedData<T> data = new PaginatedData<>(iterable, paginated);
        return new PaginatedResponse<T>(data);
    }

    /**
     * Custom pagination return using paginated data
     *
     * @param data
     * @param <T>
     * @return PaginatedResponse
     */
    public static <T> PaginatedResponse<T> paginated(PaginatedData<T> data) {
        return new PaginatedResponse<T>(data);
    }

    /**
     * Spring JPA Page Pagination Return
     *
     * @param page Spring Page
     * @param <T>
     * @return PaginatedResponse
     */
    public static <T> PaginatedResponse<T> paginated(Page<T> page) {
        CustomPage customPage = CustomPage.from(page);
        PaginatedData<T> data = new PaginatedData<>(page.getContent(), customPage);
        return new PaginatedResponse<T>(data);
    }

    public static <T> PaginatedResponse<T> paginated(PageAndStats<T> pageAndStats) {
        CustomPage customPage = CustomPage.from(pageAndStats.getPage());
        PaginatedData<T> data = new PaginatedData<>(pageAndStats.getPage().getContent(), customPage);
        data.setStats(pageAndStats.getStats());
        return new PaginatedResponse<T>(data);
    }

    /**
     * create ListResponse, ListResponse is one kind of SuccessResponse with List value inside
     *
     * @param <E> type of List Element
     */
    public static <E> ListResponse<E> list(Iterable<E> value) {
        ListData<E> data = new ListData<>(value);
        return new ListResponse<>(data);
    }

    public static <T> SuccessResponse<T> success(T value) {
        return new SuccessResponse<>(value);
    }

    public static ErrorResponse error(HttpStatus httpStatus, Error error) {
        PreConditions.notNull(httpStatus, "httpStatus");
        PreConditions.notNull(error, "error");
        return new ErrorResponse(httpStatus, error);
    }
}
