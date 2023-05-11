package info.kgeorgiy.ja.chulkov.bank.account;

/**
 * Exception to notify user, that he can't set negative amount of money.
 *
 */
public class NegativeAccountAmountAfterOperation extends Exception {

    public NegativeAccountAmountAfterOperation(final String s) {
        super(s);
    }
}
