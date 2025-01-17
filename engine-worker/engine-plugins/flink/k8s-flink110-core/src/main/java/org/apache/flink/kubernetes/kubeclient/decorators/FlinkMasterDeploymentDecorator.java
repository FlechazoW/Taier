/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.flink.kubernetes.kubeclient.decorators;

import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.apache.commons.collections.CollectionUtils;
import org.apache.flink.client.cli.CliFrontend;
import org.apache.flink.client.deployment.ClusterSpecification;
import org.apache.flink.configuration.BlobServerOptions;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.configuration.JobManagerOptions;
import org.apache.flink.configuration.ResourceManagerOptions;
import org.apache.flink.configuration.RestOptions;
import org.apache.flink.kubernetes.configuration.KubernetesConfigOptions;
import org.apache.flink.kubernetes.configuration.KubernetesConfigOptionsInternal;
import org.apache.flink.kubernetes.kubeclient.resources.KubernetesDeployment;
import org.apache.flink.kubernetes.utils.Constants;
import org.apache.flink.kubernetes.utils.KubernetesUtils;
import org.apache.flink.runtime.clusterframework.BootstrapTools;

import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentSpecBuilder;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.apache.flink.kubernetes.utils.Constants.API_VERSION;
import static org.apache.flink.kubernetes.utils.Constants.ENV_FLINK_POD_IP_ADDRESS;
import static org.apache.flink.kubernetes.utils.Constants.POD_IP_FIELD_PATH;
import static org.apache.flink.util.Preconditions.checkArgument;
import static org.apache.flink.util.Preconditions.checkNotNull;

/**
 * Flink master specific deployment configuration.
 */
public class FlinkMasterDeploymentDecorator extends Decorator<Deployment, KubernetesDeployment> {

    private static final String CONTAINER_NAME = "flink-job-manager";

    private final ClusterSpecification clusterSpecification;

    private final KubernetesClient internalClient;

    public FlinkMasterDeploymentDecorator(ClusterSpecification clusterSpecification, KubernetesClient internalClient) {
        this.clusterSpecification = clusterSpecification;
        this.internalClient = internalClient;
    }

    @Override
    protected Deployment decorateInternalResource(Deployment deployment, Configuration flinkConfig) {
        final String clusterId = flinkConfig.getString(KubernetesConfigOptions.CLUSTER_ID);
        checkNotNull(clusterId, "ClusterId must be specified!");

        final int blobServerPort = KubernetesUtils.parsePort(flinkConfig, BlobServerOptions.PORT);
        checkArgument(blobServerPort > 0, "%s should not be 0.", BlobServerOptions.PORT.key());

        final String mainClass = flinkConfig.getString(KubernetesConfigOptionsInternal.ENTRY_POINT_CLASS);
        checkNotNull(mainClass, "Main class must be specified!");

        final String namespace = flinkConfig.getString(KubernetesConfigOptions.NAMESPACE);
        String configMapName = Constants.CONFIG_MAP_PREFIX + clusterId;
        ConfigMap flinkConfigMap = this.internalClient.configMaps().inNamespace(namespace).withName(configMapName).get();

        final boolean hasLogback = flinkConfigMap.getData().containsKey(Constants.CONFIG_FILE_LOGBACK_NAME);
        final boolean hasLog4j = flinkConfigMap.getData().containsKey(Constants.CONFIG_FILE_LOG4J_NAME);

        final Map<String, String> labels = new LabelBuilder()
            .withExist(deployment.getMetadata().getLabels())
            .withJobManagerComponent()
            .toLabels();

        deployment.getMetadata().setLabels(labels);

        final Volume configMapVolume = KubernetesUtils.getConfigMapVolume(clusterId, hasLogback, hasLog4j);

        List<Volume> volumes = new ArrayList<>();
        volumes.add(configMapVolume);
        if (flinkConfig.contains(KubernetesConfigOptions.HADOOP_CONF_STRING)) {
            final Volume hadoopConfigMapVolume = KubernetesUtils.getHadoopConfigMapVolume(clusterId);
            volumes.add(hadoopConfigMapVolume);
        }

        List<Volume> customVolumes = KubernetesUtils.parseVolumesWithPrefix(Constants.KUBERNETES_JOB_MANAGER_VOLUMES_PREFIX, flinkConfig, volumes);
        if (CollectionUtils.isNotEmpty(customVolumes)) {
            volumes.addAll(customVolumes);
        }

        final Container container = createJobManagerContainer(flinkConfig, mainClass, hasLogback, hasLog4j, blobServerPort);

        final String serviceAccount = flinkConfig.getString(KubernetesConfigOptions.JOB_MANAGER_SERVICE_ACCOUNT);

        final PodSpec podSpec = new PodSpecBuilder()
            .withServiceAccountName(serviceAccount)
            .withVolumes(volumes)
            .withContainers(container)
            .withImagePullSecrets(KubernetesUtils.getImagePullSecrets(flinkConfig))
            .build();

        deployment.setSpec(new DeploymentSpecBuilder()
            .withReplicas(1)
            .withNewTemplate().withNewMetadata().withLabels(labels).endMetadata()
            .withSpec(podSpec).endTemplate()
            .withNewSelector().addToMatchLabels(labels).endSelector().build());
        return deployment;
    }

    private Container createJobManagerContainer(
        Configuration flinkConfig,
        String mainClass,
        boolean hasLogback,
        boolean hasLog4j,
        int blobServerPort) {
        final String flinkConfDirInPod = flinkConfig.getString(KubernetesConfigOptions.FLINK_CONF_DIR);
        final String logDirInPod = flinkConfig.getString(KubernetesConfigOptions.FLINK_LOG_DIR);
        final String startCommand = KubernetesUtils.getJobManagerStartCommand(
            flinkConfig,
            clusterSpecification.getMasterMemoryMB(),
            flinkConfDirInPod,
            logDirInPod,
            hasLogback,
            hasLog4j,
            mainClass,
            null);

        final ResourceRequirements requirements = KubernetesUtils.getResourceRequirements(
            clusterSpecification.getMasterMemoryMB(),
            flinkConfig.getDouble(KubernetesConfigOptions.JOB_MANAGER_CPU));

        List<VolumeMount> volumeMounts = new ArrayList();
        List<VolumeMount> configMapVolumeMounts = KubernetesUtils.getConfigMapVolumeMount(flinkConfig, hasLogback, hasLog4j);
        volumeMounts.addAll(configMapVolumeMounts);

        List<VolumeMount> customVolumeMounts = KubernetesUtils.parseVolumeMountsWithPrefix(Constants.KUBERNETES_JOB_MANAGER_VOLUMES_PREFIX, flinkConfig, volumeMounts);
        if (CollectionUtils.isNotEmpty(customVolumeMounts)) {
            volumeMounts.addAll(customVolumeMounts);
        }

        return new ContainerBuilder()
            .withName(CONTAINER_NAME)
            .withCommand(flinkConfig.getString(KubernetesConfigOptions.KUBERNETES_ENTRY_PATH))
            .withArgs(Arrays.asList("/bin/bash", "-c", startCommand))
            .withImage(flinkConfig.getString(KubernetesConfigOptions.CONTAINER_IMAGE))
            .withImagePullPolicy(flinkConfig.getString(KubernetesConfigOptions.CONTAINER_IMAGE_PULL_POLICY))
            .withResources(requirements)
            .withPorts(Arrays.asList(
                new ContainerPortBuilder().withContainerPort(flinkConfig.getInteger(RestOptions.PORT)).build(),
                new ContainerPortBuilder().withContainerPort(flinkConfig.getInteger(JobManagerOptions.PORT)).build(),
                new ContainerPortBuilder().withContainerPort(blobServerPort).build()))
            .withEnv(buildEnvForContainer(flinkConfig))
            .withVolumeMounts(volumeMounts)
            .build();
    }

    private List<EnvVar> buildEnvForContainer(Configuration flinkConfig) {
        List<EnvVar> envList =
            BootstrapTools.getEnvironmentVariables(ResourceManagerOptions.CONTAINERIZED_MASTER_ENV_PREFIX, flinkConfig)
                .entrySet()
                .stream()
                .map(kv -> new EnvVar(kv.getKey(), kv.getValue(), null)).collect(Collectors.toList());
        envList.add(new EnvVarBuilder()
            .withName(ENV_FLINK_POD_IP_ADDRESS)
            .withValueFrom(new EnvVarSourceBuilder().withNewFieldRef(API_VERSION, POD_IP_FIELD_PATH).build())
            .build());
        return envList;
    }
}
