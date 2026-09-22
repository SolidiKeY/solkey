contract FunctionVerification {
    uint total;

    function deposit(uint amount) public {
        require(amount > 0);
        total = amount;
        assert(total > 0);
    }
}
