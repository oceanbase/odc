# The docker image for build odc aarch rpm package and docker image
# @author: wenniu.ly
# @create: 2022-5-17

# based on centos8_aarch64
FROM centos:centos8

# set labels
LABEL maintainer="ODC <odc@oceanbase.com>"
LABEL description="OceanBase Developer Center (ODC) build image, include node.js and java develop environment"

# parameters
ENV LC_ALL=en_US.UTF-8


# build arguments
ARG USERNAME=admin
ARG WORKDIR=/home/admin
ARG USER_HOME_DIR="/root"
ARG NODE_VERSION="14.19.1"
ARG JDK_VERSION="java-1.8.0-openjdk-headless-1.8.0.312.b07 java-1.8.0-openjdk-1.8.0.312.b07 java-1.8.0-openjdk-devel-1.8.0.312.b07"
ARG MAVEN_VERSION=3.5.4
ARG MAVEN_BASE_URL=https://mirrors.aliyun.com/apache/maven/maven-3/${MAVEN_VERSION}/binaries
ARG BUILD_UTIL_HOME=/home/admin/util

RUN useradd -ms /bin/bash ${USERNAME}
WORKDIR ${WORKDIR}

# copy resources
COPY *.sh ${WORKDIR}/

# create java develop env, include jdk and maven
RUN mkdir -p /etc/yum.repos.d/bak/ \
    && mv /etc/yum.repos.d/*.repo /etc/yum.repos.d/bak/ \
    && curl -o /etc/yum.repos.d/CentOS-Linux-BaseOS.repo https://mirrors.aliyun.com/repo/Centos-vault-8.5.2111.repo \
    && yum clean all && yum makecache \
    && yum install -y langpacks-en glibc-all-langpacks glibc-langpack-en \
    && ln -sf /usr/share/zoneinfo/Asia/Shanghai  /etc/localtime \
    && yum install -y rpm-build git wget \
    && yum install -y --exclude=ali-jdk ${JDK_VERSION}

RUN mkdir -p /usr/share/maven /usr/share/maven/ref \
  && curl -fsSL -o /tmp/apache-maven.tar.gz ${MAVEN_BASE_URL}/apache-maven-${MAVEN_VERSION}-bin.tar.gz \
  && tar -xzf /tmp/apache-maven.tar.gz -C /usr/share/maven --strip-components=1 \
  && rm -f /tmp/apache-maven.tar.gz \
  && ln -s /usr/share/maven/bin/mvn /usr/bin/mvn



# create node develop env & ossutil
RUN sh ${WORKDIR}/create_node_env.sh && ${WORKDIR}/install_ossutil.sh ${BUILD_UTIL_HOME}

ENV JAVA_HOME=/usr/lib/jvm/java-1.8.0-openjdk-1.8.0.312.b07-2.el8_5.aarch64
ENV MAVEN_HOME=/usr/share/maven
ENV MAVEN_CONFIG=$USER_HOME_DIR/.m2
ENV NVS_HOME=/usr/local/nvs
ENV NODE_HOME=/usr/local/nvs/node/${NODE_VERSION}/arm64
ENV PATH=${NODE_HOME}/bin/:${JAVA_HOME}/bin/:$MAVEN_HOME/bin/:${BUILD_UTIL_HOME}/bin/:${NVS_HOME}/:$PATH
