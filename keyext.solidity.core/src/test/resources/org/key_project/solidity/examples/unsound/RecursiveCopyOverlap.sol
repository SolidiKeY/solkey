// SPDX-License-Identifier: GPL-2.0-only
pragma solidity ^0.8.0;

contract RecursiveCopyOverlap {
    struct Node { uint v; Node[] kids; }

    Node nodeTree;

    function copyFromOwnChild() public {
        delete nodeTree;
        nodeTree.kids.push();
        nodeTree.kids[0].kids.push();
        nodeTree.kids[0].kids.push();
        nodeTree.kids[0].kids[1].v = 5;
        nodeTree = nodeTree.kids[0];
        assert(nodeTree.kids[1].v == 5);
    }

    function copyIntoOwnChild() public {
        delete nodeTree;
        nodeTree.kids.push();
        nodeTree.kids[0] = nodeTree;
    }
}
