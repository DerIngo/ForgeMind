package de.deringo.forgemind.mcp;

import de.deringo.forgemind.core.ForgeMind;

public final class ForgeMindMCP {

    private ForgeMindMCP() {
    }

    public static void main(String[] args) {
        if (args.length > 0 && args[0].equals("--test-mode")) {
            System.out.println(new ForgeMind().greeting());
            System.exit(0);
        }
    }
}
