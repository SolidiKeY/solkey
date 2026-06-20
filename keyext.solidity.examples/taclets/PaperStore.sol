contract PaperStore {
    struct Token {
        uint value;
    }

    struct Account {
        uint balance;
        Token token;
    }

    struct Person {
        Account account;
        uint age;
    }

    uint total;
    uint age;
    uint balance;
    uint[] values;
    mapping(uint => uint) balances;
    uint[][] matrix;
    Person alice;
    Person bob;
}
