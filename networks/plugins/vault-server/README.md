# vault-server

This directory contains the storage for an initialised filesystem-backed Vault for use in acceptance-test networks.  As the credentials are used openly throughout this project **this Vault is not safe for production use**. 
  
The Vault has the following setup:

| Property | Value |
| --- | --- |
| Unseal key | `Xg/nHOs0/uuckKjcszobas4aVNjFxyRP4GtsmlmnV4U=` |
| Root token | `s.TZG2LuIkjcT9AYRNZfHrmuQn` |
| kv-v2 secret engine path | `kv` | 

## Usage

### Typical configuration 

```hcl
storage "file" {
	path = "/path/to/vault-storage"
}

listener "tcp" {
        tls_disable = 1
        tls_min_version = "tls12"
        tls_cert_file = "/path/to/server.key"
        tls_key_file = "/path/to/server.pem"
        tls_require_and_verify_client_cert = "true"
        tls_client_ca_file = "/path/to/ca.pem"
}
```

### Using with Terraform network provisioning

1. Create a `vault` Docker container
1. Mount `vault-storage` to the container
1. Mount `CertsWithQuorumRootCAandIntCA` to the container
1. Create a `config.hcl` in the container's `/vault/config/` directory
    1. Replace the paths in the above template with those created by the previous mounts 
