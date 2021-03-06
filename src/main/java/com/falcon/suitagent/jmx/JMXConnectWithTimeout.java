/*
 * www.yiji.com Inc.
 * Copyright (c) 2016 All Rights Reserved
 */
package com.falcon.suitagent.jmx;
/*
 * 修订记录:
 * guqiu@yiji.com 2016-08-09 10:25 创建
 */

import com.falcon.suitagent.util.ExecuteThreadUtil;
import com.falcon.suitagent.util.BlockingQueueUtil;

import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * @author guqiu@yiji.com
 */
public class JMXConnectWithTimeout {

    /**
     * JMX连接
     * @param url
     * JMX连接地址
     * @param jmxUser
     * JMX授权用户 null为无授权用户
     * @param jmxPassword
     * JMX授权密码 null为无授权密码
     * @param timeout
     * 超时时间
     * @param unit
     * 超时单位
     * @return
     * @throws IOException
     */
    public static JMXConnector connectWithTimeout( final JMXServiceURL url,String jmxUser,String jmxPassword, long timeout, TimeUnit unit) throws Exception {
        final BlockingQueue<Object> blockingQueue = new ArrayBlockingQueue<>(1);
        ExecuteThreadUtil.execute(() -> {
            try {
                JMXConnector connector;
                if(jmxUser != null && jmxPassword != null){
                    Map<String,Object> env = new HashMap<>();
                    String[] credentials = new String[] { jmxUser, jmxPassword };
                    env.put(JMXConnector.CREDENTIALS, credentials);
                    connector = JMXConnectorFactory.connect(url,env);
                }else{
                    connector = JMXConnectorFactory.connect(url,null);
                }
                if (!blockingQueue.offer(connector))
                    connector.close();
            } catch (Throwable t) {
                blockingQueue.offer(t);
            }
        });

        Object result = BlockingQueueUtil.getResult(blockingQueue,timeout,unit);
        blockingQueue.clear();


        if (result instanceof JMXConnector){
            return (JMXConnector) result;
        }else if (result == null){
            throw new SocketTimeoutException("Connect timed out: " + url);
        }else if(result instanceof Throwable){
            throw new IOException("JMX Connect Failed : " + url,((Throwable) result));
        }
        return null;
    }

}
