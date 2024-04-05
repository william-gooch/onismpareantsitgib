package com.CS4303.group3.levels;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import com.CS4303.group3.utils.Map;
import com.CS4303.group3.utils.Map.Ground_Tile;

import processing.core.*;
import com.fasterxml.jackson.databind.*;

public class Level_Creator extends PApplet {

    static String file_name = "./src/main/java/com/CS4303/group3/levels/default_level.json";
    static String input = "";
    
    public static void main(String[] args) {
        if(args.length > 0) file_name = "./src/main/java/com/CS4303/group3/levels/" + args[0] + ".json";
        if(args.length > 1) input = "./src/main/java/com/CS4303/group3/levels/" + args[1] + ".json";
        main(Level_Creator.class.getName());
    }

    public void settings() {
        fullScreen();
    }

    Map map;
    PVector current_pos;
    PVector size;
    boolean started = false;

    ObjectMapper mapper;

    public void setup() {
        mapper = new ObjectMapper();

        if(input == "") map = new Map();
        else try {map = mapper.readValue(new File(input), Map.class);} catch(IOException e) {e.printStackTrace();}
        current_pos = new PVector(0,0);
    }

    public void draw() {
        //draw everything in the Map class
        map.draw(this);

        if(started) {
            //draw a ground section to the mouse position
            rectMode(CORNERS);
            rect(current_pos.x, current_pos.y, mouseX, mouseY);
            rectMode(CORNER);
        }
    }

    public void keyPressed() {
        if(key == 's') {
            //save the map
            try {
                mapper.writeValue(new File(file_name), map);
            } catch(IOException e) {
                e.printStackTrace();
            }
        }

        if(key == 'd') {
            //find the object that is hovered over
            //and delete it
            Iterator<Ground_Tile> it = map.ground_tiles.iterator();
            while(it.hasNext()) {
                Ground_Tile ground_tile = it.next();
                if(is_in(ground_tile.position, ground_tile.size)) it.remove();
            }
            
        }
    }

    //checks if the mouse is over the object
    private boolean is_in(PVector object, PVector size) {
        return mouseX >= object.x
                && mouseX <= object.x + size.x
                && mouseY >= object.y
                && mouseY <= object.y + size.y;
    }

    public void mousePressed() {
        started = true;
        current_pos.set(mouseX, mouseY);
    }

    public void mouseReleased() {
        //calc the top left position and sizes
        started = false;
        map.add_ground_tile(new PVector(min(current_pos.x, mouseX), min(current_pos.y, mouseY)), new PVector(max(current_pos.x, mouseX) - min(current_pos.x, mouseX), max(current_pos.y, mouseY) - min(current_pos.y, mouseY)));
    }
}
