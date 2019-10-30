package com.sandra.falldetector2.model;

import io.realm.RealmObject;

public class Contact extends RealmObject {

    private String name;
    private String number;
    private boolean isImportant;



    public Contact(String name, String number,boolean isImportant) {
        this.setName(name);
        this.setNumber(number);
        this.setImportant(isImportant);
    }

    public Contact() {

    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }



    public boolean isImportant() {
        return isImportant;
    }

    public void setImportant(boolean important) {
        isImportant = important;
    }

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }
}