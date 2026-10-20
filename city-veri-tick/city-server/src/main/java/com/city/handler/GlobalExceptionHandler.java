package com.city.handler;

import com.city.constant.CommonMessageConstant;
import com.city.exception.RefundException;
import com.city.exception.AccountOperationException;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DuplicateKeyException;

import com.city.exception.BusinessException;
import com.city.result.Result;
import com.city.result.SystemResultCode;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<Result<Void>> handleDataAccess(DataAccessException exception) {
        log.warn("Infrastructure operation unavailable: failure={}", exception.getClass().getSimpleName());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Result.error(50301,
                CommonMessageConstant.SERVICE_UNAVAILABLE));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Result<Void>> handleArgument(IllegalArgumentException exception) {
        return ResponseEntity.badRequest().body(Result.error(exception.getMessage()));
    }

    @ExceptionHandler(RefundException.class)
    public ResponseEntity<Result<Void>> handleRefund(RefundException exception) {
        return ResponseEntity.badRequest().body(Result.error(exception.getMessage()));
    }

    @ExceptionHandler(AccountOperationException.class)
    public ResponseEntity<Result<Void>> handleAccountOperation(AccountOperationException exception) {
        return ResponseEntity.badRequest().body(Result.error(exception.getMessage()));
    }

    @ExceptionHandler(DuplicateKeyException.class)
    public ResponseEntity<Result<Void>> handleDuplicate(DuplicateKeyException exception) {
        return ResponseEntity.status(409).body(Result.error(CommonMessageConstant.DUPLICATE_RESOURCE));
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Result<Void>> handleBusiness(BusinessException exception) {
        if (exception.resultCode() == SystemResultCode.ACCOUNT_DISABLED) {
            return ResponseEntity.status(401).body(Result.error(exception.resultCode().code(), exception.resultCode().message()));
        }
        return ResponseEntity.status(exception.resultCode().code())
                .body(Result.error(exception.resultCode().message()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Result<Void>> handleValidation(MethodArgumentNotValidException exception) {
        FieldError error = exception.getBindingResult().getFieldError();
        String message = error == null ? SystemResultCode.BAD_REQUEST.message() : error.getDefaultMessage();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Result.error(message));
    }
}
