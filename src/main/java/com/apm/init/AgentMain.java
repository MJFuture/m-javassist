package com.apm.init;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.apm.collects.JdbcCommonCollects;
import com.apm.collects.SpringControllerCollects;
import com.apm.collects.SpringServiceCollects;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.LoaderClassPath;
/**
 * https://www.cnblogs.com/rickiyang/p/11368932.html   可看
 * 监听器入口方法，所有采集器注册至该对象。
 * 由该对象的transform 来传递改造后的Class byte 至 ClassLoader进行加载。
 */
public class AgentMain implements ClassFileTransformer {
    protected static AgentMain agentMain;
    private static Collect[] collects; // 采集器集合
    private Map<ClassLoader, ClassPool> classPoolMap = new ConcurrentHashMap<ClassLoader, ClassPool>();

    // 上传地址
    // 参数:
    // pro.key=
    // 访问远程服务 获取属性配置
    /**
     * mian执行后在去执行
     * @param args
     * @param inst
     */
    public static void agentmain(String args, Instrumentation inst) {
    	inst.addTransformer(new DefineTransformer(), true);
    }

    static class DefineTransformer implements ClassFileTransformer {
	    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
	        System.out.println("AgentMainTest --  premain load Class:" + className);
	        return classfileBuffer;
	    }
    }
    
    
    private static final ArrayList<String> keys;

    static {
        String paramKesy[] = {"server", "key", "secret"};
        keys = new ArrayList<String>();
        keys.addAll(Arrays.asList(paramKesy));
    }
    
    // 在应用启动前调用
    public static void premain(String agentArgs, Instrumentation inst) {
//        if (agentArgs != null) {
//            String[] paramGroup = agentArgs.split(",");
//            for (String param : paramGroup) {
//                String[] keyValue = param.split("=");
//                if (keys.contains(keyValue[0])) {
//                    System.setProperty("$bit_" + keyValue[0], keyValue[1]);
//                }
//            }
//        }
//        // 验主验置
//        if (System.getProperty("$bit_server") == null) {
//            System.setProperty("$bit_server", "http://api.ibitedu.com/receive");
//        }
//        Assert.checkNull(System.getProperty("$bit_key"),"param key is not null");
//        Assert.checkNull(System.getProperty("$bit_secret"),"param key is not null");

        //主要監控那個目標 
        collects = new Collect[]{
        		SpringServiceCollects.INSTANCE,
                JdbcCommonCollects.INSTANCE,
                SpringControllerCollects.INSTANCE
        };
        agentMain = new AgentMain();
        inst.addTransformer(agentMain);
    }
    
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
//    	System.out.println("transform --  premain load Class:" + className);
        if (className == null || loader == null
                || loader.getClass().getName().equals("sun.reflect.DelegatingClassLoader")
                || loader.getClass().getName().equals("org.apache.catalina.loader.StandardClassLoader")
                || loader.getClass().getName().equals("javax.management.remote.rmi.NoCallStackClassLoader")
                || loader.getClass().getName().equals("com.alibaba.fastjson.util.ASMClassLoader")
                || className.indexOf("$Proxy") != -1
                || className.startsWith("java")
                ) {
            return null;
        }
        
        if(className.startsWith("com/eprintServer/controller")  || className.startsWith("com/alibaba/druid/proxy/jdbc/ConnectionProxyImpl")) {
        	if (!classPoolMap.containsKey(loader)) {
        		ClassPool classPool = new ClassPool();
        		classPool.insertClassPath(new LoaderClassPath(loader));
        		classPoolMap.put(loader, classPool);
        	}
        	ClassPool cp = classPoolMap.get(loader);
        	try {
        		className = className.replaceAll("/", ".");
        		CtClass cclass = cp.get(className);
        		for (Collect c : collects) {
        			if (c.isTarget(className, loader, cclass)) { // 仅限定只能转换一次.
        				byte[] bytes = c.transform(loader, className, classfileBuffer, cclass);
        				//File f = new File("/Users/tommy/git/bit-monitoring-agent/target/" + cclass.getSimpleName() + ".class");
        				//Files.write(f.toPath(), bytes);
//        				System.out.println(String.format("%s bit APM agent insert success", className));
        				return bytes;
        			}
        		}
        	} catch (Throwable e) {
        		new Exception(String.format("%s  APM agent insert fail", className), e).printStackTrace();
        	}
        }
        return new byte[0];
    }
    
    public static void main(String[] args) {
		int i=10;System.out.println(i++);
		String str1 = "通话";
		String str2 = "重地";
		String str3 = new String("通話");
		System. out. println(String. format("str1：%d | str2：%d",  str1.hashCode(),str2.hashCode()));
		System. out. println(str1. equals(str2));
		System.out.println(Math.round(-1.5));

	}
}
