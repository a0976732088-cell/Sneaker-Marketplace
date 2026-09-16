package com.project.sneakermarket.auction.config;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.security.Principal;
import java.util.HashMap;

import org.junit.jupiter.api.Test;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.socket.WebSocketHandler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

class AuctionWebSocketHandshakeHandlerTests {

    private final AuctionWebSocketHandshakeHandler handler =
            new AuctionWebSocketHandshakeHandler();

    @Test
    void validLongSessionMemberIdBecomesWebSocketPrincipal() {
        MockHttpServletRequest servletRequest = new MockHttpServletRequest();
        servletRequest.getSession().setAttribute("memberId", 123L);

        Principal principal = handler.determineUser(
                new ServletServerHttpRequest(servletRequest),
                mock(WebSocketHandler.class),
                new HashMap<>()
        );

        assertEquals("123", principal.getName());
    }

    @Test
    void missingSessionKeepsPublicHandshakeAnonymous() {
        MockHttpServletRequest servletRequest = new MockHttpServletRequest();

        Principal principal = assertDoesNotThrow(() -> handler.determineUser(
                new ServletServerHttpRequest(servletRequest),
                mock(WebSocketHandler.class),
                new HashMap<>()
        ));

        assertNull(principal);
        assertNull(servletRequest.getSession(false));
    }

    @Test
    void invalidSessionMemberIdsNeverCreatePrincipal() {
        for (Object memberId : new Object[]{0L, -1L, 123, "123"}) {
            MockHttpServletRequest servletRequest = new MockHttpServletRequest();
            servletRequest.getSession().setAttribute("memberId", memberId);

            Principal principal = handler.determineUser(
                    new ServletServerHttpRequest(servletRequest),
                    mock(WebSocketHandler.class),
                    new HashMap<>()
            );

            assertNull(principal, () -> "unexpected principal for memberId=" + memberId);
        }
    }

    @Test
    void concurrentlyInvalidatedSessionKeepsPublicHandshakeAnonymous() {
        HttpServletRequest servletRequest = mock(HttpServletRequest.class);
        HttpSession session = mock(HttpSession.class);
        when(servletRequest.getSession(false)).thenReturn(session);
        when(session.getAttribute("memberId")).thenThrow(new IllegalStateException("invalidated"));

        Principal principal = assertDoesNotThrow(() -> handler.determineUser(
                new ServletServerHttpRequest(servletRequest),
                mock(WebSocketHandler.class),
                new HashMap<>()
        ));

        assertNull(principal);
    }
}
