package com.routz.fabricdemo.integration.domain;

import org.hyperledger.fabric.sdk.ChaincodeID;
import org.hyperledger.fabric.sdk.TransactionRequest;

import java.io.File;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class Chaincode {
    private static final String TEST_FIXTURES_PATH = "src/test/fixture";
    private static final String CHAIN_CODE_FILEPATH = "sdkintegration/gocc/sample1";
    private static String CHAIN_CODE_NAME = "example_cc_go";
    private static String CHAIN_CODE_PATH = "github.com/example_cc";
    private static String CHAIN_CODE_VERSION = "1";
    private static TransactionRequest.Type CHAIN_CODE_LANG = TransactionRequest.Type.GO_LANG;

    private String chaincodeName = CHAIN_CODE_NAME;
    private String chaincodeVersion = CHAIN_CODE_VERSION;
    private String chaincodePath = CHAIN_CODE_PATH;
    private File chaincodeLocation = Paths.get(TEST_FIXTURES_PATH, CHAIN_CODE_FILEPATH).toFile();
    private File chaincodeMetaInfLocation = new File("src/test/fixture/meta-infs/end2endit");
    private File chaincodeEndorsementPolicyYamlFile = new File(TEST_FIXTURES_PATH + "/sdkintegration/chaincodeendorsementpolicy.yaml");
    private TransactionRequest.Type chaincodeLang = CHAIN_CODE_LANG;

    private Map<String, ChaincodeFunction> functions = new HashMap<>();

    private ChaincodeID chaincodeID = setupChaincodeID();

    public String getChaincodeName() {
        return chaincodeName;
    }

    public void setChaincodeName(String chaincodeName) {
        this.chaincodeName = chaincodeName;
    }

    public String getChaincodeVersion() {
        return chaincodeVersion;
    }

    public void setChaincodeVersion(String chaincodeVersion) {
        this.chaincodeVersion = chaincodeVersion;
    }

    public String getChaincodePath() {
        return chaincodePath;
    }

    public void setChaincodePath(String chaincodePath) {
        this.chaincodePath = chaincodePath;
    }

    public File getChaincodeLocation() {
        return chaincodeLocation;
    }

    public void setChaincodeLocation(File chaincodeLocation) {
        this.chaincodeLocation = chaincodeLocation;
    }

    public File getChaincodeMetaInfLocation() {
        return chaincodeMetaInfLocation;
    }

    public void setChaincodeMetaInfLocation(File chaincodeMetaInfLocation) {
        this.chaincodeMetaInfLocation = chaincodeMetaInfLocation;
    }

    public File getChaincodeEndorsementPolicyYamlFile() {
        return chaincodeEndorsementPolicyYamlFile;
    }

    public void setChaincodeEndorsementPolicyYamlFile(File chaincodeEndorsementPolicyYamlFile) {
        this.chaincodeEndorsementPolicyYamlFile = chaincodeEndorsementPolicyYamlFile;
    }

    public TransactionRequest.Type getChaincodeLang() {
        return chaincodeLang;
    }

    public void setChaincodeLang(TransactionRequest.Type chaincodeLang) {
        this.chaincodeLang = chaincodeLang;
    }

    public Map<String, ChaincodeFunction> getFunctions() {
        return functions;
    }

    public void setFunctions(Map<String, ChaincodeFunction> functions) {
        this.functions = functions;
    }

    public ChaincodeID getChaincodeID() {
        return chaincodeID;
    }

    public void setChaincodeID(ChaincodeID chaincodeID) {
        this.chaincodeID = chaincodeID;
    }

    public ChaincodeFunction getFunction(String functionName) {
        return functions.get(functionName);
    }

    public void add(ChaincodeFunction chaincodeFunction) {
        functions.put(chaincodeFunction.getChaincodeFunctionName(), chaincodeFunction);
    }

    public ChaincodeID setupChaincodeID() {
        ChaincodeID chaincodeID;
        ChaincodeID.Builder chaincodeIDBuilder = ChaincodeID.newBuilder().setName(chaincodeName)
                .setVersion(chaincodeVersion);
        if (null != chaincodePath) {
            chaincodeIDBuilder.setPath(chaincodePath);
        }
        chaincodeID = chaincodeIDBuilder.build();
        return chaincodeID;
    }

}
