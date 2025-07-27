package org.example.transfer.handler;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.example.transfer.comm.enums.TransferTypeEnum;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.Map;

@Component
public class TransferHandlerFactory implements InitializingBean, ApplicationContextAware {
    Logger log = LogManager.getLogger(TransferHandlerFactory.class);

    private static final Map<TransferTypeEnum, TransferHandler> TRANSFER_HANDLER_MAP = new EnumMap<>(TransferTypeEnum.class);
    private ApplicationContext appContext;

    public TransferHandler getHandler(TransferTypeEnum transferType) {
        if (!TRANSFER_HANDLER_MAP.containsKey(transferType)) {
            log.error("transferType:{} is not supported by the system, please contact developer.", transferType);
        }

        return TRANSFER_HANDLER_MAP.get(transferType);
    }

    /**
     * after application context start, put beans which implement TransferHandler into TRANSFER_HANDLER_MAP
     *
     * @throws Exception
     */
    @Override
    public void afterPropertiesSet() throws Exception {
        appContext.getBeansOfType(TransferHandler.class)
                .values()
                .forEach(handler -> TRANSFER_HANDLER_MAP.put(handler.getTransferType(), handler));
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        appContext = applicationContext;
    }
}
