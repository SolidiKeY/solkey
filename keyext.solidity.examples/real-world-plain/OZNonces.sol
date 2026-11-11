// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

// Source: https://github.com/OpenZeppelin/openzeppelin-contracts/blob/v5.0.0/contracts/utils/Nonces.sol
// The real-world/OZNonces.sol port with its @custom:key specification removed.
contract OZNonces {
    mapping(address account => uint256) private _nonces;

    function nonces(address owner) public view virtual returns (uint256 n) {
        return _nonces[owner];
    }

    function _useNonce(address owner) internal virtual returns (uint256) {
        unchecked {
            return _nonces[owner]++;
        }
    }

    function _useCheckedNonce(address owner, uint256 nonce) internal virtual {
        uint256 current = _useNonce(owner);
        if (nonce != current) {
            revert();
        }
    }

    function useNonce(address owner) public returns (uint256 n) {
        return _useNonce(owner);
    }

    function useCheckedNonce(address owner, uint256 nonce) public {
        _useCheckedNonce(owner, nonce);
    }
}
