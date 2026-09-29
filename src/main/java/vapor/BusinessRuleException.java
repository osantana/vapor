package vapor;

public class BusinessRuleException extends RuntimeException {
    private String messageKey;

    public BusinessRuleException(String messageKey) {
        super(messageKey);
        this.messageKey = messageKey;
    }

    public String getMessageKey() {
        return messageKey;
    }
}
