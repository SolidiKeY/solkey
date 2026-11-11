// SPDX-License-Identifier: MIT
pragma solidity ^0.8.26;

// Source: https://github.com/Cyfrin/solidity-by-example.github.io/blob/5bcdca0239409d7336a07b66a6fca8d0bcc710e6/contracts/src/sending-ether/SendingEther.sol
// The real-world/SendEther.sol port with its @custom:key specification removed.
contract SendEther {
    function sendViaTransfer(address payable _to) public payable {
        _to.transfer(msg.value);
    }

    function sendViaSend(address payable _to) public payable {
        bool sent = _to.send(msg.value);
        require(sent);
    }

    function sendViaCall(address payable _to) public payable {
        (bool sent,) = _to.call{value: msg.value}("");
        require(sent);
    }
}
