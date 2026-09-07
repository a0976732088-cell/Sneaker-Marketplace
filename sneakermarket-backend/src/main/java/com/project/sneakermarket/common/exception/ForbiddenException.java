package com.project.sneakermarket.common.exception;

/**
 * 使用者已具備身分，
 * 但沒有權限操作指定資源時使用。
 *
 * 例如：
 * 非競標賣家本人嘗試修改或取消競標。
 */
public class ForbiddenException extends RuntimeException {

    public ForbiddenException(String message) {
        super(message);
    }
}