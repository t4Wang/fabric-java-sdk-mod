import com.routz.fabricdemo.integration.domain.Chaincode;
import com.routz.fabricdemo.integration.domain.FabricUser;
import com.routz.fabricdemo.integration.domain.TransInfo;
import com.routz.fabricdemo.integration.service.FabricChannelService;
import com.routz.fabricdemo.integration.service.FabricUserService;
import com.routz.fabricdemo.integration.util.ConfigManager;
import com.routz.fabricdemo.integration.util.FabricFactory;
import org.junit.Test;

/**
 * 测试
 */
public class FabricTest {

    static private final ConfigManager config = FabricFactory.getConfig();

    static final private String CHANNEL_NAME = "qian";
    static final private String ORG_NAME = "peerOrg1";
    static final private String TEST_USER_NAME = "user1";
    static final private String TEST_USER_SECRET = "KADKbPSnCYTI";

    @Test
    public void registerUser() throws Exception {
        // 注册用户
        FabricUser user = FabricUserService.register(TEST_USER_NAME, "org1.department1", ORG_NAME);
        System.out.println(user.getEnrollmentSecret());
    }
    @Test
    public void erollUser() throws Exception {
        // 登记用户
        FabricUserService.enroll(TEST_USER_NAME, TEST_USER_SECRET, "Org1MSP", ORG_NAME, "ca0", "http://192.168.1.66:7054", null);
    }
    @Test
    public void createChannel() throws Exception {
        String channelConfigurationPath = "src/test/fixture/sdkintegration/e2e-2Orgs/" + config.getFabricConfigGenVers() + "/" + CHANNEL_NAME + ".tx";
        FabricChannelService.constructChannel(CHANNEL_NAME, channelConfigurationPath, ORG_NAME);
    }
    @Test
    public void installChaincode() throws Exception {
        final Chaincode chaincode = new Chaincode();

        FabricChannelService.installChaincode(ORG_NAME, CHANNEL_NAME, chaincode);
    }
    @Test
    public void invoke() throws Exception {
        String[] initArgs = {"a", "500", "b", "" + 200};
        String[] chaincodeFunctionArgs = {"a", "b", "5"};
        TransInfo move = FabricChannelService.invoke(initArgs, "move", chaincodeFunctionArgs, TEST_USER_NAME, TEST_USER_SECRET, CHANNEL_NAME, ORG_NAME);

        System.out.println(move.getChannelHeight());
        System.out.println(move.getBlockNumber());
        System.out.println(move.getBlockHash());
        System.out.println(move.getPreviousBlockHash());
        System.out.println(move.getTransactionId());
    }
    @Test
    public void query() {
        String fcnName = "query";
        String[] chaincodeFunctionArgs = {"b"};
        FabricChannelService.queryTransaction(fcnName, chaincodeFunctionArgs, TEST_USER_NAME, TEST_USER_SECRET, CHANNEL_NAME, ORG_NAME);
    }
}
