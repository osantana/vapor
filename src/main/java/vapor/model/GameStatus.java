package vapor.model;

public enum GameStatus {
    PENDING("Em análise"),
    APPROVED("Aprovado"),
    REJECTED("Rejeitado");

    private final String label;

    GameStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
