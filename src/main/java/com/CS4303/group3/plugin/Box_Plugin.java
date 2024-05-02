package com.CS4303.group3.plugin;

import com.CS4303.group3.Game;
import com.CS4303.group3.Resource;
import com.CS4303.group3.plugin.Object_Plugin.Position;

import com.CS4303.group3.utils.Changeable;
import dev.dominion.ecs.api.Dominion;
import dev.dominion.ecs.api.Entity;
import processing.core.PApplet;
import processing.core.PVector;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

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
                        //draw the box
                        drawing.fill(128,128,0);
                        drawing.rect(pos.x, pos.y, playerSize, playerSize);
                        drawing.fill(0);
                        //draw an arrow
                        drawing.pushMatrix();
                        drawing.translate(pos.x+playerSize/2, pos.y+playerSize/2);
                        if(res.comp2().direction == Box.directions.LEFT) drawing.rotate(PApplet.radians(180));
                        if(res.comp2().direction == Box.directions.DOWN) drawing.rotate(PApplet.radians(90));
                        drawing.line(0,0,playerSize/2, 0);
                        drawing.line(playerSize/2, 0, playerSize/2 - 8, -8);
                        drawing.line(playerSize/2, 0, playerSize/2 - 8, 8);

                        drawing.popMatrix();
                    });
                });
        });
    }

    static class Box {
        public enum directions {
            UP,
            DOWN,
            LEFT,
            RIGHT
        }

        public BiConsumer action = null;
        public rule_types rule_type;
        public PVector size = new PVector(0,0);
        public directions direction;

        public Box() {}

        public Box(PVector size) {this.size = size;}


        public Box(BiConsumer r, PVector size, rule_types rule_type, directions direction) {
            action = r;
            this.size = size;
            this.rule_type = rule_type;
            this.direction = direction;
        }

        public void run_action(Game game, Changeable changeable) {
            action.accept(game, changeable);
        }
    }

    static class Grabbable {
        //store some rules associated with the block
        //strore the player object that is picking it up
        public Entity player = null;

        public Grabbable() {}
    }

    public enum rule_types {
        DIRECTIONAL,
        OPERATIONAL
    }

    //consumers for changing the direction of a value
    public static BiConsumer<Game, Changeable.Changeable_Direction> change_direction_up = (game, directional) -> {
        if(directional != null) directional.change(new PVector(0, -1));
    };
    public static BiConsumer<Game, Changeable.Changeable_Direction> change_direction_left = (game, directional) -> {
        if(directional != null) directional.change(new PVector(-1, 0));
    };
    public static BiConsumer<Game, Changeable.Changeable_Direction> change_direction_right = (game, directional) -> {
        if(directional != null) directional.change(new PVector(1, 0));
    };
    public static BiConsumer<Game, Changeable.Changeable_Direction> change_direction_down = (game, directional) -> {
        if(directional != null) directional.change(new PVector(0, 1));
    };

    //consumers for changing the boolean of a value
    public static BiConsumer<Game, Changeable.Changeable_Boolean> change_true = (game, directional) -> {
        if(directional != null) directional.change(true);
    };
    public static BiConsumer<Game, Changeable.Changeable_Boolean> change_false = (game, directional) -> {
        if(directional != null) directional.change(false);
    };

}
