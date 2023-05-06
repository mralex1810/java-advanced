package info.kgeorgiy.ja.chulkov.bank.account;

import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.Objects;

public class AccountImpl implements Account, Serializable {

    protected final String id;
    protected int amount;


    public AccountImpl(final Account account) throws RemoteException {
        this(account.getId(), account.getAmount());
    }

    public AccountImpl(final String id) {
        this(id, 0);
    }

    public AccountImpl(final String id, final int amount) {
        Objects.requireNonNull(id);
        this.id = id;
        this.amount = amount;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public synchronized int getAmount() {
        System.out.println("Getting amount of money for account " + id);
        return amount;
    }

    @Override
    public synchronized void setAmount(final int amount) throws NegativeAccountAmountAfterOperation {
        System.out.println("Setting amount of money for account " + id);
        if (amount < 0) {
            throw new NegativeAccountAmountAfterOperation(amount + " is less then zero. This is not credit account");
        }
        this.amount = amount;
    }
}
