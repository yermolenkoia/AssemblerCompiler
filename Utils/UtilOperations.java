package com.sp.Utils;


import com.sp.model.Identifier;
import com.sp.model.Line;

import java.io.*;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import static com.sp.Utils.Constants.*;

public class UtilOperations {

    private UtilOperations() {
    }

    //get the register number
    //EAX - 0. ECX - 1 ...
    //if not found element - returns -1
    public static int getRegisterNumber(String reg) {
        int regPos = -1;
        for (int i = 0, registersLength = getRegisters().length; i < registersLength; i++) {
            if (getRegisters()[i].equals(reg)) {
                regPos = i;
            }
        }
        while (regPos > 7) {
            regPos -= 8;
        }
        return regPos;
    }

    //get regisret position in registers array
    //eax - 0, ax - 8 ...
    public static int getRegisterPos(String reg) {
        int regPos = -1;
        for (int i = 0, registersLength = getRegisters().length; i < registersLength; i++) {
            if (getRegisters()[i].equals(reg)) {
                regPos = i;
            }
        }
        return regPos;
    }

    public static boolean containsKeyWord(String keyWord) {
        for (String s : Constants.getKeyWords()) {
            if (s.equals(keyWord)) {
                return true;
            }
        }
        return false;
    }

    public static boolean containsCommand(String command) {
        return Constants.getCommands().containsKey(command);
    }

    public static boolean containsSegmentRegister(String segRegister) {
        return getSegmentRegisters().containsKey(segRegister);
    }

    //returns CS|SS|DS ...
    public static String getSegmentRegistersToRemove() {
        String s = "";
        for (Map.Entry<String, Integer> e : getSegmentRegisters().entrySet()) {
            s += "|" + e.getKey() + ":";
        }
        return s.substring(1);
    }

    //removes string like "" - empty
    public static String[] removeEmptyAndChars(String[] array, String chars) {
        List<String> strings = new ArrayList<>();
        for (String s : array) {
            if (s != null && !s.isEmpty() && !s.equals(" ") && !chars.contains(s)) {
                strings.add(s.trim());
            }
        }
        return strings.toArray(new String[strings.size()]);
    }

    public static String[] split(String src, String delimiters, boolean include) {
        StringTokenizer tokenizer = new StringTokenizer(src, delimiters, include);
        String[] result = new String[tokenizer.countTokens()];
        int i = 0;
        while (tokenizer.hasMoreTokens()) {
            result[i++] = tokenizer.nextToken();
        }

        return removeEmptyAndChars(result, "");
    }

    public static int powOf2(int number) {
        int i = 0;
        while ((number = number >> 1) > 0) {
            i++;
        }
        return i;
    }

    //return 1 if 32 or 16
    //return 0 if 8
    public static int getRegisterAddressMode(String reg) {
        int registerNumber = -1;
        for (int i = 0; i < getRegisters().length; i++) {
            if (getRegisters()[i].equals(reg)) {
                registerNumber = i;
                break;
            }
        }
        if (registerNumber >= 0 && registerNumber <= 7 || registerNumber >= 8 && registerNumber <= 15) {
            return 1;
        }
        return 0;
    }

    public static boolean isKeyWord(String s) {
        return containsKeyWord(s);
    }

    public static boolean isCommand(String s) {
        return containsCommand(s);
    }

    public static boolean isRegister(String s) {
        return getRegisterNumber(s) != -1 || containsSegmentRegister(s)
                || USE_MATH_PROCESSORS && isMathRegister(s);
    }

    public static boolean isSegmentRegister(String s) {
        return containsSegmentRegister(s);
    }

    public static boolean isMathRegister(String register) {
        for (String s : getMathRegisters()) {
            if (s.equals(register)) {
                return true;
            }
        }
        return false;
    }

    public static boolean contains16BitRegisters(String[] data) {
        for (String s : data) {
            int regPos = getRegisterPos(s.trim());
            if (regPos >= 8 && regPos <= 15) {
                return true;
            }
        }
        return false;
    }

    public static boolean contains32BitRegisters(String[] data) {
        for (String s : data) {
            int regPos = getRegisterPos(s.trim());
            if (regPos >= 0 && regPos <= 7) {
                return true;
            }
        }
        return false;
    }

    public static boolean containsChar(String[] data, String c) {
        for (String s : data) {
            if (s.trim().contains(c)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isIdentifier(String s) {
        s = s.trim();
        if (containsSegmentRegister(s)) {
            s = s.replaceAll(getSegmentRegistersToRemove(), "");
        }
        return Character.isLetter(s.charAt(0)) && !containsKeyWord(s)
                && !containsCommand(s) && !isRegister(s) && isConstant(s) == 0 && !isTextConstant(s);
    }

    //return 2 if binary constant
    //      8 if octal
    //      10 if decimal
    //      16 if hex
    public static int isConstant(String s) {
        s = s.trim().toUpperCase();
        if (s.contains(",")) {
            //float numbers contains ","
            if (FLOAT) {
                s = s.replace(",", "");
            } else {
                return 0;
            }
        }
        if (s.startsWith("-")) {
            s = s.substring(1);
        }
        if (s.endsWith("B")) {
            for (int i = 0; i < s.length() - 1; i++) {
                if (s.charAt(i) != '0' && s.charAt(i) != '1') {
                    return 0;
                }
            }
            return 2;
        } else if (s.endsWith("H")) {
            for (int i = 0; i < s.length() - 1; i++) {
                if (!(s.charAt(i) >= '0' && s.charAt(i) <= '9' || s.charAt(i) >= 'A' && s.charAt(i) <= 'F')) {
                    return 0;
                }
            }
            return 16;
        } else if (s.startsWith("0")) {
            for (int i = 1; i < s.length(); i++) {
                if (s.charAt(i) < '0' || s.charAt(i) > '7') {
                    return 0;
                }
            }
            return 8;
        } else {
            for (int i = 0; i < s.length(); i++) {
                if (s.charAt(i) < '0' || s.charAt(i) > '9') {
                    return 0;
                }
            }
            return 10;
        }
    }

    public static boolean isCorrectScale(String s) {
        return s.equals("2") || s.equals("4") || s.equals("8");
    }

    public static boolean isTextConstant(String s) {
        return s.startsWith("\'") && s.endsWith("\'") || s.startsWith("\"") && s.endsWith("\"");
    }

    //returns positive integer number
    //radix 1, 2 or 4, 8
    //examples 5 -> 5
    // -10h -> F0 with radix 2
    // -10h -> FFF0 with radix 4
    // -200h -> 00FEFFFF; reversed bytes
    public static BigInteger parseInt(String s, Integer radix, boolean reverse) {
        int constantType = isConstant(s);
        boolean moreZero = true;
        if (constantType == 0) {
            return null;
        } else {
            if (s.startsWith("-")) {
                s = s.substring(1);
                moreZero = false;
            }
            if (s.startsWith("0")) {
                s = s.substring(1);
            }
            if (constantType == 2 || constantType == 16) {
                s = s.substring(0, s.length() - 1);
            }
            BigInteger number = new BigInteger(s, constantType);
            if (moreZero) {
                if (reverse) {
                    if (radix == 4) {
                        String result = String.format("%08X", number);
                        return new BigInteger(result.substring(6, 8) + result.substring(4, 6) + result.substring(2, 4) + result.substring(0, 2), 16);
                    } else if (radix == 8) {
                        String result = String.format("%016X", (Constants.BYTES_8.subtract(number).add(Constants.ONE)));
                        return new BigInteger(result.substring(14, 16) + result.substring(12, 14) + result.substring(10, 12) + result.substring(8, 10) +
                                result.substring(6, 8) + result.substring(4, 6) + result.substring(2, 4) + result.substring(0, 2), 16);
                    }
                }
                return number;
            } else {

                if (radix == 1) {
                    return Constants.BYTES_1.subtract(number).add(Constants.ONE);
                } else if (radix == 2) {
                    return Constants.BYTES_2.subtract(number).add(Constants.ONE);
                } else if (radix == 4) {
                    String result = String.format("%08X", (Constants.BYTES_4.subtract(number).add(Constants.ONE)));
                    if (reverse) {
                        return new BigInteger(result.substring(6, 8) + result.substring(4, 6) + result.substring(2, 4) + result.substring(0, 2), 16);
                    } else {
                        return new BigInteger(result, 16);
                    }
                } else if (radix == 8) {
                    String result = String.format("%016X", (Constants.BYTES_8.subtract(number).add(Constants.ONE)));
                    if (reverse) {
                        return new BigInteger(result.substring(14, 16) + result.substring(12, 14) + result.substring(10, 12) + result.substring(8, 10) +
                                result.substring(6, 8) + result.substring(4, 6) + result.substring(2, 4) + result.substring(0, 2), 16);
                    } else {
                        return new BigInteger(result, 16);
                    }
                }
            }
        }
        return null;
    }

    //returns float number
    public static BigInteger parseFloat(String s, Integer radix, boolean reverse) {
        if (s.contains(",")) {
            return parseInt(s.substring(0, s.indexOf(',')), radix, reverse).shiftLeft(0x8 * radix).add(parseInt(s.substring(s.indexOf(',') + 1), radix, reverse));
        } else {
            return parseInt(s, radix, reverse);
        }
    }

    //can return negative integer for EQU
    public static BigInteger parseInt(String s, Integer radix, boolean reverse, boolean negative) {
        if (negative) {
            return parseInt(s, radix, reverse);
        }
        int constantType = isConstant(s);
        boolean moreZero = true;
        if (constantType == 0) {
            return null;
        } else {
            if (s.startsWith("-")) {
                s = s.substring(1);
                moreZero = false;
            }
            if (s.startsWith("0") && s.length() != 1) {
                s = s.substring(1);
            }
            if (constantType == 2 || constantType == 16) {
                s = s.substring(0, s.length() - 1);
            }
            BigInteger number = new BigInteger(s, constantType);
            if (!moreZero) {
                number = number.negate();
            }
            if (reverse) {
                if (radix == 4) {
                    String result = String.format("%08X", number);
                    return new BigInteger(result.substring(6, 8) + result.substring(4, 6) + result.substring(2, 4) + result.substring(0, 2), 16);
                } else if (radix == 8) {
                    String result = String.format("%016X", number);
                    return new BigInteger(result.substring(14, 16) + result.substring(12, 14) + result.substring(10, 12) + result.substring(8, 10) +
                            result.substring(6, 8) + result.substring(4, 6) + result.substring(2, 4) + result.substring(0, 2), 16);
                }
            }
            return number;
        }
    }

    public static List<String> readFIle(String fileName) throws IOException {
        List<String> stringLines = new ArrayList<>();
        BufferedReader reader = new BufferedReader(new FileReader(RESOURCES_PATH + fileName + ".asm"));
        String line;
        while ((line = reader.readLine()) != null) {
            stringLines.add(line.toUpperCase());
        }
        return stringLines;
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static void writeFile(String fileName, List<Line> lines, List<Identifier> identifiers) throws IOException {
        File file = new File(RESOURCES_PATH + fileName + ".lst");
        if (!file.exists()) {
            file.createNewFile();
            file.setWritable(true);
        }
        BufferedWriter writer = new BufferedWriter(new FileWriter(file));
        int errorsCount = 0;
        for (Line l : lines) {
            if (l.getError() != null) {
                errorsCount++;
            }
            writer.write(l.toString());
            writer.newLine();
            System.out.println(l);
        }
        writer.newLine();
        System.out.println();

        writer.write(String.format("%10s|%10s", "Name", "Length"));
        writer.newLine();
        System.out.println(String.format("%10s|%10s", "Name", "Length"));
        for (Identifier i : identifiers) {
            if (i.getType().equals(getKeyWords()[0])) {
                writer.write(String.format("%10s|%10X", i.getName(), i.getLength()));
                writer.newLine();
                System.out.println(String.format("%10s|%10X", i.getName(), i.getLength()));
            }
        }
        writer.newLine();
        System.out.println();

        writer.write(String.format("%10s|%10s|%10s|%10s", "Name", "Type", "Value", "Attr"));
        System.out.println(String.format("%10s|%10s|%10s|%10s", "Name", "Type", "Value", "Attr"));
        writer.newLine();
        for (Identifier i : identifiers) {
            if (i.getType().equals(getKeyWords()[0])) {
                //nothing
            } else if (i.getType().equals(getKeyWords()[14])) {
                writer.write(String.format("%10s|%10s|%10X|%10s|%10S = %4X", i.getName(), i.getType(), i.getOffset(), i.getAttribute(), "LENGTH", i.getLength()));
                writer.newLine();
                System.out.println(String.format("%10s|%10s|%10X|%10s|%10S = %4X", i.getName(), i.getType(), i.getOffset(), i.getAttribute(), "LENGTH", i.getLength()));
            } else if (i.getType().equals(getKeyWords()[16])) {
                writer.write(String.format("%10s|%10s|%10X|%10s", i.getName(), i.getType(), i.getValue(), i.getAttribute()));
                writer.newLine();
                System.out.println(String.format("%10s|%10s|%10X|%10s", i.getName(), i.getType(), i.getValue(), i.getAttribute()));
            } else {
                writer.write(String.format("%10s|%10s|%10X|%10s", i.getName(), i.getType(), i.getOffset(), i.getAttribute()));
                writer.newLine();
                System.out.println(String.format("%10s|%10s|%10X|%10s", i.getName(), i.getType(), i.getOffset(), i.getAttribute()));
            }
        }
        writer.newLine();
        System.out.println();

        writer.write("\tErrors : " + errorsCount);
        writer.newLine();
        System.out.println("\tErrors : " + errorsCount);
        writer.close();
    }

}
