package com.bdxc.yangzk.service.impl;

import com.bdxc.yangzk.annotation.BDXCService;
import com.bdxc.yangzk.service.IDemoService;

/**
 * 核心业务逻辑
 */
@BDXCService
public class DemoService implements IDemoService {

	public String get(String name) {
		return "My name is " + name;
	}

}
