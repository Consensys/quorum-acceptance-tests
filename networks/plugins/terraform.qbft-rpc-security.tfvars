consensus = "istanbul"
qbftBlock = { block = 0, enabled = true }
plugins = {
  security = {
    name       = "quorum-security-plugin-enterprise"
    version    = "0.1.2"
    expose_api = false
  }
}
