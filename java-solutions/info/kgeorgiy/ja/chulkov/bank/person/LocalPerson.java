package info.kgeorgiy.ja.chulkov.bank.person;

import info.kgeorgiy.ja.chulkov.bank.account.Account;
import info.kgeorgiy.ja.chulkov.bank.account.AccountImpl;
import java.rmi.RemoteException;

public class LocalPerson extends AbstractPerson  {

    /**
     * Creates {@link LocalPerson} copy of {@link RemotePerson}
     *
     * @param person remote person to copy
     */
    public LocalPerson(final RemotePerson person) {
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
