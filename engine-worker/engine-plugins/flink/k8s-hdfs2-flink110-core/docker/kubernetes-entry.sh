#!/usr/bin/env bash
################################################################################
#  Licensed to the Apache Software Foundation (ASF) under one
#  or more contributor license agreements.  See the NOTICE file
#  distributed with this work for additional information
#  regarding copyright ownership.  The ASF licenses this file
#  to you under the Apache License, Version 2.0 (the
#  "License"); you may not use this file except in compliance
#  with the License.  You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  See the License for the specific language governing permissions and
# limitations under the License.
################################################################################

# The entrypoint script of flink-kubernetes integration.
# It is the command of jobmanager and taskmanager container.

bin=`dirname "$0"`
bin=`cd "$bin"; pwd`

# get Flink config
. /opt/flink/bin/config.sh

FLINK_CLASSPATH=`manglePathList $(constructFlinkClassPath):$INTERNAL_HADOOP_CLASSPATHS`
# FLINK_CLASSPATH will be used by KubernetesUtils.java to generate jobmanager and taskmanager start command.
export FLINK_CLASSPATH


sed -i "s/flinkx_hosts/$FLINKX_HOSTS/g" /opt/filebeat/conf/filebeat-dtstack.yml

if [[ $HOSTNAME == *taskmanager* ]]; then
    component="taskmanager"${HOSTNAME#*taskmanager}
else
    component="jobmanager"
fi
sed -i "s/component_value/$component/g" /opt/filebeat/conf/filebeat-dtstack.yml
sed -i "s/taskId_value/$TASK_ID/g" /opt/filebeat/conf/filebeat-dtstack.yml



filebeat_command="/opt/filebeat/bin/filebeat -c /opt/filebeat/conf/filebeat-dtstack.yml"

command="$filebeat_command & $@"
echo "Start command: $command"
exec gosu flink /opt/filebeat/bin/filebeat -c /opt/filebeat/conf/filebeat-dtstack.yml & "$@"