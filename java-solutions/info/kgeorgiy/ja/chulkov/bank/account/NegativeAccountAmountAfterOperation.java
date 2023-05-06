package info.kgeorgiy.ja.chulkov.bank.account;

public class NegativeAccountAmountAfterOperation extends Exception {

    public NegativeAccountAmountAfterOperation(final String s) {
        super(s);
    }
}
