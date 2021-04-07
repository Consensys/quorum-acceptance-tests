consensus = "raft"
plugins = {
    helloworld = {
        name = "quorum-plugin-hello-world-go"
        version = "1.0.0"
        expose_api = true
    }
}
enable_privacy_marker_tx = { block = 0, enabled = true }
