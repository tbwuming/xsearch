package com.xsearch.aop;

/**
 * @Description: 用戶操作，用作aop测试
 *
 * @author: wuming.zy
 * @version: v1.0
 * @since: Mar 8, 2017 3:58:39 PM
 */
public interface UserService {

	void addUser();

	String addUserReturnValue();

	void addUserThrowException() throws Exception;

	void addUserAround(String name);
	
}
