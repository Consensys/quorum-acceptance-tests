### Prerequisites

* Java 8
* Maven 3.5.x
* Solidity Compiler

### Getting Started

* If `quorum-cloud` is used, start SOCKS proxy for SSH tunneling
  ```
  $ ssh -D 5000 -N -o ServerAliveInterval=30 -i <private_key> ec2-user@<bastion node>
  ```
  And configure environment variables:
  ```
  QUORUM_SOCKS_PROXY_HOST=localhost
  QUORUM_SOCKS_PROXY_PORT=5000
  ```