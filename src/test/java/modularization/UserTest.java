package modularization;

import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.hyperledger.fabric.sdk.TestConfigHelper;
import org.hyperledger.fabric.sdk.security.CryptoSuite;
import org.hyperledger.fabric.sdkintegration.SampleOrg;
import org.hyperledger.fabric.sdkintegration.SampleStore;
import org.hyperledger.fabric.sdkintegration.SampleUser;
import org.hyperledger.fabric_ca.sdk.HFCAClient;
import org.hyperledger.fabric_ca.sdk.RegistrationRequest;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.security.PrivateKey;
import java.util.Properties;
import static modularization.Print.out;
import static org.hyperledger.fabric.sdk.testutils.TestUtils.resetConfig;

public class UserTest {

    static final String TEST_ADMIN_NAME = "admin";

    private final TestConfigHelper configHelper = new TestConfigHelper();

    SampleStore sampleStore = null;

    File sampleStoreFile = new File(System.getProperty("java.io.tmpdir") + "/HFCSampletest.properties");

    @Before
    public void checkConfig() throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException, MalformedURLException, org.hyperledger.fabric_ca.sdk.exception.InvalidArgumentException {
        out("\n\n\nRUNNING: UserTest.\n");
        //   configHelper.clearConfig();
        //   assertEquals(256, Config.getConfig().getSecurityLevel());
        resetConfig();
        // 如果有环境变量，设置环境变量到配置，没有则忽略
        configHelper.customizeConfig();

        if (sampleStoreFile.exists()) { //For testing start fresh
            sampleStoreFile.delete();
        }
        sampleStore = new SampleStore(sampleStoreFile);
    }

    @Test
    public void register() throws Exception {
        final String orgName = "peerOrg1";
        String caName = "ca0";
        String caLocation = "http://192.168.1.66:7054";
        Properties caProperties = null;
        final String mspid = "Org1MSP";
        String domainName = "org1.example.com";
        HFCAClient ca = HFCAClient.createNewInstance(caName, caLocation, caProperties);
        ca.setCryptoSuite(CryptoSuite.Factory.getCryptoSuite()); // 成员关系服务提供者

        SampleOrg sampleOrg = new SampleOrg(orgName, mspid);

        String affiliation = "org1.department1";
        String userName = "user2";

        // 预注册的admin （只需要用Fabric caClient登记）
        SampleUser admin = sampleStore.getMember(TEST_ADMIN_NAME, orgName);
        if (!admin.isEnrolled()) {  //Preregistered admin only needs to be enrolled with Fabric caClient.
            admin.setEnrollment(ca.enroll(admin.getName(), "adminpw"));
        }
        // 注册用户
        SampleUser user = sampleStore.getMember(userName, sampleOrg.getName());
        if (!user.isRegistered()) {  // users need to be registered AND enrolled
            RegistrationRequest rr = new RegistrationRequest(user.getName(), affiliation);
            user.setEnrollmentSecret(ca.register(rr, admin));
        }
        // 登记用户
        if (!user.isEnrolled()) {
            user.setEnrollment(ca.enroll(user.getName(), user.getEnrollmentSecret()));
            user.setMspId(mspid);
        }
    }

    @Test
    public void enroll() throws Exception {
        final String orgName = "peerOrg1";
        String caName = "ca0";
        String caLocation = "http://192.168.1.66:7054";
        Properties caProperties = null;
        final String mspid = "Org1MSP";
        String domainName = "org1.example.com";
        HFCAClient ca = HFCAClient.createNewInstance(caName, caLocation, caProperties);
        ca.setCryptoSuite(CryptoSuite.Factory.getCryptoSuite()); // 成员关系服务提供者

        SampleOrg sampleOrg = new SampleOrg(orgName, mspid);
        String userName = "user2";
        SampleUser user = sampleStore.getMember(userName, sampleOrg.getName());
        user.setEnrollmentSecret("rPtWSfEdPaHx");
        // 登记用户
        if (!user.isEnrolled()) {
            user.setEnrollment(ca.enroll(user.getName(), user.getEnrollmentSecret()));
            user.setMspId(mspid);
        }
    }

    /**
     * Will register and enroll users persisting them to samplestore.
     * 注册和登记用户将他们存入samplestore
     * @throws Exception
     */
//    public void enrollUsersSetup(SampleStore sampleStore) throws Exception {
//        ////////////////////////////
//        //Set up USERS
//
//        //SampleUser can be any implementation that implements org.hyperledger.fabric.sdk.User Interface
//        // 不止是SampleUser， 这里可以是任何实现了User接口的类
//
//        ////////////////////////////
//        // get users for all orgs
//        // 获取所有组织的用户
//
//        out("***** Enrolling Users *****");
//        for (SampleOrg sampleOrg : testSampleOrgs) {
//
//            HFCAClient ca = sampleOrg.getCAClient();
//
//            final String orgName = sampleOrg.getName();
//            // 成员关系服务提供者
//            final String mspid = sampleOrg.getMSPID();
//            ca.setCryptoSuite(CryptoSuite.Factory.getCryptoSuite());
//
//            if (testConfig.isRunningFabricTLS()) {
//                //This shows how to get a client TLS certificate from Fabric CA
//                // we will use one client TLS certificate for orderer peers etc.
//                final EnrollmentRequest enrollmentRequestTLS = new EnrollmentRequest();
////                enrollmentRequestTLS.addHost("localhost");
//                enrollmentRequestTLS.addHost("192.168.1.66");
//                enrollmentRequestTLS.setProfile("tls");
//                final Enrollment enroll = ca.enroll("admin", "adminpw", enrollmentRequestTLS);
//                final String tlsCertPEM = enroll.getCert();
//                final String tlsKeyPEM = getPEMStringFromPrivateKey(enroll.getKey());
//
//                final Properties tlsProperties = new Properties();
//
//                tlsProperties.put("clientKeyBytes", tlsKeyPEM.getBytes(UTF_8));
//                tlsProperties.put("clientCertBytes", tlsCertPEM.getBytes(UTF_8));
//                clientTLSProperties.put(sampleOrg.getName(), tlsProperties);
//                //Save in samplestore for follow on tests.
//                sampleStore.storeClientPEMTLCertificate(sampleOrg, tlsCertPEM);
//                sampleStore.storeClientPEMTLSKey(sampleOrg, tlsKeyPEM);
//            }
//
////            HFCAInfo info = ca.info(); //just check if we connect at all.
////            assertNotNull(info);
////            String infoName = info.getCAName();
////            if (infoName != null && !infoName.isEmpty()) {
////                assertEquals(ca.getCAName(), infoName);
////            }
//
//            // 预注册的admin （只需要用Fabric caClient登记）
//            SampleUser admin = sampleStore.getMember(TEST_ADMIN_NAME, orgName);
//            if (!admin.isEnrolled()) {  //Preregistered admin only needs to be enrolled with Fabric caClient.
//                admin.setEnrollment(ca.enroll(admin.getName(), "adminpw"));
//                admin.setMspId(mspid);
//            }
//            // 注册用户
//            SampleUser user = sampleStore.getMember(testUser1, sampleOrg.getName());
//            if (!user.isRegistered()) {  // users need to be registered AND enrolled
//                RegistrationRequest rr = new RegistrationRequest(user.getName(), "org1.department1");
//                user.setEnrollmentSecret(ca.register(rr, admin));
//            }
//            // 登记用户
//            if (!user.isEnrolled()) {
//                user.setEnrollment(ca.enroll(user.getName(), user.getEnrollmentSecret()));
//                user.setMspId(mspid);
//            }
//
//            final String sampleOrgName = sampleOrg.getName();
//            final String sampleOrgDomainName = sampleOrg.getDomainName();
//
//            // peerAdmin
//            SampleUser peerOrgAdmin = sampleStore.getMember(sampleOrgName + "Admin", sampleOrgName, sampleOrg.getMSPID(),
//                    Util.findFileSk(Paths.get(testConfig.getTestChannelPath(), "crypto-config/peerOrganizations/",
//                            sampleOrgDomainName, format("/users/Admin@%s/msp/keystore", sampleOrgDomainName)).toFile()),
//                    Paths.get(testConfig.getTestChannelPath(), "crypto-config/peerOrganizations/", sampleOrgDomainName,
//                            format("/users/Admin@%s/msp/signcerts/Admin@%s-cert.pem", sampleOrgDomainName, sampleOrgDomainName)).toFile());
//            sampleOrg.setPeerAdmin(peerOrgAdmin); //A special user that can create channels, join peers and install chaincode
//
//            sampleOrg.addUser(user);
//            sampleOrg.setAdmin(admin); // The admin of this org --
//        }
//    }

    static String getPEMStringFromPrivateKey(PrivateKey privateKey) throws IOException {
        StringWriter pemStrWriter = new StringWriter();
        JcaPEMWriter pemWriter = new JcaPEMWriter(pemStrWriter);

        pemWriter.writeObject(privateKey);

        pemWriter.close();

        return pemStrWriter.toString();
    }
}
