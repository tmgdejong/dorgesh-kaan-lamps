package com.dklamps.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum LampStatus {
    BROKEN,
    WORKING,
    UNKNOWN,
}