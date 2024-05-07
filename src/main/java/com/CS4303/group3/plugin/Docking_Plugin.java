package com.CS4303.group3.plugin;

import com.CS4303.group3.Game;
import com.CS4303.group3.utils.Changeable;
import com.CS4303.group3.utils.Changeable_Interface;
import dev.dominion.ecs.api.Dominion;
import dev.dominion.ecs.api.Entity;
import processing.core.PConstants;
import processing.core.PVector;

public class Docking_Plugin implements Plugin_Interface {
    Dominion dom;
    @Override
    public void build(Game game) {
        //check if a block is docked, if it is docked for the first time run it's action
        dom = game.dom;

        //draw the docker gap
        game.schedule.draw(10, drawing -> {
            dom.findEntitiesWith(Docking.class, Object_Plugin.Position.class)
                    .stream().forEach(dock -> {
                        if(dock.comp1().text != null) {
                            drawing.call(draw -> {
                                draw.fill(255,0,255);
                                draw.textSize(16);
                                draw.textAlign(PConstants.LEFT, PConstants.CENTER);
                                draw.text(dock.comp1().text.text, dock.comp1().text.position.x, dock.comp1().text.position.y, dock.comp1().text.size.x, dock.comp1().text.size.y);
                            });
                        }
                    });
        });
    }

    public static class Docking<T> extends Changeable_Interface {
        PVector size;
        Entity block = null;
        Box_Plugin.rule_types rule_type;
        float rotation;
        T default_val;

        Text text;


        float insert_range = 80f;

        public Docking(PVector size, T default_val, Box_Plugin.rule_types rule_type, Changeable changeable, Text text, float rotation) {
            super(changeable);
            this.size = size;
            this.default_val = default_val;
            this.rule_type = rule_type;
            this.text = text;
            this.rotation = rotation;
        };

        public Changeable get() {return (Changeable) super.get();}

        public void set_default_val() {
            ((Changeable)get()).get().change(default_val);
        }

        //returns true if the two positions are close enough to swap
        public boolean is_close(PVector block_pos, PVector block_size, PVector indent_position, Box_Plugin.rule_types box_type) {
            PVector this_mid_pos = new PVector(indent_position.x + size.x/2, indent_position.y + size.y/2);
            PVector block_mid_pos = new PVector(block_pos.x + block_size.x/2, block_pos.y + block_size.y/2);

            return this_mid_pos.sub(block_mid_pos).mag() < insert_range && box_type == rule_type;
        }

        public static class Text {
            public String text;
            public PVector position;
            public PVector size;
            public PVector direction;

            public Text(String text, PVector position, PVector size) {
                this.text = text;
                this.position = position;
                this.size = size;
            }
        }
    }
}
