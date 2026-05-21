package com.joyopi.point.temporal.activity;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

@ActivityInterface
public interface PointActivity {

    @ActivityMethod
    void usePoint(Long userId, Long amount);

    @ActivityMethod
    void restorePoint(Long userId, Long amount);
}
