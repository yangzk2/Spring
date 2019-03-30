package com.bdxc.yangzk.v1;

import com.bdxc.yangzk.annotation.*;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.*;

/**
 * Yangzk
 * 2019/3/27
 */
public class DispatcherServlet extends HttpServlet {
    //保存config.properties配置文件中的内容
    private Properties contextConfig = new Properties();
    //保存扫描的所有的类名
    private List<String> classNames  = new ArrayList<String>();
    private Map<String,Object> ioc = new HashMap<String,Object>();
    private Map<String,Method> handlerMapping = new HashMap<String, Method>();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        this.doPost(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        //6.调用 运行
        try {
            doDespath(req,resp);
        } catch (Exception e) {
            resp.getWriter().write("500 Exection,Detail :"+ Arrays.toString(e.getStackTrace()));
            e.printStackTrace();
        }
    }
    //页面访问 运行
    private void doDespath(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        //获取客户端请求的路径  绝对路径
        String url = req.getRequestURI();
        //处理成相对路径
        String contextPath = req.getContextPath();
        url = url.replaceAll(contextPath,"");
        //判断请求的url和项目中是否一致
        if(!this.handlerMapping.containsKey(url)){
            resp.getWriter().write("404 Not Found!!!");
            return;
        }
        Method method = this.handlerMapping.get(url);
        //从request中拿到url传过来的参数

        Map<String,String[]> parameterMap = req.getParameterMap();
        //获取方法的形参列表
        Class<?>[] parameterTypes = method.getParameterTypes();

        Object[] paramValues = new Object[parameterTypes.length];
        for (int i = 0; i < parameterTypes.length;i++){
            //获取每个请求的参数
            Class<?> parameterType = parameterTypes[i];
            if(parameterType == HttpServletRequest.class){
                paramValues[i] = req;
                continue;
            }else if(parameterType == HttpServletResponse.class){
                paramValues[i] = resp;
                continue;
            }else if(parameterType == String.class){
                BDXCRequestParam requestParam = parameterType.getAnnotation(BDXCRequestParam.class);
                if(parameterMap.containsKey(requestParam.value().trim())){
                    for (Map.Entry<String, String[]> param: parameterMap.entrySet()) {
                        String value = Arrays.toString(param.getValue())
                                .replaceAll("\\[|\\]","")
                                .replaceAll("\\s",",");
                            paramValues[i] = value;
                    }
                }
            }
        }
        //通过反射拿到method所在的class,拿到class然后在通过class拿到class的名称
        String beanName = method.getDeclaringClass().getSimpleName();
        method.invoke(ioc.get(beanName),paramValues);

    }


    @Override
    public void init(ServletConfig config) throws ServletException {
        //加载配置文件

        doLoadConfig(config.getInitParameter("contextConfigLocation"));
        //扫描类文件
        doScanner(contextConfig.getProperty("scanPackage"));
        //初始化所有扫描的类 并放入到IOC容器中
        doInstance();
        //完成依赖注入
        doAutowired();
        //初始化handlerMapping
        initHandlerMapping();
        System.out.println("BDXC Spring framework is init.");
    }
    //初始化url 和method的一对一对应关系
    private void initHandlerMapping() {
        if(ioc.isEmpty()){return;}
        for (Map.Entry<String, Object> entry : ioc.entrySet()) {
            Class<?> clazz = entry.getValue().getClass();
            //判断不是BDXCController的对象跳过
            if(!clazz.isAnnotationPresent(BDXCController.class)){
                continue;
            }
            String baseUrl = "";
            //获取controller类中的url路径
            if(clazz.isAnnotationPresent(BDXCRequestMapping.class)){
                baseUrl = clazz.getAnnotation(BDXCRequestMapping.class).value().trim();
            }
            //获取所有公开方法中的requestMapping路径
            for (Method method : clazz.getMethods()) {
                if(!method.isAnnotationPresent(BDXCRequestMapping.class)){
                    continue;
                }
                BDXCRequestMapping requestMapping = method.getAnnotation(BDXCRequestMapping.class);
                String url =("/"+baseUrl+"/"+requestMapping.value().trim()).replaceAll("/+","/");
                handlerMapping.put(url,method);
                System.out.println("Mapped :"+url+","+method);
            }


        }

    }
    //完成依赖注入
    private void doAutowired() {
        if(ioc.isEmpty()){return;}
        for (Map.Entry<String, Object> entry : ioc.entrySet()) {
            //获取实例中的所有属性
            Field[] fields = entry.getValue().getClass().getDeclaredFields();
            for (Field field : fields) {
                //不是每个属性都要进行依赖注入的 只给带有Autowired注解的属性进行注入
                if(!field.isAnnotationPresent(BDXCAutowired.class)){continue;}
                BDXCAutowired autowired = field.getAnnotation(BDXCAutowired.class);
                //判断是否自定义fieldName
                String fieldName = autowired.value().trim();
                if("".equals(fieldName)){
                    //如果没有自定义默认根据类型注入 类名默认小写
                    fieldName = toLowerFirstCase(field.getType().getName());
                }
                //判断是 public 以外的修饰符 只要加有 Autowired注解都要可以访问的到
                //反射中叫做暴力访问  强吻
                field.setAccessible(true);
                try {
                    //通过反射机制 动态为字段赋值
                    field.set(entry.getValue(),ioc.get(fieldName));
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }

        }

    }
    //将扫描到的类进行实例化
    private void doInstance() {
        //初始化，为DI做准备
        if(classNames.isEmpty()){return;}
        try {
            for (String className : classNames) {
                //通过反射实例化类对象
                Class<?> clazz = Class.forName(className);
                String beanName = "";
                //实例化类对象不是实例化所有的  这里需要判断 只有加又注解对象的我们才能进行实例化
                if(clazz.isAnnotationPresent(BDXCController.class)){
                    //1.如果是自定义的beanName
                    BDXCController controller = clazz.getAnnotation(BDXCController.class);
                    beanName = controller.value();
                    //2.如果没有自定义的beanName 默认类名首字母小写
                    if("".equals(beanName)) {
                        beanName = toLowerFirstCase(clazz.getSimpleName());
                    }
                   /* Object instance = clazz.newInstance();
                    ioc.put(beanName,instance);//放入IOC容器中*/
                }else if(clazz.isAnnotationPresent(BDXCService.class)){
                    //1. 如果是自定义的beanName
                    BDXCService service = clazz.getAnnotation(BDXCService.class);
                    beanName = service.value();
                    //2. 如果没有自定义beanName 默认类名小写
                    if("".equals(beanName)){
                        beanName = toLowerFirstCase(clazz.getSimpleName());
                    }
                }else{
                    continue;
                }
                Object instance = clazz.newInstance();
                ioc.put(beanName,instance);//放入IOC容器中
                //3. 根据类型自动赋值
                for (Class<?> i : clazz.getInterfaces()){
                    if (ioc.containsKey(i.getName())){
                        throw new Exception("The “"+i.getName()+" is exists !!");
                    }
                    //把接口的类型直接当成key
                    ioc.put(i.getName(),instance);
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }
    //类名首字母转化小写
    private String toLowerFirstCase(String simpleName) {
        char[] chars = simpleName.toCharArray();
        //大小写字母的ASCII码值相差32
        // 大小写字母的ASCII码要小于小写字母的ASCII码值
        chars[0] += 32;
        return String.valueOf(chars);
    }

    //扫描所有类文件
    private void doScanner(String scanPackage) {
        //获取配置文件中所有的包转化为文件路径 classpath
        URL url = this.getClass().getClassLoader().getResource("/"+scanPackage.replaceAll("\\.","/"));
        File classpath = new File(url.getFile());
        for (File file:classpath.listFiles()) {
            //判断是不是一个文件夹
            if(file.isDirectory()){
                doScanner(scanPackage+"."+file.getName());
            }else{
                //判断是不是以.class结尾
                if(file.getName().endsWith(".class")){continue;}
                String className = (scanPackage + "."+file.getName().replaceAll(".class",""));
                classNames.add(className);
            }
        }

    }

    /**
     * 加载配置文件
     */
    private void doLoadConfig(String contextConfigLocation) {
        //直接从类路径下找到spring主配置文件所在的路径 并通过输入流读取出来放到Properties对象中
        InputStream is = this.getClass().getClassLoader().getResourceAsStream(contextConfigLocation);
        try {
            contextConfig.load(is);
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            try {
                is.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
