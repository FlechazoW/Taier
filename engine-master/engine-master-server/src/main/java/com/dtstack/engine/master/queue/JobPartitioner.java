package com.dtstack.engine.master.queue;

import com.dtstack.engine.master.listener.QueueListener;
import com.dtstack.engine.master.zookeeper.ZkService;
import com.google.common.collect.Maps;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * company: www.dtstack.com
 * author: toutian
 * create: 2020/01/17
 */
@Component
public class JobPartitioner {

    @Autowired
    private QueueListener queueListener;

    @Autowired
    private ZkService zkService;

    public Map<String, Integer> getDefaultStrategy(List<String> aliveNodes, int jobSize) {
        Map<String, Integer> jobSizeInfo = new HashMap<>(aliveNodes.size());
        int size = (jobSize / aliveNodes.size()) + 1;
        for (String aliveNode : aliveNodes) {
            jobSizeInfo.put(aliveNode, size);
        }
        return jobSizeInfo;
    }

    /**
     * compute job number per node
     */
    public Map<String, Integer> computeBatchJobSize(Integer type, int jobSize) {
        //节点挂了就会迁移的
        List<String> aliveNodes = zkService.getAliveBrokersChildren();
        Map<Integer, Map<String, QueueInfo>> allNodesJobQueueInfo = queueListener.getAllNodesJobQueueInfo();
        if (allNodesJobQueueInfo.isEmpty()) {
            return getDefaultStrategy(aliveNodes, jobSize);
        }
        Map<String, QueueInfo> nodesJobQueue = allNodesJobQueueInfo.get(type);
        if (nodesJobQueue == null || nodesJobQueue.isEmpty()) {
            return getDefaultStrategy(aliveNodes, jobSize);
        }
        Map<String, Integer> nodeSort = Maps.newHashMap();
        int total = jobSize;
        for (Map.Entry<String, QueueInfo> queueInfoEntry : nodesJobQueue.entrySet()) {
            QueueInfo queueInfo = queueInfoEntry.getValue();
            total += queueInfo.getSize();
            //排除宕机节点
            if (aliveNodes.contains(queueInfoEntry.getKey())) {
                nodeSort.put(queueInfoEntry.getKey(), queueInfo.getSize());
            }
        }
        if (nodeSort.isEmpty()) {
            return getDefaultStrategy(aliveNodes, jobSize);
        }
        int avg = (total / nodeSort.size()) + 1;
        for (Map.Entry<String, Integer> entry : nodeSort.entrySet()) {
            entry.setValue(avg - entry.getValue());
        }
        return nodeSort;
    }

    public Map<String, Integer> computeJobCacheSize(String jobResource, int jobSize) {
        List<String> aliveNodes = zkService.getAliveBrokersChildren();
        Map<String, Map<String, GroupInfo>> allNodesGroupQueueJobResources = queueListener.getAllNodesGroupQueueInfo();
        if (allNodesGroupQueueJobResources.isEmpty()) {
            return getDefaultStrategy(aliveNodes, jobSize);
        }
        Map<String, GroupInfo> nodesGroupQueue = allNodesGroupQueueJobResources.get(jobResource);
        if (nodesGroupQueue == null || nodesGroupQueue.isEmpty()) {
            return getDefaultStrategy(aliveNodes, jobSize);
        }
        Map<String, Integer> nodeSort = Maps.newHashMap();
        int total = jobSize;
        for (Map.Entry<String, GroupInfo> groupInfoEntry : nodesGroupQueue.entrySet()) {
            GroupInfo groupInfo = groupInfoEntry.getValue();
            total += groupInfo.getSize();
            //排除宕机节点
            if (aliveNodes.contains(groupInfoEntry.getKey())) {
                nodeSort.put(groupInfoEntry.getKey(), groupInfo.getSize());
            }
        }
        if (nodeSort.isEmpty()) {
            return getDefaultStrategy(aliveNodes, jobSize);
        }
        int avg = (total / nodeSort.size()) + 1;
        for (Map.Entry<String, Integer> entry : nodeSort.entrySet()) {
            entry.setValue(avg - entry.getValue());
        }
        return nodeSort;
    }

    public Map<String, GroupInfo> getGroupInfoByJobResource(String jobResource) {
        Map<String, Map<String, GroupInfo>> allNodesGroupQueueJobResources = queueListener.getAllNodesGroupQueueInfo();
        if (allNodesGroupQueueJobResources.isEmpty()) {
            return null;
        }
        Map<String, GroupInfo> nodesGroupQueue = allNodesGroupQueueJobResources.get(jobResource);
        if (nodesGroupQueue == null || nodesGroupQueue.isEmpty()) {
            return null;
        }
        List<String> aliveBrokers = zkService.getAliveBrokersChildren();
        //将不存活节点过滤
        Iterator<Map.Entry<String, GroupInfo>> nodesGroupQueueIt = nodesGroupQueue.entrySet().iterator();
        while (nodesGroupQueueIt.hasNext()) {
            Map.Entry<String, GroupInfo> groupInfoEntry = nodesGroupQueueIt.next();
            String nodeAddress = groupInfoEntry.getKey();
            if (!aliveBrokers.contains(nodeAddress)) {
                nodesGroupQueueIt.remove();
            }
        }
        return nodesGroupQueue;
    }
}
