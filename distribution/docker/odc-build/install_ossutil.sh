#!/bin/bash
# install aliyun ossutil
# refer: https://www.alibabacloud.com/help/zh/object-storage-service/latest/getting-started-ossutil
# attention: x86 & arm ossutil are different binary

build_util_home=${1:-/home/admin/util}

mkdir -p "${build_util_home}"/bin/
if [ $(uname -m) = "aarch64" ]; then
    echo "fetch ossutilarm64"
    curl -o  "${build_util_home}"/bin/ossutil64 "https://gosspublic.alicdn.com/ossutil/1.7.10/ossutilarm64"
else
    echo "fetch ossutil64"
    curl -o  "${build_util_home}"/bin/ossutil64 "https://gosspublic.alicdn.com/ossutil/1.7.10/ossutil64"
fi

chmod 755 "${build_util_home}"/bin/ossutil64
