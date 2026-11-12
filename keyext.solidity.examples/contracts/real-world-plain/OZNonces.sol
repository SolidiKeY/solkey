// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

// Source: https://github.com/OpenZeppelin/openzeppelin-contracts/blob/v5.0.0/contracts/utils/Nonces.sol
// The real-world/OZNonces.sol port with its specification written as require and assert. unchecked
// is removed.
contract OZNonces {
    mapping(address account => uint256) private _nonces;

    /// @custom:key box
    function nonces(address owner, address a) public view virtual returns (uint256 n) {
        require(_nonces[a] >= 0);
        uint256 oldNonce = _nonces[owner];

        n = _nonces[owner];

        assert(n == _nonces[owner] && _nonces[owner] == oldNonce);
        assert(_nonces[a] >= 0);
    }

    function _useNonce(address owner) internal virtual returns (uint256) {
        return _nonces[owner]++;
    }

    function _useCheckedNonce(address owner, uint256 nonce) internal virtual {
        uint256 current = _useNonce(owner);
        if (nonce != current) {
            revert();
        }
    }

    /// @custom:key box
    function useNonce(address owner, address a) public returns (uint256 n) {
        require(_nonces[a] >= 0 && _nonces[owner] >= 0);
        require(_nonces[owner] < 115792089237316195423570985008687907853269984665640564039457584007913129639935);
        uint256 oldNonce = _nonces[owner];
        uint256 oldA = _nonces[a];

        n = _useNonce(owner);

        assert(n == oldNonce && _nonces[owner] == oldNonce + 1);
        assert(a == owner || _nonces[a] == oldA);
        assert(_nonces[a] >= 0);
    }

    /// @custom:key box
    function useCheckedNonce(address owner, uint256 nonce, address a) public {
        require(_nonces[a] >= 0 && _nonces[owner] >= 0);
        require(_nonces[owner] < 115792089237316195423570985008687907853269984665640564039457584007913129639935);
        uint256 oldNonce = _nonces[owner];
        uint256 oldA = _nonces[a];

        _useCheckedNonce(owner, nonce);

        assert(nonce == oldNonce && _nonces[owner] == oldNonce + 1);
        assert(a == owner || _nonces[a] == oldA);
        assert(_nonces[a] >= 0);
    }
}
