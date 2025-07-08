package bhuacm.dduo.deepseek.Filter;

import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

/**
 * IP过滤器实现
 */
public class IpFilter {

    private final List<Predicate<String>> ipRules;
    private final boolean enabled;

    public IpFilter(List<Predicate<String>> ipRules, boolean enabled) {
        this.ipRules = Collections.unmodifiableList(ipRules);
        this.enabled = enabled;
    }

    /**
     * 检查IP是否允许访问
     */
    public boolean isIpAllowed(String ip) {
        if (!enabled || ipRules.isEmpty()) {
            return true;
        }

        return ipRules.stream().anyMatch(rule -> rule.test(ip));
    }
}