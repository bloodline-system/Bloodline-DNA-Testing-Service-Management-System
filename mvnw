#!/usr/bin/env sh
# ----------------------------------------------------------------------------
# Maven Wrapper
# ----------------------------------------------------------------------------
# Copyright (c) 2010-2024 The Apache Software Foundation
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     https://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

MAVEN_PROJECTBASEDIR=$(cd "$(dirname "$0")" && pwd)

if [ -z "$MAVEN_PROJECTBASEDIR" ]; then
  MAVEN_PROJECTBASEDIR="."
fi

JAVA_EXE=${JAVA_HOME:+$JAVA_HOME/bin/java}
if [ -z "$JAVA_EXE" ]; then
  JAVA_EXE=java
fi

WRAPPER_JAR="$MAVEN_PROJECTBASEDIR/.mvn/wrapper/maven-wrapper.jar"
WRAPPER_PROPERTIES="$MAVEN_PROJECTBASEDIR/.mvn/wrapper/maven-wrapper.properties"

if [ ! -f "$WRAPPER_JAR" ]; then
  echo "Maven Wrapper jar not found in $WRAPPER_JAR"
  echo "Please ensure .mvn/wrapper/maven-wrapper.jar is present."
  exit 1
fi

exec "$JAVA_EXE" -Dmaven.multiModuleProjectDirectory="$MAVEN_PROJECTBASEDIR" -cp "$WRAPPER_JAR" org.apache.maven.wrapper.MavenWrapperMain "$@"
