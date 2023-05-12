package info.kgeorgiy.ja.chulkov.bank.account;

import java.io.Serializable;
import java.util.Objects;

public class AccountImpl implements Account, Serializable {

    private final String id;
    private long amount;


    /**
     * Creates a copy of another {@link AccountImpl}
     *
     * @param account to copy
     */
    public AccountImpl(final AccountImpl account) {
        this(account.getId(), account.getAmount());
    }

    /**
     * Creates a new account with specified id and zero amount
     *
     * @param id of account
     */
    public AccountImpl(final String id) {
        this(id, 0);
    }
    /**
     * Creates a new account with specified id and amount
     *
     * @param id of account
     * @param amount of account
     */
    public AccountImpl(final String id, final long amount) {
        Objects.requireNonNull(id);
        this.id = id;
        this.amount = amount;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public synchronized long getAmount() {
        System.out.println("Getting amount of money for account " + id);
        return amount;
    }

    @Override
    public synchronized void setAmount(final long amount) throws NegativeAccountAmountAfterOperation {
        System.out.println("Setting amount of money for account " + id);
        if (amount < 0) {
            throw new NegativeAccountAmountAfterOperation(amount + " is less then zero. This is not credit account");
        }
        this.amount = amount;
    }
}
