package com.sp;

import javax.script.ScriptException;
import java.io.IOException;


public class Test {

    public static void main(String... args) throws IOException, ScriptException {
        Processor processor = new Processor();
      //  processor.analyze("false");
        processor.analyze("false");
//        processor.lexemeAnalyze("true");
    }


}
