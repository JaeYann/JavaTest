package com.hmdp.utils;

import cn.hutool.core.lang.UUID;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

public class SimpleRedisLock implements ILock{

    private StringRedisTemplate stringRedisTemplate;

    private String name;

    public  SimpleRedisLock( String name,StringRedisTemplate stringRedisTemplate) {
        this.name = name;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    private static final String KEY_PREFIX = "lock";

    //获取UUID
    private static final String ID_PREFIX = UUID.randomUUID().toString(true) + "-";

    //提前定义脚本
    private static final DefaultRedisScript<Long> UNLOCK_SCRIPT;
    static {
        UNLOCK_SCRIPT = new DefaultRedisScript<>();
        UNLOCK_SCRIPT.setLocation(new ClassPathResource("unlock.lua"));
        UNLOCK_SCRIPT.setResultType(Long.class);
    }

    @Override
    public boolean tryLock(long timeoutSec) {
        //获取线程标识
        String threadId = ID_PREFIX + Thread.currentThread().getId();

        //获取锁
        Boolean success = stringRedisTemplate.opsForValue()
                //SET lock thread1 NX EX 10
                .setIfAbsent(KEY_PREFIX + name, threadId , timeoutSec, TimeUnit.SECONDS);
        return Boolean.TRUE.equals(success);
    }
    @Override
    public void unlock() {
        //调用lua脚本
        stringRedisTemplate.execute(
                UNLOCK_SCRIPT
                ,Collections.singletonList(KEY_PREFIX + name)
                ,ID_PREFIX + Thread.currentThread().getId());
    }
    /*
    @Override
    public void unLock() {
        //获取线程标识
        String threadId = ID_PREFIX + Thread.currentThread().getId();

        //获取锁中标识
        String lockId = stringRedisTemplate.opsForValue().get(KEY_PREFIX + name);

        //判断标识是否一致
        if(threadId.equals(lockId)){
            //手动释放锁
            stringRedisTemplate.delete(KEY_PREFIX + name);
        }
    }

     */
}
