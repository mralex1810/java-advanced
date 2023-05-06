package info.kgeorgiy.ja.chulkov.bank.person;

import info.kgeorgiy.ja.chulkov.bank.account.Account;
import info.kgeorgiy.ja.chulkov.bank.account.AccountImpl;
import java.rmi.RemoteException;

public class LocalPerson extends AbstractPerson  {

    public LocalPerson(final Person person) throws RemoteException {
        super(person.getFirstName(), person.getSecondName(), person.getPassport());
        for (final var idAccount : person.getAccounts().entrySet()) {
            accounts.put(idAccount.getKey(), new AccountImpl(idAccount.getValue()));
        }
    }

    @Override
    public Account createAccount(final String id) {
        try {
            return super.createAccount(id);
        } catch (final RemoteException ignored) {
            throw new AssertionError("Local create account can't throw RemoteException");
        }
    }


    @Override
    protected void export(final Account account) {}
}
