package com.project.sneakermarket.member.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.sneakermarket.common.exception.BusinessException;
import com.project.sneakermarket.common.exception.ResourceNotFoundException;

import com.project.sneakermarket.coupon.service.CouponService;

import com.project.sneakermarket.member.dto.ChangePasswordRequest;
import com.project.sneakermarket.member.dto.LoginRequest;
import com.project.sneakermarket.member.dto.MemberCreateRequest;
import com.project.sneakermarket.member.dto.MemberResponse;
import com.project.sneakermarket.member.dto.MemberUpdateRequest;

import com.project.sneakermarket.member.entity.Member;
import com.project.sneakermarket.member.repository.MemberRepository;

import lombok.RequiredArgsConstructor;


@Service
@RequiredArgsConstructor
public class MemberService {


    // =====================================================
    // Repository / Service
    // =====================================================

    private final MemberRepository memberRepository;

    private final PasswordEncoder passwordEncoder;


    /*
     * ★ Coupon 功能
     *
     * 用來在會員註冊成功之後
     * 自動發放新會員 100 元折價券。
     */
    private final CouponService couponService;



    // =====================================================
    // CREATE / 會員註冊
    // =====================================================
    @Transactional
    public MemberResponse createMember(
            MemberCreateRequest request
    ) {


        // =================================================
        // 清理輸入資料
        // =================================================

        String account =
                request.getAccount().trim();


        String email =
                request.getEmail().trim();


        String phone =
                request.getPhone().trim();


        String name =
                request.getName().trim();


        String address =
                normalizeNullableText(
                        request.getAddress()
                );


        // =================================================
        // 帳號重複檢查
        //
        // ★ Soft Delete 規則
        //
        // 只檢查 deleted_at IS NULL 的會員。
        //
        // 例如：
        //
        // member_id = 10
        // account = 789
        // deleted_at = 2026-09-15
        //
        // 這筆不算目前有效會員，
        // 所以 789 可以重新註冊。
        // =================================================

        if (
                memberRepository
                        .existsByAccountAndDeletedAtIsNull(
                                account
                        )
        ) {

            throw new BusinessException(
                    "帳號已存在"
            );
        }


        // =================================================
        // Email 重複檢查
        //
        // 同樣只檢查有效會員。
        // =================================================

        if (
                memberRepository
                        .existsByEmailAndDeletedAtIsNull(
                                email
                        )
        ) {

            throw new BusinessException(
                    "Email 已存在"
            );
        }


        // =================================================
        // 手機驗證
        // =================================================

        validatePhone(
                phone
        );


        // =================================================
        // 密碼驗證
        // =================================================

        if (
                request.getPassword() == null
                        ||
                request.getPassword().length() < 8
        ) {

            throw new BusinessException(
                    "密碼至少需要 8 碼"
            );
        }


        // =================================================
        // 建立新的 Member Entity
        //
        // ★ 即使 account 曾經使用過，
        //    Soft Delete 後重新註冊
        //    仍然會建立「新的 member_id」。
        // =================================================

        Member member =
                Member.builder()

                        .account(
                                account
                        )

                        .passwordHash(
                                passwordEncoder.encode(
                                        request.getPassword()
                                )
                        )

                        .name(
                                name
                        )

                        .email(
                                email
                        )

                        .phone(
                                phone
                        )

                        .address(
                                address
                        )

                        .role(
                                "ROLE_MEMBER"
                        )

                        /*
                         * 新會員一定是有效會員
                         */
                        .deletedAt(
                                null
                        )

                        .build();



        // =================================================
        // 儲存新會員
        // =================================================

        Member savedMember =
                memberRepository.save(
                        member
                );



        // =================================================
        // ★★★★★ 這就是這次漏掉的地方 ★★★★★
        //
        // 會員註冊成功之後，
        // 自動發放一張：
        //
        // 新會員 100 元折價券
        //
        // Coupon 會綁定：
        //
        // savedMember.memberId
        //
        // 而不是綁 account。
        //
        // 所以：
        //
        // 舊 789 → member_id = 10
        //
        // 新 789 → member_id = 12
        //
        // 新 Coupon 會屬於 member_id = 12。
        // =================================================

        couponService.issueWelcomeCoupon(
                savedMember
        );



        // =================================================
        // 回傳新會員
        // =================================================

        return MemberResponse.from(
                savedMember
        );
    }



    // =====================================================
    // READ ALL
    //
    // ADMIN 查全部會員
    //
    // ★ 只顯示 deleted_at IS NULL
    // =====================================================
    @Transactional(readOnly = true)
    public List<MemberResponse> getAllMembers() {


        return memberRepository

                .findAllByDeletedAtIsNullOrderByMemberIdAsc()

                .stream()

                .map(
                        MemberResponse::from
                )

                .toList();
    }



    // =====================================================
    // READ ONE
    //
    // 只允許取得有效會員
    // =====================================================
    @Transactional(readOnly = true)
    public MemberResponse getMember(
            Long id
    ) {


        Member member =
                findActiveMemberOrThrow(
                        id
                );


        return MemberResponse.from(
                member
        );
    }



    // =====================================================
    // UPDATE
    //
    // Admin 修改會員
    // 或
    // 會員修改自己的資料
    // =====================================================
    @Transactional
    public MemberResponse updateMember(

            Long id,

            MemberUpdateRequest request
    ) {


        /*
         * 只能修改有效會員。
         */
        Member member =
                findActiveMemberOrThrow(
                        id
                );


        // =================================================
        // 姓名
        // =================================================

        if (
                request.getName() != null
        ) {


            String name =
                    request.getName()
                            .trim();


            if (
                    name.isBlank()
            ) {

                throw new BusinessException(
                        "姓名不可空白"
                );
            }


            member.setName(
                    name
            );
        }



        // =================================================
        // Email
        // =================================================

        if (
                request.getEmail() != null
        ) {


            String email =
                    request.getEmail()
                            .trim();


            if (
                    email.isBlank()
            ) {

                throw new BusinessException(
                        "Email 不可空白"
                );
            }


            /*
             * 修改 Email 時，
             *
             * 只跟目前有效會員比較。
             *
             * 已 Soft Delete 的會員
             * 不會阻止 Email 再次使用。
             */
            if (
                    !email.equalsIgnoreCase(
                            member.getEmail()
                    )

                            &&

                    memberRepository
                            .existsByEmailAndDeletedAtIsNull(
                                    email
                            )
            ) {

                throw new BusinessException(
                        "Email 已被其他會員使用"
                );
            }


            member.setEmail(
                    email
            );
        }



        // =================================================
        // 手機
        // =================================================

        if (
                request.getPhone() != null
        ) {


            String phone =
                    request.getPhone()
                            .trim();


            validatePhone(
                    phone
            );


            member.setPhone(
                    phone
            );
        }



        // =================================================
        // 地址
        // =================================================

        if (
                request.getAddress() != null
        ) {


            member.setAddress(

                    normalizeNullableText(
                            request.getAddress()
                    )

            );
        }



        return MemberResponse.from(
                member
        );
    }



    // =====================================================
    // 修改自己的密碼
    // =====================================================
    @Transactional
    public void changePassword(

            Long memberId,

            ChangePasswordRequest request
    ) {


        /*
         * 已刪除會員不能修改密碼。
         */
        Member member =
                findActiveMemberOrThrow(
                        memberId
                );


        // =================================================
        // 驗證舊密碼
        // =================================================

        if (
                !passwordEncoder.matches(

                        request.getCurrentPassword(),

                        member.getPasswordHash()
                )
        ) {

            throw new BusinessException(
                    "目前密碼不正確"
            );
        }



        // =================================================
        // 新密碼至少 8 碼
        // =================================================

        if (
                request.getNewPassword() == null

                        ||

                request.getNewPassword()
                        .length() < 8
        ) {

            throw new BusinessException(
                    "新密碼至少需要 8 碼"
            );
        }



        // =================================================
        // 確認兩次密碼
        // =================================================

        if (
                !request.getNewPassword()
                        .equals(
                                request.getConfirmPassword()
                        )
        ) {

            throw new BusinessException(
                    "兩次輸入的新密碼不一致"
            );
        }



        // =================================================
        // 更新 BCrypt 密碼
        // =================================================

        member.setPasswordHash(

                passwordEncoder.encode(
                        request.getNewPassword()
                )

        );
    }



    // =====================================================
    // DELETE / Soft Delete
    //
    // ★ 不真的 DELETE Member。
    // =====================================================
    @Transactional
    public void deleteMember(
            Long id
    ) {


        /*
         * 只能刪除目前有效的會員。
         */
        Member member =
                findActiveMemberOrThrow(
                        id
                );


        // =================================================
        // ★ Soft Delete
        //
        // 原本錯誤作法：
        //
        // memberRepository.delete(member);
        //
        //
        // 現在：
        //
        // 只紀錄刪除時間。
        // =================================================

        member.setDeletedAt(
                LocalDateTime.now()
        );


        /*
         * 注意：
         *
         * ★ 不要在這裡呼叫：
         *
         * couponService.deleteCouponsByMemberId(id);
         *
         *
         * 因為現在我們是 Soft Delete。
         *
         * 舊 Member / Coupon 都應該保留下來，
         * 避免歷史資料、訂單、競標資料
         * 關聯被破壞。
         */


        /*
         * JPA Dirty Checking 本來就會 UPDATE。
         *
         * 保留 save 比較容易理解。
         */
        memberRepository.save(
                member
        );
    }



    // =====================================================
    // LOGIN
    //
    // ★ 只允許 deleted_at IS NULL 會員登入
    // =====================================================
    @Transactional(readOnly = true)
    public MemberResponse login(
            LoginRequest request
    ) {


        if (
                request.getAccount() == null

                        ||

                request.getPassword() == null
        ) {

            throw new BusinessException(
                    "帳號或密碼錯誤"
            );
        }


        String account =
                request.getAccount()
                        .trim();


        // =================================================
        // ★ Soft Delete Login 判斷
        // =================================================

        Member member =
                memberRepository

                        .findByAccountAndDeletedAtIsNull(
                                account
                        )

                        .orElseThrow(

                                () ->

                                        new BusinessException(
                                                "帳號或密碼錯誤"
                                        )
                        );



        // =================================================
        // BCrypt 驗證
        // =================================================

        boolean passwordMatches =
                passwordEncoder.matches(

                        request.getPassword(),

                        member.getPasswordHash()
                );



        if (
                !passwordMatches
        ) {

            throw new BusinessException(
                    "帳號或密碼錯誤"
            );
        }



        return MemberResponse.from(
                member
        );
    }



    // =====================================================
    // 確認會員是否 ADMIN
    // =====================================================
    @Transactional(readOnly = true)
    public boolean isAdmin(
            Long memberId
    ) {


        /*
         * 已刪除會員視為不存在。
         */
        Member member =
                findActiveMemberOrThrow(
                        memberId
                );


        return "ROLE_ADMIN"
                .equals(
                        member.getRole()
                );
    }



    // =====================================================
    // 共用：
    //
    // 查詢「有效會員」
    //
    // deleted_at 必須為 NULL
    // =====================================================
    private Member findActiveMemberOrThrow(
            Long id
    ) {


        return memberRepository

                .findByMemberIdAndDeletedAtIsNull(
                        id
                )

                .orElseThrow(

                        () ->

                                new ResourceNotFoundException(
                                        "找不到會員"
                                )
                );
    }



    // =====================================================
    // 手機格式驗證
    // =====================================================
    private void validatePhone(
            String phone
    ) {


        if (
                phone == null

                        ||

                !phone.matches(
                        "^09\\d{8}$"
                )
        ) {

            throw new BusinessException(
                    "手機格式錯誤，需為 09 開頭後接 8 個數字"
            );
        }
    }



    // =====================================================
    // Nullable String 處理
    // =====================================================
    private String normalizeNullableText(
            String value
    ) {


        if (
                value == null
        ) {

            return null;
        }


        String trimmed =
                value.trim();


        return trimmed.isEmpty()

                ? null

                : trimmed;
    }
}