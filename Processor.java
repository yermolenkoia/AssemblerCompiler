package com.sp;

import com.sp.model.Identifier;
import com.sp.model.Line;

import java.io.IOException;
import java.math.BigInteger;
import java.util.*;

import static com.sp.Utils.Constants.*;
import static com.sp.Utils.UtilOperations.*;

public class Processor {

    //variables for line
    private Integer lineNumber = 1;
    private Integer offset = 0;

    //variable for if statement
    private boolean openedIf = false;
    private boolean resultIf = true;

    //variables for errors
    private boolean segmentOpen = false;
    private boolean closedProgram = false;

    private List<Identifier> identifiers = new ArrayList<>();
    private boolean second = false;

    public void analyze(String fileName) throws IOException {
        List<Line> lines = new ArrayList<>();
        //read information from file
        List<String> stringLines = readFIle(fileName);
        boolean miss = true;

        //1-st round
        //parse lines fill identifiers
        for (String stringLine : stringLines) {
            if (openedIf) {
                //if statement opened
                if (resultIf) {
                    //only if part
                    if (stringLine.contains("ELSE")) {
                        miss = true;
                    } else if (stringLine.contains("ENDIF")) {
                        miss = false;
                    }
                } else {
                    //only else part
                    if (stringLine.contains("ELSE")) {
                        miss = false;
                    }
                }
                if (miss) {
                    continue;
                }
            }
            parseStringToLine(stringLine);
        }

        clearSegments();
        second = true;
        lineNumber = 1;
        offset = 0;
        miss = true;
        //2-nd round
        //parse all and remember all information
        for (String stringLine : stringLines) {
            if (openedIf) {
                //if statement opened
                if (resultIf) {
                    //only if part
                    if (stringLine.contains("ELSE")) {
                        miss = true;
                    } else if (stringLine.contains("ENDIF")) {
                        miss = false;
                    }
                } else {
                    //only else part
                    if (stringLine.contains("ELSE")) {
                        miss = false;
                    }
                }
                if (miss) {
                    continue;
                }
            }
            lines.add(parseStringToLine(stringLine));
        }
        if (!closedProgram) {
            lines.get(lines.size() - 1).setError("Program not closed");
        }
        //write all information to files
        writeFile(fileName, lines, identifiers);

    }

    @SuppressWarnings("UnusedDeclaration")
    public void lexemeAnalyze(String fileName) throws IOException {
        List<String> stringLines = readFIle(fileName);

        StringTokenizer tokenizer;
        for (String s : stringLines) {
            if (s.trim().isEmpty()) {
                continue;
            }
            tokenizer = new StringTokenizer(s, ";,:+-[] ", true);
            System.out.println("Line ---  " + s + " --- contains:");
            int i = 1;
            while (tokenizer.hasMoreTokens()) {
                String trim = tokenizer.nextToken().trim();
                if (trim.isEmpty()) {
                    continue;
                }
                String token = i++ + ": " + trim;
                token += " length:" + trim.length();
                if (isCommand(trim)) {
                    token += " - command";
                } else if (isIdentifier(trim)) {
                    token += " - identifier";
                } else if (isTextConstant(trim)) {
                    token += " text constant";
                } else if (isKeyWord(trim)) {
                    token += " - key word";
                } else if (isRegister(trim)) {
                    token += " - register";
                } else if (isConstant(trim) != 0) {
                    token += " - constant";
                } else if (trim.length() == 1) {
                    token += " - one delimiter";
                }

                System.out.println(token);
            }
            System.out.println();
        }
    }

    //parses string line to Line class
    //will be like var1 db 18 -> 5 0000 12 VAR1 DB 18
    private Line parseStringToLine(String stringLine) {
        Line line = new Line();
        stringLine = stringLine.trim();

        if (!stringLine.isEmpty()) {
            //remove comment for parsing line
            String[] strings = removeEmptyAndChars(stringLine.contains(";") ? stringLine.substring(0, stringLine.indexOf(';')).split(",| ") : stringLine.split(",| "), "");
            //get first word of all array
            if (strings.length > 0) {


                String firstWord = strings[0];
                if (firstWord.startsWith(";")) {
                    //comment line; all after ; is comment and ignored
                } else if (isIdentifier(firstWord)) {
                    line = parseIdentifier(strings);
                } else if (isCommand(firstWord)) {
                    line = parseCommand(strings);
                } else if (isKeyWord(firstWord)) {
                    line = parseKeyWord(strings);
                }
            }
        }

        line.setLineNumber(lineNumber++);
        line.setOffset(offset);
        line.setLine(stringLine);
        //generate offset by information that contains in line; like address size or operand size
        offset += line.generateOffset();
        if (findCurrentSegment() == null) {
            offset = 0;
        }
        return line;
    }

    //label, variable or segment open and close
    private Line parseIdentifier(String[] strings) {
        Line line = new Line();
        String firstWord = strings[0];

        if (firstWord.length() > 8) {
            line.setError("Identifiers length more than 8 chars");
        }
        if (firstWord.endsWith(":")) {
            //label
            line.setError(setIdentifier(firstWord.substring(0, firstWord.length() - 1), "L NEAR", ZERO));
        } else {
            //variable
            String next = strings[1];
            if (getKeyWords()[4].equals(next)) {
                //db
                if (!DB) {
                    line.setError("Can\'t use this statement");
                }
                BigInteger operand;
                if (!isTextConstant(strings[2])) {
                    line.setOperandSize(1);
                    line.setError(checkConstant(strings[2]));
                    if (FLOAT) {
                        //can exist float part
                        if (strings.length > 3) {
                            strings[2] += "," + strings[3];
                            line.setOperandSize(2);
                        }
                    }
                    operand = FLOAT ? parseFloat(strings[2], 1, true) : parseInt(strings[2], 1, true);
                    if (operand == null) {
                        return line;
                    }
                    if (operand.compareTo(ZERO) == -1 || operand.compareTo(BYTES_1) == 1) {
                        if (FLOAT && operand.compareTo(BYTES_2) == 1) {
                            line.setError("Operand size more than 2 byte");
                        } else {
                            line.setError("Operand size more than 1 byte");
                        }
                    }
                    line.setOperand(operand);
                } else {
                    if (!TEXT_CONSTANTS) {
                        line.setError("Text constants isn\'t allowed");
                    }
                    //length of constant without "" or ''
                    String text = strings[2].substring(1, strings[2].length() - 1);
                    operand = ZERO;
                    for (char c : text.toCharArray()) {
                        operand = operand.multiply(BigInteger.valueOf(0x100)).add(BigInteger.valueOf(c));
                    }
                    line.setOperandSize(text.length());
                    line.setOperand(operand);
                }

                line.setError(setIdentifier(firstWord, getKeyWords()[8], operand));
            } else if (getKeyWords()[5].equals(next)) {
                //dw
                if (!DW) {
                    line.setError("Can\'t use this statement");
                }
                line.setOperandSize(2);
                line.setError(checkConstant(strings[2]));
                if (FLOAT) {
                    //can exist float part
                    if (strings.length > 3) {
                        strings[2] += "," + strings[3];
                        line.setOperandSize(4);
                    }
                }
                BigInteger operand = FLOAT ? parseFloat(strings[2], 2, true) : parseInt(strings[2], 2, true);
                if (operand == null) {
                    return line;
                }
                if (operand.compareTo(ZERO) == -1 || operand.compareTo(BYTES_2) == 1) {
                    if (FLOAT && operand.compareTo(BYTES_4) == 1) {
                        line.setError("Operand size more than 4 bytes");
                    } else {
                        line.setError("Operand size more than 2 byte");
                    }
                }
                line.setOperand(operand);
                line.setError(setIdentifier(firstWord, getKeyWords()[9], operand));
            } else if (getKeyWords()[6].equals(next)) {
                //dd
                if (!DD) {
                    line.setError("Can\'t use this statement");
                }
                line.setOperandSize(4);
                line.setError(checkConstant(strings[2]));
                if (FLOAT) {
                    //can exist float part
                    if (strings.length > 3) {
                        strings[2] += "," + strings[3];
                        line.setOperandSize(8);
                    }
                }
                BigInteger operand = FLOAT ? parseFloat(strings[2], 4, true) : parseInt(strings[2], 4, true);
                if (operand == null) {
                    return line;
                }
                if (operand.compareTo(ZERO) == -1 || operand.compareTo(BYTES_4) == 1) {
                    if (FLOAT && operand.compareTo(BYTES_8) == 1) {
                        line.setError("Operand size more than 8 bytes");
                    } else {
                        line.setError("Operand size more than 4 byte");
                    }
                }
                line.setOperand(operand);
                line.setError(setIdentifier(firstWord, getKeyWords()[10], operand));
            } else if (getKeyWords()[7].equals(next)) {
                //dq
                if (!DQ) {
                    line.setError("Can\'t use this statement");
                }
                line.setOperandSize(8);
                line.setError(checkConstant(strings[2]));
                if (FLOAT) {
                    //can exist float part
                    if (strings.length > 3) {
                        strings[2] += "," + strings[3];
                        line.setOperandSize(16);
                    }
                }
                BigInteger operand = FLOAT ? parseFloat(strings[2], 8, true) : parseInt(strings[2], 8, true);
                if (operand == null) {
                    return line;
                }
                if (operand.compareTo(ZERO) == -1 || operand.compareTo(BYTES_8) == 1) {
                    if (FLOAT && operand.compareTo(BYTES_16) == 1) {
                        line.setError("Operand size more than 16 bytes");
                    } else {
                        line.setError("Operand size more than 8 byte");
                    }
                }
                line.setOperand(operand);
                line.setError(setIdentifier(firstWord, getKeyWords()[20], operand));
            } else if (getKeyWords()[16].equals(next)) {
                //equ
                if (!EQU) {
                    line.setError("Can\'t use this statement");
                }
                BigInteger operand = parseInt(strings[2], 4, false, false);
                if (operand == null) {
                    return line;
                }
                if (operand.compareTo(BYTES_2) == 1) {
                    line.setOperandSize(4);
                } else {
                    line.setOperandSize(2);
                }
                line.setError(checkConstant(strings[2]));
                line.setOffset(null);
                line.setOperand(operand);
                line.setError(setIdentifier(firstWord, getKeyWords()[16], operand));
            } else if (getKeyWords()[12].equals(next)) {
                //label
                String type = strings[2];
                if (!type.equals(getKeyWords()[8]) && !type.equals(getKeyWords()[9]) && !type.equals(getKeyWords()[10])) {
                    line.setError("Unknown statement");
                }
                line.setError(setIdentifier(firstWord, type, ZERO));
            } else if (getKeyWords()[0].equals(next)) {
                //segment open
                if (segmentOpen) {
                    line.setError("Opened segment before previous closed");
                }
                line.setError(setIdentifier(firstWord, getKeyWords()[0], ZERO));
                segmentOpen = true;
            } else if (getKeyWords()[1].equals(next)) {
                //segment close
                line.setError(closeSegment());
                segmentOpen = false;
            } else if (getKeyWords()[14].equals(next)) {
                if (!PROCEDURE) {
                    line.setError("Can\'t use this statement");
                }
                //procedure
                line.setError(setIdentifier(firstWord, getKeyWords()[14], ZERO));
            } else if (getKeyWords()[15].equals(next)) {
                //end of procedure
                if (!PROCEDURE) {
                    line.setError("Can\'t use this statement");
                }
                Identifier i = getIdentifierByName(firstWord);
                if (i != null) {
                    i.setClosed(true);
                    i.setLength(offset - i.getOffset());
                } else {
                    line.setError("Procedure not defined");
                }
            } else {
                line.setError("Unknown statement");
            }
        }
        return line;
    }

    //mov, add ...
    //Every command uses some rules like: reg,reg or mem,imm

    private Line parseCommand(String[] strings) {
        Line line = new Line();
        //mov, add ...
        String command = strings[0];
        //reg/reg or reg/mem ...
        String[] rule = getCommands().get(command);

        if (rule.length == 2 && strings.length < 3 || rule.length == 1 && strings.length < 2) {
            line.setError("Miss part of command");
            return line;
        }

        int registerAddressMode;
        int memoryAddressMode = 0;

        if (rule == null) {
            //command not found
            line.setError("Unknown command");
        } else {
            if (Arrays.equals(rule, REG_MEM)) {
                String[] data = new String[]{"", ""};
                //command reg mem; for MRM and SIB
                if (!isRegister(strings[1])) {
                    //register miss
                    line.setError("Miss register at left part of command");
                } else if (strings.length == 2) {
                    //mem miss
                    line.setError("Miss mem at right part of command");
                }
                //reg
                data[0] = strings[1];
                if (!isIdentifier(strings[2])) {
                    for (int i = 2; i < strings.length; i++) {
                        data[1] += strings[i] + " ";
                    }
                    line = generateMrmSib(data, rule);

                    line.setChangeSegmentPrefix(getSegmentChangePrefix(data[1]));
                    if (getKeyWords()[10].equals(strings[2])) {
                        memoryAddressMode = 1;
                        if (USE_16) {
                            line.setError("Type miss match");
                        }
                    } else if (getKeyWords()[9].equals(strings[2])) {
                        memoryAddressMode = 1;
                        line.setOperandSizePrefix(OPERAND_SIZE_PREFIX);
                    }
                } else {
                    String identifierName;
                    if (INDEX_ADDRESSING) {
                        identifierName = strings[2].split("\\[")[0];
                        //generate mem for mrm and sib
                        String mem = "";
                        for (int i = 2; i < strings.length; i++) {
                            if (strings[i].contains("[")) {
                                mem += "[" + strings[i].split("\\[")[1] + " ";
                            } else {
                                mem += strings[i] + " ";
                            }
                        }
                        line = generateMrmSib(new String[]{mem}, ONLY_MEM);
                        //set mod = 2 of mrm byte
                    } else {
                        if (strings.length > 3) {
                            line.setError("Extra characters on line");
                        }
                        identifierName = strings[2];
                    }
                    Integer segmentChangePrefix = getSegmentChangePrefix(identifierName);
                    if (segmentChangePrefix != null) {
                        line.setChangeSegmentPrefix(segmentChangePrefix);
                        identifierName = identifierName.replaceAll(getSegmentRegistersToRemove(), "");
                    }
                    Identifier i = getIdentifierByName(identifierName);
                    if (i != null) {
                        if (i.getType().equals(getKeyWords()[10])) {
                            memoryAddressMode = 1;
                            if (USE_16) {
                                line.setError("Type miss match");
                            }
                        } else if (i.getType().equals(getKeyWords()[9])) {
                            memoryAddressMode = 1;
                            line.setOperandSizePrefix(OPERAND_SIZE_PREFIX);
                        }
                        line.setChangeSegmentPrefix(checkIdentifierForPrefixChangeSegment(identifierName));
                        line.setError(checkAddressingRegister(data[0]));
                        line.setOperand(BigInteger.valueOf(i.getOffset()));
                        line.setOperandSize(4);
                        if (INDEX_ADDRESSING) {
                            line.setMrm(getRegisterNumber(data[0]) << 3 | 0x80 | 4);
                        } else {
                            line.setMrm(getRegisterNumber(data[0]) << 3 | 5);
                        }
                    } else {
                        line.setError("Unknown identifier");
                    }
                }
                registerAddressMode = getRegisterAddressMode(data[0]);
                if (registerAddressMode == 0 && memoryAddressMode == 1) {
                    line.setError("Type miss match");
                }
                line.setOperationCode(getOperationCodes().get(command) + registerAddressMode);
            } else if (Arrays.equals(rule, MEM_REG)) {
                String[] data = new String[]{"", ""};
                //command reg mem; for MRM and SIB
                if (!isRegister(strings[strings.length - 1])) {
                    //register miss
                    line.setError("Miss register at right part of command");
                } else if (strings.length == 2) {
                    //mem miss
                    line.setError("Miss mem at right part of command");
                }

                //register
                data[1] = strings[strings.length - 1];

                if (!isIdentifier(strings[1])) {

                    for (int i = 1; i < strings.length - 1; i++) {
                        data[0] += strings[i] + " ";
                    }

                    line = generateMrmSib(data, rule);

                    line.setChangeSegmentPrefix(getSegmentChangePrefix(data[0]));
                    if (strings[1].equals(getKeyWords()[9])) {
                        memoryAddressMode = 1;
                        line.setOperandSizePrefix(OPERAND_SIZE_PREFIX);
                    } else if (strings[1].equals(getKeyWords()[10])) {
                        memoryAddressMode = 1;
                    }

                } else {
                    String identifierName;
                    if (INDEX_ADDRESSING) {
                        identifierName = strings[1].split("\\[")[0];
                        //generate mem for mrm and sib
                        String mem = "";
                        for (int i = 1; i < strings.length - 1; i++) {
                            if (strings[i].contains("[")) {
                                mem += "[" + strings[i].split("\\[")[1] + " ";
                            } else {
                                mem += strings[i] + " ";
                            }
                        }
                        line = generateMrmSib(new String[]{mem}, ONLY_MEM);
                        //set mod = 2 of mrm byte
                    } else {
                        if (strings.length > 3) {
                            line.setError("Extra characters on line");
                        }
                        identifierName = strings[1];
                    }
                    Integer segmentChangePrefix = getSegmentChangePrefix(identifierName);
                    if (segmentChangePrefix != null) {
                        line.setChangeSegmentPrefix(segmentChangePrefix);
                        identifierName = identifierName.replaceAll(getSegmentRegistersToRemove(), "");
                    }
                    Identifier i = getIdentifierByName(identifierName);
                    if (i != null) {
                        if (i.getType().equals(getKeyWords()[10])) {
                            memoryAddressMode = 1;
                        } else if (i.getType().equals(getKeyWords()[9])) {
                            memoryAddressMode = 1;
                            line.setOperandSizePrefix(OPERAND_SIZE_PREFIX);
                        }
                        //check for register correct using
                        line.setChangeSegmentPrefix(checkIdentifierForPrefixChangeSegment(identifierName));
                        line.setError(checkAddressingRegister(strings[strings.length - 1]));
                        line.setOperand(BigInteger.valueOf(i.getOffset()));
                        line.setOperandSize(4);
                        if (INDEX_ADDRESSING) {
                            line.setMrm(getRegisterNumber(data[1]) << 3 | 0x80 | 4);
                        } else {
                            line.setMrm(getRegisterNumber(data[1]) << 3 | 5);
                        }
                    } else {
                        line.setError("Unknown identifier");
                    }
                }
                registerAddressMode = getRegisterAddressMode(data[1]);
                if (memoryAddressMode == 0 && registerAddressMode == 1 || memoryAddressMode == 1 && registerAddressMode == 0 ||
                        USE_16 && getRegisterPos(data[1]) >= 0 && getRegisterPos(data[1]) <= 7) {
                    line.setError("Type miss match");
                }
                line.setOperationCode(getOperationCodes().get(command) + registerAddressMode);
            } else if (Arrays.equals(rule, REG_REG)) {

                String[] data = new String[]{"", ""};
                //command reg mem; for MRM and SIB
                if (!isRegister(strings[1])) {
                    //reg1 miss
                    line.setError("Miss register at left part of command");
                } else if (!isRegister(strings[strings.length - 1])) {
                    //reg2 miss
                    line.setError("Miss register at right part of command");
                }
                data[0] = strings[1];
                data[1] = strings[strings.length - 1];
                line = generateMrmSib(data, rule);
                int firstPos = getRegisterPos(data[0]);
                int secondPos = getRegisterPos(data[1]);
                if (firstPos >= 0 && firstPos <= 7 || secondPos >= 0 && secondPos <= 7) {
                    line.setOperandSizePrefix(OPERAND_SIZE_PREFIX);
                }
                int first = getRegisterAddressMode(data[0]);
                int second = getRegisterAddressMode(data[1]);
                if (first == 0 && second == 1 || USE_16 && getRegisterPos(data[1]) <= 7) {
                    line.setError("Type miss match");
                }
                line.setOperationCode(getOperationCodes().get(command) + first);
            } else if (Arrays.equals(rule, REG_IMM)) {

                String[] data = new String[]{"", ""};
                //command reg mem; for MRM and SIB
                if (!isRegister(strings[1])) {
                    //reg1 miss
                    line.setError("Miss register at left part of command");
                } else if (strings.length == 2) {
                    //imm miss
                    line.setError("Miss imm at right part of command");
                }
                //reg
                data[0] = strings[1];
                int firstPos = getRegisterPos(data[0]);
                if (firstPos >= 0 && firstPos <= 7) {
                    line.setOperandSizePrefix(OPERAND_SIZE_PREFIX);
                }
                //imm
                data[1] = strings[strings.length - 1];

                line.setError(checkAddressingRegister(data[0]));

                int addressMode = getRegisterAddressMode(data[0]);
                int registerNumber = getRegisterNumber(data[0]);
                line.setOperationCode(getOperationCodes().get(command) + addressMode * 8 + registerNumber);

                BigInteger operand = FLOAT ? parseFloat(data[1], addressMode == 1 ? USE_32 ? 4 : 2 : 1, false) :
                        parseInt(data[1], addressMode == 1 ? USE_32 ? 4 : 2 : 1, false);
                if (operand == null) {
                    line.setError("Miss numeric constant");
                    return line;
                }
                if (addressMode == 1) {
                    if (USE_32) {
                        if (FLOAT && operand.compareTo(BYTES_8) == 1) {
                            line.setError("Data more than 8 bytes");
                        } else if (operand.compareTo(BYTES_4) == 1) {
                            line.setError("Data more than 4 bytes");
                        }
                    } else {
                        if (FLOAT && operand.compareTo(BYTES_4) == 1) {
                            line.setError("Data more than 4 bytes");
                        } else if (operand.compareTo(BYTES_2) == 1) {
                            line.setError("Data more than 2 bytes");
                        }
                    }
                } else {
                    if (FLOAT && operand.compareTo(BYTES_2) == 1) {
                        line.setError("Data more than 2 bytes");
                    } else if (operand.compareTo(BYTES_1) == 1) {
                        line.setError("Data more than 1 bytes");
                    }
                }
                line.setOperand(operand);
                line.setOperandSize(addressMode == 1 ? USE_32 ? 4 : 2 : 1);
            } else if (Arrays.equals(rule, MEM_IMM)) {
                String[] data = new String[]{""};

                if (isIdentifier(strings[1])) {
                    String identifierName;
                    if (INDEX_ADDRESSING) {
                        identifierName = strings[1].split("\\[")[0];
                        //generate mem for mrm and sib
                        String mem = "";
                        for (int i = 1; i < strings.length - 1; i++) {
                            if (strings[i].contains("[")) {
                                mem += "[" + strings[i].split("\\[")[1] + " ";
                            } else {
                                mem += strings[i] + " ";
                            }
                        }
                        line = generateMrmSib(new String[]{mem}, ONLY_MEM);
                    } else {
                        if (strings.length > 3) {
                            line.setError("Extra characters on line");
                        }
                        identifierName = strings[1];
                    }
                    Integer segmentChangePrefix = getSegmentChangePrefix(identifierName);
                    if (segmentChangePrefix != null) {
                        line.setChangeSegmentPrefix(segmentChangePrefix);
                        identifierName = identifierName.replaceAll(getSegmentRegistersToRemove(), "");
                    }
                    Identifier i = getIdentifierByName(identifierName);
                    if (i != null) {
                        line.setChangeSegmentPrefix(checkIdentifierForPrefixChangeSegment(identifierName));
                        line.setAddress(i.getOffset());
                        line.setAddressSize(4);
                        if (INDEX_ADDRESSING) {
                            line.setMrm(line.getMrm() | 0x80);
                        } else {
                            line.setMrm(0xD);
                        }
                        if (i.getType().equals(getKeyWords()[10])) {
                            memoryAddressMode = 1;
                        } else if (i.getType().equals(getKeyWords()[9])) {
                            memoryAddressMode = 1;
                            line.setOperandSizePrefix(OPERAND_SIZE_PREFIX);
                        }
                    } else {
                        line.setError("Unknown identifier");
                    }
                } else {

                    //all string like : command dword/word/byte ptr mem; data[0] must contains only mem
                    for (int i = 1; i < strings.length - 1; i++) {
                        data[0] += strings[i] + " ";
                    }

                    line = generateMrmSib(data, ONLY_MEM);

                    line.setChangeSegmentPrefix(getSegmentChangePrefix(data[0]));
                    if (getKeyWords()[10].equals(strings[1])) {
                        memoryAddressMode = 1;
                    } else if (getKeyWords()[9].equals(strings[1])) {
                        memoryAddressMode = 1;
                        line.setOperandSizePrefix(OPERAND_SIZE_PREFIX);
                    }
                }

                BigInteger operand = FLOAT ? parseFloat(strings[strings.length - 1], memoryAddressMode == 1 ? USE_32 ? 4 : 2 : 1, false) :
                        parseInt(strings[strings.length - 1], memoryAddressMode == 1 ? USE_32 ? 4 : 2 : 1, false);
                if (operand == null) {
                    line.setError("Miss numeric constant");
                    return line;
                }
                if (memoryAddressMode == 1) {
                    if (USE_32) {
                        if (FLOAT && operand.compareTo(BYTES_8) == 1) {
                            line.setError("Data more than 8 bytes");
                        } else if (operand.compareTo(BYTES_4) == 1) {
                            line.setError("Data more than 4 bytes");
                        }
                    } else {
                        if (FLOAT && operand.compareTo(BYTES_4) == 1) {
                            line.setError("Data more than 4 bytes");
                        } else if (operand.compareTo(BYTES_2) == 1) {
                            line.setError("Data more than 2 bytes");
                        }
                    }
                } else {
                    if (FLOAT && operand.compareTo(BYTES_2) == 1) {
                        line.setError("Data more than 2 bytes");
                    } else if (operand.compareTo(BYTES_1) == 1) {
                        line.setError("Data more than 1 bytes");
                    }
                }
                line.setOperand(operand);
                line.setOperandSize(memoryAddressMode == 1 ? USE_32 ? 4 : 2 : 1);

                line.setOperationCode(getOperationCodes().get(command) + memoryAddressMode);
            } else if (Arrays.equals(rule, ONLY_REG)) {

                String register = strings[1];
                if (!isRegister(register)) {
                    line.setError("Miss register");
                }

                int firstPos = getRegisterPos(register);
                if (firstPos >= 0 && firstPos <= 7) {
                    line.setOperandSizePrefix(OPERAND_SIZE_PREFIX);
                }

                line.setError(checkAddressingRegister(register));

                if (getRegisterAddressMode(strings[1]) == 1) {
                    line.setOperationCode(getOperationCodes().get(command) + getRegisterAddressMode(strings[1]));
                } else {
                    line.setOperationCode(getAddOperationCodes().get(command));
                }
                line.setMrm(0xD0 | 0x8 | getRegisterNumber(strings[1]));
            } else if (Arrays.equals(rule, ONLY_MEM)) {
                String[] data = new String[]{""};

                if (isIdentifier(strings[1])) {
                    String identifierName;
                    if (INDEX_ADDRESSING) {
                        identifierName = strings[1].split("\\[")[0];
                        //generate mem for mrm and sib
                        String mem = "";
                        for (int i = 1; i < strings.length; i++) {
                            if (strings[i].contains("[")) {
                                mem += "[" + strings[i].split("\\[")[1] + " ";
                            } else {
                                mem += strings[i] + " ";
                            }
                        }
                        line = generateMrmSib(new String[]{mem}, ONLY_MEM);
                    } else {
                        if (strings.length > 2) {
                            line.setError("Extra characters on line");
                        }
                        identifierName = strings[1];
                    }
                    Integer segmentChangePrefix = getSegmentChangePrefix(identifierName);
                    if (segmentChangePrefix != null) {
                        line.setChangeSegmentPrefix(segmentChangePrefix);
                        identifierName = identifierName.replaceAll(getSegmentRegistersToRemove(), "");
                    }
                    Identifier i = getIdentifierByName(identifierName);
                    if (i != null) {
                        line.setChangeSegmentPrefix(checkIdentifierForPrefixChangeSegment(identifierName));
                        line.setAddress(i.getOffset());
                        line.setAddressSize(4);
                        if (INDEX_ADDRESSING) {
                            line.setMrm(0xB4);
                        } else {
                            line.setMrm(0xD);
                        }
                        if (i.getType().equals(getKeyWords()[10])) {
                            memoryAddressMode = 1;
                        } else if (i.getType().equals(getKeyWords()[9])) {
                            memoryAddressMode = 1;
                            line.setOperandSizePrefix(OPERAND_SIZE_PREFIX);
                        }
                    } else {
                        line.setError("Unknown identifier");
                    }
                } else {
                    //all string like : command dword/word/byte ptr mem; data[0] must contains only mem
                    for (int i = 1; i < strings.length; i++) {
                        data[0] += strings[i] + " ";
                    }

                    line = generateMrmSib(data, rule);
                    line.setChangeSegmentPrefix(getSegmentChangePrefix(data[0]));
                }
                line.setOperationCode(getOperationCodes().get(command) + memoryAddressMode);
            } else if (Arrays.equals(rule, ONLY_LABEL)) {
                if (second) {
                    String name = strings[1];
                    if (!isIdentifier(name)) {
                        line.setError("Miss identifier");
                    } else {
                        Identifier label = getIdentifierByName(name);
                        if (label == null) {
                            line.setError("Unknown label");
                        } else {
                            if (label.getAttribute().equals(findCurrentSegment().getName())) {
                                if (offset >= label.getOffset()) {
                                    line.setOperationCode(getOperationCodes().get(command));
                                    line.setAddressSize(1);
                                    line.setAddress(0xFF - offset + label.getOffset() - 1);
                                } else {
                                    if (command.equals("JMP")) {
                                        label = getIdentifierByName(name);
                                        line.setOperationCode(getOperationCodes().get(command));
                                        line.setOperand(BigInteger.valueOf(0x909090));
                                        line.setOperandSize(3);
                                        line.setAddress(label.getOffset() - offset - 2);
                                        line.setAddressSize(1);
                                    } else if (command.startsWith("J")) {
                                        label = getIdentifierByName(name);
                                        line.setAddress(getAddOperationCodes().get(command));
                                        line.setAddressSize(2);
                                        line.setOperand(BigInteger.valueOf(label.getOffset()));
                                        line.setOperandSize(4);
                                    }
                                }
                            } else {
                                if (command.equals("JMP")) {
                                    label = getIdentifierByName(name);
                                    line.setOperationCode(0xE9);
                                    line.setAddress(label.getOffset());
                                    line.setAddressSize(4);
                                }
                            }
                        }
                    }
                } else {
                    String name = strings[1];
                    Identifier label = getIdentifierByName(name);
                    if (label == null) {
                        if (command.equals("JMP")) {
                            offset += 6;
                        } else if (command.startsWith("J")) {
                            offset += 7;
                        }
                    }
                }
            } else if (Arrays.equals(rule, EMPTY)) {
                line.setOperationCode(getOperationCodes().get(command));
            }
        }

        return line;
    }

    //assume, end
    //parse assume for prefix change segments
    private Line parseKeyWord(String[] strings) {
        Line line = new Line();

        String firstWord = strings[0];
        if (getKeyWords()[13].equals(firstWord)) {
            //assume cs:code, ds:data
            if (!second) {
                return line;
            }
            for (int index = 1; index < strings.length; index++) {
                String[] s = removeEmptyAndChars(strings[index].split(":"), "");
                Identifier i = getIdentifierByName(s[1]);
                if (!isSegmentRegister(s[0])) {
                    line.setError("Miss segment register");
                    return line;
                } else if (i == null) {
                    line.setError("Unknown identifier");
                    return line;
                }
                i.setSegmentRegister(s[0]);
            }
        } else if (getKeyWords()[2].equals(firstWord)) {
            //end
            if (strings.length > 1 && getIdentifierByName(strings[1]) == null) {
                line.setError("Statement is not identifier or unknown identifier");
            }
            closedProgram = true;
        } else if (getKeyWords()[17].equals(firstWord)) {
            //if
            openedIf = true;
            Identifier i = getIdentifierByName(strings[1]);
            if (i == null) {
                line.setError("Unknown identifier");
            } else {
                if (!i.getType().equals(getKeyWords()[16])) {
                    line.setError("Incorrect identifiers type");
                } else {
                    resultIf = i.getValue().compareTo(ZERO) != 0;
                }
            }

        } else if (getKeyWords()[18].equals(firstWord)) {
            //else
            //nothing
        } else if (getKeyWords()[19].equals(firstWord)) {
            //endif
            openedIf = false;
        }
        return line;
    }

    //data - eax/eax or eax/[eax] ...
    //rule - reg/reg or reg/mem
    //mem can be [eax + esi*2] or DWORD PTR [EAX + ESI]
    public Line generateMrmSib(String[] data, String[] rule) {
        Line line = new Line();
        int mod = 0;
        int reg;
        int rm = 0;
        Integer sib = null;
        if (rule.length == 2) {
            String first = data[0], second = data[1];
            if (Arrays.equals(rule, MEM_REG)) {
                //mem/reg or reg/mem; mem = [eax ...]
                if (!INDEX_ADDRESSING) {
                    line.setError(checkAddressingMemory(first));
                }
                line.setError(checkAddressingRegister(second));
                String mem = first.replaceAll(getKeyWords()[10] + "|" + getKeyWords()[9] + "|" + getKeyWords()[8] + "|" +
                        getKeyWords()[11] + "|\\[|]|" + getSegmentRegistersToRemove(), "");
                reg = getRegisterNumber(second);
                if (USE_32) {
                    if (ALLOWED_SIB) {
                        if (OFFSET) {
                            //only [sib +- 5]
                            int minus = mem.lastIndexOf('-');
                            if (minus != -1) {
                                sib = generateSib(mem.substring(0, minus).trim());
                                BigInteger operand = parseInt(mem.substring(minus + 1, mem.length()).trim(), 4, false);
                                if (operand.compareTo(BYTES_1) == 1) {
                                    mod = 2;
                                    line.setOperandSize(4);
                                    if (!BASE) {
                                        mod = 0;
                                    }
                                } else {
                                    mod = 1;
                                    if (!BASE) {
                                        mod = 0;
                                        line.setOperandSize(4);
                                    } else {
                                        line.setOperandSize(1);
                                    }
                                }

                                line.setOperand((line.getOperandSize() == 4) ? BYTES_4.subtract(operand).add(ONE) : BYTES_1.subtract(operand).add(ONE));
                            } else {
                                int plus = mem.lastIndexOf('+');
                                if (plus != -1) {
                                    sib = generateSib(mem.substring(0, plus).trim());
                                    BigInteger operand = parseInt(mem.substring(plus + 1, mem.length()).trim(), 4, false);
                                    if (operand.compareTo(BYTES_1) == 1) {
                                        mod = 2;
                                        line.setOperandSize(4);
                                        if (!BASE) {
                                            mod = 0;
                                        }
                                    } else {
                                        mod = 1;
                                        if (!BASE) {
                                            mod = 0;
                                            line.setOperandSize(4);
                                        } else {
                                            line.setOperandSize(1);
                                        }
                                    }
                                    line.setOperand(operand);
                                } else {
                                    return null;
                                }
                            }
                        } else {
                            sib = generateSib(mem);
                        }
                        rm = 4;
                    } else {
                        if (OFFSET) {
                            String[] strings = mem.split("\\+");
                            if (strings.length == 1) {
                                strings = mem.split("-");
                                if (strings.length == 1) {
                                    return null;
                                } else {
                                    //eax - 5
                                    BigInteger operand = parseInt(strings[1].trim(), 4, false);
                                    if (operand.compareTo(BYTES_1) == 1) {
                                        line.setOperandSize(4);
                                        mod = 2;
                                        operand = BYTES_4.subtract(operand).add(ONE);

                                    } else {
                                        line.setOperandSize(1);
                                        mod = 1;
                                        operand = BYTES_1.subtract(operand).add(ONE);
                                    }
                                    rm = getRegisterNumber(strings[0].trim());
                                    line.setOperand(operand);
                                }
                            } else {
                                //eax + 5 or 5 + eax
                                rm = getRegisterNumber(strings[0].trim());
                                if (rm == -1) {
                                    rm = getRegisterNumber(strings[1].trim());
                                    BigInteger operand = parseInt(strings[0].trim(), (strings[0].trim().length() > 4) ? 4 : 1, false);

                                    if (operand.compareTo(BYTES_1) == 1) {
                                        mod = 2;
                                        line.setOperandSize(4);
                                    } else {
                                        mod = 1;
                                        line.setOperandSize(1);
                                    }
                                    line.setOperand(operand);
                                } else {
                                    BigInteger operand = parseInt(strings[1].trim(), 4, false);
                                    if (operand.compareTo(BYTES_1) == 1) {
                                        line.setOperandSize(4);
                                        mod = 2;
                                    } else {
                                        line.setOperandSize(1);
                                        mod = 1;
                                    }
                                    line.setOperand(operand);
                                }

                            }

                        } else {
                            mod = 0;
                            rm = getRegisterNumber(mem);
                        }
                    }

                } else if (USE_16) {
                    // if using 16-bit addressing
                    String[] strings = removeEmptyAndChars(split(mem, " [+*]", true), "");
                    line.setAddressSizePrefix(ADDRESS_SIZE_PREFIX);
                    int i = 0;
                    if (OFFSET) {
                        //only mem with offset;
                        //must be like bp + 1 or bx + si + 1
                        int sum = getRegisterNumber(strings[i++]);
                        if (strings[i++].equals("+")) {
                            if (sum == 3 || sum == 5 || sum == 6 || sum == 7) {
                                BigInteger operand = parseInt(strings[i].trim(), 2, false);
                                if (operand.compareTo(BYTES_1) == 1) {
                                    line.setOperandSize(2);
                                    mod = 2;
                                } else {
                                    line.setOperandSize(1);
                                    mod = 1;
                                }
                                line.setOperand(operand);
                                rm = sum;
                            } else {
                                sum += getRegisterNumber(strings[i++]);
                                //BX + SI OR BX + DI OR BP + SI OR BP + DI
                                if (sum >= 9 && sum <= 12) {
                                    rm = sum - 9;
                                } else {
                                    return null;
                                }
                                if (strings[i++].equals("+")) {
                                    BigInteger operand = parseInt(strings[i].trim(), 2, false);
                                    if (operand.compareTo(BYTES_1) == 1) {
                                        line.setOperandSize(2);
                                        mod = 2;
                                    } else {
                                        line.setOperandSize(1);
                                        mod = 1;
                                    }
                                    line.setOperand(operand);

                                } else {
                                    //doesn't contains offset
                                    return null;
                                }
                            }
                        } else {
                            //doesn't contains offset
                            return null;
                        }
                    } else {
                        //only mem without offset
                        int sum = getRegisterNumber(strings[i++]);
                        if (strings[i++].equals("+")) {
                            sum += getRegisterNumber(strings[i]);
                            //BX + SI OR BX + DI OR BP + SI OR BP + DI
                            if (sum >= 9 && sum <= 12) {
                                rm = sum - 9;
                            } else {
                                return null;
                            }
                        } else {
                            //SI OR DI OR BP OR BX
                            if (sum == 3 || sum == 5 || sum == 6 || sum == 7) {
                                rm = sum;
                            } else {
                                return null;
                            }
                        }
                    }
                }
            } else if (Arrays.equals(rule, REG_MEM)) {
                //mem/reg or reg/mem; mem = [eax ...]
                line.setError(checkAddressingRegister(first));
                String mem = second.replaceAll(getKeyWords()[10] + "|" + getKeyWords()[9] + "|" + getKeyWords()[8] + "|" +
                        getKeyWords()[11] + "|\\[|]|" + getSegmentRegistersToRemove(), "");
                reg = getRegisterNumber(first);
                if (USE_32) {
                    if (ALLOWED_SIB) {
                        if (OFFSET) {
                            //only [sib +- 5]
                            int minus = mem.lastIndexOf('-');
                            if (minus != -1) {
                                sib = generateSib(mem.substring(0, minus).trim());
                                BigInteger operand = parseInt(mem.substring(minus + 1, mem.length()).trim(), 4, false);
                                if (operand.compareTo(BYTES_1) == 1) {
                                    mod = 2;
                                    line.setOperandSize(4);
                                    if (!BASE) {
                                        mod = 0;
                                    }
                                } else {
                                    mod = 1;
                                    if (!BASE) {
                                        mod = 0;
                                        line.setOperandSize(4);
                                    } else {
                                        line.setOperandSize(1);
                                    }
                                }

                                line.setOperand((line.getOperandSize() == 4) ? BYTES_4.subtract(operand).add(ONE) : BYTES_1.subtract(operand).add(ONE));
                            } else {
                                int plus = mem.lastIndexOf('+');
                                if (plus != -1) {
                                    sib = generateSib(mem.substring(0, plus).trim());
                                    BigInteger operand = parseInt(mem.substring(plus + 1, mem.length()).trim(), 4, false);
                                    if (operand.compareTo(BYTES_1) == 1) {
                                        mod = 2;
                                        line.setOperandSize(4);
                                        if (!BASE) {
                                            mod = 0;
                                        }
                                    } else {
                                        mod = 1;
                                        if (!BASE) {
                                            mod = 0;
                                            line.setOperandSize(4);
                                        } else {
                                            line.setOperandSize(1);
                                        }
                                    }
                                    line.setOperand(operand);
                                } else {
                                    return null;
                                }
                            }

                        } else {
                            sib = generateSib(mem);
                        }
                        rm = 4;
                    } else {
                        if (OFFSET) {
                            String[] strings = mem.split("\\+");
                            if (strings.length == 1) {
                                strings = mem.split("-");
                                if (strings.length == 1) {
                                    return null;
                                } else {
                                    //eax - 5
                                    BigInteger operand = parseInt(strings[1].trim(), 4, false);
                                    if (operand.compareTo(BYTES_1) == 1) {
                                        line.setOperandSize(4);
                                        mod = 2;
                                        operand = BYTES_4.subtract(operand).add(ONE);

                                    } else {
                                        line.setOperandSize(1);
                                        mod = 1;
                                        operand = BYTES_1.subtract(operand).add(ONE);
                                    }
                                    rm = getRegisterNumber(strings[0].trim());
                                    line.setOperand(operand);
                                }
                            } else {
                                //eax + 5 or 5 + eax
                                rm = getRegisterNumber(strings[0].trim());
                                if (rm == -1) {
                                    rm = getRegisterNumber(strings[1].trim());
                                    BigInteger operand = parseInt(strings[0].trim(), (strings[0].trim().length() > 4) ? 4 : 1, false);

                                    if (operand.compareTo(BYTES_1) == 1) {
                                        mod = 2;
                                        line.setOperandSize(4);
                                    } else {
                                        mod = 1;
                                        line.setOperandSize(1);
                                    }
                                    line.setOperand(operand);
                                } else {
                                    BigInteger operand = parseInt(strings[1].trim(), 4, false);
                                    if (operand.compareTo(BYTES_1) == 1) {
                                        line.setOperandSize(4);
                                        mod = 2;
                                    } else {
                                        line.setOperandSize(1);
                                        mod = 1;
                                    }
                                    line.setOperand(operand);
                                }

                            }

                        } else {
                            mod = 0;
                            rm = getRegisterNumber(mem);
                        }
                    }

                } else if (USE_16) {
                    // if using 16-bit addressing
                    String[] strings = removeEmptyAndChars(split(mem, " [+*]", true), "");
                    line.setAddressSizePrefix(ADDRESS_SIZE_PREFIX);
                    int i = 0;
                    if (OFFSET) {
                        //only mem with offset;
                        //must be like bp + 1 or bx + si + 1
                        int sum = getRegisterNumber(strings[i++]);
                        if (strings[i++].equals("+")) {
                            if (sum == 3 || sum == 5 || sum == 6 || sum == 7) {
                                BigInteger operand = parseInt(strings[i].trim(), 2, false);
                                if (operand.compareTo(BYTES_1) == 1) {
                                    line.setOperandSize(2);
                                    mod = 2;
                                } else {
                                    line.setOperandSize(1);
                                    mod = 1;
                                }
                                line.setOperand(operand);
                                rm = sum;
                            } else {
                                sum += getRegisterNumber(strings[i++]);
                                //BX + SI OR BX + DI OR BP + SI OR BP + DI
                                if (sum >= 9 && sum <= 12) {
                                    rm = sum - 9;
                                } else {
                                    return null;
                                }
                                if (strings[i++].equals("+")) {
                                    BigInteger operand = parseInt(strings[i].trim(), 2, false);
                                    if (operand.compareTo(BYTES_1) == 1) {
                                        line.setOperandSize(2);
                                        mod = 2;
                                    } else {
                                        line.setOperandSize(1);
                                        mod = 1;
                                    }
                                    line.setOperand(operand);

                                } else {
                                    //doesn't contains offset
                                    return null;
                                }
                            }
                        } else {
                            //doesn't contains offset
                            return null;
                        }
                    } else {
                        //only mem without offset
                        int sum = getRegisterNumber(strings[i++]);
                        if (strings[i++].equals("+")) {
                            sum += getRegisterNumber(strings[i]);
                            //BX + SI OR BX + DI OR BP + SI OR BP + DI
                            if (sum >= 9 && sum <= 12) {
                                rm = sum - 9;
                            } else {
                                return null;
                            }
                        } else {
                            //SI OR DI OR BP OR BX
                            if (sum == 3 || sum == 5 || sum == 6 || sum == 7) {
                                rm = sum;
                            } else {
                                return null;
                            }
                        }
                    }
                }

            } else if (Arrays.equals(rule, REG_REG)) {
                line.setError(checkAddressingRegister(first));
                line.setError(checkAddressingRegister(second));
                mod = 3;
                reg = getRegisterNumber(first);
                rm = getRegisterNumber(second);
                if (rm == -1) {
                    line.setError("Incorrect register");
                }
            } else {
                return null;
            }
        } else if (rule.length == 1) {
            if (!INDEX_ADDRESSING) {
                line.setError(checkAddressingMemory(data[0]));
            }
            String mem = data[0].replaceAll(getKeyWords()[10] + "|" + getKeyWords()[9] + "|" + getKeyWords()[8] + "|" +
                    getKeyWords()[11] + "|\\[|]|" + getSegmentRegistersToRemove(), "");

            reg = 1;
            if (USE_32) {
                if (ALLOWED_SIB) {
                    if (OFFSET) {
                        //only [sib +- 5]
                        int minus = mem.lastIndexOf('-');
                        if (minus != -1) {
                            sib = generateSib(mem.substring(0, minus).trim());
                            BigInteger operand = parseInt(mem.substring(minus + 1, mem.length()).trim(), 4, false);
                            if (operand.compareTo(BYTES_1) == 1) {
                                mod = 2;
                                line.setOperandSize(4);
                                if (!BASE) {
                                    mod = 0;
                                }
                            } else {
                                mod = 1;
                                if (!BASE) {
                                    mod = 0;
                                    line.setOperandSize(4);
                                } else {
                                    line.setOperandSize(1);
                                }
                            }

                            line.setOperand((line.getOperandSize() == 4) ? BYTES_4.subtract(operand).add(ONE) : BYTES_1.subtract(operand).add(ONE));
                        } else {
                            int plus = mem.lastIndexOf('+');
                            if (plus != -1) {
                                sib = generateSib(mem.substring(0, plus).trim());
                                BigInteger operand = parseInt(mem.substring(plus + 1, mem.length()).trim(), 4, false);
                                if (operand.compareTo(BYTES_1) == 1) {
                                    mod = 2;
                                    line.setOperandSize(4);
                                    if (!BASE) {
                                        mod = 0;
                                    }
                                } else {
                                    mod = 1;
                                    if (!BASE) {
                                        mod = 0;
                                        line.setOperandSize(4);
                                    } else {
                                        line.setOperandSize(1);
                                    }
                                }
                                line.setOperand(operand);
                            } else {
                                return null;
                            }
                        }
                    } else {
                        sib = generateSib(mem);
                    }
                    rm = 4;
                } else {
                    if (OFFSET) {
                        String[] strings = mem.split("\\+");
                        if (strings.length == 1) {
                            strings = mem.split("-");
                            if (strings.length == 1) {
                                return null;
                            } else {
                                //eax - 5
                                BigInteger operand = parseInt(strings[1].trim(), 4, false);
                                if (operand.compareTo(BYTES_1) == 1) {
                                    line.setOperandSize(4);
                                    mod = 2;
                                    operand = BYTES_4.subtract(operand).add(ONE);

                                } else {
                                    line.setOperandSize(1);
                                    mod = 1;
                                    operand = BYTES_1.subtract(operand).add(ONE);
                                }
                                rm = getRegisterNumber(strings[0].trim());
                                line.setOperand(operand);
                            }
                        } else {
                            //eax + 5 or 5 + eax
                            rm = getRegisterNumber(strings[0].trim());
                            if (rm == -1) {
                                rm = getRegisterNumber(strings[1].trim());
                                BigInteger operand = parseInt(strings[0].trim(), (strings[0].trim().length() > 4) ? 4 : 1, false);

                                if (operand.compareTo(BYTES_1) == 1) {
                                    mod = 2;
                                    line.setOperandSize(4);
                                } else {
                                    mod = 1;
                                    line.setOperandSize(1);
                                }
                                line.setOperand(operand);
                            } else {
                                BigInteger operand = parseInt(strings[1].trim(), 4, false);
                                if (operand.compareTo(BYTES_1) == 1) {
                                    line.setOperandSize(4);
                                    mod = 2;
                                } else {
                                    line.setOperandSize(1);
                                    mod = 1;
                                }
                                line.setOperand(operand);
                            }

                        }

                    } else {
                        mod = 0;
                        rm = getRegisterNumber(mem);
                    }
                }

            } else if (USE_16) {
                // if using 16-bit addressing
                String[] strings = removeEmptyAndChars(split(mem, " [+*]", true), "");
                line.setAddressSizePrefix(ADDRESS_SIZE_PREFIX);
                int i = 0;
                if (OFFSET) {
                    //only mem with offset;
                    //must be like bp + 1 or bx + si + 1
                    int sum = getRegisterNumber(strings[i++]);
                    if (strings[i++].equals("+")) {
                        if (sum == 3 || sum == 5 || sum == 6 || sum == 7) {
                            BigInteger operand = parseInt(strings[i].trim(), 2, false);
                            if (operand.compareTo(BYTES_1) == 1) {
                                line.setOperandSize(2);
                                mod = 2;
                            } else {
                                line.setOperandSize(1);
                                mod = 1;
                            }
                            line.setOperand(operand);
                            rm = sum;
                        } else {
                            sum += getRegisterNumber(strings[i++]);
                            //BX + SI OR BX + DI OR BP + SI OR BP + DI
                            if (sum >= 9 && sum <= 12) {
                                rm = sum - 9;
                            } else {
                                return null;
                            }
                            if (strings[i++].equals("+")) {
                                BigInteger operand = parseInt(strings[i].trim(), 2, false);
                                if (operand.compareTo(BYTES_1) == 1) {
                                    line.setOperandSize(2);
                                    mod = 2;
                                } else {
                                    line.setOperandSize(1);
                                    mod = 1;
                                }
                                line.setOperand(operand);

                            } else {
                                //doesn't contains offset
                                return null;
                            }
                        }
                    } else {
                        //doesn't contains offset
                        return null;
                    }
                } else {
                    //only mem without offset
                    int sum = getRegisterNumber(strings[i++]);
                    if (strings[i++].equals("+")) {
                        sum += getRegisterNumber(strings[i]);
                        //BX + SI OR BX + DI OR BP + SI OR BP + DI
                        if (sum >= 9 && sum <= 12) {
                            rm = sum - 9;
                        } else {
                            return null;
                        }
                    } else {
                        //SI OR DI OR BP OR BX
                        if (sum == 3 || sum == 5 || sum == 6 || sum == 7) {
                            rm = sum;
                        } else {
                            return null;
                        }
                    }
                }
            }
        } else {
            return null;
        }
        if (reg == -1) {
            line.setError("Incorrect register");
        }

        line.setMrm((mod << 6) | (reg << 3) | rm);
        line.setSib(sib);
        if (USE_32 && ALLOWED_SIB && sib == null && (Arrays.equals(rule, ONLY_MEM) || Arrays.equals(rule, REG_MEM) || Arrays.equals(rule, MEM_REG))) {
            //line.setError("Incorrect memory");
        }
        return line;
    }

    //sib: scale * index + base; scale = 1,2,4,8; index = EAX, ECX ... ; base = EAX, ECX ...
    //input like 2 * ecx + eax or ecx * 4 + ebx
    //minimum view like ecx * 2 or ecx + eax
    private Integer generateSib(String mem) {
        int scale = 0;
        int index;
        int base;

        if (!ALLOWED_SIB || !isCorrectSib(mem)) {
            return null;
        }

        //split input mem for + and * symbols
        StringTokenizer tokenizer = new StringTokenizer(mem, "+*", true);
        String[] strings = new String[tokenizer.countTokens()];

        int i = 0;
        while (tokenizer.hasMoreTokens()) {
            strings[i++] = tokenizer.nextToken().trim();
        }

        String indexS;
        String scaleS = null;
        String baseS = null;
        i = 0;
        //start check for any sib variants
        if (isRegister(indexS = strings[i++])
                && (!SCALE || strings[i++].equals("*") && isCorrectScale(scaleS = strings[i++]))
                && (!BASE || strings[i++].equals("+") && isRegister(baseS = strings[i]))) {
            //index (* scale) (+ base)
            if (SCALE) {
                scale = Integer.parseInt(scaleS);
            }
            index = getRegisterNumber(indexS);
            if (BASE) {
                base = getRegisterNumber(baseS);
            } else {
                base = 5;
            }
        } else {
            i = 0;
            if ((!SCALE || isCorrectScale(scaleS = strings[i++]) && strings[i++].equals("*")) &&
                    isRegister(indexS = strings[i++])
                    && (!BASE || strings[i++].equals("+") && isRegister(baseS = strings[i]))) {
                //(scale *) index (+ base)
                if (SCALE) {
                    scale = Integer.parseInt(scaleS);
                }
                index = getRegisterNumber(indexS);
                if (BASE) {
                    base = getRegisterNumber(baseS);
                } else {
                    base = 5;
                }
            } else {
                i = 0;
                if ((!BASE || isRegister(baseS = strings[i++]) && strings[i++].equals("+")) &&
                        (!SCALE || isCorrectScale(scaleS = strings[i++]) && strings[i++].equals("*")) &&
                        isRegister(indexS = strings[i])) {
                    //(base +)(scale *) index
                    if (SCALE) {
                        scale = Integer.parseInt(scaleS);
                    }
                    index = getRegisterNumber(indexS);
                    if (BASE) {
                        base = getRegisterNumber(baseS);
                    } else {
                        base = 5;
                    }
                } else {
                    i = 0;
                    if ((!BASE || isRegister(baseS = strings[i++]) && strings[i++].equals("+")) &&
                            isRegister(indexS = strings[i++]) &&
                            (!SCALE || strings[i++].equals("*") && isCorrectScale(scaleS = strings[i]))) {
                        //(base +) index (* scale)
                        if (SCALE) {
                            scale = Integer.parseInt(scaleS);
                        }
                        index = getRegisterNumber(indexS);
                        if (BASE) {
                            base = getRegisterNumber(baseS);
                        } else {
                            base = 5;
                        }
                    } else {
                        return null;
                    }
                }
            }
        }
        if (!SCALE && BASE) {
            i = index;
            index = base;
            base = i;
        }

        //scale can be 2, 4, 8; s in sib is a pow of 2
        scale = powOf2(scale);
        if (BASE && base == 5 || index == 4) {
            return null;
        }
        return (scale << 6) | (index << 3) | base;
    }

    //return error string if segment is not opened
    //or redefined identifier
    private String setIdentifier(String name, String type, BigInteger value) {
        if (getKeyWords()[0].equals(type)) {
            return setSegment(name, type);
        }
        Identifier identifier = findCurrentSegment();
        if (identifier == null) {
            return "Defined variable before segment opening";
        }

        if (second) {
            for (Identifier i : identifiers) {
                if (i.getName().equals(name) && i.getType().equals("ERROR")) {
                    return "Redefined identifier";
                }
            }
        } else {
            Identifier i = getIdentifierByName(name);
            if (i != null) {
                identifiers.add(new Identifier(name, "ERROR", offset, identifier.getName()));
                return "Redefined identifier";
            }
            if (getKeyWords()[16].equals(type)) {
                identifiers.add(new Identifier(name, type, offset, value, identifier.getName()));
            } else {
                identifiers.add(new Identifier(name, type, offset, identifier.getName()));
            }
        }

        return null;
    }

    private String setSegment(String name, String type) {
        Identifier identifier = getIdentifierByName(name);
        if (!second && identifier != null) {
            identifiers.add(new Identifier(name, "ERROR", offset, 0, false));
            return "Redefined identifier";
        }
        if (!second) {
            identifiers.add(new Identifier(name, type, offset, 0, false));
        } else {
            identifier.setClosed(false);
        }
        return null;
    }

    private String closeSegment() {
        Identifier i = findCurrentSegment();
        if (i != null) {
            i.setLength(offset - i.getOffset());
            i.setClosed(true);
        } else {
            return "Segment not opened";
        }
        return null;
    }

    private void clearSegments() {
        for (Identifier i : identifiers) {
            if (i.getType().equals(getKeyWords()[0])) {
                i.setClosed(null);
            }
        }
    }

    private Integer checkIdentifierForPrefixChangeSegment(String identifierName) {
        Identifier i = getIdentifierByName(identifierName);
        Identifier segment = findCurrentSegment();
        if (i == null || segment == null || !PREFIX_SEGMENT_OBVIOUSLY) {
            return null;
        }
        Identifier identifiersSegment = getIdentifierByName(i.getAttribute());
        if (!"DS".equals(identifiersSegment.getSegmentRegister())) {
            return getSegmentRegisters().get(identifiersSegment.getSegmentRegister());
        }
        return null;
    }

    //check for correct register addressing;
    //if 32-bit addressing using => ax - error
    //if 16-bit addressing using => eax - error
    //if register is correct for addressing => return null
    private String checkAddressingRegister(String register) {
        int registerNumber = getRegisterPos(register);
        /*if (USE_32 && registerNumber >= 8 && registerNumber <= 15) {
            //32-bit addressing, but used 16-bit register; ax,cx ...
            return "16-bit data used";
        } else if (USE_16 && registerNumber >= 0 && registerNumber <= 7) {
            //16-bit addressing, but used 32-bit register; eax,ecx ...
            return "32-bit data used";
        }*/
        return null;
    }

    //check for correct memory addressing;
    //if 32-bit addressing using => word ptr - error
    //if 16-bit addressing using => dword ptr - error
    //if memory is correct for addressing => return null
    private String checkAddressingMemory(String[] memory) {
        if (getKeyWords()[10].equals(memory[0])) {
            //dword                                          l
            if (!USE_32) {
                return "32-bit data used";
            }
        } else if (getKeyWords()[9].equals(memory[0])) {
            //word
            if (!USE_16) {
                return "16-bit data used";
            }
        } else if (getKeyWords()[8].equals(memory[0])) {
            //byte
        } else {
            return "Type not defined";
        }
        if (!getKeyWords()[11].equals(memory[1])) {
            return "PTR miss";
        }
        /*if (USE_32 && contains16BitRegisters(memory)) {
            return "16-bit data used";
        } else if (USE_16 && contains32BitRegisters(memory)) {
            return "32-bit data used";
        }*/
        if (!containsChar(memory, "[") || !containsChar(memory, "]")) {
            return "Miss correct brackets";
        }

        return null;
    }


    private String checkAddressingMemory(String memory) {
        return checkAddressingMemory(removeEmptyAndChars(memory.split(",| "), ""));
    }

    //check for constant types
    //return error if exist
    private String checkConstant(String s) {
        int type = isConstant(s);
        if (type == 2 && !BINARY_CONSTANTS) {
            return "Binary constants not allowed";
        } else if (type == 10 && !DECIMAL_CONSTANTS) {
            return "Decimal constants not allowed";
        } else if (type == 16 && !HEX_CONSTANTS) {
            return "Decimal constants not allowed";
        } else if (type == 0) {
            return "Miss numeric constant";
        }
        return null;
    }

    //input like EAX*2 + ESI
    //return true if correct
    private boolean isCorrectSib(String mem) {
        if (!ALLOWED_SIB) {
            return false;
        }
        if (BASE && SCALE) {
            return mem.split("\\*|\\+").length == 3;
        } else if (BASE) {
            return mem.split("\\+").length == 2;
        } else if (SCALE) {
            return mem.split("\\*").length == 2;
        }
        return true;
    }

    private Integer getSegmentChangePrefix(String s) {
        for (Map.Entry<String, Integer> e : getSegmentRegisters().entrySet()) {
            if (s.contains(e.getKey() + ":")) {
                return e.getValue();
            }
        }
        return null;
    }

    private Identifier findCurrentSegment() {
        for (Identifier i : identifiers) {
            if (i.getClosed() != null && !i.getClosed()) {
                return i;
            }
        }
        return null;
    }

    private void incIdentifierOffset(Integer offset, Integer offsetAdd) {
        int index = -1;
        for (int i = 0; i < identifiers.size(); i++) {
            if (identifiers.get(i).getOffset() > offset) {
                index = i;
            }
        }
        if (index != -1) {
            for (int i = index; i < identifiers.size(); i++) {
                identifiers.get(i).setOffset(identifiers.get(i).getOffset() + offsetAdd);
            }
        }
    }

    private Identifier getIdentifierByName(String name) {
        for (Identifier i : identifiers) {
            if (i.getName().equals(name)) {
                return i;
            }
        }
        return null;
    }
}