/*
 * Copyright 2018 David Prentiss
 */

package sim.app.agentcity;

import sim.engine.*;
import sim.display.*;
import sim.util.*;
import sim.portrayal.grid.*;
import sim.portrayal.*;
import java.awt.*;
import javax.swing.*;

public class AgentCityWithUI extends GUIState {

    public Display2D display;
    public JFrame displayFrame;

    FastValueGridPortrayal2D roadPortrayal = new FastValueGridPortrayal2D("Road");
    SparseGridPortrayal2D agentPortrayal = new SparseGridPortrayal2D();

    public static void main(String[] args) {
        new AgentCityWithUI().createController();
    }

    public AgentCityWithUI() {
        //super(new AgentCity(System.currentTimeMillis()));
        super(new AgentCity(1324367673));
    }

    public AgentCityWithUI(SimState state) { super(state); }

    public Object getSimulationInspectedObject() { return state; }

    public static String getName() { return "Transportation City"; }

    public void setupPortrayals() {
        AgentCity ac = (AgentCity)state;

        // Road colors
        int numDir = Direction.values().length; // count Directions enum
        Color roadColors[] = new Color[numDir]; // make an array of colors
        roadColors[0] = new Color(0,0,0,0); // Direction.NONE is transparent
        for (int i = 1; i < numDir; i++) {
            roadColors[i] = new Color(111,110,99);
        }
        roadPortrayal.setField(ac.roadGrid);
        roadPortrayal.setMap(new sim.util.gui.SimpleColorMap(roadColors));

        // Agent Colors
        agentPortrayal.setField(ac.agentGrid);
        agentPortrayal.setPortrayalForClass(Vehicle.class,
                new sim.portrayal.simple.OvalPortrayal2D(Color.red));
        Bag vehicles = ac.agentGrid.getAllObjects();
        //if (ac.LANE_POLICY) {
        if (false) {
            Color newColor = new Color(0, 255, 0);
            agentPortrayal.setPortrayalForAll(new sim.portrayal.simple.OvalPortrayal2D()
                {
                    public void draw(Object object, Graphics2D graphics, DrawInfo2D info)
                    {
                        Vehicle vehicle = (Vehicle)object;
                        if (vehicle.hasPassengers) {
                            paint = new Color(0, 0, 255);
                        } else {
                            paint = new Color(0, 255, 0);
                        }
                        super.draw(object, graphics, info);
                    }
                });
        } else {
            for (int i = 0; i < vehicles.numObjs; i++) {
                Color newColor = new Color(ac.random.nextInt(255),
                                           ac.random.nextInt(255), ac.random.nextInt(255));
                agentPortrayal.setPortrayalForObject(vehicles.objs[i],
                                                     new sim.portrayal.simple.OvalPortrayal2D(newColor));
            }
        }

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
        AgentCity ac = (AgentCity)state;
        super.init(c);
        ac.console = (Console)c;

        // test comment for branching issue
        display = new Display2D(4*1024, 4*2, this);
        displayFrame = display.createFrame();
        c.registerFrame(displayFrame);
        displayFrame.setVisible(true);

        display.attach(roadPortrayal, "Road");
        display.attach(agentPortrayal, "Agents");

        display.setBackdrop(Color.white);
    }

    public void quit() {
        super.quit();
        if (displayFrame != null) displayFrame.dispose();
        displayFrame = null;
        display = null;
    }
}
