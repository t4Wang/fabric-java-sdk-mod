# 官方hyberledger fabric java sdk 修改整合版

> 这个版本根据官方[hyperledger/fabric-sdk-java](https://github.com/hyperledger/fabric-sdk-java)项目修改而来。

## 项目源码地址：
[github](https://github.com/t4Wang/fabric-java-sdk-mod)

## 准备工作
本文只关注java sdk，fabric服务器部分只大概讲一下。

在java sdk跑起来之前，首先要把fabric服务器跑起来。
跑起来之后，需要把orderer的证书复制到java服务器这边用来调用使用。
除了invoke不用一定是管理员权限外，其他对链码的操作都需要是管理员权限。
拿过来证书后，首先需要注册一下，或者已经通过注册了的话可以把密码拷贝过来供enroll使用
（我自己搭建测试节点环境是参考了这篇博客，很详细 [超级账本HyperLedger：Fabric的全手动、多服务器部署教程](https://www.lijiaocn.com/%E9%A1%B9%E7%9B%AE/2018/04/26/hyperledger-fabric-deploy.html)

enroll user这个步骤是调用其他接口时用来验证身份的，既可以通过ca服务器把账号密码传过去直接enroll，也可以读取已经enroll的本地用户证书使用

```java
    // 从本地直接读取用户证书使用
    private SampleUser setupPeerAdmin(SampleOrg sampleOrg) throws Exception {
        // peerAdmin
        String domainName = sampleOrg.getDomainName();
        String orgName = sampleOrg.getName();
        File adminKeystore = Paths.get(FAB_CONFIG.getChannelPath(), "crypto-config/peerOrganizations/",
                domainName, format("/users/Admin@%s/msp/keystore", domainName)).toFile();
        File adminCert = Paths.get(FAB_CONFIG.getChannelPath(), "crypto-config/peerOrganizations/", domainName,
                format("/users/Admin@%s/msp/signcerts/Admin@%s-cert.pem", domainName, domainName)).toFile();
        SampleUser peerOrgAdmin = sampleStore.getMember(orgName + "Admin", orgName, sampleOrg.getMSPID(),
                Util.findFileSk(adminKeystore),
                adminCert);
        return peerOrgAdmin;
    }
```

## 功能划分
sdk的功能在`com.routz.fabric_java_sdk_integration.FabTest`类里都有测试用例，所有功能提供如下：
用户：
register user
enroll user

通道：
construct channel

链码：
install chaincode
check install status
instantied chaincode proposal
query instantied chaincode proposal status
install upgrade proposal
upgrade chaincode
invoke chaincode
query chaincode

## 配置
### 用户密码，通道名称， 链码路径等配置
`com.routz.fabric_java_sdk_integration.util.FabricUtils`里面配置的链码路径和版本
```java
    private static final String FUNCTION_NAME = "checkin";
    private static final String CHAIN_CODE_NAME = "checkin_go";
    private static final String CHAIN_CODE_PATH = "github.com/chickin_1_13";
    private static final String CHAIN_CODE_VERSION = "1.21";
    private static final String CHAIN_CODE_FILEPATH = "sdkintegration/gocc/chicken";
    private static final TransactionRequest.Type CHAIN_CODE_LANG = TransactionRequest.Type.GO_LANG;
```
`com.routz.fabric_java_sdk_integration.util.FabConfig`里面配置用户名密码，通道名称，组织名称，是否使用tls等属性
```java
    private static final String ORG_NAME = "peerOrg1";
    private static final String CHANNEL_NAME = "foo";
    private static final String testUser1 = "user1";
    private static final String adminPass = "adminpw";
    private static final String sercet1 = "MzoRLMuNWNnY";

    private static final boolean runningFabricCATLS = false;
    private static final boolean runningFabricTLS = false;
```
**由于配置文件比较多，我用绝对路径把证书等各种文件放在了`/var/fabj/src/test/fixture`路径下**
**上面说的一些配置只是为了简化写到了代码里，可以根据实际情况修改**

### 组织节点配置

在`src\main\resources\fabric_config.yaml`里配置有组织节点的信息配置，自己的节点按如下格式配置，启动时就会把信息读取进来
```yaml
    orgs:
      peerOrg1:
        name: peerOrg1
        mspid: Org1MSP
        domainName: org1.example.com
        peerLocations:
          peer0.org1.example.com: grpc://192.168.1.40:7051
          peer1.org1.example.com: grpc://192.168.1.41:7056
        ordererLocations:
          orderer.example.com: grpc://192.168.1.40:7050
        caName: ca0
        caLocation: http://192.168.1.43:7054
      peerOrg2:
        name: peerOrg2
        mspid: Org2MSP
        domainName: org2.example.com
        peerLocations:
          peer0.org2.example.com: grpc://192.168.1.40:8051
        ordererLocations:
          orderer.example.com: grpc://192.168.1.40:7050
        caLocation: http://192.168.1.43:8054
```

## 生成通道
官方自带通道foo和bar的tx文件，如果要自定义创建通道时，要先在服务器上生成*.tx文件，拷贝到项目路径下，将路径作为参数调用
[参考sdk](https://github.com/hyperledger/fabric-sdk-java#channel-creation-artifacts)

生成*.tx文件的命令：
```cmd
cd /项目路径/src/test/fixture/sdkintegration/e2e-2Orgs/v1.3
configtxgen --configPath . -outputCreateChannelTx 通道名.tx -profile TwoOrgsChannel_v13 -channelID 通道名
```

这样就在目录下生成`通道名.tx`文件了

## 示例链码
在下有个模拟物联网设备打卡签到的简单链码，我为什么这样写呢？可以参考我写的一篇博客[hyperledger fabric java sdk 多线程并发调用时遇到 TransactionEventException的解决办法](https://blog.csdn.net/tom9238/article/details/95448664)

> 我对 fabric 了解还不够，深感自己学识浅薄，这个sdk还有很大优化余地，有任何问题都可以提出来，我会尽力与你探讨和解决，区块链行业还算起步阶段，各种资料，社区都还不足，希望能与你共同呵护行业的发展~