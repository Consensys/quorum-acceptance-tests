#!/bin/bash

use_docker="true"

sh cleanup.sh

rm -rf TEST-LOOP-FAILED-*
rm -rf out.txt

#for COUNTER in $(seq 1 100)
#do
  echo >> out.txt
  echo >> out.txt
  echo >> out.txt
  echo >> out.txt
  echo >> out.txt
  echo >> out.txt
  date >> out.txt
  date
  echo "run count $COUNTER" >> out.txt
  echo "run count $COUNTER"
  declare -a t=( "basic || basic-raft || (advanced && raft) || networks/typical::raft" "basic || basic-istanbul || (advanced && istanbul) || networks/typical::istanbul" "gcmode && block-sync && networks/template::raft-3plus1" "gcmode && block-sync && networks/template::istanbul-3plus1" "learner-peer-management || raftdnsenable && networks/template::raft-3plus1" "validator-management && networks/template::istanbul-3plus1" "basic || basic-raft || (advanced && raft) || networks/plugins::raft" "basic || basic-istanbul || (advanced && istanbul) || networks/plugins::istanbul" "basic || basic-raft || (advanced && raft) || networks/plugins::raft-account-plugin-hashicorp-vault" "basic || basic-istanbul || (advanced && istanbul) || networks/plugins::istanbul-account-plugin-hashicorp-vault" "basic-rpc-security || networks/plugins::raft-rpc-security" "basic-rpc-security || networks/plugins::istanbul-rpc-security" "migration && networks/template::raft-4nodes" "migration && networks/template::istanbul-4nodes" "migration && networks/template::raft-4nodes-ancientdb" "migration && networks/template::istanbul-4nodes-ancientdb" "permissions-v1 && networks/template::raft-3plus1" "basic || basic-raft || networks/typical::raft" "permissions-v2 && networks/template::raft-3plus1" "privacy-enhancements-upgrade || networks/template::raft-4nodes-pe" "privacy-enhancements-upgrade || networks/template::istanbul-4nodes-pe" "multitenancy && networks/plugins::raft-multitenancy" "basic || basic-raft || (advanced && raft) || networks/typical::raft-simple-mps" "basic || basic-istanbul || (advanced && istanbul) || networks/typical::istanbul-simple-mps" "basic || networks/typical::raftmps" "basic || networks/typical::istanbulmps" "mps-upgrade-txtrace || networks/template::raft-4nodes-mps" "mps-upgrade-txtrace || networks/template::istanbul-4nodes-mps" "(basic && !nosupport && !mps && !spam && !eth-api-signed && !privacy-enhancements-disabled && !graphql && !async && !extension && !storage-root && !get-quorum-payload && !personal-api-signed) || networks/typical-besu::ibft2" )
  for tags in "${t[@]}";
  do
    echo $tags
    pe="false"
    if [ "$tag" == "(basic && !privacy-enhancements-disabled) || privacy-enhancements || basic-raft || (advanced && raft) || networks/typical::raft" ];
    then
      pe="true"
    fi
    if [ "$tag" == "(basic && !privacy-enhancements-disabled) || privacy-enhancements || basic-istanbul || (advanced && istanbul) || networks/typical::istanbul" ];
    then
      pe="true"
    fi
    if [ "$tag" == "(basic && !privacy-enhancements-disabled && !async && !extension && !storage-root && !get-quorum-payload && !personal-api-signed && !spam) || networks/typical-besu::ibft2" ];
    then
      pe="true"
    fi
    if [ "$use_docker" == "true" ];
    then
      docker run --rm --network host -e TF_VAR_privacy_enhancements="{block=0, enabled=$pe}" -v /var/run/docker.sock:/var/run/docker.sock -v /tmp/acctests:/tmp/acctests quorumengineering/acctests:develop test -Pauto -Dtags="$tags" -Dauto.outputDir=/tmp/acctests -Dnetwork.forceDestroy=true
    else
      export TF_VAR_privacy_enhancements="{block=0, enabled=$pe}"
      mvn clean test -Pauto -Dtags="$tags" >> out.txt
    fi
    if [ $? -eq 0 ]
    then
      echo "Success: $COUNTER"
    else
      echo "Failure: $COUNTER"
      echo "Failure Info:" >> out.txt
      docker ps -a >> out.txt
      mkdir "TEST-LOOP-FAILED-$COUNTER"
      for containerId in $(docker ps -qa)
      do
          echo "container: $containerId" >> out.txt
          containerName=$(docker container inspect -f='{{ .Name}}' $containerId)
          containerOut="TEST-LOOP-FAILED-${COUNTER}${containerName}.log"
          echo "containerOutputFile: $containerOut" >> out.txt
          docker container inspect $containerId >> $containerOut
          docker container logs $containerId >> $containerOut 2>&1
      done
      exit 1
    fi
    sleep 2s
    cleanup.sh >> out.txt 2>&1
  done
#done
