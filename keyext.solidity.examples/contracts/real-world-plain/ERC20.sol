// SPDX-License-Identifier: MIT
pragma solidity ^0.8.26;

// Source: https://solidity-by-example.org/app/erc20/
// The real-world/ERC20.sol port with its specification written as require and assert.
contract ERC20 {
    uint256 public totalSupply;
    mapping(address => uint256) public balanceOf;
    mapping(address => mapping(address => uint256)) public allowance;
    string public name;
    string public symbol;
    uint8 public decimals;

    constructor(string memory _name, string memory _symbol, uint8 _decimals) {
        name = _name;
        symbol = _symbol;
        decimals = _decimals;
    }

    /// @custom:key box
    function transfer(address recipient, uint256 amount, address a) external returns (bool success) {
        require(amount >= 0 && balanceOf[msg.sender] >= amount);
        uint256 oldTotalSupply = totalSupply;
        uint256 oldSenderBalance = balanceOf[msg.sender];
        uint256 oldRecipientBalance = balanceOf[recipient];
        uint256 oldA = balanceOf[a];

        balanceOf[msg.sender] -= amount;
        balanceOf[recipient] += amount;
        success = true;

        assert(success && totalSupply == oldTotalSupply);
        assert(msg.sender == recipient || (balanceOf[msg.sender] == oldSenderBalance - amount && balanceOf[recipient] == oldRecipientBalance + amount));
        assert(msg.sender != recipient || balanceOf[msg.sender] == oldSenderBalance);
        assert(a == msg.sender || a == recipient || balanceOf[a] == oldA);
    }
    /// @custom:key box
    function approve(address spender, uint256 amount) external returns (bool success) {
        require(amount >= 0);

        allowance[msg.sender][spender] = amount;
        success = true;

        assert(success && allowance[msg.sender][spender] == amount);
    }

    /// @custom:key box
    function transferFrom(address sender, address recipient, uint256 amount)
        external
        returns (bool success)
    {
        require(amount >= 0 && allowance[sender][msg.sender] >= amount && balanceOf[sender] >= amount);
        uint256 oldTotalSupply = totalSupply;
        uint256 oldAllowance = allowance[sender][msg.sender];
        uint256 oldSenderBalance = balanceOf[sender];
        uint256 oldRecipientBalance = balanceOf[recipient];

        allowance[sender][msg.sender] -= amount;
        balanceOf[sender] -= amount;
        balanceOf[recipient] += amount;
        success = true;

        assert(success && totalSupply == oldTotalSupply);
        assert(allowance[sender][msg.sender] == oldAllowance - amount);
        assert(sender == recipient || (balanceOf[sender] == oldSenderBalance - amount && balanceOf[recipient] == oldRecipientBalance + amount));
        assert(sender != recipient || balanceOf[sender] == oldSenderBalance);
    }

    /// @custom:key box
    function mint(address to, uint256 amount) external {
        require(amount >= 0);
        uint256 oldBalance = balanceOf[to];
        uint256 oldTotalSupply = totalSupply;

        balanceOf[to] += amount;
        totalSupply += amount;

        assert(balanceOf[to] == oldBalance + amount && totalSupply == oldTotalSupply + amount);
    }

    /// @custom:key box
    function burn(address from, uint256 amount) external {
        require(amount >= 0 && balanceOf[from] >= amount && totalSupply >= amount);
        uint256 oldBalance = balanceOf[from];
        uint256 oldTotalSupply = totalSupply;

        balanceOf[from] -= amount;
        totalSupply -= amount;

        assert(balanceOf[from] == oldBalance - amount && totalSupply == oldTotalSupply - amount);
    }
}
