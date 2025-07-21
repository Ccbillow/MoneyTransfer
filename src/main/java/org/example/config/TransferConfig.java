package org.example.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "transfer")
public class TransferConfig {

    /**
     * support different currency transfer,
     *
     * default: false
     */
    private boolean enableDifferentCurrencyTransfer = false;

    public boolean isEnableDifferentCurrencyTransfer() {
        return enableDifferentCurrencyTransfer;
    }

    public void setEnableDifferentCurrencyTransfer(boolean enableDifferentCurrencyTransfer) {
        this.enableDifferentCurrencyTransfer = enableDifferentCurrencyTransfer;
    }
}