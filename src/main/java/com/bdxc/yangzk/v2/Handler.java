package com.bdxc.yangzk.v2;

import com.bdxc.yangzk.annotation.BDXCRequestParam;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * dispathcherServletDemo
 * Yangzk
 * 2019/3/28
 */
public class Handler {
    //正则匹配 url
    private Pattern pattern;
    private Method method;//方法名
    private Object controller;
    private Class<?>[] paramTypes;
    //形参列表 参数名称作为key  参数的位置顺序作为值
    private Map<String,Integer> paramIndexMapping;

    public Handler(Pattern pattern, Method method, Object controller) {
        this.pattern = pattern;
        this.method = method;
        this.controller = controller;
        this.paramTypes = method.getParameterTypes();
        this.paramIndexMapping = new HashMap<String, Integer>();
        putParamIndexMapping(method);
    }

    private void putParamIndexMapping(Method method) {
        //提取方法中加了注解的参数
        Annotation[][] annotations = method.getParameterAnnotations();
       for ( int i = 0; i < annotations.length ; i++ ){
           for (Annotation annotation : annotations[i]){
               if(annotation instanceof BDXCRequestParam){
                   String paramName = ((BDXCRequestParam) annotation).value();
                   if(!"".equals(paramName)){
                       paramIndexMapping.put(paramName,i);
                   }
               }
           }
       }
       //提取方法中的request和response参数
        Class<?>[] parameterTypes = method.getParameterTypes();
       for (int i = 0 ; i < parameterTypes.length ; i++ ){
           Class<?> parameterType = parameterTypes[i];
           if (parameterType == HttpServletRequest.class
                    || parameterType == HttpServletResponse.class){
               paramIndexMapping.put(parameterType.getName(),i);
           }
       }

    }

    public Pattern getPattern() {
        return pattern;
    }

    public Method getMethod() {
        return method;
    }

    public Object getController() {
        return controller;
    }

    public Class<?>[] getParamTypes() {
        return paramTypes;
    }
}
