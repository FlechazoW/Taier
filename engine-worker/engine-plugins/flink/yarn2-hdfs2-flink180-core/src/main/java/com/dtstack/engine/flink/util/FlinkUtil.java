package com.dtstack.engine.flink.util;

import com.dtstack.engine.base.filesystem.FilesystemManager;
import com.dtstack.engine.common.enums.ComputeType;
import com.dtstack.engine.common.enums.EJobType;
import com.dtstack.engine.flink.constrant.ConfigConstrant;
import com.dtstack.engine.flink.enums.FlinkYarnMode;
import com.dtstack.engine.base.enums.ClassLoaderType;
import org.apache.commons.lang3.StringUtils;
import org.apache.flink.client.program.JobWithJars;
import org.apache.flink.client.program.PackagedProgram;
import org.apache.flink.client.program.ProgramInvocationException;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.configuration.CoreOptions;
import org.apache.flink.runtime.jobgraph.SavepointRestoreSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Properties;

/**
 * Reason:
 * Date: 2017/2/21
 * Company: www.dtstack.com
 * @author xuchao
 */

public class FlinkUtil {

    private static final Logger logger = LoggerFactory.getLogger(FlinkUtil.class);


    public static PackagedProgram buildProgram(String jarPath, List<URL> classpaths, EJobType jobType,
                                               String entryPointClass, String[] programArgs,
                                               SavepointRestoreSettings spSetting, FilesystemManager filesystemManager,
                                               Configuration configuration)
            throws IOException, ProgramInvocationException {
        if (jarPath == null) {
            throw new IllegalArgumentException("The program JAR file was not specified.");
        }
        File jarFile = new File(jarPath);

        String classloaderCache = configuration.getString(ClassLoaderType.CLASSLOADER_DTSTACK_CACHE, ClassLoaderType.CLASSLOADER_DTSTACK_CACHE_TRUE);
        configuration.setString(ClassLoaderType.CLASSLOADER_DTSTACK_CACHE, classloaderCache);

        String append = configuration.getString(CoreOptions.ALWAYS_PARENT_FIRST_LOADER_PATTERNS_ADDITIONAL);
        if (jobType == EJobType.SQL || jobType == EJobType.SYNC) {
            String dtstackAppend = "com.fasterxml.jackson.";
            if (StringUtils.isNotEmpty(append)) {
                dtstackAppend = dtstackAppend + ";" + append;
            }
            configuration.setString(CoreOptions.ALWAYS_PARENT_FIRST_LOADER_PATTERNS_ADDITIONAL, dtstackAppend);
        }

        // Get assembler class
        PackagedProgram program = entryPointClass == null ?
                new PackagedProgram(jarFile, classpaths, configuration, programArgs) :
                new PackagedProgram(jarFile, classpaths, configuration, entryPointClass, programArgs);

        program.setSavepointRestoreSettings(spSetting);

        return program;
    }

    public static File downloadJar(String remotePath, String localDir, FilesystemManager filesystemManager, boolean localPriority) throws IOException {
        if(localPriority){
            //如果不是http 或者 hdfs协议的从本地读取
            File localFile = new File(remotePath);
            if(localFile.exists()){
                return localFile;
            }
        }

        String localJarPath = FlinkUtil.getTmpFileName(remotePath, localDir);
        File downloadFile = filesystemManager.downloadFile(remotePath, localJarPath);
        logger.info("downloadFile remotePath:{} localJarPath:{}", remotePath, localJarPath);

        URL jarFileUrl;

        try {
            jarFileUrl = downloadFile.getAbsoluteFile().toURI().toURL();
        } catch (MalformedURLException e1) {
            throw new IllegalArgumentException("The jar file path is invalid.");
        }

        JobWithJars.checkJarFile(jarFileUrl);

        return downloadFile;
    }

    private static String getTmpFileName(String fileUrl, String toPath){
        String fileName = StringUtils.substringAfterLast(fileUrl, File.separator);
        String tmpFileName = toPath  + File.separator + fileName;
        return tmpFileName;
    }

    /**
     *
     * FIXME 仅针对sql执行方式,暂时未找到区分设置source,transform,sink 并行度的方式
     * 设置job运行的并行度
     * @param properties
     */
    public static int getEnvParallelism(Properties properties){
        String parallelismStr = properties.getProperty(ConfigConstrant.SQL_ENV_PARALLELISM);
        return StringUtils.isNotBlank(parallelismStr)?Integer.parseInt(parallelismStr):1;
    }


    /**
     * 针对MR类型整个job的并发度设置
     * @param properties
     * @return
     */
    public static int getJobParallelism(Properties properties){
        String parallelismStr = properties.getProperty(ConfigConstrant.MR_JOB_PARALLELISM);
        return StringUtils.isNotBlank(parallelismStr)?Integer.parseInt(parallelismStr):1;
    }


    /**
     * get task run mode
     * @param properties
     * @return
     */
    public static FlinkYarnMode getTaskRunMode(Properties properties, ComputeType computeType){
        String modeStr = properties.getProperty(ConfigConstrant.FLINK_TASK_RUN_MODE_KEY);

        if (StringUtils.isEmpty(modeStr)){
            if (ComputeType.STREAM == computeType) {
                return FlinkYarnMode.PER_JOB;
            } else {
                return FlinkYarnMode.SESSION;
            }
        }
        return FlinkYarnMode.mode(modeStr);
    }

}