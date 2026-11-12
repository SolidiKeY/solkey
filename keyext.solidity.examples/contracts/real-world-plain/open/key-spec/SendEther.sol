// SPDX-License-Identifier: MIT
pragma solidity ^0.8.26;

// Source: https://github.com/Cyfrin/solidity-by-example.github.io/blob/5bcdca0239409d7336a07b66a6fca8d0bcc710e6/contracts/src/sending-ether/SendingEther.sol
// The real-world/SendEther.sol port with its specification written as require and assert. Each
// clause with no Solidity form stays a @custom:key clause.
contract SendEther {
    /// @custom:key ensures _to != msg.sender && _to != address(this) -> net(_to) == \old(net(_to)) - msg.value && net(msg.sender) == \old(net(msg.sender)) + msg.value
    /// @custom:key ensures _to == msg.sender -> net(_to) == \old(net(_to))
    /// @custom:key ensures _to == address(this) -> net(_to) == \old(net(_to)) && net(msg.sender) == \old(net(msg.sender)) + msg.value
    /// @custom:key box
    function sendViaTransfer(address payable _to) public payable {
        _to.transfer(msg.value);
    }

    /// @custom:key ensures _to != msg.sender && _to != address(this) -> net(_to) == \old(net(_to)) - msg.value && net(msg.sender) == \old(net(msg.sender)) + msg.value
    /// @custom:key ensures _to == msg.sender -> net(_to) == \old(net(_to))
    /// @custom:key ensures _to == address(this) -> net(_to) == \old(net(_to)) && net(msg.sender) == \old(net(msg.sender)) + msg.value
    /// @custom:key box
    function sendViaSend(address payable _to) public payable {
        bool sent = _to.send(msg.value);
        require(sent);
    }

    /// @custom:key ensures _to != msg.sender && _to != address(this) -> net(_to) == \old(net(_to)) - msg.value && net(msg.sender) == \old(net(msg.sender)) + msg.value
    /// @custom:key ensures _to == msg.sender -> net(_to) == \old(net(_to))
    /// @custom:key ensures _to == address(this) -> net(_to) == \old(net(_to)) && net(msg.sender) == \old(net(msg.sender)) + msg.value
    /// @custom:key box
    function sendViaCall(address payable _to) public payable {
        (bool sent,) = _to.call{value: msg.value}("");
        require(sent);
    }
}
