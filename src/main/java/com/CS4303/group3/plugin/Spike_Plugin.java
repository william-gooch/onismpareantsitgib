package com.CS4303.group3.plugin;

import com.CS4303.group3.Game;

public class Spike_Plugin implements Plugin_Interface {
    @Override
    public void build(Game game) {
        //draw the spikes
        game.schedule.draw(-1, draw -> {
                 game.dom.findEntitiesWith(Object_Plugin.Position.class, Spikes.class)
                     .stream().forEach(res -> {
                         var pos = res.comp1().position;
                         draw.call(drawing -> {
                             //draw the player character
                             drawing.fill(128,0,0);
                             drawing.rect(pos.x, pos.y, res.comp2().width, res.comp2().height);
                         });
                     });
             });
    }

    public static class Spikes {
        float width, height;

        public Spikes(float width, float height) {
            this.width = width;
            this.height = height;
        }
    }
}
