package info.kgeorgiy.ja.chulkov.bank.person;

import info.kgeorgiy.ja.chulkov.bank.account.Account;
import info.kgeorgiy.ja.chulkov.bank.account.AccountImpl;
import java.rmi.RemoteException;

public class LocalPersonImpl extends AbstractPerson implements LocalPerson  {

    /**
     * Creates {@link LocalPersonImpl} copy of {@link RemotePersonImpl}
     *
     * @param person remote person to copy
     */
    public LocalPersonImpl(final RemotePersonImpl person) {
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
            throw new AssertionError("Create local account can't throw RemoteException");
        }
    }


    @Override
    void export(final Account account) {}
}
