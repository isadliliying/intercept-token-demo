package com.wingli.agent.helper;

import org.jsoup.Connection;
import org.jsoup.Jsoup;

import java.io.IOException;

public class UsageStatHelper {

    public static void reportUsage() {
        try {
            String appName = System.getProperty("service.name", "none");
            String statUrl = "https://www.xxx.com/plugin/stat";
            Jsoup.connect(statUrl).data("app", appName).method(Connection.Method.POST).execute();
        } catch (IOException e) {
            //ignore
        }
    }

}
