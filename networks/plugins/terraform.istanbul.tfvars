consensus = "istanbul"
plugins = {
        helloworld = {
        name = "quorum-plugin-hello-world-go"
        version = "1.0.0"
        expose_api = true
        },
    account = {
        name = "quorum-account-plugin-hashicorp-vault"
        version = "1.0.0"
        expose_api = true
    }
}
//TODO(cjh) a local tag of quorum with the vault plugin changes - remove once the quorum plugin change is public
quorum_docker_image = {
    name = "quorumengineering/quorum:hashicorp"
    local = true
}
