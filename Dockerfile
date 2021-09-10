FROM alpine:latest

ARG TERRAFORM_VERSION=0.14.7
ARG GAUGE_VERSION=1.3.3
# To have a consistent run, this must be the same as gauge-java.version in pom.xml
ARG GAUGE_JAVA_VERSION=0.7.15
ARG MAVEN_VERSION=3.8.1
ARG JDK_VERSION=14
LABEL maintainer="info@goquorum.com" \
    TERRAFORM_VERSION="${TERRAFORM_VERSION}" \
    SOLC_VERSION="${SOLC_VERSION}" \
    GAUGE_VERSION="${GAUGE_VERSION}" \
    MAVEN_VERSION="${MAVEN_VERSION}" \
    JDK_VERSION="openjdk-${JDK_VERSION}"
WORKDIR /workspace
ENV JAVA_HOME="/usr/lib/jvm/default-jvm" \
    PATH="/workspace/bin:/usr/local/maven/bin:${JAVA_HOME}/bin:${PATH}"
# require BASH for gauge to work as gauge-java plugin runs a shell script (launch.sh) which requires #!/bin/bash
COPY pom.xml manifest.json ./
RUN apk -q --no-cache --update add tar bash \
    && mkdir -p /tmp/downloads /usr/local/maven bin \
    && (pids=""; \
        (wget -O /tmp/downloads/terraform.zip -q https://releases.hashicorp.com/terraform/${TERRAFORM_VERSION}/terraform_${TERRAFORM_VERSION}_linux_amd64.zip \
        && unzip -q -o /tmp/downloads/terraform.zip -d bin) & \
        p="$!"; pids="$pids $p"; echo "  >> Installing Terraform ${TERRAFORM_VERSION} - PID $p"; \
        (wget -O /tmp/downloads/gauge.zip -q https://github.com/getgauge/gauge/releases/download/v${GAUGE_VERSION}/gauge-${GAUGE_VERSION}-linux.x86_64.zip \
        && unzip -q -o /tmp/downloads/gauge.zip -d bin && gauge install java --version ${GAUGE_JAVA_VERSION} > /dev/null && gauge install > /dev/null) & \
        p="$!"; pids="$pids $p"; echo "  >> Installing Gauge ${GAUGE_VERSION} - PID $p"; \
        (wget -O /tmp/downloads/maven.tar.gz -q https://apache.osuosl.org/maven/maven-3/${MAVEN_VERSION}/binaries/apache-maven-${MAVEN_VERSION}-bin.tar.gz \
        && tar -xzf /tmp/downloads/maven.tar.gz -C /usr/local/maven --strip-components=1) & \
        p="$!"; pids="$pids $p"; echo "  >> Installing Maven ${MAVEN_VERSION} - PID $p"; \
        (apk -q --no-cache add openjdk${JDK_VERSION} --repository=http://dl-cdn.alpinelinux.org/alpine/edge/testing) & \
        p="$!"; pids="$pids $p"; echo "  >> Installing OpenJDK ${JDK_VERSION} - PID $p"; \
        for pid in $pids; do if ! wait "$pid"; then echo "PID Failed: $pid"; exit 1; fi; done) \
    && echo "  >> Caching Maven Project Build" \
    && rm -rf /tmp/downloads

COPY . .

RUN mvn -q compile dependency:go-offline

ENTRYPOINT ["mvn", "--no-transfer-progress", "-B", "-DskipToolsCheck"]
CMD ["test", "-Dtags=basic"]
