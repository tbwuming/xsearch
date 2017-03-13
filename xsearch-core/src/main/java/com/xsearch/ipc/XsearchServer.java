package com.xsearch.ipc;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @Description: Xsearch Server reactor design
 * 
 * @author: wuming.zy
 * @version: v1.0
 * @since: Mar 9, 2017 10:27:08 AM
 */
public class XsearchServer {
	private static final Logger logger = LoggerFactory.getLogger(XsearchServer.class);

	private static final int DEFAULT_CALL_QUEUE_SIZE = 10;
	private static final int DEFAULT_HANDLER_THREAD_NUM = 2;

	private Listener listener; // 监听连接线程
	private Handler[] handlers; // 处理线程
	private BlockingQueue<Call> callQueue; // queued calls

	public XsearchServer(String hostname, int port) throws IOException {
		this.listener = new Listener(hostname, port);
		this.callQueue = new LinkedBlockingQueue<Call>(DEFAULT_CALL_QUEUE_SIZE);
	}

	/**
	 * @Description: 服务启动
	 * @return: void
	 */
	public void start() {
		listener.start();
		handlers = new Handler[DEFAULT_HANDLER_THREAD_NUM];

		for (int i = 0; i < DEFAULT_HANDLER_THREAD_NUM; i++) {
			handlers[i] = new Handler("Hander #" + (i + 1));
			handlers[i].start();
		}
	}

	/**
	 * @Description: 服务停止
	 * @return: void
	 */
	public void stop() {

	}

	/**
	 * @Description: 在reactor模式中用作监听客户端连接请求，相当于Reactor功能，只处理accept事件
	 * 
	 * @author: wuming.zy
	 * @version: v1.0
	 * @since: Jun 7, 2016 4:35:26 PM
	 */
	private class Listener extends Thread {
		private static final int DEFAULT_BACKLOG = 128; // 指定客户连接请求队列的缺省长度
		private static final int DEFAULT_READER_THREAD_NUM = 1; // 缺省读线程数量

		private Selector acceptSelector;
		private InetSocketAddress address;
		private ServerSocketChannel channel;

		// 所有的读线程，直接用于监听客户端请求，开启线程池来运行这些线程
		private Reader[] readers;
		private int currentReaderIndex;

		public Listener(String hostname, int port) throws IOException {
			address = new InetSocketAddress(hostname, port);

			// 监听TCP连接，设置为非阻塞模式，并且绑定地址
			channel = ServerSocketChannel.open();
			channel.configureBlocking(false);
			channel.socket().bind(address, DEFAULT_BACKLOG);

			readers = new Reader[DEFAULT_READER_THREAD_NUM];
			for (int i = 0; i < DEFAULT_READER_THREAD_NUM; i++) {
				readers[i] = new Reader("Reader #" + (i + 1) + " for port " + port);
				readers[i].start();
			}

			// 向selector注册监听ServerSocketChannel的特定事件，当前事件用于监听连接请求
			acceptSelector = Selector.open();
			channel.register(acceptSelector, SelectionKey.OP_ACCEPT);

			// 设置线程名称并且将监听线程设置为守护线程
			this.setName("XsearchServer Listen On " + port);
			this.setDaemon(true);
		}
		
		public String getThreadName() {
			return Thread.currentThread().getName();
		}

		@Override
		public void run() {
			logger.info("Starting " + getThreadName());
			while (true) {
				SelectionKey key = null;
				try {
					acceptSelector.select();
					Iterator<SelectionKey> iterator = acceptSelector.selectedKeys().iterator();
					while (iterator.hasNext()) {
						key = iterator.next();
						iterator.remove();
						if (key.isValid() && key.isAcceptable()) {
							doAccept(key);
						}
					}
				} catch (IOException e) {
					logger.error("Error " + getName(), e);
					closeConnection(key);
				} catch (InterruptedException e) {
					logger.error("Error " + getName(), e);
					closeConnection(key);
				} finally {
					//closeConnection(key);
				}
			}
		}

		/**
		 * @Description: 接收新客户端连接请求
		 * @param key
		 * @throws InterruptedException
		 * @throws IOException
		 * @return: void
		 */
		private void doAccept(SelectionKey key) throws InterruptedException, IOException {
			ServerSocketChannel server = (ServerSocketChannel) key.channel();
			SocketChannel channel = null;
			while ((channel = server.accept()) != null) {
				channel.configureBlocking(false);
				channel.socket().setTcpNoDelay(true);
				channel.socket().setKeepAlive(true);

				//channel.register(acceptSelector, SelectionKey.OP_READ, "Connection");
				channel.register(acceptSelector, SelectionKey.OP_READ);

				Reader reader = getReader();
				triggerReader(key, reader, channel);
			}
		}

		/**
		 * @Description: 采用轮询策略选择一个Reader线程处理新连接，线程安全保证同一时刻进来的连接请求选用不同的reader
		 * @return: Reader
		 */
		private synchronized Reader getReader() {
			currentReaderIndex = (currentReaderIndex + 1) % readers.length;
			return readers[currentReaderIndex];
		}

		/**
		 * @Description: 创建连接对象，并将其附加到key上，然后将其放在reader中待处理的连接队列里
		 * @param key
		 * @param reader
		 * @param channel
		 * @throws InterruptedException
		 * @return: void
		 */
		private void triggerReader(SelectionKey key, Reader reader, SocketChannel channel) throws InterruptedException {
			Connection connection = new Connection(channel, System.currentTimeMillis());
			key.attach(connection);
			reader.addConnection(connection);
		}

		/**
		 * @Description: 关闭连接
		 * @param key
		 * @return: void
		 */
		private void closeConnection(SelectionKey key) {
			Connection connection = (Connection) key.attachment();
			connection.close();
		}

	}

	/**
	 * @Description: 接收读事件，进行读取
	 * 
	 * @author: wuming.zy
	 * @version: v1.0
	 * @since: Jun 8, 2016 5:14:20 PM
	 */
	private class Reader extends Thread {

		private Selector readSelector;
		private final BlockingQueue<Connection> pendingConnections;

		public Reader(String name) throws IOException {
			super(name); // 线程名字
			this.readSelector = Selector.open();
			this.pendingConnections = new LinkedBlockingQueue<Connection>(100);
		}

		@Override
		public void run() {
			logger.info("Starting " + getName());
			while (true) {
				try {
					// 在读之前为目前池中的所有的连接登记读事件，因为在doAccept中没有直接登记，也避免了readSelector饥饿死掉
					int size = pendingConnections.size();
					for (int i = size; i > 0; i--) {
						Connection connection = pendingConnections.take();
						connection.channel.register(readSelector, SelectionKey.OP_READ, connection);
					}

					int readyChannels = readSelector.select();
					if (readyChannels <= 0) {
						continue;
					}

					Iterator<SelectionKey> iterator = readSelector.selectedKeys().iterator();
					while (iterator.hasNext()) {
						SelectionKey key = iterator.next();
						iterator.remove();
						if (key.isValid() && key.isReadable()) {
							doRead(key);
						}
					}
				} catch (IOException e) {
					logger.error("Error " + getName(), e);
				} catch (InterruptedException e) {
					logger.error("Error " + getName(), e);
				}
			}
		}

		/**
		 * @Description: 处理读数据请求
		 * @param key
		 * @return: void
		 */
		private void doRead(SelectionKey key) {
			Connection connection = (Connection) key.attachment();
			if (connection == null) {
				return;
			}

			// 使用该连接进行读数据，更新一下最后通信时间
			connection.setLastContactTime(System.currentTimeMillis());

			int count = -1;
			try {
				count = connection.readAndProcess();
			} catch (IOException e) {
				logger.error("Error " + getName(), e);
				count = -1;
			} catch (InterruptedException e) {
				logger.error("Error " + getName(), e);
				count = -1;
			} finally {
				if (count < 0) { // 读取失败，关闭连接
					connection.close();
				}
			}

			// 读取处理完成，更新一下最后通信时间
			connection.setLastContactTime(System.currentTimeMillis());
			System.out.println(connection.getLastContactTime());
		}

		/**
		 * @Description: 唤醒readSelector，因为readSelector有可能因为select()处于阻塞中
		 * @param connection
		 * @throws InterruptedException
		 * @return: void
		 */
		public void addConnection(Connection connection) throws InterruptedException {
			pendingConnections.put(connection);
			readSelector.wakeup();
		}
	}

	/**
	 * @Description: 连接对象，记录服务端和客户端的连接
	 * 
	 * @author: wuming.zy
	 * @version: v1.0
	 * @since: Jun 8, 2016 4:40:59 PM
	 */
	private class Connection {
		// 通信的channel及对应的socket
		private SocketChannel channel;
		private Socket socket;

		// 最后通信时间
		private long lastContactTime;

		// 远端的地址和端口号
		private String remoteAddress;
		private int remotePort;
		private InetAddress address;

		// 读取到的数据和数据长度
		private ByteBuffer dataLengthBuffer;
		private ByteBuffer dataBuffer;

		public Connection(SocketChannel channel, long lastContactTime) {
			this.channel = channel;
			this.lastContactTime = lastContactTime;
			this.socket = channel.socket();
			this.address = socket.getInetAddress();
			if (address == null) {
				this.remoteAddress = "*Unknown*";
			} else {
				this.remoteAddress = address.getHostAddress();
			}

			this.remotePort = socket.getPort();
			this.dataLengthBuffer = ByteBuffer.allocate(4);
		}

		@Override
		public String toString() {
			return remoteAddress + ":" + remotePort;
		}

		/**
		 * @Description: 实际读取数据，并进行处理
		 * @throws IOException
		 *             , InterruptedException
		 * @return: int
		 */
		public int readAndProcess() throws IOException, InterruptedException {
			
			dataBuffer = ByteBuffer.allocate(100);
			int count = channel.read(dataBuffer);
			
			/*
			dataBuffer.flip();
			byte[] bytes = new byte[dataBuffer.remaining()]; // create a byte array the length of the number of bytes written to the buffer
			dataBuffer.get(bytes); // read the bytes that were written
			System.out.println(new String(bytes));
			*/
			
			System.out.println(new String(dataBuffer.array(), 0, dataBuffer.position()));
			
			return count;
			
			/*int count = channel.read(dataLengthBuffer); // 文件中的数据写入到buffer中
			if (count < 0 || dataLengthBuffer.remaining() > 0) {
				return count;
			}

			if (dataBuffer == null) {
				dataLengthBuffer.flip();
				int dataLength = dataLengthBuffer.getInt();
				dataBuffer = ByteBuffer.allocate(dataLength);
			}

			count = channel.read(dataBuffer);
			if (dataBuffer.remaining() == 0) { // 数据成功读取完成
				dataBuffer.flip();
				System.out.println(dataBuffer.toString());
				process(dataBuffer.array());
			}

			return count;*/
		}

		/**
		 * @Description: 处理请求
		 * @return: void
		 * @throws InterruptedException
		 */
		private void process(byte[] data) throws InterruptedException {
			Call call = new Call(1, 1, this);
			callQueue.put(call);
		}

		public synchronized void close() {
			if (!channel.isOpen()) {
				return;
			}

			try {
				socket.shutdownOutput();
			} catch (Exception e) {
				logger.debug("Ignoring socket shutdown exception", e);
			}

			if (channel.isOpen()) {
				try {
					channel.close();
				} catch (Exception e) {
				}
			}

			try {
				socket.close();
			} catch (Exception e) {
				logger.debug("Ignoring socket shutdown exception", e);
			}
		}

		public long getLastContactTime() {
			return lastContactTime;
		}

		public void setLastContactTime(long lastContactTime) {
			this.lastContactTime = lastContactTime;
		}

	}

	/**
	 * @Description: 消费者，处理call队列的线程
	 * 
	 * @author: wuming.zy
	 * @version: v1.0
	 * @since: Jun 11, 2016 12:28:47 PM
	 */
	private class Handler extends Thread {
		public Handler(String name) {
			super(name);
		}

		@Override
		public void run() {
			logger.info("Starting " + getName());

			while (true) {
				try {
					Call call = callQueue.take();
					System.out.println(call.toString());
				} catch (InterruptedException e) {
					logger.error("Error " + getName(), e);
				}
			}
		}
	}

	/**
	 * @Description: 实际调用参数类
	 * 
	 * @author: wuming.zy
	 * @version: v1.0
	 * @since: Jun 11, 2016 12:46:09 PM
	 */
	private class Call {
		private final int id; // 客户调用id
		private final int retryCount; // 重试次数
		private final Connection connection; // 客户端连接
		
		//private Writable param;
		

		public Call(int id, int retryCount, Connection connection) {
			this.id = id;
			this.retryCount = retryCount;
			this.connection = connection;
		}

		@Override
		public String toString() {
			return "Call [id=" + id + ", retryCount=" + retryCount + ", connection=" + connection + "]";
			//return param.toString() + " from " + connection.toString();
		}
	}
}
