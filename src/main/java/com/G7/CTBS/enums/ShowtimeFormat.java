package com.G7.CTBS.enums;

import lombok.Getter;

@Getter
public enum ShowtimeFormat {

    TWO_D("2D"),
    THREE_D("3D"),
    IMAX("IMAX");

    private final String label;

    ShowtimeFormat(String label) {
        this.label = label;
    }

}
