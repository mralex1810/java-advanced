package info.kgeorgiy.ja.chulkov.stat.statistics;

public enum Gender {
    MASCULINE("masculine"),
    NEUTER("neuter"),
    FEMININE("feminine");

    private final String key;
    Gender(final String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }
}
