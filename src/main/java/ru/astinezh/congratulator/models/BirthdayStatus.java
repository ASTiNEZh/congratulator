package ru.astinezh.congratulator.models;

public enum BirthdayStatus {
    OTHER("OTHER"),
    RECENT("RECENT"),
    TODAY("TODAY"),
    UPCOMING("UPCOMING");

    private String status;

    BirthdayStatus(String status) {this.status = status;}
}
