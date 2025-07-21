package org.example.transfer.service;

import org.example.transfer.comm.enums.Currency;
import org.example.transfer.comm.enums.ExceptionEnum;
import org.example.transfer.exception.BusinessException;
import org.example.transfer.model.Account;
import org.example.transfer.model.FxRate;
import org.example.transfer.params.req.TransferRequest;
import org.example.transfer.repository.AccountRepository;
import org.example.transfer.repository.FxRateRepository;
import org.example.transfer.repository.TransferLogRepository;
import org.junit.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * transfer service test
 * <p>
 * only test enable different transfer type
 */
@TestPropertySource(properties = "transfer.enable-different-currency-transfer=true")
public class TransferServiceDifferentTransferTypeTest extends BaseServiceTest {
    @Autowired
    private TransferService transferService;

    @MockBean
    private AccountRepository accountRepository;

    @MockBean
    private FxRateRepository fxRateRepository;

    @MockBean
    private TransferLogRepository transferLogRepository;


    @BeforeEach
    public void init() {
        when(transferLogRepository.save(any())).thenReturn(null);
    }

    @Test
    public void testTransferNoExistingRate() {
        TransferRequest request = new TransferRequest();
        request.setFromId(1L);
        request.setToId(2L);
        request.setTransferCurrency(Currency.USD);
        request.setAmount(BigDecimal.valueOf(50));

        Account from = new Account();
        from.setId(1L);
        from.setName("Alice");
        from.setCurrency(Currency.USD);
        from.setBalance(BigDecimal.valueOf(1000));

        Account to = new Account();
        to.setId(2L);
        to.setName("Bob");
        to.setCurrency(Currency.JPN);
        to.setBalance(BigDecimal.valueOf(500));

        when(accountRepository.findAllById(List.of(1L, 2L))).thenReturn(List.of(from, to));
        when(fxRateRepository.findByFromCurrencyAndToCurrency(Currency.USD, Currency.JPN))
                .thenReturn(Optional.ofNullable(null));

        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> transferService.transfer(request)
        );

        assertEquals(ExceptionEnum.RATE_NOT_SUPPORT.getErrorCode(), ex.getErrorCode());
        assertEquals("not support rate!", ex.getErrorMsg());
    }

    @Test
    public void testTransferDiffCurrencyInsufficientBalance_Fail() {
        TransferRequest request = new TransferRequest();
        request.setFromId(1L);
        request.setToId(2L);
        request.setTransferCurrency(Currency.USD);
        request.setAmount(BigDecimal.valueOf(1000));

        Account from = new Account();
        from.setId(1L);
        from.setName("Alice");
        from.setCurrency(Currency.USD);
        from.setBalance(BigDecimal.valueOf(1000));

        Account to = new Account();
        to.setId(2L);
        to.setName("Bob");
        to.setCurrency(Currency.AUD);
        to.setBalance(BigDecimal.valueOf(500));

        FxRate rate = new FxRate();
        rate.setId(1L);
        rate.setFromCurrency(Currency.USD);
        rate.setToCurrency(Currency.AUD);
        rate.setRate(BigDecimal.valueOf(2));

        when(accountRepository.findAllById(List.of(1L, 2L))).thenReturn(List.of(from, to));
        when(fxRateRepository.findByFromCurrencyAndToCurrency(Currency.USD, Currency.AUD))
                .thenReturn(Optional.of(rate));

        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> transferService.transfer(request)
        );

        assertEquals(ExceptionEnum.MONEY_TRANSFER_ERROR.getErrorCode(), ex.getErrorCode());
        assertEquals("Insufficient balance", ex.getErrorMsg());
    }

    @Test
    public void testTransferDiffCurrency_Success() {
        TransferRequest request = new TransferRequest();
        request.setFromId(1L);
        request.setToId(2L);
        request.setTransferCurrency(Currency.USD);
        request.setAmount(BigDecimal.valueOf(50));

        Account from = new Account();
        from.setId(1L);
        from.setName("Alice");
        from.setCurrency(Currency.USD);
        from.setBalance(BigDecimal.valueOf(1000));

        Account to = new Account();
        to.setId(2L);
        to.setName("Bob");
        to.setCurrency(Currency.AUD);
        to.setBalance(BigDecimal.valueOf(500));

        FxRate rate = new FxRate();
        rate.setId(1L);
        rate.setFromCurrency(Currency.USD);
        rate.setToCurrency(Currency.AUD);
        rate.setRate(BigDecimal.valueOf(2));

        when(accountRepository.findAllById(List.of(1L, 2L))).thenReturn(List.of(from, to));
        when(fxRateRepository.findByFromCurrencyAndToCurrency(Currency.USD, Currency.AUD))
                .thenReturn(Optional.of(rate));

        transferService.transfer(request);

        assertEquals(0, BigDecimal.valueOf(949.50).compareTo(from.getBalance()));
        assertEquals(0, BigDecimal.valueOf(600).compareTo(to.getBalance()));
    }
}
