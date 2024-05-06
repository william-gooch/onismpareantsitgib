package com.CS4303.group3.plugin;

import com.CS4303.group3.Game;
import com.CS4303.group3.Resource;
import com.CS4303.group3.utils.Changeable;
import dev.dominion.ecs.api.Dominion;
import dev.dominion.ecs.api.Entity;
import javafx.scene.control.TextFormatter;
import processing.core.PConstants;
import processing.core.PVector;

public class Docking_Plugin implements Plugin_Interface {
    Dominion dom;
    @Override
    public void build(Game game) {
        //check if a block is docked, if it is docked for the first time run it's action
        dom = game.dom;

//        game.schedule.update(() -> {
//            dom.findEntitiesWith(Box_Plugin.Box.class)
//                    .stream().forEach(box -> {
//                        //check if the block is docked in any docking
//                        dom.findEntitiesWith(Docking.class, Object_Plugin.Position.class)
//                                .stream().forEach(docking -> {
//                                    if(docking.comp1().contains(box.entity(), docking.comp2().position)) {
//                                        System.out.println("The box has been docked");
//                                    }
//                                });
//
//                    });
//        });


        //draw the docker gap
        game.schedule.draw(-5, drawing -> {
            dom.findEntitiesWith(Docking.class, Object_Plugin.Position.class)
                    .stream().forEach(dock -> {
                        if(dock.comp1().text != null) {
                            drawing.call(draw -> {
                                draw.fill(255,0,255);
                                draw.textSize(16);
                                draw.textAlign(PConstants.LEFT, PConstants.CENTER);
                                draw.rect(dock.comp2().position.x, dock.comp2().position.y, dock.comp1().size.x, dock.comp1().size.y);
                                draw.text(dock.comp1().text.text, dock.comp1().text.position.x, dock.comp1().text.position.y, dock.comp1().text.size.x, dock.comp1().text.size.y);
                            });
                        }
                    });
        });
    }

    public static class Docking<T> {
        PVector size;
        Entity rule;
        Box_Plugin.rule_types rule_type;
        Changeable changeable;
        T default_val;

        Text text;


        float insert_range = 80f;

        public Docking(PVector size, T default_val, Box_Plugin.rule_types rule_type, Changeable changeable, Text text) {
            this.size = size;
            this.default_val = default_val;
            this.rule_type = rule_type;
            this.changeable = changeable;
            this.text = text;

            //set default val
//            if(changeable != null) set_default_val();
        };

        public void set_default_val() {
            changeable.get().change(default_val);
        }

        public void insert_new_rule(Entity new_rule, PVector this_position, Game game) {
            //give the old box the grabbable object
            if(rule != null) rule.add(new Box_Plugin.Grabbable());

            //remove the grabbable object from the box
            new_rule.removeType(Box_Plugin.Grabbable.class);

            //move the new box
            new_rule.get(Object_Plugin.Position.class).position = this_position;

            //run the rule of the new box
            rule = new_rule;
            if(rule.get(Box_Plugin.Box.class).action != null) rule.get(Box_Plugin.Box.class).run_action(game, changeable.get());
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
