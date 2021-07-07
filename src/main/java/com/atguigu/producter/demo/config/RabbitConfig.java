package com.atguigu.producter.demo.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.Nullable;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Configuration
public class RabbitConfig {
    @Autowired
    RabbitTemplate rabbitTemplate;
    @PostConstruct
    public void init(){
//        设置回调,消息是否发送到交换机，确认回调
        rabbitTemplate.setConfirmCallback((@Nullable CorrelationData correlationData, boolean ack, @Nullable String cause)->{
            if(ack){
                System.out.println("消息到达交换机");
            }else{
                System.out.println(cause);
            }

        });
//        消息是否发送到队列
        rabbitTemplate.setReturnCallback((Message message, int replyCode, String replyText, String exchange, String routingKey)->{
            log.error("消息没有到达队列{},{},{},{},{}",replyCode,replyText,exchange,routingKey);
        });
    }
    /**
     * 业务交换机：spring_test_exchange2
     */
    @Bean
    public Exchange exchange(){
//        return new TopicExchange("spring_test_exchange2",true,false);
      return  ExchangeBuilder.topicExchange("spring_test_exchange2").durable(true).build();
    }
    /**
     * 业务队列：spring_test_queue2
     * 指定死信交换机和死信队列的key
     */
    @Bean
    public Queue queue(){
       Map<String, Object> map = new HashMap<>();
       map.put("x-dead-letter-exchange","spring_dead_exchange");
       map.put("x-dead-letter-routing-key", "msg.dead");
//        return new Queue("spring_test_queue2", true, false, false,map);
        return QueueBuilder.durable("spring_test_queue2").withArguments(map).build();
    }
    @Bean
    public Queue ttlQueue(){
        Map<String, Object> map = new HashMap<>();
        map.put("x-message-ttl",60000);
        map.put("x-dead-letter-exchange","spring_dead_exchange");
        map.put("x-dead-letter-routing-key", "msg.dead");
//        return new Queue("spring_test_queue2", true, false, false,map);
//        return QueueBuilder.durable("spring_test_queue2").withArguments(map).build();
        return QueueBuilder.durable("spring_ttl_queue").withArguments(map).build();
    }
    /**
     * 把业务队列binding到交换机：msg.test
     */
    @Bean
    public Binding binding(Queue ttlQueue,Exchange exchange){
//        return new Binding("spring_test_queue2",Binding.DestinationType.QUEUE,"spring_test_exchange2","msg.test",null);
        return BindingBuilder.bind(ttlQueue).to(exchange).with("msg.ttl").noargs();
    }
    /**
     * 死信交换机：spring_dead_exchange
     */
    @Bean
    public Exchange deadExchange(){
        return ExchangeBuilder.topicExchange("spring_dead_exchange").durable(true).build();
    }
    /**
     * 死信队列：spring_dead_queue
     */
    @Bean
    public Queue deadqueue() {
        return new Queue("spring_dead_queue", true, false, false);
    }
    /**
     * 把死信队列binding到死信交换机：msg.dead
     */
    @Bean
    public Binding deadBinding(){
        return new Binding("spring_dead_queue",Binding.DestinationType.QUEUE,"spring_dead_exchange","msg.dead",null);
    }


}

