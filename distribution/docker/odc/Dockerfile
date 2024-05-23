# The Dockerfile for build odc-server docker image
# 2021-5-6: reduce docker size
# 2021-3-10: openjdk choiced due ajsk's crash issue in KylinOS
#            the jdk version used in docker is java-1.8.0-openjdk-1.8.0.262
# 2021-7-15：sync centos7 base image to aliyun docker reg for build stablility
# 2022-05-10：base image change to anolisos8 for avoid system vulnerabilities
# 2023-11-09: one Dockerfile for different cpu architectures, amd64 and arm64
ARG ARCH=amd64
FROM docker.io/openanolis/anolisos:8.6-x86_64 AS amd64-base
FROM docker.io/openanolis/anolisos:8.6-aarch64 AS arm64-base
FROM ${ARCH}-base AS base

# set labels
LABEL maintainer="ODC <odc@oceanbase.com>"
LABEL description="OceanBase Developer Center (ODC)"

# parameters
ARG ODC_INSTALL_DIR=/opt/odc
ARG JDK_VERSION="java-1.8.0-openjdk-headless java-1.8.0-openjdk java-1.8.0-openjdk-devel"
ARG PYTHON_VERSION=python2
ARG PYTHON_PIP_VERSION=python2-pip-9.0.3
ARG PYTHON_PIP_UPGRADE_VERSION=20.0.2

ENV LC_ALL=en_US.UTF-8
ENV LANG=en_US.utf-8

# install jdk/supervisor, tcpdump, supervisor (install by python pip)
RUN yum clean all && yum makecache \
    && yum install -y --exclude=ali-jdk ${JDK_VERSION} ${PYTHON_VERSION} ${PYTHON_PIP_VERSION} tcpdump passwd langpacks-en glibc-all-langpacks glibc-locale-source glibc-langpack-en \
    && ln -sf /usr/share/zoneinfo/Asia/Shanghai  /etc/localtime \
    && localedef -i en_US -f UTF-8 en_US.UTF-8 \
    && yum update -y --exclude=ali-jdk \
    && ln -s /usr/bin/python2 /usr/bin/python \
    && ln -s /usr/bin/pip2 /usr/bin/pip \
    && ln -s /usr/lib64/libtinfo.so.6.1 /usr/lib64/libtinfo.so.5 \
    && pip install --upgrade pip==${PYTHON_PIP_UPGRADE_VERSION} -i http://mirrors.aliyun.com/pypi/simple/ --trusted-host mirrors.aliyun.com \
    && pip install supervisor -i http://mirrors.aliyun.com/pypi/simple/ --trusted-host mirrors.aliyun.com --ignore-installed \
    && yum clean all \
    && rm -rf /var/cache/yum

RUN mkdir -p ${ODC_INSTALL_DIR}/tmp
WORKDIR ${ODC_INSTALL_DIR}

# set JAVA_HOME for use jdk instead of jre, cowork with start-odc.sh
ENV ODC_JVM_TRY_USE_JDK="1"

RUN echo '----------- DATE_CHANGE: Add this line to disable cache during docker building -------------------'

# copy files
COPY ./resources/*.rpm \
     ./resources/conf/*.conf \
     ${ODC_INSTALL_DIR}/tmp/

# supervisor and timezone configuration
# 1. supervisor 的配置更新就是拷贝文件，然后通过 docker entrypoint 指定启动程序为 supervisord
# 2. timezone 的配置说明
#   - 基础镜像 acs-reg.alipay.com/antstack/base:alios7u2_base 的缺省时区为 UTC
#   - 设置为 UTC 时，oceanbase client 连接 oracle 租户会报 OBE-01882: timezone region 'UTC' not found
#   - 为绕过此问题，设置时区为 Asia/Shanghai
# 3. 禁止 ssh 帐密登录，禁用 root 账号密码
RUN mkdir -p /etc/supervisor/conf.d \
    && rm --recursive --force --verbose /etc/supervisor/conf.d/*.conf \
    && mv ${ODC_INSTALL_DIR}/tmp/odc.conf /etc/supervisor/conf.d/base.conf \
    && mv ${ODC_INSTALL_DIR}/tmp/supervisor.conf /etc/supervisor/supervisord.conf \
    && passwd --delete root && passwd --lock root \
    && mkdir -p /etc/ssh && echo "PasswordAuthentication  no" > /etc/ssh/sshd_config


# install rpm packages and clean tmp files
RUN rpm -ivh ${ODC_INSTALL_DIR}/tmp/*.rpm \
  && rm --recursive --force --verbose ${ODC_INSTALL_DIR}/tmp

EXPOSE 8989

ENTRYPOINT ["/usr/bin/supervisord", "-c", "/etc/supervisor/supervisord.conf"]
