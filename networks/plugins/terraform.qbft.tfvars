consensus = "istanbul"
qbftBlock = { block = 0, enabled = true }
plugins = {
  helloworld = {
    name       = "quorum-plugin-hello-world-go"
    version    = "1.0.0"
    expose_api = true
  }
}
