package vapor.model;

public enum Platform {
    WINDOWS("Windows"),
    MACOS("macOS"),
    LINUX("Linux");

    private final String label;

    Platform(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    // The value used in the database and in form fields: "windows", "macos", "linux".
    public String getCode() {
        return name().toLowerCase();
    }

    public static Platform fromCode(String code) {
        return valueOf(code.toUpperCase());
    }
}
