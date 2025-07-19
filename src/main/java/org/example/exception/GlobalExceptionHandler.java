package org.example.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.apache.tomcat.util.buf.StringUtils;
import org.example.comm.enums.ExceptionEnum;
import org.example.params.resp.CommonResponse;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.ArrayList;
import java.util.List;

/**
 * global exception handler
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    /**
     * handle param valid exception
     * @param e
     * @return
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseBody
    public CommonResponse bindExceptionHandler(MethodArgumentNotValidException e) {
        BindingResult bindingResult = e.getBindingResult();
        List<String> errors = new ArrayList<>();
        bindingResult.getFieldErrors().forEach(item -> {
            errors.add(item.getDefaultMessage());
        });
        String errorMessage = StringUtils.join(errors, '|');

        CommonResponse response = new CommonResponse();
        response.setSuccess(false);
        response.setErrorCode(ExceptionEnum.PARAM_ILLEGAL.getErrorCode());
        response.setErrorMsg(errorMessage);
        return response;
    }

    /**
     * handle business exception
     * @param req
     * @param e
     * @return
     */
    @ExceptionHandler(value = BusinessException.class)
    @ResponseBody
    public CommonResponse bizExceptionHandler(HttpServletRequest req, BusinessException e){
        CommonResponse response = new CommonResponse();
        response.setSuccess(false);
        response.setErrorCode(e.getErrorCode());
        response.setErrorMsg(e.getErrorMsg());
        return response;
    }

    /**
     * handle other exception
     * @param req
     * @param e
     * @return
     */
    @ExceptionHandler(value = Exception.class)
    @ResponseBody
    public CommonResponse exceptionHandler(HttpServletRequest req, Exception e){
        CommonResponse response = new CommonResponse();
        response.setSuccess(false);
        response.setErrorCode(ExceptionEnum.INTERNAL_SERVER_ERROR.getErrorCode());
        response.setErrorMsg(ExceptionEnum.INTERNAL_SERVER_ERROR.getErrorMsg());
        return response;
    }
}
