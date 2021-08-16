# Pluggable Architecture with hello-world plugin

 Tags: networks/plugins::istanbul, networks/plugins::qbft, networks/plugins::raft

Plugin, implementing `helloworld` plugin interface, exposes an API which is delegated via JSON RPC

    | node  | name  | expected     |
    | Node1 | John  | Hello John!  |
    | Node2 | Maria | Hello Maria! |
    | Node3 | Bob   | Hello Bob!   |
    | Node4 | Liz   | Hello Liz!   |

## `plugin@helloworld_greeting` API is successfully invoked

* Calling `plugin@helloworld_greeting` API in <node> with single parameter <name> must return <expected>
