package com.apm.collects;

import com.apm.init.AbstractCollects;
import com.apm.init.AgentLoader;
import com.apm.init.Collect;
import com.apm.init.NotProguard;

import javassist.CtClass;
import javassist.CtMethod;
import javassist.Modifier;

/**
 * spring 注解服务收集
 */
@NotProguard
public class SpringServiceCollects extends AbstractCollects implements Collect {
    @NotProguard
    public static SpringServiceCollects INSTANCE = new SpringServiceCollects();

    private static final String beginSrc;
    private static final String endSrc;
    private static final String errorSrc;

    static {
        StringBuilder sbuilder = new StringBuilder();
        sbuilder.append("com.apm.collects.SpringServiceCollects instance= ");
        sbuilder.append("com.apm.collects.SpringServiceCollects.INSTANCE;\r\n");
        sbuilder.append("com.apm.AbstractCollects.Statistics statistic =instance.begin(\"%s\",\"%s\");");
        beginSrc = sbuilder.toString();
        sbuilder.setLength(0);
        sbuilder.append("instance.end(statistic);");
        endSrc = sbuilder.toString();
        sbuilder.setLength(0);
        sbuilder.append("instance.error(statistic,e);");
        errorSrc = sbuilder.toString();
    }
    /**
     * 判断是否是可采集目标
     */
    public boolean isTarget(String className, ClassLoader loader, CtClass ctclass) {
        try {
            for (Object obj : ctclass.getAnnotations()) {
                if (obj.toString().startsWith("@org.springframework.stereotype.Service")) {
                    return true;
                }
            }
        } catch (ClassNotFoundException e) {
            System.err.println(String.format("apm run error targetClassName=%s errorMessage=%s",className,e.getClass().getSimpleName()+":"+e.getMessage()));
        }
        return false;
    }

    @NotProguard
    @Override
    public Statistics begin(String className, String method) {
        ServiceStatistics serviceStatistics = new ServiceStatistics(super.begin(className, method));
        serviceStatistics.serviceName = className;
        serviceStatistics.methodName = method;
        serviceStatistics.logType="service";
        return serviceStatistics;
    }

    @Override
    public void sendStatistics(Statistics stat) {
        sendStatisticByHttp(stat,"serviceLog");
    }
    
    /**
     * 转换字节码。
     */
    public byte[] transform(ClassLoader loader, String className, byte[] classfileBuffer, CtClass ctclass) throws
            Exception {
        AgentLoader byteLoade = new AgentLoader(className, loader, ctclass);

        CtMethod[] methods = ctclass.getDeclaredMethods();
        for (CtMethod m : methods) {
            // 屏蔽非公共方法
            if (!Modifier.isPublic(m.getModifiers())) {
                continue;
            }
            // 屏蔽静态方法
            if (Modifier.isStatic(m.getModifiers())) {
                continue;
            }
            // 屏蔽本地方法
            if (Modifier.isNative(m.getModifiers())) {
                continue;
            }

            AgentLoader.MethodSrcBuild build = new AgentLoader.MethodSrcBuild();
            build.setBeginSrc(String.format(beginSrc,className,m.getName()));
            build.setEndSrc(endSrc);
            build.setErrorSrc(errorSrc);
            byteLoade.updateMethod(m, build);
        }
        return byteLoade.toBytecote();
    }

    @NotProguard
    public static class ServiceStatistics extends Statistics {
        public String serviceName; //服务名称
        public String methodName;// 方法名称
        public ServiceStatistics(Statistics s) {
            super(s);
        }
    }
    
    public static void main(String[] args) {
    	System.out.println(Math.asin(10>>4/3));
	}
}
