package info.kgeorgiy.ja.chulkov.bank;


import info.kgeorgiy.ja.chulkov.bank.account.Account;
import info.kgeorgiy.ja.chulkov.bank.account.NegativeAccountAmountAfterOperation;
import info.kgeorgiy.ja.chulkov.bank.person.Person;
import info.kgeorgiy.ja.chulkov.bank.person.PersonData;
import info.kgeorgiy.ja.chulkov.utils.ArgumentsUtils;
import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;

public final class Client {


    /**
     * Utility class.
     */
    private Client() {
    }


    /**
     * Creates a connection to bank and set money on account
     *
     * @param args array of firstName, secondName, passport, accountId, delta
     */
    public static void main(final String... args) {
        ArgumentsUtils.checkNonNullsArgs(args);
        if (args.length != 5) {
            printUsage();
            return;
        }
        final int delta;
        try {
            delta = Integer.parseInt(args[4]);
        } catch (final NumberFormatException e) {
            System.err.println("Fifth arg isn't integer" + e.getMessage());
            printUsage();
            return;
        }
        final Bank bank;
        try {
            bank = (Bank) Naming.lookup(Server.BANK);
        } catch (final NotBoundException e) {
            System.err.println("Bank is not bound");
            return;
        } catch (final MalformedURLException e) {
            System.err.println("Bank URL is invalid");
            return;
        } catch (final RemoteException e) {
            System.err.println("Bank is not found");
            return;
        }

        final PersonData personData = new PersonData(args[0], args[1], args[2]);
        Person person;
        try {
            person = bank.getRemotePerson(personData.passport());
            if (person == null) {
                System.out.println("Creating person");
                bank.createPerson(personData);
                person = bank.getRemotePerson(personData.passport());
            } else {
                System.out.println("Person already exists");
            }
        } catch (final RemoteException e) {
            System.err.println("Error on getting person");
            return;
        }
        final var accountId = args[3];
        try {
            Account account = null;
            account = person.getAccount(accountId);
            if (account == null) {
                System.out.println("Creating account");
                account = person.createAccount(accountId);
            } else {
                System.out.println("Account already exists");
            }

            System.out.println("Account id: " + account.getId());
            System.out.println("Money: " + account.getAmount());
            System.out.println("Adding money");
            try {
                account.setAmount(account.getAmount() + delta);
            } catch (final NegativeAccountAmountAfterOperation e) {
                System.err.println(e.getMessage());
            }
            System.out.println("Money: " + account.getAmount());
        } catch (final RemoteException e) {
            System.err.println("Error on manipulations with account");
        }
    }

    private static void printUsage() {
        System.err.println("""
                Usage:
                Client firstName, secondName, passport, accountId, delta
                """);
    }
}
