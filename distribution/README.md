# ODC发布模块

该模块包含所有的odc发布事宜及其相关配置

## 目录结构

```shell
├── docker 				# docker镜像打包目录
│   ├── build.sh 			# odc-server docker 构建脚本
│   ├── odc 				# odc-server Dockerfile 文件所在目录
│   │   └── Dockerfile
│   ├── odc-build 		    # odc-build Dockerfile 文件所在目录
│   │   └── build.sh        # odc-build docker 构建脚本
│   │   └── Dockerfile      # odc-build x86 Dockerfile
│   │   └── aarch
|   |       └── Dockerfile  # odc-build aarch Dockerfile
│   └── resources 			# odc-server Docker 镜像构建所需资源
│       ├── conf 			# supervisor 配置文件
│       │   ├── odc.conf
│       │   └── supervisor.conf
├── plugins                 # 存放插件 jar
├── starters                # 存放 starters jar
└── README.md
```

## ODC发布

ODC项目的所有release版本均通过RPM包以及Docker镜像的形式向外发布。

### RPM构建

RPM 构建使用 maven 插件`rpm-maven-plugin` 实现，该插件使用前需要安装`rpm-build`以及`rpm`依赖，
rpm 配置详见`server/odc-server/pom.xml` 文件中 `rpm-maven-plugin` 插件配置。

maven 构建 rpm 包过程只是针对后端的，完整的构建需要先构建前端资源并拷贝到 odc-server 模块的 resources 目录，
脚本`script/build_rpm.sh` 封装了完整的打包过程，打包后产出物 rpm 包会移动到 `distribution/docker/resources` 目录。

### Docker构建

如果本地有docker环境，可以在本地进行docker镜像的构建。构建脚本为 `distribution/docker/build.sh` 。

### RPM发布
正式的 RPM 包基于 AONE 发布，详见 `rpm` 目录。