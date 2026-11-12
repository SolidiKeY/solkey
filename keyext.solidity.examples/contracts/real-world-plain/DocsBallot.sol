// SPDX-License-Identifier: GPL-3.0
pragma solidity >=0.7.0 <0.9.0;

// Source: https://github.com/ethereum/solidity/blob/0944010b9e19e8717b7d40dd4a22a8586713631b/docs/examples/voting.rst
// The real-world/DocsBallot.sol and real-world/open/DocsBallot.sol ports with their specification
// written as require and assert. winnerName binds winningProposal()'s result to a local, the
// witness of its two \exists clauses.
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

    /// @custom:key box
    function giveRightToVote(address voter, address a, uint i) external {
        require(voters[a].weight >= 0);
        require(i >= proposals.length || proposals[i].voteCount >= 0);
        address oldChairperson = chairperson;
        bool oldVoted = voters[voter].voted;
        uint oldWeight = voters[voter].weight;

        require(msg.sender == chairperson);
        require(!voters[voter].voted);
        require(voters[voter].weight == 0);
        voters[voter].weight = 1;

        assert(oldChairperson == msg.sender && chairperson == oldChairperson);
        assert(oldVoted == false && oldWeight == 0);
        assert(voters[voter].weight == 1 && voters[voter].voted == false);
        assert(voters[a].weight >= 0);
        assert(i >= proposals.length || proposals[i].voteCount >= 0);
    }

    /// @custom:key box
    function delegate(address to, address a, uint i) external {
        require(voters[a].weight >= 0);
        require(i >= proposals.length || proposals[i].voteCount >= 0);
        require(voters[msg.sender].weight >= 0);
        uint oldWeight = voters[msg.sender].weight;
        bool oldVoted = voters[msg.sender].voted;
        uint oldAWeight = voters[a].weight;
        bool oldAVoted = voters[a].voted;
        uint oldVoteCount = i < proposals.length ? proposals[i].voteCount : 0;

        Voter storage sender = voters[msg.sender];
        require(sender.weight != 0);
        require(!sender.voted);

        require(to != msg.sender);

        /// @custom:key invariant to != msg.sender
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

        assert(oldWeight != 0 && oldVoted == false);
        assert(voters[msg.sender].voted == true && voters[msg.sender].delegate != msg.sender);
        assert(voters[voters[msg.sender].delegate].delegate == address(0));
        assert(a != voters[msg.sender].delegate || oldAVoted || voters[a].weight == oldAWeight + oldWeight);
        assert(a != voters[msg.sender].delegate || !oldAVoted || i != voters[a].vote
            || i >= proposals.length || proposals[i].voteCount == oldVoteCount + oldWeight);
        assert(voters[a].weight >= 0);
        assert(i >= proposals.length || proposals[i].voteCount >= 0);
    }

    /// @custom:key box
    function vote(uint proposal, address a, uint i) external {
        require(voters[a].weight >= 0);
        require(i >= proposals.length || proposals[i].voteCount >= 0);
        require(voters[msg.sender].weight >= 0);
        require(proposal >= 0);
        uint oldWeight = voters[msg.sender].weight;
        bool oldVoted = voters[msg.sender].voted;
        uint oldVoteCount = proposals[proposal].voteCount;

        Voter storage sender = voters[msg.sender];
        require(sender.weight != 0);
        require(!sender.voted);
        sender.voted = true;
        sender.vote = proposal;

        uint w = sender.weight;
        proposals[proposal].voteCount += w;

        assert(oldWeight != 0 && oldVoted == false);
        assert(voters[msg.sender].voted == true && voters[msg.sender].vote == proposal);
        assert(proposals[proposal].voteCount == oldVoteCount + oldWeight);
        assert(voters[a].weight >= 0);
        assert(i >= proposals.length || proposals[i].voteCount >= 0);
    }

    /// @custom:key box
    function winningProposal(address a, uint i) public view
            returns (uint winningProposal_)
    {
        require(voters[a].weight >= 0);
        require(i >= proposals.length || proposals[i].voteCount >= 0);
        require(proposals.length == 0 || proposals[0].voteCount >= 0);

        uint winningVoteCount = 0;
        /// @custom:key invariant 0 <= p && p <= proposals.length && (winningProposal_ == 0 || winningProposal_ < p)
        /// @custom:key invariant \forall uint k; 0 <= k && k < p -> proposals[k].voteCount <= winningVoteCount
        /// @custom:key invariant winningVoteCount == 0 && winningProposal_ == 0 || winningVoteCount == proposals[winningProposal_].voteCount
        for (uint p = 0; p < proposals.length; p++) {
            if (proposals[p].voteCount > winningVoteCount) {
                winningVoteCount = proposals[p].voteCount;
                winningProposal_ = p;
            }
        }

        assert(winningProposal_ == 0 || winningProposal_ < proposals.length);
        assert(i >= proposals.length || proposals[i].voteCount <= proposals[winningProposal_].voteCount);
        assert(voters[a].weight >= 0);
        assert(i >= proposals.length || proposals[i].voteCount >= 0);
    }

    /// @custom:key box
    function winnerName(address a, uint i) external view
            returns (uint256 winnerName_)
    {
        require(voters[a].weight >= 0);
        require(i >= proposals.length || proposals[i].voteCount >= 0);
        require(proposals.length == 0 || proposals[0].voteCount >= 0);

        uint w = winningProposal(a, i);
        winnerName_ = proposals[w].name;

        assert(w < proposals.length && winnerName_ == proposals[w].name);
        assert(i >= proposals.length
            || (w < proposals.length && winnerName_ == proposals[w].name && proposals[i].voteCount <= proposals[w].voteCount));
        assert(voters[a].weight >= 0);
        assert(i >= proposals.length || proposals[i].voteCount >= 0);
    }
}
