package Kurama.ComponentSystem.automations;

import Kurama.ComponentSystem.components.Component;
import Kurama.Math.Vector;
import Kurama.inputs.Input;

public class VerticalBoundaryDrag implements Automation {
    @Override
    public void run(Component current, Input input, float timeDelta) {

        Vector newPos = current.getPos().add(new Vector(input.mouseDx, 0, 0));

        // This method will always return true unless overridden
        if(current.isValidLocation(newPos, current.getWidth(), current.getHeight())) {
            current.getPos().setDataElement(0, newPos.get(0));
        }

//        for(Component attachedComp: ((VerticalBoundary)current).attachedComponents.get(BoundaryType.LEFT)) {
//
//        }
//
//        for(Component attachedComp: ((VerticalBoundary)current).attachedComponents.get(BoundaryType.RIGHT)) {
//
//        }
    }
}
