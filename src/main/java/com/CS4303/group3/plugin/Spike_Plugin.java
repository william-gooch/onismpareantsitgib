package com.CS4303.group3.plugin;

import com.CS4303.group3.Game;

public class Spike_Plugin implements Plugin_Interface {
    @Override
    public void build(Game game) {
        //draw the spikes
    }

    public static class Spikes {
        float width, height;

        public Spikes(float width, float height) {
            this.width = width;
            this.height = height;
        }
    }
}
