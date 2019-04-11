import com.routz.fabricdemo.integration.domain.FabricUser;
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

    @Test
    public void registerUser() throws Exception {
        // 注册用户
        FabricUser user = FabricUserService.register("user1", "org1.department1", "peerOrg1");
        System.out.println(user.getEnrollmentSecret());
        // dSUqAwvQbjrY
    }
    @Test
    public void erollUser() throws Exception {
        // 登记用户
        FabricUserService.enroll("user1", "baHIGPaLGBNo", "Org1MSP", "peerOrg1", "ca0", "http://192.168.1.66:7054", null);
    }
    @Test
    public void createChannel() throws Exception {
        String channelConfigurationPath = "src/test/fixture/sdkintegration/e2e-2Orgs/" + config.getFabricConfigGenVers() + "/foo.tx";
        FabricChannelService.constructChannel("foo", channelConfigurationPath, "peerOrg1");
    }
    @Test
    public void installChaincode() throws Exception {
        FabricChannelService.installChaincode("peerOrg1", "foo");
    }

    @Test
    public void sendTransaction() throws Exception {
        String[] initArgs = {"a", "500", "b", "" + 200};
        String[] chaincodeFunctionArgs = {"a", "b", "100"};
        FabricChannelService.sendTransaction(initArgs, "move", chaincodeFunctionArgs, "user1", "baHIGPaLGBNo", "foo", "peerOrg1");
    }
}
