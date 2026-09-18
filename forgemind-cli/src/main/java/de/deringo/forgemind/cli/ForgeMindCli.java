package de.deringo.forgemind.cli;

import de.deringo.forgemind.core.ForgeMind;

public final class ForgeMindCli {

    private ForgeMindCli() {
    }

    public static void main(String[] args) {
        if (args.length > 0 && args[0].equals("--test-mode")) {
            System.out.println(new ForgeMind().greeting());
            System.exit(0);
        }
        Main.main(args);
    }
}
