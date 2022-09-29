package com.util;

import org.apache.commons.codec.binary.Hex;
import org.springframework.stereotype.Component;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@Component
public class SHA256Util {
    public  String GetSHA256Str(String str) throws NoSuchAlgorithmException {

        MessageDigest messageDigest;

        String encdeStr = "";

        try {

            messageDigest = MessageDigest.getInstance("SHA-256");

            byte[] hash = messageDigest.digest(str.getBytes(StandardCharsets.UTF_8));

            encdeStr = Hex.encodeHexString(hash);

        } catch (NoSuchAlgorithmException e) {

            e.printStackTrace();

        }

        return encdeStr;

    }
}
