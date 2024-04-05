package com.CS4303.group3.levels;

import java.io.File;
import java.io.IOException;
import java.util.List;

import com.CS4303.group3.utils.Map;
import com.CS4303.group3.utils.Map.Ground_Tile;

import processing.core.*;
import com.fasterxml.jackson.databind.*;

public class Level_Creator extends PApplet {

    String file_name = "./src/main/java/com/CS4303/group3/levels/test_level.json";
    
    public static void main(String[] args) {
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
        map = new Map();
        current_pos = new PVector(0,0);
        mapper = new ObjectMapper();
        // mapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
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
