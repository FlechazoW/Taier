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

package org.apache.flink.kubernetes.configuration;

import org.apache.flink.annotation.PublicEvolving;
import org.apache.flink.configuration.ConfigOption;

import static org.apache.flink.configuration.ConfigOptions.key;

/**
 * This class holds configuration constants used by Flink's kubernetes runners.
 */
@PublicEvolving
public class KubernetesConfigOptions {

    public static final ConfigOption<String> REST_SERVICE_EXPOSED_TYPE =
        key("kubernetes.rest-service.exposed.type")
            .stringType()
            .defaultValue(ServiceExposedType.LoadBalancer.toString())
            .withDescription("It could be ClusterIP/NodePort/LoadBalancer(default). When set to ClusterIP, the rest service" +
                "will not be created.");

    public static final ConfigOption<String> JOB_MANAGER_SERVICE_ACCOUNT =
        key("kubernetes.jobmanager.service-account")
            .stringType()
            .defaultValue("default")
            .withDescription("Service account that is used by jobmanager within kubernetes cluster. " +
                "The job manager uses this service account when requesting taskmanager pods from the API server.");

    public static final ConfigOption<Double> JOB_MANAGER_CPU =
        key("kubernetes.jobmanager.cpu")
            .doubleType()
            .defaultValue(1.0)
            .withDescription("The number of cpu used by job manager");

    public static final ConfigOption<Double> TASK_MANAGER_CPU =
        key("kubernetes.taskmanager.cpu")
            .doubleType()
            .defaultValue(-1.0)
            .withDescription("The number of cpu used by task manager. By default, the cpu is set " +
                "to the number of slots per TaskManager");

    public static final ConfigOption<String> CONTAINER_IMAGE_PULL_POLICY =
        key("kubernetes.container.image.pull-policy")
            .stringType()
            .defaultValue("IfNotPresent")
            .withDescription("Kubernetes image pull policy. Valid values are Always, Never, and IfNotPresent. " +
                "The default policy is IfNotPresent to avoid putting pressure to image repository.");

    public static final ConfigOption<String> CONTAINER_IMAGE_PULL_SECRETS =
        key("kubernetes.container.image.pull-secrets")
            .stringType()
            .noDefaultValue()
            .withDescription("Kubernetes image pull secrets. Comma separated list of the Kubernetes secrets used " +
                "to access private image registries.");

    public static final ConfigOption<String> KUBE_CONFIG_FILE =
        key("kubernetes.config.file")
            .stringType()
            .noDefaultValue()
            .withDescription("The kubernetes config file will be used to create the client. The default " +
                "is located at ~/.kube/config");

    public static final ConfigOption<String> NAMESPACE =
        key("kubernetes.namespace")
            .stringType()
            .defaultValue("default")
            .withDescription("The namespace that will be used for running the jobmanager and taskmanager pods.");

    public static final ConfigOption<String> CONTAINER_START_COMMAND_TEMPLATE =
        key("kubernetes.container-start-command-template")
            .stringType()
            .defaultValue("%java% %classpath% %jvmmem% %jvmopts% %logging% %class% %args% %redirects%")
            .withDescription("Template for the kubernetes jobmanager and taskmanager container start invocation.");

    public static final ConfigOption<String> SERVICE_CREATE_TIMEOUT =
        key("kubernetes.service.create-timeout")
            .stringType()
            .defaultValue("1 min")
            .withDescription("Timeout used for creating the service. The timeout value requires a time-unit " +
                "specifier (ms/s/min/h/d).");

    // ---------------------------------------------------------------------------------
    // The following config options could be overridden by KubernetesCliOptions.
    // ---------------------------------------------------------------------------------

    public static final ConfigOption<String> CLUSTER_ID =
        key("kubernetes.cluster-id")
            .stringType()
            .noDefaultValue()
            .withDescription("The cluster id used for identifying the unique flink cluster. If it's not set, " +
                "the client will generate a random UUID name.");

    public static final ConfigOption<String> CONTAINER_IMAGE =
        key("kubernetes.container.image")
            .stringType()
            .defaultValue("flink:latest")
            .withDescription("Image to use for Flink containers.");

    /**
     * The following config options need to be set according to the image.
     */
    public static final ConfigOption<String> KUBERNETES_ENTRY_PATH =
        key("kubernetes.entry.path")
            .stringType()
            .defaultValue("/opt/flink/bin/kubernetes-entry.sh")
            .withDescription("The entrypoint script of kubernetes in the image. It will be used as command for jobmanager " +
                "and taskmanager container.");

    public static final ConfigOption<String> FLINK_CONF_DIR =
        key("kubernetes.flink.conf.dir")
            .stringType()
            .defaultValue("/opt/flink/conf")
            .withDescription("The flink conf directory that will be mounted in pod. The flink-conf.yaml, log4j.properties, " +
                "logback.xml in this path will be overwritten from config map.");

    public static final ConfigOption<String> HADOOP_CONF_DIR =
        key("kubernetes.hadoop.conf.dir")
            .stringType()
            .defaultValue("/opt/hadoop/etc/hadoop")
            .withDescription("The hadoop conf directory that will be mounted in pod. The core-site.xml" +
                "in this path will be overwritten from config map.");

    public static final ConfigOption<String> HADOOP_CONF_STRING =
        key("hadoop.conf.string")
            .stringType()
            .defaultValue(null)
            .withDescription("The hadoop conf content that will be mounted in pod");


    public static final ConfigOption<String> FLINK_LOG_DIR =
        key("kubernetes.flink.log.dir")
            .stringType()
            .defaultValue("/opt/flink/log")
            .withDescription("The directory that logs of jobmanager and taskmanager be saved in the pod.");

    public static final ConfigOption<String> FLINK_LOG_LEVEL =
        key("logLevel")
            .stringType()
            .defaultValue("info")
            .withDescription("The task run log level.");

    public static final ConfigOption<String> FLINK_LOG_FILE_NAME =
        key("logFileName")
            .stringType()
            .defaultValue("log4j.properties")
            .withDescription("The task run log file name.");

    public static final ConfigOption<String> KUBERNETES_HOST_ALIASES =
        key("kubernetes.host.aliases")
            .stringType()
            .noDefaultValue()
            .withDescription("the host aliases.");

    public static final ConfigOption<String> KUBERNETES_DOCKER_FLINKPLUGIN_PATH =
        key("kubernetes.docker.flinkplugin.path")
            .stringType()
            .defaultValue("/opt/flink/mount/flinkplugin")
            .withDescription("docker flinkplugin path.");

    public static final ConfigOption<String> KUBERNETES_DOCKER_CHECKPOINT_PATH =
        key("kubernetes.docker.checkpoint.path")
            .stringType()
            .defaultValue("/opt/flink/mount/checkpoints")
            .withDescription("docker checkpoints path.");

    public static final ConfigOption<String> KUBERNETES_DOCKER_SAVEPOINT_PATH =
        key("kubernetes.docker.savepoint.path")
            .stringType()
            .defaultValue("/opt/flink/mount/savepoints")
            .withDescription("docker savepoints path.");

    public static final ConfigOption<String> KUBERNETES_DOCKER_COMPLETEDJOB_PATH =
        key("kubernetes.docker.completedjob.path")
            .stringType()
            .defaultValue("/opt/flink/mount/completed-jobs")
            .withDescription("docker completed-jobs path.");

    public static final ConfigOption<String> KUBERNETES_DOCKER_HA_STORAGE_PATH =
        key("kubernetes.docker.ha.path")
            .stringType()
            .defaultValue("/opt/flink/mount/ha")
            .withDescription("docker ha_storage path.");


    /**
     * The flink rest service exposed type.
     */
    public enum ServiceExposedType {
        ClusterIP,
        NodePort,
        LoadBalancer
    }

    /** This class is not meant to be instantiated. */
    private KubernetesConfigOptions() {}
}
