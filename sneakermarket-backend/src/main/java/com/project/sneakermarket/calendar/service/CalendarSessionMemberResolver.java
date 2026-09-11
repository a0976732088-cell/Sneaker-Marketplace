package com.project.sneakermarket.calendar.service;
//在行事曆模組內讀取 Session

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class CalendarSessionMemberResolver {

    public Long requireMemberId(HttpServletRequest request) {

        HttpSession session = request.getSession(false);

        if (session == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "請先登入會員"
            );
        }

        Object value = session.getAttribute("memberId");

        if (!(value instanceof Long memberId) || memberId <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "登入狀態已失效"
            );
        }

        return memberId;
    }
}