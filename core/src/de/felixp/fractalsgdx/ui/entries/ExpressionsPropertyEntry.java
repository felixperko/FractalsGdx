package de.felixp.fractalsgdx.ui.entries;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.Tree;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.UIUtils;
import com.kotcrab.vis.ui.util.InputValidator;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisTable;
import com.kotcrab.vis.ui.widget.VisTextButton;
import com.kotcrab.vis.ui.widget.VisTextField;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.felixp.fractalsgdx.FractalsGdxMain;
import de.felixp.fractalsgdx.ui.MainStage;
import de.felixp.fractalsgdx.ui.actors.FractalsWindow;
import de.felixp.fractalsgdx.ui.actors.TabTraversableTextField;
import de.felixperko.expressions.ComputeExpressionBuilder;
import de.felixperko.expressions.FractalsExpression;
import de.felixperko.fractals.data.ParamContainer;
import de.felixperko.fractals.system.parameters.ExpressionsParam;
import de.felixperko.fractals.system.parameters.ParamDefinition;
import de.felixperko.fractals.system.parameters.suppliers.ParamSupplier;
import de.felixperko.fractals.system.parameters.suppliers.StaticParamSupplier;
import de.felixperko.expressions.FractalsExpressionParser;

public class ExpressionsPropertyEntry extends AbstractSingleTextPropertyEntry {

    static Pattern functionNameWhitespacesPattern = Pattern.compile("[a-z]+\\s?[a-z]+\\s?\\(");
    static Pattern variablePattern = Pattern.compile("([A-Za-z]+[0-9]?((_(n|\\(n(-[1-9][0-9]?)\\))?)?|(\\(.+\\))?)?)");
    static Pattern variableAndConstantPattern = Pattern.compile("(-?[0-9]+(\\.[0-9]?)?)|([A-Za-z]+[0-9]?((\\(.+\\))?|(_(n|\\(n(-[1-9][0-9]?)\\))?)?))");
    static Pattern missingBracketPattern = Pattern.compile("(\\^|\\*)-(([0-9]+(\\.[0-9]?)?)|([A-Za-z]+[0-9]?((\\(.+\\))?|((_(n|\\(n(-[1-9][0-9]?)\\))?)?))))");

    static List<String> staticAutocompleteTerms = new ArrayList<>(Arrays.asList(
            "abs(", "sin(", "cos(", "tan(", "sinh(", "cosh(", "tanh(", "log(", "negate(",
            "absr(", "absi(",
            "sinr(", "sini(",
            "cosr(", "cosi(",
            "tanr(", "tani(",
            "sinhr(", "sinhi(",
            "coshr(", "coshi(",
            "tanhr(", "tanhi(",
            "logr(", "logi(",
            "negater(", "negatei("
    ));

    List<String> autocompleteOptions = new ArrayList<>();
    int autocompleteIndex = 0;

    ExpressionsParam expressionsParam;

    public ExpressionsPropertyEntry(Tree.Node node, ParamContainer paramContainer, ParamDefinition parameterDefinition, boolean submitValue) {
        super(node, paramContainer, parameterDefinition, new InputValidator() {
            @Override
            public boolean validateInput(String input) {
                try {
                    FractalsExpression expr = FractalsExpressionParser.parse(input);
                    if (expr == null)
                        return false;
                    return true;
                } catch (IllegalArgumentException e) {
                    return false;
                }
            }
        }, submitValue);
        ParamSupplier clientParameter = paramContainer.getClientParameter(propertyName);
        if (clientParameter != null) {
            expressionsParam = clientParameter.getGeneral(ExpressionsParam.class);
            text = expressionsParam.getMainExpression();
        }
        showMenu = true;
    }

    public List<String> updateAutocompleteOptions(String extracted){

        List<String> options = new ArrayList<>();

        if (extracted.contains("z")){
            options.add("z");
            options.add("z_n");
            options.add("z_(n-1)");
        }

        for (String str : staticAutocompleteTerms){
            if (str.contains(extracted))
                options.add(str);
        }
        for (String paramName : paramContainer.getClientParameters().keySet()) {
            if (paramName.contains(extracted) && !paramName.equalsIgnoreCase(getPropertyName())) {
                options.add(paramName);
                if (!paramName.endsWith("_0"))
                    options.add(paramName+"_(n-1)");
            }
        }
        return options;
    }

    public String extractCurrentTerm(String input, int cursorIndex) {
        Matcher autocompleteMatcher = variablePattern.matcher(input);
        String extracted = "";
        while (autocompleteMatcher.find()){
            if (autocompleteMatcher.start() >= cursorIndex){
                break;
            }
            String group = autocompleteMatcher.group();
            if (group.length() > 0) {
                int end = autocompleteMatcher.end();
                int cut = end - cursorIndex + 1;
                int lengthAfter = group.length() - cut;
                if (lengthAfter <= 0 || lengthAfter > group.length())
                    return "";
                extracted = group.substring(0, lengthAfter);
            }
        }
        return extracted;
    }

    boolean autocompletionActive = false;
    String extracted = "";

    @Override
    public void addTextFieldListener(TabTraversableTextField textField) {
        textField.setProgrammaticChangeEvents(true);
//        TabTraversableTextField textField = super.createTextField();
        textField.addListener(new ChangeListener() {

            @Override
            public void changed(ChangeEvent event, Actor actor) {
                System.out.println(System.nanoTime()+" ChangeListener "+this);
                if (Gdx.input.isKeyPressed(Input.Keys.CONTROL_LEFT) && Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
//                    event.cancel();
                    //autocomplete, insert parentheses
                    String input = textField.getText();
                    int cursorPosition = textField.getCursorPosition();
                    String extracted = extractCurrentTerm(input, textField.getCursorPosition());
                    if (!extracted.isEmpty()) {

                        List<String> autocompleteOptions = updateAutocompleteOptions(extracted);
                        if (autocompleteOptions.isEmpty())
                            return;
                        if (!autocompletionActive || autocompleteIndex >= autocompleteOptions.size())
                            autocompleteIndex = 0;
                        String autocomplete = autocompleteOptions.isEmpty() ? "" : autocompleteOptions.get(autocompleteIndex);
                        Matcher matcher2 = variableAndConstantPattern.matcher(input);
                        autocompleteIndex++;
                        if (!autocompletionActive && matcher2.find(Math.max(0, cursorPosition - 1))) {
                            String nextVariableString = matcher2.group();

                            String replacingString = autocomplete + nextVariableString;
                            if (autocomplete.endsWith("("))
                                replacingString = replacingString + ")";

                            input = input.replace(extracted +" "+ nextVariableString, replacingString);
                            autocompletionActive = true;

                        } else if (autocompletionActive) {
//                            String currentTerm = extractCurrentTerm(input, textField.getCursorPosition());
                            String replace = null;
                            Matcher matcherFunctionName = functionNameWhitespacesPattern.matcher(input);
                            while (matcherFunctionName.find(0)){
                                if (matcherFunctionName.end() < cursorPosition)
                                    continue;
                                replace = matcherFunctionName.group();
                                break;
                            }
//                            if (autocomplete.endsWith("("))
//                                autocomplete = autocomplete.substring(0, autocomplete.length() - 1);
                            if (replace != null)
                                input = input.replace(replace, autocomplete);
                        }
                        if (!input.equals(text)){
                            setValue(input);
                            textField.setText(input);
                        }
//                        final String finalInput = input;
                        textField.setProgrammaticChangeEvents(true);
//                        textField.setText(input);
//                        textField.appendText("");
//                        Gdx.app.postRunnable(new Runnable() {
//                            @Override
//                            public void run() {
//                                textField.setText(finalInput);
                                textField.setCursorPosition(Math.max(cursorPosition-1, 0));
//                            }
//                        });
                    }
                }
            }
        });
        textField.setTextFieldFilter(new VisTextField.TextFieldFilter() {
            @Override
            public boolean acceptChar(VisTextField textField, char c) {
                boolean accept = c != 32 || !UIUtils.ctrl();
                if (!accept){
                    Gdx.app.postRunnable(new Runnable() {
                        @Override
                        public void run() {
                            autoComplete(textField);
                        }
                    });
                }
                return true;
            }
        });
        textField.setTextFieldListener(new VisTextField.TextFieldListener() {
            @Override
            public void keyTyped(VisTextField textField, char c) {
                System.out.println(System.nanoTime()+" keyTyped "+this);
                try {
                    boolean setValue = false;
                    int cursorPosition = textField.getCursorPosition();
//                    String input = textField.getText();
                    String input = text;
                    Matcher matcher = missingBracketPattern.matcher(input);
                    int replacedIndex = -1;
                    if (c == '-' && matcher.find()) {
                        //add missing bracket
                        String missingBrackets = matcher.group();
                        int start = matcher.start();
                        int end = matcher.end();
                        if (input.length() <= end || input.charAt(end) != ')') {
                            input = input.replace(missingBrackets, missingBrackets.substring(0, 1) + "(" + missingBrackets.substring(1) + ")");
                            textField.setCursorPosition(cursorPosition+1);
//                            textField.setText(input);
//                            setValue(input);
                            setValue = true;
//                            if (cursorPosition >= start - 1) {
//                            }
                        }
                    }
//                    else if (Gdx.input.isKeyPressed(Input.Keys.CONTROL_LEFT) && Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
////                        input = input.substring(0, cursorPosition) + input.substring(cursorPosition+1);
//                        textField.setText(input);
//                        textField.setCursorPosition(cursorPosition);
//
//                    }
                    else if (Character.isAlphabetic(c) || Character.isDigit(c)){
                        autocompletionActive = false;
                        extracted = "";
                    } else if (c == '\u007F' || c == '\b') { //delete
                        //remove unpaired parentheses
//                        int cursorPosition = textField.getCursorPosition();
                        if (text.charAt(cursorPosition) == '(') {
                            int searchingClosing = 1;
                            for (int i = cursorPosition; i < input.length(); i++) {
                                if (input.charAt(i) == '(')
                                    searchingClosing++;
                                else if (input.charAt(i) == ')') {
                                    if (--searchingClosing == 0) {
                                        input = input.substring(0, i) + input.substring(i + 1);
                                        replacedIndex = i;
//                                        if (textField.getCursorPosition() > i)
                                            textField.setCursorPosition(textField.getCursorPosition()+1);
                                    }
                                }
                            }
                        } else if (text.charAt(cursorPosition) == ')') {
                            int searchingOpening = 1;
                            for (int i = cursorPosition - 1; i >= 0; i--) {
                                if (input.charAt(i) == ')')
                                    searchingOpening++;
                                else if (input.charAt(i) == '(') {
                                    if (--searchingOpening == 0) {
                                        input = input.substring(0, i) + input.substring(i + 1);
                                        replacedIndex = i;
                                    }
                                }
                            }
                        }
                    }
                    if (setValue || replacedIndex >= 0) {
                        String finalInput = input;
                        int finalReplacedIndex = replacedIndex;
//                        Gdx.app.postRunnable(new Runnable() {
//                            @Override
//                            public void run() {
                                textField.setText(input);
                                text = input;
//                                setValue(finalInput);
                                textField.setCursorPosition(cursorPosition >= finalReplacedIndex ? cursorPosition - 1 : cursorPosition - 1);
//                            }
//                        });
                    }
                } catch (StringIndexOutOfBoundsException e){
                    e.printStackTrace();
                }
            }
        });
//        return textField;
    }

    public boolean autoComplete(VisTextField textField) {
        String extracted = extractCurrentTerm(text, textField.getCursorPosition());
        int cursorPosition = textField.getCursorPosition();
        textField.setText(text);
//                    textField.setCursorPosition(cursorPosition);
        List<String> autocompleteOptions = updateAutocompleteOptions(extracted);
        if (autocompleteOptions.isEmpty())
            return true;
        if (!autocompletionActive || autocompleteIndex >= autocompleteOptions.size())
            autocompleteIndex = 0;
        String autocomplete = autocompleteOptions.isEmpty() ? "" : autocompleteOptions.get(autocompleteIndex);
        Matcher matcher2 = variableAndConstantPattern.matcher(text);
        autocompleteIndex++;
        if (!autocompletionActive && matcher2.find(Math.max(0, cursorPosition - 1))) {
            String nextVariableString = matcher2.group();

            String replacingString = autocomplete + nextVariableString;
            if (autocomplete.endsWith("("))
                replacingString = replacingString + ")";

            text = text.replace(extracted + " " + nextVariableString, replacingString);
            autocompletionActive = true;
        }
        return false;
    }

    @Override
    protected void setOptionButtonListener(Button optionButton) {
        optionButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                openExpressionsWindow();
            }
        });
    }

    protected void openExpressionsWindow() {

        MainStage stage = ((MainStage) FractalsGdxMain.stage);
        FractalsWindow exprWindow = new FractalsWindow("Expressions");
        VisTable contentTable = new VisTable(true);

//        ExpressionsParam expressionsParam = getSupplier().getGeneral(ExpressionsParam.class);
//        ComputeExpressionBuilder builder = new ComputeExpressionBuilder(expressionsParam, paramContainer.getClientParameters());

        Map<String, TabTraversableTextField> fieldMap = new HashMap<>();

        VisTable expressionsTable = new VisTable(true);
        for (Map.Entry<String, String> e : expressionsParam.getExpressions().entrySet()){
            VisLabel exprLabel = new VisLabel(e.getKey()+"_(n+1) = ");
            TabTraversableTextField exprField = new TabTraversableTextField(e.getValue());
            exprField.addValidator(validator);
            exprField.addListener(new InputListener(){
                @Override
                public boolean keyDown(InputEvent event, int keycode) {
                    if (keycode == Input.Keys.ENTER){
                        submitIfValid(fieldMap);
                        return true;
                    }
                    return super.keyDown(event, keycode);
                }
            });

            expressionsTable.add(exprLabel);
            expressionsTable.add(exprField).row();
            fieldMap.put(e.getKey(), exprField);
        }

        VisTextButton submitButton = new VisTextButton("submit");
        submitButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {

                submitIfValid(fieldMap);
            }
        });

        contentTable.add(expressionsTable).row();
        contentTable.add(submitButton).row();

        exprWindow.add(contentTable);
        exprWindow.addCloseButton();
        stage.addActor(exprWindow);
        exprWindow.pack();
        exprWindow.centerWindow();
    }

    public void submitIfValid(Map<String, TabTraversableTextField> fieldMap) {
        for (TabTraversableTextField field : fieldMap.values()){
            if (!field.isInputValid())
                return;
        }

        for (Map.Entry<String, TabTraversableTextField> e : fieldMap.entrySet()){
            expressionsParam.putExpression(e.getKey(), e.getValue().getText());
        }
        boolean actualSubmitValue = submitValue;
        submitValue = true;
        submit();
        submitValue = actualSubmitValue;
    }

    @Override
    public ParamSupplier getSupplier() {
        Map<String, String> exprs = expressionsParam.getExpressions();
        expressionsParam = new ExpressionsParam(text, expressionsParam.getMainInputVar());
        expressionsParam.putExpressions(exprs);
        expressionsParam.putExpression(expressionsParam.getMainInputVar(), text);
        StaticParamSupplier supplier = new StaticParamSupplier(getPropertyName(), expressionsParam);
        supplier.setChanged(exprs.equals(expressionsParam.getExpressions()));
        supplier.setLayerRelevant(true);
        return supplier;
    }

    @Override
    protected boolean checkValue(Object valueObj) {
        return valueObj instanceof ExpressionsParam;
    }

    @Override
    protected void setCheckedValue(Object newValue) {
        if (!this.expressionsParam.equals(newValue)) {
            expressionsParam = ((ExpressionsParam) newValue);
            super.setCheckedValue(newValue);
        }
    }

    @Override
    public String getText(Object value) {
        return ((ExpressionsParam)value).getMainExpression();
    }

    @Override
    protected Object getDefaultObject() {
        return "z_n^2+c";
    }
}

