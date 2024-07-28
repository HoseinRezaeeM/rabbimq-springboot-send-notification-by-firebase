@Service 
public class NotificationPanelServiceImpl implements NotificationPanelService {

    private final NotificationPanelRepository notificationPanelRepository;

    private final GroupUserRepository groupUserRepository;
    private final Keycloak keycloak;
    private final SmsClient smsClient;
    private final NotificationTemplateRepository notificationTemplateRepository;

    private final RabbitTemplate rabbitTemplate;
    private final FirebaseMessaging firebaseMessaging;

    private final TokenRepository tokenRepository;

    public NotificationPanelServiceImpl(
        NotificationPanelRepository notificationPanelRepository,
        GroupUserRepository groupUserRepository,
        Keycloak keycloak,
        SmsClient smsClient,
        NotificationTemplateRepository notificationTemplateRepository,
        RabbitTemplate rabbitTemplate,
        FirebaseMessaging firebaseMessaging,
        TokenRepository tokenRepository
    ) {
        this.notificationPanelRepository = notificationPanelRepository;
        this.groupUserRepository = groupUserRepository;
        this.keycloak = keycloak;
        this.smsClient = smsClient;
        this.notificationTemplateRepository = notificationTemplateRepository;
        this.rabbitTemplate = rabbitTemplate;

        this.firebaseMessaging = firebaseMessaging;
        this.tokenRepository = tokenRepository;
    } implements NotificationPanelService {

    private final NotificationPanelRepository notificationPanelRepository;

    private final GroupUserRepository groupUserRepository;
    private final Keycloak keycloak;
    private final SmsClient smsClient;
    private final NotificationTemplateRepository notificationTemplateRepository;

    private final RabbitTemplate rabbitTemplate;
    private final FirebaseMessaging firebaseMessaging;

    private final TokenRepository tokenRepository;

    public NotificationPanelServiceImpl(
        NotificationPanelRepository notificationPanelRepository,
        GroupUserRepository groupUserRepository,
        Keycloak keycloak,
        SmsClient smsClient,
        NotificationTemplateRepository notificationTemplateRepository,
        RabbitTemplate rabbitTemplate,
        FirebaseMessaging firebaseMessaging,
        TokenRepository tokenRepository
    ) {
        this.notificationPanelRepository = notificationPanelRepository;
        this.groupUserRepository = groupUserRepository;
        this.keycloak = keycloak;
        this.smsClient = smsClient;
        this.notificationTemplateRepository = notificationTemplateRepository;
        this.rabbitTemplate = rabbitTemplate;

        this.firebaseMessaging = firebaseMessaging;
        this.tokenRepository = tokenRepository;
    }
@Override
    public ProjectResponse sendNotificationBySMSAndFireBase(NotificationPanelRequest request) {
        NotificationPanel panel = new NotificationPanel();
        panel.setSendAt(LocalDate.now());
        panel.setNotificationTemplate(new NotificationTemplate(request.getTemplateId()));
        panel.setType(request.getType());
        List<GroupsUser> users = groupUserRepository.findByGroupId(request.getGroupId());
        panel.setGroupsType(new GroupsType(request.getGroupId()));
        panel.setUserIds(users.stream().map(GroupsUser::getUserId).collect(Collectors.toList()));
        notificationPanelRepository.save(panel);
        Optional<NotificationTemplate> template = notificationTemplateRepository.findById(request.getTemplateId());
        String message = template.get().getBody(); //+ template.get().getUrl();

        if (request.getType().equals(PublishType.sms)) {
            List<String> userIds = panel.getUserIds();
            for (String id : userIds) {
                String username = keycloak.realm("si").users().get(id).toRepresentation().getUsername();
                try {
                    smsClient.sendOTP(username, message);
                } catch (Exception e) {
                    return new ProjectResponse("500", "سرویس اعلان با مشکل روبرو شده است .");
                }
            }
        } else if (request.getType().equals(PublishType.both)) {
            List<String> userIds = panel.getUserIds();
            for (String id : userIds) {
                UserResource resource = keycloak.realm("si").users().get(id);
                String username = resource.toRepresentation().getUsername();
                try {
                    smsClient.sendOTP(username, message);
                } catch (Exception e) {
                    return new ProjectResponse("500", "سرویس اعلان با مشکل روبرو شده است .");
                }

                NotificationRequet notificationRequet = new NotificationRequet(id, NotificationType.REGISTER);
                ObjectMapper objectMapper = new ObjectMapper();
                try {
                    rabbitTemplate.convertAndSend("notification-queue", objectMapper.writeValueAsString(notificationRequet));
                } catch (JsonProcessingException e) {
                    return new ProjectResponse("500", "سرویس اعلان با مشکل روبرو شده است .");
                }
            }
        } else if (request.getType().equals(PublishType.firebase)) {
            List<String> userIds = panel.getUserIds();
            NotificationRequet notificationRequet = null;
            for (String id : userIds) {
                notificationRequet = new NotificationRequet(id, NotificationType.REGISTER);
                // sendNotificationByTokenAndRabit(notificationRequet);
                ObjectMapper objectMapper = new ObjectMapper();
                try {
                    rabbitTemplate.convertAndSend("notification-queue", objectMapper.writeValueAsString(notificationRequet));
                } catch (JsonProcessingException e) {
                    return new ProjectResponse("500", "سرویس اعلان با مشکل روبرو شده است .");
                }
            }
        }
        return new ProjectResponse("200", "اعلان ارسال شد .");
    }
