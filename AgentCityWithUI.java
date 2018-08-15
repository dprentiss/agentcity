/*
 * Copyright 2018 David Prentiss
 */

package sim.app.agentcity;

import sim.engine.*;
import sim.display.*;
import sim.portrayal.grid.*;
import java.awt.*;
import javax.swing.*;

public class AgentCityWithUI extends GUIState {

    public Display2D display;
    public JFrame displayFrame;

    FastValueGridPortrayal2D roadPortrayal = new FastValueGridPortrayal2D("Road");
    FastValueGridPortrayal2D blockPortrayal = new FastValueGridPortrayal2D("Block", true);
    FastValueGridPortrayal2D intersectionPortrayal = new FastValueGridPortrayal2D("Intersection");

    SparseGridPortrayal2D agentPortrayal = new SparseGridPortrayal2D();

    public static void main(String[] args) {
        new AgentCityWithUI().createController();
    }

    public AgentCityWithUI() { super(new AgentCity(System.currentTimeMillis())); }
    public AgentCityWithUI(SimState state) { super(state); }

    public Object getSimulationInspectedObject() { return state; }

    public static String getName() { return "Transportation City"; }

    public void setupPortrayals() {
        AgentCity tc = (AgentCity)state;

        // Road colors
        int numDir = AgentCity.Direction.values().length; // count Directions enum
        Color roadColors[] = new Color[numDir]; // make an array of colors
        roadColors[0] = new Color(0,0,0,0); // Direction.NONE is transparent
        for (int i = 1; i < numDir; i++) {
            roadColors[i] = new Color(0,0,0,128);
        }
        roadPortrayal.setField(tc.roadGrid); 
        roadPortrayal.setMap(new sim.util.gui.SimpleColorMap(roadColors));

        // Block colors
        Color blockColors[] = new Color[2];
        blockColors[0] = new Color(0,0,0,0);
        blockColors[1] = new Color(0,255,0,128);
        blockPortrayal.setField(tc.blockGrid);
        blockPortrayal.setMap(new sim.util.gui.SimpleColorMap(blockColors));

        // Intersection colors
        Color intersectionColors[] = new Color[2];
        intersectionColors[0] = new Color(0,0,0,0);
        intersectionColors[1] = new Color(0,0,0,64);
        intersectionPortrayal.setField(tc.intersectionGrid);
        intersectionPortrayal.setMap(new sim.util.gui.SimpleColorMap(intersectionColors));

        //agentPortrayal.setField(tc.agentGrid);

        display.reset();
        display.repaint();
    }

    public void start() {
        super.start();
        setupPortrayals();
    }

    public void load(SimState state) {
        super.load(state);
        setupPortrayals();
    }

    public void init(Controller c) {
        super.init(c);

        display = new Display2D(400,400,this);
        displayFrame = display.createFrame();
        c.registerFrame(displayFrame);
        displayFrame.setVisible(true);

        display.attach(roadPortrayal, "Road");
        display.attach(blockPortrayal, "Block");
        display.attach(intersectionPortrayal, "Intersection");
        //display.attach(agentPortrayal, "Agents");

        display.setBackdrop(Color.white);
    }

    public void quit() {
        super.quit();
        if (displayFrame != null) displayFrame.dispose();
        displayFrame = null;
        display = null;
    }
}
