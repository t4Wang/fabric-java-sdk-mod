# 官方hyberledger fabric java sdk 修改整合版

> 这个版本根据官方[hyperledger/fabric-sdk-java](https://github.com/hyperledger/fabric-sdk-java)项目的
/src/test/java/sdkintegration/org.hyperledger.fabric.sdkintegration.End2endIT类修改而来。

## 项目源码地址：
[github](https://github.com/t4Wang/fabric-java-sdk-mod)

## 功能划分
用户：
register user
enroll user

通道：
construct channel
install chaincode
invoke chaincode
query chaincode

功能的调用方法都提供在`com.routz.fabricdemo.integration.service`包下面的`FabricChannelService`和`FabricUserService`下面

**test目录下有几个方法的测试用例**

## pom.xml 依赖

``` xml
<!-- hyperledger fabric -->
<!-- https://mvnrepository.com/artifact/org.hyperledger.fabric-sdk-java/fabric-sdk-java -->
<dependency>
    <groupId>org.hyperledger.fabric-sdk-java</groupId>
    <artifactId>fabric-sdk-java</artifactId>
    <version>1.4.0</version>
</dependency>
```

## 生成通道
官方自带通道foo和bar的tx文件，如果要自定义创建通道时，要先在服务器上生成*.tx文件，拷贝到项目路径下，将路径作为参数调用
[参考sdk](https://github.com/hyperledger/fabric-sdk-java#channel-creation-artifacts)

生成*.tx文件的命令：
``` cmd
cd /项目路径/src/test/fixture/sdkintegration/e2e-2Orgs/v1.3

configtxgen --configPath . -outputCreateChannelTx 通道名.tx -profile TwoOrgsChannel_v13 -channelID 通道名
```

这样就在目录下生成`通道名.tx`文件了

> ## 说明：
> 我是后端java程序员，对hyberledger fabric 和区块链方面认知有限，bugs和可优化的地方一定是有的，欢迎提issue和修改我的代码