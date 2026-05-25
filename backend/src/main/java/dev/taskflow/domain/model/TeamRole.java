package dev.taskflow.domain.model;

public enum TeamRole {
    OWNER,
    MEMBER,
    VIEWER;

    public boolean canManageMembers() {
        return this == OWNER;
    }

    public boolean canWriteTasks() {
        return this == OWNER || this == MEMBER;
    }

    public boolean canRead() {
        return true;
    }
}
