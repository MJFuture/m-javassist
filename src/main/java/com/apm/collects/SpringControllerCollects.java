package com.apm.collects;

import com.apm.common.Constants;
import com.apm.init.AbstractCollects;
import com.apm.init.AgentLoader;
import com.apm.init.Collect;
import com.apm.init.NotProguard;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.Modifier;
import javassist.bytecode.ClassFile;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 对 Controller 层进行采集
 *
 */
@NotProguard
public class SpringControllerCollects extends AbstractCollects implements Collect {
    @NotProguard
    public static SpringControllerCollects INSTANCE = new SpringControllerCollects();
    private static final String beginSrc;
    private static final String endSrc;
    private static final String errorSrc;

    private String rootRequestUrl = "";


    static {
        StringBuilder sbuilder = new StringBuilder();
        sbuilder.append("com.apm.collects.SpringControllerCollects instance= ");
        sbuilder.append("com.apm.collects.SpringControllerCollects.INSTANCE;\r\n");
//        sbuilder.append("com.apm.collects.SpringControllerCollects.WebStatistics statistic =(com.apm.collects.SpringControllerCollects.WebStatistics)instance.begin(\"%s\",\"%s\");");
        sbuilder.append("com.apm.collects.SpringControllerCollects.WebStatistics statistic =(com.apm.collects.SpringControllerCollects.WebStatistics)instance.begins(\"%s\",\"%s\",\"%s\",\"%s\");");
//        sbuilder.append("statistic.urlAddress=\"%s\";");
        beginSrc = sbuilder.toString();
//        sbuilder = new StringBuilder();
        sbuilder.setLength(0);
        sbuilder.append("instance.end(statistic);");
        endSrc = sbuilder.toString();
        sbuilder.setLength(0);
//        sbuilder = new StringBuilder();
        sbuilder.append("instance.error(statistic,e);"); //父类的方法
        errorSrc = sbuilder.toString();
    }

    /**
     * 判断是否是采集对象
     */
    public boolean isTarget(String className, ClassLoader loader, CtClass ctclass) {
        boolean result = false;
        try {
            for (Object obj : ctclass.getAnnotations()) {

                if (obj.toString().startsWith("@org.springframework.cloud.openfeign.FeignClient")
                ){
                    Constants.methodsList.add(ctclass.getDeclaredMethods());
//                        method.getAnnotations();
//                    System.out.println(obj.toString()+"============"+ctclass.getDeclaredMethods()[0].getAnnotations()[0].toString());?

                }
                // 通过正则表达示计算出RequestMapping 地z址
                if (obj.toString().startsWith("@org.springframework.web.bind.annotation.RequestMapping")
                    || obj.toString().startsWith("@org.springframework.web.bind.annotation.PostMapping")
                ) {
                    rootRequestUrl = getAnnotationValue("value", obj.toString());
//                    System.out.println(" SpringControllerCollects---Spring---RequestMapping----"+rootRequestUrl);
                } else if (obj.toString().startsWith("@org.springframework.stereotype.Controller")) {
//                    System.out.println(" SpringControllerCollects---Spring---Controller");
                    result = true;
                } else if (obj.toString().startsWith("@org.springframework.web.bind.annotation.RestController")) {
//                    System.out.println(" SpringControllerCollects---Spring---RestController");
                    result = true;
                }
            }
        } catch (ClassNotFoundException e) {
            System.err.println(String.format("bit apm run error targetClassName=%s errorMessage=%s",className,e.getClass().getSimpleName()+":"+e.getMessage()));
        }
        return result;
    }
    /**
     * 对其转换 （插入监控代码）
     */
    public byte[] transform(ClassLoader loader, String className, byte[] classfileBuffer, CtClass ctclass) throws Exception {
        AgentLoader byteLoade = new AgentLoader(className, loader, ctclass);
        List<CtMethod[]> classList = new ArrayList<>();
        //获取继承的接口类
        String[] interfaces = ctclass.getClassFile().getInterfaces();
        for (String anInterface : interfaces) {
            CtClass c = ctclass.getClassPool().get(anInterface);
            classList.add(c.getDeclaredMethods());
        }
        CtMethod[] methods = ctclass.getDeclaredMethods();
        for (CtMethod m : methods) {
//        System.out.println(" transform-----"+m);
            AtomicReference<String> requestUrl = new AtomicReference<>("");
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

            // 必须带上 RequestMapping 注解
//            if ((requestUrl = getRequestMappingValue(m)) == null) {
//                continue;
//            }
            if(classList.size()>0){
                classList.forEach(ctMethods -> {
                    //方法重载判断使用  m.getGenericSignature()  或 m.getSignature()
                    Optional<CtMethod> optional = Arrays.<CtMethod>stream(ctMethods).filter(s -> m.getSignature().equals(s.getSignature())).findFirst();
                    if (optional.isPresent()){
                        try {
                            requestUrl.set(getAnnotationValue("value", optional.get().getAnnotations()[0].toString()));
                        } catch (ClassNotFoundException e) {
//                            throw new RuntimeException(e);
                        }
                    }
                });
            }



            AgentLoader.MethodSrcBuild build = new AgentLoader.MethodSrcBuild();
            build.setBeginSrc(String.format(beginSrc, className, m.getName(), rootRequestUrl + requestUrl.get(),getRequestSwaggerValue(m)));

//            build.setBeginSrc(String.format(beginSrc, className, m.getName(), rootRequestUrl));
            build.setEndSrc(endSrc);
            build.setErrorSrc(errorSrc);
            byteLoade.updateMethod(m, build);
        }
        return byteLoade.toBytecote();
    }


    @NotProguard
    @Override
    public Statistics begin(String className, String method) {
        WebStatistics webStat = new WebStatistics(super.begin(className, method));
        webStat.urlAddress = method;
        webStat.controlName = className;
        webStat.methodName = method;
        webStat.logType="web";
        return  webStat;
    }
    @NotProguard
    public Statistics begins(String className, String method,String urlAddress,String methodSwaggerExplain) {
        WebStatistics webStat = new WebStatistics(super.begin(className, method));
        webStat.controlName = className;
        webStat.methodName = method;
        webStat.urlAddress = urlAddress;
        webStat.methodSwaggerExplain = methodSwaggerExplain;
        webStat.logType="web";
        return  webStat;
    }

    @Override
    public void sendStatistics(Statistics stat) {
        sendStatisticByHttp(stat,"webLog");
    }

    private String getRequestMappingValue(CtMethod m) throws ClassNotFoundException {
        for (Object s : m.getAnnotations()) {
            if (s.toString().startsWith("@org.springframework.web.bind.annotation.RequestMapping")) {
                String val = getAnnotationValue("value", s.toString());
                return val==null?"/":val;
            }
        }
        return null;
    }
    private String getRequestSwaggerValue(CtMethod m) throws ClassNotFoundException {
        for (Object s : m.getAnnotations()) {
            if (s.toString().startsWith("@io.swagger.annotations.ApiOperation")) {
                String val = getAnnotationValueBySwagger("value", s.toString());
                return val==null?"":val;
            }
        }
        return null;
    }

    @NotProguard
    public static class WebStatistics extends Statistics { 
        public String urlAddress; //url 地址
        public String controlName; //服务名称
        public String methodName;// 方法名称
        public String methodSwaggerExplain;// 方法说明
        public WebStatistics(Statistics s) {
            super(s);
        }
    }
}
