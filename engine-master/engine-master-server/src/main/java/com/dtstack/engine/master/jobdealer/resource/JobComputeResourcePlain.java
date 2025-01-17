package com.dtstack.engine.master.jobdealer.resource;

import com.dtstack.engine.api.domain.Cluster;
import com.dtstack.engine.api.domain.Queue;
import com.dtstack.engine.api.enums.ScheduleEngineType;
import com.dtstack.engine.common.JobClient;
import com.dtstack.engine.common.env.EnvironmentContext;
import com.dtstack.engine.dao.ClusterDao;
import com.dtstack.engine.dao.EngineTenantDao;
import com.dtstack.engine.master.impl.ClusterService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

import static com.dtstack.engine.common.constrant.ConfigConstant.RESOURCE_NAMESPACE_OR_QUEUE_DEFAULT;
import static com.dtstack.engine.common.constrant.ConfigConstant.SPLIT;

/**
 * company: www.dtstack.com
 * author: toutian
 * create: 2020/2/10
 */
@Component
public class JobComputeResourcePlain {

    @Autowired
    private CommonResource commonResource;

    @Autowired
    private EnvironmentContext environmentContext;

    @Autowired
    private EngineTenantDao engineTenantDao;

    @Autowired
    private ClusterDao clusterDao;

    @Autowired
    private ClusterService clusterService;


    public String getJobResource(JobClient jobClient) {
        this.buildJobClientGroupName(jobClient);
        ComputeResourceType computeResourceType = commonResource.newInstance(jobClient);

        String plainType = environmentContext.getComputeResourcePlain();
        String jobResource = null;
        if (ComputeResourcePlain.EngineTypeClusterQueue.name().equalsIgnoreCase(plainType)) {
            jobResource = jobClient.getEngineType() + SPLIT + jobClient.getGroupName();
        } else {
            jobResource = jobClient.getEngineType() + SPLIT + jobClient.getGroupName() + SPLIT + jobClient.getComputeType().name().toLowerCase();
        }
        return jobResource + SPLIT + computeResourceType.name();
    }


    private void buildJobClientGroupName(JobClient jobClient) {
        Long clusterId = engineTenantDao.getClusterIdByTenantId(jobClient.getTenantId());
        if(null == clusterId){
            return;
        }
        Cluster cluster = clusterDao.getOne(clusterId);
        if (null == cluster) {
            return;
        }
        String clusterName = cluster.getClusterName();
        //%s_default
        String groupName = clusterName + SPLIT + RESOURCE_NAMESPACE_OR_QUEUE_DEFAULT;

        if (ScheduleEngineType.KUBERNETES.getEngineName().equals(jobClient.getEngineType())) {
            String namespace = clusterService.getNamespace(jobClient.getParamAction(),
                    jobClient.getTenantId(), jobClient.getEngineType(), jobClient.getComputeType());
            groupName = clusterName + SPLIT + namespace;
        } else {
            Queue queue = clusterService.getQueue(jobClient.getTenantId(), clusterId);
            if (null != queue) {
                groupName = clusterName + SPLIT + queue.getQueueName();
            }
        }
        jobClient.setGroupName(groupName);
    }


    public String parseClusterFromJobResource(String jobResource) {
        if (StringUtils.isBlank(jobResource)) {
            return "";
        }
        String plainType = environmentContext.getComputeResourcePlain();
        List<String> clusterArray = new ArrayList<>();
        String[] split = jobResource.split(SPLIT);
        if (ComputeResourcePlain.EngineTypeClusterQueueComputeType.name().equalsIgnoreCase(plainType)) {
            //engineType_cluster_queue_computeType_computeResourceType 拼接方式
            for (int i = 1; i < split.length - 3; i++) {
                clusterArray.add(split[i]);
            }
        } else {
            // engineType_cluster_queue_computeResourceType 拼接方式
            for (int i = 1; i < split.length - 2; i++) {
                clusterArray.add(split[i]);
            }
        }
        return String.join(SPLIT, clusterArray);
    }


}
