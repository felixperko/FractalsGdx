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
import de.felixperko.fractals.system.parameters.suppliers.CoordinateDiscreteModuloParamSupplier;
import de.felixperko.fractals.system.parameters.suppliers.CoordinateModuloParamSupplier;
import de.felixperko.fractals.system.parameters.suppliers.ParamSupplier;
import de.felixperko.fractals.system.parameters.suppliers.StaticParamSupplier;
import de.felixperko.fractals.system.systems.infra.SystemContext;

public class ShaderBuilder {

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

    String glslType = "float";

    int iterationsVarIndexReal = -1;

    public ShaderBuilder(ComputeExpressionDomain expressionDomain, SystemContext systemContext){
        this.expressionDomain = expressionDomain;
        this.firstExpression = expressionDomain.getMainExpressions().get(0);
        this.systemContext = systemContext;
        List<ParamSupplier> params = firstExpression.getParameterList();
        List<Integer> copySlots = firstExpression.getCopySlots();

        localVariables = new String[(params.size()+copySlots.size()) * 2];

        for (int i = 0; i < params.size(); i++) {
            localVariables[i * 2] = "local_" + i * 2;
            localVariables[i * 2 + 1] = "local_" + (i * 2 + 1);
        }
        int i = params.size()*2;
        for (Integer copySlotReal : copySlots){
            localVariables[i] = "copy_"+i;
            localVariables[i+1] = "copy_"+(i+1);
            i += 2;
        }
    }

    public String processShadertemplateLine(String templateLine, float rendererHeight) {
        String line = templateLine;
        if (line.contains(MAKRO_INIT)){
            line = line.replaceAll(MAKRO_INIT, getInitString());
        }
        if (line.contains(MAKRO_KERNEL)){
            line = line.replaceAll(MAKRO_KERNEL, getKernelString());
        }
        if (line.contains(MAKRO_CONDITION)){
            line = line.replaceAll(MAKRO_CONDITION, getConditionString());
        }
        if (line.contains(MAKRO_FIELDS)){
            line = line.replaceAll(MAKRO_FIELDS, getFieldsString());
        }
        return line;
    }

    public void setUniforms(ShaderProgram computeShader){
        for (Map.Entry<String, ValueReference> e : uniforms.entrySet()){
            computeShader.setUniformf(e.getKey(), getFloatValue(e));
        }
    }

    private String getFieldsString() {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, ValueReference> e : uniforms.entrySet()){
            float val = getFloatValue(e);
            writeStringBuilderLine(sb, null, "uniform float "+e.getKey()+" = "+val+";");
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

    private String getConditionString(){
        String cond = (String)systemContext.getParamValue("condition");
        switch (cond){
            case GPUSystemContext.TEXT_COND_ABS:
                return "local_0*local_0 + local_1*local_1 > limitSq";
            case GPUSystemContext.TEXT_COND_ABS_R:
                return "abs(local_0) > limit";
            case GPUSystemContext.TEXT_COND_ABS_I:
                return "abs(local_1) > limit";
            case GPUSystemContext.TEXT_COND_ABS_MULT_RI:
                return "abs(local_0*local_1) > limit";
            case GPUSystemContext.TEXT_COND_MULT_RI:
                return "local_0*local_1 > limit";
            default:
                return "";
        }
    }

    private String getInitString() {
        StringBuilder stringBuilder = new StringBuilder();
//        List<ComputeInstruction> instructions = firstExpression.getInstructions();
//        List<ParamSupplier> params = firstExpression.getParameterList();
//        for (String localVar : localVariables)
//            writeStringBuilderLine(stringBuilder, "float "+localVar+" = 0.0;");

        List<ParamSupplier> paramSuppliers = firstExpression.getParameterList();
//        Map<String, ParamSupplier> constantSuppliers = firstExpression.getConstants();

        int varCounter = 0;
        double zoom = ((Number)systemContext.getParamValue("zoom")).toDouble();

        Map<String, String> placeholderValues = new HashMap<>();
        placeholderValues.put("type", glslType);

        for (ParamSupplier supp : paramSuppliers){
            int localVarIndexReal = varCounter++;
            int localVarIndexImag = varCounter++;
            int paramOffset = localVarIndexReal/2;
            int paramIndexReal = (localVarIndexReal+paramOffset);
            int paramIndexImag = paramIndexReal+1;
            int paramIndexDelta = paramIndexReal+2;
            String varNameReal = localVariables[localVarIndexReal];
            String varNameImag = localVariables[localVarIndexImag];

            if (supp instanceof StaticParamSupplier) {
                if (supp.getName().equals(ITERATIONS_VAR_NAME)) {
                    iterationsVarIndexReal = localVarIndexReal;
                }
                else {
                    ComplexNumber val = (ComplexNumber) ((StaticParamSupplier) supp).getObj();
                    writeStringBuilderLines(stringBuilder, placeholderValues, "//initRenderer parameter " + supp.getName(),
                            "type " + varNameReal + " = params[" + paramIndexReal + "] + params[" + paramIndexDelta + "] * deltaX;",
                            "type " + varNameImag + " = params[" + paramIndexImag + "] + params[" + paramIndexDelta + "] * deltaY;");
                }
//                writeStringBuilderLines(stringBuilder, placeholderValues, "//initRenderer parameter " + supp.getName(),
//                        "type "+varNameReal+" = mod(params["+paramIndexReal+"] + params["+paramIndexDelta+"] * deltaX, 4.0) + 2.0;",
//                        "type "+varNameImag+" = mod(params["+paramIndexImag+"] + params["+paramIndexDelta+"] * deltaY, 4.0) + 2.0;");
            }
            else if (supp instanceof CoordinateBasicShiftParamSupplier){
                CoordinateBasicShiftParamSupplier shiftSupp = (CoordinateBasicShiftParamSupplier) supp;
//                writeStringBuilderLines(stringBuilder, placeholderValues, "//initRenderer parameter " + supp.getName(),
//                        "type "+varNameReal+" = mod(params["+paramIndexReal+"] + params["+paramIndexDelta+"] * deltaX, 4.0) + 2.0;",
//                        "type "+varNameImag+" = mod(params["+paramIndexImag+"] + params["+paramIndexDelta+"] * deltaY, 4.0) + 2.0;");
                writeStringBuilderLines(stringBuilder, placeholderValues, "//initRenderer parameter " + supp.getName(),
                        "type "+varNameReal+" = params["+paramIndexReal+"] + params["+paramIndexDelta+"] * deltaX;",
                        "type "+varNameImag+" = params["+paramIndexImag+"] + params["+paramIndexDelta+"] * deltaY;");
            }
            else if (supp instanceof CoordinateModuloParamSupplier){
                writeStringBuilderLines(stringBuilder, placeholderValues, "//initRenderer parameter " + supp.getName(),
                        "type "+varNameReal+" = mod(deltaX, 4.0) + 2.0;",
                        "type "+varNameImag+" = mod(deltaY, 4.0) + 2.0;");
            }
            else if (supp instanceof CoordinateDiscreteModuloParamSupplier){
                CoordinateDiscreteModuloParamSupplier dmSupp = (CoordinateDiscreteModuloParamSupplier) supp;
                double modulo = dmSupp.getModulo().toDouble();
                double stepSize = dmSupp.getStepSize().toDouble();
                placeholderValues.put("modulo", modulo+"");
                placeholderValues.put("stepSizeFactor", stepSize/modulo+"");
                writeStringBuilderLines(stringBuilder, placeholderValues, "//initRenderer parameter " + supp.getName(),
                        "type "+varNameReal+" = mod(params["+paramIndexReal+"] + params["+paramIndexDelta+"] * deltaX, modulo)*stepSizeFactor;",
                        "type "+varNameImag+" = mod(params["+paramIndexImag+"] + params["+paramIndexDelta+"] * deltaY, modulo)*stepSizeFactor;");
            }
            else
                throw new IllegalArgumentException("Unsupported ParamSupplier "+supp.getName()+": "+supp.getClass().getName());
        }

        for (Integer copySlot : firstExpression.getCopySlots()){

            writeStringBuilderLines(stringBuilder, placeholderValues, "//initRenderer copy slots "+copySlot+", "+(copySlot+1),
                    "type "+localVariables[copySlot]  +" = 0.0;",
                    "type "+localVariables[copySlot+1]+" = 0.0;");
        }
//        for (int i = 0 ; i < kernel.paramNames.length ; i++){
//            String name = kernel.paramNames[i];
//            ParamSupplier supp = systemContext.getParamContainer().getClientParameter(name);
//
//            if (supp == null){
//                //search constant
//                supp = kernel.constantSuppliers.get(name);
//                //no constant found -> missing supplier
//                if (supp == null)
//                    throw new IllegalStateException("Missing ParamSupplier "+name);
//            }
//
//            if (supp instanceof StaticParamSupplier){
//                ComplexNumber val = (ComplexNumber)((StaticParamSupplier)supp).getObj();
//                kernel.params[i*3] = (float) val.realDouble();
//                kernel.params[i*3+1] = (float) val.imagDouble();
//                kernel.params[i*3+2] = 0;
//            }
//            else if (supp instanceof CoordinateBasicShiftParamSupplier){
//                CoordinateBasicShiftParamSupplier shiftSupp = (CoordinateBasicShiftParamSupplier) supp;
//                kernel.params[i*3] = chunk.chunkPos.realDouble();
//                kernel.params[i*3+1] = chunk.chunkPos.imagDouble();
//                kernel.params[i*3+2] = systemContext.getPixelzoom().toDouble();
//            }
//            else
//                throw new IllegalArgumentException("Unsupported ParamSupplier "+supp.getName()+": "+supp.getClass().getName());
//        }
//
//        int dataOffset = getLocalId()*instructions[1];
//        for (int j = 0 ; j < params.length/3 ; j++){
//            data[j*2+0+dataOffset] = (float)(params[j*3+0] + x * params[j*3+2]);
//            data[j*2+1+dataOffset] = (float)(params[j*3+1] + y * params[j*3+2]);
//        }
//        for (int j = params.length/3 ; j < instructions[1]/2 ; j++){
//            data[j*2+0+dataOffset] = 0;
//            data[j*2+1+dataOffset] = 0;
//        }

        System.out.println("Shader initVariables(): ");
        System.out.println(stringBuilder.toString());
        return stringBuilder.toString();
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

        if (iterationsVarIndexReal >= 0){
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("varReal", localVariables[iterationsVarIndexReal]);
            placeholders.put("varImag", localVariables[iterationsVarIndexReal+1]);
            writeStringBuilderLines(sb, placeholders,"//iteration variable",
                    "float varReal = i;",
                    "float varImag = 0.0;");
        }

        //write iteration instructions
//        for (ComputeExpression expr : expressionDomain.getMainExpressions()) {
        ComputeExpression expr = expressionDomain.getMainExpressions().get(0);
            List<ComputeInstruction> instructions = expr.getInstructions();
            for (ComputeInstruction instruction : instructions) {
                printKernelInstruction(sb, instruction);
            }
//        }

        //write orbit trap conditions
        ParamSupplier orbittrapSupp = systemContext.getParamContainer().getClientParameter(GPUSystemContext.PARAMNAME_ORBITTRAPS);
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
                                    "abs(local_1*factorR-local_0*factorI - offset) <= (width)");
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

        System.out.println("Shader iterate(): ");
        System.out.println(sb.toString());
        return sb.toString();
    }

    int temp_counter = 0;

    private String getTempVarName(){
        return "temp_"+temp_counter++;
    }

    private void printKernelInstruction(StringBuilder stringBuilder, ComputeInstruction instruction) {
//	      if (burningship > 0.0){
//	        resX = abs(resX);
//	        resY = abs(resY);
//        }
        Map<String, String> placeholderValues = new HashMap<>();
        placeholderValues.put("float", glslType);
//        switch (instruction.type){
            //TODO variable mapping
            if (instruction.type == ComputeInstruction.INSTR_ADD_COMPLEX){
                writeinstrunctionlines(stringBuilder, instruction, placeholderValues, "//add_complex",
                        "fromReal = fromReal + toReal;",
                        "fromImag = fromImag + toImag;");
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
                //newR = fromReal*toReal + fromImag*toImag
                //newI = fromImag*toReal - fromReal*toImag
                //div = toReal*toReal + toImag*toImag
                //fromReal = newR/div
                //fromImag = newI/div
//                String temp_div = getTempVarName();
//                String tempR = getTempVarName();
//                String tempI = getTempVarName();
                placeholderValues.put("temp_div", getTempVarName());
                placeholderValues.put("tempR", getTempVarName());
                placeholderValues.put("tempI", getTempVarName());
                writeinstrunctionlines(stringBuilder, instruction, placeholderValues, "//div_complex",
                        "float tempR = fromReal*toReal + fromImag*toImag;",
                        "float tempI = fromImag*toReal - fromReal*toImag;",
                        "float temp_div = toReal*toReal + toImag*toImag;",
                        "fromReal = tempR/temp_div;",
                        "fromImag = tempI/temp_div;"
                        );
            } else if (instruction.type == ComputeInstruction.INSTR_DIV_PART){
                writeinstrunctionlines(stringBuilder, instruction, placeholderValues, "//div_part",
                        "fromReal = fromReal / toReal;");
            } else if (instruction.type == ComputeInstruction.INSTR_POW_COMPLEX){
//                float temp1;
//                float temp2;
//                float temp3;
//                temp3 = sqrt(data[r1]*data[r1]+data[i1]*data[i1]);
//                if (temp3 != 0){
//                    temp3 = log(temp3);
//                    temp1 = atan2(data[i1], data[r1]);
//                    //mult
//                    temp2 = (temp3*data[r2] - temp1*data[i2]);
//                    temp1 = (temp3*data[i2] + temp1*data[r2]);
//                    temp3 = temp2;
//                    //exp
//                    temp2 = exp(temp3);
//                    data[r1] = temp2 * cos(temp1);
//                    data[i1] = temp2 * sin(temp1);
//                } else {
//                    data[r1] = data[r1]; //Workaround for "child list broken" in nested if-clause without else
//                }
                String temp1 = getTempVarName();
                String temp2 = getTempVarName();
                String temp3 = getTempVarName();
                writeinstrunctionlines(stringBuilder, instruction, placeholderValues, "//pow_complex",
                        "float "+temp3+" = sqrt(fromReal*fromReal + fromImag*fromImag);",
                        "float "+temp2+" = 0.0;",
                        "float "+temp1+" = 0.0;",
                        "if ("+temp3+" != 0) {",
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
                        "float "+tempVarName6+" = sin(2.0*fromReal)/(cos(2.0*fromReal)+cosh(2.0*fromImag));",
                        "fromImag = sinh(2.0*fromImag)/(cos(2.0*fromReal)+cosh(2.0*fromImag));",
                        "fromReal = "+tempVarName6+";");
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
                writeinstrunctionlines(stringBuilder, instruction, placeholderValues, "//tanh_complex",
                        "float "+tempVarName3+" = sinh(2.0*fromReal)/(cosh(2.0*fromReal)+cos(2.0*fromImag));",
                        "fromImag = sin(2.0*fromImag)/(cosh(2.0*fromReal)+cos(2.0*fromImag));",
                        "fromReal = "+tempVarName3+";");
//                "float temp = (cosh(2.0*fromReal)+cos(2.0*fromImag));",
//                        "fromImag = sin(2.0*fromImag)/temp;",
//                        "fromReal = sinh(2.0*fromReal)/temp;");
            } else if (instruction.type == ComputeInstruction.INSTR_TANH_PART){
                writeinstrunctionlines(stringBuilder, instruction, placeholderValues, "//tanh_part",
                        "fromReal = tanh(fromReal);");
            } else if (instruction.type == ComputeInstruction.INSTR_SQUARE_COMPLEX){
                String tempVarName2 = getTempVarName();
                writeinstrunctionlines(stringBuilder, instruction, placeholderValues, "//square_complex",
                        "float "+tempVarName2+" = fromReal*fromReal - fromImag*fromImag;",
                        "fromImag = fromReal*fromImag*2.0;",
                        "fromReal = "+tempVarName2+";");
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
                writeinstrunctionlines(stringBuilder, instruction, placeholderValues, "//reciprocal_complex",
                        "float " + tempVarName9 + " = fromReal*fromReal + fromImag*fromImag;",
                        "if (" + tempVarName9 + " != 0.0){",
                        "   fromReal =   fromReal / " + tempVarName9 + ";",
                        "   fromImag = - fromImag / " + tempVarName9 + ";",
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
        for (String line : lines){
            if (fromReal) {
                line = line.replaceAll("fromReal", localVariables[instruction.fromReal]);
            }
            if (fromImag) {
                line = line.replaceAll("fromImag", localVariables[instruction.fromImag]);
            }
            if (toReal) {
                line = line.replaceAll("toReal", localVariables[instruction.toReal]);
            }
            if (toImag) {
                line = line.replaceAll("toImag", localVariables[instruction.toImag]);
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
