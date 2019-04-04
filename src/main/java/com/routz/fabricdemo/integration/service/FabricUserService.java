package com.routz.fabricdemo.integration.service;

import com.routz.fabricdemo.integration.domain.FabricOrg;
import com.routz.fabricdemo.integration.domain.FabricUser;
import com.routz.fabricdemo.integration.domain.FabricStore;
import com.routz.fabricdemo.integration.util.ConfigManager;
import com.routz.fabricdemo.integration.util.ConfigUtils;
import com.routz.fabricdemo.integration.util.Util;
import org.hyperledger.fabric.sdk.exception.InvalidArgumentException;
import org.hyperledger.fabric.sdk.security.CryptoSuite;
import org.hyperledger.fabric_ca.sdk.HFCAClient;
import org.hyperledger.fabric_ca.sdk.RegistrationRequest;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.spec.InvalidKeySpecException;
import java.util.Properties;

import static java.lang.String.format;

public class FabricUserService {

    static final String TEST_ADMIN_NAME = "admin";
    static final String TEST_ADMIN_SECRET = "adminpw";

    static FabricStore fabricStore = FabricStore.setupFabricStore();

    public void checkConfig() throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
        //   configHelper.clearConfig();
        //   assertEquals(256, Config.getConfig().getSecurityLevel());
        ConfigUtils.resetConfig();
        // 如果有环境变量，设置环境变量到配置，没有则忽略
        ConfigUtils.customizeConfig();

    }

    public FabricUser register(String userName, String affiliation, FabricOrg org) throws Exception {
        checkConfig();
        String orgName = org.getName();
        String caName = org.getCAName();
        String caLocation = org.getCALocation();
        Properties caProperties = org.getCAProperties();
//        final String orgName = "peerOrg1";
//        String caName = "ca0";
//        String caLocation = "http://192.168.1.66:7054";
//        Properties caProperties = null;
//        final String mspid = "Org1MSP";
//        String domainName = "org1.example.com";
//        String affiliation = "org1.department1";
//        String userName = "user2";
        String adminName = TEST_ADMIN_NAME;
        String adminSecret = TEST_ADMIN_SECRET;

        HFCAClient ca = HFCAClient.createNewInstance(caName, caLocation, caProperties);
        ca.setCryptoSuite(CryptoSuite.Factory.getCryptoSuite()); // 成员关系服务提供者

        // 预注册的admin （只需要用Fabric caClient登记）
        FabricUser admin = fabricStore.getMember(adminName, orgName);
        if (!admin.isEnrolled()) {  //Preregistered admin only needs to be enrolled with Fabric caClient.
            admin.setEnrollment(ca.enroll(admin.getName(), adminSecret));
        }
        // 注册用户
        FabricUser user = fabricStore.getMember(userName, orgName);
        if (!user.isRegistered()) {  // users need to be registered AND enrolled
            RegistrationRequest rr = new RegistrationRequest(user.getName(), affiliation);
            user.setEnrollmentSecret(ca.register(rr, admin));
        }
        return user;
    }

    public void enroll(FabricUser user, FabricOrg org) throws Exception {
        checkConfig();
        String caName = org.getCAName();
        String caLocation = org.getCALocation();
        Properties caProperties = org.getCAProperties();
//        final String orgName = "peerOrg1";
//        String caName = "ca0";
//        String caLocation = "http://192.168.1.66:7054";
//        Properties caProperties = null;
//        final String mspid = "Org1MSP";
//        String domainName = "org1.example.com";
        HFCAClient ca = HFCAClient.createNewInstance(caName, caLocation, caProperties);
        ca.setCryptoSuite(CryptoSuite.Factory.getCryptoSuite()); // 成员关系服务提供者

//        String userName = "user2";
//        user.setEnrollmentSecret("rPtWSfEdPaHx");
        // 登记用户
        if (!user.isEnrolled()) {
            user.setEnrollment(ca.enroll(user.getName(), user.getEnrollmentSecret()));
//            user.setMspId(mspid);
        }
    }

    static public FabricUser setupPeerAdmin(FabricOrg sampleOrg) throws IOException, NoSuchAlgorithmException, NoSuchProviderException, InvalidKeySpecException {
        // peerAdmin
        ConfigManager config = ConfigManager.getConfig();
        String domainName = sampleOrg.getDomainName();
        String orgName = sampleOrg.getName();
        FabricUser peerOrgAdmin = fabricStore.getMember( orgName+ "Admin", orgName, sampleOrg.getMSPID(),
                Util.findFileSk(Paths.get(config.getTestChannelPath(), "crypto-config/peerOrganizations/",
                        domainName, format("/users/Admin@%s/msp/keystore", domainName)).toFile()),
                Paths.get(config.getTestChannelPath(), "crypto-config/peerOrganizations/", domainName,
                        format("/users/Admin@%s/msp/signcerts/Admin@%s-cert.pem", domainName, domainName)).toFile());
        return peerOrgAdmin;
    }
}
