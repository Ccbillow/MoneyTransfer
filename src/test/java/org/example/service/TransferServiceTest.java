package org.example.service;

import org.example.BaseTest;
import org.example.comm.enums.Currency;
import org.example.comm.enums.ExceptionEnum;
import org.example.exception.BusinessException;
import org.example.model.Account;
import org.example.model.FxRate;
import org.example.params.req.TransferRequest;
import org.example.repository.AccountRepository;
import org.example.repository.FxRateRepository;
import org.example.repository.TransferLogRepository;
import org.example.service.impl.TransferService;
import org.junit.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * transfer service test
 */
public class TransferServiceTest extends BaseTest {
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
    public void testTransfer_Same_User() {
        TransferRequest request = new TransferRequest();
        request.setFromId(1L);
        request.setToId(1L);
        request.setTransferCurrency(Currency.USD);
        request.setAmount(BigDecimal.ONE);

        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> transferService.transfer(request)
        );

        assertEquals(ex.getErrorCode(), ExceptionEnum.PARAM_ILLEGAL.getErrorCode());
        assertEquals(ex.getErrorMsg(), "same account transfer not allowed");
    }

    @Test
    public void testTransfer_From_Not_Exist() {
        TransferRequest request = new TransferRequest();
        request.setFromId(1L);
        request.setToId(2L);
        request.setTransferCurrency(Currency.USD);
        request.setAmount(BigDecimal.ONE);

        when(accountRepository.findById(1L)).thenReturn(Optional.ofNullable(null));
        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> transferService.transfer(request)
        );

        assertEquals(ex.getErrorCode(), ExceptionEnum.USER_NOT_EXIST.getErrorCode());
        assertEquals(ex.getErrorMsg(), "from account not exist");
    }

    @Test
    public void testTransfer_To_Not_Exist() {
        TransferRequest request = new TransferRequest();
        request.setFromId(1L);
        request.setToId(2L);
        request.setTransferCurrency(Currency.USD);
        request.setAmount(BigDecimal.ONE);

        Account from = new Account();
        from.setId(1L);
        from.setName("Alice");
        from.setCurrency(Currency.USD);
        from.setBalance(BigDecimal.valueOf(1000));

        when(accountRepository.findById(1L)).thenReturn(Optional.of(from));
        when(accountRepository.findById(2L)).thenReturn(Optional.ofNullable(null));

        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> transferService.transfer(request)
        );

        assertEquals(ex.getErrorCode(), ExceptionEnum.USER_NOT_EXIST.getErrorCode());
        assertEquals(ex.getErrorMsg(), "to account not exist");
    }

    @Test
    public void testTransfer_From_Currency_Not_Match() {
        TransferRequest request = new TransferRequest();
        request.setFromId(1L);
        request.setToId(2L);
        request.setTransferCurrency(Currency.AUD);
        request.setAmount(BigDecimal.ONE);

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

        when(accountRepository.findById(1L)).thenReturn(Optional.of(from));
        when(accountRepository.findById(2L)).thenReturn(Optional.of(to));

        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> transferService.transfer(request)
        );
        assertEquals(ex.getErrorCode(), ExceptionEnum.PARAM_ILLEGAL.getErrorCode());
        assertEquals(ex.getErrorMsg(), "Sender must use base currency.");
    }

    @Test
    public void testTransfer_Same_Currency_Insufficient_Balance() {
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
        to.setCurrency(Currency.USD);
        to.setBalance(BigDecimal.valueOf(500));

        when(accountRepository.findById(1L)).thenReturn(Optional.of(from));
        when(accountRepository.findById(2L)).thenReturn(Optional.of(to));

        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> transferService.transfer(request)
        );

        assertEquals(ExceptionEnum.MONEY_TRANSFER_ERROR.getErrorCode(), ex.getErrorCode());
        assertEquals("Insufficient balance", ex.getErrorMsg());
    }

    @Test
    public void testTransfer_Same_Currency() {
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
        to.setCurrency(Currency.USD);
        to.setBalance(BigDecimal.valueOf(500));

        when(accountRepository.findById(1L)).thenReturn(Optional.of(from));
        when(accountRepository.findById(2L)).thenReturn(Optional.of(to));
        when(accountRepository.save(any())).thenReturn(null);

        transferService.transfer(request);

        assertEquals(0, BigDecimal.valueOf(949.50).compareTo(from.getBalance()));
        assertEquals(0, BigDecimal.valueOf(550).compareTo(to.getBalance()));
    }

    @Test
    public void testTransfer_No_Existing_Rate() {
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

        when(accountRepository.findById(1L)).thenReturn(Optional.of(from));
        when(accountRepository.findById(2L)).thenReturn(Optional.of(to));
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
    public void testTransfer_Diff_Currency_Insufficient_Balance() {
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

        when(accountRepository.findById(1L)).thenReturn(Optional.of(from));
        when(accountRepository.findById(2L)).thenReturn(Optional.of(to));
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
    public void testTransfer_Diff_Currency() {
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
        when(accountRepository.findById(1L)).thenReturn(Optional.of(from));
        when(accountRepository.findById(2L)).thenReturn(Optional.of(to));

        when(fxRateRepository.findByFromCurrencyAndToCurrency(Currency.USD, Currency.AUD))
                .thenReturn(Optional.of(rate));

        transferService.transfer(request);

        assertEquals(0, BigDecimal.valueOf(949.50).compareTo(from.getBalance()));
        assertEquals(0, BigDecimal.valueOf(600).compareTo(to.getBalance()));
    }
}
