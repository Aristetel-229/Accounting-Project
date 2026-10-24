package com.accountpro.model;

/** An account shown in a combo box (e.g. on the Journal Entries form). */
public class AccountOption {
    public final long id;
    public final String code;
    public final String name;

    public AccountOption(long id, String code, String name) {
        this.id = id;
        this.code = code;
        this.name = name;
    }

    @Override
    public String toString() {
        return code + " - " + name;
    }
}
