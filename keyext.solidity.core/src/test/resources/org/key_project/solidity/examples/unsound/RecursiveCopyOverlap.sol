// SPDX-License-Identifier: GPL-2.0-only
pragma solidity ^0.8.0;

contract RecursiveCopyOverlap {
    struct Node { uint v; Node[] kids; }

    Node nodeTree;

    function copyIntoOwnChild() public {
        delete nodeTree;
        nodeTree.kids.push();
        nodeTree.kids[0] = nodeTree;
    }
}
