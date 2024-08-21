#
# Copyright (c) 2024 OceanBase.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

git add .;
git commit -m "更换文件内容";
git push -u --force origin dev/mayang.test;
# 复制公钥到远程机器先
ssh mayang.ysj@11.124.9.61;
cd /root/mayang-test-odc;
./odc-test-mayang.sh