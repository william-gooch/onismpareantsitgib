package com.CS4303.group3.plugin;

import com.CS4303.group3.Game;
import com.CS4303.group3.Resource;
import com.CS4303.group3.plugin.Force_Plugin.Gravity;
import com.CS4303.group3.utils.Changeable;

import dev.dominion.ecs.api.Dominion;
import processing.core.PVector;

/**
 * Door_Plugin class that represents a door in the game
    * 
 */
public class Door_Plugin implements Plugin_Interface {

    Dominion dom;
    
    @Override
    public void build(Game game) {
        dom = game.dom;
    }

    /**
     * Door class that represents a door in the game
     */
    static class Door {
        public int height, width, maxHeight, maxWidth;
        public float timeElapsed;
        public Changeable.Changeable_Interface open;
        public final float UPDATE_DELAY = 0.01f;
        public final float LOWERING_INCREMENT = 1f;

        /**
         * Constructor for the Door class
         * @param height - height of the door
         * @param width - width of the door
         */
        public Door(int height, int width) {
            this.height = height;
            this.maxHeight = height; 
            this.maxWidth = width;
            this.timeElapsed = 0;
            this.width = width;
            this.open = new Changeable.Changeable_Interface<>(false);
        }

        /**
         * Checks if the door is open or closed
         * @return boolean - true if the door is open, false if the door is closed
         */
        public boolean isOpen() {
            return (boolean) open.get();
        }

        /**
         * Lowers or raises the door up or down 
         * @param game - Game object
         * @param pos - current position of the door
         */
        public void moveDoor(Game game, PVector pos) {

            timeElapsed += game.schedule.dt();

            // update the door position every 0.01 seconds
            if (timeElapsed >= UPDATE_DELAY) { 
                if (isOpen()) {
                    lower(game, pos);
                } else {
                    raise(game, pos);
                }
                timeElapsed = 0;
            }

        }

        /**
         * Lowers the door
         * @param game - Game object
         * @param pos -  current position of the door
         */
        public void lower(Game game, PVector pos) {
            if (height > 0 && width > 0) {
                Gravity gravity = Resource.get(game, Gravity.class);

                if (gravity.gravity().y > 0) { // gravity is pulling down

                    height -= LOWERING_INCREMENT;
                    pos.y += LOWERING_INCREMENT;

                } else if (gravity.gravity().y < 0) { // gravity is pulling up

                    height -= LOWERING_INCREMENT;

                } else if (gravity.gravity().x > 0) { // gravity is pulling right

                    width -= LOWERING_INCREMENT;

                    pos.x += LOWERING_INCREMENT;

                } else if (gravity.gravity().x < 0) { // gravity is pulling left

                    width -= LOWERING_INCREMENT;

                }

                if (height < 0) {
                    height = 0;
                }

                if (width < 0) {
                    width = 0;
                }

            }
        }

        /**
         * Raises the door
         * @param game - Game object
         * @param pos - current position of the door
         */
        public void raise( Game game, PVector pos) {

            Gravity gravity = Resource.get(game, Gravity.class);
            if (height < maxHeight || width < maxWidth) {
                if (gravity.gravity().y > 0) { // gravity is pulling down

                    height += LOWERING_INCREMENT;
                    pos.y -= LOWERING_INCREMENT;

                } else if (gravity.gravity().y < 0) { // gravity is pulling up

                    height += LOWERING_INCREMENT;

                } else if (gravity.gravity().x > 0) { // gravity is pulling right

                    width += LOWERING_INCREMENT;

                    pos.x -= LOWERING_INCREMENT;

                } else if (gravity.gravity().x < 0) { // gravity is pulling left

                    width += LOWERING_INCREMENT;

                }
            }

            if (height > maxHeight) {
                height = maxHeight;
            }
            if (width > maxWidth) {
                width = maxWidth;
            }
        }

    }
}
