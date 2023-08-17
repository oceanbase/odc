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
package com.oceanbase.odc.server.web.controller;

import javax.servlet.http.HttpServletRequest;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import com.oceanbase.odc.core.shared.constant.OdcConstants;
import com.oceanbase.odc.service.common.util.WebRequestUtils;

/**
 * as odc use single page front-end architecture, all pages use index.html
 */
@Controller
public class WebIndexController {

    @RequestMapping({"/", "/index.html"})
    public ModelAndView index(HttpServletRequest request) {
        String odcBackUrl = request.getParameter(OdcConstants.ODC_BACK_URL_PARAM);
        boolean redirectUrlValid = WebRequestUtils.isRedirectUrlValid(request, odcBackUrl);
        return new ModelAndView(redirectUrlValid ? "redirect:" + odcBackUrl : "index");
    }

    @RequestMapping("/login")
    public ModelAndView login() {
        return new ModelAndView("index");
    }

}
