package bhuacm.dduo.deepseek.Controller;

import bhuacm.dduo.deepseek.Filter.IpFilter;
import bhuacm.dduo.deepseek.Test.DeepSeekContent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.WebRequest;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/dduoai")
public class DeepSeekController {

    @Autowired
    private IpFilter ipFilter;

    // 从配置文件中加载 api-key
    @Value("${deepseek.api-key}")
    private String apiKey;

    /**
     * 处理聊天请求
     * 对应前端请求地址：http://gpt.gczdy.cn:8080/dduoai/chat?message=xxx
     */
    @GetMapping("/chat")
    public String chat(@RequestParam("message") String message,
                       HttpServletRequest request) {

        // 获取并验证客户端IP
        String clientIp = getClientIp(request);
        if (!ipFilter.isIpAllowed(clientIp)) {
            throw new IpAccessDeniedException(clientIp);
        }

        try {
            DeepSeekContent deepSeekContent = new DeepSeekContent(apiKey);
            // 调用API生成回复
            return deepSeekContent.generateContent(message);
        } catch (IOException e) {
            // 使用更规范的异常处理
            throw new AiServiceException("AI服务调用失败", e);
        }
    }

    /**
     * 获取客户端真实IP地址，支持代理环境
     */
    private String getClientIp(HttpServletRequest request) {
        return Optional.ofNullable(request.getHeader("X-Forwarded-For"))
                .map(header -> header.split(",")[0].trim())
                .orElse(request.getRemoteAddr());
    }

    /**
     * IP访问拒绝异常
     */
    static class IpAccessDeniedException extends RuntimeException {
        public IpAccessDeniedException(String ip) {
            super("IP地址 [" + ip + "] 没有访问权限");
        }
    }

    /**
     * AI服务异常
     */
    static class AiServiceException extends RuntimeException {
        public AiServiceException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * 全局异常处理器
     */
    @RestControllerAdvice
    static class ExceptionHandler {

        @org.springframework.web.bind.annotation.ExceptionHandler(IpAccessDeniedException.class)
        ProblemDetail handleIpAccessDeniedException(IpAccessDeniedException ex, WebRequest request) {
            ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                    HttpStatus.FORBIDDEN, ex.getMessage());
            problemDetail.setTitle("访问拒绝");
            return problemDetail;
        }

        @org.springframework.web.bind.annotation.ExceptionHandler(AiServiceException.class)
        ProblemDetail handleAiServiceException(AiServiceException ex, WebRequest request) {
            ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                    HttpStatus.SERVICE_UNAVAILABLE, "AI服务暂时不可用");
            problemDetail.setTitle("服务异常");
            problemDetail.setProperty("errorDetails", ex.getMessage());
            return problemDetail;
        }
    }
}

