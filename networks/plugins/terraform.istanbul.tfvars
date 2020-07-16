consensus = "istanbul"
plugins = {
        helloworld = {
        name = "quorum-plugin-hello-world-go"
        version = "1.0.0"
        expose_api = true
        },
    account = {
        name = "quorum-account-plugin-hashicorp-vault"
        version = "0.0.1"
        expose_api = true
    }
}
