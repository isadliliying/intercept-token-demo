package com.wingli.agent.helper;

import com.wingli.agent.helper.util.SpringContextHolder;
import org.apache.tomcat.util.http.MimeHeaders;

public class TokenHelper {

    private static final String HEADER_TOKEN = "Token";

    public static void interceptToken(MimeHeaders headers) {
        String token = headers.getHeader(HEADER_TOKEN);
        if (token == null) return;
        //获取dubbo实例
//        UserDemoApiService userDemoApiService = SpringContextHolder.getReference(UserDemoApiService.class);
        //判断时候是特殊格式的token
        if (token.startsWith("username:")) {
            //解析出用户标识
            String username = token.replace("username:", "");
            //调用dubbo接口，获取可用的token
//            String useFullToken = userDemoApiService.getTokenByUsername(username);
            //将可用token替换进headers
//            headers.getValue(HEADER_TOKEN).setString(useFullToken);
        }
    }

}
