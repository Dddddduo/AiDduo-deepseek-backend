package bhuacm.dduo.deepseek.Filter;

import bhuacm.dduo.deepseek.Filter.IpFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * IP过滤器配置
 */
@Configuration
class IpFilterConfig {

    @Value("${ip.whitelist:127.0.0.1,localhost}")
    private String ipWhitelistConfig;

    @Value("${ip.whitelist.enabled:true}")
    private boolean ipWhitelistEnabled;

    @Bean
    public IpFilter ipFilter() {
        List<Predicate<String>> ipRules = parseIpRules(ipWhitelistConfig);
        return new IpFilter(ipRules, ipWhitelistEnabled);
    }

    /**
     * 解析IP规则，支持CIDR表示法
     */
    private List<Predicate<String>> parseIpRules(String config) {
        if (config == null || config.trim().isEmpty()) {
            return Collections.emptyList();
        }

        return Arrays.stream(config.split(","))
                .map(String::trim)
                .filter(rule -> !rule.isEmpty())
                .map(this::createIpPredicate)
                .collect(Collectors.toList());
    }

    /**
     * 创建IP地址匹配规则
     */
    private Predicate<String> createIpPredicate(String rule) {
        // 处理CIDR表示法 (例如: 192.168.1.0/24)
        if (rule.contains("/")) {
            String[] parts = rule.split("/");
            String baseIp = parts[0];
            int prefixLength = Integer.parseInt(parts[1]);

            try {
                InetAddress baseAddress = InetAddress.getByName(baseIp);
                int maskBits = 0xFFFFFFFF << (32 - prefixLength);

                if (baseAddress instanceof java.net.Inet4Address) {
                    byte[] baseBytes = baseAddress.getAddress();
                    int baseInt = ((baseBytes[0] & 0xFF) << 24) |
                            ((baseBytes[1] & 0xFF) << 16) |
                            ((baseBytes[2] & 0xFF) << 8) |
                            (baseBytes[3] & 0xFF);

                    return ip -> {
                        try {
                            InetAddress addr = InetAddress.getByName(ip);
                            if (addr instanceof java.net.Inet4Address) {
                                byte[] bytes = addr.getAddress();
                                int ipInt = ((bytes[0] & 0xFF) << 24) |
                                        ((bytes[1] & 0xFF) << 16) |
                                        ((bytes[2] & 0xFF) << 8) |
                                        (bytes[3] & 0xFF);
                                return (ipInt & maskBits) == (baseInt & maskBits);
                            }
                            return false;
                        } catch (UnknownHostException e) {
                            return false;
                        }
                    };
                }
            } catch (UnknownHostException | NumberFormatException e) {
                // 无效的CIDR格式，退化为普通IP匹配
            }
        }

        // 处理IP段通配符 (例如: 192.168.1.*)
        if (rule.contains("*")) {
            String regex = "^" + rule.replace(".", "\\.").replace("*", "[0-9]{1,3}") + "$";
            Pattern pattern = Pattern.compile(regex);
            return ip -> pattern.matcher(ip).matches();
        }

        // 处理localhost别名
        if ("localhost".equalsIgnoreCase(rule)) {
            return ip -> "127.0.0.1".equals(ip) || "0:0:0:0:0:0:0:1".equals(ip);
        }

        // 处理普通IP地址
        return rule::equals;
    }
}
