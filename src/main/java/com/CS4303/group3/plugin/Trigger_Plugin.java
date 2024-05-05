package com.CS4303.group3.plugin;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

import com.CS4303.group3.Game;
import com.CS4303.group3.plugin.Door_Plugin.Door;
import com.CS4303.group3.plugin.Force_Plugin.Gravity;
import com.CS4303.group3.plugin.Object_Plugin.Position;

import dev.dominion.ecs.api.*;
import processing.core.*;

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
                self.get(Door.class).open.change((boolean) value);
                self.get(Door.class).moveDoor(game, self.get(Position.class).position);
            }
        })),
        Map.entry("change", new Trigger((game, self, value) -> {
            if (self.has(Gravity.class)) {
                self.get(Gravity.class).changeGravity((PVector) value);
            }
        }))
    );
}
