package com.dtstack.engine.flink.plugininfo;

import com.dtstack.engine.common.exception.RdosDefineException;
import com.dtstack.engine.common.util.PublicUtil;
import com.dtstack.engine.common.JarFileInfo;
import com.dtstack.engine.common.JobClient;
import com.dtstack.engine.flink.FlinkConfig;
import com.dtstack.engine.flink.constrant.ConfigConstrant;
import com.google.common.base.Charsets;
import com.google.common.collect.Lists;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.List;


/**
 * 获取插件的工具类
 * 包括获得jar路径,获取plugin的类名称
 * Date: 2017/8/2
 * Company: www.dtstack.com
 * @author xuchao
 */

public class SqlPluginInfo {

    private static final Logger logger = LoggerFactory.getLogger(SqlPluginInfo.class);

    private String localSqlPluginDir;

    private String remoteSqlPluginDir;

    private String pluginLoadMode;

    private SqlPluginInfo(){

    }

    public static SqlPluginInfo create(FlinkConfig flinkConfig){
        SqlPluginInfo pluginInfo = new SqlPluginInfo();
        pluginInfo.init(flinkConfig);
        return pluginInfo;
    }

    private void init(FlinkConfig flinkConfig){
        this.remoteSqlPluginDir = getSqlPluginDir(flinkConfig.getRemotePluginRootDir());
        this.localSqlPluginDir = getSqlPluginDir(flinkConfig.getFlinkPluginRoot());
        this.pluginLoadMode = flinkConfig.getPluginLoadMode();
        logger.info("---------local sqlplugin dir is:" + localSqlPluginDir);
        logger.info("---------remote sqlplugin dir is:" + remoteSqlPluginDir);
    }

    public String getSqlPluginDir(String pluginRoot){
        return pluginRoot + ConfigConstrant.SP + ConfigConstrant.SQLPLUGIN_DIR;
    }

    public List<String> buildExeArgs(JobClient jobClient) throws IOException {
        List<String> args = Lists.newArrayList();
        args.add("-sql");
        args.add(URLEncoder.encode(jobClient.getSql(), Charsets.UTF_8.name()));

        args.add("-name");
        args.add(jobClient.getJobName());

        args.add("-mode");
        args.add("yarnPer");

        args.add("-pluginLoadMode");
        args.add(pluginLoadMode);


        args.add("-localSqlPluginPath");
        args.add(localSqlPluginDir);

        args.add("-remoteSqlPluginPath");
        args.add(remoteSqlPluginDir);

        args.add("-confProp");
        String confPropStr = PublicUtil.objToString(jobClient.getConfProperties());
        confPropStr = URLEncoder.encode(confPropStr, Charsets.UTF_8.name());
        args.add(confPropStr);
        return args;
    }

    public JarFileInfo createCoreJarInfo(){
        JarFileInfo jarFileInfo = new JarFileInfo();
        String coreJarFileName = getCoreJarFileName();
        String jarFilePath  = localSqlPluginDir + File.separator + coreJarFileName;
        jarFileInfo.setJarPath(jarFilePath);
        return jarFileInfo;
    }

    private String getCoreJarFileName (){
        String coreJarFileName = null;
        File pluginDir = new File(localSqlPluginDir);
        if (pluginDir.exists() && pluginDir.isDirectory()){
            File[] jarFiles = pluginDir.listFiles(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    return name.toLowerCase().startsWith(ConfigConstrant.FLINKSQl_CORE_JAR_PREFIX) && name.toLowerCase().endsWith(".jar");
                }
            });

            if (jarFiles != null && jarFiles.length > 0){
                coreJarFileName = jarFiles[0].getName();
            }
        }

        if (StringUtils.isEmpty(coreJarFileName)){
            throw new RdosDefineException("Can not find core jar file in sqlPlugin path: " + localSqlPluginDir);
        }

        return coreJarFileName;
    }

}
