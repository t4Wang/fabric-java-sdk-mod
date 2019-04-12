package com.routz.fabricdemo.integration.service;

import com.routz.fabricdemo.integration.domain.Chaincode;
import com.routz.fabricdemo.integration.domain.ChaincodeFunction;
import com.routz.fabricdemo.integration.domain.FabricOrg;
import com.routz.fabricdemo.integration.domain.FabricStore;
import com.routz.fabricdemo.integration.domain.FabricUser;
import com.routz.fabricdemo.integration.util.ConfigManager;
import com.routz.fabricdemo.integration.util.ConfigUtils;
import com.routz.fabricdemo.integration.util.FabricFactory;
import org.apache.commons.codec.binary.Hex;
import org.hyperledger.fabric.sdk.BlockEvent;
import org.hyperledger.fabric.sdk.BlockInfo;
import org.hyperledger.fabric.sdk.BlockchainInfo;
import org.hyperledger.fabric.sdk.ChaincodeEndorsementPolicy;
import org.hyperledger.fabric.sdk.ChaincodeEvent;
import org.hyperledger.fabric.sdk.ChaincodeID;
import org.hyperledger.fabric.sdk.ChaincodeResponse;
import org.hyperledger.fabric.sdk.Channel;
import org.hyperledger.fabric.sdk.HFClient;
import org.hyperledger.fabric.sdk.InstallProposalRequest;
import org.hyperledger.fabric.sdk.InstantiateProposalRequest;
import org.hyperledger.fabric.sdk.Orderer;
import org.hyperledger.fabric.sdk.Peer;
import org.hyperledger.fabric.sdk.ProposalResponse;
import org.hyperledger.fabric.sdk.QueryByChaincodeRequest;
import org.hyperledger.fabric.sdk.SDKUtils;
import org.hyperledger.fabric.sdk.TransactionInfo;
import org.hyperledger.fabric.sdk.TransactionProposalRequest;
import org.hyperledger.fabric.sdk.TransactionRequest;
import org.hyperledger.fabric.sdk.TxReadWriteSetInfo;
import org.hyperledger.fabric.sdk.User;
import org.hyperledger.fabric.sdk.exception.ChaincodeEndorsementPolicyParseException;
import org.hyperledger.fabric.sdk.exception.InvalidArgumentException;
import org.hyperledger.fabric.sdk.exception.ProposalException;
import org.hyperledger.fabric.sdk.exception.TransactionEventException;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import static com.routz.fabricdemo.integration.util.Print.assertEquals;
import static com.routz.fabricdemo.integration.util.Print.assertNotNull;
import static com.routz.fabricdemo.integration.util.Print.assertTrue;
import static com.routz.fabricdemo.integration.util.Print.fail;
import static com.routz.fabricdemo.integration.util.Print.out;
import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hyperledger.fabric.sdk.Channel.NOfEvents.createNofEvents;
import static org.hyperledger.fabric.sdk.Channel.TransactionOptions.createTransactionOptions;

public class FabricChannelService {

    static private final ConfigManager config = FabricFactory.getConfig();

    static private final FabricStore fabricStore = FabricFactory.getFabricStore();

    static final String TEST_ADMIN_NAME = "admin";

    private static Random random = new Random();

    private static final int DEPLOYWAITTIME = config.getDeployWaitTime();

    private static final byte[] EXPECTED_EVENT_DATA = "!".getBytes(UTF_8);
    private static final String EXPECTED_EVENT_NAME = "event";


    private static final ConfigUtils configHelper = new ConfigUtils();

    static {
        ConfigUtils.resetConfig();
        // 如果有环境变量，设置环境变量到配置，没有则忽略
        try {
            configHelper.customizeConfig();
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    static public void constructChannel(String channelName, String channelConfigurationPath, String orgName) throws Exception {
//        String channelConfigurationPath = TEST_FIXTURES_PATH + "/sdkintegration/e2e-2Orgs/" + config.getFabricConfigGenVers() + "/" + name + ".tx";
//        String channelName = FOO_CHANNEL_NAME;
//        FabricOrg fabricOrg = testConfig.getIntegrationTestsFabricOrg("peerOrg1");
        FabricOrg fabricOrg = config.getIntegrationTestsFabricOrg(orgName);
        HFClient client = FabricFactory.getHFClient();
        // Only peer Admin org
        FabricUser peerAdmin = FabricFactory.setupPeerAdmin(fabricOrg);
        fabricOrg.setPeerAdmin(peerAdmin);      // A special user that can create channels, join peers and install chaincode
        client.setUserContext(peerAdmin);

        Collection<Orderer> orderers = FabricFactory.setupOrderers(client, fabricOrg.getOrdererNames(), fabricOrg.getOrderNameAndLocations());
        //Just pick the first orderer in the list to create the channel.
        // 一个orderer节点 7050
        Orderer anOrderer = orderers.iterator().next();
        orderers.remove(anOrderer);

        Channel newChannel = FabricFactory.createChannel(channelConfigurationPath, client, channelName, peerAdmin, anOrderer);

        Collection<Peer> peers = FabricFactory.setupPeers(client, fabricOrg.getPeerNames(), fabricOrg.getPeerNameAndLocations());

        FabricFactory.joinPeerToChannel(newChannel, peers);

        // 将剩余命令加入通道
        for (Orderer orderer : orderers) { //add remaining orderers if any.
            newChannel.addOrderer(orderer);
        }
        //Just checks if channel can be serialized and deserialized .. otherwise this is just a waste :)
//        byte[] serializedChannelBytes = newChannel.serializeChannel();
//        newChannel.shutdown(true);
//        Channel fooChannel = client.deSerializeChannel(serializedChannelBytes).initialize();
    }

    static public void installChaincode(String orgName, String channelName) throws Exception {
        try {
//            File chaincodeLocation = Paths.get(TEST_FIXTURES_PATH, CHAIN_CODE_FILEPATH).toFile();
//            File chaincodeMetaInfLocation = new File("src/test/fixture/meta-infs/end2endit");
//            File chaincodeEndorsementPolicyYamlFile = new File(TEST_FIXTURES_PATH + "/sdkintegration/chaincodeendorsementpolicy.yaml");
//            FabricOrg fabricOrg = config.getIntegrationTestsFabricOrg("peerOrg1");
//            String channelName = FOO_CHANNEL_NAME;
            FabricOrg fabricOrg = config.getIntegrationTestsFabricOrg(orgName);

            HFClient client = FabricFactory.getHFClient();
            FabricUser fabricUser = FabricFactory.setupPeerAdmin(fabricOrg);
            client.setUserContext(fabricUser);

            Channel channel = client.newChannel(channelName);
            Collection<Orderer> orderersSetup = FabricFactory.setupOrderers(client, fabricOrg.getOrdererNames(), fabricOrg.getOrderNameAndLocations());
            for (Orderer orderer: orderersSetup) {
                channel.addOrderer(orderer);
            }

            Collection<Peer> peers = FabricFactory.setupPeers(client, fabricOrg.getPeerNames(), fabricOrg.getPeerNameAndLocations());

            FabricFactory.addPeerToChannel(channel, peers);

            channel.initialize();   // sendInstantiationProposal 必须初始化

            fabricStore.saveChannel(channel);

            // 测试捕获链码事务的列表
            Vector<ChaincodeEventCapture> chaincodeEvents = new Vector<>(); // Test list to capture chaincode events.

            Collection<ProposalResponse> responses;
            Collection<ProposalResponse> successful = new LinkedList<>();
            Collection<ProposalResponse> failed = new LinkedList<>();

            // Register a chaincode event listener that will trigger for any chaincode id and only for EXPECTED_EVENT_NAME event.
            getChaincodeEventListenerHandler(channel, chaincodeEvents);

            InstallProposalRequest installProposalRequest = getInstallProposalRequest(client);

            ////////////////////////////
            // 客户端只能向自己组织的节点发送安装请求
            // only a client from the same org as the peer can issue an install request
            /**
             * 有几个节点就需要install几个proposal
             */
            int numInstallProposal = 0;
            Collection<Peer> channelPeers = channel.getPeers();
            numInstallProposal = numInstallProposal + channelPeers.size();
            /**
             * 发送安装提案
             */
            responses = client.sendInstallProposal(installProposalRequest, channelPeers);
            /**
             * 判断installProposal响应情况
             */
            installProposalResponse(responses, successful, failed, numInstallProposal);
        } catch (Exception e) {
            e.printStackTrace();
            fail("Test failed with error : " + e.getMessage());
        }
    }

    static String transactionID = null;

    static public void invoke(String[] initArgs, String chaincodeFunctionName, String[] chaincodeFunctionArgs, String userName, String enrollmentSecret, String channelName, String orgName) throws Exception {
        FabricOrg fabricOrg = config.getIntegrationTestsFabricOrg(orgName);
//        FabricOrg fabricOrg = config.getIntegrationTestsFabricOrg("peerOrg1");
//        String channelName = FOO_CHANNEL_NAME;
//        final String orgName = fabricOrg.getName();
        final String caName = fabricOrg.getCAName();
        final String caLocation = fabricOrg.getCALocation();
        final Properties caProperties = fabricOrg.getCAProperties();
        final String mspid = fabricOrg.getMSPID();

//        String[] initArgs = {"a", "500", "b", "" + (200 + delta)};
        ChaincodeFunction initChaincodeFunction = new ChaincodeFunction("init", initArgs);

//        String[] chaincodeFunctionArgs = {"a", "b", "100"};
//        ChaincodeFunction chaincodeFunction = new ChaincodeFunction("move", chaincodeFunctionArgs);
        ChaincodeFunction chaincodeFunction = new ChaincodeFunction(chaincodeFunctionName, chaincodeFunctionArgs);

        Chaincode chaincode = new Chaincode();
        chaincode.add(initChaincodeFunction);
        chaincode.add(chaincodeFunction);

        HFClient client = FabricFactory.getHFClient();

        FabricUser fabricUser = FabricFactory.setupPeerAdmin(fabricOrg);
        fabricOrg.setPeerAdmin(fabricUser);
        client.setUserContext(fabricOrg.getPeerAdmin());

        Channel channel = client.newChannel(channelName);
        Collection<Orderer> orderersSetup = FabricFactory.setupOrderers(client, fabricOrg.getOrdererNames(), fabricOrg.getOrderNameAndLocations());
        for (Orderer orderer: orderersSetup) {
            channel.addOrderer(orderer);
        }
        Collection<Peer> peers = FabricFactory.setupPeers(client, fabricOrg.getPeerNames(), fabricOrg.getPeerNameAndLocations());
        FabricFactory.addPeerToChannel(channel, peers);
        channel.initialize();   // sendInstantiationProposal 必须初始化

        fabricStore.saveChannel(channel);

        // client, fooChannel, true, fabricOrg, 0

        // 测试捕获链码事务的列表
        Vector<ChaincodeEventCapture> chaincodeEvents = new Vector<>(); // Test list to capture chaincode events.

        // Register a chaincode event listener that will trigger for any chaincode id and only for EXPECTED_EVENT_NAME event.
        String chaincodeEventListenerHandler = getChaincodeEventListenerHandler(channel, chaincodeEvents);

        final Collection<ProposalResponse> successful = new LinkedList<>();
        final Collection<ProposalResponse> failed = new LinkedList<>();

        try {
            // 查询链码状态
            // 如果没instantiate再instantiate
            boolean isInstantiated = false;
            for (Peer peer : channel.getPeers()) {
                if (FabricFactory.checkInstantiatedChaincode(channel, peer, chaincode.getChaincodeName(), chaincode.getChaincodePath(), chaincode.getChaincodeVersion())) {
                    // 安装过链码
                    isInstantiated = true;
                }
            }

            if (isInstantiated) {
                FabricUser user = FabricUserService.enroll(userName, enrollmentSecret, mspid, orgName, caName, caLocation, caProperties);
                client.setUserContext(user);
                sendTransaction(chaincodeFunctionName, chaincodeFunctionArgs, client, channel, chaincode.getChaincodeID(), user).thenApply(transactionEvent -> {
                    transactionID = transactionEvent.getTransactionID();
                    return null;
                });
            } else {
                instantiateChaincode(chaincode, client, channel, successful, failed);
                sendTransaction(userName, enrollmentSecret, channelName, orgName, caName, caLocation, caProperties, mspid, chaincode, chaincodeFunctionName, client, channel, successful, failed);
            }

            // We can only send channel queries to peers that are in the same org as the SDK user context
            // Get the peers from the current org being used and pick one randomly to send the queries to.
            //  Set<Peer> peerSet = sampleOrg.getPeers();
            //  Peer queryPeer = peerSet.iterator().next();
            //   out("Using peer %s for channel queries", queryPeer.getName());

            BlockchainInfo channelInfo = channel.queryBlockchainInfo();
            out("Channel info for : " + channelName);
            out("Channel height: " + channelInfo.getHeight());
            String chainCurrentHash = Hex.encodeHexString(channelInfo.getCurrentBlockHash());
            String chainPreviousHash = Hex.encodeHexString(channelInfo.getPreviousBlockHash());
            out("Chain current block hash: " + chainCurrentHash);
            out("Chainl previous block hash: " + chainPreviousHash);

            // Query by block number. Should return latest block, i.e. block number 2
            BlockInfo returnedBlock = channel.queryBlockByNumber(channelInfo.getHeight() - 1);
            String previousHash = Hex.encodeHexString(returnedBlock.getPreviousHash());
            out("queryBlockByNumber returned correct block with blockNumber " + returnedBlock.getBlockNumber()
                    + " \n previous_hash " + previousHash);
            assertEquals(channelInfo.getHeight() - 1, returnedBlock.getBlockNumber());
            assertEquals(chainPreviousHash, previousHash);

            // Query by block hash. Using latest block's previous hash so should return block number 1
            byte[] hashQuery = returnedBlock.getPreviousHash();
            returnedBlock = channel.queryBlockByHash(hashQuery);
            out("queryBlockByHash returned block with blockNumber " + returnedBlock.getBlockNumber());
            assertEquals(channelInfo.getHeight() - 2, returnedBlock.getBlockNumber());

            // Query block by TxID. Since it's the last TxID, should be block 2
            returnedBlock = channel.queryBlockByTransactionID(transactionID);
            out("queryBlockByTxID returned block with blockNumber " + returnedBlock.getBlockNumber());
            assertEquals(channelInfo.getHeight() - 1, returnedBlock.getBlockNumber());

            // query transaction by ID
            TransactionInfo txInfo = channel.queryTransactionByID(transactionID);
            out("QueryTransactionByID returned TransactionInfo: txID " + txInfo.getTransactionID()
                    + "\n     validation code " + txInfo.getValidationCode().getNumber());

            if (chaincodeEventListenerHandler != null) {
                channel.unregisterChaincodeEventListener(chaincodeEventListenerHandler);

                final int numberEventsExpected =
                        channel.getPeers(EnumSet.of(Peer.PeerRole.EVENT_SOURCE)).size();
                //just make sure we get the notifications.
                for (int i = 15; i > 0; --i) {
                    if (chaincodeEvents.size() == numberEventsExpected) {
                        break;
                    } else {
                        Thread.sleep(90); // wait for the events.
                    }

                }
                assertEquals(numberEventsExpected, chaincodeEvents.size());

                for (ChaincodeEventCapture chaincodeEventCapture : chaincodeEvents) {
                    assertEquals(chaincodeEventListenerHandler, chaincodeEventCapture.handle);
                    assertEquals(transactionID, chaincodeEventCapture.chaincodeEvent.getTxId());
                    assertEquals(EXPECTED_EVENT_NAME, chaincodeEventCapture.chaincodeEvent.getEventName());
                    assertTrue(Arrays.equals(EXPECTED_EVENT_DATA, chaincodeEventCapture.chaincodeEvent.getPayload()));
                    assertEquals(chaincode.getChaincodeName(), chaincodeEventCapture.chaincodeEvent.getChaincodeId());

                    BlockEvent blockEvent = chaincodeEventCapture.blockEvent;
                    assertEquals(channelName, blockEvent.getChannelId());

                }
            } else {
                assertTrue(chaincodeEvents.isEmpty());
            }

            out("Running for Channel %s done", channelName);
        } catch (Exception e) {
            out("Caught an exception while invoking chaincode");
            e.printStackTrace();
            fail("Failed invoking chaincode with error : " + e.getMessage());
        }
    }

    static public void queryTransaction(String chaincodeQueryFunctionName, String[] chaincodeFunctionArgs, String userName, String enrollmentSecret, String channelName, String orgName) {
        try {
//            final String orgName = fabricOrg.getName();
            final FabricOrg fabricOrg = config.getIntegrationTestsFabricOrg(orgName);
            final String caName = fabricOrg.getCAName();
            final String caLocation = fabricOrg.getCALocation();
            final Properties caProperties = fabricOrg.getCAProperties();
            final String mspid = fabricOrg.getMSPID();
//            final String orgName = "peerOrg1";
//            String caName = "ca0";
//            String caLocation = "http://192.168.1.66:7054";
//            Properties caProperties = null;
//            final String mspid = "Org1MSP";
//            String domainName = "org1.example.com";

//            File chaincodeLocation = Paths.get(TEST_FIXTURES_PATH, CHAIN_CODE_FILEPATH).toFile();
//            File chaincodeMetaInfLocation = new File("src/test/fixture/meta-infs/end2endit");
//            File chaincodeEndorsementPolicyYamlFile = new File(TEST_FIXTURES_PATH + "/sdkintegration/chaincodeendorsementpolicy.yaml");
//            FabricOrg fabricOrg = config.getIntegrationTestsFabricOrg("peerOrg1");
//            String channelName = FOO_CHANNEL_NAME;
//            final String[] chaincodeFunctionArgs = {"b"};
            // "query"
            final ChaincodeFunction chaincodeFunction = new ChaincodeFunction(chaincodeQueryFunctionName, chaincodeFunctionArgs);
            final Chaincode chaincode = new Chaincode();
            final ChaincodeID chaincodeID = chaincode.getChaincodeID();

            HFClient client = FabricFactory.getHFClient();

            FabricUser peerAdmin = FabricFactory.setupPeerAdmin(fabricOrg);
            client.setUserContext(peerAdmin);

            Channel channel = client.newChannel(channelName);
            Collection<Orderer> orderersSetup = FabricFactory.setupOrderers(client, fabricOrg.getOrdererNames(), fabricOrg.getOrderNameAndLocations());
            for (Orderer orderer: orderersSetup) {
                channel.addOrderer(orderer);
            }
            Collection<Peer> peers = FabricFactory.setupPeers(client, fabricOrg.getPeerNames(), fabricOrg.getPeerNameAndLocations());
            FabricFactory.addPeerToChannel(channel, peers);

            channel.initialize();   // sendInstantiationProposal 必须初始化

            fabricStore.saveChannel(channel);

            // client, fooChannel, true, fabricOrg, 0

            // 测试捕获链码事务的列表
            Vector<ChaincodeEventCapture> chaincodeEvents = new Vector<>(); // Test list to capture chaincode events.

            // Register a chaincode event listener that will trigger for any chaincode id and only for EXPECTED_EVENT_NAME event.
            getChaincodeEventListenerHandler(channel, chaincodeEvents);

            FabricUser user = FabricUserService.enroll(userName, enrollmentSecret, mspid, orgName, caName, caLocation, caProperties);
            client.setUserContext(user);
            ////////////////////////////
            // Send Query Proposal to all peers
            // 发送查询提案
            //
            out("现在查询链码");
            QueryByChaincodeRequest queryByChaincodeRequest = client.newQueryProposalRequest();
            queryByChaincodeRequest.setArgs(chaincodeFunction.getArgs());
            queryByChaincodeRequest.setFcn(chaincodeFunction.getChaincodeFunctionName());
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
                } else {
                    String payload = proposalResponse.getProposalResponse().getResponse().getPayload().toStringUtf8();
                    out("Query payload of b from peer %s returned %s", proposalResponse.getPeer().getName(), payload);
                }
            }

        } catch (Exception e) {
            out("Caught an exception while invoking chaincode");
            e.printStackTrace();
            fail("Failed invoking chaincode with error : " + e.getMessage());
        }
    }

    static private InstallProposalRequest getInstallProposalRequest(HFClient client) throws InvalidArgumentException {
        final Chaincode chaincode = new Chaincode();
        final File chaincodeLocation = chaincode.getChaincodeLocation();
        final File chaincodeMetaInfLocation = chaincode.getChaincodeMetaInfLocation();
        final String chaincodeName = chaincode.getChaincodeName();
        final String chaincodeVersion = chaincode.getChaincodeVersion();
        final String chaincodePath = chaincode.getChaincodePath();
        final TransactionRequest.Type chaincodeLang = chaincode.getChaincodeLang();
        final ChaincodeID chaincodeID = chaincode.getChaincodeID();

        InstallProposalRequest installProposalRequest = client.newInstallProposalRequest();
        installProposalRequest.setChaincodeID(chaincodeID);
        installProposalRequest.setChaincodeSourceLocation(chaincodeLocation);
        //This sets an index on the variable a in the chaincode // see http://hyperledger-fabric.readthedocs.io/en/master/couchdb_as_state_database.html#using-couchdb-from-chaincode
        // The file IndexA.json as part of the META-INF will be packaged with the source to create the index.
        installProposalRequest.setChaincodeMetaInfLocation(chaincodeMetaInfLocation);
        installProposalRequest.setChaincodeVersion(chaincodeVersion);
        installProposalRequest.setChaincodeLanguage(chaincodeLang);
        return installProposalRequest;
    }

    static private void sendTransaction(
            String userName,
            String enrollmentSecret,
            String channelName,
            String orgName,
            String caName,
            String caLocation,
            Properties caProperties,
            String mspid,
            Chaincode chaincode,
            String functionName,
            HFClient client,
            Channel channel,
            Collection<ProposalResponse> successful,
            Collection<ProposalResponse> failed) throws Exception {
        final String chaincodePath = chaincode.getChaincodePath();
        final ChaincodeFunction function = chaincode.getFunction(functionName);
        final String chaincodeName = chaincode.getChaincodeName();
        final String chaincodeVersion = chaincode.getChaincodeVersion();
        final ChaincodeID chaincodeID = chaincode.getChaincodeID();
        final Map<String, Long> expectedMoveRCMap = new HashMap<>(); // map from channel name to move chaincode's return code.

        // Specify what events should complete the interest in this transaction. This is the default
        //  for all to complete. It's possible to specify many different combinations like
        // any from a group, all from one group and just one from another or even None(NOfEvents.createNoEvents).
        //  See. Channel.NOfEvents
        final Channel.NOfEvents nOfEvents = createNofEvents();
        if (!channel.getPeers(EnumSet.of(Peer.PeerRole.EVENT_SOURCE)).isEmpty()) {
            nOfEvents.addPeers(channel.getPeers(EnumSet.of(Peer.PeerRole.EVENT_SOURCE)));
        }
        channel.sendTransaction(successful, createTransactionOptions() //Basically the default options but shows it's usage.
                .userContext(client.getUserContext()) //could be a different user context. this is the default.
                .shuffleOrders(false) // don't shuffle any orderers the default is true.
                .orderers(channel.getOrderers()) // specify the orderers we want to try this transaction. Fails once all Orderers are tried.
                .nOfEvents(nOfEvents) // The events to signal the completion of the interest in the transaction
        ).thenApply(transactionEvent -> {

            // assertEquals(blockEvent.getChannelId(), channel.getName());
            //            final String orgName = "peerOrg1";
            //            String caName = "ca0";
            //            String caLocation = "http://192.168.1.66:7054";
            //            Properties caProperties = null;
            //            final String mspid = "Org1MSP";
            //            String domainName = "org1.example.com";
            //            String userName = "user2";
            //            user.setEnrollmentSecret("ulRAxurceJZg");
            // TODO 如果eroll前都需要fabricStore.getMember 和eroll合并
            // 登记用户
            try {
                successful.clear();
                failed.clear();

                FabricUser user = FabricUserService.enroll(userName, enrollmentSecret, mspid, orgName, caName, caLocation, caProperties);
                client.setUserContext(user);


                ///////////////
                /// Send transaction proposal to all peers
                /// 发送交易提案给所有节点
                TransactionProposalRequest transactionProposalRequest = client.newTransactionProposalRequest();
                transactionProposalRequest.setChaincodeID(chaincodeID);
                transactionProposalRequest.setChaincodeLanguage(chaincode.getChaincodeLang());
                //transactionProposalRequest.setFcn("invoke");
                transactionProposalRequest.setFcn(function.getChaincodeFunctionName());
                transactionProposalRequest.setProposalWaitTime(config.getProposalWaitTime());
                transactionProposalRequest.setArgs(function.getArgs());

                Map<String, byte[]> tm2 = new HashMap<>();
                tm2.put("HyperLedgerFabric", "TransactionProposalRequest:JavaSDK".getBytes(UTF_8)); //Just some extra junk in transient map
                tm2.put("method", "TransactionProposalRequest".getBytes(UTF_8)); // ditto
                tm2.put("result", ":)".getBytes(UTF_8));  // This should be returned in the payload see chaincode why.
                if (TransactionRequest.Type.GO_LANG.equals(chaincode.getChaincodeLang()) && config.isFabricVersionAtOrAfter("1.2")) {
                    expectedMoveRCMap.put(channelName, random.nextInt(300) + 100L); // the chaincode will return this as status see chaincode why.
                    tm2.put("rc", (expectedMoveRCMap.get(channelName) + "").getBytes(UTF_8));  // This should be returned see chaincode why.
                    // 400 and above results in the peer not endorsing!
                } else {
                    expectedMoveRCMap.put(channelName, 200L); // not really supported for Java or Node.
                }
                tm2.put(EXPECTED_EVENT_NAME, EXPECTED_EVENT_DATA);  //This should trigger an event see chaincode why.

                transactionProposalRequest.setTransientMap(tm2);

                out("sending transactionProposal to all peers with arguments: " + functionName);

                //  Collection<ProposalResponse> transactionPropResp = channel.sendTransactionProposalToEndorsers(transactionProposalRequest);
                Collection<ProposalResponse> transactionPropResp = channel.sendTransactionProposal(transactionProposalRequest, channel.getPeers());
                for (ProposalResponse response : transactionPropResp) {
                    if (response.getStatus() == ProposalResponse.Status.SUCCESS) {
                        out("Successful transaction proposal response Txid: %s from peer %s", response.getTransactionID(), response.getPeer().getName());
                        successful.add(response);
                    } else {
                        failed.add(response);
                    }
                }

                out("接收到 %d 交易提案响应. Successful+verified: %d . Failed: %d",
                        transactionPropResp.size(), successful.size(), failed.size());
                if (failed.size() > 0) {
                    ProposalResponse firstTransactionProposalResponse = failed.iterator().next();
                    fail("Not enough endorsers for invoke(move a,b,100):" + failed.size() + " endorser error: " +
                            firstTransactionProposalResponse.getMessage() +
                            ". Was verified: " + firstTransactionProposalResponse.isVerified());
                }

                // Check that all the proposals are consistent with each other. We should have only one set
                // where all the proposals above are consistent. Note the when sending to Orderer this is done automatically.
                //  Shown here as an example that applications can invoke and select.
                // See org.hyperledger.fabric.sdk.proposal.consistency_validation config property.
                // 检查 提案 一致性
                // 如果是发送给 Orderer 节点这个会自动完成 这里只是个示例
                Collection<Set<ProposalResponse>> proposalConsistencySets = SDKUtils.getProposalConsistencySets(transactionPropResp);
                if (proposalConsistencySets.size() != 1) {
                    fail(format("Expected only one set of consistent proposal responses but got %d", proposalConsistencySets.size()));
                }
                out("成功接收交易提案响应.");

                //  System.exit(10);

                ProposalResponse resp = null;
                if (!successful.isEmpty()) {
                    resp = successful.iterator().next();
                    byte[] x = resp.getChaincodeActionResponsePayload(); // This is the data returned by the chaincode.
                    String resultAsString = null;
                    if (x != null) {
                        resultAsString = new String(x, UTF_8);
                    }
                    assertEquals(":)", resultAsString);
                    assertEquals(expectedMoveRCMap.get(channelName).longValue(), resp.getChaincodeActionResponseStatus()); //Chaincode's status.

                    TxReadWriteSetInfo readWriteSetInfo = resp.getChaincodeActionResponseReadWriteSetInfo();
                    //See blockwalker below how to transverse this
                    assertNotNull(readWriteSetInfo);
                    assertTrue(readWriteSetInfo.getNsRwsetCount() > 0);

                    ChaincodeID cid = resp.getChaincodeID();
                    assertNotNull(cid);
                    final String path = cid.getPath();
                    if (null == chaincodePath) {
                        assertTrue(path == null || "".equals(path));
                    } else {
                        assertEquals(chaincodePath, path);
                    }

                    assertEquals(chaincodeName, cid.getName());
                    assertEquals(chaincodeVersion, cid.getVersion());

                    ////////////////////////////
                    // Send Transaction Transaction to orderer
                    // 发送交易给orderer
                    out("Sending chaincode transaction to orderer.");
                }
                return channel.sendTransaction(successful).get(config.getTransactionWaitTime(), TimeUnit.SECONDS);
            } catch(Exception e) {
                out("Caught an exception while invoking chaincode");
                e.printStackTrace();
                fail("Failed invoking chaincode with error : " + e.getMessage());
            }
            return null;
        }).thenApply(transactionEvent -> {
            transactionID = transactionEvent.getTransactionID();
            return null;
        }).exceptionally(e -> {
            if (e instanceof TransactionEventException) {
                BlockEvent.TransactionEvent te = ((TransactionEventException) e).getTransactionEvent();
                if (te != null) {
                    throw new AssertionError(format("Transaction with txid %s failed. %s", te.getTransactionID(), e.getMessage()), e);
                }
            }
            throw new AssertionError(format("Test failed with %s exception %s", e.getClass().getName(), e.getMessage()), e);
        }).get(config.getTransactionWaitTime(), TimeUnit.SECONDS);

    }

    static private CompletableFuture<BlockEvent.TransactionEvent> sendTransaction(String fcnName, String[] args, HFClient client, Channel channel, ChaincodeID chaincodeID, User user) {

        try {
            Collection<ProposalResponse> successful = new LinkedList<>();
            Collection<ProposalResponse> failed = new LinkedList<>();

            ///////////////
            /// Send transaction proposal to all peers
            TransactionProposalRequest transactionProposalRequest = client.newTransactionProposalRequest();
            transactionProposalRequest.setChaincodeID(chaincodeID);
            transactionProposalRequest.setFcn(fcnName);
            transactionProposalRequest.setArgs(args);
            transactionProposalRequest.setProposalWaitTime(config.getProposalWaitTime());
            if (user != null) { // specific user use that
                transactionProposalRequest.setUserContext(user);
            }

            Collection<ProposalResponse> invokePropResp = channel.sendTransactionProposal(transactionProposalRequest);
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

                throw new ProposalException(format("Not enough endorsers for invoke(move a,b,%s):%d endorser error:%s. Was verified:%b",
                        fcnName, firstTransactionProposalResponse.getStatus().getStatus(), firstTransactionProposalResponse.getMessage(), firstTransactionProposalResponse.isVerified()));

            }
            out("Successfully received transaction proposal responses.");

            ////////////////////////////
            // Send transaction to orderer
            out("Sending chaincode transaction to orderer.");
            if (user != null) {
                return channel.sendTransaction(successful, user);
            }
            return channel.sendTransaction(successful);
        } catch (Exception e) {
            throw new CompletionException(e);
        }
    }

    static private void instantiateChaincode(Chaincode chaincode, HFClient client, Channel channel, Collection<ProposalResponse> successful, Collection<ProposalResponse> failed) throws InvalidArgumentException, IOException, ChaincodeEndorsementPolicyParseException, ProposalException {
        final ChaincodeFunction function = chaincode.getFunction("init");
        final Collection<ProposalResponse> responses;
        final ChaincodeID chaincodeID = chaincode.getChaincodeID();
        final TransactionRequest.Type chaincodeLang = chaincode.getChaincodeLang();

        /**
         * Instantiate chaincode
         */
        final InstantiateProposalRequest instantiateProposalRequest = client.newInstantiationProposalRequest();
        instantiateProposalRequest.setProposalWaitTime(DEPLOYWAITTIME);
        instantiateProposalRequest.setChaincodeID(chaincodeID);
        instantiateProposalRequest.setChaincodeLanguage(chaincodeLang);
        instantiateProposalRequest.setFcn(function.getChaincodeFunctionName());
        instantiateProposalRequest.setArgs(function.getArgs());
        Map<String, byte[]> tm = new HashMap<>();
        tm.put("HyperLedgerFabric", "InstantiateProposalRequest:JavaSDK".getBytes(UTF_8));
        tm.put("method", "InstantiateProposalRequest".getBytes(UTF_8));
        instantiateProposalRequest.setTransientMap(tm);
        /*
          policy OR(Org1MSP.member, Org2MSP.member) meaning 1 signature from someone in either Org1 or Org2
          See README.md Chaincode endorsement policies section for more details.
        */
        ChaincodeEndorsementPolicy chaincodeEndorsementPolicy = new ChaincodeEndorsementPolicy();
        chaincodeEndorsementPolicy.fromYamlFile(chaincode.getChaincodeEndorsementPolicyYamlFile());
        instantiateProposalRequest.setChaincodeEndorsementPolicy(chaincodeEndorsementPolicy);

        responses = channel.sendInstantiationProposal(instantiateProposalRequest, channel.getPeers());
        /**
         * 判断 instantiate proposal response
         */
        instantiateProposalResponse(responses, successful, failed);
    }

    static private void instantiateProposalResponse(Collection<ProposalResponse> responses, Collection<ProposalResponse> successful, Collection<ProposalResponse> failed) {
        for (ProposalResponse response : responses) {
            if (response.isVerified() && response.getStatus() == ProposalResponse.Status.SUCCESS) {
                successful.add(response);
                out("Succesful instantiate proposal response Txid: %s from peer %s", response.getTransactionID(), response.getPeer().getName());
            } else {
                failed.add(response);
            }
        }
        out("Received %d instantiate proposal responses. Successful+verified: %d . Failed: %d", responses.size(), successful.size(), failed.size());
        if (failed.size() > 0) {
            for (ProposalResponse fail : failed) {
                out("Not enough endorsers for instantiate :" + successful.size() + "endorser failed with " + fail.getMessage() + ", on peer" + fail.getPeer());
            }
            ProposalResponse first = failed.iterator().next();
            fail("Not enough endorsers for instantiate :" + successful.size() + "endorser failed with " + first.getMessage() + ". Was verified:" + first.isVerified());
        }
    }

    static private void installProposalResponse(Collection<ProposalResponse> responses, Collection<ProposalResponse> successful, Collection<ProposalResponse> failed, int numInstallProposal) {
        for (ProposalResponse response : responses) {
            if (response.getStatus() == ProposalResponse.Status.SUCCESS) {
                out("Successful install proposal response Txid: %s from peer %s", response.getTransactionID(), response.getPeer().getName());
                successful.add(response);
            } else {
                failed.add(response);
            }
        }

        //   }
        out("Received %d install proposal responses. Successful+verified: %d . Failed: %d", numInstallProposal, successful.size(), failed.size());

        if (failed.size() > 0) {
            ProposalResponse first = failed.iterator().next();
            fail("Not enough endorsers for install :" + successful.size() + ".  " + first.getMessage());
        }
    }

    static private String getChaincodeEventListenerHandler(Channel channel, Vector<ChaincodeEventCapture> chaincodeEvents) throws InvalidArgumentException {
        return channel.registerChaincodeEventListener(Pattern.compile(".*"),
                        Pattern.compile(Pattern.quote(EXPECTED_EVENT_NAME)),
                        (handle, blockEvent, chaincodeEvent) -> {

                            chaincodeEvents.add(new ChaincodeEventCapture(handle, blockEvent, chaincodeEvent));

                            String es = blockEvent.getPeer() != null ? blockEvent.getPeer().getName() : "peer was null!!!";
                            out("RECEIVED Chaincode event with handle: %s, chaincode Id: %s, chaincode event name: %s, "
                                            + "transaction id: %s, event payload: \"%s\", from event source: %s",
                                    handle, chaincodeEvent.getChaincodeId(),
                                    chaincodeEvent.getEventName(),
                                    chaincodeEvent.getTxId(),
                                    new String(chaincodeEvent.getPayload()), es);
                        });
    }

    // 测试捕获链码事件
    static private class ChaincodeEventCapture { //A test class to capture chaincode events
        final String handle;
        final BlockEvent blockEvent;
        final ChaincodeEvent chaincodeEvent;

        ChaincodeEventCapture(String handle, BlockEvent blockEvent, ChaincodeEvent chaincodeEvent) {
            this.handle = handle;
            this.blockEvent = blockEvent;
            this.chaincodeEvent = chaincodeEvent;
        }
    }

}
