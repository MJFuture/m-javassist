package com.apm.javassist;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;

import javassist.ClassMap;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.CtNewMethod;

/**
 * @description:
 */
public class PreMainTraceAgent {

    public static void premain(String agentArgs, Instrumentation inst) {
        System.out.println("agentArgs : " + agentArgs);
        inst.addTransformer(new DefineTransformer(), true);
    }

    static class DefineTransformer implements ClassFileTransformer{

        @Override
        public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
            if ("com/util/JavaSsits".equals(className)) {
            	System.out.println("premain load Class:" + className);
                try {
                    // 从ClassPool获得CtClass对象
                   /* final ClassPool classPool = ClassPool.getDefault();
                    //獲取類
                    final CtClass clazz = classPool.get("com.util.JavaSsits");
                    //獲取方法
                    CtMethod convertToAbbr = clazz.getDeclaredMethod("outpln");
                    
                    
                    CtMethod copyMethod = CtNewMethod.copy(convertToAbbr,clazz,new ClassMap());
                    convertToAbbr.setName("outpln$agent");
                    copyMethod.setBody("{\n" +
                            "    long begin = System.nanoTime();\n" +
                            "    try {\n" +
                            "        return sayHello$agent($1,$2);\n" +
                            "    } finally {\n" +
                            "        System.out.println(System.nanoTime() - begin);}\n" +
                            "    }");
                    clazz.addMethod(copyMethod);
                    // 返回字节码，并且detachCtClass对象
                    byte[] byteCode = clazz.toBytecode();
                    //detach的意思是将内存中曾经被javassist加载过的Date对象移除，如果下次有需要在内存中找不到会重新走javassist加载
                    clazz.detach();
                    return byteCode;*/
                	
                	 ClassPool pool = new ClassPool(true);
                     CtClass ctClass = pool.get("com.util.JavaSsits");
                     CtMethod ctMethod = ctClass.getDeclaredMethod("sayHello");
                     
                     CtMethod copyMethod = CtNewMethod.copy(ctMethod,ctClass,new ClassMap());
                    
//                     ctMethod.setName("sayHello$agent");
                     ctMethod.setName(copyMethod.getName()+"$agent");
                     
                     copyMethod.setBody("{\n" +
                             "    long begin = System.nanoTime();\n" +
                             "    try {\n" +
//                             "        return sayHello$agent($1,$2);\n" +
                             "   return "+copyMethod.getName()+"($$);\n" +
                             "    } finally {\n" +
                             "        System.out.println(System.nanoTime() - begin);}\n" +
                             "    }");
                     
                     ctClass.addMethod(copyMethod);
                     CtMethod ctMethod2 = ctClass.getDeclaredMethod("outpln");
                     CtMethod copyMethod2 = CtNewMethod.copy(ctMethod2,ctClass,new ClassMap());
                     ctMethod2.setName("outpln$agent");
                     copyMethod2.setBody("{\n" +
                             "    long begin = System.nanoTime();\n" +
                             "    try {\n" +
                             "       System.out.println(\"啊啊啊啊啊啊啊啊啊啊啊啊啊啊\");" +
                             "    } finally {\n" +
                             "        System.out.println(System.nanoTime() - begin);}\n" +
                             "    }");
                     ctClass.addMethod(copyMethod2);
                   System.out.println("copyMethod---------"+ ctClass.getMethods().toString());
                     return ctClass.toBytecode();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
            return null;
        }
    }
}
