package cn.smilefamily.moduleexample;

public interface PermissionChecker {
    boolean hasPermission(PermissionSubject subject, User user);
}
