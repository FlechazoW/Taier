package com.dtstack.engine.flink.util;


import com.dtstack.engine.base.util.HadoopConfTool;
import org.apache.commons.lang3.StringUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hdfs.DistributedFileSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Map;

/**
 *
 * @author sishu.yss
 *
 */
public class HadoopConf {

    private static final Logger LOG = LoggerFactory.getLogger(HadoopConf.class);

    private final static String HADOOP_CONF = System.getProperty("user.dir")+"/conf/hadoop/";

    private final static String HADOOP_CONF_DIR = System.getenv("HADOOP_CONF_DIR");

    private static volatile Configuration defaultConfiguration = null;

    private static final Object initLock = new Object();

    private Configuration configuration;

    private static void initDefaultConfig(){

        if(defaultConfiguration == null){
            synchronized (initLock){
                if(defaultConfiguration != null){
                    return;
                }

                try {
                    defaultConfiguration = new Configuration();
                    String dir = StringUtils.isNotBlank(HADOOP_CONF_DIR) ? HADOOP_CONF_DIR : HADOOP_CONF;
                    File dirFile = new File(dir);
                    if(!dirFile.exists()){
                        LOG.error("-----------not set env for HADOOP_CONF_DIR!!!");
                    }else if(!dirFile.isDirectory()){
                        LOG.error("HADOOP_CONF_DIR:{} is not dir.", dir);
                    }else{
                        defaultConfiguration.set("fs.hdfs.impl", DistributedFileSystem.class.getName());
                        File[] xmlFileList = new File(dir).listFiles(new FilenameFilter() {
                            @Override
                            public boolean accept(File dir, String name) {
                                if(name.endsWith(".xml")) {
                                    return true;
                                }
                                return false;
                            }
                        });

                        if(xmlFileList != null) {
                            for(File xmlFile : xmlFileList) {
                                defaultConfiguration.addResource(xmlFile.toURI().toURL());
                            }
                        }
                    }
                } catch (Exception e) {
                    LOG.error("",e);
                }
            }
        }
    }

    public HadoopConf(){

    }

    public void initHadoopConf(Map<String, Object> conf){

        if(conf == null || conf.size() == 0){
            //读取环境变量--走默认配置
            configuration = getDefaultConfiguration();
            return;
        }

        configuration = new Configuration();
        HadoopConfTool.setFsHdfsImplDisableCache(configuration);
        conf.keySet().forEach(key ->{
            Object value = conf.get(key);
            if (value instanceof String){
                configuration.set(key, (String) value);
            } else if (value instanceof Boolean){
                configuration.setBoolean(key, (boolean) value);
            }
        });
    }

    public static Configuration getDefaultConfiguration() {
        if(defaultConfiguration == null){
            initDefaultConfig();
        }
        return defaultConfiguration;
    }

    public Configuration getConfiguration(){
        return configuration;
    }

    public String getDefaultFs(){
        return configuration.get("fs.defaultFS");
    }

}
