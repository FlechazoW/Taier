package com.dtstack.engine.flink.resource;

import com.dtstack.engine.base.resource.AbstractK8sResourceInfo;
import com.dtstack.engine.common.enums.ComputeType;
import com.dtstack.engine.common.pojo.JudgeResult;
import com.dtstack.engine.common.util.MathUtil;
import com.dtstack.engine.common.JobClient;
import com.dtstack.engine.flink.FlinkClient;
import com.dtstack.engine.flink.enums.FlinkMode;
import com.dtstack.engine.flink.util.FlinkUtil;
import com.dtstack.engine.flink.constrant.ConfigConstrant;
import com.google.common.collect.Lists;
import io.fabric8.kubernetes.api.model.Quantity;
import io.fabric8.kubernetes.api.model.ResourceQuota;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Properties;

/**
 * 用于存储从flink上获取的资源信息
 * Date: 2017/11/24
 * Company: www.dtstack.com
 *
 * @author xuchao
 */

public class FlinkK8sSeesionResourceInfo extends AbstractK8sResourceInfo {

    private static final Logger logger = LoggerFactory.getLogger(FlinkClient.class);

    private final static String PENDING_PHASE = "Pending";

    public int jobmanagerMemoryMb = ConfigConstrant.MIN_JM_MEMORY;
    public int taskmanagerMemoryMb = ConfigConstrant.MIN_JM_MEMORY;
    public int numberTaskManagers = 1;
    public int slotsPerTaskManager = 1;

    private KubernetesClient kubernetesClient;
    private String namespace;
    private Integer allowPendingPodSize;

    public FlinkK8sSeesionResourceInfo() {
    }

    public FlinkK8sSeesionResourceInfo(KubernetesClient kubernetesClient, String namespace, Integer allowPendingPodSize) {
        this.kubernetesClient = kubernetesClient;
        this.namespace = namespace;
        this.allowPendingPodSize = allowPendingPodSize;
    }

    @Override
    public JudgeResult judgeSlots(JobClient jobClient) {
        return judgeResource(jobClient);
    }

    private JudgeResult judgeResource(JobClient jobClient) {
        List<InstanceInfo> instanceInfos = getInstanceInfos(jobClient);
        return judgeResource(instanceInfos);
    }

    public JudgeResult judgeSlotsInNamespace(JobClient jobClient, ResourceQuota resourceQuota) {
        List<InstanceInfo> instanceInfos = getInstanceInfos(jobClient);
        return judgeResourceInNamespace(instanceInfos, resourceQuota);
    }

    public List<InstanceInfo> getInstanceInfos(JobClient jobClient) {
        Properties properties = jobClient.getConfProperties();

        if (properties != null && properties.containsKey(ConfigConstrant.SLOTS)) {
            slotsPerTaskManager = MathUtil.getIntegerVal(properties.get(ConfigConstrant.SLOTS));
        }

        Integer sqlParallelism = FlinkUtil.getEnvParallelism(jobClient.getConfProperties());
        Integer jobParallelism = FlinkUtil.getJobParallelism(jobClient.getConfProperties());
        int parallelism = Math.max(sqlParallelism, jobParallelism);
        if (properties != null && properties.containsKey(ConfigConstrant.CONTAINER)) {
            numberTaskManagers = MathUtil.getIntegerVal(properties.get(ConfigConstrant.CONTAINER));
        }
        numberTaskManagers = Math.max(numberTaskManagers, parallelism);

        if (properties != null && properties.containsKey(ConfigConstrant.JOBMANAGER_MEMORY_MB)) {
            jobmanagerMemoryMb = MathUtil.getIntegerVal(properties.get(ConfigConstrant.JOBMANAGER_MEMORY_MB));
        }
        if (jobmanagerMemoryMb < ConfigConstrant.MIN_JM_MEMORY) {
            jobmanagerMemoryMb = ConfigConstrant.MIN_JM_MEMORY;
        }

        if (properties != null && properties.containsKey(ConfigConstrant.TASKMANAGER_MEMORY_MB)) {
            taskmanagerMemoryMb = MathUtil.getIntegerVal(properties.get(ConfigConstrant.TASKMANAGER_MEMORY_MB));
        }
        if (taskmanagerMemoryMb < ConfigConstrant.MIN_TM_MEMORY) {
            taskmanagerMemoryMb = ConfigConstrant.MIN_TM_MEMORY;
        }

        List<InstanceInfo> instanceInfos = Lists.newArrayList(
                InstanceInfo.newRecord(numberTaskManagers, Quantity.getAmountInBytes(new Quantity(String.valueOf(slotsPerTaskManager))).doubleValue(), Quantity.getAmountInBytes(new Quantity(taskmanagerMemoryMb + "M")).doubleValue()));

        FlinkMode taskRunMode = FlinkUtil.getTaskRunMode(jobClient.getConfProperties(), jobClient.getComputeType());
        boolean isPerJob = ComputeType.STREAM == jobClient.getComputeType() || FlinkMode.isPerJob(taskRunMode);
        if (isPerJob) {
            instanceInfos.add(InstanceInfo.newRecord(1, Quantity.getAmountInBytes(new Quantity("1")).doubleValue(), Quantity.getAmountInBytes(new Quantity(jobmanagerMemoryMb + "M")).doubleValue()));
        }
        return instanceInfos;
    }

    public static FlinkK8sSeesionResourceInfoBuilder FlinkK8sSeesionResourceInfoBuilder() {
        return new FlinkK8sSeesionResourceInfoBuilder();
    }

    public static class FlinkK8sSeesionResourceInfoBuilder {
        private KubernetesClient kubernetesClient;
        private String namespace;
        private Integer allowPendingPodSize;

        public FlinkK8sSeesionResourceInfoBuilder withKubernetesClient(KubernetesClient kubernetesClient) {
            this.kubernetesClient = kubernetesClient;
            return this;
        }

        public FlinkK8sSeesionResourceInfoBuilder withNamespace(String namespace) {
            this.namespace = namespace;
            return this;
        }

        public FlinkK8sSeesionResourceInfoBuilder withAllowPendingPodSize(Integer allowPendingPodSize) {
            this.allowPendingPodSize = allowPendingPodSize;
            return this;
        }

        public FlinkK8sSeesionResourceInfo build() {
            return new FlinkK8sSeesionResourceInfo(kubernetesClient, namespace, allowPendingPodSize);
        }
    }

}
