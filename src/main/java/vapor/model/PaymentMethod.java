package vapor.model;

public enum PaymentMethod {
    CREDIT_CARD("Cartão de crédito"),
    PIX("PIX");

    private final String label;

    PaymentMethod(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
