// SPDX-License-Identifier: GPL-3.0
pragma solidity >=0.7.0 <0.9.0;

// Source: https://github.com/ethereum/solidity/blob/0944010b9e19e8717b7d40dd4a22a8586713631b/docs/examples/voting.rst
// The real-world/DocsBallot.sol port with its @custom:key specification removed.
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

    constructor(uint256[] memory proposalNames) {
        chairperson = msg.sender;
        voters[chairperson].weight = 1;
        uint256[] memory names = proposalNames;
        for (uint i = 0; i < names.length; i++) {
            proposals.push();
            proposals[proposals.length - 1].name = names[i];
            proposals[proposals.length - 1].voteCount = 0;
        }
    }

    function giveRightToVote(address voter) external {
        require(msg.sender == chairperson);
        require(!voters[voter].voted);
        require(voters[voter].weight == 0);
        voters[voter].weight = 1;
    }

    function delegate(address to) external {
        Voter storage sender = voters[msg.sender];
        require(sender.weight != 0);
        require(!sender.voted);

        require(to != msg.sender);

        while (voters[to].delegate != address(0)) {
            to = voters[to].delegate;
            require(to != msg.sender);
        }

        Voter storage delegate_ = voters[to];

        require(delegate_.weight >= 1);

        sender.voted = true;
        sender.delegate = to;

        if (delegate_.voted) {
            uint w = sender.weight;
            proposals[delegate_.vote].voteCount += w;
        } else {
            uint w = sender.weight;
            delegate_.weight += w;
        }
    }

    function vote(uint proposal) external {
        Voter storage sender = voters[msg.sender];
        require(sender.weight != 0);
        require(!sender.voted);
        sender.voted = true;
        sender.vote = proposal;

        uint w = sender.weight;
        proposals[proposal].voteCount += w;
    }

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
