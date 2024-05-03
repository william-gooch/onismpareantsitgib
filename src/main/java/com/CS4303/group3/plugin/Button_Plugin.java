package com.CS4303.group3.plugin;

import java.util.HashSet;

import com.CS4303.group3.Game;
import com.CS4303.group3.plugin.Object_Plugin.Position;

import com.CS4303.group3.utils.Changeable;
import dev.dominion.ecs.api.Dominion;
import dev.dominion.ecs.api.Entity;

public class Button_Plugin implements Plugin_Interface {
    Dominion dom;

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
        game.schedule.draw(draw -> {
            dom.findEntitiesWith(Position.class, Button.class)
                .stream().forEach(res -> {
                    var pos = res.comp1().position;
                    var height = res.comp2().height;
                    var width = res.comp2().width;
                    draw.call(drawing -> {
                        //draw the player character
                        if(res.comp2().pushed) {
                            drawing.fill(255, 0, 0);
                        } else {
                            drawing.fill(255, 165, 0);
                        }
                        drawing.rect(pos.x, pos.y, height, width);
                    });
                });
        });
    }

    static class Button extends Changeable.Changeable_Boolean {
        //store the entity on top of the button
        public Entity object = null;
        public int height, width;
        public boolean pushed;
        public float lastPushed;
        public float loweringSpeed;
        public HashSet<ButtonEventListener> listeners;

        public Button(int height, int width, float loweringSpeed) {
            super(true); //val is whether button opens or closes
            this.height = height;
            this.width = width;
            this.pushed = false;
            this.loweringSpeed = loweringSpeed;
            this.listeners = new HashSet<>();
        }

        public void push(){
            for(ButtonEventListener e: listeners){
                if(get()) e.onPush();
            }
        }

        public void release(){
            for(ButtonEventListener e: listeners){
                if(get()) e.onRelease();
            }
        }



        public void addEventListener(ButtonEventListener e){
            listeners.add(e);
        }

        public void removeEventListener(ButtonEventListener e){
            listeners.add(e);
        }
    }


}
