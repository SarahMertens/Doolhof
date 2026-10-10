package com.sarah.doolhof.dto.response;

import java.util.List;

public record WallShiftDTO(List<CellDTO> opening, List<CellDTO> closing) {

    public static final WallShiftDTO NONE = new WallShiftDTO(List.of(), List.of());

    public WallShiftDTO {
        opening = List.copyOf(opening);
        closing = List.copyOf(closing);
    }
}
