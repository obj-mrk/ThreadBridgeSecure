package mrk.infrastructure.event.payload;

import java.util.Set;

public class SecureMessagesExpiredPayload {

    private int expiredCount;
    private Set<Long> recipientIds;

    public SecureMessagesExpiredPayload() {
    }

    public int getExpiredCount() {
        return expiredCount;
    }

    public void setExpiredCount(int expiredCount) {
        this.expiredCount = expiredCount;
    }

    public Set<Long> getRecipientIds() {
        return recipientIds;
    }

    public void setRecipientIds(Set<Long> recipientIds) {
        this.recipientIds = recipientIds;
    }
}