package com.sarah.doolhof.dto.request;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class PlayerInputDTOTest {

    @Test
    void directionsBetweenMinusOneAndOneAreAccepted() {
        assertDoesNotThrow(() -> new PlayerInputDTO(-1, 1, true, true));
        assertDoesNotThrow(() -> new PlayerInputDTO(0, 0.5, false, false));
    }

    @Test
    void anythingElseIsRefused() {
        assertThrows(IllegalArgumentException.class, () -> new PlayerInputDTO(2, 0, false, false));
        assertThrows(IllegalArgumentException.class, () -> new PlayerInputDTO(0, -1.01, false, false));
        assertThrows(IllegalArgumentException.class, () -> new PlayerInputDTO(Double.NaN, 0, false, false));
    }
}
