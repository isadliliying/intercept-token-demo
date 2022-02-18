package com.wingli.agent;

import java.lang.instrument.Instrumentation;

public class Agent {

    /**
     * 入口
     */
    public static void premain(String agentArgs, Instrumentation inst) {
        Transformer transformer = new Transformer(inst);
        inst.addTransformer(transformer, true);
    }

}