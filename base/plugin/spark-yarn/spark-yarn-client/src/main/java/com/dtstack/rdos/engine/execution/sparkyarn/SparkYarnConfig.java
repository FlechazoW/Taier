package com.dtstack.rdos.engine.execution.sparkyarn;


import com.google.common.base.Strings;
import org.apache.commons.lang3.StringUtils;

import java.util.Map;

/**
 * Created by softfly on 17/8/10.
 */
public class SparkYarnConfig {

    private static final String DEFAULT_SPARK_YARN_ARCHIVE = "%s/sparkjars/jars";

    private static final String DEFAULT_SPARK_SQL_PROXY_JAR_PATH = "%s/user/spark/spark-0.0.1-SNAPSHOT.jar";

    private static final String DEFAULT_SPARK_PYTHON_EXTLIBPATH = "%s/pythons/pyspark.zip,/pythons/py4j-0.10.4-src.zip";

    private static final String DEFAULT_SPARK_SQL_PROXY_MAINCLASS = "com.dtstack.sql.main.SqlProxy";

    private static final String HDFS_FLAG = "hdfs";

    private String typeName;

    private String sparkYarnArchive;

    private String sparkSqlProxyPath;

    private String sparkSqlProxyMainClass;

    private String sparkPythonExtLibPath;

    private String md5sum;

    /**如果不是使用默认的配置---需要设置配置文件所在的hdfs路径*/
    private String confHdfsPath;

    private Map<String, Object> hadoopConf;

    private Map<String, Object> yarnConf;

    private String defaultFS;

    private String hadoopUserName;

    private String queue;

    public String getTypeName() {
        return typeName;
    }

    public void setTypeName(String typeName) {
        this.typeName = typeName;
    }

    public String getSparkYarnArchive() {
        if(Strings.isNullOrEmpty(sparkYarnArchive)){
            return String.format(DEFAULT_SPARK_YARN_ARCHIVE, defaultFS);
        }

        if(!sparkYarnArchive.trim().startsWith(HDFS_FLAG)){
            sparkYarnArchive = sparkYarnArchive.trim();
            sparkYarnArchive = defaultFS + sparkYarnArchive;
        }

        return sparkYarnArchive;
    }

    public void setSparkYarnArchive(String sparkYarnArchive) {
        this.sparkYarnArchive = sparkYarnArchive;
    }

    public String getSparkSqlProxyPath() {
        if(Strings.isNullOrEmpty(sparkSqlProxyPath)){
            return String.format(DEFAULT_SPARK_SQL_PROXY_JAR_PATH, defaultFS);
        }

        if(!sparkSqlProxyPath.trim().startsWith(HDFS_FLAG)){
            sparkSqlProxyPath = sparkSqlProxyPath.trim();
            sparkSqlProxyPath = defaultFS + sparkSqlProxyPath;
        }

        return sparkSqlProxyPath;
    }

    public void setSparkSqlProxyPath(String sparkSqlProxyPath) {
        this.sparkSqlProxyPath = sparkSqlProxyPath;
    }

    public String getSparkSqlProxyMainClass() {

        if(Strings.isNullOrEmpty(sparkSqlProxyMainClass)){
            return DEFAULT_SPARK_SQL_PROXY_MAINCLASS;
        }

        return sparkSqlProxyMainClass;
    }

    public void setSparkSqlProxyMainClass(String sparkSqlProxyMainClass) {
        this.sparkSqlProxyMainClass = sparkSqlProxyMainClass;
    }

    public String getSparkPythonExtLibPath() {
        if(Strings.isNullOrEmpty(sparkPythonExtLibPath)){
            return String.format(DEFAULT_SPARK_PYTHON_EXTLIBPATH, defaultFS);
        }

        if(!sparkPythonExtLibPath.startsWith(HDFS_FLAG)){
            sparkPythonExtLibPath = sparkPythonExtLibPath.trim();
            sparkPythonExtLibPath = defaultFS + sparkPythonExtLibPath;
        }

        return sparkPythonExtLibPath;
    }

    public void setSparkPythonExtLibPath(String sparkPythonExtLibPath) {
        this.sparkPythonExtLibPath = sparkPythonExtLibPath;
    }

    public Map<String, Object> getHadoopConf() {
        return hadoopConf;
    }

    public void setHadoopConf(Map<String, Object> hadoopConf) {
        this.hadoopConf = hadoopConf;
    }

    public Map<String, Object> getYarnConf() {
        return yarnConf;
    }

    public void setYarnConf(Map<String, Object> yarnConf) {
        this.yarnConf = yarnConf;
    }

    public String getDefaultFS() {
        return defaultFS;
    }

    public void setDefaultFS(String defaultFS) {
        this.defaultFS = defaultFS;
    }

    public String getMd5sum() {
        return md5sum;
    }

    public void setMd5sum(String md5sum) {
        this.md5sum = md5sum;
    }

    public String getConfHdfsPath() {
        return confHdfsPath;
    }

    public void setConfHdfsPath(String confHdfsPath) {
        this.confHdfsPath = confHdfsPath;
    }

    public String getHadoopUserName() {
        return hadoopUserName;
    }

    public void setHadoopUserName(String hadoopUserName) {
        this.hadoopUserName = hadoopUserName;
    }

    public String getQueue() {
        return StringUtils.isBlank(queue)? "default" : queue;
    }

    public void setQueue(String queue) {
        this.queue = queue;
    }
}