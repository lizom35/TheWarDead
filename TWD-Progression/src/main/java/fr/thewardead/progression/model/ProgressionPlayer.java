package fr.thewardead.progression.model;

import java.util.UUID;

public class ProgressionPlayer {

    private final UUID    uuid;
    private       String  name;
    private       JobClass jobClass;  // null = pas encore choisi
    private       long    xp;
    private       int     level;     // 1-10, mis à jour par ProgressionManager

    public ProgressionPlayer(UUID uuid, String name, JobClass jobClass, long xp, int level) {
        this.uuid     = uuid;
        this.name     = name;
        this.jobClass = jobClass;
        this.xp       = xp;
        this.level    = Math.max(1, Math.min(10, level));
    }

    public UUID     getUuid()     { return uuid; }
    public String   getName()     { return name; }
    public void     setName(String name) { this.name = name; }

    public JobClass getJobClass() { return jobClass; }
    public void     setJobClass(JobClass jobClass) { this.jobClass = jobClass; }
    public boolean  hasClass()    { return jobClass != null; }

    public long     getXp()       { return xp; }
    public void     addXp(long amount) { xp += amount; }

    public int      getLevel()    { return level; }
    public void     setLevel(int level) { this.level = Math.max(1, Math.min(10, level)); }
}
