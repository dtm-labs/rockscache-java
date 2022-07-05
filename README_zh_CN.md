[English](https://github.com/dtm-labs/rockscache-java) | 简体中文。

# rockscache-java

一个 Redis 缓存库，确保最终的一致性和与DB的强一致性。

本库是go语言实现的[rockscache](https://github.com/dtm-labs/rockscache)的Java移植版，原项目文档中对工作原理有详尽的阐述。因此，本文不重复原理阐述，仅介绍使用方法。

## 如何运行example

1. git clone本项目，使用IDE打开其中example目录所代表的工程，等待gradle下载所有依赖。

2. 使用Docker在本机构建一个redis服务，端口保持默认的6379

   > 如果想基于现有的redis运行此demo，请修改[application.yml](https://github.com/dtm-labs/rockscache-java/blob/main/example/src/main/resources/application.yml)中sprin.redis相关配置。

3. 使用Docker在本机构建一个mysql服务，端口保持默认的3306

   > 如果想基于现有的mysql运行此demo，请修改[application.yml](https://github.com/dtm-labs/rockscache-java/blob/main/example/src/main/resources/application.yml)中sprin.datasource相关配置。
   
4. 在mysql中建立表并初始化数据

   连接上一步建立的mysql，运行[data.sql](https://github.com/dtm-labs/rockscache-java/blob/main/example/src/main/resources/data.sql)中所有代码

5. 启动example项目

   直接用IDE运行[ExampleApplication](https://github.com/dtm-labs/rockscache-java/blob/main/example/src/main/java/io/github/dtmlabs/rcokscache/example/ExampleApplication.java)类即可

6. 访问http://localhost:8080/ui

## 如何构建自己的项目
