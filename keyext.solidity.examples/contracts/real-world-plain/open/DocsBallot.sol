// SPDX-License-Identifier: GPL-3.0
pragma solidity >=0.7.0 <0.9.0;

// Source: https://github.com/ethereum/solidity/blob/0944010b9e19e8717b7d40dd4a22a8586713631b/docs/examples/voting.rst
// The real-world/open/DocsBallot.sol port with its @custom:key specification removed.
contract DocsBallot {
    struct Voter {
        uint weight;
        bool voted;
        address delegate;
        uint vote;
    }

    struct Proposal {
        uint256 name;
        uint voteCount;
    }

    address public chairperson;

    mapping(address => Voter) public voters;

    Proposal[] public proposals;

    function winningProposal() public view
            returns (uint winningProposal_)
    {
        uint winningVoteCount = 0;
        for (uint p = 0; p < proposals.length; p++) {
            if (proposals[p].voteCount > winningVoteCount) {
                winningVoteCount = proposals[p].voteCount;
                winningProposal_ = p;
            }
        }
    }

    function winnerName() external view
            returns (uint256 winnerName_)
    {
        winnerName_ = proposals[winningProposal()].name;
    }
}
