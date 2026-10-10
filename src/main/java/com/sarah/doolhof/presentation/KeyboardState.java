package com.sarah.doolhof.presentation;

import com.sarah.doolhof.dto.request.PlayerInputDTO;
import java.awt.event.KeyEvent;
import java.util.HashSet;
import java.util.Set;

final class KeyboardState {

    private final Set<Integer> down = new HashSet<>();
    private boolean flareRequested;

    void press(int keyCode) {
        // Holding a key repeats the event; the flare should fire once per press.
        if (keyCode == KeyEvent.VK_SPACE && !down.contains(keyCode)) {
            flareRequested = true;
        }
        down.add(keyCode);
    }

    void release(int keyCode) {
        down.remove(keyCode);
    }

    void clear() {
        down.clear();
        flareRequested = false;
    }

    PlayerInputDTO poll() {
        double x = (isDown(KeyEvent.VK_RIGHT, KeyEvent.VK_D) ? 1 : 0)
            - (isDown(KeyEvent.VK_LEFT, KeyEvent.VK_A, KeyEvent.VK_Q) ? 1 : 0);
        double y = (isDown(KeyEvent.VK_DOWN, KeyEvent.VK_S) ? 1 : 0)
            - (isDown(KeyEvent.VK_UP, KeyEvent.VK_W, KeyEvent.VK_Z) ? 1 : 0);

        boolean flare = flareRequested;
        flareRequested = false;

        return new PlayerInputDTO(x, y, isDown(KeyEvent.VK_SHIFT), flare);
    }

    private boolean isDown(int... keyCodes) {
        for (int keyCode : keyCodes) {
            if (down.contains(keyCode)) {
                return true;
            }
        }
        return false;
    }
}
