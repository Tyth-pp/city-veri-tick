package com.city;

import io.jsonwebtoken.Jwts;

import java.util.Base64;

public class TokenTest {
    public static void main(String[] args) {
        // 生成一个适合 HS256 的安全密钥并转为 Base64 字符串
        String secretString = Base64.getEncoder().encodeToString(
                Jwts.SIG.HS256.key().build().getEncoded()
        );
        System.out.println("Generated Secret Key: " + secretString);
        // 将打印出来的字符串复制到你的 application.yml 中即可
    }
}
