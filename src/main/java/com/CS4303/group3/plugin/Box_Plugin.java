package com.CS4303.group3.plugin;

import com.CS4303.group3.Game;
import com.CS4303.group3.plugin.Object_Plugin.Position;

import dev.dominion.ecs.api.Dominion;
import dev.dominion.ecs.api.Entity;

public class Box_Plugin implements Plugin_Interface {
    Dominion dom;
    int playerSize = 10;


    @Override
    public void build(Game game) {
        dom = game.dom;
        playerSize = (int) (game.scale/30);

        
        //hadle object collisions

        //draw the object
        game.schedule.draw(draw -> {
            dom.findEntitiesWith(Position.class, Box.class)
                .stream().forEach(res -> {
                    var pos = res.comp1().position;
                    draw.call(drawing -> {
                        //draw the player character
                        drawing.fill(128,128,0);
                        drawing.rect(pos.x, pos.y, playerSize, playerSize);
                    });
                });
        });
    }

    static class Box {}

    static class Grabbable {
        //store some rules associated with the block
        //strore the player object that is picking it up
        public Entity player = null;

        public Grabbable() {}
    }
}
