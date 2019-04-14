package com.routz.fabricdemo.integration.domain;

public class TransInfo {
    private static Long CHANNEL_HEIGHT = 0L;

    private Long channelHeight;
    private Long blockNumber;
    private String blockHash;
    private String previousBlockHash;
    private String transactionId;

    public TransInfo(Long channelHeight, Long blockNumber, String blockHash, String previousBlockHash, String transactionId) {
        if (channelHeight > CHANNEL_HEIGHT)
            this.channelHeight = channelHeight;
        this.blockNumber = blockNumber;
        this.blockHash = blockHash;
        this.previousBlockHash = previousBlockHash;
        this.transactionId = transactionId;
    }

    public Long getChannelHeight() {
        return channelHeight;
    }

    public void setChannelHeight(Long channelHeight) {
        this.channelHeight = channelHeight;
    }

    public Long getBlockNumber() {
        return blockNumber;
    }

    public void setBlockNumber(Long blockNumber) {
        this.blockNumber = blockNumber;
    }

    public String getBlockHash() {
        return blockHash;
    }

    public void setBlockHash(String blockHash) {
        this.blockHash = blockHash;
    }

    public String getPreviousBlockHash() {
        return previousBlockHash;
    }

    public void setPreviousBlockHash(String previousBlockHash) {
        this.previousBlockHash = previousBlockHash;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }
}
