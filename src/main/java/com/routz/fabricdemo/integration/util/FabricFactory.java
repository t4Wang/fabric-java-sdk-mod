package com.routz.fabricdemo.integration.util;

import com.routz.fabricdemo.integration.domain.FabricOrg;
import com.routz.fabricdemo.integration.domain.FabricStore;
import com.routz.fabricdemo.integration.domain.FabricUser;
import org.hyperledger.fabric.protos.peer.Query;
import org.hyperledger.fabric.sdk.Channel;
import org.hyperledger.fabric.sdk.ChannelConfiguration;
import org.hyperledger.fabric.sdk.HFClient;
import org.hyperledger.fabric.sdk.Orderer;
import org.hyperledger.fabric.sdk.Peer;
import org.hyperledger.fabric.sdk.exception.CryptoException;
import org.hyperledger.fabric.sdk.exception.InvalidArgumentException;
import org.hyperledger.fabric.sdk.exception.ProposalException;
import org.hyperledger.fabric.sdk.exception.TransactionException;
import org.hyperledger.fabric.sdk.security.CryptoSuite;
import org.hyperledger.fabric_ca.sdk.HFCAClient;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.spec.InvalidKeySpecException;
import java.util.Collection;
import java.util.EnumSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static java.lang.String.format;
import static org.hyperledger.fabric.sdk.Channel.PeerOptions.createPeerOptions;

public class FabricFactory {
    // String channelName, FabricOrg fabricOrg,
    // Create instance of client.
    static private HFClient client = HFClient.createNewInstance();
    static private HFCAClient caclient;
    // All packages for PKI key creation/signing/verification implement this interface
    static private ConfigManager config = ConfigManager.getConfig();

    static FabricStore fabricStore;

    private FabricFactory() {}

    static {
        try {
            client.setCryptoSuite(CryptoSuite.Factory.getCryptoSuite());

            File fabricStoreFile = new File(System.getProperty("java.io.tmpdir") + "/HFCSampletest.properties");
            if (fabricStoreFile.exists()) { //For testing start fresh
                fabricStoreFile.delete();
            }
            fabricStore = new FabricStore(fabricStoreFile);
        } catch (CryptoException e) {
            e.printStackTrace();
        } catch (InvalidArgumentException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    static public ConfigManager getConfig() {
        return config;
    }

    static public HFClient getHFClient() {
        return client;
    }
    static public HFCAClient getCAClient(String caName, String caLocation, Properties caProperties) {
        try {
            caclient = HFCAClient.createNewInstance(caName, caLocation, caProperties);
            caclient.setCryptoSuite(CryptoSuite.Factory.getCryptoSuite()); // 成员关系服务提供者
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (CryptoException e) {
            e.printStackTrace();
        } catch (InvalidArgumentException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (org.hyperledger.fabric_ca.sdk.exception.InvalidArgumentException e) {
            e.printStackTrace();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        return caclient;
    }
    static public FabricStore getFabricStore() {
        return fabricStore;
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

    static public Channel createChannel(String path, HFClient client, String name, FabricUser peerAdmin, Orderer orderer) throws IOException, InvalidArgumentException, TransactionException {
        ChannelConfiguration channelConfiguration = new ChannelConfiguration(new File(path));

        //Create channel that has only one signer that is this orgs peer admin. If channel creation policy needed more signature they would need to be added too.
        // 只有orgs peer admin才可以创建通道
        //        Channel newChannel = client.newChannel(name);
        return client.newChannel(name, orderer, channelConfiguration, client.getChannelConfigurationSignature(channelConfiguration, peerAdmin));
    }

    static public void joinPeerToChannel(Channel channel, Collection<Peer> peers) throws ProposalException {
        for (Peer peer : peers ) {
            // 给通道加入节点
            channel.joinPeer(peer,
                    createPeerOptions()
                            .setPeerRoles(EnumSet.of(Peer.PeerRole.ENDORSING_PEER, Peer.PeerRole.LEDGER_QUERY, Peer.PeerRole.CHAINCODE_QUERY, Peer.PeerRole.EVENT_SOURCE)));
        }
    }

    static public Collection<Orderer> setupOrderers(HFClient client, Set<String> ordererNames, Map<String, String> ordererLocations) throws InvalidArgumentException {
        Collection<Orderer> orderers = new LinkedList<>();

        // orderers
        for (String ordererName : ordererNames) {
            Properties ordererProperties = config.getOrdererProperties(ordererName);
            //example of setting keepAlive to avoid timeouts on inactive http2 connections.
            // Under 5 minutes would require changes to server side to accept faster ping rates.
            ordererProperties.put("grpc.NettyChannelBuilderOption.keepAliveTime", new Object[] {5L, TimeUnit.MINUTES});
            ordererProperties.put("grpc.NettyChannelBuilderOption.keepAliveTimeout", new Object[] {8L, TimeUnit.SECONDS});
            ordererProperties.put("grpc.NettyChannelBuilderOption.keepAliveWithoutCalls", new Object[] {true});
            orderers.add(client.newOrderer(ordererName, ordererLocations.get(ordererName), ordererProperties));
        }
        return orderers;
    }

    static public Collection<Peer> setupPeers(HFClient client, Set<String> peerNames, Map<String, String> peerNamesAndLocations) throws InvalidArgumentException {
        Collection<Peer> peers = new LinkedList<>();
        for (String peerName : peerNames) {
            String peerLocation = peerNamesAndLocations.get(peerName);
            Properties peerProperties = config.getPeerProperties(peerName); //test properties for peer.. if any.
            if (peerProperties == null) {
                peerProperties = new Properties();
            }
            //Example of setting specific options on grpc's NettyChannelBuilder
            peerProperties.put("grpc.NettyChannelBuilderOption.maxInboundMessageSize", 9000000);
            // 新节点
            Peer peer = client.newPeer(peerName, peerLocation, peerProperties);
            peers.add(peer);
        }
        return peers;
    }

    static public void addPeerToChannel(Channel channel, Collection<Peer> peers) throws InvalidArgumentException {
        for (Peer peer : peers ) {
            // 给通道加入节点
            channel.addPeer(peer,
                    createPeerOptions()
                            .setPeerRoles(EnumSet.of(Peer.PeerRole.ENDORSING_PEER, Peer.PeerRole.LEDGER_QUERY, Peer.PeerRole.CHAINCODE_QUERY, Peer.PeerRole.EVENT_SOURCE)));
        }
    }

    private static boolean checkInstalledChaincode(HFClient client, Peer peer, String ccName, String ccPath, String ccVersion) throws InvalidArgumentException, ProposalException {
        List<Query.ChaincodeInfo> ccinfoList = client.queryInstalledChaincodes(peer);
        boolean found = false;

        for (Query.ChaincodeInfo ccifo : ccinfoList) {
            if (ccPath != null) {
                found = ccName.equals(ccifo.getName()) && ccPath.equals(ccifo.getPath()) && ccVersion.equals(ccifo.getVersion());
                if (found) {
                    break;
                }
            }
            found = ccName.equals(ccifo.getName()) && ccVersion.equals(ccifo.getVersion());
            if (found) {
                break;
            }
        }
        return found;
    }

    public static boolean checkInstantiatedChaincode(Channel channel, Peer peer, String ccName, String ccPath, String ccVersion) throws InvalidArgumentException, ProposalException {
        List<Query.ChaincodeInfo> ccinfoList = channel.queryInstantiatedChaincodes(peer);
        boolean found = false;

        for (Query.ChaincodeInfo ccifo : ccinfoList) {
            if (ccPath != null) {
                found = ccName.equals(ccifo.getName()) && ccPath.equals(ccifo.getPath()) && ccVersion.equals(ccifo.getVersion());
                if (found) {
                    break;
                }
            }
            found = ccName.equals(ccifo.getName()) && ccVersion.equals(ccifo.getVersion());
            if (found) {
                break;
            }
        }
        return found;
    }
}
