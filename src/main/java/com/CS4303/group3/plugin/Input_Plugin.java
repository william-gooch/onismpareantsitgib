package com.CS4303.group3.plugin;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import com.CS4303.group3.Game;
import com.CS4303.group3.Resource;

import dev.dominion.ecs.api.Dominion;

public class Input_Plugin implements Plugin_Interface {
    Dominion dom;
    boolean waiting_left = false, waiting_right = false, waiting_jump = false, waiting_throw = false, waiting = false, waiting_settings = false;

    @Override
    public void build(Game game) {
        this.dom = game.dom;

        Resource.add(game, InputSystem.class);

        game.schedule.keyDown(() -> {
            var input = Resource.get(game, InputSystem.class);
            if(input == null) return;
            input.keysDown.add(game.keyCode);
            input.lastKeyDown = game.keyCode;
            if(!waiting && game.paused) {
                if (game.keyCode == 49) {
                    waiting = true;
                    waiting_left = true;
                } else if(game.keyCode == 50) {
                    waiting = true;
                    waiting_right = true;
                } else if(game.keyCode == 51) {
                    waiting = true;
                    waiting_jump = true;
                } else if(game.keyCode == 52) {
                    waiting = true;
                    waiting_throw = true;
                } else if(game.keyCode == 48) {
                    waiting = true;
                    waiting_settings = true;
                }
            } else if(waiting) {
                waiting = false;
                if(waiting_throw) input.keybinds.put(InputSystem.keys.THROW, game.keyCode);
                if(waiting_jump) input.keybinds.put(InputSystem.keys.JUMP, game.keyCode);
                if(waiting_left) input.keybinds.put(InputSystem.keys.MOVE_LEFT, game.keyCode);
                if(waiting_right) input.keybinds.put(InputSystem.keys.MOVE_RIGHT, game.keyCode);
                if(waiting_settings) input.keybinds.put(InputSystem.keys.SETTINGS, game.keyCode);
                waiting_left = false;
                waiting_right = false;
                waiting_jump = false;
                waiting_throw = false;
                waiting = false;
                waiting_settings = false;
            }
        });

        game.schedule.keyUp(() -> {
            var input = Resource.get(game, InputSystem.class);
            if(input == null) return;
            input.keysDown.remove(game.keyCode);
        });

    }

    public static class InputSystem {
        protected int lastKeyDown;
        protected HashSet<Integer> keysDown;

        public enum keys {
            JUMP,
            MOVE_LEFT,
            MOVE_RIGHT,
            THROW,
            SETTINGS
        }

        public Map<keys, Integer> keybinds;

        public InputSystem() {
            this.keysDown = new HashSet<>();

            //setup the default keybinds
            keybinds = new HashMap<keys, Integer>();
            keybinds.put(keys.JUMP, (int) 'W');
            keybinds.put(keys.MOVE_LEFT, (int) 'A');
            keybinds.put(keys.MOVE_RIGHT, (int) 'D');
            keybinds.put(keys.THROW, (int) 'E');
            keybinds.put(keys.SETTINGS, (int) 'S');
        }

        public InputSystem(Map keybinds) {
            this.keybinds = keybinds;
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
