package com.sarah.doolhof.dto.response;

import com.sarah.doolhof.common.HunterState;

public record HunterDTO(int id, double x, double y, HunterState state, double distanceToPlayer, boolean inLight) {
}
