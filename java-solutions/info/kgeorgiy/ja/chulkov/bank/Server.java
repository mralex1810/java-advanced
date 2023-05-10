package info.kgeorgiy.ja.chulkov.bank;

import info.kgeorgiy.ja.chulkov.utils.ArgumentsUtils;
import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

public final class Server {

    private final static int DEFAULT_PORT = 8888;
    public static final String BANK = "//localhost/bank";

    public static void main(final String... args) {
        ArgumentsUtils.checkNonNullsArgs(args);
        final int port;
        try {
            port = args.length > 0 ? ArgumentsUtils.parseNonNegativeInt(args[0], "port") : DEFAULT_PORT;
        } catch (final NumberFormatException e) {
            System.err.println(e.getMessage());
            return;
        }

        final Bank bank = new RemoteBank(port);
        try {
            UnicastRemoteObject.exportObject(bank, port);
            Naming.rebind(BANK, bank);
            System.err.println("Server started");
        } catch (final RemoteException e) {
            System.err.println("Cannot export object: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        } catch (final MalformedURLException e) {
            System.err.println("Malformed URL");
        }
    }
}
