package com.dklamps;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Area
{
    P0_N("North Ground Floor"),
    P0_S("South Ground Floor"),
    P1_N("North Second Floor"),
    P1_S("South Second Floor"),
    P2_N("North Third Floor"),
    P2_S("South Third Floor");

    private final String name;
}
