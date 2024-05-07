package com.CS4303.group3.plugin;

import java.util.Map;
import com.CS4303.group3.Game;
import com.CS4303.group3.plugin.Door_Plugin.Door;
import com.CS4303.group3.plugin.Force_Plugin.Gravity;
import com.CS4303.group3.plugin.Object_Plugin.Position;
import com.CS4303.group3.utils.Changeable;

import dev.dominion.ecs.api.*;

public class Trigger_Plugin {

    static record Trigger(ITrigger trigger) {
        public void trigger(Game game, Entity self, Object value) {
            this.trigger.trigger(game, self, value);
        }
    }

    @FunctionalInterface
    static interface ITrigger {
        public void trigger(Game game, Entity self, Object value);
    }

    public static final Map<String, Trigger> STANDARD_TRIGGERS = Map.ofEntries(
        Map.entry("open", new Trigger((game, self, value) -> {
            if (self.has(Door.class)) {
                self.get(Door.class).change((boolean) value);
                self.get(Door.class).moveDoor(game, self.get(Position.class).position);
            }
        })),
        Map.entry("gravity", new Trigger((game, self, value) -> {
            if(self.has(Changeable.class)) {
                self.get(Changeable.class).get().change(value);
            } else {
                self.add(new Changeable(self.get(Gravity.class)));
            }
        })),
        Map.entry("change_target", new Trigger((game, self, value) -> {
            if(self.has(Changeable.class)) {

                self.get(Changeable.class).get().change(value);
            } else {
                self.add(new Changeable(self.get(Docking_Plugin.Docking.class)));
            }
        }))

    );
}
