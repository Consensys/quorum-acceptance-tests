consensus = "istanbul"
network_name = "plugins-istanbul"
plugins = {
    //    helloworld = {
    //    name = "quorum-plugin-hello-world-go"
    //    version = "1.0.0"
    //    expose_api = true
    //    },
    account = {
        name = "quorum-account-plugin-hashicorp-vault"
        version = "1.0.0"
        expose_api = true
    }
}
quorum_docker_image = {
    name = "quorumengineering/quorum:hashicorp"
    local = true
}
