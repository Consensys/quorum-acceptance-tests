[![Build Status](https://travis-ci.org/jpmorganchase/quorum-acceptance-tests.svg?branch=master)](https://travis-ci.org/jpmorganchase/quorum-acceptance-tests)

Latest test reports are available:
* [Specifications](https://jpmorganchase.github.io/quorum-acceptance-tests/docs/html/)
* [Raft](https://jpmorganchase.github.io/quorum-acceptance-tests/raft/)
* [Istanbul](https://jpmorganchase.github.io/quorum-acceptance-tests/istanbul/)

## Java
### Prerequisites

* Java 8
* Maven 3.5.x
* [Solidity Compiler](https://solidity.readthedocs.io/en/latest/installing-solidity.html)
* [Gauge](https://gauge.org/get_started)

### Writing Tests

* Using [Gauge](https://github.com/getgauge/gauge) test automation framework
* Test Specs are stored in [`src/specs`](src/specs) folder
  * Folder `01_basic` contains specifications which describe Quorum's basic functionalities. All specifications must be tagged as `basic`
  * Folder `02_advanced` contains specifications which are for making sure Quorum's basic functionalities are working under different conditions in the chain. All specifications must be tagged as `advanced`
* Glue codes are written in Java under [`src/test/java`](src/test/java) folder


### Running Tests

* When `quorum-cloud` is used to provision Quorum Network:
  * Start SOCKS proxy for SSH tunneling. E.g.: listening on port `5000`
    ```
    $ ssh -D 5000 -N -o ServerAliveInterval=30 -i <private_key> ec2-user@<bastion node>
    ```
  * Obtain the nodes metadata from Bastion Node (`/qdata/quorum_metadata`) and create a file `config/application-local.yml` with content similar to a sample file in [`config`](config) folder.
* When using `quorum-examples`, rename `config/application-local.7nodes.yml` to `config/application-local.yml`
* Run `mvn clean test`
  * In Travis, `basic` and `advanced` specifications (targetting the consensus used) are run
  * In local, only `basic` specifications are run. Refer to [Gauge documentation](https://docs.gauge.org/latest/execution.html) and [Gauge Maven Plugin](https://github.com/getgauge/gauge-maven-plugin) to see how to run specs selectively.
    E.g.: `mvn test -Dtags="advanced && istanbul"` to run only `advanced` specifications and scenarios tagged with `istanbul`

### Logging

* Set environment variable: `LOGGING_LEVEL_COM_QUORUM_GAUGE=DEBUG`

## Javascript

### Prerequisites
*  [Gauge](https://gauge.org/get_started)
* [npm](https://www.npmjs.com/get-npm)


### Writing Tests

* Using [Gauge](https://github.com/getgauge/gauge) test automation framework
* Test Specs are stored in [`src/specs/js`](src/specs/js) folder
 * Step implementation are written in Javascript under [`src/main/js`](src/main/js) folder

### Running Tests

* When using `quorum-examples` locally, update `src/main/js/config.js` with correct configuration.
* set language in `manifest.json` to `js`
* Run 
    ```
    npm install (only once to install dependencies)
    npm start
    ```


------

[![Gauge Badge](https://gauge.org/Gauge_Badge.svg)](https://gauge.org)
