package com.CS4303.group3.plugin;

import com.CS4303.group3.Game;
import com.CS4303.group3.utils.Map;
import com.CS4303.group3.plugin.Object_Plugin.*;

import dev.dominion.ecs.api.Dominion;
import processing.core.*;

public class Map_Plugin implements Plugin_Interface {
    Dominion dom;

    @Override
    public void build(Game game) {
        this.dom = game.dom;

        game.schedule.draw(-5, draw -> {
            dom.findEntitiesWith(Ground.class, Position.class)
                .stream().forEach(entity -> {
                    Ground ground = entity.comp1();
                    PVector position = entity.comp2().position;
                    draw.call(drawing -> {
                        drawing.push();
                        ground.draw(drawing, position);
                        drawing.pop();
                    });
                });
        });
    }

    public static class Ground {
        public PVector size;
        //maybe have a tileset

        public Ground(PVector size) {
            this.size = size;
        }

        public void draw(Game game, PVector position) {
            game.fill(255);
            game.stroke(255);
            game.rect(position.x, position.y, size.x, size.y);
        }
    } 
}
