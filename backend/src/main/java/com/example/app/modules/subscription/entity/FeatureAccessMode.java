// filepath: /backend/src/main/java/com/example/app/modules/subscription/entity/FeatureAccessMode.java
package com.example.app.modules.subscription.entity;

/** Siapa yang boleh memakai fitur. ALL = Free (dengan kuota) & VIP; VIP_ONLY = hanya VIP. */
public enum FeatureAccessMode {
    ALL,
    VIP_ONLY
}
