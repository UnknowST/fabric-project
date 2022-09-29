package com;

import com.util.SHA256Util;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class FabricGatewayApplicationTests {

    @Test
    void contextLoads() throws Exception {
        // sha256
        SHA256Util util=new SHA256Util();
        System.out.println(util.GetSHA256Str("123"));
    }

}
