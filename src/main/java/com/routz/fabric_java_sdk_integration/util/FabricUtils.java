package com.routz.fabric_java_sdk_integration.util;

import org.hyperledger.fabric.protos.peer.Query;
import org.hyperledger.fabric.sdk.BlockEvent;
import org.hyperledger.fabric.sdk.ChaincodeEndorsementPolicy;
import org.hyperledger.fabric.sdk.ChaincodeID;
import org.hyperledger.fabric.sdk.ChaincodeResponse;
import org.hyperledger.fabric.sdk.Channel;
import org.hyperledger.fabric.sdk.ChannelConfiguration;
import org.hyperledger.fabric.sdk.Enrollment;
import org.hyperledger.fabric.sdk.HFClient;
import org.hyperledger.fabric.sdk.InstallProposalRequest;
import org.hyperledger.fabric.sdk.InstantiateProposalRequest;
import org.hyperledger.fabric.sdk.Orderer;
import org.hyperledger.fabric.sdk.Peer;
import org.hyperledger.fabric.sdk.ProposalResponse;
import org.hyperledger.fabric.sdk.QueryByChaincodeRequest;
import org.hyperledger.fabric.sdk.TransactionProposalRequest;
import org.hyperledger.fabric.sdk.TransactionRequest;
import org.hyperledger.fabric.sdk.UpgradeProposalRequest;
import org.hyperledger.fabric.sdk.exception.ProposalException;
import org.hyperledger.fabric.sdk.security.CryptoSuite;
import org.hyperledger.fabric_ca.sdk.EnrollmentRequest;
import org.hyperledger.fabric_ca.sdk.HFCAClient;
import org.hyperledger.fabric_ca.sdk.RegistrationRequest;

import java.io.File;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static com.routz.fabric_java_sdk_integration.util.Print.fail;
import static com.routz.fabric_java_sdk_integration.util.Print.out;
import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hyperledger.fabric.sdk.Channel.PeerOptions.createPeerOptions;

public class FabricUtils {
    private static final String FUNCTION_NAME = "checkin";
    private static final String CHAIN_CODE_NAME = "checkin_go";
    private static final String CHAIN_CODE_PATH = "github.com/chickin_1_13";
    private static final String CHAIN_CODE_VERSION = "1.21";
    private static final String CHAIN_CODE_FILEPATH = "sdkintegration/gocc/chicken";
    private static final TransactionRequest.Type CHAIN_CODE_LANG = TransactionRequest.Type.GO_LANG;

    private static final String TEST_ADMIN_NAME = "admin";

    // 封装给java服务端调用的接口
    /**
     * 打卡记录
     * @throws Exception Exception
     * @return TransInfo
     */
    public CompletableFuture<BlockEvent.TransactionEvent> checkin(String time, String rfid, String readerId) throws Exception {
        String[] args = {rfid, readerId, time, ""};
        return invoke(FUNCTION_NAME, args);
    }

    /**
     * 分割线
     */
    private final HFClient client;
    private final Channel channel;
    private final SampleOrg sampleOrg;
    private final ChaincodeID chaincodeID;
    private final SampleStore sampleStore;

    private static final String ORG_NAME = FabConfig.getOrgName();
    private static final String USERNAME = FabConfig.getUserName();
    private static final String SECRET = FabConfig.getSercet();
    private static final String channelName = FabConfig.getChannelName();

    private static final FabConfig FAB_CONFIG = FabConfig.getConfig();

    private static final String TEST_FIXTURES_PATH = FabConfig.getTEST_FIXTURES_PATH();
    private static final File chaincodeMetaInfLocation = new File(FabConfig.getRootPath() + "src/test/fixture/meta-infs/end2endit");
    private static final File chaincodeSourceLocation = Paths.get(TEST_FIXTURES_PATH, CHAIN_CODE_FILEPATH).toFile();
    private static final File chaincodeendorsementpolicy = new File(TEST_FIXTURES_PATH + "/sdkintegration/chaincodeendorsementpolicy.yaml");
    private static final File channelConfigurationFile = new File(TEST_FIXTURES_PATH + "/sdkintegration/e2e-2Orgs/" + FAB_CONFIG.getFabricConfigGenVers() + "/" + FabConfig.getChannelName() + ".tx");
    private static final File chaincodeEndorsementPolicyYaml = new File(TEST_FIXTURES_PATH + "/sdkintegration/chaincodeendorsementpolicy.yaml");

    public FabricUtils() throws Exception {
        this(true, true);
    }
    public FabricUtils(boolean enrollUser, boolean setupChannel) throws Exception {
        sampleStore = new SampleStore();
        sampleOrg = FAB_CONFIG.getIntegrationTestsSampleOrg(ORG_NAME);
        sampleOrg.setPeerAdmin(setupPeerAdmin(sampleOrg));

        if (enrollUser) {
            SampleUser sampleUser = enroll(USERNAME, SECRET, sampleOrg.getMSPID(), sampleOrg.getName(), sampleOrg.getCAName(), sampleOrg.getCALocation(), sampleOrg.getCAProperties());
            sampleOrg.addUser(sampleUser);
        }

        client = HFClient.createNewInstance();
        client.setCryptoSuite(CryptoSuite.Factory.getCryptoSuite());
        client.setUserContext(sampleOrg.getPeerAdmin());

        if (setupChannel) {
            channel = client.newChannel(channelName);
            Collection<Orderer> orderers = new LinkedList<>();
            Set<String> ordererNames = sampleOrg.getOrdererNames();
            Map<String, String> orderNameAndLocations = sampleOrg.getOrderNameAndLocations();
            Collection<Peer> peers = new LinkedList<>();
            Set<String> peerNames = sampleOrg.getPeerNames();
            Map<String, String> peerNameAndLocations = sampleOrg.getPeerNameAndLocations();
            // orderers
            for (String ordererName : ordererNames) {
                Properties ordererProperties = FAB_CONFIG.getOrdererProperties(ordererName);
                //example of setting keepAlive to avoid timeouts on inactive http2 connections.
                // Under 5 minutes would require changes to server side to accept faster ping rates.
                ordererProperties.put("grpc.NettyChannelBuilderOption.keepAliveTime", new Object[]{5L, TimeUnit.MINUTES});
                ordererProperties.put("grpc.NettyChannelBuilderOption.keepAliveTimeout", new Object[]{8L, TimeUnit.SECONDS});
                ordererProperties.put("grpc.NettyChannelBuilderOption.keepAliveWithoutCalls", new Object[]{true});
                orderers.add(client.newOrderer(ordererName, orderNameAndLocations.get(ordererName), ordererProperties));
            }
            for (Orderer orderer: orderers) {
                channel.addOrderer(orderer);
            }
            for (String peerName : peerNames) {
                String peerLocation = peerNameAndLocations.get(peerName);
                Properties peerProperties = FAB_CONFIG.getPeerProperties(peerName); //test properties for peer.. if any.
                if (peerProperties == null) {
                    peerProperties = new Properties();
                }
                //Example of setting specific options on grpc's NettyChannelBuilder
                peerProperties.put("grpc.NettyChannelBuilderOption.maxInboundMessageSize", 9000000);
                // 新节点
                Peer peer = client.newPeer(peerName, peerLocation, peerProperties);
                peers.add(peer);
            }
            for (Peer peer : peers) {
                // 给通道加入节点
                channel.addPeer(peer,
                        createPeerOptions()
                                .setPeerRoles(EnumSet.of(Peer.PeerRole.ENDORSING_PEER, Peer.PeerRole.LEDGER_QUERY, Peer.PeerRole.CHAINCODE_QUERY, Peer.PeerRole.EVENT_SOURCE)));
            }
            channel.initialize();
        } else {
            channel = null;
        }
        chaincodeID = getChaincodeID();
    }

    public void enrollUsersSetup() throws Exception {
        // 创建通道 加入节点 测试链码
        ////////////////////////////
        //Set up USERS

        //SampleUser can be any implementation that implements org.hyperledger.fabric.sdk.User Interface
        // 不止是SampleUser， 这里可以是任何实现了User接口的类

        final SampleStore sampleStore = new SampleStore();
        final HFCAClient ca = sampleOrg.getCAClient();
        final String orgName = sampleOrg.getName();
        // 成员关系服务提供者
        final String mspid = sampleOrg.getMSPID();
        ca.setCryptoSuite(CryptoSuite.Factory.getCryptoSuite());

        if (FAB_CONFIG.isRunningFabricTLS()) {
            //This shows how to get a client TLS certificate from Fabric CA
            // we will use one client TLS certificate for orderer peers etc.
            final EnrollmentRequest enrollmentRequestTLS = new EnrollmentRequest();
            enrollmentRequestTLS.addHost("localhost");
            enrollmentRequestTLS.setProfile("tls");
            final Enrollment enroll = ca.enroll("admin", FabConfig.getAdminPass(), enrollmentRequestTLS);
            final String tlsCertPEM = enroll.getCert();
            final String tlsKeyPEM = FabConfig.getPEMStringFromPrivateKey(enroll.getKey());

            //Save in samplestore for follow on tests.
            sampleStore.storeClientPEMTLCertificate(sampleOrg, tlsCertPEM);
            sampleStore.storeClientPEMTLSKey(sampleOrg, tlsKeyPEM);
        }

        // 预注册的admin （只需要用Fabric caClient登记）
        SampleUser admin = sampleStore.getMember(TEST_ADMIN_NAME, orgName);
        if (!admin.isEnrolled()) {  //Preregistered admin only needs to be enrolled with Fabric caClient.
            admin.setEnrollment(ca.enroll(admin.getName(), FabConfig.getAdminPass()));
            admin.setMspId(mspid);
        }
        // 注册用户
        SampleUser user = sampleStore.getMember(USERNAME, orgName);
        if (!user.isRegistered()) {  // users need to be registered AND enrolled
            RegistrationRequest rr = new RegistrationRequest(user.getName(), "org1.department1");
            user.setEnrollmentSecret(ca.register(rr, admin));
        }
        System.out.println("\n\n\n\n密码 " + user.getEnrollmentSecret());
        // 登记用户
        if (!user.isEnrolled()) {
            user.setEnrollment(ca.enroll(user.getName(), user.getEnrollmentSecret()));
            user.setMspId(mspid);
        }

        sampleOrg.addUser(user);
        sampleOrg.setAdmin(admin); // The admin of this org --
    }

    public void constructChannel() throws Exception {
        ////////////////////////////
        //Construct the channel
        //

        out("构造通道 %s", channelName);

        // Only peer Admin org
        // eroll的用户

        Collection<Orderer> orderers = new LinkedList<>();

        // orderers
        for (String orderName : sampleOrg.getOrdererNames()) {

            Properties ordererProperties = FAB_CONFIG.getOrdererProperties(orderName);

            //example of setting keepAlive to avoid timeouts on inactive http2 connections.
            // Under 5 minutes would require changes to server side to accept faster ping rates.
            ordererProperties.put("grpc.NettyChannelBuilderOption.keepAliveTime", new Object[] {5L, TimeUnit.MINUTES});
            ordererProperties.put("grpc.NettyChannelBuilderOption.keepAliveTimeout", new Object[] {8L, TimeUnit.SECONDS});
            ordererProperties.put("grpc.NettyChannelBuilderOption.keepAliveWithoutCalls", new Object[] {true});

            orderers.add(client.newOrderer(orderName, sampleOrg.getOrdererLocation(orderName),
                    ordererProperties));
        }

        //Just pick the first orderer in the list to create the channel.
        // 一个orderer节点 7050
        Orderer anOrderer = orderers.iterator().next();
        orderers.remove(anOrderer);


        ChannelConfiguration channelConfiguration = new ChannelConfiguration(channelConfigurationFile);

        //Create channel that has only one signer that is this orgs peer admin. If channel creation policy needed more signature they would need to be added too.
        // 只有orgs peer admin才可以创建通道
        SampleUser peerAdmin = sampleOrg.getPeerAdmin();
        Channel newChannel = client.newChannel(channelName, anOrderer, channelConfiguration, client.getChannelConfigurationSignature(channelConfiguration, peerAdmin));

        out("创建通道： %s", channelName);

        boolean everyother = true; //test with both cases when doing peer eventing.
        for (String peerName : sampleOrg.getPeerNames()) {
            String peerLocation = sampleOrg.getPeerLocation(peerName);

            Properties peerProperties = FAB_CONFIG.getPeerProperties(peerName); //test properties for peer.. if any.
            if (peerProperties == null) {
                peerProperties = new Properties();
            }

            //Example of setting specific options on grpc's NettyChannelBuilder
            peerProperties.put("grpc.NettyChannelBuilderOption.maxInboundMessageSize", 9000000);

            // 新节点
            Peer peer = client.newPeer(peerName, peerLocation, peerProperties);
            // 给通道加入节点
            newChannel.joinPeer(peer, createPeerOptions().setPeerRoles(EnumSet.of(Peer.PeerRole.ENDORSING_PEER, Peer.PeerRole.LEDGER_QUERY, Peer.PeerRole.CHAINCODE_QUERY, Peer.PeerRole.EVENT_SOURCE))); //Default is all roles.

            out("节点 %s 加入 通道 %s", peerName, channelName);
            everyother = !everyother;
        }

        // 将剩余命令加入通道
        for (Orderer orderer : orderers) { //add remaining orderers if any.
            newChannel.addOrderer(orderer);
        }

        out("完成构造通道 %s", channelName);
    }

    public void installProposal() {
        try {
            out("运行通道 %s", channelName);

            Collection<ProposalResponse> responses;
            Collection<ProposalResponse> successful = new LinkedList<>();
            Collection<ProposalResponse> failed = new LinkedList<>();

            ////////////////////////////
            // Install Proposal Request
            //
            client.setUserContext(sampleOrg.getPeerAdmin());

            out("Creating install proposal");

            InstallProposalRequest installProposalRequest = client.newInstallProposalRequest();
            installProposalRequest.setChaincodeID(chaincodeID);

            // on foo chain install from directory.

            ////For GO language and serving just a single user, chaincodeSource is mostly likely the users GOPATH
            // 链码地址
            installProposalRequest.setChaincodeSourceLocation(chaincodeSourceLocation);
            installProposalRequest.setChaincodeMetaInfLocation(chaincodeMetaInfLocation);
            installProposalRequest.setChaincodeVersion(CHAIN_CODE_VERSION);
            installProposalRequest.setChaincodeLanguage(CHAIN_CODE_LANG);

            out("send install proposal");

            ////////////////////////////
            // 客户端只能向自己组织的节点发送安装请求
            // only a client from the same org as the peer can issue an install request
            int numInstallProposal = 0;
            //    Set<String> orgs = orgPeers.keySet();
            //   for (SampleOrg org : testSampleOrgs) {

            Collection<Peer> peers = channel.getPeers();
            numInstallProposal = numInstallProposal + peers.size();
            /**
             *
             * 发送安装提案
             *
             */
            responses = client.sendInstallProposal(installProposalRequest, peers);

            FabConfig.resultVerify(responses, successful, failed, numInstallProposal);
        } catch (Exception e) {
            out("Caught an exception running channel %s", channel.getName());
            e.printStackTrace();
            fail("Test failed with error : " + e.getMessage());
        }
    }

    public void instantiedProposal(String[] args) {
        try {
            out("运行通道 %s", channelName);

            Collection<ProposalResponse> responses;
            Collection<ProposalResponse> successful = new LinkedList<>();
            Collection<ProposalResponse> failed = new LinkedList<>();

            ///////////////
            //// Instantiate chaincode.
            InstantiateProposalRequest instantiateProposalRequest = client.newInstantiationProposalRequest();
            instantiateProposalRequest.setProposalWaitTime(FAB_CONFIG.getDeployWaitTime());
            instantiateProposalRequest.setChaincodeID(chaincodeID);
            instantiateProposalRequest.setChaincodeLanguage(CHAIN_CODE_LANG);
            instantiateProposalRequest.setFcn("init");
            instantiateProposalRequest.setArgs(args);
            Map<String, byte[]> tm = new HashMap<>();
            tm.put("HyperLedgerFabric", "InstantiateProposalRequest:JavaSDK".getBytes(UTF_8));
            tm.put("method", "InstantiateProposalRequest".getBytes(UTF_8));
            instantiateProposalRequest.setTransientMap(tm);

            /*
              policy OR(Org1MSP.member, Org2MSP.member) meaning 1 signature from someone in either Org1 or Org2
              See README.md Chaincode endorsement policies section for more details.
            */
            ChaincodeEndorsementPolicy chaincodeEndorsementPolicy = new ChaincodeEndorsementPolicy();
            chaincodeEndorsementPolicy.fromYamlFile(chaincodeendorsementpolicy);
            instantiateProposalRequest.setChaincodeEndorsementPolicy(chaincodeEndorsementPolicy);

            out("Sending instantiateProposalRequest to all peers with arguments: a and b set to 100 and %s respectively", "" + (200));
            successful.clear();
            failed.clear();

            responses = channel.sendInstantiationProposal(instantiateProposalRequest, channel.getPeers());
//            responses = channel.sendInstantiationProposal(instantiateProposalRequest);
            FabConfig.resultVerify(responses, successful, failed, 0);
            ///////////////
            /// Send instantiate transaction to orderer
            out("Sending instantiateTransaction to orderer with a and b set to 100 and %s respectively", "" + (200));

            channel.sendTransaction(successful, client.getUserContext());
        } catch (Exception e) {
            out("Caught an exception running channel %s", channel.getName());
            e.printStackTrace();
            fail("Test failed with error : " + e.getMessage());
        }
    }

    public void upgradeRequest(String[] initArgs) throws Exception {
        Collection<ProposalResponse> successful = new LinkedList<>();
        Collection<ProposalResponse> failed = new LinkedList<>();

        UpgradeProposalRequest upgradeProposalRequest = client.newUpgradeProposalRequest();
        upgradeProposalRequest.setChaincodeID(chaincodeID);
        upgradeProposalRequest.setProposalWaitTime(FAB_CONFIG.getDeployWaitTime());
        upgradeProposalRequest.setFcn("init");
        upgradeProposalRequest.setArgs(initArgs);    // no arguments don't change the ledger see chaincode.
        ChaincodeEndorsementPolicy chaincodeEndorsementPolicy;

        chaincodeEndorsementPolicy = new ChaincodeEndorsementPolicy();
        chaincodeEndorsementPolicy.fromYamlFile(chaincodeEndorsementPolicyYaml);

        upgradeProposalRequest.setChaincodeEndorsementPolicy(chaincodeEndorsementPolicy);

        out("Sending upgrade proposal");

        Collection<ProposalResponse> responses2;

        responses2 = channel.sendUpgradeProposal(upgradeProposalRequest);

        successful.clear();
        failed.clear();
        for (ProposalResponse response : responses2) {
            if (response.getStatus() == ChaincodeResponse.Status.SUCCESS) {
                out("Successful upgrade proposal response Txid: %s from peer %s", response.getTransactionID(), response.getPeer().getName());
                successful.add(response);
            } else {
                failed.add(response);
            }
        }

        out("Received %d upgrade proposal responses. Successful+verified: %d . Failed: %d", channel.getPeers().size(), successful.size(), failed.size());

        if (failed.size() > 0) {
            ProposalResponse first = failed.iterator().next();
            throw new AssertionError("Not enough endorsers for upgrade :"
                    + successful.size() + ".  " + first.getMessage());
        }

        channel.sendTransaction(successful).get(FAB_CONFIG.getTransactionWaitTime(), TimeUnit.SECONDS);
    }

    public CompletableFuture<BlockEvent.TransactionEvent> invoke(String functionName, String[] args) throws Exception {
        final SampleUser user = sampleOrg.getPeerAdmin();
        final Collection<ProposalResponse> successful = new LinkedList<>();
        final Collection<ProposalResponse> failed = new LinkedList<>();

        ///////////////
        /// Send transaction proposal to all peers
        final TransactionProposalRequest transactionProposalRequest = client.newTransactionProposalRequest();
        transactionProposalRequest.setChaincodeID(chaincodeID);
        transactionProposalRequest.setFcn(functionName);
        transactionProposalRequest.setArgs(args);
        transactionProposalRequest.setProposalWaitTime(FAB_CONFIG.getProposalWaitTime());
        if (user != null) { // specific user use that
            transactionProposalRequest.setUserContext(user);
        }
        out("sending transaction proposal to all peers with arguments");

        final Collection<ProposalResponse> invokePropResp = channel.sendTransactionProposal(transactionProposalRequest);
        for (ProposalResponse response : invokePropResp) {
            if (response.getStatus() == ChaincodeResponse.Status.SUCCESS) {
                out("Successful transaction proposal response Txid: %s from peer %s", response.getTransactionID(), response.getPeer().getName());
                successful.add(response);
            } else {
                failed.add(response);
            }
        }

        out("Received %d transaction proposal responses. Successful+verified: %d . Failed: %d",
                invokePropResp.size(), successful.size(), failed.size());
        if (failed.size() > 0) {
            ProposalResponse firstTransactionProposalResponse = failed.iterator().next();

            throw new ProposalException(format("Not enough endorsers for invoke( %s):%d endorser error:%s. Was verified:%b",
                    "0", firstTransactionProposalResponse.getStatus().getStatus(), firstTransactionProposalResponse.getMessage(), firstTransactionProposalResponse.isVerified()));

        }
        out("Successfully received transaction proposal responses.");

        ////////////////////////////
        // Send transaction to orderer
        out("Sending chaincode transaction to orderer.");
        if (user != null) {
            return channel.sendTransaction(successful, user);
        }
        return channel.sendTransaction(successful);
    }

    public void queryInstalledChaincode() throws org.hyperledger.fabric.sdk.exception.InvalidArgumentException, ProposalException {
        Peer peer = channel.getPeers().iterator().next();
        List<Query.ChaincodeInfo> ccinfoList = client.queryInstalledChaincodes(peer);
        checkChaincodeStatus(ccinfoList);
    }

    public void queryInstantiateStatus() throws Exception {
        Peer peer = channel.getPeers().iterator().next();
        out("Checking instantiated chaincode: %s, at version: %s, on peer: %s", CHAIN_CODE_NAME, CHAIN_CODE_VERSION, peer.getName());
        List<Query.ChaincodeInfo> ccinfoList = channel.queryInstantiatedChaincodes(peer);
        checkChaincodeStatus(ccinfoList);
    }

    public String query(String[] args) throws Exception {
        QueryByChaincodeRequest queryByChaincodeRequest = client.newQueryProposalRequest();
        queryByChaincodeRequest.setArgs(args);
        queryByChaincodeRequest.setFcn("get");
        queryByChaincodeRequest.setChaincodeID(chaincodeID);

        Map<String, byte[]> tm2 = new HashMap<>();
        tm2.put("HyperLedgerFabric", "QueryByChaincodeRequest:JavaSDK".getBytes(UTF_8));
        tm2.put("method", "QueryByChaincodeRequest".getBytes(UTF_8));
        queryByChaincodeRequest.setTransientMap(tm2);

        Collection<ProposalResponse> queryProposals = channel.queryByChaincode(queryByChaincodeRequest, channel.getPeers());
        for (ProposalResponse proposalResponse : queryProposals) {
            if (!proposalResponse.isVerified() || proposalResponse.getStatus() != ProposalResponse.Status.SUCCESS) {
                fail("Failed query proposal from peer " + proposalResponse.getPeer().getName() + " status: " + proposalResponse.getStatus() +
                        ". Messages: " + proposalResponse.getMessage()
                        + ". Was verified : " + proposalResponse.isVerified());
                throw new IllegalArgumentException("Failed query proposal from peer " + proposalResponse.getPeer().getName() + " status: " + proposalResponse.getStatus() +
                        ". Messages: " + proposalResponse.getMessage()
                        + ". Was verified : " + proposalResponse.isVerified());
            } else {
                String payload = proposalResponse.getProposalResponse().getResponse().getPayload().toStringUtf8();
                out("Query payload of b from peer %s returned %s", proposalResponse.getPeer().getName(), payload);
                return payload;
            }
        }
        return null;
    }
    public SampleUser enroll(String userName, String enrollmentSecret, String mspid, String orgName, String caName, String caLocation, Properties caProperties) throws Exception {
        SampleUser user = sampleStore.getMember(userName, orgName);
        HFCAClient ca = getCAClient(caName, caLocation, caProperties); // 成员关系服务提供者

        user.setEnrollmentSecret(enrollmentSecret);
        // 登记用户
        if (!user.isEnrolled()) {
            user.setEnrollment(ca.enroll(userName, enrollmentSecret));
            user.setMspId(mspid);
        }
        return user;
    }

    /**********************************************************************************************************
     *
     *
     *
     **********************************************************************************************************/

    private ChaincodeID getChaincodeID() {
        final ChaincodeID chaincodeID;
        ChaincodeID.Builder chaincodeIDBuilder = ChaincodeID.newBuilder().setName(CHAIN_CODE_NAME)
                .setVersion(CHAIN_CODE_VERSION);
        if (null != CHAIN_CODE_PATH) {
            chaincodeIDBuilder.setPath(CHAIN_CODE_PATH);
        }
        chaincodeID = chaincodeIDBuilder.build();
        return chaincodeID;
    }

    private boolean checkChaincodeStatus(List<Query.ChaincodeInfo> ccinfoList) {
        boolean found = false;

        for (Query.ChaincodeInfo ccifo : ccinfoList) {

            if (CHAIN_CODE_PATH != null) {
                found = CHAIN_CODE_NAME.equals(ccifo.getName()) && CHAIN_CODE_PATH.equals(ccifo.getPath()) && CHAIN_CODE_VERSION.equals(ccifo.getVersion());
                if (found) {
                    break;
                }
            }
            found = CHAIN_CODE_NAME.equals(ccifo.getName()) && CHAIN_CODE_VERSION.equals(ccifo.getVersion());
            if (found) {
                break;
            }
        }
        return found;
    }

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

    private HFCAClient getCAClient(String caName, String caLocation, Properties caProperties) {
        HFCAClient caclient = null;
        try {
            caclient = HFCAClient.createNewInstance(caName, caLocation, caProperties);
            caclient.setCryptoSuite(CryptoSuite.Factory.getCryptoSuite()); // 成员关系服务提供者
        } catch (Exception e) {
            e.printStackTrace();
        }
        return caclient;
    }

    private Channel setupChannel(String path, HFClient client, String name, SampleUser peerAdmin, Orderer orderer) throws Exception {
        ChannelConfiguration channelConfiguration = new ChannelConfiguration(new File(path));

        //Create channel that has only one signer that is this orgs peer admin. If channel creation policy needed more signature they would need to be added too.
        // 只有orgs peer admin才可以创建通道
        //        Channel newChannel = client.newChannel(name);
        return client.newChannel(name, orderer, channelConfiguration, client.getChannelConfigurationSignature(channelConfiguration, peerAdmin));
    }

    private void joinPeerToChannel(Channel channel, Collection<Peer> peers) throws ProposalException {
        for (Peer peer : peers) {
            // 给通道加入节点
            channel.joinPeer(peer,
                createPeerOptions()
                    .setPeerRoles(EnumSet.of(Peer.PeerRole.ENDORSING_PEER, Peer.PeerRole.LEDGER_QUERY, Peer.PeerRole.CHAINCODE_QUERY, Peer.PeerRole.EVENT_SOURCE)));
        }
    }

}
