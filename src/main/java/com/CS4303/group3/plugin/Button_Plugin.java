package com.CS4303.group3.plugin;

import java.util.HashSet;

import com.CS4303.group3.Game;

import com.CS4303.group3.utils.Changeable;
import dev.dominion.ecs.api.Dominion;
import dev.dominion.ecs.api.Entity;

/**
 * Button_Plugin class for representing a button in the game
 */
public class Button_Plugin implements Plugin_Interface {
    Dominion dom;

    /**
     * ButtonEventListener interface that represents an event listener for a button
     */
    public interface ButtonEventListener{
        public void onPush();
        public void onRelease();
    }


    @Override
    public void build(Game game) {
        dom = game.dom;

        game.schedule.update(() -> {
            dom.findEntitiesWith(Button.class)
                .stream().forEach(res -> {
                    if(res.comp().pushed) {
                        res.comp().push();
                        res.comp().lastPushed += game.schedule.dt();
                        if(res.comp().lastPushed > 0.3) {
                            res.comp().pushed = false;
                        }
                    }else{
                        res.comp().release();
                    }

                });
        });

        //draw the object
        // game.schedule.draw(draw -> {
        //     dom.findEntitiesWith(Position.class, Button.class)
        //         .stream().forEach(res -> {
        //             var pos = res.comp1().position;
        //             var height = res.comp2().height;
        //             var width = res.comp2().width;
        //             draw.call(drawing -> {
        //                 //draw the player character
        //                 if(res.comp2().pushed) {
        //                     drawing.fill(255, 0, 0);
        //                 } else {
        //                     drawing.fill(255, 165, 0);
        //                 }
        //                 drawing.rect(pos.x, pos.y, height, width);
        //             });
        //         });
        // });
    }

    /**
     * Button class that represents a button in the game
     */
    static class Button extends Changeable.Changeable_Interface {
        //store the entity on top of the button
        public Entity object = null;
        public int height, width;
        public boolean pushed;
        public float lastPushed;
        public HashSet<ButtonEventListener> listeners;

        /**
         * Constructor for the Button class
         * @param height - height of the button
         * @param width - width of the button
         */
        public Button(int height, int width) {
            super(true); //val is whether button opens or closes
            this.height = height;
            this.width = width;
            this.pushed = false;
            this.listeners = new HashSet<>();
        }

        /**
         * Pushes the button
         */
        public void push(){
            for(ButtonEventListener e: listeners){
                if((boolean)get()) e.onPush();
            }
        }

        /**
         * Releases the button
         */
        public void release(){
            for(ButtonEventListener e: listeners){
                if((boolean)get()) e.onRelease();
            }
        }

        /**
         * Adds an event listener to the button
         * @param e - ButtonEventListener object
         */
        public void addEventListener(ButtonEventListener e){
            listeners.add(e);
        }

        /**
         * Removes an event listener from the button
         * @param e - ButtonEventListener object
         */
        public void removeEventListener(ButtonEventListener e){
            listeners.add(e);
        }
    }


}
