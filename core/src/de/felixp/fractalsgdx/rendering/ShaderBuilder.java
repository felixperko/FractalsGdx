package de.felixp.fractalsgdx.rendering;

import java.util.List;

import de.felixperko.fractals.system.calculator.ComputeExpression;
import de.felixperko.fractals.system.calculator.ComputeInstruction;
import de.felixperko.fractals.system.numbers.ComplexNumber;
import de.felixperko.fractals.system.numbers.Number;
import de.felixperko.fractals.system.parameters.suppliers.CoordinateBasicShiftParamSupplier;
import de.felixperko.fractals.system.parameters.suppliers.ParamSupplier;
import de.felixperko.fractals.system.parameters.suppliers.StaticParamSupplier;
import de.felixperko.fractals.system.systems.infra.SystemContext;

public class ShaderBuilder {

    final static String MAKRO_INIT = "<INIT>";
    final static String MAKRO_KERNEL = "<ITERATE>";

    String[] localVariables;
    SystemContext systemContext;
    ComputeExpression expression;

    public ShaderBuilder(ComputeExpression expression, SystemContext systemContext){
        this.expression = expression;
        this.systemContext = systemContext;
        List<ParamSupplier> params = expression.getParameterList();
        localVariables = new String[params.size() * 2];
        for (int i = 0; i < params.size(); i++) {
            localVariables[i * 2] = "local_" + i * 2;
            localVariables[i * 2 + 1] = "local_" + (i * 2 + 1);
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
        return line;
    }

    private String getInitString() {
        StringBuilder stringBuilder = new StringBuilder();
//        List<ComputeInstruction> instructions = expression.getInstructions();
//        List<ParamSupplier> params = expression.getParameterList();
//        for (String localVar : localVariables)
//            writeStringBuilderLine(stringBuilder, "float "+localVar+" = 0.0;");

        List<ParamSupplier> paramSuppliers = expression.getParameterList();
//        Map<String, ParamSupplier> constantSuppliers = expression.getConstants();

        int varCounter = 0;
        double zoom = ((Number)systemContext.getParamValue("zoom")).toDouble();
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
                ComplexNumber val = (ComplexNumber)((StaticParamSupplier)supp).getObj();
                writeStringBuilderLines(stringBuilder, "//init parameter " + supp.getName(),
                        "float "+varNameReal+" = params["+paramIndexReal+"] + params["+paramIndexDelta+"] * deltaX;",
                        "float "+varNameImag+" = params["+paramIndexImag+"] + params["+paramIndexDelta+"] * deltaY;");
//                writeStringBuilderLines(stringBuilder, "//init parameter " + supp.getName(),
//                        varNameReal + " = " +val.realDouble()+";",
//                        varNameImag + " = " +val.imagDouble()+";");
            }
            else if (supp instanceof CoordinateBasicShiftParamSupplier){
                CoordinateBasicShiftParamSupplier shiftSupp = (CoordinateBasicShiftParamSupplier) supp;
                ComplexNumber reference = systemContext.getNumberFactory().createComplexNumber("-3.0", "0.0");
//                writeStringBuilderLines(stringBuilder, "//init parameter " + supp.getName(),
//                        varNameReal + " = " +reference.realDouble()+" + x * "+zoom+" / "+height+";",
//                        varNameImag + " = " +reference.imagDouble()+" + y * "+zoom+" / "+height+";");
                writeStringBuilderLines(stringBuilder, "//init parameter " + supp.getName(),
                        "float "+varNameReal+" = params["+paramIndexReal+"] + params["+paramIndexDelta+"] * deltaX;",
                        "float "+varNameImag+" = params["+paramIndexImag+"] + params["+paramIndexDelta+"] * deltaY;");
//                writeStringBuilderLines(stringBuilder, "//init parameter "+supp.getName(),
//                        "float "+varNameReal+" = (pos.x - 0.5)*ratio * scale + center.x;",
//                        "float "+varNameImag+" = (((pos.y - 0.5) * scale) + center.y);");
            }
            else
                throw new IllegalArgumentException("Unsupported ParamSupplier "+supp.getName()+": "+supp.getClass().getName());
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

    private String getKernelString() {
        StringBuilder stringBuilder = new StringBuilder();
        List<ComputeInstruction> instructions = expression.getInstructions();
        for (ComputeInstruction instruction : instructions){
            printKernelInstruction(stringBuilder, instruction);
        }
        System.out.println("Shader iterate(): ");
        System.out.println(stringBuilder.toString());
        return stringBuilder.toString();
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
        switch (instruction.type){
            //TODO variable mapping
            case ComputeInstruction.INSTR_ADD_COMPLEX:
                writeinstrunctionlines(stringBuilder, instruction, "//add_complex",
                        "fromReal = fromReal + toReal;",
                        "fromImag = fromImag + toImag;");
                break;
            case ComputeInstruction.INSTR_ADD_PART:
                writeinstrunctionlines(stringBuilder, instruction, "//add_part",
                        "fromReal = fromReal + toReal;");
                break;
            case ComputeInstruction.INSTR_SUB_COMPLEX:
                writeinstrunctionlines(stringBuilder, instruction, "//sub_complex",
                        "fromReal = fromReal - toReal;",
                        "fromImag = fromImag - toImag;");
                break;
            case ComputeInstruction.INSTR_SUB_PART:
                writeinstrunctionlines(stringBuilder, instruction, "//sub_part",
                        "fromReal = fromReal - toReal;");
                break;
            case ComputeInstruction.INSTR_MULT_COMPLEX:
                String tempVarName = getTempVarName();
                writeinstrunctionlines(stringBuilder, instruction, "//mult_complex",
                        "float "+tempVarName+" = fromReal*toReal - fromImag*toImag;",
                        "fromImag = (fromReal + fromImag)*(toReal + toImag) - "+tempVarName+";",
                        "fromReal = "+tempVarName+";");
                break;
            case ComputeInstruction.INSTR_MULT_PART:
                writeinstrunctionlines(stringBuilder, instruction, "//mult_part",
                        "fromReal = fromReal*fromImag;");
                break;
            case ComputeInstruction.INSTR_DIV_COMPLEX:

                break;
            case ComputeInstruction.INSTR_DIV_PART:
                writeinstrunctionlines(stringBuilder, instruction, "//div_part",
                        "fromReal = fromReal / toReal;");
                break;
            case ComputeInstruction.INSTR_POW_COMPLEX:
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
                writeinstrunctionlines(stringBuilder, instruction, "//pow_complex",
                        "float "+temp3+" = sqrt(fromReal*fromReal + fromImag*fromImag);",
                        "float "+temp2+" = 0.0;",
                        "float "+temp1+" = 0.0;",
                        "if ("+temp3+" != 0){",
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
                );
                break;
            case ComputeInstruction.INSTR_POW_PART:
                writeinstrunctionlines(stringBuilder, instruction, "//pow_part",
                        "fromReal = fromReal^fromImag;");
                break;
            case ComputeInstruction.INSTR_COPY_COMPLEX:
                writeinstrunctionlines(stringBuilder, instruction, "//copy_complex",
                        "toReal = fromReal;",
                        "toImag = fromImag;");
                break;
            case ComputeInstruction.INSTR_COPY_PART:
                writeinstrunctionlines(stringBuilder, instruction, "//copy_part",
                        "toReal = fromReal;");
                break;
            case ComputeInstruction.INSTR_ABS_COMPLEX:
                writeinstrunctionlines(stringBuilder, instruction, "//abs_complex",
                        "fromReal = abs(fromReal);",
                        "fromImag = abs(fromImag);");
                break;
            case ComputeInstruction.INSTR_ABS_PART:
                writeinstrunctionlines(stringBuilder, instruction, "//abs_part",
                        "fromReal = abs(fromReal);");
                break;
            case ComputeInstruction.INSTR_SIN_COMPLEX:
                String tempVarName7 = getTempVarName();
                writeinstrunctionlines(stringBuilder, instruction, "//sin_complex",
                        "float "+tempVarName7+" = sin(fromReal) * cosh(fromImag);",
                        "fromImag = cos(fromReal) * sinh(fromImag);",
                        "fromReal = "+tempVarName7+";");
                break;
            case ComputeInstruction.INSTR_SIN_PART:
                writeinstrunctionlines(stringBuilder, instruction, "//sin_part",
                        "fromReal = sin(fromReal);");
                break;
            case ComputeInstruction.INSTR_COS_COMPLEX:
                String tempVarName8 = getTempVarName();
                writeinstrunctionlines(stringBuilder, instruction, "//cos_complex",
                        "float "+tempVarName8+" = cos(fromReal) * cosh(fromImag);",
                        "fromImag = -sin(fromReal) * sinh(fromImag);",
                        "fromReal = "+tempVarName8+";");
                break;
            case ComputeInstruction.INSTR_COS_PART:
                writeinstrunctionlines(stringBuilder, instruction, "//cos_part",
                        "fromReal = cos(fromReal);");
                break;
            case ComputeInstruction.INSTR_TAN_COMPLEX:
                String tempVarName6 = getTempVarName();
                writeinstrunctionlines(stringBuilder, instruction, "//tan_complex",
                        "float "+tempVarName6+" = sin(2.0*fromReal)/(cos(2.0*fromReal)+cosh(2.0*fromImag));",
                        "fromImag = sinh(2.0*fromImag)/(cos(2.0*fromReal)+cosh(2.0*fromImag));",
                        "fromReal = "+tempVarName6+";");
                break;
            case ComputeInstruction.INSTR_TAN_PART:
                writeinstrunctionlines(stringBuilder, instruction, "//tan_part",
                        "fromReal = tan(fromReal);");
                break;
            case ComputeInstruction.INSTR_SINH_COMPLEX:
                String tempVarName4 = getTempVarName();
                writeinstrunctionlines(stringBuilder, instruction, "//sinh_complex",
                        "float "+tempVarName4+" = sinh(fromReal) * cos(fromImag);",
                        "fromImag = cosh(fromReal)*sin(fromImag);",
                        "fromReal = "+tempVarName4+";");
                break;
            case ComputeInstruction.INSTR_SINH_PART:
                writeinstrunctionlines(stringBuilder, instruction, "//sinh_part",
                        "fromReal = sinh(fromReal);");
                break;
            case ComputeInstruction.INSTR_COSH_COMPLEX:
                String tempVarName5 = getTempVarName();
                writeinstrunctionlines(stringBuilder, instruction, "//cosh_complex",
                        "float "+tempVarName5+" = cosh(fromReal) * cos(fromImag);",
                        "fromImag = sinh(fromReal)*sin(fromImag);",
                        "fromReal = "+tempVarName5+";");
                break;
            case ComputeInstruction.INSTR_COSH_PART:
                writeinstrunctionlines(stringBuilder, instruction, "//cosh_part",
                        "fromReal = cosh(fromReal);");
                break;
            case ComputeInstruction.INSTR_TANH_COMPLEX:
                String tempVarName3 = getTempVarName();
                writeinstrunctionlines(stringBuilder, instruction, "//tanh_complex",
                        "float "+tempVarName3+" = sinh(2.0*fromReal)/(cosh(2.0*fromReal)+cos(2.0*fromImag));",
                        "fromImag = sin(2.0*fromImag)/(cosh(2.0*fromReal)+cos(2.0*fromImag));",
                        "fromReal = "+tempVarName3+";");
//                "float temp = (cosh(2.0*fromReal)+cos(2.0*fromImag));",
//                        "fromImag = sin(2.0*fromImag)/temp;",
//                        "fromReal = sinh(2.0*fromReal)/temp;");
                break;
            case ComputeInstruction.INSTR_TANH_PART:
                writeinstrunctionlines(stringBuilder, instruction, "//tanh_part",
                        "fromReal = tanh(fromReal);");
                break;
            case ComputeInstruction.INSTR_SQUARE_COMPLEX:
                String tempVarName2 = getTempVarName();
                writeinstrunctionlines(stringBuilder, instruction, "//square_complex",
                        "float "+tempVarName2+" = fromReal*fromReal - fromImag*fromImag;",
                        "fromImag = fromReal*fromImag*2.0;",
                        "fromReal = "+tempVarName2+";");
                break;
            case ComputeInstruction.INSTR_SQUARE_PART:
                writeinstrunctionlines(stringBuilder, instruction, "//square_part",
                        "fromReal = fromReal*fromReal;");
                break;
            case ComputeInstruction.INSTR_NEGATE_COMPLEX:
                writeinstrunctionlines(stringBuilder, instruction, "//negate_complex",
                        "fromReal = -fromReal;",
                        "fromImag = -fromImag;");
                break;
            case ComputeInstruction.INSTR_NEGATE_PART:
                writeinstrunctionlines(stringBuilder, instruction, "//negate_part",
                        "fromReal = -fromReal;");
                break;
        }
    }


    private void writeinstrunctionlines(StringBuilder stringBuilder, ComputeInstruction instruction, String... lines){
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
        writeStringBuilderLines(stringBuilder, newLines);
    }


    private void writeStringBuilderLines(StringBuilder kernelStringBuilder, String... lines){
        for (String line : lines)
            writeStringBuilderLine(kernelStringBuilder, line);
    }

    private void writeStringBuilderLine(StringBuilder stringBuilder, String text){
        stringBuilder.append(text).append(System.getProperty("line.separator"));
    }
}
