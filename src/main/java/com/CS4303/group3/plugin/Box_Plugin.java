package com.CS4303.group3.plugin;

import com.CS4303.group3.Game;
import com.CS4303.group3.Resource;
import com.CS4303.group3.plugin.Object_Plugin.Position;

import com.CS4303.group3.utils.Changeable;
import com.CS4303.group3.utils.Changeable_Interface;
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
        playerSize = (int) game.playerWidth;
    }

    static class Box<T> {
        //this is currently used for determining the arrow on the front, better to store the sprite to be rendered when graphics are sorted
        public enum directions {
            UP,
            DOWN,
            LEFT,
            RIGHT
        }

        public rule_types rule_type;
        public Entity docked = null;
        public PVector size = new PVector(0,0);
        public T value;
        public directions direction;

        public Box() {}

        public Box(PVector size) {this.size = size;}


        public Box(rule_types rule_type, T value) {
            this.value = value;
            this.rule_type = rule_type;
        }
    }

    static class Grabbable {
        //store some rules associated with the block
        //strore the player object that is picking it up
        public Entity player = null;

        public Grabbable() {}

        public PVector get_above_head_position(Game game, PVector player_position, float player_size) {
            Force_Plugin.Gravity gravity = Resource.get(game, Force_Plugin.Gravity.class);
            if(gravity == null) return new PVector(0,0);
            PVector gravityNorm = gravity.gravity().copy().normalize();

            return PVector.add(player_position, new PVector(-player_size * 1.2f * gravityNorm.x, -player_size * 1.2f * gravityNorm.y, 0));
        }
    }

    public enum rule_types {
        DIRECTIONAL,
        OPERATIONAL,
        BOOLEAN,
        TARGET
    }


}
