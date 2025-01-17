package com.dtstack.engine.flink.util;

import com.dtstack.engine.common.util.MathUtil;
import com.dtstack.engine.flink.constrant.ConfigConstrant;
import org.apache.flink.client.deployment.ClusterSpecification;
import org.apache.flink.configuration.*;

import java.util.Properties;

/**
 * company: www.dtstack.com
 * author: toutian
 * create: 2018/7/5
 */
public class FlinkConfUtil {

    private final static int JOBMANAGER_HEAP_MB = 1024;
    private final static int TASKMANAGER_HEAP_MB = 1568;
    private final static int TASKMANAGER_NUMBEROFTASKSLOTS = 1;

    public static ClusterSpecification createClusterSpecification(Configuration configuration, Properties confProperties) {
        int jobmanagerMemoryMb = JOBMANAGER_HEAP_MB;
        int taskmanagerMemoryMb = TASKMANAGER_HEAP_MB;
        int numberOfTaskSlots = TASKMANAGER_NUMBEROFTASKSLOTS;

        if (confProperties != null) {
            if (confProperties.containsKey(ConfigConstrant.JOBMANAGER_MEMORY_MB)) {
                jobmanagerMemoryMb = MathUtil.getIntegerVal(confProperties.get(ConfigConstrant.JOBMANAGER_MEMORY_MB));
                if (jobmanagerMemoryMb < ConfigConstrant.MIN_JM_MEMORY) {
                    jobmanagerMemoryMb = ConfigConstrant.MIN_JM_MEMORY;
                }
            }

            if (confProperties.containsKey(ConfigConstrant.TASKMANAGER_MEMORY_MB)) {
                taskmanagerMemoryMb = MathUtil.getIntegerVal(confProperties.get(ConfigConstrant.TASKMANAGER_MEMORY_MB));
                if (taskmanagerMemoryMb < ConfigConstrant.MIN_TM_MEMORY) {
                    taskmanagerMemoryMb = ConfigConstrant.MIN_TM_MEMORY;
                }
            }

            if (confProperties.containsKey(ConfigConstrant.SLOTS)) {
                numberOfTaskSlots = MathUtil.getIntegerVal(confProperties.get(ConfigConstrant.SLOTS));
            }
        } else {
            jobmanagerMemoryMb = configuration.getInteger(JobManagerOptions.JOB_MANAGER_HEAP_MEMORY_MB, JOBMANAGER_HEAP_MB);
            taskmanagerMemoryMb = configuration.getInteger(TaskManagerOptions.TASK_MANAGER_HEAP_MEMORY_MB, TASKMANAGER_HEAP_MB);
            numberOfTaskSlots = configuration.getInteger(ConfigConstants.TASK_MANAGER_NUM_TASK_SLOTS, TASKMANAGER_NUMBEROFTASKSLOTS);
        }
        return new ClusterSpecification.ClusterSpecificationBuilder()
                .setMasterMemoryMB(jobmanagerMemoryMb)
                .setTaskManagerMemoryMB(taskmanagerMemoryMb)
                .setSlotsPerTaskManager(numberOfTaskSlots)
                .createClusterSpecification();
    }

    private static void configJobmanagerMemory(Configuration configuration, long jobmanagerMemoryMB) {
        String memorySize = String.format("%smb", String.valueOf(jobmanagerMemoryMB));
        configuration.setString(JobManagerOptions.JOB_MANAGER_HEAP_MEMORY, memorySize);
    }

    private static void configTaskmanagerMemory(Configuration configuration, long taskManagerMemoryMB) {
        String memorySize = String.format("%smb", String.valueOf(taskManagerMemoryMB));
        configuration.set(TaskManagerOptions.TOTAL_PROCESS_MEMORY, MemorySize.parse(memorySize));
    }

    private static void configTaskmanagerNumberOfTaskSlots(Configuration configuration, int slots) {
        configuration.set(TaskManagerOptions.NUM_TASK_SLOTS, slots);
    }

    public static void setResourceConfig(Configuration configuration, Properties confProperties) {
        ClusterSpecification clusterSpecification = FlinkConfUtil.createClusterSpecification(configuration, confProperties);
        configJobmanagerMemory(configuration, clusterSpecification.getMasterMemoryMB());
        configTaskmanagerMemory(configuration, clusterSpecification.getTaskManagerMemoryMB());
        configTaskmanagerNumberOfTaskSlots(configuration, clusterSpecification.getSlotsPerTaskManager());
    }

}