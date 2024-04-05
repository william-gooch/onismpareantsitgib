package com.CS4303.group3;

import java.util.ArrayList;
import java.util.concurrent.PriorityBlockingQueue;

import com.CS4303.group3.plugin.Force_Plugin;
import com.CS4303.group3.plugin.Game_Plugin;
import com.CS4303.group3.plugin.Input_Plugin;
import com.CS4303.group3.plugin.Map_Plugin;
import com.CS4303.group3.plugin.Object_Plugin;
import com.CS4303.group3.plugin.Player_Plugin;
import com.CS4303.group3.plugin.Plugin_Interface;

import dev.dominion.ecs.api.Dominion;
import processing.core.*;

public class Game extends PApplet {
    //Global Variables
    public Dominion dom;
    public GameSchedule schedule;

    PriorityBlockingQueue<PriorityDrawOperation<?>> drawQueue = new PriorityBlockingQueue<PriorityDrawOperation<?>>();

    public static String level_name = "./src/main/java/com/CS4303/group3/levels/default_level.json";

    static {
        System.setProperty("dominion.show-banner", "false");
    }

    //Main method
    public static void main(String[] args) {
        if(args.length > 0) level_name = "./src/main/java/com/CS4303/group3/levels/" + args[0] + ".json";
        main(Game.class.getName());
    }

    //Settings --Does not run this for some reason -- ask how william got this bit to run
    public void settings() {
        fullScreen();
        size((int)Math.floor(displayWidth * 0.75), (int)Math.floor(displayHeight * 0.75));
    }

    //Setup
    public void setup() {
        // size((int)Math.floor(displayWidth * 0.75), (int)Math.floor(displayHeight * 0.75));

        //draw to the background render buffer
        int[][] test_map = new int[25][30];
        for(int i = 0; i < 30; i++) test_map[24][i] = 1;

        //start Dominion
        dom = Dominion.create();

        //start scheduler
        schedule = new GameSchedule(this);

        //
        addPlugin(new Game_Plugin());
        addPlugin(new Map_Plugin());
        addPlugin(new Input_Plugin());
        addPlugin(new Object_Plugin());
        addPlugin(new Player_Plugin());
        addPlugin(new Force_Plugin());
        

        // schedule._setup.tick();
    }

    private void addPlugin(Plugin_Interface plugin) {
        plugin.build(this);
    }

    //Game Loop
    public void draw() {
        background(0);

        schedule._update.tick();

        schedule._draw.tick();
        ArrayList<PriorityDrawOperation<?>> ops = new ArrayList<>(drawQueue.size());
        drawQueue.drainTo(ops);
        ops.stream()
            .forEach(op -> {
                op.operation.perform(this);
            });
    }

    //Renderings
    public void render() {
    }

    //Inputs
    public void keyPressed() {
        schedule._keydown.tick();
    }

    public void keyReleased() {
        schedule._keyup.tick();
    }

    //Queue stuff
    @FunctionalInterface
    public interface DrawOperation {
        void perform(Game game);
    }

    static class PriorityDrawOperation<T extends DrawOperation> implements Comparable<PriorityDrawOperation<?>> {
        T operation;
        long priority;

        public PriorityDrawOperation(T operation, long priority) {
            this.operation = operation;
            this.priority = priority;
        }

        @Override
        public int compareTo(PriorityDrawOperation<?> o) {
            return Long.valueOf(this.priority).compareTo(Long.valueOf(o.priority));
        }
    }
}
