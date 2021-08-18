package de.felixp.fractalsgdx.ui.entries;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.Tree;
import com.badlogic.gdx.scenes.scene2d.utils.ArraySelection;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.UIUtils;
import com.badlogic.gdx.utils.Array;
import com.kotcrab.vis.ui.util.InputValidator;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisList;
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
import de.felixp.fractalsgdx.ui.actors.TraversableGroup;
import de.felixperko.expressions.ComputeExpressionBuilder;
import de.felixperko.expressions.ComputeExpressionDomain;
import de.felixperko.expressions.ExpressionSymbol;
import de.felixperko.expressions.FractalsExpression;
import de.felixperko.fractals.data.ParamContainer;
import de.felixperko.fractals.system.calculator.ComputeExpression;
import de.felixperko.fractals.system.calculator.ComputeInstruction;
import de.felixperko.fractals.system.parameters.ExpressionsParam;
import de.felixperko.fractals.system.parameters.ParamDefinition;
import de.felixperko.fractals.system.parameters.suppliers.ParamSupplier;
import de.felixperko.fractals.system.parameters.suppliers.StaticParamSupplier;
import de.felixperko.expressions.FractalsExpressionParser;

import static de.felixperko.fractals.system.calculator.ComputeInstruction.*;

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
    private Map<String, TabTraversableTextField> windowExpressionFields = new HashMap<>();

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

        windowExpressionFields.clear();

        TraversableGroup traversableGroup = new TraversableGroup();

        MainStage stage = ((MainStage) FractalsGdxMain.stage);
        FractalsWindow exprWindow = new FractalsWindow("Expressions");
        VisTable contentTable = new VisTable(true);

        Map<String, TabTraversableTextField> fieldMap = new HashMap<>();

        VisTable exprTable = new VisTable(true);
        for (Map.Entry<String, String> e : expressionsParam.getExpressions().entrySet()){
            final String inputVarName = e.getKey();
            VisLabel exprLabel = new VisLabel(inputVarName +"_(n+1) = ");
            TabTraversableTextField exprField = new TabTraversableTextField(e.getValue());
            traversableGroup.addField(exprField);
            exprField.setPrefWidth(300);
            exprField.addValidator(validator);
            exprField.addListener(new InputListener(){
                @Override
                public boolean keyDown(InputEvent event, int keycode) {
                    if (keycode == Input.Keys.ENTER){
                        submitIfValidAndUpdateWindow(fieldMap, contentTable, exprTable, exprWindow);
                        FractalsGdxMain.stage.setKeyboardFocus(exprField);
                        return true;
                    }
                    if (keycode == Input.Keys.ESCAPE) {
                        ((MainStage)FractalsGdxMain.stage).escapeHandled();
                        exprWindow.remove();
                    }
                    return true;
                }

                @Override
                public boolean keyTyped(InputEvent event, char character) {
                    if (character == '\t')
                        return false;
                    //inputVarName.equals(expressionsParam.getMainInputVar()) && !exprField.getText().equals(text) &&
                    if (exprField.isInputValid()) {
                        readFields();
                        expressionsParam.putExpression(inputVarName, exprField.getText());
                        updateExpressionWindowContent(contentTable, fieldMap, exprTable, exprWindow);
                        FractalsGdxMain.stage.setKeyboardFocus(exprField);
                    }
                    return super.keyTyped(event, character);
                }
            });
            windowExpressionFields.put(inputVarName, exprField);

            exprTable.add(exprLabel);
            exprTable.add(exprField).row();
            fieldMap.put(inputVarName, exprField);
        }

        updateExpressionWindowContent(contentTable, fieldMap, exprTable, exprWindow);

        exprWindow.add(contentTable);
        exprWindow.addCloseButton();
        stage.addActor(exprWindow);
        exprWindow.pack();
        exprWindow.centerWindow();
    }

    public boolean submitIfValidAndUpdateWindow(Map<String, TabTraversableTextField> fieldMap,
                VisTable contentTable, VisTable expressionsTable, FractalsWindow expressionsWindow) {
        readFields();
        boolean submitted = submitIfValid(fieldMap);
        if (submitted) {
            updateExpressionWindowContent(contentTable, fieldMap, expressionsTable, expressionsWindow);
        }
        return submitted;
    }

    private void updateExpressionWindowContent(VisTable contentTable, Map<String, TabTraversableTextField> fieldMap,
                VisTable expressionsTable, FractalsWindow expressionsWindow) {
        contentTable.clear();

        ExpressionsParam expressionsParam = getSupplier().getGeneral(ExpressionsParam.class);
        ComputeExpressionBuilder builder = new ComputeExpressionBuilder(expressionsParam, paramContainer.getClientParameters());
        ComputeExpressionDomain domain = null;
        String parseErrorMessage;

        try {
            parseErrorMessage = null;
            domain = builder.getComputeExpressionDomain(false);
        } catch (IllegalArgumentException e){
            parseErrorMessage = e.getMessage();
            domain = new ComputeExpressionDomain(new ComputeExpression("", new ArrayList<>(), new HashMap<>(), 0, new HashMap<>(), new HashMap<>(), 0));
        }

        if (parseErrorMessage != null){
            contentTable.add(expressionsTable).row();
            contentTable.add("Parse error: "+parseErrorMessage).row();
            expressionsWindow.pack();
            return;
        }

        VisList<String> paramList = new VisList<>();
        paramList.setSelection(new ArraySelection<>(new Array<>()));
        List<String> params = new ArrayList<>();
        ComputeExpression firstExpression = domain.getMainExpressions().get(0);

        Map<Integer, String> copySlotVariables = new HashMap<>();

        int counter = 0;
        for (ParamSupplier supp : firstExpression.getParameterList()){
            String varName = supp.getName();
            if (varName.endsWith("_0"))
                varName = varName.substring(0, varName.length()-2);
            boolean isIt = varName.equals("n");
            boolean isConst = varName.startsWith("CON_");
            boolean isVar = !isIt && !isConst;
            if (!isVar)
                varName = varName.replaceFirst("_", " - ");

            ExpressionSymbol symbol = builder.getExpressionSymbol(supp.getName());
            if (symbol.getCopyIndices() != null) {
                for (int index : symbol.getCopyIndices()) {
                    copySlotVariables.put(index, varName);
                }
            }

            params.add(counter+": "+(isVar ? "VAR - " : isIt ? "ITE - " : "")+varName);
            counter++;
        }
        Map<String, Integer> copyCounterList = new HashMap<>();
        for (Integer copySlot : copySlotVariables.keySet()){
            String param = copySlotVariables.get(copySlot);
            int copyCounter = copyCounterList.getOrDefault(param, 1);
            params.add((copySlot/2)+": CPY - "+param+"_copy"+(copyCounter > 1 ? copyCounter : ""));
            copyCounterList.put(param, ++copyCounter);
        }
        paramList.setColor(Color.BLUE.cpy());
        paramList.setItems(new Array(params.toArray()));

        VisTable paramTable = new VisTable(true);
        paramTable.add("Parameters: ").row();
        paramTable.add(paramList);

        VisList<String> instructionList = new VisList<>();
        instructionList.setSelection(new ArraySelection<>(new Array<>()));
        List<String> instructionValues = new ArrayList<>();
        List<String> instructionVarsValues = new ArrayList<>();
        List<ComputeInstruction> instructions = firstExpression.getInstructions();
        for (ComputeInstruction instruction : instructions){
            String[] instrStrings = getInstrStrings(instruction, params, copySlotVariables);
            instructionValues.add(instrStrings[0]);
            instructionVarsValues.add(instrStrings[1]);
        }
        instructionList.setItems(new Array(instructionValues.toArray()));

        VisList<String> instructionVarsList = new VisList<>();
        instructionVarsList.setItems(new Array(instructionVarsValues.toArray()));

        paramList.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                String selected = paramList.getSelected();
                List<String> list = new ArrayList<>();

                String paramName = selected;
                for (String instruction : instructionList.getItems()){
                    if (instruction.contains(paramName))
                        list.add(instruction);
                }

                Array<String> array = new Array(list.toArray());
                instructionList.setSelection(new ArraySelection<>(array));
            }
        });

        VisTable instrTable = new VisTable();
        instrTable.add("Instructions:").colspan(2).row();
        instrTable.add(instructionList);
        instrTable.add(instructionVarsList);

        VisTextButton submitButton = new VisTextButton("submit");
        submitButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                submitIfValidAndUpdateWindow(fieldMap, contentTable, expressionsTable, expressionsWindow);
            }
        });

        contentTable.add(expressionsTable).colspan(3).row();
        contentTable.add(paramTable).top().left().expandX().fillX();
        contentTable.addSeparator(true);
        contentTable.add(instrTable).top().left().expandX().fillX().row();
        contentTable.add(submitButton).colspan(3).row();
        expressionsWindow.pack();
    }

    static Map<Integer, String> instrNames = new HashMap<Integer, String>(){
        {
            put(INSTR_ADD_COMPLEX,          "add        ");
            put(INSTR_SUB_COMPLEX,          "sub        ");
            put(INSTR_MULT_COMPLEX,         "mult       ");
            put(INSTR_DIV_COMPLEX,          "div        ");
            put(INSTR_POW_COMPLEX,          "power      ");
            put(INSTR_COPY_COMPLEX,         "copy       ");
            put(INSTR_ABS_COMPLEX,          "abs        ");
            put(INSTR_SIN_COMPLEX,          "sin        ");
            put(INSTR_COS_COMPLEX,          "cos        ");
            put(INSTR_TAN_COMPLEX,          "tan        ");
            put(INSTR_SINH_COMPLEX,         "sinh       ");
            put(INSTR_COSH_COMPLEX,         "cosh       ");
            put(INSTR_TANH_COMPLEX,         "tanh       ");
            put(INSTR_SQUARE_COMPLEX,       "square     ");
            put(INSTR_NEGATE_COMPLEX,       "negate     ");
            put(INSTR_RECIPROCAL_COMPLEX,   "recipr     ");
            put(INSTR_LOG_COMPLEX,          "log        ");
        }
    };
    static {
        for (Integer key : new ArrayList<>(instrNames.keySet())){
            instrNames.put(key+OFFSET_INSTR_PART, instrNames.get(key));
        }
    }

    private String[] getInstrStrings(ComputeInstruction instruction, List<String> params, Map<Integer, String> copySlotVariables) {

        String instrName = instrNames.getOrDefault(instruction.type, "unknown");
        StringBuilder sb = new StringBuilder();
        appendParamReferenceString(sb, params, copySlotVariables, instruction.fromReal, instruction.fromImag, true);
        if (instruction.fromImag != instruction.fromReal+1)
            appendParamReferenceString(sb, params, copySlotVariables, instruction.fromImag, -1, false);
        appendParamReferenceString(sb, params, copySlotVariables, instruction.toReal, instruction.toImag, false);
        if (instruction.fromImag != instruction.fromReal+1)
            appendParamReferenceString(sb, params, copySlotVariables, instruction.toImag, -1, false);
        return new String[]{instrName, sb.toString()};
    }

    private void appendParamReferenceString(StringBuilder sb, List<String> params, Map<Integer, String> copySlotVariables, int slot, int imagSlot, boolean first) {
        if (slot < 0)
            return;
        if (!first)
            sb.append(", ");
        boolean imagValue = slot % 2 == 1;
        int complexSlot = slot / 2;
        String paramName = params.get(complexSlot);
//        sb.append(complexSlot).append("#");
        boolean usingBoth = imagSlot == slot + 1;
        if (!usingBoth) {
            if (!imagValue)
                sb.append("re(");
            else
                sb.append("im(");
        }

        String[] split = paramName.split("VAR - ", 2);
        if (split.length > 1)
            paramName = split[1];
        split = paramName.split("CON - ", 2);
        if (split.length > 1)
            paramName = split[1];
        split = paramName.split("CPY - ", 2);
        if (split.length > 1)
            paramName = split[1];
        split = paramName.split("ITE - ", 2);
        if (split.length > 1)
            paramName = split[1];

        sb.append(paramName);
        if (!usingBoth)
            sb.append(")");
    }

    @Override
    protected void readFields() {
        super.readFields();
        readWindowFields();
    }

    private void readWindowFields() {
        for (Map.Entry<String, TabTraversableTextField> e : windowExpressionFields.entrySet()) {
            if (e.getKey().equals(expressionsParam.getMainInputVar()) && e.getValue().isInputValid()) {
                text = e.getValue().getText();
            }
        }
        applyValueToViews(getSupplier().getGeneral());
    }

    public boolean submitIfValid(Map<String, TabTraversableTextField> fieldMap) {
        for (TabTraversableTextField field : fieldMap.values()){
            if (!field.isInputValid())
                return false;
        }

        for (Map.Entry<String, TabTraversableTextField> e : fieldMap.entrySet()){
            expressionsParam.putExpression(e.getKey(), e.getValue().getText());
        }
        boolean actualSubmitValue = submitValue;
        submitValue = true;
        submit();
        submitValue = actualSubmitValue;
        return true;
    }

    @Override
    public ParamSupplier getSupplier() {
        Map<String, String> exprs = new HashMap<>(expressionsParam.getExpressions());
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

