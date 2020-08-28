![Run Acceptance Tests](https://github.com/jpmorganchase/quorum-acceptance-tests/workflows/Run%20Acceptance%20Tests/badge.svg?branch=master)

# Quick Start

- Install [Docker Engine](https://docs.docker.com/engine/) or [Docker Desktop](https://www.docker.com/products/docker-desktop)
- Run basic acceptance tests against a new Quorum network using Raft consensus:
    ```
    docker run --rm --network host -v /var/run/docker.sock:/var/run/docker.sock -v /tmp/acctests:/tmp/acctests \
            quorumengineering/acctests:latest test -Pauto -Dtags="basic || networks/typical::raft" \
            -Dauto.outputDir=/tmp/acctests -Dnetwork.forceDestroy=true
    ```

# Development

Development environment requires the following:

* JDK 11+
* Maven 3.6.x
* [Solidity Compiler](https://solidity.readthedocs.io/en/latest/installing-solidity.html) (make sure `solc` is installed and not `solcjs`)
  * For MacOS: use `brew`
  * For Linux: use `apt`, `snap` or `emerge`
  * For Windows: download from [here](https://github.com/ethereum/solidity/releases)
* [Gauge](https://gauge.org/get_started)

With built-in provisioning feature:
* [Docker Engine](https://docs.docker.com/engine/) or [Docker Desktop](https://www.docker.com/products/docker-desktop)
* [Terraform](https://terraform.io) 0.12.x
* [Terraform Provider Quorum](https://bintray.com/quorumengineering/terraform/terraform-provider-quorum)
   - The provider should be downloaded from the link and unzipped into the directory `~/.terraform.d/plugins/`
   - Refer to [this guide](https://www.terraform.io/docs/configuration/providers.html#third-party-plugins) for more information regarding provider installation

**For more details on tools and versions being used, please refer to [Dockerfile](Dockerfile)**

## Writing Tests

* Using [Gauge](https://github.com/getgauge/gauge) test automation framework
* Test Specs are stored in [`src/specs`](src/specs) folder
  * Folder `01_basic` contains specifications which describe Quorum's basic functionalities. All specifications must be tagged as `basic`
  * Folder `02_advanced` contains specifications which are for making sure Quorum's basic functionalities are working under different conditions in the chain. All specifications must be tagged as `advanced`
* Glue codes are written in Java under [`src/test/java`](src/test/java) folder
* Tests are generally written against 4-node Quorum network

## Running Tests

### With auto-provisioning network

> :bulb: Tag `networks/typical::raft` means:
> - Executing tests which are tagged with the value
> - Instructing Maven to provision `networks/typical` with profile `raft` when using Maven Profile `auto` (i.e.: `-Pauto`)
> - `networks/typical` is a folder that contains Terraform configuration to provision the network

* Run basic tests for raft consensus: 
    ```
    mvn clean test -Pauto -Dtags="basic || basic-raft || networks/typical::raft"
    ```
* Run basic tests for istanbul consensus:
    ```
    mvn clean test -Pauto -Dtags="basic || basic-istanbul || networks/typical::istanbul"
    ```
* Force destroy the network after running tests:
    ```
    mvn clean test -Pauto -Dtags="basic || basic-raft || networks/typical::raft" -Dnetwork.forceDestroy=true
    ```
* Start the network without running tests:
    ```
    mvn process-test-resources -Pauto -Dnetwork.target="networks/typical::raft"
    ```
* Destroy the network:
    ```
    mvn exec:exec@network.terraform-destroy -Pauto -Dnetwork.folder="networks/typical" -Dnetwork.profile=raft
    ```

Below is the summary of various parameters:

| Parameters | Description |
|------------|-------------|
| `-Dnetwork.target="<folder>::<profile"` | Shorthand to specify the Terraform folder and profile being used to create Quorum Network |
| `-Dnetwork.folder="<folder>"` | Terraform folder being used to create Quorum Network |
| `-Dnetwork.profile="<profile>"` | Terraform workspace and `terraform.<profile>.tfvars` being used |
| `-Dnetwork.forceDestroy="true" or "false"` | Destroy the Quorum Network after test completed. Default is `false` |
| `-Dnetwork.skipApply="true" or "false"` | Don't create Quorum Network. Default is `false` |
| `-Dnetwork.skipWait="true" or "false"` | Don't perform health check and wait for Quorum Network. Default is `false` |
| `-Dinfra.target="<folder>::<profile"` | Shorthand to specify the Terraform folder and profile being used to create an infrastructure to host Docker Engine |
| `-Dinfra.folder="<folder>"` | Terraform folder being used to create the infrastructure |
| `-Dinfra.profile="<profile>"` | Terraform workspace and `terraform.<profile>.tfvars` being used |
| `-Dinfra.forceDestroy="true" or "false"` | Destroy the infrastructure after test completed. Default is `false` |
| `-Dinfra.skipApply="true" or "false"` | Don't create the infrastructure. Default is `false` |
| `-Dinfra.skipWait="true" or "false"` | Don't perform health check and wait for Quorum Network. Default is `false` |

### With existing `quorum-examples` network

```
SPRING_PROFILES_ACTIVE=local.7nodes mvn clean test -Dtags="basic || basic-raft || networks/typical::raft"
```

## Remote Docker

:information_source: Because Docker Java SDK [doesn't support SSH transport](https://github.com/docker-java/docker-java/issues/1130) hence we need to open TCP endpoint. 

`networks/_infra/aws-ec2` provides Terraform configuration in order to spin off an EC2 instance with remote Docker API
support.

E.g.: To start `networks/typical` with remote Docker infrastructure:

- Make sure you have the right AWS credentials in your environment
- Run: 
    ```
    mvn process-test-resources -Pauto -Dnetwork.target="networks/typical::raft" -Dinfra.target="networks/_infra/aws-ec2::us-east-1"
    ```


## Logging

* Set environment variable: `LOGGING_LEVEL_COM_QUORUM_GAUGE=DEBUG`

------

[![Gauge Badge](https://gauge.org/Gauge_Badge.svg)](https://gauge.org)
