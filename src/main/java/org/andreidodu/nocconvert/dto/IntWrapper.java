package org.andreidodu.nocconvert.dto;

import lombok.Getter;

public class IntWrapper {

    @Getter
    private int value;

    public IntWrapper(int value) {
        this.value = value;
    }


    public int incrementAndGetValue() {
        return ++value;
    }
}