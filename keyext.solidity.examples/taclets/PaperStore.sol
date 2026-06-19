contract PaperStore {
    struct Account {
        uint balance;
    }

    struct Person {
        Account account;
        uint age;
    }

    uint age;
    uint balance;
    uint[][] matrix;
    Person alice;
}
