package com.CS4303.group3.plugin;

import java.util.HashSet;

import com.CS4303.group3.Game;
import com.CS4303.group3.Resource;

import dev.dominion.ecs.api.Dominion;

public class Input_Plugin implements Plugin_Interface {
    Dominion dom;

    @Override
    public void build(Game game) {
        this.dom = game.dom;

        // Resource.add(game, InputSystem.class);

        game.schedule.keyDown(() -> {
            var input = Resource.get(game, InputSystem.class);
            input.keysDown.add(game.keyCode);
            input.lastKeyDown = game.keyCode;
        });

        game.schedule.keyUp(() -> {
            var input = Resource.get(game, InputSystem.class);
            input.keysDown.remove(game.keyCode);
        });
    }

    public static class InputSystem {
        protected int lastKeyDown;
        protected HashSet<Integer> keysDown;

        public InputSystem() {
            this.keysDown = new HashSet<>();
        }

        boolean isKeyDown(int key) {
            return keysDown.contains(key);
        }
        
        boolean isKeyDown(char key) {
            return keysDown.contains((int) key);
        }

        boolean isLastKeyDown(int key) {
            return lastKeyDown == key;
        }

        boolean isLastKeyDown(char key) {
            return lastKeyDown == ((int) key);
        }
    }
}
