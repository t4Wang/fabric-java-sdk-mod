package com.routz.fabricdemo.integration.service;

import com.routz.fabricdemo.integration.domain.FabricOrg;
import com.routz.fabricdemo.integration.domain.FabricStore;
import com.routz.fabricdemo.integration.domain.FabricUser;
import com.routz.fabricdemo.integration.util.ConfigManager;
import com.routz.fabricdemo.integration.util.ConfigUtils;
import com.routz.fabricdemo.integration.util.FabricFactory;
import org.hyperledger.fabric.sdk.User;
import org.hyperledger.fabric.sdk.security.CryptoSuite;
import org.hyperledger.fabric_ca.sdk.HFCAClient;
import org.hyperledger.fabric_ca.sdk.RegistrationRequest;

import java.util.Properties;

public class FabricUserService {

    static private final String TEST_ADMIN_NAME = "admin";
    static private final String TEST_ADMIN_SECRET = "adminpw";
    static private final FabricStore fabricStore = FabricFactory.getFabricStore();
    static private final ConfigManager config = FabricFactory.getConfig();

    static {
        //   configHelper.clearConfig();
        //   assertEquals(256, Config.getConfig().getSecurityLevel());
        ConfigUtils.resetConfig();
        // 如果有环境变量，设置环境变量到配置，没有则忽略
        try {
            ConfigUtils.customizeConfig();
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

    }

    static public FabricUser register(String userName, String affiliation, String orgName) throws Exception {
//        String orgName = fabricOrg.getName();
        FabricOrg fabricOrg = config.getIntegrationTestsFabricOrg(orgName);
        String caName = fabricOrg.getCAName();
        String caLocation = fabricOrg.getCALocation();
        Properties caProperties = fabricOrg.getCAProperties();
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

    static public FabricUser enroll(String userName, String enrollmentSecret, String mspid, String orgName, String caName, String caLocation, Properties caProperties) throws Exception {
//        String caName = org.getCAName();
//        String caLocation = org.getCALocation();
//        Properties caProperties = org.getCAProperties();
//        final String orgName = "peerOrg1";
//        String caName = "ca0";
//        String caLocation = "http://192.168.1.66:7054";
//        Properties caProperties = null;
//        final String mspid = "Org1MSP";
//        String domainName = "org1.example.com";
        FabricUser user = fabricStore.getMember(userName, orgName);
        HFCAClient ca = FabricFactory.getCAClient(caName, caLocation, caProperties); // 成员关系服务提供者

//        String userName = "user2";
//        user.setEnrollmentSecret("rPtWSfEdPaHx");
        user.setEnrollmentSecret(enrollmentSecret);
        // 登记用户
        if (!user.isEnrolled()) {
            user.setEnrollment(ca.enroll(userName, enrollmentSecret));
            user.setMspId(mspid);
        }
        return user;
    }
}
