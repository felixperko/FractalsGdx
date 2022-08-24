package de.felixp.fractalsgdx.rendering;

import com.badlogic.gdx.graphics.glutils.ShaderProgram;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import de.felixp.fractalsgdx.rendering.orbittrap.AxisOrbittrap;
import de.felixp.fractalsgdx.rendering.orbittrap.CircleOrbittrap;
import de.felixp.fractalsgdx.rendering.orbittrap.Orbittrap;
import de.felixp.fractalsgdx.rendering.orbittrap.OrbittrapContainer;
import de.felixp.fractalsgdx.rendering.valuereference.ParamAttributeValueReference;
import de.felixp.fractalsgdx.rendering.valuereference.ValueReference;
import de.felixperko.expressions.ComputeExpressionDomain;
import de.felixperko.fractals.system.calculator.ComputeExpression;
import de.felixperko.fractals.system.calculator.ComputeInstruction;
import de.felixperko.fractals.system.numbers.ComplexNumber;
import de.felixperko.fractals.system.numbers.Number;
import de.felixperko.fractals.system.parameters.suppliers.CoordinateBasicShiftParamSupplier;
import de.felixperko.fractals.system.parameters.suppliers.CoordinateDiscreteParamSupplier;
import de.felixperko.fractals.system.parameters.suppliers.CoordinateModuloParamSupplier;
import de.felixperko.fractals.system.parameters.suppliers.ParamSupplier;
import de.felixperko.fractals.system.parameters.suppliers.StaticParamSupplier;
import de.felixperko.fractals.system.systems.infra.SystemContext;

public class ShaderBuilder {

    final static String MAKRO_VERSION = "//<VERSION>";
    final static String MAKRO_FIELDS = "//<FIELDS>";
    final static String MAKRO_INIT = "//<INIT>";
    final static String MAKRO_KERNEL = "//<ITERATE>";
    final static String MAKRO_CONDITION = "true//<CONDITION>";

    final static String ITERATIONS_VAR_NAME = "n";

    final static String PREFIX_BASE_ORBITTRAP = "ot";
    Map<Orbittrap, String> orbittrapPrefixes = new HashMap<>();
    int orbittrapCounter = 0;

    String[] localVariables;
    SystemContext systemContext;
    ComputeExpression firstExpression;
    ComputeExpressionDomain expressionDomain;

    Map<String, ValueReference> uniforms = new HashMap<>();

    int iterationsVarIndexReal = -1;

    int tempVarCounter = 0;

    String precision;

    protected int getParamFloatCount(){
        return isDoublePrecision(true) ? 6 : 3;
    }

    protected String getGlslType(){
        return isDoublePrecision(false) ? "double" : "float";
    }

    protected boolean isDoublePrecision(boolean includeEmulated){
        if (includeEmulated && isEmulatedDoublePrecision())
            return true;
        return ShaderSystemContext.TEXT_PRECISION_64.equals(precision);
    }

    protected boolean isEmulatedDoublePrecision(){
        return ShaderSystemContext.TEXT_PRECISION_64_EMULATED.equals(precision);
    }

    protected boolean isPerturbedPrecision(){
        return ShaderSystemContext.TEXT_PRECISION_64_REFERENCE.equals(precision);
    }

    protected void setPrecision(String precision){
        this.precision = precision;
    }

    public ShaderBuilder(ComputeExpressionDomain expressionDomain, SystemContext systemContext){
        this.expressionDomain = expressionDomain;
        this.firstExpression = expressionDomain.getMainExpressions().get(0);
        this.systemContext = systemContext;
    }

    protected void initLocalVariables() {
        List<ParamSupplier> params = firstExpression.getParameterList();
        List<Integer> copySlots = firstExpression.getCopySlots();

        boolean emulate64bit = isEmulatedDoublePrecision();
        int varsPerParam = emulate64bit ? 4 : 2;
        localVariables = new String[(params.size() + copySlots.size()) * varsPerParam];

        for (int i = 0; i < params.size(); i++) {
            localVariables[i * varsPerParam] = "local_" + i * varsPerParam;
            localVariables[i * varsPerParam + 1] = "local_" + (i * varsPerParam + 1);
            if (emulate64bit) {
                localVariables[i * varsPerParam + 2] = "local_" + (i * varsPerParam + 2);
                localVariables[i * varsPerParam + 3] = "local_" + (i * varsPerParam + 3);
            }
        }
        int i = params.size() * varsPerParam;
        for (Integer copySlotReal : copySlots) {
            localVariables[i] = "copy_" + i;
            localVariables[i + 1] = "copy_" + (i + 1);
            i += 2;
            if (emulate64bit) {
                localVariables[i + 2] = "copy_" + (i + 2);
                localVariables[i + 3] = "copy_" + (i + 3);
                i += 2;
            }
        }
    }

    public String processShadertemplateLine(String templateLine, float rendererHeight, boolean newtonFractal) {
        String line = templateLine;
        if (line.contains(MAKRO_VERSION)){
            line = line.replaceAll(MAKRO_VERSION, getVersionString());
        }
        if (line.contains(MAKRO_INIT)){
            line = line.replaceAll(MAKRO_INIT, getInitString());
        }
        if (line.contains(MAKRO_KERNEL)){
            line = line.replaceAll(MAKRO_KERNEL, getKernelString());
        }
        if (line.contains(MAKRO_CONDITION)){
            line = line.replaceAll(MAKRO_CONDITION, getConditionString());
        }
        if (line.contains(MAKRO_FIELDS)){ //fields might be added in previous stages -> insert fields last
            line = line.replaceAll(MAKRO_FIELDS, getFieldsString());
        }
        return line;
    }

    private String getVersionString() {
        if (isDoublePrecision(false))
            return "#version 430";
        return "#version 320 es";
    }

    public void setUniforms(ShaderProgram computeShader){
        for (Map.Entry<String, ValueReference> e : uniforms.entrySet()){
            computeShader.setUniformf(e.getKey(), getFloatValue(e));
        }
    }

    private String getFieldsString() {
        StringBuilder sb = new StringBuilder();
        int paramCount = firstExpression.getParameterList().size();
        writeStringBuilderLine(sb, null, "uniform float["+ paramCount*getParamFloatCount() +"] params;");
        for (Map.Entry<String, ValueReference> e : uniforms.entrySet()){
            float val = getFloatValue(e);
            writeStringBuilderLine(sb, null, "uniform float "+e.getKey()+";");
        }
        return sb.toString();
    }

    private float getFloatValue(Map.Entry<String, ValueReference> e) {
        Object val = e.getValue().getValue();
        if (val instanceof Number)
            return (float)((Number)val).toDouble();
        else if (val instanceof Double)
            return (float)(double)val;
        else if (val instanceof Float)
            return (float)val;
        else
            throw new IllegalStateException("Can't handle uniform type for "+e.getKey()+" :"+val.getClass().getName());
    }

    private String getInitString() {

        initLocalVariables();
        StringBuilder stringBuilder = new StringBuilder();

        if (isPerturbedPrecision()) {
//            stringBuilder.append("float refR = 0.0;\n");
//            stringBuilder.append("float refI = 0.0;\n");
            stringBuilder.append("//perturbed\n");
            stringBuilder.append("float refR = cRef.x;\n");
            stringBuilder.append("float refI = cRef.y;\n");
        }
//        else {
//            sb.append("float refR = 0.0;\n");
//            sb.append("float refI = 0.0;\n");
//        }


//        List<ComputeInstruction> instructions = firstExpression.getInstructions();
//        List<ParamSupplier> params = firstExpression.getParameterList();
//        for (String localVar : localVariables)
//            writeStringBuilderLine(stringBuilder, "float "+localVar+" = 0.0;");

        Map<String, ParamSupplier> paramSuppliers = firstExpression.getParamsByName();
//        Map<String, ParamSupplier> constantSuppliers = firstExpression.getConstants();

        int varCounter = 0;

        Map<String, String> placeholderValues = new HashMap<>();
        placeholderValues.put("type", getGlslType());

        int i = 0;
        for (String name : paramSuppliers.keySet()){
            ParamSupplier supp = paramSuppliers.get(name);

            int localVarIndexReal = varCounter++;
            int localVarIndexImag = varCounter++;
            int paramIndexReal = i*getParamFloatCount();
            int paramIndexReal2 = paramIndexReal+1;
            int paramIndexImag = paramIndexReal+2;
            int paramIndexImag2 = paramIndexReal+3;
            int paramIndexDelta = paramIndexReal+4;
            int paramIndexDelta2 = paramIndexReal+5;
            if (!isDoublePrecision(true)){
                paramIndexImag = paramIndexReal+1;
                paramIndexDelta = paramIndexReal+2;
            }
            String varNameReal = localVariables[localVarIndexReal];
            String varNameImag = localVariables[localVarIndexImag];

            int localVarIndexReal2 = -1;
            int localVarIndexImag2 = -1;
            String varNameReal2 = null;
            String varNameImag2 = null;
            if (isEmulatedDoublePrecision()){
                localVarIndexReal2 = localVarIndexReal+2;
                localVarIndexImag2 = localVarIndexImag+2;
                varCounter += 2;
                varNameReal2 = localVariables[localVarIndexReal2];
                varNameImag2 = localVariables[localVarIndexImag2];
            }

            if (supp instanceof StaticParamSupplier) {
                if (name.equals(ITERATIONS_VAR_NAME)) {
                    iterationsVarIndexReal = localVarIndexReal;
                }
                else {
                    if (!isEmulatedDoublePrecision()) {
                        writeStringBuilderLines(stringBuilder, placeholderValues, "//initRenderer static parameter " + name,
                                "type " + varNameReal + " = params[" + paramIndexReal + "] + params[" + paramIndexDelta + "] * deltaX;",
                                "type " + varNameImag + " = params[" + paramIndexImag + "] + params[" + paramIndexDelta + "] * deltaY;");
                    }
                    else {
//                        writeStringBuilderLines(stringBuilder, placeholderValues, "//initRenderer parameter " + supp.getName(),
//                                "type " + varNameReal + " = params[" + paramIndexReal + "] + params[" + paramIndexDelta + "] * deltaX;",
//                                "type " + varNameImag + " = params[" + paramIndexImag + "] + params[" + paramIndexDelta + "] * deltaY;",
//                                "type " + varNameReal2 + " = 0.0;",
//                                "type " + varNameImag2 + " = 0.0;");
                        writeStringBuilderLines(stringBuilder, placeholderValues, "//initRenderer static parameter " + name + " (double)",
                                "type " + varNameReal + " = params[" + paramIndexReal + "] + params[" + paramIndexDelta + "] * deltaX;",
                                "type " + varNameImag + " = params[" + paramIndexImag + "] + params[" + paramIndexDelta + "] * deltaY;",
                                "type " + varNameReal2 + " = params[" + paramIndexReal2 + "] + params[" + paramIndexDelta2 + "] * deltaX;",
                                "type " + varNameImag2 + " = params[" + paramIndexImag2 + "] + params[" + paramIndexDelta2 + "] * deltaY;");
                    }
                }
            }
            else if (supp instanceof CoordinateDiscreteParamSupplier){
                writeStringBuilderLines(stringBuilder, placeholderValues, "//initRenderer parameter " + name,
                        "type "+varNameReal+" = round((params["+paramIndexReal+"] +  params[" + paramIndexDelta + "] * deltaX  )* gridFrequency/ moduloFrequency2 )*moduloFrequency2/gridFrequency;",
                        "type "+varNameImag+" = round((params["+paramIndexImag+"] +  params[" + paramIndexDelta + "] * deltaY  )* gridFrequency/ moduloFrequency2 )*moduloFrequency2/gridFrequency;");
            }
            else if (supp instanceof CoordinateBasicShiftParamSupplier){
                if (!isDoublePrecision(true)) {
                    writeStringBuilderLines(stringBuilder, placeholderValues, "//initRenderer map parameter " + name,
                            "type "+varNameReal+" = params["+paramIndexReal+"] + params["+paramIndexDelta+"] * deltaX;",
                            "type "+varNameImag+" = params["+paramIndexImag+"] + params["+paramIndexDelta+"] * deltaY;");
                }
                else if (isEmulatedDoublePrecision()){

//                    writeStringBuilderLines(stringBuilder, placeholderValues, "//initRenderer parameter " + supp.getName(),
//                            "type "+varNameReal+" = params["+paramIndexReal+"] + params["+paramIndexDelta+"] * deltaX;",
//                            "type "+varNameImag+" = params["+paramIndexImag+"] + params["+paramIndexDelta+"] * deltaY;",
//                            "type " + varNameReal2 + " = 0.0;",
//                            "type " + varNameImag2 + " = 0.0;");

                    //splitF(hi,lo,val)
                    //addFloatFloat(ah,al,bh,bl);
                    //multFloatFloat(ah,al,bh,bl);
                    writeStringBuilderLines(stringBuilder, placeholderValues, "//initRenderer map parameter " + name + " (float-float)",
                            "type offsetRealh = params["+paramIndexDelta+"];",
                            "type offsetImagh = params["+paramIndexDelta+"];",
                            "type offsetReall = params["+paramIndexDelta2+"];",
                            "type offsetImagl = params["+paramIndexDelta2+"];",
                            "multFloatFloat(offsetRealh, offsetReall, deltaXh, deltaXl);",
                            "multFloatFloat(offsetImagh, offsetImagl, deltaYh, deltaYl);",
                            "type "+varNameReal+" = params["+paramIndexReal+"];",
                            "type "+varNameImag+" = params["+paramIndexImag+"];",
                            "type "+varNameReal2+" = params["+paramIndexReal2+"];",
                            "type "+varNameImag2+" = params["+paramIndexImag2+"];",
                            "addFloatFloat("+varNameReal+","+varNameReal2+",offsetRealh,offsetReall);",
                            "addFloatFloat("+varNameImag+","+varNameImag2+",offsetImagh,offsetImagl);");
                }
                else {
                    writeStringBuilderLines(stringBuilder, placeholderValues, "//initRenderer map parameter " + name + " (double)",
                            "type " + varNameReal + " = double(params[" + paramIndexReal + "]) + double(params[" + paramIndexReal2 + "]) + double(params[" + paramIndexDelta + "] * deltaX);",
                            "type " + varNameImag + " = double(params[" + paramIndexImag + "]) + double(params[" + paramIndexImag2 + "]) + double(params[" + paramIndexDelta + "] * deltaY);");
//                    writeStringBuilderLines(stringBuilder, placeholderValues, "//initRenderer parameter " + supp.getName(),
//                            "type " + varNameReal + " = double(params[" + paramIndexReal + "]) + double(params[" + paramIndexReal2 + "]) + ((double(params[" + paramIndexDelta + "])+double(params[" + paramIndexDelta2 + "])) * deltaX);",
//                            "type " + varNameImag + " = double(params[" + paramIndexImag + "]) + double(params[" + paramIndexImag2 + "]) + ((double(params[" + paramIndexDelta + "])+double(params[" + paramIndexDelta2 + "])) * deltaY);");
                }
            }
            else if (supp instanceof CoordinateModuloParamSupplier){
//                writeStringBuilderLines(stringBuilder, placeholderValues, "//initRenderer parameter " + supp.getName(),
//                        "type "+varNameReal+" = mod(deltaX, 4.0) + 2.0;",
//                        "type "+varNameImag+" = mod(deltaY, 4.0) + 2.0;");
                //Modulo
                writeStringBuilderLines(stringBuilder, placeholderValues, "//initRenderer parameter " + name,
                        "type "+varNameReal+" = mod((params["+paramIndexReal+"] + params["+paramIndexDelta+"] * deltaX)*gridFrequency + moduloFrequency, moduloFrequency2) - moduloFrequency;",
                        "type "+varNameImag+" = mod((params["+paramIndexImag+"] + params["+paramIndexDelta+"] * deltaY)*gridFrequency + moduloFrequency, moduloFrequency2) - moduloFrequency;");
            }
            else
                throw new IllegalArgumentException("Unsupported ParamSupplier "+name+": "+supp.getClass().getName());
            i++;
        }

        for (Integer copySlot : firstExpression.getCopySlots()){

            writeStringBuilderLines(stringBuilder, placeholderValues, "//initRenderer copy slots "+copySlot+", "+(copySlot+1),
                    "type "+localVariables[copySlot]  +" = 0.0;",
                    "type "+localVariables[copySlot+1]+" = 0.0;");
        }

//        if (isPerturbedPrecision()){
//            stringBuilder.append("//perturbed -> c = delta\n");
////            stringBuilder.append("local_4 -= DecodeFloatSignedV3(texelFetch(referenceTexture, ivec2(1, 0), 0).rgb);\n");
////            stringBuilder.append("local_5 -= DecodeFloatSignedV3(texelFetch(referenceTexture, ivec2(1, 1), 0).rgb);\n");
//            stringBuilder.append("local_4 -= cRef.x;\n");
//            stringBuilder.append("local_5 -= cRef.y;\n");
//        }

        System.out.println("Shader initVariables(): ");
        System.out.println(stringBuilder.toString());
        return stringBuilder.toString();
    }

    private String getConditionString(){
        String cond = (String)systemContext.getParamValue(ShaderSystemContext.PARAM_CONDITION);
        switch (cond){
            case ShaderSystemContext.TEXT_COND_ABS:
                return "local_0*local_0 + local_1*local_1 > limitSq";
            case ShaderSystemContext.TEXT_COND_ABS_R:
                return "abs(local_0) > limit";
            case ShaderSystemContext.TEXT_COND_ABS_I:
                return "abs(local_1) > limit";
            case ShaderSystemContext.TEXT_COND_ABS_MULT_RI:
                return "abs(local_0*local_1) > limit";
            case ShaderSystemContext.TEXT_COND_MULT_RI:
                return "local_0*local_1 > limit";
            default:
                return "";
        }
    }

    private String getOrbittrapPrefix(Orbittrap orbittrap){
        if (orbittrapPrefixes.containsKey(orbittrap))
            return orbittrapPrefixes.get(orbittrap);
        else {
            String prefix = PREFIX_BASE_ORBITTRAP+orbittrapCounter+"_";
            orbittrapCounter++;
            orbittrapPrefixes.put(orbittrap, prefix);
            return prefix;
        }
    }

    private String getKernelString() {

        StringBuilder sb = new StringBuilder();

        if (isPerturbedPrecision()) {
            //right now specific for mandelbrot
            //f(x)_(n+1) = f(x)_n^2 + c
            //delta(x,y)_0 = f(x)_0 - f(y)_0
            //delta(x,y)_(n+1) = delta(y)_n^2 + delta(y)_0  +  2*f(x)_n * delta(x,y)_n
            //http://www.science.eclipse.co.uk/sft_maths.pdf
            sb.append("//perturbed\n");
            sb.append("refR = 2.0*DecodeFloatSignedV3(texelFetch(referenceTexture, ivec2(int(mod(i, REF_TEX_WIDTH)), int(i / REF_TEX_WIDTH)*2  ), 0).rgb);\n");
            sb.append("refI = 2.0*DecodeFloatSignedV3(texelFetch(referenceTexture, ivec2(int(mod(i, REF_TEX_WIDTH)), int(i / REF_TEX_WIDTH)*2+1), 0).rgb);\n");
            sb.append("float refTemp = refR*local_0 - refI*local_1;\n");
            sb.append("refI = (refR*local_1) + (refI*local_0);\n");
            sb.append("refR = refTemp;\n");
            sb.append("\n");
        }

        if (iterationsVarIndexReal >= 0){
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("varReal", localVariables[iterationsVarIndexReal]);
            placeholders.put("varImag", localVariables[iterationsVarIndexReal+1]);
            writeStringBuilderLines(sb, placeholders,"//iteration variable",
                    "float varReal = i;",
                    "float varImag = 0.0;");
        }

        //write iteration instructions
        ComputeExpression expr = expressionDomain.getMainExpressions().get(0);
        List<ComputeInstruction> instructions = expr.getInstructions();
        for (ComputeInstruction instruction : instructions) {
            printKernelInstruction(sb, instruction);
        }

        writeOrbitTrapConditions(sb);

        if (isPerturbedPrecision()) {
            sb.append("//perturbed\n");
            sb.append("local_0 += refR;\n");
            sb.append("local_1 += refI;\n");
        }

        System.out.println("Shader iterate(): ");
        System.out.println(sb.toString());
        return sb.toString();
    }

    private void writeOrbitTrapConditions(StringBuilder sb) {
        ParamSupplier orbittrapSupp = systemContext.getParamContainer().getParam(ShaderSystemContext.PARAM_ORBITTRAPS);
        if (orbittrapSupp != null && orbittrapSupp instanceof StaticParamSupplier){
            OrbittrapContainer trapContainer = orbittrapSupp.getGeneral(OrbittrapContainer.class);
            List<Orbittrap> traps = trapContainer.getOrbittraps();
            if (!traps.isEmpty()){
                writeStringBuilderLines(sb, null, "",
                        "if(");
                Iterator<Orbittrap> it = traps.iterator();
                while (it.hasNext()) {
                    Orbittrap trap = it.next();
                    HashMap<String, String> placeholderValues = new HashMap<>();

                    if (trap instanceof AxisOrbittrap) {
                        String otPrefix = getOrbittrapPrefix(trap);
                        uniforms.put(otPrefix+"width", new ParamAttributeValueReference(trap.getParamAttribute("width")));
                        uniforms.put(otPrefix+"offset", new ParamAttributeValueReference(trap.getParamAttribute("offset")));
                        uniforms.put(otPrefix+"factorR", new ParamAttributeValueReference(trap.getParamAttribute("angle")){
                            @Override
                            public Object getValue() {
                                double angle = ((Number)super.getValue()).toDouble();
                                return Math.cos(Math.toRadians(angle));
                            }
                        });
                        uniforms.put(otPrefix+"factorI", new ParamAttributeValueReference(trap.getParamAttribute("angle")){
                            @Override
                            public Object getValue() {
                                double angle = ((Number)super.getValue()).toDouble();
                                return Math.sin(Math.toRadians(angle));
                            }
                        });
                        placeholderValues.put("width", otPrefix+"width");
                        placeholderValues.put("offset", otPrefix+"offset");
                        placeholderValues.put("factorR", otPrefix+"factorR");
                        placeholderValues.put("factorI", otPrefix+"factorI");
                            writeStringBuilderLines(sb, placeholderValues, "//axis orbittrap",
                                    "abs(local_1*factorR-local_0*factorI - offset) <= float(width)");
//                        }
                    }
                    else if (trap instanceof CircleOrbittrap){
                        String otPrefix = getOrbittrapPrefix(trap);
                        uniforms.put(otPrefix+"centerR", new ParamAttributeValueReference(trap.getParamAttribute("center")){
                            @Override
                            public Object getValue() {
                                return ((ComplexNumber)super.getValue()).realDouble();
                            }
                        });
                        uniforms.put(otPrefix+"centerI", new ParamAttributeValueReference(trap.getParamAttribute("center")){
                            @Override
                            public Object getValue() {
                                return ((ComplexNumber)super.getValue()).imagDouble();
                            }
                        });
                        uniforms.put(otPrefix+"radius", new ParamAttributeValueReference(trap.getParamAttribute("radius")));
                        placeholderValues.put("centerR", otPrefix+"centerR");
                        placeholderValues.put("centerI", otPrefix+"centerI");
                        placeholderValues.put("radius", otPrefix+"radius");
                        writeStringBuilderLines(sb, placeholderValues, "//circle orbittrap",
                                "sqrt((local_0-centerR)*(local_0-centerR)+(local_1-centerI)*(local_1-centerI)) <= radius");
                    }

                    if (it.hasNext()){
                        writeStringBuilderLine(sb, null, "||");
                    }
                }
                writeStringBuilderLines(sb, null,
                        "){",
                        "   loopIterations = float(i + 1.0);",
                        "   break;",
                        "}");
            }
        }
    }

    private String getTempVarName(){
        return "temp_"+ tempVarCounter++;
    }

    private void printKernelInstruction(StringBuilder stringBuilder, ComputeInstruction instruction) {
//	      if (burningship > 0.0){
//	        resX = abs(resX);
//	        resY = abs(resY);
//        }
        Map<String, String> placeholderValues = new HashMap<>();
        placeholderValues.put("float", getGlslType());
//        switch (instruction.type){
            //TODO variable mapping
            if (instruction.type == ComputeInstruction.INSTR_ADD_COMPLEX){
                if (!isEmulatedDoublePrecision()) {
                    writeinstrunctionlines(stringBuilder, instruction, placeholderValues, "//add_complex",
                            "fromReal = fromReal + toReal;",
                            "fromImag = fromImag + toImag;");
                }
                else {
                    writeinstrunctionlines(stringBuilder, instruction, placeholderValues, "//add_complex (float-float)",
//                            "fromReal = fromReal +  toReal;",
//                            "fromImag = fromImag + toImag;");
                            "addFloatFloat(fromReal, fromReal2, toReal, toReal2);",
                            "addFloatFloat(fromImag, fromImag2, toImag, toImag2);");

                    //addFloatFloat(ah,al,bh,bl);
                }
            } else if (instruction.type == ComputeInstruction.INSTR_ADD_PART){
                writeinstrunctionlines(stringBuilder, instruction, placeholderValues, "//add_part",
                        "fromReal = fromReal + fromImag;");
            } else if (instruction.type == ComputeInstruction.INSTR_SUB_COMPLEX){
                writeinstrunctionlines(stringBuilder, instruction, placeholderValues, "//sub_complex",
                        "fromReal = fromReal - toReal;",
                        "fromImag = fromImag - toImag;");
            } else if (instruction.type == ComputeInstruction.INSTR_SUB_PART){
                writeinstrunctionlines(stringBuilder, instruction, placeholderValues, "//sub_part",
                        "fromReal = fromReal - fromImag;");
            } else if (instruction.type == ComputeInstruction.INSTR_MULT_COMPLEX){
                String tempVarName = getTempVarName();
                //real = ac-bd
                //imag = ad+bc
                writeinstrunctionlines(stringBuilder, instruction, placeholderValues, "//mult_complex",
                        "float "+tempVarName+" = fromReal*toReal - fromImag*toImag;",
                        "fromImag = (fromReal*toImag) + (fromImag*toReal);",
                        "fromReal = "+tempVarName+";");
//                        "float "+tempVarName+" = fromReal*toReal - fromImag*toImag;",
//                        "fromImag = (fromReal + fromImag)*(toReal + toImag) - "+tempVarName+";",
//                        "fromReal = "+tempVarName+";");
            } else if (instruction.type == ComputeInstruction.INSTR_MULT_PART){
                writeinstrunctionlines(stringBuilder, instruction, placeholderValues, "//mult_part",
                        "fromReal = fromReal*fromImag;");
            } else if (instruction.type == ComputeInstruction.INSTR_DIV_COMPLEX){
                placeholderValues.put("temp_div", getTempVarName());
                placeholderValues.put("tempR", getTempVarName());
                placeholderValues.put("tempI", getTempVarName());
                writeinstrunctionlines(stringBuilder, instruction, placeholderValues, "//div_complex",
                        "float temp_div = toReal*toReal + toImag*toImag;",
                        "float tempR = fromReal*toReal + fromImag*toImag;",
                        "float tempI = fromImag*toReal - fromReal*toImag;",
                        "if (temp_div != 0.0){",
                        "   fromReal = tempR/temp_div;",
                        "   fromImag = tempI/temp_div;",
                        "}"
                        );
            } else if (instruction.type == ComputeInstruction.INSTR_DIV_PART){
                writeinstrunctionlines(stringBuilder, instruction, placeholderValues, "//div_part",
                        "fromReal = fromReal / toReal;");
            } else if (instruction.type == ComputeInstruction.INSTR_POW_COMPLEX){
                String temp1 = getTempVarName();
                String temp2 = getTempVarName();
                String temp3 = getTempVarName();
                writeinstrunctionlines(stringBuilder, instruction, placeholderValues, "//pow_complex",
                        "float "+temp3+" = sqrt(fromReal*fromReal + fromImag*fromImag);",
                        "float "+temp2+" = 0.0;",
                        "float "+temp1+" = 0.0;",
                        "if ("+temp3+" != 0.0) {",
                        "    "+temp3+" = log("+temp3+");",
                        "    "+temp1+" = atan(fromImag, fromReal);",
                        "    "+temp2+" = ("+temp3+"*toReal - "+temp1+"*toImag);",
                        "    "+temp1+" = ("+temp3+"*toImag + "+temp1+"*toReal);",
                        "    "+temp3+" = "+temp2+";",
                        "    "+temp2+" = exp("+temp3+");",
                        "    fromReal = "+temp2+" * cos("+temp1+");",
                        "    fromImag = "+temp2+" * sin("+temp1+");",
//                        "float "+temp1+" = fromReal*fromReal - fromImag*fromImag;",
//                        "fromImag = fromReal*fromImag*2.0;",
//                        "fromReal = "+temp1+";");
                        "}"
//                        "else {",
//                        "   fromReal = 0.0;",
//                        "   fromImag = 0.0;",
//                        "}"
                );
            } else if (instruction.type == ComputeInstruction.INSTR_POW_PART){
                writeinstrunctionlines(stringBuilder, instruction, placeholderValues, "//pow_part",
                        "fromReal = fromReal^fromImag;");
            } else if (instruction.type == ComputeInstruction.INSTR_COPY_COMPLEX){
                writeinstrunctionlines(stringBuilder, instruction, placeholderValues, "//copy_complex",
                        "toReal = fromReal;",
                        "toImag = fromImag;");
            } else if (instruction.type == ComputeInstruction.INSTR_COPY_PART){
                writeinstrunctionlines(stringBuilder, instruction, placeholderValues, "//copy_part",
                        "toReal = fromReal;");
            } else if (instruction.type == ComputeInstruction.INSTR_ABS_COMPLEX){
                writeinstrunctionlines(stringBuilder, instruction, placeholderValues, "//abs_complex",
                        "fromReal = abs(fromReal);",
                        "fromImag = abs(fromImag);");
            } else if (instruction.type == ComputeInstruction.INSTR_ABS_PART){
                writeinstrunctionlines(stringBuilder, instruction, placeholderValues, "//abs_part",
                        "fromReal = abs(fromReal);");
            } else if (instruction.type == ComputeInstruction.INSTR_SIN_COMPLEX){
                String tempVarName7 = getTempVarName();
                writeinstrunctionlines(stringBuilder, instruction, placeholderValues, "//sin_complex",
                        "float "+tempVarName7+" = sin(fromReal) * cosh(fromImag);",
                        "fromImag = cos(fromReal) * sinh(fromImag);",
                        "fromReal = "+tempVarName7+";");
            } else if (instruction.type == ComputeInstruction.INSTR_SIN_PART){
                writeinstrunctionlines(stringBuilder, instruction, placeholderValues, "//sin_part",
                        "fromReal = sin(fromReal);");
            } else if (instruction.type == ComputeInstruction.INSTR_COS_COMPLEX){
                String tempVarName8 = getTempVarName();
                writeinstrunctionlines(stringBuilder, instruction, placeholderValues, "//cos_complex",
                        "float "+tempVarName8+" = cos(fromReal) * cosh(fromImag);",
                        "fromImag = -sin(fromReal) * sinh(fromImag);",
                        "fromReal = "+tempVarName8+";");
            } else if (instruction.type == ComputeInstruction.INSTR_COS_PART){
                writeinstrunctionlines(stringBuilder, instruction, placeholderValues, "//cos_part",
                        "fromReal = cos(fromReal);");
            } else if (instruction.type == ComputeInstruction.INSTR_TAN_COMPLEX){
                String tempVarName6 = getTempVarName();
                writeinstrunctionlines(stringBuilder, instruction, placeholderValues, "//tan_complex",
                        "float "+tempVarName6+" = cos(fromReal+fromReal)+cosh(fromImag+fromImag);",
                        "fromReal = sin(fromReal+fromReal)/"+tempVarName6+";",
                        "fromImag = sinh(fromImag+fromImag)/"+tempVarName6+";");
            } else if (instruction.type == ComputeInstruction.INSTR_TAN_PART){
                writeinstrunctionlines(stringBuilder, instruction, placeholderValues, "//tan_part",
                        "fromReal = tan(fromReal);");
            } else if (instruction.type == ComputeInstruction.INSTR_SINH_COMPLEX){
                String tempVarName4 = getTempVarName();
                writeinstrunctionlines(stringBuilder, instruction, placeholderValues, "//sinh_complex",
                        "float "+tempVarName4+" = sinh(fromReal) * cos(fromImag);",
                        "fromImag = cosh(fromReal)*sin(fromImag);",
                        "fromReal = "+tempVarName4+";");
            } else if (instruction.type == ComputeInstruction.INSTR_SINH_PART){
                writeinstrunctionlines(stringBuilder, instruction, placeholderValues, "//sinh_part",
                        "fromReal = sinh(fromReal);");
            } else if (instruction.type == ComputeInstruction.INSTR_COSH_COMPLEX){
                String tempVarName5 = getTempVarName();
                writeinstrunctionlines(stringBuilder, instruction, placeholderValues, "//cosh_complex",
                        "float "+tempVarName5+" = cosh(fromReal) * cos(fromImag);",
                        "fromImag = sinh(fromReal)*sin(fromImag);",
                        "fromReal = "+tempVarName5+";");
            } else if (instruction.type == ComputeInstruction.INSTR_COSH_PART){
                writeinstrunctionlines(stringBuilder, instruction, placeholderValues, "//cosh_part",
                        "fromReal = cosh(fromReal);");
            } else if (instruction.type == ComputeInstruction.INSTR_TANH_COMPLEX){
                String tempVarName3 = getTempVarName();
//                writeinstrunctionlines(stringBuilder, instruction, placeholderValues, "//tanh_complex",
//                        "float "+tempVarName3+" = sinh(2.0*fromReal)/(cosh(2.0*fromReal)+cos(2.0*fromImag));",
//                        "fromImag = sin(2.0*fromImag)/(cosh(2.0*fromReal)+cos(2.0*fromImag));",
//                        "fromReal = "+tempVarName3+";");
                writeinstrunctionlines(stringBuilder, instruction, placeholderValues, "//tanh_complex",
                        "fromReal = fromReal+fromReal;",
                        "fromImag = fromImag+fromImag;",
                        "float "+tempVarName3+" = cosh(fromReal)+cos(fromImag);",
                        "fromImag = sin(fromImag)/+"+tempVarName3+";",
                        tempVarName3+" = sinh(fromReal)/"+tempVarName3+";",
                        "fromReal = "+tempVarName3+";");
            } else if (instruction.type == ComputeInstruction.INSTR_TANH_PART){
                writeinstrunctionlines(stringBuilder, instruction, placeholderValues, "//tanh_part",
                        "fromReal = tanh(fromReal);");
            } else if (instruction.type == ComputeInstruction.INSTR_SQUARE_COMPLEX){
                if (!isEmulatedDoublePrecision()) {
                    String tempVarName2 = getTempVarName();
    //                writeinstrunctionlines(stringBuilder, instruction, placeholderValues, "//square_complex",
    //                        "float "+tempVarName2+" = fromReal*fromReal - fromImag*fromImag;",
    //                        "fromImag = fromReal*fromImag*2.0;",
    //                        "fromReal = "+tempVarName2+";");
                    writeinstrunctionlines(stringBuilder, instruction, placeholderValues, "//square_complex",
                            "float " + tempVarName2 + " = fromReal*fromImag;",
                            "fromReal = fromReal*fromReal - fromImag*fromImag;",
                            "fromImag = " + tempVarName2 + "+" + tempVarName2 + ";");
                } else {

//                    String tempVarName2 = getTempVarName();
//                    writeinstrunctionlines(stringBuilder, instruction, placeholderValues, "//square_complex",
//                            "float " + tempVarName2 + " = fromReal*fromImag;",
//                            "fromReal = fromReal*fromReal - fromImag*fromImag;",
//                            "fromImag = " + tempVarName2 + "+" + tempVarName2 + ";");

                    String tempVarName = getTempVarName();
                    String tempVarName2 = getTempVarName();
                    writeinstrunctionlines(stringBuilder, instruction, placeholderValues, "//square_complex (float-float)",
                            "float "+tempVarName+" = fromReal;",
                            "float "+tempVarName2+" = fromReal2;",
                            "multFloatFloat("+tempVarName+", "+tempVarName2+", fromImag, fromImag2);",
                            "multFloatFloat(fromReal, fromReal2, fromReal, fromReal2);",
                            "multFloatFloat(fromImag, fromImag2, fromImag, fromImag2);",
                            "fromImag = -fromImag;",
                            "fromImag2 = -fromImag2;",
                            "addFloatFloat(fromReal, fromReal2, fromImag, fromImag2);",
                            "fromImag = "+tempVarName+";",
                            "fromImag2 = "+tempVarName2+";",
                            "addFloatFloat(fromImag, fromImag2, "+tempVarName+", "+tempVarName2+");");
                }
            } else if (instruction.type == ComputeInstruction.INSTR_SQUARE_PART){
                writeinstrunctionlines(stringBuilder, instruction, placeholderValues, "//square_part",
                        "fromReal = fromReal*fromReal;");
            } else if (instruction.type == ComputeInstruction.INSTR_NEGATE_COMPLEX){
                writeinstrunctionlines(stringBuilder, instruction, placeholderValues, "//negate_complex",
                        "fromReal = -fromReal;",
                        "fromImag = -fromImag;");
            } else if (instruction.type == ComputeInstruction.INSTR_NEGATE_PART){
                writeinstrunctionlines(stringBuilder, instruction, placeholderValues, "//negate_part",
                        "fromReal = -fromReal;");
            } else if (instruction.type == ComputeInstruction.INSTR_RECIPROCAL_COMPLEX) {
                String tempVarName9 = getTempVarName();
//                writeinstrunctionlines(stringBuilder, instruction, placeholderValues, "//reciprocal_complex",
//                        "float " + tempVarName9 + " = fromReal*fromReal + fromImag*fromImag;",
//                        "if (" + tempVarName9 + " != 0.0){",
//                        "   fromReal =   fromReal / " + tempVarName9 + ";",
//                        "   fromImag = - fromImag / " + tempVarName9 + ";",
//                        "}"
//                );
                writeinstrunctionlines(stringBuilder, instruction, placeholderValues, "//reciprocal_complex",
                        "float " + tempVarName9 + " = fromReal*fromReal + fromImag*fromImag;",
                        "if (" + tempVarName9 + " != 0.0){",
                        "   "+tempVarName9+" = 1.0/"+tempVarName9+";",
                        "   fromReal =   fromReal * " + tempVarName9 + ";",
                        "   fromImag = - fromImag * " + tempVarName9 + ";",
                        "}"
                );
            } else if (instruction.type == ComputeInstruction.INSTR_LOG_PART) {
                writeinstrunctionlines(stringBuilder, instruction, placeholderValues, "//log_part",
                        "fromReal = log(fromReal);");
            } else if (instruction.type == ComputeInstruction.INSTR_LOG_COMPLEX) {
                String tempVarName10 = getTempVarName();
                writeinstrunctionlines(stringBuilder, instruction, placeholderValues, "//log_complex",
                        "float "+tempVarName10+" = atan(fromImag, fromReal);",
                        "fromReal = fromReal*fromReal;",
                        "fromImag = fromImag*fromImag;",
                        "fromReal = log(sqrt(fromReal+fromImag));",
                        "fromImag = "+tempVarName10+";");
            }
//        }
    }


    private void writeinstrunctionlines(StringBuilder stringBuilder, ComputeInstruction instruction, Map<String, String> placeholderValues, String... lines){
        String[] newLines = new String[lines.length];
        boolean fromReal = instruction.fromReal >= 0;
        boolean fromImag = instruction.fromImag >= 0;
        boolean toReal = instruction.toReal >= 0;
        boolean toImag = instruction.toImag >= 0;
        int i = 0;
        boolean emulate64bit = isEmulatedDoublePrecision();
        for (String line : lines){
            if (fromReal) {
                if (emulate64bit && localVariables.length > instruction.fromReal+2) {
                    line = line.replaceAll("fromReal2", localVariables[instruction.fromReal*2 + 2]);
                    line = line.replaceAll("fromReal", localVariables[instruction.fromReal*2]);
                } else {
                    line = line.replaceAll("fromReal", localVariables[instruction.fromReal]);
                }
            }
            if (fromImag) {
                if (emulate64bit && localVariables.length > instruction.fromReal+2) {
                    line = line.replaceAll("fromImag2", localVariables[instruction.fromImag*2 + 1]);
                    line = line.replaceAll("fromImag", localVariables[instruction.fromImag*2 - 1]);
                } else {
                    line = line.replaceAll("fromImag", localVariables[instruction.fromImag]);
                }
//                if (emulate64bit && localVariables.length > instruction.fromImag+2)
//                    line = line.replaceAll("fromImag2", localVariables[instruction.fromImag+2]);
//                line = line.replaceAll("fromImag", localVariables[instruction.fromImag]);
            }
            if (toReal) {
                if (emulate64bit && localVariables.length > instruction.fromReal+2) {
                    line = line.replaceAll("toReal2", localVariables[instruction.toReal*2 + 2]);
                    line = line.replaceAll("toReal", localVariables[instruction.toReal*2]);
                } else {
                    line = line.replaceAll("toReal", localVariables[instruction.toReal]);
                }
//                if (emulate64bit && localVariables.length > instruction.toReal+2)
//                    line = line.replaceAll("toReal2", localVariables[instruction.toReal+2]);
//                line = line.replaceAll("toReal", localVariables[instruction.toReal]);
            }
            if (toImag) {
                if (emulate64bit && localVariables.length > instruction.fromReal+2) {
                    line = line.replaceAll("toImag2", localVariables[instruction.toImag*2 + 1]);
                    line = line.replaceAll("toImag", localVariables[instruction.toImag*2 - 1]);
                } else {
                    line = line.replaceAll("toImag", localVariables[instruction.toImag]);
                }
//                if (emulate64bit && localVariables.length > instruction.toImag+2)
//                    line = line.replaceAll("toImag2", localVariables[instruction.toImag+2]);
//                line = line.replaceAll("toImag", localVariables[instruction.toImag]);
            }
            newLines[i++] = line;
        }
        writeStringBuilderLines(stringBuilder, placeholderValues, newLines);
    }


    private void writeStringBuilderLines(StringBuilder kernelStringBuilder, Map<String, String> placeholderValues, String... lines){
        for (String line : lines)
            writeStringBuilderLine(kernelStringBuilder, placeholderValues, line);
    }

    private void writeStringBuilderLine(StringBuilder stringBuilder, Map<String, String> placeholderValues, String text){
        if (placeholderValues != null) {
            for (Map.Entry<String, String> e : placeholderValues.entrySet())
                text = text.replaceAll(e.getKey(), e.getValue());
        }
        stringBuilder.append(text).append(System.getProperty("line.separator"));
    }
}
