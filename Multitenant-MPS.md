# Multitenant with multiple private states

The new acceptance tests follow the logic detailed [here](https://github.com/ConsenSys/quorum-examples/blob/master/examples/7nodes/MultitenancyAndMultiplePrivateStates.md). It is mainly an automated way to run a similar scenario.

There are two test specs:
* private_accumulator_smart_contract.spec
* private_accumulator_smart_contract-standalone.spec

The logic in both of them should be exactly the same (the only difference should be the parameters they use to identify nodes/states). 
The standalone version execises the private states on Node1, Node2 and Node3. The other one exercises the multiple states from Node1 (PS1, PS2 and PS3).

The tests prove that the experience of interacting with multiple private states on Node1 is similar to interacting with separate quorum nodes. 

## Running with quorum examples

Build [quorum](https://github.com/ConsenSys/quorum) and [tessera](https://github.com/ConsenSys/tessera).

Start a raft network with [quorum examples](https://github.com/ConsenSys/quorum-examples/blob/master/examples/7nodes/MultitenancyAndMultiplePrivateStates.md).

Run the acceptance tests exercising the multiple private states on Node1. 
```
SPRING_PROFILES_ACTIVE=local.4nodes-mps mvn clean test -Dtags="mps"
```

After running the tests you can connect to Node1 and query a specific private state (use the PSI env variable to attach).

## Running with vanilla quorum and tessera (standalone)

This runs the standalone tests using the "latest" quorum and tessera docker images.

Pull the latest versions for quorum and tessera docker images.
```
docker pull quorumengineering/tessera:latest
docker pull quorumengineering/quorum:latest
```

Run the standalone acceptance tests.
```
export TF_VAR_tessera_docker_image='{ name = "quorumengineering/tessera:latest", local = true }'
export TF_VAR_quorum_docker_image='{ name = "quorumengineering/quorum:latest", local = true }'

mvn clean test -Pauto -Dtags="mps-standalone || networks/typical::raft" -Dnetwork.forceDestroy=true
```

