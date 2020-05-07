FROM alpine:latest

ARG TERRAFORM_VERSION=0.12.24
ARG SOLC_VERSION=0.5.5
ARG GAUGE_VERSION=1.0.8
ARG TERRAFORM_PROVIDER_QUORUM_VERSION=1.0.0-beta.1
ARG MAVEN_VERSION=3.6.3
ARG JDK_VERSION=11.0.7
LABEL maintainer="info@goquorum.com" \
    TERRAFORM_VERSION="${TERRAFORM_VERSION}" \
    SOLC_VERSION=""${SOLC_VERSION} \
    GAUGE_VERSION="${GAUGE_VERSION}" \
    TERRAFORM_PROVIDER_QUORUM_VERSION="${TERRAFORM_PROVIDER_QUORUM_VERSION}" \
    MAVEN_VERSION="${MAVEN_VERSION}" \
    JDK_VERSION="openjdk-${JDK_VERSION}"
WORKDIR /workspace
COPY . .
ENV JAVA_HOME="/usr/lib/jvm/default-jvm" \
    PATH="/workspace/bin:/usr/local/maven/bin:${JAVA_HOME}/bin:${PATH}" \
    TF_VAR_output_dir="/tmp/acctests"
# require BASH for gauge to work as gauge-java plugin runs a shell script (launch.sh) which requires #!/bin/bash
RUN apk -q --no-cache --update add tar bash \
    && mkdir -p /tmp/downloads /usr/local/maven ${TF_VAR_output_dir} bin \
    && ((echo "  >> Installing Terraform ${TERRAFORM_VERSION}" \
        && wget -O /tmp/downloads/terraform.zip -q https://releases.hashicorp.com/terraform/${TERRAFORM_VERSION}/terraform_${TERRAFORM_VERSION}_linux_amd64.zip \
        && unzip -q -o /tmp/downloads/terraform.zip -d bin) \
      & (echo "  >> Installing Solc ${SOLC_VERSION}" \
        && wget -O bin/solc -q https://github.com/ethereum/solidity/releases/download/v${SOLC_VERSION}/solc-static-linux \
        && chmod +x bin/solc) \
      & (echo "  >> Installing Gauge ${GAUGE_VERSION}" \
        && wget -O /tmp/downloads/gauge.zip -q https://github.com/getgauge/gauge/releases/download/v${GAUGE_VERSION}/gauge-${GAUGE_VERSION}-linux.x86_64.zip \
        && unzip -q -o /tmp/downloads/gauge.zip -d bin && gauge install > /dev/null) \
      & (echo "  >> Installing Terraform Quorum Provider ${TERRAFORM_PROVIDER_QUORUM_VERSION}" \
        && wget -O /tmp/downloads/provider.zip -q https://dl.bintray.com/quorumengineering/terraform/terraform-provider-quorum/v${TERRAFORM_PROVIDER_QUORUM_VERSION}/terraform-provider-quorum_${TERRAFORM_PROVIDER_QUORUM_VERSION}_linux_amd64.zip \
        && unzip -q -o /tmp/downloads/provider.zip -d bin) \
      & (echo "  >> Installing Maven ${MAVEN_VERSION}" \
        && wget -O /tmp/downloads/maven.tar.gz -q https://apache.osuosl.org/maven/maven-3/${MAVEN_VERSION}/binaries/apache-maven-${MAVEN_VERSION}-bin.tar.gz \
        && tar -xzf /tmp/downloads/maven.tar.gz -C /usr/local/maven --strip-components=1) \
      & (echo "  >> Installing OpenJDK ${JDK_VERSION}" \
        && apk -q --no-cache add openjdk11 --repository=http://dl-cdn.alpinelinux.org/alpine/edge/community) \
      ; wait) \
    && echo "  >> Caching Maven Project Dependencies" \
    && mvn -q dependency:resolve dependency:resolve-plugins \
    && rm -rf /tmp/downloads

VOLUME ${TF_VAR_output_dir}
ENTRYPOINT ["mvn", "--no-transfer-progress", "-B"]
CMD ["test", "-Dtags=basic"]