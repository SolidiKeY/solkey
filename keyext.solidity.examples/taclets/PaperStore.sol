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
    mapping(uint => Person) people;
    mapping(uint => bool) flags;
    uint[][] matrix;
    Person[] persons;
    Person alice;
    Person bob;
}
