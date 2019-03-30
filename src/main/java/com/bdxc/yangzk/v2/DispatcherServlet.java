package com.bdxc.yangzk.v2;

import com.bdxc.yangzk.annotation.BDXCAutowired;
import com.bdxc.yangzk.annotation.BDXCController;
import com.bdxc.yangzk.annotation.BDXCService;
import com.bdxc.yangzk.constant.Constant;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.*;

/**
 * dispathcherServletDemo
 * Yangzk
 * 2019/3/28
 */
public class DispatcherServlet extends HttpServlet {

    //保存config.properties文件中的内容
    private  Properties locationConfig = new Properties();
    //保存扫描到的类
    private List<String> classNameList = new ArrayList<String>();
    //所谓的牛B的IOC容器
    private Map<String,Object> ioc = new HashMap<String, Object>();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doPost(req,resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        super.doPost(req, resp);
    }

    @Override
    public void init(ServletConfig config) throws ServletException {
        try {
            //1. 加载配置文件
            doLocationConfig(config);
            //2. 扫描配置文件
            doScanner(locationConfig.getProperty(Constant.SCAN_PACKAGE));

           //3. 初始化扫描到的类 并将其初始化到IOC容器中
            doInstance();

           //4. 完成依赖注入
            doAutowired();

           //5.初始化handlerMapping
            initHandlerMapping();

            System.out.println("BDXC Spring framework is init.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void initHandlerMapping() {
    }
    //完成依赖注入
    private void doAutowired() {
        if(ioc.isEmpty()){return;}
        for (Map.Entry<String, Object> entry : ioc.entrySet()) {
            //获取对象的所有属性
            Field[] fields = entry.getValue().getClass().getDeclaredFields();
            for (Field field : fields) {
                //判断不是每个属性都要进行注入 只针对加有@BDXCAutoWired注解的属性进行实例化
                if(!field.isAnnotationPresent(BDXCAutowired.class)) {
                    continue;
                }
                String filedName = field.getAnnotation(BDXCAutowired.class).value().trim();
                if("".equals(filedName)){
                    filedName = toLowerFirstCase(field.getName());
                }
                //强吻 反射中叫做暴力访问  就是对除了public之外的修饰符也可以访问 如：private
                field.setAccessible(true);
                try {
                    field.set(entry.getValue(),filedName);
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }

            }

        }

    }
    //将扫描到的类进行初始化 并放入到牛逼的IOC容器中
    private void doInstance() {
       //判断实例化对象集合不为空
        if(classNameList.isEmpty()){return;}
        try {
            //循环
            for (String className : classNameList) {
                //通过反射实例化对象
                Class<?> clazz = Class.forName(className);
                String beanName = "";
                //实例化对象不是所有的对象都要进行实例化 只对加有注解的对象进行实例化
                if(clazz.isAnnotationPresent(BDXCController.class)){
                    //1.判断该注解是否自定义名称
                    beanName = clazz.getAnnotation(BDXCController.class).value().trim();
                    if("".equals(beanName)){
                        //2. 如果为空默认类名首字母小写
                        beanName = toLowerFirstCase(clazz.getSimpleName());
                    }
                }else if(clazz.isAnnotationPresent(BDXCService.class)){
                    beanName = clazz.getAnnotation(BDXCService.class).value().trim();
                    if("".equals(beanName)){
                        beanName = toLowerFirstCase(clazz.getSimpleName());
                    }
                }else{
                    continue;
                }
                Object instance = clazz.newInstance();
                ioc.put(beanName,instance);//将实例化的对象放入到牛逼的ioc容器中
                //判断是不是接口类型
                for (Class<?> anInterface : clazz.getInterfaces()) {
                    if(ioc.containsKey(anInterface.getName())){
                        throw new Exception("The ”"+anInterface.getName()+"“ is exist !!");
                    }
                    ioc.put(anInterface.getName(),instance);//直接接口类型当作key进行注入
                }

            }
        }catch(Exception e){
            e.printStackTrace();
        }


    }
    //通过ASCII码值 首字母小写
    private String toLowerFirstCase(String simpleName) {
        char[] chars = simpleName.toCharArray();
        //因为大小写值属性相差32  如果这的类名本身就是小写的这里就会有问题
        chars[0]+=32;
        return chars.toString();
    }

    //扫描配置文件
    private void doScanner(String locationConfig) {
        //处理扫描到的类路径将.替换为/ 并且获取类的url
       URL url = this.getClass().getClassLoader().getResource(locationConfig.replaceAll("\\.","/"));
       //获取该路径下的所有文件
        File classPath = new File(url.getFile());
        for (File file : classPath.listFiles()) {
            //判断获取到的文件是不是一个文件夹 如果是一个文件夹当前路径拼接递归循环
            if (file.isDirectory()){
                doScanner(url+"/"+file.getName());
            }else {
                //判断该文件是不是以.class结尾
                if (!file.getName().endsWith(".class")) {
                    continue;
                }
                String className = locationConfig+"."+file.getName().replaceAll("\\.","/");
                classNameList.add(className);

            }

        }

    }

    //1.加载配置文件
    private void doLocationConfig(ServletConfig config) throws IOException {
        InputStream is = this.getClass().getClassLoader().getResourceAsStream(config.getInitParameter(Constant.CONTEXT_CONFIG_LOCATION));
        try {
            locationConfig.load(is);
        } catch (IOException e) {
            throw new IOException(Arrays.toString(e.getStackTrace()));
        }finally {
            try {
                is.close();
            } catch (IOException e) {
                throw new IOException(Arrays.toString(e.getStackTrace()));
            }
        }
    }
}
