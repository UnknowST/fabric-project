package com.controller;


import com.google.common.collect.Maps;
import org.apache.commons.codec.binary.StringUtils;
import org.hyperledger.fabric.gateway.Contract;
import org.hyperledger.fabric.gateway.ContractException;
import org.hyperledger.fabric.gateway.*;
import org.hyperledger.fabric.sdk.Peer;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.nio.charset.StandardCharsets;
import java.util.EnumSet;
import java.util.Map;
import java.util.concurrent.TimeoutException;

@RestController
@RequestMapping("/mguser")

public class TestController {
    @Resource
    private Contract contract;



    @Resource
    private Network network;

    @GetMapping("/getUser")
    public String getUser(String userId) throws ContractException {
        byte[] queryAResultBefore = contract.evaluateTransaction("QueryUser", userId);
        return new String(queryAResultBefore, StandardCharsets.UTF_8);
    }

    @GetMapping("/addUser")
    public String addUser(String username, String name, String phone, String age, String sex, String email, String address) throws ContractException, InterruptedException, TimeoutException {
//        byte[] invokeResult = contract.createTransaction("AddUser")
//                .setEndorsingPeers(network.getChannel().getPeers(EnumSet.of(Peer.PeerRole.ENDORSING_PEER)))
//                .submit(username, name, phone, age, sex, email, address);
//        String txId = new String(invokeResult, StandardCharsets.UTF_8);
//        return txId;
        byte[] invokeResult = contract.submitTransaction("AddUser", username, name, phone, age, sex, email, address);

        String txId = new String(invokeResult, StandardCharsets.UTF_8);
        System.out.println("txID"+txId);
        return txId;
    }

    @GetMapping("/getalluser")
    public String getallUser(String userId) throws ContractException {
        byte[] queryAResultBefore = contract.evaluateTransaction("QueryAllUser");
        return new String(queryAResultBefore, StandardCharsets.UTF_8);
    }

    @GetMapping("/inituser")
    public String InitUser() throws ContractException, InterruptedException, TimeoutException {
        byte[] queryAResultBefore = contract.submitTransaction("InitLedger");
        System.out.println();
        return new String(queryAResultBefore, StandardCharsets.UTF_8);
    }
    @GetMapping("/delete")
    public String DeleteUser(String username) throws ContractException, InterruptedException, TimeoutException {
        byte[] result=contract.submitTransaction("DeleteUser",username);
        return new String(result,StandardCharsets.UTF_8);
    }
    @GetMapping("/modifemail")
    public String test1(String username,String email) throws ContractException, InterruptedException, TimeoutException {
        byte[] result=contract.submitTransaction("ModifEmail",username,email);
        return new String(result,StandardCharsets.UTF_8);
    }

    @GetMapping("/gethistory")
    public String gethistory(String username) throws ContractException {
        byte[] res=contract.evaluateTransaction("GetUserHistory",username);
        return new String(res,StandardCharsets.UTF_8);
    }

}
