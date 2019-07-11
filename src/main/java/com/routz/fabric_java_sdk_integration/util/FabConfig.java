package com.routz.fabric_java_sdk_integration.util;

import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.hyperledger.fabric.sdk.ProposalResponse;
import org.hyperledger.fabric.sdk.helper.Utils;
import org.hyperledger.fabric_ca.sdk.HFCAClient;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.file.Paths;
import java.security.PrivateKey;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * Fabric配置
 */
public class FabConfig {

    private static final String ORG_NAME = "peerOrg1";
    private static final String CHANNEL_NAME = "foo";
    private static final String testUser1 = "user1";
    private static final String adminPass = "adminpw";
    private static final String sercet1 = "MzoRLMuNWNnY";

    private static final boolean runningFabricCATLS = false;
    private static final boolean runningFabricTLS = false;

    static private FabConfig config = FabConfig.getConfig();

    public static String getOrgName() {
        return ORG_NAME;
    }
    public static String getSercet() {
        return sercet1;
    }
    public static String getAdminPass() {
        return adminPass;
    }
    public static String getUserName() {
        return testUser1;
    }
    public static String getChannelName() {
        return CHANNEL_NAME;
    }

    public static String getTEST_FIXTURES_PATH() {
        return getRootPath() + "src/test/fixture";
    }

    protected static String getRootPath() {
        File file = new File("/var/fabj/");// 后面跟src...
        return file.getPath() + "/";
    }

    public static boolean isRunningFabricCATLS() {
        return runningFabricCATLS;
    }
    public static boolean isRunningFabricTLS() {
        return runningFabricTLS;
    }

    private static final int INVOKEWAITTIME = 32000;
    private static final int DEPLOYWAITTIME = 120000;
    private static final long PROPOSALWAITTIME = 120000;
    private static final boolean RUNIDEMIXMTTEST = false;       // org.hyperledger.fabric.sdktest.RunIdemixMTTest ORG_HYPERLEDGER_FABRIC_SDKTEST_RUNIDEMIXMTTEST
    private static final boolean RUNSERVICEDISCOVERYIT = false; // org.hyperledger.fabric.sdktest.RunIdemixMTTest ORG_HYPERLEDGER_FABRIC_SDKTEST_RUNIDEMIXMTTEST

    private final static String FAB_CONFIG_GEN_VERS = "v1.3";

    private Map<String, SampleOrg> sampleOrgs = new HashMap<>();

    private FabConfig() {
        Map<String, LinkedHashMap<String, Object>> yamlMap = null;
        try {
            Yaml yaml = new Yaml();
            InputStream resourceAsStream = FabConfig.class.getClassLoader().getResourceAsStream("fabric_config.yaml");
            if (resourceAsStream != null) {
                Map<String, Map<String, LinkedHashMap<String, Object>>> load = yaml.load(resourceAsStream);
                yamlMap = load.get("orgs");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        yamlMap.forEach((orgName, orgMap) -> {
            SampleOrg so = new SampleOrg(orgName, (String) orgMap.get("mspid"));
            so.setDomainName((String) orgMap.get("domainName"));
            Map<String, String> peerLocations = (Map) orgMap.get("peerLocations");
            Set<String> peerNames = peerLocations.keySet();
            peerNames.forEach(peerName -> so.addPeerLocation(peerName, grpcTLSify(peerLocations.get(peerName))));
            Map<String, String> ordererLocations = (Map) orgMap.get("ordererLocations");
            Set<String> ordererNames = ordererLocations.keySet();
            ordererNames.forEach(ordererName -> so.addOrdererLocation(ordererName, grpcTLSify(ordererLocations.get(ordererName))));
            String caName = (String) orgMap.get("caName"); //Try one of each name and no name.
            so.setCAName(caName);
            String caLocation = httpTLSify((String) orgMap.get("caLocation"));
            so.setCALocation(caLocation);

            if (runningFabricCATLS) {
                String cert = getRootPath() + "src/test/fixture/sdkintegration/e2e-2Orgs/FAB_CONFIG_GEN_VERS/crypto-config/peerOrganizations/DNAME/ca/ca.DNAME-cert.pem"
                        .replaceAll("DNAME", so.getDomainName()).replaceAll("FAB_CONFIG_GEN_VERS", FAB_CONFIG_GEN_VERS);
                File cf = new File(cert);
                if (!cf.exists() || !cf.isFile()) {
                    throw new RuntimeException("TEST is missing cert file " + cf.getAbsolutePath());
                }
                Properties properties = new Properties();
                properties.setProperty("pemFile", cf.getAbsolutePath());
                properties.setProperty("allowAllHostNames", "true"); //testing environment only NOT FOR PRODUCTION!
                so.setCAProperties(properties);
            }
            try {
                if (caName != null && !caName.isEmpty()) {
                    so.setCAClient(HFCAClient.createNewInstance(caName, caLocation, so.getCAProperties()));
                } else {
                    so.setCAClient(HFCAClient.createNewInstance(caLocation, so.getCAProperties()));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            sampleOrgs.put(orgName, so);
        });
    }

    /**
     * getConfig return back singleton for SDK configuration.
     *
     * @return Global configuration
     */
    public static FabConfig getConfig() {
        if (null == config) {
            config = new FabConfig();
        }
        return config;
    }

    public void destroy() {
        // config.sampleOrgs = null;
        config = null;
    }

    public String getFabricConfigGenVers() {
        return FAB_CONFIG_GEN_VERS;
    }

    private String grpcTLSify(String location) {
        location = location.trim();
        Exception e = Utils.checkGrpcUrl(location);
        if (e != null) {
            throw new RuntimeException(String.format("Bad TEST parameters for grpc url %s", location), e);
        }
        return runningFabricTLS ?
                location.replaceFirst("^grpc://", "grpcs://") : location;
    }

    private String httpTLSify(String location) {
        location = location.trim();
        return runningFabricCATLS ?
                location.replaceFirst("^http://", "https://") : location;
    }

    public int getTransactionWaitTime() {
        return INVOKEWAITTIME;
    }

    public int getDeployWaitTime() {
        return DEPLOYWAITTIME;
    }

    public long getProposalWaitTime() {
        return PROPOSALWAITTIME;
    }

    public Collection<SampleOrg> getIntegrationTestsSampleOrgs() {
        return Collections.unmodifiableCollection(sampleOrgs.values());
    }

    public SampleOrg getIntegrationTestsSampleOrg(String name) {
        return sampleOrgs.get(name);
    }

    public Properties getPeerProperties(String name) {
        return getEndPointProperties("peer", name);
    }

    public Properties getOrdererProperties(String name) {
        return getEndPointProperties("orderer", name);
    }

    public Properties getEndPointProperties(final String type, final String name) {
        Properties ret = new Properties();

        final String domainName = getDomainName(name);

        File cert = Paths.get(getChannelPath(), "crypto-config/ordererOrganizations".replace("orderer", type), domainName, type + "s",
                name, "tls/server.crt").toFile();
        if (!cert.exists()) {
            throw new RuntimeException(String.format("Missing cert file for: %s. Could not find at location: %s", name,
                    cert.getAbsolutePath()));
        }

        File clientCert;
        File clientKey;
        if ("orderer".equals(type)) {
            clientCert = Paths.get(getChannelPath(), "crypto-config/ordererOrganizations/" + domainName + "/users/Admin@" + domainName + "/tls/client.crt").toFile();

            clientKey = Paths.get(getChannelPath(), "crypto-config/ordererOrganizations/" + domainName + "/users/Admin@" + domainName + "/tls/client.key").toFile();
        } else {
            clientCert = Paths.get(getChannelPath(), "crypto-config/peerOrganizations/", domainName, "users/User1@" + domainName, "tls/client.crt").toFile();
            clientKey = Paths.get(getChannelPath(), "crypto-config/peerOrganizations/", domainName, "users/User1@" + domainName, "tls/client.key").toFile();
        }

        if (!clientCert.exists()) {
            throw new RuntimeException(String.format("Missing  client cert file for: %s. Could not find at location: %s", name,
                    clientCert.getAbsolutePath()));
        }

        if (!clientKey.exists()) {
            throw new RuntimeException(String.format("Missing  client key file for: %s. Could not find at location: %s", name,
                    clientKey.getAbsolutePath()));
        }
        ret.setProperty("clientCertFile", clientCert.getAbsolutePath());
        ret.setProperty("clientKeyFile", clientKey.getAbsolutePath());
        ret.setProperty("pemFile", cert.getAbsolutePath());
        ret.setProperty("hostnameOverride", name);
        ret.setProperty("sslProvider", "openSSL");
        ret.setProperty("negotiationType", "TLS");

        return ret;
    }

    protected String getChannelPath(){
        // this.getClass().getResource("src/test/fixture/sdkintegration/e2e-2Orgs/" + FAB_CONFIG_GEN_VERS).getPath();
        // **==
        File file = new File("/var/fabj/src/test/fixture/sdkintegration/e2e-2Orgs/" + FAB_CONFIG_GEN_VERS);
        return file.getPath();
    }

    private String getDomainName(final String name) {
        int dot = name.indexOf(".");
        if (-1 == dot) {
            return null;
        } else {
            return name.substring(dot + 1);
        }
    }

    protected static String getPEMStringFromPrivateKey(PrivateKey privateKey) throws IOException {
        StringWriter pemStrWriter = new StringWriter();
        JcaPEMWriter pemWriter = new JcaPEMWriter(pemStrWriter);
        pemWriter.writeObject(privateKey);
        pemWriter.close();
        return pemStrWriter.toString();
    }

    protected static void resultVerify(Collection<ProposalResponse> responses, Collection<ProposalResponse> successful, Collection<ProposalResponse> failed, int numInstallProposal) {
        for (ProposalResponse response : responses) {
            if (response.getStatus() == ProposalResponse.Status.SUCCESS) {
                Print.out("Successful install proposal response Txid: %s from peer %s", response.getTransactionID(), response.getPeer().getName());
                successful.add(response);
            } else {
                failed.add(response);
            }
        }
        if (numInstallProposal == 0) {
            Print.out("Received %d instantiate proposal responses. Successful+verified: %d . Failed: %d", responses.size(), successful.size(), failed.size());
        } else {
            Print.out("Received %d install proposal responses. Successful+verified: %d . Failed: %d", numInstallProposal, successful.size(), failed.size());
        }

        if (failed.size() > 0) {
            ProposalResponse first = failed.iterator().next();
            Print.fail("Not enough endorsers for install :" + successful.size() + ".  " + first.getMessage());
        }
    }
}
