package com.xsearch.aop;

import org.springframework.stereotype.Service;

/**
 * @Description: 用户操作实现类，用作aop测试
 * 
 * @author: wuming.zy
 * @version: v1.0
 * @since: Mar 8, 2017 4:00:15 PM
 */
@Service
public class UserServiceImpl implements UserService {

	@Override
	public void addUser() {
		String methodName = new Throwable().getStackTrace()[0].getMethodName();
		System.out.println(methodName + "() is running ");
	}

	@Override
	public String addUserReturnValue() {
		String methodName = new Throwable().getStackTrace()[0].getMethodName();
		System.out.println(methodName + "() is running ");
		return "success";
	}

	@Override
	public void addUserThrowException() throws Exception {
		String methodName = new Throwable().getStackTrace()[0].getMethodName();
		System.out.println(methodName + "() is running ");
		throw new Exception("Generic Error");
	}

	@Override
	public void addUserAround(String name) {
		String methodName = new Throwable().getStackTrace()[0].getMethodName();
		System.out.println(methodName + "() is running, args : " + name);
	}

}
