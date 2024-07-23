@Component
@RequiredArgsConstructor
public class ReceiverNotificationQueue {

    private final NotificationTemplateRepository notificationTemplateRepository;
    private final FirebaseMessaging firebaseMessaging;

    private final TokenRepository tokenRepository;

    @RabbitListener(queues = "notification-queue")
    public void handleMessageFromQueue(String notificationRequet) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            sendNotificationByTokenAndRabit(objectMapper.readValue(notificationRequet, NotificationRequet.class));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public String sendNotificationByTokenAndRabit(NotificationRequet notificationRequet) {
        NotificationTemplate notificationTemplate = notificationTemplateRepository.findByType(notificationRequet.getType());
        Token token = tokenRepository.findByUserId(notificationRequet.getUserId());
        Map<String, String> data = new HashMap<>();
        data.put("message", notificationTemplate.getBody());
        data.put("title", notificationTemplate.getTitle());
        data.put("image", notificationTemplate.getImage());
        data.put("url", notificationTemplate.getUrl());
        Message message = Message.builder().setToken(token.getToken()).putAllData(data).build();
        try {
            firebaseMessaging.send(message);
            return "success";
        } catch (Exception e) {
            throw new RuntimeException();
        }
    }
