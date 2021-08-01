package de.felixp.fractalsgdx.ui;

import com.kotcrab.vis.ui.widget.VisTextField;

import java.util.ArrayList;
import java.util.List;

public class TraversibleGroup {

    List<VisTraversibleValidateableTextField> fields = new ArrayList<>();

    public void addField(VisTraversibleValidateableTextField field){
        fields.add(field);
        field.setTraversibleGroup(this);
    }

    public void removeField(VisTraversibleValidateableTextField field){
        fields.remove(field);
        field.setTraversibleGroup(null);
    }

    public VisTraversibleValidateableTextField getNextField(VisTraversibleValidateableTextField currentField, boolean up){
        boolean next = false;
        VisTraversibleValidateableTextField previous = null;
        for (VisTraversibleValidateableTextField field : fields){

            if (field.isTraversalPaused()){
                continue;
            }

            if (!up && next)
                return field;
            if (!up && field == currentField)
                next = true;

            if (up) {
                if (field == currentField)
                    return previous != null ? previous : fields.get(fields.size()-1);
                previous = field;
            }
        }
        return next ? fields.get(0) : null;
    }
}
