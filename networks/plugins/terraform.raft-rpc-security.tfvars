consensus = "raft"
plugins = {
  security = {
    name       = "quorum-security-plugin-enterprise"
    version    = "0.1.2"
    expose_api = false
  }
}
enable_privacy_marker_tx = { block = 0, enabled = true }
