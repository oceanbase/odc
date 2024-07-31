# -*- coding: utf-8 -*-

#  Copyright (c) 2023 OceanBase.
#
#  Licensed under the Apache License, Version 2.0 (the "License");
#  you may not use this file except in compliance with the License.
#  You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  See the License for the specific language governing permissions and
#  limitations under the License.

import copy
import os
import sys
import traceback
import time

import json
import requests
from enum import Enum
from http import HTTPStatus

OUTPUT = {}
RESULT_FILE_KEY = "farm/results/"
TASK_QUEUE_FILE_KEY = "farm/jobs/{}.json"


def _range(start, last):
    def to_str(pos):
        if pos is None:
            return ''
        else:
            return str(pos)

    return to_str(start) + '-' + to_str(last)


def _make_range_string(range):
    if range is None:
        return ''

    start = range[0]
    last = range[1]

    if start is None and last is None:
        return ''

    return 'bytes=' + _range(start, last)


class OssProxy:

    def __init__(self, endpoint=""):
        self.endpoint = endpoint

    def get_object(self, key, _range=None):
        url = "{}/{}".format(self.endpoint, key)
        headers = {}
        if _range is not None:
            _range = (_range, None)
            headers.update({"range": _make_range_string(_range)})
        res = requests.get(url, headers=headers)
        if res.status_code < 400:
            result = res.content.decode()
            return result
        return ""

    def get_object_meta(self, key):
        url = "{}/{}".format(self.endpoint, key)
        headers = {}
        res = requests.head(url, headers=headers)
        return res.headers

    def exists_object(self, key):
        ...


class GithubProxy:

    def __init__(self, host="api.github.com"):
        self.host = host

    def get_job_by_id(self, project, pipeline_id):
        url = "https://{}/repos/{}/actions/runs/{}".format(
            self.host, project, pipeline_id
        )
        try:
            res = requests.get(
                url, headers={
                    "Accept": "application/vnd.github+json"
                }
            )
            status_code = res.status_code
            if status_code == HTTPStatus.NOT_FOUND:
                return {}
            return res.json()
        except:
            traceback.print_exc()
            return {}


class TaskStatusEnum(Enum):
    submitting = 0
    pending = 1
    running = 2
    stopping = 3
    success = 4
    fail = -1
    kill = -2
    timeout = -3
    submit_task_fail = -4


def request(method, url, params=None, payload=None, timeout=10, data=None, without_check_status=False):
    params = params or {}
    try:
        response = requests.request(
            method,
            url,
            params=params,
            json=payload,
            data=data,
            timeout=timeout
        )
        if not without_check_status and response.status_code >= 300:
            try:
                msg = response.json()["msg"]
            except:
                msg = response.text
            print("[ERROR] 错误信息:{}".format(msg))
            exit(1)
        return response
    except Exception:
        import traceback
        traceback.print_exc()
        print("请求失败，出现异常，请联系管理人员处理")
        if not without_check_status:
            exit(1)


def monitor_tasks(oss_proxy: OssProxy, github_pipeline_id, timeout):
    end_time = time.time() + int(timeout)
    end = 0
    end_task = False
    print("{}OUTPUT{}".format("-" * 20, "-" * 20))
    while time.time() <= end_time:
        # 每次刷新20
        if end_task is True:
            pass
        task_data = get_task_res(oss_proxy, github_pipeline_id)
        if task_data:
            end_task = True
        output = get_task_stage_output(oss_proxy, github_pipeline_id, end)
        if output is None:
            continue
        end += len(output)
        need_print_output = output
        if need_print_output and need_print_output.strip():
            print(need_print_output, end="")

        time.sleep(1)
        if task_data is not None:
            task_status = int(task_data["status"])
            if task_status <= TaskStatusEnum.fail.value:
                print(TaskStatusEnum._value2member_map_[task_status])
                OUTPUT.update({"success": -1})
                return False
            elif task_status >= TaskStatusEnum.success.value:
                print(TaskStatusEnum._value2member_map_[task_status])
                OUTPUT.update({"success": 1})
                return True

        time.sleep(5)
    else:
        ...


def get_task_res(oss_proxy: OssProxy, github_pipeline_id):
    try:
        result_key = RESULT_FILE_KEY + "{}.json".format(github_pipeline_id)
        origin_task_data = oss_proxy.get_object(result_key)
        return json.loads(origin_task_data)
    except:
        return


def get_task_stage_output(oss_proxy: OssProxy, github_pipeline_id, start):
    output_key = RESULT_FILE_KEY + "{}.output".format(github_pipeline_id)
    if start:
        output_meta = oss_proxy.get_object_meta(output_key)
        filesize = int(output_meta["Content-Length"])
        if start >= filesize:
            start = filesize - 1
    try:
        return oss_proxy.get_object(output_key, _range=start)
    except:
        return ""


def main(pipeline_id, project, timeout, jobname):
    print("create a new task")
    print("working....")
    global RESULT_FILE_KEY 
    RESULT_FILE_KEY = "{}/results/".format(jobname)
    global TASK_QUEUE_FILE_KEY 
    TASK_QUEUE_FILE_KEY = "{}/jobs/{{}}.json".format(jobname)
    oss_proxy = OssProxy("https://farm-ce.oss-cn-heyuan.aliyuncs.com")
    github_proxy = GithubProxy()
    job_info = github_proxy.get_job_by_id(project, pipeline_id)
    attempt_number = job_info["run_attempt"]
    run_pipeline_id = "{}-{}".format(pipeline_id, attempt_number)
    result = monitor_tasks(oss_proxy, run_pipeline_id, timeout)
    set_output(OUTPUT)
    if not result:
        exit(1)


def set_output(output):
    values = ";".join(["{}={}".format(key, value) for key, value in output.items()])
    os.system(
        'echo "{}" >> $GITHUB_OUTPUT'.format(values)
    )



if __name__ == "__main__":
    print(sys.argv)
    if len(sys.argv) < 5:
        print("缺失相关参数")
        OUTPUT.update({"success": -1})
        sys.exit(1)
    main(sys.argv[1], sys.argv[2], sys.argv[3], sys.argv[4])