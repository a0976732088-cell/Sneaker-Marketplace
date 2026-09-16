package com.project.sneakermarket.auction.entity;

import java.time.LocalDateTime;
import com.project.sneakermarket.member.entity.Member;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** 結標通知工作與信箱快照；不保存信件本文或 SMTP 錯誤原文。 */
@Entity
@Table(name = "auction_email_logs", schema = "dbo", uniqueConstraints =
        @UniqueConstraint(name = "UQ_auction_email_logs_recipient_type",
                columnNames = {"auction_id", "recipient_member_id", "email_type"}), indexes =
        @Index(name = "IX_auction_email_logs_retry", columnList = "send_status, retry_count, created_at"), check = {
        @CheckConstraint(name = "CK_auction_email_logs_role", constraint = "recipient_role IN ('SELLER', 'WINNER', 'BIDDER')"),
        @CheckConstraint(name = "CK_auction_email_logs_type", constraint = "email_type IN ('SOLD_SELLER', 'UNSOLD_SELLER', 'WON', 'LOST', 'ORDER_ABANDONED_BUYER', 'ORDER_ABANDONED_SELLER')"),
        @CheckConstraint(name = "CK_auction_email_logs_status", constraint = "send_status IN ('PENDING', 'SENT', 'FAILED')"),
        @CheckConstraint(name = "CK_auction_email_logs_retry_count", constraint = "retry_count >= 0")
})
@Getter
@Setter
@NoArgsConstructor
public class AuctionEmailLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long emailLogId;

    @Column(name = "auction_id", nullable = false, updatable = false)
    private Long auctionId;
    @Column(name = "recipient_member_id", nullable = false, updatable = false)
    private Long recipientMemberId;

    // 沿用 Result/Order scalar 寫入及唯讀 FK 關聯，不 cascade 主資料。
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "auction_id", nullable = false, insertable = false, updatable = false,
            foreignKey = @ForeignKey(name = "FK_auction_email_logs_auctions"))
    private Auction auction;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipient_member_id", nullable = false, insertable = false, updatable = false,
            foreignKey = @ForeignKey(name = "FK_auction_email_logs_members"))
    private Member recipient;

    @Column(nullable = false, updatable = false, length = 100, columnDefinition = "NVARCHAR(100)")
    private String recipientEmail;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false, length = 20, columnDefinition = "NVARCHAR(20)")
    private AuctionEmailRecipientRole recipientRole;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false, length = 40, columnDefinition = "NVARCHAR(40)")
    private AuctionEmailType emailType;
    @Column(nullable = false, updatable = false, length = 200, columnDefinition = "NVARCHAR(200)")
    private String subject;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20, columnDefinition = "NVARCHAR(20)")
    private AuctionEmailSendStatus sendStatus;
    /** 已失敗寄送次數，成功不歸零。 */
    @Column(nullable = false)
    private Integer retryCount;
    @Column(length = 1000, columnDefinition = "NVARCHAR(1000)")
    private String lastError;
    @Column(nullable = false, updatable = false, columnDefinition = "DATETIME2(0)")
    private LocalDateTime createdAt;
    @Column(nullable = false, columnDefinition = "DATETIME2(0)")
    private LocalDateTime updatedAt;
    @Column(columnDefinition = "DATETIME2(0)")
    private LocalDateTime sentAt;
}
