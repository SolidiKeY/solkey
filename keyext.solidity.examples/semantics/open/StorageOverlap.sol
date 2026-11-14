// SPDX-License-Identifier: GPL-2.0-only
pragma solidity ^0.8.0;

contract StorageOverlap {
    struct Node { uint v; Node[] kids; }
    Node nodeTree;

    // open: design-limitation: the value an overlapping recursive copy leaves in its target is unknown, the copy rules have no in-place semantics (true twin of unsound/recursiveCopyOverlap.key)
    function copyFromOwnChildReadsCleared() public {
        delete nodeTree;
        delete nodeTree.kids.push();
        delete nodeTree.kids[0].kids.push();
        delete nodeTree.kids[0].kids.push();
        nodeTree.kids[0].kids[1].v = 5;
        nodeTree = nodeTree.kids[0];
        assert(nodeTree.kids[1].v == 0);
    }
}
