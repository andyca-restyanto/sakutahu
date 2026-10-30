// filepath: /backend/src/main/java/com/example/app/modules/admin/entity/AdminRole.java
package com.example.app.modules.admin.entity;

/** OWNER = admin pertama (hanya satu); satu-satunya yang boleh mengundang & menonaktifkan admin. */
public enum AdminRole {
    OWNER,
    ADMIN
}
