package com.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.daomain.CerImage;
import com.daomain.Message;
import com.daomain.User;
import com.daomain.UserLogs;
import org.hyperledger.fabric.gateway.Contract;
import org.hyperledger.fabric.gateway.ContractException;
import org.hyperledger.fabric.gateway.Network;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/admin")
public class AdminController {
    @Value("${admin.username}")
    private String username;
    @Value("${admin.password}")
    private String password;

    @Autowired
    Message message;
    @Resource
    private Contract contract;

    @Resource
    private Network network;


    /**
     * 管理员登录
     *
     * @param captcha
     * @param username
     * @param password
     * @param request
     * @return
     */
    @RequestMapping("/login")
    public Message login(String captcha, String username, String password, HttpServletRequest request) {
        //获取验证码session
        String code = (String) request.getSession().getAttribute("CHECKCODE_SERVER");
        /*转为小写*/
        code = code.toLowerCase();
        captcha = captcha.toLowerCase();
        if (!code.equals(captcha)) {
            message.setErrcode("0");
            message.setMsg("验证码错误！");
            return message;
        }
        if (username.compareTo(this.username) == 0 && password.compareTo(password) == 0) {
            message.setErrcode("1");
            message.setMsg("success!");
            Map<String, String> map = new HashMap<>();
            map.put("username", username);
            map.put("password", password);
            request.getSession().setAttribute("user", map);
            return message;
        }
        message.setMsg("failed login!");
        message.setErrcode("0");
        return message;

    }

    /**
     * 或许用户信息列表
     *
     * @param username
     * @param name
     * @param email
     * @param limit
     * @param page
     * @return
     */
    @RequestMapping("/userlist")
    public JSONObject userlist(String username, String name, String email, Integer limit, Integer page) {
        JSONObject jsonObject = new JSONObject();
        byte[] res;
        try {
            res = contract.evaluateTransaction("QueryAllUser");
        } catch (ContractException e) {
            jsonObject.put("code", -1);
            jsonObject.put("msg", e.getMessage());
            return jsonObject;
        }
        String data = new String(res, StandardCharsets.UTF_8);
        JSONArray jsonArray = JSON.parseArray(data);
        List<User> list = new ArrayList<>();
        // 得到 list集合
        for (Object object : jsonArray) {
            JSONObject jsonObject1 = (JSONObject) JSON.toJSON(object);
            JSONObject jsonObject2 = jsonObject1.getJSONObject("Record");
            User user = JSON.parseObject(String.valueOf(jsonObject2), User.class);

            byte[] res1;
            try {
                res1 = contract.evaluateTransaction("GetTxidAndtime", user.getUsername());
            } catch (ContractException e) {
                System.out.println(e.getMessage());
                jsonObject.put("code", -1);
                jsonObject.put("msg", "数据请求失败！");
                return jsonObject;
            }
            String tt = new String(res1, StandardCharsets.UTF_8);
            JSONObject jsonObject3 = JSONObject.parseObject(tt);
            user.setTxid(jsonObject3.getString("txid"));
            user.setTimestamp(jsonObject3.getDate("timestamp"));
            list.add(user);
        }

        // 按搜索条件 筛选
        if (username != null && !username.equals(""))
            list = list.stream().filter(user -> username.equals(user.getUsername())).collect(Collectors.toList());
        if (name != null && !name.equals(""))
            list = list.stream().filter(user -> name.equals(user.getName())).collect(Collectors.toList());
        if (email != null && !email.equals(""))
            list = list.stream().filter(user -> email.equals(user.getEmail())).collect(Collectors.toList());

        // 分页查询
        List<User> list1 = new ArrayList<>();
        for (int i = (page - 1) * limit, j = 1; i < list.size() && j < limit; ++i, ++j) {
            list1.add(list.get(i));
        }
        jsonObject.put("data", list1);
        jsonObject.put("code", 0);
        jsonObject.put("count", list.size());
        jsonObject.put("msg", "数据请求成功！");
        return jsonObject;

    }

    /**
     * 用户密码重置
     *
     * @param username
     * @return
     */
    @RequestMapping("/repass")
    public Message repass(String username) {
        try {
            byte[] res = contract.submitTransaction("ModifPassw", username, "888888");
        } catch (ContractException | TimeoutException | InterruptedException e) {
            message.setErrcode("0");
            message.setMsg(e.getMessage());
            return message;
        }
        message.setMsg("密码重置成功！");
        message.setErrcode("1");
        return message;

    }

    /**
     * 按条件获取 存证列表
     *
     * @param num
     * @param username
     * @param name
     * @param txid
     * @param limit
     * @param page
     * @return
     */
    @RequestMapping("/cerlist")
    public JSONObject cerlist(String num, String username, String name, String txid, Integer limit, Integer page) {
        // 从区块链上拿到所有的 存证列表
        Contract contract1 = GetContract(network);
        JSONObject jsonObject = new JSONObject();
        byte[] res;
        try {
            res = contract1.evaluateTransaction("QueryAllInfo");
        } catch (ContractException e) {
            jsonObject.put("code", -1);
            jsonObject.put("msg", e.getMessage());
            return jsonObject;
        }
        // byte[] 转为json array
        String data = new String(res, StandardCharsets.UTF_8);
        JSONArray jsonArray = JSON.parseArray(data);
        List<CerImage> list = new ArrayList<CerImage>();
        // 遍历json数组

        for (Object object : jsonArray) {

            JSONObject jsonObject2 = (JSONObject) JSON.toJSON(object);
            // 提取 详细信息
            JSONObject jsonObject3 = jsonObject2.getJSONObject("Record");
            // json 转为实体对象
            CerImage cerImage = JSON.parseObject(String.valueOf(jsonObject3), CerImage.class);
            //获取对应交易的交易ID和时间戳
            byte[] res1;
            try {
                res1 = contract1.evaluateTransaction("GetTxidAndtime", jsonObject2.getString("Key"));
            } catch (ContractException e) {
                jsonObject.put("code", -1);
                jsonObject.put("msg", e.getMessage());
                return jsonObject;
            }
            String tt = new String(res1, StandardCharsets.UTF_8);
            //  转json
            JSONObject jsonObject4 = JSONObject.parseObject(tt);
            cerImage.setTxid(jsonObject4.getString("txid"));

            cerImage.setTimestamp(jsonObject4.getDate("timestamp"));

            // 加入list
            list.add(cerImage);
        }

        // 按搜索条件 过滤数据
        if (username != null && !username.equals(""))
            list = list.stream().filter(cerimg -> username.equals(cerimg.getUsername())).collect(Collectors.toList());
        if (num != null && !num.equals(""))
            list = list.stream().filter(cerimg -> num.equals(cerimg.getNum())).collect(Collectors.toList());
        if (name != null && !name.equals(""))
            list = list.stream().filter(cerimg -> name.equals(cerimg.getName())).collect(Collectors.toList());
        if (txid != null && !txid.equals(""))
            list = list.stream().filter(cerimg -> txid.equals(cerimg.getTxid())).collect(Collectors.toList());

        // 分页查询
        List<CerImage> list1 = new ArrayList<>();
        for (int i = (page - 1) * limit, j = 1; i < list.size() && j < limit; ++i, ++j) {
            list1.add(list.get(i));
        }
        jsonObject.put("data", list1);
        jsonObject.put("code", 0);
        jsonObject.put("count", list.size());
        jsonObject.put("msg", "数据请求成功！");
        return jsonObject;
    }

    public Contract GetContract(Network network) {
        return network.getContract("certificate");
    }
}
