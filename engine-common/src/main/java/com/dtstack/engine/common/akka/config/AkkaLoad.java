package com.dtstack.engine.common.akka.config;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Map;
import java.util.Properties;

/**
 * @Auther: dazhi
 * @Date: 2021/2/8 11:06 上午
 * @Email:dazhi@dtstack.com
 * @Description:
 */
public class AkkaLoad {

    private final static String COMMON_FILE_PATH = "application-common.properties";
    private final static String PROPERTIES_FILE_PATH = "application.properties";

    private static Config loadCommon(String configPath) {
        Properties properties = new Properties();
        try {
            properties.load(new FileInputStream(configPath));
        } catch (IOException e) {
            e.printStackTrace();
        }

        // 加载 application-common.properties
        Config load = ConfigFactory.load();

        for (Map.Entry<Object, Object> entry : properties.entrySet()) {
            String conf = entry.getKey() + "=" + entry.getValue();
            load = load.withFallback(ConfigFactory.parseString(conf));
        }

        return load;
    }

    public static Config load(String configPath) {
        // 加载 common
        Config config = loadCommon(configPath + COMMON_FILE_PATH);

        // 加载 properties
        return loadProperties(config,configPath + PROPERTIES_FILE_PATH);

    }

    private static Config loadProperties(Config config,String configPath) {
        Properties properties = new Properties();
        try {
            properties.load(new FileInputStream(configPath));
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (config != null) {
            config = config.withFallback(ConfigFactory.parseProperties(properties));
        }

        return config;
    }
}
