package com.bdxc.yangzk.controller;

import com.bdxc.yangzk.annotation.BDXCAutowired;
import com.bdxc.yangzk.annotation.BDXCController;
import com.bdxc.yangzk.annotation.BDXCRequestMapping;
import com.bdxc.yangzk.annotation.BDXCRequestParam;
import com.bdxc.yangzk.service.IDemoService;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;


//虽然，用法一样，但是没有功能
@BDXCController
@BDXCRequestMapping("/demo")
public class DemoAction {

  	@BDXCAutowired
	private IDemoService demoService;

	@BDXCRequestMapping("/query")
	public void query(HttpServletRequest req, HttpServletResponse resp,
                      @BDXCRequestParam("name") String name){
//		String result = demoService.get(name);
		String result = "My name is " + name;
		try {
			resp.getWriter().write(result);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@BDXCRequestMapping("/add")
	public void add(HttpServletRequest req, HttpServletResponse resp,
                    @BDXCRequestParam("a") Integer a, @BDXCRequestParam("b") Integer b){
		try {
			resp.getWriter().write(a + "+" + b + "=" + (a + b));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@BDXCRequestMapping("/sub")
	public void add(HttpServletRequest req, HttpServletResponse resp,
                    @BDXCRequestParam("a") Double a, @BDXCRequestParam("b") Double b){
		try {
			resp.getWriter().write(a + "-" + b + "=" + (a - b));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@BDXCRequestMapping("/remove")
	public String  remove(@BDXCRequestParam("id") Integer id){
		return "" + id;
	}

}
