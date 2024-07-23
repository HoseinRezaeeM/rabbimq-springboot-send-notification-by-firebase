@Configuration
public class RabbitConfigurer {

    private static final String topicExchangeName = "client-exchange";

    private static final String queueName = "notification-queue";

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        return new RabbitTemplate(connectionFactory);
    }

    @Bean
    public Queue queue1() {
        return new Queue(queueName);
    }
}
