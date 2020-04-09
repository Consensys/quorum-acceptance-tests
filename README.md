
## Prerequisites

* Java 11+
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
   - Refer to [this guide](https://www.terraform.io/docs/configuration/providers.html#third-party-plugins) on how to install the provider manually

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
    mvn clean test -Pauto -Dtags="basic || basic-raft || networks/typical::raft" -Dauto.forceDestroy=true
    ```
* Start the network without running tests:
    ```
    mvn process-test-resources -Pauto -Dauto.usingNetwork="networks/typical" -Dauto.networkProfile=raft
    ```
* Destroy the network:
    ```
    mvn exec:exec@terraform-destroy -Pauto -Dauto.usingNetwork="networks/typical" -Dauto.networkProfile=raft
    ```

### With existing `quorum-examples` network

```
SPRING_PROFILES_ACTIVE=local.7nodes mvn clean test -Dtags="basic || basic-raft || networks/typical::raft"
```

## Logging

* Set environment variable: `LOGGING_LEVEL_COM_QUORUM_GAUGE=DEBUG`

------

[![Gauge Badge](https://gauge.org/Gauge_Badge.svg)](https://gauge.org)
