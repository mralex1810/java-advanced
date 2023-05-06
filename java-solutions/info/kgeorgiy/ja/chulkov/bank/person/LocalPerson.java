package info.kgeorgiy.ja.chulkov.bank.person;

import info.kgeorgiy.ja.chulkov.bank.account.Account;
import info.kgeorgiy.ja.chulkov.bank.account.LocalAccount;
import java.rmi.RemoteException;

public class LocalPerson extends AbstractPerson<LocalAccount>  {

    public LocalPerson(final Person person) throws RemoteException {
        super(person.getFirstName(), person.getSecondName(), person.getPassport());
        for (final var idAccount : person.getAccounts().entrySet()) {
            accounts.put(idAccount.getKey(), new LocalAccount(idAccount.getValue()));
        }
    }

    @Override
    public Account createAccount(final String id) {
        final var accountId = passport + ":" + id;
        System.out.println("Creating account " + accountId);
        final LocalAccount account = new LocalAccount(accountId);
        if (accounts.putIfAbsent(accountId, account) == null) {
            return account;
        } else {
            return getAccount(accountId);
        }
    }

}
