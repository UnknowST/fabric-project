package com.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.daomain.Message;
import com.daomain.User;
import com.util.MailUtil;
import com.util.SHA256Util;
import org.hyperledger.fabric.gateway.Contract;
import org.hyperledger.fabric.gateway.ContractException;
import org.hyperledger.fabric.gateway.Network;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.imageio.ImageIO;
import javax.mail.MessagingException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.List;
import java.util.concurrent.TimeoutException;

@RestController
@RequestMapping("/user")
public class LoginCotroller {
    @Resource
    private Contract contract;

    @Resource
    private Network network;

    @Autowired
    private Message message;
    @Autowired
    private SHA256Util sha;
    @Autowired
    private MailUtil mailUtil;

    /**
     * 生成验证码 并保存session
     * @param request
     * @param response
     * @throws ServletException
     * @throws IOException
     */
    @RequestMapping("/checkcode")

    public void test1(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // 服务器通知浏览器不要缓存
        response.setHeader("pragma", "no-cache");
        response.setHeader("cache-control", "no-cache");
        response.setHeader("expires", "0");

        // 在内存中创建一个长80，宽30的图片，默认黑色背景
        // 参数一：长
        // 参数二：宽
        // 参数三：颜色
        int width = 80;
        int height = 40;
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        // 获取画笔
        Graphics g = image.getGraphics();
        // 设置画笔颜色为灰色
        g.setColor(Color.GRAY);
        // 填充图片
        g.fillRect(0, 0, width, height);

        // 产生4个随机验证码，12Ey
        String checkCode = getCheckCode();
        // 将验证码放入HttpSession中
        request.getSession().setAttribute("CHECKCODE_SERVER", checkCode);
        // 设置seesion有效时间为10分钟
        request.getSession().setMaxInactiveInterval(10*60);

        // 设置画笔颜色为黄色
        g.setColor(Color.YELLOW);
        // 设置字体的小大
        g.setFont(new Font("黑体", Font.BOLD, 24));
        // 向图片上写入验证码
        g.drawString(checkCode, 15, 25);

        // 将内存中的图片输出到浏览器
        // 参数一：图片对象
        // 参数二：图片的格式，如PNG,JPG,GIF
        // 参数三：图片输出到哪里去
        ImageIO.write(image, "PNG", response.getOutputStream());
    }

    /**
     * 产生4位随机字符串
     */
    private String getCheckCode() {
        String base = "0123456789ABCDEFGabcdefg";
        int size = base.length();
        Random r = new Random();
        StringBuffer sb = new StringBuffer();
        for (int i = 1; i <= 4; i++) {
            // 产生0到size-1的随机值
            int index = r.nextInt(size);
            // 在base字符串中获取下标为index的字符
            char c = base.charAt(index);
            // 将c放入到StringBuffer中去
            sb.append(c);
        }
        return sb.toString();
    }


    /**
     * 用户登录
     * @param captcha
     * @param username
     * @param password
     * @param request
     * @return
     * @throws IOException
     * @throws ClassNotFoundException
     * @throws NoSuchAlgorithmException
     */
    @RequestMapping("/login")
    public Message test1(String captcha ,String username, String password, HttpServletRequest request) throws IOException, ClassNotFoundException, NoSuchAlgorithmException {

        //获取验证码session
        String code = (String) request.getSession().getAttribute("CHECKCODE_SERVER");
        /*转为小写*/
        code = code.toLowerCase();
        captcha = captcha.toLowerCase();
        if(!code.equals(captcha)) {
            message.setErrcode("0");
            message.setMsg("验证码错误！");
            return message;
        }

        byte[] QueryResult;
        try {
            QueryResult = contract.evaluateTransaction("QueryUser", username);
        } catch (ContractException e) {
            message.setErrcode("500");
            message.setMsg(e.getMessage());
            return message;
        }
        String userstr = new String(QueryResult, StandardCharsets.UTF_8);
        User user = JSON.parseObject(userstr, User.class);
        String encpass = sha.GetSHA256Str(password);
        if (user.getPassword().compareTo(encpass) == 0) {
            message.setErrcode("200");
            message.setMsg("success!");
            request.getSession().setAttribute("user", user);

        } else {
            message.setErrcode("500");
            message.setMsg("密码或账号不对!");
        }
        return message;
    }

    /**
     * 用户注册
     * @param password
     * @param name
     * @param email
     * @param captcha
     * @param request
     * @return
     * @throws NoSuchAlgorithmException
     * @throws ContractException
     */
    @RequestMapping("/register")
    public Message test2(String password, String name,  String email,String captcha,HttpServletRequest request) throws NoSuchAlgorithmException, ContractException {
        // 先检查验证码是否正确
        String code= (String) request.getSession().getAttribute("emailcode");
        if(code==null) {
            message.setErrcode("3");
            message.setMsg("多次点击！");
            return message;
        }
        if(code.compareTo(captcha)!=0){
            message.setErrcode("0");
            message.setMsg("验证码错误！");
            return message;
        }
        request.getSession().removeAttribute("emailcode");
        String encpass = sha.GetSHA256Str(password);

        byte[] reusername=contract.evaluateTransaction("GetNum");
        String username1=new String(reusername,StandardCharsets.UTF_8);
        String username=String.valueOf(Integer.valueOf(username1)+1);

        // 注册成功 讲用户的 username 发送到用户邮箱
        String title="区块链照片存证平台";
        String context= "您好!</br>&nbsp;&nbsp;&nbsp;您的账户ID为：<span style='color:red;font-size: 23px;'>"+username+"</span>,之后你可使用ID和密码登录，请妥善保管用户ID,避免遗失。";
        try {
            mailUtil.sendMail(email,context,title);
        } catch (MessagingException e) {
            message.setErrcode("0");
            message.setMsg("注册失败，请重试！");
            return message;
        }
        byte[] result;
        try {
            result = contract.submitTransaction("AddUser", username, encpass, name, email);
        } catch (TimeoutException e) {
            message.setErrcode("0");
            message.setMsg("链码调用失败！");
            return message;
        } catch (InterruptedException e) {
            message.setErrcode("0");
            message.setMsg(e.getMessage());
            return message;
        }

        message.setErrcode("1");
        message.setMsg("success!");
        return message;
    }

    /**
     * 发送邮件 验证码
     * @param email
     * @param request
     * @return
     * @throws ContractException
     */
    @RequestMapping("/getemailcode")
    public Message Getcode(String email,HttpServletRequest request) throws ContractException {
        // 先检查邮箱是否已经注册
        if(CheckEmail(email)==0){
            message.setErrcode("0");
            message.setMsg("该邮件地址已经注册!");
            return message;
        }
        String code=getCheckCode();
        String title="区块链照片存证平台";
        String context= "您好!</br>&nbsp;&nbsp;&nbsp;您的账户注册验证码为：<span style='color:red;font-size: 23px;'>"+code+"</span>,有效时间十分钟，请尽快注册";
        try {
            mailUtil.sendMail(email,context,title);
        } catch (MessagingException e) {
            message.setErrcode("0");
            message.setMsg("邮件发送失败，请检查您的邮箱!");
            return message;
        }
        request.getSession().setAttribute("emailcode",code);
        // 设置seesion有效时间为10分钟
        request.getSession().setMaxInactiveInterval(10*60);
        message.setErrcode("1");
        message.setMsg("邮件已发送，请检查您的邮箱！");
        return message;
    }

    /**
     * 退出登录
     * @param request
     */
    @RequestMapping("/exit")
    public void ExitLogin(HttpServletRequest request){
        HttpSession session = request.getSession();
        session.invalidate();
    }

    /**
     * 检查用户邮箱是否已经注册
     * @param email
     * @return
     * @throws ContractException
     */
    public int CheckEmail(String email) throws ContractException {
        byte[] queryAResultBefore = contract.evaluateTransaction("QueryAllUser");
        String restr = new String(queryAResultBefore, StandardCharsets.UTF_8);
        JSONArray jsonArray = JSON.parseArray(restr);
        List<User> list = new ArrayList<User>();
        for (Object object : jsonArray) {
            JSONObject jsonObject = (JSONObject) JSON.toJSON(object);
            JSONObject jsonObject1 = jsonObject.getJSONObject("Record");
            list.add(JSON.parseObject(String.valueOf(jsonObject1), User.class));
        }
        for(User user:list){
            if(user.getEmail().compareTo(email)==0) {return 0;}
        }
        return 1;
    }

    /**
     * 返回区块链中最新的username
     * @return
     * @throws ContractException
     */
    public Integer GetLastUsername() throws ContractException {
        byte[] queryAResultBefore = contract.evaluateTransaction("QueryAllUser");
        String restr = new String(queryAResultBefore, StandardCharsets.UTF_8);
        JSONArray jsonArray = JSON.parseArray(restr);
        List<User> list = new ArrayList<User>();
        for (Object object : jsonArray) {
            JSONObject jsonObject = (JSONObject) JSON.toJSON(object);
            JSONObject jsonObject1 = jsonObject.getJSONObject("Record");
            list.add(JSON.parseObject(String.valueOf(jsonObject1), User.class));
        }

        Collections.sort(list, new Comparator<User>() {
            @Override
            public int compare(User o1, User o2) {
                return o1.getUsername().compareTo(o2.getUsername());
            }
        });
        return Integer.valueOf(list.get(list.size() - 1).getUsername());
    }

}



