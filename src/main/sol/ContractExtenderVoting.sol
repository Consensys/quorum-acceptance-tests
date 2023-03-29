pragma solidity ^0.5.0;

interface ContractExtenderVoting {
    function doVote(bool vote, string calldata nextuuid) external;
}
