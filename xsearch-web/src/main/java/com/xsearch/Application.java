package com.xsearch;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * @Description: springmvc test main class
 *               <p>
 *               使用SpringBootApplication注解相当于使用了3个注解，分别是@ComponentScan，@Configuration
 *               ，@EnableAutoConfiguration。 
 *               <p>
 *               这里需要注意的是Application这个类所有的package位置。
 * 
 *               比如项目的包路径是com.xsearch，对应的controller和repository包是
 *               com.xsearch.controller和com.xsearch.repository。
 *               那么这个Application需要在的包路径为com.xsearch。
 *               因为SpringBootApplication注解内部是使用ComponentScan注解
 *               ，这个注解会扫描Application包所在的路径下的各个bean。
 *               
 *               <p>
 *               https://spring.io/guides/gs/serving-web-content/
 *               https://spring.io/guides
 * 
 * 
 * @author: wuming.zy
 * @version: v1.0
 * @since: Jan 23, 2017 3:25:40 PM
 */
@SpringBootApplication
public class Application {

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}

}
