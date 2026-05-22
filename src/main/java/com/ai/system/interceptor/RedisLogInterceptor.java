package com.ai.system.interceptor;

import com.ai.system.interceptor.dto.RedisLogDTO;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import lombok.extern.slf4j.Slf4j;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.aop.framework.ProxyFactory;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

@Slf4j
public class RedisLogInterceptor implements MethodInterceptor{
    //json序列化
    protected static ObjectMapper mapper = new ObjectMapper()
            .setSerializationInclusion(JsonInclude.Include.NON_EMPTY)
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true)
            .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, true);

    protected org.slf4j.Logger getLog(){
        return log;
    }
    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        //执行目标方法
        Object result = invocation.proceed();
        String methodName = invocation.getMethod().getName();
        //针对redis的方法进行代理
        if ("getConnection".equals(methodName)) {
            ProxyFactory proxyFactory = new ProxyFactory(result);
            proxyFactory.setProxyTargetClass(true);
            proxyFactory.addAdvice((MethodInterceptor) methodInvocation ->
                interceptorRedis(methodInvocation)
            );
            return proxyFactory.getProxy();
        }
        return result;
    }

    public Object interceptorRedis(MethodInvocation invocation) throws Throwable {
        Method method = invocation.getMethod();
        String name = method.getName();
        Set<String> notSet = new HashSet<>();
        notSet.add("isPipelined");
        notSet.add("close");
        notSet.add("isQueueing");
        if (notSet.contains(name)) {
            return invocation.proceed();
        }

        RedisLogDTO dto = new RedisLogDTO();
        dto.setMethod(name);

        Object target = invocation.getThis();
        dto.setClazz(target.getClass().getName());

        Object[] args = invocation.getArguments();
        dto.setParams(serial(args));

        Object ret = null;
        long start = System.currentTimeMillis();
        try {
            ret = invocation.proceed();
            return ret;
        } catch (Exception exp) {
            dto.setExp(exp);
            throw exp;
        } finally {
            dto.setCostTime(System.currentTimeMillis() - start);
            dto.setResult(ret);
            doLog(dto);
        }
    }

    private void doLog(RedisLogDTO dto) {
        if (dto.getExp() != null) {
            getLog().warn(serial(dto));
        } else {
            getLog().debug(serial(dto));
        }
    }

    private String serial(Object[] obj) {
        if (obj == null || obj.length == 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (Object item : obj) {
            if (sb.length() > 0) {
                sb.append(",");
            }
            sb.append(serial(item));
        }
        sb.insert(0, "[").append("]");
        return sb.toString();
    }

    private String serial(Object obj) {
        if (obj == null) {
            return "";
        }
        try {
            if (obj instanceof byte[]) {
                obj = new String((byte[]) obj);
            }
            if( obj instanceof RedisLogDTO){
                RedisLogDTO dto =  (RedisLogDTO)obj;
                Object result = dto.getResult();
                if (result instanceof byte[]) {
                    dto.setResult(new String((byte[])result));
                }
                return dto.toString();
            }
            return obj.toString();
        } catch (Exception ex) {
            getLog().error("{} serialize error: {}", obj.getClass().getName(), ex.toString());
            return obj.toString();
        }
    }
}
