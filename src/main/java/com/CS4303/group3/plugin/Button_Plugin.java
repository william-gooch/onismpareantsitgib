package com.CS4303.group3.plugin;

import com.CS4303.group3.Game;
import com.CS4303.group3.plugin.Object_Plugin.Position;

import dev.dominion.ecs.api.Dominion;
import dev.dominion.ecs.api.Entity;

public class Button_Plugin implements Plugin_Interface {
    Dominion dom;


    @Override
    public void build(Game game) {
        dom = game.dom;

        

        
        //handle object collisions


        //draw the object
        game.schedule.draw(draw -> {
            dom.findEntitiesWith(Position.class, Button.class)
                .stream().forEach(res -> {
                    var pos = res.comp1().position;
                    var height = res.comp2().height;
                    var width = res.comp2().width;
                    draw.call(drawing -> {
                        //draw the player character
                        drawing.fill(255, 165, 0);
                        drawing.rect(pos.x, pos.y, height, width);
                    });
                });
        });
    }

    static class Button {
        //store the entity on top of the button
        public Entity object = null;
        public int height, width;
        public boolean pushed;
        public float loweringSpeed;

        public Button(int height, int width, float loweringSpeed) {
            this.height = height;
            this.width = width;
            this.pushed = false;
            this.loweringSpeed = loweringSpeed;
        }
    }
}
