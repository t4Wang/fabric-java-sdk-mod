package com.routz.fabric_java_sdk_integration.util;

public class TransInfo {

    private Long blockNumber;
    private String dataHash;
    private String previousHashId;
    private String calculatedBlockHash;
    private Integer envelopeCount;
    private String transactionId;
    private String channelId;
    private String transactionTimestamp;
    private String nonce;
    private String submitterMspid;
    private String certificate;

    public TransInfo() {
    }

    public TransInfo(Long blockNumber, String dataHash, String previousHashId, String calculatedBlockHash, Integer envelopeCount, String transactionId, String channelId, String transactionTimestamp, String nonce, String submitterMspid, String certificate) {
        this.blockNumber = blockNumber;
        this.dataHash = dataHash;
        this.previousHashId = previousHashId;
        this.calculatedBlockHash = calculatedBlockHash;
        this.envelopeCount = envelopeCount;
        this.transactionId = transactionId;
        this.channelId = channelId;
        this.transactionTimestamp = transactionTimestamp;
        this.nonce = nonce;
        this.submitterMspid = submitterMspid;
        this.certificate = certificate;
    }

    public Long getBlockNumber() {
        return blockNumber;
    }

    public void setBlockNumber(Long blockNumber) {
        this.blockNumber = blockNumber;
    }

    public String getDataHash() {
        return dataHash;
    }

    public void setDataHash(String dataHash) {
        this.dataHash = dataHash;
    }

    public String getPreviousHashId() {
        return previousHashId;
    }

    public void setPreviousHashId(String previousHashId) {
        this.previousHashId = previousHashId;
    }

    public String getCalculatedBlockHash() {
        return calculatedBlockHash;
    }

    public void setCalculatedBlockHash(String calculatedBlockHash) {
        this.calculatedBlockHash = calculatedBlockHash;
    }

    public Integer getEnvelopeCount() {
        return envelopeCount;
    }

    public void setEnvelopeCount(Integer envelopeCount) {
        this.envelopeCount = envelopeCount;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public String getChannelId() {
        return channelId;
    }

    public void setChannelId(String channelId) {
        this.channelId = channelId;
    }

    public String getTransactionTimestamp() {
        return transactionTimestamp;
    }

    public void setTransactionTimestamp(String transactionTimestamp) {
        this.transactionTimestamp = transactionTimestamp;
    }

    public String getNonce() {
        return nonce;
    }

    public void setNonce(String nonce) {
        this.nonce = nonce;
    }

    public String getSubmitterMspid() {
        return submitterMspid;
    }

    public void setSubmitterMspid(String submitterMspid) {
        this.submitterMspid = submitterMspid;
    }

    public String getCertificate() {
        return certificate;
    }

    public void setCertificate(String certificate) {
        this.certificate = certificate;
    }

}
