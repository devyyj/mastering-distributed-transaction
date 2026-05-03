package com.joyopi.point.controller;

import com.joyopi.point.common.response.ApiResponse;
import com.joyopi.point.controller.dto.PointRequest;
import com.joyopi.point.service.PointService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/points")
@RequiredArgsConstructor
public class PointController {
    private final PointService pointService;

    @PostMapping("/use")
    public ApiResponse<Void> usePoint(@RequestBody PointRequest request) {
        pointService.usePoint(request.getUserId(), request.getAmount());
        return ApiResponse.success(null);
    }
}
