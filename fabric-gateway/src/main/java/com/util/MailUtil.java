package com.util;

import com.sun.mail.util.MailSSLSocketFactory;
import org.springframework.stereotype.Component;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.security.GeneralSecurityException;
import java.util.Properties;

@Component
public class MailUtil {
    /*邮件发送的工具类*/
    /*对应的参数分别是
     * 收件人地址
     * 邮件内容
     * 邮件的标题
     * */
    public int sendMail(String receiver, String context, String title) throws MessagingException {
        // 1.创建一个程序与邮件服务器会话对象 Session  解决linux上发送邮件失败问题，需要下面的配置
        MailSSLSocketFactory sf = null;
        try {
            sf = new MailSSLSocketFactory();
        } catch (GeneralSecurityException e) {
            e.printStackTrace();
        }
        // 设置信任所有的主机，linux服务器设置的ssl导致不能进行发送邮件功能
        sf.setTrustAllHosts( true );


        Properties props = new Properties();
        props.setProperty("mail.smtp.auth", "true");
        props.setProperty("mail.transport.protocol", "smtp");
        props.put("mail.smtp.host", "smtp.163.com"); // smtp服务器地址
        props.setProperty("mail.smtp.port", "465");
        props.put("mail.smtp.ssl.enable", true);

        int i = 0;
        try {
            Session session = Session.getInstance(props);
            session.setDebug(true);

            Message msg = new MimeMessage(session);
            msg.setSubject(title); // 邮件标题

            // msg.setText("你好!这是我的第一个javamail程序---WQ"+code);      //邮件内容
            msg.setContent(context, "text/html;charset=utf-8");
            msg.setFrom(new InternetAddress("shoufix_cn@163.com")); // 发件人邮箱(我的163邮箱)
            msg.setRecipient(Message.RecipientType.TO, new InternetAddress(receiver)); // 收件人邮箱(我的QQ邮箱)
            msg.saveChanges();
            Transport transport = session.getTransport();
            transport.connect("shoufix_cn@163.com", "JHOQVFKLSFCPSLHB"); // 发件人邮箱,授权码(可以在邮箱设置中获取到授权码的信息)

            transport.sendMessage(msg, msg.getAllRecipients());

            transport.close();
            i = 1;
        } catch (MessagingException e) {
            e.printStackTrace();
        }
        return i;
    }


}