package com.xsearch.ipc;

import org.junit.Test;

/**
 * @Description: XsearchServer 测试
 *
 * @author: wuming.zy
 * @version: v1.0
 * @since: Mar 9, 2017 3:01:21 PM
 */
public class XsearchServerTest {

	@Test
	public void test() throws Exception{
		XsearchServer server = new XsearchServer("localhost", 9090);
		server.start();
		
		Thread.sleep(100);
	}

}
