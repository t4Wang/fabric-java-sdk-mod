package com.routz.fabricdemo.integration.domain;

public class ChaincodeFunction {

    private String chaincodeFunctionName;
    private String[] args;

    public ChaincodeFunction(String chaincodeFunctionName, String[] args) {
        this.chaincodeFunctionName = chaincodeFunctionName;
        this.args = args;
    }

    public String getChaincodeFunctionName() {
        return chaincodeFunctionName;
    }

    public void setChaincodeFunctionName(String chaincodeFunctionName) {
        this.chaincodeFunctionName = chaincodeFunctionName;
    }

    public String[] getArgs() {
        return args;
    }

    public void setArgs(String[] args) {
        this.args = args;
    }
}
