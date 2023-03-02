package com.bambi.server.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.cache.CacheManager;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.jedis.JedisClientConfiguration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import redis.clients.jedis.JedisPoolConfig;

import java.lang.reflect.Method;
import java.time.Duration;

/**
 * 描述：
 *
 * <pre>
 * HISTORY
 * ****************************************************************************
 *  ID     DATE          PERSON          REASON
 *  1      2023/2/26 7:14    Bambi        Create
 * ****************************************************************************
 * </pre>
 *
 * @author Bambi
 * @since 1.0
 */
@Configuration
// 控制自动装配顺序
@AutoConfigureBefore(RedisAutoConfiguration.class)
public class DIYRedisAutoConfiguration {
    private static Logger logger = LoggerFactory.getLogger(DIYRedisAutoConfiguration.class);
    @Value("${spring.redis.maxTotal}")
    private int maxTotal;

    @Value("${spring.redis.maxIdle}")
    private int maxIdle;

    @Value("${spring.redis.maxWaitMillis}")
    private long maxWaitMillis;

    @Value("${spring.redis.testOnBorrow}")
    private boolean testOnBorrow;

    @Value("${spring.redis.host}")
    private String host;

    @Value("${spring.redis.port}")
    private int port;

    @Value("${spring.redis.connTimeout}")
    private int connTimeout;
    @Value("${spring.redis.readTimeout}")
    private int readTimeout;

    @Value("${spring.redis.password:}")
    private String password;

    @Value("${spring.redis.database}")
    private int database;

    @Value("${spring.redis.minEvictableIdleTimeMillis}")
    private int minEvictableIdleTimeMillis;

    @Value("${spring.redis.softMinEvictableIdleTimeMillis}")
    private int softMinEvictableIdleTimeMillis;

    @Value("${spring.redis.timeBetweenEvictionRunsMillis}")
    private int timeBetweenEvictionRunsMillis;

    @Value("${spring.redis.numTestsPerEvictionRun}")
    private int numTestsPerEvictionRun;

    @Value("${spring.redis.blockWhenExhausted}")
    private boolean blockWhenExhausted;

    @Value("${spring.redis.testWhileIdle}")
    private boolean testWhileIdle;


    /**
     * jedis 连接池
     *
     * @return jedis 连接池
     */

    @Bean
    public JedisPoolConfig poolConfig()
    {
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(maxTotal);
        poolConfig.setMaxIdle(maxIdle);
        poolConfig.setMaxWaitMillis(maxWaitMillis);
        poolConfig.setTestOnBorrow(testOnBorrow);
        poolConfig.setMinEvictableIdleTimeMillis(minEvictableIdleTimeMillis);
        poolConfig.setMinEvictableIdleTimeMillis(minEvictableIdleTimeMillis);
        poolConfig.setSoftMinEvictableIdleTimeMillis(softMinEvictableIdleTimeMillis);
        poolConfig.setTimeBetweenEvictionRunsMillis(timeBetweenEvictionRunsMillis);
        poolConfig.setNumTestsPerEvictionRun(numTestsPerEvictionRun);
        poolConfig.setBlockWhenExhausted(blockWhenExhausted);
        poolConfig.setTestWhileIdle(testWhileIdle);
        return poolConfig;
    }

    /**
     * Jedis 连接工厂.
     *
     * @return 配置好的Jedis连接工厂
     */
    @Bean
    public RedisConnectionFactory connectionFactory(JedisPoolConfig jedisPoolConfig)
    {
        RedisStandaloneConfiguration configuration = new RedisStandaloneConfiguration();
        configuration.setHostName(host);
        configuration.setPort(port);
        //如果没有密码可以不设置，要注意密码要用RedisPassword类
        if (StringUtils.isNotEmpty(password))
        {
            configuration.setPassword(RedisPassword.of(password));
        }
        configuration.setDatabase(database);
        //获得默认的连接池构造
        //JedisConnectionFactory对于Standalone模式的没有（RedisStandaloneConfiguration，JedisPoolConfig）的构造函数，
        //所以用JedisClientConfiguration接口的builder方法实例化一个构造器，还得类型转换
        JedisClientConfiguration.DefaultJedisClientConfigurationBuilder builder =
                (JedisClientConfiguration.DefaultJedisClientConfigurationBuilder) JedisClientConfiguration.builder();
        builder.usePooling();
        //修改连接池配置
        builder.poolConfig(jedisPoolConfig);
        builder.connectTimeout(Duration.ofMillis(connTimeout));
        builder.readTimeout(Duration.ofMillis(readTimeout));
        //通过构造器来构造jedis客户端配置
        JedisClientConfiguration clientConfig = builder.build();


        return new JedisConnectionFactory(configuration, clientConfig);
    }


    @Bean(name = "redisTemplate")
    public RedisTemplate<Object, Object> redisTemplate(RedisConnectionFactory connectionFactory)
    {
        /*
         * Redis 序列化器.
         *
         * RedisTemplate 默认的系列化类是 JdkSerializationRedisSerializer,用JdkSerializationRedisSerializer序列化的话,
         * 被序列化的对象必须实现Serializable接口。在存储内容时，除了属性的内容外还存了其它内容在里面，总长度长，且不容易阅读。
         *
         * Jackson2JsonRedisSerializer 和 GenericJackson2JsonRedisSerializer，两者都能系列化成 json，
         * 但是后者会在 json 中加入 @class 属性，类的全路径包名，方便反系列化。前者如果存放了 List 则在反系列化的时候如果没指定
         * TypeReference 则会报错 java.util.LinkedHashMap cannot be cast to
         */
/*        RedisSerializer genericJackson2JsonRedisSerializer = new GenericJackson2JsonRedisSerializer();
        RedisSerializer stringRedisSerializer = new StringRedisSerializer();

        // 定义RedisTemplate，并设置连接工程
        RedisTemplate<Object, Object> redisTemplate = new RedisTemplate<>();

        // key 的序列化采用 StringRedisSerializer
        redisTemplate.setKeySerializer(stringRedisSerializer);
        redisTemplate.setHashKeySerializer(stringRedisSerializer);
        // value 值的序列化采用 GenericJackson2JsonRedisSerializer
        redisTemplate.setValueSerializer(stringRedisSerializer);
        redisTemplate.setHashValueSerializer(stringRedisSerializer);
        // 设置连接工厂
        redisTemplate.setConnectionFactory(connectionFactory);
        redisTemplate.setEnableTransactionSupport(false);
        return redisTemplate;*/
        RedisTemplate<Object, Object> template = new RedisTemplate<>();
        RedisSerializer<String> redisSerializer = new StringRedisSerializer();
        ObjectMapper om = new ObjectMapper();
        om.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        om.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL);
        Jackson2JsonRedisSerializer jackson2JsonRedisSerializer = new Jackson2JsonRedisSerializer(om,Object.class);
        // jackson2JsonRedisSerializer.setObjectMapper(om);
        template.setConnectionFactory(connectionFactory);
//key序列化方式
        template.setKeySerializer(redisSerializer);
//value序列化
        template.setValueSerializer(jackson2JsonRedisSerializer);
//value hashmap序列化
        template.setHashValueSerializer(jackson2JsonRedisSerializer);
        return template;
    }

    @Bean(name = "stringRedisTemplate")
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory connectionFactory)
    {
        // 定义RedisTemplate，并设置连接工程
        StringRedisTemplate redisTemplate = new StringRedisTemplate(connectionFactory);
        RedisSerializer stringRedisSerializer = new StringRedisSerializer();
        redisTemplate.setDefaultSerializer(stringRedisSerializer);
        redisTemplate.setEnableTransactionSupport(false);
        return redisTemplate;
    }

    @Bean
    public CacheManager initRedisCacheManager(RedisConnectionFactory connectionFactory)
    {
        RedisCacheManager.RedisCacheManagerBuilder builder = RedisCacheManager
                .RedisCacheManagerBuilder.fromConnectionFactory(connectionFactory);
        return builder.build();
    }
    @Bean
    public KeyGenerator keyGenerator()
    {
        return new KeyGenerator()
        {
            @Override
            public Object generate(Object o, Method method, Object... objects)
            {
                StringBuilder sb = new StringBuilder();
                sb.append(o.getClass().getName());
                sb.append(method.getName());
                for (Object obj : objects)
                {
                    sb.append(obj.toString());
                }
                return sb.toString();
            }
        };
    }

}
