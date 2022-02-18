package com.wingli.agent;

import javassist.*;
import org.springframework.boot.loader.LaunchedURLClassLoader;
import org.springframework.boot.loader.archive.Archive;
import org.springframework.boot.loader.archive.JarFileArchive;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;
import java.util.zip.ZipEntry;

public class Transformer implements ClassFileTransformer {
    private Instrumentation inst;

    public Transformer(Instrumentation inst) {
        this.inst = inst;
    }

    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
        if (className == null) return classfileBuffer;
        try{
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
                //将 agent jar 中的 jar in jar 添加到类搜索路径，必须先执行这个，否则无法找到helper模块中的类
                appendAgentNestedJars(loader);
                //上报使用情况
                //不能这样使用，是因为 Transformer 类是由AppClassLoader加载的，所以它的依赖也会由AppClassLoader加载，即使contextLoader是LaunchedURLClassLoader
//              UsageStatHelper.reportUsage();
                //只能反射调用
                Class usageStatHelper = loader.loadClass("com.wingli.agent.helper.UsageStatHelper");
                Method reportUsage = usageStatHelper.getDeclaredMethod("reportUsage");
                reportUsage.invoke(usageStatHelper);
            }
            //---------------------- 其它处理逻辑 end --------------------------
        }catch (Throwable t){
            t.printStackTrace();
        }
        return classfileBuffer;
    }

    /**
     * 获取 agent jar 的路径，抄springboot的
     */
    private String getAgentJarPath() {
        ProtectionDomain protectionDomain = getClass().getProtectionDomain();
        CodeSource codeSource = protectionDomain.getCodeSource();
        URI location = null;
        try {
            location = (codeSource == null ? null : codeSource.getLocation().toURI());
        } catch (URISyntaxException e) {
            return null;
        }
        String path = (location == null ? null : location.getSchemeSpecificPart());
        if (path == null) {
            throw new IllegalStateException("Unable to determine code source");
        }
        File root = new File(path);
        if (!root.exists()) {
            throw new IllegalStateException(
                    "Unable to determine code source from " + root);
        }
        return path;
    }

    private void appendAgentNestedJars(ClassLoader classLoader) {
        String agentJarPath = getAgentJarPath();
        if (agentJarPath == null) return;

        //LaunchedURLClassLoader 是属于 springboot-loader 的类，没有放到jar in jar里边，所以它是被AppClassLoader加载的
        if (classLoader instanceof LaunchedURLClassLoader) {
            LaunchedURLClassLoader launchedURLClassLoader = (LaunchedURLClassLoader) classLoader;
            try {
                Method method = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
                method.setAccessible(true);
                //遍历 agent jar，处理所有对应目录下的jar包，使用 JarFileArchive 获取到的url才可以处理jar in jar
                JarFileArchive jarFileArchive = new JarFileArchive(new File(agentJarPath));
                List<Archive> archiveList = jarFileArchive.getNestedArchives(new Archive.EntryFilter() {
                    @Override
                    public boolean matches(Archive.Entry entry) {
                        if (entry.isDirectory()) {
                            return false;
                        }
                        return entry.getName().startsWith("BOOT-INF/lib/") && entry.getName().endsWith(".jar");
                    }
                });
                for (Archive archive : archiveList) {
                    method.invoke(launchedURLClassLoader, archive.getUrl());
                    System.out.println("add url to classloader. url:" + archive.getUrl());
                }
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }


        System.out.println("trigger add urls to classLoader:" + classLoader.getClass().getName() + " agentJarPath:" + agentJarPath);

    }
}
