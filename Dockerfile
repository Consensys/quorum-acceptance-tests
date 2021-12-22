FROM alpine:edge

ARG TERRAFORM_VERSION=0.14.7
ARG GAUGE_VERSION=1.3.3
# To have a consistent run, this must be the same as gauge-java.version in pom.xml
ARG GAUGE_JAVA_VERSION=0.7.15
ARG JDK_VERSION=14
LABEL maintainer="info@goquorum.com" \
    TERRAFORM_VERSION="${TERRAFORM_VERSION}" \
    GAUGE_VERSION="${GAUGE_VERSION}" \
    JDK_VERSION="openjdk-${JDK_VERSION}"
WORKDIR /workspace
ENV JAVA_HOME="/usr/lib/jvm/default-jvm" \
    PATH="/workspace/bin:${JAVA_HOME}/bin:${PATH}"
# require BASH for gauge to work as gauge-java plugin runs a shell script (launch.sh) which requires #!/bin/bash
COPY pom.xml manifest.json ./
COPY ./releases.json /root/.web3j/solc/releases.json
RUN apk -q --no-cache --update add tar bash \
    && mkdir -p /tmp/downloads bin \
    && (pids=""; \
        (wget -O /tmp/downloads/terraform.zip -q https://releases.hashicorp.com/terraform/${TERRAFORM_VERSION}/terraform_${TERRAFORM_VERSION}_linux_amd64.zip \
        && unzip -q -o /tmp/downloads/terraform.zip -d bin) & \
        p="$!"; pids="$pids $p"; echo "  >> Installing Terraform ${TERRAFORM_VERSION} - PID $p"; \
        (wget -O /tmp/downloads/gauge.zip -q https://github.com/getgauge/gauge/releases/download/v${GAUGE_VERSION}/gauge-${GAUGE_VERSION}-linux.x86_64.zip \
        && unzip -q -o /tmp/downloads/gauge.zip -d bin && gauge install java --version ${GAUGE_JAVA_VERSION} > /dev/null && gauge install > /dev/null) & \
        p="$!"; pids="$pids $p"; echo "  >> Installing Gauge ${GAUGE_VERSION} - PID $p"; \
        (apk -q --no-cache add openjdk${JDK_VERSION} --repository=http://dl-cdn.alpinelinux.org/alpine/edge/testing) & \
        p="$!"; pids="$pids $p"; echo "  >> Installing OpenJDK ${JDK_VERSION} - PID $p"; \
        for pid in $pids; do if ! wait "$pid"; then echo "PID Failed: $pid"; exit 1; fi; done) \
    && rm -rf /tmp/downloads

COPY . .

RUN ./mvnw -q compile dependency:go-offline

ENTRYPOINT ["./mvnw", "--no-transfer-progress", "-B", "-DskipToolsCheck"]
CMD ["test", "-Dtags=basic"]
