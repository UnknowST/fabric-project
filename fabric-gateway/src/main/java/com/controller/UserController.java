package com.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.daomain.CerImage;
import com.daomain.Message;
import com.daomain.User;
import com.daomain.UserLogs;
import com.util.PDFUtil;
import com.util.SHA256Util;
import org.hyperledger.fabric.gateway.Contract;
import org.hyperledger.fabric.gateway.ContractException;
import org.hyperledger.fabric.gateway.Network;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeoutException;

@RestController
@RequestMapping("/user")
public class UserController {

    @Resource
    private Contract contract;

    @Resource
    private Network network;

    @Autowired
    private Message message;
    @Autowired
    private SHA256Util sha;
    @Autowired
    private PDFUtil pdfUtil;

    /**
     * 上传图片
     *
     * @param request
     * @param file
     * @return
     */
    @RequestMapping("/upload")
    public Message upload(HttpServletRequest request, @RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            message.setMsg("上传文件为空！");
            message.setMsg("0");
            return message;
        }
        Base64.Encoder encode = Base64.getEncoder();
        String code;
        try {
            code = encode.encodeToString(file.getBytes());
        } catch (IOException e) {
            message.setMsg("文件上传失败");
            message.setErrcode("0");
            return message;
        }

        String Shacode;
        try {
            Shacode = sha.GetSHA256Str(code);
        } catch (NoSuchAlgorithmException e) {
            message.setMsg("文件上传失败");
            message.setErrcode("0");
            return message;
        }
        // request.getSession().setAttribute("filehash",Shacode);

        message.setErrcode("1");
        message.setMsg("文件上传成功");
        message.setData(Shacode);
        return message;
    }

    /**
     * 生成存证保存到数据库
     *
     * @param request
     * @param title
     * @param hashcode
     * @return
     */
    @RequestMapping("/createcer")
    public Message createcer(HttpServletRequest request, String title, String hashcode) {
        User user = (User) request.getSession().getAttribute("user");
        if (user == null) {
            message.setMsg("请先登录！");
            message.setErrcode("-1");
            return message;
        }
        Contract contract1 = GetContract(network);
        byte[] getnum;
        try {
            getnum = contract1.evaluateTransaction("GetNum");
        } catch (ContractException e) {
            System.out.println(e);
            message.setErrcode("0");
            message.setMsg("GetNum合约调用失败，请重试！");
            return message;
        }
        String num1 = new String(getnum, StandardCharsets.UTF_8);
        String num = String.valueOf(Integer.parseInt(num1) + 1);

        byte[] res;
        try {
            res = contract1.submitTransaction("AddInfo", num, user.getUsername(), user.getName(), title, hashcode);
        } catch (ContractException e) {
            message.setErrcode("0");
            message.setMsg("AddInfo合约调用失败，请重试！");
            return message;
        } catch (TimeoutException e) {
            message.setErrcode("0");
            message.setMsg("AddInfo合约调用失败，请重试！");
            return message;
        } catch (InterruptedException e) {
            message.setErrcode("0");
            message.setMsg("AddInfo合约调用失败，请重试！");
            return message;
        }
        message.setErrcode("1");
        message.setMsg("存证成功！");
        return message;
    }

    /**
     * 返回某一个用户的存证列表
     *
     * @param request
     * @param limit
     * @param page
     * @return
     */
    @RequestMapping("/getlist")
    public JSONObject getlist(HttpServletRequest request, Integer limit, Integer page) {
        User user = (User) request.getSession().getAttribute("user");
        Contract contract1 = GetContract(network);
        JSONObject jsonObject = new JSONObject();

        byte[] res;
        try {
            res = contract1.evaluateTransaction("GetByUsername", user.getUsername());
        } catch (ContractException e) {
            jsonObject.put("data", null);
            jsonObject.put("code", -1);
            jsonObject.put("count", 0);
            jsonObject.put("msg", "数据请求失败！");
            return jsonObject;
        }
        String data = new String(res, StandardCharsets.UTF_8);
        // 转 json数组
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
                jsonObject.put("msg", "数据请求失败！");
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

    /**
     * 用户删除存证
     *
     * @param nums
     * @return
     */
    @RequestMapping("deletecer")
    public Message DeleteCer(String[] nums) {
        Contract contract1 = GetContract(network);
        for (String s : nums) {
            try {
                contract1.submitTransaction("DeleteInfo", s);
            } catch (ContractException | TimeoutException | InterruptedException e) {
                message.setMsg("0");
                message.setMsg("删除失败，请重试！");
                return message;
            }
        }
        message.setMsg("success!");
        message.setErrcode("1");
        return message;
    }

    /**
     * 生成证书文件
     *
     * @param cerImage
     * @param response
     * @return
     * @throws IOException
     */

    @RequestMapping("/getcertificate")
    public Message getcertificate(CerImage cerImage , HttpServletResponse response) throws IOException {
        // JSONObject jsonObject = (JSONObject) JSON.toJSON(cerImage);
        Map<String, String> map = new HashMap<>();
        map.put("num", cerImage.getNum());
        map.put("username", cerImage.getUsername());
        map.put("name", cerImage.getName());
        map.put("title", cerImage.getTitle());
        map.put("txid", cerImage.getTxid());
        map.put("hash", cerImage.getHash());
        map.put("timestamp", cerImage.getTimestamp());

        String uuid = UUID.randomUUID().toString().replaceAll("-", "");
        String path = "tmp/" + uuid + ".pdf";
        pdfUtil.template("certi.html", map, path);
        File file = new File(path);
        if (!file.exists()) {
            message.setErrcode("0");
            message.setMsg("证书生成失败，请重试！");
            return message;

        }
        message.setErrcode("1");
        message.setMsg("证书生成成功！");
        message.setData(path);
        return message;
    }

    /**
     * 游览器响应文件下载
     *
     * @param path
     * @param filename
     * @param response
     * @return
     * @throws IOException
     */
    @RequestMapping("/getfile")
    public Message GetFile(String path, String filename, HttpServletResponse response) throws IOException {
        File file = new File(path);
        if (!file.exists()) {
            System.out.println("文件不存在！");
            message.setErrcode("0");
            message.setMsg("证书生成失败，请重试！");
            return message;

        }
        //设置响应头，控制浏览器下载该文件 并将文件还原为原始的名称
        try {
            response.setHeader("content-disposition", "attachment;filename=" + URLEncoder.encode(filename + ".pdf", "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        //读取要下载的文件，保存到文件输入流
        FileInputStream in = null;
        try {
            in = new FileInputStream(path);
        } catch (FileNotFoundException e) {
            message.setErrcode("0");
            message.setMsg("证书生成失败，请重试！");
            return message;
        }
        //创建输出流
        OutputStream out = response.getOutputStream();
        //创建缓冲区
        byte buffer[] = new byte[1024];
        int len = 0;
        //循环将输入流中的内容读取到缓冲区当中
        while ((len = in.read(buffer)) > 0) {
            //输出缓冲区的内容到浏览器，实现文件下载
            out.write(buffer, 0, len);
        }
        //关闭文件输入流
        in.close();
        //关闭输出流
        out.close();
        // 删除中间 文件
        file.delete();
        message.setErrcode("1");
        message.setMsg("证书生成成功！");

        return message;
    }

    /**
     * 检查用户原密码
     *
     * @param password
     * @param request
     * @return
     * @throws NoSuchAlgorithmException
     */
    @RequestMapping("checkpass")
    public Message checkpass(String password, HttpServletRequest request) throws NoSuchAlgorithmException {
        User user = (User) request.getSession().getAttribute("user");
        String code = sha.GetSHA256Str(password);
        if (code.equals(user.getPassword()) == true) {
            message.setErrcode("1");
            message.setMsg("原密码正确！");
        } else {
            message.setErrcode("0");
            message.setMsg("原密码输入错误！");
        }
        return message;
    }

    /**
     * 修改密码
     *
     * @param password
     * @param request
     * @return
     * @throws NoSuchAlgorithmException
     */
    @RequestMapping("modifpass")
    public Message modifpass(String password, HttpServletRequest request) throws NoSuchAlgorithmException {
        String code = sha.GetSHA256Str(password);
        User user = (User) request.getSession().getAttribute("user");

        try {
            byte[] res = contract.submitTransaction("ModifPassw", user.getUsername(), code);
        } catch (ContractException | TimeoutException | InterruptedException e) {
            message.setErrcode("0");
            message.setMsg("密码修改失败，请重试！");
            return message;
        }
        // 替换原来seesion的密码
        user.setPassword(code);
        request.getSession().setAttribute("user", user);
        message.setErrcode("1");
        message.setMsg("密码修改成功！");
        return message;
    }

    /**
     * 返回用户账号操作记录
     *
     * @param request
     * @param limit
     * @param page
     * @return
     */
    @RequestMapping("/history")
    public JSONObject history(HttpServletRequest request, Integer limit, Integer page) {
        User user = (User) request.getSession().getAttribute("user");
        JSONObject jsonObject = new JSONObject();
        if (user == null) {
            jsonObject.put("data", null);
            jsonObject.put("code", -1);
            jsonObject.put("count", 0);
            jsonObject.put("msg", "请先登录！");
            return jsonObject;
        }
        byte[] res;
        try {
            res = contract.evaluateTransaction("GetUserHistory", user.getUsername());
        } catch (ContractException e) {
            jsonObject.put("data", null);
            jsonObject.put("code", -1);
            jsonObject.put("count", 0);
            jsonObject.put("msg", "请求数据失败！");
            return jsonObject;
        }
        String data = new String(res, StandardCharsets.UTF_8);
        // 转jsonarray
        JSONArray jsonArray = JSON.parseArray(data);
        List<UserLogs> list = new ArrayList<>();
        // 遍历json array
        for (Object object : jsonArray) {
            UserLogs userLogs = JSON.parseObject(String.valueOf(object), UserLogs.class);
            list.add(userLogs);
        }
        int len = list.size() - 1;
        String oldpass = list.get(len).getRecord().getPassword();
        if (list.get(0).getIsdelete().equals("true") == true) list.get(0).setMsg("删除账号。");
        list.get(len).setMsg("注册账号。");
        for (int i = len - 1; i >= 1; i--) {
            if (list.get(i).getRecord().getPassword().equals(oldpass) == false) {
                list.get(i).setMsg("修改密码。");
                oldpass = list.get(i).getRecord().getPassword();
            }
            if (list.get(i).getIsdelete().equals("true") == true) list.get(i).setMsg("删除账号。");
        }
        // 分页查询
        List<UserLogs> list1 = new ArrayList<>();
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
     * 注销账号
     *
     * @param request
     * @return
     */
    @RequestMapping("/deleteuser")
    public Message deleteuser(HttpServletRequest request) {
        User user = (User) request.getSession().getAttribute("user");
        if (user == null) {
            message.setErrcode("0");
            message.setMsg("请先登录!");
            return message;
        }
        // 删除账号
        try {
            byte[] result = contract.submitTransaction("DeleteUser", user.getUsername());
        } catch (ContractException | TimeoutException | InterruptedException e) {
            message.setErrcode("0");
            message.setMsg("账号注销失败！");
            return message;
        }
        // 删除与账号关联的 存证列表
        Contract contract1 = GetContract(network);
        byte[] res;
        try {
            // 获取某一账号下的存证列表
            res = contract1.evaluateTransaction("GetByUsername", user.getUsername());
        } catch (ContractException e) {
            message.setErrcode("0");
            message.setMsg("账号注销失败！");
            return message;
        }
        String data = new String(res, StandardCharsets.UTF_8);
        JSONArray jsonArray = JSON.parseArray(data);
        for (Object object : jsonArray) {
            JSONObject jsonObject = (JSONObject) JSON.toJSON(object);
            String key = jsonObject.getString("Key");
            try {
                // 删除存证
                contract1.evaluateTransaction("DeleteInfo", key);
            } catch (ContractException e) {
                message.setErrcode("0");
                message.setMsg(e.getMessage());
                return message;
            }
        }
        message.setErrcode("1");
        message.setMsg("success!");
        return message;
    }

    public Contract GetContract(Network network) {
        return network.getContract("certificate");
    }

}
