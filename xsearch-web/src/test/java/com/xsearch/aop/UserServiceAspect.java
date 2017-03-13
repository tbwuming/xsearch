package com.xsearch.aop;

import java.util.Arrays;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

/**
 * @Description: 用户操作检查aop测试
 * 
 * @author: wuming.zy
 * @version: v1.0
 * @since: Mar 8, 2017 4:16:35 PM
 */
@Component
@Aspect
public class UserServiceAspect {

	/**
	 * 前置通知, 在addUser方法执行之前执行
	 */
	@Before("execution(* com.xsearch.aop.UserService.addUser(..))")
	public void addUserBefore(JoinPoint joinPoint) {
		String methodName = new Throwable().getStackTrace()[0].getMethodName();
		System.out.println(methodName + "() is running ");
		System.out.println("xxoo : " + joinPoint.getSignature().getName());
		System.out.println("******");
	}

	/**
	 * 后置通知, 在addUser方法执行之后执行
	 */
	@After("execution(* com.xsearch.aop.UserService.addUser(..))")
	public void addUserAfter(JoinPoint joinPoint) {
		String methodName = new Throwable().getStackTrace()[0].getMethodName();
		System.out.println(methodName + "() is running ");
		System.out.println("xxoo : " + joinPoint.getSignature().getName());
		System.out.println("******");
	}

	/**
	 * 返回通知, 在addUserReturnValue方法返回结果之后执行
	 */
	@AfterReturning(pointcut = "execution(* com.xsearch.aop.UserService.addUserReturnValue(..))", returning = "result")
	public void addUserAfterReturning(JoinPoint joinPoint, Object result) {
		String methodName = new Throwable().getStackTrace()[0].getMethodName();
		System.out.println(methodName + "() is running ");
		System.out.println("xxoo : " + joinPoint.getSignature().getName());
		System.out.println("Method returned value is : " + result);
		System.out.println("******");
	}

	/**
	 * 异常通知, 在addUserThrowException方法抛出异常之后执行
	 */
	@AfterThrowing(pointcut = "execution(* com.xsearch.aop.UserService.addUserThrowException(..))", throwing = "error")
	public void addUserAfterThrowing(JoinPoint joinPoint, Throwable error) {
		String methodName = new Throwable().getStackTrace()[0].getMethodName();
		System.out.println(methodName + "() is running ");
		System.out.println("xxoo : " + joinPoint.getSignature().getName());
		System.out.println("Exception : " + error);
		System.out.println("******");
	}

	/**
	 * 环绕通知, 围绕着addUserAround方法执行, 配置成普通bean元素即可
	 */
	@Around("execution(* com.xsearch.aop.UserService.addUserAround(..))")
	public void addUserAround(ProceedingJoinPoint joinPoint) throws Throwable {
		String methodName = new Throwable().getStackTrace()[0].getMethodName();
		System.out.println(methodName + "() is running ");
		System.out.println("xxoo method : " + joinPoint.getSignature().getName());
		System.out.println("xxoo arguments : " + Arrays.toString(joinPoint.getArgs()));

		System.out.println("Around before is running!");
		joinPoint.proceed(); // continue on the intercepted method
		System.out.println("Around after is running!");

		System.out.println("******");
	}
	
	/**
	 * 所有带Service字符串的类的所有方法，都执行切面
	 */
	@Pointcut("execution(* com.xsearch.aop.*Service*.*(..))")
	public void pointCut() {
	}
	
	/**
	 * 前置通知，在所有带Service字符串的类的所有方法之前执行
	 * Before中的字符串是上面配置@Pointcut的那个方法，签名必须一致，带()
	 */
	@Before("pointCut()")
	public void before(JoinPoint joinPoint) {
		System.out.println("before aspect executing, xxoo pointCut");
	}
	
	//Pointcut to execute on all the methods of classes in a package
	@Pointcut("within(com.xsearch.aop.*Service*.*)")
	public void allMethodsPointcut() {
	}
}
