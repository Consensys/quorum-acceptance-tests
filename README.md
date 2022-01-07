![Standard Acceptance Tests](https://github.com/ConsenSys/quorum-acceptance-tests/workflows/Standard%20Acceptance%20Tests/badge.svg?branch=master) 
![Extra Acceptance Tests](https://github.com/ConsenSys/quorum-acceptance-tests/workflows/Extra%20Acceptance%20Tests%20(feature-enabled%20networks)/badge.svg?branch=master) 

# Quick Start

- Install [Docker Engine](https://docs.docker.com/engine/) or [Docker Desktop](https://www.docker.com/products/docker-desktop)
- Run basic acceptance tests against a new GoQuorum network using Raft consensus:
    ```
    docker run --rm --network host -v /var/run/docker.sock:/var/run/docker.sock -v /tmp/acctests:/tmp/acctests \
            quorumengineering/acctests:latest test -Pauto -Dtags="basic || networks/typical::raft" \
            -Dauto.outputDir=/tmp/acctests -Dnetwork.forceDestroy=true
    ```

# Development

Development environment requires the following:

- JDK 14+
- [Gauge](https://gauge.org/get_started)
- Run `./mvnw compile` to initiate the project with generated Java sources from Solidity source

With built-in provisioning feature:
- [Docker Engine](https://docs.docker.com/engine/) or [Docker Desktop](https://www.docker.com/products/docker-desktop)
- [Terraform](https://terraform.io) 0.14+
  
**For more details on tools and versions being used, please refer to [Dockerfile](Dockerfile)**

## Writing Tests

- Use [Gauge](https://github.com/getgauge/gauge) test automation framework
- Test Specs are stored in [`src/specs`](src/specs) folder
  - Folder `01_basic` contains specifications which describe GoQuorum's basic functionalities. All specifications must be tagged as `basic`
  - Folder `02_advanced` contains specifications which are for making sure GoQuorum's basic functionalities are working under different conditions in the chain. All specifications must be tagged as `advanced`
- Glue codes are written in Java under [`src/test/java`](src/test/java) folder
- Tests are generally written against 4-node GoQuorum network

## Running Tests

### With auto-provisioning network

> :bulb: Tag `networks/typical::raft` means:
> - Executing tests which are tagged with the value
> - Instructing Maven to provision `networks/typical` with profile `raft` when using Maven Profile `auto` (i.e.: `-Pauto`)
> - `networks/typical` is a folder that contains Terraform configuration to provision the network

- Run basic tests for raft consensus: 
    ```
    ./mvnw clean test -Pauto -Dtags="basic || basic-raft || networks/typical::raft"
    ```
- Run basic tests for istanbul consensus:
    ```
    ./mvnw clean test -Pauto -Dtags="basic || basic-istanbul || networks/typical::istanbul"
    ```
- Force destroy the network after running tests:
    ```
    ./mvnw clean test -Pauto -Dtags="basic || basic-raft || networks/typical::raft" -Dnetwork.forceDestroy=true
    ```
- Start the network without running tests:
    ```
    ./mvnw process-test-resources -Pauto -Dnetwork.target="networks/typical::raft"
    ```
- Destroy the network:
    ```
    ./mvnw exec:exec@network.terraform-destroy -Pauto -Dnetwork.folder="networks/typical" -Dnetwork.profile=raft
    ```

Below is the summary of various parameters:

| Parameters | Description |
|------------|-------------|
| `-Dnetwork.target="<folder>::<profile"` | Shorthand to specify the Terraform folder and profile being used to create GoQuorum Network |
| `-Dnetwork.folder="<folder>"` | Terraform folder being used to create GoQuorum Network |
| `-Dnetwork.profile="<profile>"` | Terraform workspace and `terraform.<profile>.tfvars` being used |
| `-Dnetwork.forceDestroy` | Destroy the GoQuorum Network after test completed. Default is `false` |
| `-Dnetwork.skipApply` | Don't create GoQuorum Network. Default is `false` |
| `-Dnetwork.skipWait` | Don't perform health check and wait for GoQuorum Network. Default is `false` |
| `-Dinfra.target="<folder>::<profile"` | Shorthand to specify the Terraform folder and profile being used to create an infrastructure to host Docker Engine |
| `-Dinfra.folder="<folder>"` | Terraform folder being used to create the infrastructure |
| `-Dinfra.profile="<profile>"` | Terraform workspace and `terraform.<profile>.tfvars` being used |
| `-Dinfra.forceDestroy` | Destroy the infrastructure after test completed. Default is `false` |
| `-Dinfra.skipApply` | Don't create the infrastructure. Default is `false` |
| `-Dinfra.skipWait` | Don't perform health check and wait for GoQuorum Network. Default is `false` |
| `-DskipToolsCheck` | Don't check local tools required to run tests. Default is `false` |
| `-DskipGenerateSol` | Don't generate Java stubs for Solidity files. Default is `false`. When running Permission spec, this should not be set to `true` as the spec depends on version of Solidity files |

### With local binaries

In order to run acceptance tests during GoQuorum/Tessera development:

- Build GoQuorum/Tessera binaries locally targeting Linux arch.
  E.g.: GoQuorum binaries are in `/xyz/go-ethereum/build/bin` folder and Tessera jar file is in `/abc/tessera/tessera-dist/tessera-app/target`
- Mount binaries dynamically to overrride existing ones in the containers
  > :bulb: Indices 0,1,2,3.. indicate Node id which you want to use the local binaries
  - GoQuorum:
  ```
  export QUORUM_DEV_LOCAL='{host_path="/xyz/go-ethereum/build/bin", container_path="/usr/local/bin"}'
  export TF_VAR_additional_quorum_container_vol="{0=[$QUORUM_DEV_LOCAL],1=[$QUORUM_DEV_LOCAL],2=[$QUORUM_DEV_LOCAL],3=[$QUORUM_DEV_LOCAL]}"
  ````
  - Tessera:
  ```
  export TESSERA_DEV_LOCAL='{host_path="/abc/tessera/tessera-dist/tessera-app/target", container_path="/tessera"}'
  export TESSERA_APP_DEV_LOCAL='"/tessera/tessera-app-20.10.1-SNAPSHOT-app.jar"'
  export TF_VAR_additional_tessera_container_vol="{0=[$TESSERA_DEV_LOCAL],1=[$TESSERA_DEV_LOCAL],2=[$TESSERA_DEV_LOCAL],3=[$TESSERA_DEV_LOCAL]}"
  export TF_VAR_tessera_app_container_path="{0=$TESSERA_APP_DEV_LOCAL,1=$TESSERA_APP_DEV_LOCAL,2=$TESSERA_APP_DEV_LOCAL,3=$TESSERA_APP_DEV_LOCAL}"
  ```

### With custom GoQuorum/Tessera Docker images

By default, official Docker images `quorumengineering/quorum:develop` and `quorumengineering/tessera:develop` in [Docker Hub](https://hub.docker.com/u/quorumengineering) will be used.
If you need to use your custom images, please follow the below guides:

- Name the branch with prefix `dev-`. E.g.: `dev-mybranch`
- Push custom GoQuorum/Tessera Docker images to [Github Container Registry](https://docs.github.com/en/packages/guides/pushing-and-pulling-docker-images) of this repo with image name and version convention
  - GoQuorum: `quorum-dev-mybranch:develop`
  - Tessera: `tessera-dev-mybranch:develop`
- Pushing changes to `dev-mybranch` will kick off github Action workflow running tests using custom images

### With existing `quorum-examples` network

```
SPRING_PROFILES_ACTIVE=local.7nodes ./mvnw clean test -Dtags="basic || basic-raft || networks/typical::raft"
```

## Remote Docker

:information_source: Because Docker Java SDK [doesn't support SSH transport](https://github.com/docker-java/docker-java/issues/1130) hence we need to open TCP endpoint. 

`networks/_infra/aws-ec2` provides Terraform configuration in order to spin off an EC2 instance with remote Docker API
support.

E.g.: To start `networks/typical` with remote Docker infrastructure:

- Make sure you have the right AWS credentials in your environment
- Run: 
    ```
    ./mvnw process-test-resources -Pauto -Dnetwork.target="networks/typical::raft" -Dinfra.target="networks/_infra/aws-ec2::us-east-1"
    ```

## Logging

- Set environment variable: `LOGGING_LEVEL_COM_QUORUM_GAUGE=DEBUG`

------

[![Gauge Badge](https://gauge.org/Gauge_Badge.svg)](https://gauge.org)
