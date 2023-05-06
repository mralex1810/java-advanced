package info.kgeorgiy.ja.chulkov.bank.account;

import java.rmi.RemoteException;

public class LocalAccount extends AbstractAccount {

    public LocalAccount(final String id) {
        super(id);
    }

    public LocalAccount(final Account acc) throws RemoteException {
        super(acc.getId(), acc.getAmount());
    }
}
