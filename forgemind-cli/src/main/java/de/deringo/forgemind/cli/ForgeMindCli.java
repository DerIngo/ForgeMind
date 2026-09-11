package de.deringo.forgemind.cli;

import de.deringo.forgemind.core.ForgeMind;

public final class ForgeMindCli {

    private ForgeMindCli() {
    }

    public static void main(String[] args) {
        System.out.println(new ForgeMind().greeting());
    }
}
