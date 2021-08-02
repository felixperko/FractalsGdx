package de.felixp.fractalsgdx.ui.actors;

import com.kotcrab.vis.ui.util.InputValidator;
import com.kotcrab.vis.ui.widget.VisTextField;
import com.kotcrab.vis.ui.widget.VisValidatableTextField;

public class VisTraversibleValidateableTextField extends VisValidatableTextField {

    public VisTraversibleValidateableTextField() {
    }

    public VisTraversibleValidateableTextField(InputValidator validator) {
        super(validator);
    }

    boolean traversalPaused = false;

    TraversibleGroup traversibleGroup;

    public void setTraversibleGroup(TraversibleGroup traversibleGroup){
        this.traversibleGroup = traversibleGroup;
    }

    public void setTraversalPaused(boolean traversalPaused) {
        this.traversalPaused = traversalPaused;
    }

    public boolean isTraversalPaused(){
        return traversalPaused;
    }

    @Override
    public void next(boolean up) {

        if (traversibleGroup == null)
            return;
        VisTextField field = traversibleGroup.getNextField(this, up);
        if (field == null)
            return;
        field.focusField();
        field.setCursorPosition(field.getText().length());

//        Stage stage = getStage();
//        if (stage == null) return;
//        getParent().localToStageCoordinates(tmp1.set(getX(), getY()));
//        VisTextField textField = findNextTextField(stage.getActors(), null, tmp2, tmp1, up);
//        if (textField == null) { // Try to wrap around.
//            if (up)
//                tmp1.set(Float.MIN_VALUE, Float.MIN_VALUE);
//            else
//                tmp1.set(Float.MAX_VALUE, Float.MAX_VALUE);
//            textField = findNextTextField(getStage().getActors(), null, tmp2, tmp1, up);
//        }
//        if (textField != null) {
//            textField.focusField();
//            textField.setCursorPosition(textField.getText().length());
//        } else
//            keyboard.show(false);
    }
}
