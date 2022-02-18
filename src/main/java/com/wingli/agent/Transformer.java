package com.wingli.agent;

import com.wingli.agent.helper.UsageStatHelper;
import javassist.*;
import java.io.ByteArrayInputStream;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;

public class Transformer implements ClassFileTransformer {
    private Instrumentation inst;

    public Transformer(Instrumentation inst) {
        this.inst = inst;
    }

    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
        if (className == null) return classfileBuffer;
        //---------------------- 具体的插桩逻辑 start ----------------------
        //匹配到对应的类
        if (className.replace("/", ".").equals("org.apache.coyote.http11.Http11InputBuffer")) {
            CtClass ctClass = null;
            CtMethod ctMethod = null;
            try {
                //构建class pool
                ClassPool classPool = new ClassPool();
                classPool.appendSystemPath();
                if (loader != null) {
                    classPool.appendClassPath(new LoaderClassPath(loader));
                }
                //匹配到对应的method
                ctClass = classPool.makeClass(new ByteArrayInputStream(classfileBuffer));
                ctMethod = ctClass.getDeclaredMethod("parseHeaders");

                //做出对应的修改,方法结束前读取并修改 headers 变量
                String finalSrc = "com.wingli.agent.helper.TokenHelper.interceptToken(headers);";
                ctMethod.insertAfter(finalSrc, true);

                //返回修改后的字节码
                return ctClass.toBytecode();
            } catch (Exception exp) {
                exp.printStackTrace();
            } finally {
                if (ctClass != null) {
                    ctClass.detach();
                }
            }
            return classfileBuffer;
        }
        //---------------------- 具体的插桩逻辑 end ------------------------

        //---------------------- 其它处理逻辑 start ------------------------
        if (className.replace("/", ".").equals("org.springframework.boot.SpringApplication")) {
            //上报使用情况
            UsageStatHelper.reportUsage();
        }
        //---------------------- 其它处理逻辑 end --------------------------

        return classfileBuffer;
    }
}
