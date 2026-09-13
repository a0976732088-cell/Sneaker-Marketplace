package com.project.sneakermarket.auction.service;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.project.sneakermarket.auction.controller.AuctionAdminController;
import com.project.sneakermarket.auction.dto.AuctionAdminBatchResponse;
import com.project.sneakermarket.auction.dto.PageData;
import com.project.sneakermarket.auction.exception.AuctionConflictException;
import com.project.sneakermarket.auction.exception.AuctionExceptionHandler;
import com.project.sneakermarket.auction.exception.AuctionUnauthorizedException;
import com.project.sneakermarket.common.exception.ForbiddenException;
import com.project.sneakermarket.common.exception.GlobalExceptionHandler;

class AuctionAdminAccessTests {
    private AuctionReferenceService references;
    private AuctionAdminQueryService queries;
    private AuctionAdminMutationService mutations;
    private AuctionAdminBatchService batches;
    private MockMvc mvc;

    @BeforeEach
    void setup() {
        references = mock(AuctionReferenceService.class);
        queries = mock(AuctionAdminQueryService.class);
        mutations = mock(AuctionAdminMutationService.class);
        batches = mock(AuctionAdminBatchService.class);
        var resolver = new AuctionSessionMemberResolver(references);
        var controller = new AuctionAdminController(queries, mutations, batches, resolver);
        mvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .setControllerAdvice(new AuctionExceptionHandler(), new GlobalExceptionHandler())
                .build();
    }

    @Test
    void anonymousAdminApiReturns401WithoutEnteringService() throws Exception {
        mvc.perform(put("/api/admin/auctions/12/remove")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"違規\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
        mvc.perform(put("/api/admin/auctions/12/restore"))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/admin/auctions/12"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
        verifyNoInteractions(mutations, queries, batches, references);
    }

    @Test
    void activeMemberWithoutAdminRoleReturns403() throws Exception {
        MockHttpSession session = session(8L);
        doThrow(new ForbiddenException("需要管理員權限")).when(references).requireAdminMember(8L);

        mvc.perform(put("/api/admin/auctions/12/restore").session(session))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("需要管理員權限"));
        verifyNoInteractions(mutations);
    }

    @Test
    void deletedOrMissingMemberWithStaleSessionReturns401() throws Exception {
        MockHttpSession session = session(9L);
        doThrow(new AuctionUnauthorizedException()).when(references).requireAdminMember(9L);

        mvc.perform(get("/api/admin/auctions").session(session))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(queries);
    }

    @Test
    void adminCanListRemoveRestoreDeleteAndBatchWithoutClientIdentity() throws Exception {
        MockHttpSession session = session(7L);
        doNothing().when(references).requireAdminMember(7L);
        when(queries.search(any(), any())).thenReturn(new PageData<>(List.of(), 0, 20, 0, 0));
        when(batches.removeAuctions(anyList(), eq(7L), eq("批次原因")))
                .thenReturn(new AuctionAdminBatchResponse(2, 2, 0, List.of()));
        when(batches.restoreAuctions(anyList(), eq(7L)))
                .thenReturn(new AuctionAdminBatchResponse(2, 2, 0, List.of()));

        mvc.perform(get("/api/admin/auctions").session(session)
                        .param("keyword", "Jordan").param("managementState", "REMOVED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.page").value(0));
        mvc.perform(get("/api/admin/auctions/12").session(session))
                .andExpect(status().isOk());
        mvc.perform(put("/api/admin/auctions/12/remove").session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"下架原因\"}"))
                .andExpect(status().isOk());
        mvc.perform(put("/api/admin/auctions/12/restore").session(session))
                .andExpect(status().isOk());
        mvc.perform(put("/api/admin/auctions/12/delete").session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"刪除原因\"}"))
                .andExpect(status().isOk());
        mvc.perform(put("/api/admin/auctions/batch/remove").session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"auctionIds\":[11,12],\"reason\":\"批次原因\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.successCount").value(2));
        mvc.perform(put("/api/admin/auctions/batch/restore").session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"auctionIds\":[11,12]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.successCount").value(2));

        verify(queries).detail(12L);
        verify(mutations).removeAuction(12L, 7L, "下架原因");
        verify(mutations).restoreAuction(12L, 7L);
        verify(mutations).deleteAuction(12L, 7L, "刪除原因");
        verify(batches).removeAuctions(List.of(11L, 12L), 7L, "批次原因");
        verify(batches).restoreAuctions(List.of(11L, 12L), 7L);
    }

    @Test
    void restoreConflictIsReturnedAsHttp409() throws Exception {
        MockHttpSession session = session(7L);
        doNothing().when(references).requireAdminMember(7L);
        doThrow(new AuctionConflictException("已刪除的競標無法重新上架"))
                .when(mutations).restoreAuction(12L, 7L);

        mvc.perform(put("/api/admin/auctions/12/restore").session(session))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("已刪除的競標無法重新上架"));
    }
    @Test
    void requestsRejectBlankLongAndClientControlledFields() throws Exception {
        MockHttpSession session = session(7L);
        doNothing().when(references).requireAdminMember(7L);
        String tooLong = "x".repeat(501);

        for (String body : List.of(
                "{\"reason\":\"   \"}",
                "{\"reason\":\"" + tooLong + "\"}",
                "{\"reason\":\"原因\",\"adminId\":99}",
                "{\"reason\":\"原因\",\"sellerId\":99}")) {
            mvc.perform(put("/api/admin/auctions/12/remove").session(session)
                            .contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest());
        }
        verifyNoInteractions(mutations);
    }

    @Test
    void batchValidatesIdsMaximumSizeAndClientControlledFieldsBeforeProcessing() throws Exception {
        MockHttpSession session = session(7L);
        doNothing().when(references).requireAdminMember(7L);
        String overLimit = java.util.stream.LongStream.rangeClosed(1, 101)
                .mapToObj(Long::toString).collect(java.util.stream.Collectors.joining(","));

        for (String body : List.of(
                "{\"auctionIds\":[],\"reason\":\"原因\"}",
                "{\"auctionIds\":[0],\"reason\":\"原因\"}",
                "{\"auctionIds\":[" + overLimit + "],\"reason\":\"原因\"}")) {
            mvc.perform(put("/api/admin/auctions/batch/delete").session(session)
                            .contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest());
        }
        mvc.perform(put("/api/admin/auctions/batch/restore").session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"auctionIds\":[1],\"adminId\":99}"))
                .andExpect(status().isBadRequest());
        mvc.perform(put("/api/admin/auctions/batch/restore").session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"auctionIds\":[" + overLimit + "]}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(batches);
    }

    private MockHttpSession session(Long memberId) {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("memberId", memberId);
        return session;
    }
}
