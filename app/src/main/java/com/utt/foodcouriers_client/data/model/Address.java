package com.utt.foodcouriers_client.data.model;

import java.io.Serializable;

public class Address implements Serializable {
    private final String id;
    private final String label;
    private final String addressLine;
    private final String note;
    private final boolean isDefault;

    public Address(String id, String label, String addressLine, String note, boolean isDefault) {
        this.id = id;
        this.label = label;
        this.addressLine = addressLine;
        this.note = note;
        this.isDefault = isDefault;
    }

    public String getId() {
        return id;
    }

    public String getLabel() {
        return label;
    }

    public String getAddressLine() {
        return addressLine;
    }

    public String getNote() {
        return note;
    }

    public boolean isDefault() {
        return isDefault;
    }
}
