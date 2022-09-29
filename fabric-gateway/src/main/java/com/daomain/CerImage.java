package com.daomain;

import com.fasterxml.jackson.annotation.JsonFormat;
import org.springframework.format.annotation.DateTimeFormat;

import javax.xml.crypto.Data;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class CerImage {

    String num, username, name, title, hash, txid;
    //@DateTimeFormat(pattern = "yyyy-MM-dd hh:mm:ss")//页面写入数据库时格式化
    //@JsonFormat(pattern = "yyyy-MM-dd hh:mm:ss", timezone = "GMT+8")  //查询输出时格式转换
    String timestamp;



    public String getNum() {
        return num;
    }

    public void setNum(String num) {
        this.num = num;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public String getTxid() {
        return txid;
    }

    public void setTxid(String txid) {
        this.txid = txid;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        //设置为东八区
        sdf.setTimeZone(TimeZone.getTimeZone("Asia/Shanghai"));
        this.timestamp = sdf.format(timestamp);
    }
    public void setTimestamp(String timestamp){
        this.timestamp=timestamp;
    }

    @Override
    public String toString() {
        return "CerImage{" +
                "num='" + num + '\'' +
                ", username='" + username + '\'' +
                ", name='" + name + '\'' +
                ", title='" + title + '\'' +
                ", hash='" + hash + '\'' +
                ", txid='" + txid + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}
