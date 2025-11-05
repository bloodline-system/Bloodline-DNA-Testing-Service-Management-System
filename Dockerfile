# Stage 1: Build Java application with Maven
FROM maven:3.9.10-amazoncorretto-21 AS build

WORKDIR /app
COPY pom.xml .
COPY src ./src
COPY uploads ./uploads
COPY uploads_information ./uploads_information

RUN mvn clean package -DskipTests

# Stage 2: Create minimal JRE using jlink
FROM amazoncorretto:21 AS jre-build

RUN yum update -y && yum install -y binutils

RUN jlink \
  --add-modules ALL-MODULE-PATH \
  --no-man-pages --no-header-files --strip-debug \
  --compress=2 \
  --output /opt/java-minimal

# Stage 3: Runtime environment using Debian slim
FROM debian:bullseye-slim

# Copy custom minimal JRE
COPY --from=jre-build /opt/java-minimal /opt/java

# Copy application
COPY --from=build /app/target/*.jar /app/app.jar

# Non-root user for safety
RUN useradd -ms /bin/bash appuser
USER appuser

WORKDIR /app
ENV PATH="/opt/java/bin:$PATH"

ENTRYPOINT ["java", "-jar", "app.jar"]
