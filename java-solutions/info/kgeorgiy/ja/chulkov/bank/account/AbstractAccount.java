package info.kgeorgiy.ja.chulkov.bank.account;

public class AbstractAccount implements Account {

    protected final String id;
    protected int amount;

    public AbstractAccount(final String id) {
        this.id = id;
        amount = 0;
    }

    public AbstractAccount(final String id, final int amount) {
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
