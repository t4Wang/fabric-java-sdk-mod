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

```xml
<!-- hyperledger fabric -->
<!-- https://mvnrepository.com/artifact/org.hyperledger.fabric-sdk-java/fabric-sdk-java -->
<dependency>
    <groupId>org.hyperledger.fabric-sdk-java</groupId>
    <artifactId>fabric-sdk-java</artifactId>
    <version>1.4.0</version>
</dependency>
```

## 组织节点配置

在`com.routz.fabricdemo.integration.util.ConfigManager`里将组织节点都配置好了，因为我这的业务是基本一上线就不会改动了，所以在这写死，有需要可以另外修改
```java
    defaultProperty(INTEGRATIONTESTS_ORG + "peerOrg1.mspid", "Org1MSP");
    defaultProperty(INTEGRATIONTESTS_ORG + "peerOrg1.domname", "org1.example.com");
    defaultProperty(INTEGRATIONTESTS_ORG + "peerOrg1.ca_location", "http://" + LOCALHOST + ":7054");
    defaultProperty(INTEGRATIONTESTS_ORG + "peerOrg1.caName", "ca0");
    defaultProperty(INTEGRATIONTESTS_ORG + "peerOrg1.peer_locations", "peer0.org1.example.com@grpc://" + LOCALHOST + ":7051, peer1.org1.example.com@grpc://" + LOCALHOST + ":7056");
    defaultProperty(INTEGRATIONTESTS_ORG + "peerOrg1.orderer_locations", "orderer.example.com@grpc://" + LOCALHOST + ":7050");
    defaultProperty(INTEGRATIONTESTS_ORG + "peerOrg2.mspid", "Org2MSP");
    defaultProperty(INTEGRATIONTESTS_ORG + "peerOrg2.domname", "org2.example.com");
    defaultProperty(INTEGRATIONTESTS_ORG + "peerOrg2.ca_location", "http://" + LOCALHOST + ":8054");
    defaultProperty(INTEGRATIONTESTS_ORG + "peerOrg2.peer_locations", "peer0.org2.example.com@grpc://" + LOCALHOST + ":8051,peer1.org2.example.com@grpc://" + LOCALHOST + ":8056");
    defaultProperty(INTEGRATIONTESTS_ORG + "peerOrg2.orderer_locations", "orderer.example.com@grpc://" + LOCALHOST + ":7050");

```
其中，组织是根据前面名字peerOrg1这样正则切分开的，后面分别是mspid，domname，ca_location，caName，peer_locations，orderer_locations
这是因为java sdk跑的节点是两个org，每个org有两个peer，每个org对应一个ca服务器，共用一个orderer，共7个节点，不同公司业务可能不同，这个我还没深入研究如果节点配置不同该在这里怎么修改。
还需要注意的是，我在上面的LOCALHOST变量里面配置的本地虚拟机ip，需要按实际情况修改

## 生成通道
官方自带通道foo和bar的tx文件，如果要自定义创建通道时，要先在服务器上生成*.tx文件，拷贝到项目路径下，将路径作为参数调用
[参考sdk](https://github.com/hyperledger/fabric-sdk-java#channel-creation-artifacts)

生成*.tx文件的命令：
```cmd
cd /项目路径/src/test/fixture/sdkintegration/e2e-2Orgs/v1.3
configtxgen --configPath . -outputCreateChannelTx 通道名.tx -profile TwoOrgsChannel_v13 -channelID 通道名
```

这样就在目录下生成`通道名.tx`文件了

> ## 说明：
> 我是后端java程序员，对hyberledger fabric 和区块链方面认知有限，bugs和可优化的地方一定是有的，欢迎提issue和修改我的代码