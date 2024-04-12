package com.CS4303.group3.plugin;

import com.CS4303.group3.Game;
import com.CS4303.group3.plugin.Object_Plugin.Position;
import com.CS4303.group3.plugin.Object_Plugin.Collider;
import com.CS4303.group3.plugin.Player_Plugin.Player;

import dev.dominion.ecs.api.Dominion;
import dev.dominion.ecs.api.Entity;

public class Box_Plugin implements Plugin_Interface {
    Dominion dom;


    @Override
    public void build(Game game) {
        dom = game.dom;

        //draw the object
        game.schedule.draw(draw -> {
            dom.findEntitiesWith(Position.class, Box.class, Collider.class)
                .stream().forEach(res -> {
                    var pos = res.comp1().position;
                    draw.call(drawing -> {
                        //draw the player character
                        drawing.fill(128,128,0);
                        drawing.rect(pos.x, pos.y, res.comp3().collider.getSize().x, res.comp3().collider.getSize().y);
                    });
                });
        });
    }

    static class Box {
        //store some rules associated with the block
        //strore the player object that is picking it up
        public Entity player = null;

        public Box() {}
    }
}
