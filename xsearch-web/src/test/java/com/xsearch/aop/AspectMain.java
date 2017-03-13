package com.xsearch.aop;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * @Description: aop测试
 * 
 * @author: wuming.zy
 * @version: v1.0
 * @since: Mar 8, 2017 4:30:23 PM
 */
public class AspectMain {

	public static void main(String[] args) {
		ApplicationContext context = new ClassPathXmlApplicationContext("spring-aop-test.xml");
		UserService userService = context.getBean(UserService.class);

		userService.addUser();

		userService.addUserReturnValue();

		try {
			userService.addUserThrowException();
		} catch (Exception e) {
			e.printStackTrace();
		}

		userService.addUserAround("jimmy");

		((ClassPathXmlApplicationContext) context).close();
	}

}
