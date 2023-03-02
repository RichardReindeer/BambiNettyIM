# BambiIM

​		这是一个个人编写的框架，框架设计图如下:

## 项目架构:

![bambiIM.drawio](https://github.com/RichardReindeer/BambiNettyIM/blob/main/docs/image/BambiNettyIM.png)

### 依赖版本:

- `SpringBoot:3.0.2`
- `JDK17`
- `netty: 4.1.89.Final`
- `guava: 31.1-jre`

​		需要注意的是,本架构代码的编写初衷是对Netty的系统化学习，且本人电脑性能存在一定瓶颈，所以架构图中非着色部分并没有对应实现，且本项目暂时舍弃了对sql等持久化操作的支持；持久化逻辑请各位自行根据源码编写。

​		源码中架构较为清晰，可以使用mybatis等中间件进行CRUD操作；代码外的中间件集群的接入可能会在后续进行更新。

## 架构设计文档

> ​		内部相关帮助文档请跳转至连接[设计文档](https://github.com/RichardReindeer/BambiNettyIM/tree/main/docs) 中查看

## 项目的配置与启动

- Protobuf由protobuf.exe自动生成，请在common中的proto包中进行本地地址更改，避免生成message时出错

- 记得修改每个模块的zookeeper 以及redis地址

- 请酌情修改服务器以及网关端口，避免产生冲突

- 确保`EzGate`模块正常运行，否则客户端无法登录服务器

  

# FIXED LOG

### 2023/03/02 

- ​		解决SLF4J依赖的版本冲突问题                                  

- ​		解决创建zk父节点时的json转换问题 

- ​       移除`Swagger`因为命名空间与`SpringBoot3.x`命名空间不匹配

  ​			参考连接[Swagger与SpringBoot3.0不兼容问题](https://stackoverflow.com/questions/71549614/springfox-type-javax-servlet-http-httpservletrequest-not-present)



## TODO

- 客户端代码重构，解决循环依赖问题
- 注册中心的改变
  - 可能会直接使用`nacos`
- 加入`feign`以及`hystrix`进行服务的熔断操作
- 加入`sentinel`
- 加入`nginx`层操作代码
