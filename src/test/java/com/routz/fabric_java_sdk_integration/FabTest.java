package com.routz.fabric_java_sdk_integration;

import com.routz.fabric_java_sdk_integration.util.FabricUtils;
import org.junit.Test;

import java.util.Date;

public class FabTest {
    /**
     * @throws Exception
     */
    @Test
    public void checkin2() throws Exception {
        FabricUtils fabricUtils = new FabricUtils();
        fabricUtils.checkin(new Date().toString(), "E20000195105006608A12031", "xxx");
    }

    /**
     * 查询
     * @throws Exception
     */
    @Test
    public void query() throws Exception {
        String[] values = {"E20000195105006608A12031"};
        FabricUtils fabricUtils = new FabricUtils();
        String query = fabricUtils.query(values);
        System.out.println(query);
    }

    /**
     * enroll用户
     * @throws Exception
     */
    @Test
    public void enrollTest() throws Exception {
        FabricUtils fabricUtils = new FabricUtils(false, false);
        fabricUtils.enrollUsersSetup();
    }

    /**
     * 构造通道
     * @throws Exception
     */
    @Test
    public void constructChannelTest() throws Exception {
        FabricUtils fabricUtils = new FabricUtils(false, false);
        fabricUtils.constructChannel();
    }

    /**
     * 安装链码
     * @throws Exception
     */
    @Test
    public void installCheckinTest() throws Exception {
        FabricUtils fabricUtils = new FabricUtils();
        fabricUtils.installProposal();
    }

    /**
     * 初始化链码
     * @throws Exception
     */
    @Test
    public void instantiedCheckinProposalTest() throws Exception {
        String[] values = {"E20000195105006608A12031", "初始化"};
        FabricUtils fabricUtils = new FabricUtils();
        fabricUtils.instantiedProposal(values);
    }

    /**
     * 查询初始化状态
     * @throws Exception
     */
    @Test
    public void queryInstantiedCheckinProposalTest() throws Exception {
        FabricUtils fabricUtils = new FabricUtils();
        fabricUtils.queryInstantiateStatus();
    }

    /**
     * 安装升级请求
     * @throws Exception
     */
    @Test
    public void installUpgradeTest() throws Exception {
        FabricUtils fabricUtils = new FabricUtils();
        fabricUtils.installProposal();
    }

    /**
     * 升级链码
     * @throws Exception
     */
    @Test
    public void upgrade() throws Exception {
        FabricUtils fabricUtils = new FabricUtils();
        String[] args = {""};
        fabricUtils.upgradeRequest(args);
    }

    /**
     * 调用链码
     * @throws Exception
     */
    @Test
    public void checkin() throws Exception {
        FabricUtils fabricUtils = new FabricUtils();
        fabricUtils.checkin(new Date().toString(), "E20000195105006608A12031", "X000001001");
    }
}
