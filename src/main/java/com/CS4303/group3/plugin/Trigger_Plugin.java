package com.CS4303.group3.plugin;

import java.util.function.Consumer;

import dev.dominion.ecs.api.*;

public class Trigger_Plugin {
    static class Trigger {
        Entity self;
        Consumer<Entity> onTrigger;

        public Trigger(Entity self, Consumer<Entity> onTrigger) {
            this.onTrigger = onTrigger;
        }

        public void trigger() {
            this.onTrigger.accept(self);
        }
    }
}
